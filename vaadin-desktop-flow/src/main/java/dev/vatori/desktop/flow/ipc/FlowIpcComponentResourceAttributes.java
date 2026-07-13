package dev.vatori.desktop.flow.ipc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class FlowIpcComponentResourceAttributes {
    private FlowIpcComponentResourceAttributes() {
    }

    static Map<String, Object> classpathComponent(
            Config config, boolean shim, String componentClass, String tag,
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
        attributes.put("dependencyOf", config.bigDecimalFieldJs());
        attributes.put("executionPolicy", shim ? "legacy-shim-fallback" : "execute-primary-module");
        attributes.put("artifactStatus", shim ? "legacy-fallback" : "available");
        attributes.put("requestedBy", "component-frontend-manifest-v1_4");
        attributes.put("failureClassification", shim
                ? "legacy-textfield-shim-fallback-not-success"
                : "real-textfield-production-graph-executed-v1_7");
        attributes.put("blockedReason", config.componentTransitiveBlockedReason());
        attributes.put("lookupKind", lookupKind(componentClass, resolver));
        if (shim) {
            attributes.put("legacyFallback", true);
            attributes.put("successPath", false);
        } else {
            attributes.put("shimmedImports", List.of());
            attributes.put("realTextFieldGraphImport", true);
            attributes.put("importRewriteStrategy", "manifest-import-rewrite-v1_4");
            attributes.put("browserExecutionPlan", "dynamic-import-blob-with-manifest-import-rewrites-v1_4");
            attributes.put("importRewrites", List.of(
                    importRewrite("lit",
                            config.textField().litEntryArtifact(),
                            "production-bundle",
                            "vaadin-dev-bundle-production-build-v1_4",
                            "dev-bundle-resource-graph-partial"),
                    importRewrite("@vaadin/text-field/src/vaadin-text-field.js",
                            config.textField().entryArtifact(),
                            "production-bundle",
                            config.textField().importResolutionStrategy(),
                            "real-textfield-production-graph-executed-v1_7"),
                    importRewrite("@vaadin/component-base/src/define.js",
                            config.textField().defineEntryArtifact(),
                            "production-bundle",
                            "vaadin-dev-bundle-production-build-v1_4",
                            "dev-bundle-resource-graph-partial")));
        }
        if ("classpath-frontend-resource".equals(resolver)) {
            attributes.put("componentPrimary", true);
        }
        return attributes;
    }

    static Map<String, Object> checkboxDevBundle(
            Config config, String tag, String moduleSpecifier, boolean primary) {
        return devBundle(config,
                "com.vaadin.flow.component.checkbox.Checkbox",
                tag,
                moduleSpecifier,
                primary,
                primary ? "Checkbox-dev-bundle-resource-v1_4" : "Checkbox-dev-bundle-support-resource-v1_4",
                primary ? "com.vaadin.flow.component.checkbox.Checkbox" : config.checkboxEntryJs());
    }

    static Map<String, Object> devBundle(
            Config config, String componentClass, String tag, String moduleSpecifier,
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
        attributes.put("blockedReason", config.devBundleComponentReason());
        attributes.put("failureClassification", "dev-bundle-resource-graph-partial");
        attributes.put("lookupKind", lookupKind);
        attributes.put("componentProductionOwnerReused", false);
        attributes.put("componentProductionOwnerReuseReason",
                "production owner stage identity is tracked separately in v1.13; focused component imports keep their existing stable resource-path identity until the owner import contract is enabled");
        attributes.put("componentProductionOwnerDependencyPaths", config.indexHtmlJs());
        if (primary) {
            attributes.put("componentPrimary", true);
        } else {
            attributes.put("componentBundleDependency", true);
        }
        if (moduleSpecifier.startsWith(config.devBundleBuildRoot())) {
            FlowIpcProductionOwnerAttributes.putDependencyResolverAttributes(
                    attributes, config.productionOwnerContext(), moduleSpecifier);
        }
        if (config.indexHtmlJs().equals(moduleSpecifier)) {
            putProductionRuntimeAttributes(attributes, config);
        }
        return attributes;
    }

    static Map<String, Object> litDevBundle(Config config) {
        Map<String, Object> attributes = devBundle(config,
                "lit",
                "",
                config.textField().litEntryArtifact(),
                false,
                "Lit-css-dev-bundle-resource-v1_4",
                config.bigDecimalFieldJs());
        attributes.put("replacesImportSpecifiers", List.of("lit"));
        attributes.put("origin", "vaadin-dev-bundle-extracted-entry");
        attributes.put("resourceOrigin", "vaadin-dev-bundle-extracted-lit-css");
        attributes.put("blockedReason",
                "Focused Lit css helper is extracted from the production dev-bundle without executing the full indexhtml chunk; full npm frontend graph remains incomplete because TextField still uses a controlled shim.");
        return attributes;
    }

    static Map<String, Object> textFieldDevBundle(Config config) {
        FlowIpcComponentManifestAttributes.TextFieldContext textField = config.textField();
        Map<String, Object> attributes = devBundle(config,
                "com.vaadin.flow.component.textfield.TextField",
                "vaadin-text-field",
                textField.entryArtifact(),
                false,
                "TextField-dev-bundle-resource-v1_4",
                config.bigDecimalFieldJs());
        attributes.put("blockedImportSpecifiers", List.of("@vaadin/text-field/src/vaadin-text-field.js"));
        attributes.put("textfieldGraphParityVersion", textField.graphParityVersion());
        attributes.put("previousBlockerVersion", textField.previousBlockerVersion());
        attributes.put("executionVersion", textField.executionVersion());
        attributes.put("textfieldSideEffectVersion", textField.sideEffectVersion());
        attributes.put("graphProbe", textField.graphProbe());
        attributes.put("graphEntryArtifact", textField.entryArtifact());
        attributes.put("classExportArtifact", textField.classExportArtifact());
        attributes.put("transitiveImports", textField.transitiveArtifacts());
        attributes.put("importResolutionStrategy", textField.importResolutionStrategy());
        attributes.put("sideEffectStrategy", textField.sideEffectStrategy());
        attributes.put("narrowedFailureClassification", textField.narrowedFailureClassification());
        attributes.put("failureLayer", textField.failureLayer());
        attributes.put("minimalReproduction", textField.minimalReproduction());
        attributes.put("blockedExecutionProbe", "legacy-v1_6-big-decimal-import-rewrite-to-textfield-dev-bundle");
        attributes.put("blockedExecutionError", "resolved-by-stable-module-identity-v1_7");
        attributes.put("firstDefinitionSource", textField.firstDefinitionSource());
        attributes.put("secondDefinitionSource", textField.secondDefinitionSource());
        attributes.put("duplicateTag", textField.duplicateTag());
        attributes.put("remainingBlockerClassification", textField.remainingBlockerClassification());
        attributes.put("moduleIdentityStrategy", textField.importResolutionStrategy());
        attributes.put("legacyShimSuccessPath", false);
        attributes.put("nextResolutionStep", textField.nextResolutionStep());
        attributes.put("blockedReason",
                "Focused TextField production class entry is executed through stable resource-path Blob module identity so the production indexhtml dependency is reused instead of re-imported and re-registering vaadin-text-field.");
        return attributes;
    }

    static Map<String, Object> defineDevBundle(Config config) {
        Map<String, Object> attributes = devBundle(config,
                "@vaadin/component-base/src/define.js",
                "",
                config.textField().defineEntryArtifact(),
                false,
                "Component-base-define-dev-bundle-resource-v1_4",
                config.bigDecimalFieldJs());
        attributes.put("replacesImportSpecifiers", List.of("@vaadin/component-base/src/define.js"));
        attributes.put("origin", "vaadin-dev-bundle-extracted-entry");
        attributes.put("resourceOrigin", "vaadin-dev-bundle-extracted-define");
        return attributes;
    }

    static Map<String, Object> currentAppGeneratedImportsSupport(
            Config config, String path, String specifier, String lookupKind, boolean placeholder) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("componentFrontendResource", true);
        attributes.put("currentAppBuildFrontendSupportResource", true);
        attributes.put("currentAppGeneratedChunkPackageResolverVersion",
                config.currentAppGeneratedChunkPackageResolverVersion());
        attributes.put("currentAppGeneratedChunkPackageResolverSource",
                config.currentAppGeneratedChunkPackageResolverSource());
        attributes.put("currentAppGeneratedChunkPackageResolverMode",
                placeholder ? "diagnostic-placeholder-for-root-generated-imports"
                        : config.currentAppGeneratedChunkPackageResolverMode());
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
        attributes.put("dependencyOf", config.currentAppBuildFrontendGeneratedImportsJs());
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
                placeholder ? config.currentAppGeneratedChunkPackageFailureLayer() : "");
        attributes.put("currentAppGeneratedChunkPackageNextResolutionStep",
                placeholder ? config.currentAppGeneratedChunkPackageNextStep() : "");
        return attributes;
    }

    static Map<String, Object> productionBundleStats(Config config) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("componentFrontendResource", true);
        attributes.put("productionBundleResource", true);
        attributes.put("productionBundleStatsGraph", true);
        attributes.put("moduleSpecifier", config.statsJson());
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
        attributes.put("runtimeResolverContractVersion", config.runtimeResolverContractVersion());
        attributes.put("logicalRequestId", "production-runtime:stats:v1_10");
        attributes.put("pathMapping", "stats-json-to-runtime-artifact-registry");
        attributes.put("dependencyGraph", "bundleImports=>VAADIN/build/indexhtml-BfYYZi8S.js");
        attributes.putAll(FlowIpcProductionRuntimeAttributes.runtimeAttributes(
                config.statsJson(), config.indexHtmlJs()));
        attributes.putAll(FlowIpcProductionOwnerAttributes.ownerAttributes(config.productionOwnerContext()));
        return attributes;
    }

    private static void putProductionRuntimeAttributes(Map<String, Object> attributes, Config config) {
        attributes.put("runtimeResolverContractVersion", config.runtimeResolverContractVersion());
        attributes.put("logicalRequestId", "production-runtime:indexhtml:v1_10");
        attributes.put("pathMapping", "stats-bundle-import-to-dev-bundle-artifact");
        attributes.put("stageUrlPolicy", "stable-module-identity-blob-url");
        attributes.put("cachePolicy", "retain-per-resource-path");
        attributes.put("cleanupOwnership", "window-lifetime-module-identity-cache");
        attributes.put("blockerClassification", FlowIpcProductionRuntimeAttributes.BLOCKER);
        attributes.put("productionBundleRuntimeArtifact", true);
        attributes.putAll(FlowIpcProductionRuntimeAttributes.runtimeAttributes(
                config.statsJson(), config.indexHtmlJs()));
        attributes.put("productionBundleRuntimeExecutionPolicy",
                "stage-execution-attempt-production-bundle-runtime-v1_10");
        attributes.put("productionBundleRuntimeExpectedSideEffect",
                "register production custom element classes when owned by a production bundle runtime");
        attributes.put("productionBundleRuntimeCachePolicy",
                "reuse stable resource-path module identity to avoid duplicate custom element definitions");
        attributes.put("productionBundleRuntimeCleanupPolicy", "retain-for-window-lifetime-module-identity-v1_7");
        attributes.put("productionBundleRuntimeFailureLayer",
                "production-runtime-ownership-not-yet-separated-from-focused-component-entry-imports");
        attributes.putAll(FlowIpcProductionOwnerAttributes.ownerAttributes(config.productionOwnerContext()));
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

    record Config(
            String bigDecimalFieldJs,
            String devBundleBuildRoot,
            String checkboxEntryJs,
            String indexHtmlJs,
            String statsJson,
            String currentAppBuildFrontendGeneratedImportsJs,
            String currentAppGeneratedChunkPackageResolverVersion,
            String currentAppGeneratedChunkPackageResolverSource,
            String currentAppGeneratedChunkPackageResolverMode,
            String currentAppGeneratedChunkPackageFailureLayer,
            String currentAppGeneratedChunkPackageNextStep,
            String runtimeResolverContractVersion,
            String componentTransitiveBlockedReason,
            String devBundleComponentReason,
            FlowIpcComponentManifestAttributes.TextFieldContext textField,
            FlowIpcProductionOwnerAttributes.Context productionOwnerContext) {
    }
}
