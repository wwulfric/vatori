package dev.vatori.desktop.flow;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class FlowIpcRouteResolver {
    private FlowIpcRouteResolver() {
    }

    static FlowIpcRouteCatalog resolve(Class<?> applicationClass) {
        String basePackage = ClassUtils.getPackageName(applicationClass);
        if (basePackage.isBlank()) {
            throw new IllegalArgumentException(
                    "Flow IPC route discovery requires the application class to be in a named package");
        }

        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Route.class));

        List<Class<? extends Component>> routes = new ArrayList<>();
        List<Class<? extends Component>> rootRoutes = new ArrayList<>();
        for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage)) {
            Class<?> candidateClass = resolveCandidate(applicationClass, candidate);
            if (!Component.class.isAssignableFrom(candidateClass)) {
                continue;
            }
            Class<? extends Component> routeClass = candidateClass.asSubclass(Component.class);
            routes.add(routeClass);
            if (isRootRoute(routeClass)) {
                rootRoutes.add(routeClass);
            }
        }
        routes.sort(Comparator.comparing(Class::getName));
        rootRoutes.sort(Comparator.comparing(Class::getName));

        if (rootRoutes.size() == 1) {
            return new FlowIpcRouteCatalog(rootRoutes.getFirst(), routes);
        }
        if (rootRoutes.isEmpty()) {
            throw new IllegalStateException("No root @Route or @RouteAlias was found under " + basePackage);
        }
        throw new IllegalStateException("Multiple root Flow routes were found under " + basePackage
                + ": " + rootRoutes.stream().map(Class::getName).toList());
    }

    private static Class<?> resolveCandidate(
            Class<?> applicationClass,
            BeanDefinition candidate) {
        String className = candidate.getBeanClassName();
        if (className == null || className.isBlank()) {
            throw new IllegalStateException("Flow IPC route candidate has no class name");
        }
        ClassLoader classLoader = applicationClass.getClassLoader();
        return ClassUtils.resolveClassName(className, classLoader);
    }

    private static boolean isRootRoute(Class<?> candidateClass) {
        Route route = candidateClass.getAnnotation(Route.class);
        if (route != null && route.value().isBlank()) {
            return true;
        }
        for (RouteAlias alias : candidateClass.getAnnotationsByType(RouteAlias.class)) {
            if (alias.value().isBlank()) {
                return true;
            }
        }
        return false;
    }
}
