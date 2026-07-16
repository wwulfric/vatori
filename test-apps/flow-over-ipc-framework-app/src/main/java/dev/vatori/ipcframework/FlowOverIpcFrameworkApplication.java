package dev.vatori.ipcframework;

import dev.vatori.desktop.DesktopApplication;
import dev.vatori.desktop.DesktopWindow;
import dev.vatori.desktop.flow.DesktopLauncher;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FlowOverIpcFrameworkApplication {
    public static void main(String[] args) {
        DesktopLauncher.launch(FlowOverIpcFrameworkApplication.class,
                DesktopApplication.builder("Flow IPC Framework App")
                        .mainWindow(DesktopWindow.builder("main")
                                .title("Flow IPC Framework App")
                                .size(1040, 760)
                                .build())
                        .build(),
                args);
    }
}
