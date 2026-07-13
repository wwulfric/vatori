package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceSummaryLinePrinter.printLine;

final class FrontendBuildCurrentAppBuildOutputTraceSummaryPrinter {
    private FrontendBuildCurrentAppBuildOutputTraceSummaryPrinter() {
    }

    static void print(TraceSummary summary) {
        GeneratedLazyBuildTraceFields generatedLazyBuild = summary.generatedLazyBuild;
        FrontendBuildPreflightTraceFields frontendBuildPreflight =
                summary.frontendBuildPreflight;
        CurrentAppBuildFrontendTraceFields currentAppBuildFrontend =
                summary.currentAppBuildFrontend;
        printLine("Flow IPC v1.26 generated lazy source boundary manifest observed",
                generatedLazyBuild.sourceBoundaryV126ManifestObserved);
        printLine("Flow IPC v1.26 generated lazy source boundary request/response observed",
                generatedLazyBuild.sourceBoundaryV126RequestObserved,
                generatedLazyBuild.sourceBoundaryV126ResponseObserved);
        printLine("Flow IPC v1.26 generated lazy source boundary stage/DOM/current blocker observed",
                generatedLazyBuild.sourceBoundaryV126StageObserved,
                generatedLazyBuild.sourceBoundaryV126DomObserved,
                generatedLazyBuild.sourceBoundaryV126CurrentBuildBlockerObserved);
        printLine("Flow IPC v1.27 generated lazy real build probe manifest observed",
                generatedLazyBuild.realBuildProbeV127ManifestObserved);
        printLine("Flow IPC v1.27 generated lazy real build probe request/response observed",
                generatedLazyBuild.realBuildProbeV127RequestObserved,
                generatedLazyBuild.realBuildProbeV127ResponseObserved);
        printLine("Flow IPC v1.27 generated lazy real build probe stage/DOM/current blocker observed",
                generatedLazyBuild.realBuildProbeV127StageObserved,
                generatedLazyBuild.realBuildProbeV127DomObserved,
                generatedLazyBuild.realBuildProbeV127CurrentBuildBlockerObserved);
        printLine("Flow IPC v1.28 generated lazy build lifecycle manifest observed",
                generatedLazyBuild.buildLifecycleV128ManifestObserved);
        printLine("Flow IPC v1.28 generated lazy build lifecycle request/response observed",
                generatedLazyBuild.buildLifecycleV128RequestObserved,
                generatedLazyBuild.buildLifecycleV128ResponseObserved);
        printLine("Flow IPC v1.28 generated lazy build lifecycle stage/DOM/current blocker observed",
                generatedLazyBuild.buildLifecycleV128StageObserved,
                generatedLazyBuild.buildLifecycleV128DomObserved,
                generatedLazyBuild.buildLifecycleV128CurrentBuildBlockerObserved);
        printLine("Flow IPC v1.29 generated lazy app build output manifest observed",
                generatedLazyBuild.appBuildOutputV129ManifestObserved);
        printLine("Flow IPC v1.29 generated lazy app build output request/response observed",
                generatedLazyBuild.appBuildOutputV129RequestObserved,
                generatedLazyBuild.appBuildOutputV129ResponseObserved);
        printLine("Flow IPC v1.29 generated lazy app build output stage/DOM/current blocker observed",
                generatedLazyBuild.appBuildOutputV129StageObserved,
                generatedLazyBuild.appBuildOutputV129DomObserved,
                generatedLazyBuild.appBuildOutputV129CurrentBuildBlockerObserved);
        printLine("Flow IPC v1.30 current app frontend build output manifest observed",
                generatedLazyBuild.currentAppFrontendBuildOutputV130ManifestObserved);
        printLine("Flow IPC v1.30 current app frontend build output request/response observed",
                generatedLazyBuild.currentAppFrontendBuildOutputV130RequestObserved,
                generatedLazyBuild.currentAppFrontendBuildOutputV130ResponseObserved);
        printLine("Flow IPC v1.30 current app frontend build output stage/DOM/build step observed",
                generatedLazyBuild.currentAppFrontendBuildOutputV130StageObserved,
                generatedLazyBuild.currentAppFrontendBuildOutputV130DomObserved,
                generatedLazyBuild.currentAppFrontendBuildOutputV130BuildStepObserved);
        printLine("Flow IPC v1.31 frontend build preflight availability manifest observed",
                frontendBuildPreflight.availabilityV131ManifestObserved);
        printLine("Flow IPC v1.31 frontend build preflight availability request/response observed",
                frontendBuildPreflight.availabilityV131RequestObserved,
                frontendBuildPreflight.availabilityV131ResponseObserved);
        printLine("Flow IPC v1.31 frontend build preflight availability stage/DOM/resolved observed",
                frontendBuildPreflight.availabilityV131StageObserved,
                frontendBuildPreflight.availabilityV131DomObserved,
                frontendBuildPreflight.availabilityV131ResolvedObserved);
        printLine("Flow IPC v1.32 current app build-frontend output manifest observed",
                currentAppBuildFrontend.outputV132ManifestObserved);
        printLine("Flow IPC v1.32 current app build-frontend output request/response observed",
                currentAppBuildFrontend.outputV132RequestObserved,
                currentAppBuildFrontend.outputV132ResponseObserved);
        printLine("Flow IPC v1.32 current app build-frontend output stage/DOM/resolved observed",
                currentAppBuildFrontend.outputV132StageObserved,
                currentAppBuildFrontend.outputV132DomObserved,
                currentAppBuildFrontend.outputV132ResolvedObserved);
        printLine("Flow IPC v1.33 current app generated chunk package edge manifest observed",
                currentAppBuildFrontend.generatedChunkPackageEdgeV133ManifestObserved);
        printLine("Flow IPC v1.33 current app generated chunk package edge request/response observed",
                currentAppBuildFrontend.generatedChunkPackageEdgeV133RequestObserved,
                currentAppBuildFrontend.generatedChunkPackageEdgeV133ResponseObserved);
        printLine("Flow IPC v1.33 current app generated chunk package edge stage/DOM/executed observed",
                currentAppBuildFrontend.generatedChunkPackageEdgeV133StageObserved,
                currentAppBuildFrontend.generatedChunkPackageEdgeV133DomObserved,
                currentAppBuildFrontend.generatedChunkPackageEdgeV133ExecutedObserved);
        printLine("Flow IPC v1.34 current app generated imports root package graph manifest observed",
                currentAppBuildFrontend.generatedImportsRootPackageGraphV134ManifestObserved);
        printLine("Flow IPC v1.34 current app generated imports root package graph request/response observed",
                currentAppBuildFrontend.generatedImportsRootPackageGraphV134RequestObserved,
                currentAppBuildFrontend.generatedImportsRootPackageGraphV134ResponseObserved);
        printLine("Flow IPC v1.34 current app generated imports root package graph stage/support/DOM observed",
                currentAppBuildFrontend.generatedImportsRootPackageGraphV134StageObserved,
                currentAppBuildFrontend.generatedImportsRootPackageGraphV134SupportExecutedObserved,
                currentAppBuildFrontend.generatedImportsRootPackageGraphV134DomObserved);
    }
}
