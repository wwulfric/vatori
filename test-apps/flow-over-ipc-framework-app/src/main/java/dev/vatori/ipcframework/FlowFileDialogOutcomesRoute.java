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
import dev.vatori.desktop.DesktopWindow;

import java.util.concurrent.atomic.AtomicInteger;

@Route("flow-file-dialog-outcomes")
public final class FlowFileDialogOutcomesRoute extends VerticalLayout {
    static final String SECONDARY_WINDOW_ID = "file-dialog-outcomes-secondary";
    static final String FIRST_TITLE = "Flow file dialog normal cancel";
    static final String CONCURRENT_TITLE = "Flow file dialog concurrent invocation";
    static final String DENIED_TITLE = "Flow file dialog denied invocation";

    private final AtomicInteger startCount = new AtomicInteger();

    public FlowFileDialogOutcomesRoute() {
        setId("framework-flow-file-dialog-outcomes-route");
        setPadding(true);
        setSpacing(true);

        Span startStatus = new Span("File dialog start: pending");
        startStatus.setId("framework-flow-file-dialog-outcomes-start-status");
        Span firstStatus = new Span("File dialog first: pending");
        firstStatus.setId("framework-flow-file-dialog-outcomes-first-status");
        Span concurrentStatus = new Span("File dialog concurrent: pending");
        concurrentStatus.setId(
                "framework-flow-file-dialog-outcomes-concurrent-status");

        Button invokeFirst = new Button(
                "Start first file dialog", event -> {
                    boolean fromClient = event.isFromClient();
                    DesktopCapabilityContext capabilityContext = Desktop.current()
                            .captureCapabilityContext();
                    startStatus.setText("File dialog start: client=" + fromClient
                            + " capabilityContextCaptured=true"
                            + " change=" + startCount.incrementAndGet());
                    UI ui = event.getSource().getUI().orElseThrow();
                    Thread.ofVirtual().name("file-dialog-first").start(() -> {
                        boolean backgroundSessionAbsent =
                                VaadinSession.getCurrent() == null;
                        DesktopFileDialogResult result = capabilityContext
                                .fileDialog().openFileResult(FIRST_TITLE);
                        String status = formatResult("File dialog first", result)
                                + " capabilityContextCaptured=true"
                                + " backgroundSessionAbsent="
                                + backgroundSessionAbsent;
                        ui.access(() -> firstStatus.setText(
                                status));
                    });
                });
        invokeFirst.setId(
                "framework-flow-file-dialog-outcomes-invoke-first");

        Button invokeConcurrent = new Button(
                "Invoke concurrent file dialog", event -> {
                    DesktopFileDialogResult result = Desktop.current()
                            .fileDialog().openFileResult(CONCURRENT_TITLE);
                    concurrentStatus.setText(
                            formatResult("File dialog concurrent", result)
                                    + " client=" + event.isFromClient());
                });
        invokeConcurrent.setId(
                "framework-flow-file-dialog-outcomes-invoke-concurrent");

        Span windowStatus = new Span("File dialog window: pending");
        windowStatus.setId("framework-flow-file-dialog-outcomes-window-status");
        Button openSecondary = new Button(
                "Open denied file dialog window", event -> {
                    DesktopCommandResult result = Desktop.current().windows()
                            .open(DesktopWindow.builder(SECONDARY_WINDOW_ID)
                                    .title("Flow file dialog outcomes denied")
                                    .size(760, 520)
                                    .route("flow-file-dialog-denied")
                                    .build());
                    windowStatus.setText("File dialog window: opened="
                            + result.success()
                            + " client=" + event.isFromClient()
                            + " error=" + errorValue(result));
                });
        openSecondary.setId(
                "framework-flow-file-dialog-outcomes-open-secondary");

        add(new H1("Flow file dialog typed outcomes acceptance"),
                invokeFirst,
                invokeConcurrent,
                startStatus,
                firstStatus,
                concurrentStatus,
                openSecondary,
                windowStatus);
    }

    private static String formatResult(
            String prefix, DesktopFileDialogResult result) {
        DesktopCommandResult commandResult = result.commandResult();
        return prefix
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
