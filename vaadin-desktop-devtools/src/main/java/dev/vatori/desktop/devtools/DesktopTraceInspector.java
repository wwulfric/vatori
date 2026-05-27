package dev.vatori.desktop.devtools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DesktopTraceInspector {
    private static final Pattern TYPE = Pattern.compile("\"type\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern DIRECTION = Pattern.compile("\"direction\"\\s*:\\s*\"([^\"]+)\"");
    private static final List<String> FLOW_IPC_REQUIRED_TYPES = List.of(
            "launch",
            "ready",
            "window.create",
            "bootstrapRequest",
            "bootstrapResponse",
            "resourceRequest",
            "resourceResponse",
            "push",
            "event",
            "resync",
            "command",
            "capabilityResult",
            "patch",
            "error",
            "webview.dom",
            "webview.snapshot",
            "shutdown");

    private DesktopTraceInspector() {
    }

    private enum AcceptanceGate {
        FLOW_IPC_V1("--require-flow-ipc-v1"),
        FLOW_IPC_V1_1("--require-flow-ipc-v1_1"),
        FLOW_IPC_V1_2("--require-flow-ipc-v1_2"),
        FLOW_IPC_V1_3("--require-flow-ipc-v1_3"),
        FLOW_IPC_V1_4("--require-flow-ipc-v1_4"),
        FLOW_IPC_V1_5("--require-flow-ipc-v1_5"),
        FLOW_IPC_V1_6("--require-flow-ipc-v1_6"),
        FLOW_IPC_V1_7("--require-flow-ipc-v1_7"),
        FLOW_IPC_V1_8("--require-flow-ipc-v1_8"),
        FLOW_IPC_V1_9_LOADONDEMAND("--require-flow-ipc-v1_9-loadondemand"),
        FLOW_IPC_V1_9("--require-flow-ipc-v1_9"),
        FLOW_IPC_V1_10_LAZY_DEPENDENCY("--require-flow-ipc-v1_10-lazy-dependency"),
        FLOW_IPC_V1_10_PRODUCTION_RUNTIME("--require-flow-ipc-v1_10-production-runtime"),
        FLOW_IPC_V1_10("--require-flow-ipc-v1_10"),
        FLOW_IPC_V1_11_LOADONDEMAND_CHUNK("--require-flow-ipc-v1_11-loadondemand-chunk"),
        FLOW_IPC_V1_11("--require-flow-ipc-v1_11"),
        FLOW_IPC_V1_12_LOADONDEMAND_CHUNK_FILE("--require-flow-ipc-v1_12-loadondemand-chunk-file"),
        FLOW_IPC_V1_12("--require-flow-ipc-v1_12"),
        FLOW_IPC_V1_13_PRODUCTION_OWNER("--require-flow-ipc-v1_13-production-owner"),
        FLOW_IPC_V1_13("--require-flow-ipc-v1_13"),
        FLOW_IPC_V1_14_PRODUCTION_OWNER_IMPORT("--require-flow-ipc-v1_14-production-owner-import"),
        FLOW_IPC_V1_14("--require-flow-ipc-v1_14"),
        FLOW_IPC_V1_15_PRODUCTION_OWNER_DEPENDENCIES(
                "--require-flow-ipc-v1_15-production-owner-dependencies"),
        FLOW_IPC_V1_15("--require-flow-ipc-v1_15"),
        FLOW_IPC_V1_16_PRODUCTION_OWNER_ORDERING(
                "--require-flow-ipc-v1_16-production-owner-ordering"),
        FLOW_IPC_V1_16("--require-flow-ipc-v1_16"),
        FLOW_IPC_V1_3_FRAMEWORK("--require-flow-ipc-v1_3-framework");

        private final String option;

        AcceptanceGate(String option) {
            this.option = option;
        }

        private static AcceptanceGate fromOption(String arg) {
            for (AcceptanceGate gate : values()) {
                if (gate.option.equals(arg)) {
                    return gate;
                }
            }
            return null;
        }

        private static String usageOptions() {
            List<String> options = new ArrayList<>();
            for (AcceptanceGate gate : values()) {
                options.add(gate.option);
            }
            return String.join("|", options);
        }

        private EnumSet<AcceptanceGate> expandedRequirements() {
            return switch (this) {
                case FLOW_IPC_V1 -> EnumSet.of(FLOW_IPC_V1);
                case FLOW_IPC_V1_1 -> EnumSet.of(FLOW_IPC_V1, FLOW_IPC_V1_1);
                case FLOW_IPC_V1_2 -> EnumSet.of(FLOW_IPC_V1, FLOW_IPC_V1_1, FLOW_IPC_V1_2);
                case FLOW_IPC_V1_3 -> EnumSet.of(FLOW_IPC_V1, FLOW_IPC_V1_1, FLOW_IPC_V1_2,
                        FLOW_IPC_V1_3);
                case FLOW_IPC_V1_4 -> EnumSet.of(FLOW_IPC_V1, FLOW_IPC_V1_1, FLOW_IPC_V1_2,
                        FLOW_IPC_V1_3, FLOW_IPC_V1_4);
                case FLOW_IPC_V1_5 -> EnumSet.of(FLOW_IPC_V1, FLOW_IPC_V1_1, FLOW_IPC_V1_2,
                        FLOW_IPC_V1_3, FLOW_IPC_V1_4, FLOW_IPC_V1_5);
                case FLOW_IPC_V1_6 -> EnumSet.of(FLOW_IPC_V1, FLOW_IPC_V1_1, FLOW_IPC_V1_2,
                        FLOW_IPC_V1_3, FLOW_IPC_V1_4, FLOW_IPC_V1_5, FLOW_IPC_V1_6);
                // v1.6 is a preserved blocker-evidence gate. v1.7/v1.8 supersede that path.
                case FLOW_IPC_V1_7 -> EnumSet.of(FLOW_IPC_V1, FLOW_IPC_V1_1, FLOW_IPC_V1_2,
                        FLOW_IPC_V1_3, FLOW_IPC_V1_4, FLOW_IPC_V1_5, FLOW_IPC_V1_7);
                case FLOW_IPC_V1_8 -> EnumSet.of(FLOW_IPC_V1, FLOW_IPC_V1_1, FLOW_IPC_V1_2,
                        FLOW_IPC_V1_3, FLOW_IPC_V1_4, FLOW_IPC_V1_5, FLOW_IPC_V1_7,
                        FLOW_IPC_V1_8);
                // Delta gate: only the v1.9 Flow.loadOnDemand contract evidence.
                case FLOW_IPC_V1_9_LOADONDEMAND -> EnumSet.of(FLOW_IPC_V1_9_LOADONDEMAND);
                // Cumulative gate: v1.8 baseline plus the v1.9 lazy-loading delta.
                case FLOW_IPC_V1_9 -> EnumSet.of(FLOW_IPC_V1, FLOW_IPC_V1_1, FLOW_IPC_V1_2,
                        FLOW_IPC_V1_3, FLOW_IPC_V1_4, FLOW_IPC_V1_5, FLOW_IPC_V1_7,
                        FLOW_IPC_V1_8, FLOW_IPC_V1_9_LOADONDEMAND, FLOW_IPC_V1_9);
                case FLOW_IPC_V1_10_LAZY_DEPENDENCY -> EnumSet.of(FLOW_IPC_V1_10_LAZY_DEPENDENCY);
                case FLOW_IPC_V1_10_PRODUCTION_RUNTIME -> EnumSet.of(FLOW_IPC_V1_10_PRODUCTION_RUNTIME);
                case FLOW_IPC_V1_10 -> EnumSet.of(FLOW_IPC_V1, FLOW_IPC_V1_1, FLOW_IPC_V1_2,
                        FLOW_IPC_V1_3, FLOW_IPC_V1_4, FLOW_IPC_V1_5, FLOW_IPC_V1_7,
                        FLOW_IPC_V1_8, FLOW_IPC_V1_9_LOADONDEMAND, FLOW_IPC_V1_9,
                        FLOW_IPC_V1_10_LAZY_DEPENDENCY, FLOW_IPC_V1_10_PRODUCTION_RUNTIME,
                        FLOW_IPC_V1_10);
                case FLOW_IPC_V1_11_LOADONDEMAND_CHUNK -> EnumSet.of(FLOW_IPC_V1_11_LOADONDEMAND_CHUNK);
                case FLOW_IPC_V1_11 -> EnumSet.of(FLOW_IPC_V1, FLOW_IPC_V1_1, FLOW_IPC_V1_2,
                        FLOW_IPC_V1_3, FLOW_IPC_V1_4, FLOW_IPC_V1_5, FLOW_IPC_V1_7,
                        FLOW_IPC_V1_8, FLOW_IPC_V1_9_LOADONDEMAND, FLOW_IPC_V1_9,
                        FLOW_IPC_V1_10_LAZY_DEPENDENCY, FLOW_IPC_V1_10_PRODUCTION_RUNTIME,
                        FLOW_IPC_V1_10, FLOW_IPC_V1_11_LOADONDEMAND_CHUNK, FLOW_IPC_V1_11);
                case FLOW_IPC_V1_12_LOADONDEMAND_CHUNK_FILE ->
                    EnumSet.of(FLOW_IPC_V1_12_LOADONDEMAND_CHUNK_FILE);
                case FLOW_IPC_V1_12 -> EnumSet.of(FLOW_IPC_V1, FLOW_IPC_V1_1, FLOW_IPC_V1_2,
                        FLOW_IPC_V1_3, FLOW_IPC_V1_4, FLOW_IPC_V1_5, FLOW_IPC_V1_7,
                        FLOW_IPC_V1_8, FLOW_IPC_V1_9_LOADONDEMAND, FLOW_IPC_V1_9,
                        FLOW_IPC_V1_10_LAZY_DEPENDENCY, FLOW_IPC_V1_10_PRODUCTION_RUNTIME,
                        FLOW_IPC_V1_10, FLOW_IPC_V1_11_LOADONDEMAND_CHUNK, FLOW_IPC_V1_11,
                        FLOW_IPC_V1_12_LOADONDEMAND_CHUNK_FILE, FLOW_IPC_V1_12);
                case FLOW_IPC_V1_13_PRODUCTION_OWNER -> EnumSet.of(FLOW_IPC_V1_13_PRODUCTION_OWNER);
                case FLOW_IPC_V1_13 -> EnumSet.of(FLOW_IPC_V1, FLOW_IPC_V1_1, FLOW_IPC_V1_2,
                        FLOW_IPC_V1_3, FLOW_IPC_V1_4, FLOW_IPC_V1_5, FLOW_IPC_V1_7,
                        FLOW_IPC_V1_8, FLOW_IPC_V1_9_LOADONDEMAND, FLOW_IPC_V1_9,
                        FLOW_IPC_V1_10_LAZY_DEPENDENCY, FLOW_IPC_V1_10_PRODUCTION_RUNTIME,
                        FLOW_IPC_V1_10, FLOW_IPC_V1_11_LOADONDEMAND_CHUNK, FLOW_IPC_V1_11,
                        FLOW_IPC_V1_12_LOADONDEMAND_CHUNK_FILE, FLOW_IPC_V1_12,
                        FLOW_IPC_V1_13_PRODUCTION_OWNER, FLOW_IPC_V1_13);
                case FLOW_IPC_V1_14_PRODUCTION_OWNER_IMPORT ->
                    EnumSet.of(FLOW_IPC_V1_14_PRODUCTION_OWNER_IMPORT);
                case FLOW_IPC_V1_14 -> EnumSet.of(FLOW_IPC_V1, FLOW_IPC_V1_1, FLOW_IPC_V1_2,
                        FLOW_IPC_V1_3, FLOW_IPC_V1_4, FLOW_IPC_V1_5, FLOW_IPC_V1_7,
                        FLOW_IPC_V1_8, FLOW_IPC_V1_9_LOADONDEMAND, FLOW_IPC_V1_9,
                        FLOW_IPC_V1_10_LAZY_DEPENDENCY, FLOW_IPC_V1_10_PRODUCTION_RUNTIME,
                        FLOW_IPC_V1_10, FLOW_IPC_V1_11_LOADONDEMAND_CHUNK, FLOW_IPC_V1_11,
                        FLOW_IPC_V1_12_LOADONDEMAND_CHUNK_FILE, FLOW_IPC_V1_12,
                        FLOW_IPC_V1_13_PRODUCTION_OWNER, FLOW_IPC_V1_13,
                        FLOW_IPC_V1_14_PRODUCTION_OWNER_IMPORT, FLOW_IPC_V1_14);
                case FLOW_IPC_V1_15_PRODUCTION_OWNER_DEPENDENCIES ->
                    EnumSet.of(FLOW_IPC_V1_15_PRODUCTION_OWNER_DEPENDENCIES);
                case FLOW_IPC_V1_15 -> EnumSet.of(FLOW_IPC_V1, FLOW_IPC_V1_1, FLOW_IPC_V1_2,
                        FLOW_IPC_V1_3, FLOW_IPC_V1_4, FLOW_IPC_V1_5, FLOW_IPC_V1_7,
                        FLOW_IPC_V1_8, FLOW_IPC_V1_9_LOADONDEMAND, FLOW_IPC_V1_9,
                        FLOW_IPC_V1_10_LAZY_DEPENDENCY, FLOW_IPC_V1_10_PRODUCTION_RUNTIME,
                        FLOW_IPC_V1_10, FLOW_IPC_V1_11_LOADONDEMAND_CHUNK, FLOW_IPC_V1_11,
                        FLOW_IPC_V1_12_LOADONDEMAND_CHUNK_FILE, FLOW_IPC_V1_12,
                        FLOW_IPC_V1_13_PRODUCTION_OWNER, FLOW_IPC_V1_13,
                        FLOW_IPC_V1_14_PRODUCTION_OWNER_IMPORT, FLOW_IPC_V1_14,
                        FLOW_IPC_V1_15_PRODUCTION_OWNER_DEPENDENCIES, FLOW_IPC_V1_15);
                case FLOW_IPC_V1_16_PRODUCTION_OWNER_ORDERING ->
                    EnumSet.of(FLOW_IPC_V1_16_PRODUCTION_OWNER_ORDERING);
                case FLOW_IPC_V1_16 -> EnumSet.of(FLOW_IPC_V1, FLOW_IPC_V1_1, FLOW_IPC_V1_2,
                        FLOW_IPC_V1_3, FLOW_IPC_V1_4, FLOW_IPC_V1_5, FLOW_IPC_V1_7,
                        FLOW_IPC_V1_8, FLOW_IPC_V1_9_LOADONDEMAND, FLOW_IPC_V1_9,
                        FLOW_IPC_V1_10_LAZY_DEPENDENCY, FLOW_IPC_V1_10_PRODUCTION_RUNTIME,
                        FLOW_IPC_V1_10, FLOW_IPC_V1_11_LOADONDEMAND_CHUNK, FLOW_IPC_V1_11,
                        FLOW_IPC_V1_12_LOADONDEMAND_CHUNK_FILE, FLOW_IPC_V1_12,
                        FLOW_IPC_V1_13_PRODUCTION_OWNER, FLOW_IPC_V1_13,
                        FLOW_IPC_V1_14_PRODUCTION_OWNER_IMPORT, FLOW_IPC_V1_14,
                        FLOW_IPC_V1_15_PRODUCTION_OWNER_DEPENDENCIES, FLOW_IPC_V1_15,
                        FLOW_IPC_V1_16_PRODUCTION_OWNER_ORDERING, FLOW_IPC_V1_16);
                case FLOW_IPC_V1_3_FRAMEWORK -> EnumSet.of(FLOW_IPC_V1_3_FRAMEWORK);
            };
        }

        private boolean isSatisfiedBy(TraceSummary summary) {
            return switch (this) {
                case FLOW_IPC_V1 -> summary.hasRequiredFlowIpcTypes();
                case FLOW_IPC_V1_1 -> summary.hasRequiredFlowIpcV1_1Types();
                case FLOW_IPC_V1_2 -> summary.hasRequiredFlowIpcV1_2Types();
                case FLOW_IPC_V1_3 -> summary.hasRequiredFlowIpcV1_3Types();
                case FLOW_IPC_V1_4 -> summary.hasRequiredFlowIpcV1_4Types();
                case FLOW_IPC_V1_5 -> summary.hasRequiredFlowIpcV1_5Types();
                case FLOW_IPC_V1_6 -> summary.hasRequiredFlowIpcV1_6Types();
                case FLOW_IPC_V1_7 -> summary.hasRequiredFlowIpcV1_7Types();
                case FLOW_IPC_V1_8 -> summary.hasRequiredFlowIpcV1_8Types();
                case FLOW_IPC_V1_9_LOADONDEMAND -> summary.hasRequiredFlowIpcV1_9LoadOnDemandTypes();
                case FLOW_IPC_V1_9 -> summary.hasRequiredFlowIpcV1_9Types();
                case FLOW_IPC_V1_10_LAZY_DEPENDENCY -> summary.hasRequiredFlowIpcV1_10LazyDependencyTypes();
                case FLOW_IPC_V1_10_PRODUCTION_RUNTIME -> summary.hasRequiredFlowIpcV1_10ProductionRuntimeTypes();
                case FLOW_IPC_V1_10 -> summary.hasRequiredFlowIpcV1_10Types();
                case FLOW_IPC_V1_11_LOADONDEMAND_CHUNK -> summary.hasRequiredFlowIpcV1_11LoadOnDemandChunkTypes();
                case FLOW_IPC_V1_11 -> summary.hasRequiredFlowIpcV1_11Types();
                case FLOW_IPC_V1_12_LOADONDEMAND_CHUNK_FILE ->
                    summary.hasRequiredFlowIpcV1_12LoadOnDemandChunkFileTypes();
                case FLOW_IPC_V1_12 -> summary.hasRequiredFlowIpcV1_12Types();
                case FLOW_IPC_V1_13_PRODUCTION_OWNER ->
                    summary.hasRequiredFlowIpcV1_13ProductionOwnerTypes();
                case FLOW_IPC_V1_13 -> summary.hasRequiredFlowIpcV1_13Types();
                case FLOW_IPC_V1_14_PRODUCTION_OWNER_IMPORT ->
                    summary.hasRequiredFlowIpcV1_14ProductionOwnerImportTypes();
                case FLOW_IPC_V1_14 -> summary.hasRequiredFlowIpcV1_14Types();
                case FLOW_IPC_V1_15_PRODUCTION_OWNER_DEPENDENCIES ->
                    summary.hasRequiredFlowIpcV1_15ProductionOwnerDependencyTypes();
                case FLOW_IPC_V1_15 -> summary.hasRequiredFlowIpcV1_15Types();
                case FLOW_IPC_V1_16_PRODUCTION_OWNER_ORDERING ->
                    summary.hasRequiredFlowIpcV1_16ProductionOwnerOrderingTypes();
                case FLOW_IPC_V1_16 -> summary.hasRequiredFlowIpcV1_16Types();
                case FLOW_IPC_V1_3_FRAMEWORK -> summary.hasRequiredFlowIpcV1_3FrameworkTypes();
            };
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0 || "--help".equals(args[0])) {
            usage();
            return;
        }

        EnumSet<AcceptanceGate> requiredGates = EnumSet.noneOf(AcceptanceGate.class);
        List<Path> paths = new ArrayList<>();
        for (String arg : args) {
            AcceptanceGate gate = AcceptanceGate.fromOption(arg);
            if (gate != null) {
                requiredGates.addAll(gate.expandedRequirements());
            } else {
                paths.add(Path.of(arg));
            }
        }

        if (paths.isEmpty()) {
            usage();
            System.exit(2);
        }

        int failures = 0;
        for (Path path : paths) {
            TraceSummary summary = inspect(path);
            summary.print(path, requiredGates.contains(AcceptanceGate.FLOW_IPC_V1));
            for (AcceptanceGate gate : AcceptanceGate.values()) {
                if (requiredGates.contains(gate) && !gate.isSatisfiedBy(summary)) {
                    failures++;
                }
            }
        }

        if (failures > 0) {
            System.exit(2);
        }
    }

    public static TraceSummary inspect(Path path) throws IOException {
        TraceSummary summary = new TraceSummary();
        for (String rawLine : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            String line = normalize(rawLine);
            if (line.isBlank()) {
                continue;
            }
            summary.total++;
            String type = first(TYPE, line, "unknown");
            String direction = first(DIRECTION, line, "unknown");
            summary.types.merge(type, 1, Integer::sum);
            summary.directions.merge(direction, 1, Integer::sum);
            if (line.contains("\"transport\":\"flow-ipc\"")) {
                summary.flowIpcTransport = true;
            }
            if (line.contains("\"compatibilityMode\":false")) {
                summary.compatibilityFalse = true;
            }
            if (line.contains("\"type\":\"launch\"")
                    && line.contains("flow-over-ipc-framework-app")
                    && line.contains("\"transport\":\"flow-ipc\"")
                    && line.contains("\"compatibilityMode\":false")) {
                summary.frameworkAppLaunchObserved = true;
            }
            if (line.contains("\"type\":\"capabilityManifest\"")
                    && line.contains("flow-over-ipc-framework-app/desktop-capabilities.json")
                    && line.contains("\"loaded\":true")
                    && line.contains("\"permissionMode\":\"manifest-enforced\"")
                    && line.contains("window.setTitle")
                    && line.contains("clipboard.writeText")
                    && line.contains("notification.show")) {
                summary.frameworkAppCapabilityManifestObserved = true;
            }
            if (line.contains("\"type\":\"capabilityManifest\"")
                    && line.contains("\"loaded\":true")
                    && containsSupportedPermissionMode(line)
                    && line.contains("window.setTitle")
                    && line.contains("clipboard.writeText")
                    && line.contains("fileDialog.openFile")
                    && line.contains("notification.show")) {
                summary.capabilityManifestObserved = true;
            }
            if (line.contains("\"type\":\"capabilityManifest\"")
                    && line.contains("\"loaded\":true")
                    && line.contains("\"permissionMode\":\"manifest-enforced\"")
                    && line.contains("window.setTitle")
                    && line.contains("clipboard.writeText")
                    && line.contains("fileDialog.openFile")
                    && line.contains("notification.show")) {
                summary.manifestEnforcedObserved = true;
            }
            if (line.contains("\"type\":\"capabilityManifest\"")
                    && line.contains("\"loaded\":true")
                    && line.contains("\"permissionMode\":\"manifest-enforced\"")
                    && line.contains("fileTransfer.download")
                    && line.contains("fileTransfer.selectUploadFile")) {
                summary.fileTransferManifestObserved = true;
            }
            if (line.contains("\"serverSequenceScope\":\"java-sidecar-to-webview\"")
                    && line.contains("\"serverSequence\"")) {
                summary.serverSequenceObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "flowIpcServerSequenceObserved", "true")
                    && containsLiteralField(line, "flowIpcServerSequenceMonotonic", "true")
                    && line.contains("bootstrapResponse:")
                    && line.contains("resourceResponse:")
                    && line.contains("push:")
                    && line.contains("patch:")) {
                summary.flowIpcServerSequenceDomObserved = true;
            }
            if (containsClickedText(line)) {
                summary.clickPatchObserved = true;
            }
            if (line.contains("native-nspasteboard")) {
                summary.nativeClipboardObserved = true;
            }
            if (line.contains("\"type\":\"capabilityResult\"")
                    && containsSupportedPermissionMode(line)
                    && line.contains("\"permissionDecision\":\"allowed\"")
                    && line.contains("\"permissionScope\":\"window:main\"")) {
                summary.capabilityPermissionObserved = true;
            }
            if (line.contains("\"type\":\"capabilityResult\"")
                    && line.contains("\"permissionDecision\":\"denied\"")
                    && line.contains("\"permissionReason\":\"manifest-denied\"")
                    && line.contains("\"success\":false")
                    && line.contains("diagnostics.deniedCapability")) {
                summary.capabilityDeniedObserved = true;
            }
            if (line.contains("\"type\":\"capabilityResult\"")
                    && line.contains("\"permissionManifestLoaded\":true")
                    && line.contains("\"permissionReason\":\"manifest-allow\"")
                    && line.contains("desktop-capabilities.json")
                    && line.contains("window.setTitle")
                    && line.contains("clipboard.writeText")
                    && line.contains("fileDialog.openFile")
                    && line.contains("notification.show")) {
                summary.capabilityManifestResultObserved = true;
            }
            if (line.contains("\"type\":\"capabilityResult\"")
                    && line.contains("\"command\":\"fileDialog.openFile\"")
                    && fileDialogResultObserved(line)
                    && line.contains("\"permissionManifestLoaded\":true")
                    && line.contains("\"permissionDecision\":\"allowed\"")
                    && line.contains("\"permissionReason\":\"manifest-allow\"")) {
                summary.fileDialogResultObserved = true;
            }
            if (line.contains("\"type\":\"capabilityResult\"")
                    && line.contains("\"command\":\"fileDialog.openFile\"")
                    && line.contains("\"mode\":\"native-appkit-open-panel\"")
                    && line.contains("\"paths\":")
                    && line.contains("\"securityScoped\":false")
                    && line.contains("\"permissionDecision\":\"allowed\"")) {
                summary.nativeFileDialogResultObserved = true;
            }
            if (line.contains("\"type\":\"command\"")
                    && line.contains("\"command\":\"fileTransfer.download\"")
                    && line.contains("\"bodyRedacted\":true")
                    && containsPositiveIntegerField(line, "bodyBytes")) {
                summary.fileTransferDownloadCommandRedactedObserved = true;
            }
            if (line.contains("\"type\":\"capabilityResult\"")
                    && line.contains("\"command\":\"fileTransfer.download\"")
                    && line.contains("\"operation\":\"download\"")
                    && line.contains("\"success\":true")
                    && line.contains("\"bodyRedacted\":true")
                    && line.contains("\"permissionDecision\":\"allowed\"")
                    && containsPositiveIntegerField(line, "byteCount")
                    && line.contains("flow-ipc-download-smoke.txt")) {
                summary.fileTransferDownloadResultObserved = true;
            }
            if (line.contains("\"type\":\"capabilityResult\"")
                    && line.contains("\"command\":\"fileTransfer.selectUploadFile\"")
                    && line.contains("\"operation\":\"upload\"")
                    && line.contains("\"success\":true")
                    && line.contains("\"bodyRedacted\":true")
                    && line.contains("\"securityScoped\":false")
                    && line.contains("\"permissionDecision\":\"allowed\"")
                    && containsPositiveIntegerField(line, "byteCount")
                    && containsPositiveIntegerField(line, "sampledBytes")
                    && line.contains("flow-ipc-upload-smoke.txt")) {
                summary.fileTransferUploadResultObserved = true;
            }
            if (line.contains("\"type\":\"error\"")
                    && line.contains("webviewVerificationProbe")
                    && line.contains("verification-error-probe")) {
                summary.webViewErrorObserved = true;
            }
            if (line.contains("\"source\":\"UidlWriter\"")
                    && line.contains("\"reason\":\"bootstrap\"")
                    && line.contains("\"containsChanges\":true")
                    && line.contains("\"containsSyncId\":true")
                    && line.contains("\"consumedByWebView\":true")
                    && line.contains("\"flow-client-engine-visible\"")) {
                summary.uidlWriterObserved = true;
            }
            if (line.contains("\"uidlRpcDispatch\":true")) {
                summary.uidlRpcDispatchObserved = true;
            }
            if (line.contains("dev.vatori.ipcframework.FlowOverIpcFrameworkView")
                    && line.contains("framework-root")
                    && line.contains("framework-budget")
                    && line.contains("framework-expedite")
                    && line.contains("framework-priority")
                    && line.contains("framework-apply")
                    && line.contains("framework-updates")
                    && line.contains("com.vaadin.flow.component.textfield.BigDecimalField")
                    && line.contains("com.vaadin.flow.component.checkbox.Checkbox")
                    && line.contains("com.vaadin.flow.component.select.Select")) {
                summary.frameworkAppBootstrapObserved = true;
            }
            if (line.contains("\"type\":\"event\"")
                    && line.contains("\"dispatch\":\"flow-client-xhr\"")
                    && line.contains("\"attachExistingElementById\"")
                    && line.contains("\"uidlRpc\"")
                    && line.contains("\"clientId\":0")) {
                summary.flowClientXhrAttachObserved = true;
            }
            if (line.contains("\"type\":\"event\"")
                    && line.contains("\"dispatch\":\"flow-client-xhr\"")
                    && line.contains("\"uidlRpc\"")
                    && line.contains("\"csrfToken\"")
                    && line.contains("\"securityKey\"")
                    && line.contains("\"event\":\"click\"")
                    && line.contains("\"clientId\":1")) {
                summary.webViewUidlRpcObserved = true;
                summary.flowClientXhrClickObserved = true;
            }
            if (line.contains("\"type\":\"event\"")
                    && line.contains("\"dispatch\":\"flow-client-xhr\"")
                    && line.contains("\"componentId\":\"framework-budget\"")
                    && line.contains("\"event\":\"change\"")
                    && line.contains("\"element.value\":\"73.25\"")) {
                summary.frameworkAppBudgetEventRpcObserved = true;
            }
            if (line.contains("\"type\":\"event\"")
                    && line.contains("\"dispatch\":\"flow-client-xhr\"")
                    && line.contains("\"componentId\":\"framework-expedite\"")
                    && line.contains("\"event\":\"change\"")
                    && line.contains("\"element.checked\":true")) {
                summary.frameworkAppCheckboxEventRpcObserved = true;
            }
            if (line.contains("\"type\":\"event\"")
                    && line.contains("\"dispatch\":\"flow-client-xhr\"")
                    && line.contains("\"componentId\":\"framework-priority\"")
                    && line.contains("\"event\":\"change\"")
                    && line.contains("\"element.value\":\"audit\"")) {
                summary.frameworkAppSelectEventRpcObserved = true;
            }
            if (line.contains("\"type\":\"event\"")
                    && line.contains("\"dispatch\":\"flow-client-xhr\"")
                    && line.contains("\"componentId\":\"framework-apply\"")
                    && line.contains("\"event\":\"click\"")) {
                summary.frameworkAppApplyEventRpcObserved = true;
            }
            if (line.contains("\"type\":\"patch\"")
                    && line.contains("\"source\":\"UidlRequestHandler\"")
                    && line.contains("\"reason\":\"event\"")
                    && line.contains("Budget changed: 73.25")) {
                summary.frameworkAppBudgetPatchObserved = true;
            }
            if (line.contains("\"type\":\"patch\"")
                    && line.contains("\"source\":\"UidlRequestHandler\"")
                    && line.contains("\"reason\":\"event\"")
                    && line.contains("Expedite changed: yes")) {
                summary.frameworkAppCheckboxPatchObserved = true;
            }
            if (line.contains("\"type\":\"patch\"")
                    && line.contains("\"source\":\"UidlRequestHandler\"")
                    && line.contains("\"reason\":\"event\"")
                    && line.contains("Priority changed: audit")) {
                summary.frameworkAppSelectPatchObserved = true;
            }
            if (line.contains("\"type\":\"patch\"")
                    && line.contains("\"source\":\"UidlRequestHandler\"")
                    && line.contains("\"reason\":\"event\"")
                    && line.contains("Revision 1:")
                    && line.contains("priority audit")
                    && line.contains("Renderer owned by Flow client engine")) {
                summary.frameworkAppApplyPatchObserved = true;
            }
            if (line.contains("\"type\":\"command\"")
                    && line.contains("\"command\":\"window.setTitle\"")
                    && line.contains("Flow IPC Framework App - 1")) {
                summary.frameworkAppWindowTitleCommandObserved = true;
            }
            if (line.contains("\"type\":\"command\"")
                    && line.contains("\"command\":\"clipboard.writeText\"")
                    && line.contains("Revision 1:")) {
                summary.frameworkAppClipboardCommandObserved = true;
            }
            if (line.contains("\"type\":\"command\"")
                    && line.contains("\"command\":\"notification.show\"")
                    && line.contains("Framework update 1 applied")) {
                summary.frameworkAppNotificationCommandObserved = true;
            }
            if (line.contains("\"type\":\"capabilityResult\"")
                    && line.contains("\"command\":\"window.setTitle\"")
                    && line.contains("\"permissionDecision\":\"allowed\"")
                    && line.contains("\"success\":true")) {
                summary.frameworkAppWindowTitleResultObserved = true;
            }
            if (line.contains("\"type\":\"capabilityResult\"")
                    && line.contains("\"command\":\"clipboard.writeText\"")
                    && line.contains("\"permissionDecision\":\"allowed\"")
                    && line.contains("\"success\":true")) {
                summary.frameworkAppClipboardResultObserved = true;
            }
            if (line.contains("\"type\":\"capabilityResult\"")
                    && line.contains("\"command\":\"notification.show\"")
                    && line.contains("\"permissionDecision\":\"allowed\"")
                    && line.contains("\"success\":true")) {
                summary.frameworkAppNotificationResultObserved = true;
            }
            if (line.contains("\"type\":\"event\"")
                    && line.contains("\"dispatch\":\"flow-client-xhr\"")
                    && line.contains("\"componentId\":\"ipc-text-input\"")
                    && line.contains("\"event\":\"change\"")
                    && line.contains("element.value")
                    && line.contains("Flow IPC typed")) {
                summary.flowClientXhrTextInputObserved = true;
            }
            if (line.contains("\"type\":\"event\"")
                    && line.contains("\"dispatch\":\"flow-client-xhr\"")
                    && line.contains("\"componentId\":\"ipc-checkbox\"")
                    && line.contains("\"event\":\"change\"")
                    && line.contains("element.checked")) {
                summary.flowClientXhrCheckboxObserved = true;
            }
            if (line.contains("\"type\":\"event\"")
                    && line.contains("\"dispatch\":\"flow-client-xhr\"")
                    && line.contains("\"componentId\":\"ipc-vaadin-big-decimal\"")
                    && line.contains("\"event\":\"change\"")
                    && line.contains("element.value")
                    && line.contains("42.5")
                    && line.contains("\"uidlRpc\"")) {
                summary.vaadinWebComponentEventRpcObserved = true;
            }
            if (line.contains("\"type\":\"event\"")
                    && line.contains("\"dispatch\":\"flow-client-xhr\"")
                    && line.contains("\"componentId\":\"ipc-vaadin-text-field\"")
                    && line.contains("\"event\":\"change\"")
                    && line.contains("element.value")
                    && line.contains("Flow IPC typed")
                    && line.contains("\"uidlRpc\"")) {
                summary.vaadinTextFieldEventRpcObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"securityKey\"")
                    && line.contains("Vaadin-Security-Key")
                    && line.contains("\"containsSecurityKey\":true")) {
                summary.securityKeyObserved = true;
            }
            if (line.contains("\"type\":\"resync\"")
                    && line.contains("\"securityKey\"")
                    && line.contains("\"syncId\"")
                    && line.contains("\"clientId\"")) {
                summary.resyncSecurityKeyObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapRequest\"")
                    && line.contains("\"sessionId\":\"flow-ipc-session\"")
                    && line.contains("\"uiId\":\"main\"")) {
                summary.bootstrapScopeObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"flowClientResources\"")
                    && line.contains("\"flow-client-classpath\"")
                    && line.contains("META-INF/frontend/FlowBootstrap.js")) {
                summary.flowClientResourceManifestObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"flowClientResources\"")
                    && line.contains("\"flow-client-classpath\"")
                    && line.contains("META-INF/frontend/FlowClient.js")
                    && line.contains("\"ipcAugmented\":true")) {
                summary.flowClientEngineResourceManifestObserved = true;
            }
            if (line.contains("\"type\":\"resourceRequest\"")
                    && line.contains("META-INF/frontend/FlowBootstrap.js")
                    && line.contains("\"securityKey\"")) {
                summary.flowClientResourceRequestObserved = true;
            }
            if (line.contains("\"type\":\"resourceRequest\"")
                    && line.contains("META-INF/frontend/FlowClient.js")
                    && line.contains("\"securityKey\"")) {
                summary.flowClientEngineResourceRequestObserved = true;
            }
            if (line.contains("\"type\":\"resourceRequest\"")
                    && line.contains("\"purpose\":\"flow-client-lazy-lookup\"")
                    && line.contains("\"lookupKind\":\"Flow.loadOnDemand-minimum-probe\"")
                    && containsPath(line, "META-INF/frontend/Flow.js")
                    && line.contains("\"securityKey\"")) {
                summary.flowClientResourceLazyLookupRequestObserved = true;
            }
            if (line.contains("\"type\":\"resourceResponse\"")
                    && line.contains("\"flowClientResource\":true")
                    && line.contains("META-INF/frontend/FlowBootstrap.js")
                    && line.contains("\"bodyRedacted\":true")) {
                summary.flowClientResourceResponseObserved = true;
            }
            if (line.contains("\"type\":\"resourceResponse\"")
                    && line.contains("\"flowClientResource\":true")
                    && line.contains("META-INF/frontend/FlowClient.js")
                    && line.contains("\"bodyRedacted\":true")) {
                summary.flowClientEngineResourceResponseObserved = true;
            }
            if (line.contains("\"type\":\"resourceResponse\"")
                    && line.contains("\"flowClientResource\":true")
                    && line.contains("\"requestPurpose\":\"flow-client-lazy-lookup\"")
                    && line.contains("\"lookupKind\":\"Flow.loadOnDemand-minimum-probe\"")
                    && containsPath(line, "META-INF/frontend/Flow.js")
                    && line.contains("\"bodyRedacted\":true")) {
                summary.flowClientResourceLazyLookupResponseObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"componentFrontendResources\"")
                    && (line.contains("\"vaadin-component-classpath-plus-v1_2-shims\"")
                            || line.contains("\"vaadin-component-classpath-plus-dev-bundle-v1_4\""))
                    && line.contains("com.vaadin.flow.component.textfield.BigDecimalField")
                    && line.contains("vaadin-big-decimal-field")
                    && (line.contains("npm-transitive-imports-shimmed")
                            || line.contains("textfield-production-class-execution-blocked")
                            || line.contains("real-textfield-production-graph-executed-v1_7"))
                    && line.contains("blockedReason")) {
                summary.vaadinWebComponentResourceManifestObserved = true;
                summary.vaadinWebComponentFailureClassificationObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"componentFrontendResources\"")
                    && line.contains("\"resolverStrategy\":\"generic-component-frontend-manifest-v1_3\"")
                    && (line.contains("\"componentCount\":2")
                            || line.contains("\"componentCount\":3")
                            || line.contains("\"componentCount\":4"))
                    && line.contains("com.vaadin.flow.component.checkbox.Checkbox")
                    && line.contains("vaadin-checkbox")
                    && (line.contains("META-INF/vatori/frontend-shims/vaadin-checkbox.js")
                            || line.contains(
                                    "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                            || line.contains("META-INF/vatori/dev-bundle-entry/vaadin-checkbox.js"))) {
                summary.componentResolverV13ManifestObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"componentFrontendResources\"")
                    && line.contains("\"frontendAssetPipelineVersion\":\"flow-ipc-frontend-assets-v1_4\"")
                    && line.contains("\"artifactGraphSource\":\"vaadin-frontend-artifact-graph-v1_4\"")) {
                summary.frontendAssetPipelineV14Observed = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"componentFrontendResources\"")
                    && containsStringField(line, "generatedLazyResolverVersion",
                            "flow-ipc-generated-lazy-resolver-v1_8")
                    && containsStringField(line, "generatedLazyExecutionVersion",
                            "flow-ipc-generated-lazy-v1_8")
                    && containsStringField(line, "generatedLazyImportResolutionStrategy",
                            "stable-relative-import-rewrite-generated-lazy-v1_8")
                    && containsStringField(line, "generatedLazyRelativeImportSpecifier",
                            "./markdown-helpers-31I0A-hg.js")
                    && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/markdown-helpers-31I0A-hg.js")) {
                summary.frontendGeneratedLazyV18ManifestObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"componentFrontendResources\"")
                    && containsStringField(line, "generatedLazyLoadOnDemandContractVersion",
                            "flow-ipc-loadondemand-v1_9")
                    && containsStringField(line, "generatedLazyTriggerSource",
                            "diagnostic-preflight-and-direct-staged-import")
                    && containsStringField(line, "generatedLazyApiOwner", "window.Vaadin.Flow.loadOnDemand")
                    && containsStringField(line, "generatedLazyApiInstallSource",
                            "generated-flow-imports-dev-bundle-after-direct-staged-import")
                    && containsStringField(line, "generatedLazyRequestedChunk",
                            "vatori-flow-ipc-generated-lazy-v1_9-probe")
                    && containsStringField(line, "generatedLazyRegistrySource",
                            "no-bootstrap-lazy-registry-observed")
                    && containsStringField(line, "generatedLazyMappingSource",
                            "component-frontend-manifest-generated-lazy-v1_9")
                    && containsStringField(line, "generatedLazyBlockerVersion",
                            "flow-ipc-loadondemand-v1_9")
                    && containsStringField(line, "generatedLazyFailureLayer",
                            "loadondemand-api-installed-by-generated-imports-not-flow-client-triggered")
                    && containsLiteralField(line, "generatedLazyFlowClientTriggered", "false")
                    && containsLiteralField(line, "generatedLazyDiagnosticBridge", "true")
                    && containsPath(line,
                            "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")) {
                summary.frontendGeneratedLazyV19ManifestObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"componentFrontendResources\"")
                    && containsStringField(line, "runtimeResolverContractVersion",
                            "flow-ipc-frontend-runtime-resolver-v1_10")
                    && line.contains("\"logicalRequestId\"")
                    && line.contains("\"pathMapping\"")) {
                summary.frontendRuntimeResolverV110ManifestObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"componentFrontendResources\"")
                    && containsStringField(line, "flowLazyDependencyVersion",
                            "flow-ipc-lazy-dependency-v1_10")
                    && containsStringField(line, "flowLazyDependencySource",
                            "flow-uidl-dependencyloader-loadmode-lazy-v1_10")
                    && containsStringField(line, "flowLazyDependencyMode", "LoadMode.LAZY")
                    && containsStringField(line, "flowLazyDependencyExpression",
                            "context://vatori-flow-ipc-lazy-v1_10.js")
                    && containsStringField(line, "flowLazyDependencyResolvedPath",
                            "vatori-flow-ipc-lazy-v1_10.js")
                    && containsLiteralField(line, "flowLazyDependencyTriggeredByFlowClient", "true")) {
                summary.flowLazyDependencyV110ManifestObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"componentFrontendResources\"")
                    && containsStringField(line, "productionBundleRuntimeVersion",
                            "flow-ipc-production-runtime-v1_10")
                    && containsStringField(line, "productionBundleRuntimeRegistrySource",
                            "vaadin-dev-bundle-stats-runtime-registry-v1_10")
                    && containsStringField(line, "productionBundleRuntimeSelectedArtifact",
                            "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && containsStringField(line, "productionBundleRuntimeBlockerVersion",
                            "flow-ipc-production-runtime-v1_10")) {
                summary.productionRuntimeV110ManifestObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"uidl\"")
                    && line.contains("return window.Vaadin.Flow.loadOnDemand('")) {
                summary.generatedLoadOnDemandChunkV111UidlExpressionObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"componentFrontendResources\"")
                    && containsStringField(line, "loadOnDemandChunkContractVersion",
                            "flow-ipc-loadondemand-chunk-v1_11")
                    && containsStringField(line, "generatedLazyChunkExpressionSource",
                            "bootstrap-uidl-dependencies")
                    && containsStringField(line, "generatedLazyChunkTriggerOwner",
                            "visible-flow-client-dependencyloader")
                    && containsStringField(line, "loadOnDemandChunkRegistrySource",
                            "uidl-expression-to-dev-bundle-generated-flow-imports-registry-v1_11")
                    && containsStringField(line, "loadOnDemandChunkMappingSource",
                            "flow-ipc-generated-loadondemand-chunk-map-v1_11")
                    && containsStringField(line, "loadOnDemandChunkBlockerVersion",
                            "flow-ipc-loadondemand-chunk-v1_11")
                    && containsLiteralField(line, "generatedLazyChunkDiagnosticBridgeUsed", "false")
                    && containsLiteralField(line, "generatedLazyChunkDirectImportProbeUsed", "false")
                    && containsPath(line,
                            "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")) {
                summary.generatedLoadOnDemandChunkV111ManifestObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"componentFrontendResources\"")
                    && containsStringField(line, "generatedLoadOnDemandChunkFileRegistryVersion",
                            "flow-ipc-loadondemand-chunk-file-v1_12")
                    && containsStringField(line, "generatedLoadOnDemandChunkFileRegistrySource",
                            "generated-flow-imports-body-parser-v1_12")
                    && containsStringField(line, "generatedLoadOnDemandChunkFileMappingSource",
                            "real-generated-flow-imports-loadondemand-table-v1_12")
                    && containsStringField(line, "generatedLoadOnDemandGeneratedImportsArtifact",
                            "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                    && containsLiteralField(line, "generatedLoadOnDemandNoOpLoadOnDemand", "true")
                    && containsStringField(line, "loadOnDemandTableShape", "no-op-promise-resolve-0")
                    && containsStringField(line, "chunkFileResolver",
                            "generated-flow-imports-relative-chunk-file-resolver-v1_12")
                    && containsStringField(line, "chunkFileStatus", "not-generated")
                    && containsStringField(line, "loadOnDemandChunkFileFailureLayer", "chunk-file-not-generated")
                    && containsStringField(line, "loadOnDemandChunkFileBlockerVersion",
                            "flow-ipc-loadondemand-chunk-file-v1_12")
                    && line.contains("noChunkGeneratedReason")) {
                summary.generatedLoadOnDemandChunkFileV112ManifestObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"componentFrontendResources\"")
                    && containsStringField(line, "productionBundleRuntimeOwnerVersion",
                            "flow-ipc-production-owner-v1_13")
                    && containsStringField(line, "productionBundleRuntimeOwner",
                            "flow-ipc-production-bundle-runtime")
                    && containsStringField(line, "productionBundleRuntimeEntryArtifact",
                            "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && containsStringField(line, "productionBundleRuntimeSource",
                            "vaadin-dev-bundle-stats")
                    && containsStringField(line, "productionBundleRuntimeSelectedBy",
                            "stats-entrypoint-or-focused-production-entry")
                    && containsStringField(line, "productionBundleRuntimeOwnerModuleIdentity",
                            "production-owner-v1_13:vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && containsStringField(line, "productionBundleRuntimeOwnerExecutionPolicy",
                            "stage-owner-contract-production-bundle-runtime-v1_13")
                    && containsStringField(line, "productionBundleRuntimeOwnerBlockerVersion",
                            "flow-ipc-production-owner-v1_13")
                    && containsProductionOwnerFailureLayer(line, "productionBundleRuntimeOwnerFailureLayer")
                    && containsLiteralField(line, "componentProductionOwnerReused", "false")) {
                summary.productionRuntimeOwnerV113ManifestObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"componentFrontendResources\"")
                    && containsStringField(line, "productionBundleRuntimeOwnerImportVersion",
                            "flow-ipc-production-owner-import-v1_14")
                    && containsStringField(line, "productionBundleRuntimeOwnerImportPolicy",
                            "controlled-owner-dynamic-import-after-component-graph-v1_14")
                    && containsStringField(line, "productionBundleRuntimeOwnerImportArtifact",
                            "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && line.contains("productionBundleRuntimeOwnerImportExpectedSideEffects")
                    && line.contains("productionBundleRuntimeOwnerImportAcceptableFailures")
                    && line.contains("production-owner-browser-import-failed")
                    && line.contains("production-owner-duplicate-custom-element")
                    && line.contains("production-owner-side-effect-unobservable")
                    && containsLiteralField(line, "productionBundleRuntimeOwnerImportAttempted", "false")
                    && containsLiteralField(line, "productionBundleRuntimeOwnerImported", "false")) {
                summary.productionRuntimeOwnerImportV114ManifestObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"componentFrontendResources\"")
                    && containsStringField(line, "productionBundleRuntimeOwnerDependencyResolverVersion",
                            "flow-ipc-production-owner-deps-v1_15")
                    && containsStringField(line, "productionBundleRuntimeOwnerDependencyResolverRoot",
                            "vaadin-dev-bundle/webapp/VAADIN/build/")
                    && containsStringField(line, "productionBundleRuntimeOwnerDependencyResolverPolicy",
                            "owner-scoped-recursive-relative-import-graph-v1_15")
                    && containsStringField(line,
                            "productionBundleRuntimeOwnerDependencyResolverModuleIdentityNamespace",
                            "production-owner-deps-v1_15")
                    && line.contains("productionBundleRuntimeOwnerImportPhase")
                    && line.contains("productionBundleRuntimeOwnerImportPhaseCandidates")
                    && line.contains("isolated-preflight")
                    && line.contains("before-component-imports")
                    && line.contains("productionBundleRuntimeOwnerImportDiagnosticModes")
                    && line.contains("productionBundleRuntimeOwnerDependencyResolverExpectedArtifacts")
                    && containsPath(line,
                            "vaadin-dev-bundle/webapp/VAADIN/build/copilot-log-plugin-h3VQq9-2-BlujgixT.js")
                    && containsPath(line,
                            "vaadin-dev-bundle/webapp/VAADIN/build/FlowClient-RTzSA_ve.js")
                    && line.contains("productionBundleRuntimeOwnerDependencyResolverAcceptableFailures")
                    && line.contains("production-owner-missing-real-artifact")
                    && line.contains("flow-ipc-production-owner-deps-v1_15")
                    && containsLiteralField(line, "productionBundleRuntimeOwnerDependencyGraphClosed", "false")) {
                summary.productionRuntimeOwnerDependenciesV115ManifestObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"componentFrontendResources\"")
                    && containsStringField(line, "productionBundleRuntimeOwnerImportOrderingVersion",
                            "flow-ipc-production-owner-ordering-v1_16")
                    && containsStringField(line, "productionBundleRuntimeOwnerImportPhasePolicy",
                            "owner-first-before-component-imports-with-after-component-fallback-v1_16")
                    && line.contains("productionBundleRuntimeOwnerImportOrderingAcceptableFailures")
                    && line.contains("production-owner-component-reuse-contract-missing")
                    && containsStringField(line, "componentProductionOwnerReuseVersion",
                            "flow-ipc-component-owner-reuse-v1_16")
                    && containsStringField(line, "componentProductionOwnerReusePolicy",
                            "reuse-owner-owned-custom-elements-before-focused-imports-v1_16")
                    && line.contains("componentProductionOwnerReuseAcceptableFailures")
                    && line.contains("owner-import-failed-before-component-import")
                    && containsLiteralField(line, "componentProductionOwnerReused", "false")) {
                summary.productionRuntimeOwnerOrderingV116ManifestObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"artifactFamilies\"")
                    && line.contains("flow-client-classpath")
                    && line.contains("vaadin-component-classpath-frontend")
                    && line.contains("npm-frontend")
                    && line.contains("theme-static")
                    && line.contains("production-bundle")
                    && line.contains("dev-mode-bundle")
                    && line.contains("generated-lazy")) {
                summary.frontendArtifactFamilyModelObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"dependencyGraph\"")
                    && line.contains("\"dependencyOf\"")
                    && line.contains("\"executionPolicy\"")
                    && line.contains("\"requestedBy\":\"component-frontend-manifest-v1_4\"")) {
                summary.frontendDependencyGraphObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"artifactFamily\":\"npm-frontend\"")
                    && (line.contains("\"executionPolicy\":\"blocked-shim-fallback\"")
                            || line.contains("\"executionPolicy\":\"legacy-shim-fallback\""))
                    && (line.contains("\"artifactStatus\":\"blocked-fallback\"")
                            || line.contains("\"artifactStatus\":\"legacy-fallback\""))
                    && (line.contains("\"failureClassification\":\"npm-transitive-imports-shimmed\"")
                            || line.contains("\"failureClassification\":\"textfield-production-class-execution-blocked\"")
                            || line.contains("\"failureClassification\":\"legacy-textfield-shim-fallback-not-success\""))) {
                summary.frontendShimBlockerObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"componentFrontendResources\"")
                    && line.contains("\"importRewriteStrategy\":\"manifest-import-rewrite-v1_4\"")
                    && line.contains("\"browserExecutionPlan\":\"dynamic-import-blob-with-manifest-import-rewrites-v1_4\"")
                    && line.contains("\"importRewrites\"")
                    && line.contains("\"specifier\":\"lit\"")
                    && line.contains("\"specifier\":\"@vaadin/text-field/src/vaadin-text-field.js\"")
                    && (line.contains("\"resourcePath\":\"META-INF/vatori/frontend-shims/lit.js\"")
                            || line.contains("\"resourcePath\":\"META-INF/vatori/dev-bundle-entry/lit-css.js\""))) {
                summary.frontendManifestImportRewriteObserved = true;
            }
            if ((line.contains("\"type\":\"bootstrapResponse\"") || line.contains("\"type\":\"resourceResponse\""))
                    && line.contains("\"lookupKind\":\"TextField-dev-bundle-resource-v1_4\"")
                    && (line.contains("\"blockedExecutionProbe\":\"big-decimal-import-rewrite-to-textfield-dev-bundle\"")
                            || line.contains("\"blockedExecutionProbe\":\"legacy-v1_6-big-decimal-import-rewrite-to-textfield-dev-bundle\""))
                    && (line.contains("\"blockedExecutionError\":\"null is not an object (evaluating 'outlet._root')\"")
                            || line.contains("\"blockedExecutionError\":\"resolved-by-stable-module-identity-v1_7\""))
                    && line.contains("\"nextResolutionStep\"")) {
                summary.frontendTextFieldDevBundleBlockerObserved = true;
            }
            if ((line.contains("\"type\":\"bootstrapResponse\"") || line.contains("\"type\":\"resourceResponse\""))
                    && line.contains("\"lookupKind\":\"TextField-dev-bundle-resource-v1_4\"")
                    && line.contains("\"textfieldGraphParityVersion\":\"flow-ipc-textfield-graph-v1_6\"")
                    && line.contains("\"previousBlockerVersion\":\"flow-ipc-textfield-v1_6\"")
                    && line.contains("\"executionVersion\":\"flow-ipc-textfield-v1_7\"")
                    && line.contains("\"textfieldSideEffectVersion\":\"flow-ipc-textfield-side-effect-v1_7\"")
                    && line.contains("\"narrowedFailureClassification\":\"textfield-production-indexhtml-side-effect-deduped\"")
                    && line.contains("\"failureLayer\":\"resolved-v1_6-custom-element-registry-conflict\"")
                    && line.contains("\"classExportArtifact\":\"vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js\"")
                    && line.contains("\"duplicateTag\":\"vaadin-text-field\"")
                    && line.contains("\"remainingBlockerClassification\":\"none\"")
                    && line.contains("\"moduleIdentityStrategy\":\"stable-resource-path-blob-module-identity-v1_7\"")
                    && line.contains("\"legacyShimSuccessPath\":false")
                    && !line.contains("META-INF/vatori/frontend-shims/vaadin-text-field.js")) {
                summary.frontendTextFieldV17ManifestObserved = true;
            }
            if ((line.contains("\"type\":\"bootstrapResponse\"") || line.contains("\"type\":\"resourceResponse\""))
                    && line.contains("\"lookupKind\":\"TextField-dev-bundle-resource-v1_4\"")
                    && line.contains("\"textfieldGraphParityVersion\":\"flow-ipc-textfield-graph-v1_6\"")
                    && line.contains("\"blockerVersion\":\"flow-ipc-textfield-v1_6\"")
                    && line.contains("\"narrowedFailureClassification\":\"textfield-production-indexhtml-duplicate-custom-element-definition\"")
                    && line.contains("\"failureLayer\":\"custom-element-registry-conflict-after-production-indexhtml-reimport\"")
                    && line.contains("\"minimalReproduction\"")
                    && line.contains("indexhtml-BfYYZi8S.js")
                    && line.contains("\"nextResolutionStep\"")) {
                summary.frontendTextFieldV16ManifestObserved = true;
            }
            if (line.contains("\"type\":\"resourceRequest\"")
                    && line.contains("META-INF/vatori/dev-bundle-entry/vaadin-text-field.js")
                    && line.contains("\"lookupKind\":\"TextField-dev-bundle-resource-v1_4\"")
                    && line.contains("\"blockerVersion\":\"flow-ipc-textfield-v1_6\"")
                    && line.contains("\"narrowedFailureClassification\":\"textfield-production-indexhtml-duplicate-custom-element-definition\"")
                    && line.contains("\"transitiveImports\"")) {
                summary.frontendTextFieldV16RequestObserved = true;
            }
            if (line.contains("\"type\":\"resourceRequest\"")
                    && line.contains("META-INF/vatori/dev-bundle-entry/vaadin-text-field.js")
                    && line.contains("\"lookupKind\":\"TextField-dev-bundle-resource-v1_4\"")
                    && line.contains("\"previousBlockerVersion\":\"flow-ipc-textfield-v1_6\"")
                    && line.contains("\"executionVersion\":\"flow-ipc-textfield-v1_7\"")
                    && line.contains("\"textfieldSideEffectVersion\":\"flow-ipc-textfield-side-effect-v1_7\"")
                    && line.contains("\"classExportArtifact\":\"vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js\"")
                    && line.contains("\"importResolutionStrategy\":\"stable-resource-path-blob-module-identity-v1_7\"")
                    && line.contains("\"transitiveImports\"")
                    && !line.contains("META-INF/vatori/frontend-shims/vaadin-text-field.js")) {
                summary.frontendTextFieldV17RequestObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"artifactFamily\":\"production-bundle\"")
                    && line.contains("\"executionPolicy\":\"execute-dev-bundle-entry\"")
                    && line.contains("\"executionPolicy\":\"dependency-module\"")
                    && line.contains("\"failureClassification\":\"dev-bundle-resource-graph-partial\"")) {
                summary.frontendProductionBundlePartialObserved = true;
            }
            if ((line.contains("\"type\":\"bootstrapResponse\"") || line.contains("\"type\":\"resourceResponse\""))
                    && line.contains("\"artifactFamily\":\"production-bundle\"")
                    && line.contains("\"artifactStatus\":\"available\"")
                    && line.contains("\"lookupKind\":\"Production-bundle-stats-resource-v1_4\"")) {
                summary.frontendProductionBundleStatsResourceObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"artifactFamily\":\"theme-static\"")
                    && line.contains("\"strictBlocker\":true")
                    && line.contains("\"failureClassification\":\"theme-static-resource-not-declared\"")) {
                summary.frontendThemeStaticStrictBlockerObserved = true;
            }
            if ((line.contains("\"type\":\"bootstrapResponse\"") || line.contains("\"type\":\"resourceResponse\""))
                    && line.contains("\"artifactFamily\":\"theme-static\"")
                    && line.contains("\"artifactStatus\":\"available\"")
                    && line.contains("\"lookupKind\":\"Theme-static-resource-v1_4\"")) {
                summary.frontendThemeStaticResourceObserved = true;
            }
            if ((line.contains("\"type\":\"bootstrapResponse\"") || line.contains("\"type\":\"resourceResponse\""))
                    && line.contains("\"artifactFamily\":\"generated-lazy\"")
                    && line.contains("\"artifactStatus\":\"available\"")
                    && line.contains("\"lookupKind\":\"Generated-flow-imports-resource-v1_4\"")) {
                summary.frontendGeneratedLazyResourceObserved = true;
            }
            if ((line.contains("\"type\":\"bootstrapResponse\"") || line.contains("\"type\":\"resourceResponse\""))
                    && line.contains("\"artifactFamily\":\"generated-lazy\"")
                    && line.contains("\"artifactStatus\":\"available\"")
                    && line.contains("\"lookupKind\":\"Generated-flow-imports-dev-bundle-chunk-v1_4\"")
                    && line.contains("\"generatedLazyBundleChunk\":true")
                    && line.contains("vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")) {
                summary.frontendGeneratedLazyBundleChunkResourceObserved = true;
            }
            if ((line.contains("\"type\":\"bootstrapResponse\"") || line.contains("\"type\":\"resourceResponse\""))
                    && line.contains("\"artifactFamily\":\"dev-mode-bundle\"")
                    && line.contains("\"artifactStatus\":\"available\"")
                    && line.contains("\"lookupKind\":\"Dev-mode-package-lock-resource-v1_4\"")) {
                summary.frontendDevModeResourceObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"artifactFamily\":\"dev-mode-bundle\"")
                    && line.contains("\"strictBlocker\":true")
                    && (line.contains("\"failureClassification\":\"dev-mode-bundle-resource-graph-unmapped\"")
                            || line.contains("\"failureClassification\":\"dev-mode-bundle-execution-unmapped\""))) {
                summary.frontendDevModeStrictBlockerObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"artifactFamily\":\"generated-lazy\"")
                    && line.contains("\"strictBlocker\":true")
                    && (line.contains("\"failureClassification\":\"generated-lazy-resource-unmapped\"")
                            || line.contains("\"failureClassification\":\"generated-lazy-execution-unmapped\"")
                            || line.contains(
                                    "\"failureClassification\":\"generated-lazy-loadondemand-api-boundary-v1_9\""))) {
                summary.frontendGeneratedLazyStrictBlockerObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "frontendArtifactGraphManifestObserved", "true")
                    && containsStringField(line, "frontendAssetPipelineVersion", "flow-ipc-frontend-assets-v1_4")
                    && containsStringField(line, "frontendArtifactGraphSource",
                            "vaadin-frontend-artifact-graph-v1_4")) {
                summary.frontendAssetPipelineV14Observed = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && line.contains("frontendArtifactFamilies")
                    && line.contains("flow-client-classpath")
                    && line.contains("vaadin-component-classpath-frontend")
                    && line.contains("npm-frontend")
                    && line.contains("theme-static")
                    && line.contains("production-bundle")
                    && line.contains("dev-mode-bundle")
                    && line.contains("generated-lazy")) {
                summary.frontendArtifactFamilyModelObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "frontendArtifactGraphDependencyObserved", "true")
                    && line.contains("frontendArtifactGraphRequestedBy")
                    && line.contains("frontendArtifactGraphDependencyOf")
                    && line.contains("frontendArtifactGraphModuleSpecifiers")
                    && line.contains("frontendArtifactExecutionPolicies")) {
                summary.frontendDependencyGraphObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "frontendArtifactShimFallback", "true")) {
                summary.frontendShimBlockerObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsStringField(line, "vaadinWebComponentImportRewriteStrategy",
                            "manifest-import-rewrite-v1_4")
                    && containsPositiveIntegerField(line, "vaadinWebComponentImportRewriteCount")
                    && line.contains("vaadinWebComponentImportRewriteSpecifiers")
                    && line.contains("vaadinWebComponentImportRewritePaths")) {
                summary.frontendManifestImportRewriteObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "frontendArtifactProductionBundlePartial", "true")) {
                summary.frontendProductionBundlePartialObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "frontendArtifactProductionBundleStatsVerified", "true")
                    && line.contains("frontendArtifactProductionBundleStatsPath")) {
                summary.frontendProductionBundleStatsResourceObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "frontendArtifactThemeStaticVerified", "true")) {
                summary.frontendThemeStaticResourceObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "frontendArtifactGeneratedLazyVerified", "true")
                    && line.contains("frontendArtifactGeneratedLazyPath")) {
                summary.frontendGeneratedLazyResourceObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "frontendArtifactGeneratedLazyBundleChunkVerified", "true")
                    && line.contains("frontendArtifactGeneratedLazyBundleChunkPath")
                    && line.contains("vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")) {
                summary.frontendGeneratedLazyBundleChunkResourceObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "frontendArtifactDevModeVerified", "true")
                    && line.contains("frontendArtifactDevModePath")) {
                summary.frontendDevModeResourceObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "frontendArtifactThemeStaticBlocker", "true")) {
                summary.frontendThemeStaticStrictBlockerObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "frontendArtifactDevModeBlocker", "true")) {
                summary.frontendDevModeStrictBlockerObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "frontendArtifactGeneratedLazyBlocker", "true")) {
                summary.frontendGeneratedLazyStrictBlockerObserved = true;
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && line.contains("\"stagingResolverVersion\":\"flow-ipc-resource-staging-v1_5\"")
                    && line.contains("\"sourcePath\"")
                    && line.contains("\"stagedUrlScheme\"")
                    && line.contains("\"artifactFamily\"")
                    && line.contains("\"moduleSpecifier\"")
                    && line.contains("\"sha256\"")
                    && line.contains("\"bytes\"")
                    && line.contains("\"dependencyOf\"")
                    && line.contains("\"requestedBy\"")
                    && line.contains("\"cacheStatus\"")
                    && line.contains("\"cleanupPolicy\"")
                    && line.contains("\"executionStatus\"")) {
                summary.frontendResourceStageObserved = true;
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && line.contains("\"executionStatus\":\"executed\"")) {
                summary.frontendResourceStageExecutedObserved = true;
                if (line.contains("\"failureClassification\":\"")
                        && !line.contains("\"failureClassification\":\"\"")) {
                    summary.frontendResourceStageFailureClassificationObserved = true;
                }
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && line.contains("\"executionStatus\":\"strict-blocker\"")
                    && line.contains("\"browserError\"")) {
                summary.frontendResourceStageStrictBlockerObserved = true;
                if (line.contains("\"failureClassification\":\"")
                        && !line.contains("\"failureClassification\":\"\"")) {
                    summary.frontendResourceStageFailureClassificationObserved = true;
                }
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && line.contains("\"lookupKind\":\"TextField-dev-bundle-resource-v1_4\"")
                    && line.contains("\"executionStatus\":\"executed\"")) {
                summary.frontendTextFieldStagedExecutionObserved = true;
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && line.contains("\"lookupKind\":\"TextField-dev-bundle-resource-v1_4\"")
                    && line.contains("\"sourcePath\":\"META-INF/vatori/dev-bundle-entry/vaadin-text-field.js\"")
                    && line.contains("\"executionStatus\":\"executed\"")
                    && line.contains("\"previousBlockerVersion\":\"flow-ipc-textfield-v1_6\"")
                    && line.contains("\"executionVersion\":\"flow-ipc-textfield-v1_7\"")
                    && line.contains("\"textfieldSideEffectVersion\":\"flow-ipc-textfield-side-effect-v1_7\"")
                    && line.contains("\"classExportArtifact\":\"vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js\"")
                    && line.contains("\"moduleIdentityStrategy\":\"stable-resource-path-blob-module-identity-v1_7\"")
                    && line.contains("\"customElementTag\":\"vaadin-text-field\"")
                    && line.contains("\"duplicateTag\":\"vaadin-text-field\"")
                    && line.contains("\"remainingBlockerClassification\":\"none\"")
                    && !line.contains("META-INF/vatori/frontend-shims/vaadin-text-field.js")) {
                summary.frontendTextFieldV17StageExecutedObserved = true;
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && line.contains("vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && line.contains("\"moduleIdentityStrategy\":\"stable-resource-path-blob-module-identity-v1_7\"")
                    && (line.contains("\"executionStatus\":\"execution-attempt\"")
                            || line.contains("\"executionStatus\":\"staged\"")
                            || line.contains("\"executionStatus\":\"executed\""))) {
                summary.frontendTextFieldV17ModuleIdentityDependencyObserved = true;
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && line.contains("\"lookupKind\":\"TextField-dev-bundle-resource-v1_4\"")
                    && line.contains("\"executionStatus\":\"strict-blocker\"")
                    && line.contains("\"browserError\"")
                    && line.contains("\"failureClassification\":\"textfield-production-class-execution-blocked\"")) {
                summary.frontendTextFieldStagedBlockerObserved = true;
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && line.contains("\"lookupKind\":\"TextField-dev-bundle-resource-v1_4\"")
                    && line.contains("\"sourcePath\":\"META-INF/vatori/dev-bundle-entry/vaadin-text-field.js\"")
                    && line.contains("\"executionStatus\":\"strict-blocker\"")
                    && line.contains("\"blockerVersion\":\"flow-ipc-textfield-v1_6\"")
                    && line.contains("\"textfieldGraphParityVersion\":\"flow-ipc-textfield-graph-v1_6\"")
                    && line.contains("\"narrowedFailureClassification\":\"textfield-production-indexhtml-duplicate-custom-element-definition\"")
                    && line.contains("\"failureLayer\":\"custom-element-registry-conflict-after-production-indexhtml-reimport\"")
                    && line.contains("\"importResolutionStrategy\":\"blob-module-rewrite-dev-bundle-entry-plus-indexhtml-dependency-v1_6\"")
                    && line.contains("\"customElementTag\":\"vaadin-text-field\"")
                    && line.contains("\"customElementRegistryBefore\"")
                    && line.contains("\"customElementRegistryAfter\"")
                    && line.contains("\"minimalReproduction\"")
                    && line.contains("\"nextResolutionStep\"")
                    && line.contains("\"browserError\"")
                    && !line.contains("META-INF/vatori/frontend-shims/vaadin-text-field.js")) {
                summary.frontendTextFieldV16StageBlockerObserved = true;
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && line.contains("vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && line.contains("\"executionStatus\":\"strict-blocker\"")
                    && line.contains("\"blockerVersion\":\"flow-ipc-textfield-v1_6\"")
                    && line.contains("\"failureClassification\":\"")
                    && !line.contains("\"failureClassification\":\"\"")
                    && line.contains("\"browserError\"")) {
                summary.frontendTextFieldV16DependencyClassificationObserved = true;
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && line.contains("\"artifactFamily\":\"generated-lazy\"")
                    && line.contains("vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                    && (line.contains("\"executionStatus\":\"execution-attempt\"")
                            || line.contains("\"executionStatus\":\"executed\"")
                            || line.contains("\"executionStatus\":\"strict-blocker\""))) {
                summary.frontendGeneratedLazyExecutionAttemptObserved = true;
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && line.contains("\"artifactFamily\":\"generated-lazy\"")
                    && line.contains("vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                    && (line.contains("\"executionStatus\":\"executed\"")
                            || (line.contains("\"executionStatus\":\"strict-blocker\"")
                                    && line.contains("\"browserError\"")))) {
                summary.frontendGeneratedLazyExecutionResolvedObserved = true;
            }
            if (line.contains("\"type\":\"resourceRequest\"")
                    && line.contains("\"purpose\":\"vaadin-web-component-resource-probe\"")
                    && line.contains("\"lookupKind\":\"Generated-flow-imports-dev-bundle-chunk-v1_4\"")
                    && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                    && containsStringField(line, "generatedLazyResolverVersion",
                            "flow-ipc-generated-lazy-resolver-v1_8")
                    && containsStringField(line, "generatedLazyExecutionVersion", "flow-ipc-generated-lazy-v1_8")
                    && containsStringField(line, "generatedLazyImportResolutionStrategy",
                            "stable-relative-import-rewrite-generated-lazy-v1_8")) {
                summary.frontendGeneratedLazyV18RequestObserved = true;
            }
            if (line.contains("\"type\":\"resourceRequest\"")
                    && line.contains("\"purpose\":\"vaadin-web-component-resource-probe\"")
                    && line.contains("\"lookupKind\":\"Generated-flow-imports-dev-bundle-chunk-v1_4\"")
                    && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                    && containsStringField(line, "generatedLazyLoadOnDemandContractVersion",
                            "flow-ipc-loadondemand-v1_9")
                    && containsStringField(line, "generatedLazyTriggerSource",
                            "diagnostic-preflight-and-direct-staged-import")
                    && containsStringField(line, "generatedLazyApiOwner", "window.Vaadin.Flow.loadOnDemand")
                    && containsStringField(line, "generatedLazyRequestedChunk",
                            "vatori-flow-ipc-generated-lazy-v1_9-probe")
                    && containsStringField(line, "generatedLazyMappingSource",
                            "component-frontend-manifest-generated-lazy-v1_9")
                    && containsLiteralField(line, "generatedLazyFlowClientTriggered", "false")
                    && containsLiteralField(line, "generatedLazyDiagnosticBridge", "true")) {
                summary.frontendGeneratedLazyV19RequestObserved = true;
            }
            if (line.contains("\"type\":\"resourceRequest\"")
                    && line.contains("\"purpose\":\"flow-lazy-dependency-loader\"")
                    && line.contains("\"lookupKind\":\"Flow-lazy-dependency-resource-v1_10\"")
                    && containsStringField(line, "runtimeResolverContractVersion",
                            "flow-ipc-frontend-runtime-resolver-v1_10")
                    && containsStringField(line, "lazyDependencyVersion",
                            "flow-ipc-lazy-dependency-v1_10")
                    && containsStringField(line, "lazyDependencySource",
                            "flow-uidl-dependencyloader-loadmode-lazy-v1_10")
                    && containsStringField(line, "lazyDependencyMode", "LoadMode.LAZY")
                    && containsStringField(line, "lazyDependencyExpression",
                            "context://vatori-flow-ipc-lazy-v1_10.js")
                    && containsStringField(line, "lazyDependencyResolvedPath",
                            "vatori-flow-ipc-lazy-v1_10.js")
                    && containsLiteralField(line, "lazyDependencyTriggeredByFlowClient", "true")
                    && containsPath(line, "vatori-flow-ipc-lazy-v1_10.js")) {
                summary.flowLazyDependencyV110RequestObserved = true;
            }
            if (line.contains("\"type\":\"resourceRequest\"")
                    && line.contains("\"purpose\":\"vaadin-web-component-resource-probe\"")
                    && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && containsLiteralField(line, "productionBundleRuntimeArtifact", "true")
                    && containsStringField(line, "productionBundleRuntimeVersion",
                            "flow-ipc-production-runtime-v1_10")
                    && containsStringField(line, "productionBundleRuntimeRegistrySource",
                            "vaadin-dev-bundle-stats-runtime-registry-v1_10")
                    && containsStringField(line, "productionBundleRuntimeSelectedArtifact",
                            "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")) {
                summary.productionRuntimeV110RequestObserved = true;
            }
            if (line.contains("\"type\":\"resourceRequest\"")
                    && line.contains("\"purpose\":\"vaadin-web-component-resource-probe\"")
                    && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && containsLiteralField(line, "productionBundleRuntimeArtifact", "true")
                    && containsStringField(line, "productionBundleRuntimeOwnerVersion",
                            "flow-ipc-production-owner-v1_13")
                    && containsStringField(line, "productionBundleRuntimeOwner",
                            "flow-ipc-production-bundle-runtime")
                    && containsStringField(line, "productionBundleRuntimeEntryArtifact",
                            "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && containsStringField(line, "productionBundleRuntimeOwnerModuleIdentity",
                            "production-owner-v1_13:vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && containsStringField(line, "productionBundleRuntimeOwnerExecutionPolicy",
                            "stage-owner-contract-production-bundle-runtime-v1_13")
                    && containsStringField(line, "productionBundleRuntimeOwnerBlockerVersion",
                            "flow-ipc-production-owner-v1_13")
                    && containsProductionOwnerFailureLayer(line, "productionBundleRuntimeOwnerFailureLayer")
                    && containsLiteralField(line, "componentProductionOwnerReused", "false")) {
                summary.productionRuntimeOwnerV113RequestObserved = true;
            }
            if (line.contains("\"type\":\"resourceRequest\"")
                    && line.contains("\"purpose\":\"vaadin-web-component-resource-probe\"")
                    && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && containsLiteralField(line, "productionBundleRuntimeArtifact", "true")
                    && containsStringField(line, "productionBundleRuntimeOwnerImportVersion",
                            "flow-ipc-production-owner-import-v1_14")
                    && containsStringField(line, "productionBundleRuntimeOwnerImportPolicy",
                            "controlled-owner-dynamic-import-after-component-graph-v1_14")
                    && containsStringField(line, "productionBundleRuntimeOwnerImportArtifact",
                            "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && line.contains("productionBundleRuntimeOwnerImportExpectedSideEffects")
                    && line.contains("productionBundleRuntimeOwnerImportAcceptableFailures")
                    && containsLiteralField(line, "productionBundleRuntimeOwnerImportAttempted", "false")
                    && containsLiteralField(line, "productionBundleRuntimeOwnerImported", "false")) {
                summary.productionRuntimeOwnerImportV114RequestObserved = true;
            }
            if (line.contains("\"type\":\"resourceRequest\"")
                    && (line.contains("\"purpose\":\"vaadin-web-component-resource-probe\"")
                            || line.contains("\"purpose\":\"production-owner-dependency-v1_15\""))
                    && containsLiteralField(line, "productionOwnerDependencyResource", "true")
                    && containsStringField(line, "lookupKind", "Production-owner-dependency-resource-v1_15")
                    && containsStringField(line, "requestedBy", "production-owner-dependency-resolver-v1_15")
                    && containsStringField(line, "executionPolicy", "owner-dependency-module-v1_15")
                    && (containsPath(line,
                            "vaadin-dev-bundle/webapp/VAADIN/build/copilot-log-plugin-h3VQq9-2-BlujgixT.js")
                            || containsPath(line,
                                    "vaadin-dev-bundle/webapp/VAADIN/build/FlowClient-RTzSA_ve.js"))
                    && containsStringField(line, "productionBundleRuntimeOwnerDependencyResolverVersion",
                            "flow-ipc-production-owner-deps-v1_15")
                    && containsStringField(line, "productionBundleRuntimeOwnerDependencyResolverRoot",
                            "vaadin-dev-bundle/webapp/VAADIN/build/")
                    && containsStringField(line, "productionBundleRuntimeOwnerDependencyResolverPolicy",
                            "owner-scoped-recursive-relative-import-graph-v1_15")) {
                summary.productionRuntimeOwnerDependenciesV115RequestObserved = true;
            }
            if (line.contains("\"type\":\"resourceRequest\"")
                    && line.contains("\"purpose\":\"vaadin-web-component-resource-probe\"")
                    && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && containsLiteralField(line, "productionBundleRuntimeArtifact", "true")
                    && containsStringField(line, "productionBundleRuntimeOwnerImportOrderingVersion",
                            "flow-ipc-production-owner-ordering-v1_16")
                    && containsStringField(line, "productionBundleRuntimeOwnerImportPhasePolicy",
                            "owner-first-before-component-imports-with-after-component-fallback-v1_16")
                    && containsStringField(line, "componentProductionOwnerReuseVersion",
                            "flow-ipc-component-owner-reuse-v1_16")
                    && containsStringField(line, "componentProductionOwnerReusePolicy",
                            "reuse-owner-owned-custom-elements-before-focused-imports-v1_16")
                    && line.contains("componentProductionOwnerReuseAcceptableFailures")
                    && containsLiteralField(line, "componentProductionOwnerReused", "false")) {
                summary.productionRuntimeOwnerOrderingV116RequestObserved = true;
            }
            if (line.contains("\"type\":\"resourceRequest\"")
                    && line.contains("\"purpose\":\"generated-loadondemand-chunk-v1_11\"")
                    && line.contains("\"lookupKind\":\"Generated-flow-imports-dev-bundle-chunk-v1_4\"")
                    && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                    && containsStringField(line, "loadOnDemandChunkContractVersion",
                            "flow-ipc-loadondemand-chunk-v1_11")
                    && containsStringField(line, "generatedLazyChunkExpressionSource",
                            "bootstrap-uidl-dependencies")
                    && containsStringField(line, "generatedLazyChunkTriggerOwner",
                            "visible-flow-client-dependencyloader")
                    && containsStringFieldPrefix(line, "loadOnDemandChunkRequestedChunk", "")
                    && containsLiteralField(line, "generatedLazyChunkTriggeredByFlowClient", "true")
                    && containsLiteralField(line, "loadOnDemandChunkTriggeredByFlowClient", "true")
                    && containsLiteralField(line, "generatedLazyChunkDiagnosticBridgeUsed", "false")
                    && containsLiteralField(line, "generatedLazyChunkDirectImportProbeUsed", "false")
                    && line.contains("return window.Vaadin.Flow.loadOnDemand('")) {
                summary.generatedLoadOnDemandChunkV111RequestObserved = true;
            }
            if (line.contains("\"type\":\"resourceRequest\"")
                    && line.contains("\"purpose\":\"generated-loadondemand-chunk-v1_11\"")
                    && containsStringField(line, "generatedLoadOnDemandChunkFileRegistryVersion",
                            "flow-ipc-loadondemand-chunk-file-v1_12")
                    && containsStringField(line, "generatedLoadOnDemandChunkFileRegistrySource",
                            "generated-flow-imports-body-parser-v1_12")
                    && containsStringField(line, "chunkFileStatus", "not-generated")
                    && containsStringField(line, "loadOnDemandTableShape", "no-op-promise-resolve-0")
                    && containsStringField(line, "loadOnDemandChunkFileFailureLayer", "chunk-file-not-generated")
                    && containsStringField(line, "loadOnDemandChunkFileBlockerVersion",
                            "flow-ipc-loadondemand-chunk-file-v1_12")
                    && containsLiteralField(line, "loadOnDemandChunkFileTriggeredByFlowClient", "true")
                    && containsLiteralField(line, "loadOnDemandChunkTriggeredByFlowClient", "true")
                    && containsStringFieldPrefix(line, "loadOnDemandChunkRequestedChunk", "")) {
                summary.generatedLoadOnDemandChunkFileV112RequestObserved = true;
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && line.contains("\"artifactFamily\":\"generated-lazy\"")
                    && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/markdown-helpers-31I0A-hg.js")
                    && containsStringField(line, "generatedLazyResolverVersion",
                            "flow-ipc-generated-lazy-resolver-v1_8")
                    && containsStringField(line, "generatedLazyImportResolutionStrategy",
                            "stable-relative-import-rewrite-generated-lazy-v1_8")
                    && containsLiteralField(line, "generatedLazyRelativeDependencyResolved", "true")
                    && containsStringField(line, "generatedLazyRelativeImportSpecifier",
                            "./markdown-helpers-31I0A-hg.js")
                    && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/markdown-helpers-31I0A-hg.js")) {
                summary.frontendGeneratedLazyV18DependencyRewriteObserved = true;
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && line.contains("\"artifactFamily\":\"generated-lazy\"")
                    && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                    && containsStringField(line, "generatedLazyExecutionVersion", "flow-ipc-generated-lazy-v1_8")
                    && containsStringField(line, "generatedLazyImportResolutionStrategy",
                            "stable-relative-import-rewrite-generated-lazy-v1_8")
                    && (line.contains("\"executionStatus\":\"executed\"")
                            || (line.contains("\"executionStatus\":\"strict-blocker\"")
                                    && ((containsStringField(line, "generatedLazyBlockerVersion",
                                            "flow-ipc-generated-lazy-v1_8")
                                            && containsStringField(line, "failureLayer",
                                                    "relative-dependency-resolved-flow-loadondemand-boundary"))
                                            || (containsStringField(line, "generatedLazyBlockerVersion",
                                                    "flow-ipc-loadondemand-v1_9")
                                                    && containsStringField(line, "failureLayer",
                                                            "loadondemand-api-installed-by-generated-imports-not-flow-client-triggered")))))) {
                summary.frontendGeneratedLazyV18ExecutionOrNarrowedBlockerObserved = true;
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && line.contains("\"artifactFamily\":\"generated-lazy\"")
                    && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                    && containsStringField(line, "generatedLazyLoadOnDemandContractVersion",
                            "flow-ipc-loadondemand-v1_9")
                    && containsStringField(line, "generatedLazyTriggerSource",
                            "diagnostic-preflight-and-direct-staged-import")
                    && containsStringField(line, "generatedLazyApiOwner", "window.Vaadin.Flow.loadOnDemand")
                    && containsStringField(line, "generatedLazyRequestedChunk",
                            "vatori-flow-ipc-generated-lazy-v1_9-probe")
                    && containsStringField(line, "generatedLazyMappingSource",
                            "component-frontend-manifest-generated-lazy-v1_9")
                    && containsStringField(line, "generatedLazyBlockerVersion",
                            "flow-ipc-loadondemand-v1_9")
                    && containsStringField(line, "failureLayer",
                            "loadondemand-api-installed-by-generated-imports-not-flow-client-triggered")
                    && containsLiteralField(line, "generatedLazyFlowClientTriggered", "false")
                    && containsLiteralField(line, "generatedLazyDiagnosticBridge", "true")
                    && (line.contains("\"executionStatus\":\"execution-attempt\"")
                            || line.contains("\"executionStatus\":\"executed\"")
                            || line.contains("\"executionStatus\":\"strict-blocker\""))) {
                summary.frontendGeneratedLazyV19StageObserved = true;
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && line.contains("\"artifactFamily\":\"flow-lazy-dependency\"")
                    && containsPath(line, "vatori-flow-ipc-lazy-v1_10.js")
                    && containsStringField(line, "runtimeResolverContractVersion",
                            "flow-ipc-frontend-runtime-resolver-v1_10")
                    && containsStringField(line, "lazyDependencyVersion",
                            "flow-ipc-lazy-dependency-v1_10")
                    && containsStringField(line, "lazyDependencySource",
                            "flow-uidl-dependencyloader-loadmode-lazy-v1_10")
                    && containsStringField(line, "lazyDependencyMode", "LoadMode.LAZY")
                    && containsLiteralField(line, "lazyDependencyTriggeredByFlowClient", "true")
                    && line.contains("\"executionStatus\":\"executed\"")) {
                summary.flowLazyDependencyV110StageObserved = true;
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && line.contains("\"artifactFamily\":\"production-bundle\"")
                    && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && containsLiteralField(line, "productionBundleRuntimeArtifact", "true")
                    && containsStringField(line, "runtimeResolverContractVersion",
                            "flow-ipc-frontend-runtime-resolver-v1_10")
                    && containsStringField(line, "productionBundleRuntimeVersion",
                            "flow-ipc-production-runtime-v1_10")
                    && containsStringField(line, "productionBundleRuntimeRegistrySource",
                            "vaadin-dev-bundle-stats-runtime-registry-v1_10")
                    && containsStringField(line, "productionBundleRuntimeSelectedArtifact",
                            "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && containsStringField(line, "productionBundleRuntimeBlockerVersion",
                            "flow-ipc-production-runtime-v1_10")
                    && (line.contains("\"executionStatus\":\"execution-attempt\"")
                            || line.contains("\"executionStatus\":\"executed\"")
                            || line.contains("\"executionStatus\":\"execution-failed\""))) {
                summary.productionRuntimeV110StageObserved = true;
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && containsStringField(line, "owner", "production-bundle-runtime")
                    && line.contains("\"artifactFamily\":\"production-bundle\"")
                    && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && containsLiteralField(line, "productionBundleRuntimeArtifact", "true")
                    && containsStringField(line, "productionBundleRuntimeOwnerVersion",
                            "flow-ipc-production-owner-v1_13")
                    && containsStringField(line, "productionBundleRuntimeOwner",
                            "flow-ipc-production-bundle-runtime")
                    && containsStringField(line, "productionBundleRuntimeEntryArtifact",
                            "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && containsStringField(line, "productionBundleRuntimeOwnerModuleIdentity",
                            "production-owner-v1_13:vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && containsStringField(line, "productionBundleRuntimeOwnerExecutionPolicy",
                            "stage-owner-contract-production-bundle-runtime-v1_13")
                    && containsStringField(line, "productionBundleRuntimeOwnerBlockerVersion",
                            "flow-ipc-production-owner-v1_13")
                    && containsProductionOwnerFailureLayer(line, "productionBundleRuntimeOwnerFailureLayer")
                    && containsStringFieldPrefix(line, "productionBundleRuntimeOwnerExecutionResult",
                            "strict-blocker:")
                    && containsStringField(line, "moduleIdentityKey",
                            "production-owner-v1_13:vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && containsLiteralField(line, "componentProductionOwnerReused", "false")
                    && (line.contains("\"executionStatus\":\"execution-attempt\"")
                            || line.contains("\"executionStatus\":\"strict-blocker\"")
                            || line.contains("\"executionStatus\":\"execution-failed\""))) {
                summary.productionRuntimeOwnerV113StageObserved = true;
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && containsStringField(line, "owner", "production-bundle-runtime")
                    && line.contains("\"artifactFamily\":\"production-bundle\"")
                    && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && containsLiteralField(line, "productionBundleRuntimeArtifact", "true")
                    && containsStringField(line, "productionBundleRuntimeOwnerImportVersion",
                            "flow-ipc-production-owner-import-v1_14")
                    && containsStringField(line, "productionBundleRuntimeOwnerImportPolicy",
                            "controlled-owner-dynamic-import-after-component-graph-v1_14")
                    && containsStringField(line, "productionBundleRuntimeOwnerImportArtifact",
                            "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && containsLiteralField(line, "productionBundleRuntimeOwnerImportAttempted", "true")
                    && line.contains("\"productionBundleRuntimeOwnerImportDependencyRewriteResult\"")
                    && line.contains("\"productionBundleRuntimeOwnerImportVaadinGlobalBefore\"")
                    && line.contains("\"productionBundleRuntimeOwnerImportVaadinGlobalAfter\"")
                    && containsStringField(line, "productionBundleRuntimeOwnerImportModuleIdentity",
                            "production-owner-import-v1_14:vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && containsProductionOwnerImportFinalResult(line,
                            "productionBundleRuntimeOwnerExecutionResult",
                            "productionBundleRuntimeOwnerFailureLayer")
                    && (line.contains("\"executionStatus\":\"executed\"")
                            || line.contains("\"executionStatus\":\"strict-blocker\"")
                            || line.contains("\"executionStatus\":\"execution-failed\""))) {
                summary.productionRuntimeOwnerImportV114StageObserved = true;
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && containsStringField(line, "owner", "production-bundle-runtime")
                    && line.contains("\"artifactFamily\":\"production-bundle\"")
                    && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && containsLiteralField(line, "productionBundleRuntimeArtifact", "true")
                    && containsStringField(line, "productionBundleRuntimeOwnerDependencyResolverVersion",
                            "flow-ipc-production-owner-deps-v1_15")
                    && containsStringField(line, "productionBundleRuntimeOwnerDependencyResolverRoot",
                            "vaadin-dev-bundle/webapp/VAADIN/build/")
                    && containsStringField(line, "productionBundleRuntimeOwnerDependencyResolverPolicy",
                            "owner-scoped-recursive-relative-import-graph-v1_15")
                    && containsStringField(line,
                            "productionBundleRuntimeOwnerDependencyResolverModuleIdentityNamespace",
                            "production-owner-deps-v1_15")
                    && containsStringField(line, "productionBundleRuntimeOwnerImportPhase",
                            "after-component-imports")
                    && line.contains("productionBundleRuntimeOwnerImportPhaseCandidates")
                    && line.contains("isolated-preflight")
                    && line.contains("before-component-imports")
                    && line.contains("productionBundleRuntimeOwnerImportDiagnosticModes")
                    && containsLiteralField(line, "productionBundleRuntimeOwnerDependencyGraphClosed", "true")
                    && containsPositiveIntegerField(line,
                            "productionBundleRuntimeOwnerDependencyGraphTotalSpecifierCount")
                    && containsPositiveIntegerField(line,
                            "productionBundleRuntimeOwnerDependencyGraphResolvedCount")
                    && containsPositiveIntegerField(line,
                            "productionBundleRuntimeOwnerDependencyGraphStagedCount")
                    && containsPositiveIntegerField(line, "productionBundleRuntimeOwnerImportStageCount")
                    && line.contains("productionBundleRuntimeOwnerDependencyClassifications")
                    && line.contains("resolved-existing-artifact")
                    && line.contains("productionBundleRuntimeOwnerImportResolvedDependencyPaths")
                    && (containsPath(line,
                            "vaadin-dev-bundle/webapp/VAADIN/build/copilot-log-plugin-h3VQq9-2-BlujgixT.js")
                            || containsPath(line,
                                    "vaadin-dev-bundle/webapp/VAADIN/build/FlowClient-RTzSA_ve.js"))
                    && line.contains("productionBundleRuntimeOwnerImportMissingDependencyPaths")
                    && line.contains("productionBundleRuntimeOwnerImportMissingSpecifiers")
                    && containsProductionOwnerDependencyFinalResult(line,
                            "productionBundleRuntimeOwnerExecutionResult",
                            "productionBundleRuntimeOwnerFailureLayer")
                    && !containsStringField(line, "productionBundleRuntimeOwnerFailureLayer",
                            "production-owner-relative-import-unresolved")
                    && (line.contains("\"executionStatus\":\"executed\"")
                            || line.contains("\"executionStatus\":\"strict-blocker\"")
                            || line.contains("\"executionStatus\":\"execution-failed\""))) {
                summary.productionRuntimeOwnerDependenciesV115StageObserved = true;
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && containsStringField(line, "owner", "production-bundle-runtime")
                    && line.contains("\"artifactFamily\":\"production-bundle\"")
                    && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && containsLiteralField(line, "productionBundleRuntimeArtifact", "true")
                    && containsStringField(line, "productionBundleRuntimeOwnerImportOrderingVersion",
                            "flow-ipc-production-owner-ordering-v1_16")
                    && containsStringField(line, "productionBundleRuntimeOwnerImportPhasePolicy",
                            "owner-first-before-component-imports-with-after-component-fallback-v1_16")
                    && containsOwnerFirstOrIsolatedPhase(line, "productionBundleRuntimeOwnerImportPhase")
                    && containsStringField(line, "productionBundleRuntimeOwnerImportOrderingObservedPhases",
                            "before-component-imports")
                    && line.contains("productionBundleRuntimeOwnerImportOrderingResult")
                    && line.contains("owner-first-import-attempted-before-component-focused-imports")
                    && containsStringField(line, "componentProductionOwnerReuseVersion",
                            "flow-ipc-component-owner-reuse-v1_16")
                    && containsStringField(line, "componentProductionOwnerReusePolicy",
                            "reuse-owner-owned-custom-elements-before-focused-imports-v1_16")
                    && line.contains("componentProductionOwnerReuseMode")
                    && line.contains("componentProductionOwnerReuseReason")
                    && containsProductionOwnerOrderingFinalResult(line,
                            "productionBundleRuntimeOwnerExecutionResult",
                            "productionBundleRuntimeOwnerFailureLayer")
                    && !containsStringField(line, "productionBundleRuntimeOwnerFailureLayer",
                            "production-owner-relative-import-unresolved")
                    && (line.contains("\"executionStatus\":\"executed\"")
                            || line.contains("\"executionStatus\":\"strict-blocker\"")
                            || line.contains("\"executionStatus\":\"execution-failed\""))) {
                summary.productionRuntimeOwnerOrderingV116StageObserved = true;
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && line.contains("\"artifactFamily\":\"generated-lazy\"")
                    && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                    && containsStringField(line, "loadOnDemandChunkContractVersion",
                            "flow-ipc-loadondemand-chunk-v1_11")
                    && containsStringField(line, "generatedLazyChunkExpressionSource",
                            "bootstrap-uidl-dependencies")
                    && containsStringFieldPrefix(line, "loadOnDemandChunkRequestedChunk", "")
                    && containsLiteralField(line, "generatedLazyChunkDiagnosticBridgeUsed", "false")
                    && containsLiteralField(line, "generatedLazyChunkDirectImportProbeUsed", "false")
                    && containsStringField(line, "loadOnDemandChunkBlockerVersion",
                            "flow-ipc-loadondemand-chunk-v1_11")
                    && containsStringField(line, "loadOnDemandChunkFailureLayer",
                            "dev-bundle-generated-imports-loadondemand-no-op-no-chunk-file")
                    && (line.contains("\"executionStatus\":\"executed\"")
                            || line.contains("\"executionStatus\":\"strict-blocker\""))) {
                summary.generatedLoadOnDemandChunkV111StageObserved = true;
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && line.contains("\"artifactFamily\":\"generated-lazy\"")
                    && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                    && containsStringField(line, "generatedLoadOnDemandChunkFileRegistryVersion",
                            "flow-ipc-loadondemand-chunk-file-v1_12")
                    && containsStringField(line, "generatedLoadOnDemandChunkFileRegistrySource",
                            "generated-flow-imports-body-parser-v1_12")
                    && containsStringField(line, "chunkFileStatus", "not-generated")
                    && containsStringField(line, "loadOnDemandTableShape", "no-op-promise-resolve-0")
                    && containsLiteralField(line, "loadOnDemandChunkFileStageObserved", "false")
                    && containsLiteralField(line, "loadOnDemandChunkFileExecuted", "false")
                    && containsGeneratedChunkFileNoOpResolution(line)
                    && containsStringField(line, "loadOnDemandChunkFileFailureLayer", "chunk-file-not-generated")
                    && containsStringField(line, "loadOnDemandChunkFileBlockerVersion",
                            "flow-ipc-loadondemand-chunk-file-v1_12")
                    && containsStringField(line, "loadOnDemandChunkFileSideEffectProof",
                            "no-chunk-generated-by-real-generated-flow-imports-no-op-table")
                    && containsGeneratedChunkFileStrictBlockerResult(line)) {
                summary.generatedLoadOnDemandChunkFileV112StageObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsStringField(line, "frontendStagingResolverVersion", "flow-ipc-resource-staging-v1_5")
                    && containsLiteralField(line, "frontendStagingObserved", "true")
                    && containsPositiveIntegerField(line, "frontendStagedResourceCount")
                    && line.contains("frontendStagedResourcePaths")
                    && line.contains("frontendStagedResourceFamilies")
                    && line.contains("frontendStagedResourceSchemes")) {
                summary.frontendStagingDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && (containsLiteralField(line, "frontendArtifactTextFieldStagedBlocker", "true")
                            || line.contains("frontendArtifactTextFieldStagedBlockerPath"))
                    && line.contains("TextField")
                    && line.contains("frontendArtifactTextFieldStagedBlockerError")) {
                summary.frontendTextFieldStagedBlockerObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsStringField(line, "frontendArtifactTextFieldGraphParityVersion",
                            "flow-ipc-textfield-graph-v1_6")
                    && containsStringField(line, "frontendArtifactTextFieldBlockerVersion",
                            "flow-ipc-textfield-v1_6")
                    && containsStringField(line, "frontendArtifactTextFieldNarrowedFailureClassification",
                            "textfield-production-indexhtml-duplicate-custom-element-definition")
                    && containsStringField(line, "frontendArtifactTextFieldFailureLayer",
                            "custom-element-registry-conflict-after-production-indexhtml-reimport")
                    && line.contains("frontendArtifactTextFieldCustomElementRegistryBefore")
                    && line.contains("frontendArtifactTextFieldCustomElementRegistryAfter")
                    && line.contains("frontendArtifactTextFieldNextResolutionStep")
                    && line.contains("frontendArtifactTextFieldStagedBlockerPath")
                    && line.contains("META-INF/vatori/dev-bundle-entry/vaadin-text-field.js")) {
                summary.frontendTextFieldV16DomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsStringField(line, "frontendArtifactTextFieldGraphParityVersion",
                            "flow-ipc-textfield-graph-v1_6")
                    && containsStringField(line, "frontendArtifactTextFieldBlockerVersion",
                            "flow-ipc-textfield-v1_6")
                    && containsStringField(line, "frontendArtifactTextFieldExecutionVersion",
                            "flow-ipc-textfield-v1_7")
                    && containsStringField(line, "frontendArtifactTextFieldSideEffectVersion",
                            "flow-ipc-textfield-side-effect-v1_7")
                    && containsLiteralField(line, "frontendArtifactTextFieldExecuted", "true")
                    && containsLiteralField(line, "frontendArtifactTextFieldShimUsed", "false")
                    && containsStringField(line, "frontendArtifactTextFieldClassExportArtifact",
                            "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && containsStringField(line, "frontendArtifactTextFieldDuplicateTag", "vaadin-text-field")
                    && containsStringField(line, "frontendArtifactTextFieldRemainingBlockerClassification",
                            "none")
                    && containsStringField(line, "frontendArtifactTextFieldModuleIdentityStrategy",
                            "stable-resource-path-blob-module-identity-v1_7")
                    && line.contains("frontendArtifactTextFieldModuleIdentityPaths")
                    && line.contains("META-INF/vatori/dev-bundle-entry/vaadin-text-field.js")
                    && containsLiteralField(line, "frontendArtifactTextFieldShimUsed", "false")) {
                summary.frontendTextFieldV17DomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "frontendArtifactGeneratedLazyExecutionAttempted", "true")
                    && line.contains("frontendArtifactGeneratedLazyExecutionResult")
                    && line.contains("frontendArtifactGeneratedLazyLoadOnDemandResult")) {
                summary.frontendGeneratedLazyExecutionAttemptObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "frontendArtifactGeneratedLazyExecutionAttempted", "true")
                    && (containsLiteralField(line, "frontendArtifactGeneratedLazyExecuted", "true")
                            || line.contains("strict-blocker:vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js"))) {
                summary.frontendGeneratedLazyExecutionResolvedObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && line.contains("frontendArtifactGeneratedLazyLoadOnDemandResult")
                    && (line.contains("staged-import-attempted")
                            || line.contains("api-installed-by-staged-generated-flow-imports")
                            || line.contains("api-present-before-generated-flow-imports"))) {
                summary.frontendLoadOnDemandBoundaryObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsStringField(line, "frontendArtifactGeneratedLazyResolverVersion",
                            "flow-ipc-generated-lazy-resolver-v1_8")
                    && containsStringField(line, "frontendArtifactGeneratedLazyExecutionVersion",
                            "flow-ipc-generated-lazy-v1_8")
                    && containsStringField(line, "frontendArtifactGeneratedLazyImportResolutionStrategy",
                            "stable-relative-import-rewrite-generated-lazy-v1_8")
                    && containsLiteralField(line, "frontendArtifactGeneratedLazyRelativeDependencyResolved", "true")
                    && containsStringField(line, "frontendArtifactGeneratedLazyRelativeImportSpecifier",
                            "./markdown-helpers-31I0A-hg.js")
                    && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/markdown-helpers-31I0A-hg.js")
                    && (containsLiteralField(line, "frontendArtifactGeneratedLazyExecuted", "true")
                            || containsStringField(line, "frontendArtifactGeneratedLazyBlockerVersion",
                                    "flow-ipc-generated-lazy-v1_8")
                            || containsStringField(line, "frontendArtifactGeneratedLazyBlockerVersion",
                                    "flow-ipc-loadondemand-v1_9"))) {
                summary.frontendGeneratedLazyV18DomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsStringField(line, "frontendArtifactGeneratedLazyLoadOnDemandContractVersion",
                            "flow-ipc-loadondemand-v1_9")
                    && containsStringField(line, "frontendArtifactGeneratedLazyTriggerSource",
                            "diagnostic-preflight-and-direct-staged-import")
                    && containsStringField(line, "frontendArtifactGeneratedLazyApiOwner",
                            "window.Vaadin.Flow.loadOnDemand")
                    && containsStringField(line, "frontendArtifactGeneratedLazyApiInstallSource",
                            "generated-flow-imports-dev-bundle-after-direct-staged-import")
                    && containsStringField(line, "frontendArtifactGeneratedLazyRequestedChunk",
                            "vatori-flow-ipc-generated-lazy-v1_9-probe")
                    && containsStringField(line, "frontendArtifactGeneratedLazyRegistrySource",
                            "no-bootstrap-lazy-registry-observed")
                    && containsStringField(line, "frontendArtifactGeneratedLazyMappingSource",
                            "component-frontend-manifest-generated-lazy-v1_9")
                    && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                    && containsStringField(line, "frontendArtifactGeneratedLazyBlockerVersion",
                            "flow-ipc-loadondemand-v1_9")
                    && containsStringField(line, "frontendArtifactGeneratedLazyFailureLayer",
                            "loadondemand-api-installed-by-generated-imports-not-flow-client-triggered")
                    && containsLiteralField(line, "frontendArtifactGeneratedLazyFlowClientTriggered", "false")
                    && containsLiteralField(line, "frontendArtifactGeneratedLazyDiagnosticBridge", "true")
                    && containsLiteralField(line, "frontendArtifactGeneratedLazyLoadOnDemandBlocker", "true")
                    && line.contains("frontendArtifactGeneratedLazyLoadOnDemandResult")
                    && (line.contains("diagnostic-loadondemand-call")
                            || line.contains("diagnostic-direct-import-required"))) {
                summary.frontendGeneratedLazyV19DomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsStringField(line, "frontendRuntimeResolverContractVersion",
                            "flow-ipc-frontend-runtime-resolver-v1_10")
                    && line.contains("frontendRuntimeResolverLogicalRequestIds")
                    && line.contains("flow-lazy-dependency:v1_10")
                    && line.contains("production-runtime:indexhtml:v1_10")) {
                summary.frontendRuntimeResolverV110ManifestObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "flowLazyDependencyObserved", "true")
                    && containsLiteralField(line, "flowLazyDependencyRequested", "true")
                    && containsLiteralField(line, "flowLazyDependencyResponse", "true")
                    && containsLiteralField(line, "flowLazyDependencyStageObserved", "true")
                    && containsLiteralField(line, "flowLazyDependencyExecuted", "true")
                    && containsStringField(line, "flowLazyDependencyVersion",
                            "flow-ipc-lazy-dependency-v1_10")
                    && containsStringField(line, "flowLazyDependencySource",
                            "flow-uidl-dependencyloader-loadmode-lazy-v1_10")
                    && containsStringField(line, "flowLazyDependencyMode", "LoadMode.LAZY")
                    && containsStringField(line, "flowLazyDependencyExpression",
                            "context://vatori-flow-ipc-lazy-v1_10.js")
                    && containsStringField(line, "flowLazyDependencyResolvedPath",
                            "vatori-flow-ipc-lazy-v1_10.js")
                    && containsLiteralField(line, "flowLazyDependencyTriggeredByFlowClient", "true")
                    && line.contains("executed:vatori-flow-ipc-lazy-v1_10.js")) {
                summary.flowLazyDependencyV110DomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "frontendArtifactProductionBundleRuntimeObserved", "true")
                    && containsLiteralField(line, "frontendArtifactProductionBundleRuntimeStageAttempted", "true")
                    && containsLiteralField(line, "frontendArtifactProductionBundleRuntimeBlocker", "true")
                    && containsStringField(line, "frontendArtifactProductionBundleRuntimeVersion",
                            "flow-ipc-production-runtime-v1_10")
                    && containsStringField(line, "frontendArtifactProductionBundleRuntimeRegistrySource",
                            "vaadin-dev-bundle-stats-runtime-registry-v1_10")
                    && containsStringField(line, "frontendArtifactProductionBundleRuntimeSelectedArtifact",
                            "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && containsStringField(line, "frontendArtifactProductionBundleRuntimeFailureLayer",
                            "production-runtime-ownership-not-yet-separated-from-focused-component-entry-imports")
                    && (line.contains("execution-attempt:vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                            || containsStringFieldPrefix(line,
                                    "frontendArtifactProductionBundleRuntimeExecutionResult",
                                    "strict-blocker:production-owner-")
                            || containsStringFieldPrefix(line,
                                    "frontendArtifactProductionBundleRuntimeExecutionResult",
                                    "imported:vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js"))) {
                summary.productionRuntimeV110DomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "frontendArtifactProductionBundleRuntimeOwnerObserved", "true")
                    && containsLiteralField(line, "frontendArtifactProductionBundleRuntimeOwnerStageObserved", "true")
                    && containsLiteralField(line, "frontendArtifactProductionBundleRuntimeOwnerImported", "false")
                    && containsLiteralField(line, "frontendArtifactProductionBundleRuntimeOwnerBlocker", "true")
                    && containsStringField(line, "frontendArtifactProductionBundleRuntimeOwnerVersion",
                            "flow-ipc-production-owner-v1_13")
                    && containsStringField(line, "frontendArtifactProductionBundleRuntimeOwner",
                            "flow-ipc-production-bundle-runtime")
                    && containsStringField(line, "frontendArtifactProductionBundleRuntimeOwnerEntryArtifact",
                            "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && containsStringField(line, "frontendArtifactProductionBundleRuntimeOwnerModuleIdentity",
                            "production-owner-v1_13:vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && containsStringField(line, "frontendArtifactProductionBundleRuntimeOwnerExecutionPolicy",
                            "stage-owner-contract-production-bundle-runtime-v1_13")
                    && containsStringField(line, "frontendArtifactProductionBundleRuntimeOwnerBlockerVersion",
                            "flow-ipc-production-owner-v1_13")
                    && containsProductionOwnerFailureLayer(line,
                            "frontendArtifactProductionBundleRuntimeOwnerFailureLayer")
                    && containsStringFieldPrefix(line,
                            "frontendArtifactProductionBundleRuntimeOwnerExecutionResult",
                            "strict-blocker:")
                    && containsStringFieldPrefix(line,
                            "frontendArtifactProductionBundleRuntimeOwnerSideEffectProof",
                            "owner-stage-resolved")
                    && containsLiteralField(line, "componentProductionOwnerReused", "false")) {
                summary.productionRuntimeOwnerV113DomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "frontendArtifactProductionBundleRuntimeOwnerObserved", "true")
                    && containsLiteralField(line, "frontendArtifactProductionBundleRuntimeOwnerStageObserved", "true")
                    && containsStringField(line, "frontendArtifactProductionBundleRuntimeOwnerVersion",
                            "flow-ipc-production-owner-v1_13")
                    && containsStringField(line, "frontendArtifactProductionBundleRuntimeOwner",
                            "flow-ipc-production-bundle-runtime")
                    && containsStringField(line, "frontendArtifactProductionBundleRuntimeOwnerEntryArtifact",
                            "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && containsStringField(line, "frontendArtifactProductionBundleRuntimeOwnerModuleIdentity",
                            "production-owner-v1_13:vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && containsLiteralField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerImportAttempted", "true")
                    && containsStringField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerImportVersion",
                            "flow-ipc-production-owner-import-v1_14")
                    && containsLiteralField(line, "componentProductionOwnerReused", "false")) {
                summary.productionRuntimeOwnerV113DomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "frontendArtifactProductionBundleRuntimeOwnerObserved", "true")
                    && containsLiteralField(line, "frontendArtifactProductionBundleRuntimeOwnerStageObserved", "true")
                    && containsLiteralField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerImportAttempted", "true")
                    && containsStringField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerImportVersion",
                            "flow-ipc-production-owner-import-v1_14")
                    && containsStringField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerImportPolicy",
                            "controlled-owner-dynamic-import-after-component-graph-v1_14")
                    && containsStringField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerImportArtifact",
                            "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && line.contains("frontendArtifactProductionBundleRuntimeOwnerImportDependencyRewriteResult")
                    && line.contains("frontendArtifactProductionBundleRuntimeOwnerImportVaadinGlobalBefore")
                    && line.contains("frontendArtifactProductionBundleRuntimeOwnerImportVaadinGlobalAfter")
                    && containsStringField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerImportModuleIdentity",
                            "production-owner-import-v1_14:vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && containsProductionOwnerImportFinalResult(line,
                            "frontendArtifactProductionBundleRuntimeOwnerExecutionResult",
                            "frontendArtifactProductionBundleRuntimeOwnerFailureLayer")
                    && containsLiteralField(line, "componentProductionOwnerReused", "false")) {
                summary.productionRuntimeOwnerImportV114DomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "frontendArtifactProductionBundleRuntimeOwnerObserved", "true")
                    && containsLiteralField(line, "frontendArtifactProductionBundleRuntimeOwnerStageObserved", "true")
                    && containsLiteralField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerImportAttempted", "true")
                    && containsStringField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerDependencyResolverVersion",
                            "flow-ipc-production-owner-deps-v1_15")
                    && containsStringField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerDependencyResolverRoot",
                            "vaadin-dev-bundle/webapp/VAADIN/build/")
                    && containsStringField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerDependencyResolverPolicy",
                            "owner-scoped-recursive-relative-import-graph-v1_15")
                    && containsStringField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerDependencyResolverModuleIdentityNamespace",
                            "production-owner-deps-v1_15")
                    && containsStringField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerDependencyResolverRootArtifact",
                            "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && line.contains("frontendArtifactProductionBundleRuntimeOwnerImportPhase")
                    && line.contains("frontendArtifactProductionBundleRuntimeOwnerImportPhaseCandidates")
                    && line.contains("isolated-preflight")
                    && line.contains("before-component-imports")
                    && line.contains("frontendArtifactProductionBundleRuntimeOwnerImportDiagnosticModes")
                    && containsLiteralField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerDependencyGraphClosed", "true")
                    && containsPositiveIntegerField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerDependencyGraphTotalSpecifierCount")
                    && containsPositiveIntegerField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerDependencyGraphResolvedCount")
                    && containsPositiveIntegerField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerDependencyGraphStagedCount")
                    && containsPositiveIntegerField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerImportStageCount")
                    && line.contains("frontendArtifactProductionBundleRuntimeOwnerDependencyClassifications")
                    && line.contains("resolved-existing-artifact")
                    && line.contains("frontendArtifactProductionBundleRuntimeOwnerImportMissingDependencyPaths")
                    && line.contains("frontendArtifactProductionBundleRuntimeOwnerImportMissingSpecifiers")
                    && containsProductionOwnerDependencyFinalResult(line,
                            "frontendArtifactProductionBundleRuntimeOwnerExecutionResult",
                            "frontendArtifactProductionBundleRuntimeOwnerFailureLayer")
                    && !containsStringField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerFailureLayer",
                            "production-owner-relative-import-unresolved")
                    && containsLiteralField(line, "componentProductionOwnerReused", "false")) {
                summary.productionRuntimeOwnerDependenciesV115DomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "frontendArtifactProductionBundleRuntimeOwnerObserved", "true")
                    && containsLiteralField(line, "frontendArtifactProductionBundleRuntimeOwnerStageObserved", "true")
                    && containsLiteralField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerImportAttempted", "true")
                    && containsStringField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerImportOrderingVersion",
                            "flow-ipc-production-owner-ordering-v1_16")
                    && containsStringField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerImportPhasePolicy",
                            "owner-first-before-component-imports-with-after-component-fallback-v1_16")
                    && line.contains("frontendArtifactProductionBundleRuntimeOwnerImportOrderingObservedPhases")
                    && line.contains("before-component-imports")
                    && line.contains("frontendArtifactProductionBundleRuntimeOwnerImportOrderingResult")
                    && line.contains("owner-first-import-attempted-before-component-focused-imports")
                    && containsStringField(line, "componentProductionOwnerReuseVersion",
                            "flow-ipc-component-owner-reuse-v1_16")
                    && containsStringField(line, "componentProductionOwnerReusePolicy",
                            "reuse-owner-owned-custom-elements-before-focused-imports-v1_16")
                    && line.contains("componentProductionOwnerReuseMode")
                    && line.contains("componentProductionOwnerReuseReason")
                    && containsProductionOwnerOrderingFinalResult(line,
                            "frontendArtifactProductionBundleRuntimeOwnerExecutionResult",
                            "frontendArtifactProductionBundleRuntimeOwnerFailureLayer")
                    && !containsStringField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerFailureLayer",
                            "production-owner-relative-import-unresolved")) {
                summary.productionRuntimeOwnerOrderingV116DomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "generatedLazyChunkUidlExpressionObserved", "true")
                    && containsStringField(line, "generatedLazyChunkExpressionSource",
                            "bootstrap-uidl-dependencies")
                    && line.contains("return window.Vaadin.Flow.loadOnDemand('")
                    && containsStringFieldPrefix(line, "generatedLazyChunkId", "")
                    && containsStringField(line, "generatedLazyChunkTriggerOwner",
                            "visible-flow-client-dependencyloader")
                    && containsLiteralField(line, "generatedLazyChunkTriggeredByFlowClient", "true")
                    && containsLiteralField(line, "generatedLazyChunkDiagnosticBridgeUsed", "false")
                    && containsLiteralField(line, "generatedLazyChunkDirectImportProbeUsed", "false")
                    && containsStringField(line, "loadOnDemandChunkContractVersion",
                            "flow-ipc-loadondemand-chunk-v1_11")
                    && containsStringFieldPrefix(line, "loadOnDemandChunkRequestedChunk", "")
                    && containsLiteralField(line, "loadOnDemandChunkTriggeredByFlowClient", "true")
                    && (containsLiteralField(line, "loadOnDemandChunkStageObserved", "true")
                            || summary.generatedLoadOnDemandChunkV111StageObserved)
                    && containsStringField(line, "loadOnDemandChunkResolvedArtifactPath",
                            "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                    && containsStringField(line, "loadOnDemandChunkBlockerVersion",
                            "flow-ipc-loadondemand-chunk-v1_11")
                    && containsStringField(line, "loadOnDemandChunkFailureLayer",
                            "dev-bundle-generated-imports-loadondemand-no-op-no-chunk-file")
                    && (containsLiteralField(line, "loadOnDemandChunkExecuted", "true")
                            || containsStringFieldPrefix(line, "loadOnDemandChunkExecutionResult",
                                    "strict-blocker:")
                            || containsStringField(line, "loadOnDemandChunkFailureLayer",
                                    "dev-bundle-generated-imports-loadondemand-no-op-no-chunk-file"))) {
                summary.generatedLoadOnDemandChunkV111DomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsStringField(line, "generatedLoadOnDemandChunkFileRegistryVersion",
                            "flow-ipc-loadondemand-chunk-file-v1_12")
                    && containsStringField(line, "generatedLoadOnDemandChunkFileRegistrySource",
                            "generated-flow-imports-body-parser-v1_12")
                    && containsStringField(line, "generatedLoadOnDemandChunkFileMappingSource",
                            "real-generated-flow-imports-loadondemand-table-v1_12")
                    && containsStringField(line, "generatedLoadOnDemandGeneratedImportsArtifact",
                            "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                    && containsLiteralField(line, "generatedLoadOnDemandNoOpLoadOnDemand", "true")
                    && containsStringField(line, "loadOnDemandTableShape", "no-op-promise-resolve-0")
                    && containsStringField(line, "chunkFileResolver",
                            "generated-flow-imports-relative-chunk-file-resolver-v1_12")
                    && containsStringField(line, "chunkFileStatus", "not-generated")
                    && containsLiteralField(line, "loadOnDemandChunkFileTriggeredByFlowClient", "true")
                    && containsLiteralField(line, "loadOnDemandChunkFileStageObserved", "false")
                    && containsLiteralField(line, "loadOnDemandChunkFileExecuted", "false")
                    && containsGeneratedChunkFileNoOpResolution(line)
                    && containsStringField(line, "loadOnDemandChunkFileFailureLayer", "chunk-file-not-generated")
                    && containsStringField(line, "loadOnDemandChunkFileBlockerVersion",
                            "flow-ipc-loadondemand-chunk-file-v1_12")
                    && containsStringField(line, "loadOnDemandChunkFileSideEffectProof",
                            "no-chunk-generated-by-real-generated-flow-imports-no-op-table")
                    && containsGeneratedChunkFileStrictBlockerResult(line)
                    && line.contains("noChunkGeneratedReason")) {
                summary.generatedLoadOnDemandChunkFileV112DomObserved = true;
            }
            if (line.contains("\"type\":\"resourceRequest\"")
                    && line.contains("\"purpose\":\"vaadin-web-component-resource-probe\"")
                    && line.contains("\"lookupKind\":\"BigDecimalField-minimum-component-resource\"")
                    && line.contains("META-INF/resources/frontend/vaadin-big-decimal-field.js")
                    && line.contains("\"securityKey\"")) {
                summary.vaadinWebComponentResourceRequestObserved = true;
            }
            if (line.contains("\"type\":\"resourceRequest\"")
                    && line.contains("\"purpose\":\"vaadin-web-component-resource-probe\"")
                    && line.contains("\"lookupKind\":\"Checkbox-component-resource-v1_3\"")
                    && line.contains("META-INF/vatori/frontend-shims/vaadin-checkbox.js")
                    && line.contains("\"securityKey\"")) {
                summary.vaadinSecondWebComponentResourceRequestObserved = true;
            }
            if (line.contains("\"type\":\"resourceRequest\"")
                    && line.contains("\"purpose\":\"vaadin-web-component-resource-probe\"")
                    && line.contains("\"lookupKind\":\"Checkbox-dev-bundle-resource-v1_4\"")
                    && (line.contains("vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                            || line.contains("META-INF/vatori/dev-bundle-entry/vaadin-checkbox.js"))
                    && line.contains("\"securityKey\"")) {
                summary.vaadinSecondWebComponentResourceRequestObserved = true;
                summary.vaadinSecondWebComponentDevBundleResourceObserved = true;
            }
            if (line.contains("\"type\":\"resourceResponse\"")
                    && line.contains("\"componentFrontendResource\":true")
                    && line.contains("META-INF/resources/frontend/vaadin-big-decimal-field.js")
                    && line.contains("\"bodyRedacted\":true")
                    && line.contains("\"blockedReason\"")
                    && (line.contains("\"failureClassification\":\"npm-transitive-imports-shimmed\"")
                            || line.contains("\"failureClassification\":\"textfield-production-class-execution-blocked\"")
                            || line.contains("\"failureClassification\":\"real-textfield-production-graph-executed-v1_7\""))) {
                summary.vaadinWebComponentResourceResponseObserved = true;
                summary.vaadinWebComponentFailureClassificationObserved = true;
            }
            if (line.contains("\"type\":\"resourceResponse\"")
                    && line.contains("\"componentFrontendResource\":true")
                    && line.contains("META-INF/vatori/frontend-shims/vaadin-checkbox.js")
                    && line.contains("\"lookupKind\":\"Checkbox-component-resource-v1_3\"")
                    && line.contains("\"bodyRedacted\":true")
                    && line.contains("\"failureClassification\":\"npm-transitive-imports-shimmed\"")) {
                summary.vaadinSecondWebComponentResourceResponseObserved = true;
                summary.vaadinWebComponentFailureClassificationObserved = true;
            }
            if (line.contains("\"type\":\"resourceResponse\"")
                    && line.contains("\"componentFrontendResource\":true")
                    && (line.contains("vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                            || line.contains("META-INF/vatori/dev-bundle-entry/vaadin-checkbox.js"))
                    && line.contains("\"lookupKind\":\"Checkbox-dev-bundle-resource-v1_4\"")
                    && line.contains("\"bodyRedacted\":true")
                    && line.contains("\"devBundleResource\":true")) {
                summary.vaadinSecondWebComponentResourceResponseObserved = true;
                summary.vaadinSecondWebComponentDevBundleResourceObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && line.contains("flowClientResourceLoaded")
                    && line.contains("flowClientResourceVerified")
                    && line.contains("flowClientResourceModuleLoaded")
                    && line.contains("flowClientBootstrapExported")
                    && line.contains("flowClientBootstrapInitialized")
                    && line.contains("ROOT-2521314")
                    && line.contains("flowClientResourceExecuted")
                    && line.contains("META-INF/frontend/FlowBootstrap.js")) {
                summary.flowClientResourceDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "flowClientResourceManifestCount", "6")
                    && containsLiteralField(line, "flowClientResourceGraphRequestedCount", "6")
                    && containsLiteralField(line, "flowClientResourceGraphResponseCount", "6")
                    && containsLiteralField(line, "flowClientResourceGraphComplete", "true")
                    && containsLiteralField(line, "flowClientResourceGraphVerified", "true")
                    && line.contains("META-INF/frontend/FlowBootstrap.js")
                    && line.contains("META-INF/frontend/FlowClient.js")
                    && line.contains("META-INF/frontend/Flow.js")
                    && line.contains("META-INF/frontend/index.js")
                    && line.contains("META-INF/resources/VAADIN/static/client/compile.properties")
                    && line.contains(".cache.js")) {
                summary.flowClientResourceGraphDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "flowClientResourceExecutionClassificationComplete", "true")
                    && containsLiteralField(line, "flowClientResourceVerifiedCount", "6")
                    && containsLiteralField(line, "flowClientResourceExecutedCount", "2")
                    && containsLiteralField(line, "flowClientResourceVerifiedOnlyCount", "4")
                    && containsLiteralField(line, "flowClientResourceIntentionallyNotExecutedCount", "4")
                    && containsLiteralField(line, "flowClientResourceExecutionFailedCount", "0")
                    && line.contains("flowClientResourceExecutedPaths")
                    && line.contains("META-INF/frontend/FlowBootstrap.js")
                    && line.contains("META-INF/frontend/FlowClient.js")
                    && line.contains("flowClientResourceVerifiedOnlyPaths")
                    && line.contains("META-INF/frontend/Flow.js")
                    && line.contains("META-INF/frontend/index.js")
                    && line.contains("META-INF/resources/VAADIN/static/client/compile.properties")
                    && line.contains(".cache.js")) {
                summary.flowClientResourceExecutionClassificationDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "flowClientResourceLazyLookupRequested", "true")
                    && containsLiteralField(line, "flowClientResourceLazyLookupResponse", "true")
                    && containsLiteralField(line, "flowClientResourceLazyLookupVerified", "true")
                    && containsStringField(line, "flowClientResourceLazyLookupPurpose", "flow-client-lazy-lookup")
                    && containsStringField(line, "flowClientResourceLazyLookupKind", "Flow.loadOnDemand-minimum-probe")
                    && containsStringField(line, "flowClientResourceLazyLookupPath", "META-INF/frontend/Flow.js")
                    && containsStringField(line, "flowClientResourceLazyLookupResult",
                            "verified-only:intentionally-not-executed")
                    && line.contains("flowClientResourceLoadOnDemandProbeResult")
                    && containsStringField(line, "flowClientResourceBootstrapLazyEntriesResult",
                            "no-bootstrap-LAZY-entry-in-smoke")
                    && containsLiteralField(line, "flowClientResourceStaticLookupVerified", "true")
                    && containsStringField(line, "flowClientResourceStaticLookupPath",
                            "META-INF/resources/VAADIN/static/client/compile.properties")
                    && (containsStringField(line, "flowClientResourceThemeLookupResult",
                            "no-theme-resource-declared-by-smoke-app")
                            || line.contains("\"flowClientResourceThemeLookupResult\":\"verified:")
                            || line.contains("\\\"flowClientResourceThemeLookupResult\\\":\\\"verified:"))) {
                summary.flowClientResourceLazyLookupDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "vaadinWebComponentResourceManifestObserved", "true")
                    && containsPositiveIntegerField(line, "vaadinWebComponentResourceManifestCount")
                    && containsLiteralField(line, "vaadinWebComponentResourceRequested", "true")
                    && containsPositiveIntegerField(line, "vaadinWebComponentResourceRequestedCount")
                    && containsPositiveIntegerField(line, "vaadinWebComponentResourceResponseCount")
                    && containsLiteralField(line, "vaadinWebComponentResourceResolved", "true")
                    && containsLiteralField(line, "vaadinWebComponentResourceVerified", "true")
                    && containsLiteralField(line, "vaadinWebComponentResourceGraphComplete", "true")
                    && containsLiteralField(line, "vaadinWebComponentResourceGraphVerified", "true")
                    && line.contains("META-INF/resources/frontend/vaadin-big-decimal-field.js")
                    && (line.contains("META-INF/vatori/frontend-shims/vaadin-text-field.js")
                            || line.contains("META-INF/vatori/dev-bundle-entry/vaadin-text-field.js"))
                    && (line.contains("npm-transitive-imports-shimmed")
                            || line.contains("textfield-production-class-execution-blocked")
                            || line.contains("real-textfield-production-graph-executed-v1_7"))
                    && (line.contains("full @vaadin/text-field npm graph")
                            || line.contains("lit npm helper")
                            || line.contains("@vaadin/text-field import is still a controlled shim")
                            || line.contains("@vaadin/text-field import resolves to the real focused TextField dev-bundle entry"))) {
                summary.vaadinWebComponentResourceDomObserved = true;
                summary.vaadinWebComponentFailureClassificationObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "vaadinWebComponentResourceExecuted", "true")
                    && containsStringField(line, "vaadinWebComponentResourceExecutedPath",
                            "META-INF/resources/frontend/vaadin-big-decimal-field.js")
                    && (containsStringField(line, "vaadinWebComponentResourceImportMode",
                            "dynamic-import-blob-with-v1_2-shimmed-transitive-imports")
                            || containsStringField(line, "vaadinWebComponentResourceImportMode",
                                    "dynamic-import-blob-with-manifest-import-rewrites-v1_4"))
                    && containsLiteralField(line, "vaadinWebComponentTagDefined", "true")
                    && containsLiteralField(line, "vaadinWebComponentResourceExecutionFailed", "false")) {
                summary.vaadinWebComponentResourceExecutedDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsStringField(line, "vaadinWebComponentResourceResolverStrategy",
                            "generic-component-frontend-manifest-v1_3")
                    && containsLiteralField(line, "vaadinWebComponentResourceExecuted", "true")
                    && (containsLiteralField(line, "vaadinWebComponentResourceExecutedCount", "2")
                            || containsLiteralField(line, "vaadinWebComponentResourceExecutedCount", "3")
                            || containsLiteralField(line, "vaadinWebComponentResourceExecutedCount", "4"))
                    && line.contains("META-INF/resources/frontend/vaadin-big-decimal-field.js")
                    && (line.contains("META-INF/vatori/frontend-shims/vaadin-checkbox.js")
                            || line.contains(
                                    "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                            || line.contains("META-INF/vatori/dev-bundle-entry/vaadin-checkbox.js"))
                    && (containsStringField(line, "vaadinWebComponentResourceExecutedTags",
                            "vaadin-big-decimal-field,vaadin-checkbox")
                            || containsStringField(line, "vaadinWebComponentResourceExecutedTags",
                                    "vaadin-big-decimal-field,vaadin-checkbox,vaadin-select")
                            || containsStringField(line, "vaadinWebComponentResourceExecutedTags",
                                    "vaadin-big-decimal-field,vaadin-text-field,vaadin-checkbox,vaadin-select"))
                    && containsLiteralField(line, "vaadinWebComponentResourceExecutionFailed", "false")) {
                summary.componentResolverV13DomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && line.contains("flowClientEngineResourceLoaded")
                    && line.contains("flowClientEngineResourceVerified")
                    && line.contains("flowClientEngineModuleLoaded")
                    && line.contains("flowClientEngineInitExported")
                    && line.contains("flowClientEngineInitialized")
                    && line.contains("flowClientEngineClientRegistered")
                    && line.contains("flowClientEngineIpcMessageBridgeInstalled")
                    && line.contains("flowClientEngineIpcResyncBridgeInstalled")
                    && line.contains("ROOT")
                    && line.contains("META-INF/frontend/FlowClient.js")) {
                summary.flowClientEngineDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "flowClientEngineBootstrapDomBound", "true")
                    && line.contains("flowClientEngineBoundNodeIds")
                    && line.contains("flowClientEngineBootstrapStatusText")
                    && line.contains("flowClientEngineBootstrapButtonText")
                    && line.contains("Increment over IPC")) {
                summary.flowClientEngineBootstrapDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "flowClientEngineListBound", "true")
                    && containsLiteralField(line, "flowClientEngineListItemCount", "4")
                    && line.contains("1 Bootstrap over IPC READY")
                    && line.contains("4 Desktop command/capabilityResult over IPC READY")) {
                summary.flowClientEngineListDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "flowClientEngineXhrBridgeInstalled", "true")
                    && containsLiteralField(line, "flowClientEngineXhrRequestObserved", "true")
                    && containsLiteralField(line, "flowClientEngineXhrResponseObserved", "true")
                    && containsLiteralField(line, "flowClientEngineEventPatchConsumed", "true")
                    && containsClickedStatusField(line, "flowClientEngineEventStatusText")
                    && containsStringField(line, "flowClientEngineXhrError", "")) {
                summary.flowClientEngineXhrDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "flowClientEngineTextInputEventObserved", "true")
                    && containsStringField(line, "flowClientEngineTextInputStatusText", "Text input: Flow IPC typed")
                    && containsLiteralField(line, "flowClientEngineCheckboxEventObserved", "true")
                    && containsStringField(line, "flowClientEngineCheckboxStatusText", "Checkbox: checked")
                    && containsPositiveIntegerField(line, "flowClientEngineComponentEventPatchCount")
                    && line.contains("ipc-text-input")
                    && line.contains("ipc-checkbox")) {
                summary.flowClientEngineComponentEventsDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "vaadinWebComponentRendered", "true")
                    && containsStringField(line, "vaadinWebComponentRenderedTag", "vaadin-big-decimal-field")
                    && containsStringField(line, "vaadinWebComponentEventDispatchMode", "dom-event")
                    && containsLiteralField(line, "vaadinWebComponentEventObserved", "true")
                    && containsLiteralField(line, "vaadinWebComponentEventRpcObserved", "true")
                    && containsStringField(line, "vaadinWebComponentEventComponentId", "ipc-vaadin-big-decimal")
                    && containsStringField(line, "vaadinWebComponentEventType", "change")
                    && containsStringField(line, "vaadinWebComponentEventValue", "42.5")
                    && containsStringField(line, "vaadinWebComponentStatusText", "Vaadin BigDecimalField: 42.5")
                    && containsStringField(line, "vaadinWebComponentPostEventState", "Vaadin BigDecimalField: 42.5")
                    && containsStringField(line, "vaadinWebComponentFailureReason", "")) {
                summary.vaadinWebComponentDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "vaadinTextFieldRendered", "true")
                    && containsStringField(line, "vaadinTextFieldRenderedTag", "vaadin-text-field")
                    && containsLiteralField(line, "vaadinTextFieldTagDefined", "true")
                    && containsStringField(line, "vaadinTextFieldEventDispatchMode", "dom-event")
                    && containsLiteralField(line, "vaadinTextFieldEventObserved", "true")
                    && containsLiteralField(line, "vaadinTextFieldEventRpcObserved", "true")
                    && containsStringField(line, "vaadinTextFieldEventComponentId", "ipc-vaadin-text-field")
                    && containsStringField(line, "vaadinTextFieldEventType", "change")
                    && containsStringField(line, "vaadinTextFieldEventValue", "Flow IPC typed")
                    && containsStringField(line, "vaadinTextFieldStatusText", "Vaadin TextField: Flow IPC typed")
                    && containsStringField(line, "vaadinTextFieldPostEventState", "Vaadin TextField: Flow IPC typed")
                    && containsStringField(line, "vaadinTextFieldFailureReason", "")) {
                summary.vaadinTextFieldDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "vaadinSecondWebComponentRendered", "true")
                    && containsStringField(line, "vaadinSecondWebComponentRenderedTag", "vaadin-checkbox")
                    && containsLiteralField(line, "vaadinSecondWebComponentTagDefined", "true")
                    && containsStringField(line, "vaadinSecondWebComponentEventDispatchMode", "dom-event")
                    && containsLiteralField(line, "vaadinSecondWebComponentEventObserved", "true")
                    && containsLiteralField(line, "vaadinSecondWebComponentEventRpcObserved", "true")
                    && containsStringField(line, "vaadinSecondWebComponentEventComponentId", "ipc-vaadin-checkbox")
                    && containsStringField(line, "vaadinSecondWebComponentEventType", "change")
                    && containsStringField(line, "vaadinSecondWebComponentEventValue", "true")
                    && containsStringField(line, "vaadinSecondWebComponentStatusText", "Vaadin Checkbox: checked")
                    && containsStringField(line, "vaadinSecondWebComponentPostEventState", "Vaadin Checkbox: checked")
                    && containsStringField(line, "vaadinSecondWebComponentFailureReason", "")) {
                summary.vaadinSecondWebComponentDomObserved = true;
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
                    && line.contains("flow-ipc-download-smoke.txt")
                    && line.contains("flow-ipc-upload-smoke.txt")) {
                summary.fileTransferDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "flowClientEngineIpcMessageBridgeInstalled", "true")
                    && containsLiteralField(line, "flowClientEnginePushPatchConsumed", "true")
                    && containsStringField(line, "flowClientEnginePushStatusText", "5 Server push over IPC READY")
                    && containsLiteralField(line, "flowClientEngineQueuedMessageCount", "0")) {
                summary.flowClientEnginePushDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsPositiveIntegerField(line, "flowClientEngineQueuedMessageHighWater")
                    && containsPositiveIntegerField(line, "flowClientEngineQueuedPushCount")
                    && containsPositiveIntegerField(line, "flowClientEngineDeliveredQueuedMessageCount")
                    && line.contains("flowClientEngineQueuedMessageOrder")
                    && line.contains("flowClientEngineDeliveredQueuedMessageOrder")
                    && line.contains("push:3")) {
                summary.flowClientEnginePushQueueDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsStringField(line, "flowClientEnginePushOrderStatusText", "6 Ordered push over IPC READY")
                    && containsLiteralField(line, "flowClientEngineDeliveredPushCount", "2")
                    && containsStringField(line, "flowClientEngineDeliveredMessageOrder", "push:3,push:4")
                    && containsLiteralField(line, "flowClientEngineIgnoredMessageCount", "1")
                    && containsStringField(line, "flowClientEngineIgnoredMessageOrder", "push:4:duplicate-late")
                    && containsLiteralField(line, "flowClientEngineDuplicateLateIgnored", "true")
                    && containsLiteralField(line, "flowClientEngineOrderingLastDeliveredServerSequence", "4")
                    && containsLiteralField(line, "flowClientEngineRecoveryResyncRequested", "true")
                    && containsStringField(line, "flowClientEngineRecoveryResyncReason",
                            "ordering-recovery-duplicate-late")) {
                summary.flowClientEnginePushOrderingDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "flowClientEngineIpcResyncBridgeInstalled", "true")
                    && containsLiteralField(line, "flowClientEngineResyncRequested", "true")
                    && containsLiteralField(line, "flowClientEngineResyncPatchConsumed", "true")) {
                summary.flowClientEngineResyncDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "flowClientEngineVisibleOwner", "true")
                    && containsStringField(line, "flowClientEngineVisibleRootId", "flow-client-engine-root")
                    && containsClickedStatusField(line, "flowClientEngineVisibleStatusText")
                    && containsStringField(line, "flowClientEngineVisibleButtonText", "Increment over IPC")
                    && containsLiteralField(line, "flowClientEngineLocalFallbackHidden", "true")) {
                summary.flowClientEngineVisibleDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "flowClientEngineViewportReady", "true")
                    && line.contains("flowClientEngineViewportRootTag")
                    && containsStringField(line, "flowClientEnginePromotionGate", "generic-flow-viewport")
                    && containsLiteralField(line, "flowClientEngineVisibleOwner", "true")
                    && containsLiteralField(line, "flowClientEngineLocalFallbackHidden", "true")) {
                summary.flowClientEngineGenericViewportDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.firstMeaningfulPaint\"")
                    && containsStringField(line, "transport", "flow-ipc")
                    && containsStringField(line, "promotionGate", "generic-flow-viewport")
                    && containsLiteralField(line, "flowClientEngineVisibleOwner", "true")
                    && containsLiteralField(line, "flowClientEngineLocalFallbackHidden", "true")) {
                summary.firstMeaningfulPaintObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && line.contains("Flow IPC Framework Desk")
                    && containsLiteralField(line, "frameworkAppRendered", "true")
                    && containsStringField(line, "frameworkAppRootId", "framework-root")
                    && containsLiteralField(line, "frameworkAppBudgetRendered", "true")
                    && containsLiteralField(line, "frameworkAppCheckboxRendered", "true")
                    && containsLiteralField(line, "frameworkAppSelectRendered", "true")
                    && containsLiteralField(line, "frameworkAppApplyRendered", "true")
                    && containsStringField(line, "flowClientEngineViewportRootId", "framework-root")
                    && containsStringField(line, "flowClientEnginePromotionGate", "generic-flow-viewport")
                    && containsLiteralField(line, "flowClientEngineBootstrapDomBound", "false")
                    && containsLiteralField(line, "flowClientEngineVisibleOwner", "true")
                    && containsLiteralField(line, "flowClientEngineLocalFallbackHidden", "true")) {
                summary.frameworkAppDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "frameworkAppBudgetEventRpcObserved", "true")
                    && containsLiteralField(line, "frameworkAppBudgetEventObserved", "true")
                    && containsLiteralField(line, "frameworkAppCheckboxEventRpcObserved", "true")
                    && containsLiteralField(line, "frameworkAppCheckboxEventObserved", "true")
                    && containsLiteralField(line, "frameworkAppSelectEventRpcObserved", "true")
                    && containsLiteralField(line, "frameworkAppSelectEventObserved", "true")
                    && containsLiteralField(line, "frameworkAppApplyEventRpcObserved", "true")
                    && containsLiteralField(line, "frameworkAppApplyEventObserved", "true")
                    && containsLiteralField(line, "frameworkAppInteractionComplete", "true")
                    && containsPositiveIntegerField(line, "frameworkAppEventPatchCount")
                    && line.contains("framework-budget")
                    && line.contains("framework-expedite")
                    && line.contains("framework-priority")
                    && line.contains("framework-apply")
                    && line.contains("Revision 1:")
                    && line.contains("priority audit")
                    && line.contains("Renderer owned by Flow client engine")) {
                summary.frameworkAppInteractionDomObserved = true;
            }
            if (line.contains("\"type\":\"event\"")
                    && line.contains("\"sessionId\":\"flow-ipc-session\"")
                    && line.contains("\"uiId\":\"main\"")) {
                summary.eventScopeObserved = true;
            }
            if (line.contains("\"type\":\"resync\"")
                    && line.contains("\"sessionId\":\"flow-ipc-session\"")
                    && line.contains("\"uiId\":\"main\"")) {
                summary.resyncScopeObserved = true;
            }
            if (line.contains("\"type\":\"patch\"")
                    && line.contains("\"source\":\"UidlRequestHandler\"")
                    && line.contains("\"eventDispatch\":\"flow-client-xhr\"")
                    && containsClickedText(line)
                    && line.contains("\"consumedByWebView\":true")
                    && line.contains("\"flow-client-engine-visible\"")) {
                summary.flowClientXhrPatchObserved = true;
            }
            if (line.contains("\"type\":\"patch\"")
                    && line.contains("\"source\":\"UidlRequestHandler\"")
                    && line.contains("\"eventDispatch\":\"flow-client-xhr\"")
                    && line.contains("Text input: Flow IPC typed")
                    && line.contains("\"consumedByWebView\":true")
                    && line.contains("\"flow-client-engine-visible\"")) {
                summary.textInputPatchObserved = true;
            }
            if (line.contains("\"type\":\"patch\"")
                    && line.contains("\"source\":\"UidlRequestHandler\"")
                    && line.contains("\"eventDispatch\":\"flow-client-xhr\"")
                    && line.contains("Checkbox: checked")
                    && line.contains("\"consumedByWebView\":true")
                    && line.contains("\"flow-client-engine-visible\"")) {
                summary.checkboxPatchObserved = true;
            }
            if (line.contains("\"type\":\"patch\"")
                    && line.contains("\"source\":\"UidlRequestHandler\"")
                    && line.contains("\"eventDispatch\":\"flow-client-xhr\"")
                    && line.contains("Vaadin BigDecimalField: 42.5")
                    && line.contains("\"consumedByWebView\":true")
                    && line.contains("\"flow-client-engine-visible\"")) {
                summary.vaadinWebComponentPatchObserved = true;
            }
            if (line.contains("\"type\":\"patch\"")
                    && line.contains("\"source\":\"UidlRequestHandler\"")
                    && line.contains("\"eventDispatch\":\"flow-client-xhr\"")
                    && line.contains("Vaadin TextField: Flow IPC typed")
                    && line.contains("\"consumedByWebView\":true")
                    && line.contains("\"flow-client-engine-visible\"")) {
                summary.vaadinTextFieldPatchObserved = true;
            }
            if (line.contains("\"type\":\"event\"")
                    && line.contains("\"dispatch\":\"flow-client-xhr\"")
                    && line.contains("\"componentId\":\"ipc-vaadin-checkbox\"")
                    && line.contains("\"event\":\"change\"")
                    && line.contains("element.checked")
                    && line.contains("\"uidlRpc\"")) {
                summary.vaadinSecondWebComponentEventRpcObserved = true;
            }
            if (line.contains("\"type\":\"patch\"")
                    && line.contains("\"source\":\"UidlRequestHandler\"")
                    && line.contains("\"eventDispatch\":\"flow-client-xhr\"")
                    && line.contains("Vaadin Checkbox: checked")
                    && line.contains("\"consumedByWebView\":true")
                    && line.contains("\"flow-client-engine-visible\"")) {
                summary.vaadinSecondWebComponentPatchObserved = true;
            }
            if (line.contains("\"type\":\"patch\"")
                    && line.contains("\"source\":\"UidlRequestHandler\"")
                    && line.contains("\"consumedByWebView\":true")
                    && line.contains("\"flow-client-engine-visible\"")) {
                summary.uidlPatchObserved = true;
            }
            if (line.contains("\"type\":\"push\"")
                    && line.contains("\"source\":\"UidlWriter\"")
                    && line.contains("\"reason\":\"push\"")
                    && line.contains("\"async\":true")
                    && line.contains("\"consumedByWebView\":true")
                    && line.contains("\"flow-client-engine-visible\"")) {
                summary.pushUidlObserved = true;
            }
            if (line.contains("\"type\":\"push\"")
                    && line.contains("\"pushIndex\":2")
                    && line.contains("6 Ordered push over IPC READY")
                    && line.contains("\"source\":\"UidlWriter\"")
                    && line.contains("\"reason\":\"push\"")
                    && line.contains("\"async\":true")
                    && line.contains("\"flow-client-engine-visible\"")) {
                summary.multiPushUidlObserved = true;
            }
            if (line.contains("\"type\":\"push\"")
                    && line.contains("\"orderingProbe\"")
                    && line.contains("\"kind\":\"duplicate-late\"")
                    && line.contains("\"effectiveServerSequence\":4")
                    && line.contains("\"expectedAction\":\"ignore-and-request-resync\"")) {
                summary.duplicateLatePushProbeObserved = true;
            }
            if (line.contains("\"type\":\"resync\"")
                    && line.contains("\"reason\":\"ordering-recovery-duplicate-late\"")
                    && line.contains("\"securityKey\"")
                    && line.contains("\"syncId\"")
                    && line.contains("\"clientId\"")) {
                summary.recoveryResyncRequestObserved = true;
            }
            if (line.contains("\"type\":\"patch\"")
                    && line.contains("\"source\":\"UidlRequestHandler\"")
                    && line.contains("\"reason\":\"resync\"")
                    && line.contains("\"eventDispatch\":\"flow-client-xhr\"")
                    && line.contains("\"resync\":true")
                    && line.contains("resynchronize")
                    && line.contains("\"consumedByWebView\":true")
                    && line.contains("\"flow-client-engine-visible\"")) {
                summary.resyncUidlObserved = true;
            }
        }
        return summary;
    }

    private static String normalize(String line) {
        String trimmed = line.trim();
        String payload = trimmed.startsWith("HOST_PROTOCOL ") ? trimmed.substring("HOST_PROTOCOL ".length()) : trimmed;
        return payload.replace("\\/", "/");
    }

    private static String first(Pattern pattern, String line, String fallback) {
        Matcher matcher = pattern.matcher(line);
        return matcher.find() ? matcher.group(1) : fallback;
    }

    private static boolean containsLiteralField(String line, String field, String value) {
        return line.contains("\"" + field + "\":" + value)
                || line.contains("\\\"" + field + "\\\":" + value);
    }

    private static boolean containsStringField(String line, String field, String value) {
        String slashEscapedValue = value.replace("/", "\\/");
        return line.contains("\"" + field + "\":\"" + value + "\"")
                || line.contains("\"" + field + "\":\"" + slashEscapedValue + "\"")
                || line.contains("\\\"" + field + "\\\":\\\"" + value + "\\\"")
                || line.contains("\\\"" + field + "\\\":\\\"" + slashEscapedValue + "\\\"");
    }

    private static boolean containsStringFieldPrefix(String line, String field, String valuePrefix) {
        String slashEscapedValuePrefix = valuePrefix.replace("/", "\\/");
        return line.contains("\"" + field + "\":\"" + valuePrefix)
                || line.contains("\"" + field + "\":\"" + slashEscapedValuePrefix)
                || line.contains("\\\"" + field + "\\\":\\\"" + valuePrefix)
                || line.contains("\\\"" + field + "\\\":\\\"" + slashEscapedValuePrefix);
    }

    private static boolean containsProductionOwnerFailureLayer(String line, String field) {
        return containsStringField(line, field, "production-owner-duplicate-custom-element")
                || containsStringField(line, field, "production-owner-relative-import-unresolved")
                || containsStringField(line, field, "production-owner-missing-real-artifact")
                || containsStringField(line, field, "production-owner-browser-import-failed")
                || containsStringField(line, field, "production-owner-side-effect-unobservable")
                || containsStringField(line, field, "production-owner-global-collision")
                || containsStringField(line, field, "production-owner-runtime-contract-missing");
    }

    private static boolean containsProductionOwnerImportFailureLayer(String line, String field) {
        return containsStringField(line, field, "production-owner-duplicate-custom-element")
                || containsStringField(line, field, "production-owner-relative-import-unresolved")
                || containsStringField(line, field, "production-owner-missing-real-artifact")
                || containsStringField(line, field, "production-owner-browser-import-failed")
                || containsStringField(line, field, "production-owner-side-effect-unobservable")
                || containsStringField(line, field, "production-owner-global-collision");
    }

    private static boolean containsProductionOwnerDependencyFailureLayer(String line, String field) {
        return containsStringField(line, field, "production-owner-duplicate-custom-element")
                || containsStringField(line, field, "production-owner-missing-real-artifact")
                || containsStringField(line, field, "production-owner-browser-import-failed")
                || containsStringField(line, field, "production-owner-side-effect-unobservable")
                || containsStringField(line, field, "production-owner-global-collision")
                || containsStringField(line, field, "production-owner-component-reuse-contract-missing")
                || containsStringField(line, field, "production-owner-unsupported-vaadin-production-owner-contract");
    }

    private static boolean containsProductionOwnerOrderingFailureLayer(String line, String field) {
        return containsStringField(line, field, "production-owner-duplicate-custom-element")
                || containsStringField(line, field, "production-owner-browser-import-failed")
                || containsStringField(line, field, "production-owner-side-effect-unobservable")
                || containsStringField(line, field, "production-owner-global-collision")
                || containsStringField(line, field, "production-owner-component-reuse-contract-missing")
                || containsStringField(line, field, "production-owner-unsupported-vaadin-production-owner-contract");
    }

    private static boolean containsOwnerFirstOrIsolatedPhase(String line, String field) {
        return containsStringField(line, field, "before-component-imports")
                || containsStringField(line, field, "isolated-preflight");
    }

    private static boolean containsProductionOwnerImportFinalResult(String line, String resultField,
            String failureLayerField) {
        return containsStringFieldPrefix(line, resultField,
                "imported:vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                || (containsStringFieldPrefix(line, resultField, "strict-blocker:production-owner-")
                        && containsProductionOwnerImportFailureLayer(line, failureLayerField)
                        && !containsStringField(line, failureLayerField,
                                "production-owner-runtime-contract-missing"));
    }

    private static boolean containsProductionOwnerDependencyFinalResult(String line, String resultField,
            String failureLayerField) {
        return containsStringFieldPrefix(line, resultField,
                "imported:vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                || (containsStringFieldPrefix(line, resultField, "strict-blocker:production-owner-")
                        && containsProductionOwnerDependencyFailureLayer(line, failureLayerField));
    }

    private static boolean containsProductionOwnerOrderingFinalResult(String line, String resultField,
            String failureLayerField) {
        return containsStringFieldPrefix(line, resultField,
                "imported:vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                || (containsStringFieldPrefix(line, resultField, "strict-blocker:production-owner-")
                        && containsProductionOwnerOrderingFailureLayer(line, failureLayerField));
    }

    private static boolean containsGeneratedChunkFileNoOpResolution(String line) {
        return containsLiteralField(line, "loadOnDemandChunkFileCallbackResolved", "true")
                || (containsLiteralField(line, "loadOnDemandChunkFileCallbackResolved", "false")
                        && containsStringField(line, "loadOnDemandChunkFileFailureLayer",
                                "chunk-file-not-generated")
                        && containsStringField(line, "loadOnDemandChunkFileSideEffectProof",
                                "no-chunk-generated-by-real-generated-flow-imports-no-op-table"));
    }

    private static boolean containsGeneratedChunkFileStrictBlockerResult(String line) {
        return containsStringFieldPrefix(line, "loadOnDemandChunkFileExecutionResult",
                "strict-blocker:chunk-file-not-generated")
                || containsStringFieldPrefix(line, "loadOnDemandChunkFileExecutionResult",
                        "strict-blocker:not-generated")
                || (containsStringField(line, "chunkFileStatus", "not-generated")
                        && containsStringField(line, "loadOnDemandChunkFileFailureLayer",
                                "chunk-file-not-generated"));
    }

    private static boolean containsPath(String line, String path) {
        return line.contains(path) || line.contains(path.replace("/", "\\/"));
    }

    private static boolean containsPositiveIntegerField(String line, String field) {
        for (int digit = 1; digit <= 9; digit++) {
            if (line.contains("\"" + field + "\":" + digit)
                    || line.contains("\\\"" + field + "\\\":" + digit)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsClickedStatusField(String line, String field) {
        for (int digit = 1; digit <= 9; digit++) {
            String prefix = "Clicks: " + digit;
            if (line.contains("\"" + field + "\":\"" + prefix)
                    || line.contains("\\\"" + field + "\\\":\\\"" + prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsClickedText(String line) {
        for (int digit = 1; digit <= 9; digit++) {
            if (line.contains("Clicks: " + digit)) {
                return true;
            }
        }
        return false;
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
                || line.contains("\"permissionMode\":\"manifest-enforced\"")
                || line.contains("\"permissionMode\":\"trace-only-dry-run\"");
    }

    private static void usage() {
        System.out.println("Usage: bin/flow-ipc-trace-inspect [" + AcceptanceGate.usageOptions()
                + "] <trace.ndjson>...");
    }

    public static final class TraceSummary {
        private int total;
        private final Map<String, Integer> types = new LinkedHashMap<>();
        private final Map<String, Integer> directions = new LinkedHashMap<>();
        private boolean flowIpcTransport;
        private boolean compatibilityFalse;
        private boolean capabilityManifestObserved;
        private boolean capabilityManifestResultObserved;
        private boolean fileDialogResultObserved;
        private boolean nativeFileDialogResultObserved;
        private boolean manifestEnforcedObserved;
        private boolean capabilityDeniedObserved;
        private boolean fileTransferManifestObserved;
        private boolean fileTransferDownloadCommandRedactedObserved;
        private boolean fileTransferDownloadResultObserved;
        private boolean fileTransferUploadResultObserved;
        private boolean fileTransferDomObserved;
        private boolean serverSequenceObserved;
        private boolean flowIpcServerSequenceDomObserved;
        private boolean clickPatchObserved;
        private boolean nativeClipboardObserved;
        private boolean capabilityPermissionObserved;
        private boolean webViewErrorObserved;
        private boolean uidlWriterObserved;
        private boolean securityKeyObserved;
        private boolean resyncSecurityKeyObserved;
        private boolean bootstrapScopeObserved;
        private boolean eventScopeObserved;
        private boolean resyncScopeObserved;
        private boolean flowClientResourceManifestObserved;
        private boolean flowClientResourceRequestObserved;
        private boolean flowClientResourceResponseObserved;
        private boolean flowClientResourceDomObserved;
        private boolean flowClientResourceGraphDomObserved;
        private boolean flowClientResourceExecutionClassificationDomObserved;
        private boolean flowClientResourceLazyLookupRequestObserved;
        private boolean flowClientResourceLazyLookupResponseObserved;
        private boolean flowClientResourceLazyLookupDomObserved;
        private boolean flowClientEngineResourceManifestObserved;
        private boolean flowClientEngineResourceRequestObserved;
        private boolean flowClientEngineResourceResponseObserved;
        private boolean flowClientEngineDomObserved;
        private boolean flowClientEngineBootstrapDomObserved;
        private boolean flowClientEngineListDomObserved;
        private boolean flowClientEngineXhrDomObserved;
        private boolean flowClientEngineVisibleDomObserved;
        private boolean flowClientEnginePushDomObserved;
        private boolean flowClientEnginePushQueueDomObserved;
        private boolean flowClientEnginePushOrderingDomObserved;
        private boolean flowClientEngineResyncDomObserved;
        private boolean flowClientEngineGenericViewportDomObserved;
        private boolean uidlRpcDispatchObserved;
        private boolean webViewUidlRpcObserved;
        private boolean flowClientXhrAttachObserved;
        private boolean flowClientXhrClickObserved;
        private boolean flowClientXhrTextInputObserved;
        private boolean flowClientXhrCheckboxObserved;
        private boolean flowClientXhrPatchObserved;
        private boolean textInputPatchObserved;
        private boolean checkboxPatchObserved;
        private boolean flowClientEngineComponentEventsDomObserved;
        private boolean vaadinWebComponentResourceManifestObserved;
        private boolean vaadinWebComponentResourceRequestObserved;
        private boolean vaadinWebComponentResourceResponseObserved;
        private boolean vaadinWebComponentResourceDomObserved;
        private boolean vaadinWebComponentResourceExecutedDomObserved;
        private boolean vaadinWebComponentFailureClassificationObserved;
        private boolean vaadinWebComponentEventRpcObserved;
        private boolean vaadinWebComponentPatchObserved;
        private boolean vaadinWebComponentDomObserved;
        private boolean vaadinTextFieldEventRpcObserved;
        private boolean vaadinTextFieldPatchObserved;
        private boolean vaadinTextFieldDomObserved;
        private boolean componentResolverV13ManifestObserved;
        private boolean componentResolverV13DomObserved;
        private boolean vaadinSecondWebComponentResourceRequestObserved;
        private boolean vaadinSecondWebComponentResourceResponseObserved;
        private boolean vaadinSecondWebComponentDevBundleResourceObserved;
        private boolean vaadinSecondWebComponentEventRpcObserved;
        private boolean vaadinSecondWebComponentPatchObserved;
        private boolean vaadinSecondWebComponentDomObserved;
        private boolean uidlPatchObserved;
        private boolean pushUidlObserved;
        private boolean multiPushUidlObserved;
        private boolean duplicateLatePushProbeObserved;
        private boolean recoveryResyncRequestObserved;
        private boolean resyncUidlObserved;
        private boolean frameworkAppLaunchObserved;
        private boolean frameworkAppCapabilityManifestObserved;
        private boolean frameworkAppBootstrapObserved;
        private boolean frameworkAppBudgetEventRpcObserved;
        private boolean frameworkAppCheckboxEventRpcObserved;
        private boolean frameworkAppSelectEventRpcObserved;
        private boolean frameworkAppApplyEventRpcObserved;
        private boolean frameworkAppBudgetPatchObserved;
        private boolean frameworkAppCheckboxPatchObserved;
        private boolean frameworkAppSelectPatchObserved;
        private boolean frameworkAppApplyPatchObserved;
        private boolean frameworkAppWindowTitleCommandObserved;
        private boolean frameworkAppClipboardCommandObserved;
        private boolean frameworkAppNotificationCommandObserved;
        private boolean frameworkAppWindowTitleResultObserved;
        private boolean frameworkAppClipboardResultObserved;
        private boolean frameworkAppNotificationResultObserved;
        private boolean frameworkAppDomObserved;
        private boolean frameworkAppInteractionDomObserved;
        private boolean firstMeaningfulPaintObserved;
        private boolean frontendAssetPipelineV14Observed;
        private boolean frontendArtifactFamilyModelObserved;
        private boolean frontendDependencyGraphObserved;
        private boolean frontendShimBlockerObserved;
        private boolean frontendManifestImportRewriteObserved;
        private boolean frontendTextFieldDevBundleBlockerObserved;
        private boolean frontendProductionBundlePartialObserved;
        private boolean frontendProductionBundleStatsResourceObserved;
        private boolean frontendThemeStaticResourceObserved;
        private boolean frontendThemeStaticStrictBlockerObserved;
        private boolean frontendGeneratedLazyResourceObserved;
        private boolean frontendGeneratedLazyBundleChunkResourceObserved;
        private boolean frontendDevModeResourceObserved;
        private boolean frontendDevModeStrictBlockerObserved;
        private boolean frontendGeneratedLazyStrictBlockerObserved;
        private boolean frontendResourceStageObserved;
        private boolean frontendResourceStageExecutedObserved;
        private boolean frontendResourceStageStrictBlockerObserved;
        private boolean frontendResourceStageFailureClassificationObserved;
        private boolean frontendStagingDomObserved;
        private boolean frontendTextFieldStagedExecutionObserved;
        private boolean frontendTextFieldStagedBlockerObserved;
        private boolean frontendTextFieldV16ManifestObserved;
        private boolean frontendTextFieldV16RequestObserved;
        private boolean frontendTextFieldV16StageBlockerObserved;
        private boolean frontendTextFieldV16DependencyClassificationObserved;
        private boolean frontendTextFieldV16DomObserved;
        private boolean frontendTextFieldV17ManifestObserved;
        private boolean frontendTextFieldV17RequestObserved;
        private boolean frontendTextFieldV17StageExecutedObserved;
        private boolean frontendTextFieldV17ModuleIdentityDependencyObserved;
        private boolean frontendTextFieldV17DomObserved;
        private boolean frontendGeneratedLazyExecutionAttemptObserved;
        private boolean frontendGeneratedLazyExecutionResolvedObserved;
        private boolean frontendLoadOnDemandBoundaryObserved;
        private boolean frontendGeneratedLazyV18ManifestObserved;
        private boolean frontendGeneratedLazyV18RequestObserved;
        private boolean frontendGeneratedLazyV18DependencyRewriteObserved;
        private boolean frontendGeneratedLazyV18ExecutionOrNarrowedBlockerObserved;
        private boolean frontendGeneratedLazyV18DomObserved;
        private boolean frontendGeneratedLazyV19ManifestObserved;
        private boolean frontendGeneratedLazyV19RequestObserved;
        private boolean frontendGeneratedLazyV19StageObserved;
        private boolean frontendGeneratedLazyV19DomObserved;
        private boolean frontendRuntimeResolverV110ManifestObserved;
        private boolean flowLazyDependencyV110ManifestObserved;
        private boolean flowLazyDependencyV110RequestObserved;
        private boolean flowLazyDependencyV110StageObserved;
        private boolean flowLazyDependencyV110DomObserved;
        private boolean productionRuntimeV110ManifestObserved;
        private boolean productionRuntimeV110RequestObserved;
        private boolean productionRuntimeV110StageObserved;
        private boolean productionRuntimeV110DomObserved;
        private boolean generatedLoadOnDemandChunkV111ManifestObserved;
        private boolean generatedLoadOnDemandChunkV111UidlExpressionObserved;
        private boolean generatedLoadOnDemandChunkV111RequestObserved;
        private boolean generatedLoadOnDemandChunkV111StageObserved;
        private boolean generatedLoadOnDemandChunkV111DomObserved;
        private boolean generatedLoadOnDemandChunkFileV112ManifestObserved;
        private boolean generatedLoadOnDemandChunkFileV112RequestObserved;
        private boolean generatedLoadOnDemandChunkFileV112StageObserved;
        private boolean generatedLoadOnDemandChunkFileV112DomObserved;
        private boolean productionRuntimeOwnerV113ManifestObserved;
        private boolean productionRuntimeOwnerV113RequestObserved;
        private boolean productionRuntimeOwnerV113StageObserved;
        private boolean productionRuntimeOwnerV113DomObserved;
        private boolean productionRuntimeOwnerImportV114ManifestObserved;
        private boolean productionRuntimeOwnerImportV114RequestObserved;
        private boolean productionRuntimeOwnerImportV114StageObserved;
        private boolean productionRuntimeOwnerImportV114DomObserved;
        private boolean productionRuntimeOwnerDependenciesV115ManifestObserved;
        private boolean productionRuntimeOwnerDependenciesV115RequestObserved;
        private boolean productionRuntimeOwnerDependenciesV115StageObserved;
        private boolean productionRuntimeOwnerDependenciesV115DomObserved;
        private boolean productionRuntimeOwnerOrderingV116ManifestObserved;
        private boolean productionRuntimeOwnerOrderingV116RequestObserved;
        private boolean productionRuntimeOwnerOrderingV116StageObserved;
        private boolean productionRuntimeOwnerOrderingV116DomObserved;

        public boolean hasRequiredFlowIpcTypes() {
            return types.keySet().containsAll(FLOW_IPC_REQUIRED_TYPES)
                    && flowIpcTransport
                    && compatibilityFalse
                    && capabilityManifestObserved
                    && capabilityManifestResultObserved
                    && fileDialogResultObserved
                    && serverSequenceObserved
                    && flowIpcServerSequenceDomObserved
                    && clickPatchObserved
                    && nativeClipboardObserved
                    && capabilityPermissionObserved
                    && webViewErrorObserved
                    && uidlWriterObserved
                    && securityKeyObserved
                    && resyncSecurityKeyObserved
                    && bootstrapScopeObserved
                    && eventScopeObserved
                    && resyncScopeObserved
                    && flowClientResourceManifestObserved
                    && flowClientResourceRequestObserved
                    && flowClientResourceResponseObserved
                    && flowClientResourceDomObserved
                    && flowClientResourceGraphDomObserved
                    && flowClientEngineResourceManifestObserved
                    && flowClientEngineResourceRequestObserved
                    && flowClientEngineResourceResponseObserved
                    && flowClientEngineDomObserved
                    && flowClientEngineBootstrapDomObserved
                    && flowClientEngineListDomObserved
                    && flowClientEngineXhrDomObserved
                    && flowClientEngineVisibleDomObserved
                    && flowClientEnginePushDomObserved
                    && flowClientEnginePushQueueDomObserved
                    && flowClientEngineResyncDomObserved
                    && uidlRpcDispatchObserved
                    && webViewUidlRpcObserved
                    && flowClientXhrAttachObserved
                    && flowClientXhrClickObserved
                    && flowClientXhrPatchObserved
                    && uidlPatchObserved
                    && pushUidlObserved
                    && resyncUidlObserved;
        }

        public boolean hasRequiredFlowIpcV1_1Types() {
            return hasRequiredFlowIpcTypes()
                    && nativeFileDialogResultObserved
                    && manifestEnforcedObserved
                    && capabilityDeniedObserved
                    && fileTransferManifestObserved
                    && fileTransferDownloadCommandRedactedObserved
                    && fileTransferDownloadResultObserved
                    && fileTransferUploadResultObserved
                    && fileTransferDomObserved
                    && multiPushUidlObserved
                    && duplicateLatePushProbeObserved
                    && flowClientEnginePushOrderingDomObserved
                    && recoveryResyncRequestObserved
                    && flowClientResourceExecutionClassificationDomObserved
                    && flowClientResourceLazyLookupRequestObserved
                    && flowClientResourceLazyLookupResponseObserved
                    && flowClientResourceLazyLookupDomObserved
                    && flowClientXhrTextInputObserved
                    && flowClientXhrCheckboxObserved
                    && textInputPatchObserved
                    && checkboxPatchObserved
                    && flowClientEngineComponentEventsDomObserved;
        }

        public boolean hasRequiredFlowIpcV1_2Types() {
            return hasRequiredFlowIpcV1_1Types()
                    && vaadinWebComponentResourceManifestObserved
                    && vaadinWebComponentResourceRequestObserved
                    && vaadinWebComponentResourceResponseObserved
                    && vaadinWebComponentResourceDomObserved
                    && vaadinWebComponentResourceExecutedDomObserved
                    && vaadinWebComponentFailureClassificationObserved
                    && vaadinWebComponentEventRpcObserved
                    && vaadinWebComponentPatchObserved
                    && vaadinWebComponentDomObserved;
        }

        public boolean hasRequiredFlowIpcV1_3Types() {
            return hasRequiredFlowIpcV1_2Types()
                    && flowClientEngineGenericViewportDomObserved
                    && componentResolverV13ManifestObserved
                    && componentResolverV13DomObserved
                    && vaadinSecondWebComponentResourceRequestObserved
                    && vaadinSecondWebComponentResourceResponseObserved
                    && vaadinSecondWebComponentEventRpcObserved
                    && vaadinSecondWebComponentPatchObserved
                    && vaadinSecondWebComponentDomObserved;
        }

        public boolean hasRequiredFlowIpcV1_4Types() {
            return hasRequiredFlowIpcV1_3Types()
                    && frontendAssetPipelineV14Observed
                    && frontendArtifactFamilyModelObserved
                    && frontendDependencyGraphObserved
                    && frontendShimBlockerObserved
                    && frontendManifestImportRewriteObserved
                    && frontendTextFieldDevBundleBlockerObserved
                    && frontendProductionBundlePartialObserved
                    && frontendProductionBundleStatsResourceObserved
                    && frontendThemeStaticResourceObserved
                    && frontendDevModeResourceObserved
                    && frontendDevModeStrictBlockerObserved
                    && frontendGeneratedLazyStrictBlockerObserved
                    && frontendGeneratedLazyResourceObserved
                    && frontendGeneratedLazyBundleChunkResourceObserved;
        }

        public boolean hasRequiredFlowIpcV1_5Types() {
            return hasRequiredFlowIpcV1_4Types()
                    && frontendResourceStageObserved
                    && frontendStagingDomObserved
                    && (frontendResourceStageExecutedObserved || frontendResourceStageStrictBlockerObserved)
                    && frontendResourceStageFailureClassificationObserved
                    && (frontendTextFieldStagedExecutionObserved || frontendTextFieldStagedBlockerObserved)
                    && frontendGeneratedLazyExecutionAttemptObserved
                    && frontendGeneratedLazyExecutionResolvedObserved
                    && frontendLoadOnDemandBoundaryObserved;
        }

        public boolean hasRequiredFlowIpcV1_6Types() {
            return hasRequiredFlowIpcV1_5Types()
                    && frontendTextFieldV16ManifestObserved
                    && frontendTextFieldV16RequestObserved
                    && frontendTextFieldV16StageBlockerObserved
                    && frontendTextFieldV16DependencyClassificationObserved
                    && frontendTextFieldV16DomObserved;
        }

        public boolean hasRequiredFlowIpcV1_7Types() {
            return hasRequiredFlowIpcV1_5Types()
                    && frontendTextFieldV17ManifestObserved
                    && frontendTextFieldV17RequestObserved
                    && frontendTextFieldV17StageExecutedObserved
                    && frontendTextFieldV17ModuleIdentityDependencyObserved
                    && frontendTextFieldV17DomObserved
                    && vaadinTextFieldEventRpcObserved
                    && vaadinTextFieldPatchObserved
                    && vaadinTextFieldDomObserved;
        }

        public boolean hasRequiredFlowIpcV1_8Types() {
            return hasRequiredFlowIpcV1_7Types()
                    && frontendGeneratedLazyV18ManifestObserved
                    && (frontendGeneratedLazyV18RequestObserved || generatedLoadOnDemandChunkV111RequestObserved)
                    && frontendGeneratedLazyV18DependencyRewriteObserved
                    && frontendGeneratedLazyV18ExecutionOrNarrowedBlockerObserved
                    && frontendGeneratedLazyV18DomObserved;
        }

        public boolean hasRequiredFlowIpcV1_9LoadOnDemandTypes() {
            return frontendGeneratedLazyV19ManifestObserved
                    && (frontendGeneratedLazyV19RequestObserved || generatedLoadOnDemandChunkV111RequestObserved)
                    && frontendGeneratedLazyV19StageObserved
                    && frontendGeneratedLazyV19DomObserved;
        }

        public boolean hasRequiredFlowIpcV1_9Types() {
            return hasRequiredFlowIpcV1_8Types()
                    && hasRequiredFlowIpcV1_9LoadOnDemandTypes();
        }

        public boolean hasRequiredFlowIpcV1_10LazyDependencyTypes() {
            return frontendRuntimeResolverV110ManifestObserved
                    && flowLazyDependencyV110ManifestObserved
                    && flowLazyDependencyV110RequestObserved
                    && flowLazyDependencyV110StageObserved
                    && flowLazyDependencyV110DomObserved;
        }

        public boolean hasRequiredFlowIpcV1_10ProductionRuntimeTypes() {
            return frontendRuntimeResolverV110ManifestObserved
                    && productionRuntimeV110ManifestObserved
                    && productionRuntimeV110RequestObserved
                    && productionRuntimeV110StageObserved
                    && productionRuntimeV110DomObserved;
        }

        public boolean hasRequiredFlowIpcV1_10Types() {
            return hasRequiredFlowIpcV1_9Types()
                    && hasRequiredFlowIpcV1_10LazyDependencyTypes()
                    && hasRequiredFlowIpcV1_10ProductionRuntimeTypes();
        }

        public boolean hasRequiredFlowIpcV1_11LoadOnDemandChunkTypes() {
            return generatedLoadOnDemandChunkV111ManifestObserved
                    && generatedLoadOnDemandChunkV111UidlExpressionObserved
                    && generatedLoadOnDemandChunkV111RequestObserved
                    && generatedLoadOnDemandChunkV111StageObserved
                    && generatedLoadOnDemandChunkV111DomObserved;
        }

        public boolean hasRequiredFlowIpcV1_11Types() {
            return hasRequiredFlowIpcV1_10Types()
                    && hasRequiredFlowIpcV1_11LoadOnDemandChunkTypes();
        }

        public boolean hasRequiredFlowIpcV1_12LoadOnDemandChunkFileTypes() {
            return generatedLoadOnDemandChunkFileV112ManifestObserved
                    && generatedLoadOnDemandChunkFileV112RequestObserved
                    && generatedLoadOnDemandChunkFileV112StageObserved
                    && generatedLoadOnDemandChunkFileV112DomObserved;
        }

        public boolean hasRequiredFlowIpcV1_12Types() {
            return hasRequiredFlowIpcV1_11Types()
                    && hasRequiredFlowIpcV1_12LoadOnDemandChunkFileTypes();
        }

        public boolean hasRequiredFlowIpcV1_13ProductionOwnerTypes() {
            return productionRuntimeOwnerV113ManifestObserved
                    && productionRuntimeOwnerV113RequestObserved
                    && productionRuntimeOwnerV113StageObserved
                    && productionRuntimeOwnerV113DomObserved;
        }

        public boolean hasRequiredFlowIpcV1_13Types() {
            return hasRequiredFlowIpcV1_12Types()
                    && hasRequiredFlowIpcV1_13ProductionOwnerTypes();
        }

        public boolean hasRequiredFlowIpcV1_14ProductionOwnerImportTypes() {
            return productionRuntimeOwnerImportV114ManifestObserved
                    && productionRuntimeOwnerImportV114RequestObserved
                    && productionRuntimeOwnerImportV114StageObserved
                    && productionRuntimeOwnerImportV114DomObserved;
        }

        public boolean hasRequiredFlowIpcV1_14Types() {
            return hasRequiredFlowIpcV1_13Types()
                    && hasRequiredFlowIpcV1_14ProductionOwnerImportTypes();
        }

        public boolean hasRequiredFlowIpcV1_15ProductionOwnerDependencyTypes() {
            return productionRuntimeOwnerDependenciesV115ManifestObserved
                    && productionRuntimeOwnerDependenciesV115RequestObserved
                    && productionRuntimeOwnerDependenciesV115StageObserved
                    && productionRuntimeOwnerDependenciesV115DomObserved;
        }

        public boolean hasRequiredFlowIpcV1_15Types() {
            return hasRequiredFlowIpcV1_14Types()
                    && hasRequiredFlowIpcV1_15ProductionOwnerDependencyTypes();
        }

        public boolean hasRequiredFlowIpcV1_16ProductionOwnerOrderingTypes() {
            return productionRuntimeOwnerOrderingV116ManifestObserved
                    && productionRuntimeOwnerOrderingV116RequestObserved
                    && productionRuntimeOwnerOrderingV116StageObserved
                    && productionRuntimeOwnerOrderingV116DomObserved;
        }

        public boolean hasRequiredFlowIpcV1_16Types() {
            return hasRequiredFlowIpcV1_15Types()
                    && hasRequiredFlowIpcV1_16ProductionOwnerOrderingTypes();
        }

        public boolean hasRequiredFlowIpcV1_3FrameworkTypes() {
            return types.keySet().containsAll(List.of(
                    "launch",
                    "ready",
                    "capabilityManifest",
                    "window.create",
                    "bootstrapRequest",
                    "bootstrapResponse",
                    "resourceRequest",
                    "resourceResponse",
                    "event",
                    "patch",
                    "command",
                    "capabilityResult",
                    "webview.firstMeaningfulPaint",
                    "webview.dom",
                    "webview.snapshot",
                    "shutdown"))
                    && flowIpcTransport
                    && compatibilityFalse
                    && frameworkAppLaunchObserved
                    && frameworkAppCapabilityManifestObserved
                    && frameworkAppBootstrapObserved
                    && serverSequenceObserved
                    && uidlWriterObserved
                    && uidlRpcDispatchObserved
                    && flowClientResourceManifestObserved
                    && flowClientResourceRequestObserved
                    && flowClientResourceResponseObserved
                    && flowClientResourceDomObserved
                    && flowClientResourceGraphDomObserved
                    && flowClientResourceExecutionClassificationDomObserved
                    && flowClientResourceLazyLookupRequestObserved
                    && flowClientResourceLazyLookupResponseObserved
                    && flowClientResourceLazyLookupDomObserved
                    && flowClientEngineResourceManifestObserved
                    && flowClientEngineResourceRequestObserved
                    && flowClientEngineResourceResponseObserved
                    && flowClientEngineDomObserved
                    && flowClientEngineGenericViewportDomObserved
                    && firstMeaningfulPaintObserved
                    && componentResolverV13ManifestObserved
                    && componentResolverV13DomObserved
                    && vaadinWebComponentResourceRequestObserved
                    && vaadinWebComponentResourceResponseObserved
                    && vaadinSecondWebComponentResourceRequestObserved
                    && vaadinSecondWebComponentResourceResponseObserved
                    && vaadinSecondWebComponentDevBundleResourceObserved
                    && frameworkAppDomObserved
                    && frameworkAppBudgetEventRpcObserved
                    && frameworkAppCheckboxEventRpcObserved
                    && frameworkAppSelectEventRpcObserved
                    && frameworkAppApplyEventRpcObserved
                    && frameworkAppBudgetPatchObserved
                    && frameworkAppCheckboxPatchObserved
                    && frameworkAppSelectPatchObserved
                    && frameworkAppApplyPatchObserved
                    && frameworkAppWindowTitleCommandObserved
                    && frameworkAppClipboardCommandObserved
                    && frameworkAppNotificationCommandObserved
                    && frameworkAppWindowTitleResultObserved
                    && frameworkAppClipboardResultObserved
                    && frameworkAppNotificationResultObserved
                    && frameworkAppInteractionDomObserved;
        }

        private void print(Path path, boolean printBaseMissingTypes) {
            System.out.println("Trace: " + path);
            System.out.println("Messages: " + total);
            System.out.println("Directions: " + directions);
            System.out.println("Types: " + types);
            System.out.println("transport=flow-ipc: " + flowIpcTransport);
            System.out.println("compatibilityMode=false: " + compatibilityFalse);
            System.out.println("capability manifest observed: " + capabilityManifestObserved);
            System.out.println("capability manifest result observed: " + capabilityManifestResultObserved);
            System.out.println("file dialog result observed: " + fileDialogResultObserved);
            System.out.println("native file dialog result observed: " + nativeFileDialogResultObserved);
            System.out.println("manifest-enforced permission observed: " + manifestEnforcedObserved);
            System.out.println("capability denied observed: " + capabilityDeniedObserved);
            System.out.println("file transfer manifest observed: " + fileTransferManifestObserved);
            System.out.println("file transfer download command redacted observed: "
                    + fileTransferDownloadCommandRedactedObserved);
            System.out.println("file transfer download result observed: " + fileTransferDownloadResultObserved);
            System.out.println("file transfer upload result observed: " + fileTransferUploadResultObserved);
            System.out.println("file transfer DOM observed: " + fileTransferDomObserved);
            System.out.println("server sequence observed: " + serverSequenceObserved);
            System.out.println("Flow IPC server sequence DOM observed: " + flowIpcServerSequenceDomObserved);
            System.out.println("click patch observed: " + clickPatchObserved);
            System.out.println("native clipboard observed: " + nativeClipboardObserved);
            System.out.println("capability permission observed: " + capabilityPermissionObserved);
            System.out.println("WebView error observed: " + webViewErrorObserved);
            System.out.println("UIDL writer observed: " + uidlWriterObserved);
            System.out.println("Flow security key observed: " + securityKeyObserved);
            System.out.println("resync security key observed: " + resyncSecurityKeyObserved);
            System.out.println("bootstrap scope observed: " + bootstrapScopeObserved);
            System.out.println("event scope observed: " + eventScopeObserved);
            System.out.println("resync scope observed: " + resyncScopeObserved);
            System.out.println("Flow client resource manifest observed: " + flowClientResourceManifestObserved);
            System.out.println("Flow client resource request observed: " + flowClientResourceRequestObserved);
            System.out.println("Flow client resource response observed: " + flowClientResourceResponseObserved);
            System.out.println("Flow client resource DOM observed: " + flowClientResourceDomObserved);
            System.out.println("Flow client resource graph DOM observed: " + flowClientResourceGraphDomObserved);
            System.out.println("Flow client resource execution classification DOM observed: "
                    + flowClientResourceExecutionClassificationDomObserved);
            System.out.println("Flow client resource lazy lookup request observed: "
                    + flowClientResourceLazyLookupRequestObserved);
            System.out.println("Flow client resource lazy lookup response observed: "
                    + flowClientResourceLazyLookupResponseObserved);
            System.out.println("Flow client resource lazy lookup DOM observed: "
                    + flowClientResourceLazyLookupDomObserved);
            System.out.println("Flow client engine resource manifest observed: " + flowClientEngineResourceManifestObserved);
            System.out.println("Flow client engine resource request observed: " + flowClientEngineResourceRequestObserved);
            System.out.println("Flow client engine resource response observed: " + flowClientEngineResourceResponseObserved);
            System.out.println("Flow client engine DOM observed: " + flowClientEngineDomObserved);
            System.out.println("Flow client engine bootstrap DOM observed: " + flowClientEngineBootstrapDomObserved);
            System.out.println("Flow client engine list DOM observed: " + flowClientEngineListDomObserved);
            System.out.println("Flow client engine XHR DOM observed: " + flowClientEngineXhrDomObserved);
            System.out.println("Flow client engine visible DOM observed: " + flowClientEngineVisibleDomObserved);
            System.out.println("Flow client engine push DOM observed: " + flowClientEnginePushDomObserved);
            System.out.println("Flow client engine push queue DOM observed: " + flowClientEnginePushQueueDomObserved);
            System.out.println("Flow client engine push ordering DOM observed: " + flowClientEnginePushOrderingDomObserved);
            System.out.println("Flow client engine resync DOM observed: " + flowClientEngineResyncDomObserved);
            System.out.println("Flow client engine generic viewport DOM observed: "
                    + flowClientEngineGenericViewportDomObserved);
            System.out.println("UIDL RPC dispatch observed: " + uidlRpcDispatchObserved);
            System.out.println("WebView UIDL RPC observed: " + webViewUidlRpcObserved);
            System.out.println("Flow client XHR attach observed: " + flowClientXhrAttachObserved);
            System.out.println("Flow client XHR click observed: " + flowClientXhrClickObserved);
            System.out.println("Flow client XHR text input observed: " + flowClientXhrTextInputObserved);
            System.out.println("Flow client XHR checkbox observed: " + flowClientXhrCheckboxObserved);
            System.out.println("Flow client XHR patch observed: " + flowClientXhrPatchObserved);
            System.out.println("Text input patch observed: " + textInputPatchObserved);
            System.out.println("Checkbox patch observed: " + checkboxPatchObserved);
            System.out.println("Flow client component events DOM observed: " + flowClientEngineComponentEventsDomObserved);
            System.out.println("Vaadin Web Component resource manifest observed: "
                    + vaadinWebComponentResourceManifestObserved);
            System.out.println("Vaadin Web Component resource request observed: "
                    + vaadinWebComponentResourceRequestObserved);
            System.out.println("Vaadin Web Component resource response observed: "
                    + vaadinWebComponentResourceResponseObserved);
            System.out.println("Vaadin Web Component resource DOM observed: "
                    + vaadinWebComponentResourceDomObserved);
            System.out.println("Vaadin Web Component resource executed DOM observed: "
                    + vaadinWebComponentResourceExecutedDomObserved);
            System.out.println("Vaadin Web Component failure classification observed: "
                    + vaadinWebComponentFailureClassificationObserved);
            System.out.println("Vaadin Web Component event RPC observed: "
                    + vaadinWebComponentEventRpcObserved);
            System.out.println("Vaadin Web Component patch observed: "
                    + vaadinWebComponentPatchObserved);
            System.out.println("Vaadin Web Component DOM observed: "
                    + vaadinWebComponentDomObserved);
            System.out.println("Vaadin TextField event RPC observed: " + vaadinTextFieldEventRpcObserved);
            System.out.println("Vaadin TextField patch observed: " + vaadinTextFieldPatchObserved);
            System.out.println("Vaadin TextField DOM observed: " + vaadinTextFieldDomObserved);
            System.out.println("Flow IPC v1.3 component resolver manifest observed: "
                    + componentResolverV13ManifestObserved);
            System.out.println("Flow IPC v1.3 component resolver DOM observed: "
                    + componentResolverV13DomObserved);
            System.out.println("Second Vaadin Web Component resource request observed: "
                    + vaadinSecondWebComponentResourceRequestObserved);
            System.out.println("Second Vaadin Web Component resource response observed: "
                    + vaadinSecondWebComponentResourceResponseObserved);
            System.out.println("Second Vaadin Web Component dev-bundle resource observed: "
                    + vaadinSecondWebComponentDevBundleResourceObserved);
            System.out.println("Second Vaadin Web Component event RPC observed: "
                    + vaadinSecondWebComponentEventRpcObserved);
            System.out.println("Second Vaadin Web Component patch observed: "
                    + vaadinSecondWebComponentPatchObserved);
            System.out.println("Second Vaadin Web Component DOM observed: "
                    + vaadinSecondWebComponentDomObserved);
            System.out.println("UIDL request handler patch observed: " + uidlPatchObserved);
            System.out.println("server push UIDL observed: " + pushUidlObserved);
            System.out.println("multi-push UIDL observed: " + multiPushUidlObserved);
            System.out.println("duplicate/late push probe observed: " + duplicateLatePushProbeObserved);
            System.out.println("recovery resync request observed: " + recoveryResyncRequestObserved);
            System.out.println("resync UIDL observed: " + resyncUidlObserved);
            System.out.println("framework app launch observed: " + frameworkAppLaunchObserved);
            System.out.println("framework app capability manifest observed: "
                    + frameworkAppCapabilityManifestObserved);
            System.out.println("framework app bootstrap observed: " + frameworkAppBootstrapObserved);
            System.out.println("framework app budget event RPC observed: "
                    + frameworkAppBudgetEventRpcObserved);
            System.out.println("framework app checkbox event RPC observed: "
                    + frameworkAppCheckboxEventRpcObserved);
            System.out.println("framework app select event RPC observed: "
                    + frameworkAppSelectEventRpcObserved);
            System.out.println("framework app apply event RPC observed: "
                    + frameworkAppApplyEventRpcObserved);
            System.out.println("framework app budget patch observed: " + frameworkAppBudgetPatchObserved);
            System.out.println("framework app checkbox patch observed: " + frameworkAppCheckboxPatchObserved);
            System.out.println("framework app select patch observed: " + frameworkAppSelectPatchObserved);
            System.out.println("framework app apply patch observed: " + frameworkAppApplyPatchObserved);
            System.out.println("framework app window title command/result observed: "
                    + frameworkAppWindowTitleCommandObserved + "/" + frameworkAppWindowTitleResultObserved);
            System.out.println("framework app clipboard command/result observed: "
                    + frameworkAppClipboardCommandObserved + "/" + frameworkAppClipboardResultObserved);
            System.out.println("framework app notification command/result observed: "
                    + frameworkAppNotificationCommandObserved + "/" + frameworkAppNotificationResultObserved);
            System.out.println("first meaningful paint observed: " + firstMeaningfulPaintObserved);
            System.out.println("framework app DOM observed: " + frameworkAppDomObserved);
            System.out.println("framework app interaction DOM observed: " + frameworkAppInteractionDomObserved);
            System.out.println("Flow IPC v1.4 frontend asset pipeline observed: "
                    + frontendAssetPipelineV14Observed);
            System.out.println("Flow IPC v1.4 frontend artifact families observed: "
                    + frontendArtifactFamilyModelObserved);
            System.out.println("Flow IPC v1.4 frontend dependency graph observed: "
                    + frontendDependencyGraphObserved);
            System.out.println("Flow IPC v1.4 frontend shim blocker observed: "
                    + frontendShimBlockerObserved);
            System.out.println("Flow IPC v1.4 manifest import rewrite observed: "
                    + frontendManifestImportRewriteObserved);
            System.out.println("Flow IPC v1.4 TextField dev-bundle blocker observed: "
                    + frontendTextFieldDevBundleBlockerObserved);
            System.out.println("Flow IPC v1.4 production bundle partial graph observed: "
                    + frontendProductionBundlePartialObserved);
            System.out.println("Flow IPC v1.4 production bundle stats resource observed: "
                    + frontendProductionBundleStatsResourceObserved);
            System.out.println("Flow IPC v1.4 theme/static strict blocker observed: "
                    + frontendThemeStaticStrictBlockerObserved);
            System.out.println("Flow IPC v1.4 theme/static resource observed: "
                    + frontendThemeStaticResourceObserved);
            System.out.println("Flow IPC v1.4 dev-mode strict blocker observed: "
                    + frontendDevModeStrictBlockerObserved);
            System.out.println("Flow IPC v1.4 dev-mode resource observed: "
                    + frontendDevModeResourceObserved);
            System.out.println("Flow IPC v1.4 generated/lazy strict blocker observed: "
                    + frontendGeneratedLazyStrictBlockerObserved);
            System.out.println("Flow IPC v1.4 generated/lazy resource observed: "
                    + frontendGeneratedLazyResourceObserved);
            System.out.println("Flow IPC v1.4 generated/lazy dev-bundle chunk observed: "
                    + frontendGeneratedLazyBundleChunkResourceObserved);
            System.out.println("Flow IPC v1.5 resource staging observed: "
                    + frontendResourceStageObserved);
            System.out.println("Flow IPC v1.5 staging DOM observed: "
                    + frontendStagingDomObserved);
            System.out.println("Flow IPC v1.5 staged execution/strict blocker observed: "
                    + frontendResourceStageExecutedObserved + "/" + frontendResourceStageStrictBlockerObserved);
            System.out.println("Flow IPC v1.5 staged failure classification observed: "
                    + frontendResourceStageFailureClassificationObserved);
            System.out.println("Flow IPC v1.5 TextField staged execution/blocker observed: "
                    + frontendTextFieldStagedExecutionObserved + "/" + frontendTextFieldStagedBlockerObserved);
            System.out.println("Flow IPC v1.5 generated/lazy execution attempt/resolution observed: "
                    + frontendGeneratedLazyExecutionAttemptObserved + "/"
                    + frontendGeneratedLazyExecutionResolvedObserved);
            System.out.println("Flow IPC v1.5 loadOnDemand boundary observed: "
                    + frontendLoadOnDemandBoundaryObserved);
            System.out.println("Flow IPC v1.8 generated/lazy manifest observed: "
                    + frontendGeneratedLazyV18ManifestObserved);
            System.out.println("Flow IPC v1.8 generated/lazy request observed: "
                    + frontendGeneratedLazyV18RequestObserved);
            System.out.println("Flow IPC v1.8 generated/lazy dependency rewrite observed: "
                    + frontendGeneratedLazyV18DependencyRewriteObserved);
            System.out.println("Flow IPC v1.8 generated/lazy execution/narrowed blocker observed: "
                    + frontendGeneratedLazyV18ExecutionOrNarrowedBlockerObserved);
            System.out.println("Flow IPC v1.8 generated/lazy DOM observed: "
                    + frontendGeneratedLazyV18DomObserved);
            System.out.println("Flow IPC v1.9 loadOnDemand manifest observed: "
                    + frontendGeneratedLazyV19ManifestObserved);
            System.out.println("Flow IPC v1.9 loadOnDemand request observed: "
                    + frontendGeneratedLazyV19RequestObserved);
            System.out.println("Flow IPC v1.9 loadOnDemand stage observed: "
                    + frontendGeneratedLazyV19StageObserved);
            System.out.println("Flow IPC v1.9 loadOnDemand DOM observed: "
                    + frontendGeneratedLazyV19DomObserved);
            System.out.println("Flow IPC v1.10 runtime resolver manifest observed: "
                    + frontendRuntimeResolverV110ManifestObserved);
            System.out.println("Flow IPC v1.10 lazy dependency manifest observed: "
                    + flowLazyDependencyV110ManifestObserved);
            System.out.println("Flow IPC v1.10 lazy dependency request observed: "
                    + flowLazyDependencyV110RequestObserved);
            System.out.println("Flow IPC v1.10 lazy dependency stage observed: "
                    + flowLazyDependencyV110StageObserved);
            System.out.println("Flow IPC v1.10 lazy dependency DOM observed: "
                    + flowLazyDependencyV110DomObserved);
            System.out.println("Flow IPC v1.10 production runtime manifest observed: "
                    + productionRuntimeV110ManifestObserved);
            System.out.println("Flow IPC v1.10 production runtime request observed: "
                    + productionRuntimeV110RequestObserved);
            System.out.println("Flow IPC v1.10 production runtime stage observed: "
                    + productionRuntimeV110StageObserved);
            System.out.println("Flow IPC v1.10 production runtime DOM observed: "
                    + productionRuntimeV110DomObserved);
            System.out.println("Flow IPC v1.11 loadOnDemand chunk manifest observed: "
                    + generatedLoadOnDemandChunkV111ManifestObserved);
            System.out.println("Flow IPC v1.11 loadOnDemand UIDL expression observed: "
                    + generatedLoadOnDemandChunkV111UidlExpressionObserved);
            System.out.println("Flow IPC v1.11 loadOnDemand chunk request observed: "
                    + generatedLoadOnDemandChunkV111RequestObserved);
            System.out.println("Flow IPC v1.11 loadOnDemand chunk stage observed: "
                    + generatedLoadOnDemandChunkV111StageObserved);
            System.out.println("Flow IPC v1.11 loadOnDemand chunk DOM observed: "
                    + generatedLoadOnDemandChunkV111DomObserved);
            System.out.println("Flow IPC v1.12 loadOnDemand chunk-file manifest observed: "
                    + generatedLoadOnDemandChunkFileV112ManifestObserved);
            System.out.println("Flow IPC v1.12 loadOnDemand chunk-file request observed: "
                    + generatedLoadOnDemandChunkFileV112RequestObserved);
            System.out.println("Flow IPC v1.12 loadOnDemand chunk-file stage observed: "
                    + generatedLoadOnDemandChunkFileV112StageObserved);
            System.out.println("Flow IPC v1.12 loadOnDemand chunk-file DOM observed: "
                    + generatedLoadOnDemandChunkFileV112DomObserved);
            System.out.println("Flow IPC v1.13 production owner manifest observed: "
                    + productionRuntimeOwnerV113ManifestObserved);
            System.out.println("Flow IPC v1.13 production owner request observed: "
                    + productionRuntimeOwnerV113RequestObserved);
            System.out.println("Flow IPC v1.13 production owner stage observed: "
                    + productionRuntimeOwnerV113StageObserved);
            System.out.println("Flow IPC v1.13 production owner DOM observed: "
                    + productionRuntimeOwnerV113DomObserved);
            System.out.println("Flow IPC v1.14 production owner import manifest observed: "
                    + productionRuntimeOwnerImportV114ManifestObserved);
            System.out.println("Flow IPC v1.14 production owner import request observed: "
                    + productionRuntimeOwnerImportV114RequestObserved);
            System.out.println("Flow IPC v1.14 production owner import stage observed: "
                    + productionRuntimeOwnerImportV114StageObserved);
            System.out.println("Flow IPC v1.14 production owner import DOM observed: "
                    + productionRuntimeOwnerImportV114DomObserved);
            System.out.println("Flow IPC v1.15 production owner dependency manifest observed: "
                    + productionRuntimeOwnerDependenciesV115ManifestObserved);
            System.out.println("Flow IPC v1.15 production owner dependency request observed: "
                    + productionRuntimeOwnerDependenciesV115RequestObserved);
            System.out.println("Flow IPC v1.15 production owner dependency stage observed: "
                    + productionRuntimeOwnerDependenciesV115StageObserved);
            System.out.println("Flow IPC v1.15 production owner dependency DOM observed: "
                    + productionRuntimeOwnerDependenciesV115DomObserved);
            System.out.println("Flow IPC v1.16 production owner ordering manifest observed: "
                    + productionRuntimeOwnerOrderingV116ManifestObserved);
            System.out.println("Flow IPC v1.16 production owner ordering request observed: "
                    + productionRuntimeOwnerOrderingV116RequestObserved);
            System.out.println("Flow IPC v1.16 production owner ordering stage observed: "
                    + productionRuntimeOwnerOrderingV116StageObserved);
            System.out.println("Flow IPC v1.16 production owner ordering DOM observed: "
                    + productionRuntimeOwnerOrderingV116DomObserved);
            System.out.println("Flow IPC v1.6 TextField graph manifest observed: "
                    + frontendTextFieldV16ManifestObserved);
            System.out.println("Flow IPC v1.6 TextField graph request observed: "
                    + frontendTextFieldV16RequestObserved);
            System.out.println("Flow IPC v1.6 TextField narrowed staged blocker observed: "
                    + frontendTextFieldV16StageBlockerObserved);
            System.out.println("Flow IPC v1.6 TextField dependency classification observed: "
                    + frontendTextFieldV16DependencyClassificationObserved);
            System.out.println("Flow IPC v1.6 TextField DOM diagnostics observed: "
                    + frontendTextFieldV16DomObserved);
            System.out.println("Flow IPC v1.7 TextField side-effect manifest observed: "
                    + frontendTextFieldV17ManifestObserved);
            System.out.println("Flow IPC v1.7 TextField side-effect request observed: "
                    + frontendTextFieldV17RequestObserved);
            System.out.println("Flow IPC v1.7 TextField staged execution observed: "
                    + frontendTextFieldV17StageExecutedObserved);
            System.out.println("Flow IPC v1.7 TextField module identity dependency observed: "
                    + frontendTextFieldV17ModuleIdentityDependencyObserved);
            System.out.println("Flow IPC v1.7 TextField DOM diagnostics observed: "
                    + frontendTextFieldV17DomObserved);
            List<String> missing = FLOW_IPC_REQUIRED_TYPES.stream()
                    .filter(type -> !types.containsKey(type))
                    .toList();
            if (printBaseMissingTypes && !missing.isEmpty()) {
                System.out.println("Missing required types: " + missing);
            }
        }
    }
}
