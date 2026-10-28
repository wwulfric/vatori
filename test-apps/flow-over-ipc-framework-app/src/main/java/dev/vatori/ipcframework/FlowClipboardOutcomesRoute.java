package dev.vatori.ipcframework;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import dev.vatori.desktop.Desktop;
import dev.vatori.desktop.DesktopClipboardReadResult;
import dev.vatori.desktop.DesktopClipboardWriteResult;
import dev.vatori.desktop.DesktopCommandResult;
import dev.vatori.desktop.DesktopWindow;

import java.util.concurrent.atomic.AtomicInteger;

@Route("flow-clipboard-outcomes")
public final class FlowClipboardOutcomesRoute extends VerticalLayout {
    static final String SECONDARY_WINDOW_ID =
            "clipboard-outcomes-secondary";
    static final String DENIED_WRITE_PROBE =
            "clipboard-denied-probe";

    private final AtomicInteger noTextCount = new AtomicInteger();
    private final AtomicInteger emptyCount = new AtomicInteger();
    private final AtomicInteger deniedCount = new AtomicInteger();

    public FlowClipboardOutcomesRoute() {
        setId("framework-flow-clipboard-outcomes-route");
        setPadding(true);
        setSpacing(true);

        Span noTextStatus = new Span("Clipboard no text: pending");
        noTextStatus.setId(
                "framework-flow-clipboard-outcomes-no-text-status");
        Button readNoText = new Button(
                "Read clipboard without text", event -> {
                    DesktopClipboardReadResult result = Desktop.current()
                            .clipboard().readTextResult();
                    noTextStatus.setText(
                            "Clipboard no text: outcome="
                                    + result.outcome().name()
                                    + " textPresent="
                                    + result.text().isPresent()
                                    + " length=" + textLength(result)
                                    + " client=" + event.isFromClient()
                                    + " count="
                                    + noTextCount.incrementAndGet());
                });
        readNoText.setId(
                "framework-flow-clipboard-outcomes-read-no-text");

        Span emptyStatus = new Span("Clipboard empty: pending");
        emptyStatus.setId(
                "framework-flow-clipboard-outcomes-empty-status");
        Button writeAndReadEmpty = new Button(
                "Write and read empty clipboard text", event -> {
                    DesktopClipboardWriteResult writeResult =
                            Desktop.current().clipboard().writeTextResult("");
                    DesktopClipboardReadResult readResult =
                            Desktop.current().clipboard().readTextResult();
                    emptyStatus.setText(
                            "Clipboard empty: writeOutcome="
                                    + writeResult.outcome().name()
                                    + " readOutcome="
                                    + readResult.outcome().name()
                                    + " textPresent="
                                    + readResult.text().isPresent()
                                    + " length=" + textLength(readResult)
                                    + " client=" + event.isFromClient()
                                    + " count="
                                    + emptyCount.incrementAndGet());
                });
        writeAndReadEmpty.setId(
                "framework-flow-clipboard-outcomes-write-read-empty");

        Span deniedStatus = new Span("Clipboard denied: pending");
        deniedStatus.setId(
                "framework-flow-clipboard-outcomes-denied-status");
        Button invokeDenied = new Button(
                "Invoke denied clipboard write and read", event -> {
                    DesktopClipboardWriteResult writeResult =
                            Desktop.current().clipboard()
                                    .writeTextResult(DENIED_WRITE_PROBE);
                    DesktopClipboardReadResult readResult =
                            Desktop.current().clipboard().readTextResult();
                    deniedStatus.setText(
                            "Clipboard denied: writeOutcome="
                                    + writeResult.outcome().name()
                                    + " writeLength="
                                    + DENIED_WRITE_PROBE.length()
                                    + " readOutcome="
                                    + readResult.outcome().name()
                                    + " textPresent="
                                    + readResult.text().isPresent()
                                    + " length=" + textLength(readResult)
                                    + " client=" + event.isFromClient()
                                    + " count="
                                    + deniedCount.incrementAndGet());
                });
        invokeDenied.setId(
                "framework-flow-clipboard-outcomes-invoke-denied");

        Span windowStatus = new Span("Clipboard window: pending");
        windowStatus.setId(
                "framework-flow-clipboard-outcomes-window-status");
        Button openSecondary = new Button(
                "Open denied clipboard window", event -> {
                    DesktopCommandResult result = Desktop.current().windows()
                            .open(DesktopWindow.builder(SECONDARY_WINDOW_ID)
                                    .title("Flow clipboard outcomes denied")
                                    .size(760, 520)
                                    .route("flow-clipboard-outcomes")
                                    .build());
                    windowStatus.setText(
                            "Clipboard window: opened=" + result.success()
                                    + " client=" + event.isFromClient()
                                    + " error=" + errorValue(result));
                });
        openSecondary.setId(
                "framework-flow-clipboard-outcomes-open-secondary");

        add(new H1("Flow clipboard typed outcomes acceptance"),
                readNoText,
                noTextStatus,
                writeAndReadEmpty,
                emptyStatus,
                invokeDenied,
                deniedStatus,
                openSecondary,
                windowStatus);
    }

    private static int textLength(DesktopClipboardReadResult result) {
        return result.text().map(String::length).orElse(0);
    }

    private static String errorValue(DesktopCommandResult result) {
        String error = result.error();
        return error == null || error.isBlank() ? "none" : error;
    }
}
