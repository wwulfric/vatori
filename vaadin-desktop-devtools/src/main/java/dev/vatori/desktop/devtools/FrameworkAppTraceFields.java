package dev.vatori.desktop.devtools;

final class FrameworkAppTraceFields {
    boolean launchObserved;
    boolean capabilityManifestObserved;
    boolean bootstrapObserved;
    boolean budgetEventRpcObserved;
    boolean checkboxEventRpcObserved;
    boolean selectEventRpcObserved;
    boolean applyEventRpcObserved;
    boolean budgetPatchObserved;
    boolean checkboxPatchObserved;
    boolean selectPatchObserved;
    boolean applyPatchObserved;
    boolean windowTitleCommandObserved;
    boolean clipboardCommandObserved;
    boolean notificationCommandObserved;
    boolean windowTitleResultObserved;
    boolean clipboardResultObserved;
    boolean notificationResultObserved;
    boolean nativeClipboardReadMatrixCommandObserved;
    boolean nativeClipboardReadMatrixResultObserved;
    boolean nativeClipboardReadMatrixPermissionObserved;
    boolean nativeClipboardReadMatrixTextObserved;
    boolean nativeClipboardReadMatrixDomObserved;
    boolean domObserved;
    boolean interactionDomObserved;

    boolean hasFrameworkBaseSignals() {
        return launchObserved
                && capabilityManifestObserved
                && bootstrapObserved
                && domObserved
                && budgetEventRpcObserved
                && checkboxEventRpcObserved
                && selectEventRpcObserved
                && applyEventRpcObserved
                && budgetPatchObserved
                && checkboxPatchObserved
                && selectPatchObserved
                && applyPatchObserved
                && windowTitleCommandObserved
                && clipboardCommandObserved
                && notificationCommandObserved
                && windowTitleResultObserved
                && clipboardResultObserved
                && notificationResultObserved
                && interactionDomObserved;
    }

    boolean hasNativeClipboardReadMatrixSignals() {
        return nativeClipboardReadMatrixCommandObserved
                && nativeClipboardReadMatrixResultObserved
                && nativeClipboardReadMatrixPermissionObserved
                && nativeClipboardReadMatrixTextObserved
                && nativeClipboardReadMatrixDomObserved;
    }
}
