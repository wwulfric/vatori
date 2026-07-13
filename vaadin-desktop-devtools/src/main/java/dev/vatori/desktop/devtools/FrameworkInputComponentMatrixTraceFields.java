package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class FrameworkInputComponentMatrixTraceFields {
    boolean inputParityV157ManifestObserved;
    boolean inputParityV157StageObserved;
    boolean inputParityV157CompleteStageObserved;
    boolean inputParityV157DomObserved;
    boolean bootstrapObserved;
    boolean decisionEventRpcObserved;
    boolean notesEventRpcObserved;
    boolean emailEventRpcObserved;
    boolean scoreEventRpcObserved;
    boolean secretEventRpcObserved;
    boolean reviewTimeEventRpcObserved;
    boolean decisionPatchObserved;
    boolean notesPatchObserved;
    boolean emailPatchObserved;
    boolean scorePatchObserved;
    boolean secretPatchObserved;
    boolean reviewTimePatchObserved;
    boolean domObserved;

    boolean hasFrameworkSignals() {
        return bootstrapObserved
                && decisionEventRpcObserved
                && notesEventRpcObserved
                && emailEventRpcObserved
                && scoreEventRpcObserved
                && secretEventRpcObserved
                && reviewTimeEventRpcObserved
                && decisionPatchObserved
                && notesPatchObserved
                && emailPatchObserved
                && scorePatchObserved
                && secretPatchObserved
                && reviewTimePatchObserved
                && domObserved;
    }

    boolean hasInputParityGateSignals() {
        return inputParityV157ManifestObserved
                && inputParityV157StageObserved
                && (inputParityV157CompleteStageObserved || inputParityV157DomObserved)
                && inputParityV157DomObserved;
    }

    static boolean containsInputComponentParityMatrixV157Fields(String line) {
        return containsStringField(line, "inputComponentParityMatrixVersion",
                        "flow-ipc-input-component-parity-matrix-v1_57")
                && containsStringField(line, "inputComponentParityMatrixSource",
                        "frontend-resource-pipeline-basic-closure-v1_56")
                && containsStringField(line, "inputComponentParityMatrixScope",
                        "framework-validation-app-common-input-components")
                && containsStringField(line, "inputComponentParityMatrixAppShape",
                        "framework-validation-app")
                && containsStringField(line, "inputComponentParityMatrixTargetTags",
                        "vaadin-radio-group,vaadin-text-area,vaadin-email-field,vaadin-integer-field,vaadin-password-field,vaadin-time-picker")
                && containsStringField(line, "inputComponentParityMatrixJavaClasses",
                        "com.vaadin.flow.component.radiobutton.RadioButtonGroup,com.vaadin.flow.component.textfield.TextArea,com.vaadin.flow.component.textfield.EmailField,com.vaadin.flow.component.textfield.IntegerField,com.vaadin.flow.component.textfield.PasswordField,com.vaadin.flow.component.timepicker.TimePicker")
                && containsStringField(line, "inputComponentParityMatrixComponentIds",
                        "framework-decision,framework-notes,framework-contact-email,framework-score,framework-secret,framework-review-time,framework-input-matrix-status")
                && containsStringField(line, "inputComponentParityMatrixModuleSpecifiers",
                        "@vaadin/radio-group/src/vaadin-radio-group.js,@vaadin/text-area/src/vaadin-text-area.js,@vaadin/email-field/src/vaadin-email-field.js,@vaadin/integer-field/src/vaadin-integer-field.js,@vaadin/password-field/src/vaadin-password-field.js,@vaadin/time-picker/src/vaadin-time-picker.js")
                && containsStringFieldPrefix(line, "inputComponentParityMatrixSelectedArtifacts",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && containsPositiveIntegerField(line, "inputComponentParityMatrixSelectedArtifactCount")
                && containsStringField(line, "inputComponentParityMatrixGeneratedImportsOwnerSource",
                        "current-app-generated-imports-select-owner-coverage-v1_41")
                && containsStringField(line, "inputComponentParityMatrixGeneratedImportsOwnerArtifact",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && containsStringFieldPrefix(line,
                        "inputComponentParityMatrixProductionSupportArtifact",
                        "current-app-production/VAADIN/build/generated-flow-imports-")
                && line.contains("inputComponentParityMatrixProductionSupportSha256")
                && containsPositiveIntegerField(line,
                        "inputComponentParityMatrixProductionSupportBytes")
                && line.contains("inputComponentParityMatrixProductionEntryArtifact")
                && containsStringFieldPrefix(line, "inputComponentParityMatrixSourceChunkArtifact",
                        "current-app-frontend/generated/flow/chunks/chunk-")
                && containsStringField(line, "inputComponentParityMatrixSourceChunkStatus",
                        "available")
                && line.contains("inputComponentParityMatrixResolvedSpecifiers")
                && line.contains("inputComponentParityMatrixUnresolvedSpecifiers")
                && containsStringField(line, "inputComponentParityMatrixAllowedSchemes",
                        "vatori-resource,blob")
                && containsStringField(line, "inputComponentParityMatrixIntegrityPolicy",
                        "sha256-and-byte-count-for-selected-artifacts")
                && containsStringField(line, "inputComponentParityMatrixMavenGoal",
                        "build-frontend")
                && containsStringField(line, "inputComponentParityMatrixBuildStatus", "success")
                && line.contains("inputComponentParityMatrixOutputUsability")
                && containsLiteralField(line, "inputComponentParityMatrixFrameworkAppRequired",
                        "true")
                && containsLiteralField(line, "inputComponentParityMatrixFrameworkAppObserved",
                        "true")
                && containsLiteralField(line, "inputComponentParityMatrixCliContractEnabled",
                        "true")
                && containsLiteralField(line, "inputComponentParityMatrixManifestReady", "true")
                && containsLiteralField(line, "inputComponentParityMatrixRuntimeHandoffRequired",
                        "true")
                && containsLiteralField(line, "inputComponentParityMatrixRuntimeHandoffReady",
                        "true")
                && containsLiteralField(line, "inputComponentParityMatrixNoLocalhost", "true")
                && containsLiteralField(line, "inputComponentParityMatrixFixtureSubstituted",
                        "false")
                && line.contains("inputComponentParityMatrixAcceptableFailures")
                && line.contains("inputComponentParityMatrixRemainingBlockers")
                && line.contains("inputComponentParityMatrixNextResolutionStep");
    }

    static boolean containsInputComponentParityMatrixV157Complete(String line) {
        return containsInputComponentParityMatrixV157Fields(line)
                && containsLiteralField(line,
                        "inputComponentParityMatrixRuntimeHandoffSatisfied", "true")
                && containsLiteralField(line, "inputComponentParityMatrixStaged", "true")
                && containsLiteralField(line,
                        "inputComponentParityMatrixCustomSchemeObserved", "true")
                && containsLiteralField(line,
                        "inputComponentParityMatrixNoLocalhost", "true");
    }
}
