package dev.vatori.desktop;

import java.util.Map;
import java.util.Objects;

/**
 * Terminal result of requesting cancellation of a native write grant.
 * A confirmed cancellation closes the grant but preserves the file and all
 * bytes already written. No result promises fsync durability or rollback.
 * An unknown prefix is reported as {@code -1}, never as an assumed zero.
 */
public final class DesktopFileWriteCancelResult {
    public enum Outcome {
        INVALIDATED, DENIED, UNAVAILABLE, CANCELED, ALREADY_CANCELED, ERROR
    }

    private final Outcome outcome;
    private final long nextOffset;
    private final DesktopCommandResult commandResult;

    private DesktopFileWriteCancelResult(Outcome outcome, long nextOffset,
            DesktopCommandResult commandResult) {
        this.outcome = outcome;
        this.nextOffset = nextOffset;
        this.commandResult = commandResult;
    }

    static DesktopFileWriteCancelResult from(DesktopCommandResult result) {
        Objects.requireNonNull(result, "result");
        Map<String, Object> payload = result.payload();
        DesktopCommandResult sanitized =
                DesktopFileReadGrantResult.withoutGrantId(result);
        if (Boolean.TRUE.equals(payload.get("operationInvalidated"))) {
            return unknown(Outcome.INVALIDATED, sanitized);
        }
        if ("denied".equals(payload.get("permissionDecision"))) {
            return unknown(Outcome.DENIED, sanitized);
        }
        if (!result.success()
                && Boolean.TRUE.equals(payload.get("grantUnavailable"))
                && "grant-unavailable".equals(payload.get("errorCode"))) {
            return unknown(Outcome.UNAVAILABLE, sanitized);
        }
        Long next = DesktopFileWriteGrantResult.exactLong(payload.get("nextOffset"));
        if (!"fileGrant.cancelWrite".equals(payload.get("command"))
                || !"cancel-write-grant".equals(payload.get("operation"))
                || !Boolean.valueOf(result.success()).equals(payload.get("success"))
                || !Boolean.TRUE.equals(payload.get("cancellationRequested"))
                || !Boolean.TRUE.equals(payload.get("grantUnavailable"))
                || !"unavailable".equals(payload.get("grantState"))
                || !Boolean.FALSE.equals(payload.get("pathExposed"))
                || !(payload.get("securityScopeStopped") instanceof Boolean)
                || payload.containsKey("path") || payload.containsKey("paths")
                || payload.containsKey("url") || payload.containsKey("bodyBase64")
                || payload.containsKey("released") || payload.containsKey("offset")
                || payload.containsKey("bytesWritten")
                || next == null || next < 0) {
            return unknown(Outcome.ERROR, sanitized);
        }
        if (result.success() && Boolean.TRUE.equals(payload.get("canceled"))
                && !payload.containsKey("errorCode") && !payload.containsKey("error")) {
            return new DesktopFileWriteCancelResult(Outcome.CANCELED, next,
                    sanitized);
        }
        if (!result.success() && Boolean.FALSE.equals(payload.get("canceled"))
                && "grant-close-failed".equals(payload.get("errorCode"))) {
            return new DesktopFileWriteCancelResult(Outcome.ERROR, next,
                    sanitized);
        }
        return unknown(Outcome.ERROR, sanitized);
    }

    static DesktopFileWriteCancelResult alreadyCanceled(
            DesktopFileWriteCancelResult confirmed) {
        Objects.requireNonNull(confirmed, "confirmed");
        return new DesktopFileWriteCancelResult(Outcome.ALREADY_CANCELED,
                confirmed.nextOffset, confirmed.commandResult);
    }

    public Outcome outcome() {
        return outcome;
    }

    /**
     * Returns the host-confirmed total written prefix, or {@code -1} when
     * unknown. A close failure may still carry a reliable prefix; check the
     * outcome independently before assuming native cleanup succeeded.
     */
    public long nextOffset() {
        return nextOffset;
    }

    public DesktopCommandResult commandResult() {
        return commandResult;
    }

    private static DesktopFileWriteCancelResult unknown(Outcome outcome,
            DesktopCommandResult result) {
        return new DesktopFileWriteCancelResult(outcome, -1, result);
    }
}
