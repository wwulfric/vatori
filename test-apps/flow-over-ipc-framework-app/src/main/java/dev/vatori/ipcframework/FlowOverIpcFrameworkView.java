package dev.vatori.ipcframework;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.avatar.AvatarGroup.AvatarGroupItem;
import com.vaadin.flow.component.avatar.AvatarGroupVariant;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.checkbox.CheckboxGroupVariant;
import com.vaadin.flow.component.checkbox.CheckboxVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBoxVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datepicker.DatePickerVariant;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePickerVariant;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.BoxSizing;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.ScrollerVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.richtexteditor.RichTextEditor;
import com.vaadin.flow.component.richtexteditor.RichTextEditorVariant;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.select.SelectVariant;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.TabsVariant;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextAreaVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.component.timepicker.TimePickerVariant;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.UploadFormat;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginOverlay;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageInputVariant;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.component.sidenav.SideNavVariant;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.streams.DownloadEvent;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.UploadHandler;
import dev.vatori.desktop.Desktop;
import tools.jackson.databind.JsonNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Route("")
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
        Span standardPushStatus = StandardBundlePushProbe.attachTo(this);
        Span status = new Span("Ready for framework validation");
        status.setId("framework-status");

        BigDecimalField budget = new BigDecimalField("Budget");
        budget.setId("framework-budget");
        budget.setPlaceholder("42.5");
        budget.setValue(new BigDecimal("42.5"));
        budget.addThemeVariants(TextFieldVariant.LUMO_SMALL);

        Checkbox expedited = new Checkbox("Expedite processing");
        expedited.setId("framework-expedite");
        expedited.setHelperText("Routes through the fast lane");
        expedited.addThemeVariants(CheckboxVariant.LUMO_HELPER_ABOVE_FIELD);

        CheckboxGroup<String> auditChannels = new CheckboxGroup<>("Audit channels");
        auditChannels.setId("framework-checkbox-group");
        auditChannels.setItems(List.of("security", "compliance", "qa", "release"));
        auditChannels.setItemLabelGenerator(value -> switch (value) {
            case "security" -> "Security review";
            case "compliance" -> "Compliance review";
            case "qa" -> "QA sign-off";
            case "release" -> "Release desk";
            default -> value;
        });
        auditChannels.setValue(Set.of("security"));
        auditChannels.addThemeVariants(CheckboxGroupVariant.LUMO_VERTICAL);
        Span checkboxGroupStatus = new Span("CheckboxGroup: value=security");
        checkboxGroupStatus.setId("framework-checkbox-group-status");
        auditChannels.addValueChangeListener(event -> {
            String value = event.getValue().stream()
                    .sorted()
                    .collect(Collectors.joining(","));
            checkboxGroupStatus.setText("CheckboxGroup client value: " + value);
        });
        Button checkboxGroupServerUpdate = new Button("Set CheckboxGroup", event -> {
            auditChannels.setValue(Set.of("qa", "release"));
            checkboxGroupStatus.setText("CheckboxGroup server value: qa,release");
        });
        checkboxGroupServerUpdate.setId("framework-checkbox-group-server-update");

        Select<String> priority = new Select<>();
        priority.setId("framework-priority");
        priority.setLabel("Priority");
        priority.setItems("standard", "audit", "deferred");
        priority.setValue(priorityValue);
        priority.addThemeVariants(SelectVariant.LUMO_SMALL);

        ComboBox<String> reviewer = new ComboBox<>("Reviewer");
        reviewer.setId("framework-reviewer");
        reviewer.setItems("platform", "desktop", "audit");
        reviewer.setValue(reviewerValue);
        reviewer.addThemeVariants(ComboBoxVariant.LUMO_SMALL);

        DatePicker dueDate = new DatePicker("Due date");
        dueDate.setId("framework-due-date");
        dueDate.setValue(LocalDate.of(2026, 6, 15));
        dueDate.addThemeVariants(DatePickerVariant.LUMO_SMALL);

        Span matrixStatus = new Span("Component matrix: pending");
        matrixStatus.setId("framework-matrix-status");
        dueDate.addValueChangeListener(event -> {
            LocalDate value = event.getValue();
            if (value != null) {
                dueDateValue = value.toString();
            }
            matrixStatus.setText("Due date changed: " + dueDateValue);
        });

        RadioButtonGroup<String> decision = new RadioButtonGroup<>("Decision");
        decision.setId("framework-decision");
        decision.setItems("review", "approve", "hold");
        decision.setValue("review");
        decision.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);

        TextField caseTitle = new TextField("Case title");
        caseTitle.setId("framework-case-title");
        caseTitle.setValue("Initial title");
        caseTitle.setClearButtonVisible(true);
        caseTitle.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        Span textFieldStatus = new Span("TextField: value=Initial title");
        textFieldStatus.setId("framework-text-field-status");
        Button textFieldServerUpdate = new Button("Set TextField", event -> {
            caseTitle.setValue("Server issued title");
            textFieldStatus.setText("TextField server value: Server issued title");
        });
        textFieldServerUpdate.setId("framework-text-field-server-update");

        TextArea notes = new TextArea("Notes");
        notes.setId("framework-notes");
        notes.setValue("Initial notes");
        notes.addThemeVariants(TextAreaVariant.LUMO_SMALL);

        EmailField contactEmail = new EmailField("Contact email");
        contactEmail.setId("framework-contact-email");
        contactEmail.setValue("owner@example.test");
        contactEmail.addThemeVariants(TextFieldVariant.LUMO_SMALL);

        IntegerField score = new IntegerField("Score");
        score.setId("framework-score");
        score.setMin(0);
        score.setMax(10);
        score.setValue(4);
        score.addThemeVariants(TextFieldVariant.LUMO_SMALL);

        NumberField riskScore = new NumberField("Risk score");
        riskScore.setId("framework-risk-score");
        riskScore.setMin(0);
        riskScore.setMax(10);
        riskScore.setStep(0.25);
        riskScore.setValue(2.5);
        riskScore.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        Span numberFieldStatus = new Span("NumberField: value=2.5");
        numberFieldStatus.setId("framework-number-field-status");
        Button numberFieldServerUpdate = new Button("Set NumberField", event -> {
            riskScore.setValue(8.25);
            numberFieldStatus.setText("NumberField server value: 8.25");
        });
        numberFieldServerUpdate.setId("framework-number-field-server-update");

        PasswordField secret = new PasswordField("Secret");
        secret.setId("framework-secret");
        secret.setValue("initial-secret");
        secret.addThemeVariants(TextFieldVariant.LUMO_SMALL);

        TimePicker reviewTime = new TimePicker("Review time");
        reviewTime.setId("framework-review-time");
        reviewTime.setValue(java.time.LocalTime.of(9, 30));
        reviewTime.addThemeVariants(TimePickerVariant.LUMO_SMALL);

        DateTimePicker reviewDateTime = new DateTimePicker("Review date and time");
        reviewDateTime.setId("framework-review-date-time");
        reviewDateTime.setValue(LocalDateTime.of(2026, 6, 15, 9, 30));
        reviewDateTime.addThemeVariants(DateTimePickerVariant.LUMO_SMALL);
        Span dateTimePickerStatus = new Span("DateTimePicker: value=2026-06-15T09:30");
        dateTimePickerStatus.setId("framework-date-time-picker-status");
        Button dateTimePickerServerUpdate = new Button("Set DateTimePicker", event -> {
            reviewDateTime.setValue(LocalDateTime.of(2026, 8, 1, 16, 20));
            dateTimePickerStatus.setText("DateTimePicker server value: 2026-08-01T16:20");
        });
        dateTimePickerServerUpdate.setId("framework-date-time-picker-server-update");

        Span inputMatrixStatus = new Span("Input matrix: pending");
        inputMatrixStatus.setId("framework-input-matrix-status");
        decision.addValueChangeListener(event ->
                inputMatrixStatus.setText("Decision changed: " + event.getValue()));
        caseTitle.addValueChangeListener(event -> {
            inputMatrixStatus.setText("TextField changed: " + event.getValue());
            textFieldStatus.setText("TextField client value: " + event.getValue());
        });
        notes.addValueChangeListener(event ->
                inputMatrixStatus.setText("Notes changed: " + event.getValue()));
        contactEmail.addValueChangeListener(event ->
                inputMatrixStatus.setText("Email changed: " + event.getValue()));
        score.addValueChangeListener(event ->
                inputMatrixStatus.setText("Score changed: " + event.getValue()));
        riskScore.addValueChangeListener(event -> {
            String value = event.getValue() == null ? "null" : event.getValue().toString();
            inputMatrixStatus.setText("Risk changed: " + value);
            numberFieldStatus.setText("NumberField client value: " + value);
        });
        secret.addValueChangeListener(event -> {
            String value = event.getValue() == null ? "" : event.getValue();
            inputMatrixStatus.setText("Secret changed: length=" + value.length());
        });
        reviewTime.addValueChangeListener(event ->
                inputMatrixStatus.setText("Review time changed: " + event.getValue()));
        reviewDateTime.addValueChangeListener(event -> {
            LocalDateTime value = event.getValue();
            String text = value == null ? "null" : value.toString();
            inputMatrixStatus.setText("Review date-time changed: " + text);
            dateTimePickerStatus.setText("DateTimePicker client value: " + text);
        });

        MultiSelectComboBox<String> assignees = new MultiSelectComboBox<>("Assignees");
        assignees.setId("framework-assignees");
        assignees.setItems("ava", "sol", "kai");
        assignees.setValue(Set.of("ava"));
        assignees.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);

        ListBox<String> laneListBox = new ListBox<>();
        laneListBox.setId("framework-list-box");
        laneListBox.setItems(List.of("analysis", "build", "support"));
        laneListBox.setItemLabelGenerator(value -> switch (value) {
            case "analysis" -> "Analysis lane";
            case "build" -> "Build lane";
            case "support" -> "Support lane";
            default -> value;
        });
        laneListBox.setValue("analysis");
        Span listBoxStatus = new Span("ListBox: value=analysis");
        listBoxStatus.setId("framework-list-box-status");
        laneListBox.addValueChangeListener(event ->
                listBoxStatus.setText("ListBox client value: " + event.getValue()));
        Button listBoxServerUpdate = new Button("Set ListBox", event -> {
            laneListBox.setValue("support");
            listBoxStatus.setText("ListBox server value: support");
        });
        listBoxServerUpdate.setId("framework-list-box-server-update");

        MultiSelectListBox<String> reviewStageListBox = new MultiSelectListBox<>();
        reviewStageListBox.setId("framework-multi-select-list-box");
        reviewStageListBox.setItems(List.of("triage", "design", "verify"));
        reviewStageListBox.setItemLabelGenerator(value -> switch (value) {
            case "triage" -> "Triage stage";
            case "design" -> "Design stage";
            case "verify" -> "Verify stage";
            default -> value;
        });
        reviewStageListBox.setValue(Set.of("triage"));
        Span multiSelectListBoxStatus = new Span("MultiSelectListBox: value=triage");
        multiSelectListBoxStatus.setId("framework-multi-select-list-box-status");
        reviewStageListBox.addValueChangeListener(event -> {
            String value = event.getValue().stream()
                    .sorted()
                    .collect(Collectors.joining(","));
            multiSelectListBoxStatus.setText("MultiSelectListBox client value: " + value);
        });
        Button multiSelectListBoxServerUpdate = new Button("Set MultiSelectListBox", event -> {
            reviewStageListBox.setValue(Set.of("design", "verify"));
            multiSelectListBoxStatus.setText("MultiSelectListBox server value: design,verify");
        });
        multiSelectListBoxServerUpdate.setId("framework-multi-select-list-box-server-update");

        ReviewAssignmentField customAssignment = new ReviewAssignmentField();
        Span customFieldStatus = new Span("CustomField: value=draft|queued");
        customFieldStatus.setId("framework-custom-field-status");
        customAssignment.addValueChangeListener(event ->
                customFieldStatus.setText("CustomField client value: " + event.getValue()));
        Button customFieldServerUpdate = new Button("Set CustomField", event -> {
            customAssignment.setValue("deploy|approved");
            customFieldStatus.setText("CustomField server value: deploy|approved");
        });
        customFieldServerUpdate.setId("framework-custom-field-server-update");

        Span splitPrimary = new Span("Split primary pane");
        splitPrimary.setId("framework-split-primary");
        Span splitSecondary = new Span("Split secondary pane");
        splitSecondary.setId("framework-split-secondary");
        SplitLayout splitLayout = new SplitLayout(splitPrimary, splitSecondary);
        splitLayout.setId("framework-split-layout");
        splitLayout.setWidth("360px");
        splitLayout.setHeight("96px");
        splitLayout.setSplitterPosition(45);
        Span splitLayoutStatus = new Span("SplitLayout: position=45.0");
        splitLayoutStatus.setId("framework-split-layout-status");
        splitLayout.addSplitterDragEndListener(event ->
                splitLayoutStatus.setText("SplitLayout drag position: "
                        + splitLayout.getSplitterPosition()));
        Button splitLayoutServerUpdate = new Button("Set SplitLayout", event -> {
            splitLayout.setSplitterPosition(62.5);
            splitLayoutStatus.setText("SplitLayout server position: 62.5");
        });
        splitLayoutServerUpdate.setId("framework-split-layout-server-update");

        Tab overviewTab = new Tab("Overview");
        overviewTab.setId("framework-nav-tab-overview");
        Tab auditTab = new Tab("Audit");
        auditTab.setId("framework-nav-tab-audit");
        Tabs navTabs = new Tabs(overviewTab, auditTab);
        navTabs.setId("framework-nav-tabs");
        navTabs.addThemeVariants(TabsVariant.LUMO_SMALL);

        Details auditDetails = new Details("Audit details", new Span("Disclosure content"));
        auditDetails.setId("framework-details");
        auditDetails.addThemeVariants(DetailsVariant.LUMO_SMALL);

        Accordion queueAccordion = new Accordion();
        queueAccordion.setId("framework-accordion");
        AccordionPanel queuePanel = queueAccordion.add("Queue", new Span("Queue disclosure content"));

        Span navigationMatrixStatus = new Span("Navigation matrix: pending");
        navigationMatrixStatus.setId("framework-navigation-matrix-status");
        Span assigneesStatus = new Span("Assignees: pending");
        assigneesStatus.setId("framework-assignees-status");
        assignees.addValueChangeListener(event -> {
            String value = event.getValue().stream()
                    .sorted()
                    .collect(Collectors.joining(","));
            String statusMessage = "Assignees changed: " + value;
            assigneesStatus.setText(statusMessage);
            navigationMatrixStatus.setText(statusMessage);
        });
        navTabs.addSelectedChangeListener(event -> {
            if (event.getSelectedTab() == auditTab) {
                navigationMatrixStatus.setText("Tab changed: audit");
            } else {
                navigationMatrixStatus.setText("Tab changed: overview");
            }
        });
        auditDetails.addOpenedChangeListener(event ->
                navigationMatrixStatus.setText("Details opened: " + event.isOpened()));
        queueAccordion.addOpenedChangeListener(event -> {
            if (event.getOpenedPanel().filter(panel -> panel == queuePanel).isPresent()) {
                navigationMatrixStatus.setText("Accordion opened: queue");
            } else {
                navigationMatrixStatus.setText("Accordion closed");
            }
        });

        MenuBar menu = new MenuBar();
        menu.setId("framework-menu");
        menu.addThemeVariants(MenuBarVariant.LUMO_SMALL);
        menu.addItem("Audit Action", event -> navigationMatrixStatus.setText("Menu action: audit"))
                .setId("framework-menu-audit");

        Button contextMenuTarget = new Button("Context Target");
        contextMenuTarget.setId("framework-context-menu-target");
        ContextMenu contextMenu = new ContextMenu(contextMenuTarget);
        contextMenu.setId("framework-context-menu");
        contextMenu.setOpenOnClick(true);

        Span overlayMatrixStatus = new Span("Overlay matrix: pending");
        overlayMatrixStatus.setId("framework-overlay-matrix-status");
        contextMenu.addOpenedChangeListener(event -> {
            if (event.isOpened()) {
                overlayMatrixStatus.setText("Context menu opened: true");
            }
        });
        MenuItem contextMenuAudit = contextMenu.addItem("Context Audit",
                event -> overlayMatrixStatus.setText("Context menu action: audit"));
        contextMenuAudit.setId("framework-context-menu-audit");

        Button popoverTarget = new Button("Popover Target");
        popoverTarget.setId("framework-popover-target");
        Popover popover = new Popover(new Span("Popover audit content"));
        popover.setId("framework-popover");
        popover.setTarget(popoverTarget);
        popover.setOpenOnClick(true);
        popover.addOpenedChangeListener(event -> {
            if (event.isOpened()) {
                overlayMatrixStatus.setText("Popover opened: true");
            }
        });

        Span notificationStatus = new Span("Notification: pending");
        notificationStatus.setId("framework-notification-status");
        Button notificationOpen = new Button("Show Notification", event -> {
            Notification notification = new Notification(
                    "Flow IPC notification opened: audit", 0, Notification.Position.BOTTOM_START);
            notification.setId("framework-notification");
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            notification.setAssertive(true);
            notification.open();
            overlayMatrixStatus.setText("Notification opened: true");
            notificationStatus.setText("Notification opened: Flow IPC notification opened: audit");
        });
        notificationOpen.setId("framework-notification-open");

        Span confirmDialogStatus = new Span("ConfirmDialog: pending");
        confirmDialogStatus.setId("framework-confirm-dialog-status");
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setId("framework-confirm-dialog");
        confirmDialog.setHeader("Flow IPC confirm");
        confirmDialog.setText("Confirm audit action through Flow IPC");
        confirmDialog.setConfirmText("Confirm audit");
        confirmDialog.setCancelText("Cancel audit");
        confirmDialog.setRejectText("Reject audit");
        confirmDialog.setCancelable(true);
        confirmDialog.setRejectable(true);
        confirmDialog.addOpenedChangeListener(event -> {
            if (event.isOpened()) {
                overlayMatrixStatus.setText("ConfirmDialog opened: true");
                confirmDialogStatus.setText("ConfirmDialog opened: requested");
            }
        });
        confirmDialog.addConfirmListener(event -> {
            overlayMatrixStatus.setText("ConfirmDialog confirmed: true");
            confirmDialogStatus.setText("ConfirmDialog confirmed: audit");
        });
        confirmDialog.addCancelListener(event -> {
            overlayMatrixStatus.setText("ConfirmDialog canceled: true");
            confirmDialogStatus.setText("ConfirmDialog canceled: audit");
        });
        confirmDialog.addRejectListener(event -> {
            overlayMatrixStatus.setText("ConfirmDialog rejected: true");
            confirmDialogStatus.setText("ConfirmDialog rejected: audit");
        });
        Button confirmDialogOpen = new Button("Open Confirm Dialog", event -> {
            confirmDialogStatus.setText("ConfirmDialog open requested");
            confirmDialog.open();
        });
        confirmDialogOpen.setId("framework-confirm-dialog-open");

        ProgressBar progressBar = new ProgressBar(0, 1, 0.25);
        progressBar.setId("framework-progress-bar");
        Span progressBarStatus = new Span("ProgressBar: value=0.25 indeterminate=false");
        progressBarStatus.setId("framework-progress-bar-status");
        Button progressValueUpdate = new Button("Set Progress Value", event -> {
            progressBar.setIndeterminate(false);
            progressBar.setValue(0.75);
            progressBarStatus.setText("ProgressBar value: 0.75 indeterminate=false");
        });
        progressValueUpdate.setId("framework-progress-value-update");
        Button progressIndeterminateUpdate = new Button("Set Progress Indeterminate", event -> {
            progressBar.setIndeterminate(true);
            progressBarStatus.setText("ProgressBar indeterminate: true value=0.75");
        });
        progressIndeterminateUpdate.setId("framework-progress-indeterminate-update");

        Span richContentStatus = new Span("Rich content: pending");
        richContentStatus.setId("framework-rich-content-status");

        Avatar avatar = new Avatar("Ava Desktop");
        avatar.setId("framework-avatar");
        avatar.addThemeVariants(AvatarVariant.LUMO_SMALL);
        avatar.setAbbreviation("AD");
        avatar.setColorIndex(4);
        avatar.setTooltipEnabled(true);
        avatar.getElement().addEventListener("click", event ->
                richContentStatus.setText("Avatar clicked: ava"));

        AvatarGroup avatarGroup = new AvatarGroup(
                avatarGroupItem("Ava Desktop", "AD", 4),
                avatarGroupItem("Kai IPC", "KI", 5));
        avatarGroup.setId("framework-avatar-group");
        avatarGroup.addThemeVariants(AvatarGroupVariant.LUMO_SMALL);
        avatarGroup.setMaxItemsVisible(2);

        MessageList messageList = new MessageList();
        messageList.setId("framework-message-list");
        messageList.setMarkdown(true);
        messageList.setAnnounceMessages(true);
        messageList.setItems(List.of(new MessageListItem(
                "Flow **IPC** message seed",
                Instant.parse("2026-06-10T00:00:00Z"),
                "Ava Desktop",
                "AD")));
        MessageInput messageInput = new MessageInput(event -> {
            String value = event.getValue() == null ? "" : event.getValue();
            messageList.addItem(new MessageListItem(
                    "Echo: " + value,
                    Instant.parse("2026-06-10T00:01:00Z"),
                    "Flow IPC",
                    "FI"));
            richContentStatus.setText("Message submitted: " + value);
        });
        messageInput.setId("framework-message-input");
        messageInput.addThemeVariants(MessageInputVariant.AURA_ICON_BUTTON);

        SideNav sideNav = new SideNav("Framework navigation");
        sideNav.setId("framework-side-nav");
        sideNav.addThemeVariants(SideNavVariant.AURA_FILLED);
        sideNav.setCollapsible(true);
        sideNav.setExpanded(true);
        SideNavItem sideNavAudit = new SideNavItem("Audit queue", "#audit-queue");
        sideNavAudit.setId("framework-side-nav-audit");
        sideNavAudit.getElement().addEventListener("click", event ->
                richContentStatus.setText("Side nav selected: audit"));
        sideNav.addItem(sideNavAudit);

        LoginI18n loginI18n = LoginI18n.createDefault();
        loginI18n.getForm().setTitle("Flow IPC access");
        loginI18n.getForm().setUsername("User");
        loginI18n.getForm().setPassword("Password");
        loginI18n.getForm().setSubmit("Sign in");
        loginI18n.getForm().setForgotPassword("Recover Flow IPC access");
        loginI18n.getErrorMessage().setTitle("Flow IPC login failed");
        loginI18n.getErrorMessage().setMessage("Use the framework validation credentials.");
        LoginOverlay loginOverlay = new LoginOverlay(loginI18n);
        loginOverlay.setId("framework-login-overlay");
        loginOverlay.setTitle("Flow IPC Login");
        loginOverlay.setDescription("Framework validation");
        loginOverlay.setForgotPasswordButtonVisible(true);
        loginOverlay.setError(true);
        loginOverlay.getElement().addEventListener("opened-changed", event -> {
            JsonNode openedNode = event.getEventData().get("element.opened");
            boolean opened = openedNode != null && Boolean.parseBoolean(openedNode.asString());
            loginOverlay.setOpened(opened);
            richContentStatus.setText("Login overlay opened: " + opened);
        }).addEventData("element.opened");
        loginOverlay.addLoginListener(event -> {
            String password = event.getPassword() == null ? "" : event.getPassword();
            if ("invalid".equals(event.getUsername())) {
                richContentStatus.setText("Login rejected: " + event.getUsername()
                        + " password-length=" + password.length());
                loginOverlay.setError(true);
                loginOverlay.setOpened(true);
            } else if ("overlay-show-error".equals(event.getUsername())) {
                loginOverlay.showErrorMessage("Desktop overlay session expired",
                        "Re-authenticate in the overlay before continuing.");
                richContentStatus.setText("Login overlay show error rejected: " + event.getUsername()
                        + " password-length=" + password.length()
                        + " title=Desktop overlay session expired");
                loginOverlay.setOpened(true);
            } else {
                richContentStatus.setText("Login submitted: " + event.getUsername()
                        + " password-length=" + password.length());
                loginOverlay.setError(false);
                loginOverlay.close();
            }
        });
        loginOverlay.addForgotPasswordListener(event ->
                richContentStatus.setText("Login forgot password requested"));
        Button loginOverlayReopen = new Button("Open Login Overlay", event -> {
            loginOverlay.setOpened(true);
            richContentStatus.setText("Login overlay reopened: true");
        });
        loginOverlayReopen.setId("framework-login-overlay-reopen");
        LoginForm loginForm = new LoginForm();
        loginForm.setId("framework-login-form");
        loginForm.setI18n(loginI18n);
        loginForm.setForgotPasswordButtonVisible(true);
        final String loginFormActionPostPath = "flow-ipc-login-form-action";
        loginForm.setEnabled(true);
        loginForm.setError(false);
        Span loginFormStatus = new Span("LoginForm idle");
        loginFormStatus.setId("framework-login-form-status");
        loginForm.addLoginListener(event -> {
            String password = event.getPassword() == null ? "" : event.getPassword();
            if ("inline-invalid".equals(event.getUsername())) {
                loginForm.setEnabled(true);
                loginForm.setError(true);
                loginFormStatus.setText("LoginForm rejected: " + event.getUsername()
                        + " password-length=" + password.length());
                return;
            }
            if ("inline-recovered".equals(event.getUsername())) {
                loginForm.setEnabled(true);
                loginForm.setError(false);
                loginFormStatus.setText("LoginForm recovered: " + event.getUsername()
                        + " password-length=" + password.length());
                return;
            }
            loginForm.setEnabled(true);
            loginForm.setError(false);
            loginFormStatus.setText("LoginForm submitted: " + event.getUsername()
                    + " password-length=" + password.length());
        });
        loginForm.addForgotPasswordListener(event -> {
            loginForm.setEnabled(true);
            loginForm.setError(false);
            loginFormStatus.setText("LoginForm forgot password requested");
        });
        LoginForm loginFormActionPost = new LoginForm();
        loginFormActionPost.setId("framework-login-form-action-post");
        loginFormActionPost.setI18n(loginI18n);
        loginFormActionPost.setForgotPasswordButtonVisible(true);
        loginFormActionPost.getElement().setAttribute("action", loginFormActionPostPath);
        loginFormActionPost.setEnabled(true);
        loginFormActionPost.setError(false);
        Span loginFormActionPostStatus = new Span("LoginForm action post idle");
        loginFormActionPostStatus.setId("framework-login-form-action-post-status");
        loginFormActionPost.addLoginListener(event -> {
            String password = event.getPassword() == null ? "" : event.getPassword();
            loginFormActionPost.setEnabled(true);
            loginFormActionPost.setError(false);
            loginFormActionPostStatus.setText("LoginForm action post submitted: "
                    + event.getUsername() + " password-length="
                    + password.length() + " action="
                    + loginFormActionPostPath);
        });
        final String loginOverlayActionPostPath = "flow-ipc-login-overlay-action";
        LoginOverlay loginOverlayActionPost = new LoginOverlay(loginI18n);
        loginOverlayActionPost.setId("framework-login-overlay-action-post");
        loginOverlayActionPost.setTitle("Flow IPC LoginOverlay action post");
        loginOverlayActionPost.setDescription("Framework validation");
        loginOverlayActionPost.setForgotPasswordButtonVisible(true);
        loginOverlayActionPost.getElement().setAttribute("action", loginOverlayActionPostPath);
        loginOverlayActionPost.setEnabled(true);
        loginOverlayActionPost.setError(false);
        Span loginOverlayActionPostStatus = new Span("LoginOverlay action post idle");
        loginOverlayActionPostStatus.setId("framework-login-overlay-action-post-status");
        Button loginOverlayActionPostOpen = new Button("Open LoginOverlay Action Post", event -> {
            loginOverlayActionPost.setOpened(true);
            loginOverlayActionPostStatus.setText("LoginOverlay action post opened: true");
        });
        loginOverlayActionPostOpen.setId("framework-login-overlay-action-post-open");
        loginOverlayActionPost.addLoginListener(event -> {
            String password = event.getPassword() == null ? "" : event.getPassword();
            loginOverlayActionPost.setEnabled(true);
            loginOverlayActionPost.setError(false);
            loginOverlayActionPostStatus.setText("LoginOverlay action post submitted: "
                    + event.getUsername() + " password-length="
                    + password.length() + " action="
                    + loginOverlayActionPostPath);
            loginOverlayActionPost.close();
        });
        LoginOverlay loginOverlaySubmitButton = new LoginOverlay(loginI18n);
        loginOverlaySubmitButton.setId("framework-login-overlay-submit-button");
        loginOverlaySubmitButton.setTitle("Flow IPC LoginOverlay submit button");
        loginOverlaySubmitButton.setDescription("Framework validation");
        loginOverlaySubmitButton.setForgotPasswordButtonVisible(true);
        loginOverlaySubmitButton.setEnabled(true);
        loginOverlaySubmitButton.setError(false);
        Span loginOverlaySubmitButtonStatus = new Span("LoginOverlay submit button idle");
        loginOverlaySubmitButtonStatus.setId("framework-login-overlay-submit-button-status");
        loginOverlaySubmitButtonStatus.getElement().addEventListener(
                "vatori-login-overlay-submit-button-open", event -> {
                    loginOverlaySubmitButton.setOpened(true);
                    loginOverlaySubmitButtonStatus.setText("LoginOverlay submit button opened: true");
                });
        loginOverlaySubmitButton.addLoginListener(event -> {
            String password = event.getPassword() == null ? "" : event.getPassword();
            loginOverlaySubmitButton.setEnabled(true);
            loginOverlaySubmitButton.setError(false);
            loginOverlaySubmitButtonStatus.setText("LoginOverlay submit button submitted: "
                    + event.getUsername() + " password-length=" + password.length());
            loginOverlaySubmitButton.close();
        });
        LoginOverlay loginOverlayEnter = new LoginOverlay(loginI18n);
        loginOverlayEnter.setId("framework-login-overlay-enter");
        loginOverlayEnter.setTitle("Flow IPC LoginOverlay Enter submit");
        loginOverlayEnter.setDescription("Framework validation");
        loginOverlayEnter.setForgotPasswordButtonVisible(true);
        loginOverlayEnter.setEnabled(true);
        loginOverlayEnter.setError(false);
        Span loginOverlayEnterStatus = new Span("LoginOverlay enter idle");
        loginOverlayEnterStatus.setId("framework-login-overlay-enter-status");
        loginOverlayEnterStatus.getElement().addEventListener(
                "vatori-login-overlay-enter-open", event -> {
                    loginOverlayEnter.setOpened(true);
                    loginOverlayEnterStatus.setText("LoginOverlay enter opened: true");
                });
        loginOverlayEnter.addLoginListener(event -> {
            String password = event.getPassword() == null ? "" : event.getPassword();
            loginOverlayEnter.setEnabled(true);
            loginOverlayEnter.setError(false);
            loginOverlayEnterStatus.setText("LoginOverlay enter submitted: "
                    + event.getUsername() + " password-length=" + password.length());
        });
        LoginForm loginFormShowError = new LoginForm();
        loginFormShowError.setId("framework-login-form-show-error");
        loginFormShowError.setI18n(loginI18n);
        loginFormShowError.setForgotPasswordButtonVisible(true);
        loginFormShowError.setEnabled(true);
        loginFormShowError.setError(false);
        Span loginFormShowErrorStatus = new Span("LoginForm show error idle");
        loginFormShowErrorStatus.setId("framework-login-form-show-error-status");
        loginFormShowError.addLoginListener(event -> {
            String password = event.getPassword() == null ? "" : event.getPassword();
            loginFormShowError.setEnabled(true);
            loginFormShowError.showErrorMessage("Desktop session expired",
                    "Re-authenticate through Flow IPC before continuing.");
            loginFormShowErrorStatus.setText("LoginForm show error rejected: "
                    + event.getUsername() + " password-length="
                    + password.length() + " title=Desktop session expired");
        });
        LoginForm loginFormButton = new LoginForm();
        loginFormButton.setId("framework-login-form-button");
        loginFormButton.setI18n(loginI18n);
        loginFormButton.setForgotPasswordButtonVisible(true);
        loginFormButton.setEnabled(true);
        loginFormButton.setError(false);
        Span loginFormButtonStatus = new Span("LoginForm button idle");
        loginFormButtonStatus.setId("framework-login-form-button-status");
        loginFormButton.addLoginListener(event -> {
            String password = event.getPassword() == null ? "" : event.getPassword();
            loginFormButton.setEnabled(true);
            loginFormButton.setError(false);
            loginFormButtonStatus.setText("LoginForm button submitted: "
                    + event.getUsername() + " password-length=" + password.length());
        });
        LoginForm loginFormEnter = new LoginForm();
        loginFormEnter.setId("framework-login-form-enter");
        loginFormEnter.setI18n(loginI18n);
        loginFormEnter.setForgotPasswordButtonVisible(true);
        loginFormEnter.setEnabled(true);
        loginFormEnter.setError(false);
        Span loginFormEnterStatus = new Span("LoginForm enter idle");
        loginFormEnterStatus.setId("framework-login-form-enter-status");
        loginFormEnter.addLoginListener(event -> {
            String password = event.getPassword() == null ? "" : event.getPassword();
            loginFormEnter.setEnabled(true);
            loginFormEnter.setError(false);
            loginFormEnterStatus.setText("LoginForm enter submitted: "
                    + event.getUsername() + " password-length=" + password.length());
        });
        LoginForm loginFormEnabled = new LoginForm();
        loginFormEnabled.setId("framework-login-form-enabled");
        loginFormEnabled.setI18n(loginI18n);
        loginFormEnabled.setForgotPasswordButtonVisible(true);
        loginFormEnabled.setEnabled(false);
        loginFormEnabled.setError(false);
        Span loginFormEnabledStatus = new Span("LoginForm enabled lifecycle: disabled");
        loginFormEnabledStatus.setId("framework-login-form-enabled-status");
        AtomicInteger loginFormEnabledClickCount = new AtomicInteger();
        Button loginFormEnable = new Button("Enable LoginForm", event -> {
            loginFormEnabled.setEnabled(true);
            loginFormEnabled.setError(false);
            loginFormEnabledStatus.setText("LoginForm enabled lifecycle: enabled click="
                    + loginFormEnabledClickCount.incrementAndGet());
        });
        loginFormEnable.setId("framework-login-form-enable");
        AtomicInteger loginFormEnabledSubmitCount = new AtomicInteger();
        loginFormEnabled.addLoginListener(event -> {
            String password = event.getPassword() == null ? "" : event.getPassword();
            loginFormEnabled.setEnabled(true);
            loginFormEnabled.setError(false);
            loginFormEnabledStatus.setText("LoginForm enabled lifecycle submitted: "
                    + event.getUsername() + " password-length=" + password.length()
                    + " submit=" + loginFormEnabledSubmitCount.incrementAndGet());
        });
        LoginI18n fullLoginI18n = LoginI18n.createDefault();
        LoginI18n.Header fullLoginI18nHeader = new LoginI18n.Header();
        fullLoginI18nHeader.setTitle("Flow IPC secure workspace");
        fullLoginI18nHeader.setDescription("Use your desktop validation identity.");
        fullLoginI18n.setHeader(fullLoginI18nHeader);
        fullLoginI18n.getForm().setTitle("Desktop credentials");
        fullLoginI18n.getForm().setUsername("Workspace user");
        fullLoginI18n.getForm().setPassword("Workspace password");
        fullLoginI18n.getForm().setSubmit("Unlock workspace");
        fullLoginI18n.getForm().setForgotPassword("Recover workspace access");
        fullLoginI18n.getErrorMessage().setTitle("Workspace login blocked");
        fullLoginI18n.getErrorMessage().setMessage("Check the validation account and retry.");
        fullLoginI18n.getErrorMessage().setUsername("Username is required");
        fullLoginI18n.getErrorMessage().setPassword("Password is required");
        fullLoginI18n.setAdditionalInformation(
                "Desktop validation keeps authentication local to this session.");
        LoginOverlay loginOverlayFullI18n = new LoginOverlay(fullLoginI18n);
        loginOverlayFullI18n.setId("framework-login-overlay-i18n-full");
        loginOverlayFullI18n.setForgotPasswordButtonVisible(true);
        loginOverlayFullI18n.setError(true);
        Span loginOverlayFullI18nStatus = new Span("LoginOverlay full i18n idle");
        loginOverlayFullI18nStatus.setId("framework-login-overlay-i18n-full-status");
        loginOverlayFullI18nStatus.getElement().addEventListener(
                "vatori-login-overlay-i18n-full-open", event -> {
                    loginOverlayFullI18n.setOpened(true);
                    loginOverlayFullI18nStatus.setText("LoginOverlay full i18n opened: true");
                });
        LoginOverlay loginOverlayNoAutofocus = new LoginOverlay(loginI18n);
        loginOverlayNoAutofocus.setId("framework-login-overlay-no-autofocus");
        loginOverlayNoAutofocus.setTitle("Flow IPC Login no autofocus");
        loginOverlayNoAutofocus.setDescription("Framework validation");
        loginOverlayNoAutofocus.setForgotPasswordButtonVisible(true);
        loginOverlayNoAutofocus.setError(false);
        loginOverlayNoAutofocus.getElement().setProperty("noAutofocus", true);
        loginOverlayNoAutofocus.getElement().setAttribute("no-autofocus", true);
        loginOverlayNoAutofocus.addAttachListener(event -> event.getSource().getElement().executeJs(
                "this.noAutofocus = true; this.setAttribute('no-autofocus', '');"));
        Span loginOverlayNoAutofocusStatus = new Span("LoginOverlay no autofocus idle");
        loginOverlayNoAutofocusStatus.setId("framework-login-overlay-no-autofocus-status");
        loginOverlayNoAutofocus.getElement().addEventListener("vatori-login-overlay-no-autofocus-open", event -> {
            loginOverlayNoAutofocus.setOpened(true);
            loginOverlayNoAutofocusStatus.setText("LoginOverlay no autofocus opened: true");
        });
        loginOverlayNoAutofocus.getElement().addEventListener("vatori-login-overlay-no-autofocus-close", event -> {
            loginOverlayNoAutofocus.setOpened(false);
            loginOverlayNoAutofocusStatus.setText("LoginOverlay no autofocus closed: true");
        });
        VerticalLayout loginOverlayAutofocusControlHost = new VerticalLayout();
        loginOverlayAutofocusControlHost.setId("framework-login-overlay-autofocus-control-host");
        loginOverlayAutofocusControlHost.setPadding(false);
        loginOverlayAutofocusControlHost.setSpacing(false);
        Span loginOverlayAutofocusControlStatus = new Span("LoginOverlay autofocus control idle");
        loginOverlayAutofocusControlStatus.setId("framework-login-overlay-autofocus-control-status");
        loginOverlayAutofocusControlStatus.getElement().addEventListener(
                "vatori-login-overlay-autofocus-control-open", event -> {
                    LoginOverlay loginOverlayAutofocusControl = new LoginOverlay(loginI18n);
                    loginOverlayAutofocusControl.setId("framework-login-overlay-autofocus-control");
                    loginOverlayAutofocusControl.setTitle("Flow IPC Login autofocus control");
                    loginOverlayAutofocusControl.setDescription("Framework validation");
                    loginOverlayAutofocusControl.setForgotPasswordButtonVisible(true);
                    loginOverlayAutofocusControl.setError(false);
                    loginOverlayAutofocusControl.getElement().addEventListener(
                            "vatori-login-overlay-autofocus-control-close", closeEvent -> {
                                loginOverlayAutofocusControl.setOpened(false);
                                loginOverlayAutofocusControlStatus
                                        .setText("LoginOverlay autofocus control closed: true");
                            });
                    loginOverlayAutofocusControl.setOpened(true);
                    loginOverlayAutofocusControlHost.removeAll();
                    loginOverlayAutofocusControlHost.add(loginOverlayAutofocusControl);
                    loginOverlayAutofocusControlStatus.setText("LoginOverlay autofocus control opened: true");
                });
        LoginOverlay loginOverlayOutsideClose = new LoginOverlay(loginI18n);
        loginOverlayOutsideClose.setId("framework-login-overlay-outside-close");
        loginOverlayOutsideClose.setTitle("Flow IPC LoginOverlay outside close");
        loginOverlayOutsideClose.setDescription("Framework validation");
        loginOverlayOutsideClose.setForgotPasswordButtonVisible(true);
        loginOverlayOutsideClose.setError(false);
        Span loginOverlayOutsideCloseStatus = new Span("LoginOverlay outside close idle");
        loginOverlayOutsideCloseStatus.setId("framework-login-overlay-outside-close-status");
        loginOverlayOutsideClose.getElement().addEventListener(
                "vatori-login-overlay-outside-open", event -> {
                    loginOverlayOutsideClose.setOpened(true);
                    loginOverlayOutsideCloseStatus.setText("LoginOverlay outside close opened: true");
                });
        loginOverlayOutsideClose.getElement().addEventListener("opened-changed", event -> {
            JsonNode openedNode = event.getEventData().get("element.opened");
            boolean opened = openedNode != null && Boolean.parseBoolean(openedNode.asString());
            loginOverlayOutsideClose.setOpened(opened);
            loginOverlayOutsideCloseStatus.setText("LoginOverlay outside close opened: " + opened);
        }).addEventData("element.opened");
        LoginOverlay loginOverlayEscapeKey = new LoginOverlay(loginI18n);
        loginOverlayEscapeKey.setId("framework-login-overlay-escape-key");
        loginOverlayEscapeKey.setTitle("Flow IPC LoginOverlay escape key");
        loginOverlayEscapeKey.setDescription("Framework validation");
        loginOverlayEscapeKey.setForgotPasswordButtonVisible(true);
        loginOverlayEscapeKey.setError(false);
        Span loginOverlayEscapeKeyStatus = new Span("LoginOverlay escape key idle");
        loginOverlayEscapeKeyStatus.setId("framework-login-overlay-escape-key-status");
        loginOverlayEscapeKey.getElement().addEventListener(
                "vatori-login-overlay-escape-open", event -> {
                    loginOverlayEscapeKey.setOpened(true);
                    loginOverlayEscapeKeyStatus.setText("LoginOverlay escape key opened: true");
                });
        loginOverlayEscapeKey.getElement().addEventListener("opened-changed", event -> {
            JsonNode openedNode = event.getEventData().get("element.opened");
            boolean opened = openedNode != null && Boolean.parseBoolean(openedNode.asString());
            loginOverlayEscapeKey.setOpened(opened);
            loginOverlayEscapeKeyStatus.setText("LoginOverlay escape key opened: " + opened);
        }).addEventData("element.opened");
        LoginOverlay loginOverlayTabTraversal = new LoginOverlay(loginI18n);
        loginOverlayTabTraversal.setId("framework-login-overlay-tab-traversal");
        loginOverlayTabTraversal.setTitle("Flow IPC LoginOverlay tab traversal");
        loginOverlayTabTraversal.setDescription("Framework validation");
        loginOverlayTabTraversal.setForgotPasswordButtonVisible(true);
        loginOverlayTabTraversal.setError(false);
        Span loginOverlayTabTraversalStatus = new Span("LoginOverlay tab traversal idle");
        loginOverlayTabTraversalStatus.setId("framework-login-overlay-tab-traversal-status");
        loginOverlayTabTraversal.getElement().addEventListener(
                "vatori-login-overlay-tab-open", event -> {
                    loginOverlayTabTraversal.setOpened(true);
                    loginOverlayTabTraversalStatus.setText("LoginOverlay tab traversal opened: true");
                });
        LoginOverlay loginOverlayShiftTabTraversal = new LoginOverlay(loginI18n);
        loginOverlayShiftTabTraversal.setId("framework-login-overlay-shift-tab-traversal");
        loginOverlayShiftTabTraversal.setTitle("Flow IPC LoginOverlay shift tab traversal");
        loginOverlayShiftTabTraversal.setDescription("Framework validation");
        loginOverlayShiftTabTraversal.setForgotPasswordButtonVisible(true);
        loginOverlayShiftTabTraversal.setError(false);
        Span loginOverlayShiftTabTraversalStatus = new Span("LoginOverlay shift tab traversal idle");
        loginOverlayShiftTabTraversalStatus.setId("framework-login-overlay-shift-tab-traversal-status");
        loginOverlayShiftTabTraversal.getElement().addEventListener(
                "vatori-login-overlay-shift-tab-open", event -> {
                    loginOverlayShiftTabTraversal.setOpened(true);
                    loginOverlayShiftTabTraversalStatus.setText("LoginOverlay shift tab traversal opened: true");
                });
        LoginOverlay loginOverlayAutofillAttributes = new LoginOverlay(loginI18n);
        loginOverlayAutofillAttributes.setId("framework-login-overlay-autofill-attributes");
        loginOverlayAutofillAttributes.setTitle("Flow IPC LoginOverlay autofill attributes");
        loginOverlayAutofillAttributes.setDescription("Framework validation");
        loginOverlayAutofillAttributes.setForgotPasswordButtonVisible(true);
        loginOverlayAutofillAttributes.setError(false);
        Span loginOverlayAutofillAttributesStatus = new Span("LoginOverlay autofill attributes idle");
        loginOverlayAutofillAttributesStatus.setId("framework-login-overlay-autofill-attributes-status");
        loginOverlayAutofillAttributes.getElement().addEventListener(
                "vatori-login-overlay-autofill-attributes-open", event -> {
                    loginOverlayAutofillAttributes.setOpened(true);
                    loginOverlayAutofillAttributesStatus.setText(
                            "LoginOverlay autofill attributes opened: true");
                });
        loginOverlayAutofillAttributes.getElement().addEventListener(
                "vatori-login-overlay-autofill-attributes-close", event -> {
                    loginOverlayAutofillAttributes.setOpened(false);
                    loginOverlayAutofillAttributesStatus.setText(
                            "LoginOverlay autofill attributes closed: true");
                });
        LoginOverlay loginOverlayA11ySemantics = new LoginOverlay(loginI18n);
        loginOverlayA11ySemantics.setId("framework-login-overlay-a11y-semantics");
        loginOverlayA11ySemantics.setTitle("Flow IPC LoginOverlay a11y semantics");
        loginOverlayA11ySemantics.setDescription("Framework validation");
        loginOverlayA11ySemantics.setForgotPasswordButtonVisible(true);
        loginOverlayA11ySemantics.setError(false);
        Span loginOverlayA11ySemanticsStatus = new Span("LoginOverlay a11y semantics idle");
        loginOverlayA11ySemanticsStatus.setId("framework-login-overlay-a11y-semantics-status");
        loginOverlayA11ySemantics.getElement().addEventListener(
                "vatori-login-overlay-a11y-semantics-open", event -> {
                    loginOverlayA11ySemantics.setOpened(true);
                    loginOverlayA11ySemanticsStatus.setText(
                            "LoginOverlay a11y semantics opened: true");
                });
        loginOverlayA11ySemantics.getElement().addEventListener(
                "vatori-login-overlay-a11y-semantics-close", event -> {
                    loginOverlayA11ySemantics.setOpened(false);
                    loginOverlayA11ySemanticsStatus.setText(
                            "LoginOverlay a11y semantics closed: true");
                });
        LoginOverlay loginOverlayDisabledGuard = new LoginOverlay(loginI18n);
        loginOverlayDisabledGuard.setId("framework-login-overlay-disabled-guard");
        loginOverlayDisabledGuard.setTitle("Flow IPC Login disabled guard");
        loginOverlayDisabledGuard.setDescription("Framework validation");
        loginOverlayDisabledGuard.setForgotPasswordButtonVisible(true);
        loginOverlayDisabledGuard.setError(false);
        loginOverlayDisabledGuard.setEnabled(false);
        Span loginOverlayDisabledGuardStatus = new Span("LoginOverlay disabled guard idle");
        loginOverlayDisabledGuardStatus.setId("framework-login-overlay-disabled-guard-status");
        loginOverlayDisabledGuardStatus.getElement().addEventListener(
                "vatori-login-overlay-disabled-guard-open", event -> {
                    loginOverlayDisabledGuard.setOpened(true);
                    loginOverlayDisabledGuardStatus.setText("LoginOverlay disabled guard opened: true");
                });
        loginOverlayDisabledGuardStatus.getElement().addEventListener(
                "vatori-login-overlay-disabled-guard-close", event -> {
                    loginOverlayDisabledGuard.setOpened(false);
                    loginOverlayDisabledGuardStatus.setText("LoginOverlay disabled guard closed: true");
                });
        loginOverlayDisabledGuard.addLoginListener(event -> {
            String password = event.getPassword() == null ? "" : event.getPassword();
            loginOverlayDisabledGuardStatus.setText("LoginOverlay disabled guard unexpected login: "
                    + event.getUsername() + " password-length=" + password.length());
        });
        LoginOverlay loginOverlayDisabledKeyboardGuard = new LoginOverlay(loginI18n);
        loginOverlayDisabledKeyboardGuard.setId("framework-login-overlay-disabled-keyboard-guard");
        loginOverlayDisabledKeyboardGuard.setTitle("Flow IPC Login disabled keyboard guard");
        loginOverlayDisabledKeyboardGuard.setDescription("Framework validation");
        loginOverlayDisabledKeyboardGuard.setForgotPasswordButtonVisible(true);
        loginOverlayDisabledKeyboardGuard.setError(false);
        loginOverlayDisabledKeyboardGuard.setEnabled(false);
        Span loginOverlayDisabledKeyboardGuardStatus = new Span(
                "LoginOverlay disabled keyboard guard idle");
        loginOverlayDisabledKeyboardGuardStatus.setId(
                "framework-login-overlay-disabled-keyboard-guard-status");
        loginOverlayDisabledKeyboardGuardStatus.getElement().addEventListener(
                "vatori-login-overlay-disabled-keyboard-guard-open", event -> {
                    loginOverlayDisabledKeyboardGuard.setOpened(true);
                    loginOverlayDisabledKeyboardGuardStatus.setText(
                            "LoginOverlay disabled keyboard guard opened: true");
                });
        loginOverlayDisabledKeyboardGuard.addLoginListener(event -> {
            String password = event.getPassword() == null ? "" : event.getPassword();
            loginOverlayDisabledKeyboardGuardStatus.setText(
                    "LoginOverlay disabled keyboard guard unexpected login: "
                            + event.getUsername() + " password-length=" + password.length());
        });
        LoginForm loginFormFullI18n = new LoginForm();
        loginFormFullI18n.setId("framework-login-form-i18n-full");
        loginFormFullI18n.setI18n(fullLoginI18n);
        loginFormFullI18n.setForgotPasswordButtonVisible(true);
        loginFormFullI18n.setEnabled(true);
        loginFormFullI18n.setError(true);
        Span loginFormFullI18nStatus = new Span("LoginForm full i18n idle");
        loginFormFullI18nStatus.setId("framework-login-form-i18n-full-status");
        Span loginFormNoAutofocusStatus = new Span("LoginForm no autofocus idle");
        loginFormNoAutofocusStatus.setId("framework-login-form-no-autofocus-status");
        VerticalLayout loginFormNoAutofocusHost = new VerticalLayout();
        loginFormNoAutofocusHost.setId("framework-login-form-no-autofocus-host");
        loginFormNoAutofocusHost.setPadding(false);
        loginFormNoAutofocusHost.setSpacing(false);
        AtomicInteger loginFormNoAutofocusRenderCount = new AtomicInteger();
        loginFormNoAutofocusStatus.getElement().addEventListener(
                "vatori-login-form-no-autofocus-render", event -> {
                    LoginForm loginFormNoAutofocus = new LoginForm();
                    loginFormNoAutofocus.setId("framework-login-form-no-autofocus");
                    loginFormNoAutofocus.setI18n(loginI18n);
                    loginFormNoAutofocus.setForgotPasswordButtonVisible(true);
                    loginFormNoAutofocus.setEnabled(true);
                    loginFormNoAutofocus.setError(false);
                    loginFormNoAutofocus.getElement().setProperty("noAutofocus", true);
                    loginFormNoAutofocus.getElement().setAttribute("no-autofocus", true);
                    loginFormNoAutofocus.addAttachListener(attachEvent ->
                            attachEvent.getSource().getElement().executeJs(
                                    "this.noAutofocus = true; this.setAttribute('no-autofocus', '');"));
                    loginFormNoAutofocusHost.removeAll();
                    loginFormNoAutofocusHost.add(loginFormNoAutofocus);
                    loginFormNoAutofocusStatus.setText("LoginForm no autofocus rendered: "
                            + loginFormNoAutofocusRenderCount.incrementAndGet());
                });
        LoginForm loginFormAutofocusControl = new LoginForm();
        loginFormAutofocusControl.setId("framework-login-form-autofocus-control");
        loginFormAutofocusControl.setI18n(loginI18n);
        loginFormAutofocusControl.setForgotPasswordButtonVisible(true);
        loginFormAutofocusControl.setEnabled(true);
        loginFormAutofocusControl.setError(false);
        Span loginFormAutofocusControlStatus = new Span("LoginForm autofocus control idle");
        loginFormAutofocusControlStatus.setId("framework-login-form-autofocus-control-status");
        LoginForm loginFormTabTraversal = new LoginForm();
        loginFormTabTraversal.setId("framework-login-form-tab-traversal");
        loginFormTabTraversal.setI18n(loginI18n);
        loginFormTabTraversal.setForgotPasswordButtonVisible(true);
        loginFormTabTraversal.setEnabled(true);
        loginFormTabTraversal.setError(false);
        Span loginFormTabTraversalStatus = new Span("LoginForm tab traversal idle");
        loginFormTabTraversalStatus.setId("framework-login-form-tab-traversal-status");
        LoginForm loginFormShiftTabTraversal = new LoginForm();
        loginFormShiftTabTraversal.setId("framework-login-form-shift-tab-traversal");
        loginFormShiftTabTraversal.setI18n(loginI18n);
        loginFormShiftTabTraversal.setForgotPasswordButtonVisible(true);
        loginFormShiftTabTraversal.setEnabled(true);
        loginFormShiftTabTraversal.setError(false);
        Span loginFormShiftTabTraversalStatus = new Span("LoginForm shift tab traversal idle");
        loginFormShiftTabTraversalStatus.setId("framework-login-form-shift-tab-traversal-status");
        LoginForm loginFormAutofillAttributes = new LoginForm();
        loginFormAutofillAttributes.setId("framework-login-form-autofill-attributes");
        loginFormAutofillAttributes.setI18n(loginI18n);
        loginFormAutofillAttributes.setForgotPasswordButtonVisible(true);
        loginFormAutofillAttributes.setEnabled(true);
        loginFormAutofillAttributes.setError(false);
        Span loginFormAutofillAttributesStatus = new Span("LoginForm autofill attributes idle");
        loginFormAutofillAttributesStatus.setId("framework-login-form-autofill-attributes-status");
        LoginForm loginFormA11ySemantics = new LoginForm();
        loginFormA11ySemantics.setId("framework-login-form-a11y-semantics");
        loginFormA11ySemantics.setI18n(loginI18n);
        loginFormA11ySemantics.setForgotPasswordButtonVisible(true);
        loginFormA11ySemantics.setEnabled(true);
        loginFormA11ySemantics.setError(false);
        Span loginFormA11ySemanticsStatus = new Span("LoginForm a11y semantics idle");
        loginFormA11ySemanticsStatus.setId("framework-login-form-a11y-semantics-status");

        RichTextEditor richTextEditor = new RichTextEditor();
        richTextEditor.setId("framework-rich-text-editor");
        richTextEditor.setValue("<p>Flow IPC rich text seed</p>");
        richTextEditor.addThemeVariants(RichTextEditorVariant.LUMO_COMPACT);

        Span richTextEditorStatus = new Span("RichTextEditor: pending");
        richTextEditorStatus.setId("framework-rich-text-editor-status");
        Span richTextEditorBehaviorStatus = new Span("RichTextEditor behavior: pending");
        richTextEditorBehaviorStatus.setId("framework-rich-text-editor-behavior-status");
        Span richTextEditorEditingStatus = new Span("RichTextEditor editing: pending");
        richTextEditorEditingStatus.setId("framework-rich-text-editor-editing-status");
        richTextEditor.addValueChangeListener(event -> {
            String value = event.getValue() == null ? "" : event.getValue();
            if (value.contains("Flow IPC rich text")) {
                richTextEditorStatus.setText("Rich text changed: Flow IPC rich text bold="
                        + value.contains("strong"));
            }
        });
        AtomicBoolean richTextEditorSelectionObserved = new AtomicBoolean();
        AtomicBoolean richTextEditorToolbarObserved = new AtomicBoolean();
        AtomicBoolean richTextEditorKeyboardObserved = new AtomicBoolean();
        AtomicBoolean richTextEditorValueObserved = new AtomicBoolean();
        AtomicBoolean richTextEditorFormatObserved = new AtomicBoolean();
        AtomicBoolean richTextEditorClipboardObserved = new AtomicBoolean();
        AtomicBoolean richTextEditorLinkObserved = new AtomicBoolean();
        richTextEditor.getElement().addEventListener("value-changed", event -> {
            JsonNode detailValueNode = event.getEventData().get("event.detail.value");
            JsonNode valueNode = event.getEventData().get("element.value");
            JsonNode htmlValueNode = event.getEventData().get("element.htmlValue");
            String value = detailValueNode == null
                    ? (valueNode == null ? "" : valueNode.asString())
                    : detailValueNode.asString();
            String htmlValue = htmlValueNode == null ? "" : htmlValueNode.asString();
            if (value.contains("Flow IPC rich text")
                    || htmlValue.contains("Flow IPC rich text")) {
                richTextEditorValueObserved.set(true);
                richTextEditorBehaviorStatus.setText("Rich text behavior delta/html: Flow IPC rich text bold="
                        + (value.contains("bold") || htmlValue.contains("strong")));
                return;
            }
            if (!value.isBlank()) {
                String marker = value.contains("Flow IPC rich text")
                        ? "Flow IPC rich text"
                        : value;
                richTextEditorStatus.setText("Rich text changed: " + marker);
            }
        }).addEventData("event.detail.value")
                .addEventData("element.value")
                .addEventData("element.htmlValue");
        richTextEditor.getElement().addEventListener("vatori-rich-text-editor-selection", event -> {
            JsonNode indexNode = event.getEventData().get("event.detail.index");
            JsonNode lengthNode = event.getEventData().get("event.detail.length");
            JsonNode textNode = event.getEventData().get("event.detail.text");
            int index = parseInt(indexNode);
            int length = parseInt(lengthNode);
            String text = textNode == null ? "" : textNode.asString();
            richTextEditorSelectionObserved.set(true);
            richTextEditorBehaviorStatus.setText("Rich text behavior selection: "
                    + index + "-" + (index + length) + " " + text);
        }).addEventData("event.detail.index")
                .addEventData("event.detail.length")
                .addEventData("event.detail.text");
        richTextEditor.getElement().addEventListener("vatori-rich-text-editor-toolbar", event -> {
            JsonNode formatNode = event.getEventData().get("event.detail.format");
            JsonNode sourceNode = event.getEventData().get("event.detail.source");
            String format = formatNode == null ? "" : formatNode.asString();
            String source = sourceNode == null ? "" : sourceNode.asString();
            richTextEditorToolbarObserved.set(true);
            richTextEditorBehaviorStatus.setText("Rich text behavior toolbar: "
                    + format + " source=" + source);
        }).addEventData("event.detail.format")
                .addEventData("event.detail.source");
        richTextEditor.getElement().addEventListener("vatori-rich-text-editor-format", event -> {
            JsonNode formatsNode = event.getEventData().get("event.detail.formats");
            JsonNode sourceNode = event.getEventData().get("event.detail.source");
            JsonNode rangeNode = event.getEventData().get("event.detail.range");
            String formats = formatsNode == null ? "" : formatsNode.asString();
            String source = sourceNode == null ? "" : sourceNode.asString();
            String range = rangeNode == null ? "" : rangeNode.asString();
            richTextEditorFormatObserved.set(true);
            richTextEditorEditingStatus.setText("Rich text editing format: "
                    + formats + " source=" + source + " range=" + range);
        }).addEventData("event.detail.formats")
                .addEventData("event.detail.source")
                .addEventData("event.detail.range");
        richTextEditor.getElement().addEventListener("vatori-rich-text-editor-clipboard", event -> {
            JsonNode operationNode = event.getEventData().get("event.detail.operation");
            JsonNode textNode = event.getEventData().get("event.detail.text");
            JsonNode htmlNode = event.getEventData().get("event.detail.html");
            String operation = operationNode == null ? "" : operationNode.asString();
            String text = textNode == null ? "" : textNode.asString();
            String html = htmlNode == null ? "" : htmlNode.asString();
            richTextEditorClipboardObserved.set(true);
            richTextEditorEditingStatus.setText("Rich text editing clipboard: "
                    + operation + " text=" + text + " html=" + html.contains("<strong>"));
            Desktop.current().clipboard().writeText("Flow IPC native clipboard | " + text);
            Desktop.current().clipboard().readText();
        }).addEventData("event.detail.operation")
                .addEventData("event.detail.text")
                .addEventData("event.detail.html");
        richTextEditor.getElement().addEventListener("vatori-rich-text-editor-link", event -> {
            JsonNode hrefNode = event.getEventData().get("event.detail.href");
            JsonNode textNode = event.getEventData().get("event.detail.text");
            JsonNode listNode = event.getEventData().get("event.detail.list");
            String href = hrefNode == null ? "" : hrefNode.asString();
            String text = textNode == null ? "" : textNode.asString();
            String list = listNode == null ? "" : listNode.asString();
            richTextEditorLinkObserved.set(true);
            richTextEditorEditingStatus.setText("Rich text editing link/list: "
                    + text + " href=" + href + " list=" + list);
        }).addEventData("event.detail.href")
                .addEventData("event.detail.text")
                .addEventData("event.detail.list");
        richTextEditor.getElement().addEventListener("keydown", event -> {
            JsonNode keyNode = event.getEventData().get("event.key");
            JsonNode metaKeyNode = event.getEventData().get("event.metaKey");
            JsonNode ctrlKeyNode = event.getEventData().get("event.ctrlKey");
            String key = keyNode == null ? "" : keyNode.asString();
            boolean shortcut = (metaKeyNode != null && metaKeyNode.asBoolean())
                    || (ctrlKeyNode != null && ctrlKeyNode.asBoolean());
            richTextEditorKeyboardObserved.set(true);
            richTextEditorBehaviorStatus.setText("Rich text behavior keyboard: "
                    + key + " shortcut=" + shortcut);
        }).addEventData("event.key")
                .addEventData("event.metaKey")
                .addEventData("event.ctrlKey");

        Span lifecycleFocusStatus = new Span("Lifecycle/focus behavior: pending");
        lifecycleFocusStatus.setId("framework-lifecycle-focus-status");

        TextField lifecycleFocus = new TextField("Lifecycle focus");
        lifecycleFocus.setId("framework-lifecycle-focus");
        lifecycleFocus.setValue("focus seed");
        lifecycleFocus.addFocusListener(event ->
                lifecycleFocusStatus.setText("Lifecycle focus: active"));
        lifecycleFocus.addBlurListener(event ->
                lifecycleFocusStatus.setText("Lifecycle blur: inactive"));

        Span lifecycleDynamic = new Span("Lifecycle dynamic probe");
        lifecycleDynamic.setId("framework-lifecycle-dynamic");
        AtomicInteger lifecycleAttachCount = new AtomicInteger();
        AtomicInteger lifecycleDetachCount = new AtomicInteger();
        lifecycleDynamic.addAttachListener(event ->
                lifecycleFocusStatus.setText("Lifecycle attached: dynamic "
                        + lifecycleAttachCount.incrementAndGet()));
        lifecycleDynamic.addDetachListener(event ->
                lifecycleFocusStatus.setText("Lifecycle detached: dynamic "
                        + lifecycleDetachCount.incrementAndGet()));

        Button lifecycleToggle = new Button("Toggle Lifecycle", event -> {
            if (lifecycleDynamic.getParent().isPresent()) {
                remove(lifecycleDynamic);
            } else {
                add(lifecycleDynamic);
            }
        });
        lifecycleToggle.setId("framework-lifecycle-toggle");

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
        grid.addThemeVariants(GridVariant.LUMO_COMPACT);
        grid.addColumn(WorkItem::id).setHeader("Ticket");
        grid.addColumn(WorkItem::owner).setHeader("Owner");
        grid.addColumn(WorkItem::state).setHeader("State");
        grid.setItems(workItems);
        grid.setAllRowsVisible(true);

        GridContextMenu<WorkItem> gridContextMenu = grid.addContextMenu();
        gridContextMenu.setId("framework-grid-context-menu");
        gridContextMenu.setOpenOnClick(true);
        gridContextMenu.addGridContextMenuOpenedListener(event -> {
            if (event.isOpened()) {
                overlayMatrixStatus.setText("Grid context menu opened: true");
            }
        });
        GridMenuItem<WorkItem> gridContextMenuAudit = gridContextMenu.addItem("Grid Audit",
                event -> overlayMatrixStatus.setText("Grid context menu action: audit"));
        gridContextMenuAudit.setId("framework-grid-context-menu-audit");

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
                Flow IPC StreamResource
                owner=framework-validation-app
                transport=flow-ipc-resource-request
                """.getBytes(StandardCharsets.UTF_8);
        StreamResource streamResource = new StreamResource("flow-ipc-framework-stream-resource.txt",
                (stream, session) -> stream.write(streamResourceBytes));
        streamResource.setContentType("text/plain; charset=UTF-8");
        streamResource.setHeader("X-Vatori-Flow-IPC", "stream-resource");

        Anchor streamResourceDownload = new Anchor(streamResource, "Download StreamResource");
        streamResourceDownload.setId("framework-stream-resource-download");
        streamResourceDownload.getElement().setAttribute("download", true);

        Span streamResourceStatus = new Span("StreamResource: pending");
        streamResourceStatus.setId("framework-stream-resource-status");

        byte[] binaryStreamResourceBytes = binaryStreamResourceBytes();
        StreamResource binaryStreamResource = new StreamResource("flow-ipc-framework-binary-stream-resource.bin",
                (stream, session) -> stream.write(binaryStreamResourceBytes));
        binaryStreamResource.setContentType("application/octet-stream");
        binaryStreamResource.setHeader("X-Vatori-Flow-IPC", "stream-resource-range");

        Anchor streamResourceRangeDownload = new Anchor(binaryStreamResource, "Download Binary StreamResource");
        streamResourceRangeDownload.setId("framework-stream-resource-range-download");
        streamResourceRangeDownload.getElement().setAttribute("download", true);

        Span streamResourceRangeStatus = new Span("StreamResource range: pending");
        streamResourceRangeStatus.setId("framework-stream-resource-range-status");

        Span streamResourceChunksStatus = new Span("StreamResource chunks: pending");
        streamResourceChunksStatus.setId("framework-stream-resource-chunks-status");

        byte[] downloadHandlerBytes = """
                Flow IPC DownloadHandler
                owner=framework-validation-app
                transport=flow-ipc-resource-request-to-native-save
                """.getBytes(StandardCharsets.UTF_8);
        DownloadHandler inputStreamDownloadHandler = DownloadHandler.fromInputStream(
                event -> new DownloadResponse(
                        new ByteArrayInputStream(downloadHandlerBytes),
                        "flow-ipc-download-handler.txt",
                        "text/plain; charset=UTF-8",
                        downloadHandlerBytes.length));
        DownloadHandler namedDownloadHandler = new DownloadHandler() {
            @Override
            public void handleDownloadRequest(DownloadEvent event) throws IOException {
                inputStreamDownloadHandler.handleDownloadRequest(event);
            }

            @Override
            public String getUrlPostfix() {
                return "flow-ipc-download-handler.txt";
            }
        };
        Anchor downloadHandlerSaveOpen = new Anchor(namedDownloadHandler,
                "DownloadHandler Save/Open");
        downloadHandlerSaveOpen.setId("framework-download-handler-save-open");

        byte[] standardStreamingDownloadBytes = standardStreamingDownloadBytes();
        DownloadHandler standardStreamingDownloadHandler = DownloadHandler.fromInputStream(
                event -> new DownloadResponse(
                        new ByteArrayInputStream(standardStreamingDownloadBytes),
                        "flow-ipc-standard-streaming.bin",
                        "application/octet-stream",
                        standardStreamingDownloadBytes.length));
        Anchor standardStreamingDownload = new Anchor(
                standardStreamingDownloadHandler,
                "Standard bundle streaming probe");
        standardStreamingDownload.setId("framework-standard-streaming-download");

        Span downloadHandlerSaveOpenStatus = new Span("DownloadHandler save/open: pending");
        downloadHandlerSaveOpenStatus.setId("framework-download-handler-save-open-status");

        Span uploadStatus = new Span("Upload receiver: pending");
        uploadStatus.setId("framework-upload-status");
        Upload upload = new Upload(UploadHandler.inMemory((metadata, bytes) -> {
            String body = new String(bytes, StandardCharsets.UTF_8);
            boolean multipartMarker = body.contains("Flow IPC Upload Multipart");
            boolean marker = multipartMarker || body.contains("Flow IPC Upload");
            String prefix = multipartMarker ? "Multipart upload received: " : "Upload received: ";
            uploadStatus.setText(prefix + metadata.fileName()
                    + " bytes=" + bytes.length
                    + " marker=" + marker
                    + " contentType=" + metadata.contentType()
                    + (multipartMarker ? " progress=completed cancel=classified" : ""));
        }));
        upload.setId("framework-upload");
        upload.setMaxFiles(1);
        upload.setMaxFileSize(1024 * 1024);
        upload.setAcceptedFileTypes("text/plain", ".txt");
        upload.setUploadFormat(UploadFormat.MULTIPART);
        upload.setAutoUpload(false);

        List<String> virtualListItems = IntStream.rangeClosed(1, 80)
                .mapToObj(index -> "Virtual queue item " + String.format("%03d", index))
                .toList();
        Span virtualListStatus = new Span("VirtualList: pending");
        virtualListStatus.setId("framework-virtual-list-status");
        AtomicInteger virtualListFetchCount = new AtomicInteger();
        AtomicReference<String> virtualListLastRange = new AtomicReference<>("pending");
        VirtualList<String> virtualList = new VirtualList<>();
        virtualList.setId("framework-virtual-list");
        virtualList.setHeight("160px");
        virtualList.setWidth("360px");
        virtualList.setDataProvider(DataProvider.fromCallbacks(query -> {
            int offset = query.getOffset();
            int limit = query.getLimit();
            int end = Math.min(offset + limit, virtualListItems.size());
            int fetchCount = virtualListFetchCount.incrementAndGet();
            virtualListLastRange.set(offset + "-" + end);
            virtualListStatus.setText("VirtualList fetch " + fetchCount + ": " + offset + "-" + end);
            return virtualListItems.stream().skip(offset).limit(limit);
        }, query -> virtualListItems.size()));
        virtualList.setRenderer(item -> item);
        Button virtualListScrollEnd = new Button("Scroll VirtualList End", event -> {
            virtualList.scrollToIndex(79);
            virtualListStatus.setText("VirtualList server scroll: 79 range="
                    + virtualListLastRange.get());
        });
        virtualListScrollEnd.setId("framework-virtual-list-scroll-end");

        OwnerForm ownerForm = new OwnerForm();
        ownerForm.setOwner("platform");
        Binder<OwnerForm> binder = new Binder<>(OwnerForm.class);
        TextField ownerName = new TextField("Owner name");
        ownerName.setId("framework-owner-name");
        ownerName.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        ownerName.setValue(ownerForm.getOwner());
        binder.forField(ownerName)
                .withValidator(value -> value != null && value.trim().length() >= 3,
                        "Owner name must have at least three characters")
                .bind(OwnerForm::getOwner, OwnerForm::setOwner);
        binder.setBean(ownerForm);

        Span validationStatus = new Span("Validation: pending");
        validationStatus.setId("framework-validation-status");

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
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("360px", 2));
        formLayout.setLabelWidth("96px");
        formLayout.setLabelSpacing("12px");
        formLayout.setColumnSpacing("18px");
        formLayout.setRowSpacing("10px");
        formLayout.setColumnWidth("16em");
        formLayout.setMaxColumns(2);
        formLayout.setMinColumns(1);
        formLayout.setExpandFields(true);
        formLayout.setLabelsAside(true);
        Span formLayoutStatus = new Span("FormLayout: columns=2 colspan=2");
        formLayoutStatus.setId("framework-form-layout-status");
        formLayout.addFormItem(ownerName, "Owner");
        formLayout.add(validateOwner, validationStatus);
        formLayout.setColspan(validationStatus, 2);
        formLayout.addClickListener(event ->
                formLayoutStatus.setText("FormLayout clicked: true owner="
                        + ownerForm.getOwner()));

        Button formLayoutServerUpdate = new Button("Update FormLayout", event -> {
            formLayout.setResponsiveSteps(
                    new FormLayout.ResponsiveStep("0", 1),
                    new FormLayout.ResponsiveStep("360px", 2),
                    new FormLayout.ResponsiveStep("600px", 3));
            formLayout.setMaxColumns(3);
            formLayout.setColumnWidth("14em");
            formLayout.setLabelWidth("112px");
            formLayout.setLabelSpacing("16px");
            formLayout.setColumnSpacing("24px");
            formLayout.setRowSpacing("12px");
            formLayout.setExpandColumns(true);
            formLayout.setColspan(validationStatus, 3);
            formLayoutStatus.setText("FormLayout server layout: max=3 colspan=3 label=112px column=24px");
        });
        formLayoutServerUpdate.setId("framework-form-layout-server-update");

        Span horizontalLead = new Span("Queue");
        horizontalLead.setId("framework-horizontal-layout-lead");
        Span horizontalGrow = new Span("Review lane");
        horizontalGrow.setId("framework-horizontal-layout-grow");
        Span horizontalTail = new Span("Ready");
        horizontalTail.setId("framework-horizontal-layout-tail");
        HorizontalLayout horizontalLayout = new HorizontalLayout(horizontalLead, horizontalGrow, horizontalTail);
        horizontalLayout.setId("framework-horizontal-layout");
        horizontalLayout.setWidth("420px");
        horizontalLayout.setHeight("72px");
        horizontalLayout.setSpacing("14px");
        horizontalLayout.setPadding(false);
        horizontalLayout.setMargin(false);
        horizontalLayout.setWrap(false);
        horizontalLayout.setBoxSizing(BoxSizing.BORDER_BOX);
        horizontalLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        horizontalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        horizontalLayout.setAlignSelf(FlexComponent.Alignment.END, horizontalTail);
        horizontalLayout.setFlexGrow(1.0, horizontalGrow);
        horizontalLayout.setFlexShrink(0.0, horizontalLead);
        Span horizontalLayoutStatus = new Span("HorizontalLayout: between center grow=1");
        horizontalLayoutStatus.setId("framework-horizontal-layout-status");
        horizontalLayout.addClickListener(event ->
                horizontalLayoutStatus.setText("HorizontalLayout clicked: true children="
                        + horizontalLayout.getComponentCount()));
        Button horizontalLayoutServerUpdate = new Button("Update HorizontalLayout", event -> {
            horizontalLayout.setWidth("480px");
            horizontalLayout.setSpacing("22px");
            horizontalLayout.setPadding(true);
            horizontalLayout.setMargin(true);
            horizontalLayout.setWrap(true);
            horizontalLayout.setBoxSizing(BoxSizing.CONTENT_BOX);
            horizontalLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
            horizontalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.EVENLY);
            horizontalLayout.setAlignSelf(FlexComponent.Alignment.BASELINE, horizontalTail);
            horizontalLayout.setFlexGrow(2.0, horizontalGrow);
            horizontalTail.setText("Aligned");
            horizontalLayoutStatus.setText("HorizontalLayout server layout: evenly stretch grow=2 tail=baseline");
        });
        horizontalLayoutServerUpdate.setId("framework-horizontal-layout-server-update");

        Span flexAlpha = new Span("Alpha");
        flexAlpha.setId("framework-flex-layout-alpha");
        Span flexBeta = new Span("Beta");
        flexBeta.setId("framework-flex-layout-beta");
        Span flexGamma = new Span("Gamma");
        flexGamma.setId("framework-flex-layout-gamma");
        FlexLayout flexLayout = new FlexLayout(flexAlpha, flexBeta, flexGamma);
        flexLayout.setId("framework-flex-layout");
        flexLayout.setWidth("420px");
        flexLayout.setHeight("112px");
        flexLayout.getStyle().set("gap", "8px");
        flexLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        flexLayout.setFlexDirection(FlexLayout.FlexDirection.ROW);
        flexLayout.setAlignContent(FlexLayout.ContentAlignment.CENTER);
        flexLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        flexLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        flexLayout.setFlexBasis("120px", flexBeta);
        flexLayout.setOrder(1, flexGamma);
        flexLayout.setFlexGrow(1.0, flexBeta);
        Span flexLayoutStatus = new Span("FlexLayout: wrap row basis=120 order=1");
        flexLayoutStatus.setId("framework-flex-layout-status");
        flexLayout.addClickListener(event ->
                flexLayoutStatus.setText("FlexLayout clicked: true children="
                        + flexLayout.getComponentCount()));
        Button flexLayoutServerUpdate = new Button("Update FlexLayout", event -> {
            flexLayout.setWidth("460px");
            flexLayout.getStyle().set("gap", "18px");
            flexLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP_REVERSE);
            flexLayout.setFlexDirection(FlexLayout.FlexDirection.COLUMN_REVERSE);
            flexLayout.setAlignContent(FlexLayout.ContentAlignment.SPACE_BETWEEN);
            flexLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.AROUND);
            flexLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
            flexLayout.setFlexBasis("180px", flexBeta);
            flexLayout.setOrder(3, flexAlpha);
            flexLayout.setFlexGrow(2.0, flexBeta);
            flexGamma.setText("Gamma patched");
            flexLayoutStatus.setText("FlexLayout server layout: column-reverse wrap-reverse basis=180 order=3");
        });
        flexLayoutServerUpdate.setId("framework-flex-layout-server-update");

        Span verticalHeader = new Span("Inbox");
        verticalHeader.setId("framework-vertical-layout-header");
        Span verticalBody = new Span("Review stack");
        verticalBody.setId("framework-vertical-layout-body");
        Span verticalFooter = new Span("Queued");
        verticalFooter.setId("framework-vertical-layout-footer");
        VerticalLayout verticalLayout = new VerticalLayout(verticalHeader, verticalBody, verticalFooter);
        verticalLayout.setId("framework-vertical-layout");
        verticalLayout.setWidth("320px");
        verticalLayout.setHeight("148px");
        verticalLayout.setSpacing("10px");
        verticalLayout.setPadding(false);
        verticalLayout.setMargin(false);
        verticalLayout.setBoxSizing(BoxSizing.BORDER_BOX);
        verticalLayout.setAlignItems(FlexComponent.Alignment.START);
        verticalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        verticalLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.END, verticalBody);
        verticalLayout.setFlexGrow(1.0, verticalFooter);
        Span verticalLayoutStatus = new Span("VerticalLayout: between start grow=1 body=end");
        verticalLayoutStatus.setId("framework-vertical-layout-status");
        verticalLayout.addClickListener(event ->
                verticalLayoutStatus.setText("VerticalLayout clicked: true children="
                        + verticalLayout.getComponentCount()));
        Button verticalLayoutServerUpdate = new Button("Update VerticalLayout", event -> {
            verticalLayout.setWidth("360px");
            verticalLayout.setHeight("168px");
            verticalLayout.setSpacing("20px");
            verticalLayout.setPadding(true);
            verticalLayout.setMargin(true);
            verticalLayout.setBoxSizing(BoxSizing.CONTENT_BOX);
            verticalLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
            verticalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.EVENLY);
            verticalLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, verticalBody);
            verticalLayout.setFlexGrow(2.0, verticalFooter);
            verticalFooter.setText("Dequeued");
            verticalLayoutStatus.setText("VerticalLayout server layout: evenly stretch grow=2 body=center");
        });
        verticalLayoutServerUpdate.setId("framework-vertical-layout-server-update");

        VerticalLayout scrollerContent = new VerticalLayout();
        scrollerContent.setId("framework-scroller-content");
        scrollerContent.setPadding(false);
        scrollerContent.setSpacing(false);
        scrollerContent.setWidth("280px");
        scrollerContent.setHeight("520px");
        IntStream.rangeClosed(1, 12).forEach(index -> {
            Span row = new Span(String.format("Scroll item %02d", index));
            if (index == 12) {
                row.setId("framework-scroller-bottom");
            }
            row.getStyle()
                    .set("display", "block")
                    .set("height", "42px");
            scrollerContent.add(row);
        });
        Scroller scroller = new Scroller(scrollerContent, Scroller.ScrollDirection.VERTICAL);
        scroller.setId("framework-scroller");
        scroller.setWidth("300px");
        scroller.setHeight("128px");
        scroller.addThemeVariants(ScrollerVariant.LUMO_OVERFLOW_INDICATORS);
        Span scrollerStatus = new Span("Scroller: vertical ready");
        scrollerStatus.setId("framework-scroller-status");
        AtomicInteger scrollerScrollCount = new AtomicInteger();
        scroller.getElement().addEventListener("scroll", event -> {
            JsonNode topNode = event.getEventData().get("element.scrollTop");
            int scrollTop = topNode == null ? -1 : topNode.asInt();
            scrollerStatus.setText("Scroller scrolled: top=" + scrollTop
                    + " count=" + scrollerScrollCount.incrementAndGet());
        }).addEventData("element.scrollTop");
        Button scrollerServerUpdate = new Button("Update Scroller", event -> {
            HorizontalLayout horizontalScrollerContent = new HorizontalLayout();
            horizontalScrollerContent.setId("framework-scroller-content");
            horizontalScrollerContent.setPadding(false);
            horizontalScrollerContent.setSpacing("12px");
            horizontalScrollerContent.setWidth("760px");
            horizontalScrollerContent.setHeight("88px");
            IntStream.rangeClosed(1, 6).forEach(index -> {
                Span item = new Span("Horizontal patched " + index);
                if (index == 6) {
                    item.setId("framework-scroller-horizontal-tail");
                }
                item.getStyle()
                        .set("display", "inline-flex")
                        .set("min-width", "116px")
                        .set("height", "64px")
                        .set("align-items", "center");
                horizontalScrollerContent.add(item);
            });
            scroller.setContent(horizontalScrollerContent);
            scroller.setScrollDirection(Scroller.ScrollDirection.HORIZONTAL);
            scroller.setWidth("340px");
            scroller.setHeight("96px");
            scrollerStatus.setText("Scroller server layout: horizontal patched width=340");
        });
        scrollerServerUpdate.setId("framework-scroller-server-update");

        Dialog detailsDialog = new Dialog();
        detailsDialog.setId("framework-dialog");
        Span dialogContent = new Span("Dialog details pending");
        dialogContent.setId("framework-dialog-content");
        detailsDialog.add(dialogContent);

        Span componentStatus = new Span("Components: pending");
        componentStatus.setId("framework-component-status");
        Span latestUpdate = new Span("Revision pending");
        latestUpdate.setId("framework-latest-update");
        AtomicInteger frameworkBaseChangeCount = new AtomicInteger();
        reviewer.addValueChangeListener(event -> {
            String value = event.getValue();
            if (value != null && !value.isBlank()) {
                reviewerValue = value;
            }
            matrixStatus.setText("Reviewer changed: " + reviewerValue
                    + " change=" + frameworkBaseChangeCount.incrementAndGet());
        });

        budget.addValueChangeListener(event -> {
            BigDecimal value = event.getValue();
            componentStatus.setText("Budget changed: " + (value == null ? "" : value)
                    + " change=" + frameworkBaseChangeCount.incrementAndGet());
        });

        expedited.addValueChangeListener(event -> {
            boolean checked = Boolean.TRUE.equals(event.getValue());
            componentStatus.setText("Expedite changed: " + (checked ? "yes" : "no")
                    + " change=" + frameworkBaseChangeCount.incrementAndGet());
        });

        priority.addValueChangeListener(event -> {
            String value = event.getValue();
            if (value != null && !value.isBlank()) {
                priorityValue = value;
            }
            componentStatus.setText("Priority changed: " + priorityValue
                    + " change=" + frameworkBaseChangeCount.incrementAndGet());
        });

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
            String clipboardText = update + " | Flow IPC native clipboard";
            status.setText(update);
            latestUpdate.setText(update);
            Desktop.current().setWindowTitle("Flow IPC Framework App - " + revision);
            Desktop.current().clipboard().writeText(clipboardText);
            Desktop.current().clipboard().readText();
            Desktop.current().notifications().show("Flow IPC", "Framework update " + revision + " applied");
        });
        apply.setId("framework-apply");
        apply.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        updateList.setId("framework-updates");
        updateList.setPadding(false);
        updateList.setSpacing(false);
        appendUpdate("Catalog loaded");
        appendUpdate("Renderer owned by Flow client engine");
        updateList.add(latestUpdate);

        add(title, standardPushStatus, budget, expedited,
                auditChannels, checkboxGroupServerUpdate, checkboxGroupStatus,
                priority, reviewer, dueDate, decision, caseTitle,
                textFieldServerUpdate, textFieldStatus, notes,
                contactEmail, score, riskScore, numberFieldServerUpdate, numberFieldStatus,
                secret, reviewTime, reviewDateTime,
                dateTimePickerServerUpdate, dateTimePickerStatus, inputMatrixStatus, assignees,
                assigneesStatus, laneListBox, listBoxServerUpdate, listBoxStatus,
                reviewStageListBox, multiSelectListBoxServerUpdate, multiSelectListBoxStatus,
                customAssignment, customFieldServerUpdate, customFieldStatus,
                splitLayout, splitLayoutServerUpdate, splitLayoutStatus,
                navTabs, auditDetails, queueAccordion, menu, navigationMatrixStatus,
                contextMenuTarget, popoverTarget, popover, notificationOpen, notificationStatus,
                confirmDialogOpen, confirmDialog, confirmDialogStatus,
                progressBar, progressValueUpdate, progressIndeterminateUpdate, progressBarStatus,
                overlayMatrixStatus, openDialog, detailsDialog,
                avatar, avatarGroup, messageList, messageInput, sideNav, loginOverlay,
                loginOverlayReopen, richContentStatus, loginForm, loginFormStatus,
                loginFormActionPost, loginFormActionPostStatus,
                loginOverlayActionPostOpen, loginOverlayActionPost, loginOverlayActionPostStatus,
                loginOverlaySubmitButton, loginOverlaySubmitButtonStatus,
                loginOverlayEnter, loginOverlayEnterStatus,
                loginFormShowError, loginFormShowErrorStatus,
                loginFormButton, loginFormButtonStatus, loginFormEnter, loginFormEnterStatus,
                loginFormEnable, loginFormEnabled, loginFormEnabledStatus,
                loginOverlayFullI18n, loginOverlayFullI18nStatus,
                loginOverlayNoAutofocus, loginOverlayNoAutofocusStatus,
                loginOverlayAutofocusControlHost, loginOverlayAutofocusControlStatus,
                loginOverlayOutsideClose, loginOverlayOutsideCloseStatus,
                loginOverlayEscapeKey, loginOverlayEscapeKeyStatus,
                loginOverlayTabTraversal, loginOverlayTabTraversalStatus,
                loginOverlayShiftTabTraversal, loginOverlayShiftTabTraversalStatus,
                loginOverlayAutofillAttributes, loginOverlayAutofillAttributesStatus,
                loginOverlayA11ySemantics, loginOverlayA11ySemanticsStatus,
                loginOverlayDisabledGuard, loginOverlayDisabledGuardStatus,
                loginOverlayDisabledKeyboardGuard, loginOverlayDisabledKeyboardGuardStatus,
                loginFormFullI18n, loginFormFullI18nStatus,
                loginFormNoAutofocusStatus, loginFormNoAutofocusHost,
                loginFormAutofocusControl, loginFormAutofocusControlStatus,
                loginFormTabTraversal, loginFormTabTraversalStatus,
                loginFormShiftTabTraversal, loginFormShiftTabTraversalStatus,
                loginFormAutofillAttributes, loginFormAutofillAttributesStatus,
                loginFormA11ySemantics, loginFormA11ySemanticsStatus,
                richTextEditor, richTextEditorStatus, richTextEditorBehaviorStatus,
                richTextEditorEditingStatus,
                lifecycleFocus, lifecycleToggle, lifecycleFocusStatus,
                grid, lazyGrid, lazyGridStatus, lazyGridReport, streamResourceDownload,
                streamResourceStatus, streamResourceRangeDownload, streamResourceRangeStatus,
                streamResourceChunksStatus, downloadHandlerSaveOpen, downloadHandlerSaveOpenStatus,
                standardStreamingDownload,
                upload, uploadStatus, virtualList, virtualListScrollEnd, virtualListStatus,
                formLayout, formLayoutServerUpdate, formLayoutStatus,
                horizontalLayout, horizontalLayoutServerUpdate, horizontalLayoutStatus,
                flexLayout, flexLayoutServerUpdate, flexLayoutStatus,
                verticalLayout, verticalLayoutServerUpdate, verticalLayoutStatus,
                scroller, scrollerServerUpdate, scrollerStatus,
                apply, status, componentStatus, matrixStatus,
                updateList);
    }

    private void appendUpdate(String text) {
        updateList.add(new Span(text));
    }

    private static AvatarGroupItem avatarGroupItem(String name, String abbreviation, int colorIndex) {
        AvatarGroupItem item = new AvatarGroupItem(name);
        item.setAbbreviation(abbreviation);
        item.setColorIndex(colorIndex);
        return item;
    }

    private static int parseInt(JsonNode node) {
        if (node == null) {
            return 0;
        }
        try {
            return Integer.parseInt(node.asString());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static byte[] binaryStreamResourceBytes() {
        byte[] bytes = new byte[8192];
        for (int index = 0; index < bytes.length; index++) {
            bytes[index] = (byte) ((index * 31 + 17) & 0xff);
        }
        byte[] marker = "Flow IPC Binary StreamResource\n".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(marker, 0, bytes, 0, marker.length);
        byte[] tail = "\nrange-tail=stream-resource-range".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(tail, 0, bytes, bytes.length - tail.length, tail.length);
        return bytes;
    }

    private static byte[] standardStreamingDownloadBytes() {
        byte[] bytes = new byte[512 * 1024 + 73];
        for (int index = 0; index < bytes.length; index++) {
            bytes[index] = (byte) ('a' + index % 26);
        }
        byte[] marker = "Flow IPC Standard Bundle Streaming Download\n"
                .getBytes(StandardCharsets.UTF_8);
        System.arraycopy(marker, 0, bytes, 0, marker.length);
        return bytes;
    }

    private record WorkItem(String id, String owner, String state) {
    }

    private static final class ReviewAssignmentField extends CustomField<String> {
        private final TextField owner = new TextField("Owner");
        private final TextField state = new TextField("State");
        private boolean updatingPresentation;

        private ReviewAssignmentField() {
            setId("framework-custom-field");
            setLabel("Custom assignment");
            owner.setId("framework-custom-field-owner");
            owner.addThemeVariants(TextFieldVariant.LUMO_SMALL);
            state.setId("framework-custom-field-state");
            state.addThemeVariants(TextFieldVariant.LUMO_SMALL);
            owner.addValueChangeListener(event -> updateFromChildren());
            state.addValueChangeListener(event -> updateFromChildren());
            add(owner, state);
            setValue("draft|queued");
        }

        @Override
        protected String generateModelValue() {
            return normalize(owner.getValue()) + "|" + normalize(state.getValue());
        }

        @Override
        protected void setPresentationValue(String value) {
            String[] parts = splitCustomFieldValue(value);
            updatingPresentation = true;
            try {
                owner.setValue(parts[0]);
                state.setValue(parts[1]);
            } finally {
                updatingPresentation = false;
            }
        }

        private void updateFromChildren() {
            if (!updatingPresentation) {
                updateValue();
            }
        }

        private static String normalize(String value) {
            return value == null ? "" : value.trim();
        }

        private static String[] splitCustomFieldValue(String value) {
            String normalized = normalize(value);
            int separator = normalized.indexOf('|');
            if (separator < 0) {
                return new String[]{normalized, ""};
            }
            return new String[]{
                    normalized.substring(0, separator),
                    normalized.substring(separator + 1)
            };
        }
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
