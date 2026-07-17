package dev.vatori.ipcframework;

import dev.vatori.desktop.DesktopApplication;
import dev.vatori.desktop.DesktopWindow;
import dev.vatori.desktop.flow.DesktopFlowApplication;
import dev.vatori.desktop.flow.DesktopLauncher;

public class FlowOverIpcFrameworkApplication {
    public static void main(String[] args) {
        DesktopLauncher.launch(
                DesktopApplication.builder("Flow IPC Framework App")
                        .mainWindow(DesktopWindow.builder("main")
                                .title("Flow IPC Framework App")
                                .size(1040, 760)
                                .build())
                        .build(),
                DesktopFlowApplication.builder(FlowOverIpcFrameworkView.class).build(),
                args);
    }
}
