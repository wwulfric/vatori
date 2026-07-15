package dev.vatori.desktop.flow;

import com.vaadin.flow.component.Component;

import java.util.List;
import java.util.Objects;

record FlowIpcRouteCatalog(
        Class<? extends Component> rootRoute,
        List<Class<? extends Component>> routes) {
    FlowIpcRouteCatalog {
        Objects.requireNonNull(rootRoute, "rootRoute");
        routes = List.copyOf(routes);
        if (!routes.contains(rootRoute)) {
            throw new IllegalArgumentException("Flow IPC routes must contain the root route");
        }
    }
}
