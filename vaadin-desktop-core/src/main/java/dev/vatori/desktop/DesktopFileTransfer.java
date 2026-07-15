package dev.vatori.desktop;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DesktopFileTransfer {
    private static final int MAX_INLINE_DOWNLOAD_BYTES = 64 * 1024;

    private final NativeBridge nativeBridge;

    DesktopFileTransfer(NativeBridge nativeBridge) {
        this.nativeBridge = nativeBridge;
    }

    public DesktopCommandResult downloadText(String suggestedName, String mimeType, String body) {
        String safeBody = body == null ? "" : body;
        int byteCount = safeBody.getBytes(StandardCharsets.UTF_8).length;
        if (byteCount > MAX_INLINE_DOWNLOAD_BYTES) {
            throw new IllegalArgumentException("Inline download body exceeds "
                    + MAX_INLINE_DOWNLOAD_BYTES + " bytes");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("suggestedName", suggestedName == null || suggestedName.isBlank()
                ? "download.txt"
                : suggestedName);
        payload.put("mimeType", mimeType == null || mimeType.isBlank()
                ? "text/plain; charset=utf-8"
                : mimeType);
        payload.put("bodyEncoding", "utf-8");
        payload.put("byteCount", byteCount);
        payload.put("body", safeBody);
        return nativeBridge.invoke(new DesktopCommand("fileTransfer.download", payload));
    }

    public DesktopCommandResult selectUploadFile(String title) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", title == null || title.isBlank() ? "Select File" : title);
        payload.put("readBytesLimit", MAX_INLINE_DOWNLOAD_BYTES);
        return nativeBridge.invoke(new DesktopCommand("fileTransfer.selectUploadFile", payload));
    }
}
