package dev.vatori.desktop.flow.ipc;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class FlowIpcDiagnostics {
    private FlowIpcDiagnostics() {
    }

    static String redactLocation(String location) {
        if (location == null || location.isBlank()) {
            return "";
        }
        String redacted = location;
        String userDir = Path.of("").toAbsolutePath().normalize().toString();
        if (!userDir.isBlank()) {
            redacted = redacted.replace(userDir, "$APP");
        }
        String home = System.getProperty("user.home", "");
        if (!home.isBlank()) {
            redacted = redacted.replace(home, "$HOME");
        }
        return redacted;
    }

    static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }
}
