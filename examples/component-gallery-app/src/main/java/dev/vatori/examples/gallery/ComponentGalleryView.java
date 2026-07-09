package dev.vatori.examples.gallery;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.avatar.AvatarGroup.AvatarGroupItem;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginOverlay;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.richtexteditor.RichTextEditor;
import com.vaadin.flow.component.richtexteditor.RichTextEditorVariant;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.UploadHandler;
import dev.vatori.desktop.Desktop;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

@Route("")
public class ComponentGalleryView extends VerticalLayout {
    private final Span status = new Span("Ready");
    private int revision;

    public ComponentGalleryView() {
        setId("component-gallery-root");
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle().set("background", "var(--lumo-contrast-5pct)");

        HorizontalLayout header = createHeader();
        HorizontalLayout body = new HorizontalLayout();
        body.setSizeFull();
        body.setSpacing(false);
        body.setPadding(false);

        VerticalLayout nav = createNavigation();
        VerticalLayout content = createContent();
        Scroller scroller = new Scroller(content);
        scroller.setId("gallery-scroller");
        scroller.setSizeFull();

        body.add(nav, scroller);
        body.expand(scroller);
        add(header, body);
        expand(body);
    }

    private HorizontalLayout createHeader() {
        H1 title = new H1("Component Gallery");
        title.getStyle()
                .set("font-size", "var(--lumo-font-size-xl)")
                .set("line-height", "1")
                .set("margin", "0");

        Button ping = new Button("Notify", VaadinIcon.BELL.create(), event -> {
            revision++;
            status.setText("Native notification sent " + revision);
            Desktop.current().notifications().show("Component Gallery", "Gallery notification " + revision);
        });

        Button titleButton = new Button("Window Title", VaadinIcon.DESKTOP.create(), event -> {
            revision++;
            String text = "Component Gallery - revision " + revision;
            status.setText(text);
            Desktop.current().setWindowTitle(text);
        });

        HorizontalLayout actions = new HorizontalLayout(ping, titleButton);
        actions.setAlignItems(Alignment.CENTER);
        actions.setSpacing(true);

        HorizontalLayout header = new HorizontalLayout(title, status, actions);
        header.setId("gallery-header");
        header.setWidthFull();
        header.setPadding(true);
        header.setSpacing(true);
        header.setAlignItems(Alignment.CENTER);
        header.expand(status);
        header.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)");
        return header;
    }

    private VerticalLayout createNavigation() {
        VerticalLayout nav = new VerticalLayout();
        nav.setId("gallery-navigation");
        nav.setWidth("230px");
        nav.setHeightFull();
        nav.setPadding(true);
        nav.setSpacing(false);
        nav.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-right", "1px solid var(--lumo-contrast-10pct)")
                .set("overflow", "auto");

        nav.add(navButton("Inputs", "inputs", VaadinIcon.EDIT),
                navButton("Choices", "choices", VaadinIcon.CHECK_SQUARE_O),
                navButton("Data", "data", VaadinIcon.TABLE),
                navButton("Feedback", "feedback", VaadinIcon.COMMENT),
                navButton("Navigation", "navigation", VaadinIcon.MENU),
                navButton("Layouts", "layouts", VaadinIcon.SPLIT),
                navButton("Content", "content", VaadinIcon.FILE_TEXT),
                navButton("Transfer", "transfer", VaadinIcon.UPLOAD),
                navButton("Deferred", "deferred", VaadinIcon.ARCHIVE));
        return nav;
    }

    private Button navButton(String label, String targetId, VaadinIcon icon) {
        Button button = new Button(label, icon.create(), event -> scrollTo(targetId));
        button.setWidthFull();
        button.getStyle()
                .set("justify-content", "flex-start")
                .set("margin-bottom", "var(--lumo-space-xs)");
        return button;
    }

    private VerticalLayout createContent() {
        VerticalLayout content = new VerticalLayout();
        content.setId("gallery-content");
        content.setWidthFull();
        content.setPadding(false);
        content.setSpacing(false);
        content.getStyle().set("max-width", "1180px");
        content.add(section("inputs", "Inputs", "Text and value components for common desktop forms.",
                        sample("Text fields", textFields()),
                        sample("Date and time", dateAndTimeFields()),
                        sample("Numeric controls", numericFields())),
                section("choices", "Choices", "Single and multi-value selectors.",
                        sample("Boolean and radio", booleanChoices()),
                        sample("Dropdown selectors", dropdownChoices()),
                        sample("List Box", listChoices())),
                section("data", "Data", "Tables, hierarchy and large-list surfaces.",
                        sample("Grid", ordersGrid()),
                        sample("Tree Grid", projectTree()),
                        sample("Virtual List", peopleList()),
                        sample("Identity and status", identitySamples())),
                section("feedback", "Feedback", "Transient messages and overlay interactions.",
                        sample("Dialog", dialogSample()),
                        sample("Confirm Dialog", confirmDialogSample()),
                        sample("Notification, Tooltip, Popover", lightweightOverlaySample()),
                        sample("Progress", progressSample())),
                section("navigation", "Navigation", "Local action surfaces and disclosure controls.",
                        sample("Tabs", tabsSample()),
                        sample("Menu Bar and Context Menu", menuSamples()),
                        sample("Side Navigation", sideNavSample()),
                        sample("Details and Accordion", disclosureSamples())),
                section("layouts", "Layouts", "Flow layout primitives inside a desktop window.",
                        sample("Form Layout", formLayoutSample()),
                        sample("Split Layout", splitLayoutSample()),
                        sample("Card", cardSample()),
                        sample("Login", loginSample())),
                section("content", "Content", "Text, messages and rendered content.",
                        sample("Markdown", markdownSample()),
                        sample("Rich Text Editor", richTextEditorSample()),
                        sample("Messages", messagesSample()),
                        sample("Icons and HTML", htmlElementsSample())),
                section("transfer", "Transfer", "Local upload and generated download resources.",
                        sample("Upload", uploadSample()),
                        sample("Download", downloadSample())),
                section("deferred", "Deferred / Advanced", "Components kept out of the first zero-config gallery.",
                        deferredComponents()));
        return content;
    }

    private Div section(String id, String title, String subtitle, Component... children) {
        Div section = new Div();
        section.setId(id);
        section.setWidthFull();
        section.getStyle()
                .set("box-sizing", "border-box")
                .set("padding", "var(--lumo-space-xl)")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)");

        H2 heading = new H2(title);
        heading.getStyle()
                .set("font-size", "var(--lumo-font-size-xl)")
                .set("margin", "0 0 var(--lumo-space-xs)");

        Paragraph note = new Paragraph(subtitle);
        note.getStyle()
                .set("margin", "0 0 var(--lumo-space-m)")
                .set("color", "var(--lumo-secondary-text-color)");

        Div grid = new Div();
        grid.setWidthFull();
        grid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(280px, 1fr))")
                .set("gap", "var(--lumo-space-m)")
                .set("align-items", "stretch");
        grid.add(children);

        section.add(heading, note, grid);
        return section;
    }

    private Div sample(String title, Component... components) {
        Div sample = new Div();
        sample.getStyle()
                .set("box-sizing", "border-box")
                .set("min-height", "180px")
                .set("padding", "var(--lumo-space-m)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "8px")
                .set("background", "var(--lumo-base-color)");

        H3 heading = new H3(title);
        heading.getStyle()
                .set("font-size", "var(--lumo-font-size-m)")
                .set("margin", "0 0 var(--lumo-space-s)");

        VerticalLayout stack = new VerticalLayout(components);
        stack.setPadding(false);
        stack.setSpacing(true);
        stack.setWidthFull();
        sample.add(heading, stack);
        return sample;
    }

    private Component[] textFields() {
        TextField name = new TextField("Text Field");
        name.setValue("vatori");
        name.setWidthFull();

        TextArea notes = new TextArea("Text Area");
        notes.setValue("Local desktop windows need compact, inspectable Flow views.");
        notes.setWidthFull();
        notes.setMinHeight("86px");

        EmailField email = new EmailField("Email Field");
        email.setValue("owner@example.test");
        email.setWidthFull();

        PasswordField password = new PasswordField("Password Field");
        password.setValue("desktop");
        password.setWidthFull();
        return new Component[]{name, notes, email, password};
    }

    private Component[] dateAndTimeFields() {
        DatePicker date = new DatePicker("Date Picker");
        date.setValue(LocalDate.of(2026, 6, 4));
        date.setWidthFull();

        DateTimePicker dateTime = new DateTimePicker("Date Time Picker");
        dateTime.setValue(LocalDateTime.of(2026, 6, 4, 9, 30));
        dateTime.setWidthFull();

        TimePicker time = new TimePicker("Time Picker");
        time.setValue(LocalTime.of(10, 15));
        time.setWidthFull();
        return new Component[]{date, dateTime, time};
    }

    private Component[] numericFields() {
        IntegerField integer = new IntegerField("Integer Field");
        integer.setValue(7);
        integer.setMin(0);
        integer.setMax(10);
        integer.setWidthFull();

        NumberField number = new NumberField("Number Field");
        number.setValue(42.5);
        number.setWidthFull();

        BigDecimalField decimal = new BigDecimalField("BigDecimal Field");
        decimal.setValue(new BigDecimal("1234.56"));
        decimal.setWidthFull();
        return new Component[]{integer, number, decimal};
    }

    private Component[] booleanChoices() {
        Checkbox checkbox = new Checkbox("Checkbox");
        checkbox.setValue(true);

        CheckboxGroup<String> group = new CheckboxGroup<>("Checkbox Group");
        group.setItems("Flow", "Desktop", "IPC");
        group.select("Flow", "Desktop");

        RadioButtonGroup<String> radio = new RadioButtonGroup<>("Radio Button Group");
        radio.setItems("Compact", "Comfortable", "Dense");
        radio.setValue("Comfortable");
        return new Component[]{checkbox, group, radio};
    }

    private Component[] dropdownChoices() {
        Select<String> select = new Select<>();
        select.setLabel("Select");
        select.setItems("Preview", "Audit", "Release");
        select.setValue("Audit");
        select.setWidthFull();

        ComboBox<String> comboBox = new ComboBox<>("Combo Box");
        comboBox.setItems("macOS", "Windows", "Linux");
        comboBox.setValue("macOS");
        comboBox.setWidthFull();

        MultiSelectComboBox<String> multi = new MultiSelectComboBox<>("Multi-Select Combo Box");
        multi.setItems("Button", "Grid", "Dialog", "Upload");
        multi.setValue(Set.of("Button", "Grid"));
        multi.setWidthFull();
        return new Component[]{select, comboBox, multi};
    }

    private Component[] listChoices() {
        ListBox<String> listBox = new ListBox<>();
        listBox.setItems("Overview", "Forms", "Data", "Transfers");
        listBox.setValue("Forms");
        listBox.setHeight("120px");

        return new Component[]{listBox};
    }

    private Component ordersGrid() {
        Grid<OrderRow> grid = new Grid<>(OrderRow.class, false);
        grid.addColumn(OrderRow::id).setHeader("ID").setWidth("70px");
        grid.addColumn(OrderRow::customer).setHeader("Customer");
        grid.addColumn(OrderRow::status).setHeader("Status");
        grid.setItems(IntStream.rangeClosed(1, 8)
                .mapToObj(index -> new OrderRow(index, "Customer " + index, index % 2 == 0 ? "Ready" : "Queued"))
                .toList());
        grid.setAllRowsVisible(true);
        grid.setWidthFull();
        return grid;
    }

    private Component projectTree() {
        TreeGrid<ProjectNode> tree = new TreeGrid<>();
        tree.addHierarchyColumn(ProjectNode::name).setHeader("Work");
        tree.addColumn(ProjectNode::owner).setHeader("Owner");
        tree.setItems(projectNodes(), ProjectNode::children);
        tree.setAllRowsVisible(true);
        tree.setWidthFull();
        return tree;
    }

    private Component peopleList() {
        VirtualList<String> list = new VirtualList<>();
        list.setItems(IntStream.rangeClosed(1, 40).mapToObj(index -> "Queue item " + index).toList());
        list.setRenderer(item -> item);
        list.setHeight("170px");
        list.setWidthFull();
        return list;
    }

    private Component[] identitySamples() {
        Avatar avatar = new Avatar("Ava Platform");
        avatar.setAbbreviation("AP");
        avatar.setColorIndex(5);

        AvatarGroup group = new AvatarGroup(
                avatarGroupItem("Ava Platform", "AP", 5),
                avatarGroupItem("Kai IPC", "KI", 6),
                avatarGroupItem("Sol Host", "SH", 7));
        group.setMaxItemsVisible(2);

        Span ready = new Span("Ready");
        ready.getElement().getThemeList().add("badge success");

        Span review = new Span("Review");
        review.getElement().getThemeList().add("badge contrast");

        Icon icon = VaadinIcon.CHECK_CIRCLE.create();
        icon.setColor("var(--lumo-success-color)");

        HorizontalLayout row = new HorizontalLayout(avatar, group, ready, review, icon);
        row.setAlignItems(Alignment.CENTER);
        return new Component[]{row};
    }

    private Component dialogSample() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Dialog");
        dialog.add(new Paragraph("Overlay content rendered by the local desktop WebView."));
        Button close = new Button("Close", event -> dialog.close());
        dialog.getFooter().add(close);

        Button open = new Button("Open Dialog", event -> {
            status.setText("Dialog opened");
            dialog.open();
        });
        open.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        return new Div(open, dialog);
    }

    private Component confirmDialogSample() {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Confirm Dialog");
        confirm.setText("Apply the sample action?");
        confirm.setCancelable(true);
        confirm.setConfirmText("Apply");
        confirm.addConfirmListener(event -> status.setText("Confirm action applied"));

        Button open = new Button("Open Confirm Dialog", event -> confirm.open());
        return new Div(open, confirm);
    }

    private Component[] lightweightOverlaySample() {
        Button notify = new Button("Show Notification", event -> {
            Notification.show("Notification from Component Gallery", 2400, Notification.Position.TOP_END);
            status.setText("Notification shown");
        });

        Button help = new Button("Tooltip Target", VaadinIcon.QUESTION_CIRCLE.create());
        help.setTooltipText("Tooltip");

        Button popoverTarget = new Button("Popover Target");
        Popover popover = new Popover(new Span("Popover content"));
        popover.setTarget(popoverTarget);
        popover.setOpenOnClick(true);

        return new Component[]{notify, help, popoverTarget, popover};
    }

    private Component progressSample() {
        ProgressBar progress = new ProgressBar();
        progress.setValue(0.45);
        progress.setWidthFull();

        Button advance = new Button("Advance", event -> {
            double next = progress.getValue() >= 1.0 ? 0.1 : progress.getValue() + 0.15;
            progress.setValue(next);
            status.setText("Progress " + Math.round(next * 100) + "%");
        });
        return new VerticalLayout(progress, advance);
    }

    private Component tabsSample() {
        Span content = new Span("Overview selected");
        Tabs tabs = new Tabs(new Tab("Overview"), new Tab("Events"), new Tab("Resources"));
        tabs.addSelectedChangeListener(event -> content.setText(event.getSelectedTab().getLabel() + " selected"));
        tabs.setWidthFull();
        return new VerticalLayout(tabs, content);
    }

    private Component[] menuSamples() {
        MenuBar menuBar = new MenuBar();
        menuBar.addItem("Refresh", event -> status.setText("Menu refresh"));
        menuBar.addItem("Export", event -> status.setText("Menu export"));

        Button contextTarget = new Button("Context Menu Target");
        ContextMenu contextMenu = new ContextMenu(contextTarget);
        contextMenu.setOpenOnClick(true);
        contextMenu.addItem("Inspect", event -> status.setText("Context inspect"));
        contextMenu.addItem("Reset", event -> status.setText("Context reset"));

        Grid<OrderRow> contextGrid = new Grid<>(OrderRow.class, false);
        contextGrid.addColumn(OrderRow::id).setHeader("ID").setWidth("70px");
        contextGrid.addColumn(OrderRow::customer).setHeader("Customer");
        contextGrid.setItems(List.of(
                new OrderRow(201, "Grid Context", "Ready"),
                new OrderRow(202, "Right click or open", "Queued")));
        contextGrid.setAllRowsVisible(true);
        contextGrid.setWidthFull();

        GridContextMenu<OrderRow> gridMenu = new GridContextMenu<>(contextGrid);
        gridMenu.addItem("Open Row", event -> event.getItem()
                .ifPresent(item -> status.setText("Grid context row " + item.id())));
        gridMenu.addItem("Mark Ready", event -> status.setText("Grid context action"));

        return new Component[]{menuBar, contextTarget, contextGrid};
    }

    private Component sideNavSample() {
        SideNav sideNav = new SideNav("Workspace");
        sideNav.setCollapsible(true);
        sideNav.setExpanded(true);

        SideNavItem overview = new SideNavItem("Overview", "#inputs", VaadinIcon.DASHBOARD.create());
        SideNavItem data = new SideNavItem("Data", "#data", VaadinIcon.TABLE.create());
        SideNavItem transfer = new SideNavItem("Transfer", "#transfer", VaadinIcon.UPLOAD.create());
        overview.getElement().addEventListener("click", event -> status.setText("SideNav overview"));
        data.getElement().addEventListener("click", event -> status.setText("SideNav data"));
        transfer.getElement().addEventListener("click", event -> status.setText("SideNav transfer"));

        sideNav.addItem(overview, data, transfer);
        sideNav.setWidthFull();
        return sideNav;
    }

    private Component[] disclosureSamples() {
        Details details = new Details("Details", new Span("Details content"));
        details.setOpened(true);

        Accordion accordion = new Accordion();
        accordion.add("Queue", new Span("Queue content"));
        accordion.add("History", new Span("History content"));
        return new Component[]{details, accordion};
    }

    private Component formLayoutSample() {
        TextField firstName = new TextField("First name");
        TextField lastName = new TextField("Last name");
        EmailField email = new EmailField("Email");

        FormLayout layout = new FormLayout(firstName, lastName, email);
        layout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("420px", 2));
        return layout;
    }

    private Component splitLayoutSample() {
        Div master = panel("Master", "Issue A-100\nIssue A-101\nIssue A-102");
        Div detail = panel("Detail", "Selected issue metadata");
        SplitLayout split = new SplitLayout(master, detail);
        split.setSplitterPosition(38);
        split.setHeight("160px");
        split.setWidthFull();
        return split;
    }

    private Component cardSample() {
        Card card = new Card();
        card.setTitle("Card");
        card.setSubtitle("Grouped content");
        card.add(new Span("Status, metadata and actions can live here."));
        card.addToFooter(new Button("Action", event -> status.setText("Card action")));
        return card;
    }

    private Component loginSample() {
        LoginI18n i18n = LoginI18n.createDefault();
        i18n.getForm().setTitle("Desktop session");
        i18n.getForm().setUsername("User");
        i18n.getForm().setPassword("Password");
        i18n.getForm().setSubmit("Sign in");

        LoginForm login = new LoginForm();
        login.setI18n(i18n);
        login.setEnabled(false);

        LoginOverlay overlay = new LoginOverlay(i18n);
        overlay.setTitle("vatori");
        overlay.setDescription("Local desktop session");
        overlay.addLoginListener(event -> {
            status.setText("Login overlay submitted: " + event.getUsername());
            overlay.close();
        });
        Button openOverlay = new Button("Open Login Overlay", event -> overlay.setOpened(true));
        return new VerticalLayout(login, openOverlay, overlay);
    }

    private Component markdownSample() {
        Markdown markdown = new Markdown("""
                ### Markdown
                - Flow component
                - Rendered inside the desktop window
                """);
        markdown.setWidthFull();
        return markdown;
    }

    private Component richTextEditorSample() {
        RichTextEditor editor = new RichTextEditor();
        editor.addThemeVariants(RichTextEditorVariant.LUMO_COMPACT);
        editor.asHtml().setValue("""
                <p><strong>Rich text</strong> is now shown as a real Flow component.</p>
                <p>Use this surface to inspect editor toolbar and content rendering.</p>
                """);
        editor.setHeight("220px");
        editor.setWidthFull();
        editor.addValueChangeListener(event -> status.setText("Rich text updated"));
        return editor;
    }

    private Component messagesSample() {
        List<MessageListItem> items = new ArrayList<>();
        items.add(new MessageListItem("Gallery loaded", Instant.now(), "Ava"));
        items.add(new MessageListItem("Component pass ready", Instant.now(), "Sol"));

        MessageList list = new MessageList(items);
        list.setHeight("160px");
        list.setWidthFull();

        MessageInput input = new MessageInput(event -> {
            list.addItem(new MessageListItem(event.getValue(), Instant.now(), "You"));
            status.setText("Message submitted");
        });
        return new VerticalLayout(list, input);
    }

    private Component htmlElementsSample() {
        UnorderedList list = new UnorderedList(
                new ListItem(new Span("Span")),
                new ListItem(new Text("Text node")),
                new ListItem(VaadinIcon.CODE.create(), new Text(" Icon")));
        return list;
    }

    private Component uploadSample() {
        Span uploadStatus = new Span("No file uploaded");
        Upload upload = new Upload(UploadHandler.inMemory((metadata, bytes) ->
                uploadStatus.setText(metadata.fileName() + " - " + bytes.length + " bytes")));
        upload.setMaxFiles(1);
        upload.setMaxFileSize(1024 * 1024);
        upload.setAcceptedFileTypes("text/plain", ".txt", ".md");
        upload.setAutoUpload(false);
        upload.setWidthFull();
        return new VerticalLayout(upload, uploadStatus);
    }

    private Component downloadSample() {
        byte[] bytes = """
                vatori Component Gallery
                Generated DownloadHandler response
                """.getBytes(StandardCharsets.UTF_8);
        DownloadHandler handler = DownloadHandler.fromInputStream(event -> new DownloadResponse(
                new ByteArrayInputStream(bytes),
                "component-gallery.txt",
                "text/plain; charset=UTF-8",
                bytes.length));

        Anchor anchor = new Anchor(handler, "Download Text File");
        anchor.setDownload(true);
        return anchor;
    }

    private Component[] deferredComponents() {
        return new Component[]{
                placeholder("App Layout", "Root shell sample"),
                placeholder("Auto CRUD", "Data endpoint sample"),
                placeholder("Auto Grid", "Data endpoint sample"),
                placeholder("Auto Form", "Data endpoint sample"),
                placeholder("Charts", "Additional dependency"),
                placeholder("CRUD", "Editor workflow"),
                placeholder("Custom Field", "Composite field sample"),
                placeholder("Dashboard", "Dashboard state model"),
                placeholder("Grid Pro", "Inline editing dependency"),
                placeholder("Map", "Map provider setup"),
                placeholder("Master-Detail Layout", "Responsive detail shell"),
                placeholder("Slider", "Vaadin feature flag required"),
                placeholder("Spreadsheet", "Spreadsheet dependency")
        };
    }

    private Component placeholder(String name, String note) {
        Div placeholder = new Div();
        placeholder.getStyle()
                .set("box-sizing", "border-box")
                .set("min-height", "118px")
                .set("padding", "var(--lumo-space-m)")
                .set("border", "1px dashed var(--lumo-contrast-30pct)")
                .set("border-radius", "8px")
                .set("background", "var(--lumo-contrast-5pct)");

        Span componentName = new Span(name);
        componentName.getStyle()
                .set("display", "block")
                .set("font-weight", "600")
                .set("margin-bottom", "var(--lumo-space-xs)");

        Span state = new Span("尚未实现");
        state.getElement().getThemeList().add("badge contrast");

        Paragraph detail = new Paragraph(note);
        detail.getStyle()
                .set("margin", "var(--lumo-space-s) 0 0")
                .set("color", "var(--lumo-secondary-text-color)");

        placeholder.add(componentName, state, detail);
        return placeholder;
    }

    private Div panel(String title, String body) {
        Div panel = new Div();
        panel.getStyle()
                .set("box-sizing", "border-box")
                .set("height", "100%")
                .set("padding", "var(--lumo-space-s)")
                .set("white-space", "pre-line")
                .set("background", "var(--lumo-contrast-5pct)");
        panel.add(new Span(title), new Paragraph(body));
        return panel;
    }

    private void scrollTo(String id) {
        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "document.getElementById($0)?.scrollIntoView({behavior:'smooth',block:'start'})", id));
    }

    private List<ProjectNode> projectNodes() {
        return List.of(new ProjectNode("vatori", "platform", List.of(
                        new ProjectNode("Flow compatibility", "flow", List.of()),
                        new ProjectNode("Native host", "host", List.of()))),
                new ProjectNode("Examples", "developer", List.of(
                new ProjectNode("Counter", "developer", List.of()),
                new ProjectNode("Component Gallery", "developer", List.of()))));
    }

    private AvatarGroupItem avatarGroupItem(String name, String abbreviation, int colorIndex) {
        AvatarGroupItem item = new AvatarGroupItem(name, abbreviation);
        item.setColorIndex(colorIndex);
        return item;
    }

    private record OrderRow(int id, String customer, String status) {
    }

    private record ProjectNode(String name, String owner, List<ProjectNode> children) {
    }
}
