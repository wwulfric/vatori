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

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Route("flow-concurrent-upload")
public final class FlowConcurrentUploadRoute extends VerticalLayout {
    private static final String CANCELLED_FILE =
            "flow-ipc-concurrent-cancel.bin";
    private static final String SURVIVOR_FILE =
            "flow-ipc-concurrent-survivor.bin";
    private static final int CANCELLED_LENGTH = 4 * 1024 * 1024 + 101;
    private static final int SURVIVOR_LENGTH = 3 * 1024 * 1024 + 149;
    private static final int BUFFER_SIZE = 32 * 1024;

    public FlowConcurrentUploadRoute() {
        setId("framework-flow-concurrent-upload-route");
        setPadding(true);
        setSpacing(true);

        AtomicInteger requests = new AtomicInteger();
        AtomicInteger activeRequests = new AtomicInteger();
        AtomicInteger maximumActiveRequests = new AtomicInteger();
        AtomicInteger cancelledRequests = new AtomicInteger();
        AtomicInteger successfulRequests = new AtomicInteger();
        AtomicInteger invalidRequests = new AtomicInteger();
        AtomicLong cancelledBytes = new AtomicLong(-1);
        AtomicLong successfulBytes = new AtomicLong(-1);
        AtomicLong cancelledDeclaredBytes = new AtomicLong(-1);
        AtomicLong successfulDeclaredBytes = new AtomicLong(-1);
        AtomicBoolean cancelledPatternValid = new AtomicBoolean();
        AtomicBoolean successfulPatternValid = new AtomicBoolean();
        AtomicReference<String> cancelledFileName = new AtomicReference<>("");
        AtomicReference<String> successfulFileName = new AtomicReference<>("");

        UploadHandler handler = new UploadHandler() {
            @Override
            public void handleUploadRequest(UploadEvent event)
                    throws IOException {
                requests.incrementAndGet();
                int active = activeRequests.incrementAndGet();
                maximumActiveRequests.accumulateAndGet(active, Math::max);

                String fileName = event.getFileName();
                FileSpec spec = fileSpec(fileName);
                long received = 0;
                boolean patternValid = spec != null;
                try (InputStream input = event.getInputStream()) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int read;
                    while ((read = input.read(buffer)) >= 0) {
                        if (spec != null) {
                            for (int index = 0; index < read; index++) {
                                int expected = spec.expectedByte(received + index);
                                if ((buffer[index] & 0xff) != expected) {
                                    patternValid = false;
                                }
                            }
                        }
                        received += read;
                    }
                } catch (IOException error) {
                    cancelledRequests.incrementAndGet();
                    cancelledBytes.set(received);
                    cancelledDeclaredBytes.set(event.getFileSize());
                    cancelledPatternValid.set(patternValid);
                    cancelledFileName.set(fileName);
                    throw error;
                } finally {
                    activeRequests.decrementAndGet();
                }

                if (spec == null || received != spec.length()
                        || !patternValid) {
                    invalidRequests.incrementAndGet();
                    throw new IOException(
                            "Concurrent upload content validation failed");
                }
                successfulRequests.incrementAndGet();
                successfulBytes.set(received);
                successfulDeclaredBytes.set(event.getFileSize());
                successfulPatternValid.set(true);
                successfulFileName.set(fileName);
            }
        };

        Upload upload = new Upload(handler);
        upload.setId("framework-flow-concurrent-upload");
        upload.setUploadFormat(UploadFormat.RAW);
        upload.setAutoUpload(false);
        upload.setMaxFiles(2);
        upload.setMaxFileSize(CANCELLED_LENGTH + 1024);
        upload.setAcceptedFileTypes("application/octet-stream", ".bin");

        AtomicInteger fileRemovedEvents = new AtomicInteger();
        AtomicInteger allFinishedEvents = new AtomicInteger();
        AtomicReference<String> removedFileName = new AtomicReference<>("");
        Span listenerStatus = new Span(
                "Concurrent upload listeners: pending");
        listenerStatus.setId(
                "framework-flow-concurrent-upload-listener-status");
        Runnable updateListenerStatus = () -> listenerStatus.setText(
                "Concurrent upload listeners:"
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

        Span status = new Span("Concurrent upload: pending");
        status.setId("framework-flow-concurrent-upload-status");
        AtomicInteger confirmationClicks = new AtomicInteger();
        Button confirm = new Button("Confirm concurrent upload", event ->
                status.setText("Concurrent upload: confirmed"
                        + " clicks=" + confirmationClicks.incrementAndGet()
                        + " requests=" + requests.get()
                        + " active=" + activeRequests.get()
                        + " maxActive=" + maximumActiveRequests.get()
                        + " cancelled=" + cancelledRequests.get()
                        + " cancelledBytes=" + cancelledBytes.get()
                        + " cancelledDeclared="
                        + cancelledDeclaredBytes.get()
                        + " cancelledPattern="
                        + cancelledPatternValid.get()
                        + " cancelledFile=" + cancelledFileName.get()
                        + " successes=" + successfulRequests.get()
                        + " successBytes=" + successfulBytes.get()
                        + " successDeclared="
                        + successfulDeclaredBytes.get()
                        + " successPattern="
                        + successfulPatternValid.get()
                        + " successFile=" + successfulFileName.get()
                        + " invalid=" + invalidRequests.get()
                        + " cancelledTotal=" + CANCELLED_LENGTH
                        + " survivorTotal=" + SURVIVOR_LENGTH));
        confirm.setId("framework-flow-concurrent-upload-confirm");

        Button returnToFramework = new Button(
                "Return to framework", event ->
                        event.getSource().getUI().ifPresent(
                                ui -> ui.navigate(
                                        FlowOverIpcFrameworkView.class)));
        returnToFramework.setId("framework-flow-concurrent-upload-return");

        add(
                new H1("Flow concurrent upload acceptance"),
                upload,
                listenerStatus,
                status,
                confirm,
                returnToFramework);
    }

    private static FileSpec fileSpec(String fileName) {
        if (CANCELLED_FILE.equals(fileName)) {
            return new FileSpec(CANCELLED_LENGTH, 17, 3);
        }
        if (SURVIVOR_FILE.equals(fileName)) {
            return new FileSpec(SURVIVOR_LENGTH, 29, 7);
        }
        return null;
    }

    private record FileSpec(int length, int multiplier, int increment) {
        private int expectedByte(long offset) {
            return (int) ((offset * multiplier + increment) & 0xff);
        }
    }
}
