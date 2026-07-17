package dev.vatori.desktop.flow;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.di.InstantiatorFactory;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Describes the Vaadin routes and optional dependency injection used by a desktop application. */
public final class DesktopFlowApplication {
    private final Class<? extends Component> rootRoute;
    private final List<Class<? extends Component>> routes;
    private final InstantiatorFactory instantiatorFactory;

    private DesktopFlowApplication(Builder builder) {
        routes = List.copyOf(builder.routes);
        List<Class<? extends Component>> roots = routes.stream()
                .filter(DesktopFlowApplication::isRootRoute)
                .toList();
        if (roots.size() != 1) {
            throw new IllegalArgumentException(
                    "Desktop Flow application requires exactly one root @Route or @RouteAlias, found "
                            + roots);
        }
        rootRoute = roots.getFirst();
        instantiatorFactory = builder.instantiatorFactory;
    }

    public static Builder builder(
            Class<? extends Component> firstRoute,
            Class<? extends Component>... additionalRoutes) {
        Builder builder = new Builder().route(firstRoute);
        for (Class<? extends Component> route : additionalRoutes) {
            builder.route(route);
        }
        return builder;
    }

    public Class<? extends Component> rootRoute() {
        return rootRoute;
    }

    public List<Class<? extends Component>> routes() {
        return routes;
    }

    public Optional<InstantiatorFactory> instantiatorFactory() {
        return Optional.ofNullable(instantiatorFactory);
    }

    private static boolean isRootRoute(Class<?> routeClass) {
        Route route = routeClass.getAnnotation(Route.class);
        if (route != null && route.value().isBlank()) {
            return true;
        }
        for (RouteAlias alias : routeClass.getAnnotationsByType(RouteAlias.class)) {
            if (alias.value().isBlank()) {
                return true;
            }
        }
        return false;
    }

    public static final class Builder {
        private final LinkedHashSet<Class<? extends Component>> routes = new LinkedHashSet<>();
        private InstantiatorFactory instantiatorFactory;

        private Builder() {
        }

        public Builder route(Class<? extends Component> routeClass) {
            routes.add(Objects.requireNonNull(routeClass, "routeClass"));
            return this;
        }

        public Builder routes(Collection<Class<? extends Component>> routeClasses) {
            Objects.requireNonNull(routeClasses, "routeClasses");
            new ArrayList<>(routeClasses).forEach(this::route);
            return this;
        }

        public Builder instantiatorFactory(InstantiatorFactory factory) {
            instantiatorFactory = Objects.requireNonNull(factory, "factory");
            return this;
        }

        public DesktopFlowApplication build() {
            return new DesktopFlowApplication(this);
        }
    }
}
