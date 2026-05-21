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
        if (!(componentEntries instanceof List<?> componentResources) || componentResources.size() < 7) {
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
        requireStrictBlocker(componentManifest, "theme-static", "theme-static-resource-not-declared");
        requireStrictBlocker(componentManifest, "dev-mode-bundle", "dev-mode-bundle-resource-graph-unmapped");
        requireStrictBlocker(componentManifest, "generated-lazy", "generated-lazy-resource-unmapped");
        requireComponentResourceMetadata(componentResources);
        String checkboxDevBundlePath = "META-INF/vatori/dev-bundle-entry/vaadin-checkbox.js";
        String bigDecimalPath = "META-INF/resources/frontend/vaadin-big-decimal-field.js";
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

    private static void requireComponentResourceMetadata(List<?> componentResources) {
        boolean classpathResourceObserved = false;
        boolean shimBlockerObserved = false;
        boolean devBundleEntryObserved = false;
        boolean devBundleDependencyObserved = false;
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
        }
        if (!classpathResourceObserved || !shimBlockerObserved || !devBundleEntryObserved
                || !devBundleDependencyObserved) {
            throw new IllegalStateException("Component frontend resource manifest is missing a v1.4 artifact family");
        }
    }
}
