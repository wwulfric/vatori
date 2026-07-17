package dev.vatori.examples.grid;

import dev.vatori.desktop.DesktopApplication;
import dev.vatori.desktop.DesktopWindow;
import dev.vatori.desktop.flow.DesktopFlowApplication;
import dev.vatori.desktop.flow.DesktopLauncher;

public class GridApplication {
    public static void main(String[] args) {
        DesktopLauncher.launch(
                DesktopApplication.builder("Grid App")
                        .mainWindow(DesktopWindow.builder("main")
                                .title("Vaadin Desktop Grid")
                                .size(1040, 720)
                                .build())
                        .build(),
                DesktopFlowApplication.builder(GridView.class).build(),
                args);
    }
}
