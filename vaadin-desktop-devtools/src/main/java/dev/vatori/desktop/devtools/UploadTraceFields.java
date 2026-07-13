package dev.vatori.desktop.devtools;

final class UploadTraceFields {
    boolean receiverV153ManifestObserved;
    boolean receiverV153RequestObserved;
    boolean receiverV153ResponseObserved;
    boolean receiverV153PatchObserved;
    boolean receiverV153DomObserved;
    boolean multipartV154ManifestObserved;
    boolean multipartV154RequestObserved;
    boolean multipartV154ResponseObserved;
    boolean multipartV154PatchObserved;
    boolean multipartV154DomObserved;
    boolean receiverBootstrapObserved;
    boolean receiverDomObserved;
    boolean multipartDomObserved;

    boolean hasReceiverSignals() {
        return receiverBootstrapObserved && receiverDomObserved;
    }

    boolean hasReceiverGateSignals() {
        return receiverV153ManifestObserved
                && receiverV153RequestObserved
                && receiverV153ResponseObserved
                && receiverV153PatchObserved
                && receiverV153DomObserved;
    }

    boolean hasMultipartSignals() {
        return receiverBootstrapObserved && multipartDomObserved;
    }

    boolean hasMultipartGateSignals() {
        return multipartV154ManifestObserved
                && multipartV154RequestObserved
                && multipartV154ResponseObserved
                && multipartV154PatchObserved
                && multipartV154DomObserved;
    }

    static boolean containsUploadReceiverV153Fields(String line) {
        return UploadReceiverTraceFields.containsUploadReceiverV153Fields(line);
    }

    static boolean containsUploadReceiverV153Response(String line) {
        return UploadReceiverTraceFields.containsUploadReceiverV153Response(line);
    }

    static boolean containsUploadReceiverV153Dom(String line) {
        return UploadReceiverTraceFields.containsUploadReceiverV153Dom(line);
    }

    static boolean containsUploadMultipartV154Fields(String line) {
        return UploadMultipartTraceFields.containsUploadMultipartV154Fields(line);
    }

    static boolean containsUploadMultipartV154Response(String line) {
        return UploadMultipartTraceFields.containsUploadMultipartV154Response(line);
    }

    static boolean containsUploadMultipartV154Dom(String line) {
        return UploadMultipartTraceFields.containsUploadMultipartV154Dom(line);
    }
}
