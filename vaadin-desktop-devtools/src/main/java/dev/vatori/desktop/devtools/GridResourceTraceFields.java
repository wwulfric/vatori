package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class GridResourceTraceFields {
    boolean lazyDataProviderV148ManifestObserved;
    boolean lazyDataProviderV148StageObserved;
    boolean lazyDataProviderV148CompleteStageObserved;
    boolean lazyDataProviderV148DomObserved;
    boolean scrollVirtualizationV149ManifestObserved;
    boolean scrollVirtualizationV149StageObserved;
    boolean scrollVirtualizationV149CompleteStageObserved;
    boolean scrollVirtualizationV149DomObserved;
    boolean lazyBootstrapObserved;
    boolean lazyPublishedRpcObserved;
    boolean lazyReportEventRpcObserved;
    boolean lazyFetchPatchObserved;
    boolean lazyReportPatchObserved;
    boolean lazyDomObserved;
    String lazyFetchPatchRange = "";
    boolean scrollBootstrapObserved;
    boolean scrollPublishedRpcObserved;
    boolean scrollReportEventRpcObserved;
    boolean scrollFetchPatchObserved;
    boolean scrollReportPatchObserved;
    boolean scrollDomObserved;

    boolean hasLazyDataProviderSignals() {
        return lazyBootstrapObserved
                && lazyPublishedRpcObserved
                && lazyReportEventRpcObserved
                && lazyFetchPatchObserved
                && lazyReportPatchObserved
                && lazyDomObserved;
    }

    boolean hasLazyDataProviderGateSignals() {
        return lazyDataProviderV148ManifestObserved
                && lazyDataProviderV148StageObserved
                && lazyDataProviderV148CompleteStageObserved
                && lazyDataProviderV148DomObserved;
    }

    boolean hasScrollVirtualizationSignals(boolean gateDomObserved) {
        boolean successPath = scrollPublishedRpcObserved
                && scrollReportEventRpcObserved
                && scrollFetchPatchObserved
                && scrollReportPatchObserved;
        boolean acceptedBlockerPath = gateDomObserved && !successPath;
        return scrollBootstrapObserved
                && scrollDomObserved
                && (successPath || acceptedBlockerPath);
    }

    boolean hasScrollVirtualizationGateSignals() {
        return scrollVirtualizationV149ManifestObserved
                && scrollVirtualizationV149StageObserved
                && scrollVirtualizationV149CompleteStageObserved
                && scrollVirtualizationV149DomObserved;
    }

    static boolean containsCurrentAppProductionMatrixRuntimePath(String line) {
        return containsPath(line, "current-app-production/VAADIN/build/generated-flow-imports-")
                || containsPath(line, "current-app-production/VAADIN/build/chunk-")
                || containsPath(line, "current-app-production/VAADIN/build/indexhtml-");
    }

    static boolean containsGridLazyDataProviderV148Fields(String line) {
        return containsStringField(line, "gridLazyDataProviderVersion",
                        "flow-ipc-grid-lazy-data-provider-v1_48")
                && containsStringField(line, "gridLazyDataProviderSource",
                        "data-component-resource-matrix-v1_47")
                && containsStringField(line, "gridLazyDataProviderScope",
                        "framework-validation-app-grid-callback-data-provider-viewport-rpc")
                && containsStringField(line, "gridLazyDataProviderAppShape",
                        "framework-validation-app")
                && containsStringField(line, "gridLazyDataProviderTargetTags",
                        "vaadin-grid,vaadin-grid-column")
                && containsStringField(line, "gridLazyDataProviderJavaClasses",
                        "com.vaadin.flow.component.grid.Grid,com.vaadin.flow.data.provider.CallbackDataProvider,com.vaadin.flow.data.provider.DataCommunicator")
                && containsStringField(line, "gridLazyDataProviderComponentIds",
                        "framework-lazy-grid,framework-lazy-grid-status,framework-lazy-grid-report")
                && containsStringField(line, "gridLazyDataProviderModuleSpecifiers",
                        "@vaadin/grid/src/vaadin-grid.js")
                && containsStringField(line, "gridLazyDataProviderPublishedMethod", "setViewportRange")
                && containsStringField(line, "gridLazyDataProviderViewportRange", "20:8")
                && containsStringField(line, "gridLazyDataProviderExpectedResult",
                        "callback-backed-grid-fetch-normalized-range-15-20-after-viewport-20-8")
                && containsStringFieldPrefix(line, "gridLazyDataProviderSelectedArtifacts",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && containsPositiveIntegerField(line, "gridLazyDataProviderSelectedArtifactCount")
                && containsStringField(line, "gridLazyDataProviderGeneratedImportsOwnerSource",
                        "current-app-generated-imports-select-owner-coverage-v1_41")
                && containsStringField(line, "gridLazyDataProviderGeneratedImportsOwnerArtifact",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && containsStringFieldPrefix(line,
                        "gridLazyDataProviderProductionSupportArtifact",
                        "current-app-production/VAADIN/build/generated-flow-imports-")
                && line.contains("gridLazyDataProviderProductionSupportSha256")
                && containsPositiveIntegerField(line,
                        "gridLazyDataProviderProductionSupportBytes")
                && line.contains("gridLazyDataProviderProductionEntryArtifact")
                && containsStringFieldPrefix(line, "gridLazyDataProviderSourceChunkArtifact",
                        "current-app-frontend/generated/flow/chunks/chunk-")
                && containsStringField(line, "gridLazyDataProviderSourceChunkStatus",
                        "available")
                && line.contains("gridLazyDataProviderResolvedSpecifiers")
                && line.contains("gridLazyDataProviderUnresolvedSpecifiers")
                && containsStringField(line, "gridLazyDataProviderAllowedSchemes",
                        "vatori-resource,blob")
                && containsStringField(line, "gridLazyDataProviderIntegrityPolicy",
                        "sha256-and-byte-count-for-selected-artifacts")
                && containsStringField(line, "gridLazyDataProviderMavenGoal",
                        "build-frontend")
                && containsStringField(line, "gridLazyDataProviderBuildStatus", "success")
                && line.contains("gridLazyDataProviderOutputUsability")
                && containsLiteralField(line, "gridLazyDataProviderFrameworkAppRequired",
                        "true")
                && containsLiteralField(line, "gridLazyDataProviderFrameworkAppObserved",
                        "true")
                && containsLiteralField(line, "gridLazyDataProviderCliContractEnabled",
                        "true")
                && containsLiteralField(line, "gridLazyDataProviderManifestReady", "true")
                && containsLiteralField(line, "gridLazyDataProviderRuntimeHandoffRequired",
                        "true")
                && containsLiteralField(line, "gridLazyDataProviderRuntimeHandoffReady",
                        "true")
                && containsLiteralField(line, "gridLazyDataProviderNoLocalhost", "true")
                && containsLiteralField(line, "gridLazyDataProviderFixtureSubstituted",
                        "false")
                && line.contains("gridLazyDataProviderAcceptableFailures")
                && line.contains("gridLazyDataProviderRemainingBlockers")
                && line.contains("gridLazyDataProviderNextResolutionStep");
    }

    static boolean containsGridLazyDataProviderV148Complete(String line) {
        return containsGridLazyDataProviderV148Fields(line)
                && containsLiteralField(line,
                        "gridLazyDataProviderRuntimeHandoffSatisfied", "true")
                && containsLiteralField(line, "gridLazyDataProviderStaged", "true")
                && containsLiteralField(line,
                        "gridLazyDataProviderCustomSchemeObserved", "true")
                && containsLiteralField(line,
                        "gridLazyDataProviderNoLocalhost", "true");
    }

    static boolean containsGridScrollVirtualizationV149Fields(String line) {
        return containsStringField(line, "gridScrollVirtualizationVersion",
                        "flow-ipc-grid-scroll-virtualization-v1_49")
                && containsStringField(line, "gridScrollVirtualizationSource",
                        "grid-lazy-data-provider-v1_48")
                && containsStringField(line, "gridScrollVirtualizationScope",
                        "framework-validation-app-grid-client-scroll-api-probe")
                && containsStringField(line, "gridScrollVirtualizationAppShape",
                        "framework-validation-app")
                && containsStringField(line, "gridScrollVirtualizationTargetTags",
                        "vaadin-grid,vaadin-grid-column")
                && containsStringField(line, "gridScrollVirtualizationJavaClasses",
                        "com.vaadin.flow.component.grid.Grid,com.vaadin.flow.data.provider.CallbackDataProvider,com.vaadin.flow.data.provider.DataCommunicator")
                && containsStringField(line, "gridScrollVirtualizationComponentIds",
                        "framework-lazy-grid,framework-lazy-grid-status,framework-lazy-grid-report")
                && containsStringField(line, "gridScrollVirtualizationModuleSpecifiers",
                        "@vaadin/grid/src/vaadin-grid.js")
                && containsStringField(line, "gridScrollVirtualizationClientApi",
                        "scrollToIndex")
                && containsPositiveIntegerField(line, "gridScrollVirtualizationRequestedIndex")
                && containsStringField(line, "gridScrollVirtualizationExpectedResult",
                        "client-scroll-api-triggers-viewport-rpc-and-lazy-fetch-near-index-45")
                && containsStringFieldPrefix(line, "gridScrollVirtualizationSelectedArtifacts",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && containsPositiveIntegerField(line,
                        "gridScrollVirtualizationSelectedArtifactCount")
                && containsStringField(line, "gridScrollVirtualizationGeneratedImportsOwnerSource",
                        "current-app-generated-imports-select-owner-coverage-v1_41")
                && containsStringField(line,
                        "gridScrollVirtualizationGeneratedImportsOwnerArtifact",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && containsStringFieldPrefix(line,
                        "gridScrollVirtualizationProductionSupportArtifact",
                        "current-app-production/VAADIN/build/generated-flow-imports-")
                && line.contains("gridScrollVirtualizationProductionSupportSha256")
                && containsPositiveIntegerField(line,
                        "gridScrollVirtualizationProductionSupportBytes")
                && line.contains("gridScrollVirtualizationProductionEntryArtifact")
                && containsStringFieldPrefix(line, "gridScrollVirtualizationSourceChunkArtifact",
                        "current-app-frontend/generated/flow/chunks/chunk-")
                && containsStringField(line, "gridScrollVirtualizationSourceChunkStatus",
                        "available")
                && line.contains("gridScrollVirtualizationResolvedSpecifiers")
                && line.contains("gridScrollVirtualizationUnresolvedSpecifiers")
                && containsStringField(line, "gridScrollVirtualizationAllowedSchemes",
                        "vatori-resource,blob")
                && containsStringField(line, "gridScrollVirtualizationIntegrityPolicy",
                        "sha256-and-byte-count-for-selected-artifacts")
                && containsStringField(line, "gridScrollVirtualizationMavenGoal",
                        "build-frontend")
                && containsStringField(line, "gridScrollVirtualizationBuildStatus", "success")
                && line.contains("gridScrollVirtualizationOutputUsability")
                && containsLiteralField(line, "gridScrollVirtualizationFrameworkAppRequired",
                        "true")
                && containsLiteralField(line, "gridScrollVirtualizationFrameworkAppObserved",
                        "true")
                && containsLiteralField(line, "gridScrollVirtualizationCliContractEnabled",
                        "true")
                && containsLiteralField(line, "gridScrollVirtualizationManifestReady", "true")
                && containsLiteralField(line, "gridScrollVirtualizationRuntimeHandoffRequired",
                        "true")
                && containsLiteralField(line, "gridScrollVirtualizationRuntimeHandoffReady",
                        "true")
                && containsLiteralField(line, "gridScrollVirtualizationNoLocalhost", "true")
                && containsLiteralField(line, "gridScrollVirtualizationFixtureSubstituted",
                        "false")
                && line.contains("gridScrollVirtualizationAcceptableFailures")
                && line.contains("gridScrollVirtualizationRemainingBlockers")
                && line.contains("gridScrollVirtualizationNextResolutionStep");
    }

    static boolean containsGridScrollVirtualizationV149Complete(String line) {
        return containsGridScrollVirtualizationV149Fields(line)
                && containsLiteralField(line,
                        "gridScrollVirtualizationRuntimeHandoffSatisfied", "true")
                && containsLiteralField(line, "gridScrollVirtualizationStaged", "true")
                && containsLiteralField(line,
                        "gridScrollVirtualizationCustomSchemeObserved", "true")
                && containsLiteralField(line,
                        "gridScrollVirtualizationNoLocalhost", "true");
    }
}
