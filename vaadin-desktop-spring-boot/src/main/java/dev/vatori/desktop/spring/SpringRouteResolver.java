package dev.vatori.desktop.spring;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class SpringRouteResolver {
    private SpringRouteResolver() {
    }

    static List<Class<? extends Component>> resolve(Class<?> applicationClass) {
        String basePackage = ClassUtils.getPackageName(applicationClass);
        if (basePackage.isBlank()) {
            throw new IllegalArgumentException(
                    "Spring desktop route discovery requires the application class to be in a named package");
        }

        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Route.class));

        List<Class<? extends Component>> routes = new ArrayList<>();
        for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage)) {
            String className = candidate.getBeanClassName();
            if (className == null || className.isBlank()) {
                throw new IllegalStateException("Spring desktop route candidate has no class name");
            }
            Class<?> candidateClass = ClassUtils.resolveClassName(
                    className, applicationClass.getClassLoader());
            if (Component.class.isAssignableFrom(candidateClass)) {
                routes.add(candidateClass.asSubclass(Component.class));
            }
        }
        routes.sort(Comparator.comparing(Class::getName));
        if (routes.isEmpty()) {
            throw new IllegalStateException("No @Route was found under " + basePackage);
        }
        return List.copyOf(routes);
    }
}
