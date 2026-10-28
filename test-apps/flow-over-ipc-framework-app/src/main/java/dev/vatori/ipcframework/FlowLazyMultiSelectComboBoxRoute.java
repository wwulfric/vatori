package dev.vatori.ipcframework;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.router.Route;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Route("flow-lazy-multi-select-combo-box")
public final class FlowLazyMultiSelectComboBoxRoute
        extends VerticalLayout {
    public FlowLazyMultiSelectComboBoxRoute() {
        setId("framework-flow-lazy-multi-select-combo-box-route");
        setPadding(true);
        setSpacing(true);

        List<LazyReviewer> catalog = IntStream.rangeClosed(1, 80)
                .mapToObj(index -> new LazyReviewer(
                        "R-%03d".formatted(index),
                        switch (index) {
                            case 60 -> "Xeno";
                            case 61 -> "Zed";
                            default -> "Reviewer %03d".formatted(index);
                        }))
                .toList();
        Map<String, LazyReviewer> catalogById = catalog.stream()
                .collect(Collectors.toUnmodifiableMap(
                        LazyReviewer::id, Function.identity()));
        LazyReviewer initialSelected =
                new LazyReviewer("R-060", "Xeno");
        AtomicReference<Set<LazyReviewer>> selectedSnapshot =
                new AtomicReference<>(Set.of(initialSelected));

        Span initialStatus = new Span(
                "Lazy multi-select initial: pending selected=R-060");
        initialStatus.setId(
                "framework-flow-lazy-multi-select-combo-box-initial-status");
        Span farStatus = new Span(
                "Lazy multi-select far page: pending selected=R-060");
        farStatus.setId(
                "framework-flow-lazy-multi-select-combo-box-far-status");
        Span zQueryStatus = new Span(
                "Lazy multi-select z query: pending selected=R-060");
        zQueryStatus.setId(
                "framework-flow-lazy-multi-select-combo-box-z-query-status");
        Span repeatZQueryStatus = new Span(
                "Lazy multi-select repeated z query: pending"
                        + " fetch-count=0 selected=R-060");
        repeatZQueryStatus.setId(
                "framework-flow-lazy-multi-select-combo-box-repeat-z-query-status");
        Span xQueryStatus = new Span(
                "Lazy multi-select x query: pending selected=R-060");
        xQueryStatus.setId(
                "framework-flow-lazy-multi-select-combo-box-x-query-status");
        Span selectionStatus = new Span(
                "Lazy multi-select selection: selected=R-060 change=0"
                        + " added=none removed=none client=false");
        selectionStatus.setId(
                "framework-flow-lazy-multi-select-combo-box-selection-status");
        AtomicInteger selectionChanges = new AtomicInteger();
        QueryEvidence evidence = new QueryEvidence(
                initialStatus, farStatus, zQueryStatus,
                repeatZQueryStatus, xQueryStatus);

        CallbackDataProvider<LazyReviewer, String> provider =
                new CallbackDataProvider<>(query -> {
                    String filter = normalizeFilter(
                            query.getFilter().orElse(""));
                    List<LazyReviewer> matching = matching(catalog, filter);
                    List<LazyReviewer> fetched = matching.stream()
                            .skip(query.getOffset())
                            .limit(query.getLimit())
                            .map(template -> new LazyReviewer(
                                    template.id(), template.label()))
                            .toList();
                    evidence.observeFetch(
                            filter,
                            query.getOffset(),
                            query.getLimit(),
                            fetched,
                            selectedSummary(selectedSnapshot.get()),
                            initialSelected,
                            catalogById);
                    return fetched.stream();
                }, query -> {
                    String filter = normalizeFilter(
                            query.getFilter().orElse(""));
                    int count = matching(catalog, filter).size();
                    evidence.observeCount(
                            filter,
                            count,
                            selectedSummary(selectedSnapshot.get()));
                    return count;
                }, LazyReviewer::id);

        MultiSelectComboBox<LazyReviewer> reviewers =
                new MultiSelectComboBox<>("Lazy reviewers");
        reviewers.setId(
                "framework-flow-lazy-multi-select-combo-box-field");
        reviewers.setAriaLabel("Flow IPC lazy multi-select reviewers");
        reviewers.setItemLabelGenerator(
                item -> item.id() + " " + item.label());
        reviewers.setPageSize(10);
        reviewers.setItems(provider);
        reviewers.setValue(Set.of(initialSelected));
        reviewers.setKeepFilter(true);
        reviewers.setAutoExpand(
                MultiSelectComboBox.AutoExpandMode.VERTICAL);
        reviewers.setWidth("520px");
        reviewers.addSelectionListener(event -> {
            selectedSnapshot.set(Set.copyOf(event.getAllSelectedItems()));
            selectionStatus.setText(
                    "Lazy multi-select selection: selected="
                            + selectedSummary(event.getAllSelectedItems())
                            + " change=" + selectionChanges.incrementAndGet()
                            + " added="
                            + selectedSummary(event.getAddedSelection())
                            + " removed="
                            + selectedSummary(event.getRemovedSelection())
                            + " client=" + event.isFromClient());
        });

        Button returnToFramework = new Button("Return to framework", event ->
                event.getSource().getUI().ifPresent(ui ->
                        ui.navigate(FlowOverIpcFrameworkView.class)));
        returnToFramework.setId(
                "framework-flow-lazy-multi-select-combo-box-return");

        add(
                new H1("Flow lazy MultiSelectComboBox acceptance"),
                reviewers,
                initialStatus,
                farStatus,
                zQueryStatus,
                repeatZQueryStatus,
                xQueryStatus,
                selectionStatus,
                returnToFramework);
    }

    private static List<LazyReviewer> matching(
            List<LazyReviewer> catalog, String filter) {
        return catalog.stream()
                .filter(item -> filter.isEmpty()
                        || item.label().toLowerCase(Locale.ROOT)
                                .contains(filter))
                .toList();
    }

    private static String normalizeFilter(String filter) {
        return filter == null
                ? ""
                : filter.trim().toLowerCase(Locale.ROOT);
    }

    private static String selectedSummary(Set<LazyReviewer> selected) {
        if (selected.isEmpty()) {
            return "none";
        }
        return selected.stream()
                .map(LazyReviewer::id)
                .sorted()
                .collect(Collectors.joining(","));
    }

    private static final class QueryEvidence {
        private final Span initialStatus;
        private final Span farStatus;
        private final Span zQueryStatus;
        private final Span repeatZQueryStatus;
        private final Span xQueryStatus;
        private boolean initialCountObserved;
        private boolean initialFetchObserved;
        private boolean farFetchObserved;
        private boolean zCountObserved;
        private boolean zFetchObserved;
        private int zFetchCount;
        private boolean xCountObserved;
        private boolean xFetchObserved;

        private QueryEvidence(
                Span initialStatus,
                Span farStatus,
                Span zQueryStatus,
                Span repeatZQueryStatus,
                Span xQueryStatus) {
            this.initialStatus = initialStatus;
            this.farStatus = farStatus;
            this.zQueryStatus = zQueryStatus;
            this.repeatZQueryStatus = repeatZQueryStatus;
            this.xQueryStatus = xQueryStatus;
        }

        private void observeCount(
                String filter, int count, String selected) {
            if (filter.isEmpty() && count == 80
                    && "R-060".equals(selected)) {
                initialCountObserved = true;
            } else if ("z".equals(filter) && count == 1
                    && "R-060".equals(selected)) {
                zCountObserved = true;
            } else if ("x".equals(filter) && count == 1
                    && "R-060,R-061".equals(selected)) {
                xCountObserved = true;
            }
            refresh();
        }

        private void observeFetch(
                String filter,
                int offset,
                int limit,
                List<LazyReviewer> fetched,
                String selected,
                LazyReviewer initialSelected,
                Map<String, LazyReviewer> catalogById) {
            boolean freshFromCatalog = fetched.stream().allMatch(item ->
                    item != catalogById.get(item.id()));
            if ("z".equals(filter)) {
                zFetchCount++;
            }
            if (filter.isEmpty() && offset == 0 && limit > 0
                    && !fetched.isEmpty() && freshFromCatalog
                    && "R-060".equals(selected)) {
                initialFetchObserved = true;
            }
            LazyReviewer fetchedR060 = fetched.stream()
                    .filter(item -> "R-060".equals(item.id()))
                    .findFirst().orElse(null);
            if (filter.isEmpty() && offset > 0 && limit > 0
                    && fetchedR060 != null
                    && fetchedR060 != initialSelected
                    && fetchedR060 != catalogById.get("R-060")
                    && "R-060".equals(selected)) {
                farFetchObserved = true;
            }
            if ("z".equals(filter) && offset == 0 && limit > 0
                    && fetched.size() == 1
                    && "R-061".equals(fetched.getFirst().id())
                    && fetched.getFirst() != catalogById.get("R-061")
                    && "R-060".equals(selected)) {
                zFetchObserved = true;
            }
            if ("z".equals(filter) && offset == 0 && limit > 0
                    && fetched.size() == 1
                    && "R-061".equals(fetched.getFirst().id())
                    && fetched.getFirst() != catalogById.get("R-061")) {
                repeatZQueryStatus.setText(
                        "Lazy multi-select repeated z query: fetch-count="
                                + zFetchCount
                                + " fetch=R-061 offset=0"
                                + " limit-positive=true fresh=true selected="
                                + selected);
            }
            if ("x".equals(filter) && offset == 0 && limit > 0
                    && fetched.size() == 1
                    && "R-060".equals(fetched.getFirst().id())
                    && fetched.getFirst() != initialSelected
                    && fetched.getFirst() != catalogById.get("R-060")
                    && "R-060,R-061".equals(selected)) {
                xFetchObserved = true;
            }
            refresh();
        }

        private void refresh() {
            if (initialCountObserved && initialFetchObserved) {
                initialStatus.setText(
                        "Lazy multi-select initial: filter=all count=80"
                                + " fetch=true offset=0 limit-positive=true"
                                + " fresh=true selected=R-060");
            }
            if (farFetchObserved) {
                farStatus.setText(
                        "Lazy multi-select far page: filter=all"
                                + " offset-positive=true limit-positive=true"
                                + " contains=R-060 fresh=true"
                                + " selected=R-060");
            }
            if (zCountObserved && zFetchObserved) {
                zQueryStatus.setText(
                        "Lazy multi-select z query: filter=z count=1"
                                + " fetch=R-061 offset=0"
                                + " limit-positive=true fresh=true"
                                + " selected=R-060");
            }
            if (xCountObserved && xFetchObserved) {
                xQueryStatus.setText(
                        "Lazy multi-select x query: filter=x count=1"
                                + " fetch=R-060 offset=0"
                                + " limit-positive=true fresh=true"
                                + " selected=R-060,R-061");
            }
        }
    }

    private static final class LazyReviewer {
        private final String id;
        private final String label;

        private LazyReviewer(String id, String label) {
            this.id = id;
            this.label = label;
        }

        private String id() {
            return id;
        }

        private String label() {
            return label;
        }
    }
}
