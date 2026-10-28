package dev.vatori.desktop;

import java.util.Map;
import java.util.Objects;

/**
 * Result of one bounded native file write. On cooperative cancellation or a
 * reported I/O error, valid exact prefix counts are retained even though the
 * grant is no longer usable. {@link Outcome#CANCELED} has a shorter-than-requested
 * prefix; a fully completed block remains {@link Outcome#WRITTEN}.
 * Unavailable or malformed count information uses offset/nextOffset -1 and
 * bytesWritten 0; it is not evidence that an unsuccessful operation made no
 * filesystem changes. An already accepted write may complete successfully while
 * concurrent release or cancellation makes the grant unavailable; its known
 * written count is independent of availability for another operation.
 */
public final class DesktopFileWriteResult {
    public enum Outcome {
        INVALIDATED, DENIED, UNAVAILABLE, INVALID_RANGE, INVALID_DATA, BUSY,
        WRITTEN, CANCELED, ERROR
    }

    private final Outcome outcome;
    private final long offset;
    private final long nextOffset;
    private final int bytesWritten;
    private final DesktopCommandResult commandResult;

    private DesktopFileWriteResult(Outcome outcome, long offset, long nextOffset,
            int bytesWritten, DesktopCommandResult commandResult) {
        this.outcome = outcome;
        this.offset = offset;
        this.nextOffset = nextOffset;
        this.bytesWritten = bytesWritten;
        this.commandResult = commandResult;
    }

    static DesktopFileWriteResult from(DesktopCommandResult result,
            long requestedOffset, int requestedBytes, int maxWriteBytes) {
        Objects.requireNonNull(result, "result");
        Map<String, Object> payload = result.payload();
        DesktopCommandResult sanitized =
                DesktopFileReadGrantResult.withoutGrantId(result);
        if (Boolean.TRUE.equals(payload.get("operationInvalidated"))) {
            return terminal(Outcome.INVALIDATED, sanitized);
        }
        if ("denied".equals(payload.get("permissionDecision"))) {
            return terminal(Outcome.DENIED, sanitized);
        }
        Object errorCode = payload.get("errorCode");
        boolean canceled = !result.success()
                && "write-canceled".equals(errorCode);
        if (!result.success()) {
            if (Boolean.TRUE.equals(payload.get("grantUnavailable"))
                    && "grant-unavailable".equals(errorCode)) {
                return terminal(Outcome.UNAVAILABLE, sanitized);
            }
            if ("invalid-write-range".equals(errorCode)) {
                return terminal(Outcome.INVALID_RANGE, sanitized);
            }
            if ("invalid-write-data".equals(errorCode)) {
                return terminal(Outcome.INVALID_DATA, sanitized);
            }
            if ("grant-busy".equals(errorCode)) {
                return terminal(Outcome.BUSY, sanitized);
            }
            if (!"write-failed".equals(errorCode) && !canceled) {
                return terminal(Outcome.ERROR, sanitized);
            }
        } else if (errorCode != null) {
            return terminal(Outcome.ERROR, sanitized);
        }

        if (canceled) {
            if (!"fileGrant.write".equals(payload.get("command"))
                    || !"write-grant".equals(payload.get("operation"))
                    || !Boolean.FALSE.equals(payload.get("success"))
                    || !Boolean.TRUE.equals(payload.get("canceled"))
                    || !Boolean.TRUE.equals(payload.get("grantUnavailable"))
                    || !"unavailable".equals(payload.get("grantState"))
                    || !Boolean.FALSE.equals(payload.get("pathExposed"))
                    || payload.containsKey("path") || payload.containsKey("paths")
                    || payload.containsKey("url") || payload.containsKey("bodyBase64")) {
                return terminal(Outcome.ERROR, sanitized);
            }
        } else if (payload.containsKey("canceled")
                && !Boolean.FALSE.equals(payload.get("canceled"))) {
            return terminal(Outcome.ERROR, sanitized);
        }

        Long offset = DesktopFileWriteGrantResult.exactLong(payload.get("offset"));
        Long next = DesktopFileWriteGrantResult.exactLong(payload.get("nextOffset"));
        Long count = DesktopFileWriteGrantResult.exactLong(payload.get("bytesWritten"));
        if (requestedOffset < 0 || requestedBytes <= 0
                || requestedBytes > maxWriteBytes
                || requestedBytes > DesktopFileWriteGrant.MAX_WRITE_BYTES
                || offset == null || offset != requestedOffset
                || next == null || count == null || count < 0
                || count > requestedBytes || offset > Long.MAX_VALUE - count
                || next != offset + count
                || (result.success() && count != requestedBytes)
                || (canceled && count >= requestedBytes)) {
            return terminal(Outcome.ERROR, sanitized);
        }
        return new DesktopFileWriteResult(result.success()
                ? Outcome.WRITTEN : canceled ? Outcome.CANCELED : Outcome.ERROR,
                offset, next, count.intValue(),
                sanitized);
    }

    public Outcome outcome() {
        return outcome;
    }

    public long offset() {
        return offset;
    }

    public long nextOffset() {
        return nextOffset;
    }

    public int bytesWritten() {
        return bytesWritten;
    }

    public DesktopCommandResult commandResult() {
        return commandResult;
    }

    private static DesktopFileWriteResult terminal(Outcome outcome,
            DesktopCommandResult result) {
        return new DesktopFileWriteResult(outcome, -1, -1, 0, result);
    }
}
