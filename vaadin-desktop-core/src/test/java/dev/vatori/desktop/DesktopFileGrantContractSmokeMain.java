package dev.vatori.desktop;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DesktopFileGrantContractSmokeMain {
    private static final String GRANT_ID =
            "2f904b69-fdc3-4f43-bc65-b4b3ee323017";

    private DesktopFileGrantContractSmokeMain() {
    }

    public static void main(String[] args) {
        classifiesSelectedGrantAndRetainsBoundBridge();
        capturesAndRetainsOneBoundBridgeForTheGrantLifetime();
        classifiesSelectionTerminalOutcomesInOrder();
        rejectsMalformedAndIncompleteSelections();
        emitsExactReadAndReleaseCommands();
        classifiesEveryReadOutcome();
        rejectsMalformedSuccessfulReadsAndDefensivelyCopiesBytes();
        classifiesEveryReleaseOutcome();
        settlesDoubleReleaseLocallyButStillSendsReadAfterRelease();
    }

    private static void classifiesSelectedGrantAndRetainsBoundBridge() {
        DesktopCommandResult selected = selectedResult();
        RecordingBridge bridge = new RecordingBridge(selected);

        DesktopFileReadGrantResult result = new DesktopFileDialog(bridge)
                .openReadGrant("Choose report");

        expectEquals(DesktopFileReadGrantResult.Outcome.SELECTED,
                result.outcome(), "selected outcome");
        expectEquals(false, result.commandResult().payload()
                        .containsKey("grantId"),
                "selected opaque id hidden");
        DesktopFileReadGrant grant = result.grant().orElseThrow();
        expectEquals("report.txt", grant.filename(), "selected filename");
        expectEquals(19L, grant.byteCount(), "selected byte count");
        expectEquals(4096, grant.maxReadBytes(), "selected read limit");
        expectEquals(true, grant.securityScoped(),
                "selected security scope marker");
        expectEquals(1, bridge.invocationCount(), "selection invocation count");
        expectCommand(bridge.commands().get(0), "fileDialog.openReadGrant",
                Map.of("title", "Choose report"), "selection command");
    }

    private static void capturesAndRetainsOneBoundBridgeForTheGrantLifetime() {
        byte[] expected = "captured".getBytes(StandardCharsets.UTF_8);
        RecordingBridge captured = new RecordingBridge(
                selectedResult(),
                readResult(0, expected, false),
                DesktopCommandResult.ok(Map.of("released", true)));
        CaptureRedirectBridge original = new CaptureRedirectBridge(captured);

        DesktopFileReadGrant grant = new DesktopFileDialog(original)
                .openReadGrant("Choose captured report").grant().orElseThrow();
        DesktopFileReadResult read = grant.read(0, 32);
        DesktopFileGrantReleaseResult release = grant.release();

        expectEquals(1, original.captureCount,
                "bound bridge capture count");
        expectEquals(0, original.invocationCount,
                "unbound bridge invocation count");
        expectEquals(3, captured.invocationCount(),
                "captured bridge invocation count");
        expectEquals(DesktopFileReadResult.Outcome.DATA, read.outcome(),
                "captured bridge read outcome");
        expectArrayEquals(expected, read.bytes(),
                "captured bridge read bytes");
        expectEquals(DesktopFileGrantReleaseResult.Outcome.RELEASED,
                release.outcome(), "captured bridge release outcome");
        expectCommand(captured.commands().get(0),
                "fileDialog.openReadGrant",
                Map.of("title", "Choose captured report"),
                "captured selection command");
        expectCommand(captured.commands().get(1), "fileGrant.read", Map.of(
                "grantId", GRANT_ID,
                "offset", 0L,
                "maxBytes", 32), "captured read command");
        expectCommand(captured.commands().get(2), "fileGrant.release", Map.of(
                "grantId", GRANT_ID), "captured release command");
    }

    private static void classifiesSelectionTerminalOutcomesInOrder() {
        DesktopCommandResult invalidated = DesktopCommandResult.error(Map.of(
                "operationInvalidated", true,
                "permissionDecision", "denied",
                "canceled", true), "invalidated");
        expectSelectionOutcome(invalidated,
                DesktopFileReadGrantResult.Outcome.INVALIDATED,
                "selection invalidation precedence");

        DesktopCommandResult denied = DesktopCommandResult.error(Map.of(
                "permissionDecision", "denied",
                "canceled", true), "denied");
        expectSelectionOutcome(denied,
                DesktopFileReadGrantResult.Outcome.DENIED,
                "selection denial precedence");

        expectSelectionOutcome(DesktopCommandResult.ok(Map.of(
                        "canceled", true)),
                DesktopFileReadGrantResult.Outcome.CANCELED,
                "selection cancellation");

        expectSelectionOutcome(DesktopCommandResult.error(Map.of(
                        "canceled", true), "failed cancellation"),
                DesktopFileReadGrantResult.Outcome.ERROR,
                "failed selection cancellation");
    }

    private static void rejectsMalformedAndIncompleteSelections() {
        List<Map<String, Object>> malformed = new ArrayList<>();
        malformed.add(selectionPayload("not-a-uuid", "report.txt", 19L,
                4096, true));
        malformed.add(selectionPayload(GRANT_ID, " ", 19L, 4096, true));
        malformed.add(selectionPayload(GRANT_ID, "report.txt", -1L, 4096,
                true));
        malformed.add(selectionPayload(GRANT_ID, "report.txt", 19L, 0,
                true));
        malformed.add(selectionPayload(GRANT_ID, "report.txt", 19L, 4096,
                "true"));
        Map<String, Object> missingCanceled = new LinkedHashMap<>(
                selectionPayload(GRANT_ID, "report.txt", 19L, 4096, true));
        missingCanceled.remove("canceled");
        malformed.add(missingCanceled);

        for (Map<String, Object> payload : malformed) {
            DesktopFileReadGrantResult result = selection(
                    DesktopCommandResult.ok(payload));
            expectEquals(DesktopFileReadGrantResult.Outcome.ERROR,
                    result.outcome(), "malformed selection outcome");
            expectEquals(Optional.empty(), result.grant(),
                    "malformed selection grant");
        }

        DesktopFileReadGrantResult failedWithGrant = selection(
                DesktopCommandResult.error(selectionPayload(
                        GRANT_ID, "report.txt", 19L, 4096, true),
                        "selection failed"));
        expectEquals(DesktopFileReadGrantResult.Outcome.ERROR,
                failedWithGrant.outcome(), "failed selection with grant");
    }

    private static void emitsExactReadAndReleaseCommands() {
        byte[] expected = "range".getBytes(StandardCharsets.UTF_8);
        RecordingBridge bridge = new RecordingBridge(
                selectedResult(),
                readResult(7, expected, false),
                DesktopCommandResult.ok(Map.of("released", true)));
        DesktopFileReadGrant grant = new DesktopFileDialog(bridge)
                .openReadGrant("Choose report").grant().orElseThrow();

        DesktopFileReadResult read = grant.read(7, 32);
        DesktopFileGrantReleaseResult release = grant.release();

        expectEquals(DesktopFileReadResult.Outcome.DATA, read.outcome(),
                "exact read outcome");
        expectEquals(DesktopFileGrantReleaseResult.Outcome.RELEASED,
                release.outcome(), "exact release outcome");
        expectEquals(3, bridge.invocationCount(), "exact command count");
        expectCommand(bridge.commands().get(1), "fileGrant.read", Map.of(
                "grantId", GRANT_ID,
                "offset", 7L,
                "maxBytes", 32), "read command");
        expectCommand(bridge.commands().get(2), "fileGrant.release", Map.of(
                "grantId", GRANT_ID), "release command");
    }

    private static void classifiesEveryReadOutcome() {
        expectReadOutcome(DesktopCommandResult.error(Map.of(
                        "operationInvalidated", true,
                        "permissionDecision", "denied",
                        "grantUnavailable", true,
                        "errorCode", "grant-unavailable"), "invalidated"),
                DesktopFileReadResult.Outcome.INVALIDATED,
                "read invalidation precedence");
        expectReadOutcome(DesktopCommandResult.error(Map.of(
                        "permissionDecision", "denied",
                        "grantUnavailable", true,
                        "errorCode", "grant-unavailable"), "denied"),
                DesktopFileReadResult.Outcome.DENIED,
                "read denial precedence");
        expectReadOutcome(unavailableResult(),
                DesktopFileReadResult.Outcome.UNAVAILABLE,
                "read unavailable");
        expectReadOutcome(DesktopCommandResult.error(Map.of(
                        "errorCode", "invalid-read-range"), "invalid range"),
                DesktopFileReadResult.Outcome.INVALID_RANGE,
                "read invalid range");

        byte[] data = "payload".getBytes(StandardCharsets.UTF_8);
        DesktopFileReadResult dataResult = read(readResult(0, data, true),
                0, 64);
        expectEquals(DesktopFileReadResult.Outcome.DATA,
                dataResult.outcome(), "read data outcome");
        expectArrayEquals(data, dataResult.bytes(), "read data bytes");
        expectEquals(0L, dataResult.offset(), "read data offset");
        expectEquals((long) data.length, dataResult.nextOffset(),
                "read data next offset");
        expectEquals(true, dataResult.endOfFile(), "read data eof marker");

        DesktopFileReadResult eof = read(readResult(19, new byte[0], true),
                19, 64);
        expectEquals(DesktopFileReadResult.Outcome.END_OF_FILE,
                eof.outcome(), "read eof outcome");
        expectArrayEquals(new byte[0], eof.bytes(), "read eof bytes");
        expectEquals(19L, eof.offset(), "read eof offset");
        expectEquals(19L, eof.nextOffset(), "read eof next offset");
        expectEquals(true, eof.endOfFile(), "read eof marker");

        expectReadOutcome(DesktopCommandResult.error("generic read error"),
                DesktopFileReadResult.Outcome.ERROR, "read generic error");
    }

    private static void rejectsMalformedSuccessfulReadsAndDefensivelyCopiesBytes() {
        Map<String, Object> malformedBase64 = readPayload(
                0, 3, 3, false, "%%%not-base64%%%");
        expectEquals(DesktopFileReadResult.Outcome.ERROR,
                read(DesktopCommandResult.ok(malformedBase64), 0, 16).outcome(),
                "malformed base64 outcome");

        List<Map<String, Object>> malformed = List.of(
                readPayload(0, 3, 3, false,
                        Base64.getEncoder().encodeToString(new byte[]{1, 2})),
                readPayload(1, 3, 4, false,
                        Base64.getEncoder().encodeToString(new byte[]{1, 2, 3})),
                readPayload(0, 3, 4, false,
                        Base64.getEncoder().encodeToString(new byte[]{1, 2, 3})),
                readPayload(0, 0, 0, false, ""));
        for (Map<String, Object> payload : malformed) {
            expectEquals(DesktopFileReadResult.Outcome.ERROR,
                    read(DesktopCommandResult.ok(payload), 0, 16).outcome(),
                    "malformed successful read outcome");
        }

        byte[] original = new byte[]{4, 5, 6};
        DesktopFileReadResult result = read(readResult(0, original, false),
                0, 16);
        byte[] first = result.bytes();
        first[0] = 99;
        expectArrayEquals(original, result.bytes(), "defensive read bytes");

        DesktopFileReadResult oversizedRequest = read(
                readResult(0, original, false), 0, 4097);
        expectEquals(DesktopFileReadResult.Outcome.ERROR,
                oversizedRequest.outcome(), "oversized successful read");
    }

    private static void classifiesEveryReleaseOutcome() {
        expectReleaseOutcome(DesktopCommandResult.error(Map.of(
                        "operationInvalidated", true,
                        "permissionDecision", "denied",
                        "grantUnavailable", true,
                        "errorCode", "grant-unavailable"), "invalidated"),
                DesktopFileGrantReleaseResult.Outcome.INVALIDATED,
                "release invalidation precedence");
        expectReleaseOutcome(DesktopCommandResult.error(Map.of(
                        "permissionDecision", "denied",
                        "grantUnavailable", true,
                        "errorCode", "grant-unavailable"), "denied"),
                DesktopFileGrantReleaseResult.Outcome.DENIED,
                "release denial precedence");
        expectReleaseOutcome(unavailableResult(),
                DesktopFileGrantReleaseResult.Outcome.UNAVAILABLE,
                "release unavailable");
        expectReleaseOutcome(DesktopCommandResult.ok(Map.of(
                        "released", true)),
                DesktopFileGrantReleaseResult.Outcome.RELEASED,
                "release released");
        expectReleaseOutcome(DesktopCommandResult.ok(Map.of(
                        "alreadyReleased", true)),
                DesktopFileGrantReleaseResult.Outcome.ERROR,
                "release host cannot synthesize local already released");
        expectReleaseOutcome(DesktopCommandResult.error(Map.of(
                        "released", true), "release failed"),
                DesktopFileGrantReleaseResult.Outcome.ERROR,
                "release failed");
        expectReleaseOutcome(DesktopCommandResult.ok(Map.of(
                        "released", false)),
                DesktopFileGrantReleaseResult.Outcome.ERROR,
                "release incomplete");
    }

    private static void settlesDoubleReleaseLocallyButStillSendsReadAfterRelease() {
        DesktopCommandResult released = DesktopCommandResult.ok(Map.of(
                "released", true));
        RecordingBridge bridge = new RecordingBridge(
                selectedResult(), released, unavailableResult());
        DesktopFileReadGrant grant = new DesktopFileDialog(bridge)
                .openReadGrant("Choose report").grant().orElseThrow();

        DesktopFileGrantReleaseResult firstRelease = grant.release();
        DesktopFileGrantReleaseResult secondRelease = grant.release();
        DesktopFileReadResult readAfterRelease = grant.read(0, 16);

        expectEquals(DesktopFileGrantReleaseResult.Outcome.RELEASED,
                firstRelease.outcome(), "first release outcome");
        expectEquals(DesktopFileGrantReleaseResult.Outcome.ALREADY_RELEASED,
                secondRelease.outcome(), "second release outcome");
        expectSame(released, secondRelease.commandResult(),
                "local double release raw result");
        expectEquals(DesktopFileReadResult.Outcome.UNAVAILABLE,
                readAfterRelease.outcome(), "read after release outcome");
        expectEquals(3, bridge.invocationCount(),
                "read after release invocation count");
        expectCommand(bridge.commands().get(2), "fileGrant.read", Map.of(
                "grantId", GRANT_ID,
                "offset", 0L,
                "maxBytes", 16), "read after release command");
    }

    private static DesktopCommandResult selectedResult() {
        return DesktopCommandResult.ok(selectionPayload(
                GRANT_ID, "report.txt", 19L, 4096, true));
    }

    private static Map<String, Object> selectionPayload(String grantId,
            String filename, Object byteCount, Object maxReadBytes,
            Object securityScoped) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("canceled", false);
        payload.put("grantId", grantId);
        payload.put("filename", filename);
        payload.put("byteCount", byteCount);
        payload.put("maxReadBytes", maxReadBytes);
        payload.put("securityScoped", securityScoped);
        return payload;
    }

    private static DesktopCommandResult readResult(long offset, byte[] bytes,
            boolean endOfFile) {
        return DesktopCommandResult.ok(readPayload(
                offset, bytes.length, offset + bytes.length, endOfFile,
                Base64.getEncoder().encodeToString(bytes)));
    }

    private static Map<String, Object> readPayload(long offset,
            int bytesRead, long nextOffset, boolean endOfFile,
            String bodyBase64) {
        return Map.of(
                "bodyBase64", bodyBase64,
                "bytesRead", bytesRead,
                "offset", offset,
                "nextOffset", nextOffset,
                "endOfFile", endOfFile);
    }

    private static DesktopCommandResult unavailableResult() {
        return DesktopCommandResult.error(Map.of(
                "grantUnavailable", true,
                "errorCode", "grant-unavailable"), "grant unavailable");
    }

    private static DesktopFileReadGrantResult selection(
            DesktopCommandResult commandResult) {
        return new DesktopFileDialog(new RecordingBridge(commandResult))
                .openReadGrant("Choose report");
    }

    private static DesktopFileReadResult read(
            DesktopCommandResult commandResult, long offset, int maxBytes) {
        RecordingBridge bridge = new RecordingBridge(
                selectedResult(), commandResult);
        DesktopFileReadGrant grant = new DesktopFileDialog(bridge)
                .openReadGrant("Choose report").grant().orElseThrow();
        return grant.read(offset, maxBytes);
    }

    private static void expectSelectionOutcome(
            DesktopCommandResult commandResult,
            DesktopFileReadGrantResult.Outcome expected, String label) {
        DesktopFileReadGrantResult result = selection(commandResult);
        expectEquals(expected, result.outcome(), label);
        expectEquals(Optional.empty(), result.grant(), label + " grant");
    }

    private static void expectReadOutcome(DesktopCommandResult commandResult,
            DesktopFileReadResult.Outcome expected, String label) {
        DesktopFileReadResult result = read(commandResult, 0, 16);
        expectEquals(expected, result.outcome(), label);
        expectArrayEquals(new byte[0], result.bytes(), label + " bytes");
    }

    private static void expectReleaseOutcome(
            DesktopCommandResult commandResult,
            DesktopFileGrantReleaseResult.Outcome expected, String label) {
        RecordingBridge bridge = new RecordingBridge(
                selectedResult(), commandResult);
        DesktopFileReadGrant grant = new DesktopFileDialog(bridge)
                .openReadGrant("Choose report").grant().orElseThrow();
        expectEquals(expected, grant.release().outcome(), label);
    }

    private static void expectCommand(DesktopCommand command,
            String expectedName, Map<String, Object> expectedPayload,
            String label) {
        expectEquals(expectedName, command.name(), label + " name");
        expectEquals(expectedPayload, command.payload(), label + " payload");
    }

    private static void expectEquals(Object expected, Object actual,
            String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected=" + expected
                    + " actual=" + actual);
        }
    }

    private static void expectSame(Object expected, Object actual,
            String label) {
        if (expected != actual) {
            throw new AssertionError(label + ": expected same instance");
        }
    }

    private static void expectArrayEquals(byte[] expected, byte[] actual,
            String label) {
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(label + ": expected="
                    + Arrays.toString(expected) + " actual="
                    + Arrays.toString(actual));
        }
    }

    private static final class RecordingBridge implements NativeBridge {
        private final Deque<DesktopCommandResult> results = new ArrayDeque<>();
        private final List<DesktopCommand> commands = new ArrayList<>();

        private RecordingBridge(DesktopCommandResult... results) {
            this.results.addAll(List.of(results));
        }

        @Override
        public DesktopCommandResult invoke(DesktopCommand command) {
            commands.add(command);
            DesktopCommandResult result = results.pollFirst();
            if (result == null) {
                throw new AssertionError("Unexpected command: " + command);
            }
            return result;
        }

        private int invocationCount() {
            return commands.size();
        }

        private List<DesktopCommand> commands() {
            return List.copyOf(commands);
        }
    }

    private static final class CaptureRedirectBridge implements NativeBridge {
        private final NativeBridge captured;
        private int captureCount;
        private int invocationCount;

        private CaptureRedirectBridge(NativeBridge captured) {
            this.captured = captured;
        }

        @Override
        public DesktopCommandResult invoke(DesktopCommand command) {
            invocationCount++;
            throw new AssertionError("Unbound bridge invoked: " + command);
        }

        @Override
        public NativeBridge captureCapabilityContext() {
            captureCount++;
            return captured;
        }
    }
}
