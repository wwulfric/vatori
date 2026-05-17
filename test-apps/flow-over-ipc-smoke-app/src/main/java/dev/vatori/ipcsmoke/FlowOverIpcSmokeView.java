package dev.vatori.ipcsmoke;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import dev.vatori.desktop.Desktop;

@Route("")
public class FlowOverIpcSmokeView extends VerticalLayout {
    private int count;

    public FlowOverIpcSmokeView() {
        setId("ipc-root");
        setPadding(true);
        setSpacing(true);

        H1 title = new H1("Vaadin Flow over IPC Smoke App");
        title.setId("ipc-title");

        Span status = new Span("Clicks: 0");
        status.setId("ipc-status");

        Button button = new Button("Increment over IPC", event -> {
            count++;
            status.setText("Clicks: " + count);
            Desktop.current().setWindowTitle("Vaadin Flow over IPC - " + count);
            Desktop.current().notifications().show("Flow IPC", "Button clicked " + count + " time(s)");
            Desktop.current().clipboard().writeText("Vaadin Flow over IPC clicks: " + count);
        });
        button.setId("ipc-button");

        VerticalLayout list = new VerticalLayout();
        list.setId("ipc-list");
        list.setPadding(false);
        list.setSpacing(false);
        list.add(new Span("1 Bootstrap over IPC READY"));
        list.add(new Span("2 Event dispatch over IPC READY"));
        list.add(new Span("3 Replace-root patch over IPC READY"));
        list.add(new Span("4 Desktop command/capabilityResult over IPC READY"));

        add(title, button, status, list);
    }
}
