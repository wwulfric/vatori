package dev.vatori.desktop.cli;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarFile;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public final class VaadinDesktopCli {
    private static final String USAGE = """
            Usage:
              bin/vaadin-desktop dev <module>
              bin/vaadin-desktop dev-vite <module>
              bin/vaadin-desktop dev-compat <module>
              bin/vaadin-desktop package <module>
              bin/vaadin-desktop help

            dev uses a packaged standard Vaadin production bundle over Flow IPC.
            dev-vite uses the standard Vaadin frontend through a managed Vite server
            while Flow browser requests continue over opaque IPC.
            dev-compat requires the optional Spring Boot adapter and uses localhost HTTP.
            """;
    private static final String HOST_CONTROL_PREFIX = "VATORI_HOST_CONTROL ";
    private static final String HOST_CONTROL_RESULT_PREFIX =
            "VATORI_HOST_CONTROL_RESULT ";
    private static final Duration DEVELOPMENT_REBUILD_DEBOUNCE =
            Duration.ofMillis(500);
    private static final Duration HOST_POLL_INTERVAL = Duration.ofMillis(150);
    private static final Duration HOST_REPLACEMENT_RESULT_TIMEOUT =
            Duration.ofSeconds(60);

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
        if (!command.equals("dev") && !command.equals("dev-vite")
                && !command.equals("dev-compat")
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
            ApplicationBundleInputs bundleInputs =
                    ApplicationBundleInputs.discover(module);
            String transport = command.equals("dev-compat")
                    ? "compatibility-http"
                    : "flow-ipc";
            DevelopmentInputs developmentInputs = buildSidecar(
                    root,
                    module,
                    args[1],
                    environment,
                    out,
                    command.equals("dev-vite"));
            Path runnableJar = findRunnableJar(module.resolve("target"));
            Path capabilityManifest = module.resolve("desktop-capabilities.json");
            if (!Files.isRegularFile(capabilityManifest)) {
                capabilityManifest = null;
            }
            if (command.equals("dev-vite")) {
                return runViteDevelopment(root, module, args[1], runnableJar,
                        capabilityManifest, bundleInputs, developmentInputs,
                        environment, out, err);
            }

            Path webApp = transport.equals("flow-ipc")
                    ? buildProductionFrontend(
                            root, module, args[1], environment, out)
                    : null;
            Path app = buildMacosHost(root, transport, runnableJar, webApp,
                    capabilityManifest, null, bundleInputs, environment);
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

    private static DevelopmentInputs buildSidecar(
            Path root,
            Path module,
            String moduleSelector,
            Map<String, String> environment,
            PrintStream out,
            boolean captureDevelopmentModel)
            throws CliException {
        String maven = environment.getOrDefault("MAVEN_EXECUTABLE", "mvn");
        String mode = "1".equals(environment.get("VAADIN_DESKTOP_MAVEN_OFFLINE"))
                ? "-o"
                : "-U";
        String repository = environment.getOrDefault(
                "MAVEN_REPO", root.resolve(".m2/repository").toString());
        DevelopmentModelCapture capture = captureDevelopmentModel
                ? DevelopmentModelCapture.create(root)
                : null;
        try {
            out.println("Building runnable sidecar JAR for " + moduleSelector);
            List<String> command = new ArrayList<>(List.of(
                    maven, mode, "-Dmaven.repo.local=" + repository));
            if (capture != null) {
                capture.configure(command);
            }
            command.addAll(List.of(
                    "-f", root.resolve("pom.xml").toString(),
                    "-pl", moduleSelector, "-am", "-DskipTests",
                    "-Dmaven.jar.forceCreation=true", "install"));
            runProcess(root, command, environment, true);
            return capture == null ? null
                    : capture.read(root, module);
        } finally {
            if (capture != null) {
                capture.close();
            }
        }
    }

    private static Path buildProductionFrontend(
            Path root,
            Path module,
            String moduleSelector,
            Map<String, String> environment,
            PrintStream out) throws CliException {
        String maven = environment.getOrDefault("MAVEN_EXECUTABLE", "mvn");
        String mode = "1".equals(environment.get("VAADIN_DESKTOP_MAVEN_OFFLINE"))
                ? "-o"
                : "-U";
        String repository = environment.getOrDefault(
                "MAVEN_REPO", root.resolve(".m2/repository").toString());
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
        return requireWebApp(module);
    }

    private static int runViteDevelopment(
            Path root,
            Path module,
            String moduleSelector,
            Path runnableJar,
            Path capabilityManifest,
            ApplicationBundleInputs bundleInputs,
            DevelopmentInputs developmentInputs,
            Map<String, String> environment,
            PrintStream out,
            PrintStream err) throws CliException {
        Map<String, String> developmentEnvironment =
                new LinkedHashMap<>(environment);
        developmentEnvironment.put(
                "VAADIN_DESKTOP_FLOW_PROJECT_DIRECTORY",
                module.toString());
        GeneratedFrontendWorkspace workspace =
                GeneratedFrontendWorkspace.capture(module);
        CliException failure = null;
        try {
            prepareViteFrontend(
                    root, module, developmentEnvironment, out);
            try (ViteDevelopmentServer vite =
                         ViteDevelopmentServer.start(
                                 module, developmentEnvironment, out)) {
                DevelopmentWebApp developmentWebApp = new DevelopmentWebApp(
                        vite.origin(), "/index.html");
                Path app = buildMacosHost(root, "flow-ipc", runnableJar, null,
                        capabilityManifest, developmentWebApp, bundleInputs,
                        developmentEnvironment);
                return launchWithDevelopmentReload(
                        root, module, moduleSelector, app,
                        root.resolve(".build/native-hosts/macos"),
                        developmentInputs, developmentEnvironment, out, err);
            }
        } catch (CliException exception) {
            failure = exception;
            throw exception;
        } finally {
            try {
                workspace.cleanGeneratedFiles();
            } catch (CliException cleanupFailure) {
                if (failure == null) {
                    throw cleanupFailure;
                }
                failure.addSuppressed(cleanupFailure);
            }
        }
    }

    private static void prepareViteFrontend(
            Path root,
            Path module,
            Map<String, String> environment,
            PrintStream out) throws CliException {
        String maven = environment.getOrDefault("MAVEN_EXECUTABLE", "mvn");
        String mode = "1".equals(environment.get("VAADIN_DESKTOP_MAVEN_OFFLINE"))
                ? "-o"
                : "-U";
        String repository = environment.getOrDefault(
                "MAVEN_REPO", root.resolve(".m2/repository").toString());
        String vaadinVersion = readVaadinVersion(root.resolve("pom.xml"));
        Path temporaryPom = module.resolve(".vatori-vite-pom.xml");
        writeVitePom(module.resolve("pom.xml"), temporaryPom);
        try {
            out.println("Preparing standard Vaadin Vite workspace for "
                    + root.relativize(module));
            List<String> common = List.of(
                    maven, mode, "-Dmaven.repo.local=" + repository,
                    "-f", temporaryPom.toString(), "-DskipTests",
                    "-Dfrontend.ignore.version.checks=true");
            List<String> scan = new ArrayList<>(common);
            scan.add("com.vaadin:vaadin-maven-plugin:" + vaadinVersion
                    + ":build-frontend");
            runProcess(root, scan, environment, true);

            List<String> prepare = new ArrayList<>(common);
            prepare.add("-Dvaadin.frontend.hotdeploy=true");
            prepare.add("com.vaadin:vaadin-maven-plugin:" + vaadinVersion
                    + ":prepare-frontend");
            runProcess(root, prepare, environment, true);
            requireViteWorkspace(module);
        } finally {
            try {
                Files.deleteIfExists(temporaryPom);
            } catch (IOException exception) {
                throw new CliException(1,
                        "Cannot remove temporary Vite POM " + temporaryPom
                                + ": " + exception.getMessage());
            }
        }
    }

    static void writeVitePom(Path source, Path target)
            throws CliException {
        if (Files.exists(target)) {
            throw new CliException(1,
                    "Temporary Vite POM already exists: " + target);
        }
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(
                    "http://apache.org/xml/features/disallow-doctype-decl", true);
            Document document = factory.newDocumentBuilder().parse(source.toFile());
            Element plugin = findOrAddVaadinPlugin(document);
            Element configuration = directChild(plugin, "configuration");
            if (configuration == null) {
                configuration = document.createElementNS(
                        plugin.getNamespaceURI(), "configuration");
                plugin.appendChild(configuration);
            }
            setChildText(document, configuration, "generateBundle", "false");
            setChildText(document, configuration, "cleanFrontendFiles", "false");
            setChildText(document, configuration, "frontendHotdeploy", "true");
            setChildText(document, configuration, "optimizeBundle", "false");
            addDevelopmentFrontendDependency(document);

            var transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(
                    new DOMSource(document),
                    new StreamResult(target.toFile()));
        } catch (Exception exception) {
            throw new CliException(1,
                    "Cannot create temporary Vite POM from " + source
                            + ": " + exception.getMessage());
        }
    }

    private static Element findOrAddVaadinPlugin(Document document) {
        var plugins = document.getElementsByTagNameNS("*", "plugin");
        for (int index = 0; index < plugins.getLength(); index++) {
            Element plugin = (Element) plugins.item(index);
            Element groupId = directChild(plugin, "groupId");
            Element artifactId = directChild(plugin, "artifactId");
            if (groupId != null && artifactId != null
                    && groupId.getTextContent().trim().equals("com.vaadin")
                    && artifactId.getTextContent().trim()
                            .equals("vaadin-maven-plugin")) {
                return plugin;
            }
        }
        Element project = document.getDocumentElement();
        Element build = directChild(project, "build");
        if (build == null) {
            build = document.createElementNS(project.getNamespaceURI(), "build");
            project.appendChild(build);
        }
        Element pluginContainer = directChild(build, "plugins");
        if (pluginContainer == null) {
            pluginContainer = document.createElementNS(
                    project.getNamespaceURI(), "plugins");
            build.appendChild(pluginContainer);
        }
        Element plugin = document.createElementNS(
                project.getNamespaceURI(), "plugin");
        setChildText(document, plugin, "groupId", "com.vaadin");
        setChildText(document, plugin, "artifactId", "vaadin-maven-plugin");
        setChildText(document, plugin, "version", "${vaadin.version}");
        pluginContainer.appendChild(plugin);
        return plugin;
    }

    private static Element directChild(Element parent, String localName) {
        for (Node child = parent.getFirstChild();
             child != null;
             child = child.getNextSibling()) {
            if (child instanceof Element element
                    && localName.equals(element.getLocalName())) {
                return element;
            }
        }
        return null;
    }

    private static void setChildText(
            Document document,
            Element parent,
            String localName,
            String value) {
        Element child = directChild(parent, localName);
        if (child == null) {
            child = document.createElementNS(parent.getNamespaceURI(), localName);
            parent.appendChild(child);
        }
        child.setTextContent(value);
    }

    private static void addDevelopmentFrontendDependency(Document document) {
        Element project = document.getDocumentElement();
        Element dependencies = directChild(project, "dependencies");
        if (dependencies == null) {
            dependencies = document.createElementNS(
                    project.getNamespaceURI(), "dependencies");
            Element build = directChild(project, "build");
            project.insertBefore(dependencies, build);
        }
        for (Node child = dependencies.getFirstChild();
             child != null;
             child = child.getNextSibling()) {
            if (!(child instanceof Element dependency)
                    || !"dependency".equals(dependency.getLocalName())) {
                continue;
            }
            Element groupId = directChild(dependency, "groupId");
            Element artifactId = directChild(dependency, "artifactId");
            if (groupId != null && artifactId != null
                    && "com.vaadin".equals(groupId.getTextContent().trim())
                    && "vaadin-dev-server".equals(
                            artifactId.getTextContent().trim())) {
                return;
            }
        }
        Element dependency = document.createElementNS(
                project.getNamespaceURI(), "dependency");
        setChildText(document, dependency, "groupId", "com.vaadin");
        setChildText(document, dependency, "artifactId", "vaadin-dev-server");
        setChildText(document, dependency, "version", "${vaadin.version}");
        setChildText(document, dependency, "scope", "runtime");
        dependencies.appendChild(dependency);
    }

    private static void requireViteWorkspace(Path module) throws CliException {
        List<Path> required = List.of(
                module.resolve("package.json"),
                module.resolve("vite.config.ts"),
                module.resolve("node_modules/vite/bin/vite.js"),
                module.resolve("src/main/frontend/generated/vite-devmode.ts"),
                module.resolve("src/main/frontend/generated/vaadin.ts"));
        List<Path> missing = required.stream()
                .filter(path -> !Files.isRegularFile(path))
                .toList();
        if (!missing.isEmpty()) {
            throw new CliException(1,
                    "Vaadin Vite workspace is incomplete: " + missing);
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
            Path webApp, Path capabilityManifest,
            DevelopmentWebApp developmentWebApp,
            ApplicationBundleInputs bundleInputs,
            Map<String, String> environment)
            throws CliException {
        String builder = environment.getOrDefault(
                "VAADIN_DESKTOP_MACOS_HOST_BUILDER",
                root.resolve("bin/vaadin-desktop-build-macos-host").toString());
        List<String> command = new ArrayList<>(List.of(
                "bash", builder,
                root.toString(), transport, runnableJar.toString(),
                webApp == null ? "" : webApp.toString(),
                capabilityManifest == null ? "" : capabilityManifest.toString(),
                developmentWebApp == null ? "" : developmentWebApp.origin(),
                developmentWebApp == null ? "" : developmentWebApp.entryPath()));
        command.addAll(bundleInputs.macosBuilderArguments());
        runProcess(root, command, environment, true);
        Path app = root.resolve(".build/native-hosts/macos/VaadinDesktopHost.app");
        if (!Files.isExecutable(app.resolve("Contents/MacOS/VaadinDesktopHost"))) {
            throw new CliException(1, "macOS host build did not produce an executable app: " + app);
        }
        return app;
    }

    static int launch(Path app, Path logDirectory,
            Map<String, String> environment, PrintStream out) throws CliException {
        try (HostProcess host = HostProcess.start(
                app, logDirectory, environment, out)) {
            return host.waitForExit();
        }
    }

    private static int launchWithDevelopmentReload(
            Path root,
            Path module,
            String moduleSelector,
            Path app,
            Path logDirectory,
            DevelopmentInputs developmentInputs,
            Map<String, String> environment,
            PrintStream out,
            PrintStream err) throws CliException {
        Path stagedJar = app.resolve(
                "Contents/Resources/Sidecar/application.jar");
        try (HostProcess host = HostProcess.start(
                app, logDirectory, environment, out);
             DevelopmentInputWatcher watcher =
                     DevelopmentInputWatcher.watch(developmentInputs)) {
            if (watcher.isActive()) {
                out.println("Watching development inputs in "
                        + developmentInputs.moduleDirectories().size()
                        + " reactor module(s).");
            }
            DevelopmentInputChange pendingChange =
                    DevelopmentInputChange.none();
            long rebuildDeadlineNanos = 0;
            while (!host.waitFor(HOST_POLL_INTERVAL)) {
                DevelopmentInputChange observedChange =
                        watcher.pollChanges();
                if (observedChange.changed()) {
                    pendingChange = pendingChange.merge(observedChange);
                    rebuildDeadlineNanos = System.nanoTime()
                            + DEVELOPMENT_REBUILD_DEBOUNCE.toNanos();
                }
                if (!pendingChange.changed()
                        || System.nanoTime() < rebuildDeadlineNanos) {
                    continue;
                }
                DevelopmentInputChange rebuildChange = pendingChange;
                pendingChange = DevelopmentInputChange.none();
                out.println("Development inputs changed; rebuilding sidecar.");
                Path rebuiltJar;
                try {
                    DevelopmentInputs refreshedInputs = buildSidecar(
                            root,
                            module,
                            moduleSelector,
                            environment,
                            out,
                            true);
                    if (rebuildChange.buildConfigurationChanged()) {
                        out.println("Build configuration changed; refreshing "
                                + "the standard Vaadin Vite workspace.");
                        prepareViteFrontend(
                                root, module, environment, out);
                    }
                    rebuiltJar = findRunnableJar(
                            module.resolve("target"));
                    watcher.refresh(refreshedInputs);
                } catch (CliException exception) {
                    err.println("Development rebuild failed; the running sidecar "
                            + "and staged JAR were left unchanged.");
                    err.println(exception.getMessage());
                    continue;
                }
                if (!host.isAlive()) {
                    return host.waitForExit();
                }
                String requestId = "development-reload-"
                        + UUID.randomUUID().toString().replace("-", "");
                try (SidecarJarReplacement replacement =
                             SidecarJarReplacement.prepare(
                                     rebuiltJar, stagedJar)) {
                    host.requestSidecarReplacement(requestId);
                    out.println(
                            "Rebuilt sidecar staged; replacing Java runtime.");
                    HostControlResult result =
                            host.awaitReplacementResult(
                                    requestId,
                                    HOST_REPLACEMENT_RESULT_TIMEOUT);
                    if (result.status()
                            == HostControlResultStatus.READY) {
                        replacement.commit();
                        out.println("Rebuilt Java runtime is ready.");
                        continue;
                    }
                    if (result.status()
                            != HostControlResultStatus.FAILED) {
                        throw new CliException(1,
                                "Unexpected sidecar replacement result: "
                                        + result.status().wireValue);
                    }

                    replacement.restoreStableJar();
                    err.println("Rebuilt Java runtime failed before ready ("
                            + result.cause()
                            + "); restoring the previous sidecar.");
                    host.requestSidecarRollback(requestId);
                    HostControlResult rollback =
                            host.awaitReplacementResult(
                                    requestId,
                                    HOST_REPLACEMENT_RESULT_TIMEOUT);
                    if (rollback.status()
                            != HostControlResultStatus.ROLLED_BACK) {
                        throw new CliException(1,
                                "Previous sidecar could not be restored: "
                                        + rollback.cause());
                    }
                    replacement.commit();
                    out.println(
                            "Previous Java runtime restored after failed replacement.");
                }
            }
            return host.waitForExit();
        }
    }

    static void stageSidecarJar(Path source, Path destination)
            throws CliException {
        Path parent = destination.getParent();
        Path temporary = null;
        try {
            Files.createDirectories(parent);
            temporary = Files.createTempFile(
                    parent, ".application-", ".jar.tmp");
            Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
            Files.move(
                    temporary,
                    destination,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new CliException(1,
                    "Cannot stage rebuilt sidecar JAR: "
                            + exception.getMessage());
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // The staged destination is authoritative after the move.
                }
            }
        }
    }

    static String sidecarReplacementControlLine(String requestId) {
        requireHostControlRequestId(requestId);
        return HOST_CONTROL_PREFIX
                + "{\"version\":1,\"type\":\"replaceSidecar\","
                + "\"requestId\":\"" + requestId + "\","
                + "\"reason\":\"development-inputs-rebuilt\"}\n";
    }

    static String sidecarRollbackControlLine(String requestId) {
        requireHostControlRequestId(requestId);
        return HOST_CONTROL_PREFIX
                + "{\"version\":1,\"type\":\"rollbackSidecar\","
                + "\"requestId\":\"" + requestId + "\","
                + "\"reason\":\"replacement-candidate-failed\"}\n";
    }

    static final class SidecarJarReplacement
            implements AutoCloseable {
        private final Path stagedJar;
        private final Path stableBackup;
        private boolean committed;

        private SidecarJarReplacement(
                Path stagedJar,
                Path stableBackup) {
            this.stagedJar = stagedJar;
            this.stableBackup = stableBackup;
        }

        static SidecarJarReplacement prepare(
                Path candidate,
                Path stagedJar) throws CliException {
            if (!Files.isRegularFile(stagedJar)) {
                throw new CliException(1,
                        "Staged sidecar JAR is missing: "
                                + stagedJar);
            }
            Path stableBackup = stagedJar.resolveSibling(
                    ".application-stable.jar");
            stageSidecarJar(stagedJar, stableBackup);
            try {
                stageSidecarJar(candidate, stagedJar);
            } catch (CliException exception) {
                try {
                    stageSidecarJar(stableBackup, stagedJar);
                } catch (CliException restoreFailure) {
                    exception.addSuppressed(restoreFailure);
                }
                try {
                    Files.deleteIfExists(stableBackup);
                } catch (IOException cleanupFailure) {
                    exception.addSuppressed(cleanupFailure);
                }
                throw exception;
            }
            return new SidecarJarReplacement(
                    stagedJar, stableBackup);
        }

        void restoreStableJar() throws CliException {
            stageSidecarJar(stableBackup, stagedJar);
        }

        void commit() throws CliException {
            committed = true;
            try {
                Files.deleteIfExists(stableBackup);
            } catch (IOException exception) {
                throw new CliException(1,
                        "Cannot remove stable sidecar backup "
                                + stableBackup + ": "
                                + exception.getMessage());
            }
        }

        @Override
        public void close() throws CliException {
            if (committed) {
                return;
            }
            CliException failure = null;
            try {
                restoreStableJar();
            } catch (CliException exception) {
                failure = exception;
            }
            try {
                Files.deleteIfExists(stableBackup);
            } catch (IOException exception) {
                if (failure == null) {
                    failure = new CliException(1,
                            "Cannot clean stable sidecar backup "
                                    + stableBackup + ": "
                                    + exception.getMessage());
                } else {
                    failure.addSuppressed(exception);
                }
            }
            if (failure != null) {
                throw failure;
            }
        }
    }

    enum HostControlResultStatus {
        READY("ready"),
        FAILED("failed"),
        ROLLED_BACK("rolledBack"),
        ROLLBACK_FAILED("rollbackFailed");

        private final String wireValue;

        HostControlResultStatus(String wireValue) {
            this.wireValue = wireValue;
        }

        private static HostControlResultStatus parse(
                String value) throws CliException {
            for (HostControlResultStatus status : values()) {
                if (status.wireValue.equals(value)) {
                    return status;
                }
            }
            throw new CliException(1,
                    "Unsupported host-control result status: "
                            + value);
        }
    }

    record HostControlResult(
            String requestId,
            HostControlResultStatus status,
            String cause) {
        static HostControlResult parse(
                String line) throws CliException {
            if (!line.startsWith(HOST_CONTROL_RESULT_PREFIX)) {
                return null;
            }
            String[] fields = line.substring(
                            HOST_CONTROL_RESULT_PREFIX.length())
                    .split(" ", -1);
            if (fields.length != 4
                    || !fields[0].equals("1")
                    || !validHostControlToken(fields[1], 64)
                    || !validHostControlToken(fields[3], 128)) {
                throw new CliException(1,
                        "Invalid host-control result framing.");
            }
            return new HostControlResult(
                    fields[1],
                    HostControlResultStatus.parse(fields[2]),
                    fields[3]);
        }
    }

    private static boolean validHostControlToken(
            String value,
            int maximumLength) {
        if (value.isEmpty() || value.length() > maximumLength) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            boolean asciiDigit =
                    character >= '0' && character <= '9';
            boolean asciiUpper =
                    character >= 'A' && character <= 'Z';
            boolean asciiLower =
                    character >= 'a' && character <= 'z';
            if (!asciiDigit
                    && !asciiUpper
                    && !asciiLower
                    && character != '-'
                    && character != '_') {
                return false;
            }
        }
        return true;
    }

    private static void requireHostControlRequestId(
            String requestId) {
        if (!validHostControlToken(requestId, 64)) {
            throw new IllegalArgumentException(
                    "Invalid host-control request id.");
        }
    }

    private static final class HostProcess implements AutoCloseable {
        private final Process process;
        private final BufferedWriter controlWriter;
        private final BlockingQueue<HostControlResult> controlResults;
        private final AtomicReference<CliException> stdoutFailure;
        private final Thread stdoutPump;
        private final Path stderr;

        private HostProcess(
                Process process,
                BufferedWriter controlWriter,
                BlockingQueue<HostControlResult> controlResults,
                AtomicReference<CliException> stdoutFailure,
                Thread stdoutPump,
                Path stderr) {
            this.process = process;
            this.controlWriter = controlWriter;
            this.controlResults = controlResults;
            this.stdoutFailure = stdoutFailure;
            this.stdoutPump = stdoutPump;
            this.stderr = stderr;
        }

        static HostProcess start(
                Path app,
                Path logDirectory,
                Map<String, String> environment,
                PrintStream out) throws CliException {
            Path stdout = logDirectory.resolve("host.out.log");
            Path stderr = logDirectory.resolve("host.err.log");
            Path executable = app.resolve(
                    "Contents/MacOS/VaadinDesktopHost");
            if (!Files.isExecutable(executable)) {
                throw new CliException(1,
                        "macOS host executable is missing: "
                                + executable);
            }
            try {
                Files.createDirectories(logDirectory);
                Files.writeString(stdout, "");
                Files.writeString(stderr, "");
            } catch (IOException exception) {
                throw new CliException(1,
                        "Cannot create host log directory: "
                                + exception.getMessage());
            }
            out.println("Vaadin Desktop Host logs:");
            out.println("  stdout: " + stdout);
            out.println("  stderr: " + stderr);
            ProcessBuilder builder = new ProcessBuilder(
                    executable.toString())
                    .directory(app.getParent().toFile())
                    .redirectError(stderr.toFile());
            builder.environment().putAll(environment);
            Process process;
            try {
                process = builder.start();
            } catch (IOException exception) {
                throw new CliException(1,
                        "Cannot execute macOS host " + executable + ": "
                                + exception.getMessage());
            }
            BlockingQueue<HostControlResult> controlResults =
                    new LinkedBlockingQueue<>();
            AtomicReference<CliException> stdoutFailure =
                    new AtomicReference<>();
            Thread stdoutPump = Thread.ofPlatform()
                    .name("vatori-host-stdout")
                    .daemon(true)
                    .unstarted(() -> pumpHostStdout(
                            process,
                            stdout,
                            controlResults,
                            stdoutFailure));
            stdoutPump.start();
            return new HostProcess(
                    process,
                    new BufferedWriter(new OutputStreamWriter(
                            process.getOutputStream(),
                            StandardCharsets.UTF_8)),
                    controlResults,
                    stdoutFailure,
                    stdoutPump,
                    stderr);
        }

        private static void pumpHostStdout(
                Process process,
                Path stdout,
                BlockingQueue<HostControlResult> controlResults,
                AtomicReference<CliException> failure) {
            try (BufferedReader reader = new BufferedReader(
                         new InputStreamReader(
                                 process.getInputStream(),
                                 StandardCharsets.UTF_8));
                 BufferedWriter writer = Files.newBufferedWriter(
                         stdout,
                         StandardCharsets.UTF_8,
                         StandardOpenOption.TRUNCATE_EXISTING,
                         StandardOpenOption.WRITE)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    writer.write(line);
                    writer.newLine();
                    writer.flush();
                    HostControlResult result =
                            HostControlResult.parse(line);
                    if (result != null) {
                        controlResults.add(result);
                    }
                }
            } catch (IOException exception) {
                failure.compareAndSet(null, new CliException(1,
                        "Cannot capture macOS host stdout: "
                                + exception.getMessage()));
            } catch (CliException exception) {
                failure.compareAndSet(null, exception);
            }
        }

        boolean isAlive() {
            return process.isAlive();
        }

        boolean waitFor(Duration duration) throws CliException {
            try {
                return process.waitFor(
                        duration.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException exception) {
                process.destroy();
                Thread.currentThread().interrupt();
                throw new CliException(
                        130, "macOS host launch interrupted.");
            }
        }

        int waitForExit() throws CliException {
            try {
                int status = process.waitFor();
                joinStdoutPump();
                requireStdoutHealthy();
                if (status != 0) {
                    throw new CliException(status,
                            "macOS host exited with code " + status
                                    + ". See " + stderr);
                }
                return status;
            } catch (InterruptedException exception) {
                process.destroy();
                Thread.currentThread().interrupt();
                throw new CliException(
                        130, "macOS host launch interrupted.");
            }
        }

        void requestSidecarReplacement(
                String requestId) throws CliException {
            writeControlLine(
                    sidecarReplacementControlLine(requestId),
                    "replacement");
        }

        void requestSidecarRollback(
                String requestId) throws CliException {
            writeControlLine(
                    sidecarRollbackControlLine(requestId),
                    "rollback");
        }

        private void writeControlLine(
                String line,
                String operation) throws CliException {
            try {
                controlWriter.write(line);
                controlWriter.flush();
            } catch (IOException exception) {
                throw new CliException(1,
                        "Cannot request sidecar " + operation + " from the "
                                + "macOS host: " + exception.getMessage());
            }
        }

        HostControlResult awaitReplacementResult(
                String requestId,
                Duration timeout) throws CliException {
            long deadline = System.nanoTime() + timeout.toNanos();
            while (true) {
                requireStdoutHealthy();
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    throw new CliException(1,
                            "Timed out waiting for sidecar replacement "
                                    + "result " + requestId + ".");
                }
                HostControlResult result;
                try {
                    result = controlResults.poll(
                            Math.min(
                                    TimeUnit.NANOSECONDS.toMillis(
                                            remaining),
                                    HOST_POLL_INTERVAL.toMillis()),
                            TimeUnit.MILLISECONDS);
                } catch (InterruptedException exception) {
                    process.destroy();
                    Thread.currentThread().interrupt();
                    throw new CliException(
                            130,
                            "Interrupted while waiting for sidecar "
                                    + "replacement result.");
                }
                if (result != null) {
                    if (!result.requestId().equals(requestId)) {
                        throw new CliException(1,
                                "Unexpected host-control result for "
                                        + result.requestId()
                                        + " while waiting for "
                                        + requestId + ".");
                    }
                    return result;
                }
                if (!process.isAlive()) {
                    return failedHostControlResult(requestId);
                }
            }
        }

        private HostControlResult failedHostControlResult(
                String requestId) throws CliException {
            HostControlResult result = controlResults.poll();
            if (result != null
                    && result.requestId().equals(requestId)) {
                return result;
            }
            waitForExit();
            throw new CliException(1,
                    "macOS host exited before reporting sidecar "
                            + "replacement result " + requestId + ".");
        }

        private void requireStdoutHealthy()
                throws CliException {
            CliException failure = stdoutFailure.get();
            if (failure != null) {
                throw failure;
            }
        }

        private void joinStdoutPump()
                throws CliException {
            try {
                stdoutPump.join(
                        Duration.ofSeconds(3).toMillis());
                if (stdoutPump.isAlive()) {
                    throw new CliException(1,
                            "Timed out draining macOS host stdout.");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new CliException(
                        130,
                        "Interrupted while draining macOS host stdout.");
            }
        }

        @Override
        public void close() {
            try {
                controlWriter.close();
            } catch (IOException ignored) {
                // Process teardown below is the cleanup boundary.
            }
            if (!process.isAlive()) {
                return;
            }
            process.destroy();
            try {
                if (!process.waitFor(3, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    process.waitFor(3, TimeUnit.SECONDS);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
            try {
                stdoutPump.join(
                        Duration.ofSeconds(3).toMillis());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static final class DevelopmentModelCapture
            implements AutoCloseable {
        private static final String OUTPUT_PROPERTY =
                "vatori.development.model.output";

        private final Path extensionArtifact;
        private final Path output;

        private DevelopmentModelCapture(
                Path extensionArtifact,
                Path output) {
            this.extensionArtifact = extensionArtifact;
            this.output = output;
        }

        static DevelopmentModelCapture create(Path root)
                throws CliException {
            Path artifact;
            try {
                artifact = Path.of(VaadinDesktopCli.class
                                .getProtectionDomain()
                                .getCodeSource()
                                .getLocation()
                                .toURI())
                        .toAbsolutePath().normalize();
            } catch (Exception exception) {
                throw new CliException(1,
                        "Cannot locate the Vaadin Desktop CLI artifact: "
                                + exception.getMessage());
            }
            if (!Files.isRegularFile(artifact)) {
                throw new CliException(1,
                        "dev-vite requires the packaged CLI artifact for "
                                + "Maven model discovery: " + artifact);
            }
            Path directory = root.resolve(
                    ".build/vaadin-desktop-cli");
            try {
                Files.createDirectories(directory);
                Path output = Files.createTempFile(
                        directory,
                        "development-model-",
                        ".txt");
                return new DevelopmentModelCapture(artifact, output);
            } catch (IOException exception) {
                throw new CliException(1,
                        "Cannot prepare Maven development model capture: "
                                + exception.getMessage());
            }
        }

        void configure(List<String> command) {
            command.add("-Dmaven.ext.class.path=" + extensionArtifact);
            command.add("-D" + OUTPUT_PROPERTY + "=" + output);
        }

        DevelopmentInputs read(Path root, Path selectedModule)
                throws CliException {
            return DevelopmentInputs.readEffectiveModel(
                    output, root, selectedModule);
        }

        @Override
        public void close() {
            try {
                Files.deleteIfExists(output);
                Files.deleteIfExists(output.resolveSibling(
                        output.getFileName() + ".tmp"));
            } catch (IOException ignored) {
                // The model is transient and a later run uses a unique path.
            }
        }
    }

    static final class DevelopmentInputs {
        private static final String MODEL_HEADER =
                "VATORI_MAVEN_DEVELOPMENT_MODEL\t1";
        private static final List<String> MAVEN_CONFIGURATION_FILES = List.of(
                "maven.config", "jvm.config", "extensions.xml");

        private final List<Path> moduleDirectories;
        private final Set<Path> directoryRoots;
        private final Set<Path> files;
        private final Set<Path> buildConfigurationFiles;

        private DevelopmentInputs(
                List<Path> moduleDirectories,
                Set<Path> directoryRoots,
                Set<Path> files,
                Set<Path> buildConfigurationFiles) {
            this.moduleDirectories = List.copyOf(moduleDirectories);
            this.directoryRoots = Set.copyOf(directoryRoots);
            this.files = Set.copyOf(files);
            this.buildConfigurationFiles =
                    Set.copyOf(buildConfigurationFiles);
        }

        static DevelopmentInputs readEffectiveModel(
                Path model,
                Path root,
                Path selectedModule)
                throws CliException {
            Path normalizedRoot =
                    root.toAbsolutePath().normalize();
            Path normalizedSelected =
                    selectedModule.toAbsolutePath().normalize();
            LinkedHashSet<Path> modules = new LinkedHashSet<>();
            LinkedHashSet<Path> directories = new LinkedHashSet<>();
            LinkedHashSet<Path> inputFiles = new LinkedHashSet<>();
            LinkedHashSet<Path> buildFiles = new LinkedHashSet<>();
            List<String> lines;
            try {
                lines = Files.readAllLines(
                        model, StandardCharsets.UTF_8);
            } catch (IOException exception) {
                throw new CliException(1,
                        "Cannot read Maven development model " + model
                                + ": " + exception.getMessage());
            }
            if (lines.isEmpty() || !MODEL_HEADER.equals(lines.getFirst())) {
                throw new CliException(1,
                        "Maven did not export a supported development model: "
                                + model);
            }
            for (int index = 1; index < lines.size(); index++) {
                String line = lines.get(index);
                int separator = line.indexOf('\t');
                if (separator <= 0 || separator == line.length() - 1) {
                    throw new CliException(1,
                            "Malformed Maven development model entry at line "
                                    + (index + 1));
                }
                String type = line.substring(0, separator);
                Path path = decodeModelPath(
                        line.substring(separator + 1), index + 1);
                switch (type) {
                    case "module" -> modules.add(path);
                    case "root" -> directories.add(path);
                    case "pom" -> {
                        if (path.startsWith(normalizedRoot)) {
                            inputFiles.add(path);
                            buildFiles.add(path);
                        }
                    }
                    default -> throw new CliException(1,
                            "Unknown Maven development model entry '"
                                    + type + "' at line " + (index + 1));
                }
            }
            if (!modules.contains(normalizedSelected)) {
                throw new CliException(1,
                        "Maven's selected reactor does not contain the "
                                + "application module: " + normalizedSelected);
            }
            Path rootPom = normalizedRoot.resolve("pom.xml");
            inputFiles.add(rootPom);
            buildFiles.add(rootPom);
            for (String name : MAVEN_CONFIGURATION_FILES) {
                Path configuration = normalizedRoot
                        .resolve(".mvn")
                        .resolve(name)
                        .normalize();
                inputFiles.add(configuration);
                buildFiles.add(configuration);
            }
            return new DevelopmentInputs(
                    modules.stream()
                            .sorted(Comparator.comparing(Path::toString))
                            .toList(),
                    directories,
                    inputFiles,
                    buildFiles);
        }

        List<Path> moduleDirectories() {
            return moduleDirectories;
        }

        Set<Path> directoryRoots() {
            return directoryRoots;
        }

        Set<Path> files() {
            return files;
        }

        boolean includes(Path path) {
            Path normalized = path.toAbsolutePath().normalize();
            return files.contains(normalized)
                    || directoryRoots.stream()
                    .anyMatch(normalized::startsWith);
        }

        boolean isBuildConfiguration(Path path) {
            return buildConfigurationFiles.contains(
                    path.toAbsolutePath().normalize());
        }

        private static Path decodeModelPath(
                String encoded,
                int lineNumber) throws CliException {
            try {
                byte[] bytes = Base64.getUrlDecoder().decode(encoded);
                return Path.of(new String(bytes, StandardCharsets.UTF_8))
                        .toAbsolutePath().normalize();
            } catch (IllegalArgumentException exception) {
                throw new CliException(1,
                        "Malformed path in Maven development model at line "
                                + lineNumber);
            }
        }
    }

    private record DevelopmentInputChange(
            boolean changed,
            boolean buildConfigurationChanged) {
        private static DevelopmentInputChange none() {
            return new DevelopmentInputChange(false, false);
        }

        private DevelopmentInputChange merge(
                DevelopmentInputChange other) {
            return new DevelopmentInputChange(
                    changed || other.changed,
                    buildConfigurationChanged
                            || other.buildConfigurationChanged);
        }
    }

    private static final class DevelopmentInputWatcher
            implements AutoCloseable {
        private final WatchService watchService;
        private final Set<Path> registeredDirectories =
                new HashSet<>();
        private DevelopmentInputs inputs;

        private DevelopmentInputWatcher(
                WatchService watchService,
                DevelopmentInputs inputs) {
            this.watchService = watchService;
            this.inputs = inputs;
        }

        static DevelopmentInputWatcher watch(
                DevelopmentInputs inputs) throws CliException {
            try {
                WatchService watchService =
                        inputs.moduleDirectories().getFirst()
                                .getFileSystem()
                                .newWatchService();
                DevelopmentInputWatcher watcher =
                        new DevelopmentInputWatcher(
                                watchService, inputs);
                watcher.registerInputs();
                return watcher;
            } catch (IOException exception) {
                throw new CliException(1,
                        "Cannot watch development inputs: "
                                + exception.getMessage());
            }
        }

        boolean isActive() {
            return !inputs.directoryRoots().isEmpty()
                    || !inputs.files().isEmpty();
        }

        void refresh(DevelopmentInputs refreshed)
                throws CliException {
            inputs = refreshed;
            registerInputs();
        }

        DevelopmentInputChange pollChanges()
                throws CliException {
            boolean changed = false;
            boolean buildConfigurationChanged = false;
            WatchKey key;
            while ((key = watchService.poll()) != null) {
                Path directory = (Path) key.watchable();
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind()
                            == StandardWatchEventKinds.OVERFLOW) {
                        changed = true;
                        buildConfigurationChanged = true;
                        continue;
                    }
                    Path relative = (Path) event.context();
                    Path path = directory.resolve(relative)
                            .toAbsolutePath().normalize();
                    if (event.kind()
                            == StandardWatchEventKinds.ENTRY_CREATE
                            && Files.isDirectory(path)
                            && containsOrPrecedesInput(path)) {
                        registerRecursively(path);
                    }
                    if (inputs.includes(path)) {
                        changed = true;
                        buildConfigurationChanged |=
                                inputs.isBuildConfiguration(path);
                    }
                }
                if (!key.reset()) {
                    registeredDirectories.remove(
                            directory.toAbsolutePath().normalize());
                }
            }
            return new DevelopmentInputChange(
                    changed, buildConfigurationChanged);
        }

        private void registerInputs() throws CliException {
            for (Path directory : inputs.directoryRoots()) {
                registerNearestExisting(directory.getParent());
                if (Files.isDirectory(directory)) {
                    registerRecursively(directory);
                }
            }
            for (Path file : inputs.files()) {
                registerNearestExisting(file.getParent());
            }
        }

        private boolean containsOrPrecedesInput(Path directory) {
            return inputs.directoryRoots().stream().anyMatch(root ->
                    root.startsWith(directory)
                            || directory.startsWith(root))
                    || inputs.files().stream().anyMatch(file ->
                    file.startsWith(directory));
        }

        private void registerNearestExisting(Path start)
                throws CliException {
            Path directory = start;
            while (directory != null
                    && !Files.isDirectory(directory)) {
                directory = directory.getParent();
            }
            if (directory != null) {
                registerDirectory(directory);
            }
        }

        private void registerRecursively(Path root) throws CliException {
            try (var paths = Files.walk(root)) {
                for (Path path : paths
                        .filter(Files::isDirectory)
                        .toList()) {
                    registerDirectory(path);
                }
            } catch (IOException exception) {
                throw new CliException(1,
                        "Cannot register development input directory "
                                + root + ": " + exception.getMessage());
            }
        }

        private void registerDirectory(Path directory)
                throws CliException {
            Path normalized =
                    directory.toAbsolutePath().normalize();
            if (!registeredDirectories.add(normalized)) {
                return;
            }
            try {
                normalized.register(
                        watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_DELETE);
            } catch (IOException exception) {
                registeredDirectories.remove(normalized);
                throw new CliException(1,
                        "Cannot watch development input directory "
                                + normalized + ": "
                                + exception.getMessage());
            }
        }

        @Override
        public void close() {
            try {
                watchService.close();
            } catch (IOException ignored) {
                // Watch service cleanup has no user-visible state.
            }
        }
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
                    generated(module.resolve(
                            "src/main/frontend/generated/vite-devmode.ts")),
                    generated(module.resolve(
                            "src/main/frontend/generated/jar-resources/vaadin-dev-tools")),
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

    private record DevelopmentWebApp(String origin, String entryPath) {
    }

    private static final class ViteDevelopmentServer implements AutoCloseable {
        private static final Duration START_TIMEOUT = Duration.ofSeconds(30);

        private final Process process;
        private final String origin;

        private ViteDevelopmentServer(Process process, String origin) {
            this.process = process;
            this.origin = origin;
        }

        static ViteDevelopmentServer start(
                Path module,
                Map<String, String> environment,
                PrintStream out) throws CliException {
            int port = freeLoopbackPort();
            String origin = "http://127.0.0.1:" + port;
            String node = findNodeExecutable(environment);
            List<String> command = List.of(
                    node,
                    module.resolve("node_modules/vite/bin/vite.js").toString(),
                    "--config", module.resolve("vite.config.ts").toString(),
                    "--host", "127.0.0.1",
                    "--port", Integer.toString(port),
                    "--strictPort",
                    "--base", "/");
            ProcessBuilder builder = new ProcessBuilder(command)
                    .directory(module.toFile())
                    .inheritIO();
            builder.environment().putAll(environment);
            Process process;
            try {
                process = builder.start();
            } catch (IOException exception) {
                throw new CliException(1,
                        "Cannot start Vite: " + exception.getMessage());
            }
            ViteDevelopmentServer server =
                    new ViteDevelopmentServer(process, origin);
            try {
                server.awaitReady();
            } catch (CliException exception) {
                server.close();
                throw exception;
            }
            out.println("Vaadin Vite server: " + origin);
            return server;
        }

        String origin() {
            return origin;
        }

        private void awaitReady() throws CliException {
            long deadline = System.nanoTime() + START_TIMEOUT.toNanos();
            while (System.nanoTime() < deadline) {
                if (!process.isAlive()) {
                    throw new CliException(process.exitValue(),
                            "Vite exited before becoming ready");
                }
                try {
                    HttpURLConnection connection = (HttpURLConnection) URI
                            .create(origin + "/index.html")
                            .toURL()
                            .openConnection();
                    connection.setConnectTimeout(250);
                    connection.setReadTimeout(250);
                    connection.setUseCaches(false);
                    int status = connection.getResponseCode();
                    connection.disconnect();
                    if (status >= 200 && status < 300) {
                        return;
                    }
                } catch (IOException ignored) {
                    // Vite has not bound the loopback listener yet.
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new CliException(130,
                            "Interrupted while waiting for Vite");
                }
            }
            throw new CliException(1,
                    "Vite did not become ready within "
                            + START_TIMEOUT.toSeconds() + " seconds");
        }

        @Override
        public void close() {
            if (!process.isAlive()) {
                return;
            }
            process.destroy();
            try {
                if (!process.waitFor(3, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    process.waitFor(3, TimeUnit.SECONDS);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }

        private static int freeLoopbackPort() throws CliException {
            try (ServerSocket socket = new ServerSocket(0, 1,
                    java.net.InetAddress.getLoopbackAddress())) {
                return socket.getLocalPort();
            } catch (IOException exception) {
                throw new CliException(1,
                        "Cannot reserve a loopback port for Vite: "
                                + exception.getMessage());
            }
        }

        private static String findNodeExecutable(
                Map<String, String> environment) {
            String configured = environment.get("NODE_EXECUTABLE");
            if (configured != null && !configured.isBlank()) {
                return configured;
            }
            Path vaadinHome = Path.of(
                    System.getProperty("user.home"), ".vaadin");
            if (Files.isDirectory(vaadinHome)) {
                try (var entries = Files.list(vaadinHome)) {
                    var candidates = entries
                            .filter(path -> path.getFileName().toString()
                                    .startsWith("node-v"))
                            .map(path -> path.resolve("bin/node"))
                            .filter(Files::isExecutable)
                            .sorted(Comparator.comparing(Path::toString).reversed())
                            .toList();
                    if (!candidates.isEmpty()) {
                        return candidates.getFirst().toString();
                    }
                } catch (IOException ignored) {
                    // Fall back to PATH.
                }
            }
            return "node";
        }
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
