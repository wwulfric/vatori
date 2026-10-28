package dev.vatori.desktop;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A normalized terminal outcome from
 * {@link DesktopNotification#showResult(String, String)}.
 * The original command result remains available for platform-specific
 * diagnostics. Unknown or invalidated results do not prove that no
 * operating-system side effect occurred and must not imply a safe retry.
 */
public final class DesktopNotificationResult {
    public enum Outcome {
        /** The invocation's owner or sidecar generation is no longer valid. */
        INVALIDATED,
        /** The application's capability manifest denied the command. */
        DENIED,
        /** An active operating-system authorization prompt is required. */
        AUTHORIZATION_REQUIRED,
        /** Operating-system notification authorization was denied. */
        OS_DENIED,
        /**
         * The operating system accepted the notification request. This is not
         * confirmation that a banner was displayed, delivered or seen.
         */
        SCHEDULED,
        /** A transport, native operation or result-contract failure. */
        ERROR
    }

    private final Outcome outcome;
    private final Optional<String> notificationId;
    private final DesktopCommandResult commandResult;

    private DesktopNotificationResult(Outcome outcome,
            Optional<String> notificationId,
            DesktopCommandResult commandResult) {
        this.outcome = Objects.requireNonNull(outcome, "outcome");
        this.notificationId = Objects.requireNonNull(notificationId,
                "notificationId");
        this.commandResult = Objects.requireNonNull(commandResult,
                "commandResult");
    }

    static DesktopNotificationResult from(DesktopCommandResult commandResult) {
        Objects.requireNonNull(commandResult, "commandResult");
        Map<String, Object> payload = commandResult.payload();

        if (Boolean.TRUE.equals(payload.get("operationInvalidated"))) {
            return withoutIdentifier(Outcome.INVALIDATED, commandResult);
        }
        if ("denied".equals(payload.get("permissionDecision"))) {
            return withoutIdentifier(Outcome.DENIED, commandResult);
        }
        // Native API diagnostics describe errors, never authorization decisions
        // or accepted requests. Preserve them only in the original result.
        if (payload.containsKey("nativeErrorDomain")
                || payload.containsKey("nativeErrorCode")) {
            return withoutIdentifier(Outcome.ERROR, commandResult);
        }
        if (!"notification.show".equals(payload.get("command"))
                || !"native-user-notifications".equals(payload.get("mode"))
                || !Boolean.valueOf(commandResult.success())
                        .equals(payload.get("success"))
                || !absentOrFalse(payload, "operationInvalidated")
                || !absentOrFalse(payload, "canceled")
                || payload.containsKey("cancelReason")) {
            return withoutIdentifier(Outcome.ERROR, commandResult);
        }

        if (commandResult.success()) {
            if (Boolean.TRUE.equals(payload.get("scheduled"))
                    && payload.get("notificationId") instanceof String id
                    && !id.isBlank()
                    && commandResult.error() == null
                    && !payload.containsKey("error")
                    && !payload.containsKey("errorCode")) {
                return new DesktopNotificationResult(Outcome.SCHEDULED,
                        Optional.of(id), commandResult);
            }
        } else if (Boolean.FALSE.equals(payload.get("scheduled"))
                && !payload.containsKey("notificationId")) {
            if ("notification-authorization-required"
                    .equals(payload.get("errorCode"))) {
                return withoutIdentifier(Outcome.AUTHORIZATION_REQUIRED,
                        commandResult);
            }
            if ("notification-authorization-denied"
                    .equals(payload.get("errorCode"))) {
                return withoutIdentifier(Outcome.OS_DENIED, commandResult);
            }
        }
        return withoutIdentifier(Outcome.ERROR, commandResult);
    }

    public Outcome outcome() {
        return outcome;
    }

    /**
     * Returns the identifier accepted by the operating system, only for
     * {@link Outcome#SCHEDULED}. It is not a delivery receipt or a persistent
     * capability for cancellation or resumption.
     */
    public Optional<String> notificationId() {
        return notificationId;
    }

    public DesktopCommandResult commandResult() {
        return commandResult;
    }

    private static boolean absentOrFalse(Map<String, Object> payload,
            String name) {
        return !payload.containsKey(name)
                || Boolean.FALSE.equals(payload.get(name));
    }

    private static DesktopNotificationResult withoutIdentifier(Outcome outcome,
            DesktopCommandResult commandResult) {
        return new DesktopNotificationResult(outcome, Optional.empty(),
                commandResult);
    }
}
