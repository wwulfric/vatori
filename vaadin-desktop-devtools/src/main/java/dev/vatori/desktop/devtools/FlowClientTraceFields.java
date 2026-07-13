package dev.vatori.desktop.devtools;

final class FlowClientTraceFields {
    boolean resourceManifestObserved;
    boolean resourceRequestObserved;
    boolean resourceResponseObserved;
    boolean resourceDomObserved;
    boolean resourceGraphDomObserved;
    boolean resourceExecutionClassificationDomObserved;
    boolean resourceLazyLookupRequestObserved;
    boolean resourceLazyLookupResponseObserved;
    boolean resourceLazyLookupDomObserved;
    boolean engineResourceManifestObserved;
    boolean engineResourceRequestObserved;
    boolean engineResourceResponseObserved;
    boolean engineDomObserved;
    boolean engineBootstrapDomObserved;
    boolean engineListDomObserved;
    boolean engineXhrDomObserved;
    boolean engineVisibleDomObserved;
    boolean enginePushDomObserved;
    boolean enginePushQueueDomObserved;
    boolean enginePushOrderingDomObserved;
    boolean engineResyncDomObserved;
    boolean engineGenericViewportDomObserved;
    boolean uidlRpcDispatchObserved;
    boolean webViewUidlRpcObserved;
    boolean xhrAttachObserved;
    boolean xhrClickObserved;
    boolean xhrTextInputObserved;
    boolean xhrCheckboxObserved;
    boolean xhrPatchObserved;
    boolean textInputPatchObserved;
    boolean checkboxPatchObserved;
    boolean engineComponentEventsDomObserved;

    boolean hasBaseSignals() {
        return resourceManifestObserved
                && resourceRequestObserved
                && resourceResponseObserved
                && resourceDomObserved
                && resourceGraphDomObserved
                && engineResourceManifestObserved
                && engineResourceRequestObserved
                && engineResourceResponseObserved
                && engineDomObserved
                && engineBootstrapDomObserved
                && engineListDomObserved
                && engineXhrDomObserved
                && engineVisibleDomObserved
                && enginePushDomObserved
                && enginePushQueueDomObserved
                && engineResyncDomObserved
                && uidlRpcDispatchObserved
                && webViewUidlRpcObserved
                && xhrAttachObserved
                && xhrClickObserved
                && xhrPatchObserved;
    }

    boolean hasV1_1Signals() {
        return enginePushOrderingDomObserved
                && resourceExecutionClassificationDomObserved
                && resourceLazyLookupRequestObserved
                && resourceLazyLookupResponseObserved
                && resourceLazyLookupDomObserved
                && xhrTextInputObserved
                && xhrCheckboxObserved
                && textInputPatchObserved
                && checkboxPatchObserved
                && engineComponentEventsDomObserved;
    }

    boolean hasV1_3Signals() {
        return engineGenericViewportDomObserved;
    }

    boolean hasFrameworkBaseSignals() {
        return uidlRpcDispatchObserved
                && resourceManifestObserved
                && resourceRequestObserved
                && resourceResponseObserved
                && resourceDomObserved
                && resourceGraphDomObserved
                && resourceExecutionClassificationDomObserved
                && resourceLazyLookupRequestObserved
                && resourceLazyLookupResponseObserved
                && resourceLazyLookupDomObserved
                && engineResourceManifestObserved
                && engineResourceRequestObserved
                && engineResourceResponseObserved
                && engineDomObserved
                && engineGenericViewportDomObserved;
    }
}
