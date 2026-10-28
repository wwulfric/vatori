package dev.vatori.desktop;

import java.util.Map;
import java.util.Objects;

/**
 * An owner-bound, read-only native file grant. The opaque grant identity is
 * intentionally not exposed by this API.
 */
public final class DesktopFileReadGrant implements AutoCloseable {
    private final NativeBridge nativeBridge;
    private final String grantId;
    private final String filename;
    private final long byteCount;
    private final int maxReadBytes;
    private final boolean securityScoped;

    private boolean released;
    private DesktopCommandResult successfulReleaseResult;

    DesktopFileReadGrant(NativeBridge nativeBridge, String grantId,
            String filename, long byteCount, int maxReadBytes,
            boolean securityScoped) {
        this.nativeBridge = Objects.requireNonNull(nativeBridge, "nativeBridge");
        this.grantId = Objects.requireNonNull(grantId, "grantId");
        this.filename = Objects.requireNonNull(filename, "filename");
        this.byteCount = byteCount;
        this.maxReadBytes = maxReadBytes;
        this.securityScoped = securityScoped;
    }

    public String filename() {
        return filename;
    }

    public long byteCount() {
        return byteCount;
    }

    public int maxReadBytes() {
        return maxReadBytes;
    }

    public boolean securityScoped() {
        return securityScoped;
    }

    /**
     * Reads one host-bounded range. Validation and grant availability remain a
     * host responsibility, including after this handle has been released.
     */
    public DesktopFileReadResult read(long offset, int maxBytes) {
        DesktopCommandResult result = nativeBridge.invoke(new DesktopCommand(
                "fileGrant.read", Map.of(
                        "grantId", grantId,
                        "offset", offset,
                        "maxBytes", maxBytes)));
        return DesktopFileReadResult.from(result, offset, maxBytes,
                maxReadBytes);
    }

    /**
     * Releases this grant. After one successful host release, later calls are
     * settled locally as {@code ALREADY_RELEASED}.
     */
    public synchronized DesktopFileGrantReleaseResult release() {
        if (released) {
            return DesktopFileGrantReleaseResult.alreadyReleased(
                    successfulReleaseResult);
        }
        DesktopCommandResult commandResult = nativeBridge.invoke(
                new DesktopCommand("fileGrant.release",
                        Map.of("grantId", grantId)));
        DesktopFileGrantReleaseResult result =
                DesktopFileGrantReleaseResult.from(commandResult);
        if (result.outcome()
                == DesktopFileGrantReleaseResult.Outcome.RELEASED) {
            released = true;
            successfulReleaseResult = result.commandResult();
        }
        return result;
    }

    @Override
    public void close() {
        release();
    }
}
