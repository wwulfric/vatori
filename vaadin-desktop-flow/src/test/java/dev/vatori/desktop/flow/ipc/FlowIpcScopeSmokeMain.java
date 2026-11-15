package dev.vatori.desktop.flow.ipc;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.VaadinSessionState;
import dev.vatori.desktop.DesktopApplication;
import dev.vatori.desktop.DesktopCapabilityContext;
import dev.vatori.desktop.DesktopCommand;
import dev.vatori.desktop.DesktopCommandResult;
import dev.vatori.desktop.DesktopContext;
import dev.vatori.desktop.NativeBridge;
import dev.vatori.desktop.flow.transport.DesktopFlowHttpResponse;
import dev.vatori.desktop.flow.transport.DesktopFlowHttpResponseHead;
import dev.vatori.desktop.flow.transport.DesktopFlowOpaqueHttpSession;
import dev.vatori.desktop.flow.transport.DesktopFlowOpaqueHttpSessionRegistry;
import dev.vatori.desktop.flow.transport.DesktopFlowPushMessage;
import dev.vatori.desktop.ipc.SidecarWire;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class FlowIpcScopeSmokeMain {
    private FlowIpcScopeSmokeMain() {
    }

    public static void main(String[] args) throws Exception {
        verifyOpaqueRequestBodyCredits();
        verifyOpaqueRequestBodyCancelUnblocksReader();
        verifyOpaqueResponseBackpressureAndCancel();
        verifyOpaqueExchangeRegistryLifecycle();
        verifyEarlyResponseWaitsForRequestEnd();
        verifyOpaqueSessionRegistry();
        verifyPushDeliveryAcknowledgement();
        verifyWindowCloseScopeBeforeCancellation();
        verifyWindowCloseFollowsQueuedRequests();
        verifyDifferentWindowHandlersCanProgressIndependently();
        verifyReopenedWindowWaitsForPriorCloseTeardown();
        verifyRetiredScopeFramesAreFencedAcrossReopen();
        verifyRepeatedReopenSessionOwnership();
        verifyFailedPreCloseContextRollsBackBeforeReopen();
        verifyStandardBundleRejectsUnknownTransport();
        verifyDefaultCapabilityContextCaptureCompatibility();
        verifySingleWindowCompatibilityWithCurrentVaadinSession();
        verifyCapturedCapabilityContextKeepsExactScope();
        verifyCapturedCapabilityContextRetiresWithScope();
        verifyUnboundCapabilityScopeFailsClosedWhenAmbiguous();
        verifyNativeCapabilityCorrelation();
        verifyNativeCapabilityFailurePayload();
        verifyNativeCapabilityNullPayloadSettles();
        verifyNativeCapabilityScopeMismatch();
        verifyMessageLoopReadsCapabilityResultWhileHandlerWaits();
        verifyWindowCloseReleasesPendingNativeCapability();
        verifyUnbootstrappedDefaultWindowCloseDropsRetiredTraffic();
        verifyClosedNativeCapabilityScopeCannotReactivate();
        verifyNativeScopeRegistrationRollbackPreservesLifecycle();
        verifyClosedSecondaryScopeCannotReactivateAfterTeardown();
        verifyLateNativeCapabilityResultAfterWindowCloseIsIgnored();
        verifyProtocolVersionAndFraming();
        System.out.println("standard Flow IPC transport smoke passed");
    }

    private static void verifyProtocolVersionAndFraming() {
        SidecarInboundEnvelope.parse("""
                {"version":1,"type":"shutdown","payload":{}}
                """);
        for (String invalid : List.of(
                "{\"type\":\"shutdown\",\"payload\":{}}",
                "{\"version\":2,\"type\":\"shutdown\",\"payload\":{}}",
                "{\"version\":\"1\",\"type\":\"shutdown\",\"payload\":{}}")) {
            try {
                SidecarInboundEnvelope.parse(invalid);
                throw new IllegalStateException("Sidecar accepted an invalid protocol version");
            } catch (SidecarProtocolException expected) {
                // Expected strict numeric version guard.
            }
        }
        String invalidVersionOutput = captureRuntime(
                SidecarWire.PREFIX
                        + "{\"version\":2,\"type\":\"shutdown\",\"payload\":{}}\n");
        if (!invalidVersionOutput.contains("\"code\":\"protocolVersion\"")) {
            throw new IllegalStateException(
                    "Unknown protocol version did not produce a protocol error: "
                            + invalidVersionOutput);
        }
        String invalidFramingOutput = captureRuntime(
                "VFIPC2 {\"version\":1,\"type\":\"shutdown\",\"payload\":{}}\n");
        if (!invalidFramingOutput.contains("\"code\":\"protocolFraming\"")) {
            throw new IllegalStateException(
                    "Invalid protocol framing did not produce a protocol error: "
                            + invalidFramingOutput);
        }
    }

    private static void verifyStandardBundleRejectsUnknownTransport() {
        String input = SidecarWire.PREFIX
                + envelope("unsupportedMessage", "unsupported", "flow-ipc-session", "main") + "\n"
                + SidecarWire.PREFIX
                + envelope("shutdown", "standard-shutdown", "flow-ipc-session", "main") + "\n";
        String output = captureRuntime(input);
        if (!output.contains("\"runtime\":\"standard-vaadin-bundle-opaque-ipc\"")
                || !output.contains("\"uidlFallback\":\"none\"")
                || !output.contains("\"id\":\"unsupported\"")
                || !output.contains("\"code\":\"unsupportedTransport\"")) {
            throw new IllegalStateException(
                    "Standard bundle runtime accepted an unknown transport message:\n" + output);
        }
    }

    private static void verifyNativeCapabilityCorrelation() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        FlowIpcScope scope = new FlowIpcScope("main", "flow-ipc-session", "main");
        SidecarOutput sidecarOutput = new SidecarOutput(
                new PrintStream(output, true, StandardCharsets.UTF_8), "", scope);
        SidecarNativeBridge bridge = new SidecarNativeBridge(sidecarOutput, scope);
        AtomicReference<DesktopCommandResult> result = new AtomicReference<>();
        Thread caller = Thread.ofVirtual().start(() -> result.set(bridge.invoke(
                new DesktopCommand("clipboard.readText", Map.of()))));
        for (int attempt = 0; attempt < 100 && output.size() == 0; attempt++) {
            Thread.sleep(5);
        }
        if (!output.toString(StandardCharsets.UTF_8).contains("\"type\":\"command\"")) {
            throw new IllegalStateException("Native bridge did not emit a command envelope");
        }
        bridge.complete(scope, SidecarInboundEnvelope.parse("""
                {"version":1,"type":"capabilityResult","id":"capability-desktop-command-1",
                 "correlationId":"desktop-command-1","sessionId":"flow-ipc-session",
                 "uiId":"main","windowId":"main",
                 "payload":{"success":true,"text":"from-native"}}
                """));
        caller.join(1000);
        DesktopCommandResult commandResult = result.get();
        bridge.close();
        if (caller.isAlive() || commandResult == null || !commandResult.success()
                || !"from-native".equals(commandResult.payload().get("text"))) {
            throw new IllegalStateException(
                    "Native bridge did not correlate the real capability result");
        }
    }

    private static void verifyDefaultCapabilityContextCaptureCompatibility() {
        NativeBridge bridge = command -> DesktopCommandResult.ok(Map.of(
                "command", command.name()));
        DesktopCapabilityContext captured = desktopContext(bridge)
                .captureCapabilityContext();
        DesktopCommandResult result = captured.setWindowTitle("Default capture");
        if (captured.nativeBridge() != bridge
                || !result.success()
                || !"window.setTitle".equals(result.payload().get("command"))) {
            throw new IllegalStateException(
                    "Default NativeBridge capture changed compatibility semantics");
        }
    }

    private static void verifySingleWindowCompatibilityWithCurrentVaadinSession()
            throws Exception {
        FlowIpcScope defaultScope = new FlowIpcScope(
                "main", "compatibility-session", "main");
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        SidecarOutput output = new SidecarOutput(
                new PrintStream(capturedOutput, true, StandardCharsets.UTF_8),
                "",
                defaultScope);
        SidecarNativeBridge bridge = SidecarNativeBridge.singleWindow(
                output, defaultScope);
        DesktopFlowOpaqueHttpSessionRegistry sessions =
                DesktopFlowOpaqueHttpSessionRegistry.create(
                        "compatibility-capability-context-smoke",
                        "",
                        ScopeSmokeView.class,
                        ignored -> {
                        });
        VaadinSession currentSession = vaadinSession(
                sessions.session("unregistered-compatibility-session"));
        Thread directCaller = null;
        Thread capturedCaller = null;
        try {
            AtomicReference<DesktopCommandResult> directResult =
                    new AtomicReference<>();
            directCaller = Thread.ofVirtual().start(() -> {
                VaadinSession.setCurrent(currentSession);
                try {
                    directResult.set(bridge.invoke(new DesktopCommand(
                            "window.setTitle", Map.of("title", "Compatibility"))));
                } finally {
                    VaadinSession.setCurrent(null);
                }
            });
            awaitOutput(capturedOutput, "desktop-command-1");
            bridge.complete(defaultScope, SidecarInboundEnvelope.parse("""
                    {"version":1,"type":"capabilityResult",
                     "id":"capability-desktop-command-1",
                     "correlationId":"desktop-command-1",
                     "sessionId":"compatibility-session","uiId":"main",
                     "windowId":"main","payload":{"success":true}}
                    """));
            directCaller.join(1000);

            VaadinSession.setCurrent(currentSession);
            DesktopCapabilityContext captured = desktopContext(bridge)
                    .captureCapabilityContext();
            VaadinSession.setCurrent(null);
            AtomicReference<DesktopCommandResult> capturedResult =
                    new AtomicReference<>();
            capturedCaller = Thread.ofVirtual().start(() -> capturedResult.set(
                    captured.setWindowTitle("Captured compatibility")));
            awaitOutput(capturedOutput, "desktop-command-2");
            bridge.complete(defaultScope, SidecarInboundEnvelope.parse("""
                    {"version":1,"type":"capabilityResult",
                     "id":"capability-desktop-command-2",
                     "correlationId":"desktop-command-2",
                     "sessionId":"compatibility-session","uiId":"main",
                     "windowId":"main","payload":{"success":true}}
                    """));
            capturedCaller.join(1000);

            if (directCaller.isAlive()
                    || capturedCaller.isAlive()
                    || directResult.get() == null
                    || !directResult.get().success()
                    || capturedResult.get() == null
                    || !capturedResult.get().success()
                    || !capturedOutput.toString(StandardCharsets.UTF_8)
                            .contains("\"sessionId\":\"compatibility-session\"")) {
                throw new IllegalStateException(
                        "Single-window compatibility lost its default capability scope");
            }
        } finally {
            VaadinSession.setCurrent(null);
            bridge.close();
            sessions.close();
            if (directCaller != null) {
                directCaller.join(1000);
            }
            if (capturedCaller != null) {
                capturedCaller.join(1000);
            }
        }
    }

    private static void verifyCapturedCapabilityContextKeepsExactScope()
            throws Exception {
        FlowIpcScope defaultScope = new FlowIpcScope(
                "main", "flow-ipc-session", "main");
        FlowIpcScope secondaryScope = new FlowIpcScope(
                "secondary",
                "flow-ipc-window:11111111-2222-4333-8444-555555555555",
                "secondary");
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        SidecarOutput output = new SidecarOutput(
                new PrintStream(capturedOutput, true, StandardCharsets.UTF_8),
                "",
                defaultScope);
        SidecarNativeBridge bridge = new SidecarNativeBridge(output, defaultScope);
        DesktopFlowOpaqueHttpSessionRegistry sessions =
                DesktopFlowOpaqueHttpSessionRegistry.create(
                        "captured-capability-scope-smoke",
                        "",
                        ScopeSmokeView.class,
                        ignored -> {
                        });
        Thread exactCaller = null;
        Thread mismatchCaller = null;
        try {
            bridge.registerScope(secondaryScope);
            VaadinSession.setCurrent(vaadinSession(
                    sessions.session(secondaryScope.sessionId())));
            DesktopCapabilityContext captured = desktopContext(bridge)
                    .captureCapabilityContext();
            VaadinSession.setCurrent(null);

            AtomicReference<DesktopCommandResult> exactResult =
                    new AtomicReference<>();
            exactCaller = Thread.ofVirtual().start(() -> exactResult.set(
                    captured.setWindowTitle("Captured secondary")));
            awaitOutput(capturedOutput, "desktop-command-1");
            String firstEnvelope = capturedOutput.toString(StandardCharsets.UTF_8);
            if (!firstEnvelope.contains(
                    "\"sessionId\":\"" + secondaryScope.sessionId() + "\"")
                    || !firstEnvelope.contains(
                    "\"windowId\":\"secondary\"")) {
                throw new IllegalStateException(
                        "Captured capability context did not emit the exact secondary scope:\n"
                                + firstEnvelope);
            }
            bridge.complete(secondaryScope, SidecarInboundEnvelope.parse("""
                    {"version":1,"type":"capabilityResult",
                     "id":"capability-desktop-command-1",
                     "correlationId":"desktop-command-1",
                     "sessionId":"%s","uiId":"secondary",
                     "windowId":"secondary","payload":{"success":true}}
                    """.formatted(secondaryScope.sessionId())));
            exactCaller.join(1000);
            if (exactCaller.isAlive()
                    || exactResult.get() == null
                    || !exactResult.get().success()) {
                throw new IllegalStateException(
                        "Captured secondary capability did not settle in its exact scope");
            }

            AtomicReference<DesktopCommandResult> mismatchResult =
                    new AtomicReference<>();
            mismatchCaller = Thread.ofVirtual().start(() -> mismatchResult.set(
                    captured.clipboard().writeText("scope mismatch")));
            awaitOutput(capturedOutput, "desktop-command-2");
            try {
                bridge.complete(defaultScope, SidecarInboundEnvelope.parse("""
                        {"version":1,"type":"capabilityResult",
                         "id":"capability-desktop-command-2",
                         "correlationId":"desktop-command-2",
                         "sessionId":"flow-ipc-session","uiId":"main",
                         "windowId":"main","payload":{"success":true}}
                        """));
                throw new IllegalStateException(
                        "Captured capability accepted a result from another scope");
            } catch (SecurityException expected) {
                // Correlation alone cannot move a captured invocation across scopes.
            }
            mismatchCaller.join(1000);
            DesktopCommandResult rejected = mismatchResult.get();
            if (mismatchCaller.isAlive()
                    || rejected == null
                    || rejected.success()
                    || !"Native capability result scope mismatch".equals(
                            rejected.error())) {
                throw new IllegalStateException(
                        "Wrong-scope completion did not safely reject a captured invocation");
            }
        } finally {
            VaadinSession.setCurrent(null);
            bridge.unregisterScope(secondaryScope);
            bridge.close();
            sessions.close();
            if (exactCaller != null) {
                exactCaller.join(1000);
            }
            if (mismatchCaller != null) {
                mismatchCaller.join(1000);
            }
        }
    }

    private static void verifyCapturedCapabilityContextRetiresWithScope()
            throws Exception {
        FlowIpcScope defaultScope = new FlowIpcScope(
                "main", "flow-ipc-session", "main");
        FlowIpcScope originalScope = new FlowIpcScope(
                "secondary",
                "flow-ipc-window:aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee",
                "secondary");
        FlowIpcScope reopenedScope = new FlowIpcScope(
                "secondary",
                "flow-ipc-window:99999999-8888-4777-8666-555555555555",
                "secondary");
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        SidecarOutput output = new SidecarOutput(
                new PrintStream(capturedOutput, true, StandardCharsets.UTF_8),
                "",
                defaultScope);
        SidecarNativeBridge bridge = new SidecarNativeBridge(output, defaultScope);
        DesktopFlowOpaqueHttpSessionRegistry sessions =
                DesktopFlowOpaqueHttpSessionRegistry.create(
                        "captured-capability-retirement-smoke",
                        "",
                        ScopeSmokeView.class,
                        ignored -> {
                        });
        Thread pendingCaller = null;
        Thread reopenedCaller = null;
        try {
            bridge.registerScope(originalScope);
            VaadinSession.setCurrent(vaadinSession(
                    sessions.session(originalScope.sessionId())));
            DesktopCapabilityContext originalContext = desktopContext(bridge)
                    .captureCapabilityContext();
            VaadinSession.setCurrent(null);

            AtomicReference<DesktopCommandResult> pendingResult =
                    new AtomicReference<>();
            pendingCaller = Thread.ofVirtual().start(() -> pendingResult.set(
                    originalContext.nativeBridge().invoke(new DesktopCommand(
                            "fileDialog.openFile", Map.of("title", "Retire owner")))));
            awaitOutput(capturedOutput, "desktop-command-1");
            bridge.closeScope(originalScope);
            pendingCaller.join(1000);
            if (pendingCaller.isAlive()
                    || !isOwnerWindowClosed(pendingResult.get())) {
                throw new IllegalStateException(
                        "Scope close did not settle a pending captured capability");
            }

            bridge.unregisterScope(originalScope);
            bridge.registerScope(reopenedScope);
            int outputBytesBeforeLateInvocation = capturedOutput.size();
            DesktopCommandResult lateResult = originalContext.setWindowTitle(
                    "Must not reach reopened window");
            if (!isOwnerWindowClosed(lateResult)
                    || capturedOutput.size() != outputBytesBeforeLateInvocation) {
                throw new IllegalStateException(
                        "Retired captured context retargeted a reopened public window id");
            }

            VaadinSession.setCurrent(vaadinSession(
                    sessions.session(reopenedScope.sessionId())));
            DesktopCapabilityContext reopenedContext = desktopContext(bridge)
                    .captureCapabilityContext();
            VaadinSession.setCurrent(null);
            AtomicReference<DesktopCommandResult> reopenedResult =
                    new AtomicReference<>();
            reopenedCaller = Thread.ofVirtual().start(() -> reopenedResult.set(
                    reopenedContext.setWindowTitle("Reopened secondary")));
            awaitOutput(capturedOutput, "desktop-command-3");
            String reopenedEnvelope = capturedOutput.toString(StandardCharsets.UTF_8);
            if (!reopenedEnvelope.contains(
                    "\"sessionId\":\"" + reopenedScope.sessionId() + "\"")) {
                throw new IllegalStateException(
                        "New captured context did not use the reopened scope UUID");
            }
            bridge.complete(reopenedScope, SidecarInboundEnvelope.parse("""
                    {"version":1,"type":"capabilityResult",
                     "id":"capability-desktop-command-3",
                     "correlationId":"desktop-command-3",
                     "sessionId":"%s","uiId":"secondary",
                     "windowId":"secondary","payload":{"success":true}}
                    """.formatted(reopenedScope.sessionId())));
            reopenedCaller.join(1000);
            if (reopenedCaller.isAlive()
                    || reopenedResult.get() == null
                    || !reopenedResult.get().success()) {
                throw new IllegalStateException(
                        "Reopened scope could not create a fresh captured context");
            }
            // Exercise the public runtime facade after the fresh owner has
            // already completed a capability. Reusing a window label must not
            // revive either reads or mutating requests from the retired owner.
            int bytesBeforeRetiredRuntimeCalls = capturedOutput.size();
            if (VaadinSession.getCurrent() != null) {
                throw new IllegalStateException("Runtime probe inherited a Flow session");
            }
            DesktopCommandResult retiredStatus = originalContext.runtime()
                    .replacementStatus();
            DesktopCommandResult retiredRequest = originalContext.runtime()
                    .requestReplacement("stale-owner-probe", "next");
            if (!isOwnerWindowClosed(retiredStatus)
                    || !isOwnerWindowClosed(retiredRequest)
                    || !Boolean.TRUE.equals(retiredStatus.payload().get("canceled"))
                    || !Boolean.TRUE.equals(retiredRequest.payload().get("canceled"))
                    || !"runtime.replacement.status".equals(
                            retiredStatus.payload().get("command"))
                    || !"runtime.replacement.request".equals(
                            retiredRequest.payload().get("command"))
                    || capturedOutput.size() != bytesBeforeRetiredRuntimeCalls) {
                throw new IllegalStateException(
                        "Retired runtime facade emitted IPC or lost typed invalidation");
            }
        } finally {
            VaadinSession.setCurrent(null);
            bridge.closeScope(reopenedScope);
            bridge.unregisterScope(reopenedScope);
            bridge.close();
            sessions.close();
            if (pendingCaller != null) {
                pendingCaller.join(1000);
            }
            if (reopenedCaller != null) {
                reopenedCaller.join(1000);
            }
        }
    }

    private static void verifyUnboundCapabilityScopeFailsClosedWhenAmbiguous()
            throws Exception {
        FlowIpcScope defaultScope = new FlowIpcScope(
                "main", "flow-ipc-session", "main");
        FlowIpcScope secondaryScope = new FlowIpcScope(
                "secondary",
                "flow-ipc-window:12345678-1234-4234-8234-123456789abc",
                "secondary");
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        SidecarOutput output = new SidecarOutput(
                new PrintStream(capturedOutput, true, StandardCharsets.UTF_8),
                "",
                defaultScope);
        SidecarNativeBridge bridge = new SidecarNativeBridge(output, defaultScope);
        Thread fallbackCaller = null;
        try {
            VaadinSession.setCurrent(null);
            bridge.registerScope(secondaryScope);
            DesktopCommandResult ambiguous = bridge.invoke(new DesktopCommand(
                    "notification.show", Map.of("title", "Ambiguous", "body", "scope")));
            if (ambiguous.success()
                    || !("Native capability scope is ambiguous without a current Vaadin session; "
                            + "capture a DesktopCapabilityContext before leaving the Flow callback")
                            .equals(ambiguous.error())
                    || capturedOutput.size() != 0) {
                throw new IllegalStateException(
                        "Unbound invocation did not fail closed with multiple live scopes");
            }

            bridge.unregisterScope(secondaryScope);
            AtomicReference<DesktopCommandResult> fallbackResult =
                    new AtomicReference<>();
            fallbackCaller = Thread.ofVirtual().start(() -> fallbackResult.set(
                    bridge.invoke(new DesktopCommand(
                            "clipboard.readText", Map.of()))));
            awaitOutput(capturedOutput, "desktop-command-2");
            String fallbackEnvelope = capturedOutput.toString(StandardCharsets.UTF_8);
            if (!fallbackEnvelope.contains(
                    "\"sessionId\":\"flow-ipc-session\"")
                    || !fallbackEnvelope.contains("\"windowId\":\"main\"")) {
                throw new IllegalStateException(
                        "Single live default scope lost legacy main fallback");
            }
            bridge.complete(defaultScope, SidecarInboundEnvelope.parse("""
                    {"version":1,"type":"capabilityResult",
                     "id":"capability-desktop-command-2",
                     "correlationId":"desktop-command-2",
                     "sessionId":"flow-ipc-session","uiId":"main",
                     "windowId":"main",
                     "payload":{"success":true,"text":"legacy-main"}}
                    """));
            fallbackCaller.join(1000);
            if (fallbackCaller.isAlive()
                    || fallbackResult.get() == null
                    || !fallbackResult.get().success()) {
                throw new IllegalStateException(
                        "Single default scope fallback did not complete");
            }
        } finally {
            bridge.close();
            if (fallbackCaller != null) {
                fallbackCaller.join(1000);
            }
        }
    }

    private static void verifyNativeCapabilityFailurePayload() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        FlowIpcScope scope = new FlowIpcScope("main", "flow-ipc-session", "main");
        SidecarOutput sidecarOutput = new SidecarOutput(
                new PrintStream(output, true, StandardCharsets.UTF_8), "", scope);
        SidecarNativeBridge bridge = new SidecarNativeBridge(sidecarOutput, scope);
        AtomicReference<DesktopCommandResult> result = new AtomicReference<>();
        Thread caller = Thread.ofVirtual().start(() -> result.set(bridge.invoke(
                new DesktopCommand("clipboard.readText", Map.of()))));
        try {
            for (int attempt = 0; attempt < 100 && output.size() == 0; attempt++) {
                Thread.sleep(5);
            }
            if (!output.toString(StandardCharsets.UTF_8).contains("\"type\":\"command\"")) {
                throw new IllegalStateException(
                        "Native bridge did not emit a command for the denied capability");
            }

            bridge.complete(scope, SidecarInboundEnvelope.parse("""
                    {"version":1,"type":"capabilityResult","id":"capability-unrelated",
                     "correlationId":"desktop-command-unrelated","sessionId":"flow-ipc-session",
                     "uiId":"main","windowId":"main",
                     "payload":{"success":false,"error":"unrelated"}}
                    """));
            Thread.sleep(20);
            if (!caller.isAlive() || result.get() != null) {
                throw new IllegalStateException(
                        "Native bridge completed a command from an unrelated correlation id");
            }

            Map<String, Object> expectedPayload = Map.ofEntries(
                    Map.entry("success", false),
                    Map.entry("command", "clipboard.readText"),
                    Map.entry("error", "Command not allowed by desktop-capabilities.json"),
                    Map.entry("permissionMode", "manifest-enforced"),
                    Map.entry("permissionDecision", "denied"),
                    Map.entry("permissionReason", "manifest-denied"),
                    Map.entry("permissionScope", "window:main"),
                    Map.entry("permissionManifestLoaded", true),
                    Map.entry("permissionManifestPath", "/bundle/desktop-capabilities.json"),
                    Map.entry("permissionManifestCommands", "clipboard.writeText"));
            bridge.complete(scope, SidecarInboundEnvelope.parse("""
                    {"version":1,"type":"capabilityResult","id":"capability-desktop-command-1",
                     "correlationId":"desktop-command-1","sessionId":"flow-ipc-session",
                     "uiId":"main","windowId":"main",
                     "payload":{"success":false,"command":"clipboard.readText",
                     "error":"Command not allowed by desktop-capabilities.json",
                     "permissionMode":"manifest-enforced","permissionDecision":"denied",
                     "permissionReason":"manifest-denied","permissionScope":"window:main",
                     "permissionManifestLoaded":true,
                     "permissionManifestPath":"/bundle/desktop-capabilities.json",
                     "permissionManifestCommands":"clipboard.writeText"}}
                    """));
            caller.join(1000);
            DesktopCommandResult commandResult = result.get();
            if (caller.isAlive() || commandResult == null || commandResult.success()
                    || !"Command not allowed by desktop-capabilities.json"
                    .equals(commandResult.error())
                    || !expectedPayload.equals(commandResult.payload())) {
                throw new IllegalStateException(
                        "Native bridge did not preserve the denied capability payload");
            }
        } finally {
            bridge.close();
            caller.join(1000);
        }
    }

    private static void verifyNativeCapabilityNullPayloadSettles()
            throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        FlowIpcScope scope = new FlowIpcScope(
                "main", "flow-ipc-session", "main");
        SidecarOutput sidecarOutput = new SidecarOutput(
                new PrintStream(output, true, StandardCharsets.UTF_8),
                "",
                scope);
        SidecarNativeBridge bridge = new SidecarNativeBridge(
                sidecarOutput, scope);
        AtomicReference<DesktopCommandResult> result = new AtomicReference<>();
        Thread caller = Thread.ofVirtual().start(() -> result.set(bridge.invoke(
                new DesktopCommand("clipboard.readText", Map.of()))));
        try {
            awaitOutput(output, "desktop-command-1");
            bridge.complete(scope, SidecarInboundEnvelope.parse("""
                    {"version":1,"type":"capabilityResult",
                     "id":"capability-desktop-command-1",
                     "correlationId":"desktop-command-1",
                     "sessionId":"flow-ipc-session","uiId":"main",
                     "windowId":"main",
                     "payload":{"success":true,"text":null}}
                    """));
            caller.join(1000);
            DesktopCommandResult settled = result.get();
            if (caller.isAlive()
                    || settled == null
                    || !settled.success()
                    || !settled.payload().containsKey("text")
                    || settled.payload().get("text") != null) {
                throw new IllegalStateException(
                        "Null-valued capability payload did not settle safely");
            }
        } finally {
            bridge.close();
            caller.join(1000);
        }
    }

    private static void verifyNativeCapabilityScopeMismatch() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        FlowIpcScope scope = new FlowIpcScope("main", "flow-ipc-session", "main");
        SidecarOutput sidecarOutput = new SidecarOutput(
                new PrintStream(output, true, StandardCharsets.UTF_8), "", scope);
        SidecarNativeBridge bridge = new SidecarNativeBridge(sidecarOutput, scope);
        AtomicReference<DesktopCommandResult> result = new AtomicReference<>();
        Thread caller = Thread.ofVirtual().start(() -> result.set(bridge.invoke(
                new DesktopCommand("window.setTitle", Map.of("title", "Scope smoke")))));
        try {
            for (int attempt = 0; attempt < 100 && output.size() == 0; attempt++) {
                Thread.sleep(5);
            }
            try {
                bridge.complete(
                        new FlowIpcScope(
                                "secondary", "flow-ipc-session-secondary", "secondary"),
                        SidecarInboundEnvelope.parse("""
                                {"version":1,"type":"capabilityResult",
                                 "id":"capability-desktop-command-1",
                                 "correlationId":"desktop-command-1",
                                 "sessionId":"flow-ipc-session-secondary",
                                 "uiId":"secondary","windowId":"secondary",
                                 "payload":{"success":false,
                                 "permissionDecision":"denied",
                                 "permissionScope":"window:secondary",
                                 "error":"forged scope"}}
                                """));
                throw new IllegalStateException(
                        "Native bridge accepted a capability result from another scope");
            } catch (SecurityException expected) {
                // A correlated result must still belong to the issuing Flow IPC scope.
            }
            caller.join(1000);
            DesktopCommandResult commandResult = result.get();
            if (caller.isAlive() || commandResult == null || commandResult.success()
                    || !"Native capability result scope mismatch".equals(commandResult.error())
                    || !commandResult.payload().isEmpty()) {
                throw new IllegalStateException(
                        "Native bridge did not reject the mismatched capability result scope");
            }
        } finally {
            bridge.close();
            caller.join(1000);
        }
    }

    private static void verifyMessageLoopReadsCapabilityResultWhileHandlerWaits()
            throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        FlowIpcScope scope = new FlowIpcScope("main", "flow-ipc-session", "main");
        SidecarOutput sidecarOutput = new SidecarOutput(
                new PrintStream(output, true, StandardCharsets.UTF_8), "", scope);
        SidecarNativeBridge bridge = new SidecarNativeBridge(sidecarOutput, scope);
        PipedWriter inputWriter = new PipedWriter();
        SidecarMessageLoop messageLoop = new SidecarMessageLoop(
                new BufferedReader(new PipedReader(inputWriter)),
                sidecarOutput,
                bridge,
                scope);
        AtomicReference<DesktopCommandResult> result = new AtomicReference<>();
        messageLoop.register("invokeCapability", (ignoredScope, envelope, id, correlationId) ->
                result.set(bridge.invoke(new DesktopCommand(
                        "clipboard.readText", Map.of()))));
        Thread loop = Thread.ofVirtual().start(messageLoop::run);
        inputWriter.write(SidecarWire.PREFIX
                + envelope("invokeCapability", "invoke", "flow-ipc-session", "main")
                + "\n");
        inputWriter.flush();
        for (int attempt = 0; attempt < 100
                && !output.toString(StandardCharsets.UTF_8)
                        .contains("\"type\":\"command\""); attempt++) {
            Thread.sleep(5);
        }
        if (!output.toString(StandardCharsets.UTF_8).contains("\"type\":\"command\"")) {
            throw new IllegalStateException(
                    "Sidecar handler did not emit a native capability command");
        }
        inputWriter.write(SidecarWire.PREFIX + """
                {"version":1,"type":"capabilityResult","id":"capability-desktop-command-1",
                 "correlationId":"desktop-command-1","sessionId":"flow-ipc-session",
                 "uiId":"main","windowId":"main",
                 "payload":{"success":true,"text":"from-message-loop"}}
                """.replace("\n", "") + "\n");
        inputWriter.write(SidecarWire.PREFIX
                + envelope("shutdown", "shutdown", "flow-ipc-session", "main")
                + "\n");
        inputWriter.flush();
        inputWriter.close();
        loop.join(1000);
        DesktopCommandResult commandResult = result.get();
        if (loop.isAlive() || commandResult == null || !commandResult.success()
                || !"from-message-loop".equals(commandResult.payload().get("text"))) {
            throw new IllegalStateException(
                    "Sidecar reader could not correlate a capability result while its handler waited");
        }
    }

    private static void verifyWindowCloseReleasesPendingNativeCapability()
            throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        FlowIpcScope scope = new FlowIpcScope(
                "main", "flow-ipc-session", "main");
        SidecarOutput sidecarOutput = new SidecarOutput(
                new PrintStream(output, true, StandardCharsets.UTF_8), "", scope);
        SidecarNativeBridge bridge = new SidecarNativeBridge(sidecarOutput, scope);
        PipedWriter inputWriter = new PipedWriter();
        SidecarMessageLoop messageLoop = new SidecarMessageLoop(
                new BufferedReader(new PipedReader(inputWriter)),
                sidecarOutput,
                bridge,
                scope);
        AtomicReference<DesktopCommandResult> result = new AtomicReference<>();
        AtomicInteger stage = new AtomicInteger();
        messageLoop.register(
                "invokeCapability",
                (ignoredScope, envelope, id, correlationId) -> {
                    result.set(bridge.invoke(new DesktopCommand(
                            "fileDialog.openFile", Map.of("title", "Close scope"))));
                    stage.compareAndSet(0, 1);
                });
        messageLoop.onWindowClosed(ignoredScope -> {
            if (!stage.compareAndSet(1, 2)) {
                throw new IllegalStateException(
                        "Window close did not release the pending native capability first");
            }
            messageLoop.requestShutdown();
        });
        Thread loop = Thread.ofVirtual().start(messageLoop::run);
        try {
            inputWriter.write(SidecarWire.PREFIX
                    + envelope(
                            "invokeCapability",
                            "invoke-close-scope",
                            "flow-ipc-session",
                            "main")
                    + "\n");
            inputWriter.flush();
            for (int attempt = 0; attempt < 100
                    && !output.toString(StandardCharsets.UTF_8)
                            .contains("\"type\":\"command\""); attempt++) {
                Thread.sleep(5);
            }
            if (!output.toString(StandardCharsets.UTF_8)
                    .contains("\"type\":\"command\"")) {
                throw new IllegalStateException(
                        "Pending native capability was not emitted before window close");
            }
            inputWriter.write(SidecarWire.PREFIX
                    + envelope(
                            "windowClosed",
                            "closed-pending-capability",
                            "flow-ipc-session",
                            "main")
                    + "\n");
            inputWriter.flush();
            inputWriter.close();
            loop.join(1000);
            DesktopCommandResult commandResult = result.get();
            if (loop.isAlive()
                    || stage.get() != 2
                    || commandResult == null
                    || commandResult.success()
                    || !"Owner window closed before native capability completed"
                            .equals(commandResult.error())
                    || !Boolean.TRUE.equals(
                            commandResult.payload().get("canceled"))
                    || !"owner-window-closed".equals(
                            commandResult.payload().get("cancelReason"))
                    || !Boolean.TRUE.equals(
                            commandResult.payload().get("operationInvalidated"))) {
                throw new IllegalStateException(
                        "Window close did not deterministically settle the pending native capability"
                                + ": loopAlive=" + loop.isAlive()
                                + ", stage=" + stage.get()
                                + ", result=" + commandResult);
            }
        } finally {
            messageLoop.requestShutdown();
            inputWriter.close();
            loop.join(1000);
            if (loop.isAlive()) {
                throw new IllegalStateException(
                        "Unbootstrapped close smoke did not stop its message loop");
            }
        }
    }

    private static void verifyClosedNativeCapabilityScopeCannotReactivate() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        FlowIpcScope scope = new FlowIpcScope(
                "main", "flow-ipc-session", "main");
        SidecarOutput sidecarOutput = new SidecarOutput(
                new PrintStream(output, true, StandardCharsets.UTF_8), "", scope);
        SidecarNativeBridge bridge = new SidecarNativeBridge(sidecarOutput, scope);
        try {
            bridge.closeScope(scope);
            DesktopCommandResult rejected = bridge.invoke(new DesktopCommand(
                    "clipboard.readText", Map.of()));
            if (rejected.success()
                    || !"owner-window-closed".equals(
                            rejected.payload().get("cancelReason"))) {
                throw new IllegalStateException(
                        "Closed native capability scope accepted a new command");
            }
            try {
                bridge.registerScope(scope);
                throw new IllegalStateException(
                        "Closed native capability scope was reactivated");
            } catch (IllegalStateException expected) {
                if (!expected.getMessage().startsWith(
                        "Closed Flow IPC scope cannot be reactivated")) {
                    throw expected;
                }
            }
            bridge.unregisterScope(scope);
            DesktopCommandResult afterTeardown = bridge.invoke(new DesktopCommand(
                    "clipboard.readText", Map.of()));
            if (afterTeardown.success()
                    || !"owner-window-closed".equals(
                            afterTeardown.payload().get("cancelReason"))) {
                throw new IllegalStateException(
                        "Retired default scope emitted a command after teardown");
            }
            if (output.size() != 0) {
                throw new IllegalStateException(
                        "Rejected closed-scope commands reached the sidecar output");
            }
        } finally {
            bridge.close();
        }
    }

    private static void verifyNativeScopeRegistrationRollbackPreservesLifecycle()
            throws Exception {
        FlowIpcScope defaultScope = new FlowIpcScope(
                "main", "flow-ipc-session", "main");
        FlowIpcScope temporaryClaim = new FlowIpcScope(
                "temporary", defaultScope.sessionId(), "temporary");
        FlowIpcScope concurrentlyClosed = new FlowIpcScope(
                "secondary", "flow-ipc-window:rollback-closed", "secondary");
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        SidecarOutput output = new SidecarOutput(
                new PrintStream(captured, true, StandardCharsets.UTF_8),
                "",
                defaultScope);
        SidecarNativeBridge bridge = new SidecarNativeBridge(
                output, defaultScope);
        Thread caller = null;
        try {
            bridge.registerScope(defaultScope);
            bridge.rollbackScopeRegistration(defaultScope);
            bridge.registerScope(temporaryClaim);
            bridge.unregisterScope(temporaryClaim);
            bridge.registerScope(defaultScope);

            AtomicReference<DesktopCommandResult> result =
                    new AtomicReference<>();
            caller = Thread.ofVirtual().start(() -> result.set(bridge.invoke(
                    new DesktopCommand("clipboard.readText", Map.of()))));
            awaitOutput(captured, "desktop-command-1");
            bridge.complete(defaultScope, SidecarInboundEnvelope.parse("""
                    {"version":1,"type":"capabilityResult",
                     "id":"capability-desktop-command-1",
                     "correlationId":"desktop-command-1",
                     "sessionId":"flow-ipc-session","uiId":"main",
                     "windowId":"main",
                     "payload":{"success":true,"text":"rollback-live"}}
                    """));
            caller.join(1000);
            if (caller.isAlive()
                    || result.get() == null
                    || !result.get().success()
                    || !"rollback-live".equals(
                            result.get().payload().get("text"))) {
                throw new IllegalStateException(
                        "Registration rollback retired the live default scope");
            }

            bridge.registerScope(concurrentlyClosed);
            bridge.closeScope(concurrentlyClosed);
            bridge.rollbackScopeRegistration(concurrentlyClosed);
            try {
                bridge.registerScope(concurrentlyClosed);
                throw new IllegalStateException(
                        "Registration rollback erased a concurrent close tombstone");
            } catch (IllegalStateException expected) {
                if (!expected.getMessage().startsWith(
                        "Closed Flow IPC scope cannot be reactivated")) {
                    throw expected;
                }
            }
        } finally {
            bridge.close();
            if (caller != null) {
                caller.join(1000);
            }
        }
    }

    private static void verifyUnbootstrappedDefaultWindowCloseDropsRetiredTraffic()
            throws Exception {
        FlowIpcScope scope = new FlowIpcScope(
                "main", "flow-ipc-session", "main");
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        SidecarOutput output = new SidecarOutput(
                new PrintStream(captured, true, StandardCharsets.UTF_8),
                "", scope);
        SidecarNativeBridge bridge = new SidecarNativeBridge(output, scope);
        PipedWriter inputWriter = new PipedWriter();
        SidecarMessageLoop messageLoop = new SidecarMessageLoop(
                new BufferedReader(new PipedReader(inputWriter)),
                output,
                bridge,
                scope);
        CountDownLatch closeHandled = new CountDownLatch(1);
        AtomicInteger invocationCount = new AtomicInteger();
        messageLoop.onWindowClosed(ignored -> closeHandled.countDown());
        messageLoop.register(
                "invokeAfterClose",
                (ignoredScope, envelope, id, correlationId) ->
                        invocationCount.incrementAndGet());
        Thread loop = Thread.ofVirtual().start(messageLoop::run);
        try {
            inputWriter.write(SidecarWire.PREFIX
                    + envelope(
                            "windowClosed",
                            "unbootstrapped-main-close",
                            "flow-ipc-session",
                            "main") + "\n");
            inputWriter.write(SidecarWire.PREFIX
                    + envelope(
                            "invokeAfterClose",
                            "invoke-after-unbootstrapped-close",
                            "flow-ipc-session",
                            "main") + "\n");
            inputWriter.flush();
            if (!closeHandled.await(1, TimeUnit.SECONDS)
                    || invocationCount.get() != 0
                    || captured.size() != 0) {
                throw new IllegalStateException(
                        "Unbootstrapped default window admitted retired traffic");
            }
        } finally {
            messageLoop.requestShutdown();
            inputWriter.close();
            loop.join(1000);
        }
    }

    private static void verifyClosedSecondaryScopeCannotReactivateAfterTeardown() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        FlowIpcScope defaultScope = new FlowIpcScope(
                "main", "flow-ipc-session", "main");
        FlowIpcScope closedScope = new FlowIpcScope(
                "secondary", "flow-ipc-window:closed", "secondary");
        FlowIpcScope replacementScope = new FlowIpcScope(
                "replacement", "flow-ipc-window:closed", "replacement");
        FlowIpcScope reopenedScope = new FlowIpcScope(
                "secondary", "flow-ipc-window:reopened", "secondary");
        SidecarOutput sidecarOutput = new SidecarOutput(
                new PrintStream(output, true, StandardCharsets.UTF_8),
                "",
                defaultScope);
        SidecarNativeBridge bridge = new SidecarNativeBridge(
                sidecarOutput, defaultScope);
        try {
            bridge.registerScope(closedScope);
            bridge.closeScope(closedScope);
            bridge.unregisterScope(closedScope);
            try {
                bridge.registerScope(closedScope);
                throw new IllegalStateException(
                        "Closed secondary native scope was reactivated after teardown");
            } catch (IllegalStateException expected) {
                if (!expected.getMessage().startsWith(
                        "Closed Flow IPC scope cannot be reactivated")) {
                    throw expected;
                }
            }
            bridge.registerScope(replacementScope);
            bridge.unregisterScope(replacementScope);
            bridge.registerScope(reopenedScope);
            bridge.unregisterScope(reopenedScope);
            if (output.size() != 0) {
                throw new IllegalStateException(
                        "Secondary scope lifecycle emitted sidecar output");
            }
        } finally {
            bridge.close();
        }
    }

    private static void verifyLateNativeCapabilityResultAfterWindowCloseIsIgnored()
            throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        FlowIpcScope scope = new FlowIpcScope(
                "main", "flow-ipc-session", "main");
        SidecarOutput sidecarOutput = new SidecarOutput(
                new PrintStream(output, true, StandardCharsets.UTF_8), "", scope);
        SidecarNativeBridge bridge = new SidecarNativeBridge(sidecarOutput, scope);
        AtomicReference<DesktopCommandResult> result = new AtomicReference<>();
        Thread caller = Thread.ofVirtual().start(() -> result.set(bridge.invoke(
                new DesktopCommand("fileDialog.openFile", Map.of()))));
        try {
            for (int attempt = 0; attempt < 100
                    && !output.toString(StandardCharsets.UTF_8)
                            .contains("desktop-command-1"); attempt++) {
                Thread.sleep(5);
            }
            bridge.closeScope(scope);
            caller.join(1000);
            DesktopCommandResult closedResult = result.get();
            if (caller.isAlive()
                    || closedResult == null
                    || !"owner-window-closed".equals(
                            closedResult.payload().get("cancelReason"))) {
                throw new IllegalStateException(
                        "Scope close did not win the pending capability result");
            }
            bridge.complete(scope, SidecarInboundEnvelope.parse("""
                    {"version":1,"type":"capabilityResult",
                     "id":"capability-desktop-command-1",
                     "correlationId":"desktop-command-1",
                     "sessionId":"flow-ipc-session","uiId":"main",
                     "windowId":"main",
                     "payload":{"success":true,"path":"/late"}}
                    """));
            if (result.get() != closedResult) {
                throw new IllegalStateException(
                        "Late capability result replaced the scope-close result");
            }
        } finally {
            bridge.close();
            caller.join(1000);
        }
    }

    private static String captureRuntime(String input) {
        InputStream originalIn = System.in;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
            FlowIpcScope scope = new FlowIpcScope("main", "flow-ipc-session", "main");
            SidecarOutput output = new SidecarOutput(
                    new PrintStream(captured, true, StandardCharsets.UTF_8), "", scope);
            SidecarNativeBridge nativeBridge = new SidecarNativeBridge(output, scope);
            StandardFlowIpcRuntime runtime = new StandardFlowIpcRuntime(
                    "",
                    List.of(ScopeSmokeView.class),
                    null,
                    DesktopApplication.builder("Scope Smoke").build(),
                    output,
                    nativeBridge,
                    scope);
            runtime.run();
            nativeBridge.close();
        } finally {
            System.setIn(originalIn);
        }
        return captured.toString(StandardCharsets.UTF_8);
    }

    private static DesktopContext desktopContext(NativeBridge nativeBridge) {
        return new DesktopContext(
                DesktopApplication.builder("Captured Capability Smoke").build(),
                URI.create("vatori-app://app/"),
                nativeBridge,
                Map.of());
    }

    private static void awaitOutput(
            ByteArrayOutputStream output, String marker) throws InterruptedException {
        for (int attempt = 0; attempt < 100
                && !output.toString(StandardCharsets.UTF_8).contains(marker);
                attempt++) {
            Thread.sleep(5);
        }
        if (!output.toString(StandardCharsets.UTF_8).contains(marker)) {
            throw new IllegalStateException(
                    "Native bridge did not emit expected marker " + marker);
        }
    }

    private static boolean isOwnerWindowClosed(DesktopCommandResult result) {
        return result != null
                && !result.success()
                && "owner-window-closed".equals(
                        result.payload().get("cancelReason"))
                && Boolean.TRUE.equals(
                        result.payload().get("operationInvalidated"));
    }

    private static void verifyOpaqueRequestBodyCredits() throws Exception {
        AtomicInteger credits = new AtomicInteger();
        OpaqueHttpRequestBodyInputStream body =
                new OpaqueHttpRequestBodyInputStream(6, credits::addAndGet);
        body.accept(0, "abc".getBytes(StandardCharsets.UTF_8));
        body.accept(1, "def".getBytes(StandardCharsets.UTF_8));
        body.finish(2, 6);
        if (!Arrays.equals(body.readAllBytes(), "abcdef".getBytes(StandardCharsets.UTF_8))
                || credits.get() != 2) {
            throw new IllegalStateException("Opaque request stream did not replenish consumed chunk credits");
        }
        if (body.read() != -1 || body.read(new byte[1]) != -1) {
            throw new IllegalStateException("Opaque request EOF must remain terminal");
        }

        OpaqueHttpRequestBodyInputStream invalid =
                new OpaqueHttpRequestBodyInputStream(-1, ignored -> {
                });
        try {
            invalid.accept(1, new byte[]{1});
            throw new IllegalStateException("Opaque request stream accepted an out-of-order chunk");
        } catch (IllegalArgumentException expected) {
            // Expected sequence guard.
        }
    }

    private static void verifyOpaqueRequestBodyCancelUnblocksReader()
            throws Exception {
        OpaqueHttpRequestBodyInputStream body =
                new OpaqueHttpRequestBodyInputStream(-1, ignored -> {
                });
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread reader = Thread.ofVirtual().start(() -> {
            try {
                body.readAllBytes();
            } catch (Throwable error) {
                failure.set(error);
            }
        });
        Thread.sleep(20);
        body.cancel();
        reader.join(1000);
        if (reader.isAlive()
                || !(failure.get() instanceof java.io.IOException)) {
            throw new IllegalStateException(
                    "Opaque request cancellation did not unblock its reader",
                    failure.get());
        }
    }

    private static void verifyOpaqueResponseBackpressureAndCancel() throws Exception {
        List<byte[]> chunks = new ArrayList<>();
        AtomicInteger endedChunks = new AtomicInteger(-1);
        OpaqueHttpResponseOutputStream response = new OpaqueHttpResponseOutputStream(
                new OpaqueHttpResponseOutputStream.ResponseSender() {
                    @Override
                    public void start(DesktopFlowHttpResponseHead head) {
                    }

                    @Override
                    public void chunk(int sequence, byte[] bytes) {
                        if (sequence != chunks.size()) {
                            throw new IllegalStateException("Opaque response chunk sequence is not ordered");
                        }
                        chunks.add(bytes);
                    }

                    @Override
                    public void end(int chunkCount, long byteCount) {
                        endedChunks.set(chunkCount);
                    }
                });
        response.start(new DesktopFlowHttpResponseHead(200, "OK", Map.of()));
        byte[] bytes = new byte[OpaqueHttpResponseOutputStream.CHUNK_SIZE * 2 + 7];
        Arrays.fill(bytes, (byte) 0x41);
        AtomicBoolean completed = new AtomicBoolean();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread writer = Thread.ofVirtual().start(() -> {
            try {
                response.write(bytes);
                response.finish();
                completed.set(true);
            } catch (Throwable error) {
                failure.set(error);
            }
        });
        Thread.sleep(20);
        if (completed.get()) {
            throw new IllegalStateException("Opaque response ignored its credit window");
        }
        response.grantCredits(1);
        Thread.sleep(20);
        if (completed.get() || chunks.size() != 1) {
            throw new IllegalStateException("Opaque response did not stop after consuming one credit");
        }
        response.grantCredits(2);
        writer.join(1000);
        if (!completed.get() || failure.get() != null || chunks.size() != 3
                || endedChunks.get() != 3) {
            throw new IllegalStateException("Opaque response did not resume after receiving credit", failure.get());
        }

        OpaqueHttpResponseOutputStream cancelled = new OpaqueHttpResponseOutputStream(
                new NoopResponseSender());
        cancelled.start(new DesktopFlowHttpResponseHead(200, "OK", Map.of()));
        AtomicReference<Throwable> cancelFailure = new AtomicReference<>();
        Thread cancelledWriter = Thread.ofVirtual().start(() -> {
            try {
                cancelled.write(new byte[OpaqueHttpResponseOutputStream.CHUNK_SIZE]);
            } catch (Throwable error) {
                cancelFailure.set(error);
            }
        });
        Thread.sleep(20);
        cancelled.cancel();
        cancelledWriter.join(1000);
        if (cancelledWriter.isAlive() || cancelFailure.get() == null) {
            throw new IllegalStateException("Opaque response cancellation did not unblock its writer");
        }
    }

    private static void verifyOpaqueExchangeRegistryLifecycle()
            throws Exception {
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        FlowIpcScope scope = new FlowIpcScope(
                "main", "flow-ipc-registry-session", "main");
        SidecarOutput output = new SidecarOutput(
                new PrintStream(captured, true, StandardCharsets.UTF_8),
                "", scope);
        DesktopFlowOpaqueHttpSession session =
                DesktopFlowOpaqueHttpSession.create(
                        scope.sessionId(), "registry-smoke", "",
                        ScopeSmokeView.class);
        try (StandardFlowIpcExchanges exchanges =
                     new StandardFlowIpcExchanges(output)) {
            String requestId = "reusable-request";
            exchanges.open(scope, session,
                    streamEnvelope("opaqueHttpStreamOpen", "first-open", """
                            {"requestId":"reusable-request","method":"GET",
                             "url":"https://app.vatori.invalid/","headers":{},
                             "contentLength":0}
                            """), "first-open", "first-open");
            exchanges.finishRequest(scope,
                    streamEnvelope("opaqueHttpStreamEnd", "first-end", """
                            {"requestId":"reusable-request","chunkCount":0,
                             "bodyBytes":0}
                            """));
            exchanges.grantResponseCredits(scope,
                    streamEnvelope("opaqueHttpStreamResponseCredit",
                            "first-response-credit", """
                                    {"requestId":"reusable-request",
                                     "credit":16}
                                    """));

            boolean reopenedAfterCompletion = false;
            for (int attempt = 0; attempt < 200; attempt++) {
                if (!captured.toString(StandardCharsets.UTF_8).contains(
                        "\"type\":\"opaqueHttpStreamResponseEnd\"")) {
                    Thread.sleep(5);
                    continue;
                }
                try {
                    exchanges.open(scope, session,
                            streamEnvelope("opaqueHttpStreamOpen",
                                    "second-open", """
                                            {"requestId":"reusable-request",
                                             "method":"GET",
                                             "url":"https://app.vatori.invalid/",
                                             "headers":{},"contentLength":0}
                                            """),
                            "second-open", "second-open");
                    reopenedAfterCompletion = true;
                    break;
                } catch (IllegalArgumentException error) {
                    if (!error.getMessage().contains(
                            "Duplicate opaque HTTP request id")) {
                        throw error;
                    }
                    Thread.sleep(5);
                }
            }
            if (!reopenedAfterCompletion) {
                throw new IllegalStateException(
                        "Completed opaque exchange was not reclaimed");
            }

            exchanges.cancel(scope,
                    streamEnvelope("opaqueHttpStreamCancel", "second-cancel",
                            "{\"requestId\":\"reusable-request\"}"));
            try {
                exchanges.acceptChunk(scope,
                        streamEnvelope("opaqueHttpStreamChunk", "late-chunk",
                                """
                                        {"requestId":"reusable-request",
                                         "chunkSequence":0,"bodyBase64":"YQ==",
                                         "bodyBytes":1}
                                        """));
                throw new IllegalStateException(
                        "Cancelled opaque exchange accepted a late request frame");
            } catch (IllegalArgumentException expected) {
                if (!expected.getMessage().contains(
                        "Unknown opaque HTTP request id " + requestId)) {
                    throw expected;
                }
            }

            exchanges.open(scope, session,
                    streamEnvelope("opaqueHttpStreamOpen", "third-open", """
                            {"requestId":"reusable-request","method":"GET",
                             "url":"https://app.vatori.invalid/","headers":{},
                             "contentLength":0}
                            """), "third-open", "third-open");
            exchanges.cancel(scope,
                    streamEnvelope("opaqueHttpStreamCancel", "third-cancel",
                            "{\"requestId\":\"reusable-request\"}"));
        } finally {
            session.close();
        }
    }

    private static void verifyEarlyResponseWaitsForRequestEnd() throws Exception {
        for (int length : new int[]{0, 6, -1}) {
            ByteArrayOutputStream captured = new ByteArrayOutputStream();
            FlowIpcScope scope = new FlowIpcScope("main", "early-response-" + length, "main");
            SidecarOutput output = new SidecarOutput(new PrintStream(captured, true,
                    StandardCharsets.UTF_8), "", scope);
            DesktopFlowOpaqueHttpSession session = DesktopFlowOpaqueHttpSession.create(
                    scope.sessionId(), "early-response", "", ScopeSmokeView.class);
            CountDownLatch handled = new CountDownLatch(1);
            VaadinSession state = vaadinSession(session);
            state.lock();
            try {
                state.addRequestHandler((ignored, request, response) -> {
                    response.setStatus(204);
                    handled.countDown();
                    return true;
                });
            } finally { state.unlock(); }
            try (StandardFlowIpcExchanges exchanges = new StandardFlowIpcExchanges(output)) {
                exchanges.open(scope, session, streamEnvelope("opaqueHttpStreamOpen", "early",
                        "{\"requestId\":\"early\",\"method\":\"POST\",\"url\":\"https://app.vatori.invalid/early\",\"headers\":{},\"contentLength\":" + length + "}"), "early", "early");
                if (!handled.await(2, TimeUnit.SECONDS)) throw new AssertionError("handler not reached");
                Thread.sleep(50);
                if (captured.toString(StandardCharsets.UTF_8).contains("\"type\":\"opaqueHttpStreamResponseEnd\""))
                    throw new AssertionError("response completed before request terminator");
                if (length != 0) exchanges.acceptChunk(scope, streamEnvelope("opaqueHttpStreamChunk", "chunk",
                        "{\"requestId\":\"early\",\"chunkSequence\":0,\"bodyBase64\":\"YWJjZGVm\"}"));
                exchanges.finishRequest(scope, streamEnvelope("opaqueHttpStreamEnd", "end",
                        "{\"requestId\":\"early\",\"chunkCount\":" + (length == 0 ? 0 : 1) + ",\"bodyBytes\":" + (length == 0 ? 0 : 6) + "}"));
                awaitOutput(captured, "\"type\":\"opaqueHttpStreamResponseEnd\"");
            } finally { session.close(); }
        }
    }

    private static void verifyOpaqueSessionRegistry() {
        DesktopFlowOpaqueHttpSessionRegistry registry =
                DesktopFlowOpaqueHttpSessionRegistry.create(
                        "scope-smoke", "", ScopeSmokeView.class, ignored -> {
                        });
        try {
            DesktopFlowOpaqueHttpSession main = registry.session("flow-ipc-session");
            DesktopFlowOpaqueHttpSession secondary =
                    registry.session("flow-ipc-session-secondary");
            if (main == secondary
                    || !main.sessionId().equals("flow-ipc-session")
                    || !secondary.sessionId().equals("flow-ipc-session-secondary")
                    || registry.size() != 2) {
                throw new IllegalStateException("Opaque session registry did not isolate host scopes");
            }
            DesktopFlowHttpResponse bootstrap = secondary.handle(
                    "GET",
                    "https://app.vatori.invalid/?v-r=init&location=&query="
                            + "&v-browserDetails=%7B%22v-wn%22%3A%22scope-close-smoke%22%7D",
                    Map.of(),
                    new byte[0]);
            if (bootstrap.statusCode() != 200) {
                throw new IllegalStateException(
                        "Opaque session lifecycle bootstrap failed: "
                                + bootstrap.statusCode());
            }
            VaadinSession vaadinSession = vaadinSession(secondary);
            AtomicBoolean sessionDestroyed = new AtomicBoolean();
            AtomicBoolean uiDetached = new AtomicBoolean();
            vaadinSession.lock();
            try {
                vaadinSession.addSessionDestroyListener(
                        ignored -> sessionDestroyed.set(true));
                if (vaadinSession.getUIs().isEmpty()) {
                    throw new IllegalStateException(
                            "Opaque session bootstrap did not create a Flow UI");
                }
                vaadinSession.getUIs().forEach(
                        ui -> ui.addDetachListener(
                                ignored -> uiDetached.set(true)));
            } finally {
                vaadinSession.unlock();
            }
            if (registry.closeSession(
                    "flow-ipc-session-secondary", main)
                    || registry.size() != 2) {
                throw new IllegalStateException(
                        "Expected-session rollback removed another registry owner");
            }
            if (!registry.closeSession("flow-ipc-session-secondary")
                    || registry.size() != 1
                    || registry.session("flow-ipc-session") != main) {
                throw new IllegalStateException(
                        "Closing one opaque session affected another host scope");
            }
            vaadinSession.lock();
            try {
                if (vaadinSession.getState() != VaadinSessionState.CLOSED
                        || !vaadinSession.getUIs().isEmpty()) {
                    throw new IllegalStateException(
                            "Closed opaque session retained its Flow lifecycle state");
                }
            } finally {
                vaadinSession.unlock();
            }
            if (!sessionDestroyed.get() || !uiDetached.get()) {
                throw new IllegalStateException(
                        "Opaque session close did not synchronously destroy its Flow UI");
            }
            secondary.close();
        } finally {
            registry.close();
        }
        registry.close();
        boolean rejected = false;
        try {
            registry.session("after-close");
        } catch (IllegalStateException expected) {
            rejected = true;
        }
        if (!rejected) {
            throw new IllegalStateException("Closed opaque session registry accepted a session");
        }
    }

    private static void verifyPushDeliveryAcknowledgement() {
        FlowIpcScope scope = new FlowIpcScope(
                "main", "flow-ipc-session", "main");
        List<StandardFlowIpcPushDelivery.Delivery> deliveries =
                new ArrayList<>();
        StandardFlowIpcPushDelivery tracker =
                new StandardFlowIpcPushDelivery(deliveries::add, false);
        try {
            tracker.registerScope(scope);
            tracker.connect(scope, "opaque-push-1", 4);
            tracker.send(scope, new DesktopFlowPushMessage(
                    scope.sessionId(),
                    "opaque-push-1",
                    7,
                    5,
                    "{\"syncId\":5}",
                    false));
            tracker.retryUnacknowledged();
            if (deliveries.size() != 2
                    || deliveries.get(0).pushSequence() != 1
                    || deliveries.get(1).pushSequence() != 1
                    || deliveries.get(0).deliveryAttempt() != 1
                    || deliveries.get(1).deliveryAttempt() != 2
                    || deliveries.get(0).flowServerSyncId() != 5
                    || !deliveries.get(0).uidl()
                    .equals(deliveries.get(1).uidl())) {
                throw new IllegalStateException(
                        "Unacknowledged opaque push was not retried identically");
            }
            if (tracker.acknowledge(scope, "opaque-push-1", 1, 4)) {
                throw new IllegalStateException(
                        "Opaque push accepted an acknowledgement before Flow saw it");
            }
            tracker.retryUnacknowledged();
            if (deliveries.size() != 3
                    || deliveries.get(2).deliveryAttempt() != 3) {
                throw new IllegalStateException(
                        "Unconfirmed opaque push stopped retrying");
            }
            if (!tracker.acknowledge(scope, "opaque-push-1", 1, 5)) {
                throw new IllegalStateException(
                        "Opaque push acknowledgement did not clear delivery");
            }
            tracker.retryUnacknowledged();
            if (deliveries.size() != 3) {
                throw new IllegalStateException(
                        "Acknowledged opaque push was delivered again");
            }

            tracker.send(scope, new DesktopFlowPushMessage(
                    scope.sessionId(),
                    "opaque-push-1",
                    7,
                    6,
                    "{\"syncId\":6}",
                    false));
            tracker.send(scope, new DesktopFlowPushMessage(
                    scope.sessionId(),
                    "opaque-push-1",
                    7,
                    7,
                    "{\"syncId\":7,\"resynchronize\":true}",
                    true));
            tracker.retryUnacknowledged();
            StandardFlowIpcPushDelivery.Delivery retry =
                    deliveries.get(deliveries.size() - 1);
            if (!retry.resync()
                    || retry.flowServerSyncId() != 7
                    || retry.deliveryAttempt() != 2) {
                throw new IllegalStateException(
                        "Resync did not supersede older pending opaque pushes");
            }
            tracker.disconnect(scope, "opaque-push-1");
            int disconnectedDeliveryCount = deliveries.size();
            tracker.retryUnacknowledged();
            if (deliveries.size() != disconnectedDeliveryCount) {
                throw new IllegalStateException(
                        "Disconnected opaque push continued retrying");
            }
            tracker.connect(scope, "opaque-push-1", 7);
            tracker.retryUnacknowledged();
            if (deliveries.size() != disconnectedDeliveryCount) {
                throw new IllegalStateException(
                        "Reconnect last-seen metadata did not confirm pending push");
            }
        } finally {
            tracker.close();
        }
    }

    private static void verifyWindowCloseScopeBeforeCancellation()
            throws Exception {
        FlowIpcScope owner = new FlowIpcScope("main", "flow-ipc-session", "main");
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        SidecarOutput output = new SidecarOutput(
                new PrintStream(captured, true, StandardCharsets.UTF_8),
                "", owner);
        SidecarNativeBridge nativeBridge = new SidecarNativeBridge(output, owner);
        StandardFlowIpcSessions sessions = new StandardFlowIpcSessions(
                List.of(ScopeSmokeView.class), null, output, nativeBridge,
                true, null);
        AtomicBoolean cancellationAttempted = new AtomicBoolean();
        try {
            sessions.session(owner);
            try {
                sessions.closeWindow(
                        new FlowIpcScope("main", "forged-session", "main"),
                        () -> cancellationAttempted.set(true));
                throw new IllegalStateException(
                        "Window close accepted a mismatched Flow IPC scope");
            } catch (SecurityException expected) {
                // The session owner must be checked before exchange cancellation.
            }
            if (cancellationAttempted.get()) {
                throw new IllegalStateException(
                        "Window close cancelled exchanges before validating its scope");
            }
            FlowIpcScope unopened = new FlowIpcScope(
                    "unopened", "flow-ipc-window:unopened", "unopened");
            nativeBridge.closeScope(unopened);
            sessions.closeWindow(unopened, () -> cancellationAttempted.set(true));
            nativeBridge.registerScope(new FlowIpcScope(
                    "replacement",
                    "flow-ipc-window:unopened",
                    "replacement"));
            if (cancellationAttempted.get()) {
                throw new IllegalStateException(
                        "Unopened window close cancelled nonexistent exchanges");
            }

            FlowIpcScope secondary = new FlowIpcScope(
                    "secondary", "flow-ipc-window:secondary", "secondary");
            FlowIpcScope replacement = new FlowIpcScope(
                    "replacement-secondary",
                    secondary.sessionId(),
                    "replacement-secondary");
            DesktopFlowOpaqueHttpSession secondarySession =
                    sessions.session(secondary);
            VaadinSession closedVaadinSession =
                    vaadinSession(secondarySession);
            nativeBridge.closeScope(secondary);
            AtomicBoolean replacementRejectedDuringTeardown =
                    new AtomicBoolean();
            sessions.closeWindow(secondary, () -> {
                try {
                    nativeBridge.registerScope(replacement);
                    throw new IllegalStateException(
                            "Closing scope registration was released before session teardown");
                } catch (SecurityException expected) {
                    replacementRejectedDuringTeardown.set(true);
                }
            });
            if (!replacementRejectedDuringTeardown.get()) {
                throw new IllegalStateException(
                        "Closing scope did not remain registered through session teardown");
            }
            nativeBridge.registerScope(replacement);
            nativeBridge.unregisterScope(replacement);

            int outputBytesBeforeClosedSessionInvoke = captured.size();
            AtomicReference<DesktopCommandResult> closedSessionResult =
                    new AtomicReference<>();
            Thread closedSessionCaller = Thread.ofVirtual().start(() -> {
                VaadinSession.setCurrent(closedVaadinSession);
                try {
                    closedSessionResult.set(nativeBridge.invoke(
                            new DesktopCommand(
                                    "clipboard.readText", Map.of())));
                } finally {
                    VaadinSession.setCurrent(null);
                }
            });
            closedSessionCaller.join(1000);
            boolean closedSessionInvokeHung = closedSessionCaller.isAlive();
            if (closedSessionInvokeHung) {
                nativeBridge.close();
                closedSessionCaller.join(1000);
            }
            DesktopCommandResult rejected = closedSessionResult.get();
            if (closedSessionInvokeHung
                    || rejected == null
                    || rejected.success()
                    || !"Closed Vaadin session has no active Flow IPC scope"
                            .equals(rejected.error())
                    || captured.size()
                            != outputBytesBeforeClosedSessionInvoke) {
                throw new IllegalStateException(
                        "Closed Vaadin session fell back to the default native scope");
            }
        } finally {
            sessions.close();
            nativeBridge.close();
        }
    }

    private static void verifyWindowCloseFollowsQueuedRequests() {
        String input = SidecarWire.PREFIX
                + envelope("queuedRequest", "queued", "flow-ipc-session", "main") + "\n"
                + SidecarWire.PREFIX
                + envelope("windowClosed", "closed", "flow-ipc-session", "main") + "\n";
        FlowIpcScope scope = new FlowIpcScope("main", "flow-ipc-session", "main");
        SidecarOutput output = new SidecarOutput(
                new PrintStream(new ByteArrayOutputStream()), "", scope);
        SidecarNativeBridge nativeBridge = new SidecarNativeBridge(output, scope);
        SidecarMessageLoop messageLoop = new SidecarMessageLoop(
                new BufferedReader(new java.io.StringReader(input)),
                output,
                nativeBridge,
                scope);
        AtomicInteger stage = new AtomicInteger();
        messageLoop.register("queuedRequest", (ignoredScope, envelope, id, correlationId) -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Queued request was interrupted", error);
            }
            stage.compareAndSet(0, 1);
        });
        messageLoop.onWindowClosed(ignored -> {
            if (!stage.compareAndSet(1, 2)) {
                throw new IllegalStateException(
                        "Window close overtook an earlier sidecar request");
            }
            messageLoop.requestShutdown();
        });
        messageLoop.run();
        if (stage.get() != 2) {
            throw new IllegalStateException(
                    "Window close did not run after the queued sidecar request");
        }
    }

    private static void verifyDifferentWindowHandlersCanProgressIndependently() {
        String input = SidecarWire.PREFIX
                + envelope(
                        "blockingRequest",
                        "secondary-blocking",
                        "flow-ipc-session-secondary",
                        "secondary") + "\n"
                + SidecarWire.PREFIX
                + envelope(
                        "independentRequest",
                        "main-independent",
                        "flow-ipc-session",
                        "main") + "\n"
                + SidecarWire.PREFIX
                + envelope(
                        "shutdown",
                        "independent-shutdown",
                        "flow-ipc-session",
                        "main") + "\n";
        FlowIpcScope scope = new FlowIpcScope(
                "main", "flow-ipc-session", "main");
        SidecarOutput output = new SidecarOutput(
                new PrintStream(new ByteArrayOutputStream()), "", scope);
        SidecarNativeBridge nativeBridge = new SidecarNativeBridge(output, scope);
        SidecarMessageLoop messageLoop = new SidecarMessageLoop(
                new BufferedReader(new java.io.StringReader(input)),
                output,
                nativeBridge,
                scope);
        CountDownLatch secondaryStarted = new CountDownLatch(1);
        CountDownLatch mainProgressed = new CountDownLatch(1);
        AtomicBoolean secondaryTimedOut = new AtomicBoolean();
        messageLoop.register(
                "blockingRequest",
                (ignoredScope, envelope, id, correlationId) -> {
                    secondaryStarted.countDown();
                    try {
                        if (!mainProgressed.await(2, TimeUnit.SECONDS)) {
                            secondaryTimedOut.set(true);
                        }
                    } catch (InterruptedException error) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(
                                "Independent secondary handler was interrupted",
                                error);
                    }
                });
        messageLoop.register(
                "independentRequest",
                (ignoredScope, envelope, id, correlationId) -> {
                    try {
                        if (!secondaryStarted.await(1, TimeUnit.SECONDS)) {
                            throw new IllegalStateException(
                                    "Main handler overtook secondary dispatch");
                        }
                    } catch (InterruptedException error) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(
                                "Independent main handler was interrupted",
                                error);
                    }
                    mainProgressed.countDown();
                });
        messageLoop.run();
        if (secondaryTimedOut.get() || mainProgressed.getCount() != 0) {
            throw new IllegalStateException(
                    "One blocked window prevented another window from progressing");
        }
    }

    private static void verifyReopenedWindowWaitsForPriorCloseTeardown() {
        String input = SidecarWire.PREFIX + """
                {"version":1,"type":"windowClosed","id":"old-close",
                 "correlationId":"old-close","sessionId":"old-session",
                 "uiId":"secondary","windowId":"secondary","payload":{}}
                """.replace("\n", "") + "\n"
                + SidecarWire.PREFIX + """
                {"version":1,"type":"reopenedRequest","id":"new-request",
                 "correlationId":"new-request","sessionId":"new-session",
                 "uiId":"secondary","windowId":"secondary","payload":{}}
                """.replace("\n", "") + "\n"
                + SidecarWire.PREFIX
                + envelope(
                        "shutdown",
                        "handoff-shutdown",
                        "flow-ipc-session",
                        "main") + "\n";
        FlowIpcScope scope = new FlowIpcScope(
                "main", "flow-ipc-session", "main");
        SidecarOutput output = new SidecarOutput(
                new PrintStream(new ByteArrayOutputStream()), "", scope);
        SidecarNativeBridge nativeBridge = new SidecarNativeBridge(output, scope);
        SidecarMessageLoop messageLoop = new SidecarMessageLoop(
                new BufferedReader(new java.io.StringReader(input)),
                output,
                nativeBridge,
                scope);
        AtomicInteger stage = new AtomicInteger();
        messageLoop.onWindowClosed(ignored -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                        "Prior window close teardown was interrupted", error);
            }
            stage.compareAndSet(0, 1);
        });
        messageLoop.register(
                "reopenedRequest",
                (ignoredScope, envelope, id, correlationId) -> {
                    if (!stage.compareAndSet(1, 2)) {
                        throw new IllegalStateException(
                                "Reopened window overtook prior close teardown");
                    }
                });
        messageLoop.run();
        if (stage.get() != 2) {
            throw new IllegalStateException(
                    "Reopened window did not follow prior close teardown");
        }
    }

    private static void verifyRetiredScopeFramesAreFencedAcrossReopen()
            throws Exception {
        FlowIpcScope firstRetiredScope = new FlowIpcScope(
                "secondary", "flow-ipc-window:incarnation-a", "secondary");
        FlowIpcScope secondRetiredScope = new FlowIpcScope(
                "secondary", "flow-ipc-window:incarnation-b", "secondary");
        FlowIpcScope currentScope = new FlowIpcScope(
                "secondary", "flow-ipc-window:incarnation-c", "secondary");
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        SidecarOutput output = new SidecarOutput(
                new PrintStream(captured, true, StandardCharsets.UTF_8),
                "",
                currentScope);
        SidecarNativeBridge nativeBridge = new SidecarNativeBridge(
                output, currentScope);
        nativeBridge.registerScope(firstRetiredScope);
        PipedWriter inputWriter = new PipedWriter();
        SidecarMessageLoop messageLoop = new SidecarMessageLoop(
                new BufferedReader(new PipedReader(inputWriter)),
                output,
                nativeBridge,
                currentScope);
        AtomicInteger stage = new AtomicInteger();
        AtomicInteger admittedProbeCount = new AtomicInteger();
        AtomicInteger lateRetiredProbeCount = new AtomicInteger();
        AtomicInteger closeCount = new AtomicInteger();
        AtomicInteger currentHealthCount = new AtomicInteger();
        AtomicReference<DesktopCommandResult> currentResult =
                new AtomicReference<>();
        messageLoop.register(
                "preCloseProbe",
                (scope, envelope, id, correlationId) -> {
                    admittedProbeCount.incrementAndGet();
                    boolean first = firstRetiredScope.equals(scope)
                            && stage.compareAndSet(0, 1);
                    boolean second = secondRetiredScope.equals(scope)
                            && stage.compareAndSet(3, 4);
                    if (!first && !second) {
                        throw new IllegalStateException(
                                "Incarnation pre-close work lost ordering");
                    }
                });
        messageLoop.register(
                "retiredProbe",
                (scope, envelope, id, correlationId) ->
                        lateRetiredProbeCount.incrementAndGet());
        messageLoop.register(
                "secondIncarnationCreate",
                (scope, envelope, id, correlationId) -> {
                    if (!secondRetiredScope.equals(scope)
                            || !stage.compareAndSet(2, 3)) {
                        throw new IllegalStateException(
                                "Second incarnation overtook first teardown");
                    }
                    nativeBridge.registerScope(secondRetiredScope);
                });
        messageLoop.onWindowClosed(scope -> {
            closeCount.incrementAndGet();
            try {
                Thread.sleep(50);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                        "Retired-scope teardown was interrupted", error);
            }
            boolean first = firstRetiredScope.equals(scope)
                    && stage.compareAndSet(1, 2);
            boolean second = secondRetiredScope.equals(scope)
                    && stage.compareAndSet(4, 5);
            if (!first && !second) {
                throw new IllegalStateException(
                        "Retired-scope close overtook admitted work");
            }
            nativeBridge.unregisterScope(scope);
        });
        messageLoop.register(
                "currentCapability",
                (scope, envelope, id, correlationId) -> {
                    if (!currentScope.equals(scope)
                            || !stage.compareAndSet(5, 6)) {
                        throw new IllegalStateException(
                                "Third incarnation overtook second teardown");
                    }
                    DesktopCommandResult result = nativeBridge.invoke(
                            new DesktopCommand(
                                    "clipboard.readText", Map.of()));
                    currentResult.set(result);
                    if (!stage.compareAndSet(6, 7)) {
                        throw new IllegalStateException(
                                "Third-incarnation capability completion lost ordering");
                    }
                });
        messageLoop.register(
                "currentHealth",
                (scope, envelope, id, correlationId) -> {
                    DesktopCommandResult result = currentResult.get();
                    if (!currentScope.equals(scope)
                            || result == null
                            || !result.success()
                            || !"fresh-result".equals(
                                    result.payload().get("text"))
                            || !stage.compareAndSet(7, 8)) {
                        throw new IllegalStateException(
                                "Third incarnation was poisoned by retired traffic");
                    }
                    assertClosedScopeCannotRegister(
                            nativeBridge, firstRetiredScope);
                    assertClosedScopeCannotRegister(
                            nativeBridge, secondRetiredScope);
                    currentHealthCount.incrementAndGet();
                });

        Thread loop = Thread.ofVirtual().start(messageLoop::run);
        try {
            inputWriter.write(SidecarWire.PREFIX
                    + envelope(
                            "preCloseProbe",
                            "incarnation-a-before-close",
                            firstRetiredScope.sessionId(),
                            firstRetiredScope.uiId()) + "\n");
            inputWriter.write(SidecarWire.PREFIX
                    + envelope(
                            "windowClosed",
                            "incarnation-a-close",
                            firstRetiredScope.sessionId(),
                            firstRetiredScope.uiId()) + "\n");
            inputWriter.write(SidecarWire.PREFIX
                    + envelope(
                            "retiredProbe",
                            "incarnation-a-late-registered",
                            firstRetiredScope.sessionId(),
                            firstRetiredScope.uiId()) + "\n");
            inputWriter.write(SidecarWire.PREFIX
                    + envelope(
                            "futureRetiredTransport",
                            "incarnation-a-future-type",
                            firstRetiredScope.sessionId(),
                            firstRetiredScope.uiId()) + "\n");
            inputWriter.write(SidecarWire.PREFIX
                    + envelope(
                            "windowClosed",
                            "incarnation-a-duplicate-close",
                            firstRetiredScope.sessionId(),
                            firstRetiredScope.uiId()) + "\n");
            inputWriter.write(SidecarWire.PREFIX
                    + envelope(
                            "secondIncarnationCreate",
                            "incarnation-b-create",
                            secondRetiredScope.sessionId(),
                            secondRetiredScope.uiId()) + "\n");
            inputWriter.flush();
            long secondDeadline = System.nanoTime()
                    + TimeUnit.SECONDS.toNanos(2);
            while (stage.get() < 3 && System.nanoTime() < secondDeadline) {
                Thread.onSpinWait();
            }
            if (stage.get() != 3) {
                throw new IllegalStateException(
                        "Second incarnation did not follow first teardown");
            }

            inputWriter.write(SidecarWire.PREFIX
                    + envelope(
                            "preCloseProbe",
                            "incarnation-b-before-close",
                            secondRetiredScope.sessionId(),
                            secondRetiredScope.uiId()) + "\n");
            inputWriter.write(SidecarWire.PREFIX
                    + envelope(
                            "windowClosed",
                            "incarnation-b-close",
                            secondRetiredScope.sessionId(),
                            secondRetiredScope.uiId()) + "\n");
            inputWriter.write(SidecarWire.PREFIX
                    + envelope(
                            "retiredProbe",
                            "incarnation-b-late-registered",
                            secondRetiredScope.sessionId(),
                            secondRetiredScope.uiId()) + "\n");
            inputWriter.write(SidecarWire.PREFIX
                    + envelope(
                            "futureRetiredTransport",
                            "incarnation-b-future-type",
                            secondRetiredScope.sessionId(),
                            secondRetiredScope.uiId()) + "\n");
            inputWriter.write(SidecarWire.PREFIX
                    + envelope(
                            "windowClosed",
                            "incarnation-b-duplicate-close",
                            secondRetiredScope.sessionId(),
                            secondRetiredScope.uiId()) + "\n");
            inputWriter.write(SidecarWire.PREFIX
                    + envelope(
                            "currentCapability",
                            "incarnation-c-capability",
                            currentScope.sessionId(),
                            currentScope.uiId()) + "\n");
            inputWriter.flush();
            awaitOutput(captured, "desktop-command-1");

            inputWriter.write(SidecarWire.PREFIX + """
                    {"version":1,"type":"capabilityResult",
                     "id":"incarnation-a-forged-result",
                     "correlationId":"desktop-command-1",
                     "sessionId":"flow-ipc-window:incarnation-a",
                     "uiId":"secondary","windowId":"secondary",
                     "payload":{"success":true,"text":"retired-a-result"}}
                    """.replace("\n", "") + "\n");
            inputWriter.write(SidecarWire.PREFIX + """
                    {"version":1,"type":"capabilityResult",
                     "id":"incarnation-b-forged-result",
                     "correlationId":"desktop-command-1",
                     "sessionId":"flow-ipc-window:incarnation-b",
                     "uiId":"secondary","windowId":"secondary",
                     "payload":{"success":true,"text":"retired-b-result"}}
                    """.replace("\n", "") + "\n");
            inputWriter.write(SidecarWire.PREFIX + """
                    {"version":1,"type":"capabilityResult",
                     "id":"incarnation-c-real-result",
                     "correlationId":"desktop-command-1",
                     "sessionId":"flow-ipc-window:incarnation-c",
                     "uiId":"secondary","windowId":"secondary",
                     "payload":{"success":true,"text":"fresh-result"}}
                    """.replace("\n", "") + "\n");
            inputWriter.write(SidecarWire.PREFIX
                    + envelope(
                            "currentHealth",
                            "incarnation-c-health",
                            currentScope.sessionId(),
                            currentScope.uiId()) + "\n");
            inputWriter.write(SidecarWire.PREFIX
                    + envelope(
                            "retiredProbe",
                            "incarnation-a-late-after-c",
                            firstRetiredScope.sessionId(),
                            firstRetiredScope.uiId()) + "\n");
            inputWriter.write(SidecarWire.PREFIX
                    + envelope(
                            "retiredProbe",
                            "incarnation-b-late-after-c",
                            secondRetiredScope.sessionId(),
                            secondRetiredScope.uiId()) + "\n");
            inputWriter.write(SidecarWire.PREFIX
                    + envelope(
                            "shutdown",
                            "incarnation-a-retired-scope-shutdown",
                            firstRetiredScope.sessionId(),
                            firstRetiredScope.uiId()) + "\n");
            inputWriter.flush();
            loop.join(2000);

            String outputText = captured.toString(StandardCharsets.UTF_8);
            String commandMarker = "\"type\":\"command\"";
            int firstCommand = outputText.indexOf(commandMarker);
            if (loop.isAlive()
                    || stage.get() != 8
                    || admittedProbeCount.get() != 2
                    || lateRetiredProbeCount.get() != 0
                    || closeCount.get() != 2
                    || currentHealthCount.get() != 1
                    || currentResult.get() == null
                    || !currentResult.get().success()
                    || firstCommand < 0
                    || outputText.indexOf(
                            commandMarker,
                            firstCommand + commandMarker.length()) >= 0
                    || outputText.contains("\"type\":\"error\"")) {
                throw new IllegalStateException(
                        "Retired scopes crossed the repeated-reopen fence: stage="
                                + stage.get()
                                + ", admittedProbes=" + admittedProbeCount.get()
                                + ", lateRetiredProbes="
                                + lateRetiredProbeCount.get()
                                + ", closes=" + closeCount.get()
                                + ", currentHealth="
                                + currentHealthCount.get()
                                + ", result=" + currentResult.get()
                                + ", output=" + outputText);
            }
        } finally {
            messageLoop.requestShutdown();
            inputWriter.close();
            loop.join(1000);
            if (loop.isAlive()) {
                throw new IllegalStateException(
                        "Retired-scope fence smoke did not stop its message loop");
            }
        }
    }

    private static void verifyRepeatedReopenSessionOwnership() {
        FlowIpcScope defaultScope = new FlowIpcScope(
                "main", "flow-ipc-session", "main");
        FlowIpcScope firstScope = new FlowIpcScope(
                "secondary", "flow-ipc-window:ownership-a", "secondary");
        FlowIpcScope secondScope = new FlowIpcScope(
                "secondary", "flow-ipc-window:ownership-b", "secondary");
        FlowIpcScope thirdScope = new FlowIpcScope(
                "secondary", "flow-ipc-window:ownership-c", "secondary");
        SidecarOutput output = new SidecarOutput(
                new PrintStream(new ByteArrayOutputStream()), "", defaultScope);
        SidecarNativeBridge nativeBridge = new SidecarNativeBridge(
                output, defaultScope);
        StandardFlowIpcSessions sessions = new StandardFlowIpcSessions(
                List.of(ScopeSmokeView.class),
                null,
                output,
                nativeBridge,
                true,
                null);
        try {
            DesktopFlowOpaqueHttpSession first = sessions.session(firstScope);
            if (!firstScope.sessionId().equals(first.sessionId())) {
                throw new IllegalStateException(
                        "First incarnation acquired the wrong session");
            }
            nativeBridge.closeScope(firstScope);
            sessions.closeWindow(firstScope, () -> {
            });
            DesktopFlowOpaqueHttpSession second = sessions.session(secondScope);
            if (!secondScope.sessionId().equals(second.sessionId())) {
                throw new IllegalStateException(
                        "Second incarnation acquired the wrong session");
            }
            nativeBridge.closeScope(secondScope);
            sessions.closeWindow(secondScope, () -> {
            });
            DesktopFlowOpaqueHttpSession third = sessions.session(thirdScope);

            DesktopFlowOpaqueHttpSessionRegistry registry =
                    sessionRegistry(sessions);
            if (!thirdScope.sessionId().equals(third.sessionId())
                    || vaadinSession(first).getState()
                            != VaadinSessionState.CLOSED
                    || vaadinSession(second).getState()
                            != VaadinSessionState.CLOSED
                    || registry.size() != 1
                    || vaadinSession(third).getState()
                            != VaadinSessionState.OPEN) {
                throw new IllegalStateException(
                        "Repeated reopen leaked a retired session owner");
            }
            assertClosedScopeCannotRegister(nativeBridge, firstScope);
            assertClosedScopeCannotRegister(nativeBridge, secondScope);
        } finally {
            sessions.close();
            nativeBridge.close();
        }
    }

    private static void assertClosedScopeCannotRegister(
            SidecarNativeBridge nativeBridge, FlowIpcScope scope) {
        try {
            nativeBridge.registerScope(scope);
            throw new IllegalStateException(
                    "Retired Flow IPC scope was reactivated: " + scope);
        } catch (IllegalStateException expected) {
            if (!expected.getMessage().startsWith(
                    "Closed Flow IPC scope cannot be reactivated")) {
                throw expected;
            }
        }
    }

    private static void verifyFailedPreCloseContextRollsBackBeforeReopen()
            throws Exception {
        FlowIpcScope defaultScope = new FlowIpcScope(
                "main", "flow-ipc-session", "main");
        FlowIpcScope closingScope = new FlowIpcScope(
                "secondary", "flow-ipc-window:rollback-a", "secondary");
        FlowIpcScope reopenedScope = new FlowIpcScope(
                "secondary", "flow-ipc-window:rollback-b", "secondary");
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        SidecarOutput output = new SidecarOutput(
                new PrintStream(captured, true, StandardCharsets.UTF_8),
                "",
                defaultScope);
        SidecarNativeBridge nativeBridge = new SidecarNativeBridge(
                output, defaultScope);
        StandardFlowIpcSessions sessions = new StandardFlowIpcSessions(
                List.of(ScopeSmokeView.class),
                null,
                output,
                nativeBridge,
                true,
                null);
        PipedWriter inputWriter = new PipedWriter();
        SidecarMessageLoop messageLoop = new SidecarMessageLoop(
                new BufferedReader(new PipedReader(inputWriter)),
                output,
                nativeBridge,
                defaultScope);
        CountDownLatch closingHandlerStarted = new CountDownLatch(1);
        CountDownLatch releaseClosingHandler = new CountDownLatch(1);
        CountDownLatch reopenedHandlerFinished = new CountDownLatch(1);
        AtomicReference<Throwable> closingFailure = new AtomicReference<>();
        AtomicReference<Throwable> reopenedFailure = new AtomicReference<>();
        AtomicReference<DesktopFlowOpaqueHttpSession> reopenedSession =
                new AtomicReference<>();
        AtomicInteger closeCount = new AtomicInteger();
        AtomicInteger releaseCount = new AtomicInteger();
        messageLoop.register(
                "delayedSessionCreate",
                (scope, envelope, id, correlationId) -> {
                    closingHandlerStarted.countDown();
                    try {
                        if (!releaseClosingHandler.await(
                                2, TimeUnit.SECONDS)) {
                            throw new IllegalStateException(
                                    "Independent lane did not release delayed context");
                        }
                        sessions.session(scope);
                        closingFailure.set(new IllegalStateException(
                                "Retired scope created a session after its tombstone"));
                    } catch (IllegalStateException expected) {
                        if (!expected.getMessage().startsWith(
                                "Closed Flow IPC scope cannot be reactivated")
                                || expected.getSuppressed().length != 0) {
                            closingFailure.set(expected);
                        }
                    } catch (InterruptedException error) {
                        Thread.currentThread().interrupt();
                        closingFailure.set(error);
                    }
                });
        messageLoop.register(
                "releaseDelayedContext",
                (scope, envelope, id, correlationId) -> {
                    if (!defaultScope.equals(scope)) {
                        throw new IllegalStateException(
                                "Context release did not use the independent main lane");
                    }
                    releaseCount.incrementAndGet();
                    releaseClosingHandler.countDown();
                });
        messageLoop.onWindowClosed(scope -> {
            if (!closingScope.equals(scope)) {
                throw new IllegalStateException(
                        "Context rollback closed an unexpected scope");
            }
            sessions.closeWindow(scope, () -> {
            });
            closeCount.incrementAndGet();
        });
        messageLoop.register(
                "reopenedSessionCreate",
                (scope, envelope, id, correlationId) -> {
                    try {
                        if (!reopenedScope.equals(scope)) {
                            throw new IllegalStateException(
                                    "Reopen request used an unexpected scope");
                        }
                        reopenedSession.set(sessions.session(scope));
                    } catch (RuntimeException | Error failure) {
                        reopenedFailure.set(failure);
                    } finally {
                        reopenedHandlerFinished.countDown();
                    }
                });

        Thread loop = Thread.ofVirtual().start(messageLoop::run);
        try {
            inputWriter.write(SidecarWire.PREFIX
                    + envelope(
                            "delayedSessionCreate",
                            "rollback-a-create",
                            closingScope.sessionId(),
                            closingScope.uiId()) + "\n");
            inputWriter.flush();
            if (!closingHandlerStarted.await(1, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "Delayed context handler did not start");
            }
            inputWriter.write(SidecarWire.PREFIX
                    + envelope(
                            "windowClosed",
                            "rollback-a-close",
                            closingScope.sessionId(),
                            closingScope.uiId()) + "\n");
            inputWriter.write(SidecarWire.PREFIX
                    + envelope(
                            "releaseDelayedContext",
                            "rollback-a-release",
                            defaultScope.sessionId(),
                            defaultScope.uiId()) + "\n");
            inputWriter.write(SidecarWire.PREFIX
                    + envelope(
                            "reopenedSessionCreate",
                            "rollback-b-create",
                            reopenedScope.sessionId(),
                            reopenedScope.uiId()) + "\n");
            inputWriter.flush();
            if (!reopenedHandlerFinished.await(3, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "Reopened context did not finish after rollback");
            }

            DesktopFlowOpaqueHttpSessionRegistry registry =
                    sessionRegistry(sessions);
            if (closingFailure.get() != null
                    || reopenedFailure.get() != null
                    || reopenedSession.get() == null
                    || !reopenedScope.sessionId().equals(
                            reopenedSession.get().sessionId())
                    || closeCount.get() != 1
                    || releaseCount.get() != 1
                    || registry.size() != 1
                    || captured.toString(StandardCharsets.UTF_8)
                            .contains("\"type\":\"error\"")) {
                throw new IllegalStateException(
                        "Failed context creation leaked across reopen: closingFailure="
                                + closingFailure.get()
                                + ", reopenedFailure=" + reopenedFailure.get()
                                + ", closes=" + closeCount.get()
                                + ", releases=" + releaseCount.get()
                                + ", registrySize=" + registry.size()
                                + ", output="
                                + captured.toString(StandardCharsets.UTF_8));
            }

            inputWriter.write(SidecarWire.PREFIX
                    + envelope(
                            "shutdown",
                            "rollback-shutdown",
                            defaultScope.sessionId(),
                            defaultScope.uiId()) + "\n");
            inputWriter.flush();
            loop.join(2000);
            if (loop.isAlive()) {
                throw new IllegalStateException(
                        "Context rollback smoke did not honor shutdown");
            }
        } finally {
            releaseClosingHandler.countDown();
            messageLoop.requestShutdown();
            inputWriter.close();
            loop.join(1000);
            sessions.close();
            nativeBridge.close();
            if (loop.isAlive()) {
                throw new IllegalStateException(
                        "Context rollback smoke did not stop its message loop");
            }
        }
    }

    private static final class NoopResponseSender
            implements OpaqueHttpResponseOutputStream.ResponseSender {
        @Override
        public void start(DesktopFlowHttpResponseHead head) {
        }

        @Override
        public void chunk(int sequence, byte[] bytes) {
        }

        @Override
        public void end(int chunkCount, long byteCount) {
        }
    }

    private static String envelope(String type, String id, String sessionId, String uiId) {
        return """
                {"version":1,"type":"%s","id":"%s","correlationId":"%s","sessionId":"%s","uiId":"%s","route":"","payload":{},"error":null,"timestamp":0}
                """.formatted(type, id, id, sessionId, uiId).trim();
    }

    private static SidecarInboundEnvelope streamEnvelope(
            String type, String id, String payload) {
        return SidecarInboundEnvelope.parse("""
                {"version":1,"type":"%s","id":"%s",
                 "correlationId":"%s","sessionId":"flow-ipc-registry-session",
                 "uiId":"main","windowId":"main","route":"",
                 "payload":%s,"error":null,"timestamp":0}
                """.formatted(type, id, id, payload).trim());
    }

    private static VaadinSession vaadinSession(
            DesktopFlowOpaqueHttpSession opaqueSession) {
        try {
            Field field = DesktopFlowOpaqueHttpSession.class
                    .getDeclaredField("session");
            field.setAccessible(true);
            return (VaadinSession) field.get(opaqueSession);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(
                    "Unable to inspect opaque Vaadin session", error);
        }
    }

    private static DesktopFlowOpaqueHttpSessionRegistry sessionRegistry(
            StandardFlowIpcSessions sessions) {
        try {
            Field field = StandardFlowIpcSessions.class
                    .getDeclaredField("sessionRegistry");
            field.setAccessible(true);
            return (DesktopFlowOpaqueHttpSessionRegistry) field.get(sessions);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(
                    "Unable to inspect Flow IPC session registry", error);
        }
    }

    @Route("")
    public static final class ScopeSmokeView extends VerticalLayout {
        public ScopeSmokeView() {
            setId("flow-ipc-scope-smoke");
            add(new Button("Scope smoke"));
        }
    }
}
