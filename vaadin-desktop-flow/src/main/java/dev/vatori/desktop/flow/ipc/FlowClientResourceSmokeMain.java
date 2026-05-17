package dev.vatori.desktop.flow.ipc;

import java.util.List;
import java.util.Map;

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
        if (!resources.contains("META-INF/frontend/FlowBootstrap.js")) {
            throw new IllegalStateException("FlowBootstrap.js was not found on the Flow IPC runtime classpath");
        }

        Map<String, Object> response = resources.response("META-INF/frontend/FlowBootstrap.js");
        Object body = response.get("body");
        if (!(body instanceof String text)
                || !text.contains("window.Vaadin.Flow.initApplication")
                || !text.contains("export { init }")) {
            throw new IllegalStateException("FlowBootstrap.js did not expose the expected Flow bootstrap entry");
        }
        System.out.println("Flow client resources loaded from classpath: " + entries.size());
        System.out.println("FlowBootstrap.js bytes: " + response.get("bytes"));
    }
}
