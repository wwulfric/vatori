package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.containsAll;

final class FrameworkEventTraceInspector {
    private FrameworkEventTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (!isFlowClientXhrEvent(line)) {
            return;
        }
        if (line.contains("\"attachExistingElementById\"")
                && line.contains("\"uidlRpc\"")
                && line.contains("\"clientId\":0")) {
            summary.flowClient.xhrAttachObserved = true;
        }
        if (line.contains("\"uidlRpc\"")
                && line.contains("\"csrfToken\"")
                && line.contains("\"securityKey\"")
                && line.contains("\"event\":\"click\"")
                && line.contains("\"clientId\":1")) {
            summary.flowClient.webViewUidlRpcObserved = true;
            summary.flowClient.xhrClickObserved = true;
        }
        if (componentEvent(line, "framework-budget", "change",
                "\"element.value\":\"73.25\"")) {
            summary.frameworkApp.budgetEventRpcObserved = true;
        }
        if (componentEvent(line, "framework-expedite", "change",
                "\"element.checked\":true")) {
            summary.frameworkApp.checkboxEventRpcObserved = true;
        }
        if (componentEvent(line, "framework-priority", "change",
                "\"element.value\":\"audit\"")) {
            summary.frameworkApp.selectEventRpcObserved = true;
        }
        if (componentEvent(line, "framework-apply", "click")) {
            summary.frameworkApp.applyEventRpcObserved = true;
        }
        if (componentEvent(line, "framework-reviewer", "change",
                "\"element.value\":\"desktop\"")) {
            summary.frameworkComponentMatrix.reviewerEventRpcObserved = true;
        }
        if (componentEvent(line, "framework-due-date", "change",
                "\"element.value\":\"2026-06-16\"")) {
            summary.frameworkComponentMatrix.dueDateEventRpcObserved = true;
        }
        if (componentEvent(line, "framework-dialog-open", "click")) {
            summary.frameworkComponentMatrix.dialogEventRpcObserved = true;
        }
        if (componentEvent(line, "framework-owner-name", "change",
                "\"element.value\":\"Ava Desktop\"")) {
            summary.frameworkComponentMatrix.dataOwnerEventRpcObserved = true;
        }
        if (componentEvent(line, "framework-validation-action", "click")) {
            summary.frameworkComponentMatrix.dataValidateEventRpcObserved = true;
        }
        if (componentEvent(line, "framework-decision", "change",
                "\"element.value\":\"approve\"")) {
            summary.frameworkInputComponentMatrix.decisionEventRpcObserved = true;
        }
        if (componentEvent(line, "framework-notes", "change",
                "\"element.value\":\"Flow IPC notes v1.57\"")) {
            summary.frameworkInputComponentMatrix.notesEventRpcObserved = true;
        }
        if (componentEvent(line, "framework-contact-email", "change",
                "\"element.value\":\"owner@vatori.dev\"")) {
            summary.frameworkInputComponentMatrix.emailEventRpcObserved = true;
        }
        if (componentEvent(line, "framework-score", "change",
                "\"element.value\":\"9\"")) {
            summary.frameworkInputComponentMatrix.scoreEventRpcObserved = true;
        }
        if (componentEvent(line, "framework-secret", "change",
                "\"element.value\":\"flow-ipc-secret\"")) {
            summary.frameworkInputComponentMatrix.secretEventRpcObserved = true;
        }
        if (componentEvent(line, "framework-review-time", "change",
                "\"element.value\":\"14:45\"")) {
            summary.frameworkInputComponentMatrix.reviewTimeEventRpcObserved = true;
        }
        if (componentEvent(line, "framework-assignees", "selected-items-changed",
                "\"event.detail.value\":\"ava,sol\"")) {
            summary.frameworkNavigationComponentMatrix.assigneesEventRpcObserved = true;
        }
        if (componentEvent(line, "framework-nav-tabs", "selected-changed",
                "\"element.selected\":1")) {
            summary.frameworkNavigationComponentMatrix.tabsEventRpcObserved = true;
        }
        if (componentEvent(line, "framework-details", "opened-changed",
                "\"element.opened\":true")) {
            summary.frameworkNavigationComponentMatrix.detailsEventRpcObserved = true;
        }
        if (componentEvent(line, "framework-accordion", "opened-changed",
                "\"element.opened\":0")) {
            summary.frameworkNavigationComponentMatrix.accordionEventRpcObserved = true;
        }
        if (componentEvent(line, "framework-menu", "click")) {
            summary.frameworkNavigationComponentMatrix.menuEventRpcObserved = true;
        }
        OverlayRichContentTraceInspector.inspectLine(summary, line);
        if (componentEvent(line, "framework-lifecycle-focus", "focus")) {
            summary.frameworkLifecycleFocusBehaviorMatrix.focusEventRpcObserved = true;
        }
        if (componentEvent(line, "framework-lifecycle-focus", "blur")) {
            summary.frameworkLifecycleFocusBehaviorMatrix.blurEventRpcObserved = true;
        }
        if (componentEvent(line, "framework-lifecycle-toggle", "click")) {
            summary.frameworkLifecycleFocusBehaviorMatrix.attachEventRpcObserved = true;
            summary.frameworkLifecycleFocusBehaviorMatrix.detachEventRpcObserved = true;
        }
        if (componentEvent(line, "framework-lazy-grid", "setViewportRange",
                "\"rpcType\":\"publishedEventHandler\"",
                "\"templateEventMethodName\":\"setViewportRange\"",
                "20",
                "8")) {
            summary.gridResources.lazyPublishedRpcObserved = true;
        }
        if (componentEvent(line, "framework-lazy-grid", "setViewportRange",
                "\"rpcType\":\"publishedEventHandler\"",
                "\"triggerSource\":\"client-scroll-api\"")) {
            summary.gridResources.scrollPublishedRpcObserved = true;
        }
        if (componentEvent(line, "framework-lazy-grid-report", "click")) {
            summary.gridResources.lazyReportEventRpcObserved = true;
            summary.gridResources.scrollReportEventRpcObserved = true;
        }
    }

    private static boolean isFlowClientXhrEvent(String line) {
        return line.contains("\"type\":\"event\"")
                && line.contains("\"dispatch\":\"flow-client-xhr\"");
    }

    private static boolean componentEvent(
            String line, String componentId, String event, String... requiredFragments) {
        return line.contains("\"componentId\":\"" + componentId + "\"")
                && line.contains("\"event\":\"" + event + "\"")
                && containsAll(line, requiredFragments);
    }

}
