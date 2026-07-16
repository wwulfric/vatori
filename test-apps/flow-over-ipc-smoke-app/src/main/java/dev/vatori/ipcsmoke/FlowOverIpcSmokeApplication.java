package dev.vatori.ipcsmoke;

import dev.vatori.desktop.DesktopApplication;
import dev.vatori.desktop.DesktopWindow;
import dev.vatori.desktop.flow.DesktopLauncher;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FlowOverIpcSmokeApplication {
    public static void main(String[] args) {
        DesktopLauncher.launch(FlowOverIpcSmokeApplication.class,
                DesktopApplication.builder("Flow IPC Smoke App")
                .mainWindow(DesktopWindow.builder("main").title("Vaadin Flow over IPC").size(1000, 720).build())
                .build(), args);
    }
}
