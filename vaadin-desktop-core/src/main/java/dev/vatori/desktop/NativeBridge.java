package dev.vatori.desktop;

public interface NativeBridge {
    DesktopCommandResult invoke(DesktopCommand command);

    /**
     * Captures the native capability invocation context that is current for the
     * caller. Scoped Flow bridges capture this while a Flow listener or
     * {@code UI.access} callback is current. Bridges without scoped invocation
     * semantics remain compatible by returning themselves.
     *
     * @return a bridge that preserves the caller's capability context
     */
    default NativeBridge captureCapabilityContext() {
        return this;
    }
}
