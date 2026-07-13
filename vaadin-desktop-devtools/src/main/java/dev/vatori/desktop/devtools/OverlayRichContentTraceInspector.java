package dev.vatori.desktop.devtools;

final class OverlayRichContentTraceInspector {
    private OverlayRichContentTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (!line.contains("\"type\":\"event\"")
                || !line.contains("\"dispatch\":\"flow-client-xhr\"")) {
            return;
        }
        if (line.contains("\"componentId\":\"framework-context-menu\"")
                && ((line.contains("\"event\":\"opened-changed\"")
                        && line.contains("\"element.opened\":true"))
                    || line.contains("\"event\":\"click\"")
                    || line.contains("\"event\":\"vatori-context-menu-open\""))) {
            summary.frameworkOverlayComponentMatrix.contextMenuOpenEventRpcObserved = true;
        }
        if (line.contains("\"componentId\":\"framework-context-menu-audit\"")
                && (line.contains("\"event\":\"click\"")
                    || line.contains("\"event\":\"vatori-context-menu-audit\""))) {
            summary.frameworkOverlayComponentMatrix.contextMenuItemEventRpcObserved = true;
        }
        if (line.contains("\"componentId\":\"framework-grid-context-menu\"")
                && ((line.contains("\"event\":\"opened-changed\"")
                        && line.contains("\"element.opened\":true"))
                    || line.contains("\"event\":\"click\"")
                    || line.contains("\"event\":\"vatori-grid-context-menu-open\""))) {
            summary.frameworkOverlayComponentMatrix.gridContextMenuOpenEventRpcObserved = true;
        }
        if (line.contains("\"componentId\":\"framework-grid-context-menu-audit\"")
                && (line.contains("\"event\":\"click\"")
                    || line.contains("\"event\":\"vatori-grid-context-menu-audit\""))) {
            summary.frameworkOverlayComponentMatrix.gridContextMenuItemEventRpcObserved = true;
        }
        if (line.contains("\"componentId\":\"framework-popover\"")
                && ((line.contains("\"event\":\"opened-changed\"")
                        && line.contains("\"element.opened\":true"))
                    || line.contains("\"event\":\"click\"")
                    || line.contains("\"event\":\"vatori-popover-open\""))) {
            summary.frameworkOverlayComponentMatrix.popoverOpenEventRpcObserved = true;
        }
        if (line.contains("\"componentId\":\"framework-avatar\"")
                && (line.contains("\"event\":\"click\"")
                    || line.contains("\"event\":\"vatori-avatar-click\""))) {
            summary.frameworkContentAppShellLoginMatrix.avatarEventRpcObserved = true;
        }
        if (line.contains("\"componentId\":\"framework-message-input\"")
                && (line.contains("\"event\":\"submit\"")
                    || line.contains("\"event\":\"vatori-message-submit\""))
                && line.contains("\"event.detail.value\":\"Flow IPC message v1.60\"")) {
            summary.frameworkContentAppShellLoginMatrix.messageEventRpcObserved = true;
        }
        if (line.contains("\"componentId\":\"framework-side-nav-audit\"")
                && (line.contains("\"event\":\"click\"")
                    || line.contains("\"event\":\"vatori-side-nav-audit\""))) {
            summary.frameworkContentAppShellLoginMatrix.sideNavEventRpcObserved = true;
        }
        if (line.contains("\"componentId\":\"framework-login-overlay\"")
                && ((line.contains("\"event\":\"opened-changed\"")
                        && line.contains("\"element.opened\":true"))
                    || line.contains("\"event\":\"vatori-login-overlay-open\""))) {
            summary.frameworkContentAppShellLoginMatrix.loginOpenEventRpcObserved = true;
        }
        if (line.contains("\"componentId\":\"framework-login-overlay\"")
                && (line.contains("\"event\":\"login\"")
                    || line.contains("\"event\":\"vatori-login-submit\""))
                && line.contains("\"event.detail.username\":\"ava\"")
                && (line.contains("\"event.detail.password\":\"desktop\"")
                    || line.contains("\"event\":\"vatori-login-submit\""))) {
            summary.frameworkContentAppShellLoginMatrix.loginSubmitEventRpcObserved = true;
        }
    }
}
