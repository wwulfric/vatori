package dev.vatori.examples.counter;

import dev.vatori.desktop.DesktopApplication;
import dev.vatori.desktop.DesktopWindow;
import dev.vatori.desktop.flow.DesktopFlowApplication;
import dev.vatori.desktop.flow.DesktopLauncher;

public class CounterApplication {
    public static void main(String[] args) {
        DesktopLauncher.launch(
                DesktopApplication.builder("Counter App")
                        .mainWindow(DesktopWindow.builder("main")
                                .title("Vaadin Desktop Counter")
                                .size(900, 620)
                                .build())
                        .build(),
                DesktopFlowApplication.builder(
                        CounterView.class, CounterDetailsView.class).build(),
                args);
    }
}
