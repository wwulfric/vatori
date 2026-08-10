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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Route("flow-in-flight-request")
public final class FlowInFlightRequestRoute extends VerticalLayout {
    private static final int CONTENT_LENGTH = 4 * 1024 * 1024 + 509;
    private static final int BUFFER_SIZE = 32 * 1024;
    private static final long READ_DELAY_MILLIS = 8;

    public FlowInFlightRequestRoute() {
        setId("framework-flow-in-flight-request-route");
        setPadding(true);
        setSpacing(true);

        AtomicInteger requests = new AtomicInteger();
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger invalid = new AtomicInteger();
        AtomicLong successfulBytes = new AtomicLong(-1);
        AtomicLong declaredBytes = new AtomicLong(-1);
        AtomicReference<String> fileName = new AtomicReference<>("");
        AtomicReference<String> contentType = new AtomicReference<>("");

        UploadHandler handler = new UploadHandler() {
            @Override
            public void handleUploadRequest(UploadEvent event)
                    throws IOException {
                requests.incrementAndGet();
                declaredBytes.set(event.getFileSize());
                fileName.set(event.getFileName());
                contentType.set(event.getContentType());

                long received = 0;
                boolean patternValid = true;
                try (InputStream input = event.getInputStream()) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int read;
                    while ((read = input.read(buffer)) >= 0) {
                        for (int index = 0; index < read; index++) {
                            int expected = (int) (((received + index) * 29 + 7)
                                    & 0xff);
                            if ((buffer[index] & 0xff) != expected) {
                                patternValid = false;
                            }
                        }
                        received += read;
                        try {
                            Thread.sleep(READ_DELAY_MILLIS);
                        } catch (InterruptedException error) {
                            Thread.currentThread().interrupt();
                            throw new IOException(
                                    "Interrupted while consuming request", error);
                        }
                    }
                }

                if (received != CONTENT_LENGTH || !patternValid) {
                    invalid.incrementAndGet();
                    throw new IOException(
                            "In-flight request content validation failed");
                }
                successfulBytes.set(received);
                successes.incrementAndGet();
            }
        };

        Upload upload = new Upload(handler);
        upload.setId("framework-flow-in-flight-request-upload");
        upload.setUploadFormat(UploadFormat.RAW);
        upload.setAutoUpload(false);
        upload.setMaxFiles(1);
        upload.setMaxFileSize(CONTENT_LENGTH + 1024);
        upload.setAcceptedFileTypes("application/octet-stream", ".bin");

        Span status = new Span("In-flight request: pending");
        status.setId("framework-flow-in-flight-request-status");
        AtomicInteger confirmationClicks = new AtomicInteger();
        Button confirm = new Button("Confirm recovered request", event ->
                status.setText("In-flight request: confirmed"
                        + " clicks=" + confirmationClicks.incrementAndGet()
                        + " requests=" + requests.get()
                        + " successes=" + successes.get()
                        + " successBytes=" + successfulBytes.get()
                        + " invalid=" + invalid.get()
                        + " file=" + fileName.get()
                        + " type=" + contentType.get()
                        + " declared=" + declaredBytes.get()
                        + " total=" + CONTENT_LENGTH));
        confirm.setId("framework-flow-in-flight-request-confirm");

        Button returnToFramework = new Button("Return to framework", event ->
                event.getSource().getUI().ifPresent(
                        ui -> ui.navigate(FlowOverIpcFrameworkView.class)));
        returnToFramework.setId("framework-flow-in-flight-request-return");

        add(
                new H1("Flow in-flight request recovery acceptance"),
                upload,
                status,
                confirm,
                returnToFramework);
    }
}
