package dev.vatori.desktop;

import java.util.Map;
import java.util.Objects;

/** Normalized result of releasing a native file grant. */
public final class DesktopFileGrantReleaseResult {
    public enum Outcome {
        INVALIDATED,
        DENIED,
        UNAVAILABLE,
        RELEASED,
        ALREADY_RELEASED,
        ERROR
    }

    private final Outcome outcome;
    private final DesktopCommandResult commandResult;

    private DesktopFileGrantReleaseResult(Outcome outcome,
            DesktopCommandResult commandResult) {
        this.outcome = Objects.requireNonNull(outcome, "outcome");
        this.commandResult = Objects.requireNonNull(commandResult,
                "commandResult");
    }

    static DesktopFileGrantReleaseResult from(
            DesktopCommandResult commandResult) {
        Objects.requireNonNull(commandResult, "commandResult");
        Map<String, Object> payload = commandResult.payload();
        DesktopCommandResult publicCommandResult =
                DesktopFileReadGrantResult.withoutGrantId(commandResult);

        if (Boolean.TRUE.equals(payload.get("operationInvalidated"))) {
            return result(Outcome.INVALIDATED, publicCommandResult);
        }
        if ("denied".equals(payload.get("permissionDecision"))) {
            return result(Outcome.DENIED, publicCommandResult);
        }
        if (Boolean.TRUE.equals(payload.get("grantUnavailable"))
                && "grant-unavailable".equals(payload.get("errorCode"))) {
            return result(Outcome.UNAVAILABLE, publicCommandResult);
        }
        if (commandResult.success()
                && Boolean.TRUE.equals(payload.get("released"))) {
            return result(Outcome.RELEASED, publicCommandResult);
        }
        return result(Outcome.ERROR, publicCommandResult);
    }

    static DesktopFileGrantReleaseResult alreadyReleased(
            DesktopCommandResult successfulReleaseResult) {
        return result(Outcome.ALREADY_RELEASED,
                Objects.requireNonNull(successfulReleaseResult,
                        "successfulReleaseResult"));
    }

    public Outcome outcome() {
        return outcome;
    }

    public DesktopCommandResult commandResult() {
        return commandResult;
    }

    private static DesktopFileGrantReleaseResult result(Outcome outcome,
            DesktopCommandResult commandResult) {
        return new DesktopFileGrantReleaseResult(outcome, commandResult);
    }
}
