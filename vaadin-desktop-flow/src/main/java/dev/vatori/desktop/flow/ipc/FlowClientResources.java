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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final String DEV_BUNDLE_GENERATED_FLOW_IMPORTS_JS =
            "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js";
    private static final String DEV_BUNDLE_MARKDOWN_HELPERS_JS =
            "vaadin-dev-bundle/webapp/VAADIN/build/markdown-helpers-31I0A-hg.js";
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
    private static final String FLOW_LAZY_DEPENDENCY_JS =
            "vatori-flow-ipc-lazy-v1_10.js";
    private static final String FRONTEND_ASSET_PIPELINE_VERSION = "flow-ipc-frontend-assets-v1_4";
    private static final String FRONTEND_ARTIFACT_GRAPH_SOURCE = "vaadin-frontend-artifact-graph-v1_4";
    private static final String COMPONENT_RESOLVER_V1_4 = "frontend-artifact-graph-v1_4";
    private static final String RUNTIME_RESOLVER_CONTRACT_VERSION =
            "flow-ipc-frontend-runtime-resolver-v1_10";
    private static final String TEXT_FIELD_GRAPH_PARITY_VERSION = "flow-ipc-textfield-graph-v1_6";
    private static final String TEXT_FIELD_PREVIOUS_BLOCKER_VERSION = "flow-ipc-textfield-v1_6";
    private static final String TEXT_FIELD_EXECUTION_VERSION = "flow-ipc-textfield-v1_7";
    private static final String TEXT_FIELD_SIDE_EFFECT_VERSION = "flow-ipc-textfield-side-effect-v1_7";
    private static final String TEXT_FIELD_GRAPH_PROBE = "direct-textfield-dev-bundle-entry-module-identity-v1_7";
    private static final String TEXT_FIELD_IMPORT_RESOLUTION_STRATEGY =
            "stable-resource-path-blob-module-identity-v1_7";
    private static final String TEXT_FIELD_SIDE_EFFECT_STRATEGY =
            "reuse-production-indexhtml-module-identity-before-textfield-entry-import";
    private static final String TEXT_FIELD_NARROWED_FAILURE_CLASSIFICATION =
            "textfield-production-indexhtml-side-effect-deduped";
    private static final String TEXT_FIELD_FAILURE_LAYER =
            "resolved-v1_6-custom-element-registry-conflict";
    private static final String TEXT_FIELD_CLASS_EXPORT_ARTIFACT = DEV_BUNDLE_INDEX_HTML_JS;
    private static final String TEXT_FIELD_FIRST_DEFINITION_SOURCE =
            "production indexhtml stable module identity used by component graph";
    private static final String TEXT_FIELD_SECOND_DEFINITION_SOURCE =
            "TextField entry reuses cached production indexhtml module and only calls guarded defineCustomElement";
    private static final String TEXT_FIELD_DUPLICATE_TAG = "vaadin-text-field";
    private static final String TEXT_FIELD_REMAINING_BLOCKER_CLASSIFICATION = "none";
    private static final String TEXT_FIELD_MINIMAL_REPRODUCTION =
            "stage META-INF/vatori/dev-bundle-entry/vaadin-text-field.js, rewrite ./indexhtml-BfYYZi8S.js to the stable staged dependency already used by the component graph, import the entry, and verify the registry no longer throws duplicate definition";
    private static final String TEXT_FIELD_NEXT_RESOLUTION_STEP =
            "replace the retained Blob module identity with a custom-scheme or file-backed resolver that preserves the same one-module-per-artifact identity without keeping large Blob URLs for the full window lifetime";
    private static final String GENERATED_LAZY_RESOLVER_VERSION = "flow-ipc-generated-lazy-resolver-v1_8";
    private static final String GENERATED_LAZY_EXECUTION_VERSION = "flow-ipc-generated-lazy-v1_8";
    private static final String GENERATED_LAZY_IMPORT_RESOLUTION_STRATEGY =
            "stable-relative-import-rewrite-generated-lazy-v1_8";
    private static final String GENERATED_LAZY_GRAPH_PROBE =
            "direct-generated-flow-imports-dev-bundle-chunk-v1_8";
    private static final String GENERATED_LAZY_LOAD_ON_DEMAND_CONTRACT_VERSION =
            "flow-ipc-loadondemand-v1_9";
    private static final String GENERATED_LAZY_LOAD_ON_DEMAND_TRIGGER_SOURCE =
            "diagnostic-preflight-and-direct-staged-import";
    private static final String GENERATED_LAZY_LOAD_ON_DEMAND_API_OWNER =
            "window.Vaadin.Flow.loadOnDemand";
    private static final String GENERATED_LAZY_LOAD_ON_DEMAND_API_INSTALL_SOURCE =
            "generated-flow-imports-dev-bundle-after-direct-staged-import";
    private static final String GENERATED_LAZY_LOAD_ON_DEMAND_REQUESTED_CHUNK =
            "vatori-flow-ipc-generated-lazy-v1_9-probe";
    private static final String GENERATED_LAZY_LOAD_ON_DEMAND_REGISTRY_SOURCE =
            "no-bootstrap-lazy-registry-observed";
    private static final String GENERATED_LAZY_LOAD_ON_DEMAND_MAPPING_SOURCE =
            "component-frontend-manifest-generated-lazy-v1_9";
    private static final String GENERATED_LAZY_BLOCKER_VERSION = "flow-ipc-loadondemand-v1_9";
    private static final String GENERATED_LAZY_RELATIVE_IMPORT_SPECIFIER = "./markdown-helpers-31I0A-hg.js";
    private static final String GENERATED_LAZY_FAILURE_LAYER =
            "loadondemand-api-installed-by-generated-imports-not-flow-client-triggered";
    private static final String GENERATED_LAZY_NEXT_RESOLUTION_STEP =
            "discover the Flow client bootstrap lazy registry or supported Vaadin SPI that maps real lazy keys to generated-lazy artifacts without direct staged import";
    private static final String GENERATED_LAZY_CHUNK_CONTRACT_VERSION = "flow-ipc-loadondemand-chunk-v1_11";
    private static final String GENERATED_LAZY_CHUNK_EXPRESSION_SOURCE = "bootstrap-uidl-dependencies";
    private static final String GENERATED_LAZY_CHUNK_TRIGGER_OWNER = "visible-flow-client-dependencyloader";
    private static final String GENERATED_LAZY_CHUNK_REGISTRY_SOURCE =
            "uidl-expression-to-dev-bundle-generated-flow-imports-registry-v1_11";
    private static final String GENERATED_LAZY_CHUNK_MAPPING_SOURCE =
            "flow-ipc-generated-loadondemand-chunk-map-v1_11";
    private static final String GENERATED_LAZY_CHUNK_API_INSTALL_SOURCE =
            "flow-ipc-loadondemand-bridge-before-generated-imports";
    private static final String GENERATED_LAZY_CHUNK_FAILURE_LAYER =
            "dev-bundle-generated-imports-loadondemand-no-op-no-chunk-file";
    private static final String GENERATED_LAZY_CHUNK_BLOCKER_VERSION = "flow-ipc-loadondemand-chunk-v1_11";
    private static final String GENERATED_LAZY_CHUNK_NEXT_RESOLUTION_STEP =
            "derive chunk-id-to-file mapping from generated-flow-imports or stats.json instead of staging only the generated imports module";
    private static final String GENERATED_LAZY_CHUNK_FILE_REGISTRY_VERSION =
            "flow-ipc-loadondemand-chunk-file-v1_12";
    private static final String GENERATED_LAZY_CHUNK_FILE_REGISTRY_SOURCE =
            "generated-flow-imports-body-parser-v1_12";
    private static final String GENERATED_LAZY_CHUNK_FILE_MAPPING_SOURCE =
            "real-generated-flow-imports-loadondemand-table-v1_12";
    private static final String GENERATED_LAZY_CHUNK_FILE_RESOLVER =
            "generated-flow-imports-relative-chunk-file-resolver-v1_12";
    private static final String GENERATED_LAZY_CHUNK_FILE_BLOCKER_VERSION =
            "flow-ipc-loadondemand-chunk-file-v1_12";
    private static final String GENERATED_LAZY_CHUNK_FILE_NOT_GENERATED_REASON =
            "real generated-flow-imports loadOnDemand table is a Promise.resolve(0) no-op; this Vaadin build did not generate a ./chunks/chunk-*.js branch for the current app";
    private static final String GENERATED_LAZY_CHUNK_FILE_NEXT_RESOLUTION_STEP =
            "make the smoke app produce a real Vaadin generated lazy chunk or route the next build's ./chunks/chunk-*.js branch through this resolver";
    private static final Pattern GENERATED_LAZY_CHUNK_IMPORT =
            Pattern.compile("key\\s*={2,3}\\s*['\"]([^'\"]+)['\"][\\s\\S]*?pending\\.push\\(import\\(['\"](\\./chunks/[^'\"]+\\.js)['\"]\\)\\)");
    private static final Pattern GENERATED_LAZY_NOOP_LOAD_ON_DEMAND =
            Pattern.compile("const\\s+loadOnDemand\\s*=\\s*\\([^)]*\\)\\s*=>\\s*\\{\\s*return\\s+Promise\\.resolve\\(0\\);?\\s*\\}");
    private static final String FLOW_LAZY_DEPENDENCY_VERSION = "flow-ipc-lazy-dependency-v1_10";
    private static final String FLOW_LAZY_DEPENDENCY_SOURCE =
            "flow-uidl-dependencyloader-loadmode-lazy-v1_10";
    private static final String FLOW_LAZY_DEPENDENCY_EXPRESSION =
            "context://vatori-flow-ipc-lazy-v1_10.js";
    private static final String FLOW_LAZY_DEPENDENCY_TRIGGER_OWNER =
            "visible-flow-client-dependencyloader";
    private static final String FLOW_LAZY_DEPENDENCY_RESOLVER =
            "flow-ipc-dependencyloader-resource-resolver-v1_10";
    private static final String FLOW_LAZY_DEPENDENCY_BODY = """
            window.VaadinDesktopIpcLazyDependencyProbe = {
              executed: true,
              source: 'flow-uidl-dependencyloader-loadmode-lazy-v1_10',
              mode: 'LoadMode.LAZY',
              path: 'vatori-flow-ipc-lazy-v1_10.js'
            };
            window.dispatchEvent(new CustomEvent('vatori-flow-ipc-lazy-dependency-v1_10', {
              detail: window.VaadinDesktopIpcLazyDependencyProbe
            }));
            """;
    private static final String PRODUCTION_RUNTIME_VERSION = "flow-ipc-production-runtime-v1_10";
    private static final String PRODUCTION_RUNTIME_REGISTRY_SOURCE =
            "vaadin-dev-bundle-stats-runtime-registry-v1_10";
    private static final String PRODUCTION_RUNTIME_DEPENDENCY_CHAIN =
            DEV_BUNDLE_STATS_JSON + "=>bundleImports=>VAADIN/build/indexhtml-BfYYZi8S.js=>"
                    + DEV_BUNDLE_INDEX_HTML_JS;
    private static final String PRODUCTION_RUNTIME_MODULE_IDENTITY =
            "stable-resource-path-blob-module-identity-v1_7:" + DEV_BUNDLE_INDEX_HTML_JS;
    private static final String PRODUCTION_RUNTIME_BLOCKER =
            "flow-ipc-production-runtime-v1_10";
    private static final String PRODUCTION_RUNTIME_NEXT_STEP =
            "promote the stats-selected production bundle entry from staged execution-attempt to an owned module runtime that can execute side effects without relying on focused component entry imports";
    private static final String PRODUCTION_RUNTIME_OWNER_VERSION =
            "flow-ipc-production-owner-v1_13";
    private static final String PRODUCTION_RUNTIME_OWNER =
            "flow-ipc-production-bundle-runtime";
    private static final String PRODUCTION_RUNTIME_OWNER_SOURCE =
            "vaadin-dev-bundle-stats";
    private static final String PRODUCTION_RUNTIME_OWNER_SELECTED_BY =
            "stats-entrypoint-or-focused-production-entry";
    private static final String PRODUCTION_RUNTIME_OWNER_MODULE_IDENTITY =
            "production-owner-v1_13:" + DEV_BUNDLE_INDEX_HTML_JS;
    private static final String PRODUCTION_RUNTIME_OWNER_LIFECYCLE =
            "created,staged,import-skipped,side-effect-unobservable,not-reused,cleanup-owned";
    private static final String PRODUCTION_RUNTIME_OWNER_EXECUTION_POLICY =
            "stage-owner-contract-production-bundle-runtime-v1_13";
    private static final String PRODUCTION_RUNTIME_OWNER_BLOCKER =
            "flow-ipc-production-owner-v1_13";
    private static final String PRODUCTION_RUNTIME_OWNER_FAILURE_LAYER =
            "production-owner-runtime-contract-missing";
    private static final String PRODUCTION_RUNTIME_OWNER_SIDE_EFFECT_PROOF =
            "owner-stage-resolved-import-intentionally-disabled-until-production-runtime-contract-can-own-indexhtml-side-effects";
    private static final String PRODUCTION_RUNTIME_OWNER_NEXT_STEP =
            "unify the production owner module identity with component focused dependency imports or move the owner to a custom-scheme/file-backed runtime before enabling indexhtml side effects";
    private static final String FLOW_CLIENT_IPC_BRIDGE_NEEDLE = """
            j.sendEventMessage=mI(function(a,b,c){var d=i.a.Z();d.Ab(a,b,c)});j.initializing=false;\
            """;
    private static final String FLOW_CLIENT_IPC_BRIDGE_REPLACEMENT = """
            j.sendEventMessage=mI(function(a,b,c){var d=i.a.Z();d.Ab(a,b,c)});j.handleIpcMessage=mI(function(a){var b=typeof a==='string'?JSON.parse(a):a;Pr(i.a.X(),b)});j.requestIpcResync=mI(function(){Cs(Ic(wk(i.a,tf),16))});j.initializing=false;\
            """;
    private static final String COMPONENT_TRANSITIVE_BLOCKED_REASON =
            "the @vaadin/text-field import resolves to the real focused TextField dev-bundle entry; the legacy shim remains only as a regression fallback and is not counted as the v1.7 success path";
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
        add(discoveredComponentResources, DEV_BUNDLE_GENERATED_FLOW_IMPORTS_JS,
                "vaadin-dev-bundle-generated-flow-imports-chunk", true,
                generatedLazyDevBundleChunkAttributes(DEV_BUNDLE_GENERATED_FLOW_IMPORTS_JS,
                        "Generated-flow-imports-dev-bundle-chunk-v1_4",
                        "Flow.loadOnDemand",
                        true));
        add(discoveredComponentResources, DEV_BUNDLE_MARKDOWN_HELPERS_JS,
                "vaadin-dev-bundle-generated-flow-imports-dependency", true,
                generatedLazyDevBundleChunkAttributes(DEV_BUNDLE_MARKDOWN_HELPERS_JS,
                        "Generated-flow-imports-dev-bundle-dependency-v1_4",
                        DEV_BUNDLE_GENERATED_FLOW_IMPORTS_JS,
                        false));
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
        addText(discoveredComponentResources, FLOW_LAZY_DEPENDENCY_JS,
                "flow-lazy-dependency-script", FLOW_LAZY_DEPENDENCY_BODY,
                flowLazyDependencyAttributes());

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
        manifest.put("textfieldGraphParityVersion", TEXT_FIELD_GRAPH_PARITY_VERSION);
        manifest.put("textfieldPreviousBlockerVersion", TEXT_FIELD_PREVIOUS_BLOCKER_VERSION);
        manifest.put("textfieldExecutionVersion", TEXT_FIELD_EXECUTION_VERSION);
        manifest.put("textfieldSideEffectVersion", TEXT_FIELD_SIDE_EFFECT_VERSION);
        manifest.put("textfieldGraphProbe", TEXT_FIELD_GRAPH_PROBE);
        manifest.put("textfieldImportResolutionStrategy", TEXT_FIELD_IMPORT_RESOLUTION_STRATEGY);
        manifest.put("textfieldSideEffectStrategy", TEXT_FIELD_SIDE_EFFECT_STRATEGY);
        manifest.put("textfieldNarrowedFailureClassification", TEXT_FIELD_NARROWED_FAILURE_CLASSIFICATION);
        manifest.put("textfieldFailureLayer", TEXT_FIELD_FAILURE_LAYER);
        manifest.put("textfieldEntryArtifact", DEV_BUNDLE_TEXT_FIELD_ENTRY_JS);
        manifest.put("textfieldClassExportArtifact", TEXT_FIELD_CLASS_EXPORT_ARTIFACT);
        manifest.put("textfieldTransitiveArtifacts", List.of(DEV_BUNDLE_INDEX_HTML_JS, DEV_BUNDLE_COMMONJS_JS));
        manifest.put("textfieldFirstDefinitionSource", TEXT_FIELD_FIRST_DEFINITION_SOURCE);
        manifest.put("textfieldSecondDefinitionSource", TEXT_FIELD_SECOND_DEFINITION_SOURCE);
        manifest.put("textfieldDuplicateTag", TEXT_FIELD_DUPLICATE_TAG);
        manifest.put("textfieldRemainingBlockerClassification", TEXT_FIELD_REMAINING_BLOCKER_CLASSIFICATION);
        manifest.put("textfieldMinimalReproduction", TEXT_FIELD_MINIMAL_REPRODUCTION);
        manifest.put("textfieldNextResolutionStep", TEXT_FIELD_NEXT_RESOLUTION_STEP);
        manifest.put("generatedLazyResolverVersion", GENERATED_LAZY_RESOLVER_VERSION);
        manifest.put("generatedLazyExecutionVersion", GENERATED_LAZY_EXECUTION_VERSION);
        manifest.put("generatedLazyImportResolutionStrategy", GENERATED_LAZY_IMPORT_RESOLUTION_STRATEGY);
        manifest.put("generatedLazyGraphProbe", GENERATED_LAZY_GRAPH_PROBE);
        manifest.put("generatedLazyLoadOnDemandContractVersion",
                GENERATED_LAZY_LOAD_ON_DEMAND_CONTRACT_VERSION);
        manifest.put("generatedLazyTriggerSource", GENERATED_LAZY_LOAD_ON_DEMAND_TRIGGER_SOURCE);
        manifest.put("generatedLazyApiOwner", GENERATED_LAZY_LOAD_ON_DEMAND_API_OWNER);
        manifest.put("generatedLazyApiInstallSource",
                GENERATED_LAZY_LOAD_ON_DEMAND_API_INSTALL_SOURCE);
        manifest.put("generatedLazyRequestedChunk", GENERATED_LAZY_LOAD_ON_DEMAND_REQUESTED_CHUNK);
        manifest.put("generatedLazyRegistrySource", GENERATED_LAZY_LOAD_ON_DEMAND_REGISTRY_SOURCE);
        manifest.put("generatedLazyMappingSource", GENERATED_LAZY_LOAD_ON_DEMAND_MAPPING_SOURCE);
        manifest.put("generatedLazyResolvedArtifactPath", DEV_BUNDLE_GENERATED_FLOW_IMPORTS_JS);
        manifest.put("generatedLazyFlowClientTriggered", false);
        manifest.put("generatedLazyDiagnosticBridge", true);
        manifest.put("generatedLazyBlockerVersion", GENERATED_LAZY_BLOCKER_VERSION);
        manifest.put("generatedLazyRelativeImportSpecifier", GENERATED_LAZY_RELATIVE_IMPORT_SPECIFIER);
        manifest.put("generatedLazyResolvedDependencyPath", DEV_BUNDLE_MARKDOWN_HELPERS_JS);
        manifest.put("generatedLazyFailureLayer", GENERATED_LAZY_FAILURE_LAYER);
        manifest.put("generatedLazyNextResolutionStep", GENERATED_LAZY_NEXT_RESOLUTION_STEP);
        manifest.put("generatedLazyChunkExpressionSource", GENERATED_LAZY_CHUNK_EXPRESSION_SOURCE);
        manifest.put("generatedLazyChunkTriggerOwner", GENERATED_LAZY_CHUNK_TRIGGER_OWNER);
        manifest.put("generatedLazyChunkResolvedArtifactPath", DEV_BUNDLE_GENERATED_FLOW_IMPORTS_JS);
        manifest.put("generatedLazyChunkDiagnosticBridgeUsed", false);
        manifest.put("generatedLazyChunkDirectImportProbeUsed", false);
        manifest.put("generatedLazyChunkFailureLayer", GENERATED_LAZY_CHUNK_FAILURE_LAYER);
        manifest.put("generatedLazyChunkBlockerVersion", GENERATED_LAZY_CHUNK_BLOCKER_VERSION);
        manifest.put("generatedLazyChunkNextResolutionStep", GENERATED_LAZY_CHUNK_NEXT_RESOLUTION_STEP);
        manifest.put("loadOnDemandChunkContractVersion", GENERATED_LAZY_CHUNK_CONTRACT_VERSION);
        manifest.put("loadOnDemandChunkApiOwner", GENERATED_LAZY_LOAD_ON_DEMAND_API_OWNER);
        manifest.put("loadOnDemandChunkApiInstallSource", GENERATED_LAZY_CHUNK_API_INSTALL_SOURCE);
        manifest.put("loadOnDemandChunkRegistrySource", GENERATED_LAZY_CHUNK_REGISTRY_SOURCE);
        manifest.put("loadOnDemandChunkMappingSource", GENERATED_LAZY_CHUNK_MAPPING_SOURCE);
        manifest.put("loadOnDemandChunkResolvedArtifactPath", DEV_BUNDLE_GENERATED_FLOW_IMPORTS_JS);
        manifest.put("loadOnDemandChunkModuleIdentity",
                "stable-resource-path-blob-module-identity-v1_7:" + DEV_BUNDLE_GENERATED_FLOW_IMPORTS_JS);
        manifest.put("loadOnDemandChunkFailureLayer", GENERATED_LAZY_CHUNK_FAILURE_LAYER);
        manifest.put("loadOnDemandChunkBlockerVersion", GENERATED_LAZY_CHUNK_BLOCKER_VERSION);
        manifest.put("loadOnDemandChunkNextResolutionStep", GENERATED_LAZY_CHUNK_NEXT_RESOLUTION_STEP);
        manifest.putAll(generatedLoadOnDemandChunkFileRegistryAttributes(DEV_BUNDLE_GENERATED_FLOW_IMPORTS_JS));
        manifest.put("runtimeResolverContractVersion", RUNTIME_RESOLVER_CONTRACT_VERSION);
        manifest.put("flowLazyDependencyVersion", FLOW_LAZY_DEPENDENCY_VERSION);
        manifest.put("flowLazyDependencySource", FLOW_LAZY_DEPENDENCY_SOURCE);
        manifest.put("flowLazyDependencyMode", "LoadMode.LAZY");
        manifest.put("flowLazyDependencyExpression", FLOW_LAZY_DEPENDENCY_EXPRESSION);
        manifest.put("flowLazyDependencyChunkId", "vatori-flow-ipc-lazy-v1_10");
        manifest.put("flowLazyDependencyResolvedPath", FLOW_LAZY_DEPENDENCY_JS);
        manifest.put("flowLazyDependencyTriggerOwner", FLOW_LAZY_DEPENDENCY_TRIGGER_OWNER);
        manifest.put("flowLazyDependencyTriggeredByFlowClient", true);
        manifest.put("flowLazyDependencyResolver", FLOW_LAZY_DEPENDENCY_RESOLVER);
        manifest.put("productionBundleRuntimeVersion", PRODUCTION_RUNTIME_VERSION);
        manifest.put("productionBundleRuntimeRegistrySource", PRODUCTION_RUNTIME_REGISTRY_SOURCE);
        manifest.put("productionBundleRuntimeStatsSource", DEV_BUNDLE_STATS_JSON);
        manifest.put("productionBundleRuntimeSelectedArtifact", DEV_BUNDLE_INDEX_HTML_JS);
        manifest.put("productionBundleRuntimeDependencyChain", PRODUCTION_RUNTIME_DEPENDENCY_CHAIN);
        manifest.put("productionBundleRuntimeModuleIdentity", PRODUCTION_RUNTIME_MODULE_IDENTITY);
        manifest.put("productionBundleRuntimeBlockerVersion", PRODUCTION_RUNTIME_BLOCKER);
        manifest.put("productionBundleRuntimeNextResolutionStep", PRODUCTION_RUNTIME_NEXT_STEP);
        manifest.putAll(productionRuntimeOwnerAttributes());
        manifest.put("artifactFamilies", List.of("flow-client-classpath",
                "vaadin-component-classpath-frontend",
                "npm-frontend",
                "theme-static",
                "production-bundle",
                "dev-mode-bundle",
                "generated-lazy",
                "flow-lazy-dependency"));
        manifest.put("artifactFamilyStatuses", List.of(
                artifactFamilyStatus("flow-client-classpath", "available", "classpath-flow-client-resource"),
                artifactFamilyStatus("vaadin-component-classpath-frontend", "available",
                        "classpath-component-frontend-resource"),
                artifactFamilyStatus("npm-frontend", "partial", "real-textfield-production-graph-executed-v1_7"),
                artifactFamilyStatus("theme-static", "available", "theme-static-classpath-resource"),
                artifactFamilyStatus("production-bundle", "partial",
                        "production-bundle-runtime-owner-v1_13"),
                artifactFamilyStatus("dev-mode-bundle", "partial", "dev-mode-package-lock-verified"),
                artifactFamilyStatus("generated-lazy", "partial",
                        "generated-lazy-loadondemand-chunk-mapped-v1_11"),
                artifactFamilyStatus("flow-lazy-dependency", "available",
                        "flow-lazy-dependency-runtime-v1_10")));
        manifest.put("dependencyGraph", Map.of("declared", true,
                "resolver", COMPONENT_RESOLVER_V1_4,
                "resourceCount", entries.size()));
        manifest.put("strictBlockers", List.of(
                strictBlocker("dev-mode-bundle", "dev-mode-bundle-execution-unmapped",
                        "dev-mode package-lock dependency graph is verified over IPC, but Vite/dev-server module execution is not yet mapped into the no-localhost resolver"),
                strictBlocker("generated-lazy", "generated-lazy-loadondemand-api-boundary-v1_9",
                        "generated-flow-imports relative dependencies execute through the no-localhost staging resolver, but the real Flow.loadOnDemand API is still installed by the direct staged generated-flow-imports artifact rather than triggered by the Flow client engine"),
                strictBlocker("generated-lazy", GENERATED_LAZY_CHUNK_BLOCKER_VERSION,
                        "real UIDL Flow.loadOnDemand expressions are observed and mapped to the generated-flow-imports artifact, but this dev bundle exposes a no-op loadOnDemand table without chunk-file resolution"),
                strictBlocker("generated-lazy", GENERATED_LAZY_CHUNK_FILE_BLOCKER_VERSION,
                        "generated-flow-imports has been parsed for chunk-file branches; this Vaadin dev bundle exposes a real no-op Promise.resolve(0) loadOnDemand table, so there is no generated ./chunks/chunk-*.js file to stage yet"),
                strictBlocker("production-bundle", "flow-ipc-production-runtime-v1_10",
                        "production bundle stats now selects an executable runtime artifact and stages it through stable module identity, but full production runtime ownership is still blocked until side effects can be executed without relying on focused component entry imports"),
                strictBlocker("production-bundle", PRODUCTION_RUNTIME_OWNER_BLOCKER,
                        "production bundle owner is now explicit and independently staged, but import remains blocked until the owner can safely execute indexhtml side effects without colliding with focused component entry imports")));
        manifest.put("transport", "flow-ipc");
        manifest.put("resourceLoading", "ipc-resource-request");
        manifest.put("componentClass", "com.vaadin.flow.component.textfield.BigDecimalField");
        manifest.put("componentTag", "vaadin-big-decimal-field");
        manifest.put("primaryResource", BIG_DECIMAL_FIELD_JS);
        manifest.put("resolverStrategy", "generic-component-frontend-manifest-v1_3");
        manifest.put("componentCount", 4);
        manifest.put("componentTags", List.of("vaadin-big-decimal-field", "vaadin-checkbox",
                "vaadin-select", "vaadin-text-field"));
        manifest.put("primaryResources", List.of(BIG_DECIMAL_FIELD_JS,
                DEV_BUNDLE_CHECKBOX_ENTRY_JS,
                DEV_BUNDLE_SELECT_ENTRY_JS,
                DEV_BUNDLE_TEXT_FIELD_ENTRY_JS));
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
                        "productionBundleReady", true),
                Map.of("componentClass", "com.vaadin.flow.component.textfield.TextField",
                        "componentTag", "vaadin-text-field",
                        "primaryResource", DEV_BUNDLE_TEXT_FIELD_ENTRY_JS,
                        "requestedModule", "@vaadin/text-field/src/vaadin-text-field.js",
                        "resourceOrigin", "vaadin-dev-bundle-text-field-entry",
                        "productionBundleReady", true)));
        manifest.put("requestedComponent", "com.vaadin.flow.component.textfield.TextField");
        manifest.put("substituteReason", "TextField class is available but its npm frontend graph is not classpath-resolved in this smoke runtime; BigDecimalField provides a real TextField-family classpath frontend module.");
        manifest.put("classpath", true);
        manifest.put("npmFrontendGraphComplete", false);
        manifest.put("productionBundleReady", false);
        manifest.put("devModeReady", false);
        manifest.put("failureClassification", "real-textfield-production-graph-executed-v1_7");
        manifest.put("narrowedFailureClassification", TEXT_FIELD_NARROWED_FAILURE_CLASSIFICATION);
        manifest.put("previousBlockerVersion", TEXT_FIELD_PREVIOUS_BLOCKER_VERSION);
        manifest.put("executionVersion", TEXT_FIELD_EXECUTION_VERSION);
        manifest.put("blockedReason", COMPONENT_TRANSITIVE_BLOCKED_REASON);
        manifest.put("unresolvedImports", List.of(
                Map.of("specifier", "@vaadin/text-field/src/vaadin-text-field.js",
                        "classification", "legacy-textfield-shim-fallback-not-success",
                        "resolution", TEXT_FIELD_SHIM_JS)));
        manifest.put("resolvedImports", List.of(
                Map.of("specifier", "lit",
                        "classification", "focused-extracted-production-bundle-helper",
                        "resolution", DEV_BUNDLE_LIT_ENTRY_JS),
                Map.of("specifier", "@vaadin/text-field/src/vaadin-text-field.js",
                        "classification", "real-textfield-production-graph-executed-v1_7",
                        "resolution", DEV_BUNDLE_TEXT_FIELD_ENTRY_JS),
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
        attributes.put("executionPolicy", shim ? "legacy-shim-fallback" : "execute-primary-module");
        attributes.put("artifactStatus", shim ? "legacy-fallback" : "available");
        attributes.put("requestedBy", "component-frontend-manifest-v1_4");
        attributes.put("failureClassification", shim
                ? "legacy-textfield-shim-fallback-not-success"
                : "real-textfield-production-graph-executed-v1_7");
        attributes.put("blockedReason", COMPONENT_TRANSITIVE_BLOCKED_REASON);
        attributes.put("lookupKind", lookupKind(componentClass, resolver));
        if (shim) {
            attributes.put("legacyFallback", true);
            attributes.put("successPath", false);
        }
        if (!shim) {
            attributes.put("shimmedImports", List.of());
            attributes.put("realTextFieldGraphImport", true);
            attributes.put("importRewriteStrategy", "manifest-import-rewrite-v1_4");
            attributes.put("browserExecutionPlan", "dynamic-import-blob-with-manifest-import-rewrites-v1_4");
            attributes.put("importRewrites", List.of(
                    importRewrite("lit",
                            DEV_BUNDLE_LIT_ENTRY_JS,
                            "production-bundle",
                            "vaadin-dev-bundle-production-build-v1_4",
                            "dev-bundle-resource-graph-partial"),
                    importRewrite("@vaadin/text-field/src/vaadin-text-field.js",
                            DEV_BUNDLE_TEXT_FIELD_ENTRY_JS,
                            "production-bundle",
                            TEXT_FIELD_IMPORT_RESOLUTION_STRATEGY,
                            "real-textfield-production-graph-executed-v1_7"),
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
        attributes.put("componentProductionOwnerReused", false);
        attributes.put("componentProductionOwnerReuseReason",
                "production owner stage identity is tracked separately in v1.13; focused component imports keep their existing stable resource-path identity until the owner import contract is enabled");
        attributes.put("componentProductionOwnerDependencyPaths", DEV_BUNDLE_INDEX_HTML_JS);
        if (primary) {
            attributes.put("componentPrimary", true);
        } else {
            attributes.put("componentBundleDependency", true);
        }
        if (DEV_BUNDLE_INDEX_HTML_JS.equals(moduleSpecifier)) {
            attributes.put("runtimeResolverContractVersion", RUNTIME_RESOLVER_CONTRACT_VERSION);
            attributes.put("logicalRequestId", "production-runtime:indexhtml:v1_10");
            attributes.put("pathMapping", "stats-bundle-import-to-dev-bundle-artifact");
            attributes.put("stageUrlPolicy", "stable-module-identity-blob-url");
            attributes.put("cachePolicy", "retain-per-resource-path");
            attributes.put("cleanupOwnership", "window-lifetime-module-identity-cache");
            attributes.put("blockerClassification", PRODUCTION_RUNTIME_BLOCKER);
            attributes.put("productionBundleRuntimeArtifact", true);
            attributes.put("productionBundleRuntimeVersion", PRODUCTION_RUNTIME_VERSION);
            attributes.put("productionBundleRuntimeRegistrySource", PRODUCTION_RUNTIME_REGISTRY_SOURCE);
            attributes.put("productionBundleRuntimeStatsSource", DEV_BUNDLE_STATS_JSON);
            attributes.put("productionBundleRuntimeSelectedArtifact", DEV_BUNDLE_INDEX_HTML_JS);
            attributes.put("productionBundleRuntimeDependencyChain", PRODUCTION_RUNTIME_DEPENDENCY_CHAIN);
            attributes.put("productionBundleRuntimeModuleIdentity", PRODUCTION_RUNTIME_MODULE_IDENTITY);
            attributes.put("productionBundleRuntimeExecutionPolicy",
                    "stage-execution-attempt-production-bundle-runtime-v1_10");
            attributes.put("productionBundleRuntimeExpectedSideEffect",
                    "register production custom element classes when owned by a production bundle runtime");
            attributes.put("productionBundleRuntimeCachePolicy",
                    "reuse stable resource-path module identity to avoid duplicate custom element definitions");
            attributes.put("productionBundleRuntimeCleanupPolicy", "retain-for-window-lifetime-module-identity-v1_7");
            attributes.put("productionBundleRuntimeBlockerVersion", PRODUCTION_RUNTIME_BLOCKER);
            attributes.put("productionBundleRuntimeFailureLayer",
                    "production-runtime-ownership-not-yet-separated-from-focused-component-entry-imports");
            attributes.put("productionBundleRuntimeNextResolutionStep", PRODUCTION_RUNTIME_NEXT_STEP);
            attributes.putAll(productionRuntimeOwnerAttributes());
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
        attributes.put("textfieldGraphParityVersion", TEXT_FIELD_GRAPH_PARITY_VERSION);
        attributes.put("previousBlockerVersion", TEXT_FIELD_PREVIOUS_BLOCKER_VERSION);
        attributes.put("executionVersion", TEXT_FIELD_EXECUTION_VERSION);
        attributes.put("textfieldSideEffectVersion", TEXT_FIELD_SIDE_EFFECT_VERSION);
        attributes.put("graphProbe", TEXT_FIELD_GRAPH_PROBE);
        attributes.put("graphEntryArtifact", DEV_BUNDLE_TEXT_FIELD_ENTRY_JS);
        attributes.put("classExportArtifact", TEXT_FIELD_CLASS_EXPORT_ARTIFACT);
        attributes.put("transitiveImports", List.of(DEV_BUNDLE_INDEX_HTML_JS, DEV_BUNDLE_COMMONJS_JS));
        attributes.put("importResolutionStrategy", TEXT_FIELD_IMPORT_RESOLUTION_STRATEGY);
        attributes.put("sideEffectStrategy", TEXT_FIELD_SIDE_EFFECT_STRATEGY);
        attributes.put("narrowedFailureClassification", TEXT_FIELD_NARROWED_FAILURE_CLASSIFICATION);
        attributes.put("failureLayer", TEXT_FIELD_FAILURE_LAYER);
        attributes.put("minimalReproduction", TEXT_FIELD_MINIMAL_REPRODUCTION);
        attributes.put("blockedExecutionProbe", "legacy-v1_6-big-decimal-import-rewrite-to-textfield-dev-bundle");
        attributes.put("blockedExecutionError", "resolved-by-stable-module-identity-v1_7");
        attributes.put("firstDefinitionSource", TEXT_FIELD_FIRST_DEFINITION_SOURCE);
        attributes.put("secondDefinitionSource", TEXT_FIELD_SECOND_DEFINITION_SOURCE);
        attributes.put("duplicateTag", TEXT_FIELD_DUPLICATE_TAG);
        attributes.put("remainingBlockerClassification", TEXT_FIELD_REMAINING_BLOCKER_CLASSIFICATION);
        attributes.put("moduleIdentityStrategy", TEXT_FIELD_IMPORT_RESOLUTION_STRATEGY);
        attributes.put("legacyShimSuccessPath", false);
        attributes.put("nextResolutionStep", TEXT_FIELD_NEXT_RESOLUTION_STEP);
        attributes.put("blockedReason",
                "Focused TextField production class entry is executed through stable resource-path Blob module identity so the production indexhtml dependency is reused instead of re-imported and re-registering vaadin-text-field.");
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
        attributes.put("runtimeResolverContractVersion", RUNTIME_RESOLVER_CONTRACT_VERSION);
        attributes.put("logicalRequestId", "production-runtime:stats:v1_10");
        attributes.put("pathMapping", "stats-json-to-runtime-artifact-registry");
        attributes.put("dependencyGraph", "bundleImports=>VAADIN/build/indexhtml-BfYYZi8S.js");
        attributes.put("productionBundleRuntimeVersion", PRODUCTION_RUNTIME_VERSION);
        attributes.put("productionBundleRuntimeRegistrySource", PRODUCTION_RUNTIME_REGISTRY_SOURCE);
        attributes.put("productionBundleRuntimeStatsSource", DEV_BUNDLE_STATS_JSON);
        attributes.put("productionBundleRuntimeSelectedArtifact", DEV_BUNDLE_INDEX_HTML_JS);
        attributes.put("productionBundleRuntimeDependencyChain", PRODUCTION_RUNTIME_DEPENDENCY_CHAIN);
        attributes.put("productionBundleRuntimeModuleIdentity", PRODUCTION_RUNTIME_MODULE_IDENTITY);
        attributes.put("productionBundleRuntimeBlockerVersion", PRODUCTION_RUNTIME_BLOCKER);
        attributes.put("productionBundleRuntimeNextResolutionStep", PRODUCTION_RUNTIME_NEXT_STEP);
        attributes.putAll(productionRuntimeOwnerAttributes());
        return attributes;
    }

    private static Map<String, Object> productionRuntimeOwnerAttributes() {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("productionBundleRuntimeOwnerVersion", PRODUCTION_RUNTIME_OWNER_VERSION);
        attributes.put("productionBundleRuntimeOwner", PRODUCTION_RUNTIME_OWNER);
        attributes.put("productionBundleRuntimeEntryArtifact", DEV_BUNDLE_INDEX_HTML_JS);
        attributes.put("productionBundleRuntimeDependencyChain", PRODUCTION_RUNTIME_DEPENDENCY_CHAIN);
        attributes.put("productionBundleRuntimeSource", PRODUCTION_RUNTIME_OWNER_SOURCE);
        attributes.put("productionBundleRuntimeSelectedBy", PRODUCTION_RUNTIME_OWNER_SELECTED_BY);
        attributes.put("productionBundleRuntimeOwnerModuleIdentity",
                PRODUCTION_RUNTIME_OWNER_MODULE_IDENTITY);
        attributes.put("productionBundleRuntimeOwnerLifecycle",
                PRODUCTION_RUNTIME_OWNER_LIFECYCLE);
        attributes.put("productionBundleRuntimeOwnerExecutionPolicy",
                PRODUCTION_RUNTIME_OWNER_EXECUTION_POLICY);
        attributes.put("productionBundleRuntimeOwnerStageUrlPolicy",
                "temporary-blob-module-url-owner-namespace-v1_13");
        attributes.put("productionBundleRuntimeOwnerCachePolicy",
                "retain-per-owner-module-identity");
        attributes.put("productionBundleRuntimeOwnerCleanupStrategy",
                "cleanup-owned-window-lifetime-module-identity");
        attributes.put("productionBundleRuntimeOwnerBlockerVersion",
                PRODUCTION_RUNTIME_OWNER_BLOCKER);
        attributes.put("productionBundleRuntimeOwnerFailureLayer",
                PRODUCTION_RUNTIME_OWNER_FAILURE_LAYER);
        attributes.put("productionBundleRuntimeOwnerSideEffectProof",
                PRODUCTION_RUNTIME_OWNER_SIDE_EFFECT_PROOF);
        attributes.put("productionBundleRuntimeOwnerBrowserError", "");
        attributes.put("productionBundleRuntimeOwnerNextResolutionStep",
                PRODUCTION_RUNTIME_OWNER_NEXT_STEP);
        attributes.put("productionBundleRuntimeOwnerExpectedSideEffect",
                "own production indexhtml custom element and Flow global side effects before component imports reuse them");
        attributes.put("productionBundleRuntimeOwnerCustomElementTags",
                "vaadin-text-field,vaadin-checkbox,vaadin-select");
        attributes.put("componentProductionOwnerReused", false);
        attributes.put("componentProductionOwnerReuseReason",
                "owner import disabled until production indexhtml side effects can be owned without duplicate custom element definitions");
        attributes.put("componentProductionOwnerDependencyPaths", DEV_BUNDLE_INDEX_HTML_JS);
        return attributes;
    }

    private static Map<String, Object> flowLazyDependencyAttributes() {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("componentFrontendResource", true);
        attributes.put("flowLazyDependencyResource", true);
        attributes.put("runtimeResolverContractVersion", RUNTIME_RESOLVER_CONTRACT_VERSION);
        attributes.put("logicalRequestId", "flow-lazy-dependency:v1_10");
        attributes.put("artifactFamily", "flow-lazy-dependency");
        attributes.put("origin", "classpath");
        attributes.put("resourceOrigin", "flow-uidl-loadmode-lazy-dependency");
        attributes.put("moduleSpecifier", FLOW_LAZY_DEPENDENCY_EXPRESSION);
        attributes.put("resolver", FLOW_LAZY_DEPENDENCY_RESOLVER);
        attributes.put("dependencyOf", "UidlWriter:LoadMode.LAZY");
        attributes.put("executionPolicy", "execute-after-flow-dependencyloader-trigger-v1_10");
        attributes.put("artifactStatus", "available");
        attributes.put("requestedBy", "FlowClient.DependencyLoader");
        attributes.put("failureClassification", "flow-lazy-dependency-runtime-v1_10");
        attributes.put("blockedReason",
                "real Flow LoadMode.LAZY dependency is resolved through IPC instead of a localhost URL");
        attributes.put("lookupKind", "Flow-lazy-dependency-resource-v1_10");
        attributes.put("pathMapping", "context-url-to-ipc-resource-path");
        attributes.put("dependencyGraph", "UidlWriter:LAZY=>DependencyLoader=>script.src=>IPC resourceRequest");
        attributes.put("stageUrlPolicy", "blob-module-url");
        attributes.put("cachePolicy", "single-trigger-per-resource-path");
        attributes.put("cleanupOwnership", "revoke-after-lazy-dependency-load");
        attributes.put("blockerClassification", "none");
        attributes.put("lazyDependencyVersion", FLOW_LAZY_DEPENDENCY_VERSION);
        attributes.put("lazyDependencySource", FLOW_LAZY_DEPENDENCY_SOURCE);
        attributes.put("lazyDependencyMode", "LoadMode.LAZY");
        attributes.put("lazyDependencyExpression", FLOW_LAZY_DEPENDENCY_EXPRESSION);
        attributes.put("lazyDependencyChunkId", "vatori-flow-ipc-lazy-v1_10");
        attributes.put("lazyDependencyResolvedPath", FLOW_LAZY_DEPENDENCY_JS);
        attributes.put("lazyDependencyTriggerOwner", FLOW_LAZY_DEPENDENCY_TRIGGER_OWNER);
        attributes.put("lazyDependencyTriggeredByFlowClient", true);
        attributes.put("lazyDependencyResolver", FLOW_LAZY_DEPENDENCY_RESOLVER);
        attributes.put("lazyDependencyExecutionExpectedGlobal",
                "window.VaadinDesktopIpcLazyDependencyProbe.executed");
        attributes.put("shim", false);
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
        attributes.put("generatedLazyLoadOnDemandContractVersion",
                GENERATED_LAZY_LOAD_ON_DEMAND_CONTRACT_VERSION);
        attributes.put("generatedLazyTriggerSource", "repository-fixture-verified-only");
        attributes.put("generatedLazyApiOwner", "repository-local generated Flow fixture");
        attributes.put("generatedLazyApiInstallSource", "repository-generated-flow-imports-fixture");
        attributes.put("generatedLazyRequestedChunk", GENERATED_LAZY_LOAD_ON_DEMAND_REQUESTED_CHUNK);
        attributes.put("generatedLazyRegistrySource", "repository-fixture-static-map");
        attributes.put("generatedLazyMappingSource", "repository-generated-flow-imports-fixture");
        attributes.put("generatedLazyResolvedArtifactPath", GENERATED_FLOW_IMPORTS_JS);
        attributes.put("generatedLazyFlowClientTriggered", false);
        attributes.put("generatedLazyDiagnosticBridge", false);
        attributes.put("generatedLazyChunkExpressionSource", "repository-fixture-verified-only");
        attributes.put("generatedLazyChunkTriggerOwner", "repository-local-generated-flow-fixture");
        attributes.put("generatedLazyChunkResolvedArtifactPath", GENERATED_FLOW_IMPORTS_JS);
        attributes.put("generatedLazyChunkDiagnosticBridgeUsed", false);
        attributes.put("generatedLazyChunkDirectImportProbeUsed", false);
        return attributes;
    }

    private static Map<String, Object> generatedLazyDevBundleChunkAttributes(String path, String lookupKind,
            String dependencyOf, boolean primary) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("componentFrontendResource", true);
        attributes.put("generatedLazyResource", true);
        attributes.put("generatedLazyBundleChunk", primary);
        attributes.put("generatedLazyBundleDependency", !primary);
        attributes.put("generatedLazyResolverVersion", GENERATED_LAZY_RESOLVER_VERSION);
        attributes.put("generatedLazyExecutionVersion", GENERATED_LAZY_EXECUTION_VERSION);
        attributes.put("generatedLazyImportResolutionStrategy", GENERATED_LAZY_IMPORT_RESOLUTION_STRATEGY);
        attributes.put("generatedLazyGraphProbe", GENERATED_LAZY_GRAPH_PROBE);
        attributes.put("generatedLazyLoadOnDemandContractVersion",
                GENERATED_LAZY_LOAD_ON_DEMAND_CONTRACT_VERSION);
        attributes.put("generatedLazyTriggerSource",
                primary ? GENERATED_LAZY_LOAD_ON_DEMAND_TRIGGER_SOURCE
                        : "generated-lazy-stable-relative-dependency");
        attributes.put("generatedLazyApiOwner", GENERATED_LAZY_LOAD_ON_DEMAND_API_OWNER);
        attributes.put("generatedLazyApiInstallSource",
                primary ? GENERATED_LAZY_LOAD_ON_DEMAND_API_INSTALL_SOURCE
                        : "dependency-staged-for-generated-flow-imports");
        attributes.put("generatedLazyRequestedChunk", GENERATED_LAZY_LOAD_ON_DEMAND_REQUESTED_CHUNK);
        attributes.put("generatedLazyRegistrySource", GENERATED_LAZY_LOAD_ON_DEMAND_REGISTRY_SOURCE);
        attributes.put("generatedLazyMappingSource", GENERATED_LAZY_LOAD_ON_DEMAND_MAPPING_SOURCE);
        attributes.put("generatedLazyResolvedArtifactPath", path);
        attributes.put("generatedLazyFlowClientTriggered", false);
        attributes.put("generatedLazyDiagnosticBridge", primary);
        attributes.put("generatedLazyBlockerVersion", GENERATED_LAZY_BLOCKER_VERSION);
        attributes.put("generatedLazyRelativeImportSpecifier", GENERATED_LAZY_RELATIVE_IMPORT_SPECIFIER);
        attributes.put("generatedLazyResolvedDependencyPath", DEV_BUNDLE_MARKDOWN_HELPERS_JS);
        attributes.put("generatedLazyChunkExpressionSource",
                primary ? GENERATED_LAZY_CHUNK_EXPRESSION_SOURCE
                        : "generated-lazy-stable-relative-dependency");
        attributes.put("generatedLazyChunkTriggerOwner",
                primary ? GENERATED_LAZY_CHUNK_TRIGGER_OWNER
                        : "generated-flow-imports-relative-import");
        attributes.put("generatedLazyChunkResolvedArtifactPath", path);
        attributes.put("generatedLazyChunkDiagnosticBridgeUsed", false);
        attributes.put("generatedLazyChunkDirectImportProbeUsed", false);
        attributes.put("generatedLazyChunkFailureLayer", GENERATED_LAZY_CHUNK_FAILURE_LAYER);
        attributes.put("generatedLazyChunkBlockerVersion", GENERATED_LAZY_CHUNK_BLOCKER_VERSION);
        attributes.put("generatedLazyChunkNextResolutionStep", GENERATED_LAZY_CHUNK_NEXT_RESOLUTION_STEP);
        attributes.put("loadOnDemandChunkContractVersion", GENERATED_LAZY_CHUNK_CONTRACT_VERSION);
        attributes.put("loadOnDemandChunkApiOwner", GENERATED_LAZY_LOAD_ON_DEMAND_API_OWNER);
        attributes.put("loadOnDemandChunkApiInstallSource", GENERATED_LAZY_CHUNK_API_INSTALL_SOURCE);
        attributes.put("loadOnDemandChunkRegistrySource", GENERATED_LAZY_CHUNK_REGISTRY_SOURCE);
        attributes.put("loadOnDemandChunkMappingSource", GENERATED_LAZY_CHUNK_MAPPING_SOURCE);
        attributes.put("loadOnDemandChunkResolvedArtifactPath", path);
        attributes.put("loadOnDemandChunkModuleIdentity",
                "stable-resource-path-blob-module-identity-v1_7:" + path);
        attributes.put("loadOnDemandChunkFailureLayer", GENERATED_LAZY_CHUNK_FAILURE_LAYER);
        attributes.put("loadOnDemandChunkBlockerVersion", GENERATED_LAZY_CHUNK_BLOCKER_VERSION);
        attributes.put("loadOnDemandChunkNextResolutionStep", GENERATED_LAZY_CHUNK_NEXT_RESOLUTION_STEP);
        if (primary) {
            attributes.putAll(generatedLoadOnDemandChunkFileRegistryAttributes(path));
        }
        attributes.put("devBundleResource", true);
        attributes.put("productionBundleResource", true);
        attributes.put("moduleSpecifier", path);
        attributes.put("resolver", "vaadin-dev-bundle-generated-flow-imports-v1_4");
        attributes.put("shim", false);
        attributes.put("artifactFamily", "generated-lazy");
        attributes.put("origin", "vaadin-dev-bundle");
        attributes.put("resourceOrigin",
                primary ? "vaadin-dev-bundle-generated-flow-imports"
                        : "vaadin-dev-bundle-generated-flow-imports-dependency");
        attributes.put("dependencyOf", dependencyOf);
        attributes.put("executionPolicy",
                primary ? "stage-and-import-generated-lazy-chunk-v1_8"
                        : "stable-dependency-module-generated-lazy-v1_8");
        attributes.put("artifactStatus", "partial");
        attributes.put("requestedBy", "component-frontend-manifest-v1_4");
        attributes.put("failureClassification",
                primary ? "generated-lazy-loadondemand-contract-narrowed-v1_9"
                        : "generated-lazy-dependency-module-resolved-v1_8");
        attributes.put("blockedReason",
                primary
                        ? "Vaadin dev-bundle generated-flow-imports chunk executes with relative imports rewritten to stable module identities; Flow.loadOnDemand remains a diagnostic call installed by that direct import instead of a Flow-client-triggered lazy request"
                        : "generated-flow-imports dependency chunk is available as a stable staged module identity for the v1.8 generated/lazy resolver");
        attributes.put("lookupKind", lookupKind);
        attributes.put("blockerVersion", GENERATED_LAZY_BLOCKER_VERSION);
        attributes.put("failureLayer", GENERATED_LAZY_FAILURE_LAYER);
        attributes.put("nextResolutionStep", GENERATED_LAZY_NEXT_RESOLUTION_STEP);
        attributes.put("importResolutionStrategy", GENERATED_LAZY_IMPORT_RESOLUTION_STRATEGY);
        attributes.put("moduleIdentityStrategy", "stable-resource-path-blob-module-identity-v1_7");
        if (primary) {
            attributes.put("transitiveImports", List.of(
                    "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js",
                    "vaadin-dev-bundle/webapp/VAADIN/build/commonjsHelpers-CUmg6egw.js",
                    "vaadin-dev-bundle/webapp/VAADIN/build/markdown-helpers-31I0A-hg.js"));
        }
        return attributes;
    }

    private static Map<String, Object> generatedLoadOnDemandChunkFileRegistryAttributes(String generatedImportsPath) {
        GeneratedLoadOnDemandChunkFileRegistry registry =
                GeneratedLoadOnDemandChunkFileRegistry.parse(generatedImportsPath,
                        classpathResourceText(generatedImportsPath));
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("generatedLoadOnDemandChunkFileRegistryVersion",
                GENERATED_LAZY_CHUNK_FILE_REGISTRY_VERSION);
        attributes.put("generatedLoadOnDemandChunkFileRegistrySource", registry.registrySource());
        attributes.put("generatedLoadOnDemandChunkFileMappingSource", registry.mappingSource());
        attributes.put("generatedLoadOnDemandGeneratedImportsArtifact", registry.generatedImportsArtifact());
        attributes.put("generatedLoadOnDemandSourceBuildMode", registry.sourceBuildMode());
        attributes.put("generatedLoadOnDemandSourceArtifactFamily", registry.sourceArtifactFamily());
        attributes.put("generatedLoadOnDemandNoOpLoadOnDemand", registry.noOpLoadOnDemand());
        attributes.put("loadOnDemandTableShape", registry.loadOnDemandTableShape());
        attributes.put("chunkId", registry.chunkId());
        attributes.put("chunkImportSpecifier", registry.chunkImportSpecifier());
        attributes.put("chunkArtifactPath", registry.chunkArtifactPath());
        attributes.put("chunkContentHash", registry.chunkContentHash());
        attributes.put("chunkFileResolver", registry.chunkFileResolver());
        attributes.put("chunkFileStatus", registry.chunkFileStatus());
        attributes.put("chunkFileBytes", registry.chunkFileBytes());
        attributes.put("chunkFileSha256", registry.chunkFileSha256());
        attributes.put("noChunkGeneratedReason", registry.noChunkGeneratedReason());
        attributes.put("loadOnDemandChunkFileRegistryVersion", GENERATED_LAZY_CHUNK_FILE_REGISTRY_VERSION);
        attributes.put("loadOnDemandChunkFileRegistrySource", registry.registrySource());
        attributes.put("loadOnDemandChunkFileMappingSource", registry.mappingSource());
        attributes.put("loadOnDemandChunkFileGeneratedImportsArtifact", registry.generatedImportsArtifact());
        attributes.put("loadOnDemandChunkFileNoOpLoadOnDemand", registry.noOpLoadOnDemand());
        attributes.put("loadOnDemandChunkFileTableShape", registry.loadOnDemandTableShape());
        attributes.put("loadOnDemandChunkFileResolver", registry.chunkFileResolver());
        attributes.put("loadOnDemandChunkFileStatus", registry.chunkFileStatus());
        attributes.put("loadOnDemandChunkFileBytes", registry.chunkFileBytes());
        attributes.put("loadOnDemandChunkFileSha256", registry.chunkFileSha256());
        attributes.put("loadOnDemandChunkFileFailureLayer", registry.failureLayer());
        attributes.put("loadOnDemandChunkFileBlockerVersion", GENERATED_LAZY_CHUNK_FILE_BLOCKER_VERSION);
        attributes.put("loadOnDemandChunkFileNextResolutionStep", registry.nextResolutionStep());
        attributes.put("loadOnDemandChunkFileSideEffectProof", registry.sideEffectProof());
        return attributes;
    }

    private static String classpathResourceText(String path) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = FlowClientResources.class.getClassLoader();
        }
        try (InputStream stream = loader.getResourceAsStream(path)) {
            if (stream == null) {
                return "";
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalStateException("Unable to read Flow client resource " + path, error);
        }
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
        String executionPolicy = flowClientExecutionPolicy(kind);
        attributes.put("dependencyOf", flowClientDependencyOf(kind));
        attributes.put("executionPolicy", executionPolicy);
        attributes.put("artifactStatus", "available");
        attributes.put("failureClassification", "classpath-flow-client-resource");
        attributes.put("requestedBy", "flow-client-manifest-v1_4");
        if (!"execute-primary-module".equals(executionPolicy)) {
            attributes.put("blockedReason",
                    "Flow client classpath resource is verified over IPC but intentionally not executed by the current dev-ipc runtime slice");
        }
        return attributes;
    }

    private static String flowClientExecutionPolicy(String kind) {
        if ("flow-bootstrap-module".equals(kind) || "flow-client-module".equals(kind)) {
            return "execute-primary-module";
        }
        return "verified-only-intentionally-not-executed";
    }

    private static String flowClientDependencyOf(String kind) {
        return switch (kind) {
            case "flow-bootstrap-module" -> "flow-client-bootstrap";
            case "flow-client-module" -> "flow-client-engine";
            case "flow-client-loader-module" -> "Flow.loadOnDemand";
            case "flow-client-index-module" -> "flow-client-entrypoint";
            case "flow-client-static-metadata" -> "flow-client-static-resource-graph";
            case "flow-client-gwt-bundle" -> "flow-client-gwt-resource-graph";
            default -> "flow-client-classpath-resource-graph";
        };
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

    private record GeneratedLoadOnDemandChunkFileRegistry(
            String chunkId,
            String chunkImportSpecifier,
            String chunkArtifactPath,
            String chunkContentHash,
            String registrySource,
            String mappingSource,
            String generatedImportsArtifact,
            boolean noOpLoadOnDemand,
            String noChunkGeneratedReason,
            String sourceBuildMode,
            String sourceArtifactFamily,
            String loadOnDemandTableShape,
            String chunkFileResolver,
            String chunkFileStatus,
            int chunkFileBytes,
            String chunkFileSha256,
            String failureLayer,
            String nextResolutionStep,
            String sideEffectProof) {

        static GeneratedLoadOnDemandChunkFileRegistry parse(String generatedImportsArtifact, String body) {
            String sourceBuildMode = sourceBuildMode(generatedImportsArtifact);
            String sourceArtifactFamily = "generated-lazy";
            String safeBody = body == null ? "" : body;
            Matcher chunkMatcher = GENERATED_LAZY_CHUNK_IMPORT.matcher(safeBody);
            if (chunkMatcher.find()) {
                String chunkId = chunkMatcher.group(1);
                String chunkImportSpecifier = chunkMatcher.group(2);
                String chunkArtifactPath = resolveRelativeChunkPath(generatedImportsArtifact, chunkImportSpecifier);
                byte[] chunkBody = classpathResourceBytes(chunkArtifactPath);
                if (chunkBody.length > 0) {
                    String sha256 = Resource.sha256(chunkBody);
                    return new GeneratedLoadOnDemandChunkFileRegistry(chunkId,
                            chunkImportSpecifier,
                            chunkArtifactPath,
                            sha256,
                            GENERATED_LAZY_CHUNK_FILE_REGISTRY_SOURCE,
                            GENERATED_LAZY_CHUNK_FILE_MAPPING_SOURCE,
                            generatedImportsArtifact,
                            false,
                            "",
                            sourceBuildMode,
                            sourceArtifactFamily,
                            "branch-table-dynamic-import",
                            GENERATED_LAZY_CHUNK_FILE_RESOLVER,
                            "available",
                            chunkBody.length,
                            sha256,
                            "",
                            "",
                            "chunk-file-body-available-for-staged-import");
                }
                return new GeneratedLoadOnDemandChunkFileRegistry(chunkId,
                        chunkImportSpecifier,
                        chunkArtifactPath,
                        "",
                        GENERATED_LAZY_CHUNK_FILE_REGISTRY_SOURCE,
                        GENERATED_LAZY_CHUNK_FILE_MAPPING_SOURCE,
                        generatedImportsArtifact,
                        false,
                        "",
                        sourceBuildMode,
                        sourceArtifactFamily,
                        "branch-table-dynamic-import",
                        GENERATED_LAZY_CHUNK_FILE_RESOLVER,
                        "not-found",
                        0,
                        "",
                        "chunk-file-not-found",
                        "add the generated chunk artifact to the no-localhost resolver classpath or staging source",
                        "chunk-branch-observed-but-artifact-body-missing");
            }
            boolean noOp = GENERATED_LAZY_NOOP_LOAD_ON_DEMAND.matcher(safeBody).find();
            return new GeneratedLoadOnDemandChunkFileRegistry("",
                    "",
                    "",
                    "",
                    GENERATED_LAZY_CHUNK_FILE_REGISTRY_SOURCE,
                    GENERATED_LAZY_CHUNK_FILE_MAPPING_SOURCE,
                    generatedImportsArtifact,
                    noOp,
                    noOp ? GENERATED_LAZY_CHUNK_FILE_NOT_GENERATED_REASON
                            : "generated-flow-imports did not match a supported loadOnDemand chunk table shape",
                    sourceBuildMode,
                    sourceArtifactFamily,
                    noOp ? "no-op-promise-resolve-0" : "unrecognized",
                    GENERATED_LAZY_CHUNK_FILE_RESOLVER,
                    noOp ? "not-generated" : "unrecognized",
                    0,
                    "",
                    noOp ? "chunk-file-not-generated" : "chunk-registry-unrecognized",
                    noOp ? GENERATED_LAZY_CHUNK_FILE_NEXT_RESOLUTION_STEP
                            : "extend the parser for the generated-flow-imports table shape emitted by this Vaadin version",
                    noOp ? "no-chunk-generated-by-real-generated-flow-imports-no-op-table"
                            : "loadondemand-table-shape-unrecognized");
        }

        private static String sourceBuildMode(String path) {
            if (path.startsWith("vaadin-dev-bundle/")) {
                return "vaadin-dev-bundle-production-build";
            }
            if (path.startsWith("META-INF/vatori/generated/")) {
                return "repository-generated-flow-fixture";
            }
            return "classpath-resource";
        }

        private static String resolveRelativeChunkPath(String generatedImportsArtifact, String specifier) {
            int lastSlash = generatedImportsArtifact.lastIndexOf('/');
            String parent = lastSlash < 0 ? "" : generatedImportsArtifact.substring(0, lastSlash + 1);
            String relative = specifier.startsWith("./") ? specifier.substring(2) : specifier;
            return parent + relative;
        }

        private static byte[] classpathResourceBytes(String path) {
            if (path == null || path.isBlank()) {
                return new byte[0];
            }
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (loader == null) {
                loader = FlowClientResources.class.getClassLoader();
            }
            try (InputStream stream = loader.getResourceAsStream(path)) {
                return stream == null ? new byte[0] : stream.readAllBytes();
            } catch (IOException error) {
                throw new IllegalStateException("Unable to read generated loadOnDemand chunk " + path, error);
            }
        }
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
