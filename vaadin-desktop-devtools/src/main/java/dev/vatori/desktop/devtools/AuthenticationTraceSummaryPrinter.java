package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceSummaryLinePrinter.printLine;

final class AuthenticationTraceSummaryPrinter {
    private AuthenticationTraceSummaryPrinter() {
    }

    static void print(TraceSummary summary) {
        AuthenticationTraceFields authentication = summary.authentication;
        printLine("LoginOverlay property matrix DOM observed",
                authentication.loginOverlayPropertyMatrixDomObserved);
        printLine("LoginOverlay forgot password event matrix DOM observed",
                authentication.loginOverlayForgotPasswordEventMatrixDomObserved);
        printLine("LoginOverlay login event matrix DOM observed", authentication.loginOverlayLoginEventMatrixDomObserved);
        printLine("LoginOverlay invalid login event matrix DOM observed",
                authentication.loginOverlayInvalidLoginEventMatrixDomObserved);
        printLine("LoginOverlay close lifecycle matrix DOM observed",
                authentication.loginOverlayCloseLifecycleMatrixDomObserved);
        printLine("LoginOverlay recovery login matrix DOM observed",
                authentication.loginOverlayRecoveryLoginMatrixDomObserved);
        printLine("LoginOverlay server reopen matrix DOM observed", authentication.loginOverlayServerReopenMatrixDomObserved);
        printLine("LoginOverlay showErrorMessage matrix DOM observed", authentication.loginOverlayShowErrorMatrixDomObserved);
        printLine("LoginOverlay full i18n matrix DOM observed", authentication.loginOverlayFullI18nMatrixDomObserved);
        printLine("LoginOverlay no-autofocus matrix DOM observed", authentication.loginOverlayNoAutofocusMatrixDomObserved);
        printLine("LoginOverlay autofocus control matrix DOM observed",
                authentication.loginOverlayAutofocusControlMatrixDomObserved);
        printLine("LoginOverlay ESC close matrix DOM observed", authentication.loginOverlayEscCloseMatrixDomObserved);
        printLine("LoginOverlay outside close matrix DOM observed", authentication.loginOverlayOutsideCloseMatrixDomObserved);
        printLine("LoginOverlay Tab traversal matrix DOM observed", authentication.loginOverlayTabTraversalMatrixDomObserved);
        printLine("LoginOverlay Shift+Tab traversal matrix DOM observed",
                authentication.loginOverlayShiftTabTraversalMatrixDomObserved);
        printLine("LoginOverlay autofill attributes matrix DOM observed",
                authentication.loginOverlayAutofillAttributesMatrixDomObserved);
        printLine("LoginOverlay modal a11y semantics matrix DOM observed",
                authentication.loginOverlayA11ySemanticsMatrixDomObserved);
        printLine("LoginOverlay error a11y matrix DOM observed", authentication.loginOverlayErrorA11yMatrixDomObserved);
        printLine("LoginOverlay error focus matrix DOM observed", authentication.loginOverlayErrorFocusMatrixDomObserved);
        printLine("LoginOverlay error focus retry matrix DOM observed",
                authentication.loginOverlayErrorFocusRetryMatrixDomObserved);
        printLine("LoginOverlay error focus retry target matrix DOM observed",
                authentication.loginOverlayErrorFocusRetryTargetMatrixDomObserved);
        printLine("LoginOverlay disabled guard matrix DOM observed", authentication.loginOverlayDisabledGuardMatrixDomObserved);
        printLine("LoginOverlay disabled keyboard guard matrix DOM observed",
                authentication.loginOverlayDisabledKeyboardGuardMatrixDomObserved);
        printLine("LoginOverlay action post matrix DOM observed", authentication.loginOverlayActionPostMatrixDomObserved);
        printLine("LoginOverlay submit button matrix DOM observed", authentication.loginOverlaySubmitButtonMatrixDomObserved);
        printLine("LoginOverlay Enter submit matrix DOM observed", authentication.loginOverlayEnterSubmitMatrixDomObserved);
        printLine("LoginForm property matrix DOM observed", authentication.loginFormPropertyMatrixDomObserved);
        printLine("LoginForm login event matrix DOM observed", authentication.loginFormLoginEventMatrixDomObserved);
        printLine("LoginForm invalid login event matrix DOM observed",
                authentication.loginFormInvalidLoginEventMatrixDomObserved);
        printLine("LoginForm forgot password event matrix DOM observed",
                authentication.loginFormForgotPasswordEventMatrixDomObserved);
        printLine("LoginForm action post matrix DOM observed", authentication.loginFormActionPostMatrixDomObserved);
        printLine("LoginForm submit button matrix DOM observed", authentication.loginFormSubmitButtonMatrixDomObserved);
        printLine("LoginForm enter submit matrix DOM observed", authentication.loginFormEnterSubmitMatrixDomObserved);
        printLine("LoginForm recovery login matrix DOM observed", authentication.loginFormRecoveryLoginMatrixDomObserved);
        printLine("LoginForm enabled lifecycle matrix DOM observed",
                authentication.loginFormEnabledLifecycleMatrixDomObserved);
        printLine("LoginForm disabled guard matrix DOM observed", authentication.loginFormDisabledGuardMatrixDomObserved);
        printLine("LoginForm disabled keyboard guard matrix DOM observed",
                authentication.loginFormDisabledKeyboardGuardMatrixDomObserved);
        printLine("LoginForm no-autofocus matrix DOM observed", authentication.loginFormNoAutofocusMatrixDomObserved);
        printLine("LoginForm autofocus control matrix DOM observed", authentication.loginFormAutofocusControlMatrixDomObserved);
        printLine("LoginForm Tab traversal matrix DOM observed", authentication.loginFormTabTraversalMatrixDomObserved);
        printLine("LoginForm Shift+Tab traversal matrix DOM observed",
                authentication.loginFormShiftTabTraversalMatrixDomObserved);
        printLine("LoginForm autofill attributes matrix DOM observed",
                authentication.loginFormAutofillAttributesMatrixDomObserved);
        printLine("LoginForm a11y semantics matrix DOM observed", authentication.loginFormA11ySemanticsMatrixDomObserved);
        printLine("LoginForm full i18n matrix DOM observed", authentication.loginFormFullI18nMatrixDomObserved);
        printLine("LoginForm showErrorMessage matrix DOM observed", authentication.loginFormShowErrorMatrixDomObserved);
        printLine("LoginForm error a11y matrix DOM observed", authentication.loginFormErrorA11yMatrixDomObserved);
        printLine("LoginForm error focus matrix DOM observed", authentication.loginFormErrorFocusMatrixDomObserved);
        printLine("LoginForm error focus retry matrix DOM observed", authentication.loginFormErrorFocusRetryMatrixDomObserved);
        printLine("LoginForm error focus retry target matrix DOM observed",
                authentication.loginFormErrorFocusRetryTargetMatrixDomObserved);
        printLine("LoginForm error focus retry target state matrix DOM observed",
                authentication.loginFormErrorFocusRetryTargetStateMatrixDomObserved);
        printLine("LoginForm error focus retry target layout matrix DOM observed",
                authentication.loginFormErrorFocusRetryTargetLayoutMatrixDomObserved);
        printLine("LoginForm error focus retry target hidden source matrix DOM observed",
                authentication.loginFormErrorFocusRetryTargetHiddenSourceMatrixDomObserved);
        printLine("LoginForm error focus retry target app-root hidden ancestor DOM observed",
                authentication.loginFormErrorFocusRetryTargetAppRootHiddenAncestorMatrixDomObserved);
        printLine("LoginForm error focus retry target visible owner DOM observed",
                authentication.loginFormErrorFocusRetryTargetVisibleOwnerMatrixDomObserved);
        printLine("LoginForm error focus retry target visible owner focus DOM observed",
                authentication.loginFormErrorFocusRetryTargetVisibleOwnerFocusMatrixDomObserved);
        printLine("LoginForm error focus retry target visible owner focus containment DOM observed",
                authentication.loginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentMatrixDomObserved);
        printLine("LoginForm error focus retry target visible owner focus containment stability DOM observed",
                authentication.loginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentStabilityMatrixDomObserved);
    }
}
