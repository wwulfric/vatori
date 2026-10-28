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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicInteger;

/** Public notification calls with test-only, content-free delivery correlation. */
@Route("flow-notification-delivery")
public final class FlowNotificationDeliveryRoute extends VerticalLayout {
    private static final String PREFIX = "framework-flow-notification-delivery-";
    private static final String BODY = "Native foreground delivery validation";

    public FlowNotificationDeliveryRoute() {
        setId(PREFIX + "route");
        setPadding(true);
        setSpacing(true);
        add(new H1("Flow native notification delivery"),
                operation("prepare", "Prepare notification permission",
                        "Vatori notification preparation"),
                operation("probe", "Probe foreground notification",
                        "Vatori foreground probe"));
    }

    private static VerticalLayout operation(String stage, String label, String title) {
        AtomicInteger count = new AtomicInteger();
        Span status = new Span("Notification " + stage + ": pending");
        status.setId(PREFIX + stage + "-status");
        Button invoke = new Button(label, event -> {
            boolean client = event.isFromClient();
            int change = count.incrementAndGet();
            UI ui = event.getSource().getUI().orElseThrow();
            DesktopCapabilityContext capabilities =
                    Desktop.current().captureCapabilityContext();
            status.getElement().removeAttribute("data-notification-digest");
            status.setText("Notification " + stage + ": pending");
            Thread.ofVirtual().name("notification-delivery-" + stage).start(() -> {
                boolean backgroundSessionAbsent = VaadinSession.getCurrent() == null;
                DesktopNotificationResult result = capabilities.notifications()
                        .showResult(title, BODY);
                DesktopCommandResult command = result.commandResult();
                String digest = result.notificationId()
                        .map(FlowNotificationDeliveryRoute::identifierDigest)
                        .orElse(null);
                // Raw identifier, title, body and native errors never enter the
                // DOM or diagnostics. The digest is only a test correlation key,
                // not a delivery receipt or a notification cancellation token.
                String state = "Notification " + stage + ": outcome=" + result.outcome()
                        + " success=" + command.success()
                        + " scheduled=" + Boolean.TRUE.equals(command.payload().get("scheduled"))
                        + " notificationIdPresent=" + result.notificationId().isPresent()
                        + " client=" + client + " change=" + change
                        + " capabilityContextCaptured=true"
                        + " backgroundSessionAbsent=" + backgroundSessionAbsent
                        + " permissionMetadataValid=" + validPermissionMetadata(command);
                ui.access(() -> {
                    if (digest == null) {
                        status.getElement().removeAttribute("data-notification-digest");
                    } else {
                        status.getElement().setAttribute("data-notification-digest", digest);
                    }
                    status.setText(state);
                });
            });
        });
        invoke.setId(PREFIX + "invoke-" + stage);
        VerticalLayout row = new VerticalLayout(invoke, status);
        row.setPadding(false);
        row.setSpacing(false);
        return row;
    }

    private static String identifierDigest(String identifier) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(identifier.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }

    private static boolean validPermissionMetadata(DesktopCommandResult result) {
        var payload = result.payload();
        return "notification.show".equals(payload.get("command"))
                && "manifest-enforced".equals(payload.get("permissionMode"))
                && "allowed".equals(payload.get("permissionDecision"))
                && "manifest-allow".equals(payload.get("permissionReason"))
                && "window:main".equals(payload.get("permissionScope"))
                && Boolean.TRUE.equals(payload.get("permissionManifestLoaded"))
                && "$APP/Contents/Resources/desktop-capabilities.json"
                        .equals(payload.get("permissionManifestPath"))
                && "notification.show".equals(payload.get("permissionManifestCommands"));
    }
}
