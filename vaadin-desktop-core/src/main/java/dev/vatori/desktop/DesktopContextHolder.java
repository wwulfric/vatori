package dev.vatori.desktop;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class DesktopContextHolder {
    private static final AtomicReference<DesktopContext> CURRENT = new AtomicReference<>();

    private DesktopContextHolder() {
    }

    public static void set(DesktopContext context) {
        CURRENT.set(context);
    }

    public static DesktopContext current() {
        return optional().orElseThrow(() -> new IllegalStateException(
                "DesktopContext is not available. Start the app through DesktopLauncher."));
    }

    public static Optional<DesktopContext> optional() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static boolean isAvailable() {
        return CURRENT.get() != null;
    }

    public static void clear() {
        CURRENT.set(null);
    }
}

