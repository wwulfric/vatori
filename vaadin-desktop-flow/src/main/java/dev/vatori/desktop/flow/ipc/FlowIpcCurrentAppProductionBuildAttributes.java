package dev.vatori.desktop.flow.ipc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class FlowIpcCurrentAppProductionBuildAttributes {
    private FlowIpcCurrentAppProductionBuildAttributes() {
    }

    static Map<String, Object> attributes(
            CurrentAppBuildFrontendOutputEvidence evidence,
            String virtualPath,
            boolean loginFormPrimaryResource,
            Config config,
            Map<String, Object> generatedImportsRootAttributes,
            Map<String, Object> buildFrontendOutputAttributes) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("componentFrontendResource", true);
        attributes.put("currentAppBuildFrontendResource", true);
        attributes.put("currentAppProductionBuildResource", true);
        attributes.put("currentAppProductionBuildResourceRole",
                resourceRole(evidence, virtualPath));
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
        attributes.put("blockerVersion", config.blockerVersion());
        attributes.put("failureLayer", evidence.sourceGeneratedImportsRootFailureLayer());
        attributes.put("nextResolutionStep", config.nextResolutionStep());
        attributes.put("importRewriteStrategy",
                "current-app-production-build-relative-import-rewrite-v1_34");
        attributes.put("browserExecutionPlan",
                "stage-current-app-production-build-module-with-relative-rewrites-v1_34");
        attributes.put("importRewrites", importRewrites(evidence, virtualPath));
        attributes.put("transitiveImports", transitiveImports(evidence, virtualPath));
        if (loginFormPrimaryResource) {
            putLoginFormAttributes(attributes, config, virtualPath);
        }
        attributes.putAll(generatedImportsRootAttributes);
        attributes.putAll(buildFrontendOutputAttributes);
        return attributes;
    }

    private static String resourceRole(CurrentAppBuildFrontendOutputEvidence evidence, String virtualPath) {
        if (virtualPath.equals(evidence.sourceGeneratedImportsRootProductionSupportArtifact())) {
            return "generated-imports-root-support";
        }
        if (virtualPath.equals(evidence.productionEntryScriptVirtualPath())) {
            return "production-entry-script";
        }
        return "production-relative-dependency";
    }

    private static void putLoginFormAttributes(
            Map<String, Object> attributes,
            Config config,
            String virtualPath) {
        attributes.put("componentPrimary", true);
        attributes.put("componentClass", config.loginFormComponentClass());
        attributes.put("componentTag", config.loginFormTag());
        attributes.put("requestedModule", config.loginFormRequestedModule());
        attributes.put("resourceOrigin", "current-app-production-webapp-build-login-form");
        attributes.put("executionPolicy", "execute-current-app-login-form-production-build-v1_101");
        attributes.put("lookupKind", "LoginForm-current-app-production-build-resource-v1_101");
        attributes.put("loginFormProductionBuildResourceVersion",
                config.loginFormProductionBuildResourceVersion());
        attributes.put("loginFormProductionBuildResourceArtifact", virtualPath);
        attributes.put("loginFormProductionBuildResourceNoLocalhost", true);
        attributes.put("loginFormProductionBuildResourceFixtureSubstituted", false);
    }

    private static List<Map<String, Object>> importRewrites(
            CurrentAppBuildFrontendOutputEvidence evidence, String currentPath) {
        List<Map<String, Object>> rewrites = new ArrayList<>();
        for (String path : FlowIpcJavaScriptModuleGraph.csvValues(evidence.productionBuildJsVirtualPaths())) {
            if (path.equals(currentPath)) {
                continue;
            }
            if (currentPath.equals(evidence.productionEntryScriptVirtualPath())
                    && path.equals(evidence.sourceGeneratedImportsRootProductionSupportArtifact())) {
                continue;
            }
            rewrites.add(importRewrite("./" + FlowIpcBuildOutputScanner.fileName(path),
                    path,
                    "current-app-production-build",
                    "current-app-production-build-relative-import-rewrite-v1_34",
                    "current-app-production-build-relative-dependency-v1_34"));
        }
        return rewrites;
    }

    private static List<String> transitiveImports(
            CurrentAppBuildFrontendOutputEvidence evidence, String currentPath) {
        List<String> imports = new ArrayList<>();
        for (String path : FlowIpcJavaScriptModuleGraph.csvValues(evidence.productionBuildJsVirtualPaths())) {
            if (!path.equals(currentPath)) {
                imports.add(path);
            }
        }
        return imports;
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
            String blockerVersion,
            String nextResolutionStep,
            String loginFormComponentClass,
            String loginFormTag,
            String loginFormRequestedModule,
            String loginFormProductionBuildResourceVersion) {
    }
}
