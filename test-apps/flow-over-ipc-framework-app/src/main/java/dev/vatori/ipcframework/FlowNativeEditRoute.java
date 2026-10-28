package dev.vatori.ipcframework;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.Autocapitalize;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import dev.vatori.desktop.Desktop;
import dev.vatori.desktop.DesktopCommandResult;
import dev.vatori.desktop.DesktopWindow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Observes ordinary TextField changes without implementing editing commands. */
@Route("flow-native-edit")
public final class FlowNativeEditRoute extends VerticalLayout {
    public FlowNativeEditRoute() {
        setId("framework-flow-native-edit-route");
        setPadding(true);
        setSpacing(true);

        TextField field = new TextField("Native edit text");
        field.setId("framework-flow-native-edit-field");
        field.setAriaLabel("Native edit text");
        field.setWidth("26em");
        // Isolate undo ownership from platform text substitution.
        field.setAutocorrect(false);
        field.setAutocapitalize(Autocapitalize.NONE);
        field.setValueChangeMode(ValueChangeMode.EAGER);
        field.setValue("");

        Span configStatus = new Span(
                "Native edit config: component=TextField, mode="
                        + field.getValueChangeMode()
                        + ", initial=" + valueToken(field.getValue())
                        + ", initialLength=" + field.getValue().length());
        configStatus.setId("framework-flow-native-edit-config-status");
        Span valueStatus = new Span("Native edit value: "
                + valueSummary(field.getValue(), false, 0, null));
        valueStatus.setId("framework-flow-native-edit-value-status");
        Span historyStatus = new Span("Native edit history: pending");
        historyStatus.setId("framework-flow-native-edit-history-status");

        AtomicInteger valueChanges = new AtomicInteger();
        List<String> history = new ArrayList<>();
        field.addValueChangeListener(event -> {
            String entry = valueSummary(event.getValue(), event.isFromClient(),
                    valueChanges.incrementAndGet(), event.getOldValue());
            valueStatus.setText("Native edit value: " + entry);
            history.add(entry);
            historyStatus.setText("Native edit history: "
                    + String.join(" | ", history));
        });

        Span windowStatus = new Span("Native edit window: pending");
        windowStatus.setId("framework-flow-native-edit-window-status");
        AtomicInteger windowChanges = new AtomicInteger();
        Span closeStatus = new Span("Native edit close: pending");
        closeStatus.setId("framework-flow-native-edit-close-status");
        AtomicInteger closeChanges = new AtomicInteger();
        Button closeSecondary = new Button("Close native edit secondary");
        closeSecondary.setId("framework-flow-native-edit-close-secondary");
        closeSecondary.setEnabled(false);
        Button openSecondary = new Button("Open native edit secondary", event -> {
            DesktopCommandResult result = Desktop.current().windows().open(
                    DesktopWindow.builder("native-edit-secondary")
                            .title("Flow native edit secondary")
                            .size(760, 520)
                            .route("flow-native-edit")
                            .build());
            String error = result.error();
            windowStatus.setText("Native edit window: opened=" + result.success()
                    + ", client=" + event.isFromClient()
                    + ", count=" + windowChanges.incrementAndGet()
                    + ", error=" + (error == null || error.isBlank() ? "none" : error));
            if (result.success()) {
                event.getSource().setEnabled(false);
                closeSecondary.setEnabled(true);
            }
        });
        openSecondary.setId("framework-flow-native-edit-open-secondary");
        closeSecondary.addClickListener(event -> {
            DesktopCommandResult result = Desktop.current().windows()
                    .close("native-edit-secondary");
            String error = result.error();
            // A successful command schedules native close; it is not a detach receipt.
            closeStatus.setText("Native edit close: closing=" + result.success()
                    + ", client=" + event.isFromClient()
                    + ", count=" + closeChanges.incrementAndGet()
                    + ", error=" + (error == null || error.isBlank() ? "none" : error));
            if (result.success()) {
                event.getSource().setEnabled(false);
                openSecondary.setEnabled(true);
            }
        });

        add(new H1("Flow native editing"), field, openSecondary, windowStatus,
                configStatus, valueStatus, historyStatus);
        add(closeSecondary, closeStatus);
    }

    private static String valueSummary(
            String value, boolean client, int change, String oldValue) {
        return "value=" + valueToken(value)
                + ", length=" + value.length()
                + ", client=" + client
                + ", change=" + change
                + ", oldValue=" + valueToken(oldValue)
                + ", oldLength=" + (oldValue == null ? -1 : oldValue.length());
    }

    private static String valueToken(String value) {
        return value == null ? "none" : value.isEmpty() ? "empty" : value;
    }
}
