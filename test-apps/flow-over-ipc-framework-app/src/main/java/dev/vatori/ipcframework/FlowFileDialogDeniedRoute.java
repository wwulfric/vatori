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
import dev.vatori.desktop.DesktopFileDialogResult;

@Route("flow-file-dialog-denied")
public final class FlowFileDialogDeniedRoute extends VerticalLayout {
    public FlowFileDialogDeniedRoute() {
        setId("framework-flow-file-dialog-denied-route");
        setPadding(true);
        setSpacing(true);

        Span deniedStatus = new Span("File dialog denied: pending");
        deniedStatus.setId("framework-flow-file-dialog-outcomes-denied-status");
        Button invokeDenied = new Button(
                "Invoke denied file dialog", event -> {
                    boolean fromClient = event.isFromClient();
                    UI ui = event.getSource().getUI().orElseThrow();
                    DesktopCapabilityContext capabilityContext = Desktop.current()
                            .captureCapabilityContext();
                    Thread.ofVirtual().name("file-dialog-denied").start(() -> {
                        boolean backgroundSessionAbsent =
                                VaadinSession.getCurrent() == null;
                        DesktopFileDialogResult result = capabilityContext
                                .fileDialog().openFileResult(
                                        FlowFileDialogOutcomesRoute.DENIED_TITLE);
                        String status = formatResult(result)
                                + " capabilityContextCaptured=true"
                                + " backgroundSessionAbsent="
                                + backgroundSessionAbsent
                                + " client=" + fromClient;
                        ui.access(() -> deniedStatus.setText(status));
                    });
                });
        invokeDenied.setId(
                "framework-flow-file-dialog-outcomes-invoke-denied");

        add(new H1("Flow file dialog denied outcome acceptance"),
                invokeDenied,
                deniedStatus);
    }

    private static String formatResult(DesktopFileDialogResult result) {
        DesktopCommandResult commandResult = result.commandResult();
        return "File dialog denied"
                + ": outcome=" + result.outcome().name()
                + " selected=" + result.selectedPath()
                        .map(Object::toString).orElse("none")
                + " cancelReason=" + result.cancelReason().orElse("none")
                + " success=" + commandResult.success()
                + " canceled=" + payloadValue(commandResult, "canceled")
                + " error=" + errorValue(commandResult)
                + " permissionMode="
                + payloadValue(commandResult, "permissionMode")
                + " permissionDecision="
                + payloadValue(commandResult, "permissionDecision")
                + " permissionReason="
                + payloadValue(commandResult, "permissionReason")
                + " permissionScope="
                + payloadValue(commandResult, "permissionScope")
                + " permissionManifestLoaded="
                + payloadValue(commandResult, "permissionManifestLoaded")
                + " permissionManifestCommands="
                + payloadValue(commandResult, "permissionManifestCommands")
                + " command=" + payloadValue(commandResult, "command");
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
