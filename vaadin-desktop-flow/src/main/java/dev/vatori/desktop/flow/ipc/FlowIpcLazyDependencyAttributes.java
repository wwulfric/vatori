package dev.vatori.desktop.flow.ipc;

import java.util.LinkedHashMap;
import java.util.Map;

final class FlowIpcLazyDependencyAttributes {
    static final String RESOURCE_PATH = "vatori-flow-ipc-lazy-v1_10.js";
    static final String BODY = """
            window.VaadinDesktopIpcLazyDependencyProbe = {
              executed: true,
              source: 'flow-uidl-dependencyloader-loadmode-lazy-v1_10',
              mode: 'LoadMode.LAZY',
              path: 'vatori-flow-ipc-lazy-v1_10.js'
            };
            window.dispatchEvent(new CustomEvent('vatori-flow-ipc-lazy-dependency-v1_10', {
              detail: window.VaadinDesktopIpcLazyDependencyProbe
            }));
            """;

    private static final String VERSION = "flow-ipc-lazy-dependency-v1_10";
    private static final String SOURCE =
            "flow-uidl-dependencyloader-loadmode-lazy-v1_10";
    private static final String EXPRESSION =
            "context://vatori-flow-ipc-lazy-v1_10.js";
    private static final String CHUNK_ID = "vatori-flow-ipc-lazy-v1_10";
    private static final String TRIGGER_OWNER =
            "visible-flow-client-dependencyloader";
    private static final String RESOLVER =
            "flow-ipc-dependencyloader-resource-resolver-v1_10";

    private FlowIpcLazyDependencyAttributes() {
    }

    static Map<String, Object> manifestAttributes() {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("flowLazyDependencyVersion", VERSION);
        manifest.put("flowLazyDependencySource", SOURCE);
        manifest.put("flowLazyDependencyMode", "LoadMode.LAZY");
        manifest.put("flowLazyDependencyExpression", EXPRESSION);
        manifest.put("flowLazyDependencyChunkId", CHUNK_ID);
        manifest.put("flowLazyDependencyResolvedPath", RESOURCE_PATH);
        manifest.put("flowLazyDependencyTriggerOwner", TRIGGER_OWNER);
        manifest.put("flowLazyDependencyTriggeredByFlowClient", true);
        manifest.put("flowLazyDependencyResolver", RESOLVER);
        return manifest;
    }

    static Map<String, Object> resourceAttributes(String runtimeResolverContractVersion) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("componentFrontendResource", true);
        attributes.put("flowLazyDependencyResource", true);
        attributes.put("runtimeResolverContractVersion", runtimeResolverContractVersion);
        attributes.put("logicalRequestId", "flow-lazy-dependency:v1_10");
        attributes.put("artifactFamily", "flow-lazy-dependency");
        attributes.put("origin", "classpath");
        attributes.put("resourceOrigin", "flow-uidl-loadmode-lazy-dependency");
        attributes.put("moduleSpecifier", EXPRESSION);
        attributes.put("resolver", RESOLVER);
        attributes.put("dependencyOf", "UidlWriter:LoadMode.LAZY");
        attributes.put("executionPolicy", "execute-after-flow-dependencyloader-trigger-v1_10");
        attributes.put("artifactStatus", "available");
        attributes.put("requestedBy", "FlowClient.DependencyLoader");
        attributes.put("failureClassification", "flow-lazy-dependency-runtime-v1_10");
        attributes.put("blockedReason",
                "real Flow LoadMode.LAZY dependency is resolved through IPC instead of a localhost URL");
        attributes.put("lookupKind", "Flow-lazy-dependency-resource-v1_10");
        attributes.put("pathMapping", "context-url-to-ipc-resource-path");
        attributes.put("dependencyGraph", "UidlWriter:LAZY=>DependencyLoader=>script.src=>IPC resourceRequest");
        attributes.put("stageUrlPolicy", "host-owned-resource-scheme-file-backed-v1_17");
        attributes.put("cachePolicy", "single-trigger-per-resource-path");
        attributes.put("cleanupOwnership", "cleanup-owned-window-lifetime-file-backed-v1_17");
        attributes.put("blockerClassification", "none");
        attributes.put("frontendResourceLoaderExecutionSlice", "flow-lazy-dependency-v1_17");
        attributes.put("frontendResourceLoaderExpectedStagingMode", "custom-scheme");
        attributes.put("frontendResourceLoaderRelativeImportBase", RESOURCE_PATH);
        attributes.put("frontendResourceLoaderMimeType", "text/javascript; charset=utf-8");
        attributes.put("lazyDependencyVersion", VERSION);
        attributes.put("lazyDependencySource", SOURCE);
        attributes.put("lazyDependencyMode", "LoadMode.LAZY");
        attributes.put("lazyDependencyExpression", EXPRESSION);
        attributes.put("lazyDependencyChunkId", CHUNK_ID);
        attributes.put("lazyDependencyResolvedPath", RESOURCE_PATH);
        attributes.put("lazyDependencyTriggerOwner", TRIGGER_OWNER);
        attributes.put("lazyDependencyTriggeredByFlowClient", true);
        attributes.put("lazyDependencyResolver", RESOLVER);
        attributes.put("lazyDependencyExecutionExpectedGlobal",
                "window.VaadinDesktopIpcLazyDependencyProbe.executed");
        attributes.put("shim", false);
        return attributes;
    }
}
