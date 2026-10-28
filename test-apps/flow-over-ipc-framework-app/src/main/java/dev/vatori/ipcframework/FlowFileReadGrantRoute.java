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
import dev.vatori.desktop.DesktopFileGrantReleaseResult;
import dev.vatori.desktop.DesktopFileReadGrant;
import dev.vatori.desktop.DesktopFileReadGrantResult;
import dev.vatori.desktop.DesktopFileReadResult;
import dev.vatori.desktop.DesktopWindow;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

@Route("flow-file-read-grant")
public final class FlowFileReadGrantRoute extends VerticalLayout {
    static final String SECONDARY_WINDOW_ID = "file-read-grant-secondary";
    static final String FIRST_TITLE =
            "Flow file read grant explicit release";
    static final String SECOND_TITLE =
            "Flow file read grant automatic revoke";
    static final String FIXTURE_FILENAME =
            "vatori-file-read-grant-fixture.txt";
    static final String FIXTURE_CONTENT =
            "vatori-file-read-grant-fixture::0123456789::tail";

    private static final String ID_PREFIX =
            "framework-flow-file-read-grant-";
    private static final int FIRST_READ_BYTES = 19;
    private static final byte[] FIRST_READ_EXPECTED = Arrays.copyOf(
            FIXTURE_CONTENT.getBytes(StandardCharsets.UTF_8),
            FIRST_READ_BYTES);

    private final AtomicInteger selectFirstChanges = new AtomicInteger();
    private final AtomicInteger readFirstChanges = new AtomicInteger();
    private final AtomicInteger releaseFirstChanges = new AtomicInteger();
    private final AtomicInteger readAfterReleaseChanges = new AtomicInteger();
    private final AtomicInteger selectSecondChanges = new AtomicInteger();
    private final AtomicInteger openSecondaryChanges = new AtomicInteger();
    private final AtomicInteger closeSecondaryChanges = new AtomicInteger();

    private volatile DesktopFileReadGrant firstGrant;
    @SuppressWarnings("FieldCanBeLocal")
    private volatile DesktopFileReadGrant secondGrant;

    public FlowFileReadGrantRoute() {
        setId(ID_PREFIX + "route");
        setPadding(true);
        setSpacing(true);

        Span openSecondaryStatus = pendingStatus(
                "File grant secondary open", "open-secondary-status");
        Button openSecondary = new Button(
                "Open file read grant window", event -> {
                    DesktopCommandResult result = Desktop.current().windows()
                            .open(DesktopWindow.builder(SECONDARY_WINDOW_ID)
                                    .title("Flow file read grant")
                                    .size(760, 520)
                                    .route("flow-file-read-grant")
                                    .build());
                    openSecondaryStatus.setText(
                            "File grant secondary open: opened="
                                    + result.success()
                                    + " client=" + event.isFromClient()
                                    + " change="
                                    + openSecondaryChanges.incrementAndGet());
                });
        openSecondary.setId(ID_PREFIX + "open-secondary");

        Span closeSecondaryStatus = pendingStatus(
                "File grant secondary close", "close-secondary-status");
        Button closeSecondary = new Button(
                "Close file read grant window", event -> {
                    DesktopCommandResult result = Desktop.current().windows()
                            .close(SECONDARY_WINDOW_ID);
                    closeSecondaryStatus.setText(
                            "File grant secondary close: closed="
                                    + result.success()
                                    + " client=" + event.isFromClient()
                                    + " change="
                                    + closeSecondaryChanges.incrementAndGet());
                });
        closeSecondary.setId(ID_PREFIX + "close-secondary");

        Span selectFirstStatus = pendingStatus(
                "First file grant selection", "select-first-status");
        Button selectFirst = new Button(
                "Select first file read grant", event -> {
                    boolean client = event.isFromClient();
                    UI ui = event.getSource().getUI().orElseThrow();
                    DesktopCapabilityContext capabilityContext = Desktop.current()
                            .captureCapabilityContext();
                    Thread.ofVirtual().name("file-read-grant-select-first")
                            .start(() -> {
                                boolean backgroundSessionAbsent =
                                        VaadinSession.getCurrent() == null;
                                DesktopFileReadGrantResult result =
                                        capabilityContext.fileDialog()
                                                .openReadGrant(FIRST_TITLE);
                                DesktopFileReadGrant grant = result.grant()
                                        .orElse(null);
                                firstGrant = grant;
                                String status = formatSelection(
                                        "First file grant selection", result,
                                        grant, backgroundSessionAbsent, client,
                                        selectFirstChanges.incrementAndGet());
                                ui.access(() -> selectFirstStatus.setText(status));
                            });
                });
        selectFirst.setId(ID_PREFIX + "select-first");

        Span readFirstStatus = pendingStatus(
                "First file grant read", "read-first-status");
        Button readFirst = new Button("Read first file grant", event -> {
            boolean client = event.isFromClient();
            UI ui = event.getSource().getUI().orElseThrow();
            DesktopFileReadGrant grant = firstGrant;
            Thread.ofVirtual().name("file-read-grant-read-first")
                    .start(() -> {
                        String status;
                        if (grant == null) {
                            status = formatMissingGrant(
                                    "First file grant read", client,
                                    readFirstChanges.incrementAndGet());
                        } else {
                            DesktopFileReadResult result = grant.read(
                                    0, FIRST_READ_BYTES);
                            boolean contentMatches = result.outcome()
                                    == DesktopFileReadResult.Outcome.DATA
                                    && Arrays.equals(result.bytes(),
                                            FIRST_READ_EXPECTED);
                            status = formatRead(
                                    "First file grant read", result, grant,
                                    client,
                                    readFirstChanges.incrementAndGet())
                                    + " contentMatches=" + contentMatches;
                        }
                        String completedStatus = status;
                        ui.access(() -> readFirstStatus.setText(
                                completedStatus));
                    });
        });
        readFirst.setId(ID_PREFIX + "read-first");

        Span releaseFirstStatus = pendingStatus(
                "First file grant release", "release-first-status");
        Button releaseFirst = new Button(
                "Release first file read grant", event -> {
                    boolean client = event.isFromClient();
                    UI ui = event.getSource().getUI().orElseThrow();
                    DesktopFileReadGrant grant = firstGrant;
                    Thread.ofVirtual().name("file-read-grant-release-first")
                            .start(() -> {
                                String status;
                                if (grant == null) {
                                    status = formatMissingGrant(
                                            "First file grant release", client,
                                            releaseFirstChanges
                                                    .incrementAndGet());
                                } else {
                                    DesktopFileGrantReleaseResult result =
                                            grant.release();
                                    status = "First file grant release: outcome="
                                            + result.outcome().name()
                                            + formatGrant(grant)
                                            + " client=" + client
                                            + " change="
                                            + releaseFirstChanges
                                                    .incrementAndGet();
                                }
                                String completedStatus = status;
                                ui.access(() -> releaseFirstStatus.setText(
                                        completedStatus));
                            });
                });
        releaseFirst.setId(ID_PREFIX + "release-first");

        Span readAfterReleaseStatus = pendingStatus(
                "First file grant read after release",
                "read-after-release-status");
        Button readAfterRelease = new Button(
                "Read first file grant after release", event -> {
                    boolean client = event.isFromClient();
                    UI ui = event.getSource().getUI().orElseThrow();
                    DesktopFileReadGrant grant = firstGrant;
                    Thread.ofVirtual()
                            .name("file-read-grant-read-after-release")
                            .start(() -> {
                                String status;
                                if (grant == null) {
                                    status = formatMissingGrant(
                                            "First file grant read after release",
                                            client,
                                            readAfterReleaseChanges
                                                    .incrementAndGet());
                                } else {
                                    DesktopFileReadResult result = grant.read(
                                            0, FIRST_READ_BYTES);
                                    status = formatRead(
                                            "First file grant read after release",
                                            result, grant, client,
                                            readAfterReleaseChanges
                                                    .incrementAndGet());
                                }
                                String completedStatus = status;
                                ui.access(() -> readAfterReleaseStatus.setText(
                                        completedStatus));
                            });
                });
        readAfterRelease.setId(ID_PREFIX + "read-after-release");

        Span selectSecondStatus = pendingStatus(
                "Second file grant selection", "select-second-status");
        Button selectSecond = new Button(
                "Select second file read grant", event -> {
                    boolean client = event.isFromClient();
                    UI ui = event.getSource().getUI().orElseThrow();
                    DesktopCapabilityContext capabilityContext = Desktop.current()
                            .captureCapabilityContext();
                    Thread.ofVirtual().name("file-read-grant-select-second")
                            .start(() -> {
                                boolean backgroundSessionAbsent =
                                        VaadinSession.getCurrent() == null;
                                DesktopFileReadGrantResult result =
                                        capabilityContext.fileDialog()
                                                .openReadGrant(SECOND_TITLE);
                                DesktopFileReadGrant grant = result.grant()
                                        .orElse(null);
                                secondGrant = grant;
                                String status = formatSelection(
                                        "Second file grant selection", result,
                                        grant, backgroundSessionAbsent, client,
                                        selectSecondChanges.incrementAndGet());
                                ui.access(() -> selectSecondStatus.setText(
                                        status));
                            });
                });
        selectSecond.setId(ID_PREFIX + "select-second");

        add(new H1("Flow file read grant acceptance"),
                openSecondary,
                openSecondaryStatus,
                closeSecondary,
                closeSecondaryStatus,
                selectFirst,
                selectFirstStatus,
                readFirst,
                readFirstStatus,
                releaseFirst,
                releaseFirstStatus,
                readAfterRelease,
                readAfterReleaseStatus,
                selectSecond,
                selectSecondStatus);
    }

    private static Span pendingStatus(String label, String idSuffix) {
        Span status = new Span(label + ": pending");
        status.setId(ID_PREFIX + idSuffix);
        return status;
    }

    private static String formatSelection(String label,
            DesktopFileReadGrantResult result, DesktopFileReadGrant grant,
            boolean backgroundSessionAbsent, boolean client, int change) {
        return label
                + ": outcome=" + result.outcome().name()
                + formatGrant(grant)
                + " capabilityContextCaptured=true"
                + " backgroundSessionAbsent=" + backgroundSessionAbsent
                + " client=" + client
                + " change=" + change;
    }

    private static String formatRead(String label,
            DesktopFileReadResult result, DesktopFileReadGrant grant,
            boolean client, int change) {
        byte[] bytes = result.bytes();
        return label
                + ": outcome=" + result.outcome().name()
                + " requestedOffset=0"
                + " requestedBytes=" + FIRST_READ_BYTES
                + " bytesRead=" + bytes.length
                + " offset=" + result.offset()
                + " nextOffset=" + result.nextOffset()
                + " eof=" + result.endOfFile()
                + formatGrant(grant)
                + " client=" + client
                + " change=" + change;
    }

    private static String formatMissingGrant(
            String label, boolean client, int change) {
        return label
                + ": outcome=ERROR"
                + " missingGrant=true"
                + formatGrant(null)
                + " client=" + client
                + " change=" + change;
    }

    private static String formatGrant(DesktopFileReadGrant grant) {
        if (grant == null) {
            return " grantPresent=false"
                    + " filename=none"
                    + " byteCount=-1"
                    + " maxReadBytes=-1"
                    + " securityScoped=false";
        }
        return " grantPresent=true"
                + " filename=" + grant.filename()
                + " byteCount=" + grant.byteCount()
                + " maxReadBytes=" + grant.maxReadBytes()
                + " securityScoped=" + grant.securityScoped();
    }
}
