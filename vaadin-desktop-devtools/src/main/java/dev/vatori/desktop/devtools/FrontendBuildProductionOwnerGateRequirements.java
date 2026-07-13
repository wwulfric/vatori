package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.AcceptanceGate.*;
import static dev.vatori.desktop.devtools.FrontendBuildGateRequirementSets.*;

import java.util.EnumSet;

final class FrontendBuildProductionOwnerGateRequirements {
    private FrontendBuildProductionOwnerGateRequirements() {
    }

    static EnumSet<AcceptanceGate> expand(AcceptanceGate gate) {
        return switch (gate) {
            case FLOW_IPC_V1_13_PRODUCTION_OWNER -> only(FLOW_IPC_V1_13_PRODUCTION_OWNER);
            case FLOW_IPC_V1_13 -> with(FLOW_IPC_V1_12,
                    FLOW_IPC_V1_13_PRODUCTION_OWNER,
                    FLOW_IPC_V1_13);
            case FLOW_IPC_V1_14_PRODUCTION_OWNER_IMPORT ->
                only(FLOW_IPC_V1_14_PRODUCTION_OWNER_IMPORT);
            case FLOW_IPC_V1_14 -> with(FLOW_IPC_V1_13,
                    FLOW_IPC_V1_14_PRODUCTION_OWNER_IMPORT,
                    FLOW_IPC_V1_14);
            case FLOW_IPC_V1_15_PRODUCTION_OWNER_DEPENDENCIES ->
                only(FLOW_IPC_V1_15_PRODUCTION_OWNER_DEPENDENCIES);
            case FLOW_IPC_V1_15 -> with(FLOW_IPC_V1_14,
                    FLOW_IPC_V1_15_PRODUCTION_OWNER_DEPENDENCIES,
                    FLOW_IPC_V1_15);
            case FLOW_IPC_V1_16_PRODUCTION_OWNER_ORDERING ->
                only(FLOW_IPC_V1_16_PRODUCTION_OWNER_ORDERING);
            case FLOW_IPC_V1_16 -> with(FLOW_IPC_V1_15,
                    FLOW_IPC_V1_16_PRODUCTION_OWNER_ORDERING,
                    FLOW_IPC_V1_16);
            case FLOW_IPC_V1_17_RESOURCE_SCHEME -> only(FLOW_IPC_V1_17_RESOURCE_SCHEME);
            case FLOW_IPC_V1_17 -> with(FLOW_IPC_V1_16,
                    FLOW_IPC_V1_17_RESOURCE_SCHEME,
                    FLOW_IPC_V1_17);
            case FLOW_IPC_V1_18_PRODUCTION_OWNER_RESOURCE_SCHEME ->
                only(FLOW_IPC_V1_18_PRODUCTION_OWNER_RESOURCE_SCHEME);
            case FLOW_IPC_V1_18 -> with(FLOW_IPC_V1_17,
                    FLOW_IPC_V1_18_PRODUCTION_OWNER_RESOURCE_SCHEME,
                    FLOW_IPC_V1_18);
            case FLOW_IPC_V1_19_PRODUCTION_OWNER_SIDE_EFFECT ->
                only(FLOW_IPC_V1_19_PRODUCTION_OWNER_SIDE_EFFECT);
            case FLOW_IPC_V1_19 -> with(FLOW_IPC_V1_18,
                    FLOW_IPC_V1_19_PRODUCTION_OWNER_SIDE_EFFECT,
                    FLOW_IPC_V1_19);
            case FLOW_IPC_V1_20_PRODUCTION_OWNER_OWNED_RUNTIME ->
                only(FLOW_IPC_V1_20_PRODUCTION_OWNER_OWNED_RUNTIME);
            case FLOW_IPC_V1_20 -> with(FLOW_IPC_V1_19,
                    FLOW_IPC_V1_20_PRODUCTION_OWNER_OWNED_RUNTIME,
                    FLOW_IPC_V1_20);
            case FLOW_IPC_V1_21_FRONTEND_RESOLVER_CONTRACT ->
                only(FLOW_IPC_V1_21_FRONTEND_RESOLVER_CONTRACT);
            case FLOW_IPC_V1_21 -> with(FLOW_IPC_V1_20,
                    FLOW_IPC_V1_21_FRONTEND_RESOLVER_CONTRACT,
                    FLOW_IPC_V1_21);
            case FLOW_IPC_V1_22_GENERATED_CHUNK_FIXTURE ->
                only(FLOW_IPC_V1_22_GENERATED_CHUNK_FIXTURE);
            case FLOW_IPC_V1_22 -> with(FLOW_IPC_V1_21,
                    FLOW_IPC_V1_22_GENERATED_CHUNK_FIXTURE,
                    FLOW_IPC_V1_22);
            case FLOW_IPC_V1_23_PRODUCTION_OWNER_POSITIVE_FIXTURE ->
                only(FLOW_IPC_V1_23_PRODUCTION_OWNER_POSITIVE_FIXTURE);
            case FLOW_IPC_V1_23 -> with(FLOW_IPC_V1_22,
                    FLOW_IPC_V1_23_PRODUCTION_OWNER_POSITIVE_FIXTURE,
                    FLOW_IPC_V1_23);
            case FLOW_IPC_V1_24_FRONTEND_PIPELINE_CLOSURE ->
                only(FLOW_IPC_V1_24_FRONTEND_PIPELINE_CLOSURE);
            case FLOW_IPC_V1_24 -> with(FLOW_IPC_V1_23,
                    FLOW_IPC_V1_24_FRONTEND_PIPELINE_CLOSURE,
                    FLOW_IPC_V1_24);
            case FLOW_IPC_V1_25_PACKAGED_FRONTEND_BASELINE ->
                only(FLOW_IPC_V1_25_PACKAGED_FRONTEND_BASELINE);
            case FLOW_IPC_V1_25 -> with(FLOW_IPC_V1_24,
                    FLOW_IPC_V1_25_PACKAGED_FRONTEND_BASELINE,
                    FLOW_IPC_V1_25);
            default -> null;
        };
    }
}
