package dev.vatori.ipcframework;

import dev.vatori.desktop.DesktopApplication;
import dev.vatori.desktop.DesktopWindow;
import dev.vatori.desktop.flow.DesktopFlowApplication;
import dev.vatori.desktop.flow.DesktopLauncher;

public class FlowOverIpcFrameworkApplication {
    private static final String STARTUP_MODE = "normal";

    public static void main(String[] args) {
        if ("exit-before-ready".equals(STARTUP_MODE)) {
            System.exit(42);
        }
        DesktopLauncher.launch(
                DesktopApplication.builder("Flow IPC Framework App")
                        .mainWindow(DesktopWindow.builder("main")
                                .title("Flow IPC Framework App")
                                .size(1040, 760)
                                .build())
                        .build(),
                DesktopFlowApplication.builder(
                                FlowOverIpcFrameworkView.class,
                                FlowConcurrentMultipartUploadRoute.class,
                                FlowConcurrentUploadRoute.class,
                                FlowCrossSessionMultipartUploadRoute.class,
                                FlowInFlightRequestRoute.class,
                                FlowRequestFanoutRoute.class,
                                FlowDndRoute.class,
                                FlowInputPopupRoute.class,
                                FlowLargeDownloadRoute.class,
                                FlowLargeMultipartUploadRoute.class,
                                FlowLargeUploadRoute.class,
                                FlowMenuPopupRoute.class,
                                FlowOverlayStackRoute.class,
                                JarFrontendResourceRoute.class,
                                LazyFrontendRoute.class,
                                NestedLazyFrontendRoute.class)
                        .multipartConfig(
                                FlowMultipartAcceptanceStorage.config())
                        .build(),
                args);
    }
}
