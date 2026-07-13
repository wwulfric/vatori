package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.AcceptanceGate.*;
import static dev.vatori.desktop.devtools.FrontendBuildGateRequirementSets.*;

import java.util.EnumSet;

final class FrontendBuildCoreGateRequirements {
    private FrontendBuildCoreGateRequirements() {
    }

    static EnumSet<AcceptanceGate> expand(AcceptanceGate gate) {
        return switch (gate) {
            case FLOW_IPC_V1_4 -> withBase(FLOW_IPC_V1_4);
            case FLOW_IPC_V1_5 -> with(FLOW_IPC_V1_4, FLOW_IPC_V1_5);
            case FLOW_IPC_V1_6 -> with(FLOW_IPC_V1_5, FLOW_IPC_V1_6);
            // v1.6 is a preserved blocker-evidence gate. v1.7/v1.8 supersede that path.
            case FLOW_IPC_V1_7 -> with(FLOW_IPC_V1_5, FLOW_IPC_V1_7);
            case FLOW_IPC_V1_8 -> with(FLOW_IPC_V1_7, FLOW_IPC_V1_8);
            case FLOW_IPC_V1_9_LOADONDEMAND -> only(FLOW_IPC_V1_9_LOADONDEMAND);
            case FLOW_IPC_V1_9 -> with(FLOW_IPC_V1_8, FLOW_IPC_V1_9_LOADONDEMAND,
                    FLOW_IPC_V1_9);
            case FLOW_IPC_V1_10_LAZY_DEPENDENCY -> only(FLOW_IPC_V1_10_LAZY_DEPENDENCY);
            case FLOW_IPC_V1_10_PRODUCTION_RUNTIME -> only(FLOW_IPC_V1_10_PRODUCTION_RUNTIME);
            case FLOW_IPC_V1_10 -> with(FLOW_IPC_V1_9,
                    FLOW_IPC_V1_10_LAZY_DEPENDENCY,
                    FLOW_IPC_V1_10_PRODUCTION_RUNTIME,
                    FLOW_IPC_V1_10);
            case FLOW_IPC_V1_11_LOADONDEMAND_CHUNK -> only(FLOW_IPC_V1_11_LOADONDEMAND_CHUNK);
            case FLOW_IPC_V1_11 -> with(FLOW_IPC_V1_10,
                    FLOW_IPC_V1_11_LOADONDEMAND_CHUNK,
                    FLOW_IPC_V1_11);
            case FLOW_IPC_V1_12_LOADONDEMAND_CHUNK_FILE ->
                only(FLOW_IPC_V1_12_LOADONDEMAND_CHUNK_FILE);
            case FLOW_IPC_V1_12 -> with(FLOW_IPC_V1_11,
                    FLOW_IPC_V1_12_LOADONDEMAND_CHUNK_FILE,
                    FLOW_IPC_V1_12);
            default -> null;
        };
    }
}
