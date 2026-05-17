package dev.vatori.desktop.flow.ipc;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.button.Button;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

final class FlowIpcRenderer {
    private final IdentityHashMap<Component, String> ids = new IdentityHashMap<>();
    private final Map<String, Component> components = new LinkedHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger();

    Map<String, Object> render(Component component) {
        components.clear();
        return renderComponent(component);
    }

    Component component(String componentId) {
        return components.get(componentId);
    }

    private Map<String, Object> renderComponent(Component component) {
        String id = id(component);
        components.put(id, component);

        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", id);
        String tag = tag(component);
        node.put("tag", tag);
        node.put("kind", kind(component, tag));
        node.put("text", text(component));
        node.put("clickable", component instanceof Button);
        node.put("children", component.getChildren()
                .map(this::renderComponent)
                .toList());
        return node;
    }

    private String id(Component component) {
        Optional<String> explicit;
        try {
            explicit = component.getId();
        } catch (UnsupportedOperationException error) {
            explicit = Optional.empty();
        }
        if (explicit.isPresent() && !explicit.get().isBlank()) {
            return explicit.get();
        }
        return ids.computeIfAbsent(component, ignored -> "flow-ipc-c" + sequence.incrementAndGet());
    }

    private static String tag(Component component) {
        try {
            String tag = component.getElement().getTag();
            return tag == null || tag.isBlank() ? "text" : tag;
        } catch (UnsupportedOperationException error) {
            return "text";
        }
    }

    private static String kind(Component component, String tag) {
        if (component instanceof Button) {
            return "button";
        }
        if (tag.contains("vertical-layout") || tag.contains("horizontal-layout")) {
            return "layout";
        }
        if ("text".equals(tag)) {
            return "text";
        }
        return tag;
    }

    private static String text(Component component) {
        if (component instanceof HasText hasText) {
            return hasText.getText();
        }
        String directText;
        try {
            directText = component.getElement().getText();
        } catch (UnsupportedOperationException error) {
            directText = "";
        }
        if (directText != null && !directText.isBlank()) {
            return directText;
        }
        List<Component> children = component.getChildren().toList();
        if (children.isEmpty()) {
            String recursive;
            try {
                recursive = component.getElement().getTextRecursively();
            } catch (UnsupportedOperationException error) {
                recursive = "";
            }
            return recursive == null ? "" : recursive;
        }
        return "";
    }
}
