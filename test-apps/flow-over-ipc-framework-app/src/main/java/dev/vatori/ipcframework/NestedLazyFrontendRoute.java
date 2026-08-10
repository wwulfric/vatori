package dev.vatori.ipcframework;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.util.concurrent.atomic.AtomicInteger;

@Route(value = "lazy-nested", layout = LazyFrontendLayout.class)
@JsModule("./vatori-lazy-nested-probe.js")
@CssImport("./styles/vatori-lazy-nested-route.css")
public final class NestedLazyFrontendRoute extends VerticalLayout {
    public NestedLazyFrontendRoute() {
        setId("framework-nested-lazy-route");
        addClassName("vatori-lazy-nested-route");
        setPadding(true);
        setSpacing(true);

        H1 title = new H1("Nested lazy frontend route");
        Span status = new Span("Nested lazy route ready");
        status.setId("framework-nested-lazy-status");

        NestedLazyProbe probe = new NestedLazyProbe();
        AtomicInteger clientClickCount = new AtomicInteger();
        probe.getElement().addEventListener("click", event -> {
            int count = clientClickCount.incrementAndGet();
            probe.setMessage("client event acknowledged");
            status.setText("Nested lazy client event: count=" + count);
        });

        Button serverUpdate = new Button("Patch nested lazy frontend", event -> {
            probe.setMessage("server patched");
            status.setText("Nested lazy server state: patched");
        });
        serverUpdate.setId("framework-nested-lazy-server-update");

        Button returnToFramework = new Button("Return from nested lazy frontend", event ->
                event.getSource().getUI()
                        .ifPresent(ui -> ui.navigate(FlowOverIpcFrameworkView.class)));
        returnToFramework.setId("framework-nested-lazy-return");

        add(title, probe, serverUpdate, returnToFramework, status);
    }

    @Tag("vatori-lazy-nested-probe")
    public static final class NestedLazyProbe extends Component {
        private NestedLazyProbe() {
            setId("framework-nested-lazy-probe");
            setMessage("module ready");
        }

        private void setMessage(String message) {
            getElement().setProperty("message", message);
        }
    }
}
