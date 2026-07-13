package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class LoginOverlayAutofillAttributesTraceInspector {
    private LoginOverlayAutofillAttributesTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        boolean loginOverlayAutofillAttributesObserved =
                containsLiteralField(line,
                        "loginOverlayAutofillAttributesMatrixFormObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayAutofillAttributesMatrixUsernameNameObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayAutofillAttributesMatrixPasswordNameObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayAutofillAttributesMatrixUsernameAutocompleteObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayAutofillAttributesMatrixPasswordAutocompleteObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayAutofillAttributesMatrixPasswordTypeObserved", "true")
                && containsStringField(line,
                        "loginOverlayAutofillAttributesMatrixAttributeClassification",
                        "autofill-attributes-observed");
        boolean loginOverlayAutofillAttributesAcceptedBlockerObserved =
                containsLiteralField(line,
                        "loginOverlayAutofillAttributesMatrixAcceptedBlockerObserved", "true")
                && containsStringField(line,
                        "loginOverlayAutofillAttributesMatrixAttributeClassification",
                        "autofill-attributes-incomplete-in-framework-validation-view");
        if (containsLiteralField(line,
                "loginOverlayAutofillAttributesMatrixComplete", "true")
                && containsStringField(line,
                        "loginOverlayAutofillAttributesMatrixVersion",
                        "flow-ipc-login-overlay-autofill-attributes-v1_128")
                && containsLiteralField(line,
                        "loginOverlayAutofillAttributesMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginOverlayAutofillAttributesMatrixOpenTriggerObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayAutofillAttributesMatrixOpenEventSent", "true")
                && containsLiteralField(line,
                        "loginOverlayAutofillAttributesMatrixOpenEventRpcObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayAutofillAttributesMatrixOpenPatchObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayAutofillAttributesMatrixOpenedObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayAutofillAttributesMatrixOpenStatusObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayAutofillAttributesMatrixUsernameControlObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayAutofillAttributesMatrixPasswordControlObserved", "true")
                && (loginOverlayAutofillAttributesObserved
                        || loginOverlayAutofillAttributesAcceptedBlockerObserved)
                && containsLiteralField(line,
                        "loginOverlayAutofillAttributesMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginOverlayAutofillAttributesMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginOverlayAutofillAttributesMatrixDomObserved = true;
        }
    }
}
