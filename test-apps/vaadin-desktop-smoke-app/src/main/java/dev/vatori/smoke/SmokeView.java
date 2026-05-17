package dev.vatori.smoke;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import dev.vatori.desktop.Desktop;

import java.util.List;

@Route("")
public class SmokeView extends VerticalLayout {
    private int count;

    public SmokeView() {
        setPadding(true);
        setSpacing(true);

        Span status = new Span("Clicks: 0");
        status.setId("smoke-status");

        Button button = new Button("Increment and notify", event -> {
            count++;
            status.setText("Clicks: " + count);
            Desktop.current().notifications().show("Smoke App", "Button clicked " + count + " time(s)");
        });
        button.setId("smoke-button");

        Grid<Row> grid = new Grid<>(Row.class, false);
        grid.setId("smoke-grid");
        grid.addColumn(Row::id).setHeader("ID");
        grid.addColumn(Row::name).setHeader("Name");
        grid.addColumn(Row::state).setHeader("State");
        grid.setItems(List.of(
                new Row(1, "Bootstrap", "READY"),
                new Row(2, "IPC", "SCAFFOLDED"),
                new Row(3, "Desktop API", "READY")));

        add(new H1("Vaadin Desktop Smoke App"), button, status, grid);
    }

    public record Row(int id, String name, String state) {
    }
}

