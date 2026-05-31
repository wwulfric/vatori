package dev.vatori.desktop.flow.ipc;

import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.router.Route;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
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
import java.util.stream.Stream;

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
    private static final String PACKAGED_FRONTEND_BASELINE_VERSION =
            "flow-ipc-packaged-frontend-baseline-v1_25";
    private static final String PACKAGED_FRONTEND_PRODUCT_PATH =
            "packaging-time-staged-frontend-baseline";
    private static final String PACKAGED_FRONTEND_INVENTORY_SCOPE =
            FRONTEND_RESOLVER_CONTRACT_SCOPE;
    private static final String PACKAGED_FRONTEND_PREFERRED_LOADER =
            "custom-scheme-file-backed";
    private static final String PACKAGED_FRONTEND_ALLOWED_FALLBACK =
            "diagnostic-blob-only";
    private static final String PACKAGED_FRONTEND_INTEGRITY =
            "sha256-and-byte-count";
    private static final String PACKAGED_FRONTEND_CLEANUP_POLICY =
            "window-lifetime-cache-cleanup";
    private static final String PACKAGED_FRONTEND_REAL_BUILD_STATUS =
            "product-parity-pending";
    private static final String PACKAGED_FRONTEND_REMAINING_BLOCKERS =
            FRONTEND_PIPELINE_CLOSURE_REMAINING_BLOCKERS;
    private static final String GENERATED_LAZY_SOURCE_BOUNDARY_VERSION =
            "flow-ipc-generated-lazy-source-boundary-v1_26";
    private static final String GENERATED_LAZY_SOURCE_MECHANISM =
            "vaadin-AbstractUpdateImports-generateLazyLoadedChunks";
    private static final String GENERATED_LAZY_SOURCE_ARTIFACT =
            "com.vaadin.flow.server.frontend.AbstractUpdateImports";
    private static final String GENERATED_LAZY_SOURCE_SCANNER =
            "com.vaadin.flow.server.frontend.scanner.FrontendDependencies";
    private static final String GENERATED_LAZY_SOURCE_LAZY_ROUTE_RULE =
            "ChunkInfo(type=ROUTE,eager=false)";
    private static final String GENERATED_LAZY_SOURCE_CHUNK_TRIGGER_INPUT =
            "DependencyTrigger classes plus lazyJavascript/lazyCss";
    private static final String GENERATED_LAZY_SOURCE_POSITIVE_TABLE_SHAPE =
            "branch-table-dynamic-import";
    private static final String GENERATED_LAZY_SOURCE_POSITIVE_IMPORT_PATTERN =
            "pending.push(import('./chunks/chunk-<content-hash>.js'))";
    private static final String GENERATED_LAZY_SOURCE_NO_CHUNK_FALLBACK =
            "Promise.resolve(0)";
    private static final String GENERATED_LAZY_SOURCE_CURRENT_BUILD_STATUS =
            "no-real-generated-lazy-chunk-produced";
    private static final String GENERATED_LAZY_SOURCE_BUILD_INPUT_STATUS =
            "current-smoke-dev-bundle-has-no-lazy-javascript-or-css-chunk-input";
    private static final String GENERATED_LAZY_SOURCE_SUPPORTED_BUILD_PATH =
            "run-vaadin-update-imports-with-non-eager-route-dependency-or-supported-lazy-chunk-spi";
    private static final String GENERATED_LAZY_SOURCE_DECISION =
            "build-spi-required-before-real-app-chunk-parity";
    private static final String GENERATED_LAZY_SOURCE_REMAINING_BLOCKER =
            "real-app-generated-chunk-file-not-produced-by-current-build";
    private static final String GENERATED_LAZY_SOURCE_NEXT_STEP =
            "make the app build produce a non-empty lazyJavascript/lazyCss ChunkInfo or add a supported Vaadin lazy chunk SPI before replacing the v1.22 fixture with real app/build chunk parity";
    private static final String GENERATED_LAZY_REAL_BUILD_PROBE_VERSION =
            "flow-ipc-generated-lazy-real-build-probe-v1_27";
    private static final String GENERATED_LAZY_REAL_BUILD_PROBE_PROPERTIES =
            "META-INF/vatori/generated-lazy-real-build-probe-v1_27.properties";
    private static final String GENERATED_LAZY_REAL_BUILD_DEFAULT_ROUTE_CLASS =
            "dev.vatori.ipcsmoke.FlowIpcGeneratedLazyProbeView";
    private static final String GENERATED_LAZY_REAL_BUILD_DEFAULT_ROUTE =
            "flow-ipc-lazy-probe-v1_27";
    private static final String GENERATED_LAZY_REAL_BUILD_DEFAULT_DEPENDENCY =
            "context://vatori-flow-ipc-real-build-probe-v1_27.js";
    private static final String GENERATED_LAZY_REAL_BUILD_DEFAULT_TRIGGER =
            GENERATED_LAZY_REAL_BUILD_DEFAULT_ROUTE_CLASS;
    private static final String GENERATED_LAZY_REAL_BUILD_SCANNER_SOURCE =
            "route-annotation-classpath-probe-v1_27";
    private static final String GENERATED_LAZY_REAL_BUILD_FAILURE_LAYER =
            "scanner-output-not-connected-to-dev-bundle-generated-imports";
    private static final String GENERATED_LAZY_REAL_BUILD_NEXT_STEP =
            "run a Vaadin frontend build/update-imports path that includes the non-eager v1.27 route input and compare generated-flow-imports output";
    private static final String GENERATED_LAZY_BUILD_LIFECYCLE_VERSION =
            "flow-ipc-generated-lazy-build-lifecycle-v1_28";
    private static final String GENERATED_LAZY_BUILD_LIFECYCLE_SCANNER_SOURCE =
            "route-annotation-classpath-probe-plus-vaadin-FrontendDependencies-contract-v1_28";
    private static final String GENERATED_LAZY_BUILD_LIFECYCLE_BUILD_TASK_SOURCE =
            "dev-ipc-runtime-classpath-resource-resolver-v1_28";
    private static final String GENERATED_LAZY_BUILD_LIFECYCLE_FAILURE_LAYER =
            "runtime-using-stale-packaged-dev-bundle";
    private static final String GENERATED_LAZY_BUILD_LIFECYCLE_NEXT_STEP =
            "connect dev-ipc resource resolution to a Vaadin prepare-frontend/update-imports output for the v1.27 route, or replace the static dev-bundle artifact with a supported generated lazy chunk build SPI";
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
    private static final String CURRENT_APP_PRODUCTION_OWNER_IMPORT_VERSION =
            "flow-ipc-current-app-production-owner-import-v1_36";
    private static final String CURRENT_APP_PRODUCTION_OWNER_IMPORT_SOURCE =
            "current-app-production-runtime-ownership-handoff-v1_35";
    private static final String CURRENT_APP_PRODUCTION_OWNER_IMPORT_POLICY =
            "owner-first-current-app-production-entry-import-v1_36";
    private static final String CURRENT_APP_PRODUCTION_OWNER_IMPORT_EXPECTED_SIDE_EFFECTS =
            "window.Vaadin,window.Vaadin.Flow,window.Vaadin.Flow.loadOnDemand,customElements:vaadin-vertical-layout|vaadin-text-field|vaadin-checkbox|vaadin-select";
    private static final String CURRENT_APP_PRODUCTION_OWNER_IMPORT_FAILURE_LAYER =
            "current-app-production-owner-import-side-effect-pending";
    private static final String CURRENT_APP_PRODUCTION_OWNER_IMPORT_NEXT_STEP =
            "make the current app production owner import expose reusable Flow/component side effects, or switch this handoff to a supported Vaadin build/runtime SPI";
    private static final String CURRENT_APP_PRODUCTION_OWNER_COMPONENT_REUSE_VERSION =
            "flow-ipc-current-app-production-owner-component-reuse-v1_37";
    private static final String CURRENT_APP_PRODUCTION_OWNER_COMPONENT_REUSE_SOURCE =
            "current-app-production-owner-import-side-effect-v1_36";
    private static final String CURRENT_APP_PRODUCTION_OWNER_COMPONENT_REUSE_POLICY =
            "reuse-current-app-owner-defined-custom-elements-before-focused-import-v1_37";
    private static final String CURRENT_APP_PRODUCTION_OWNER_COMPONENT_REUSE_EXPECTED_TAGS =
            "vaadin-text-field";
    private static final String CURRENT_APP_PRODUCTION_OWNER_COMPONENT_REUSE_FAILURE_LAYER =
            "current-app-production-owner-component-reuse-pending";
    private static final String CURRENT_APP_PRODUCTION_OWNER_COMPONENT_REUSE_NEXT_STEP =
            "extend current app owner reuse to earlier component paths and broader Vaadin component/resource coverage, then cover dev-mode/Vite parity";
    private static final String CURRENT_APP_PRODUCTION_OWNER_COMPONENT_REUSE_MATRIX_VERSION =
            "flow-ipc-current-app-owner-component-reuse-matrix-v1_38";
    private static final String CURRENT_APP_PRODUCTION_OWNER_COMPONENT_REUSE_MATRIX_SOURCE =
            "current-app-production-owner-component-reuse-v1_37";
    private static final String CURRENT_APP_PRODUCTION_OWNER_COMPONENT_REUSE_MATRIX_POLICY =
            "classify-current-app-owner-reuse-or-focused-fallback-for-covered-components-v1_38";
    private static final String CURRENT_APP_PRODUCTION_OWNER_COMPONENT_REUSE_MATRIX_EXPECTED_TAGS =
            "vaadin-big-decimal-field,vaadin-checkbox,vaadin-select,vaadin-text-field";
    private static final String CURRENT_APP_PRODUCTION_OWNER_COMPONENT_REUSE_MATRIX_FAILURE_LAYER =
            "current-app-owner-component-reuse-matrix-pending";
    private static final String CURRENT_APP_PRODUCTION_OWNER_COMPONENT_REUSE_MATRIX_NEXT_STEP =
            "move current app owner import earlier than covered component focused imports, or switch to a supported Vaadin build/runtime SPI before broadening component parity";
    private static final String CURRENT_APP_PRODUCTION_OWNER_EARLY_IMPORT_VERSION =
            "flow-ipc-current-app-production-owner-early-import-v1_39";
    private static final String CURRENT_APP_PRODUCTION_OWNER_EARLY_IMPORT_SOURCE =
            "current-app-owner-component-reuse-matrix-v1_38";
    private static final String CURRENT_APP_PRODUCTION_OWNER_EARLY_IMPORT_POLICY =
            "request-current-app-production-owner-before-covered-component-imports-v1_39";
    private static final String CURRENT_APP_PRODUCTION_OWNER_EARLY_IMPORT_PHASE =
            "before-component-imports";
    private static final String CURRENT_APP_PRODUCTION_OWNER_EARLY_IMPORT_REUSABLE_TAGS =
            "vaadin-checkbox,vaadin-select,vaadin-text-field";
    private static final String CURRENT_APP_PRODUCTION_OWNER_EARLY_IMPORT_FAILURE_LAYER =
            "current-app-production-owner-early-import-pending";
    private static final String CURRENT_APP_PRODUCTION_OWNER_EARLY_IMPORT_NEXT_STEP =
            "move remaining owner-ready focused fallback tags to a supported Vaadin build/runtime SPI or broaden current app owner tag coverage";
    private static final String CURRENT_APP_GENERATED_IMPORTS_OWNER_SIDE_EFFECT_VERSION =
            "flow-ipc-current-app-generated-imports-select-owner-coverage-v1_41";
    private static final String CURRENT_APP_GENERATED_IMPORTS_OWNER_SIDE_EFFECT_SOURCE =
            "current-app-generated-imports-owner-side-effect-v1_40";
    private static final String CURRENT_APP_GENERATED_IMPORTS_OWNER_SIDE_EFFECT_POLICY =
            "cover-current-app-production-generated-imports-select-before-components-v1_41";
    private static final String CURRENT_APP_GENERATED_IMPORTS_OWNER_SIDE_EFFECT_CANDIDATE =
            "current-app-production-generated-imports-support-artifact";
    private static final String CURRENT_APP_GENERATED_IMPORTS_OWNER_SIDE_EFFECT_EXPECTED_TAGS =
            "vaadin-big-decimal-field,vaadin-checkbox,vaadin-select,vaadin-text-field";
    private static final String CURRENT_APP_GENERATED_IMPORTS_OWNER_SIDE_EFFECT_COVERED_TAGS =
            "vaadin-big-decimal-field,vaadin-checkbox,vaadin-select,vaadin-text-field";
    private static final String CURRENT_APP_GENERATED_IMPORTS_OWNER_SIDE_EFFECT_KNOWN_MISSING_TAGS =
            "";
    private static final String CURRENT_APP_GENERATED_IMPORTS_OWNER_SIDE_EFFECT_EXPECTED_GLOBALS =
            "window.Vaadin,window.Vaadin.Flow,window.Vaadin.Flow.loadOnDemand";
    private static final String CURRENT_APP_GENERATED_IMPORTS_OWNER_SIDE_EFFECT_FAILURE_LAYER =
            "current-app-generated-imports-select-owner-coverage-pending";
    private static final String CURRENT_APP_GENERATED_IMPORTS_OWNER_SIDE_EFFECT_NEXT_STEP =
            "promote four-tag current app generated imports side effects into the owner runtime contract, then broaden component/resource coverage and dev-mode/Vite parity";
    private static final String CURRENT_APP_GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_VERSION =
            "flow-ipc-current-app-generated-imports-owner-runtime-contract-v1_42";
    private static final String CURRENT_APP_GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_SOURCE =
            "current-app-generated-imports-select-owner-coverage-v1_41";
    private static final String CURRENT_APP_GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_ARTIFACT_SOURCE =
            "current-app-production-build-frontend-generated-imports-support-artifact";
    private static final String CURRENT_APP_GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_OWNER_PRIORITY =
            "before-covered-focused-component-imports";
    private static final String CURRENT_APP_GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_SKIP_POLICY =
            "skip-covered-focused-imports-when-owner-side-effect-satisfied";
    private static final String CURRENT_APP_GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_FALLBACK_POLICY =
            "strict-blocker-with-focused-fallback-diagnostics";
    private static final String CURRENT_APP_GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_RESOLVER_HANDOFF =
            "component-resolver-owned-input";
    private static final String CURRENT_APP_GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_FAILURE_LAYER =
            "current-app-generated-imports-owner-runtime-contract-pending";
    private static final String CURRENT_APP_GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_NEXT_STEP =
            "connect this owner contract to real generated lazy chunk SPI, dev-mode/Vite parity, and broader component/resource coverage";
    private static final String CURRENT_APP_GENERATED_IMPORTS_OWNER_LAZY_CHUNK_BRIDGE_VERSION =
            "flow-ipc-current-app-generated-imports-owner-lazy-chunk-bridge-v1_42";
    private static final String CURRENT_APP_GENERATED_IMPORTS_OWNER_LAZY_CHUNK_BRIDGE_SOURCE =
            "current-app-build-frontend-output-v1_32";
    private static final String CURRENT_APP_GENERATED_IMPORTS_OWNER_LAZY_CHUNK_SPI_DECISION =
            "packaging-time-build-frontend-staging";
    private static final String GENERATED_LAZY_SPI_DECISION_VERSION =
            "flow-ipc-generated-lazy-spi-decision-v1_43";
    private static final String GENERATED_LAZY_SPI_DECISION_SOURCE =
            "current-app-generated-imports-owner-runtime-contract-v1_42";
    private static final String GENERATED_LAZY_SPI_AUDITED_SOURCE_SURFACES =
            "AbstractUpdateImports,FrontendDependencies,ChunkInfo,build-frontend-generated-output,production-stats-json,prod-bundle";
    private static final String GENERATED_LAZY_SPI_CHUNK_INFO_API_STATUS =
            "internal-may-be-renamed-or-removed";
    private static final String GENERATED_LAZY_SPI_REGISTRY_SOURCE =
            "current-app-build-frontend-output-v1_32";
    private static final String GENERATED_LAZY_SPI_DECISION =
            "packaging-time-build-frontend-staging";
    private static final String GENERATED_LAZY_SPI_UNAVAILABLE_REASON =
            "vaadin-chunkinfo-internal-no-supported-runtime-spi";
    private static final String GENERATED_LAZY_SPI_FAILURE_LAYER =
            "supported-vaadin-lazy-chunk-spi-unavailable";
    private static final String GENERATED_LAZY_SPI_PRODUCT_PATH =
            "packaging-time-build-frontend-output-staging";
    private static final String GENERATED_LAZY_SPI_EXECUTION_EVIDENCE =
            "current-app-generated-chunk-loadondemand-executed-v1_33";
    private static final String GENERATED_LAZY_SPI_EXECUTION_TRIGGER =
            "host-decision-audit-probe-not-flow-client-spi";
    private static final String GENERATED_LAZY_SPI_NEXT_STEP =
            "productize packaging-time build-frontend artifacts as framework-managed staged frontend inventory, or upstream/support a stable Vaadin lazy chunk SPI";
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
        add(discoveredComponentResources, DISABLE_ON_CLICK_FUNCTIONS_JS,
                "vaadin-generated-jar-resource-disable-on-click", true,
                currentAppGeneratedImportsSupportAttributes(
                        DISABLE_ON_CLICK_FUNCTIONS_JS,
                        "Frontend/generated/jar-resources/disableOnClickFunctions.js",
                        "current-app-generated-imports-disable-on-click-v1_33",
                        false));
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
        addCurrentAppBuildFrontendGeneratedLazyResources(discoveredComponentResources);
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
        manifest.putAll(generatedLazySourceBoundaryAttributes());
        manifest.putAll(generatedLazyRealBuildProbeAttributes());
        manifest.putAll(generatedLazyBuildLifecycleAttributes());
        manifest.putAll(generatedLazyAppBuildOutputAttributes());
        manifest.putAll(currentAppFrontendBuildOutputAttributes());
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
                strictBlocker("generated-lazy", GENERATED_LAZY_BUILD_LIFECYCLE_VERSION,
                        "the v1.27 route/lazy JavaScript probe is visible on the app classpath, but the no-localhost runtime still resolves generated-flow-imports from the static packaged Vaadin dev-bundle instead of an app update-imports output"),
                strictBlocker("generated-lazy", GENERATED_LAZY_APP_BUILD_OUTPUT_VERSION,
                        "current app build output discovery is explicit, but generated-flow-imports is still missing from the app module output so the runtime uses the packaged dev-bundle fallback"),
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
        Path sourceGeneratedImports = sourceGeneratedImportsPath(appModuleDirectory());
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
        Path productionBuildRoot = productionBuildRootPath(appModuleDirectory());
        if (!Files.isDirectory(productionBuildRoot)) {
            return;
        }
        try (Stream<Path> stream = Files.list(productionBuildRoot)) {
            for (Path artifact : stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".js"))
                    .sorted()
                    .toList()) {
                String virtualPath = currentAppProductionBuildVirtualPath(artifact);
                addFile(target,
                        virtualPath,
                        artifact,
                        "vaadin-current-app-production-build-resource",
                        true,
                        currentAppProductionBuildAttributes(evidence, virtualPath));
            }
        } catch (IOException error) {
            throw new IllegalStateException("Unable to inspect current app production build output "
                    + productionBuildRoot, error);
        }
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

    private static Map<String, Object> currentAppGeneratedImportsSupportAttributes(
            String path, String specifier, String lookupKind, boolean placeholder) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("componentFrontendResource", true);
        attributes.put("currentAppBuildFrontendSupportResource", true);
        attributes.put("currentAppGeneratedChunkPackageResolverVersion",
                CURRENT_APP_GENERATED_CHUNK_PACKAGE_RESOLVER_VERSION);
        attributes.put("currentAppGeneratedChunkPackageResolverSource",
                CURRENT_APP_GENERATED_CHUNK_PACKAGE_RESOLVER_SOURCE);
        attributes.put("currentAppGeneratedChunkPackageResolverMode",
                placeholder ? "diagnostic-placeholder-for-root-generated-imports"
                        : CURRENT_APP_GENERATED_CHUNK_PACKAGE_RESOLVER_MODE);
        attributes.put("moduleSpecifier", specifier);
        attributes.put("resolver", placeholder
                ? "current-app-generated-imports-diagnostic-placeholder-v1_33"
                : "current-app-generated-imports-classpath-support-v1_33");
        attributes.put("shim", placeholder);
        attributes.put("artifactFamily", placeholder ? "current-app-generated-imports-placeholder"
                : "vaadin-component-classpath-frontend");
        attributes.put("origin", placeholder ? "diagnostic-placeholder" : "classpath");
        attributes.put("resourceOrigin", placeholder ? "current-app-generated-imports-placeholder"
                : "classpath-frontend-resource");
        attributes.put("dependencyOf", CURRENT_APP_BUILD_FRONTEND_GENERATED_IMPORTS_JS);
        attributes.put("executionPolicy", "current-app-generated-imports-support-module-v1_33");
        attributes.put("artifactStatus", placeholder ? "diagnostic-placeholder" : "available");
        attributes.put("requestedBy", "component-frontend-manifest-v1_33");
        attributes.put("failureClassification", placeholder
                ? "current-app-generated-imports-root-placeholder-v1_33"
                : "current-app-generated-imports-support-resource-v1_33");
        attributes.put("blockedReason", placeholder
                ? "Current app generated imports root still depends on @vaadin/common-frontend/ConnectionIndicator.js; this diagnostic placeholder lets the source generated loadOnDemand table execute while preserving the remaining package graph blocker."
                : "Classpath support resource for current app source generated imports root.");
        attributes.put("lookupKind", lookupKind);
        attributes.put("componentBundleDependency", true);
        attributes.put("currentAppGeneratedChunkPackageFailureLayer",
                placeholder ? CURRENT_APP_GENERATED_CHUNK_PACKAGE_FAILURE_LAYER : "");
        attributes.put("currentAppGeneratedChunkPackageNextResolutionStep",
                placeholder ? CURRENT_APP_GENERATED_CHUNK_PACKAGE_NEXT_STEP : "");
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

    private static Map<String, Object> currentAppBuildFrontendGeneratedLazyAttributes(
            CurrentAppBuildFrontendOutputEvidence evidence, boolean importsArtifact) {
        String path = importsArtifact
                ? CURRENT_APP_BUILD_FRONTEND_GENERATED_IMPORTS_JS
                : evidence.sourceChunkVirtualPath();
        FrontendBuildStepEvidence buildStep = frontendBuildStepEvidence();
        AppBuildGeneratedImportsEvidence appOutput = appBuildGeneratedImportsEvidence();
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("componentFrontendResource", true);
        attributes.put("generatedLazyResource", true);
        attributes.put("currentAppBuildFrontendResource", true);
        attributes.put("currentAppBuildFrontendResourceRole", importsArtifact ? "generated-imports" : "chunk");
        attributes.put("generatedLazyBundleChunk", importsArtifact);
        attributes.put("generatedLazyBundleDependency", !importsArtifact);
        attributes.put("moduleSpecifier", path);
        attributes.put("resolver", "current-app-build-frontend-output-v1_32");
        attributes.put("shim", false);
        attributes.put("artifactFamily", "generated-lazy");
        attributes.put("origin", "current-app-build-output");
        attributes.put("resourceOrigin", importsArtifact
                ? "current-app-source-generated-flow-imports"
                : "current-app-source-generated-flow-imports-chunk");
        attributes.put("dependencyOf", importsArtifact ? "Flow.loadOnDemand"
                : CURRENT_APP_BUILD_FRONTEND_GENERATED_IMPORTS_JS);
        attributes.put("executionPolicy", importsArtifact
                ? "stage-and-import-current-app-generated-lazy-v1_32"
                : "current-app-generated-lazy-chunk-body-v1_32");
        attributes.put("artifactStatus", "available");
        attributes.put("requestedBy", "component-frontend-manifest-v1_32");
        attributes.put("failureClassification", importsArtifact
                ? "current-app-build-frontend-generated-imports-v1_32"
                : "current-app-build-frontend-generated-chunk-v1_32");
        attributes.put("blockedReason", evidence.failureLayer());
        attributes.put("lookupKind", importsArtifact
                ? "Current-app-generated-flow-imports-build-output-v1_32"
                : "Current-app-generated-flow-imports-chunk-build-output-v1_32");
        attributes.put("blockerVersion", CURRENT_APP_BUILD_FRONTEND_OUTPUT_VERSION);
        attributes.put("failureLayer", evidence.failureLayer());
        attributes.put("nextResolutionStep", CURRENT_APP_BUILD_FRONTEND_NEXT_STEP);
        attributes.put("currentAppBuildFrontendOutputVersion", CURRENT_APP_BUILD_FRONTEND_OUTPUT_VERSION);
        attributes.put("currentAppBuildFrontendOutputSource", CURRENT_APP_BUILD_FRONTEND_OUTPUT_SOURCE);
        attributes.put("currentAppBuildFrontendGoal", evidence.goal());
        attributes.put("currentAppBuildFrontendRuntimeCandidate", evidence.runtimeCandidate());
        attributes.put("currentAppBuildFrontendFailureLayer", evidence.failureLayer());
        attributes.put("currentAppBuildFrontendSourceGeneratedImportsObserved",
                evidence.sourceGeneratedImportsObserved());
        attributes.put("currentAppBuildFrontendSourceGeneratedImportsPath",
                evidence.sourceGeneratedImportsPath());
        attributes.put("currentAppBuildFrontendSourceGeneratedImportsSha256",
                evidence.sourceGeneratedImportsSha256());
        attributes.put("currentAppBuildFrontendSourceGeneratedImportsTableShape",
                evidence.sourceGeneratedImportsTableShape());
        attributes.put("currentAppBuildFrontendSourceChunkStatus", evidence.sourceChunkStatus());
        attributes.put("currentAppBuildFrontendSourceChunkArtifact", evidence.sourceChunkArtifact());
        attributes.put("currentAppBuildFrontendSourceChunkVirtualPath", evidence.sourceChunkVirtualPath());
        attributes.put("currentAppBuildFrontendSourceChunkContainsBareImports",
                evidence.sourceChunkContainsBareImports());
        attributes.put("currentAppGeneratedChunkPackageResolverVersion",
                CURRENT_APP_GENERATED_CHUNK_PACKAGE_RESOLVER_VERSION);
        attributes.put("currentAppGeneratedChunkPackageResolverSource",
                CURRENT_APP_GENERATED_CHUNK_PACKAGE_RESOLVER_SOURCE);
        attributes.put("currentAppGeneratedChunkPackageResolverMode", evidence.sourceChunkPackageResolverMode());
        attributes.put("currentAppGeneratedChunkPackageBareSpecifiers",
                evidence.sourceChunkBareImportSpecifiers());
        attributes.put("currentAppGeneratedChunkPackageResolvedSpecifiers",
                evidence.sourceChunkPackageResolvedSpecifiers());
        attributes.put("currentAppGeneratedChunkPackageUnresolvedSpecifiers",
                evidence.sourceChunkPackageUnresolvedSpecifiers());
        attributes.put("currentAppGeneratedChunkPackageFailureLayer", evidence.failureLayer());
        attributes.put("currentAppGeneratedChunkPackageNextResolutionStep",
                CURRENT_APP_GENERATED_CHUNK_PACKAGE_NEXT_STEP);
        attributes.putAll(currentAppGeneratedImportsRootPackageGraphAttributes(evidence));
        attributes.put("currentAppBuildFrontendProductionBuildObserved",
                evidence.productionBuildObserved());
        attributes.put("currentAppBuildFrontendProductionGeneratedImportsObserved",
                evidence.productionGeneratedImportsObserved());
        attributes.put("currentAppBuildFrontendProductionGeneratedImportsPath",
                evidence.productionGeneratedImportsPath());
        attributes.put("currentAppBuildFrontendProdBundleObserved", evidence.prodBundleObserved());
        attributes.put("currentAppBuildFrontendProdBundlePath", evidence.prodBundlePath());
        attributes.put("currentAppBuildFrontendNoLocalhost", true);
        attributes.put("currentAppBuildFrontendFixtureSubstituted", false);
        attributes.putAll(currentAppProductionRuntimeOwnershipAttributes(evidence));
        attributes.putAll(frontendBuildPreflightAvailabilityAttributes(
                buildStep, appOutput, frontendBuildRuntimeSource(buildStep, appOutput)));
        if (importsArtifact) {
            attributes.put("generatedLazyLoadOnDemandContractVersion",
                    GENERATED_LAZY_LOAD_ON_DEMAND_CONTRACT_VERSION);
            attributes.put("generatedLazyTriggerSource", "current-app-build-frontend-output-v1_32");
            attributes.put("generatedLazyApiOwner", GENERATED_LAZY_LOAD_ON_DEMAND_API_OWNER);
            attributes.put("generatedLazyApiInstallSource",
                    "current-app-generated-flow-imports-after-staged-import-v1_32");
            attributes.put("generatedLazyRequestedChunk", evidence.sourceChunkId());
            attributes.put("generatedLazyRegistrySource", "current-app-generated-flow-imports-source-v1_32");
            attributes.put("generatedLazyMappingSource", "current-app-generated-chunk-map-v1_32");
            attributes.put("generatedLazyResolvedArtifactPath", path);
            attributes.put("generatedLazyFlowClientTriggered", false);
            attributes.put("generatedLazyDiagnosticBridge", false);
            attributes.put("generatedLazyChunkExpressionSource", "bootstrap-uidl-dependencies");
            attributes.put("generatedLazyChunkTriggerOwner", "visible-flow-client-dependencyloader");
            attributes.put("generatedLazyChunkResolvedArtifactPath", path);
            attributes.put("generatedLazyChunkFailureLayer", evidence.failureLayer());
            attributes.put("generatedLazyChunkBlockerVersion", CURRENT_APP_BUILD_FRONTEND_OUTPUT_VERSION);
            attributes.put("generatedLazyChunkNextResolutionStep", CURRENT_APP_BUILD_FRONTEND_NEXT_STEP);
            attributes.put("loadOnDemandChunkContractVersion", GENERATED_LAZY_CHUNK_CONTRACT_VERSION);
            attributes.put("loadOnDemandChunkApiOwner", GENERATED_LAZY_LOAD_ON_DEMAND_API_OWNER);
            attributes.put("loadOnDemandChunkApiInstallSource",
                    "current-app-build-frontend-generated-imports-v1_32");
            attributes.put("loadOnDemandChunkRegistrySource", "current-app-generated-flow-imports-source-v1_32");
            attributes.put("loadOnDemandChunkMappingSource", "current-app-generated-chunk-map-v1_32");
            attributes.put("loadOnDemandChunkResolvedArtifactPath", path);
            attributes.put("loadOnDemandChunkModuleIdentity",
                    "stable-current-app-build-frontend-module-identity-v1_32:" + path);
            attributes.put("loadOnDemandChunkFailureLayer", evidence.failureLayer());
            attributes.put("loadOnDemandChunkBlockerVersion", CURRENT_APP_BUILD_FRONTEND_OUTPUT_VERSION);
            attributes.put("loadOnDemandChunkNextResolutionStep", CURRENT_APP_BUILD_FRONTEND_NEXT_STEP);
            attributes.put("chunkId", evidence.sourceChunkId());
            attributes.put("chunkImportSpecifier", evidence.sourceChunkImportSpecifier());
            attributes.put("chunkArtifactPath", evidence.sourceChunkVirtualPath());
            attributes.put("chunkFileStatus", evidence.sourceChunkStatus());
            attributes.put("chunkFileBytes", evidence.sourceChunkBytes());
            attributes.put("chunkFileSha256", evidence.sourceChunkSha256());
            attributes.put("chunkFileResolver", "current-app-generated-flow-imports-relative-chunk-v1_32");
            attributes.put("loadOnDemandChunkFileRegistryVersion", CURRENT_APP_BUILD_FRONTEND_OUTPUT_VERSION);
            attributes.put("loadOnDemandChunkFileRegistrySource",
                    "current-app-generated-flow-imports-body-parser-v1_32");
            attributes.put("loadOnDemandChunkFileMappingSource", "current-app-generated-chunk-map-v1_32");
            attributes.put("loadOnDemandChunkFileGeneratedImportsArtifact", path);
            attributes.put("loadOnDemandChunkFileNoOpLoadOnDemand", false);
            attributes.put("loadOnDemandChunkFileTableShape", evidence.sourceGeneratedImportsTableShape());
            attributes.put("loadOnDemandChunkFileResolver",
                    "current-app-generated-flow-imports-relative-chunk-v1_32");
            attributes.put("loadOnDemandChunkFileStatus", evidence.sourceChunkStatus());
            attributes.put("loadOnDemandChunkFileBytes", evidence.sourceChunkBytes());
            attributes.put("loadOnDemandChunkFileSha256", evidence.sourceChunkSha256());
            attributes.put("loadOnDemandChunkFileFailureLayer", evidence.failureLayer());
            attributes.put("loadOnDemandChunkFileBlockerVersion",
                    CURRENT_APP_GENERATED_CHUNK_PACKAGE_RESOLVER_VERSION);
            attributes.put("loadOnDemandChunkFileNextResolutionStep",
                    CURRENT_APP_GENERATED_CHUNK_PACKAGE_NEXT_STEP);
            attributes.put("loadOnDemandChunkFileSideEffectProof",
                    evidence.sourceChunkPackageUnresolvedSpecifiers().isBlank()
                            && evidence.sourceGeneratedImportsRootUnresolvedSpecifiers().isBlank()
                            ? "current-app-source-generated-imports-root-package-graph-resolved-v1_34"
                            : "chunk-file-body-available-but-bare-npm-imports-remain");
            attributes.put("importRewriteStrategy", "current-app-generated-imports-root-package-graph-rewrite-v1_34");
            attributes.put("browserExecutionPlan",
                    "dynamic-import-current-app-generated-imports-with-root-package-graph-rewrites-v1_34");
            attributes.put("importRewrites", currentAppGeneratedImportsImportRewrites(evidence));
            attributes.put("transitiveImports", currentAppGeneratedImportsTransitiveImports(evidence));
            attributes.put("currentAppGeneratedChunkPackageRootPlaceholderUsed",
                    evidence.sourceGeneratedImportsRootPlaceholderUsed());
            attributes.putAll(currentAppBuildFrontendOutputAttributes());
        } else {
            attributes.put("chunkId", evidence.sourceChunkId());
            attributes.put("chunkArtifactPath", path);
            attributes.put("chunkFileStatus", evidence.sourceChunkStatus());
            attributes.put("chunkFileBytes", evidence.sourceChunkBytes());
            attributes.put("chunkFileSha256", evidence.sourceChunkSha256());
            attributes.put("chunkFileResolver", "current-app-generated-flow-imports-relative-chunk-v1_32");
            attributes.put("loadOnDemandChunkFileStatus", evidence.sourceChunkStatus());
            attributes.put("loadOnDemandChunkFileBytes", evidence.sourceChunkBytes());
            attributes.put("loadOnDemandChunkFileSha256", evidence.sourceChunkSha256());
            attributes.put("loadOnDemandChunkFileSideEffectProof",
                    evidence.sourceChunkPackageUnresolvedSpecifiers().isBlank()
                            ? "current-app-source-generated-chunk-package-edge-resolved-v1_33"
                            : "chunk-file-body-available-but-bare-npm-imports-remain");
            attributes.put("importRewriteStrategy", "current-app-generated-chunk-package-rewrite-v1_33");
            attributes.put("browserExecutionPlan",
                    "dynamic-import-current-app-generated-chunk-with-package-rewrites-v1_33");
            attributes.put("importRewrites", currentAppGeneratedChunkImportRewrites());
            attributes.put("transitiveImports", List.of(DEV_BUNDLE_INDEX_HTML_JS, DEV_BUNDLE_COMMONJS_JS));
        }
        return attributes;
    }

    private static List<Map<String, Object>> currentAppGeneratedImportsImportRewrites(
            CurrentAppBuildFrontendOutputEvidence evidence) {
        List<Map<String, Object>> rewrites = new ArrayList<>();
        String productionSupport = evidence.sourceGeneratedImportsRootProductionSupportArtifact();
        if (!productionSupport.isBlank()) {
            for (String specifier : csvValues(evidence.sourceGeneratedImportsRootBareImportSpecifiers())) {
                rewrites.add(importRewrite(specifier,
                        productionSupport,
                        "current-app-production-build",
                        "current-app-generated-imports-root-production-support-rewrite-v1_34",
                        "current-app-generated-imports-root-package-graph-v1_34"));
            }
        }
        if (!evidence.sourceChunkImportSpecifier().isBlank()
                && !evidence.sourceChunkVirtualPath().isBlank()) {
            rewrites.add(importRewrite(evidence.sourceChunkImportSpecifier(),
                    evidence.sourceChunkVirtualPath(),
                    "generated-lazy",
                    "current-app-generated-imports-dynamic-chunk-rewrite-v1_33",
                    "current-app-generated-chunk-package-edge-v1_33"));
        }
        return rewrites;
    }

    private static List<Map<String, Object>> currentAppGeneratedChunkImportRewrites() {
        return List.of(importRewrite("@vaadin/vertical-layout/src/vaadin-vertical-layout.js",
                DEV_BUNDLE_INDEX_HTML_JS,
                "production-bundle",
                "current-app-generated-chunk-production-indexhtml-rewrite-v1_33",
                "current-app-generated-chunk-package-edge-v1_33"));
    }

    private static List<String> currentAppGeneratedImportsTransitiveImports(
            CurrentAppBuildFrontendOutputEvidence evidence) {
        List<String> imports = new ArrayList<>();
        for (String path : csvValues(evidence.productionBuildJsVirtualPaths())) {
            if (!imports.contains(path)) {
                imports.add(path);
            }
        }
        if (!evidence.sourceChunkVirtualPath().isBlank()) {
            imports.add(evidence.sourceChunkVirtualPath());
        }
        return imports;
    }

    private static Map<String, Object> currentAppGeneratedImportsRootPackageGraphAttributes(
            CurrentAppBuildFrontendOutputEvidence evidence) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("currentAppGeneratedImportsRootPackageGraphVersion",
                CURRENT_APP_GENERATED_IMPORTS_ROOT_PACKAGE_GRAPH_VERSION);
        attributes.put("currentAppGeneratedImportsRootPackageGraphSource",
                CURRENT_APP_GENERATED_IMPORTS_ROOT_PACKAGE_GRAPH_SOURCE);
        attributes.put("currentAppGeneratedImportsRootPackageResolverMode",
                evidence.sourceGeneratedImportsRootPackageResolverMode());
        attributes.put("currentAppGeneratedImportsRootBareSpecifiers",
                evidence.sourceGeneratedImportsRootBareImportSpecifiers());
        attributes.put("currentAppGeneratedImportsRootStatsBundleImports",
                evidence.statsBundleImports());
        attributes.put("currentAppGeneratedImportsRootResolvedSpecifiers",
                evidence.sourceGeneratedImportsRootResolvedSpecifiers());
        attributes.put("currentAppGeneratedImportsRootUnresolvedSpecifiers",
                evidence.sourceGeneratedImportsRootUnresolvedSpecifiers());
        attributes.put("currentAppGeneratedImportsRootProductionSupportArtifact",
                evidence.sourceGeneratedImportsRootProductionSupportArtifact());
        attributes.put("currentAppGeneratedImportsRootProductionSupportObserved",
                evidence.sourceGeneratedImportsRootProductionSupportObserved());
        attributes.put("currentAppGeneratedImportsRootProductionEntryArtifact",
                evidence.productionEntryScriptVirtualPath());
        attributes.put("currentAppGeneratedImportsRootProductionBuildArtifacts",
                evidence.productionBuildJsVirtualPaths());
        attributes.put("currentAppGeneratedImportsRootPlaceholderUsed",
                evidence.sourceGeneratedImportsRootPlaceholderUsed());
        attributes.put("currentAppGeneratedImportsRootFailureLayer",
                evidence.sourceGeneratedImportsRootFailureLayer());
        attributes.put("currentAppGeneratedImportsRootNextResolutionStep",
                CURRENT_APP_GENERATED_IMPORTS_ROOT_NEXT_STEP);
        attributes.putAll(currentAppProductionRuntimeOwnershipAttributes(evidence));
        return attributes;
    }

    private static Map<String, Object> currentAppProductionRuntimeOwnershipAttributes(
            CurrentAppBuildFrontendOutputEvidence evidence) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        boolean rootResolved = evidence.sourceGeneratedImportsRootUnresolvedSpecifiers().isBlank()
                && evidence.sourceGeneratedImportsRootProductionSupportObserved()
                && !evidence.sourceGeneratedImportsRootPlaceholderUsed();
        boolean chunkResolved = "available".equals(evidence.sourceChunkStatus())
                && evidence.sourceChunkPackageUnresolvedSpecifiers().isBlank();
        boolean handoffReady = evidence.sourceGeneratedImportsObserved()
                && chunkResolved
                && rootResolved
                && evidence.productionGeneratedImportsObserved()
                && evidence.statsObserved()
                && evidence.prodBundleObserved();
        attributes.put("currentAppProductionRuntimeOwnershipVersion",
                CURRENT_APP_PRODUCTION_RUNTIME_OWNERSHIP_VERSION);
        attributes.put("currentAppProductionRuntimeOwnershipSource",
                CURRENT_APP_PRODUCTION_RUNTIME_OWNERSHIP_SOURCE);
        attributes.put("currentAppProductionRuntimeOwnershipPrimaryCandidate",
                CURRENT_APP_PRODUCTION_RUNTIME_OWNERSHIP_PRIMARY_CANDIDATE);
        attributes.put("currentAppProductionRuntimeOwnershipHandoffPolicy",
                CURRENT_APP_PRODUCTION_RUNTIME_OWNERSHIP_HANDOFF_POLICY);
        attributes.put("currentAppProductionRuntimeOwnershipSelectedImportsArtifact",
                CURRENT_APP_BUILD_FRONTEND_GENERATED_IMPORTS_JS);
        attributes.put("currentAppProductionRuntimeOwnershipSelectedChunkArtifact",
                evidence.sourceChunkVirtualPath());
        attributes.put("currentAppProductionRuntimeOwnershipProductionSupportArtifact",
                evidence.sourceGeneratedImportsRootProductionSupportArtifact());
        attributes.put("currentAppProductionRuntimeOwnershipProductionEntryArtifact",
                evidence.productionEntryScriptVirtualPath());
        attributes.put("currentAppProductionRuntimeOwnershipStatsObserved", evidence.statsObserved());
        attributes.put("currentAppProductionRuntimeOwnershipProdBundleObserved", evidence.prodBundleObserved());
        attributes.put("currentAppProductionRuntimeOwnershipArtifactInventory",
                currentAppProductionRuntimeOwnershipArtifactInventory(evidence));
        attributes.put("currentAppProductionRuntimeOwnershipRootPackageGraphResolved", rootResolved);
        attributes.put("currentAppProductionRuntimeOwnershipChunkPackageGraphResolved", chunkResolved);
        attributes.put("currentAppProductionRuntimeOwnershipProductionSupportObserved",
                evidence.sourceGeneratedImportsRootProductionSupportObserved());
        attributes.put("currentAppProductionRuntimeOwnershipHandoffReady", handoffReady);
        attributes.put("currentAppProductionRuntimeOwnershipOwnerSideEffectSatisfied", false);
        attributes.put("currentAppProductionRuntimeOwnershipOwnerReuseSatisfied", false);
        attributes.put("currentAppProductionRuntimeOwnershipNoLocalhost", true);
        attributes.put("currentAppProductionRuntimeOwnershipFixtureSubstituted", false);
        attributes.put("currentAppProductionRuntimeOwnershipFailureLayer",
                handoffReady ? CURRENT_APP_PRODUCTION_RUNTIME_OWNERSHIP_FAILURE_LAYER : evidence.failureLayer());
        attributes.put("currentAppProductionRuntimeOwnershipNextResolutionStep",
                CURRENT_APP_PRODUCTION_RUNTIME_OWNERSHIP_NEXT_STEP);
        attributes.putAll(currentAppProductionOwnerImportAttributes(evidence, handoffReady));
        return attributes;
    }

    private static Map<String, Object> currentAppProductionOwnerImportAttributes(
            CurrentAppBuildFrontendOutputEvidence evidence, boolean handoffReady) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("currentAppProductionOwnerImportVersion",
                CURRENT_APP_PRODUCTION_OWNER_IMPORT_VERSION);
        attributes.put("currentAppProductionOwnerImportSource",
                CURRENT_APP_PRODUCTION_OWNER_IMPORT_SOURCE);
        attributes.put("currentAppProductionOwnerImportPolicy",
                CURRENT_APP_PRODUCTION_OWNER_IMPORT_POLICY);
        attributes.put("currentAppProductionOwnerImportCandidate",
                CURRENT_APP_PRODUCTION_RUNTIME_OWNERSHIP_PRIMARY_CANDIDATE);
        attributes.put("currentAppProductionOwnerImportArtifact",
                evidence.productionEntryScriptVirtualPath());
        attributes.put("currentAppProductionOwnerImportSupportArtifact",
                evidence.sourceGeneratedImportsRootProductionSupportArtifact());
        attributes.put("currentAppProductionOwnerImportDependencyPaths",
                evidence.productionBuildJsVirtualPaths());
        attributes.put("currentAppProductionOwnerImportExpectedSideEffects",
                CURRENT_APP_PRODUCTION_OWNER_IMPORT_EXPECTED_SIDE_EFFECTS);
        attributes.put("currentAppProductionOwnerImportPhase",
                "after-current-app-generated-root-execution");
        attributes.put("currentAppProductionOwnerImportHandoffReady", handoffReady);
        attributes.put("currentAppProductionOwnerImportAttempted", false);
        attributes.put("currentAppProductionOwnerImported", false);
        attributes.put("currentAppProductionOwnerImportOverCustomScheme", false);
        attributes.put("currentAppProductionOwnerImportDependencyStageCount", 0);
        attributes.put("currentAppProductionOwnerImportSideEffectSatisfied", false);
        attributes.put("currentAppProductionOwnerImportReuseSatisfied", false);
        attributes.put("currentAppProductionOwnerImportNoLocalhost", true);
        attributes.put("currentAppProductionOwnerImportFixtureSubstituted", false);
        attributes.put("currentAppProductionOwnerImportFailureLayer",
                handoffReady ? CURRENT_APP_PRODUCTION_OWNER_IMPORT_FAILURE_LAYER : evidence.failureLayer());
        attributes.put("currentAppProductionOwnerImportNextResolutionStep",
                CURRENT_APP_PRODUCTION_OWNER_IMPORT_NEXT_STEP);
        attributes.put("currentAppProductionOwnerComponentReuseVersion",
                CURRENT_APP_PRODUCTION_OWNER_COMPONENT_REUSE_VERSION);
        attributes.put("currentAppProductionOwnerComponentReuseSource",
                CURRENT_APP_PRODUCTION_OWNER_COMPONENT_REUSE_SOURCE);
        attributes.put("currentAppProductionOwnerComponentReusePolicy",
                CURRENT_APP_PRODUCTION_OWNER_COMPONENT_REUSE_POLICY);
        attributes.put("currentAppProductionOwnerComponentReuseExpectedTags",
                CURRENT_APP_PRODUCTION_OWNER_COMPONENT_REUSE_EXPECTED_TAGS);
        attributes.put("currentAppProductionOwnerComponentReuseDecision",
                "pending-current-app-owner-reuse-check");
        attributes.put("currentAppProductionOwnerComponentReuseReason",
                "current app production owner import side effects must be checked before focused component import");
        attributes.put("currentAppProductionOwnerComponentReuseReusedTags", "");
        attributes.put("currentAppProductionOwnerComponentReuseSkippedFocusedImports", "");
        attributes.put("currentAppProductionOwnerComponentReuseFallbackFocusedImports", "");
        attributes.put("currentAppProductionOwnerComponentReuseFailureLayer",
                handoffReady ? CURRENT_APP_PRODUCTION_OWNER_COMPONENT_REUSE_FAILURE_LAYER : evidence.failureLayer());
        attributes.put("currentAppProductionOwnerComponentReuseNextResolutionStep",
                CURRENT_APP_PRODUCTION_OWNER_COMPONENT_REUSE_NEXT_STEP);
        attributes.put("currentAppProductionOwnerComponentReused", false);
        attributes.put("currentAppProductionOwnerComponentReuseNoLocalhost", true);
        attributes.put("currentAppProductionOwnerComponentReuseMatrixVersion",
                CURRENT_APP_PRODUCTION_OWNER_COMPONENT_REUSE_MATRIX_VERSION);
        attributes.put("currentAppProductionOwnerComponentReuseMatrixSource",
                CURRENT_APP_PRODUCTION_OWNER_COMPONENT_REUSE_MATRIX_SOURCE);
        attributes.put("currentAppProductionOwnerComponentReuseMatrixPolicy",
                CURRENT_APP_PRODUCTION_OWNER_COMPONENT_REUSE_MATRIX_POLICY);
        attributes.put("currentAppProductionOwnerComponentReuseMatrixExpectedTags",
                CURRENT_APP_PRODUCTION_OWNER_COMPONENT_REUSE_MATRIX_EXPECTED_TAGS);
        attributes.put("currentAppProductionOwnerComponentReuseMatrixObservedTags", "");
        attributes.put("currentAppProductionOwnerComponentReuseMatrixReusedTags", "");
        attributes.put("currentAppProductionOwnerComponentReuseMatrixFallbackTags", "");
        attributes.put("currentAppProductionOwnerComponentReuseMatrixSkippedFocusedImports", "");
        attributes.put("currentAppProductionOwnerComponentReuseMatrixFallbackFocusedImports", "");
        attributes.put("currentAppProductionOwnerComponentReuseMatrixOrderingBlockers", "");
        attributes.put("currentAppProductionOwnerComponentReuseMatrixDecision",
                "pending-current-app-owner-component-reuse-matrix");
        attributes.put("currentAppProductionOwnerComponentReuseMatrixReason",
                "covered components must be classified as current app owner reuse or focused fallback");
        attributes.put("currentAppProductionOwnerComponentReuseMatrixFailureLayer",
                handoffReady ? CURRENT_APP_PRODUCTION_OWNER_COMPONENT_REUSE_MATRIX_FAILURE_LAYER
                        : evidence.failureLayer());
        attributes.put("currentAppProductionOwnerComponentReuseMatrixNextResolutionStep",
                CURRENT_APP_PRODUCTION_OWNER_COMPONENT_REUSE_MATRIX_NEXT_STEP);
        attributes.put("currentAppProductionOwnerComponentReuseMatrixComplete", false);
        attributes.put("currentAppProductionOwnerComponentReuseMatrixTextFieldReuseRetained", false);
        attributes.put("currentAppProductionOwnerComponentReuseMatrixEarlyFallbackClassified", false);
        attributes.put("currentAppProductionOwnerComponentReuseMatrixNoLocalhost", true);
        attributes.put("currentAppProductionOwnerEarlyImportVersion",
                CURRENT_APP_PRODUCTION_OWNER_EARLY_IMPORT_VERSION);
        attributes.put("currentAppProductionOwnerEarlyImportSource",
                CURRENT_APP_PRODUCTION_OWNER_EARLY_IMPORT_SOURCE);
        attributes.put("currentAppProductionOwnerEarlyImportPolicy",
                CURRENT_APP_PRODUCTION_OWNER_EARLY_IMPORT_POLICY);
        attributes.put("currentAppProductionOwnerEarlyImportPhase",
                CURRENT_APP_PRODUCTION_OWNER_EARLY_IMPORT_PHASE);
        attributes.put("currentAppProductionOwnerEarlyImportReusableTags",
                CURRENT_APP_PRODUCTION_OWNER_EARLY_IMPORT_REUSABLE_TAGS);
        attributes.put("currentAppProductionOwnerEarlyImportCoveredTags",
                CURRENT_APP_PRODUCTION_OWNER_COMPONENT_REUSE_MATRIX_EXPECTED_TAGS);
        attributes.put("currentAppProductionOwnerEarlyImportReusedTags", "");
        attributes.put("currentAppProductionOwnerEarlyImportSkippedFocusedImports", "");
        attributes.put("currentAppProductionOwnerEarlyImportFallbackTags", "");
        attributes.put("currentAppProductionOwnerEarlyImportFallbackFocusedImports", "");
        attributes.put("currentAppProductionOwnerEarlyImportOrderingBlockers", "");
        attributes.put("currentAppProductionOwnerEarlyImportDecision",
                "pending-current-app-owner-early-import");
        attributes.put("currentAppProductionOwnerEarlyImportReason",
                "current app production owner must be imported before covered component focused imports");
        attributes.put("currentAppProductionOwnerEarlyImportFailureLayer",
                handoffReady ? CURRENT_APP_PRODUCTION_OWNER_EARLY_IMPORT_FAILURE_LAYER : evidence.failureLayer());
        attributes.put("currentAppProductionOwnerEarlyImportNextResolutionStep",
                CURRENT_APP_PRODUCTION_OWNER_EARLY_IMPORT_NEXT_STEP);
        attributes.put("currentAppProductionOwnerEarlyImportRequestBeforeCoveredComponents", false);
        attributes.put("currentAppProductionOwnerEarlyImportAttempted", false);
        attributes.put("currentAppProductionOwnerEarlyImported", false);
        attributes.put("currentAppProductionOwnerEarlyImportSideEffectSatisfied", false);
        attributes.put("currentAppProductionOwnerEarlyImportPartialReuseSatisfied", false);
        attributes.put("currentAppProductionOwnerEarlyImportNoLocalhost", true);
        attributes.put("currentAppProductionOwnerEarlyImportFixtureSubstituted", false);
        attributes.put("currentAppGeneratedImportsOwnerSideEffectVersion",
                CURRENT_APP_GENERATED_IMPORTS_OWNER_SIDE_EFFECT_VERSION);
        attributes.put("currentAppGeneratedImportsOwnerSideEffectSource",
                CURRENT_APP_GENERATED_IMPORTS_OWNER_SIDE_EFFECT_SOURCE);
        attributes.put("currentAppGeneratedImportsOwnerSideEffectPolicy",
                CURRENT_APP_GENERATED_IMPORTS_OWNER_SIDE_EFFECT_POLICY);
        attributes.put("currentAppGeneratedImportsOwnerSideEffectCandidate",
                CURRENT_APP_GENERATED_IMPORTS_OWNER_SIDE_EFFECT_CANDIDATE);
        attributes.put("currentAppGeneratedImportsOwnerSideEffectArtifact",
                evidence.sourceGeneratedImportsRootProductionSupportArtifact());
        attributes.put("currentAppGeneratedImportsOwnerSideEffectExpectedTags",
                CURRENT_APP_GENERATED_IMPORTS_OWNER_SIDE_EFFECT_EXPECTED_TAGS);
        attributes.put("currentAppGeneratedImportsOwnerSideEffectCoveredTags",
                CURRENT_APP_GENERATED_IMPORTS_OWNER_SIDE_EFFECT_COVERED_TAGS);
        attributes.put("currentAppGeneratedImportsOwnerSideEffectKnownMissingTags",
                CURRENT_APP_GENERATED_IMPORTS_OWNER_SIDE_EFFECT_KNOWN_MISSING_TAGS);
        attributes.put("currentAppGeneratedImportsOwnerSideEffectExpectedGlobals",
                CURRENT_APP_GENERATED_IMPORTS_OWNER_SIDE_EFFECT_EXPECTED_GLOBALS);
        attributes.put("currentAppGeneratedImportsOwnerSideEffectPhase",
                CURRENT_APP_PRODUCTION_OWNER_EARLY_IMPORT_PHASE);
        attributes.put("currentAppGeneratedImportsOwnerSideEffectObservedTags", "");
        attributes.put("currentAppGeneratedImportsOwnerSideEffectMissingTags", "");
        attributes.put("currentAppGeneratedImportsOwnerSideEffectReusedTags", "");
        attributes.put("currentAppGeneratedImportsOwnerSideEffectSkippedFocusedImports", "");
        attributes.put("currentAppGeneratedImportsOwnerSideEffectFallbackTags", "");
        attributes.put("currentAppGeneratedImportsOwnerSideEffectFallbackFocusedImports", "");
        attributes.put("currentAppGeneratedImportsOwnerSideEffectDecision",
                "pending-current-app-generated-imports-owner-side-effect");
        attributes.put("currentAppGeneratedImportsOwnerSideEffectReason",
                "current app production generated imports support artifact must be imported before focused component imports");
        attributes.put("currentAppGeneratedImportsOwnerSideEffectFailureLayer",
                handoffReady ? CURRENT_APP_GENERATED_IMPORTS_OWNER_SIDE_EFFECT_FAILURE_LAYER
                        : evidence.failureLayer());
        attributes.put("currentAppGeneratedImportsOwnerSideEffectNextResolutionStep",
                CURRENT_APP_GENERATED_IMPORTS_OWNER_SIDE_EFFECT_NEXT_STEP);
        attributes.put("currentAppGeneratedImportsOwnerSideEffectAttempted", false);
        attributes.put("currentAppGeneratedImportsOwnerSideEffectImported", false);
        attributes.put("currentAppGeneratedImportsOwnerSideEffectSatisfied", false);
        attributes.put("currentAppGeneratedImportsOwnerSideEffectPartialReuseSatisfied", false);
        attributes.put("currentAppGeneratedImportsOwnerSideEffectLoadOnDemandPresent", false);
        attributes.put("currentAppGeneratedImportsOwnerSideEffectNoLocalhost", true);
        attributes.put("currentAppGeneratedImportsOwnerSideEffectFixtureSubstituted", false);
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractVersion",
                CURRENT_APP_GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_VERSION);
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractSource",
                CURRENT_APP_GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_SOURCE);
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractArtifactSource",
                CURRENT_APP_GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_ARTIFACT_SOURCE);
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractSelectedArtifact",
                evidence.sourceGeneratedImportsRootProductionSupportArtifact());
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractSelectedArtifactSha256",
                evidence.productionGeneratedImportsSha256());
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractSelectedArtifactBytes",
                evidence.productionGeneratedImportsBytes());
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractExpectedTags",
                CURRENT_APP_GENERATED_IMPORTS_OWNER_SIDE_EFFECT_EXPECTED_TAGS);
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractCoveredTags",
                CURRENT_APP_GENERATED_IMPORTS_OWNER_SIDE_EFFECT_COVERED_TAGS);
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractOwnerPriority",
                CURRENT_APP_GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_OWNER_PRIORITY);
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractSkipPolicy",
                CURRENT_APP_GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_SKIP_POLICY);
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractFallbackPolicy",
                CURRENT_APP_GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_FALLBACK_POLICY);
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractResolverHandoff",
                CURRENT_APP_GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_RESOLVER_HANDOFF);
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractDecision",
                "pending-current-app-generated-imports-owner-runtime-contract");
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractFailureLayer",
                handoffReady ? CURRENT_APP_GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_FAILURE_LAYER
                        : evidence.failureLayer());
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractNextResolutionStep",
                CURRENT_APP_GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_NEXT_STEP);
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractAttempted", false);
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractImported", false);
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractSatisfied", false);
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractResolverHandoffSatisfied", false);
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractFocusedImportsSkipped", false);
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractNoLocalhost", true);
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractFixtureSubstituted", false);
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractObservedTags", "");
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractReusedTags", "");
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractSkippedFocusedImports", "");
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractFallbackTags", "");
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractBrowserError", "");
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractModuleIdentity", "");
        attributes.put("currentAppGeneratedImportsOwnerLazyChunkBridgeVersion",
                CURRENT_APP_GENERATED_IMPORTS_OWNER_LAZY_CHUNK_BRIDGE_VERSION);
        attributes.put("currentAppGeneratedImportsOwnerLazyChunkBridgeSource",
                CURRENT_APP_GENERATED_IMPORTS_OWNER_LAZY_CHUNK_BRIDGE_SOURCE);
        attributes.put("currentAppGeneratedImportsOwnerLazyChunkBridgeGeneratedImportsTableShape",
                evidence.sourceGeneratedImportsTableShape());
        attributes.put("currentAppGeneratedImportsOwnerLazyChunkBridgeChunkId",
                evidence.sourceChunkId());
        attributes.put("currentAppGeneratedImportsOwnerLazyChunkBridgeChunkArtifact",
                evidence.sourceChunkVirtualPath());
        attributes.put("currentAppGeneratedImportsOwnerLazyChunkBridgeChunkStatus",
                evidence.sourceChunkStatus());
        attributes.put("currentAppGeneratedImportsOwnerLazyChunkBridgeBuildOutputSource",
                evidence.goal());
        attributes.put("currentAppGeneratedImportsOwnerLazyChunkBridgeSupportedSpiDecision",
                CURRENT_APP_GENERATED_IMPORTS_OWNER_LAZY_CHUNK_SPI_DECISION);
        attributes.put("currentAppGeneratedImportsOwnerLazyChunkBridgeNoLocalhost", true);
        attributes.put("currentAppGeneratedImportsOwnerLazyChunkBridgeFixtureSubstituted", false);
        attributes.put("generatedLazySpiDecisionVersion", GENERATED_LAZY_SPI_DECISION_VERSION);
        attributes.put("generatedLazySpiDecisionSource", GENERATED_LAZY_SPI_DECISION_SOURCE);
        attributes.put("generatedLazySpiAuditedSourceSurfaces",
                GENERATED_LAZY_SPI_AUDITED_SOURCE_SURFACES);
        attributes.put("generatedLazySpiChunkInfoApiStatus",
                GENERATED_LAZY_SPI_CHUNK_INFO_API_STATUS);
        attributes.put("generatedLazySpiRegistrySource", GENERATED_LAZY_SPI_REGISTRY_SOURCE);
        attributes.put("generatedLazySpiGeneratedImportsArtifact",
                CURRENT_APP_BUILD_FRONTEND_GENERATED_IMPORTS_JS);
        attributes.put("generatedLazySpiGeneratedImportsSha256",
                evidence.sourceGeneratedImportsSha256());
        attributes.put("generatedLazySpiGeneratedImportsBytes",
                evidence.sourceGeneratedImportsBytes());
        attributes.put("generatedLazySpiGeneratedImportsTableShape",
                evidence.sourceGeneratedImportsTableShape());
        attributes.put("generatedLazySpiChunkId", evidence.sourceChunkId());
        attributes.put("generatedLazySpiChunkImportSpecifier",
                evidence.sourceChunkImportSpecifier());
        attributes.put("generatedLazySpiChunkArtifact", evidence.sourceChunkVirtualPath());
        attributes.put("generatedLazySpiChunkStatus", evidence.sourceChunkStatus());
        attributes.put("generatedLazySpiChunkSha256", evidence.sourceChunkSha256());
        attributes.put("generatedLazySpiChunkBytes", evidence.sourceChunkBytes());
        attributes.put("generatedLazySpiBuildOutputSource", evidence.goal());
        attributes.put("generatedLazySpiProductionSupportArtifact",
                evidence.sourceGeneratedImportsRootProductionSupportArtifact());
        attributes.put("generatedLazySpiStatsPath", evidence.statsPath());
        attributes.put("generatedLazySpiProdBundlePath", evidence.prodBundlePath());
        attributes.put("generatedLazySpiDecision", GENERATED_LAZY_SPI_DECISION);
        attributes.put("generatedLazySpiUnavailableReason",
                GENERATED_LAZY_SPI_UNAVAILABLE_REASON);
        attributes.put("generatedLazySpiFailureLayer", GENERATED_LAZY_SPI_FAILURE_LAYER);
        attributes.put("generatedLazySpiProductPath", GENERATED_LAZY_SPI_PRODUCT_PATH);
        attributes.put("generatedLazySpiExecutionEvidence", GENERATED_LAZY_SPI_EXECUTION_EVIDENCE);
        attributes.put("generatedLazySpiExecutionTrigger", GENERATED_LAZY_SPI_EXECUTION_TRIGGER);
        attributes.put("generatedLazySpiNextResolutionStep", GENERATED_LAZY_SPI_NEXT_STEP);
        attributes.put("generatedLazySpiAvailable", false);
        attributes.put("generatedLazySpiRuntimePathSelected", false);
        attributes.put("generatedLazySpiPackagingTimeStagingSelected", true);
        attributes.put("generatedLazySpiCurrentAppGeneratedImportsObserved",
                evidence.sourceGeneratedImportsObserved());
        attributes.put("generatedLazySpiCurrentAppChunkAvailable",
                "available".equals(evidence.sourceChunkStatus()));
        attributes.put("generatedLazySpiCurrentAppChunkExecuted", false);
        attributes.put("generatedLazySpiProductionSupportObserved",
                evidence.sourceGeneratedImportsRootProductionSupportObserved());
        attributes.put("generatedLazySpiStatsObserved", evidence.statsObserved());
        attributes.put("generatedLazySpiProdBundleObserved", evidence.prodBundleObserved());
        attributes.put("generatedLazySpiNoLocalhost", true);
        attributes.put("generatedLazySpiFixtureSubstituted", false);
        return attributes;
    }

    private static String currentAppProductionRuntimeOwnershipArtifactInventory(
            CurrentAppBuildFrontendOutputEvidence evidence) {
        List<String> inventory = new ArrayList<>();
        addNonBlank(inventory, CURRENT_APP_BUILD_FRONTEND_GENERATED_IMPORTS_JS);
        addNonBlank(inventory, evidence.sourceChunkVirtualPath());
        addNonBlank(inventory, evidence.sourceGeneratedImportsRootProductionSupportArtifact());
        addNonBlank(inventory, evidence.productionEntryScriptVirtualPath());
        inventory.addAll(csvValues(evidence.productionBuildJsVirtualPaths()));
        addNonBlank(inventory, evidence.prodBundlePath());
        return String.join(",", inventory.stream().distinct().toList());
    }

    private static void addNonBlank(List<String> target, String value) {
        if (value != null && !value.isBlank()) {
            target.add(value);
        }
    }

    private static Map<String, Object> currentAppProductionBuildAttributes(
            CurrentAppBuildFrontendOutputEvidence evidence, String virtualPath) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("componentFrontendResource", true);
        attributes.put("currentAppBuildFrontendResource", true);
        attributes.put("currentAppProductionBuildResource", true);
        attributes.put("currentAppProductionBuildResourceRole",
                virtualPath.equals(evidence.sourceGeneratedImportsRootProductionSupportArtifact())
                        ? "generated-imports-root-support"
                        : virtualPath.equals(evidence.productionEntryScriptVirtualPath())
                        ? "production-entry-script"
                        : "production-relative-dependency");
        attributes.put("moduleSpecifier", virtualPath);
        attributes.put("resolver", "current-app-production-webapp-build-v1_34");
        attributes.put("shim", false);
        attributes.put("artifactFamily", "current-app-production-build");
        attributes.put("origin", "current-app-production-webapp-build");
        attributes.put("resourceOrigin", "current-app-production-webapp-build");
        attributes.put("dependencyOf", evidence.sourceGeneratedImportsRootProductionSupportArtifact());
        attributes.put("executionPolicy", "stage-current-app-production-build-support-v1_34");
        attributes.put("artifactStatus", "available");
        attributes.put("requestedBy", "current-app-generated-imports-root-package-graph-v1_34");
        attributes.put("failureClassification", "current-app-production-build-support-resource-v1_34");
        attributes.put("blockedReason",
                "current app production webapp build artifact staged as real support for source generated imports root package graph; remaining blocker is production owner-owned runtime parity, not a diagnostic placeholder");
        attributes.put("lookupKind", "Current-app-production-build-support-v1_34");
        attributes.put("blockerVersion", CURRENT_APP_GENERATED_IMPORTS_ROOT_PACKAGE_GRAPH_VERSION);
        attributes.put("failureLayer", evidence.sourceGeneratedImportsRootFailureLayer());
        attributes.put("nextResolutionStep", CURRENT_APP_GENERATED_IMPORTS_ROOT_NEXT_STEP);
        attributes.put("importRewriteStrategy", "current-app-production-build-relative-import-rewrite-v1_34");
        attributes.put("browserExecutionPlan", "stage-current-app-production-build-module-with-relative-rewrites-v1_34");
        attributes.put("importRewrites", currentAppProductionBuildImportRewrites(evidence, virtualPath));
        attributes.put("transitiveImports", currentAppProductionBuildTransitiveImports(evidence, virtualPath));
        attributes.putAll(currentAppGeneratedImportsRootPackageGraphAttributes(evidence));
        attributes.putAll(currentAppBuildFrontendOutputAttributes());
        return attributes;
    }

    private static List<Map<String, Object>> currentAppProductionBuildImportRewrites(
            CurrentAppBuildFrontendOutputEvidence evidence, String currentPath) {
        List<Map<String, Object>> rewrites = new ArrayList<>();
        for (String path : csvValues(evidence.productionBuildJsVirtualPaths())) {
            if (path.equals(currentPath)) {
                continue;
            }
            if (currentPath.equals(evidence.productionEntryScriptVirtualPath())
                    && path.equals(evidence.sourceGeneratedImportsRootProductionSupportArtifact())) {
                continue;
            }
            rewrites.add(importRewrite("./" + fileName(path),
                    path,
                    "current-app-production-build",
                    "current-app-production-build-relative-import-rewrite-v1_34",
                    "current-app-production-build-relative-dependency-v1_34"));
        }
        return rewrites;
    }

    private static List<String> currentAppProductionBuildTransitiveImports(
            CurrentAppBuildFrontendOutputEvidence evidence, String currentPath) {
        List<String> imports = new ArrayList<>();
        for (String path : csvValues(evidence.productionBuildJsVirtualPaths())) {
            if (!path.equals(currentPath)) {
                imports.add(path);
            }
        }
        return imports;
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
            attributes.putAll(generatedLazySourceBoundaryAttributes());
            attributes.putAll(generatedLazyRealBuildProbeAttributes());
            attributes.putAll(generatedLazyBuildLifecycleAttributes());
            attributes.putAll(generatedLazyAppBuildOutputAttributes());
            attributes.putAll(currentAppFrontendBuildOutputAttributes());
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

    private static Map<String, Object> generatedLazySourceBoundaryAttributes() {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("generatedLazySourceBoundaryVersion", GENERATED_LAZY_SOURCE_BOUNDARY_VERSION);
        attributes.put("generatedLazySourceMechanism", GENERATED_LAZY_SOURCE_MECHANISM);
        attributes.put("generatedLazySourceArtifact", GENERATED_LAZY_SOURCE_ARTIFACT);
        attributes.put("generatedLazySourceScanner", GENERATED_LAZY_SOURCE_SCANNER);
        attributes.put("generatedLazySourceLazyRouteRule", GENERATED_LAZY_SOURCE_LAZY_ROUTE_RULE);
        attributes.put("generatedLazySourceChunkTriggerInput", GENERATED_LAZY_SOURCE_CHUNK_TRIGGER_INPUT);
        attributes.put("generatedLazySourcePositiveTableShape", GENERATED_LAZY_SOURCE_POSITIVE_TABLE_SHAPE);
        attributes.put("generatedLazySourcePositiveImportPattern", GENERATED_LAZY_SOURCE_POSITIVE_IMPORT_PATTERN);
        attributes.put("generatedLazySourceNoChunkFallback", GENERATED_LAZY_SOURCE_NO_CHUNK_FALLBACK);
        attributes.put("generatedLazySourceCurrentBuildArtifact", DEV_BUNDLE_GENERATED_FLOW_IMPORTS_JS);
        attributes.put("generatedLazySourceCurrentBuildStatus", GENERATED_LAZY_SOURCE_CURRENT_BUILD_STATUS);
        attributes.put("generatedLazySourceCurrentBuildTableShape", "no-op-promise-resolve-0");
        attributes.put("generatedLazySourceCurrentBuildChunkStatus", "not-generated");
        attributes.put("generatedLazySourceCurrentBuildNoOpConfirmed", true);
        attributes.put("generatedLazySourceCurrentBuildChunkArtifact", "");
        attributes.put("generatedLazySourceBuildInputStatus", GENERATED_LAZY_SOURCE_BUILD_INPUT_STATUS);
        attributes.put("generatedLazySourceSupportedBuildPath", GENERATED_LAZY_SOURCE_SUPPORTED_BUILD_PATH);
        attributes.put("generatedLazySourceDecision", GENERATED_LAZY_SOURCE_DECISION);
        attributes.put("generatedLazySourceRemainingBlocker", GENERATED_LAZY_SOURCE_REMAINING_BLOCKER);
        attributes.put("generatedLazySourceNextResolutionStep", GENERATED_LAZY_SOURCE_NEXT_STEP);
        attributes.put("generatedLazySourceNoLocalhost", true);
        return attributes;
    }

    private static Map<String, Object> generatedLazyRealBuildProbeAttributes() {
        Properties properties = generatedLazyRealBuildProbeProperties();
        boolean probeManifestObserved = !properties.isEmpty();
        String routeClass = propertyOrDefault(properties, "routeClass",
                GENERATED_LAZY_REAL_BUILD_DEFAULT_ROUTE_CLASS);
        String expectedRoute = propertyOrDefault(properties, "route",
                GENERATED_LAZY_REAL_BUILD_DEFAULT_ROUTE);
        String expectedTrigger = propertyOrDefault(properties, "trigger",
                GENERATED_LAZY_REAL_BUILD_DEFAULT_TRIGGER);
        String expectedDependency = propertyOrDefault(properties, "dependency",
                GENERATED_LAZY_REAL_BUILD_DEFAULT_DEPENDENCY);

        Class<?> probeClass = loadOptionalClass(routeClass);
        Route route = probeClass == null ? null : probeClass.getAnnotation(Route.class);
        JavaScript[] scripts = probeClass == null ? new JavaScript[0]
                : probeClass.getAnnotationsByType(JavaScript.class);
        boolean routeObserved = route != null;
        boolean routeMatches = routeObserved && expectedRoute.equals(route.value());
        boolean lazyJavascriptObserved = false;
        for (JavaScript script : scripts) {
            if ("LAZY".equals(script.loadMode().name())
                    && expectedDependency.equals(script.value())) {
                lazyJavascriptObserved = true;
                break;
            }
        }
        boolean scannerObserved = routeMatches && lazyJavascriptObserved;

        GeneratedLoadOnDemandChunkFileRegistry registry =
                GeneratedLoadOnDemandChunkFileRegistry.parse(DEV_BUNDLE_GENERATED_FLOW_IMPORTS_JS,
                        classpathResourceText(DEV_BUNDLE_GENERATED_FLOW_IMPORTS_JS));
        boolean branchTable = "branch-table-dynamic-import".equals(registry.loadOnDemandTableShape());
        String failureLayer;
        if (branchTable) {
            failureLayer = "pending-real-generated-chunk-stage";
        } else if (!probeManifestObserved) {
            failureLayer = "lazy-route-probe-metadata-not-on-classpath";
        } else if (!routeObserved) {
            failureLayer = "lazy-route-input-class-not-on-classpath";
        } else if (!lazyJavascriptObserved) {
            failureLayer = "lazy-route-input-no-lazy-javascript";
        } else {
            failureLayer = GENERATED_LAZY_REAL_BUILD_FAILURE_LAYER;
        }

        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("generatedLazyRealBuildProbeVersion", GENERATED_LAZY_REAL_BUILD_PROBE_VERSION);
        attributes.put("generatedLazyRealBuildProbeManifest", GENERATED_LAZY_REAL_BUILD_PROBE_PROPERTIES);
        attributes.put("generatedLazyRealBuildProbeManifestObserved", probeManifestObserved);
        attributes.put("generatedLazyRealBuildInputRoute", expectedRoute);
        attributes.put("generatedLazyRealBuildInputRouteClass", routeClass);
        attributes.put("generatedLazyRealBuildInputTrigger", expectedTrigger);
        attributes.put("generatedLazyRealBuildInputDependency", expectedDependency);
        attributes.put("generatedLazyRealBuildInputEager", false);
        attributes.put("generatedLazyRealBuildInputRouteObserved", routeObserved);
        attributes.put("generatedLazyRealBuildInputRouteMatches", routeMatches);
        attributes.put("generatedLazyRealBuildScannerSource", GENERATED_LAZY_REAL_BUILD_SCANNER_SOURCE);
        attributes.put("generatedLazyRealBuildScannerObserved", scannerObserved);
        attributes.put("generatedLazyRealBuildLazyJavascriptObserved", lazyJavascriptObserved);
        attributes.put("generatedLazyRealBuildGeneratedImportsArtifact", DEV_BUNDLE_GENERATED_FLOW_IMPORTS_JS);
        attributes.put("generatedLazyRealBuildGeneratedImportsTableShape", registry.loadOnDemandTableShape());
        attributes.put("generatedLazyRealBuildNoOpLoadOnDemand", registry.noOpLoadOnDemand());
        attributes.put("generatedLazyRealBuildChunkStatus", registry.chunkFileStatus());
        attributes.put("generatedLazyRealBuildChunkArtifact", registry.chunkArtifactPath());
        attributes.put("generatedLazyRealBuildFailureLayer", failureLayer);
        attributes.put("generatedLazyRealBuildNextResolutionStep", GENERATED_LAZY_REAL_BUILD_NEXT_STEP);
        attributes.put("generatedLazyRealBuildFixtureSubstituted", false);
        attributes.put("generatedLazyRealBuildNoLocalhost", true);
        return attributes;
    }

    private static Properties generatedLazyRealBuildProbeProperties() {
        Properties properties = new Properties();
        String text = classpathResourceText(GENERATED_LAZY_REAL_BUILD_PROBE_PROPERTIES);
        if (text.isBlank()) {
            return properties;
        }
        try {
            properties.load(new java.io.ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
            return properties;
        } catch (IOException error) {
            throw new IllegalStateException("Unable to read generated lazy real build probe metadata", error);
        }
    }

    private static Map<String, Object> generatedLazyBuildLifecycleAttributes() {
        Properties properties = generatedLazyRealBuildProbeProperties();
        boolean probeManifestObserved = !properties.isEmpty();
        String routeClass = propertyOrDefault(properties, "routeClass",
                GENERATED_LAZY_REAL_BUILD_DEFAULT_ROUTE_CLASS);
        String expectedRoute = propertyOrDefault(properties, "route",
                GENERATED_LAZY_REAL_BUILD_DEFAULT_ROUTE);
        String expectedDependency = propertyOrDefault(properties, "dependency",
                GENERATED_LAZY_REAL_BUILD_DEFAULT_DEPENDENCY);

        Class<?> probeClass = loadOptionalClass(routeClass);
        Route route = probeClass == null ? null : probeClass.getAnnotation(Route.class);
        JavaScript[] scripts = probeClass == null ? new JavaScript[0]
                : probeClass.getAnnotationsByType(JavaScript.class);
        boolean routeObserved = route != null;
        boolean routeMatches = routeObserved && expectedRoute.equals(route.value());
        boolean lazyJavascriptObserved = false;
        for (JavaScript script : scripts) {
            if ("LAZY".equals(script.loadMode().name())
                    && expectedDependency.equals(script.value())) {
                lazyJavascriptObserved = true;
                break;
            }
        }
        boolean scannerObserved = probeManifestObserved && routeMatches && lazyJavascriptObserved;

        byte[] generatedImportsBytes = classpathResourceBytes(DEV_BUNDLE_GENERATED_FLOW_IMPORTS_JS);
        String generatedImportsBody = new String(generatedImportsBytes, StandardCharsets.UTF_8);
        GeneratedLoadOnDemandChunkFileRegistry registry =
                GeneratedLoadOnDemandChunkFileRegistry.parse(DEV_BUNDLE_GENERATED_FLOW_IMPORTS_JS,
                        generatedImportsBody);
        ClasspathArtifactEvidence generatedImportsEvidence =
                classpathArtifactEvidence(DEV_BUNDLE_GENERATED_FLOW_IMPORTS_JS);
        WriteTargetEvidence writeTarget =
                writeTargetEvidence(DEV_BUNDLE_GENERATED_FLOW_IMPORTS_JS);
        boolean branchTable = "branch-table-dynamic-import".equals(registry.loadOnDemandTableShape());
        boolean updateImportsObserved = writeTarget.observed()
                && "current-build".equals(generatedImportsEvidence.source());

        String failureLayer;
        if (!probeManifestObserved) {
            failureLayer = "lazy-route-probe-metadata-not-on-classpath";
        } else if (!routeObserved) {
            failureLayer = "lazy-route-not-in-route-registry";
        } else if (!lazyJavascriptObserved) {
            failureLayer = "lazy-route-input-no-lazy-javascript";
        } else if ("static-dev-bundle".equals(generatedImportsEvidence.source())) {
            failureLayer = GENERATED_LAZY_BUILD_LIFECYCLE_FAILURE_LAYER;
        } else if (!updateImportsObserved) {
            failureLayer = "update-imports-not-run-for-ipc-runtime";
        } else if (!branchTable) {
            failureLayer = "vaadin-build-does-not-emit-chunk-for-loadmode-lazy-route-probe";
        } else {
            failureLayer = "pending-real-generated-chunk-stage";
        }

        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("generatedLazyBuildLifecycleVersion", GENERATED_LAZY_BUILD_LIFECYCLE_VERSION);
        attributes.put("generatedLazyBuildLifecycleInputRoute", expectedRoute);
        attributes.put("generatedLazyBuildLifecycleInputRouteClass", routeClass);
        attributes.put("generatedLazyBuildLifecycleInputDependency", expectedDependency);
        attributes.put("generatedLazyBuildLifecycleInputEager", false);
        attributes.put("generatedLazyBuildLifecycleScannerSource",
                GENERATED_LAZY_BUILD_LIFECYCLE_SCANNER_SOURCE);
        attributes.put("generatedLazyBuildLifecycleScannerStatus",
                scannerObserved ? "observed" : "missing");
        attributes.put("generatedLazyBuildLifecycleScannerObserved", scannerObserved);
        attributes.put("generatedLazyBuildLifecycleLazyJavascriptObserved", lazyJavascriptObserved);
        attributes.put("generatedLazyBuildLifecycleLazyRouteChunkInput",
                "ChunkInfo(type=ROUTE,eager=false,trigger=" + routeClass + ",scripts=1)");
        attributes.put("generatedLazyBuildLifecycleBuildTaskSource",
                GENERATED_LAZY_BUILD_LIFECYCLE_BUILD_TASK_SOURCE);
        attributes.put("generatedLazyBuildLifecycleUpdateImportsObserved", updateImportsObserved);
        attributes.put("generatedLazyBuildLifecycleGeneratedImportsSource",
                generatedImportsEvidence.source());
        attributes.put("generatedLazyBuildLifecycleGeneratedImportsRuntimePath",
                generatedImportsEvidence.runtimePath());
        attributes.put("generatedLazyBuildLifecycleGeneratedImportsProtocol",
                generatedImportsEvidence.protocol());
        attributes.put("generatedLazyBuildLifecycleGeneratedImportsMtimeMillis",
                generatedImportsEvidence.lastModifiedMillis());
        attributes.put("generatedLazyBuildLifecycleGeneratedImportsSha256",
                Resource.sha256(generatedImportsBytes));
        attributes.put("generatedLazyBuildLifecycleGeneratedImportsBytes",
                generatedImportsBytes.length);
        attributes.put("generatedLazyBuildLifecycleGeneratedImportsTableShape",
                registry.loadOnDemandTableShape());
        attributes.put("generatedLazyBuildLifecycleGeneratedImportsNoOpLoadOnDemand",
                registry.noOpLoadOnDemand());
        attributes.put("generatedLazyBuildLifecycleWriteTarget", writeTarget.path());
        attributes.put("generatedLazyBuildLifecycleWriteTargetObserved", writeTarget.observed());
        attributes.put("generatedLazyBuildLifecycleWriteTargetMtimeMillis",
                writeTarget.lastModifiedMillis());
        attributes.put("generatedLazyBuildLifecycleRuntimeUsesStaticDevBundle",
                "static-dev-bundle".equals(generatedImportsEvidence.source()));
        attributes.put("generatedLazyBuildLifecycleRuntimeProbeMismatch",
                scannerObserved && "static-dev-bundle".equals(generatedImportsEvidence.source()));
        attributes.put("generatedLazyBuildLifecycleChunkStatus", registry.chunkFileStatus());
        attributes.put("generatedLazyBuildLifecycleChunkArtifact", registry.chunkArtifactPath());
        attributes.put("generatedLazyBuildLifecycleFailureLayer", failureLayer);
        attributes.put("generatedLazyBuildLifecycleNextResolutionStep",
                GENERATED_LAZY_BUILD_LIFECYCLE_NEXT_STEP);
        attributes.put("generatedLazyBuildLifecycleFixtureSubstituted", false);
        attributes.put("generatedLazyBuildLifecycleNoLocalhost", true);
        return attributes;
    }

    private static Map<String, Object> generatedLazyAppBuildOutputAttributes() {
        Properties properties = generatedLazyRealBuildProbeProperties();
        boolean probeManifestObserved = !properties.isEmpty();
        String routeClass = propertyOrDefault(properties, "routeClass",
                GENERATED_LAZY_REAL_BUILD_DEFAULT_ROUTE_CLASS);
        String expectedRoute = propertyOrDefault(properties, "route",
                GENERATED_LAZY_REAL_BUILD_DEFAULT_ROUTE);
        String expectedDependency = propertyOrDefault(properties, "dependency",
                GENERATED_LAZY_REAL_BUILD_DEFAULT_DEPENDENCY);

        Class<?> probeClass = loadOptionalClass(routeClass);
        Route route = probeClass == null ? null : probeClass.getAnnotation(Route.class);
        JavaScript[] scripts = probeClass == null ? new JavaScript[0]
                : probeClass.getAnnotationsByType(JavaScript.class);
        boolean routeObserved = route != null && expectedRoute.equals(route.value());
        boolean lazyJavascriptObserved = false;
        for (JavaScript script : scripts) {
            if ("LAZY".equals(script.loadMode().name())
                    && expectedDependency.equals(script.value())) {
                lazyJavascriptObserved = true;
                break;
            }
        }
        boolean scannerObserved = routeObserved && lazyJavascriptObserved;

        AppBuildGeneratedImportsEvidence appOutput = appBuildGeneratedImportsEvidence();
        ClasspathArtifactEvidence fallbackEvidence =
                classpathArtifactEvidence(DEV_BUNDLE_GENERATED_FLOW_IMPORTS_JS);
        boolean usesFallback = !appOutput.observed()
                && "static-dev-bundle".equals(fallbackEvidence.source());
        String runtimeSource = appOutput.observed()
                ? "current-app-build-output"
                : usesFallback ? "packaged-dev-bundle-fallback" : "missing-build-output";

        String failureLayer;
        if (!probeManifestObserved) {
            failureLayer = "lazy-route-probe-metadata-not-on-classpath";
        } else if (!scannerObserved) {
            failureLayer = "lazy-route-input-not-observed-by-scanner";
        } else if (!appOutput.observed()) {
            failureLayer = GENERATED_LAZY_APP_BUILD_OUTPUT_FAILURE_LAYER;
        } else if ("available".equals(appOutput.chunkStatus())) {
            failureLayer = "pending-real-generated-chunk-stage";
        } else if ("branch-table-dynamic-import".equals(appOutput.tableShape())) {
            failureLayer = "app-build-output-generated-chunk-file-missing";
        } else {
            failureLayer = "app-build-output-has-no-generated-lazy-chunk";
        }

        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("generatedLazyAppBuildOutputVersion", GENERATED_LAZY_APP_BUILD_OUTPUT_VERSION);
        attributes.put("generatedLazyAppBuildOutputDiscoverySource",
                GENERATED_LAZY_APP_BUILD_OUTPUT_DISCOVERY_SOURCE);
        attributes.put("generatedLazyAppBuildOutputInputRoute", expectedRoute);
        attributes.put("generatedLazyAppBuildOutputInputRouteClass", routeClass);
        attributes.put("generatedLazyAppBuildOutputInputDependency", expectedDependency);
        attributes.put("generatedLazyAppBuildOutputScannerObserved", scannerObserved);
        attributes.put("generatedLazyAppBuildOutputRuntimeSource", runtimeSource);
        attributes.put("generatedLazyAppBuildOutputModulePath", appOutput.modulePath());
        attributes.put("generatedLazyAppBuildOutputGeneratedImportsPath", appOutput.path());
        attributes.put("generatedLazyAppBuildOutputGeneratedImportsObserved", appOutput.observed());
        attributes.put("generatedLazyAppBuildOutputGeneratedImportsMtimeMillis",
                appOutput.lastModifiedMillis());
        attributes.put("generatedLazyAppBuildOutputGeneratedImportsBytes", appOutput.bytes());
        attributes.put("generatedLazyAppBuildOutputGeneratedImportsSha256", appOutput.sha256());
        attributes.put("generatedLazyAppBuildOutputGeneratedImportsTableShape", appOutput.tableShape());
        attributes.put("generatedLazyAppBuildOutputGeneratedImportsNoOpLoadOnDemand",
                appOutput.noOpLoadOnDemand());
        attributes.put("generatedLazyAppBuildOutputChunkReferences", appOutput.chunkReferences());
        attributes.put("generatedLazyAppBuildOutputChunkStatus", appOutput.chunkStatus());
        attributes.put("generatedLazyAppBuildOutputChunkArtifact", appOutput.chunkArtifact());
        attributes.put("generatedLazyAppBuildOutputFallbackReason",
                usesFallback ? "current-app-build-output-generated-imports-missing" : "");
        attributes.put("generatedLazyAppBuildOutputPackagedFallbackPath",
                fallbackEvidence.runtimePath());
        attributes.put("generatedLazyAppBuildOutputSpiAssessment",
                appOutput.observed() ? "requires-chunk-output-or-supported-spi" : "requires-build-step");
        attributes.put("generatedLazyAppBuildOutputFailureLayer", failureLayer);
        attributes.put("generatedLazyAppBuildOutputNextResolutionStep",
                GENERATED_LAZY_APP_BUILD_OUTPUT_NEXT_STEP);
        attributes.put("generatedLazyAppBuildOutputFixtureSubstituted", false);
        attributes.put("generatedLazyAppBuildOutputNoLocalhost", true);
        return attributes;
    }

    private static Map<String, Object> currentAppFrontendBuildOutputAttributes() {
        AppBuildGeneratedImportsEvidence appOutput = appBuildGeneratedImportsEvidence();
        FrontendBuildStepEvidence buildStep = frontendBuildStepEvidence();
        Path module = appModuleDirectory();
        List<Map<String, Object>> inventory = currentAppFrontendBuildOutputInventory(module);
        String outputRoot = firstObservedInventoryPath(inventory);
        if (outputRoot.isBlank()) {
            outputRoot = redactLocation(module.resolve("target").normalize().toString());
        }

        boolean buildStepAttempted = "success".equals(buildStep.status())
                || "failed".equals(buildStep.status())
                || "unavailable".equals(buildStep.status());
        String runtimeSource = frontendBuildRuntimeSource(buildStep, appOutput);

        String failureLayer;
        if (!buildStepAttempted) {
            failureLayer = "current-app-frontend-build-step-not-enabled";
        } else if ("unavailable".equals(buildStep.status())) {
            failureLayer = "vaadin-frontend-build-step-unavailable";
        } else if (!appOutput.observed()) {
            failureLayer = "vaadin-frontend-build-step-output-incomplete";
        } else if ("available".equals(appOutput.chunkStatus())) {
            failureLayer = "pending-real-generated-chunk-stage";
        } else if ("branch-table-dynamic-import".equals(appOutput.tableShape())) {
            failureLayer = "current-app-build-output-generated-chunk-file-missing";
        } else {
            failureLayer = "current-app-build-output-has-no-generated-lazy-chunk";
        }

        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("currentAppFrontendBuildOutputVersion", CURRENT_APP_FRONTEND_BUILD_OUTPUT_VERSION);
        attributes.put("currentAppFrontendBuildOutputDiscoverySource",
                CURRENT_APP_FRONTEND_BUILD_DISCOVERY_SOURCE);
        attributes.put("currentAppFrontendBuildStepCommand", buildStep.command());
        attributes.put("currentAppFrontendBuildStepExitCode", buildStep.exitCode());
        attributes.put("currentAppFrontendBuildStepStatus", buildStep.status());
        attributes.put("currentAppFrontendBuildStepDurationMillis", buildStep.durationMillis());
        attributes.put("currentAppFrontendBuildStepSummary", buildStep.summary());
        attributes.put("currentAppFrontendBuildStepStdoutLog", buildStep.stdoutLog());
        attributes.put("currentAppFrontendBuildStepStderrLog", buildStep.stderrLog());
        attributes.put("currentAppFrontendBuildModulePath", appOutput.modulePath());
        attributes.put("currentAppFrontendBuildOutputRoots", buildStep.outputRoots());
        attributes.put("currentAppFrontendBuildOutputRoot", outputRoot);
        attributes.put("currentAppFrontendBuildOutputInventory", inventory);
        attributes.put("currentAppFrontendBuildOutputRuntimeSource", runtimeSource);
        attributes.put("currentAppFrontendBuildGeneratedImportsPath", appOutput.path());
        attributes.put("currentAppFrontendBuildGeneratedImportsObserved", appOutput.observed());
        attributes.put("currentAppFrontendBuildGeneratedImportsMtimeMillis",
                appOutput.lastModifiedMillis());
        attributes.put("currentAppFrontendBuildGeneratedImportsBytes", appOutput.bytes());
        attributes.put("currentAppFrontendBuildGeneratedImportsSha256", appOutput.sha256());
        attributes.put("currentAppFrontendBuildGeneratedImportsTableShape", appOutput.tableShape());
        attributes.put("currentAppFrontendBuildGeneratedImportsNoOpLoadOnDemand",
                appOutput.noOpLoadOnDemand());
        attributes.put("currentAppFrontendBuildChunkReferences", appOutput.chunkReferences());
        attributes.put("currentAppFrontendBuildChunkStatus", appOutput.chunkStatus());
        attributes.put("currentAppFrontendBuildChunkArtifact", appOutput.chunkArtifact());
        attributes.put("currentAppFrontendBuildFailureLayer", failureLayer);
        attributes.put("currentAppFrontendBuildNextResolutionStep", CURRENT_APP_FRONTEND_BUILD_NEXT_STEP);
        attributes.put("currentAppFrontendBuildFixtureSubstituted", false);
        attributes.put("currentAppFrontendBuildNoLocalhost", true);
        attributes.putAll(frontendBuildPreflightAvailabilityAttributes(buildStep, appOutput, runtimeSource));
        attributes.putAll(currentAppBuildFrontendOutputAttributes());
        return attributes;
    }

    private static Map<String, Object> currentAppBuildFrontendOutputAttributes() {
        FrontendBuildStepEvidence buildStep = frontendBuildStepEvidence();
        CurrentAppBuildFrontendOutputEvidence evidence = currentAppBuildFrontendOutputEvidence();
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("currentAppBuildFrontendOutputVersion", CURRENT_APP_BUILD_FRONTEND_OUTPUT_VERSION);
        attributes.put("currentAppBuildFrontendOutputSource", CURRENT_APP_BUILD_FRONTEND_OUTPUT_SOURCE);
        attributes.put("currentAppBuildFrontendGoal", evidence.goal());
        attributes.put("currentAppBuildFrontendProductionMode", buildStep.productionMode());
        attributes.put("currentAppBuildFrontendForceProductionBuild", buildStep.forceProductionBuild());
        attributes.put("currentAppBuildFrontendIgnoreVersionChecks", buildStep.ignoreVersionChecks());
        attributes.put("currentAppBuildFrontendOutputUsability", buildStep.outputUsability());
        attributes.put("currentAppBuildFrontendSourceGeneratedImportsObserved",
                evidence.sourceGeneratedImportsObserved());
        attributes.put("currentAppBuildFrontendSourceGeneratedImportsPath",
                evidence.sourceGeneratedImportsPath());
        attributes.put("currentAppBuildFrontendSourceGeneratedImportsMtimeMillis",
                evidence.sourceGeneratedImportsMtimeMillis());
        attributes.put("currentAppBuildFrontendSourceGeneratedImportsBytes",
                evidence.sourceGeneratedImportsBytes());
        attributes.put("currentAppBuildFrontendSourceGeneratedImportsSha256",
                evidence.sourceGeneratedImportsSha256());
        attributes.put("currentAppBuildFrontendSourceGeneratedImportsTableShape",
                evidence.sourceGeneratedImportsTableShape());
        attributes.put("currentAppBuildFrontendSourceChunkId", evidence.sourceChunkId());
        attributes.put("currentAppBuildFrontendSourceChunkImportSpecifier",
                evidence.sourceChunkImportSpecifier());
        attributes.put("currentAppBuildFrontendSourceChunkArtifact", evidence.sourceChunkArtifact());
        attributes.put("currentAppBuildFrontendSourceChunkVirtualPath", evidence.sourceChunkVirtualPath());
        attributes.put("currentAppBuildFrontendSourceChunkStatus", evidence.sourceChunkStatus());
        attributes.put("currentAppBuildFrontendSourceChunkBytes", evidence.sourceChunkBytes());
        attributes.put("currentAppBuildFrontendSourceChunkSha256", evidence.sourceChunkSha256());
        attributes.put("currentAppBuildFrontendSourceChunkContainsBareImports",
                evidence.sourceChunkContainsBareImports());
        attributes.put("currentAppGeneratedChunkPackageResolverVersion",
                CURRENT_APP_GENERATED_CHUNK_PACKAGE_RESOLVER_VERSION);
        attributes.put("currentAppGeneratedChunkPackageResolverSource",
                CURRENT_APP_GENERATED_CHUNK_PACKAGE_RESOLVER_SOURCE);
        attributes.put("currentAppGeneratedChunkPackageResolverMode", evidence.sourceChunkPackageResolverMode());
        attributes.put("currentAppGeneratedChunkPackageBareSpecifiers",
                evidence.sourceChunkBareImportSpecifiers());
        attributes.put("currentAppGeneratedChunkPackageResolvedSpecifiers",
                evidence.sourceChunkPackageResolvedSpecifiers());
        attributes.put("currentAppGeneratedChunkPackageUnresolvedSpecifiers",
                evidence.sourceChunkPackageUnresolvedSpecifiers());
        attributes.put("currentAppGeneratedChunkPackageFailureLayer", evidence.failureLayer());
        attributes.put("currentAppGeneratedChunkPackageNextResolutionStep",
                CURRENT_APP_GENERATED_CHUNK_PACKAGE_NEXT_STEP);
        attributes.putAll(currentAppGeneratedImportsRootPackageGraphAttributes(evidence));
        attributes.put("currentAppBuildFrontendProductionBuildObserved",
                evidence.productionBuildObserved());
        attributes.put("currentAppBuildFrontendProductionBuildRoot", evidence.productionBuildRoot());
        attributes.put("currentAppBuildFrontendProductionGeneratedImportsObserved",
                evidence.productionGeneratedImportsObserved());
        attributes.put("currentAppBuildFrontendProductionGeneratedImportsPath",
                evidence.productionGeneratedImportsPath());
        attributes.put("currentAppBuildFrontendProductionGeneratedImportsBytes",
                evidence.productionGeneratedImportsBytes());
        attributes.put("currentAppBuildFrontendProductionGeneratedImportsSha256",
                evidence.productionGeneratedImportsSha256());
        attributes.put("currentAppBuildFrontendStatsObserved", evidence.statsObserved());
        attributes.put("currentAppBuildFrontendStatsPath", evidence.statsPath());
        attributes.put("currentAppBuildFrontendProdBundleObserved", evidence.prodBundleObserved());
        attributes.put("currentAppBuildFrontendProdBundlePath", evidence.prodBundlePath());
        attributes.put("currentAppBuildFrontendProdBundleBytes", evidence.prodBundleBytes());
        attributes.put("currentAppBuildFrontendProdBundleSha256", evidence.prodBundleSha256());
        attributes.put("currentAppBuildFrontendRuntimeCandidate", evidence.runtimeCandidate());
        attributes.put("currentAppBuildFrontendFailureLayer", evidence.failureLayer());
        attributes.put("currentAppBuildFrontendNextResolutionStep", CURRENT_APP_BUILD_FRONTEND_NEXT_STEP);
        attributes.put("currentAppBuildFrontendNoLocalhost", true);
        attributes.put("currentAppBuildFrontendFixtureSubstituted", false);
        attributes.putAll(currentAppProductionRuntimeOwnershipAttributes(evidence));
        return attributes;
    }

    private static String frontendBuildRuntimeSource(
            FrontendBuildStepEvidence buildStep,
            AppBuildGeneratedImportsEvidence appOutput) {
        boolean buildStepAttempted = "success".equals(buildStep.status())
                || "failed".equals(buildStep.status())
                || "unavailable".equals(buildStep.status());
        if (appOutput.observed()) {
            return "current-app-build-output";
        }
        if ("unavailable".equals(buildStep.status())) {
            return "build-step-unavailable";
        }
        if (buildStepAttempted) {
            return "current-app-build-output-incomplete";
        }
        return "packaged-dev-bundle-fallback";
    }

    private static Map<String, Object> frontendBuildPreflightAvailabilityAttributes(
            FrontendBuildStepEvidence buildStep,
            AppBuildGeneratedImportsEvidence appOutput,
            String runtimeSource) {
        String failureLayer = frontendBuildPreflightFailureLayer(buildStep, appOutput);
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("frontendBuildPreflightAvailabilityVersion",
                FRONTEND_BUILD_PREFLIGHT_AVAILABILITY_VERSION);
        attributes.put("frontendBuildPreflightAvailabilitySource",
                FRONTEND_BUILD_PREFLIGHT_AVAILABILITY_SOURCE);
        attributes.put("frontendBuildPreflightAvailabilityMode", buildStep.availabilityMode());
        attributes.put("frontendBuildPreflightEffectiveMavenMode", buildStep.effectiveMavenMode());
        attributes.put("frontendBuildPreflightPluginCoordinates", buildStep.pluginCoordinates());
        attributes.put("frontendBuildPreflightPluginResolutionStatus",
                buildStep.pluginResolutionStatus());
        attributes.put("frontendBuildPreflightGoalResult", buildStep.goalResult());
        attributes.put("frontendBuildPreflightOutputUsability", buildStep.outputUsability());
        attributes.put("frontendBuildPreflightRuntimeSourceDecision", runtimeSource);
        attributes.put("frontendBuildPreflightBuildStepStatus", buildStep.status());
        attributes.put("frontendBuildPreflightFailureLayer", failureLayer);
        attributes.put("frontendBuildPreflightNextResolutionStep", FRONTEND_BUILD_PREFLIGHT_NEXT_STEP);
        attributes.put("frontendBuildPreflightBuildStepUnavailableCleared",
                !"unavailable".equals(buildStep.status())
                        && !"missing".equals(buildStep.pluginResolutionStatus())
                        && !"plugin-unavailable".equals(buildStep.goalResult()));
        attributes.put("frontendBuildPreflightNoLocalhost", true);
        return attributes;
    }

    private static String frontendBuildPreflightFailureLayer(
            FrontendBuildStepEvidence buildStep,
            AppBuildGeneratedImportsEvidence appOutput) {
        if ("skipped".equals(buildStep.status())) {
            return "frontend-build-preflight-not-enabled";
        }
        if ("unavailable".equals(buildStep.status())
                || "missing".equals(buildStep.pluginResolutionStatus())
                || "plugin-unavailable".equals(buildStep.goalResult())) {
            return "frontend-build-plugin-unavailable";
        }
        if ("failed".equals(buildStep.status()) || "failed".equals(buildStep.goalResult())) {
            return "frontend-build-goal-failed-before-output";
        }
        if (!"success".equals(buildStep.status())) {
            return "frontend-build-preflight-unknown";
        }
        if (!appOutput.observed()) {
            return "frontend-build-plugin-resolved-output-incomplete";
        }
        if ("available".equals(appOutput.chunkStatus())) {
            return "current-app-build-output-generated-chunk-available-pending-stage";
        }
        if ("branch-table-dynamic-import".equals(appOutput.tableShape())) {
            return "current-app-build-output-generated-chunk-file-missing";
        }
        return "current-app-build-output-has-no-generated-lazy-chunk";
    }

    private static FrontendBuildStepEvidence frontendBuildStepEvidence() {
        String enabled = System.getenv("VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_V1_30");
        String enabledV131 = System.getenv("VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_V1_31");
        String enabledV132 = System.getenv("VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_V1_32");
        String status = System.getenv("VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_STEP_STATUS");
        if ((enabled == null || enabled.isBlank())
                && (enabledV131 == null || enabledV131.isBlank())
                && (enabledV132 == null || enabledV132.isBlank())
                || status == null || status.isBlank()) {
            status = "skipped";
        }
        return new FrontendBuildStepEvidence(
                compactText(System.getenv("VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_STEP_COMMAND")),
                parseInt(System.getenv("VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_STEP_EXIT_CODE"), -1),
                status,
                parseLong(System.getenv("VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_STEP_DURATION_MILLIS"), 0L),
                compactText(System.getenv("VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_OUTPUT_ROOTS")),
                compactText(System.getenv("VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_STEP_SUMMARY")),
                compactText(System.getenv("VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_STDOUT_LOG")),
                compactText(System.getenv("VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_STDERR_LOG")),
                compactText(System.getenv("VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_AVAILABILITY_MODE"),
                        "offline-default"),
                compactText(System.getenv("VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_EFFECTIVE_MAVEN_MODE"),
                        "-o"),
                compactText(System.getenv("VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_PLUGIN_COORDINATES")),
                compactText(System.getenv("VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_PLUGIN_RESOLUTION_STATUS"),
                        "not-attempted"),
                compactText(System.getenv("VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL_RESULT"),
                        "skipped"),
                compactText(System.getenv("VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_OUTPUT_USABILITY"),
                        "not-inspected"),
                compactText(System.getenv("VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL_NAME"),
                        "prepare-frontend"),
                compactText(System.getenv("VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_PRODUCTION_MODE"),
                        "false"),
                compactText(System.getenv("VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_FORCE_PRODUCTION_BUILD"),
                        "false"),
                compactText(System.getenv("VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_IGNORE_VERSION_CHECKS"),
                        "false"));
    }

    private static List<Map<String, Object>> currentAppFrontendBuildOutputInventory(Path module) {
        Path target = module.resolve("target").normalize();
        List<Path> candidates = List.of(
                target,
                module.resolve("src/main/frontend/generated").normalize(),
                module.resolve("src/main/bundles").normalize(),
                target.resolve("classes").resolve(DEV_BUNDLE_GENERATED_FLOW_IMPORTS_JS).normalize(),
                target.resolve("classes/META-INF/VAADIN/webapp/VAADIN/build").normalize(),
                target.resolve("classes/META-INF/VAADIN/config").normalize(),
                target.resolve("classes/META-INF/VAADIN/build").normalize(),
                target.resolve("classes/META-INF/resources/VAADIN/build").normalize(),
                target.resolve("generated-resources/META-INF/VAADIN/build").normalize(),
                target.resolve("frontend/generated").normalize(),
                module.resolve("frontend/generated").normalize(),
                target.resolve("classes/META-INF/resources/vatori-flow-ipc-real-build-probe-v1_27.js")
                        .normalize());
        List<Map<String, Object>> inventory = new ArrayList<>();
        for (Path candidate : candidates) {
            inventory.add(buildOutputCandidate(candidate));
        }
        return inventory;
    }

    private static Map<String, Object> buildOutputCandidate(Path candidate) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        boolean exists = Files.exists(candidate);
        boolean file = Files.isRegularFile(candidate);
        boolean directory = Files.isDirectory(candidate);
        metadata.put("path", redactLocation(candidate.toString()));
        metadata.put("exists", exists);
        metadata.put("file", file);
        metadata.put("directory", directory);
        metadata.put("status", file ? "file" : directory ? "directory" : "missing");
        metadata.put("mtimeMillis", lastModifiedMillis(candidate));
        if (file) {
            try {
                byte[] bytes = Files.readAllBytes(candidate);
                metadata.put("fileCount", 1L);
                metadata.put("byteCount", (long) bytes.length);
                metadata.put("sha256", Resource.sha256(bytes));
            } catch (IOException error) {
                metadata.put("fileCount", 0L);
                metadata.put("byteCount", 0L);
                metadata.put("sha256", "");
                metadata.put("readError", error.getClass().getSimpleName());
            }
        } else if (directory) {
            DirectoryInventory directoryInventory = directoryInventory(candidate);
            metadata.put("fileCount", directoryInventory.fileCount());
            metadata.put("byteCount", directoryInventory.byteCount());
            metadata.put("sha256", "");
            if (!directoryInventory.readError().isBlank()) {
                metadata.put("readError", directoryInventory.readError());
            }
        } else {
            metadata.put("fileCount", 0L);
            metadata.put("byteCount", 0L);
            metadata.put("sha256", "");
        }
        return metadata;
    }

    private static DirectoryInventory directoryInventory(Path directory) {
        long[] counters = new long[] { 0L, 0L };
        try (Stream<Path> stream = Files.walk(directory, 8)) {
            stream.filter(Files::isRegularFile).forEach(path -> {
                counters[0]++;
                try {
                    counters[1] += Files.size(path);
                } catch (IOException ignored) {
                    // Byte count is diagnostic only.
                }
            });
            return new DirectoryInventory(counters[0], counters[1], "");
        } catch (IOException error) {
            return new DirectoryInventory(0L, 0L, error.getClass().getSimpleName());
        }
    }

    private static String firstObservedInventoryPath(List<Map<String, Object>> inventory) {
        for (Map<String, Object> candidate : inventory) {
            if (Boolean.TRUE.equals(candidate.get("exists"))) {
                Object path = candidate.get("path");
                return path == null ? "" : path.toString();
            }
        }
        return "";
    }

    private static long lastModifiedMillis(Path path) {
        if (!Files.exists(path)) {
            return 0L;
        }
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException error) {
            return 0L;
        }
    }

    private static AppBuildGeneratedImportsEvidence appBuildGeneratedImportsEvidence() {
        Path module = appModuleDirectory();
        Path target = module.resolve("target").normalize();
        Path generatedImports = findAppGeneratedImports(module);
        if (generatedImports == null || !Files.isRegularFile(generatedImports)) {
            Path candidate = target.resolve("classes").resolve(DEV_BUNDLE_GENERATED_FLOW_IMPORTS_JS).normalize();
            return new AppBuildGeneratedImportsEvidence(
                    redactLocation(module.toString()),
                    redactLocation(candidate.toString()),
                    false,
                    0L,
                    0,
                    "",
                    "missing",
                    false,
                    0,
                    "missing-build-output",
                    "",
                    "");
        }
        try {
            byte[] bytes = Files.readAllBytes(generatedImports);
            String body = new String(bytes, StandardCharsets.UTF_8);
            GeneratedLoadOnDemandChunkFileRegistry registry =
                    GeneratedLoadOnDemandChunkFileRegistry.parse(redactLocation(generatedImports.toString()),
                            body);
            String chunkStatus = registry.chunkFileStatus();
            String chunkArtifact = registry.chunkArtifactPath();
            if ("branch-table-dynamic-import".equals(registry.loadOnDemandTableShape())
                    && !registry.chunkImportSpecifier().isBlank()) {
                Path chunkPath = generatedImports.getParent()
                        .resolve(registry.chunkImportSpecifier().replaceFirst("^\\./", ""))
                        .normalize();
                chunkArtifact = redactLocation(chunkPath.toString());
                chunkStatus = Files.isRegularFile(chunkPath) ? "available" : "not-found";
            }
            return new AppBuildGeneratedImportsEvidence(
                    redactLocation(module.toString()),
                    redactLocation(generatedImports.toString()),
                    true,
                    Files.getLastModifiedTime(generatedImports).toMillis(),
                    bytes.length,
                    Resource.sha256(bytes),
                    registry.loadOnDemandTableShape(),
                    registry.noOpLoadOnDemand(),
                    countGeneratedLazyChunkReferences(body),
                    chunkStatus,
                    chunkArtifact,
                    "");
        } catch (IOException error) {
            return new AppBuildGeneratedImportsEvidence(
                    redactLocation(module.toString()),
                    redactLocation(generatedImports.toString()),
                    false,
                    0L,
                    0,
                    "",
                    "unreadable",
                    false,
                    0,
                    "blocked",
                    "",
                    error.getClass().getSimpleName());
        }
    }

    private static CurrentAppBuildFrontendOutputEvidence currentAppBuildFrontendOutputEvidence() {
        FrontendBuildStepEvidence buildStep = frontendBuildStepEvidence();
        Path module = appModuleDirectory();
        Path sourceGeneratedImports = sourceGeneratedImportsPath(module);
        FileEvidence sourceImports = fileEvidence(sourceGeneratedImports);
        String tableShape = "missing";
        String chunkId = "";
        String chunkImportSpecifier = "";
        String chunkArtifact = "";
        String chunkVirtualPath = "";
        String chunkStatus = sourceImports.observed() ? "not-generated" : "missing";
        FileEvidence sourceChunk = FileEvidence.missing(module.resolve("src/main/frontend/generated/flow/chunks"));
        boolean sourceChunkContainsBareImports = false;
        String sourceChunkBareImportSpecifiers = "";
        String sourceChunkPackageResolvedSpecifiers = "";
        String sourceChunkPackageUnresolvedSpecifiers = "";
        String sourceChunkPackageResolverMode = "not-applicable";
        String sourceGeneratedImportsRootBareImportSpecifiers = "";

        if (sourceImports.observed()) {
            try {
                String body = Files.readString(sourceGeneratedImports, StandardCharsets.UTF_8);
                sourceGeneratedImportsRootBareImportSpecifiers =
                        String.join(",", bareModuleImportSpecifiers(body));
                GeneratedLoadOnDemandChunkFileRegistry registry =
                        GeneratedLoadOnDemandChunkFileRegistry.parse(
                                CURRENT_APP_BUILD_FRONTEND_GENERATED_IMPORTS_JS, body);
                tableShape = registry.loadOnDemandTableShape();
                chunkId = registry.chunkId();
                chunkImportSpecifier = registry.chunkImportSpecifier();
                if (!chunkImportSpecifier.isBlank()) {
                    Path chunkPath = sourceGeneratedImports.getParent()
                            .resolve(chunkImportSpecifier.replaceFirst("^\\./", ""))
                            .normalize();
                    sourceChunk = fileEvidence(chunkPath);
                    chunkArtifact = sourceChunk.path();
                    chunkVirtualPath = CURRENT_APP_BUILD_FRONTEND_GENERATED_CHUNK_ROOT
                            + chunkImportSpecifier.replaceFirst("^\\./", "");
                    chunkStatus = sourceChunk.observed() ? "available" : "not-found";
                    if (sourceChunk.observed()) {
                        List<String> bareSpecifiers =
                                bareModuleImportSpecifiers(Files.readString(chunkPath, StandardCharsets.UTF_8));
                        List<String> resolvedSpecifiers = bareSpecifiers.stream()
                                .filter(FlowClientResources::isCurrentAppGeneratedChunkResolvableSpecifier)
                                .toList();
                        List<String> unresolvedSpecifiers = bareSpecifiers.stream()
                                .filter(specifier -> !isCurrentAppGeneratedChunkResolvableSpecifier(specifier))
                                .toList();
                        sourceChunkContainsBareImports = !bareSpecifiers.isEmpty();
                        sourceChunkBareImportSpecifiers = String.join(",", bareSpecifiers);
                        sourceChunkPackageResolvedSpecifiers = String.join(",", resolvedSpecifiers);
                        sourceChunkPackageUnresolvedSpecifiers = String.join(",", unresolvedSpecifiers);
                        if (bareSpecifiers.isEmpty()) {
                            sourceChunkPackageResolverMode = "no-bare-specifiers";
                        } else if (unresolvedSpecifiers.isEmpty()) {
                            sourceChunkPackageResolverMode = CURRENT_APP_GENERATED_CHUNK_PACKAGE_RESOLVER_MODE;
                        } else {
                            sourceChunkPackageResolverMode = "current-app-source-chunk-package-edge-unresolved";
                        }
                    }
                }
            } catch (IOException error) {
                tableShape = "unreadable";
                chunkStatus = "blocked";
            }
        }

        Path statsPath = module.resolve("target/classes/META-INF/VAADIN/config/stats.json").normalize();
        FileEvidence stats = fileEvidence(statsPath);
        FileEvidence prodBundle = fileEvidence(module.resolve("src/main/bundles/prod.bundle").normalize());
        Path productionBuildRoot = productionBuildRootPath(module);
        boolean productionBuildObserved = Files.isDirectory(productionBuildRoot);
        String productionEntryScriptVirtualPath = currentAppProductionEntryScriptVirtualPath(statsPath);
        Path productionGeneratedImports = findProductionGeneratedImports(
                productionBuildRoot, productionEntryScriptVirtualPath);
        FileEvidence productionGeneratedImportsEvidence = fileEvidence(productionGeneratedImports);
        String productionBuildJsVirtualPaths = currentAppProductionBuildVirtualPaths(productionBuildRoot);
        String productionGeneratedImportsVirtualPath = productionGeneratedImports == null
                ? "" : currentAppProductionBuildVirtualPath(productionGeneratedImports);
        String statsBundleImports = String.join(",", statsBundleImports(statsPath));
        List<String> sourceRootBareSpecifiers = csvValues(sourceGeneratedImportsRootBareImportSpecifiers);
        boolean rootProductionSupportObserved = productionGeneratedImportsEvidence.observed()
                && !productionGeneratedImportsVirtualPath.isBlank()
                && !statsBundleImports.isBlank();
        List<String> statsBundleImportList = csvValues(statsBundleImports);
        List<String> sourceRootResolvedSpecifiers = sourceRootBareSpecifiers.stream()
                .filter(specifier -> rootProductionSupportObserved && statsBundleImportList.contains(specifier))
                .toList();
        List<String> sourceRootUnresolvedSpecifiers = sourceRootBareSpecifiers.stream()
                .filter(specifier -> !sourceRootResolvedSpecifiers.contains(specifier))
                .toList();
        String sourceGeneratedImportsRootResolvedSpecifiers = String.join(",", sourceRootResolvedSpecifiers);
        String sourceGeneratedImportsRootUnresolvedSpecifiers = String.join(",", sourceRootUnresolvedSpecifiers);
        String sourceGeneratedImportsRootPackageResolverMode;
        if (sourceRootBareSpecifiers.isEmpty()) {
            sourceGeneratedImportsRootPackageResolverMode = "no-root-bare-specifiers";
        } else if (sourceRootUnresolvedSpecifiers.isEmpty()) {
            sourceGeneratedImportsRootPackageResolverMode =
                    CURRENT_APP_GENERATED_IMPORTS_ROOT_PACKAGE_RESOLVER_MODE;
        } else {
            sourceGeneratedImportsRootPackageResolverMode =
                    "current-app-source-generated-imports-root-package-graph-unresolved";
        }
        boolean sourceGeneratedImportsRootPlaceholderUsed = false;
        String sourceGeneratedImportsRootFailureLayer = sourceRootUnresolvedSpecifiers.isEmpty()
                ? CURRENT_APP_GENERATED_IMPORTS_ROOT_FAILURE_LAYER
                : "current-app-source-generated-imports-root-package-specifier-unresolved";

        String runtimeCandidate;
        if (sourceImports.observed()) {
            runtimeCandidate = "source-generated-imports";
        } else if (productionGeneratedImportsEvidence.observed()) {
            runtimeCandidate = "production-webapp-build";
        } else {
            runtimeCandidate = "none";
        }

        String failureLayer;
        if (!"build-frontend".equals(buildStep.goal())) {
            failureLayer = "build-frontend-goal-not-selected";
        } else if ("failed".equals(buildStep.status()) || "failed".equals(buildStep.goalResult())) {
            failureLayer = "build-frontend-goal-failed-before-output";
        } else if (!sourceImports.observed() && !productionGeneratedImportsEvidence.observed()) {
            failureLayer = "build-frontend-output-missing-generated-imports";
        } else if ("available".equals(chunkStatus)
                && sourceChunkContainsBareImports
                && !sourceChunkPackageUnresolvedSpecifiers.isBlank()) {
            failureLayer = "current-app-source-generated-chunk-npm-specifier-unresolved";
        } else if (!sourceGeneratedImportsRootUnresolvedSpecifiers.isBlank()) {
            failureLayer = "current-app-source-generated-imports-root-package-specifier-unresolved";
        } else if ("available".equals(chunkStatus) && sourceChunkContainsBareImports) {
            failureLayer = CURRENT_APP_GENERATED_IMPORTS_ROOT_FAILURE_LAYER;
        } else if ("available".equals(chunkStatus)) {
            failureLayer = "pending-current-app-source-generated-chunk-stage";
        } else if (productionGeneratedImportsEvidence.observed()) {
            failureLayer = "current-app-production-generated-imports-owner-boundary";
        } else {
            failureLayer = "current-app-build-frontend-output-incomplete";
        }

        return new CurrentAppBuildFrontendOutputEvidence(
                buildStep.goal(),
                sourceImports.observed(),
                sourceImports.path(),
                sourceImports.lastModifiedMillis(),
                sourceImports.bytes(),
                sourceImports.sha256(),
                tableShape,
                chunkId,
                chunkImportSpecifier,
                chunkArtifact,
                chunkVirtualPath,
                chunkStatus,
                sourceChunk.bytes(),
                sourceChunk.sha256(),
                sourceChunkContainsBareImports,
                sourceChunkBareImportSpecifiers,
                sourceChunkPackageResolvedSpecifiers,
                sourceChunkPackageUnresolvedSpecifiers,
                sourceChunkPackageResolverMode,
                sourceGeneratedImportsRootBareImportSpecifiers,
                statsBundleImports,
                sourceGeneratedImportsRootResolvedSpecifiers,
                sourceGeneratedImportsRootUnresolvedSpecifiers,
                sourceGeneratedImportsRootPackageResolverMode,
                productionGeneratedImportsVirtualPath,
                rootProductionSupportObserved,
                sourceGeneratedImportsRootPlaceholderUsed,
                sourceGeneratedImportsRootFailureLayer,
                productionEntryScriptVirtualPath,
                productionBuildJsVirtualPaths,
                productionBuildObserved,
                redactLocation(productionBuildRoot.toString()),
                productionGeneratedImportsEvidence.observed(),
                productionGeneratedImportsEvidence.path(),
                productionGeneratedImportsEvidence.bytes(),
                productionGeneratedImportsEvidence.sha256(),
                stats.observed(),
                stats.path(),
                prodBundle.observed(),
                prodBundle.path(),
                prodBundle.bytes(),
                prodBundle.sha256(),
                runtimeCandidate,
                failureLayer);
    }

    private static Path appModuleDirectory() {
        String module = System.getenv("VAADIN_DESKTOP_MODULE");
        if (module != null && !module.isBlank()) {
            return Path.of(module).toAbsolutePath().normalize();
        }
        String repoRoot = System.getenv("VAADIN_DESKTOP_REPO_ROOT");
        Path root = repoRoot == null || repoRoot.isBlank()
                ? Path.of("").toAbsolutePath().normalize()
                : Path.of(repoRoot).toAbsolutePath().normalize();
        Path smokeApp = root.resolve("test-apps/flow-over-ipc-smoke-app").normalize();
        if (Files.isDirectory(smokeApp)) {
            return smokeApp;
        }
        return root;
    }

    private static Path sourceGeneratedImportsPath(Path module) {
        return module.resolve("src/main/frontend/generated/flow/generated-flow-imports.js").normalize();
    }

    private static Path productionBuildRootPath(Path module) {
        return module.resolve("target/classes/META-INF/VAADIN/webapp/VAADIN/build").normalize();
    }

    private static String currentAppProductionBuildVirtualPath(Path artifact) {
        return CURRENT_APP_PRODUCTION_BUILD_VIRTUAL_ROOT + artifact.getFileName();
    }

    private static String currentAppProductionBuildVirtualPaths(Path productionBuildRoot) {
        if (!Files.isDirectory(productionBuildRoot)) {
            return "";
        }
        try (Stream<Path> stream = Files.list(productionBuildRoot)) {
            return String.join(",", stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".js"))
                    .sorted()
                    .map(FlowClientResources::currentAppProductionBuildVirtualPath)
                    .toList());
        } catch (IOException ignored) {
            return "";
        }
    }

    private static String currentAppProductionEntryScriptVirtualPath(Path statsPath) {
        String entry = firstStatsArrayValue(statsPath, "entryScripts");
        if (entry.isBlank()) {
            return "";
        }
        String prefix = "VAADIN/build/";
        if (entry.startsWith(prefix)) {
            return CURRENT_APP_PRODUCTION_BUILD_VIRTUAL_ROOT + entry.substring(prefix.length());
        }
        return CURRENT_APP_PRODUCTION_BUILD_VIRTUAL_ROOT + fileName(entry);
    }

    private static Path findAppGeneratedImports(Path module) {
        List<Path> candidates = List.of(
                sourceGeneratedImportsPath(module),
                module.resolve("target/classes").resolve(DEV_BUNDLE_GENERATED_FLOW_IMPORTS_JS),
                module.resolve("target/classes/META-INF/VAADIN/build/generated-flow-imports-VW7hRaNw.js"),
                module.resolve("target/generated-resources").resolve(DEV_BUNDLE_GENERATED_FLOW_IMPORTS_JS),
                module.resolve("target/frontend/generated-flow-imports-VW7hRaNw.js"));
        for (Path candidate : candidates) {
            Path normalized = candidate.normalize();
            if (Files.isRegularFile(normalized)) {
                return normalized;
            }
        }
        Path target = module.resolve("target").normalize();
        if (!Files.isDirectory(target)) {
            return null;
        }
        try (Stream<Path> stream = Files.find(target, 8,
                (path, attributes) -> attributes.isRegularFile()
                        && path.getFileName().toString().startsWith("generated-flow-imports")
                        && path.getFileName().toString().endsWith(".js"))) {
            return stream.findFirst().orElse(null);
        } catch (IOException ignored) {
            return null;
        }
    }

    private static Path findProductionGeneratedImports(Path root, String productionEntryScriptVirtualPath) {
        if (root == null || !Files.isDirectory(root)) {
            return null;
        }
        try (Stream<Path> stream = Files.find(root, 2,
                (path, attributes) -> attributes.isRegularFile()
                        && path.getFileName().toString().startsWith("generated-flow-imports")
                        && path.getFileName().toString().endsWith(".js"))) {
            List<Path> candidates = stream.sorted().toList();
            String entryFileName = fileName(productionEntryScriptVirtualPath);
            if (!entryFileName.isBlank()) {
                String importNeedle = "./" + entryFileName;
                for (Path candidate : candidates) {
                    try {
                        if (Files.readString(candidate, StandardCharsets.UTF_8).contains(importNeedle)) {
                            return candidate;
                        }
                    } catch (IOException ignored) {
                        // Fall through to the deterministic first candidate when a body is unreadable.
                    }
                }
            }
            return candidates.isEmpty() ? null : candidates.get(0);
        } catch (IOException ignored) {
            return null;
        }
    }

    private static FileEvidence fileEvidence(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return FileEvidence.missing(path);
        }
        try {
            byte[] bytes = Files.readAllBytes(path);
            return new FileEvidence(redactLocation(path.toString()),
                    true,
                    Files.getLastModifiedTime(path).toMillis(),
                    bytes.length,
                    Resource.sha256(bytes));
        } catch (IOException error) {
            return FileEvidence.missing(path);
        }
    }

    private static boolean containsBareModuleImports(String body) {
        return !bareModuleImportSpecifiers(body).isEmpty();
    }

    private static List<String> bareModuleImportSpecifiers(String body) {
        if (body == null || body.isBlank()) {
            return List.of();
        }
        Pattern pattern = Pattern.compile(
                "(?m)^\\s*import\\s+(?:[^'\";]+\\s+from\\s+)?['\"](?!\\.|/|[a-zA-Z][a-zA-Z0-9+.-]*:)([^'\"]+)['\"]");
        Matcher matcher = pattern.matcher(body);
        List<String> specifiers = new ArrayList<>();
        while (matcher.find()) {
            String specifier = matcher.group(1);
            if (specifier != null && !specifier.isBlank() && !specifiers.contains(specifier)) {
                specifiers.add(specifier);
            }
        }
        return specifiers;
    }

    private static List<String> csvValues(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (String value : csv.split(",")) {
            String trimmed = value.trim();
            if (!trimmed.isBlank() && !values.contains(trimmed)) {
                values.add(trimmed);
            }
        }
        return values;
    }

    private static String fileName(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private static List<String> statsBundleImports(Path statsPath) {
        return statsArrayValues(statsPath, "bundleImports");
    }

    private static String firstStatsArrayValue(Path statsPath, String key) {
        List<String> values = statsArrayValues(statsPath, key);
        return values.isEmpty() ? "" : values.get(0);
    }

    private static List<String> statsArrayValues(Path statsPath, String key) {
        if (statsPath == null || !Files.isRegularFile(statsPath)) {
            return List.of();
        }
        try {
            String body = Files.readString(statsPath, StandardCharsets.UTF_8);
            Pattern array = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
            Matcher arrayMatcher = array.matcher(body);
            if (!arrayMatcher.find()) {
                return List.of();
            }
            Matcher valueMatcher = Pattern.compile("\"([^\"]+)\"").matcher(arrayMatcher.group(1));
            List<String> values = new ArrayList<>();
            while (valueMatcher.find()) {
                String value = valueMatcher.group(1);
                if (!values.contains(value)) {
                    values.add(value);
                }
            }
            return values;
        } catch (IOException ignored) {
            return List.of();
        }
    }

    private static boolean isCurrentAppGeneratedChunkResolvableSpecifier(String specifier) {
        return CURRENT_APP_GENERATED_CHUNK_RESOLVED_SPECIFIERS.equals(specifier);
    }

    private static int countGeneratedLazyChunkReferences(String body) {
        if (body == null || body.isBlank()) {
            return 0;
        }
        int count = 0;
        Matcher matcher = GENERATED_LAZY_CHUNK_IMPORT.matcher(body);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static String propertyOrDefault(Properties properties, String key, String defaultValue) {
        String value = properties.getProperty(key);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static int parseInt(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException error) {
            return fallback;
        }
    }

    private static long parseLong(String value, long fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException error) {
            return fallback;
        }
    }

    private static String compactText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    private static String compactText(String value, String fallback) {
        String compacted = compactText(value);
        return compacted.isBlank() ? fallback : compacted;
    }

    private static Class<?> loadOptionalClass(String className) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = FlowClientResources.class.getClassLoader();
        }
        try {
            return Class.forName(className, false, loader);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private static ClasspathArtifactEvidence classpathArtifactEvidence(String path) {
        URL url = classpathResourceUrl(path);
        if (url == null) {
            return new ClasspathArtifactEvidence("missing", "", "", 0L);
        }
        String location = url.toExternalForm();
        return new ClasspathArtifactEvidence(classpathArtifactSource(location),
                redactLocation(location),
                url.getProtocol(),
                classpathArtifactLastModified(url));
    }

    private static URL classpathResourceUrl(String path) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = FlowClientResources.class.getClassLoader();
        }
        return loader.getResource(path);
    }

    private static String classpathArtifactSource(String location) {
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

    private static long classpathArtifactLastModified(URL url) {
        try {
            if ("file".equals(url.getProtocol())) {
                return Files.getLastModifiedTime(Path.of(url.toURI())).toMillis();
            }
            if ("jar".equals(url.getProtocol())) {
                String text = url.toExternalForm();
                int separator = text.indexOf("!/");
                if (text.startsWith("jar:") && separator > 4) {
                    URL jarUrl = new URL(text.substring(4, separator));
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

    private static WriteTargetEvidence writeTargetEvidence(String relativePath) {
        Path target = Path.of("").toAbsolutePath().normalize()
                .resolve("target")
                .resolve("classes")
                .resolve(relativePath)
                .normalize();
        boolean observed = Files.isRegularFile(target);
        long lastModified = 0L;
        if (observed) {
            try {
                lastModified = Files.getLastModifiedTime(target).toMillis();
            } catch (IOException ignored) {
                lastModified = 0L;
            }
        }
        return new WriteTargetEvidence(redactLocation(target.toString()), observed, lastModified);
    }

    private static String redactLocation(String location) {
        if (location == null || location.isBlank()) {
            return "";
        }
        String redacted = location;
        String userDir = Path.of("").toAbsolutePath().normalize().toString();
        if (!userDir.isBlank()) {
            redacted = redacted.replace(userDir, "$APP");
        }
        String home = System.getProperty("user.home", "");
        if (!home.isBlank()) {
            redacted = redacted.replace(home, "$HOME");
        }
        return redacted;
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
        attributes.putAll(packagedFrontendBaselineAttributes());
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

    private static Map<String, Object> packagedFrontendBaselineAttributes() {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("packagedFrontendBaselineVersion", PACKAGED_FRONTEND_BASELINE_VERSION);
        attributes.put("packagedFrontendProductPath", PACKAGED_FRONTEND_PRODUCT_PATH);
        attributes.put("packagedFrontendInventoryScope", PACKAGED_FRONTEND_INVENTORY_SCOPE);
        attributes.put("packagedFrontendPreferredLoader", PACKAGED_FRONTEND_PREFERRED_LOADER);
        attributes.put("packagedFrontendAllowedFallback", PACKAGED_FRONTEND_ALLOWED_FALLBACK);
        attributes.put("packagedFrontendIntegrity", PACKAGED_FRONTEND_INTEGRITY);
        attributes.put("packagedFrontendCleanupPolicy", PACKAGED_FRONTEND_CLEANUP_POLICY);
        attributes.put("packagedFrontendRealBuildStatus", PACKAGED_FRONTEND_REAL_BUILD_STATUS);
        attributes.put("packagedFrontendRemainingBlockers", PACKAGED_FRONTEND_REMAINING_BLOCKERS);
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

    private record ClasspathArtifactEvidence(
            String source,
            String runtimePath,
            String protocol,
            long lastModifiedMillis) {
    }

    private record WriteTargetEvidence(
            String path,
            boolean observed,
            long lastModifiedMillis) {
    }

    private record AppBuildGeneratedImportsEvidence(
            String modulePath,
            String path,
            boolean observed,
            long lastModifiedMillis,
            int bytes,
            String sha256,
            String tableShape,
            boolean noOpLoadOnDemand,
            int chunkReferences,
            String chunkStatus,
            String chunkArtifact,
            String readError) {
    }

    private record FrontendBuildStepEvidence(
            String command,
            int exitCode,
            String status,
            long durationMillis,
            String outputRoots,
            String summary,
            String stdoutLog,
            String stderrLog,
            String availabilityMode,
            String effectiveMavenMode,
            String pluginCoordinates,
            String pluginResolutionStatus,
            String goalResult,
            String outputUsability,
            String goal,
            String productionMode,
            String forceProductionBuild,
            String ignoreVersionChecks) {
    }

    private record FileEvidence(
            String path,
            boolean observed,
            long lastModifiedMillis,
            int bytes,
            String sha256) {

        private static FileEvidence missing(Path path) {
            return new FileEvidence(path == null ? "" : redactLocation(path.toString()), false, 0L, 0, "");
        }
    }

    private record CurrentAppBuildFrontendOutputEvidence(
            String goal,
            boolean sourceGeneratedImportsObserved,
            String sourceGeneratedImportsPath,
            long sourceGeneratedImportsMtimeMillis,
            int sourceGeneratedImportsBytes,
            String sourceGeneratedImportsSha256,
            String sourceGeneratedImportsTableShape,
            String sourceChunkId,
            String sourceChunkImportSpecifier,
            String sourceChunkArtifact,
            String sourceChunkVirtualPath,
            String sourceChunkStatus,
            int sourceChunkBytes,
            String sourceChunkSha256,
            boolean sourceChunkContainsBareImports,
            String sourceChunkBareImportSpecifiers,
            String sourceChunkPackageResolvedSpecifiers,
            String sourceChunkPackageUnresolvedSpecifiers,
            String sourceChunkPackageResolverMode,
            String sourceGeneratedImportsRootBareImportSpecifiers,
            String statsBundleImports,
            String sourceGeneratedImportsRootResolvedSpecifiers,
            String sourceGeneratedImportsRootUnresolvedSpecifiers,
            String sourceGeneratedImportsRootPackageResolverMode,
            String sourceGeneratedImportsRootProductionSupportArtifact,
            boolean sourceGeneratedImportsRootProductionSupportObserved,
            boolean sourceGeneratedImportsRootPlaceholderUsed,
            String sourceGeneratedImportsRootFailureLayer,
            String productionEntryScriptVirtualPath,
            String productionBuildJsVirtualPaths,
            boolean productionBuildObserved,
            String productionBuildRoot,
            boolean productionGeneratedImportsObserved,
            String productionGeneratedImportsPath,
            int productionGeneratedImportsBytes,
            String productionGeneratedImportsSha256,
            boolean statsObserved,
            String statsPath,
            boolean prodBundleObserved,
            String prodBundlePath,
            int prodBundleBytes,
            String prodBundleSha256,
            String runtimeCandidate,
            String failureLayer) {
    }

    private record DirectoryInventory(
            long fileCount,
            long byteCount,
            String readError) {
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
