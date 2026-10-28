package dev.vatori.ipcframework;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import dev.vatori.desktop.Desktop;
import dev.vatori.desktop.DesktopCommandResult;
import dev.vatori.desktop.DesktopWindow;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

@Route("flow-main-window-close-recovery")
public final class FlowMainWindowCloseRecoveryRoute extends VerticalLayout {
    static final String SECONDARY_WINDOW_ID = "main-close-recovery-secondary";
    static final String RECOVERED_SECONDARY_TITLE =
            "Flow main-close recovery secondary READY";
    static final String GHOST_MAIN_TITLE =
            "Ghost main capability must not reach host";

    private final AtomicInteger probeChangeCount = new AtomicInteger();

    public FlowMainWindowCloseRecoveryRoute() {
        setId("framework-flow-main-window-close-recovery-route");
        setPadding(true);
        setSpacing(true);

        Span windowStatus = new Span("Main close recovery window: pending");
        windowStatus.setId(
                "framework-flow-main-window-close-recovery-window-status");

        Button openSecondary = new Button(
                "Open recovery secondary", event -> {
                    DesktopCommandResult result = Desktop.current().windows()
                            .open(DesktopWindow.builder(SECONDARY_WINDOW_ID)
                                    .title("Flow main-close recovery secondary")
                                    .size(760, 520)
                                    .route("flow-main-window-close-recovery")
                                    .build());
                    windowStatus.setText(
                            "Main close recovery window: opened="
                                    + result.success()
                                    + " client=" + event.isFromClient()
                                    + " error=" + errorValue(result));
                });
        openSecondary.setId(
                "framework-flow-main-window-close-recovery-open-window");

        Button closeMain = new Button(
                "Close main window", event -> {
                    DesktopCommandResult result = Desktop.current().windows()
                            .close("main");
                    windowStatus.setText(
                            "Main close recovery window: mainClosing="
                                    + result.success()
                                    + " client=" + event.isFromClient()
                                    + " error=" + errorValue(result));
                });
        closeMain.setId(
                "framework-flow-main-window-close-recovery-close-main");

        Span probeStatus = new Span("Main close recovery probe: pending");
        probeStatus.setId(
                "framework-flow-main-window-close-recovery-probe-status");
        Button runProbe = new Button(
                "Verify closed main after recovery", event -> {
                    CompletableFuture<BackgroundProbe> future =
                            CompletableFuture.supplyAsync(() ->
                                    new BackgroundProbe(
                                            VaadinSession.getCurrent() != null,
                                            Desktop.current().setWindowTitle(
                                                    GHOST_MAIN_TITLE)));
                    BackgroundProbe background = null;
                    boolean timedOut = false;
                    try {
                        background = future.get(2, TimeUnit.SECONDS);
                    } catch (TimeoutException error) {
                        timedOut = true;
                        future.cancel(true);
                    } catch (InterruptedException error) {
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException ignored) {
                        // The exact failed state is rendered below for acceptance.
                    }

                    DesktopCommandResult backgroundResult = background == null
                            ? null : background.result();
                    boolean backgroundSessionPresent = background != null
                            && background.sessionPresent();
                    boolean backgroundSuccess = backgroundResult != null
                            && backgroundResult.success();
                    boolean backgroundCanceled = backgroundResult != null
                            && Boolean.TRUE.equals(backgroundResult.payload()
                            .get("canceled"));
                    boolean backgroundInvalidated = backgroundResult != null
                            && Boolean.TRUE.equals(backgroundResult.payload()
                            .get("operationInvalidated"));
                    String backgroundReason = backgroundResult == null
                            ? "none"
                            : String.valueOf(backgroundResult.payload()
                            .getOrDefault("cancelReason", "none"));

                    DesktopCommandResult secondaryResult = Desktop.current()
                            .setWindowTitle(RECOVERED_SECONDARY_TITLE);
                    probeStatus.setText(
                            "Main close recovery probe: backgroundSession="
                                    + backgroundSessionPresent
                                    + " backgroundSuccess=" + backgroundSuccess
                                    + " backgroundCanceled=" + backgroundCanceled
                                    + " backgroundInvalidated="
                                    + backgroundInvalidated
                                    + " backgroundReason=" + backgroundReason
                                    + " backgroundTimedOut=" + timedOut
                                    + " secondarySuccess="
                                    + secondaryResult.success()
                                    + " secondaryError="
                                    + errorValue(secondaryResult)
                                    + " client=" + event.isFromClient()
                                    + " change="
                                    + probeChangeCount.incrementAndGet());
                });
        runProbe.setId(
                "framework-flow-main-window-close-recovery-probe");

        add(new H1("Flow main-window close recovery acceptance"),
                openSecondary,
                closeMain,
                windowStatus,
                runProbe,
                probeStatus);
    }

    private static String errorValue(DesktopCommandResult result) {
        String error = result.error();
        return error == null || error.isBlank() ? "none" : error;
    }

    private record BackgroundProbe(
            boolean sessionPresent,
            DesktopCommandResult result) {
    }
}
