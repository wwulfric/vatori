package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class GeneratedLazySourceBoundaryTraceFields {
    private GeneratedLazySourceBoundaryTraceFields() {
    }

    static boolean containsGeneratedLazySourceBoundaryV126Fields(String line) {
        return containsStringField(line, "generatedLazySourceBoundaryVersion",
                "flow-ipc-generated-lazy-source-boundary-v1_26")
                && containsStringField(line, "generatedLazySourceMechanism",
                        "vaadin-AbstractUpdateImports-generateLazyLoadedChunks")
                && line.contains("generatedLazySourceArtifact")
                && containsStringField(line, "generatedLazySourceScanner",
                        "com.vaadin.flow.server.frontend.scanner.FrontendDependencies")
                && containsStringField(line, "generatedLazySourceLazyRouteRule",
                        "ChunkInfo(type=ROUTE,eager=false)")
                && containsStringField(line, "generatedLazySourceChunkTriggerInput",
                        "DependencyTrigger classes plus lazyJavascript/lazyCss")
                && containsStringField(line, "generatedLazySourcePositiveTableShape",
                        "branch-table-dynamic-import")
                && line.contains("pending.push(import('./chunks/chunk-<content-hash>.js'))")
                && containsStringField(line, "generatedLazySourceNoChunkFallback",
                        "Promise.resolve(0)")
                && containsStringField(line, "generatedLazySourceCurrentBuildArtifact",
                        "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                && containsStringField(line, "generatedLazySourceCurrentBuildStatus",
                        "no-real-generated-lazy-chunk-produced")
                && containsStringField(line, "generatedLazySourceCurrentBuildTableShape",
                        "no-op-promise-resolve-0")
                && containsStringField(line, "generatedLazySourceCurrentBuildChunkStatus",
                        "not-generated")
                && containsLiteralField(line, "generatedLazySourceCurrentBuildNoOpConfirmed", "true")
                && containsStringField(line, "generatedLazySourceBuildInputStatus",
                        "current-smoke-dev-bundle-has-no-lazy-javascript-or-css-chunk-input")
                && containsStringField(line, "generatedLazySourceSupportedBuildPath",
                        "run-vaadin-update-imports-with-non-eager-route-dependency-or-supported-lazy-chunk-spi")
                && containsStringField(line, "generatedLazySourceDecision",
                        "build-spi-required-before-real-app-chunk-parity")
                && containsStringField(line, "generatedLazySourceRemainingBlocker",
                        "real-app-generated-chunk-file-not-produced-by-current-build")
                && line.contains("v1.22 fixture")
                && containsLiteralField(line, "generatedLazySourceNoLocalhost", "true");
    }
}
