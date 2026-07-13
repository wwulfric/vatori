package dev.vatori.desktop.devtools;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TraceSummary {
    int total;
    final Map<String, Integer> types = new LinkedHashMap<>();
    final Map<String, Integer> directions = new LinkedHashMap<>();
    final CoreTraceFields core = new CoreTraceFields();
    final CoreCapabilityTraceFields coreCapabilities = new CoreCapabilityTraceFields();
    final FlowClientTraceFields flowClient = new FlowClientTraceFields();
    final VaadinComponentTraceFields vaadinComponents = new VaadinComponentTraceFields();
    final FrameworkAppTraceFields frameworkApp = new FrameworkAppTraceFields();
    final ThemeMatrixTraceFields themeMatrix = new ThemeMatrixTraceFields();
    final AuthenticationTraceFields authentication = new AuthenticationTraceFields();
    final FrameworkComponentMatrixTraceFields frameworkComponentMatrix =
            new FrameworkComponentMatrixTraceFields();
    final FrameworkInputComponentMatrixTraceFields frameworkInputComponentMatrix =
            new FrameworkInputComponentMatrixTraceFields();
    final FrameworkNavigationComponentMatrixTraceFields frameworkNavigationComponentMatrix =
            new FrameworkNavigationComponentMatrixTraceFields();
    final FrameworkOverlayComponentMatrixTraceFields frameworkOverlayComponentMatrix =
            new FrameworkOverlayComponentMatrixTraceFields();
    final FrameworkContentAppShellLoginMatrixTraceFields frameworkContentAppShellLoginMatrix =
            new FrameworkContentAppShellLoginMatrixTraceFields();
    final FrameworkLifecycleFocusBehaviorMatrixTraceFields frameworkLifecycleFocusBehaviorMatrix =
            new FrameworkLifecycleFocusBehaviorMatrixTraceFields();
    final FrameworkRichTextEditorTraceFields frameworkRichTextEditor =
            new FrameworkRichTextEditorTraceFields();
    final GridResourceTraceFields gridResources = new GridResourceTraceFields();
    final StreamResourceTraceFields streamResources = new StreamResourceTraceFields();
    final UploadTraceFields uploads = new UploadTraceFields();
    final DownloadTraceFields downloads = new DownloadTraceFields();
    final FrontendResourceClosureTraceFields frontendResourceClosure =
            new FrontendResourceClosureTraceFields();
    final FrontendBuildPipelineTraceFields frontendBuildPipeline =
            new FrontendBuildPipelineTraceFields();
    final ProductionOwnerRuntimeTraceFields productionOwnerRuntime =
            new ProductionOwnerRuntimeTraceFields();
    final GeneratedLazyBuildTraceFields generatedLazyBuild =
            new GeneratedLazyBuildTraceFields();
    final FrontendBuildPreflightTraceFields frontendBuildPreflight =
            new FrontendBuildPreflightTraceFields();
    final CurrentAppBuildFrontendTraceFields currentAppBuildFrontend =
            new CurrentAppBuildFrontendTraceFields();
    final FrontendBuildCurrentAppProductionOwnerTraceFields currentAppProductionOwner =
            new FrontendBuildCurrentAppProductionOwnerTraceFields();
    final FrontendBuildCurrentAppOwnerComponentReuseTraceFields currentAppOwnerComponentReuse =
            new FrontendBuildCurrentAppOwnerComponentReuseTraceFields();
    final FrontendBuildCurrentAppOwnerEarlyImportTraceFields currentAppOwnerEarlyImport =
            new FrontendBuildCurrentAppOwnerEarlyImportTraceFields();
    final FrontendBuildGeneratedImportsOwnerTraceFields generatedImportsOwner =
            new FrontendBuildGeneratedImportsOwnerTraceFields();
    final FrontendBuildGeneratedLazySpiDecisionTraceFields generatedLazySpiDecision =
            new FrontendBuildGeneratedLazySpiDecisionTraceFields();
    final FrontendBuildPackagingTraceFields packaging =
            new FrontendBuildPackagingTraceFields();
    final FrontendBuildComponentResourceTraceFields componentResources =
            new FrontendBuildComponentResourceTraceFields();
}
