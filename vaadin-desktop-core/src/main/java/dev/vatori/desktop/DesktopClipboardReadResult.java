package dev.vatori.desktop;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A normalized terminal outcome from
 * {@link DesktopClipboard#readTextResult()}.
 * The original command result remains available for platform-specific
 * diagnostics.
 */
public final class DesktopClipboardReadResult {
    public enum Outcome {
        INVALIDATED,
        DENIED,
        TEXT,
        NO_TEXT,
        ERROR
    }

    private final Outcome outcome;
    private final Optional<String> text;
    private final DesktopCommandResult commandResult;

    private DesktopClipboardReadResult(Outcome outcome,
            Optional<String> text, DesktopCommandResult commandResult) {
        this.outcome = Objects.requireNonNull(outcome, "outcome");
        this.text = Objects.requireNonNull(text, "text");
        this.commandResult = Objects.requireNonNull(commandResult,
                "commandResult");
    }

    static DesktopClipboardReadResult from(
            DesktopCommandResult commandResult) {
        Objects.requireNonNull(commandResult, "commandResult");
        Map<String, Object> payload = commandResult.payload();

        if (Boolean.TRUE.equals(payload.get("operationInvalidated"))) {
            return result(Outcome.INVALIDATED, Optional.empty(),
                    commandResult);
        }
        if ("denied".equals(payload.get("permissionDecision"))) {
            return result(Outcome.DENIED, Optional.empty(), commandResult);
        }
        if (commandResult.success()) {
            Object value = payload.get("text");
            if (value instanceof String typedText
                    && Boolean.TRUE.equals(payload.get("textPresent"))) {
                return result(Outcome.TEXT, Optional.of(typedText),
                        commandResult);
            }
            if (Boolean.FALSE.equals(payload.get("textPresent"))
                    && !payload.containsKey("text")) {
                return result(Outcome.NO_TEXT, Optional.empty(),
                        commandResult);
            }
        }
        return result(Outcome.ERROR, Optional.empty(), commandResult);
    }

    public Outcome outcome() {
        return outcome;
    }

    public Optional<String> text() {
        return text;
    }

    public DesktopCommandResult commandResult() {
        return commandResult;
    }

    private static DesktopClipboardReadResult result(Outcome outcome,
            Optional<String> text, DesktopCommandResult commandResult) {
        return new DesktopClipboardReadResult(outcome, text, commandResult);
    }
}
