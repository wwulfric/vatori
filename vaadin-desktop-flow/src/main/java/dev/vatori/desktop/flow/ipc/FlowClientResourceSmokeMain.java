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
            requirePresentString(metadata.get("executionPolicy"), path + " execution policy");
            requireEquals("available", metadata.get("artifactStatus"), path + " artifact status");
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
            requireEquals(metadata.get("executionPolicy"), response.get("executionPolicy"),
                    path + " response execution policy");
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
        requireStrictBlocker(componentManifest, "npm-frontend", "textfield-production-class-execution-blocked");
        requireStrictBlocker(componentManifest, "dev-mode-bundle", "dev-mode-bundle-execution-unmapped");
        requireStrictBlocker(componentManifest, "generated-lazy", "generated-lazy-execution-unmapped");
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
        String devModePackageLockPath = "vaadin-dev-bundle/package-lock.json";
        String productionBundleStatsPath = "vaadin-dev-bundle/config/stats.json";
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
                "META-INF/vatori/frontend-shims/vaadin-text-field.js");
        requireImportRewrite(bigDecimalResponse, "@vaadin/component-base/src/define.js",
                defineDevBundlePath);
        Object shimmedImports = bigDecimalResponse.get("shimmedImports");
        if (!(shimmedImports instanceof List<?> shimmedImportList)
                || !shimmedImportList.equals(List.of("@vaadin/text-field/src/vaadin-text-field.js"))) {
            throw new IllegalStateException("BigDecimalField should report only TextField as remaining shim");
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
                        "big-decimal-import-rewrite-to-textfield-dev-bundle")
                || !(textFieldResponse.get("blockedExecutionError") instanceof String error
                        && error.contains("outlet._root"))) {
            throw new IllegalStateException("TextField component frontend dev-bundle module is missing metadata");
        }
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
                        "verified-only-intentionally-not-executed")
                || !Objects.equals(generatedFlowImportsDevBundleResponse.get("artifactStatus"), "available")
                || !Boolean.FALSE.equals(generatedFlowImportsDevBundleResponse.get("shim"))) {
            throw new IllegalStateException("Generated Flow imports dev-bundle chunk is missing v1.4 metadata");
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
                        "verified-only-intentionally-not-executed")
                || !Objects.equals(generatedFlowImportsMarkdownHelperResponse.get("artifactStatus"), "available")
                || !Boolean.FALSE.equals(generatedFlowImportsMarkdownHelperResponse.get("shim"))) {
            throw new IllegalStateException("Generated Flow imports markdown helper is missing v1.4 metadata");
        }
        Object generatedFlowImportsMarkdownHelperBody = generatedFlowImportsMarkdownHelperResponse.get("body");
        if (!(generatedFlowImportsMarkdownHelperBody instanceof String markdownHelperText)
                || !markdownHelperText.contains("DOMPurify")
                || !markdownHelperText.contains("function renderMarkdownToElement")
                || !markdownHelperText.contains("export {\n  renderMarkdownToElement\n};")) {
            throw new IllegalStateException("Generated Flow imports markdown helper body is missing Vaadin helper exports");
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
        boolean shimBlockerObserved = false;
        boolean devBundleEntryObserved = false;
        boolean devBundleDependencyObserved = false;
        boolean productionBundleStatsObserved = false;
        boolean themeStaticResourceObserved = false;
        boolean generatedLazyResourceObserved = false;
        boolean generatedLazyBundleChunkObserved = false;
        boolean devModePackageGraphObserved = false;
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
            shimBlockerObserved = shimBlockerObserved
                    || "npm-frontend".equals(artifactFamily)
                    && "blocked-shim-fallback".equals(executionPolicy)
                    && Boolean.TRUE.equals(metadata.get("shim"));
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
                    && "verified-only-intentionally-not-executed".equals(executionPolicy)
                    && "available".equals(metadata.get("artifactStatus"))
                    && Boolean.TRUE.equals(metadata.get("generatedLazyResource"));
            generatedLazyBundleChunkObserved = generatedLazyBundleChunkObserved
                    || "generated-lazy".equals(artifactFamily)
                    && "verified-only-intentionally-not-executed".equals(executionPolicy)
                    && "available".equals(metadata.get("artifactStatus"))
                    && Boolean.TRUE.equals(metadata.get("generatedLazyBundleChunk"))
                    && "vaadin-dev-bundle".equals(metadata.get("origin"));
            devModePackageGraphObserved = devModePackageGraphObserved
                    || "dev-mode-bundle".equals(artifactFamily)
                    && "verified-only-intentionally-not-executed".equals(executionPolicy)
                    && "available".equals(metadata.get("artifactStatus"))
                    && Boolean.TRUE.equals(metadata.get("devModePackageGraph"));
        }
        if (!classpathResourceObserved || !shimBlockerObserved || !devBundleEntryObserved
                || !devBundleDependencyObserved || !productionBundleStatsObserved || !themeStaticResourceObserved
                || !generatedLazyResourceObserved || !generatedLazyBundleChunkObserved || !devModePackageGraphObserved) {
            throw new IllegalStateException("Component frontend resource manifest is missing a v1.4 artifact family");
        }
    }
}
