package dev.vatori.desktop.devtools;

import java.util.EnumSet;

final class AcceptanceGateExpansion {
    private AcceptanceGateExpansion() {
    }

    static EnumSet<AcceptanceGate> single(AcceptanceGate gate) {
        return EnumSet.of(gate);
    }

    static EnumSet<AcceptanceGate> pair(AcceptanceGate detailGate, AcceptanceGate aggregateGate) {
        return EnumSet.of(detailGate, aggregateGate);
    }
}
