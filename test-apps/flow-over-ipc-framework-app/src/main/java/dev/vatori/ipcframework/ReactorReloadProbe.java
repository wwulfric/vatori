package dev.vatori.ipcframework;

import com.vaadin.flow.component.html.Span;
import dev.vatori.ipcframework.shared.ReactorReloadValue;

final class ReactorReloadProbe extends Span {
    ReactorReloadProbe() {
        String revision = ReactorReloadValue.revision();
        setText("Reactor reload revision: " + revision);
        setId("framework-reactor-reload-probe");
        getElement().setAttribute("data-revision", revision);
    }
}
