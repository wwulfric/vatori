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
import dev.vatori.desktop.DesktopWindow;
import dev.vatori.desktop.NativeBridge;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Deliberately uses the public raw bridge: the owner-bound grant facade would
 * preserve the original owner and could not exercise a foreign window request.
 * The token is confined to private test-app state, never status text or logs.
 */
@Route("flow-file-read-grant-isolation")
public final class FlowFileReadGrantIsolationRoute extends VerticalLayout {
    static final String SECONDARY_WINDOW_ID =
            "file-read-grant-isolation-secondary";
    static final String PANEL_TITLE = "Flow file read grant owner isolation";
    private static final String ID_PREFIX =
            "framework-flow-file-read-grant-isolation-";
    private static final int READ_BYTES = 19;
    private static final byte[] EXPECTED_READ = Arrays.copyOf(
            FlowFileReadGrantRoute.FIXTURE_CONTENT.getBytes(
                    StandardCharsets.UTF_8), READ_BYTES);
    private static final AtomicReference<Run> ACTIVE_RUN = new AtomicReference<>();

    private Run mainRun;
    private Run ownerRun;

    public FlowFileReadGrantIsolationRoute() {
        setId(ID_PREFIX + "route");
        setPadding(true);
        setSpacing(true);
        add(new H1("Flow file read grant owner isolation"));

        Span openStatus = pending("secondary open", "open-secondary");
        AtomicInteger openChanges = new AtomicInteger();
        Button open = new Button("Open file grant isolation window", event -> {
            event.getSource().setEnabled(false);
            Run run = new Run();
            boolean installed = ACTIVE_RUN.compareAndSet(null, run);
            boolean opened = false;
            if (installed) {
                mainRun = run;
                opened = Desktop.current().windows().open(
                        DesktopWindow.builder(SECONDARY_WINDOW_ID)
                                .title("Flow file grant owner isolation")
                                .size(760, 620)
                                .route("flow-file-read-grant-isolation")
                                .build()).success();
                if (!opened) {
                    clearRun(run);
                }
            }
            openStatus.setText(label("secondary open") + "opened=" + opened
                    + " client=" + event.isFromClient()
                    + " change=" + openChanges.incrementAndGet());
        });
        open.setId(ID_PREFIX + "open-secondary");
        add(open, openStatus);

        Span selectionStatus = pending("selection", "select");
        AtomicInteger selectionChanges = new AtomicInteger();
        Button select = new Button("Select file grant owner", event -> {
            event.getSource().setEnabled(false);
            boolean client = event.isFromClient();
            UI ui = event.getSource().getUI().orElseThrow();
            NativeBridge bridge = Desktop.current().nativeBridge()
                    .captureCapabilityContext();
            Run run = ACTIVE_RUN.get();
            ownerRun = run;
            Thread.ofVirtual().name("file-grant-isolation-select").start(() -> {
                boolean background = VaadinSession.getCurrent() == null;
                String status;
                try {
                    DesktopCommandResult result = bridge.invoke(
                            new DesktopCommand("fileDialog.openReadGrant",
                                    Map.of("title", PANEL_TITLE)));
                    Map<String, Object> payload = result.payload();
                    String token = canonicalUuid(payload.get("grantId"));
                    boolean noPath = Boolean.FALSE.equals(payload.get("pathExposed"))
                            && !payload.containsKey("path")
                            && !payload.containsKey("url");
                    boolean selected = result.success() && token != null
                            && Boolean.FALSE.equals(payload.get("canceled"))
                            && Boolean.TRUE.equals(payload.get("grantPresent"))
                            && number(payload, "byteCount") == 48
                            && number(payload, "maxReadBytes") == 65_536
                            && payload.get("securityScoped") instanceof Boolean
                            && permissionAllowed(result) && noPath
                            && run != null && ACTIVE_RUN.get() == run
                            && run.select(token);
                    status = label("selection")
                            + "outcome=" + (selected ? "SELECTED" : "ERROR")
                            + " grantPresent=" + selected
                            + " byteCount=" + number(payload, "byteCount")
                            + " maxReadBytes=" + number(payload, "maxReadBytes")
                            + " securityScoped="
                            + Boolean.TRUE.equals(payload.get("securityScoped"))
                            + " permissionAllowed=" + permissionAllowed(result)
                            + " noPath=" + noPath;
                } catch (RuntimeException ignored) {
                    status = label("selection") + "outcome=ERROR fixtureError=true";
                }
                publish(ui, selectionStatus, status + asyncSuffix(background,
                        client, selectionChanges.incrementAndGet()));
            });
        });
        select.setId(ID_PREFIX + "select");
        add(select, selectionStatus);

        addOperation("foreign read", "foreign-read", 1, false, false);
        addOperation("foreign release", "foreign-release", 2, false, true);
        addOperation("owner read", "owner-read", 3, true, false);
        addOperation("owner release", "owner-release", 4, true, true);

        Span closeStatus = pending("secondary close", "close-secondary");
        AtomicInteger closeChanges = new AtomicInteger();
        Button close = new Button("Close file grant isolation window", event -> {
            event.getSource().setEnabled(false);
            Run run = mainRun;
            boolean closed = run != null && run.released()
                    && Desktop.current().windows().close(SECONDARY_WINDOW_ID)
                            .success();
            if (closed) {
                clearRun(run);
            }
            closeStatus.setText(label("secondary close") + "closed=" + closed
                    + " sharedStateCleared="
                    + (closed && ACTIVE_RUN.get() != run && run.cleared())
                    + " client=" + event.isFromClient()
                    + " change=" + closeChanges.incrementAndGet());
        });
        close.setId(ID_PREFIX + "close-secondary");
        add(close, closeStatus);
        addDetachListener(event -> {
            clearRun(mainRun);
            clearRun(ownerRun);
        });
    }

    private void addOperation(String name, String suffix, int step,
            boolean owner, boolean release) {
        Span status = pending(name, suffix);
        AtomicInteger changes = new AtomicInteger();
        Button button = new Button("Invoke " + name, event -> {
            event.getSource().setEnabled(false);
            boolean client = event.isFromClient();
            UI ui = event.getSource().getUI().orElseThrow();
            // Capture this clicking window, not the selecting window's bridge.
            NativeBridge bridge = Desktop.current().nativeBridge()
                    .captureCapabilityContext();
            Run run = owner ? ownerRun : mainRun;
            Thread.ofVirtual().name("file-grant-isolation-" + suffix).start(() -> {
                boolean background = VaadinSession.getCurrent() == null;
                String completed;
                try {
                    String selectedToken = run == null ? null : run.claim(step);
                    if (selectedToken == null || ACTIVE_RUN.get() != run) {
                        completed = label(name) + "outcome=ERROR missingGrant=true";
                    } else {
                        Map<String, Object> arguments = release
                                ? Map.of("grantId", selectedToken)
                                : Map.of("grantId", selectedToken,
                                        "offset", 0, "maxBytes", READ_BYTES);
                        DesktopCommand command = new DesktopCommand(release
                                ? "fileGrant.release" : "fileGrant.read", arguments);
                        boolean sameTokenUsed = selectedToken.equals(
                                command.payload().get("grantId"))
                                && run.matches(selectedToken);
                        DesktopCommandResult result = bridge.invoke(command);
                        if (!owner) {
                            completed = foreignStatus(name, result, sameTokenUsed,
                                    run.matches(selectedToken));
                        } else if (release) {
                            boolean released = result.success()
                                    && Boolean.TRUE.equals(result.payload().get("released"))
                                    && permissionAllowed(result);
                            if (released) {
                                run.release(selectedToken);
                            }
                            completed = label(name)
                                    + "outcome=" + (released ? "RELEASED" : "ERROR")
                                    + " success=" + result.success()
                                    + " sameTokenUsed=" + sameTokenUsed
                                    + " permissionAllowed=" + permissionAllowed(result)
                                    + " released=" + released;
                        } else {
                            completed = ownerReadStatus(result, sameTokenUsed);
                        }
                    }
                } catch (RuntimeException ignored) {
                    // Never render exception text: it may contain native payload data.
                    completed = label(name) + "outcome=ERROR fixtureError=true";
                }
                publish(ui, status, completed + asyncSuffix(background,
                        client, changes.incrementAndGet()));
            });
        });
        button.setId(ID_PREFIX + suffix);
        add(button, status);
    }

    private static String foreignStatus(String name, DesktopCommandResult result,
            boolean sameTokenUsed, boolean ownerTokenUnchanged) {
        Map<String, Object> payload = result.payload();
        boolean unavailable = !result.success()
                && Boolean.TRUE.equals(payload.get("grantUnavailable"))
                && "unavailable".equals(payload.get("grantState"))
                && "grant-unavailable".equals(payload.get("errorCode"));
        boolean noContent = !payload.containsKey("bodyBase64")
                && !payload.containsKey("bodyEncoding")
                && !payload.containsKey("path") && !payload.containsKey("url");
        return label(name) + "outcome=" + (unavailable ? "UNAVAILABLE" : "ERROR")
                + " unavailable=" + unavailable
                + " grantUnavailable=" + Boolean.TRUE.equals(payload.get("grantUnavailable"))
                + " permissionAllowed=" + permissionAllowed(result)
                + " noContent=" + noContent
                + " sameTokenUsed=" + sameTokenUsed
                + " ownerTokenUnchanged=" + ownerTokenUnchanged;
    }

    private static String ownerReadStatus(DesktopCommandResult result,
            boolean sameTokenUsed) {
        Map<String, Object> payload = result.payload();
        byte[] bytes = "base64".equals(payload.get("bodyEncoding"))
                && payload.get("bodyBase64") instanceof String encoded
                ? Base64.getDecoder().decode(encoded) : new byte[0];
        boolean data = result.success()
                && Boolean.FALSE.equals(payload.get("grantUnavailable"))
                && "active".equals(payload.get("grantState"))
                && number(payload, "bytesRead") == bytes.length;
        return label("owner read") + "outcome=" + (data ? "DATA" : "ERROR")
                + " success=" + result.success()
                + " sameTokenUsed=" + sameTokenUsed
                + " permissionAllowed=" + permissionAllowed(result)
                + " requestedOffset=0 requestedBytes=" + READ_BYTES
                + " bytesRead=" + bytes.length
                + " offset=" + number(payload, "offset")
                + " nextOffset=" + number(payload, "nextOffset")
                + " eof=" + Boolean.TRUE.equals(payload.get("endOfFile"))
                + " contentMatches=" + (data && Arrays.equals(bytes, EXPECTED_READ));
    }

    private static String label(String name) {
        return "File grant isolation " + name + ": ";
    }

    private static Span pending(String name, String suffix) {
        Span status = new Span(label(name) + "pending");
        status.setId(ID_PREFIX + suffix + "-status");
        return status;
    }

    private static String asyncSuffix(boolean background, boolean client, int change) {
        return " capabilityContextCaptured=true backgroundSessionAbsent=" + background
                + " client=" + client + " change=" + change;
    }

    private static void publish(UI ui, Span status, String text) {
        try {
            ui.access(() -> status.setText(text));
        } catch (UIDetachedException ignored) {
            // The fixture was retired while its public native operation settled.
        }
    }

    private static boolean permissionAllowed(DesktopCommandResult result) {
        return "allowed".equals(result.payload().get("permissionDecision"));
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
            return UUID.fromString(text).toString().equalsIgnoreCase(text) ? text : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static void clearRun(Run run) {
        if (run != null) {
            ACTIVE_RUN.compareAndSet(run, null);
            run.clear();
        }
    }

    /** Process-local fixture coordination; never stores UI, session or bridge. */
    private static final class Run {
        private String grantId;
        private int nextStep;
        private boolean released;
        private boolean cleared;

        synchronized boolean select(String selectedToken) {
            if (cleared || nextStep != 0 || grantId != null) {
                return false;
            }
            grantId = selectedToken;
            nextStep = 1;
            return true;
        }

        synchronized String claim(int step) {
            if (cleared || released || nextStep != step || grantId == null) {
                return null;
            }
            nextStep++;
            return grantId;
        }

        synchronized boolean matches(String token) {
            return !cleared && token.equals(grantId);
        }

        synchronized void release(String token) {
            if (matches(token)) {
                grantId = null;
                released = true;
            }
        }

        synchronized boolean released() {
            return released;
        }

        synchronized boolean cleared() {
            return cleared && grantId == null;
        }

        synchronized void clear() {
            grantId = null;
            cleared = true;
        }
    }
}
