package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.ProductionOwnerTraceFields.*;
import static dev.vatori.desktop.devtools.ResourceStageTraceFields.*;
import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class ProductionOwnerResourceRuntimeStageTraceInspector {
    private ProductionOwnerResourceRuntimeStageTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (isResourceStage(line)
                && line.contains("\"artifactFamily\":\"production-bundle\"")
                && containsStringField(line, "productionBundleRuntimeOwnerResourceLoaderVersion",
                        "flow-ipc-production-owner-resource-loader-v1_18")
                && containsStringField(line, "productionBundleRuntimeOwnerResourceLoaderMode",
                        "custom-scheme-file-backed")
                && containsStringField(line, "productionBundleRuntimeOwnerResourceScheme",
                        "vatori-resource")
                && containsStringField(line, "productionBundleRuntimeOwnerResourceGraphMode",
                        "owner-root-and-resolved-dependencies")
                && containsStringField(line, "productionBundleRuntimeOwnerResourceBasePolicy",
                        "stable-owner-resource-path-base-v1_18")
                && containsStringField(line, "productionBundleRuntimeOwnerResourceLoaderNamespace",
                        "production-owner")
                && containsLiteralField(line, "productionBundleRuntimeOwnerBlobFallbackAllowed", "false")
                && containsStringField(line, "productionBundleRuntimeOwnerDependencyBlobFallbackPolicy",
                        "explicit-classified-only")
                && containsStringField(line, "frontendResourceLoaderMode",
                        "custom-scheme-file-backed")
                && containsStringField(line, "stagingMode", "custom-scheme")
                && containsStringField(line, "productionBundleRuntimeOwnerResourcePathMode",
                        "stable-source-path")
                && !containsStringField(line, "stagingMode", "blob-fallback")) {
            if (containsStringField(line, "productionBundleRuntimeOwnerResourceLoaderArtifactRole",
                    "root")
                    && containsPath(line,
                            "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && (line.contains("\"executionStatus\":\"execution-attempt\"")
                            || line.contains("\"executionStatus\":\"strict-blocker\"")
                            || line.contains("\"executionStatus\":\"executed\""))) {
                summary.productionOwnerRuntime.resourceSchemeV118RootStageObserved = true;
                summary.productionOwnerRuntime.resourceSchemeV118ImportObserved = true;
            }
            if (containsStringField(line, "productionBundleRuntimeOwnerResourceLoaderArtifactRole",
                    "dependency")
                    && line.contains("productionBundleRuntimeOwnerDependencyCustomSchemeProbeResult")) {
                summary.productionOwnerRuntime.resourceSchemeV118DependencyStageObserved = true;
            }
        }
        if (isResourceStage(line)
                && line.contains("\"artifactFamily\":\"production-bundle\"")
                && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                && containsStringField(line, "productionBundleRuntimeOwnerSideEffectVersion",
                        "flow-ipc-production-owner-side-effect-v1_19")
                && containsStringField(line, "productionBundleRuntimeOwnerSideEffectPolicy",
                        "custom-scheme-owner-import-side-effect-contract-v1_19")
                && containsStringField(line, "productionBundleRuntimeOwnerSideEffectExpectedTags",
                        "vaadin-text-field,vaadin-checkbox,vaadin-select")
                && line.contains("productionBundleRuntimeOwnerSideEffectObservedTags")
                && line.contains("productionBundleRuntimeOwnerSideEffectMissingTags")
                && containsStringField(line, "productionBundleRuntimeOwnerSideEffectExpectedGlobals",
                        "window.Vaadin,window.Vaadin.Flow,window.Vaadin.Flow.loadOnDemand")
                && containsLiteralField(line, "productionBundleRuntimeOwnerSideEffectNoBlobFallback", "true")
                && line.contains("productionBundleRuntimeOwnerModuleImported")
                && line.contains("productionBundleRuntimeOwnerSideEffectSatisfied")
                && containsProductionOwnerSideEffectV119FinalResult(line,
                        "productionBundleRuntimeOwnerSideEffectFailureLayer")
                && containsStringField(line, "componentProductionOwnerReuseDecisionVersion",
                        "flow-ipc-component-owner-reuse-decision-v1_19")
                && containsStringField(line, "componentProductionOwnerReuseDecisionSource",
                        "production-owner-custom-scheme-side-effect-v1_19")
                && containsComponentOwnerReuseDecisionV119(line,
                        "componentProductionOwnerReuseDecision")
                && containsLiteralField(line, "productionBundleRuntimeOwnerBlobFallbackUsed", "false")) {
            summary.productionOwnerRuntime.sideEffectV119StageObserved = true;
            summary.productionOwnerRuntime.sideEffectV119ReuseDecisionObserved = true;
        }
        if (isResourceStage(line)
                && line.contains("\"artifactFamily\":\"production-bundle\"")
                && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                && containsStringField(line, "productionBundleRuntimeOwnerOwnedRuntimeVersion",
                        "flow-ipc-production-owner-owned-runtime-v1_20")
                && containsStringField(line, "productionBundleRuntimeOwnerOwnedRuntimePolicy",
                        "owner-first-custom-scheme-runtime-reuse-v1_20")
                && containsStringField(line, "productionBundleRuntimeOwnerOwnedRuntimePhase",
                        "before-component-imports")
                && containsStringField(line, "productionBundleRuntimeOwnerOwnedRuntimeActualPhase",
                        "before-component-imports")
                && containsStringField(line, "productionBundleRuntimeOwnerOwnedRuntimeExpectedTags",
                        "vaadin-text-field,vaadin-checkbox,vaadin-select")
                && containsLiteralField(line, "productionBundleRuntimeOwnerOwnedRuntimeNoBlobFallback", "true")
                && containsLiteralField(line, "productionBundleRuntimeOwnerOwnedRuntimeAttempted", "true")
                && line.contains("productionBundleRuntimeOwnerOwnedRuntimeSatisfied")
                && containsProductionOwnerOwnedRuntimeV120FinalResult(line,
                        "productionBundleRuntimeOwnerOwnedRuntimeFailureLayer")
                && containsStringField(line, "componentProductionOwnerReusePolicyVersionV1_20",
                        "flow-ipc-component-owner-reuse-policy-v1_20")
                && containsStringField(line, "componentProductionOwnerReusePolicyV1_20",
                        "owner-first-then-focused-fallback-v1_20")
                && containsComponentOwnerReuseDecisionV120(line,
                        "componentProductionOwnerReuseDecision")
                && containsLiteralField(line, "productionBundleRuntimeOwnerBlobFallbackUsed", "false")) {
            summary.productionOwnerRuntime.ownedRuntimeV120StageObserved = true;
            summary.productionOwnerRuntime.ownedRuntimeV120ReuseDecisionObserved = true;
        }
        if (isResourceStage(line)
                && (containsStringField(line, "productionBundleRuntimeOwnerOwnedRuntimeFailureLayer",
                        "production-owner-owned-missing-real-artifact")
                        || containsStringField(line, "componentProductionOwnerReuseDecision",
                                "focused-fallback-after-owner-owned-runtime-attempt"))) {
            summary.productionOwnerRuntime.positiveFixtureV123RealBlockerObserved = true;
        }
    }
}
