package dev.vatori.desktop.devtools;

import java.io.PrintStream;
import java.util.Objects;

final class TraceSummaryLinePrinter {
    private static final ThreadLocal<PrintStream> OUTPUT = new ThreadLocal<>();

    private TraceSummaryLinePrinter() {
    }

    static void withOutput(PrintStream output, Runnable printer) {
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(printer, "printer");
        PrintStream previous = OUTPUT.get();
        OUTPUT.set(output);
        try {
            printer.run();
        } finally {
            if (previous == null) {
                OUTPUT.remove();
            } else {
                OUTPUT.set(previous);
            }
        }
    }

    static void printLine(String label, boolean... values) {
        printLine(label, "", values);
    }

    static void printLine(String label, String valuePrefix, boolean... values) {
        StringBuilder line = new StringBuilder(label).append(": ").append(valuePrefix);
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                line.append('/');
            }
            line.append(values[index]);
        }
        output().println(line);
    }

    private static PrintStream output() {
        PrintStream output = OUTPUT.get();
        return output == null ? System.out : output;
    }
}
