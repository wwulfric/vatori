package dev.vatori.desktop.flow.ipc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class FlowIpcComponentManifestAttributes {
    private static final String SOURCE = "vaadin-component-classpath-plus-dev-bundle-v1_4";
    private static final String RESOLVER_STRATEGY = "generic-component-frontend-manifest-v1_3";

    private FlowIpcComponentManifestAttributes() {
    }

    static Map<String, Object> attributes(Context context) {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("source", SOURCE);
        manifest.put("frontendAssetPipelineVersion", context.frontendAssetPipelineVersion());
        manifest.put("artifactGraphSource", context.artifactGraphSource());
        putTextFieldAttributes(manifest, context.textField());
        manifest.putAll(context.generatedLazyManifestAttributes());
        manifest.putAll(FlowIpcProductionOwnerAttributes.positiveFixtureManifestAttributes());
        manifest.put("runtimeResolverContractVersion", context.runtimeResolverContractVersion());
        manifest.putAll(FlowIpcFrontendPipelineAttributes.resourceLoader());
        manifest.putAll(FlowIpcFrontendPipelineAttributes.resolverContract());
        manifest.putAll(FlowIpcLazyDependencyAttributes.manifestAttributes());
        manifest.putAll(FlowIpcProductionRuntimeAttributes.runtimeAttributes(
                context.runtime().statsJson(), context.runtime().selectedArtifact()));
        manifest.putAll(context.productionRuntimeOwnerAttributes());
        putArtifactFamilyAttributes(manifest, context);
        manifest.put("transport", "flow-ipc");
        manifest.put("resourceLoading", "ipc-resource-request");
        putComponentInventoryAttributes(manifest, context.components());
        manifest.put("requestedComponent", "com.vaadin.flow.component.textfield.TextField");
        manifest.put("substituteReason",
                "TextField class is available but its npm frontend graph is not classpath-resolved in this smoke runtime; BigDecimalField provides a real TextField-family classpath frontend module.");
        manifest.put("classpath", true);
        manifest.put("npmFrontendGraphComplete", false);
        manifest.put("productionBundleReady", false);
        manifest.put("devModeReady", false);
        manifest.put("failureClassification", "real-textfield-production-graph-executed-v1_7");
        manifest.put("narrowedFailureClassification", context.textField().narrowedFailureClassification());
        manifest.put("previousBlockerVersion", context.textField().previousBlockerVersion());
        manifest.put("executionVersion", context.textField().executionVersion());
        manifest.put("blockedReason", context.componentTransitiveBlockedReason());
        manifest.put("unresolvedImports", List.of(
                Map.of("specifier", "@vaadin/text-field/src/vaadin-text-field.js",
                        "classification", "legacy-textfield-shim-fallback-not-success",
                        "resolution", context.textField().shimJs())));
        manifest.put("resolvedImports", List.of(
                Map.of("specifier", "lit",
                        "classification", "focused-extracted-production-bundle-helper",
                        "resolution", context.textField().litEntryArtifact()),
                Map.of("specifier", "@vaadin/text-field/src/vaadin-text-field.js",
                        "classification", "real-textfield-production-graph-executed-v1_7",
                        "resolution", context.textField().entryArtifact()),
                Map.of("specifier", "@vaadin/component-base/src/define.js",
                        "classification", "focused-extracted-production-bundle-helper",
                        "resolution", context.textField().defineEntryArtifact())));
        manifest.put("count", context.resourceCount());
        return manifest;
    }

    private static void putTextFieldAttributes(Map<String, Object> manifest, TextFieldContext context) {
        manifest.put("textfieldGraphParityVersion", context.graphParityVersion());
        manifest.put("textfieldPreviousBlockerVersion", context.previousBlockerVersion());
        manifest.put("textfieldExecutionVersion", context.executionVersion());
        manifest.put("textfieldSideEffectVersion", context.sideEffectVersion());
        manifest.put("textfieldGraphProbe", context.graphProbe());
        manifest.put("textfieldImportResolutionStrategy", context.importResolutionStrategy());
        manifest.put("textfieldSideEffectStrategy", context.sideEffectStrategy());
        manifest.put("textfieldNarrowedFailureClassification", context.narrowedFailureClassification());
        manifest.put("textfieldFailureLayer", context.failureLayer());
        manifest.put("textfieldEntryArtifact", context.entryArtifact());
        manifest.put("textfieldClassExportArtifact", context.classExportArtifact());
        manifest.put("textfieldTransitiveArtifacts", context.transitiveArtifacts());
        manifest.put("textfieldFirstDefinitionSource", context.firstDefinitionSource());
        manifest.put("textfieldSecondDefinitionSource", context.secondDefinitionSource());
        manifest.put("textfieldDuplicateTag", context.duplicateTag());
        manifest.put("textfieldRemainingBlockerClassification", context.remainingBlockerClassification());
        manifest.put("textfieldMinimalReproduction", context.minimalReproduction());
        manifest.put("textfieldNextResolutionStep", context.nextResolutionStep());
    }

    private static void putArtifactFamilyAttributes(Map<String, Object> manifest, Context context) {
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
                "resolver", context.componentResolver(),
                "resourceCount", context.resourceCount()));
        manifest.put("strictBlockers", List.of(
                strictBlocker("dev-mode-bundle", "dev-mode-bundle-execution-unmapped",
                        "dev-mode package-lock dependency graph is verified over IPC, but Vite/dev-server module execution is not yet mapped into the no-localhost resolver"),
                strictBlocker("generated-lazy", "generated-lazy-loadondemand-api-boundary-v1_9",
                        "generated-flow-imports relative dependencies execute through the no-localhost staging resolver, but the real Flow.loadOnDemand API is still installed by the direct staged generated-flow-imports artifact rather than triggered by the Flow client engine"),
                strictBlocker("generated-lazy",
                        FlowIpcGeneratedLazyAttributes.CHUNK_BLOCKER_VERSION,
                        "real UIDL Flow.loadOnDemand expressions are observed and mapped to the generated-flow-imports artifact, but this dev bundle exposes a no-op loadOnDemand table without chunk-file resolution"),
                strictBlocker("generated-lazy",
                        FlowIpcGeneratedLazyAttributes.CHUNK_FILE_BLOCKER_VERSION,
                        "generated-flow-imports has been parsed for chunk-file branches; this Vaadin dev bundle exposes a real no-op Promise.resolve(0) loadOnDemand table, so there is no generated ./chunks/chunk-*.js file to stage yet"),
                strictBlocker("generated-lazy",
                        FlowIpcGeneratedLazyBuildDiagnostics.BUILD_LIFECYCLE_VERSION,
                        "the v1.27 route/lazy JavaScript probe is visible on the app classpath, but the no-localhost runtime still resolves generated-flow-imports from the static packaged Vaadin dev-bundle instead of an app update-imports output"),
                strictBlocker("generated-lazy", context.generatedLazyAppBuildOutputVersion(),
                        "current app build output discovery is explicit, but generated-flow-imports is still missing from the app module output so the runtime uses the packaged dev-bundle fallback"),
                strictBlocker("production-bundle", FlowIpcProductionRuntimeAttributes.BLOCKER,
                        "production bundle stats now selects an executable runtime artifact and stages it through stable module identity, but full production runtime ownership is still blocked until side effects can be executed without relying on focused component entry imports"),
                strictBlocker("production-bundle",
                        FlowIpcProductionOwnerAttributes.RUNTIME_OWNER_BLOCKER,
                        "production bundle owner is now explicit and independently staged, but import remains blocked until the owner can safely execute indexhtml side effects without colliding with focused component entry imports"),
                strictBlocker("production-bundle",
                        FlowIpcProductionOwnerAttributes.RUNTIME_OWNER_IMPORT_VERSION,
                        "production bundle owner import is now a controlled v1.14 probe; success requires observable owner side effects, and acceptable failure must come from a real import attempt against the stats-selected indexhtml artifact"),
                strictBlocker("production-bundle",
                        FlowIpcProductionOwnerAttributes.RUNTIME_OWNER_DEPENDENCY_RESOLVER_VERSION,
                        "production bundle owner dependency resolver must classify and stage the real dev-bundle build graph before the owner import result is accepted")));
    }

    private static void putComponentInventoryAttributes(Map<String, Object> manifest,
            ComponentInventoryContext context) {
        manifest.put("componentClass", "com.vaadin.flow.component.textfield.BigDecimalField");
        manifest.put("componentTag", "vaadin-big-decimal-field");
        manifest.put("primaryResource", context.bigDecimalFieldArtifact());
        manifest.put("resolverStrategy", RESOLVER_STRATEGY);
        List<String> componentTags = new ArrayList<>(List.of("vaadin-big-decimal-field",
                "vaadin-checkbox", "vaadin-select", "vaadin-text-field"));
        List<String> primaryResources = new ArrayList<>(List.of(context.bigDecimalFieldArtifact(),
                context.checkboxEntryArtifact(), context.selectEntryArtifact(),
                context.textFieldEntryArtifact()));
        List<Map<String, Object>> components = new ArrayList<>(List.of(
                Map.of("componentClass", "com.vaadin.flow.component.textfield.BigDecimalField",
                        "componentTag", "vaadin-big-decimal-field",
                        "primaryResource", context.bigDecimalFieldArtifact(),
                        "resourceOrigin", "classpath-frontend-resource",
                        "productionBundleReady", false),
                Map.of("componentClass", "com.vaadin.flow.component.checkbox.Checkbox",
                        "componentTag", "vaadin-checkbox",
                        "primaryResource", context.checkboxEntryArtifact(),
                        "requestedModule", "@vaadin/checkbox/src/vaadin-checkbox.js",
                        "resourceOrigin", "vaadin-dev-bundle-checkbox-entry",
                        "productionBundleReady", true),
                Map.of("componentClass", "com.vaadin.flow.component.select.Select",
                        "componentTag", "vaadin-select",
                        "primaryResource", context.selectEntryArtifact(),
                        "requestedModule", "@vaadin/select/src/vaadin-select.js",
                        "resourceOrigin", "vaadin-dev-bundle-select-entry",
                        "productionBundleReady", true),
                Map.of("componentClass", "com.vaadin.flow.component.textfield.TextField",
                        "componentTag", "vaadin-text-field",
                        "primaryResource", context.textFieldEntryArtifact(),
                        "requestedModule", "@vaadin/text-field/src/vaadin-text-field.js",
                        "resourceOrigin", "vaadin-dev-bundle-text-field-entry",
                        "productionBundleReady", true)));
        if (!context.loginFormProductionArtifact().isBlank()) {
            componentTags.add(context.loginForm().tag());
            primaryResources.add(context.loginFormProductionArtifact());
            components.add(Map.of("componentClass", context.loginForm().componentClass(),
                    "componentTag", context.loginForm().tag(),
                    "primaryResource", context.loginFormProductionArtifact(),
                    "requestedModule", context.loginForm().requestedModule(),
                    "resourceOrigin", "current-app-production-webapp-build-login-form",
                    "productionBundleReady", true));
        }
        manifest.put("componentCount", componentTags.size());
        manifest.put("componentTags", componentTags);
        manifest.put("primaryResources", primaryResources);
        manifest.put("components", components);
    }

    private static Map<String, Object> artifactFamilyStatus(
            String family, String status, String classification) {
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

    record Context(
            int resourceCount,
            String frontendAssetPipelineVersion,
            String artifactGraphSource,
            String componentResolver,
            String runtimeResolverContractVersion,
            String generatedLazyAppBuildOutputVersion,
            String componentTransitiveBlockedReason,
            TextFieldContext textField,
            RuntimeContext runtime,
            ComponentInventoryContext components,
            Map<String, Object> generatedLazyManifestAttributes,
            Map<String, Object> productionRuntimeOwnerAttributes) {
    }

    record TextFieldContext(
            String graphParityVersion,
            String previousBlockerVersion,
            String executionVersion,
            String sideEffectVersion,
            String graphProbe,
            String importResolutionStrategy,
            String sideEffectStrategy,
            String narrowedFailureClassification,
            String failureLayer,
            String shimJs,
            String entryArtifact,
            String classExportArtifact,
            List<String> transitiveArtifacts,
            String litEntryArtifact,
            String defineEntryArtifact,
            String firstDefinitionSource,
            String secondDefinitionSource,
            String duplicateTag,
            String remainingBlockerClassification,
            String minimalReproduction,
            String nextResolutionStep) {
    }

    record RuntimeContext(String statsJson, String selectedArtifact) {
    }

    record ComponentInventoryContext(
            String bigDecimalFieldArtifact,
            String checkboxEntryArtifact,
            String selectEntryArtifact,
            String textFieldEntryArtifact,
            String loginFormProductionArtifact,
            LoginFormContext loginForm) {
    }

    record LoginFormContext(String componentClass, String tag, String requestedModule) {
    }
}
