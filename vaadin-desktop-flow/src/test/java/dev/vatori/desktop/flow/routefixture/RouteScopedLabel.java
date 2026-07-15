package dev.vatori.desktop.flow.routefixture;

import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;

@SpringComponent
@UIScope
public class RouteScopedLabel {
    public String value() {
        return "with UI scope";
    }
}
