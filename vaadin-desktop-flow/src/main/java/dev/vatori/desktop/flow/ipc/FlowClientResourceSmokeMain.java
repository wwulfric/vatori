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
            if (!resources.contains(path)) {
                throw new IllegalStateException("Manifest resource is not available from FlowClientResources: " + path);
            }
            Map<String, Object> response = resources.response(path);
            requireEquals(path, response.get("path"), path + " response path");
            requireEquals(metadata.get("kind"), response.get("kind"), path + " response kind");
            requireEquals(metadata.get("bytes"), response.get("bytes"), path + " response byte count");
            requireEquals(metadata.get("sha256"), response.get("sha256"), path + " response sha256");
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
        System.out.println("Flow client resources loaded from classpath: " + entries.size());
        System.out.println("Flow client resource graph responses verified: " + entries.size());
        System.out.println("FlowBootstrap.js bytes: " + bootstrapResponse.get("bytes"));
        System.out.println("FlowClient.js bytes: " + clientResponse.get("bytes"));
        System.out.println("FlowClient.js IPC bridge: " + clientResponse.get("ipcAugmented"));
    }

    private static void requireEquals(Object expected, Object actual, String label) {
        if (!Objects.equals(expected, actual)) {
            throw new IllegalStateException(label + " mismatch: expected=" + expected + ", actual=" + actual);
        }
    }
}
