package dev.vatori.desktop.flow.ipc;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ApplicationConstants;
import dev.vatori.desktop.flow.transport.DesktopFlowRuntimeSession;
import dev.vatori.desktop.flow.transport.FlowPatchResponse;
import dev.vatori.desktop.ipc.FlowIpcEnvelope;
import dev.vatori.desktop.ipc.IpcJson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class FlowIpcRuntime {
    static final String PREFIX = "VFIPC1 ";
    private static final String IPC_SESSION_ID = "flow-ipc-session";
    private static final String IPC_UI_ID = "main";
    private static final String DYNAMIC_RESOURCE_PREFIX = "VAADIN/dynamic/resource/";

    private final Class<? extends Component> routeClass;
    private final String route;
    private final FlowIpcRenderer renderer = new FlowIpcRenderer();
    private final FlowClientResources flowClientResources = new FlowClientResources();
    private final AtomicLong sequence = new AtomicLong();
    private final AtomicLong serverSequence = new AtomicLong();
    private final AtomicLong clientToServerSequence = new AtomicLong();
    private Component root;
    private DesktopFlowRuntimeSession flowSession;

    FlowIpcRuntime(Class<? extends Component> routeClass) {
        this.routeClass = Objects.requireNonNull(routeClass, "routeClass");
        Route routeAnnotation = routeClass.getAnnotation(Route.class);
        this.route = routeAnnotation == null ? "" : routeAnnotation.value();
    }

    void run() {
        Map<String, Object> readyPayload = new LinkedHashMap<>();
        readyPayload.put("transport", "flow-ipc");
        readyPayload.put("compatibilityMode", false);
        readyPayload.put("route", route);
        readyPayload.put("runtime", "repository-local-flow-ipc-adapter");
        readyPayload.put("uidlWriter", true);
        readyPayload.put("uidlRpcDispatch", true);
        readyPayload.put("flowClientResourceLoading", "ipc-resource-request");
        readyPayload.put("componentFrontendResourceLoading", "ipc-resource-request");
        readyPayload.put("uidlRpcProducer", "webview-bridge");
        readyPayload.put("uidlConsumer", "flow-client-engine-visible");
        readyPayload.put("uidlFallback", "minimum-webview-uidl-applier");
        send("ready", "flow-ipc-ready", "flow-ipc-ready", readyPayload);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith(PREFIX)) {
                    continue;
                }
                handle(line.substring(PREFIX.length()));
            }
        } catch (IOException error) {
            sendError("io", "Unable to read Flow IPC stdin: " + error.getMessage(), null, null);
        }
    }

    private void handle(String json) {
        String type = JsonFields.topLevelString(json, "type");
        String id = JsonFields.topLevelString(json, "id");
        String correlationId = JsonFields.topLevelString(json, "correlationId");
        try {
            switch (type == null ? "" : type) {
                case "bootstrapRequest" -> bootstrap(json, id, correlationId);
                case "resourceRequest" -> handleResourceRequest(json, id, correlationId);
                case "uploadRequest" -> handleUploadRequest(json, id, correlationId);
                case "event" -> handleEvent(json, id, correlationId);
                case "resync" -> handleResync(json, id, correlationId);
                case "error" -> handleClientError(json, id);
                case "shutdown" -> System.exit(0);
                default -> sendError("unknownMessage", "Unsupported Flow IPC message type: " + type, id, correlationId);
            }
        } catch (SecurityException error) {
            sendError("invalidCsrfToken", error.getMessage(), id, correlationId);
        } catch (IllegalArgumentException | IllegalStateException error) {
            sendError("invalidClientState", error.getMessage(), id, correlationId);
        } catch (RuntimeException error) {
            error.printStackTrace(System.err);
            sendError("runtime", error.getMessage() == null ? error.toString() : error.getMessage(), id, correlationId);
        }
    }

    private void bootstrap(String json, String id, String correlationId) {
        ensureBootstrapped();
        validateEnvelopeScope(json);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("transport", "flow-ipc");
        payload.put("compatibilityMode", false);
        payload.put("route", route);
        payload.put("uidl", uidlPayload("bootstrap", false));
        payload.put("flowClientResources", flowClientResources.manifest());
        payload.put("componentFrontendResources", flowClientResources.componentManifest());
        payload.put("clientState", Map.of(
                "sessionId", flowSession.sessionId(),
                "uiId", flowSession.uiId(),
                "csrfToken", flowSession.csrfToken(),
                "securityKey", flowSession.securityKey(),
                "securityTokenId", ApplicationConstants.UIDL_SECURITY_TOKEN_ID,
                "syncId", -1,
                "clientId", 0,
                "eventDispatch", "flow-client-xhr"));
        payload.put("tree", renderer.render(root));
        send("bootstrapResponse", nextId("bootstrap"), correlationIdOr(id, correlationId), payload);
        sendServerInitiatedPushes(id, correlationId);
    }

    private void handleResourceRequest(String json, String id, String correlationId) {
        ensureBootstrapped();
        validateEnvelopeScope(json);
        validateSecurityKey(json);
        String path = JsonFields.string(json, "path");
        if (path == null || path.isBlank()) {
            sendError("resourceNotFound", "Unknown Flow IPC resource: " + path, id, correlationId);
            return;
        }
        Map<String, Object> response;
        if (isDynamicStreamResourcePath(path)) {
            int rangeStart = JsonFields.integer(json, "streamResourceRangeStart", -1);
            int rangeEndInclusive = JsonFields.integer(json, "streamResourceRangeEndInclusive", -1);
            boolean chunkedDownload = JsonFields.bool(json, "streamResourceChunkedDownload", false);
            int chunkIndex = JsonFields.integer(json, "streamResourceChunkIndex", -1);
            int chunkCount = JsonFields.integer(json, "streamResourceChunkCount", -1);
            int chunkSize = JsonFields.integer(json, "streamResourceChunkSize", -1);
            boolean downloadHandlerSaveOpen = JsonFields.booleanValue(json, "downloadHandlerSaveOpen", false);
            response = new LinkedHashMap<>(flowSession.streamResourceResponse(path, rangeStart, rangeEndInclusive)
                    .orElseGet(() -> {
                        Map<String, Object> missing = new LinkedHashMap<>();
                        missing.put("path", normalizeDynamicResourcePath(path));
                        missing.put("artifactFamily", downloadHandlerSaveOpen
                                ? "flow-download-handler-save-open"
                                : (rangeStart >= 0 ? "flow-stream-resource-range" : "flow-stream-resource"));
                        missing.put("dynamicStreamResource", !downloadHandlerSaveOpen);
                        missing.put("dynamicDownloadHandlerResource", downloadHandlerSaveOpen);
                        missing.put("streamResourceVersion", rangeStart >= 0
                                ? "flow-ipc-stream-resource-range-v1_51"
                                : "flow-ipc-stream-resource-v1_50");
                        missing.put("downloadHandlerVersion", downloadHandlerSaveOpen
                                ? "flow-ipc-download-handler-save-open-v1_55"
                                : "");
                        missing.put("streamResourceStatus", "not-found");
                        missing.put("downloadHandlerStatus", downloadHandlerSaveOpen ? "not-found" : "");
                        missing.put("streamResourceFailureLayer", "stream-resource-registry-unavailable");
                        missing.put("downloadHandlerFailureLayer", downloadHandlerSaveOpen
                                ? "download-handler-registry-unavailable"
                                : "");
                        missing.put("streamResourceNoLocalhost", true);
                        missing.put("streamResourceFixtureSubstituted", false);
                        missing.put("downloadHandlerNoLocalhost", downloadHandlerSaveOpen);
                        missing.put("downloadHandlerFixtureSubstituted", false);
                        return missing;
                    }));
            if (chunkedDownload) {
                response.put("artifactFamily", "flow-stream-resource-chunks");
                response.put("streamResourceVersion", "flow-ipc-stream-resource-chunks-v1_52");
                response.put("streamResourceChunkedDownload", true);
                response.put("streamResourceChunkIndex", chunkIndex);
                response.put("streamResourceChunkCount", chunkCount);
                response.put("streamResourceChunkSize", chunkSize);
                response.put("streamResourceChunkRangeHeader",
                        "bytes=%d-%d".formatted(rangeStart, rangeEndInclusive));
                response.put("streamResourceChunkSequence",
                        "%d/%d".formatted(chunkIndex + 1, Math.max(chunkCount, 0)));
                response.put("streamResourceResponseMode", "range-chunk-sequential");
                response.put("streamResourceBackpressureEvidence",
                        "sequential-single-chunk-window-full-body-omitted");
            }
            if (downloadHandlerSaveOpen) {
                response.put("downloadHandlerSaveOpenRequested", true);
                response.put("downloadHandlerSaveTargetPath",
                        Objects.toString(JsonFields.string(json, "downloadSaveTargetPath"), ""));
                response.put("downloadHandlerOpenAfterSaveRequested",
                        JsonFields.booleanValue(json, "downloadOpenAfterSave", false));
                response.put("downloadHandlerUserGesture",
                        Objects.toString(JsonFields.string(json, "downloadUserGesture"), ""));
                response.put("downloadHandlerWindowLabel",
                        Objects.toString(JsonFields.string(json, "downloadWindowLabel"), "main"));
            }
        } else if (flowClientResources.contains(path)) {
            response = new LinkedHashMap<>(flowClientResources.response(path));
        } else {
            sendError("resourceNotFound", "Unknown Flow IPC resource: " + path, id, correlationId);
            return;
        }
        String purpose = JsonFields.string(json, "purpose");
        if (purpose != null && !purpose.isBlank()) {
            response.put("requestPurpose", purpose);
        }
        String lookupKind = JsonFields.string(json, "lookupKind");
        if (lookupKind != null && !lookupKind.isBlank()) {
            response.put("lookupKind", lookupKind);
        }
        send("resourceResponse", nextId("resource"), correlationIdOr(id, correlationId),
                response);
    }

    private static boolean isDynamicStreamResourcePath(String path) {
        return normalizeDynamicResourcePath(path).startsWith(DYNAMIC_RESOURCE_PREFIX);
    }

    private static String normalizeDynamicResourcePath(String path) {
        return path != null && path.startsWith("/") ? path.substring(1) : path;
    }

    private void handleUploadRequest(String json, String id, String correlationId) {
        ensureBootstrapped();
        validateEnvelopeScope(json);
        validateSecurityKey(json);
        String path = JsonFields.string(json, "path");
        if (path == null || path.isBlank()) {
            sendError("uploadTargetNotFound", "Unknown Flow IPC upload target: " + path, id, correlationId);
            return;
        }
        String fileName = JsonFields.string(json, "uploadFileName");
        if (fileName == null || fileName.isBlank()) {
            fileName = "flow-ipc-upload-v1_53.txt";
        }
        String contentType = JsonFields.string(json, "uploadContentType");
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }
        String bodyMode = JsonFields.string(json, "uploadBodyMode");
        if (bodyMode == null || bodyMode.isBlank()) {
            bodyMode = "raw-xhr-compatible";
        }
        String multipartBoundary = JsonFields.string(json, "uploadMultipartBoundary");
        String fileContentType = JsonFields.string(json, "uploadFileContentType");
        if (fileContentType == null || fileContentType.isBlank()) {
            fileContentType = contentType;
        }
        String fileSha256 = JsonFields.string(json, "uploadFileSha256");
        String progressCheckpoints = JsonFields.string(json, "uploadProgressCheckpoints");
        String progressTerminalStatus = JsonFields.string(json, "uploadProgressTerminalStatus");
        String cancelProbeResult = JsonFields.string(json, "uploadCancelProbeResult");
        String cancelFailureLayer = JsonFields.string(json, "uploadCancelFailureLayer");
        String bodyBase64 = JsonFields.string(json, "uploadBodyBase64");
        byte[] bytes = bodyBase64 == null || bodyBase64.isBlank()
                ? new byte[0]
                : Base64.getDecoder().decode(bodyBase64);
        long fileBytes = JsonFields.longValue(json, "uploadFileBytes", bytes.length);
        long progressTotalBytes = JsonFields.longValue(json, "uploadProgressTotalBytes", bytes.length);
        long progressDeliveredBytes = JsonFields.longValue(json, "uploadProgressDeliveredBytes", bytes.length);
        boolean progressConsumedByVaadinClient =
                JsonFields.booleanValue(json, "uploadProgressConsumedByVaadinClient", false);
        boolean cancelProbeAttempted = JsonFields.booleanValue(json, "uploadCancelProbeAttempted", false);
        DesktopFlowRuntimeSession.UploadRequestMetadata metadata =
                new DesktopFlowRuntimeSession.UploadRequestMetadata(
                        bodyMode,
                        multipartBoundary == null ? "" : multipartBoundary,
                        fileContentType,
                        fileBytes,
                        fileSha256 == null ? "" : fileSha256,
                        progressCheckpoints == null ? "" : progressCheckpoints,
                        progressTotalBytes,
                        progressDeliveredBytes,
                        progressTerminalStatus == null ? "" : progressTerminalStatus,
                        progressConsumedByVaadinClient,
                        cancelProbeAttempted,
                        cancelProbeResult == null ? "" : cancelProbeResult,
                        cancelFailureLayer == null ? "" : cancelFailureLayer);
        DesktopFlowRuntimeSession.FlowUploadResponse response =
                flowSession.uploadResourceResponse(path, fileName, contentType, bytes, metadata);
        Map<String, Object> payload = new LinkedHashMap<>(response.payload());
        String purpose = JsonFields.string(json, "purpose");
        if (purpose != null && !purpose.isBlank()) {
            payload.put("requestPurpose", purpose);
        }
        String lookupKind = JsonFields.string(json, "lookupKind");
        if (lookupKind != null && !lookupKind.isBlank()) {
            payload.put("lookupKind", lookupKind);
        }
        payload.put("uploadReceiverRequestBodyBase64Observed", bodyBase64 != null && !bodyBase64.isBlank());
        payload.put("uploadReceiverRequestBodyRedacted", false);
        send("uploadResponse", nextId("upload"), correlationIdOr(id, correlationId), payload);
        if (response.patch() != null) {
            sendPatch("upload", id, correlationId, response.patch(), "flow-ipc-upload-resource");
        }
    }

    private void handleClientError(String json, String id) {
        String code = JsonFields.string(json, "code");
        if (code == null || code.isBlank()) {
            code = "webviewError";
        }
        String message = JsonFields.string(json, "message");
        if (message == null || message.isBlank()) {
            message = "WebView reported a Flow IPC error";
        }
        System.err.println("FLOW_IPC_CLIENT_ERROR id=" + id + " code=" + code + " message=" + message);
        System.err.flush();
    }

    private void sendServerInitiatedPushes(String id, String correlationId) {
        sendServerInitiatedPush(
                id,
                correlationId,
                1,
                "ipc-push-status",
                "5 Server push over IPC READY");
        String secondMessageId = nextId("push");
        Map<String, Object> secondPayload = serverInitiatedPushPayload(
                2,
                "ipc-push-order-status",
                "6 Ordered push over IPC READY");
        long secondServerSequence = send(
                "push",
                secondMessageId,
                correlationIdOr(id, correlationId),
                secondPayload);

        Map<String, Object> duplicateLateProbe = new LinkedHashMap<>(secondPayload);
        duplicateLateProbe.put("reason", "server-initiated-duplicate-late-probe");
        duplicateLateProbe.put("orderingProbe", Map.of(
                "kind", "duplicate-late",
                "effectiveServerSequence", secondServerSequence,
                "replayOfMessageId", secondMessageId,
                "expectedAction", "ignore-and-request-resync"));
        send("push", nextId("push-probe"), correlationIdOr(id, correlationId), duplicateLateProbe);
    }

    private void sendServerInitiatedPush(String id, String correlationId, int pushIndex, String componentId,
                                         String text) {
        send("push", nextId("push"), correlationIdOr(id, correlationId),
                serverInitiatedPushPayload(pushIndex, componentId, text));
    }

    private Map<String, Object> serverInitiatedPushPayload(int pushIndex, String componentId, String text) {
        if (root instanceof HasComponents container) {
            flowSession.runLocked(() -> {
                Span pushed = new Span(text);
                pushed.setId(componentId);
                container.add(pushed);
            });
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reason", "server-initiated-update");
        payload.put("pushIndex", pushIndex);
        payload.put("ordering", Map.of(
                "mode", "monotonic-server-sequence",
                "expectedAction", "deliver-in-order"));
        payload.put("transport", "flow-ipc");
        payload.put("compatibilityMode", false);
        payload.put("uidlWriter", true);
        payload.put("uidl", uidlPayload("push", false));
        payload.put("tree", renderer.render(root));
        return payload;
    }

    private Component instantiateRoute() {
        try {
            return routeClass.getDeclaredConstructor().newInstance();
        } catch (InvocationTargetException error) {
            Throwable cause = error.getTargetException();
            String causeMessage = cause == null ? error.toString()
                    : cause.getClass().getName() + ": " + cause.getMessage();
            throw new IllegalStateException("Unable to instantiate Flow IPC route "
                    + routeClass.getName() + ": " + causeMessage, cause == null ? error : cause);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException error) {
            throw new IllegalStateException("Unable to instantiate Flow IPC route "
                    + routeClass.getName() + ": " + error.getMessage(), error);
        }
    }

    private void handleEvent(String json, String id, String correlationId) {
        ensureBootstrapped();
        validateEnvelopeScope(json);
        validateSecurityKey(json);
        String uidlRpc = JsonFields.object(json, "uidlRpc");
        if (uidlRpc != null && !uidlRpc.isBlank()) {
            String dispatch = JsonFields.string(json, "dispatch");
            int clientId = JsonFields.integer(uidlRpc, "clientId", -1);
            if (clientId >= 0) {
                clientToServerSequence.accumulateAndGet(clientId + 1L, Math::max);
            }
            FlowPatchResponse response = isAttachExistingElementRequest(uidlRpc)
                    ? flowSession.acknowledgeClientMessage(uidlRpc, clientId)
                    : handleClientMessage(uidlRpc, clientId);
            sendPatch("event", id, correlationId, response,
                    dispatch == null || dispatch.isBlank() ? "flow-client-rpc" : dispatch);
            return;
        }

        String action = JsonFields.string(json, "action");
        String componentId = JsonFields.string(json, "componentId");
        if (!"click".equals(action)) {
            sendError("unsupportedEvent", "Only click events are supported by the v1 minimum adapter.", id, correlationId);
            return;
        }
        Component target = renderer.component(componentId);
        if (target instanceof Button button) {
            int nodeId = button.getElement().getNode().getId();
            FlowPatchResponse response = handleDomEvent(nodeId, "click");
            sendPatch("event", id, correlationId, response, "java-translated-rpc");
        } else {
            sendError("unsupportedTarget", "No clickable Vaadin Button registered for componentId=" + componentId,
                    id, correlationId);
        }
    }

    private void handleResync(String json, String id, String correlationId) {
        ensureBootstrapped();
        validateEnvelopeScope(json);
        validateSecurityKey(json);
        String uidlRpc = JsonFields.object(json, "uidlRpc");
        if (uidlRpc != null && !uidlRpc.isBlank()) {
            String dispatch = JsonFields.string(json, "dispatch");
            int clientId = JsonFields.integer(uidlRpc, "clientId", -1);
            try {
                FlowPatchResponse response = flowSession.handleClientMessage(uidlRpc, clientId, true);
                sendPatch("resync", id, correlationId, response,
                        dispatch == null || dispatch.isBlank() ? "flow-client-xhr" : dispatch);
            } catch (IOException error) {
                throw new IllegalStateException("Unable to dispatch Flow UIDL resync: " + error.getMessage(), error);
            }
            return;
        }
        sendPatch("resync", id, correlationId);
    }

    private void validateEnvelopeScope(String json) {
        String sessionId = JsonFields.string(json, "sessionId");
        if (!Objects.equals(flowSession.sessionId(), sessionId)) {
            throw new IllegalArgumentException("Flow IPC sessionId mismatch: expected "
                    + flowSession.sessionId() + " but got " + sessionId);
        }

        String uiId = JsonFields.string(json, "uiId");
        if (!Objects.equals(IPC_UI_ID, uiId)) {
            throw new IllegalArgumentException("Flow IPC uiId mismatch: expected "
                    + IPC_UI_ID + " but got " + uiId);
        }
    }

    private void validateSecurityKey(String json) {
        String securityKey = JsonFields.string(json, "securityKey");
        if (!Objects.equals(flowSession.securityKey(), securityKey)) {
            throw new SecurityException("Invalid Flow IPC securityKey");
        }
    }

    private FlowPatchResponse handleClientMessage(String requestBody, int clientToServerMessageId) {
        try {
            int messageId = clientToServerMessageId < 0
                    ? (int) clientToServerSequence.getAndIncrement()
                    : clientToServerMessageId;
            return flowSession.handleClientMessage(requestBody, messageId, false);
        } catch (IOException error) {
            throw new IllegalStateException("Unable to dispatch Flow UIDL RPC event: " + error.getMessage(), error);
        }
    }

    private static boolean isAttachExistingElementRequest(String requestBody) {
        return requestBody != null
                && requestBody.contains("\"attachExistingElementById\"")
                && !requestBody.contains("\"event\"");
    }

    private FlowPatchResponse handleDomEvent(int nodeId, String eventType) {
        try {
            return flowSession.handleDomEvent(nodeId, eventType, (int) clientToServerSequence.getAndIncrement());
        } catch (IOException error) {
            throw new IllegalStateException("Unable to dispatch Flow UIDL RPC event: " + error.getMessage(), error);
        }
    }

    private void sendPatch(String reason, String id, String correlationId) {
        sendPatch(reason, id, correlationId, null);
    }

    private void sendPatch(String reason, String id, String correlationId, FlowPatchResponse uidlResponse) {
        sendPatch(reason, id, correlationId, uidlResponse, uidlResponse == null ? "uidl-writer" : "unknown");
    }

    private void sendPatch(String reason, String id, String correlationId, FlowPatchResponse uidlResponse,
                           String eventDispatch) {
        ensureBootstrapped();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("patchType", "replaceRoot");
        payload.put("reason", reason);
        payload.put("transport", "flow-ipc");
        payload.put("compatibilityMode", false);
        payload.put("eventDispatch", eventDispatch);
        payload.put("uidl", uidlResponse == null
                ? uidlPayload(reason, "resync".equals(reason))
                : uidlPayload(reason, uidlResponse, eventDispatch));
        payload.put("tree", renderer.render(root));
        send("patch", nextId("patch"), correlationIdOr(id, correlationId), payload);
    }

    private void ensureBootstrapped() {
        if (root == null) {
            flowSession = DesktopFlowRuntimeSession.create(
                    IPC_SESSION_ID,
                    1,
                    "flow-ipc",
                    this::instantiateRoute);
            root = flowSession.root();
        }
        if (flowSession == null) {
            flowSession = DesktopFlowRuntimeSession.create(
                    IPC_SESSION_ID,
                    1,
                    "flow-ipc",
                    root);
        }
    }

    private Map<String, Object> uidlPayload(String reason, boolean resync) {
        boolean async = "push".equals(reason);
        String uidlJson = "bootstrap".equals(reason)
                ? flowSession.createBootstrapUidl()
                : flowSession.createUidl(async, resync);
        Map<String, Object> uidl = new LinkedHashMap<>();
        uidl.put("source", "UidlWriter");
        uidl.put("format", "vaadin-uidl-json");
        uidl.put("reason", reason);
        uidl.put("resync", resync);
        uidl.put("async", async);
        uidl.put("desktopSession", true);
        uidl.put("consumedByWebView", true);
        uidl.put("consumer", "bootstrap".equals(reason) || "push".equals(reason)
                ? "flow-client-engine-visible"
                : "minimum-webview-uidl-applier-fallback");
        uidl.put("containsChanges", uidlJson.contains("\"changes\""));
        uidl.put("containsSyncId", uidlJson.contains("\"syncId\""));
        uidl.put("containsSecurityKey", uidlJson.contains("\"" + ApplicationConstants.UIDL_SECURITY_TOKEN_ID + "\""));
        uidl.put("body", uidlJson);
        return uidl;
    }

    private Map<String, Object> uidlPayload(String reason, FlowPatchResponse response, String eventDispatch) {
        String uidlJson = response.uidlJson();
        Map<String, Object> uidl = new LinkedHashMap<>();
        uidl.put("source", "UidlRequestHandler");
        uidl.put("format", "vaadin-uidl-json");
        uidl.put("reason", reason);
        uidl.put("resync", response.resync());
        uidl.put("desktopSession", response.desktopVaadinRequestUsed());
        uidl.put("consumedByWebView", true);
        uidl.put("consumer", "flow-client-xhr".equals(eventDispatch)
                ? "flow-client-engine-visible"
                : "minimum-webview-uidl-applier-fallback");
        uidl.put("statusCode", response.statusCode());
        uidl.put("contentType", response.contentType());
        uidl.put("containsChanges", uidlJson.contains("\"changes\""));
        uidl.put("containsSyncId", uidlJson.contains("\"syncId\""));
        uidl.put("body", uidlJson);
        return uidl;
    }

    private long send(String type, String id, String correlationId, Map<String, Object> payload) {
        Map<String, Object> sequencedPayload = nullSafePayload(payload);
        long nextServerSequence = serverSequence.incrementAndGet();
        sequencedPayload.put("serverSequence", nextServerSequence);
        sequencedPayload.put("serverSequenceScope", "java-sidecar-to-webview");
        FlowIpcEnvelope envelope = FlowIpcEnvelope.message(type, id, correlationId,
                IPC_SESSION_ID, IPC_UI_ID, route, sequencedPayload);
        System.out.println(PREFIX + IpcJson.stringify(envelope));
        System.out.flush();
        return nextServerSequence;
    }

    private static Map<String, Object> nullSafePayload(Map<String, Object> payload) {
        Map<String, Object> copy = new LinkedHashMap<>();
        payload.forEach((key, value) -> copy.put(key, Objects.requireNonNullElse(value, "")));
        return copy;
    }

    private void sendError(String code, String message, String id, String correlationId) {
        FlowIpcEnvelope envelope = FlowIpcEnvelope.error(
                id == null ? nextId("error") : id,
                correlationId,
                IPC_SESSION_ID,
                IPC_UI_ID,
                route,
                code,
                message);
        System.out.println(PREFIX + IpcJson.stringify(envelope));
        System.out.flush();
    }

    private String nextId(String prefix) {
        return prefix + "-" + sequence.incrementAndGet();
    }

    private static String correlationIdOr(String id, String correlationId) {
        return correlationId == null || correlationId.isBlank() ? id : correlationId;
    }

    private static final class JsonFields {
        private static final Pattern STRING_FIELD = Pattern.compile(
                "\"%s\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");

        private JsonFields() {
        }

        static String string(String json, String field) {
            Matcher matcher = Pattern.compile(String.format(STRING_FIELD.pattern(), Pattern.quote(field))).matcher(json);
            if (!matcher.find()) {
                return null;
            }
            return unescape(matcher.group(1));
        }

        static int integer(String json, String field, int fallback) {
            Matcher matcher = Pattern.compile(
                    "\"" + Pattern.quote(field) + "\"\\s*:\\s*(-?\\d+)").matcher(json);
            if (!matcher.find()) {
                return fallback;
            }
            return Integer.parseInt(matcher.group(1));
        }

        static long longValue(String json, String field, long fallback) {
            Matcher matcher = Pattern.compile(
                    "\"" + Pattern.quote(field) + "\"\\s*:\\s*(-?\\d+)").matcher(json);
            if (!matcher.find()) {
                return fallback;
            }
            return Long.parseLong(matcher.group(1));
        }

        static boolean bool(String json, String field, boolean fallback) {
            Matcher matcher = Pattern.compile(
                    "\"" + Pattern.quote(field) + "\"\\s*:\\s*(true|false)").matcher(json);
            if (!matcher.find()) {
                return fallback;
            }
            return Boolean.parseBoolean(matcher.group(1));
        }

        static boolean booleanValue(String json, String field, boolean fallback) {
            return bool(json, field, fallback);
        }

        static String object(String json, String field) {
            Matcher matcher = Pattern.compile(
                    "\"" + Pattern.quote(field) + "\"\\s*:\\s*\\{").matcher(json);
            if (!matcher.find()) {
                return null;
            }
            int start = matcher.end() - 1;
            int depth = 0;
            boolean inString = false;
            boolean escaped = false;
            for (int index = start; index < json.length(); index++) {
                char character = json.charAt(index);
                if (escaped) {
                    escaped = false;
                    continue;
                }
                if (character == '\\') {
                    escaped = inString;
                    continue;
                }
                if (character == '"') {
                    inString = !inString;
                    continue;
                }
                if (inString) {
                    continue;
                }
                if (character == '{') {
                    depth++;
                } else if (character == '}') {
                    depth--;
                    if (depth == 0) {
                        return json.substring(start, index + 1);
                    }
                }
            }
            return null;
        }

        private static String unescape(String value) {
            return value.replace("\\\"", "\"")
                    .replace("\\/", "/")
                    .replace("\\\\", "\\")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t");
        }

        static String topLevelString(String json, String field) {
            if (json == null || json.isBlank()) {
                return null;
            }

            int depth = 0;
            for (int index = 0; index < json.length(); ) {
                char character = json.charAt(index);
                if (character == '"') {
                    JsonString parsed = parseJsonString(json, index);
                    if (depth == 1 && field.equals(parsed.value())) {
                        int cursor = skipWhitespace(json, parsed.end() + 1);
                        if (cursor < json.length() && json.charAt(cursor) == ':') {
                            cursor = skipWhitespace(json, cursor + 1);
                            if (cursor < json.length() && json.charAt(cursor) == '"') {
                                return parseJsonString(json, cursor).value();
                            }
                        }
                    }
                    index = parsed.end() + 1;
                    continue;
                }
                if (character == '{' || character == '[') {
                    depth++;
                } else if (character == '}' || character == ']') {
                    depth--;
                }
                index++;
            }
            return null;
        }

        private static int skipWhitespace(String json, int index) {
            int cursor = index;
            while (cursor < json.length() && Character.isWhitespace(json.charAt(cursor))) {
                cursor++;
            }
            return cursor;
        }

        private static JsonString parseJsonString(String json, int quoteIndex) {
            StringBuilder value = new StringBuilder();
            boolean escaped = false;
            for (int index = quoteIndex + 1; index < json.length(); index++) {
                char character = json.charAt(index);
                if (escaped) {
                    value.append(switch (character) {
                        case '"' -> '"';
                        case '\\' -> '\\';
                        case '/' -> '/';
                        case 'n' -> '\n';
                        case 'r' -> '\r';
                        case 't' -> '\t';
                        default -> character;
                    });
                    escaped = false;
                } else if (character == '\\') {
                    escaped = true;
                } else if (character == '"') {
                    return new JsonString(value.toString(), index);
                } else {
                    value.append(character);
                }
            }
            return new JsonString(value.toString(), json.length() - 1);
        }

        private record JsonString(String value, int end) {
        }
    }
}
