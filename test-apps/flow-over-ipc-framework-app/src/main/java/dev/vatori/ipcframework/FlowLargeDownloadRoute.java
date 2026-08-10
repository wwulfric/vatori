package dev.vatori.ipcframework;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadEvent;
import com.vaadin.flow.server.streams.DownloadHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Route("flow-large-download")
public final class FlowLargeDownloadRoute extends VerticalLayout {
    private static final int CONTENT_LENGTH = 4 * 1024 * 1024 + 113;
    private static final int BUFFER_SIZE = 32 * 1024;

    public FlowLargeDownloadRoute() {
        setId("framework-flow-large-download-route");
        setPadding(true);
        setSpacing(true);

        AtomicInteger requests = new AtomicInteger();
        AtomicInteger rangeRequests = new AtomicInteger();
        AtomicInteger cancelledRequests = new AtomicInteger();
        AtomicLong lastRangeStart = new AtomicLong(-1);

        DownloadHandler handler = new DownloadHandler() {
            @Override
            public void handleDownloadRequest(DownloadEvent event)
                    throws IOException {
                requests.incrementAndGet();
                String rangeHeader = event.getRequest().getHeader("Range");
                long rangeStart = rangeStart(rangeHeader);
                if (rangeStart < 0 || rangeStart >= CONTENT_LENGTH) {
                    event.getResponse().setStatus(416);
                    event.getResponse().setHeader(
                            "Content-Range", "bytes */" + CONTENT_LENGTH);
                    return;
                }
                if (rangeHeader != null && !rangeHeader.isBlank()) {
                    rangeRequests.incrementAndGet();
                    lastRangeStart.set(rangeStart);
                    event.getResponse().setStatus(206);
                    event.getResponse().setHeader(
                            "Content-Range",
                            "bytes " + rangeStart + "-"
                                    + (CONTENT_LENGTH - 1) + "/"
                                    + CONTENT_LENGTH);
                }
                event.getResponse().setHeader("Accept-Ranges", "bytes");
                event.getResponse().setHeader(
                        "X-Vatori-Range-Start", String.valueOf(rangeStart));
                event.setFileName("flow-ipc-large-download.bin");
                event.setContentType("application/octet-stream");
                event.setContentLength(CONTENT_LENGTH - rangeStart);
                try (OutputStream output = event.getOutputStream()) {
                    writeContent(output, rangeStart);
                } catch (IOException error) {
                    cancelledRequests.incrementAndGet();
                    throw error;
                }
            }

            @Override
            public String getUrlPostfix() {
                return "flow-ipc-large-download.bin";
            }
        };

        Anchor download = new Anchor(handler, "Large resumable download");
        download.setId("framework-flow-large-download-link");
        download.getElement().setAttribute("download", true);

        Span status = new Span("Large download: pending");
        status.setId("framework-flow-large-download-status");
        Button confirm = new Button("Confirm transfer", event ->
                status.setText("Large download: confirmed"
                        + " requests=" + requests.get()
                        + " ranges=" + rangeRequests.get()
                        + " cancelled=" + cancelledRequests.get()
                        + " rangeStart=" + lastRangeStart.get()
                        + " total=" + CONTENT_LENGTH));
        confirm.setId("framework-flow-large-download-confirm");

        Button returnToFramework = new Button("Return to framework", event ->
                event.getSource().getUI().ifPresent(
                        ui -> ui.navigate(FlowOverIpcFrameworkView.class)));
        returnToFramework.setId("framework-flow-large-download-return");

        add(
                new H1("Flow large download acceptance"),
                download,
                status,
                confirm,
                returnToFramework);
    }

    private static long rangeStart(String rangeHeader) {
        if (rangeHeader == null || rangeHeader.isBlank()) {
            return 0;
        }
        if (!rangeHeader.startsWith("bytes=") || !rangeHeader.endsWith("-")) {
            return -1;
        }
        try {
            return Long.parseLong(rangeHeader.substring(
                    "bytes=".length(), rangeHeader.length() - 1));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static void writeContent(OutputStream output, long start)
            throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        long offset = start;
        while (offset < CONTENT_LENGTH) {
            int length = (int) Math.min(buffer.length, CONTENT_LENGTH - offset);
            for (int index = 0; index < length; index++) {
                buffer[index] = (byte) (((offset + index) * 31 + 17) & 0xff);
            }
            output.write(buffer, 0, length);
            offset += length;
        }
    }
}
