package dev.vatori.ipcframework;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.util.concurrent.atomic.AtomicInteger;

@Route("lazy-frontend")
public final class LazyFrontendRoute extends VerticalLayout {
    public LazyFrontendRoute() {
        setId("framework-lazy-frontend-route");
        setPadding(true);
        setSpacing(true);

        H1 title = new H1("Lazy frontend route");
        Span status = new Span("Lazy frontend route ready");
        status.setId("framework-lazy-frontend-status");

        LazyFrontendProbe probe = new LazyFrontendProbe();
        AtomicInteger clientClickCount = new AtomicInteger();
        probe.getElement().addEventListener("click", event -> {
            int count = clientClickCount.incrementAndGet();
            probe.setMessage("client event acknowledged");
            status.setText("Lazy frontend client event: count=" + count);
        });

        Button serverUpdate = new Button("Patch lazy frontend", event -> {
            probe.setMessage("server patched");
            status.setText("Lazy frontend server state: patched");
        });
        serverUpdate.setId("framework-lazy-frontend-server-update");

        Button returnToFramework = new Button("Return to framework", event ->
                event.getSource().getUI()
                        .ifPresent(ui -> ui.navigate(FlowOverIpcFrameworkView.class)));
        returnToFramework.setId("framework-lazy-frontend-return");

        add(title, probe, serverUpdate, returnToFramework, status);
    }

    @Tag("vatori-lazy-probe")
    @JsModule("./vatori-lazy-probe.js")
    public static final class LazyFrontendProbe extends Component {
        private LazyFrontendProbe() {
            setId("framework-lazy-frontend-probe");
            setMessage("module ready");
        }

        private void setMessage(String message) {
            getElement().setProperty("message", message);
        }
    }
}
