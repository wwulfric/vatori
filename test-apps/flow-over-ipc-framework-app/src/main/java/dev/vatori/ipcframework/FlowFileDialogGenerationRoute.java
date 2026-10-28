package dev.vatori.ipcframework;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import dev.vatori.desktop.Desktop;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Route("flow-file-dialog-generation")
public final class FlowFileDialogGenerationRoute extends VerticalLayout {
    private final AtomicInteger changeCount = new AtomicInteger();

    public FlowFileDialogGenerationRoute() {
        setId("framework-flow-file-dialog-generation-route");
        setPadding(true);
        setSpacing(true);

        Span result = new Span(formatResult("none", false, 0));
        result.setId("framework-flow-file-dialog-generation-result");

        Button openGenerationOne = new Button(
                "Open generation one file dialog", event -> {
            Optional<Path> selected = Desktop.current().fileDialog()
                    .openFile("Flow file dialog generation one");
            result.setText(formatResult(
                    selected.map(Path::toString).orElse("none"),
                    event.isFromClient(),
                    changeCount.incrementAndGet()));
        });
        openGenerationOne.setId(
                "framework-flow-file-dialog-generation-one-open");

        Button openGenerationTwo = new Button(
                "Open generation two file dialog", event -> {
            Optional<Path> selected = Desktop.current().fileDialog()
                    .openFile("Flow file dialog generation two");
            result.setText(formatResult(
                    selected.map(Path::toString).orElse("none"),
                    event.isFromClient(),
                    changeCount.incrementAndGet()));
        });
        openGenerationTwo.setId(
                "framework-flow-file-dialog-generation-two-open");

        add(new H1("Flow file dialog generation acceptance"),
                openGenerationOne,
                openGenerationTwo,
                result);
    }

    private static String formatResult(
            String selected, boolean fromClient, int changeCount) {
        return "File dialog generation result: selected=" + selected
                + " client=" + fromClient
                + " change=" + changeCount;
    }
}
