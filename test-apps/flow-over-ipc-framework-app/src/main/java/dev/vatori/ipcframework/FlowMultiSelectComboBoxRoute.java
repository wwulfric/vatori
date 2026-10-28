package dev.vatori.ipcframework;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Route("flow-multi-select-combo-box")
public final class FlowMultiSelectComboBoxRoute extends VerticalLayout {
    public FlowMultiSelectComboBoxRoute() {
        setId("framework-flow-multi-select-combo-box-route");
        setPadding(true);
        setSpacing(true);

        Span selectionStatus = new Span(
                "Multi-select selection: selected=ava change=0"
                        + " added=none removed=none client=false");
        selectionStatus.setId(
                "framework-flow-multi-select-combo-box-selection-status");
        AtomicInteger selectionChanges = new AtomicInteger();

        MultiSelectComboBox<String> reviewers =
                new MultiSelectComboBox<>("Reviewers");
        reviewers.setId("framework-flow-multi-select-combo-box-field");
        reviewers.setAriaLabel("Flow IPC multi-select reviewers");
        reviewers.setItems("ava", "sol", "kai");
        reviewers.setValue(Set.of("ava"));
        reviewers.setAutoExpand(
                MultiSelectComboBox.AutoExpandMode.VERTICAL);
        reviewers.setWidth("420px");
        reviewers.addSelectionListener(event ->
                selectionStatus.setText(
                        "Multi-select selection: selected="
                                + summary(event.getAllSelectedItems())
                                + " change="
                                + selectionChanges.incrementAndGet()
                                + " added="
                                + summary(event.getAddedSelection())
                                + " removed="
                                + summary(event.getRemovedSelection())
                                + " client=" + event.isFromClient()));

        Span clearSelectionStatus = new Span(
                "Multi-select clear: selected=kai,sol change=0"
                        + " added=none removed=none client=false");
        clearSelectionStatus.setId(
                "framework-flow-multi-select-combo-box-clear-status");
        AtomicInteger clearSelectionChanges = new AtomicInteger();

        MultiSelectComboBox<String> clearReviewers =
                new MultiSelectComboBox<>("Clear reviewers");
        clearReviewers.setId(
                "framework-flow-multi-select-combo-box-clear-field");
        clearReviewers.setAriaLabel(
                "Flow IPC multi-select clear reviewers");
        clearReviewers.setItems("ava", "sol", "kai");
        clearReviewers.setValue(Set.of("kai", "sol"));
        clearReviewers.setClearButtonVisible(true);
        clearReviewers.setAutoExpand(
                MultiSelectComboBox.AutoExpandMode.VERTICAL);
        clearReviewers.setWidth("420px");
        clearReviewers.addSelectionListener(event ->
                clearSelectionStatus.setText(
                        "Multi-select clear: selected="
                                + summary(event.getAllSelectedItems())
                                + " change="
                                + clearSelectionChanges.incrementAndGet()
                                + " added="
                                + summary(event.getAddedSelection())
                                + " removed="
                                + summary(event.getRemovedSelection())
                                + " client=" + event.isFromClient()));

        Span filteringQueryStatus = new Span(
                "Multi-select filtering query: filter=none server=false"
                        + " matches=ava,kai,sol selected=ava");
        filteringQueryStatus.setId(
                "framework-flow-multi-select-combo-box-filter-query-status");
        Span filteringSelectionStatus = new Span(
                "Multi-select filtering selection: selected=ava change=0"
                        + " added=none removed=none client=false");
        filteringSelectionStatus.setId(
                "framework-flow-multi-select-combo-box-filter-status");
        AtomicBoolean filteringServerQueryObserved = new AtomicBoolean();
        AtomicInteger filteringSelectionChanges = new AtomicInteger();

        MultiSelectComboBox<String> filteringReviewers =
                new MultiSelectComboBox<>("Filter reviewers");
        filteringReviewers.setId(
                "framework-flow-multi-select-combo-box-filter-field");
        filteringReviewers.setAriaLabel(
                "Flow IPC multi-select filter reviewers");
        filteringReviewers.setItems((item, filter) -> {
            String normalizedFilter = filter == null
                    ? ""
                    : filter.toLowerCase(Locale.ROOT);
            boolean matches = item.toLowerCase(Locale.ROOT)
                    .contains(normalizedFilter);
            if (matches && "k".equals(normalizedFilter)
                    && "kai".equals(item)
                    && filteringServerQueryObserved.compareAndSet(
                            false, true)) {
                filteringQueryStatus.setText(
                        "Multi-select filtering query: filter=k"
                                + " server=true matches=kai selected=ava");
            }
            return matches;
        }, List.of("ava", "sol", "kai"));
        filteringReviewers.setValue(Set.of("ava"));
        filteringReviewers.setKeepFilter(true);
        filteringReviewers.setAutoExpand(
                MultiSelectComboBox.AutoExpandMode.VERTICAL);
        filteringReviewers.setWidth("420px");
        filteringReviewers.addSelectionListener(event ->
                filteringSelectionStatus.setText(
                        "Multi-select filtering selection: selected="
                                + summary(event.getAllSelectedItems())
                                + " change="
                                + filteringSelectionChanges.incrementAndGet()
                                + " added="
                                + summary(event.getAddedSelection())
                                + " removed="
                                + summary(event.getRemovedSelection())
                                + " client=" + event.isFromClient()));

        Span defaultClearQueryStatus = new Span(
                "Multi-select default-clear query: filter=none server=false"
                        + " matches=ava,kai,sol selected=ava");
        defaultClearQueryStatus.setId(
                "framework-flow-multi-select-combo-box-default-clear-query-status");
        Span defaultClearSelectionStatus = new Span(
                "Multi-select default-clear selection: selected=ava change=0"
                        + " added=none removed=none client=false");
        defaultClearSelectionStatus.setId(
                "framework-flow-multi-select-combo-box-default-clear-status");
        AtomicBoolean defaultClearServerQueryObserved = new AtomicBoolean();
        AtomicInteger defaultClearSelectionChanges = new AtomicInteger();

        MultiSelectComboBox<String> defaultClearReviewers =
                new MultiSelectComboBox<>("Default-clear reviewers");
        defaultClearReviewers.setId(
                "framework-flow-multi-select-combo-box-default-clear-field");
        defaultClearReviewers.setAriaLabel(
                "Flow IPC multi-select default-clear reviewers");
        defaultClearReviewers.setItems((item, filter) -> {
            String normalizedFilter = filter == null
                    ? ""
                    : filter.toLowerCase(Locale.ROOT);
            boolean matches = item.toLowerCase(Locale.ROOT)
                    .contains(normalizedFilter);
            if (matches && "k".equals(normalizedFilter)
                    && "kai".equals(item)
                    && defaultClearServerQueryObserved.compareAndSet(
                            false, true)) {
                defaultClearQueryStatus.setText(
                        "Multi-select default-clear query: filter=k"
                                + " server=true matches=kai selected=ava");
            }
            return matches;
        }, List.of("ava", "sol", "kai"));
        defaultClearReviewers.setValue(Set.of("ava"));
        defaultClearReviewers.setAutoExpand(
                MultiSelectComboBox.AutoExpandMode.VERTICAL);
        defaultClearReviewers.setWidth("420px");
        defaultClearReviewers.addSelectionListener(event ->
                defaultClearSelectionStatus.setText(
                        "Multi-select default-clear selection: selected="
                                + summary(event.getAllSelectedItems())
                                + " change="
                                + defaultClearSelectionChanges.incrementAndGet()
                                + " added="
                                + summary(event.getAddedSelection())
                                + " removed="
                                + summary(event.getRemovedSelection())
                                + " client=" + event.isFromClient()));

        Button returnToFramework = new Button("Return to framework", event ->
                event.getSource().getUI().ifPresent(ui ->
                        ui.navigate(FlowOverIpcFrameworkView.class)));
        returnToFramework.setId(
                "framework-flow-multi-select-combo-box-return");

        add(
                new H1("Flow MultiSelectComboBox token acceptance"),
                reviewers,
                selectionStatus,
                new H2("Clear-button collection acceptance"),
                clearReviewers,
                clearSelectionStatus,
                new H2("Server filtering acceptance"),
                filteringReviewers,
                filteringQueryStatus,
                filteringSelectionStatus,
                new H2("Default post-selection filter clearing acceptance"),
                defaultClearReviewers,
                defaultClearQueryStatus,
                defaultClearSelectionStatus,
                returnToFramework);
    }

    private static String summary(Set<String> values) {
        if (values.isEmpty()) {
            return "none";
        }
        return values.stream().sorted().collect(Collectors.joining(","));
    }
}
