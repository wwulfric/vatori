package dev.vatori.desktop.devtools;

import java.util.EnumSet;

final class FrontendBuildAcceptanceGateRequirements {
    private FrontendBuildAcceptanceGateRequirements() {
    }

    static EnumSet<AcceptanceGate> expand(AcceptanceGate gate) {
        for (FrontendBuildGateRequirementFamily family : FrontendBuildGateRequirementFamily.values()) {
            EnumSet<AcceptanceGate> requirements = family.expand(gate);
            if (requirements != null) {
                return requirements;
            }
        }
        throw new IllegalArgumentException("Unsupported frontend build gate: " + gate);
    }
}
