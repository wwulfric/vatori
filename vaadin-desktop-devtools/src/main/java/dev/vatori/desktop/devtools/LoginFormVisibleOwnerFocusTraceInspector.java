package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class LoginFormVisibleOwnerFocusTraceInspector {
    private LoginFormVisibleOwnerFocusTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        boolean loginFormErrorFocusRetryTargetVisibleOwnerFlowClient =
                containsStringField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerMatrixSelectedOwner",
                        "flow-client-engine")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerMatrixSelectedInsideFlowClientRoot",
                        "true");
        boolean loginFormErrorFocusRetryTargetVisibleOwnerDocumentVisible =
                containsStringField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerMatrixSelectedOwner",
                        "document-visible")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerMatrixSelectedInsideFlowClientRoot",
                        "false");
        if (containsLiteralField(line, "frameworkAppRendered", "true")
                && containsLiteralField(line, "frameworkAppAutoUpdateDone", "true")
                && containsLiteralField(line, "flowClientEngineVisibleOwner", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerMatrixComplete", "true")
                && containsStringField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerMatrixVersion",
                        "flow-ipc-login-form-error-focus-retry-target-visible-owner-v1_143")
                && containsLiteralField(line, "loginFormErrorFocusMatrixComplete", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerMatrixLoginFormObserved", "true")
                && (loginFormErrorFocusRetryTargetVisibleOwnerFlowClient
                        || loginFormErrorFocusRetryTargetVisibleOwnerDocumentVisible)
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerMatrixSelectedInsideHiddenAppRoot", "false")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerMatrixUsernameControlObserved", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerMatrixUsernameControlRectObserved", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerMatrixUsernameControlHiddenMarkerObserved", "false")
                && containsStringField(line,
                        "loginFormErrorFocusRetryTargetHiddenSourceMatrixHiddenSourceClassification",
                        "retry-target-hidden-source-visible-owner-no-hidden-marker")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginFormErrorFocusRetryTargetVisibleOwnerMatrixDomObserved = true;
        }
        if (containsLiteralField(line, "frameworkAppRendered", "true")
                && containsLiteralField(line, "frameworkAppAutoUpdateDone", "true")
                && containsLiteralField(line, "flowClientEngineVisibleOwner", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerFocusMatrixComplete", "true")
                && containsStringField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerFocusMatrixVersion",
                        "flow-ipc-login-form-error-focus-retry-target-visible-owner-focus-v1_144")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerFocusMatrixVisibleOwnerComplete",
                        "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerMatrixComplete", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerFocusMatrixUsernameControlObserved",
                        "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerFocusMatrixUsernameControlRectObserved",
                        "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerFocusMatrixActiveElementObserved",
                        "true")
                && containsStringField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerFocusMatrixActiveElementTagName",
                        "input")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerFocusMatrixUsernameFocusObserved",
                        "true")
                && containsStringField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerFocusMatrixFocusClassification",
                        "visible-owner-focus-username-observed")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerFocusMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerFocusMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginFormErrorFocusRetryTargetVisibleOwnerFocusMatrixDomObserved = true;
        }
        if (containsLiteralField(line, "frameworkAppRendered", "true")
                && containsLiteralField(line, "frameworkAppAutoUpdateDone", "true")
                && containsLiteralField(line, "flowClientEngineVisibleOwner", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentMatrixComplete", "true")
                && containsStringField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentMatrixVersion",
                        "flow-ipc-login-form-error-focus-retry-target-visible-owner-focus-containment-v1_145")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentMatrixFocusComplete",
                        "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerFocusMatrixComplete", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentMatrixActiveElementObserved",
                        "true")
                && containsStringField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentMatrixActiveElementTagName",
                        "input")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentMatrixActiveElementWithinLoginFormObserved",
                        "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentMatrixUsernameFocusObserved",
                        "true")
                && containsStringField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentMatrixContainmentClassification",
                        "visible-owner-focus-containment-observed")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentMatrixNoLocalhost",
                        "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentMatrixFixtureSubstituted",
                        "false")) {
            summary.authentication.loginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentMatrixDomObserved = true;
        }
        if (containsLiteralField(line, "frameworkAppRendered", "true")
                && containsLiteralField(line, "frameworkAppAutoUpdateDone", "true")
                && containsLiteralField(line, "flowClientEngineVisibleOwner", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentStabilityMatrixComplete",
                        "true")
                && containsStringField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentStabilityMatrixVersion",
                        "flow-ipc-login-form-error-focus-retry-target-visible-owner-focus-containment-stability-v1_146")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentStabilityMatrixContainmentComplete",
                        "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentMatrixComplete",
                        "true")
                && containsPositiveIntegerField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentStabilityMatrixStableObservationCount")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentStabilityMatrixStableObservationThreshold",
                        "2")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentStabilityMatrixStableObservationComplete",
                        "true")
                && containsStringField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentStabilityMatrixStabilityClassification",
                        "visible-owner-focus-containment-stable")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentStabilityMatrixNoLocalhost",
                        "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentStabilityMatrixFixtureSubstituted",
                        "false")) {
            summary.authentication.loginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentStabilityMatrixDomObserved = true;
        }
    }
}
