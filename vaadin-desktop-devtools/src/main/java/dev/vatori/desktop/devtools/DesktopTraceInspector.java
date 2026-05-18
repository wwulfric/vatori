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
            "error",
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
        boolean requireFlowIpcV1_1 = false;
        List<Path> paths = new ArrayList<>();
        for (String arg : args) {
            if ("--require-flow-ipc-v1".equals(arg)) {
                requireFlowIpc = true;
            } else if ("--require-flow-ipc-v1_1".equals(arg)) {
                requireFlowIpc = true;
                requireFlowIpcV1_1 = true;
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
            if (requireFlowIpcV1_1 && !summary.hasRequiredFlowIpcV1_1Types()) {
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
            if (line.contains("\"type\":\"capabilityManifest\"")
                    && line.contains("\"loaded\":true")
                    && containsSupportedPermissionMode(line)
                    && line.contains("window.setTitle")
                    && line.contains("clipboard.writeText")
                    && line.contains("fileDialog.openFile")
                    && line.contains("notification.show")) {
                summary.capabilityManifestObserved = true;
            }
            if (line.contains("\"type\":\"capabilityManifest\"")
                    && line.contains("\"loaded\":true")
                    && line.contains("\"permissionMode\":\"manifest-enforced\"")
                    && line.contains("window.setTitle")
                    && line.contains("clipboard.writeText")
                    && line.contains("fileDialog.openFile")
                    && line.contains("notification.show")) {
                summary.manifestEnforcedObserved = true;
            }
            if (line.contains("\"type\":\"capabilityManifest\"")
                    && line.contains("\"loaded\":true")
                    && line.contains("\"permissionMode\":\"manifest-enforced\"")
                    && line.contains("fileTransfer.download")
                    && line.contains("fileTransfer.selectUploadFile")) {
                summary.fileTransferManifestObserved = true;
            }
            if (line.contains("\"serverSequenceScope\":\"java-sidecar-to-webview\"")
                    && line.contains("\"serverSequence\"")) {
                summary.serverSequenceObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "flowIpcServerSequenceObserved", "true")
                    && containsLiteralField(line, "flowIpcServerSequenceMonotonic", "true")
                    && line.contains("bootstrapResponse:")
                    && line.contains("resourceResponse:")
                    && line.contains("push:")
                    && line.contains("patch:")) {
                summary.flowIpcServerSequenceDomObserved = true;
            }
            if (line.contains("Clicks: 1")) {
                summary.clickPatchObserved = true;
            }
            if (line.contains("native-nspasteboard")) {
                summary.nativeClipboardObserved = true;
            }
            if (line.contains("\"type\":\"capabilityResult\"")
                    && containsSupportedPermissionMode(line)
                    && line.contains("\"permissionDecision\":\"allowed\"")
                    && line.contains("\"permissionScope\":\"window:main\"")) {
                summary.capabilityPermissionObserved = true;
            }
            if (line.contains("\"type\":\"capabilityResult\"")
                    && line.contains("\"permissionDecision\":\"denied\"")
                    && line.contains("\"permissionReason\":\"manifest-denied\"")
                    && line.contains("\"success\":false")
                    && line.contains("diagnostics.deniedCapability")) {
                summary.capabilityDeniedObserved = true;
            }
            if (line.contains("\"type\":\"capabilityResult\"")
                    && line.contains("\"permissionManifestLoaded\":true")
                    && line.contains("\"permissionReason\":\"manifest-allow\"")
                    && line.contains("desktop-capabilities.json")
                    && line.contains("window.setTitle")
                    && line.contains("clipboard.writeText")
                    && line.contains("fileDialog.openFile")
                    && line.contains("notification.show")) {
                summary.capabilityManifestResultObserved = true;
            }
            if (line.contains("\"type\":\"capabilityResult\"")
                    && line.contains("\"command\":\"fileDialog.openFile\"")
                    && fileDialogResultObserved(line)
                    && line.contains("\"permissionManifestLoaded\":true")
                    && line.contains("\"permissionDecision\":\"allowed\"")
                    && line.contains("\"permissionReason\":\"manifest-allow\"")) {
                summary.fileDialogResultObserved = true;
            }
            if (line.contains("\"type\":\"capabilityResult\"")
                    && line.contains("\"command\":\"fileDialog.openFile\"")
                    && line.contains("\"mode\":\"native-appkit-open-panel\"")
                    && line.contains("\"paths\":")
                    && line.contains("\"securityScoped\":false")
                    && line.contains("\"permissionDecision\":\"allowed\"")) {
                summary.nativeFileDialogResultObserved = true;
            }
            if (line.contains("\"type\":\"command\"")
                    && line.contains("\"command\":\"fileTransfer.download\"")
                    && line.contains("\"bodyRedacted\":true")
                    && containsPositiveIntegerField(line, "bodyBytes")) {
                summary.fileTransferDownloadCommandRedactedObserved = true;
            }
            if (line.contains("\"type\":\"capabilityResult\"")
                    && line.contains("\"command\":\"fileTransfer.download\"")
                    && line.contains("\"operation\":\"download\"")
                    && line.contains("\"success\":true")
                    && line.contains("\"bodyRedacted\":true")
                    && line.contains("\"permissionDecision\":\"allowed\"")
                    && containsPositiveIntegerField(line, "byteCount")
                    && line.contains("flow-ipc-download-smoke.txt")) {
                summary.fileTransferDownloadResultObserved = true;
            }
            if (line.contains("\"type\":\"capabilityResult\"")
                    && line.contains("\"command\":\"fileTransfer.selectUploadFile\"")
                    && line.contains("\"operation\":\"upload\"")
                    && line.contains("\"success\":true")
                    && line.contains("\"bodyRedacted\":true")
                    && line.contains("\"securityScoped\":false")
                    && line.contains("\"permissionDecision\":\"allowed\"")
                    && containsPositiveIntegerField(line, "byteCount")
                    && containsPositiveIntegerField(line, "sampledBytes")
                    && line.contains("flow-ipc-upload-smoke.txt")) {
                summary.fileTransferUploadResultObserved = true;
            }
            if (line.contains("\"type\":\"error\"")
                    && line.contains("webviewVerificationProbe")
                    && line.contains("verification-error-probe")) {
                summary.webViewErrorObserved = true;
            }
            if (line.contains("\"source\":\"UidlWriter\"")
                    && line.contains("\"reason\":\"bootstrap\"")
                    && line.contains("\"containsChanges\":true")
                    && line.contains("\"containsSyncId\":true")
                    && line.contains("\"consumedByWebView\":true")
                    && line.contains("\"flow-client-engine-visible\"")) {
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
            if (line.contains("\"type\":\"event\"")
                    && line.contains("\"dispatch\":\"flow-client-xhr\"")
                    && line.contains("\"componentId\":\"ipc-text-input\"")
                    && line.contains("\"event\":\"change\"")
                    && line.contains("element.value")
                    && line.contains("Flow IPC typed")) {
                summary.flowClientXhrTextInputObserved = true;
            }
            if (line.contains("\"type\":\"event\"")
                    && line.contains("\"dispatch\":\"flow-client-xhr\"")
                    && line.contains("\"componentId\":\"ipc-checkbox\"")
                    && line.contains("\"event\":\"change\"")
                    && line.contains("element.checked")) {
                summary.flowClientXhrCheckboxObserved = true;
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
                    && line.contains("META-INF/frontend/FlowClient.js")
                    && line.contains("\"ipcAugmented\":true")) {
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
            if (line.contains("\"type\":\"resourceRequest\"")
                    && line.contains("\"purpose\":\"flow-client-lazy-lookup\"")
                    && line.contains("\"lookupKind\":\"Flow.loadOnDemand-minimum-probe\"")
                    && line.contains("META-INF/frontend/Flow.js")
                    && line.contains("\"securityKey\"")) {
                summary.flowClientResourceLazyLookupRequestObserved = true;
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
            if (line.contains("\"type\":\"resourceResponse\"")
                    && line.contains("\"flowClientResource\":true")
                    && line.contains("\"requestPurpose\":\"flow-client-lazy-lookup\"")
                    && line.contains("\"lookupKind\":\"Flow.loadOnDemand-minimum-probe\"")
                    && line.contains("META-INF/frontend/Flow.js")
                    && line.contains("\"bodyRedacted\":true")) {
                summary.flowClientResourceLazyLookupResponseObserved = true;
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
                    && containsLiteralField(line, "flowClientResourceManifestCount", "6")
                    && containsLiteralField(line, "flowClientResourceGraphRequestedCount", "6")
                    && containsLiteralField(line, "flowClientResourceGraphResponseCount", "6")
                    && containsLiteralField(line, "flowClientResourceGraphComplete", "true")
                    && containsLiteralField(line, "flowClientResourceGraphVerified", "true")
                    && line.contains("META-INF/frontend/FlowBootstrap.js")
                    && line.contains("META-INF/frontend/FlowClient.js")
                    && line.contains("META-INF/frontend/Flow.js")
                    && line.contains("META-INF/frontend/index.js")
                    && line.contains("META-INF/resources/VAADIN/static/client/compile.properties")
                    && line.contains(".cache.js")) {
                summary.flowClientResourceGraphDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "flowClientResourceExecutionClassificationComplete", "true")
                    && containsLiteralField(line, "flowClientResourceVerifiedCount", "6")
                    && containsLiteralField(line, "flowClientResourceExecutedCount", "2")
                    && containsLiteralField(line, "flowClientResourceVerifiedOnlyCount", "4")
                    && containsLiteralField(line, "flowClientResourceIntentionallyNotExecutedCount", "4")
                    && containsLiteralField(line, "flowClientResourceExecutionFailedCount", "0")
                    && line.contains("flowClientResourceExecutedPaths")
                    && line.contains("META-INF/frontend/FlowBootstrap.js")
                    && line.contains("META-INF/frontend/FlowClient.js")
                    && line.contains("flowClientResourceVerifiedOnlyPaths")
                    && line.contains("META-INF/frontend/Flow.js")
                    && line.contains("META-INF/frontend/index.js")
                    && line.contains("META-INF/resources/VAADIN/static/client/compile.properties")
                    && line.contains(".cache.js")) {
                summary.flowClientResourceExecutionClassificationDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "flowClientResourceLazyLookupRequested", "true")
                    && containsLiteralField(line, "flowClientResourceLazyLookupResponse", "true")
                    && containsLiteralField(line, "flowClientResourceLazyLookupVerified", "true")
                    && containsStringField(line, "flowClientResourceLazyLookupPurpose", "flow-client-lazy-lookup")
                    && containsStringField(line, "flowClientResourceLazyLookupKind", "Flow.loadOnDemand-minimum-probe")
                    && containsStringField(line, "flowClientResourceLazyLookupPath", "META-INF/frontend/Flow.js")
                    && containsStringField(line, "flowClientResourceLazyLookupResult",
                            "verified-only:intentionally-not-executed")
                    && line.contains("flowClientResourceLoadOnDemandProbeResult")
                    && containsStringField(line, "flowClientResourceBootstrapLazyEntriesResult",
                            "no-bootstrap-LAZY-entry-in-smoke")
                    && containsLiteralField(line, "flowClientResourceStaticLookupVerified", "true")
                    && containsStringField(line, "flowClientResourceStaticLookupPath",
                            "META-INF/resources/VAADIN/static/client/compile.properties")
                    && containsStringField(line, "flowClientResourceThemeLookupResult",
                            "no-theme-resource-declared-by-smoke-app")) {
                summary.flowClientResourceLazyLookupDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && line.contains("flowClientEngineResourceLoaded")
                    && line.contains("flowClientEngineResourceVerified")
                    && line.contains("flowClientEngineModuleLoaded")
                    && line.contains("flowClientEngineInitExported")
                    && line.contains("flowClientEngineInitialized")
                    && line.contains("flowClientEngineClientRegistered")
                    && line.contains("flowClientEngineIpcMessageBridgeInstalled")
                    && line.contains("flowClientEngineIpcResyncBridgeInstalled")
                    && line.contains("ROOT")
                    && line.contains("META-INF/frontend/FlowClient.js")) {
                summary.flowClientEngineDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "flowClientEngineBootstrapDomBound", "true")
                    && line.contains("flowClientEngineBoundNodeIds")
                    && line.contains("flowClientEngineBootstrapStatusText")
                    && line.contains("flowClientEngineBootstrapButtonText")
                    && line.contains("Increment over IPC")) {
                summary.flowClientEngineBootstrapDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "flowClientEngineListBound", "true")
                    && containsLiteralField(line, "flowClientEngineListItemCount", "4")
                    && line.contains("1 Bootstrap over IPC READY")
                    && line.contains("4 Desktop command/capabilityResult over IPC READY")) {
                summary.flowClientEngineListDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "flowClientEngineXhrBridgeInstalled", "true")
                    && containsLiteralField(line, "flowClientEngineXhrRequestObserved", "true")
                    && containsLiteralField(line, "flowClientEngineXhrResponseObserved", "true")
                    && containsLiteralField(line, "flowClientEngineEventPatchConsumed", "true")
                    && containsClickedStatusField(line, "flowClientEngineEventStatusText")
                    && containsStringField(line, "flowClientEngineXhrError", "")) {
                summary.flowClientEngineXhrDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "flowClientEngineTextInputEventObserved", "true")
                    && containsStringField(line, "flowClientEngineTextInputStatusText", "Text input: Flow IPC typed")
                    && containsLiteralField(line, "flowClientEngineCheckboxEventObserved", "true")
                    && containsStringField(line, "flowClientEngineCheckboxStatusText", "Checkbox: checked")
                    && containsPositiveIntegerField(line, "flowClientEngineComponentEventPatchCount")
                    && line.contains("ipc-text-input")
                    && line.contains("ipc-checkbox")) {
                summary.flowClientEngineComponentEventsDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "fileTransferDownloadResultObserved", "true")
                    && containsLiteralField(line, "fileTransferUploadResultObserved", "true")
                    && containsLiteralField(line, "fileTransferDownloadBodyRedacted", "true")
                    && containsLiteralField(line, "fileTransferUploadBodyRedacted", "true")
                    && containsPositiveIntegerField(line, "fileTransferDownloadByteCount")
                    && containsPositiveIntegerField(line, "fileTransferUploadByteCount")
                    && containsPositiveIntegerField(line, "fileTransferUploadSampledBytes")
                    && containsStringField(line, "fileTransferResultOrder", "download,upload")
                    && line.contains("flow-ipc-download-smoke.txt")
                    && line.contains("flow-ipc-upload-smoke.txt")) {
                summary.fileTransferDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "flowClientEngineIpcMessageBridgeInstalled", "true")
                    && containsLiteralField(line, "flowClientEnginePushPatchConsumed", "true")
                    && containsStringField(line, "flowClientEnginePushStatusText", "5 Server push over IPC READY")
                    && containsLiteralField(line, "flowClientEngineQueuedMessageCount", "0")) {
                summary.flowClientEnginePushDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsPositiveIntegerField(line, "flowClientEngineQueuedMessageHighWater")
                    && containsPositiveIntegerField(line, "flowClientEngineQueuedPushCount")
                    && containsPositiveIntegerField(line, "flowClientEngineDeliveredQueuedMessageCount")
                    && line.contains("flowClientEngineQueuedMessageOrder")
                    && line.contains("flowClientEngineDeliveredQueuedMessageOrder")
                    && line.contains("push:3")) {
                summary.flowClientEnginePushQueueDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsStringField(line, "flowClientEnginePushOrderStatusText", "6 Ordered push over IPC READY")
                    && containsLiteralField(line, "flowClientEngineDeliveredPushCount", "2")
                    && containsStringField(line, "flowClientEngineDeliveredMessageOrder", "push:3,push:4")
                    && containsLiteralField(line, "flowClientEngineIgnoredMessageCount", "1")
                    && containsStringField(line, "flowClientEngineIgnoredMessageOrder", "push:4:duplicate-late")
                    && containsLiteralField(line, "flowClientEngineDuplicateLateIgnored", "true")
                    && containsLiteralField(line, "flowClientEngineOrderingLastDeliveredServerSequence", "4")
                    && containsLiteralField(line, "flowClientEngineRecoveryResyncRequested", "true")
                    && containsStringField(line, "flowClientEngineRecoveryResyncReason",
                            "ordering-recovery-duplicate-late")) {
                summary.flowClientEnginePushOrderingDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "flowClientEngineIpcResyncBridgeInstalled", "true")
                    && containsLiteralField(line, "flowClientEngineResyncRequested", "true")
                    && containsLiteralField(line, "flowClientEngineResyncPatchConsumed", "true")) {
                summary.flowClientEngineResyncDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "flowClientEngineVisibleOwner", "true")
                    && containsStringField(line, "flowClientEngineVisibleRootId", "flow-client-engine-root")
                    && containsClickedStatusField(line, "flowClientEngineVisibleStatusText")
                    && containsStringField(line, "flowClientEngineVisibleButtonText", "Increment over IPC")
                    && containsLiteralField(line, "flowClientEngineLocalFallbackHidden", "true")) {
                summary.flowClientEngineVisibleDomObserved = true;
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
                    && line.contains("\"flow-client-engine-visible\"")) {
                summary.flowClientXhrPatchObserved = true;
            }
            if (line.contains("\"type\":\"patch\"")
                    && line.contains("\"source\":\"UidlRequestHandler\"")
                    && line.contains("\"eventDispatch\":\"flow-client-xhr\"")
                    && line.contains("Text input: Flow IPC typed")
                    && line.contains("\"consumedByWebView\":true")
                    && line.contains("\"flow-client-engine-visible\"")) {
                summary.textInputPatchObserved = true;
            }
            if (line.contains("\"type\":\"patch\"")
                    && line.contains("\"source\":\"UidlRequestHandler\"")
                    && line.contains("\"eventDispatch\":\"flow-client-xhr\"")
                    && line.contains("Checkbox: checked")
                    && line.contains("\"consumedByWebView\":true")
                    && line.contains("\"flow-client-engine-visible\"")) {
                summary.checkboxPatchObserved = true;
            }
            if (line.contains("\"type\":\"patch\"")
                    && line.contains("\"source\":\"UidlRequestHandler\"")
                    && line.contains("\"consumedByWebView\":true")
                    && line.contains("\"flow-client-engine-visible\"")) {
                summary.uidlPatchObserved = true;
            }
            if (line.contains("\"type\":\"push\"")
                    && line.contains("\"source\":\"UidlWriter\"")
                    && line.contains("\"reason\":\"push\"")
                    && line.contains("\"async\":true")
                    && line.contains("\"consumedByWebView\":true")
                    && line.contains("\"flow-client-engine-visible\"")) {
                summary.pushUidlObserved = true;
            }
            if (line.contains("\"type\":\"push\"")
                    && line.contains("\"pushIndex\":2")
                    && line.contains("6 Ordered push over IPC READY")
                    && line.contains("\"source\":\"UidlWriter\"")
                    && line.contains("\"reason\":\"push\"")
                    && line.contains("\"async\":true")
                    && line.contains("\"flow-client-engine-visible\"")) {
                summary.multiPushUidlObserved = true;
            }
            if (line.contains("\"type\":\"push\"")
                    && line.contains("\"orderingProbe\"")
                    && line.contains("\"kind\":\"duplicate-late\"")
                    && line.contains("\"effectiveServerSequence\":4")
                    && line.contains("\"expectedAction\":\"ignore-and-request-resync\"")) {
                summary.duplicateLatePushProbeObserved = true;
            }
            if (line.contains("\"type\":\"resync\"")
                    && line.contains("\"reason\":\"ordering-recovery-duplicate-late\"")
                    && line.contains("\"securityKey\"")
                    && line.contains("\"syncId\"")
                    && line.contains("\"clientId\"")) {
                summary.recoveryResyncRequestObserved = true;
            }
            if (line.contains("\"type\":\"patch\"")
                    && line.contains("\"source\":\"UidlRequestHandler\"")
                    && line.contains("\"reason\":\"resync\"")
                    && line.contains("\"eventDispatch\":\"flow-client-xhr\"")
                    && line.contains("\"resync\":true")
                    && line.contains("resynchronize")
                    && line.contains("\"consumedByWebView\":true")
                    && line.contains("\"flow-client-engine-visible\"")) {
                summary.resyncUidlObserved = true;
            }
        }
        return summary;
    }

    private static String normalize(String line) {
        String trimmed = line.trim();
        String payload = trimmed.startsWith("HOST_PROTOCOL ") ? trimmed.substring("HOST_PROTOCOL ".length()) : trimmed;
        return payload.replace("\\/", "/");
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

    private static boolean containsPositiveIntegerField(String line, String field) {
        for (int digit = 1; digit <= 9; digit++) {
            if (line.contains("\"" + field + "\":" + digit)
                    || line.contains("\\\"" + field + "\\\":" + digit)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsClickedStatusField(String line, String field) {
        for (int digit = 1; digit <= 9; digit++) {
            String prefix = "Clicks: " + digit;
            if (line.contains("\"" + field + "\":\"" + prefix)
                    || line.contains("\\\"" + field + "\\\":\\\"" + prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean fileDialogResultObserved(String line) {
        boolean v1Scaffold = line.contains("\"mode\":\"scaffold-cancelled\"")
                && line.contains("\"canceled\":true");
        boolean appKitPanel = line.contains("\"mode\":\"native-appkit-open-panel\"")
                && line.contains("\"canceled\":")
                && line.contains("\"paths\":")
                && line.contains("\"securityScoped\":false");
        return v1Scaffold || appKitPanel;
    }

    private static boolean containsSupportedPermissionMode(String line) {
        return line.contains("\"permissionMode\":\"allow-all-dev\"")
                || line.contains("\"permissionMode\":\"manifest-enforced\"")
                || line.contains("\"permissionMode\":\"trace-only-dry-run\"");
    }

    private static void usage() {
        System.out.println("Usage: bin/flow-ipc-trace-inspect [--require-flow-ipc-v1|--require-flow-ipc-v1_1] <trace.ndjson>...");
    }

    public static final class TraceSummary {
        private int total;
        private final Map<String, Integer> types = new LinkedHashMap<>();
        private final Map<String, Integer> directions = new LinkedHashMap<>();
        private boolean flowIpcTransport;
        private boolean compatibilityFalse;
        private boolean capabilityManifestObserved;
        private boolean capabilityManifestResultObserved;
        private boolean fileDialogResultObserved;
        private boolean nativeFileDialogResultObserved;
        private boolean manifestEnforcedObserved;
        private boolean capabilityDeniedObserved;
        private boolean fileTransferManifestObserved;
        private boolean fileTransferDownloadCommandRedactedObserved;
        private boolean fileTransferDownloadResultObserved;
        private boolean fileTransferUploadResultObserved;
        private boolean fileTransferDomObserved;
        private boolean serverSequenceObserved;
        private boolean flowIpcServerSequenceDomObserved;
        private boolean clickPatchObserved;
        private boolean nativeClipboardObserved;
        private boolean capabilityPermissionObserved;
        private boolean webViewErrorObserved;
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
        private boolean flowClientResourceGraphDomObserved;
        private boolean flowClientResourceExecutionClassificationDomObserved;
        private boolean flowClientResourceLazyLookupRequestObserved;
        private boolean flowClientResourceLazyLookupResponseObserved;
        private boolean flowClientResourceLazyLookupDomObserved;
        private boolean flowClientEngineResourceManifestObserved;
        private boolean flowClientEngineResourceRequestObserved;
        private boolean flowClientEngineResourceResponseObserved;
        private boolean flowClientEngineDomObserved;
        private boolean flowClientEngineBootstrapDomObserved;
        private boolean flowClientEngineListDomObserved;
        private boolean flowClientEngineXhrDomObserved;
        private boolean flowClientEngineVisibleDomObserved;
        private boolean flowClientEnginePushDomObserved;
        private boolean flowClientEnginePushQueueDomObserved;
        private boolean flowClientEnginePushOrderingDomObserved;
        private boolean flowClientEngineResyncDomObserved;
        private boolean uidlRpcDispatchObserved;
        private boolean webViewUidlRpcObserved;
        private boolean flowClientXhrAttachObserved;
        private boolean flowClientXhrClickObserved;
        private boolean flowClientXhrTextInputObserved;
        private boolean flowClientXhrCheckboxObserved;
        private boolean flowClientXhrPatchObserved;
        private boolean textInputPatchObserved;
        private boolean checkboxPatchObserved;
        private boolean flowClientEngineComponentEventsDomObserved;
        private boolean uidlPatchObserved;
        private boolean pushUidlObserved;
        private boolean multiPushUidlObserved;
        private boolean duplicateLatePushProbeObserved;
        private boolean recoveryResyncRequestObserved;
        private boolean resyncUidlObserved;

        public boolean hasRequiredFlowIpcTypes() {
            return types.keySet().containsAll(FLOW_IPC_REQUIRED_TYPES)
                    && flowIpcTransport
                    && compatibilityFalse
                    && capabilityManifestObserved
                    && capabilityManifestResultObserved
                    && fileDialogResultObserved
                    && serverSequenceObserved
                    && flowIpcServerSequenceDomObserved
                    && clickPatchObserved
                    && nativeClipboardObserved
                    && capabilityPermissionObserved
                    && webViewErrorObserved
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
                    && flowClientResourceGraphDomObserved
                    && flowClientEngineResourceManifestObserved
                    && flowClientEngineResourceRequestObserved
                    && flowClientEngineResourceResponseObserved
                    && flowClientEngineDomObserved
                    && flowClientEngineBootstrapDomObserved
                    && flowClientEngineListDomObserved
                    && flowClientEngineXhrDomObserved
                    && flowClientEngineVisibleDomObserved
                    && flowClientEnginePushDomObserved
                    && flowClientEnginePushQueueDomObserved
                    && flowClientEngineResyncDomObserved
                    && uidlRpcDispatchObserved
                    && webViewUidlRpcObserved
                    && flowClientXhrAttachObserved
                    && flowClientXhrClickObserved
                    && flowClientXhrPatchObserved
                    && uidlPatchObserved
                    && pushUidlObserved
                    && resyncUidlObserved;
        }

        public boolean hasRequiredFlowIpcV1_1Types() {
            return hasRequiredFlowIpcTypes()
                    && nativeFileDialogResultObserved
                    && manifestEnforcedObserved
                    && capabilityDeniedObserved
                    && fileTransferManifestObserved
                    && fileTransferDownloadCommandRedactedObserved
                    && fileTransferDownloadResultObserved
                    && fileTransferUploadResultObserved
                    && fileTransferDomObserved
                    && multiPushUidlObserved
                    && duplicateLatePushProbeObserved
                    && flowClientEnginePushOrderingDomObserved
                    && recoveryResyncRequestObserved
                    && flowClientResourceExecutionClassificationDomObserved
                    && flowClientResourceLazyLookupRequestObserved
                    && flowClientResourceLazyLookupResponseObserved
                    && flowClientResourceLazyLookupDomObserved
                    && flowClientXhrTextInputObserved
                    && flowClientXhrCheckboxObserved
                    && textInputPatchObserved
                    && checkboxPatchObserved
                    && flowClientEngineComponentEventsDomObserved;
        }

        private void print(Path path) {
            System.out.println("Trace: " + path);
            System.out.println("Messages: " + total);
            System.out.println("Directions: " + directions);
            System.out.println("Types: " + types);
            System.out.println("transport=flow-ipc: " + flowIpcTransport);
            System.out.println("compatibilityMode=false: " + compatibilityFalse);
            System.out.println("capability manifest observed: " + capabilityManifestObserved);
            System.out.println("capability manifest result observed: " + capabilityManifestResultObserved);
            System.out.println("file dialog result observed: " + fileDialogResultObserved);
            System.out.println("native file dialog result observed: " + nativeFileDialogResultObserved);
            System.out.println("manifest-enforced permission observed: " + manifestEnforcedObserved);
            System.out.println("capability denied observed: " + capabilityDeniedObserved);
            System.out.println("file transfer manifest observed: " + fileTransferManifestObserved);
            System.out.println("file transfer download command redacted observed: "
                    + fileTransferDownloadCommandRedactedObserved);
            System.out.println("file transfer download result observed: " + fileTransferDownloadResultObserved);
            System.out.println("file transfer upload result observed: " + fileTransferUploadResultObserved);
            System.out.println("file transfer DOM observed: " + fileTransferDomObserved);
            System.out.println("server sequence observed: " + serverSequenceObserved);
            System.out.println("Flow IPC server sequence DOM observed: " + flowIpcServerSequenceDomObserved);
            System.out.println("click patch observed: " + clickPatchObserved);
            System.out.println("native clipboard observed: " + nativeClipboardObserved);
            System.out.println("capability permission observed: " + capabilityPermissionObserved);
            System.out.println("WebView error observed: " + webViewErrorObserved);
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
            System.out.println("Flow client resource graph DOM observed: " + flowClientResourceGraphDomObserved);
            System.out.println("Flow client resource execution classification DOM observed: "
                    + flowClientResourceExecutionClassificationDomObserved);
            System.out.println("Flow client resource lazy lookup request observed: "
                    + flowClientResourceLazyLookupRequestObserved);
            System.out.println("Flow client resource lazy lookup response observed: "
                    + flowClientResourceLazyLookupResponseObserved);
            System.out.println("Flow client resource lazy lookup DOM observed: "
                    + flowClientResourceLazyLookupDomObserved);
            System.out.println("Flow client engine resource manifest observed: " + flowClientEngineResourceManifestObserved);
            System.out.println("Flow client engine resource request observed: " + flowClientEngineResourceRequestObserved);
            System.out.println("Flow client engine resource response observed: " + flowClientEngineResourceResponseObserved);
            System.out.println("Flow client engine DOM observed: " + flowClientEngineDomObserved);
            System.out.println("Flow client engine bootstrap DOM observed: " + flowClientEngineBootstrapDomObserved);
            System.out.println("Flow client engine list DOM observed: " + flowClientEngineListDomObserved);
            System.out.println("Flow client engine XHR DOM observed: " + flowClientEngineXhrDomObserved);
            System.out.println("Flow client engine visible DOM observed: " + flowClientEngineVisibleDomObserved);
            System.out.println("Flow client engine push DOM observed: " + flowClientEnginePushDomObserved);
            System.out.println("Flow client engine push queue DOM observed: " + flowClientEnginePushQueueDomObserved);
            System.out.println("Flow client engine push ordering DOM observed: " + flowClientEnginePushOrderingDomObserved);
            System.out.println("Flow client engine resync DOM observed: " + flowClientEngineResyncDomObserved);
            System.out.println("UIDL RPC dispatch observed: " + uidlRpcDispatchObserved);
            System.out.println("WebView UIDL RPC observed: " + webViewUidlRpcObserved);
            System.out.println("Flow client XHR attach observed: " + flowClientXhrAttachObserved);
            System.out.println("Flow client XHR click observed: " + flowClientXhrClickObserved);
            System.out.println("Flow client XHR text input observed: " + flowClientXhrTextInputObserved);
            System.out.println("Flow client XHR checkbox observed: " + flowClientXhrCheckboxObserved);
            System.out.println("Flow client XHR patch observed: " + flowClientXhrPatchObserved);
            System.out.println("Text input patch observed: " + textInputPatchObserved);
            System.out.println("Checkbox patch observed: " + checkboxPatchObserved);
            System.out.println("Flow client component events DOM observed: " + flowClientEngineComponentEventsDomObserved);
            System.out.println("UIDL request handler patch observed: " + uidlPatchObserved);
            System.out.println("server push UIDL observed: " + pushUidlObserved);
            System.out.println("multi-push UIDL observed: " + multiPushUidlObserved);
            System.out.println("duplicate/late push probe observed: " + duplicateLatePushProbeObserved);
            System.out.println("recovery resync request observed: " + recoveryResyncRequestObserved);
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
