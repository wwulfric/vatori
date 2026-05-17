package dev.vatori.examples.capabilities;

import dev.vatori.desktop.DesktopApplication;
import dev.vatori.desktop.DesktopWindow;
import dev.vatori.desktop.flow.DesktopLauncher;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CapabilitiesApplication {
    public static void main(String[] args) {
        DesktopLauncher.launch(CapabilitiesApplication.class, DesktopApplication.builder("Capabilities App")
                .mainWindow(DesktopWindow.builder("main").title("Vaadin Desktop Capabilities").build())
                .build(), args);
    }
}

