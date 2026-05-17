package dev.vatori.examples.capabilities;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import dev.vatori.desktop.Desktop;

@Route("")
public class CapabilitiesView extends VerticalLayout {
    public CapabilitiesView() {
        Span status = new Span("Ready");
        status.setId("capability-status");

        Button notify = new Button("Notify", event -> {
            Desktop.current().notifications().show("Vaadin Desktop", "Notification command reached native bridge.");
            status.setText("Notification command logged");
        });

        Button clipboard = new Button("Write Clipboard", event -> {
            Desktop.current().clipboard().writeText("Hello from Vaadin Desktop");
            String text = Desktop.current().clipboard().readText().orElse("");
            status.setText("Clipboard contains: " + text);
        });

        add(new H1("Desktop Capabilities"), notify, clipboard, status);
    }
}

