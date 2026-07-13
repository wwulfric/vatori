package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.ProductionOwnerTraceFields.*;
import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class ProductionOwnerRuntimeDomTraceInspector {
    private ProductionOwnerRuntimeDomTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("\"type\":\"webview.dom\"")
                && containsStringField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerResourceLoaderVersion",
                        "flow-ipc-production-owner-resource-loader-v1_18")
                && containsStringField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerResourceLoaderMode",
                        "custom-scheme-file-backed")
                && containsStringField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerResourceScheme",
                        "vatori-resource")
                && containsStringField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerResourceGraphMode",
                        "owner-root-and-resolved-dependencies")
                && containsStringField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerResourceBasePolicy",
                        "stable-owner-resource-path-base-v1_18")
                && containsStringField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerResourceLoaderNamespace",
                        "production-owner")
                && containsStringField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerResourceLoaderRootArtifact",
                        "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                && containsStringFieldPrefix(line,
                        "frontendArtifactProductionBundleRuntimeOwnerRootCustomSchemeUrl",
                        "vatori-resource://flow-ipc/production-owner/")
                && containsPositiveIntegerField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerDependencyCustomSchemeUrlCount")
                && line.contains("frontendArtifactProductionBundleRuntimeOwnerDependencyCustomSchemePaths")
                && containsLiteralField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerImportOverCustomScheme", "true")
                && containsLiteralField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerCustomSchemeRequestObserved", "true")
                && containsLiteralField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerCustomSchemeResponseObserved", "true")
                && containsLiteralField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerDependencyCustomSchemeRequestObserved",
                        "true")
                && containsLiteralField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerDependencyCustomSchemeResponseObserved",
                        "true")
                && containsLiteralField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerBlobFallbackAllowed", "false")
                && containsLiteralField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerBlobFallbackUsed", "false")
                && containsStringField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerDependencyBlobFallbackPolicy",
                        "explicit-classified-only")
                && containsStringField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerResourcePathMode",
                        "stable-source-path")
                && (containsProductionOwnerResourceSchemeV118FinalLayer(line,
                        "frontendArtifactProductionBundleRuntimeOwnerFailureLayer")
                        || containsStringField(line,
                                "frontendArtifactProductionBundleRuntimeOwnerFailureLayer",
                                "production-owner-component-reuse-contract-missing"))
                && !containsStringField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerFailureLayer",
                        "production-owner-relative-import-unresolved")
                && !containsStringField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerFailureLayer",
                        "production-owner-runtime-contract-missing")
                && !containsStringField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerFailureLayer",
                        "production-owner-missing-real-artifact")) {
            summary.productionOwnerRuntime.resourceSchemeV118DomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsStringField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerSideEffectVersion",
                        "flow-ipc-production-owner-side-effect-v1_19")
                && containsStringField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerSideEffectPolicy",
                        "custom-scheme-owner-import-side-effect-contract-v1_19")
                && containsStringField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerSideEffectExpectedTags",
                        "vaadin-text-field,vaadin-checkbox,vaadin-select")
                && line.contains("frontendArtifactProductionBundleRuntimeOwnerSideEffectObservedTags")
                && line.contains("frontendArtifactProductionBundleRuntimeOwnerSideEffectMissingTags")
                && containsStringField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerSideEffectExpectedGlobals",
                        "window.Vaadin,window.Vaadin.Flow,window.Vaadin.Flow.loadOnDemand")
                && containsLiteralField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerSideEffectNoBlobFallback", "true")
                && line.contains("frontendArtifactProductionBundleRuntimeOwnerModuleImported")
                && line.contains("frontendArtifactProductionBundleRuntimeOwnerSideEffectSatisfied")
                && containsProductionOwnerSideEffectV119FinalResult(line,
                        "frontendArtifactProductionBundleRuntimeOwnerSideEffectFailureLayer")
                && containsStringField(line, "componentProductionOwnerReuseDecisionVersion",
                        "flow-ipc-component-owner-reuse-decision-v1_19")
                && containsStringField(line, "componentProductionOwnerReuseDecisionSource",
                        "production-owner-custom-scheme-side-effect-v1_19")
                && containsComponentOwnerReuseDecisionV119(line,
                        "componentProductionOwnerReuseDecision")
                && containsLiteralField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerBlobFallbackUsed", "false")
                && containsLiteralField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerImportOverCustomScheme", "true")) {
            summary.productionOwnerRuntime.sideEffectV119DomObserved = true;
            summary.productionOwnerRuntime.sideEffectV119ReuseDecisionObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsStringField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerOwnedRuntimeVersion",
                        "flow-ipc-production-owner-owned-runtime-v1_20")
                && containsStringField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerOwnedRuntimePolicy",
                        "owner-first-custom-scheme-runtime-reuse-v1_20")
                && containsStringField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerOwnedRuntimePhase",
                        "before-component-imports")
                && containsStringField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerOwnedRuntimeActualPhase",
                        "before-component-imports")
                && containsStringField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerOwnedRuntimeExpectedTags",
                        "vaadin-text-field,vaadin-checkbox,vaadin-select")
                && containsLiteralField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerOwnedRuntimeNoBlobFallback", "true")
                && containsLiteralField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerOwnedRuntimeAttempted", "true")
                && line.contains("frontendArtifactProductionBundleRuntimeOwnerOwnedRuntimeSatisfied")
                && containsProductionOwnerOwnedRuntimeV120FinalResult(line,
                        "frontendArtifactProductionBundleRuntimeOwnerOwnedRuntimeFailureLayer")
                && containsStringField(line, "componentProductionOwnerReusePolicyVersionV1_20",
                        "flow-ipc-component-owner-reuse-policy-v1_20")
                && containsStringField(line, "componentProductionOwnerReusePolicyV1_20",
                        "owner-first-then-focused-fallback-v1_20")
                && containsComponentOwnerReuseDecisionV120(line,
                        "componentProductionOwnerReuseDecision")
                && containsLiteralField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerBlobFallbackUsed", "false")
                && containsLiteralField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerImportOverCustomScheme", "true")) {
            summary.productionOwnerRuntime.ownedRuntimeV120DomObserved = true;
            summary.productionOwnerRuntime.ownedRuntimeV120ReuseDecisionObserved = true;
        }
    }
}
