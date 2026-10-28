package dev.vatori.ipcframework;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import dev.vatori.desktop.Desktop;

/** Public Flow interaction followed by public final-window close. */
@Route("flow-host-shutdown-termination")
public final class FlowHostShutdownTerminationRoute extends VerticalLayout {
    private static final String PREFIX = "framework-flow-host-shutdown-termination-";
    private int preparationCount;
    private int closeCount;
    private boolean prepared;

    public FlowHostShutdownTerminationRoute() {
        setId(PREFIX + "route");
        String runId = FlowHostShutdownTerminationStartup.runId();
        String mode = FlowHostShutdownTerminationStartup.mode();
        long pid = ProcessHandle.current().pid();
        String identityFields = "runId=" + runId + " pid=" + pid + " mode=" + mode;
        Span identity = new Span("Host shutdown termination: " + identityFields);
        identity.setId(PREFIX + "identity");
        identity.getElement().setAttribute("data-run-id", runId);
        identity.getElement().setAttribute("data-process-id", Long.toString(pid));
        identity.getElement().setAttribute("data-mode", mode);
        Span counter = new Span("Host shutdown counter: 0");
        counter.setId(PREFIX + "counter");
        Span pushed = new Span("Host shutdown push: pending");
        pushed.setId(PREFIX + "push-status");

        Button close = new Button("Close last window");
        close.setId(PREFIX + "final");
        close.setEnabled(false);
        close.getElement().addEventListener("click", event -> {
            if (!prepared || closeCount != 0) return;
            close.setEnabled(false);
            boolean trusted = event.getEventData().path("event.isTrusted").asBoolean(false);
            String title = "Host shutdown close: " + identityFields + " trusted=" + trusted
                    + " closeCount=" + ++closeCount;
            // The title command/result proves the final Java listener before
            // native close; a final DOM/UIDL response is intentionally not required.
            if (Desktop.current().setWindowTitle(title).success()) {
                Desktop.current().windows().close("main");
            }
        }).addEventData("event.isTrusted");

        Button prepare = new Button("Prepare host shutdown", event -> {
            event.getSource().setEnabled(false);
            UI ui = event.getSource().getUI().orElseThrow();
            boolean client = event.isFromClient();
            int count = ++preparationCount;
            counter.setText("Host shutdown counter: " + count);
            Thread.ofVirtual().name("host-shutdown-termination-ui-update").start(() -> {
                boolean backgroundSessionAbsent = VaadinSession.getCurrent() == null;
                ui.access(() -> {
                    pushed.setText("Host shutdown push: " + identityFields + " count=" + count
                            + " client=" + client + " backgroundSessionAbsent=" + backgroundSessionAbsent);
                    prepared = true;
                    close.setEnabled(true);
                    ui.push();
                });
            });
        });
        prepare.setId(PREFIX + "prepare");
        prepare.setEnabled(FlowHostShutdownTerminationStartup.enabled());
        add(new H1("Flow host shutdown termination"), identity, prepare, close, counter, pushed);
    }
}
