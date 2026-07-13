package dev.vatori.desktop.flow.ipc;

import java.util.Map;

final class FlowIpcFrontendBuildEnvironment {
    private static final String PREFIX = "VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_";

    private final Map<String, String> values;

    private FlowIpcFrontendBuildEnvironment(Map<String, String> values) {
        this.values = Map.copyOf(values);
    }

    static FlowIpcFrontendBuildEnvironment current() {
        return new FlowIpcFrontendBuildEnvironment(System.getenv());
    }

    String buildValue(String name) {
        return values.get(PREFIX + name);
    }

    String buildValue(String preferredName, String fallbackName) {
        String preferred = buildValue(preferredName);
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return buildValue(fallbackName);
    }

    boolean flag(String name) {
        return "1".equals(buildValue(name));
    }

    boolean anyFlagEnabled(String... names) {
        for (String name : names) {
            if (flag(name)) {
                return true;
            }
        }
        return false;
    }
}
