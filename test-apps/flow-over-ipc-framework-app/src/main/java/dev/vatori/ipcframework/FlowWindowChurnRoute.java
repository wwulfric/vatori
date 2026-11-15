package dev.vatori.ipcframework;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import dev.vatori.desktop.Desktop;
import dev.vatori.desktop.DesktopCapabilityContext;
import dev.vatori.desktop.DesktopCommandResult;
import dev.vatori.desktop.DesktopWindow;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Bounded acceptance application; no framework-internal scope or transport access. */
@Route("flow-window-churn")
public final class FlowWindowChurnRoute extends VerticalLayout {
    static final String CHURN = "churn-secondary";
    static final String SIBLING = "churn-sibling";
    private static final List<DesktopCapabilityContext> CAPTURED = new ArrayList<>();
    private final String document = UUID.randomUUID().toString();
    private int changes;
    private int operations;
    private boolean probed;

    public FlowWindowChurnRoute() {
        setId("window-churn-route");
        Span identity = new Span(document);
        identity.setId("window-churn-document");
        Span state = new Span("change=0");
        state.setId("window-churn-state");
        Button mutate = new Button("Change this owner", event ->
                state.setText("change=" + ++changes + " client=" + event.isFromClient()));
        mutate.setId("window-churn-mutate");
        Span operation = new Span("pending");
        operation.setId("window-churn-operation");
        add(identity, state, mutate, operation);
        for (String owner : List.of(SIBLING, CHURN)) {
            Button open = new Button("Open " + owner, event -> {
                DesktopCommandResult result = Desktop.current().windows().open(
                        DesktopWindow.builder(owner).title(owner).size(720, 520)
                                .route("flow-window-churn").build());
                operation.setText("open=" + owner + " success=" + result.success()
                        + " client=" + event.isFromClient() + " operation=" + ++operations);
            });
            open.setId("window-churn-open-" + owner);
            Button close = new Button("Close " + owner, event -> {
                DesktopCommandResult result = Desktop.current().windows().close(owner);
                operation.setText("close=" + owner + " success=" + result.success()
                        + " client=" + event.isFromClient() + " operation=" + ++operations);
            });
            close.setId("window-churn-close-" + owner);
            add(open, close);
        }
        Span result = new Span("pending");
        result.setId("window-churn-probe-result");
        Button probe = new Button("Probe retired owners and capture this owner", event -> {
            if (probed) {
                result.setText("error=duplicate-probe");
                return;
            }
            probed = true;
            DesktopCapabilityContext fresh = Desktop.current().captureCapabilityContext();
            List<DesktopCapabilityContext> retired;
            synchronized (CAPTURED) {
                if (CAPTURED.size() >= 11) {
                    result.setText("error=profile-exhausted");
                    return;
                }
                retired = List.copyOf(CAPTURED);
                CAPTURED.add(fresh);
            }
            boolean fromClient = event.isFromClient();
            UI ui = event.getSource().getUI().orElseThrow();
            Thread.ofVirtual().name("window-churn-probe").start(() -> {
                boolean background = VaadinSession.getCurrent() == null;
                try {
                    int before = rejectRetired(retired, "before");
                    boolean freshSuccess = fresh.setWindowTitle("Churn owner " + retired.size()).success();
                    int after = rejectRetired(retired, "after");
                    ui.access(() -> result.setText("cycle=" + retired.size()
                            + " before=" + before + " after=" + after
                            + " fresh=" + freshSuccess + " background=" + background
                            + " client=" + fromClient));
                } catch (RuntimeException failure) {
                    ui.access(() -> result.setText("error=" + failure.getClass().getSimpleName()));
                }
            });
        });
        probe.setId("window-churn-probe");
        add(probe, result);
    }

    static int rejectRetired(List<DesktopCapabilityContext> retired, String phase) {
        int rejected = 0;
        for (int i = 0; i < retired.size(); i++) {
            DesktopCommandResult result = retired.get(i).setWindowTitle("Stale churn " + phase + " " + i);
            if (!result.success() && Boolean.TRUE.equals(result.payload().get("canceled"))
                    && Boolean.TRUE.equals(result.payload().get("operationInvalidated"))
                    && "owner-window-closed".equals(result.payload().get("cancelReason"))) rejected++;
        }
        return rejected;
    }
}
