package dev.vatori.ipcframework;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLayout;

@JsModule("./vatori-lazy-layout-probe.js")
@CssImport("./styles/vatori-lazy-layout.css")
public final class LazyFrontendLayout extends VerticalLayout implements RouterLayout {
    public LazyFrontendLayout() {
        setId("framework-nested-lazy-layout");
        addClassName("vatori-lazy-layout");
        setPadding(true);
        setSpacing(true);

        Span title = new Span("Nested lazy layout");
        title.setId("framework-nested-lazy-layout-title");

        LazyLayoutProbe probe = new LazyLayoutProbe();
        probe.getElement().setProperty("message", "layout ready");
        add(title, probe);
    }

    @Tag("vatori-lazy-layout-probe")
    public static final class LazyLayoutProbe extends Component {
        private LazyLayoutProbe() {
            setId("framework-nested-lazy-layout-probe");
        }
    }
}
