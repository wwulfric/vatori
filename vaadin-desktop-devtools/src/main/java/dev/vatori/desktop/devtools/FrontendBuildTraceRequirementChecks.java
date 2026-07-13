package dev.vatori.desktop.devtools;

final class FrontendBuildTraceRequirementChecks {
    private FrontendBuildTraceRequirementChecks() {
    }

    static boolean isSatisfied(AcceptanceGate gate, TraceSummary summary) {
        return switch (gate) {
            case FLOW_IPC_V1_4 -> TraceSummaryFrontendPipelineRequirementChecks
                    .hasRequiredFlowIpcV1_4Types(summary);
            case FLOW_IPC_V1_5 -> TraceSummaryFrontendPipelineRequirementChecks
                    .hasRequiredFlowIpcV1_5Types(summary);
            case FLOW_IPC_V1_6 -> TraceSummaryFrontendPipelineRequirementChecks
                    .hasRequiredFlowIpcV1_6Types(summary);
            case FLOW_IPC_V1_7 -> TraceSummaryFrontendPipelineRequirementChecks
                    .hasRequiredFlowIpcV1_7Types(summary);
            case FLOW_IPC_V1_8 -> TraceSummaryFrontendPipelineRequirementChecks
                    .hasRequiredFlowIpcV1_8Types(summary);
            case FLOW_IPC_V1_9_LOADONDEMAND -> TraceSummaryFrontendPipelineRequirementChecks
                    .hasRequiredFlowIpcV1_9LoadOnDemandTypes(summary);
            case FLOW_IPC_V1_9 -> TraceSummaryFrontendPipelineRequirementChecks
                    .hasRequiredFlowIpcV1_9Types(summary);
            case FLOW_IPC_V1_10_LAZY_DEPENDENCY -> TraceSummaryFrontendPipelineRequirementChecks
                    .hasRequiredFlowIpcV1_10LazyDependencyTypes(summary);
            case FLOW_IPC_V1_10_PRODUCTION_RUNTIME -> TraceSummaryFrontendPipelineRequirementChecks
                    .hasRequiredFlowIpcV1_10ProductionRuntimeTypes(summary);
            case FLOW_IPC_V1_10 -> TraceSummaryFrontendPipelineRequirementChecks
                    .hasRequiredFlowIpcV1_10Types(summary);
            case FLOW_IPC_V1_11_LOADONDEMAND_CHUNK -> TraceSummaryFrontendPipelineRequirementChecks
                    .hasRequiredFlowIpcV1_11LoadOnDemandChunkTypes(summary);
            case FLOW_IPC_V1_11 -> TraceSummaryFrontendPipelineRequirementChecks
                    .hasRequiredFlowIpcV1_11Types(summary);
            case FLOW_IPC_V1_12_LOADONDEMAND_CHUNK_FILE ->
                TraceSummaryFrontendPipelineRequirementChecks
                        .hasRequiredFlowIpcV1_12LoadOnDemandChunkFileTypes(summary);
            case FLOW_IPC_V1_12 -> TraceSummaryFrontendPipelineRequirementChecks
                    .hasRequiredFlowIpcV1_12Types(summary);
            case FLOW_IPC_V1_13_PRODUCTION_OWNER ->
                TraceSummaryFrontendPipelineRequirementChecks
                        .hasRequiredFlowIpcV1_13ProductionOwnerTypes(summary);
            case FLOW_IPC_V1_13 -> TraceSummaryFrontendPipelineRequirementChecks
                    .hasRequiredFlowIpcV1_13Types(summary);
            case FLOW_IPC_V1_14_PRODUCTION_OWNER_IMPORT ->
                TraceSummaryFrontendPipelineRequirementChecks
                        .hasRequiredFlowIpcV1_14ProductionOwnerImportTypes(summary);
            case FLOW_IPC_V1_14 -> TraceSummaryFrontendPipelineRequirementChecks
                    .hasRequiredFlowIpcV1_14Types(summary);
            case FLOW_IPC_V1_15_PRODUCTION_OWNER_DEPENDENCIES ->
                TraceSummaryFrontendPipelineRequirementChecks
                        .hasRequiredFlowIpcV1_15ProductionOwnerDependencyTypes(summary);
            case FLOW_IPC_V1_15 -> TraceSummaryFrontendPipelineRequirementChecks
                    .hasRequiredFlowIpcV1_15Types(summary);
            case FLOW_IPC_V1_16_PRODUCTION_OWNER_ORDERING ->
                TraceSummaryFrontendPipelineRequirementChecks
                        .hasRequiredFlowIpcV1_16ProductionOwnerOrderingTypes(summary);
            case FLOW_IPC_V1_16 -> TraceSummaryFrontendPipelineRequirementChecks
                    .hasRequiredFlowIpcV1_16Types(summary);
            case FLOW_IPC_V1_17_RESOURCE_SCHEME ->
                TraceSummaryFrontendPipelineRequirementChecks
                        .hasRequiredFlowIpcV1_17ResourceSchemeTypes(summary);
            case FLOW_IPC_V1_17 -> TraceSummaryFrontendPipelineRequirementChecks
                    .hasRequiredFlowIpcV1_17Types(summary);
            case FLOW_IPC_V1_18_PRODUCTION_OWNER_RESOURCE_SCHEME ->
                TraceSummaryFrontendPipelineRequirementChecks
                        .hasRequiredFlowIpcV1_18ProductionOwnerResourceSchemeTypes(summary);
            case FLOW_IPC_V1_18 -> TraceSummaryFrontendPipelineRequirementChecks
                    .hasRequiredFlowIpcV1_18Types(summary);
            case FLOW_IPC_V1_19_PRODUCTION_OWNER_SIDE_EFFECT ->
                TraceSummaryFrontendPipelineRequirementChecks
                        .hasRequiredFlowIpcV1_19ProductionOwnerSideEffectTypes(summary);
            case FLOW_IPC_V1_19 -> TraceSummaryFrontendPipelineRequirementChecks
                    .hasRequiredFlowIpcV1_19Types(summary);
            case FLOW_IPC_V1_20_PRODUCTION_OWNER_OWNED_RUNTIME ->
                TraceSummaryFrontendPipelineRequirementChecks
                        .hasRequiredFlowIpcV1_20ProductionOwnerOwnedRuntimeTypes(summary);
            case FLOW_IPC_V1_20 -> TraceSummaryFrontendPipelineRequirementChecks
                    .hasRequiredFlowIpcV1_20Types(summary);
            case FLOW_IPC_V1_21_FRONTEND_RESOLVER_CONTRACT ->
                TraceSummaryFrontendPipelineRequirementChecks
                        .hasRequiredFlowIpcV1_21FrontendResolverContractTypes(summary);
            case FLOW_IPC_V1_21 -> TraceSummaryFrontendPipelineRequirementChecks
                    .hasRequiredFlowIpcV1_21Types(summary);
            case FLOW_IPC_V1_22_GENERATED_CHUNK_FIXTURE ->
                TraceSummaryFrontendPipelineRequirementChecks
                        .hasRequiredFlowIpcV1_22GeneratedChunkFixtureTypes(summary);
            case FLOW_IPC_V1_22 -> TraceSummaryFrontendPipelineRequirementChecks
                    .hasRequiredFlowIpcV1_22Types(summary);
            case FLOW_IPC_V1_23_PRODUCTION_OWNER_POSITIVE_FIXTURE ->
                TraceSummaryFrontendPipelineRequirementChecks
                        .hasRequiredFlowIpcV1_23ProductionOwnerPositiveFixtureTypes(summary);
            case FLOW_IPC_V1_23 -> TraceSummaryFrontendPipelineRequirementChecks
                    .hasRequiredFlowIpcV1_23Types(summary);
            case FLOW_IPC_V1_24_FRONTEND_PIPELINE_CLOSURE ->
                TraceSummaryFrontendPipelineRequirementChecks
                        .hasRequiredFlowIpcV1_24FrontendPipelineClosureTypes(summary);
            case FLOW_IPC_V1_24 -> TraceSummaryFrontendPipelineRequirementChecks
                    .hasRequiredFlowIpcV1_24Types(summary);
            case FLOW_IPC_V1_25_PACKAGED_FRONTEND_BASELINE ->
                TraceSummaryFrontendPipelineRequirementChecks
                        .hasRequiredFlowIpcV1_25PackagedFrontendBaselineTypes(summary);
            case FLOW_IPC_V1_25 -> TraceSummaryFrontendPipelineRequirementChecks
                    .hasRequiredFlowIpcV1_25Types(summary);
            case FLOW_IPC_V1_26_GENERATED_LAZY_SOURCE_BOUNDARY ->
                TraceSummaryFrontendBuildRequirementChecks
                        .hasRequiredFlowIpcV1_26GeneratedLazySourceBoundaryTypes(summary);
            case FLOW_IPC_V1_26 -> TraceSummaryFrontendBuildRequirementChecks
                    .hasRequiredFlowIpcV1_26Types(summary);
            case FLOW_IPC_V1_27_GENERATED_LAZY_REAL_BUILD_PROBE ->
                TraceSummaryFrontendBuildRequirementChecks
                        .hasRequiredFlowIpcV1_27GeneratedLazyRealBuildProbeTypes(summary);
            case FLOW_IPC_V1_27 -> TraceSummaryFrontendBuildRequirementChecks
                    .hasRequiredFlowIpcV1_27Types(summary);
            case FLOW_IPC_V1_28_GENERATED_LAZY_BUILD_LIFECYCLE ->
                TraceSummaryFrontendBuildRequirementChecks
                        .hasRequiredFlowIpcV1_28GeneratedLazyBuildLifecycleTypes(summary);
            case FLOW_IPC_V1_28 -> TraceSummaryFrontendBuildRequirementChecks
                    .hasRequiredFlowIpcV1_28Types(summary);
            case FLOW_IPC_V1_29_GENERATED_LAZY_APP_BUILD_OUTPUT ->
                TraceSummaryFrontendBuildRequirementChecks
                        .hasRequiredFlowIpcV1_29GeneratedLazyAppBuildOutputTypes(summary);
            case FLOW_IPC_V1_29 -> TraceSummaryFrontendBuildRequirementChecks
                    .hasRequiredFlowIpcV1_29Types(summary);
            case FLOW_IPC_V1_30_CURRENT_APP_FRONTEND_BUILD_OUTPUT ->
                TraceSummaryFrontendBuildRequirementChecks
                        .hasRequiredFlowIpcV1_30CurrentAppFrontendBuildOutputTypes(summary);
            case FLOW_IPC_V1_30 -> TraceSummaryFrontendBuildRequirementChecks
                    .hasRequiredFlowIpcV1_30Types(summary);
            case FLOW_IPC_V1_31_FRONTEND_BUILD_PREFLIGHT_AVAILABILITY ->
                TraceSummaryFrontendBuildRequirementChecks
                        .hasRequiredFlowIpcV1_31FrontendBuildPreflightAvailabilityTypes(summary);
            case FLOW_IPC_V1_31 -> TraceSummaryFrontendBuildRequirementChecks
                    .hasRequiredFlowIpcV1_31Types(summary);
            case FLOW_IPC_V1_32_BUILD_FRONTEND_OUTPUT ->
                TraceSummaryFrontendBuildRequirementChecks
                        .hasRequiredFlowIpcV1_32BuildFrontendOutputTypes(summary);
            case FLOW_IPC_V1_32 -> TraceSummaryFrontendBuildRequirementChecks
                    .hasRequiredFlowIpcV1_32Types(summary);
            case FLOW_IPC_V1_33_CURRENT_APP_GENERATED_CHUNK_PACKAGE_EDGE ->
                TraceSummaryFrontendBuildRequirementChecks
                        .hasRequiredFlowIpcV1_33CurrentAppGeneratedChunkPackageEdgeTypes(summary);
            case FLOW_IPC_V1_33 -> TraceSummaryFrontendBuildRequirementChecks
                    .hasRequiredFlowIpcV1_33Types(summary);
            case FLOW_IPC_V1_34_CURRENT_APP_GENERATED_IMPORTS_ROOT_PACKAGE_GRAPH ->
                TraceSummaryFrontendBuildRequirementChecks
                        .hasRequiredFlowIpcV1_34CurrentAppGeneratedImportsRootPackageGraphTypes(summary);
            case FLOW_IPC_V1_34 -> TraceSummaryFrontendBuildRequirementChecks
                    .hasRequiredFlowIpcV1_34Types(summary);
            case FLOW_IPC_V1_35_CURRENT_APP_PRODUCTION_RUNTIME_OWNERSHIP ->
                TraceSummaryFrontendBuildRequirementChecks
                        .hasRequiredFlowIpcV1_35CurrentAppProductionRuntimeOwnershipTypes(summary);
            case FLOW_IPC_V1_35 -> TraceSummaryFrontendBuildRequirementChecks
                    .hasRequiredFlowIpcV1_35Types(summary);
            case FLOW_IPC_V1_36_CURRENT_APP_PRODUCTION_OWNER_IMPORT ->
                TraceSummaryFrontendBuildRequirementChecks
                        .hasRequiredFlowIpcV1_36CurrentAppProductionOwnerImportTypes(summary);
            case FLOW_IPC_V1_36 -> TraceSummaryFrontendBuildRequirementChecks
                    .hasRequiredFlowIpcV1_36Types(summary);
            case FLOW_IPC_V1_37_CURRENT_APP_PRODUCTION_OWNER_COMPONENT_REUSE ->
                TraceSummaryFrontendBuildRequirementChecks
                        .hasRequiredFlowIpcV1_37CurrentAppProductionOwnerComponentReuseTypes(summary);
            case FLOW_IPC_V1_37 -> TraceSummaryFrontendBuildRequirementChecks
                    .hasRequiredFlowIpcV1_37Types(summary);
            case FLOW_IPC_V1_38_CURRENT_APP_OWNER_COMPONENT_REUSE_MATRIX ->
                TraceSummaryFrontendBuildRequirementChecks
                        .hasRequiredFlowIpcV1_38CurrentAppOwnerComponentReuseMatrixTypes(summary);
            case FLOW_IPC_V1_38 -> TraceSummaryFrontendBuildRequirementChecks
                    .hasRequiredFlowIpcV1_38Types(summary);
            case FLOW_IPC_V1_39_CURRENT_APP_OWNER_EARLY_IMPORT ->
                TraceSummaryFrontendBuildRequirementChecks
                        .hasRequiredFlowIpcV1_39CurrentAppOwnerEarlyImportTypes(summary);
            case FLOW_IPC_V1_39 -> TraceSummaryFrontendBuildRequirementChecks
                    .hasRequiredFlowIpcV1_39Types(summary);
            case FLOW_IPC_V1_40_CURRENT_APP_GENERATED_IMPORTS_OWNER_SIDE_EFFECT ->
                TraceSummaryFrontendBuildRequirementChecks
                        .hasRequiredFlowIpcV1_40CurrentAppGeneratedImportsOwnerSideEffectTypes(summary);
            case FLOW_IPC_V1_40 -> TraceSummaryFrontendBuildRequirementChecks
                    .hasRequiredFlowIpcV1_40Types(summary);
            case FLOW_IPC_V1_41_CURRENT_APP_GENERATED_IMPORTS_SELECT_OWNER_COVERAGE ->
                TraceSummaryFrontendBuildRequirementChecks
                        .hasRequiredFlowIpcV1_41CurrentAppGeneratedImportsSelectOwnerCoverageTypes(summary);
            case FLOW_IPC_V1_41 -> TraceSummaryFrontendBuildRequirementChecks
                    .hasRequiredFlowIpcV1_41Types(summary);
            case FLOW_IPC_V1_42_GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT ->
                TraceSummaryFrontendBuildRequirementChecks
                        .hasRequiredFlowIpcV1_42GeneratedImportsOwnerRuntimeContractTypes(summary);
            case FLOW_IPC_V1_42 -> TraceSummaryFrontendBuildRequirementChecks
                    .hasRequiredFlowIpcV1_42Types(summary);
            case FLOW_IPC_V1_43_GENERATED_LAZY_SPI_DECISION ->
                TraceSummaryFrontendBuildRequirementChecks
                        .hasRequiredFlowIpcV1_43GeneratedLazySpiDecisionTypes(summary);
            case FLOW_IPC_V1_43 -> TraceSummaryFrontendBuildRequirementChecks
                    .hasRequiredFlowIpcV1_43Types(summary);
            case FLOW_IPC_V1_44_PACKAGING_FRONTEND_INVENTORY ->
                TraceSummaryFrontendBuildRequirementChecks
                        .hasRequiredFlowIpcV1_44PackagingFrontendInventoryTypes(summary);
            case FLOW_IPC_V1_44 -> TraceSummaryFrontendBuildRequirementChecks
                    .hasRequiredFlowIpcV1_44Types(summary);
            case FLOW_IPC_V1_45_PACKAGING_RUNTIME_HANDOFF_REUSE ->
                TraceSummaryFrontendBuildRequirementChecks
                        .hasRequiredFlowIpcV1_45PackagingRuntimeHandoffReuseTypes(summary);
            case FLOW_IPC_V1_45_FRAMEWORK_PACKAGING_RUNTIME_HANDOFF_REUSE ->
                TraceSummaryFrontendBuildRequirementChecks
                        .hasRequiredFlowIpcV1_45FrameworkPackagingRuntimeHandoffReuseTypes(summary);
            case FLOW_IPC_V1_45 -> TraceSummaryFrontendBuildRequirementChecks
                    .hasRequiredFlowIpcV1_45Types(summary);
            default -> throw new IllegalArgumentException("Unsupported frontend build gate: " + gate);
        };
    }
}
