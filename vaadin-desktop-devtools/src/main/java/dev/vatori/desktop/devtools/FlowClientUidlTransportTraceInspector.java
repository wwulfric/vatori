package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class FlowClientUidlTransportTraceInspector {
    private FlowClientUidlTransportTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("\"type\":\"patch\"")
                && line.contains("\"source\":\"UidlRequestHandler\"")
                && line.contains("\"reason\":\"event\"")
                && containsClickedText(line)
                && line.contains("\"consumedByWebView\":true")
                && line.contains("\"flow-client-engine-visible\"")) {
            summary.flowClient.xhrPatchObserved = true;
        }
        if (line.contains("\"type\":\"patch\"")
                && line.contains("\"source\":\"UidlRequestHandler\"")
                && line.contains("\"eventDispatch\":\"flow-client-xhr\"")
                && line.contains("Text input: Flow IPC typed")
                && line.contains("\"consumedByWebView\":true")
                && line.contains("\"flow-client-engine-visible\"")) {
            summary.flowClient.textInputPatchObserved = true;
        }
        if (line.contains("\"type\":\"patch\"")
                && line.contains("\"source\":\"UidlRequestHandler\"")
                && line.contains("\"eventDispatch\":\"flow-client-xhr\"")
                && line.contains("Checkbox: checked")
                && line.contains("\"consumedByWebView\":true")
                && line.contains("\"flow-client-engine-visible\"")) {
            summary.flowClient.checkboxPatchObserved = true;
        }
        if (line.contains("\"type\":\"patch\"")
                && line.contains("\"source\":\"UidlRequestHandler\"")
                && line.contains("\"eventDispatch\":\"flow-client-xhr\"")
                && line.contains("Vaadin BigDecimalField: 42.5")
                && line.contains("\"consumedByWebView\":true")
                && line.contains("\"flow-client-engine-visible\"")) {
            summary.vaadinComponents.webComponentPatchObserved = true;
        }
        if (line.contains("\"type\":\"patch\"")
                && line.contains("\"source\":\"UidlRequestHandler\"")
                && line.contains("\"eventDispatch\":\"flow-client-xhr\"")
                && line.contains("Vaadin TextField: Flow IPC typed")
                && line.contains("\"consumedByWebView\":true")
                && line.contains("\"flow-client-engine-visible\"")) {
            summary.vaadinComponents.textFieldPatchObserved = true;
        }
        if (line.contains("\"type\":\"patch\"")
                && line.contains("\"source\":\"UidlRequestHandler\"")
                && line.contains("\"eventDispatch\":\"flow-client-xhr\"")
                && line.contains("Vaadin Checkbox: checked")
                && line.contains("\"consumedByWebView\":true")
                && line.contains("\"flow-client-engine-visible\"")) {
            summary.vaadinComponents.secondWebComponentPatchObserved = true;
        }
        if (line.contains("\"type\":\"patch\"")
                && line.contains("\"source\":\"UidlRequestHandler\"")
                && line.contains("\"consumedByWebView\":true")
                && line.contains("\"flow-client-engine-visible\"")) {
            summary.core.uidlPatchObserved = true;
        }
        if (line.contains("\"type\":\"push\"")
                && line.contains("\"source\":\"UidlWriter\"")
                && line.contains("\"reason\":\"push\"")
                && line.contains("\"async\":true")
                && line.contains("\"consumedByWebView\":true")
                && line.contains("\"flow-client-engine-visible\"")) {
            summary.core.pushUidlObserved = true;
        }
        if (line.contains("\"type\":\"push\"")
                && line.contains("\"pushIndex\":2")
                && line.contains("6 Ordered push over IPC READY")
                && line.contains("\"source\":\"UidlWriter\"")
                && line.contains("\"reason\":\"push\"")
                && line.contains("\"async\":true")
                && line.contains("\"flow-client-engine-visible\"")) {
            summary.core.multiPushUidlObserved = true;
        }
        if (line.contains("\"type\":\"push\"")
                && line.contains("\"orderingProbe\"")
                && line.contains("\"kind\":\"duplicate-late\"")
                && line.contains("\"effectiveServerSequence\":4")
                && line.contains("\"expectedAction\":\"ignore-and-request-resync\"")) {
            summary.core.duplicateLatePushProbeObserved = true;
        }
        if (line.contains("\"type\":\"resync\"")
                && line.contains("\"reason\":\"ordering-recovery-duplicate-late\"")
                && line.contains("\"securityKey\"")
                && line.contains("\"syncId\"")
                && line.contains("\"clientId\"")) {
            summary.core.recoveryResyncRequestObserved = true;
        }
        if (line.contains("\"type\":\"patch\"")
                && line.contains("\"source\":\"UidlRequestHandler\"")
                && line.contains("\"reason\":\"resync\"")
                && line.contains("\"resync\":true")
                && line.contains("resynchronize")
                && line.contains("\"consumedByWebView\":true")
                && line.contains("\"flow-client-engine-visible\"")) {
            summary.core.resyncUidlObserved = true;
        }
    }
}
