package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.FrontendBuildComponentResourceTraceFields.*;
import static dev.vatori.desktop.devtools.FrontendBuildCurrentAppOwnerTraceFields.*;
import static dev.vatori.desktop.devtools.FrontendBuildGeneratedImportsOwnerTraceFields.*;
import static dev.vatori.desktop.devtools.FrontendBuildPackagingTraceFields.*;
import static dev.vatori.desktop.devtools.FrontendResourceClosureTraceFields.*;
import static dev.vatori.desktop.devtools.GridResourceTraceFields.*;
import static dev.vatori.desktop.devtools.StreamResourceTraceFields.*;
import static dev.vatori.desktop.devtools.UploadTraceFields.*;
import static dev.vatori.desktop.devtools.DownloadTraceFields.*;
import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class ComponentResourceMatrixDomTraceInspector {
    private ComponentResourceMatrixDomTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (!isWebviewDomLine(line)) {
            return;
        }
        if (containsPackagingRuntimeHandoffReuseV145Complete(line)
                && containsBroaderComponentResourceMatrixV146Complete(line)
                && containsStringField(line, "broaderComponentResourceMatrixAppShape",
                        "framework-validation-app")
                && containsStringField(line, "frameworkComponentMatrixVersion",
                        "flow-ipc-broader-component-resource-matrix-v1_46")
                && containsTrueFields(line,
                        "frameworkComponentMatrixComplete",
                        "frameworkComponentMatrixReviewerEventObserved",
                        "frameworkComponentMatrixDueDateEventObserved",
                        "frameworkComponentMatrixDialogEventObserved")
                && containsAll(line,
                        "Dialog opened: desktop due 2026-06-16")) {
            summary.componentResources.broaderMatrixV146DomObserved = true;
        }
        if (containsBroaderComponentResourceMatrixV146Complete(line)
                && containsDataComponentResourceMatrixV147Complete(line)
                && containsStringField(line, "dataComponentResourceMatrixAppShape",
                        "framework-validation-app")
                && containsStringField(line, "frameworkDataComponentMatrixVersion",
                        "flow-ipc-data-component-resource-matrix-v1_47")
                && containsTrueFields(line,
                        "frameworkDataComponentMatrixComplete",
                        "frameworkDataComponentMatrixGridRendered",
                        "frameworkDataComponentMatrixVirtualListRendered",
                        "frameworkDataComponentMatrixFormLayoutRendered",
                        "frameworkDataComponentMatrixOwnerFieldRendered",
                        "frameworkDataComponentMatrixOwnerEventObserved",
                        "frameworkDataComponentMatrixValidateEventObserved")
                && containsAll(line,
                        "Validation accepted: Ava Desktop")) {
            summary.componentResources.dataMatrixV147DomObserved = true;
        }
        if (containsTrueFields(line,
                        "frameworkInputComponentMatrixComplete",
                        "frameworkInputComponentMatrixDecisionRendered",
                        "frameworkInputComponentMatrixNotesRendered",
                        "frameworkInputComponentMatrixEmailRendered",
                        "frameworkInputComponentMatrixScoreRendered",
                        "frameworkInputComponentMatrixSecretRendered",
                        "frameworkInputComponentMatrixReviewTimeRendered",
                        "frameworkInputComponentMatrixDecisionEventObserved",
                        "frameworkInputComponentMatrixNotesEventObserved",
                        "frameworkInputComponentMatrixEmailEventObserved",
                        "frameworkInputComponentMatrixScoreEventObserved",
                        "frameworkInputComponentMatrixSecretEventObserved",
                        "frameworkInputComponentMatrixReviewTimeEventObserved")
                && containsStringField(line, "frameworkInputComponentMatrixVersion",
                        "flow-ipc-input-component-parity-matrix-v1_57")
                && containsAll(line,
                        "Review time changed: 14:45")) {
            summary.frameworkInputComponentMatrix.inputParityV157DomObserved = true;
        }
        if (containsTrueFields(line,
                        "frameworkNavigationComponentMatrixComplete",
                        "frameworkNavigationComponentMatrixAssigneesRendered",
                        "frameworkNavigationComponentMatrixTabsRendered",
                        "frameworkNavigationComponentMatrixDetailsRendered",
                        "frameworkNavigationComponentMatrixAccordionRendered",
                        "frameworkNavigationComponentMatrixMenuRendered",
                        "frameworkNavigationComponentMatrixAssigneesEventObserved",
                        "frameworkNavigationComponentMatrixTabsEventObserved",
                        "frameworkNavigationComponentMatrixDetailsEventObserved",
                        "frameworkNavigationComponentMatrixAccordionEventObserved",
                        "frameworkNavigationComponentMatrixMenuEventObserved")
                && containsStringField(line, "frameworkNavigationComponentMatrixVersion",
                        "flow-ipc-multi-value-navigation-disclosure-matrix-v1_58")
                && containsAll(line,
                        "Menu action: audit")) {
            summary.frameworkNavigationComponentMatrix.multiValueDisclosureV158DomObserved = true;
        }
        if (containsTrueFields(line,
                        "frameworkOverlayComponentMatrixComplete",
                        "frameworkOverlayComponentMatrixContextMenuRendered",
                        "frameworkOverlayComponentMatrixGridContextMenuRendered",
                        "frameworkOverlayComponentMatrixPopoverRendered",
                        "frameworkOverlayComponentMatrixContextMenuOpenEventObserved",
                        "frameworkOverlayComponentMatrixContextMenuItemEventObserved",
                        "frameworkOverlayComponentMatrixGridContextMenuOpenEventObserved",
                        "frameworkOverlayComponentMatrixGridContextMenuItemEventObserved",
                        "frameworkOverlayComponentMatrixPopoverOpenEventObserved")
                && containsStringField(line, "frameworkOverlayComponentMatrixVersion",
                        "flow-ipc-overlay-context-menu-matrix-v1_59")
                && containsAll(line,
                        "Popover opened: true")) {
            summary.frameworkOverlayComponentMatrix.contextMenuMatrixV159DomObserved = true;
        }
        if (containsTrueFields(line,
                        "frameworkRichContentComponentMatrixComplete",
                        "frameworkRichContentComponentMatrixAvatarRendered",
                        "frameworkRichContentComponentMatrixMessageRendered",
                        "frameworkRichContentComponentMatrixSideNavRendered",
                        "frameworkRichContentComponentMatrixLoginOverlayRendered",
                        "frameworkRichContentComponentMatrixAvatarEventObserved",
                        "frameworkRichContentComponentMatrixMessageEventObserved",
                        "frameworkRichContentComponentMatrixSideNavEventObserved",
                        "frameworkRichContentComponentMatrixLoginOpenEventObserved",
                        "frameworkRichContentComponentMatrixLoginSubmitEventObserved")
                && containsStringField(line,
                        "frameworkRichContentComponentMatrixVersion",
                        "flow-ipc-content-app-shell-login-matrix-v1_60")
                && containsAll(line,
                        "Login submitted: ava")) {
            summary.frameworkContentAppShellLoginMatrix.appShellLoginV160DomObserved = true;
        }
        if (containsDataComponentResourceMatrixV147Complete(line)
                && containsTrueFields(line,
                        "frameworkLazyDataProviderComplete",
                        "frameworkLazyDataProviderGridRendered",
                        "frameworkLazyDataProviderPublishedRpcObserved",
                        "frameworkLazyDataProviderFetchObserved",
                        "frameworkLazyDataProviderReportEventObserved")
                && containsStringField(line, "frameworkLazyDataProviderVersion",
                        "flow-ipc-grid-lazy-data-provider-v1_48")
                && containsAll(line,
                        "Lazy grid fetches:")
                && containsLazyGridReportRange(line)) {
            summary.gridResources.lazyDataProviderV148DomObserved = true;
        }
        if (containsAnyTrueField(line,
                        "frameworkGridScrollVirtualizationComplete",
                        "frameworkGridScrollVirtualizationAcceptedBlockerObserved")
                && containsStringField(line, "frameworkGridScrollVirtualizationVersion",
                        "flow-ipc-grid-scroll-virtualization-v1_49")
                && containsTrueFields(line,
                        "frameworkGridScrollVirtualizationClientApiInvoked")) {
            summary.gridResources.scrollVirtualizationV149DomObserved = true;
        }
        if (containsStreamResourceV150Dom(line)) {
            summary.streamResources.basicV150DomObserved = true;
        }
        if (containsStreamResourceRangeV151Dom(line)) {
            summary.streamResources.rangeV151DomObserved = true;
        }
        if (containsStreamResourceChunksV152Dom(line)) {
            summary.streamResources.chunksV152DomObserved = true;
            summary.streamResources.chunksDomObserved = true;
        }
        if (containsUploadReceiverV153Dom(line)) {
            summary.uploads.receiverV153DomObserved = true;
            summary.uploads.receiverDomObserved = true;
        }
        if (containsUploadMultipartV154Dom(line)) {
            summary.uploads.multipartV154DomObserved = true;
            summary.uploads.multipartDomObserved = true;
        }
        if (containsDownloadHandlerSaveOpenV155Dom(line)) {
            summary.downloads.saveOpenV155DomObserved = true;
            summary.downloads.saveOpenDomObserved = true;
        }
        if (containsFrontendResourcePipelineBasicClosureV156Dom(line)) {
            summary.frontendResourceClosure.domObserved = true;
        }
    }

    private static boolean isWebviewDomLine(String line) {
        return line.contains("\"type\":\"webview.dom\"");
    }

}
