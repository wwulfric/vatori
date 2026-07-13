package dev.vatori.desktop.devtools;

final class CoreTraceFields {
    boolean flowIpcTransport;
    boolean compatibilityFalse;
    boolean serverSequenceObserved;
    boolean flowIpcServerSequenceDomObserved;
    boolean clickPatchObserved;
    boolean webViewErrorObserved;
    boolean uidlWriterObserved;
    boolean securityKeyObserved;
    boolean resyncSecurityKeyObserved;
    boolean bootstrapScopeObserved;
    boolean eventScopeObserved;
    boolean resyncScopeObserved;
    boolean uidlPatchObserved;
    boolean pushUidlObserved;
    boolean multiPushUidlObserved;
    boolean duplicateLatePushProbeObserved;
    boolean recoveryResyncRequestObserved;
    boolean resyncUidlObserved;
    boolean firstMeaningfulPaintObserved;

    boolean hasBaseSignals() {
        return flowIpcTransport
                && compatibilityFalse
                && serverSequenceObserved
                && flowIpcServerSequenceDomObserved
                && clickPatchObserved
                && webViewErrorObserved
                && uidlWriterObserved
                && securityKeyObserved
                && resyncSecurityKeyObserved
                && bootstrapScopeObserved
                && eventScopeObserved
                && resyncScopeObserved
                && uidlPatchObserved
                && pushUidlObserved
                && resyncUidlObserved;
    }

    boolean hasV1_1Signals() {
        return multiPushUidlObserved
                && duplicateLatePushProbeObserved
                && recoveryResyncRequestObserved;
    }
}
