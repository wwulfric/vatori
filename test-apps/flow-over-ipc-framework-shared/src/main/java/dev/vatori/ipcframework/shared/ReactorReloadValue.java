package dev.vatori.ipcframework.shared;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class ReactorReloadValue {
    private ReactorReloadValue() {
    }

    public static String revision() {
        try (InputStream input = ReactorReloadValue.class
                .getResourceAsStream("/vatori-reactor-reload.txt")) {
            if (input == null) {
                throw new IllegalStateException(
                        "Reactor reload resource is missing");
            }
            return new String(
                    input.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Cannot read reactor reload resource", exception);
        }
    }
}
