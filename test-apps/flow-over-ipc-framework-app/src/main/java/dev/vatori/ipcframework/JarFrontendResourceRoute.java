package dev.vatori.ipcframework;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import dev.vatori.ipcframework.frontendaddon.JarFrontendResourceProbe;

import java.util.concurrent.atomic.AtomicInteger;

@Route("jar-frontend")
public final class JarFrontendResourceRoute extends VerticalLayout {
    public JarFrontendResourceRoute() {
        setId("framework-jar-frontend-route");
        setPadding(true);
        setSpacing(true);

        H1 title = new H1("Dependency JAR frontend resource graph");
        Span status = new Span("Dependency JAR frontend ready");
        status.setId("framework-jar-frontend-status");

        JarFrontendResourceProbe probe = new JarFrontendResourceProbe();
        probe.setId("framework-jar-frontend-probe");
        AtomicInteger activationCount = new AtomicInteger();
        probe.addActivationListener(event -> {
            int count = activationCount.incrementAndGet();
            probe.setServerMessage("server-ack-" + count);
            status.setText("Dependency JAR frontend event: count=" + count);
        });

        Button returnToFramework = new Button(
                "Return from dependency JAR frontend", event ->
                event.getSource().getUI().ifPresent(ui ->
                        ui.navigate(FlowOverIpcFrameworkView.class)));
        returnToFramework.setId("framework-jar-frontend-return");

        add(title, probe, status, returnToFramework);
    }
}
