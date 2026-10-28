package dev.vatori.ipcframework;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** Ordinary Flow listener and push, observed across test-only JVM generations. */
@Route("flow-recovery-termination")
public final class FlowRecoveryTerminationRoute extends VerticalLayout {
    private static final String PREFIX = "framework-flow-recovery-termination-";

    public FlowRecoveryTerminationRoute() {
        setId(PREFIX + "route");
        setPadding(true);
        setSpacing(true);
        int generation = FlowRecoveryTerminationStartup.generation();
        long pid = ProcessHandle.current().pid();
        String document = UUID.randomUUID().toString();
        String identityFields = "generation=" + generation + " pid=" + pid + " document=" + document;
        Span identity = new Span("Recovery termination: " + identityFields);
        identity.setId(PREFIX + "identity");
        identity.getElement().setAttribute("data-generation", Integer.toString(generation));
        identity.getElement().setAttribute("data-process-id", Long.toString(pid));
        identity.getElement().setAttribute("data-document-marker", document);
        Span counter = new Span("Recovery counter: 0");
        counter.setId(PREFIX + "counter");
        Span pushed = new Span("Recovery push: pending");
        pushed.setId(PREFIX + "push-status");
        AtomicInteger changes = new AtomicInteger();
        Button invoke = new Button("Update recovery state", event -> {
            UI ui = event.getSource().getUI().orElseThrow();
            boolean client = event.isFromClient();
            int count = changes.incrementAndGet();
            counter.setText("Recovery counter: " + count);
            Thread.ofVirtual().name("recovery-termination-ui-update").start(() -> {
                boolean backgroundSessionAbsent = VaadinSession.getCurrent() == null;
                ui.access(() -> {
                    pushed.setText("Recovery push: " + identityFields + " count=" + count
                            + " client=" + client
                            + " backgroundSessionAbsent=" + backgroundSessionAbsent);
                    ui.push();
                });
            });
        });
        invoke.setId(PREFIX + "invoke");
        add(new H1("Flow recovery termination"), identity, invoke, counter, pushed);
    }
}
