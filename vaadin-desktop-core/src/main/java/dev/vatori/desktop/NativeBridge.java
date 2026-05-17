package dev.vatori.desktop;

public interface NativeBridge {
    DesktopCommandResult invoke(DesktopCommand command);
}

