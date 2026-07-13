package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class FrameworkComponentMatrixDomTraceInspector {
    private FrameworkComponentMatrixDomTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (!isFrameworkDomLine(line)) {
            return;
        }
        if (containsTrueFields(line,
                "frameworkComponentMatrixRendered",
                "frameworkComponentMatrixReviewerRendered",
                "frameworkComponentMatrixDueDateRendered",
                "frameworkComponentMatrixDialogButtonRendered",
                "frameworkComponentMatrixReviewerEventRpcObserved",
                "frameworkComponentMatrixReviewerEventObserved",
                "frameworkComponentMatrixDueDateEventRpcObserved",
                "frameworkComponentMatrixDueDateEventObserved",
                "frameworkComponentMatrixDialogEventRpcObserved",
                "frameworkComponentMatrixDialogEventObserved",
                "frameworkComponentMatrixComplete")
                && containsAll(line,
                        "framework-reviewer",
                        "framework-due-date",
                        "framework-dialog-open",
                        "Dialog opened: desktop due 2026-06-16")) {
            summary.frameworkComponentMatrix.domObserved = true;
        }
        if (containsTrueFields(line,
                "frameworkDataComponentMatrixRendered",
                "frameworkDataComponentMatrixGridRendered",
                "frameworkDataComponentMatrixVirtualListRendered",
                "frameworkDataComponentMatrixFormLayoutRendered",
                "frameworkDataComponentMatrixOwnerFieldRendered",
                "frameworkDataComponentMatrixOwnerEventRpcObserved",
                "frameworkDataComponentMatrixOwnerEventObserved",
                "frameworkDataComponentMatrixValidateEventRpcObserved",
                "frameworkDataComponentMatrixValidateEventObserved",
                "frameworkDataComponentMatrixComplete")
                && containsAll(line,
                        "framework-grid",
                        "framework-virtual-list",
                        "framework-form-layout",
                        "framework-owner-name",
                        "Validation accepted: Ava Desktop")) {
            summary.frameworkComponentMatrix.dataDomObserved = true;
        }
        if (containsTrueFields(line,
                "frameworkInputComponentMatrixRendered",
                "frameworkInputComponentMatrixDecisionRendered",
                "frameworkInputComponentMatrixNotesRendered",
                "frameworkInputComponentMatrixEmailRendered",
                "frameworkInputComponentMatrixScoreRendered",
                "frameworkInputComponentMatrixSecretRendered",
                "frameworkInputComponentMatrixReviewTimeRendered",
                "frameworkInputComponentMatrixDecisionEventRpcObserved",
                "frameworkInputComponentMatrixDecisionEventObserved",
                "frameworkInputComponentMatrixNotesEventRpcObserved",
                "frameworkInputComponentMatrixNotesEventObserved",
                "frameworkInputComponentMatrixEmailEventRpcObserved",
                "frameworkInputComponentMatrixEmailEventObserved",
                "frameworkInputComponentMatrixScoreEventRpcObserved",
                "frameworkInputComponentMatrixScoreEventObserved",
                "frameworkInputComponentMatrixSecretEventRpcObserved",
                "frameworkInputComponentMatrixSecretEventObserved",
                "frameworkInputComponentMatrixReviewTimeEventRpcObserved",
                "frameworkInputComponentMatrixReviewTimeEventObserved",
                "frameworkInputComponentMatrixComplete")
                && containsAll(line,
                        "framework-decision",
                        "framework-notes",
                        "framework-contact-email",
                        "framework-review-time",
                        "Review time changed: 14:45")) {
            summary.frameworkInputComponentMatrix.domObserved = true;
        }
        if (containsTrueFields(line,
                "frameworkNavigationComponentMatrixRendered",
                "frameworkNavigationComponentMatrixAssigneesRendered",
                "frameworkNavigationComponentMatrixTabsRendered",
                "frameworkNavigationComponentMatrixDetailsRendered",
                "frameworkNavigationComponentMatrixAccordionRendered",
                "frameworkNavigationComponentMatrixMenuRendered",
                "frameworkNavigationComponentMatrixAssigneesEventRpcObserved",
                "frameworkNavigationComponentMatrixAssigneesEventObserved",
                "frameworkNavigationComponentMatrixTabsEventRpcObserved",
                "frameworkNavigationComponentMatrixTabsEventObserved",
                "frameworkNavigationComponentMatrixDetailsEventRpcObserved",
                "frameworkNavigationComponentMatrixDetailsEventObserved",
                "frameworkNavigationComponentMatrixAccordionEventRpcObserved",
                "frameworkNavigationComponentMatrixAccordionEventObserved",
                "frameworkNavigationComponentMatrixMenuEventRpcObserved",
                "frameworkNavigationComponentMatrixMenuEventObserved",
                "frameworkNavigationComponentMatrixComplete")
                && containsAll(line,
                        "framework-assignees",
                        "framework-nav-tabs",
                        "framework-details",
                        "framework-menu",
                        "Menu action: audit")) {
            summary.frameworkNavigationComponentMatrix.domObserved = true;
        }
        if (containsTrueFields(line,
                "frameworkOverlayComponentMatrixRendered",
                "frameworkOverlayComponentMatrixContextMenuRendered",
                "frameworkOverlayComponentMatrixGridContextMenuRendered",
                "frameworkOverlayComponentMatrixPopoverRendered",
                "frameworkOverlayComponentMatrixContextMenuOpenEventRpcObserved",
                "frameworkOverlayComponentMatrixContextMenuOpenEventObserved",
                "frameworkOverlayComponentMatrixContextMenuItemEventRpcObserved",
                "frameworkOverlayComponentMatrixContextMenuItemEventObserved",
                "frameworkOverlayComponentMatrixGridContextMenuOpenEventRpcObserved",
                "frameworkOverlayComponentMatrixGridContextMenuOpenEventObserved",
                "frameworkOverlayComponentMatrixGridContextMenuItemEventRpcObserved",
                "frameworkOverlayComponentMatrixGridContextMenuItemEventObserved",
                "frameworkOverlayComponentMatrixPopoverOpenEventRpcObserved",
                "frameworkOverlayComponentMatrixPopoverOpenEventObserved",
                "frameworkOverlayComponentMatrixComplete")
                && containsAll(line,
                        "framework-context-menu",
                        "framework-grid-context-menu",
                        "framework-popover",
                        "Popover opened: true")) {
            summary.frameworkOverlayComponentMatrix.domObserved = true;
        }
        if (containsTrueFields(line,
                "frameworkRichContentComponentMatrixRendered",
                "frameworkRichContentComponentMatrixAvatarRendered",
                "frameworkRichContentComponentMatrixMessageRendered",
                "frameworkRichContentComponentMatrixSideNavRendered",
                "frameworkRichContentComponentMatrixLoginOverlayRendered",
                "frameworkRichContentComponentMatrixAvatarEventRpcObserved",
                "frameworkRichContentComponentMatrixAvatarEventObserved",
                "frameworkRichContentComponentMatrixMessageEventRpcObserved",
                "frameworkRichContentComponentMatrixMessageEventObserved",
                "frameworkRichContentComponentMatrixSideNavEventRpcObserved",
                "frameworkRichContentComponentMatrixSideNavEventObserved",
                "frameworkRichContentComponentMatrixLoginOpenEventRpcObserved",
                "frameworkRichContentComponentMatrixLoginOpenEventObserved",
                "frameworkRichContentComponentMatrixLoginSubmitEventRpcObserved",
                "frameworkRichContentComponentMatrixLoginSubmitEventObserved",
                "frameworkRichContentComponentMatrixComplete")
                && containsAll(line,
                        "framework-avatar",
                        "framework-message-input",
                        "framework-side-nav",
                        "framework-login-overlay",
                        "Login submitted: ava")) {
            summary.frameworkContentAppShellLoginMatrix.domObserved = true;
        }
    }

    private static boolean isFrameworkDomLine(String line) {
        return line.contains("\"type\":\"webview.dom\"")
                && line.contains("Flow IPC Framework Desk");
    }

}
