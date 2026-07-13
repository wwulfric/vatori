package dev.vatori.desktop.flow.ipc;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class FlowIpcClasspathResources {
    private FlowIpcClasspathResources() {
    }

    static String text(String path) {
        try (InputStream stream = classLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return "";
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalStateException("Unable to read Flow client resource " + path, error);
        }
    }

    static byte[] bytes(String path) {
        if (path == null || path.isBlank()) {
            return new byte[0];
        }
        try (InputStream stream = classLoader().getResourceAsStream(path)) {
            return stream == null ? new byte[0] : stream.readAllBytes();
        } catch (IOException error) {
            throw new IllegalStateException("Unable to read Flow IPC resource " + path, error);
        }
    }

    static ArtifactEvidence artifactEvidence(String path) {
        URL url = resourceUrl(path);
        if (url == null) {
            return new ArtifactEvidence("missing", "", "", 0L);
        }
        String location = url.toExternalForm();
        return new ArtifactEvidence(artifactSource(location),
                FlowIpcDiagnostics.redactLocation(location),
                url.getProtocol(),
                artifactLastModified(url));
    }

    private static URL resourceUrl(String path) {
        return classLoader().getResource(path);
    }

    private static ClassLoader classLoader() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return loader == null ? FlowIpcClasspathResources.class.getClassLoader() : loader;
    }

    private static String artifactSource(String location) {
        if (location.contains("/com/vaadin/vaadin-dev-bundle/")
                || (location.contains("vaadin-dev-bundle-") && location.contains(".jar!"))) {
            return "static-dev-bundle";
        }
        if (location.contains("/target/classes/") && location.contains("/vaadin-dev-bundle/")) {
            return "copied";
        }
        if (location.contains("/target/")) {
            return "current-build";
        }
        return "unknown";
    }

    private static long artifactLastModified(URL url) {
        try {
            if ("file".equals(url.getProtocol())) {
                return Files.getLastModifiedTime(Path.of(url.toURI())).toMillis();
            }
            if ("jar".equals(url.getProtocol())) {
                String text = url.toExternalForm();
                int separator = text.indexOf("!/");
                if (text.startsWith("jar:") && separator > 4) {
                    URL jarUrl = URI.create(text.substring(4, separator)).toURL();
                    if ("file".equals(jarUrl.getProtocol())) {
                        return Files.getLastModifiedTime(Path.of(jarUrl.toURI())).toMillis();
                    }
                }
            }
            return 0L;
        } catch (IOException | URISyntaxException | IllegalArgumentException error) {
            return 0L;
        }
    }

    record ArtifactEvidence(
            String source,
            String runtimePath,
            String protocol,
            long lastModifiedMillis) {
    }
}
