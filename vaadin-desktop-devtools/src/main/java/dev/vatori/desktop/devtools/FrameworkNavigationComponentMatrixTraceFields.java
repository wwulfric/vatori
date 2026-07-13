package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class FrameworkNavigationComponentMatrixTraceFields {
    boolean multiValueDisclosureV158ManifestObserved;
    boolean multiValueDisclosureV158StageObserved;
    boolean multiValueDisclosureV158CompleteStageObserved;
    boolean multiValueDisclosureV158DomObserved;
    boolean bootstrapObserved;
    boolean assigneesEventRpcObserved;
    boolean tabsEventRpcObserved;
    boolean detailsEventRpcObserved;
    boolean accordionEventRpcObserved;
    boolean menuEventRpcObserved;
    boolean assigneesPatchObserved;
    boolean tabsPatchObserved;
    boolean detailsPatchObserved;
    boolean accordionPatchObserved;
    boolean menuPatchObserved;
    boolean domObserved;

    boolean hasFrameworkSignals() {
        return bootstrapObserved
                && assigneesEventRpcObserved
                && tabsEventRpcObserved
                && detailsEventRpcObserved
                && accordionEventRpcObserved
                && menuEventRpcObserved
                && assigneesPatchObserved
                && tabsPatchObserved
                && detailsPatchObserved
                && accordionPatchObserved
                && menuPatchObserved
                && domObserved;
    }

    boolean hasMultiValueDisclosureGateSignals() {
        return multiValueDisclosureV158ManifestObserved
                && multiValueDisclosureV158StageObserved
                && (multiValueDisclosureV158CompleteStageObserved
                        || multiValueDisclosureV158DomObserved)
                && multiValueDisclosureV158DomObserved;
    }

    static boolean containsMultiValueNavigationDisclosureMatrixV158Fields(String line) {
        return containsStringField(line, "multiValueNavigationDisclosureMatrixVersion",
                        "flow-ipc-multi-value-navigation-disclosure-matrix-v1_58")
                && containsStringField(line, "multiValueNavigationDisclosureMatrixSource",
                        "input-component-parity-matrix-v1_57")
                && containsStringField(line, "multiValueNavigationDisclosureMatrixScope",
                        "framework-validation-app-multi-value-navigation-disclosure-menu")
                && containsStringField(line, "multiValueNavigationDisclosureMatrixAppShape",
                        "framework-validation-app")
                && containsStringField(line, "multiValueNavigationDisclosureMatrixTargetTags",
                        "vaadin-multi-select-combo-box,vaadin-tabs,vaadin-tab,vaadin-details,vaadin-accordion,vaadin-menu-bar")
                && containsStringField(line, "multiValueNavigationDisclosureMatrixJavaClasses",
                        "com.vaadin.flow.component.combobox.MultiSelectComboBox,com.vaadin.flow.component.tabs.Tabs,com.vaadin.flow.component.tabs.Tab,com.vaadin.flow.component.details.Details,com.vaadin.flow.component.accordion.Accordion,com.vaadin.flow.component.menubar.MenuBar")
                && containsStringField(line, "multiValueNavigationDisclosureMatrixComponentIds",
                        "framework-assignees,framework-nav-tabs,framework-nav-tab-audit,framework-details,framework-accordion,framework-menu,framework-menu-audit,framework-navigation-matrix-status")
                && containsStringField(line,
                        "multiValueNavigationDisclosureMatrixModuleSpecifiers",
                        "@vaadin/multi-select-combo-box/src/vaadin-multi-select-combo-box.js,@vaadin/tabs/src/vaadin-tabs.js,@vaadin/tabs/src/vaadin-tab.js,@vaadin/details/src/vaadin-details.js,@vaadin/accordion/src/vaadin-accordion.js,@vaadin/menu-bar/src/vaadin-menu-bar.js")
                && containsStringFieldPrefix(line,
                        "multiValueNavigationDisclosureMatrixSelectedArtifacts",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && containsPositiveIntegerField(line,
                        "multiValueNavigationDisclosureMatrixSelectedArtifactCount")
                && containsStringField(line,
                        "multiValueNavigationDisclosureMatrixGeneratedImportsOwnerSource",
                        "current-app-generated-imports-select-owner-coverage-v1_41")
                && containsStringField(line,
                        "multiValueNavigationDisclosureMatrixGeneratedImportsOwnerArtifact",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && containsStringFieldPrefix(line,
                        "multiValueNavigationDisclosureMatrixProductionSupportArtifact",
                        "current-app-production/VAADIN/build/generated-flow-imports-")
                && line.contains("multiValueNavigationDisclosureMatrixProductionSupportSha256")
                && containsPositiveIntegerField(line,
                        "multiValueNavigationDisclosureMatrixProductionSupportBytes")
                && line.contains("multiValueNavigationDisclosureMatrixProductionEntryArtifact")
                && containsStringFieldPrefix(line,
                        "multiValueNavigationDisclosureMatrixSourceChunkArtifact",
                        "current-app-frontend/generated/flow/chunks/chunk-")
                && containsStringField(line,
                        "multiValueNavigationDisclosureMatrixSourceChunkStatus", "available")
                && line.contains("multiValueNavigationDisclosureMatrixResolvedSpecifiers")
                && line.contains("multiValueNavigationDisclosureMatrixUnresolvedSpecifiers")
                && containsStringField(line,
                        "multiValueNavigationDisclosureMatrixAllowedSchemes",
                        "vatori-resource,blob")
                && containsStringField(line,
                        "multiValueNavigationDisclosureMatrixIntegrityPolicy",
                        "sha256-and-byte-count-for-selected-artifacts")
                && containsStringField(line,
                        "multiValueNavigationDisclosureMatrixMavenGoal", "build-frontend")
                && containsStringField(line,
                        "multiValueNavigationDisclosureMatrixBuildStatus", "success")
                && line.contains("multiValueNavigationDisclosureMatrixOutputUsability")
                && containsLiteralField(line,
                        "multiValueNavigationDisclosureMatrixFrameworkAppRequired", "true")
                && containsLiteralField(line,
                        "multiValueNavigationDisclosureMatrixFrameworkAppObserved", "true")
                && containsLiteralField(line,
                        "multiValueNavigationDisclosureMatrixCliContractEnabled", "true")
                && containsLiteralField(line,
                        "multiValueNavigationDisclosureMatrixManifestReady", "true")
                && containsLiteralField(line,
                        "multiValueNavigationDisclosureMatrixRuntimeHandoffRequired", "true")
                && containsLiteralField(line,
                        "multiValueNavigationDisclosureMatrixRuntimeHandoffReady", "true")
                && containsLiteralField(line,
                        "multiValueNavigationDisclosureMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "multiValueNavigationDisclosureMatrixFixtureSubstituted", "false")
                && line.contains("multiValueNavigationDisclosureMatrixAcceptableFailures")
                && line.contains("overlay-focus-keyboard-behavior-not-covered")
                && line.contains("multiValueNavigationDisclosureMatrixRemainingBlockers")
                && line.contains("multiValueNavigationDisclosureMatrixNextResolutionStep");
    }

    static boolean containsMultiValueNavigationDisclosureMatrixV158Complete(String line) {
        return containsMultiValueNavigationDisclosureMatrixV158Fields(line)
                && containsLiteralField(line,
                        "multiValueNavigationDisclosureMatrixRuntimeHandoffSatisfied", "true")
                && containsLiteralField(line,
                        "multiValueNavigationDisclosureMatrixStaged", "true")
                && containsLiteralField(line,
                        "multiValueNavigationDisclosureMatrixCustomSchemeObserved", "true")
                && containsLiteralField(line,
                        "multiValueNavigationDisclosureMatrixNoLocalhost", "true");
    }

}
