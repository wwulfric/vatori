package dev.vatori.desktop.devtools;

import java.util.EnumSet;

enum AcceptanceGateFamily {
    CORE {
        @Override
        boolean contains(AcceptanceGate gate) {
            return gate != AcceptanceGate.FLOW_IPC_V1_3_FRAMEWORK
                    && AcceptanceGateMilestones.between(gate, 1, 3);
        }

        @Override
        EnumSet<AcceptanceGate> expand(AcceptanceGate gate) {
            return switch (gate) {
                case FLOW_IPC_V1 -> EnumSet.of(AcceptanceGate.FLOW_IPC_V1);
                case FLOW_IPC_V1_1 -> EnumSet.of(AcceptanceGate.FLOW_IPC_V1,
                        AcceptanceGate.FLOW_IPC_V1_1);
                case FLOW_IPC_V1_2 -> EnumSet.of(AcceptanceGate.FLOW_IPC_V1,
                        AcceptanceGate.FLOW_IPC_V1_1, AcceptanceGate.FLOW_IPC_V1_2);
                case FLOW_IPC_V1_3 -> EnumSet.of(AcceptanceGate.FLOW_IPC_V1,
                        AcceptanceGate.FLOW_IPC_V1_1, AcceptanceGate.FLOW_IPC_V1_2,
                        AcceptanceGate.FLOW_IPC_V1_3);
                default -> throw unsupported(gate);
            };
        }

        @Override
        boolean isSatisfied(AcceptanceGate gate, TraceSummary summary) {
            return switch (gate) {
                case FLOW_IPC_V1 -> TraceSummaryCoreRequirementChecks
                        .hasRequiredFlowIpcTypes(summary);
                case FLOW_IPC_V1_1 -> TraceSummaryCoreRequirementChecks
                        .hasRequiredFlowIpcV1_1Types(summary);
                case FLOW_IPC_V1_2 -> TraceSummaryCoreRequirementChecks
                        .hasRequiredFlowIpcV1_2Types(summary);
                case FLOW_IPC_V1_3 -> TraceSummaryCoreRequirementChecks
                        .hasRequiredFlowIpcV1_3Types(summary);
                default -> throw unsupported(gate);
            };
        }
    },
    FRONTEND_BUILD {
        @Override
        boolean contains(AcceptanceGate gate) {
            return AcceptanceGateMilestones.between(gate, 4, 45);
        }

        @Override
        EnumSet<AcceptanceGate> expand(AcceptanceGate gate) {
            return FrontendBuildAcceptanceGateRequirements.expand(gate);
        }

        @Override
        boolean isSatisfied(AcceptanceGate gate, TraceSummary summary) {
            return FrontendBuildTraceRequirementChecks.isSatisfied(gate, summary);
        }
    },
    COMPONENT_RESOURCE {
        @Override
        boolean contains(AcceptanceGate gate) {
            return AcceptanceGateMilestones.between(gate, 46, 65);
        }

        @Override
        EnumSet<AcceptanceGate> expand(AcceptanceGate gate) {
            return ComponentResourceAcceptanceGateRequirements.expand(gate);
        }

        @Override
        boolean isSatisfied(AcceptanceGate gate, TraceSummary summary) {
            return ComponentResourceTraceRequirementChecks.isSatisfied(gate, summary);
        }
    },
    THEME_MATRIX {
        @Override
        boolean contains(AcceptanceGate gate) {
            return AcceptanceGateMilestones.between(gate, 66, 89);
        }

        @Override
        EnumSet<AcceptanceGate> expand(AcceptanceGate gate) {
            return ThemeMatrixAcceptanceGateRequirements.expand(gate);
        }

        @Override
        boolean isSatisfied(AcceptanceGate gate, TraceSummary summary) {
            return ThemeMatrixTraceRequirementChecks.isSatisfied(gate, summary);
        }
    },
    AUTHENTICATION {
        @Override
        boolean contains(AcceptanceGate gate) {
            return AcceptanceGateMilestones.between(gate, 90, 146);
        }

        @Override
        EnumSet<AcceptanceGate> expand(AcceptanceGate gate) {
            return AuthenticationAcceptanceGateRequirements.expand(gate);
        }

        @Override
        boolean isSatisfied(AcceptanceGate gate, TraceSummary summary) {
            return AuthenticationTraceRequirementChecks.isSatisfied(gate, summary);
        }
    },
    FRAMEWORK {
        @Override
        boolean contains(AcceptanceGate gate) {
            return gate == AcceptanceGate.FLOW_IPC_V1_3_FRAMEWORK;
        }

        @Override
        EnumSet<AcceptanceGate> expand(AcceptanceGate gate) {
            return EnumSet.of(AcceptanceGate.FLOW_IPC_V1_3_FRAMEWORK);
        }

        @Override
        boolean isSatisfied(AcceptanceGate gate, TraceSummary summary) {
            return TraceSummaryFrameworkRequirementChecks
                    .hasRequiredFlowIpcV1_3FrameworkTypes(summary);
        }
    };

    abstract boolean contains(AcceptanceGate gate);

    abstract EnumSet<AcceptanceGate> expand(AcceptanceGate gate);

    abstract boolean isSatisfied(AcceptanceGate gate, TraceSummary summary);

    static AcceptanceGateFamily of(AcceptanceGate gate) {
        for (AcceptanceGateFamily family : values()) {
            if (family.contains(gate)) {
                return family;
            }
        }
        throw new IllegalArgumentException("Unsupported acceptance gate: " + gate);
    }

    static IllegalArgumentException unsupported(AcceptanceGate gate) {
        return new IllegalArgumentException("Unsupported acceptance gate for family: " + gate);
    }

}
