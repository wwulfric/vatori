package dev.vatori.desktop;

import java.util.Map;
import java.util.Objects;

/**
 * A normalized terminal outcome from
 * {@link DesktopClipboard#writeTextResult(String)}.
 * The original command result remains available for platform-specific
 * diagnostics.
 */
public final class DesktopClipboardWriteResult {
    public enum Outcome {
        INVALIDATED,
        DENIED,
        WRITTEN,
        ERROR
    }

    private final Outcome outcome;
    private final DesktopCommandResult commandResult;

    private DesktopClipboardWriteResult(Outcome outcome,
            DesktopCommandResult commandResult) {
        this.outcome = Objects.requireNonNull(outcome, "outcome");
        this.commandResult = Objects.requireNonNull(commandResult,
                "commandResult");
    }

    static DesktopClipboardWriteResult from(
            DesktopCommandResult commandResult) {
        Objects.requireNonNull(commandResult, "commandResult");
        Map<String, Object> payload = commandResult.payload();

        if (Boolean.TRUE.equals(payload.get("operationInvalidated"))) {
            return new DesktopClipboardWriteResult(Outcome.INVALIDATED,
                    commandResult);
        }
        if ("denied".equals(payload.get("permissionDecision"))) {
            return new DesktopClipboardWriteResult(Outcome.DENIED,
                    commandResult);
        }
        if (commandResult.success()
                && Boolean.TRUE.equals(payload.get("written"))) {
            return new DesktopClipboardWriteResult(Outcome.WRITTEN,
                    commandResult);
        }
        return new DesktopClipboardWriteResult(Outcome.ERROR, commandResult);
    }

    public Outcome outcome() {
        return outcome;
    }

    public DesktopCommandResult commandResult() {
        return commandResult;
    }
}
