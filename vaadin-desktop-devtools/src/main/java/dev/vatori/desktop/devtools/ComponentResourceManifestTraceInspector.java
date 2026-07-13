package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.DownloadTraceFields.*;
import static dev.vatori.desktop.devtools.FrontendBuildComponentResourceTraceFields.*;
import static dev.vatori.desktop.devtools.FrameworkContentAppShellLoginMatrixTraceFields.*;
import static dev.vatori.desktop.devtools.FrameworkInputComponentMatrixTraceFields.*;
import static dev.vatori.desktop.devtools.FrameworkNavigationComponentMatrixTraceFields.*;
import static dev.vatori.desktop.devtools.FrameworkOverlayComponentMatrixTraceFields.*;
import static dev.vatori.desktop.devtools.GridResourceTraceFields.*;
import static dev.vatori.desktop.devtools.StreamResourceTraceFields.*;
import static dev.vatori.desktop.devtools.UploadTraceFields.*;

final class ComponentResourceManifestTraceInspector {
    private ComponentResourceManifestTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsBroaderComponentResourceMatrixV146Fields(line)) {
            summary.componentResources.broaderMatrixV146ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsDataComponentResourceMatrixV147Fields(line)) {
            summary.componentResources.dataMatrixV147ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsInputComponentParityMatrixV157Fields(line)) {
            summary.frameworkInputComponentMatrix.inputParityV157ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsMultiValueNavigationDisclosureMatrixV158Fields(line)) {
            summary.frameworkNavigationComponentMatrix.multiValueDisclosureV158ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsOverlayContextMenuMatrixV159Fields(line)) {
            summary.frameworkOverlayComponentMatrix.contextMenuMatrixV159ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsContentAppShellLoginMatrixV160Fields(line)) {
            summary.frameworkContentAppShellLoginMatrix.appShellLoginV160ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsGridLazyDataProviderV148Fields(line)) {
            summary.gridResources.lazyDataProviderV148ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsGridScrollVirtualizationV149Fields(line)) {
            summary.gridResources.scrollVirtualizationV149ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsStreamResourceV150Fields(line)) {
            summary.streamResources.basicV150ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsStreamResourceRangeV151Fields(line)) {
            summary.streamResources.rangeV151ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsStreamResourceChunksV152Fields(line)) {
            summary.streamResources.chunksV152ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsUploadReceiverV153Fields(line)) {
            summary.uploads.receiverV153ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsUploadMultipartV154Fields(line)) {
            summary.uploads.multipartV154ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsDownloadHandlerSaveOpenV155Fields(line)) {
            summary.downloads.saveOpenV155ManifestObserved = true;
        }
    }
}
