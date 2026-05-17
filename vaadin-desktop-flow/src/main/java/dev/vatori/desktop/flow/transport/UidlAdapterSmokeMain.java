package dev.vatori.desktop.flow.transport;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

public final class UidlAdapterSmokeMain {
    private UidlAdapterSmokeMain() {
    }

    public static void main(String[] args) {
        DesktopFlowRuntimeSession session = DesktopFlowRuntimeSession.create(
                "uidl-smoke-session",
                1,
                "flow-ipc-uidl-smoke",
                new Div(new Span("UIDL smoke over desktop adapter")));
        String uidl = session.createBootstrapUidl();
        System.out.println(uidl);
        if (!uidl.contains("\"changes\"") || !uidl.contains("\"syncId\"")
                || !uidl.contains("\"Vaadin-Security-Key\"")) {
            throw new IllegalStateException("UIDL smoke did not produce expected Flow UIDL fields");
        }
    }
}
