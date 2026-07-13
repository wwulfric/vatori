package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.FrontendBuildComponentResourceTraceFields.*;
import static dev.vatori.desktop.devtools.FrontendBuildCurrentAppOwnerTraceFields.*;
import static dev.vatori.desktop.devtools.FrontendBuildGeneratedImportsOwnerTraceFields.*;
import static dev.vatori.desktop.devtools.FrontendBuildPackagingTraceFields.*;
import static dev.vatori.desktop.devtools.FrameworkContentAppShellLoginMatrixTraceFields.*;
import static dev.vatori.desktop.devtools.FrameworkInputComponentMatrixTraceFields.*;
import static dev.vatori.desktop.devtools.FrameworkNavigationComponentMatrixTraceFields.*;
import static dev.vatori.desktop.devtools.FrameworkOverlayComponentMatrixTraceFields.*;
import static dev.vatori.desktop.devtools.GridResourceTraceFields.*;
import static dev.vatori.desktop.devtools.StreamResourceTraceFields.*;
import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class ComponentResourceMatrixStageTraceInspector {
    private ComponentResourceMatrixStageTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("\"type\":\"resourceStage\"")
                && containsPath(line, "current-app-production/VAADIN/build/generated-flow-imports-")
                && containsBroaderComponentResourceMatrixV146Fields(line)
                && containsKnownStageStatus(line)) {
            summary.componentResources.broaderMatrixV146StageObserved = true;
        }
        if (line.contains("\"type\":\"resourceStage\"")
                && containsPath(line, "current-app-production/VAADIN/build/generated-flow-imports-")
                && containsDataComponentResourceMatrixV147Fields(line)
                && containsKnownStageStatus(line)) {
            summary.componentResources.dataMatrixV147StageObserved = true;
        }
        if (line.contains("\"type\":\"resourceStage\"")
                && containsPath(line, "current-app-production/VAADIN/build/generated-flow-imports-")
                && containsInputComponentParityMatrixV157Fields(line)
                && containsKnownStageStatus(line)) {
            summary.frameworkInputComponentMatrix.inputParityV157StageObserved = true;
        }
        if (line.contains("\"type\":\"resourceStage\"")
                && containsPath(line, "current-app-production/VAADIN/build/generated-flow-imports-")
                && containsMultiValueNavigationDisclosureMatrixV158Fields(line)
                && containsKnownStageStatus(line)) {
            summary.frameworkNavigationComponentMatrix.multiValueDisclosureV158StageObserved = true;
        }
        if (line.contains("\"type\":\"resourceStage\"")
                && containsPath(line, "current-app-production/VAADIN/build/generated-flow-imports-")
                && containsOverlayContextMenuMatrixV159Fields(line)
                && containsKnownStageStatus(line)) {
            summary.frameworkOverlayComponentMatrix.contextMenuMatrixV159StageObserved = true;
        }
        if (line.contains("\"type\":\"resourceStage\"")
                && containsCurrentAppProductionMatrixRuntimePath(line)
                && containsContentAppShellLoginMatrixV160Fields(line)
                && containsKnownStageStatus(line)) {
            summary.frameworkContentAppShellLoginMatrix.appShellLoginV160StageObserved = true;
        }
        if (line.contains("\"type\":\"resourceStage\"")
                && containsPath(line, "current-app-production/VAADIN/build/generated-flow-imports-")
                && containsGridLazyDataProviderV148Fields(line)
                && containsKnownStageStatus(line)) {
            summary.gridResources.lazyDataProviderV148StageObserved = true;
        }
        if (line.contains("\"type\":\"resourceStage\"")
                && containsPath(line, "current-app-production/VAADIN/build/generated-flow-imports-")
                && containsGridScrollVirtualizationV149Fields(line)
                && containsKnownStageStatus(line)) {
            summary.gridResources.scrollVirtualizationV149StageObserved = true;
        }
        if (line.contains("\"type\":\"resourceStage\"")
                && containsPath(line, "current-app-production/VAADIN/build/generated-flow-imports-")
                && containsBroaderComponentResourceMatrixV146Complete(line)
                && line.contains("\"executionStatus\":\"executed\"")) {
            summary.componentResources.broaderMatrixV146CompleteStageObserved = true;
        }
        if (line.contains("\"type\":\"resourceStage\"")
                && containsPath(line, "current-app-production/VAADIN/build/generated-flow-imports-")
                && containsDataComponentResourceMatrixV147Complete(line)
                && line.contains("\"executionStatus\":\"executed\"")) {
            summary.componentResources.dataMatrixV147CompleteStageObserved = true;
        }
        if (line.contains("\"type\":\"resourceStage\"")
                && containsPath(line, "current-app-production/VAADIN/build/generated-flow-imports-")
                && containsInputComponentParityMatrixV157Complete(line)
                && line.contains("\"executionStatus\":\"executed\"")) {
            summary.frameworkInputComponentMatrix.inputParityV157CompleteStageObserved = true;
        }
        if (line.contains("\"type\":\"resourceStage\"")
                && containsPath(line, "current-app-production/VAADIN/build/generated-flow-imports-")
                && containsMultiValueNavigationDisclosureMatrixV158Complete(line)
                && line.contains("\"executionStatus\":\"executed\"")) {
            summary.frameworkNavigationComponentMatrix.multiValueDisclosureV158CompleteStageObserved = true;
        }
        if (line.contains("\"type\":\"resourceStage\"")
                && containsPath(line, "current-app-production/VAADIN/build/generated-flow-imports-")
                && containsOverlayContextMenuMatrixV159Complete(line)
                && line.contains("\"executionStatus\":\"executed\"")) {
            summary.frameworkOverlayComponentMatrix.contextMenuMatrixV159CompleteStageObserved = true;
        }
        if (line.contains("\"type\":\"resourceStage\"")
                && containsCurrentAppProductionMatrixRuntimePath(line)
                && containsContentAppShellLoginMatrixV160Complete(line)
                && line.contains("\"executionStatus\":\"executed\"")) {
            summary.frameworkContentAppShellLoginMatrix.appShellLoginV160CompleteStageObserved = true;
        }
        if (line.contains("\"type\":\"resourceStage\"")
                && containsPath(line, "current-app-production/VAADIN/build/generated-flow-imports-")
                && containsGridLazyDataProviderV148Complete(line)
                && line.contains("\"executionStatus\":\"executed\"")) {
            summary.gridResources.lazyDataProviderV148CompleteStageObserved = true;
        }
        if (line.contains("\"type\":\"resourceStage\"")
                && containsPath(line, "current-app-production/VAADIN/build/generated-flow-imports-")
                && containsGridScrollVirtualizationV149Complete(line)
                && line.contains("\"executionStatus\":\"executed\"")) {
            summary.gridResources.scrollVirtualizationV149CompleteStageObserved = true;
        }
    }

    private static boolean containsKnownStageStatus(String line) {
        return line.contains("\"executionStatus\":\"executed\"")
                || line.contains("\"executionStatus\":\"strict-blocker\"")
                || line.contains("\"executionStatus\":\"staged\"");
    }
}
