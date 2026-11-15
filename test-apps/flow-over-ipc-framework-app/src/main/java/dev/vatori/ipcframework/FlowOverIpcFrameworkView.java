package dev.vatori.ipcframework;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.avatar.AvatarGroup.AvatarGroupItem;
import com.vaadin.flow.component.avatar.AvatarGroupVariant;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
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
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.card.CardVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DropEffect;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.dnd.EffectAllowed;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.masterdetaillayout.MasterDetailLayout;
import com.vaadin.flow.component.masterdetaillayout.MasterDetailLayout.Containment;
import com.vaadin.flow.component.masterdetaillayout.MasterDetailLayout.Orientation;
import com.vaadin.flow.component.masterdetaillayout.MasterDetailLayout.OverlayMode;
import com.vaadin.flow.component.masterdetaillayout.MasterDetailLayoutVariant;
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
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.richtexteditor.RichTextEditor;
import com.vaadin.flow.component.richtexteditor.RichTextEditorVariant;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.select.SelectVariant;
import com.vaadin.flow.component.slider.Slider;
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
import com.vaadin.flow.component.internal.AllowInert;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.provider.hierarchy.AbstractBackEndHierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.DisabledUpdateMode;
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
import com.vaadin.flow.component.dependency.JsModule;
import dev.vatori.desktop.Desktop;
import dev.vatori.desktop.DesktopCommandResult;
import dev.vatori.desktop.DesktopWindow;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Route("")
@JsModule("./vatori-vite-hmr-probe.js")
public class FlowOverIpcFrameworkView extends VerticalLayout {
    private static final AtomicReference<dev.vatori.desktop.DesktopCapabilityContext>
            RETAINED_RUNTIME_CONTEXT = new AtomicReference<>();

    private static boolean retiredRuntimeResult(dev.vatori.desktop.DesktopCommandResult result,
            String command) {
        return !result.success()
                && Boolean.TRUE.equals(result.payload().get("canceled"))
                && Boolean.TRUE.equals(result.payload().get("operationInvalidated"))
                && "owner-window-closed".equals(result.payload().get("cancelReason"))
                && command.equals(result.payload().get("command"));
    }
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
        Html viteHmrProbe = new Html(
                "<vatori-vite-hmr-probe id=\"framework-vite-hmr-probe\"></vatori-vite-hmr-probe>");
        JavaReloadProbe javaReloadProbe = new JavaReloadProbe();
        ReactorReloadProbe reactorReloadProbe = new ReactorReloadProbe();
        EffectiveMavenProfileProbe effectiveMavenProfileProbe =
                new EffectiveMavenProfileProbe();
        FlowRpcProbe flowRpcProbe = new FlowRpcProbe();
        Span runtimeStatus = new Span("not-queried");
        runtimeStatus.setId("framework-runtime-replacement-status");
        Span runtimeCaptured = new Span("not-captured");
        runtimeCaptured.setId("framework-runtime-captured");
        Button queryRuntimeStatus = new Button("Query runtime replacement status", event -> {
            runtimeStatus.setText("querying");
            var ui = event.getSource().getUI().orElseThrow();
            var capabilities = Desktop.current().captureCapabilityContext();
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                var result = capabilities.runtime().replacementStatus();
                boolean retained = result.success() && "replaced".equals(result.payload().get("status"));
                if (retained) RETAINED_RUNTIME_CONTEXT.set(capabilities);
                ui.access(() -> {
                    runtimeStatus.setText(result.success()
                            ? String.valueOf(result.payload().get("status")) : "query-failed");
                    if (retained) runtimeCaptured.setText("captured");
                });
            });
        });
        queryRuntimeStatus.setId("framework-runtime-replacement-query");
        Span runtimeRequestResult = new Span("not-requested");
        runtimeRequestResult.setId("framework-runtime-replacement-request-result");
        Button replaceRuntime = new Button("Replace runtime with declared next candidate", event -> {
            var ui = event.getSource().getUI().orElseThrow();
            var capabilities = Desktop.current().captureCapabilityContext();
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                var result = capabilities.runtime().requestReplacement("framework-product-request", "next");
                ui.access(() -> runtimeRequestResult.setText(String.valueOf(result.payload().get(
                        result.success() ? "status" : "errorCode"))));
            });
        });
        replaceRuntime.setId("framework-runtime-replacement-request");
        Button burstRuntime = new Button("Concurrent runtime replacement requests", event -> {
            var capabilities = Desktop.current().captureCapabilityContext();
            var barrier = new java.util.concurrent.CyclicBarrier(4);
            var workers = java.util.concurrent.Executors.newFixedThreadPool(4);
            for (int i = 0; i < 4; i++) {
                String requestId = "framework-burst-" + (i % 2);
                workers.submit(() -> {
                    try {
                        barrier.await(5, java.util.concurrent.TimeUnit.SECONDS);
                        capabilities.runtime().requestReplacement(requestId, "next");
                    } catch (Exception failure) {
                        System.err.println("Runtime request burst failed: " + failure);
                    }
                });
            }
            workers.shutdown();
        });
        burstRuntime.setId("framework-runtime-replacement-burst");
        Button openRuntimeObserver = new Button("Open independent runtime observer", event ->
                Desktop.current().windows().open(DesktopWindow.builder("runtime-observer")
                        .title("Runtime status observer").size(1000, 760).route("").build()));
        openRuntimeObserver.setId("framework-runtime-observer-open");
        Button closeRuntimeObserver = new Button("Close runtime observer", event ->
                Desktop.current().windows().close("runtime-observer"));
        closeRuntimeObserver.setId("framework-runtime-observer-close");
        Span staleRuntimeStatus = new Span("not-probed");
        staleRuntimeStatus.setId("framework-runtime-stale-status");
        Button probeStaleRuntime = new Button("Probe retired runtime context", event -> {
            var retained = RETAINED_RUNTIME_CONTEXT.get();
            var ui = event.getSource().getUI().orElseThrow();
            boolean fromClient = event.isFromClient();
            staleRuntimeStatus.setText("probing");
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                boolean background = com.vaadin.flow.server.VaadinSession.getCurrent() == null;
                if (retained == null) {
                    ui.access(() -> staleRuntimeStatus.setText("missing-context"));
                    return;
                }
                var query = retained.runtime().replacementStatus();
                var request = retained.runtime().requestReplacement("stale-owner-probe", "next");
                String result = "query=" + retiredRuntimeResult(query, "runtime.replacement.status")
                        + " request=" + retiredRuntimeResult(request, "runtime.replacement.request")
                        + " background=" + background + " client=" + fromClient;
                ui.access(() -> staleRuntimeStatus.setText(result));
            });
        });
        probeStaleRuntime.setId("framework-runtime-stale-probe");
        add(queryRuntimeStatus, runtimeStatus, replaceRuntime, runtimeRequestResult, burstRuntime,
                openRuntimeObserver, closeRuntimeObserver, runtimeCaptured, probeStaleRuntime, staleRuntimeStatus);
        Button openLazyFrontend = new Button("Open lazy frontend route", event ->
                event.getSource().getUI()
                        .ifPresent(ui -> ui.navigate(LazyFrontendRoute.class)));
        openLazyFrontend.setId("framework-lazy-frontend-open");
        Button openNestedLazyFrontend = new Button("Open nested lazy frontend route", event ->
                event.getSource().getUI()
                        .ifPresent(ui -> ui.navigate(NestedLazyFrontendRoute.class)));
        openNestedLazyFrontend.setId("framework-nested-lazy-open");
        Button openJarFrontend = new Button(
                "Open dependency JAR frontend route", event ->
                event.getSource().getUI()
                        .ifPresent(ui ->
                                ui.navigate(JarFrontendResourceRoute.class)));
        openJarFrontend.setId("framework-jar-frontend-open");
        Button openFlowDnd = new Button("Open Flow DnD route", event ->
                event.getSource().getUI()
                        .ifPresent(ui -> ui.navigate(FlowDndRoute.class)));
        openFlowDnd.setId("framework-flow-dnd-open");
        Button openFlowOverlayStack = new Button(
                "Open Flow overlay stack route", event ->
                event.getSource().getUI()
                        .ifPresent(ui ->
                                ui.navigate(FlowOverlayStackRoute.class)));
        openFlowOverlayStack.setId("framework-flow-overlay-stack-open-route");
        Button openFlowInputPopup = new Button(
                "Open Flow input popup route", event ->
                event.getSource().getUI()
                        .ifPresent(ui ->
                                ui.navigate(FlowInputPopupRoute.class)));
        openFlowInputPopup.setId("framework-flow-input-popup-open-route");
        Button openFlowMenuPopup = new Button(
                "Open Flow menu popup route", event ->
                event.getSource().getUI()
                        .ifPresent(ui ->
                                ui.navigate(FlowMenuPopupRoute.class)));
        openFlowMenuPopup.setId("framework-flow-menu-popup-open-route");
        Button openFlowLargeDownload = new Button(
                "Open Flow large download route", event ->
                event.getSource().getUI()
                        .ifPresent(ui ->
                                ui.navigate(FlowLargeDownloadRoute.class)));
        openFlowLargeDownload.setId(
                "framework-flow-large-download-open-route");
        Button openFlowLargeUpload = new Button(
                "Open Flow large upload route", event ->
                event.getSource().getUI()
                        .ifPresent(ui ->
                                ui.navigate(FlowLargeUploadRoute.class)));
        openFlowLargeUpload.setId(
                "framework-flow-large-upload-open-route");
        Button openFlowInFlightRequest = new Button(
                "Open Flow in-flight request route", event ->
                event.getSource().getUI()
                        .ifPresent(ui ->
                                ui.navigate(FlowInFlightRequestRoute.class)));
        openFlowInFlightRequest.setId(
                "framework-flow-in-flight-request-open-route");
        Button openFlowLargeMultipartUpload = new Button(
                "Open Flow large multipart upload route", event ->
                event.getSource().getUI()
                        .ifPresent(ui ->
                                ui.navigate(
                                        FlowLargeMultipartUploadRoute.class)));
        openFlowLargeMultipartUpload.setId(
                "framework-flow-large-multipart-upload-open-route");
        Button openFlowConcurrentUpload = new Button(
                "Open Flow concurrent upload route", event ->
                event.getSource().getUI()
                        .ifPresent(ui ->
                                ui.navigate(FlowConcurrentUploadRoute.class)));
        openFlowConcurrentUpload.setId(
                "framework-flow-concurrent-upload-open-route");
        Button openFlowRequestFanout = new Button(
                "Open Flow request fan-out route", event ->
                event.getSource().getUI()
                        .ifPresent(ui ->
                                ui.navigate(FlowRequestFanoutRoute.class)));
        openFlowRequestFanout.setId(
                "framework-flow-request-fanout-open-route");
        Button openFlowConcurrentMultipartUpload = new Button(
                "Open Flow concurrent multipart upload route", event ->
                event.getSource().getUI()
                        .ifPresent(ui -> ui.navigate(
                                FlowConcurrentMultipartUploadRoute.class)));
        openFlowConcurrentMultipartUpload.setId(
                "framework-flow-concurrent-multipart-upload-open-route");
        Button openFlowCrossSessionMultipartUpload = new Button(
                "Open Flow cross-session multipart upload route", event ->
                event.getSource().getUI()
                        .ifPresent(ui -> ui.navigate(
                                FlowCrossSessionMultipartUploadRoute.class)));
        openFlowCrossSessionMultipartUpload.setId(
                "framework-flow-cross-session-multipart-upload-open-route");
        Button openFlowCapabilityContextReopen = new Button(
                "Open Flow capability context reopen route", event ->
                event.getSource().getUI()
                        .ifPresent(ui -> ui.navigate(
                                FlowCapabilityContextReopenRoute.class)));
        openFlowCapabilityContextReopen.setId(
                "framework-flow-capability-context-reopen-open-route");
        Button openFlowCapabilityPermission = new Button(
                "Open Flow capability permission route", event ->
                event.getSource().getUI()
                        .ifPresent(ui -> ui.navigate(
                                FlowCapabilityPermissionRoute.class)));
        openFlowCapabilityPermission.setId(
                "framework-flow-capability-permission-open-route");
        Button openFlowClipboardOutcomes = new Button(
                "Open Flow clipboard outcomes route", event ->
                event.getSource().getUI()
                        .ifPresent(ui -> ui.navigate(
                                FlowClipboardOutcomesRoute.class)));
        openFlowClipboardOutcomes.setId(
                "framework-flow-clipboard-outcomes-open-route");
        Button openFlowNotificationOutcomes = new Button(
                "Open Flow notification outcomes route", event ->
                event.getSource().getUI()
                        .ifPresent(ui -> ui.navigate(
                                FlowNotificationOutcomesRoute.class)));
        openFlowNotificationOutcomes.setId(
                "framework-flow-notification-outcomes-open-route");
        Button openFlowNotificationDelivery = new Button(
                "Open Flow notification delivery route", event ->
                event.getSource().getUI()
                        .ifPresent(ui -> ui.navigate(
                                FlowNotificationDeliveryRoute.class)));
        openFlowNotificationDelivery.setId(
                "framework-flow-notification-delivery-open-route");
        Button openFlowFileDialogGeneration = new Button(
                "Open Flow file dialog generation route", event ->
                event.getSource().getUI()
                        .ifPresent(ui -> ui.navigate(
                                FlowFileDialogGenerationRoute.class)));
        openFlowFileDialogGeneration.setId(
                "framework-flow-file-dialog-generation-open-route");
        Button openFlowFileDialogOutcomes = new Button(
                "Open Flow file dialog outcomes route", event ->
                event.getSource().getUI()
                        .ifPresent(ui -> ui.navigate(
                                FlowFileDialogOutcomesRoute.class)));
        openFlowFileDialogOutcomes.setId(
                "framework-flow-file-dialog-outcomes-open-route");
        Button openFlowFileReadGrant = new Button(
                "Open Flow file read grant route", event ->
                event.getSource().getUI()
                        .ifPresent(ui -> ui.navigate(
                                FlowFileReadGrantRoute.class)));
        openFlowFileReadGrant.setId(
                "framework-flow-file-read-grant-open-route");
        Button openFlowFileReadGrantIsolation = new Button(
                "Open Flow file read grant isolation route", event ->
                event.getSource().getUI()
                        .ifPresent(ui -> ui.navigate(
                                FlowFileReadGrantIsolationRoute.class)));
        openFlowFileReadGrantIsolation.setId(
                "framework-flow-file-read-grant-isolation-open-route");
        Button openFlowFileReadGrantReplay = new Button(
                "Open Flow file read grant replay route", event ->
                event.getSource().getUI().ifPresent(ui -> ui.navigate(
                        FlowFileReadGrantReplayRoute.class)));
        openFlowFileReadGrantReplay.setId(
                "framework-flow-file-read-grant-replay-open-route");
        Button openFlowFileReadGrantRange = new Button(
                "Open Flow file read grant range route", event ->
                event.getSource().getUI().ifPresent(ui -> ui.navigate(
                        FlowFileReadGrantRangeRoute.class)));
        openFlowFileReadGrantRange.setId(
                "framework-flow-file-read-grant-range-open-route");
        Button openFlowFileWriteGrant = new Button(
                "Open Flow file write grant route", event ->
                event.getSource().getUI().ifPresent(ui -> ui.navigate(
                        FlowFileWriteGrantRoute.class)));
        openFlowFileWriteGrant.setId(
                "framework-flow-file-write-grant-open-route");
        Button openFlowFileWriteGrantIsolation = new Button(
                "Open Flow file write grant isolation route", event ->
                event.getSource().getUI().ifPresent(ui -> ui.navigate(
                        FlowFileWriteGrantIsolationRoute.class)));
        openFlowFileWriteGrantIsolation.setId(
                "framework-flow-file-write-grant-isolation-open");
        Button openFlowFileWriteGrantRecovery = new Button(
                "Open Flow file write grant recovery route", event ->
                event.getSource().getUI().ifPresent(ui -> ui.navigate(
                        FlowFileWriteGrantRecoveryRoute.class)));
        openFlowFileWriteGrantRecovery.setId(
                "framework-flow-file-write-grant-recovery-open-route");
        Button openFlowFileWriteGrantReplay = new Button(
                "Open Flow file write grant recovery replay",
                event -> event.getSource().getUI().ifPresent(ui -> ui.navigate(
                        FlowFileWriteGrantReplayRoute.class)));
        openFlowFileWriteGrantReplay.setId(
                "framework-flow-file-write-grant-replay-open-route");
        Button openFlowFileWriteGrantOwnerClose = new Button(
                "Open Flow write grant owner-close window", event -> {
                    event.getSource().setEnabled(false);
                    DesktopCommandResult opened = Desktop.current().windows().open(
                            DesktopWindow.builder("file-write-owner")
                                    .title("Flow write grant owner close")
                                    .size(1000, 900)
                                    .route("flow-file-write-grant")
                                    .build());
                    if (opened.success()) {
                        Desktop.current().windows().close("main");
                    }
                });
        openFlowFileWriteGrantOwnerClose.setId(
                "framework-flow-file-write-grant-owner-close-open");
        Button openFlowFileDialogOwnerClose = new Button(
                "Open Flow file dialog owner-close route", event ->
                event.getSource().getUI()
                        .ifPresent(ui -> ui.navigate(
                                FlowFileDialogOwnerWindowCloseRoute.class)));
        openFlowFileDialogOwnerClose.setId(
                "framework-flow-file-dialog-owner-close-open-route");
        Button openFlowMainWindowCloseRecovery = new Button(
                "Open Flow main-window close recovery route", event ->
                event.getSource().getUI()
                        .ifPresent(ui -> ui.navigate(
                                FlowMainWindowCloseRecoveryRoute.class)));
        openFlowMainWindowCloseRecovery.setId(
                "framework-flow-main-window-close-recovery-open-route");
        Button openFlowLastWindowClose = new Button(
                "Open Flow final-window close route", event ->
                event.getSource().getUI().ifPresent(ui -> ui.navigate(
                        FlowLastWindowCloseRoute.class)));
        openFlowLastWindowClose.setId("framework-flow-last-window-close-open-route");
        Button openFlowLazyGridSelection = new Button(
                "Open Flow lazy Grid selection route", event ->
                event.getSource().getUI()
                        .ifPresent(ui -> ui.navigate(
                                FlowLazyGridSelectionRoute.class)));
        openFlowLazyGridSelection.setId(
                "framework-flow-lazy-grid-selection-open-route");
        Button openLazyDataBaseline = new Button("Open bounded lazy-data baseline", event ->
                event.getSource().getUI().ifPresent(ui -> ui.navigate(FlowLazyDataBaselineRoute.class)));
        openLazyDataBaseline.setId("lazy-data-baseline-open");
        Button openFlowMultiSelectComboBox = new Button(
                "Open Flow MultiSelectComboBox route", event ->
                event.getSource().getUI()
                        .ifPresent(ui -> ui.navigate(
                                FlowMultiSelectComboBoxRoute.class)));
        openFlowMultiSelectComboBox.setId(
                "framework-flow-multi-select-combo-box-open-route");
        Button openFlowLazyMultiSelectComboBox = new Button(
                "Open Flow lazy MultiSelectComboBox route", event ->
                event.getSource().getUI()
                        .ifPresent(ui -> ui.navigate(
                                FlowLazyMultiSelectComboBoxRoute.class)));
        openFlowLazyMultiSelectComboBox.setId(
                "framework-flow-lazy-multi-select-combo-box-open-route");
        Button openFlowLocalizedNumber = new Button(
                "Open Flow localized number route", event ->
                event.getSource().getUI()
                        .ifPresent(ui -> ui.navigate(
                                FlowLocalizedNumberRoute.class)));
        openFlowLocalizedNumber.setId(
                "framework-flow-localized-number-open-route");
        Button openFlowLocalizedNumberValidation = new Button(
                "Open Flow localized number validation route", event ->
                event.getSource().getUI()
                        .ifPresent(ui -> ui.navigate(
                                FlowLocalizedNumberValidationRoute.class)));
        openFlowLocalizedNumberValidation.setId(
                "framework-flow-localized-number-validation-open-route");
        Button openFlowNativeEdit = new Button(
                "Open Flow native edit route", event ->
                event.getSource().getUI()
                        .ifPresent(ui -> ui.navigate(FlowNativeEditRoute.class)));
        openFlowNativeEdit.setId("framework-flow-native-edit-open-route");
        Button openFlowExternalFileDrop = new Button(
                "Open Flow external file drop route", event ->
                event.getSource().getUI()
                        .ifPresent(ui -> ui.navigate(
                                FlowExternalFileDropRoute.class)));
        openFlowExternalFileDrop.setId(
                "framework-flow-external-file-drop-open-route");

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

        Slider riskTolerance = new Slider("Risk tolerance", 0, 10);
        riskTolerance.setId("framework-slider");
        riskTolerance.setStep(0.5);
        riskTolerance.setValue(3.0);
        riskTolerance.setMinMaxVisible(true);
        riskTolerance.setValueAlwaysVisible(true);
        riskTolerance.setAriaLabel("Risk tolerance slider");
        Span sliderStatus = new Span("Slider: value=3.0 range=0.0-10.0 step=0.5");
        sliderStatus.setId("framework-slider-status");
        riskTolerance.addValueChangeListener(event ->
                sliderStatus.setText("Slider client value: " + event.getValue()));
        Button sliderServerUpdate = new Button("Set Slider", event -> {
            riskTolerance.setMin(2.0);
            riskTolerance.setMax(12.0);
            riskTolerance.setStep(1.0);
            riskTolerance.setValue(9.0);
            riskTolerance.setMinMaxVisible(false);
            riskTolerance.setValueAlwaysVisible(false);
            sliderStatus.setText("Slider server value: 9.0 range=2.0-12.0 step=1.0");
        });
        sliderServerUpdate.setId("framework-slider-server-update");

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
        TextField popoverFocusField = new TextField("Popover focus field");
        popoverFocusField.setId("framework-popover-focus-field");
        Button popoverLastAction = new Button("Popover last action");
        popoverLastAction.setId("framework-popover-last-action");
        Popover popover = new Popover(
                new Span("Popover audit content"),
                popoverFocusField,
                popoverLastAction);
        popover.setId("framework-popover");
        popover.setTarget(popoverTarget);
        popover.setOpenOnClick(true);
        popover.setModal(false);
        popover.setAutofocus(false);
        popover.setCloseOnEsc(true);
        popover.setCloseOnOutsideClick(true);
        popover.setRole("dialog");
        popover.setAriaLabel("Flow IPC anchored modeless popover");
        Span popoverLifecycleStatus = new Span("Popover lifecycle idle");
        popoverLifecycleStatus.setId("framework-popover-lifecycle-status");
        popover.addOpenedChangeListener(event -> {
            if (event.isOpened()) {
                overlayMatrixStatus.setText("Popover opened: true");
            }
            popoverLifecycleStatus.setText("Popover lifecycle "
                    + (event.isOpened() ? "opened" : "closed")
                    + ": client=" + event.isFromClient());
        });
        Button popoverOutsideTarget = new Button("Popover outside target");
        popoverOutsideTarget.setId("framework-popover-outside-target");

        Dialog mixedOverlayDialog = new Dialog();
        mixedOverlayDialog.setId("framework-mixed-overlay-dialog");
        mixedOverlayDialog.setHeaderTitle("Flow IPC mixed overlay stack");
        mixedOverlayDialog.setCloseOnEsc(true);
        mixedOverlayDialog.setFocusTrap(true);
        Button mixedOverlayPopoverTarget = new Button("Mixed overlay popover target");
        mixedOverlayPopoverTarget.setId("framework-mixed-overlay-popover-target");
        TextField mixedOverlayPopoverFocusField =
                new TextField("Mixed overlay popover focus field");
        mixedOverlayPopoverFocusField.setId(
                "framework-mixed-overlay-popover-focus-field");
        Button mixedOverlayPopoverLastAction =
                new Button("Mixed overlay popover last action");
        mixedOverlayPopoverLastAction.setId(
                "framework-mixed-overlay-popover-last-action");
        Popover mixedOverlayPopover = new Popover(
                new Span("Mixed overlay popover content"),
                mixedOverlayPopoverFocusField,
                mixedOverlayPopoverLastAction);
        mixedOverlayPopover.setId("framework-mixed-overlay-popover");
        mixedOverlayPopover.setTarget(mixedOverlayPopoverTarget);
        mixedOverlayPopover.setOpenOnClick(true);
        mixedOverlayPopover.setModal(false);
        mixedOverlayPopover.setAutofocus(false);
        mixedOverlayPopover.setCloseOnEsc(true);
        mixedOverlayPopover.setCloseOnOutsideClick(true);
        mixedOverlayPopover.setRole("dialog");
        mixedOverlayPopover.setAriaLabel("Flow IPC mixed modeless popover");
        Span mixedOverlayPopoverLifecycleStatus =
                new Span("Mixed overlay Popover lifecycle idle");
        mixedOverlayPopoverLifecycleStatus.setId(
                "framework-mixed-overlay-popover-lifecycle-status");
        mixedOverlayPopover.addOpenedChangeListener(event -> {
            mixedOverlayPopoverLifecycleStatus.setText(
                    "Mixed overlay Popover lifecycle "
                            + (event.isOpened() ? "opened" : "closed")
                            + ": client=" + event.isFromClient());
            if (event.isOpened()) {
                overlayMatrixStatus.setText("Mixed overlay Popover opened: true");
            }
        });
        Button mixedOverlayOutsideTarget =
                new Button("Mixed overlay dialog outside target");
        mixedOverlayOutsideTarget.setId("framework-mixed-overlay-outside-target");
        Span mixedOverlayDialogLifecycleStatus =
                new Span("Mixed overlay Dialog lifecycle idle");
        mixedOverlayDialogLifecycleStatus.setId(
                "framework-mixed-overlay-dialog-lifecycle-status");
        mixedOverlayDialog.addOpenedChangeListener(event ->
                mixedOverlayDialogLifecycleStatus.setText(
                        "Mixed overlay Dialog lifecycle "
                                + (event.isOpened() ? "opened" : "closed")
                                + ": client=" + event.isFromClient()));
        mixedOverlayDialog.add(
                mixedOverlayPopoverTarget,
                mixedOverlayPopover,
                mixedOverlayPopoverLifecycleStatus,
                mixedOverlayOutsideTarget,
                mixedOverlayDialogLifecycleStatus);
        Button mixedOverlayDialogOpen = new Button("Open mixed overlay stack", event -> {
            mixedOverlayDialog.open();
            mixedOverlayPopoverTarget.focus();
        });
        mixedOverlayDialogOpen.setId("framework-mixed-overlay-dialog-open");

        Button nestedPopoverOuterTarget =
                new Button("Nested popover outer target");
        nestedPopoverOuterTarget.setId("framework-nested-popover-outer-target");
        Button nestedPopoverInnerTarget =
                new Button("Nested popover inner target");
        nestedPopoverInnerTarget.setId("framework-nested-popover-inner-target");
        TextField nestedPopoverInnerFocusField =
                new TextField("Nested popover inner focus field");
        nestedPopoverInnerFocusField.setId(
                "framework-nested-popover-inner-focus-field");
        Button nestedPopoverInnerLastAction =
                new Button("Nested popover inner last action");
        nestedPopoverInnerLastAction.setId(
                "framework-nested-popover-inner-last-action");
        Popover nestedPopoverInner = new Popover(
                new Span("Nested popover inner content"),
                nestedPopoverInnerFocusField,
                nestedPopoverInnerLastAction);
        nestedPopoverInner.setId("framework-nested-popover-inner");
        nestedPopoverInner.setTarget(nestedPopoverInnerTarget);
        nestedPopoverInner.setOpenOnClick(true);
        nestedPopoverInner.setModal(false);
        nestedPopoverInner.setAutofocus(false);
        nestedPopoverInner.setCloseOnEsc(true);
        nestedPopoverInner.setCloseOnOutsideClick(true);
        nestedPopoverInner.setRole("dialog");
        nestedPopoverInner.setAriaLabel("Flow IPC nested inner popover");
        Span nestedPopoverInnerLifecycleStatus =
                new Span("Nested inner Popover lifecycle idle");
        nestedPopoverInnerLifecycleStatus.setId(
                "framework-nested-popover-inner-lifecycle-status");
        nestedPopoverInner.addOpenedChangeListener(event ->
                nestedPopoverInnerLifecycleStatus.setText(
                        "Nested inner Popover lifecycle "
                                + (event.isOpened() ? "opened" : "closed")
                                + ": client=" + event.isFromClient()));
        Button nestedPopoverOuterLastAction =
                new Button("Nested popover outer last action");
        nestedPopoverOuterLastAction.setId(
                "framework-nested-popover-outer-last-action");
        Popover nestedPopoverOuter = new Popover(
                new Span("Nested popover outer content"),
                nestedPopoverInnerTarget,
                nestedPopoverInner,
                nestedPopoverInnerLifecycleStatus,
                nestedPopoverOuterLastAction);
        nestedPopoverOuter.setId("framework-nested-popover-outer");
        nestedPopoverOuter.setTarget(nestedPopoverOuterTarget);
        nestedPopoverOuter.setOpenOnClick(true);
        nestedPopoverOuter.setModal(false);
        nestedPopoverOuter.setAutofocus(false);
        nestedPopoverOuter.setCloseOnEsc(true);
        nestedPopoverOuter.setCloseOnOutsideClick(true);
        nestedPopoverOuter.setRole("dialog");
        nestedPopoverOuter.setAriaLabel("Flow IPC nested outer popover");
        Span nestedPopoverOuterLifecycleStatus =
                new Span("Nested outer Popover lifecycle idle");
        nestedPopoverOuterLifecycleStatus.setId(
                "framework-nested-popover-outer-lifecycle-status");
        nestedPopoverOuter.addOpenedChangeListener(event -> {
            nestedPopoverOuterLifecycleStatus.setText(
                    "Nested outer Popover lifecycle "
                            + (event.isOpened() ? "opened" : "closed")
                            + ": client=" + event.isFromClient());
            if (event.isOpened()) {
                overlayMatrixStatus.setText("Nested anchored Popover opened: true");
            }
        });
        Button nestedPopoverOutsideTarget =
                new Button("Nested popover outside target");
        nestedPopoverOutsideTarget.setId(
                "framework-nested-popover-outside-target");

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
        Span lazyGridSelectionStatus = new Span("Lazy grid selection: pending");
        lazyGridSelectionStatus.setId("framework-lazy-grid-selection-status");
        Span lazyGridQueryStatus = new Span("Lazy grid query: pending");
        lazyGridQueryStatus.setId("framework-lazy-grid-query-status");
        Span lazyGridCountStatus = new Span("Lazy grid count: pending");
        lazyGridCountStatus.setId("framework-lazy-grid-count-status");
        AtomicInteger lazyGridFetchCount = new AtomicInteger();
        AtomicInteger lazyGridCountCount = new AtomicInteger();
        AtomicReference<String> lazyGridLastRange = new AtomicReference<>("pending");

        Grid<WorkItem> lazyGrid = new Grid<>(WorkItem.class, false);
        lazyGrid.setId("framework-lazy-grid");
        lazyGrid.addColumn(WorkItem::id).setHeader("Lazy ticket")
                .setKey("id").setSortProperty("id").setSortable(true);
        lazyGrid.addColumn(WorkItem::owner).setHeader("Lazy owner")
                .setKey("owner").setSortProperty("owner").setSortable(true);
        lazyGrid.addColumn(WorkItem::state).setHeader("Lazy state")
                .setKey("state").setSortProperty("state").setSortable(true);
        lazyGrid.setPageSize(5);
        lazyGrid.setHeight("220px");

        CallbackDataProvider<WorkItem, String> lazyGridDataProvider =
                DataProvider.fromFilteringCallbacks(query -> {
            String filter = normalizeLazyGridFilter(query.getFilter().orElse(""));
            List<WorkItem> matchingItems = queryLazyWorkItems(
                    lazyWorkItems, filter, query.getSortOrders());
            int offset = query.getOffset();
            int limit = query.getLimit();
            int end = Math.min(offset + limit, matchingItems.size());
            int fetchCount = lazyGridFetchCount.incrementAndGet();
            lazyGridLastRange.set(offset + "-" + end);
            lazyGridStatus.setText("Lazy grid fetch " + fetchCount + ": " + offset + "-" + end);
            lazyGridQueryStatus.setText("Lazy grid query: sort="
                    + lazyGridSortSummary(query.getSortOrders())
                    + ", filter=" + (filter.isEmpty() ? "all" : filter)
                    + ", matches=" + matchingItems.size()
                    + ", range=" + offset + "-" + end);
            return matchingItems.stream().skip(offset).limit(limit);
        }, query -> {
            int countCall = lazyGridCountCount.incrementAndGet();
            String filter = normalizeLazyGridFilter(query.getFilter().orElse(""));
            int matches = queryLazyWorkItems(
                    lazyWorkItems, filter, query.getSortOrders()).size();
            lazyGridCountStatus.setText("Lazy grid count " + countCall + ": filter="
                    + (filter.isEmpty() ? "all" : filter)
                    + ", matches=" + matches);
            return matches;
        });
        ConfigurableFilterDataProvider<WorkItem, Void, String>
                configurableLazyGridDataProvider =
                lazyGridDataProvider.withConfigurableFilter();
        lazyGrid.setDataProvider(configurableLazyGridDataProvider);

        TextField lazyGridFilter = new TextField("Lazy owner filter");
        lazyGridFilter.setId("framework-lazy-grid-filter");
        lazyGridFilter.setPlaceholder("Filter by owner");
        lazyGridFilter.setClearButtonVisible(true);
        lazyGridFilter.setValueChangeMode(ValueChangeMode.EAGER);
        lazyGridFilter.addValueChangeListener(event ->
                configurableLazyGridDataProvider.setFilter(
                        normalizeLazyGridFilter(event.getValue())));
        lazyGrid.addSelectionListener(event -> lazyGridSelectionStatus.setText(
                event.getFirstSelectedItem()
                        .map(item -> "Lazy grid selection: " + item.id())
                        .orElse("Lazy grid selection: cleared")));

        Button lazyGridReport = new Button("Report Lazy Grid", event ->
                lazyGridStatus.setText("Lazy grid fetches: " + lazyGridFetchCount.get()
                        + ", count calls: " + lazyGridCountCount.get()
                        + ", range: " + lazyGridLastRange.get()));
        lazyGridReport.setId("framework-lazy-grid-report");

        TreeWorkItem platformDomain = new TreeWorkItem(
                "domain-platform", "Platform", "active");
        TreeWorkItem productDomain = new TreeWorkItem(
                "domain-product", "Product", "active");
        TreeWorkItem runtimeArea = new TreeWorkItem(
                "area-runtime", "Runtime", "ready");
        TreeWorkItem hostArea = new TreeWorkItem(
                "area-native-host", "Native host", "ready");
        TreeWorkItem transportCapability = new TreeWorkItem(
                "capability-opaque-ipc", "Opaque IPC", "verified");
        TreeWorkItem pushCapability = new TreeWorkItem(
                "capability-push", "Push", "verified");
        TreeWorkItem componentCapability = new TreeWorkItem(
                "capability-components", "Components", "active");
        List<TreeWorkItem> treeGridRoots = List.of(platformDomain, productDomain);
        Map<String, List<TreeWorkItem>> treeGridChildren = new LinkedHashMap<>();
        treeGridChildren.put(platformDomain.id(),
                new ArrayList<>(List.of(runtimeArea, hostArea)));
        treeGridChildren.put(productDomain.id(),
                new ArrayList<>(List.of(componentCapability)));
        treeGridChildren.put(runtimeArea.id(),
                new ArrayList<>(List.of(transportCapability, pushCapability)));

        Span treeGridQueryStatus = new Span("Tree grid query: pending");
        treeGridQueryStatus.setId("framework-tree-grid-query-status");
        Span treeGridCountStatus = new Span("Tree grid count: pending");
        treeGridCountStatus.setId("framework-tree-grid-count-status");
        Span treeGridLifecycleStatus = new Span("Tree grid lifecycle: pending");
        treeGridLifecycleStatus.setId("framework-tree-grid-lifecycle-status");
        Span treeGridSelectionStatus = new Span("Tree grid selection: pending");
        treeGridSelectionStatus.setId("framework-tree-grid-selection-status");
        Span treeGridRefreshStatus = new Span("Tree grid refresh: pending");
        treeGridRefreshStatus.setId("framework-tree-grid-refresh-status");
        AtomicInteger treeGridFetchCount = new AtomicInteger();
        AtomicInteger treeGridCountCount = new AtomicInteger();

        AbstractBackEndHierarchicalDataProvider<TreeWorkItem, Void>
                treeGridDataProvider =
                new AbstractBackEndHierarchicalDataProvider<>() {
            @Override
            public int getChildCount(HierarchicalQuery<TreeWorkItem, Void> query) {
                List<TreeWorkItem> children = treeGridChildren(
                        query.getParent(), treeGridRoots, treeGridChildren);
                treeGridCountStatus.setText("Tree grid count "
                        + treeGridCountCount.incrementAndGet()
                        + ": parent=" + treeGridParentId(query.getParent())
                        + ", count=" + children.size());
                return children.size();
            }

            @Override
            public boolean hasChildren(TreeWorkItem item) {
                return !treeGridChildren.getOrDefault(item.id(), List.of()).isEmpty();
            }

            @Override
            protected Stream<TreeWorkItem> fetchChildrenFromBackEnd(
                    HierarchicalQuery<TreeWorkItem, Void> query) {
                List<TreeWorkItem> children = treeGridChildren(
                        query.getParent(), treeGridRoots, treeGridChildren);
                int offset = query.getOffset();
                int end = Math.min(offset + query.getLimit(), children.size());
                treeGridQueryStatus.setText("Tree grid fetch "
                        + treeGridFetchCount.incrementAndGet()
                        + ": parent=" + treeGridParentId(query.getParent())
                        + ", range=" + offset + "-" + end);
                return children.stream().skip(offset).limit(query.getLimit());
            }
        };

        TreeGrid<TreeWorkItem> treeGrid = new TreeGrid<>();
        treeGrid.setId("framework-tree-grid");
        treeGrid.addHierarchyColumn(TreeWorkItem::label)
                .setHeader("Capability");
        treeGrid.addColumn(TreeWorkItem::id).setHeader("Tree id");
        treeGrid.addColumn(TreeWorkItem::state).setHeader("Tree state");
        treeGrid.setPageSize(4);
        treeGrid.setHeight("260px");
        treeGrid.setDataProvider(treeGridDataProvider);
        treeGrid.addExpandListener(event -> event.getItems().stream().findFirst()
                .ifPresent(item -> treeGridLifecycleStatus.setText(
                        "Tree grid expand: " + item.id()
                                + " client=" + event.isFromClient())));
        treeGrid.addCollapseListener(event -> event.getItems().stream().findFirst()
                .ifPresent(item -> treeGridLifecycleStatus.setText(
                        "Tree grid collapse: " + item.id()
                                + " client=" + event.isFromClient())));
        treeGrid.addSelectionListener(event ->
                treeGridSelectionStatus.setText(event.getFirstSelectedItem()
                        .map(item -> "Tree grid selection: " + item.id())
                        .orElse("Tree grid selection: cleared")));

        Button treeGridRefresh = new Button("Add runtime capability", event -> {
            List<TreeWorkItem> runtimeChildren = treeGridChildren.get(runtimeArea.id());
            boolean alreadyAdded = runtimeChildren.stream()
                    .anyMatch(item -> item.id().equals("capability-trace"));
            if (!alreadyAdded) {
                runtimeChildren.add(new TreeWorkItem(
                        "capability-trace", "Trace inspector", "new"));
            }
            treeGridDataProvider.refreshItem(runtimeArea, true);
            treeGridRefreshStatus.setText(
                    "Tree grid refresh: area-runtime children="
                            + runtimeChildren.size());
        });
        treeGridRefresh.setId("framework-tree-grid-refresh");

        List<EditableWorkItem> editableWorkItems = new ArrayList<>(List.of(
                new EditableWorkItem("E-100", "platform", "queued"),
                new EditableWorkItem("E-101", "desktop", "review"),
                new EditableWorkItem("E-102", "audit", "ready")));
        Grid<EditableWorkItem> editorGrid = new Grid<>(EditableWorkItem.class, false);
        editorGrid.setId("framework-editor-grid");
        editorGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
        editorGrid.setAllRowsVisible(true);

        Editor<EditableWorkItem> editor = editorGrid.getEditor();
        Binder<EditableWorkItem> editorBinder = new Binder<>(EditableWorkItem.class);
        editor.setBinder(editorBinder);
        editor.setBuffered(true);

        Span editorRendererStatus = new Span("ComponentRenderer: pending");
        editorRendererStatus.setId("framework-editor-renderer-status");
        Span editorOpenStatus = new Span("Grid editor open: pending");
        editorOpenStatus.setId("framework-editor-open-status");
        Span editorSaveStatus = new Span("Grid editor save: pending");
        editorSaveStatus.setId("framework-editor-save-status");
        Span editorCancelStatus = new Span("Grid editor cancel: pending");
        editorCancelStatus.setId("framework-editor-cancel-status");
        Span editorValidationStatus = new Span("Grid editor validation: pending");
        editorValidationStatus.setId("framework-editor-validation-status");
        Span litRendererStatus = new Span("LitRenderer: pending");
        litRendererStatus.setId("framework-lit-renderer-status");

        Set<String> renderedEditorItems = new LinkedHashSet<>();
        Set<String> openedEditorItems = new LinkedHashSet<>();
        List<String> cancelledEditorItems = new ArrayList<>();
        Map<Button, EditableWorkItem> editorItemsByButton = new IdentityHashMap<>();

        editorGrid.addColumn(item -> item == null ? "" : item.getId())
                .setHeader("Editor ticket");
        Grid.Column<EditableWorkItem> editorOwnerColumn =
                editorGrid.addColumn(item -> item == null ? "" : item.getOwner())
                        .setHeader("Editor owner");
        editorGrid.addColumn(item -> item == null ? "" : item.getState())
                .setHeader("Editor state");
        editorGrid.addColumn(LitRenderer.<EditableWorkItem>of("""
                <button class="framework-lit-renderer-action"
                        @click="${advanceState}">
                  Lit ${item.id}: ${item.state}
                </button>
                """)
                .withProperty("id", EditableWorkItem::getId)
                .withProperty("state", EditableWorkItem::getState)
                .withFunction("advanceState", item -> {
                    item.setState("lit-updated");
                    editorGrid.getDataProvider().refreshItem(item);
                    litRendererStatus.setText("LitRenderer function: "
                            + item.getId() + " state=" + item.getState());
                }))
                .setHeader("Lit renderer");

        TextField editorOwner = new TextField();
        editorOwner.setId("framework-editor-owner-field");
        editorOwner.setAriaLabel("Editor owner");
        editorOwner.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        editorBinder.forField(editorOwner)
                .asRequired("Owner is required")
                .bind(EditableWorkItem::getOwner, EditableWorkItem::setOwner);
        editorOwnerColumn.setEditorComponent(editorOwner);

        Button editorSave = new Button("Save", event -> {
            if (!editor.save()) {
                String itemId = editor.isOpen()
                        ? editor.getItem().getId()
                        : "none";
                editorValidationStatus.setText(
                        "Grid editor validation blocked: " + itemId);
            }
        });
        editorSave.setId("framework-editor-save");
        Button editorCancel = new Button("Cancel", event -> editor.cancel());
        editorCancel.setId("framework-editor-cancel");
        HorizontalLayout editorActions = new HorizontalLayout(editorSave, editorCancel);
        editorActions.setPadding(false);
        editorActions.setSpacing(true);

        Grid.Column<EditableWorkItem> editorActionColumn = editorGrid.addColumn(
                new ComponentRenderer<Button, EditableWorkItem>(() -> {
                    Button edit = new Button("Edit");
                    edit.addClickListener(event -> {
                        EditableWorkItem item = editorItemsByButton.get(edit);
                        if (item != null) {
                            editor.editItem(item);
                        }
                    });
                    edit.addDetachListener(event -> editorItemsByButton.remove(edit));
                    return edit;
                }, (edit, item) -> {
                    if (item == null) {
                        editorItemsByButton.remove(edit);
                        edit.setVisible(false);
                        edit.getElement().removeAttribute("id");
                        return;
                    }
                    editorItemsByButton.put(edit, item);
                    edit.setVisible(true);
                    edit.setId("framework-editor-open-" + item.getId());
                    edit.setText("Edit " + item.getId());
                    renderedEditorItems.add(item.getId());
                    editorRendererStatus.setText("ComponentRenderer items: "
                            + String.join(",", renderedEditorItems));
                }))
                .setHeader("Editor actions");
        editorActionColumn.setEditorComponent(editorActions);
        editorGrid.setItems(editableWorkItems);

        editor.addOpenListener(event -> {
            openedEditorItems.add(event.getItem().getId());
            editorOpenStatus.setText("Grid editor opens: "
                    + String.join(",", openedEditorItems));
        });
        editor.addSaveListener(event -> {
            EditableWorkItem item = event.getItem();
            editorGrid.getDataProvider().refreshItem(item);
            editorSaveStatus.setText("Grid editor save: " + item.getId()
                    + " owner=" + item.getOwner());
        });
        editor.addCancelListener(event -> {
            EditableWorkItem item = event.getItem();
            if (item == null) {
                return;
            }
            cancelledEditorItems.add(item.getId() + " owner=" + item.getOwner());
            editorCancelStatus.setText("Grid editor cancels: "
                    + String.join(",", cancelledEditorItems));
        });

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

        Span flowDndSource = new Span("Drag Flow payload");
        flowDndSource.setId("framework-flow-dnd-source");
        flowDndSource.getStyle()
                .set("display", "inline-block")
                .set("padding", "12px")
                .set("border", "1px solid var(--lumo-primary-color)")
                .set("cursor", "grab");
        Span flowDndTarget = new Span("Drop Flow payload");
        flowDndTarget.setId("framework-flow-dnd-target");
        flowDndTarget.getStyle()
                .set("display", "inline-block")
                .set("min-width", "180px")
                .set("padding", "12px")
                .set("border", "1px dashed var(--lumo-contrast-50pct)");
        Span flowDndStartStatus = new Span("Flow DnD drag start: pending");
        flowDndStartStatus.setId("framework-flow-dnd-start-status");
        Span flowDndDropStatus = new Span("Flow DnD drop: pending");
        flowDndDropStatus.setId("framework-flow-dnd-drop-status");
        Span flowDndEndStatus = new Span("Flow DnD drag end: pending");
        flowDndEndStatus.setId("framework-flow-dnd-end-status");

        DragSource<Span> flowDragSource = DragSource.create(flowDndSource);
        flowDragSource.setEffectAllowed(EffectAllowed.MOVE);
        flowDragSource.setDragData("flow-dnd-initial");
        flowDragSource.addDragStartListener(event -> {
            event.setDragData("flow-dnd-payload");
            flowDndStartStatus.setText(
                    "Flow DnD drag start: source=framework-flow-dnd-source"
                            + " data=flow-dnd-payload effectAllowed=MOVE");
        });
        flowDragSource.addDragEndListener(event -> {
            flowDndEndStatus.setText("Flow DnD drag end: dropEffect="
                    + event.getDropEffect() + " successful="
                    + event.isSuccessful());
            event.clearDragData();
        });

        DropTarget<Span> flowDropTarget = DropTarget.create(flowDndTarget);
        flowDropTarget.setDropEffect(DropEffect.MOVE);
        flowDropTarget.addDropListener(event -> {
            String sourceId = event.getDragSourceComponent()
                    .flatMap(component -> component.getId())
                    .orElse("none");
            String dragData = event.getDragData()
                    .map(String::valueOf)
                    .orElse("none");
            flowDndDropStatus.setText("Flow DnD drop: source=" + sourceId
                    + " data=" + dragData
                    + " effectAllowed=" + event.getEffectAllowed()
                    + " dropEffect=" + event.getDropEffect());
        });

        HorizontalLayout flowDndSurface = new HorizontalLayout(
                flowDndSource, flowDndTarget);
        flowDndSurface.setId("framework-flow-dnd-surface");
        flowDndSurface.setAlignItems(Alignment.CENTER);
        VerticalLayout flowDndProbe = new VerticalLayout(
                flowDndSurface, flowDndStartStatus, flowDndDropStatus,
                flowDndEndStatus);
        flowDndProbe.setId("framework-flow-dnd-probe");
        flowDndProbe.setPadding(false);
        flowDndProbe.setSpacing(false);

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

        Icon icon = new Icon(VaadinIcon.CLIPBOARD_CHECK);
        icon.setId("framework-icon");
        icon.setColor("var(--lumo-primary-color)");
        icon.setSize("24px");
        icon.getElement().setAttribute("aria-label", "Framework icon");
        Span iconStatus = new Span("Icon: vaadin:clipboard-check color=primary size=24");
        iconStatus.setId("framework-icon-status");
        AtomicInteger iconClickCount = new AtomicInteger();
        icon.addClickListener(event -> iconStatus.setText("Icon clicked: icon="
                + icon.getIcon() + " count=" + iconClickCount.incrementAndGet()));
        Button iconServerUpdate = new Button("Update Icon", event -> {
            icon.setIcon(VaadinIcon.COG);
            icon.setColor("var(--lumo-success-color)");
            icon.setSize("32px");
            iconStatus.setText("Icon clicked: icon=vaadin:clipboard-check count="
                    + iconClickCount.get()
                    + " | Icon server state: vaadin:cog color=success size=32");
        });
        iconServerUpdate.setId("framework-icon-server-update");

        AppLayout appLayout = new AppLayout();
        appLayout.setId("framework-app-layout");
        appLayout.setPrimarySection(AppLayout.Section.DRAWER);
        appLayout.setDrawerOpened(true);
        appLayout.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("display", "block")
                .set("height", "220px")
                .set("overflow", "hidden")
                .set("position", "relative")
                .set("width", "420px");
        DrawerToggle appLayoutToggle = new DrawerToggle();
        appLayoutToggle.setId("framework-app-layout-toggle");
        Span appLayoutTitle = new Span("IPC Shell");
        appLayoutTitle.setId("framework-app-layout-title");
        Span appLayoutContent = new Span("AppLayout content: overview");
        appLayoutContent.setId("framework-app-layout-content");
        Span appLayoutStatus = new Span("AppLayout: drawer open primary=drawer");
        appLayoutStatus.setId("framework-app-layout-status");
        Button appLayoutDrawerAction = new Button("Drawer Audit", event ->
                appLayoutStatus.setText("AppLayout drawer action: audit"));
        appLayoutDrawerAction.setId("framework-app-layout-drawer-action");
        AtomicInteger appLayoutToggleCount = new AtomicInteger();
        appLayoutToggle.addClickListener(event ->
                appLayoutStatus.setText("AppLayout toggle clicked: count="
                        + appLayoutToggleCount.incrementAndGet()));
        Button appLayoutServerUpdate = new Button("Update AppLayout", event -> {
            appLayout.setPrimarySection(AppLayout.Section.NAVBAR);
            appLayout.setDrawerOpened(false);
            appLayoutContent.setText("AppLayout content: patched navbar closed");
            appLayoutStatus.setText("AppLayout toggle clicked: count="
                    + appLayoutToggleCount.get()
                    + " | AppLayout server state: primary=navbar drawer=false");
        });
        appLayoutServerUpdate.setId("framework-app-layout-server-update");
        appLayout.addToNavbar(appLayoutToggle, appLayoutTitle);
        appLayout.addToDrawer(appLayoutDrawerAction);
        appLayout.setContent(appLayoutContent);

        MasterDetailLayout masterDetailLayout = new MasterDetailLayout();
        masterDetailLayout.setId("framework-master-detail-layout");
        masterDetailLayout.addThemeVariants(MasterDetailLayoutVariant.AURA_INSET_DRAWER);
        masterDetailLayout.setWidth("420px");
        masterDetailLayout.setHeight("220px");
        masterDetailLayout.setMasterSize("150px");
        masterDetailLayout.setMasterMinSize("120px");
        masterDetailLayout.setDetailSize("270px");
        masterDetailLayout.setDetailMinSize("180px");
        masterDetailLayout.setOrientation(Orientation.HORIZONTAL);
        masterDetailLayout.setContainment(Containment.LAYOUT);
        masterDetailLayout.setOverlayMode(OverlayMode.DRAWER);
        masterDetailLayout.setForceOverlay(false);
        masterDetailLayout.setAnimationEnabled(false);
        VerticalLayout masterDetailMaster = new VerticalLayout();
        masterDetailMaster.setId("framework-master-detail-master");
        masterDetailMaster.setPadding(false);
        masterDetailMaster.setSpacing(false);
        masterDetailMaster.add(
                new Span("Master queue seeded"),
                new Span("Queue item IPC-101"));
        VerticalLayout masterDetailDetail = new VerticalLayout();
        masterDetailDetail.setId("framework-master-detail-detail");
        masterDetailDetail.setPadding(false);
        masterDetailDetail.setSpacing(false);
        masterDetailDetail.add(
                new Span("Detail panel seeded"),
                new Span("Detail virtual child ready"));
        masterDetailLayout.setMaster(masterDetailMaster);
        masterDetailLayout.setDetail(masterDetailDetail);
        Span masterDetailStatus = new Span("MasterDetail: horizontal drawer layout");
        masterDetailStatus.setId("framework-master-detail-status");
        AtomicInteger masterDetailBackdropCount = new AtomicInteger();
        masterDetailLayout.addBackdropClickListener(event ->
                masterDetailStatus.setText("MasterDetail backdrop clicked: count="
                        + masterDetailBackdropCount.incrementAndGet()));
        Button masterDetailServerUpdate = new Button("Update MasterDetail", event -> {
            masterDetailLayout.setMasterSize("180px");
            masterDetailLayout.setMasterMinSize("140px");
            masterDetailLayout.setDetailSize("260px");
            masterDetailLayout.setDetailMinSize("200px");
            masterDetailLayout.setOrientation(Orientation.VERTICAL);
            masterDetailLayout.setContainment(Containment.VIEWPORT);
            masterDetailLayout.setOverlayMode(OverlayMode.STACK);
            masterDetailLayout.setForceOverlay(true);
            VerticalLayout patchedMaster = new VerticalLayout();
            patchedMaster.setId("framework-master-detail-master-patched");
            patchedMaster.setPadding(false);
            patchedMaster.setSpacing(false);
            patchedMaster.add(
                    new Span("Master queue patched"),
                    new Span("Queue item IPC-202"));
            VerticalLayout patchedDetail = new VerticalLayout();
            patchedDetail.setId("framework-master-detail-detail-patched");
            patchedDetail.setPadding(false);
            patchedDetail.setSpacing(false);
            patchedDetail.add(
                    new Span("Detail panel patched"),
                    new Span("Detail virtual child replaced"));
            masterDetailLayout.setMaster(patchedMaster);
            masterDetailLayout.setDetail(patchedDetail);
            masterDetailStatus.setText("MasterDetail backdrop clicked: count="
                    + masterDetailBackdropCount.get()
                    + " | MasterDetail server state: vertical stack overlay viewport patched"
                    + " animation=disabled");
        });
        masterDetailServerUpdate.setId("framework-master-detail-server-update");
        Button masterDetailAnimationUpdate = new Button("Enable MasterDetail Animation", event -> {
            masterDetailLayout.setAnimationEnabled(true);
            masterDetailStatus.setText("MasterDetail backdrop clicked: count="
                    + masterDetailBackdropCount.get()
                    + " | MasterDetail server state: vertical stack overlay viewport patched"
                    + " animation=enabled");
        });
        masterDetailAnimationUpdate.setId("framework-master-detail-animation-update");

        Card auditCard = new Card();
        auditCard.setId("framework-card");
        auditCard.addThemeVariants(CardVariant.LUMO_OUTLINED);
        auditCard.setAriaRole("region");
        auditCard.setAriaLabel("Framework audit card");
        Span cardMedia = new Span("Card media: IPC");
        cardMedia.setId("framework-card-media");
        auditCard.setMedia(cardMedia);
        auditCard.setTitle("Flow IPC card", 3);
        Span cardSubtitle = new Span("Card subtitle: seeded");
        cardSubtitle.setId("framework-card-subtitle");
        auditCard.setSubtitle(cardSubtitle);
        Span cardHeaderPrefix = new Span("P1");
        cardHeaderPrefix.setId("framework-card-header-prefix");
        auditCard.setHeaderPrefix(cardHeaderPrefix);
        Span cardHeaderSuffix = new Span("queued");
        cardHeaderSuffix.setId("framework-card-header-suffix");
        auditCard.setHeaderSuffix(cardHeaderSuffix);
        Span cardBody = new Span("Card body: routed through Flow client");
        cardBody.setId("framework-card-body");
        Span cardFooter = new Span("Card footer: ready");
        cardFooter.setId("framework-card-footer");
        auditCard.add(cardBody);
        auditCard.addToFooter(cardFooter);
        Span cardStatus = new Span("Card: seeded outlined role=region");
        cardStatus.setId("framework-card-status");
        AtomicInteger cardClickCount = new AtomicInteger();
        auditCard.getElement().addEventListener("click", event ->
                cardStatus.setText("Card clicked: count=" + cardClickCount.incrementAndGet()));
        Button cardServerUpdate = new Button("Update Card", event -> {
            auditCard.addThemeVariants(CardVariant.LUMO_ELEVATED, CardVariant.LUMO_HORIZONTAL);
            auditCard.setAriaRole("article");
            auditCard.setAriaLabel("Framework audit card patched");
            Span patchedMedia = new Span("Card media: server patched");
            patchedMedia.setId("framework-card-media-patched");
            auditCard.setMedia(patchedMedia);
            auditCard.setTitle("Flow IPC patched card", 2);
            Span patchedSubtitle = new Span("Card subtitle: server patched");
            patchedSubtitle.setId("framework-card-subtitle-patched");
            auditCard.setSubtitle(patchedSubtitle);
            Span patchedPrefix = new Span("P2");
            patchedPrefix.setId("framework-card-header-prefix-patched");
            auditCard.setHeaderPrefix(patchedPrefix);
            Span patchedSuffix = new Span("approved");
            patchedSuffix.setId("framework-card-header-suffix-patched");
            auditCard.setHeaderSuffix(patchedSuffix);
            cardBody.setText("Card body: server patched");
            cardFooter.setText("Card footer: closed");
            cardStatus.setText("Card clicked: count=" + cardClickCount.get()
                    + " | Card server state: elevated horizontal patched");
        });
        cardServerUpdate.setId("framework-card-server-update");

        Markdown markdown = new Markdown("""
                # Flow IPC Markdown

                - **client** ready
                - [audit link](https://example.test/audit)
                """);
        markdown.setId("framework-markdown");
        markdown.setWidth("420px");
        Span markdownStatus = new Span("Markdown: seeded content");
        markdownStatus.setId("framework-markdown-status");
        AtomicInteger markdownClickCount = new AtomicInteger();
        markdown.getElement().addEventListener("click", event ->
                markdownStatus.setText("Markdown clicked: count="
                        + markdownClickCount.incrementAndGet()));
        Button markdownServerUpdate = new Button("Update Markdown", event -> {
            markdown.setContent("""
                    ## Flow IPC Markdown patched

                    1. server patch
                    2. `inline-code`
                    """);
            markdown.appendContent("\n\n> appended quote through Flow IPC");
            markdownStatus.setText("Markdown clicked: count=" + markdownClickCount.get()
                    + " | Markdown server state: patched appended");
        });
        markdownServerUpdate.setId("framework-markdown-server-update");

        Badge badge = new Badge("queued", 7, new Icon(VaadinIcon.CLIPBOARD_CHECK));
        badge.setId("framework-badge");
        badge.setRole("status");
        badge.addThemeVariants(BadgeVariant.CONTRAST, BadgeVariant.SMALL);
        Span badgeStatus = new Span("Badge: queued number=7 theme=contrast small");
        badgeStatus.setId("framework-badge-status");
        AtomicInteger badgeClickCount = new AtomicInteger();
        badge.getElement().addEventListener("click", event ->
                badgeStatus.setText("Badge clicked: count="
                        + badgeClickCount.incrementAndGet()
                        + " text=" + badge.getText()
                        + " number=" + badge.getNumber()));
        Button badgeServerUpdate = new Button("Update Badge", event -> {
            badge.setText("approved");
            badge.setNumber(12);
            badge.setIcon(new Icon(VaadinIcon.COG));
            badge.setRole("mark");
            badge.addThemeVariants(BadgeVariant.SUCCESS, BadgeVariant.FILLED);
            badgeStatus.setText("Badge clicked: count=" + badgeClickCount.get()
                    + " | Badge server state: approved number=12 success filled icon=cog role=mark");
        });
        badgeServerUpdate.setId("framework-badge-server-update");

        Dialog detailsDialog = new Dialog();
        detailsDialog.setId("framework-dialog");
        detailsDialog.setHeaderTitle("Flow IPC keyboard dialog");
        detailsDialog.setCloseOnEsc(true);
        detailsDialog.setFocusTrap(true);
        Span dialogContent = new Span("Dialog details pending");
        dialogContent.setId("framework-dialog-content");
        TextField dialogFocusField = new TextField("Dialog focus field");
        dialogFocusField.setId("framework-dialog-focus-field");
        Button dialogLastAction = new Button("Dialog last action");
        dialogLastAction.setId("framework-dialog-last-action");
        Dialog nestedDialog = new Dialog();
        nestedDialog.setId("framework-nested-dialog");
        nestedDialog.setHeaderTitle("Flow IPC nested keyboard dialog");
        nestedDialog.setCloseOnEsc(true);
        nestedDialog.setFocusTrap(true);
        TextField nestedDialogFocusField = new TextField("Nested dialog focus field");
        nestedDialogFocusField.setId("framework-nested-dialog-focus-field");
        Button nestedDialogLastAction = new Button("Nested dialog last action");
        nestedDialogLastAction.setId("framework-nested-dialog-last-action");
        Span nestedDialogLifecycleStatus = new Span("Nested dialog lifecycle: pending");
        nestedDialogLifecycleStatus.setId("framework-nested-dialog-lifecycle-status");
        nestedDialog.addOpenedChangeListener(event -> nestedDialogLifecycleStatus.setText(
                event.isOpened()
                        ? "Nested dialog lifecycle opened: client=" + event.isFromClient()
                        : "Nested dialog lifecycle closed: client=" + event.isFromClient()));
        nestedDialog.add(nestedDialogFocusField, nestedDialogLastAction);
        Button openNestedDialog = new Button("Open nested dialog", event -> {
            nestedDialog.open();
            nestedDialogFocusField.focus();
        });
        openNestedDialog.setId("framework-nested-dialog-open");
        Span dialogLifecycleStatus = new Span("Dialog lifecycle: pending");
        dialogLifecycleStatus.setId("framework-dialog-lifecycle-status");
        detailsDialog.addOpenedChangeListener(event -> dialogLifecycleStatus.setText(
                event.isOpened()
                        ? "Dialog lifecycle opened: client=" + event.isFromClient()
                        : "Dialog lifecycle closed: client=" + event.isFromClient()));
        detailsDialog.add(dialogContent, dialogFocusField, openNestedDialog,
                dialogLastAction);

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
            dialogFocusField.focus();
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

        add(title, standardPushStatus, viteHmrProbe, javaReloadProbe,
                reactorReloadProbe, effectiveMavenProfileProbe, flowRpcProbe,
                openLazyFrontend, openNestedLazyFrontend, openJarFrontend,
                openFlowDnd,
                openFlowOverlayStack, openFlowInputPopup, openFlowMenuPopup,
                openFlowLargeDownload, openFlowLargeUpload,
                openFlowInFlightRequest,
                openFlowLargeMultipartUpload,
                openFlowConcurrentUpload,
                openFlowRequestFanout,
                openFlowConcurrentMultipartUpload,
                openFlowCrossSessionMultipartUpload,
                openFlowCapabilityContextReopen,
                openFlowCapabilityPermission,
                openFlowClipboardOutcomes,
                openFlowNotificationOutcomes,
                openFlowNotificationDelivery,
                openFlowFileDialogGeneration,
                openFlowFileDialogOutcomes,
                openFlowFileReadGrant,
                openFlowFileReadGrantIsolation,
                openFlowFileReadGrantReplay,
                openFlowFileReadGrantRange,
                openFlowFileWriteGrant,
                openFlowFileWriteGrantIsolation,
                openFlowFileWriteGrantRecovery,
                openFlowFileWriteGrantReplay,
                openFlowFileWriteGrantOwnerClose,
                openFlowFileDialogOwnerClose,
                openFlowMainWindowCloseRecovery,
                openFlowLastWindowClose,
                openFlowLazyGridSelection,
                openLazyDataBaseline,
                openFlowMultiSelectComboBox,
                openFlowLazyMultiSelectComboBox,
                openFlowLocalizedNumber,
                openFlowLocalizedNumberValidation,
                openFlowNativeEdit,
                openFlowExternalFileDrop,
                budget, expedited,
                auditChannels, checkboxGroupServerUpdate, checkboxGroupStatus,
                priority, reviewer, dueDate, decision, caseTitle,
                textFieldServerUpdate, textFieldStatus, notes,
                contactEmail, score, riskScore, numberFieldServerUpdate, numberFieldStatus,
                riskTolerance, sliderServerUpdate, sliderStatus,
                secret, reviewTime, reviewDateTime,
                dateTimePickerServerUpdate, dateTimePickerStatus, inputMatrixStatus, assignees,
                assigneesStatus, laneListBox, listBoxServerUpdate, listBoxStatus,
                reviewStageListBox, multiSelectListBoxServerUpdate, multiSelectListBoxStatus,
                customAssignment, customFieldServerUpdate, customFieldStatus,
                splitLayout, splitLayoutServerUpdate, splitLayoutStatus,
                navTabs, auditDetails, queueAccordion, menu, navigationMatrixStatus,
                contextMenuTarget, popoverTarget, popover, popoverLifecycleStatus,
                popoverOutsideTarget, mixedOverlayDialogOpen, mixedOverlayDialog,
                nestedPopoverOuterTarget, nestedPopoverOuter,
                nestedPopoverOuterLifecycleStatus, nestedPopoverOutsideTarget,
                notificationOpen, notificationStatus,
                confirmDialogOpen, confirmDialog, confirmDialogStatus,
                progressBar, progressValueUpdate, progressIndeterminateUpdate, progressBarStatus,
                overlayMatrixStatus, openDialog, detailsDialog, dialogLifecycleStatus,
                nestedDialogLifecycleStatus,
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
                grid, lazyGridFilter, lazyGrid, lazyGridStatus, lazyGridSelectionStatus,
                lazyGridQueryStatus, lazyGridCountStatus, lazyGridReport,
                treeGrid, treeGridRefresh, treeGridQueryStatus, treeGridCountStatus,
                treeGridLifecycleStatus, treeGridSelectionStatus, treeGridRefreshStatus,
                editorGrid, editorRendererStatus, editorOpenStatus, editorSaveStatus,
                editorCancelStatus, editorValidationStatus, litRendererStatus,
                streamResourceDownload,
                streamResourceStatus, streamResourceRangeDownload, streamResourceRangeStatus,
                streamResourceChunksStatus, downloadHandlerSaveOpen, downloadHandlerSaveOpenStatus,
                standardStreamingDownload,
                upload, uploadStatus, flowDndProbe,
                virtualList, virtualListScrollEnd, virtualListStatus,
                formLayout, formLayoutServerUpdate, formLayoutStatus,
                horizontalLayout, horizontalLayoutServerUpdate, horizontalLayoutStatus,
                flexLayout, flexLayoutServerUpdate, flexLayoutStatus,
                verticalLayout, verticalLayoutServerUpdate, verticalLayoutStatus,
                scroller, scrollerServerUpdate, scrollerStatus,
                icon, iconServerUpdate, iconStatus,
                appLayout, appLayoutServerUpdate, appLayoutStatus,
                masterDetailLayout, masterDetailServerUpdate,
                masterDetailAnimationUpdate, masterDetailStatus,
                auditCard, cardServerUpdate, cardStatus,
                markdown, markdownServerUpdate, markdownStatus,
                badge, badgeServerUpdate, badgeStatus,
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

    private static List<WorkItem> queryLazyWorkItems(List<WorkItem> items,
            String filter, List<QuerySortOrder> sortOrders) {
        Comparator<WorkItem> comparator = null;
        for (QuerySortOrder sortOrder : sortOrders) {
            Comparator<WorkItem> propertyComparator = switch (sortOrder.getSorted()) {
                case "id" -> Comparator.comparing(WorkItem::id);
                case "owner" -> Comparator.comparing(WorkItem::owner);
                case "state" -> Comparator.comparing(WorkItem::state);
                default -> null;
            };
            if (propertyComparator == null) {
                continue;
            }
            if (sortOrder.getDirection() == SortDirection.DESCENDING) {
                propertyComparator = propertyComparator.reversed();
            }
            comparator = comparator == null
                    ? propertyComparator
                    : comparator.thenComparing(propertyComparator);
        }
        if (comparator != null) {
            comparator = comparator.thenComparing(WorkItem::id);
        }
        var stream = items.stream()
                .filter(item -> filter.isEmpty()
                        || item.owner().toLowerCase(Locale.ROOT).contains(filter));
        if (comparator != null) {
            stream = stream.sorted(comparator);
        }
        return stream.toList();
    }

    private static String lazyGridSortSummary(List<QuerySortOrder> sortOrders) {
        if (sortOrders.isEmpty()) {
            return "none";
        }
        return sortOrders.stream()
                .map(sortOrder -> sortOrder.getSorted() + ":"
                        + (sortOrder.getDirection() == SortDirection.ASCENDING
                                ? "ascending"
                                : "descending"))
                .collect(Collectors.joining("+"));
    }

    private static String normalizeLazyGridFilter(String filter) {
        return filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
    }

    private static List<TreeWorkItem> treeGridChildren(
            TreeWorkItem parent,
            List<TreeWorkItem> roots,
            Map<String, List<TreeWorkItem>> children) {
        return parent == null
                ? roots
                : children.getOrDefault(parent.id(), List.of());
    }

    private static String treeGridParentId(TreeWorkItem parent) {
        return parent == null ? "root" : parent.id();
    }

    private record WorkItem(String id, String owner, String state) {
    }

    private record TreeWorkItem(String id, String label, String state) {
    }

    public static final class EditableWorkItem {
        private final String id;
        private String owner;
        private String state;

        private EditableWorkItem(String id, String owner, String state) {
            this.id = id;
            this.owner = owner;
            this.state = state;
        }

        public String getId() {
            return id;
        }

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }
    }

    public static final class FlowRpcProbe extends VerticalLayout {
        private final Span clientCallableStatus =
                new Span("ClientCallable: pending");
        private final Span clientCallableStructuredStatus =
                new Span("ClientCallable structured: pending");
        private final Span clientCallableTypeShapesStatus =
                new Span("ClientCallable type shapes: pending");
        private final Span pendingJavaScriptSuccessStatus =
                new Span("Pending JavaScript success: pending");
        private final Span pendingJavaScriptFailureStatus =
                new Span("Pending JavaScript failure: pending");
        private final Span pendingJavaScriptStructuredStatus =
                new Span("Pending JavaScript structured: pending");
        private final Span pendingJavaScriptTypeShapesStatus =
                new Span("Pending JavaScript type shapes: pending");
        private final Span pendingJavaScriptCancellationStatus =
                new Span("Pending JavaScript cancellation: pending");
        private final Span pendingJavaScriptSentCancellationStatus =
                new Span("Pending JavaScript sent cancellation: pending");
        private final Span pendingJavaScriptInvisibleOwner =
                new Span("Pending JavaScript invisible owner");
        private final Span pendingJavaScriptInvisibleOwnerStatus =
                new Span("Pending JavaScript invisible owner: pending");
        private final Span pendingJavaScriptDetachedOwner =
                new Span("Pending JavaScript detached owner");
        private final Span pendingJavaScriptDetachedOwnerStatus =
                new Span("Pending JavaScript detached owner: pending");
        private final Span pendingJavaScriptFutureSuccessStatus =
                new Span("Pending JavaScript future success: pending");
        private final Span pendingJavaScriptFutureFailureStatus =
                new Span("Pending JavaScript future failure: pending");
        private final Span pendingJavaScriptSubscriptionStatus =
                new Span("Pending JavaScript subscriptions: pending");
        private final Span pendingJavaScriptLateSubscriptionStatus =
                new Span("Pending JavaScript late subscription: pending");
        private final Span pendingJavaScriptFutureCancellationStatus =
                new Span("Pending JavaScript future cancellation: pending");
        private PendingJavaScriptResult sentPendingJavaScript;
        private PendingJavaScriptResult lateSubscriptionPendingJavaScript;
        private boolean sentCancellationAttempted;
        private boolean sentBeforeCancellation;
        private boolean sentCancellationAccepted;
        private String sentPendingResult = "pending";
        private int subscriptionCallbackCount;
        private String subscriptionFirstResult = "pending";
        private String subscriptionSecondResult = "pending";
        private String subscriptionFutureResult = "pending";
        private String subscriptionError = "none";
        private boolean lateSubscriptionAttempted;
        private boolean lateSubscriptionSent;
        private boolean lateSubscriptionRejected;
        private int lateSubscriptionCallbackCount;
        private String lateSubscriptionResult = "pending";
        private String lateSubscriptionError = "none";
        private int typeShapeCallbackCount;
        private String typeShapeNumber = "pending";
        private String typeShapeEnum = "pending";
        private String typeShapeArray = "pending";
        private String typeShapeNullable = "pending";
        private String typeShapeNested = "pending";
        private String typeShapeError = "none";

        private FlowRpcProbe() {
            setId("framework-flow-rpc-probe");
            setPadding(false);
            setSpacing(false);

            clientCallableStatus.setId("framework-client-callable-status");
            clientCallableStructuredStatus.setId(
                    "framework-client-callable-structured-status");
            clientCallableTypeShapesStatus.setId(
                    "framework-client-callable-type-shapes-status");
            pendingJavaScriptSuccessStatus.setId(
                    "framework-pending-javascript-success-status");
            pendingJavaScriptFailureStatus.setId(
                    "framework-pending-javascript-failure-status");
            pendingJavaScriptStructuredStatus.setId(
                    "framework-pending-javascript-structured-status");
            pendingJavaScriptTypeShapesStatus.setId(
                    "framework-pending-javascript-type-shapes-status");
            pendingJavaScriptCancellationStatus.setId(
                    "framework-pending-javascript-cancellation-status");
            pendingJavaScriptSentCancellationStatus.setId(
                    "framework-pending-javascript-sent-cancellation-status");
            pendingJavaScriptInvisibleOwner.setId(
                    "framework-pending-javascript-invisible-owner");
            pendingJavaScriptInvisibleOwner.setVisible(false);
            pendingJavaScriptInvisibleOwnerStatus.setId(
                    "framework-pending-javascript-invisible-owner-status");
            pendingJavaScriptDetachedOwner.setId(
                    "framework-pending-javascript-detached-owner");
            pendingJavaScriptDetachedOwner.setVisible(false);
            pendingJavaScriptDetachedOwnerStatus.setId(
                    "framework-pending-javascript-detached-owner-status");
            pendingJavaScriptFutureSuccessStatus.setId(
                    "framework-pending-javascript-future-success-status");
            pendingJavaScriptFutureFailureStatus.setId(
                    "framework-pending-javascript-future-failure-status");
            pendingJavaScriptSubscriptionStatus.setId(
                    "framework-pending-javascript-subscription-status");
            pendingJavaScriptLateSubscriptionStatus.setId(
                    "framework-pending-javascript-late-subscription-status");
            pendingJavaScriptFutureCancellationStatus.setId(
                    "framework-pending-javascript-future-cancellation-status");

            Button pendingJavaScriptSuccess =
                    new Button("Run pending JavaScript success", event ->
                            getElement().executeJs(
                                            "return Promise.resolve('pending-js:' + $0)",
                                            "desktop-rpc")
                                    .then(String.class, value ->
                                                    pendingJavaScriptSuccessStatus.setText(
                                                            "Pending JavaScript resolved: "
                                                                    + value),
                                            error ->
                                                    pendingJavaScriptSuccessStatus.setText(
                                                            "Pending JavaScript unexpected error: "
                                                                    + error)));
            pendingJavaScriptSuccess.setId(
                    "framework-pending-javascript-success");

            Button pendingJavaScriptFailure =
                    new Button("Run pending JavaScript failure", event ->
                            getElement().executeJs(
                                            "return Promise.reject(new Error($0))",
                                            "pending-js-rejected")
                                    .then(String.class, value ->
                                                    pendingJavaScriptFailureStatus.setText(
                                                            "Pending JavaScript unexpected value: "
                                                                    + value),
                                            error ->
                                                    pendingJavaScriptFailureStatus.setText(
                                                            "Pending JavaScript rejected: "
                                                                    + error)));
            pendingJavaScriptFailure.setId(
                    "framework-pending-javascript-failure");

            Button pendingJavaScriptStructured =
                    new Button("Run pending JavaScript structured", event ->
                            getElement().executeJs("""
                                    return Promise.resolve($0.items.map(
                                      (item, index) => ({
                                        code: item.code + '-client',
                                        quantity: item.quantity + index
                                      })));
                                    """,
                                            new StructuredRpcRequest(
                                                    "java-structured",
                                                    true,
                                                    List.of(
                                                            new StructuredRpcItem(
                                                                    "alpha", 2),
                                                            new StructuredRpcItem(
                                                                    "beta", 3))))
                                    .then(
                                            new TypeReference<
                                                    List<StructuredRpcItem>>() {
                                            },
                                            items ->
                                                    pendingJavaScriptStructuredStatus.setText(
                                                            "Pending JavaScript structured: "
                                                                    + summarizeItems(
                                                                            items)
                                                                    + " total="
                                                                    + totalQuantity(
                                                                            items)),
                                            error ->
                                                    pendingJavaScriptStructuredStatus.setText(
                                                            "Pending JavaScript structured error: "
                                                                    + error)));
            pendingJavaScriptStructured.setId(
                    "framework-pending-javascript-structured");

            Button pendingJavaScriptTypeShapes =
                    new Button("Run pending JavaScript type shapes", event -> {
                        typeShapeCallbackCount = 0;
                        typeShapeNumber = "pending";
                        typeShapeEnum = "pending";
                        typeShapeArray = "pending";
                        typeShapeNullable = "pending";
                        typeShapeNested = "pending";
                        typeShapeError = "none";
                        updateTypeShapeStatus();

                        getElement().executeJs("return $0", 42L)
                                .then(Long.class,
                                        value -> completeTypeShape(
                                                "number",
                                                String.valueOf(value)),
                                        error -> failTypeShape(
                                                "number", error));
                        getElement().executeJs(
                                        "return $0", RpcTypeShapeKind.BETA)
                                .then(RpcTypeShapeKind.class,
                                        value -> completeTypeShape(
                                                "enum",
                                                String.valueOf(value)),
                                        error -> failTypeShape(
                                                "enum", error));
                        getElement().executeJs(
                                        "return $0", new int[]{3, 5, 8})
                                .then(int[].class,
                                        value -> completeTypeShape(
                                                "array",
                                                Arrays.toString(value)),
                                        error -> failTypeShape(
                                                "array", error));
                        getElement().executeJs(
                                        "return $0", (String) null)
                                .then(String.class,
                                        value -> completeTypeShape(
                                                "nullable",
                                                String.valueOf(value)),
                                        error -> failTypeShape(
                                                "nullable", error));
                        Map<String, List<Integer>> nested =
                                new LinkedHashMap<>();
                        nested.put("scores", List.of(13, 21));
                        getElement().executeJs("return $0", nested)
                                .then(
                                        new TypeReference<
                                                Map<String, List<Integer>>>() {
                                        },
                                        value -> completeTypeShape(
                                                "nested",
                                                String.valueOf(
                                                        value.get("scores"))),
                                        error -> failTypeShape(
                                                "nested", error));
                    });
            pendingJavaScriptTypeShapes.setId(
                    "framework-pending-javascript-type-shapes");

            Button pendingJavaScriptCancellation =
                    new Button("Cancel pending JavaScript", event -> {
                        AtomicReference<String> cancellationError =
                                new AtomicReference<>("none");
                        PendingJavaScriptResult pending =
                                getElement().executeJs("""
                                        window.VaadinDesktopCanceledRpcExecuted = true;
                                        return 'unexpected';
                                        """);
                        pending.then(String.class,
                                value ->
                                        pendingJavaScriptCancellationStatus.setText(
                                                "Pending JavaScript cancellation unexpected: "
                                                        + value),
                                cancellationError::set);
                        boolean sentBefore = pending.isSentToBrowser();
                        boolean canceled = pending.cancelExecution();
                        pendingJavaScriptCancellationStatus.setText(
                                "Pending JavaScript cancellation: canceled="
                                        + canceled
                                        + " sentBefore=" + sentBefore
                                        + " error=" + cancellationError.get());
                    });
            pendingJavaScriptCancellation.setId(
                    "framework-pending-javascript-cancellation");

            Button pendingJavaScriptSentStart =
                    new Button("Run sent pending JavaScript", event -> {
                        sentCancellationAttempted = false;
                        sentBeforeCancellation = false;
                        sentCancellationAccepted = false;
                        sentPendingResult = "pending";
                        sentPendingJavaScript = getElement().executeJs("""
                                window.VaadinDesktopSentRpcExecuted = true;
                                return new Promise(resolve =>
                                  window.setTimeout(
                                    () => resolve('sent-result'), 1200));
                                """);
                        sentPendingJavaScript.then(String.class, value -> {
                            sentPendingResult = value;
                            updateSentCancellationStatus();
                        }, error -> {
                            sentPendingResult = "error:" + error;
                            updateSentCancellationStatus();
                        });
                        pendingJavaScriptSentCancellationStatus.setText(
                                "Pending JavaScript sent cancellation: started");
                    });
            pendingJavaScriptSentStart.setId(
                    "framework-pending-javascript-sent-start");

            Button pendingJavaScriptSentCancel =
                    new Button("Cancel sent pending JavaScript", event -> {
                        if (sentPendingJavaScript == null) {
                            pendingJavaScriptSentCancellationStatus.setText(
                                    "Pending JavaScript sent cancellation: missing");
                            return;
                        }
                        sentBeforeCancellation =
                                sentPendingJavaScript.isSentToBrowser();
                        sentCancellationAccepted =
                                sentPendingJavaScript.cancelExecution();
                        sentCancellationAttempted = true;
                        updateSentCancellationStatus();
                    });
            pendingJavaScriptSentCancel.setId(
                    "framework-pending-javascript-sent-cancel");

            Button pendingJavaScriptInvisibleOwnerQueue =
                    new Button("Queue invisible owner JavaScript", event -> {
                        pendingJavaScriptInvisibleOwner.setVisible(false);
                        pendingJavaScriptInvisibleOwnerStatus.setText(
                                "Pending JavaScript invisible owner: queued");
                        pendingJavaScriptInvisibleOwner.getElement().executeJs("""
                                window.VaadinDesktopInvisibleOwnerRpcExecuted = true;
                                return Promise.resolve(
                                  'invisible-owner-result');
                                """).then(String.class,
                                value ->
                                        pendingJavaScriptInvisibleOwnerStatus
                                                .setText(
                                                        "Pending JavaScript invisible owner: resolved="
                                                                + value),
                                error ->
                                        pendingJavaScriptInvisibleOwnerStatus
                                                .setText(
                                                        "Pending JavaScript invisible owner: error="
                                                                + error));
                    });
            pendingJavaScriptInvisibleOwnerQueue.setId(
                    "framework-pending-javascript-invisible-owner-queue");

            Button pendingJavaScriptInvisibleOwnerReveal =
                    new Button("Reveal invisible owner", event -> {
                        pendingJavaScriptInvisibleOwner.setVisible(true);
                        pendingJavaScriptInvisibleOwnerStatus.setText(
                                "Pending JavaScript invisible owner: revealed");
                    });
            pendingJavaScriptInvisibleOwnerReveal.setId(
                    "framework-pending-javascript-invisible-owner-reveal");

            VerticalLayout detachedOwnerContainer = new VerticalLayout();
            detachedOwnerContainer.setPadding(false);
            detachedOwnerContainer.setSpacing(false);
            detachedOwnerContainer.setId(
                    "framework-pending-javascript-detached-owner-container");
            detachedOwnerContainer.add(pendingJavaScriptDetachedOwner);

            Button pendingJavaScriptDetachedOwnerQueue =
                    new Button("Queue detached owner JavaScript", event -> {
                        if (pendingJavaScriptDetachedOwner.getParent()
                                .isEmpty()) {
                            detachedOwnerContainer.add(
                                    pendingJavaScriptDetachedOwner);
                        }
                        pendingJavaScriptDetachedOwner.setVisible(false);
                        pendingJavaScriptDetachedOwnerStatus.setText(
                                "Pending JavaScript detached owner: queued");
                        pendingJavaScriptDetachedOwner.getElement().executeJs("""
                                window.VaadinDesktopDetachedOwnerRpcExecuted = true;
                                return Promise.resolve(
                                  'detached-owner-result');
                                """).then(String.class,
                                value ->
                                        pendingJavaScriptDetachedOwnerStatus
                                                .setText(
                                                        "Pending JavaScript detached owner: unexpected="
                                                                + value),
                                error ->
                                        pendingJavaScriptDetachedOwnerStatus
                                                .setText(
                                                        "Pending JavaScript detached owner: error="
                                                                + error));
                    });
            pendingJavaScriptDetachedOwnerQueue.setId(
                    "framework-pending-javascript-detached-owner-queue");

            Button pendingJavaScriptDetachedOwnerDetach =
                    new Button("Detach JavaScript owner", event -> {
                        pendingJavaScriptDetachedOwner.getElement()
                                .removeFromParent();
                        pendingJavaScriptDetachedOwnerStatus.setText(
                                "Pending JavaScript detached owner: detached");
                    });
            pendingJavaScriptDetachedOwnerDetach.setId(
                    "framework-pending-javascript-detached-owner-detach");

            Button pendingJavaScriptDetachedOwnerReattach =
                    new Button("Reattach JavaScript owner", event -> {
                        if (pendingJavaScriptDetachedOwner.getParent()
                                .isEmpty()) {
                            detachedOwnerContainer.add(
                                    pendingJavaScriptDetachedOwner);
                        }
                        pendingJavaScriptDetachedOwner.setVisible(true);
                        pendingJavaScriptDetachedOwnerStatus.setText(
                                "Pending JavaScript detached owner: reattached");
                    });
            pendingJavaScriptDetachedOwnerReattach.setId(
                    "framework-pending-javascript-detached-owner-reattach");

            Button pendingJavaScriptDetachedOwnerConfirm =
                    new Button("Confirm detached owner cleanup", event ->
                            pendingJavaScriptDetachedOwnerStatus.setText(
                                    "Pending JavaScript detached owner: confirmed"));
            pendingJavaScriptDetachedOwnerConfirm.setId(
                    "framework-pending-javascript-detached-owner-confirm");

            Button pendingJavaScriptFutureSuccess =
                    new Button("Run pending JavaScript future success",
                            event -> {
                                PendingJavaScriptResult pending =
                                        getElement().executeJs("""
                                                window.VaadinDesktopFutureRpcExecuted = true;
                                                return Promise.resolve([
                                                  {
                                                    code: 'future-alpha',
                                                    quantity: 7
                                                  },
                                                  {
                                                    code: 'future-beta',
                                                    quantity: 9
                                                  }
                                                ]);
                                                """);
                                CompletableFuture<List<StructuredRpcItem>> future =
                                        pending.toCompletableFuture(
                                                new TypeReference<
                                                        List<StructuredRpcItem>>() {
                                                });
                                boolean initialDone = future.isDone();
                                AtomicBoolean deadlockGuard =
                                        new AtomicBoolean();
                                AtomicReference<String> guardError =
                                        new AtomicReference<>("none");
                                try {
                                    future.get(0, TimeUnit.MILLISECONDS);
                                    guardError.set("missing");
                                } catch (IllegalStateException exception) {
                                    deadlockGuard.set(exception.getMessage()
                                            != null
                                            && exception.getMessage().contains(
                                                    "locked the session"));
                                } catch (Exception exception) {
                                    guardError.set(exception.getClass()
                                            .getSimpleName());
                                }
                                pendingJavaScriptFutureSuccessStatus.setText(
                                        "Pending JavaScript future success: started"
                                                + " initialDone=" + initialDone
                                                + " deadlockGuard="
                                                + deadlockGuard.get()
                                                + " guardError="
                                                + guardError.get());
                                future.whenComplete((items, error) -> {
                                    if (error != null) {
                                        pendingJavaScriptFutureSuccessStatus
                                                .setText(
                                                        "Pending JavaScript future success: error="
                                                                + error);
                                        return;
                                    }
                                    pendingJavaScriptFutureSuccessStatus
                                            .setText(
                                                    "Pending JavaScript future success:"
                                                            + " initialDone="
                                                            + initialDone
                                                            + " deadlockGuard="
                                                            + deadlockGuard.get()
                                                            + " guardError="
                                                            + guardError.get()
                                                            + " items="
                                                            + summarizeItems(
                                                                    items)
                                                            + " total="
                                                            + totalQuantity(
                                                                    items));
                                });
                            });
            pendingJavaScriptFutureSuccess.setId(
                    "framework-pending-javascript-future-success");

            Button pendingJavaScriptFutureFailure =
                    new Button("Run pending JavaScript future failure",
                            event -> {
                                CompletableFuture<String> future =
                                        getElement().executeJs("""
                                                return Promise.reject(
                                                  new Error('future-rejected'));
                                                """).toCompletableFuture(
                                                        String.class);
                                pendingJavaScriptFutureFailureStatus.setText(
                                        "Pending JavaScript future failure: started");
                                future.whenComplete((value, error) -> {
                                    if (error == null) {
                                        pendingJavaScriptFutureFailureStatus
                                                .setText(
                                                        "Pending JavaScript future failure: unexpected="
                                                                + value);
                                        return;
                                    }
                                    pendingJavaScriptFutureFailureStatus
                                            .setText(
                                                    "Pending JavaScript future failure: type="
                                                            + error.getClass()
                                                                    .getSimpleName()
                                                            + " message="
                                                            + error.getMessage());
                                });
                            });
            pendingJavaScriptFutureFailure.setId(
                    "framework-pending-javascript-future-failure");

            Button pendingJavaScriptSubscriptions =
                    new Button("Run pending JavaScript subscriptions",
                            event -> {
                                subscriptionCallbackCount = 0;
                                subscriptionFirstResult = "pending";
                                subscriptionSecondResult = "pending";
                                subscriptionFutureResult = "pending";
                                subscriptionError = "none";
                                PendingJavaScriptResult pending =
                                        getElement().executeJs(
                                                "return Promise.resolve('subscription-result')");
                                pending.then(String.class, value -> {
                                    subscriptionCallbackCount++;
                                    subscriptionFirstResult = value;
                                    updateSubscriptionStatus();
                                }, error -> {
                                    subscriptionError = error;
                                    updateSubscriptionStatus();
                                });
                                pending.then(String.class, value -> {
                                    subscriptionCallbackCount++;
                                    subscriptionSecondResult = value;
                                    updateSubscriptionStatus();
                                }, error -> {
                                    subscriptionError = error;
                                    updateSubscriptionStatus();
                                });
                                pending.toCompletableFuture(String.class)
                                        .whenComplete((value, error) -> {
                                            if (error != null) {
                                                subscriptionError =
                                                        error.getMessage();
                                            } else {
                                                subscriptionFutureResult =
                                                        value;
                                            }
                                            updateSubscriptionStatus();
                                        });
                                updateSubscriptionStatus();
                            });
            pendingJavaScriptSubscriptions.setId(
                    "framework-pending-javascript-subscriptions");

            Button pendingJavaScriptLateSubscriptionStart =
                    new Button("Run late subscription JavaScript", event -> {
                        lateSubscriptionAttempted = false;
                        lateSubscriptionSent = false;
                        lateSubscriptionRejected = false;
                        lateSubscriptionCallbackCount = 0;
                        lateSubscriptionResult = "pending";
                        lateSubscriptionError = "none";
                        lateSubscriptionPendingJavaScript =
                                getElement().executeJs("""
                                        window.VaadinDesktopLateSubscriptionRpcExecuted = true;
                                        return new Promise(resolve =>
                                          window.setTimeout(
                                            () => resolve('late-result'),
                                            1400));
                                        """);
                        lateSubscriptionPendingJavaScript.then(
                                String.class, value -> {
                                    lateSubscriptionResult = value;
                                    updateLateSubscriptionStatus();
                                }, error -> {
                                    lateSubscriptionError = error;
                                    updateLateSubscriptionStatus();
                                });
                        pendingJavaScriptLateSubscriptionStatus.setText(
                                "Pending JavaScript late subscription: started");
                    });
            pendingJavaScriptLateSubscriptionStart.setId(
                    "framework-pending-javascript-late-subscription-start");

            Button pendingJavaScriptLateSubscriptionAttempt =
                    new Button("Attempt late subscription", event -> {
                        if (lateSubscriptionPendingJavaScript == null) {
                            pendingJavaScriptLateSubscriptionStatus.setText(
                                    "Pending JavaScript late subscription: missing");
                            return;
                        }
                        lateSubscriptionSent =
                                lateSubscriptionPendingJavaScript
                                        .isSentToBrowser();
                        try {
                            lateSubscriptionPendingJavaScript.then(
                                    String.class, value -> {
                                        lateSubscriptionCallbackCount++;
                                        updateLateSubscriptionStatus();
                                    }, error -> {
                                        lateSubscriptionCallbackCount++;
                                        lateSubscriptionError = error;
                                        updateLateSubscriptionStatus();
                                    });
                        } catch (IllegalStateException exception) {
                            lateSubscriptionRejected = true;
                            lateSubscriptionError = exception.getMessage();
                        }
                        lateSubscriptionAttempted = true;
                        updateLateSubscriptionStatus();
                    });
            pendingJavaScriptLateSubscriptionAttempt.setId(
                    "framework-pending-javascript-late-subscription-attempt");

            Button pendingJavaScriptFutureCancellation =
                    new Button("Cancel pending JavaScript future", event -> {
                        AtomicReference<String> errorType =
                                new AtomicReference<>("none");
                        AtomicReference<String> errorMessage =
                                new AtomicReference<>("none");
                        PendingJavaScriptResult pending =
                                getElement().executeJs("""
                                        window.VaadinDesktopFutureCanceledRpcExecuted = true;
                                        return 'unexpected';
                                        """);
                        CompletableFuture<String> future =
                                pending.toCompletableFuture(String.class);
                        future.whenComplete((value, error) -> {
                            if (error != null) {
                                errorType.set(
                                        error.getClass().getSimpleName());
                                errorMessage.set(error.getMessage());
                            }
                        });
                        boolean canceled = pending.cancelExecution();
                        pendingJavaScriptFutureCancellationStatus.setText(
                                "Pending JavaScript future cancellation:"
                                        + " canceled=" + canceled
                                        + " done=" + future.isDone()
                                        + " exceptional="
                                        + future.isCompletedExceptionally()
                                        + " type=" + errorType.get()
                                        + " message=" + errorMessage.get());
                    });
            pendingJavaScriptFutureCancellation.setId(
                    "framework-pending-javascript-future-cancellation");

            DisabledClientCallableProbe disabledClientCallableProbe =
                    new DisabledClientCallableProbe();
            InertClientCallableProbe inertClientCallableProbe =
                    new InertClientCallableProbe();
            VerticalLayout inertClientCallableModal =
                    new VerticalLayout();
            inertClientCallableModal.setId(
                    "framework-inert-client-callable-modal");
            inertClientCallableModal.setPadding(false);
            inertClientCallableModal.setSpacing(false);
            Button inertClientCallableModalClose =
                    new Button("Close inert ClientCallable modal",
                            event ->
                                    inertClientCallableModal
                                            .removeFromParent());
            inertClientCallableModalClose.setId(
                    "framework-inert-client-callable-modal-close");
            inertClientCallableModal.add(
                    new Span("Background RPC should remain inert."),
                    inertClientCallableModalClose);
            Button inertClientCallableModalOpen =
                    new Button("Open inert ClientCallable modal",
                            event -> getUI().orElseThrow()
                                    .addModal(inertClientCallableModal));
            inertClientCallableModalOpen.setId(
                    "framework-inert-client-callable-modal-open");

            add(clientCallableStatus, clientCallableStructuredStatus,
                    clientCallableTypeShapesStatus,
                    pendingJavaScriptSuccess,
                    pendingJavaScriptSuccessStatus,
                    pendingJavaScriptFailure,
                    pendingJavaScriptFailureStatus,
                    pendingJavaScriptStructured,
                    pendingJavaScriptStructuredStatus,
                    pendingJavaScriptTypeShapes,
                    pendingJavaScriptTypeShapesStatus,
                    pendingJavaScriptCancellation,
                    pendingJavaScriptCancellationStatus,
                    pendingJavaScriptSentStart,
                    pendingJavaScriptSentCancel,
                    pendingJavaScriptSentCancellationStatus,
                    pendingJavaScriptInvisibleOwnerQueue,
                    pendingJavaScriptInvisibleOwnerReveal,
                    pendingJavaScriptInvisibleOwnerStatus,
                    pendingJavaScriptInvisibleOwner,
                    pendingJavaScriptDetachedOwnerQueue,
                    pendingJavaScriptDetachedOwnerDetach,
                    pendingJavaScriptDetachedOwnerReattach,
                    pendingJavaScriptDetachedOwnerConfirm,
                    pendingJavaScriptDetachedOwnerStatus,
                    detachedOwnerContainer,
                    pendingJavaScriptFutureSuccess,
                    pendingJavaScriptFutureSuccessStatus,
                    pendingJavaScriptFutureFailure,
                    pendingJavaScriptFutureFailureStatus,
                    pendingJavaScriptSubscriptions,
                    pendingJavaScriptSubscriptionStatus,
                    pendingJavaScriptLateSubscriptionStart,
                    pendingJavaScriptLateSubscriptionAttempt,
                    pendingJavaScriptLateSubscriptionStatus,
                    pendingJavaScriptFutureCancellation,
                    pendingJavaScriptFutureCancellationStatus,
                    disabledClientCallableProbe,
                    inertClientCallableModalOpen,
                    inertClientCallableProbe);
        }

        private void updateSentCancellationStatus() {
            if (!sentCancellationAttempted) {
                return;
            }
            pendingJavaScriptSentCancellationStatus.setText(
                    "Pending JavaScript sent cancellation: sent="
                            + sentBeforeCancellation
                            + " canceled=" + sentCancellationAccepted
                            + " result=" + sentPendingResult);
        }

        private void updateSubscriptionStatus() {
            pendingJavaScriptSubscriptionStatus.setText(
                    "Pending JavaScript subscriptions: callbacks="
                            + subscriptionCallbackCount
                            + " first=" + subscriptionFirstResult
                            + " second=" + subscriptionSecondResult
                            + " future=" + subscriptionFutureResult
                            + " error=" + subscriptionError);
        }

        private void updateLateSubscriptionStatus() {
            if (!lateSubscriptionAttempted) {
                return;
            }
            pendingJavaScriptLateSubscriptionStatus.setText(
                    "Pending JavaScript late subscription: sent="
                            + lateSubscriptionSent
                            + " rejected=" + lateSubscriptionRejected
                            + " lateCallbacks="
                            + lateSubscriptionCallbackCount
                            + " result=" + lateSubscriptionResult
                            + " error=" + lateSubscriptionError);
        }

        private void completeTypeShape(String type, String value) {
            switch (type) {
                case "number" -> typeShapeNumber = value;
                case "enum" -> typeShapeEnum = value;
                case "array" -> typeShapeArray = value;
                case "nullable" -> typeShapeNullable = value;
                case "nested" -> typeShapeNested = value;
                default -> throw new IllegalArgumentException(
                        "Unknown RPC type shape: " + type);
            }
            typeShapeCallbackCount++;
            updateTypeShapeStatus();
        }

        private void failTypeShape(String type, String error) {
            typeShapeError = type + ":" + error;
            typeShapeCallbackCount++;
            updateTypeShapeStatus();
        }

        private void updateTypeShapeStatus() {
            pendingJavaScriptTypeShapesStatus.setText(
                    "Pending JavaScript type shapes: callbacks="
                            + typeShapeCallbackCount
                            + " number=" + typeShapeNumber
                            + " enum=" + typeShapeEnum
                            + " array=" + typeShapeArray
                            + " nullable=" + typeShapeNullable
                            + " nested=" + typeShapeNested
                            + " error=" + typeShapeError);
        }

        @ClientCallable
        public String resolveClientCall(String value) {
            clientCallableStatus.setText("ClientCallable invoked: " + value);
            return "ClientCallable response: " + value;
        }

        @ClientCallable
        public String rejectClientCall(String value) {
            throw new IllegalStateException(
                    "ClientCallable rejected: " + value);
        }

        @ClientCallable
        public StructuredRpcResponse roundTripStructured(
                StructuredRpcRequest request) {
            List<StructuredRpcItem> items =
                    request.getItems() == null ? List.of() : request.getItems();
            int total = totalQuantity(items);
            clientCallableStructuredStatus.setText(
                    "ClientCallable structured: request="
                            + request.getRequestId()
                            + " active=" + request.isActive()
                            + " items=" + summarizeItems(items)
                            + " total=" + total);

            Map<String, Integer> quantities = new LinkedHashMap<>();
            items.forEach(item ->
                    quantities.put(item.getCode(), item.getQuantity()));
            return new StructuredRpcResponse(
                    "server-" + request.getRequestId(),
                    request.isActive(),
                    total,
                    items,
                    quantities);
        }

        @ClientCallable
        public RpcTypeShapeResponse roundTripTypeShapes(
                Long number,
                RpcTypeShapeKind kind,
                int[] samples,
                List<StructuredRpcItem> items,
                int primitiveDefault,
                String nullableValue,
                String... labels) {
            clientCallableTypeShapesStatus.setText(
                    "ClientCallable type shapes: number=" + number
                            + " enum=" + kind
                            + " samples=" + Arrays.toString(samples)
                            + " items=" + summarizeItems(items)
                            + " primitiveDefault=" + primitiveDefault
                            + " nullable=" + nullableValue
                            + " labels=" + Arrays.toString(labels));
            int[] incremented = Arrays.stream(samples)
                    .map(value -> value + 1)
                    .toArray();
            return new RpcTypeShapeResponse(
                    number + 1,
                    RpcTypeShapeKind.ALPHA,
                    incremented,
                    items,
                    primitiveDefault,
                    nullableValue,
                    labels);
        }

        private static int totalQuantity(List<StructuredRpcItem> items) {
            return items.stream()
                    .mapToInt(StructuredRpcItem::getQuantity)
                    .sum();
        }

        private static String summarizeItems(List<StructuredRpcItem> items) {
            return items.stream()
                    .map(item -> item.getCode() + ":" + item.getQuantity())
                    .collect(Collectors.joining(","));
        }

        public static final class StructuredRpcRequest {
            private String requestId;
            private boolean active;
            private List<StructuredRpcItem> items = List.of();

            public StructuredRpcRequest() {
            }

            public StructuredRpcRequest(
                    String requestId,
                    boolean active,
                    List<StructuredRpcItem> items) {
                this.requestId = requestId;
                this.active = active;
                this.items = items;
            }

            public String getRequestId() {
                return requestId;
            }

            public void setRequestId(String requestId) {
                this.requestId = requestId;
            }

            public boolean isActive() {
                return active;
            }

            public void setActive(boolean active) {
                this.active = active;
            }

            public List<StructuredRpcItem> getItems() {
                return items;
            }

            public void setItems(List<StructuredRpcItem> items) {
                this.items = items;
            }
        }

        public static final class StructuredRpcItem {
            private String code;
            private int quantity;

            public StructuredRpcItem() {
            }

            public StructuredRpcItem(String code, int quantity) {
                this.code = code;
                this.quantity = quantity;
            }

            public String getCode() {
                return code;
            }

            public void setCode(String code) {
                this.code = code;
            }

            public int getQuantity() {
                return quantity;
            }

            public void setQuantity(int quantity) {
                this.quantity = quantity;
            }
        }

        public static final class StructuredRpcResponse {
            private final String requestId;
            private final boolean accepted;
            private final int totalQuantity;
            private final List<StructuredRpcItem> items;
            private final Map<String, Integer> quantities;

            public StructuredRpcResponse(
                    String requestId,
                    boolean accepted,
                    int totalQuantity,
                    List<StructuredRpcItem> items,
                    Map<String, Integer> quantities) {
                this.requestId = requestId;
                this.accepted = accepted;
                this.totalQuantity = totalQuantity;
                this.items = items;
                this.quantities = quantities;
            }

            public String getRequestId() {
                return requestId;
            }

            public boolean isAccepted() {
                return accepted;
            }

            public int getTotalQuantity() {
                return totalQuantity;
            }

            public List<StructuredRpcItem> getItems() {
                return items;
            }

            public Map<String, Integer> getQuantities() {
                return quantities;
            }
        }

        public enum RpcTypeShapeKind {
            ALPHA,
            BETA
        }

        public static final class RpcTypeShapeResponse {
            private final Long number;
            private final RpcTypeShapeKind kind;
            private final int[] samples;
            private final List<StructuredRpcItem> items;
            private final int primitiveDefault;
            private final String nullableValue;
            private final String[] labels;

            public RpcTypeShapeResponse(
                    Long number,
                    RpcTypeShapeKind kind,
                    int[] samples,
                    List<StructuredRpcItem> items,
                    int primitiveDefault,
                    String nullableValue,
                    String[] labels) {
                this.number = number;
                this.kind = kind;
                this.samples = samples;
                this.items = items;
                this.primitiveDefault = primitiveDefault;
                this.nullableValue = nullableValue;
                this.labels = labels;
            }

            public Long getNumber() {
                return number;
            }

            public RpcTypeShapeKind getKind() {
                return kind;
            }

            public int[] getSamples() {
                return samples;
            }

            public List<StructuredRpcItem> getItems() {
                return items;
            }

            public int getPrimitiveDefault() {
                return primitiveDefault;
            }

            public String getNullableValue() {
                return nullableValue;
            }

            public String[] getLabels() {
                return labels;
            }
        }

        public static final class DisabledClientCallableProbe
                extends VerticalLayout {
            private final Span status =
                    new Span("Disabled ClientCallable: default=0 always=0");
            private int defaultInvocations;
            private int alwaysInvocations;

            private DisabledClientCallableProbe() {
                setId("framework-disabled-client-callable-probe");
                setPadding(false);
                setSpacing(false);
                status.setId("framework-disabled-client-callable-status");
                add(status);
                setEnabled(false);
            }

            @ClientCallable
            public void invokeWhileDisabledDefault() {
                defaultInvocations++;
                updateStatus();
            }

            @ClientCallable(DisabledUpdateMode.ALWAYS)
            public void invokeWhileDisabledAlways() {
                alwaysInvocations++;
                updateStatus();
            }

            private void updateStatus() {
                status.setText("Disabled ClientCallable: default="
                        + defaultInvocations + " always=" + alwaysInvocations);
            }
        }

        public static final class InertClientCallableProbe
                extends VerticalLayout {
            private final Span status =
                    new Span("Inert ClientCallable: default=0 allowed=0"
                            + " last=none");
            private int defaultInvocations;
            private int allowedInvocations;

            private InertClientCallableProbe() {
                setId("framework-inert-client-callable-probe");
                setPadding(false);
                setSpacing(false);
                status.setId("framework-inert-client-callable-status");
                add(status);
            }

            @ClientCallable
            public String invokeDefault(String value) {
                defaultInvocations++;
                String result = "inert-default:" + value + ":"
                        + defaultInvocations;
                updateStatus(result);
                return result;
            }

            @ClientCallable
            @AllowInert
            public String invokeAllowed(String value) {
                allowedInvocations++;
                String result = "inert-allowed:" + value + ":"
                        + allowedInvocations;
                updateStatus(result);
                return result;
            }

            private void updateStatus(String result) {
                status.setText("Inert ClientCallable: default="
                        + defaultInvocations + " allowed="
                        + allowedInvocations + " last=" + result);
            }
        }
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
