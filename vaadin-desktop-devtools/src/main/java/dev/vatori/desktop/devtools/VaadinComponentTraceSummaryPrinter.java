package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceSummaryLinePrinter.printLine;

final class VaadinComponentTraceSummaryPrinter {
    private VaadinComponentTraceSummaryPrinter() {
    }

    static void print(VaadinComponentTraceFields components) {
        printLine("Vaadin Web Component resource manifest observed",
                components.webComponentResourceManifestObserved);
        printLine("Vaadin Web Component resource request observed",
                components.webComponentResourceRequestObserved);
        printLine("Vaadin Web Component resource response observed",
                components.webComponentResourceResponseObserved);
        printLine("Vaadin Web Component resource DOM observed", components.webComponentResourceDomObserved);
        printLine("Vaadin Web Component resource executed DOM observed",
                components.webComponentResourceExecutedDomObserved);
        printLine("Vaadin Web Component failure classification observed",
                components.webComponentFailureClassificationObserved);
        printLine("Vaadin Web Component event RPC observed", components.webComponentEventRpcObserved);
        printLine("Vaadin Web Component patch observed", components.webComponentPatchObserved);
        printLine("Vaadin Web Component DOM observed", components.webComponentDomObserved);
        printLine("Vaadin TextField event RPC observed", components.textFieldEventRpcObserved);
        printLine("Vaadin TextField patch observed", components.textFieldPatchObserved);
        printLine("Vaadin TextField DOM observed", components.textFieldDomObserved);
        printLine("Flow IPC v1.3 component resolver manifest observed", components.resolverV13ManifestObserved);
        printLine("Flow IPC v1.3 component resolver DOM observed", components.resolverV13DomObserved);
        printLine("Second Vaadin Web Component resource request observed",
                components.secondWebComponentResourceRequestObserved);
        printLine("Second Vaadin Web Component resource response observed",
                components.secondWebComponentResourceResponseObserved);
        printLine("Second Vaadin Web Component dev-bundle resource observed",
                components.secondWebComponentDevBundleResourceObserved);
        printLine("Second Vaadin Web Component event RPC observed",
                components.secondWebComponentEventRpcObserved);
        printLine("Second Vaadin Web Component patch observed", components.secondWebComponentPatchObserved);
        printLine("Second Vaadin Web Component DOM observed", components.secondWebComponentDomObserved);
    }
}
