package dev.vatori.desktop.cli;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public final class VaadinDesktopCliSmokeMain {
    private VaadinDesktopCliSmokeMain() {
    }

    public static void main(String[] args) throws Exception {
        Path target = Files.createTempDirectory("vatori-cli-jars");
        try {
            expectJarSelectionFailure(target, "zero runnable JARs");
            Path first = target.resolve("application.jar");
            writeJar(first, "example.Main");
            if (!VaadinDesktopCli.findRunnableJar(target).equals(first)) {
                throw new IllegalStateException("CLI selected the wrong runnable JAR");
            }
            writeJar(target.resolve("plain.jar"), null);
            if (!VaadinDesktopCli.findRunnableJar(target).equals(first)) {
                throw new IllegalStateException("CLI treated a plain JAR as runnable");
            }
            writeJar(target.resolve("second.jar"), "example.SecondMain");
            expectJarSelectionFailure(target, "multiple runnable JARs");
            verifyVitePomWithoutConfiguredPlugin(target);
            verifyCapabilityManifestFallbackModes();
            verifyCapabilityManifestBuilder(target);
            verifyApplicationBundleInputs(target);
            verifyReactorDevelopmentInputs(target);
            verifySidecarStaging(target);
            verifySidecarReplacementTransaction(target);
            verifySidecarReplacementControl();
            verifyHostExitPropagation(target);
        } finally {
            try (var paths = Files.walk(target)) {
                for (Path path : paths.sorted((left, right) -> right.compareTo(left)).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int helpStatus = VaadinDesktopCli.run(
                new String[]{"help"}, Map.of(),
                new PrintStream(output, true, StandardCharsets.UTF_8), System.err);
        String help = output.toString(StandardCharsets.UTF_8);
        if (helpStatus != 0 || !help.contains("dev-vite")
                || !help.contains("opaque IPC")) {
            throw new IllegalStateException("CLI help contract is incomplete");
        }
        int unknownStatus = VaadinDesktopCli.run(
                new String[]{"unknown"}, Map.of(), System.out, System.err);
        if (unknownStatus == 0) {
            throw new IllegalStateException("CLI accepted an unknown command");
        }
        System.out.println("Vaadin Desktop CLI contract passed");
    }

    private static void verifySidecarStaging(Path directory) throws Exception {
        Path source = directory.resolve("rebuilt-sidecar.jar");
        Path destination = directory.resolve(
                "FakeHost.app/Contents/Resources/Sidecar/application.jar");
        Files.writeString(source, "rebuilt");
        Files.createDirectories(destination.getParent());
        Files.writeString(destination, "baseline");

        VaadinDesktopCli.stageSidecarJar(source, destination);

        boolean temporaryRemains;
        try (var paths = Files.list(destination.getParent())) {
            temporaryRemains = paths.anyMatch(path ->
                    path.getFileName().toString().endsWith(".jar.tmp"));
        }
        if (!"rebuilt".equals(Files.readString(destination))
                || temporaryRemains) {
            throw new IllegalStateException(
                    "CLI did not atomically replace the staged sidecar JAR");
        }
    }

    private static void verifyCapabilityManifestFallbackModes() {
        for (String command : List.of("dev", "dev-vite", "dev-compat")) {
            if (!"allow-all-dev".equals(
                    VaadinDesktopCli.capabilityManifestFallbackMode(command))) {
                throw new IllegalStateException(
                        "CLI development command is not explicitly permissive: "
                                + command);
            }
        }
        if (!"manifest-enforced".equals(
                VaadinDesktopCli.capabilityManifestFallbackMode("package"))) {
            throw new IllegalStateException(
                    "CLI package command is not fail-closed");
        }
        try {
            VaadinDesktopCli.capabilityManifestFallbackMode("unknown");
            throw new IllegalStateException(
                    "CLI accepted an unknown capability manifest policy command");
        } catch (IllegalArgumentException expected) {
            // Only supported CLI commands may select a fallback policy.
        }
    }

    private static void verifyCapabilityManifestBuilder(Path directory)
            throws Exception {
        Path repository = Path.of(System.getProperty(
                "vatori.repo.root", "")).toAbsolutePath().normalize();
        Path builder = repository.resolve(
                "bin/vaadin-desktop-build-macos-host");
        if (!Files.isRegularFile(builder)) {
            throw new IllegalStateException(
                    "macOS host builder is unavailable: " + builder);
        }

        Path root = directory.resolve("builder-root");
        Path source = root.resolve(
                "native-hosts/macos/Sources/VaadinDesktopHost/main.swift");
        Path resources = root.resolve("native-hosts/macos/Resources");
        Path icon = root.resolve("assets/brand/vatori-icon.icns");
        Path applicationJar = root.resolve("application.jar");
        Path webApp = root.resolve("webapp");
        Files.createDirectories(source.getParent());
        Files.createDirectories(resources);
        Files.createDirectories(icon.getParent());
        Files.createDirectories(webApp);
        Files.writeString(source, "print(\"builder smoke\")\n");
        Files.writeString(resources.resolve("FlowIpcOpaqueHttpBridge.js"), "");
        Files.writeString(resources.resolve("FlowIpcPushBridge.js"), "");
        Files.write(icon, new byte[]{'i', 'c', 'n', 's'});
        Files.writeString(applicationJar, "sidecar");
        Files.writeString(webApp.resolve("index.html"), "<!doctype html>\n");

        runBuilder(builder, root, applicationJar, webApp, "", null, true);
        Path stagedManifest = root.resolve(
                ".build/native-hosts/macos/VaadinDesktopHost.app/Contents/Resources/desktop-capabilities.json");
        assertManifest(stagedManifest,
                "{\"schemaVersion\":1,\"permissionMode\":\"manifest-enforced\",\"windows\":[]}\n",
                "secure builder default");

        runBuilder(builder, root, applicationJar, webApp, "",
                "allow-all-dev", true);
        assertManifest(stagedManifest,
                "{\"schemaVersion\":1,\"permissionMode\":\"allow-all-dev\",\"windows\":[]}\n",
                "explicit development fallback");

        Path explicitManifest = root.resolve("explicit-capabilities.json");
        String enforced = """
                {
                  "schemaVersion": 1,
                  "permissionMode": "manifest-enforced",
                  "windows": [{"label":"main","commands":["window.setTitle"]}]
                }
                """;
        Files.writeString(explicitManifest, enforced);
        runBuilder(builder, root, applicationJar, webApp,
                explicitManifest.toString(), "manifest-enforced", true);
        assertCanonicalProductManifest(stagedManifest,
                "explicit enforced manifest", true);

        String allowAllDevelopment = """
                {
                  "schemaVersion": 1,
                  "permissionMode": "allow-all-dev",
                  "windows": []
                }
                """;
        Files.writeString(explicitManifest, allowAllDevelopment);
        runBuilder(builder, root, applicationJar, webApp,
                explicitManifest.toString(), "allow-all-dev", true);
        assertManifest(stagedManifest, allowAllDevelopment,
                "explicit allow-all development manifest copy");

        String traceOnlyDevelopment = """
                {
                  "schemaVersion": 1,
                  "permissionMode": "trace-only-dry-run",
                  "windows": []
                }
                """;
        Files.writeString(explicitManifest, traceOnlyDevelopment);
        runBuilder(builder, root, applicationJar, webApp,
                explicitManifest.toString(), "allow-all-dev", true);
        assertManifest(stagedManifest, traceOnlyDevelopment,
                "explicit trace-only development manifest copy");

        String duplicatePermissionMode = """
                {
                  "schemaVersion": 1,
                  "permissionMode": "allow-all-dev",
                  "permissionMode": "manifest-enforced",
                  "windows": []
                }
                """;
        Files.writeString(explicitManifest, duplicatePermissionMode);
        runBuilder(builder, root, applicationJar, webApp,
                explicitManifest.toString(), "manifest-enforced", true);
        assertCanonicalProductManifest(stagedManifest,
                "duplicate-key product manifest", false);
        String productBaseline = Files.readString(stagedManifest);
        Path preservationMarker = stagedManifest.resolveSibling(
                "policy-preservation.marker");
        Files.writeString(preservationMarker, "preserve");

        assertProductManifestRejected(builder, root,
                applicationJar, webApp, explicitManifest, stagedManifest,
                preservationMarker, allowAllDevelopment,
                "Product capability manifest must use "
                        + "permissionMode=manifest-enforced; found: "
                        + "allow-all-dev",
                productBaseline);
        assertProductManifestRejected(builder, root,
                applicationJar, webApp, explicitManifest, stagedManifest,
                preservationMarker, traceOnlyDevelopment,
                "Product capability manifest must use "
                        + "permissionMode=manifest-enforced; found: "
                        + "trace-only-dry-run",
                productBaseline);
        assertProductManifestRejected(builder, root,
                applicationJar, webApp, explicitManifest, stagedManifest,
                preservationMarker, "{\"schemaVersion\":1,",
                "Product capability manifest is not valid JSON",
                productBaseline);
        assertProductManifestRejected(builder, root,
                applicationJar, webApp, explicitManifest, stagedManifest,
                preservationMarker,
                "<?xml version=\"1.0\"?><plist version=\"1.0\">"
                        + "<dict><key>schemaVersion</key><integer>1</integer>"
                        + "<key>permissionMode</key><string>manifest-enforced"
                        + "</string><key>windows</key><array/></dict></plist>",
                "Product capability manifest is not valid JSON",
                productBaseline);
        assertProductManifestRejected(builder, root,
                applicationJar, webApp, explicitManifest, stagedManifest,
                preservationMarker,
                "{\"schemaVersion\":1,\"windows\":[]}",
                "Product capability manifest must define "
                        + "permissionMode=manifest-enforced",
                productBaseline);
        assertProductManifestRejected(builder, root,
                applicationJar, webApp, explicitManifest, stagedManifest,
                preservationMarker,
                "{\"schemaVersion\":1,\"permissionMode\":"
                        + "\"unknown\",\"windows\":[]}",
                "Product capability manifest must use "
                        + "permissionMode=manifest-enforced",
                productBaseline);
        assertProductManifestRejected(builder, root,
                applicationJar, webApp, explicitManifest, stagedManifest,
                preservationMarker,
                "{\"schemaVersion\":1,\"permissionMode\":"
                        + "\"manifest-enforced\\n\",\"windows\":[]}",
                "Product capability manifest must use "
                        + "permissionMode=manifest-enforced",
                productBaseline);

        ProcessResult invalid = runBuilder(builder, root, applicationJar,
                webApp, "", "invalid-mode", false);
        if (invalid.status() != 2
                || !invalid.output().contains(
                "Unsupported capability manifest fallback mode: invalid-mode")) {
            throw new IllegalStateException(
                    "Builder accepted an invalid capability fallback mode: "
                            + invalid);
        }
    }

    private static void assertProductManifestRejected(
            Path builder, Path root, Path applicationJar, Path webApp,
            Path explicitManifest, Path stagedManifest,
            Path preservationMarker, String manifest,
            String expectedDiagnostic, String expectedStagedManifest)
            throws Exception {
        Files.writeString(explicitManifest, manifest);
        ProcessResult rejected = runBuilder(builder, root, applicationJar,
                webApp, explicitManifest.toString(), "manifest-enforced",
                false);
        String expectedOutput = expectedDiagnostic + System.lineSeparator();
        if (rejected.status() != 2
                || !expectedOutput.equals(rejected.output())) {
            throw new IllegalStateException("Builder accepted an invalid "
                    + "product manifest: " + rejected);
        }
        assertManifest(stagedManifest, expectedStagedManifest,
                "product manifest after rejection");
        if (!Files.isRegularFile(preservationMarker)
                || !"preserve".equals(Files.readString(preservationMarker))) {
            throw new IllegalStateException(
                    "Builder replaced the staged app before rejecting a "
                            + "product manifest");
        }
    }

    private static void assertCanonicalProductManifest(
            Path stagedManifest, String scenario,
            boolean expectWindowSetTitle) throws Exception {
        String actual = Files.readString(stagedManifest);
        if (occurrences(actual, "\"permissionMode\"") != 1
                || !actual.contains("\"permissionMode\":\"manifest-enforced\"")
                || !actual.contains("\"schemaVersion\":1")
                || !actual.contains("\"windows\":[")
                || actual.contains("allow-all-dev")
                || actual.contains("trace-only-dry-run")
                || expectWindowSetTitle != actual.contains("window.setTitle")) {
            throw new IllegalStateException(
                    "Builder did not canonicalize " + scenario + ": "
                            + actual);
        }
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }

    private static ProcessResult runBuilder(Path builder, Path root,
            Path applicationJar, Path webApp, String capabilityManifest,
            String fallbackMode, boolean expectSuccess) throws Exception {
        List<String> command = new java.util.ArrayList<>(List.of(
                "bash", builder.toString(), root.toString(), "flow-ipc",
                applicationJar.toString(), webApp.toString()));
        if (!capabilityManifest.isEmpty() || fallbackMode != null) {
            command.add(capabilityManifest);
        }
        if (fallbackMode != null) {
            command.addAll(List.of("", "", "", "", "", fallbackMode));
        }
        Path temporaryRoot = root.resolve("builder-temp");
        Files.createDirectories(temporaryRoot);
        ProcessBuilder processBuilder = new ProcessBuilder(command)
                .redirectErrorStream(true);
        processBuilder.environment().put("TMPDIR", temporaryRoot.toString());
        Process process = processBuilder.start();
        String output = new String(
                process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int status = process.waitFor();
        if (expectSuccess && status != 0) {
            throw new IllegalStateException(
                    "macOS host builder failed: " + output);
        }
        try (var paths = Files.list(temporaryRoot)) {
            if (paths.anyMatch(path -> path.getFileName().toString()
                    .startsWith("vatori-capability-manifest."))) {
                throw new IllegalStateException(
                        "Builder leaked capability manifest snapshot state");
            }
        }
        return new ProcessResult(status, output);
    }

    private static void assertManifest(Path manifest, String expected,
            String scenario) throws Exception {
        String actual = Files.readString(manifest);
        if (!expected.equals(actual)) {
            throw new IllegalStateException(
                    "Builder did not preserve " + scenario + ": " + actual);
        }
    }

    private record ProcessResult(int status, String output) {
    }

    private static void verifyApplicationBundleInputs(Path directory)
            throws Exception {
        Path module = directory.resolve("bundle-inputs-app");
        Path desktop = module.resolve("src/main/desktop");
        Path icon = desktop.resolve("macos/AppIcon.icns");
        Path commonResource = desktop.resolve("resources/common.txt");
        Path macosResource = desktop.resolve("macos/resources/platform.txt");
        Files.createDirectories(icon.getParent());
        Files.createDirectories(commonResource.getParent());
        Files.createDirectories(macosResource.getParent());
        Files.writeString(icon, "test-icon");
        Files.writeString(commonResource, "common");
        Files.writeString(macosResource, "macos");

        ApplicationBundleInputs inputs = ApplicationBundleInputs.discover(module);
        if (!icon.toAbsolutePath().normalize().equals(inputs.macosIcon())
                || !commonResource.getParent().toAbsolutePath().normalize()
                .equals(inputs.commonResources())
                || !macosResource.getParent().toAbsolutePath().normalize()
                .equals(inputs.macosResources())
                || inputs.macosBuilderArguments().stream().anyMatch(String::isBlank)) {
            throw new IllegalStateException(
                    "CLI did not discover application-owned bundle inputs");
        }

        Path defaultModule = directory.resolve("default-bundle-inputs-app");
        ApplicationBundleInputs defaults =
                ApplicationBundleInputs.discover(defaultModule);
        if (defaults.macosIcon() != null
                || defaults.commonResources() != null
                || defaults.macosResources() != null
                || defaults.macosBuilderArguments().stream()
                .anyMatch(value -> !value.isEmpty())) {
            throw new IllegalStateException(
                    "CLI did not preserve default bundle resource fallback");
        }
    }

    private static void verifyReactorDevelopmentInputs(Path directory)
            throws Exception {
        Path root = directory.resolve("reactor");
        Path app = root.resolve("app");
        Path shared = root.resolve("shared");
        Path profileShared = root.resolve("profile-shared");
        Path unrelated = root.resolve("unrelated");
        Path appSources = app.resolve("src/main/java");
        Path sharedResources = shared.resolve("src/main/resources");
        Path customSources = profileShared.resolve("sources/java");
        Path customResources = profileShared.resolve("resources/main");
        Files.createDirectories(appSources);
        Files.createDirectories(sharedResources);
        Files.createDirectories(customSources);
        Files.createDirectories(customResources);
        Files.createDirectories(unrelated.resolve("src/main/java"));
        for (Path pom : List.of(
                root.resolve("pom.xml"),
                app.resolve("pom.xml"),
                shared.resolve("pom.xml"),
                profileShared.resolve("pom.xml"))) {
            Files.createDirectories(pom.getParent());
            Files.writeString(pom, "<project/>");
        }
        Path model = root.resolve("effective-model.txt");
        Files.write(model, List.of(
                "VATORI_MAVEN_DEVELOPMENT_MODEL\t1",
                modelEntry("module", app),
                modelEntry("module", shared),
                modelEntry("module", profileShared),
                modelEntry("root", appSources),
                modelEntry("root", sharedResources),
                modelEntry("root", customSources),
                modelEntry("root", customResources),
                modelEntry("pom", root.resolve("pom.xml")),
                modelEntry("pom", app.resolve("pom.xml")),
                modelEntry("pom", shared.resolve("pom.xml")),
                modelEntry("pom", profileShared.resolve("pom.xml"))));

        VaadinDesktopCli.DevelopmentInputs inputs =
                VaadinDesktopCli.DevelopmentInputs.readEffectiveModel(
                        model, root, app);

        if (inputs.moduleDirectories().size() != 3
                || !inputs.moduleDirectories().contains(app)
                || !inputs.moduleDirectories().contains(shared)
                || !inputs.moduleDirectories().contains(profileShared)
                || inputs.moduleDirectories().contains(unrelated)
                || !inputs.includes(app.resolve(
                        "src/main/java/example/App.java"))
                || !inputs.includes(shared.resolve(
                        "src/main/resources/probe.txt"))
                || !inputs.includes(customSources.resolve(
                        "example/ProfileValue.java"))
                || !inputs.includes(customResources.resolve(
                        "profile.txt"))
                || !inputs.includes(profileShared.resolve("pom.xml"))
                || !inputs.includes(root.resolve("pom.xml"))
                || !inputs.includes(root.resolve(
                        ".mvn/maven.config"))
                || inputs.includes(unrelated.resolve(
                        "src/main/java/example/Unrelated.java"))) {
            throw new IllegalStateException(
                    "CLI reactor development inputs are incomplete: "
                            + inputs.moduleDirectories());
        }
    }

    private static String modelEntry(String type, Path path) {
        String encoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(path.toAbsolutePath().normalize().toString()
                        .getBytes(StandardCharsets.UTF_8));
        return type + "\t" + encoded;
    }

    private static void verifySidecarReplacementControl()
            throws Exception {
        String requestId = "reload-request-1";
        String line = VaadinDesktopCli.sidecarReplacementControlLine(
                requestId);
        if (!line.startsWith("VATORI_HOST_CONTROL ")
                || !line.contains("\"version\":1")
                || !line.contains("\"type\":\"replaceSidecar\"")
                || !line.contains("\"requestId\":\"" + requestId + "\"")
                || !line.contains(
                "\"reason\":\"development-inputs-rebuilt\"")
                || !line.endsWith("\n")) {
            throw new IllegalStateException(
                    "CLI host-control envelope is not versioned and framed");
        }
        String rollback = VaadinDesktopCli.sidecarRollbackControlLine(
                requestId);
        if (!rollback.contains("\"type\":\"rollbackSidecar\"")
                || !rollback.contains(
                        "\"requestId\":\"" + requestId + "\"")) {
            throw new IllegalStateException(
                    "CLI rollback control envelope is incomplete");
        }
        VaadinDesktopCli.HostControlResult result =
                VaadinDesktopCli.HostControlResult.parse(
                        "VATORI_HOST_CONTROL_RESULT 1 "
                                + requestId
                                + " rolledBack stable-sidecar-ready");
        if (!requestId.equals(result.requestId())
                || result.status()
                != VaadinDesktopCli.HostControlResultStatus.ROLLED_BACK
                || !"stable-sidecar-ready".equals(result.cause())) {
            throw new IllegalStateException(
                    "CLI did not parse the host replacement result");
        }
    }

    private static void verifySidecarReplacementTransaction(
            Path directory) throws Exception {
        Path source = directory.resolve(
                "transaction-candidate.jar");
        Path destination = directory.resolve(
                "TransactionHost.app/Contents/Resources/Sidecar/application.jar");
        Files.writeString(source, "candidate");
        Files.createDirectories(destination.getParent());
        Files.writeString(destination, "stable");

        try (VaadinDesktopCli.SidecarJarReplacement replacement =
                     VaadinDesktopCli.SidecarJarReplacement.prepare(
                             source, destination)) {
            if (!"candidate".equals(Files.readString(destination))) {
                throw new IllegalStateException(
                        "CLI did not stage the replacement candidate");
            }
            replacement.restoreStableJar();
            if (!"stable".equals(Files.readString(destination))) {
                throw new IllegalStateException(
                        "CLI did not restore the stable sidecar JAR");
            }
            replacement.commit();
        }
        if (Files.exists(destination.resolveSibling(
                ".application-stable.jar"))) {
            throw new IllegalStateException(
                    "CLI left the stable sidecar backup behind");
        }
    }

    private static void expectJarSelectionFailure(Path target, String scenario) {
        try {
            VaadinDesktopCli.findRunnableJar(target);
            throw new IllegalStateException("CLI accepted " + scenario);
        } catch (VaadinDesktopCli.CliException expected) {
            // Expected strict unique runnable JAR contract.
        }
    }

    private static void writeJar(Path path, String mainClass) throws Exception {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        if (mainClass != null) {
            manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, mainClass);
        }
        try (JarOutputStream ignored = new JarOutputStream(
                Files.newOutputStream(path), manifest)) {
            // Manifest-only fixtures are sufficient for runnable JAR selection.
        }
    }

    private static void verifyVitePomWithoutConfiguredPlugin(Path directory)
            throws Exception {
        Path source = directory.resolve("source-pom.xml");
        Path generated = directory.resolve("generated-pom.xml");
        Files.writeString(source, """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>example</groupId>
                  <artifactId>app</artifactId>
                  <version>1</version>
                </project>
                """);
        VaadinDesktopCli.writeVitePom(source, generated);

        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        var document = factory.newDocumentBuilder().parse(generated.toFile());
        Element plugin = findCoordinates(
                document.getElementsByTagNameNS("*", "plugin"),
                "com.vaadin", "vaadin-maven-plugin");
        if (plugin == null
                || !"false".equals(childText(plugin, "generateBundle"))
                || !"false".equals(childText(plugin, "cleanFrontendFiles"))
                || !"true".equals(childText(plugin, "frontendHotdeploy"))
                || !"false".equals(childText(plugin, "optimizeBundle"))) {
            throw new IllegalStateException(
                    "CLI did not create the standard Vite plugin configuration");
        }
        Element dependency = findCoordinates(
                document.getElementsByTagNameNS("*", "dependency"),
                "com.vaadin", "vaadin-dev-server");
        if (dependency == null
                || !"runtime".equals(childText(dependency, "scope"))
                || !"${vaadin.version}".equals(childText(dependency, "version"))) {
            throw new IllegalStateException(
                    "CLI did not create the build-only Vite resource dependency");
        }
    }

    private static void verifyHostExitPropagation(Path directory) throws Exception {
        Path app = directory.resolve("FakeHost.app");
        Path executable = app.resolve("Contents/MacOS/VaadinDesktopHost");
        Path logs = directory.resolve("logs");
        Files.createDirectories(executable.getParent());
        Files.writeString(executable, """
                #!/bin/sh
                printf 'fake host stdout\n'
                printf 'fake host stderr\n' >&2
                exit 17
                """);
        if (!executable.toFile().setExecutable(true)) {
            throw new IllegalStateException("Cannot make fake host executable");
        }
        try {
            VaadinDesktopCli.launch(app, logs, Map.of(), System.out);
            throw new IllegalStateException("CLI accepted a failed native host");
        } catch (VaadinDesktopCli.CliException expected) {
            if (expected.exitCode() != 17
                    || !expected.getMessage().contains("host exited with code 17")
                    || !Files.readString(logs.resolve("host.out.log"))
                    .contains("fake host stdout")
                    || !Files.readString(logs.resolve("host.err.log"))
                    .contains("fake host stderr")) {
                throw new IllegalStateException(
                        "CLI did not preserve native host failure evidence", expected);
            }
        }
    }

    private static Element findCoordinates(
            org.w3c.dom.NodeList nodes,
            String groupId,
            String artifactId) {
        for (int index = 0; index < nodes.getLength(); index++) {
            Element element = (Element) nodes.item(index);
            if (groupId.equals(childText(element, "groupId"))
                    && artifactId.equals(childText(element, "artifactId"))) {
                return element;
            }
        }
        return null;
    }

    private static String childText(Element parent, String localName) {
        for (Node child = parent.getFirstChild();
             child != null;
             child = child.getNextSibling()) {
            if (child instanceof Element element
                    && localName.equals(element.getLocalName())) {
                if ("configuration".equals(localName)) {
                    return "";
                }
                return element.getTextContent().trim();
            }
            if (child instanceof Element element
                    && "configuration".equals(element.getLocalName())) {
                String value = childText(element, localName);
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        return "";
    }
}
