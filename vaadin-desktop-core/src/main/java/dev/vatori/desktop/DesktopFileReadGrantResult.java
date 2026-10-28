package dev.vatori.desktop;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A normalized terminal outcome from
 * {@link DesktopFileDialog#openReadGrant(String)}.
 */
public final class DesktopFileReadGrantResult {
    public enum Outcome {
        INVALIDATED,
        DENIED,
        CANCELED,
        SELECTED,
        ERROR
    }

    private final Outcome outcome;
    private final Optional<DesktopFileReadGrant> grant;
    private final DesktopCommandResult commandResult;

    private DesktopFileReadGrantResult(Outcome outcome,
            Optional<DesktopFileReadGrant> grant,
            DesktopCommandResult commandResult) {
        this.outcome = Objects.requireNonNull(outcome, "outcome");
        this.grant = Objects.requireNonNull(grant, "grant");
        this.commandResult = Objects.requireNonNull(commandResult,
                "commandResult");
    }

    static DesktopFileReadGrantResult from(
            DesktopCommandResult commandResult, NativeBridge nativeBridge) {
        Objects.requireNonNull(commandResult, "commandResult");
        Objects.requireNonNull(nativeBridge, "nativeBridge");
        Map<String, Object> payload = commandResult.payload();
        DesktopCommandResult publicCommandResult = withoutGrantId(commandResult);

        if (Boolean.TRUE.equals(payload.get("operationInvalidated"))) {
            return result(Outcome.INVALIDATED, Optional.empty(),
                    publicCommandResult);
        }
        if ("denied".equals(payload.get("permissionDecision"))) {
            return result(Outcome.DENIED, Optional.empty(), publicCommandResult);
        }
        if (commandResult.success()
                && Boolean.TRUE.equals(payload.get("canceled"))) {
            return result(Outcome.CANCELED, Optional.empty(),
                    publicCommandResult);
        }
        if (commandResult.success()
                && Boolean.FALSE.equals(payload.get("canceled"))) {
            String grantId = canonicalUuid(payload.get("grantId"));
            String filename = nonBlankString(payload.get("filename"));
            Long byteCount = nonNegativeLong(payload.get("byteCount"));
            Integer maxReadBytes = positiveInteger(payload.get("maxReadBytes"));
            Object securityScoped = payload.get("securityScoped");
            if (grantId != null && filename != null && byteCount != null
                    && maxReadBytes != null
                    && securityScoped instanceof Boolean scoped) {
                DesktopFileReadGrant grant = new DesktopFileReadGrant(
                        nativeBridge, grantId, filename, byteCount,
                        maxReadBytes, scoped);
                return result(Outcome.SELECTED, Optional.of(grant),
                        publicCommandResult);
            }
        }
        return result(Outcome.ERROR, Optional.empty(), publicCommandResult);
    }

    public Outcome outcome() {
        return outcome;
    }

    public Optional<DesktopFileReadGrant> grant() {
        return grant;
    }

    public DesktopCommandResult commandResult() {
        return commandResult;
    }

    private static DesktopFileReadGrantResult result(Outcome outcome,
            Optional<DesktopFileReadGrant> grant,
            DesktopCommandResult commandResult) {
        return new DesktopFileReadGrantResult(outcome, grant, commandResult);
    }

    static DesktopCommandResult withoutGrantId(
            DesktopCommandResult commandResult) {
        if (!commandResult.payload().containsKey("grantId")) {
            return commandResult;
        }
        Map<String, Object> payload = new LinkedHashMap<>(
                commandResult.payload());
        payload.remove("grantId");
        return new DesktopCommandResult(commandResult.success(), payload,
                commandResult.error());
    }

    private static String canonicalUuid(Object value) {
        if (!(value instanceof String text) || text.isBlank()) {
            return null;
        }
        try {
            UUID parsed = UUID.fromString(text);
            return parsed.toString().equalsIgnoreCase(text) ? text : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String nonBlankString(Object value) {
        return value instanceof String text && !text.isBlank() ? text : null;
    }

    private static Long nonNegativeLong(Object value) {
        Long exact = exactLong(value);
        return exact != null && exact >= 0 ? exact : null;
    }

    private static Integer positiveInteger(Object value) {
        Long exact = exactLong(value);
        return exact != null && exact > 0 && exact <= Integer.MAX_VALUE
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
