package dev.vatori.desktop.flow.ipc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

final class FlowIpcFrontendBuildEvidence {
    private FlowIpcFrontendBuildEvidence() {
    }

    static String runtimeSource(
            FrontendBuildStepEvidence buildStep,
            AppBuildGeneratedImportsEvidence appOutput) {
        boolean buildStepAttempted = "success".equals(buildStep.status())
                || "failed".equals(buildStep.status())
                || "unavailable".equals(buildStep.status());
        if (appOutput.observed()) {
            return "current-app-build-output";
        }
        if ("unavailable".equals(buildStep.status())) {
            return "build-step-unavailable";
        }
        if (buildStepAttempted) {
            return "current-app-build-output-incomplete";
        }
        return "packaged-dev-bundle-fallback";
    }

    static FrontendBuildStepEvidence buildStep() {
        FlowIpcFrontendBuildEnvironment environment = FlowIpcFrontendBuildEnvironment.current();
        String status = environment.buildValue("STEP_STATUS");
        if (!environment.anyFlagEnabled("V1_30", "V1_31", "V1_32") || status == null || status.isBlank()) {
            status = "skipped";
        }
        return new FrontendBuildStepEvidence(
                compactText(environment.buildValue("STEP_COMMAND")),
                parseInt(environment.buildValue("STEP_EXIT_CODE"), -1),
                status,
                parseLong(environment.buildValue("STEP_DURATION_MILLIS"), 0L),
                compactText(environment.buildValue("OUTPUT_ROOTS")),
                compactText(environment.buildValue("STEP_SUMMARY")),
                compactText(environment.buildValue("STDOUT_LOG")),
                compactText(environment.buildValue("STDERR_LOG")),
                compactText(environment.buildValue("AVAILABILITY_MODE"), "offline-default"),
                compactText(environment.buildValue("EFFECTIVE_MAVEN_MODE"), "-o"),
                compactText(environment.buildValue("PLUGIN_COORDINATES")),
                compactText(environment.buildValue("PLUGIN_RESOLUTION_STATUS"), "not-attempted"),
                compactText(environment.buildValue("MAVEN_GOAL_RESULT", "GOAL_RESULT"), "skipped"),
                compactText(environment.buildValue("OUTPUT_USABILITY"), "not-inspected"),
                compactText(environment.buildValue("MAVEN_GOAL_NAME", "GOAL_NAME"), "prepare-frontend"),
                compactText(environment.buildValue("PRODUCTION_MODE"), "false"),
                compactText(environment.buildValue("FORCE_PRODUCTION_BUILD"), "false"),
                compactText(environment.buildValue("IGNORE_VERSION_CHECKS"), "false"));
    }

    static String firstObservedInventoryPath(List<Map<String, Object>> inventory) {
        for (Map<String, Object> candidate : inventory) {
            if (Boolean.TRUE.equals(candidate.get("exists"))) {
                Object path = candidate.get("path");
                return path == null ? "" : path.toString();
            }
        }
        return "";
    }

    static AppBuildGeneratedImportsEvidence appGeneratedImports(String devBundleGeneratedFlowImportsPath) {
        Path module = FlowIpcBuildOutputScanner.appModuleDirectory();
        Path target = module.resolve("target").normalize();
        Path generatedImports = FlowIpcBuildOutputScanner.findAppGeneratedImports(
                module,
                devBundleGeneratedFlowImportsPath);
        if (generatedImports == null || !Files.isRegularFile(generatedImports)) {
            Path candidate = target.resolve("classes").resolve(devBundleGeneratedFlowImportsPath).normalize();
            return new AppBuildGeneratedImportsEvidence(
                    FlowIpcDiagnostics.redactLocation(module.toString()),
                    FlowIpcDiagnostics.redactLocation(candidate.toString()),
                    false,
                    0L,
                    0,
                    "",
                    "missing",
                    false,
                    0,
                    "missing-build-output",
                    "",
                    "");
        }
        try {
            byte[] bytes = Files.readAllBytes(generatedImports);
            String body = new String(bytes, StandardCharsets.UTF_8);
            GeneratedLoadOnDemandChunkFileRegistry registry =
                    GeneratedLoadOnDemandChunkFileRegistry.parse(
                            FlowIpcDiagnostics.redactLocation(generatedImports.toString()),
                            body);
            String chunkStatus = registry.chunkFileStatus();
            String chunkArtifact = registry.chunkArtifactPath();
            if ("branch-table-dynamic-import".equals(registry.loadOnDemandTableShape())
                    && !registry.chunkImportSpecifier().isBlank()) {
                Path chunkPath = generatedImports.getParent()
                        .resolve(registry.chunkImportSpecifier().replaceFirst("^\\./", ""))
                        .normalize();
                chunkArtifact = FlowIpcDiagnostics.redactLocation(chunkPath.toString());
                chunkStatus = Files.isRegularFile(chunkPath) ? "available" : "not-found";
            }
            return new AppBuildGeneratedImportsEvidence(
                    FlowIpcDiagnostics.redactLocation(module.toString()),
                    FlowIpcDiagnostics.redactLocation(generatedImports.toString()),
                    true,
                    Files.getLastModifiedTime(generatedImports).toMillis(),
                    bytes.length,
                    FlowIpcDiagnostics.sha256(bytes),
                    registry.loadOnDemandTableShape(),
                    registry.noOpLoadOnDemand(),
                    GeneratedLoadOnDemandChunkFileRegistry.countChunkReferences(body),
                    chunkStatus,
                    chunkArtifact,
                    "");
        } catch (IOException error) {
            return new AppBuildGeneratedImportsEvidence(
                    FlowIpcDiagnostics.redactLocation(module.toString()),
                    FlowIpcDiagnostics.redactLocation(generatedImports.toString()),
                    false,
                    0L,
                    0,
                    "",
                    "unreadable",
                    false,
                    0,
                    "blocked",
                    "",
                    error.getClass().getSimpleName());
        }
    }

    private static int parseInt(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException error) {
            return fallback;
        }
    }

    private static long parseLong(String value, long fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException error) {
            return fallback;
        }
    }

    private static String compactText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    private static String compactText(String value, String fallback) {
        String compacted = value == null ? "" : value.replace('\n', ' ').replace('\r', ' ').trim();
        return compacted.isBlank() ? fallback : compacted;
    }
}

record AppBuildGeneratedImportsEvidence(
        String modulePath,
        String path,
        boolean observed,
        long lastModifiedMillis,
        int bytes,
        String sha256,
        String tableShape,
        boolean noOpLoadOnDemand,
        int chunkReferences,
        String chunkStatus,
        String chunkArtifact,
        String readError) {
}

record FrontendBuildStepEvidence(
        String command,
        int exitCode,
        String status,
        long durationMillis,
        String outputRoots,
        String summary,
        String stdoutLog,
        String stderrLog,
        String availabilityMode,
        String effectiveMavenMode,
        String pluginCoordinates,
        String pluginResolutionStatus,
        String mavenGoalResult,
        String outputUsability,
        String mavenGoal,
        String productionMode,
        String forceProductionBuild,
        String ignoreVersionChecks) {
}
