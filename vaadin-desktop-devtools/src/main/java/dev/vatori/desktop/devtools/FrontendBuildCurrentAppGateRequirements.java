package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.AcceptanceGate.*;
import static dev.vatori.desktop.devtools.FrontendBuildGateRequirementSets.*;

import java.util.EnumSet;

final class FrontendBuildCurrentAppGateRequirements {
    private FrontendBuildCurrentAppGateRequirements() {
    }

    static EnumSet<AcceptanceGate> expand(AcceptanceGate gate) {
        return switch (gate) {
            case FLOW_IPC_V1_26_GENERATED_LAZY_SOURCE_BOUNDARY ->
                only(FLOW_IPC_V1_26_GENERATED_LAZY_SOURCE_BOUNDARY);
            case FLOW_IPC_V1_26 -> with(FLOW_IPC_V1_25,
                    FLOW_IPC_V1_26_GENERATED_LAZY_SOURCE_BOUNDARY,
                    FLOW_IPC_V1_26);
            case FLOW_IPC_V1_27_GENERATED_LAZY_REAL_BUILD_PROBE ->
                only(FLOW_IPC_V1_27_GENERATED_LAZY_REAL_BUILD_PROBE);
            case FLOW_IPC_V1_27 -> with(FLOW_IPC_V1_26,
                    FLOW_IPC_V1_27_GENERATED_LAZY_REAL_BUILD_PROBE,
                    FLOW_IPC_V1_27);
            case FLOW_IPC_V1_28_GENERATED_LAZY_BUILD_LIFECYCLE ->
                only(FLOW_IPC_V1_28_GENERATED_LAZY_BUILD_LIFECYCLE);
            case FLOW_IPC_V1_28 -> with(FLOW_IPC_V1_27,
                    FLOW_IPC_V1_28_GENERATED_LAZY_BUILD_LIFECYCLE,
                    FLOW_IPC_V1_28);
            case FLOW_IPC_V1_29_GENERATED_LAZY_APP_BUILD_OUTPUT ->
                only(FLOW_IPC_V1_29_GENERATED_LAZY_APP_BUILD_OUTPUT);
            case FLOW_IPC_V1_29 -> with(FLOW_IPC_V1_28,
                    FLOW_IPC_V1_29_GENERATED_LAZY_APP_BUILD_OUTPUT,
                    FLOW_IPC_V1_29);
            case FLOW_IPC_V1_30_CURRENT_APP_FRONTEND_BUILD_OUTPUT ->
                only(FLOW_IPC_V1_30_CURRENT_APP_FRONTEND_BUILD_OUTPUT);
            case FLOW_IPC_V1_30 -> with(FLOW_IPC_V1_29,
                    FLOW_IPC_V1_30_CURRENT_APP_FRONTEND_BUILD_OUTPUT,
                    FLOW_IPC_V1_30);
            case FLOW_IPC_V1_31_FRONTEND_BUILD_PREFLIGHT_AVAILABILITY ->
                only(FLOW_IPC_V1_31_FRONTEND_BUILD_PREFLIGHT_AVAILABILITY);
            case FLOW_IPC_V1_31 -> with(FLOW_IPC_V1_30,
                    FLOW_IPC_V1_31_FRONTEND_BUILD_PREFLIGHT_AVAILABILITY,
                    FLOW_IPC_V1_31);
            case FLOW_IPC_V1_32_BUILD_FRONTEND_OUTPUT ->
                only(FLOW_IPC_V1_32_BUILD_FRONTEND_OUTPUT);
            case FLOW_IPC_V1_32 -> with(FLOW_IPC_V1_31,
                    FLOW_IPC_V1_32_BUILD_FRONTEND_OUTPUT,
                    FLOW_IPC_V1_32);
            case FLOW_IPC_V1_33_CURRENT_APP_GENERATED_CHUNK_PACKAGE_EDGE ->
                only(FLOW_IPC_V1_33_CURRENT_APP_GENERATED_CHUNK_PACKAGE_EDGE);
            case FLOW_IPC_V1_33 -> with(FLOW_IPC_V1_32,
                    FLOW_IPC_V1_33_CURRENT_APP_GENERATED_CHUNK_PACKAGE_EDGE,
                    FLOW_IPC_V1_33);
            case FLOW_IPC_V1_34_CURRENT_APP_GENERATED_IMPORTS_ROOT_PACKAGE_GRAPH ->
                only(FLOW_IPC_V1_34_CURRENT_APP_GENERATED_IMPORTS_ROOT_PACKAGE_GRAPH);
            case FLOW_IPC_V1_34 -> with(FLOW_IPC_V1_33,
                    FLOW_IPC_V1_34_CURRENT_APP_GENERATED_IMPORTS_ROOT_PACKAGE_GRAPH,
                    FLOW_IPC_V1_34);
            case FLOW_IPC_V1_35_CURRENT_APP_PRODUCTION_RUNTIME_OWNERSHIP ->
                only(FLOW_IPC_V1_35_CURRENT_APP_PRODUCTION_RUNTIME_OWNERSHIP);
            case FLOW_IPC_V1_35 -> with(FLOW_IPC_V1_34,
                    FLOW_IPC_V1_35_CURRENT_APP_PRODUCTION_RUNTIME_OWNERSHIP,
                    FLOW_IPC_V1_35);
            case FLOW_IPC_V1_36_CURRENT_APP_PRODUCTION_OWNER_IMPORT ->
                only(FLOW_IPC_V1_36_CURRENT_APP_PRODUCTION_OWNER_IMPORT);
            case FLOW_IPC_V1_36 -> with(FLOW_IPC_V1_35,
                    FLOW_IPC_V1_36_CURRENT_APP_PRODUCTION_OWNER_IMPORT,
                    FLOW_IPC_V1_36);
            case FLOW_IPC_V1_37_CURRENT_APP_PRODUCTION_OWNER_COMPONENT_REUSE ->
                only(FLOW_IPC_V1_37_CURRENT_APP_PRODUCTION_OWNER_COMPONENT_REUSE);
            case FLOW_IPC_V1_37 -> only(FLOW_IPC_V1_37_CURRENT_APP_PRODUCTION_OWNER_COMPONENT_REUSE,
                    FLOW_IPC_V1_37);
            case FLOW_IPC_V1_38_CURRENT_APP_OWNER_COMPONENT_REUSE_MATRIX ->
                only(FLOW_IPC_V1_38_CURRENT_APP_OWNER_COMPONENT_REUSE_MATRIX);
            case FLOW_IPC_V1_38 -> only(FLOW_IPC_V1_38_CURRENT_APP_OWNER_COMPONENT_REUSE_MATRIX,
                    FLOW_IPC_V1_38);
            case FLOW_IPC_V1_39_CURRENT_APP_OWNER_EARLY_IMPORT ->
                only(FLOW_IPC_V1_39_CURRENT_APP_OWNER_EARLY_IMPORT);
            case FLOW_IPC_V1_39 -> only(FLOW_IPC_V1_39_CURRENT_APP_OWNER_EARLY_IMPORT,
                    FLOW_IPC_V1_39);
            case FLOW_IPC_V1_40_CURRENT_APP_GENERATED_IMPORTS_OWNER_SIDE_EFFECT ->
                only(FLOW_IPC_V1_40_CURRENT_APP_GENERATED_IMPORTS_OWNER_SIDE_EFFECT);
            case FLOW_IPC_V1_40 -> only(FLOW_IPC_V1_40_CURRENT_APP_GENERATED_IMPORTS_OWNER_SIDE_EFFECT,
                    FLOW_IPC_V1_40);
            case FLOW_IPC_V1_41_CURRENT_APP_GENERATED_IMPORTS_SELECT_OWNER_COVERAGE ->
                only(FLOW_IPC_V1_41_CURRENT_APP_GENERATED_IMPORTS_SELECT_OWNER_COVERAGE);
            case FLOW_IPC_V1_41 -> only(FLOW_IPC_V1_41_CURRENT_APP_GENERATED_IMPORTS_SELECT_OWNER_COVERAGE,
                    FLOW_IPC_V1_41);
            case FLOW_IPC_V1_42_GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT ->
                only(FLOW_IPC_V1_42_GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT);
            case FLOW_IPC_V1_42 -> only(FLOW_IPC_V1_42_GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT,
                    FLOW_IPC_V1_42);
            case FLOW_IPC_V1_43_GENERATED_LAZY_SPI_DECISION ->
                only(FLOW_IPC_V1_43_GENERATED_LAZY_SPI_DECISION);
            case FLOW_IPC_V1_43 -> only(FLOW_IPC_V1_43_GENERATED_LAZY_SPI_DECISION,
                    FLOW_IPC_V1_43);
            case FLOW_IPC_V1_44_PACKAGING_FRONTEND_INVENTORY ->
                only(FLOW_IPC_V1_44_PACKAGING_FRONTEND_INVENTORY);
            case FLOW_IPC_V1_44 -> only(FLOW_IPC_V1_44_PACKAGING_FRONTEND_INVENTORY,
                    FLOW_IPC_V1_44);
            case FLOW_IPC_V1_45_PACKAGING_RUNTIME_HANDOFF_REUSE ->
                only(FLOW_IPC_V1_45_PACKAGING_RUNTIME_HANDOFF_REUSE);
            case FLOW_IPC_V1_45_FRAMEWORK_PACKAGING_RUNTIME_HANDOFF_REUSE ->
                only(FLOW_IPC_V1_45_FRAMEWORK_PACKAGING_RUNTIME_HANDOFF_REUSE);
            case FLOW_IPC_V1_45 -> only(FLOW_IPC_V1_45_PACKAGING_RUNTIME_HANDOFF_REUSE,
                    FLOW_IPC_V1_45);
            default -> null;
        };
    }
}
