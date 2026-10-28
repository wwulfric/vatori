package dev.vatori.desktop;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class DesktopClipboardContractSmokeMain {
    private DesktopClipboardContractSmokeMain() {
    }

    public static void main(String[] args) {
        classifiesReadTextAndRetainsRawResult();
        acceptsAnEmptyReadTextValue();
        classifiesAnExactNoTextResult();
        classifiesReadInvalidationBeforeDenialAndText();
        classifiesReadDenialBeforeText();
        rejectsFailedMalformedAndIncompleteReadResults();
        preservesTheLegacyReadContractBySingleDelegation();
        keepsMalformedLegacyReadValuesEmpty();
        classifiesWrittenAndRetainsRawResult();
        classifiesWriteInvalidationBeforeDenialAndWritten();
        classifiesWriteDenialBeforeWritten();
        rejectsFailedMalformedAndIncompleteWriteResults();
        preservesTheLegacyWriteContractBySingleDelegation();
        rejectsNullTextBeforeInvokingTheBridge();
    }

    private static void classifiesReadTextAndRetainsRawResult() {
        DesktopCommandResult commandResult = DesktopCommandResult.ok(Map.of(
                "text", "clipboard value",
                "textPresent", true));
        DesktopClipboardReadResult result = read(commandResult);

        expectEquals(DesktopClipboardReadResult.Outcome.TEXT,
                result.outcome(), "read text outcome");
        expectEquals(Optional.of("clipboard value"), result.text(),
                "read text value");
        expectSame(commandResult, result.commandResult(),
                "read raw command result");
    }

    private static void acceptsAnEmptyReadTextValue() {
        DesktopClipboardReadResult result = read(
                DesktopCommandResult.ok(Map.of(
                        "text", "",
                        "textPresent", true)));

        expectEquals(DesktopClipboardReadResult.Outcome.TEXT,
                result.outcome(), "empty read text outcome");
        expectEquals(Optional.of(""), result.text(),
                "empty read text value");
    }

    private static void classifiesAnExactNoTextResult() {
        DesktopClipboardReadResult noText = read(
                DesktopCommandResult.ok(Map.of("textPresent", false)));

        expectEquals(DesktopClipboardReadResult.Outcome.NO_TEXT,
                noText.outcome(), "no-text outcome");
        expectEquals(Optional.empty(), noText.text(), "no-text value");
    }

    private static void classifiesReadInvalidationBeforeDenialAndText() {
        DesktopCommandResult commandResult = DesktopCommandResult.error(Map.of(
                "operationInvalidated", true,
                "permissionDecision", "denied",
                "text", "must not escape",
                "textPresent", true), "read failed");
        DesktopClipboardReadResult result = read(commandResult);

        expectEquals(DesktopClipboardReadResult.Outcome.INVALIDATED,
                result.outcome(), "read invalidation precedence");
        expectEquals(Optional.empty(), result.text(),
                "read invalidation text");
        expectSame(commandResult, result.commandResult(),
                "read invalidation raw result");
    }

    private static void classifiesReadDenialBeforeText() {
        DesktopClipboardReadResult result = read(
                DesktopCommandResult.error(Map.of(
                        "permissionDecision", "denied",
                        "text", "must not escape",
                        "textPresent", true), "read failed"));

        expectEquals(DesktopClipboardReadResult.Outcome.DENIED,
                result.outcome(), "read denial precedence");
        expectEquals(Optional.empty(), result.text(), "read denial text");
    }

    private static void rejectsFailedMalformedAndIncompleteReadResults() {
        DesktopClipboardReadResult failed = read(
                DesktopCommandResult.error(Map.of(
                        "text", "failed value",
                        "textPresent", true), "read failed"));
        DesktopClipboardReadResult nonString = read(
                DesktopCommandResult.ok(Map.of(
                        "text", 42,
                        "textPresent", true)));
        DesktopClipboardReadResult incomplete = read(
                DesktopCommandResult.ok(Map.of("textPresent", true)));
        DesktopClipboardReadResult missingPresence = read(
                DesktopCommandResult.ok(Map.of("text", "missing flag")));
        DesktopClipboardReadResult contradictoryText = read(
                DesktopCommandResult.ok(Map.of(
                        "text", "must not escape",
                        "textPresent", false)));
        Map<String, Object> nullTextPayload = new LinkedHashMap<>();
        nullTextPayload.put("text", null);
        nullTextPayload.put("textPresent", true);
        DesktopClipboardReadResult nullText = read(
                DesktopCommandResult.ok(nullTextPayload));
        Map<String, Object> nullNoTextPayload = new LinkedHashMap<>();
        nullNoTextPayload.put("textPresent", false);
        nullNoTextPayload.put("text", null);
        DesktopClipboardReadResult nullNoText = read(
                DesktopCommandResult.ok(nullNoTextPayload));
        DesktopClipboardReadResult failedNoText = read(
                DesktopCommandResult.error(Map.of("textPresent", false),
                        "read failed"));

        for (DesktopClipboardReadResult result : new DesktopClipboardReadResult[]{
                failed, nonString, incomplete, missingPresence,
                contradictoryText, nullText, nullNoText, failedNoText}) {
            expectEquals(DesktopClipboardReadResult.Outcome.ERROR,
                    result.outcome(), "malformed read outcome");
            expectEquals(Optional.empty(), result.text(),
                    "malformed read text");
        }
    }

    private static void preservesTheLegacyReadContractBySingleDelegation() {
        RecordingBridge bridge = new RecordingBridge(
                DesktopCommandResult.ok(Map.of(
                        "text", "legacy value",
                        "textPresent", true)));
        DesktopClipboard clipboard = new DesktopClipboard(bridge);

        expectEquals(Optional.of("legacy value"), clipboard.readText(),
                "legacy read value");
        expectEquals(1, bridge.invocationCount,
                "legacy read invocation count");
        expectEquals("clipboard.readText", bridge.command.name(),
                "legacy read command name");
        expectEquals(Map.of(), bridge.command.payload(),
                "legacy read command payload");
    }

    private static void keepsMalformedLegacyReadValuesEmpty() {
        RecordingBridge bridge = new RecordingBridge(
                DesktopCommandResult.ok(Map.of("text", 42)));
        DesktopClipboard clipboard = new DesktopClipboard(bridge);

        expectEquals(Optional.empty(), clipboard.readText(),
                "malformed legacy read value");
        expectEquals(1, bridge.invocationCount,
                "malformed legacy read invocation count");
    }

    private static void classifiesWrittenAndRetainsRawResult() {
        DesktopCommandResult commandResult = DesktopCommandResult.ok(
                Map.of("written", true));
        DesktopClipboardWriteResult result = write(commandResult, "value");

        expectEquals(DesktopClipboardWriteResult.Outcome.WRITTEN,
                result.outcome(), "written outcome");
        expectSame(commandResult, result.commandResult(),
                "write raw command result");
    }

    private static void classifiesWriteInvalidationBeforeDenialAndWritten() {
        DesktopCommandResult commandResult = DesktopCommandResult.error(Map.of(
                "operationInvalidated", true,
                "permissionDecision", "denied",
                "written", true), "write failed");
        DesktopClipboardWriteResult result = write(commandResult, "value");

        expectEquals(DesktopClipboardWriteResult.Outcome.INVALIDATED,
                result.outcome(), "write invalidation precedence");
        expectSame(commandResult, result.commandResult(),
                "write invalidation raw result");
    }

    private static void classifiesWriteDenialBeforeWritten() {
        DesktopClipboardWriteResult result = write(
                DesktopCommandResult.error(Map.of(
                        "permissionDecision", "denied",
                        "written", true), "write failed"),
                "value");

        expectEquals(DesktopClipboardWriteResult.Outcome.DENIED,
                result.outcome(), "write denial precedence");
    }

    private static void rejectsFailedMalformedAndIncompleteWriteResults() {
        DesktopClipboardWriteResult failed = write(
                DesktopCommandResult.error(Map.of("written", true),
                        "write failed"),
                "value");
        DesktopClipboardWriteResult falseWritten = write(
                DesktopCommandResult.ok(Map.of("written", false)),
                "value");
        DesktopClipboardWriteResult malformed = write(
                DesktopCommandResult.ok(Map.of("written", "true")),
                "value");
        DesktopClipboardWriteResult incomplete = write(
                DesktopCommandResult.ok(Map.of()), "value");

        for (DesktopClipboardWriteResult result
                : new DesktopClipboardWriteResult[]{
                        failed, falseWritten, malformed, incomplete}) {
            expectEquals(DesktopClipboardWriteResult.Outcome.ERROR,
                    result.outcome(), "malformed write outcome");
        }
    }

    private static void preservesTheLegacyWriteContractBySingleDelegation() {
        DesktopCommandResult commandResult = DesktopCommandResult.ok(
                Map.of("written", true));
        RecordingBridge bridge = new RecordingBridge(commandResult);
        DesktopClipboard clipboard = new DesktopClipboard(bridge);

        expectSame(commandResult, clipboard.writeText("legacy value"),
                "legacy write raw result");
        expectEquals(1, bridge.invocationCount,
                "legacy write invocation count");
        expectEquals("clipboard.writeText", bridge.command.name(),
                "legacy write command name");
        expectEquals(Map.of("text", "legacy value"),
                bridge.command.payload(), "legacy write command payload");
    }

    private static void rejectsNullTextBeforeInvokingTheBridge() {
        RecordingBridge typedBridge = new RecordingBridge(
                DesktopCommandResult.ok(Map.of("written", true)));
        DesktopClipboard typedClipboard = new DesktopClipboard(typedBridge);
        expectNullPointer(
                () -> typedClipboard.writeTextResult(null),
                "typed null write");
        expectEquals(0, typedBridge.invocationCount,
                "typed null write invocation count");

        RecordingBridge legacyBridge = new RecordingBridge(
                DesktopCommandResult.ok(Map.of("written", true)));
        DesktopClipboard legacyClipboard = new DesktopClipboard(legacyBridge);
        expectNullPointer(
                () -> legacyClipboard.writeText(null),
                "legacy null write");
        expectEquals(0, legacyBridge.invocationCount,
                "legacy null write invocation count");
    }

    private static DesktopClipboardReadResult read(
            DesktopCommandResult commandResult) {
        return new DesktopClipboard(new RecordingBridge(commandResult))
                .readTextResult();
    }

    private static DesktopClipboardWriteResult write(
            DesktopCommandResult commandResult, String text) {
        return new DesktopClipboard(new RecordingBridge(commandResult))
                .writeTextResult(text);
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

    private static void expectNullPointer(Runnable action, String label) {
        try {
            action.run();
        } catch (NullPointerException expected) {
            return;
        }
        throw new AssertionError(label + ": expected NullPointerException");
    }

    private static final class RecordingBridge implements NativeBridge {
        private final DesktopCommandResult result;
        private int invocationCount;
        private DesktopCommand command;

        private RecordingBridge(DesktopCommandResult result) {
            this.result = result;
        }

        @Override
        public DesktopCommandResult invoke(DesktopCommand command) {
            invocationCount++;
            this.command = command;
            return result;
        }
    }
}
