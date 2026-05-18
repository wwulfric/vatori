package dev.vatori.ipcsmoke;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import dev.vatori.desktop.Desktop;
import dev.vatori.desktop.DesktopCommand;
import tools.jackson.databind.JsonNode;

import java.util.Map;

@Route("")
public class FlowOverIpcSmokeView extends VerticalLayout {
    private int count;

    public FlowOverIpcSmokeView() {
        setId("ipc-root");
        setPadding(true);
        setSpacing(true);

        H1 title = new H1("Vaadin Flow over IPC Smoke App");
        title.setId("ipc-title");

        Span status = new Span("Clicks: 0");
        status.setId("ipc-status");

        Button button = new Button("Increment over IPC", event -> {
            count++;
            status.setText("Clicks: " + count);
            Desktop.current().setWindowTitle("Vaadin Flow over IPC - " + count);
            Desktop.current().notifications().show("Flow IPC", "Button clicked " + count + " time(s)");
            Desktop.current().clipboard().writeText("Vaadin Flow over IPC clicks: " + count);
            Desktop.current().fileDialog().openFile("Flow IPC File Dialog");
            Desktop.current().nativeBridge().invoke(new DesktopCommand("diagnostics.deniedCapability",
                    Map.of("reason", "manifest-deny-smoke")));
        });
        button.setId("ipc-button");

        VerticalLayout list = new VerticalLayout();
        list.setId("ipc-list");
        list.setPadding(false);
        list.setSpacing(false);
        list.add(new Span("1 Bootstrap over IPC READY"));
        list.add(new Span("2 Event dispatch over IPC READY"));
        list.add(new Span("3 Replace-root patch over IPC READY"));
        list.add(new Span("4 Desktop command/capabilityResult over IPC READY"));

        add(title, button, status, list);

        Input textInput = new Input();
        textInput.setId("ipc-text-input");
        textInput.setPlaceholder("Text event over IPC");
        Span textStatus = new Span("Text input: pending");
        textStatus.setId("ipc-text-status");
        textInput.getElement().addEventListener("change", event -> {
            JsonNode valueNode = event.getEventData().get("element.value");
            String value = valueNode == null ? "" : valueNode.asString();
            textStatus.setText("Text input: " + value);
        }).addEventData("element.value");

        Input checkbox = new Input();
        checkbox.setId("ipc-checkbox");
        checkbox.setType("checkbox");
        Span checkboxStatus = new Span("Checkbox: pending");
        checkboxStatus.setId("ipc-checkbox-status");
        checkbox.getElement().addEventListener("change", event -> {
            JsonNode checkedNode = event.getEventData().get("element.checked");
            boolean checked = checkedNode != null && checkedNode.asBoolean();
            checkboxStatus.setText("Checkbox: " + (checked ? "checked" : "unchecked"));
        }).addEventData("element.checked");

        VerticalLayout eventMatrix = new VerticalLayout();
        eventMatrix.setId("ipc-event-matrix");
        eventMatrix.setPadding(false);
        eventMatrix.setSpacing(false);
        eventMatrix.add(new Span("6 Component event matrix over IPC READY"),
                textInput, textStatus, checkbox, checkboxStatus);
        add(eventMatrix);
    }
}
