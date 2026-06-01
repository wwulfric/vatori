package dev.vatori.desktop.flow.transport;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.MessageDigestUtil;
import com.vaadin.flow.server.AbstractStreamResource;
import com.vaadin.flow.server.PwaRegistry;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.SessionLockCheckStrategy;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.StreamResourceRegistry.ElementStreamResource;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.shared.communication.PushMode;
import dev.vatori.desktop.ipc.IpcJson;
import jakarta.servlet.http.Part;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DesktopFlowRuntimeSession {
    private static final Pattern JSON_STRING_FIELD = Pattern.compile("\"%s\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");
    private static final Pattern JSON_INT_FIELD = Pattern.compile("\"%s\"\\s*:\\s*(-?\\d+)");

    private final VaadinSession session;
    private final UI ui;
    private final Component root;
    private final UidlFlowTransportAdapter uidlAdapter;
    private final String sessionId;
    private final int uiId;
    private int expectedClientToServerMessageId;
    private int lastSyncId = -1;

    private DesktopFlowRuntimeSession(VaadinSession session, UI ui, Component root, UidlFlowTransportAdapter uidlAdapter,
                                      String sessionId, int uiId) {
        this.session = Objects.requireNonNull(session, "session");
        this.ui = Objects.requireNonNull(ui, "ui");
        this.root = Objects.requireNonNull(root, "root");
        this.uidlAdapter = Objects.requireNonNull(uidlAdapter, "uidlAdapter");
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.uiId = uiId;
    }

    public static DesktopFlowRuntimeSession create(String sessionId, int uiId, String appId, Component root) {
        Objects.requireNonNull(root, "root");
        return create(sessionId, uiId, appId, () -> root);
    }

    public static DesktopFlowRuntimeSession create(String sessionId, int uiId, String appId,
                                                   Supplier<Component> rootSupplier) {
        Objects.requireNonNull(rootSupplier, "rootSupplier");

        DesktopFlowVaadinService service = new DesktopFlowVaadinService(appId);
        WrappedSession wrappedSession = new DesktopWrappedSession(sessionId);
        VaadinSession session = new VaadinSession(service);
        session.refreshTransients(wrappedSession, service);
        service.storeSession(wrappedSession, session);

        session.lock();
        try {
            UI ui = new UI();
            ui.getInternals().setSession(session);
            FlowClientMessage bootstrapMessage = new FlowClientMessage(
                    wrappedSession.getId(),
                    uiId,
                    "",
                    "",
                    "",
                    -1,
                    false);
            VaadinRequest request = DesktopVaadinRequest.uidl(session, bootstrapMessage);
            DesktopVaadinResponse response = new DesktopVaadinResponse(service);
            Map<Class<?>, CurrentInstance> oldInstances = CurrentInstance.getInstances();
            CurrentInstance.setCurrent(ui);
            CurrentInstance.set(VaadinRequest.class, request);
            CurrentInstance.set(VaadinResponse.class, response);
            try {
                ui.doInit(request, uiId, appId);
                Component root = Objects.requireNonNull(rootSupplier.get(), "rootSupplier.get()");
                ui.add(root);
                session.addUI(ui);
                return new DesktopFlowRuntimeSession(session, ui, root, new UidlFlowTransportAdapter(),
                        wrappedSession.getId(), uiId);
            } finally {
                CurrentInstance.clearAll();
                CurrentInstance.restoreInstances(oldInstances);
            }
        } finally {
            session.unlock();
        }
    }

    public UI ui() {
        return ui;
    }

    public Component root() {
        return root;
    }

    public String csrfToken() {
        return ui.getCsrfToken();
    }

    public String securityKey() {
        return ui.getCsrfToken();
    }

    public String sessionId() {
        return sessionId;
    }

    public int uiId() {
        return uiId;
    }

    public void runLocked(Runnable action) {
        session.lock();
        try {
            action.run();
        } finally {
            session.unlock();
        }
    }

    public String createUidl(boolean resync) {
        return createUidl(false, resync);
    }

    public String createUidl(boolean async, boolean resync) {
        session.lock();
        try {
            String uidl = uidlAdapter.createUidl(ui, async, resync);
            observeSyncId(uidl);
            return uidl;
        } finally {
            session.unlock();
        }
    }

    public String createBootstrapUidl() {
        session.lock();
        try {
            String uidl = addBootstrapSecurityKey(uidlAdapter.createUidl(ui, false));
            observeSyncId(uidl);
            return uidl;
        } finally {
            session.unlock();
        }
    }

    public FlowPatchResponse handleClientMessage(String requestBody, int clientToServerMessageId,
                                                 boolean resync) throws IOException {
        validateClientMessage(requestBody, clientToServerMessageId);
        FlowClientMessage message = new FlowClientMessage(
                sessionId,
                uiId,
                "",
                requestBody,
                ui.getCsrfToken(),
                clientToServerMessageId,
                resync);
        FlowPatchResponse response = uidlAdapter.handleClientMessage(ui, message);
        observeSyncId(response.uidlJson());
        // Recovery resync is a side-channel in this IPC slice; it must not consume the next event id.
        if (!resync) {
            expectedClientToServerMessageId = clientToServerMessageId + 1;
        }
        return response;
    }

    public FlowPatchResponse acknowledgeClientMessage(String requestBody, int clientToServerMessageId) {
        session.lock();
        try {
            validateClientMessage(requestBody, clientToServerMessageId);
            String hashMessage = requestBody.length() > 64 * 1024
                    ? requestBody.substring(0, 64 * 1024)
                    : requestBody;
            ui.getInternals().setLastProcessedClientToServerId(
                    clientToServerMessageId, MessageDigestUtil.sha256(hashMessage));
            expectedClientToServerMessageId = clientToServerMessageId + 1;
            String uidlJson = """
                    {"clientId":%d,"syncId":%d}
                    """.formatted(expectedClientToServerMessageId, lastSyncId);
            ui.getInternals().setLastRequestResponse(uidlJson);
            return new FlowPatchResponse(
                    sessionId,
                    uiId,
                    uidlJson,
                    "application/json; charset=UTF-8",
                    200,
                    Collections.emptyMap(),
                    true,
                    false);
        } finally {
            session.unlock();
        }
    }

    public FlowUploadResponse uploadResourceResponse(String path, String fileName, String contentType, byte[] bytes) {
        return uploadResourceResponse(path, fileName, contentType, bytes, UploadRequestMetadata.raw());
    }

    public FlowUploadResponse uploadResourceResponse(String path, String fileName, String contentType, byte[] bytes,
                                                     UploadRequestMetadata metadata) {
        Objects.requireNonNull(path, "path");
        UploadRequestMetadata requestMetadata = metadata == null ? UploadRequestMetadata.raw() : metadata;
        byte[] uploadBytes = bytes == null ? new byte[0] : bytes.clone();
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        boolean multipart = requestMetadata.multipart();
        String uploadVersion = multipart
                ? "flow-ipc-upload-multipart-v1_54"
                : "flow-ipc-upload-receiver-v1_53";
        String uploadTransport = multipart
                ? "ipc-upload-request-multipart-form-data"
                : "ipc-upload-request-raw-xhr-compatible";
        session.lock();
        try {
            URI uri = dynamicResourceUri(normalizedPath);
            AbstractStreamResource abstractResource = session.getResourceRegistry().getResource(uri).orElse(null);
            if (abstractResource == null) {
                return new FlowUploadResponse(uploadFailurePayload(normalizedPath, fileName, contentType, uploadBytes,
                        "not-found", "upload-resource-registry-unavailable", "", requestMetadata), null);
            }
            if (!(abstractResource instanceof ElementStreamResource elementResource)) {
                return new FlowUploadResponse(uploadFailurePayload(normalizedPath, fileName, contentType, uploadBytes,
                        abstractResource.getClass().getName(), "upload-resource-not-element-request-handler", "",
                        requestMetadata), null);
            }

            DesktopVaadinRequest request = DesktopVaadinRequest.upload(session, sessionId, uiId, normalizedPath,
                    uploadBytes, fileName, contentType);
            String receiverFileName = request.firstPart()
                    .map(part -> Objects.toString(part.getSubmittedFileName(), fileName))
                    .orElse(fileName);
            String receiverContentType = request.firstPart()
                    .map(part -> Objects.toString(part.getContentType(), requestMetadata.fileContentType()))
                    .orElse(contentType);
            long receiverFileBytes = request.firstPart()
                    .map(Part::getSize)
                    .orElse((long) uploadBytes.length);
            DesktopVaadinResponse response = new DesktopVaadinResponse(session.getService());
            Map<Class<?>, CurrentInstance> oldInstances = CurrentInstance.getInstances();
            CurrentInstance.setCurrent(ui);
            CurrentInstance.set(VaadinRequest.class, request);
            CurrentInstance.set(VaadinResponse.class, response);
            try {
                elementResource.getElementRequestHandler().handleRequest(request, response, session,
                        elementResource.getOwner());
                String uidlJson = uidlAdapter.createUidl(ui, false, false);
                observeSyncId(uidlJson);
                FlowPatchResponse patch = new FlowPatchResponse(
                        sessionId,
                        uiId,
                        uidlJson,
                        "application/json; charset=UTF-8",
                        response.statusCode(),
                        response.headers(),
                        true,
                        false);
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("path", normalizedPath);
                payload.put("artifactFamily", "flow-upload-receiver");
                payload.put("dynamicUploadResource", true);
                payload.put("uploadReceiverVersion", multipart
                        ? "flow-ipc-upload-receiver-v1_53+multipart-v1_54"
                        : "flow-ipc-upload-receiver-v1_53");
                payload.put("uploadReceiverRegistrySource", "VaadinSession.StreamResourceRegistry");
                payload.put("uploadReceiverResourceType", ElementStreamResource.class.getName());
                payload.put("uploadReceiverConcreteResourceType", abstractResource.getClass().getName());
                payload.put("uploadReceiverHandlerType",
                        elementResource.getElementRequestHandler().getClass().getName());
                payload.put("uploadReceiverOwnerNodeId", elementResource.getOwner().getNode().getId());
                payload.put("uploadReceiverTransport", uploadTransport);
                payload.put("uploadReceiverNoLocalhost", true);
                payload.put("uploadReceiverFixtureSubstituted", false);
                payload.put("uploadReceiverMultipartParity", multipart);
                payload.put("uploadReceiverStatus", response.statusCode() >= 200 && response.statusCode() < 300
                        ? "resolved"
                        : "response-status-" + response.statusCode());
                payload.put("uploadReceiverFileName", receiverFileName);
                payload.put("uploadReceiverContentType", receiverContentType);
                payload.put("uploadReceiverBytes", uploadBytes.length);
                payload.put("uploadReceiverSha256", MessageDigestUtil.sha256Hex(uploadBytes));
                payload.put("uploadReceiverResponseStatusCode", response.statusCode());
                payload.put("uploadReceiverResponseContentType", Objects.toString(response.contentType(), ""));
                payload.put("uploadReceiverResponseHeaders", response.headers());
                payload.put("uploadReceiverResponseBody", Objects.toString(response.bodyAsString(), ""));
                payload.put("uploadReceiverPatchContainsChanges", uidlJson.contains("\"changes\""));
                String expectedStatusPrefix = multipart ? "Multipart upload received: " : "Upload received: ";
                payload.put("uploadReceiverPatchContainsStatus",
                        uidlJson.contains(expectedStatusPrefix + receiverFileName));
                payload.put("uploadReceiverFailureLayer", "");
                payload.put("uploadReceiverNextResolutionStep",
                        multipart
                                ? "turn bounded multipart evidence into multi-file/streaming progress/cancel parity"
                                : "extend raw UploadHandler path toward multipart/progress/cancel/backpressure parity");
                if (multipart) {
                    putUploadMultipartPayload(payload, normalizedPath, receiverFileName, receiverContentType,
                            receiverFileBytes, uploadBytes, request, response, uidlJson, requestMetadata,
                            elementResource.getElementRequestHandler().getClass().getName(), uploadVersion);
                }
                return new FlowUploadResponse(payload, patch);
            } catch (IOException error) {
                throw new IllegalStateException("Unable to dispatch Flow IPC upload resource: "
                        + error.getMessage(), error);
            } finally {
                CurrentInstance.clearAll();
                CurrentInstance.restoreInstances(oldInstances);
            }
        } finally {
            session.unlock();
        }
    }

    private static Map<String, Object> uploadFailurePayload(String normalizedPath, String fileName, String contentType,
                                                           byte[] uploadBytes, String resourceType,
                                                           String failureLayer, String responseBody,
                                                           UploadRequestMetadata metadata) {
        UploadRequestMetadata requestMetadata = metadata == null ? UploadRequestMetadata.raw() : metadata;
        boolean multipart = requestMetadata.multipart();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("path", normalizedPath);
        payload.put("artifactFamily", "flow-upload-receiver");
        payload.put("dynamicUploadResource", true);
        payload.put("uploadReceiverVersion", multipart
                ? "flow-ipc-upload-receiver-v1_53+multipart-v1_54"
                : "flow-ipc-upload-receiver-v1_53");
        payload.put("uploadReceiverRegistrySource", "VaadinSession.StreamResourceRegistry");
        payload.put("uploadReceiverResourceType", resourceType);
        payload.put("uploadReceiverTransport", multipart
                ? "ipc-upload-request-multipart-form-data"
                : "ipc-upload-request-raw-xhr-compatible");
        payload.put("uploadReceiverHandlerType", "");
        payload.put("uploadReceiverResponseStatusCode", 0);
        payload.put("uploadReceiverResponseContentType", "");
        payload.put("uploadReceiverNoLocalhost", true);
        payload.put("uploadReceiverFixtureSubstituted", false);
        payload.put("uploadReceiverMultipartParity", multipart);
        payload.put("uploadReceiverStatus", "not-resolved");
        payload.put("uploadReceiverFileName", fileName);
        payload.put("uploadReceiverContentType", contentType);
        payload.put("uploadReceiverBytes", uploadBytes.length);
        payload.put("uploadReceiverSha256", MessageDigestUtil.sha256Hex(uploadBytes));
        payload.put("uploadReceiverResponseBody", responseBody);
        payload.put("uploadReceiverFailureLayer", failureLayer);
        payload.put("uploadReceiverNextResolutionStep",
                "resolve vaadin-upload target to ElementStreamResource and invoke UploadHandler");
        if (multipart) {
            putUploadMultipartPayload(payload, normalizedPath, fileName, requestMetadata.fileContentType(),
                    requestMetadata.fileBytes(), uploadBytes, null, null, "", requestMetadata, "",
                    "flow-ipc-upload-multipart-v1_54");
            payload.put("uploadMultipartFailureLayer", failureLayer);
        }
        return payload;
    }

    private static void putUploadMultipartPayload(Map<String, Object> payload, String normalizedPath, String fileName,
                                                  String fileContentType, long fileBytes, byte[] uploadBytes,
                                                  DesktopVaadinRequest request, DesktopVaadinResponse response,
                                                  String uidlJson, UploadRequestMetadata metadata,
                                                  String handlerType, String version) {
        int partCount = request == null ? 0 : request.partCount();
        payload.put("uploadMultipartVersion", version);
        payload.put("uploadMultipartSource", "upload-receiver-v1_53");
        payload.put("uploadMultipartScope",
                "framework-validation-app-upload-element-request-handler-bounded-multipart-progress-cancel");
        payload.put("uploadMultipartAppShape", "framework-validation-app");
        payload.put("uploadMultipartJavaClasses",
                "com.vaadin.flow.component.upload.Upload,com.vaadin.flow.server.streams.UploadHandler,com.vaadin.flow.server.StreamResourceRegistry.ElementStreamResource,jakarta.servlet.http.Part");
        payload.put("uploadMultipartComponentIds", "framework-upload,framework-upload-status");
        payload.put("uploadMultipartDynamicPathPrefix", "VAADIN/dynamic/resource/");
        payload.put("uploadMultipartTargetPath", normalizedPath);
        payload.put("uploadMultipartRequestBodyMode", "multipart-form-data");
        payload.put("uploadMultipartBoundary", metadata.multipartBoundary());
        payload.put("uploadMultipartPartHeaders",
                "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName
                        + "\",Content-Type: " + fileContentType);
        payload.put("uploadMultipartFileName", fileName);
        payload.put("uploadMultipartFileContentType", fileContentType);
        payload.put("uploadMultipartFileBytes", fileBytes);
        payload.put("uploadMultipartFileSha256", metadata.fileSha256());
        payload.put("uploadMultipartBytes", uploadBytes.length);
        payload.put("uploadMultipartSha256", MessageDigestUtil.sha256Hex(uploadBytes));
        payload.put("uploadMultipartPartCount", partCount);
        payload.put("uploadMultipartReceiverUsesHttpServletRequest", request instanceof jakarta.servlet.http.HttpServletRequest);
        payload.put("uploadMultipartResourceType", payload.getOrDefault("uploadReceiverResourceType", ""));
        payload.put("uploadMultipartHandlerType", handlerType);
        payload.put("uploadMultipartStatus", payload.getOrDefault("uploadReceiverStatus", "not-resolved"));
        payload.put("uploadMultipartResponseStatusCode", response == null ? 0 : response.statusCode());
        payload.put("uploadMultipartPatchContainsChanges", uidlJson.contains("\"changes\""));
        payload.put("uploadMultipartPatchContainsStatus",
                uidlJson.contains("Multipart upload received: " + fileName));
        payload.put("uploadMultipartProgressCheckpoints", metadata.progressCheckpoints());
        payload.put("uploadMultipartProgressTotalBytes", metadata.progressTotalBytes());
        payload.put("uploadMultipartProgressDeliveredBytes", metadata.progressDeliveredBytes());
        payload.put("uploadMultipartProgressTerminalStatus", metadata.progressTerminalStatus());
        payload.put("uploadMultipartProgressConsumedByVaadinClient", metadata.progressConsumedByVaadinClient());
        payload.put("uploadMultipartCancelProbeAttempted", metadata.cancelProbeAttempted());
        payload.put("uploadMultipartCancelProbeResult", metadata.cancelProbeResult());
        payload.put("uploadMultipartCancelFailureLayer", metadata.cancelFailureLayer());
        payload.put("uploadMultipartNoLocalhost", true);
        payload.put("uploadMultipartFixtureSubstituted", false);
        payload.put("uploadMultipartParity", true);
        payload.put("uploadMultipartFailureLayer", "");
        payload.put("uploadMultipartRemainingBlockers",
                "multi-file-upload,large-file-streaming,parallel-upload,retry-resume,real-backpressure,upload-client-progress-ui-consumption,cancel-abort-wiring,downloadhandler-parity,os-file-grants,dev-mode-vite-parity,installer-packaging,durable-reconnect");
        payload.put("uploadMultipartNextResolutionStep",
                "turn bounded single-file multipart evidence into streaming upload with real progress/cancel/backpressure");
    }

    public Optional<Map<String, Object>> streamResourceResponse(String path, int rangeStart, int rangeEndInclusive) {
        Objects.requireNonNull(path, "path");
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        AbstractStreamResource abstractResource;
        session.lock();
        try {
            URI uri = dynamicResourceUri(normalizedPath);
            abstractResource = session.getResourceRegistry().getResource(uri).orElse(null);
        } finally {
            session.unlock();
        }
        if (abstractResource == null) {
            return Optional.empty();
        }
        if (!(abstractResource instanceof StreamResource resource)) {
            throw new IllegalArgumentException("Unsupported Flow IPC dynamic resource type: "
                    + abstractResource.getClass().getName());
        }

        byte[] bytes;
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            resource.getWriter().accept(output, session);
            bytes = output.toByteArray();
        } catch (IOException error) {
            throw new IllegalStateException("Unable to read Flow IPC StreamResource: " + error.getMessage(), error);
        }

        boolean rangeRequested = rangeStart >= 0 && rangeEndInclusive >= rangeStart;
        int effectiveRangeStart = rangeRequested ? Math.min(rangeStart, bytes.length) : 0;
        boolean rangeSatisfied = !rangeRequested || effectiveRangeStart < bytes.length;
        int effectiveRangeEndInclusive = rangeRequested && rangeSatisfied
                ? Math.min(rangeEndInclusive, bytes.length - 1)
                : Math.max(bytes.length - 1, 0);
        byte[] responseBytes = rangeRequested && rangeSatisfied
                ? Arrays.copyOfRange(bytes, effectiveRangeStart, effectiveRangeEndInclusive + 1)
                : (rangeRequested ? new byte[0] : bytes);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("path", normalizedPath);
        payload.put("artifactFamily", rangeRequested ? "flow-stream-resource-range" : "flow-stream-resource");
        payload.put("dynamicStreamResource", true);
        payload.put("streamResourceVersion", rangeRequested
                ? "flow-ipc-stream-resource-range-v1_51"
                : "flow-ipc-stream-resource-v1_50");
        payload.put("streamResourceRegistrySource", "VaadinSession.StreamResourceRegistry");
        payload.put("streamResourceTransport", "ipc-resource-request");
        payload.put("streamResourceNoLocalhost", true);
        payload.put("streamResourceFixtureSubstituted", false);
        payload.put("streamResourceStatus", rangeRequested ? "partial-content" : "resolved");
        payload.put("streamResourcePath", normalizedPath);
        payload.put("streamResourceFileName", resource.getName());
        payload.put("streamResourceContentType", contentType(resource));
        payload.put("streamResourceCacheTime", resource.getCacheTime());
        payload.put("streamResourceHeaders", resource.getHeaders());
        payload.put("streamResourceBytes", bytes.length);
        payload.put("streamResourceSha256", MessageDigestUtil.sha256Hex(bytes));
        payload.put("streamResourceBodyBase64", Base64.getEncoder().encodeToString(responseBytes));
        if (!rangeRequested && bytes.length <= 4096) {
            payload.put("streamResourceText", new String(bytes, StandardCharsets.UTF_8));
        }
        if (rangeRequested) {
            payload.put("streamResourceRangeRequested", true);
            payload.put("streamResourceRangeHeader",
                    "bytes=%d-%d".formatted(rangeStart, rangeEndInclusive));
            payload.put("streamResourceRangeStart", effectiveRangeStart);
            payload.put("streamResourceRangeEndInclusive", effectiveRangeEndInclusive);
            payload.put("streamResourceRangeBytes", responseBytes.length);
            payload.put("streamResourceRangeSha256", MessageDigestUtil.sha256Hex(responseBytes));
            payload.put("streamResourceRangeBodyBase64",
                    Base64.getEncoder().encodeToString(responseBytes));
            payload.put("streamResourceFullBytes", bytes.length);
            payload.put("streamResourceFullSha256", MessageDigestUtil.sha256Hex(bytes));
            payload.put("streamResourceFullBodyOmitted", true);
            payload.put("streamResourceResponseMode", "range-slice");
            payload.put("streamResourceRangeSatisfied", rangeSatisfied);
            payload.put("streamResourceBackpressureEvidence",
                    "full-body-integrity-recorded-but-response-body-limited-to-requested-range");
        }
        payload.put("streamResourceFailureLayer", "");
        payload.put("streamResourceNextResolutionStep",
                "extend dynamic resource handling toward DownloadHandler, Upload and streaming backpressure");
        return Optional.of(payload);
    }

    public FlowPatchResponse handleDomEvent(int nodeId, String eventType, int clientToServerMessageId)
            throws IOException {
        String requestBody = """
                {
                  "csrfToken": "%s",
                  "securityKey": "%s",
                  "rpc": [
                    {
                      "type": "event",
                      "node": %d,
                      "event": "%s",
                      "data": {}
                    }
                  ],
                  "syncId": %d,
                  "clientId": %d
                }
                """.formatted(ui.getCsrfToken(), securityKey(), nodeId, eventType, lastSyncId,
                clientToServerMessageId);
        return handleClientMessage(requestBody, clientToServerMessageId, false);
    }

    private void validateClientMessage(String requestBody, int clientToServerMessageId) {
        String token = jsonString(requestBody, "csrfToken");
        if (!Objects.equals(ui.getCsrfToken(), token)) {
            throw new SecurityException("Invalid Flow IPC csrfToken");
        }

        String securityKey = jsonString(requestBody, "securityKey");
        if (securityKey != null && !Objects.equals(securityKey(), securityKey)) {
            throw new SecurityException("Invalid Flow IPC securityKey");
        }

        Integer requestSyncId = jsonInt(requestBody, "syncId");
        if (requestSyncId == null) {
            throw new IllegalArgumentException("Missing Flow IPC syncId");
        }
        if (requestSyncId != lastSyncId) {
            throw new IllegalStateException("Flow IPC syncId mismatch: expected "
                    + lastSyncId + " but got " + requestSyncId);
        }

        Integer bodyClientId = jsonInt(requestBody, "clientId");
        if (bodyClientId == null) {
            throw new IllegalArgumentException("Missing Flow IPC clientId");
        }
        if (bodyClientId != clientToServerMessageId) {
            throw new IllegalArgumentException("Flow IPC clientId mismatch between envelope and UIDL body");
        }
        if (clientToServerMessageId != expectedClientToServerMessageId) {
            throw new IllegalStateException("Flow IPC clientId out of order: expected "
                    + expectedClientToServerMessageId + " but got " + clientToServerMessageId);
        }
    }

    private static URI dynamicResourceUri(String path) {
        try {
            return new URI(path);
        } catch (URISyntaxException error) {
            throw new IllegalArgumentException("Invalid Flow IPC dynamic resource path: " + path, error);
        }
    }

    private static String contentType(StreamResource resource) {
        try {
            String contentType = resource.getContentTypeResolver().apply(resource, null);
            return contentType == null || contentType.isBlank()
                    ? "application/octet-stream"
                    : contentType;
        } catch (RuntimeException error) {
            return "application/octet-stream";
        }
    }

    private String addBootstrapSecurityKey(String uidlJson) {
        String fieldName = ApplicationConstants.UIDL_SECURITY_TOKEN_ID;
        if (uidlJson.contains("\"" + fieldName + "\"")) {
            return uidlJson;
        }
        int insertAt = uidlJson.lastIndexOf('}');
        if (insertAt < 0) {
            throw new IllegalStateException("Unable to add Flow UIDL security key to malformed UIDL JSON");
        }
        String prefix = uidlJson.substring(0, insertAt);
        String suffix = uidlJson.substring(insertAt);
        String separator = prefix.endsWith("{") ? "" : ",";
        return prefix + separator + IpcJson.stringify(fieldName) + ":" + IpcJson.stringify(securityKey()) + suffix;
    }

    private void observeSyncId(String uidlJson) {
        Integer syncId = jsonInt(uidlJson, "syncId");
        if (syncId != null) {
            lastSyncId = syncId;
        }
    }

    private static String jsonString(String json, String field) {
        if (json == null || json.isBlank()) {
            return null;
        }
        Matcher matcher = Pattern.compile(String.format(JSON_STRING_FIELD.pattern(), Pattern.quote(field))).matcher(json);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private static Integer jsonInt(String json, String field) {
        if (json == null || json.isBlank()) {
            return null;
        }
        Matcher matcher = Pattern.compile(String.format(JSON_INT_FIELD.pattern(), Pattern.quote(field))).matcher(json);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }

    public record FlowUploadResponse(Map<String, Object> payload, FlowPatchResponse patch) {
    }

    public record UploadRequestMetadata(String bodyMode, String multipartBoundary, String fileContentType,
                                        long fileBytes, String fileSha256, String progressCheckpoints,
                                        long progressTotalBytes, long progressDeliveredBytes,
                                        String progressTerminalStatus, boolean progressConsumedByVaadinClient,
                                        boolean cancelProbeAttempted, String cancelProbeResult,
                                        String cancelFailureLayer) {
        static UploadRequestMetadata raw() {
            return new UploadRequestMetadata("raw-xhr-compatible", "", "", 0L, "", "", 0L, 0L,
                    "", false, false, "", "");
        }

        boolean multipart() {
            return "multipart-form-data".equals(bodyMode);
        }
    }

    private static final class DesktopFlowVaadinService extends VaadinService {
        private final String appId;

        private DesktopFlowVaadinService(String appId) {
            super(deploymentConfiguration());
            this.appId = appId == null || appId.isBlank() ? "flow-ipc" : appId;
        }

        @Override
        public String getMimeType(String resourceName) {
            return null;
        }

        @Override
        protected RouteRegistry getRouteRegistry() {
            return null;
        }

        @Override
        protected PwaRegistry getPwaRegistry() {
            return null;
        }

        @Override
        public String getContextRootRelativePath(VaadinRequest request) {
            return "";
        }

        @Override
        protected boolean requestCanCreateSession(VaadinRequest request) {
            return true;
        }

        @Override
        public String getServiceName() {
            return appId;
        }

        @Override
        public String getMainDivId(VaadinSession session, VaadinRequest request) {
            return "outlet";
        }

        @Override
        public URL getStaticResource(String url) {
            return null;
        }

        @Override
        public URL getResource(String url) {
            return null;
        }

        @Override
        public InputStream getResourceAsStream(String url) {
            return null;
        }

        @Override
        public String resolveResource(String url) {
            return url;
        }

        @Override
        protected VaadinContext constructVaadinContext() {
            return new DesktopFlowVaadinContext();
        }

        @Override
        protected Lock getSessionLock(WrappedSession wrappedSession) {
            Object lock = wrappedSession.getAttribute("flow-ipc-session-lock");
            if (lock instanceof Lock existing) {
                return existing;
            }
            ReentrantLock created = new ReentrantLock();
            wrappedSession.setAttribute("flow-ipc-session-lock", created);
            return created;
        }

        @Override
        public Iterable<com.vaadin.flow.server.DependencyFilter> getDependencyFilters() {
            return List.of();
        }

        private void storeSession(WrappedSession wrappedSession, VaadinSession session) {
            writeToHttpSession(wrappedSession, session);
        }
    }

    private static final class DesktopFlowVaadinContext implements VaadinContext {
        private static final Lookup EMPTY_LOOKUP = new Lookup() {
            @Override
            public <T> T lookup(Class<T> serviceClass) {
                return null;
            }

            @Override
            public <T> Collection<T> lookupAll(Class<T> serviceClass) {
                return Collections.emptyList();
            }
        };

        private final Map<Class<?>, Object> attributes = new LinkedHashMap<>();

        private DesktopFlowVaadinContext() {
            attributes.put(Lookup.class, EMPTY_LOOKUP);
        }

        @Override
        public synchronized <T> T getAttribute(Class<T> type, Supplier<T> defaultValueSupplier) {
            Object value = attributes.get(type);
            if (value == null && defaultValueSupplier != null) {
                value = defaultValueSupplier.get();
                attributes.put(type, value);
            }
            return type.cast(value);
        }

        @Override
        public synchronized <T> void setAttribute(Class<T> clazz, T value) {
            if (value == null) {
                attributes.remove(clazz);
            } else {
                attributes.put(clazz, value);
            }
        }

        @Override
        public synchronized void removeAttribute(Class<?> clazz) {
            attributes.remove(clazz);
        }

        @Override
        public Enumeration<String> getContextParameterNames() {
            return Collections.emptyEnumeration();
        }

        @Override
        public String getContextParameter(String name) {
            return null;
        }
    }

    private static DeploymentConfiguration deploymentConfiguration() {
        return (DeploymentConfiguration) Proxy.newProxyInstance(
                DesktopFlowRuntimeSession.class.getClassLoader(),
                new Class<?>[]{DeploymentConfiguration.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isProductionMode", "isRequestTiming", "isSyncIdCheckEnabled",
                            "isXsrfProtectionEnabled", "isCloseIdleSessions" -> false;
                    case "getHeartbeatInterval" -> 300;
                    case "getMaxMessageSuspendTimeout" -> 5000;
                    case "getWebComponentDisconnect" -> 300;
                    case "getPushMode" -> PushMode.DISABLED;
                    case "getSessionLockCheckStrategy" -> SessionLockCheckStrategy.ASSERT;
                    case "getProjectFolder" -> new File(".");
                    case "getFrontendFolder" -> new File("frontend");
                    case "getClassLoaderName" -> null;
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == double.class) {
            return 0D;
        }
        if (type == String.class) {
            return "";
        }
        if (type == ClassLoader.class) {
            return DesktopFlowRuntimeSession.class.getClassLoader();
        }
        return null;
    }
}
