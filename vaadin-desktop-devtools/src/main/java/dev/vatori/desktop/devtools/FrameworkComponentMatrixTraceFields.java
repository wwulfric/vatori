package dev.vatori.desktop.devtools;

final class FrameworkComponentMatrixTraceFields {
    boolean bootstrapObserved;
    boolean reviewerEventRpcObserved;
    boolean dueDateEventRpcObserved;
    boolean dialogEventRpcObserved;
    boolean reviewerPatchObserved;
    boolean dueDatePatchObserved;
    boolean dialogPatchObserved;
    boolean domObserved;
    boolean dataBootstrapObserved;
    boolean dataOwnerEventRpcObserved;
    boolean dataValidateEventRpcObserved;
    boolean dataOwnerPatchObserved;
    boolean dataValidatePatchObserved;
    boolean dataDomObserved;

    boolean hasBasicSignals() {
        return bootstrapObserved
                && reviewerEventRpcObserved
                && dueDateEventRpcObserved
                && dialogEventRpcObserved
                && reviewerPatchObserved
                && dueDatePatchObserved
                && dialogPatchObserved
                && domObserved;
    }

    boolean hasDataSignals() {
        return dataBootstrapObserved
                && dataOwnerEventRpcObserved
                && dataValidateEventRpcObserved
                && dataOwnerPatchObserved
                && dataValidatePatchObserved
                && dataDomObserved;
    }
}
