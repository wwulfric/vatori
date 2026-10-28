package dev.vatori.desktop;

import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Normalized terminal result of selecting a create-new-only native file. */
public final class DesktopFileWriteGrantResult {
    public enum Outcome {
        INVALIDATED, DENIED, CANCELED, SELECTED, TARGET_EXISTS, ERROR
    }

    private final Outcome outcome;
    private final Optional<DesktopFileWriteGrant> grant;
    private final DesktopCommandResult commandResult;

    private DesktopFileWriteGrantResult(Outcome outcome,
            Optional<DesktopFileWriteGrant> grant,
            DesktopCommandResult commandResult) {
        this.outcome = outcome;
        this.grant = grant;
        this.commandResult = commandResult;
    }

    static DesktopFileWriteGrantResult from(DesktopCommandResult result,
            NativeBridge bridge) {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(bridge, "bridge");
        Map<String, Object> payload = result.payload();
        DesktopCommandResult sanitized =
                DesktopFileReadGrantResult.withoutGrantId(result);
        Outcome terminal;
        if (Boolean.TRUE.equals(payload.get("operationInvalidated"))) {
            terminal = Outcome.INVALIDATED;
        } else if ("denied".equals(payload.get("permissionDecision"))) {
            terminal = Outcome.DENIED;
        } else if (!result.success()
                && "target-exists".equals(payload.get("errorCode"))) {
            terminal = Outcome.TARGET_EXISTS;
        } else if (result.success()
                && Boolean.TRUE.equals(payload.get("canceled"))
                && !payload.containsKey("grantId")) {
            terminal = Outcome.CANCELED;
        } else if (result.success()
                && Boolean.FALSE.equals(payload.get("canceled"))
                && Boolean.TRUE.equals(payload.get("createNewOnly"))
                && Boolean.FALSE.equals(payload.get("pathExposed"))
                && Boolean.TRUE.equals(payload.get("usedSavePanel"))
                && !payload.containsKey("path") && !payload.containsKey("paths")
                && !payload.containsKey("url") && !payload.containsKey("errorCode")) {
            String id = canonicalUuid(payload.get("grantId"));
            Object filename = payload.get("filename");
            Long limit = exactLong(payload.get("maxWriteBytes"));
            if (id != null && filename instanceof String name && validFilename(name)
                    && limit != null && limit > 0
                    && limit <= DesktopFileWriteGrant.MAX_WRITE_BYTES
                    && payload.get("securityScoped") instanceof Boolean scoped) {
                return new DesktopFileWriteGrantResult(Outcome.SELECTED,
                        Optional.of(new DesktopFileWriteGrant(bridge, id, name,
                                limit.intValue(), scoped)), sanitized);
            }
            terminal = Outcome.ERROR;
        } else {
            terminal = Outcome.ERROR;
        }
        return new DesktopFileWriteGrantResult(terminal, Optional.empty(), sanitized);
    }

    public Outcome outcome() {
        return outcome;
    }

    public Optional<DesktopFileWriteGrant> grant() {
        return grant;
    }

    public DesktopCommandResult commandResult() {
        return commandResult;
    }

    private static boolean validFilename(String value) {
        return !value.isBlank() && !value.equals(".") && !value.equals("..")
                && value.indexOf('/') < 0 && value.indexOf('\0') < 0;
    }

    private static String canonicalUuid(Object value) {
        if (!(value instanceof String text)) {
            return null;
        }
        try {
            return UUID.fromString(text).toString().equalsIgnoreCase(text)
                    ? text : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    static Long exactLong(Object value) {
        if (value instanceof Byte || value instanceof Short
                || value instanceof Integer || value instanceof Long) {
            return ((Number) value).longValue();
        }
        if (value instanceof BigInteger integer && integer.bitLength() < Long.SIZE) {
            return integer.longValue();
        }
        return null;
    }
}
