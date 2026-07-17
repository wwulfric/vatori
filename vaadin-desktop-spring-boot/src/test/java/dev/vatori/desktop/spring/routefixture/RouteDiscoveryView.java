package dev.vatori.desktop.spring.routefixture;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

@Route("")
public final class RouteDiscoveryView extends Div {
    public RouteDiscoveryView(RouteDiscoveryService service, RouteScopedLabel scopedLabel) {
        setText(service.message("root", scopedLabel));
    }
}
