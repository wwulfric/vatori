package dev.vatori.desktop.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import javax.xml.parsers.DocumentBuilderFactory;

public final class VaadinDesktopCli {
    private static final String USAGE = """
            Usage:
              bin/vaadin-desktop dev <module>
              bin/vaadin-desktop dev-compat <module>
              bin/vaadin-desktop package <module>
              bin/vaadin-desktop help

            dev uses a packaged standard Vaadin production bundle over Flow IPC.
            dev-compat requires the optional Spring Boot adapter and uses localhost HTTP.
            """;

    private VaadinDesktopCli() {
    }

    public static void main(String[] args) {
        int status = run(args, System.getenv(), System.out, System.err);
        if (status != 0) {
            System.exit(status);
        }
    }

    static int run(String[] args, Map<String, String> environment,
            PrintStream out, PrintStream err) {
        String command = args.length == 0 ? "help" : args[0];
        if (command.equals("help") || command.equals("--help") || command.equals("-h")) {
            out.print(USAGE);
            return 0;
        }
        if (!command.equals("dev") && !command.equals("dev-compat")
                && !command.equals("package")) {
            err.println("Unknown command: " + command);
            return 2;
        }
        if (args.length != 2) {
            err.println("Usage: bin/vaadin-desktop " + command + " <module>");
            return 2;
        }
        if (!System.getProperty("os.name", "").toLowerCase().contains("mac")) {
            err.println("Vaadin Desktop currently provides only native-hosts/macos.");
            return 1;
        }

        try {
            Path root = repositoryRoot();
            Path module = resolveModule(root, args[1]);
            String transport = command.equals("dev-compat")
                    ? "compatibility-http"
                    : "flow-ipc";
            buildApplication(root, module, args[1], environment, out);
            Path runnableJar = findRunnableJar(module.resolve("target"));
            Path webApp = transport.equals("flow-ipc")
                    ? requireWebApp(module)
                    : null;
            Path capabilityManifest = module.resolve("desktop-capabilities.json");
            if (!Files.isRegularFile(capabilityManifest)) {
                capabilityManifest = null;
            }
            Path app = buildMacosHost(root, transport, runnableJar, webApp,
                    capabilityManifest, environment);
            if (command.equals("package")) {
                out.println("macOS app: " + app);
                return 0;
            }
            return launch(app, root.resolve(".build/native-hosts/macos"), environment, out);
        } catch (CliException exception) {
            err.println(exception.getMessage());
            return exception.exitCode();
        }
    }

    private static Path repositoryRoot() throws CliException {
        String configured = System.getProperty("vatori.repo.root", "");
        if (configured.isBlank()) {
            throw new CliException(1,
                    "CLI bootstrap did not provide the vatori repository root.");
        }
        Path root = Path.of(configured).toAbsolutePath().normalize();
        if (!Files.isRegularFile(root.resolve("pom.xml"))) {
            throw new CliException(1, "Repository pom.xml not found: " + root);
        }
        return root;
    }

    private static Path resolveModule(Path root, String value) throws CliException {
        Path module = root.resolve(value).normalize();
        if (!module.startsWith(root) || !Files.isRegularFile(module.resolve("pom.xml"))) {
            throw new CliException(1, "Application module not found: " + value);
        }
        return module;
    }

    private static void buildApplication(Path root, Path module, String moduleSelector,
            Map<String, String> environment, PrintStream out)
            throws CliException {
        String maven = environment.getOrDefault("MAVEN_EXECUTABLE", "mvn");
        String mode = "1".equals(environment.get("VAADIN_DESKTOP_MAVEN_OFFLINE"))
                ? "-o"
                : "-U";
        String repository = environment.getOrDefault(
                "MAVEN_REPO", root.resolve(".m2/repository").toString());
        out.println("Building runnable sidecar JAR for " + moduleSelector);
        runProcess(root, List.of(
                maven, mode, "-Dmaven.repo.local=" + repository,
                "-f", root.resolve("pom.xml").toString(),
                "-pl", moduleSelector, "-am", "-DskipTests",
                "-Dmaven.jar.forceCreation=true", "install"), environment, true);

        String vaadinVersion = readVaadinVersion(root.resolve("pom.xml"));
        out.println("Building standard Vaadin production bundle for " + moduleSelector);
        GeneratedFrontendWorkspace workspace =
                GeneratedFrontendWorkspace.capture(module);
        CliException failure = null;
        try {
            runProcess(root, List.of(
                    maven, mode, "-Dmaven.repo.local=" + repository,
                    "-f", module.resolve("pom.xml").toString(),
                    "-DskipTests", "-Dvaadin.productionMode=true",
                    "-Dvaadin.force.production.build=true",
                    "-Dfrontend.ignore.version.checks=true",
                    "com.vaadin:vaadin-maven-plugin:" + vaadinVersion
                            + ":build-frontend",
                    "package"), environment, true);
        } catch (CliException exception) {
            failure = exception;
        }
        try {
            workspace.cleanGeneratedFiles();
        } catch (CliException cleanupFailure) {
            if (failure == null) {
                throw cleanupFailure;
            }
            failure.addSuppressed(cleanupFailure);
        }
        if (failure != null) {
            throw failure;
        }
    }

    private static String readVaadinVersion(Path pom) throws CliException {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            var document = factory.newDocumentBuilder().parse(pom.toFile());
            var values = document.getElementsByTagName("vaadin.version");
            if (values.getLength() != 1 || values.item(0).getTextContent().isBlank()) {
                throw new CliException(1, "vaadin.version is missing from " + pom);
            }
            return values.item(0).getTextContent().trim();
        } catch (CliException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new CliException(1, "Cannot read " + pom + ": " + exception.getMessage());
        }
    }

    static Path findRunnableJar(Path target) throws CliException {
        if (!Files.isDirectory(target)) {
            throw new CliException(1, "Application target directory not found: " + target);
        }
        List<Path> candidates = new ArrayList<>();
        try (var entries = Files.list(target)) {
            for (Path entry : entries.sorted(Comparator.comparing(Path::toString)).toList()) {
                String name = entry.getFileName().toString();
                if (!Files.isRegularFile(entry) || !name.endsWith(".jar")
                        || name.endsWith("-sources.jar") || name.endsWith("-javadoc.jar")
                        || name.endsWith(".jar.original") || name.startsWith("original-")) {
                    continue;
                }
                try (JarFile jar = new JarFile(entry.toFile())) {
                    if (jar.getManifest() != null
                            && jar.getManifest().getMainAttributes().getValue("Main-Class") != null) {
                        candidates.add(entry.toAbsolutePath().normalize());
                    }
                }
            }
        } catch (IOException exception) {
            throw new CliException(1,
                    "Cannot inspect runnable JARs in " + target + ": " + exception.getMessage());
        }
        if (candidates.size() != 1) {
            throw new CliException(1, "Expected exactly one runnable JAR in " + target
                    + " but found " + candidates.size() + ": " + candidates);
        }
        return candidates.getFirst();
    }

    private static Path requireWebApp(Path module) throws CliException {
        Path webApp = module.resolve("target/classes/META-INF/VAADIN/webapp");
        if (!Files.isRegularFile(webApp.resolve("index.html"))) {
            throw new CliException(1,
                    "Standard Vaadin production bundle is incomplete: " + webApp);
        }
        return webApp;
    }

    private static Path buildMacosHost(Path root, String transport, Path runnableJar,
            Path webApp, Path capabilityManifest, Map<String, String> environment)
            throws CliException {
        List<String> command = new ArrayList<>(List.of(
                "bash", root.resolve("bin/vaadin-desktop-build-macos-host").toString(),
                root.toString(), transport, runnableJar.toString(),
                webApp == null ? "" : webApp.toString(),
                capabilityManifest == null ? "" : capabilityManifest.toString()));
        runProcess(root, command, environment, true);
        Path app = root.resolve(".build/native-hosts/macos/VaadinDesktopHost.app");
        if (!Files.isExecutable(app.resolve("Contents/MacOS/VaadinDesktopHost"))) {
            throw new CliException(1, "macOS host build did not produce an executable app: " + app);
        }
        return app;
    }

    private static int launch(Path app, Path logDirectory,
            Map<String, String> environment, PrintStream out) throws CliException {
        try {
            Files.createDirectories(logDirectory);
        } catch (IOException exception) {
            throw new CliException(1, "Cannot create host log directory: " + exception.getMessage());
        }
        Path stdout = logDirectory.resolve("host.out.log");
        Path stderr = logDirectory.resolve("host.err.log");
        List<String> command = new ArrayList<>(List.of(
                "open", "-n", "-W", "-o", stdout.toString(),
                "--stderr", stderr.toString()));
        String path = environment.get("PATH");
        if (path != null) {
            command.addAll(List.of("--env", "PATH=" + path));
        }
        String javaHome = environment.get("JAVA_HOME");
        if (javaHome != null) {
            command.addAll(List.of("--env", "JAVA_HOME=" + javaHome));
        }
        command.add(app.toString());
        out.println("Vaadin Desktop Host logs:");
        out.println("  stdout: " + stdout);
        out.println("  stderr: " + stderr);
        return runProcess(app.getParent(), command, environment, true);
    }

    private static int runProcess(Path directory, List<String> command,
            Map<String, String> environment, boolean inheritIo) throws CliException {
        ProcessBuilder builder = new ProcessBuilder(command).directory(directory.toFile());
        builder.environment().putAll(environment);
        if (inheritIo) {
            builder.inheritIO();
        }
        try {
            int status = builder.start().waitFor();
            if (status != 0) {
                throw new CliException(status,
                        "Command failed with exit code " + status + ": " + command.getFirst());
            }
            return status;
        } catch (IOException exception) {
            throw new CliException(1,
                    "Cannot execute " + command.getFirst() + ": " + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CliException(130, "Command interrupted: " + command.getFirst());
        }
    }

    private record GeneratedFrontendWorkspace(List<GeneratedPath> paths) {
        private static GeneratedFrontendWorkspace capture(Path module) {
            return new GeneratedFrontendWorkspace(List.of(
                    generated(module.resolve("package.json")),
                    generated(module.resolve("package-lock.json")),
                    generated(module.resolve("pnpm-lock.yaml")),
                    generated(module.resolve("bun.lock")),
                    generated(module.resolve("bun.lockb")),
                    generated(module.resolve("tsconfig.json")),
                    generated(module.resolve("types.d.ts")),
                    generated(module.resolve("vite.config.ts")),
                    generated(module.resolve("vite.generated.ts")),
                    generated(module.resolve("node_modules")),
                    generated(module.resolve("src/main/frontend/generated")),
                    generated(module.resolve("src/main/frontend/index.html")),
                    generated(module.resolve("src/main/bundles"))));
        }

        private static GeneratedPath generated(Path path) {
            return new GeneratedPath(path, Files.exists(path));
        }

        private void cleanGeneratedFiles() throws CliException {
            for (GeneratedPath generated : paths) {
                if (!generated.existed()) {
                    deleteRecursively(generated.path());
                }
            }
        }

        private static void deleteRecursively(Path path) throws CliException {
            if (!Files.exists(path)) {
                return;
            }
            try (var entries = Files.walk(path)) {
                for (Path entry : entries.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(entry);
                }
            } catch (IOException exception) {
                throw new CliException(1,
                        "Cannot clean generated frontend path " + path + ": "
                                + exception.getMessage());
            }
        }
    }

    private record GeneratedPath(Path path, boolean existed) {
    }

    static final class CliException extends Exception {
        private final int exitCode;

        CliException(int exitCode, String message) {
            super(message);
            this.exitCode = exitCode;
        }

        int exitCode() {
            return exitCode;
        }
    }
}
