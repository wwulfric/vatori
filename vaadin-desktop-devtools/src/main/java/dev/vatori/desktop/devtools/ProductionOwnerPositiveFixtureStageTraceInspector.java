package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.ResourceStageTraceFields.*;
import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class ProductionOwnerPositiveFixtureStageTraceInspector {
    private ProductionOwnerPositiveFixtureStageTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (isResourceStage(line)
                && line.contains("\"artifactFamily\":\"production-bundle\"")
                && containsPath(line, "META-INF/vatori/production-owner/owner-runtime-v1_23.js")
                && containsLiteralField(line, "productionBundleRuntimeOwnerPositiveFixture", "true")
                && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureRole", "root")
                && containsLiteralField(line, "productionBundleRuntimeOwnerPositiveFixtureStageObserved",
                        "true")
                && containsLiteralField(line, "productionBundleRuntimeOwnerPositiveFixtureImported",
                        "true")
                && containsLiteralField(line,
                        "productionBundleRuntimeOwnerPositiveFixtureSideEffectSatisfied", "true")
                && containsLiteralField(line,
                        "productionBundleRuntimeOwnerPositiveFixtureOwnerOwnedRuntimeSatisfied", "true")
                && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureVersion",
                        "flow-ipc-production-owner-positive-runtime-v1_23")
                && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureSource",
                        "repository-production-owner-positive-fixture-v1_23")
                && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureExecutionPolicy",
                        "owner-first-custom-scheme-positive-fixture-v1_23")
                && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureExpectedTags",
                        "vatori-production-owner-fixture")
                && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureExpectedGlobals",
                        "window.VaadinDesktopProductionOwnerV123")
                && containsLiteralField(line, "productionBundleRuntimeOwnerPositiveFixtureNoBlobFallback",
                        "true")
                && containsStringFieldPrefix(line,
                        "productionBundleRuntimeOwnerPositiveFixtureRootCustomSchemeUrl",
                        "vatori-resource://flow-ipc/production-owner/")
                && containsStringFieldPrefix(line,
                        "productionBundleRuntimeOwnerPositiveFixtureDependencyCustomSchemeUrl",
                        "vatori-resource://flow-ipc/production-owner/")
                && containsStringFieldPrefix(line,
                        "productionBundleRuntimeOwnerPositiveFixtureExecutionResult", "executed:")
                && containsStringField(line,
                        "productionBundleRuntimeOwnerPositiveFixtureComponentReuseDecision",
                        "owner-reused-positive-fixture")
                && containsStringField(line, "stagedUrlScheme", "vatori-resource")
                && containsStringField(line, "executionStatus", "executed")) {
            summary.productionOwnerRuntime.positiveFixtureV123RootStageObserved = true;
        }
        if (isResourceStage(line)
                && line.contains("\"artifactFamily\":\"production-bundle\"")
                && containsPath(line, "META-INF/vatori/production-owner/owner-dependency-v1_23.js")
                && containsLiteralField(line, "productionBundleRuntimeOwnerPositiveFixture", "true")
                && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureRole", "dependency")
                && containsLiteralField(line, "productionBundleRuntimeOwnerPositiveFixtureStageObserved",
                        "true")
                && containsLiteralField(line, "productionBundleRuntimeOwnerPositiveFixtureImported",
                        "true")
                && containsLiteralField(line,
                        "productionBundleRuntimeOwnerPositiveFixtureSideEffectSatisfied", "true")
                && containsLiteralField(line,
                        "productionBundleRuntimeOwnerPositiveFixtureOwnerOwnedRuntimeSatisfied", "true")
                && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureVersion",
                        "flow-ipc-production-owner-positive-runtime-v1_23")
                && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureSource",
                        "repository-production-owner-positive-fixture-v1_23")
                && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureExecutionPolicy",
                        "owner-first-custom-scheme-positive-fixture-v1_23")
                && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureExpectedTags",
                        "vatori-production-owner-fixture")
                && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureExpectedGlobals",
                        "window.VaadinDesktopProductionOwnerV123")
                && containsLiteralField(line, "productionBundleRuntimeOwnerPositiveFixtureNoBlobFallback",
                        "true")
                && containsStringFieldPrefix(line,
                        "productionBundleRuntimeOwnerPositiveFixtureDependencyCustomSchemeUrl",
                        "vatori-resource://flow-ipc/production-owner/")
                && containsStringFieldPrefix(line,
                        "productionBundleRuntimeOwnerPositiveFixtureExecutionResult", "executed:")
                && containsStringField(line,
                        "productionBundleRuntimeOwnerPositiveFixtureComponentReuseDecision",
                        "owner-reused-positive-fixture")
                && containsStringField(line, "stagedUrlScheme", "vatori-resource")
                && containsStringField(line, "executionStatus", "executed")) {
            summary.productionOwnerRuntime.positiveFixtureV123DependencyStageObserved = true;
        }
    }
}
