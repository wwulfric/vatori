package dev.vatori.ipcframework;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.UploadFormat;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadEvent;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.server.streams.UploadResult;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Route("flow-external-file-drop")
public final class FlowExternalFileDropRoute extends VerticalLayout {
    private static final String PREFIX = "framework-flow-external-file-drop-";
    private static final int MAX_FILE_SIZE = 1024 * 1024;

    private final AtomicInteger handlerCalls = new AtomicInteger();
    private final AtomicInteger handlerFailures = new AtomicInteger();
    private final AtomicInteger responseCalls = new AtomicInteger();
    private final AtomicInteger successfulResponses = new AtomicInteger();
    private final AtomicInteger failedResponses = new AtomicInteger();
    private final AtomicReference<String> receivedSummary = new AtomicReference<>();
    private final AtomicReference<String> responseSummary = new AtomicReference<>();
    private int allFinished;
    private boolean allFinishedClient;
    private int rejected;
    private int removed;

    private final Span uploadStatus = new Span("External file drop upload: pending");
    private final Span responseStatus = new Span("External file drop response: pending");
    private final Span listenerStatus = new Span();

    public FlowExternalFileDropRoute() {
        setId(PREFIX + "route");
        setPadding(true);
        setSpacing(true);
        UI ownerUI = Objects.requireNonNull(UI.getCurrent());

        UploadHandler handler = new UploadHandler() {
            @Override
            public void handleUploadRequest(UploadEvent event) throws IOException {
                handlerCalls.incrementAndGet();
                try {
                    MessageDigest digest = sha256();
                    long bytesRead = 0;
                    try (InputStream input = event.getInputStream()) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = input.read(buffer)) != -1) {
                            bytesRead += read;
                            if (bytesRead > MAX_FILE_SIZE) {
                                throw new IOException("Upload exceeded the file size limit");
                            }
                            digest.update(buffer, 0, read);
                        }
                    }
                    String requestContentType = mediaType(
                            event.getRequest().getContentType());
                    receivedSummary.set("fileName=" + observed(event.getFileName())
                            + ", declaredBytes=" + event.getFileSize()
                            + ", bytesRead=" + bytesRead
                            + ", sha256=" + HexFormat.of().formatHex(digest.digest())
                            + ", contentType=" + observed(event.getContentType())
                            + ", requestContentType=" + requestContentType
                            + ", requestMethod=" + event.getRequest().getMethod()
                            + ", multipart=" + "multipart/form-data".equals(requestContentType));
                } catch (IOException | RuntimeException error) {
                    handlerFailures.incrementAndGet();
                    throw error;
                } finally {
                    ownerUI.access(FlowExternalFileDropRoute.this::updateStatus);
                }
            }

            @Override
            public void responseHandled(UploadResult result) {
                responseCalls.incrementAndGet();
                if (result.success()) {
                    successfulResponses.incrementAndGet();
                } else {
                    failedResponses.incrementAndGet();
                }
                responseSummary.set("success=" + result.success()
                        + ", acceptedFiles=" + result.acceptedFiles().size()
                        + ", acceptedFile=" + (result.acceptedFiles().isEmpty()
                                ? "none" : String.join("|", result.acceptedFiles()))
                        + ", rejectedFiles=" + result.rejectedFiles().size()
                        + ", exception=" + (result.exception() != null));
                UploadHandler.super.responseHandled(result);
                ownerUI.access(FlowExternalFileDropRoute.this::updateStatus);
            }

            @Override
            public long getFileSizeMax() {
                return MAX_FILE_SIZE;
            }

            @Override
            public long getRequestSizeMax() {
                return MAX_FILE_SIZE + 64 * 1024;
            }

            @Override
            public long getFileCountMax() {
                return 1;
            }
        };

        Upload upload = new Upload(handler);
        upload.setId(PREFIX + "upload");
        upload.setUploadFormat(UploadFormat.MULTIPART);
        upload.setAutoUpload(true);
        upload.setDropAllowed(true);
        upload.setMaxFiles(1);
        upload.setMaxFileSize(MAX_FILE_SIZE);
        upload.setWidth("36em");
        upload.setMinHeight("14em");
        upload.setDropLabel(new Span("Drop a file from Finder here"));
        upload.addAllFinishedListener(event -> {
            allFinished++;
            // Upstream AllFinishedEvent is server-originated even when the
            // upload itself began with a trusted browser drop.
            allFinishedClient = event.isFromClient();
            updateStatus();
        });
        upload.addFileRejectedListener(event -> {
            rejected++;
            updateStatus();
        });
        upload.addFileRemovedListener(event -> {
            removed++;
            updateStatus();
        });

        Span configStatus = new Span("External file drop config: format="
                + UploadFormat.MULTIPART
                + ", autoUpload=" + upload.isAutoUpload()
                + ", dropAllowed=" + upload.isDropAllowed()
                + ", maxFiles=" + upload.getMaxFiles()
                + ", maxFileSize=" + upload.getMaxFileSize());
        configStatus.setId(PREFIX + "config-status");
        uploadStatus.setId(PREFIX + "upload-status");
        responseStatus.setId(PREFIX + "response-status");
        listenerStatus.setId(PREFIX + "listener-status");
        updateStatus();
        add(new H1("Flow external file drop"), upload, configStatus,
                uploadStatus, responseStatus, listenerStatus);
    }

    private void updateStatus() {
        String received = receivedSummary.get();
        if (received != null || handlerFailures.get() > 0) {
            uploadStatus.setText("External file drop upload: handlerCalls="
                    + handlerCalls.get()
                    + ", handlerFailures=" + handlerFailures.get()
                    + (received == null ? ", failed=true" : ", " + received));
        }
        String response = responseSummary.get();
        if (response != null) {
            responseStatus.setText("External file drop response: responseCalls="
                    + responseCalls.get()
                    + ", successfulResponses=" + successfulResponses.get()
                    + ", failedResponses=" + failedResponses.get()
                    + ", " + response);
        }
        listenerStatus.setText("External file drop listeners: allFinished="
                + allFinished + ", allFinishedClient=" + allFinishedClient
                + ", rejected=" + rejected + ", removed=" + removed);
    }

    private static MessageDigest sha256() throws IOException {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException error) {
            throw new IOException("SHA-256 is unavailable", error);
        }
    }

    private static String observed(String value) {
        return value == null ? "none" : value;
    }

    private static String mediaType(String value) {
        return value == null ? "none"
                : value.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    }
}
