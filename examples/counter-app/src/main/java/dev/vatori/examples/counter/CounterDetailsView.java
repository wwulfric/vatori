package dev.vatori.examples.counter;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route("details")
public final class CounterDetailsView extends VerticalLayout {
    public CounterDetailsView() {
        CounterApplicationService applicationService = new CounterApplicationService();
        setPadding(true);
        setSpacing(true);
        add(new H2("Counter details"), new Paragraph(applicationService.details()),
                new RouterLink("Back to counter", CounterView.class));
    }
}
