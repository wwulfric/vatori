package dev.vatori.ipcsmoke;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.LoadMode;
import dev.vatori.desktop.Desktop;
import dev.vatori.desktop.DesktopCommand;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Route("")
@JavaScript(value = "context://vatori-flow-ipc-lazy-v1_10.js", loadMode = LoadMode.LAZY)
public class FlowOverIpcSmokeView extends VerticalLayout {
    private static final Path MODULE_BUILD = Path.of(System.getProperty("user.dir")).resolve(".build");
    private static final Path DOWNLOAD_TARGET = MODULE_BUILD.resolve("flow-ipc-download-smoke.txt");
    private static final Path UPLOAD_SOURCE = MODULE_BUILD.resolve("flow-ipc-upload-smoke.txt");

    private int count;

    public FlowOverIpcSmokeView() {
        setId("ipc-root");
        setPadding(true);
        setSpacing(true);

        H1 title = new H1("Vaadin Flow over IPC Smoke App");
        title.setId("ipc-title");

        Span status = new Span("Clicks: 0");
        status.setId("ipc-status");

        Span fileTransferStatus = new Span("File transfer: pending");
        fileTransferStatus.setId("ipc-file-transfer-status");

        Button button = new Button("Increment over IPC", event -> {
            count++;
            status.setText("Clicks: " + count);
            Desktop.current().setWindowTitle("Vaadin Flow over IPC - " + count);
            Desktop.current().notifications().show("Flow IPC", "Button clicked " + count + " time(s)");
            Desktop.current().clipboard().writeText("Vaadin Flow over IPC clicks: " + count);
            Desktop.current().fileDialog().openFile("Flow IPC File Dialog");
            runFileTransferSmoke(fileTransferStatus);
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

        add(title, button, status, list, fileTransferStatus);

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

        BigDecimalField bigDecimalField = new BigDecimalField("Vaadin BigDecimalField over IPC");
        bigDecimalField.setId("ipc-vaadin-big-decimal");
        bigDecimalField.setPlaceholder("42.5");
        Span bigDecimalStatus = new Span("Vaadin BigDecimalField: pending");
        bigDecimalStatus.setId("ipc-vaadin-big-decimal-status");
        bigDecimalField.getElement().addEventListener("change", event -> {
            JsonNode valueNode = event.getEventData().get("element.value");
            String value = valueNode == null ? "" : valueNode.asString();
            bigDecimalStatus.setText("Vaadin BigDecimalField: " + value);
        }).addEventData("element.value");

        TextField textField = new TextField("Vaadin TextField over IPC");
        textField.setId("ipc-vaadin-text-field");
        textField.setPlaceholder("Flow IPC typed");
        Span textFieldStatus = new Span("Vaadin TextField: pending");
        textFieldStatus.setId("ipc-vaadin-text-field-status");
        textField.getElement().addEventListener("change", event -> {
            JsonNode valueNode = event.getEventData().get("element.value");
            String value = valueNode == null ? "" : valueNode.asString();
            textFieldStatus.setText("Vaadin TextField: " + value);
        }).addEventData("element.value");

        Checkbox vaadinCheckbox = new Checkbox("Vaadin Checkbox over IPC");
        vaadinCheckbox.setId("ipc-vaadin-checkbox");
        Span vaadinCheckboxStatus = new Span("Vaadin Checkbox: pending");
        vaadinCheckboxStatus.setId("ipc-vaadin-checkbox-status");
        vaadinCheckbox.getElement().addEventListener("change", event -> {
            JsonNode checkedNode = event.getEventData().get("element.checked");
            boolean checked = checkedNode != null && checkedNode.asBoolean();
            vaadinCheckboxStatus.setText("Vaadin Checkbox: " + (checked ? "checked" : "unchecked"));
        }).addEventData("element.checked");

        Select<String> vaadinSelect = new Select<>();
        vaadinSelect.setId("ipc-vaadin-select");
        vaadinSelect.setLabel("Vaadin Select over IPC");
        vaadinSelect.setItems("standard", "audit", "deferred");
        vaadinSelect.setValue("standard");
        Span vaadinSelectStatus = new Span("Vaadin Select: pending");
        vaadinSelectStatus.setId("ipc-vaadin-select-status");
        vaadinSelect.getElement().addEventListener("change", event -> {
            JsonNode valueNode = event.getEventData().get("element.value");
            String value = valueNode == null ? "" : valueNode.asString();
            vaadinSelectStatus.setText("Vaadin Select: " + value);
        }).addEventData("element.value");

        VerticalLayout eventMatrix = new VerticalLayout();
        eventMatrix.setId("ipc-event-matrix");
        eventMatrix.setPadding(false);
        eventMatrix.setSpacing(false);
        eventMatrix.add(new Span("6 Component event matrix over IPC READY"),
                textInput, textStatus, checkbox, checkboxStatus,
                bigDecimalField, bigDecimalStatus,
                textField, textFieldStatus,
                vaadinCheckbox, vaadinCheckboxStatus,
                vaadinSelect, vaadinSelectStatus);
        add(eventMatrix);
    }

    private void runFileTransferSmoke(Span fileTransferStatus) {
        try {
            Files.createDirectories(UPLOAD_SOURCE.getParent());
            Files.writeString(UPLOAD_SOURCE, "Flow IPC upload smoke\n", StandardCharsets.UTF_8);
            Desktop.current().fileTransfer().downloadText(
                    "flow-ipc-download-smoke.txt",
                    "text/plain; charset=utf-8",
                    "Flow IPC download smoke\n",
                    DOWNLOAD_TARGET);
            Desktop.current().fileTransfer().selectUploadFile("Flow IPC Upload", UPLOAD_SOURCE);
            fileTransferStatus.setText("File transfer: requested");
        } catch (IOException error) {
            fileTransferStatus.setText("File transfer: failed " + error.getClass().getSimpleName());
        }
    }
}
