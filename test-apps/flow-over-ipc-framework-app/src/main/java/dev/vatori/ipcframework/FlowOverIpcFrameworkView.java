package dev.vatori.ipcframework;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.router.Route;
import dev.vatori.desktop.Desktop;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Route("")
@com.vaadin.flow.component.dependency.JavaScript(
        value = "context://vatori-flow-ipc-lazy-v1_10.js",
        loadMode = com.vaadin.flow.shared.ui.LoadMode.LAZY)
public class FlowOverIpcFrameworkView extends VerticalLayout {
    private final VerticalLayout updateList = new VerticalLayout();
    private int revision;
    private String priorityValue = "standard";
    private String reviewerValue = "platform";
    private String dueDateValue = "2026-06-15";

    public FlowOverIpcFrameworkView() {
        setId("framework-root");
        setPadding(true);
        setSpacing(true);
        setWidthFull();

        H1 title = new H1("Flow IPC Framework Desk");
        Span status = new Span("Ready for framework validation");
        status.setId("framework-status");

        BigDecimalField budget = new BigDecimalField("Budget");
        budget.setId("framework-budget");
        budget.setPlaceholder("42.5");
        budget.setValue(new BigDecimal("42.5"));

        Checkbox expedited = new Checkbox("Expedite processing");
        expedited.setId("framework-expedite");

        Select<String> priority = new Select<>();
        priority.setId("framework-priority");
        priority.setLabel("Priority");
        priority.setItems("standard", "audit", "deferred");
        priority.setValue(priorityValue);

        ComboBox<String> reviewer = new ComboBox<>("Reviewer");
        reviewer.setId("framework-reviewer");
        reviewer.setItems("platform", "desktop", "audit");
        reviewer.setValue(reviewerValue);

        DatePicker dueDate = new DatePicker("Due date");
        dueDate.setId("framework-due-date");
        dueDate.setValue(LocalDate.of(2026, 6, 15));

        Span matrixStatus = new Span("Component matrix: pending");
        matrixStatus.setId("framework-matrix-status");

        Dialog detailsDialog = new Dialog();
        detailsDialog.setId("framework-dialog");
        Span dialogContent = new Span("Dialog details pending");
        dialogContent.setId("framework-dialog-content");
        detailsDialog.add(dialogContent);

        Span componentStatus = new Span("Components: pending");
        componentStatus.setId("framework-component-status");
        Span latestUpdate = new Span("Revision pending");
        latestUpdate.setId("framework-latest-update");

        budget.getElement().addEventListener("change", event -> {
            JsonNode valueNode = event.getEventData().get("element.value");
            String value = valueNode == null ? "" : valueNode.asString();
            componentStatus.setText("Budget changed: " + value);
        }).addEventData("element.value");

        expedited.getElement().addEventListener("change", event -> {
            JsonNode checkedNode = event.getEventData().get("element.checked");
            boolean checked = checkedNode != null && checkedNode.asBoolean();
            componentStatus.setText("Expedite changed: " + (checked ? "yes" : "no"));
        }).addEventData("element.checked");

        priority.getElement().addEventListener("change", event -> {
            JsonNode valueNode = event.getEventData().get("element.value");
            String value = valueNode == null ? "" : valueNode.asString();
            if (!value.isBlank()) {
                priorityValue = value;
            }
            componentStatus.setText("Priority changed: " + priorityValue);
        }).addEventData("element.value");

        reviewer.getElement().addEventListener("change", event -> {
            JsonNode valueNode = event.getEventData().get("element.value");
            String value = valueNode == null ? "" : valueNode.asString();
            if (!value.isBlank()) {
                reviewerValue = value;
            }
            matrixStatus.setText("Reviewer changed: " + reviewerValue);
        }).addEventData("element.value");

        dueDate.getElement().addEventListener("change", event -> {
            JsonNode valueNode = event.getEventData().get("element.value");
            String value = valueNode == null ? "" : valueNode.asString();
            if (!value.isBlank()) {
                dueDateValue = value;
            }
            matrixStatus.setText("Due date changed: " + dueDateValue);
        }).addEventData("element.value");

        Button openDialog = new Button("Open Details", event -> {
            String text = "Dialog opened: " + reviewerValue + " due " + dueDateValue;
            dialogContent.setText(text);
            matrixStatus.setText(text);
            detailsDialog.open();
        });
        openDialog.setId("framework-dialog-open");

        Button apply = new Button("Apply Update", event -> {
            revision++;
            String budgetText = budget.getValue() == null ? "unset" : budget.getValue().toPlainString();
            String mode = expedited.getValue() ? "expedited" : "standard";
            String update = "Revision " + revision + ": budget " + budgetText
                    + ", mode " + mode + ", priority " + priorityValue;
            status.setText(update);
            latestUpdate.setText(update);
            Desktop.current().setWindowTitle("Flow IPC Framework App - " + revision);
            Desktop.current().clipboard().writeText(update);
            Desktop.current().notifications().show("Flow IPC", "Framework update " + revision + " applied");
        });
        apply.setId("framework-apply");

        updateList.setId("framework-updates");
        updateList.setPadding(false);
        updateList.setSpacing(false);
        appendUpdate("Catalog loaded");
        appendUpdate("Renderer owned by Flow client engine");
        updateList.add(latestUpdate);

        add(title, budget, expedited, priority, reviewer, dueDate, openDialog, detailsDialog,
                apply, status, componentStatus, matrixStatus, updateList);
    }

    private void appendUpdate(String text) {
        updateList.add(new Span(text));
    }
}
