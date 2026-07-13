package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class LoginFormFocusA11yTraceInspector {
    private LoginFormFocusA11yTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        boolean loginFormAutofocusControlDefaultFocusObserved =
                containsLiteralField(line,
                        "loginFormAutofocusControlMatrixDefaultFocusObserved", "true");
        boolean loginFormAutofocusControlAcceptedBlockerObserved =
                containsLiteralField(line,
                        "loginFormAutofocusControlMatrixAcceptedBlockerObserved", "true")
                && containsStringField(line,
                        "loginFormAutofocusControlMatrixFocusClassification",
                        "default-focus-not-observed-in-framework-validation-view");
        if (containsLiteralField(line,
                "loginFormAutofocusControlMatrixComplete", "true")
                && containsStringField(line,
                        "loginFormAutofocusControlMatrixVersion",
                        "flow-ipc-login-form-autofocus-control-v1_119")
                && containsLiteralField(line,
                        "loginFormAutofocusControlMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginFormAutofocusControlMatrixUsernameControlObserved", "true")
                && (loginFormAutofocusControlDefaultFocusObserved
                        || loginFormAutofocusControlAcceptedBlockerObserved)
                && containsLiteralField(line,
                        "loginFormAutofocusControlMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginFormAutofocusControlMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginFormAutofocusControlMatrixDomObserved = true;
        }
        boolean loginFormTabTraversalObserved =
                containsLiteralField(line,
                        "loginFormTabTraversalMatrixPasswordFocusObserved", "true")
                && containsStringField(line,
                        "loginFormTabTraversalMatrixFocusClassification",
                        "tab-focus-password-observed");
        boolean loginFormTabTraversalAcceptedBlockerObserved =
                containsLiteralField(line,
                        "loginFormTabTraversalMatrixAcceptedBlockerObserved", "true")
                && containsStringField(line,
                        "loginFormTabTraversalMatrixFocusClassification",
                        "synthetic-tab-did-not-move-focus-to-password-in-framework-validation-view");
        if (containsLiteralField(line,
                "loginFormTabTraversalMatrixComplete", "true")
                && containsStringField(line,
                        "loginFormTabTraversalMatrixVersion",
                        "flow-ipc-login-form-tab-traversal-v1_125")
                && containsLiteralField(line,
                        "loginFormTabTraversalMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginFormTabTraversalMatrixUsernameControlObserved", "true")
                && containsLiteralField(line,
                        "loginFormTabTraversalMatrixPasswordControlObserved", "true")
                && containsLiteralField(line,
                        "loginFormTabTraversalMatrixUsernameFocusAttempted", "true")
                && containsLiteralField(line,
                        "loginFormTabTraversalMatrixTabKeyAttempted", "true")
                && (loginFormTabTraversalObserved
                        || loginFormTabTraversalAcceptedBlockerObserved)
                && containsLiteralField(line,
                        "loginFormTabTraversalMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginFormTabTraversalMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginFormTabTraversalMatrixDomObserved = true;
        }
        boolean loginFormShiftTabTraversalObserved =
                containsLiteralField(line,
                        "loginFormShiftTabTraversalMatrixUsernameFocusObserved", "true")
                && containsStringField(line,
                        "loginFormShiftTabTraversalMatrixFocusClassification",
                        "shift-tab-focus-username-observed");
        boolean loginFormShiftTabTraversalAcceptedBlockerObserved =
                containsLiteralField(line,
                        "loginFormShiftTabTraversalMatrixAcceptedBlockerObserved", "true")
                && containsStringField(line,
                        "loginFormShiftTabTraversalMatrixFocusClassification",
                        "synthetic-shift-tab-did-not-move-focus-to-username-in-framework-validation-view");
        if (containsLiteralField(line,
                "loginFormShiftTabTraversalMatrixComplete", "true")
                && containsStringField(line,
                        "loginFormShiftTabTraversalMatrixVersion",
                        "flow-ipc-login-form-shift-tab-traversal-v1_126")
                && containsLiteralField(line,
                        "loginFormShiftTabTraversalMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginFormShiftTabTraversalMatrixUsernameControlObserved", "true")
                && containsLiteralField(line,
                        "loginFormShiftTabTraversalMatrixPasswordControlObserved", "true")
                && containsLiteralField(line,
                        "loginFormShiftTabTraversalMatrixPasswordFocusAttempted", "true")
                && containsLiteralField(line,
                        "loginFormShiftTabTraversalMatrixShiftTabKeyAttempted", "true")
                && (loginFormShiftTabTraversalObserved
                        || loginFormShiftTabTraversalAcceptedBlockerObserved)
                && containsLiteralField(line,
                        "loginFormShiftTabTraversalMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginFormShiftTabTraversalMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginFormShiftTabTraversalMatrixDomObserved = true;
        }
        boolean loginFormAutofillAttributesObserved =
                containsLiteralField(line,
                        "loginFormAutofillAttributesMatrixFormObserved", "true")
                && containsLiteralField(line,
                        "loginFormAutofillAttributesMatrixUsernameNameObserved", "true")
                && containsLiteralField(line,
                        "loginFormAutofillAttributesMatrixPasswordNameObserved", "true")
                && containsLiteralField(line,
                        "loginFormAutofillAttributesMatrixUsernameAutocompleteObserved", "true")
                && containsLiteralField(line,
                        "loginFormAutofillAttributesMatrixPasswordAutocompleteObserved", "true")
                && containsLiteralField(line,
                        "loginFormAutofillAttributesMatrixPasswordTypeObserved", "true")
                && containsStringField(line,
                        "loginFormAutofillAttributesMatrixAttributeClassification",
                        "autofill-attributes-observed");
        boolean loginFormAutofillAttributesAcceptedBlockerObserved =
                containsLiteralField(line,
                        "loginFormAutofillAttributesMatrixAcceptedBlockerObserved", "true")
                && containsStringField(line,
                        "loginFormAutofillAttributesMatrixAttributeClassification",
                        "autofill-attributes-incomplete-in-framework-validation-view");
        if (containsLiteralField(line,
                "loginFormAutofillAttributesMatrixComplete", "true")
                && containsStringField(line,
                        "loginFormAutofillAttributesMatrixVersion",
                        "flow-ipc-login-form-autofill-attributes-v1_127")
                && containsLiteralField(line,
                        "loginFormAutofillAttributesMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginFormAutofillAttributesMatrixUsernameControlObserved", "true")
                && containsLiteralField(line,
                        "loginFormAutofillAttributesMatrixPasswordControlObserved", "true")
                && (loginFormAutofillAttributesObserved
                        || loginFormAutofillAttributesAcceptedBlockerObserved)
                && containsLiteralField(line,
                        "loginFormAutofillAttributesMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginFormAutofillAttributesMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginFormAutofillAttributesMatrixDomObserved = true;
        }
        boolean loginFormA11ySemanticsObserved =
                containsLiteralField(line,
                        "loginFormA11ySemanticsMatrixFormObserved", "true")
                && containsLiteralField(line,
                        "loginFormA11ySemanticsMatrixTitleTextObserved", "true")
                && containsLiteralField(line,
                        "loginFormA11ySemanticsMatrixUsernameLabelObserved", "true")
                && containsLiteralField(line,
                        "loginFormA11ySemanticsMatrixPasswordLabelObserved", "true")
                && containsLiteralField(line,
                        "loginFormA11ySemanticsMatrixSubmitButtonObserved", "true")
                && containsLiteralField(line,
                        "loginFormA11ySemanticsMatrixForgotPasswordObserved", "true")
                && containsStringField(line,
                        "loginFormA11ySemanticsMatrixA11yClassification",
                        "inline-a11y-semantics-observed");
        boolean loginFormA11ySemanticsAcceptedBlockerObserved =
                containsLiteralField(line,
                        "loginFormA11ySemanticsMatrixAcceptedBlockerObserved", "true")
                && containsStringField(line,
                        "loginFormA11ySemanticsMatrixA11yClassification",
                        "inline-a11y-semantics-incomplete-in-framework-validation-view");
        if (containsLiteralField(line,
                "loginFormA11ySemanticsMatrixComplete", "true")
                && containsStringField(line,
                        "loginFormA11ySemanticsMatrixVersion",
                        "flow-ipc-login-form-a11y-semantics-v1_130")
                && containsLiteralField(line,
                        "loginFormA11ySemanticsMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginFormA11ySemanticsMatrixUsernameControlObserved", "true")
                && containsLiteralField(line,
                        "loginFormA11ySemanticsMatrixPasswordControlObserved", "true")
                && (loginFormA11ySemanticsObserved
                        || loginFormA11ySemanticsAcceptedBlockerObserved)
                && containsLiteralField(line,
                        "loginFormA11ySemanticsMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginFormA11ySemanticsMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginFormA11ySemanticsMatrixDomObserved = true;
        }
    }
}
