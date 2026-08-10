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

@Route("flow-concurrent-multipart-upload")
public final class FlowConcurrentMultipartUploadRoute extends VerticalLayout {
    private static final String CANCELLED_FILE =
            "flow-ipc-concurrent-multipart-cancel.bin";
    private static final String SURVIVOR_FILE =
            "flow-ipc-concurrent-multipart-survivor.bin";
    private static final int CANCELLED_LENGTH = 4 * 1024 * 1024 + 173;
    private static final int SURVIVOR_LENGTH = 3 * 1024 * 1024 + 257;
    private static final int BUFFER_SIZE = 32 * 1024;

    public FlowConcurrentMultipartUploadRoute() {
        setId("framework-flow-concurrent-multipart-upload-route");
        setPadding(true);
        setSpacing(true);

        FlowMultipartAcceptanceStorage.RequestDirectoryMonitor storageMonitor =
                FlowMultipartAcceptanceStorage.monitor();
        addDetachListener(event -> storageMonitor.close());

        AtomicInteger handlerCalls = new AtomicInteger();
        AtomicInteger handlerFailures = new AtomicInteger();
        AtomicInteger responseCalls = new AtomicInteger();
        AtomicInteger failedResponses = new AtomicInteger();
        AtomicInteger successfulResponses = new AtomicInteger();
        AtomicInteger successfulAcceptedFiles = new AtomicInteger(-1);
        AtomicInteger invalidRequests = new AtomicInteger();
        AtomicLong successfulBytes = new AtomicLong(-1);
        AtomicLong successfulDeclaredBytes = new AtomicLong(-1);
        AtomicLong spillBytes = new AtomicLong(-1);
        AtomicBoolean successfulPatternValid = new AtomicBoolean();
        AtomicBoolean diskSpillObserved = new AtomicBoolean();
        AtomicReference<String> handlerFileName = new AtomicReference<>("");
        AtomicReference<String> failedHandlerFileName =
                new AtomicReference<>("");
        AtomicReference<String> successfulAcceptedFile =
                new AtomicReference<>("");
        AtomicReference<String> contentType = new AtomicReference<>("");

        UploadHandler handler = new UploadHandler() {
            @Override
            public void handleUploadRequest(UploadEvent event)
                    throws IOException {
                handlerCalls.incrementAndGet();
                handlerFileName.set(event.getFileName());
                successfulDeclaredBytes.set(event.getFileSize());
                contentType.set(event.getContentType());

                FlowMultipartAcceptanceStorage.Snapshot storage =
                        FlowMultipartAcceptanceStorage.snapshot();
                spillBytes.set(storage.largestFileBytes());
                diskSpillObserved.set(storage.spilled(SURVIVOR_LENGTH));

                long received = 0;
                boolean patternValid = SURVIVOR_FILE.equals(
                        event.getFileName());
                try (InputStream input = event.getInputStream()) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int read;
                    while ((read = input.read(buffer)) >= 0) {
                        for (int index = 0; index < read; index++) {
                            int expected = expectedSurvivorByte(
                                    received + index);
                            if ((buffer[index] & 0xff) != expected) {
                                patternValid = false;
                            }
                        }
                        received += read;
                    }
                } catch (IOException error) {
                    handlerFailures.incrementAndGet();
                    failedHandlerFileName.set(event.getFileName());
                    throw error;
                }

                if (received != SURVIVOR_LENGTH || !patternValid) {
                    invalidRequests.incrementAndGet();
                    throw new IOException(
                            "Concurrent multipart survivor validation failed");
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
                }
                UploadHandler.super.responseHandled(result);
            }
        };

        Upload upload = new Upload(handler);
        upload.setId("framework-flow-concurrent-multipart-upload");
        upload.setUploadFormat(UploadFormat.MULTIPART);
        upload.setAutoUpload(false);
        upload.setMaxFiles(2);
        upload.setMaxFileSize(CANCELLED_LENGTH + 1024);
        upload.setAcceptedFileTypes("application/octet-stream", ".bin");

        AtomicInteger fileRemovedEvents = new AtomicInteger();
        AtomicInteger allFinishedEvents = new AtomicInteger();
        AtomicReference<String> removedFileName = new AtomicReference<>("");
        Span listenerStatus = new Span(
                "Concurrent multipart upload listeners: pending");
        listenerStatus.setId(
                "framework-flow-concurrent-multipart-upload-listener-status");
        Runnable updateListenerStatus = () -> listenerStatus.setText(
                "Concurrent multipart upload listeners:"
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

        Span status = new Span("Concurrent multipart upload: pending");
        status.setId("framework-flow-concurrent-multipart-upload-status");
        AtomicInteger confirmationClicks = new AtomicInteger();
        Button confirm = new Button(
                "Confirm concurrent multipart upload", event -> {
                    FlowMultipartAcceptanceStorage.MonitorSnapshot monitor =
                            storageMonitor.snapshot();
                    storageMonitor.close();
                    boolean temporaryPartsCleaned =
                            FlowMultipartAcceptanceStorage.isEmpty();
                    status.setText("Concurrent multipart upload: confirmed"
                            + " clicks=" + confirmationClicks.incrementAndGet()
                            + " handlerCalls=" + handlerCalls.get()
                            + " handlerFailures=" + handlerFailures.get()
                            + " handlerFile=" + handlerFileName.get()
                            + " failedHandlerFile="
                            + failedHandlerFileName.get()
                            + " responseCalls=" + responseCalls.get()
                            + " failedResponses=" + failedResponses.get()
                            + " successfulResponses="
                            + successfulResponses.get()
                            + " successfulAccepted="
                            + successfulAcceptedFiles.get()
                            + " successfulAcceptedFile="
                            + successfulAcceptedFile.get()
                            + " successBytes=" + successfulBytes.get()
                            + " successDeclared="
                            + successfulDeclaredBytes.get()
                            + " successPattern="
                            + successfulPatternValid.get()
                            + " type=" + contentType.get()
                            + " diskSpill=" + diskSpillObserved.get()
                            + " spillBytes=" + spillBytes.get()
                            + " maxTempDirs="
                            + monitor.maximumDirectories()
                            + " maxTempFiles=" + monitor.maximumFiles()
                            + " monitorError=" + monitor.failure()
                            + " tempCleanup=" + temporaryPartsCleaned
                            + " invalid=" + invalidRequests.get()
                            + " removed=" + fileRemovedEvents.get()
                            + " removedFile=" + removedFileName.get()
                            + " allFinished=" + allFinishedEvents.get()
                            + " cancelledTotal=" + CANCELLED_LENGTH
                            + " survivorTotal=" + SURVIVOR_LENGTH);
                });
        confirm.setId(
                "framework-flow-concurrent-multipart-upload-confirm");

        Button returnToFramework = new Button(
                "Return to framework", event ->
                        event.getSource().getUI().ifPresent(
                                ui -> ui.navigate(
                                        FlowOverIpcFrameworkView.class)));
        returnToFramework.setId(
                "framework-flow-concurrent-multipart-upload-return");

        add(
                new H1("Flow concurrent multipart upload acceptance"),
                upload,
                listenerStatus,
                status,
                confirm,
                returnToFramework);
    }

    private static int expectedSurvivorByte(long offset) {
        return (int) ((offset * 37 + 11) & 0xff);
    }
}
