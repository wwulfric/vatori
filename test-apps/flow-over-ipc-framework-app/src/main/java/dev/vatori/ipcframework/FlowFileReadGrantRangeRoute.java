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
import dev.vatori.desktop.DesktopFileReadGrant;
import dev.vatori.desktop.DesktopFileReadGrantResult;
import dev.vatori.desktop.DesktopFileReadResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Public typed grant operations, with no raw-token or host-internal access. */
@Route("flow-file-read-grant-range")
public final class FlowFileReadGrantRangeRoute extends VerticalLayout {
    static final String PANEL_TITLE = "Flow file read grant bounded ranges";
    static final String FIXTURE_FILENAME = "vatori-file-read-grant-range-fixture.bin";
    private static final String PREFIX = "framework-flow-file-read-grant-range-";
    private static final int FILE_BYTES = 65_573;
    private static final int MAX_READ_BYTES = 65_536;
    private static final List<ReadCase> READS = List.of(
            new ReadCase("negative", -1, 16),
            new ReadCase("zero", 0, 0),
            new ReadCase("oversized", 0, 65_537),
            new ReadCase("maximum", 0, MAX_READ_BYTES),
            new ReadCase("tail", 65_536, MAX_READ_BYTES),
            new ReadCase("eof", FILE_BYTES, 1),
            new ReadCase("beyond", 65_590, 16),
            new ReadCase("seek", 257, 113));

    private final AtomicInteger sequence = new AtomicInteger();
    private final AtomicReference<DesktopFileReadGrant> selectedGrant = new AtomicReference<>();
    private volatile boolean capabilityContextCaptured;

    public FlowFileReadGrantRangeRoute() {
        setId(PREFIX + "route");
        setPadding(true);
        setSpacing(true);
        add(new H1("Flow typed file read grant bounded ranges"));
        operation("select", 0, this::select);
        for (int index = 0; index < READS.size(); index++) {
            ReadCase read = READS.get(index);
            operation(read.stage(), index + 1, ignored -> read(read));
        }
        operation("release", READS.size() + 1, ignored -> release());
    }

    private void operation(String stage, int expectedSequence, StageWork work) {
        Span status = new Span("File grant range " + stage + ": pending");
        status.setId(PREFIX + stage + "-status");
        AtomicInteger changes = new AtomicInteger();
        Button button = new Button("Invoke grant range " + stage, event -> {
            event.getSource().setEnabled(false);
            UI ui = event.getSource().getUI().orElseThrow();
            boolean client = event.isFromClient();
            // Capture once while the selecting listener owns the Flow context.
            // Later operations use only the same returned owner-bound facade.
            DesktopCapabilityContext context = expectedSequence == 0
                    ? Desktop.current().captureCapabilityContext() : null;
            if (context != null) {
                capabilityContextCaptured = true;
            }
            Thread.ofVirtual().name("file-grant-range-" + stage).start(() -> {
                boolean background = VaadinSession.getCurrent() == null;
                String result;
                try {
                    if (!sequence.compareAndSet(expectedSequence, expectedSequence + 1)) {
                        throw new IllegalStateException("Out-of-order fixture operation");
                    }
                    result = work.invoke(context);
                } catch (RuntimeException ignored) {
                    // Native payloads and exception messages never become DOM text.
                    result = "outcome=ERROR fixtureError=true";
                }
                String text = "File grant range " + stage + ": " + result
                        + " capabilityContextCaptured=" + capabilityContextCaptured
                        + " backgroundSessionAbsent=" + background
                        + " client=" + client + " change=" + changes.incrementAndGet();
                try {
                    ui.access(() -> status.setText(text));
                } catch (UIDetachedException ignored) {
                    // Native owner teardown remains responsible for grant cleanup.
                }
            });
        });
        button.setId(PREFIX + stage);
        add(button, status);
    }

    private String select(DesktopCapabilityContext context) {
        DesktopFileReadGrantResult result = context.fileDialog().openReadGrant(PANEL_TITLE);
        DesktopFileReadGrant grant = result.grant().orElse(null);
        if (grant != null && !selectedGrant.compareAndSet(null, grant)) {
            throw new IllegalStateException("Duplicate fixture grant");
        }
        Map<String, Object> payload = result.commandResult().payload();
        return "outcome=" + result.outcome().name() + metadata(grant)
                + " permissionAllowed=" + permissionAllowed(result.commandResult())
                + " publicTokenHidden=" + !payload.containsKey("grantId")
                + " noPath=" + (Boolean.FALSE.equals(payload.get("pathExposed"))
                        && !payload.containsKey("path") && !payload.containsKey("paths")
                        && !payload.containsKey("url"));
    }

    private String read(ReadCase request) {
        DesktopFileReadGrant grant = requireGrant();
        DesktopFileReadResult result = grant.read(request.offset(), request.maxBytes());
        byte[] bytes = result.bytes();
        Map<String, Object> payload = result.commandResult().payload();
        boolean readable = result.outcome() == DesktopFileReadResult.Outcome.DATA
                || result.outcome() == DesktopFileReadResult.Outcome.END_OF_FILE;
        boolean noContent = bytes.length == 0 && !payload.containsKey("bodyBase64")
                && !payload.containsKey("bodyEncoding") && !payload.containsKey("bodyBytes")
                && !payload.containsKey("path") && !payload.containsKey("url");
        return "outcome=" + result.outcome().name()
                + " requestedOffset=" + request.offset()
                + " requestedMaxBytes=" + request.maxBytes()
                + " bytesRead=" + bytes.length
                + " offset=" + result.offset() + " nextOffset=" + result.nextOffset()
                + " eof=" + result.endOfFile()
                + " contentMatches=" + (readable && matchesContent(request, bytes))
                + " noContent=" + noContent
                + " grantActive=" + ("active".equals(payload.get("grantState"))
                        && Boolean.FALSE.equals(payload.get("grantUnavailable")))
                + " sameGrantFacade=" + (grant == selectedGrant.get())
                + " permissionAllowed=" + permissionAllowed(result.commandResult())
                + " publicTokenHidden=" + !payload.containsKey("grantId")
                + metadata(grant);
    }

    private String release() {
        DesktopFileReadGrant grant = requireGrant();
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

    private DesktopFileReadGrant requireGrant() {
        DesktopFileReadGrant grant = selectedGrant.get();
        if (grant == null) {
            throw new IllegalStateException("Missing fixture grant");
        }
        return grant;
    }

    private static boolean matchesContent(ReadCase request, byte[] bytes) {
        if (request.offset() < 0 || request.maxBytes() <= 0 || request.maxBytes() > MAX_READ_BYTES) {
            return false;
        }
        long remaining = Math.max(0, FILE_BYTES - request.offset());
        int expectedCount = (int) Math.min(request.maxBytes(), remaining);
        if (bytes.length != expectedCount) {
            return false;
        }
        for (int index = 0; index < bytes.length; index++) {
            long position = request.offset() + index;
            int expectedByte = (int) ((position * 31 + position / 251 + 17) % 256);
            if (Byte.toUnsignedInt(bytes[index]) != expectedByte) {
                return false;
            }
        }
        return true;
    }

    private static boolean permissionAllowed(DesktopCommandResult result) {
        return "allowed".equals(result.payload().get("permissionDecision"));
    }

    private static String metadata(DesktopFileReadGrant grant) {
        return " grantPresent=" + (grant != null)
                + " byteCount=" + (grant == null ? -1 : grant.byteCount())
                + " maxReadBytes=" + (grant == null ? -1 : grant.maxReadBytes())
                + " securityScoped=" + (grant != null && grant.securityScoped())
                + " filenameMatches=" + (grant != null && FIXTURE_FILENAME.equals(grant.filename()));
    }

    private record ReadCase(String stage, long offset, int maxBytes) { }

    @FunctionalInterface
    private interface StageWork { String invoke(DesktopCapabilityContext context); }
}
