package dev.vatori.ipcframework;

import dev.vatori.desktop.DesktopApplication;
import dev.vatori.desktop.DesktopWindow;
import dev.vatori.desktop.flow.ipc.FlowIpcLauncher;

public class FlowOverIpcFrameworkApplication {
    public static void main(String[] args) {
        FlowIpcLauncher.launch(FlowOverIpcFrameworkView.class,
                DesktopApplication.builder("Flow IPC Framework App")
                        .mainWindow(DesktopWindow.builder("main")
                                .title("Flow IPC Framework App")
                                .size(1040, 760)
                                .build())
                        .build(),
                args);
    }
}
