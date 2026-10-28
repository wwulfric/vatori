package dev.vatori.ipcframework;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import dev.vatori.desktop.Desktop;

/** Application-side probe of the public final-window lifecycle. */
@Route("flow-last-window-close")
public final class FlowLastWindowCloseRoute extends VerticalLayout {
    private int preparationCount;

    public FlowLastWindowCloseRoute() {
        setId("framework-flow-last-window-close-route");
        Span status = new Span("Last window close: pending");
        status.setId("framework-flow-last-window-close-status");
        Button close = new Button("Close last window", event -> {
            // The native title round trip records the Java listener before its
            // window disappears; no final browser patch is promised on close.
            if (Desktop.current().setWindowTitle(
                    "Final window close listener client=" + event.isFromClient())
                    .success()) {
                Desktop.current().windows().close("main");
            }
        });
        close.setId("framework-flow-last-window-close-final");
        close.setEnabled(false);
        Button prepare = new Button("Prepare final close", event -> {
            status.setText("Last window close: prepared=true client="
                    + event.isFromClient() + " change=" + ++preparationCount);
            close.setEnabled(true);
        });
        prepare.setId("framework-flow-last-window-close-prepare");
        add(prepare, close, status);
    }
}
