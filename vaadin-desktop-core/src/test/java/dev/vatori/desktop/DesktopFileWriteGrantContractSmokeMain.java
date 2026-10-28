package dev.vatori.desktop;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Deterministic public contract checks; no platform or filesystem substitute. */
public final class DesktopFileWriteGrantContractSmokeMain {
    private static final String GRANT_ID = "8148ee25-dc02-49b7-8002-fbf5bf23810b";

    private DesktopFileWriteGrantContractSmokeMain() {
    }

    public static void main(String[] args) throws Exception {
        capturesOneOwnerBridgeAndSendsExactCommands();
        classifiesSelectionOutcomesInOrder();
        rejectsMalformedSelections();
        boundsInputBeforeInvokingAndDoesNotRetainMutableBytes();
        passesNegativeOffsetsToTheHost();
        classifiesWriteOutcomesAndRejectsContradictions();
        validatesExactSuccessfulCountsAndOverflow();
        preservesOnlyValidPartialFailureCounts();
        releasesIdempotentlyWithoutLocallyForgingWriteAvailability();
        retriesReleaseOnlyAfterUnsuccessfulRelease();
        classifiesCancelResultsAndPreservesOnlyReliablePrefixes();
        rejectsMalformedCancelResults();
        validatesCanceledWritePrefixesAndContradictions();
        cancelsIdempotentlyWithoutForgingReleaseOrAvailability();
        cancellationAfterReleaseStillReachesHost();
        onlyConfirmedCancellationIsCached();
        writeAndCancelCanOverlap();
        terminalCallsShareOneLock();
        interruptedWaitIsNotConfirmedCancellation();
        System.out.println("Native file write grant Java contract smoke passed");
    }

    private static void capturesOneOwnerBridgeAndSendsExactCommands() {
        RecordingBridge captured = new RecordingBridge(selected(),
                written(0, 3), DesktopCommandResult.ok(Map.of("released", true)));
        CaptureBridge unbound = new CaptureBridge(captured);
        DesktopFileWriteGrantResult selection = new DesktopFileDialog(unbound)
                .saveNewWriteGrant("Save report", "report.bin");
        equal(DesktopFileWriteGrantResult.Outcome.SELECTED, selection.outcome(), "selection");
        equal(false, selection.commandResult().payload().containsKey("grantId"), "hidden token");
        DesktopFileWriteGrant grant = selection.grant().orElseThrow();
        equal("report.bin", grant.filename(), "basename");
        equal(65_536, grant.maxWriteBytes(), "bounded metadata");
        equal(true, grant.securityScoped(), "security scope marker");
        DesktopFileWriteResult write = grant.write(0, new byte[]{0, -1, 10});
        equal(DesktopFileWriteResult.Outcome.WRITTEN, write.outcome(), "write outcome");
        equal(3, write.bytesWritten(), "write count");
        equal(3L, write.nextOffset(), "next offset");
        equal(DesktopFileGrantReleaseResult.Outcome.RELEASED,
                grant.release().outcome(), "release outcome");
        equal(1, unbound.captures, "capture count");
        equal(3, captured.commands.size(), "bound commands");
        command(captured.commands.get(0), "fileDialog.saveNewWriteGrant",
                Map.of("title", "Save report", "suggestedName", "report.bin"));
        command(captured.commands.get(1), "fileGrant.write",
                Map.of("grantId", GRANT_ID, "offset", 0L, "bodyBase64", "AP8K"));
        command(captured.commands.get(2), "fileGrant.releaseWrite",
                Map.of("grantId", GRANT_ID));
    }

    private static void classifiesSelectionOutcomesInOrder() {
        selectionOutcome(DesktopCommandResult.error(Map.of(
                "operationInvalidated", true, "permissionDecision", "denied",
                "errorCode", "target-exists", "canceled", true), "invalidated"),
                DesktopFileWriteGrantResult.Outcome.INVALIDATED);
        selectionOutcome(DesktopCommandResult.error(Map.of(
                "permissionDecision", "denied", "errorCode", "target-exists"), "denied"),
                DesktopFileWriteGrantResult.Outcome.DENIED);
        selectionOutcome(DesktopCommandResult.error(Map.of(
                "errorCode", "target-exists"), "existing target"),
                DesktopFileWriteGrantResult.Outcome.TARGET_EXISTS);
        selectionOutcome(DesktopCommandResult.ok(Map.of("canceled", true)),
                DesktopFileWriteGrantResult.Outcome.CANCELED);
        selectionOutcome(DesktopCommandResult.error(Map.of("canceled", true), "failed"),
                DesktopFileWriteGrantResult.Outcome.ERROR);
        selectionOutcome(DesktopCommandResult.ok(Map.of(
                "canceled", true, "grantId", GRANT_ID)),
                DesktopFileWriteGrantResult.Outcome.ERROR);
        selectionOutcome(DesktopCommandResult.ok(Map.of("errorCode", "target-exists")),
                DesktopFileWriteGrantResult.Outcome.ERROR);
    }

    private static void rejectsMalformedSelections() {
        Map<String, Object> valid = selectionPayload();
        for (String key : List.of("canceled", "grantId", "filename", "maxWriteBytes",
                "securityScoped", "createNewOnly", "pathExposed", "usedSavePanel")) {
            Map<String, Object> payload = new LinkedHashMap<>(valid);
            payload.remove(key);
            selectionOutcome(DesktopCommandResult.ok(payload),
                    DesktopFileWriteGrantResult.Outcome.ERROR);
        }
        for (Map.Entry<String, Object> invalid : List.<Map.Entry<String, Object>>of(
                Map.entry("grantId", "1-1-1-1-1"),
                Map.entry("filename", "/tmp/report.bin"), Map.entry("filename", ".."),
                Map.entry("filename", ""), Map.entry("filename", "bad\0name"),
                Map.entry("maxWriteBytes", 0), Map.entry("maxWriteBytes", 65_537),
                Map.entry("maxWriteBytes", 65_536.0),
                Map.entry("maxWriteBytes", BigInteger.ONE.shiftLeft(63)),
                Map.entry("securityScoped", "true"), Map.entry("createNewOnly", false),
                Map.entry("pathExposed", true), Map.entry("usedSavePanel", false),
                Map.entry("path", "/private/selected"), Map.entry("paths", List.of()),
                Map.entry("url", "file:///private/selected"),
                Map.entry("errorCode", "selection-error"))) {
            Map<String, Object> payload = new LinkedHashMap<>(valid);
            payload.put(invalid.getKey(), invalid.getValue());
            selectionOutcome(DesktopCommandResult.ok(payload),
                    DesktopFileWriteGrantResult.Outcome.ERROR);
        }
        Map<String, Object> smaller = new LinkedHashMap<>(valid);
        smaller.put("maxWriteBytes", BigInteger.valueOf(4));
        DesktopFileWriteGrantResult result = select(DesktopCommandResult.ok(smaller));
        equal(4, result.grant().orElseThrow().maxWriteBytes(), "smaller host bound");
    }

    private static void boundsInputBeforeInvokingAndDoesNotRetainMutableBytes() {
        RecordingBridge bridge = new RecordingBridge(selected(), written(0, 3), written(3, 65_536));
        DesktopFileWriteGrant grant = grant(bridge);
        for (byte[] invalid : new byte[][]{null, new byte[0], new byte[65_537]}) {
            expectIllegalArgument(() -> grant.write(0, invalid));
        }
        equal(1, bridge.commands.size(), "invalid input sends no command");
        byte[] bytes = {1, 2, 3};
        grant.write(0, bytes);
        bytes[0] = 99;
        equal("AQID", bridge.commands.get(1).payload().get("bodyBase64"), "immutable body snapshot");
        grant.write(3, new byte[65_536]);
        equal(65_536, Base64.getDecoder().decode((String) bridge.commands.get(2)
                .payload().get("bodyBase64")).length, "maximum block accepted");

        Map<String, Object> smaller = selectionPayload();
        smaller.put("maxWriteBytes", 2);
        RecordingBridge limited = new RecordingBridge(DesktopCommandResult.ok(smaller));
        DesktopFileWriteGrant limitedGrant = grant(limited);
        expectIllegalArgument(() -> limitedGrant.write(0, new byte[3]));
        equal(1, limited.commands.size(), "host-specific bound enforced before command");
    }

    private static void passesNegativeOffsetsToTheHost() {
        RecordingBridge bridge = new RecordingBridge(selected(), error("invalid-write-range"));
        DesktopFileWriteResult result = grant(bridge).write(-1, new byte[]{1});
        equal(DesktopFileWriteResult.Outcome.INVALID_RANGE, result.outcome(), "negative offset outcome");
        equal(-1L, bridge.commands.get(1).payload().get("offset"), "negative offset forwarded");
    }

    private static void classifiesWriteOutcomesAndRejectsContradictions() {
        writeOutcome(DesktopCommandResult.error(Map.of("operationInvalidated", true,
                "permissionDecision", "denied", "errorCode", "grant-unavailable",
                "grantUnavailable", true), "invalidated"), DesktopFileWriteResult.Outcome.INVALIDATED);
        writeOutcome(DesktopCommandResult.error(Map.of("permissionDecision", "denied",
                "errorCode", "grant-unavailable", "grantUnavailable", true), "denied"),
                DesktopFileWriteResult.Outcome.DENIED);
        writeOutcome(unavailable(), DesktopFileWriteResult.Outcome.UNAVAILABLE);
        writeOutcome(error("invalid-write-range"), DesktopFileWriteResult.Outcome.INVALID_RANGE);
        writeOutcome(error("invalid-write-data"), DesktopFileWriteResult.Outcome.INVALID_DATA);
        writeOutcome(error("grant-busy"), DesktopFileWriteResult.Outcome.BUSY);
        writeOutcome(error("unknown"), DesktopFileWriteResult.Outcome.ERROR);
        writeOutcome(error("grant-unavailable"), DesktopFileWriteResult.Outcome.ERROR);
        Map<String, Object> contradiction = writePayload(0, 3);
        contradiction.put("errorCode", "invalid-write-range");
        writeOutcome(DesktopCommandResult.ok(contradiction), DesktopFileWriteResult.Outcome.ERROR);
        contradiction.remove("errorCode");
        contradiction.put("grantUnavailable", true);
        DesktopFileWriteResult concurrentRelease = DesktopFileWriteResult.from(
                DesktopCommandResult.ok(contradiction), 0, 3, 65_536);
        equal(DesktopFileWriteResult.Outcome.WRITTEN, concurrentRelease.outcome(),
                "accepted write succeeds during release");
        equal(3, concurrentRelease.bytesWritten(), "release race retains known bytes");
        equal(3L, concurrentRelease.nextOffset(), "release race retains known offset");
    }

    private static void validatesExactSuccessfulCountsAndOverflow() {
        for (String missing : List.of("offset", "nextOffset", "bytesWritten")) {
            Map<String, Object> payload = writePayload(0, 3);
            payload.remove(missing);
            writeOutcome(DesktopCommandResult.ok(payload), DesktopFileWriteResult.Outcome.ERROR);
        }
        for (Map.Entry<String, Object> invalid : List.<Map.Entry<String, Object>>of(
                Map.entry("offset", 1L), Map.entry("offset", 0.0),
                Map.entry("nextOffset", 4L), Map.entry("nextOffset", -1L),
                Map.entry("bytesWritten", 2), Map.entry("bytesWritten", 4),
                Map.entry("bytesWritten", -1), Map.entry("bytesWritten", 3.0))) {
            Map<String, Object> payload = writePayload(0, 3);
            payload.put(invalid.getKey(), invalid.getValue());
            writeOutcome(DesktopCommandResult.ok(payload), DesktopFileWriteResult.Outcome.ERROR);
        }
        DesktopFileWriteResult overflow = DesktopFileWriteResult.from(
                DesktopCommandResult.ok(Map.of("offset", Long.MAX_VALUE,
                        "nextOffset", Long.MIN_VALUE, "bytesWritten", 1)),
                Long.MAX_VALUE, 1, 65_536);
        equal(DesktopFileWriteResult.Outcome.ERROR, overflow.outcome(), "overflow rejected");
        DesktopFileWriteResult edge = DesktopFileWriteResult.from(
                DesktopCommandResult.ok(Map.of("offset", Long.MAX_VALUE - 1,
                        "nextOffset", Long.MAX_VALUE, "bytesWritten", 1)),
                Long.MAX_VALUE - 1, 1, 65_536);
        equal(DesktopFileWriteResult.Outcome.WRITTEN, edge.outcome(), "maximum exact offset");
        for (int length : new int[]{0, 65_537}) {
            equal(DesktopFileWriteResult.Outcome.ERROR, DesktopFileWriteResult.from(
                    written(0, length), 0, length, 65_536).outcome(), "invalid requested count");
        }
    }

    private static void preservesOnlyValidPartialFailureCounts() {
        Map<String, Object> payload = writePayload(5, 2);
        payload.put("errorCode", "write-failed");
        payload.put("grantUnavailable", true);
        payload.put("grantId", GRANT_ID);
        DesktopFileWriteResult partial = DesktopFileWriteResult.from(
                DesktopCommandResult.error(payload, "write failed"), 5, 3, 65_536);
        equal(DesktopFileWriteResult.Outcome.ERROR, partial.outcome(), "partial error");
        equal(5L, partial.offset(), "partial offset");
        equal(7L, partial.nextOffset(), "partial next offset");
        equal(2, partial.bytesWritten(), "partial written prefix");
        equal(false, partial.commandResult().payload().containsKey("grantId"), "partial hides token");
        payload.put("nextOffset", 8L);
        DesktopFileWriteResult malformed = DesktopFileWriteResult.from(
                DesktopCommandResult.error(payload, "write failed"), 5, 3, 65_536);
        equal(-1L, malformed.offset(), "malformed prefix offset unknown");
        equal(-1L, malformed.nextOffset(), "malformed prefix next unknown");
        equal(0, malformed.bytesWritten(), "malformed prefix count unknown");
        payload.put("nextOffset", 7L);
        payload.put("errorCode", "other-error");
        equal(-1L, DesktopFileWriteResult.from(DesktopCommandResult.error(payload, "failed"),
                5, 3, 65_536).offset(), "unrelated error cannot claim prefix");
    }

    private static void releasesIdempotentlyWithoutLocallyForgingWriteAvailability() {
        RecordingBridge bridge = new RecordingBridge(selected(),
                DesktopCommandResult.ok(Map.of("released", true, "grantId", GRANT_ID)),
                unavailable());
        DesktopFileWriteGrant grant = grant(bridge);
        DesktopFileGrantReleaseResult first = grant.release();
        DesktopFileGrantReleaseResult second = grant.release();
        equal(DesktopFileGrantReleaseResult.Outcome.RELEASED, first.outcome(), "first release");
        equal(DesktopFileGrantReleaseResult.Outcome.ALREADY_RELEASED, second.outcome(), "second release");
        equal(false, second.commandResult().payload().containsKey("grantId"), "release hides token");
        if (first.commandResult() != second.commandResult()) {
            throw new AssertionError("Local release must retain sanitized successful result");
        }
        grant.close();
        equal(2, bridge.commands.size(), "close after release is local");
        equal(DesktopFileWriteResult.Outcome.UNAVAILABLE,
                grant.write(0, new byte[]{1}).outcome(), "host rejects write after release");
        equal(3, bridge.commands.size(), "write after release still reaches host");
    }

    private static void retriesReleaseOnlyAfterUnsuccessfulRelease() {
        RecordingBridge bridge = new RecordingBridge(selected(),
                DesktopCommandResult.error("release failed"),
                DesktopCommandResult.ok(Map.of("released", true)));
        DesktopFileWriteGrant grant = grant(bridge);
        equal(DesktopFileGrantReleaseResult.Outcome.ERROR, grant.release().outcome(), "failed release");
        equal(DesktopFileGrantReleaseResult.Outcome.RELEASED, grant.release().outcome(), "retried release");
        equal(3, bridge.commands.size(), "retry invokes host");
    }

    private static void classifiesCancelResultsAndPreservesOnlyReliablePrefixes() {
        for (long prefix : new long[]{0, 65_542, Long.MAX_VALUE}) {
            Map<String, Object> payload = cancelPayload(prefix, true);
            payload.put("grantId", GRANT_ID);
            DesktopFileWriteCancelResult result = DesktopFileWriteCancelResult.from(DesktopCommandResult.ok(payload));
            equal(DesktopFileWriteCancelResult.Outcome.CANCELED, result.outcome(), "confirmed cancellation");
            equal(prefix, result.nextOffset(), "confirmed total prefix");
            equal(false, result.commandResult().payload().containsKey("grantId"), "cancel hides private token");
        }
        Map<String, Object> failed = cancelPayload(17, false);
        failed.put("errorCode", "grant-close-failed");
        DesktopFileWriteCancelResult closeFailure = DesktopFileWriteCancelResult.from(DesktopCommandResult.error(failed, "close failed"));
        equal(DesktopFileWriteCancelResult.Outcome.ERROR, closeFailure.outcome(), "close failure not cancellation success");
        equal(17L, closeFailure.nextOffset(), "close failure retains exact prefix");
        failed.put("errorCode", "unrelated-failure");
        cancelUnknown(DesktopCommandResult.error(failed, "failed"), DesktopFileWriteCancelResult.Outcome.ERROR);
        for (Map.Entry<String, DesktopFileWriteCancelResult.Outcome> entry : Map.of(
                "operationInvalidated", DesktopFileWriteCancelResult.Outcome.INVALIDATED,
                "permissionDecision", DesktopFileWriteCancelResult.Outcome.DENIED,
                "errorCode", DesktopFileWriteCancelResult.Outcome.UNAVAILABLE).entrySet()) {
            var payload = cancelPayload(65_542, false);
            payload.put(entry.getKey(), entry.getKey().equals("operationInvalidated") ? true : entry.getKey().equals("permissionDecision") ? "denied" : "grant-unavailable");
            payload.put("grantId", GRANT_ID);
            cancelUnknown(DesktopCommandResult.error(payload, "not available"), entry.getValue());
        }
        cancelUnknown(DesktopCommandResult.error("Interrupted while waiting for native capability result"), DesktopFileWriteCancelResult.Outcome.ERROR);
        cancelUnknown(DesktopCommandResult.error("Sidecar runtime closed"), DesktopFileWriteCancelResult.Outcome.ERROR);
    }

    private static void rejectsMalformedCancelResults() {
        for (String field : List.of("released", "offset", "bytesWritten", "error")) {
            var payload = cancelPayload(65_542, true);
            payload.put(field, field.equals("error") ? "contradictory failure" : 0);
            cancelUnknown(DesktopCommandResult.ok(payload), DesktopFileWriteCancelResult.Outcome.ERROR);
        }
        for (boolean success : new boolean[]{true, false}) {
            Map<String, Object> valid = cancelPayload(65_542, success);
            if (!success) valid.put("errorCode", "grant-close-failed");
            for (String key : List.of("command", "operation", "success", "canceled", "cancellationRequested", "grantUnavailable", "grantState", "nextOffset", "securityScopeStopped", "pathExposed")) {
                var payload = new LinkedHashMap<>(valid); payload.remove(key);
                cancelUnknown(new DesktopCommandResult(success, payload, success ? null : "close failed"), DesktopFileWriteCancelResult.Outcome.ERROR);
            }
            for (Map.Entry<String, Object> invalid : List.<Map.Entry<String, Object>>of(
                    Map.entry("command", "fileGrant.releaseWrite"), Map.entry("operation", "write-grant"),
                    Map.entry("success", !success), Map.entry("success", Boolean.toString(success)),
                    Map.entry("canceled", !success), Map.entry("canceled", Boolean.toString(success)),
                    Map.entry("cancellationRequested", false), Map.entry("grantUnavailable", false),
                    Map.entry("grantState", "active"), Map.entry("securityScopeStopped", "true"),
                    Map.entry("pathExposed", true), Map.entry("path", "/private/selected"),
                    Map.entry("paths", List.of()), Map.entry("url", "file:///private/selected"), Map.entry("bodyBase64", "AA=="),
                    Map.entry("nextOffset", -1L), Map.entry("nextOffset", 65_542.0), Map.entry("nextOffset", "65542"),
                    Map.entry("nextOffset", true), Map.entry("nextOffset", BigInteger.ONE.shiftLeft(63)),
                    Map.entry("errorCode", "write-canceled"))) {
                var payload = new LinkedHashMap<>(valid); payload.put(invalid.getKey(), invalid.getValue());
                cancelUnknown(new DesktopCommandResult(success, payload, success ? null : "close failed"), DesktopFileWriteCancelResult.Outcome.ERROR);
            }
        }
    }

    private static void validatesCanceledWritePrefixesAndContradictions() {
        for (int count : new int[]{0, 1, 2}) {
            var payload = canceledWritePayload(5, count); payload.put("grantId", GRANT_ID);
            DesktopFileWriteResult result = DesktopFileWriteResult.from(DesktopCommandResult.error(payload, "write canceled"), 5, 3, 65_536);
            equal(DesktopFileWriteResult.Outcome.CANCELED, result.outcome(), "cooperative canceled write");
            equal(5L, result.offset(), "canceled write offset"); equal(5L + count, result.nextOffset(), "canceled next offset");
            equal(count, result.bytesWritten(), "canceled write exact prefix");
            equal(false, result.commandResult().payload().containsKey("grantId"), "canceled write hides token");
        }
        for (String key : List.of("command", "operation", "success", "canceled", "grantUnavailable", "grantState", "pathExposed", "offset", "bytesWritten", "nextOffset")) {
            var payload = canceledWritePayload(0, 2); payload.remove(key);
            writeOutcome(DesktopCommandResult.error(payload, "write canceled"), DesktopFileWriteResult.Outcome.ERROR);
        }
        for (Map.Entry<String, Object> invalid : List.<Map.Entry<String, Object>>of(
                Map.entry("command", "fileGrant.cancelWrite"), Map.entry("operation", "cancel-write-grant"),
                Map.entry("success", true), Map.entry("canceled", false), Map.entry("canceled", "true"),
                Map.entry("grantUnavailable", false), Map.entry("grantState", "active"), Map.entry("pathExposed", true),
                Map.entry("offset", 0.0), Map.entry("offset", -1L), Map.entry("nextOffset", 3L), Map.entry("nextOffset", "2"),
                Map.entry("bytesWritten", -1), Map.entry("bytesWritten", 3), Map.entry("bytesWritten", 2.0),
                Map.entry("bytesWritten", BigInteger.ONE.shiftLeft(63)), Map.entry("bodyBase64", "AA=="), Map.entry("path", "/private/selected"))) {
            var payload = canceledWritePayload(0, 2); payload.put(invalid.getKey(), invalid.getValue());
            writeOutcome(DesktopCommandResult.error(payload, "write canceled"), DesktopFileWriteResult.Outcome.ERROR);
        }
        var complete = canceledWritePayload(0, 3);
        writeOutcome(DesktopCommandResult.error(complete, "write canceled"), DesktopFileWriteResult.Outcome.ERROR);
        writeOutcome(DesktopCommandResult.ok(complete), DesktopFileWriteResult.Outcome.ERROR);
        var contradiction = writePayload(0, 3); contradiction.put("canceled", true);
        writeOutcome(DesktopCommandResult.ok(contradiction), DesktopFileWriteResult.Outcome.ERROR);
        var overflow = canceledWritePayload(Long.MAX_VALUE, 1);
        equal(-1L, DesktopFileWriteResult.from(DesktopCommandResult.error(overflow, "canceled"), Long.MAX_VALUE, 2, 65_536).nextOffset(), "canceled arithmetic overflow rejected");
        var edge = canceledWritePayload(Long.MAX_VALUE - 2, 1);
        equal(Long.MAX_VALUE - 1, DesktopFileWriteResult.from(DesktopCommandResult.error(edge, "canceled"), Long.MAX_VALUE - 2, 2, 65_536).nextOffset(), "canceled edge prefix preserved");
    }

    private static void cancelsIdempotentlyWithoutForgingReleaseOrAvailability() {
        var payload = cancelPayload(65_542, true); payload.put("grantId", GRANT_ID);
        RecordingBridge captured = new RecordingBridge(selected(), DesktopCommandResult.ok(payload), unavailable(), unavailable());
        CaptureBridge unbound = new CaptureBridge(captured);
        DesktopFileWriteGrant grant = new DesktopFileDialog(unbound).saveNewWriteGrant("Save report", "report.bin").grant().orElseThrow();
        DesktopFileWriteCancelResult first = grant.cancel(), second = grant.cancel();
        equal(DesktopFileWriteCancelResult.Outcome.CANCELED, first.outcome(), "first cancellation");
        equal(DesktopFileWriteCancelResult.Outcome.ALREADY_CANCELED, second.outcome(), "local repeated cancellation");
        equal(65_542L, second.nextOffset(), "cached final prefix");
        if (first.commandResult() != second.commandResult()) throw new AssertionError("Cancellation did not retain original sanitized result");
        grant.close(); equal(2, captured.commands.size(), "close after cancellation is local");
        equal(DesktopFileGrantReleaseResult.Outcome.UNAVAILABLE, grant.release().outcome(), "release after cancel is not forged RELEASED");
        equal(DesktopFileWriteResult.Outcome.UNAVAILABLE, grant.write(65_542, new byte[]{1}).outcome(), "write after cancellation reaches host");
        equal(1, unbound.captures, "cancellation uses captured owner");
        command(captured.commands.get(1), "fileGrant.cancelWrite", Map.of("grantId", GRANT_ID));
        command(captured.commands.get(2), "fileGrant.releaseWrite", Map.of("grantId", GRANT_ID));
        equal(4, captured.commands.size(), "only confirmed cancel is settled locally");
    }

    private static void cancellationAfterReleaseStillReachesHost() {
        RecordingBridge bridge = new RecordingBridge(selected(), DesktopCommandResult.ok(Map.of("released", true)), unavailable());
        DesktopFileWriteGrant grant = grant(bridge); grant.release();
        equal(DesktopFileWriteCancelResult.Outcome.UNAVAILABLE, grant.cancel().outcome(), "cancel after release host unavailable");
        command(bridge.commands.get(2), "fileGrant.cancelWrite", Map.of("grantId", GRANT_ID));
        grant.close(); equal(3, bridge.commands.size(), "release idempotence remains unchanged");
    }

    private static void onlyConfirmedCancellationIsCached() {
        var malformed = cancelPayload(3, true); malformed.put("nextOffset", "3");
        var closeFailed = cancelPayload(3, false); closeFailed.put("errorCode", "grant-close-failed");
        RecordingBridge bridge = new RecordingBridge(selected(), DesktopCommandResult.error("lost reply"), DesktopCommandResult.ok(malformed),
                DesktopCommandResult.error(closeFailed, "close failed"), DesktopCommandResult.ok(cancelPayload(3, true)));
        DesktopFileWriteGrant grant = grant(bridge);
        for (int i = 0; i < 3; i++) equal(DesktopFileWriteCancelResult.Outcome.ERROR, grant.cancel().outcome(), "unconfirmed result must not be cached");
        equal(DesktopFileWriteCancelResult.Outcome.CANCELED, grant.cancel().outcome(), "explicit later call may confirm cancellation");
        equal(DesktopFileWriteCancelResult.Outcome.ALREADY_CANCELED, grant.cancel().outcome(), "confirmation cached only once");
        equal(5, bridge.commands.size(), "no automatic retries");
    }

    private static void writeAndCancelCanOverlap() throws Exception {
        CountDownLatch writing = new CountDownLatch(1), cancelEntered = new CountDownLatch(1);
        AtomicReference<DesktopFileWriteResult> written = new AtomicReference<>();
        NativeBridge bridge = command -> {
            if (command.name().equals("fileDialog.saveNewWriteGrant")) return selected();
            if (command.name().equals("fileGrant.write")) {
                writing.countDown(); await(cancelEntered);
                return DesktopCommandResult.error(canceledWritePayload(0, 2), "write canceled");
            }
            equal("fileGrant.cancelWrite", command.name(), "concurrent cancel command"); cancelEntered.countDown();
            return DesktopCommandResult.ok(cancelPayload(2, true));
        };
        DesktopFileWriteGrant grant = grant(bridge);
        Thread writer = Thread.ofVirtual().start(() -> written.set(grant.write(0, new byte[]{1, 2, 3})));
        try {
            await(writing); DesktopFileWriteCancelResult canceled = grant.cancel();
            writer.join(2000); if (writer.isAlive()) throw new AssertionError("Cancellation did not overlap blocked write");
            equal(DesktopFileWriteCancelResult.Outcome.CANCELED, canceled.outcome(), "concurrent cancellation settled");
            equal(2L, canceled.nextOffset(), "cancel final prefix");
            equal(DesktopFileWriteResult.Outcome.CANCELED, written.get().outcome(), "write canceled prefix");
            equal(canceled.nextOffset(), written.get().nextOffset(), "two independent results agree");
        } finally { cancelEntered.countDown(); writer.join(2000); }
    }

    private static void terminalCallsShareOneLock() throws Exception {
        CountDownLatch cancelEntered = new CountDownLatch(1), finishCancel = new CountDownLatch(1), releaseStarted = new CountDownLatch(1);
        AtomicInteger cancels = new AtomicInteger(), releases = new AtomicInteger();
        AtomicReference<DesktopFileWriteGrant> handle = new AtomicReference<>();
        AtomicReference<DesktopFileWriteCancelResult> first = new AtomicReference<>(), repeated = new AtomicReference<>();
        AtomicReference<DesktopFileGrantReleaseResult> released = new AtomicReference<>();
        NativeBridge bridge = command -> {
            if (command.name().equals("fileDialog.saveNewWriteGrant")) return selected();
            if (!Thread.holdsLock(handle.get())) throw new AssertionError("Terminal calls do not share the facade lock");
            if (command.name().equals("fileGrant.cancelWrite")) {
                cancels.incrementAndGet(); cancelEntered.countDown(); await(finishCancel);
                return DesktopCommandResult.ok(cancelPayload(3, true));
            }
            releases.incrementAndGet(); equal(0L, finishCancel.getCount(), "release entered before cancellation settled"); return unavailable();
        };
        DesktopFileWriteGrant grant = grant(bridge); handle.set(grant);
        Thread cancel = Thread.ofVirtual().start(() -> first.set(grant.cancel())); await(cancelEntered);
        Thread again = Thread.ofVirtual().start(() -> repeated.set(grant.cancel()));
        Thread release = Thread.ofVirtual().start(() -> { releaseStarted.countDown(); released.set(grant.release()); });
        try { await(releaseStarted); equal(0, releases.get(), "release waits behind ongoing cancel"); }
        finally { finishCancel.countDown(); cancel.join(2000); again.join(2000); release.join(2000); }
        if (cancel.isAlive() || again.isAlive() || release.isAlive()) throw new AssertionError("Terminal call deadlock");
        equal(DesktopFileWriteCancelResult.Outcome.CANCELED, first.get().outcome(), "first concurrent cancel");
        equal(DesktopFileWriteCancelResult.Outcome.ALREADY_CANCELED, repeated.get().outcome(), "second concurrent cancel local");
        equal(DesktopFileGrantReleaseResult.Outcome.UNAVAILABLE, released.get().outcome(), "concurrent release stays distinct");
        equal(1, cancels.get(), "concurrent cancellation sends once"); equal(1, releases.get(), "release still reaches host");
    }

    private static void interruptedWaitIsNotConfirmedCancellation() throws Exception {
        CountDownLatch entered = new CountDownLatch(1), waitForReply = new CountDownLatch(1);
        AtomicInteger invocations = new AtomicInteger(); AtomicReference<DesktopFileWriteCancelResult> interrupted = new AtomicReference<>();
        AtomicReference<Boolean> interruptionRetained = new AtomicReference<>();
        NativeBridge bridge = command -> {
            if (command.name().equals("fileDialog.saveNewWriteGrant")) return selected();
            if (invocations.incrementAndGet() > 1) return DesktopCommandResult.ok(cancelPayload(7, true));
            entered.countDown();
            try { waitForReply.await(); throw new AssertionError("Expected interrupted bridge wait"); }
            catch (InterruptedException expected) { Thread.currentThread().interrupt(); return DesktopCommandResult.error("Interrupted while waiting for native capability result"); }
        };
        DesktopFileWriteGrant grant = grant(bridge);
        Thread caller = Thread.ofVirtual().start(() -> { interrupted.set(grant.cancel()); interruptionRetained.set(Thread.currentThread().isInterrupted()); });
        await(entered); caller.interrupt(); caller.join(2000);
        if (caller.isAlive()) throw new AssertionError("Interrupted cancel caller did not finish");
        equal(true, interruptionRetained.get(), "interruption retained");
        equal(DesktopFileWriteCancelResult.Outcome.ERROR, interrupted.get().outcome(), "interruption is not native cancellation confirmation");
        equal(-1L, interrupted.get().nextOffset(), "interrupted result must not guess zero written bytes");
        equal(1, invocations.get(), "interruption does not auto-retry");
        equal(7L, grant.cancel().nextOffset(), "later explicit host confirmation not masked by interrupted wait");
    }

    private static Map<String, Object> cancelPayload(long nextOffset, boolean success) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("command", "fileGrant.cancelWrite"); payload.put("operation", "cancel-write-grant"); payload.put("success", success);
        payload.put("canceled", success); payload.put("cancellationRequested", true); payload.put("grantUnavailable", true);
        payload.put("grantState", "unavailable"); payload.put("nextOffset", nextOffset); payload.put("securityScopeStopped", true); payload.put("pathExposed", false);
        return payload;
    }

    private static Map<String, Object> canceledWritePayload(long offset, int count) {
        var payload = new LinkedHashMap<>(writePayload(offset, count));
        payload.put("command", "fileGrant.write"); payload.put("operation", "write-grant"); payload.put("success", false);
        payload.put("errorCode", "write-canceled"); payload.put("canceled", true); payload.put("grantUnavailable", true);
        payload.put("grantState", "unavailable"); payload.put("pathExposed", false);
        return payload;
    }

    private static void cancelUnknown(DesktopCommandResult input, DesktopFileWriteCancelResult.Outcome expected) {
        DesktopFileWriteCancelResult result = DesktopFileWriteCancelResult.from(input);
        equal(expected, result.outcome(), "cancel outcome"); equal(-1L, result.nextOffset(), "unconfirmed prefix is unknown");
        equal(false, result.commandResult().payload().containsKey("grantId"), "cancel result hides token");
    }

    private static void await(CountDownLatch latch) {
        try { if (!latch.await(2, TimeUnit.SECONDS)) throw new AssertionError("Timed out waiting for contract smoke barrier"); }
        catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new AssertionError(error); }
    }

    private static DesktopFileWriteGrantResult select(DesktopCommandResult result) {
        return new DesktopFileDialog(new RecordingBridge(result))
                .saveNewWriteGrant("Save report", "report.bin");
    }

    private static DesktopFileWriteGrant grant(NativeBridge bridge) {
        return new DesktopFileDialog(bridge).saveNewWriteGrant("Save report", "report.bin")
                .grant().orElseThrow();
    }

    private static DesktopCommandResult selected() {
        return DesktopCommandResult.ok(selectionPayload());
    }

    private static Map<String, Object> selectionPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("canceled", false);
        payload.put("grantId", GRANT_ID);
        payload.put("filename", "report.bin");
        payload.put("maxWriteBytes", 65_536);
        payload.put("securityScoped", true);
        payload.put("createNewOnly", true);
        payload.put("pathExposed", false);
        payload.put("usedSavePanel", true);
        return payload;
    }

    private static DesktopCommandResult written(long offset, int count) {
        return DesktopCommandResult.ok(writePayload(offset, count));
    }

    private static Map<String, Object> writePayload(long offset, int count) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("offset", offset);
        payload.put("nextOffset", offset + count);
        payload.put("bytesWritten", count);
        return payload;
    }

    private static DesktopCommandResult error(String code) {
        return DesktopCommandResult.error(Map.of("errorCode", code), "operation failed");
    }

    private static DesktopCommandResult unavailable() {
        return DesktopCommandResult.error(Map.of("errorCode", "grant-unavailable",
                "grantUnavailable", true), "unavailable");
    }

    private static void selectionOutcome(DesktopCommandResult input,
            DesktopFileWriteGrantResult.Outcome expected) {
        DesktopFileWriteGrantResult result = select(input);
        equal(expected, result.outcome(), "selection terminal");
        equal(Optional.empty(), result.grant(), "terminal no grant");
        equal(false, result.commandResult().payload().containsKey("grantId"), "terminal token hidden");
    }

    private static void writeOutcome(DesktopCommandResult input,
            DesktopFileWriteResult.Outcome expected) {
        DesktopFileWriteResult result = DesktopFileWriteResult.from(input, 0, 3, 65_536);
        equal(expected, result.outcome(), "write terminal");
        equal(-1L, result.offset(), "terminal offset");
        equal(-1L, result.nextOffset(), "terminal next offset");
        equal(0, result.bytesWritten(), "terminal count");
    }

    private static void expectIllegalArgument(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException");
    }

    private static void command(DesktopCommand actual, String name,
            Map<String, Object> payload) {
        equal(name, actual.name(), "command name");
        equal(payload, actual.payload(), "command arguments");
    }

    private static void equal(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected=" + expected + " actual=" + actual);
        }
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
            DesktopCommandResult next = results.pollFirst();
            if (next == null) {
                throw new AssertionError("Unexpected command " + command.name());
            }
            return next;
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
