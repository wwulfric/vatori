package dev.vatori.desktop.flow.ipc;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class FlowClientResourceSmokeMain {
    private FlowClientResourceSmokeMain() {
    }

    public static void main(String[] args) {
        FlowClientResources resources = new FlowClientResources();
        Map<String, Object> manifest = resources.manifest();
        Object resourceEntries = manifest.get("resources");
        if (!(resourceEntries instanceof List<?> entries) || entries.isEmpty()) {
            throw new IllegalStateException("Flow client resource manifest is empty");
        }
        if (!Objects.equals(manifest.get("count"), entries.size())) {
            throw new IllegalStateException("Flow client resource manifest count does not match resources size");
        }
        requireEquals("flow-ipc-frontend-assets-v1_4", manifest.get("frontendAssetPipelineVersion"),
                "Flow client frontend asset pipeline version");
        requireEquals("vaadin-frontend-artifact-graph-v1_4", manifest.get("artifactGraphSource"),
                "Flow client artifact graph source");
        requireFrontendResourceLoaderMetadata(manifest, "Flow client manifest");
        requireFrontendResolverContractMetadata(manifest, "Flow client manifest");
        requireListContains(manifest.get("artifactFamilies"), "flow-client-classpath",
                "Flow client artifact families");
        if (!resources.contains("META-INF/frontend/FlowBootstrap.js")) {
            throw new IllegalStateException("FlowBootstrap.js was not found on the Flow IPC runtime classpath");
        }
        if (!resources.contains("META-INF/frontend/FlowClient.js")) {
            throw new IllegalStateException("FlowClient.js was not found on the Flow IPC runtime classpath");
        }

        boolean flowModuleFound = false;
        boolean indexModuleFound = false;
        boolean compilePropertiesFound = false;
        boolean gwtBundleFound = false;
        for (Object entry : entries) {
            if (!(entry instanceof Map<?, ?> metadata)) {
                throw new IllegalStateException("Flow client resource manifest contains a non-object entry");
            }
            Object pathValue = metadata.get("path");
            if (!(pathValue instanceof String path) || path.isBlank()) {
                throw new IllegalStateException("Flow client resource manifest entry is missing path");
            }
            requireEquals("flow-client-classpath", metadata.get("artifactFamily"),
                    path + " artifact family");
            requireEquals("classpath", metadata.get("origin"), path + " origin");
            requirePresentString(metadata.get("moduleSpecifier"), path + " module specifier");
            requirePresentString(metadata.get("resolver"), path + " resolver");
            requirePresentString(metadata.get("dependencyOf"), path + " dependencyOf");
            String executionPolicy = requirePresentString(metadata.get("executionPolicy"), path + " execution policy");
            requireEquals("available", metadata.get("artifactStatus"), path + " artifact status");
            requirePresentString(metadata.get("failureClassification"), path + " failure classification");
            if (!"execute-primary-module".equals(executionPolicy)) {
                requirePresentString(metadata.get("blockedReason"), path + " blocked reason");
            }
            if (!resources.contains(path)) {
                throw new IllegalStateException("Manifest resource is not available from FlowClientResources: " + path);
            }
            Map<String, Object> response = resources.response(path);
            requireEquals(path, response.get("path"), path + " response path");
            requireEquals(metadata.get("kind"), response.get("kind"), path + " response kind");
            requireEquals(metadata.get("bytes"), response.get("bytes"), path + " response byte count");
            requireEquals(metadata.get("sha256"), response.get("sha256"), path + " response sha256");
            requireEquals(metadata.get("artifactFamily"), response.get("artifactFamily"),
                    path + " response artifact family");
            requireEquals(metadata.get("dependencyOf"), response.get("dependencyOf"),
                    path + " response dependencyOf");
            requireEquals(metadata.get("executionPolicy"), response.get("executionPolicy"),
                    path + " response execution policy");
            requireEquals(metadata.get("failureClassification"), response.get("failureClassification"),
                    path + " response failure classification");
            requireFrontendResolverContractMetadata(metadata, path + " manifest entry");
            requireFrontendResolverContractMetadata(response, path + " response");
            if (!Boolean.TRUE.equals(response.get("flowClientResource")) || !Boolean.TRUE.equals(response.get("available"))) {
                throw new IllegalStateException("Flow client resource response is not marked available: " + path);
            }
            if (Boolean.TRUE.equals(metadata.get("text"))) {
                Object body = response.get("body");
                if (!(body instanceof String text) || text.isEmpty()) {
                    throw new IllegalStateException("Text Flow client resource response is missing body: " + path);
                }
            }
            flowModuleFound = flowModuleFound || "META-INF/frontend/Flow.js".equals(path);
            indexModuleFound = indexModuleFound || "META-INF/frontend/index.js".equals(path);
            compilePropertiesFound = compilePropertiesFound
                    || "META-INF/resources/VAADIN/static/client/compile.properties".equals(path);
            gwtBundleFound = gwtBundleFound || "flow-client-gwt-bundle".equals(metadata.get("kind"));
        }
        if (entries.size() < 6 || !flowModuleFound || !indexModuleFound || !compilePropertiesFound || !gwtBundleFound) {
            throw new IllegalStateException("Flow client resource manifest does not cover the expected classpath graph");
        }

        Map<String, Object> bootstrapResponse = resources.response("META-INF/frontend/FlowBootstrap.js");
        Object bootstrapBody = bootstrapResponse.get("body");
        if (!(bootstrapBody instanceof String bootstrapText)
                || !bootstrapText.contains("window.Vaadin.Flow.initApplication")
                || !bootstrapText.contains("export { init }")) {
            throw new IllegalStateException("FlowBootstrap.js did not expose the expected Flow bootstrap entry");
        }

        Map<String, Object> clientResponse = resources.response("META-INF/frontend/FlowClient.js");
        Object clientBody = clientResponse.get("body");
        if (!(clientBody instanceof String clientText)
                || !clientText.contains("export function init")
                || !clientText.contains("Vaadin.Flow.registerWidgetset")) {
            throw new IllegalStateException("FlowClient.js did not expose the expected Flow client entry");
        }
        if (!clientText.contains("handleIpcMessage") || !clientText.contains("requestIpcResync")) {
            throw new IllegalStateException("FlowClient.js did not expose the Flow IPC bridge methods");
        }
        Map<String, Object> componentManifest = resources.componentManifest();
        Object componentEntries = componentManifest.get("resources");
        if (!(componentEntries instanceof List<?> componentResources) || componentResources.size() < 15) {
            throw new IllegalStateException("Component frontend resource manifest does not contain the framework graph");
        }
        if (!Objects.equals(componentManifest.get("resolverStrategy"), "generic-component-frontend-manifest-v1_3")) {
            throw new IllegalStateException("Component frontend manifest is not using the v1.3 resolver strategy");
        }
        requireEquals("flow-ipc-frontend-assets-v1_4", componentManifest.get("frontendAssetPipelineVersion"),
                "Component frontend asset pipeline version");
        requireEquals("vaadin-frontend-artifact-graph-v1_4", componentManifest.get("artifactGraphSource"),
                "Component artifact graph source");
        requireFrontendResourceLoaderMetadata(componentManifest, "Component frontend manifest");
        requireFrontendResolverContractMetadata(componentManifest, "Component frontend manifest");
        requireEquals("flow-ipc-textfield-graph-v1_6", componentManifest.get("textfieldGraphParityVersion"),
                "TextField graph parity version");
        requireEquals("flow-ipc-textfield-v1_6", componentManifest.get("textfieldPreviousBlockerVersion"),
                "TextField previous blocker version");
        requireEquals("flow-ipc-textfield-v1_7", componentManifest.get("textfieldExecutionVersion"),
                "TextField execution version");
        requireEquals("flow-ipc-textfield-side-effect-v1_7", componentManifest.get("textfieldSideEffectVersion"),
                "TextField side-effect version");
        requireEquals("textfield-production-indexhtml-side-effect-deduped",
                componentManifest.get("textfieldNarrowedFailureClassification"),
                "TextField narrowed failure classification");
        requireEquals("resolved-v1_6-custom-element-registry-conflict",
                componentManifest.get("textfieldFailureLayer"),
                "TextField failure layer");
        requireEquals("META-INF/vatori/dev-bundle-entry/vaadin-text-field.js",
                componentManifest.get("textfieldEntryArtifact"),
                "TextField entry artifact");
        requireEquals("vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js",
                componentManifest.get("textfieldClassExportArtifact"),
                "TextField class export artifact");
        requireEquals("vaadin-text-field", componentManifest.get("textfieldDuplicateTag"),
                "TextField duplicate tag");
        requireEquals("none", componentManifest.get("textfieldRemainingBlockerClassification"),
                "TextField remaining blocker classification");
        requireEquals("stable-resource-path-blob-module-identity-v1_7",
                componentManifest.get("textfieldImportResolutionStrategy"),
                "TextField import resolution strategy");
        requireEquals("reuse-production-indexhtml-module-identity-before-textfield-entry-import",
                componentManifest.get("textfieldSideEffectStrategy"),
                "TextField side-effect strategy");
        requireListContains(componentManifest.get("textfieldTransitiveArtifacts"),
                "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js",
                "TextField transitive artifacts");
        requireListContains(componentManifest.get("artifactFamilies"), "vaadin-component-classpath-frontend",
                "Component artifact families");
        requireListContains(componentManifest.get("artifactFamilies"), "npm-frontend",
                "Component artifact families");
        requireListContains(componentManifest.get("artifactFamilies"), "theme-static",
                "Component artifact families");
        requireListContains(componentManifest.get("artifactFamilies"), "production-bundle",
                "Component artifact families");
        requireListContains(componentManifest.get("artifactFamilies"), "dev-mode-bundle",
                "Component artifact families");
        requireListContains(componentManifest.get("artifactFamilies"), "generated-lazy",
                "Component artifact families");
        requireListContains(componentManifest.get("artifactFamilies"), "flow-lazy-dependency",
                "Component artifact families");
        requireNoStrictBlocker(componentManifest, "npm-frontend");
        requireStrictBlocker(componentManifest, "dev-mode-bundle", "dev-mode-bundle-execution-unmapped");
        requireStrictBlocker(componentManifest, "generated-lazy", "generated-lazy-loadondemand-api-boundary-v1_9");
        requireStrictBlocker(componentManifest, "generated-lazy", "flow-ipc-loadondemand-chunk-v1_11");
        requireStrictBlocker(componentManifest, "generated-lazy", "flow-ipc-loadondemand-chunk-file-v1_12");
        requireStrictBlocker(componentManifest, "production-bundle", "flow-ipc-production-runtime-v1_10");
        requireStrictBlocker(componentManifest, "production-bundle", "flow-ipc-production-owner-deps-v1_15");
        requireEquals("flow-ipc-frontend-runtime-resolver-v1_10",
                componentManifest.get("runtimeResolverContractVersion"),
                "Runtime resolver contract version");
        requireEquals("flow-ipc-generated-lazy-resolver-v1_8",
                componentManifest.get("generatedLazyResolverVersion"),
                "Generated/lazy resolver version");
        requireEquals("flow-ipc-generated-lazy-v1_8",
                componentManifest.get("generatedLazyExecutionVersion"),
                "Generated/lazy execution version");
        requireEquals("stable-relative-import-rewrite-generated-lazy-v1_8",
                componentManifest.get("generatedLazyImportResolutionStrategy"),
                "Generated/lazy import resolution strategy");
        requireEquals("./markdown-helpers-31I0A-hg.js",
                componentManifest.get("generatedLazyRelativeImportSpecifier"),
                "Generated/lazy relative import specifier");
        requireEquals("vaadin-dev-bundle/webapp/VAADIN/build/markdown-helpers-31I0A-hg.js",
                componentManifest.get("generatedLazyResolvedDependencyPath"),
                "Generated/lazy resolved dependency path");
        requireEquals("flow-ipc-loadondemand-v1_9",
                componentManifest.get("generatedLazyLoadOnDemandContractVersion"),
                "Generated/lazy loadOnDemand contract version");
        requireEquals("diagnostic-preflight-and-direct-staged-import",
                componentManifest.get("generatedLazyTriggerSource"),
                "Generated/lazy trigger source");
        requireEquals("window.Vaadin.Flow.loadOnDemand",
                componentManifest.get("generatedLazyApiOwner"),
                "Generated/lazy API owner");
        requireEquals("generated-flow-imports-dev-bundle-after-direct-staged-import",
                componentManifest.get("generatedLazyApiInstallSource"),
                "Generated/lazy API install source");
        requireEquals("vatori-flow-ipc-generated-lazy-v1_9-probe",
                componentManifest.get("generatedLazyRequestedChunk"),
                "Generated/lazy requested chunk");
        requireEquals("no-bootstrap-lazy-registry-observed",
                componentManifest.get("generatedLazyRegistrySource"),
                "Generated/lazy registry source");
        requireEquals("component-frontend-manifest-generated-lazy-v1_9",
                componentManifest.get("generatedLazyMappingSource"),
                "Generated/lazy mapping source");
        requireEquals("vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js",
                componentManifest.get("generatedLazyResolvedArtifactPath"),
                "Generated/lazy resolved artifact path");
        requireEquals(Boolean.FALSE, componentManifest.get("generatedLazyFlowClientTriggered"),
                "Generated/lazy Flow-client trigger flag");
        requireEquals(Boolean.TRUE, componentManifest.get("generatedLazyDiagnosticBridge"),
                "Generated/lazy diagnostic bridge flag");
        requireEquals("flow-ipc-loadondemand-v1_9",
                componentManifest.get("generatedLazyBlockerVersion"),
                "Generated/lazy blocker version");
        requireEquals("loadondemand-api-installed-by-generated-imports-not-flow-client-triggered",
                componentManifest.get("generatedLazyFailureLayer"),
                "Generated/lazy failure layer");
        requireEquals("flow-ipc-loadondemand-chunk-v1_11",
                componentManifest.get("loadOnDemandChunkContractVersion"),
                "Generated/lazy real UIDL loadOnDemand chunk contract version");
        requireEquals("bootstrap-uidl-dependencies",
                componentManifest.get("generatedLazyChunkExpressionSource"),
                "Generated/lazy real UIDL expression source");
        requireEquals("visible-flow-client-dependencyloader",
                componentManifest.get("generatedLazyChunkTriggerOwner"),
                "Generated/lazy real UIDL trigger owner");
        requireEquals("vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js",
                componentManifest.get("loadOnDemandChunkResolvedArtifactPath"),
                "Generated/lazy real UIDL resolved artifact path");
        requireEquals(Boolean.FALSE, componentManifest.get("generatedLazyChunkDiagnosticBridgeUsed"),
                "Generated/lazy real UIDL diagnostic bridge flag");
        requireEquals(Boolean.FALSE, componentManifest.get("generatedLazyChunkDirectImportProbeUsed"),
                "Generated/lazy real UIDL direct import probe flag");
        requireEquals("dev-bundle-generated-imports-loadondemand-no-op-no-chunk-file",
                componentManifest.get("loadOnDemandChunkFailureLayer"),
                "Generated/lazy real UIDL loadOnDemand blocker layer");
        requireEquals("flow-ipc-loadondemand-chunk-file-v1_12",
                componentManifest.get("generatedLoadOnDemandChunkFileRegistryVersion"),
                "Generated/lazy loadOnDemand chunk-file registry version");
        requireEquals("generated-flow-imports-body-parser-v1_12",
                componentManifest.get("generatedLoadOnDemandChunkFileRegistrySource"),
                "Generated/lazy loadOnDemand chunk-file registry source");
        requireEquals("real-generated-flow-imports-loadondemand-table-v1_12",
                componentManifest.get("generatedLoadOnDemandChunkFileMappingSource"),
                "Generated/lazy loadOnDemand chunk-file mapping source");
        requireEquals("vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js",
                componentManifest.get("generatedLoadOnDemandGeneratedImportsArtifact"),
                "Generated/lazy loadOnDemand generated imports artifact");
        requireEquals(Boolean.TRUE, componentManifest.get("generatedLoadOnDemandNoOpLoadOnDemand"),
                "Generated/lazy loadOnDemand no-op table flag");
        requireEquals("no-op-promise-resolve-0", componentManifest.get("loadOnDemandTableShape"),
                "Generated/lazy loadOnDemand table shape");
        requireEquals("generated-flow-imports-relative-chunk-file-resolver-v1_12",
                componentManifest.get("chunkFileResolver"),
                "Generated/lazy loadOnDemand chunk-file resolver");
        requireEquals("not-generated", componentManifest.get("chunkFileStatus"),
                "Generated/lazy loadOnDemand chunk-file status");
        requireEquals("chunk-file-not-generated", componentManifest.get("loadOnDemandChunkFileFailureLayer"),
                "Generated/lazy loadOnDemand chunk-file failure layer");
        requireEquals("flow-ipc-lazy-dependency-v1_10",
                componentManifest.get("flowLazyDependencyVersion"),
                "Flow lazy dependency version");
        requireEquals("flow-uidl-dependencyloader-loadmode-lazy-v1_10",
                componentManifest.get("flowLazyDependencySource"),
                "Flow lazy dependency source");
        requireEquals("LoadMode.LAZY", componentManifest.get("flowLazyDependencyMode"),
                "Flow lazy dependency mode");
        requireEquals("context://vatori-flow-ipc-lazy-v1_10.js",
                componentManifest.get("flowLazyDependencyExpression"),
                "Flow lazy dependency expression");
        requireEquals("vatori-flow-ipc-lazy-v1_10",
                componentManifest.get("flowLazyDependencyChunkId"),
                "Flow lazy dependency chunk id");
        requireEquals("vatori-flow-ipc-lazy-v1_10.js",
                componentManifest.get("flowLazyDependencyResolvedPath"),
                "Flow lazy dependency resolved path");
        requireEquals(Boolean.TRUE, componentManifest.get("flowLazyDependencyTriggeredByFlowClient"),
                "Flow lazy dependency Flow-client trigger flag");
        requireEquals("flow-ipc-production-runtime-v1_10",
                componentManifest.get("productionBundleRuntimeVersion"),
                "Production runtime version");
        requireEquals("vaadin-dev-bundle-stats-runtime-registry-v1_10",
                componentManifest.get("productionBundleRuntimeRegistrySource"),
                "Production runtime registry source");
        requireEquals("vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js",
                componentManifest.get("productionBundleRuntimeSelectedArtifact"),
                "Production runtime selected artifact");
        requireEquals("flow-ipc-production-owner-deps-v1_15",
                componentManifest.get("productionBundleRuntimeOwnerDependencyResolverVersion"),
                "Production owner dependency resolver version");
        requireEquals("vaadin-dev-bundle/webapp/VAADIN/build/",
                componentManifest.get("productionBundleRuntimeOwnerDependencyResolverRoot"),
                "Production owner dependency resolver root");
        requireEquals("owner-scoped-recursive-relative-import-graph-v1_15",
                componentManifest.get("productionBundleRuntimeOwnerDependencyResolverPolicy"),
                "Production owner dependency resolver policy");
        requireEquals("isolated-preflight,before-component-imports,after-component-imports",
                componentManifest.get("productionBundleRuntimeOwnerImportPhaseCandidates"),
                "Production owner import phase candidates");
        requireStringContains(componentManifest.get("productionBundleRuntimeOwnerImportDiagnosticModes"),
                "isolated-preflight:available-not-enabled",
                "Production owner import diagnostic modes");
        requireStringContains(componentManifest.get("productionBundleRuntimeOwnerDependencyResolverExpectedArtifacts"),
                "copilot-log-plugin-h3VQq9-2-BlujgixT.js",
                "Production owner dependency resolver expected artifacts");
        requireStringContains(componentManifest.get("productionBundleRuntimeOwnerDependencyResolverExpectedArtifacts"),
                "FlowClient-RTzSA_ve.js",
                "Production owner dependency resolver expected artifacts");
        requireEquals("production-owner-missing-real-artifact,production-owner-browser-import-failed,production-owner-duplicate-custom-element,production-owner-global-collision,production-owner-side-effect-unobservable",
                componentManifest.get("productionBundleRuntimeOwnerDependencyResolverAcceptableFailures"),
                "Production owner dependency resolver acceptable failures");
        requireEquals("flow-ipc-production-owner-ordering-v1_16",
                componentManifest.get("productionBundleRuntimeOwnerImportOrderingVersion"),
                "Production owner import ordering version");
        requireEquals("owner-first-before-component-imports-with-after-component-fallback-v1_16",
                componentManifest.get("productionBundleRuntimeOwnerImportPhasePolicy"),
                "Production owner import phase policy");
        requireStringContains(componentManifest.get("productionBundleRuntimeOwnerImportOrderingAcceptableFailures"),
                "production-owner-component-reuse-contract-missing",
                "Production owner import ordering acceptable failures");
        requireEquals("flow-ipc-component-owner-reuse-v1_16",
                componentManifest.get("componentProductionOwnerReuseVersion"),
                "Component owner reuse version");
        requireEquals("reuse-owner-owned-custom-elements-before-focused-imports-v1_16",
                componentManifest.get("componentProductionOwnerReusePolicy"),
                "Component owner reuse policy");
        requireStringContains(componentManifest.get("componentProductionOwnerReuseAcceptableFailures"),
                "owner-import-failed-before-component-import",
                "Component owner reuse acceptable failures");
        requireProductionOwnerResourceLoaderMetadata(componentManifest, true,
                "Component frontend manifest production owner resource loader");
        requireProductionOwnerSideEffectMetadata(componentManifest,
                "Component frontend manifest production owner side-effect");
        requireComponentResourceMetadata(componentResources);
        String checkboxDevBundlePath = "META-INF/vatori/dev-bundle-entry/vaadin-checkbox.js";
        String selectDevBundlePath = "META-INF/vatori/dev-bundle-entry/vaadin-select.js";
        String litDevBundlePath = "META-INF/vatori/dev-bundle-entry/lit-css.js";
        String textFieldDevBundlePath = "META-INF/vatori/dev-bundle-entry/vaadin-text-field.js";
        String defineDevBundlePath = "META-INF/vatori/dev-bundle-entry/component-base-define.js";
        String bigDecimalPath = "META-INF/resources/frontend/vaadin-big-decimal-field.js";
        String themeStaticPath = "META-INF/resources/frontend/themes/vatori-flow-ipc-smoke/theme-static.css";
        String generatedFlowImportsPath = "META-INF/vatori/generated/flow/generated-flow-imports.js";
        String generatedFlowImportsDevBundlePath =
                "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js";
        String generatedFlowImportsMarkdownHelperPath =
                "vaadin-dev-bundle/webapp/VAADIN/build/markdown-helpers-31I0A-hg.js";
        String devBundleIndexHtmlPath = "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js";
        String devModePackageLockPath = "vaadin-dev-bundle/package-lock.json";
        String productionBundleStatsPath = "vaadin-dev-bundle/config/stats.json";
        String flowLazyDependencyPath = "vatori-flow-ipc-lazy-v1_10.js";
        if (!resources.contains(bigDecimalPath)) {
            throw new IllegalStateException("Component frontend manifest is missing the BigDecimalField module");
        }
        Map<String, Object> bigDecimalResponse = resources.response(bigDecimalPath);
        if (!Objects.equals(bigDecimalResponse.get("lookupKind"), "BigDecimalField-minimum-component-resource")
                || !Objects.equals(bigDecimalResponse.get("componentTag"), "vaadin-big-decimal-field")
                || !Boolean.TRUE.equals(bigDecimalResponse.get("componentPrimary"))
                || !Objects.equals(bigDecimalResponse.get("artifactFamily"), "vaadin-component-classpath-frontend")
                || !Objects.equals(bigDecimalResponse.get("origin"), "classpath")
                || !Objects.equals(bigDecimalResponse.get("executionPolicy"), "execute-primary-module")
                || !Objects.equals(bigDecimalResponse.get("artifactStatus"), "available")
                || !Boolean.FALSE.equals(bigDecimalResponse.get("shim"))) {
            throw new IllegalStateException("BigDecimalField component frontend module is missing metadata");
        }
        if (!Objects.equals(bigDecimalResponse.get("importRewriteStrategy"), "manifest-import-rewrite-v1_4")
                || !Objects.equals(bigDecimalResponse.get("browserExecutionPlan"),
                        "dynamic-import-blob-with-manifest-import-rewrites-v1_4")) {
            throw new IllegalStateException("BigDecimalField component frontend module is missing import rewrite metadata");
        }
        requireImportRewrite(bigDecimalResponse, "lit", litDevBundlePath);
        requireImportRewrite(bigDecimalResponse, "@vaadin/text-field/src/vaadin-text-field.js",
                textFieldDevBundlePath);
        requireImportRewrite(bigDecimalResponse, "@vaadin/component-base/src/define.js",
                defineDevBundlePath);
        Object shimmedImports = bigDecimalResponse.get("shimmedImports");
        if (!(shimmedImports instanceof List<?> shimmedImportList)
                || !shimmedImportList.isEmpty()) {
            throw new IllegalStateException("BigDecimalField should not count TextField as a v1.7 shim");
        }
        if (!Boolean.TRUE.equals(bigDecimalResponse.get("realTextFieldGraphImport"))) {
            throw new IllegalStateException("BigDecimalField should import the real TextField graph");
        }
        if (!resources.contains(litDevBundlePath)) {
            throw new IllegalStateException("Component frontend manifest is missing the Lit css dev-bundle module");
        }
        Map<String, Object> litResponse = resources.response(litDevBundlePath);
        if (!Objects.equals(litResponse.get("lookupKind"), "Lit-css-dev-bundle-resource-v1_4")
                || !Objects.equals(litResponse.get("componentClass"), "lit")
                || !Boolean.TRUE.equals(litResponse.get("componentBundleDependency"))
                || !Boolean.TRUE.equals(litResponse.get("devBundleResource"))
                || !Objects.equals(litResponse.get("artifactFamily"), "production-bundle")
                || !Objects.equals(litResponse.get("origin"), "vaadin-dev-bundle-extracted-entry")
                || !Objects.equals(litResponse.get("executionPolicy"), "dependency-module")
                || !Objects.equals(litResponse.get("artifactStatus"), "partial")
                || !Boolean.FALSE.equals(litResponse.get("shim"))) {
            throw new IllegalStateException("Lit css dev-bundle module is missing metadata");
        }
        Object litBody = litResponse.get("body");
        if (!(litBody instanceof String litText)
                || !litText.contains("class CSSResult")
                || !litText.contains("export const css")
                || !litText.contains("export const unsafeCSS")) {
            throw new IllegalStateException("Lit css dev-bundle entry body is missing real export bridge");
        }
        if (!resources.contains(textFieldDevBundlePath)) {
            throw new IllegalStateException("Component frontend manifest is missing the TextField dev-bundle module");
        }
        Map<String, Object> textFieldResponse = resources.response(textFieldDevBundlePath);
        if (!Objects.equals(textFieldResponse.get("lookupKind"), "TextField-dev-bundle-resource-v1_4")
                || !Objects.equals(textFieldResponse.get("componentTag"), "vaadin-text-field")
                || !Objects.equals(textFieldResponse.get("componentClass"),
                        "com.vaadin.flow.component.textfield.TextField")
                || !Boolean.TRUE.equals(textFieldResponse.get("componentBundleDependency"))
                || !Boolean.TRUE.equals(textFieldResponse.get("devBundleResource"))
                || !Objects.equals(textFieldResponse.get("artifactFamily"), "production-bundle")
                || !Objects.equals(textFieldResponse.get("origin"), "vaadin-dev-bundle")
                || !Objects.equals(textFieldResponse.get("executionPolicy"), "dependency-module")
                || !Objects.equals(textFieldResponse.get("artifactStatus"), "partial")
                || !Boolean.FALSE.equals(textFieldResponse.get("shim"))
                || !Objects.equals(textFieldResponse.get("blockedExecutionProbe"),
                        "legacy-v1_6-big-decimal-import-rewrite-to-textfield-dev-bundle")
                || !(textFieldResponse.get("blockedExecutionError") instanceof String error
                        && error.equals("resolved-by-stable-module-identity-v1_7"))) {
            throw new IllegalStateException("TextField component frontend dev-bundle module is missing metadata");
        }
        if (!Objects.equals(textFieldResponse.get("textfieldGraphParityVersion"), "flow-ipc-textfield-graph-v1_6")
                || !Objects.equals(textFieldResponse.get("previousBlockerVersion"), "flow-ipc-textfield-v1_6")
                || !Objects.equals(textFieldResponse.get("executionVersion"), "flow-ipc-textfield-v1_7")
                || !Objects.equals(textFieldResponse.get("textfieldSideEffectVersion"),
                        "flow-ipc-textfield-side-effect-v1_7")
                || !Objects.equals(textFieldResponse.get("graphProbe"),
                        "direct-textfield-dev-bundle-entry-module-identity-v1_7")
                || !Objects.equals(textFieldResponse.get("importResolutionStrategy"),
                        "stable-resource-path-blob-module-identity-v1_7")
                || !Objects.equals(textFieldResponse.get("sideEffectStrategy"),
                        "reuse-production-indexhtml-module-identity-before-textfield-entry-import")
                || !Objects.equals(textFieldResponse.get("narrowedFailureClassification"),
                        "textfield-production-indexhtml-side-effect-deduped")
                || !Objects.equals(textFieldResponse.get("failureLayer"),
                        "resolved-v1_6-custom-element-registry-conflict")
                || !Objects.equals(textFieldResponse.get("classExportArtifact"),
                        "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                || !Objects.equals(textFieldResponse.get("duplicateTag"), "vaadin-text-field")
                || !Objects.equals(textFieldResponse.get("remainingBlockerClassification"), "none")
                || !Objects.equals(textFieldResponse.get("moduleIdentityStrategy"),
                        "stable-resource-path-blob-module-identity-v1_7")
                || !Boolean.FALSE.equals(textFieldResponse.get("legacyShimSuccessPath"))
                || !(textFieldResponse.get("minimalReproduction") instanceof String minimalReproduction
                        && minimalReproduction.contains("indexhtml-BfYYZi8S.js")
                        && minimalReproduction.contains("registry no longer throws duplicate definition"))
                || !(textFieldResponse.get("nextResolutionStep") instanceof String nextStep
                        && nextStep.contains("custom-scheme"))) {
            throw new IllegalStateException("TextField dev-bundle entry is missing v1.7 side-effect metadata");
        }
        requireListContains(textFieldResponse.get("transitiveImports"),
                "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js",
                "TextField transitive imports");
        Object textFieldBody = textFieldResponse.get("body");
        if (!(textFieldBody instanceof String textFieldText)
                || !textFieldText.contains("c7 as TextField")
                || !textFieldText.contains("export { defineCustomElement, TextField }")) {
            throw new IllegalStateException("TextField dev-bundle entry body is missing real export bridge");
        }
        if (!resources.contains(defineDevBundlePath)) {
            throw new IllegalStateException("Component frontend manifest is missing the component-base define dev-bundle module");
        }
        Map<String, Object> defineResponse = resources.response(defineDevBundlePath);
        if (!Objects.equals(defineResponse.get("lookupKind"), "Component-base-define-dev-bundle-resource-v1_4")
                || !Objects.equals(defineResponse.get("componentClass"), "@vaadin/component-base/src/define.js")
                || !Boolean.TRUE.equals(defineResponse.get("componentBundleDependency"))
                || !Boolean.TRUE.equals(defineResponse.get("devBundleResource"))
                || !Objects.equals(defineResponse.get("artifactFamily"), "production-bundle")
                || !Objects.equals(defineResponse.get("origin"), "vaadin-dev-bundle-extracted-entry")
                || !Objects.equals(defineResponse.get("executionPolicy"), "dependency-module")
                || !Objects.equals(defineResponse.get("artifactStatus"), "partial")
                || !Boolean.FALSE.equals(defineResponse.get("shim"))) {
            throw new IllegalStateException("component-base define dev-bundle module is missing metadata");
        }
        Object defineBody = defineResponse.get("body");
        if (!(defineBody instanceof String defineText)
                || !defineText.contains("export function defineCustomElement")
                || !defineText.contains("window.Vaadin.featureFlags")) {
            throw new IllegalStateException("component-base define dev-bundle entry body is missing real export bridge");
        }
        if (!resources.contains(productionBundleStatsPath)) {
            throw new IllegalStateException("Component frontend manifest is missing the production bundle stats graph");
        }
        Map<String, Object> productionBundleStatsResponse = resources.response(productionBundleStatsPath);
        if (!Objects.equals(productionBundleStatsResponse.get("lookupKind"),
                "Production-bundle-stats-resource-v1_4")
                || !Boolean.TRUE.equals(productionBundleStatsResponse.get("productionBundleResource"))
                || !Boolean.TRUE.equals(productionBundleStatsResponse.get("productionBundleStatsGraph"))
                || !Objects.equals(productionBundleStatsResponse.get("artifactFamily"), "production-bundle")
                || !Objects.equals(productionBundleStatsResponse.get("origin"), "vaadin-dev-bundle")
                || !Objects.equals(productionBundleStatsResponse.get("executionPolicy"),
                        "verified-only-intentionally-not-executed")
                || !Objects.equals(productionBundleStatsResponse.get("artifactStatus"), "available")
                || !Boolean.FALSE.equals(productionBundleStatsResponse.get("shim"))) {
            throw new IllegalStateException("Production bundle stats graph resource is missing v1.4 metadata");
        }
        Object productionBundleStatsBody = productionBundleStatsResponse.get("body");
        if (!(productionBundleStatsBody instanceof String statsText)
                || !statsText.contains("\"packageJsonDependencies\"")
                || !statsText.contains("\"npmModules\"")
                || !statsText.contains("\"bundleImports\"")
                || !statsText.contains("\"@vaadin/text-field\"")
                || !statsText.contains("\"@vaadin/checkbox\"")
                || !statsText.contains("\"@vaadin/select\"")
                || !statsText.contains("VAADIN/build/indexhtml-BfYYZi8S.js")) {
            throw new IllegalStateException("Production bundle stats graph body is missing Vaadin bundle evidence");
        }
        if (!Objects.equals(productionBundleStatsResponse.get("runtimeResolverContractVersion"),
                "flow-ipc-frontend-runtime-resolver-v1_10")
                || !Objects.equals(productionBundleStatsResponse.get("productionBundleRuntimeVersion"),
                        "flow-ipc-production-runtime-v1_10")
                || !Objects.equals(productionBundleStatsResponse.get("productionBundleRuntimeSelectedArtifact"),
                        devBundleIndexHtmlPath)
                || !Objects.equals(productionBundleStatsResponse.get("productionBundleRuntimeBlockerVersion"),
                        "flow-ipc-production-runtime-v1_10")
                || !Objects.equals(productionBundleStatsResponse.get("productionBundleRuntimeOwnerVersion"),
                        "flow-ipc-production-owner-v1_13")
                || !Objects.equals(productionBundleStatsResponse.get("productionBundleRuntimeOwner"),
                        "flow-ipc-production-bundle-runtime")
                || !Objects.equals(productionBundleStatsResponse.get("productionBundleRuntimeEntryArtifact"),
                        devBundleIndexHtmlPath)
                || !Objects.equals(productionBundleStatsResponse.get("productionBundleRuntimeOwnerFailureLayer"),
                        "production-owner-runtime-contract-missing")
                || !Objects.equals(productionBundleStatsResponse.get("productionBundleRuntimeOwnerImportVersion"),
                        "flow-ipc-production-owner-import-v1_14")
                || !Objects.equals(productionBundleStatsResponse.get("productionBundleRuntimeOwnerImportPolicy"),
                        "controlled-owner-dynamic-import-after-component-graph-v1_14")
                || !Objects.equals(productionBundleStatsResponse.get("productionBundleRuntimeOwnerImportArtifact"),
                        devBundleIndexHtmlPath)
                || !Objects.equals(
                        productionBundleStatsResponse.get("productionBundleRuntimeOwnerImportPhaseCandidates"),
                        "isolated-preflight,before-component-imports,after-component-imports")
                || !String.valueOf(
                        productionBundleStatsResponse.get("productionBundleRuntimeOwnerImportDiagnosticModes"))
                        .contains("before-component-imports:available-not-enabled")
                || !Objects.equals(
                        productionBundleStatsResponse.get("productionBundleRuntimeOwnerImportExpectedSideEffects"),
                        "window.Vaadin,window.Vaadin.Flow,window.Vaadin.Flow.loadOnDemand,customElements:vaadin-text-field|vaadin-checkbox|vaadin-select")
                || !Objects.equals(
                        productionBundleStatsResponse.get("productionBundleRuntimeOwnerImportAcceptableFailures"),
                        "production-owner-relative-import-unresolved,production-owner-browser-import-failed,production-owner-duplicate-custom-element,production-owner-global-collision,production-owner-side-effect-unobservable")
                || !Objects.equals(
                        productionBundleStatsResponse.get("productionBundleRuntimeOwnerDependencyResolverVersion"),
                        "flow-ipc-production-owner-deps-v1_15")
                || !Objects.equals(
                        productionBundleStatsResponse.get("productionBundleRuntimeOwnerDependencyResolverRoot"),
                        "vaadin-dev-bundle/webapp/VAADIN/build/")
                || !Objects.equals(
                        productionBundleStatsResponse.get("productionBundleRuntimeOwnerDependencyResolverPolicy"),
                        "owner-scoped-recursive-relative-import-graph-v1_15")
                || !Objects.equals(
                        productionBundleStatsResponse
                                .get("productionBundleRuntimeOwnerDependencyResolverAcceptableFailures"),
                        "production-owner-missing-real-artifact,production-owner-browser-import-failed,production-owner-duplicate-custom-element,production-owner-global-collision,production-owner-side-effect-unobservable")
                || !Objects.equals(
                        productionBundleStatsResponse.get("productionBundleRuntimeOwnerImportOrderingVersion"),
                        "flow-ipc-production-owner-ordering-v1_16")
                || !Objects.equals(productionBundleStatsResponse.get("productionBundleRuntimeOwnerImportPhasePolicy"),
                        "owner-first-before-component-imports-with-after-component-fallback-v1_16")
                || !Objects.equals(productionBundleStatsResponse.get("componentProductionOwnerReuseVersion"),
                        "flow-ipc-component-owner-reuse-v1_16")
                || !Objects.equals(productionBundleStatsResponse.get("componentProductionOwnerReusePolicy"),
                        "reuse-owner-owned-custom-elements-before-focused-imports-v1_16")) {
            throw new IllegalStateException("Production bundle stats graph is missing runtime owner metadata");
        }
        requireProductionOwnerResourceLoaderMetadata(productionBundleStatsResponse, true,
                "Production bundle stats owner resource loader");
        requireProductionOwnerSideEffectMetadata(productionBundleStatsResponse,
                "Production bundle stats owner side-effect");
        requireStringContains(
                productionBundleStatsResponse.get("productionBundleRuntimeOwnerDependencyResolverExpectedArtifacts"),
                "copilot-log-plugin-h3VQq9-2-BlujgixT.js",
                "Production bundle stats dependency resolver expected artifacts");
        Map<String, Object> productionRuntimeResponse = resources.response(devBundleIndexHtmlPath);
        if (!Boolean.TRUE.equals(productionRuntimeResponse.get("productionBundleRuntimeArtifact"))
                || !Objects.equals(productionRuntimeResponse.get("runtimeResolverContractVersion"),
                        "flow-ipc-frontend-runtime-resolver-v1_10")
                || !Objects.equals(productionRuntimeResponse.get("logicalRequestId"),
                        "production-runtime:indexhtml:v1_10")
                || !Objects.equals(productionRuntimeResponse.get("productionBundleRuntimeVersion"),
                        "flow-ipc-production-runtime-v1_10")
                || !Objects.equals(productionRuntimeResponse.get("productionBundleRuntimeRegistrySource"),
                        "vaadin-dev-bundle-stats-runtime-registry-v1_10")
                || !Objects.equals(productionRuntimeResponse.get("productionBundleRuntimeSelectedArtifact"),
                        devBundleIndexHtmlPath)
                || !Objects.equals(productionRuntimeResponse.get("productionBundleRuntimeBlockerVersion"),
                        "flow-ipc-production-runtime-v1_10")
                || !Objects.equals(productionRuntimeResponse.get("productionBundleRuntimeExecutionPolicy"),
                        "stage-execution-attempt-production-bundle-runtime-v1_10")
                || !Objects.equals(productionRuntimeResponse.get("productionBundleRuntimeOwnerVersion"),
                        "flow-ipc-production-owner-v1_13")
                || !Objects.equals(productionRuntimeResponse.get("productionBundleRuntimeOwner"),
                        "flow-ipc-production-bundle-runtime")
                || !Objects.equals(productionRuntimeResponse.get("productionBundleRuntimeOwnerModuleIdentity"),
                        "production-owner-v1_13:" + devBundleIndexHtmlPath)
                || !Objects.equals(productionRuntimeResponse.get("productionBundleRuntimeOwnerExecutionPolicy"),
                        "stage-owner-contract-production-bundle-runtime-v1_13")
                || !Objects.equals(productionRuntimeResponse.get("productionBundleRuntimeOwnerBlockerVersion"),
                        "flow-ipc-production-owner-v1_13")
                || !Objects.equals(productionRuntimeResponse.get("productionBundleRuntimeOwnerFailureLayer"),
                        "production-owner-runtime-contract-missing")
                || !Objects.equals(productionRuntimeResponse.get("productionBundleRuntimeOwnerImportVersion"),
                        "flow-ipc-production-owner-import-v1_14")
                || !Objects.equals(productionRuntimeResponse.get("productionBundleRuntimeOwnerImportPolicy"),
                        "controlled-owner-dynamic-import-after-component-graph-v1_14")
                || !Objects.equals(productionRuntimeResponse.get("productionBundleRuntimeOwnerImportArtifact"),
                        devBundleIndexHtmlPath)
                || !Objects.equals(
                        productionRuntimeResponse.get("productionBundleRuntimeOwnerImportPhaseCandidates"),
                        "isolated-preflight,before-component-imports,after-component-imports")
                || !String.valueOf(
                        productionRuntimeResponse.get("productionBundleRuntimeOwnerImportDiagnosticModes"))
                        .contains("isolated-preflight:available-not-enabled")
                || !Objects.equals(
                        productionRuntimeResponse.get("productionBundleRuntimeOwnerImportExpectedSideEffects"),
                        "window.Vaadin,window.Vaadin.Flow,window.Vaadin.Flow.loadOnDemand,customElements:vaadin-text-field|vaadin-checkbox|vaadin-select")
                || !Objects.equals(
                        productionRuntimeResponse.get("productionBundleRuntimeOwnerImportAcceptableFailures"),
                        "production-owner-relative-import-unresolved,production-owner-browser-import-failed,production-owner-duplicate-custom-element,production-owner-global-collision,production-owner-side-effect-unobservable")
                || !Objects.equals(
                        productionRuntimeResponse.get("productionBundleRuntimeOwnerDependencyResolverVersion"),
                        "flow-ipc-production-owner-deps-v1_15")
                || !Objects.equals(
                        productionRuntimeResponse.get("productionBundleRuntimeOwnerDependencyResolverRoot"),
                        "vaadin-dev-bundle/webapp/VAADIN/build/")
                || !Objects.equals(
                        productionRuntimeResponse.get("productionBundleRuntimeOwnerDependencyResolverPolicy"),
                        "owner-scoped-recursive-relative-import-graph-v1_15")
                || !Objects.equals(
                        productionRuntimeResponse
                                .get("productionBundleRuntimeOwnerDependencyResolverAcceptableFailures"),
                        "production-owner-missing-real-artifact,production-owner-browser-import-failed,production-owner-duplicate-custom-element,production-owner-global-collision,production-owner-side-effect-unobservable")
                || !Objects.equals(
                        productionRuntimeResponse.get("productionBundleRuntimeOwnerImportOrderingVersion"),
                        "flow-ipc-production-owner-ordering-v1_16")
                || !Objects.equals(productionRuntimeResponse.get("productionBundleRuntimeOwnerImportPhasePolicy"),
                        "owner-first-before-component-imports-with-after-component-fallback-v1_16")
                || !Objects.equals(productionRuntimeResponse.get("componentProductionOwnerReuseVersion"),
                        "flow-ipc-component-owner-reuse-v1_16")
                || !Objects.equals(productionRuntimeResponse.get("componentProductionOwnerReusePolicy"),
                        "reuse-owner-owned-custom-elements-before-focused-imports-v1_16")
                || !Boolean.FALSE.equals(productionRuntimeResponse.get("componentProductionOwnerReused"))) {
            throw new IllegalStateException("Production bundle runtime artifact is missing runtime owner metadata");
        }
        requireProductionOwnerResourceLoaderMetadata(productionRuntimeResponse, true,
                "Production runtime owner resource loader");
        requireProductionOwnerSideEffectMetadata(productionRuntimeResponse,
                "Production runtime owner side-effect");
        requireStringContains(
                productionRuntimeResponse.get("productionBundleRuntimeOwnerDependencyResolverExpectedArtifacts"),
                "FlowClient-RTzSA_ve.js",
                "Production runtime dependency resolver expected artifacts");
        String ownerDependencyPath =
                "vaadin-dev-bundle/webapp/VAADIN/build/copilot-log-plugin-h3VQq9-2-BlujgixT.js";
        if (!resources.contains(ownerDependencyPath)) {
            throw new IllegalStateException("Component frontend manifest is missing production owner dependency "
                    + ownerDependencyPath);
        }
        Map<String, Object> ownerDependencyResponse = resources.response(ownerDependencyPath);
        if (!Objects.equals(ownerDependencyResponse.get("lookupKind"), "Production-owner-dependency-resource-v1_15")
                || !Boolean.TRUE.equals(ownerDependencyResponse.get("productionOwnerDependencyResource"))
                || !Objects.equals(ownerDependencyResponse.get("artifactFamily"), "production-bundle")
                || !Objects.equals(ownerDependencyResponse.get("executionPolicy"), "owner-dependency-module-v1_15")
                || !Objects.equals(ownerDependencyResponse.get("productionBundleRuntimeOwnerDependencyResolverVersion"),
                        "flow-ipc-production-owner-deps-v1_15")
                || !Objects.equals(ownerDependencyResponse.get("productionBundleRuntimeOwnerDependencyResolverRoot"),
                        "vaadin-dev-bundle/webapp/VAADIN/build/")
                || !Objects.equals(
                        ownerDependencyResponse.get("productionBundleRuntimeOwnerDependencyResolverClassification"),
                        "resolved-existing-artifact")) {
            throw new IllegalStateException("Production owner dependency resource is missing v1.15 metadata");
        }
        requireProductionOwnerResourceLoaderMetadata(ownerDependencyResponse, false,
                "Production owner dependency resource loader");
        requireProductionOwnerSideEffectMetadata(ownerDependencyResponse,
                "Production owner dependency side-effect");
        if (!resources.contains(checkboxDevBundlePath)) {
            throw new IllegalStateException("Component frontend manifest is missing the checkbox dev-bundle module");
        }
        Map<String, Object> checkboxResponse = resources.response(checkboxDevBundlePath);
        if (!Objects.equals(checkboxResponse.get("lookupKind"), "Checkbox-dev-bundle-resource-v1_4")
                || !Objects.equals(checkboxResponse.get("componentTag"), "vaadin-checkbox")
                || !Boolean.TRUE.equals(checkboxResponse.get("componentPrimary"))
                || !Boolean.TRUE.equals(checkboxResponse.get("devBundleResource"))
                || !Objects.equals(checkboxResponse.get("artifactFamily"), "production-bundle")
                || !Objects.equals(checkboxResponse.get("origin"), "vaadin-dev-bundle-focused-entry")
                || !Objects.equals(checkboxResponse.get("executionPolicy"), "execute-dev-bundle-entry")
                || !Objects.equals(checkboxResponse.get("artifactStatus"), "partial")
                || !Boolean.FALSE.equals(checkboxResponse.get("shim"))) {
            throw new IllegalStateException("Checkbox component frontend dev-bundle module is missing metadata");
        }
        if (!resources.contains(themeStaticPath)) {
            throw new IllegalStateException("Component frontend manifest is missing the theme/static resource");
        }
        if (!resources.contains(selectDevBundlePath)) {
            throw new IllegalStateException("Component frontend manifest is missing the Select dev-bundle module");
        }
        Map<String, Object> selectResponse = resources.response(selectDevBundlePath);
        if (!Objects.equals(selectResponse.get("lookupKind"), "Select-dev-bundle-resource-v1_4")
                || !Objects.equals(selectResponse.get("componentTag"), "vaadin-select")
                || !Objects.equals(selectResponse.get("componentClass"),
                        "com.vaadin.flow.component.select.Select")
                || !Boolean.TRUE.equals(selectResponse.get("componentPrimary"))
                || !Boolean.TRUE.equals(selectResponse.get("devBundleResource"))
                || !Objects.equals(selectResponse.get("artifactFamily"), "production-bundle")
                || !Objects.equals(selectResponse.get("origin"), "vaadin-dev-bundle-focused-entry")
                || !Objects.equals(selectResponse.get("executionPolicy"), "execute-dev-bundle-entry")
                || !Objects.equals(selectResponse.get("artifactStatus"), "partial")
                || !Boolean.FALSE.equals(selectResponse.get("shim"))) {
            throw new IllegalStateException("Select component frontend dev-bundle module is missing metadata");
        }
        Map<String, Object> themeStaticResponse = resources.response(themeStaticPath);
        if (!Objects.equals(themeStaticResponse.get("lookupKind"), "Theme-static-resource-v1_4")
                || !Boolean.TRUE.equals(themeStaticResponse.get("themeStaticResource"))
                || !Objects.equals(themeStaticResponse.get("artifactFamily"), "theme-static")
                || !Objects.equals(themeStaticResponse.get("origin"), "classpath")
                || !Objects.equals(themeStaticResponse.get("executionPolicy"), "verify-and-inject-static-style")
                || !Objects.equals(themeStaticResponse.get("artifactStatus"), "available")
                || !Boolean.FALSE.equals(themeStaticResponse.get("shim"))) {
            throw new IllegalStateException("Theme/static frontend resource is missing v1.4 metadata");
        }
        if (!resources.contains(generatedFlowImportsPath)) {
            throw new IllegalStateException("Component frontend manifest is missing generated Flow imports");
        }
        Map<String, Object> generatedFlowImportsResponse = resources.response(generatedFlowImportsPath);
        if (!Objects.equals(generatedFlowImportsResponse.get("lookupKind"),
                "Generated-flow-imports-resource-v1_4")
                || !Boolean.TRUE.equals(generatedFlowImportsResponse.get("generatedLazyResource"))
                || !Objects.equals(generatedFlowImportsResponse.get("artifactFamily"), "generated-lazy")
                || !Objects.equals(generatedFlowImportsResponse.get("origin"), "repository-generated-flow-imports")
                || !Objects.equals(generatedFlowImportsResponse.get("executionPolicy"),
                        "verified-only-intentionally-not-executed")
                || !Objects.equals(generatedFlowImportsResponse.get("artifactStatus"), "available")
                || !Boolean.FALSE.equals(generatedFlowImportsResponse.get("shim"))) {
            throw new IllegalStateException("Generated Flow imports resource is missing v1.4 metadata");
        }
        Object generatedFlowImportsBody = generatedFlowImportsResponse.get("body");
        if (!(generatedFlowImportsBody instanceof String generatedFlowImportsText)
                || !generatedFlowImportsText.contains("@vaadin/button/src/vaadin-button.js")
                || !generatedFlowImportsText.contains("window.Vaadin.Flow.loadOnDemand")) {
            throw new IllegalStateException("Generated Flow imports resource body is missing Vaadin generated imports");
        }
        if (!resources.contains(generatedFlowImportsDevBundlePath)) {
            throw new IllegalStateException("Component frontend manifest is missing the dev-bundle generated Flow imports chunk");
        }
        Map<String, Object> generatedFlowImportsDevBundleResponse =
                resources.response(generatedFlowImportsDevBundlePath);
        if (!Objects.equals(generatedFlowImportsDevBundleResponse.get("lookupKind"),
                "Generated-flow-imports-dev-bundle-chunk-v1_4")
                || !Boolean.TRUE.equals(generatedFlowImportsDevBundleResponse.get("generatedLazyResource"))
                || !Boolean.TRUE.equals(generatedFlowImportsDevBundleResponse.get("generatedLazyBundleChunk"))
                || !Boolean.TRUE.equals(generatedFlowImportsDevBundleResponse.get("devBundleResource"))
                || !Boolean.TRUE.equals(generatedFlowImportsDevBundleResponse.get("productionBundleResource"))
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("artifactFamily"), "generated-lazy")
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("origin"), "vaadin-dev-bundle")
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("executionPolicy"),
                        "stage-and-import-generated-lazy-chunk-v1_8")
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("artifactStatus"), "partial")
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("generatedLazyResolverVersion"),
                        "flow-ipc-generated-lazy-resolver-v1_8")
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("generatedLazyExecutionVersion"),
                        "flow-ipc-generated-lazy-v1_8")
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("generatedLazyImportResolutionStrategy"),
                        "stable-relative-import-rewrite-generated-lazy-v1_8")
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("generatedLazyRelativeImportSpecifier"),
                        "./markdown-helpers-31I0A-hg.js")
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("generatedLazyResolvedDependencyPath"),
                        generatedFlowImportsMarkdownHelperPath)
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("generatedLazyLoadOnDemandContractVersion"),
                        "flow-ipc-loadondemand-v1_9")
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("generatedLazyTriggerSource"),
                        "diagnostic-preflight-and-direct-staged-import")
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("generatedLazyApiOwner"),
                        "window.Vaadin.Flow.loadOnDemand")
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("generatedLazyApiInstallSource"),
                        "generated-flow-imports-dev-bundle-after-direct-staged-import")
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("generatedLazyRequestedChunk"),
                        "vatori-flow-ipc-generated-lazy-v1_9-probe")
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("generatedLazyRegistrySource"),
                        "no-bootstrap-lazy-registry-observed")
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("generatedLazyMappingSource"),
                        "component-frontend-manifest-generated-lazy-v1_9")
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("generatedLazyResolvedArtifactPath"),
                        generatedFlowImportsDevBundlePath)
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("generatedLazyBlockerVersion"),
                        "flow-ipc-loadondemand-v1_9")
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("loadOnDemandChunkContractVersion"),
                        "flow-ipc-loadondemand-chunk-v1_11")
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("generatedLazyChunkExpressionSource"),
                        "bootstrap-uidl-dependencies")
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("generatedLazyChunkTriggerOwner"),
                        "visible-flow-client-dependencyloader")
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("loadOnDemandChunkResolvedArtifactPath"),
                        generatedFlowImportsDevBundlePath)
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("loadOnDemandChunkBlockerVersion"),
                        "flow-ipc-loadondemand-chunk-v1_11")
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("loadOnDemandChunkFailureLayer"),
                        "dev-bundle-generated-imports-loadondemand-no-op-no-chunk-file")
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("generatedLoadOnDemandChunkFileRegistryVersion"),
                        "flow-ipc-loadondemand-chunk-file-v1_12")
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("generatedLoadOnDemandChunkFileRegistrySource"),
                        "generated-flow-imports-body-parser-v1_12")
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("generatedLoadOnDemandChunkFileMappingSource"),
                        "real-generated-flow-imports-loadondemand-table-v1_12")
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("generatedLoadOnDemandGeneratedImportsArtifact"),
                        generatedFlowImportsDevBundlePath)
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("generatedLoadOnDemandSourceBuildMode"),
                        "vaadin-dev-bundle-production-build")
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("generatedLoadOnDemandSourceArtifactFamily"),
                        "generated-lazy")
                || !Boolean.TRUE.equals(generatedFlowImportsDevBundleResponse.get("generatedLoadOnDemandNoOpLoadOnDemand"))
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("loadOnDemandTableShape"),
                        "no-op-promise-resolve-0")
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("chunkFileResolver"),
                        "generated-flow-imports-relative-chunk-file-resolver-v1_12")
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("chunkFileStatus"),
                        "not-generated")
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("chunkFileBytes"), 0)
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("loadOnDemandChunkFileFailureLayer"),
                        "chunk-file-not-generated")
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("loadOnDemandChunkFileBlockerVersion"),
                        "flow-ipc-loadondemand-chunk-file-v1_12")
                || !(generatedFlowImportsDevBundleResponse.get("noChunkGeneratedReason") instanceof String noChunkReason
                        && noChunkReason.contains("Promise.resolve(0)"))
                || !(generatedFlowImportsDevBundleResponse.get("loadOnDemandChunkFileNextResolutionStep")
                        instanceof String chunkFileNextStep
                        && chunkFileNextStep.contains("./chunks/chunk-*.js"))
                || !Boolean.FALSE.equals(
                        generatedFlowImportsDevBundleResponse.get("generatedLazyChunkDiagnosticBridgeUsed"))
                || !Boolean.FALSE.equals(
                        generatedFlowImportsDevBundleResponse.get("generatedLazyChunkDirectImportProbeUsed"))
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("failureLayer"),
                        "loadondemand-api-installed-by-generated-imports-not-flow-client-triggered")
                || !Boolean.FALSE.equals(generatedFlowImportsDevBundleResponse.get("generatedLazyFlowClientTriggered"))
                || !Boolean.TRUE.equals(generatedFlowImportsDevBundleResponse.get("generatedLazyDiagnosticBridge"))
                || !Boolean.FALSE.equals(generatedFlowImportsDevBundleResponse.get("shim"))) {
            throw new IllegalStateException("Generated Flow imports dev-bundle chunk is missing v1.9 metadata");
        }
        Object generatedFlowImportsDevBundleBody = generatedFlowImportsDevBundleResponse.get("body");
        if (!(generatedFlowImportsDevBundleBody instanceof String generatedFlowImportsDevBundleText)
                || !generatedFlowImportsDevBundleText.contains("from \"./indexhtml-BfYYZi8S.js\"")
                || !generatedFlowImportsDevBundleText.contains("defineCustomElement(FieldOutline)")
                || !generatedFlowImportsDevBundleText.contains("renderMarkdownToElement")
                || !generatedFlowImportsDevBundleText.contains("case \"vaadin-text-field\"")
                || !generatedFlowImportsDevBundleText.contains("case \"vaadin-checkbox\"")
                || !generatedFlowImportsDevBundleText.contains("case \"vaadin-select\"")) {
            throw new IllegalStateException("Generated Flow imports dev-bundle chunk body is missing Vaadin generated imports");
        }
        if (!resources.contains(generatedFlowImportsMarkdownHelperPath)) {
            throw new IllegalStateException("Component frontend manifest is missing the generated Flow imports markdown helper");
        }
        Map<String, Object> generatedFlowImportsMarkdownHelperResponse =
                resources.response(generatedFlowImportsMarkdownHelperPath);
        if (!Objects.equals(generatedFlowImportsMarkdownHelperResponse.get("lookupKind"),
                "Generated-flow-imports-dev-bundle-dependency-v1_4")
                || !Boolean.TRUE.equals(generatedFlowImportsMarkdownHelperResponse.get("generatedLazyResource"))
                || !Boolean.TRUE.equals(generatedFlowImportsMarkdownHelperResponse.get("generatedLazyBundleDependency"))
                || !Objects.equals(generatedFlowImportsMarkdownHelperResponse.get("artifactFamily"), "generated-lazy")
                || !Objects.equals(generatedFlowImportsMarkdownHelperResponse.get("origin"), "vaadin-dev-bundle")
                || !Objects.equals(generatedFlowImportsMarkdownHelperResponse.get("dependencyOf"),
                        generatedFlowImportsDevBundlePath)
                || !Objects.equals(generatedFlowImportsMarkdownHelperResponse.get("executionPolicy"),
                        "stable-dependency-module-generated-lazy-v1_8")
                || !Objects.equals(generatedFlowImportsMarkdownHelperResponse.get("artifactStatus"), "partial")
                || !Objects.equals(generatedFlowImportsMarkdownHelperResponse.get("generatedLazyResolverVersion"),
                        "flow-ipc-generated-lazy-resolver-v1_8")
                || !Objects.equals(generatedFlowImportsMarkdownHelperResponse.get("generatedLazyExecutionVersion"),
                        "flow-ipc-generated-lazy-v1_8")
                || !Objects.equals(generatedFlowImportsMarkdownHelperResponse.get("generatedLazyImportResolutionStrategy"),
                        "stable-relative-import-rewrite-generated-lazy-v1_8")
                || !Objects.equals(generatedFlowImportsMarkdownHelperResponse.get("generatedLazyLoadOnDemandContractVersion"),
                        "flow-ipc-loadondemand-v1_9")
                || !Objects.equals(generatedFlowImportsMarkdownHelperResponse.get("generatedLazyApiOwner"),
                        "window.Vaadin.Flow.loadOnDemand")
                || !Objects.equals(generatedFlowImportsMarkdownHelperResponse.get("generatedLazyResolvedArtifactPath"),
                        generatedFlowImportsMarkdownHelperPath)
                || !Objects.equals(generatedFlowImportsMarkdownHelperResponse.get("generatedLazyBlockerVersion"),
                        "flow-ipc-loadondemand-v1_9")
                || !Boolean.FALSE.equals(generatedFlowImportsMarkdownHelperResponse.get("generatedLazyFlowClientTriggered"))
                || !Boolean.FALSE.equals(generatedFlowImportsMarkdownHelperResponse.get("generatedLazyDiagnosticBridge"))
                || !Boolean.FALSE.equals(generatedFlowImportsMarkdownHelperResponse.get("shim"))) {
            throw new IllegalStateException("Generated Flow imports markdown helper is missing v1.9 metadata");
        }
        Object generatedFlowImportsMarkdownHelperBody = generatedFlowImportsMarkdownHelperResponse.get("body");
        if (!(generatedFlowImportsMarkdownHelperBody instanceof String markdownHelperText)
                || !markdownHelperText.contains("DOMPurify")
                || !markdownHelperText.contains("function renderMarkdownToElement")
                || !markdownHelperText.contains("export {\n  renderMarkdownToElement\n};")) {
            throw new IllegalStateException("Generated Flow imports markdown helper body is missing Vaadin helper exports");
        }
        if (!resources.contains(flowLazyDependencyPath)) {
            throw new IllegalStateException("Component frontend manifest is missing the v1.10 Flow lazy dependency");
        }
        Map<String, Object> flowLazyDependencyResponse = resources.response(flowLazyDependencyPath);
        requireFrontendResourceLoaderMetadata(flowLazyDependencyResponse, "Flow lazy dependency response");
        requireFrontendResolverContractMetadata(flowLazyDependencyResponse, "Flow lazy dependency response");
        if (!Objects.equals(flowLazyDependencyResponse.get("lookupKind"),
                "Flow-lazy-dependency-resource-v1_10")
                || !Boolean.TRUE.equals(flowLazyDependencyResponse.get("flowLazyDependencyResource"))
                || !Objects.equals(flowLazyDependencyResponse.get("runtimeResolverContractVersion"),
                        "flow-ipc-frontend-runtime-resolver-v1_10")
                || !Objects.equals(flowLazyDependencyResponse.get("artifactFamily"), "flow-lazy-dependency")
                || !Objects.equals(flowLazyDependencyResponse.get("executionPolicy"),
                        "execute-after-flow-dependencyloader-trigger-v1_10")
                || !Objects.equals(flowLazyDependencyResponse.get("lazyDependencyVersion"),
                        "flow-ipc-lazy-dependency-v1_10")
                || !Objects.equals(flowLazyDependencyResponse.get("lazyDependencySource"),
                        "flow-uidl-dependencyloader-loadmode-lazy-v1_10")
                || !Objects.equals(flowLazyDependencyResponse.get("lazyDependencyMode"), "LoadMode.LAZY")
                || !Objects.equals(flowLazyDependencyResponse.get("lazyDependencyExpression"),
                        "context://vatori-flow-ipc-lazy-v1_10.js")
                || !Objects.equals(flowLazyDependencyResponse.get("lazyDependencyResolvedPath"),
                        flowLazyDependencyPath)
                || !Objects.equals(flowLazyDependencyResponse.get("lazyDependencyTriggerOwner"),
                        "visible-flow-client-dependencyloader")
                || !Objects.equals(flowLazyDependencyResponse.get("stageUrlPolicy"),
                        "host-owned-resource-scheme-file-backed-v1_17")
                || !Objects.equals(flowLazyDependencyResponse.get("cleanupOwnership"),
                        "cleanup-owned-window-lifetime-file-backed-v1_17")
                || !Objects.equals(flowLazyDependencyResponse.get("frontendResourceLoaderExecutionSlice"),
                        "flow-lazy-dependency-v1_17")
                || !Objects.equals(flowLazyDependencyResponse.get("frontendResourceLoaderExpectedStagingMode"),
                        "custom-scheme")
                || !Objects.equals(flowLazyDependencyResponse.get("frontendResourceLoaderMimeType"),
                        "text/javascript; charset=utf-8")
                || !Boolean.TRUE.equals(flowLazyDependencyResponse.get("lazyDependencyTriggeredByFlowClient"))
                || !Boolean.FALSE.equals(flowLazyDependencyResponse.get("shim"))) {
            throw new IllegalStateException("Flow lazy dependency resource is missing v1.10 metadata");
        }
        Object flowLazyDependencyBody = flowLazyDependencyResponse.get("body");
        if (!(flowLazyDependencyBody instanceof String lazyDependencyText)
                || !lazyDependencyText.contains("VaadinDesktopIpcLazyDependencyProbe")
                || !lazyDependencyText.contains("flow-uidl-dependencyloader-loadmode-lazy-v1_10")) {
            throw new IllegalStateException("Flow lazy dependency body is missing runtime probe evidence");
        }
        if (!resources.contains(devModePackageLockPath)) {
            throw new IllegalStateException("Component frontend manifest is missing the dev-mode package graph");
        }
        Map<String, Object> devModePackageLockResponse = resources.response(devModePackageLockPath);
        if (!Objects.equals(devModePackageLockResponse.get("lookupKind"),
                "Dev-mode-package-lock-resource-v1_4")
                || !Boolean.TRUE.equals(devModePackageLockResponse.get("devModeBundleResource"))
                || !Boolean.TRUE.equals(devModePackageLockResponse.get("devModePackageGraph"))
                || !Objects.equals(devModePackageLockResponse.get("artifactFamily"), "dev-mode-bundle")
                || !Objects.equals(devModePackageLockResponse.get("origin"), "vaadin-dev-bundle")
                || !Objects.equals(devModePackageLockResponse.get("executionPolicy"),
                        "verified-only-intentionally-not-executed")
                || !Objects.equals(devModePackageLockResponse.get("artifactStatus"), "available")
                || !Boolean.FALSE.equals(devModePackageLockResponse.get("shim"))) {
            throw new IllegalStateException("Dev-mode package graph resource is missing v1.4 metadata");
        }
        Object devModePackageLockBody = devModePackageLockResponse.get("body");
        if (!(devModePackageLockBody instanceof String packageLockText)
                || !packageLockText.contains("\"devDependencies\"")
                || !packageLockText.contains("\"vite\"")
                || !packageLockText.contains("\"@vitejs/plugin-react\"")
                || !packageLockText.contains("\"@vaadin/text-field\"")
                || !packageLockText.contains("\"@vaadin/checkbox\"")) {
            throw new IllegalStateException("Dev-mode package graph body is missing Vaadin/Vite dependency evidence");
        }
        System.out.println("Flow client resources loaded from classpath: " + entries.size());
        System.out.println("Flow client resource graph responses verified: " + entries.size());
        System.out.println("Component frontend resources loaded: " + componentResources.size());
        System.out.println("BigDecimal component classpath resource available: "
                + resources.contains(bigDecimalPath));
        System.out.println("FlowBootstrap.js bytes: " + bootstrapResponse.get("bytes"));
        System.out.println("FlowClient.js bytes: " + clientResponse.get("bytes"));
        System.out.println("FlowClient.js IPC bridge: " + clientResponse.get("ipcAugmented"));
    }

    private static void requireFrontendResourceLoaderMetadata(Map<String, Object> metadata, String label) {
        requireEquals("flow-ipc-resource-scheme-v1_17",
                metadata.get("frontendResourceLoaderVersion"),
                label + " frontend resource loader version");
        requireEquals("custom-scheme,file-backed,blob-fallback",
                metadata.get("frontendResourceLoaderModes"),
                label + " frontend resource loader modes");
        requireEquals("custom-scheme-file-backed",
                metadata.get("frontendResourceLoaderPreferredMode"),
                label + " frontend resource loader preferred mode");
        requireEquals(Boolean.TRUE,
                metadata.get("frontendResourceLoaderNoLocalhost"),
                label + " frontend resource loader no-localhost flag");
        requireStringContains(metadata.get("frontendResourceLoaderAcceptableFallbacks"),
                "production-owner-blob-fallback",
                label + " frontend resource loader acceptable fallbacks");
    }

    private static void requireFrontendResolverContractMetadata(Map<?, ?> metadata, String label) {
        requireEquals("flow-ipc-frontend-resolver-contract-v1_21",
                metadata.get("frontendResolverContractVersion"),
                label + " frontend resolver contract version");
        requireEquals("flow-client,component-frontend,theme-static,generated-lazy,flow-lazy-dependency,production-owner,dev-mode",
                metadata.get("frontendResolverContractScope"),
                label + " frontend resolver contract scope");
        requireEquals("stable-source-path-plus-owner-namespace-v1_21",
                metadata.get("frontendResolverIdentityPolicy"),
                label + " frontend resolver identity policy");
        requireEquals("sha256-required-for-bodied-resources-v1_21",
                metadata.get("frontendResolverIntegrityPolicy"),
                label + " frontend resolver integrity policy");
        requireEquals("window-lifetime-content-addressed-v1_21",
                metadata.get("frontendResolverCachePolicy"),
                label + " frontend resolver cache policy");
        requireEquals("host-owned-window-lifetime-v1_21",
                metadata.get("frontendResolverCleanupPolicy"),
                label + " frontend resolver cleanup policy");
        requireEquals("blob,vatori-resource",
                metadata.get("frontendResolverAllowedSchemes"),
                label + " frontend resolver allowed schemes");
        requireEquals(Boolean.TRUE,
                metadata.get("frontendResolverNoLocalhost"),
                label + " frontend resolver no-localhost flag");
        requireStringContains(metadata.get("frontendResolverFamiliesCovered"),
                "flow-client-classpath",
                label + " frontend resolver covered families");
        requireStringContains(metadata.get("frontendResolverFamiliesCovered"),
                "production-bundle",
                label + " frontend resolver covered families");
        requireStringContains(metadata.get("frontendResolverAcceptableGaps"),
                "production-owner-owned-missing-real-artifact",
                label + " frontend resolver acceptable gaps");
    }

    private static void requireProductionOwnerResourceLoaderMetadata(Map<String, Object> metadata,
            boolean rootArtifact, String label) {
        requireEquals("flow-ipc-production-owner-resource-loader-v1_18",
                metadata.get("productionBundleRuntimeOwnerResourceLoaderVersion"),
                label + " version");
        requireEquals("custom-scheme-file-backed",
                metadata.get("productionBundleRuntimeOwnerResourceLoaderMode"),
                label + " mode");
        requireEquals("vatori-resource",
                metadata.get("productionBundleRuntimeOwnerResourceScheme"),
                label + " scheme");
        requireEquals("owner-root-and-resolved-dependencies",
                metadata.get("productionBundleRuntimeOwnerResourceGraphMode"),
                label + " graph mode");
        requireEquals("stable-owner-resource-path-base-v1_18",
                metadata.get("productionBundleRuntimeOwnerResourceBasePolicy"),
                label + " base policy");
        requireEquals("production-owner",
                metadata.get("productionBundleRuntimeOwnerResourceLoaderNamespace"),
                label + " namespace");
        requireEquals("vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js",
                metadata.get("productionBundleRuntimeOwnerResourceLoaderRootArtifact"),
                label + " root artifact");
        requireEquals(rootArtifact ? "root" : "dependency",
                metadata.get("productionBundleRuntimeOwnerResourceLoaderArtifactRole"),
                label + " artifact role");
        requireEquals(Boolean.FALSE,
                metadata.get("productionBundleRuntimeOwnerBlobFallbackAllowed"),
                label + " root blob fallback flag");
        requireEquals("explicit-classified-only",
                metadata.get("productionBundleRuntimeOwnerDependencyBlobFallbackPolicy"),
                label + " dependency blob fallback policy");
        requireEquals(Boolean.TRUE,
                metadata.get("productionBundleRuntimeOwnerResourceLoaderNoLocalhost"),
                label + " no-localhost flag");
        requireEquals("host-owned-custom-scheme-stable-source-path-v1_18",
                metadata.get("productionBundleRuntimeOwnerResourceLoaderStageUrlPolicy"),
                label + " stage URL policy");
        requireStringContains(metadata.get("productionBundleRuntimeOwnerResourceLoaderAcceptableFailures"),
                "production-owner-custom-scheme-missing-real-artifact",
                label + " acceptable failures");
        requireStringContains(metadata.get("productionBundleRuntimeOwnerResourceLoaderExpectedArtifacts"),
                "copilot-log-plugin-h3VQq9-2-BlujgixT.js",
                label + " expected artifacts");
    }

    private static void requireProductionOwnerSideEffectMetadata(Map<String, Object> metadata, String label) {
        requireEquals("flow-ipc-production-owner-side-effect-v1_19",
                metadata.get("productionBundleRuntimeOwnerSideEffectVersion"),
                label + " version");
        requireEquals("custom-scheme-owner-import-side-effect-contract-v1_19",
                metadata.get("productionBundleRuntimeOwnerSideEffectPolicy"),
                label + " policy");
        requireEquals("vaadin-text-field,vaadin-checkbox,vaadin-select",
                metadata.get("productionBundleRuntimeOwnerSideEffectExpectedTags"),
                label + " expected tags");
        requireEquals("window.Vaadin,window.Vaadin.Flow,window.Vaadin.Flow.loadOnDemand",
                metadata.get("productionBundleRuntimeOwnerSideEffectExpectedGlobals"),
                label + " expected globals");
        requireStringContains(metadata.get("productionBundleRuntimeOwnerSideEffectAcceptableFailures"),
                "production-owner-custom-scheme-runtime-side-effect-blocked",
                label + " acceptable failures");
        requireEquals(Boolean.TRUE,
                metadata.get("productionBundleRuntimeOwnerSideEffectNoBlobFallback"),
                label + " no-blob fallback flag");
        requireEquals("flow-ipc-component-owner-reuse-decision-v1_19",
                metadata.get("componentProductionOwnerReuseDecisionVersion"),
                label + " component reuse decision version");
        requireEquals("production-owner-custom-scheme-side-effect-v1_19",
                metadata.get("componentProductionOwnerReuseDecisionSource"),
                label + " component reuse decision source");
        requireEquals("pending-runtime-side-effect-check",
                metadata.get("componentProductionOwnerReuseDecision"),
                label + " component reuse decision");
        requireStringContains(metadata.get("componentProductionOwnerReuseDecisionTags"),
                "vaadin-text-field",
                label + " component reuse decision tags");
        requireEquals("flow-ipc-production-owner-owned-runtime-v1_20",
                metadata.get("productionBundleRuntimeOwnerOwnedRuntimeVersion"),
                label + " owner-owned runtime version");
        requireEquals("owner-first-custom-scheme-runtime-reuse-v1_20",
                metadata.get("productionBundleRuntimeOwnerOwnedRuntimePolicy"),
                label + " owner-owned runtime policy");
        requireEquals("before-component-imports",
                metadata.get("productionBundleRuntimeOwnerOwnedRuntimePhase"),
                label + " owner-owned runtime phase");
        requireEquals("vaadin-text-field,vaadin-checkbox,vaadin-select",
                metadata.get("productionBundleRuntimeOwnerOwnedRuntimeExpectedTags"),
                label + " owner-owned runtime expected tags");
        requireEquals("window.Vaadin,window.Vaadin.Flow,window.Vaadin.Flow.loadOnDemand",
                metadata.get("productionBundleRuntimeOwnerOwnedRuntimeExpectedGlobals"),
                label + " owner-owned runtime expected globals");
        requireEquals(Boolean.TRUE,
                metadata.get("productionBundleRuntimeOwnerOwnedRuntimeNoBlobFallback"),
                label + " owner-owned runtime no-blob fallback flag");
        requireStringContains(metadata.get("productionBundleRuntimeOwnerOwnedRuntimeAcceptableFailures"),
                "production-owner-owned-side-effect-unobservable",
                label + " owner-owned runtime acceptable failures");
        requireEquals(Boolean.FALSE,
                metadata.get("productionBundleRuntimeOwnerOwnedRuntimeAttempted"),
                label + " owner-owned runtime attempted default");
        requireEquals(Boolean.FALSE,
                metadata.get("productionBundleRuntimeOwnerOwnedRuntimeSatisfied"),
                label + " owner-owned runtime satisfied default");
        requireEquals("pending-owner-first-custom-scheme-runtime-reuse-v1_20",
                metadata.get("productionBundleRuntimeOwnerOwnedRuntimeFailureLayer"),
                label + " owner-owned runtime pending layer");
        requireEquals("flow-ipc-component-owner-reuse-policy-v1_20",
                metadata.get("componentProductionOwnerReusePolicyVersionV1_20"),
                label + " component reuse policy version");
        requireEquals("owner-first-then-focused-fallback-v1_20",
                metadata.get("componentProductionOwnerReusePolicyV1_20"),
                label + " component reuse policy");
        requireEquals("before-component-imports",
                metadata.get("componentProductionOwnerReuseDecisionPhase"),
                label + " component reuse decision phase");
    }

    private static void requireEquals(Object expected, Object actual, String label) {
        if (!Objects.equals(expected, actual)) {
            throw new IllegalStateException(label + " mismatch: expected=" + expected + ", actual=" + actual);
        }
    }

    private static String requirePresentString(Object value, String label) {
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalStateException(label + " is missing");
        }
        return text;
    }

    private static void requireListContains(Object value, String expected, String label) {
        if (!(value instanceof List<?> items) || !items.contains(expected)) {
            throw new IllegalStateException(label + " does not contain " + expected + ": " + value);
        }
    }

    private static void requireStringContains(Object value, String expected, String label) {
        if (!(value instanceof String text) || !text.contains(expected)) {
            throw new IllegalStateException(label + " does not contain " + expected + ": " + value);
        }
    }

    private static void requireStrictBlocker(Map<String, Object> manifest, String family, String classification) {
        Object value = manifest.get("strictBlockers");
        if (!(value instanceof List<?> blockers)) {
            throw new IllegalStateException("Component frontend manifest is missing strictBlockers");
        }
        for (Object blocker : blockers) {
            if (blocker instanceof Map<?, ?> metadata
                    && Objects.equals(metadata.get("artifactFamily"), family)
                    && Objects.equals(metadata.get("failureClassification"), classification)
                    && Boolean.TRUE.equals(metadata.get("strictBlocker"))
                    && metadata.get("blockedReason") instanceof String reason
                    && !reason.isBlank()) {
                return;
            }
        }
        throw new IllegalStateException("Component frontend manifest is missing strict blocker for " + family);
    }

    private static void requireNoStrictBlocker(Map<String, Object> manifest, String family) {
        Object value = manifest.get("strictBlockers");
        if (!(value instanceof List<?> blockers)) {
            throw new IllegalStateException("Component frontend manifest is missing strictBlockers");
        }
        for (Object blocker : blockers) {
            if (blocker instanceof Map<?, ?> metadata
                    && Objects.equals(metadata.get("artifactFamily"), family)
                    && Boolean.TRUE.equals(metadata.get("strictBlocker"))) {
                throw new IllegalStateException("Component frontend manifest should not contain strict blocker for "
                        + family + ": " + metadata);
            }
        }
    }

    private static void requireImportRewrite(Map<String, Object> metadata, String specifier, String resourcePath) {
        Object value = metadata.get("importRewrites");
        if (!(value instanceof List<?> rewrites)) {
            throw new IllegalStateException("Component frontend resource is missing importRewrites");
        }
        for (Object rewrite : rewrites) {
            if (rewrite instanceof Map<?, ?> entry
                    && Objects.equals(entry.get("specifier"), specifier)
                    && Objects.equals(entry.get("resourcePath"), resourcePath)
                    && entry.get("resolver") instanceof String resolver
                    && !resolver.isBlank()
                    && entry.get("failureClassification") instanceof String classification
                    && !classification.isBlank()) {
                return;
            }
        }
        throw new IllegalStateException("Component frontend resource is missing import rewrite for " + specifier);
    }

    private static void requireComponentResourceMetadata(List<?> componentResources) {
        boolean classpathResourceObserved = false;
        boolean legacyShimFallbackObserved = false;
        boolean devBundleEntryObserved = false;
        boolean devBundleDependencyObserved = false;
        boolean productionBundleStatsObserved = false;
        boolean themeStaticResourceObserved = false;
        boolean generatedLazyResourceObserved = false;
        boolean generatedLazyBundleChunkObserved = false;
        boolean generatedLoadOnDemandChunkObserved = false;
        boolean devModePackageGraphObserved = false;
        boolean flowLazyDependencyObserved = false;
        boolean productionRuntimeArtifactObserved = false;
        boolean productionRuntimeOwnerObserved = false;
        boolean productionRuntimeOwnerImportObserved = false;
        boolean productionRuntimeOwnerDependencyObserved = false;
        for (Object entry : componentResources) {
            if (!(entry instanceof Map<?, ?> metadata)) {
                throw new IllegalStateException("Component frontend resource manifest contains a non-object entry");
            }
            String artifactFamily = requirePresentString(metadata.get("artifactFamily"),
                    "Component frontend artifact family");
            requirePresentString(metadata.get("origin"), "Component frontend origin");
            requirePresentString(metadata.get("moduleSpecifier"), "Component frontend module specifier");
            requirePresentString(metadata.get("resolver"), "Component frontend resolver");
            requirePresentString(metadata.get("dependencyOf"), "Component frontend dependencyOf");
            String executionPolicy = requirePresentString(metadata.get("executionPolicy"),
                    "Component frontend execution policy");
            requirePresentString(metadata.get("artifactStatus"), "Component frontend artifact status");
            requirePresentString(metadata.get("failureClassification"),
                    "Component frontend failure classification");
            requirePresentString(metadata.get("blockedReason"), "Component frontend blocked reason");
            classpathResourceObserved = classpathResourceObserved
                    || "vaadin-component-classpath-frontend".equals(artifactFamily);
            legacyShimFallbackObserved = legacyShimFallbackObserved
                    || "npm-frontend".equals(artifactFamily)
                    && "legacy-shim-fallback".equals(executionPolicy)
                    && Boolean.TRUE.equals(metadata.get("shim"))
                    && Boolean.TRUE.equals(metadata.get("legacyFallback"))
                    && Boolean.FALSE.equals(metadata.get("successPath"));
            devBundleEntryObserved = devBundleEntryObserved
                    || "production-bundle".equals(artifactFamily)
                    && "execute-dev-bundle-entry".equals(executionPolicy);
            devBundleDependencyObserved = devBundleDependencyObserved
                    || "production-bundle".equals(artifactFamily)
                    && "dependency-module".equals(executionPolicy);
            productionBundleStatsObserved = productionBundleStatsObserved
                    || "production-bundle".equals(artifactFamily)
                    && "verified-only-intentionally-not-executed".equals(executionPolicy)
                    && "available".equals(metadata.get("artifactStatus"))
                    && Boolean.TRUE.equals(metadata.get("productionBundleStatsGraph"));
            themeStaticResourceObserved = themeStaticResourceObserved
                    || "theme-static".equals(artifactFamily)
                    && "verify-and-inject-static-style".equals(executionPolicy)
                    && "available".equals(metadata.get("artifactStatus"));
            generatedLazyResourceObserved = generatedLazyResourceObserved
                    || "generated-lazy".equals(artifactFamily)
                    && ("verified-only-intentionally-not-executed".equals(executionPolicy)
                            || executionPolicy.startsWith("stage-and-import-generated-lazy-chunk")
                            || executionPolicy.startsWith("stable-dependency-module-generated-lazy"))
                    && ("available".equals(metadata.get("artifactStatus"))
                            || "partial".equals(metadata.get("artifactStatus")))
                    && Boolean.TRUE.equals(metadata.get("generatedLazyResource"));
            generatedLazyBundleChunkObserved = generatedLazyBundleChunkObserved
                    || "generated-lazy".equals(artifactFamily)
                    && "stage-and-import-generated-lazy-chunk-v1_8".equals(executionPolicy)
                    && "partial".equals(metadata.get("artifactStatus"))
                    && Boolean.TRUE.equals(metadata.get("generatedLazyBundleChunk"))
                    && "vaadin-dev-bundle".equals(metadata.get("origin"))
                    && "flow-ipc-generated-lazy-resolver-v1_8".equals(metadata.get("generatedLazyResolverVersion"))
                    && "flow-ipc-loadondemand-v1_9".equals(
                            metadata.get("generatedLazyLoadOnDemandContractVersion"))
                    && "diagnostic-preflight-and-direct-staged-import".equals(
                            metadata.get("generatedLazyTriggerSource"))
                    && Boolean.FALSE.equals(metadata.get("generatedLazyFlowClientTriggered"))
                    && Boolean.TRUE.equals(metadata.get("generatedLazyDiagnosticBridge"));
            generatedLoadOnDemandChunkObserved = generatedLoadOnDemandChunkObserved
                    || "generated-lazy".equals(artifactFamily)
                    && "stage-and-import-generated-lazy-chunk-v1_8".equals(executionPolicy)
                    && "flow-ipc-loadondemand-chunk-v1_11".equals(
                            metadata.get("loadOnDemandChunkContractVersion"))
                    && "bootstrap-uidl-dependencies".equals(metadata.get("generatedLazyChunkExpressionSource"))
                    && "visible-flow-client-dependencyloader".equals(metadata.get("generatedLazyChunkTriggerOwner"))
                    && "flow-ipc-loadondemand-chunk-v1_11".equals(
                            metadata.get("loadOnDemandChunkBlockerVersion"))
                    && Boolean.FALSE.equals(metadata.get("generatedLazyChunkDiagnosticBridgeUsed"))
                    && Boolean.FALSE.equals(metadata.get("generatedLazyChunkDirectImportProbeUsed"));
            devModePackageGraphObserved = devModePackageGraphObserved
                    || "dev-mode-bundle".equals(artifactFamily)
                    && "verified-only-intentionally-not-executed".equals(executionPolicy)
                    && "available".equals(metadata.get("artifactStatus"))
                    && Boolean.TRUE.equals(metadata.get("devModePackageGraph"));
            flowLazyDependencyObserved = flowLazyDependencyObserved
                    || "flow-lazy-dependency".equals(artifactFamily)
                    && "execute-after-flow-dependencyloader-trigger-v1_10".equals(executionPolicy)
                    && "available".equals(metadata.get("artifactStatus"))
                    && Boolean.TRUE.equals(metadata.get("flowLazyDependencyResource"))
                    && "flow-ipc-frontend-runtime-resolver-v1_10".equals(
                            metadata.get("runtimeResolverContractVersion"))
                    && "flow-ipc-lazy-dependency-v1_10".equals(metadata.get("lazyDependencyVersion"))
                    && Boolean.TRUE.equals(metadata.get("lazyDependencyTriggeredByFlowClient"));
            productionRuntimeArtifactObserved = productionRuntimeArtifactObserved
                    || "production-bundle".equals(artifactFamily)
                    && Boolean.TRUE.equals(metadata.get("productionBundleRuntimeArtifact"))
                    && "flow-ipc-frontend-runtime-resolver-v1_10".equals(
                            metadata.get("runtimeResolverContractVersion"))
                    && "flow-ipc-production-runtime-v1_10".equals(
                            metadata.get("productionBundleRuntimeVersion"))
                    && "stage-execution-attempt-production-bundle-runtime-v1_10".equals(
                            metadata.get("productionBundleRuntimeExecutionPolicy"));
            productionRuntimeOwnerObserved = productionRuntimeOwnerObserved
                    || "production-bundle".equals(artifactFamily)
                    && Boolean.TRUE.equals(metadata.get("productionBundleRuntimeArtifact"))
                    && "flow-ipc-production-owner-v1_13".equals(
                            metadata.get("productionBundleRuntimeOwnerVersion"))
                    && "flow-ipc-production-bundle-runtime".equals(
                            metadata.get("productionBundleRuntimeOwner"))
                    && "stage-owner-contract-production-bundle-runtime-v1_13".equals(
                            metadata.get("productionBundleRuntimeOwnerExecutionPolicy"))
                    && "production-owner-runtime-contract-missing".equals(
                            metadata.get("productionBundleRuntimeOwnerFailureLayer"))
                    && Boolean.FALSE.equals(metadata.get("componentProductionOwnerReused"));
            productionRuntimeOwnerImportObserved = productionRuntimeOwnerImportObserved
                    || "production-bundle".equals(artifactFamily)
                    && Boolean.TRUE.equals(metadata.get("productionBundleRuntimeArtifact"))
                    && "flow-ipc-production-owner-import-v1_14".equals(
                            metadata.get("productionBundleRuntimeOwnerImportVersion"))
                    && "controlled-owner-dynamic-import-after-component-graph-v1_14".equals(
                            metadata.get("productionBundleRuntimeOwnerImportPolicy"))
                    && "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js".equals(
                            metadata.get("productionBundleRuntimeOwnerImportArtifact"))
                    && "production-owner-relative-import-unresolved,production-owner-browser-import-failed,production-owner-duplicate-custom-element,production-owner-global-collision,production-owner-side-effect-unobservable".equals(
                            metadata.get("productionBundleRuntimeOwnerImportAcceptableFailures"))
                    && Boolean.FALSE.equals(metadata.get("productionBundleRuntimeOwnerImportAttempted"))
                    && Boolean.FALSE.equals(metadata.get("productionBundleRuntimeOwnerImported"));
            productionRuntimeOwnerDependencyObserved = productionRuntimeOwnerDependencyObserved
                    || "production-bundle".equals(artifactFamily)
                    && "owner-dependency-module-v1_15".equals(executionPolicy)
                    && Boolean.TRUE.equals(metadata.get("productionOwnerDependencyResource"))
                    && "flow-ipc-production-owner-deps-v1_15".equals(
                            metadata.get("productionBundleRuntimeOwnerDependencyResolverVersion"))
                    && "owner-scoped-recursive-relative-import-graph-v1_15".equals(
                            metadata.get("productionBundleRuntimeOwnerDependencyResolverPolicy"))
                    && "resolved-existing-artifact".equals(
                            metadata.get("productionBundleRuntimeOwnerDependencyResolverClassification"));
        }
        if (!classpathResourceObserved || !legacyShimFallbackObserved || !devBundleEntryObserved
                || !devBundleDependencyObserved || !productionBundleStatsObserved || !themeStaticResourceObserved
                || !generatedLazyResourceObserved || !generatedLazyBundleChunkObserved || !devModePackageGraphObserved
                || !generatedLoadOnDemandChunkObserved || !flowLazyDependencyObserved
                || !productionRuntimeArtifactObserved || !productionRuntimeOwnerObserved
                || !productionRuntimeOwnerImportObserved || !productionRuntimeOwnerDependencyObserved) {
            throw new IllegalStateException("Component frontend resource manifest is missing a v1.4 artifact family");
        }
    }
}
