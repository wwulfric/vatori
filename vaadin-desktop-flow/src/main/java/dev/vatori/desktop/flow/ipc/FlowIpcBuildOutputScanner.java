package dev.vatori.desktop.flow.ipc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

final class FlowIpcBuildOutputScanner {
    private FlowIpcBuildOutputScanner() {
    }

    static Path appModuleDirectory() {
        String module = System.getenv("VAADIN_DESKTOP_MODULE");
        if (module != null && !module.isBlank()) {
            return Path.of(module).toAbsolutePath().normalize();
        }
        String repoRoot = System.getenv("VAADIN_DESKTOP_REPO_ROOT");
        Path root = repoRoot == null || repoRoot.isBlank()
                ? Path.of("").toAbsolutePath().normalize()
                : Path.of(repoRoot).toAbsolutePath().normalize();
        Path smokeApp = root.resolve("test-apps/flow-over-ipc-smoke-app").normalize();
        if (Files.isDirectory(smokeApp)) {
            return smokeApp;
        }
        return root;
    }

    static Path sourceGeneratedImportsPath(Path module) {
        return module.resolve("src/main/frontend/generated/flow/generated-flow-imports.js").normalize();
    }

    static Path findAppGeneratedImports(Path module, String devBundleGeneratedFlowImportsPath) {
        List<Path> candidates = List.of(
                sourceGeneratedImportsPath(module),
                module.resolve("target/classes").resolve(devBundleGeneratedFlowImportsPath),
                module.resolve("target/classes/META-INF/VAADIN/build/generated-flow-imports-VW7hRaNw.js"),
                module.resolve("target/generated-resources").resolve(devBundleGeneratedFlowImportsPath),
                module.resolve("target/frontend/generated-flow-imports-VW7hRaNw.js"));
        for (Path candidate : candidates) {
            Path normalized = candidate.normalize();
            if (Files.isRegularFile(normalized)) {
                return normalized;
            }
        }
        Path target = module.resolve("target").normalize();
        if (!Files.isDirectory(target)) {
            return null;
        }
        try (Stream<Path> stream = Files.find(target, 8,
                (path, attributes) -> attributes.isRegularFile()
                        && path.getFileName().toString().startsWith("generated-flow-imports")
                        && path.getFileName().toString().endsWith(".js"))) {
            return stream.findFirst().orElse(null);
        } catch (IOException ignored) {
            return null;
        }
    }

    static List<Map<String, Object>> frontendBuildOutputInventory(
            Path module,
            String devBundleGeneratedFlowImportsPath) {
        Path target = module.resolve("target").normalize();
        List<Path> candidates = List.of(
                target,
                module.resolve("src/main/frontend/generated").normalize(),
                module.resolve("src/main/bundles").normalize(),
                target.resolve("classes").resolve(devBundleGeneratedFlowImportsPath).normalize(),
                target.resolve("classes/META-INF/VAADIN/webapp/VAADIN/build").normalize(),
                target.resolve("classes/META-INF/VAADIN/config").normalize(),
                target.resolve("classes/META-INF/VAADIN/build").normalize(),
                target.resolve("classes/META-INF/resources/VAADIN/build").normalize(),
                target.resolve("generated-resources/META-INF/VAADIN/build").normalize(),
                target.resolve("frontend/generated").normalize(),
                module.resolve("frontend/generated").normalize(),
                target.resolve("classes/META-INF/resources/vatori-flow-ipc-real-build-probe-v1_27.js")
                        .normalize());
        return candidates.stream()
                .map(FlowIpcBuildOutputScanner::buildOutputCandidate)
                .toList();
    }

    static FileEvidence fileEvidence(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return FileEvidence.missing(path);
        }
        try {
            byte[] bytes = Files.readAllBytes(path);
            return new FileEvidence(FlowIpcDiagnostics.redactLocation(path.toString()),
                    true,
                    Files.getLastModifiedTime(path).toMillis(),
                    bytes.length,
                    FlowIpcDiagnostics.sha256(bytes));
        } catch (IOException error) {
            return FileEvidence.missing(path);
        }
    }

    static WriteTargetEvidence writeTargetEvidence(String relativePath) {
        Path target = Path.of("").toAbsolutePath().normalize()
                .resolve("target")
                .resolve("classes")
                .resolve(relativePath)
                .normalize();
        boolean observed = Files.isRegularFile(target);
        return new WriteTargetEvidence(
                FlowIpcDiagnostics.redactLocation(target.toString()),
                observed,
                observed ? lastModifiedMillis(target) : 0L);
    }

    static Path productionBuildRoot(Path module) {
        return module.resolve("target/classes/META-INF/VAADIN/webapp/VAADIN/build").normalize();
    }

    static String productionBuildVirtualPath(Path artifact, String virtualRoot) {
        return virtualRoot + artifact.getFileName();
    }

    static String productionBuildVirtualPaths(Path productionBuildRoot, String virtualRoot) {
        if (!Files.isDirectory(productionBuildRoot)) {
            return "";
        }
        try (Stream<Path> stream = Files.list(productionBuildRoot)) {
            return String.join(",", stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".js"))
                    .sorted()
                    .map(path -> productionBuildVirtualPath(path, virtualRoot))
                    .toList());
        } catch (IOException ignored) {
            return "";
        }
    }

    static String productionBuildArtifactContaining(
            Path productionBuildRoot,
            String marker,
            String virtualRoot) {
        if (!Files.isDirectory(productionBuildRoot) || marker == null || marker.isBlank()) {
            return "";
        }
        try (Stream<Path> stream = Files.list(productionBuildRoot)) {
            for (Path path : stream
                    .filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(".js"))
                    .sorted()
                    .toList()) {
                if (fileContains(path, marker)) {
                    return productionBuildVirtualPath(path, virtualRoot);
                }
            }
        } catch (IOException ignored) {
            return "";
        }
        return "";
    }

    static List<Path> productionBuildJavaScriptArtifacts(Path productionBuildRoot) {
        if (!Files.isDirectory(productionBuildRoot)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(productionBuildRoot)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".js"))
                    .sorted()
                    .toList();
        } catch (IOException error) {
            throw new IllegalStateException("Unable to inspect current app production build output "
                    + productionBuildRoot, error);
        }
    }

    static String productionEntryScriptVirtualPath(Path statsPath, String virtualRoot) {
        String entry = firstStatsArrayValue(statsPath, "entryScripts");
        if (entry.isBlank()) {
            return "";
        }
        String prefix = "VAADIN/build/";
        if (entry.startsWith(prefix)) {
            return virtualRoot + entry.substring(prefix.length());
        }
        return virtualRoot + fileName(entry);
    }

    static Path findProductionGeneratedImports(Path root, String productionEntryScriptVirtualPath) {
        if (root == null || !Files.isDirectory(root)) {
            return null;
        }
        try (Stream<Path> stream = Files.find(root, 2,
                (path, attributes) -> attributes.isRegularFile()
                        && path.getFileName().toString().startsWith("generated-flow-imports")
                        && path.getFileName().toString().endsWith(".js"))) {
            List<Path> candidates = stream.sorted().toList();
            String entryFileName = fileName(productionEntryScriptVirtualPath);
            if (!entryFileName.isBlank()) {
                String importNeedle = "./" + entryFileName;
                for (Path candidate : candidates) {
                    try {
                        if (Files.readString(candidate, StandardCharsets.UTF_8).contains(importNeedle)) {
                            return candidate;
                        }
                    } catch (IOException ignored) {
                        // Fall through to the deterministic first candidate when a body is unreadable.
                    }
                }
            }
            return candidates.isEmpty() ? null : candidates.get(0);
        } catch (IOException ignored) {
            return null;
        }
    }

    static List<String> statsBundleImports(Path statsPath) {
        return statsArrayValues(statsPath, "bundleImports");
    }

    static String fileName(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private static Map<String, Object> buildOutputCandidate(Path candidate) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        boolean exists = Files.exists(candidate);
        boolean file = Files.isRegularFile(candidate);
        boolean directory = Files.isDirectory(candidate);
        metadata.put("path", FlowIpcDiagnostics.redactLocation(candidate.toString()));
        metadata.put("exists", exists);
        metadata.put("file", file);
        metadata.put("directory", directory);
        metadata.put("status", file ? "file" : directory ? "directory" : "missing");
        metadata.put("mtimeMillis", lastModifiedMillis(candidate));
        if (file) {
            try {
                byte[] bytes = Files.readAllBytes(candidate);
                metadata.put("fileCount", 1L);
                metadata.put("byteCount", (long) bytes.length);
                metadata.put("sha256", FlowIpcDiagnostics.sha256(bytes));
            } catch (IOException error) {
                metadata.put("fileCount", 0L);
                metadata.put("byteCount", 0L);
                metadata.put("sha256", "");
                metadata.put("readError", error.getClass().getSimpleName());
            }
        } else if (directory) {
            DirectoryInventory directoryInventory = directoryInventory(candidate);
            metadata.put("fileCount", directoryInventory.fileCount());
            metadata.put("byteCount", directoryInventory.byteCount());
            metadata.put("sha256", "");
            if (!directoryInventory.readError().isBlank()) {
                metadata.put("readError", directoryInventory.readError());
            }
        } else {
            metadata.put("fileCount", 0L);
            metadata.put("byteCount", 0L);
            metadata.put("sha256", "");
        }
        return metadata;
    }

    private static boolean fileContains(Path file, String marker) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8).contains(marker);
        } catch (IOException ignored) {
            return false;
        }
    }

    private static String firstStatsArrayValue(Path statsPath, String key) {
        List<String> values = statsArrayValues(statsPath, key);
        return values.isEmpty() ? "" : values.get(0);
    }

    private static List<String> statsArrayValues(Path statsPath, String key) {
        if (statsPath == null || !Files.isRegularFile(statsPath)) {
            return List.of();
        }
        try {
            String body = Files.readString(statsPath, StandardCharsets.UTF_8);
            Pattern array = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
            Matcher arrayMatcher = array.matcher(body);
            if (!arrayMatcher.find()) {
                return List.of();
            }
            Matcher valueMatcher = Pattern.compile("\"([^\"]+)\"").matcher(arrayMatcher.group(1));
            List<String> values = new ArrayList<>();
            while (valueMatcher.find()) {
                String value = valueMatcher.group(1);
                if (!values.contains(value)) {
                    values.add(value);
                }
            }
            return values;
        } catch (IOException ignored) {
            return List.of();
        }
    }

    private static DirectoryInventory directoryInventory(Path directory) {
        long[] counters = new long[] { 0L, 0L };
        try (Stream<Path> stream = Files.walk(directory, 8)) {
            stream.filter(Files::isRegularFile).forEach(path -> {
                counters[0]++;
                try {
                    counters[1] += Files.size(path);
                } catch (IOException ignored) {
                    // Byte count is diagnostic only.
                }
            });
            return new DirectoryInventory(counters[0], counters[1], "");
        } catch (IOException error) {
            return new DirectoryInventory(0L, 0L, error.getClass().getSimpleName());
        }
    }

    private static long lastModifiedMillis(Path path) {
        if (!Files.exists(path)) {
            return 0L;
        }
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException error) {
            return 0L;
        }
    }

    record FileEvidence(
            String path,
            boolean observed,
            long lastModifiedMillis,
            int bytes,
            String sha256) {

        private static FileEvidence missing(Path path) {
            return new FileEvidence(
                    path == null ? "" : FlowIpcDiagnostics.redactLocation(path.toString()),
                    false,
                    0L,
                    0,
                    "");
        }
    }

    record WriteTargetEvidence(
            String path,
            boolean observed,
            long lastModifiedMillis) {
    }

    private record DirectoryInventory(
            long fileCount,
            long byteCount,
            String readError) {
    }
}
