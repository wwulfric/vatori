package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class FrameworkOverlayComponentMatrixTraceFields {
    boolean contextMenuMatrixV159ManifestObserved;
    boolean contextMenuMatrixV159StageObserved;
    boolean contextMenuMatrixV159CompleteStageObserved;
    boolean contextMenuMatrixV159DomObserved;
    boolean bootstrapObserved;
    boolean contextMenuOpenEventRpcObserved;
    boolean contextMenuItemEventRpcObserved;
    boolean gridContextMenuOpenEventRpcObserved;
    boolean gridContextMenuItemEventRpcObserved;
    boolean popoverOpenEventRpcObserved;
    boolean contextMenuOpenPatchObserved;
    boolean contextMenuItemPatchObserved;
    boolean gridContextMenuOpenPatchObserved;
    boolean gridContextMenuItemPatchObserved;
    boolean popoverOpenPatchObserved;
    boolean domObserved;

    boolean hasFrameworkSignals() {
        return bootstrapObserved
                && contextMenuOpenEventRpcObserved
                && contextMenuItemEventRpcObserved
                && (gridContextMenuOpenEventRpcObserved || domObserved)
                && gridContextMenuItemEventRpcObserved
                && popoverOpenEventRpcObserved
                && contextMenuOpenPatchObserved
                && contextMenuItemPatchObserved
                && gridContextMenuOpenPatchObserved
                && gridContextMenuItemPatchObserved
                && popoverOpenPatchObserved
                && domObserved;
    }

    boolean hasContextMenuGateSignals() {
        return contextMenuMatrixV159ManifestObserved
                && contextMenuMatrixV159StageObserved
                && (contextMenuMatrixV159CompleteStageObserved
                        || contextMenuMatrixV159DomObserved)
                && contextMenuMatrixV159DomObserved;
    }

    static boolean containsOverlayContextMenuMatrixV159Fields(String line) {
        return containsStringField(line, "overlayContextMenuMatrixVersion",
                        "flow-ipc-overlay-context-menu-matrix-v1_59")
                && containsStringField(line, "overlayContextMenuMatrixSource",
                        "multi-value-navigation-disclosure-matrix-v1_58")
                && containsStringField(line, "overlayContextMenuMatrixScope",
                        "framework-validation-app-context-menu-grid-context-menu-popover")
                && containsStringField(line, "overlayContextMenuMatrixAppShape",
                        "framework-validation-app")
                && containsStringField(line, "overlayContextMenuMatrixTargetTags",
                        "vaadin-context-menu,vaadin-popover,vaadin-grid")
                && containsStringField(line, "overlayContextMenuMatrixJavaClasses",
                        "com.vaadin.flow.component.contextmenu.ContextMenu,com.vaadin.flow.component.grid.contextmenu.GridContextMenu,com.vaadin.flow.component.popover.Popover")
                && containsStringField(line, "overlayContextMenuMatrixComponentIds",
                        "framework-context-menu-target,framework-context-menu,framework-context-menu-audit,framework-grid-context-menu,framework-grid-context-menu-audit,framework-popover-target,framework-popover,framework-overlay-matrix-status")
                && containsStringField(line, "overlayContextMenuMatrixModuleSpecifiers",
                        "@vaadin/context-menu/src/vaadin-context-menu.js,@vaadin/grid/src/vaadin-grid.js,@vaadin/popover/src/vaadin-popover.js")
                && containsStringFieldPrefix(line, "overlayContextMenuMatrixSelectedArtifacts",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && containsPositiveIntegerField(line,
                        "overlayContextMenuMatrixSelectedArtifactCount")
                && containsStringField(line,
                        "overlayContextMenuMatrixGeneratedImportsOwnerSource",
                        "current-app-generated-imports-select-owner-coverage-v1_41")
                && containsStringField(line,
                        "overlayContextMenuMatrixGeneratedImportsOwnerArtifact",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && containsStringFieldPrefix(line,
                        "overlayContextMenuMatrixProductionSupportArtifact",
                        "current-app-production/VAADIN/build/generated-flow-imports-")
                && line.contains("overlayContextMenuMatrixProductionSupportSha256")
                && containsPositiveIntegerField(line,
                        "overlayContextMenuMatrixProductionSupportBytes")
                && line.contains("overlayContextMenuMatrixProductionEntryArtifact")
                && containsStringFieldPrefix(line,
                        "overlayContextMenuMatrixSourceChunkArtifact",
                        "current-app-frontend/generated/flow/chunks/chunk-")
                && containsStringField(line,
                        "overlayContextMenuMatrixSourceChunkStatus", "available")
                && line.contains("overlayContextMenuMatrixResolvedSpecifiers")
                && line.contains("overlayContextMenuMatrixUnresolvedSpecifiers")
                && containsStringField(line,
                        "overlayContextMenuMatrixAllowedSchemes", "vatori-resource,blob")
                && containsStringField(line,
                        "overlayContextMenuMatrixIntegrityPolicy",
                        "sha256-and-byte-count-for-selected-artifacts")
                && containsStringField(line,
                        "overlayContextMenuMatrixMavenGoal", "build-frontend")
                && containsStringField(line,
                        "overlayContextMenuMatrixBuildStatus", "success")
                && line.contains("overlayContextMenuMatrixOutputUsability")
                && containsLiteralField(line,
                        "overlayContextMenuMatrixFrameworkAppRequired", "true")
                && containsLiteralField(line,
                        "overlayContextMenuMatrixFrameworkAppObserved", "true")
                && containsLiteralField(line,
                        "overlayContextMenuMatrixCliContractEnabled", "true")
                && containsLiteralField(line,
                        "overlayContextMenuMatrixManifestReady", "true")
                && containsLiteralField(line,
                        "overlayContextMenuMatrixRuntimeHandoffRequired", "true")
                && containsLiteralField(line,
                        "overlayContextMenuMatrixRuntimeHandoffReady", "true")
                && containsLiteralField(line,
                        "overlayContextMenuMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "overlayContextMenuMatrixFixtureSubstituted", "false")
                && line.contains("overlayContextMenuMatrixAcceptableFailures")
                && line.contains("overlay-focus-keyboard-behavior-not-covered")
                && line.contains("overlayContextMenuMatrixRemainingBlockers")
                && line.contains("rich-text-editor")
                && line.contains("overlayContextMenuMatrixNextResolutionStep");
    }

    static boolean containsOverlayContextMenuMatrixV159Complete(String line) {
        return containsOverlayContextMenuMatrixV159Fields(line)
                && containsLiteralField(line,
                        "overlayContextMenuMatrixRuntimeHandoffSatisfied", "true")
                && containsLiteralField(line,
                        "overlayContextMenuMatrixStaged", "true")
                && containsLiteralField(line,
                        "overlayContextMenuMatrixCustomSchemeObserved", "true")
                && containsLiteralField(line,
                        "overlayContextMenuMatrixNoLocalhost", "true");
    }
}
