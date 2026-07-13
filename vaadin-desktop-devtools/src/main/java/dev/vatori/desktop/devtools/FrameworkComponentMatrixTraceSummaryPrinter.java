package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceSummaryLinePrinter.printLine;

final class FrameworkComponentMatrixTraceSummaryPrinter {
    private FrameworkComponentMatrixTraceSummaryPrinter() {
    }

    static void print(TraceSummary summary) {
        FrameworkComponentMatrixTraceFields componentMatrix = summary.frameworkComponentMatrix;
        FrameworkInputComponentMatrixTraceFields inputMatrix =
                summary.frameworkInputComponentMatrix;
        FrameworkNavigationComponentMatrixTraceFields navigationMatrix =
                summary.frameworkNavigationComponentMatrix;
        FrameworkOverlayComponentMatrixTraceFields overlayMatrix =
                summary.frameworkOverlayComponentMatrix;
        FrameworkContentAppShellLoginMatrixTraceFields contentAppShellLoginMatrix =
                summary.frameworkContentAppShellLoginMatrix;
        FrameworkLifecycleFocusBehaviorMatrixTraceFields lifecycleFocusMatrix =
                summary.frameworkLifecycleFocusBehaviorMatrix;
        FrameworkRichTextEditorTraceFields richTextEditor = summary.frameworkRichTextEditor;
        printLine("framework component matrix bootstrap observed",
                componentMatrix.bootstrapObserved);
        printLine("framework component matrix event RPC observed",
                componentMatrix.reviewerEventRpcObserved,
                componentMatrix.dueDateEventRpcObserved,
                componentMatrix.dialogEventRpcObserved);
        printLine("framework component matrix patch observed",
                componentMatrix.reviewerPatchObserved,
                componentMatrix.dueDatePatchObserved,
                componentMatrix.dialogPatchObserved);
        printLine("framework component matrix DOM observed", componentMatrix.domObserved);
        printLine("framework data component matrix bootstrap observed",
                componentMatrix.dataBootstrapObserved);
        printLine("framework data component matrix event RPC observed",
                componentMatrix.dataOwnerEventRpcObserved,
                componentMatrix.dataValidateEventRpcObserved);
        printLine("framework data component matrix patch observed",
                componentMatrix.dataOwnerPatchObserved,
                componentMatrix.dataValidatePatchObserved);
        printLine("framework data component matrix DOM observed", componentMatrix.dataDomObserved);
        printLine("framework input component matrix bootstrap observed",
                inputMatrix.bootstrapObserved);
        printLine("framework input component matrix event RPC observed",
                "decision/notes/email/score/secret/time=",
                inputMatrix.decisionEventRpcObserved,
                inputMatrix.notesEventRpcObserved,
                inputMatrix.emailEventRpcObserved,
                inputMatrix.scoreEventRpcObserved,
                inputMatrix.secretEventRpcObserved,
                inputMatrix.reviewTimeEventRpcObserved);
        printLine("framework input component matrix patch observed",
                "decision/notes/email/score/secret/time=",
                inputMatrix.decisionPatchObserved,
                inputMatrix.notesPatchObserved,
                inputMatrix.emailPatchObserved,
                inputMatrix.scorePatchObserved,
                inputMatrix.secretPatchObserved,
                inputMatrix.reviewTimePatchObserved);
        printLine("framework input component matrix DOM observed", inputMatrix.domObserved);
        printLine("framework navigation component matrix bootstrap observed",
                navigationMatrix.bootstrapObserved);
        printLine("framework navigation component matrix event RPC observed",
                "assignees/tabs/details/accordion/menu=",
                navigationMatrix.assigneesEventRpcObserved,
                navigationMatrix.tabsEventRpcObserved,
                navigationMatrix.detailsEventRpcObserved,
                navigationMatrix.accordionEventRpcObserved,
                navigationMatrix.menuEventRpcObserved);
        printLine("framework navigation component matrix patch observed",
                "assignees/tabs/details/accordion/menu=",
                navigationMatrix.assigneesPatchObserved,
                navigationMatrix.tabsPatchObserved,
                navigationMatrix.detailsPatchObserved,
                navigationMatrix.accordionPatchObserved,
                navigationMatrix.menuPatchObserved);
        printLine("framework navigation component matrix DOM observed",
                navigationMatrix.domObserved);
        printLine("framework overlay component matrix bootstrap observed",
                overlayMatrix.bootstrapObserved);
        printLine("framework overlay component matrix event RPC observed",
                "context-open/context-item/grid-open/grid-item/popover=",
                overlayMatrix.contextMenuOpenEventRpcObserved,
                overlayMatrix.contextMenuItemEventRpcObserved,
                overlayMatrix.gridContextMenuOpenEventRpcObserved,
                overlayMatrix.gridContextMenuItemEventRpcObserved,
                overlayMatrix.popoverOpenEventRpcObserved);
        printLine("framework overlay component matrix patch observed",
                "context-open/context-item/grid-open/grid-item/popover=",
                overlayMatrix.contextMenuOpenPatchObserved,
                overlayMatrix.contextMenuItemPatchObserved,
                overlayMatrix.gridContextMenuOpenPatchObserved,
                overlayMatrix.gridContextMenuItemPatchObserved,
                overlayMatrix.popoverOpenPatchObserved);
        printLine("framework overlay component matrix DOM observed", overlayMatrix.domObserved);
        printLine("framework rich content component matrix bootstrap observed",
                contentAppShellLoginMatrix.bootstrapObserved);
        printLine("framework rich content component matrix event RPC observed",
                "avatar/message/side-nav/login-open/login-submit=",
                contentAppShellLoginMatrix.avatarEventRpcObserved,
                contentAppShellLoginMatrix.messageEventRpcObserved,
                contentAppShellLoginMatrix.sideNavEventRpcObserved,
                contentAppShellLoginMatrix.loginOpenEventRpcObserved,
                contentAppShellLoginMatrix.loginSubmitEventRpcObserved);
        printLine("framework rich content component matrix patch observed",
                "avatar/message/side-nav/login-open/login-submit=",
                contentAppShellLoginMatrix.avatarPatchObserved,
                contentAppShellLoginMatrix.messagePatchObserved,
                contentAppShellLoginMatrix.sideNavPatchObserved,
                contentAppShellLoginMatrix.loginOpenPatchObserved,
                contentAppShellLoginMatrix.loginSubmitPatchObserved);
        printLine("framework rich content component matrix DOM observed",
                contentAppShellLoginMatrix.domObserved);
        printLine("framework rich text editor matrix bootstrap/event/patch/DOM observed",
                richTextEditor.matrixBootstrapObserved,
                richTextEditor.matrixValueEventRpcObserved,
                richTextEditor.matrixValuePatchObserved,
                richTextEditor.matrixDomObserved);
        printLine("framework rich text editor behavior matrix bootstrap observed",
                richTextEditor.behaviorBootstrapObserved);
        printLine("framework rich text editor behavior matrix event RPC observed",
                "selection/toolbar/keyboard/delta=",
                richTextEditor.behaviorSelectionEventRpcObserved,
                richTextEditor.behaviorToolbarEventRpcObserved,
                richTextEditor.behaviorKeyboardEventRpcObserved,
                richTextEditor.behaviorDeltaEventRpcObserved);
        printLine("framework rich text editor behavior matrix patch observed",
                "selection/toolbar/keyboard/delta/DOM=",
                richTextEditor.behaviorSelectionPatchObserved,
                richTextEditor.behaviorToolbarPatchObserved,
                richTextEditor.behaviorKeyboardPatchObserved,
                richTextEditor.behaviorDeltaPatchObserved,
                richTextEditor.behaviorDomObserved);
        printLine("framework lifecycle/focus behavior matrix bootstrap observed",
                lifecycleFocusMatrix.bootstrapObserved);
        printLine("framework lifecycle/focus behavior matrix event RPC observed",
                "focus/blur/attach/detach=",
                lifecycleFocusMatrix.focusEventRpcObserved,
                lifecycleFocusMatrix.blurEventRpcObserved,
                lifecycleFocusMatrix.attachEventRpcObserved,
                lifecycleFocusMatrix.detachEventRpcObserved);
        printLine("framework lifecycle/focus behavior matrix patch observed",
                "focus/blur/attach/detach=",
                lifecycleFocusMatrix.focusPatchObserved,
                lifecycleFocusMatrix.blurPatchObserved,
                lifecycleFocusMatrix.attachPatchObserved,
                lifecycleFocusMatrix.detachPatchObserved);
        printLine("framework lifecycle/focus behavior matrix DOM observed",
                lifecycleFocusMatrix.domObserved);
    }
}
