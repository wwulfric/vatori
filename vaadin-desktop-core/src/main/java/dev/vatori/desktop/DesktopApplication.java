package dev.vatori.desktop;

import java.util.LinkedHashMap;
import java.util.Map;

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
            this.name = name;
        }

        public Builder mainWindow(DesktopWindow mainWindow) {
            this.mainWindow = mainWindow;
            return this;
        }

        public Builder metadata(String key, String value) {
            metadata.put(key, value);
            return this;
        }

        public DesktopApplication build() {
            return new DesktopApplication(this);
        }
    }
}

