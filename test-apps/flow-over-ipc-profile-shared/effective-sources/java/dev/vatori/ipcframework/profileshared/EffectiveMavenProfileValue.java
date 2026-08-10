package dev.vatori.ipcframework.profileshared;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class EffectiveMavenProfileValue {
    private static final String SOURCE_REVISION = "baseline";

    private EffectiveMavenProfileValue() {
    }

    public static String sourceRevision() {
        return SOURCE_REVISION;
    }

    public static String resourceRevision() {
        try (InputStream input = EffectiveMavenProfileValue.class
                .getResourceAsStream("/vatori-effective-maven-profile.txt")) {
            if (input == null) {
                return "missing";
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    .trim();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Cannot read the effective Maven profile resource",
                    exception);
        }
    }
}
