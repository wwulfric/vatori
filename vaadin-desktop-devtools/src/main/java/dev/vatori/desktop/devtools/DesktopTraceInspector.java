package dev.vatori.desktop.devtools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DesktopTraceInspector {
    private static final Pattern TYPE = Pattern.compile("\"type\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern DIRECTION = Pattern.compile("\"direction\"\\s*:\\s*\"([^\"]+)\"");
    private static final List<String> FLOW_IPC_REQUIRED_TYPES = List.of(
            "launch",
            "ready",
            "window.create",
            "bootstrapRequest",
            "bootstrapResponse",
            "resourceRequest",
            "resourceResponse",
            "push",
            "event",
            "resync",
            "command",
            "capabilityResult",
            "patch",
            "webview.dom",
            "webview.snapshot",
            "shutdown");

    private DesktopTraceInspector() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0 || "--help".equals(args[0])) {
            usage();
            return;
        }

        boolean requireFlowIpc = false;
        List<Path> paths = new ArrayList<>();
        for (String arg : args) {
            if ("--require-flow-ipc-v1".equals(arg)) {
                requireFlowIpc = true;
            } else {
                paths.add(Path.of(arg));
            }
        }

        if (paths.isEmpty()) {
            usage();
            System.exit(2);
        }

        int failures = 0;
        for (Path path : paths) {
            TraceSummary summary = inspect(path);
            summary.print(path);
            if (requireFlowIpc && !summary.hasRequiredFlowIpcTypes()) {
                failures++;
            }
        }

        if (failures > 0) {
            System.exit(2);
        }
    }

    public static TraceSummary inspect(Path path) throws IOException {
        TraceSummary summary = new TraceSummary();
        for (String rawLine : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            String line = normalize(rawLine);
            if (line.isBlank()) {
                continue;
            }
            summary.total++;
            String type = first(TYPE, line, "unknown");
            String direction = first(DIRECTION, line, "unknown");
            summary.types.merge(type, 1, Integer::sum);
            summary.directions.merge(direction, 1, Integer::sum);
            if (line.contains("\"transport\":\"flow-ipc\"")) {
                summary.flowIpcTransport = true;
            }
            if (line.contains("\"compatibilityMode\":false")) {
                summary.compatibilityFalse = true;
            }
            if (line.contains("Clicks: 1")) {
                summary.clickPatchObserved = true;
            }
            if (line.contains("native-nspasteboard")) {
                summary.nativeClipboardObserved = true;
            }
            if (line.contains("\"source\":\"UidlWriter\"")
                    && line.contains("\"containsChanges\":true")
                    && line.contains("\"containsSyncId\":true")
                    && line.contains("\"consumedByWebView\":true")
                    && line.contains("\"minimum-webview-uidl-applier\"")) {
                summary.uidlWriterObserved = true;
            }
            if (line.contains("\"uidlRpcDispatch\":true")) {
                summary.uidlRpcDispatchObserved = true;
            }
            if (line.contains("\"type\":\"event\"")
                    && line.contains("\"dispatch\":\"flow-client-xhr\"")
                    && line.contains("\"attachExistingElementById\"")
                    && line.contains("\"uidlRpc\"")
                    && line.contains("\"clientId\":0")) {
                summary.flowClientXhrAttachObserved = true;
            }
            if (line.contains("\"type\":\"event\"")
                    && line.contains("\"dispatch\":\"flow-client-xhr\"")
                    && line.contains("\"uidlRpc\"")
                    && line.contains("\"csrfToken\"")
                    && line.contains("\"securityKey\"")
                    && line.contains("\"event\":\"click\"")
                    && line.contains("\"clientId\":1")) {
                summary.webViewUidlRpcObserved = true;
                summary.flowClientXhrClickObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"securityKey\"")
                    && line.contains("Vaadin-Security-Key")
                    && line.contains("\"containsSecurityKey\":true")) {
                summary.securityKeyObserved = true;
            }
            if (line.contains("\"type\":\"resync\"")
                    && line.contains("\"securityKey\"")
                    && line.contains("\"syncId\"")
                    && line.contains("\"clientId\"")) {
                summary.resyncSecurityKeyObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapRequest\"")
                    && line.contains("\"sessionId\":\"flow-ipc-session\"")
                    && line.contains("\"uiId\":\"main\"")) {
                summary.bootstrapScopeObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"flowClientResources\"")
                    && line.contains("\"flow-client-classpath\"")
                    && line.contains("META-INF/frontend/FlowBootstrap.js")) {
                summary.flowClientResourceManifestObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"flowClientResources\"")
                    && line.contains("\"flow-client-classpath\"")
                    && line.contains("META-INF/frontend/FlowClient.js")) {
                summary.flowClientEngineResourceManifestObserved = true;
            }
            if (line.contains("\"type\":\"resourceRequest\"")
                    && line.contains("META-INF/frontend/FlowBootstrap.js")
                    && line.contains("\"securityKey\"")) {
                summary.flowClientResourceRequestObserved = true;
            }
            if (line.contains("\"type\":\"resourceRequest\"")
                    && line.contains("META-INF/frontend/FlowClient.js")
                    && line.contains("\"securityKey\"")) {
                summary.flowClientEngineResourceRequestObserved = true;
            }
            if (line.contains("\"type\":\"resourceResponse\"")
                    && line.contains("\"flowClientResource\":true")
                    && line.contains("META-INF/frontend/FlowBootstrap.js")
                    && line.contains("\"bodyRedacted\":true")) {
                summary.flowClientResourceResponseObserved = true;
            }
            if (line.contains("\"type\":\"resourceResponse\"")
                    && line.contains("\"flowClientResource\":true")
                    && line.contains("META-INF/frontend/FlowClient.js")
                    && line.contains("\"bodyRedacted\":true")) {
                summary.flowClientEngineResourceResponseObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && line.contains("flowClientResourceLoaded")
                    && line.contains("flowClientResourceVerified")
                    && line.contains("flowClientResourceModuleLoaded")
                    && line.contains("flowClientBootstrapExported")
                    && line.contains("flowClientBootstrapInitialized")
                    && line.contains("ROOT-2521314")
                    && line.contains("flowClientResourceExecuted")
                    && line.contains("META-INF/frontend/FlowBootstrap.js")) {
                summary.flowClientResourceDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && line.contains("flowClientEngineResourceLoaded")
                    && line.contains("flowClientEngineResourceVerified")
                    && line.contains("flowClientEngineModuleLoaded")
                    && line.contains("flowClientEngineInitExported")
                    && line.contains("flowClientEngineInitialized")
                    && line.contains("flowClientEngineClientRegistered")
                    && line.contains("ROOT")
                    && line.contains("META-INF/frontend/FlowClient.js")) {
                summary.flowClientEngineDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && line.contains("flowClientEngineBootstrapDomBound")
                    && line.contains("flowClientEngineBoundNodeIds")
                    && line.contains("19,16,14,12")
                    && line.contains("flowClientEngineBootstrapStatusText")
                    && line.contains("flowClientEngineBootstrapButtonText")
                    && line.contains("Increment over IPC")) {
                summary.flowClientEngineBootstrapDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "flowClientEngineXhrBridgeInstalled", "true")
                    && containsLiteralField(line, "flowClientEngineXhrRequestObserved", "true")
                    && containsLiteralField(line, "flowClientEngineXhrResponseObserved", "true")
                    && containsLiteralField(line, "flowClientEngineXhrLastClientId", "1")
                    && containsLiteralField(line, "flowClientEngineEventPatchConsumed", "true")
                    && containsStringField(line, "flowClientEngineEventStatusText", "Clicks: 1")
                    && containsStringField(line, "flowClientEngineXhrError", "")) {
                summary.flowClientEngineXhrDomObserved = true;
            }
            if (line.contains("\"type\":\"event\"")
                    && line.contains("\"sessionId\":\"flow-ipc-session\"")
                    && line.contains("\"uiId\":\"main\"")) {
                summary.eventScopeObserved = true;
            }
            if (line.contains("\"type\":\"resync\"")
                    && line.contains("\"sessionId\":\"flow-ipc-session\"")
                    && line.contains("\"uiId\":\"main\"")) {
                summary.resyncScopeObserved = true;
            }
            if (line.contains("\"type\":\"patch\"")
                    && line.contains("\"source\":\"UidlRequestHandler\"")
                    && line.contains("\"eventDispatch\":\"flow-client-xhr\"")
                    && line.contains("Clicks: 1")
                    && line.contains("\"consumedByWebView\":true")
                    && line.contains("\"minimum-webview-uidl-applier\"")) {
                summary.flowClientXhrPatchObserved = true;
            }
            if (line.contains("\"type\":\"patch\"")
                    && line.contains("\"source\":\"UidlRequestHandler\"")
                    && line.contains("\"consumedByWebView\":true")
                    && line.contains("\"minimum-webview-uidl-applier\"")) {
                summary.uidlPatchObserved = true;
            }
            if (line.contains("\"type\":\"push\"")
                    && line.contains("\"source\":\"UidlWriter\"")
                    && line.contains("\"reason\":\"push\"")
                    && line.contains("\"consumedByWebView\":true")
                    && line.contains("\"minimum-webview-uidl-applier\"")) {
                summary.pushUidlObserved = true;
            }
            if (line.contains("\"type\":\"patch\"")
                    && line.contains("\"source\":\"UidlWriter\"")
                    && line.contains("\"reason\":\"resync\"")
                    && line.contains("\"resync\":true")
                    && line.contains("resynchronize")
                    && line.contains("\"consumedByWebView\":true")
                    && line.contains("\"minimum-webview-uidl-applier\"")) {
                summary.resyncUidlObserved = true;
            }
        }
        return summary;
    }

    private static String normalize(String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("HOST_PROTOCOL ") ? trimmed.substring("HOST_PROTOCOL ".length()) : trimmed;
    }

    private static String first(Pattern pattern, String line, String fallback) {
        Matcher matcher = pattern.matcher(line);
        return matcher.find() ? matcher.group(1) : fallback;
    }

    private static boolean containsLiteralField(String line, String field, String value) {
        return line.contains("\"" + field + "\":" + value)
                || line.contains("\\\"" + field + "\\\":" + value);
    }

    private static boolean containsStringField(String line, String field, String value) {
        return line.contains("\"" + field + "\":\"" + value + "\"")
                || line.contains("\\\"" + field + "\\\":\\\"" + value + "\\\"");
    }

    private static void usage() {
        System.out.println("Usage: bin/flow-ipc-trace-inspect [--require-flow-ipc-v1] <trace.ndjson>...");
    }

    public static final class TraceSummary {
        private int total;
        private final Map<String, Integer> types = new LinkedHashMap<>();
        private final Map<String, Integer> directions = new LinkedHashMap<>();
        private boolean flowIpcTransport;
        private boolean compatibilityFalse;
        private boolean clickPatchObserved;
        private boolean nativeClipboardObserved;
        private boolean uidlWriterObserved;
        private boolean securityKeyObserved;
        private boolean resyncSecurityKeyObserved;
        private boolean bootstrapScopeObserved;
        private boolean eventScopeObserved;
        private boolean resyncScopeObserved;
        private boolean flowClientResourceManifestObserved;
        private boolean flowClientResourceRequestObserved;
        private boolean flowClientResourceResponseObserved;
        private boolean flowClientResourceDomObserved;
        private boolean flowClientEngineResourceManifestObserved;
        private boolean flowClientEngineResourceRequestObserved;
        private boolean flowClientEngineResourceResponseObserved;
        private boolean flowClientEngineDomObserved;
        private boolean flowClientEngineBootstrapDomObserved;
        private boolean flowClientEngineXhrDomObserved;
        private boolean uidlRpcDispatchObserved;
        private boolean webViewUidlRpcObserved;
        private boolean flowClientXhrAttachObserved;
        private boolean flowClientXhrClickObserved;
        private boolean flowClientXhrPatchObserved;
        private boolean uidlPatchObserved;
        private boolean pushUidlObserved;
        private boolean resyncUidlObserved;

        public boolean hasRequiredFlowIpcTypes() {
            return types.keySet().containsAll(FLOW_IPC_REQUIRED_TYPES)
                    && flowIpcTransport
                    && compatibilityFalse
                    && clickPatchObserved
                    && nativeClipboardObserved
                    && uidlWriterObserved
                    && securityKeyObserved
                    && resyncSecurityKeyObserved
                    && bootstrapScopeObserved
                    && eventScopeObserved
                    && resyncScopeObserved
                    && flowClientResourceManifestObserved
                    && flowClientResourceRequestObserved
                    && flowClientResourceResponseObserved
                    && flowClientResourceDomObserved
                    && flowClientEngineResourceManifestObserved
                    && flowClientEngineResourceRequestObserved
                    && flowClientEngineResourceResponseObserved
                    && flowClientEngineDomObserved
                    && flowClientEngineBootstrapDomObserved
                    && flowClientEngineXhrDomObserved
                    && uidlRpcDispatchObserved
                    && webViewUidlRpcObserved
                    && flowClientXhrAttachObserved
                    && flowClientXhrClickObserved
                    && flowClientXhrPatchObserved
                    && uidlPatchObserved
                    && pushUidlObserved
                    && resyncUidlObserved;
        }

        private void print(Path path) {
            System.out.println("Trace: " + path);
            System.out.println("Messages: " + total);
            System.out.println("Directions: " + directions);
            System.out.println("Types: " + types);
            System.out.println("transport=flow-ipc: " + flowIpcTransport);
            System.out.println("compatibilityMode=false: " + compatibilityFalse);
            System.out.println("click patch observed: " + clickPatchObserved);
            System.out.println("native clipboard observed: " + nativeClipboardObserved);
            System.out.println("UIDL writer observed: " + uidlWriterObserved);
            System.out.println("Flow security key observed: " + securityKeyObserved);
            System.out.println("resync security key observed: " + resyncSecurityKeyObserved);
            System.out.println("bootstrap scope observed: " + bootstrapScopeObserved);
            System.out.println("event scope observed: " + eventScopeObserved);
            System.out.println("resync scope observed: " + resyncScopeObserved);
            System.out.println("Flow client resource manifest observed: " + flowClientResourceManifestObserved);
            System.out.println("Flow client resource request observed: " + flowClientResourceRequestObserved);
            System.out.println("Flow client resource response observed: " + flowClientResourceResponseObserved);
            System.out.println("Flow client resource DOM observed: " + flowClientResourceDomObserved);
            System.out.println("Flow client engine resource manifest observed: " + flowClientEngineResourceManifestObserved);
            System.out.println("Flow client engine resource request observed: " + flowClientEngineResourceRequestObserved);
            System.out.println("Flow client engine resource response observed: " + flowClientEngineResourceResponseObserved);
            System.out.println("Flow client engine DOM observed: " + flowClientEngineDomObserved);
            System.out.println("Flow client engine bootstrap DOM observed: " + flowClientEngineBootstrapDomObserved);
            System.out.println("Flow client engine XHR DOM observed: " + flowClientEngineXhrDomObserved);
            System.out.println("UIDL RPC dispatch observed: " + uidlRpcDispatchObserved);
            System.out.println("WebView UIDL RPC observed: " + webViewUidlRpcObserved);
            System.out.println("Flow client XHR attach observed: " + flowClientXhrAttachObserved);
            System.out.println("Flow client XHR click observed: " + flowClientXhrClickObserved);
            System.out.println("Flow client XHR patch observed: " + flowClientXhrPatchObserved);
            System.out.println("UIDL request handler patch observed: " + uidlPatchObserved);
            System.out.println("server push UIDL observed: " + pushUidlObserved);
            System.out.println("resync UIDL observed: " + resyncUidlObserved);
            List<String> missing = FLOW_IPC_REQUIRED_TYPES.stream()
                    .filter(type -> !types.containsKey(type))
                    .toList();
            if (!missing.isEmpty()) {
                System.out.println("Missing required types: " + missing);
            }
        }
    }
}
