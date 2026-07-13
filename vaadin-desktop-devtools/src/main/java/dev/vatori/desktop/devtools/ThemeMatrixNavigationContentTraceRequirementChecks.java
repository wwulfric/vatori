package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.AcceptanceGate.*;
import static dev.vatori.desktop.devtools.TraceRequirementChain.cumulative;
import static dev.vatori.desktop.devtools.TraceRequirementChain.entry;

final class ThemeMatrixNavigationContentTraceRequirementChecks {
    private static final TraceRequirementChain NAVIGATION_CONTENT_THEME_CHAIN = cumulative(
            "navigation/content theme matrix trace gate",
            ThemeMatrixInputTraceRequirementChecks::hasRequiredFlowIpcV1_80Types,
            entry(FLOW_IPC_V1_81_TABS_THEME_VARIANT_MATRIX, FLOW_IPC_V1_81,
                    summary -> summary.themeMatrix.tabsDomObserved),
            entry(FLOW_IPC_V1_82_DETAILS_THEME_VARIANT_MATRIX, FLOW_IPC_V1_82,
                    summary -> summary.themeMatrix.detailsDomObserved),
            entry(FLOW_IPC_V1_83_MENU_BAR_THEME_VARIANT_MATRIX, FLOW_IPC_V1_83,
                    summary -> summary.themeMatrix.menuBarDomObserved),
            entry(FLOW_IPC_V1_84_GRID_THEME_VARIANT_MATRIX, FLOW_IPC_V1_84,
                    summary -> summary.themeMatrix.gridDomObserved),
            entry(FLOW_IPC_V1_85_AVATAR_THEME_VARIANT_MATRIX, FLOW_IPC_V1_85,
                    summary -> summary.themeMatrix.avatarDomObserved),
            entry(FLOW_IPC_V1_86_AVATAR_GROUP_THEME_VARIANT_MATRIX, FLOW_IPC_V1_86,
                    summary -> summary.themeMatrix.avatarGroupDomObserved),
            entry(FLOW_IPC_V1_87_MESSAGE_INPUT_THEME_VARIANT_MATRIX, FLOW_IPC_V1_87,
                    summary -> summary.themeMatrix.messageInputDomObserved),
            entry(FLOW_IPC_V1_88_SIDE_NAV_THEME_VARIANT_MATRIX, FLOW_IPC_V1_88,
                    summary -> summary.themeMatrix.sideNavDomObserved),
            entry(FLOW_IPC_V1_89_MESSAGE_LIST_MARKDOWN_MATRIX, FLOW_IPC_V1_89,
                    summary -> summary.themeMatrix.messageListMarkdownDomObserved));

    private ThemeMatrixNavigationContentTraceRequirementChecks() {
    }

    static boolean isSatisfied(AcceptanceGate gate, TraceSummary summary) {
        return NAVIGATION_CONTENT_THEME_CHAIN.isSatisfied(gate, summary);
    }
}
