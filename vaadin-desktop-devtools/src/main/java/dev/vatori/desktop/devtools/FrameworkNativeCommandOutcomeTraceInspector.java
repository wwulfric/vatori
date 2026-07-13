package dev.vatori.desktop.devtools;

final class FrameworkNativeCommandOutcomeTraceInspector {
    private FrameworkNativeCommandOutcomeTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("\"type\":\"command\"")
                && line.contains("\"command\":\"window.setTitle\"")
                && line.contains("Flow IPC Framework App - 1")) {
            summary.frameworkApp.windowTitleCommandObserved = true;
        }
        if (line.contains("\"type\":\"command\"")
                && line.contains("\"command\":\"clipboard.writeText\"")
                && line.contains("Revision ")) {
            summary.frameworkApp.clipboardCommandObserved = true;
        }
        if (line.contains("\"type\":\"command\"")
                && line.contains("\"command\":\"clipboard.readText\"")) {
            summary.frameworkApp.nativeClipboardReadMatrixCommandObserved = true;
        }
        if (line.contains("\"type\":\"command\"")
                && line.contains("\"command\":\"notification.show\"")
                && line.contains("Framework update 1 applied")) {
            summary.frameworkApp.notificationCommandObserved = true;
        }
        if (line.contains("\"type\":\"capabilityResult\"")
                && line.contains("\"command\":\"window.setTitle\"")
                && line.contains("\"permissionDecision\":\"allowed\"")
                && line.contains("\"success\":true")) {
            summary.frameworkApp.windowTitleResultObserved = true;
        }
        if (line.contains("\"type\":\"capabilityResult\"")
                && line.contains("\"command\":\"clipboard.writeText\"")
                && line.contains("\"permissionDecision\":\"allowed\"")
                && line.contains("\"success\":true")) {
            summary.frameworkApp.clipboardResultObserved = true;
        }
        if (line.contains("\"type\":\"capabilityResult\"")
                && line.contains("\"command\":\"clipboard.readText\"")
                && line.contains("\"mode\":\"native-nspasteboard\"")
                && line.contains("\"permissionMode\":\"manifest-enforced\"")
                && line.contains("\"permissionDecision\":\"allowed\"")
                && line.contains("\"success\":true")) {
            summary.frameworkApp.nativeClipboardReadMatrixResultObserved = true;
            summary.frameworkApp.nativeClipboardReadMatrixPermissionObserved = true;
            if (line.contains("Flow IPC native clipboard v1.65")) {
                summary.frameworkApp.nativeClipboardReadMatrixTextObserved = true;
            }
        }
        if (line.contains("\"type\":\"capabilityResult\"")
                && line.contains("\"command\":\"notification.show\"")
                && line.contains("\"permissionDecision\":\"allowed\"")
                && line.contains("\"success\":true")) {
            summary.frameworkApp.notificationResultObserved = true;
        }
    }
}
