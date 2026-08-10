package dev.vatori.desktop;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Read-only application resources staged by the platform packager. */
public final class DesktopResources {
    private static final String RESOURCE_DIRECTORY_PROPERTY =
            "vatori.application.resources";
    private static final String RESOURCE_DIRECTORY_ENVIRONMENT =
            "VATORI_APPLICATION_RESOURCES";

    private final Path directory;

    private DesktopResources(Path directory) {
        this.directory = directory;
    }

    static DesktopResources fromLaunchEnvironment() {
        String configured = System.getProperty(RESOURCE_DIRECTORY_PROPERTY);
        if (configured == null || configured.isBlank()) {
            configured = System.getenv(RESOURCE_DIRECTORY_ENVIRONMENT);
        }
        Path directory = configured == null || configured.isBlank()
                ? null
                : Path.of(configured).toAbsolutePath().normalize();
        return new DesktopResources(directory);
    }

    public boolean isAvailable() {
        return directory != null && Files.isDirectory(directory);
    }

    public Path directory() {
        if (!isAvailable()) {
            throw new IllegalStateException(
                    "No application resources were bundled for this desktop application");
        }
        return directory;
    }

    public Path resolve(String relativePath) {
        Path root = directory();
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("Application resource path must not be blank");
        }
        String portablePath = relativePath.replace('\\', '/');
        String[] segments = portablePath.split("/", -1);
        if (portablePath.startsWith("/") || portablePath.startsWith("~")) {
            throw invalidPath(relativePath);
        }
        Path candidate = root;
        for (String segment : segments) {
            if (segment.isBlank() || segment.equals(".") || segment.equals("..")) {
                throw invalidPath(relativePath);
            }
            candidate = candidate.resolve(segment);
        }
        candidate = candidate.normalize();
        if (!candidate.startsWith(root) || !Files.exists(candidate)) {
            throw new IllegalArgumentException(
                    "Bundled application resource does not exist: " + relativePath);
        }
        return candidate;
    }

    public InputStream open(String relativePath) throws IOException {
        Path resource = resolve(relativePath);
        if (!Files.isRegularFile(resource)) {
            throw new IOException(
                    "Bundled application resource is not a file: " + relativePath);
        }
        return Files.newInputStream(resource);
    }

    private static IllegalArgumentException invalidPath(String path) {
        return new IllegalArgumentException(
                "Application resource path must be relative and cannot contain empty, '.' or '..' segments: "
                        + path);
    }
}
