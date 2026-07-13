package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class FrontendBuildComponentResourceTraceFields {
    boolean broaderMatrixV146ManifestObserved;
    boolean broaderMatrixV146StageObserved;
    boolean broaderMatrixV146CompleteStageObserved;
    boolean broaderMatrixV146DomObserved;
    boolean dataMatrixV147ManifestObserved;
    boolean dataMatrixV147StageObserved;
    boolean dataMatrixV147CompleteStageObserved;
    boolean dataMatrixV147DomObserved;

    boolean hasBroaderMatrixV1_46Signals() {
        return broaderMatrixV146ManifestObserved
                && broaderMatrixV146StageObserved
                && broaderMatrixV146CompleteStageObserved
                && broaderMatrixV146DomObserved;
    }

    boolean hasDataMatrixV1_47Signals() {
        return dataMatrixV147ManifestObserved
                && dataMatrixV147StageObserved
                && dataMatrixV147CompleteStageObserved
                && dataMatrixV147DomObserved;
    }

    static boolean containsBroaderComponentResourceMatrixV146Fields(String line) {
        return containsStringField(line, "broaderComponentResourceMatrixVersion",
                        "flow-ipc-broader-component-resource-matrix-v1_46")
                && containsStringField(line, "broaderComponentResourceMatrixSource",
                        "packaging-runtime-handoff-reuse-v1_45")
                && containsStringField(line, "broaderComponentResourceMatrixScope",
                        "framework-validation-app-combobox-datepicker-dialog")
                && containsStringField(line, "broaderComponentResourceMatrixAppShape",
                        "framework-validation-app")
                && containsStringField(line, "broaderComponentResourceMatrixTargetTags",
                        "vaadin-combo-box,vaadin-date-picker,vaadin-dialog")
                && containsStringField(line, "broaderComponentResourceMatrixJavaClasses",
                        "com.vaadin.flow.component.combobox.ComboBox,com.vaadin.flow.component.datepicker.DatePicker,com.vaadin.flow.component.dialog.Dialog")
                && containsStringField(line, "broaderComponentResourceMatrixComponentIds",
                        "framework-reviewer,framework-due-date,framework-dialog-open,framework-dialog")
                && containsStringField(line, "broaderComponentResourceMatrixModuleSpecifiers",
                        "@vaadin/combo-box/src/vaadin-combo-box.js,@vaadin/date-picker/src/vaadin-date-picker.js,@vaadin/dialog/src/vaadin-dialog.js")
                && containsStringFieldPrefix(line, "broaderComponentResourceMatrixSelectedArtifacts",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && containsPositiveIntegerField(line, "broaderComponentResourceMatrixSelectedArtifactCount")
                && containsStringField(line, "broaderComponentResourceMatrixGeneratedImportsOwnerSource",
                        "current-app-generated-imports-select-owner-coverage-v1_41")
                && containsStringField(line, "broaderComponentResourceMatrixGeneratedImportsOwnerArtifact",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && containsStringFieldPrefix(line,
                        "broaderComponentResourceMatrixProductionSupportArtifact",
                        "current-app-production/VAADIN/build/generated-flow-imports-")
                && line.contains("broaderComponentResourceMatrixProductionSupportSha256")
                && containsPositiveIntegerField(line,
                        "broaderComponentResourceMatrixProductionSupportBytes")
                && line.contains("broaderComponentResourceMatrixProductionEntryArtifact")
                && containsStringFieldPrefix(line, "broaderComponentResourceMatrixSourceChunkArtifact",
                        "current-app-frontend/generated/flow/chunks/chunk-")
                && containsStringField(line, "broaderComponentResourceMatrixSourceChunkStatus",
                        "available")
                && line.contains("broaderComponentResourceMatrixResolvedSpecifiers")
                && line.contains("broaderComponentResourceMatrixUnresolvedSpecifiers")
                && containsStringField(line, "broaderComponentResourceMatrixAllowedSchemes",
                        "vatori-resource,blob")
                && containsStringField(line, "broaderComponentResourceMatrixIntegrityPolicy",
                        "sha256-and-byte-count-for-selected-artifacts")
                && containsStringField(line, "broaderComponentResourceMatrixMavenGoal",
                        "build-frontend")
                && containsStringField(line, "broaderComponentResourceMatrixBuildStatus", "success")
                && line.contains("broaderComponentResourceMatrixOutputUsability")
                && containsLiteralField(line, "broaderComponentResourceMatrixFrameworkAppRequired",
                        "true")
                && containsLiteralField(line, "broaderComponentResourceMatrixFrameworkAppObserved",
                        "true")
                && containsLiteralField(line, "broaderComponentResourceMatrixCliContractEnabled",
                        "true")
                && containsLiteralField(line, "broaderComponentResourceMatrixManifestReady", "true")
                && containsLiteralField(line, "broaderComponentResourceMatrixRuntimeHandoffRequired",
                        "true")
                && containsLiteralField(line, "broaderComponentResourceMatrixRuntimeHandoffReady",
                        "true")
                && containsLiteralField(line, "broaderComponentResourceMatrixNoLocalhost", "true")
                && containsLiteralField(line, "broaderComponentResourceMatrixFixtureSubstituted",
                        "false")
                && line.contains("broaderComponentResourceMatrixAcceptableFailures")
                && line.contains("broaderComponentResourceMatrixRemainingBlockers")
                && line.contains("broaderComponentResourceMatrixNextResolutionStep");
    }

    static boolean containsBroaderComponentResourceMatrixV146Complete(String line) {
        return containsBroaderComponentResourceMatrixV146Fields(line)
                && containsLiteralField(line,
                        "broaderComponentResourceMatrixRuntimeHandoffSatisfied", "true")
                && containsLiteralField(line, "broaderComponentResourceMatrixStaged", "true")
                && containsLiteralField(line,
                        "broaderComponentResourceMatrixCustomSchemeObserved", "true")
                && containsLiteralField(line,
                        "broaderComponentResourceMatrixNoLocalhost", "true");
    }

    static boolean containsDataComponentResourceMatrixV147Fields(String line) {
        return containsStringField(line, "dataComponentResourceMatrixVersion",
                        "flow-ipc-data-component-resource-matrix-v1_47")
                && containsStringField(line, "dataComponentResourceMatrixSource",
                        "broader-component-resource-matrix-v1_46")
                && containsStringField(line, "dataComponentResourceMatrixScope",
                        "framework-validation-app-grid-virtual-list-form-binder")
                && containsStringField(line, "dataComponentResourceMatrixAppShape",
                        "framework-validation-app")
                && containsStringField(line, "dataComponentResourceMatrixTargetTags",
                        "vaadin-grid,vaadin-grid-column,vaadin-virtual-list,vaadin-form-layout,vaadin-text-field")
                && containsStringField(line, "dataComponentResourceMatrixJavaClasses",
                        "com.vaadin.flow.component.grid.Grid,com.vaadin.flow.component.virtuallist.VirtualList,com.vaadin.flow.component.formlayout.FormLayout,com.vaadin.flow.data.binder.Binder,com.vaadin.flow.component.textfield.TextField")
                && containsStringField(line, "dataComponentResourceMatrixComponentIds",
                        "framework-grid,framework-virtual-list,framework-form-layout,framework-owner-name,framework-validation-action,framework-validation-status")
                && containsStringField(line, "dataComponentResourceMatrixModuleSpecifiers",
                        "@vaadin/grid/src/vaadin-grid.js,@vaadin/virtual-list/src/vaadin-virtual-list.js,@vaadin/form-layout/src/vaadin-form-layout.js,@vaadin/text-field/src/vaadin-text-field.js")
                && containsStringFieldPrefix(line, "dataComponentResourceMatrixSelectedArtifacts",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && containsPositiveIntegerField(line, "dataComponentResourceMatrixSelectedArtifactCount")
                && containsStringField(line, "dataComponentResourceMatrixGeneratedImportsOwnerSource",
                        "current-app-generated-imports-select-owner-coverage-v1_41")
                && containsStringField(line, "dataComponentResourceMatrixGeneratedImportsOwnerArtifact",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && containsStringFieldPrefix(line,
                        "dataComponentResourceMatrixProductionSupportArtifact",
                        "current-app-production/VAADIN/build/generated-flow-imports-")
                && line.contains("dataComponentResourceMatrixProductionSupportSha256")
                && containsPositiveIntegerField(line,
                        "dataComponentResourceMatrixProductionSupportBytes")
                && line.contains("dataComponentResourceMatrixProductionEntryArtifact")
                && containsStringFieldPrefix(line, "dataComponentResourceMatrixSourceChunkArtifact",
                        "current-app-frontend/generated/flow/chunks/chunk-")
                && containsStringField(line, "dataComponentResourceMatrixSourceChunkStatus",
                        "available")
                && line.contains("dataComponentResourceMatrixResolvedSpecifiers")
                && line.contains("dataComponentResourceMatrixUnresolvedSpecifiers")
                && containsStringField(line, "dataComponentResourceMatrixAllowedSchemes",
                        "vatori-resource,blob")
                && containsStringField(line, "dataComponentResourceMatrixIntegrityPolicy",
                        "sha256-and-byte-count-for-selected-artifacts")
                && containsStringField(line, "dataComponentResourceMatrixMavenGoal",
                        "build-frontend")
                && containsStringField(line, "dataComponentResourceMatrixBuildStatus", "success")
                && line.contains("dataComponentResourceMatrixOutputUsability")
                && containsLiteralField(line, "dataComponentResourceMatrixFrameworkAppRequired",
                        "true")
                && containsLiteralField(line, "dataComponentResourceMatrixFrameworkAppObserved",
                        "true")
                && containsLiteralField(line, "dataComponentResourceMatrixCliContractEnabled",
                        "true")
                && containsLiteralField(line, "dataComponentResourceMatrixManifestReady", "true")
                && containsLiteralField(line, "dataComponentResourceMatrixRuntimeHandoffRequired",
                        "true")
                && containsLiteralField(line, "dataComponentResourceMatrixRuntimeHandoffReady",
                        "true")
                && containsLiteralField(line, "dataComponentResourceMatrixNoLocalhost", "true")
                && containsLiteralField(line, "dataComponentResourceMatrixFixtureSubstituted",
                        "false")
                && line.contains("dataComponentResourceMatrixAcceptableFailures")
                && line.contains("dataComponentResourceMatrixRemainingBlockers")
                && line.contains("dataComponentResourceMatrixNextResolutionStep");
    }

    static boolean containsDataComponentResourceMatrixV147Complete(String line) {
        return containsDataComponentResourceMatrixV147Fields(line)
                && containsLiteralField(line,
                        "dataComponentResourceMatrixRuntimeHandoffSatisfied", "true")
                && containsLiteralField(line, "dataComponentResourceMatrixStaged", "true")
                && containsLiteralField(line,
                        "dataComponentResourceMatrixCustomSchemeObserved", "true")
                && containsLiteralField(line,
                        "dataComponentResourceMatrixNoLocalhost", "true");
    }
}
