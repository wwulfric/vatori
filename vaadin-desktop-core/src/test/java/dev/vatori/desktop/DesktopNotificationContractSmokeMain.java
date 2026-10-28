package dev.vatori.desktop;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

public final class DesktopNotificationContractSmokeMain {
    private DesktopNotificationContractSmokeMain() {
    }

    public static void main(String[] args) throws Exception {
        classifiesScheduledAndRetainsTheExactIdentifier();
        classifiesRequiredAndDeniedAuthorization();
        classifiesInvalidationBeforeAllOtherFields();
        classifiesManifestDenialBeforeNativeOutcomes();
        rejectsMissingAndMalformedCommonFields();
        rejectsIncompleteAndContradictoryScheduledResults();
        rejectsIncompleteAndContradictoryAuthorizationResults();
        rejectsNativeErrorDiagnosticsInNonErrorNativeOutcomes();
        retainsNativeErrorDiagnosticsWithoutInferringAuthorization();
        doesNotInferOutcomesFromErrorText();
        preservesLegacyResultsWithExactlyOneInvocation();
        reusesTheCapturedNotificationFacadeOnBackgroundWork();
        rejectsNullArgumentsBeforeInvokingTheBridge();
        System.out.println("Desktop notification result contract smoke passed");
    }

    private static void classifiesScheduledAndRetainsTheExactIdentifier() {
        Map<String, Object> payload = scheduledPayload();
        payload.put("notificationId", "native-request-123");
        payload.put("title", "Title");
        payload.put("body", "Body");
        payload.put("permissionDecision", "allowed");
        payload.put("operationInvalidated", false);
        payload.put("canceled", false);
        check(DesktopCommandResult.ok(payload),
                DesktopNotificationResult.Outcome.SCHEDULED,
                Optional.of("native-request-123"));
    }

    private static void classifiesRequiredAndDeniedAuthorization() {
        check(authorizationFailure("notification-authorization-required"),
                DesktopNotificationResult.Outcome.AUTHORIZATION_REQUIRED,
                Optional.empty());
        check(authorizationFailure("notification-authorization-denied"),
                DesktopNotificationResult.Outcome.OS_DENIED, Optional.empty());

        // The stable error code is sufficient; localized diagnostic text is
        // optional and must not control classification.
        check(new DesktopCommandResult(false,
                        authorizationPayload("notification-authorization-denied"),
                        null),
                DesktopNotificationResult.Outcome.OS_DENIED, Optional.empty());
    }

    private static void classifiesInvalidationBeforeAllOtherFields() {
        Map<String, Object> payload = scheduledPayload();
        payload.put("operationInvalidated", true);
        payload.put("permissionDecision", "denied");
        payload.put("canceled", true);
        payload.put("cancelReason", "owner-window-closed");
        payload.put("errorCode", "notification-authorization-denied");
        payload.put("nativeErrorDomain", "UNErrorDomain");
        payload.put("nativeErrorCode", 1L);
        for (boolean success : List.of(false, true)) {
            check(new DesktopCommandResult(success, payload, "invalidated"),
                    DesktopNotificationResult.Outcome.INVALIDATED,
                    Optional.empty());
        }
        check(DesktopCommandResult.error(
                        Map.of("operationInvalidated", true), "invalidated"),
                DesktopNotificationResult.Outcome.INVALIDATED, Optional.empty());
    }

    private static void classifiesManifestDenialBeforeNativeOutcomes() {
        Map<String, Object> payload = scheduledPayload();
        payload.put("permissionDecision", "denied");
        payload.put("canceled", true);
        payload.put("cancelReason", "contradictory native fields");
        payload.put("errorCode", "notification-authorization-required");
        payload.put("nativeErrorDomain", "other");
        payload.put("nativeErrorCode", -1L);
        for (boolean success : List.of(false, true)) {
            check(new DesktopCommandResult(success, payload, "denied"),
                    DesktopNotificationResult.Outcome.DENIED, Optional.empty());
        }
        check(DesktopCommandResult.error(
                        Map.of("permissionDecision", "denied"), "denied"),
                DesktopNotificationResult.Outcome.DENIED, Optional.empty());
    }

    private static void rejectsMissingAndMalformedCommonFields() {
        List<Map<String, Object>> bases = List.of(scheduledPayload(),
                authorizationPayload("notification-authorization-required"),
                authorizationPayload("notification-authorization-denied"));
        for (Map<String, Object> base : bases) {
            boolean success = Boolean.TRUE.equals(base.get("success"));
            for (String key : List.of("command", "mode", "success", "scheduled")) {
                malformed(success, without(base, key), null);
                for (Object value : new Object[]{null, "", 0, "false", List.of()}) {
                    malformed(success, replacing(base, key, value), null);
                }
            }
            malformed(success, replacing(base, "command", "clipboard.readText"), null);
            malformed(success, replacing(base, "mode", "web-notifications"), null);
            malformed(success, replacing(base, "success", !success), null);
            malformed(success, replacing(base, "canceled", true), null);
            for (Object value : new Object[]{null, "false", "true", 0}) {
                malformed(success, replacing(base, "canceled", value), null);
                malformed(success,
                        replacing(base, "operationInvalidated", value), null);
            }
            for (Object value : new Object[]{null, "", "owner-window-closed"}) {
                malformed(success, replacing(base, "cancelReason", value), null);
            }
        }
        malformed(false, Map.of(), "transport failure");
        malformed(true, Map.of(), null);
    }

    private static void rejectsIncompleteAndContradictoryScheduledResults() {
        Map<String, Object> base = scheduledPayload();
        malformed(true, without(base, "notificationId"), null);
        for (Object value : new Object[]{null, "", " \t\n", 42, true, List.of("id")}) {
            malformed(true, replacing(base, "notificationId", value), null);
        }
        malformed(true, replacing(base, "scheduled", false), null);
        malformed(false, base, "failed command with success payload");
        for (String key : List.of("error", "errorCode")) {
            for (Object value : new Object[]{null, "", "failure",
                    "notification-authorization-denied",
                    "notification-authorization-required"}) {
                malformed(true, replacing(base, key, value), null);
            }
        }
        malformed(true, base, "native scheduling error");
        malformed(true, base, "");
    }

    private static void rejectsIncompleteAndContradictoryAuthorizationResults() {
        for (String code : List.of("notification-authorization-required",
                "notification-authorization-denied")) {
            Map<String, Object> base = authorizationPayload(code);
            malformed(false, without(base, "errorCode"), "authorization denied");
            for (Object value : new Object[]{null, "", true, 42,
                    "notification-authorization-failed",
                    "notification-scheduling-failed", "unknown-error"}) {
                malformed(false, replacing(base, "errorCode", value),
                        "Notification permission was denied");
            }
            for (Object value : new Object[]{null, "", "accepted-id", 42}) {
                malformed(false, replacing(base, "notificationId", value), null);
            }
            malformed(false, replacing(base, "scheduled", true), null);
            malformed(true, base, "authorization failure");
            Map<String, Object> successful = replacing(base, "success", true);
            malformed(true, successful, null);
        }
    }

    private static void doesNotInferOutcomesFromErrorText() {
        for (String error : List.of("Notification permission was denied",
                "Notification permission requires an active authorization prompt",
                "Notification has been scheduled",
                "notification-authorization-denied")) {
            malformed(false, Map.of(), error);
            Map<String, Object> payload = authorizationPayload("unknown-error");
            payload.put("error", error);
            malformed(false, payload, error);
        }
        Map<String, Object> oldNativeSuccess = without(scheduledPayload(), "scheduled");
        malformed(true, oldNativeSuccess, null);
    }

    private static void rejectsNativeErrorDiagnosticsInNonErrorNativeOutcomes() {
        for (Map<String, Object> base : List.of(scheduledPayload(),
                authorizationPayload("notification-authorization-required"),
                authorizationPayload("notification-authorization-denied"))) {
            boolean success = Boolean.TRUE.equals(base.get("success"));
            for (String key : List.of("nativeErrorDomain", "nativeErrorCode")) {
                for (Object value : new Object[]{null, "", "UNErrorDomain",
                        "other", 0L, 1L, -1L, true, List.of(), Map.of()}) {
                    malformed(success, replacing(base, key, value), null);
                }
            }
            Map<String, Object> diagnostic = replacing(base,
                    "nativeErrorDomain", "UNErrorDomain");
            diagnostic.put("nativeErrorCode", 1L);
            malformed(success, diagnostic, null);
        }
    }

    private static void retainsNativeErrorDiagnosticsWithoutInferringAuthorization() {
        for (String errorCode : List.of("notification-authorization-failed",
                "notification-scheduling-failed", "unknown-error")) {
            for (String domain : List.of("UNErrorDomain", "other")) {
                for (long code : new long[]{Long.MIN_VALUE, -1L, 0L, 1L,
                        Long.MAX_VALUE}) {
                    DesktopCommandResult raw = nativeFailure(errorCode, domain, code);
                    check(raw, DesktopNotificationResult.Outcome.ERROR,
                            Optional.empty());
                    equal(domain, raw.payload().get("nativeErrorDomain"),
                            "retained native error domain");
                    equal(code, raw.payload().get("nativeErrorCode"),
                            "retained numeric native error code");
                }
            }
        }
        Map<String, Object> noStableCode = without(
                nativeFailure("notification-authorization-failed",
                        "UNErrorDomain", 1L).payload(), "errorCode");
        malformed(false, noStableCode, "Notification permission was denied");
    }

    private static void preservesLegacyResultsWithExactlyOneInvocation() {
        List<DesktopCommandResult> results = List.of(
                DesktopCommandResult.ok(scheduledPayload()),
                authorizationFailure("notification-authorization-required"),
                authorizationFailure("notification-authorization-denied"),
                DesktopCommandResult.error(
                        Map.of("operationInvalidated", true), "invalidated"),
                DesktopCommandResult.error(
                        Map.of("permissionDecision", "denied"), "denied"),
                nativeFailure("notification-authorization-failed", "UNErrorDomain", 1L),
                nativeFailure("notification-scheduling-failed", "other", -1L),
                DesktopCommandResult.error("arbitrary failure"),
                DesktopCommandResult.ok(Map.of("legacy-platform-field", 42)));
        for (DesktopCommandResult raw : results) {
            RecordingBridge bridge = new RecordingBridge(raw);
            DesktopNotification notifications = new DesktopNotification(bridge);
            same(raw, notifications.show("Title", "Body"), "legacy raw result");
            equal(1, bridge.commands.size(), "legacy single invocation");
            command(bridge.commands.getFirst(), "Title", "Body");
        }

        // Preserve the original API's argument forwarding; the host validates
        // blank titles, and an empty body remains a valid caller input.
        DesktopCommandResult raw = DesktopCommandResult.error("blank title");
        RecordingBridge bridge = new RecordingBridge(raw);
        same(raw, new DesktopNotification(bridge).show("", ""),
                "blank argument raw result");
        command(bridge.commands.getFirst(), "", "");
    }

    private static void reusesTheCapturedNotificationFacadeOnBackgroundWork()
            throws Exception {
        DesktopCommandResult scheduled = DesktopCommandResult.ok(scheduledPayload());
        DesktopCommandResult invalidated = DesktopCommandResult.error(Map.of(
                "operationInvalidated", true,
                "cancelReason", "owner-window-closed",
                "canceled", true), "owner closed");
        RecordingBridge bound = new RecordingBridge(scheduled, invalidated,
                invalidated);
        CaptureBridge unbound = new CaptureBridge(bound);
        DesktopContext context = new DesktopContext(
                DesktopApplication.builder("Notification contract").build(),
                URI.create("vatori-app://app/"), unbound, Map.of());
        DesktopCapabilityContext captured = context.captureCapabilityContext();
        DesktopNotification notifications = captured.notifications();
        same(notifications, captured.notifications(), "stable captured facade");
        same(bound, captured.nativeBridge(), "captured bridge identity");

        FutureTask<Void> work = new FutureTask<>(() -> {
            DesktopNotificationResult first = notifications.showResult("First", "");
            equal(DesktopNotificationResult.Outcome.SCHEDULED,
                    first.outcome(), "captured scheduled outcome");
            same(scheduled, first.commandResult(), "captured raw result");
            DesktopNotificationResult retired = notifications.showResult("Second", "");
            equal(DesktopNotificationResult.Outcome.INVALIDATED,
                    retired.outcome(), "captured invalidation outcome");
            equal(Optional.empty(), retired.notificationId(),
                    "captured invalidation identifier");
            same(invalidated, notifications.show("Third", ""),
                    "captured legacy raw result");
            return null;
        });
        Thread.startVirtualThread(work);
        work.get(10, TimeUnit.SECONDS);
        equal(1, unbound.captures, "capture happens only once");
        equal(3, bound.commands.size(), "captured invocation count");
        command(bound.commands.get(0), "First", "");
        command(bound.commands.get(1), "Second", "");
        command(bound.commands.get(2), "Third", "");
    }

    private static void rejectsNullArgumentsBeforeInvokingTheBridge() {
        RecordingBridge bridge = new RecordingBridge();
        DesktopNotification notifications = new DesktopNotification(bridge);
        nullPointer(() -> notifications.showResult(null, "Body"));
        nullPointer(() -> notifications.showResult("Title", null));
        nullPointer(() -> notifications.show(null, "Body"));
        nullPointer(() -> notifications.show("Title", null));
        nullPointer(() -> DesktopNotificationResult.from(null));
        equal(0, bridge.commands.size(), "null argument invocation count");
    }

    private static Map<String, Object> scheduledPayload() {
        return new LinkedHashMap<>(Map.of(
                "command", "notification.show",
                "mode", "native-user-notifications",
                "success", true,
                "scheduled", true,
                "notificationId", "native-request-id"));
    }

    private static Map<String, Object> authorizationPayload(String code) {
        return new LinkedHashMap<>(Map.of(
                "command", "notification.show",
                "mode", "native-user-notifications",
                "success", false,
                "scheduled", false,
                "errorCode", code));
    }

    private static DesktopCommandResult authorizationFailure(String code) {
        Map<String, Object> payload = authorizationPayload(code);
        payload.put("error", "localized diagnostic");
        return DesktopCommandResult.error(payload, "localized diagnostic");
    }

    private static DesktopCommandResult nativeFailure(String errorCode,
            String domain, long code) {
        Map<String, Object> payload = authorizationPayload(errorCode);
        payload.put("nativeErrorDomain", domain);
        payload.put("nativeErrorCode", code);
        payload.put("error", "localized diagnostic");
        return DesktopCommandResult.error(payload, "localized diagnostic");
    }

    private static Map<String, Object> replacing(Map<String, Object> original,
            String key, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(original);
        copy.put(key, value);
        return copy;
    }

    private static Map<String, Object> without(Map<String, Object> original,
            String key) {
        Map<String, Object> copy = new LinkedHashMap<>(original);
        copy.remove(key);
        return copy;
    }

    private static void malformed(boolean success, Map<String, Object> payload,
            String error) {
        check(new DesktopCommandResult(success, payload, error),
                DesktopNotificationResult.Outcome.ERROR, Optional.empty());
    }

    private static void check(DesktopCommandResult raw,
            DesktopNotificationResult.Outcome outcome,
            Optional<String> identifier) {
        RecordingBridge bridge = new RecordingBridge(raw);
        DesktopNotificationResult result = new DesktopNotification(bridge)
                .showResult("Title", "Body");
        equal(outcome, result.outcome(), "outcome for " + raw);
        equal(identifier, result.notificationId(), "identifier for " + raw);
        same(raw, result.commandResult(), "retained raw result");
        equal(1, bridge.commands.size(), "typed single invocation");
        command(bridge.commands.getFirst(), "Title", "Body");
    }

    private static void command(DesktopCommand actual, String title, String body) {
        equal("notification.show", actual.name(), "command name");
        equal(Map.of("title", title, "body", body), actual.payload(),
                "command payload");
    }

    private static void equal(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected=" + expected
                    + " actual=" + actual);
        }
    }

    private static void same(Object expected, Object actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + ": expected same instance");
        }
    }

    private static void nullPointer(Runnable action) {
        try {
            action.run();
        } catch (NullPointerException expected) {
            return;
        }
        throw new AssertionError("Expected NullPointerException");
    }

    private static final class RecordingBridge implements NativeBridge {
        private final Deque<DesktopCommandResult> results = new ArrayDeque<>();
        private final List<DesktopCommand> commands = new ArrayList<>();

        private RecordingBridge(DesktopCommandResult... results) {
            this.results.addAll(List.of(results));
        }

        @Override
        public DesktopCommandResult invoke(DesktopCommand command) {
            commands.add(command);
            DesktopCommandResult result = results.pollFirst();
            if (result == null) {
                throw new AssertionError("Unexpected command " + command.name());
            }
            return result;
        }
    }

    private static final class CaptureBridge implements NativeBridge {
        private final NativeBridge captured;
        private int captures;

        private CaptureBridge(NativeBridge captured) {
            this.captured = captured;
        }

        @Override
        public DesktopCommandResult invoke(DesktopCommand command) {
            throw new AssertionError("Unbound capability context invoked");
        }

        @Override
        public NativeBridge captureCapabilityContext() {
            captures++;
            return captured;
        }
    }
}
