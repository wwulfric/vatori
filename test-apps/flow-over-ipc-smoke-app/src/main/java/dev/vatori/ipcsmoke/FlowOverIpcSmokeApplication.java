package dev.vatori.ipcsmoke;

import dev.vatori.desktop.DesktopApplication;
import dev.vatori.desktop.DesktopWindow;
import dev.vatori.desktop.flow.ipc.FlowIpcLauncher;

public class FlowOverIpcSmokeApplication {
    public static void main(String[] args) {
        FlowIpcLauncher.launch(FlowOverIpcSmokeView.class, DesktopApplication.builder("Flow IPC Smoke App")
                .mainWindow(DesktopWindow.builder("main").title("Vaadin Flow over IPC").size(1000, 720).build())
                .build(), args);
    }
}
