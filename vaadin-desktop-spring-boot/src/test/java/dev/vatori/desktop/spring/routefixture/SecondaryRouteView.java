package dev.vatori.desktop.spring.routefixture;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

@Route("secondary")
public final class SecondaryRouteView extends Div {
    public SecondaryRouteView(RouteDiscoveryService service, RouteScopedLabel scopedLabel) {
        setText(service.message("secondary", scopedLabel));
    }
}
