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

@Route("flow-large-upload")
public final class FlowLargeUploadRoute extends VerticalLayout {
    private static final int CONTENT_LENGTH = 4 * 1024 * 1024 + 211;
    private static final int BUFFER_SIZE = 32 * 1024;

    public FlowLargeUploadRoute() {
        setId("framework-flow-large-upload-route");
        setPadding(true);
        setSpacing(true);

        AtomicInteger requests = new AtomicInteger();
        AtomicInteger cancelledRequests = new AtomicInteger();
        AtomicInteger successfulRequests = new AtomicInteger();
        AtomicInteger invalidRequests = new AtomicInteger();
        AtomicInteger fileRemovedEvents = new AtomicInteger();
        AtomicLong cancelledBytes = new AtomicLong(-1);
        AtomicLong successfulBytes = new AtomicLong(-1);
        AtomicLong declaredBytes = new AtomicLong(-1);
        AtomicBoolean cancelledPatternValid = new AtomicBoolean();
        AtomicBoolean successfulPatternValid = new AtomicBoolean();
        AtomicReference<String> fileName = new AtomicReference<>("");
        AtomicReference<String> contentType = new AtomicReference<>("");
        AtomicReference<String> removedFileName = new AtomicReference<>("");

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
                            int expected = (int) (((received + index) * 31 + 17)
                                    & 0xff);
                            if ((buffer[index] & 0xff) != expected) {
                                patternValid = false;
                            }
                        }
                        received += read;
                    }
                } catch (IOException error) {
                    cancelledRequests.incrementAndGet();
                    cancelledBytes.set(received);
                    cancelledPatternValid.set(patternValid);
                    throw error;
                }

                if (received != CONTENT_LENGTH || !patternValid) {
                    invalidRequests.incrementAndGet();
                    throw new IOException("Large upload content validation failed");
                }
                successfulRequests.incrementAndGet();
                successfulBytes.set(received);
                successfulPatternValid.set(true);
            }
        };

        Upload upload = new Upload(handler);
        upload.setId("framework-flow-large-upload");
        upload.setUploadFormat(UploadFormat.RAW);
        upload.setAutoUpload(false);
        upload.setMaxFiles(1);
        upload.setMaxFileSize(CONTENT_LENGTH + 1024);
        upload.setAcceptedFileTypes("application/octet-stream", ".bin");

        Span listenerStatus = new Span("Large upload listener: pending");
        listenerStatus.setId("framework-flow-large-upload-listener-status");
        upload.addFileRemovedListener(event -> {
            int count = fileRemovedEvents.incrementAndGet();
            removedFileName.set(event.getFileName());
            listenerStatus.setText("Large upload listener: removed=" + count
                    + " file=" + event.getFileName());
        });

        Span status = new Span("Large upload: pending");
        status.setId("framework-flow-large-upload-status");
        AtomicInteger confirmationClicks = new AtomicInteger();
        Button confirm = new Button("Confirm upload", event ->
                status.setText("Large upload: confirmed"
                        + " clicks=" + confirmationClicks.incrementAndGet()
                        + " requests=" + requests.get()
                        + " cancelled=" + cancelledRequests.get()
                        + " cancelledBytes=" + cancelledBytes.get()
                        + " cancelPattern=" + cancelledPatternValid.get()
                        + " successes=" + successfulRequests.get()
                        + " successBytes=" + successfulBytes.get()
                        + " successPattern=" + successfulPatternValid.get()
                        + " invalid=" + invalidRequests.get()
                        + " removed=" + fileRemovedEvents.get()
                        + " removedFile=" + removedFileName.get()
                        + " file=" + fileName.get()
                        + " type=" + contentType.get()
                        + " declared=" + declaredBytes.get()
                        + " total=" + CONTENT_LENGTH));
        confirm.setId("framework-flow-large-upload-confirm");

        Button returnToFramework = new Button("Return to framework", event ->
                event.getSource().getUI().ifPresent(
                        ui -> ui.navigate(FlowOverIpcFrameworkView.class)));
        returnToFramework.setId("framework-flow-large-upload-return");

        add(
                new H1("Flow large upload acceptance"),
                upload,
                listenerStatus,
                status,
                confirm,
                returnToFramework);
    }
}
