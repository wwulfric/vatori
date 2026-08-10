package dev.vatori.desktop.testsupport.trace;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Inspects standard production and Vite-development Flow IPC runtimes. */
public final class StandardBundleTraceInspector {
    private static final String HOST_PROTOCOL_PREFIX = "HOST_PROTOCOL ";
    private static final ObjectMapper JSON = JsonMapper.builder().build();

    private StandardBundleTraceInspector() {
    }

    public static void main(String[] args) {
        String[] effectiveArgs = args.length == 0 ? environmentArguments() : args;
        int exitCode = run(effectiveArgs, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    private static String[] environmentArguments() {
        if ("1".equals(System.getenv("VATORI_TRACE_INSPECT_HELP"))) {
            return new String[]{"--help"};
        }
        List<String> arguments = new ArrayList<>();
        String gates = System.getenv("VATORI_TRACE_INSPECT_GATES");
        if (gates != null && !gates.isBlank()) {
            for (String gate : gates.split(",")) {
                if (!gate.isBlank()) {
                    arguments.add(gate);
                }
            }
        }
        String trace = System.getenv("VATORI_TRACE_INSPECT_FILE");
        if (trace != null && !trace.isBlank()) {
            arguments.add(trace);
        }
        return arguments.toArray(String[]::new);
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length == 1 && "--help".equals(args[0])) {
            printHelp(out);
            return 0;
        }

        Set<Gate> gates = EnumSet.noneOf(Gate.class);
        Path trace = null;
        for (String argument : args) {
            Gate gate = Gate.forOption(argument);
            if (gate != null) {
                gates.add(gate);
            } else if (argument.startsWith("--")) {
                err.println("Unknown option: " + argument);
                printHelp(err);
                return 2;
            } else if (trace == null) {
                trace = Path.of(argument);
            } else {
                err.println("Only one trace file may be inspected at a time.");
                return 2;
            }
        }
        if (trace == null) {
            printHelp(err);
            return 2;
        }

        TraceFacts facts;
        try {
            facts = read(trace);
        } catch (IOException | IllegalArgumentException error) {
            err.println("Unable to inspect " + trace + ": " + error.getMessage());
            return 1;
        }

        out.printf("Trace: %s%n", trace);
        out.printf("HOST_PROTOCOL messages: %d%n", facts.messageCount);
        out.printf("Types: %s%n", facts.typeCounts);
        if (gates.isEmpty()) {
            out.println("No acceptance gate requested.");
            return 0;
        }

        List<String> failures = new ArrayList<>();
        for (Gate gate : gates) {
            List<String> missing = gate.missing(facts);
            if (missing.isEmpty()) {
                out.println("PASS " + gate.option);
            } else {
                failures.add(gate.option + ": " + String.join(", ", missing));
            }
        }
        failures.forEach(failure -> err.println("FAIL " + failure));
        return failures.isEmpty() ? 0 : 2;
    }

    private static TraceFacts read(Path trace) throws IOException {
        TraceFacts facts = new TraceFacts();
        int lineNumber = 0;
        for (String line : Files.readAllLines(trace)) {
            lineNumber++;
            if (!line.startsWith(HOST_PROTOCOL_PREFIX)) {
                continue;
            }
            JsonNode root;
            try {
                root = JSON.readTree(line.substring(HOST_PROTOCOL_PREFIX.length()));
            } catch (JacksonException error) {
                throw new IllegalArgumentException(
                        "invalid HOST_PROTOCOL JSON at line " + lineNumber, error);
            }
            if (root == null || !root.isObject()) {
                throw new IllegalArgumentException(
                        "HOST_PROTOCOL line " + lineNumber + " is not an object");
            }
            facts.accept(root);
        }
        if (facts.messageCount == 0) {
            throw new IllegalArgumentException("no HOST_PROTOCOL messages found");
        }
        return facts;
    }

    private static void printHelp(PrintStream out) {
        out.println("Usage: flow-ipc-trace-inspect [gate ...] <host.out.log>");
        out.println("Gates:");
        for (Gate gate : Gate.values()) {
            out.printf("  %-62s %s%n", gate.option, gate.description);
        }
    }

    private enum Gate {
        RUNTIME(
                "--require-flow-ipc-standard-bundle-runtime",
                "packaged bundle, opaque requests, push and clean shutdown"),
        VITE_DEVELOPMENT(
                "--require-flow-ipc-vite-development-runtime",
                "Vite resource graph, HMR, opaque Flow requests and clean shutdown"),
        JAVA_RELOAD_DEVELOPMENT(
                "--require-flow-ipc-java-reload-development",
                "Java rebuild, planned sidecar replacement and retained native window"),
        REACTOR_RELOAD_DEVELOPMENT(
                "--require-flow-ipc-reactor-reload-development",
                "effective Maven reactor, custom roots and profile reload"),
        JAVA_RELOAD_ROLLBACK_DEVELOPMENT(
                "--require-flow-ipc-java-reload-rollback-development",
                "failed Java replacement rollback to the last ready sidecar"),
        FRAMEWORK(
                "--require-flow-ipc-standard-bundle-framework-app",
                "representative Flow component and native capability workflow"),
        LAZY_GRID_QUERY(
                "--require-flow-ipc-lazy-grid-query",
                "lazy DataProvider viewport, selection, sorting and filtering"),
        PUSH_RECOVERY(
                "--require-flow-ipc-push-recovery",
                "scoped push reconnect, gap resync and post-recovery delivery"),
        PUSH_DELIVERY(
                "--require-flow-ipc-push-delivery",
                "acknowledged same-sequence retry of a terminal in-flight push"),
        LARGE_DOWNLOAD_RESUME(
                "--require-flow-ipc-large-download-resume",
                "large DownloadHandler cancellation and HTTP Range resume"),
        LARGE_UPLOAD_RETRY(
                "--require-flow-ipc-large-upload-retry",
                "large UploadHandler progress, cancellation and re-selection retry"),
        IN_FLIGHT_REQUEST_RECOVERY(
                "--require-flow-ipc-in-flight-request-recovery",
                "in-flight opaque request failure and fresh-session recovery"),
        LARGE_MULTIPART_UPLOAD_RETRY(
                "--require-flow-ipc-large-multipart-upload-retry",
                "large multipart Upload parsing cancellation and re-selection retry"),
        CONCURRENT_UPLOAD(
                "--require-flow-ipc-concurrent-upload",
                "same-window concurrent Upload request isolation and cancellation"),
        REQUEST_FANOUT(
                "--require-flow-ipc-request-fanout",
                "same-window request fan-out, credit and cancellation isolation"),
        CONCURRENT_MULTIPART_UPLOAD(
                "--require-flow-ipc-concurrent-multipart-upload",
                "concurrent multipart transport with session-locked parsing"),
        CROSS_SESSION_MULTIPART_UPLOAD(
                "--require-flow-ipc-cross-session-multipart-upload",
                "cross-session multipart parser concurrency and cancellation isolation"),
        FLOW_DND(
                "--require-flow-ipc-flow-dnd",
                "Flow DragSource and DropTarget event, data and effect transport"),
        FLOW_INPUT_POPUP(
                "--require-flow-ipc-flow-input-popup",
                "owner-bound input popup keyboard, focus and accessibility"),
        LOCALIZED_INPUT_POPUP(
                "--require-flow-ipc-localized-input-popup",
                "localized owner-bound time input popup and Java value round-trip"),
        PICKER_VALIDATION(
                "--require-flow-ipc-picker-validation",
                "bounded picker Java validation, invalid patch and recovery"),
        FLOW_MENU_POPUP(
                "--require-flow-ipc-flow-menu-popup",
                "owner-bound menu popup native input, focus and accessibility"),
        FLOW_OVERLAY_STACK(
                "--require-flow-ipc-flow-overlay-stack",
                "three-level heterogeneous overlay ownership and focus"),
        STRUCTURED_RPC(
                "--require-flow-ipc-structured-rpc",
                "structured ClientCallable and PendingJavaScriptResult transport"),
        SIDECAR_RECOVERY(
                "--require-flow-ipc-standard-bundle-sidecar-recovery",
                "crash-loop budget reset with retained native window and WebView"),
        SIDECAR_RECOVERY_EXHAUSTION(
                "--require-flow-ipc-sidecar-recovery-exhaustion",
                "fatal host exit after the consecutive-failure budget is exhausted"),
        HOST_STARTUP_FAILURE(
                "--require-flow-ipc-host-startup-failure",
                "fatal sidecar failure before ready without creating a window"),
        HOST_READY_TIMEOUT(
                "--require-flow-ipc-host-ready-timeout",
                "live sidecar initial-ready timeout without creating a window");

        private final String option;
        private final String description;

        Gate(String option, String description) {
            this.option = option;
            this.description = description;
        }

        private static Gate forOption(String option) {
            for (Gate gate : values()) {
                if (gate.option.equals(option)) {
                    return gate;
                }
            }
            return null;
        }

        private List<String> missing(TraceFacts facts) {
            if (this == VITE_DEVELOPMENT) {
                return facts.viteDevelopmentMissing();
            }
            if (this == JAVA_RELOAD_DEVELOPMENT) {
                return facts.javaReloadDevelopmentMissing();
            }
            if (this == REACTOR_RELOAD_DEVELOPMENT) {
                return facts.reactorReloadDevelopmentMissing();
            }
            if (this == JAVA_RELOAD_ROLLBACK_DEVELOPMENT) {
                return facts.javaReloadRollbackDevelopmentMissing();
            }
            if (this == HOST_STARTUP_FAILURE) {
                return facts.hostStartupFailureMissing();
            }
            if (this == HOST_READY_TIMEOUT) {
                return facts.hostReadyTimeoutMissing();
            }
            if (this == SIDECAR_RECOVERY_EXHAUSTION) {
                return facts.sidecarRecoveryExhaustionMissing();
            }
            List<String> missing = facts.runtimeMissing();
            if (this == FRAMEWORK) {
                facts.requireFramework(missing);
            } else if (this == LAZY_GRID_QUERY) {
                facts.requireLazyGridQuery(missing);
            } else if (this == PUSH_RECOVERY) {
                facts.requirePushRecovery(missing);
            } else if (this == PUSH_DELIVERY) {
                facts.requirePushDelivery(missing);
            } else if (this == LARGE_DOWNLOAD_RESUME) {
                facts.requireLargeDownloadResume(missing);
            } else if (this == LARGE_UPLOAD_RETRY) {
                facts.requireLargeUploadRetry(missing);
            } else if (this == IN_FLIGHT_REQUEST_RECOVERY) {
                facts.requireInFlightRequestRecovery(missing);
            } else if (this == LARGE_MULTIPART_UPLOAD_RETRY) {
                facts.requireLargeMultipartUploadRetry(missing);
            } else if (this == CONCURRENT_UPLOAD) {
                facts.requireConcurrentUpload(missing);
            } else if (this == REQUEST_FANOUT) {
                facts.requireRequestFanout(missing);
            } else if (this == CONCURRENT_MULTIPART_UPLOAD) {
                facts.requireConcurrentMultipartUpload(missing);
            } else if (this == CROSS_SESSION_MULTIPART_UPLOAD) {
                facts.requireCrossSessionMultipartUpload(missing);
            } else if (this == FLOW_DND) {
                facts.requireFlowDnd(missing);
            } else if (this == FLOW_INPUT_POPUP) {
                facts.requireFlowInputPopup(missing);
            } else if (this == LOCALIZED_INPUT_POPUP) {
                facts.requireLocalizedInputPopup(missing);
            } else if (this == PICKER_VALIDATION) {
                facts.requirePickerValidation(missing);
            } else if (this == FLOW_MENU_POPUP) {
                facts.requireFlowMenuPopup(missing);
            } else if (this == FLOW_OVERLAY_STACK) {
                facts.requireFlowOverlayStack(missing);
            } else if (this == STRUCTURED_RPC) {
                facts.requireStructuredRpc(missing);
            } else if (this == SIDECAR_RECOVERY) {
                facts.requireRecovery(missing);
            }
            return missing;
        }
    }

    private static final class TraceFacts {
        private final Map<String, Integer> typeCounts = new LinkedHashMap<>();
        private int messageCount;
        private boolean standardLaunch;
        private boolean standardJavaReady;
        private boolean viteJavaReady;
        private int viteJavaReadyCount;
        private boolean standardBundleReady;
        private boolean standardFlowClientResource;
        private boolean viteWebAppReady;
        private boolean viteClientResource;
        private boolean viteDevModeResource;
        private boolean viteVaadinResource;
        private boolean viteFlowClientResource;
        private int viteProbeResourceCount;
        private boolean viteResourceFailure;
        private int standardLazyFrontendChunkResourceCount;
        private boolean standardLazyTransitiveFrontendResource;
        private boolean firstMeaningfulPaint;
        private boolean viteFirstMeaningfulPaint;
        private boolean shutdown;
        private boolean errorObserved;
        private int opaqueRequestCount;
        private int opaqueResponseCount;
        private int opaqueResponseEndCount;
        private int opaqueResponseCancelCount;
        private int opaqueRangeRequestCount;
        private int opaquePartialResponseCount;
        private int opaqueLargeResponseEndCount;
        private int opaqueLargeRequestCount;
        private int opaqueLargeRequestEndCount;
        private final Set<String> opaqueSuccessfulResponseRequestIds =
                new java.util.HashSet<>();
        private final Set<String> opaqueCancelledRequestIds =
                new java.util.HashSet<>();
        private final Set<String> opaqueRangeRequestIds =
                new java.util.HashSet<>();
        private final Set<String> opaquePartialResponseRequestIds =
                new java.util.HashSet<>();
        private final Set<String> opaqueLargeResponseEndRequestIds =
                new java.util.HashSet<>();
        private final Set<String> opaqueLargeRequestIds =
                new java.util.HashSet<>();
        private final Set<String> opaqueRawUploadRequestIds =
                new java.util.HashSet<>();
        private final Set<String> opaqueLargeRequestEndIds =
                new java.util.HashSet<>();
        private final Set<String> opaqueRawRequestIds =
                new java.util.HashSet<>();
        private final Set<String> opaqueMultipartUploadRequestIds =
                new java.util.HashSet<>();
        private final Map<String, Long> opaqueRequestContentLengths =
                new LinkedHashMap<>();
        private final Map<String, Long> opaqueRequestChunkBytes =
                new LinkedHashMap<>();
        private final Map<String, Long> opaqueRequestEndBodyBytes =
                new LinkedHashMap<>();
        private final Map<String, Integer> opaqueRequestOpenOrders =
                new LinkedHashMap<>();
        private final Map<String, Integer> opaqueRequestEndOrders =
                new LinkedHashMap<>();
        private final Map<String, Integer> opaqueRequestCancelOrders =
                new LinkedHashMap<>();
        private final Map<String, Integer> opaqueResponseStatuses =
                new LinkedHashMap<>();
        private final Set<ScopedRequestKey> scopedMultipartUploadRequestKeys =
                new java.util.HashSet<>();
        private final Set<ScopedRequestKey> scopedCancelledRequestKeys =
                new java.util.HashSet<>();
        private final Set<ScopedRequestKey> scopedSuccessfulResponseKeys =
                new java.util.HashSet<>();
        private final Map<ScopedRequestKey, Long> scopedRequestChunkBytes =
                new LinkedHashMap<>();
        private final Map<ScopedRequestKey, Long> scopedRequestEndBodyBytes =
                new LinkedHashMap<>();
        private final Map<ScopedRequestKey, Integer> scopedRequestOpenOrders =
                new LinkedHashMap<>();
        private final Map<ScopedRequestKey, Integer> scopedRequestEndOrders =
                new LinkedHashMap<>();
        private final Map<ScopedRequestKey, Integer> scopedRequestCancelOrders =
                new LinkedHashMap<>();
        private final Map<ScopedRequestKey, Integer> scopedResponseStatuses =
                new LinkedHashMap<>();
        private int pushConnectCount;
        private int pushConnectedCount;
        private int pushCount;
        private int pushDisconnectCount;
        private int pushResyncRequestCount;
        private int pushResyncResponseCount;
        private boolean pushInitialMessageObserved;
        private boolean pushDisconnectAfterMessage;
        private boolean pushReconnectAfterDisconnect;
        private boolean pushResyncRequestAfterReconnect;
        private boolean pushResyncResponseAfterRequest;
        private boolean pushAfterResync;
        private boolean pushRecoveryConnectionConsistent = true;
        private String pushRecoveryScope = "";
        private int initialPushLastSeenServerSyncId = -1;
        private int reconnectLastSeenServerSyncId = -1;
        private long initialPushSequence = -1;
        private long resyncPushSequence = -1;
        private long postResyncPushSequence = -1;
        private final Map<SidecarPushKey, PushDeliveryTrace> pushDeliveries =
                new LinkedHashMap<>();
        private int currentSidecarGeneration;
        private int currentSidecarProcessIdentifier;
        private int pushAcknowledgementCount;
        private boolean pushSameSequenceRedelivery;
        private boolean pushRedeliveryAcknowledged;
        private boolean pushDeliveryScopeConsistent = true;
        private long pushRedeliverySequence = -1;
        private boolean sidecarLaunchAttempted;
        private boolean fatalSidecarStartupFailure;
        private boolean initialReadyDeadline;
        private boolean initialReadyTimeout;
        private boolean windowCreated;
        private JsonNode frameworkAcceptance;
        private JsonNode pushRecoveryAcceptance;
        private JsonNode pushDeliveryAcceptance;
        private JsonNode largeDownloadAcceptance;
        private JsonNode largeUploadAcceptance;
        private JsonNode inFlightRequestRecoveryAcceptance;
        private JsonNode largeMultipartUploadAcceptance;
        private JsonNode concurrentUploadAcceptance;
        private JsonNode requestFanoutAcceptance;
        private JsonNode concurrentMultipartUploadAcceptance;
        private JsonNode crossSessionMultipartUploadAcceptance;
        private JsonNode lazyGridQueryAcceptance;
        private JsonNode flowDndAcceptance;
        private JsonNode flowInputPopupAcceptance;
        private JsonNode localizedInputPopupAcceptance;
        private JsonNode pickerValidationAcceptance;
        private JsonNode flowMenuPopupAcceptance;
        private JsonNode flowOverlayStackAcceptance;
        private JsonNode structuredRpcAcceptance;
        private JsonNode viteAcceptance;
        private JsonNode javaReloadAcceptance;
        private JsonNode reactorReloadAcceptance;
        private JsonNode javaRollbackAcceptance;
        private JsonNode recoveryAcceptance;
        private JsonNode sidecarUnacknowledgedPushBoundary;
        private boolean sidecarUnacknowledgedPushTraceCorrelated;
        private boolean sidecarUnacknowledgedPushProcessCorrelated;
        private int sidecarUnacknowledgedPushBoundaryOrder;
        private int sidecarUnacknowledgedPushFailureOrder;
        private int sidecarUnacknowledgedPushExitOrder;
        private int sidecarUnacknowledgedPushRecoveryReadyOrder;
        private int sidecarUnacknowledgedPushProcessIdentifier;
        private JsonNode recoveryExhaustionAcceptance;
        private JsonNode sidecarRecoveryExhausted;
        private final List<Integer> sidecarRecoveryScheduledAttempts =
                new ArrayList<>();
        private final List<Integer> sidecarRecoveryReadyAttempts =
                new ArrayList<>();
        private int sidecarRecoveryStabilityDeadlineCount;
        private JsonNode sidecarRecoveryBudgetReset;
        private int sidecarFailureInjectionCount;
        private int inFlightFailureInjectionOrder;
        private int inFlightSidecarExitOrder;
        private int inFlightRecoveryReadyOrder;
        private int oldDocumentProbeOpenCount;
        private final List<Integer> opaqueRequestOpenEventOrders =
                new ArrayList<>();
        private final List<Integer> pushConnectOrders = new ArrayList<>();
        private final List<Integer> pushDisconnectOrders = new ArrayList<>();
        private final List<InFlightOpaqueRequestTrace> inFlightLargeRawRequests =
                new ArrayList<>();
        private boolean hostControlReceived;
        private int hostReplacementControlCount;
        private boolean hostRollbackControlReceived;
        private boolean sidecarReplacementRequested;
        private int sidecarReplacementRequestedCount;
        private int sidecarReplacementReadyCount;
        private JsonNode sidecarReplacementReady;
        private JsonNode sidecarReplacementFailed;
        private JsonNode sidecarRollbackReady;

        private void accept(JsonNode root) {
            messageCount++;
            String type = text(root, "type");
            typeCounts.merge(type.isBlank() ? "unknown" : type, 1, Integer::sum);
            JsonNode payload = root.path("payload");
            switch (type) {
                case "launch" -> standardLaunch =
                        "flow-ipc".equals(text(payload, "transport"));
                case "launch.sidecar" -> sidecarLaunchAttempted = true;
                case "sidecar.started" -> {
                    currentSidecarGeneration = payload.path("generation")
                            .asInt(currentSidecarGeneration);
                    currentSidecarProcessIdentifier = payload.path(
                            "processIdentifier").asInt();
                }
                case "sidecar.initialReadyDeadline" ->
                        initialReadyDeadline = true;
                case "window.create" -> windowCreated = true;
                case "ready" -> {
                    JsonNode runtime = payload.path("envelope").path("payload");
                    if (!runtime.isObject()) {
                        runtime = payload;
                    }
                    boolean standardRuntime = "standard-vaadin-bundle-opaque-ipc"
                            .equals(text(runtime, "runtime"))
                            && "opaque-http-stream-ipc"
                            .equals(text(runtime, "browserRequestTransport"))
                            && "flow-ipc".equals(text(runtime, "transport"))
                            && "standard-production-bundle".equals(
                            text(runtime, "flowClientResourceLoading"));
                    boolean viteRuntime = "standard-vaadin-bundle-opaque-ipc"
                            .equals(text(runtime, "runtime"))
                            && "opaque-http-stream-ipc"
                            .equals(text(runtime, "browserRequestTransport"))
                            && "flow-ipc".equals(text(runtime, "transport"))
                            && "vite-development-server".equals(
                            text(runtime, "flowClientResourceLoading"));
                    standardJavaReady |= standardRuntime;
                    viteJavaReady |= viteRuntime;
                    if (viteRuntime) {
                        viteJavaReadyCount++;
                    }
                }
                case "flowWebAppReady" -> viteWebAppReady |=
                        "development-server".equals(text(payload, "mode"))
                                && "opaque-http-stream-ipc".equals(
                                text(payload, "requestTransport"));
                case "standardBundleReady" -> standardBundleReady |=
                        "opaque-http-stream-ipc".equals(text(payload, "requestTransport"));
                case "standardBundleResource" -> {
                    String path = text(payload, "path");
                    standardFlowClientResource |= path.contains("FlowClient-")
                            || path.contains("generated-flow-imports-");
                    if (path.contains("/VAADIN/build/chunk-")
                            && path.endsWith(".js")) {
                        standardLazyFrontendChunkResourceCount++;
                    }
                    standardLazyTransitiveFrontendResource |=
                            path.contains("/VAADIN/build/vatori-lazy-transitive-")
                                    && path.endsWith(".js");
                }
                case "developmentWebAppResource" -> {
                    String path = text(payload, "path");
                    int status = payload.path("status").asInt();
                    boolean successful = status >= 200 && status < 400;
                    viteResourceFailure |= !successful;
                    viteClientResource |= successful && "/@vite/client".equals(path);
                    viteDevModeResource |= successful
                            && "/generated/vite-devmode.ts".equals(path);
                    viteVaadinResource |= successful
                            && "/generated/vaadin.ts".equals(path);
                    viteFlowClientResource |= successful && path.endsWith("/FlowClient.js");
                    if (successful && "/vatori-vite-hmr-probe.js".equals(path)) {
                        viteProbeResourceCount++;
                    }
                }
                case "webview.firstMeaningfulPaint" -> {
                    firstMeaningfulPaint = true;
                    viteFirstMeaningfulPaint |= "vite-development-server".equals(
                            text(envelopePayload(payload), "source"));
                }
                case "opaqueHttpStreamOpen" -> {
                    opaqueRequestCount++;
                    opaqueRequestOpenEventOrders.add(messageCount);
                    JsonNode request = envelopePayload(payload);
                    String requestId = text(request, "requestId");
                    ScopedRequestKey scopedKey = scopedRequestKey(request);
                    if (scopedKey != null) {
                        scopedRequestOpenOrders.putIfAbsent(
                                scopedKey, messageCount);
                    }
                    if (!requestId.isBlank()) {
                        opaqueRequestOpenOrders.putIfAbsent(
                                requestId, messageCount);
                        opaqueRequestContentLengths.put(
                                requestId,
                                request.path("contentLength").asLong(-1));
                        String method = text(request, "method");
                        if (("POST".equals(method) || "PUT".equals(method))
                                && headerNamed(request, "Content-Type")
                                && headerNamed(request, "X-Filename")) {
                            opaqueRawRequestIds.add(requestId);
                        }
                        if ("POST".equals(method)
                                && text(request, "url").contains("/upload")
                                && headerNamed(request, "Content-Type")
                                && !headerNamed(request, "X-Filename")
                                && request.path("contentLength").asLong(-1)
                                < 0) {
                            opaqueMultipartUploadRequestIds.add(requestId);
                            if (scopedKey != null) {
                                scopedMultipartUploadRequestKeys.add(scopedKey);
                            }
                        }
                    }
                    if (text(request, "url").contains(
                            "vatori-inflight-old-document-probe")) {
                        oldDocumentProbeOpenCount++;
                    }
                    if (headerNamed(request, "Range")) {
                        opaqueRangeRequestCount++;
                        addRequestId(opaqueRangeRequestIds, request);
                    }
                    if (request.path("contentLength").asLong(-1)
                            > 4L * 1024 * 1024) {
                        opaqueLargeRequestCount++;
                        addRequestId(opaqueLargeRequestIds, request);
                        String method = text(request, "method");
                        if (("POST".equals(method) || "PUT".equals(method))
                                && headerNamed(request, "Content-Type")
                                && headerNamed(request, "X-Filename")) {
                            addRequestId(opaqueRawUploadRequestIds, request);
                            inFlightLargeRawRequests.add(
                                    new InFlightOpaqueRequestTrace(
                                            requestId,
                                            request.path("contentLength")
                                                    .asLong(-1),
                                            messageCount));
                        }
                    }
                }
                case "opaqueHttpStreamChunk" -> {
                    JsonNode requestChunk = envelopePayload(payload);
                    String requestId = text(requestChunk, "requestId");
                    ScopedRequestKey scopedKey =
                            scopedRequestKey(requestChunk);
                    if (scopedKey != null) {
                        scopedRequestChunkBytes.merge(
                                scopedKey,
                                requestChunk.path("bodyBytes").asLong(0),
                                Long::sum);
                    }
                    if (!requestId.isBlank()) {
                        opaqueRequestChunkBytes.merge(
                                requestId,
                                requestChunk.path("bodyBytes").asLong(0),
                                Long::sum);
                        InFlightOpaqueRequestTrace inFlight =
                                latestInFlightRequest(requestId);
                        if (inFlight != null) {
                            inFlight.chunkBytes += requestChunk.path(
                                    "bodyBytes").asLong(0);
                        }
                    }
                }
                case "opaqueHttpStreamEnd" -> {
                    JsonNode requestEnd = envelopePayload(payload);
                    String requestId = text(requestEnd, "requestId");
                    ScopedRequestKey scopedKey = scopedRequestKey(requestEnd);
                    if (scopedKey != null) {
                        scopedRequestEndBodyBytes.put(
                                scopedKey,
                                requestEnd.path("bodyBytes").asLong(-1));
                        scopedRequestEndOrders.put(scopedKey, messageCount);
                    }
                    if (!requestId.isBlank()) {
                        opaqueRequestEndBodyBytes.put(
                                requestId,
                                requestEnd.path("bodyBytes").asLong(-1));
                        opaqueRequestEndOrders.put(requestId, messageCount);
                        InFlightOpaqueRequestTrace inFlight =
                                latestInFlightRequest(requestId);
                        if (inFlight != null) {
                            inFlight.endBodyBytes = requestEnd.path(
                                    "bodyBytes").asLong(-1);
                            inFlight.endOrder = messageCount;
                        }
                    }
                    if (requestEnd.path("bodyBytes").asLong(-1)
                            > 4L * 1024 * 1024) {
                        opaqueLargeRequestEndCount++;
                        addRequestId(opaqueLargeRequestEndIds, requestEnd);
                    }
                }
                case "opaqueHttpStreamCancel" -> {
                    opaqueResponseCancelCount++;
                    JsonNode cancellation = envelopePayload(payload);
                    addRequestId(opaqueCancelledRequestIds, cancellation);
                    ScopedRequestKey scopedKey =
                            scopedRequestKey(cancellation);
                    if (scopedKey != null) {
                        scopedCancelledRequestKeys.add(scopedKey);
                        scopedRequestCancelOrders.put(scopedKey, messageCount);
                    }
                    String requestId = text(cancellation, "requestId");
                    if (!requestId.isBlank()) {
                        opaqueRequestCancelOrders.put(requestId, messageCount);
                        InFlightOpaqueRequestTrace inFlight =
                                latestInFlightRequest(requestId);
                        if (inFlight != null) {
                            inFlight.cancelOrder = messageCount;
                        }
                    }
                }
                case "opaqueHttpStreamResponse" -> {
                    JsonNode response = envelopePayload(payload);
                    int status = response.path("status").asInt();
                    ScopedRequestKey scopedKey = scopedRequestKey(response);
                    if (scopedKey != null) {
                        scopedResponseStatuses.put(scopedKey, status);
                        if (status >= 200 && status < 400) {
                            scopedSuccessfulResponseKeys.add(scopedKey);
                        }
                    }
                    String requestId = text(response, "requestId");
                    if (!requestId.isBlank()) {
                        opaqueResponseStatuses.put(requestId, status);
                        InFlightOpaqueRequestTrace inFlight =
                                latestInFlightRequest(requestId);
                        if (inFlight != null) {
                            inFlight.responseStatus = status;
                            inFlight.responseOrder = messageCount;
                        }
                    }
                    if (status >= 200 && status < 400) {
                        opaqueResponseCount++;
                        addRequestId(
                                opaqueSuccessfulResponseRequestIds,
                                response);
                    }
                    if (status == 206) {
                        opaquePartialResponseCount++;
                        addRequestId(
                                opaquePartialResponseRequestIds,
                                response);
                    }
                }
                case "opaqueHttpStreamResponseEnd" -> {
                    opaqueResponseEndCount++;
                    JsonNode responseEnd = envelopePayload(payload);
                    InFlightOpaqueRequestTrace inFlight =
                            latestInFlightRequest(text(responseEnd, "requestId"));
                    if (inFlight != null) {
                        inFlight.responseEndOrder = messageCount;
                    }
                    if (responseEnd.path("bodyBytes").asLong()
                            >= 3L * 1024 * 1024) {
                        opaqueLargeResponseEndCount++;
                        addRequestId(
                                opaqueLargeResponseEndRequestIds,
                                responseEnd);
                    }
                }
                case "opaquePushConnect" -> {
                    pushConnectOrders.add(messageCount);
                    acceptPushConnect(envelopePayload(payload));
                }
                case "opaquePushConnected" -> pushConnectedCount++;
                case "opaquePushDisconnect" -> {
                    pushDisconnectOrders.add(messageCount);
                    acceptPushDisconnect(envelopePayload(payload));
                }
                case "opaquePushResync" ->
                        acceptPushResyncRequest(envelopePayload(payload));
                case "opaquePushAck" ->
                        acceptPushAcknowledgement(envelopePayload(payload));
                case "opaquePush" -> acceptPush(envelopePayload(payload));
                case "standardBundle.frameworkAcceptance" -> frameworkAcceptance = payload;
                case "flowPushRecovery.frameworkAcceptance" ->
                        pushRecoveryAcceptance = payload;
                case "flowPushDelivery.frameworkAcceptance" ->
                        pushDeliveryAcceptance = payload;
                case "flowLargeDownload.frameworkAcceptance" ->
                        largeDownloadAcceptance = payload;
                case "flowLargeUpload.frameworkAcceptance" ->
                        largeUploadAcceptance = payload;
                case "flowInFlightRequestRecovery.frameworkAcceptance" ->
                        inFlightRequestRecoveryAcceptance = payload;
                case "flowLargeMultipartUpload.frameworkAcceptance" ->
                        largeMultipartUploadAcceptance = payload;
                case "flowConcurrentUpload.frameworkAcceptance" ->
                        concurrentUploadAcceptance = payload;
                case "flowRequestFanout.frameworkAcceptance" ->
                        requestFanoutAcceptance = payload;
                case "flowConcurrentMultipartUpload.frameworkAcceptance" ->
                        concurrentMultipartUploadAcceptance = payload;
                case "flowCrossSessionMultipartUpload.frameworkAcceptance" ->
                        crossSessionMultipartUploadAcceptance = payload;
                case "lazyGridQuery.frameworkAcceptance" ->
                        lazyGridQueryAcceptance = payload;
                case "flowDnd.frameworkAcceptance" ->
                        flowDndAcceptance = payload;
                case "flowInputPopup.frameworkAcceptance" ->
                        flowInputPopupAcceptance = payload;
                case "flowInputPopup.localizedTimeAcceptance" ->
                        localizedInputPopupAcceptance = payload;
                case "pickerValidation.frameworkAcceptance" ->
                        pickerValidationAcceptance = payload;
                case "flowMenuPopup.frameworkAcceptance" ->
                        flowMenuPopupAcceptance = payload;
                case "flowOverlayStack.frameworkAcceptance" ->
                        flowOverlayStackAcceptance = payload;
                case "structuredRpc.frameworkAcceptance" ->
                        structuredRpcAcceptance = payload;
                case "viteDevelopment.frameworkAcceptance" -> viteAcceptance = payload;
                case "javaReload.frameworkAcceptance" ->
                        javaReloadAcceptance = payload;
                case "reactorReload.frameworkAcceptance" ->
                        reactorReloadAcceptance = payload;
                case "javaRollback.frameworkAcceptance" ->
                        javaRollbackAcceptance = payload;
                case "standardBundle.sidecarRestartAcceptance" -> recoveryAcceptance = payload;
                case "standardBundle.sidecarUnacknowledgedPushBoundary" ->
                        acceptSidecarUnacknowledgedPushBoundary(payload);
                case "standardBundle.sidecarRecoveryExhaustionAcceptance" ->
                        recoveryExhaustionAcceptance = payload;
                case "acceptance.sidecarFailureInjected" -> {
                    sidecarFailureInjectionCount++;
                    if ("unacknowledged-push-replacement".equals(
                            text(payload, "reason"))) {
                        sidecarUnacknowledgedPushFailureOrder = messageCount;
                        sidecarUnacknowledgedPushProcessCorrelated &=
                                sidecarUnacknowledgedPushProcessIdentifier > 0
                                        && sidecarUnacknowledgedPushProcessIdentifier
                                        == payload.path(
                                        "processIdentifier").asInt();
                    }
                    if ("in-flight-opaque-request".equals(
                            text(payload, "reason"))) {
                        inFlightFailureInjectionOrder = messageCount;
                    }
                }
                case "sidecar.recoveryScheduled" ->
                        sidecarRecoveryScheduledAttempts.add(
                                payload.path("attempt").asInt());
                case "sidecar.recoveryReady" -> {
                    sidecarRecoveryReadyAttempts.add(
                            payload.path("attempt").asInt());
                    if (sidecarUnacknowledgedPushFailureOrder > 0
                            && sidecarUnacknowledgedPushRecoveryReadyOrder == 0) {
                        sidecarUnacknowledgedPushRecoveryReadyOrder = messageCount;
                    }
                    if (inFlightFailureInjectionOrder > 0) {
                        inFlightRecoveryReadyOrder = messageCount;
                    }
                }
                case "sidecar.exit" -> {
                    if (sidecarUnacknowledgedPushFailureOrder > 0
                            && sidecarUnacknowledgedPushRecoveryReadyOrder == 0
                            && sidecarUnacknowledgedPushExitOrder == 0) {
                        sidecarUnacknowledgedPushExitOrder = messageCount;
                        sidecarUnacknowledgedPushProcessCorrelated &=
                                sidecarUnacknowledgedPushProcessIdentifier > 0
                                        && sidecarUnacknowledgedPushProcessIdentifier
                                        == payload.path(
                                        "processIdentifier").asInt();
                    }
                    if (inFlightFailureInjectionOrder > 0
                            && inFlightRecoveryReadyOrder == 0) {
                        inFlightSidecarExitOrder = messageCount;
                        for (InFlightOpaqueRequestTrace request
                                : inFlightLargeRawRequests) {
                            request.frozen = true;
                        }
                    }
                }
                case "sidecar.recoveryStabilityDeadline" ->
                        sidecarRecoveryStabilityDeadlineCount++;
                case "sidecar.recoveryBudgetReset" ->
                        sidecarRecoveryBudgetReset = payload;
                case "hostControl.received" -> {
                    boolean replacement = "replaceSidecar".equals(
                            text(payload, "type"));
                    hostControlReceived |= replacement;
                    if (replacement) {
                        hostReplacementControlCount++;
                    }
                    hostRollbackControlReceived |=
                            "rollbackSidecar".equals(text(payload, "type"));
                }
                case "sidecar.replacementRequested" -> {
                        sidecarReplacementRequested = true;
                    sidecarReplacementRequestedCount++;
                }
                case "sidecar.replacementReady" -> {
                        sidecarReplacementReady = payload;
                    sidecarReplacementReadyCount++;
                }
                case "sidecar.replacementFailed" ->
                        sidecarReplacementFailed = payload;
                case "sidecar.rollbackReady" ->
                        sidecarRollbackReady = payload;
                case "shutdown.sidecar" -> shutdown = true;
                case "error", "webview.error" -> {
                    errorObserved = true;
                    String code = text(payload, "code");
                    fatalSidecarStartupFailure |= "sidecarStartFailed".equals(code)
                            || "sidecarUnexpectedExit".equals(code);
                    initialReadyTimeout |=
                            "sidecarInitialReadyTimeout".equals(code);
                    if ("sidecarRecoveryExhausted".equals(code)) {
                        sidecarRecoveryExhausted = payload;
                    }
                }
                default -> {
                }
            }
        }

        private void acceptPushConnect(JsonNode payload) {
            pushConnectCount++;
            String scope = pushScope(payload);
            int lastSeenServerSyncId = payload.path("lastSeenServerSyncId")
                    .asInt(-1);
            if (pushRecoveryScope.isEmpty()) {
                pushRecoveryScope = scope;
                initialPushLastSeenServerSyncId = lastSeenServerSyncId;
            } else if (pushDisconnectAfterMessage
                    && !pushReconnectAfterDisconnect) {
                pushRecoveryConnectionConsistent &= pushRecoveryScope.equals(scope);
                if (pushRecoveryScope.equals(scope)) {
                    pushReconnectAfterDisconnect = true;
                    reconnectLastSeenServerSyncId = lastSeenServerSyncId;
                }
            }
        }

        private void acceptPushDisconnect(JsonNode payload) {
            pushDisconnectCount++;
            if (pushInitialMessageObserved && !pushDisconnectAfterMessage) {
                boolean sameScope = pushRecoveryScope.equals(pushScope(payload));
                pushRecoveryConnectionConsistent &= sameScope;
                pushDisconnectAfterMessage = sameScope;
            }
        }

        private void acceptPushResyncRequest(JsonNode payload) {
            pushResyncRequestCount++;
            if (pushReconnectAfterDisconnect
                    && !pushResyncRequestAfterReconnect) {
                boolean sameScope = pushRecoveryScope.equals(pushScope(payload));
                pushRecoveryConnectionConsistent &= sameScope;
                pushResyncRequestAfterReconnect = sameScope;
            }
        }

        private void acceptPush(JsonNode payload) {
            pushCount++;
            acceptPushDelivery(payload);
            boolean resync = payload.path("resync").asBoolean(false);
            long sequence = payload.path("pushSequence").asLong(-1L);
            boolean sameScope = pushRecoveryScope.equals(pushScope(payload));
            if (resync) {
                pushResyncResponseCount++;
                if (pushResyncRequestAfterReconnect
                        && !pushResyncResponseAfterRequest) {
                    pushRecoveryConnectionConsistent &= sameScope;
                    pushResyncResponseAfterRequest = sameScope
                            && sequence > initialPushSequence;
                    if (pushResyncResponseAfterRequest) {
                        resyncPushSequence = sequence;
                    }
                }
                return;
            }
            if (!pushInitialMessageObserved && sameScope && sequence >= 0) {
                pushInitialMessageObserved = true;
                initialPushSequence = sequence;
            } else if (pushResyncResponseAfterRequest && sameScope
                    && sequence > resyncPushSequence) {
                pushAfterResync = true;
                postResyncPushSequence = sequence;
            }
        }

        private void acceptPushDelivery(JsonNode payload) {
            long sequence = payload.path("pushSequence").asLong(-1L);
            if (sequence <= 0) {
                return;
            }
            int deliveryAttempt = payload.path("deliveryAttempt").asInt(1);
            int flowServerSyncId = payload.path("flowServerSyncId").asInt(-1);
            int uidlBytes = payload.path("uidlBytes").asInt(-1);
            String scope = pushScope(payload);
            SidecarPushKey key = new SidecarPushKey(
                    currentSidecarGeneration, sequence);
            PushDeliveryTrace existing = pushDeliveries.get(key);
            if (existing == null) {
                pushDeliveries.put(key, new PushDeliveryTrace(
                        scope,
                        pushConnectionScope(payload),
                        flowServerSyncId,
                        uidlBytes,
                        deliveryAttempt));
                return;
            }
            existing.maxDeliveryAttempt = Math.max(
                    existing.maxDeliveryAttempt, deliveryAttempt);
            if (deliveryAttempt > 1) {
                boolean sameDelivery = existing.scope.equals(scope)
                        && existing.flowServerSyncId == flowServerSyncId
                        && existing.uidlBytes == uidlBytes;
                pushDeliveryScopeConsistent &= sameDelivery;
                if (sameDelivery) {
                    pushSameSequenceRedelivery = true;
                    pushRedeliverySequence = sequence;
                }
            }
        }

        private void acceptPushAcknowledgement(JsonNode payload) {
            pushAcknowledgementCount++;
            long sequence = payload.path("pushSequence").asLong(-1L);
            PushDeliveryTrace delivery = pushDeliveries.get(
                    new SidecarPushKey(currentSidecarGeneration, sequence));
            if (delivery == null) {
                return;
            }
            delivery.acknowledgementOrder = messageCount;
            if (delivery.maxDeliveryAttempt <= 1) {
                return;
            }
            int flowServerSyncId = payload.path("flowServerSyncId").asInt(-1);
            int lastSeenServerSyncId = payload.path("lastSeenServerSyncId")
                    .asInt(-1);
            boolean sameDelivery = delivery.scope.equals(pushScope(payload))
                    && delivery.flowServerSyncId == flowServerSyncId;
            boolean flowSeen = flowServerSyncId < 0
                    || lastSeenServerSyncId >= flowServerSyncId;
            pushDeliveryScopeConsistent &= sameDelivery;
            pushRedeliveryAcknowledged |= sameDelivery && flowSeen;
        }

        private void acceptSidecarUnacknowledgedPushBoundary(
                JsonNode payload) {
            sidecarUnacknowledgedPushBoundary = payload;
            sidecarUnacknowledgedPushBoundaryOrder = messageCount;
            sidecarUnacknowledgedPushProcessIdentifier = payload.path(
                    "processIdentifier").asInt();
            sidecarUnacknowledgedPushProcessCorrelated =
                    sidecarUnacknowledgedPushProcessIdentifier > 0
                            && sidecarUnacknowledgedPushProcessIdentifier
                            == currentSidecarProcessIdentifier;
            long sequence = payload.path("pushSequence").asLong(-1L);
            PushDeliveryTrace delivery = pushDeliveries.get(
                    new SidecarPushKey(currentSidecarGeneration, sequence));
            sidecarUnacknowledgedPushTraceCorrelated =
                    delivery != null
                            && delivery.connectionScope.equals(
                            pushConnectionScope(payload))
                            && delivery.flowServerSyncId == payload.path(
                            "flowServerSyncId").asInt(-1)
                            && delivery.maxDeliveryAttempt >= payload.path(
                            "deliveryAttempt").asInt(1)
                            && delivery.acknowledgementOrder == 0;
        }

        private record SidecarPushKey(int generation, long sequence) {
        }

        private static final class PushDeliveryTrace {
            private final String scope;
            private final String connectionScope;
            private final int flowServerSyncId;
            private final int uidlBytes;
            private int maxDeliveryAttempt;
            private int acknowledgementOrder;

            private PushDeliveryTrace(
                    String scope,
                    String connectionScope,
                    int flowServerSyncId,
                    int uidlBytes,
                    int deliveryAttempt) {
                this.scope = scope;
                this.connectionScope = connectionScope;
                this.flowServerSyncId = flowServerSyncId;
                this.uidlBytes = uidlBytes;
                this.maxDeliveryAttempt = deliveryAttempt;
            }
        }

        private static String pushScope(JsonNode payload) {
            return String.join("\u0000",
                    text(payload, "connectionId"),
                    text(payload, "windowId"),
                    text(payload, "sessionId"),
                    text(payload, "uiId"),
                    payload.path("flowUiId").isNumber()
                            ? Integer.toString(payload.path("flowUiId").asInt())
                            : "");
        }

        private static String pushConnectionScope(JsonNode payload) {
            return String.join("\u0000",
                    text(payload, "connectionId"),
                    payload.path("flowUiId").isNumber()
                            ? Integer.toString(payload.path("flowUiId").asInt())
                            : "");
        }

        private List<String> hostStartupFailureMissing() {
            List<String> missing = new ArrayList<>();
            require(missing, standardLaunch, "standard flow-ipc launch");
            require(missing, sidecarLaunchAttempted, "sidecar launch attempt");
            require(missing, fatalSidecarStartupFailure,
                    "fatal sidecar startup failure");
            require(missing, !standardJavaReady && !viteJavaReady,
                    "no Java ready before failure");
            require(missing, !windowCreated, "no native window created");
            return missing;
        }

        private List<String> hostReadyTimeoutMissing() {
            List<String> missing = new ArrayList<>();
            require(missing, standardLaunch, "standard flow-ipc launch");
            require(missing, sidecarLaunchAttempted, "sidecar launch attempt");
            require(missing, initialReadyDeadline, "initial ready deadline");
            require(missing, initialReadyTimeout, "initial ready timeout");
            require(missing, !standardJavaReady && !viteJavaReady,
                    "no Java ready before timeout");
            require(missing, !windowCreated, "no native window created");
            return missing;
        }

        private List<String> sidecarRecoveryExhaustionMissing() {
            List<String> missing = new ArrayList<>();
            require(missing, standardLaunch, "standard flow-ipc launch");
            require(missing, standardJavaReady, "standard Java runtime ready");
            require(missing, standardBundleReady, "packaged bundle ready");
            require(missing, windowCreated, "native window created");
            require(missing, firstMeaningfulPaint, "first meaningful paint");
            require(missing, recoveryExhaustionAcceptance != null,
                    "sidecar recovery exhaustion acceptance");
            if (recoveryExhaustionAcceptance != null) {
                for (String field : List.of(
                        "armed", "rapidFailuresRecovered", "processRestarted",
                        "windowPreserved", "webViewPreserved")) {
                    require(missing, bool(recoveryExhaustionAcceptance, field),
                            "recovery exhaustion " + field);
                }
                require(missing,
                        recoveryExhaustionAcceptance.path(
                                "recoveryGeneration").asInt() >= 2,
                        "two recovery generations before exhaustion");
            }
            require(missing, sidecarFailureInjectionCount >= 3,
                    "three unexpected sidecar failures");
            require(missing,
                    sidecarRecoveryScheduledAttempts.equals(List.of(1, 2)),
                    "bounded recovery attempts 1 and 2");
            require(missing,
                    sidecarRecoveryReadyAttempts.equals(List.of(1, 2)),
                    "ready recoveries 1 and 2 before exhaustion");
            require(missing, sidecarRecoveryExhausted != null,
                    "sidecar recovery exhausted error");
            if (sidecarRecoveryExhausted != null) {
                require(missing,
                        sidecarRecoveryExhausted.path("attempts").asInt() == 2,
                        "exhausted recovery attempt count");
                require(missing,
                        sidecarRecoveryExhausted.path("restartLimit").asInt() == 2,
                        "manifest recovery restart limit");
            }
            return missing;
        }

        private List<String> runtimeMissing() {
            List<String> missing = new ArrayList<>();
            require(missing, standardLaunch, "standard flow-ipc launch");
            require(missing, standardJavaReady, "standard Java runtime ready");
            require(missing, standardBundleReady, "packaged bundle ready");
            require(missing, standardFlowClientResource, "standard Flow client resource");
            require(missing, firstMeaningfulPaint, "first meaningful paint");
            require(missing, opaqueRequestCount >= 2, "bootstrap and UIDL requests");
            require(missing, opaqueResponseCount >= 2, "successful opaque responses");
            require(missing, opaqueResponseEndCount >= 2, "complete opaque responses");
            require(missing, pushConnectedCount >= 1, "opaque push connection");
            require(missing, pushCount >= 1, "opaque push message");
            require(missing, shutdown, "sidecar shutdown");
            require(missing, !errorObserved, "no host/runtime error");
            return missing;
        }

        private List<String> viteDevelopmentMissing() {
            List<String> missing = new ArrayList<>();
            require(missing, standardLaunch, "standard flow-ipc launch");
            require(missing, viteJavaReady, "development Java runtime ready");
            require(missing, viteWebAppReady, "Vite development web app ready");
            require(missing, viteClientResource, "Vite HMR client resource");
            require(missing, viteDevModeResource, "Vaadin Vite dev-mode entry");
            require(missing, viteVaadinResource, "generated Vaadin frontend entry");
            require(missing, viteFlowClientResource, "standard Flow client resource");
            require(missing, viteProbeResourceCount >= 2, "HMR probe resource reload");
            require(missing, viteFirstMeaningfulPaint, "Vite first meaningful paint");
            require(missing, opaqueRequestCount >= 2, "bootstrap and UIDL requests");
            require(missing, opaqueResponseCount >= 2, "successful opaque responses");
            require(missing, opaqueResponseEndCount >= 2, "complete opaque responses");
            require(missing, pushConnectedCount >= 1, "opaque push connection");
            require(missing, pushCount >= 1, "opaque push message");
            require(missing, !viteResourceFailure, "successful Vite resource responses");
            requireViteAcceptance(missing);
            require(missing, shutdown, "sidecar shutdown");
            require(missing, !errorObserved, "no host/runtime error");
            return missing;
        }

        private List<String> javaReloadDevelopmentMissing() {
            List<String> missing = new ArrayList<>();
            require(missing, standardLaunch, "standard flow-ipc launch");
            require(missing, viteJavaReadyCount >= 2,
                    "initial and rebuilt development Java runtime ready");
            require(missing, viteWebAppReady,
                    "Vite development web app ready");
            require(missing, viteClientResource,
                    "Vite HMR client resource");
            require(missing, viteDevModeResource,
                    "Vaadin Vite dev-mode entry");
            require(missing, viteVaadinResource,
                    "generated Vaadin frontend entry");
            require(missing, viteFlowClientResource,
                    "standard Flow client resource");
            require(missing, viteFirstMeaningfulPaint,
                    "Vite first meaningful paint");
            require(missing, opaqueRequestCount >= 4,
                    "opaque requests before and after Java reload");
            require(missing, opaqueResponseCount >= 4,
                    "successful opaque responses before and after Java reload");
            require(missing, opaqueResponseEndCount >= 4,
                    "complete opaque responses before and after Java reload");
            require(missing, pushConnectedCount >= 2,
                    "opaque push connection before and after Java reload");
            require(missing, pushCount >= 2,
                    "opaque push before and after Java reload");
            require(missing, !viteResourceFailure,
                    "successful Vite resource responses");
            require(missing, hostControlReceived,
                    "CLI-to-host replacement control");
            require(missing, sidecarReplacementRequested,
                    "planned sidecar replacement request");
            requireSidecarReplacement(missing);
            requireJavaReloadAcceptance(missing);
            require(missing, shutdown, "sidecar shutdown");
            require(missing, !errorObserved, "no host/runtime error");
            return missing;
        }

        private List<String> reactorReloadDevelopmentMissing() {
            List<String> missing = new ArrayList<>();
            require(missing, standardLaunch, "standard flow-ipc launch");
            require(missing, viteJavaReadyCount >= 4,
                    "initial and three rebuilt development Java runtimes ready");
            require(missing, viteWebAppReady,
                    "Vite development web app ready");
            require(missing, viteClientResource,
                    "Vite HMR client resource");
            require(missing, viteDevModeResource,
                    "Vaadin Vite dev-mode entry");
            require(missing, viteVaadinResource,
                    "generated Vaadin frontend entry");
            require(missing, viteFlowClientResource,
                    "standard Flow client resource");
            require(missing, viteFirstMeaningfulPaint,
                    "Vite first meaningful paint");
            require(missing, opaqueRequestCount >= 8,
                    "opaque requests across all Maven-model reloads");
            require(missing, opaqueResponseCount >= 8,
                    "successful opaque responses across all Maven-model reloads");
            require(missing, opaqueResponseEndCount >= 8,
                    "complete opaque responses across all Maven-model reloads");
            require(missing, pushConnectedCount >= 4,
                    "opaque push connection across all Maven-model reloads");
            require(missing, pushCount >= 4,
                    "opaque pushes across all Maven-model reloads");
            require(missing, !viteResourceFailure,
                    "successful Vite resource responses");
            require(missing, hostControlReceived,
                    "CLI-to-host replacement control");
            require(missing, sidecarReplacementRequested,
                    "planned sidecar replacement request");
            require(missing, hostReplacementControlCount == 3,
                    "exactly three CLI-to-host replacement controls");
            require(missing, sidecarReplacementRequestedCount == 3,
                    "exactly three planned sidecar replacement requests");
            require(missing, sidecarReplacementReadyCount == 3,
                    "exactly three ready sidecar replacements");
            requireSidecarReplacement(missing);
            requireReactorReloadAcceptance(missing);
            require(missing, shutdown, "sidecar shutdown");
            require(missing, !errorObserved, "no host/runtime error");
            return missing;
        }

        private List<String> javaReloadRollbackDevelopmentMissing() {
            List<String> missing = new ArrayList<>();
            require(missing, standardLaunch, "standard flow-ipc launch");
            require(missing, viteJavaReadyCount >= 2,
                    "initial and restored development Java runtime ready");
            require(missing, viteWebAppReady,
                    "Vite development web app ready");
            require(missing, viteClientResource,
                    "Vite HMR client resource");
            require(missing, viteDevModeResource,
                    "Vaadin Vite dev-mode entry");
            require(missing, viteVaadinResource,
                    "generated Vaadin frontend entry");
            require(missing, viteFlowClientResource,
                    "standard Flow client resource");
            require(missing, viteFirstMeaningfulPaint,
                    "Vite first meaningful paint");
            require(missing, opaqueRequestCount >= 4,
                    "opaque requests before and after rollback");
            require(missing, opaqueResponseCount >= 4,
                    "successful opaque responses before and after rollback");
            require(missing, opaqueResponseEndCount >= 4,
                    "complete opaque responses before and after rollback");
            require(missing, pushConnectedCount >= 2,
                    "opaque push connection before and after rollback");
            require(missing, pushCount >= 2,
                    "opaque push before and after rollback");
            require(missing, !viteResourceFailure,
                    "successful Vite resource responses");
            require(missing, hostControlReceived,
                    "CLI-to-host replacement control");
            require(missing, hostRollbackControlReceived,
                    "CLI-to-host rollback control");
            require(missing, sidecarReplacementRequested,
                    "planned sidecar replacement request");
            requireFailedSidecarReplacement(missing);
            requireSidecarRollback(missing);
            requireJavaRollbackAcceptance(missing);
            require(missing, shutdown, "sidecar shutdown");
            require(missing, !errorObserved, "no host/runtime error");
            return missing;
        }

        private void requireFailedSidecarReplacement(
                List<String> missing) {
            JsonNode payload = sidecarReplacementFailed;
            require(missing, payload != null,
                    "failed replacement result");
            if (payload == null) {
                return;
            }
            require(missing,
                    "candidate-exited-before-ready".equals(
                            text(payload, "cause")),
                    "candidate exit before ready");
            require(missing,
                    payload.path("processIdentifier").asLong() > 0,
                    "failed candidate process identifier");
            require(missing,
                    !text(payload, "requestId").isBlank(),
                    "failed replacement request id");
        }

        private void requireSidecarRollback(
                List<String> missing) {
            JsonNode payload = sidecarRollbackReady;
            require(missing, payload != null,
                    "stable sidecar rollback ready");
            if (payload == null) {
                return;
            }
            require(missing, bool(payload, "processReplaced"),
                    "rollback process replaced");
            require(missing,
                    payload.path("windowsReloaded").asInt() >= 1,
                    "rollback window reload");
            require(missing,
                    payload.path("recoveryGeneration").asInt() >= 1,
                    "rollback generation");
            require(missing,
                    "candidate-exited-before-ready".equals(
                            text(payload, "replacementFailure")),
                    "rollback replacement failure attribution");
            require(missing,
                    payload.path("previousProcessIdentifier").asLong() > 0
                            && payload.path("processIdentifier").asLong() > 0
                            && payload.path("previousProcessIdentifier").asLong()
                            != payload.path("processIdentifier").asLong(),
                    "rollback process identifiers");
            require(missing,
                    !text(payload, "requestId").isBlank(),
                    "rollback request id");
            require(missing,
                    sidecarReplacementFailed != null
                            && text(payload, "requestId").equals(
                            text(sidecarReplacementFailed, "requestId")),
                    "replacement and rollback request correlation");
        }

        private void requireJavaRollbackAcceptance(
                List<String> missing) {
            JsonNode payload = javaRollbackAcceptance;
            require(missing, payload != null,
                    "Java rollback acceptance result");
            if (payload == null) {
                return;
            }
            for (String field : List.of(
                    "complete", "baselineObserved",
                    "failingCandidateApplied",
                    "stableRuntimeRestored", "sourceRestored",
                    "processReplaced", "windowPreserved",
                    "webViewPreserved", "webViewReloaded",
                    "flowClientOwned", "requestBridgeRecovered",
                    "pushRecovered", "errorFree")) {
                require(missing, bool(payload, field),
                        "Java rollback " + field);
            }
            require(missing, "development-server".equals(
                            text(payload, "mode")),
                    "Java rollback development-server mode");
            require(missing,
                    payload.path("recoveryGeneration").asInt() >= 1,
                    "Java rollback generation");
            require(missing,
                    payload.path("previousProcessIdentifier").asLong() > 0
                            && payload.path("processIdentifier").asLong() > 0
                            && payload.path("previousProcessIdentifier").asLong()
                            != payload.path("processIdentifier").asLong(),
                    "Java rollback replaced sidecar process");
        }

        private void requireSidecarReplacement(List<String> missing) {
            JsonNode payload = sidecarReplacementReady;
            require(missing, payload != null,
                    "planned sidecar replacement ready");
            if (payload == null) {
                return;
            }
            require(missing, bool(payload, "processReplaced"),
                    "planned replacement process replaced");
            require(missing, payload.path("windowsReloaded").asInt() >= 1,
                    "planned replacement window reload");
            require(missing, "development-inputs-rebuilt".equals(
                            text(payload, "reason")),
                    "planned development-input replacement reason");
            require(missing,
                    payload.path("recoveryGeneration").asInt() >= 1,
                    "planned replacement generation");
            require(missing,
                    payload.path("previousProcessIdentifier").asLong() > 0
                            && payload.path("processIdentifier").asLong() > 0
                            && payload.path("previousProcessIdentifier").asLong()
                            != payload.path("processIdentifier").asLong(),
                    "planned replacement process identifiers");
        }

        private void requireJavaReloadAcceptance(List<String> missing) {
            JsonNode payload = javaReloadAcceptance;
            require(missing, payload != null,
                    "Java reload acceptance result");
            if (payload == null) {
                return;
            }
            for (String field : List.of(
                    "complete", "baselineObserved", "updatedObserved",
                    "sourceRestored", "processReplaced", "windowPreserved",
                    "webViewPreserved", "webViewReloaded", "flowClientOwned",
                    "requestBridgeRecovered", "pushRecovered",
                    "errorFree")) {
                require(missing, bool(payload, field),
                        "Java reload " + field);
            }
            require(missing, "development-server".equals(
                            text(payload, "mode")),
                    "Java reload development-server mode");
            require(missing,
                    payload.path("recoveryGeneration").asInt() >= 1,
                    "Java reload generation");
            require(missing,
                    payload.path("previousProcessIdentifier").asLong() > 0
                            && payload.path("processIdentifier").asLong() > 0
                            && payload.path("previousProcessIdentifier").asLong()
                            != payload.path("processIdentifier").asLong(),
                    "Java reload replaced sidecar process");
        }

        private void requireReactorReloadAcceptance(
                List<String> missing) {
            JsonNode payload = reactorReloadAcceptance;
            require(missing, payload != null,
                    "reactor reload acceptance result");
            if (payload == null) {
                return;
            }
            for (String field : List.of(
                    "complete", "baselineObserved", "updatedObserved",
                    "configurationUpdatedObserved",
                    "sourceUpdatedObserved", "resourceUpdatedObserved",
                    "configurationRestored",
                    "sourceRestored", "resourceRestored",
                    "buildConfigurationObserved",
                    "dependentModuleObserved",
                    "dependentResourceObserved",
                    "effectiveMavenModelObserved",
                    "profileActivatedModuleObserved",
                    "customSourceRootObserved",
                    "customResourceRootObserved",
                    "processReplaced", "windowPreserved",
                    "webViewPreserved", "webViewReloaded",
                    "flowClientOwned", "requestBridgeRecovered",
                    "pushRecovered", "errorFree")) {
                require(missing, bool(payload, field),
                        "reactor reload " + field);
            }
            require(missing, "development-server".equals(
                            text(payload, "mode")),
                    "reactor reload development-server mode");
            require(missing,
                    payload.path("recoveryGeneration").asInt() == 3,
                    "exactly three reactor reload generations");
            require(missing,
                    payload.path("replacementCount").asInt() == 3,
                    "exactly three effective Maven model replacements");
            require(missing,
                    payload.path("previousProcessIdentifier").asLong() > 0
                            && payload.path("processIdentifier").asLong() > 0
                            && payload.path(
                            "previousProcessIdentifier").asLong()
                            != payload.path(
                            "processIdentifier").asLong(),
                    "reactor reload replaced sidecar process");
        }

        private void requireViteAcceptance(List<String> missing) {
            JsonNode payload = viteAcceptance;
            require(missing, payload != null, "Vite HMR acceptance result");
            if (payload == null) {
                return;
            }
            for (String field : List.of(
                    "complete", "viteConnected", "baselineObserved", "updatedObserved",
                    "sourceRestored", "flowClientOwned",
                    "opaqueRequestBridgeObserved", "opaqueResponseBridgeObserved",
                    "errorFree")) {
                require(missing, bool(payload, field), "Vite " + field);
            }
            require(missing, "development-server".equals(text(payload, "mode")),
                    "Vite development-server mode");
        }

        private void requirePushRecovery(List<String> missing) {
            JsonNode payload = pushRecoveryAcceptance;
            require(missing, payload != null, "push recovery acceptance result");
            if (payload != null) {
                for (String field : List.of(
                        "complete", "standardBundleMode", "rootRendered",
                        "flowClientOwned", "opaqueRequestBridgeObserved",
                        "opaqueResponseBridgeObserved", "pushBridgeInstalled",
                        "initialPushObserved", "reconnectObserved",
                        "gapDetected", "resyncObserved", "connectionPreserved",
                        "postResyncPushObserved", "postRecoveryDomObserved",
                        "errorFree")) {
                    require(missing, bool(payload, field),
                            "push recovery " + field);
                }
            }
            require(missing, pushConnectCount >= 2,
                    "initial and reopened push connect envelopes");
            require(missing, pushConnectedCount >= 2,
                    "initial and reopened push connected envelopes");
            require(missing, pushDisconnectCount >= 1,
                    "scoped push disconnect envelope");
            require(missing, pushResyncRequestCount >= 1,
                    "push gap resync request envelope");
            require(missing, pushResyncResponseCount >= 1,
                    "server resync push envelope");
            require(missing, pushInitialMessageObserved,
                    "initial normal push before disconnect");
            require(missing, pushDisconnectAfterMessage,
                    "disconnect after initial push");
            require(missing, pushReconnectAfterDisconnect,
                    "reconnect after disconnect");
            require(missing,
                    reconnectLastSeenServerSyncId
                            > initialPushLastSeenServerSyncId,
                    "advanced last-seen server sync id on reconnect");
            require(missing, pushResyncRequestAfterReconnect,
                    "gap resync request after reconnect");
            require(missing, pushResyncResponseAfterRequest,
                    "server resync response after request");
            require(missing, pushAfterResync,
                    "normal push after resync response");
            require(missing,
                    initialPushSequence >= 0
                            && resyncPushSequence > initialPushSequence
                            && postResyncPushSequence > resyncPushSequence,
                    "strictly increasing recovery push sequence");
            require(missing, pushRecoveryConnectionConsistent,
                    "consistent push window/session/UI/connection scope");
        }

        private void requirePushDelivery(List<String> missing) {
            JsonNode payload = pushDeliveryAcceptance;
            require(missing, payload != null,
                    "push delivery acceptance result");
            if (payload != null) {
                for (String field : List.of(
                        "complete", "standardBundleMode", "rootRendered",
                        "flowClientOwned", "opaqueRequestBridgeObserved",
                        "opaqueResponseBridgeObserved", "pushBridgeInstalled",
                        "baselineAcknowledged", "firstDeliveryDropped",
                        "sameSequenceRedelivered",
                        "flowLastSeenAcknowledged", "terminalDomObserved",
                        "connectionPreserved", "errorFree")) {
                    require(missing, bool(payload, field),
                            "push delivery " + field);
                }
            }
            require(missing, pushAcknowledgementCount >= 1,
                    "opaque push acknowledgement envelope");
            require(missing, pushSameSequenceRedelivery,
                    "same-sequence opaque push redelivery");
            require(missing, pushRedeliveryAcknowledged,
                    "redelivered push Flow last-seen acknowledgement");
            require(missing, pushDeliveryScopeConsistent,
                    "consistent redelivery acknowledgement scope");
            if (payload != null) {
                require(missing,
                        pushRedeliverySequence > 0
                                && pushRedeliverySequence
                                == payload.path("droppedSequence").asLong(-1),
                        "dropped and redelivered push sequence correlation");
            }
        }

        private void requireLargeDownloadResume(List<String> missing) {
            JsonNode payload = largeDownloadAcceptance;
            require(missing, payload != null,
                    "large download acceptance result");
            if (payload != null) {
                for (String field : List.of(
                        "complete", "standardBundleMode", "rootRendered",
                        "flowClientOwned", "opaqueRequestBridgeObserved",
                        "opaqueResponseBridgeObserved", "pushObserved",
                        "largeDownloadInitialResponseObserved",
                        "largeDownloadCancellationObserved",
                        "largeDownloadPartialResponseObserved",
                        "largeDownloadContentRangeObserved",
                        "largeDownloadExactReassemblyObserved",
                        "largeDownloadServerCancellationObserved",
                        "largeDownloadJavaListenerObserved", "errorFree")) {
                    require(missing, bool(payload, field),
                            "large download " + field);
                }
                long totalBytes = payload.path("totalBytes").asLong(-1);
                long cancelledBytes = payload.path("cancelledBytes").asLong(-1);
                long resumedBytes = payload.path("resumedBytes").asLong(-1);
                require(missing, payload.path("initialStatus").asInt() == 200,
                        "large download initial HTTP 200 response");
                require(missing, payload.path("resumeStatus").asInt() == 206,
                        "large download resumed HTTP 206 response");
                require(missing, totalBytes > 4L * 1024 * 1024,
                        "large download total byte count");
                require(missing,
                        cancelledBytes >= 256L * 1024
                                && cancelledBytes < totalBytes,
                        "large download bounded cancellation offset");
                require(missing,
                        resumedBytes == totalBytes - cancelledBytes,
                        "large download resumed byte count");
            }
            require(missing, opaqueResponseCancelCount >= 1,
                    "opaque response cancellation envelope");
            require(missing, opaqueRangeRequestCount >= 1,
                    "opaque Range request envelope");
            require(missing, opaquePartialResponseCount >= 1,
                    "opaque HTTP 206 response envelope");
            require(missing, opaqueLargeResponseEndCount >= 1,
                    "complete large opaque response envelope");
            require(missing,
                    opaqueCancelledRequestIds.stream().anyMatch(
                            opaqueSuccessfulResponseRequestIds::contains),
                    "correlated successful response cancellation");
            require(missing,
                    opaqueRangeRequestIds.stream().anyMatch(requestId ->
                            opaquePartialResponseRequestIds.contains(requestId)
                                    && opaqueLargeResponseEndRequestIds.contains(
                                    requestId)),
                    "correlated Range, HTTP 206 and large response completion");
        }

        private void requireLargeUploadRetry(List<String> missing) {
            JsonNode payload = largeUploadAcceptance;
            require(missing, payload != null,
                    "large upload acceptance result");
            if (payload != null) {
                for (String field : List.of(
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
                        "largeUploadJavaListenerObserved", "errorFree")) {
                    require(missing, bool(payload, field),
                            "large upload " + field);
                }
                long totalBytes = payload.path("totalBytes").asLong(-1);
                long cancelThreshold = payload.path("cancelThreshold").asLong(-1);
                long firstProgressBytes =
                        payload.path("firstProgressBytes").asLong(-1);
                long retryProgressBytes =
                        payload.path("retryProgressBytes").asLong(-1);
                long serverCancelledBytes =
                        payload.path("serverCancelledBytes").asLong(-1);
                require(missing, totalBytes > 4L * 1024 * 1024,
                        "large upload total byte count");
                require(missing,
                        cancelThreshold >= 256L * 1024
                                && firstProgressBytes >= cancelThreshold
                                && firstProgressBytes < totalBytes,
                        "large upload bounded progress cancellation");
                require(missing, retryProgressBytes == totalBytes,
                        "large upload retry progress byte count");
                require(missing,
                        serverCancelledBytes >= 128L * 1024
                                && serverCancelledBytes < totalBytes,
                        "large upload server cancellation byte count");
                require(missing,
                        payload.path("serverSuccessBytes").asLong(-1)
                                == totalBytes,
                        "large upload successful body byte count");
                require(missing,
                        payload.path("serverDeclaredBytes").asLong(-1)
                                == totalBytes,
                        "large upload declared body byte count");
                require(missing, payload.path("startEvents").asInt(-1) == 2,
                        "large upload two start events");
                require(missing, payload.path("abortEvents").asInt(-1) == 1,
                        "large upload one abort event");
                require(missing, payload.path("removeEvents").asInt(-1) == 1,
                        "large upload one removal event");
                require(missing, payload.path("successEvents").asInt(-1) == 1,
                        "large upload one success event");
                require(missing, payload.path("errorEvents").asInt(-1) == 0,
                        "large upload zero error events");
            }
            require(missing, opaqueLargeRequestCount >= 2,
                    "two large opaque request open envelopes");
            require(missing, opaqueRawUploadRequestIds.size() >= 2,
                    "two large raw upload request envelopes");
            require(missing, opaqueLargeRequestEndCount >= 1,
                    "complete large opaque request envelope");
            require(missing,
                    opaqueRawUploadRequestIds.stream().anyMatch(
                            opaqueCancelledRequestIds::contains),
                    "correlated large raw upload cancellation");
            require(missing,
                    opaqueRawUploadRequestIds.stream().anyMatch(requestId ->
                            !opaqueCancelledRequestIds.contains(requestId)
                                    && opaqueLargeRequestEndIds.contains(requestId)
                                    && opaqueSuccessfulResponseRequestIds.contains(
                                    requestId)),
                    "correlated large raw upload completion and response");
        }

        private void requireInFlightRequestRecovery(List<String> missing) {
            JsonNode payload = inFlightRequestRecoveryAcceptance;
            require(missing, payload != null,
                    "in-flight request recovery acceptance result");
            if (payload != null) {
                for (String field : List.of(
                        "complete", "standardBundleMode",
                        "interruptedRequestInFlight",
                        "interruptedRequestFailed",
                        "oldDocumentSidecarDisconnected",
                        "oldDocumentLateRequestRejected",
                        "oldDocumentRequestCountStable",
                        "oldDocumentPushRejected", "processRestarted",
                        "windowPreserved", "webViewPreserved",
                        "webViewReloaded", "freshFlowClientOwned",
                        "requestBridgeRecovered", "pushRecovered",
                        "recoveredRequestComplete",
                        "recoveredRequestExactBytes",
                        "javaConfirmationObserved", "errorFree")) {
                    require(missing, bool(payload, field),
                            "in-flight request recovery " + field);
                }
                long totalBytes = payload.path("totalBytes").asLong(-1);
                long interruptThreshold = payload.path(
                        "interruptThreshold").asLong(-1);
                long interruptedProgress = payload.path(
                        "interruptedProgressBytes").asLong(-1);
                require(missing, totalBytes > 4L * 1024 * 1024,
                        "in-flight request total byte count");
                require(missing,
                        interruptThreshold >= 256L * 1024
                                && interruptedProgress >= interruptThreshold
                                && interruptedProgress < totalBytes,
                        "in-flight request bounded interrupted progress");
                require(missing,
                        payload.path("disconnectedRequestCount").asInt() >= 1,
                        "old document disconnected request rejection count");
                require(missing,
                        payload.path("disconnectedPushOutboundCount").asInt()
                                >= 2,
                        "old document disconnected push rejection count");
                require(missing,
                        payload.path("recoveryGeneration").asInt() >= 1,
                        "in-flight request recovery generation");
                int previousProcess = payload.path(
                        "previousProcessIdentifier").asInt();
                int recoveredProcess = payload.path(
                        "processIdentifier").asInt();
                require(missing,
                        previousProcess > 0 && recoveredProcess > 0
                                && previousProcess != recoveredProcess,
                        "in-flight request replaced Java process");
            }

            require(missing, inFlightFailureInjectionOrder > 0,
                    "in-flight request sidecar failure injection");
            require(missing, inFlightSidecarExitOrder
                            > inFlightFailureInjectionOrder,
                    "in-flight request sidecar exit after failure injection");
            require(missing, inFlightRecoveryReadyOrder
                            > inFlightSidecarExitOrder,
                    "replacement sidecar ready after interrupted request");
            require(missing, sidecarRecoveryScheduledAttempts.equals(List.of(1)),
                    "single scheduled recovery attempt");
            require(missing, sidecarRecoveryReadyAttempts.equals(List.of(1)),
                    "single ready recovery attempt");
            require(missing, oldDocumentProbeOpenCount == 0,
                    "no old-document opaque request envelope");
            require(missing,
                    opaqueRequestOpenEventOrders.stream().noneMatch(order ->
                            order > inFlightFailureInjectionOrder
                                    && order < inFlightRecoveryReadyOrder),
                    "no old-document opaque request during recovery");
            require(missing,
                    pushConnectOrders.stream().noneMatch(order ->
                            order > inFlightFailureInjectionOrder
                                    && order < inFlightRecoveryReadyOrder),
                    "no old-document push connect during recovery");
            require(missing,
                    pushDisconnectOrders.stream().noneMatch(order ->
                            order > inFlightFailureInjectionOrder
                                    && order < inFlightRecoveryReadyOrder),
                    "no old-document push disconnect during recovery");

            require(missing, inFlightLargeRawRequests.size() == 2,
                    "exactly two large raw request generations");
            if (inFlightLargeRawRequests.size() == 2) {
                InFlightOpaqueRequestTrace interrupted =
                        inFlightLargeRawRequests.get(0);
                InFlightOpaqueRequestTrace recovered =
                        inFlightLargeRawRequests.get(1);
                require(missing,
                        interrupted.openOrder < inFlightFailureInjectionOrder,
                        "interrupted request opened before sidecar failure");
                require(missing,
                        interrupted.chunkBytes >= 256L * 1024
                                && interrupted.chunkBytes
                                < interrupted.contentLength,
                        "interrupted request partial opaque body");
                require(missing, interrupted.endOrder == 0,
                        "interrupted request has no request end");
                require(missing, interrupted.cancelOrder == 0,
                        "interrupted request emits no cancel to dead sidecar");
                require(missing, interrupted.responseOrder == 0,
                        "interrupted request has no response");
                require(missing,
                        recovered.openOrder > inFlightRecoveryReadyOrder,
                        "recovered request opened after replacement ready");
                require(missing,
                        recovered.contentLength > 4L * 1024 * 1024
                                && recovered.chunkBytes
                                == recovered.contentLength
                                && recovered.endBodyBytes
                                == recovered.contentLength,
                        "recovered request exact opaque body");
                require(missing,
                        recovered.endOrder > recovered.openOrder,
                        "recovered request end after open");
                require(missing,
                        recovered.responseStatus >= 200
                                && recovered.responseStatus < 300,
                        "recovered request successful response");
                require(missing,
                        recovered.responseOrder > recovered.endOrder
                                && recovered.responseEndOrder
                                > recovered.responseOrder,
                        "recovered response completed after request body");
                if (payload != null) {
                    require(missing,
                            interrupted.requestId.equals(
                                    text(payload, "interruptedRequestId")),
                            "interrupted request id correlation");
                    require(missing,
                            recovered.requestId.equals(
                                    text(payload, "recoveredRequestId")),
                            "recovered request id correlation");
                }
            }
        }

        private void requireLargeMultipartUploadRetry(List<String> missing) {
            JsonNode payload = largeMultipartUploadAcceptance;
            require(missing, payload != null,
                    "large multipart upload acceptance result");
            if (payload == null) {
                return;
            }
            for (String field : List.of(
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
                    "largeMultipartUploadJavaListenersObserved",
                    "errorFree")) {
                require(missing, bool(payload, field),
                        "large multipart upload " + field);
            }

            long totalFileBytes =
                    payload.path("totalFileBytes").asLong(-1);
            long cancelThreshold =
                    payload.path("cancelThreshold").asLong(-1);
            long firstProgressBytes =
                    payload.path("firstProgressBytes").asLong(-1);
            long retryProgressBytes =
                    payload.path("retryProgressBytes").asLong(-1);
            require(missing, totalFileBytes > 4L * 1024 * 1024,
                    "large multipart upload file byte count");
            require(missing,
                    cancelThreshold >= 256L * 1024
                            && firstProgressBytes >= cancelThreshold
                            && firstProgressBytes < totalFileBytes,
                    "large multipart upload bounded progress cancellation");
            require(missing,
                    retryProgressBytes >= totalFileBytes
                            && retryProgressBytes
                            < totalFileBytes + 64L * 1024,
                    "large multipart upload framed retry progress");
            require(missing,
                    payload.path("progressEvents").asInt(-1) >= 3
                            && payload.path("startEvents").asInt(-1) == 2
                            && payload.path("requestEvents").asInt(-1) == 2
                            && payload.path("multipartRequestEvents")
                            .asInt(-1) == 2
                            && payload.path("multipartEntryEvents")
                            .asInt(-1) == 2
                            && payload.path("abortEvents").asInt(-1) == 1
                            && payload.path("removeEvents").asInt(-1) == 1
                            && payload.path("successEvents").asInt(-1) == 1
                            && payload.path("errorEvents").asInt(-1) == 0,
                    "large multipart upload browser event cardinality");
            require(missing,
                    payload.path("serverHandlerCalls").asInt(-1) == 1
                            && payload.path("serverHandlerFailures")
                            .asInt(-1) == 0
                            && payload.path("serverResponseCalls")
                            .asInt(-1) == 1
                            && payload.path("serverFailedResponses")
                            .asInt(-1) == 0
                            && payload.path("serverFailedAcceptedFiles")
                            .asInt(-1) == -1
                            && !bool(payload, "failedResponseHasException")
                            && text(payload, "failureType").isBlank()
                            && payload.path("serverSuccessfulResponses")
                            .asInt(-1) == 1
                            && payload.path("serverSuccessfulAcceptedFiles")
                            .asInt(-1) == 1,
                    "large multipart upload cancellation isolation");
            require(missing,
                    payload.path("serverSuccessfulBytes").asLong(-1)
                            == totalFileBytes
                            && payload.path("serverDeclaredBytes").asLong(-1)
                            == totalFileBytes
                            && payload.path("serverSpillBytes").asLong(-1)
                            == totalFileBytes,
                    "large multipart upload exact parsed part");

            require(missing, opaqueMultipartUploadRequestIds.size() == 2,
                    "two multipart opaque upload request opens");
            Set<String> cancelledIds = new java.util.HashSet<>(
                    opaqueMultipartUploadRequestIds);
            cancelledIds.retainAll(opaqueCancelledRequestIds);
            require(missing, cancelledIds.size() == 1,
                    "one correlated multipart request cancellation");
            if (cancelledIds.size() != 1) {
                return;
            }
            String cancelledId = cancelledIds.iterator().next();
            Set<String> completedIds = new java.util.HashSet<>(
                    opaqueMultipartUploadRequestIds);
            completedIds.remove(cancelledId);
            completedIds.removeIf(requestId ->
                    !opaqueRequestEndBodyBytes.containsKey(requestId)
                            || !opaqueSuccessfulResponseRequestIds.contains(
                            requestId));
            require(missing, completedIds.size() == 1,
                    "one correlated multipart retry completion and response");
            if (completedIds.size() != 1) {
                return;
            }
            String completedId = completedIds.iterator().next();
            require(missing,
                    !opaqueRequestEndBodyBytes.containsKey(cancelledId)
                            && !opaqueSuccessfulResponseRequestIds.contains(
                            cancelledId)
                            && opaqueRequestChunkBytes.getOrDefault(
                            cancelledId, -1L) == firstProgressBytes,
                    "multipart cancellation has no terminal success");
            require(missing,
                    opaqueRequestChunkBytes.getOrDefault(completedId, -1L)
                            == retryProgressBytes
                            && opaqueRequestEndBodyBytes.getOrDefault(
                            completedId, -1L) == retryProgressBytes
                            && opaqueResponseStatuses.getOrDefault(
                            completedId, -1) >= 200
                            && opaqueResponseStatuses.getOrDefault(
                            completedId, -1) < 400,
                    "multipart retry chunk, end and response correlation");
            Integer cancelledTerminal =
                    opaqueRequestCancelOrders.get(cancelledId);
            Integer retryOpen = opaqueRequestOpenOrders.get(completedId);
            require(missing,
                    cancelledTerminal != null && retryOpen != null
                            && cancelledTerminal < retryOpen,
                    "multipart retry opened after cancellation");
        }

        private void requireConcurrentUpload(List<String> missing) {
            JsonNode payload = concurrentUploadAcceptance;
            require(missing, payload != null,
                    "concurrent upload acceptance result");
            if (payload == null) {
                return;
            }
            for (String field : List.of(
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
                    "concurrentUploadJavaListenersObserved", "errorFree")) {
                require(missing, bool(payload, field),
                        "concurrent upload " + field);
            }

            long cancelledTotal =
                    payload.path("cancelledTotalBytes").asLong(-1);
            long survivorTotal =
                    payload.path("survivorTotalBytes").asLong(-1);
            long cancelThreshold =
                    payload.path("cancelThreshold").asLong(-1);
            long overlapThreshold =
                    payload.path("overlapThreshold").asLong(-1);
            long cancelledProgress =
                    payload.path("cancelledProgressBytes").asLong(-1);
            long survivorProgress =
                    payload.path("survivorProgressBytes").asLong(-1);
            long serverCancelledBytes =
                    payload.path("serverCancelledBytes").asLong(-1);

            require(missing, cancelledTotal > 4L * 1024 * 1024,
                    "concurrent upload cancelled body size");
            require(missing, survivorTotal > 3L * 1024 * 1024,
                    "concurrent upload survivor body size");
            require(missing,
                    cancelThreshold >= 256L * 1024
                            && overlapThreshold >= 64L * 1024
                            && cancelledProgress >= cancelThreshold
                            && cancelledProgress < cancelledTotal
                            && survivorProgress == survivorTotal,
                    "concurrent upload bounded browser progress");
            require(missing,
                    payload.path("progressEvents").asInt(-1) >= 3
                            && payload.path("startEvents").asInt(-1) == 2
                            && payload.path("abortEvents").asInt(-1) == 1
                            && payload.path("removeEvents").asInt(-1) == 1
                            && payload.path("successEvents").asInt(-1) == 1
                            && payload.path("errorEvents").asInt(-1) == 0,
                    "concurrent upload browser event cardinality");
            require(missing,
                    payload.path("browserMaximumActive").asInt(-1) >= 2
                            && payload.path("serverMaximumActive").asInt(-1) >= 2,
                    "concurrent upload overlapping browser and server requests");
            require(missing,
                    serverCancelledBytes >= overlapThreshold
                            && serverCancelledBytes < cancelledTotal
                            && payload.path("serverCancelledDeclaredBytes")
                            .asLong(-1) == cancelledTotal,
                    "concurrent upload bounded server cancellation");
            require(missing,
                    payload.path("serverSuccessBytes").asLong(-1)
                            == survivorTotal
                            && payload.path("serverSuccessDeclaredBytes")
                            .asLong(-1) == survivorTotal,
                    "concurrent upload exact survivor body");

            Set<String> cancelledIds =
                    rawRequestIdsWithLength(cancelledTotal);
            Set<String> survivorIds = rawRequestIdsWithLength(survivorTotal);
            require(missing, cancelledIds.size() == 1,
                    "one correlated concurrent cancelled request open");
            require(missing, survivorIds.size() == 1,
                    "one correlated concurrent survivor request open");
            if (cancelledIds.size() != 1 || survivorIds.size() != 1) {
                return;
            }

            String cancelledId = cancelledIds.iterator().next();
            String survivorId = survivorIds.iterator().next();
            require(missing, !cancelledId.equals(survivorId),
                    "distinct concurrent opaque request identifiers");
            require(missing,
                    opaqueRequestCancelOrders.containsKey(cancelledId)
                            && !opaqueRequestEndBodyBytes.containsKey(cancelledId)
                            && !opaqueSuccessfulResponseRequestIds.contains(
                            cancelledId),
                    "correlated concurrent request cancellation");
            require(missing,
                    opaqueRequestEndBodyBytes.getOrDefault(survivorId, -1L)
                            == survivorTotal
                            && opaqueSuccessfulResponseRequestIds.contains(
                            survivorId)
                            && opaqueResponseStatuses.getOrDefault(
                            survivorId, -1) >= 200
                            && opaqueResponseStatuses.getOrDefault(
                            survivorId, -1) < 400,
                    "correlated concurrent survivor completion and response");

            Integer cancelledOpen = opaqueRequestOpenOrders.get(cancelledId);
            Integer survivorOpen = opaqueRequestOpenOrders.get(survivorId);
            Integer cancelledTerminal =
                    opaqueRequestCancelOrders.get(cancelledId);
            Integer survivorTerminal = opaqueRequestEndOrders.get(survivorId);
            require(missing,
                    cancelledOpen != null && survivorOpen != null
                            && cancelledTerminal != null
                            && survivorTerminal != null
                            && Math.max(cancelledOpen, survivorOpen)
                            < Math.min(cancelledTerminal, survivorTerminal),
                    "concurrent requests opened before either terminal frame");
        }

        private void requireRequestFanout(List<String> missing) {
            JsonNode payload = requestFanoutAcceptance;
            require(missing, payload != null,
                    "request fan-out acceptance result");
            if (payload == null) {
                return;
            }
            int batchSize = 6;
            int batchCount = 10;
            int requestCount = batchSize * batchCount;
            int cancelledCount = 2 * batchCount;
            int survivorCount = requestCount - cancelledCount;
            for (String field : List.of(
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
                    "cancellationTombstoneExpired", "errorFree")) {
                require(missing, bool(payload, field),
                        "request fan-out " + field);
            }

            JsonNode lengthsNode = payload.path("fileLengths");
            JsonNode cancelledLengthsNode =
                    payload.path("cancelledFileLengths");
            JsonNode survivorLengthsNode =
                    payload.path("survivorFileLengths");
            JsonNode cancelThresholdsNode =
                    payload.path("cancelThresholds");
            JsonNode progressNode = payload.path("progressBytes");
            JsonNode requestIdsNode = payload.path("requestIds");
            JsonNode firstBatchRequestIdsNode =
                    payload.path("firstBatchRequestIds");
            JsonNode secondBatchRequestIdsNode =
                    payload.path("secondBatchRequestIds");
            JsonNode batchStartEventsNode = payload.path("batchStartEvents");
            JsonNode batchTerminalEventsNode =
                    payload.path("batchTerminalEvents");
            require(missing,
                    lengthsNode.isArray()
                            && lengthsNode.size() == requestCount,
                    "sixty request fan-out body lengths");
            require(missing,
                    cancelledLengthsNode.isArray()
                            && cancelledLengthsNode.size() == cancelledCount,
                    "twenty request fan-out cancellation lengths");
            require(missing,
                    survivorLengthsNode.isArray()
                            && survivorLengthsNode.size() == survivorCount,
                    "forty request fan-out survivor lengths");
            require(missing,
                    cancelThresholdsNode.isArray()
                            && cancelThresholdsNode.size() == requestCount
                            && progressNode.isArray()
                            && progressNode.size() == requestCount,
                    "sixty request fan-out thresholds and progress values");
            require(missing,
                    requestIdsNode.isArray()
                            && requestIdsNode.size() == requestCount
                            && firstBatchRequestIdsNode.isArray()
                            && firstBatchRequestIdsNode.size() == batchSize
                            && secondBatchRequestIdsNode.isArray()
                            && secondBatchRequestIdsNode.size() == batchSize,
                    "sixty correlated requests across ten fan-out batches");
            require(missing,
                    payload.path("batchCount").asInt(-1) == batchCount
                            && payload.path("completedBatches").asInt(-1)
                            == batchCount
                            && batchCountsEqual(
                            batchStartEventsNode, batchCount, batchSize)
                            && batchCountsEqual(
                            batchTerminalEventsNode, batchCount, batchSize),
                    "ten completed request fan-out batch boundaries");
            if (!lengthsNode.isArray()
                    || lengthsNode.size() != requestCount
                    || !cancelledLengthsNode.isArray()
                    || cancelledLengthsNode.size() != cancelledCount
                    || !survivorLengthsNode.isArray()
                    || survivorLengthsNode.size() != survivorCount
                    || !cancelThresholdsNode.isArray()
                    || cancelThresholdsNode.size() != requestCount
                    || !progressNode.isArray()
                    || progressNode.size() != requestCount
                    || !requestIdsNode.isArray()
                    || requestIdsNode.size() != requestCount
                    || !firstBatchRequestIdsNode.isArray()
                    || firstBatchRequestIdsNode.size() != batchSize
                    || !secondBatchRequestIdsNode.isArray()
                    || secondBatchRequestIdsNode.size() != batchSize) {
                return;
            }

            Set<Long> lengths = new java.util.LinkedHashSet<>();
            Set<Long> firstBatchLengths = new java.util.LinkedHashSet<>();
            Set<Long> secondBatchLengths = new java.util.LinkedHashSet<>();
            Set<Long> churnLengths = new java.util.LinkedHashSet<>();
            Set<Long> cancelledLengths = new java.util.LinkedHashSet<>();
            Set<Long> survivorLengths = new java.util.LinkedHashSet<>();
            for (int index = 0; index < lengthsNode.size(); index++) {
                long length = lengthsNode.get(index).asLong(-1);
                lengths.add(length);
                if (index < batchSize) {
                    firstBatchLengths.add(length);
                } else if (index < 2 * batchSize) {
                    secondBatchLengths.add(length);
                } else {
                    churnLengths.add(length);
                }
            }
            for (JsonNode length : cancelledLengthsNode) {
                cancelledLengths.add(length.asLong(-1));
            }
            for (JsonNode length : survivorLengthsNode) {
                survivorLengths.add(length.asLong(-1));
            }
            Set<Long> partitionedLengths = new java.util.LinkedHashSet<>();
            partitionedLengths.addAll(cancelledLengths);
            partitionedLengths.addAll(survivorLengths);
            require(missing,
                    lengths.size() == requestCount
                            && firstBatchLengths.size() == batchSize
                            && firstBatchLengths.stream().allMatch(length ->
                            length > 4L * 1024 * 1024)
                            && secondBatchLengths.size() == batchSize
                            && secondBatchLengths.stream().allMatch(length ->
                            length > 2L * 1024 * 1024)
                            && churnLengths.size()
                            == requestCount - 2 * batchSize
                            && churnLengths.stream().allMatch(length ->
                            length > 256L * 1024),
                    "sixty unique raw request lengths across ten batches");
            require(missing,
                    cancelledLengths.size() == cancelledCount
                            && survivorLengths.size() == survivorCount
                            && partitionedLengths.equals(lengths),
                    "request fan-out cancellation and survivor partition");

            Map<Long, Long> cancelMinimumByLength = new LinkedHashMap<>();
            boolean progressValid = true;
            for (int index = 0; index < lengthsNode.size(); index++) {
                long length = lengthsNode.get(index).asLong(-1);
                long cancelThreshold =
                        cancelThresholdsNode.get(index).asLong(-1);
                long progress = progressNode.get(index).asLong(-1);
                long expectedThreshold = index < 2 * batchSize
                        ? 512L * 1024 : 192L * 1024;
                progressValid &= cancelThreshold == expectedThreshold
                        && cancelThreshold > 128L * 1024;
                cancelMinimumByLength.put(length,
                        index < 2 * batchSize
                                ? 512L * 1024 : 160L * 1024);
                if (cancelledLengths.contains(length)) {
                    progressValid &= progress >= cancelThreshold
                            && progress < length;
                } else {
                    progressValid &= progress == length;
                }
            }
            require(missing, progressValid,
                    "request fan-out progress beyond initial credit");
            require(missing,
                    payload.path("startEvents").asInt(-1) == requestCount
                            && payload.path("abortEvents").asInt(-1)
                            == cancelledCount
                            && payload.path("removeEvents").asInt(-1)
                            == cancelledCount
                            && payload.path("successEvents").asInt(-1)
                            == survivorCount
                            && payload.path("errorEvents").asInt(-1) == 0
                            && payload.path("cancelIssuedCount").asInt(-1)
                            == cancelledCount,
                    "request fan-out browser event cardinality");
            require(missing,
                    payload.path("maxConcurrentUploads").asInt(-1) == 3
                            && payload.path("browserMaximumActive").asInt(-1)
                            == 3
                            && payload.path("serverMaximumActive").asInt(-1)
                            == 3
                            && payload.path("allBeyondThreshold").asBoolean(),
                    "request fan-out bounded browser and Java queue overlap");
            long serverCancelledMinimum =
                    payload.path("serverCancelledMinimum").asLong(-1);
            long serverCancelledMaximum =
                    payload.path("serverCancelledMaximum").asLong(-1);
            long maximumCancelledLength = cancelledLengths.stream()
                    .mapToLong(Long::longValue).max().orElse(-1);
            require(missing,
                    serverCancelledMinimum >= 160L * 1024
                            && serverCancelledMaximum >= 512L * 1024
                            && serverCancelledMaximum
                            < maximumCancelledLength,
                    "request fan-out bounded Java cancellation");
            long expectedSuccessBytes = survivorLengths.stream()
                    .mapToLong(Long::longValue).sum();
            require(missing,
                    payload.path("serverSuccessBytes").asLong(-1)
                            == expectedSuccessBytes
                            && payload.path("serverExpectedSuccessBytes")
                            .asLong(-1) == expectedSuccessBytes,
                    "request fan-out exact Java survivor bytes");

            Map<Long, String> requestIdsByLength = new LinkedHashMap<>();
            for (long length : lengths) {
                Set<String> requestIds = rawRequestIdsWithLength(length);
                require(missing, requestIds.size() == 1,
                        "one raw request with fan-out length " + length);
                if (requestIds.size() == 1) {
                    requestIdsByLength.put(
                            length, requestIds.iterator().next());
                }
            }
            require(missing, requestIdsByLength.size() == requestCount
                            && new java.util.HashSet<>(
                            requestIdsByLength.values()).size() == requestCount,
                    "sixty distinct correlated fan-out request identifiers");
            if (requestIdsByLength.size() != requestCount) {
                return;
            }

            boolean browserRequestIdsMatchTrace = true;
            List<Set<String>> requestIdBatches = new ArrayList<>();
            for (int batch = 0; batch < batchCount; batch++) {
                requestIdBatches.add(new java.util.LinkedHashSet<>());
            }
            Set<String> cancelledRequestIds = new java.util.LinkedHashSet<>();
            Set<String> survivorRequestIds = new java.util.LinkedHashSet<>();
            for (int index = 0; index < lengthsNode.size(); index++) {
                long length = lengthsNode.get(index).asLong(-1);
                String requestId = requestIdsByLength.get(length);
                browserRequestIdsMatchTrace &= requestId != null
                        && requestId.equals(requestIdsNode.get(index).asText(""));
                if (requestId == null) {
                    continue;
                }
                requestIdBatches.get(index / batchSize).add(requestId);
                if (index < batchSize) {
                    browserRequestIdsMatchTrace &= requestId.equals(
                            firstBatchRequestIdsNode.get(index).asText(""));
                } else if (index < 2 * batchSize) {
                    browserRequestIdsMatchTrace &= requestId.equals(
                            secondBatchRequestIdsNode.get(index - batchSize)
                                    .asText(""));
                }
                (cancelledLengths.contains(length)
                        ? cancelledRequestIds : survivorRequestIds).add(requestId);
            }
            Set<String> firstBatchRequestIds = requestIdBatches.get(0);
            Set<String> secondBatchRequestIds = requestIdBatches.get(1);
            require(missing,
                    browserRequestIdsMatchTrace
                            && requestIdBatches.stream().allMatch(
                            requestIds -> requestIds.size() == batchSize)
                            && requestIdBatches.stream()
                            .flatMap(Set::stream).distinct().count()
                            == requestCount
                            && java.util.Collections.disjoint(
                            firstBatchRequestIds, secondBatchRequestIds),
                    "browser request registry ids correlated across ten batches");

            for (Map.Entry<Long, String> entry
                    : requestIdsByLength.entrySet()) {
                long length = entry.getKey();
                String requestId = entry.getValue();
                long chunkBytes =
                        opaqueRequestChunkBytes.getOrDefault(requestId, -1L);
                if (cancelledLengths.contains(length)) {
                    long cancelMinimum = cancelMinimumByLength
                            .getOrDefault(length, -1L);
                    Integer cancelOrder =
                            opaqueRequestCancelOrders.get(requestId);
                    require(missing,
                            cancelOrder != null
                                    && !opaqueRequestEndBodyBytes.containsKey(
                                    requestId)
                                    && !opaqueSuccessfulResponseRequestIds
                                    .contains(requestId)
                                    && chunkBytes >= cancelMinimum
                                    && chunkBytes < length,
                            "correlated fan-out request cancellation for "
                                    + length);
                } else {
                    Integer endOrder = opaqueRequestEndOrders.get(requestId);
                    require(missing,
                            endOrder != null
                                    && chunkBytes == length
                                    && opaqueRequestEndBodyBytes.getOrDefault(
                                    requestId, -1L) == length
                                    && opaqueSuccessfulResponseRequestIds
                                    .contains(requestId)
                                    && opaqueResponseStatuses.getOrDefault(
                                    requestId, -1) >= 200
                                    && opaqueResponseStatuses.getOrDefault(
                                    requestId, -1) < 400,
                            "correlated fan-out survivor completion for "
                                    + length);
                }
            }

            String ignoredLateResponseRequestId = payload.path(
                    "ignoredLateResponseRequestId").asText("");
            String reclaimedRequestId =
                    payload.path("reclaimedRequestId").asText("");
            require(missing,
                    payload.path("lateFrameProbeCount").asInt(-1) == 4
                            && firstBatchRequestIds.contains(
                            ignoredLateResponseRequestId)
                            && cancelledRequestIds.contains(
                            ignoredLateResponseRequestId)
                            && firstBatchRequestIds.contains(reclaimedRequestId)
                            && survivorRequestIds.contains(reclaimedRequestId)
                            && payload.path("registryProbeExpectedError")
                            .asText("").replaceAll("\\s+", "").equals(
                                    "unknownrequest" + reclaimedRequestId),
                    "request registry reclamation and cancelled late-response isolation");

            long tombstoneRetentionElapsedMillis = payload.path(
                    "tombstoneRetentionElapsedMillis").asLong(-1);
            long tombstoneExpiryElapsedMillis = payload.path(
                    "tombstoneExpiryElapsedMillis").asLong(-1);
            require(missing,
                    payload.path("tombstoneBoundaryProbeCount").asInt(-1) == 2
                            && tombstoneRetentionElapsedMillis >= 54_000
                            && tombstoneRetentionElapsedMillis < 60_000
                            && tombstoneExpiryElapsedMillis >= 60_000
                            && payload.path("tombstoneExpiryExpectedError")
                            .asText("").replaceAll("\\s+", "").equals(
                                    "unknownrequest"
                                            + ignoredLateResponseRequestId),
                    "bounded cancellation tombstone retention and expiry");

            require(missing,
                    requestIdBatches.stream().allMatch(
                            this::boundedRequestBatch),
                    "bounded queue replenishment in all ten fan-out batches");
            boolean batchesStrictlySequential = true;
            for (int batch = 1; batch < requestIdBatches.size(); batch++) {
                int latestPreviousTerminal = requestIdBatches.get(batch - 1)
                        .stream()
                        .map(this::opaqueRequestTerminalOrder)
                        .filter(java.util.Objects::nonNull)
                        .mapToInt(Integer::intValue).max().orElse(-1);
                int earliestCurrentOpen = requestIdBatches.get(batch).stream()
                        .map(opaqueRequestOpenOrders::get)
                        .filter(java.util.Objects::nonNull)
                        .mapToInt(Integer::intValue).min().orElse(-1);
                batchesStrictlySequential &= latestPreviousTerminal >= 0
                        && earliestCurrentOpen > latestPreviousTerminal;
            }
            require(missing,
                    batchesStrictlySequential,
                    "every fan-out batch terminated before the next opened");
        }

        private static boolean batchCountsEqual(
                JsonNode values, int count, int expected) {
            if (!values.isArray() || values.size() != count) {
                return false;
            }
            for (JsonNode value : values) {
                if (value.asInt(-1) != expected) {
                    return false;
                }
            }
            return true;
        }

        private Integer opaqueRequestTerminalOrder(String requestId) {
            Integer cancelOrder = opaqueRequestCancelOrders.get(requestId);
            return cancelOrder == null
                    ? opaqueRequestEndOrders.get(requestId) : cancelOrder;
        }

        private boolean boundedRequestBatch(Set<String> requestIds) {
            if (requestIds.size() != 6) {
                return false;
            }
            Map<Integer, Integer> activeDeltas = new java.util.TreeMap<>();
            int earliestTerminal = Integer.MAX_VALUE;
            int latestOpen = -1;
            int latestTerminal = -1;
            for (String requestId : requestIds) {
                Integer openOrder = opaqueRequestOpenOrders.get(requestId);
                Integer terminalOrder = opaqueRequestTerminalOrder(requestId);
                if (openOrder == null || terminalOrder == null) {
                    return false;
                }
                activeDeltas.merge(openOrder, 1, Integer::sum);
                activeDeltas.merge(terminalOrder, -1, Integer::sum);
                earliestTerminal = Math.min(earliestTerminal, terminalOrder);
                latestOpen = Math.max(latestOpen, openOrder);
                latestTerminal = Math.max(latestTerminal, terminalOrder);
            }
            int activeRequests = 0;
            int maximumActiveRequests = 0;
            for (int delta : activeDeltas.values()) {
                activeRequests += delta;
                maximumActiveRequests = Math.max(
                        maximumActiveRequests, activeRequests);
            }
            int opensBeforeFirstTerminal = 0;
            for (String requestId : requestIds) {
                if (opaqueRequestOpenOrders.get(requestId) < earliestTerminal) {
                    opensBeforeFirstTerminal++;
                }
            }
            return maximumActiveRequests == 3
                    && activeRequests == 0
                    && opensBeforeFirstTerminal == 3
                    && earliestTerminal < latestOpen
                    && latestOpen < latestTerminal;
        }

        private void requireConcurrentMultipartUpload(List<String> missing) {
            JsonNode payload = concurrentMultipartUploadAcceptance;
            require(missing, payload != null,
                    "concurrent multipart upload acceptance result");
            if (payload == null) {
                return;
            }
            for (String field : List.of(
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
                    "errorFree")) {
                require(missing, bool(payload, field),
                        "concurrent multipart upload " + field);
            }

            long cancelledTotal =
                    payload.path("cancelledTotalBytes").asLong(-1);
            long survivorTotal =
                    payload.path("survivorTotalBytes").asLong(-1);
            long cancelThreshold =
                    payload.path("cancelThreshold").asLong(-1);
            long overlapThreshold =
                    payload.path("overlapThreshold").asLong(-1);
            long cancelledProgress =
                    payload.path("cancelledProgressBytes").asLong(-1);
            long survivorProgress =
                    payload.path("survivorProgressBytes").asLong(-1);

            require(missing, cancelledTotal > 4L * 1024 * 1024,
                    "concurrent multipart cancelled file size");
            require(missing, survivorTotal > 3L * 1024 * 1024,
                    "concurrent multipart survivor file size");
            require(missing,
                    cancelThreshold >= 256L * 1024
                            && overlapThreshold >= 64L * 1024
                            && cancelledProgress >= cancelThreshold
                            && cancelledProgress
                            < cancelledTotal + 64L * 1024
                            && survivorProgress >= survivorTotal
                            && survivorProgress
                            < survivorTotal + 64L * 1024,
                    "concurrent multipart bounded browser progress");
            require(missing,
                    payload.path("progressEvents").asInt(-1) >= 3
                            && payload.path("startEvents").asInt(-1) == 2
                            && payload.path("requestEvents").asInt(-1) == 2
                            && payload.path("multipartRequestEvents")
                            .asInt(-1) == 2
                            && payload.path("multipartEntryEvents")
                            .asInt(-1) == 2
                            && payload.path("abortEvents").asInt(-1) == 1
                            && payload.path("removeEvents").asInt(-1) == 1
                            && payload.path("successEvents").asInt(-1) == 1
                            && payload.path("errorEvents").asInt(-1) == 0
                            && payload.path("browserMaximumActive")
                            .asInt(-1) >= 2,
                    "concurrent multipart browser event cardinality");
            require(missing,
                    payload.path("serverMaximumTemporaryDirectories")
                            .asInt(-1) == 1
                            && payload.path("serverMaximumTemporaryFiles")
                            .asInt(-1) == 1
                            && text(payload, "serverMonitorError").isBlank(),
                    "upstream session-locked multipart parsing");
            require(missing,
                    payload.path("serverHandlerCalls").asInt(-1) == 1
                            && payload.path("serverHandlerFailures")
                            .asInt(-1) == 0
                            && "flow-ipc-concurrent-multipart-survivor.bin"
                            .equals(text(payload, "serverHandlerFile"))
                            && text(payload, "serverFailedHandlerFile")
                            .isBlank()
                            && payload.path("serverResponseCalls")
                            .asInt(-1) == 1
                            && payload.path("serverFailedResponses")
                            .asInt(-1) == 0
                            && payload.path("serverSuccessfulResponses")
                            .asInt(-1) == 1
                            && payload.path("serverSuccessfulAcceptedFiles")
                            .asInt(-1) == 1
                            && "flow-ipc-concurrent-multipart-survivor.bin"
                            .equals(text(
                                    payload,
                                    "serverSuccessfulAcceptedFile")),
                    "concurrent multipart application-handler isolation");
            require(missing,
                    payload.path("serverSuccessBytes").asLong(-1)
                            == survivorTotal
                            && payload.path("serverSuccessDeclaredBytes")
                            .asLong(-1) == survivorTotal
                            && payload.path("serverSpillBytes").asLong(-1)
                            == survivorTotal,
                    "concurrent multipart exact survivor part");

            require(missing, opaqueMultipartUploadRequestIds.size() == 2,
                    "two concurrent multipart opaque request opens");
            Set<String> cancelledIds = new java.util.HashSet<>(
                    opaqueMultipartUploadRequestIds);
            cancelledIds.retainAll(opaqueCancelledRequestIds);
            require(missing, cancelledIds.size() == 1,
                    "one correlated concurrent multipart cancellation");
            if (cancelledIds.size() != 1) {
                return;
            }
            String cancelledId = cancelledIds.iterator().next();
            Set<String> completedIds = new java.util.HashSet<>(
                    opaqueMultipartUploadRequestIds);
            completedIds.remove(cancelledId);
            completedIds.removeIf(requestId ->
                    !opaqueRequestEndBodyBytes.containsKey(requestId)
                            || !opaqueSuccessfulResponseRequestIds.contains(
                            requestId));
            require(missing, completedIds.size() == 1,
                    "one concurrent multipart survivor response");
            if (completedIds.size() != 1) {
                return;
            }
            String survivorId = completedIds.iterator().next();
            require(missing,
                    !opaqueRequestEndBodyBytes.containsKey(cancelledId)
                            && !opaqueSuccessfulResponseRequestIds.contains(
                            cancelledId)
                            && opaqueRequestChunkBytes.getOrDefault(
                            cancelledId, -1L) == cancelledProgress,
                    "concurrent multipart cancellation has no terminal success");
            require(missing,
                    opaqueRequestChunkBytes.getOrDefault(survivorId, -1L)
                            == survivorProgress
                            && opaqueRequestEndBodyBytes.getOrDefault(
                            survivorId, -1L) == survivorProgress
                            && opaqueResponseStatuses.getOrDefault(
                            survivorId, -1) >= 200
                            && opaqueResponseStatuses.getOrDefault(
                            survivorId, -1) < 400,
                    "concurrent multipart survivor completion correlation");

            Integer cancelledOpen = opaqueRequestOpenOrders.get(cancelledId);
            Integer survivorOpen = opaqueRequestOpenOrders.get(survivorId);
            Integer cancelledTerminal =
                    opaqueRequestCancelOrders.get(cancelledId);
            Integer survivorTerminal = opaqueRequestEndOrders.get(survivorId);
            require(missing,
                    cancelledOpen != null && survivorOpen != null
                            && cancelledTerminal != null
                            && survivorTerminal != null
                            && Math.max(cancelledOpen, survivorOpen)
                            < Math.min(cancelledTerminal, survivorTerminal),
                    "concurrent multipart requests overlap before termination");
        }

        private void requireCrossSessionMultipartUpload(
                List<String> missing) {
            JsonNode payload = crossSessionMultipartUploadAcceptance;
            require(missing, payload != null,
                    "cross-session multipart upload acceptance result");
            if (payload == null) {
                return;
            }
            for (String field : List.of(
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
                    "crossWindowProgressOverlapObserved", "errorFree")) {
                require(missing, bool(payload, field),
                        "cross-session multipart upload " + field);
            }

            String mainWindowId = text(payload, "mainWindowId");
            String secondaryWindowId = text(payload, "secondaryWindowId");
            String mainSessionId = text(payload, "mainSessionId");
            String secondarySessionId = text(payload, "secondarySessionId");
            String mainUiId = text(payload, "mainUiId");
            String secondaryUiId = text(payload, "secondaryUiId");
            require(missing,
                    "main".equals(mainWindowId)
                            && "multipart-secondary".equals(
                            secondaryWindowId)
                            && !mainSessionId.isBlank()
                            && !secondarySessionId.isBlank()
                            && !mainSessionId.equals(secondarySessionId)
                            && !mainUiId.isBlank()
                            && !secondaryUiId.isBlank()
                            && !mainUiId.equals(secondaryUiId),
                    "distinct cross-session multipart window scope");

            long cancelledTotal =
                    payload.path("cancelledTotalBytes").asLong(-1);
            long survivorTotal =
                    payload.path("survivorTotalBytes").asLong(-1);
            long overlapThreshold =
                    payload.path("overlapThreshold").asLong(-1);
            long mainProgress =
                    payload.path("mainProgressBytes").asLong(-1);
            long secondaryProgress =
                    payload.path("secondaryProgressBytes").asLong(-1);
            require(missing, cancelledTotal > 12L * 1024 * 1024,
                    "cross-session multipart cancelled file size");
            require(missing, survivorTotal > 10L * 1024 * 1024,
                    "cross-session multipart survivor file size");
            require(missing,
                    payload.path("mainTotalBytes").asLong(-1)
                            == cancelledTotal
                            && payload.path("secondaryTotalBytes").asLong(-1)
                            == survivorTotal
                            && overlapThreshold >= 512L * 1024
                            && mainProgress >= overlapThreshold
                            && mainProgress < cancelledTotal + 64L * 1024
                            && secondaryProgress >= survivorTotal
                            && secondaryProgress
                            < survivorTotal + 64L * 1024,
                    "bounded cross-session multipart browser progress");
            require(missing,
                    payload.path("mainStartEvents").asInt(-1) == 1
                            && payload.path("mainRequestEvents").asInt(-1) == 1
                            && payload.path("mainMultipartRequestEvents")
                            .asInt(-1) == 1
                            && payload.path("mainMultipartEntryEvents")
                            .asInt(-1) == 1
                            && payload.path("mainProgressEvents").asInt(-1) > 1
                            && payload.path("mainAbortEvents").asInt(-1) == 1
                            && payload.path("mainRemoveEvents").asInt(-1) == 1
                            && payload.path("mainSuccessEvents").asInt(-1) == 0
                            && payload.path("mainErrorEvents").asInt(-1) == 0
                            && payload.path("secondaryStartEvents")
                            .asInt(-1) == 1
                            && payload.path("secondaryRequestEvents")
                            .asInt(-1) == 1
                            && payload.path("secondaryMultipartRequestEvents")
                            .asInt(-1) == 1
                            && payload.path("secondaryMultipartEntryEvents")
                            .asInt(-1) == 1
                            && payload.path("secondaryProgressEvents")
                            .asInt(-1) > 1
                            && payload.path("secondaryAbortEvents")
                            .asInt(-1) == 0
                            && payload.path("secondaryRemoveEvents")
                            .asInt(-1) == 0
                            && payload.path("secondarySuccessEvents")
                            .asInt(-1) == 1
                            && payload.path("secondaryErrorEvents")
                            .asInt(-1) == 0,
                    "cross-session multipart browser event cardinality");
            require(missing,
                    "flow-ipc-cross-session-multipart-cancel.bin".equals(
                            text(payload, "mainAbortFileName"))
                            && "flow-ipc-cross-session-multipart-cancel.bin"
                            .equals(text(payload, "mainRemovedFileName"))
                            && "flow-ipc-cross-session-multipart-survivor.bin"
                            .equals(text(payload, "secondarySuccessFileName")),
                    "cross-session multipart browser file identity");
            require(missing,
                    payload.path("maximumTemporaryDirectories").asInt(-1) >= 2
                            && payload.path("maximumTemporaryFiles")
                            .asInt(-1) >= 2
                            && text(payload, "mainServerMonitorError").isBlank()
                            && text(payload, "secondaryServerMonitorError")
                            .isBlank(),
                    "parallel request-scoped multipart temporary storage");
            require(missing,
                    payload.path("mainServerHandlerCalls").asInt(-1) == 0
                            && payload.path("mainServerHandlerFailures")
                            .asInt(-1) == 0
                            && text(payload, "mainServerHandlerFile").isBlank()
                            && text(payload, "mainServerFailedHandlerFile")
                            .isBlank()
                            && payload.path("mainServerResponseCalls")
                            .asInt(-1) == 0
                            && payload.path("mainServerFailedResponses")
                            .asInt(-1) == 0,
                    "canceled session avoids application handler dispatch");
            require(missing,
                    payload.path("secondaryServerHandlerCalls")
                            .asInt(-1) == 1
                            && payload.path("secondaryServerHandlerFailures")
                            .asInt(-1) == 0
                            && "flow-ipc-cross-session-multipart-survivor.bin"
                            .equals(text(payload, "secondaryServerHandlerFile"))
                            && text(payload, "secondaryServerFailedHandlerFile")
                            .isBlank()
                            && payload.path("secondaryServerResponseCalls")
                            .asInt(-1) == 1
                            && payload.path("secondaryServerFailedResponses")
                            .asInt(-1) == 0
                            && payload.path("secondaryServerSuccessfulResponses")
                            .asInt(-1) == 1
                            && payload.path("secondaryServerAcceptedFiles")
                            .asInt(-1) == 1
                            && "flow-ipc-cross-session-multipart-survivor.bin"
                            .equals(text(payload, "secondaryServerAcceptedFile")),
                    "survivor session application handler result");
            require(missing,
                    payload.path("secondaryServerSuccessBytes").asLong(-1)
                            == survivorTotal
                            && payload.path("secondaryServerDeclaredBytes")
                            .asLong(-1) == survivorTotal
                            && payload.path("secondaryServerSpillBytes")
                            .asLong(-1) == survivorTotal
                            && bool(payload, "secondaryServerPatternValid")
                            && bool(payload,
                            "secondaryServerDiskSpillObserved")
                            && bool(payload,
                            "mainServerTemporaryPartCleanupObserved")
                            && bool(payload,
                            "secondaryServerTemporaryPartCleanupObserved"),
                    "exact cross-session multipart survivor and cleanup");

            Set<ScopedRequestKey> mainKeys = scopedMultipartKeys(
                    mainWindowId, mainSessionId, mainUiId);
            Set<ScopedRequestKey> secondaryKeys = scopedMultipartKeys(
                    secondaryWindowId, secondarySessionId, secondaryUiId);
            require(missing, scopedMultipartUploadRequestKeys.size() == 2,
                    "two scoped multipart request opens");
            require(missing, mainKeys.size() == 1,
                    "one main-window multipart request open");
            require(missing, secondaryKeys.size() == 1,
                    "one secondary-window multipart request open");
            if (mainKeys.size() != 1 || secondaryKeys.size() != 1) {
                return;
            }
            ScopedRequestKey cancelledKey = mainKeys.iterator().next();
            ScopedRequestKey survivorKey = secondaryKeys.iterator().next();
            require(missing, !cancelledKey.equals(survivorKey),
                    "distinct scoped multipart request keys");
            require(missing,
                    scopedCancelledRequestKeys.contains(cancelledKey)
                            && !scopedRequestEndBodyBytes.containsKey(
                            cancelledKey)
                            && !scopedSuccessfulResponseKeys.contains(
                            cancelledKey)
                            && scopedRequestChunkBytes.getOrDefault(
                            cancelledKey, -1L) == mainProgress,
                    "scoped main-window multipart cancellation");
            require(missing,
                    !scopedCancelledRequestKeys.contains(survivorKey)
                            && scopedRequestChunkBytes.getOrDefault(
                            survivorKey, -1L) == secondaryProgress
                            && scopedRequestEndBodyBytes.getOrDefault(
                            survivorKey, -1L) == secondaryProgress
                            && scopedSuccessfulResponseKeys.contains(
                            survivorKey)
                            && scopedResponseStatuses.getOrDefault(
                            survivorKey, -1) >= 200
                            && scopedResponseStatuses.getOrDefault(
                            survivorKey, -1) < 400,
                    "scoped secondary-window multipart completion");

            Integer cancelledOpen =
                    scopedRequestOpenOrders.get(cancelledKey);
            Integer survivorOpen = scopedRequestOpenOrders.get(survivorKey);
            Integer cancelledTerminal =
                    scopedRequestCancelOrders.get(cancelledKey);
            Integer survivorTerminal =
                    scopedRequestEndOrders.get(survivorKey);
            require(missing,
                    cancelledOpen != null && survivorOpen != null
                            && cancelledTerminal != null
                            && survivorTerminal != null
                            && Math.max(cancelledOpen, survivorOpen)
                            < Math.min(cancelledTerminal, survivorTerminal),
                    "cross-window multipart requests overlap before termination");
        }

        private Set<ScopedRequestKey> scopedMultipartKeys(
                String windowId,
                String sessionId,
                String uiId) {
            Set<ScopedRequestKey> matches = new java.util.HashSet<>();
            for (ScopedRequestKey key : scopedMultipartUploadRequestKeys) {
                if (key.windowId.equals(windowId)
                        && key.sessionId.equals(sessionId)
                        && key.uiId.equals(uiId)) {
                    matches.add(key);
                }
            }
            return matches;
        }

        private Set<String> rawRequestIdsWithLength(long contentLength) {
            Set<String> matches = new java.util.HashSet<>();
            for (String requestId : opaqueRawRequestIds) {
                if (opaqueRequestContentLengths.getOrDefault(requestId, -1L)
                        == contentLength) {
                    matches.add(requestId);
                }
            }
            return matches;
        }

        private void requireFramework(List<String> missing) {
            JsonNode payload = frameworkAcceptance;
            require(missing, payload != null, "framework acceptance result");
            if (payload == null) {
                return;
            }
            require(missing, standardLazyFrontendChunkResourceCount >= 2,
                    "multiple lazy frontend production chunk resources");
            require(missing, standardLazyTransitiveFrontendResource,
                    "transitive lazy frontend production resource");
            for (String field : List.of(
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
                    "applyEventObserved",
                    "avatarClickObserved", "messageInputSubmitObserved",
                    "richTextEditorEditObserved",
                    "loginOverlaySubmitObserved",
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
                    "tabsSelectionObserved", "detailsOpenObserved",
                    "accordionOpenObserved",
                    "menuBarActionObserved", "sideNavSelectionObserved",
                    "contextMenuOpenObserved", "contextMenuItemClickObserved",
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
                    "nativeClipboardApplied")) {
                require(missing, bool(payload, field), "framework " + field);
            }
        }

        private void requireLazyGridQuery(List<String> missing) {
            JsonNode payload = lazyGridQueryAcceptance;
            require(missing, payload != null, "lazy Grid query acceptance result");
            if (payload == null) {
                return;
            }
            for (String field : List.of(
                    "complete", "standardBundleMode", "rootRendered",
                    "flowClientOwned", "opaqueRequestBridgeObserved",
                    "opaqueResponseBridgeObserved", "pushObserved", "errorFree",
                    "lazyGridFarViewportObserved", "lazyGridSelectionObserved",
                    "lazyGridReturnViewportObserved", "lazyGridSortObserved",
                    "lazyGridFilterObserved", "lazyGridFilterResetObserved")) {
                require(missing, bool(payload, field), "lazy Grid " + field);
            }
        }

        private void requireFlowDnd(List<String> missing) {
            JsonNode payload = flowDndAcceptance;
            require(missing, payload != null, "Flow DnD acceptance result");
            if (payload == null) {
                return;
            }
            for (String field : List.of(
                    "complete", "standardBundleMode", "rootRendered",
                    "flowClientOwned", "opaqueRequestBridgeObserved",
                    "opaqueResponseBridgeObserved", "pushObserved", "errorFree",
                    "flowDndDedicatedRouteObserved",
                    "flowDndConfiguredObserved",
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
                    "flowDndDropObserved", "flowDndDragEndObserved")) {
                require(missing, bool(payload, field), "Flow DnD " + field);
            }
            require(missing, "appkit-event-queue-pointer".equals(
                            text(payload, "flowDndInputMode")),
                    "Flow DnD native input mode");
            require(missing,
                    payload.has("flowDndBrowserProgrammaticObserved")
                            && !bool(payload,
                            "flowDndBrowserProgrammaticObserved"),
                    "Flow DnD scripted input disabled");
            String dragStartStatus = text(payload, "flowDndDragStartStatus");
            String dropStatus = text(payload, "flowDndDropStatus");
            String dragEndStatus = text(payload, "flowDndDragEndStatus");
            require(missing, dragStartStatus.contains("client=true")
                            && dragStartStatus.contains("change=1"),
                    "Flow DnD Java drag-start listener");
            require(missing, dropStatus.contains("client=true")
                            && dropStatus.contains("change=1"),
                    "Flow DnD Java drop listener");
            require(missing, dragEndStatus.contains("client=true")
                            && dragEndStatus.contains("change=1"),
                    "Flow DnD Java drag-end listener");
        }

        private void requireFlowOverlayStack(List<String> missing) {
            JsonNode payload = flowOverlayStackAcceptance;
            require(missing, payload != null,
                    "Flow overlay stack acceptance result");
            if (payload == null) {
                return;
            }
            for (String field : List.of(
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
                    "flowOverlayStackLayeredFocusRestoredObserved")) {
                require(missing, bool(payload, field),
                        "Flow overlay stack " + field);
            }
        }

        private void requireFlowInputPopup(List<String> missing) {
            JsonNode payload = flowInputPopupAcceptance;
            require(missing, payload != null,
                    "Flow input popup acceptance result");
            if (payload == null) {
                return;
            }
            for (String field : List.of(
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
                    "flowInputPopupDatePointerJavaLifecycleObserved")) {
                require(missing, bool(payload, field),
                        "Flow input popup " + field);
            }
            require(missing,
                    payload.path("flowInputPopupNativeKeyboardEventCount")
                            .asInt() >= 12,
                    "Flow input popup native keyboard event count");
            require(missing,
                    payload.path("flowInputPopupNativePointerEventCount")
                            .asInt() >= 2,
                    "Flow input popup native pointer event count");
            require(missing,
                    payload.path(
                            "flowInputPopupNativePointerTrustedEventCount")
                            .asInt() >= 2,
                    "Flow input popup trusted native pointer event count");
        }

        private void requireLocalizedInputPopup(List<String> missing) {
            JsonNode payload = localizedInputPopupAcceptance;
            require(missing, payload != null,
                    "localized input popup acceptance result");
            if (payload == null) {
                return;
            }
            for (String field : List.of(
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
                    "flowInputPopupTimeNativeKeyboardObserved")) {
                require(missing, bool(payload, field),
                        "localized input popup " + field);
            }
            require(missing,
                    payload.path("flowInputPopupNativeKeyboardEventCount")
                            .asInt() >= 6,
                    "localized input popup native keyboard event count");
            String stages = text(payload,
                    "flowInputPopupNativeKeyboardStages");
            require(missing,
                    stages.contains("time-open")
                            && stages.contains("time-move")
                            && stages.contains("time-select"),
                    "localized input popup native keyboard stages");
        }

        private void requirePickerValidation(List<String> missing) {
            JsonNode payload = pickerValidationAcceptance;
            require(missing, payload != null,
                    "picker validation acceptance result");
            if (payload == null) {
                return;
            }
            for (String field : List.of(
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
                    "pickerValidationOverlaysClosedObserved")) {
                require(missing, bool(payload, field),
                        "picker validation " + field);
            }
            require(missing,
                    "2026-08-12T13:30".equals(
                            text(payload, "pickerValidationValue")),
                    "picker validation recovered value");
            require(missing,
                    "2026-08-12T12:00".equals(
                            text(payload, "pickerValidationMin")),
                    "picker validation minimum constraint");
            require(missing,
                    "2026-08-12T14:00".equals(
                            text(payload, "pickerValidationMax")),
                    "picker validation maximum constraint");
            require(missing,
                    !payload.path("pickerValidationInvalid").asBoolean(true),
                    "picker validation recovered invalid state");
            require(missing,
                    text(payload, "pickerValidationErrorMessage").isEmpty(),
                    "picker validation cleared error message");
            String javaStatus = text(payload, "pickerValidationJavaStatus");
            require(missing,
                    javaStatus.contains("value=2026-08-12T13:30")
                            && javaStatus.contains("invalid=false")
                            && javaStatus.contains("error=none")
                            && javaStatus.contains("client=true"),
                    "picker validation recovered Java listener state");
            String validationStatus = text(payload,
                    "pickerValidationJavaValidationStatus");
            require(missing,
                    validationStatus.contains("valid=false")
                            && validationStatus.contains("invalid=true")
                            && validationStatus.contains("error=")
                            && validationStatus.contains("malformed"),
                    "picker validation malformed Java validation listener state");
        }

        private void requireFlowMenuPopup(List<String> missing) {
            JsonNode payload = flowMenuPopupAcceptance;
            require(missing, payload != null,
                    "Flow menu popup acceptance result");
            if (payload == null) {
                return;
            }
            for (String field : List.of(
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
                    "flowMenuPopupGridPolicyFocusRestoredObserved")) {
                require(missing, bool(payload, field),
                        "Flow menu popup " + field);
            }
            require(missing,
                    payload.path("flowMenuPopupNativeKeyboardEventCount")
                            .asInt() >= 69,
                    "Flow menu popup native keyboard event count");
            require(missing,
                    payload.path("flowMenuPopupNativePointerEventCount")
                            .asInt() >= 10,
                    "Flow menu popup native pointer event count");
            int renderedItems = payload.path(
                    "flowMenuPopupGridRenderedItemCount").asInt();
            require(missing, renderedItems > 0 && renderedItems < 180,
                    "Flow menu popup virtualized Grid rendered item count");
        }

        private void requireStructuredRpc(List<String> missing) {
            JsonNode payload = structuredRpcAcceptance;
            require(missing, payload != null,
                    "structured RPC acceptance result");
            if (payload == null) {
                return;
            }
            for (String field : List.of(
                    "complete", "standardBundleMode", "rootRendered",
                    "flowClientOwned", "opaqueRequestBridgeObserved",
                    "opaqueResponseBridgeObserved", "pushObserved", "errorFree",
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
                    "structuredRpcInertRecoveryObserved")) {
                require(missing, bool(payload, field),
                        "structured RPC " + field);
            }
            require(missing,
                    !bool(payload, "structuredRpcCanceledSideEffectExecuted"),
                    "structured RPC canceled side effect not executed");
            require(missing,
                    !bool(payload,
                            "structuredRpcDetachedOwnerSideEffectExecuted"),
                    "structured RPC detached owner side effect not executed");
            require(missing,
                    !bool(payload,
                            "structuredRpcFutureCanceledSideEffectExecuted"),
                    "structured RPC canceled future side effect not executed");
            require(missing,
                    !bool(payload, "structuredRpcInertDefaultSettled"),
                    "structured RPC inert default promise remains pending");
        }

        private void requireRecovery(List<String> missing) {
            JsonNode payload = recoveryAcceptance;
            require(missing, payload != null, "sidecar recovery acceptance result");
            if (payload == null) {
                return;
            }
            for (String field : List.of(
                    "complete", "standardBundleMode", "errorFree", "processRestarted",
                    "windowPreserved", "webViewPreserved", "webViewReloaded",
                    "serverUiStateReset", "requestBridgeRecovered", "pushRecovered",
                    "unacknowledgedPushObservedBeforeReplacement",
                    "unacknowledgedPushUnackedBeforeReplacement",
                    "stalePushNotReplayed",
                    "freshPushSequenceAfterReplacement",
                    "interactionRecovered", "rapidFailuresRecovered",
                    "stableBudgetResetExercised")) {
                require(missing, bool(payload, field), "recovery " + field);
            }
            require(missing, payload.path(
                            "unacknowledgedPushSequence").asLong() > 0,
                    "recovery unacknowledged push sequence");
            require(missing, payload.path(
                            "unacknowledgedPushFlowServerSyncId").asInt() >= 0,
                    "recovery unacknowledged Flow server sync id");
            require(missing,
                    payload.path("firstRecoveredPushMessageCount").asInt() == 2,
                    "fresh session received exactly two baseline pushes");
            require(missing,
                    payload.path("firstRecoveredPushAckCount").asInt() == 2,
                    "fresh session acknowledged exactly two baseline pushes");
            require(missing,
                    payload.path("firstRecoveredPushFirstSequence").asInt() == 1,
                    "fresh session push sequence starts at one");
            require(missing,
                    payload.path("firstRecoveredPushLastSequence").asInt() == 2,
                    "fresh session baseline push sequence ends at two");
            require(missing, sidecarUnacknowledgedPushBoundary != null,
                    "unacknowledged push replacement boundary");
            if (sidecarUnacknowledgedPushBoundary != null) {
                require(missing, bool(
                                sidecarUnacknowledgedPushBoundary, "observed"),
                        "unacknowledged push observed before replacement");
                require(missing, bool(
                                sidecarUnacknowledgedPushBoundary,
                                "unacknowledged"),
                        "push remained unacknowledged before replacement");
                require(missing,
                        sidecarUnacknowledgedPushBoundary.path(
                                "initialAckCount").asInt() >= 2
                                && sidecarUnacknowledgedPushBoundary.path(
                                "ackCount").asInt()
                                == sidecarUnacknowledgedPushBoundary.path(
                                "initialAckCount").asInt(),
                        "no browser acknowledgement before replacement");
                require(missing,
                        sidecarUnacknowledgedPushBoundary.path(
                                "recoveryGeneration").asInt() == 0,
                        "unacknowledged push belongs to initial generation");
            }
            require(missing, sidecarUnacknowledgedPushTraceCorrelated,
                    "trace-correlated old push remained unacknowledged");
            require(missing, sidecarUnacknowledgedPushProcessCorrelated,
                    "unacknowledged push and failed sidecar process correlation");
            require(missing,
                    sidecarUnacknowledgedPushBoundaryOrder > 0
                            && sidecarUnacknowledgedPushFailureOrder
                            > sidecarUnacknowledgedPushBoundaryOrder,
                    "sidecar failure after unacknowledged push observation");
            require(missing,
                    sidecarUnacknowledgedPushExitOrder
                            > sidecarUnacknowledgedPushFailureOrder,
                    "old sidecar exit after unacknowledged push failure injection");
            require(missing,
                    sidecarUnacknowledgedPushRecoveryReadyOrder
                            > sidecarUnacknowledgedPushExitOrder,
                    "fresh sidecar ready after old unacknowledged push exit");
            require(missing, payload.path("recoveryGeneration").asInt() >= 3,
                    "three recovery generations");
            require(missing, payload.path("processReplacementCount").asInt() >= 3,
                    "three sidecar process replacements");
            require(missing,
                    payload.path("previousProcessIdentifier").asLong() > 0
                            && payload.path("processIdentifier").asLong() > 0
                            && payload.path("previousProcessIdentifier").asLong()
                            != payload.path("processIdentifier").asLong(),
                    "replaced sidecar process");
            require(missing, sidecarFailureInjectionCount >= 3,
                    "three unexpected sidecar failures");
            require(missing,
                    sidecarRecoveryScheduledAttempts.equals(List.of(1, 2, 1)),
                    "recovery attempts 1, 2, then 1 after stability");
            require(missing,
                    sidecarRecoveryReadyAttempts.equals(List.of(1, 2, 1)),
                    "ready recoveries 1, 2, then 1 after stability");
            require(missing, sidecarRecoveryStabilityDeadlineCount >= 3,
                    "stability deadline after each recovery");
            require(missing, sidecarRecoveryBudgetReset != null,
                    "recovery budget reset");
            if (sidecarRecoveryBudgetReset != null) {
                require(missing,
                        sidecarRecoveryBudgetReset.path("previousAttempts").asInt()
                                >= 2,
                        "recovery budget reset after rapid failures");
                require(missing,
                        sidecarRecoveryBudgetReset.path("stableRunSeconds").asDouble()
                                >= 1,
                        "manifest-defined recovery stable run");
            }
        }

        private static void require(List<String> missing, boolean condition, String label) {
            if (!condition && !missing.contains(label)) {
                missing.add(label);
            }
        }

        private InFlightOpaqueRequestTrace latestInFlightRequest(
                String requestId) {
            if (requestId == null || requestId.isBlank()) {
                return null;
            }
            for (int index = inFlightLargeRawRequests.size() - 1;
                    index >= 0; index--) {
                InFlightOpaqueRequestTrace request =
                        inFlightLargeRawRequests.get(index);
                if (!request.frozen && request.requestId.equals(requestId)) {
                    return request;
                }
            }
            return null;
        }

        private static final class InFlightOpaqueRequestTrace {
            private final String requestId;
            private final long contentLength;
            private final int openOrder;
            private long chunkBytes;
            private long endBodyBytes = -1;
            private int endOrder;
            private int cancelOrder;
            private int responseStatus = -1;
            private int responseOrder;
            private int responseEndOrder;
            private boolean frozen;

            private InFlightOpaqueRequestTrace(
                    String requestId,
                    long contentLength,
                    int openOrder) {
                this.requestId = requestId;
                this.contentLength = contentLength;
                this.openOrder = openOrder;
            }
        }

        private static ScopedRequestKey scopedRequestKey(JsonNode payload) {
            String windowId = text(payload, "windowId");
            String sessionId = text(payload, "sessionId");
            String uiId = text(payload, "uiId");
            String requestId = text(payload, "requestId");
            if (windowId.isBlank() || sessionId.isBlank()
                    || uiId.isBlank() || requestId.isBlank()) {
                return null;
            }
            return new ScopedRequestKey(
                    windowId, sessionId, uiId, requestId);
        }

        private record ScopedRequestKey(
                String windowId,
                String sessionId,
                String uiId,
                String requestId) {
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isString() ? value.stringValue() : "";
    }

    private static boolean bool(JsonNode node, String field) {
        return node.path(field).isBoolean() && node.path(field).asBoolean();
    }

    private static boolean headerNamed(JsonNode payload, String expected) {
        for (String name : text(payload, "headerNames").split(",")) {
            if (expected.equalsIgnoreCase(name.trim())) {
                return true;
            }
        }
        return false;
    }

    private static void addRequestId(Set<String> ids, JsonNode payload) {
        String requestId = text(payload, "requestId");
        if (!requestId.isBlank()) {
            ids.add(requestId);
        }
    }

    private static JsonNode envelopePayload(JsonNode payload) {
        JsonNode nested = payload.path("envelope").path("payload");
        return nested.isObject() ? nested : payload;
    }
}
