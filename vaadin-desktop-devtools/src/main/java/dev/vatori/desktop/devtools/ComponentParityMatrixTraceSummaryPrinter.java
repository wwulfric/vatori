package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceSummaryLinePrinter.printLine;

final class ComponentParityMatrixTraceSummaryPrinter {
    private ComponentParityMatrixTraceSummaryPrinter() {
    }

    static void print(TraceSummary summary) {
        FrontendBuildComponentResourceTraceFields componentResources =
                summary.componentResources;
        FrameworkInputComponentMatrixTraceFields inputComponents =
                summary.frameworkInputComponentMatrix;
        FrameworkNavigationComponentMatrixTraceFields navigationComponents =
                summary.frameworkNavigationComponentMatrix;
        FrameworkOverlayComponentMatrixTraceFields overlayComponents =
                summary.frameworkOverlayComponentMatrix;
        FrameworkContentAppShellLoginMatrixTraceFields contentAppShellLogin =
                summary.frameworkContentAppShellLoginMatrix;
        FrameworkRichTextEditorTraceFields richTextEditor =
                summary.frameworkRichTextEditor;
        printLine("Flow IPC v1.46 broader component resource matrix manifest observed",
                componentResources.broaderMatrixV146ManifestObserved);
        printLine("Flow IPC v1.46 broader component resource matrix stage/complete/DOM observed",
                componentResources.broaderMatrixV146StageObserved,
                componentResources.broaderMatrixV146CompleteStageObserved,
                componentResources.broaderMatrixV146DomObserved);
        printLine("Flow IPC v1.47 data component resource matrix manifest observed",
                componentResources.dataMatrixV147ManifestObserved);
        printLine("Flow IPC v1.47 data component resource matrix stage/complete/DOM observed",
                componentResources.dataMatrixV147StageObserved,
                componentResources.dataMatrixV147CompleteStageObserved,
                componentResources.dataMatrixV147DomObserved);
        printLine("Flow IPC v1.57 input component parity matrix manifest observed",
                inputComponents.inputParityV157ManifestObserved);
        printLine("Flow IPC v1.57 input component parity matrix stage/complete/DOM observed",
                inputComponents.inputParityV157StageObserved,
                inputComponents.inputParityV157CompleteStageObserved,
                inputComponents.inputParityV157DomObserved);
        printLine("Flow IPC v1.58 multi-value navigation disclosure matrix manifest observed",
                navigationComponents.multiValueDisclosureV158ManifestObserved);
        printLine("Flow IPC v1.58 multi-value navigation disclosure matrix stage/complete/DOM observed",
                navigationComponents.multiValueDisclosureV158StageObserved,
                navigationComponents.multiValueDisclosureV158CompleteStageObserved,
                navigationComponents.multiValueDisclosureV158DomObserved);
        printLine("Flow IPC v1.59 overlay context menu matrix manifest observed",
                overlayComponents.contextMenuMatrixV159ManifestObserved);
        printLine("Flow IPC v1.59 overlay context menu matrix stage/complete/DOM observed",
                overlayComponents.contextMenuMatrixV159StageObserved,
                overlayComponents.contextMenuMatrixV159CompleteStageObserved,
                overlayComponents.contextMenuMatrixV159DomObserved);
        printLine("Flow IPC v1.60 content app shell login matrix manifest observed",
                contentAppShellLogin.appShellLoginV160ManifestObserved);
        printLine("Flow IPC v1.60 content app shell login matrix stage/complete/DOM observed",
                contentAppShellLogin.appShellLoginV160StageObserved,
                contentAppShellLogin.appShellLoginV160CompleteStageObserved,
                contentAppShellLogin.appShellLoginV160DomObserved);
        printLine("Flow IPC v1.61 lifecycle/focus behavior matrix DOM observed",
                summary.frameworkLifecycleFocusBehaviorMatrix.domObserved);
        printLine("Flow IPC v1.62 RichTextEditor matrix manifest observed",
                richTextEditor.matrixV162ManifestObserved);
        printLine("Flow IPC v1.62 RichTextEditor matrix stage/complete/DOM observed",
                richTextEditor.matrixV162StageObserved,
                richTextEditor.matrixV162CompleteStageObserved,
                richTextEditor.matrixV162DomObserved);
        printLine("Flow IPC v1.63 RichTextEditor behavior matrix manifest observed",
                richTextEditor.behaviorMatrixV163ManifestObserved);
        printLine("Flow IPC v1.63 RichTextEditor behavior matrix stage/complete/DOM observed",
                richTextEditor.behaviorMatrixV163StageObserved,
                richTextEditor.behaviorMatrixV163CompleteStageObserved,
                richTextEditor.behaviorMatrixV163DomObserved);
        printLine("Flow IPC v1.64 RichTextEditor editing matrix manifest observed",
                richTextEditor.editingMatrixV164ManifestObserved);
        printLine("Flow IPC v1.64 RichTextEditor editing matrix stage/complete/DOM observed",
                richTextEditor.editingMatrixV164StageObserved,
                richTextEditor.editingMatrixV164CompleteStageObserved,
                richTextEditor.editingMatrixV164DomObserved);
    }
}
