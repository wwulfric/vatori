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
import dev.vatori.desktop.DesktopCommand;
import dev.vatori.desktop.DesktopCommandResult;
import dev.vatori.desktop.NativeBridge;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Captures the clicking window's public raw bridge, not an owner-bound grant
 * facade. The actual selected token remains in private process-local state.
 */
@Route("flow-file-write-grant-isolation")
public final class FlowFileWriteGrantIsolationRoute extends VerticalLayout {
    static final String SECONDARY_WINDOW_ID = "file-write-grant-isolation-secondary";
    static final String PANEL_TITLE = "Flow file write grant owner isolation";
    private static final String PREFIX = "framework-flow-file-write-grant-isolation-";
    private static final String FILENAME = "vatori-file-write-grant-fixture.bin";
    private static final byte[] TAIL = {0, -1, 1, -2, 42, 10};
    private static final byte[] FOREIGN_BYTES = "GUEST!".getBytes(StandardCharsets.US_ASCII);
    private static final AtomicReference<Run> ACTIVE_RUN = new AtomicReference<>();

    private volatile Run mainRun;
    private volatile Run ownerRun;

    public FlowFileWriteGrantIsolationRoute() {
        setId(PREFIX + "route");
        setPadding(true);
        setSpacing(true);
        add(new H1("Flow file write grant owner isolation"));
        operation("secondary open", "open-secondary", this::openSecondary);
        operation("selection", "select", this::select);
        operation("owner write first", "owner-write-first",
                bridge -> useGrant(bridge, 1, true, false));
        operation("foreign write", "foreign-write",
                bridge -> useGrant(bridge, 2, false, false));
        operation("foreign release", "foreign-release",
                bridge -> useGrant(bridge, 3, false, true));
        operation("owner write tail", "owner-write-tail",
                bridge -> useGrant(bridge, 4, true, false));
        operation("owner release", "owner-release",
                bridge -> useGrant(bridge, 5, true, true));
        operation("secondary close", "close-secondary", this::closeSecondary);
        addDetachListener(event -> {
            clearRun(mainRun);
            clearRun(ownerRun);
        });
    }

    private void operation(String name, String suffix, StageWork work) {
        Span status = new Span(label(name) + "pending");
        status.setId(PREFIX + suffix + "-status");
        AtomicInteger changes = new AtomicInteger();
        AtomicInteger listeners = new AtomicInteger();
        Button button = new Button("Invoke " + name, event -> {
            int listenerCount = listeners.incrementAndGet();
            event.getSource().setEnabled(false);
            UI ui = event.getSource().getUI().orElseThrow();
            boolean client = event.isFromClient();
            NativeBridge bridge = Desktop.current().nativeBridge()
                    .captureCapabilityContext();
            Thread.ofVirtual().name("file-write-grant-isolation-" + suffix).start(() -> {
                boolean background = VaadinSession.getCurrent() == null;
                String fields;
                try {
                    fields = work.invoke(bridge);
                } catch (RuntimeException ignored) {
                    // Exceptions can contain native payloads; never render them.
                    fields = "outcome=ERROR fixtureError=true";
                }
                String text = label(name) + fields
                        + " capabilityContextCaptured=true backgroundSessionAbsent=" + background
                        + " client=" + client + " change=" + changes.incrementAndGet()
                        + " listenerCount=" + listenerCount;
                try {
                    ui.access(() -> status.setText(text));
                } catch (UIDetachedException ignored) {
                    // Native owner teardown owns grant cleanup, not this view.
                }
            });
        });
        button.setId(PREFIX + suffix);
        add(button, status);
    }

    private String openSecondary(NativeBridge bridge) {
        Run run = new Run();
        if (!ACTIVE_RUN.compareAndSet(null, run)) {
            return "opened=false permissionAllowed=false";
        }
        mainRun = run;
        boolean opened = false;
        try {
            DesktopCommandResult result = bridge.invoke(new DesktopCommand("window.open",
                    Map.of("id", SECONDARY_WINDOW_ID, "title", PANEL_TITLE,
                            "width", 1000, "height", 900,
                            "route", "flow-file-write-grant-isolation")));
            opened = result.success() && permissionAllowed(result)
                    && SECONDARY_WINDOW_ID.equals(result.payload().get("windowId"))
                    && "flow-file-write-grant-isolation".equals(result.payload().get("route"));
            return "opened=" + opened + " permissionAllowed=" + permissionAllowed(result);
        } finally {
            if (!opened) {
                clearRun(run);
            }
        }
    }

    private String select(NativeBridge bridge) {
        Run run = ACTIVE_RUN.get();
        if (run == null || !run.beginSelection()) {
            throw invalid();
        }
        ownerRun = run;
        DesktopCommandResult result = bridge.invoke(new DesktopCommand(
                "fileDialog.saveNewWriteGrant",
                Map.of("title", PANEL_TITLE, "suggestedName", FILENAME)));
        Map<String, Object> payload = result.payload();
        String token = canonicalUuid(payload.get("grantId"));
        boolean filenameMatches = FILENAME.equals(payload.get("filename"));
        boolean selected = result.success() && token != null
                && Boolean.FALSE.equals(payload.get("canceled"))
                && Boolean.TRUE.equals(payload.get("grantPresent"))
                && Boolean.TRUE.equals(payload.get("createNewOnly"))
                && Boolean.TRUE.equals(payload.get("usedSavePanel"))
                && Boolean.TRUE.equals(payload.get("securityScoped"))
                && number(payload, "maxWriteBytes") == 65_536 && filenameMatches
                && permissionAllowed(result) && noPath(payload)
                && !payload.containsKey("error") && !payload.containsKey("errorCode")
                && ACTIVE_RUN.get() == run && run.select(token);
        return "outcome=" + (selected ? "SELECTED" : "ERROR")
                + " grantPresent=" + selected
                + " maxWriteBytes=" + number(payload, "maxWriteBytes")
                + " securityScoped=" + Boolean.TRUE.equals(payload.get("securityScoped"))
                + " filenameMatches=" + filenameMatches
                + " permissionAllowed=" + permissionAllowed(result)
                + " noPath=" + noPath(payload)
                + " createNewOnly=" + Boolean.TRUE.equals(payload.get("createNewOnly"));
    }

    private String useGrant(NativeBridge bridge, int step, boolean owner, boolean release) {
        Run run = owner ? ownerRun : mainRun;
        String token = run == null ? null : run.claim(step);
        if (token == null || ACTIVE_RUN.get() != run) {
            throw invalid();
        }
        long offset = step == 1 ? 0 : 65_536;
        byte[] bytes = step == 1 ? firstBlock() : owner ? TAIL.clone() : FOREIGN_BYTES.clone();
        DesktopCommand command = new DesktopCommand(
                release ? "fileGrant.releaseWrite" : "fileGrant.write",
                release ? Map.of("grantId", token)
                        : Map.of("grantId", token, "offset", offset,
                                "bodyBase64", Base64.getEncoder().encodeToString(bytes)));
        boolean sameTokenUsed = token.equals(command.payload().get("grantId"))
                && run.matches(token);
        DesktopCommandResult result = bridge.invoke(command);
        if (!owner) {
            return foreignStatus(run, token, step, result, sameTokenUsed, release);
        }
        if (release) {
            return releaseStatus(run, token, result, sameTokenUsed);
        }
        return writeStatus(run, token, step, result, sameTokenUsed, offset, bytes.length);
    }

    private static String foreignStatus(Run run, String token, int step,
            DesktopCommandResult result, boolean sameTokenUsed, boolean release) {
        Map<String, Object> payload = result.payload();
        boolean noContent = noPath(payload) && !payload.containsKey("bodyBase64")
                && !payload.containsKey("bodyEncoding") && !payload.containsKey("grantId")
                && !payload.containsKey("filename");
        boolean noWriteProgress = !payload.containsKey("offset")
                && !payload.containsKey("bytesWritten") && !payload.containsKey("nextOffset");
        boolean unchanged = run.matches(token);
        boolean unavailable = !result.success()
                && Boolean.TRUE.equals(payload.get("grantUnavailable"))
                && "unavailable".equals(payload.get("grantState"))
                && "grant-unavailable".equals(payload.get("errorCode"))
                && permissionAllowed(result) && noContent && noWriteProgress
                && sameTokenUsed && unchanged && run.completeStep(token, step);
        return "outcome=" + (unavailable ? "UNAVAILABLE" : "ERROR")
                + " unavailable=" + unavailable
                + " grantUnavailable=" + Boolean.TRUE.equals(payload.get("grantUnavailable"))
                + " permissionAllowed=" + permissionAllowed(result)
                + " noContent=" + noContent + " noWriteProgress=" + noWriteProgress
                + " sameTokenUsed=" + sameTokenUsed + " ownerTokenUnchanged=" + unchanged
                + (release ? "" : " requestedOffset=65536 requestedBytes=6");
    }

    private static String writeStatus(Run run, String token, int step,
            DesktopCommandResult result, boolean sameTokenUsed, long offset, int count) {
        Map<String, Object> payload = result.payload();
        boolean written = result.success() && sameTokenUsed && permissionAllowed(result)
                && Boolean.FALSE.equals(payload.get("grantUnavailable"))
                && "active".equals(payload.get("grantState")) && noPath(payload)
                && number(payload, "maxWriteBytes") == 65_536
                && number(payload, "offset") == offset
                && number(payload, "bytesWritten") == count
                && number(payload, "nextOffset") == offset + count
                && !payload.containsKey("error") && !payload.containsKey("errorCode")
                && run.written(token, step, offset + count);
        return "outcome=" + (written ? "WRITTEN" : "ERROR")
                + " success=" + result.success() + " sameTokenUsed=" + sameTokenUsed
                + " permissionAllowed=" + permissionAllowed(result)
                + " requestedOffset=" + offset + " requestedBytes=" + count
                + " bytesWritten=" + number(payload, "bytesWritten")
                + " offset=" + number(payload, "offset")
                + " nextOffset=" + number(payload, "nextOffset")
                + " maxWriteBytes=" + number(payload, "maxWriteBytes")
                + " grantUnavailable=" + Boolean.TRUE.equals(payload.get("grantUnavailable"))
                + " noPath=" + noPath(payload);
    }

    private static String releaseStatus(Run run, String token,
            DesktopCommandResult result, boolean sameTokenUsed) {
        Map<String, Object> payload = result.payload();
        boolean released = result.success() && sameTokenUsed && permissionAllowed(result)
                && Boolean.TRUE.equals(payload.get("released"))
                && Boolean.TRUE.equals(payload.get("grantUnavailable"))
                && "unavailable".equals(payload.get("grantState"))
                && Boolean.TRUE.equals(payload.get("securityScopeStopped"))
                && number(payload, "nextOffset") == 65_542 && noPath(payload)
                && !payload.containsKey("error") && !payload.containsKey("errorCode")
                && run.release(token);
        return "outcome=" + (released ? "RELEASED" : "ERROR")
                + " success=" + result.success() + " sameTokenUsed=" + sameTokenUsed
                + " permissionAllowed=" + permissionAllowed(result)
                + " released=" + released
                + " securityScopeStopped=" + Boolean.TRUE.equals(payload.get("securityScopeStopped"))
                + " nextOffset=" + number(payload, "nextOffset")
                + " tokenCleared=" + run.tokenCleared();
    }

    private String closeSecondary(NativeBridge bridge) {
        Run run = mainRun;
        if (run == null || ACTIVE_RUN.get() != run || !run.released()) {
            throw invalid();
        }
        DesktopCommandResult result = bridge.invoke(new DesktopCommand(
                "window.close", Map.of("id", SECONDARY_WINDOW_ID)));
        boolean closed = result.success() && permissionAllowed(result)
                && SECONDARY_WINDOW_ID.equals(result.payload().get("windowId"));
        if (closed) {
            clearRun(run);
        }
        return "closed=" + closed + " permissionAllowed=" + permissionAllowed(result)
                + " sharedStateCleared=" + (closed && ACTIVE_RUN.get() != run && run.cleared());
    }

    private static String label(String name) {
        return "File write grant isolation " + name + ": ";
    }

    private static boolean permissionAllowed(DesktopCommandResult result) {
        return "allowed".equals(result.payload().get("permissionDecision"));
    }

    private static boolean noPath(Map<String, Object> payload) {
        return Boolean.FALSE.equals(payload.get("pathExposed"))
                && !payload.containsKey("path") && !payload.containsKey("paths")
                && !payload.containsKey("url");
    }

    private static long number(Map<String, Object> payload, String field) {
        Object value = payload.get(field);
        return value instanceof Byte || value instanceof Short
                || value instanceof Integer || value instanceof Long
                ? ((Number) value).longValue() : -1;
    }

    private static String canonicalUuid(Object value) {
        if (!(value instanceof String text)) {
            return null;
        }
        try {
            return UUID.fromString(text).toString().equals(text) ? text : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static byte[] firstBlock() {
        byte[] bytes = new byte[65_536];
        for (int index = 0; index < bytes.length; index++) {
            bytes[index] = (byte) (index % 251);
        }
        return bytes;
    }

    private static IllegalStateException invalid() {
        return new IllegalStateException("File write grant isolation state is invalid");
    }

    private static void clearRun(Run run) {
        if (run != null) {
            ACTIVE_RUN.compareAndSet(run, null);
            run.clear();
        }
    }

    @FunctionalInterface
    private interface StageWork {
        String invoke(NativeBridge bridge);
    }

    /** Contains no UI, session or bridge; a step advances only after its result. */
    private static final class Run {
        private String grantId;
        private int nextStep;
        private long writtenBytes;
        private boolean inFlight;
        private boolean released;
        private boolean cleared;

        synchronized boolean beginSelection() {
            if (cleared || inFlight || nextStep != 0 || grantId != null) {
                return false;
            }
            inFlight = true;
            return true;
        }

        synchronized boolean select(String token) {
            if (cleared || !inFlight || nextStep != 0 || grantId != null) {
                return false;
            }
            grantId = token;
            nextStep = 1;
            inFlight = false;
            return true;
        }

        synchronized String claim(int step) {
            if (cleared || released || inFlight || nextStep != step || grantId == null) {
                return null;
            }
            inFlight = true;
            return grantId;
        }

        synchronized boolean matches(String token) {
            return !cleared && token.equals(grantId);
        }

        synchronized boolean completeStep(String token, int step) {
            if (!matches(token) || !inFlight || nextStep != step) {
                return false;
            }
            nextStep++;
            inFlight = false;
            return true;
        }

        synchronized boolean written(String token, int step, long nextOffset) {
            if (!(step == 1 && writtenBytes == 0 && nextOffset == 65_536
                    || step == 4 && writtenBytes == 65_536 && nextOffset == 65_542)
                    || !completeStep(token, step)) {
                return false;
            }
            writtenBytes = nextOffset;
            return true;
        }

        synchronized boolean release(String token) {
            if (writtenBytes != 65_542 || !completeStep(token, 5)) {
                return false;
            }
            grantId = null;
            released = true;
            return true;
        }

        synchronized boolean released() {
            return !cleared && released && nextStep == 6 && writtenBytes == 65_542;
        }

        synchronized boolean tokenCleared() {
            return released && grantId == null;
        }

        synchronized boolean cleared() {
            return cleared && grantId == null;
        }

        synchronized void clear() {
            grantId = null;
            inFlight = false;
            cleared = true;
        }
    }
}
