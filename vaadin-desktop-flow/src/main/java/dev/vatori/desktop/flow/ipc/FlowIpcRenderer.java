package dev.vatori.desktop.flow.ipc;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.dom.Element;
import tools.jackson.databind.JsonNode;

import java.io.Serializable;
import java.util.ArrayList;
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
        node.put("nodeId", nodeId(component));
        String tag = tag(component);
        node.put("tag", tag);
        node.put("kind", kind(component, tag));
        node.put("text", text(component));
        node.put("attributes", attributes(component));
        node.put("properties", properties(component));
        node.put("clickable", component instanceof Button);
        node.put("children", component.getChildren()
                .map(this::renderComponent)
                .toList());
        return node;
    }

    private static int nodeId(Component component) {
        try {
            return component.getElement().getNode().getId();
        } catch (UnsupportedOperationException error) {
            return -1;
        }
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

    private static Map<String, Object> attributes(Component component) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        try {
            Element element = component.getElement();
            element.getAttributeNames()
                    .filter(name -> !"id".equalsIgnoreCase(name))
                    .forEach(name -> attributes.put(name, element.getAttribute(name)));
        } catch (UnsupportedOperationException error) {
            return Map.of();
        }
        return attributes;
    }

    private static Map<String, Object> properties(Component component) {
        Map<String, Object> properties = new LinkedHashMap<>();
        try {
            Element element = component.getElement();
            element.getPropertyNames()
                    .filter(FlowIpcRenderer::isSafePrimitivePropertyName)
                    .forEach(name -> {
                        Object value = rendererPropertyValue(name, element.getPropertyRaw(name));
                        if (value != null) {
                            properties.put(name, value);
                        }
                    });
        } catch (UnsupportedOperationException error) {
            return Map.of();
        }
        return properties;
    }

    private static boolean isSafePrimitivePropertyName(String name) {
        return !("id".equals(name)
                || "innerHTML".equals(name)
                || "outerHTML".equals(name)
                || "textContent".equals(name));
    }

    private static Object rendererPropertyValue(String name, Serializable value) {
        Object primitiveValue = primitivePropertyValue(value);
        if (primitiveValue != null) {
            return primitiveValue;
        }
        if ("i18n".equals(name) && value instanceof JsonNode jsonValue) {
            return jsonValue(jsonValue);
        }
        return null;
    }

    private static Object primitivePropertyValue(Serializable value) {
        if (value instanceof String || value instanceof Boolean || value instanceof Number) {
            return value;
        }
        return null;
    }

    private static Object jsonValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            Map<String, Object> values = new LinkedHashMap<>();
            for (Map.Entry<String, JsonNode> entry : node.properties()) {
                values.put(entry.getKey(), jsonValue(entry.getValue()));
            }
            return values;
        }
        if (node.isArray()) {
            List<Object> values = new ArrayList<>();
            for (JsonNode value : node.values()) {
                values.add(jsonValue(value));
            }
            return values;
        }
        if (node.isTextual()) {
            return node.stringValue();
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        if (node.isNumber()) {
            return node.numberValue();
        }
        return node.asString();
    }
}
