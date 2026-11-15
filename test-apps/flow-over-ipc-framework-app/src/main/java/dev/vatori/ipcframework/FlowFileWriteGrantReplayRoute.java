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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Public raw commands with a private test-only old-token handoff, not grant persistence. */
@Route("flow-file-write-grant-replay")
public final class FlowFileWriteGrantReplayRoute extends VerticalLayout {
    static final String SECONDARY_WINDOW_ID = "file-write-grant-replay-secondary";
    static final String WINDOW_TITLE = "Flow file write grant recovery replay";
    private static final String ROUTE = "flow-file-write-grant-replay";
    private static final String PREFIX = "framework-flow-file-write-grant-replay-";
    private static final String FILENAME = "vatori-file-write-grant-fixture.bin";
    private static final byte[] TAIL = {0, -1, 1, -2, 42, 10};
    private static final byte[] POISON = "GUEST!".getBytes(StandardCharsets.US_ASCII);
    private static final AtomicReference<Run> ACTIVE_RUN = new AtomicReference<>();
    private volatile Run ownerRun;

    public FlowFileWriteGrantReplayRoute() {
        setId(PREFIX + "route");
        setPadding(true);
        setSpacing(true);
        add(new H1(WINDOW_TITLE));
        operation("open-secondary", this::openSecondary);
        operation("old-select-new", bridge -> select(bridge, false));
        operation("old-write-first", bridge -> write(bridge, 1, 0, firstBlock()));
        operation("old-invalid-range", bridge -> write(bridge, 2, -1, new byte[]{1}));
        operation("old-write-tail", bridge -> write(bridge, 3, 65_536, TAIL.clone()));
        operation("replay-old-write", bridge -> replay(bridge, false));
        operation("replay-old-release", bridge -> replay(bridge, true));
        operation("fresh-select-new", bridge -> select(bridge, true));
        operation("fresh-write-first", bridge -> write(bridge, 7, 0, firstBlock()));
        operation("fresh-invalid-range", bridge -> write(bridge, 8, -1, new byte[]{1}));
        operation("fresh-write-tail", bridge -> write(bridge, 9, 65_536, TAIL.clone()));
        operation("fresh-release", this::releaseFresh);
        operation("fresh-write-after-release", this::writeAfterRelease);
        operation("close-secondary", this::closeSecondary);
        add(FileGrantReplacementAction.button());
        // A recovered main route must not clear the owner's private handoff.
        // Successful final release removes the record; runner cleanup is the backstop.
    }

    private void operation(String suffix, Work work) {
        Span status = new Span(suffix + ": pending");
        status.setId(PREFIX + suffix + "-status");
        AtomicInteger changes = new AtomicInteger();
        AtomicInteger listeners = new AtomicInteger();
        Button button = new Button("Invoke " + suffix, event -> {
            int listenerCount = listeners.incrementAndGet();
            event.getSource().setEnabled(false);
            UI ui = event.getSource().getUI().orElseThrow();
            boolean client = event.isFromClient();
            NativeBridge bridge = Desktop.current().nativeBridge().captureCapabilityContext();
            Thread.ofVirtual().name("write-grant-replay-" + suffix).start(() -> {
                boolean background = VaadinSession.getCurrent() == null;
                String fields;
                try {
                    fields = work.invoke(bridge);
                } catch (IOException | RuntimeException ignored) {
                    // Neither native payloads nor exception paths belong in UI/logs.
                    fields = "outcome=ERROR fixtureError=true";
                }
                String text = suffix + ": " + fields
                        + " capabilityContextCaptured=true backgroundSessionAbsent=" + background
                        + " client=" + client + " change=" + changes.incrementAndGet()
                        + " listenerCount=" + listenerCount;
                try {
                    ui.access(() -> status.setText(text));
                } catch (UIDetachedException ignored) {
                    // Native owner retirement, not this detached UI, owns grant cleanup.
                }
            });
        });
        button.setId(PREFIX + suffix);
        add(button, status);
    }

    private String openSecondary(NativeBridge bridge) {
        Run run = new Run();
        if (!ACTIVE_RUN.compareAndSet(null, run)) { throw invalid(); }
        DesktopCommandResult result = bridge.invoke(new DesktopCommand("window.open",
                Map.of("id", SECONDARY_WINDOW_ID, "title", WINDOW_TITLE,
                        "width", 1000, "height", 900, "route", ROUTE)));
        boolean opened = result.success() && allowed(result)
                && SECONDARY_WINDOW_ID.equals(result.payload().get("windowId"))
                && ROUTE.equals(result.payload().get("route"));
        if (!opened) { ACTIVE_RUN.compareAndSet(run, null); run.clear(); }
        return "opened=" + opened + " permissionAllowed=" + allowed(result);
    }

    private String select(NativeBridge bridge, boolean fresh) throws IOException {
        Run run = fresh ? requireOwner() : ACTIVE_RUN.get();
        int step = fresh ? 6 : 0;
        if (run == null || !run.begin(step)) { throw invalid(); }
        ownerRun = run;
        DesktopCommandResult result = bridge.invoke(new DesktopCommand("fileDialog.saveNewWriteGrant",
                Map.of("title", "Flow file write grant new file", "suggestedName", FILENAME)));
        Map<String, Object> payload = result.payload();
        String token = canonicalUuid(payload.get("grantId"));
        if (!result.success() || !allowed(result) || token == null
                || !Boolean.FALSE.equals(payload.get("canceled"))
                || !Boolean.TRUE.equals(payload.get("grantPresent"))
                || !Boolean.TRUE.equals(payload.get("createNewOnly"))
                || !Boolean.TRUE.equals(payload.get("usedSavePanel"))
                || !Boolean.TRUE.equals(payload.get("securityScoped"))
                || number(payload, "maxWriteBytes") != 65_536 || !FILENAME.equals(payload.get("filename"))
                || !noPath(payload) || payload.containsKey("error") || payload.containsKey("errorCode")) {
            throw invalid();
        }
        String fields = "outcome=SELECTED grantPresent=true maxWriteBytes=65536 securityScoped=true"
                + " filenameMatches=true createNewOnly=true noPath=true tokenPrivate=true permissionAllowed=true";
        if (!fresh) {
            Path path = FileWriteGrantReplayHandoff.configuredPath();
            var old = FileWriteGrantReplayHandoff.create(path, token, ProcessHandle.current().pid());
            if (!old.token().equals(token) || !FileWriteGrantReplayHandoff.retained(path, old)
                    || !run.selectedOld(path, old)) { throw invalid(); }
            return fields + " handoffWritten=true handoffVerified=true handoffProtected=true"
                    + " producerProcessId=" + old.producerPid();
        }
        var old = run.old();
        boolean retained = FileWriteGrantReplayHandoff.retained(run.path(), old);
        if (!retained || old.token().equals(token) || old.producerPid() == ProcessHandle.current().pid()
                || !run.selectedFresh(token)) { throw invalid(); }
        return fields + " freshTokenDistinct=true recoveredProcess=true handoffRetained=true"
                + processFields(old);
    }

    private String write(NativeBridge bridge, int step, long offset, byte[] bytes) throws IOException {
        Run run = requireOwner();
        if (!run.begin(step)) { throw invalid(); }
        String token = run.token(step >= 7);
        DesktopCommand command = writeCommand(token, offset, bytes);
        boolean same = token.equals(command.payload().get("grantId")) && run.matches(token, step >= 7);
        DesktopCommandResult result = bridge.invoke(command);
        Map<String, Object> payload = result.payload();
        if (offset < 0) {
            boolean valid = !result.success() && same && allowed(result) && noContent(payload) && noProgress(payload)
                    && Boolean.FALSE.equals(payload.get("grantUnavailable")) && "active".equals(payload.get("grantState"))
                    && "invalid-write-range".equals(payload.get("errorCode")) && number(payload, "maxWriteBytes") == 65_536;
            if (!valid || !run.complete(step)) { throw invalid(); }
            return "outcome=INVALID_RANGE success=false sameTokenUsed=true requestedOffset=-1 requestedBytes=1"
                    + " noContent=true noWriteProgress=true maxWriteBytes=65536 grantUnavailable=false noPath=true permissionAllowed=true";
        }
        boolean written = result.success() && same && allowed(result) && noPath(payload)
                && Boolean.FALSE.equals(payload.get("grantUnavailable")) && "active".equals(payload.get("grantState"))
                && number(payload, "offset") == offset && number(payload, "bytesWritten") == bytes.length
                && number(payload, "nextOffset") == offset + bytes.length && number(payload, "maxWriteBytes") == 65_536
                && !payload.containsKey("error") && !payload.containsKey("errorCode");
        if (!written || !run.written(step, offset, bytes.length)) { throw invalid(); }
        String fields = "outcome=WRITTEN success=true sameTokenUsed=true permissionAllowed=true"
                + " requestedOffset=" + offset + " requestedBytes=" + bytes.length
                + " bytesWritten=" + bytes.length + " offset=" + offset + " nextOffset=" + (offset + bytes.length)
                + " maxWriteBytes=65536 grantUnavailable=false noPath=true";
        if (step == 3) {
            if (!FileWriteGrantReplayHandoff.retained(run.path(), run.old())) { throw invalid(); }
            fields += " handoffRetained=true";
        }
        return fields;
    }

    private String replay(NativeBridge bridge, boolean release) throws IOException {
        Run run;
        if (!release) {
            Path path = FileWriteGrantReplayHandoff.configuredPath();
            var old = FileWriteGrantReplayHandoff.readRecovered(path, ProcessHandle.current().pid());
            run = Run.recovered(path, old);
            if (!ACTIVE_RUN.compareAndSet(null, run)) { throw invalid(); }
            ownerRun = run;
        } else { run = requireOwner(); }
        int step = release ? 5 : 4;
        if (!run.begin(step)) { throw invalid(); }
        var old = run.old();
        String token = old.token();
        DesktopCommand command = release ? releaseCommand(token) : writeCommand(token, 65_542, POISON.clone());
        boolean same = token.equals(command.payload().get("grantId")) && run.matches(token, false);
        DesktopCommandResult result = bridge.invoke(command);
        boolean retained = FileWriteGrantReplayHandoff.retained(run.path(), old);
        if (!same || !retained || old.producerPid() == ProcessHandle.current().pid()
                || !unavailable(result) || !run.complete(step)) { throw invalid(); }
        return unavailableFields() + " sameTokenUsed=true oldTokenUnchanged=true recoveredProcess=true handoffRetained=true"
                + processFields(old) + (release ? "" : " requestedOffset=65542 requestedBytes=6");
    }

    private String releaseFresh(NativeBridge bridge) throws IOException {
        Run run = requireOwner();
        if (!run.begin(10)) { throw invalid(); }
        String token = run.token(true);
        DesktopCommand command = releaseCommand(token);
        boolean same = token.equals(command.payload().get("grantId")) && run.matches(token, true);
        DesktopCommandResult result = bridge.invoke(command);
        Map<String, Object> payload = result.payload();
        boolean released = result.success() && same && allowed(result) && noPath(payload)
                && Boolean.TRUE.equals(payload.get("released")) && Boolean.TRUE.equals(payload.get("grantUnavailable"))
                && "unavailable".equals(payload.get("grantState")) && Boolean.TRUE.equals(payload.get("securityScopeStopped"))
                && number(payload, "nextOffset") == 65_542
                && !payload.containsKey("error") && !payload.containsKey("errorCode");
        if (!released || !FileWriteGrantReplayHandoff.remove(run.path(), run.old()) || !run.released()) { throw invalid(); }
        return "outcome=RELEASED success=true sameTokenUsed=true permissionAllowed=true released=true grantUnavailable=true"
                + " securityScopeStopped=true nextOffset=65542 noPath=true handoffRemoved=true";
    }

    private String writeAfterRelease(NativeBridge bridge) throws IOException {
        Run run = requireOwner();
        if (!run.begin(11)) { throw invalid(); }
        String token = run.token(true);
        DesktopCommand command = writeCommand(token, 65_542, new byte[]{1});
        boolean same = token.equals(command.payload().get("grantId")) && run.matches(token, true);
        DesktopCommandResult result = bridge.invoke(command);
        if (!same || !unavailable(result) || !run.complete(11)) { throw invalid(); }
        return unavailableFields() + " sameTokenUsed=true requestedOffset=65542 requestedBytes=1";
    }

    private String closeSecondary(NativeBridge bridge) {
        Run run = ACTIVE_RUN.get();
        if (run == null || !run.finished()) { throw invalid(); }
        DesktopCommandResult result = bridge.invoke(new DesktopCommand("window.close", Map.of("id", SECONDARY_WINDOW_ID)));
        boolean closed = result.success() && allowed(result)
                && SECONDARY_WINDOW_ID.equals(result.payload().get("windowId"));
        if (closed) { ACTIVE_RUN.compareAndSet(run, null); run.clear(); }
        return "closed=" + closed + " permissionAllowed=" + allowed(result)
                + " sharedStateCleared=" + (closed && ACTIVE_RUN.get() != run && run.cleared());
    }

    private Run requireOwner() {
        Run run = ownerRun;
        if (run == null || ACTIVE_RUN.get() != run) { throw invalid(); }
        return run;
    }

    private static String processFields(FileWriteGrantReplayHandoff.Snapshot old) {
        return " producerProcessId=" + old.producerPid() + " currentProcessId=" + ProcessHandle.current().pid();
    }

    private static DesktopCommand writeCommand(String token, long offset, byte[] bytes) {
        return new DesktopCommand("fileGrant.write", Map.of("grantId", token, "offset", offset,
                "bodyBase64", Base64.getEncoder().encodeToString(bytes)));
    }

    private static DesktopCommand releaseCommand(String token) {
        return new DesktopCommand("fileGrant.releaseWrite", Map.of("grantId", token));
    }

    private static boolean unavailable(DesktopCommandResult result) {
        Map<String, Object> payload = result.payload();
        return !result.success() && allowed(result) && Boolean.TRUE.equals(payload.get("grantUnavailable"))
                && "unavailable".equals(payload.get("grantState")) && "grant-unavailable".equals(payload.get("errorCode"))
                && number(payload, "maxWriteBytes") == 65_536 && noContent(payload) && noProgress(payload)
                && !payload.containsKey("released");
    }

    private static String unavailableFields() {
        return "outcome=UNAVAILABLE unavailable=true grantUnavailable=true permissionAllowed=true noContent=true"
                + " noWriteProgress=true noReleaseEffect=true maxWriteBytes=65536";
    }

    private static boolean allowed(DesktopCommandResult result) {
        return "allowed".equals(result.payload().get("permissionDecision"));
    }

    private static boolean noPath(Map<String, Object> payload) {
        return Boolean.FALSE.equals(payload.get("pathExposed")) && !payload.containsKey("path")
                && !payload.containsKey("paths") && !payload.containsKey("url");
    }

    private static boolean noContent(Map<String, Object> payload) {
        return noPath(payload) && !payload.containsKey("bodyBase64") && !payload.containsKey("bodyEncoding")
                && !payload.containsKey("grantId") && !payload.containsKey("filename");
    }

    private static boolean noProgress(Map<String, Object> payload) {
        return !payload.containsKey("offset") && !payload.containsKey("bytesWritten") && !payload.containsKey("nextOffset");
    }

    private static long number(Map<String, Object> payload, String field) {
        Object value = payload.get(field);
        return value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long
                ? ((Number) value).longValue() : -1;
    }

    private static String canonicalUuid(Object value) {
        if (!(value instanceof String token)) { return null; }
        try { return UUID.fromString(token).toString().equals(token) ? token : null; }
        catch (IllegalArgumentException ignored) { return null; }
    }

    private static byte[] firstBlock() {
        byte[] bytes = new byte[65_536];
        for (int i = 0; i < bytes.length; i++) { bytes[i] = (byte) (i % 251); }
        return bytes;
    }

    private static IllegalStateException invalid() {
        return new IllegalStateException("Write grant replay fixture is unavailable");
    }

    @FunctionalInterface
    private interface Work { String invoke(NativeBridge bridge) throws IOException; }

    /** Process-local coordination only; the old JVM never lends its bridge to a new one. */
    private static final class Run {
        private Path path;
        private FileWriteGrantReplayHandoff.Snapshot old;
        private String fresh;
        private int step;
        private long oldBytes;
        private long freshBytes;
        private boolean busy;
        private boolean handoffRemoved;
        private boolean cleared;

        static Run recovered(Path path, FileWriteGrantReplayHandoff.Snapshot old) {
            Run run = new Run(); run.path = path; run.old = old; run.oldBytes = 65_542; run.step = 4; return run;
        }
        synchronized boolean begin(int expected) {
            if (cleared || busy || step != expected) { return false; }
            busy = true; return true;
        }
        synchronized boolean complete(int expected) {
            if (cleared || !busy || step != expected) { return false; }
            step++; busy = false; return true;
        }
        synchronized boolean selectedOld(Path path, FileWriteGrantReplayHandoff.Snapshot old) {
            if (!complete(0)) { return false; }
            this.path = path; this.old = old; return true;
        }
        synchronized boolean selectedFresh(String token) {
            if (!complete(6)) { return false; }
            fresh = token; return true;
        }
        synchronized boolean matches(String token, boolean freshToken) {
            return !cleared && token != null && token.equals(freshToken ? fresh : old == null ? null : old.token());
        }
        synchronized String token(boolean freshToken) {
            String token = freshToken ? fresh : old == null ? null : old.token();
            if (cleared || token == null) { throw invalid(); }
            return token;
        }
        synchronized boolean written(int expected, long offset, int count) {
            boolean freshToken = expected >= 7;
            long current = freshToken ? freshBytes : oldBytes;
            if (current != offset || !(offset == 0 && count == 65_536 || offset == 65_536 && count == 6)
                    || !complete(expected)) { return false; }
            if (freshToken) { freshBytes = offset + count; } else { oldBytes = offset + count; }
            return true;
        }
        synchronized boolean released() {
            if (freshBytes != 65_542 || !complete(10)) { return false; }
            old = null; path = null; handoffRemoved = true; return true;
        }
        synchronized Path path() { if (cleared || path == null) { throw invalid(); } return path; }
        synchronized FileWriteGrantReplayHandoff.Snapshot old() { if (cleared || old == null) { throw invalid(); } return old; }
        synchronized boolean finished() { return !cleared && !busy && step == 12 && handoffRemoved && freshBytes == 65_542; }
        synchronized void clear() { path = null; old = null; fresh = null; busy = false; cleared = true; }
        synchronized boolean cleared() { return cleared && path == null && old == null && fresh == null; }
    }
}
