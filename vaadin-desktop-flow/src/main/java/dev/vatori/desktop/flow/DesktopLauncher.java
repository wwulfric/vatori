package dev.vatori.desktop.flow;

import dev.vatori.desktop.DesktopApplication;
import dev.vatori.desktop.flow.ipc.SidecarRuntime;
import dev.vatori.desktop.flow.ipc.SidecarTransport;

import java.util.Objects;

/** Starts a Vaadin application as a native-host-owned Java sidecar. */
public final class DesktopLauncher {
    private DesktopLauncher() {
    }

    public static void launch(
            DesktopApplication application,
            DesktopFlowApplication flowApplication,
            String[] args) {
        Objects.requireNonNull(application, "application");
        Objects.requireNonNull(flowApplication, "flowApplication");
        if (SidecarTransport.required() != SidecarTransport.FLOW_IPC) {
            throw new IllegalStateException(
                    "Compatibility HTTP requires vaadin-desktop-spring-boot "
                            + "and SpringDesktopLauncher.");
        }
        SidecarRuntime.runFlow(
                flowApplication.rootRoute(),
                flowApplication.routes(),
                application,
                args,
                flowApplication.instantiatorFactory().orElse(null));
    }
}
