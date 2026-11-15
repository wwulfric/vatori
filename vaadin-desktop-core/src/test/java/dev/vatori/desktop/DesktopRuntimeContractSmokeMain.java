package dev.vatori.desktop;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class DesktopRuntimeContractSmokeMain {
    public static void main(String[] args) {
        var directCalls = new AtomicInteger();
        var capturedCalls = new AtomicInteger();
        var response = DesktopCommandResult.ok(Map.of("status", "none"));
        NativeBridge captured = command -> {
            check(command.name().equals("runtime.replacement.status"));
            check(command.payload().isEmpty());
            capturedCalls.incrementAndGet();
            return response;
        };
        NativeBridge direct = new NativeBridge() {
            public DesktopCommandResult invoke(DesktopCommand command) {
                check(command.name().equals("runtime.replacement.status"));
                check(command.payload().isEmpty());
                directCalls.incrementAndGet();
                return response;
            }
            public NativeBridge captureCapabilityContext() { return captured; }
        };
        var context = new DesktopContext(DesktopApplication.builder("Runtime contract").build(),
                URI.create("https://app.vatori.invalid/"), direct, Map.of());
        check(context.runtime().replacementStatus() == response);
        check(context.captureCapabilityContext().runtime().replacementStatus() == response);
        check(directCalls.get() == 1 && capturedCalls.get() == 1);
        var denied = DesktopCommandResult.error("denied");
        check(new DesktopRuntime(command -> denied).replacementStatus() == denied);
        var requests = new AtomicInteger();
        var runtime = new DesktopRuntime(command -> {
            check(command.name().equals("runtime.replacement.request"));
            check(command.payload().equals(Map.of("requestId", "request", "candidateId", "next")));
            requests.incrementAndGet();
            return response;
        });
        check(runtime.requestReplacement("request", "next") == response);
        for (String invalid : new String[] {"", "../next", "x".repeat(129)}) {
            try { runtime.requestReplacement("request", invalid); throw new AssertionError("accepted invalid candidate"); }
            catch (IllegalArgumentException expected) { }
        }
        check(requests.get() == 1);
        System.out.println("Desktop runtime query facade smoke passed (exact result and captured owner bridge)");
    }

    private static void check(boolean valid) {
        if (!valid) throw new AssertionError("runtime facade contract");
    }
}
