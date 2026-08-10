package dev.vatori.ipcframework;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Route("flow-menu-popup")
public final class FlowMenuPopupRoute extends VerticalLayout {
    public FlowMenuPopupRoute() {
        setId("framework-flow-menu-popup-route");
        setPadding(true);
        setSpacing(true);

        Span menuStatus = new Span("Menu popup MenuBar action: pending");
        menuStatus.setId("framework-flow-menu-popup-menubar-status");
        AtomicInteger menuActions = new AtomicInteger();
        Span nestedMenuStatus =
                new Span("Menu popup nested MenuBar action: pending");
        nestedMenuStatus.setId(
                "framework-flow-menu-popup-nested-menubar-status");
        AtomicInteger nestedMenuActions = new AtomicInteger();

        MenuBar menuBar = new MenuBar();
        menuBar.setId("framework-flow-menu-popup-menubar");
        MenuItem operations = menuBar.addItem("Operations");
        operations.setId("framework-flow-menu-popup-menubar-owner");
        operations.getSubMenu().addItem("Approve", event ->
                menuStatus.setText(
                        "Menu popup MenuBar action: approve client="
                                + event.isFromClient()
                                + " change=" + menuActions.incrementAndGet()))
                .setId("framework-flow-menu-popup-menubar-approve");
        operations.getSubMenu().addItem("Escalate", event ->
                menuStatus.setText(
                        "Menu popup MenuBar action: escalate client="
                                + event.isFromClient()
                                + " change=" + menuActions.incrementAndGet()))
                .setId("framework-flow-menu-popup-menubar-escalate");
        MenuItem delivery = operations.getSubMenu().addItem("Delivery");
        delivery.setId("framework-flow-menu-popup-menubar-delivery");
        delivery.getSubMenu().addItem("Canary", event ->
                nestedMenuStatus.setText(
                        "Menu popup nested MenuBar action: canary client="
                                + event.isFromClient()
                                + " change="
                                + nestedMenuActions.incrementAndGet()))
                .setId("framework-flow-menu-popup-menubar-canary");
        delivery.getSubMenu().addItem("Production", event ->
                nestedMenuStatus.setText(
                        "Menu popup nested MenuBar action: production client="
                                + event.isFromClient()
                                + " change="
                                + nestedMenuActions.incrementAndGet()))
                .setId("framework-flow-menu-popup-menubar-production");

        Span overflowMenuStatus =
                new Span("Menu popup overflow MenuBar action: pending");
        overflowMenuStatus.setId(
                "framework-flow-menu-popup-overflow-menubar-status");
        AtomicInteger overflowMenuActions = new AtomicInteger();
        Span overflowNestedMenuStatus = new Span(
                "Menu popup overflow nested MenuBar action: pending");
        overflowNestedMenuStatus.setId(
                "framework-flow-menu-popup-overflow-nested-status");
        AtomicInteger overflowNestedMenuActions = new AtomicInteger();
        MenuBar overflowMenuBar = new MenuBar();
        overflowMenuBar.setId(
                "framework-flow-menu-popup-overflow-menubar");
        overflowMenuBar.setWidth("260px");
        addOverflowMenuItem(overflowMenuBar, overflowMenuStatus,
                overflowMenuActions, "Dashboard", "dashboard");
        addOverflowMenuItem(overflowMenuBar, overflowMenuStatus,
                overflowMenuActions, "Build pipeline", "build-pipeline");
        addOverflowMenuItem(overflowMenuBar, overflowMenuStatus,
                overflowMenuActions, "Release train", "release-train");
        MenuItem overflowObservability =
                overflowMenuBar.addItem("Observability");
        overflowObservability.setId(
                "framework-flow-menu-popup-overflow-observability");
        addOverflowNestedMenuItem(
                overflowObservability,
                overflowNestedMenuStatus,
                overflowNestedMenuActions,
                "Metrics",
                "metrics");
        addOverflowNestedMenuItem(
                overflowObservability,
                overflowNestedMenuStatus,
                overflowNestedMenuActions,
                "Traces",
                "traces");
        addOverflowMenuItem(overflowMenuBar, overflowMenuStatus,
                overflowMenuActions, "Security review", "security-review");
        addOverflowMenuItem(overflowMenuBar, overflowMenuStatus,
                overflowMenuActions, "Audit log", "audit-log");

        Span contextStatus =
                new Span("Menu popup ContextMenu action: pending");
        contextStatus.setId("framework-flow-menu-popup-context-status");
        Span contextLifecycle =
                new Span("Menu popup ContextMenu lifecycle: pending");
        contextLifecycle.setId(
                "framework-flow-menu-popup-context-lifecycle");
        Span contextNestedStatus =
                new Span("Menu popup nested ContextMenu action: pending");
        contextNestedStatus.setId(
                "framework-flow-menu-popup-context-nested-status");
        AtomicInteger contextActions = new AtomicInteger();
        AtomicInteger contextNestedActions = new AtomicInteger();
        AtomicInteger contextLifecycleChanges = new AtomicInteger();

        Button contextTarget = new Button("Open native context menu");
        contextTarget.setId("framework-flow-menu-popup-context-target");
        ContextMenu contextMenu = new ContextMenu(contextTarget);
        contextMenu.setId("framework-flow-menu-popup-context-menu");
        contextMenu.addOpenedChangeListener(event ->
                contextLifecycle.setText(
                        "Menu popup ContextMenu lifecycle: "
                                + (event.isOpened() ? "opened" : "closed")
                                + " client=" + event.isFromClient()
                                + " change="
                                + contextLifecycleChanges.incrementAndGet()));
        contextMenu.addItem("Inspect", event ->
                contextStatus.setText(
                        "Menu popup ContextMenu action: inspect client="
                                + event.isFromClient()
                                + " change="
                                + contextActions.incrementAndGet()))
                .setId("framework-flow-menu-popup-context-inspect");
        contextMenu.addItem("Archive", event ->
                contextStatus.setText(
                        "Menu popup ContextMenu action: archive client="
                                + event.isFromClient()
                                + " change="
                                + contextActions.incrementAndGet()))
                .setId("framework-flow-menu-popup-context-archive");
        MenuItem contextShare = contextMenu.addItem("Share");
        contextShare.setId("framework-flow-menu-popup-context-share");
        contextShare.getSubMenu().addItem("Copy link", event ->
                contextNestedStatus.setText(
                        "Menu popup nested ContextMenu action: copy-link"
                                + " client=" + event.isFromClient()
                                + " change="
                                + contextNestedActions.incrementAndGet()))
                .setId(
                        "framework-flow-menu-popup-context-share-copy-link");
        contextShare.getSubMenu().addItem("Export report", event ->
                contextNestedStatus.setText(
                        "Menu popup nested ContextMenu action: export-report"
                                + " client=" + event.isFromClient()
                                + " change="
                                + contextNestedActions.incrementAndGet()))
                .setId(
                        "framework-flow-menu-popup-context-share-export-report");

        Button outsideFocusTarget = new Button("Menu popup outside target");
        outsideFocusTarget.setId(
                "framework-flow-menu-popup-outside-target");

        Span gridOpenStatus =
                new Span("Menu popup GridContextMenu open: pending");
        gridOpenStatus.setId("framework-flow-menu-popup-grid-open-status");
        Span gridActionStatus =
                new Span("Menu popup GridContextMenu action: pending");
        gridActionStatus.setId(
                "framework-flow-menu-popup-grid-action-status");
        Span gridNestedActionStatus = new Span(
                "Menu popup nested GridContextMenu action: pending");
        gridNestedActionStatus.setId(
                "framework-flow-menu-popup-grid-nested-action-status");
        Span gridLifecycleStatus =
                new Span("Menu popup GridContextMenu lifecycle: pending");
        gridLifecycleStatus.setId(
                "framework-flow-menu-popup-grid-lifecycle-status");
        AtomicInteger gridOpenChanges = new AtomicInteger();
        AtomicInteger gridActions = new AtomicInteger();
        AtomicInteger gridNestedActions = new AtomicInteger();
        AtomicInteger gridLifecycleChanges = new AtomicInteger();

        Grid<MenuRow> grid = new Grid<>(MenuRow.class, false);
        grid.setId("framework-flow-menu-popup-grid");
        grid.setWidth("680px");
        grid.setHeight("240px");
        Grid.Column<MenuRow> ticketColumn =
                grid.addColumn(MenuRow::ticket).setHeader("Ticket")
                        .setKey("ticket");
        ticketColumn.getElement().setProperty("id", "ticket");
        Grid.Column<MenuRow> ownerColumn =
                grid.addColumn(MenuRow::owner).setHeader("Owner")
                        .setKey("owner");
        ownerColumn.getElement().setProperty("id", "owner");
        Grid.Column<MenuRow> stateColumn =
                grid.addColumn(MenuRow::state).setHeader("State")
                        .setKey("state");
        stateColumn.getElement().setProperty("id", "state");
        grid.setItems(IntStream.rangeClosed(1, 180)
                .mapToObj(index -> new MenuRow(
                        "M-" + String.format("%03d", index),
                        switch (index % 3) {
                            case 0 -> "platform";
                            case 1 -> "desktop";
                            default -> "audit";
                        },
                        "queue-" + ((index - 1) / 20)))
                .toList());

        GridContextMenu<MenuRow> gridContextMenu =
                grid.addContextMenu();
        gridContextMenu.setId(
                "framework-flow-menu-popup-grid-context-menu");
        gridContextMenu.addGridContextMenuOpenedListener(event -> {
            String item = event.getItem()
                    .map(MenuRow::ticket)
                    .orElse("none");
            String column = event.getColumnId().orElse("none");
            gridLifecycleStatus.setText(
                    "Menu popup GridContextMenu lifecycle: "
                            + (event.isOpened() ? "opened" : "closed")
                            + " item=" + item
                            + " column=" + column
                            + " client=" + event.isFromClient()
                            + " change="
                            + gridLifecycleChanges.incrementAndGet());
            if (event.isOpened()) {
                gridOpenStatus.setText(
                        "Menu popup GridContextMenu open: item=" + item
                                + " column=" + column
                                + " client=" + event.isFromClient()
                                + " change="
                                + gridOpenChanges.incrementAndGet());
            }
        });
        gridContextMenu.addItem("Inspect row", event ->
                gridActionStatus.setText(
                        "Menu popup GridContextMenu action: inspect item="
                                + event.getItem()
                                        .map(MenuRow::ticket)
                                        .orElse("none")
                                + " client=" + event.isFromClient()
                                + " change="
                                + gridActions.incrementAndGet()))
                .setId("framework-flow-menu-popup-grid-inspect");
        gridContextMenu.addItem("Archive row", event ->
                gridActionStatus.setText(
                        "Menu popup GridContextMenu action: archive item="
                                + event.getItem()
                                        .map(MenuRow::ticket)
                                        .orElse("none")
                                + " client=" + event.isFromClient()
                                + " change="
                                + gridActions.incrementAndGet()))
                .setId("framework-flow-menu-popup-grid-archive");
        GridMenuItem<MenuRow> gridManage =
                gridContextMenu.addItem("Manage row");
        gridManage.setId("framework-flow-menu-popup-grid-manage");
        gridManage.getSubMenu().addItem("Assign desktop", event ->
                gridNestedActionStatus.setText(
                        "Menu popup nested GridContextMenu action: assign"
                                + " item="
                                + event.getItem()
                                        .map(MenuRow::ticket)
                                        .orElse("none")
                                + " client=" + event.isFromClient()
                                + " change="
                                + gridNestedActions.incrementAndGet()))
                .setId(
                        "framework-flow-menu-popup-grid-manage-assign");
        gridManage.getSubMenu().addItem("Escalate audit", event ->
                gridNestedActionStatus.setText(
                        "Menu popup nested GridContextMenu action: escalate"
                                + " item="
                                + event.getItem()
                                        .map(MenuRow::ticket)
                                        .orElse("none")
                                + " client=" + event.isFromClient()
                                + " change="
                                + gridNestedActions.incrementAndGet()))
                .setId(
                        "framework-flow-menu-popup-grid-manage-escalate");

        Span gridPolicyStatus = new Span(
                "Menu popup GridContextMenu policy: pending");
        gridPolicyStatus.setId(
                "framework-flow-menu-popup-grid-policy-status");
        Span gridPolicyLifecycleStatus = new Span(
                "Menu popup GridContextMenu policy lifecycle: pending");
        gridPolicyLifecycleStatus.setId(
                "framework-flow-menu-popup-grid-policy-lifecycle-status");
        Span gridPolicyActionStatus = new Span(
                "Menu popup GridContextMenu policy action: pending");
        gridPolicyActionStatus.setId(
                "framework-flow-menu-popup-grid-policy-action-status");
        AtomicInteger gridPolicyChanges = new AtomicInteger();
        AtomicInteger gridPolicyLifecycleChanges = new AtomicInteger();
        AtomicInteger gridPolicyActions = new AtomicInteger();

        Grid<MenuRow> gridPolicy = new Grid<>(MenuRow.class, false);
        gridPolicy.setId("framework-flow-menu-popup-grid-policy");
        gridPolicy.setWidth("680px");
        gridPolicy.setHeight("220px");
        Grid.Column<MenuRow> policyTicketColumn =
                gridPolicy.addColumn(MenuRow::ticket)
                        .setHeader("Policy ticket")
                        .setKey("policy-ticket");
        policyTicketColumn.getElement().setProperty(
                "id", "policy-ticket");
        gridPolicy.addColumn(MenuRow::owner)
                .setHeader("Policy owner")
                .setKey("policy-owner");
        gridPolicy.setItems(new MenuRow(
                "P-001", "desktop", "policy-probe"));

        GridContextMenu<MenuRow> gridPolicyContextMenu =
                gridPolicy.addContextMenu();
        gridPolicyContextMenu.setId(
                "framework-flow-menu-popup-grid-policy-context-menu");
        gridPolicyContextMenu.addGridContextMenuOpenedListener(event -> {
            String item = event.getItem()
                    .map(MenuRow::ticket)
                    .orElse("none");
            String column = event.getColumnId().orElse("none");
            gridPolicyLifecycleStatus.setText(
                    "Menu popup GridContextMenu policy lifecycle: "
                            + (event.isOpened() ? "opened" : "closed")
                            + " item=" + item
                            + " column=" + column
                            + " client=" + event.isFromClient()
                            + " change="
                            + gridPolicyLifecycleChanges.incrementAndGet());
        });
        GridMenuItem<MenuRow> gridPolicyInspect =
                gridPolicyContextMenu.addItem(
                        "Inspect policy row",
                        event -> gridPolicyActionStatus.setText(
                                "Menu popup GridContextMenu policy action:"
                                        + " inspect item="
                                        + event.getItem()
                                                .map(MenuRow::ticket)
                                                .orElse("none")
                                        + " client="
                                        + event.isFromClient()
                                        + " change="
                                        + gridPolicyActions
                                                .incrementAndGet()));
        gridPolicyInspect.setId(
                "framework-flow-menu-popup-grid-policy-inspect");
        gridPolicyContextMenu.setDynamicContentHandler(item -> {
            boolean allowed = item != null;
            gridPolicyInspect.setText(allowed
                    ? "Inspect " + item.ticket()
                    : "Inspect policy row");
            gridPolicyStatus.setText(
                    "Menu popup GridContextMenu policy: item="
                            + (allowed ? item.ticket() : "none")
                            + " allowed=" + allowed
                            + " change="
                            + gridPolicyChanges.incrementAndGet());
            return allowed;
        });

        Button returnToFramework = new Button("Return to framework", event ->
                event.getSource().getUI()
                        .ifPresent(ui ->
                                ui.navigate(FlowOverIpcFrameworkView.class)));
        returnToFramework.setId("framework-flow-menu-popup-return");

        add(
                new H1("Flow owner-bound menu popup acceptance"),
                menuBar,
                menuStatus,
                nestedMenuStatus,
                overflowMenuBar,
                overflowMenuStatus,
                overflowNestedMenuStatus,
                contextTarget,
                contextStatus,
                contextLifecycle,
                contextNestedStatus,
                outsideFocusTarget,
                grid,
                gridOpenStatus,
                gridActionStatus,
                gridNestedActionStatus,
                gridLifecycleStatus,
                gridPolicy,
                gridPolicyStatus,
                gridPolicyLifecycleStatus,
                gridPolicyActionStatus,
                returnToFramework);
    }

    private static void addOverflowMenuItem(
            MenuBar menuBar,
            Span status,
            AtomicInteger actions,
            String label,
            String action) {
        menuBar.addItem(label, event ->
                status.setText(
                        "Menu popup overflow MenuBar action: " + action
                                + " client=" + event.isFromClient()
                                + " change=" + actions.incrementAndGet()))
                .setId("framework-flow-menu-popup-overflow-" + action);
    }

    private static void addOverflowNestedMenuItem(
            MenuItem parent,
            Span status,
            AtomicInteger actions,
            String label,
            String action) {
        parent.getSubMenu().addItem(label, event ->
                status.setText(
                        "Menu popup overflow nested MenuBar action: " + action
                                + " client=" + event.isFromClient()
                                + " change=" + actions.incrementAndGet()))
                .setId(
                        "framework-flow-menu-popup-overflow-observability-"
                                + action);
    }

    private record MenuRow(String ticket, String owner, String state) {
    }
}
