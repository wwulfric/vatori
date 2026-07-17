package dev.vatori.examples.gallery;

import dev.vatori.desktop.DesktopApplication;
import dev.vatori.desktop.DesktopWindow;
import dev.vatori.desktop.flow.DesktopFlowApplication;
import dev.vatori.desktop.flow.DesktopLauncher;

public class ComponentGalleryApplication {
    public static void main(String[] args) {
        DesktopLauncher.launch(
                DesktopApplication.builder("vatori Component Gallery")
                        .mainWindow(DesktopWindow.builder("main")
                                .title("vatori Component Gallery")
                                .size(1280, 860)
                                .build())
                        .build(),
                DesktopFlowApplication.builder(ComponentGalleryView.class).build(),
                args);
    }
}
