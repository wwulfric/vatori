package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceSummaryLinePrinter.printLine;

final class FrontendBuildCurrentAppOwnerTraceSummaryPrinter {
    private FrontendBuildCurrentAppOwnerTraceSummaryPrinter() {
    }

    static void print(TraceSummary summary) {
        FrontendBuildCurrentAppProductionOwnerTraceFields currentAppProductionOwner =
                summary.currentAppProductionOwner;
        FrontendBuildCurrentAppOwnerComponentReuseTraceFields currentAppOwnerComponentReuse =
                summary.currentAppOwnerComponentReuse;
        FrontendBuildCurrentAppOwnerEarlyImportTraceFields currentAppOwnerEarlyImport =
                summary.currentAppOwnerEarlyImport;
        printLine("Flow IPC v1.35 current app production runtime ownership manifest observed",
                currentAppProductionOwner.runtimeOwnershipV135ManifestObserved);
        printLine("Flow IPC v1.35 current app production runtime ownership request/response observed",
                currentAppProductionOwner.runtimeOwnershipV135RequestObserved,
                currentAppProductionOwner.runtimeOwnershipV135ResponseObserved);
        printLine("Flow IPC v1.35 current app production runtime ownership stage/handoff/DOM observed",
                currentAppProductionOwner.runtimeOwnershipV135StageObserved,
                currentAppProductionOwner.runtimeOwnershipV135HandoffObserved,
                currentAppProductionOwner.runtimeOwnershipV135DomObserved);
        printLine("Flow IPC v1.36 current app production owner import manifest observed",
                currentAppProductionOwner.ownerImportV136ManifestObserved);
        printLine("Flow IPC v1.36 current app production owner import request/response observed",
                currentAppProductionOwner.ownerImportV136RequestObserved,
                currentAppProductionOwner.ownerImportV136ResponseObserved);
        printLine("Flow IPC v1.36 current app production owner import stage/attempt/DOM observed",
                currentAppProductionOwner.ownerImportV136StageObserved,
                currentAppProductionOwner.ownerImportV136AttemptObserved,
                currentAppProductionOwner.ownerImportV136DomObserved);
        printLine("Flow IPC v1.37 current app production owner component reuse manifest observed",
                currentAppOwnerComponentReuse.productionOwnerComponentReuseV137ManifestObserved);
        printLine("Flow IPC v1.37 current app production owner component reuse request/response observed",
                currentAppOwnerComponentReuse.productionOwnerComponentReuseV137RequestObserved,
                currentAppOwnerComponentReuse.productionOwnerComponentReuseV137ResponseObserved);
        printLine("Flow IPC v1.37 current app production owner component reuse stage/TextField/DOM observed",
                currentAppOwnerComponentReuse.productionOwnerComponentReuseV137StageObserved,
                currentAppOwnerComponentReuse.productionOwnerComponentReuseV137TextFieldReuseObserved,
                currentAppOwnerComponentReuse.productionOwnerComponentReuseV137DomObserved);
        printLine("Flow IPC v1.38 current app owner component reuse matrix manifest observed",
                currentAppOwnerComponentReuse.matrixV138ManifestObserved);
        printLine("Flow IPC v1.38 current app owner component reuse matrix request/response observed",
                currentAppOwnerComponentReuse.matrixV138RequestObserved,
                currentAppOwnerComponentReuse.matrixV138ResponseObserved);
        printLine("Flow IPC v1.38 current app owner component reuse matrix stage/complete/DOM observed",
                currentAppOwnerComponentReuse.matrixV138StageObserved,
                currentAppOwnerComponentReuse.matrixV138CompleteStageObserved,
                currentAppOwnerComponentReuse.matrixV138DomObserved);
        printLine("Flow IPC v1.39 current app owner early import manifest observed",
                currentAppOwnerEarlyImport.earlyImportV139ManifestObserved);
        printLine("Flow IPC v1.39 current app owner early import request/response observed",
                currentAppOwnerEarlyImport.earlyImportV139RequestObserved,
                currentAppOwnerEarlyImport.earlyImportV139ResponseObserved);
        printLine("Flow IPC v1.39 current app owner early import stage/complete/DOM observed",
                currentAppOwnerEarlyImport.earlyImportV139StageObserved,
                currentAppOwnerEarlyImport.earlyImportV139CompleteStageObserved,
                currentAppOwnerEarlyImport.earlyImportV139DomObserved);
    }
}
