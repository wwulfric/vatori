package dev.vatori.desktop.devtools;

final class FrameworkRichTextEditorTraceFields {
    boolean matrixV162ManifestObserved;
    boolean matrixV162StageObserved;
    boolean matrixV162CompleteStageObserved;
    boolean matrixV162DomObserved;
    boolean behaviorMatrixV163ManifestObserved;
    boolean behaviorMatrixV163StageObserved;
    boolean behaviorMatrixV163CompleteStageObserved;
    boolean behaviorMatrixV163DomObserved;
    boolean editingMatrixV164ManifestObserved;
    boolean editingMatrixV164StageObserved;
    boolean editingMatrixV164CompleteStageObserved;
    boolean editingMatrixV164DomObserved;
    boolean matrixBootstrapObserved;
    boolean matrixValueEventRpcObserved;
    boolean matrixValuePatchObserved;
    boolean matrixDomObserved;
    boolean behaviorBootstrapObserved;
    boolean behaviorSelectionEventRpcObserved;
    boolean behaviorToolbarEventRpcObserved;
    boolean behaviorKeyboardEventRpcObserved;
    boolean behaviorDeltaEventRpcObserved;
    boolean behaviorSelectionPatchObserved;
    boolean behaviorToolbarPatchObserved;
    boolean behaviorKeyboardPatchObserved;
    boolean behaviorDeltaPatchObserved;
    boolean behaviorDomObserved;
    boolean editingBootstrapObserved;
    boolean editingFormatEventRpcObserved;
    boolean editingClipboardEventRpcObserved;
    boolean editingLinkEventRpcObserved;
    boolean editingFormatPatchObserved;
    boolean editingClipboardPatchObserved;
    boolean editingLinkPatchObserved;
    boolean editingDomObserved;

    boolean hasMatrixSignals() {
        return matrixBootstrapObserved
                && matrixValueEventRpcObserved
                && matrixValuePatchObserved
                && matrixDomObserved;
    }

    boolean hasMatrixGateSignals() {
        return matrixV162ManifestObserved
                && matrixV162StageObserved
                && (matrixV162CompleteStageObserved || matrixV162DomObserved)
                && matrixV162DomObserved;
    }

    boolean hasBehaviorSignals() {
        return behaviorBootstrapObserved
                && behaviorSelectionEventRpcObserved
                && behaviorToolbarEventRpcObserved
                && behaviorKeyboardEventRpcObserved
                && behaviorDeltaEventRpcObserved
                && behaviorSelectionPatchObserved
                && behaviorToolbarPatchObserved
                && behaviorKeyboardPatchObserved
                && behaviorDeltaPatchObserved
                && behaviorDomObserved;
    }

    boolean hasBehaviorGateSignals() {
        return behaviorMatrixV163ManifestObserved
                && behaviorMatrixV163StageObserved
                && (behaviorMatrixV163CompleteStageObserved || behaviorMatrixV163DomObserved)
                && behaviorMatrixV163DomObserved;
    }

    boolean hasEditingSignals() {
        return editingBootstrapObserved
                && editingFormatEventRpcObserved
                && editingClipboardEventRpcObserved
                && editingLinkEventRpcObserved
                && editingFormatPatchObserved
                && editingClipboardPatchObserved
                && editingLinkPatchObserved
                && editingDomObserved;
    }

    boolean hasEditingGateSignals() {
        return editingMatrixV164ManifestObserved
                && editingMatrixV164StageObserved
                && (editingMatrixV164CompleteStageObserved || editingMatrixV164DomObserved)
                && editingMatrixV164DomObserved;
    }
}
