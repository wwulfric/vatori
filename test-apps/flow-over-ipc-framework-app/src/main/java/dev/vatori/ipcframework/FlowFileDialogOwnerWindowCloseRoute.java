package dev.vatori.ipcframework;

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
import java.util.concurrent.atomic.AtomicReference;

@Route("flow-file-dialog-owner-window-close")
public final class FlowFileDialogOwnerWindowCloseRoute extends VerticalLayout {
    static final String SECONDARY_WINDOW_ID = "owner-close-secondary";
    static final String DIALOG_TITLE = "Flow owner window close file dialog";

    private static final AtomicInteger resultChangeCount = new AtomicInteger();
    private static final AtomicReference<String> latestJavaResult =
            new AtomicReference<>("outcome=pending selected=none"
                    + " cancelReason=none operationInvalidated=false"
                    + " capabilityContextCaptured=false"
                    + " backgroundSessionAbsent=false"
                    + " secondaryClient=false change=0");
    private final AtomicInteger healthChangeCount = new AtomicInteger();

    public FlowFileDialogOwnerWindowCloseRoute() {
        setId("framework-flow-file-dialog-owner-close-route");
        setPadding(true);
        setSpacing(true);

        Span windowStatus = new Span("Owner close window: pending");
        windowStatus.setId("framework-flow-file-dialog-owner-close-window-status");
        Button openSecondary = new Button(
                "Open owner window", event -> {
                    DesktopCommandResult result = Desktop.current().windows()
                            .open(DesktopWindow.builder(SECONDARY_WINDOW_ID)
                                    .title("Flow owner close secondary")
                                    .size(760, 520)
                                    .route("flow-file-dialog-owner-window-close")
                                    .build());
                    windowStatus.setText("Owner close window: opened="
                            + result.success()
                            + " client=" + event.isFromClient()
                            + " error=" + errorValue(result));
                });
        openSecondary.setId(
                "framework-flow-file-dialog-owner-close-open-window");

        Button closeSecondary = new Button(
                "Close owner window", event -> {
                    DesktopCommandResult result = Desktop.current().windows()
                            .close(SECONDARY_WINDOW_ID);
                    windowStatus.setText("Owner close window: closing="
                            + result.success()
                            + " client=" + event.isFromClient()
                            + " error=" + errorValue(result));
                });
        closeSecondary.setId(
                "framework-flow-file-dialog-owner-close-close-window");

        Span javaResult = new Span("Owner close Java result: outcome=pending"
                + " selected=none cancelReason=none operationInvalidated=false"
                + " capabilityContextCaptured=false"
                + " backgroundSessionAbsent=false"
                + " secondaryClient=false change=0");
        javaResult.setId("framework-flow-file-dialog-owner-close-java-result");
        Button invokeFileDialog = new Button(
                "Open owner file dialog", event -> {
                    boolean fromClient = event.isFromClient();
                    DesktopCapabilityContext capabilityContext = Desktop.current()
                            .captureCapabilityContext();
                    Thread.ofVirtual().name("file-dialog-owner-close").start(() -> {
                        boolean backgroundSessionAbsent =
                                VaadinSession.getCurrent() == null;
                        DesktopFileDialogResult result = capabilityContext
                                .fileDialog().openFileResult(DIALOG_TITLE);
                        String outcome = "outcome=" + result.outcome()
                                + " selected="
                                + result.selectedPath().map(Object::toString)
                                        .orElse("none")
                                + " cancelReason="
                                + result.cancelReason().orElse("none")
                                + " operationInvalidated="
                                + result.commandResult().payload().getOrDefault(
                                        "operationInvalidated", false)
                                + " capabilityContextCaptured=true"
                                + " backgroundSessionAbsent="
                                + backgroundSessionAbsent
                                + " secondaryClient=" + fromClient
                                + " change="
                                + resultChangeCount.incrementAndGet();
                        latestJavaResult.set(outcome);
                    });
                });
        invokeFileDialog.setId(
                "framework-flow-file-dialog-owner-close-invoke");

        Span healthStatus = new Span("Owner close main health: pending");
        healthStatus.setId("framework-flow-file-dialog-owner-close-health-status");
        Button verifyMainHealth = new Button(
                "Verify main after owner close", event -> healthStatus.setText(
                        "Owner close main health: result=" + latestJavaResult.get()
                                + " client=" + event.isFromClient()
                                + " change=" + healthChangeCount.incrementAndGet()));
        verifyMainHealth.setId(
                "framework-flow-file-dialog-owner-close-health");

        add(new H1("Flow file dialog owner-window close acceptance"),
                openSecondary,
                closeSecondary,
                windowStatus,
                invokeFileDialog,
                javaResult,
                verifyMainHealth,
                healthStatus);
    }

    private static String errorValue(DesktopCommandResult result) {
        String error = result.error();
        return error == null || error.isBlank() ? "none" : error;
    }
}
