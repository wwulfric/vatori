package dev.vatori.ipcframework;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.grid.dataview.GridLazyDataView;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.SelectionPreservationMode;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataChangeEvent;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Route("flow-lazy-grid-selection")
public final class FlowLazyGridSelectionRoute extends VerticalLayout {
    public FlowLazyGridSelectionRoute() {
        setId("framework-flow-lazy-grid-selection-route");
        setPadding(true);
        setSpacing(true);

        List<WorkItem> workItems = new ArrayList<>(
                IntStream.rangeClosed(1, 80)
                        .mapToObj(index -> new WorkItem(
                                "L-%03d".formatted(index),
                                switch (index % 3) {
                                    case 0 -> "platform";
                                    case 1 -> "desktop";
                                    default -> "audit";
                                },
                                switch (index % 4) {
                                    case 0 -> "ready";
                                    case 1 -> "queued";
                                    case 2 -> "active";
                                    default -> "review";
                                }))
                        .toList());

        Grid<WorkItem> grid = new Grid<>(WorkItem.class, false);
        grid.setId("framework-flow-lazy-grid-selection-grid");
        grid.addColumn(WorkItem::id).setHeader("Ticket")
                .setKey("id").setSortProperty("id").setSortable(true);
        grid.addColumn(WorkItem::owner).setHeader("Lazy owner")
                .setKey("owner").setSortProperty("owner").setSortable(true);
        grid.addColumn(WorkItem::state).setHeader("State")
                .setKey("state").setSortProperty("state").setSortable(true);
        grid.setPageSize(5);
        grid.setHeight("220px");
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.setSelectionPreservationMode(SelectionPreservationMode.PRESERVE_ALL);

        Span fetchStatus = new Span(
                "Lazy grid query: pending selected=none");
        fetchStatus.setId("framework-flow-lazy-grid-selection-query-status");
        Span countStatus = new Span(
                "Lazy Grid selection count: pending selected=none");
        countStatus.setId("framework-flow-lazy-grid-selection-count-status");
        Span selectionStatus = new Span(
                "Lazy grid selection: none; change=0, added=none,"
                        + " removed=none, client=false");
        selectionStatus.setId("framework-flow-lazy-grid-selection-status");
        Span refreshStatus = new Span("Lazy grid refresh: pending");
        refreshStatus.setId(
                "framework-flow-lazy-grid-selection-refresh-status");
        Span refreshAllStatus = new Span(
                "Lazy grid refresh all: pending");
        refreshAllStatus.setId(
                "framework-flow-lazy-grid-selection-refresh-all-status");
        Span removalStatus = new Span("Lazy grid selected removal: pending");
        removalStatus.setId(
                "framework-flow-lazy-grid-selection-removal-status");
        Span selectionHistoryStatus = new Span(
                "Lazy grid selection history: pending");
        selectionHistoryStatus.setId(
                "framework-flow-lazy-grid-selection-selection-history");
        List<String> selectionHistory = new ArrayList<>();
        Button removeSelected = new Button("Delete selected L-060");
        removeSelected.setId(
                "framework-flow-lazy-grid-selection-remove-selected");
        removeSelected.setEnabled(false);
        AtomicInteger fetchCount = new AtomicInteger();
        AtomicInteger countCount = new AtomicInteger();
        AtomicInteger selectionChangeCount = new AtomicInteger();
        AtomicInteger refreshChangeCount = new AtomicInteger();
        AtomicInteger refreshAllChangeCount = new AtomicInteger();
        AtomicBoolean refreshAllEventArmed = new AtomicBoolean();
        AtomicInteger refreshAllDataChangeEvents = new AtomicInteger();
        AtomicInteger refreshAllDataRefreshEvents = new AtomicInteger();
        AtomicInteger removalChanges = new AtomicInteger();
        AtomicBoolean removalEventArmed = new AtomicBoolean();
        AtomicInteger removalDataChangeEvents = new AtomicInteger();
        AtomicInteger removalDataRefreshEvents = new AtomicInteger();

        CallbackDataProvider<WorkItem, String> dataProvider =
                new CallbackDataProvider<>(query -> {
                    String filter = normalizeFilter(
                            query.getFilter().orElse(""));
                    List<WorkItem> matchingItems = queryItems(
                            workItems, filter, query.getSortOrders());
                    int offset = query.getOffset();
                    int end = Math.min(
                            offset + query.getLimit(), matchingItems.size());
                    fetchStatus.setText("Lazy grid query "
                            + fetchCount.incrementAndGet()
                            + ": range=" + offset + "-" + end
                            + " sort=" + sortSummary(query.getSortOrders())
                            + " filter=" + displayFilter(filter)
                            + " matches=" + matchingItems.size()
                            + " selected="
                            + selectedSummary(grid.getSelectedItems()));
                    return matchingItems.stream()
                            .skip(offset)
                            .limit(query.getLimit());
                }, query -> {
                    String filter = normalizeFilter(
                            query.getFilter().orElse(""));
                    int matches = queryItems(
                            workItems, filter, query.getSortOrders()).size();
                    countStatus.setText("Lazy Grid selection count "
                            + countCount.incrementAndGet()
                            + ": filter=" + displayFilter(filter)
                            + " matches=" + matches
                            + " selected="
                            + selectedSummary(grid.getSelectedItems()));
                    return matches;
                }, WorkItem::id);
        dataProvider.addDataProviderListener(event -> {
            if (removalEventArmed.get()) {
                if (event instanceof DataChangeEvent.DataRefreshEvent<?>) {
                    removalDataRefreshEvents.incrementAndGet();
                } else {
                    removalDataChangeEvents.incrementAndGet();
                }
            }
            if (!refreshAllEventArmed.get()) {
                return;
            }
            if (event instanceof DataChangeEvent.DataRefreshEvent<?>) {
                refreshAllDataRefreshEvents.incrementAndGet();
            } else {
                refreshAllDataChangeEvents.incrementAndGet();
            }
        });
        ConfigurableFilterDataProvider<WorkItem, Void, String>
                configurableDataProvider =
                dataProvider.withConfigurableFilter();
        grid.setDataProvider(configurableDataProvider);

        TextField ownerFilter = new TextField("Owner filter");
        ownerFilter.setId("framework-flow-lazy-grid-selection-filter");
        ownerFilter.setPlaceholder("Filter by owner");
        ownerFilter.setClearButtonVisible(true);
        ownerFilter.setValueChangeMode(ValueChangeMode.EAGER);
        ownerFilter.addValueChangeListener(event ->
                configurableDataProvider.setFilter(
                        normalizeFilter(event.getValue())));

        @SuppressWarnings("unchecked")
        GridMultiSelectionModel<WorkItem> selectionModel =
                (GridMultiSelectionModel<WorkItem>) grid.getSelectionModel();
        selectionModel.addMultiSelectionListener(event -> {
            String entry = "Lazy grid selection: "
                    + selectedSummary(event.getAllSelectedItems())
                    + "; change=" + selectionChangeCount.incrementAndGet()
                    + ", added="
                    + selectedSummary(event.getAddedSelection())
                    + ", removed="
                    + selectedSummary(event.getRemovedSelection())
                    + ", client=" + event.isFromClient();
            selectionStatus.setText(entry);
            selectionHistory.add(entry);
            selectionHistoryStatus.setText("Lazy grid selection history: "
                    + String.join(" | ", selectionHistory));
            removeSelected.setEnabled(removalChanges.get() == 0
                    && "L-060".equals(
                            selectedSummary(event.getAllSelectedItems())));
        });

        removeSelected.addClickListener(event -> {
            WorkItem removed = workItems.stream()
                    .filter(item -> "L-060".equals(item.id()))
                    .findFirst()
                    .orElseThrow();
            String beforeSelected = selectedSummary(grid.getSelectedItems());
            boolean removedSelected = selectionModel.isSelected(removed);
            if (!removedSelected || !"L-060".equals(beforeSelected)
                    || removalChanges.get() != 0) {
                throw new IllegalStateException(
                        "Removal requires only L-060 to be selected");
            }
            int beforeTotal = workItems.size();
            int selectionChangesBefore = selectionChangeCount.get();
            List<String> operations = new ArrayList<>();

            // PRESERVE_ALL intentionally retains selection on refreshAll.
            // Deleting a selected record explicitly clears it through Grid.
            grid.deselect(removed);
            operations.add("deselect");
            String afterDeselect = selectedSummary(grid.getSelectedItems());
            int selectionChangesAfterDeselect = selectionChangeCount.get();
            boolean removeSucceeded = workItems.remove(removed);
            operations.add("remove");
            int afterTotal = workItems.size();
            int fetchBaseline = fetchCount.get();
            int countBaseline = countCount.get();
            removalEventArmed.set(true);
            try {
                configurableDataProvider.refreshAll();
                operations.add("refreshAll");
            } finally {
                removalEventArmed.set(false);
            }
            removalStatus.setText(
                    "Lazy grid selected removal: operation="
                            + String.join(">", operations)
                            + "; policy=" + grid.getSelectionPreservationMode()
                            + ", removed=" + removed.id()
                            + ", removedSelected=" + removedSelected
                            + ", beforeTotal=" + beforeTotal
                            + ", afterTotal=" + afterTotal
                            + ", beforeSelected=" + beforeSelected
                            + ", afterDeselect=" + afterDeselect
                            + ", afterRefresh="
                            + selectedSummary(grid.getSelectedItems())
                            + ", selectionChangesBefore="
                            + selectionChangesBefore
                            + ", selectionChangesAfterDeselect="
                            + selectionChangesAfterDeselect
                            + ", selectionChangesAfterRefresh="
                            + selectionChangeCount.get()
                            + ", removeSucceeded=" + removeSucceeded
                            + ", removalChanges="
                            + removalChanges.incrementAndGet()
                            + ", dataChangeEvents="
                            + removalDataChangeEvents.get()
                            + ", dataRefreshEvents="
                            + removalDataRefreshEvents.get()
                            + ", fetchBaseline=" + fetchBaseline
                            + ", countBaseline=" + countBaseline
                            + ", client=" + event.isFromClient());
            event.getSource().setEnabled(false);
        });

        Button refreshItem = new Button("Refresh L-060", event -> {
            int itemIndex = IntStream.range(0, workItems.size())
                    .filter(index -> "L-060".equals(
                            workItems.get(index).id()))
                    .findFirst()
                    .orElseThrow();
            WorkItem previous = workItems.get(itemIndex);
            WorkItem replacement = new WorkItem(
                    previous.id(), previous.owner(), "refreshed");
            workItems.set(itemIndex, replacement);

            boolean stableId = dataProvider.getId(previous)
                    .equals(dataProvider.getId(replacement));
            boolean selected = selectionModel.isSelected(replacement);
            configurableDataProvider.refreshItem(replacement);
            refreshStatus.setText("Lazy grid refresh: id=L-060;"
                    + " previous=" + previous.state()
                    + ", current=" + replacement.state()
                    + ", sameInstance=" + (previous == replacement)
                    + ", equals=" + previous.equals(replacement)
                    + ", stableId=" + stableId
                    + ", selected=" + selected
                    + ", selectionChanges="
                    + selectionChangeCount.get()
                    + ", refreshChanges="
                    + refreshChangeCount.incrementAndGet()
                    + ", client=" + event.isFromClient()
                    + ", operation=refreshItem");
            event.getSource().setEnabled(false);
        });
        refreshItem.setId(
                "framework-flow-lazy-grid-selection-refresh-item");

        Button refreshAll = new Button(
                "Remove L-003 and Refresh All", event -> {
                    int beforeTotal = workItems.size();
                    int beforeFiltered = ownerCount(workItems, "platform");
                    WorkItem removed = workItems.stream()
                            .filter(item -> "L-003".equals(item.id()))
                            .findFirst()
                            .orElseThrow();
                    workItems.remove(removed);
                    int afterTotal = workItems.size();
                    int afterFiltered = ownerCount(workItems, "platform");

                    refreshAllDataChangeEvents.set(0);
                    refreshAllDataRefreshEvents.set(0);
                    refreshAllEventArmed.set(true);
                    try {
                        configurableDataProvider.refreshAll();
                    } finally {
                        refreshAllEventArmed.set(false);
                    }
                    refreshAllStatus.setText(
                            "Lazy grid refresh all: operation=refreshAll;"
                                    + " removed=" + removed.id()
                                    + ", beforeTotal=" + beforeTotal
                                    + ", afterTotal=" + afterTotal
                                    + ", beforeFiltered=" + beforeFiltered
                                    + ", afterFiltered=" + afterFiltered
                                    + ", selected="
                                    + selectedSummary(grid.getSelectedItems())
                                    + ", selectionChanges="
                                    + selectionChangeCount.get()
                                    + ", refreshAllChanges="
                                    + refreshAllChangeCount.incrementAndGet()
                                    + ", dataChangeEvents="
                                    + refreshAllDataChangeEvents.get()
                                    + ", dataRefreshEvents="
                                    + refreshAllDataRefreshEvents.get()
                                    + ", client=" + event.isFromClient());
                    event.getSource().setEnabled(false);
                });
        refreshAll.setId(
                "framework-flow-lazy-grid-selection-refresh-all");

        List<WorkItem> unknownSizeItems = new ArrayList<>(
                IntStream.rangeClosed(1, 67)
                        .mapToObj(FlowLazyGridSelectionRoute::unknownWorkItem)
                        .toList());

        Grid<WorkItem> unknownSizeGrid = new Grid<>(WorkItem.class, false);
        unknownSizeGrid.setId("framework-flow-unknown-size-grid");
        unknownSizeGrid.addColumn(WorkItem::id).setHeader("Ticket")
                .setKey("id");
        unknownSizeGrid.addColumn(WorkItem::owner).setHeader("Owner")
                .setKey("owner");
        unknownSizeGrid.addColumn(WorkItem::state).setHeader("State")
                .setKey("state");
        unknownSizeGrid.setPageSize(5);
        unknownSizeGrid.setHeight("220px");
        unknownSizeGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        unknownSizeGrid.setSelectionPreservationMode(
                SelectionPreservationMode.PRESERVE_ALL);

        Span unknownFetchStatus = new Span(
                "Unknown-size Grid fetch: pending");
        unknownFetchStatus.setId(
                "framework-flow-unknown-size-grid-fetch-status");
        Span unknownCountStatus = new Span(
                "Unknown-size Grid count: pending");
        unknownCountStatus.setId(
                "framework-flow-unknown-size-grid-count-status");
        Span unknownSelectionStatus = new Span(
                "Unknown-size Grid selection: none; change=0, added=none,"
                        + " removed=none, client=false");
        unknownSelectionStatus.setId(
                "framework-flow-unknown-size-grid-selection-status");
        Span unknownMutationStatus = new Span(
                "Unknown-size Grid mutation: pending");
        unknownMutationStatus.setId(
                "framework-flow-unknown-size-grid-mutation-status");
        Span unknownShrinkStatus = new Span(
                "Unknown-size Grid shrink: pending");
        unknownShrinkStatus.setId(
                "framework-flow-unknown-size-grid-shrink-status");
        Button unknownShrinkRefreshAll = new Button(
                "Remove U-073..U-093 and Refresh All");
        unknownShrinkRefreshAll.setId(
                "framework-flow-unknown-size-grid-shrink-refresh-all");
        unknownShrinkRefreshAll.setEnabled(false);

        AtomicInteger unknownFetchCount = new AtomicInteger();
        AtomicInteger unknownMaxEnd = new AtomicInteger();
        AtomicBoolean unknownCrossedInitial = new AtomicBoolean();
        AtomicBoolean unknownTerminalShort = new AtomicBoolean();
        AtomicInteger unknownTerminalReturned = new AtomicInteger(-1);
        AtomicInteger unknownTerminalLimit = new AtomicInteger(-1);
        AtomicInteger unknownTerminalEnd = new AtomicInteger(-1);
        String[] unknownTerminalLast = { "none" };
        AtomicBoolean unknownMutationActive = new AtomicBoolean();
        AtomicInteger unknownMutationMaxEnd = new AtomicInteger();
        AtomicBoolean unknownMutationCrossedPriorExact = new AtomicBoolean();
        AtomicBoolean unknownMutationTerminalShort = new AtomicBoolean();
        AtomicInteger unknownMutationTerminalReturned =
                new AtomicInteger(-1);
        AtomicInteger unknownMutationTerminalLimit = new AtomicInteger(-1);
        AtomicInteger unknownMutationTerminalEnd = new AtomicInteger(-1);
        String[] unknownMutationTerminalLast = { "none" };
        AtomicBoolean unknownShrinkActive = new AtomicBoolean();
        AtomicInteger unknownShrinkFetchCount = new AtomicInteger();
        AtomicInteger unknownShrinkMaxEnd = new AtomicInteger();
        AtomicBoolean unknownShrinkCrossedInitial = new AtomicBoolean();
        AtomicBoolean unknownShrinkTerminalShort = new AtomicBoolean();
        AtomicInteger unknownShrinkTerminalReturned = new AtomicInteger(-1);
        AtomicInteger unknownShrinkTerminalLimit = new AtomicInteger(-1);
        AtomicInteger unknownShrinkTerminalEnd = new AtomicInteger(-1);
        String[] unknownShrinkTerminalLast = { "none" };

        GridLazyDataView<WorkItem> unknownSizeDataView =
                unknownSizeGrid.setItems(query -> {
                    int offset = query.getOffset();
                    int limit = query.getLimit();
                    int returned = offset >= unknownSizeItems.size()
                            ? 0
                            : Math.min(limit,
                                    unknownSizeItems.size() - offset);
                    int end = offset + returned;
                    String last = returned == 0
                            ? "none"
                            : unknownSizeItems.get(end - 1).id();
                    if (returned > 0) {
                        unknownMaxEnd.accumulateAndGet(end, Math::max);
                    }
                    if (unknownMaxEnd.get() > 40) {
                        unknownCrossedInitial.set(true);
                    }
                    if (!unknownMutationActive.get()
                            && !unknownTerminalShort.get()
                            && returned > 0
                            && returned < limit
                            && end == unknownSizeItems.size()) {
                        unknownTerminalReturned.set(returned);
                        unknownTerminalLimit.set(limit);
                        unknownTerminalEnd.set(end);
                        unknownTerminalLast[0] = last;
                        unknownTerminalShort.set(true);
                    }
                    if (unknownMutationActive.get()
                            && !unknownShrinkActive.get() && returned > 0) {
                        unknownMutationMaxEnd.accumulateAndGet(end, Math::max);
                        if (unknownMutationMaxEnd.get() > 67) {
                            unknownMutationCrossedPriorExact.set(true);
                        }
                    }
                    if (unknownMutationActive.get()
                            && !unknownShrinkActive.get()
                            && !unknownMutationTerminalShort.get()
                            && returned > 0
                            && returned < limit
                            && end == unknownSizeItems.size()) {
                        unknownMutationTerminalReturned.set(returned);
                        unknownMutationTerminalLimit.set(limit);
                        unknownMutationTerminalEnd.set(end);
                        unknownMutationTerminalLast[0] = last;
                        unknownMutationTerminalShort.set(true);
                    }
                    if (unknownShrinkActive.get()) {
                        unknownShrinkFetchCount.incrementAndGet();
                        if (returned > 0) {
                            unknownShrinkMaxEnd.accumulateAndGet(
                                    end, Math::max);
                            if (unknownShrinkMaxEnd.get() > 40) {
                                unknownShrinkCrossedInitial.set(true);
                            }
                        }
                        if (!unknownShrinkTerminalShort.get()
                                && returned > 0
                                && returned < limit
                                && end == unknownSizeItems.size()) {
                            unknownShrinkTerminalReturned.set(returned);
                            unknownShrinkTerminalLimit.set(limit);
                            unknownShrinkTerminalEnd.set(end);
                            unknownShrinkTerminalLast[0] = last;
                            unknownShrinkTerminalShort.set(true);
                        }
                    }
                    unknownFetchStatus.setText(
                            "Unknown-size Grid fetch: call="
                                    + unknownFetchCount.incrementAndGet()
                                    + ", offset=" + offset
                                    + ", limit=" + limit
                                    + ", returned=" + returned
                                    + ", end=" + end
                                    + ", last=" + last
                                    + ", maxEnd=" + unknownMaxEnd.get()
                                    + ", crossedInitial="
                                    + unknownCrossedInitial.get()
                                    + ", terminalShort="
                                    + unknownTerminalShort.get()
                                    + ", terminalReturned="
                                    + displayUnknownMetric(
                                            unknownTerminalReturned.get())
                                    + ", terminalLimit="
                                    + displayUnknownMetric(
                                            unknownTerminalLimit.get())
                                    + ", terminalEnd="
                                    + displayUnknownMetric(
                                            unknownTerminalEnd.get())
                                    + ", terminalLast="
                                    + unknownTerminalLast[0]
                                    + ", mutationActive="
                                    + unknownMutationActive.get()
                                    + ", mutationMaxEnd="
                                    + unknownMutationMaxEnd.get()
                                    + ", mutationCrossedPriorExact="
                                    + unknownMutationCrossedPriorExact.get()
                                    + ", mutationTerminalShort="
                                    + unknownMutationTerminalShort.get()
                                    + ", mutationTerminalReturned="
                                    + displayUnknownMetric(
                                            unknownMutationTerminalReturned
                                                    .get())
                                    + ", mutationTerminalLimit="
                                    + displayUnknownMetric(
                                            unknownMutationTerminalLimit.get())
                                    + ", mutationTerminalEnd="
                                    + displayUnknownMetric(
                                            unknownMutationTerminalEnd.get())
                                    + ", mutationTerminalLast="
                                    + unknownMutationTerminalLast[0]
                                    + ", shrinkActive="
                                    + unknownShrinkActive.get()
                                    + ", shrinkFetchCount="
                                    + unknownShrinkFetchCount.get()
                                    + ", shrinkMaxEnd="
                                    + unknownShrinkMaxEnd.get()
                                    + ", shrinkCrossedInitial="
                                    + unknownShrinkCrossedInitial.get()
                                    + ", shrinkTerminalShort="
                                    + unknownShrinkTerminalShort.get()
                                    + ", shrinkTerminalReturned="
                                    + displayUnknownMetric(
                                            unknownShrinkTerminalReturned.get())
                                    + ", shrinkTerminalLimit="
                                    + displayUnknownMetric(
                                            unknownShrinkTerminalLimit.get())
                                    + ", shrinkTerminalEnd="
                                    + displayUnknownMetric(
                                            unknownShrinkTerminalEnd.get())
                                    + ", shrinkTerminalLast="
                                    + unknownShrinkTerminalLast[0]);
                    return unknownSizeItems.stream()
                            .skip(offset)
                            .limit(limit);
                });
        unknownSizeDataView.setIdentifierProvider(WorkItem::id);
        unknownSizeDataView.setItemCountEstimate(40);
        unknownSizeDataView.setItemCountEstimateIncrease(40);

        Span unknownConfigStatus = new Span(
                "Unknown-size Grid: fetchOnly=true, estimate="
                        + unknownSizeDataView.getItemCountEstimate()
                        + ", increase="
                        + unknownSizeDataView.getItemCountEstimateIncrease()
                        + ", backendItems=" + unknownSizeItems.size());
        unknownConfigStatus.setId(
                "framework-flow-unknown-size-grid-config-status");

        AtomicBoolean unknownMutationEventArmed = new AtomicBoolean();
        AtomicInteger unknownMutationDataChangeEvents = new AtomicInteger();
        AtomicInteger unknownMutationDataRefreshEvents = new AtomicInteger();
        AtomicBoolean unknownShrinkEventArmed = new AtomicBoolean();
        AtomicInteger unknownShrinkDataChangeEvents = new AtomicInteger();
        AtomicInteger unknownShrinkDataRefreshEvents = new AtomicInteger();
        unknownSizeGrid.getDataProvider().addDataProviderListener(event -> {
            if (unknownShrinkEventArmed.get()) {
                if (event instanceof DataChangeEvent.DataRefreshEvent<?>) {
                    unknownShrinkDataRefreshEvents.incrementAndGet();
                } else {
                    unknownShrinkDataChangeEvents.incrementAndGet();
                }
            }
            if (!unknownMutationEventArmed.get()) {
                return;
            }
            if (event instanceof DataChangeEvent.DataRefreshEvent<?>) {
                unknownMutationDataRefreshEvents.incrementAndGet();
            } else {
                unknownMutationDataChangeEvents.incrementAndGet();
            }
        });

        AtomicInteger unknownCountEvents = new AtomicInteger();
        AtomicInteger unknownMaxEstimated = new AtomicInteger();
        List<String> unknownCountHistory = new ArrayList<>();
        AtomicInteger unknownInitialMaxEstimated = new AtomicInteger(-1);
        AtomicInteger unknownInitialExact = new AtomicInteger(-1);
        String[] unknownInitialHistory = { "none" };
        AtomicInteger unknownMutationResetEstimate = new AtomicInteger(-1);
        AtomicInteger unknownMutationMaxEstimated = new AtomicInteger();
        AtomicInteger unknownMutationExact = new AtomicInteger(-1);
        List<String> unknownMutationHistory = new ArrayList<>();
        AtomicInteger unknownShrinkCountEvents = new AtomicInteger();
        AtomicInteger unknownShrinkResetEstimate = new AtomicInteger(-1);
        AtomicInteger unknownShrinkMaxEstimated = new AtomicInteger();
        AtomicInteger unknownShrinkExact = new AtomicInteger(-1);
        List<String> unknownShrinkHistory = new ArrayList<>();
        unknownSizeDataView.addItemCountChangeListener(event -> {
            if (event.isItemCountEstimated()) {
                unknownMaxEstimated.accumulateAndGet(
                        event.getItemCount(), Math::max);
            }
            unknownCountHistory.add(event.getItemCount()
                    + (event.isItemCountEstimated() ? "E" : "X"));
            if (!unknownMutationActive.get()
                    && !event.isItemCountEstimated()
                    && event.getItemCount() == 67) {
                unknownInitialMaxEstimated.set(unknownMaxEstimated.get());
                unknownInitialExact.set(event.getItemCount());
                unknownInitialHistory[0] = String.join(
                        ">", unknownCountHistory);
            }
            if (unknownMutationActive.get() && !unknownShrinkActive.get()) {
                unknownMutationHistory.add(event.getItemCount()
                        + (event.isItemCountEstimated() ? "E" : "X"));
                if (event.isItemCountEstimated()) {
                    unknownMutationResetEstimate.compareAndSet(
                            -1, event.getItemCount());
                    unknownMutationMaxEstimated.accumulateAndGet(
                            event.getItemCount(), Math::max);
                } else if (event.getItemCount() == unknownSizeItems.size()) {
                    unknownMutationExact.set(event.getItemCount());
                    unknownShrinkRefreshAll.setEnabled(true);
                }
            }
            if (unknownShrinkActive.get()) {
                unknownShrinkCountEvents.incrementAndGet();
                unknownShrinkHistory.add(event.getItemCount()
                        + (event.isItemCountEstimated() ? "E" : "X"));
                if (event.isItemCountEstimated()) {
                    unknownShrinkResetEstimate.compareAndSet(
                            -1, event.getItemCount());
                    unknownShrinkMaxEstimated.accumulateAndGet(
                            event.getItemCount(), Math::max);
                } else if (event.getItemCount() == unknownSizeItems.size()) {
                    unknownShrinkExact.set(event.getItemCount());
                }
            }
            unknownCountStatus.setText(
                    "Unknown-size Grid count: events="
                            + unknownCountEvents.incrementAndGet()
                            + ", current=" + event.getItemCount()
                            + ", estimated="
                            + event.isItemCountEstimated()
                            + ", maxEstimated="
                            + unknownMaxEstimated.get()
                            + ", history="
                            + String.join(">", unknownCountHistory)
                            + ", initialMaxEstimated="
                            + displayUnknownMetric(
                                    unknownInitialMaxEstimated.get())
                            + ", initialExact="
                            + displayUnknownMetric(unknownInitialExact.get())
                            + ", initialHistory="
                            + unknownInitialHistory[0]
                            + ", mutationResetEstimate="
                            + displayUnknownMetric(
                                    unknownMutationResetEstimate.get())
                            + ", mutationMaxEstimated="
                            + unknownMutationMaxEstimated.get()
                            + ", mutationExact="
                            + displayUnknownMetric(unknownMutationExact.get())
                            + ", mutationHistory="
                            + (unknownMutationHistory.isEmpty()
                                    ? "none"
                                    : String.join(">",
                                            unknownMutationHistory))
                            + ", shrinkCountEvents="
                            + unknownShrinkCountEvents.get()
                            + ", shrinkResetEstimate="
                            + displayUnknownMetric(
                                    unknownShrinkResetEstimate.get())
                            + ", shrinkMaxEstimated="
                            + unknownShrinkMaxEstimated.get()
                            + ", shrinkExact="
                            + displayUnknownMetric(unknownShrinkExact.get())
                            + ", shrinkHistory="
                            + (unknownShrinkHistory.isEmpty()
                                    ? "none"
                                    : String.join(">", unknownShrinkHistory)));
        });

        @SuppressWarnings("unchecked")
        GridMultiSelectionModel<WorkItem> unknownSelectionModel =
                (GridMultiSelectionModel<WorkItem>)
                        unknownSizeGrid.getSelectionModel();
        AtomicInteger unknownSelectionChanges = new AtomicInteger();
        unknownSelectionModel.addMultiSelectionListener(event ->
                unknownSelectionStatus.setText(
                        "Unknown-size Grid selection: "
                                + selectedSummary(
                                        event.getAllSelectedItems())
                                + "; change="
                                + unknownSelectionChanges.incrementAndGet()
                                + ", added="
                                + selectedSummary(event.getAddedSelection())
                                + ", removed="
                                + selectedSummary(event.getRemovedSelection())
                                + ", client=" + event.isFromClient()));

        AtomicInteger unknownMutationChanges = new AtomicInteger();
        Button unknownMutationRefreshAll = new Button(
                "Append U-068..U-093 and Refresh All", event -> {
                    int before = unknownSizeItems.size();
                    IntStream.rangeClosed(68, 93)
                            .mapToObj(
                                    FlowLazyGridSelectionRoute::unknownWorkItem)
                            .forEach(unknownSizeItems::add);
                    int after = unknownSizeItems.size();

                    unknownMutationMaxEnd.set(0);
                    unknownMutationCrossedPriorExact.set(false);
                    unknownMutationTerminalShort.set(false);
                    unknownMutationTerminalReturned.set(-1);
                    unknownMutationTerminalLimit.set(-1);
                    unknownMutationTerminalEnd.set(-1);
                    unknownMutationTerminalLast[0] = "none";
                    unknownMutationResetEstimate.set(-1);
                    unknownMutationMaxEstimated.set(0);
                    unknownMutationExact.set(-1);
                    unknownMutationHistory.clear();
                    unknownMutationDataChangeEvents.set(0);
                    unknownMutationDataRefreshEvents.set(0);
                    unknownMutationActive.set(true);
                    unknownMutationEventArmed.set(true);
                    try {
                        unknownSizeDataView.refreshAll();
                    } finally {
                        unknownMutationEventArmed.set(false);
                    }

                    unknownMutationStatus.setText(
                            "Unknown-size Grid mutation: operation=refreshAll;"
                                    + " added=U-068..U-093, before=" + before
                                    + ", after=" + after
                                    + ", selected="
                                    + selectedSummary(
                                            unknownSizeGrid
                                                    .getSelectedItems())
                                    + ", selectionChanges="
                                    + unknownSelectionChanges.get()
                                    + ", mutationChanges="
                                    + unknownMutationChanges.incrementAndGet()
                                    + ", dataChangeEvents="
                                    + unknownMutationDataChangeEvents.get()
                                    + ", dataRefreshEvents="
                                    + unknownMutationDataRefreshEvents.get()
                                    + ", client=" + event.isFromClient());
                    event.getSource().setEnabled(false);
                });
        unknownMutationRefreshAll.setId(
                "framework-flow-unknown-size-grid-mutation-refresh-all");

        AtomicInteger unknownShrinkChanges = new AtomicInteger();
        unknownShrinkRefreshAll.addClickListener(event -> {
            int before = unknownSizeItems.size();
            if (before != 93 || unknownMutationExact.get() != 93
                    || unknownShrinkActive.get()
                    || !"U-067".equals(selectedSummary(
                            unknownSizeGrid.getSelectedItems()))) {
                throw new IllegalStateException(
                        "Shrink requires the selected, settled append epoch");
            }
            WorkItem retained = unknownSizeItems.get(66);
            List<WorkItem> removed = List.copyOf(
                    unknownSizeItems.subList(72, before));
            boolean removedSelected = removed.stream()
                    .anyMatch(unknownSelectionModel::isSelected);
            if (removedSelected) {
                throw new IllegalStateException(
                        "Shrink must not remove a selected item");
            }
            int fetchBaseline = unknownFetchCount.get();
            int countBaseline = unknownCountEvents.get();
            unknownSizeItems.subList(72, before).clear();
            int after = unknownSizeItems.size();

            // Preserve the initial and append snapshots; this is a new fetch
            // and item-count epoch driven solely by the public refresh API.
            unknownShrinkActive.set(true);
            unknownShrinkEventArmed.set(true);
            try {
                unknownSizeDataView.refreshAll();
            } finally {
                unknownShrinkEventArmed.set(false);
            }
            unknownShrinkStatus.setText(
                    "Unknown-size Grid shrink: operation=refreshAll;"
                            + " removed=U-073..U-093, before=" + before
                            + ", after=" + after
                            + ", removedCount=" + removed.size()
                            + ", removedSelected=" + removedSelected
                            + ", retainedSameInstance="
                            + (retained == unknownSizeItems.get(66))
                            + ", selected="
                            + selectedSummary(
                                    unknownSizeGrid.getSelectedItems())
                            + ", selectionChanges="
                            + unknownSelectionChanges.get()
                            + ", shrinkChanges="
                            + unknownShrinkChanges.incrementAndGet()
                            + ", dataChangeEvents="
                            + unknownShrinkDataChangeEvents.get()
                            + ", dataRefreshEvents="
                            + unknownShrinkDataRefreshEvents.get()
                            + ", fetchBaseline=" + fetchBaseline
                            + ", countBaseline=" + countBaseline
                            + ", client=" + event.isFromClient());
            event.getSource().setEnabled(false);
        });

        H2 unknownSizeHeading = new H2(
                "Unknown-size Grid multi-selection acceptance");
        unknownSizeHeading.setId(
                "framework-flow-unknown-size-grid-heading");

        Button returnToFramework = new Button("Return to framework", event ->
                event.getSource().getUI().ifPresent(ui ->
                        ui.navigate(FlowOverIpcFrameworkView.class)));
        returnToFramework.setId(
                "framework-flow-lazy-grid-selection-return");

        add(
                new H1("Flow lazy Grid multi-selection acceptance"),
                ownerFilter,
                grid,
                refreshItem,
                refreshStatus,
                refreshAll,
                refreshAllStatus,
                fetchStatus,
                countStatus,
                selectionStatus,
                removeSelected,
                removalStatus,
                selectionHistoryStatus,
                unknownSizeHeading,
                unknownConfigStatus,
                unknownSizeGrid,
                unknownFetchStatus,
                unknownCountStatus,
                unknownSelectionStatus,
                unknownMutationRefreshAll,
                unknownMutationStatus,
                unknownShrinkRefreshAll,
                unknownShrinkStatus,
                returnToFramework);
    }

    private static List<WorkItem> queryItems(
            List<WorkItem> items,
            String filter,
            List<QuerySortOrder> sortOrders) {
        Comparator<WorkItem> comparator = null;
        for (QuerySortOrder order : sortOrders) {
            Comparator<WorkItem> propertyComparator = switch (
                    order.getSorted()) {
                case "owner" -> Comparator.comparing(WorkItem::owner);
                case "state" -> Comparator.comparing(WorkItem::state);
                default -> Comparator.comparing(WorkItem::id);
            };
            if (order.getDirection() == SortDirection.DESCENDING) {
                propertyComparator = propertyComparator.reversed();
            }
            comparator = comparator == null
                    ? propertyComparator
                    : comparator.thenComparing(propertyComparator);
        }
        if (comparator != null) {
            comparator = comparator.thenComparing(WorkItem::id);
        }

        List<WorkItem> matchingItems = items.stream()
                .filter(item -> filter.isEmpty()
                        || item.owner().toLowerCase(Locale.ROOT)
                                .contains(filter))
                .collect(Collectors.toCollection(ArrayList::new));
        if (comparator != null) {
            matchingItems.sort(comparator);
        }
        return matchingItems;
    }

    private static String normalizeFilter(String filter) {
        return filter == null
                ? ""
                : filter.trim().toLowerCase(Locale.ROOT);
    }

    private static int ownerCount(List<WorkItem> items, String owner) {
        return (int) items.stream()
                .filter(item -> owner.equals(item.owner()))
                .count();
    }

    private static String displayFilter(String filter) {
        return filter.isEmpty() ? "all" : filter;
    }

    private static String sortSummary(List<QuerySortOrder> sortOrders) {
        if (sortOrders.isEmpty()) {
            return "none";
        }
        return sortOrders.stream()
                .map(order -> order.getSorted() + ":"
                        + order.getDirection().name()
                                .toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(","));
    }

    private static String displayUnknownMetric(int metric) {
        return metric < 0 ? "none" : Integer.toString(metric);
    }

    private static WorkItem unknownWorkItem(int index) {
        return new WorkItem(
                "U-%03d".formatted(index),
                switch (index % 3) {
                    case 0 -> "platform";
                    case 1 -> "desktop";
                    default -> "audit";
                },
                switch (index % 4) {
                    case 0 -> "ready";
                    case 1 -> "queued";
                    case 2 -> "active";
                    default -> "review";
                });
    }

    private static String selectedSummary(Set<WorkItem> selectedItems) {
        if (selectedItems.isEmpty()) {
            return "none";
        }
        return selectedItems.stream()
                .map(WorkItem::id)
                .sorted()
                .collect(Collectors.joining(","));
    }

    private record WorkItem(String id, String owner, String state) {
    }
}
