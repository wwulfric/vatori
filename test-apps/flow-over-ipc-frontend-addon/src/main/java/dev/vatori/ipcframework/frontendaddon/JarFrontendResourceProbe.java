package dev.vatori.ipcframework.frontendaddon;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.shared.Registration;

@Tag("vatori-jar-frontend-probe")
@JsModule("./vatori-jar-frontend-probe.js")
@CssImport("./vatori-jar-frontend-probe.css")
public final class JarFrontendResourceProbe extends Component {
    public JarFrontendResourceProbe() {
        getElement().setProperty("serverMessage", "server-ready");
        getElement().addEventListener(
                "vatori-addon-activate",
                event -> fireEvent(new ActivationEvent(this, true)));
    }

    public Registration addActivationListener(
            ComponentEventListener<ActivationEvent> listener) {
        return addListener(ActivationEvent.class, listener);
    }

    public void setServerMessage(String message) {
        getElement().setProperty("serverMessage", message);
    }

    public static final class ActivationEvent
            extends ComponentEvent<JarFrontendResourceProbe> {
        private ActivationEvent(
                JarFrontendResourceProbe source,
                boolean fromClient) {
            super(source, fromClient);
        }
    }
}
