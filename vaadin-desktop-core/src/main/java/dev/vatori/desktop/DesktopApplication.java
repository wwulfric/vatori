package dev.vatori.desktop;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class DesktopApplication {
    private final String name;
    private final DesktopWindow mainWindow;
    private final Map<String, String> metadata;

    private DesktopApplication(Builder builder) {
        this.name = builder.name;
        this.mainWindow = builder.mainWindow;
        this.metadata = Map.copyOf(builder.metadata);
    }

    public String name() {
        return name;
    }

    public DesktopWindow mainWindow() {
        return mainWindow;
    }

    public Map<String, String> metadata() {
        return metadata;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static final class Builder {
        private final String name;
        private DesktopWindow mainWindow = DesktopWindow.builder("main").build();
        private final Map<String, String> metadata = new LinkedHashMap<>();

        private Builder(String name) {
            this.name = requireText(name, "Application name");
        }

        public Builder mainWindow(DesktopWindow mainWindow) {
            this.mainWindow = Objects.requireNonNull(mainWindow, "mainWindow");
            return this;
        }

        public Builder metadata(String key, String value) {
            metadata.put(
                    requireText(key, "Metadata key"),
                    Objects.requireNonNull(value, "Metadata value"));
            return this;
        }

        public DesktopApplication build() {
            return new DesktopApplication(this);
        }

        private static String requireText(String value, String name) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(name + " must not be blank");
            }
            return value;
        }
    }
}
