package dev.vatori.ipcframework;

import com.vaadin.flow.component.button.Button;
import dev.vatori.desktop.Desktop;

/** Shared fixture action; replacement still goes through the public application API. */
final class FileGrantReplacementAction {
    private FileGrantReplacementAction() { }

    static void prepareReadHandoff(java.nio.file.Path path) throws java.io.IOException {
        // Historical replay runners supply an existing private directory. The
        // isolated Sandbox profile can create only its unique, fixed-name leaf.
        var parent = path.getParent();
        if (java.nio.file.Files.exists(parent, java.nio.file.LinkOption.NOFOLLOW_LINKS)) return;
        if (!"old-grant-token.bin".equals(path.getFileName().toString())
                || !parent.getFileName().toString().matches("vatori-file-read-grant-replay\\.[A-Za-z0-9]{6}")) {
            throw new java.io.IOException("Invalid private read handoff directory");
        }
        java.nio.file.Files.createDirectory(parent, java.nio.file.attribute.PosixFilePermissions
                .asFileAttribute(java.nio.file.attribute.PosixFilePermissions.fromString("rwx------")));
    }

    static Button button() {
        Button button = new Button("Replace runtime", event -> {
            event.getSource().setEnabled(false);
            var context = Desktop.current().captureCapabilityContext();
            Thread.ofVirtual().name("file-grant-replacement").start(() ->
                    context.runtime().requestReplacement("file-grant-replacement", "next"));
        });
        button.setId("file-grant-product-replacement");
        return button;
    }
}
