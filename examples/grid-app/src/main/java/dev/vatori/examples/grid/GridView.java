package dev.vatori.examples.grid;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.router.Route;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Route("")
public class GridView extends VerticalLayout {
    private static final List<OrderRow> ORDERS = IntStream.rangeClosed(1, 200)
            .mapToObj(id -> new OrderRow(id, "Customer " + id, id % 2 == 0 ? "READY" : "PENDING"))
            .toList();

    public GridView() {
        setSizeFull();
        Grid<OrderRow> grid = new Grid<>(OrderRow.class, false);
        grid.addColumn(OrderRow::id).setHeader("ID").setWidth("90px");
        grid.addColumn(OrderRow::customer).setHeader("Customer");
        grid.addColumn(OrderRow::status).setHeader("Status");
        grid.setItems(new CallbackDataProvider<>(
                query -> ORDERS.stream().skip(query.getOffset()).limit(query.getLimit()),
                query -> ORDERS.size()));
        grid.setId("orders-grid");
        grid.setSizeFull();
        add(new H1("Vaadin Desktop Grid"), grid);
        expand(grid);
    }

    public record OrderRow(int id, String customer, String status) {
    }
}

