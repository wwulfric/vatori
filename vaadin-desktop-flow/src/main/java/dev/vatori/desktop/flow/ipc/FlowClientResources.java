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
    private static final String FLOW_CLIENT_JS = "META-INF/frontend/FlowClient.js";
    private static final String BIG_DECIMAL_FIELD_JS =
            "META-INF/resources/frontend/vaadin-big-decimal-field.js";
    private static final String LIT_SHIM_JS = "META-INF/vatori/frontend-shims/lit.js";
    private static final String TEXT_FIELD_SHIM_JS =
            "META-INF/vatori/frontend-shims/vaadin-text-field.js";
    private static final String CHECKBOX_SHIM_JS =
            "META-INF/vatori/frontend-shims/vaadin-checkbox.js";
    private static final String DEFINE_SHIM_JS =
            "META-INF/vatori/frontend-shims/component-base-define.js";
    private static final String FLOW_CLIENT_IPC_BRIDGE_NEEDLE = """
            j.sendEventMessage=mI(function(a,b,c){var d=i.a.Z();d.Ab(a,b,c)});j.initializing=false;\
            """;
    private static final String FLOW_CLIENT_IPC_BRIDGE_REPLACEMENT = """
            j.sendEventMessage=mI(function(a,b,c){var d=i.a.Z();d.Ab(a,b,c)});j.handleIpcMessage=mI(function(a){var b=typeof a==='string'?JSON.parse(a):a;Pr(i.a.X(),b)});j.requestIpcResync=mI(function(){Cs(Ic(wk(i.a,tf),16))});j.initializing=false;\
            """;
    private static final String COMPONENT_TRANSITIVE_BLOCKED_REASON =
            "full @vaadin/text-field npm graph is not bundled in the v1.2 no-localhost smoke runtime";
    private static final String CHECKBOX_TRANSITIVE_BLOCKED_REASON =
            "full @vaadin/checkbox npm graph is not bundled in the v1.3 no-localhost smoke runtime";
    private static final String LIT_SHIM_SOURCE = """
            export function css(strings, ...values) {
              const raw = strings && strings.raw ? strings.raw : strings;
              let text = '';
              for (let index = 0; index < raw.length; index += 1) {
                text += raw[index];
                if (index < values.length) {
                  text += values[index];
                }
              }
              return {
                cssText: text,
                toString() {
                  return text;
                }
              };
            }
            """;
    private static final String DEFINE_SHIM_SOURCE = """
            export function defineCustomElement(elementClass) {
              const tag = elementClass && elementClass.is;
              if (tag && !customElements.get(tag)) {
                customElements.define(tag, elementClass);
              }
            }
            """;
    private static final String TEXT_FIELD_SHIM_SOURCE = """
            export class TextField extends HTMLElement {
              static get is() {
                return 'vaadin-text-field';
              }

              static get lumoInjector() {
                return {};
              }

              static get styles() {
                return [];
              }

              constructor() {
                super();
                this._value = '';
                this.inputElement = document.createElement('input');
                this.inputElement.part = 'input-field';
                this.inputElement.addEventListener('input', () => {
                  this.value = this.inputElement.value;
                  this.dispatchEvent(new CustomEvent('value-changed', {
                    bubbles: true,
                    composed: true,
                    detail: { value: this.value }
                  }));
                });
                this.inputElement.addEventListener('change', () => {
                  this.value = this.inputElement.value;
                  this.dispatchEvent(new Event('change', {
                    bubbles: true,
                    composed: true
                  }));
                });
              }

              connectedCallback() {
                if (!this.inputElement.isConnected) {
                  this.appendChild(this.inputElement);
                }
                if (!this.__vatoriReadyDone && typeof this.ready === 'function') {
                  this.__vatoriReadyDone = true;
                  this.ready();
                }
              }

              ready() {
              }

              set value(value) {
                this._value = value == null ? '' : String(value);
                if (this.inputElement && this.inputElement.value !== this._value) {
                  this.inputElement.value = this._value;
                }
              }

              get value() {
                return this.inputElement ? this.inputElement.value : this._value;
              }

              set placeholder(value) {
                if (this.inputElement) {
                  this.inputElement.placeholder = value == null ? '' : String(value);
                }
              }

              get placeholder() {
                return this.inputElement ? this.inputElement.placeholder : '';
              }
            }

            if (!customElements.get(TextField.is)) {
              customElements.define(TextField.is, TextField);
            }
            """;
    private static final String CHECKBOX_SHIM_SOURCE = """
            export class Checkbox extends HTMLElement {
              static get is() {
                return 'vaadin-checkbox';
              }

              constructor() {
                super();
                this._checked = false;
                this.inputElement = document.createElement('input');
                this.inputElement.type = 'checkbox';
                this.inputElement.addEventListener('change', () => {
                  this.checked = this.inputElement.checked;
                  this.dispatchEvent(new CustomEvent('checked-changed', {
                    bubbles: true,
                    composed: true,
                    detail: { value: this.checked }
                  }));
                  this.dispatchEvent(new Event('change', {
                    bubbles: true,
                    composed: true
                  }));
                });
              }

              connectedCallback() {
                if (!this.inputElement.isConnected) {
                  this.appendChild(this.inputElement);
                }
              }

              set checked(value) {
                this._checked = value === true || value === 'true';
                if (this.inputElement) {
                  this.inputElement.checked = this._checked;
                }
              }

              get checked() {
                return this.inputElement ? this.inputElement.checked : this._checked;
              }
            }

            if (!customElements.get(Checkbox.is)) {
              customElements.define(Checkbox.is, Checkbox);
            }
            """;

    private final Map<String, Resource> flowClientResources;
    private final Map<String, Resource> componentResources;
    private final Map<String, Resource> allResources;

    FlowClientResources() {
        Map<String, Resource> discoveredFlowClientResources = new LinkedHashMap<>();
        add(discoveredFlowClientResources, "META-INF/frontend/FlowBootstrap.js", "flow-bootstrap-module", true);
        add(discoveredFlowClientResources, FLOW_CLIENT_JS, "flow-client-module", true);
        add(discoveredFlowClientResources, "META-INF/frontend/Flow.js", "flow-module", true);
        add(discoveredFlowClientResources, "META-INF/frontend/index.js", "flow-index-module", true);
        add(discoveredFlowClientResources, COMPILE_PROPERTIES, "flow-client-compile-properties", true);
        String gwtClient = gwtClientResource();
        if (gwtClient != null) {
            add(discoveredFlowClientResources, gwtClient, "flow-client-gwt-bundle", true);
        }

        Map<String, Resource> discoveredComponentResources = new LinkedHashMap<>();
        add(discoveredComponentResources, BIG_DECIMAL_FIELD_JS, "vaadin-web-component-module", true,
                componentAttributes(false,
                        "com.vaadin.flow.component.textfield.BigDecimalField",
                        "vaadin-big-decimal-field",
                        "./vaadin-big-decimal-field.js",
                        "classpath-frontend-resource"));
        addText(discoveredComponentResources, LIT_SHIM_JS, "vaadin-web-component-import-shim", LIT_SHIM_SOURCE,
                componentAttributes(true, "lit", "", "lit", "v1_2-inline-shim"));
        addText(discoveredComponentResources, TEXT_FIELD_SHIM_JS, "vaadin-web-component-import-shim",
                TEXT_FIELD_SHIM_SOURCE,
                componentAttributes(true, "@vaadin/text-field", "vaadin-text-field",
                        "@vaadin/text-field/src/vaadin-text-field.js", "v1_2-inline-shim"));
        addText(discoveredComponentResources, CHECKBOX_SHIM_JS, "vaadin-web-component-npm-shim",
                CHECKBOX_SHIM_SOURCE,
                componentAttributes(true, "com.vaadin.flow.component.checkbox.Checkbox", "vaadin-checkbox",
                        "@vaadin/checkbox/src/vaadin-checkbox.js", "v1_3-npm-shim"));
        addText(discoveredComponentResources, DEFINE_SHIM_JS, "vaadin-web-component-import-shim",
                DEFINE_SHIM_SOURCE,
                componentAttributes(true, "@vaadin/component-base", "",
                        "@vaadin/component-base/src/define.js", "v1_2-inline-shim"));

        flowClientResources = Collections.unmodifiableMap(new LinkedHashMap<>(discoveredFlowClientResources));
        componentResources = Collections.unmodifiableMap(new LinkedHashMap<>(discoveredComponentResources));
        Map<String, Resource> combined = new LinkedHashMap<>();
        combined.putAll(flowClientResources);
        combined.putAll(componentResources);
        allResources = Collections.unmodifiableMap(combined);
    }

    Map<String, Object> manifest() {
        List<Map<String, Object>> entries = new ArrayList<>();
        for (Resource resource : flowClientResources.values()) {
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

    Map<String, Object> componentManifest() {
        List<Map<String, Object>> entries = new ArrayList<>();
        for (Resource resource : componentResources.values()) {
            entries.add(resource.metadata());
        }
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("source", "vaadin-component-classpath-plus-v1_2-shims");
        manifest.put("transport", "flow-ipc");
        manifest.put("resourceLoading", "ipc-resource-request");
        manifest.put("componentClass", "com.vaadin.flow.component.textfield.BigDecimalField");
        manifest.put("componentTag", "vaadin-big-decimal-field");
        manifest.put("primaryResource", BIG_DECIMAL_FIELD_JS);
        manifest.put("resolverStrategy", "generic-component-frontend-manifest-v1_3");
        manifest.put("componentCount", 2);
        manifest.put("componentTags", List.of("vaadin-big-decimal-field", "vaadin-checkbox"));
        manifest.put("primaryResources", List.of(BIG_DECIMAL_FIELD_JS, CHECKBOX_SHIM_JS));
        manifest.put("components", List.of(
                Map.of("componentClass", "com.vaadin.flow.component.textfield.BigDecimalField",
                        "componentTag", "vaadin-big-decimal-field",
                        "primaryResource", BIG_DECIMAL_FIELD_JS,
                        "resourceOrigin", "classpath-frontend-resource",
                        "productionBundleReady", false),
                Map.of("componentClass", "com.vaadin.flow.component.checkbox.Checkbox",
                        "componentTag", "vaadin-checkbox",
                        "primaryResource", CHECKBOX_SHIM_JS,
                        "requestedModule", "@vaadin/checkbox/src/vaadin-checkbox.js",
                        "resourceOrigin", "npm-frontend-shim",
                        "productionBundleReady", false)));
        manifest.put("requestedComponent", "com.vaadin.flow.component.textfield.TextField");
        manifest.put("substituteReason", "TextField class is available but its npm frontend graph is not classpath-resolved in this smoke runtime; BigDecimalField provides a real TextField-family classpath frontend module.");
        manifest.put("classpath", true);
        manifest.put("npmFrontendGraphComplete", false);
        manifest.put("productionBundleReady", false);
        manifest.put("devModeReady", false);
        manifest.put("failureClassification", "npm-transitive-imports-shimmed");
        manifest.put("blockedReason", COMPONENT_TRANSITIVE_BLOCKED_REASON);
        manifest.put("unresolvedImports", List.of(
                Map.of("specifier", "lit",
                        "classification", "npm-frontend-artifact-missing",
                        "resolution", LIT_SHIM_JS),
                Map.of("specifier", "@vaadin/text-field/src/vaadin-text-field.js",
                        "classification", "npm-frontend-artifact-missing",
                        "resolution", TEXT_FIELD_SHIM_JS),
                Map.of("specifier", "@vaadin/checkbox/src/vaadin-checkbox.js",
                        "classification", "npm-frontend-artifact-missing",
                        "resolution", CHECKBOX_SHIM_JS),
                Map.of("specifier", "@vaadin/component-base/src/define.js",
                        "classification", "npm-frontend-artifact-missing",
                        "resolution", DEFINE_SHIM_JS)));
        manifest.put("count", entries.size());
        manifest.put("resources", entries);
        return manifest;
    }

    Map<String, Object> response(String path) {
        Resource resource = allResources.get(path);
        if (resource == null) {
            throw new IllegalArgumentException("Unknown Flow IPC resource: " + path);
        }
        Map<String, Object> response = resource.metadata();
        response.put("available", true);
        response.put("encoding", resource.text ? "utf-8" : "base64-not-implemented");
        if (resource.text) {
            response.put("body", new String(resource.bytes, StandardCharsets.UTF_8));
        }
        return response;
    }

    boolean contains(String path) {
        return allResources.containsKey(path);
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
        add(target, path, kind, text, Map.of("flowClientResource", true));
    }

    private void add(Map<String, Resource> target, String path, String kind, boolean text, Map<String, Object> attributes) {
        Resource resource = read(path, kind, text, attributes);
        if (resource != null) {
            target.put(path, resource);
        }
    }

    private void addText(Map<String, Resource> target, String path, String kind, String body,
            Map<String, Object> attributes) {
        target.put(path, new Resource(path, kind, body.getBytes(StandardCharsets.UTF_8), true, false, attributes));
    }

    private Resource read(String path, String kind, boolean text) {
        return read(path, kind, text, Map.of("flowClientResource", true));
    }

    private Resource read(String path, String kind, boolean text, Map<String, Object> attributes) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = FlowClientResources.class.getClassLoader();
        }
        try (InputStream stream = loader.getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            byte[] bytes = stream.readAllBytes();
            boolean ipcAugmented = false;
            if (FLOW_CLIENT_JS.equals(path)) {
                String source = new String(bytes, StandardCharsets.UTF_8);
                if (!source.contains(FLOW_CLIENT_IPC_BRIDGE_NEEDLE)) {
                    throw new IllegalStateException("Unable to install Flow IPC bridge into FlowClient.js");
                }
                source = source.replace(FLOW_CLIENT_IPC_BRIDGE_NEEDLE, FLOW_CLIENT_IPC_BRIDGE_REPLACEMENT);
                bytes = source.getBytes(StandardCharsets.UTF_8);
                ipcAugmented = true;
            }
            return new Resource(path, kind, bytes, text, ipcAugmented, attributes);
        } catch (IOException error) {
            throw new IllegalStateException("Unable to read Flow client resource " + path, error);
        }
    }

    private static Map<String, Object> componentAttributes(boolean shim, String componentClass, String tag,
            String moduleSpecifier, String resolver) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("componentFrontendResource", true);
        attributes.put("componentClass", componentClass);
        if (!tag.isBlank()) {
            attributes.put("componentTag", tag);
        }
        attributes.put("moduleSpecifier", moduleSpecifier);
        attributes.put("resolver", resolver);
        attributes.put("shim", shim);
        attributes.put("failureClassification", "npm-transitive-imports-shimmed");
        attributes.put("blockedReason", "v1_3-npm-shim".equals(resolver)
                ? CHECKBOX_TRANSITIVE_BLOCKED_REASON
                : COMPONENT_TRANSITIVE_BLOCKED_REASON);
        attributes.put("lookupKind", lookupKind(componentClass, resolver));
        if (!shim) {
            attributes.put("shimmedImports", List.of("lit",
                    "@vaadin/text-field/src/vaadin-text-field.js",
                    "@vaadin/component-base/src/define.js"));
        }
        if ("v1_3-npm-shim".equals(resolver)) {
            attributes.put("componentPrimary", true);
        }
        if ("classpath-frontend-resource".equals(resolver)) {
            attributes.put("componentPrimary", true);
        }
        return attributes;
    }

    private static String lookupKind(String componentClass, String resolver) {
        if ("classpath-frontend-resource".equals(resolver)
                && "com.vaadin.flow.component.textfield.BigDecimalField".equals(componentClass)) {
            return "BigDecimalField-minimum-component-resource";
        }
        if ("v1_3-npm-shim".equals(resolver)
                && "com.vaadin.flow.component.checkbox.Checkbox".equals(componentClass)) {
            return "Checkbox-component-resource-v1_3";
        }
        return "component-transitive-import-shim";
    }

    private record Resource(String path, String kind, byte[] bytes, boolean text, boolean ipcAugmented,
            Map<String, Object> attributes) {
        private Resource {
            Objects.requireNonNull(path, "path");
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(bytes, "bytes");
            attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        }

        Map<String, Object> metadata() {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("path", path);
            metadata.put("kind", kind);
            metadata.put("bytes", bytes.length);
            metadata.put("sha256", sha256(bytes));
            metadata.put("text", text);
            metadata.put("ipcAugmented", ipcAugmented);
            metadata.putAll(attributes);
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
