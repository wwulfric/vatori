package dev.vatori.flowipc.demo;

import dev.vatori.flowipc.runtime.MockFlowRuntime;
import dev.vatori.flowipc.spi.FlowTransport;

import java.util.List;
import java.util.Map;

public final class InMemoryDemoMain {
    private InMemoryDemoMain() {
    }

    public static void main(String[] args) {
        FlowTransport transport = new MockFlowRuntime();

        FlowTransport.BootstrapResult bootstrap = transport.bootstrap(
                new FlowTransport.BootstrapRequest("/counter", "session-demo", Map.of("engine", "inmemory")));
        print("BOOTSTRAP", bootstrap.patches());

        FlowTransport.PatchResult clickOne = transport.dispatch(new FlowTransport.ClientEvent(
                bootstrap.sessionId(), bootstrap.uiId(), "counter.increment", "click", Map.of()));
        print("CLICK_1", clickOne.patches());

        FlowTransport.PatchResult clickTwo = transport.dispatch(new FlowTransport.ClientEvent(
                bootstrap.sessionId(), bootstrap.uiId(), "counter.increment", "click", Map.of()));
        print("CLICK_2", clickTwo.patches());

        FlowTransport.PatchResult rows = transport.dispatch(new FlowTransport.ClientEvent(
                bootstrap.sessionId(), bootstrap.uiId(), "orders.grid", "requestRows",
                Map.of("offset", 0, "limit", 3)));
        print("GRID_ROWS", rows.patches());

        FlowTransport.PatchResult resync = transport.resync(new FlowTransport.ResyncRequest(
                bootstrap.sessionId(), bootstrap.uiId(), "inmemory-resync-check"));
        print("RESYNC", resync.patches());

        assertPatch(clickTwo.patches(), "setText", "#counter.value", "2");
        assertPatch(rows.patches(), "replaceRows", "#orders.grid", null);
        System.out.println("ASSERT OK: in-memory transport completed button -> listener -> patch without HTTP.");
    }

    private static void print(String label, List<FlowTransport.Patch> patches) {
        System.out.println(label);
        for (FlowTransport.Patch patch : patches) {
            System.out.println("  " + patch);
        }
    }

    private static void assertPatch(List<FlowTransport.Patch> patches, String op, String target, String text) {
        boolean matched = patches.stream().anyMatch(patch -> {
            if (!op.equals(patch.op()) || !target.equals(patch.target())) {
                return false;
            }
            return text == null || text.equals(String.valueOf(patch.value().get("text")));
        });
        if (!matched) {
            throw new IllegalStateException("Expected patch not found: op=" + op + ", target=" + target);
        }
    }
}

