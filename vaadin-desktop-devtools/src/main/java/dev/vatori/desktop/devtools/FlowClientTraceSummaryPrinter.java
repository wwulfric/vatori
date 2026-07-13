package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceSummaryLinePrinter.printLine;

final class FlowClientTraceSummaryPrinter {
    private FlowClientTraceSummaryPrinter() {
    }

    static void print(FlowClientTraceFields flowClient) {
        printLine("Flow client resource manifest observed", flowClient.resourceManifestObserved);
        printLine("Flow client resource request observed", flowClient.resourceRequestObserved);
        printLine("Flow client resource response observed", flowClient.resourceResponseObserved);
        printLine("Flow client resource DOM observed", flowClient.resourceDomObserved);
        printLine("Flow client resource graph DOM observed", flowClient.resourceGraphDomObserved);
        printLine("Flow client resource execution classification DOM observed",
                flowClient.resourceExecutionClassificationDomObserved);
        printLine("Flow client resource lazy lookup request observed",
                flowClient.resourceLazyLookupRequestObserved);
        printLine("Flow client resource lazy lookup response observed",
                flowClient.resourceLazyLookupResponseObserved);
        printLine("Flow client resource lazy lookup DOM observed", flowClient.resourceLazyLookupDomObserved);
        printLine("Flow client engine resource manifest observed", flowClient.engineResourceManifestObserved);
        printLine("Flow client engine resource request observed", flowClient.engineResourceRequestObserved);
        printLine("Flow client engine resource response observed", flowClient.engineResourceResponseObserved);
        printLine("Flow client engine DOM observed", flowClient.engineDomObserved);
        printLine("Flow client engine bootstrap DOM observed", flowClient.engineBootstrapDomObserved);
        printLine("Flow client engine list DOM observed", flowClient.engineListDomObserved);
        printLine("Flow client engine XHR DOM observed", flowClient.engineXhrDomObserved);
        printLine("Flow client engine visible DOM observed", flowClient.engineVisibleDomObserved);
        printLine("Flow client engine push DOM observed", flowClient.enginePushDomObserved);
        printLine("Flow client engine push queue DOM observed", flowClient.enginePushQueueDomObserved);
        printLine("Flow client engine push ordering DOM observed", flowClient.enginePushOrderingDomObserved);
        printLine("Flow client engine resync DOM observed", flowClient.engineResyncDomObserved);
        printLine("Flow client engine generic viewport DOM observed", flowClient.engineGenericViewportDomObserved);
        printLine("UIDL RPC dispatch observed", flowClient.uidlRpcDispatchObserved);
        printLine("WebView UIDL RPC observed", flowClient.webViewUidlRpcObserved);
        printLine("Flow client XHR attach observed", flowClient.xhrAttachObserved);
        printLine("Flow client XHR click observed", flowClient.xhrClickObserved);
        printLine("Flow client XHR text input observed", flowClient.xhrTextInputObserved);
        printLine("Flow client XHR checkbox observed", flowClient.xhrCheckboxObserved);
        printLine("Flow client XHR patch observed", flowClient.xhrPatchObserved);
        printLine("Text input patch observed", flowClient.textInputPatchObserved);
        printLine("Checkbox patch observed", flowClient.checkboxPatchObserved);
        printLine("Flow client component events DOM observed", flowClient.engineComponentEventsDomObserved);
    }
}
