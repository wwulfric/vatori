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
        String id = null;
        String correlationId = null;
        try {
            FlowIpcInboundEnvelope envelope = FlowIpcInboundEnvelope.parse(json);
            String type = envelope.type();
            id = envelope.id();
            correlationId = envelope.correlationId();
            switch (type == null ? "" : type) {
                case "bootstrapRequest" -> bootstrap(envelope, id, correlationId);
                case "resourceRequest" -> handleResourceRequest(envelope, id, correlationId);
                case "uploadRequest" -> handleUploadRequest(envelope, id, correlationId);
                case "event" -> handleEvent(envelope, id, correlationId);
                case "resync" -> handleResync(envelope, id, correlationId);
                case "error" -> handleClientError(envelope, id);
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

    private void bootstrap(FlowIpcInboundEnvelope envelope, String id, String correlationId) {
        ensureBootstrapped();
        validateEnvelopeScope(envelope);
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

    private void handleResourceRequest(FlowIpcInboundEnvelope envelope, String id, String correlationId) {
        ensureBootstrapped();
        validateEnvelopeScope(envelope);
        validateSecurityKey(envelope);
        String path = envelope.string("path");
        if (path == null || path.isBlank()) {
            sendError("resourceNotFound", "Unknown Flow IPC resource: " + path, id, correlationId);
            return;
        }
        Map<String, Object> response;
        if (isDynamicStreamResourcePath(path)) {
            int rangeStart = envelope.integer("streamResourceRangeStart", -1);
            int rangeEndInclusive = envelope.integer("streamResourceRangeEndInclusive", -1);
            boolean chunkedDownload = envelope.booleanValue("streamResourceChunkedDownload", false);
            int chunkIndex = envelope.integer("streamResourceChunkIndex", -1);
            int chunkCount = envelope.integer("streamResourceChunkCount", -1);
            int chunkSize = envelope.integer("streamResourceChunkSize", -1);
            boolean downloadHandlerSaveOpen = envelope.booleanValue("downloadHandlerSaveOpen", false);
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
                        Objects.toString(envelope.string("downloadSaveTargetPath"), ""));
                response.put("downloadHandlerOpenAfterSaveRequested",
                        envelope.booleanValue("downloadOpenAfterSave", false));
                response.put("downloadHandlerUserGesture",
                        Objects.toString(envelope.string("downloadUserGesture"), ""));
                response.put("downloadHandlerWindowLabel",
                        Objects.toString(envelope.string("downloadWindowLabel"), "main"));
            }
        } else if (flowClientResources.contains(path)) {
            response = new LinkedHashMap<>(flowClientResources.response(path));
        } else {
            sendError("resourceNotFound", "Unknown Flow IPC resource: " + path, id, correlationId);
            return;
        }
        String purpose = envelope.string("purpose");
        if (purpose != null && !purpose.isBlank()) {
            response.put("requestPurpose", purpose);
        }
        String lookupKind = envelope.string("lookupKind");
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

    private void handleUploadRequest(FlowIpcInboundEnvelope envelope, String id, String correlationId) {
        ensureBootstrapped();
        validateEnvelopeScope(envelope);
        validateSecurityKey(envelope);
        String path = envelope.string("path");
        if (path == null || path.isBlank()) {
            sendError("uploadTargetNotFound", "Unknown Flow IPC upload target: " + path, id, correlationId);
            return;
        }
        String fileName = envelope.string("uploadFileName");
        if (fileName == null || fileName.isBlank()) {
            fileName = "flow-ipc-upload-v1_53.txt";
        }
        String contentType = envelope.string("uploadContentType");
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }
        String bodyMode = envelope.string("uploadBodyMode");
        if (bodyMode == null || bodyMode.isBlank()) {
            bodyMode = "raw-xhr-compatible";
        }
        String multipartBoundary = envelope.string("uploadMultipartBoundary");
        String fileContentType = envelope.string("uploadFileContentType");
        if (fileContentType == null || fileContentType.isBlank()) {
            fileContentType = contentType;
        }
        String fileSha256 = envelope.string("uploadFileSha256");
        String progressCheckpoints = envelope.string("uploadProgressCheckpoints");
        String progressTerminalStatus = envelope.string("uploadProgressTerminalStatus");
        String cancelProbeResult = envelope.string("uploadCancelProbeResult");
        String cancelFailureLayer = envelope.string("uploadCancelFailureLayer");
        String bodyBase64 = envelope.string("uploadBodyBase64");
        byte[] bytes = bodyBase64 == null || bodyBase64.isBlank()
                ? new byte[0]
                : Base64.getDecoder().decode(bodyBase64);
        long fileBytes = envelope.longValue("uploadFileBytes", bytes.length);
        long progressTotalBytes = envelope.longValue("uploadProgressTotalBytes", bytes.length);
        long progressDeliveredBytes = envelope.longValue("uploadProgressDeliveredBytes", bytes.length);
        boolean progressConsumedByVaadinClient =
                envelope.booleanValue("uploadProgressConsumedByVaadinClient", false);
        boolean cancelProbeAttempted = envelope.booleanValue("uploadCancelProbeAttempted", false);
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
        String purpose = envelope.string("purpose");
        if (purpose != null && !purpose.isBlank()) {
            payload.put("requestPurpose", purpose);
        }
        String lookupKind = envelope.string("lookupKind");
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

    private void handleClientError(FlowIpcInboundEnvelope envelope, String id) {
        String code = envelope.errorString("code");
        if (code == null || code.isBlank()) {
            code = "webviewError";
        }
        String message = envelope.errorString("message");
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

    private void handleEvent(FlowIpcInboundEnvelope envelope, String id, String correlationId) {
        ensureBootstrapped();
        validateEnvelopeScope(envelope);
        validateSecurityKey(envelope);
        String uidlRpc = envelope.objectJson("uidlRpc");
        if (uidlRpc != null && !uidlRpc.isBlank()) {
            String dispatch = envelope.string("dispatch");
            int clientId = envelope.objectInteger("uidlRpc", "clientId", -1);
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

        String action = envelope.string("action");
        String componentId = envelope.string("componentId");
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

    private void handleResync(FlowIpcInboundEnvelope envelope, String id, String correlationId) {
        ensureBootstrapped();
        validateEnvelopeScope(envelope);
        validateSecurityKey(envelope);
        String uidlRpc = envelope.objectJson("uidlRpc");
        if (uidlRpc != null && !uidlRpc.isBlank()) {
            String dispatch = envelope.string("dispatch");
            int clientId = envelope.objectInteger("uidlRpc", "clientId", -1);
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

    private void validateEnvelopeScope(FlowIpcInboundEnvelope envelope) {
        String sessionId = envelope.sessionId();
        if (!Objects.equals(flowSession.sessionId(), sessionId)) {
            throw new IllegalArgumentException("Flow IPC sessionId mismatch: expected "
                    + flowSession.sessionId() + " but got " + sessionId);
        }

        String uiId = envelope.uiId();
        if (!Objects.equals(IPC_UI_ID, uiId)) {
            throw new IllegalArgumentException("Flow IPC uiId mismatch: expected "
                    + IPC_UI_ID + " but got " + uiId);
        }
    }

    private void validateSecurityKey(FlowIpcInboundEnvelope envelope) {
        String securityKey = envelope.string("securityKey");
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
}
