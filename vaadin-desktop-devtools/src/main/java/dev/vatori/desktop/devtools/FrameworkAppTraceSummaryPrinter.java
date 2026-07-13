package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceSummaryLinePrinter.printLine;

final class FrameworkAppTraceSummaryPrinter {
    private FrameworkAppTraceSummaryPrinter() {
    }

    static void printInteractionSignals(FrameworkAppTraceFields frameworkApp) {
        printLine("framework app launch observed", frameworkApp.launchObserved);
        printLine("framework app capability manifest observed", frameworkApp.capabilityManifestObserved);
        printLine("framework app bootstrap observed", frameworkApp.bootstrapObserved);
        printLine("framework app budget event RPC observed", frameworkApp.budgetEventRpcObserved);
        printLine("framework app checkbox event RPC observed", frameworkApp.checkboxEventRpcObserved);
        printLine("framework app select event RPC observed", frameworkApp.selectEventRpcObserved);
        printLine("framework app apply event RPC observed", frameworkApp.applyEventRpcObserved);
        printLine("framework app budget patch observed", frameworkApp.budgetPatchObserved);
        printLine("framework app checkbox patch observed", frameworkApp.checkboxPatchObserved);
        printLine("framework app select patch observed", frameworkApp.selectPatchObserved);
        printLine("framework app apply patch observed", frameworkApp.applyPatchObserved);
        printLine("framework app window title command/result observed",
                frameworkApp.windowTitleCommandObserved,
                frameworkApp.windowTitleResultObserved);
        printLine("framework app clipboard command/result observed",
                frameworkApp.clipboardCommandObserved,
                frameworkApp.clipboardResultObserved);
    }

    static void printNativeClipboardReadMatrix(FrameworkAppTraceFields frameworkApp) {
        printLine("native clipboard read matrix command/result/permission/text/DOM observed",
                frameworkApp.nativeClipboardReadMatrixCommandObserved,
                frameworkApp.nativeClipboardReadMatrixResultObserved,
                frameworkApp.nativeClipboardReadMatrixPermissionObserved,
                frameworkApp.nativeClipboardReadMatrixTextObserved,
                frameworkApp.nativeClipboardReadMatrixDomObserved);
    }

    static void printNotificationSignals(FrameworkAppTraceFields frameworkApp) {
        printLine("framework app notification command/result observed",
                frameworkApp.notificationCommandObserved,
                frameworkApp.notificationResultObserved);
    }

    static void printDomSignals(FrameworkAppTraceFields frameworkApp) {
        printLine("framework app DOM observed", frameworkApp.domObserved);
        printLine("framework app interaction DOM observed", frameworkApp.interactionDomObserved);
    }
}
