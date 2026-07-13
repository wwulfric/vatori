package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceSummaryLinePrinter.printLine;

final class ResourcePipelineTraceSummaryPrinter {
    private ResourcePipelineTraceSummaryPrinter() {
    }

    static void print(TraceSummary summary) {
        GridResourceTraceFields gridResources = summary.gridResources;
        StreamResourceTraceFields streamResources = summary.streamResources;
        UploadTraceFields uploads = summary.uploads;
        DownloadTraceFields downloads = summary.downloads;
        FrontendResourceClosureTraceFields frontendResourceClosure =
                summary.frontendResourceClosure;
        FrontendBuildPipelineTraceFields frontendBuildPipeline =
                summary.frontendBuildPipeline;
        printLine("Flow IPC v1.48 Grid lazy data provider manifest observed",
                gridResources.lazyDataProviderV148ManifestObserved);
        printLine("Flow IPC v1.48 Grid lazy data provider stage/complete/DOM observed",
                gridResources.lazyDataProviderV148StageObserved,
                gridResources.lazyDataProviderV148CompleteStageObserved,
                gridResources.lazyDataProviderV148DomObserved);
        printLine("framework lazy data provider bootstrap/RPC/patch/DOM observed",
                gridResources.lazyBootstrapObserved,
                gridResources.lazyPublishedRpcObserved,
                gridResources.lazyFetchPatchObserved,
                gridResources.lazyDomObserved);
        printLine("Flow IPC v1.49 Grid scroll virtualization manifest observed",
                gridResources.scrollVirtualizationV149ManifestObserved);
        printLine("Flow IPC v1.49 Grid scroll virtualization stage/complete/DOM observed",
                gridResources.scrollVirtualizationV149StageObserved,
                gridResources.scrollVirtualizationV149CompleteStageObserved,
                gridResources.scrollVirtualizationV149DomObserved);
        printLine("framework Grid scroll virtualization bootstrap/RPC/patch/DOM observed",
                gridResources.scrollBootstrapObserved,
                gridResources.scrollPublishedRpcObserved,
                gridResources.scrollFetchPatchObserved,
                gridResources.scrollDomObserved);
        printLine("Flow IPC v1.50 StreamResource manifest/request/response/DOM observed",
                streamResources.basicV150ManifestObserved,
                streamResources.basicV150RequestObserved,
                streamResources.basicV150ResponseObserved,
                streamResources.basicV150DomObserved);
        printLine("framework StreamResource bootstrap/DOM observed",
                streamResources.basicBootstrapObserved,
                streamResources.basicDomObserved);
        printLine("Flow IPC v1.51 StreamResource range manifest/request/response/DOM observed",
                streamResources.rangeV151ManifestObserved,
                streamResources.rangeV151RequestObserved,
                streamResources.rangeV151ResponseObserved,
                streamResources.rangeV151DomObserved);
        printLine("framework StreamResource range bootstrap/DOM observed",
                streamResources.rangeBootstrapObserved,
                streamResources.rangeDomObserved);
        printLine("Flow IPC v1.52 StreamResource chunks manifest/request/response/DOM observed",
                streamResources.chunksV152ManifestObserved,
                streamResources.chunksV152RequestObserved,
                streamResources.chunksV152ResponseObserved,
                streamResources.chunksV152DomObserved);
        printLine("framework StreamResource chunks bootstrap/DOM observed",
                streamResources.chunksBootstrapObserved,
                streamResources.chunksDomObserved);
        printLine("Flow IPC v1.53 Upload receiver manifest/request/response/patch/DOM observed",
                uploads.receiverV153ManifestObserved,
                uploads.receiverV153RequestObserved,
                uploads.receiverV153ResponseObserved,
                uploads.receiverV153PatchObserved,
                uploads.receiverV153DomObserved);
        printLine("framework Upload receiver bootstrap/DOM observed",
                uploads.receiverBootstrapObserved,
                uploads.receiverDomObserved);
        printLine("Flow IPC v1.54 Upload multipart manifest/request/response/patch/DOM observed",
                uploads.multipartV154ManifestObserved,
                uploads.multipartV154RequestObserved,
                uploads.multipartV154ResponseObserved,
                uploads.multipartV154PatchObserved,
                uploads.multipartV154DomObserved);
        printLine("framework Upload multipart DOM observed", uploads.multipartDomObserved);
        printLine("Flow IPC v1.55 DownloadHandler save/open manifest/request/response/capability/DOM observed",
                downloads.saveOpenV155ManifestObserved,
                downloads.saveOpenV155RequestObserved,
                downloads.saveOpenV155ResponseObserved,
                downloads.saveOpenV155CapabilityResultObserved,
                downloads.saveOpenV155DomObserved);
        printLine("framework DownloadHandler save/open bootstrap/DOM observed",
                downloads.saveOpenBootstrapObserved,
                downloads.saveOpenDomObserved);
        printLine("Flow IPC v1.56 frontend/resource pipeline basic closure DOM observed",
                frontendResourceClosure.domObserved);
        printLine("Flow IPC v1.6 TextField graph manifest observed",
                frontendBuildPipeline.textFieldV16ManifestObserved);
        printLine("Flow IPC v1.6 TextField graph request observed",
                frontendBuildPipeline.textFieldV16RequestObserved);
        printLine("Flow IPC v1.6 TextField narrowed staged blocker observed",
                frontendBuildPipeline.textFieldV16StageBlockerObserved);
        printLine("Flow IPC v1.6 TextField dependency classification observed",
                frontendBuildPipeline.textFieldV16DependencyClassificationObserved);
        printLine("Flow IPC v1.6 TextField DOM diagnostics observed",
                frontendBuildPipeline.textFieldV16DomObserved);
        printLine("Flow IPC v1.7 TextField side-effect manifest observed",
                frontendBuildPipeline.textFieldV17ManifestObserved);
        printLine("Flow IPC v1.7 TextField side-effect request observed",
                frontendBuildPipeline.textFieldV17RequestObserved);
        printLine("Flow IPC v1.7 TextField staged execution observed",
                frontendBuildPipeline.textFieldV17StageExecutedObserved);
        printLine("Flow IPC v1.7 TextField module identity dependency observed",
                frontendBuildPipeline.textFieldV17ModuleIdentityDependencyObserved);
        printLine("Flow IPC v1.7 TextField DOM diagnostics observed",
                frontendBuildPipeline.textFieldV17DomObserved);
    }
}
