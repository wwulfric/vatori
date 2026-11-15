package dev.vatori.ipcframework;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import dev.vatori.desktop.DesktopApplication;
import dev.vatori.desktop.DesktopCapabilityContext;
import dev.vatori.desktop.DesktopCommandResult;
import dev.vatori.desktop.DesktopContext;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Fixture validation only: fake public bridge and server-side clicks, no OS/IPC claim. */
public final class FlowWindowChurnFixtureSmokeMain {
    public static void main(String[] args) {
        var first = new FlowWindowChurnRoute();
        var sibling = new FlowWindowChurnRoute();
        require(!span(first, "window-churn-document").getText().equals(
                span(sibling, "window-churn-document").getText()), "document identity");
        ((Button) find(first, "window-churn-mutate")).click();
        require(span(first, "window-churn-state").getText().equals("change=1 client=false"), "server click evidence");
        require(span(sibling, "window-churn-state").getText().equals("change=0"), "sibling local state");
        Map<String, Object> payload = Map.of("canceled", true, "operationInvalidated", true,
                "cancelReason", "owner-window-closed");
        List<String> titles = new ArrayList<>();
        List<DesktopCapabilityContext> contexts = new ArrayList<>();
        for (int i = 0; i < 10; i++) contexts.add(context(DesktopCommandResult.error(payload, "retired"), titles));
        require(FlowWindowChurnRoute.rejectRetired(contexts, "before") == 10, "ten retired contexts");
        require(titles.size() == 10 && titles.get(9).equals("Stale churn before 9"), "all probes actually invoked");
        for (String key : payload.keySet()) {
            var missing = new LinkedHashMap<>(payload);
            missing.remove(key);
            require(FlowWindowChurnRoute.rejectRetired(List.of(context(
                    DesktopCommandResult.error(missing, "incomplete"), titles)), "after") == 0, "missing " + key);
        }
        for (DesktopCommandResult wrong : List.of(DesktopCommandResult.ok(payload),
                DesktopCommandResult.error("generic failure"),
                DesktopCommandResult.error(Map.of("canceled", true, "operationInvalidated", true,
                        "cancelReason", "sidecar-exit"), "wrong owner lifetime"))) {
            require(FlowWindowChurnRoute.rejectRetired(List.of(context(wrong, titles)), "after") == 0,
                    "wrong result rejected");
        }
        System.out.println("PASS churn fixture: isolated server state and ten-context rejection predicate; no WKWebView/IPC evidence");
    }

    private static DesktopCapabilityContext context(DesktopCommandResult result, List<String> titles) {
        return new DesktopContext(DesktopApplication.builder("fixture").build(), URI.create("app://fixture"), command -> {
            require(command.name().equals("window.setTitle"), "public title command");
            titles.add(command.payload().get("title").toString());
            return result;
        }, Map.of()).captureCapabilityContext();
    }

    private static Component find(FlowWindowChurnRoute route, String id) {
        return route.getChildren().filter(child -> child.getId().orElse("").equals(id)).findFirst().orElseThrow();
    }

    private static Span span(FlowWindowChurnRoute route, String id) { return (Span) find(route, id); }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
