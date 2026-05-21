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
    private static final String TEXT_FIELD_SHIM_JS =
            "META-INF/vatori/frontend-shims/vaadin-text-field.js";
    private static final String DEV_BUNDLE_COMMONJS_JS =
            "vaadin-dev-bundle/webapp/VAADIN/build/commonjsHelpers-CUmg6egw.js";
    private static final String DEV_BUNDLE_INDEX_HTML_JS =
            "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js";
    private static final String DEV_BUNDLE_STATS_JSON =
            "vaadin-dev-bundle/config/stats.json";
    private static final String DEV_BUNDLE_LIT_ENTRY_JS =
            "META-INF/vatori/dev-bundle-entry/lit-css.js";
    private static final String DEV_BUNDLE_TEXT_FIELD_ENTRY_JS =
            "META-INF/vatori/dev-bundle-entry/vaadin-text-field.js";
    private static final String DEV_BUNDLE_DEFINE_ENTRY_JS =
            "META-INF/vatori/dev-bundle-entry/component-base-define.js";
    private static final String GENERATED_FLOW_IMPORTS_JS =
            "META-INF/vatori/generated/flow/generated-flow-imports.js";
    private static final String DEV_MODE_PACKAGE_LOCK_JSON =
            "vaadin-dev-bundle/package-lock.json";
    private static final String DEV_BUNDLE_CHECKBOX_ENTRY_JS =
            "META-INF/vatori/dev-bundle-entry/vaadin-checkbox.js";
    private static final String DEV_BUNDLE_SELECT_ENTRY_JS =
            "META-INF/vatori/dev-bundle-entry/vaadin-select.js";
    private static final String THEME_STATIC_CSS =
            "META-INF/resources/frontend/themes/vatori-flow-ipc-smoke/theme-static.css";
    private static final String FRONTEND_ASSET_PIPELINE_VERSION = "flow-ipc-frontend-assets-v1_4";
    private static final String FRONTEND_ARTIFACT_GRAPH_SOURCE = "vaadin-frontend-artifact-graph-v1_4";
    private static final String COMPONENT_RESOLVER_V1_4 = "frontend-artifact-graph-v1_4";
    private static final String FLOW_CLIENT_IPC_BRIDGE_NEEDLE = """
            j.sendEventMessage=mI(function(a,b,c){var d=i.a.Z();d.Ab(a,b,c)});j.initializing=false;\
            """;
    private static final String FLOW_CLIENT_IPC_BRIDGE_REPLACEMENT = """
            j.sendEventMessage=mI(function(a,b,c){var d=i.a.Z();d.Ab(a,b,c)});j.handleIpcMessage=mI(function(a){var b=typeof a==='string'?JSON.parse(a):a;Pr(i.a.X(),b)});j.requestIpcResync=mI(function(){Cs(Ic(wk(i.a,tf),16))});j.initializing=false;\
            """;
    private static final String COMPONENT_TRANSITIVE_BLOCKED_REASON =
            "the @vaadin/text-field import is still a controlled shim; lit css and component-base define resolve through focused extracted production bundle helpers";
    private static final String DEV_BUNDLE_COMPONENT_REASON =
            "Focused Vaadin dev-bundle entries import production component classes and register real custom elements; full production bundle routing is still not complete.";
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

              set invalid(value) {
                this.toggleAttribute('invalid', Boolean(value));
              }

              get invalid() {
                return this.hasAttribute('invalid');
              }
            }
            """;
    private static final String TEXT_FIELD_DEV_BUNDLE_ENTRY_SOURCE = """
            import { aG as defineCustomElement, c7 as TextField } from './indexhtml-BfYYZi8S.js';

            defineCustomElement(TextField);

            export { defineCustomElement, TextField };
            """;
    private static final String LIT_DEV_BUNDLE_ENTRY_SOURCE = """
            const constructionToken = Symbol('vatori-lit-css-result');

            class CSSResult {
              constructor(cssText, strings, token) {
                if (token !== constructionToken) {
                  throw new Error('CSSResult is not constructable. Use `unsafeCSS` or `css` instead.');
                }
                this.cssText = cssText;
                this.t = strings;
                this._$cssResult$ = true;
              }

              get styleSheet() {
                if (!this.o && 'CSSStyleSheet' in globalThis) {
                  this.o = new CSSStyleSheet();
                  this.o.replaceSync(this.cssText);
                }
                return this.o;
              }

              toString() {
                return this.cssText;
              }
            }

            function textFromCssValue(value) {
              if (value && value._$cssResult$ === true) {
                return value.cssText;
              }
              if (typeof value === 'number') {
                return value;
              }
              throw new Error(
                "Value passed to 'css' function must be a 'css' function result. Use 'unsafeCSS' to pass non-literal values."
              );
            }

            export const unsafeCSS = (value) => new CSSResult(String(value), undefined, constructionToken);

            export const css = (strings, ...values) => {
              const raw = strings && strings.raw ? strings.raw : strings;
              const cssText = raw.length === 1
                ? raw[0]
                : values.reduce((text, value, index) => {
                    return text + textFromCssValue(value) + raw[index + 1];
                  }, raw[0]);
              return new CSSResult(cssText, raw, constructionToken);
            };
            """;
    private static final String DEFINE_DEV_BUNDLE_ENTRY_SOURCE = """
            window.Vaadin ||= {};
            window.Vaadin.featureFlags ||= {};

            function dashToCamelCase(dash) {
              return dash.replace(/-[a-z]/gu, (match) => match[1].toUpperCase());
            }

            const experimentalMap = {};

            export function defineCustomElement(CustomElement, version = '25.1.2') {
              Object.defineProperty(CustomElement, 'version', {
                get() {
                  return version;
                }
              });
              if (CustomElement.experimental) {
                const featureFlagKey = typeof CustomElement.experimental === 'string'
                  ? CustomElement.experimental
                  : `${dashToCamelCase(CustomElement.is.split('-').slice(1).join('-'))}Component`;
                if (!window.Vaadin.featureFlags[featureFlagKey] && !experimentalMap[featureFlagKey]) {
                  experimentalMap[featureFlagKey] = new Set();
                  experimentalMap[featureFlagKey].add(CustomElement);
                  Object.defineProperty(window.Vaadin.featureFlags, featureFlagKey, {
                    get() {
                      return experimentalMap[featureFlagKey].size === 0;
                    },
                    set(value) {
                      if (!!value && experimentalMap[featureFlagKey].size > 0) {
                        experimentalMap[featureFlagKey].forEach((elementClass) => {
                          customElements.define(elementClass.is, elementClass);
                        });
                        experimentalMap[featureFlagKey].clear();
                      }
                    }
                  });
                  return;
                } else if (experimentalMap[featureFlagKey]) {
                  experimentalMap[featureFlagKey].add(CustomElement);
                  return;
                }
              }
              const defined = customElements.get(CustomElement.is);
              if (!defined) {
                customElements.define(CustomElement.is, CustomElement);
              } else {
                const definedVersion = defined.version;
                if (definedVersion && CustomElement.version && definedVersion === CustomElement.version) {
                  console.warn(`The component ${CustomElement.is} has been loaded twice`);
                } else {
                  console.error(
                    `Tried to define ${CustomElement.is} version ${CustomElement.version} when version ${defined.version} is already in use. Something will probably break.`
                  );
                }
              }
            }
            """;
    private static final String CHECKBOX_DEV_BUNDLE_ENTRY_SOURCE = """
            import { aG as defineCustomElement, c5 as Checkbox } from './indexhtml-BfYYZi8S.js';

            defineCustomElement(Checkbox);

            export { Checkbox };
            """;
    private static final String SELECT_DEV_BUNDLE_ENTRY_SOURCE = """
            import { aG as defineCustomElement, c6 as Select } from './indexhtml-BfYYZi8S.js';

            defineCustomElement(Select);

            export { Select };
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
        addText(discoveredComponentResources, TEXT_FIELD_SHIM_JS, "vaadin-web-component-import-shim",
                TEXT_FIELD_SHIM_SOURCE,
                componentAttributes(true, "com.vaadin.flow.component.textfield.TextField",
                        "vaadin-text-field",
                        "@vaadin/text-field/src/vaadin-text-field.js",
                        "v1_2-inline-shim"));
        add(discoveredComponentResources, DEV_BUNDLE_COMMONJS_JS, "vaadin-dev-bundle-support-module", true,
                devBundleAttributes("", DEV_BUNDLE_COMMONJS_JS, false));
        add(discoveredComponentResources, DEV_BUNDLE_INDEX_HTML_JS, "vaadin-dev-bundle-support-module", true,
                devBundleAttributes("", DEV_BUNDLE_INDEX_HTML_JS, false));
        add(discoveredComponentResources, DEV_BUNDLE_STATS_JSON, "vaadin-production-bundle-stats", true,
                productionBundleStatsAttributes());
        addText(discoveredComponentResources, DEV_BUNDLE_LIT_ENTRY_JS,
                "vaadin-dev-bundle-lit-css-entry", LIT_DEV_BUNDLE_ENTRY_SOURCE,
                litDevBundleAttributes());
        addText(discoveredComponentResources, DEV_BUNDLE_TEXT_FIELD_ENTRY_JS,
                "vaadin-dev-bundle-text-field-entry", TEXT_FIELD_DEV_BUNDLE_ENTRY_SOURCE,
                textFieldDevBundleAttributes());
        addText(discoveredComponentResources, DEV_BUNDLE_DEFINE_ENTRY_JS,
                "vaadin-dev-bundle-define-entry", DEFINE_DEV_BUNDLE_ENTRY_SOURCE,
                defineDevBundleAttributes());
        add(discoveredComponentResources, GENERATED_FLOW_IMPORTS_JS,
                "vaadin-generated-flow-imports-module", true,
                generatedLazyAttributes());
        add(discoveredComponentResources, DEV_MODE_PACKAGE_LOCK_JSON,
                "vaadin-dev-mode-package-lock", true,
                devModePackageLockAttributes());
        addText(discoveredComponentResources, DEV_BUNDLE_CHECKBOX_ENTRY_JS,
                "vaadin-dev-bundle-checkbox-entry", CHECKBOX_DEV_BUNDLE_ENTRY_SOURCE,
                devBundleAttributes("vaadin-checkbox", DEV_BUNDLE_CHECKBOX_ENTRY_JS, true));
        addText(discoveredComponentResources, DEV_BUNDLE_SELECT_ENTRY_JS,
                "vaadin-dev-bundle-select-entry", SELECT_DEV_BUNDLE_ENTRY_SOURCE,
                devBundleAttributes("com.vaadin.flow.component.select.Select",
                        "vaadin-select",
                        DEV_BUNDLE_SELECT_ENTRY_JS,
                        true,
                        "Select-dev-bundle-resource-v1_4",
                        "com.vaadin.flow.component.select.Select"));
        add(discoveredComponentResources, THEME_STATIC_CSS, "theme-static-resource", true,
                themeStaticAttributes());

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
        manifest.put("frontendAssetPipelineVersion", FRONTEND_ASSET_PIPELINE_VERSION);
        manifest.put("artifactGraphSource", FRONTEND_ARTIFACT_GRAPH_SOURCE);
        manifest.put("artifactFamilies", List.of("flow-client-classpath"));
        manifest.put("resolverStrategy", COMPONENT_RESOLVER_V1_4);
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
        manifest.put("source", "vaadin-component-classpath-plus-dev-bundle-v1_4");
        manifest.put("frontendAssetPipelineVersion", FRONTEND_ASSET_PIPELINE_VERSION);
        manifest.put("artifactGraphSource", FRONTEND_ARTIFACT_GRAPH_SOURCE);
        manifest.put("artifactFamilies", List.of("flow-client-classpath",
                "vaadin-component-classpath-frontend",
                "npm-frontend",
                "theme-static",
                "production-bundle",
                "dev-mode-bundle",
                "generated-lazy"));
        manifest.put("artifactFamilyStatuses", List.of(
                artifactFamilyStatus("flow-client-classpath", "available", "classpath-flow-client-resource"),
                artifactFamilyStatus("vaadin-component-classpath-frontend", "available",
                        "classpath-component-frontend-resource"),
                artifactFamilyStatus("npm-frontend", "blocked", "textfield-production-class-execution-blocked"),
                artifactFamilyStatus("theme-static", "available", "theme-static-classpath-resource"),
                artifactFamilyStatus("production-bundle", "partial", "production-bundle-stats-verified"),
                artifactFamilyStatus("dev-mode-bundle", "partial", "dev-mode-package-lock-verified"),
                artifactFamilyStatus("generated-lazy", "partial", "generated-flow-imports-verified")));
        manifest.put("dependencyGraph", Map.of("declared", true,
                "resolver", COMPONENT_RESOLVER_V1_4,
                "resourceCount", entries.size()));
        manifest.put("strictBlockers", List.of(
                strictBlocker("npm-frontend", "textfield-production-class-execution-blocked",
                        COMPONENT_TRANSITIVE_BLOCKED_REASON),
                strictBlocker("dev-mode-bundle", "dev-mode-bundle-execution-unmapped",
                        "dev-mode package-lock dependency graph is verified over IPC, but Vite/dev-server module execution is not yet mapped into the no-localhost resolver"),
                strictBlocker("generated-lazy", "generated-lazy-execution-unmapped",
                        "generated-flow-imports is verified over IPC, but its transitive npm/jar imports and loadOnDemand execution are not yet mapped")));
        manifest.put("transport", "flow-ipc");
        manifest.put("resourceLoading", "ipc-resource-request");
        manifest.put("componentClass", "com.vaadin.flow.component.textfield.BigDecimalField");
        manifest.put("componentTag", "vaadin-big-decimal-field");
        manifest.put("primaryResource", BIG_DECIMAL_FIELD_JS);
        manifest.put("resolverStrategy", "generic-component-frontend-manifest-v1_3");
        manifest.put("componentCount", 3);
        manifest.put("componentTags", List.of("vaadin-big-decimal-field", "vaadin-checkbox", "vaadin-select"));
        manifest.put("primaryResources", List.of(BIG_DECIMAL_FIELD_JS,
                DEV_BUNDLE_CHECKBOX_ENTRY_JS,
                DEV_BUNDLE_SELECT_ENTRY_JS));
        manifest.put("components", List.of(
                Map.of("componentClass", "com.vaadin.flow.component.textfield.BigDecimalField",
                        "componentTag", "vaadin-big-decimal-field",
                        "primaryResource", BIG_DECIMAL_FIELD_JS,
                        "resourceOrigin", "classpath-frontend-resource",
                        "productionBundleReady", false),
                Map.of("componentClass", "com.vaadin.flow.component.checkbox.Checkbox",
                        "componentTag", "vaadin-checkbox",
                        "primaryResource", DEV_BUNDLE_CHECKBOX_ENTRY_JS,
                        "requestedModule", "@vaadin/checkbox/src/vaadin-checkbox.js",
                        "resourceOrigin", "vaadin-dev-bundle-checkbox-entry",
                        "productionBundleReady", true),
                Map.of("componentClass", "com.vaadin.flow.component.select.Select",
                        "componentTag", "vaadin-select",
                        "primaryResource", DEV_BUNDLE_SELECT_ENTRY_JS,
                        "requestedModule", "@vaadin/select/src/vaadin-select.js",
                        "resourceOrigin", "vaadin-dev-bundle-select-entry",
                        "productionBundleReady", true)));
        manifest.put("requestedComponent", "com.vaadin.flow.component.textfield.TextField");
        manifest.put("substituteReason", "TextField class is available but its npm frontend graph is not classpath-resolved in this smoke runtime; BigDecimalField provides a real TextField-family classpath frontend module.");
        manifest.put("classpath", true);
        manifest.put("npmFrontendGraphComplete", false);
        manifest.put("productionBundleReady", false);
        manifest.put("devModeReady", false);
        manifest.put("failureClassification", "textfield-production-class-execution-blocked");
        manifest.put("blockedReason", COMPONENT_TRANSITIVE_BLOCKED_REASON);
        manifest.put("unresolvedImports", List.of(
                Map.of("specifier", "@vaadin/text-field/src/vaadin-text-field.js",
                        "classification", "textfield-production-class-execution-blocked",
                        "resolution", TEXT_FIELD_SHIM_JS)));
        manifest.put("resolvedImports", List.of(
                Map.of("specifier", "lit",
                        "classification", "focused-extracted-production-bundle-helper",
                        "resolution", DEV_BUNDLE_LIT_ENTRY_JS),
                Map.of("specifier", "@vaadin/component-base/src/define.js",
                        "classification", "focused-extracted-production-bundle-helper",
                        "resolution", DEV_BUNDLE_DEFINE_ENTRY_JS)));
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
        add(target, path, kind, text, flowClientAttributes(path, kind));
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
        return read(path, kind, text, flowClientAttributes(path, kind));
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
        attributes.put("artifactFamily", shim ? "npm-frontend" : "vaadin-component-classpath-frontend");
        attributes.put("origin", shim ? "inline-diagnostic-shim" : "classpath");
        attributes.put("resourceOrigin", shim ? "inline-diagnostic-shim" : "classpath-frontend-resource");
        attributes.put("dependencyOf", BIG_DECIMAL_FIELD_JS);
        attributes.put("executionPolicy", shim ? "blocked-shim-fallback" : "execute-primary-module");
        attributes.put("artifactStatus", shim ? "blocked-fallback" : "available");
        attributes.put("requestedBy", "component-frontend-manifest-v1_4");
        attributes.put("failureClassification", "textfield-production-class-execution-blocked");
        attributes.put("blockedReason", COMPONENT_TRANSITIVE_BLOCKED_REASON);
        attributes.put("lookupKind", lookupKind(componentClass, resolver));
        if (!shim) {
            attributes.put("shimmedImports", List.of("@vaadin/text-field/src/vaadin-text-field.js"));
            attributes.put("importRewriteStrategy", "manifest-import-rewrite-v1_4");
            attributes.put("browserExecutionPlan", "dynamic-import-blob-with-manifest-import-rewrites-v1_4");
            attributes.put("importRewrites", List.of(
                    importRewrite("lit",
                            DEV_BUNDLE_LIT_ENTRY_JS,
                            "production-bundle",
                            "vaadin-dev-bundle-production-build-v1_4",
                            "dev-bundle-resource-graph-partial"),
                    importRewrite("@vaadin/text-field/src/vaadin-text-field.js",
                            TEXT_FIELD_SHIM_JS,
                            "npm-frontend",
                            "manifest-import-rewrite-v1_4",
                            "textfield-production-class-execution-blocked"),
                    importRewrite("@vaadin/component-base/src/define.js",
                            DEV_BUNDLE_DEFINE_ENTRY_JS,
                            "production-bundle",
                            "vaadin-dev-bundle-production-build-v1_4",
                            "dev-bundle-resource-graph-partial")));
        }
        if ("classpath-frontend-resource".equals(resolver)) {
            attributes.put("componentPrimary", true);
        }
        return attributes;
    }

    private static Map<String, Object> devBundleAttributes(String tag, String moduleSpecifier, boolean primary) {
        return devBundleAttributes("com.vaadin.flow.component.checkbox.Checkbox",
                tag,
                moduleSpecifier,
                primary,
                primary ? "Checkbox-dev-bundle-resource-v1_4" : "Checkbox-dev-bundle-support-resource-v1_4",
                primary ? "com.vaadin.flow.component.checkbox.Checkbox" : DEV_BUNDLE_CHECKBOX_ENTRY_JS);
    }

    private static Map<String, Object> devBundleAttributes(String componentClass, String tag, String moduleSpecifier,
            boolean primary, String lookupKind, String dependencyOf) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("componentFrontendResource", true);
        attributes.put("componentClass", componentClass);
        if (!tag.isBlank()) {
            attributes.put("componentTag", tag);
        }
        attributes.put("moduleSpecifier", moduleSpecifier);
        attributes.put("resolver", "vaadin-dev-bundle-production-build-v1_4");
        attributes.put("shim", false);
        attributes.put("artifactFamily", "production-bundle");
        attributes.put("origin", primary ? "vaadin-dev-bundle-focused-entry" : "vaadin-dev-bundle");
        attributes.put("resourceOrigin", "vaadin-dev-bundle");
        attributes.put("dependencyOf", dependencyOf);
        attributes.put("executionPolicy", primary ? "execute-dev-bundle-entry" : "dependency-module");
        attributes.put("artifactStatus", "partial");
        attributes.put("requestedBy", "component-frontend-manifest-v1_4");
        attributes.put("productionBundleReady", true);
        attributes.put("devBundleResource", true);
        attributes.put("blockedReason", DEV_BUNDLE_COMPONENT_REASON);
        attributes.put("failureClassification", "dev-bundle-resource-graph-partial");
        attributes.put("lookupKind", lookupKind);
        if (primary) {
            attributes.put("componentPrimary", true);
        } else {
            attributes.put("componentBundleDependency", true);
        }
        return attributes;
    }

    private static Map<String, Object> litDevBundleAttributes() {
        Map<String, Object> attributes = devBundleAttributes("lit",
                "",
                DEV_BUNDLE_LIT_ENTRY_JS,
                false,
                "Lit-css-dev-bundle-resource-v1_4",
                BIG_DECIMAL_FIELD_JS);
        attributes.put("replacesImportSpecifiers", List.of("lit"));
        attributes.put("origin", "vaadin-dev-bundle-extracted-entry");
        attributes.put("resourceOrigin", "vaadin-dev-bundle-extracted-lit-css");
        attributes.put("blockedReason",
                "Focused Lit css helper is extracted from the production dev-bundle without executing the full indexhtml chunk; full npm frontend graph remains incomplete because TextField still uses a controlled shim.");
        return attributes;
    }

    private static Map<String, Object> textFieldDevBundleAttributes() {
        Map<String, Object> attributes = devBundleAttributes("com.vaadin.flow.component.textfield.TextField",
                "vaadin-text-field",
                DEV_BUNDLE_TEXT_FIELD_ENTRY_JS,
                false,
                "TextField-dev-bundle-resource-v1_4",
                BIG_DECIMAL_FIELD_JS);
        attributes.put("blockedImportSpecifiers", List.of("@vaadin/text-field/src/vaadin-text-field.js"));
        attributes.put("blockedExecutionProbe", "big-decimal-import-rewrite-to-textfield-dev-bundle");
        attributes.put("blockedExecutionError", "null is not an object (evaluating 'outlet._root')");
        attributes.put("nextResolutionStep", "stage or isolate the TextField npm/frontend graph without executing the full indexhtml application chunk before FlowClient.init");
        attributes.put("blockedReason",
                "Focused TextField production class entry is verified, but BigDecimalField execution still needs full TextField graph parity; replacing the current shim with this entry triggers the recorded WebView execution blocker.");
        return attributes;
    }

    private static Map<String, Object> defineDevBundleAttributes() {
        Map<String, Object> attributes = devBundleAttributes("@vaadin/component-base/src/define.js",
                "",
                DEV_BUNDLE_DEFINE_ENTRY_JS,
                false,
                "Component-base-define-dev-bundle-resource-v1_4",
                BIG_DECIMAL_FIELD_JS);
        attributes.put("replacesImportSpecifiers", List.of("@vaadin/component-base/src/define.js"));
        attributes.put("origin", "vaadin-dev-bundle-extracted-entry");
        attributes.put("resourceOrigin", "vaadin-dev-bundle-extracted-define");
        return attributes;
    }

    private static Map<String, Object> productionBundleStatsAttributes() {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("componentFrontendResource", true);
        attributes.put("productionBundleResource", true);
        attributes.put("productionBundleStatsGraph", true);
        attributes.put("moduleSpecifier", DEV_BUNDLE_STATS_JSON);
        attributes.put("resolver", "vaadin-production-bundle-stats-v1_4");
        attributes.put("shim", false);
        attributes.put("artifactFamily", "production-bundle");
        attributes.put("origin", "vaadin-dev-bundle");
        attributes.put("resourceOrigin", "vaadin-dev-bundle-stats");
        attributes.put("dependencyOf", "production-bundle-frontend-build");
        attributes.put("executionPolicy", "verified-only-intentionally-not-executed");
        attributes.put("artifactStatus", "available");
        attributes.put("requestedBy", "component-frontend-manifest-v1_4");
        attributes.put("failureClassification", "production-bundle-stats-verified");
        attributes.put("blockedReason",
                "verified Vaadin production bundle stats graph; full production bundle execution remains partial");
        attributes.put("lookupKind", "Production-bundle-stats-resource-v1_4");
        attributes.put("productionBundleExecutionReady", false);
        return attributes;
    }

    private static Map<String, Object> generatedLazyAttributes() {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("componentFrontendResource", true);
        attributes.put("generatedLazyResource", true);
        attributes.put("moduleSpecifier", GENERATED_FLOW_IMPORTS_JS);
        attributes.put("resolver", "repository-generated-flow-imports-v1_4");
        attributes.put("shim", false);
        attributes.put("artifactFamily", "generated-lazy");
        attributes.put("origin", "repository-generated-flow-imports");
        attributes.put("resourceOrigin", "repository-generated-flow-imports-fixture");
        attributes.put("dependencyOf", "Flow.loadOnDemand");
        attributes.put("executionPolicy", "verified-only-intentionally-not-executed");
        attributes.put("artifactStatus", "available");
        attributes.put("requestedBy", "component-frontend-manifest-v1_4");
        attributes.put("failureClassification", "generated-flow-imports-verified");
        attributes.put("blockedReason",
                "verified generated Flow imports resource; full loadOnDemand execution remains unmapped");
        attributes.put("lookupKind", "Generated-flow-imports-resource-v1_4");
        return attributes;
    }

    private static Map<String, Object> devModePackageLockAttributes() {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("componentFrontendResource", true);
        attributes.put("devModeBundleResource", true);
        attributes.put("devModePackageGraph", true);
        attributes.put("packageManager", "npm");
        attributes.put("moduleSpecifier", DEV_MODE_PACKAGE_LOCK_JSON);
        attributes.put("resolver", "vaadin-dev-bundle-package-graph-v1_4");
        attributes.put("shim", false);
        attributes.put("artifactFamily", "dev-mode-bundle");
        attributes.put("origin", "vaadin-dev-bundle");
        attributes.put("resourceOrigin", "vaadin-dev-bundle-package-lock");
        attributes.put("dependencyOf", "dev-mode-frontend-build");
        attributes.put("executionPolicy", "verified-only-intentionally-not-executed");
        attributes.put("artifactStatus", "available");
        attributes.put("requestedBy", "component-frontend-manifest-v1_4");
        attributes.put("failureClassification", "dev-mode-package-lock-verified");
        attributes.put("blockedReason",
                "verified Vaadin dev-mode package-lock dependency graph; Vite/dev-server module execution remains unmapped");
        attributes.put("lookupKind", "Dev-mode-package-lock-resource-v1_4");
        attributes.put("devModeExecutionReady", false);
        return attributes;
    }

    private static Map<String, Object> themeStaticAttributes() {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("componentFrontendResource", true);
        attributes.put("themeStaticResource", true);
        attributes.put("themeName", "vatori-flow-ipc-smoke");
        attributes.put("moduleSpecifier", THEME_STATIC_CSS);
        attributes.put("resolver", "theme-static-classpath-resource-v1_4");
        attributes.put("shim", false);
        attributes.put("artifactFamily", "theme-static");
        attributes.put("origin", "classpath");
        attributes.put("resourceOrigin", "theme-static-classpath-resource");
        attributes.put("dependencyOf", "theme:vatori-flow-ipc-smoke");
        attributes.put("executionPolicy", "verify-and-inject-static-style");
        attributes.put("artifactStatus", "available");
        attributes.put("requestedBy", "component-frontend-manifest-v1_4");
        attributes.put("failureClassification", "theme-static-classpath-resource");
        attributes.put("blockedReason", "resolved theme/static classpath resource for the v1.4 no-localhost lookup path");
        attributes.put("lookupKind", "Theme-static-resource-v1_4");
        return attributes;
    }

    private static String lookupKind(String componentClass, String resolver) {
        if ("classpath-frontend-resource".equals(resolver)
                && "com.vaadin.flow.component.textfield.BigDecimalField".equals(componentClass)) {
            return "BigDecimalField-minimum-component-resource";
        }
        return "component-transitive-import-shim";
    }

    private static Map<String, Object> importRewrite(String specifier, String resourcePath, String artifactFamily,
            String resolver, String failureClassification) {
        return Map.of("specifier", specifier,
                "resourcePath", resourcePath,
                "artifactFamily", artifactFamily,
                "resolver", resolver,
                "failureClassification", failureClassification);
    }

    private static Map<String, Object> flowClientAttributes(String path, String kind) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("flowClientResource", true);
        attributes.put("artifactFamily", "flow-client-classpath");
        attributes.put("origin", "classpath");
        attributes.put("resourceOrigin", "flow-client-classpath");
        attributes.put("moduleSpecifier", path);
        attributes.put("resolver", "flow-client-classpath-resource");
        attributes.put("executionPolicy", flowClientExecutionPolicy(kind));
        attributes.put("artifactStatus", "available");
        attributes.put("requestedBy", "flow-client-manifest-v1_4");
        return attributes;
    }

    private static String flowClientExecutionPolicy(String kind) {
        if ("flow-bootstrap-module".equals(kind) || "flow-client-module".equals(kind)) {
            return "execute-primary-module";
        }
        return "verified-only-intentionally-not-executed";
    }

    private static Map<String, Object> artifactFamilyStatus(String family, String status, String classification) {
        return Map.of("artifactFamily", family,
                "status", status,
                "failureClassification", classification);
    }

    private static Map<String, Object> strictBlocker(String family, String classification, String reason) {
        return Map.of("artifactFamily", family,
                "status", "blocked",
                "strictBlocker", true,
                "failureClassification", classification,
                "blockedReason", reason);
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
