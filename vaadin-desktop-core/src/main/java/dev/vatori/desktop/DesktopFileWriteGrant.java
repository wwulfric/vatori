package dev.vatori.desktop;

import java.util.Base64;
import java.util.Map;
import java.util.Objects;

/**
 * An owner-bound, process-lifetime grant for a newly created file. The host
 * retains the selected URL and opaque identity. Writes modify the file
 * immediately; neither release nor owner teardown rolls back or deletes it.
 * This handle provides no automatic retry, resume, overwrite or fsync guarantee.
 */
public final class DesktopFileWriteGrant implements AutoCloseable {
    static final int MAX_WRITE_BYTES = 65_536;

    private final NativeBridge nativeBridge;
    private final String grantId;
    private final String filename;
    private final int maxWriteBytes;
    private final boolean securityScoped;
    private boolean released;
    private DesktopCommandResult successfulReleaseResult;
    private DesktopFileWriteCancelResult successfulCancelResult;

    DesktopFileWriteGrant(NativeBridge nativeBridge, String grantId,
            String filename, int maxWriteBytes, boolean securityScoped) {
        this.nativeBridge = Objects.requireNonNull(nativeBridge, "nativeBridge");
        this.grantId = Objects.requireNonNull(grantId, "grantId");
        this.filename = Objects.requireNonNull(filename, "filename");
        this.maxWriteBytes = maxWriteBytes;
        this.securityScoped = securityScoped;
    }

    public String filename() {
        return filename;
    }

    public int maxWriteBytes() {
        return maxWriteBytes;
    }

    public boolean securityScoped() {
        return securityScoped;
    }

    /**
     * Writes one bounded block at the exact host-maintained next offset.
     * Negative, stale and nonsequential offsets are validated by the host.
     * Calls after release still reach the host to establish unavailability.
     * An I/O error may report a written prefix and invalidates the host grant.
     * Cancellation can report a shorter written prefix. A write that has
     * already finished remains successful even if cancellation closes the
     * grant before its reply arrives.
     *
     * @throws IllegalArgumentException if bytes is null, empty or exceeds the
     *         grant's bounded write size; no command is sent in these cases
     */
    public DesktopFileWriteResult write(long offset, byte[] bytes) {
        if (bytes == null || bytes.length == 0
                || bytes.length > maxWriteBytes
                || bytes.length > MAX_WRITE_BYTES) {
            throw new IllegalArgumentException(
                    "Write block must contain 1 to " + maxWriteBytes + " bytes");
        }
        byte[] snapshot = bytes.clone();
        DesktopCommandResult result = nativeBridge.invoke(new DesktopCommand(
                "fileGrant.write", Map.of(
                        "grantId", grantId,
                        "offset", offset,
                        "bodyBase64", Base64.getEncoder().encodeToString(snapshot))));
        return DesktopFileWriteResult.from(result, offset, snapshot.length,
                maxWriteBytes);
    }

    /**
     * Closes the host handle without deleting or rolling back the file. A
     * successful release is settled locally on subsequent calls.
     */
    public synchronized DesktopFileGrantReleaseResult release() {
        if (released) {
            return DesktopFileGrantReleaseResult.alreadyReleased(
                    successfulReleaseResult);
        }
        DesktopFileGrantReleaseResult result = DesktopFileGrantReleaseResult.from(
                nativeBridge.invoke(new DesktopCommand("fileGrant.releaseWrite",
                        Map.of("grantId", grantId))));
        if (result.outcome() == DesktopFileGrantReleaseResult.Outcome.RELEASED) {
            released = true;
            successfulReleaseResult = result.commandResult();
        }
        return result;
    }

    /**
     * Requests cooperative cancellation of this grant and waits for its native
     * handle to close. The returned next offset is the confirmed total written
     * prefix, not a rollback point. The file is never deleted or truncated.
     *
     * <p>The worker stops when it observes cancellation at a write checkpoint.
     * An attempt that has passed that checkpoint may still finish; this does
     * not interrupt a system call or promise a bounded completion time. Call
     * from a worker thread when the UI must remain responsive. An interrupted
     * wait or lost reply is not proof that cancellation completed or that no
     * bytes were written, and is not automatically retried.</p>
     *
     * <p>A confirmed cancellation is retained for repeated calls on this
     * facade. Cancellation after release still reaches the host, as does
     * release or write after cancellation.</p>
     */
    public synchronized DesktopFileWriteCancelResult cancel() {
        if (successfulCancelResult != null) {
            return DesktopFileWriteCancelResult.alreadyCanceled(
                    successfulCancelResult);
        }
        DesktopFileWriteCancelResult result = DesktopFileWriteCancelResult.from(
                nativeBridge.invoke(new DesktopCommand("fileGrant.cancelWrite",
                        Map.of("grantId", grantId))));
        if (result.outcome() == DesktopFileWriteCancelResult.Outcome.CANCELED) {
            successfulCancelResult = result;
        }
        return result;
    }

    @Override
    public synchronized void close() {
        if (successfulCancelResult == null) {
            release();
        }
    }
}
