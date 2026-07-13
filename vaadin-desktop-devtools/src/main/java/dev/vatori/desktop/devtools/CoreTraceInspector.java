package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class CoreTraceInspector {
    private static final String FLOW_IPC_FRAMEWORK_APP = "flow-over-ipc-framework-app";
    private static final String FRAMEWORK_APP_CAPABILITY_MANIFEST =
            FLOW_IPC_FRAMEWORK_APP + "/desktop-capabilities.json";
    private static final String MANIFEST_ENFORCED = "\"permissionMode\":\"manifest-enforced\"";
    private static final String CAPABILITY_MANIFEST_TYPE = "\"type\":\"capabilityManifest\"";
    private static final String CAPABILITY_RESULT_TYPE = "\"type\":\"capabilityResult\"";
    private static final String FILE_DIALOG_OPEN_FILE = "\"command\":\"fileDialog.openFile\"";
    private static final String FILE_TRANSFER_DOWNLOAD = "\"command\":\"fileTransfer.download\"";
    private static final String FILE_TRANSFER_UPLOAD = "\"command\":\"fileTransfer.selectUploadFile\"";
    private static final String FLOW_IPC_DOWNLOAD_SMOKE = "flow-ipc-download-smoke.txt";
    private static final String FLOW_IPC_UPLOAD_SMOKE = "flow-ipc-upload-smoke.txt";

    private CoreTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("\"transport\":\"flow-ipc\"")) {
            summary.core.flowIpcTransport = true;
        }
        if (line.contains("\"compatibilityMode\":false")) {
            summary.core.compatibilityFalse = true;
        }
        if (line.contains("\"type\":\"launch\"")
                && line.contains(FLOW_IPC_FRAMEWORK_APP)
                && line.contains("\"transport\":\"flow-ipc\"")
                && line.contains("\"compatibilityMode\":false")) {
            summary.frameworkApp.launchObserved = true;
        }
        if (isLoadedCapabilityManifest(line)
                && line.contains(FRAMEWORK_APP_CAPABILITY_MANIFEST)
                && line.contains(MANIFEST_ENFORCED)
                && containsShellCapabilities(line)) {
            summary.frameworkApp.capabilityManifestObserved = true;
        }
        if (isLoadedCapabilityManifest(line)
                && containsSupportedPermissionMode(line)
                && containsCoreCapabilities(line)) {
            summary.coreCapabilities.capabilityManifestObserved = true;
        }
        if (isLoadedCapabilityManifest(line)
                && line.contains(MANIFEST_ENFORCED)
                && containsCoreCapabilities(line)) {
            summary.coreCapabilities.manifestEnforcedObserved = true;
        }
        if (isLoadedCapabilityManifest(line)
                && line.contains(MANIFEST_ENFORCED)
                && containsFileTransferCapabilities(line)) {
            summary.coreCapabilities.fileTransferManifestObserved = true;
        }
        if (line.contains("\"serverSequenceScope\":\"java-sidecar-to-webview\"")
                && line.contains("\"serverSequence\"")) {
            summary.core.serverSequenceObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "flowIpcServerSequenceObserved", "true")
                && containsLiteralField(line, "flowIpcServerSequenceMonotonic", "true")
                && containsPositiveIntegerField(line, "flowIpcServerSequenceCount")) {
            summary.core.flowIpcServerSequenceDomObserved = true;
        }
        if (containsClickedText(line)) {
            summary.core.clickPatchObserved = true;
        }
        if (line.contains("native-nspasteboard")) {
            summary.coreCapabilities.nativeClipboardObserved = true;
        }
        if (isCapabilityResult(line)
                && containsSupportedPermissionMode(line)
                && line.contains("\"permissionDecision\":\"allowed\"")
                && line.contains("\"permissionScope\":\"window:main\"")) {
            summary.coreCapabilities.capabilityPermissionObserved = true;
        }
        if (isCapabilityResult(line)
                && line.contains("\"permissionDecision\":\"denied\"")
                && line.contains("\"permissionReason\":\"manifest-denied\"")
                && line.contains("\"success\":false")
                && line.contains("diagnostics.deniedCapability")) {
            summary.coreCapabilities.capabilityDeniedObserved = true;
        }
        if (isCapabilityResult(line)
                && line.contains("\"permissionManifestLoaded\":true")
                && line.contains("\"permissionReason\":\"manifest-allow\"")
                && line.contains("desktop-capabilities.json")
                && containsCoreCapabilities(line)) {
            summary.coreCapabilities.capabilityManifestResultObserved = true;
        }
        if (isCapabilityResult(line)
                && line.contains(FILE_DIALOG_OPEN_FILE)
                && fileDialogResultObserved(line)
                && line.contains("\"permissionManifestLoaded\":true")
                && line.contains("\"permissionDecision\":\"allowed\"")
                && line.contains("\"permissionReason\":\"manifest-allow\"")) {
            summary.coreCapabilities.fileDialogResultObserved = true;
        }
        if (isCapabilityResult(line)
                && line.contains(FILE_DIALOG_OPEN_FILE)
                && line.contains("\"mode\":\"native-appkit-open-panel\"")
                && line.contains("\"paths\":")
                && line.contains("\"securityScoped\":false")
                && line.contains("\"permissionDecision\":\"allowed\"")) {
            summary.coreCapabilities.nativeFileDialogResultObserved = true;
        }
        if (line.contains("\"type\":\"command\"")
                && line.contains(FILE_TRANSFER_DOWNLOAD)
                && line.contains("\"bodyRedacted\":true")
                && containsPositiveIntegerField(line, "bodyBytes")) {
            summary.coreCapabilities.fileTransferDownloadCommandRedactedObserved = true;
        }
        if (isCapabilityResult(line)
                && line.contains(FILE_TRANSFER_DOWNLOAD)
                && line.contains("\"operation\":\"download\"")
                && line.contains("\"success\":true")
                && line.contains("\"bodyRedacted\":true")
                && line.contains("\"permissionDecision\":\"allowed\"")
                && containsPositiveIntegerField(line, "byteCount")
                && line.contains(FLOW_IPC_DOWNLOAD_SMOKE)) {
            summary.coreCapabilities.fileTransferDownloadResultObserved = true;
        }
        if (isCapabilityResult(line)
                && line.contains(FILE_TRANSFER_UPLOAD)
                && line.contains("\"operation\":\"upload\"")
                && line.contains("\"success\":true")
                && line.contains("\"bodyRedacted\":true")
                && line.contains("\"securityScoped\":false")
                && line.contains("\"permissionDecision\":\"allowed\"")
                && containsPositiveIntegerField(line, "byteCount")
                && containsPositiveIntegerField(line, "sampledBytes")
                && line.contains(FLOW_IPC_UPLOAD_SMOKE)) {
            summary.coreCapabilities.fileTransferUploadResultObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "fileTransferDownloadResultObserved", "true")
                && containsLiteralField(line, "fileTransferUploadResultObserved", "true")
                && containsLiteralField(line, "fileTransferDownloadBodyRedacted", "true")
                && containsLiteralField(line, "fileTransferUploadBodyRedacted", "true")
                && containsPositiveIntegerField(line, "fileTransferDownloadByteCount")
                && containsPositiveIntegerField(line, "fileTransferUploadByteCount")
                && containsPositiveIntegerField(line, "fileTransferUploadSampledBytes")
                && containsStringFieldPrefix(line, "fileTransferResultOrder", "download,upload")
                && line.contains(FLOW_IPC_DOWNLOAD_SMOKE)
                && line.contains(FLOW_IPC_UPLOAD_SMOKE)) {
            summary.coreCapabilities.fileTransferDomObserved = true;
        }
        if (line.contains("\"type\":\"error\"")
                && line.contains("webviewVerificationProbe")
                && line.contains("verification-error-probe")) {
            summary.core.webViewErrorObserved = true;
        }
        if (line.contains("\"source\":\"UidlWriter\"")
                && line.contains("\"reason\":\"bootstrap\"")
                && line.contains("\"containsChanges\":true")
                && line.contains("\"containsSyncId\":true")
                && line.contains("\"consumedByWebView\":true")
                && line.contains("\"flow-client-engine-visible\"")) {
            summary.core.uidlWriterObserved = true;
        }
        if (line.contains("\"uidlRpcDispatch\":true")) {
            summary.flowClient.uidlRpcDispatchObserved = true;
        }
    }

    private static boolean isLoadedCapabilityManifest(String line) {
        return line.contains(CAPABILITY_MANIFEST_TYPE)
                && line.contains("\"loaded\":true");
    }

    private static boolean isCapabilityResult(String line) {
        return line.contains(CAPABILITY_RESULT_TYPE);
    }

    private static boolean containsShellCapabilities(String line) {
        return containsAll(line,
                "window.setTitle",
                "clipboard.writeText",
                "notification.show");
    }

    private static boolean containsCoreCapabilities(String line) {
        return containsShellCapabilities(line)
                && line.contains("fileDialog.openFile");
    }

    private static boolean containsFileTransferCapabilities(String line) {
        return containsAll(line, "fileTransfer.download", "fileTransfer.selectUploadFile");
    }

    private static boolean fileDialogResultObserved(String line) {
        boolean v1Scaffold = line.contains("\"mode\":\"scaffold-cancelled\"")
                && line.contains("\"canceled\":true");
        boolean appKitPanel = line.contains("\"mode\":\"native-appkit-open-panel\"")
                && line.contains("\"canceled\":")
                && line.contains("\"paths\":")
                && line.contains("\"securityScoped\":false");
        return v1Scaffold || appKitPanel;
    }

    private static boolean containsSupportedPermissionMode(String line) {
        return line.contains("\"permissionMode\":\"allow-all-dev\"")
                || line.contains(MANIFEST_ENFORCED)
                || line.contains("\"permissionMode\":\"trace-only-dry-run\"");
    }
}
