package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.AcceptanceGate.*;

import java.util.EnumSet;

final class FrontendBuildGateRequirementSets {
    private FrontendBuildGateRequirementSets() {
    }

    static EnumSet<AcceptanceGate> withBase(AcceptanceGate... gates) {
        EnumSet<AcceptanceGate> result = EnumSet.of(FLOW_IPC_V1, FLOW_IPC_V1_1, FLOW_IPC_V1_2,
                FLOW_IPC_V1_3);
        addAll(result, gates);
        return result;
    }

    static EnumSet<AcceptanceGate> with(AcceptanceGate previousMilestone,
            AcceptanceGate... gates) {
        EnumSet<AcceptanceGate> result = previousMilestone.expandedRequirements();
        addAll(result, gates);
        return result;
    }

    static EnumSet<AcceptanceGate> only(AcceptanceGate gate, AcceptanceGate... gates) {
        EnumSet<AcceptanceGate> result = EnumSet.of(gate);
        addAll(result, gates);
        return result;
    }

    private static void addAll(EnumSet<AcceptanceGate> result, AcceptanceGate... gates) {
        for (AcceptanceGate gate : gates) {
            result.add(gate);
        }
    }
}
