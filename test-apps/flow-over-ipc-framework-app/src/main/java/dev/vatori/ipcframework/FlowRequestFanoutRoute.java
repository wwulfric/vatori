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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Route("flow-request-fanout")
public final class FlowRequestFanoutRoute extends VerticalLayout {
    private static final int BUFFER_SIZE = 32 * 1024;
    private static final int FILE_COUNT = 60;
    private static final int LARGE_CANCEL_THRESHOLD = 512 * 1024;
    private static final int CHURN_CANCEL_RECEIVED_MINIMUM = 160 * 1024;
    private static final List<FileSpec> FILES = Stream.concat(
            List.of(
                    initialFile(0, 4 * 1024 * 1024 + 37, 17, 3),
                    initialFile(1, 4 * 1024 * 1024 + 83, 29, 5),
                    initialFile(2, 4 * 1024 * 1024 + 139, 37, 7),
                    initialFile(3, 4 * 1024 * 1024 + 197, 43, 11),
                    initialFile(4, 4 * 1024 * 1024 + 251, 53, 13),
                    initialFile(5, 4 * 1024 * 1024 + 307, 61, 17),
                    initialFile(6, 2 * 1024 * 1024 + 359, 67, 19),
                    initialFile(7, 2 * 1024 * 1024 + 419, 71, 23),
                    initialFile(8, 2 * 1024 * 1024 + 487, 73, 29),
                    initialFile(9, 2 * 1024 * 1024 + 557, 79, 31),
                    initialFile(10, 2 * 1024 * 1024 + 631, 83, 37),
                    initialFile(11, 2 * 1024 * 1024 + 701, 89, 41))
                    .stream(),
            IntStream.range(12, FILE_COUNT)
                    .mapToObj(FlowRequestFanoutRoute::churnFile))
            .toList();

    public FlowRequestFanoutRoute() {
        setId("framework-flow-request-fanout-route");
        setPadding(true);
        setSpacing(true);

        AtomicInteger requests = new AtomicInteger();
        AtomicInteger activeRequests = new AtomicInteger();
        AtomicInteger maximumActiveRequests = new AtomicInteger();
        AtomicInteger cancelledRequests = new AtomicInteger();
        AtomicInteger successfulRequests = new AtomicInteger();
        AtomicInteger invalidRequests = new AtomicInteger();
        Map<String, TransferResult> results = new ConcurrentHashMap<>();

        UploadHandler handler = new UploadHandler() {
            @Override
            public void handleUploadRequest(UploadEvent event)
                    throws IOException {
                requests.incrementAndGet();
                int active = activeRequests.incrementAndGet();
                maximumActiveRequests.accumulateAndGet(active, Math::max);

                String fileName = event.getFileName();
                FileSpec spec = fileSpec(fileName);
                long declaredBytes = event.getFileSize();
                long received = 0;
                boolean patternValid = spec != null;
                try (InputStream input = event.getInputStream()) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int read;
                    while ((read = input.read(buffer)) >= 0) {
                        if (spec != null) {
                            for (int index = 0; index < read; index++) {
                                if ((buffer[index] & 0xff)
                                        != spec.expectedByte(received + index)) {
                                    patternValid = false;
                                }
                            }
                        }
                        received += read;
                    }
                } catch (IOException error) {
                    boolean validCancellation = spec != null
                            && spec.cancelled()
                            && received >= spec.cancelThreshold()
                            && received < spec.length()
                            && declaredBytes == spec.length()
                            && patternValid;
                    results.put(fileName, new TransferResult(
                            received, declaredBytes, patternValid,
                            true, validCancellation));
                    if (validCancellation) {
                        cancelledRequests.incrementAndGet();
                    } else {
                        invalidRequests.incrementAndGet();
                    }
                    throw error;
                } finally {
                    activeRequests.decrementAndGet();
                }

                boolean validSuccess = spec != null
                        && !spec.cancelled()
                        && received == spec.length()
                        && declaredBytes == spec.length()
                        && patternValid;
                results.put(fileName, new TransferResult(
                        received, declaredBytes, patternValid,
                        false, validSuccess));
                if (!validSuccess) {
                    invalidRequests.incrementAndGet();
                    throw new IOException(
                            "Request fan-out content validation failed");
                }
                successfulRequests.incrementAndGet();
            }
        };

        Upload upload = new Upload(handler);
        upload.setId("framework-flow-request-fanout");
        upload.setUploadFormat(UploadFormat.RAW);
        upload.setAutoUpload(false);
        upload.setMaxFiles(FILES.size());
        upload.setMaxFileSize(maximumFileLength() + 1024);
        upload.setAcceptedFileTypes("application/octet-stream", ".bin");

        AtomicInteger fileRemovedEvents = new AtomicInteger();
        AtomicInteger allFinishedEvents = new AtomicInteger();
        Set<String> removedFileNames = ConcurrentHashMap.newKeySet();
        Span listenerStatus = new Span("Request fan-out listeners: pending");
        listenerStatus.setId(
                "framework-flow-request-fanout-listener-status");
        Runnable updateListenerStatus = () -> listenerStatus.setText(
                "Request fan-out listeners:"
                        + " removed=" + fileRemovedEvents.get()
                        + " removedFiles=" + sortedCsv(removedFileNames)
                        + " allFinished=" + allFinishedEvents.get());
        upload.addFileRemovedListener(event -> {
            fileRemovedEvents.incrementAndGet();
            removedFileNames.add(event.getFileName());
            updateListenerStatus.run();
        });
        upload.addAllFinishedListener(event -> {
            allFinishedEvents.incrementAndGet();
            updateListenerStatus.run();
        });

        Span status = new Span("Request fan-out: pending");
        status.setId("framework-flow-request-fanout-status");
        AtomicInteger confirmationClicks = new AtomicInteger();
        Button confirm = new Button("Confirm request fan-out", event -> {
            List<TransferResult> cancelledResults = FILES.stream()
                    .filter(FileSpec::cancelled)
                    .map(spec -> results.get(spec.name()))
                    .toList();
            List<TransferResult> survivorResults = FILES.stream()
                    .filter(spec -> !spec.cancelled())
                    .map(spec -> results.get(spec.name()))
                    .toList();
            boolean cancellationsValid = cancelledResults.stream()
                    .allMatch(result -> result != null
                            && result.cancelled() && result.valid());
            boolean survivorsValid = survivorResults.stream()
                    .allMatch(result -> result != null
                            && !result.cancelled() && result.valid());
            long cancelledMinimum = cancelledResults.stream()
                    .filter(result -> result != null)
                    .mapToLong(TransferResult::receivedBytes)
                    .min().orElse(-1);
            long cancelledMaximum = cancelledResults.stream()
                    .filter(result -> result != null)
                    .mapToLong(TransferResult::receivedBytes)
                    .max().orElse(-1);
            long successBytes = survivorResults.stream()
                    .filter(result -> result != null)
                    .mapToLong(TransferResult::receivedBytes)
                    .sum();
            long expectedSuccessBytes = FILES.stream()
                    .filter(spec -> !spec.cancelled())
                    .mapToLong(FileSpec::length)
                    .sum();
            boolean declarationsValid = FILES.stream().allMatch(spec -> {
                TransferResult result = results.get(spec.name());
                return result != null
                        && result.declaredBytes() == spec.length();
            });
            boolean patternsValid = results.size() == FILES.size()
                    && results.values().stream()
                    .allMatch(TransferResult::patternValid);
            String resultMask = FILES.stream().map(spec -> {
                TransferResult result = results.get(spec.name());
                if (result == null || !result.valid()) {
                    return "x";
                }
                return result.cancelled() ? "c" : "s";
            }).reduce("", String::concat);

            status.setText("Request fan-out: confirmed"
                    + " clicks=" + confirmationClicks.incrementAndGet()
                    + " requests=" + requests.get()
                    + " active=" + activeRequests.get()
                    + " maxActive=" + maximumActiveRequests.get()
                    + " cancelled=" + cancelledRequests.get()
                    + " cancellationsValid=" + cancellationsValid
                    + " cancelledMin=" + cancelledMinimum
                    + " cancelledMax=" + cancelledMaximum
                    + " successes=" + successfulRequests.get()
                    + " survivorsValid=" + survivorsValid
                    + " successBytes=" + successBytes
                    + " expectedSuccessBytes=" + expectedSuccessBytes
                    + " declarationsValid=" + declarationsValid
                    + " patternsValid=" + patternsValid
                    + " results=" + results.size()
                    + " resultMask=" + resultMask
                    + " invalid=" + invalidRequests.get());
        });
        confirm.setId("framework-flow-request-fanout-confirm");

        Button returnToFramework = new Button(
                "Return to framework", event ->
                        event.getSource().getUI().ifPresent(
                                ui -> ui.navigate(
                                        FlowOverIpcFrameworkView.class)));
        returnToFramework.setId("framework-flow-request-fanout-return");

        add(new H1("Flow request fan-out acceptance"),
                upload, listenerStatus, status, confirm, returnToFramework);
    }

    private static FileSpec fileSpec(String fileName) {
        return FILES.stream()
                .filter(spec -> spec.name().equals(fileName))
                .findFirst().orElse(null);
    }

    private static FileSpec initialFile(int index, int length,
                                        int multiplier, int increment) {
        boolean cancelled = isCancelledIndex(index);
        return new FileSpec(fileName(index, cancelled), length, multiplier,
                increment, cancelled, LARGE_CANCEL_THRESHOLD);
    }

    private static FileSpec churnFile(int index) {
        boolean cancelled = isCancelledIndex(index);
        return new FileSpec(fileName(index, cancelled),
                256 * 1024 + index * 73 + 17,
                index * 14 + 17, index * 11 + 3, cancelled,
                CHURN_CANCEL_RECEIVED_MINIMUM);
    }

    private static boolean isCancelledIndex(int index) {
        return index % 6 == 1 || index % 6 == 4;
    }

    private static String fileName(int index, boolean cancelled) {
        return "flow-ipc-request-fanout-" + index + "-"
                + (cancelled ? "cancel" : "survivor") + ".bin";
    }

    private static int maximumFileLength() {
        return FILES.stream().mapToInt(FileSpec::length).max().orElseThrow();
    }

    private static String sortedCsv(Set<String> values) {
        return values.stream().sorted().reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private record FileSpec(String name, int length, int multiplier,
                            int increment, boolean cancelled,
                            int cancelThreshold) {
        private int expectedByte(long offset) {
            return (int) ((offset * multiplier + increment) & 0xff);
        }
    }

    private record TransferResult(long receivedBytes, long declaredBytes,
                                  boolean patternValid, boolean cancelled,
                                  boolean valid) {
    }
}
