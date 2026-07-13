package dev.vatori.desktop.flow.ipc;

import java.util.LinkedHashMap;
import java.util.Map;

final class FlowIpcStaticResourceAttributes {
    private FlowIpcStaticResourceAttributes() {
    }

    static Map<String, Object> devModePackageLock(String path) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("componentFrontendResource", true);
        attributes.put("devModeBundleResource", true);
        attributes.put("devModePackageGraph", true);
        attributes.put("packageManager", "npm");
        attributes.put("moduleSpecifier", path);
        attributes.put("resolver", "vaadin-dev-bundle-package-graph-v1_4");
        attributes.put("shim", false);
        attributes.put("artifactFamily", "dev-mode-bundle");
        attributes.put("origin", "vaadin-dev-bundle");
        attributes.put("resourceOrigin", "vaadin-dev-bundle-package-lock");
        attributes.put("dependencyOf", "dev-mode-frontend-build");
        attributes.put("executionPolicy", "verified-only-intentionally-not-executed");
        attributes.put("artifactStatus", "available");
        attributes.put("requestedBy", "component-frontend-manifest-v1_4");
        attributes.put("failureClassification", "dev-mode-package-lock-verified");
        attributes.put("blockedReason",
                "verified Vaadin dev-mode package-lock dependency graph; Vite/dev-server module execution remains unmapped");
        attributes.put("lookupKind", "Dev-mode-package-lock-resource-v1_4");
        attributes.put("devModeExecutionReady", false);
        return attributes;
    }

    static Map<String, Object> themeStatic(String path) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("componentFrontendResource", true);
        attributes.put("themeStaticResource", true);
        attributes.put("themeName", "vatori-flow-ipc-smoke");
        attributes.put("moduleSpecifier", path);
        attributes.put("resolver", "theme-static-classpath-resource-v1_4");
        attributes.put("shim", false);
        attributes.put("artifactFamily", "theme-static");
        attributes.put("origin", "classpath");
        attributes.put("resourceOrigin", "theme-static-classpath-resource");
        attributes.put("dependencyOf", "theme:vatori-flow-ipc-smoke");
        attributes.put("executionPolicy", "verify-and-inject-static-style");
        attributes.put("artifactStatus", "available");
        attributes.put("requestedBy", "component-frontend-manifest-v1_4");
        attributes.put("failureClassification", "theme-static-classpath-resource");
        attributes.put("blockedReason", "resolved theme/static classpath resource for the v1.4 no-localhost lookup path");
        attributes.put("lookupKind", "Theme-static-resource-v1_4");
        return attributes;
    }

    static Map<String, Object> flowClient(String path, String kind) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("flowClientResource", true);
        attributes.put("artifactFamily", "flow-client-classpath");
        attributes.put("origin", "classpath");
        attributes.put("resourceOrigin", "flow-client-classpath");
        attributes.put("moduleSpecifier", path);
        attributes.put("resolver", "flow-client-classpath-resource");
        String executionPolicy = flowClientExecutionPolicy(kind);
        attributes.put("dependencyOf", flowClientDependencyOf(kind));
        attributes.put("executionPolicy", executionPolicy);
        attributes.put("artifactStatus", "available");
        attributes.put("failureClassification", "classpath-flow-client-resource");
        attributes.put("requestedBy", "flow-client-manifest-v1_4");
        if (!"execute-primary-module".equals(executionPolicy)) {
            attributes.put("blockedReason",
                    "Flow client classpath resource is verified over IPC but intentionally not executed by the current dev-ipc runtime slice");
        }
        return attributes;
    }

    private static String flowClientExecutionPolicy(String kind) {
        if ("flow-bootstrap-module".equals(kind) || "flow-client-module".equals(kind)) {
            return "execute-primary-module";
        }
        return "verified-only-intentionally-not-executed";
    }

    private static String flowClientDependencyOf(String kind) {
        return switch (kind) {
            case "flow-bootstrap-module" -> "flow-client-bootstrap";
            case "flow-client-module" -> "flow-client-engine";
            case "flow-client-loader-module" -> "Flow.loadOnDemand";
            case "flow-client-index-module" -> "flow-client-entrypoint";
            case "flow-client-static-metadata" -> "flow-client-static-resource-graph";
            case "flow-client-gwt-bundle" -> "flow-client-gwt-resource-graph";
            default -> "flow-client-classpath-resource-graph";
        };
    }
}
