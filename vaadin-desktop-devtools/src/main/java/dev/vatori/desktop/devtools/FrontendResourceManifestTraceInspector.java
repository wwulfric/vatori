package dev.vatori.desktop.devtools;

final class FrontendResourceManifestTraceInspector {
    private FrontendResourceManifestTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"artifactFamilies\"")
                && line.contains("flow-client-classpath")
                && line.contains("vaadin-component-classpath-frontend")
                && line.contains("npm-frontend")
                && line.contains("theme-static")
                && line.contains("production-bundle")
                && line.contains("dev-mode-bundle")
                && line.contains("generated-lazy")) {
            summary.frontendBuildPipeline.artifactFamilyModelObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"dependencyGraph\"")
                && line.contains("\"dependencyOf\"")
                && line.contains("\"executionPolicy\"")
                && line.contains("\"requestedBy\":\"component-frontend-manifest-v1_4\"")) {
            summary.frontendBuildPipeline.dependencyGraphObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"artifactFamily\":\"npm-frontend\"")
                && (line.contains("\"executionPolicy\":\"blocked-shim-fallback\"")
                        || line.contains("\"executionPolicy\":\"legacy-shim-fallback\""))
                && (line.contains("\"artifactStatus\":\"blocked-fallback\"")
                        || line.contains("\"artifactStatus\":\"legacy-fallback\""))
                && (line.contains("\"failureClassification\":\"npm-transitive-imports-shimmed\"")
                        || line.contains("\"failureClassification\":\"textfield-production-class-execution-blocked\"")
                        || line.contains("\"failureClassification\":\"legacy-textfield-shim-fallback-not-success\""))) {
            summary.frontendBuildPipeline.shimBlockerObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && line.contains("\"importRewriteStrategy\":\"manifest-import-rewrite-v1_4\"")
                && line.contains("\"browserExecutionPlan\":\"dynamic-import-blob-with-manifest-import-rewrites-v1_4\"")
                && line.contains("\"importRewrites\"")
                && line.contains("\"specifier\":\"lit\"")
                && line.contains("\"specifier\":\"@vaadin/text-field/src/vaadin-text-field.js\"")
                && (line.contains("\"resourcePath\":\"META-INF/vatori/frontend-shims/lit.js\"")
                        || line.contains("\"resourcePath\":\"META-INF/vatori/dev-bundle-entry/lit-css.js\""))) {
            summary.frontendBuildPipeline.manifestImportRewriteObserved = true;
        }
        if ((line.contains("\"type\":\"bootstrapResponse\"") || line.contains("\"type\":\"resourceResponse\""))
                && line.contains("\"lookupKind\":\"TextField-dev-bundle-resource-v1_4\"")
                && (line.contains("\"blockedExecutionProbe\":\"big-decimal-import-rewrite-to-textfield-dev-bundle\"")
                        || line.contains("\"blockedExecutionProbe\":\"legacy-v1_6-big-decimal-import-rewrite-to-textfield-dev-bundle\""))
                && (line.contains("\"blockedExecutionError\":\"null is not an object (evaluating 'outlet._root')\"")
                        || line.contains("\"blockedExecutionError\":\"resolved-by-stable-module-identity-v1_7\""))
                && line.contains("\"nextResolutionStep\"")) {
            summary.frontendBuildPipeline.textFieldDevBundleBlockerObserved = true;
        }
        if ((line.contains("\"type\":\"bootstrapResponse\"") || line.contains("\"type\":\"resourceResponse\""))
                && line.contains("\"lookupKind\":\"TextField-dev-bundle-resource-v1_4\"")
                && line.contains("\"textfieldGraphParityVersion\":\"flow-ipc-textfield-graph-v1_6\"")
                && line.contains("\"previousBlockerVersion\":\"flow-ipc-textfield-v1_6\"")
                && line.contains("\"executionVersion\":\"flow-ipc-textfield-v1_7\"")
                && line.contains("\"textfieldSideEffectVersion\":\"flow-ipc-textfield-side-effect-v1_7\"")
                && line.contains("\"narrowedFailureClassification\":\"textfield-production-indexhtml-side-effect-deduped\"")
                && line.contains("\"failureLayer\":\"resolved-v1_6-custom-element-registry-conflict\"")
                && line.contains("\"classExportArtifact\":\"vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js\"")
                && line.contains("\"duplicateTag\":\"vaadin-text-field\"")
                && line.contains("\"remainingBlockerClassification\":\"none\"")
                && line.contains("\"moduleIdentityStrategy\":\"stable-resource-path-blob-module-identity-v1_7\"")
                && line.contains("\"legacyShimSuccessPath\":false")
                && !line.contains("META-INF/vatori/frontend-shims/vaadin-text-field.js")) {
            summary.frontendBuildPipeline.textFieldV17ManifestObserved = true;
        }
        if ((line.contains("\"type\":\"bootstrapResponse\"") || line.contains("\"type\":\"resourceResponse\""))
                && line.contains("\"lookupKind\":\"TextField-dev-bundle-resource-v1_4\"")
                && line.contains("\"textfieldGraphParityVersion\":\"flow-ipc-textfield-graph-v1_6\"")
                && line.contains("\"blockerVersion\":\"flow-ipc-textfield-v1_6\"")
                && line.contains("\"narrowedFailureClassification\":\"textfield-production-indexhtml-duplicate-custom-element-definition\"")
                && line.contains("\"failureLayer\":\"custom-element-registry-conflict-after-production-indexhtml-reimport\"")
                && line.contains("\"minimalReproduction\"")
                && line.contains("indexhtml-BfYYZi8S.js")
                && line.contains("\"nextResolutionStep\"")) {
            summary.frontendBuildPipeline.textFieldV16ManifestObserved = true;
        }
        if (line.contains("\"type\":\"resourceRequest\"")
                && line.contains("META-INF/vatori/dev-bundle-entry/vaadin-text-field.js")
                && line.contains("\"lookupKind\":\"TextField-dev-bundle-resource-v1_4\"")
                && line.contains("\"blockerVersion\":\"flow-ipc-textfield-v1_6\"")
                && line.contains("\"narrowedFailureClassification\":\"textfield-production-indexhtml-duplicate-custom-element-definition\"")
                && line.contains("\"transitiveImports\"")) {
            summary.frontendBuildPipeline.textFieldV16RequestObserved = true;
        }
        if (line.contains("\"type\":\"resourceRequest\"")
                && line.contains("META-INF/vatori/dev-bundle-entry/vaadin-text-field.js")
                && line.contains("\"lookupKind\":\"TextField-dev-bundle-resource-v1_4\"")
                && line.contains("\"previousBlockerVersion\":\"flow-ipc-textfield-v1_6\"")
                && line.contains("\"executionVersion\":\"flow-ipc-textfield-v1_7\"")
                && line.contains("\"textfieldSideEffectVersion\":\"flow-ipc-textfield-side-effect-v1_7\"")
                && line.contains("\"classExportArtifact\":\"vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js\"")
                && line.contains("\"importResolutionStrategy\":\"stable-resource-path-blob-module-identity-v1_7\"")
                && line.contains("\"transitiveImports\"")
                && !line.contains("META-INF/vatori/frontend-shims/vaadin-text-field.js")) {
            summary.frontendBuildPipeline.textFieldV17RequestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"artifactFamily\":\"production-bundle\"")
                && line.contains("\"executionPolicy\":\"execute-dev-bundle-entry\"")
                && line.contains("\"executionPolicy\":\"dependency-module\"")
                && line.contains("\"failureClassification\":\"dev-bundle-resource-graph-partial\"")) {
            summary.frontendBuildPipeline.productionBundlePartialObserved = true;
        }
        if ((line.contains("\"type\":\"bootstrapResponse\"") || line.contains("\"type\":\"resourceResponse\""))
                && line.contains("\"artifactFamily\":\"production-bundle\"")
                && line.contains("\"artifactStatus\":\"available\"")
                && line.contains("\"lookupKind\":\"Production-bundle-stats-resource-v1_4\"")) {
            summary.frontendBuildPipeline.productionBundleStatsResourceObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"artifactFamily\":\"theme-static\"")
                && line.contains("\"strictBlocker\":true")
                && line.contains("\"failureClassification\":\"theme-static-resource-not-declared\"")) {
            summary.frontendBuildPipeline.themeStaticStrictBlockerObserved = true;
        }
        if ((line.contains("\"type\":\"bootstrapResponse\"") || line.contains("\"type\":\"resourceResponse\""))
                && line.contains("\"artifactFamily\":\"theme-static\"")
                && line.contains("\"artifactStatus\":\"available\"")
                && line.contains("\"lookupKind\":\"Theme-static-resource-v1_4\"")) {
            summary.frontendBuildPipeline.themeStaticResourceObserved = true;
        }
        if ((line.contains("\"type\":\"bootstrapResponse\"") || line.contains("\"type\":\"resourceResponse\""))
                && line.contains("\"artifactFamily\":\"generated-lazy\"")
                && line.contains("\"artifactStatus\":\"available\"")
                && line.contains("\"lookupKind\":\"Generated-flow-imports-resource-v1_4\"")) {
            summary.frontendBuildPipeline.generatedLazyResourceObserved = true;
        }
        if ((line.contains("\"type\":\"bootstrapResponse\"") || line.contains("\"type\":\"resourceResponse\""))
                && line.contains("\"artifactFamily\":\"generated-lazy\"")
                && line.contains("\"artifactStatus\":\"available\"")
                && line.contains("\"lookupKind\":\"Generated-flow-imports-dev-bundle-chunk-v1_4\"")
                && line.contains("\"generatedLazyBundleChunk\":true")
                && line.contains("vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")) {
            summary.frontendBuildPipeline.generatedLazyBundleChunkResourceObserved = true;
        }
        if ((line.contains("\"type\":\"bootstrapResponse\"") || line.contains("\"type\":\"resourceResponse\""))
                && line.contains("\"artifactFamily\":\"dev-mode-bundle\"")
                && line.contains("\"artifactStatus\":\"available\"")
                && line.contains("\"lookupKind\":\"Dev-mode-package-lock-resource-v1_4\"")) {
            summary.frontendBuildPipeline.devModeResourceObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"artifactFamily\":\"dev-mode-bundle\"")
                && line.contains("\"strictBlocker\":true")
                && (line.contains("\"failureClassification\":\"dev-mode-bundle-resource-graph-unmapped\"")
                        || line.contains("\"failureClassification\":\"dev-mode-bundle-execution-unmapped\""))) {
            summary.frontendBuildPipeline.devModeStrictBlockerObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"artifactFamily\":\"generated-lazy\"")
                && line.contains("\"strictBlocker\":true")
                && (line.contains("\"failureClassification\":\"generated-lazy-resource-unmapped\"")
                        || line.contains("\"failureClassification\":\"generated-lazy-execution-unmapped\"")
                        || line.contains(
                                "\"failureClassification\":\"generated-lazy-loadondemand-api-boundary-v1_9\""))) {
            summary.frontendBuildPipeline.generatedLazyStrictBlockerObserved = true;
        }
    }
}
