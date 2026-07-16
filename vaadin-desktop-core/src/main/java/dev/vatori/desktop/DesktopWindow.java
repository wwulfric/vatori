package dev.vatori.desktop;

public final class DesktopWindow {
    private final String id;
    private final String title;
    private final int width;
    private final int height;

    private DesktopWindow(Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.width = builder.width;
        this.height = builder.height;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private String title = "Vaadin Desktop";
        private int width = 1100;
        private int height = 760;

        private Builder(String id) {
            this.id = requireText(id, "Window id");
        }

        public Builder title(String title) {
            this.title = requireText(title, "Window title");
            return this;
        }

        public Builder size(int width, int height) {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException(
                        "Window width and height must be positive");
            }
            this.width = width;
            this.height = height;
            return this;
        }

        public DesktopWindow build() {
            return new DesktopWindow(this);
        }

        private static String requireText(String value, String name) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(name + " must not be blank");
            }
            return value;
        }
    }
}
