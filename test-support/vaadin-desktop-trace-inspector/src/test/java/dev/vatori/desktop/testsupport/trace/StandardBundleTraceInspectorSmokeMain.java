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
                    "--require-flow-ipc-standard-bundle-sidecar-recovery",
                    trace.toString()
            }, new PrintStream(output), new PrintStream(errors));
            String summary = output.toString(StandardCharsets.UTF_8);
            if (exitCode != 0 || !errors.toString(StandardCharsets.UTF_8).isEmpty()
                    || !summary.contains("PASS --require-flow-ipc-standard-bundle-runtime")
                    || !summary.contains("PASS --require-flow-ipc-standard-bundle-framework-app")
                    || !summary.contains("PASS --require-flow-ipc-standard-bundle-sidecar-recovery")) {
                throw new IllegalStateException(
                        "Standard bundle inspector rejected a valid trace:\n" + summary
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
        } finally {
            Files.deleteIfExists(trace);
        }
        System.out.println("Standard bundle trace inspector smoke passed");
    }

    private static String validTrace() {
        StringBuilder trace = new StringBuilder();
        trace.append(protocol("launch", """
                {"transport":"flow-ipc","standardBundleMode":true,"compatibilityMode":false}
                """));
        trace.append(protocol("ready", """
                {"envelope":{"payload":{"runtime":"standard-vaadin-bundle-opaque-ipc",
                "browserRequestTransport":"opaque-http-stream-ipc",
                "standardBundleMode":true,"compatibilityMode":false}}}
                """));
        trace.append(protocol("standardBundleReady",
                "{\"requestTransport\":\"opaque-http-stream-ipc\"}"));
        trace.append(protocol("standardBundleResource",
                "{\"path\":\"/VAADIN/build/FlowClient-test.js\"}"));
        trace.append(protocol("webview.firstMeaningfulPaint", "{}"));
        trace.append(protocol("opaqueHttpStreamOpen", "{}"));
        trace.append(protocol("opaqueHttpStreamOpen", "{}"));
        trace.append(protocol("opaqueHttpStreamResponse", "{\"status\":200}"));
        trace.append(protocol("opaqueHttpStreamResponse",
                "{\"envelope\":{\"payload\":{\"status\":200}}}"));
        trace.append(protocol("opaqueHttpStreamResponseEnd", "{}"));
        trace.append(protocol("opaqueHttpStreamResponseEnd", "{}"));
        trace.append(protocol("opaquePushConnected", "{}"));
        trace.append(protocol("opaquePush", "{}"));
        trace.append(protocol("standardBundle.frameworkAcceptance", allTrue(
                "complete", "standardBundleMode", "rootRendered", "flowClientOwned",
                "opaqueRequestBridgeObserved", "opaqueResponseBridgeObserved",
                "pushRecoveryObserved", "errorFree", "budgetEventObserved",
                "checkboxEventObserved", "selectEventObserved", "datePickerEventObserved",
                "dialogEventObserved", "formValidationObserved", "applyEventObserved",
                "serverStateUpdated", "nativeWindowTitleApplied", "nativeClipboardApplied")));
        trace.append(protocol("standardBundle.sidecarRestartAcceptance", """
                {"complete":true,"standardBundleMode":true,"errorFree":true,
                "processRestarted":true,"windowPreserved":true,"webViewPreserved":true,
                "webViewReloaded":true,"serverUiStateReset":true,
                "requestBridgeRecovered":true,"pushRecovered":true,
                "interactionRecovered":true,"recoveryGeneration":1,
                "previousProcessIdentifier":101,"processIdentifier":202}
                """));
        trace.append(protocol("shutdown.sidecar", "{}"));
        return trace.toString();
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

    private static String protocol(String type, String payload) {
        return "HOST_PROTOCOL {\"type\":\"" + type + "\",\"payload\":"
                + payload.replaceAll("\\s+", "") + "}\n";
    }
}
