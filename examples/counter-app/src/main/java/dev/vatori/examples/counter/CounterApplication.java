package dev.vatori.examples.counter;

import dev.vatori.desktop.DesktopApplication;
import dev.vatori.desktop.DesktopWindow;
import dev.vatori.desktop.flow.DesktopLauncher;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CounterApplication {
    public static void main(String[] args) {
        DesktopLauncher.launch(CounterApplication.class, DesktopApplication.builder("Counter App")
                .mainWindow(DesktopWindow.builder("main").title("Vaadin Desktop Counter").size(900, 620).build())
                .build(), args);
    }
}

