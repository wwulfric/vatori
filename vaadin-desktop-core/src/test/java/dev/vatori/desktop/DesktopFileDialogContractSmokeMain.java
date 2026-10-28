package dev.vatori.desktop;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DesktopFileDialogContractSmokeMain {
    private DesktopFileDialogContractSmokeMain() {
    }

    public static void main(String[] args) {
        classifiesSelectedPathAndRetainsRawResult();
        acceptsTheHostPathsArrayAsSelectionEvidence();
        ignoresMalformedHostPathsWithoutEscapingTheTypedContract();
        classifiesSuccessfulCancellationBeforeSelection();
        classifiesInvalidationBeforePermissionDenial();
        classifiesPermissionDenialBeforeCancellation();
        treatsInProgressRejectionAsAnError();
        treatsOtherFailedAndIncompleteResultsAsErrors();
        preservesTheLegacyOpenFileContractByDelegation();
        preservesTheLegacyEmptyResultForInvalidation();
    }

    private static void classifiesSelectedPathAndRetainsRawResult() {
        DesktopCommandResult commandResult = DesktopCommandResult.ok(Map.of(
                "canceled", false,
                "path", "/tmp/selected.txt",
                "paths", List.of("/tmp/selected.txt")));
        DesktopFileDialogResult result = invoke(commandResult);

        expectEquals(DesktopFileDialogResult.Outcome.SELECTED, result.outcome(),
                "selected outcome");
        expectEquals(Optional.of(Path.of("/tmp/selected.txt")),
                result.selectedPath(), "selected path");
        expectEquals(Optional.empty(), result.cancelReason(),
                "selected cancel reason");
        expectSame(commandResult, result.commandResult(), "raw command result");
    }

    private static void acceptsTheHostPathsArrayAsSelectionEvidence() {
        DesktopFileDialogResult result = invoke(DesktopCommandResult.ok(Map.of(
                "canceled", false,
                "paths", List.of("/tmp/from-paths.txt"))));

        expectEquals(DesktopFileDialogResult.Outcome.SELECTED, result.outcome(),
                "paths-array outcome");
        expectEquals(Optional.of(Path.of("/tmp/from-paths.txt")),
                result.selectedPath(), "paths-array selection");
    }

    private static void ignoresMalformedHostPathsWithoutEscapingTheTypedContract() {
        String malformedPath = String.valueOf('\0') + "malformed";
        DesktopFileDialogResult fallback = invoke(DesktopCommandResult.ok(Map.of(
                "canceled", false,
                "path", malformedPath,
                "paths", List.of(malformedPath, "/tmp/fallback.txt"))));
        DesktopFileDialogResult malformedOnly = invoke(
                DesktopCommandResult.ok(Map.of(
                        "canceled", false,
                        "path", malformedPath)));

        expectEquals(DesktopFileDialogResult.Outcome.SELECTED, fallback.outcome(),
                "malformed primary path fallback outcome");
        expectEquals(Optional.of(Path.of("/tmp/fallback.txt")),
                fallback.selectedPath(), "malformed primary path fallback");
        expectEquals(DesktopFileDialogResult.Outcome.ERROR,
                malformedOnly.outcome(), "malformed-only path outcome");
        expectEquals(Optional.empty(), malformedOnly.selectedPath(),
                "malformed-only selected path");
    }

    private static void classifiesSuccessfulCancellationBeforeSelection() {
        DesktopFileDialogResult result = invoke(DesktopCommandResult.ok(Map.of(
                "canceled", true,
                "path", "/tmp/must-not-be-selected.txt",
                "paths", List.of("/tmp/must-not-be-selected.txt"))));

        expectEquals(DesktopFileDialogResult.Outcome.CANCELED, result.outcome(),
                "successful cancellation outcome");
        expectEquals(Optional.empty(), result.selectedPath(),
                "successful cancellation path");
    }

    private static void classifiesInvalidationBeforePermissionDenial() {
        DesktopCommandResult commandResult = DesktopCommandResult.error(Map.of(
                "canceled", true,
                "operationInvalidated", true,
                "cancelReason", "owner-window-closed",
                "permissionDecision", "denied"), "owner closed");
        DesktopFileDialogResult result = invoke(commandResult);

        expectEquals(DesktopFileDialogResult.Outcome.INVALIDATED, result.outcome(),
                "invalidation precedence");
        expectEquals(Optional.of("owner-window-closed"), result.cancelReason(),
                "invalidation reason");
        expectSame(commandResult, result.commandResult(), "invalidation raw result");
    }

    private static void classifiesPermissionDenialBeforeCancellation() {
        DesktopFileDialogResult result = invoke(DesktopCommandResult.ok(Map.of(
                "canceled", true,
                "permissionDecision", "denied",
                "permissionReason", "manifest-denied")));

        expectEquals(DesktopFileDialogResult.Outcome.DENIED, result.outcome(),
                "denial precedence");
        expectEquals(Optional.empty(), result.selectedPath(), "denial path");
    }

    private static void treatsInProgressRejectionAsAnError() {
        DesktopFileDialogResult result = invoke(DesktopCommandResult.error(Map.of(
                "canceled", true,
                "paths", List.of(),
                "permissionDecision", "allowed"),
                "File dialog already in progress"));

        expectEquals(DesktopFileDialogResult.Outcome.ERROR, result.outcome(),
                "in-progress outcome");
        expectEquals(Optional.empty(), result.cancelReason(),
                "in-progress cancel reason");
    }

    private static void treatsOtherFailedAndIncompleteResultsAsErrors() {
        DesktopFileDialogResult failed = invoke(
                DesktopCommandResult.error("Native panel failed"));
        DesktopFileDialogResult incomplete = invoke(
                DesktopCommandResult.ok(Map.of("canceled", false, "paths", List.of())));

        expectEquals(DesktopFileDialogResult.Outcome.ERROR, failed.outcome(),
                "generic error outcome");
        expectEquals(DesktopFileDialogResult.Outcome.ERROR, incomplete.outcome(),
                "incomplete success outcome");
    }

    private static void preservesTheLegacyOpenFileContractByDelegation() {
        RecordingBridge bridge = new RecordingBridge(DesktopCommandResult.ok(Map.of(
                "canceled", false,
                "path", "/tmp/legacy.txt",
                "paths", List.of("/tmp/legacy.txt"))));
        DesktopFileDialog dialog = new DesktopFileDialog(bridge);

        expectEquals(Optional.of(Path.of("/tmp/legacy.txt")),
                dialog.openFile("Choose a file"), "legacy selected path");
        expectEquals(1, bridge.invocationCount, "legacy invocation count");
        expectEquals("fileDialog.openFile", bridge.command.name(),
                "legacy command name");
        expectEquals(Map.of("title", "Choose a file"), bridge.command.payload(),
                "legacy command payload");
    }

    private static void preservesTheLegacyEmptyResultForInvalidation() {
        DesktopCommandResult invalidated = DesktopCommandResult.error(Map.of(
                "canceled", true,
                "operationInvalidated", true,
                "cancelReason", "owner-window-closed"), "owner closed");
        DesktopFileDialog dialog = new DesktopFileDialog(
                new RecordingBridge(invalidated));

        expectEquals(Optional.empty(), dialog.openFile("Choose a file"),
                "legacy invalidated result");
    }

    private static DesktopFileDialogResult invoke(DesktopCommandResult commandResult) {
        return new DesktopFileDialog(new RecordingBridge(commandResult))
                .openFileResult("Choose a file");
    }

    private static void expectEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected=" + expected + " actual=" + actual);
        }
    }

    private static void expectSame(Object expected, Object actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + ": expected same instance");
        }
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
