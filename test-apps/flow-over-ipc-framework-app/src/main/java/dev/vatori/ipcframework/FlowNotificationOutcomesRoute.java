package dev.vatori.ipcframework;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import dev.vatori.desktop.Desktop;
import dev.vatori.desktop.DesktopCapabilityContext;
import dev.vatori.desktop.DesktopCommandResult;
import dev.vatori.desktop.DesktopNotificationResult;
import dev.vatori.desktop.DesktopWindow;

import java.util.concurrent.atomic.AtomicInteger;

/** Ordinary public API calls; only test status and stable selectors are fixture-specific. */
@Route("flow-notification-outcomes")
public final class FlowNotificationOutcomesRoute extends VerticalLayout {
    static final String SECONDARY_WINDOW_ID = "notification-outcomes-secondary";

    public FlowNotificationOutcomesRoute() {
        setId("framework-flow-notification-outcomes-route");
        setPadding(true);
        setSpacing(true);
        add(new H1("Flow native notification outcomes"),
                operation("invalid", "Show notification with a blank title", " "),
                operation("show", "Request native notification",
                        "Vatori notification acceptance"),
                operation("denied", "Invoke denied notification",
                        "Vatori denied notification"));

        Span windowStatus = new Span("Notification window: pending");
        windowStatus.setId("framework-flow-notification-outcomes-window-status");
        Button openSecondary = new Button("Open denied notification window", event -> {
            DesktopCommandResult result = Desktop.current().windows().open(
                    DesktopWindow.builder(SECONDARY_WINDOW_ID)
                            .title("Flow notification permission denied")
                            .size(760, 620)
                            .route("flow-notification-outcomes")
                            .build());
            windowStatus.setText("Notification window: opened=" + result.success()
                    + " client=" + event.isFromClient());
        });
        openSecondary.setId("framework-flow-notification-outcomes-open-secondary");
        add(openSecondary, windowStatus);
    }

    private static VerticalLayout operation(String name, String label, String title) {
        AtomicInteger count = new AtomicInteger();
        Span status = new Span("Notification " + name + ": pending");
        status.setId("framework-flow-notification-outcomes-" + name + "-status");
        Button invoke = new Button(label, event -> {
            boolean client = event.isFromClient();
            int change = count.incrementAndGet();
            UI ui = event.getSource().getUI().orElseThrow();
            DesktopCapabilityContext capabilities =
                    Desktop.current().captureCapabilityContext();
            Thread.ofVirtual().name("notification-" + name).start(() -> {
                boolean backgroundSessionAbsent = VaadinSession.getCurrent() == null;
                DesktopNotificationResult result = capabilities.notifications()
                        .showResult(title, "Native capability outcome validation");
                DesktopCommandResult command = result.commandResult();
                // Do not expose notification title, body, identifier or error message.
                String state = "Notification " + name + ": outcome=" + result.outcome()
                        + " success=" + command.success()
                        + " scheduled=" + Boolean.TRUE.equals(command.payload().get("scheduled"))
                        + " notificationIdPresent=" + result.notificationId().isPresent()
                        + " errorCode=" + errorCode(command)
                        + " client=" + client + " change=" + change
                        + " capabilityContextCaptured=true"
                        + " backgroundSessionAbsent=" + backgroundSessionAbsent
                        + " permissionMetadataValid=" + validPermissionMetadata(command, name);
                ui.access(() -> status.setText(state));
            });
        });
        invoke.setId("framework-flow-notification-outcomes-invoke-" + name);
        VerticalLayout row = new VerticalLayout(invoke, status);
        row.setPadding(false);
        row.setSpacing(false);
        return row;
    }

    private static String errorCode(DesktopCommandResult result) {
        Object code = result.payload().get("errorCode");
        if (code == null) {
            return "none";
        }
        return switch (code.toString()) {
            case "notification-title-invalid", "notification-authorization-required",
                    "notification-authorization-denied", "notification-authorization-failed",
                    "notification-scheduling-failed" -> code.toString();
            default -> "unexpected";
        };
    }

    private static boolean validPermissionMetadata(DesktopCommandResult result, String name) {
        boolean denied = "denied".equals(name);
        var payload = result.payload();
        return "notification.show".equals(payload.get("command"))
                && "manifest-enforced".equals(payload.get("permissionMode"))
                && (denied ? "denied" : "allowed").equals(payload.get("permissionDecision"))
                && (denied ? "manifest-denied" : "manifest-allow")
                        .equals(payload.get("permissionReason"))
                && (denied ? "window:" + SECONDARY_WINDOW_ID : "window:main")
                        .equals(payload.get("permissionScope"))
                && Boolean.TRUE.equals(payload.get("permissionManifestLoaded"))
                && (denied ? "window.close,window.open" : "notification.show,window.open")
                        .equals(payload.get("permissionManifestCommands"));
    }
}
