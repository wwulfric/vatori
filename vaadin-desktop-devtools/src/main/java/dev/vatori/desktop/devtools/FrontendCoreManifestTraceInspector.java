package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class FrontendCoreManifestTraceInspector {
    private FrontendCoreManifestTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && (line.contains("\"vaadin-component-classpath-plus-v1_2-shims\"")
                        || line.contains("\"vaadin-component-classpath-plus-dev-bundle-v1_4\""))
                && line.contains("com.vaadin.flow.component.textfield.BigDecimalField")
                && line.contains("vaadin-big-decimal-field")
                && (line.contains("npm-transitive-imports-shimmed")
                        || line.contains("textfield-production-class-execution-blocked")
                        || line.contains("real-textfield-production-graph-executed-v1_7"))
                && line.contains("blockedReason")) {
            summary.vaadinComponents.webComponentResourceManifestObserved = true;
            summary.vaadinComponents.webComponentFailureClassificationObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && line.contains("\"resolverStrategy\":\"generic-component-frontend-manifest-v1_3\"")
                && (line.contains("\"componentCount\":2")
                        || line.contains("\"componentCount\":3")
                        || line.contains("\"componentCount\":4"))
                && line.contains("com.vaadin.flow.component.checkbox.Checkbox")
                && line.contains("vaadin-checkbox")
                && (line.contains("META-INF/vatori/frontend-shims/vaadin-checkbox.js")
                        || line.contains(
                                "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                        || line.contains("META-INF/vatori/dev-bundle-entry/vaadin-checkbox.js"))) {
            summary.vaadinComponents.resolverV13ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && line.contains("\"frontendAssetPipelineVersion\":\"flow-ipc-frontend-assets-v1_4\"")
                && line.contains("\"artifactGraphSource\":\"vaadin-frontend-artifact-graph-v1_4\"")) {
            summary.frontendBuildPipeline.assetPipelineV14Observed = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsStringField(line, "generatedLazyResolverVersion",
                        "flow-ipc-generated-lazy-resolver-v1_8")
                && containsStringField(line, "generatedLazyExecutionVersion",
                        "flow-ipc-generated-lazy-v1_8")
                && containsStringField(line, "generatedLazyImportResolutionStrategy",
                        "stable-relative-import-rewrite-generated-lazy-v1_8")
                && containsStringField(line, "generatedLazyRelativeImportSpecifier",
                        "./markdown-helpers-31I0A-hg.js")
                && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/markdown-helpers-31I0A-hg.js")) {
            summary.frontendBuildPipeline.generatedLazyV18ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsStringField(line, "generatedLazyLoadOnDemandContractVersion",
                        "flow-ipc-loadondemand-v1_9")
                && containsStringField(line, "generatedLazyTriggerSource",
                        "diagnostic-preflight-and-direct-staged-import")
                && containsStringField(line, "generatedLazyApiOwner", "window.Vaadin.Flow.loadOnDemand")
                && containsStringField(line, "generatedLazyApiInstallSource",
                        "generated-flow-imports-dev-bundle-after-direct-staged-import")
                && containsStringField(line, "generatedLazyRequestedChunk",
                        "vatori-flow-ipc-generated-lazy-v1_9-probe")
                && containsStringField(line, "generatedLazyRegistrySource",
                        "no-bootstrap-lazy-registry-observed")
                && containsStringField(line, "generatedLazyMappingSource",
                        "component-frontend-manifest-generated-lazy-v1_9")
                && containsStringField(line, "generatedLazyBlockerVersion",
                        "flow-ipc-loadondemand-v1_9")
                && containsStringField(line, "generatedLazyFailureLayer",
                        "loadondemand-api-installed-by-generated-imports-not-flow-client-triggered")
                && containsLiteralField(line, "generatedLazyFlowClientTriggered", "false")
                && containsLiteralField(line, "generatedLazyDiagnosticBridge", "true")
                && containsPath(line,
                        "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")) {
            summary.frontendBuildPipeline.generatedLazyV19ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsStringField(line, "runtimeResolverContractVersion",
                        "flow-ipc-frontend-runtime-resolver-v1_10")
                && line.contains("\"logicalRequestId\"")
                && line.contains("\"pathMapping\"")) {
            summary.frontendBuildPipeline.runtimeResolverV110ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsStringField(line, "flowLazyDependencyVersion",
                        "flow-ipc-lazy-dependency-v1_10")
                && containsStringField(line, "flowLazyDependencySource",
                        "flow-uidl-dependencyloader-loadmode-lazy-v1_10")
                && containsStringField(line, "flowLazyDependencyMode", "LoadMode.LAZY")
                && containsStringField(line, "flowLazyDependencyExpression",
                        "context://vatori-flow-ipc-lazy-v1_10.js")
                && containsStringField(line, "flowLazyDependencyResolvedPath",
                        "vatori-flow-ipc-lazy-v1_10.js")
                && containsLiteralField(line, "flowLazyDependencyTriggeredByFlowClient", "true")) {
            summary.frontendBuildPipeline.flowLazyDependencyV110ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsStringField(line, "frontendResourceLoaderVersion",
                        "flow-ipc-resource-scheme-v1_17")
                && containsStringField(line, "frontendResourceLoaderModes",
                        "custom-scheme,file-backed,blob-fallback")
                && containsStringField(line, "frontendResourceLoaderPreferredMode",
                        "custom-scheme-file-backed")
                && containsLiteralField(line, "frontendResourceLoaderNoLocalhost", "true")
                && line.contains("frontendResourceLoaderAcceptableFallbacks")
                && line.contains("production-owner-blob-fallback")) {
            summary.frontendBuildPipeline.resourceSchemeV117ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsStringField(line, "frontendResolverContractVersion",
                        "flow-ipc-frontend-resolver-contract-v1_21")
                && containsStringField(line, "frontendResolverContractScope",
                        "flow-client,component-frontend,theme-static,generated-lazy,flow-lazy-dependency,production-owner,dev-mode")
                && containsStringField(line, "frontendResolverIdentityPolicy",
                        "stable-source-path-plus-owner-namespace-v1_21")
                && containsStringField(line, "frontendResolverIntegrityPolicy",
                        "sha256-required-for-bodied-resources-v1_21")
                && containsStringField(line, "frontendResolverCachePolicy",
                        "window-lifetime-content-addressed-v1_21")
                && containsStringField(line, "frontendResolverCleanupPolicy",
                        "host-owned-window-lifetime-v1_21")
                && containsStringField(line, "frontendResolverAllowedSchemes",
                        "blob,vatori-resource")
                && containsLiteralField(line, "frontendResolverNoLocalhost", "true")
                && containsStringField(line, "frontendResolverFamiliesCovered",
                        "flow-client-classpath,vaadin-component-classpath-frontend,npm-frontend,theme-static,generated-lazy,flow-lazy-dependency,production-bundle,dev-mode-bundle")
                && containsStringField(line, "frontendResolverAcceptableGaps",
                        "generated-lazy-chunk-file-not-generated,production-owner-owned-missing-real-artifact,dev-mode-vite-execution-unmapped")) {
            summary.frontendBuildPipeline.resolverContractV121ManifestObserved = true;
        }
    }
}
