package dev.vatori.desktop.devtools;

import java.util.EnumSet;

enum FrontendBuildGateRequirementFamily {
    CORE {
        @Override
        EnumSet<AcceptanceGate> expand(AcceptanceGate gate) {
            return FrontendBuildCoreGateRequirements.expand(gate);
        }
    },
    PRODUCTION_OWNER {
        @Override
        EnumSet<AcceptanceGate> expand(AcceptanceGate gate) {
            return FrontendBuildProductionOwnerGateRequirements.expand(gate);
        }
    },
    CURRENT_APP {
        @Override
        EnumSet<AcceptanceGate> expand(AcceptanceGate gate) {
            return FrontendBuildCurrentAppGateRequirements.expand(gate);
        }
    };

    abstract EnumSet<AcceptanceGate> expand(AcceptanceGate gate);
}
