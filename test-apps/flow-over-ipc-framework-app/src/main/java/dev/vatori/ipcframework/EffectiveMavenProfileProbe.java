package dev.vatori.ipcframework;

import com.vaadin.flow.component.html.Span;
import java.lang.reflect.InvocationTargetException;

final class EffectiveMavenProfileProbe extends Span {
    private static final String VALUE_CLASS =
            "dev.vatori.ipcframework.profileshared."
                    + "EffectiveMavenProfileValue";

    EffectiveMavenProfileProbe() {
        String sourceRevision = "unavailable";
        String resourceRevision = "unavailable";
        boolean profileModuleAvailable = false;
        try {
            Class<?> valueClass = Class.forName(VALUE_CLASS);
            sourceRevision = invoke(valueClass, "sourceRevision");
            resourceRevision = invoke(valueClass, "resourceRevision");
            profileModuleAvailable = true;
        } catch (ClassNotFoundException exception) {
            // The fixture is intentionally absent outside its Maven profile.
        }
        setText("Effective Maven profile: source=" + sourceRevision
                + ", resource=" + resourceRevision);
        setId("framework-effective-maven-profile-probe");
        getElement().setAttribute(
                "data-profile-module-available",
                Boolean.toString(profileModuleAvailable));
        getElement().setAttribute(
                "data-source-revision", sourceRevision);
        getElement().setAttribute(
                "data-resource-revision", resourceRevision);
    }

    private static String invoke(Class<?> valueClass, String method) {
        try {
            return (String) valueClass.getMethod(method).invoke(null);
        } catch (NoSuchMethodException | IllegalAccessException
                 | InvocationTargetException exception) {
            throw new IllegalStateException(
                    "Cannot read effective Maven profile value", exception);
        }
    }
}
