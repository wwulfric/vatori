package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class ProductionOwnerFixtureDomTraceInspector {
    private ProductionOwnerFixtureDomTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "productionBundleRuntimeOwnerPositiveFixtureObserved",
                        "true")
                && containsLiteralField(line, "productionBundleRuntimeOwnerPositiveFixtureStageObserved",
                        "true")
                && containsLiteralField(line, "productionBundleRuntimeOwnerPositiveFixtureImported",
                        "true")
                && containsLiteralField(line,
                        "productionBundleRuntimeOwnerPositiveFixtureSideEffectSatisfied", "true")
                && containsLiteralField(line,
                        "productionBundleRuntimeOwnerPositiveFixtureOwnerOwnedRuntimeSatisfied", "true")
                && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureVersion",
                        "flow-ipc-production-owner-positive-runtime-v1_23")
                && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureSource",
                        "repository-production-owner-positive-fixture-v1_23")
                && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureRootArtifact",
                        "META-INF/vatori/production-owner/owner-runtime-v1_23.js")
                && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureDependencyArtifact",
                        "META-INF/vatori/production-owner/owner-dependency-v1_23.js")
                && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureExpectedTags",
                        "vatori-production-owner-fixture")
                && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureExpectedGlobals",
                        "window.VaadinDesktopProductionOwnerV123")
                && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureExecutionPolicy",
                        "owner-first-custom-scheme-positive-fixture-v1_23")
                && containsLiteralField(line, "productionBundleRuntimeOwnerPositiveFixtureNoBlobFallback",
                        "true")
                && containsStringFieldPrefix(line,
                        "productionBundleRuntimeOwnerPositiveFixtureRootCustomSchemeUrl",
                        "vatori-resource://flow-ipc/production-owner/")
                && containsStringFieldPrefix(line,
                        "productionBundleRuntimeOwnerPositiveFixtureDependencyCustomSchemeUrl",
                        "vatori-resource://flow-ipc/production-owner/")
                && containsStringFieldPrefix(line,
                        "productionBundleRuntimeOwnerPositiveFixtureExecutionResult", "executed:")
                && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureSideEffectProof",
                        "customElements:vatori-production-owner-fixture;global:window.VaadinDesktopProductionOwnerV123;dependency=window.VaadinDesktopProductionOwnerDependencyV123.executed")
                && containsStringField(line,
                        "productionBundleRuntimeOwnerPositiveFixtureComponentReuseDecision",
                        "owner-reused-positive-fixture")
                && containsStringField(line, "componentProductionOwnerReuseDecision",
                        "owner-reused-positive-fixture")
                && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureFailureLayer", "")) {
            summary.productionOwnerRuntime.positiveFixtureV123DomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && (containsStringField(line, "frontendArtifactProductionBundleRuntimeOwnerOwnedRuntimeFailureLayer",
                        "production-owner-owned-missing-real-artifact")
                        || containsStringField(line, "productionBundleRuntimeOwnerOwnedRuntimeFailureLayer",
                                "production-owner-owned-missing-real-artifact")
                        || containsStringField(line, "componentProductionOwnerReuseDecision",
                                "focused-fallback-after-owner-owned-runtime-attempt"))) {
            summary.productionOwnerRuntime.positiveFixtureV123RealBlockerObserved = true;
        }
    }
}
