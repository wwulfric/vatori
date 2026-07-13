package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class CurrentAppBuildFrontendTraceFields {
    boolean outputV132ManifestObserved;
    boolean outputV132RequestObserved;
    boolean outputV132ResponseObserved;
    boolean outputV132StageObserved;
    boolean outputV132DomObserved;
    boolean outputV132ResolvedObserved;
    boolean generatedChunkPackageEdgeV133ManifestObserved;
    boolean generatedChunkPackageEdgeV133RequestObserved;
    boolean generatedChunkPackageEdgeV133ResponseObserved;
    boolean generatedChunkPackageEdgeV133StageObserved;
    boolean generatedChunkPackageEdgeV133DomObserved;
    boolean generatedChunkPackageEdgeV133ExecutedObserved;
    boolean generatedImportsRootPackageGraphV134ManifestObserved;
    boolean generatedImportsRootPackageGraphV134RequestObserved;
    boolean generatedImportsRootPackageGraphV134ResponseObserved;
    boolean generatedImportsRootPackageGraphV134StageObserved;
    boolean generatedImportsRootPackageGraphV134SupportExecutedObserved;
    boolean generatedImportsRootPackageGraphV134DomObserved;

    boolean hasOutputV1_32Signals() {
        return outputV132ManifestObserved
                && outputV132RequestObserved
                && outputV132ResponseObserved
                && outputV132StageObserved
                && outputV132DomObserved
                && outputV132ResolvedObserved;
    }

    boolean hasGeneratedChunkPackageEdgeV1_33Signals() {
        return generatedChunkPackageEdgeV133ManifestObserved
                && generatedChunkPackageEdgeV133RequestObserved
                && generatedChunkPackageEdgeV133ResponseObserved
                && generatedChunkPackageEdgeV133StageObserved
                && generatedChunkPackageEdgeV133DomObserved
                && generatedChunkPackageEdgeV133ExecutedObserved;
    }

    boolean hasGeneratedImportsRootPackageGraphV1_34Signals() {
        return generatedImportsRootPackageGraphV134ManifestObserved
                && generatedImportsRootPackageGraphV134RequestObserved
                && generatedImportsRootPackageGraphV134ResponseObserved
                && generatedImportsRootPackageGraphV134StageObserved
                && generatedImportsRootPackageGraphV134SupportExecutedObserved
                && generatedImportsRootPackageGraphV134DomObserved;
    }

    static boolean containsCurrentAppBuildFrontendOutputV132Fields(String line) {
        return containsStringField(line, "currentAppBuildFrontendOutputVersion",
                "flow-ipc-current-app-build-frontend-output-v1_32")
                && containsStringField(line, "currentAppBuildFrontendOutputSource",
                        "dev-ipc-current-app-build-frontend-output-contract-v1_32")
                && containsStringField(line, "currentAppBuildFrontendMavenGoal", "build-frontend")
                && containsCurrentAppBuildFrontendOutputUsability(line)
                && containsLiteralField(line, "currentAppBuildFrontendSourceGeneratedImportsObserved", "true")
                && line.contains("currentAppBuildFrontendSourceGeneratedImportsPath")
                && line.contains("currentAppBuildFrontendSourceGeneratedImportsSha256")
                && containsStringField(line, "currentAppBuildFrontendSourceGeneratedImportsTableShape",
                        "branch-table-dynamic-import")
                && line.contains("currentAppBuildFrontendSourceChunkId")
                && line.contains("currentAppBuildFrontendSourceChunkImportSpecifier")
                && line.contains("currentAppBuildFrontendSourceChunkArtifact")
                && line.contains("currentAppBuildFrontendSourceChunkVirtualPath")
                && containsStringField(line, "currentAppBuildFrontendSourceChunkStatus", "available")
                && line.contains("currentAppBuildFrontendSourceChunkBytes")
                && line.contains("currentAppBuildFrontendSourceChunkSha256")
                && line.contains("currentAppBuildFrontendSourceChunkContainsBareImports")
                && containsLiteralField(line, "currentAppBuildFrontendProductionBuildObserved", "true")
                && line.contains("currentAppBuildFrontendProductionBuildRoot")
                && containsLiteralField(line,
                        "currentAppBuildFrontendProductionGeneratedImportsObserved", "true")
                && line.contains("currentAppBuildFrontendProductionGeneratedImportsPath")
                && containsLiteralField(line, "currentAppBuildFrontendProdBundleObserved", "true")
                && line.contains("currentAppBuildFrontendProdBundlePath")
                && containsStringField(line, "currentAppBuildFrontendRuntimeCandidate",
                        "source-generated-imports")
                && containsCurrentAppBuildFrontendFailureLayer(line)
                && line.contains("currentAppBuildFrontendNextResolutionStep")
                && containsLiteralField(line, "currentAppBuildFrontendNoLocalhost", "true")
                && containsLiteralField(line, "currentAppBuildFrontendFixtureSubstituted", "false");
    }

    static boolean containsCurrentAppBuildFrontendOutputUsability(String line) {
        return containsStringField(line, "currentAppBuildFrontendOutputUsability",
                "source-generated-imports-and-chunk")
                || containsStringField(line, "currentAppBuildFrontendOutputUsability",
                        "source-generated-imports-no-chunk")
                || containsStringField(line, "currentAppBuildFrontendOutputUsability",
                        "production-generated-imports-bundled")
                || containsStringField(line, "currentAppBuildFrontendOutputUsability",
                        "production-bundle-only");
    }

    static boolean containsCurrentAppBuildFrontendFailureLayer(String line) {
        return containsStringField(line, "currentAppBuildFrontendFailureLayer",
                "current-app-source-generated-chunk-npm-specifier-unresolved")
                || containsStringField(line, "currentAppBuildFrontendFailureLayer",
                        "pending-current-app-source-generated-chunk-stage")
                || containsStringField(line, "currentAppBuildFrontendFailureLayer",
                        "current-app-production-generated-imports-owner-boundary")
                || containsStringField(line, "currentAppBuildFrontendFailureLayer",
                        "current-app-source-generated-imports-root-package-graph-pending")
                || containsStringField(line, "currentAppBuildFrontendFailureLayer",
                        "current-app-generated-imports-root-package-graph-resolved-production-owner-pending");
    }

    static boolean containsCurrentAppGeneratedChunkPackageEdgeV133Fields(String line) {
        return containsCurrentAppBuildFrontendOutputV132Fields(line)
                && containsStringField(line, "currentAppGeneratedChunkPackageResolverVersion",
                        "flow-ipc-current-app-generated-chunk-package-resolver-v1_33")
                && containsStringField(line, "currentAppGeneratedChunkPackageResolverSource",
                        "current-app-source-generated-chunk")
                && containsStringField(line, "currentAppGeneratedChunkPackageResolverMode",
                        "current-app-source-chunk-package-edge-to-production-indexhtml")
                && containsStringField(line, "currentAppGeneratedChunkPackageBareSpecifiers",
                        "@vaadin/vertical-layout/src/vaadin-vertical-layout.js")
                && containsStringField(line, "currentAppGeneratedChunkPackageResolvedSpecifiers",
                        "@vaadin/vertical-layout/src/vaadin-vertical-layout.js")
                && containsStringField(line, "currentAppGeneratedChunkPackageUnresolvedSpecifiers", "")
                && containsCurrentAppGeneratedChunkPackageFailureLayer(line)
                && line.contains("currentAppGeneratedChunkPackageNextResolutionStep");
    }

    static boolean containsCurrentAppGeneratedChunkPackageFailureLayer(String line) {
        return containsStringField(line, "currentAppGeneratedChunkPackageFailureLayer",
                "current-app-source-generated-imports-root-package-graph-pending")
                || containsStringField(line, "currentAppGeneratedChunkPackageFailureLayer",
                        "current-app-generated-imports-root-package-graph-resolved-production-owner-pending");
    }

    static boolean containsCurrentAppGeneratedImportsRootPackageGraphV134Fields(String line) {
        return containsCurrentAppGeneratedChunkPackageEdgeV133Fields(line)
                && containsStringField(line, "currentAppGeneratedImportsRootPackageGraphVersion",
                        "flow-ipc-current-app-generated-imports-root-package-graph-v1_34")
                && containsStringField(line, "currentAppGeneratedImportsRootPackageGraphSource",
                        "current-app-source-generated-imports-root")
                && containsStringField(line, "currentAppGeneratedImportsRootPackageResolverMode",
                        "current-app-source-generated-imports-root-to-current-app-production-graph")
                && line.contains("currentAppGeneratedImportsRootBareSpecifiers")
                && line.contains("@vaadin/common-frontend/ConnectionIndicator.js")
                && line.contains("currentAppGeneratedImportsRootStatsBundleImports")
                && line.contains("currentAppGeneratedImportsRootResolvedSpecifiers")
                && containsStringField(line, "currentAppGeneratedImportsRootUnresolvedSpecifiers", "")
                && containsStringFieldPrefix(line,
                        "currentAppGeneratedImportsRootProductionSupportArtifact",
                        "current-app-production/VAADIN/build/generated-flow-imports-")
                && containsLiteralField(line,
                        "currentAppGeneratedImportsRootProductionSupportObserved", "true")
                && containsLiteralField(line, "currentAppGeneratedImportsRootPlaceholderUsed", "false")
                && containsStringField(line, "currentAppGeneratedImportsRootFailureLayer",
                        "current-app-generated-imports-root-package-graph-resolved-production-owner-pending")
                && line.contains("currentAppGeneratedImportsRootNextResolutionStep");
    }
}
