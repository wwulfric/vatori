package dev.vatori.desktop.devtools;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

final class TraceInspectorCli {
    static final int EXIT_OK = 0;
    static final int EXIT_USAGE_ERROR = 2;

    private TraceInspectorCli() {
    }

    static int run(String[] args) {
        return run(args, System.out, System.err);
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length == 0) {
            usage(err);
            return EXIT_USAGE_ERROR;
        }
        if ("--help".equals(args[0])) {
            usage(out);
            return EXIT_OK;
        }

        EnumSet<AcceptanceGate> requiredGates = EnumSet.noneOf(AcceptanceGate.class);
        List<Path> paths = new ArrayList<>();
        for (String arg : args) {
            AcceptanceGate gate = AcceptanceGate.fromOption(arg);
            if (gate != null) {
                requiredGates.addAll(gate.expandedRequirements());
            } else if (arg.startsWith("--")) {
                err.println("Unknown option: " + arg);
                usage(err);
                return EXIT_USAGE_ERROR;
            } else {
                paths.add(Path.of(arg));
            }
        }

        if (paths.isEmpty()) {
            usage(err);
            return EXIT_USAGE_ERROR;
        }

        int failures = 0;
        for (Path path : paths) {
            TraceSummary summary = inspectTrace(path, err);
            if (summary == null) {
                failures++;
                continue;
            }
            TraceSummaryPrinter.print(
                    summary,
                    path,
                    requiredGates.contains(AcceptanceGate.FLOW_IPC_V1),
                    out);
            failures += reportMissingRequiredGates(
                    path,
                    evaluateRequiredGates(summary, requiredGates),
                    err);
        }

        if (failures > 0) {
            return EXIT_USAGE_ERROR;
        }
        return EXIT_OK;
    }

    private static TraceSummary inspectTrace(Path path, PrintStream err) {
        try {
            return DesktopTraceInspector.inspect(path);
        } catch (IOException exception) {
            err.println("Unable to read trace: " + path + " (" + exception.getMessage() + ")");
            return null;
        }
    }

    private static List<GateEvaluation> evaluateRequiredGates(
            TraceSummary summary,
            EnumSet<AcceptanceGate> requiredGates) {
        List<GateEvaluation> evaluations = new ArrayList<>();
        for (AcceptanceGate gate : AcceptanceGate.values()) {
            if (requiredGates.contains(gate)) {
                evaluations.add(new GateEvaluation(gate, gate.isSatisfiedBy(summary)));
            }
        }
        return evaluations;
    }

    private static int reportMissingRequiredGates(
            Path path,
            List<GateEvaluation> evaluations,
            PrintStream err) {
        int failures = 0;
        for (GateEvaluation evaluation : evaluations) {
            if (evaluation.missing()) {
                err.println(evaluation.missingMessage(path));
                failures++;
            }
        }
        return failures;
    }

    private static void usage(PrintStream out) {
        out.println("Usage: bin/flow-ipc-trace-inspect [" + AcceptanceGate.usageOptions()
                + "] <trace.ndjson>...");
    }

    private record GateEvaluation(AcceptanceGate gate, boolean satisfied) {
        boolean missing() {
            return !satisfied;
        }

        String missingMessage(Path path) {
            return "Missing required acceptance gate for " + path + ": " + gate.option();
        }
    }
}
