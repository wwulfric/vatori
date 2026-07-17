package dev.vatori.desktop.spring;

import dev.vatori.desktop.DesktopApplication;
import dev.vatori.desktop.DesktopWindow;
import dev.vatori.desktop.spring.launcherfixture.LauncherContractApplication;
import dev.vatori.desktop.spring.launcherfixture.LauncherContractLifecycle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public final class SpringLauncherContractSmokeMain {
    private static final String TRANSPORT_PROPERTY = "vaadin.desktop.transport";

    private SpringLauncherContractSmokeMain() {
    }

    public static void main(String[] args) {
        String flowOutput = launchFlowUntilEof();
        assertReady(flowOutput, "flow-ipc", false);
        String compatibilityOutput = launchCompatibilityUntilShutdown(
                "VFIPC {\"version\":1,\"type\":\"shutdown\",\"id\":\"contract-shutdown\",\"payload\":{}}\n"
                        .getBytes(StandardCharsets.UTF_8));
        assertReady(compatibilityOutput, "compatibility-http", true);
        if (LauncherContractLifecycle.closedContexts() != 2) {
            throw new IllegalStateException(
                    "SpringDesktopLauncher did not close contexts after EOF and shutdown");
        }
        System.out.println("Spring desktop launcher lifecycle contract passed");
    }

    private static String launchFlowUntilEof() {
        String previousTransport = System.getProperty(TRANSPORT_PROPERTY);
        InputStream previousIn = System.in;
        PrintStream previousOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try {
            System.setProperty(TRANSPORT_PROPERTY, "flow-ipc");
            System.setIn(new ByteArrayInputStream(new byte[0]));
            System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
            try {
                SpringDesktopLauncher.launch(
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
        String previousTransport = System.getProperty(TRANSPORT_PROPERTY);
        InputStream previousIn = System.in;
        PrintStream previousOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try {
            System.setProperty(TRANSPORT_PROPERTY, "compatibility-http");
            System.setIn(new ByteArrayInputStream(input));
            System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
            SpringDesktopLauncher.launch(
                    LauncherContractApplication.class, application(), new String[0]);
            return captured.toString(StandardCharsets.UTF_8);
        } finally {
            System.setIn(previousIn);
            System.setOut(previousOut);
            restoreProperty(previousTransport);
        }
    }

    private static DesktopApplication application() {
        return DesktopApplication.builder("Spring Launcher Contract")
                .metadata("contract", "spring-sidecar")
                .mainWindow(DesktopWindow.builder("main")
                        .title("Spring Launcher Contract Window")
                        .size(880, 640)
                        .build())
                .build();
    }

    private static void assertReady(
            String output,
            String transport,
            boolean expectsApplicationUrl) {
        for (String token : new String[]{
                "VFIPC ",
                "\"type\":\"ready\"",
                "\"transport\":\"" + transport + "\"",
                "\"name\":\"Spring Launcher Contract\"",
                "\"id\":\"main\"",
                "\"title\":\"Spring Launcher Contract Window\""}) {
            if (!output.contains(token)) {
                throw new IllegalStateException(
                        "Spring sidecar ready payload is missing " + token + ":\n" + output);
            }
        }
        if (expectsApplicationUrl != output.contains("\"applicationUrl\":\"http://127.0.0.1:")) {
            throw new IllegalStateException(
                    "Spring sidecar applicationUrl contract is invalid:\n" + output);
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
