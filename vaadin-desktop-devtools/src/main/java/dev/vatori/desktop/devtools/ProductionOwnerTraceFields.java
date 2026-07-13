package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class ProductionOwnerTraceFields {
    private ProductionOwnerTraceFields() {
    }

    static boolean containsProductionOwnerFailureLayer(String line, String field) {
        return containsStringField(line, field, "production-owner-duplicate-custom-element")
                || containsStringField(line, field, "production-owner-relative-import-unresolved")
                || containsStringField(line, field, "production-owner-missing-real-artifact")
                || containsStringField(line, field, "production-owner-browser-import-failed")
                || containsStringField(line, field, "production-owner-side-effect-unobservable")
                || containsStringField(line, field, "production-owner-global-collision")
                || containsStringField(line, field, "production-owner-runtime-contract-missing");
    }

    static boolean containsProductionOwnerResourceSchemeV118FinalLayer(String line, String field) {
        return containsProductionOwnerCustomSchemeFailureLayer(line, field)
                || containsProductionOwnerOwnedRuntimeFailureLayer(line, field);
    }

    static boolean containsProductionOwnerSideEffectV119FinalResult(String line, String failureLayerField) {
        return containsLiteralField(line, "productionBundleRuntimeOwnerSideEffectSatisfied", "true")
                || containsLiteralField(line, "frontendArtifactProductionBundleRuntimeOwnerSideEffectSatisfied", "true")
                || containsProductionOwnerCustomSchemeFailureLayer(line, failureLayerField);
    }

    static boolean containsComponentOwnerReuseDecisionV119(String line, String field) {
        return containsStringField(line, field, "owner-reused")
                || containsStringField(line, field, "owner-reused-positive-fixture")
                || containsStringField(line, field, "focused-fallback-after-owner-side-effect-check")
                || containsStringField(line, field, "focused-fallback-after-owner-owned-runtime-attempt");
    }

    static boolean containsProductionOwnerOwnedRuntimeV120FinalResult(String line,
            String failureLayerField) {
        return containsLiteralField(line, "productionBundleRuntimeOwnerOwnedRuntimeSatisfied", "true")
                || containsLiteralField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerOwnedRuntimeSatisfied", "true")
                || containsProductionOwnerOwnedRuntimeFailureLayer(line, failureLayerField);
    }

    static boolean containsComponentOwnerReuseDecisionV120(String line, String field) {
        return containsStringField(line, field, "owner-reused")
                || containsStringField(line, field, "owner-reused-positive-fixture")
                || containsStringField(line, field, "focused-fallback-after-owner-owned-runtime-attempt");
    }

    static boolean containsOwnerFirstOrIsolatedPhase(String line, String field) {
        return containsStringField(line, field, "before-component-imports")
                || containsStringField(line, field, "isolated-preflight");
    }

    static boolean containsProductionOwnerImportFinalResult(String line, String resultField,
            String failureLayerField) {
        return containsStringFieldPrefix(line, resultField,
                "imported:vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                || (containsStringFieldPrefix(line, resultField, "strict-blocker:production-owner-")
                        && containsProductionOwnerImportFailureLayer(line, failureLayerField)
                        && !containsStringField(line, failureLayerField,
                                "production-owner-runtime-contract-missing"));
    }

    static boolean containsProductionOwnerDependencyFinalResult(String line, String resultField,
            String failureLayerField) {
        return containsStringFieldPrefix(line, resultField,
                "imported:vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                || (containsStringFieldPrefix(line, resultField, "strict-blocker:production-owner-")
                        && containsProductionOwnerDependencyFailureLayer(line, failureLayerField));
    }

    static boolean containsProductionOwnerOrderingFinalResult(String line, String resultField,
            String failureLayerField) {
        return containsStringFieldPrefix(line, resultField,
                "imported:vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                || (containsStringFieldPrefix(line, resultField, "strict-blocker:production-owner-")
                        && containsProductionOwnerOrderingFailureLayer(line, failureLayerField));
    }

    static boolean containsGeneratedChunkFileNoOpResolution(String line) {
        return containsLiteralField(line, "loadOnDemandChunkFileCallbackResolved", "true")
                || (containsLiteralField(line, "loadOnDemandChunkFileCallbackResolved", "false")
                        && containsStringField(line, "loadOnDemandChunkFileFailureLayer",
                                "chunk-file-not-generated")
                        && containsStringField(line, "loadOnDemandChunkFileSideEffectProof",
                                "no-chunk-generated-by-real-generated-flow-imports-no-op-table"));
    }

    static boolean containsGeneratedChunkFileStrictBlockerResult(String line) {
        return containsStringFieldPrefix(line, "loadOnDemandChunkFileExecutionResult",
                "strict-blocker:chunk-file-not-generated")
                || containsStringFieldPrefix(line, "loadOnDemandChunkFileExecutionResult",
                        "strict-blocker:not-generated")
                || (containsStringField(line, "chunkFileStatus", "not-generated")
                        && containsStringField(line, "loadOnDemandChunkFileFailureLayer",
                                "chunk-file-not-generated"));
    }

    private static boolean containsProductionOwnerImportFailureLayer(String line, String field) {
        return containsStringField(line, field, "production-owner-duplicate-custom-element")
                || containsStringField(line, field, "production-owner-relative-import-unresolved")
                || containsStringField(line, field, "production-owner-missing-real-artifact")
                || containsStringField(line, field, "production-owner-browser-import-failed")
                || containsStringField(line, field, "production-owner-side-effect-unobservable")
                || containsStringField(line, field, "production-owner-global-collision")
                || containsProductionOwnerCustomSchemeFailureLayer(line, field);
    }

    private static boolean containsProductionOwnerDependencyFailureLayer(String line, String field) {
        return containsStringField(line, field, "production-owner-duplicate-custom-element")
                || containsStringField(line, field, "production-owner-missing-real-artifact")
                || containsStringField(line, field, "production-owner-browser-import-failed")
                || containsStringField(line, field, "production-owner-side-effect-unobservable")
                || containsStringField(line, field, "production-owner-global-collision")
                || containsStringField(line, field, "production-owner-component-reuse-contract-missing")
                || containsStringField(line, field, "production-owner-unsupported-vaadin-production-owner-contract")
                || containsProductionOwnerCustomSchemeFailureLayer(line, field);
    }

    private static boolean containsProductionOwnerOrderingFailureLayer(String line, String field) {
        return containsStringField(line, field, "production-owner-duplicate-custom-element")
                || containsStringField(line, field, "production-owner-browser-import-failed")
                || containsStringField(line, field, "production-owner-side-effect-unobservable")
                || containsStringField(line, field, "production-owner-global-collision")
                || containsStringField(line, field, "production-owner-component-reuse-contract-missing")
                || containsStringField(line, field, "production-owner-unsupported-vaadin-production-owner-contract")
                || containsProductionOwnerCustomSchemeFailureLayer(line, field);
    }

    private static boolean containsProductionOwnerCustomSchemeFailureLayer(String line, String field) {
        return containsStringField(line, field, "production-owner-custom-scheme-missing-real-artifact")
                || containsStringField(line, field, "production-owner-custom-scheme-duplicate-custom-element")
                || containsStringField(line, field, "production-owner-custom-scheme-runtime-side-effect-blocked")
                || containsStringField(line, field, "production-owner-custom-scheme-browser-import-failed")
                || containsStringField(line, field, "production-owner-custom-scheme-unsupported-vaadin-contract");
    }

    private static boolean containsProductionOwnerOwnedRuntimeFailureLayer(String line, String field) {
        return containsStringField(line, field, "production-owner-owned-side-effect-unobservable")
                || containsStringField(line, field, "production-owner-owned-duplicate-custom-element")
                || containsStringField(line, field, "production-owner-owned-missing-real-artifact")
                || containsStringField(line, field, "production-owner-owned-browser-import-failed")
                || containsStringField(line, field, "production-owner-owned-unsupported-vaadin-contract");
    }
}
