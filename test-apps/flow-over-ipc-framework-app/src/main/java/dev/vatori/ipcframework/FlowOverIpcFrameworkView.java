package dev.vatori.ipcframework;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import dev.vatori.desktop.Desktop;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

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

        List<WorkItem> workItems = List.of(
                new WorkItem("A-100", "platform", "queued"),
                new WorkItem("A-101", "desktop", "review"),
                new WorkItem("A-102", "audit", "ready"));
        List<WorkItem> lazyWorkItems = IntStream.rangeClosed(1, 80)
                .mapToObj(index -> new WorkItem("L-" + String.format("%03d", index),
                        switch (index % 3) {
                            case 0 -> "platform";
                            case 1 -> "desktop";
                            default -> "audit";
                        },
                        "page-" + ((index - 1) / 10)))
                .toList();

        Grid<WorkItem> grid = new Grid<>(WorkItem.class, false);
        grid.setId("framework-grid");
        grid.addColumn(WorkItem::id).setHeader("Ticket");
        grid.addColumn(WorkItem::owner).setHeader("Owner");
        grid.addColumn(WorkItem::state).setHeader("State");
        grid.setItems(workItems);
        grid.setAllRowsVisible(true);

        Span lazyGridStatus = new Span("Lazy grid: pending");
        lazyGridStatus.setId("framework-lazy-grid-status");
        AtomicInteger lazyGridFetchCount = new AtomicInteger();
        AtomicInteger lazyGridCountCount = new AtomicInteger();
        AtomicReference<String> lazyGridLastRange = new AtomicReference<>("pending");

        Grid<WorkItem> lazyGrid = new Grid<>(WorkItem.class, false);
        lazyGrid.setId("framework-lazy-grid");
        lazyGrid.addColumn(WorkItem::id).setHeader("Lazy ticket");
        lazyGrid.addColumn(WorkItem::owner).setHeader("Lazy owner");
        lazyGrid.addColumn(WorkItem::state).setHeader("Lazy state");
        lazyGrid.setPageSize(5);
        lazyGrid.setHeight("220px");
        lazyGrid.setItems(query -> {
            int offset = query.getOffset();
            int limit = query.getLimit();
            int end = Math.min(offset + limit, lazyWorkItems.size());
            int fetchCount = lazyGridFetchCount.incrementAndGet();
            lazyGridLastRange.set(offset + "-" + end);
            lazyGridStatus.setText("Lazy grid fetch " + fetchCount + ": " + offset + "-" + end);
            return lazyWorkItems.stream().skip(offset).limit(limit);
        }, query -> {
            lazyGridCountCount.incrementAndGet();
            return lazyWorkItems.size();
        });

        Button lazyGridReport = new Button("Report Lazy Grid", event ->
                lazyGridStatus.setText("Lazy grid fetches: " + lazyGridFetchCount.get()
                        + ", count calls: " + lazyGridCountCount.get()
                        + ", range: " + lazyGridLastRange.get()));
        lazyGridReport.setId("framework-lazy-grid-report");

        byte[] streamResourceBytes = """
                Flow IPC StreamResource v1.50
                owner=framework-validation-app
                transport=flow-ipc-resource-request
                """.getBytes(StandardCharsets.UTF_8);
        StreamResource streamResource = new StreamResource("flow-ipc-framework-v1_50.txt",
                (stream, session) -> stream.write(streamResourceBytes));
        streamResource.setContentType("text/plain; charset=UTF-8");
        streamResource.setHeader("X-Vatori-Flow-IPC", "stream-resource-v1_50");

        Anchor streamResourceDownload = new Anchor(streamResource, "Download StreamResource");
        streamResourceDownload.setId("framework-stream-resource-download");
        streamResourceDownload.getElement().setAttribute("download", true);

        Span streamResourceStatus = new Span("StreamResource: pending");
        streamResourceStatus.setId("framework-stream-resource-status");

        byte[] binaryStreamResourceBytes = binaryStreamResourceBytes();
        StreamResource binaryStreamResource = new StreamResource("flow-ipc-framework-v1_51.bin",
                (stream, session) -> stream.write(binaryStreamResourceBytes));
        binaryStreamResource.setContentType("application/octet-stream");
        binaryStreamResource.setHeader("X-Vatori-Flow-IPC", "stream-resource-range-v1_51");

        Anchor streamResourceRangeDownload = new Anchor(binaryStreamResource, "Download Binary StreamResource");
        streamResourceRangeDownload.setId("framework-stream-resource-range-download");
        streamResourceRangeDownload.getElement().setAttribute("download", true);

        Span streamResourceRangeStatus = new Span("StreamResource range: pending");
        streamResourceRangeStatus.setId("framework-stream-resource-range-status");

        Span streamResourceChunksStatus = new Span("StreamResource chunks: pending");
        streamResourceChunksStatus.setId("framework-stream-resource-chunks-status");

        VirtualList<String> virtualList = new VirtualList<>();
        virtualList.setId("framework-virtual-list");
        virtualList.setItems(List.of("Queue alpha", "Queue beta", "Queue release"));
        virtualList.setRenderer(item -> item);

        OwnerForm ownerForm = new OwnerForm();
        ownerForm.setOwner("platform");
        Binder<OwnerForm> binder = new Binder<>(OwnerForm.class);
        TextField ownerName = new TextField("Owner name");
        ownerName.setId("framework-owner-name");
        ownerName.setValue(ownerForm.getOwner());
        binder.forField(ownerName)
                .withValidator(value -> value != null && value.trim().length() >= 3,
                        "Owner name must have at least three characters")
                .bind(OwnerForm::getOwner, OwnerForm::setOwner);
        binder.setBean(ownerForm);

        Span validationStatus = new Span("Validation: pending");
        validationStatus.setId("framework-validation-status");

        ownerName.getElement().addEventListener("change", event -> {
            JsonNode valueNode = event.getEventData().get("element.value");
            String value = valueNode == null ? "" : valueNode.asString();
            ownerName.setValue(value);
        }).addEventData("element.value");

        Button validateOwner = new Button("Validate Owner", event -> {
            if (binder.writeBeanIfValid(ownerForm)) {
                validationStatus.setText("Validation accepted: " + ownerForm.getOwner());
            } else {
                validationStatus.setText("Validation rejected");
            }
        });
        validateOwner.setId("framework-validation-action");

        FormLayout formLayout = new FormLayout();
        formLayout.setId("framework-form-layout");
        formLayout.addFormItem(ownerName, "Owner");
        formLayout.add(validateOwner, validationStatus);

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
                grid, lazyGrid, lazyGridStatus, lazyGridReport, streamResourceDownload,
                streamResourceStatus, streamResourceRangeDownload, streamResourceRangeStatus,
                streamResourceChunksStatus, virtualList, formLayout,
                apply, status, componentStatus, matrixStatus,
                updateList);
    }

    private void appendUpdate(String text) {
        updateList.add(new Span(text));
    }

    private static byte[] binaryStreamResourceBytes() {
        byte[] bytes = new byte[8192];
        for (int index = 0; index < bytes.length; index++) {
            bytes[index] = (byte) ((index * 31 + 17) & 0xff);
        }
        byte[] marker = "Flow IPC Binary StreamResource v1.51\n".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(marker, 0, bytes, 0, marker.length);
        byte[] tail = "\nrange-tail=v1_51".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(tail, 0, bytes, bytes.length - tail.length, tail.length);
        return bytes;
    }

    private record WorkItem(String id, String owner, String state) {
    }

    public static final class OwnerForm {
        private String owner;

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }
    }
}
