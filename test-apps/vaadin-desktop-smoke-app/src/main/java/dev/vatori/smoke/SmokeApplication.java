package dev.vatori.smoke;

import dev.vatori.desktop.DesktopApplication;
import dev.vatori.desktop.DesktopWindow;
import dev.vatori.desktop.spring.SpringDesktopLauncher;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SmokeApplication {
    public static void main(String[] args) {
        SpringDesktopLauncher.launch(SmokeApplication.class, DesktopApplication.builder("Smoke App")
                .mainWindow(DesktopWindow.builder("main").title("Vaadin Desktop Smoke App").size(1000, 700).build())
                .build(), args);
    }
}
