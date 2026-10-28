package dev.vatori.ipcframework;

import dev.vatori.desktop.DesktopApplication;
import dev.vatori.desktop.DesktopWindow;
import dev.vatori.desktop.flow.DesktopFlowApplication;
import dev.vatori.desktop.flow.DesktopLauncher;

public class FlowOverIpcFrameworkApplication {
    private static final String STARTUP_MODE = "normal";

    public static void main(String[] args) {
        FlowFatalProtocolStartup.initialize();
        FlowHostShutdownTerminationStartup.initialize();
        FlowRecoveryTerminationStartup.initialize();
        if ("exit-before-ready".equals(STARTUP_MODE)) {
            System.exit(42);
        }
        DesktopLauncher.launch(
                DesktopApplication.builder("Flow IPC Framework App")
                        .mainWindow(DesktopWindow.builder("main")
                                .title("Flow IPC Framework App")
                                .size(1040, 760)
                                .route(FlowHostShutdownTerminationStartup.enabled()
                                        ? "flow-host-shutdown-termination"
                                        : FlowRecoveryTerminationStartup.enabled()
                                                ? "flow-recovery-termination" : "")
                                .build())
                        .build(),
                DesktopFlowApplication.builder(
                                FlowOverIpcFrameworkView.class,
                                FlowConcurrentMultipartUploadRoute.class,
                                FlowConcurrentUploadRoute.class,
                                FlowCrossSessionMultipartUploadRoute.class,
                                FlowInFlightRequestRoute.class,
                                FlowRequestFanoutRoute.class,
                                FlowRecoveryTerminationRoute.class,
                                FlowHostShutdownTerminationRoute.class,
                                FlowCapabilityContextReopenRoute.class,
                                FlowCapabilityPermissionRoute.class,
                                FlowClipboardOutcomesRoute.class,
                                FlowNotificationOutcomesRoute.class,
                                FlowNotificationDeliveryRoute.class,
                                FlowDndRoute.class,
                                FlowFileDialogDeniedRoute.class,
                                FlowFileDialogGenerationRoute.class,
                                FlowFileDialogOutcomesRoute.class,
                                FlowFileDialogOwnerWindowCloseRoute.class,
                                FlowFileReadGrantRoute.class,
                                FlowFileReadGrantIsolationRoute.class,
                                FlowFileReadGrantReplayRoute.class,
                                FlowFileReadGrantRangeRoute.class,
                                FlowFileWriteGrantRoute.class,
                                FlowFileWriteGrantIsolationRoute.class,
                                FlowFileWriteGrantRecoveryRoute.class,
                                FlowFileWriteGrantReplayRoute.class,
                                FlowMainWindowCloseRecoveryRoute.class,
                                FlowLastWindowCloseRoute.class,
                                FlowInputPopupRoute.class,
                                FlowLocalizedNumberRoute.class,
                                FlowLocalizedNumberValidationRoute.class,
                                FlowNativeEditRoute.class,
                                FlowExternalFileDropRoute.class,
                                FlowLazyGridSelectionRoute.class,
                                FlowLazyMultiSelectComboBoxRoute.class,
                                FlowMultiSelectComboBoxRoute.class,
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
        FlowHostShutdownTerminationStartup.launcherReturned();
    }
}
