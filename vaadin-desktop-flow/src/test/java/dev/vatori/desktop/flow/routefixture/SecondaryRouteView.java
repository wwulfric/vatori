package dev.vatori.desktop.flow.routefixture;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

@Route("secondary")
public final class SecondaryRouteView extends Div {
    public SecondaryRouteView() {
        setText("Pure secondary route");
    }
}
