import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Test-support sidecar that drives the unmodified production macOS host through
 * repeated secondary-window retirement and one fresh reincarnation.
 */
public final class RetiredScopeProductionHostSidecar {
    private static final String WIRE_PREFIX = "VFIPC ";
    private static final String WINDOW_ID = "capability-reopen-secondary";
    private static final String SESSION_PREFIX = "flow-ipc-window:";
    private static final String PASS_MARKER =
            "RETIRED_SCOPE_PRODUCTION_HOST_PASS";
    private static final String FAIL_MARKER =
            "RETIRED_SCOPE_PRODUCTION_HOST_FAIL";
    private static final long WATCHDOG_MILLIS = 50_000L;

    private static final String OPEN_A = "retired-host-open-a";
    private static final String CLOSE_A = "retired-host-close-a";
    private static final String RETIRED_A = "retired-host-command-a";
    private static final String OPEN_B = "retired-host-open-b";
    private static final String CLOSE_B = "retired-host-close-b";
    private static final String RETIRED_B = "retired-host-command-b";
    private static final String OPEN_C = "retired-host-open-c";
    private static final String LIVE_C = "retired-host-live-command-c";
    private static final String CLOSE_C = "retired-host-close-c";
    private static final String CLOSE_MAIN = "retired-host-close-main";

    private static final Scope MAIN_SCOPE =
            new Scope("main", "flow-ipc-session", "main");

    private final AtomicBoolean finished = new AtomicBoolean();

    private Scope scopeA;
    private Scope scopeB;
    private Scope scopeC;
    private Request requestA;
    private Request requestB;
    private Request requestC;
    private String expectedCapture = "A";

    private boolean openAResult;
    private boolean closeASent;
    private boolean closeAResult;
    private boolean windowClosedA;
    private boolean openBSent;
    private boolean openBResult;
    private boolean closeBSent;
    private boolean closeBResult;
    private boolean windowClosedB;
    private boolean retiredProbesSent;
    private boolean retiredAResult;
    private boolean retiredBResult;
    private boolean openCSent;
    private boolean openCResult;
    private boolean responseMetadataC;
    private boolean responseCreditC;
    private boolean responseBodyC;
    private boolean liveCResult;
    private boolean closeCSent;
    private boolean closeCResult;
    private boolean windowClosedC;
    private boolean closeMainSent;
    private boolean closeMainResult;
    private boolean windowClosedMain;
    private String phase = "booting";

    public static void main(String[] args) {
        new RetiredScopeProductionHostSidecar().run();
    }

    private void run() {
        try {
            require("flow-ipc".equals(System.getenv("VAADIN_DESKTOP_TRANSPORT")),
                    "host did not launch the fixture with flow-ipc transport");
            writePidFile();
            startWatchdog();
            sendReady();
            phase = "opening-a";
            sendOpen(OPEN_A, "Retired scope production host A");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    System.in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith(WIRE_PREFIX)) {
                        fail("unexpected unframed host input");
                    }
                    handleEnvelope(parseObject(line.substring(WIRE_PREFIX.length())));
                    if (finished.get()) {
                        return;
                    }
                }
            }
            fail("host stdin closed before the shutdown envelope");
        } catch (Throwable failure) {
            fail(failure.getMessage() == null
                    ? failure.getClass().getName()
                    : failure.getMessage());
        }
    }

    private void handleEnvelope(Map<String, Object> envelope) {
        require(number(envelope, "version").intValue() == 1,
                "host envelope version is not 1");
        String type = string(envelope, "type");
        if ("shutdown".equals(type)) {
            complete();
            return;
        }
        if ("error".equals(type)) {
            fail("host sent an error envelope: " + envelope);
        }
        switch (type) {
            case "capabilityResult" -> handleCapabilityResult(envelope);
            case "windowClosed" -> handleWindowClosed(envelope);
            case "opaqueHttpStreamOpen" -> handleStreamOpen(envelope);
            case "opaqueHttpStreamResponseCredit" -> handleResponseCredit(envelope);
            default -> handleOtherBrowserEnvelope(envelope, type);
        }
    }

    private void handleCapabilityResult(Map<String, Object> envelope) {
        String correlationId = string(envelope, "correlationId");
        Map<String, Object> payload = object(envelope, "payload");
        switch (correlationId) {
            case OPEN_A -> {
                requireScope(envelope, MAIN_SCOPE, "open A result");
                requireSuccessfulResult(payload, "window.open", WINDOW_ID);
                require(!openAResult, "duplicate open A result");
                openAResult = true;
                maybeCloseA();
            }
            case CLOSE_A -> {
                requireScope(envelope, MAIN_SCOPE, "close A result");
                requireSuccessfulResult(payload, "window.close", WINDOW_ID);
                require(!closeAResult, "duplicate close A result");
                closeAResult = true;
                maybeOpenB();
            }
            case RETIRED_A -> {
                requireScope(envelope, required(scopeA, "A scope"),
                        "retired A result");
                requireRetiredResult(payload);
                require(!retiredAResult, "duplicate retired A result");
                retiredAResult = true;
                maybeOpenC();
            }
            case OPEN_B -> {
                requireScope(envelope, MAIN_SCOPE, "open B result");
                requireSuccessfulResult(payload, "window.open", WINDOW_ID);
                require(!openBResult, "duplicate open B result");
                openBResult = true;
                maybeCloseB();
            }
            case CLOSE_B -> {
                requireScope(envelope, MAIN_SCOPE, "close B result");
                requireSuccessfulResult(payload, "window.close", WINDOW_ID);
                require(!closeBResult, "duplicate close B result");
                closeBResult = true;
                maybeSendRetiredProbes();
            }
            case RETIRED_B -> {
                requireScope(envelope, required(scopeB, "B scope"),
                        "retired B result");
                requireRetiredResult(payload);
                require(!retiredBResult, "duplicate retired B result");
                retiredBResult = true;
                maybeOpenC();
            }
            case OPEN_C -> {
                requireScope(envelope, MAIN_SCOPE, "open C result");
                requireSuccessfulResult(payload, "window.open", WINDOW_ID);
                require(!openCResult, "duplicate open C result");
                openCResult = true;
                maybeStartCResponse();
            }
            case LIVE_C -> {
                requireScope(envelope, required(scopeC, "C scope"),
                        "live C title result");
                requireSuccessfulResult(payload, "window.setTitle", null);
                require("Retired scope production host C live"
                                .equals(string(payload, "title")),
                        "live C title payload mismatch: " + payload);
                require("native-appkit".equals(string(payload, "mode")),
                        "live C title did not use AppKit: " + payload);
                require("manifest-enforced".equals(string(payload, "permissionMode")),
                        "live C title permission mode mismatch: " + payload);
                require("allowed".equals(string(payload, "permissionDecision")),
                        "live C title was not manifest-allowed: " + payload);
                require(("window:" + WINDOW_ID)
                                .equals(string(payload, "permissionScope")),
                        "live C title permission scope mismatch: " + payload);
                require(Boolean.TRUE.equals(payload.get("permissionManifestLoaded")),
                        "live C title did not use a loaded manifest: " + payload);
                require(responseBodyC,
                        "live C title result arrived before the streamed response body");
                require(!liveCResult, "duplicate live C title result");
                liveCResult = true;
                sendCloseC();
            }
            case CLOSE_C -> {
                requireScope(envelope, MAIN_SCOPE, "close C result");
                requireSuccessfulResult(payload, "window.close", WINDOW_ID);
                require(!closeCResult, "duplicate close C result");
                closeCResult = true;
                maybeCloseMain();
            }
            case CLOSE_MAIN -> {
                requireScope(envelope, MAIN_SCOPE, "close main result");
                requireSuccessfulResult(payload, "window.close", "main");
                require(!closeMainResult, "duplicate close main result");
                closeMainResult = true;
                phase = "awaiting-main-window-closed";
            }
            default -> fail("unexpected capability result correlation: " + correlationId);
        }
    }

    private void handleWindowClosed(Map<String, Object> envelope) {
        Scope scope = Scope.from(envelope);
        if (scopeA != null && scopeA.equals(scope)) {
            require(!windowClosedA, "duplicate A windowClosed");
            windowClosedA = true;
            maybeOpenB();
            return;
        }
        if (scopeB != null && scopeB.equals(scope)) {
            require(windowClosedA, "B closed before A");
            require(!windowClosedB, "duplicate B windowClosed");
            windowClosedB = true;
            maybeSendRetiredProbes();
            return;
        }
        if (scopeC != null && scopeC.equals(scope)) {
            require(responseBodyC && liveCResult,
                    "C closed before its live transport and command completed");
            require(!windowClosedC, "duplicate C windowClosed");
            windowClosedC = true;
            maybeCloseMain();
            return;
        }
        if (MAIN_SCOPE.equals(scope)) {
            require(closeMainSent, "main closed before the fixture requested it");
            require(!windowClosedMain, "duplicate main windowClosed");
            windowClosedMain = true;
            phase = "awaiting-shutdown";
            return;
        }
        fail("windowClosed carried an unknown scope: " + scope);
    }

    private void handleStreamOpen(Map<String, Object> envelope) {
        Scope scope = Scope.from(envelope);
        if (MAIN_SCOPE.equals(scope)) {
            return;
        }
        validateSecondaryScope(scope);
        Map<String, Object> payload = object(envelope, "payload");
        String requestId = string(payload, "requestId");
        String correlationId = string(envelope, "correlationId");
        require(requestId.equals(correlationId),
                "browser requestId and correlationId differ");
        Request request = new Request(scope, requestId, correlationId);

        if ("A".equals(expectedCapture)) {
            scopeA = scope;
            requestA = request;
            expectedCapture = "";
            maybeCloseA();
            return;
        }
        if ("B".equals(expectedCapture)) {
            require(!scope.equals(scopeA), "B reused A's complete scope");
            require(!scope.sessionId().equals(scopeA.sessionId()),
                    "B reused A's host-generated session UUID");
            scopeB = scope;
            requestB = request;
            expectedCapture = "";
            maybeCloseB();
            return;
        }
        if ("C".equals(expectedCapture)) {
            require(!scope.equals(scopeA) && !scope.equals(scopeB),
                    "C reused a retired complete scope");
            require(!scope.sessionId().equals(scopeA.sessionId())
                            && !scope.sessionId().equals(scopeB.sessionId()),
                    "C reused a retired host-generated session UUID");
            scopeC = scope;
            requestC = request;
            expectedCapture = "";
            maybeStartCResponse();
            return;
        }
        require(scope.equals(scopeA) || scope.equals(scopeB) || scope.equals(scopeC),
                "secondary request arrived from an untracked scope: " + scope);
    }

    private void handleResponseCredit(Map<String, Object> envelope) {
        Scope scope = Scope.from(envelope);
        Map<String, Object> payload = object(envelope, "payload");
        String requestId = string(payload, "requestId");
        if (MAIN_SCOPE.equals(scope)) {
            return;
        }
        Request c = required(requestC, "C request");
        require(c.matches(scope, requestId),
                "response credit did not match C's exact scope and requestId");
        require(responseMetadataC,
                "C response credit arrived before response metadata was sent");
        require(number(payload, "credit").longValue() > 0,
                "C response credit was not positive");
        if (!responseCreditC) {
            responseCreditC = true;
            sendCResponseBodyAndLiveCommand();
        }
    }

    private void handleOtherBrowserEnvelope(
            Map<String, Object> envelope, String type) {
        if (!type.startsWith("opaqueHttp") && !type.startsWith("opaquePush")) {
            fail("unexpected host envelope type: " + type);
        }
        Scope scope = Scope.from(envelope);
        require(MAIN_SCOPE.equals(scope)
                        || scope.equals(scopeA)
                        || scope.equals(scopeB)
                        || scope.equals(scopeC),
                "browser envelope carried an untracked scope: " + scope);
    }

    private void maybeCloseA() {
        if (openAResult && requestA != null && !closeASent) {
            closeASent = true;
            phase = "closing-a";
            sendClose(CLOSE_A, WINDOW_ID);
        }
    }

    private void maybeOpenB() {
        if (closeAResult && windowClosedA && !openBSent) {
            openBSent = true;
            expectedCapture = "B";
            phase = "opening-b";
            sendOpen(OPEN_B, "Retired scope production host B");
        }
    }

    private void maybeCloseB() {
        if (openBResult && requestB != null && !closeBSent) {
            closeBSent = true;
            phase = "closing-b";
            sendClose(CLOSE_B, WINDOW_ID);
        }
    }

    private void maybeSendRetiredProbes() {
        if (closeBResult && windowClosedB && !retiredProbesSent) {
            retiredProbesSent = true;
            phase = "probing-retired-a-and-b";
            sendRetiredProbe("a", required(scopeA, "A scope"),
                    required(requestA, "A request"), RETIRED_A);
            sendRetiredProbe("b", required(scopeB, "B scope"),
                    required(requestB, "B request"), RETIRED_B);
        }
    }

    private void maybeOpenC() {
        if (retiredAResult && retiredBResult && !openCSent) {
            openCSent = true;
            expectedCapture = "C";
            phase = "opening-c";
            sendOpen(OPEN_C, "Retired scope production host C");
        }
    }

    private void maybeStartCResponse() {
        if (openCResult && requestC != null && !responseMetadataC) {
            responseMetadataC = true;
            phase = "awaiting-c-response-credit";
            sendStreamResponseMetadata(requestC, 200, "OK");
        }
    }

    private void sendCResponseBodyAndLiveCommand() {
        require(!responseBodyC, "C response body was already sent");
        byte[] body = "retired-scope-production-host-c"
                .getBytes(StandardCharsets.UTF_8);
        sendEnvelope(
                "opaqueHttpStreamResponseChunk",
                "retired-host-c-response-chunk",
                requestC.correlationId(),
                requestC.scope(),
                "{\"requestId\":" + quote(requestC.requestId())
                        + ",\"chunkSequence\":0,\"bodyBase64\":"
                        + quote(Base64.getEncoder().encodeToString(body))
                        + ",\"bodyBytes\":" + body.length
                        + ",\"transport\":\"opaque-http-stream-ipc\"}");
        sendEnvelope(
                "opaqueHttpStreamResponseEnd",
                "retired-host-c-response-end",
                requestC.correlationId(),
                requestC.scope(),
                "{\"requestId\":" + quote(requestC.requestId())
                        + ",\"chunkCount\":1,\"bodyBytes\":" + body.length
                        + ",\"transport\":\"opaque-http-stream-ipc\"}");
        responseBodyC = true;
        phase = "live-c-command";
        sendCommand(
                required(scopeC, "C scope"),
                LIVE_C,
                "window.setTitle",
                "{\"title\":\"Retired scope production host C live\"}");
    }

    private void sendCloseC() {
        require(!closeCSent, "C close was already requested");
        closeCSent = true;
        phase = "closing-c";
        sendClose(CLOSE_C, WINDOW_ID);
    }

    private void maybeCloseMain() {
        if (closeCResult && windowClosedC && !closeMainSent) {
            closeMainSent = true;
            phase = "closing-main";
            sendClose(CLOSE_MAIN, "main");
        }
    }

    private void sendRetiredProbe(
            String generation, Scope scope, Request request, String correlationId) {
        sendStreamResponseMetadata(request, 599,
                "Retired scope " + generation.toUpperCase());
        sendEnvelope(
                "opaquePush",
                "retired-host-push-" + generation,
                "retired-host-push-" + generation,
                scope,
                "{\"pushId\":" + quote("retired-host-push-" + generation)
                        + ",\"bodyBase64\":\"\"}");
        sendCommand(
                scope,
                correlationId,
                "window.setTitle",
                "{\"title\":" + quote("Retired scope " + generation) + "}");
    }

    private void sendReady() {
        String payload = "{"
                + "\"transport\":\"flow-ipc\","
                + "\"application\":{"
                + "\"name\":\"Retired Scope Production Host Gate\","
                + "\"metadata\":{\"fixture\":\"test-support\"},"
                + "\"mainWindow\":{"
                + "\"id\":\"main\","
                + "\"title\":\"Retired Scope Production Host Gate\","
                + "\"width\":760,\"height\":520,\"route\":\"\"}},"
                + "\"runtime\":\"retired-scope-production-host-fixture\","
                + "\"browserRequestTransport\":\"opaque-http-stream-ipc\","
                + "\"enabledFeatureFlags\":[]}";
        sendEnvelope("ready", "sidecar-ready", "sidecar-ready", MAIN_SCOPE, payload);
    }

    private void sendOpen(String correlationId, String title) {
        sendCommand(
                MAIN_SCOPE,
                correlationId,
                "window.open",
                "{\"id\":" + quote(WINDOW_ID)
                        + ",\"title\":" + quote(title)
                        + ",\"width\":640,\"height\":420,\"route\":\"\"}");
    }

    private void sendClose(String correlationId, String windowId) {
        sendCommand(
                MAIN_SCOPE,
                correlationId,
                "window.close",
                "{\"id\":" + quote(windowId) + "}");
    }

    private void sendCommand(
            Scope scope, String correlationId, String command, String argsJson) {
        sendEnvelope(
                "command",
                correlationId,
                correlationId,
                scope,
                "{\"command\":" + quote(command) + ",\"args\":" + argsJson + "}");
    }

    private void sendStreamResponseMetadata(
            Request request, int status, String statusText) {
        sendEnvelope(
                "opaqueHttpStreamResponse",
                "retired-host-http-response-" + request.scope().sessionId(),
                request.correlationId(),
                request.scope(),
                "{\"requestId\":" + quote(request.requestId())
                        + ",\"status\":" + status
                        + ",\"statusText\":" + quote(statusText)
                        + ",\"headers\":{\"Content-Type\":\"text/plain; charset=utf-8\"},"
                        + "\"transport\":\"opaque-http-stream-ipc\"}");
    }

    private static void sendEnvelope(
            String type,
            String id,
            String correlationId,
            Scope scope,
            String payloadJson) {
        String json = "{\"version\":1"
                + ",\"type\":" + quote(type)
                + ",\"id\":" + quote(id)
                + ",\"correlationId\":" + quote(correlationId)
                + ",\"sessionId\":" + quote(scope.sessionId())
                + ",\"uiId\":" + quote(scope.uiId())
                + ",\"windowId\":" + quote(scope.windowId())
                + ",\"route\":\"\""
                + ",\"payload\":" + payloadJson
                + ",\"error\":null"
                + ",\"timestamp\":" + System.currentTimeMillis()
                + "}";
        System.out.println(WIRE_PREFIX + json);
        System.out.flush();
    }

    private static void requireSuccessfulResult(
            Map<String, Object> payload, String command, String targetWindowId) {
        require(Boolean.TRUE.equals(payload.get("success")),
                command + " did not succeed: " + payload);
        require(command.equals(string(payload, "command")),
                "capability result command mismatch: " + payload);
        if (targetWindowId != null) {
            require(targetWindowId.equals(string(payload, "windowId")),
                    "capability result target window mismatch: " + payload);
        }
    }

    private static void requireRetiredResult(Map<String, Object> payload) {
        require(Boolean.FALSE.equals(payload.get("success")),
                "retired command unexpectedly succeeded: " + payload);
        require("window.setTitle".equals(string(payload, "command")),
                "retired result command mismatch: " + payload);
        require(Boolean.TRUE.equals(payload.get("canceled")),
                "retired result was not canceled: " + payload);
        require("owner-window-closed".equals(string(payload, "cancelReason")),
                "retired result cancel reason mismatch: " + payload);
        require(Boolean.TRUE.equals(payload.get("operationInvalidated")),
                "retired operation was not invalidated: " + payload);
        require("Owner window closed before native capability completed"
                        .equals(string(payload, "error")),
                "retired result error mismatch: " + payload);
    }

    private static void requireScope(
            Map<String, Object> envelope, Scope expected, String label) {
        Scope actual = Scope.from(envelope);
        require(expected.equals(actual),
                label + " scope mismatch: expected=" + expected + " actual=" + actual);
    }

    private static void validateSecondaryScope(Scope scope) {
        require(WINDOW_ID.equals(scope.windowId()),
                "secondary scope has the wrong public window id: " + scope);
        require(WINDOW_ID.equals(scope.uiId()),
                "secondary scope has the wrong UI id: " + scope);
        require(scope.sessionId().startsWith(SESSION_PREFIX),
                "secondary scope lacks the opaque session prefix: " + scope);
        String uuid = scope.sessionId().substring(SESSION_PREFIX.length());
        try {
            UUID.fromString(uuid);
        } catch (IllegalArgumentException invalid) {
            fail("secondary scope session is not a UUID: " + scope.sessionId());
        }
    }

    private void complete() {
        require(openAResult && closeAResult && windowClosedA,
                "A did not complete before shutdown");
        require(openBResult && closeBResult && windowClosedB,
                "B did not complete before shutdown");
        require(retiredAResult && retiredBResult,
                "retired A/B typed results were incomplete before shutdown");
        require(openCResult && responseMetadataC && responseCreditC && responseBodyC,
                "C streamed response was incomplete before shutdown");
        require(liveCResult && closeCResult && windowClosedC,
                "C live command/lifecycle was incomplete before shutdown");
        require(closeMainResult && windowClosedMain,
                "main lifecycle was incomplete before shutdown");
        require(finished.compareAndSet(false, true), "duplicate shutdown envelope");
        System.err.println(PASS_MARKER
                + " A=" + scopeA.sessionId()
                + " B=" + scopeB.sessionId()
                + " C=" + scopeC.sessionId()
                + " requestIds=" + requestA.requestId()
                + "/" + requestB.requestId()
                + "/" + requestC.requestId());
        System.err.flush();
    }

    private void startWatchdog() {
        Thread watchdog = new Thread(() -> {
            try {
                Thread.sleep(WATCHDOG_MILLIS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return;
            }
            if (!finished.get()) {
                fail("watchdog timed out in phase " + phase);
            }
        }, "retired-scope-production-host-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
    }

    private static void writePidFile() throws IOException {
        String value = System.getenv("VATORI_RETIRED_SCOPE_PRODUCTION_HOST_PID_FILE");
        require(value != null && !value.isBlank(), "fixture PID file was not configured");
        Files.writeString(
                Path.of(value),
                Long.toString(ProcessHandle.current().pid()),
                StandardCharsets.UTF_8);
    }

    private static Map<String, Object> parseObject(String json) {
        Object parsed = new JsonParser(json).parse();
        if (!(parsed instanceof Map<?, ?>)) {
            fail("host envelope is not a JSON object");
            return Map.of();
        }
        Map<?, ?> values = (Map<?, ?>) parsed;
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) values;
        return result;
    }

    private static Map<String, Object> object(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (!(value instanceof Map<?, ?>)) {
            fail("missing object field " + key + ": " + values);
            return Map.of();
        }
        Map<?, ?> object = (Map<?, ?>) value;
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) object;
        return result;
    }

    private static String string(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (!(value instanceof String) || ((String) value).isBlank()) {
            fail("missing string field " + key + ": " + values);
            return "";
        }
        return (String) value;
    }

    private static Number number(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (!(value instanceof Number)) {
            fail("missing number field " + key + ": " + values);
            return 0;
        }
        return (Number) value;
    }

    private static <T> T required(T value, String label) {
        require(value != null, label + " is missing");
        return value;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            fail(message);
        }
    }

    private static void fail(String message) {
        System.err.println(FAIL_MARKER + " " + message);
        System.err.flush();
        Runtime.getRuntime().halt(86);
    }

    private static String quote(String value) {
        StringBuilder result = new StringBuilder(value.length() + 2).append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\b' -> result.append("\\b");
                case '\f' -> result.append("\\f");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (character < 0x20) {
                        result.append(String.format("\\u%04x", (int) character));
                    } else {
                        result.append(character);
                    }
                }
            }
        }
        return result.append('"').toString();
    }

    private record Scope(String windowId, String sessionId, String uiId) {
        private static Scope from(Map<String, Object> envelope) {
            return new Scope(
                    string(envelope, "windowId"),
                    string(envelope, "sessionId"),
                    string(envelope, "uiId"));
        }
    }

    private record Request(
            Scope scope, String requestId, String correlationId) {
        private boolean matches(Scope candidateScope, String candidateRequestId) {
            return scope.equals(candidateScope) && requestId.equals(candidateRequestId);
        }
    }

    private static final class JsonParser {
        private final String source;
        private int offset;

        private JsonParser(String source) {
            this.source = source;
        }

        private Object parse() {
            Object result = parseValue();
            skipWhitespace();
            if (offset != source.length()) {
                throw error("trailing JSON input");
            }
            return result;
        }

        private Object parseValue() {
            skipWhitespace();
            if (offset >= source.length()) {
                throw error("unexpected end of JSON input");
            }
            return switch (source.charAt(offset)) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> parseNumber();
            };
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> result = new LinkedHashMap<>();
            skipWhitespace();
            if (consume('}')) {
                return result;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                result.put(key, parseValue());
                skipWhitespace();
                if (consume('}')) {
                    return result;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> result = new ArrayList<>();
            skipWhitespace();
            if (consume(']')) {
                return result;
            }
            while (true) {
                result.add(parseValue());
                skipWhitespace();
                if (consume(']')) {
                    return result;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder result = new StringBuilder();
            while (offset < source.length()) {
                char character = source.charAt(offset++);
                if (character == '"') {
                    return result.toString();
                }
                if (character != '\\') {
                    result.append(character);
                    continue;
                }
                if (offset >= source.length()) {
                    throw error("unterminated JSON escape");
                }
                char escape = source.charAt(offset++);
                switch (escape) {
                    case '"', '\\', '/' -> result.append(escape);
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'u' -> result.append(parseUnicodeEscape());
                    default -> throw error("unsupported JSON escape");
                }
            }
            throw error("unterminated JSON string");
        }

        private char parseUnicodeEscape() {
            if (offset + 4 > source.length()) {
                throw error("short JSON unicode escape");
            }
            String hex = source.substring(offset, offset + 4);
            offset += 4;
            try {
                return (char) Integer.parseInt(hex, 16);
            } catch (NumberFormatException invalid) {
                throw error("invalid JSON unicode escape");
            }
        }

        private Object parseLiteral(String literal, Object value) {
            if (!source.startsWith(literal, offset)) {
                throw error("invalid JSON literal");
            }
            offset += literal.length();
            return value;
        }

        private Number parseNumber() {
            int start = offset;
            if (consume('-')) {
                // Sign consumed.
            }
            while (offset < source.length()
                    && Character.isDigit(source.charAt(offset))) {
                offset++;
            }
            boolean floatingPoint = false;
            if (consume('.')) {
                floatingPoint = true;
                while (offset < source.length()
                        && Character.isDigit(source.charAt(offset))) {
                    offset++;
                }
            }
            if (offset < source.length()
                    && (source.charAt(offset) == 'e' || source.charAt(offset) == 'E')) {
                floatingPoint = true;
                offset++;
                if (offset < source.length()
                        && (source.charAt(offset) == '+' || source.charAt(offset) == '-')) {
                    offset++;
                }
                while (offset < source.length()
                        && Character.isDigit(source.charAt(offset))) {
                    offset++;
                }
            }
            if (offset == start) {
                throw error("invalid JSON number");
            }
            String number = source.substring(start, offset);
            try {
                return floatingPoint ? Double.parseDouble(number) : Long.parseLong(number);
            } catch (NumberFormatException invalid) {
                throw error("invalid JSON number");
            }
        }

        private boolean consume(char expected) {
            if (offset < source.length() && source.charAt(offset) == expected) {
                offset++;
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            if (!consume(expected)) {
                throw error("expected '" + expected + "'");
            }
        }

        private void skipWhitespace() {
            while (offset < source.length()
                    && Character.isWhitespace(source.charAt(offset))) {
                offset++;
            }
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at offset " + offset);
        }
    }
}
