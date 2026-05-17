package dev.vatori.examples.grid;

import dev.vatori.desktop.DesktopApplication;
import dev.vatori.desktop.DesktopWindow;
import dev.vatori.desktop.flow.DesktopLauncher;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GridApplication {
    public static void main(String[] args) {
        DesktopLauncher.launch(GridApplication.class, DesktopApplication.builder("Grid App")
                .mainWindow(DesktopWindow.builder("main").title("Vaadin Desktop Grid").size(1040, 720).build())
                .build(), args);
    }
}

