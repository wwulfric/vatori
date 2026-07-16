package dev.vatori.desktop.flow;

import dev.vatori.desktop.DesktopApplication;
import dev.vatori.desktop.DesktopWindow;
import dev.vatori.desktop.flow.launcherfixture.LauncherContractApplication;
import dev.vatori.desktop.flow.launcherfixture.LauncherContractLifecycle;
import org.springframework.context.support.GenericApplicationContext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public final class LauncherContractSmokeMain {
    private static final String TRANSPORT_PROPERTY = "vaadin.desktop.transport";

    private LauncherContractSmokeMain() {
    }

    public static void main(String[] args) {
        verifyApplicationDescriptorValidation();
        verifyRejectedTransport(null, "transport is missing");
        verifyRejectedTransport("browser", "Unknown desktop sidecar transport");
        String flowOutput = launchUntilInput("flow-ipc", new byte[0]);
        assertReady(flowOutput, "flow-ipc", false);
        String compatibilityOutput = launchCompatibilityUntilShutdown(
                "VFIPC1 {\"type\":\"shutdown\",\"id\":\"contract-shutdown\",\"payload\":{}}\n"
                        .getBytes(StandardCharsets.UTF_8));
        assertReady(compatibilityOutput, "compatibility-http", true);
        if (LauncherContractLifecycle.closedContexts() != 2) {
            throw new IllegalStateException(
                    "DesktopLauncher did not close Spring contexts after EOF and shutdown");
        }
        System.out.println("Desktop launcher contract smoke passed");
    }

    private static void verifyApplicationDescriptorValidation() {
        expectInvalid(() -> DesktopApplication.builder(" "), "application name");
        expectInvalid(() -> DesktopWindow.builder(""), "window id");
        expectInvalid(() -> DesktopWindow.builder("main").title(" "), "window title");
        expectInvalid(() -> DesktopWindow.builder("main").size(0, 640), "window size");
    }

    private static void expectInvalid(Runnable action, String contract) {
        try {
            action.run();
            throw new IllegalStateException("Builder accepted invalid " + contract);
        } catch (IllegalArgumentException expected) {
            // Expected descriptor guard.
        }
    }

    private static void verifyRejectedTransport(String transport, String expectedMessage) {
        String previous = System.getProperty(TRANSPORT_PROPERTY);
        try {
            if (transport == null) {
                System.clearProperty(TRANSPORT_PROPERTY);
            } else {
                System.setProperty(TRANSPORT_PROPERTY, transport);
            }
            try {
                DesktopLauncher.launch(
                        LauncherContractApplication.class, application(), new String[0]);
                throw new IllegalStateException(
                        "DesktopLauncher accepted invalid transport " + transport);
            } catch (IllegalStateException expected) {
                if (!expected.getMessage().contains(expectedMessage)) {
                    throw expected;
                }
            }
        } finally {
            restoreProperty(previous);
        }
    }

    private static String launchUntilInput(String transport, byte[] input) {
        String previousTransport = System.getProperty(TRANSPORT_PROPERTY);
        InputStream previousIn = System.in;
        PrintStream previousOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try {
            System.setProperty(TRANSPORT_PROPERTY, transport);
            System.setIn(new ByteArrayInputStream(input));
            System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
            try {
                DesktopLauncher.launch(
                        LauncherContractApplication.class, application(), new String[0]);
            } catch (RuntimeException error) {
                previousOut.print(captured.toString(StandardCharsets.UTF_8));
                throw error;
            }
            return captured.toString(StandardCharsets.UTF_8);
        } finally {
            System.setIn(previousIn);
            System.setOut(previousOut);
            restoreProperty(previousTransport);
        }
    }

    private static String launchCompatibilityUntilShutdown(byte[] input) {
        InputStream previousIn = System.in;
        PrintStream previousOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean(LauncherContractLifecycle.class);
        context.refresh();
        try {
            System.setIn(new ByteArrayInputStream(input));
            System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
            DesktopLauncher.runCompatibilitySidecar(
                    context,
                    application(),
                    new String[0],
                    java.net.URI.create("http://127.0.0.1:45123/"));
            return captured.toString(StandardCharsets.UTF_8);
        } finally {
            System.setIn(previousIn);
            System.setOut(previousOut);
            if (context.isActive()) {
                context.close();
            }
        }
    }

    private static DesktopApplication application() {
        return DesktopApplication.builder("Launcher Contract")
                .metadata("contract", "sidecar")
                .mainWindow(DesktopWindow.builder("main")
                        .title("Launcher Contract Window")
                        .size(880, 640)
                        .build())
                .build();
    }

    private static void assertReady(
            String output,
            String transport,
            boolean expectsApplicationUrl) {
        for (String token : new String[]{
                "VFIPC1 ",
                "\"type\":\"ready\"",
                "\"transport\":\"" + transport + "\"",
                "\"name\":\"Launcher Contract\"",
                "\"metadata\":{\"contract\":\"sidecar\"}",
                "\"id\":\"main\"",
                "\"title\":\"Launcher Contract Window\"",
                "\"width\":880",
                "\"height\":640"}) {
            if (!output.contains(token)) {
                throw new IllegalStateException(
                        "Sidecar ready payload is missing " + token + ":\n" + output);
            }
        }
        if (expectsApplicationUrl != output.contains("\"applicationUrl\":\"http://127.0.0.1:")) {
            throw new IllegalStateException(
                    "Sidecar ready applicationUrl contract is invalid:\n" + output);
        }
    }

    private static void restoreProperty(String value) {
        if (value == null) {
            System.clearProperty(TRANSPORT_PROPERTY);
        } else {
            System.setProperty(TRANSPORT_PROPERTY, value);
        }
    }
}
