package dev.vatori.ipcframework;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.avatar.AvatarGroup.AvatarGroupItem;
import com.vaadin.flow.component.avatar.AvatarGroupVariant;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBoxVariant;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datepicker.DatePickerVariant;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.richtexteditor.RichTextEditor;
import com.vaadin.flow.component.richtexteditor.RichTextEditorVariant;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.select.SelectVariant;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.TabsVariant;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextAreaVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.component.timepicker.TimePickerVariant;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.binder.Binder;
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
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
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
        budget.addThemeVariants(TextFieldVariant.LUMO_SMALL);

        Checkbox expedited = new Checkbox("Expedite processing");
        expedited.setId("framework-expedite");
        expedited.setHelperText("Routes through the fast lane");
        expedited.addThemeVariants(CheckboxVariant.LUMO_HELPER_ABOVE_FIELD);

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

        RadioButtonGroup<String> decision = new RadioButtonGroup<>("Decision");
        decision.setId("framework-decision");
        decision.setItems("review", "approve", "hold");
        decision.setValue("review");
        decision.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);

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

        PasswordField secret = new PasswordField("Secret");
        secret.setId("framework-secret");
        secret.setValue("initial-secret");
        secret.addThemeVariants(TextFieldVariant.LUMO_SMALL);

        TimePicker reviewTime = new TimePicker("Review time");
        reviewTime.setId("framework-review-time");
        reviewTime.setValue(java.time.LocalTime.of(9, 30));
        reviewTime.addThemeVariants(TimePickerVariant.LUMO_SMALL);

        Span inputMatrixStatus = new Span("Input matrix: pending");
        inputMatrixStatus.setId("framework-input-matrix-status");

        MultiSelectComboBox<String> assignees = new MultiSelectComboBox<>("Assignees");
        assignees.setId("framework-assignees");
        assignees.setItems("ava", "sol", "kai");
        assignees.setValue(Set.of("ava"));
        assignees.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);

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
        queueAccordion.add("Queue", new Span("Queue disclosure content"));

        Span navigationMatrixStatus = new Span("Navigation matrix: pending");
        navigationMatrixStatus.setId("framework-navigation-matrix-status");

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
        contextMenuTarget.addClickListener(event ->
                overlayMatrixStatus.setText("Context menu opened: true"));
        contextMenuTarget.getElement().addEventListener("vatori-context-menu-audit", event ->
                overlayMatrixStatus.setText("Context menu action: audit"));
        contextMenu.getElement().addEventListener("vatori-context-menu-open", event ->
                overlayMatrixStatus.setText("Context menu opened: true"));

        MenuItem contextMenuAudit = contextMenu.addItem("Context Audit",
                event -> overlayMatrixStatus.setText("Context menu action: audit"));
        contextMenuAudit.setId("framework-context-menu-audit");
        contextMenuAudit.getElement().addEventListener("click", event ->
                overlayMatrixStatus.setText("Context menu action: audit"));
        contextMenu.getElement().addEventListener("vatori-context-menu-audit", event ->
                overlayMatrixStatus.setText("Context menu action: audit"));

        Button popoverTarget = new Button("Popover Target");
        popoverTarget.setId("framework-popover-target");
        Popover popover = new Popover(new Span("Popover audit content"));
        popover.setId("framework-popover");
        popover.setTarget(popoverTarget);
        popover.setOpenOnClick(true);
        popoverTarget.addClickListener(event ->
                overlayMatrixStatus.setText("Popover opened: true"));
        popoverTarget.getElement().addEventListener("vatori-popover-open", event ->
                overlayMatrixStatus.setText("Popover opened: true"));
        popover.getElement().addEventListener("vatori-popover-open", event ->
                overlayMatrixStatus.setText("Popover opened: true"));

        Span richContentStatus = new Span("Rich content: pending");
        richContentStatus.setId("framework-rich-content-status");
        richContentStatus.getElement().addEventListener("vatori-avatar-click", event ->
                richContentStatus.setText("Avatar clicked: ava"));
        richContentStatus.getElement().addEventListener("vatori-message-submit", event -> {
            JsonNode valueNode = event.getEventData().get("event.detail.value");
            String value = valueNode == null ? "" : valueNode.asString();
            richContentStatus.setText("Message submitted: " + value);
        }).addEventData("event.detail.value");
        richContentStatus.getElement().addEventListener("vatori-side-nav-audit", event ->
                richContentStatus.setText("Side nav selected: audit"));
        richContentStatus.getElement().addEventListener("vatori-login-overlay-open", event ->
                richContentStatus.setText("Login overlay opened: true"));
        richContentStatus.getElement().addEventListener("vatori-login-submit", event -> {
            JsonNode usernameNode = event.getEventData().get("event.detail.username");
            String username = usernameNode == null ? "" : usernameNode.asString();
            richContentStatus.setText("Login submitted: " + username);
        }).addEventData("event.detail.username");

        Avatar avatar = new Avatar("Ava Desktop");
        avatar.setId("framework-avatar");
        avatar.addThemeVariants(AvatarVariant.LUMO_SMALL);
        avatar.setAbbreviation("AD");
        avatar.setColorIndex(4);
        avatar.setTooltipEnabled(true);
        avatar.getElement().addEventListener("click", event ->
                richContentStatus.setText("Avatar clicked: ava"));
        avatar.getElement().addEventListener("vatori-avatar-click", event ->
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
        messageInput.getElement().addEventListener("vatori-message-submit", event -> {
            JsonNode valueNode = event.getEventData().get("event.detail.value");
            String value = valueNode == null ? "" : valueNode.asString();
            messageList.addItem(new MessageListItem(
                    "Echo: " + value,
                    Instant.parse("2026-06-10T00:02:00Z"),
                    "Flow IPC",
                    "FI"));
            richContentStatus.setText("Message submitted: " + value);
        }).addEventData("event.detail.value");

        SideNav sideNav = new SideNav("Framework navigation");
        sideNav.setId("framework-side-nav");
        sideNav.addThemeVariants(SideNavVariant.AURA_FILLED);
        sideNav.setCollapsible(true);
        sideNav.setExpanded(true);
        SideNavItem sideNavAudit = new SideNavItem("Audit queue", "#audit-queue");
        sideNavAudit.setId("framework-side-nav-audit");
        sideNavAudit.getElement().addEventListener("click", event ->
                richContentStatus.setText("Side nav selected: audit"));
        sideNav.getElement().addEventListener("vatori-side-nav-audit", event ->
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
        loginOverlay.getElement().addEventListener("vatori-login-overlay-open", event ->
                richContentStatus.setText("Login overlay opened: true"));
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
        loginOverlay.getElement().addEventListener("vatori-login-submit", event -> {
            JsonNode usernameNode = event.getEventData().get("event.detail.username");
            String username = usernameNode == null ? "" : usernameNode.asString();
            richContentStatus.setText("Login submitted: " + username);
            loginOverlay.close();
        }).addEventData("event.detail.username");
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
        final String loginFormActionPostPath = "flow-ipc-login-form-action-v1_101";
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
        final String loginOverlayActionPostPath = "flow-ipc-login-overlay-action-v1_116";
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
        loginOverlayActionPostStatus.getElement().addEventListener(
                "vatori-login-overlay-action-post-open", event -> {
                    loginOverlayActionPost.setOpened(true);
                    loginOverlayActionPostStatus.setText("LoginOverlay action post opened: true");
                });
        loginOverlayActionPost.addLoginListener(event -> {
            String password = event.getPassword() == null ? "" : event.getPassword();
            loginOverlayActionPost.setEnabled(true);
            loginOverlayActionPost.setError(false);
            loginOverlayActionPostStatus.setText("LoginOverlay action post submitted: "
                    + event.getUsername() + " password-length="
                    + password.length() + " action="
                    + loginOverlayActionPostPath);
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
        LoginOverlay loginOverlayAutofocusControl = new LoginOverlay(loginI18n);
        loginOverlayAutofocusControl.setId("framework-login-overlay-autofocus-control");
        loginOverlayAutofocusControl.setTitle("Flow IPC Login autofocus control");
        loginOverlayAutofocusControl.setDescription("Framework validation");
        loginOverlayAutofocusControl.setForgotPasswordButtonVisible(true);
        loginOverlayAutofocusControl.setError(false);
        Span loginOverlayAutofocusControlStatus = new Span("LoginOverlay autofocus control idle");
        loginOverlayAutofocusControlStatus.setId("framework-login-overlay-autofocus-control-status");
        loginOverlayAutofocusControl.getElement().addEventListener(
                "vatori-login-overlay-autofocus-control-open", event -> {
                    loginOverlayAutofocusControl.setOpened(true);
                    loginOverlayAutofocusControlStatus.setText("LoginOverlay autofocus control opened: true");
                });
        LoginOverlay loginOverlayEscClose = new LoginOverlay(loginI18n);
        loginOverlayEscClose.setId("framework-login-overlay-esc-close");
        loginOverlayEscClose.setTitle("Flow IPC LoginOverlay ESC close");
        loginOverlayEscClose.setDescription("Framework validation");
        loginOverlayEscClose.setForgotPasswordButtonVisible(true);
        loginOverlayEscClose.setError(false);
        Span loginOverlayEscCloseStatus = new Span("LoginOverlay ESC close idle");
        loginOverlayEscCloseStatus.setId("framework-login-overlay-esc-close-status");
        loginOverlayEscClose.getElement().addEventListener("vatori-login-overlay-esc-open", event -> {
            loginOverlayEscClose.setOpened(true);
            loginOverlayEscCloseStatus.setText("LoginOverlay ESC close opened: true");
        });
        loginOverlayEscClose.getElement().addEventListener("opened-changed", event -> {
            JsonNode openedNode = event.getEventData().get("element.opened");
            boolean opened = openedNode != null && Boolean.parseBoolean(openedNode.asString());
            loginOverlayEscClose.setOpened(opened);
            loginOverlayEscCloseStatus.setText("LoginOverlay ESC close opened: " + opened);
        }).addEventData("element.opened");
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
        LoginForm loginFormNoAutofocus = new LoginForm();
        loginFormNoAutofocus.setId("framework-login-form-no-autofocus");
        loginFormNoAutofocus.setI18n(loginI18n);
        loginFormNoAutofocus.setForgotPasswordButtonVisible(true);
        loginFormNoAutofocus.setEnabled(true);
        loginFormNoAutofocus.setError(false);
        loginFormNoAutofocus.getElement().setProperty("noAutofocus", true);
        loginFormNoAutofocus.getElement().setAttribute("no-autofocus", true);
        loginFormNoAutofocus.addAttachListener(event -> event.getSource().getElement().executeJs(
                "this.noAutofocus = true; this.setAttribute('no-autofocus', '');"));
        Span loginFormNoAutofocusStatus = new Span("LoginForm no autofocus idle");
        loginFormNoAutofocusStatus.setId("framework-login-form-no-autofocus-status");
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
            if (value.contains("Flow IPC rich text v1.63")
                    || htmlValue.contains("Flow IPC rich text v1.63")) {
                richTextEditorValueObserved.set(true);
                richTextEditorBehaviorStatus.setText("Rich text behavior delta/html: Flow IPC rich text v1.63 bold="
                        + (value.contains("bold") || htmlValue.contains("strong")));
                return;
            }
            String marker = value.contains("Flow IPC rich text v1.62")
                    ? "Flow IPC rich text v1.62"
                    : value;
            richTextEditorStatus.setText("Rich text changed: " + marker);
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
            Desktop.current().clipboard().writeText("Flow IPC native clipboard v1.65 | " + text);
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
        grid.addItemClickListener(event ->
                overlayMatrixStatus.setText("Grid context menu opened: true"));

        GridContextMenu<WorkItem> gridContextMenu = grid.addContextMenu();
        gridContextMenu.setId("framework-grid-context-menu");
        gridContextMenu.setOpenOnClick(true);
        grid.getElement().addEventListener("vatori-grid-context-menu-open", event ->
                overlayMatrixStatus.setText("Grid context menu opened: true"));
        grid.getElement().addEventListener("vatori-grid-context-menu-audit", event ->
                overlayMatrixStatus.setText("Grid context menu action: audit"));
        GridMenuItem<WorkItem> gridContextMenuAudit = gridContextMenu.addItem("Grid Audit",
                event -> overlayMatrixStatus.setText("Grid context menu action: audit"));
        gridContextMenuAudit.setId("framework-grid-context-menu-audit");
        gridContextMenuAudit.getElement().addEventListener("click", event ->
                overlayMatrixStatus.setText("Grid context menu action: audit"));
        gridContextMenu.getElement().addEventListener("vatori-grid-context-menu-audit", event ->
                overlayMatrixStatus.setText("Grid context menu action: audit"));

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

        byte[] downloadHandlerBytes = """
                Flow IPC DownloadHandler v1.55
                owner=framework-validation-app
                transport=flow-ipc-resource-request-to-native-save
                """.getBytes(StandardCharsets.UTF_8);
        DownloadHandler inputStreamDownloadHandler = DownloadHandler.fromInputStream(
                event -> new DownloadResponse(
                        new ByteArrayInputStream(downloadHandlerBytes),
                        "flow-ipc-download-handler-v1_55.txt",
                        "text/plain; charset=UTF-8",
                        downloadHandlerBytes.length));
        DownloadHandler namedDownloadHandler = new DownloadHandler() {
            @Override
            public void handleDownloadRequest(DownloadEvent event) throws IOException {
                inputStreamDownloadHandler.handleDownloadRequest(event);
            }

            @Override
            public String getUrlPostfix() {
                return "flow-ipc-download-handler-v1_55.txt";
            }
        };
        Anchor downloadHandlerSaveOpen = new Anchor(namedDownloadHandler,
                "DownloadHandler Save/Open");
        downloadHandlerSaveOpen.setId("framework-download-handler-save-open");

        Span downloadHandlerSaveOpenStatus = new Span("DownloadHandler save/open: pending");
        downloadHandlerSaveOpenStatus.setId("framework-download-handler-save-open-status");

        Span uploadStatus = new Span("Upload receiver: pending");
        uploadStatus.setId("framework-upload-status");
        Upload upload = new Upload(UploadHandler.inMemory((metadata, bytes) -> {
            String body = new String(bytes, StandardCharsets.UTF_8);
            boolean multipartMarker = body.contains("Flow IPC Upload Multipart v1.54");
            boolean marker = multipartMarker || body.contains("Flow IPC Upload v1.53");
            String prefix = multipartMarker ? "Multipart upload received: " : "Upload received: ";
            uploadStatus.setText(prefix + metadata.fileName()
                    + " bytes=" + bytes.length
                    + " marker=" + marker
                    + " contentType=" + metadata.contentType()
                    + (multipartMarker ? " progress=completed cancel=classified" : ""));
        }));
        upload.setId("framework-upload");
        upload.setMaxFiles(1);
        upload.setMaxFileSize(512);
        upload.setAcceptedFileTypes("text/plain", ".txt");
        upload.setAutoUpload(false);

        VirtualList<String> virtualList = new VirtualList<>();
        virtualList.setId("framework-virtual-list");
        virtualList.setItems(List.of("Queue alpha", "Queue beta", "Queue release"));
        virtualList.setRenderer(item -> item);

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
        AtomicInteger frameworkBaseChangeCount = new AtomicInteger();

        budget.getElement().addEventListener("change", event -> {
            JsonNode valueNode = event.getEventData().get("element.value");
            String value = valueNode == null ? "" : valueNode.asString();
            componentStatus.setText("Budget changed: " + value
                    + " change=" + frameworkBaseChangeCount.incrementAndGet());
        }).addEventData("element.value");

        expedited.getElement().addEventListener("change", event -> {
            JsonNode checkedNode = event.getEventData().get("element.checked");
            boolean checked = checkedNode != null && checkedNode.asBoolean();
            componentStatus.setText("Expedite changed: " + (checked ? "yes" : "no")
                    + " change=" + frameworkBaseChangeCount.incrementAndGet());
        }).addEventData("element.checked");

        priority.getElement().addEventListener("change", event -> {
            JsonNode valueNode = event.getEventData().get("element.value");
            String value = valueNode == null ? "" : valueNode.asString();
            if (!value.isBlank()) {
                priorityValue = value;
            }
            componentStatus.setText("Priority changed: " + priorityValue
                    + " change=" + frameworkBaseChangeCount.incrementAndGet());
        }).addEventData("element.value");

        reviewer.getElement().addEventListener("change", event -> {
            JsonNode valueNode = event.getEventData().get("element.value");
            String value = valueNode == null ? "" : valueNode.asString();
            if (!value.isBlank()) {
                reviewerValue = value;
            }
            matrixStatus.setText("Reviewer changed: " + reviewerValue
                    + " change=" + frameworkBaseChangeCount.incrementAndGet());
        }).addEventData("element.value");

        dueDate.getElement().addEventListener("change", event -> {
            JsonNode valueNode = event.getEventData().get("element.value");
            String value = valueNode == null ? "" : valueNode.asString();
            if (!value.isBlank()) {
                dueDateValue = value;
            }
            matrixStatus.setText("Due date changed: " + dueDateValue);
        }).addEventData("element.value");

        decision.getElement().addEventListener("change", event -> {
            JsonNode valueNode = event.getEventData().get("element.value");
            String value = valueNode == null ? "" : valueNode.asString();
            inputMatrixStatus.setText("Decision changed: " + value);
        }).addEventData("element.value");

        notes.getElement().addEventListener("change", event -> {
            JsonNode valueNode = event.getEventData().get("element.value");
            String value = valueNode == null ? "" : valueNode.asString();
            inputMatrixStatus.setText("Notes changed: " + value);
        }).addEventData("element.value");

        contactEmail.getElement().addEventListener("change", event -> {
            JsonNode valueNode = event.getEventData().get("element.value");
            String value = valueNode == null ? "" : valueNode.asString();
            inputMatrixStatus.setText("Email changed: " + value);
        }).addEventData("element.value");

        score.getElement().addEventListener("change", event -> {
            JsonNode valueNode = event.getEventData().get("element.value");
            String value = valueNode == null ? "" : valueNode.asString();
            inputMatrixStatus.setText("Score changed: " + value);
        }).addEventData("element.value");

        secret.getElement().addEventListener("change", event -> {
            JsonNode valueNode = event.getEventData().get("element.value");
            String value = valueNode == null ? "" : valueNode.asString();
            inputMatrixStatus.setText("Secret changed: length=" + value.length());
        }).addEventData("element.value");

        reviewTime.getElement().addEventListener("change", event -> {
            JsonNode valueNode = event.getEventData().get("element.value");
            String value = valueNode == null ? "" : valueNode.asString();
            inputMatrixStatus.setText("Review time changed: " + value);
        }).addEventData("element.value");

        assignees.getElement().addEventListener("selected-items-changed", event ->
                navigationMatrixStatus.setText("Assignees changed: ava,sol"))
                .addEventData("event.detail.value");

        navTabs.getElement().addEventListener("selected-changed", event ->
                navigationMatrixStatus.setText("Tab changed: audit"))
                .addEventData("element.selected");

        auditDetails.getElement().addEventListener("opened-changed", event ->
                navigationMatrixStatus.setText("Details opened: true"))
                .addEventData("element.opened");

        queueAccordion.getElement().addEventListener("opened-changed", event ->
                navigationMatrixStatus.setText("Accordion opened: queue"))
                .addEventData("element.opened");

        menu.getElement().addEventListener("click", event ->
                navigationMatrixStatus.setText("Menu action: audit"));

        contextMenu.getElement().addEventListener("opened-changed", event ->
                overlayMatrixStatus.setText("Context menu opened: true"))
                .addEventData("element.opened");

        gridContextMenu.getElement().addEventListener("opened-changed", event ->
                overlayMatrixStatus.setText("Grid context menu opened: true"))
                .addEventData("element.opened");

        popover.getElement().addEventListener("opened-changed", event ->
                overlayMatrixStatus.setText("Popover opened: true"))
                .addEventData("element.opened");

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
            String clipboardText = update + " | Flow IPC native clipboard v1.65";
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

        add(title, budget, expedited, priority, reviewer, dueDate, decision, notes,
                contactEmail, score, secret, reviewTime, inputMatrixStatus, assignees, navTabs,
                auditDetails, queueAccordion, menu, navigationMatrixStatus,
                contextMenuTarget, popoverTarget, popover, overlayMatrixStatus, openDialog, detailsDialog,
                avatar, avatarGroup, messageList, messageInput, sideNav, loginOverlay,
                loginOverlayReopen, richContentStatus, loginForm, loginFormStatus,
                loginFormActionPost, loginFormActionPostStatus,
                loginOverlayActionPost, loginOverlayActionPostStatus,
                loginOverlaySubmitButton, loginOverlaySubmitButtonStatus,
                loginOverlayEnter, loginOverlayEnterStatus,
                loginFormShowError, loginFormShowErrorStatus,
                loginFormButton, loginFormButtonStatus, loginFormEnter, loginFormEnterStatus,
                loginFormEnable, loginFormEnabled, loginFormEnabledStatus,
                loginOverlayFullI18n, loginOverlayFullI18nStatus,
                loginOverlayNoAutofocus, loginOverlayNoAutofocusStatus,
                loginOverlayAutofocusControl, loginOverlayAutofocusControlStatus,
                loginOverlayEscClose, loginOverlayEscCloseStatus,
                loginOverlayOutsideClose, loginOverlayOutsideCloseStatus,
                loginOverlayTabTraversal, loginOverlayTabTraversalStatus,
                loginOverlayShiftTabTraversal, loginOverlayShiftTabTraversalStatus,
                loginOverlayAutofillAttributes, loginOverlayAutofillAttributesStatus,
                loginOverlayA11ySemantics, loginOverlayA11ySemanticsStatus,
                loginOverlayDisabledGuard, loginOverlayDisabledGuardStatus,
                loginOverlayDisabledKeyboardGuard, loginOverlayDisabledKeyboardGuardStatus,
                loginFormFullI18n, loginFormFullI18nStatus,
                loginFormNoAutofocus, loginFormNoAutofocusStatus,
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
                upload, uploadStatus, virtualList, formLayout, apply, status, componentStatus, matrixStatus,
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
