package dev.vatori.desktop.devtools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

public final class AcceptanceGateSmokeMain {
    private AcceptanceGateSmokeMain() {
    }

    public static void main(String[] args) throws IOException {
        for (AcceptanceGate gate : AcceptanceGate.values()) {
            requireSame(gate, AcceptanceGate.fromOption(gate.option()),
                    "option roundtrip for " + gate);
            requireSame(gate.family(), AcceptanceGateFamily.of(gate),
                    "family lookup for " + gate);
            requireTrue(gate.expandedRequirements().contains(gate),
                    "expanded requirements include requested gate for " + gate);
        }

        requireSame(AcceptanceGate.FLOW_IPC_V1,
                AcceptanceGate.fromOption("--require-flow-ipc-v1"),
                "base v1 option");
        requireSame(AcceptanceGate.FLOW_IPC_V1_9_LOADONDEMAND,
                AcceptanceGate.fromOption("--require-flow-ipc-v1_9-loadondemand"),
                "v1.9 load-on-demand option");
        requireSame(AcceptanceGate.FLOW_IPC_V1_10_LAZY_DEPENDENCY,
                AcceptanceGate.fromOption("--require-flow-ipc-v1_10-lazy-dependency"),
                "v1.10 lazy dependency option");
        requireSame(AcceptanceGate.FLOW_IPC_V1_146,
                AcceptanceGate.fromOption("--require-flow-ipc-v1_146"),
                "v1.146 aggregate option");
        requireSame(AcceptanceGate.FLOW_IPC_V1_3_FRAMEWORK,
                AcceptanceGate.fromOption("--require-flow-ipc-v1_3-framework"),
                "framework option");
        requireEquals(1, AcceptanceGateMilestones.number(AcceptanceGate.FLOW_IPC_V1),
                "base v1 milestone");
        requireEquals(3,
                AcceptanceGateMilestones.number(AcceptanceGate.FLOW_IPC_V1_3_FRAMEWORK),
                "framework gate milestone");
        requireEquals(146,
                AcceptanceGateMilestones.number(AcceptanceGate.FLOW_IPC_V1_146),
                "latest authentication aggregate milestone");

        requireFamily(AcceptanceGateFamily.CORE,
                AcceptanceGate.FLOW_IPC_V1,
                AcceptanceGate.FLOW_IPC_V1_3);
        requireFamily(AcceptanceGateFamily.FRONTEND_BUILD,
                AcceptanceGate.FLOW_IPC_V1_4,
                AcceptanceGate.FLOW_IPC_V1_45);
        requireFamily(AcceptanceGateFamily.COMPONENT_RESOURCE,
                AcceptanceGate.FLOW_IPC_V1_46_BROADER_COMPONENT_RESOURCE_MATRIX,
                AcceptanceGate.FLOW_IPC_V1_65);
        requireFamily(AcceptanceGateFamily.THEME_MATRIX,
                AcceptanceGate.FLOW_IPC_V1_66_RICH_TEXT_EDITOR_THEME_VARIANT_MATRIX,
                AcceptanceGate.FLOW_IPC_V1_89);
        requireFamily(AcceptanceGateFamily.AUTHENTICATION,
                AcceptanceGate.FLOW_IPC_V1_90_LOGIN_OVERLAY_PROPERTY_MATRIX,
                AcceptanceGate.FLOW_IPC_V1_146);
        requireFamily(AcceptanceGateFamily.FRAMEWORK,
                AcceptanceGate.FLOW_IPC_V1_3_FRAMEWORK);
        requireAuthenticationRouting(
                AcceptanceGate.FLOW_IPC_V1_90_LOGIN_OVERLAY_PROPERTY_MATRIX,
                AcceptanceGate.FLOW_IPC_V1_96,
                AcceptanceGate.FLOW_IPC_V1_97_LOGIN_FORM_LOGIN_EVENT,
                AcceptanceGate.FLOW_IPC_V1_110,
                AcceptanceGate.FLOW_IPC_V1_111_LOGIN_OVERLAY_SHOW_ERROR,
                AcceptanceGate.FLOW_IPC_V1_128,
                AcceptanceGate.FLOW_IPC_V1_129_LOGIN_OVERLAY_A11Y_SEMANTICS,
                AcceptanceGate.FLOW_IPC_V1_146);

        EnumSet<AcceptanceGate> coreV1_3 =
                AcceptanceGate.FLOW_IPC_V1_3.expandedRequirements();
        requireTrue(coreV1_3.equals(EnumSet.of(
                AcceptanceGate.FLOW_IPC_V1,
                AcceptanceGate.FLOW_IPC_V1_1,
                AcceptanceGate.FLOW_IPC_V1_2,
                AcceptanceGate.FLOW_IPC_V1_3)),
                "core v1.3 expands through earlier core gates");
        requireCliUsageExit(TraceInspectorCli.EXIT_OK, true, "--help");
        requireCliUsageExit(TraceInspectorCli.EXIT_USAGE_ERROR, false);
        requireCliUsageExit(TraceInspectorCli.EXIT_USAGE_ERROR, false, "--require-flow-ipc-v1");
        requireUnknownOptionExit();
        requireMissingTracePathExit();
        requireMixedTraceBatchExit();
        requireMissingGatePathContextExit();
        requireCliTracePathExit();

        System.out.println("Acceptance gate option/family smoke passed");
    }

    private static void requireFamily(
            AcceptanceGateFamily expected,
            AcceptanceGate... gates) {
        for (AcceptanceGate gate : gates) {
            requireSame(expected, gate.family(), "family for " + gate);
        }
    }

    private static void requireAuthenticationRouting(AcceptanceGate... gates) {
        TraceSummary emptySummary = new TraceSummary();
        for (AcceptanceGate gate : gates) {
            AuthenticationTraceRequirementChecks.isSatisfied(gate, emptySummary);
        }
    }

    private static void requireCliUsageExit(
            int expectedStatus,
            boolean stdoutUsage,
            String... args) {
        CliRunResult result = runCli(args);
        requireEquals(expectedStatus, result.status(), "CLI exit for " + String.join(" ", args));
        String usageOutput = stdoutUsage ? result.stdout() : result.stderr();
        String emptyOutput = stdoutUsage ? result.stderr() : result.stdout();
        requireTrue(usageOutput.startsWith(
                "Usage: bin/flow-ipc-trace-inspect [--require-flow-ipc-v1"),
                "CLI usage output for " + String.join(" ", args));
        requireEquals("", emptyOutput,
                "CLI empty stream for " + String.join(" ", args));
    }

    private static void requireUnknownOptionExit() {
        CliRunResult result = runCli("--require-flow-ipc-v1_999");
        requireEquals(TraceInspectorCli.EXIT_USAGE_ERROR, result.status(),
                "unknown option CLI exit");
        requireEquals("", result.stdout(), "unknown option CLI stdout");
        requireTrue(result.stderr().startsWith("Unknown option: --require-flow-ipc-v1_999\n"
                + "Usage: bin/flow-ipc-trace-inspect [--require-flow-ipc-v1"),
                "unknown option CLI stderr");
    }

    private static void requireMissingTracePathExit() throws IOException {
        Path missingTrace = Files.createTempFile("vatori-trace-inspector-missing", ".log");
        Files.deleteIfExists(missingTrace);
        CliRunResult result = runCli(missingTrace.toString());
        requireEquals(TraceInspectorCli.EXIT_USAGE_ERROR, result.status(),
                "missing trace path CLI exit");
        requireEquals("", result.stdout(), "missing trace path CLI stdout");
        requireTrue(result.stderr().startsWith("Unable to read trace: " + missingTrace),
                "missing trace path CLI stderr");
    }

    private static void requireMixedTraceBatchExit() throws IOException {
        Path missingTrace = Files.createTempFile("vatori-trace-inspector-batch-missing", ".log");
        Path validTrace = Files.createTempFile("vatori-trace-inspector-batch-valid", ".log");
        Files.deleteIfExists(missingTrace);
        try {
            Files.writeString(validTrace,
                    "HOST_PROTOCOL {\"type\":\"launch\",\"direction\":\"send\","
                            + "\"payload\":{\"transport\":\"flow-ipc\"}}\n",
                    StandardCharsets.UTF_8);
            CliRunResult result = runCli(missingTrace.toString(), validTrace.toString());
            requireEquals(TraceInspectorCli.EXIT_USAGE_ERROR, result.status(),
                    "mixed trace batch CLI exit");
            requireTrue(result.stderr().startsWith("Unable to read trace: " + missingTrace),
                    "mixed trace batch stderr");
            requireTrue(result.stdout().startsWith("Trace: " + validTrace),
                    "mixed trace batch stdout");
        } finally {
            Files.deleteIfExists(validTrace);
        }
    }

    private static void requireMissingGatePathContextExit() throws IOException {
        Path trace = Files.createTempFile("vatori-trace-inspector-missing-gate", ".log");
        try {
            Files.writeString(trace,
                    "HOST_PROTOCOL {\"type\":\"launch\",\"direction\":\"send\","
                            + "\"payload\":{\"transport\":\"flow-ipc\"}}\n",
                    StandardCharsets.UTF_8);
            CliRunResult result = runCli("--require-flow-ipc-v1", trace.toString());
            requireEquals(TraceInspectorCli.EXIT_USAGE_ERROR, result.status(),
                    "missing gate CLI exit");
            requireTrue(result.stdout().startsWith("Trace: " + trace),
                    "missing gate CLI stdout");
            requireTrue(result.stderr().contains("Missing required acceptance gate for " + trace
                            + ": --require-flow-ipc-v1"),
                    "missing gate CLI stderr path context");
        } finally {
            Files.deleteIfExists(trace);
        }
    }

    private static void requireCliTracePathExit() throws IOException {
        Path trace = Files.createTempFile("vatori-trace-inspector-cli-smoke", ".log");
        try {
            Files.writeString(trace,
                    "HOST_PROTOCOL {\"type\":\"launch\",\"direction\":\"send\","
                            + "\"payload\":{\"transport\":\"flow-ipc\"}}\n",
                    StandardCharsets.UTF_8);
            CliRunResult result = runCli(trace.toString());
            requireEquals(TraceInspectorCli.EXIT_OK, result.status(), "CLI trace path exit");
            requireTrue(result.stdout().startsWith("Trace: " + trace),
                    "CLI trace path stdout");
            requireEquals("", result.stderr(), "CLI trace path stderr");
        } finally {
            Files.deleteIfExists(trace);
        }
    }

    private static CliRunResult runCli(String... args) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int status = TraceInspectorCli.run(
                args,
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
        return new CliRunResult(
                status,
                stdout.toString(StandardCharsets.UTF_8),
                stderr.toString(StandardCharsets.UTF_8));
    }

    private static void requireSame(Object expected, Object actual, String label) {
        if (expected != actual) {
            throw new IllegalStateException(label + ": expected " + expected
                    + " but got " + actual);
        }
    }

    private static void requireEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException(label + ": expected " + expected
                    + " but got " + actual);
        }
    }

    private static void requireTrue(boolean value, String label) {
        if (!value) {
            throw new IllegalStateException(label);
        }
    }

    private record CliRunResult(int status, String stdout, String stderr) {
    }
}
