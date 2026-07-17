package dev.vatori.desktop;

public final class DesktopWindow {
    private final String id;
    private final String title;
    private final int width;
    private final int height;
    private final String route;

    private DesktopWindow(Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.width = builder.width;
        this.height = builder.height;
        this.route = builder.route;
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

    public String route() {
        return route;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private String title = "Vaadin Desktop";
        private int width = 1100;
        private int height = 760;
        private String route = "";

        private Builder(String id) {
            this.id = requireWindowId(id);
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

        public Builder route(String route) {
            this.route = requireRelativeRoute(route);
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

        private static String requireWindowId(String value) {
            String id = requireText(value, "Window id");
            if (id.length() > 128 || !id.matches("[A-Za-z0-9._:-]+")) {
                throw new IllegalArgumentException(
                        "Window id must contain only letters, digits, '.', '_', ':' or '-'");
            }
            return id;
        }

        private static String requireRelativeRoute(String value) {
            if (value == null) {
                throw new IllegalArgumentException("Window route must not be null");
            }
            if (value.isEmpty()) {
                return value;
            }
            if (value.startsWith("/") || value.startsWith("~")
                    || value.contains("\\") || value.contains("://")) {
                throw new IllegalArgumentException("Window route must be relative");
            }
            for (String segment : value.split("/", -1)) {
                if (segment.isBlank() || segment.equals(".") || segment.equals("..")) {
                    throw new IllegalArgumentException(
                            "Window route must not contain empty or traversal segments");
                }
            }
            return value;
        }
    }
}
