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

/** Main-window controls for the existing public typed write-grant route. */
@Route("flow-file-write-grant-recovery")
public final class FlowFileWriteGrantRecoveryRoute extends VerticalLayout {
    static final String SECONDARY_WINDOW_ID = "file-write-grant-recovery-secondary";
    private static final String PREFIX = "framework-flow-file-write-grant-recovery-";

    public FlowFileWriteGrantRecoveryRoute() {
        setId(PREFIX + "route");
        setPadding(true);
        setSpacing(true);

        Span openStatus = pending("open-secondary");
        AtomicInteger openChanges = new AtomicInteger();
        AtomicInteger openListeners = new AtomicInteger();
        Button open = new Button("Open file write grant recovery window", event -> {
            int listenerCount = openListeners.incrementAndGet();
            event.getSource().setEnabled(false);
            DesktopCommandResult result = Desktop.current().windows().open(
                    DesktopWindow.builder(SECONDARY_WINDOW_ID)
                            .title("Flow file write grant recovery")
                            .size(1000, 900)
                            .route("flow-file-write-grant")
                            .build());
            openStatus.setText("open-secondary: opened=" + result.success()
                    + " client=" + event.isFromClient()
                    + " change=" + openChanges.incrementAndGet()
                    + " listenerCount=" + listenerCount);
        });
        open.setId(PREFIX + "open-secondary");

        Span closeStatus = pending("close-secondary");
        AtomicInteger closeChanges = new AtomicInteger();
        AtomicInteger closeListeners = new AtomicInteger();
        Button close = new Button("Close file write grant recovery window", event -> {
            int listenerCount = closeListeners.incrementAndGet();
            event.getSource().setEnabled(false);
            // A recovered JVM gets a new view while the native owner survives.
            // Closing therefore does not depend on this instance having opened it.
            DesktopCommandResult result = Desktop.current().windows().close(SECONDARY_WINDOW_ID);
            closeStatus.setText("close-secondary: closed=" + result.success()
                    + " client=" + event.isFromClient()
                    + " change=" + closeChanges.incrementAndGet()
                    + " listenerCount=" + listenerCount);
        });
        close.setId(PREFIX + "close-secondary");
        add(new H1("Flow file write grant recovery"), open, openStatus, close, closeStatus);
    }

    private static Span pending(String operation) {
        Span status = new Span(operation + ": pending");
        status.setId(PREFIX + operation + "-status");
        return status;
    }
}
