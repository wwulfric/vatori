package dev.vatori.ipcframework;

import com.vaadin.flow.component.html.Span;

final class JavaReloadProbe extends Span {
    private static final String REVISION = "baseline";

    JavaReloadProbe() {
        super("Java reload revision: " + REVISION);
        setId("framework-java-reload-probe");
        getElement().setAttribute("data-revision", REVISION);
    }
}
