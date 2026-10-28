package dev.vatori.desktop;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class DesktopFileDialog {
    private final NativeBridge nativeBridge;

    DesktopFileDialog(NativeBridge nativeBridge) {
        this.nativeBridge = nativeBridge;
    }

    public Optional<Path> openFile(String title) {
        return openFileResult(title).selectedPath();
    }

    public DesktopFileDialogResult openFileResult(String title) {
        DesktopCommandResult result = nativeBridge.invoke(new DesktopCommand(
                "fileDialog.openFile", Map.of("title", title)));
        return DesktopFileDialogResult.from(result);
    }

    /**
     * Opens a native file panel and requests an owner-bound, read-only grant for
     * the selected file. The returned grant retains this dialog's native bridge,
     * including any captured Flow capability scope.
     *
     * @param title native panel title
     * @return normalized selection outcome
     */
    public DesktopFileReadGrantResult openReadGrant(String title) {
        Objects.requireNonNull(title, "title");
        NativeBridge grantBridge = nativeBridge.captureCapabilityContext();
        DesktopCommandResult result = grantBridge.invoke(new DesktopCommand(
                "fileDialog.openReadGrant", Map.of("title", title)));
        return DesktopFileReadGrantResult.from(result, grantBridge);
    }

    /**
     * Selects a new file in a native save panel and creates an owner-bound write
     * grant. An existing target is rejected, never replaced or truncated.
     * Selection creates an empty file; writes are immediate and releasing the
     * grant never deletes or rolls back that file.
     *
     * @param title native panel title
     * @param suggestedName suggested filename, not an application-selected path
     * @return normalized selection outcome
     */
    public DesktopFileWriteGrantResult saveNewWriteGrant(String title,
            String suggestedName) {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(suggestedName, "suggestedName");
        NativeBridge grantBridge = nativeBridge.captureCapabilityContext();
        DesktopCommandResult result = grantBridge.invoke(new DesktopCommand(
                "fileDialog.saveNewWriteGrant", Map.of(
                        "title", title, "suggestedName", suggestedName)));
        return DesktopFileWriteGrantResult.from(result, grantBridge);
    }
}
