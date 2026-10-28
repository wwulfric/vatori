package dev.vatori.ipcframework;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UIDetachedException;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import dev.vatori.desktop.Desktop;
import dev.vatori.desktop.DesktopCapabilityContext;
import dev.vatori.desktop.DesktopCommandResult;
import dev.vatori.desktop.DesktopFileGrantReleaseResult;
import dev.vatori.desktop.DesktopFileWriteGrant;
import dev.vatori.desktop.DesktopFileWriteGrantResult;
import dev.vatori.desktop.DesktopFileWriteCancelResult;
import dev.vatori.desktop.DesktopFileWriteResult;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Exercises only the public owner-bound, create-new-only write API. */
@Route("flow-file-write-grant")
public final class FlowFileWriteGrantRoute extends VerticalLayout {
    private static final String PREFIX = "framework-flow-file-write-grant-";
    private static final String FILENAME = "vatori-file-write-grant-fixture.bin";
    private static final byte[] TAIL = {0, -1, 1, -2, 42, 10};
    private final AtomicInteger sequence = new AtomicInteger();
    private final AtomicReference<DesktopFileWriteGrant> selectedGrant =
            new AtomicReference<>();
    private final AtomicReference<DesktopFileWriteGrant> preparedGrant =
            new AtomicReference<>();
    private final AtomicInteger closeActiveListeners = new AtomicInteger();
    private final Button closeActive = new Button("Close owner with active write grant");
    private volatile boolean capabilityContextCaptured;
    private volatile long writtenByteCount;

    public FlowFileWriteGrantRoute() {
        setId(PREFIX + "route");
        setPadding(true);
        setSpacing(true);
        add(new H1("Flow create-new file write grant"));
        operation("select-new", 0, true, context -> select(context,
                "Flow file write grant new file", FILENAME, true));
        operation("write-first", 1, false, ignored -> write(0, firstBlock()));
        operation("invalid-range", 2, false, ignored -> write(-1, new byte[]{1}));
        operation("write-tail", 3, false, ignored -> write(65_536, TAIL));
        operation("release", 4, false, ignored -> release());
        operation("write-after-release", 5, false,
                ignored -> write(65_542, new byte[]{1}));
        operation("select-existing", 6, true, context -> select(context,
                "Flow file write grant existing target", FILENAME, false));
        operation("select-canceled", 7, true, context -> select(context,
                "Flow file write grant canceled", "canceled-write-grant.bin", false));
        closeActive.setId(PREFIX + "close-active");
        closeActive.setEnabled(false);
        closeActive.getElement().addEventListener("click", event -> {
            int listenerCount = closeActiveListeners.incrementAndGet();
            boolean trusted = event.getEventData().path("event.isTrusted").asBoolean(false);
            boolean retained = selectedGrant.get() != null
                    && preparedGrant.get() == selectedGrant.get();
            boolean prepared = preparedToClose();
            if (!trusted || !prepared || !retained || listenerCount != 1
                    || !sequence.compareAndSet(4, 9)) {
                return;
            }
            closeActive.setEnabled(false);
            // This title round trip records the trusted listener and retained
            // grant before the owner disappears; no final DOM patch is needed.
            if (Desktop.current().setWindowTitle(
                    "Write grant owner close trusted=true prepared=true retained=true listenerCount=1 bytes=65542")
                    .success()) {
                Desktop.current().windows().close("file-write-owner");
            }
        }).addEventData("event.isTrusted");
        add(closeActive);
        // An independent terminal branch after the same four bounded-write
        // stages. Existing release and panel-cancellation observations stay intact.
        operation("cancel", 4, false, ignored -> cancel());
        operation("cancel-again", 5, false, ignored -> cancel());
        operation("write-after-cancel", 6, false,
                ignored -> write(65_542, new byte[]{1}));
    }

    private void operation(String stage, int expectedSequence, boolean capture,
            StageWork work) {
        Span status = new Span(stage + ": pending");
        status.setId(PREFIX + stage + "-status");
        AtomicInteger changes = new AtomicInteger();
        AtomicInteger listeners = new AtomicInteger();
        Button button = new Button("Invoke write grant " + stage, event -> {
            int listenerCount = listeners.incrementAndGet();
            event.getSource().setEnabled(false);
            UI ui = event.getSource().getUI().orElseThrow();
            boolean client = event.isFromClient();
            DesktopCapabilityContext context = capture
                    ? Desktop.current().captureCapabilityContext() : null;
            if (context != null) {
                capabilityContextCaptured = true;
            }
            Thread.ofVirtual().name("file-write-grant-" + stage).start(() -> {
                boolean background = VaadinSession.getCurrent() == null;
                String result;
                try {
                    // Existing-target rejection is independently exercised by
                    // the host smoke; the real-panel runner may skip that stage.
                    boolean ordered = expectedSequence == 7
                            && sequence.compareAndSet(6, 8);
                    if (!ordered && !sequence.compareAndSet(
                            expectedSequence, expectedSequence + 1)) {
                        throw new IllegalStateException("Out-of-order fixture operation");
                    }
                    result = work.invoke(context);
                } catch (RuntimeException ignored) {
                    result = "outcome=ERROR fixtureError=true";
                }
                String completed = stage + ": " + result
                        + " capabilityContextCaptured=" + capabilityContextCaptured
                        + " backgroundSessionAbsent=" + background
                        + " client=" + client
                        + " change=" + changes.incrementAndGet()
                        + " listenerCount=" + listenerCount;
                try {
                    ui.access(() -> {
                        status.setText(completed);
                        closeActive.setEnabled(preparedToClose());
                    });
                } catch (UIDetachedException ignored) {
                    // Host owner teardown closes the grant without deleting it.
                }
            });
        });
        button.setId(PREFIX + stage);
        add(button, status);
    }

    private String select(DesktopCapabilityContext context, String title,
            String suggestedName, boolean retain) {
        DesktopFileWriteGrant original = selectedGrant.get();
        DesktopFileWriteGrantResult result = context.fileDialog()
                .saveNewWriteGrant(title, suggestedName);
        DesktopFileWriteGrant grant = result.grant().orElse(null);
        if (retain && grant != null && !selectedGrant.compareAndSet(null, grant)) {
            grant.release();
            throw new IllegalStateException("Duplicate fixture grant");
        }
        if (!retain && grant != null) {
            // An unexpected success is still visibly SELECTED, but does not
            // leak an extra host handle or replace the original grant facade.
            grant.release();
        }
        Map<String, Object> payload = result.commandResult().payload();
        return "outcome=" + result.outcome().name() + metadata(grant)
                + " permissionAllowed=" + permissionAllowed(result.commandResult())
                + " publicTokenHidden=" + !payload.containsKey("grantId")
                + " noPath=" + (Boolean.FALSE.equals(payload.get("pathExposed"))
                        && !payload.containsKey("path") && !payload.containsKey("paths")
                        && !payload.containsKey("url"))
                + " createNewOnly=" + Boolean.TRUE.equals(payload.get("createNewOnly"))
                + " originalGrantRetained=" + (retain
                        ? grant != null && selectedGrant.get() == grant
                        : original != null && selectedGrant.get() == original);
    }

    private String write(long offset, byte[] bytes) {
        DesktopFileWriteGrant grant = requireGrant();
        DesktopFileWriteResult result = grant.write(offset, bytes);
        if (result.outcome() == DesktopFileWriteResult.Outcome.WRITTEN) {
            writtenByteCount = result.nextOffset();
            if (offset == 65_536 && bytes.length == TAIL.length
                    && result.bytesWritten() == TAIL.length
                    && result.nextOffset() == 65_542 && sequence.get() == 4) {
                preparedGrant.set(grant);
            }
        }
        return "outcome=" + result.outcome().name()
                + " requestedOffset=" + offset + " requestedBytes=" + bytes.length
                + " bytesWritten=" + result.bytesWritten()
                + " offset=" + result.offset() + " nextOffset=" + result.nextOffset()
                + " sameGrantFacade=" + (grant == selectedGrant.get())
                + " permissionAllowed=" + permissionAllowed(result.commandResult())
                + " publicTokenHidden=" + !result.commandResult().payload().containsKey("grantId")
                + metadata(grant);
    }

    private String release() {
        DesktopFileWriteGrant grant = requireGrant();
        DesktopFileGrantReleaseResult result = grant.release();
        Map<String, Object> payload = result.commandResult().payload();
        return "outcome=" + result.outcome().name()
                + " released=" + (result.outcome() == DesktopFileGrantReleaseResult.Outcome.RELEASED
                        && Boolean.TRUE.equals(payload.get("released")))
                + " sameGrantFacade=" + (grant == selectedGrant.get())
                + " permissionAllowed=" + permissionAllowed(result.commandResult())
                + " publicTokenHidden=" + !payload.containsKey("grantId")
                + " securityScopeStopped=" + Boolean.TRUE.equals(payload.get("securityScopeStopped"))
                + metadata(grant);
    }

    private String cancel() {
        DesktopFileWriteGrant grant = requireGrant();
        DesktopFileWriteCancelResult result = grant.cancel();
        Map<String, Object> payload = result.commandResult().payload();
        // AutoCloseable cleanup is harmless after a successful cancellation;
        // it must not submit a second release or cancellation command.
        if (result.outcome() == DesktopFileWriteCancelResult.Outcome.CANCELED
                || result.outcome() == DesktopFileWriteCancelResult.Outcome.ALREADY_CANCELED) {
            grant.close();
        }
        return "outcome=" + result.outcome().name()
                + " canceled=" + Boolean.TRUE.equals(payload.get("canceled"))
                + " cancellationRequested=" + Boolean.TRUE.equals(payload.get("cancellationRequested"))
                + " grantUnavailable=" + Boolean.TRUE.equals(payload.get("grantUnavailable"))
                + " grantState=" + payload.get("grantState")
                + " nextOffset=" + result.nextOffset()
                + " sameGrantFacade=" + (grant == selectedGrant.get())
                + " permissionAllowed=" + permissionAllowed(result.commandResult())
                + " publicTokenHidden=" + !payload.containsKey("grantId")
                + " noPath=" + (Boolean.FALSE.equals(payload.get("pathExposed"))
                        && !payload.containsKey("path") && !payload.containsKey("paths")
                        && !payload.containsKey("url"))
                + " securityScopeStopped=" + Boolean.TRUE.equals(payload.get("securityScopeStopped"))
                + metadata(grant);
    }

    private DesktopFileWriteGrant requireGrant() {
        DesktopFileWriteGrant grant = selectedGrant.get();
        if (grant == null) {
            throw new IllegalStateException("Missing fixture grant");
        }
        return grant;
    }

    private boolean preparedToClose() {
        return sequence.get() == 4 && writtenByteCount == 65_542
                && selectedGrant.get() != null
                && preparedGrant.get() == selectedGrant.get();
    }

    private static boolean permissionAllowed(DesktopCommandResult result) {
        return "allowed".equals(result.payload().get("permissionDecision"));
    }

    private static String metadata(DesktopFileWriteGrant grant) {
        return " grantPresent=" + (grant != null)
                + " maxWriteBytes=" + (grant == null ? -1 : grant.maxWriteBytes())
                + " securityScoped=" + (grant != null && grant.securityScoped())
                + " filenameMatches=" + (grant != null && FILENAME.equals(grant.filename()));
    }

    private static byte[] firstBlock() {
        byte[] bytes = new byte[65_536];
        for (int index = 0; index < bytes.length; index++) {
            bytes[index] = (byte) (index % 251);
        }
        return bytes;
    }

    @FunctionalInterface
    private interface StageWork {
        String invoke(DesktopCapabilityContext context);
    }
}
