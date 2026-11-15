package dev.vatori.desktop;

import java.util.Map;
import java.util.Objects;

/** Native runtime lifecycle queries bound to the current capability owner. */
public final class DesktopRuntime {
    private final NativeBridge bridge;

    DesktopRuntime(NativeBridge bridge) {
        this.bridge = Objects.requireNonNull(bridge, "bridge");
    }

    /**
     * Queries the latest host-held replacement record for this native window
     * lifetime. Requires {@code runtime.replacement.status} permission.
     * A successful payload has {@code status}: {@code none}, {@code pending},
     * {@code replaced}, {@code rolled-back}, {@code rollback-failed},
     * {@code retirement-failed}, or {@code interrupted}. A record also includes
     * {@code operationId}, {@code requestId}, {@code candidateId}, and
     * {@code requestingGeneration}. A successful query is not proof that a
     * replacement succeeded. Records do not survive host exit or window close.
     *
     * <p>A fresh host returns {@code none} until it admits a product request.
     * Development CLI replacement is not recorded as product replacement.</p>
     */
    public DesktopCommandResult replacementStatus() {
        return bridge.invoke(new DesktopCommand("runtime.replacement.status", Map.of()));
    }

    /**
     * Requests a host-declared candidate, under the explicit
     * {@code runtime.replacement.request} manifest permission. Use a unique
     * request ID per intended operation; matching retries in the same owner
     * and generation do not start another replacement. The receipt may be
     * lost when this JVM exits. Query {@link #replacementStatus()} from the
     * new runtime for the host-held outcome. Java heap/UI state is not retained.
     * Neither argument is an executable path. Admission is not completion.
     */
    public DesktopCommandResult requestReplacement(String requestId, String candidateId) {
        requireIdentifier(requestId);
        requireIdentifier(candidateId);
        return bridge.invoke(new DesktopCommand("runtime.replacement.request",
                Map.of("requestId", requestId, "candidateId", candidateId)));
    }

    private static void requireIdentifier(String value) {
        Objects.requireNonNull(value, "identifier");
        if (!value.matches("[A-Za-z0-9._:-]{1,128}")) {
            throw new IllegalArgumentException("Expected a bounded runtime identifier, not a path");
        }
    }
}
