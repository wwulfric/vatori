package dev.vatori.desktop.flow.ipc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
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
    private static final String DISABLE_ON_CLICK_FUNCTIONS_JS =
            "META-INF/resources/frontend/disableOnClickFunctions.js";
    private static final String TEXT_FIELD_SHIM_JS =
            "META-INF/vatori/frontend-shims/vaadin-text-field.js";
    private static final String DEV_BUNDLE_COMMONJS_JS =
            "vaadin-dev-bundle/webapp/VAADIN/build/commonjsHelpers-CUmg6egw.js";
    private static final String DEV_BUNDLE_BUILD_ROOT =
            "vaadin-dev-bundle/webapp/VAADIN/build/";
    private static final String DEV_BUNDLE_INDEX_HTML_JS =
            "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js";
    private static final String DEV_BUNDLE_COPILOT_LOG_PLUGIN_JS =
            "vaadin-dev-bundle/webapp/VAADIN/build/copilot-log-plugin-h3VQq9-2-BlujgixT.js";
    private static final String DEV_BUNDLE_COPILOT_INFO_PLUGIN_JS =
            "vaadin-dev-bundle/webapp/VAADIN/build/copilot-info-plugin-sfSeRBmW-DQ0SanHi.js";
    private static final String DEV_BUNDLE_COPILOT_FEATURES_PLUGIN_JS =
            "vaadin-dev-bundle/webapp/VAADIN/build/copilot-features-plugin-Bclmq7gl-NZmEZo5L.js";
    private static final String DEV_BUNDLE_COPILOT_FEEDBACK_PLUGIN_JS =
            "vaadin-dev-bundle/webapp/VAADIN/build/copilot-feedback-plugin-1zAxYjpE-BgRBGxgc.js";
    private static final String DEV_BUNDLE_COPILOT_SETTINGS_PANEL_JS =
            "vaadin-dev-bundle/webapp/VAADIN/build/copilot-settings-panel-ZoFKAdB8-B8JXXb5r.js";
    private static final String DEV_BUNDLE_COPILOT_IMPERSONATOR_PLUGIN_JS =
            "vaadin-dev-bundle/webapp/VAADIN/build/copilot-impersonator-plugin-DcZdAFXt-DdRy9h0o.js";
    private static final String DEV_BUNDLE_COPILOT_DEVELOPMENT_SETUP_GUIDE_JS =
            "vaadin-dev-bundle/webapp/VAADIN/build/copilot-development-setup-user-guide-Bg1Jpy5v-DVg5L7gy.js";
    private static final String DEV_BUNDLE_BASE_PANEL_JS =
            "vaadin-dev-bundle/webapp/VAADIN/build/base-panel-Djw-d72n-DCiGU9ul.js";
    private static final String DEV_BUNDLE_COPILOT_FOCUS_TRAP_JS =
            "vaadin-dev-bundle/webapp/VAADIN/build/copilot-focus-trap-D6_tO8dw-BKIBibXo.js";
    private static final String DEV_BUNDLE_TYPESCRIPT_JS =
            "vaadin-dev-bundle/webapp/VAADIN/build/typescript-jmYPauaZ-CsDy5M4U.js";
    private static final String DEV_BUNDLE_COPILOT_GLOBAL_VARS_LATER_JS =
            "vaadin-dev-bundle/webapp/VAADIN/build/copilot-global-vars-later-DKkGjSH--ij_B3zsO.js";
    private static final String DEV_BUNDLE_COPILOT_INIT_STEP2_JS =
            "vaadin-dev-bundle/webapp/VAADIN/build/copilot-init-step2-D0JX2IbT-DHUx_SJy.js";
    private static final String DEV_BUNDLE_REPEAT_JS =
            "vaadin-dev-bundle/webapp/VAADIN/build/repeat-B6aSW1iL-LbvYsU-M.js";
    private static final String DEV_BUNDLE_LIT_RENDERER_JS =
            "vaadin-dev-bundle/webapp/VAADIN/build/lit-renderer-CWzNMnPU-CUV1j_pj.js";
    private static final String DEV_BUNDLE_DIRECTIVE_HELPERS_JS =
            "vaadin-dev-bundle/webapp/VAADIN/build/directive-helpers-BuSA7Eqp-CCzZr0Zr.js";
    private static final String DEV_BUNDLE_STATE_JS =
            "vaadin-dev-bundle/webapp/VAADIN/build/state-aTUQA9pO-DwC6YZCk.js";
    private static final String DEV_BUNDLE_ICONS_JS =
            "vaadin-dev-bundle/webapp/VAADIN/build/icons-CJYK-fIm-Bt18GX7w.js";
    private static final String DEV_BUNDLE_GENERATED_FLOW_IMPORTS_JS =
            "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js";
    private static final String DEV_BUNDLE_MARKDOWN_HELPERS_JS =
            "vaadin-dev-bundle/webapp/VAADIN/build/markdown-helpers-31I0A-hg.js";
    private static final String DEV_BUNDLE_FLOW_BOOTSTRAP_CHUNK_JS =
            "vaadin-dev-bundle/webapp/VAADIN/build/FlowBootstrap-r4imYSsC.js";
    private static final String DEV_BUNDLE_FLOW_CLIENT_CHUNK_JS =
            "vaadin-dev-bundle/webapp/VAADIN/build/FlowClient-RTzSA_ve.js";
    private static final String DEV_BUNDLE_COPILOT_DEVTOOLS_JS =
            "vaadin-dev-bundle/webapp/VAADIN/build/copilot-devtools-Cpd_CV50-CKw1U-T_.js";
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
    private static final String GENERATED_FLOW_IMPORTS_V1_22_JS =
            "META-INF/vatori/generated/flow/generated-flow-imports-v1_22.js";
    private static final String GENERATED_FLOW_IMPORTS_CHUNK_V1_22_JS =
            "META-INF/vatori/generated/flow/chunks/chunk-v1_22.js";
    private static final String DEV_MODE_PACKAGE_LOCK_JSON =
            "vaadin-dev-bundle/package-lock.json";
    private static final String DEV_BUNDLE_CHECKBOX_ENTRY_JS =
            "META-INF/vatori/dev-bundle-entry/vaadin-checkbox.js";
    private static final String DEV_BUNDLE_SELECT_ENTRY_JS =
            "META-INF/vatori/dev-bundle-entry/vaadin-select.js";
    private static final String LOGIN_FORM_COMPONENT_CLASS =
            "com.vaadin.flow.component.login.LoginForm";
    private static final String LOGIN_FORM_TAG = "vaadin-login-form";
    private static final String LOGIN_FORM_REQUESTED_MODULE =
            "@vaadin/login/src/vaadin-login-form.js";
    private static final String LOGIN_FORM_PRODUCTION_BUILD_RESOURCE_VERSION =
            "flow-ipc-login-form-production-build-resource-v1_101";
    private static final String THEME_STATIC_CSS =
            "META-INF/resources/frontend/themes/vatori-flow-ipc-smoke/theme-static.css";
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
    private static final String GENERATED_LAZY_APP_BUILD_OUTPUT_VERSION =
            "flow-ipc-generated-lazy-app-build-output-v1_29";
    private static final String GENERATED_LAZY_APP_BUILD_OUTPUT_DISCOVERY_SOURCE =
            "dev-ipc-module-target-generated-imports-discovery-v1_29";
    private static final String GENERATED_LAZY_APP_BUILD_OUTPUT_FAILURE_LAYER =
            "app-build-output-missing-generated-imports";
    private static final String GENERATED_LAZY_APP_BUILD_OUTPUT_NEXT_STEP =
            "run Vaadin prepare-frontend/update-imports for the dev-ipc app module or connect a supported Vaadin lazy chunk SPI so generated-flow-imports comes from current app build output instead of packaged dev-bundle fallback";
    private static final String CURRENT_APP_FRONTEND_BUILD_OUTPUT_VERSION =
            "flow-ipc-current-app-frontend-build-output-v1_30";
    private static final String CURRENT_APP_FRONTEND_BUILD_DISCOVERY_SOURCE =
            "dev-ipc-current-app-frontend-build-output-materialization-v1_30";
    private static final String CURRENT_APP_FRONTEND_BUILD_NEXT_STEP =
            "make the Vaadin frontend build step produce generated-flow-imports/chunks for the current dev-ipc app, or replace the probe with a supported Vaadin lazy chunk SPI before removing packaged dev-bundle fallback";
    private static final String FRONTEND_BUILD_PREFLIGHT_AVAILABILITY_VERSION =
            "flow-ipc-frontend-build-preflight-availability-v1_31";
    private static final String FRONTEND_BUILD_PREFLIGHT_AVAILABILITY_SOURCE =
            "dev-ipc-vaadin-maven-plugin-availability-v1_31";
    private static final String FRONTEND_BUILD_PREFLIGHT_NEXT_STEP =
            "run a full Vaadin frontend build/package step or connect a supported Vaadin lazy chunk SPI so current app generated-flow-imports/chunks become resolver-owned runtime artifacts";
    private static final String CURRENT_APP_BUILD_FRONTEND_OUTPUT_VERSION =
            "flow-ipc-current-app-build-frontend-output-v1_32";
    private static final String CURRENT_APP_BUILD_FRONTEND_OUTPUT_SOURCE =
            "dev-ipc-current-app-build-frontend-output-contract-v1_32";
    private static final String CURRENT_APP_BUILD_FRONTEND_GENERATED_IMPORTS_JS =
            "current-app-frontend/generated/flow/generated-flow-imports.js";
    private static final String CURRENT_APP_BUILD_FRONTEND_GENERATED_CHUNK_ROOT =
            "current-app-frontend/generated/flow/";
    private static final String CURRENT_APP_PRODUCTION_BUILD_VIRTUAL_ROOT =
            "current-app-production/VAADIN/build/";
    private static final String CURRENT_APP_BUILD_FRONTEND_NEXT_STEP =
            "stage the current app generated-flow-imports/chunk through the no-localhost resolver and add a supported resolver for any remaining npm/bare specifier or production owner import boundary";
    private static final String CURRENT_APP_GENERATED_CHUNK_PACKAGE_RESOLVER_VERSION =
            "flow-ipc-current-app-generated-chunk-package-resolver-v1_33";
    private static final String CURRENT_APP_GENERATED_CHUNK_PACKAGE_RESOLVER_SOURCE =
            "current-app-source-generated-chunk";
    private static final String CURRENT_APP_GENERATED_CHUNK_PACKAGE_RESOLVER_MODE =
            "current-app-source-chunk-package-edge-to-production-indexhtml";
    private static final String CURRENT_APP_GENERATED_CHUNK_RESOLVED_SPECIFIERS =
            "@vaadin/vertical-layout/src/vaadin-vertical-layout.js";
    private static final String CURRENT_APP_GENERATED_CHUNK_PACKAGE_FAILURE_LAYER =
            "current-app-source-generated-imports-root-package-graph-pending";
    private static final String CURRENT_APP_GENERATED_CHUNK_PACKAGE_NEXT_STEP =
            "replace the remaining generated imports root package placeholder with real package graph resources, then promote the current app generated imports/chunk path from diagnostic execution to Flow-client-owned runtime";
    private static final String CURRENT_APP_GENERATED_IMPORTS_ROOT_PACKAGE_GRAPH_VERSION =
            "flow-ipc-current-app-generated-imports-root-package-graph-v1_34";
    private static final String CURRENT_APP_GENERATED_IMPORTS_ROOT_PACKAGE_GRAPH_SOURCE =
            "current-app-source-generated-imports-root";
    private static final String CURRENT_APP_GENERATED_IMPORTS_ROOT_PACKAGE_RESOLVER_MODE =
            "current-app-source-generated-imports-root-to-current-app-production-graph";
    private static final String CURRENT_APP_GENERATED_IMPORTS_ROOT_FAILURE_LAYER =
            "current-app-generated-imports-root-package-graph-resolved-production-owner-pending";
    private static final String CURRENT_APP_GENERATED_IMPORTS_ROOT_NEXT_STEP =
            "promote the current app production generated imports support graph toward production owner-owned runtime side effects, then cover dev-mode/Vite or a supported Vaadin frontend SPI";
    private static final String CURRENT_APP_PRODUCTION_RUNTIME_OWNERSHIP_VERSION =
            "flow-ipc-current-app-production-runtime-ownership-v1_35";
    private static final String CURRENT_APP_PRODUCTION_RUNTIME_OWNERSHIP_SOURCE =
            "current-app-build-frontend-output-v1_32-to-v1_34";
    private static final String CURRENT_APP_PRODUCTION_RUNTIME_OWNERSHIP_PRIMARY_CANDIDATE =
            "current-app-build-frontend-production-runtime";
    private static final String CURRENT_APP_PRODUCTION_RUNTIME_OWNERSHIP_HANDOFF_POLICY =
            "current-app-source-root-chunk-plus-production-support-to-owner-runtime-v1_35";
    private static final String CURRENT_APP_PRODUCTION_RUNTIME_OWNERSHIP_FAILURE_LAYER =
            "current-app-production-runtime-ownership-handoff-pending";
    private static final String CURRENT_APP_PRODUCTION_RUNTIME_OWNERSHIP_NEXT_STEP =
            "connect the current app production runtime candidate to owner import, side-effect and component reuse, or replace the handoff with a supported Vaadin build SPI";
    private static final String PACKAGING_FRONTEND_INVENTORY_VERSION =
            "flow-ipc-packaging-time-frontend-inventory-v1_44";
    private static final String PACKAGING_FRONTEND_INVENTORY_SOURCE =
            "generated-lazy-spi-decision-v1_43";
    private static final String PACKAGING_FRONTEND_INVENTORY_PRODUCER =
            "vaadin-maven-plugin-build-frontend";
    private static final String PACKAGING_FRONTEND_INVENTORY_MODE =
            "framework-managed-current-app-build-output";
    private static final String PACKAGING_FRONTEND_INVENTORY_REQUIRED_ARTIFACTS =
            "source-generated-imports,source-generated-chunk,production-generated-imports-support,production-entry,production-build-graph,stats-json,prod-bundle";
    private static final String PACKAGING_FRONTEND_INVENTORY_LOADER_POLICY =
            "custom-scheme-file-backed-primary-with-blob-diagnostic-fallback";
    private static final String PACKAGING_FRONTEND_INVENTORY_ALLOWED_SCHEMES =
            "vatori-resource,blob";
    private static final String PACKAGING_FRONTEND_INVENTORY_INTEGRITY_POLICY =
            "sha256-and-byte-count-for-selected-artifacts";
    private static final String PACKAGING_FRONTEND_INVENTORY_IDENTITY_POLICY =
            "stable-source-path-plus-owner-namespace";
    private static final String PACKAGING_FRONTEND_INVENTORY_CACHE_POLICY =
            "window-lifetime-cache-by-stable-resource-path";
    private static final String PACKAGING_FRONTEND_INVENTORY_CLEANUP_POLICY =
            "delete-staged-files-on-window-close";
    private static final String PACKAGING_FRONTEND_INVENTORY_HANDOFF_POLICY =
            "generated-imports-owner-runtime-contract-v1_42";
    private static final String PACKAGING_FRONTEND_INVENTORY_REMAINING_BLOCKERS =
            "dev-mode-vite-parity,broad-component-resource-parity,installer-packaging,durable-reconnect";
    private static final String PACKAGING_FRONTEND_INVENTORY_NEXT_STEP =
            "turn the framework-managed build-frontend inventory into a reusable packaging/runtime handoff instead of a smoke-app-only trace contract";
    private static final String PACKAGING_RUNTIME_HANDOFF_REUSE_VERSION =
            "flow-ipc-packaging-runtime-handoff-reuse-v1_45";
    private static final String PACKAGING_RUNTIME_HANDOFF_REUSE_SOURCE =
            "packaging-time-frontend-inventory-v1_44";
    private static final String PACKAGING_RUNTIME_HANDOFF_REUSE_SCOPE =
            "module-relative-build-frontend-inventory-to-runtime-owner";
    private static final String PACKAGING_RUNTIME_HANDOFF_REUSE_ARTIFACT_BASE_POLICY =
            "module-relative-source-and-target-output";
    private static final String PACKAGING_RUNTIME_HANDOFF_REUSE_RUNTIME_CONTRACTS =
            "packaging-inventory-v1_44,generated-imports-owner-runtime-contract-v1_42,host-owned-custom-scheme-loader-v1_17";
    private static final String PACKAGING_RUNTIME_HANDOFF_REUSE_VALIDATION_TARGETS =
            "strict-smoke-app,framework-validation-app";
    private static final String PACKAGING_RUNTIME_HANDOFF_REUSE_REMAINING_BLOCKERS =
            "dev-mode-vite-parity,broad-component-resource-parity,installer-packaging,durable-reconnect";
    private static final String PACKAGING_RUNTIME_HANDOFF_REUSE_NEXT_STEP =
            "lift the reusable runtime handoff into packaging artifacts and broaden component/resource coverage beyond the smoke/framework validation matrix";
    private static final List<String> PRODUCTION_RUNTIME_OWNER_DEPENDENCY_ARTIFACTS = List.of(
            DEV_BUNDLE_COMMONJS_JS,
            DEV_BUNDLE_INDEX_HTML_JS,
            DEV_BUNDLE_COPILOT_LOG_PLUGIN_JS,
            DEV_BUNDLE_COPILOT_INFO_PLUGIN_JS,
            DEV_BUNDLE_COPILOT_FEATURES_PLUGIN_JS,
            DEV_BUNDLE_COPILOT_FEEDBACK_PLUGIN_JS,
            DEV_BUNDLE_COPILOT_SETTINGS_PANEL_JS,
            DEV_BUNDLE_COPILOT_IMPERSONATOR_PLUGIN_JS,
            DEV_BUNDLE_COPILOT_DEVELOPMENT_SETUP_GUIDE_JS,
            DEV_BUNDLE_BASE_PANEL_JS,
            DEV_BUNDLE_COPILOT_FOCUS_TRAP_JS,
            DEV_BUNDLE_TYPESCRIPT_JS,
            DEV_BUNDLE_COPILOT_GLOBAL_VARS_LATER_JS,
            DEV_BUNDLE_COPILOT_INIT_STEP2_JS,
            DEV_BUNDLE_REPEAT_JS,
            DEV_BUNDLE_LIT_RENDERER_JS,
            DEV_BUNDLE_DIRECTIVE_HELPERS_JS,
            DEV_BUNDLE_STATE_JS,
            DEV_BUNDLE_ICONS_JS,
            DEV_BUNDLE_GENERATED_FLOW_IMPORTS_JS,
            DEV_BUNDLE_MARKDOWN_HELPERS_JS,
            DEV_BUNDLE_FLOW_BOOTSTRAP_CHUNK_JS,
            DEV_BUNDLE_FLOW_CLIENT_CHUNK_JS,
            DEV_BUNDLE_COPILOT_DEVTOOLS_JS);
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
            import { c7 as TextField } from './indexhtml-BfYYZi8S.js';
            import { defineCustomElement } from './component-base-define.js';

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
              const versionDescriptor = Object.getOwnPropertyDescriptor(CustomElement, 'version');
              if (!versionDescriptor || versionDescriptor.configurable) {
                Object.defineProperty(CustomElement, 'version', {
                  configurable: true,
                  get() {
                    return version;
                  }
                });
              }
              if (CustomElement.experimental) {
                const featureFlagKey = typeof CustomElement.experimental === 'string'
                  ? CustomElement.experimental
                  : `${dashToCamelCase(CustomElement.is.split('-').slice(1).join('-'))}Component`;
                if (!window.Vaadin.featureFlags[featureFlagKey] && !experimentalMap[featureFlagKey]) {
                  experimentalMap[featureFlagKey] = new Set();
                  experimentalMap[featureFlagKey].add(CustomElement);
                  const featureFlagDescriptor = Object.getOwnPropertyDescriptor(window.Vaadin.featureFlags, featureFlagKey);
                  if (!featureFlagDescriptor || featureFlagDescriptor.configurable) {
                    Object.defineProperty(window.Vaadin.featureFlags, featureFlagKey, {
                      configurable: true,
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
                  }
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
            import { c5 as Checkbox } from './indexhtml-BfYYZi8S.js';
            import { defineCustomElement } from './component-base-define.js';

            defineCustomElement(Checkbox);

            export { Checkbox };
            """;
    private static final String SELECT_DEV_BUNDLE_ENTRY_SOURCE = """
            import { c6 as Select } from './indexhtml-BfYYZi8S.js';
            import { defineCustomElement } from './component-base-define.js';

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
        FlowIpcComponentResourceAttributes.Config resourceAttributeConfig =
                componentResourceAttributeConfig();
        add(discoveredComponentResources, BIG_DECIMAL_FIELD_JS, "vaadin-web-component-module", true,
                FlowIpcComponentResourceAttributes.classpathComponent(resourceAttributeConfig, false,
                        "com.vaadin.flow.component.textfield.BigDecimalField",
                        "vaadin-big-decimal-field",
                        "./vaadin-big-decimal-field.js",
                        "classpath-frontend-resource"));
        add(discoveredComponentResources, DISABLE_ON_CLICK_FUNCTIONS_JS,
                "vaadin-generated-jar-resource-disable-on-click", true,
                FlowIpcComponentResourceAttributes.currentAppGeneratedImportsSupport(resourceAttributeConfig,
                        DISABLE_ON_CLICK_FUNCTIONS_JS,
                        "Frontend/generated/jar-resources/disableOnClickFunctions.js",
                        "current-app-generated-imports-disable-on-click-v1_33",
                        false));
        addText(discoveredComponentResources, TEXT_FIELD_SHIM_JS, "vaadin-web-component-import-shim",
                TEXT_FIELD_SHIM_SOURCE,
                FlowIpcComponentResourceAttributes.classpathComponent(resourceAttributeConfig, true,
                        "com.vaadin.flow.component.textfield.TextField",
                        "vaadin-text-field",
                        "@vaadin/text-field/src/vaadin-text-field.js",
                        "v1_2-inline-shim"));
        add(discoveredComponentResources, DEV_BUNDLE_COMMONJS_JS, "vaadin-dev-bundle-support-module", true,
                FlowIpcComponentResourceAttributes.checkboxDevBundle(
                        resourceAttributeConfig, "", DEV_BUNDLE_COMMONJS_JS, false));
        add(discoveredComponentResources, DEV_BUNDLE_INDEX_HTML_JS, "vaadin-dev-bundle-support-module", true,
                FlowIpcComponentResourceAttributes.checkboxDevBundle(
                        resourceAttributeConfig, "", DEV_BUNDLE_INDEX_HTML_JS, false));
        add(discoveredComponentResources, DEV_BUNDLE_STATS_JSON, "vaadin-production-bundle-stats", true,
                FlowIpcComponentResourceAttributes.productionBundleStats(resourceAttributeConfig));
        addText(discoveredComponentResources, DEV_BUNDLE_LIT_ENTRY_JS,
                "vaadin-dev-bundle-lit-css-entry", LIT_DEV_BUNDLE_ENTRY_SOURCE,
                FlowIpcComponentResourceAttributes.litDevBundle(resourceAttributeConfig));
        addText(discoveredComponentResources, DEV_BUNDLE_TEXT_FIELD_ENTRY_JS,
                "vaadin-dev-bundle-text-field-entry", TEXT_FIELD_DEV_BUNDLE_ENTRY_SOURCE,
                FlowIpcComponentResourceAttributes.textFieldDevBundle(resourceAttributeConfig));
        addText(discoveredComponentResources, DEV_BUNDLE_DEFINE_ENTRY_JS,
                "vaadin-dev-bundle-define-entry", DEFINE_DEV_BUNDLE_ENTRY_SOURCE,
                FlowIpcComponentResourceAttributes.defineDevBundle(resourceAttributeConfig));
        add(discoveredComponentResources, GENERATED_FLOW_IMPORTS_JS,
                "vaadin-generated-flow-imports-module", true,
                generatedLazyAttributes());
        add(discoveredComponentResources, GENERATED_FLOW_IMPORTS_V1_22_JS,
                "vaadin-generated-flow-imports-v1_22-module", true,
                generatedLazyChunkFileFixtureAttributes(true));
        add(discoveredComponentResources, GENERATED_FLOW_IMPORTS_CHUNK_V1_22_JS,
                "vaadin-generated-flow-imports-v1_22-chunk", true,
                generatedLazyChunkFileFixtureAttributes(false));
        addCurrentAppBuildFrontendGeneratedLazyResources(discoveredComponentResources);
        add(discoveredComponentResources, FlowIpcProductionOwnerAttributes.FIXTURE_ROOT,
                "vaadin-production-owner-positive-fixture-root-v1_23", true,
                productionOwnerPositiveFixtureAttributes(true));
        add(discoveredComponentResources, FlowIpcProductionOwnerAttributes.FIXTURE_DEPENDENCY,
                "vaadin-production-owner-positive-fixture-dependency-v1_23", true,
                productionOwnerPositiveFixtureAttributes(false));
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
                FlowIpcStaticResourceAttributes.devModePackageLock(DEV_MODE_PACKAGE_LOCK_JSON));
        addText(discoveredComponentResources, DEV_BUNDLE_CHECKBOX_ENTRY_JS,
                "vaadin-dev-bundle-checkbox-entry", CHECKBOX_DEV_BUNDLE_ENTRY_SOURCE,
                FlowIpcComponentResourceAttributes.checkboxDevBundle(
                        resourceAttributeConfig, "vaadin-checkbox", DEV_BUNDLE_CHECKBOX_ENTRY_JS, true));
        addText(discoveredComponentResources, DEV_BUNDLE_SELECT_ENTRY_JS,
                "vaadin-dev-bundle-select-entry", SELECT_DEV_BUNDLE_ENTRY_SOURCE,
                FlowIpcComponentResourceAttributes.devBundle(resourceAttributeConfig,
                        "com.vaadin.flow.component.select.Select",
                        "vaadin-select",
                        DEV_BUNDLE_SELECT_ENTRY_JS,
                        true,
                        "Select-dev-bundle-resource-v1_4",
                        "com.vaadin.flow.component.select.Select"));
        add(discoveredComponentResources, THEME_STATIC_CSS, "theme-static-resource", true,
                FlowIpcStaticResourceAttributes.themeStatic(THEME_STATIC_CSS));
        addText(discoveredComponentResources, FlowIpcLazyDependencyAttributes.RESOURCE_PATH,
                "flow-lazy-dependency-script", FlowIpcLazyDependencyAttributes.BODY,
                FlowIpcLazyDependencyAttributes.resourceAttributes(RUNTIME_RESOLVER_CONTRACT_VERSION));
        for (String path : PRODUCTION_RUNTIME_OWNER_DEPENDENCY_ARTIFACTS) {
            if (!discoveredComponentResources.containsKey(path)) {
                add(discoveredComponentResources, path, "vaadin-production-owner-dependency", true,
                        productionOwnerDependencyAttributes(path));
            }
        }

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
        manifest.putAll(frontendResourceLoaderAttributes());
        manifest.putAll(frontendResolverContractAttributes());
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
        String loginFormProductionArtifact = FlowIpcBuildOutputScanner.productionBuildArtifactContaining(
                FlowIpcBuildOutputScanner.productionBuildRoot(FlowIpcBuildOutputScanner.appModuleDirectory()),
                LOGIN_FORM_TAG,
                CURRENT_APP_PRODUCTION_BUILD_VIRTUAL_ROOT);
        Map<String, Object> manifest = FlowIpcComponentManifestAttributes.attributes(
                componentManifestContext(entries.size(), loginFormProductionArtifact));
        manifest.put("resources", entries);
        return manifest;
    }

    private static FlowIpcComponentManifestAttributes.Context componentManifestContext(
            int resourceCount, String loginFormProductionArtifact) {
        return new FlowIpcComponentManifestAttributes.Context(
                resourceCount,
                FRONTEND_ASSET_PIPELINE_VERSION,
                FRONTEND_ARTIFACT_GRAPH_SOURCE,
                COMPONENT_RESOLVER_V1_4,
                RUNTIME_RESOLVER_CONTRACT_VERSION,
                GENERATED_LAZY_APP_BUILD_OUTPUT_VERSION,
                COMPONENT_TRANSITIVE_BLOCKED_REASON,
                textFieldContext(),
                new FlowIpcComponentManifestAttributes.RuntimeContext(
                        DEV_BUNDLE_STATS_JSON, DEV_BUNDLE_INDEX_HTML_JS),
                new FlowIpcComponentManifestAttributes.ComponentInventoryContext(
                        BIG_DECIMAL_FIELD_JS,
                        DEV_BUNDLE_CHECKBOX_ENTRY_JS,
                        DEV_BUNDLE_SELECT_ENTRY_JS,
                        DEV_BUNDLE_TEXT_FIELD_ENTRY_JS,
                        loginFormProductionArtifact,
                        new FlowIpcComponentManifestAttributes.LoginFormContext(
                                LOGIN_FORM_COMPONENT_CLASS,
                                LOGIN_FORM_TAG,
                                LOGIN_FORM_REQUESTED_MODULE)),
                FlowIpcGeneratedLazyAttributes.manifestAttributes(
                        generatedLazyContext(), generatedLazyPrimaryDiagnosticsAttributes()),
                productionRuntimeOwnerAttributes());
    }

    private static FlowIpcComponentResourceAttributes.Config componentResourceAttributeConfig() {
        return new FlowIpcComponentResourceAttributes.Config(
                BIG_DECIMAL_FIELD_JS,
                DEV_BUNDLE_BUILD_ROOT,
                DEV_BUNDLE_CHECKBOX_ENTRY_JS,
                DEV_BUNDLE_INDEX_HTML_JS,
                DEV_BUNDLE_STATS_JSON,
                CURRENT_APP_BUILD_FRONTEND_GENERATED_IMPORTS_JS,
                CURRENT_APP_GENERATED_CHUNK_PACKAGE_RESOLVER_VERSION,
                CURRENT_APP_GENERATED_CHUNK_PACKAGE_RESOLVER_SOURCE,
                CURRENT_APP_GENERATED_CHUNK_PACKAGE_RESOLVER_MODE,
                CURRENT_APP_GENERATED_CHUNK_PACKAGE_FAILURE_LAYER,
                CURRENT_APP_GENERATED_CHUNK_PACKAGE_NEXT_STEP,
                RUNTIME_RESOLVER_CONTRACT_VERSION,
                COMPONENT_TRANSITIVE_BLOCKED_REASON,
                DEV_BUNDLE_COMPONENT_REASON,
                textFieldContext(),
                productionOwnerContext());
    }

    private static FlowIpcComponentManifestAttributes.TextFieldContext textFieldContext() {
        return new FlowIpcComponentManifestAttributes.TextFieldContext(
                TEXT_FIELD_GRAPH_PARITY_VERSION,
                TEXT_FIELD_PREVIOUS_BLOCKER_VERSION,
                TEXT_FIELD_EXECUTION_VERSION,
                TEXT_FIELD_SIDE_EFFECT_VERSION,
                TEXT_FIELD_GRAPH_PROBE,
                TEXT_FIELD_IMPORT_RESOLUTION_STRATEGY,
                TEXT_FIELD_SIDE_EFFECT_STRATEGY,
                TEXT_FIELD_NARROWED_FAILURE_CLASSIFICATION,
                TEXT_FIELD_FAILURE_LAYER,
                TEXT_FIELD_SHIM_JS,
                DEV_BUNDLE_TEXT_FIELD_ENTRY_JS,
                TEXT_FIELD_CLASS_EXPORT_ARTIFACT,
                List.of(DEV_BUNDLE_INDEX_HTML_JS, DEV_BUNDLE_COMMONJS_JS),
                DEV_BUNDLE_LIT_ENTRY_JS,
                DEV_BUNDLE_DEFINE_ENTRY_JS,
                TEXT_FIELD_FIRST_DEFINITION_SOURCE,
                TEXT_FIELD_SECOND_DEFINITION_SOURCE,
                TEXT_FIELD_DUPLICATE_TAG,
                TEXT_FIELD_REMAINING_BLOCKER_CLASSIFICATION,
                TEXT_FIELD_MINIMAL_REPRODUCTION,
                TEXT_FIELD_NEXT_RESOLUTION_STEP);
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
        add(target, path, kind, text, FlowIpcStaticResourceAttributes.flowClient(path, kind));
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

    private void addFile(Map<String, Resource> target, String virtualPath, Path file, String kind,
            boolean text, Map<String, Object> attributes) {
        if (file == null || !Files.isRegularFile(file)) {
            return;
        }
        try {
            target.put(virtualPath, new Resource(virtualPath, kind, Files.readAllBytes(file), text, false,
                    attributes));
        } catch (IOException error) {
            throw new IllegalStateException("Unable to read current app frontend build output " + file, error);
        }
    }

    private void addCurrentAppBuildFrontendGeneratedLazyResources(Map<String, Resource> target) {
        CurrentAppBuildFrontendOutputEvidence evidence = currentAppBuildFrontendOutputEvidence();
        if (!evidence.sourceGeneratedImportsObserved()) {
            return;
        }
        Path sourceGeneratedImports = FlowIpcBuildOutputScanner.sourceGeneratedImportsPath(FlowIpcBuildOutputScanner.appModuleDirectory());
        addFile(target,
                CURRENT_APP_BUILD_FRONTEND_GENERATED_IMPORTS_JS,
                sourceGeneratedImports,
                "vaadin-current-app-generated-flow-imports",
                true,
                currentAppBuildFrontendGeneratedLazyAttributes(evidence, true));
        if ("available".equals(evidence.sourceChunkStatus()) && !evidence.sourceChunkVirtualPath().isBlank()) {
            Path sourceChunk = sourceGeneratedImports.getParent()
                    .resolve(evidence.sourceChunkImportSpecifier().replaceFirst("^\\./", ""))
                    .normalize();
            addFile(target,
                    evidence.sourceChunkVirtualPath(),
                    sourceChunk,
                    "vaadin-current-app-generated-flow-imports-chunk",
                    true,
                    currentAppBuildFrontendGeneratedLazyAttributes(evidence, false));
        }
        addCurrentAppProductionBuildResources(target, evidence);
    }

    private void addCurrentAppProductionBuildResources(Map<String, Resource> target,
            CurrentAppBuildFrontendOutputEvidence evidence) {
        Path productionBuildRoot = FlowIpcBuildOutputScanner.productionBuildRoot(FlowIpcBuildOutputScanner.appModuleDirectory());
        if (!Files.isDirectory(productionBuildRoot)) {
            return;
        }
        String loginFormArtifact = FlowIpcBuildOutputScanner.productionBuildArtifactContaining(
                productionBuildRoot,
                LOGIN_FORM_TAG,
                CURRENT_APP_PRODUCTION_BUILD_VIRTUAL_ROOT);
        for (Path artifact : FlowIpcBuildOutputScanner.productionBuildJavaScriptArtifacts(productionBuildRoot)) {
            String virtualPath = FlowIpcBuildOutputScanner.productionBuildVirtualPath(
                    artifact,
                    CURRENT_APP_PRODUCTION_BUILD_VIRTUAL_ROOT);
            addFile(target,
                    virtualPath,
                    artifact,
                    "vaadin-current-app-production-build-resource",
                    true,
                    currentAppProductionBuildAttributes(evidence, virtualPath,
                            virtualPath.equals(loginFormArtifact)));
        }
    }

    private Resource read(String path, String kind, boolean text) {
        return read(path, kind, text, FlowIpcStaticResourceAttributes.flowClient(path, kind));
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

    private static Map<String, Object> productionRuntimeOwnerAttributes() {
        return FlowIpcProductionOwnerAttributes.ownerAttributes(productionOwnerContext());
    }

    private static Map<String, Object> productionOwnerDependencyAttributes(String path) {
        return FlowIpcProductionOwnerAttributes.dependencyAttributes(path, productionOwnerContext());
    }

    private static FlowIpcProductionOwnerAttributes.Context productionOwnerContext() {
        return new FlowIpcProductionOwnerAttributes.Context(
                DEV_BUNDLE_BUILD_ROOT,
                DEV_BUNDLE_INDEX_HTML_JS,
                DEV_BUNDLE_COMMONJS_JS,
                DEV_BUNDLE_TEXT_FIELD_ENTRY_JS,
                DEV_BUNDLE_CHECKBOX_ENTRY_JS,
                DEV_BUNDLE_SELECT_ENTRY_JS,
                FlowIpcProductionRuntimeAttributes.dependencyChain(
                        DEV_BUNDLE_STATS_JSON, DEV_BUNDLE_INDEX_HTML_JS),
                PRODUCTION_RUNTIME_OWNER_DEPENDENCY_ARTIFACTS);
    }

    private static Map<String, Object> generatedLazyAttributes() {
        return FlowIpcGeneratedLazyAttributes.repositoryFixtureAttributes(generatedLazyContext());
    }

    private static Map<String, Object> generatedLazyChunkFileFixtureAttributes(boolean importsArtifact) {
        return FlowIpcGeneratedLazyAttributes.chunkFileFixtureAttributes(
                generatedLazyContext(), importsArtifact);
    }

    private static Map<String, Object> currentAppBuildFrontendGeneratedLazyAttributes(
            CurrentAppBuildFrontendOutputEvidence evidence, boolean importsArtifact) {
        return FlowIpcCurrentAppGeneratedLazyResourceAttributes.attributes(
                evidence,
                importsArtifact,
                currentAppGeneratedLazyResourceConfig(),
                currentAppGeneratedImportsRootPackageGraphAttributes(evidence),
                currentAppProductionRuntimeOwnershipAttributes(evidence),
                importsArtifact ? currentAppBuildFrontendOutputAttributes() : Map.of());
    }

    private static FlowIpcCurrentAppGeneratedLazyResourceAttributes.Config
            currentAppGeneratedLazyResourceConfig() {
        return new FlowIpcCurrentAppGeneratedLazyResourceAttributes.Config(
                CURRENT_APP_BUILD_FRONTEND_GENERATED_IMPORTS_JS,
                CURRENT_APP_BUILD_FRONTEND_OUTPUT_VERSION,
                CURRENT_APP_BUILD_FRONTEND_OUTPUT_SOURCE,
                CURRENT_APP_BUILD_FRONTEND_NEXT_STEP,
                CURRENT_APP_GENERATED_CHUNK_PACKAGE_RESOLVER_VERSION,
                CURRENT_APP_GENERATED_CHUNK_PACKAGE_RESOLVER_SOURCE,
                CURRENT_APP_GENERATED_CHUNK_PACKAGE_NEXT_STEP,
                DEV_BUNDLE_GENERATED_FLOW_IMPORTS_JS,
                DEV_BUNDLE_INDEX_HTML_JS,
                DEV_BUNDLE_COMMONJS_JS,
                currentAppFrontendBuildOutputConfig());
    }

    private static Map<String, Object> currentAppGeneratedImportsRootPackageGraphAttributes(
            CurrentAppBuildFrontendOutputEvidence evidence) {
        return FlowIpcCurrentAppGeneratedImportsRootAttributes.attributes(
                evidence,
                currentAppGeneratedImportsRootPackageGraphConfig());
    }

    private static FlowIpcCurrentAppGeneratedImportsRootAttributes.Config
            currentAppGeneratedImportsRootPackageGraphConfig() {
        return new FlowIpcCurrentAppGeneratedImportsRootAttributes.Config(
                CURRENT_APP_GENERATED_IMPORTS_ROOT_PACKAGE_GRAPH_VERSION,
                CURRENT_APP_GENERATED_IMPORTS_ROOT_PACKAGE_GRAPH_SOURCE,
                CURRENT_APP_GENERATED_IMPORTS_ROOT_NEXT_STEP,
                currentAppProductionRuntimeOwnershipConfig());
    }

    private static Map<String, Object> currentAppProductionRuntimeOwnershipAttributes(
            CurrentAppBuildFrontendOutputEvidence evidence) {
        return FlowIpcCurrentAppProductionRuntimeOwnershipAttributes.attributes(
                evidence,
                currentAppProductionRuntimeOwnershipConfig());
    }

    private static FlowIpcCurrentAppProductionRuntimeOwnershipAttributes.Config
            currentAppProductionRuntimeOwnershipConfig() {
        return new FlowIpcCurrentAppProductionRuntimeOwnershipAttributes.Config(
                CURRENT_APP_PRODUCTION_RUNTIME_OWNERSHIP_VERSION,
                CURRENT_APP_PRODUCTION_RUNTIME_OWNERSHIP_SOURCE,
                CURRENT_APP_PRODUCTION_RUNTIME_OWNERSHIP_PRIMARY_CANDIDATE,
                CURRENT_APP_PRODUCTION_RUNTIME_OWNERSHIP_HANDOFF_POLICY,
                CURRENT_APP_PRODUCTION_RUNTIME_OWNERSHIP_FAILURE_LAYER,
                CURRENT_APP_PRODUCTION_RUNTIME_OWNERSHIP_NEXT_STEP,
                CURRENT_APP_BUILD_FRONTEND_GENERATED_IMPORTS_JS,
                new FlowIpcPackagingHandoffAttributes.Config(
                        PACKAGING_FRONTEND_INVENTORY_VERSION,
                        PACKAGING_FRONTEND_INVENTORY_SOURCE,
                        PACKAGING_FRONTEND_INVENTORY_PRODUCER,
                        PACKAGING_FRONTEND_INVENTORY_MODE,
                        PACKAGING_FRONTEND_INVENTORY_REQUIRED_ARTIFACTS,
                        PACKAGING_FRONTEND_INVENTORY_LOADER_POLICY,
                        PACKAGING_FRONTEND_INVENTORY_ALLOWED_SCHEMES,
                        PACKAGING_FRONTEND_INVENTORY_INTEGRITY_POLICY,
                        PACKAGING_FRONTEND_INVENTORY_IDENTITY_POLICY,
                        PACKAGING_FRONTEND_INVENTORY_CACHE_POLICY,
                        PACKAGING_FRONTEND_INVENTORY_CLEANUP_POLICY,
                        PACKAGING_FRONTEND_INVENTORY_HANDOFF_POLICY,
                        PACKAGING_FRONTEND_INVENTORY_REMAINING_BLOCKERS,
                        PACKAGING_FRONTEND_INVENTORY_NEXT_STEP,
                        PACKAGING_RUNTIME_HANDOFF_REUSE_VERSION,
                        PACKAGING_RUNTIME_HANDOFF_REUSE_SOURCE,
                        PACKAGING_RUNTIME_HANDOFF_REUSE_SCOPE,
                        PACKAGING_RUNTIME_HANDOFF_REUSE_ARTIFACT_BASE_POLICY,
                        PACKAGING_RUNTIME_HANDOFF_REUSE_RUNTIME_CONTRACTS,
                        PACKAGING_RUNTIME_HANDOFF_REUSE_VALIDATION_TARGETS,
                        PACKAGING_RUNTIME_HANDOFF_REUSE_REMAINING_BLOCKERS,
                        PACKAGING_RUNTIME_HANDOFF_REUSE_NEXT_STEP,
                        CURRENT_APP_BUILD_FRONTEND_GENERATED_IMPORTS_JS));
    }

    private static Map<String, Object> currentAppProductionBuildAttributes(
            CurrentAppBuildFrontendOutputEvidence evidence, String virtualPath,
            boolean loginFormPrimaryResource) {
        return FlowIpcCurrentAppProductionBuildAttributes.attributes(
                evidence,
                virtualPath,
                loginFormPrimaryResource,
                currentAppProductionBuildConfig(),
                currentAppGeneratedImportsRootPackageGraphAttributes(evidence),
                currentAppBuildFrontendOutputAttributes());
    }

    private static FlowIpcCurrentAppProductionBuildAttributes.Config currentAppProductionBuildConfig() {
        return new FlowIpcCurrentAppProductionBuildAttributes.Config(
                CURRENT_APP_GENERATED_IMPORTS_ROOT_PACKAGE_GRAPH_VERSION,
                CURRENT_APP_GENERATED_IMPORTS_ROOT_NEXT_STEP,
                LOGIN_FORM_COMPONENT_CLASS,
                LOGIN_FORM_TAG,
                LOGIN_FORM_REQUESTED_MODULE,
                LOGIN_FORM_PRODUCTION_BUILD_RESOURCE_VERSION);
    }

    private static Map<String, Object> productionOwnerPositiveFixtureAttributes(boolean rootArtifact) {
        return FlowIpcProductionOwnerAttributes.positiveFixtureAttributes(rootArtifact);
    }

    private static Map<String, Object> generatedLazyDevBundleChunkAttributes(String path, String lookupKind,
            String dependencyOf, boolean primary) {
        return FlowIpcGeneratedLazyAttributes.devBundleChunkAttributes(
                generatedLazyContext(),
                path,
                lookupKind,
                dependencyOf,
                primary,
                primary ? generatedLazyPrimaryDiagnosticsAttributes() : Map.of(),
                productionOwnerContext());
    }

    private static FlowIpcGeneratedLazyAttributes.Context generatedLazyContext() {
        return new FlowIpcGeneratedLazyAttributes.Context(
                GENERATED_FLOW_IMPORTS_JS,
                GENERATED_FLOW_IMPORTS_V1_22_JS,
                GENERATED_FLOW_IMPORTS_CHUNK_V1_22_JS,
                DEV_BUNDLE_GENERATED_FLOW_IMPORTS_JS,
                DEV_BUNDLE_MARKDOWN_HELPERS_JS);
    }

    private static Map<String, Object> generatedLazyPrimaryDiagnosticsAttributes() {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.putAll(FlowIpcGeneratedLazyAttributes.sourceBoundaryAttributes(
                DEV_BUNDLE_GENERATED_FLOW_IMPORTS_JS));
        attributes.putAll(FlowIpcGeneratedLazyBuildDiagnostics.realBuildProbeAttributes(
                DEV_BUNDLE_GENERATED_FLOW_IMPORTS_JS));
        attributes.putAll(FlowIpcGeneratedLazyBuildDiagnostics.buildLifecycleAttributes(
                DEV_BUNDLE_GENERATED_FLOW_IMPORTS_JS));
        attributes.putAll(generatedLazyAppBuildOutputAttributes());
        attributes.putAll(currentAppFrontendBuildOutputAttributes());
        return attributes;
    }

    private static Map<String, Object> generatedLazyAppBuildOutputAttributes() {
        return FlowIpcGeneratedLazyAppBuildOutputAttributes.attributes(
                generatedLazyAppBuildOutputConfig());
    }

    private static FlowIpcGeneratedLazyAppBuildOutputAttributes.Config
            generatedLazyAppBuildOutputConfig() {
        return new FlowIpcGeneratedLazyAppBuildOutputAttributes.Config(
                GENERATED_LAZY_APP_BUILD_OUTPUT_VERSION,
                GENERATED_LAZY_APP_BUILD_OUTPUT_DISCOVERY_SOURCE,
                GENERATED_LAZY_APP_BUILD_OUTPUT_FAILURE_LAYER,
                GENERATED_LAZY_APP_BUILD_OUTPUT_NEXT_STEP,
                DEV_BUNDLE_GENERATED_FLOW_IMPORTS_JS);
    }

    private static Map<String, Object> currentAppFrontendBuildOutputAttributes() {
        return FlowIpcCurrentAppFrontendBuildOutputAttributes.frontendOutputAttributes(
                currentAppFrontendBuildOutputConfig(),
                currentAppBuildFrontendOutputAttributes());
    }

    private static Map<String, Object> currentAppBuildFrontendOutputAttributes() {
        FrontendBuildStepEvidence buildStep = FlowIpcFrontendBuildEvidence.buildStep();
        CurrentAppBuildFrontendOutputEvidence evidence = currentAppBuildFrontendOutputEvidence();
        return FlowIpcCurrentAppFrontendBuildOutputAttributes.buildFrontendOutputAttributes(
                evidence,
                buildStep,
                currentAppFrontendBuildOutputConfig(),
                currentAppGeneratedImportsRootPackageGraphAttributes(evidence));
    }

    private static FlowIpcCurrentAppFrontendBuildOutputAttributes.Config
            currentAppFrontendBuildOutputConfig() {
        return new FlowIpcCurrentAppFrontendBuildOutputAttributes.Config(
                CURRENT_APP_FRONTEND_BUILD_OUTPUT_VERSION,
                CURRENT_APP_FRONTEND_BUILD_DISCOVERY_SOURCE,
                CURRENT_APP_FRONTEND_BUILD_NEXT_STEP,
                FRONTEND_BUILD_PREFLIGHT_AVAILABILITY_VERSION,
                FRONTEND_BUILD_PREFLIGHT_AVAILABILITY_SOURCE,
                FRONTEND_BUILD_PREFLIGHT_NEXT_STEP,
                CURRENT_APP_BUILD_FRONTEND_OUTPUT_VERSION,
                CURRENT_APP_BUILD_FRONTEND_OUTPUT_SOURCE,
                CURRENT_APP_BUILD_FRONTEND_NEXT_STEP,
                CURRENT_APP_GENERATED_CHUNK_PACKAGE_RESOLVER_VERSION,
                CURRENT_APP_GENERATED_CHUNK_PACKAGE_RESOLVER_SOURCE,
                CURRENT_APP_GENERATED_CHUNK_PACKAGE_NEXT_STEP,
                DEV_BUNDLE_GENERATED_FLOW_IMPORTS_JS);
    }

    private static CurrentAppBuildFrontendOutputEvidence currentAppBuildFrontendOutputEvidence() {
        return FlowIpcCurrentAppBuildOutputEvidence.current(currentAppBuildFrontendOutputConfig());
    }

    private static FlowIpcCurrentAppBuildOutputEvidence.Config currentAppBuildFrontendOutputConfig() {
        return new FlowIpcCurrentAppBuildOutputEvidence.Config(
                CURRENT_APP_BUILD_FRONTEND_GENERATED_IMPORTS_JS,
                CURRENT_APP_BUILD_FRONTEND_GENERATED_CHUNK_ROOT,
                CURRENT_APP_PRODUCTION_BUILD_VIRTUAL_ROOT,
                CURRENT_APP_GENERATED_CHUNK_RESOLVED_SPECIFIERS,
                CURRENT_APP_GENERATED_CHUNK_PACKAGE_RESOLVER_MODE,
                CURRENT_APP_GENERATED_IMPORTS_ROOT_PACKAGE_RESOLVER_MODE,
                CURRENT_APP_GENERATED_IMPORTS_ROOT_FAILURE_LAYER);
    }

    private static Map<String, Object> frontendResourceLoaderAttributes() {
        return FlowIpcFrontendPipelineAttributes.resourceLoader();
    }

    private static Map<String, Object> frontendResolverContractAttributes() {
        return FlowIpcFrontendPipelineAttributes.resolverContract();
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
            metadata.put("sha256", FlowIpcDiagnostics.sha256(bytes));
            metadata.put("text", text);
            metadata.put("ipcAugmented", ipcAugmented);
            metadata.putAll(frontendResourceLoaderAttributes());
            metadata.putAll(frontendResolverContractAttributes());
            metadata.putAll(attributes);
            return metadata;
        }

    }
}
