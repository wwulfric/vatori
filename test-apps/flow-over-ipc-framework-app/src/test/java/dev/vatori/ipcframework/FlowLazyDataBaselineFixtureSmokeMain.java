package dev.vatori.ipcframework;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.SortDirection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/** Real public Flow objects, direct provider queries/server actions. No browser, IPC or client input. */
public final class FlowLazyDataBaselineFixtureSmokeMain {
    private static int pages;

    public static void main(String[] args) {
        var route = new FlowLazyDataBaselineRoute();
        verify(route, "exact");
        verify(route, "unknown");
        System.out.println("PASS lazy-data fixture: two 1000-item providers, page size 50, ten cycles each, "
                + pages + " queried pages, selected removal and provider isolation (server-only; no WKWebView/IPC claim)");
    }

    @SuppressWarnings("unchecked")
    private static void verify(FlowLazyDataBaselineRoute route, String mode) {
        String prefix = "lazy-baseline-" + mode + "-";
        Grid<FlowLazyDataBaselineRoute.Row> grid = (Grid<FlowLazyDataBaselineRoute.Row>) find(route, prefix + "grid");
        var provider = (DataProvider<FlowLazyDataBaselineRoute.Row, Void>) grid.getDataProvider();
        var status = (Span) find(route, prefix + "status");
        var advance = (Button) find(route, prefix + "advance");
        var remove = (Button) find(route, prefix + "remove");
        var other = (Span) find(route, "lazy-baseline-" + (mode.equals("exact") ? "unknown" : "exact") + "-status");
        String otherBefore = other.getText();
        check(grid.getPageSize() == 50, "configured page size");
        if (mode.equals("unknown")) {
            check(grid.getLazyDataView().getItemCountEstimate() == 100, "configured unknown-size estimate");
            check(grid.getLazyDataView().getItemCountEstimateIncrease() == 100, "configured estimate increase");
        }
        String beforeEmptyRemove = status.getText();
        remove.click();
        check(status.getText().equals(beforeEmptyRemove), "empty-selection removal is a no-op");
        for (int cycle = 0; cycle <= 10; cycle++) {
            final int epoch = cycle;
            var expected = new ArrayList<>(IntStream.rangeClosed(1, 1000)
                    .filter(id -> epoch == 0 || id % 11 != epoch).boxed().toList());
            SortDirection direction = cycle % 2 == 0 ? SortDirection.ASCENDING : SortDirection.DESCENDING;
            if (direction == SortDirection.DESCENDING) Collections.reverse(expected);
            if (cycle > 0) {
                advance.click();
                check(grid.getSortOrder().size() == 1 && grid.getSortOrder().getFirst().getDirection() == direction,
                        "public sort action agrees with epoch " + cycle);
            }
            List<Integer> actual = allPages(provider, direction);
            check(actual.equals(expected), mode + " exact ordered filtered dataset for cycle " + cycle);
            check(actual.stream().distinct().count() == actual.size(), "no duplicate IDs");
            check(status.getText().contains("cycle=" + cycle + ";total=1000;matches=" + expected.size() + ";"),
                    "actual callback status describes dataset");
            check(status.getText().endsWith("client=false"), "server action must not claim client provenance");
            if (mode.equals("exact")) check(provider.size(new Query<>()) == expected.size(), "exact count callback");
            else check(status.getText().contains(";counts=0;"), "fetch-only provider never uses exact count callback");
            check(advance.isEnabled() == (cycle < 10), "bounded action enablement");
        }
        var selected = provider.fetch(query(0, SortDirection.ASCENDING)).findFirst().orElseThrow();
        grid.select(selected);
        check(status.getText().contains(";selected=[" + selected.id() + "]"), "Java selection receipt");
        remove.click();
        check(grid.getSelectedItems().isEmpty(), "explicit public deselect before backing-list removal");
        var remaining = allPages(provider, SortDirection.ASCENDING);
        var expectedRemaining = IntStream.rangeClosed(1, 1000)
                .filter(id -> id % 11 != 10 && id != selected.id()).boxed().toList();
        check(remaining.equals(expectedRemaining), "exact survivor dataset, no lost neighbors or removed ID");
        check(status.getText().contains(";total=999;"), "one item actually removed");
        check(status.getText().contains(";removals=1;selected=[];client=false"), "mutation and provenance receipt");
        check(status.getElement().getAttribute("data-removed-id").equals(Integer.toString(selected.id())), "removed ID receipt");
        String settled = status.getText();
        remove.click();
        check(status.getText().equals(settled), "duplicate remove without selection does not mutate");
        var neighbor = provider.fetch(query(0, SortDirection.ASCENDING)).findFirst().orElseThrow();
        grid.select(neighbor);
        check(grid.getSelectedItems().size() == 1 && grid.getSelectedItems().contains(neighbor),
                "surviving neighbor remains selectable after mutation");
        check(other.getText().equals(otherBefore), "independent provider state is unchanged");
    }

    private static List<Integer> allPages(DataProvider<FlowLazyDataBaselineRoute.Row, Void> provider, SortDirection direction) {
        List<Integer> result = new ArrayList<>();
        for (int offset = 0; offset <= 1000; offset += 50) {
            var page = provider.fetch(query(offset, direction)).toList();
            pages++;
            check(page.size() <= 50, "callback honors requested limit");
            result.addAll(page.stream().map(FlowLazyDataBaselineRoute.Row::id).toList());
            if (page.size() < 50) return result;
        }
        throw new AssertionError("provider did not terminate within bounded dataset");
    }

    private static Query<FlowLazyDataBaselineRoute.Row, Void> query(int offset, SortDirection direction) {
        return new Query<>(offset, 50, List.of(new QuerySortOrder("id", direction)), null, null);
    }

    private static Component find(Component root, String id) {
        return tree(root).filter(component -> component.getId().orElse("").equals(id)).findFirst().orElseThrow();
    }
    private static Stream<Component> tree(Component root) {
        return Stream.concat(Stream.of(root), root.getChildren().flatMap(FlowLazyDataBaselineFixtureSmokeMain::tree));
    }
    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
}
