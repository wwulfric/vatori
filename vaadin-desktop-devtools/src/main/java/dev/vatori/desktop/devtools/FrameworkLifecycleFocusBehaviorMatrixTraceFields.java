package dev.vatori.desktop.devtools;

final class FrameworkLifecycleFocusBehaviorMatrixTraceFields {
    boolean bootstrapObserved;
    boolean focusEventRpcObserved;
    boolean blurEventRpcObserved;
    boolean attachEventRpcObserved;
    boolean detachEventRpcObserved;
    boolean focusPatchObserved;
    boolean blurPatchObserved;
    boolean attachPatchObserved;
    boolean detachPatchObserved;
    boolean domObserved;

    boolean hasFrameworkSignals() {
        return bootstrapObserved
                && focusEventRpcObserved
                && blurEventRpcObserved
                && attachEventRpcObserved
                && detachEventRpcObserved
                && focusPatchObserved
                && blurPatchObserved
                && attachPatchObserved
                && detachPatchObserved
                && domObserved;
    }
}
