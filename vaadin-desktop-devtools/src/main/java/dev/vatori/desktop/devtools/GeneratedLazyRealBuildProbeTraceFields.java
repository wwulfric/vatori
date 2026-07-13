package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class GeneratedLazyRealBuildProbeTraceFields {
    private GeneratedLazyRealBuildProbeTraceFields() {
    }

    static boolean containsGeneratedLazyRealBuildProbeV127Fields(String line) {
        return containsStringField(line, "generatedLazyRealBuildProbeVersion",
                "flow-ipc-generated-lazy-real-build-probe-v1_27")
                && containsStringField(line, "generatedLazyRealBuildProbeManifest",
                        "META-INF/vatori/generated-lazy-real-build-probe-v1_27.properties")
                && containsLiteralField(line, "generatedLazyRealBuildProbeManifestObserved", "true")
                && containsStringField(line, "generatedLazyRealBuildInputRoute",
                        "flow-ipc-lazy-probe-v1_27")
                && containsStringField(line, "generatedLazyRealBuildInputRouteClass",
                        "dev.vatori.ipcsmoke.FlowIpcGeneratedLazyProbeView")
                && containsStringField(line, "generatedLazyRealBuildInputTrigger",
                        "dev.vatori.ipcsmoke.FlowIpcGeneratedLazyProbeView")
                && containsStringField(line, "generatedLazyRealBuildInputDependency",
                        "context://vatori-flow-ipc-real-build-probe-v1_27.js")
                && containsLiteralField(line, "generatedLazyRealBuildInputEager", "false")
                && containsLiteralField(line, "generatedLazyRealBuildInputRouteObserved", "true")
                && containsLiteralField(line, "generatedLazyRealBuildInputRouteMatches", "true")
                && containsStringField(line, "generatedLazyRealBuildScannerSource",
                        "route-annotation-classpath-probe-v1_27")
                && containsLiteralField(line, "generatedLazyRealBuildScannerObserved", "true")
                && containsLiteralField(line, "generatedLazyRealBuildLazyJavascriptObserved", "true")
                && containsStringField(line, "generatedLazyRealBuildGeneratedImportsArtifact",
                        "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                && containsStringField(line, "generatedLazyRealBuildGeneratedImportsTableShape",
                        "no-op-promise-resolve-0")
                && containsLiteralField(line, "generatedLazyRealBuildNoOpLoadOnDemand", "true")
                && containsStringField(line, "generatedLazyRealBuildChunkStatus", "not-generated")
                && containsStringField(line, "generatedLazyRealBuildFailureLayer",
                        "scanner-output-not-connected-to-dev-bundle-generated-imports")
                && line.contains("non-eager v1.27 route input")
                && containsLiteralField(line, "generatedLazyRealBuildFixtureSubstituted", "false")
                && containsLiteralField(line, "generatedLazyRealBuildNoLocalhost", "true");
    }
}
