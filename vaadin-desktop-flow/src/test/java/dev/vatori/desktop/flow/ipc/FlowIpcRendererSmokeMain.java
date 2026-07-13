package dev.vatori.desktop.flow.ipc;

import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class FlowIpcRendererSmokeMain {
    private static final String PASSWORD_SECRET = "renderer-password-secret";
    private static final String TOKEN_SECRET = "renderer-token-secret";
    private static final String SESSION_SECRET = "renderer-session-secret";
    private static final String LOGIN_SECRET = "renderer-login-secret";

    private FlowIpcRendererSmokeMain() {
    }

    public static void main(String[] args) {
        VerticalLayout root = new VerticalLayout();
        root.setId("renderer-smoke-root");

        PasswordField passwordField = new PasswordField("Password");
        passwordField.setId("renderer-password-field");
        passwordField.setValue(PASSWORD_SECRET);
        passwordField.getElement().setAttribute("name", "password");
        passwordField.getElement().setAttribute("type", "password");
        passwordField.getElement().setAttribute("autocomplete", "current-password");
        passwordField.getElement().setAttribute("aria-label", "Password field");
        passwordField.getElement().setAttribute("value", PASSWORD_SECRET);
        passwordField.getElement().setAttribute("data-token", TOKEN_SECRET);
        passwordField.getElement().setProperty("label", "Renderer password");
        passwordField.getElement().setProperty("disabled", true);
        passwordField.getElement().setProperty("value", PASSWORD_SECRET);
        passwordField.getElement().setProperty("token", TOKEN_SECRET);
        passwordField.getElement().setProperty("securityKey", TOKEN_SECRET);
        passwordField.getElement().setProperty("sessionId", SESSION_SECRET);
        passwordField.getElement().setProperty("unsafeInfo", "renderer-unsafe-info");

        LoginForm loginForm = new LoginForm();
        loginForm.setId("renderer-login-form");
        loginForm.getElement().setProperty("password", LOGIN_SECRET);
        loginForm.getElement().setProperty("csrfToken", TOKEN_SECRET);
        loginForm.getElement().setProperty("label", "Renderer login");

        root.add(passwordField, loginForm);

        Map<String, Object> tree = new FlowIpcRenderer().render(root);
        Map<String, Object> passwordNode = requireNode(tree, "renderer-password-field");
        Map<String, Object> loginNode = requireNode(tree, "renderer-login-form");

        Map<?, ?> passwordAttributes = requireMap(passwordNode.get("attributes"), "password attributes");
        requireEquals("password", passwordAttributes.get("name"), "password field name attribute");
        requireEquals("password", passwordAttributes.get("type"), "password field type attribute");
        requireEquals("current-password", passwordAttributes.get("autocomplete"),
                "password field autocomplete attribute");
        requireEquals("Password field", passwordAttributes.get("aria-label"), "password field aria-label attribute");
        requireAbsent(passwordAttributes, "value", "password value attribute");
        requireAbsent(passwordAttributes, "data-token", "password data-token attribute");

        Map<?, ?> passwordProperties = requireMap(passwordNode.get("properties"), "password properties");
        requireEquals("Renderer password", passwordProperties.get("label"), "password safe label property");
        requireEquals(Boolean.TRUE, passwordProperties.get("disabled"), "password safe disabled property");
        requireAbsent(passwordProperties, "value", "password value property");
        requireAbsent(passwordProperties, "token", "password token property");
        requireAbsent(passwordProperties, "securityKey", "password securityKey property");
        requireAbsent(passwordProperties, "sessionId", "password sessionId property");
        requireAbsent(passwordProperties, "unsafeInfo", "password unsafeInfo property");

        Map<?, ?> loginProperties = requireMap(loginNode.get("properties"), "login properties");
        requireEquals("Renderer login", loginProperties.get("label"), "login safe label property");
        requireAbsent(loginProperties, "password", "login password property");
        requireAbsent(loginProperties, "csrfToken", "login csrfToken property");

        requireNotContains(tree, PASSWORD_SECRET, "password secret");
        requireNotContains(tree, TOKEN_SECRET, "token secret");
        requireNotContains(tree, SESSION_SECRET, "session secret");
        requireNotContains(tree, LOGIN_SECRET, "login secret");

        System.out.println("Flow IPC renderer state allowlist smoke passed");
    }

    private static Map<String, Object> requireNode(Map<String, Object> node, String id) {
        Map<String, Object> found = findNode(node, id);
        if (found == null) {
            throw new IllegalStateException("Renderer tree did not contain node id: " + id);
        }
        return found;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> findNode(Map<String, Object> node, String id) {
        if (Objects.equals(node.get("id"), id)) {
            return node;
        }
        Object children = node.get("children");
        if (children instanceof List<?> list) {
            for (Object child : list) {
                if (child instanceof Map<?, ?> childNode) {
                    Map<String, Object> found = findNode((Map<String, Object>) childNode, id);
                    if (found != null) {
                        return found;
                    }
                }
            }
        }
        return null;
    }

    private static Map<?, ?> requireMap(Object value, String label) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalStateException("Expected map for " + label + ", got: " + value);
        }
        return map;
    }

    private static void requireEquals(Object expected, Object actual, String label) {
        if (!Objects.equals(expected, actual)) {
            throw new IllegalStateException(label + " mismatch, expected " + expected + " but got " + actual);
        }
    }

    private static void requireAbsent(Map<?, ?> values, String key, String label) {
        if (values.containsKey(key)) {
            throw new IllegalStateException("Renderer exposed " + label + ": " + values.get(key));
        }
    }

    private static void requireNotContains(Object value, String needle, String label) {
        if (String.valueOf(value).contains(needle)) {
            throw new IllegalStateException("Renderer tree leaked " + label);
        }
    }
}
