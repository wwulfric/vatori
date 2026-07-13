package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceSummaryLinePrinter.printLine;

final class CoreCapabilityTraceSummaryPrinter {
    private CoreCapabilityTraceSummaryPrinter() {
    }

    static void print(CoreCapabilityTraceFields capabilities) {
        printLine("capability manifest observed", capabilities.capabilityManifestObserved);
        printLine("capability manifest result observed", capabilities.capabilityManifestResultObserved);
        printLine("file dialog result observed", capabilities.fileDialogResultObserved);
        printLine("native file dialog result observed", capabilities.nativeFileDialogResultObserved);
        printLine("manifest-enforced permission observed", capabilities.manifestEnforcedObserved);
        printLine("capability denied observed", capabilities.capabilityDeniedObserved);
        printLine("file transfer manifest observed", capabilities.fileTransferManifestObserved);
        printLine("file transfer download command redacted observed",
                capabilities.fileTransferDownloadCommandRedactedObserved);
        printLine("file transfer download result observed", capabilities.fileTransferDownloadResultObserved);
        printLine("file transfer upload result observed", capabilities.fileTransferUploadResultObserved);
        printLine("file transfer DOM observed", capabilities.fileTransferDomObserved);
    }
}
