package dev.vatori.desktop;

import java.util.Map;

public final class DesktopNotification {
    private final NativeBridge nativeBridge;

    DesktopNotification(NativeBridge nativeBridge) {
        this.nativeBridge = nativeBridge;
    }

    /**
     * Invokes the native notification capability once and returns its original
     * command result. Use {@link #showResult(String, String)} to distinguish
     * manifest denial, operating-system authorization and scheduling outcomes.
     */
    public DesktopCommandResult show(String title, String body) {
        return showResult(title, body).commandResult();
    }

    /**
     * Requests a native notification. A {@code SCHEDULED} result means the
     * operating system accepted the request, not that a banner was displayed
     * or seen by the user.
     */
    public DesktopNotificationResult showResult(String title, String body) {
        return DesktopNotificationResult.from(nativeBridge.invoke(
                new DesktopCommand("notification.show", Map.of(
                        "title", title,
                        "body", body))));
    }
}

