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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** An external-app raw-command probe; the private handoff is test-only. */
@Route("flow-file-read-grant-replay")
public final class FlowFileReadGrantReplayRoute extends VerticalLayout {
    static final String SECONDARY_WINDOW_ID = "file-read-grant-replay-secondary";
    static final String OLD_TITLE = "Flow file read grant before recovery";
    static final String FRESH_TITLE = "Flow file read grant after recovery";
    private static final String PREFIX = "framework-flow-file-read-grant-replay-";
    private static final AtomicReference<Run> ACTIVE_RUN = new AtomicReference<>();
    private static final int READ_BYTES = 19;
    private static final byte[] EXPECTED = Arrays.copyOf(
            FlowFileReadGrantRoute.FIXTURE_CONTENT.getBytes(StandardCharsets.UTF_8), READ_BYTES);
    private volatile Run ownerRun;

    public FlowFileReadGrantReplayRoute() {
        setId(PREFIX + "route");
        setPadding(true);
        setSpacing(true);
        add(new H1("Flow file read grant recovery replay"));
        Span openStatus = pending("secondary open", "open-secondary");
        AtomicInteger openChanges = new AtomicInteger();
        Button open = new Button("Open file grant replay window", event -> {
            event.getSource().setEnabled(false);
            Run run = new Run();
            boolean opened = ACTIVE_RUN.compareAndSet(null, run)
                    && Desktop.current().windows().open(
                            DesktopWindow.builder(SECONDARY_WINDOW_ID)
                                    .title("Flow file grant recovery replay")
                                    .size(760, 680).route("flow-file-read-grant-replay")
                                    .build()).success();
            if (!opened) {
                ACTIVE_RUN.compareAndSet(run, null);
            }
            openStatus.setText(label("secondary open") + "opened=" + opened
                    + " client=" + event.isFromClient()
                    + " change=" + openChanges.incrementAndGet());
        });
        open.setId(PREFIX + "open-secondary");
        add(open, openStatus);
        operation("old selection", "select-old", this::selectOld);
        operation("old read", "old-read", bridge -> replayOld(bridge, false));
        operation("old release", "old-release", bridge -> replayOld(bridge, true));
        operation("fresh selection", "select-fresh", this::selectFresh);
        operation("fresh read", "fresh-read", bridge -> useFresh(bridge, false));
        operation("fresh release", "fresh-release", bridge -> useFresh(bridge, true));
        Span closeStatus = pending("secondary close", "close-secondary");
        AtomicInteger closeChanges = new AtomicInteger();
        Button close = new Button("Close file grant replay window", event -> {
            event.getSource().setEnabled(false);
            Run run = ACTIVE_RUN.get();
            boolean closed = run != null && run.finished()
                    && Desktop.current().windows().close(SECONDARY_WINDOW_ID).success();
            if (closed) {
                ACTIVE_RUN.compareAndSet(run, null);
                run.clear();
            }
            closeStatus.setText(label("secondary close") + "closed=" + closed
                    + " sharedStateCleared="
                    + (closed && ACTIVE_RUN.get() != run && run.cleared())
                    + " client=" + event.isFromClient()
                    + " change=" + closeChanges.incrementAndGet());
        });
        close.setId(PREFIX + "close-secondary");
        add(close, closeStatus);
        // Main navigation after recovery must not erase the owner's handoff.
        // Only successful final release consumes it; runner EXIT is the backstop.
    }

    private void operation(String name, String suffix, Operation action) {
        Span status = pending(name, suffix);
        AtomicInteger changes = new AtomicInteger();
        Button button = new Button("Invoke " + name, event -> {
            event.getSource().setEnabled(false);
            UI ui = event.getSource().getUI().orElseThrow();
            boolean client = event.isFromClient();
            NativeBridge bridge = Desktop.current().nativeBridge().captureCapabilityContext();
            Thread.ofVirtual().name("file-grant-replay-" + suffix).start(() -> {
                boolean background = VaadinSession.getCurrent() == null;
                String result;
                try {
                    result = action.invoke(bridge);
                } catch (IOException | RuntimeException ignored) {
                    result = "outcome=ERROR fixtureError=true";
                }
                String text = label(name) + result
                        + " capabilityContextCaptured=true backgroundSessionAbsent=" + background
                        + " client=" + client + " change=" + changes.incrementAndGet();
                try {
                    ui.access(() -> status.setText(text));
                } catch (UIDetachedException ignored) {
                    // No exception messages, handoff paths or native payloads in logs.
                }
            });
        });
        button.setId(PREFIX + suffix);
        add(button, status);
    }

    private String selectOld(NativeBridge bridge) throws IOException {
        Run run = ACTIVE_RUN.get();
        if (run == null || !run.advance(0, 1)) {
            throw invalid();
        }
        ownerRun = run;
        DesktopCommandResult result = select(bridge, OLD_TITLE);
        String token = selectedToken(result);
        Path path = FileReadGrantReplayHandoff.configuredPath();
        long pid = ProcessHandle.current().pid();
        FileReadGrantReplayHandoff.Snapshot snapshot =
                FileReadGrantReplayHandoff.create(path, token, pid);
        run.setOld(snapshot, path);
        return selectionStatus(result)
                + " handoffWritten=true handoffVerified=true handoffProtected=true"
                + " producerProcessId=" + pid;
    }

    private String replayOld(NativeBridge bridge, boolean release) throws IOException {
        Run run;
        if (!release) {
            Path path = FileReadGrantReplayHandoff.configuredPath();
            FileReadGrantReplayHandoff.Snapshot restored =
                    FileReadGrantReplayHandoff.readRecovered(path, ProcessHandle.current().pid());
            run = new Run();
            run.setOld(restored, path);
            if (!run.advance(0, 2) || !ACTIVE_RUN.compareAndSet(null, run)) {
                throw invalid();
            }
            ownerRun = run;
        } else {
            run = requireOwner();
            if (!run.advance(2, 3)) {
                throw invalid();
            }
        }
        FileReadGrantReplayHandoff.Snapshot old = run.old();
        String token = old.token();
        DesktopCommand command = tokenCommand(token, release);
        boolean sameToken = token.equals(command.payload().get("grantId")) && run.matchesOld(token);
        DesktopCommandResult result = bridge.invoke(command);
        Map<String, Object> payload = result.payload();
        boolean unavailable = !result.success()
                && Boolean.TRUE.equals(payload.get("grantUnavailable"))
                && "unavailable".equals(payload.get("grantState"))
                && "grant-unavailable".equals(payload.get("errorCode"));
        boolean retained = FileReadGrantReplayHandoff.retained(run.path(), old);
        return "outcome=" + (unavailable ? "UNAVAILABLE" : "ERROR")
                + " unavailable=" + unavailable
                + " grantUnavailable=" + Boolean.TRUE.equals(payload.get("grantUnavailable"))
                + " permissionAllowed=" + allowed(result)
                + " noContent=" + noContent(payload)
                + " sameTokenUsed=" + sameToken
                + " oldTokenUnchanged=" + (retained && run.matchesOld(token))
                + " recoveredProcess=" + (old.producerPid() != ProcessHandle.current().pid())
                + " producerProcessId=" + old.producerPid()
                + " currentProcessId=" + ProcessHandle.current().pid()
                + (release ? " handoffRetained=" + retained : "");
    }

    private String selectFresh(NativeBridge bridge) throws IOException {
        Run run = requireOwner();
        if (!run.advance(3, 4)) {
            throw invalid();
        }
        DesktopCommandResult result = select(bridge, FRESH_TITLE);
        String token = selectedToken(result);
        boolean distinct = !run.old().token().equals(token);
        if (!distinct) {
            throw invalid();
        }
        run.setFresh(token);
        return selectionStatus(result) + " freshTokenDistinct=true recoveredProcess="
                + (run.old().producerPid() != ProcessHandle.current().pid());
    }

    private String useFresh(NativeBridge bridge, boolean release) throws IOException {
        Run run = requireOwner();
        if (!run.advance(release ? 5 : 4, release ? 6 : 5)) {
            throw invalid();
        }
        String token = run.fresh();
        DesktopCommand command = tokenCommand(token, release);
        boolean sameToken = token.equals(command.payload().get("grantId")) && run.matchesFresh(token);
        DesktopCommandResult result = bridge.invoke(command);
        Map<String, Object> payload = result.payload();
        if (release) {
            boolean released = result.success() && Boolean.TRUE.equals(payload.get("released"))
                    && allowed(result);
            boolean removed = released && FileReadGrantReplayHandoff.remove(run.path(), run.old());
            if (removed) {
                run.finish();
            }
            return "outcome=" + (released ? "RELEASED" : "ERROR")
                    + " success=" + result.success() + " sameTokenUsed=" + sameToken
                    + " permissionAllowed=" + allowed(result) + " released=" + released
                    + " handoffRemoved=" + removed;
        }
        byte[] bytes = "base64".equals(payload.get("bodyEncoding"))
                && payload.get("bodyBase64") instanceof String encoded
                ? Base64.getDecoder().decode(encoded) : new byte[0];
        boolean data = result.success() && Boolean.FALSE.equals(payload.get("grantUnavailable"))
                && "active".equals(payload.get("grantState"))
                && number(payload, "bytesRead") == bytes.length;
        return "outcome=" + (data ? "DATA" : "ERROR") + " success=" + result.success()
                + " sameTokenUsed=" + sameToken + " permissionAllowed=" + allowed(result)
                + " requestedOffset=0 requestedBytes=" + READ_BYTES + " bytesRead=" + bytes.length
                + " offset=" + number(payload, "offset") + " nextOffset=" + number(payload, "nextOffset")
                + " eof=" + Boolean.TRUE.equals(payload.get("endOfFile"))
                + " contentMatches=" + (data && Arrays.equals(bytes, EXPECTED));
    }

    private Run requireOwner() throws IOException {
        Run run = ownerRun;
        if (run == null || ACTIVE_RUN.get() != run) {
            throw invalid();
        }
        return run;
    }

    private static DesktopCommandResult select(NativeBridge bridge, String title) {
        return bridge.invoke(new DesktopCommand("fileDialog.openReadGrant", Map.of("title", title)));
    }

    private static String selectedToken(DesktopCommandResult result) throws IOException {
        Map<String, Object> payload = result.payload();
        Object value = payload.get("grantId");
        if (!result.success() || !allowed(result) || !(value instanceof String token)
                || token.length() != 36 || !UUID.fromString(token).toString().equals(token)
                || !Boolean.TRUE.equals(payload.get("grantPresent"))
                || !Boolean.FALSE.equals(payload.get("canceled"))
                || number(payload, "byteCount") != 48 || number(payload, "maxReadBytes") != 65_536
                || !(payload.get("securityScoped") instanceof Boolean)
                || !Boolean.FALSE.equals(payload.get("pathExposed"))
                || payload.containsKey("path") || payload.containsKey("url")) {
            throw invalid();
        }
        return token;
    }

    private static String selectionStatus(DesktopCommandResult result) {
        return "outcome=SELECTED grantPresent=true byteCount=48 maxReadBytes=65536"
                + " securityScoped=" + Boolean.TRUE.equals(result.payload().get("securityScoped"))
                + " permissionAllowed=" + allowed(result) + " noPath=true";
    }

    private static DesktopCommand tokenCommand(String token, boolean release) {
        return new DesktopCommand(release ? "fileGrant.release" : "fileGrant.read",
                release ? Map.of("grantId", token)
                        : Map.of("grantId", token, "offset", 0, "maxBytes", READ_BYTES));
    }

    private static boolean noContent(Map<String, Object> payload) {
        return !payload.containsKey("bodyBase64") && !payload.containsKey("bodyEncoding")
                && !payload.containsKey("path") && !payload.containsKey("url");
    }

    private static boolean allowed(DesktopCommandResult result) {
        return "allowed".equals(result.payload().get("permissionDecision"));
    }

    private static long number(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value instanceof Byte || value instanceof Short || value instanceof Integer
                || value instanceof Long ? ((Number) value).longValue() : -1;
    }

    private static String label(String name) { return "File grant replay " + name + ": "; }
    private static Span pending(String name, String suffix) {
        Span span = new Span(label(name) + "pending");
        span.setId(PREFIX + suffix + "-status");
        return span;
    }
    private static IOException invalid() { return new IOException("File grant replay fixture is unavailable"); }

    @FunctionalInterface
    private interface Operation { String invoke(NativeBridge bridge) throws IOException; }

    private static final class Run {
        private int step;
        private FileReadGrantReplayHandoff.Snapshot old;
        private Path path;
        private String fresh;
        private boolean finished;
        private boolean cleared;
        synchronized boolean advance(int before, int after) {
            if (cleared || step != before) { return false; }
            step = after;
            return true;
        }
        synchronized void setOld(FileReadGrantReplayHandoff.Snapshot snapshot, Path value) {
            old = snapshot;
            path = value;
        }
        synchronized FileReadGrantReplayHandoff.Snapshot old() { return old; }
        synchronized Path path() { return path; }
        synchronized boolean matchesOld(String token) { return old != null && token.equals(old.token()); }
        synchronized void setFresh(String token) { fresh = token; }
        synchronized String fresh() { return fresh; }
        synchronized boolean matchesFresh(String token) { return token.equals(fresh); }
        synchronized void finish() { old = null; fresh = null; path = null; finished = true; }
        synchronized boolean finished() { return finished; }
        synchronized void clear() { old = null; fresh = null; path = null; cleared = true; }
        synchronized boolean cleared() { return cleared && old == null && fresh == null; }
    }
}
