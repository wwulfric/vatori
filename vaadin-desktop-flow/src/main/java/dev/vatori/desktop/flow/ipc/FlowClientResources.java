package dev.vatori.desktop.flow.ipc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

final class FlowClientResources {
    private static final String COMPILE_PROPERTIES =
            "META-INF/resources/VAADIN/static/client/compile.properties";

    private final Map<String, Resource> resources;

    FlowClientResources() {
        Map<String, Resource> discovered = new LinkedHashMap<>();
        add(discovered, "META-INF/frontend/FlowBootstrap.js", "flow-bootstrap-module", true);
        add(discovered, "META-INF/frontend/FlowClient.js", "flow-client-module", true);
        add(discovered, "META-INF/frontend/Flow.js", "flow-module", true);
        add(discovered, "META-INF/frontend/index.js", "flow-index-module", true);
        add(discovered, COMPILE_PROPERTIES, "flow-client-compile-properties", true);
        String gwtClient = gwtClientResource();
        if (gwtClient != null) {
            add(discovered, gwtClient, "flow-client-gwt-bundle", true);
        }
        resources = Collections.unmodifiableMap(new LinkedHashMap<>(discovered));
    }

    Map<String, Object> manifest() {
        List<Map<String, Object>> entries = new ArrayList<>();
        for (Resource resource : resources.values()) {
            entries.add(resource.metadata());
        }
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("source", "flow-client-classpath");
        manifest.put("transport", "flow-ipc");
        manifest.put("resourceLoading", "ipc-resource-request");
        manifest.put("classpath", true);
        manifest.put("count", entries.size());
        manifest.put("resources", entries);
        return manifest;
    }

    Map<String, Object> response(String path) {
        Resource resource = resources.get(path);
        if (resource == null) {
            throw new IllegalArgumentException("Unknown Flow client resource: " + path);
        }
        Map<String, Object> response = resource.metadata();
        response.put("flowClientResource", true);
        response.put("available", true);
        response.put("encoding", resource.text ? "utf-8" : "base64-not-implemented");
        if (resource.text) {
            response.put("body", new String(resource.bytes, StandardCharsets.UTF_8));
        }
        return response;
    }

    boolean contains(String path) {
        return resources.containsKey(path);
    }

    private String gwtClientResource() {
        Resource compileProperties = read(COMPILE_PROPERTIES, "flow-client-compile-properties", true);
        if (compileProperties == null) {
            return null;
        }
        Properties properties = new Properties();
        try {
            properties.load(new java.io.ByteArrayInputStream(compileProperties.bytes));
        } catch (IOException error) {
            throw new IllegalStateException("Unable to read Flow client compile.properties", error);
        }
        String jsFile = properties.getProperty("jsFile");
        if (jsFile == null || jsFile.isBlank()) {
            return null;
        }
        return "META-INF/resources/VAADIN/static/client/" + jsFile;
    }

    private void add(Map<String, Resource> target, String path, String kind, boolean text) {
        Resource resource = read(path, kind, text);
        if (resource != null) {
            target.put(path, resource);
        }
    }

    private Resource read(String path, String kind, boolean text) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = FlowClientResources.class.getClassLoader();
        }
        try (InputStream stream = loader.getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            return new Resource(path, kind, stream.readAllBytes(), text);
        } catch (IOException error) {
            throw new IllegalStateException("Unable to read Flow client resource " + path, error);
        }
    }

    private record Resource(String path, String kind, byte[] bytes, boolean text) {
        private Resource {
            Objects.requireNonNull(path, "path");
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(bytes, "bytes");
        }

        Map<String, Object> metadata() {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("path", path);
            metadata.put("kind", kind);
            metadata.put("bytes", bytes.length);
            metadata.put("sha256", sha256(bytes));
            metadata.put("text", text);
            return metadata;
        }

        private static String sha256(byte[] value) {
            try {
                return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
            } catch (NoSuchAlgorithmException error) {
                throw new IllegalStateException("SHA-256 is not available", error);
            }
        }
    }
}
