package dev.vatori.ipcframework;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import dev.vatori.desktop.Desktop;
import dev.vatori.desktop.DesktopCommandResult;
import dev.vatori.desktop.DesktopWindow;

import java.util.concurrent.atomic.AtomicInteger;

@Route("flow-capability-permission")
public final class FlowCapabilityPermissionRoute extends VerticalLayout {
    private static final String SECONDARY_WINDOW_ID = "permission-secondary";
    private static final String ALLOWED_WINDOW_TITLE =
            "Capability permission allowed title";
    private final AtomicInteger changeCount = new AtomicInteger();

    public FlowCapabilityPermissionRoute() {
        setId("framework-flow-capability-permission-route");
        setPadding(true);
        setSpacing(true);

        Span resultStatus = new Span("Capability permission result: pending");
        resultStatus.setId("framework-flow-capability-permission-result");
        Button invokeSetTitle = new Button(
                "Invoke scoped window title command", event -> {
                    DesktopCommandResult result = Desktop.current()
                            .setWindowTitle(ALLOWED_WINDOW_TITLE);
                    resultStatus.setText(formatResult(
                            result,
                            event.isFromClient(),
                            changeCount.incrementAndGet()));
                });
        invokeSetTitle.setId("framework-flow-capability-permission-invoke");

        Span windowStatus = new Span("Capability permission window: pending");
        windowStatus.setId(
                "framework-flow-capability-permission-window-status");
        Button openSecondary = new Button(
                "Open capability permission window", event -> {
                    DesktopCommandResult result = Desktop.current().windows()
                            .open(DesktopWindow.builder(SECONDARY_WINDOW_ID)
                                    .title("Capability permission secondary")
                                    .size(760, 520)
                                    .route("flow-capability-permission")
                                    .build());
                    windowStatus.setText(
                            "Capability permission window: success="
                                    + result.success()
                                    + " error=" + errorValue(result));
                });
        openSecondary.setId(
                "framework-flow-capability-permission-open-window");

        Button returnToFramework = new Button(
                "Return to framework", event -> event.getSource().getUI()
                        .ifPresent(ui -> ui.navigate(
                                FlowOverIpcFrameworkView.class)));
        returnToFramework.setId(
                "framework-flow-capability-permission-return");

        add(new H1("Flow capability permission acceptance"),
                invokeSetTitle,
                resultStatus,
                openSecondary,
                windowStatus,
                returnToFramework);
    }

    private static String formatResult(
            DesktopCommandResult result,
            boolean fromClient,
            int changeCount) {
        return "Capability permission result: success=" + result.success()
                + " error=" + errorValue(result)
                + " permissionMode=" + payloadValue(result, "permissionMode")
                + " permissionDecision="
                + payloadValue(result, "permissionDecision")
                + " permissionReason="
                + payloadValue(result, "permissionReason")
                + " permissionScope="
                + payloadValue(result, "permissionScope")
                + " command=" + payloadValue(result, "command")
                + " client=" + fromClient
                + " change=" + changeCount;
    }

    private static String errorValue(DesktopCommandResult result) {
        String error = result.error();
        return error == null || error.isBlank() ? "none" : error;
    }

    private static String payloadValue(
            DesktopCommandResult result, String key) {
        Object value = result.payload().get(key);
        return value == null ? "missing" : value.toString();
    }
}
