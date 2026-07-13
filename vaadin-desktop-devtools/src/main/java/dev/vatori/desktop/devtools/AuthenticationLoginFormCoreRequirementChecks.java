package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.AcceptanceGate.*;
import static dev.vatori.desktop.devtools.TraceRequirementChain.cumulative;
import static dev.vatori.desktop.devtools.TraceRequirementChain.entry;

final class AuthenticationLoginFormCoreRequirementChecks {
    private static final TraceRequirementChain LOGIN_FORM_CORE_CHAIN = cumulative(
            "authentication login form core trace gate",
            AuthenticationOverlayCoreRequirementChecks::hasRequiredFlowIpcV1_96Types,
            entry(FLOW_IPC_V1_97_LOGIN_FORM_LOGIN_EVENT, FLOW_IPC_V1_97,
                    summary -> summary.authentication.loginFormLoginEventMatrixDomObserved),
            entry(FLOW_IPC_V1_98_LOGIN_FORM_INVALID_LOGIN_EVENT, FLOW_IPC_V1_98,
                    summary -> summary.authentication.loginFormInvalidLoginEventMatrixDomObserved),
            entry(FLOW_IPC_V1_99_LOGIN_FORM_FORGOT_PASSWORD_EVENT, FLOW_IPC_V1_99,
                    summary -> summary.authentication.loginFormForgotPasswordEventMatrixDomObserved),
            entry(FLOW_IPC_V1_100_LOGIN_FORM_PROPERTY_MATRIX, FLOW_IPC_V1_100,
                    summary -> summary.authentication.loginFormPropertyMatrixDomObserved),
            entry(FLOW_IPC_V1_101_LOGIN_FORM_ACTION_POST, FLOW_IPC_V1_101,
                    summary -> summary.authentication.loginFormActionPostMatrixDomObserved),
            entry(FLOW_IPC_V1_102_LOGIN_FORM_SUBMIT_BUTTON, FLOW_IPC_V1_102,
                    summary -> summary.authentication.loginFormSubmitButtonMatrixDomObserved),
            entry(FLOW_IPC_V1_103_LOGIN_FORM_ENTER_SUBMIT, FLOW_IPC_V1_103,
                    summary -> summary.authentication.loginFormEnterSubmitMatrixDomObserved),
            entry(FLOW_IPC_V1_104_LOGIN_FORM_RECOVERY_LOGIN, FLOW_IPC_V1_104,
                    summary -> summary.authentication.loginFormRecoveryLoginMatrixDomObserved),
            entry(FLOW_IPC_V1_105_LOGIN_FORM_ENABLED_LIFECYCLE, FLOW_IPC_V1_105,
                    summary -> summary.authentication.loginFormEnabledLifecycleMatrixDomObserved),
            entry(FLOW_IPC_V1_106_LOGIN_FORM_DISABLED_GUARD, FLOW_IPC_V1_106,
                    summary -> summary.authentication.loginFormDisabledGuardMatrixDomObserved),
            entry(FLOW_IPC_V1_107_LOGIN_FORM_DISABLED_KEYBOARD_GUARD, FLOW_IPC_V1_107,
                    summary -> summary.authentication.loginFormDisabledKeyboardGuardMatrixDomObserved),
            entry(FLOW_IPC_V1_108_LOGIN_FORM_NO_AUTOFOCUS, FLOW_IPC_V1_108,
                    summary -> summary.authentication.loginFormNoAutofocusMatrixDomObserved),
            entry(FLOW_IPC_V1_109_LOGIN_FORM_I18N_FULL, FLOW_IPC_V1_109,
                    summary -> summary.authentication.loginFormFullI18nMatrixDomObserved),
            entry(FLOW_IPC_V1_110_LOGIN_FORM_SHOW_ERROR, FLOW_IPC_V1_110,
                    summary -> summary.authentication.loginFormShowErrorMatrixDomObserved));

    private AuthenticationLoginFormCoreRequirementChecks() {
    }

    static boolean isSatisfied(AcceptanceGate gate, TraceSummary summary) {
        return LOGIN_FORM_CORE_CHAIN.isSatisfied(gate, summary);
    }

    static boolean hasRequiredFlowIpcV1_110Types(TraceSummary summary) {
        return LOGIN_FORM_CORE_CHAIN.isSatisfied(FLOW_IPC_V1_110, summary);
    }
}
