package dev.vatori.desktop.devtools;

final class FrameworkEventPatchTraceInspector {
    private FrameworkEventPatchTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (!isUidlEventPatch(line)) {
            return;
        }
        if (line.contains("Budget changed: 73.25")) {
            summary.frameworkApp.budgetPatchObserved = true;
        }
        if (line.contains("Expedite changed: yes")) {
            summary.frameworkApp.checkboxPatchObserved = true;
        }
        if (line.contains("Priority changed: audit")) {
            summary.frameworkApp.selectPatchObserved = true;
        }
        if (line.contains("Revision ")
                && line.contains("priority audit")
                && line.contains("Renderer owned by Flow client engine")) {
            summary.frameworkApp.applyPatchObserved = true;
        }
        if (line.contains("Reviewer changed: desktop")) {
            summary.frameworkComponentMatrix.reviewerPatchObserved = true;
        }
        if (line.contains("Due date changed: 2026-06-16")) {
            summary.frameworkComponentMatrix.dueDatePatchObserved = true;
        }
        if (line.contains("Dialog opened: desktop due 2026-06-16")) {
            summary.frameworkComponentMatrix.dialogPatchObserved = true;
        }
        if (line.contains("Ava Desktop")) {
            summary.frameworkComponentMatrix.dataOwnerPatchObserved = true;
        }
        if (line.contains("Validation accepted: Ava Desktop")) {
            summary.frameworkComponentMatrix.dataValidatePatchObserved = true;
        }
        if (line.contains("Decision changed: approve")) {
            summary.frameworkInputComponentMatrix.decisionPatchObserved = true;
        }
        if (line.contains("Notes changed: Flow IPC notes v1.57")) {
            summary.frameworkInputComponentMatrix.notesPatchObserved = true;
        }
        if (line.contains("Email changed: owner@vatori.dev")) {
            summary.frameworkInputComponentMatrix.emailPatchObserved = true;
        }
        if (line.contains("Score changed: 9")) {
            summary.frameworkInputComponentMatrix.scorePatchObserved = true;
        }
        if (line.contains("Secret changed: length=15")) {
            summary.frameworkInputComponentMatrix.secretPatchObserved = true;
        }
        if (line.contains("Review time changed: 14:45")) {
            summary.frameworkInputComponentMatrix.reviewTimePatchObserved = true;
        }
        if (line.contains("Assignees changed: ava,sol")) {
            summary.frameworkNavigationComponentMatrix.assigneesPatchObserved = true;
        }
        if (line.contains("Tab changed: audit")) {
            summary.frameworkNavigationComponentMatrix.tabsPatchObserved = true;
        }
        if (line.contains("Details opened: true")) {
            summary.frameworkNavigationComponentMatrix.detailsPatchObserved = true;
        }
        if (line.contains("Accordion opened: queue")) {
            summary.frameworkNavigationComponentMatrix.accordionPatchObserved = true;
        }
        if (line.contains("Menu action: audit")) {
            summary.frameworkNavigationComponentMatrix.menuPatchObserved = true;
        }
        if (line.contains("Context menu opened: true")) {
            summary.frameworkOverlayComponentMatrix.contextMenuOpenPatchObserved = true;
        }
        if (line.contains("Context menu action: audit")) {
            summary.frameworkOverlayComponentMatrix.contextMenuItemPatchObserved = true;
        }
        if (line.contains("Grid context menu opened: true")) {
            summary.frameworkOverlayComponentMatrix.gridContextMenuOpenPatchObserved = true;
        }
        if (line.contains("Grid context menu action: audit")) {
            summary.frameworkOverlayComponentMatrix.gridContextMenuItemPatchObserved = true;
        }
        if (line.contains("Popover opened: true")) {
            summary.frameworkOverlayComponentMatrix.popoverOpenPatchObserved = true;
        }
        if (line.contains("Avatar clicked: ava")) {
            summary.frameworkContentAppShellLoginMatrix.avatarPatchObserved = true;
        }
        if (line.contains("Message submitted: Flow IPC message v1.60")) {
            summary.frameworkContentAppShellLoginMatrix.messagePatchObserved = true;
        }
        if (line.contains("Side nav selected: audit")) {
            summary.frameworkContentAppShellLoginMatrix.sideNavPatchObserved = true;
        }
        if (line.contains("Login overlay opened: true")) {
            summary.frameworkContentAppShellLoginMatrix.loginOpenPatchObserved = true;
        }
        if (line.contains("Login submitted: ava")) {
            summary.frameworkContentAppShellLoginMatrix.loginSubmitPatchObserved = true;
        }
        if (line.contains("Lifecycle focus: active")) {
            summary.frameworkLifecycleFocusBehaviorMatrix.focusPatchObserved = true;
        }
        if (line.contains("Lifecycle blur: inactive")) {
            summary.frameworkLifecycleFocusBehaviorMatrix.blurPatchObserved = true;
        }
        if (line.contains("Lifecycle attached: dynamic")) {
            summary.frameworkLifecycleFocusBehaviorMatrix.attachPatchObserved = true;
        }
        if (line.contains("Lifecycle detached: dynamic")) {
            summary.frameworkLifecycleFocusBehaviorMatrix.detachPatchObserved = true;
        }
    }

    private static boolean isUidlEventPatch(String line) {
        return line.contains("\"type\":\"patch\"")
                && line.contains("\"source\":\"UidlRequestHandler\"")
                && line.contains("\"reason\":\"event\"");
    }
}
