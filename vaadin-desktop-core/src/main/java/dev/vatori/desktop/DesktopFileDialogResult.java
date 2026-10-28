package dev.vatori.desktop;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A normalized terminal outcome from {@link DesktopFileDialog#openFileResult(String)}.
 * The original command result remains available for platform-specific diagnostics.
 */
public final class DesktopFileDialogResult {
    public enum Outcome {
        INVALIDATED,
        DENIED,
        CANCELED,
        SELECTED,
        ERROR
    }

    private final Outcome outcome;
    private final Optional<Path> selectedPath;
    private final Optional<String> cancelReason;
    private final DesktopCommandResult commandResult;

    private DesktopFileDialogResult(Outcome outcome,
            Optional<Path> selectedPath,
            Optional<String> cancelReason,
            DesktopCommandResult commandResult) {
        this.outcome = Objects.requireNonNull(outcome, "outcome");
        this.selectedPath = Objects.requireNonNull(selectedPath, "selectedPath");
        this.cancelReason = Objects.requireNonNull(cancelReason, "cancelReason");
        this.commandResult = Objects.requireNonNull(commandResult, "commandResult");
    }

    static DesktopFileDialogResult from(DesktopCommandResult commandResult) {
        Objects.requireNonNull(commandResult, "commandResult");
        Map<String, Object> payload = commandResult.payload();
        Optional<String> cancelReason = nonBlankString(payload.get("cancelReason"));

        if (Boolean.TRUE.equals(payload.get("operationInvalidated"))) {
            return new DesktopFileDialogResult(Outcome.INVALIDATED,
                    Optional.empty(), cancelReason, commandResult);
        }
        if ("denied".equals(payload.get("permissionDecision"))) {
            return new DesktopFileDialogResult(Outcome.DENIED,
                    Optional.empty(), cancelReason, commandResult);
        }
        if (commandResult.success()
                && Boolean.TRUE.equals(payload.get("canceled"))) {
            return new DesktopFileDialogResult(Outcome.CANCELED,
                    Optional.empty(), cancelReason, commandResult);
        }
        if (commandResult.success()) {
            Optional<Path> selectedPath = selectedPath(payload);
            if (selectedPath.isPresent()) {
                return new DesktopFileDialogResult(Outcome.SELECTED,
                        selectedPath, cancelReason, commandResult);
            }
        }
        return new DesktopFileDialogResult(Outcome.ERROR,
                Optional.empty(), cancelReason, commandResult);
    }

    public Outcome outcome() {
        return outcome;
    }

    public Optional<Path> selectedPath() {
        return selectedPath;
    }

    public Optional<String> cancelReason() {
        return cancelReason;
    }

    public DesktopCommandResult commandResult() {
        return commandResult;
    }

    private static Optional<Path> selectedPath(Map<String, Object> payload) {
        Optional<Path> selectedPath = path(payload.get("path"));
        if (selectedPath.isPresent()) {
            return selectedPath;
        }
        Object paths = payload.get("paths");
        if (paths instanceof Iterable<?> values) {
            for (Object value : values) {
                selectedPath = path(value);
                if (selectedPath.isPresent()) {
                    return selectedPath;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<Path> path(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Path.of(String.valueOf(value)));
        } catch (InvalidPathException ignored) {
            return Optional.empty();
        }
    }

    private static Optional<String> nonBlankString(Object value) {
        return value instanceof String text && !text.isBlank()
                ? Optional.of(text)
                : Optional.empty();
    }
}
