package dev.vatori.examples.counter;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import dev.vatori.desktop.Desktop;

@Route("")
public class CounterView extends VerticalLayout {
    private int count;

    public CounterView() {
        CounterApplicationService applicationService = new CounterApplicationService();
        setPadding(true);
        setSpacing(true);

        Span value = new Span("0");
        value.setId("counter-value");

        Button increment = new Button("Increment", event -> {
            count++;
            value.setText(String.valueOf(count));
            Desktop.current().setWindowTitle("Counter: " + count);
        });
        increment.setId("counter-button");

        add(new H1(applicationService.title()), value, increment,
                new RouterLink("Details", CounterDetailsView.class));
    }
}
