package dev.vatori.ipcframework;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridLazyDataView;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/** Bounded data-volume fixture. Only public Flow APIs; no automatic acceptance. */
@Route("flow-lazy-data-baseline")
public final class FlowLazyDataBaselineRoute extends VerticalLayout {
    public FlowLazyDataBaselineRoute() {
        setId("lazy-data-baseline");
        add(new Sample("exact", false), new Sample("unknown", true));
    }

    record Row(int id, String value) {}

    private static final class Sample extends VerticalLayout {
        private final List<Row> rows = new ArrayList<>(IntStream.rangeClosed(1, 1000)
                .mapToObj(id -> new Row(id, "Item " + id)).toList());
        private final Grid<Row> grid = new Grid<>();
        private final Span status = new Span();
        private final Span queryStatus = new Span();
        private int cycle;
        private int fetches;
        private int counts;
        private int selections;
        private int removals;
        private int fetchBaseline;
        private int countBaseline;
        private boolean lastActionFromClient;
        private final GridLazyDataView<Row> view;

        Sample(String mode, boolean unknown) {
            String prefix = "lazy-baseline-" + mode + "-";
            setId(prefix + "sample");
            grid.setId(prefix + "grid");
            var idColumn = grid.addColumn(Row::id).setKey("id")
                    .setHeader("ID").setSortProperty("id").setSortable(true);
            grid.addColumn(Row::value).setHeader("Value");
            grid.setPageSize(50);
            grid.setHeight("260px");
            status.setId(prefix + "status");
            queryStatus.setId(prefix + "query");
            if (unknown) {
                view = grid.setItems(this::fetch);
                view.setItemCountUnknown();
                view.setItemCountEstimate(100);
                view.setItemCountEstimateIncrease(100);
            } else {
                view = grid.setItems(this::fetch, query -> {
                    counts++;
                    publish();
                    return matching().size();
                });
            }
            view.setIdentifierProvider(Row::id);
            grid.addSelectionListener(event -> {
                selections++;
                status.getElement().setAttribute("data-selection-client", Boolean.toString(event.isFromClient()));
                publish();
            });
            Button advance = new Button("Next filter/sort/refresh cycle");
            advance.setId(prefix + "advance");
            advance.addClickListener(event -> {
                if (cycle >= 10) return;
                fetchBaseline = fetches;
                countBaseline = counts;
                cycle++;
                lastActionFromClient = event.isFromClient();
                grid.sort(List.of(new GridSortOrder<>(idColumn,
                        cycle % 2 == 0 ? SortDirection.ASCENDING : SortDirection.DESCENDING)));
                view.refreshAll();
                grid.scrollToStart();
                advance.setEnabled(cycle < 10);
                publish();
            });
            Button remove = new Button("Deselect and remove selected item");
            remove.setId(prefix + "remove");
            remove.addClickListener(event -> {
                var selected = grid.getSelectedItems().stream().findFirst();
                if (selected.isEmpty()) return;
                Row row = selected.orElseThrow();
                fetchBaseline = fetches;
                countBaseline = counts;
                grid.deselect(row);
                if (rows.removeIf(value -> value.id() == row.id())) removals++;
                status.getElement().setAttribute("data-removed-id", Integer.toString(row.id()));
                lastActionFromClient = event.isFromClient();
                view.refreshAll();
                publish();
            });
            status.getElement().setAttribute("data-mode", mode);
            status.getElement().setAttribute("data-page-size", "50");
            add(status, queryStatus, grid, advance, remove);
            publish();
        }

        private List<Row> matching() {
            // Cycle zero exposes all 1,000 rows; each subsequent cycle excludes
            // a different residue. Stable IDs survive sorting and refreshes.
            return rows.stream().filter(row -> cycle == 0 || row.id() % 11 != cycle).toList();
        }

        private Stream<Row> fetch(Query<Row, Void> query) {
            fetches++;
            Comparator<Row> order = Comparator.comparingInt(Row::id);
            boolean descending = !query.getSortOrders().isEmpty()
                    && query.getSortOrders().getFirst().getDirection() == SortDirection.DESCENDING;
            var page = matching().stream().sorted(descending ? order.reversed() : order)
                    .skip(query.getOffset()).limit(query.getLimit()).toList();
            queryStatus.setText("cycle=" + cycle + ";offset=" + query.getOffset()
                    + ";limit=" + query.getLimit() + ";returned=" + page.size()
                    + ";direction=" + (descending ? "desc" : "asc")
                    + ";ids=" + page.stream().map(row -> Integer.toString(row.id())).toList());
            publish();
            return page.stream();
        }

        private void publish() {
            status.setText("cycle=" + cycle + ";total=" + rows.size() + ";matches=" + matching().size()
                    + ";fetches=" + fetches + ";counts=" + counts
                    + ";fetchBaseline=" + fetchBaseline + ";countBaseline=" + countBaseline
                    + ";selections=" + selections + ";removals=" + removals
                    + ";selected=" + grid.getSelectedItems().stream().map(Row::id).sorted().toList()
                    + ";client=" + lastActionFromClient);
        }
    }
}
