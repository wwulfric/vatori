package dev.vatori.desktop.flow.ipc;

import java.nio.file.Files;
import java.nio.file.Path;

enum FlowFrontendMode {
    PRODUCTION_BUNDLE("production-bundle", "standard-production-bundle", true),
    DEVELOPMENT_SERVER("development-server", "vite-development-server", false);

    private static final String ENVIRONMENT_NAME =
            "VAADIN_DESKTOP_FLOW_FRONTEND_MODE";
    private static final String PROJECT_DIRECTORY_ENVIRONMENT_NAME =
            "VAADIN_DESKTOP_FLOW_PROJECT_DIRECTORY";

    private final String environmentValue;
    private final String resourceLoading;
    private final boolean productionMode;

    FlowFrontendMode(
            String environmentValue,
            String resourceLoading,
            boolean productionMode) {
        this.environmentValue = environmentValue;
        this.resourceLoading = resourceLoading;
        this.productionMode = productionMode;
    }

    static FlowFrontendMode current() {
        String configured = System.getenv(ENVIRONMENT_NAME);
        if (configured == null || configured.isBlank()) {
            return PRODUCTION_BUNDLE;
        }
        for (FlowFrontendMode mode : values()) {
            if (mode.environmentValue.equals(configured)) {
                return mode;
            }
        }
        throw new IllegalStateException(
                "Unsupported " + ENVIRONMENT_NAME + ": " + configured);
    }

    String resourceLoading() {
        return resourceLoading;
    }

    boolean productionMode() {
        return productionMode;
    }

    Path projectDirectory() {
        if (productionMode) {
            return null;
        }
        String configured = System.getenv(PROJECT_DIRECTORY_ENVIRONMENT_NAME);
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException(
                    PROJECT_DIRECTORY_ENVIRONMENT_NAME
                            + " is required for the development frontend");
        }
        Path directory = Path.of(configured).toAbsolutePath().normalize();
        if (!Files.isDirectory(directory)) {
            throw new IllegalStateException(
                    "Development project directory does not exist: "
                            + directory);
        }
        return directory;
    }
}
