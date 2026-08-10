package dev.vatori.desktop.testsupport.trace;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class StandardBundleTraceInspectorSmokeMain {
    private StandardBundleTraceInspectorSmokeMain() {
    }

    public static void main(String[] args) throws Exception {
        Path trace = Files.createTempFile("vatori-standard-bundle-trace", ".log");
        try {
            Files.writeString(trace, validTrace(), StandardCharsets.UTF_8);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ByteArrayOutputStream errors = new ByteArrayOutputStream();
            int exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-standard-bundle-runtime",
                    "--require-flow-ipc-standard-bundle-framework-app",
                    "--require-flow-ipc-lazy-grid-query",
                    "--require-flow-ipc-push-delivery",
                    "--require-flow-ipc-push-recovery",
                    "--require-flow-ipc-large-download-resume",
                    "--require-flow-ipc-large-upload-retry",
                    "--require-flow-ipc-large-multipart-upload-retry",
                    "--require-flow-ipc-concurrent-upload",
                    "--require-flow-ipc-flow-dnd",
                    "--require-flow-ipc-flow-input-popup",
                    "--require-flow-ipc-localized-input-popup",
                    "--require-flow-ipc-picker-validation",
                    "--require-flow-ipc-flow-menu-popup",
                    "--require-flow-ipc-flow-overlay-stack",
                    "--require-flow-ipc-structured-rpc",
                    "--require-flow-ipc-standard-bundle-sidecar-recovery",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            String summary = output.toString(StandardCharsets.UTF_8);
            if (exitCode != 0 || !errors.toString(StandardCharsets.UTF_8).isEmpty()
                    || !summary.contains("PASS --require-flow-ipc-standard-bundle-runtime")
                    || !summary.contains("PASS --require-flow-ipc-standard-bundle-framework-app")
                    || !summary.contains("PASS --require-flow-ipc-lazy-grid-query")
                    || !summary.contains("PASS --require-flow-ipc-push-delivery")
                    || !summary.contains("PASS --require-flow-ipc-push-recovery")
                    || !summary.contains(
                    "PASS --require-flow-ipc-large-download-resume")
                    || !summary.contains(
                    "PASS --require-flow-ipc-large-upload-retry")
                    || !summary.contains(
                    "PASS --require-flow-ipc-large-multipart-upload-retry")
                    || !summary.contains(
                    "PASS --require-flow-ipc-concurrent-upload")
                    || !summary.contains("PASS --require-flow-ipc-flow-dnd")
                    || !summary.contains("PASS --require-flow-ipc-flow-input-popup")
                    || !summary.contains(
                    "PASS --require-flow-ipc-localized-input-popup")
                    || !summary.contains(
                    "PASS --require-flow-ipc-picker-validation")
                    || !summary.contains("PASS --require-flow-ipc-flow-menu-popup")
                    || !summary.contains("PASS --require-flow-ipc-flow-overlay-stack")
                    || !summary.contains("PASS --require-flow-ipc-structured-rpc")
                    || !summary.contains("PASS --require-flow-ipc-standard-bundle-sidecar-recovery")) {
                throw new IllegalStateException(
                        "Standard bundle inspector rejected a valid trace:\n" + summary
                                + errors.toString(StandardCharsets.UTF_8));
            }

            Files.writeString(
                    trace,
                    validConcurrentMultipartTrace(),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-concurrent-multipart-upload",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            summary = output.toString(StandardCharsets.UTF_8);
            if (exitCode != 0
                    || !errors.toString(StandardCharsets.UTF_8).isEmpty()
                    || !summary.contains(
                    "PASS --require-flow-ipc-concurrent-multipart-upload")) {
                throw new IllegalStateException(
                        "Concurrent multipart inspector rejected a valid trace:\n"
                                + summary
                                + errors.toString(StandardCharsets.UTF_8));
            }

            Files.writeString(
                    trace,
                    validReactorReloadTrace().replace(
                            "\"customSourceRootObserved\":true",
                            "\"customSourceRootObserved\":false"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-reactor-reload-development",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2
                    || !errors.toString(StandardCharsets.UTF_8).contains(
                    "reactor reload customSourceRootObserved")) {
                throw new IllegalStateException(
                        "Reactor reload inspector accepted a missing "
                                + "custom source-root transition");
            }

            Files.writeString(
                    trace,
                    validConcurrentMultipartTrace().replace(
                            "\"concurrentMultipartUploadSessionSerializationObserved\":true",
                            "\"concurrentMultipartUploadSessionSerializationObserved\":false"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-concurrent-multipart-upload",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains("concurrent multipart upload "
                            + "concurrentMultipartUploadSessionSerializationObserved")) {
                throw new IllegalStateException(
                        "Concurrent multipart inspector accepted missing "
                                + "session serialization");
            }

            Files.writeString(
                    trace,
                    validRequestFanoutTrace(),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-request-fanout",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            summary = output.toString(StandardCharsets.UTF_8);
            if (exitCode != 0
                    || !errors.toString(StandardCharsets.UTF_8).isEmpty()
                    || !summary.contains(
                    "PASS --require-flow-ipc-request-fanout")) {
                throw new IllegalStateException(
                        "Request fan-out inspector rejected a valid trace:\n"
                                + summary
                                + errors.toString(StandardCharsets.UTF_8));
            }

            Files.writeString(
                    trace,
                    validRequestFanoutTrace().replace(
                            "\"tombstoneExpiryElapsedMillis\":61000",
                            "\"tombstoneExpiryElapsedMillis\":59000"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-request-fanout",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains("bounded cancellation tombstone retention and expiry")) {
                throw new IllegalStateException(
                        "Request fan-out inspector accepted an early "
                                + "tombstone expiry");
            }

            Files.writeString(
                    trace,
                    validRequestFanoutTrace().replace(
                            "\"requestId\":\"request-fanout-11\",\"status\":200",
                            "\"requestId\":\"unrelated-request-fanout\","
                                    + "\"status\":200"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-request-fanout",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains("correlated fan-out survivor completion for "
                            + "2097853")) {
                throw new IllegalStateException(
                        "Request fan-out inspector accepted an uncorrelated "
                                + "survivor response");
            }

            Files.writeString(
                    trace,
                    validInFlightRequestRecoveryTrace(),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-in-flight-request-recovery",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            summary = output.toString(StandardCharsets.UTF_8);
            if (exitCode != 0
                    || !errors.toString(StandardCharsets.UTF_8).isEmpty()
                    || !summary.contains(
                    "PASS --require-flow-ipc-in-flight-request-recovery")) {
                throw new IllegalStateException(
                        "In-flight request recovery inspector rejected a valid trace:\n"
                                + summary
                                + errors.toString(StandardCharsets.UTF_8));
            }

            Files.writeString(
                    trace,
                    validInFlightRequestRecoveryTrace().replace(
                            protocol("sidecar.recoveryScheduled", "{\"attempt\":1}"),
                            protocol("send", "opaqueHttpStreamOpen", """
                                    {"requestId":"old-document-request",
                                    "url":"?vatori-inflight-old-document-probe=1",
                                    "method":"GET","contentLength":0,
                                    "headerNames":""}
                                    """)
                                    + protocol("sidecar.recoveryScheduled",
                                    "{\"attempt\":1}")),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-in-flight-request-recovery",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains("no old-document opaque request during recovery")) {
                throw new IllegalStateException(
                        "In-flight request recovery inspector accepted an old "
                                + "document request");
            }

            Files.writeString(
                    trace,
                    validCrossSessionMultipartTrace(),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-cross-session-multipart-upload",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            summary = output.toString(StandardCharsets.UTF_8);
            if (exitCode != 0
                    || !errors.toString(StandardCharsets.UTF_8).isEmpty()
                    || !summary.contains(
                    "PASS --require-flow-ipc-cross-session-multipart-upload")) {
                throw new IllegalStateException(
                        "Cross-session multipart inspector rejected a valid trace:\n"
                                + summary
                                + errors.toString(StandardCharsets.UTF_8));
            }

            Files.writeString(
                    trace,
                    validCrossSessionMultipartTrace().replace(
                            "\"crossSessionMultipartUploadParserConcurrencyObserved\":true",
                            "\"crossSessionMultipartUploadParserConcurrencyObserved\":false"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-cross-session-multipart-upload",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains("cross-session multipart upload "
                            + "crossSessionMultipartUploadParserConcurrencyObserved")) {
                throw new IllegalStateException(
                        "Cross-session multipart inspector accepted missing "
                                + "parser concurrency");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"resync\":true,\"pushSequence\":2",
                            "\"resync\":false,\"pushSequence\":2"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-push-recovery",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains("server resync push envelope")) {
                throw new IllegalStateException(
                        "Push recovery inspector accepted a missing resync response");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"flowServerSyncId\":4,\"lastSeenServerSyncId\":4",
                            "\"flowServerSyncId\":4,\"lastSeenServerSyncId\":3"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-push-delivery",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains("redelivered push Flow last-seen acknowledgement")) {
                throw new IllegalStateException(
                        "Push delivery inspector accepted an unconfirmed redelivery");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"largeDownloadExactReassemblyObserved\":true",
                            "\"largeDownloadExactReassemblyObserved\":false"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-large-download-resume",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains("large download "
                            + "largeDownloadExactReassemblyObserved")) {
                throw new IllegalStateException(
                        "Large download inspector accepted invalid reassembly");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"largeUploadExactBytesObserved\":true",
                            "\"largeUploadExactBytesObserved\":false"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-large-upload-retry",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains("large upload largeUploadExactBytesObserved")) {
                throw new IllegalStateException(
                        "Large upload inspector accepted invalid bytes");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"requestId\":\"large-upload-retry\",\"status\":200",
                            "\"requestId\":\"unrelated-upload\",\"status\":200"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-large-upload-retry",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains("correlated large raw upload completion and response")) {
                throw new IllegalStateException(
                        "Large upload inspector accepted uncorrelated completion");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"largeMultipartUploadExactBytesObserved\":true",
                            "\"largeMultipartUploadExactBytesObserved\":false"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-large-multipart-upload-retry",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains("large multipart upload "
                            + "largeMultipartUploadExactBytesObserved")) {
                throw new IllegalStateException(
                        "Large multipart upload inspector accepted invalid bytes");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"requestId\":\"large-multipart-upload-retry\","
                                    + "\"status\":200",
                            "\"requestId\":\"unrelated-multipart-upload\","
                                    + "\"status\":200"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-large-multipart-upload-retry",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains("one correlated multipart retry completion "
                            + "and response")) {
                throw new IllegalStateException(
                        "Large multipart upload inspector accepted an "
                                + "uncorrelated retry response");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"concurrentUploadRequestIsolationObserved\":true",
                            "\"concurrentUploadRequestIsolationObserved\":false"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-concurrent-upload",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains("concurrent upload "
                            + "concurrentUploadRequestIsolationObserved")) {
                throw new IllegalStateException(
                        "Concurrent upload inspector accepted failed isolation");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"requestId\":\"concurrent-upload-survivor\","
                                    + "\"status\":200",
                            "\"requestId\":\"unrelated-concurrent-upload\","
                                    + "\"status\":200"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-concurrent-upload",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains("correlated concurrent survivor completion "
                            + "and response")) {
                throw new IllegalStateException(
                        "Concurrent upload inspector accepted an uncorrelated "
                                + "survivor response");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"requestId\":\"large-download-resume\","
                                    + "\"method\":\"GET\"",
                            "\"requestId\":\"range-without-response\","
                                    + "\"method\":\"GET\""),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-large-download-resume",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains("correlated Range, HTTP 206 and large response "
                            + "completion")) {
                throw new IllegalStateException(
                        "Large download inspector accepted uncorrelated frames");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"lazyGridFilterResetObserved\":true",
                            "\"lazyGridFilterResetObserved\":false"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-lazy-grid-query",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains("lazy Grid lazyGridFilterResetObserved")) {
                throw new IllegalStateException(
                        "Lazy Grid query inspector accepted an incomplete query cycle");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"flowDndDragEndObserved\":true",
                            "\"flowDndDragEndObserved\":false"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-flow-dnd",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains("Flow DnD flowDndDragEndObserved")) {
                throw new IllegalStateException(
                        "Flow DnD inspector accepted an incomplete drag cycle");
            }

            Files.writeString(
                    trace,
                    validTrace()
                            .replace(
                            "\"flowDndInputMode\":\"appkit-event-queue-pointer\"",
                            "\"flowDndInputMode\":\"scripted-browser-events\"")
                            .replace(
                            "\"flowDndBrowserProgrammaticObserved\":false",
                            "\"flowDndBrowserProgrammaticObserved\":true")
                            .replace(
                            "\"flowDndNativePointerObserved\":true",
                            "\"flowDndNativePointerObserved\":false"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-flow-dnd",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains("Flow DnD native input mode")) {
                throw new IllegalStateException(
                        "Flow DnD inspector accepted scripted browser input");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"flowOverlayStackLayeredFocusRestoredObserved\":true",
                            "\"flowOverlayStackLayeredFocusRestoredObserved\":false"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-flow-overlay-stack",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains(
                            "Flow overlay stack "
                                    + "flowOverlayStackLayeredFocusRestoredObserved")) {
                throw new IllegalStateException(
                        "Flow overlay stack inspector accepted incomplete focus restoration");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"flowInputPopupDateEscapeCloseObserved\":true",
                            "\"flowInputPopupDateEscapeCloseObserved\":false"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-flow-input-popup",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains(
                            "Flow input popup "
                                    + "flowInputPopupDateEscapeCloseObserved")) {
                throw new IllegalStateException(
                        "Flow input popup inspector accepted incomplete Escape close");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"flowInputPopupSelectJavaValueObserved\":true",
                            "\"flowInputPopupSelectJavaValueObserved\":false"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-flow-input-popup",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains(
                            "Flow input popup "
                                    + "flowInputPopupSelectJavaValueObserved")) {
                throw new IllegalStateException(
                        "Flow input popup inspector accepted a missing Select value event");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"flowInputPopupTimeLocaleFormatObserved\":true",
                            "\"flowInputPopupTimeLocaleFormatObserved\":false"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-localized-input-popup",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains(
                            "localized input popup "
                                    + "flowInputPopupTimeLocaleFormatObserved")) {
                throw new IllegalStateException(
                        "Localized input popup inspector accepted a missing time format");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"pickerValidationErrorMessagePatchObserved\":true",
                            "\"pickerValidationErrorMessagePatchObserved\":false"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-picker-validation",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains(
                            "picker validation "
                                    + "pickerValidationErrorMessagePatchObserved")) {
                throw new IllegalStateException(
                        "Picker validation inspector accepted a missing error patch");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"pickerValidationJavaMalformedValidationObserved\":true",
                            "\"pickerValidationJavaMalformedValidationObserved\":false"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-picker-validation",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains("picker validation "
                            + "pickerValidationJavaMalformedValidationObserved")) {
                throw new IllegalStateException(
                        "Picker validation inspector accepted missing malformed "
                                + "Java validation evidence");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"flowInputPopupNativePointerTrustedObserved\":true",
                            "\"flowInputPopupNativePointerTrustedObserved\":false"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-flow-input-popup",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains(
                            "Flow input popup "
                                    + "flowInputPopupNativePointerTrustedObserved")) {
                throw new IllegalStateException(
                        "Flow input popup inspector accepted untrusted pointer input");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"flowMenuPopupNativePointerTrustedObserved\":true",
                            "\"flowMenuPopupNativePointerTrustedObserved\":false"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-flow-menu-popup",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains(
                            "Flow menu popup "
                                    + "flowMenuPopupNativePointerTrustedObserved")) {
                throw new IllegalStateException(
                        "Flow menu popup inspector accepted untrusted pointer input");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"flowMenuPopupGridRowContextObserved\":true",
                            "\"flowMenuPopupGridRowContextObserved\":false"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-flow-menu-popup",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains(
                            "Flow menu popup "
                                    + "flowMenuPopupGridRowContextObserved")) {
                throw new IllegalStateException(
                        "Flow menu popup inspector accepted missing Grid row context");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"flowMenuPopupGridNestedJavaListenerObserved\":true",
                            "\"flowMenuPopupGridNestedJavaListenerObserved\":false"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-flow-menu-popup",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains(
                            "Flow menu popup "
                                    + "flowMenuPopupGridNestedJavaListenerObserved")) {
                throw new IllegalStateException(
                        "Flow menu popup inspector accepted a missing nested Grid listener");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"flowMenuPopupGridNestedContextRetainedObserved\":true",
                            "\"flowMenuPopupGridNestedContextRetainedObserved\":false"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-flow-menu-popup",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains(
                            "Flow menu popup "
                                    + "flowMenuPopupGridNestedContextRetainedObserved")) {
                throw new IllegalStateException(
                        "Flow menu popup inspector accepted missing "
                                + "Grid nested context retention");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"flowMenuPopupGridPolicyDynamicContentObserved\":true",
                            "\"flowMenuPopupGridPolicyDynamicContentObserved\":false"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-flow-menu-popup",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains(
                            "Flow menu popup "
                                    + "flowMenuPopupGridPolicyDynamicContentObserved")) {
                throw new IllegalStateException(
                        "Flow menu popup inspector accepted missing Grid dynamic content");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"flowMenuPopupNestedEscapeParentRemainsOpenObserved\":true",
                            "\"flowMenuPopupNestedEscapeParentRemainsOpenObserved\":false"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-flow-menu-popup",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains(
                            "Flow menu popup "
                                    + "flowMenuPopupNestedEscapeParentRemainsOpenObserved")) {
                throw new IllegalStateException(
                        "Flow menu popup inspector accepted a collapsed nested Escape stack");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"flowMenuPopupOverflowItemsRestoredObserved\":true",
                            "\"flowMenuPopupOverflowItemsRestoredObserved\":false"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-flow-menu-popup",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains(
                            "Flow menu popup "
                                    + "flowMenuPopupOverflowItemsRestoredObserved")) {
                throw new IllegalStateException(
                        "Flow menu popup inspector accepted missing overflow restoration");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"flowMenuPopupOverflowNestedLeftReturnObserved\":true",
                            "\"flowMenuPopupOverflowNestedLeftReturnObserved\":false"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-flow-menu-popup",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains(
                            "Flow menu popup "
                                    + "flowMenuPopupOverflowNestedLeftReturnObserved")) {
                throw new IllegalStateException(
                        "Flow menu popup inspector accepted missing "
                                + "overflow-nested ArrowLeft return");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"flowMenuPopupOverflowNestedEscapeParentRemainsOpenObserved\":true",
                            "\"flowMenuPopupOverflowNestedEscapeParentRemainsOpenObserved\":false"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-flow-menu-popup",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains(
                            "Flow menu popup "
                                    + "flowMenuPopupOverflowNestedEscapeParentRemainsOpenObserved")) {
                throw new IllegalStateException(
                        "Flow menu popup inspector accepted a collapsed "
                                + "overflow-nested Escape stack");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"flowMenuPopupContextNestedEscapeParentRemainsOpenObserved\":true",
                            "\"flowMenuPopupContextNestedEscapeParentRemainsOpenObserved\":false"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-flow-menu-popup",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains(
                            "Flow menu popup "
                                    + "flowMenuPopupContextNestedEscapeParentRemainsOpenObserved")) {
                throw new IllegalStateException(
                        "Flow menu popup inspector accepted a collapsed "
                                + "nested ContextMenu Escape stack");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"structuredRpcCancellationObserved\":true",
                            "\"structuredRpcCancellationObserved\":false"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-structured-rpc",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains(
                            "structured RPC structuredRpcCancellationObserved")) {
                throw new IllegalStateException(
                        "Structured RPC inspector accepted missing cancellation");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"structuredRpcPendingJavaScriptTypeShapesObserved\":true",
                            "\"structuredRpcPendingJavaScriptTypeShapesObserved\":false"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-structured-rpc",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains(
                            "structured RPC structuredRpcPendingJavaScriptTypeShapesObserved")) {
                throw new IllegalStateException(
                        "Structured RPC inspector accepted a missing type-shape matrix");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"structuredRpcDisabledDefaultBlockedObserved\":true",
                            "\"structuredRpcDisabledDefaultBlockedObserved\":false"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-structured-rpc",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains(
                            "structured RPC structuredRpcDisabledDefaultBlockedObserved")) {
                throw new IllegalStateException(
                        "Structured RPC inspector accepted disabled default RPC");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"structuredRpcDetachedOwnerCleanupObserved\":true",
                            "\"structuredRpcDetachedOwnerCleanupObserved\":false"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-structured-rpc",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains(
                            "structured RPC structuredRpcDetachedOwnerCleanupObserved")) {
                throw new IllegalStateException(
                        "Structured RPC inspector accepted missing detached owner cleanup");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"structuredRpcDetachedOwnerSideEffectExecuted\":false",
                            "\"structuredRpcDetachedOwnerSideEffectExecuted\":true"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-structured-rpc",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains(
                            "structured RPC detached owner side effect not executed")) {
                throw new IllegalStateException(
                        "Structured RPC inspector accepted a detached owner side effect");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"structuredRpcLateSubscriptionRejectedObserved\":true",
                            "\"structuredRpcLateSubscriptionRejectedObserved\":false"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-structured-rpc",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains(
                            "structured RPC structuredRpcLateSubscriptionRejectedObserved")) {
                throw new IllegalStateException(
                        "Structured RPC inspector accepted a late subscription");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"structuredRpcFutureCanceledSideEffectExecuted\":false",
                            "\"structuredRpcFutureCanceledSideEffectExecuted\":true"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-structured-rpc",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains(
                            "structured RPC canceled future side effect not executed")) {
                throw new IllegalStateException(
                        "Structured RPC inspector accepted a canceled future side effect");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"structuredRpcInertDefaultBlockedObserved\":true",
                            "\"structuredRpcInertDefaultBlockedObserved\":false"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-structured-rpc",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains(
                            "structured RPC structuredRpcInertDefaultBlockedObserved")) {
                throw new IllegalStateException(
                        "Structured RPC inspector accepted an inert default invocation");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"structuredRpcInertDefaultSettled\":false",
                            "\"structuredRpcInertDefaultSettled\":true"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-structured-rpc",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains(
                            "structured RPC inert default promise remains pending")) {
                throw new IllegalStateException(
                        "Structured RPC inspector accepted a settled inert default promise");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "{\"attempt\":2,\"delayMillis\":1000}",
                            "{\"attempt\":1,\"delayMillis\":500}"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-standard-bundle-sidecar-recovery",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains("recovery attempts 1, 2, then 1 after stability")) {
                throw new IllegalStateException(
                        "Sidecar recovery inspector accepted an invalid "
                                + "crash-loop attempt sequence");
            }

            String oldPushAcknowledgement = protocol(
                    "send", "opaquePushAck", """
                    {"connectionId":"opaque-push-1","windowId":"main",
                    "sessionId":"flow-ipc-session","uiId":"main","flowUiId":0,
                    "pushSequence":5,"flowServerSyncId":5,
                    "lastSeenServerSyncId":5}
                    """);
            Files.writeString(
                    trace,
                    validTrace().replace(
                            unacknowledgedPushBoundary(),
                            oldPushAcknowledgement
                                    + unacknowledgedPushBoundary()),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-standard-bundle-sidecar-recovery",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains(
                            "trace-correlated old push remained unacknowledged")) {
                throw new IllegalStateException(
                        "Sidecar recovery inspector accepted an acknowledged "
                                + "old-generation push");
            }

            Files.writeString(
                    trace,
                    validTrace().replace(
                            "\"stalePushNotReplayed\":true",
                            "\"stalePushNotReplayed\":false"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-standard-bundle-sidecar-recovery",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains("recovery stalePushNotReplayed")) {
                throw new IllegalStateException(
                        "Sidecar recovery inspector accepted a replayed "
                                + "old-generation push");
            }

            Files.writeString(
                    trace,
                    validRecoveryExhaustionTrace(),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-sidecar-recovery-exhaustion",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            summary = output.toString(StandardCharsets.UTF_8);
            if (exitCode != 0
                    || !errors.toString(StandardCharsets.UTF_8).isEmpty()
                    || !summary.contains(
                    "PASS --require-flow-ipc-sidecar-recovery-exhaustion")) {
                throw new IllegalStateException(
                        "Sidecar recovery exhaustion inspector rejected a valid trace:\n"
                                + summary
                                + errors.toString(StandardCharsets.UTF_8));
            }

            Files.writeString(trace,
                    protocol("launch", "{\"transport\":\"flow-ipc\"}"),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-standard-bundle-runtime", trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            if (exitCode != 2 || !errors.toString(StandardCharsets.UTF_8)
                    .contains("standard Java runtime ready")) {
                throw new IllegalStateException(
                        "Standard bundle inspector accepted an incomplete trace");
            }

            Files.writeString(trace, validViteTrace(), StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-vite-development-runtime", trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            summary = output.toString(StandardCharsets.UTF_8);
            if (exitCode != 0 || !errors.toString(StandardCharsets.UTF_8).isEmpty()
                    || !summary.contains(
                    "PASS --require-flow-ipc-vite-development-runtime")) {
                throw new IllegalStateException(
                        "Vite development inspector rejected a valid trace:\n" + summary
                                + errors.toString(StandardCharsets.UTF_8));
            }

            Files.writeString(
                    trace, validJavaReloadTrace(), StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-java-reload-development",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            summary = output.toString(StandardCharsets.UTF_8);
            if (exitCode != 0 || !errors.toString(StandardCharsets.UTF_8).isEmpty()
                    || !summary.contains(
                    "PASS --require-flow-ipc-java-reload-development")) {
                throw new IllegalStateException(
                        "Java reload inspector rejected a valid trace:\n"
                                + summary
                                + errors.toString(StandardCharsets.UTF_8));
            }

            Files.writeString(
                    trace,
                    validReactorReloadTrace(),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-reactor-reload-development",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            summary = output.toString(StandardCharsets.UTF_8);
            if (exitCode != 0
                    || !errors.toString(StandardCharsets.UTF_8).isEmpty()
                    || !summary.contains(
                    "PASS --require-flow-ipc-reactor-reload-development")) {
                throw new IllegalStateException(
                        "Reactor reload inspector rejected a valid trace:\n"
                                + summary
                                + errors.toString(StandardCharsets.UTF_8));
            }

            Files.writeString(
                    trace,
                    validJavaRollbackTrace(),
                    StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-java-reload-rollback-development",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            summary = output.toString(StandardCharsets.UTF_8);
            if (exitCode != 0
                    || !errors.toString(StandardCharsets.UTF_8).isEmpty()
                    || !summary.contains(
                    "PASS --require-flow-ipc-java-reload-rollback-development")) {
                throw new IllegalStateException(
                        "Java rollback inspector rejected a valid trace:\n"
                                + summary
                                + errors.toString(StandardCharsets.UTF_8));
            }

            Files.writeString(trace, validStartupFailureTrace(), StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-host-startup-failure", trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            summary = output.toString(StandardCharsets.UTF_8);
            if (exitCode != 0 || !errors.toString(StandardCharsets.UTF_8).isEmpty()
                    || !summary.contains(
                    "PASS --require-flow-ipc-host-startup-failure")) {
                throw new IllegalStateException(
                        "Host startup failure inspector rejected a valid trace:\n"
                                + summary + errors.toString(StandardCharsets.UTF_8));
            }

            Files.writeString(trace, validReadyTimeoutTrace(), StandardCharsets.UTF_8);
            output.reset();
            errors.reset();
            exitCode = StandardBundleTraceInspector.run(new String[]{
                    "--require-flow-ipc-host-ready-timeout", trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            summary = output.toString(StandardCharsets.UTF_8);
            if (exitCode != 0 || !errors.toString(StandardCharsets.UTF_8).isEmpty()
                    || !summary.contains(
                    "PASS --require-flow-ipc-host-ready-timeout")) {
                throw new IllegalStateException(
                        "Host ready timeout inspector rejected a valid trace:\n"
                                + summary + errors.toString(StandardCharsets.UTF_8));
            }
        } finally {
            Files.deleteIfExists(trace);
        }
        System.out.println("Standard bundle trace inspector smoke passed");
    }

    private static String validTrace() {
        StringBuilder trace = new StringBuilder();
        trace.append(protocol("launch", """
                {"transport":"flow-ipc"}
                """));
        trace.append(protocol("launch.sidecar", """
                {"generation":1,"transport":"flow-ipc"}
                """));
        trace.append(protocol("sidecar.started", """
                {"generation":1,"processIdentifier":101,"transport":"flow-ipc"}
                """));
        trace.append(protocol("ready", """
                {"runtime":"standard-vaadin-bundle-opaque-ipc",
                "browserRequestTransport":"opaque-http-stream-ipc",
                "transport":"flow-ipc",
                "flowClientResourceLoading":"standard-production-bundle"}
                """));
        trace.append(protocol("standardBundleReady",
                "{\"requestTransport\":\"opaque-http-stream-ipc\"}"));
        trace.append(protocol("standardBundleResource",
                "{\"path\":\"/VAADIN/build/FlowClient-test.js\"}"));
        trace.append(protocol("standardBundleResource",
                "{\"path\":\"/VAADIN/build/chunk-route-test.js\"}"));
        trace.append(protocol("standardBundleResource",
                "{\"path\":\"/VAADIN/build/chunk-nested-route-test.js\"}"));
        trace.append(protocol("standardBundleResource",
                "{\"path\":\"/VAADIN/build/vatori-lazy-transitive-test.js\"}"));
        trace.append(protocol("webview.firstMeaningfulPaint", "{}"));
        trace.append(protocol("opaqueHttpStreamOpen", "{}"));
        trace.append(protocol("opaqueHttpStreamOpen", "{}"));
        trace.append(protocol("opaqueHttpStreamResponse", "{\"status\":200}"));
        trace.append(protocol("opaqueHttpStreamResponse",
                "{\"envelope\":{\"payload\":{\"status\":200}}}"));
        trace.append(protocol("opaqueHttpStreamResponseEnd", "{}"));
        trace.append(protocol("opaqueHttpStreamResponseEnd", "{}"));
        trace.append(protocol("send", "opaqueHttpStreamOpen", """
                {"requestId":"large-download-initial","method":"GET",
                "headerNames":""}
                """));
        trace.append(protocol("recv", "opaqueHttpStreamResponse", """
                {"requestId":"large-download-initial","status":200}
                """));
        trace.append(protocol("send", "opaqueHttpStreamCancel", """
                {"requestId":"large-download-initial"}
                """));
        trace.append(protocol("send", "opaqueHttpStreamOpen", """
                {"requestId":"large-download-resume","method":"GET",
                "headerNames":"Range"}
                """));
        trace.append(protocol("recv", "opaqueHttpStreamResponse", """
                {"requestId":"large-download-resume","status":206}
                """));
        trace.append(protocol("recv", "opaqueHttpStreamResponseEnd", """
                {"requestId":"large-download-resume","chunkCount":121,
                "bodyBytes":3932273}
                """));
        trace.append(protocol("send", "opaqueHttpStreamOpen", """
                {"requestId":"large-upload-cancel","method":"POST",
                "headerNames":"Content-Type,X-Filename",
                "contentLength":4194515}
                """));
        trace.append(protocol("send", "opaqueHttpStreamChunk", """
                {"requestId":"large-upload-cancel","chunkSequence":7,
                "bodyBytes":32768}
                """));
        trace.append(protocol("send", "opaqueHttpStreamCancel", """
                {"requestId":"large-upload-cancel"}
                """));
        trace.append(protocol("send", "opaqueHttpStreamOpen", """
                {"requestId":"large-upload-retry","method":"POST",
                "headerNames":"Content-Type,X-Filename",
                "contentLength":4194515}
                """));
        trace.append(protocol("send", "opaqueHttpStreamEnd", """
                {"requestId":"large-upload-retry","chunkCount":129,
                "bodyBytes":4194515}
                """));
        trace.append(protocol("recv", "opaqueHttpStreamResponse", """
                {"requestId":"large-upload-retry","status":200}
                """));
        trace.append(protocol("send", "opaqueHttpStreamOpen", """
                {"requestId":"large-multipart-upload-cancel","method":"POST",
                "url":"https://flow-ipc.invalid/VAADIN/dynamic/resource/0/test/upload",
                "headerNames":"Content-Type","contentLength":-1}
                """));
        trace.append(protocol("send", "opaqueHttpStreamChunk", """
                {"requestId":"large-multipart-upload-cancel",
                "chunkSequence":8,"bodyBytes":262299}
                """));
        trace.append(protocol("send", "opaqueHttpStreamCancel", """
                {"requestId":"large-multipart-upload-cancel"}
                """));
        trace.append(protocol("send", "opaqueHttpStreamOpen", """
                {"requestId":"large-multipart-upload-retry","method":"POST",
                "url":"https://flow-ipc.invalid/VAADIN/dynamic/resource/0/test/upload",
                "headerNames":"Content-Type","contentLength":-1}
                """));
        trace.append(protocol("send", "opaqueHttpStreamChunk", """
                {"requestId":"large-multipart-upload-retry",
                "chunkSequence":129,"bodyBytes":4194822}
                """));
        trace.append(protocol("send", "opaqueHttpStreamEnd", """
                {"requestId":"large-multipart-upload-retry","chunkCount":129,
                "bodyBytes":4194822}
                """));
        trace.append(protocol("recv", "opaqueHttpStreamResponse", """
                {"requestId":"large-multipart-upload-retry","status":200}
                """));
        trace.append(protocol("send", "opaqueHttpStreamOpen", """
                {"requestId":"concurrent-upload-cancel","method":"POST",
                "headerNames":"Content-Type,X-Filename",
                "contentLength":4194405}
                """));
        trace.append(protocol("send", "opaqueHttpStreamOpen", """
                {"requestId":"concurrent-upload-survivor","method":"POST",
                "headerNames":"Content-Type,X-Filename",
                "contentLength":3145877}
                """));
        trace.append(protocol("send", "opaqueHttpStreamChunk", """
                {"requestId":"concurrent-upload-cancel","chunkSequence":4,
                "bodyBytes":32768}
                """));
        trace.append(protocol("send", "opaqueHttpStreamChunk", """
                {"requestId":"concurrent-upload-survivor","chunkSequence":4,
                "bodyBytes":32768}
                """));
        trace.append(protocol("send", "opaqueHttpStreamCancel", """
                {"requestId":"concurrent-upload-cancel"}
                """));
        trace.append(protocol("send", "opaqueHttpStreamEnd", """
                {"requestId":"concurrent-upload-survivor","chunkCount":97,
                "bodyBytes":3145877}
                """));
        trace.append(protocol("recv", "opaqueHttpStreamResponse", """
                {"requestId":"concurrent-upload-survivor","status":200}
                """));
        trace.append(protocol("send", "opaquePushConnect", """
                {"connectionId":"opaque-push-1","windowId":"main",
                "sessionId":"flow-ipc-session","uiId":"main","flowUiId":0,
                "lastSeenServerSyncId":0}
                """));
        trace.append(protocol("recv", "opaquePushConnected", """
                {"connectionId":"opaque-push-1","windowId":"main",
                "sessionId":"flow-ipc-session","uiId":"main","flowUiId":0}
                """));
        trace.append(protocol("recv", "opaquePush", """
                {"connectionId":"opaque-push-1","windowId":"main",
                "sessionId":"flow-ipc-session","uiId":"main","flowUiId":0,
                "resync":false,"pushSequence":1}
                """));
        trace.append(protocol("send", "opaquePushDisconnect", """
                {"connectionId":"opaque-push-1","windowId":"main",
                "sessionId":"flow-ipc-session","uiId":"main","flowUiId":0}
                """));
        trace.append(protocol("send", "opaquePushConnect", """
                {"connectionId":"opaque-push-1","windowId":"main",
                "sessionId":"flow-ipc-session","uiId":"main","flowUiId":0,
                "lastSeenServerSyncId":1}
                """));
        trace.append(protocol("send", "opaquePushResync", """
                {"connectionId":"opaque-push-1","windowId":"main",
                "sessionId":"flow-ipc-session","uiId":"main","flowUiId":0}
                """));
        trace.append(protocol("recv", "opaquePushConnected", """
                {"connectionId":"opaque-push-1","windowId":"main",
                "sessionId":"flow-ipc-session","uiId":"main","flowUiId":0}
                """));
        trace.append(protocol("recv", "opaquePush", """
                {"connectionId":"opaque-push-1","windowId":"main",
                "sessionId":"flow-ipc-session","uiId":"main","flowUiId":0,
                "resync":true,"pushSequence":2}
                """));
        trace.append(protocol("recv", "opaquePush", """
                {"connectionId":"opaque-push-1","windowId":"main",
                "sessionId":"flow-ipc-session","uiId":"main","flowUiId":0,
                "resync":false,"pushSequence":3}
                """));
        trace.append(protocol("recv", "opaquePush", """
                {"connectionId":"opaque-push-1","windowId":"main",
                "sessionId":"flow-ipc-session","uiId":"main","flowUiId":0,
                "resync":false,"pushSequence":4,"flowServerSyncId":4,
                "deliveryAttempt":1,"uidlBytes":128}
                """));
        trace.append(protocol("recv", "opaquePush", """
                {"connectionId":"opaque-push-1","windowId":"main",
                "sessionId":"flow-ipc-session","uiId":"main","flowUiId":0,
                "resync":false,"pushSequence":4,"flowServerSyncId":4,
                "deliveryAttempt":2,"uidlBytes":128}
                """));
        trace.append(protocol("send", "opaquePushAck", """
                {"connectionId":"opaque-push-1","windowId":"main",
                "sessionId":"flow-ipc-session","uiId":"main","flowUiId":0,
                "pushSequence":4,"flowServerSyncId":4,"lastSeenServerSyncId":4}
                """));
        trace.append(protocol("flowPushRecovery.frameworkAcceptance", allTrue(
                "complete", "standardBundleMode", "rootRendered", "flowClientOwned",
                "opaqueRequestBridgeObserved", "opaqueResponseBridgeObserved",
                "pushBridgeInstalled", "initialPushObserved", "reconnectObserved",
                "gapDetected", "resyncObserved", "connectionPreserved",
                "postResyncPushObserved", "postRecoveryDomObserved", "errorFree")));
        trace.append(protocol("flowPushDelivery.frameworkAcceptance",
                allTrue(
                        "complete", "standardBundleMode", "rootRendered",
                        "flowClientOwned", "opaqueRequestBridgeObserved",
                        "opaqueResponseBridgeObserved", "pushBridgeInstalled",
                        "baselineAcknowledged", "firstDeliveryDropped",
                        "sameSequenceRedelivered",
                        "flowLastSeenAcknowledged", "terminalDomObserved",
                        "connectionPreserved", "errorFree")
                        .replace("}", ",\"droppedSequence\":4}")));
        String largeDownloadAcceptance = allTrue(
                "complete", "standardBundleMode", "rootRendered",
                "flowClientOwned", "opaqueRequestBridgeObserved",
                "opaqueResponseBridgeObserved", "pushObserved",
                "largeDownloadInitialResponseObserved",
                "largeDownloadCancellationObserved",
                "largeDownloadPartialResponseObserved",
                "largeDownloadContentRangeObserved",
                "largeDownloadExactReassemblyObserved",
                "largeDownloadServerCancellationObserved",
                "largeDownloadJavaListenerObserved", "errorFree");
        largeDownloadAcceptance = withNumber(
                largeDownloadAcceptance, "initialStatus", 200);
        largeDownloadAcceptance = withNumber(
                largeDownloadAcceptance, "resumeStatus", 206);
        largeDownloadAcceptance = withNumber(
                largeDownloadAcceptance, "totalBytes", 4194417);
        largeDownloadAcceptance = withNumber(
                largeDownloadAcceptance, "cancelledBytes", 262144);
        largeDownloadAcceptance = withNumber(
                largeDownloadAcceptance, "resumedBytes", 3932273);
        trace.append(protocol(
                "flowLargeDownload.frameworkAcceptance",
                largeDownloadAcceptance));
        String largeUploadAcceptance = allTrue(
                "complete", "standardBundleMode", "rootRendered",
                "flowClientOwned", "opaqueRequestBridgeObserved",
                "opaqueResponseBridgeObserved", "pushObserved",
                "largeUploadProgressObserved",
                "largeUploadAbortObserved",
                "largeUploadRemovalObserved",
                "largeUploadRetryObserved",
                "largeUploadSuccessObserved",
                "largeUploadExactBytesObserved",
                "largeUploadServerCancellationObserved",
                "largeUploadJavaListenerObserved", "errorFree");
        largeUploadAcceptance = withNumber(
                largeUploadAcceptance, "totalBytes", 4194515);
        largeUploadAcceptance = withNumber(
                largeUploadAcceptance, "cancelThreshold", 262144);
        largeUploadAcceptance = withNumber(
                largeUploadAcceptance, "firstProgressBytes", 262144);
        largeUploadAcceptance = withNumber(
                largeUploadAcceptance, "retryProgressBytes", 4194515);
        largeUploadAcceptance = withNumber(
                largeUploadAcceptance, "serverCancelledBytes", 131072);
        largeUploadAcceptance = withNumber(
                largeUploadAcceptance, "serverSuccessBytes", 4194515);
        largeUploadAcceptance = withNumber(
                largeUploadAcceptance, "serverDeclaredBytes", 4194515);
        largeUploadAcceptance = withNumber(
                largeUploadAcceptance, "startEvents", 2);
        largeUploadAcceptance = withNumber(
                largeUploadAcceptance, "abortEvents", 1);
        largeUploadAcceptance = withNumber(
                largeUploadAcceptance, "removeEvents", 1);
        largeUploadAcceptance = withNumber(
                largeUploadAcceptance, "successEvents", 1);
        largeUploadAcceptance = withNumber(
                largeUploadAcceptance, "errorEvents", 0);
        trace.append(protocol(
                "flowLargeUpload.frameworkAcceptance",
                largeUploadAcceptance));
        String largeMultipartUploadAcceptance = allTrue(
                "complete", "standardBundleMode", "rootRendered",
                "flowClientOwned", "opaqueRequestBridgeObserved",
                "opaqueResponseBridgeObserved", "pushObserved",
                "largeMultipartUploadRequestObserved",
                "largeMultipartUploadProgressObserved",
                "largeMultipartUploadAbortObserved",
                "largeMultipartUploadRemovalObserved",
                "largeMultipartUploadRetryObserved",
                "largeMultipartUploadSuccessObserved",
                "largeMultipartUploadCancellationIsolationObserved",
                "largeMultipartUploadExactBytesObserved",
                "largeMultipartUploadDiskSpillObserved",
                "largeMultipartUploadTemporaryPartCleanupObserved",
                "largeMultipartUploadJavaListenersObserved", "errorFree");
        largeMultipartUploadAcceptance = withNumber(
                largeMultipartUploadAcceptance, "totalFileBytes", 4194611);
        largeMultipartUploadAcceptance = withNumber(
                largeMultipartUploadAcceptance, "cancelThreshold", 262144);
        largeMultipartUploadAcceptance = withNumber(
                largeMultipartUploadAcceptance, "firstProgressBytes", 262299);
        largeMultipartUploadAcceptance = withNumber(
                largeMultipartUploadAcceptance, "retryProgressBytes", 4194822);
        largeMultipartUploadAcceptance = withNumber(
                largeMultipartUploadAcceptance, "progressEvents", 130);
        largeMultipartUploadAcceptance = withNumber(
                largeMultipartUploadAcceptance, "startEvents", 2);
        largeMultipartUploadAcceptance = withNumber(
                largeMultipartUploadAcceptance, "requestEvents", 2);
        largeMultipartUploadAcceptance = withNumber(
                largeMultipartUploadAcceptance, "multipartRequestEvents", 2);
        largeMultipartUploadAcceptance = withNumber(
                largeMultipartUploadAcceptance, "multipartEntryEvents", 2);
        largeMultipartUploadAcceptance = withNumber(
                largeMultipartUploadAcceptance, "abortEvents", 1);
        largeMultipartUploadAcceptance = withNumber(
                largeMultipartUploadAcceptance, "removeEvents", 1);
        largeMultipartUploadAcceptance = withNumber(
                largeMultipartUploadAcceptance, "successEvents", 1);
        largeMultipartUploadAcceptance = withNumber(
                largeMultipartUploadAcceptance, "errorEvents", 0);
        largeMultipartUploadAcceptance = withNumber(
                largeMultipartUploadAcceptance, "serverHandlerCalls", 1);
        largeMultipartUploadAcceptance = withNumber(
                largeMultipartUploadAcceptance, "serverHandlerFailures", 0);
        largeMultipartUploadAcceptance = withNumber(
                largeMultipartUploadAcceptance, "serverResponseCalls", 1);
        largeMultipartUploadAcceptance = withNumber(
                largeMultipartUploadAcceptance, "serverFailedResponses", 0);
        largeMultipartUploadAcceptance = withNumber(
                largeMultipartUploadAcceptance,
                "serverFailedAcceptedFiles", -1);
        largeMultipartUploadAcceptance = withNumber(
                largeMultipartUploadAcceptance,
                "serverSuccessfulResponses", 1);
        largeMultipartUploadAcceptance = withNumber(
                largeMultipartUploadAcceptance,
                "serverSuccessfulAcceptedFiles", 1);
        largeMultipartUploadAcceptance = withNumber(
                largeMultipartUploadAcceptance,
                "serverSuccessfulBytes", 4194611);
        largeMultipartUploadAcceptance = withNumber(
                largeMultipartUploadAcceptance,
                "serverDeclaredBytes", 4194611);
        largeMultipartUploadAcceptance = withNumber(
                largeMultipartUploadAcceptance,
                "serverSpillBytes", 4194611);
        trace.append(protocol(
                "flowLargeMultipartUpload.frameworkAcceptance",
                largeMultipartUploadAcceptance));
        String concurrentUploadAcceptance = allTrue(
                "complete", "standardBundleMode", "rootRendered",
                "flowClientOwned", "opaqueRequestBridgeObserved",
                "opaqueResponseBridgeObserved", "pushObserved",
                "concurrentUploadBrowserOverlapObserved",
                "concurrentUploadProgressObserved",
                "concurrentUploadAbortObserved",
                "concurrentUploadRemovalObserved",
                "concurrentUploadSurvivorObserved",
                "concurrentUploadRequestIsolationObserved",
                "concurrentUploadExactBytesObserved",
                "concurrentUploadJavaListenersObserved", "errorFree");
        concurrentUploadAcceptance = withNumber(
                concurrentUploadAcceptance, "cancelledTotalBytes", 4194405);
        concurrentUploadAcceptance = withNumber(
                concurrentUploadAcceptance, "survivorTotalBytes", 3145877);
        concurrentUploadAcceptance = withNumber(
                concurrentUploadAcceptance, "cancelThreshold", 262144);
        concurrentUploadAcceptance = withNumber(
                concurrentUploadAcceptance, "overlapThreshold", 65536);
        concurrentUploadAcceptance = withNumber(
                concurrentUploadAcceptance, "cancelledProgressBytes", 262144);
        concurrentUploadAcceptance = withNumber(
                concurrentUploadAcceptance, "survivorProgressBytes", 3145877);
        concurrentUploadAcceptance = withNumber(
                concurrentUploadAcceptance, "progressEvents", 16);
        concurrentUploadAcceptance = withNumber(
                concurrentUploadAcceptance, "startEvents", 2);
        concurrentUploadAcceptance = withNumber(
                concurrentUploadAcceptance, "abortEvents", 1);
        concurrentUploadAcceptance = withNumber(
                concurrentUploadAcceptance, "removeEvents", 1);
        concurrentUploadAcceptance = withNumber(
                concurrentUploadAcceptance, "successEvents", 1);
        concurrentUploadAcceptance = withNumber(
                concurrentUploadAcceptance, "errorEvents", 0);
        concurrentUploadAcceptance = withNumber(
                concurrentUploadAcceptance, "browserMaximumActive", 2);
        concurrentUploadAcceptance = withNumber(
                concurrentUploadAcceptance, "serverMaximumActive", 2);
        concurrentUploadAcceptance = withNumber(
                concurrentUploadAcceptance, "serverCancelledBytes", 131072);
        concurrentUploadAcceptance = withNumber(
                concurrentUploadAcceptance,
                "serverCancelledDeclaredBytes",
                4194405);
        concurrentUploadAcceptance = withNumber(
                concurrentUploadAcceptance, "serverSuccessBytes", 3145877);
        concurrentUploadAcceptance = withNumber(
                concurrentUploadAcceptance,
                "serverSuccessDeclaredBytes",
                3145877);
        trace.append(protocol(
                "flowConcurrentUpload.frameworkAcceptance",
                concurrentUploadAcceptance));
        trace.append(protocol("standardBundle.frameworkAcceptance", allTrue(
                "complete", "standardBundleMode", "rootRendered", "flowClientOwned",
                "opaqueRequestBridgeObserved", "opaqueResponseBridgeObserved",
                "pushRecoveryObserved", "errorFree", "budgetEventObserved",
                "lazyFrontendRouteObserved", "lazyFrontendClientObserved",
                "lazyFrontendServerObserved", "lazyFrontendReturned",
                "nestedLazyLayoutObserved", "nestedLazyRouteObserved",
                "nestedLazyTransitiveModuleObserved",
                "nestedLazyClientObserved", "nestedLazyServerObserved",
                "nestedLazyReturned",
                "checkboxEventObserved", "selectEventObserved",
                "comboBoxEventObserved", "datePickerEventObserved",
                "radioGroupEventObserved", "textFieldClientObserved",
                "textFieldServerObserved", "textAreaEventObserved",
                "emailFieldEventObserved", "integerFieldEventObserved",
                "checkboxGroupClientObserved", "checkboxGroupServerObserved",
                "passwordFieldEventObserved", "timePickerEventObserved",
                "sliderClientObserved", "sliderServerObserved",
                "multiSelectComboBoxEventObserved",
                "dialogEventObserved", "dialogInitialFocusObserved",
                "dialogFocusTrapObserved", "dialogEscapeCloseObserved",
                "dialogFocusRestoredObserved", "dialogA11yObserved",
                "modalOverlayStackOpenedObserved",
                "modalOverlayStackInitialFocusObserved",
                "modalOverlayStackFocusTrapObserved",
                "modalOverlayStackTopEscapeObserved",
                "modalOverlayStackParentRemainsOpenObserved",
                "modalOverlayStackFocusRestoredObserved",
                "modalOverlayStackA11yObserved",
                "formValidationObserved",
                "formLayoutClickObserved", "formLayoutServerObserved",
                "horizontalLayoutClickObserved", "horizontalLayoutServerObserved",
                "flexLayoutClickObserved", "flexLayoutServerObserved",
                "verticalLayoutClickObserved", "verticalLayoutServerObserved",
                "scrollerClientObserved", "scrollerServerObserved",
                "iconClientObserved", "iconServerObserved",
                "appLayoutClientObserved", "appLayoutServerObserved",
                "masterDetailClientObserved", "masterDetailContentObserved",
                "masterDetailServerObserved",
                "cardClientObserved", "cardServerObserved",
                "markdownClientObserved", "markdownServerObserved",
                "badgeClientObserved", "badgeServerObserved",
                "avatarClickObserved", "messageInputSubmitObserved",
                "richTextEditorEditObserved", "loginOverlaySubmitObserved",
                "loginOverlayForgotPasswordObserved",
                "loginOverlayShowErrorObserved", "loginFormSubmitObserved",
                "loginFormForgotPasswordObserved",
                "loginOverlayActionPostSubmitObserved",
                "loginFormActionPostSubmitObserved",
                "loginOverlaySubmitButtonObserved",
                "loginFormSubmitButtonObserved",
                "loginFormShowErrorObserved",
                "loginFormEnabledLifecycleObserved",
                "loginFormFullI18nObserved",
                "loginFormAutofillAttributesObserved",
                "loginFormA11ySemanticsObserved",
                "loginFormNoAutofocusObserved",
                "loginOverlayAutofocusControlObserved",
                "loginOverlayNoAutofocusObserved",
                "loginOverlayOutsideClickPreventedObserved",
                "loginOverlayEscapeKeyPreventedObserved",
                "loginOverlayAutofillAttributesObserved",
                "loginOverlayA11ySemanticsObserved",
                "loginOverlayFullI18nObserved",
                "tabsSelectionObserved",
                "detailsOpenObserved", "accordionOpenObserved", "menuBarActionObserved",
                "sideNavSelectionObserved", "applyEventObserved", "contextMenuOpenObserved", "contextMenuItemClickObserved",
                "gridContextMenuOpenObserved", "gridContextMenuItemClickObserved",
                "lazyGridScrollObserved", "lazyGridFarViewportObserved",
                "lazyGridSelectionObserved", "lazyGridReturnViewportObserved",
                "lazyGridSortObserved", "lazyGridFilterObserved",
                "lazyGridFilterResetObserved",
                "treeGridRootLoadedObserved",
                "treeGridPlatformExpandedObserved",
                "treeGridRuntimeExpandedObserved",
                "treeGridSelectionObserved", "treeGridRefreshObserved",
                "treeGridCollapseObserved",
                "componentRendererObserved", "gridEditorOpenedObserved",
                "gridEditorSaveObserved", "gridEditorCancelObserved",
                "gridEditorValidationObserved",
                "gridEditorValidationCancelObserved",
                "litRendererPropertiesObserved",
                "litRendererFunctionObserved",
                "litRendererRefreshObserved",
                "clientCallableResolveObserved",
                "clientCallableRejectObserved",
                "pendingJavaScriptResolveObserved",
                "pendingJavaScriptRejectObserved",
                "flowDndConfiguredObserved",
                "flowDndDragStartObserved",
                "flowDndDropObserved",
                "flowDndDragEndObserved",
                "virtualListClientObserved", "virtualListServerObserved",
                "popoverOpenObserved",
                "anchoredModelessOverlayObserved",
                "anchoredModelessOverlayA11yObserved",
                "anchoredModelessOverlayTabOrderObserved",
                "anchoredModelessOverlayEscapeCloseObserved",
                "anchoredModelessOverlayEscapeFocusRestoredObserved",
                "anchoredModelessOverlayOutsideCloseObserved",
                "anchoredModelessOverlayOutsideFocusPreservedObserved",
                "mixedOverlayStackDialogOpenedObserved",
                "mixedOverlayStackOpenedObserved",
                "mixedOverlayStackA11yObserved",
                "mixedOverlayStackTabOrderObserved",
                "mixedOverlayStackTopEscapeObserved",
                "mixedOverlayStackParentRemainsOpenObserved",
                "mixedOverlayStackPopoverFocusRestoredObserved",
                "mixedOverlayStackOutsideCloseObserved",
                "mixedOverlayStackOutsideFocusPreservedObserved",
                "mixedOverlayStackParentEscapeObserved",
                "mixedOverlayStackLayeredFocusRestoredObserved",
                "nestedAnchoredOverlayStackOuterOpenedObserved",
                "nestedAnchoredOverlayStackOpenedObserved",
                "nestedAnchoredOverlayStackA11yObserved",
                "nestedAnchoredOverlayStackTabOrderObserved",
                "nestedAnchoredOverlayStackTopEscapeObserved",
                "nestedAnchoredOverlayStackParentRemainsOpenObserved",
                "nestedAnchoredOverlayStackInnerFocusRestoredObserved",
                "nestedAnchoredOverlayStackParentEscapeObserved",
                "nestedAnchoredOverlayStackLayeredFocusRestoredObserved",
                "nestedAnchoredOverlayStackOutsideTopCloseObserved",
                "nestedAnchoredOverlayStackOutsideParentRemainsOpenObserved",
                "nestedAnchoredOverlayStackOutsideFocusPreservedObserved",
                "nestedAnchoredOverlayStackOutsideParentCloseObserved",
                "notificationOpenObserved",
                "confirmDialogOpenObserved", "confirmDialogConfirmObserved",
                "confirmDialogCancelObserved", "confirmDialogRejectObserved",
                "progressBarValueObserved", "progressBarIndeterminateObserved",
                "dateTimePickerClientObserved", "dateTimePickerServerObserved",
                "numberFieldClientObserved", "numberFieldServerObserved",
                "splitLayoutDragObserved", "splitLayoutServerObserved",
                "listBoxClientObserved", "listBoxServerObserved",
                "multiSelectListBoxClientObserved", "multiSelectListBoxServerObserved",
                "customFieldClientObserved", "customFieldServerObserved",
                "uploadSubmitObserved",
                "streamResourceFetchObserved", "binaryStreamResourceFetchObserved",
                "downloadHandlerFetchObserved", "standardStreamingDownloadFetchObserved",
                "serverStateUpdated", "nativeWindowTitleApplied",
                "nativeClipboardApplied")));
        trace.append(protocol("lazyGridQuery.frameworkAcceptance", allTrue(
                "complete", "standardBundleMode", "rootRendered",
                "flowClientOwned", "opaqueRequestBridgeObserved",
                "opaqueResponseBridgeObserved", "pushObserved", "errorFree",
                "lazyGridFarViewportObserved", "lazyGridSelectionObserved",
                "lazyGridReturnViewportObserved", "lazyGridSortObserved",
                "lazyGridFilterObserved", "lazyGridFilterResetObserved")));
        String flowDndAcceptance = withText(withText(withText(withText(
                withFalse(allTrue(
                "complete", "standardBundleMode", "rootRendered",
                "flowClientOwned", "opaqueRequestBridgeObserved",
                "opaqueResponseBridgeObserved", "pushObserved", "errorFree",
                "flowDndDedicatedRouteObserved", "flowDndConfiguredObserved",
                "flowDndNativePointerObserved",
                "flowDndTrustedBrowserEventsObserved",
                "flowDndDocumentPointerDownTrusted",
                "flowDndPointerDownTrusted",
                "flowDndDragStartTrusted",
                "flowDndDragEnterTrusted",
                "flowDndDragOverTrusted",
                "flowDndDropTrusted",
                "flowDndDragEndTrusted",
                "flowDndDragStartObserved",
                "flowDndDropObserved", "flowDndDragEndObserved"),
                "flowDndBrowserProgrammaticObserved"),
                "flowDndInputMode", "appkit-event-queue-pointer"),
                "flowDndDragStartStatus", "client=true change=1"),
                "flowDndDropStatus", "client=true change=1"),
                "flowDndDragEndStatus", "client=true change=1");
        trace.append(protocol("flowDnd.frameworkAcceptance", flowDndAcceptance));
        trace.append(protocol("flowInputPopup.frameworkAcceptance", withNumber(
                withNumber(withNumber(allTrue(
                "complete", "standardBundleMode", "rootRendered",
                "flowClientOwned", "opaqueRequestBridgeObserved",
                "opaqueResponseBridgeObserved", "pushObserved", "errorFree",
                "flowInputPopupDedicatedRouteObserved",
                "flowInputPopupBrowserProgrammaticInputObserved",
                "flowInputPopupNativeKeyboardObserved",
                "flowInputPopupNativePointerObserved",
                "flowInputPopupNativePointerTrustedObserved",
                "flowInputPopupComboOpenedObserved",
                "flowInputPopupComboA11yObserved",
                "flowInputPopupComboOwnerFocusObserved",
                "flowInputPopupComboActiveDescendantObserved",
                "flowInputPopupComboSelectionObserved",
                "flowInputPopupComboJavaValueObserved",
                "flowInputPopupComboClosedObserved",
                "flowInputPopupComboFocusRestoredObserved",
                "flowInputPopupComboCollapsedA11yObserved",
                "flowInputPopupSelectOpenedObserved",
                "flowInputPopupSelectA11yObserved",
                "flowInputPopupSelectInitialFocusObserved",
                "flowInputPopupSelectKeyboardMoveObserved",
                "flowInputPopupSelectSelectionObserved",
                "flowInputPopupSelectJavaValueObserved",
                "flowInputPopupSelectClosedObserved",
                "flowInputPopupSelectFocusRestoredObserved",
                "flowInputPopupSelectCollapsedA11yObserved",
                "flowInputPopupSelectEscapeOpenedObserved",
                "flowInputPopupSelectEscapeInitialFocusObserved",
                "flowInputPopupSelectEscapeCloseObserved",
                "flowInputPopupSelectValueRetainedObserved",
                "flowInputPopupSelectPointerOpenedObserved",
                "flowInputPopupSelectPointerCloseObserved",
                "flowInputPopupSelectPointerValueRetainedObserved",
                "flowInputPopupSelectPointerFocusRestoredObserved",
                "flowInputPopupSelectPointerJavaLifecycleObserved",
                "flowInputPopupSelectPointerUnderlyingClickSuppressedObserved",
                "flowInputPopupSelectNativeKeyboardObserved",
                "flowInputPopupDateOpenedObserved",
                "flowInputPopupDateA11yObserved",
                "flowInputPopupDateCalendarFocusObserved",
                "flowInputPopupDateKeyboardMoveObserved",
                "flowInputPopupDateSelectionObserved",
                "flowInputPopupDateJavaValueObserved",
                "flowInputPopupDateClosedObserved",
                "flowInputPopupDateFocusRestoredObserved",
                "flowInputPopupDateEscapeOpenedObserved",
                "flowInputPopupDateEscapeCloseObserved",
                "flowInputPopupDateValueRetainedObserved",
                "flowInputPopupDateJavaLifecycleObserved",
                "flowInputPopupDatePointerOpenedObserved",
                "flowInputPopupDatePointerFocusObserved",
                "flowInputPopupDatePointerCloseObserved",
                "flowInputPopupDatePointerValueRetainedObserved",
                "flowInputPopupDatePointerFocusRestoredObserved",
                "flowInputPopupDatePointerUnderlyingClickSuppressedObserved",
                "flowInputPopupDatePointerJavaLifecycleObserved"),
                "flowInputPopupNativeKeyboardEventCount", 12),
                "flowInputPopupNativePointerEventCount", 2),
                "flowInputPopupNativePointerTrustedEventCount", 2)));
        String localizedInputPopupAcceptance = withText(withNumber(allTrue(
                "complete", "standardBundleMode", "rootRendered",
                "flowClientOwned", "opaqueRequestBridgeObserved",
                "opaqueResponseBridgeObserved", "pushObserved", "errorFree",
                "flowInputPopupDedicatedRouteObserved",
                "flowInputPopupLocalizedTimeOnly",
                "flowInputPopupNativeKeyboardObserved",
                "flowInputPopupTimeOpenedObserved",
                "flowInputPopupTimeA11yObserved",
                "flowInputPopupTimeInitialOwnerFocusObserved",
                "flowInputPopupTimeLocaleFormatObserved",
                "flowInputPopupTimeFirstOptionFocusObserved",
                "flowInputPopupTimeSecondOptionFocusObserved",
                "flowInputPopupTimeSelectedOptionFocusObserved",
                "flowInputPopupTimeKeyboardMoveObserved",
                "flowInputPopupTimeSelectionObserved",
                "flowInputPopupTimeJavaValueObserved",
                "flowInputPopupTimeClosedObserved",
                "flowInputPopupTimeFocusRestoredObserved",
                "flowInputPopupTimeNativeKeyboardObserved"),
                "flowInputPopupNativeKeyboardEventCount", 6),
                "flowInputPopupNativeKeyboardStages",
                "time-open,time-move,time-move,time-move,time-move,time-select");
        trace.append(protocol(
                "flowInputPopup.localizedTimeAcceptance",
                localizedInputPopupAcceptance));
        String pickerValidationAcceptance = withText(withText(withText(withText(
                withText(
                withFalse(allTrue(
                        "complete", "standardBundleMode", "rootRendered",
                        "flowClientOwned", "opaqueRequestBridgeObserved",
                        "opaqueResponseBridgeObserved", "pushObserved", "errorFree",
                        "pickerValidationBrowserProgrammaticInputObserved",
                        "pickerValidationDedicatedRouteObserved",
                        "pickerValidationInitialStateObserved",
                        "pickerValidationConstraintPropertiesObserved",
                        "pickerValidationInvalidActionDispatched",
                        "pickerValidationMinimumBrowserChangeObserved",
                        "pickerValidationJavaInvalidObserved",
                        "pickerValidationInvalidPatchObserved",
                        "pickerValidationErrorMessagePatchObserved",
                        "pickerValidationChildInvalidPatchObserved",
                        "pickerValidationRecoveryActionDispatched",
                        "pickerValidationMinimumRecoveryBrowserChangeObserved",
                        "pickerValidationJavaRecoveryObserved",
                        "pickerValidationRecoveredValuePatchObserved",
                        "pickerValidationValidPatchObserved",
                        "pickerValidationErrorClearedObserved",
                        "pickerValidationChildValidPatchObserved",
                        "pickerValidationMaximumActionDispatched",
                        "pickerValidationMaximumBrowserChangeObserved",
                        "pickerValidationJavaMaximumObserved",
                        "pickerValidationMaximumInvalidPatchObserved",
                        "pickerValidationMaximumErrorMessagePatchObserved",
                        "pickerValidationMaximumChildInvalidPatchObserved",
                        "pickerValidationMaximumRecoveryActionDispatched",
                        "pickerValidationMaximumRecoveryBrowserChangeObserved",
                        "pickerValidationMaximumRecoveryObserved",
                        "pickerValidationIncompleteActionDispatched",
                        "pickerValidationIncompleteBrowserChangeObserved",
                        "pickerValidationJavaIncompleteObserved",
                        "pickerValidationIncompleteInvalidPatchObserved",
                        "pickerValidationIncompleteErrorMessagePatchObserved",
                        "pickerValidationIncompleteChildInvalidPatchObserved",
                        "pickerValidationIncompleteRecoveryActionDispatched",
                        "pickerValidationIncompleteRecoveryBrowserChangeObserved",
                        "pickerValidationIncompleteRecoveryObserved",
                        "pickerValidationRequiredActionDispatched",
                        "pickerValidationRequiredBrowserChangeObserved",
                        "pickerValidationJavaRequiredObserved",
                        "pickerValidationRequiredInvalidPatchObserved",
                        "pickerValidationRequiredErrorMessagePatchObserved",
                        "pickerValidationRequiredChildInvalidPatchObserved",
                        "pickerValidationMalformedActionDispatched",
                        "pickerValidationMalformedInputEventObserved",
                        "pickerValidationMalformedChildUnparsableEventObserved",
                        "pickerValidationMalformedParentUnparsableEventObserved",
                        "pickerValidationJavaMalformedValidationObserved",
                        "pickerValidationMalformedInvalidPatchObserved",
                        "pickerValidationMalformedErrorMessagePatchObserved",
                        "pickerValidationMalformedChildInvalidPatchObserved",
                        "pickerValidationFinalRecoveryActionDispatched",
                        "pickerValidationFinalRecoveryBrowserChangeObserved",
                        "pickerValidationFinalJavaRecoveryObserved",
                        "pickerValidationFinalValuePatchObserved",
                        "pickerValidationFinalValidPatchObserved",
                        "pickerValidationFinalErrorClearedObserved",
                        "pickerValidationFinalChildValidPatchObserved",
                        "pickerValidationOverlaysClosedObserved"),
                        "pickerValidationInvalid",
                        "pickerValidationDatePickerInvalid",
                        "pickerValidationTimePickerInvalid"),
                "pickerValidationValue", "2026-08-12T13:30"),
                "pickerValidationMin", "2026-08-12T12:00"),
                "pickerValidationMax", "2026-08-12T14:00"),
                "pickerValidationErrorMessage", ""),
                "pickerValidationJavaStatus",
                "Picker validation: value=2026-08-12T13:30 invalid=false "
                        + "error=none client=true change=2");
        pickerValidationAcceptance = withText(
                pickerValidationAcceptance,
                "pickerValidationJavaValidationStatus",
                "Picker validation event: valid=false invalid=true "
                        + "error=Picker validation value is malformed change=2");
        trace.append(protocol(
                "pickerValidation.frameworkAcceptance",
                pickerValidationAcceptance));
        trace.append(protocol("flowMenuPopup.frameworkAcceptance", withNumber(
                withNumber(withNumber(allTrue(
                "complete", "standardBundleMode", "rootRendered",
                "flowClientOwned", "opaqueRequestBridgeObserved",
                "opaqueResponseBridgeObserved", "pushObserved", "errorFree",
                "flowMenuPopupDedicatedRouteObserved",
                "flowMenuPopupBrowserProgrammaticNavigationObserved",
                "flowMenuPopupNativeKeyboardObserved",
                "flowMenuPopupNativeKeyboardTrustedObserved",
                "flowMenuPopupNativePointerObserved",
                "flowMenuPopupNativePointerTrustedObserved",
                "flowMenuPopupMenuOpenedObserved",
                "flowMenuPopupMenuA11yObserved",
                "flowMenuPopupMenuOwnerFocusWhileOpenObserved",
                "flowMenuPopupMenuExpandedButtonMatchesOwner",
                "flowMenuPopupMenuInitialFocusObserved",
                "flowMenuPopupMenuKeyboardMoveObserved",
                "flowMenuPopupMenuSelectionObserved",
                "flowMenuPopupMenuJavaListenerObserved",
                "flowMenuPopupMenuClosedObserved",
                "flowMenuPopupMenuFocusRestoredObserved",
                "flowMenuPopupMenuEscapeOpenedObserved",
                "flowMenuPopupMenuEscapeCloseObserved",
                "flowMenuPopupNestedRootOpenedObserved",
                "flowMenuPopupNestedRootInitialFocusObserved",
                "flowMenuPopupNestedFirstMoveObserved",
                "flowMenuPopupNestedParentFocusObserved",
                "flowMenuPopupNestedChildOpenedObserved",
                "flowMenuPopupNestedA11yObserved",
                "flowMenuPopupNestedInitialFocusObserved",
                "flowMenuPopupNestedKeyboardMoveObserved",
                "flowMenuPopupNestedLeftReturnObserved",
                "flowMenuPopupNestedParentRemainsOpenObserved",
                "flowMenuPopupNestedParentFocusRestoredObserved",
                "flowMenuPopupNestedReopenedObserved",
                "flowMenuPopupNestedReopenedInitialFocusObserved",
                "flowMenuPopupNestedSelectionMoveObserved",
                "flowMenuPopupNestedSelectionObserved",
                "flowMenuPopupNestedJavaListenerObserved",
                "flowMenuPopupNestedClosedObserved",
                "flowMenuPopupNestedFocusRestoredObserved",
                "flowMenuPopupNestedEscapeRootOpenedObserved",
                "flowMenuPopupNestedEscapeFirstMoveObserved",
                "flowMenuPopupNestedEscapeParentFocusObserved",
                "flowMenuPopupNestedEscapeChildOpenedObserved",
                "flowMenuPopupNestedEscapeChildCloseObserved",
                "flowMenuPopupNestedEscapeParentRemainsOpenObserved",
                "flowMenuPopupNestedEscapeParentFocusRestoredObserved",
                "flowMenuPopupNestedEscapeRootCloseObserved",
                "flowMenuPopupNestedEscapeOwnerFocusRestoredObserved",
                "flowMenuPopupOverflowNarrowObserved",
                "flowMenuPopupOverflowOpenedObserved",
                "flowMenuPopupOverflowA11yObserved",
                "flowMenuPopupOverflowInitialFocusObserved",
                "flowMenuPopupOverflowSelectionObserved",
                "flowMenuPopupOverflowJavaListenerObserved",
                "flowMenuPopupOverflowClosedObserved",
                "flowMenuPopupOverflowFocusRestoredObserved",
                "flowMenuPopupOverflowEscapeOpenedObserved",
                "flowMenuPopupOverflowEscapeInitialFocusObserved",
                "flowMenuPopupOverflowEscapeCloseObserved",
                "flowMenuPopupOverflowEscapeFocusRestoredObserved",
                "flowMenuPopupOverflowNestedCollapsedParentObserved",
                "flowMenuPopupOverflowNestedRootOpenedObserved",
                "flowMenuPopupOverflowNestedRootInitialFocusObserved",
                "flowMenuPopupOverflowNestedParentFocusObserved",
                "flowMenuPopupOverflowNestedChildOpenedObserved",
                "flowMenuPopupOverflowNestedA11yObserved",
                "flowMenuPopupOverflowNestedInitialFocusObserved",
                "flowMenuPopupOverflowNestedKeyboardMoveObserved",
                "flowMenuPopupOverflowNestedLeftReturnObserved",
                "flowMenuPopupOverflowNestedParentRemainsOpenObserved",
                "flowMenuPopupOverflowNestedParentFocusRestoredObserved",
                "flowMenuPopupOverflowNestedReopenedObserved",
                "flowMenuPopupOverflowNestedReopenedInitialFocusObserved",
                "flowMenuPopupOverflowNestedSelectionMoveObserved",
                "flowMenuPopupOverflowNestedSelectionObserved",
                "flowMenuPopupOverflowNestedJavaListenerObserved",
                "flowMenuPopupOverflowNestedClosedObserved",
                "flowMenuPopupOverflowNestedFocusRestoredObserved",
                "flowMenuPopupOverflowNestedEscapeRootOpenedObserved",
                "flowMenuPopupOverflowNestedEscapeRootInitialFocusObserved",
                "flowMenuPopupOverflowNestedEscapeParentFocusObserved",
                "flowMenuPopupOverflowNestedEscapeChildOpenedObserved",
                "flowMenuPopupOverflowNestedEscapeChildCloseObserved",
                "flowMenuPopupOverflowNestedEscapeParentRemainsOpenObserved",
                "flowMenuPopupOverflowNestedEscapeParentFocusRestoredObserved",
                "flowMenuPopupOverflowNestedEscapeRootCloseObserved",
                "flowMenuPopupOverflowNestedEscapeOwnerFocusRestoredObserved",
                "flowMenuPopupOverflowExpandedObserved",
                "flowMenuPopupOverflowItemsRestoredObserved",
                "flowMenuPopupContextOpenedObserved",
                "flowMenuPopupContextA11yObserved",
                "flowMenuPopupContextInitialFocusObserved",
                "flowMenuPopupContextKeyboardMoveObserved",
                "flowMenuPopupContextSelectionObserved",
                "flowMenuPopupContextJavaListenerObserved",
                "flowMenuPopupContextClosedObserved",
                "flowMenuPopupContextFocusRestoredObserved",
                "flowMenuPopupContextEscapeOpenedObserved",
                "flowMenuPopupContextEscapeCloseObserved",
                "flowMenuPopupContextJavaOpenObserved",
                "flowMenuPopupContextJavaCloseObserved",
                "flowMenuPopupContextNestedRootOpenedObserved",
                "flowMenuPopupContextNestedRootInitialFocusObserved",
                "flowMenuPopupContextNestedJavaRootOpenObserved",
                "flowMenuPopupContextNestedFirstMoveObserved",
                "flowMenuPopupContextNestedParentFocusObserved",
                "flowMenuPopupContextNestedChildOpenedObserved",
                "flowMenuPopupContextNestedA11yObserved",
                "flowMenuPopupContextNestedInitialFocusObserved",
                "flowMenuPopupContextNestedKeyboardMoveObserved",
                "flowMenuPopupContextNestedLeftReturnObserved",
                "flowMenuPopupContextNestedParentRemainsOpenObserved",
                "flowMenuPopupContextNestedParentFocusRestoredObserved",
                "flowMenuPopupContextNestedReopenedObserved",
                "flowMenuPopupContextNestedReopenedInitialFocusObserved",
                "flowMenuPopupContextNestedSelectionMoveObserved",
                "flowMenuPopupContextNestedSelectionObserved",
                "flowMenuPopupContextNestedJavaListenerObserved",
                "flowMenuPopupContextNestedJavaRootCloseObserved",
                "flowMenuPopupContextNestedClosedObserved",
                "flowMenuPopupContextNestedFocusRestoredObserved",
                "flowMenuPopupContextNestedEscapeRootOpenedObserved",
                "flowMenuPopupContextNestedEscapeRootInitialFocusObserved",
                "flowMenuPopupContextNestedEscapeJavaRootOpenObserved",
                "flowMenuPopupContextNestedEscapeFirstMoveObserved",
                "flowMenuPopupContextNestedEscapeParentFocusObserved",
                "flowMenuPopupContextNestedEscapeChildOpenedObserved",
                "flowMenuPopupContextNestedEscapeChildCloseObserved",
                "flowMenuPopupContextNestedEscapeParentRemainsOpenObserved",
                "flowMenuPopupContextNestedEscapeParentFocusRestoredObserved",
                "flowMenuPopupContextNestedEscapeRootCloseObserved",
                "flowMenuPopupContextNestedEscapeJavaRootCloseObserved",
                "flowMenuPopupContextNestedEscapeOwnerFocusRestoredObserved",
                "flowMenuPopupGridOpenedObserved",
                "flowMenuPopupGridVirtualizedTargetObserved",
                "flowMenuPopupGridRowContextObserved",
                "flowMenuPopupGridColumnContextObserved",
                "flowMenuPopupGridInitialFocusObserved",
                "flowMenuPopupGridKeyboardMoveObserved",
                "flowMenuPopupGridSelectionObserved",
                "flowMenuPopupGridJavaOpenObserved",
                "flowMenuPopupGridJavaListenerObserved",
                "flowMenuPopupGridJavaCloseObserved",
                "flowMenuPopupGridClosedObserved",
                "flowMenuPopupGridFocusRestoredObserved",
                "flowMenuPopupGridPointerTrustedObserved",
                "flowMenuPopupGridNestedRootOpenedObserved",
                "flowMenuPopupGridNestedRootInitialFocusObserved",
                "flowMenuPopupGridNestedJavaRootOpenObserved",
                "flowMenuPopupGridNestedContextObserved",
                "flowMenuPopupGridNestedFirstMoveObserved",
                "flowMenuPopupGridNestedParentFocusObserved",
                "flowMenuPopupGridNestedChildOpenedObserved",
                "flowMenuPopupGridNestedA11yObserved",
                "flowMenuPopupGridNestedInitialFocusObserved",
                "flowMenuPopupGridNestedKeyboardMoveObserved",
                "flowMenuPopupGridNestedLeftReturnObserved",
                "flowMenuPopupGridNestedParentRemainsOpenObserved",
                "flowMenuPopupGridNestedParentFocusRestoredObserved",
                "flowMenuPopupGridNestedContextRetainedObserved",
                "flowMenuPopupGridNestedReopenedObserved",
                "flowMenuPopupGridNestedReopenedInitialFocusObserved",
                "flowMenuPopupGridNestedSelectionMoveObserved",
                "flowMenuPopupGridNestedSelectionObserved",
                "flowMenuPopupGridNestedJavaListenerObserved",
                "flowMenuPopupGridNestedJavaRootCloseObserved",
                "flowMenuPopupGridNestedClosedObserved",
                "flowMenuPopupGridNestedFocusRestoredObserved",
                "flowMenuPopupGridNestedEscapeRootOpenedObserved",
                "flowMenuPopupGridNestedEscapeRootInitialFocusObserved",
                "flowMenuPopupGridNestedEscapeJavaRootOpenObserved",
                "flowMenuPopupGridNestedEscapeFirstMoveObserved",
                "flowMenuPopupGridNestedEscapeParentFocusObserved",
                "flowMenuPopupGridNestedEscapeChildOpenedObserved",
                "flowMenuPopupGridNestedEscapeChildCloseObserved",
                "flowMenuPopupGridNestedEscapeParentRemainsOpenObserved",
                "flowMenuPopupGridNestedEscapeParentFocusRestoredObserved",
                "flowMenuPopupGridNestedEscapeRootCloseObserved",
                "flowMenuPopupGridNestedEscapeJavaRootCloseObserved",
                "flowMenuPopupGridNestedEscapeOwnerFocusRestoredObserved",
                "flowMenuPopupGridPolicyHeaderPointerTrustedObserved",
                "flowMenuPopupGridPolicyHeaderContextObserved",
                "flowMenuPopupGridPolicyHeaderJavaRejectObserved",
                "flowMenuPopupGridPolicyHeaderClosedObserved",
                "flowMenuPopupGridPolicyEmptyPointerTrustedObserved",
                "flowMenuPopupGridPolicyEmptyGeometryObserved",
                "flowMenuPopupGridPolicyEmptyContextObserved",
                "flowMenuPopupGridPolicyEmptyJavaRejectObserved",
                "flowMenuPopupGridPolicyEmptyClosedObserved",
                "flowMenuPopupGridPolicyRowPointerTrustedObserved",
                "flowMenuPopupGridPolicyRowContextObserved",
                "flowMenuPopupGridPolicyRowOpenedObserved",
                "flowMenuPopupGridPolicyRowInitialFocusObserved",
                "flowMenuPopupGridPolicyDynamicContentObserved",
                "flowMenuPopupGridPolicyJavaOpenObserved",
                "flowMenuPopupGridPolicyJavaListenerObserved",
                "flowMenuPopupGridPolicyJavaCloseObserved",
                "flowMenuPopupGridPolicyClosedObserved",
                "flowMenuPopupGridPolicyFocusRestoredObserved"),
                        "flowMenuPopupNativeKeyboardEventCount", 69),
                        "flowMenuPopupNativePointerEventCount", 10),
                        "flowMenuPopupGridRenderedItemCount", 15)));
        trace.append(protocol("flowOverlayStack.frameworkAcceptance", allTrue(
                "complete", "standardBundleMode", "rootRendered",
                "flowClientOwned", "opaqueRequestBridgeObserved",
                "opaqueResponseBridgeObserved", "pushObserved", "errorFree",
                "flowOverlayStackDedicatedRouteObserved",
                "flowOverlayStackBrowserProgrammaticInputObserved",
                "flowOverlayStackDialogOpenedObserved",
                "flowOverlayStackPopoverOpenedObserved",
                "flowOverlayStackConfirmOpenedObserved",
                "flowOverlayStackThreeLevelOpenedObserved",
                "flowOverlayStackA11yObserved",
                "flowOverlayStackConfirmInitialFocusObserved",
                "flowOverlayStackTopEscapeObserved",
                "flowOverlayStackParentLayersRemainObserved",
                "flowOverlayStackConfirmFocusRestoredObserved",
                "flowOverlayStackPopoverOutsideCloseObserved",
                "flowOverlayStackDialogRemainsObserved",
                "flowOverlayStackOutsideFocusPreservedObserved",
                "flowOverlayStackDialogEscapeObserved",
                "flowOverlayStackLayeredFocusRestoredObserved")));
        trace.append(protocol("structuredRpc.frameworkAcceptance", withFalse(
                allTrue(
                        "complete", "standardBundleMode", "rootRendered",
                        "flowClientOwned", "opaqueRequestBridgeObserved",
                        "opaqueResponseBridgeObserved", "pushObserved",
                        "errorFree",
                        "structuredRpcClientCallableInputObserved",
                        "structuredRpcClientCallableResultObserved",
                        "structuredRpcClientCallableTypeShapeInputObserved",
                        "structuredRpcClientCallableTypeShapeResultObserved",
                        "structuredRpcPendingJavaScriptObserved",
                        "structuredRpcPendingJavaScriptTypeShapesObserved",
                        "structuredRpcCancellationObserved",
                        "structuredRpcSentExecutionObserved",
                        "structuredRpcSentCancellationObserved",
                        "structuredRpcDisabledDefaultBlockedObserved",
                        "structuredRpcDisabledAlwaysAllowedObserved",
                        "structuredRpcInvisibleOwnerDeferredObserved",
                        "structuredRpcInvisibleOwnerResolvedObserved",
                        "structuredRpcInvisibleOwnerSideEffectExecuted",
                        "structuredRpcDetachedOwnerQueuedObserved",
                        "structuredRpcDetachedOwnerDetachedObserved",
                        "structuredRpcDetachedOwnerReattachedObserved",
                        "structuredRpcDetachedOwnerCleanupObserved",
                        "structuredRpcFutureSuccessObserved",
                        "structuredRpcFutureDeadlockGuardObserved",
                        "structuredRpcFutureSuccessSideEffectExecuted",
                        "structuredRpcFutureFailureObserved",
                        "structuredRpcMultipleSubscriptionsObserved",
                        "structuredRpcLateSubscriptionSentObserved",
                        "structuredRpcLateSubscriptionRejectedObserved",
                        "structuredRpcFutureCancellationObserved",
                        "structuredRpcInertDefaultBlockedObserved",
                        "structuredRpcInertAllowedObserved",
                        "structuredRpcInertRecoveryObserved"),
                "structuredRpcCanceledSideEffectExecuted",
                "structuredRpcDetachedOwnerSideEffectExecuted",
                "structuredRpcFutureCanceledSideEffectExecuted",
                "structuredRpcInertDefaultSettled")));
        trace.append(protocol("recv", "opaquePush", """
                {"connectionId":"opaque-push-1","windowId":"main",
                "sessionId":"flow-ipc-session","uiId":"main","flowUiId":0,
                "resync":false,"pushSequence":5,"flowServerSyncId":5,
                "deliveryAttempt":1,"uidlBytes":173}
                """));
        trace.append(unacknowledgedPushBoundary());
        trace.append(protocol("acceptance.sidecarFailureInjected", """
                {"processIdentifier":101,"reason":"unacknowledged-push-replacement",
                "signal":9}
                """));
        trace.append(protocol("sidecar.exit", """
                {"generation":1,"processIdentifier":101,"status":9,
                "expected":false}
                """));
        trace.append(protocol("sidecar.recoveryScheduled",
                "{\"attempt\":1,\"delayMillis\":500}"));
        trace.append(protocol("sidecar.started", """
                {"generation":2,"processIdentifier":202,"transport":"flow-ipc"}
                """));
        trace.append(protocol("sidecar.recoveryReady",
                "{\"attempt\":1,\"processIdentifier\":202,\"recoveryGeneration\":1}"));
        trace.append(protocol("sidecar.recoveryStabilityDeadline",
                "{\"attempts\":1,\"stableRunSeconds\":2}"));
        trace.append(protocol("acceptance.sidecarFailureInjected",
                "{\"processIdentifier\":202,\"signal\":9}"));
        trace.append(protocol("sidecar.exit", """
                {"generation":2,"processIdentifier":202,"status":9,
                "expected":false}
                """));
        trace.append(protocol("sidecar.recoveryScheduled",
                "{\"attempt\":2,\"delayMillis\":1000}"));
        trace.append(protocol("sidecar.started", """
                {"generation":3,"processIdentifier":303,"transport":"flow-ipc"}
                """));
        trace.append(protocol("sidecar.recoveryReady",
                "{\"attempt\":2,\"processIdentifier\":303}"));
        trace.append(protocol("sidecar.recoveryStabilityDeadline",
                "{\"attempts\":2,\"stableRunSeconds\":2}"));
        trace.append(protocol("sidecar.recoveryBudgetReset",
                "{\"previousAttempts\":2,\"processIdentifier\":303,"
                        + "\"stableRunSeconds\":2}"));
        trace.append(protocol("acceptance.sidecarFailureInjected",
                "{\"processIdentifier\":303,\"signal\":9}"));
        trace.append(protocol("sidecar.exit", """
                {"generation":3,"processIdentifier":303,"status":9,
                "expected":false}
                """));
        trace.append(protocol("sidecar.recoveryScheduled",
                "{\"attempt\":1,\"delayMillis\":500}"));
        trace.append(protocol("sidecar.started", """
                {"generation":4,"processIdentifier":404,"transport":"flow-ipc"}
                """));
        trace.append(protocol("sidecar.recoveryReady",
                "{\"attempt\":1,\"processIdentifier\":404}"));
        trace.append(protocol("sidecar.recoveryStabilityDeadline",
                "{\"attempts\":1,\"stableRunSeconds\":2}"));
        trace.append(protocol("standardBundle.sidecarRestartAcceptance", """
                {"complete":true,"standardBundleMode":true,"errorFree":true,
                "processRestarted":true,"windowPreserved":true,"webViewPreserved":true,
                "webViewReloaded":true,"serverUiStateReset":true,
                "requestBridgeRecovered":true,"pushRecovered":true,
                "unacknowledgedPushObservedBeforeReplacement":true,
                "unacknowledgedPushUnackedBeforeReplacement":true,
                "stalePushNotReplayed":true,
                "freshPushSequenceAfterReplacement":true,
                "unacknowledgedPushSequence":5,
                "unacknowledgedPushFlowServerSyncId":5,
                "firstRecoveredPushMessageCount":2,
                "firstRecoveredPushAckCount":2,
                "firstRecoveredPushFirstSequence":1,
                "firstRecoveredPushLastSequence":2,
                "interactionRecovered":true,"rapidFailuresRecovered":true,
                "stableBudgetResetExercised":true,"recoveryGeneration":3,
                "processReplacementCount":3,
                "previousProcessIdentifier":101,"processIdentifier":404}
                """));
        trace.append(protocol("shutdown.sidecar", "{}"));
        return trace.toString();
    }

    private static String unacknowledgedPushBoundary() {
        return protocol(
                "standardBundle.sidecarUnacknowledgedPushBoundary", """
                {"observed":true,"unacknowledged":true,
                "processIdentifier":101,"recoveryGeneration":0,
                "connectionId":"opaque-push-1","windowId":"main",
                "sessionId":"flow-ipc-session","uiId":"main","flowUiId":0,
                "pushSequence":5,"flowServerSyncId":5,"deliveryAttempt":1,
                "initialAckCount":4,"ackCount":4}
                """);
    }

    private static String validRequestFanoutTrace() {
        StringBuilder trace = new StringBuilder(validTrace());
        int[] initialLengths = {
                4194341, 4194387, 4194443,
                4194501, 4194555, 4194611,
                2097511, 2097571, 2097639,
                2097709, 2097783, 2097853
        };
        int[] lengths = java.util.stream.IntStream.range(0, 60)
                .map(index -> index < initialLengths.length
                        ? initialLengths[index]
                        : 256 * 1024 + index * 73 + 17)
                .toArray();
        int[] cancelThresholds = java.util.stream.IntStream.range(0, 60)
                .map(index -> index < initialLengths.length
                        ? 512 * 1024 : 192 * 1024)
                .toArray();
        int[] cancelRequestBytes = java.util.stream.IntStream.range(0, 60)
                .map(index -> index < initialLengths.length
                        ? 512 * 1024 : 160 * 1024)
                .toArray();
        java.util.function.IntConsumer open = index ->
            trace.append(protocol("send", "opaqueHttpStreamOpen",
                    "{\"requestId\":\"request-fanout-" + index
                            + "\",\"method\":\"POST\","
                            + "\"headerNames\":\"Content-Type,X-Filename\","
                            + "\"contentLength\":" + lengths[index] + "}"));
        java.util.function.IntConsumer cancel = index -> {
            trace.append(protocol("send", "opaqueHttpStreamChunk",
                    "{\"requestId\":\"request-fanout-" + index
                            + "\",\"chunkSequence\":17,"
                            + "\"bodyBytes\":"
                            + cancelRequestBytes[index] + "}"));
            trace.append(protocol("send", "opaqueHttpStreamCancel",
                    "{\"requestId\":\"request-fanout-" + index
                            + "\"}"));
        };
        java.util.function.IntConsumer complete = index -> {
            int chunkCount = (lengths[index] + 32 * 1024 - 1)
                    / (32 * 1024);
            trace.append(protocol("send", "opaqueHttpStreamChunk",
                    "{\"requestId\":\"request-fanout-" + index
                            + "\",\"chunkSequence\":" + chunkCount + ","
                            + "\"bodyBytes\":" + lengths[index] + "}"));
            trace.append(protocol("send", "opaqueHttpStreamEnd",
                    "{\"requestId\":\"request-fanout-" + index
                            + "\",\"chunkCount\":" + chunkCount + ","
                            + "\"bodyBytes\":" + lengths[index] + "}"));
            trace.append(protocol("recv", "opaqueHttpStreamResponse",
                    "{\"requestId\":\"request-fanout-" + index
                            + "\",\"status\":200}"));
        };

        for (int batch = 0; batch < 10; batch++) {
            int base = batch * 6;
            open.accept(base);
            open.accept(base + 1);
            open.accept(base + 2);
            cancel.accept(base + 1);
            open.accept(base + 3);
            complete.accept(base);
            open.accept(base + 4);
            complete.accept(base + 2);
            open.accept(base + 5);
            cancel.accept(base + 4);
            complete.accept(base + 3);
            complete.accept(base + 5);
        }

        String acceptance = allTrue(
                "complete", "standardBundleMode", "rootRendered",
                "flowClientOwned", "opaqueRequestBridgeObserved",
                "opaqueResponseBridgeObserved", "pushObserved",
                "requestFanoutBrowserOverlapObserved",
                "requestFanoutQueueReplenishmentObserved",
                "requestFanoutBeyondInitialCreditObserved",
                "requestFanoutCancellationObserved",
                "requestFanoutSurvivorsObserved",
                "requestFanoutServerIsolationObserved",
                "requestFanoutExactBytesObserved",
                "requestFanoutJavaListenersObserved",
                "requestFanoutRegistryTurnoverObserved",
                "requestFanoutLateResponseIsolationObserved",
                "requestFanoutCancellationTombstoneExpiryObserved",
                "requestIdsUnique", "cancelledLateFramesIgnored",
                "completedRequestReclaimed",
                "cancellationTombstoneRetained",
                "cancellationTombstoneExpired", "errorFree",
                "allBeyondThreshold");
        acceptance = withNumbers(
                acceptance, "fileLengths", lengths);
        int[] cancelledLengths = java.util.stream.IntStream.range(0, 60)
                .filter(index -> index % 6 == 1 || index % 6 == 4)
                .map(index -> lengths[index])
                .toArray();
        int[] survivorLengths = java.util.stream.IntStream.range(0, 60)
                .filter(index -> index % 6 != 1 && index % 6 != 4)
                .map(index -> lengths[index])
                .toArray();
        int[] progressBytes = java.util.stream.IntStream.range(0, 60)
                .map(index -> index % 6 == 1 || index % 6 == 4
                        ? cancelThresholds[index] : lengths[index])
                .toArray();
        int[] batchEvents = java.util.stream.IntStream.range(0, 10)
                .map(ignored -> 6)
                .toArray();
        acceptance = withNumbers(
                acceptance, "cancelledFileLengths", cancelledLengths);
        acceptance = withNumbers(
                acceptance, "survivorFileLengths", survivorLengths);
        acceptance = withNumbers(
                acceptance, "cancelThresholds", cancelThresholds);
        acceptance = withNumbers(
                acceptance, "progressBytes", progressBytes);
        acceptance = withNumber(acceptance, "batchCount", 10);
        acceptance = withNumber(acceptance, "completedBatches", 10);
        acceptance = withNumbers(
                acceptance, "batchStartEvents", batchEvents);
        acceptance = withNumbers(
                acceptance, "batchTerminalEvents", batchEvents);
        acceptance = withNumber(acceptance, "startEvents", 60);
        acceptance = withNumber(acceptance, "abortEvents", 20);
        acceptance = withNumber(acceptance, "removeEvents", 20);
        acceptance = withNumber(acceptance, "successEvents", 40);
        acceptance = withNumber(acceptance, "errorEvents", 0);
        acceptance = withNumber(
                acceptance, "cancelIssuedCount", 20);
        acceptance = withNumber(
                acceptance, "browserMaximumActive", 3);
        acceptance = withNumber(
                acceptance, "serverMaximumActive", 3);
        acceptance = withNumber(
                acceptance, "maxConcurrentUploads", 3);
        acceptance = withNumber(
                acceptance, "serverCancelledMinimum", 163840);
        acceptance = withNumber(
                acceptance, "serverCancelledMaximum", 524288);
        int expectedSuccessBytes = java.util.Arrays.stream(survivorLengths)
                .sum();
        acceptance = withNumber(
                acceptance, "serverSuccessBytes", expectedSuccessBytes);
        acceptance = withNumber(
                acceptance, "serverExpectedSuccessBytes",
                expectedSuccessBytes);
        String[] requestIds = java.util.stream.IntStream.range(0, 60)
                .mapToObj(index -> "request-fanout-" + index)
                .toArray(String[]::new);
        acceptance = withTexts(acceptance, "requestIds", requestIds);
        acceptance = withTexts(
                acceptance, "firstBatchRequestIds",
                java.util.Arrays.copyOfRange(requestIds, 0, 6));
        acceptance = withTexts(
                acceptance, "secondBatchRequestIds",
                java.util.Arrays.copyOfRange(requestIds, 6, 12));
        acceptance = withNumber(acceptance, "lateFrameProbeCount", 4);
        acceptance = withNumber(
                acceptance, "tombstoneBoundaryProbeCount", 2);
        acceptance = withNumber(
                acceptance, "tombstoneRetentionElapsedMillis", 55000);
        acceptance = withNumber(
                acceptance, "tombstoneExpiryElapsedMillis", 61000);
        acceptance = withText(
                acceptance, "ignoredLateResponseRequestId",
                "request-fanout-1");
        acceptance = withText(
                acceptance, "reclaimedRequestId", "request-fanout-0");
        acceptance = withText(
                acceptance, "registryProbeExpectedError",
                "unknown request request-fanout-0");
        acceptance = withText(
                acceptance, "tombstoneExpiryExpectedError",
                "unknown request request-fanout-1");
        trace.append(protocol(
                "flowRequestFanout.frameworkAcceptance", acceptance));
        return trace.toString();
    }

    private static String validInFlightRequestRecoveryTrace() {
        StringBuilder trace = new StringBuilder();
        trace.append(protocol("launch", "{\"transport\":\"flow-ipc\"}"));
        String ready = """
                {"envelope":{"payload":{
                "runtime":"standard-vaadin-bundle-opaque-ipc",
                "browserRequestTransport":"opaque-http-stream-ipc",
                "transport":"flow-ipc",
                "flowClientResourceLoading":"standard-production-bundle"}}}
                """;
        trace.append(protocol("ready", ready));
        trace.append(protocol("standardBundleReady",
                "{\"requestTransport\":\"opaque-http-stream-ipc\"}"));
        trace.append(protocol("standardBundleResource",
                "{\"path\":\"/VAADIN/build/FlowClient-test.js\"}"));
        trace.append(protocol("webview.firstMeaningfulPaint", "{}"));
        trace.append(protocol("send", "opaqueHttpStreamOpen", """
                {"requestId":"opaque-http-1","url":"/bootstrap",
                "method":"POST","contentLength":0,
                "headerNames":"Content-Type"}
                """));
        trace.append(protocol("send", "opaqueHttpStreamEnd", """
                {"requestId":"opaque-http-1","chunkCount":0,"bodyBytes":0}
                """));
        trace.append(protocol("recv", "opaqueHttpStreamResponse", """
                {"requestId":"opaque-http-1","status":200}
                """));
        trace.append(protocol("recv", "opaqueHttpStreamResponseEnd", """
                {"requestId":"opaque-http-1","chunkCount":0,"bodyBytes":0}
                """));
        trace.append(protocol("send", "opaquePushConnect", """
                {"connectionId":"opaque-push-1","pushSequence":0}
                """));
        trace.append(protocol("recv", "opaquePushConnected", """
                {"connectionId":"opaque-push-1"}
                """));
        trace.append(protocol("recv", "opaquePush", """
                {"connectionId":"opaque-push-1","pushSequence":1,
                "flowServerSyncId":1,"deliveryAttempt":1,"uidlBytes":128}
                """));
        trace.append(protocol("send", "opaqueHttpStreamOpen", """
                {"requestId":"opaque-http-5",
                "url":"/flow-in-flight-request/upload",
                "method":"POST","contentLength":4194813,
                "headerNames":"Content-Type,X-Filename"}
                """));
        trace.append(protocol("send", "opaqueHttpStreamChunk", """
                {"requestId":"opaque-http-5","chunkSequence":7,
                "bodyBytes":262144}
                """));
        trace.append(protocol("acceptance.sidecarFailureInjected", """
                {"reason":"in-flight-opaque-request",
                "processIdentifier":101,"requestId":"opaque-http-5",
                "progressBytes":262144}
                """));
        trace.append(protocol("sidecar.exit", """
                {"processIdentifier":101,"status":9,"expected":false}
                """));
        trace.append(protocol("sidecar.recoveryScheduled", "{\"attempt\":1}"));
        trace.append(protocol("ready", ready));
        trace.append(protocol("sidecar.recoveryReady", """
                {"attempt":1,"previousProcessIdentifier":101,
                "processIdentifier":202,"processReplaced":true,
                "windowsReloaded":1,"recoveryGeneration":1}
                """));
        trace.append(protocol("webview.firstMeaningfulPaint",
                "{\"recoveryGeneration\":1}"));
        trace.append(protocol("send", "opaquePushConnect", """
                {"connectionId":"opaque-push-1","pushSequence":0}
                """));
        trace.append(protocol("recv", "opaquePushConnected", """
                {"connectionId":"opaque-push-1"}
                """));
        trace.append(protocol("recv", "opaquePush", """
                {"connectionId":"opaque-push-1","pushSequence":1,
                "flowServerSyncId":1,"deliveryAttempt":1,"uidlBytes":128}
                """));
        trace.append(protocol("send", "opaqueHttpStreamOpen", """
                {"requestId":"opaque-http-5",
                "url":"/flow-in-flight-request/upload",
                "method":"POST","contentLength":4194813,
                "headerNames":"Content-Type,X-Filename"}
                """));
        trace.append(protocol("send", "opaqueHttpStreamChunk", """
                {"requestId":"opaque-http-5","chunkSequence":128,
                "bodyBytes":4194813}
                """));
        trace.append(protocol("send", "opaqueHttpStreamEnd", """
                {"requestId":"opaque-http-5","chunkCount":129,
                "bodyBytes":4194813}
                """));
        trace.append(protocol("recv", "opaqueHttpStreamResponse", """
                {"requestId":"opaque-http-5","status":200}
                """));
        trace.append(protocol("recv", "opaqueHttpStreamResponseEnd", """
                {"requestId":"opaque-http-5","chunkCount":0,"bodyBytes":0}
                """));
        trace.append(protocol(
                "flowInFlightRequestRecovery.frameworkAcceptance", """
                {"complete":true,"standardBundleMode":true,
                "interruptedRequestInFlight":true,
                "interruptedRequestFailed":true,
                "oldDocumentSidecarDisconnected":true,
                "oldDocumentLateRequestRejected":true,
                "oldDocumentRequestCountStable":true,
                "oldDocumentPushRejected":true,
                "processRestarted":true,"windowPreserved":true,
                "webViewPreserved":true,"webViewReloaded":true,
                "freshFlowClientOwned":true,"requestBridgeRecovered":true,
                "pushRecovered":true,"recoveredRequestComplete":true,
                "recoveredRequestExactBytes":true,
                "javaConfirmationObserved":true,"errorFree":true,
                "totalBytes":4194813,"interruptThreshold":262144,
                "interruptedProgressBytes":262144,
                "disconnectedRequestCount":1,
                "disconnectedPushOutboundCount":2,
                "recoveryGeneration":1,
                "previousProcessIdentifier":101,"processIdentifier":202,
                "interruptedRequestId":"opaque-http-5",
                "recoveredRequestId":"opaque-http-5"}
                """));
        trace.append(protocol("shutdown.sidecar", "{}"));
        return trace.toString();
    }

    private static String validConcurrentMultipartTrace() {
        StringBuilder trace = new StringBuilder(validTrace().replace(
                "/VAADIN/dynamic/resource/0/test/upload",
                "/VAADIN/dynamic/resource/0/test/not-multipart"));
        trace.append(protocol("send", "opaqueHttpStreamOpen", """
                {"requestId":"concurrent-multipart-cancel","method":"POST",
                "url":"https://flow-ipc.invalid/VAADIN/dynamic/resource/0/test/upload",
                "headerNames":"Content-Type","contentLength":-1}
                """));
        trace.append(protocol("send", "opaqueHttpStreamOpen", """
                {"requestId":"concurrent-multipart-survivor","method":"POST",
                "url":"https://flow-ipc.invalid/VAADIN/dynamic/resource/0/test/upload",
                "headerNames":"Content-Type","contentLength":-1}
                """));
        trace.append(protocol("send", "opaqueHttpStreamChunk", """
                {"requestId":"concurrent-multipart-cancel",
                "chunkSequence":9,"bodyBytes":262333}
                """));
        trace.append(protocol("send", "opaqueHttpStreamChunk", """
                {"requestId":"concurrent-multipart-survivor",
                "chunkSequence":97,"bodyBytes":3146206}
                """));
        trace.append(protocol("send", "opaqueHttpStreamCancel", """
                {"requestId":"concurrent-multipart-cancel"}
                """));
        trace.append(protocol("send", "opaqueHttpStreamEnd", """
                {"requestId":"concurrent-multipart-survivor",
                "chunkCount":97,"bodyBytes":3146206}
                """));
        trace.append(protocol("recv", "opaqueHttpStreamResponse", """
                {"requestId":"concurrent-multipart-survivor","status":200}
                """));

        String acceptance = allTrue(
                "complete", "standardBundleMode", "rootRendered",
                "flowClientOwned", "opaqueRequestBridgeObserved",
                "opaqueResponseBridgeObserved", "pushObserved",
                "concurrentMultipartUploadRequestObserved",
                "concurrentMultipartUploadBrowserOverlapObserved",
                "concurrentMultipartUploadProgressObserved",
                "concurrentMultipartUploadAbortObserved",
                "concurrentMultipartUploadRemovalObserved",
                "concurrentMultipartUploadSurvivorObserved",
                "concurrentMultipartUploadSessionSerializationObserved",
                "concurrentMultipartUploadCancellationIsolationObserved",
                "concurrentMultipartUploadExactBytesObserved",
                "concurrentMultipartUploadDiskSpillObserved",
                "concurrentMultipartUploadTemporaryPartCleanupObserved",
                "concurrentMultipartUploadJavaListenersObserved",
                "errorFree", "serverDiskSpillObserved",
                "serverTemporaryPartCleanupObserved");
        acceptance = withNumber(
                acceptance, "cancelledTotalBytes", 4194477);
        acceptance = withNumber(
                acceptance, "survivorTotalBytes", 3145985);
        acceptance = withNumber(
                acceptance, "cancelThreshold", 262144);
        acceptance = withNumber(
                acceptance, "overlapThreshold", 98304);
        acceptance = withNumber(
                acceptance, "cancelledProgressBytes", 262333);
        acceptance = withNumber(
                acceptance, "survivorProgressBytes", 3146206);
        acceptance = withNumber(acceptance, "progressEvents", 18);
        acceptance = withNumber(acceptance, "startEvents", 2);
        acceptance = withNumber(acceptance, "requestEvents", 2);
        acceptance = withNumber(
                acceptance, "multipartRequestEvents", 2);
        acceptance = withNumber(
                acceptance, "multipartEntryEvents", 2);
        acceptance = withNumber(acceptance, "abortEvents", 1);
        acceptance = withNumber(acceptance, "removeEvents", 1);
        acceptance = withNumber(acceptance, "successEvents", 1);
        acceptance = withNumber(acceptance, "errorEvents", 0);
        acceptance = withNumber(
                acceptance, "browserMaximumActive", 2);
        acceptance = withNumber(
                acceptance, "serverMaximumTemporaryDirectories", 1);
        acceptance = withNumber(
                acceptance, "serverMaximumTemporaryFiles", 1);
        acceptance = withNumber(
                acceptance, "serverHandlerCalls", 1);
        acceptance = withNumber(
                acceptance, "serverHandlerFailures", 0);
        acceptance = withNumber(
                acceptance, "serverResponseCalls", 1);
        acceptance = withNumber(
                acceptance, "serverFailedResponses", 0);
        acceptance = withNumber(
                acceptance, "serverSuccessfulResponses", 1);
        acceptance = withNumber(
                acceptance, "serverSuccessfulAcceptedFiles", 1);
        acceptance = withNumber(
                acceptance, "serverSuccessBytes", 3145985);
        acceptance = withNumber(
                acceptance, "serverSuccessDeclaredBytes", 3145985);
        acceptance = withNumber(
                acceptance, "serverSpillBytes", 3145985);
        acceptance = withText(
                acceptance,
                "serverHandlerFile",
                "flow-ipc-concurrent-multipart-survivor.bin");
        acceptance = withText(
                acceptance, "serverFailedHandlerFile", "");
        acceptance = withText(
                acceptance,
                "serverSuccessfulAcceptedFile",
                "flow-ipc-concurrent-multipart-survivor.bin");
        acceptance = withText(
                acceptance, "serverMonitorError", "");
        trace.append(protocol(
                "flowConcurrentMultipartUpload.frameworkAcceptance",
                acceptance));
        return trace.toString();
    }

    private static String validCrossSessionMultipartTrace() {
        StringBuilder trace = new StringBuilder(validTrace().replace(
                "/VAADIN/dynamic/resource/0/test/upload",
                "/VAADIN/dynamic/resource/0/test/not-multipart"));
        trace.append(protocol("send", "opaqueHttpStreamOpen", """
                {"requestId":"cross-session-multipart","method":"POST",
                "url":"https://flow-ipc.invalid/VAADIN/dynamic/resource/0/test/upload",
                "headerNames":"Content-Type","contentLength":-1,
                "windowId":"main","sessionId":"flow-ipc-session",
                "uiId":"main"}
                """));
        trace.append(protocol("send", "opaqueHttpStreamOpen", """
                {"requestId":"cross-session-multipart","method":"POST",
                "url":"https://flow-ipc.invalid/VAADIN/dynamic/resource/0/test/upload",
                "headerNames":"Content-Type","contentLength":-1,
                "windowId":"multipart-secondary",
                "sessionId":"flow-ipc-session-secondary",
                "uiId":"multipart-secondary"}
                """));
        trace.append(protocol("send", "opaqueHttpStreamChunk", """
                {"requestId":"cross-session-multipart",
                "bodyBytes":524517,"windowId":"main",
                "sessionId":"flow-ipc-session","uiId":"main"}
                """));
        trace.append(protocol("send", "opaqueHttpStreamChunk", """
                {"requestId":"cross-session-multipart",
                "bodyBytes":10486253,"windowId":"multipart-secondary",
                "sessionId":"flow-ipc-session-secondary",
                "uiId":"multipart-secondary"}
                """));
        trace.append(protocol("send", "opaqueHttpStreamCancel", """
                {"requestId":"cross-session-multipart","windowId":"main",
                "sessionId":"flow-ipc-session","uiId":"main"}
                """));
        trace.append(protocol("send", "opaqueHttpStreamEnd", """
                {"requestId":"cross-session-multipart",
                "bodyBytes":10486253,"windowId":"multipart-secondary",
                "sessionId":"flow-ipc-session-secondary",
                "uiId":"multipart-secondary"}
                """));
        trace.append(protocol("recv", "opaqueHttpStreamResponse", """
                {"requestId":"cross-session-multipart","status":200,
                "windowId":"multipart-secondary",
                "sessionId":"flow-ipc-session-secondary",
                "uiId":"multipart-secondary"}
                """));

        String acceptance = allTrue(
                "complete", "standardBundleMode", "rootRendered",
                "flowClientOwned", "opaqueRequestBridgeObserved",
                "opaqueResponseBridgeObserved", "pushObserved",
                "crossSessionMultipartUploadIdentityIsolationObserved",
                "crossSessionMultipartUploadRequestObserved",
                "crossSessionMultipartUploadTransportOverlapObserved",
                "crossSessionMultipartUploadParserConcurrencyObserved",
                "crossSessionMultipartUploadCancellationIsolationObserved",
                "crossSessionMultipartUploadSurvivorObserved",
                "crossSessionMultipartUploadExactBytesObserved",
                "crossSessionMultipartUploadDiskSpillObserved",
                "crossSessionMultipartUploadTemporaryPartCleanupObserved",
                "crossSessionMultipartUploadJavaListenersObserved",
                "crossWindowProgressOverlapObserved", "errorFree",
                "mainServerTemporaryPartCleanupObserved",
                "secondaryServerTemporaryPartCleanupObserved",
                "secondaryServerPatternValid",
                "secondaryServerDiskSpillObserved");
        acceptance = withText(acceptance, "mainWindowId", "main");
        acceptance = withText(
                acceptance, "mainSessionId", "flow-ipc-session");
        acceptance = withText(acceptance, "mainUiId", "main");
        acceptance = withText(
                acceptance, "secondaryWindowId", "multipart-secondary");
        acceptance = withText(
                acceptance,
                "secondarySessionId",
                "flow-ipc-session-secondary");
        acceptance = withText(
                acceptance, "secondaryUiId", "multipart-secondary");
        acceptance = withNumber(
                acceptance, "cancelledTotalBytes", 12583093);
        acceptance = withNumber(
                acceptance, "survivorTotalBytes", 10486023);
        acceptance = withNumber(
                acceptance, "overlapThreshold", 524288);
        acceptance = withNumber(
                acceptance, "mainTotalBytes", 12583093);
        acceptance = withNumber(
                acceptance, "secondaryTotalBytes", 10486023);
        acceptance = withNumber(
                acceptance, "mainProgressBytes", 524517);
        acceptance = withNumber(
                acceptance, "secondaryProgressBytes", 10486253);
        for (String field : new String[]{
                "mainStartEvents", "mainRequestEvents",
                "mainMultipartRequestEvents", "mainMultipartEntryEvents",
                "mainAbortEvents", "mainRemoveEvents",
                "secondaryStartEvents", "secondaryRequestEvents",
                "secondaryMultipartRequestEvents",
                "secondaryMultipartEntryEvents", "secondarySuccessEvents",
                "secondaryServerHandlerCalls",
                "secondaryServerResponseCalls",
                "secondaryServerSuccessfulResponses",
                "secondaryServerAcceptedFiles"}) {
            acceptance = withNumber(acceptance, field, 1);
        }
        acceptance = withNumber(acceptance, "mainProgressEvents", 17);
        acceptance = withNumber(
                acceptance, "secondaryProgressEvents", 321);
        for (String field : new String[]{
                "mainSuccessEvents", "mainErrorEvents",
                "secondaryAbortEvents", "secondaryRemoveEvents",
                "secondaryErrorEvents", "mainServerHandlerCalls",
                "mainServerHandlerFailures", "mainServerResponseCalls",
                "mainServerFailedResponses",
                "secondaryServerHandlerFailures",
                "secondaryServerFailedResponses"}) {
            acceptance = withNumber(acceptance, field, 0);
        }
        acceptance = withNumber(
                acceptance, "maximumTemporaryDirectories", 2);
        acceptance = withNumber(
                acceptance, "maximumTemporaryFiles", 2);
        acceptance = withNumber(
                acceptance, "secondaryServerSuccessBytes", 10486023);
        acceptance = withNumber(
                acceptance, "secondaryServerDeclaredBytes", 10486023);
        acceptance = withNumber(
                acceptance, "secondaryServerSpillBytes", 10486023);
        acceptance = withText(
                acceptance,
                "mainAbortFileName",
                "flow-ipc-cross-session-multipart-cancel.bin");
        acceptance = withText(
                acceptance,
                "mainRemovedFileName",
                "flow-ipc-cross-session-multipart-cancel.bin");
        acceptance = withText(
                acceptance,
                "secondarySuccessFileName",
                "flow-ipc-cross-session-multipart-survivor.bin");
        acceptance = withText(
                acceptance, "mainServerHandlerFile", "");
        acceptance = withText(
                acceptance, "mainServerFailedHandlerFile", "");
        acceptance = withText(
                acceptance, "mainServerMonitorError", "");
        acceptance = withText(
                acceptance,
                "secondaryServerHandlerFile",
                "flow-ipc-cross-session-multipart-survivor.bin");
        acceptance = withText(
                acceptance, "secondaryServerFailedHandlerFile", "");
        acceptance = withText(
                acceptance,
                "secondaryServerAcceptedFile",
                "flow-ipc-cross-session-multipart-survivor.bin");
        acceptance = withText(
                acceptance, "secondaryServerMonitorError", "");
        trace.append(protocol(
                "flowCrossSessionMultipartUpload.frameworkAcceptance",
                acceptance));
        return trace.toString();
    }

    private static String validViteTrace() {
        StringBuilder trace = new StringBuilder();
        trace.append(protocol("launch", "{\"transport\":\"flow-ipc\"}"));
        trace.append(protocol("ready", """
                {"envelope":{"payload":{"runtime":"standard-vaadin-bundle-opaque-ipc",
                "browserRequestTransport":"opaque-http-stream-ipc",
                "transport":"flow-ipc",
                "flowClientResourceLoading":"vite-development-server"}}}
                """));
        trace.append(protocol("flowWebAppReady", """
                {"mode":"development-server",
                "requestTransport":"opaque-http-stream-ipc"}
                """));
        trace.append(developmentResource("/@vite/client"));
        trace.append(developmentResource("/generated/vite-devmode.ts"));
        trace.append(developmentResource("/generated/vaadin.ts"));
        trace.append(developmentResource("/generated/jar-resources/FlowClient.js"));
        trace.append(developmentResource("/vatori-vite-hmr-probe.js"));
        trace.append(developmentResource("/vatori-vite-hmr-probe.js"));
        trace.append(protocol("webview.firstMeaningfulPaint",
                "{\"source\":\"vite-development-server\"}"));
        trace.append(protocol("opaqueHttpStreamOpen", "{}"));
        trace.append(protocol("opaqueHttpStreamOpen", "{}"));
        trace.append(protocol("opaqueHttpStreamResponse", "{\"status\":200}"));
        trace.append(protocol("opaqueHttpStreamResponse",
                "{\"envelope\":{\"payload\":{\"status\":200}}}"));
        trace.append(protocol("opaqueHttpStreamResponseEnd", "{}"));
        trace.append(protocol("opaqueHttpStreamResponseEnd", "{}"));
        trace.append(protocol("opaquePushConnected", "{}"));
        trace.append(protocol("opaquePush", "{}"));
        trace.append(protocol("viteDevelopment.frameworkAcceptance",
                allTrue(
                        "complete", "viteConnected", "baselineObserved",
                        "updatedObserved", "sourceRestored", "flowClientOwned",
                        "opaqueRequestBridgeObserved", "opaqueResponseBridgeObserved",
                        "errorFree").replace("}", ",\"mode\":\"development-server\"}")));
        trace.append(protocol("shutdown.sidecar", "{}"));
        return trace.toString();
    }

    private static String validRecoveryExhaustionTrace() {
        StringBuilder trace = new StringBuilder();
        trace.append(protocol("launch", "{\"transport\":\"flow-ipc\"}"));
        trace.append(protocol("window.create", "{\"label\":\"main\"}"));
        trace.append(protocol("ready", """
                {"envelope":{"payload":{
                "runtime":"standard-vaadin-bundle-opaque-ipc",
                "browserRequestTransport":"opaque-http-stream-ipc",
                "transport":"flow-ipc",
                "flowClientResourceLoading":"standard-production-bundle"}}}
                """));
        trace.append(protocol("standardBundleReady",
                "{\"requestTransport\":\"opaque-http-stream-ipc\"}"));
        trace.append(protocol("webview.firstMeaningfulPaint", "{}"));
        trace.append(protocol("acceptance.sidecarFailureInjected",
                "{\"processIdentifier\":101,\"signal\":9}"));
        trace.append(protocol("sidecar.recoveryScheduled", "{\"attempt\":1}"));
        trace.append(protocol("sidecar.recoveryReady",
                "{\"attempt\":1,\"processIdentifier\":202}"));
        trace.append(protocol("acceptance.sidecarFailureInjected",
                "{\"processIdentifier\":202,\"signal\":9}"));
        trace.append(protocol("sidecar.recoveryScheduled", "{\"attempt\":2}"));
        trace.append(protocol("sidecar.recoveryReady",
                "{\"attempt\":2,\"processIdentifier\":303}"));
        trace.append(protocol(
                "standardBundle.sidecarRecoveryExhaustionAcceptance", """
                {"armed":true,"rapidFailuresRecovered":true,
                "recoveryGeneration":2,"processRestarted":true,
                "windowPreserved":true,"webViewPreserved":true}
                """));
        trace.append(protocol("acceptance.sidecarFailureInjected",
                "{\"processIdentifier\":303,\"signal\":9}"));
        trace.append(protocol("error", """
                {"code":"sidecarRecoveryExhausted","attempts":2,
                "restartLimit":2}
                """));
        return trace.toString();
    }

    private static String validJavaReloadTrace() {
        StringBuilder trace = new StringBuilder();
        trace.append(protocol("launch", "{\"transport\":\"flow-ipc\"}"));
        String ready = """
                {"envelope":{"payload":{
                "runtime":"standard-vaadin-bundle-opaque-ipc",
                "browserRequestTransport":"opaque-http-stream-ipc",
                "transport":"flow-ipc",
                "flowClientResourceLoading":"vite-development-server"}}}
                """;
        trace.append(protocol("ready", ready));
        trace.append(protocol("flowWebAppReady", """
                {"mode":"development-server",
                "requestTransport":"opaque-http-stream-ipc"}
                """));
        trace.append(developmentResource("/@vite/client"));
        trace.append(developmentResource("/generated/vite-devmode.ts"));
        trace.append(developmentResource("/generated/vaadin.ts"));
        trace.append(developmentResource(
                "/generated/jar-resources/FlowClient.js"));
        trace.append(protocol("webview.firstMeaningfulPaint",
                "{\"source\":\"vite-development-server\"}"));
        appendOpaqueRuntime(trace);
        trace.append(protocol("hostControl.received", """
                {"type":"replaceSidecar",
                "reason":"development-inputs-rebuilt"}
                """));
        trace.append(protocol("sidecar.replacementRequested", """
                {"reason":"development-inputs-rebuilt",
                "processIdentifier":101,"windowCount":1}
                """));
        trace.append(protocol("ready", ready));
        trace.append(protocol("sidecar.replacementReady", """
                {"reason":"development-inputs-rebuilt",
                "previousProcessIdentifier":101,
                "processIdentifier":202,
                "processReplaced":true,
                "windowsReloaded":1,
                "recoveryGeneration":1}
                """));
        trace.append(protocol("webview.firstMeaningfulPaint",
                "{\"source\":\"vite-development-server\"}"));
        appendOpaqueRuntime(trace);
        trace.append(protocol("javaReload.frameworkAcceptance", """
                {"complete":true,
                "baselineObserved":true,
                "updatedObserved":true,
                "sourceRestored":true,
                "processReplaced":true,
                "previousProcessIdentifier":101,
                "processIdentifier":202,
                "windowPreserved":true,
                "webViewPreserved":true,
                "webViewReloaded":true,
                "recoveryGeneration":1,
                "flowClientOwned":true,
                "requestBridgeRecovered":true,
                "pushRecovered":true,
                "errorFree":true,
                "mode":"development-server"}
                """));
        trace.append(protocol("shutdown.sidecar", "{}"));
        return trace.toString();
    }

    private static String validReactorReloadTrace() {
        StringBuilder trace = new StringBuilder();
        trace.append(protocol(
                "launch", "{\"transport\":\"flow-ipc\"}"));
        String ready = """
                {"envelope":{"payload":{
                "runtime":"standard-vaadin-bundle-opaque-ipc",
                "browserRequestTransport":"opaque-http-stream-ipc",
                "transport":"flow-ipc",
                "flowClientResourceLoading":"vite-development-server"}}}
                """;
        trace.append(protocol("ready", ready));
        trace.append(protocol("flowWebAppReady", """
                {"mode":"development-server",
                "requestTransport":"opaque-http-stream-ipc"}
                """));
        trace.append(developmentResource("/@vite/client"));
        trace.append(developmentResource(
                "/generated/vite-devmode.ts"));
        trace.append(developmentResource("/generated/vaadin.ts"));
        trace.append(developmentResource(
                "/generated/jar-resources/FlowClient.js"));
        trace.append(protocol("webview.firstMeaningfulPaint",
                "{\"source\":\"vite-development-server\"}"));
        appendOpaqueRuntime(trace);
        int previousProcessIdentifier = 301;
        for (int generation = 1; generation <= 3; generation++) {
            int processIdentifier = 301 + generation;
            trace.append(protocol("hostControl.received", """
                    {"type":"replaceSidecar",
                    "reason":"development-inputs-rebuilt"}
                    """));
            trace.append(protocol("sidecar.replacementRequested", """
                    {"reason":"development-inputs-rebuilt",
                    "processIdentifier":%d,"windowCount":1}
                    """.formatted(previousProcessIdentifier)));
            trace.append(protocol("ready", ready));
            trace.append(protocol("sidecar.replacementReady", """
                    {"reason":"development-inputs-rebuilt",
                    "previousProcessIdentifier":%d,
                    "processIdentifier":%d,
                    "processReplaced":true,
                    "windowsReloaded":1,
                    "recoveryGeneration":%d}
                    """.formatted(
                    previousProcessIdentifier,
                    processIdentifier,
                    generation)));
            trace.append(protocol("webview.firstMeaningfulPaint",
                    "{\"source\":\"vite-development-server\"}"));
            appendOpaqueRuntime(trace);
            previousProcessIdentifier = processIdentifier;
        }
        trace.append(protocol(
                "reactorReload.frameworkAcceptance", """
                {"complete":true,
                "baselineObserved":true,
                "updatedObserved":true,
                "configurationUpdatedObserved":true,
                "sourceUpdatedObserved":true,
                "resourceUpdatedObserved":true,
                "configurationRestored":true,
                "sourceRestored":true,
                "resourceRestored":true,
                "buildConfigurationObserved":true,
                "dependentModuleObserved":true,
                "dependentResourceObserved":true,
                "effectiveMavenModelObserved":true,
                "profileActivatedModuleObserved":true,
                "customSourceRootObserved":true,
                "customResourceRootObserved":true,
                "processReplaced":true,
                "previousProcessIdentifier":301,
                "processIdentifier":304,
                "windowPreserved":true,
                "webViewPreserved":true,
                "webViewReloaded":true,
                "recoveryGeneration":3,
                "replacementCount":3,
                "flowClientOwned":true,
                "requestBridgeRecovered":true,
                "pushRecovered":true,
                "errorFree":true,
                "mode":"development-server"}
                """));
        trace.append(protocol("shutdown.sidecar", "{}"));
        return trace.toString();
    }

    private static String validJavaRollbackTrace() {
        StringBuilder trace = new StringBuilder();
        trace.append(protocol(
                "launch", "{\"transport\":\"flow-ipc\"}"));
        String ready = """
                {"envelope":{"payload":{
                "runtime":"standard-vaadin-bundle-opaque-ipc",
                "browserRequestTransport":"opaque-http-stream-ipc",
                "transport":"flow-ipc",
                "flowClientResourceLoading":"vite-development-server"}}}
                """;
        trace.append(protocol("ready", ready));
        trace.append(protocol("flowWebAppReady", """
                {"mode":"development-server",
                "requestTransport":"opaque-http-stream-ipc"}
                """));
        trace.append(developmentResource("/@vite/client"));
        trace.append(developmentResource(
                "/generated/vite-devmode.ts"));
        trace.append(developmentResource(
                "/generated/vaadin.ts"));
        trace.append(developmentResource(
                "/generated/jar-resources/FlowClient.js"));
        trace.append(protocol(
                "webview.firstMeaningfulPaint",
                "{\"source\":\"vite-development-server\"}"));
        appendOpaqueRuntime(trace);
        trace.append(protocol("hostControl.received", """
                {"type":"replaceSidecar",
                "requestId":"java-reload-1",
                "reason":"development-inputs-rebuilt"}
                """));
        trace.append(protocol("sidecar.replacementRequested", """
                {"requestId":"java-reload-1",
                "reason":"development-inputs-rebuilt",
                "processIdentifier":101,"windowCount":1}
                """));
        trace.append(protocol("sidecar.replacementFailed", """
                {"requestId":"java-reload-1",
                "reason":"development-inputs-rebuilt",
                "cause":"candidate-exited-before-ready",
                "processIdentifier":202,"status":42}
                """));
        trace.append(protocol("hostControl.received", """
                {"type":"rollbackSidecar",
                "requestId":"java-reload-1",
                "reason":"replacement-candidate-failed"}
                """));
        trace.append(protocol("ready", ready));
        trace.append(protocol("sidecar.rollbackReady", """
                {"requestId":"java-reload-1",
                "reason":"development-inputs-rebuilt",
                "replacementFailure":"candidate-exited-before-ready",
                "previousProcessIdentifier":101,
                "processIdentifier":303,
                "processReplaced":true,
                "windowsReloaded":1,
                "recoveryGeneration":1}
                """));
        trace.append(protocol(
                "webview.firstMeaningfulPaint",
                "{\"source\":\"vite-development-server\"}"));
        appendOpaqueRuntime(trace);
        trace.append(protocol(
                "javaRollback.frameworkAcceptance", """
                {"complete":true,
                "baselineObserved":true,
                "failingCandidateApplied":true,
                "stableRuntimeRestored":true,
                "sourceRestored":true,
                "processReplaced":true,
                "previousProcessIdentifier":101,
                "processIdentifier":303,
                "windowPreserved":true,
                "webViewPreserved":true,
                "webViewReloaded":true,
                "recoveryGeneration":1,
                "flowClientOwned":true,
                "requestBridgeRecovered":true,
                "pushRecovered":true,
                "errorFree":true,
                "mode":"development-server"}
                """));
        trace.append(protocol("shutdown.sidecar", "{}"));
        return trace.toString();
    }

    private static void appendOpaqueRuntime(StringBuilder trace) {
        trace.append(protocol("opaqueHttpStreamOpen", "{}"));
        trace.append(protocol("opaqueHttpStreamOpen", "{}"));
        trace.append(protocol("opaqueHttpStreamResponse",
                "{\"status\":200}"));
        trace.append(protocol("opaqueHttpStreamResponse",
                "{\"envelope\":{\"payload\":{\"status\":200}}}"));
        trace.append(protocol("opaqueHttpStreamResponseEnd", "{}"));
        trace.append(protocol("opaqueHttpStreamResponseEnd", "{}"));
        trace.append(protocol("opaquePushConnected", "{}"));
        trace.append(protocol("opaquePush", "{}"));
    }

    private static String validStartupFailureTrace() {
        StringBuilder trace = new StringBuilder();
        trace.append(protocol("launch", "{\"transport\":\"flow-ipc\"}"));
        trace.append(protocol("launch.sidecar", """
                {"generation":1,"transport":"flow-ipc"}
                """));
        trace.append(protocol("sidecar.started", """
                {"generation":1,"processIdentifier":101,"transport":"flow-ipc"}
                """));
        trace.append(protocol("sidecar.exit", """
                {"generation":1,"processIdentifier":101,"status":1,
                "expected":false}
                """));
        trace.append(protocol("error", """
                {"code":"sidecarUnexpectedExit","generation":1,
                "processIdentifier":101,"status":1,"recoverySupported":false}
                """));
        return trace.toString();
    }

    private static String validReadyTimeoutTrace() {
        StringBuilder trace = new StringBuilder();
        trace.append(protocol("launch", "{\"transport\":\"flow-ipc\"}"));
        trace.append(protocol("launch.sidecar", """
                {"generation":1,"transport":"flow-ipc"}
                """));
        trace.append(protocol("sidecar.started", """
                {"generation":1,"processIdentifier":101,"transport":"flow-ipc"}
                """));
        trace.append(protocol("sidecar.initialReadyDeadline", """
                {"processIdentifier":101,"timeoutSeconds":1}
                """));
        trace.append(protocol("error", """
                {"code":"sidecarInitialReadyTimeout","processIdentifier":101,
                "timeoutSeconds":1}
                """));
        return trace.toString();
    }

    private static String developmentResource(String path) {
        return protocol("developmentWebAppResource",
                "{\"path\":\"" + path + "\",\"status\":200}");
    }

    private static String allTrue(String... fields) {
        StringBuilder json = new StringBuilder("{");
        for (int index = 0; index < fields.length; index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append('"').append(fields[index]).append("\":true");
        }
        return json.append('}').toString();
    }

    private static String withFalse(String json, String... fields) {
        StringBuilder result = new StringBuilder(
                json.substring(0, json.length() - 1));
        for (String field : fields) {
            result.append(',').append('"').append(field)
                    .append("\":false");
        }
        return result.append('}').toString();
    }

    private static String withNumber(
            String json,
            String field,
            int value) {
        return json.substring(0, json.length() - 1)
                + ",\"" + field + "\":" + value + "}";
    }

    private static String withNumbers(
            String json,
            String field,
            int... values) {
        StringBuilder result = new StringBuilder(
                json.substring(0, json.length() - 1));
        result.append(',').append('"').append(field).append("\":[");
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                result.append(',');
            }
            result.append(values[index]);
        }
        return result.append("]}").toString();
    }

    private static String withText(
            String json,
            String field,
            String value) {
        String escaped = value.replace("\\", "\\\\")
                .replace("\"", "\\\"");
        return json.substring(0, json.length() - 1)
                + ",\"" + field + "\":\"" + escaped + "\"}";
    }

    private static String withTexts(
            String json,
            String field,
            String... values) {
        StringBuilder result = new StringBuilder(
                json.substring(0, json.length() - 1));
        result.append(',').append('"').append(field).append("\":[");
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                result.append(',');
            }
            result.append('"').append(values[index].replace("\\", "\\\\")
                    .replace("\"", "\\\"")).append('"');
        }
        return result.append("]}").toString();
    }

    private static String protocol(String type, String payload) {
        return "HOST_PROTOCOL {\"type\":\"" + type + "\",\"payload\":"
                + payload.replaceAll("\\s+", "") + "}\n";
    }

    private static String protocol(String direction, String type, String payload) {
        return "HOST_PROTOCOL {\"direction\":\"" + direction
                + "\",\"type\":\"" + type + "\",\"payload\":"
                + payload.replaceAll("\\s+", "") + "}\n";
    }
}
