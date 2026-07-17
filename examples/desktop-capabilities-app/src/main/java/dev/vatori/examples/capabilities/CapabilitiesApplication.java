package dev.vatori.examples.capabilities;

import dev.vatori.desktop.DesktopApplication;
import dev.vatori.desktop.DesktopWindow;
import dev.vatori.desktop.flow.DesktopFlowApplication;
import dev.vatori.desktop.flow.DesktopLauncher;

public class CapabilitiesApplication {
    public static void main(String[] args) {
        DesktopLauncher.launch(
                DesktopApplication.builder("Capabilities App")
                        .mainWindow(DesktopWindow.builder("main")
                                .title("Vaadin Desktop Capabilities")
                                .build())
                        .build(),
                DesktopFlowApplication.builder(CapabilitiesView.class).build(),
                args);
    }
}
