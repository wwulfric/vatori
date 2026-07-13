package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class GeneratedLazyBuildLifecycleTraceFields {
    private GeneratedLazyBuildLifecycleTraceFields() {
    }

    static boolean containsGeneratedLazyBuildLifecycleV128Fields(String line) {
        return containsStringField(line, "generatedLazyBuildLifecycleVersion",
                "flow-ipc-generated-lazy-build-lifecycle-v1_28")
                && containsStringField(line, "generatedLazyBuildLifecycleInputRoute",
                        "flow-ipc-lazy-probe-v1_27")
                && containsStringField(line, "generatedLazyBuildLifecycleInputRouteClass",
                        "dev.vatori.ipcsmoke.FlowIpcGeneratedLazyProbeView")
                && containsStringField(line, "generatedLazyBuildLifecycleInputDependency",
                        "context://vatori-flow-ipc-real-build-probe-v1_27.js")
                && containsLiteralField(line, "generatedLazyBuildLifecycleInputEager", "false")
                && containsStringField(line, "generatedLazyBuildLifecycleScannerSource",
                        "route-annotation-classpath-probe-plus-vaadin-FrontendDependencies-contract-v1_28")
                && containsStringField(line, "generatedLazyBuildLifecycleScannerStatus", "observed")
                && containsLiteralField(line, "generatedLazyBuildLifecycleScannerObserved", "true")
                && containsLiteralField(line, "generatedLazyBuildLifecycleLazyJavascriptObserved", "true")
                && line.contains("ChunkInfo(type=ROUTE,eager=false")
                && containsStringField(line, "generatedLazyBuildLifecycleBuildTaskSource",
                        "dev-ipc-runtime-classpath-resource-resolver-v1_28")
                && containsLiteralField(line, "generatedLazyBuildLifecycleUpdateImportsObserved", "false")
                && containsStringField(line, "generatedLazyBuildLifecycleGeneratedImportsSource",
                        "static-dev-bundle")
                && line.contains("generatedLazyBuildLifecycleGeneratedImportsRuntimePath")
                && containsStringField(line, "generatedLazyBuildLifecycleGeneratedImportsProtocol", "jar")
                && line.contains("generatedLazyBuildLifecycleGeneratedImportsMtimeMillis")
                && line.contains("generatedLazyBuildLifecycleGeneratedImportsSha256")
                && line.contains("generatedLazyBuildLifecycleGeneratedImportsBytes")
                && containsStringField(line, "generatedLazyBuildLifecycleGeneratedImportsTableShape",
                        "no-op-promise-resolve-0")
                && containsLiteralField(line,
                        "generatedLazyBuildLifecycleGeneratedImportsNoOpLoadOnDemand", "true")
                && line.contains("generatedLazyBuildLifecycleWriteTarget")
                && containsLiteralField(line, "generatedLazyBuildLifecycleWriteTargetObserved", "false")
                && line.contains("generatedLazyBuildLifecycleWriteTargetMtimeMillis")
                && containsLiteralField(line,
                        "generatedLazyBuildLifecycleRuntimeUsesStaticDevBundle", "true")
                && containsLiteralField(line, "generatedLazyBuildLifecycleRuntimeProbeMismatch", "true")
                && containsStringField(line, "generatedLazyBuildLifecycleChunkStatus", "not-generated")
                && containsStringField(line, "generatedLazyBuildLifecycleFailureLayer",
                        "runtime-using-stale-packaged-dev-bundle")
                && line.contains("update-imports")
                && containsLiteralField(line, "generatedLazyBuildLifecycleFixtureSubstituted", "false")
                && containsLiteralField(line, "generatedLazyBuildLifecycleNoLocalhost", "true");
    }
}
