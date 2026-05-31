package dev.vatori.ipcframework;

import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.LoadMode;

@Route("flow-ipc-lazy-probe-v1_27")
@JavaScript(value = "context://vatori-flow-ipc-real-build-probe-v1_27.js", loadMode = LoadMode.LAZY)
public class FlowIpcFrameworkGeneratedLazyProbeView extends VerticalLayout {

    public FlowIpcFrameworkGeneratedLazyProbeView() {
        setId("flow-ipc-lazy-probe-v1_27");
        add(new Span("Flow IPC framework generated lazy real build probe v1.27"));
    }
}
