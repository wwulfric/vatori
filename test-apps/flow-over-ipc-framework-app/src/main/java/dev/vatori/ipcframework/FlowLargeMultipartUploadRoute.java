package dev.vatori.ipcframework;

import com.vaadin.flow.component.button.Button;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Route("flow-large-multipart-upload")
public final class FlowLargeMultipartUploadRoute extends VerticalLayout {
    private static final String FILE_NAME =
            "flow-ipc-large-multipart-upload.bin";
    private static final int CONTENT_LENGTH = 4 * 1024 * 1024 + 307;
    private static final int BUFFER_SIZE = 32 * 1024;

    public FlowLargeMultipartUploadRoute() {
        setId("framework-flow-large-multipart-upload-route");
        setPadding(true);
        setSpacing(true);

        AtomicInteger handlerCalls = new AtomicInteger();
        AtomicInteger handlerFailures = new AtomicInteger();
        AtomicInteger responseCalls = new AtomicInteger();
        AtomicInteger failedResponses = new AtomicInteger();
        AtomicInteger successfulResponses = new AtomicInteger();
        AtomicInteger failedAcceptedFiles = new AtomicInteger(-1);
        AtomicInteger successfulAcceptedFiles = new AtomicInteger(-1);
        AtomicInteger invalidRequests = new AtomicInteger();
        AtomicLong successfulBytes = new AtomicLong(-1);
        AtomicLong declaredBytes = new AtomicLong(-1);
        AtomicBoolean successfulPatternValid = new AtomicBoolean();
        AtomicBoolean failedResponseHasException = new AtomicBoolean();
        AtomicBoolean diskSpillObserved = new AtomicBoolean();
        AtomicLong spillBytes = new AtomicLong(-1);
        AtomicReference<String> failureType = new AtomicReference<>("");
        AtomicReference<String> successfulFileName =
                new AtomicReference<>("");
        AtomicReference<String> successfulAcceptedFile =
                new AtomicReference<>("");
        AtomicReference<String> contentType = new AtomicReference<>("");

        UploadHandler handler = new UploadHandler() {
            @Override
            public void handleUploadRequest(UploadEvent event)
                    throws IOException {
                handlerCalls.incrementAndGet();
                declaredBytes.set(event.getFileSize());
                successfulFileName.set(event.getFileName());
                contentType.set(event.getContentType());
                FlowMultipartAcceptanceStorage.Snapshot storage =
                        FlowMultipartAcceptanceStorage.snapshot();
                spillBytes.set(storage.largestFileBytes());
                diskSpillObserved.set(storage.spilled(CONTENT_LENGTH));

                long received = 0;
                boolean patternValid = true;
                try (InputStream input = event.getInputStream()) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int read;
                    while ((read = input.read(buffer)) >= 0) {
                        for (int index = 0; index < read; index++) {
                            int expected = expectedByte(received + index);
                            if ((buffer[index] & 0xff) != expected) {
                                patternValid = false;
                            }
                        }
                        received += read;
                    }
                } catch (IOException error) {
                    handlerFailures.incrementAndGet();
                    throw error;
                }

                if (received != CONTENT_LENGTH || !patternValid) {
                    invalidRequests.incrementAndGet();
                    throw new IOException(
                            "Large multipart upload content validation failed");
                }
                successfulBytes.set(received);
                successfulPatternValid.set(true);
            }

            @Override
            public void responseHandled(UploadResult result) {
                responseCalls.incrementAndGet();
                if (result.success()) {
                    successfulResponses.incrementAndGet();
                    successfulAcceptedFiles.set(result.acceptedFiles().size());
                    successfulAcceptedFile.set(
                            result.acceptedFiles().isEmpty()
                                    ? ""
                                    : result.acceptedFiles().getFirst());
                } else {
                    failedResponses.incrementAndGet();
                    failedAcceptedFiles.set(result.acceptedFiles().size());
                    failedResponseHasException.set(result.exception() != null);
                    failureType.set(result.exception() == null
                            ? ""
                            : result.exception().getClass().getSimpleName());
                }
                UploadHandler.super.responseHandled(result);
            }
        };

        Upload upload = new Upload(handler);
        upload.setId("framework-flow-large-multipart-upload");
        upload.setUploadFormat(UploadFormat.MULTIPART);
        upload.setAutoUpload(false);
        upload.setMaxFiles(1);
        upload.setMaxFileSize(CONTENT_LENGTH + 1024);
        upload.setAcceptedFileTypes("application/octet-stream", ".bin");

        AtomicInteger fileRemovedEvents = new AtomicInteger();
        AtomicInteger allFinishedEvents = new AtomicInteger();
        AtomicReference<String> removedFileName = new AtomicReference<>("");
        Span listenerStatus = new Span(
                "Large multipart upload listeners: pending");
        listenerStatus.setId(
                "framework-flow-large-multipart-upload-listener-status");
        Runnable updateListenerStatus = () -> listenerStatus.setText(
                "Large multipart upload listeners:"
                        + " removed=" + fileRemovedEvents.get()
                        + " removedFile=" + removedFileName.get()
                        + " allFinished=" + allFinishedEvents.get());
        upload.addFileRemovedListener(event -> {
            fileRemovedEvents.incrementAndGet();
            removedFileName.set(event.getFileName());
            updateListenerStatus.run();
        });
        upload.addAllFinishedListener(event -> {
            allFinishedEvents.incrementAndGet();
            updateListenerStatus.run();
        });

        Span status = new Span("Large multipart upload: pending");
        status.setId("framework-flow-large-multipart-upload-status");
        AtomicInteger confirmationClicks = new AtomicInteger();
        Button confirm = new Button("Confirm multipart upload", event -> {
            boolean temporaryPartsCleaned =
                    FlowMultipartAcceptanceStorage.isEmpty();
            status.setText("Large multipart upload: confirmed"
                        + " clicks=" + confirmationClicks.incrementAndGet()
                        + " handlerCalls=" + handlerCalls.get()
                        + " handlerFailures=" + handlerFailures.get()
                        + " responseCalls=" + responseCalls.get()
                        + " failedResponses=" + failedResponses.get()
                        + " failedHasException="
                        + failedResponseHasException.get()
                        + " failureType=" + failureType.get()
                        + " failedAccepted=" + failedAcceptedFiles.get()
                        + " successfulResponses=" + successfulResponses.get()
                        + " successfulAccepted="
                        + successfulAcceptedFiles.get()
                        + " successfulAcceptedFile="
                        + successfulAcceptedFile.get()
                        + " successBytes=" + successfulBytes.get()
                        + " declared=" + declaredBytes.get()
                        + " successPattern="
                        + successfulPatternValid.get()
                        + " successFile=" + successfulFileName.get()
                        + " type=" + contentType.get()
                        + " diskSpill=" + diskSpillObserved.get()
                        + " spillBytes=" + spillBytes.get()
                        + " tempCleanup=" + temporaryPartsCleaned
                        + " invalid=" + invalidRequests.get()
                        + " removed=" + fileRemovedEvents.get()
                        + " removedFile=" + removedFileName.get()
                        + " allFinished=" + allFinishedEvents.get()
                        + " total=" + CONTENT_LENGTH);
        });
        confirm.setId("framework-flow-large-multipart-upload-confirm");

        Button returnToFramework = new Button(
                "Return to framework", event ->
                        event.getSource().getUI().ifPresent(
                                ui -> ui.navigate(
                                        FlowOverIpcFrameworkView.class)));
        returnToFramework.setId(
                "framework-flow-large-multipart-upload-return");

        add(
                new H1("Flow large multipart upload acceptance"),
                upload,
                listenerStatus,
                status,
                confirm,
                returnToFramework);
    }

    private static int expectedByte(long offset) {
        return (int) ((offset * 43 + 23) & 0xff);
    }
}
