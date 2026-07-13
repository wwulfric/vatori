package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.AcceptanceGate.*;
import static dev.vatori.desktop.devtools.TraceRequirementChain.cumulative;
import static dev.vatori.desktop.devtools.TraceRequirementChain.entry;

final class AuthenticationOverlayBehaviorRequirementChecks {
    private static final TraceRequirementChain OVERLAY_BEHAVIOR_CHAIN = cumulative(
            "authentication overlay behavior trace gate",
            AuthenticationLoginFormCoreRequirementChecks::hasRequiredFlowIpcV1_110Types,
            entry(FLOW_IPC_V1_111_LOGIN_OVERLAY_SHOW_ERROR, FLOW_IPC_V1_111,
                    summary -> summary.authentication.loginOverlayShowErrorMatrixDomObserved),
            entry(FLOW_IPC_V1_112_LOGIN_OVERLAY_I18N_FULL, FLOW_IPC_V1_112,
                    summary -> summary.authentication.loginOverlayFullI18nMatrixDomObserved),
            entry(FLOW_IPC_V1_113_LOGIN_OVERLAY_NO_AUTOFOCUS, FLOW_IPC_V1_113,
                    summary -> summary.authentication.loginOverlayNoAutofocusMatrixDomObserved),
            entry(FLOW_IPC_V1_114_LOGIN_OVERLAY_DISABLED_GUARD, FLOW_IPC_V1_114,
                    summary -> summary.authentication.loginOverlayDisabledGuardMatrixDomObserved),
            entry(FLOW_IPC_V1_115_LOGIN_OVERLAY_DISABLED_KEYBOARD_GUARD, FLOW_IPC_V1_115,
                    summary -> summary.authentication.loginOverlayDisabledKeyboardGuardMatrixDomObserved),
            entry(FLOW_IPC_V1_116_LOGIN_OVERLAY_ACTION_POST, FLOW_IPC_V1_116,
                    summary -> summary.authentication.loginOverlayActionPostMatrixDomObserved),
            entry(FLOW_IPC_V1_117_LOGIN_OVERLAY_SUBMIT_BUTTON, FLOW_IPC_V1_117,
                    summary -> summary.authentication.loginOverlaySubmitButtonMatrixDomObserved),
            entry(FLOW_IPC_V1_118_LOGIN_OVERLAY_ENTER_SUBMIT, FLOW_IPC_V1_118,
                    summary -> summary.authentication.loginOverlayEnterSubmitMatrixDomObserved),
            entry(FLOW_IPC_V1_119_LOGIN_FORM_AUTOFOCUS_CONTROL, FLOW_IPC_V1_119,
                    summary -> summary.authentication.loginFormAutofocusControlMatrixDomObserved),
            entry(FLOW_IPC_V1_120_LOGIN_OVERLAY_AUTOFOCUS_CONTROL, FLOW_IPC_V1_120,
                    summary -> summary.authentication.loginOverlayAutofocusControlMatrixDomObserved),
            entry(FLOW_IPC_V1_121_LOGIN_OVERLAY_ESC_CLOSE, FLOW_IPC_V1_121,
                    summary -> summary.authentication.loginOverlayEscCloseMatrixDomObserved),
            entry(FLOW_IPC_V1_122_LOGIN_OVERLAY_OUTSIDE_CLOSE, FLOW_IPC_V1_122,
                    summary -> summary.authentication.loginOverlayOutsideCloseMatrixDomObserved),
            entry(FLOW_IPC_V1_123_LOGIN_OVERLAY_TAB_TRAVERSAL, FLOW_IPC_V1_123,
                    summary -> summary.authentication.loginOverlayTabTraversalMatrixDomObserved),
            entry(FLOW_IPC_V1_124_LOGIN_OVERLAY_SHIFT_TAB_TRAVERSAL, FLOW_IPC_V1_124,
                    summary -> summary.authentication.loginOverlayShiftTabTraversalMatrixDomObserved),
            entry(FLOW_IPC_V1_125_LOGIN_FORM_TAB_TRAVERSAL, FLOW_IPC_V1_125,
                    summary -> summary.authentication.loginFormTabTraversalMatrixDomObserved),
            entry(FLOW_IPC_V1_126_LOGIN_FORM_SHIFT_TAB_TRAVERSAL, FLOW_IPC_V1_126,
                    summary -> summary.authentication.loginFormShiftTabTraversalMatrixDomObserved),
            entry(FLOW_IPC_V1_127_LOGIN_FORM_AUTOFILL_ATTRIBUTES, FLOW_IPC_V1_127,
                    summary -> summary.authentication.loginFormAutofillAttributesMatrixDomObserved),
            entry(FLOW_IPC_V1_128_LOGIN_OVERLAY_AUTOFILL_ATTRIBUTES, FLOW_IPC_V1_128,
                    summary -> summary.authentication.loginOverlayAutofillAttributesMatrixDomObserved));

    private AuthenticationOverlayBehaviorRequirementChecks() {
    }

    static boolean isSatisfied(AcceptanceGate gate, TraceSummary summary) {
        return OVERLAY_BEHAVIOR_CHAIN.isSatisfied(gate, summary);
    }

    static boolean hasRequiredFlowIpcV1_128Types(TraceSummary summary) {
        return OVERLAY_BEHAVIOR_CHAIN.isSatisfied(FLOW_IPC_V1_128, summary);
    }
}
