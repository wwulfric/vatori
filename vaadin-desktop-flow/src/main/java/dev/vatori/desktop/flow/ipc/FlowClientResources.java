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
    private static final String PRODUCTION_OWNER_FIXTURE_ROOT_V1_23_JS =
            "META-INF/vatori/production-owner/owner-runtime-v1_23.js";
    private static final String PRODUCTION_OWNER_FIXTURE_DEPENDENCY_V1_23_JS =
            "META-INF/vatori/production-owner/owner-dependency-v1_23.js";
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
    private static final String GENERATED_LAZY_CHUNK_FILE_EXECUTION_VERSION =
            "flow-ipc-loadondemand-chunk-file-execution-v1_22";
    private static final String GENERATED_LAZY_CHUNK_FILE_FIXTURE_SOURCE =
            "repository-vaadin-shaped-generated-flow-imports-v1_22";
    private static final String GENERATED_LAZY_CHUNK_FILE_FIXTURE_CHUNK_ID =
            "vatori-flow-ipc-generated-lazy-chunk-v1_22";
    private static final String GENERATED_LAZY_CHUNK_FILE_FIXTURE_TRIGGER_OWNER =
            "repository-fixture-generated-loadondemand-executor";
    private static final String GENERATED_LAZY_CHUNK_FILE_FIXTURE_SIDE_EFFECT =
            "window.VaadinDesktopGeneratedLazyChunkV122.executed";
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
    private static final String PRODUCTION_RUNTIME_OWNER_IMPORT_VERSION =
            "flow-ipc-production-owner-import-v1_14";
    private static final String PRODUCTION_RUNTIME_OWNER_IMPORT_POLICY =
            "controlled-owner-dynamic-import-after-component-graph-v1_14";
    private static final String PRODUCTION_RUNTIME_OWNER_IMPORT_PHASE =
            "after-component-imports";
    private static final String PRODUCTION_RUNTIME_OWNER_IMPORT_PHASE_CANDIDATES =
            "isolated-preflight,before-component-imports,after-component-imports";
    private static final String PRODUCTION_RUNTIME_OWNER_IMPORT_DIAGNOSTIC_MODES =
            "isolated-preflight:available-not-enabled,before-component-imports:available-not-enabled,after-component-imports:default";
    private static final String PRODUCTION_RUNTIME_OWNER_IMPORT_EXPECTED_SIDE_EFFECTS =
            "window.Vaadin,window.Vaadin.Flow,window.Vaadin.Flow.loadOnDemand,customElements:vaadin-text-field|vaadin-checkbox|vaadin-select";
    private static final String PRODUCTION_RUNTIME_OWNER_IMPORT_ACCEPTABLE_FAILURES =
            "production-owner-relative-import-unresolved,production-owner-browser-import-failed,production-owner-duplicate-custom-element,production-owner-global-collision,production-owner-side-effect-unobservable";
    private static final String PRODUCTION_RUNTIME_OWNER_IMPORT_REWRITE_STRATEGY =
            "rewrite-relative-dev-bundle-dependencies-before-owner-import-v1_14";
    private static final String PRODUCTION_RUNTIME_OWNER_IMPORT_NEXT_STEP =
            "if owner import fails, move the production owner to a custom-scheme/file-backed resolver or tighten dependency rewrite/global collision guards around the real indexhtml artifact";
    private static final String PRODUCTION_RUNTIME_OWNER_DEPENDENCY_RESOLVER_VERSION =
            "flow-ipc-production-owner-deps-v1_15";
    private static final String PRODUCTION_RUNTIME_OWNER_DEPENDENCY_RESOLVER_POLICY =
            "owner-scoped-recursive-relative-import-graph-v1_15";
    private static final String PRODUCTION_RUNTIME_OWNER_DEPENDENCY_RESOLVER_ROOT =
            DEV_BUNDLE_BUILD_ROOT;
    private static final String PRODUCTION_RUNTIME_OWNER_DEPENDENCY_RESOLVER_MODULE_IDENTITY_NAMESPACE =
            "production-owner-deps-v1_15";
    private static final String PRODUCTION_RUNTIME_OWNER_DEPENDENCY_RESOLVER_ACCEPTABLE_FAILURES =
            "production-owner-missing-real-artifact,production-owner-browser-import-failed,production-owner-duplicate-custom-element,production-owner-global-collision,production-owner-side-effect-unobservable";
    private static final String PRODUCTION_RUNTIME_OWNER_DEPENDENCY_RESOLVER_NEXT_STEP =
            "run production owner import after the real dev-bundle relative dependency graph has been resolved, staged and classified";
    private static final String PRODUCTION_RUNTIME_OWNER_IMPORT_ORDERING_VERSION =
            "flow-ipc-production-owner-ordering-v1_16";
    private static final String PRODUCTION_RUNTIME_OWNER_IMPORT_PHASE_POLICY =
            "owner-first-before-component-imports-with-after-component-fallback-v1_16";
    private static final String PRODUCTION_RUNTIME_OWNER_IMPORT_ORDERING_ACCEPTABLE_FAILURES =
            "production-owner-duplicate-custom-element,production-owner-global-collision,production-owner-side-effect-unobservable,production-owner-browser-import-failed,production-owner-component-reuse-contract-missing,production-owner-unsupported-vaadin-production-owner-contract";
    private static final String COMPONENT_PRODUCTION_OWNER_REUSE_VERSION =
            "flow-ipc-component-owner-reuse-v1_16";
    private static final String COMPONENT_PRODUCTION_OWNER_REUSE_POLICY =
            "reuse-owner-owned-custom-elements-before-focused-imports-v1_16";
    private static final String COMPONENT_PRODUCTION_OWNER_REUSE_ACCEPTABLE_FAILURES =
            "owner-import-failed-before-component-import,owner-import-succeeded-but-component-class-export-unreachable,owner-side-effects-global-but-focused-entry-still-required,vaadin-production-artifact-does-not-expose-reusable-owner-contract,blob-module-identity-cannot-safely-merge-owner-and-component-lifetime";
    private static final String FRONTEND_RESOURCE_LOADER_VERSION =
            "flow-ipc-resource-scheme-v1_17";
    private static final String FRONTEND_RESOURCE_LOADER_MODES =
            "custom-scheme,file-backed,blob-fallback";
    private static final String FRONTEND_RESOURCE_LOADER_PREFERRED_MODE =
            "custom-scheme-file-backed";
    private static final String FRONTEND_RESOURCE_LOADER_ACCEPTABLE_FALLBACKS =
            "production-owner-blob-fallback,generated-lazy-blob-fallback,component-complex-graph-blob-fallback";
    private static final String PRODUCTION_RUNTIME_OWNER_RESOURCE_LOADER_VERSION =
            "flow-ipc-production-owner-resource-loader-v1_18";
    private static final String PRODUCTION_RUNTIME_OWNER_RESOURCE_LOADER_MODE =
            "custom-scheme-file-backed";
    private static final String PRODUCTION_RUNTIME_OWNER_RESOURCE_SCHEME = "vatori-resource";
    private static final String PRODUCTION_RUNTIME_OWNER_RESOURCE_GRAPH_MODE =
            "owner-root-and-resolved-dependencies";
    private static final String PRODUCTION_RUNTIME_OWNER_RESOURCE_BASE_POLICY =
            "stable-owner-resource-path-base-v1_18";
    private static final String PRODUCTION_RUNTIME_OWNER_DEPENDENCY_BLOB_FALLBACK_POLICY =
            "explicit-classified-only";
    private static final String PRODUCTION_RUNTIME_OWNER_RESOURCE_LOADER_ACCEPTABLE_FAILURES =
            "production-owner-custom-scheme-missing-real-artifact,production-owner-custom-scheme-duplicate-custom-element,production-owner-custom-scheme-runtime-side-effect-blocked,production-owner-custom-scheme-browser-import-failed";
    private static final String PRODUCTION_RUNTIME_OWNER_RESOURCE_LOADER_NAMESPACE =
            "production-owner";
    private static final String PRODUCTION_RUNTIME_OWNER_SIDE_EFFECT_VERSION =
            "flow-ipc-production-owner-side-effect-v1_19";
    private static final String PRODUCTION_RUNTIME_OWNER_SIDE_EFFECT_POLICY =
            "custom-scheme-owner-import-side-effect-contract-v1_19";
    private static final String PRODUCTION_RUNTIME_OWNER_SIDE_EFFECT_EXPECTED_TAGS =
            "vaadin-text-field,vaadin-checkbox,vaadin-select";
    private static final String PRODUCTION_RUNTIME_OWNER_SIDE_EFFECT_EXPECTED_GLOBALS =
            "window.Vaadin,window.Vaadin.Flow,window.Vaadin.Flow.loadOnDemand";
    private static final String PRODUCTION_RUNTIME_OWNER_SIDE_EFFECT_ACCEPTABLE_FAILURES =
            "production-owner-custom-scheme-runtime-side-effect-blocked,production-owner-custom-scheme-duplicate-custom-element,production-owner-custom-scheme-browser-import-failed,production-owner-custom-scheme-missing-real-artifact,production-owner-custom-scheme-unsupported-vaadin-contract";
    private static final String COMPONENT_PRODUCTION_OWNER_REUSE_DECISION_VERSION =
            "flow-ipc-component-owner-reuse-decision-v1_19";
    private static final String COMPONENT_PRODUCTION_OWNER_REUSE_DECISION_SOURCE =
            "production-owner-custom-scheme-side-effect-v1_19";
    private static final String PRODUCTION_RUNTIME_OWNER_OWNED_RUNTIME_VERSION =
            "flow-ipc-production-owner-owned-runtime-v1_20";
    private static final String PRODUCTION_RUNTIME_OWNER_OWNED_RUNTIME_POLICY =
            "owner-first-custom-scheme-runtime-reuse-v1_20";
    private static final String PRODUCTION_RUNTIME_OWNER_OWNED_RUNTIME_PHASE =
            "before-component-imports";
    private static final String PRODUCTION_RUNTIME_OWNER_OWNED_RUNTIME_ACCEPTABLE_FAILURES =
            "production-owner-owned-side-effect-unobservable,production-owner-owned-duplicate-custom-element,production-owner-owned-missing-real-artifact,production-owner-owned-browser-import-failed,production-owner-owned-unsupported-vaadin-contract";
    private static final String COMPONENT_PRODUCTION_OWNER_REUSE_POLICY_VERSION_V1_20 =
            "flow-ipc-component-owner-reuse-policy-v1_20";
    private static final String COMPONENT_PRODUCTION_OWNER_REUSE_POLICY_V1_20 =
            "owner-first-then-focused-fallback-v1_20";
    private static final String PRODUCTION_OWNER_POSITIVE_FIXTURE_VERSION =
            "flow-ipc-production-owner-positive-runtime-v1_23";
    private static final String PRODUCTION_OWNER_POSITIVE_FIXTURE_SOURCE =
            "repository-production-owner-positive-fixture-v1_23";
    private static final String PRODUCTION_OWNER_POSITIVE_FIXTURE_EXPECTED_TAGS =
            "vatori-production-owner-fixture";
    private static final String PRODUCTION_OWNER_POSITIVE_FIXTURE_EXPECTED_GLOBALS =
            "window.VaadinDesktopProductionOwnerV123";
    private static final String PRODUCTION_OWNER_POSITIVE_FIXTURE_EXECUTION_POLICY =
            "owner-first-custom-scheme-positive-fixture-v1_23";
    private static final String FRONTEND_RESOLVER_CONTRACT_VERSION =
            "flow-ipc-frontend-resolver-contract-v1_21";
    private static final String FRONTEND_RESOLVER_CONTRACT_SCOPE =
            "flow-client,component-frontend,theme-static,generated-lazy,flow-lazy-dependency,production-owner,dev-mode";
    private static final String FRONTEND_RESOLVER_IDENTITY_POLICY =
            "stable-source-path-plus-owner-namespace-v1_21";
    private static final String FRONTEND_RESOLVER_INTEGRITY_POLICY =
            "sha256-required-for-bodied-resources-v1_21";
    private static final String FRONTEND_RESOLVER_CACHE_POLICY =
            "window-lifetime-content-addressed-v1_21";
    private static final String FRONTEND_RESOLVER_CLEANUP_POLICY =
            "host-owned-window-lifetime-v1_21";
    private static final String FRONTEND_RESOLVER_ALLOWED_SCHEMES =
            "blob,vatori-resource";
    private static final String FRONTEND_RESOLVER_FAMILIES_COVERED =
            "flow-client-classpath,vaadin-component-classpath-frontend,npm-frontend,theme-static,generated-lazy,flow-lazy-dependency,production-bundle,dev-mode-bundle";
    private static final String FRONTEND_RESOLVER_ACCEPTABLE_GAPS =
            "generated-lazy-chunk-file-not-generated,production-owner-owned-missing-real-artifact,dev-mode-vite-execution-unmapped";
    private static final String FRONTEND_PIPELINE_CLOSURE_VERSION =
            "flow-ipc-frontend-pipeline-closure-v1_24";
    private static final String FRONTEND_PIPELINE_CLOSURE_SCOPE =
            FRONTEND_RESOLVER_CONTRACT_SCOPE;
    private static final String FRONTEND_PIPELINE_CLOSURE_TRANSPORT =
            "no-localhost-ipc-resource-request";
    private static final String FRONTEND_PIPELINE_CLOSURE_INFRASTRUCTURE_STATUS =
            "closed-for-representative-fixture-paths";
    private static final String FRONTEND_PIPELINE_CLOSURE_REAL_BUILD_STATUS =
            "product-parity-pending";
    private static final String FRONTEND_PIPELINE_CLOSURE_SATISFIED_PROOFS =
            "flow-client-executed,component-frontend-executed,theme-static-verified,flow-lazy-dependency-executed,generated-chunk-fixture-executed,production-owner-fixture-executed";
    private static final String FRONTEND_PIPELINE_CLOSURE_REMAINING_BLOCKERS =
            "real-generated-lazy-chunk-file-not-generated,real-production-owner-owned-runtime-missing-real-artifact,dev-mode-vite-execution-unmapped";
    private static final String FRONTEND_PIPELINE_CLOSURE_DECISION =
            "basic-pipeline-infrastructure-closed-real-build-parity-pending";
    private static final String FRONTEND_PIPELINE_CLOSURE_NEXT_PRODUCT_PATH =
            "packaging-time-staged-frontend-or-supported-vaadin-build-spi";
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
    private static final String PRODUCTION_RUNTIME_OWNER_DEPENDENCY_RESOLVER_EXPECTED_ARTIFACTS =
            String.join(",", PRODUCTION_RUNTIME_OWNER_DEPENDENCY_ARTIFACTS);
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
        add(discoveredComponentResources, GENERATED_FLOW_IMPORTS_V1_22_JS,
                "vaadin-generated-flow-imports-v1_22-module", true,
                generatedLazyChunkFileFixtureAttributes(true));
        add(discoveredComponentResources, GENERATED_FLOW_IMPORTS_CHUNK_V1_22_JS,
                "vaadin-generated-flow-imports-v1_22-chunk", true,
                generatedLazyChunkFileFixtureAttributes(false));
        add(discoveredComponentResources, PRODUCTION_OWNER_FIXTURE_ROOT_V1_23_JS,
                "vaadin-production-owner-positive-fixture-root-v1_23", true,
                productionOwnerPositiveFixtureAttributes(true));
        add(discoveredComponentResources, PRODUCTION_OWNER_FIXTURE_DEPENDENCY_V1_23_JS,
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
        manifest.put("generatedLazyChunkFileExecutionVersion", GENERATED_LAZY_CHUNK_FILE_EXECUTION_VERSION);
        manifest.put("generatedLazyChunkFileFixtureSource", GENERATED_LAZY_CHUNK_FILE_FIXTURE_SOURCE);
        manifest.put("generatedLazyChunkFileFixtureImportsArtifact", GENERATED_FLOW_IMPORTS_V1_22_JS);
        manifest.put("generatedLazyChunkFileFixtureChunkArtifact", GENERATED_FLOW_IMPORTS_CHUNK_V1_22_JS);
        manifest.put("generatedLazyChunkFileFixtureChunkId", GENERATED_LAZY_CHUNK_FILE_FIXTURE_CHUNK_ID);
        manifest.put("generatedLazyChunkFileFixtureTriggerOwner", GENERATED_LAZY_CHUNK_FILE_FIXTURE_TRIGGER_OWNER);
        manifest.put("generatedLazyChunkFileFixtureExpectedSideEffect",
                GENERATED_LAZY_CHUNK_FILE_FIXTURE_SIDE_EFFECT);
        manifest.put("productionBundleRuntimeOwnerPositiveFixtureVersion",
                PRODUCTION_OWNER_POSITIVE_FIXTURE_VERSION);
        manifest.put("productionBundleRuntimeOwnerPositiveFixtureSource",
                PRODUCTION_OWNER_POSITIVE_FIXTURE_SOURCE);
        manifest.put("productionBundleRuntimeOwnerPositiveFixtureRootArtifact",
                PRODUCTION_OWNER_FIXTURE_ROOT_V1_23_JS);
        manifest.put("productionBundleRuntimeOwnerPositiveFixtureDependencyArtifact",
                PRODUCTION_OWNER_FIXTURE_DEPENDENCY_V1_23_JS);
        manifest.put("productionBundleRuntimeOwnerPositiveFixtureExpectedTags",
                PRODUCTION_OWNER_POSITIVE_FIXTURE_EXPECTED_TAGS);
        manifest.put("productionBundleRuntimeOwnerPositiveFixtureExpectedGlobals",
                PRODUCTION_OWNER_POSITIVE_FIXTURE_EXPECTED_GLOBALS);
        manifest.put("productionBundleRuntimeOwnerPositiveFixtureExecutionPolicy",
                PRODUCTION_OWNER_POSITIVE_FIXTURE_EXECUTION_POLICY);
        manifest.put("productionBundleRuntimeOwnerPositiveFixtureNoBlobFallback", true);
        manifest.put("runtimeResolverContractVersion", RUNTIME_RESOLVER_CONTRACT_VERSION);
        manifest.putAll(frontendResourceLoaderAttributes());
        manifest.putAll(frontendResolverContractAttributes());
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
                        "production bundle owner is now explicit and independently staged, but import remains blocked until the owner can safely execute indexhtml side effects without colliding with focused component entry imports"),
                strictBlocker("production-bundle", PRODUCTION_RUNTIME_OWNER_IMPORT_VERSION,
                        "production bundle owner import is now a controlled v1.14 probe; success requires observable owner side effects, and acceptable failure must come from a real import attempt against the stats-selected indexhtml artifact"),
                strictBlocker("production-bundle", PRODUCTION_RUNTIME_OWNER_DEPENDENCY_RESOLVER_VERSION,
                        "production bundle owner dependency resolver must classify and stage the real dev-bundle build graph before the owner import result is accepted")));
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
        if (moduleSpecifier.startsWith(DEV_BUNDLE_BUILD_ROOT)) {
            putProductionOwnerDependencyResolverAttributes(attributes, moduleSpecifier);
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
        attributes.put("productionBundleRuntimeOwnerImportVersion",
                PRODUCTION_RUNTIME_OWNER_IMPORT_VERSION);
        attributes.put("productionBundleRuntimeOwnerImportPolicy",
                PRODUCTION_RUNTIME_OWNER_IMPORT_POLICY);
        attributes.put("productionBundleRuntimeOwnerImportArtifact", DEV_BUNDLE_INDEX_HTML_JS);
        attributes.put("productionBundleRuntimeOwnerImportPhase",
                PRODUCTION_RUNTIME_OWNER_IMPORT_PHASE);
        attributes.put("productionBundleRuntimeOwnerImportPhaseCandidates",
                PRODUCTION_RUNTIME_OWNER_IMPORT_PHASE_CANDIDATES);
        attributes.put("productionBundleRuntimeOwnerImportDiagnosticModes",
                PRODUCTION_RUNTIME_OWNER_IMPORT_DIAGNOSTIC_MODES);
        attributes.put("productionBundleRuntimeOwnerImportExpectedSideEffects",
                PRODUCTION_RUNTIME_OWNER_IMPORT_EXPECTED_SIDE_EFFECTS);
        attributes.put("productionBundleRuntimeOwnerImportAcceptableFailures",
                PRODUCTION_RUNTIME_OWNER_IMPORT_ACCEPTABLE_FAILURES);
        attributes.put("productionBundleRuntimeOwnerImportRewriteStrategy",
                PRODUCTION_RUNTIME_OWNER_IMPORT_REWRITE_STRATEGY);
        attributes.put("productionBundleRuntimeOwnerImportSourceArtifactFamily", "production-bundle");
        attributes.put("productionBundleRuntimeOwnerImportDependencyPaths",
                DEV_BUNDLE_INDEX_HTML_JS + "," + DEV_BUNDLE_COMMONJS_JS);
        attributes.put("productionBundleRuntimeOwnerDependencyResolverVersion",
                PRODUCTION_RUNTIME_OWNER_DEPENDENCY_RESOLVER_VERSION);
        attributes.put("productionBundleRuntimeOwnerDependencyResolverRoot",
                PRODUCTION_RUNTIME_OWNER_DEPENDENCY_RESOLVER_ROOT);
        attributes.put("productionBundleRuntimeOwnerDependencyResolverPolicy",
                PRODUCTION_RUNTIME_OWNER_DEPENDENCY_RESOLVER_POLICY);
        attributes.put("productionBundleRuntimeOwnerDependencyResolverExpectedArtifacts",
                PRODUCTION_RUNTIME_OWNER_DEPENDENCY_RESOLVER_EXPECTED_ARTIFACTS);
        attributes.put("productionBundleRuntimeOwnerDependencyResolverAcceptableFailures",
                PRODUCTION_RUNTIME_OWNER_DEPENDENCY_RESOLVER_ACCEPTABLE_FAILURES);
        attributes.put("productionBundleRuntimeOwnerDependencyResolverModuleIdentityNamespace",
                PRODUCTION_RUNTIME_OWNER_DEPENDENCY_RESOLVER_MODULE_IDENTITY_NAMESPACE);
        attributes.put("productionBundleRuntimeOwnerDependencyResolverNextResolutionStep",
                PRODUCTION_RUNTIME_OWNER_DEPENDENCY_RESOLVER_NEXT_STEP);
        attributes.put("productionBundleRuntimeOwnerImportOrderingVersion",
                PRODUCTION_RUNTIME_OWNER_IMPORT_ORDERING_VERSION);
        attributes.put("productionBundleRuntimeOwnerImportPhasePolicy",
                PRODUCTION_RUNTIME_OWNER_IMPORT_PHASE_POLICY);
        attributes.put("productionBundleRuntimeOwnerImportOrderingAcceptableFailures",
                PRODUCTION_RUNTIME_OWNER_IMPORT_ORDERING_ACCEPTABLE_FAILURES);
        attributes.put("productionBundleRuntimeOwnerImportOrderingResult",
                "not-run-until-host-selects-owner-first-or-after-component-phase");
        attributes.put("productionBundleRuntimeOwnerImportOrderingObservedPhases", "");
        attributes.put("productionBundleRuntimeOwnerDependencyGraphClosed", false);
        attributes.put("productionBundleRuntimeOwnerImportStageCount", 0);
        attributes.put("productionBundleRuntimeOwnerImportMissingDependencyPaths", "");
        attributes.put("productionBundleRuntimeOwnerImportAttempted", false);
        attributes.put("productionBundleRuntimeOwnerImported", false);
        attributes.put("productionBundleRuntimeOwnerImportNextResolutionStep",
                PRODUCTION_RUNTIME_OWNER_IMPORT_NEXT_STEP);
        attributes.put("componentProductionOwnerReuseVersion",
                COMPONENT_PRODUCTION_OWNER_REUSE_VERSION);
        attributes.put("componentProductionOwnerReusePolicy",
                COMPONENT_PRODUCTION_OWNER_REUSE_POLICY);
        attributes.put("componentProductionOwnerReuseAcceptableFailures",
                COMPONENT_PRODUCTION_OWNER_REUSE_ACCEPTABLE_FAILURES);
        attributes.put("componentProductionOwnerReuseMode",
                "decision-required-after-owner-import-v1_16");
        attributes.put("componentProductionOwnerReused", false);
        attributes.put("componentProductionOwnerReuseReason",
                "owner import ordering and component reuse decision are evaluated by the v1.16 host runtime");
        attributes.put("componentProductionOwnerOwnedTags", "");
        attributes.put("componentProductionOwnerSkippedDefinitions", "");
        attributes.put("componentProductionOwnerFallbackImports",
                DEV_BUNDLE_TEXT_FIELD_ENTRY_JS + "," + DEV_BUNDLE_CHECKBOX_ENTRY_JS + "," + DEV_BUNDLE_SELECT_ENTRY_JS);
        attributes.put("componentProductionOwnerDependencyPaths", DEV_BUNDLE_INDEX_HTML_JS);
        attributes.put("componentProductionOwnerImportOrder",
                "owner-first-or-component-focused-fallback-v1_16");
        putProductionOwnerResourceLoaderAttributes(attributes, DEV_BUNDLE_INDEX_HTML_JS, true);
        putProductionOwnerSideEffectAttributes(attributes);
        return attributes;
    }

    private static Map<String, Object> productionOwnerDependencyAttributes(String path) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("componentFrontendResource", true);
        attributes.put("moduleSpecifier", path);
        attributes.put("resolver", "vaadin-production-owner-dependency-resolver-v1_15");
        attributes.put("shim", false);
        attributes.put("artifactFamily", "production-bundle");
        attributes.put("origin", "vaadin-dev-bundle");
        attributes.put("resourceOrigin", "vaadin-dev-bundle-production-owner-dependency");
        attributes.put("dependencyOf", DEV_BUNDLE_INDEX_HTML_JS);
        attributes.put("executionPolicy", "owner-dependency-module-v1_15");
        attributes.put("artifactStatus", "available");
        attributes.put("requestedBy", "production-owner-dependency-resolver-v1_15");
        attributes.put("failureClassification", "production-owner-dependency-resolved-existing-artifact");
        attributes.put("blockedReason",
                "real Vaadin dev-bundle production artifact exposed for owner-scoped dependency graph closure; missing paths are classified at runtime instead of being faked");
        attributes.put("lookupKind", "Production-owner-dependency-resource-v1_15");
        attributes.put("devBundleResource", true);
        attributes.put("productionBundleResource", true);
        attributes.put("productionOwnerDependencyResource", true);
        putProductionOwnerDependencyResolverAttributes(attributes, path);
        putProductionOwnerResourceLoaderAttributes(attributes, path, false);
        putProductionOwnerSideEffectAttributes(attributes);
        return attributes;
    }

    private static void putProductionOwnerDependencyResolverAttributes(Map<String, Object> attributes, String path) {
        attributes.put("productionBundleRuntimeOwnerDependencyResolverVersion",
                PRODUCTION_RUNTIME_OWNER_DEPENDENCY_RESOLVER_VERSION);
        attributes.put("productionBundleRuntimeOwnerDependencyResolverRoot",
                PRODUCTION_RUNTIME_OWNER_DEPENDENCY_RESOLVER_ROOT);
        attributes.put("productionBundleRuntimeOwnerDependencyResolverPolicy",
                PRODUCTION_RUNTIME_OWNER_DEPENDENCY_RESOLVER_POLICY);
        attributes.put("productionBundleRuntimeOwnerDependencyResolverExpectedArtifacts",
                PRODUCTION_RUNTIME_OWNER_DEPENDENCY_RESOLVER_EXPECTED_ARTIFACTS);
        attributes.put("productionBundleRuntimeOwnerDependencyResolverAcceptableFailures",
                PRODUCTION_RUNTIME_OWNER_DEPENDENCY_RESOLVER_ACCEPTABLE_FAILURES);
        attributes.put("productionBundleRuntimeOwnerDependencyResolverModuleIdentityNamespace",
                PRODUCTION_RUNTIME_OWNER_DEPENDENCY_RESOLVER_MODULE_IDENTITY_NAMESPACE);
        attributes.put("productionBundleRuntimeOwnerDependencyResolverRootArtifact",
                DEV_BUNDLE_INDEX_HTML_JS);
        attributes.put("productionBundleRuntimeOwnerDependencyResolverArtifact", path);
        attributes.put("productionBundleRuntimeOwnerDependencyResolverClassification",
                "resolved-existing-artifact");
    }

    private static void putProductionOwnerResourceLoaderAttributes(Map<String, Object> attributes, String path,
            boolean rootArtifact) {
        attributes.put("productionBundleRuntimeOwnerResourceLoaderVersion",
                PRODUCTION_RUNTIME_OWNER_RESOURCE_LOADER_VERSION);
        attributes.put("productionBundleRuntimeOwnerResourceLoaderMode",
                PRODUCTION_RUNTIME_OWNER_RESOURCE_LOADER_MODE);
        attributes.put("productionBundleRuntimeOwnerResourceScheme",
                PRODUCTION_RUNTIME_OWNER_RESOURCE_SCHEME);
        attributes.put("productionBundleRuntimeOwnerResourceGraphMode",
                PRODUCTION_RUNTIME_OWNER_RESOURCE_GRAPH_MODE);
        attributes.put("productionBundleRuntimeOwnerResourceBasePolicy",
                PRODUCTION_RUNTIME_OWNER_RESOURCE_BASE_POLICY);
        attributes.put("productionBundleRuntimeOwnerResourceLoaderNamespace",
                PRODUCTION_RUNTIME_OWNER_RESOURCE_LOADER_NAMESPACE);
        attributes.put("productionBundleRuntimeOwnerResourceLoaderRootArtifact",
                DEV_BUNDLE_INDEX_HTML_JS);
        attributes.put("productionBundleRuntimeOwnerResourceLoaderArtifact", path);
        attributes.put("productionBundleRuntimeOwnerResourceLoaderArtifactRole",
                rootArtifact ? "root" : "dependency");
        attributes.put("productionBundleRuntimeOwnerBlobFallbackAllowed", false);
        attributes.put("productionBundleRuntimeOwnerDependencyBlobFallbackPolicy",
                PRODUCTION_RUNTIME_OWNER_DEPENDENCY_BLOB_FALLBACK_POLICY);
        attributes.put("productionBundleRuntimeOwnerResourceLoaderAcceptableFailures",
                PRODUCTION_RUNTIME_OWNER_RESOURCE_LOADER_ACCEPTABLE_FAILURES);
        attributes.put("productionBundleRuntimeOwnerResourceLoaderExpectedArtifacts",
                DEV_BUNDLE_INDEX_HTML_JS + "," + PRODUCTION_RUNTIME_OWNER_DEPENDENCY_RESOLVER_EXPECTED_ARTIFACTS);
        attributes.put("productionBundleRuntimeOwnerResourceLoaderNoLocalhost", true);
        attributes.put("productionBundleRuntimeOwnerResourceLoaderRelativeImportBase",
                rootArtifact ? DEV_BUNDLE_INDEX_HTML_JS : path);
        attributes.put("productionBundleRuntimeOwnerResourceLoaderStageUrlPolicy",
                "host-owned-custom-scheme-stable-source-path-v1_18");
        attributes.put("productionBundleRuntimeOwnerResourceLoaderCleanupPolicy",
                "cleanup-owned-window-lifetime-file-backed-production-owner-v1_18");
    }

    private static void putProductionOwnerSideEffectAttributes(Map<String, Object> attributes) {
        attributes.put("productionBundleRuntimeOwnerSideEffectVersion",
                PRODUCTION_RUNTIME_OWNER_SIDE_EFFECT_VERSION);
        attributes.put("productionBundleRuntimeOwnerSideEffectPolicy",
                PRODUCTION_RUNTIME_OWNER_SIDE_EFFECT_POLICY);
        attributes.put("productionBundleRuntimeOwnerSideEffectExpectedTags",
                PRODUCTION_RUNTIME_OWNER_SIDE_EFFECT_EXPECTED_TAGS);
        attributes.put("productionBundleRuntimeOwnerSideEffectExpectedGlobals",
                PRODUCTION_RUNTIME_OWNER_SIDE_EFFECT_EXPECTED_GLOBALS);
        attributes.put("productionBundleRuntimeOwnerSideEffectAcceptableFailures",
                PRODUCTION_RUNTIME_OWNER_SIDE_EFFECT_ACCEPTABLE_FAILURES);
        attributes.put("productionBundleRuntimeOwnerSideEffectNoBlobFallback", true);
        attributes.put("productionBundleRuntimeOwnerModuleImported", false);
        attributes.put("productionBundleRuntimeOwnerSideEffectSatisfied", false);
        attributes.put("productionBundleRuntimeOwnerSideEffectFailureLayer",
                "pending-custom-scheme-owner-import-side-effect-check-v1_19");
        attributes.put("componentProductionOwnerReuseDecisionVersion",
                COMPONENT_PRODUCTION_OWNER_REUSE_DECISION_VERSION);
        attributes.put("componentProductionOwnerReuseDecisionSource",
                COMPONENT_PRODUCTION_OWNER_REUSE_DECISION_SOURCE);
        attributes.put("componentProductionOwnerReuseDecision",
                "pending-runtime-side-effect-check");
        attributes.put("componentProductionOwnerReuseDecisionReason",
                "production owner custom-scheme import side effects must be checked before focused component resolver can reuse owner-owned tags");
        attributes.put("componentProductionOwnerReuseDecisionTags",
                PRODUCTION_RUNTIME_OWNER_SIDE_EFFECT_EXPECTED_TAGS);
        putProductionOwnerOwnedRuntimeAttributes(attributes);
    }

    private static void putProductionOwnerOwnedRuntimeAttributes(Map<String, Object> attributes) {
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimeVersion",
                PRODUCTION_RUNTIME_OWNER_OWNED_RUNTIME_VERSION);
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimePolicy",
                PRODUCTION_RUNTIME_OWNER_OWNED_RUNTIME_POLICY);
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimePhase",
                PRODUCTION_RUNTIME_OWNER_OWNED_RUNTIME_PHASE);
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimeExpectedTags",
                PRODUCTION_RUNTIME_OWNER_SIDE_EFFECT_EXPECTED_TAGS);
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimeExpectedGlobals",
                PRODUCTION_RUNTIME_OWNER_SIDE_EFFECT_EXPECTED_GLOBALS);
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimeNoBlobFallback", true);
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimeAcceptableFailures",
                PRODUCTION_RUNTIME_OWNER_OWNED_RUNTIME_ACCEPTABLE_FAILURES);
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimeAttempted", false);
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimeSatisfied", false);
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimeFailureLayer",
                "pending-owner-first-custom-scheme-runtime-reuse-v1_20");
        attributes.put("componentProductionOwnerReusePolicyVersionV1_20",
                COMPONENT_PRODUCTION_OWNER_REUSE_POLICY_VERSION_V1_20);
        attributes.put("componentProductionOwnerReusePolicyV1_20",
                COMPONENT_PRODUCTION_OWNER_REUSE_POLICY_V1_20);
        attributes.put("componentProductionOwnerReuseDecisionPhase",
                PRODUCTION_RUNTIME_OWNER_OWNED_RUNTIME_PHASE);
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
        attributes.put("stageUrlPolicy", "host-owned-resource-scheme-file-backed-v1_17");
        attributes.put("cachePolicy", "single-trigger-per-resource-path");
        attributes.put("cleanupOwnership", "cleanup-owned-window-lifetime-file-backed-v1_17");
        attributes.put("blockerClassification", "none");
        attributes.put("frontendResourceLoaderExecutionSlice", "flow-lazy-dependency-v1_17");
        attributes.put("frontendResourceLoaderExpectedStagingMode", "custom-scheme");
        attributes.put("frontendResourceLoaderRelativeImportBase", FLOW_LAZY_DEPENDENCY_JS);
        attributes.put("frontendResourceLoaderMimeType", "text/javascript; charset=utf-8");
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

    private static Map<String, Object> generatedLazyChunkFileFixtureAttributes(boolean importsArtifact) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("componentFrontendResource", true);
        attributes.put("generatedLazyResource", true);
        attributes.put("generatedLazyChunkFileFixture", true);
        attributes.put("generatedLazyChunkFileFixtureRole", importsArtifact ? "imports" : "chunk");
        attributes.put("generatedLazyChunkFileExecutionVersion", GENERATED_LAZY_CHUNK_FILE_EXECUTION_VERSION);
        attributes.put("generatedLazyChunkFileFixtureSource", GENERATED_LAZY_CHUNK_FILE_FIXTURE_SOURCE);
        attributes.put("generatedLazyChunkFileFixtureImportsArtifact", GENERATED_FLOW_IMPORTS_V1_22_JS);
        attributes.put("generatedLazyChunkFileFixtureChunkArtifact", GENERATED_FLOW_IMPORTS_CHUNK_V1_22_JS);
        attributes.put("generatedLazyChunkFileFixtureChunkId", GENERATED_LAZY_CHUNK_FILE_FIXTURE_CHUNK_ID);
        attributes.put("generatedLazyChunkFileFixtureTriggerOwner",
                GENERATED_LAZY_CHUNK_FILE_FIXTURE_TRIGGER_OWNER);
        attributes.put("generatedLazyChunkFileFixtureExpectedSideEffect",
                GENERATED_LAZY_CHUNK_FILE_FIXTURE_SIDE_EFFECT);
        attributes.put("loadOnDemandChunkFileBlockerVersion", "");
        attributes.put("blockerClassification", "none");
        attributes.put("artifactFamily", "generated-lazy");
        attributes.put("origin", "repository-generated-flow-imports-fixture");
        attributes.put("resourceOrigin", importsArtifact
                ? "repository-generated-flow-imports-v1_22"
                : "repository-generated-flow-imports-v1_22-chunk");
        attributes.put("dependencyOf", importsArtifact ? "Flow.loadOnDemand" : GENERATED_FLOW_IMPORTS_V1_22_JS);
        attributes.put("executionPolicy", importsArtifact
                ? "stage-and-import-generated-lazy-chunk-file-fixture-v1_22"
                : "generated-lazy-chunk-file-body-v1_22");
        attributes.put("artifactStatus", "available");
        attributes.put("requestedBy", "component-frontend-manifest-v1_22");
        attributes.put("failureClassification", importsArtifact
                ? "generated-lazy-chunk-file-positive-fixture-v1_22"
                : "generated-lazy-chunk-file-body-positive-fixture-v1_22");
        attributes.put("blockedReason", importsArtifact
                ? "Vaadin-shaped repository fixture contains a real generated loadOnDemand chunk table and is used only to prove the no-localhost chunk-file executor positive path"
                : "Repository chunk body for the v1.22 generated loadOnDemand positive fixture");
        attributes.put("lookupKind", importsArtifact
                ? "Generated-flow-imports-positive-fixture-v1_22"
                : "Generated-flow-imports-positive-fixture-chunk-v1_22");
        attributes.put("moduleSpecifier", importsArtifact ? GENERATED_FLOW_IMPORTS_V1_22_JS
                : GENERATED_FLOW_IMPORTS_CHUNK_V1_22_JS);
        attributes.put("resolver", "repository-generated-flow-imports-chunk-file-fixture-v1_22");
        attributes.put("shim", false);
        if (importsArtifact) {
            attributes.putAll(generatedLoadOnDemandChunkFileRegistryAttributes(GENERATED_FLOW_IMPORTS_V1_22_JS));
            attributes.put("loadOnDemandChunkContractVersion", GENERATED_LAZY_CHUNK_CONTRACT_VERSION);
            attributes.put("loadOnDemandChunkApiOwner", GENERATED_LAZY_LOAD_ON_DEMAND_API_OWNER);
            attributes.put("loadOnDemandChunkApiInstallSource",
                    "repository-generated-flow-imports-v1_22-after-staged-import");
            attributes.put("loadOnDemandChunkRegistrySource", "repository-fixture-chunk-table-v1_22");
            attributes.put("loadOnDemandChunkMappingSource", GENERATED_LAZY_CHUNK_FILE_MAPPING_SOURCE);
            attributes.put("loadOnDemandChunkResolvedArtifactPath", GENERATED_FLOW_IMPORTS_V1_22_JS);
            attributes.put("loadOnDemandChunkRequestedChunk", GENERATED_LAZY_CHUNK_FILE_FIXTURE_CHUNK_ID);
            attributes.put("loadOnDemandChunkFailureLayer", "");
            attributes.put("loadOnDemandChunkNextResolutionStep", "");
        } else {
            byte[] chunkBody = classpathResourceBytes(GENERATED_FLOW_IMPORTS_CHUNK_V1_22_JS);
            attributes.put("chunkId", GENERATED_LAZY_CHUNK_FILE_FIXTURE_CHUNK_ID);
            attributes.put("chunkArtifactPath", GENERATED_FLOW_IMPORTS_CHUNK_V1_22_JS);
            attributes.put("chunkFileStatus", "available");
            attributes.put("chunkFileBytes", chunkBody.length);
            attributes.put("chunkFileSha256", Resource.sha256(chunkBody));
            attributes.put("chunkFileResolver", GENERATED_LAZY_CHUNK_FILE_RESOLVER);
            attributes.put("loadOnDemandChunkFileStatus", "available");
            attributes.put("loadOnDemandChunkFileBytes", chunkBody.length);
            attributes.put("loadOnDemandChunkFileSha256", Resource.sha256(chunkBody));
            attributes.put("loadOnDemandChunkFileSideEffectProof", "chunk-file-body-available-for-staged-import");
        }
        return attributes;
    }

    private static Map<String, Object> productionOwnerPositiveFixtureAttributes(boolean rootArtifact) {
        String path = rootArtifact
                ? PRODUCTION_OWNER_FIXTURE_ROOT_V1_23_JS
                : PRODUCTION_OWNER_FIXTURE_DEPENDENCY_V1_23_JS;
        String dependencyPath = rootArtifact
                ? "production-owner-positive-fixture"
                : PRODUCTION_OWNER_FIXTURE_ROOT_V1_23_JS;
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("componentFrontendResource", true);
        attributes.put("productionBundleResource", true);
        attributes.put("productionOwnerPositiveFixture", true);
        attributes.put("productionBundleRuntimeOwnerPositiveFixture", true);
        attributes.put("productionBundleRuntimeOwnerPositiveFixtureRole", rootArtifact ? "root" : "dependency");
        attributes.put("productionBundleRuntimeOwnerPositiveFixtureVersion",
                PRODUCTION_OWNER_POSITIVE_FIXTURE_VERSION);
        attributes.put("productionBundleRuntimeOwnerPositiveFixtureSource",
                PRODUCTION_OWNER_POSITIVE_FIXTURE_SOURCE);
        attributes.put("productionBundleRuntimeOwnerPositiveFixtureRootArtifact",
                PRODUCTION_OWNER_FIXTURE_ROOT_V1_23_JS);
        attributes.put("productionBundleRuntimeOwnerPositiveFixtureDependencyArtifact",
                PRODUCTION_OWNER_FIXTURE_DEPENDENCY_V1_23_JS);
        attributes.put("productionBundleRuntimeOwnerPositiveFixtureExpectedTags",
                PRODUCTION_OWNER_POSITIVE_FIXTURE_EXPECTED_TAGS);
        attributes.put("productionBundleRuntimeOwnerPositiveFixtureExpectedGlobals",
                PRODUCTION_OWNER_POSITIVE_FIXTURE_EXPECTED_GLOBALS);
        attributes.put("productionBundleRuntimeOwnerPositiveFixtureExecutionPolicy",
                PRODUCTION_OWNER_POSITIVE_FIXTURE_EXECUTION_POLICY);
        attributes.put("productionBundleRuntimeOwnerPositiveFixtureNoBlobFallback", true);
        attributes.put("moduleSpecifier", path);
        attributes.put("resolver", "repository-production-owner-positive-fixture-v1_23");
        attributes.put("shim", false);
        attributes.put("artifactFamily", "production-bundle");
        attributes.put("origin", "repository-production-owner-fixture");
        attributes.put("resourceOrigin", rootArtifact
                ? "repository-production-owner-positive-fixture-root"
                : "repository-production-owner-positive-fixture-dependency");
        attributes.put("dependencyOf", dependencyPath);
        attributes.put("executionPolicy", rootArtifact
                ? PRODUCTION_OWNER_POSITIVE_FIXTURE_EXECUTION_POLICY
                : "owner-positive-fixture-dependency-v1_23");
        attributes.put("artifactStatus", "available");
        attributes.put("requestedBy", "component-frontend-manifest-v1_23");
        attributes.put("failureClassification", rootArtifact
                ? "production-owner-positive-fixture-root-v1_23"
                : "production-owner-positive-fixture-dependency-v1_23");
        attributes.put("blockedReason",
                "Repository production owner fixture proves the custom-scheme owner runtime positive path without claiming real Vaadin production bundle parity");
        attributes.put("lookupKind", rootArtifact
                ? "Production-owner-positive-fixture-root-v1_23"
                : "Production-owner-positive-fixture-dependency-v1_23");
        attributes.put("productionBundleRuntimeOwnerVersion", PRODUCTION_RUNTIME_OWNER_VERSION);
        attributes.put("productionBundleRuntimeOwner", PRODUCTION_RUNTIME_OWNER);
        attributes.put("productionBundleRuntimeEntryArtifact", PRODUCTION_OWNER_FIXTURE_ROOT_V1_23_JS);
        attributes.put("productionBundleRuntimeOwnerImportArtifact", PRODUCTION_OWNER_FIXTURE_ROOT_V1_23_JS);
        attributes.put("productionBundleRuntimeOwnerImportVersion", PRODUCTION_RUNTIME_OWNER_IMPORT_VERSION);
        attributes.put("productionBundleRuntimeOwnerImportPolicy",
                "owner-first-positive-fixture-import-v1_23");
        attributes.put("productionBundleRuntimeOwnerImportPhase", "before-component-imports");
        attributes.put("productionBundleRuntimeOwnerImportPhaseCandidates",
                PRODUCTION_RUNTIME_OWNER_IMPORT_PHASE_CANDIDATES);
        attributes.put("productionBundleRuntimeOwnerImportExpectedSideEffects",
                PRODUCTION_OWNER_POSITIVE_FIXTURE_EXPECTED_GLOBALS
                        + ",customElements:" + PRODUCTION_OWNER_POSITIVE_FIXTURE_EXPECTED_TAGS);
        attributes.put("productionBundleRuntimeOwnerImportAcceptableFailures",
                "production-owner-positive-fixture-browser-import-failed,production-owner-positive-fixture-side-effect-missing");
        attributes.put("productionBundleRuntimeOwnerImportRewriteStrategy",
                "custom-scheme-owner-positive-fixture-relative-import-rewrite-v1_23");
        attributes.put("productionBundleRuntimeOwnerResourceLoaderVersion",
                PRODUCTION_RUNTIME_OWNER_RESOURCE_LOADER_VERSION);
        attributes.put("productionBundleRuntimeOwnerResourceLoaderMode",
                PRODUCTION_RUNTIME_OWNER_RESOURCE_LOADER_MODE);
        attributes.put("productionBundleRuntimeOwnerResourceScheme",
                PRODUCTION_RUNTIME_OWNER_RESOURCE_SCHEME);
        attributes.put("productionBundleRuntimeOwnerResourceGraphMode",
                "positive-fixture-root-and-dependency-v1_23");
        attributes.put("productionBundleRuntimeOwnerResourceBasePolicy",
                PRODUCTION_RUNTIME_OWNER_RESOURCE_BASE_POLICY);
        attributes.put("productionBundleRuntimeOwnerResourceLoaderNamespace",
                PRODUCTION_RUNTIME_OWNER_RESOURCE_LOADER_NAMESPACE);
        attributes.put("productionBundleRuntimeOwnerResourceLoaderRootArtifact",
                PRODUCTION_OWNER_FIXTURE_ROOT_V1_23_JS);
        attributes.put("productionBundleRuntimeOwnerResourceLoaderArtifact", path);
        attributes.put("productionBundleRuntimeOwnerResourceLoaderArtifactRole",
                rootArtifact ? "positive-fixture-root" : "positive-fixture-dependency");
        attributes.put("productionBundleRuntimeOwnerBlobFallbackAllowed", false);
        attributes.put("productionBundleRuntimeOwnerResourceLoaderNoLocalhost", true);
        attributes.put("productionBundleRuntimeOwnerResourceLoaderRelativeImportBase", path);
        attributes.put("productionBundleRuntimeOwnerResourceLoaderStageUrlPolicy",
                "host-owned-custom-scheme-stable-source-path-v1_23");
        attributes.put("productionBundleRuntimeOwnerResourceLoaderCleanupPolicy",
                "cleanup-owned-window-lifetime-file-backed-production-owner-v1_23");
        attributes.put("productionBundleRuntimeOwnerResourceLoaderExpectedArtifacts",
                PRODUCTION_OWNER_FIXTURE_ROOT_V1_23_JS + "," + PRODUCTION_OWNER_FIXTURE_DEPENDENCY_V1_23_JS);
        attributes.put("productionBundleRuntimeOwnerSideEffectVersion",
                PRODUCTION_RUNTIME_OWNER_SIDE_EFFECT_VERSION);
        attributes.put("productionBundleRuntimeOwnerSideEffectPolicy",
                "positive-fixture-owner-side-effect-contract-v1_23");
        attributes.put("productionBundleRuntimeOwnerSideEffectExpectedTags",
                PRODUCTION_OWNER_POSITIVE_FIXTURE_EXPECTED_TAGS);
        attributes.put("productionBundleRuntimeOwnerSideEffectExpectedGlobals",
                PRODUCTION_OWNER_POSITIVE_FIXTURE_EXPECTED_GLOBALS);
        attributes.put("productionBundleRuntimeOwnerSideEffectAcceptableFailures",
                "production-owner-positive-fixture-side-effect-missing,production-owner-positive-fixture-browser-import-failed");
        attributes.put("productionBundleRuntimeOwnerSideEffectNoBlobFallback", true);
        attributes.put("productionBundleRuntimeOwnerModuleImported", false);
        attributes.put("productionBundleRuntimeOwnerSideEffectSatisfied", false);
        attributes.put("productionBundleRuntimeOwnerSideEffectFailureLayer",
                "pending-production-owner-positive-fixture-import-v1_23");
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimeVersion",
                PRODUCTION_RUNTIME_OWNER_OWNED_RUNTIME_VERSION);
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimePolicy",
                "owner-first-positive-fixture-runtime-reuse-v1_23");
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimePhase", "before-component-imports");
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimeExpectedTags",
                PRODUCTION_OWNER_POSITIVE_FIXTURE_EXPECTED_TAGS);
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimeExpectedGlobals",
                PRODUCTION_OWNER_POSITIVE_FIXTURE_EXPECTED_GLOBALS);
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimeNoBlobFallback", true);
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimeAttempted", false);
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimeSatisfied", false);
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimeFailureLayer",
                "pending-production-owner-positive-fixture-runtime-v1_23");
        attributes.put("componentProductionOwnerReuseDecisionVersion",
                COMPONENT_PRODUCTION_OWNER_REUSE_DECISION_VERSION);
        attributes.put("componentProductionOwnerReuseDecisionSource",
                "production-owner-positive-fixture-v1_23");
        attributes.put("componentProductionOwnerReuseDecision",
                "pending-production-owner-positive-fixture");
        attributes.put("componentProductionOwnerReuseDecisionReason",
                "positive fixture must import through owner custom scheme before reuse can be counted");
        attributes.put("componentProductionOwnerReuseDecisionTags",
                PRODUCTION_OWNER_POSITIVE_FIXTURE_EXPECTED_TAGS);
        attributes.put("componentProductionOwnerReusePolicyVersionV1_20",
                COMPONENT_PRODUCTION_OWNER_REUSE_POLICY_VERSION_V1_20);
        attributes.put("componentProductionOwnerReusePolicyV1_20",
                COMPONENT_PRODUCTION_OWNER_REUSE_POLICY_V1_20);
        attributes.put("componentProductionOwnerReuseDecisionPhase", "before-component-imports");
        if (!rootArtifact) {
            byte[] dependencyBody = classpathResourceBytes(PRODUCTION_OWNER_FIXTURE_DEPENDENCY_V1_23_JS);
            attributes.put("productionBundleRuntimeOwnerPositiveFixtureDependencyBytes", dependencyBody.length);
            attributes.put("productionBundleRuntimeOwnerPositiveFixtureDependencySha256",
                    Resource.sha256(dependencyBody));
        }
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
        putProductionOwnerDependencyResolverAttributes(attributes, path);
        attributes.put("productionOwnerDependencyResource", true);
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

    private static Map<String, Object> frontendResourceLoaderAttributes() {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("frontendResourceLoaderVersion", FRONTEND_RESOURCE_LOADER_VERSION);
        attributes.put("frontendResourceLoaderModes", FRONTEND_RESOURCE_LOADER_MODES);
        attributes.put("frontendResourceLoaderPreferredMode", FRONTEND_RESOURCE_LOADER_PREFERRED_MODE);
        attributes.put("frontendResourceLoaderNoLocalhost", true);
        attributes.put("frontendResourceLoaderAcceptableFallbacks",
                FRONTEND_RESOURCE_LOADER_ACCEPTABLE_FALLBACKS);
        return attributes;
    }

    private static Map<String, Object> frontendResolverContractAttributes() {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("frontendResolverContractVersion", FRONTEND_RESOLVER_CONTRACT_VERSION);
        attributes.put("frontendResolverContractScope", FRONTEND_RESOLVER_CONTRACT_SCOPE);
        attributes.put("frontendResolverIdentityPolicy", FRONTEND_RESOLVER_IDENTITY_POLICY);
        attributes.put("frontendResolverIntegrityPolicy", FRONTEND_RESOLVER_INTEGRITY_POLICY);
        attributes.put("frontendResolverCachePolicy", FRONTEND_RESOLVER_CACHE_POLICY);
        attributes.put("frontendResolverCleanupPolicy", FRONTEND_RESOLVER_CLEANUP_POLICY);
        attributes.put("frontendResolverAllowedSchemes", FRONTEND_RESOLVER_ALLOWED_SCHEMES);
        attributes.put("frontendResolverNoLocalhost", true);
        attributes.put("frontendResolverFamiliesCovered", FRONTEND_RESOLVER_FAMILIES_COVERED);
        attributes.put("frontendResolverAcceptableGaps", FRONTEND_RESOLVER_ACCEPTABLE_GAPS);
        attributes.putAll(frontendPipelineClosureAttributes());
        return attributes;
    }

    private static Map<String, Object> frontendPipelineClosureAttributes() {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("frontendPipelineClosureVersion", FRONTEND_PIPELINE_CLOSURE_VERSION);
        attributes.put("frontendPipelineClosureScope", FRONTEND_PIPELINE_CLOSURE_SCOPE);
        attributes.put("frontendPipelineClosureTransport", FRONTEND_PIPELINE_CLOSURE_TRANSPORT);
        attributes.put("frontendPipelineClosureInfrastructureStatus",
                FRONTEND_PIPELINE_CLOSURE_INFRASTRUCTURE_STATUS);
        attributes.put("frontendPipelineClosureRealBuildStatus",
                FRONTEND_PIPELINE_CLOSURE_REAL_BUILD_STATUS);
        attributes.put("frontendPipelineClosureSatisfiedProofs",
                FRONTEND_PIPELINE_CLOSURE_SATISFIED_PROOFS);
        attributes.put("frontendPipelineClosureRemainingBlockers",
                FRONTEND_PIPELINE_CLOSURE_REMAINING_BLOCKERS);
        attributes.put("frontendPipelineClosureDecision", FRONTEND_PIPELINE_CLOSURE_DECISION);
        attributes.put("frontendPipelineClosureNextProductPath",
                FRONTEND_PIPELINE_CLOSURE_NEXT_PRODUCT_PATH);
        attributes.put("frontendPipelineClosureNoLocalhost", true);
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
            return FlowClientResources.classpathResourceBytes(path);
        }
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
            throw new IllegalStateException("Unable to read Flow IPC resource " + path, error);
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
            metadata.putAll(frontendResourceLoaderAttributes());
            metadata.putAll(frontendResolverContractAttributes());
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
