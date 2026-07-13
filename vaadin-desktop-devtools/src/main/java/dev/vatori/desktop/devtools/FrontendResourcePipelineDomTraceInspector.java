package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class FrontendResourcePipelineDomTraceInspector {
    private FrontendResourcePipelineDomTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "frontendArtifactGraphManifestObserved", "true")
                && containsStringField(line, "frontendAssetPipelineVersion", "flow-ipc-frontend-assets-v1_4")
                && containsStringField(line, "frontendArtifactGraphSource",
                        "vaadin-frontend-artifact-graph-v1_4")) {
            summary.frontendBuildPipeline.assetPipelineV14Observed = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && line.contains("frontendArtifactFamilies")
                && line.contains("flow-client-classpath")
                && line.contains("vaadin-component-classpath-frontend")
                && line.contains("npm-frontend")
                && line.contains("theme-static")
                && line.contains("production-bundle")
                && line.contains("dev-mode-bundle")
                && line.contains("generated-lazy")) {
            summary.frontendBuildPipeline.artifactFamilyModelObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "frontendArtifactGraphDependencyObserved", "true")
                && line.contains("frontendArtifactGraphRequestedBy")
                && line.contains("frontendArtifactGraphDependencyOf")
                && line.contains("frontendArtifactGraphModuleSpecifiers")
                && line.contains("frontendArtifactExecutionPolicies")) {
            summary.frontendBuildPipeline.dependencyGraphObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "frontendArtifactShimFallback", "true")) {
            summary.frontendBuildPipeline.shimBlockerObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsStringField(line, "vaadinWebComponentImportRewriteStrategy",
                        "manifest-import-rewrite-v1_4")
                && containsPositiveIntegerField(line, "vaadinWebComponentImportRewriteCount")
                && line.contains("vaadinWebComponentImportRewriteSpecifiers")
                && line.contains("vaadinWebComponentImportRewritePaths")) {
            summary.frontendBuildPipeline.manifestImportRewriteObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "frontendArtifactProductionBundlePartial", "true")) {
            summary.frontendBuildPipeline.productionBundlePartialObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "frontendArtifactProductionBundleStatsVerified", "true")
                && line.contains("frontendArtifactProductionBundleStatsPath")) {
            summary.frontendBuildPipeline.productionBundleStatsResourceObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "frontendArtifactThemeStaticVerified", "true")) {
            summary.frontendBuildPipeline.themeStaticResourceObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "frontendArtifactGeneratedLazyVerified", "true")
                && line.contains("frontendArtifactGeneratedLazyPath")) {
            summary.frontendBuildPipeline.generatedLazyResourceObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "frontendArtifactGeneratedLazyBundleChunkVerified", "true")
                && line.contains("frontendArtifactGeneratedLazyBundleChunkPath")
                && line.contains("vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")) {
            summary.frontendBuildPipeline.generatedLazyBundleChunkResourceObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "frontendArtifactDevModeVerified", "true")
                && line.contains("frontendArtifactDevModePath")) {
            summary.frontendBuildPipeline.devModeResourceObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "frontendArtifactThemeStaticBlocker", "true")) {
            summary.frontendBuildPipeline.themeStaticStrictBlockerObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "frontendArtifactDevModeBlocker", "true")) {
            summary.frontendBuildPipeline.devModeStrictBlockerObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "frontendArtifactGeneratedLazyBlocker", "true")) {
            summary.frontendBuildPipeline.generatedLazyStrictBlockerObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsStringField(line, "frontendStagingResolverVersion", "flow-ipc-resource-staging-v1_5")
                && containsLiteralField(line, "frontendStagingObserved", "true")
                && containsPositiveIntegerField(line, "frontendStagedResourceCount")
                && line.contains("frontendStagedResourcePaths")
                && line.contains("frontendStagedResourceFamilies")
                && line.contains("frontendStagedResourceSchemes")) {
            summary.frontendBuildPipeline.stagingDomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && (containsLiteralField(line, "frontendArtifactTextFieldStagedBlocker", "true")
                        || line.contains("frontendArtifactTextFieldStagedBlockerPath"))
                && line.contains("TextField")
                && line.contains("frontendArtifactTextFieldStagedBlockerError")) {
            summary.frontendBuildPipeline.textFieldStagedBlockerObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsStringField(line, "frontendArtifactTextFieldGraphParityVersion",
                        "flow-ipc-textfield-graph-v1_6")
                && containsStringField(line, "frontendArtifactTextFieldBlockerVersion",
                        "flow-ipc-textfield-v1_6")
                && containsStringField(line, "frontendArtifactTextFieldNarrowedFailureClassification",
                        "textfield-production-indexhtml-duplicate-custom-element-definition")
                && containsStringField(line, "frontendArtifactTextFieldFailureLayer",
                        "custom-element-registry-conflict-after-production-indexhtml-reimport")
                && line.contains("frontendArtifactTextFieldCustomElementRegistryBefore")
                && line.contains("frontendArtifactTextFieldCustomElementRegistryAfter")
                && line.contains("frontendArtifactTextFieldNextResolutionStep")
                && line.contains("frontendArtifactTextFieldStagedBlockerPath")
                && line.contains("META-INF/vatori/dev-bundle-entry/vaadin-text-field.js")) {
            summary.frontendBuildPipeline.textFieldV16DomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsStringField(line, "frontendArtifactTextFieldGraphParityVersion",
                        "flow-ipc-textfield-graph-v1_6")
                && containsStringField(line, "frontendArtifactTextFieldBlockerVersion",
                        "flow-ipc-textfield-v1_6")
                && containsStringField(line, "frontendArtifactTextFieldExecutionVersion",
                        "flow-ipc-textfield-v1_7")
                && containsStringField(line, "frontendArtifactTextFieldSideEffectVersion",
                        "flow-ipc-textfield-side-effect-v1_7")
                && containsLiteralField(line, "frontendArtifactTextFieldExecuted", "true")
                && containsLiteralField(line, "frontendArtifactTextFieldShimUsed", "false")
                && containsStringField(line, "frontendArtifactTextFieldClassExportArtifact",
                        "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                && containsStringField(line, "frontendArtifactTextFieldDuplicateTag", "vaadin-text-field")
                && containsStringField(line, "frontendArtifactTextFieldRemainingBlockerClassification",
                        "none")
                && (containsStringField(line, "frontendArtifactTextFieldModuleIdentityStrategy",
                        "stable-resource-path-blob-module-identity-v1_7")
                        || containsStringField(line, "frontendArtifactTextFieldModuleIdentityStrategy",
                                "owner-attempt-custom-element-class-export-v1_20")
                        || containsStringField(line, "frontendArtifactTextFieldModuleIdentityStrategy",
                                "component-graph-existing-custom-element-class-export-v1_7"))
                && line.contains("frontendArtifactTextFieldModuleIdentityPaths")
                && line.contains("META-INF/vatori/dev-bundle-entry/vaadin-text-field.js")
                && containsLiteralField(line, "frontendArtifactTextFieldShimUsed", "false")) {
            summary.frontendBuildPipeline.textFieldV17DomObserved = true;
        }
    }
}
