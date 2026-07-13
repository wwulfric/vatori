package dev.vatori.desktop.devtools;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

final class TraceRequirementChain {
    private final String label;
    private final Predicate<TraceSummary> baseCheck;
    private final Entry[] entries;
    private final Map<AcceptanceGate, Predicate<TraceSummary>> gateChecks;

    private TraceRequirementChain(String label, Predicate<TraceSummary> baseCheck, Entry... entries) {
        this.label = Objects.requireNonNull(label, "label");
        this.baseCheck = Objects.requireNonNull(baseCheck, "baseCheck");
        this.entries = Objects.requireNonNull(entries, "entries").clone();

        EnumMap<AcceptanceGate, Predicate<TraceSummary>> gateChecks =
                new EnumMap<>(AcceptanceGate.class);
        for (int i = 0; i < this.entries.length; i++) {
            Entry entry = Objects.requireNonNull(this.entries[i], "entry");
            int index = i;
            add(gateChecks, entry.detailGate(), entry.detailCheck());
            add(gateChecks, entry.cumulativeGate(), summary -> hasCumulative(index, summary));
        }
        this.gateChecks = Collections.unmodifiableMap(gateChecks);
    }

    static TraceRequirementChain cumulative(
            String label, Predicate<TraceSummary> baseCheck, Entry... entries) {
        return new TraceRequirementChain(label, baseCheck, entries);
    }

    static Entry entry(
            AcceptanceGate detailGate,
            AcceptanceGate cumulativeGate,
            Predicate<TraceSummary> detailCheck) {
        return new Entry(detailGate, cumulativeGate, detailCheck);
    }

    boolean isSatisfied(AcceptanceGate gate, TraceSummary summary) {
        Predicate<TraceSummary> check = gateChecks.get(gate);
        if (check == null) {
            throw new IllegalArgumentException("Unsupported " + label + ": " + gate);
        }
        return check.test(summary);
    }

    private boolean hasCumulative(int index, TraceSummary summary) {
        entryAt(index);
        if (!baseCheck.test(summary)) {
            return false;
        }
        for (int i = 0; i <= index; i++) {
            if (!entries[i].detailCheck().test(summary)) {
                return false;
            }
        }
        return true;
    }

    private Entry entryAt(int index) {
        if (index < 0 || index >= entries.length) {
            throw new IllegalArgumentException("Unsupported " + label + " index: " + index);
        }
        return entries[index];
    }

    private static void add(
            EnumMap<AcceptanceGate, Predicate<TraceSummary>> gateChecks,
            AcceptanceGate gate,
            Predicate<TraceSummary> check) {
        Predicate<TraceSummary> previous = gateChecks.put(
                Objects.requireNonNull(gate, "gate"),
                Objects.requireNonNull(check, "check"));
        if (previous != null) {
            throw new IllegalArgumentException("Duplicate trace requirement gate: " + gate);
        }
    }

    record Entry(
            AcceptanceGate detailGate,
            AcceptanceGate cumulativeGate,
            Predicate<TraceSummary> detailCheck) {
        Entry {
            Objects.requireNonNull(detailGate, "detailGate");
            Objects.requireNonNull(cumulativeGate, "cumulativeGate");
            Objects.requireNonNull(detailCheck, "detailCheck");
        }
    }
}
