package dev.vatori.desktop;

public final class Desktop {
    private Desktop() {
    }

    public static DesktopContext current() {
        return DesktopContextHolder.current();
    }

    public static boolean isAvailable() {
        return DesktopContextHolder.isAvailable();
    }
}

