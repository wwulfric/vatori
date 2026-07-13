package dev.vatori.desktop.devtools;

final class CoreCapabilityTraceFields {
    boolean capabilityManifestObserved;
    boolean capabilityManifestResultObserved;
    boolean fileDialogResultObserved;
    boolean nativeFileDialogResultObserved;
    boolean manifestEnforcedObserved;
    boolean capabilityDeniedObserved;
    boolean fileTransferManifestObserved;
    boolean fileTransferDownloadCommandRedactedObserved;
    boolean fileTransferDownloadResultObserved;
    boolean fileTransferUploadResultObserved;
    boolean fileTransferDomObserved;
    boolean nativeClipboardObserved;
    boolean capabilityPermissionObserved;

    boolean hasBaseSignals() {
        return capabilityManifestObserved
                && capabilityManifestResultObserved
                && fileDialogResultObserved
                && nativeClipboardObserved
                && capabilityPermissionObserved;
    }

    boolean hasV1_1Signals() {
        return nativeFileDialogResultObserved
                && manifestEnforcedObserved
                && capabilityDeniedObserved
                && fileTransferManifestObserved
                && fileTransferDownloadCommandRedactedObserved
                && fileTransferDownloadResultObserved
                && fileTransferUploadResultObserved
                && fileTransferDomObserved;
    }
}
