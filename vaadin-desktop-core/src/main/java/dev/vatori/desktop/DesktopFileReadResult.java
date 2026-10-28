package dev.vatori.desktop;

import java.math.BigInteger;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

/** Normalized result of one bounded read from a native file grant. */
public final class DesktopFileReadResult {
    public enum Outcome {
        INVALIDATED,
        DENIED,
        UNAVAILABLE,
        INVALID_RANGE,
        DATA,
        END_OF_FILE,
        ERROR
    }

    private final Outcome outcome;
    private final byte[] bytes;
    private final long offset;
    private final long nextOffset;
    private final boolean endOfFile;
    private final DesktopCommandResult commandResult;

    private DesktopFileReadResult(Outcome outcome, byte[] bytes,
            long offset, long nextOffset, boolean endOfFile,
            DesktopCommandResult commandResult) {
        this.outcome = Objects.requireNonNull(outcome, "outcome");
        this.bytes = Objects.requireNonNull(bytes, "bytes").clone();
        this.offset = offset;
        this.nextOffset = nextOffset;
        this.endOfFile = endOfFile;
        this.commandResult = Objects.requireNonNull(commandResult,
                "commandResult");
    }

    static DesktopFileReadResult from(DesktopCommandResult commandResult,
            long requestedOffset, int requestedMaxBytes,
            int grantMaxReadBytes) {
        Objects.requireNonNull(commandResult, "commandResult");
        Map<String, Object> payload = commandResult.payload();
        DesktopCommandResult publicCommandResult =
                DesktopFileReadGrantResult.withoutGrantId(commandResult);

        if (Boolean.TRUE.equals(payload.get("operationInvalidated"))) {
            return terminal(Outcome.INVALIDATED, publicCommandResult);
        }
        if ("denied".equals(payload.get("permissionDecision"))) {
            return terminal(Outcome.DENIED, publicCommandResult);
        }
        if (Boolean.TRUE.equals(payload.get("grantUnavailable"))
                && "grant-unavailable".equals(payload.get("errorCode"))) {
            return terminal(Outcome.UNAVAILABLE, publicCommandResult);
        }
        if ("invalid-read-range".equals(payload.get("errorCode"))) {
            return terminal(Outcome.INVALID_RANGE, publicCommandResult);
        }
        if (!commandResult.success()) {
            return terminal(Outcome.ERROR, publicCommandResult);
        }

        Long offset = nonNegativeLong(payload.get("offset"));
        Long nextOffset = nonNegativeLong(payload.get("nextOffset"));
        Integer bytesRead = nonNegativeInteger(payload.get("bytesRead"));
        Object endOfFileValue = payload.get("endOfFile");
        Object encodedValue = payload.get("bodyBase64");
        if (offset == null || offset != requestedOffset || nextOffset == null
                || bytesRead == null || !(endOfFileValue instanceof Boolean eof)
                || !(encodedValue instanceof String encoded)
                || requestedMaxBytes < 0
                || requestedMaxBytes > grantMaxReadBytes
                || bytesRead > requestedMaxBytes) {
            return terminal(Outcome.ERROR, publicCommandResult);
        }

        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException ignored) {
            return terminal(Outcome.ERROR, publicCommandResult);
        }
        if (decoded.length != bytesRead
                || offset > Long.MAX_VALUE - bytesRead
                || nextOffset != offset + bytesRead) {
            return terminal(Outcome.ERROR, publicCommandResult);
        }
        if (bytesRead == 0) {
            return eof
                    ? new DesktopFileReadResult(Outcome.END_OF_FILE,
                            decoded, offset, nextOffset, true,
                            publicCommandResult)
                    : terminal(Outcome.ERROR, publicCommandResult);
        }
        return new DesktopFileReadResult(Outcome.DATA, decoded, offset,
                nextOffset, eof, publicCommandResult);
    }

    public Outcome outcome() {
        return outcome;
    }

    public byte[] bytes() {
        return bytes.clone();
    }

    public long offset() {
        return offset;
    }

    public long nextOffset() {
        return nextOffset;
    }

    public boolean endOfFile() {
        return endOfFile;
    }

    public DesktopCommandResult commandResult() {
        return commandResult;
    }

    private static DesktopFileReadResult terminal(Outcome outcome,
            DesktopCommandResult commandResult) {
        return new DesktopFileReadResult(outcome, new byte[0], -1, -1,
                false, commandResult);
    }

    private static Long nonNegativeLong(Object value) {
        Long exact = exactLong(value);
        return exact != null && exact >= 0 ? exact : null;
    }

    private static Integer nonNegativeInteger(Object value) {
        Long exact = exactLong(value);
        return exact != null && exact >= 0 && exact <= Integer.MAX_VALUE
                ? exact.intValue()
                : null;
    }

    private static Long exactLong(Object value) {
        if (value instanceof Byte || value instanceof Short
                || value instanceof Integer || value instanceof Long) {
            return ((Number) value).longValue();
        }
        if (value instanceof BigInteger integer
                && integer.bitLength() < Long.SIZE) {
            return integer.longValue();
        }
        return null;
    }
}
