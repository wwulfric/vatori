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
        FLOW_IPC_V1_17_RESOURCE_SCHEME("--require-flow-ipc-v1_17-resource-scheme"),
        FLOW_IPC_V1_17("--require-flow-ipc-v1_17"),
        FLOW_IPC_V1_18_PRODUCTION_OWNER_RESOURCE_SCHEME(
                "--require-flow-ipc-v1_18-production-owner-resource-scheme"),
        FLOW_IPC_V1_18("--require-flow-ipc-v1_18"),
        FLOW_IPC_V1_19_PRODUCTION_OWNER_SIDE_EFFECT(
                "--require-flow-ipc-v1_19-production-owner-side-effect"),
        FLOW_IPC_V1_19("--require-flow-ipc-v1_19"),
        FLOW_IPC_V1_20_PRODUCTION_OWNER_OWNED_RUNTIME(
                "--require-flow-ipc-v1_20-production-owner-owned-runtime"),
        FLOW_IPC_V1_20("--require-flow-ipc-v1_20"),
        FLOW_IPC_V1_21_FRONTEND_RESOLVER_CONTRACT(
                "--require-flow-ipc-v1_21-frontend-resolver-contract"),
        FLOW_IPC_V1_21("--require-flow-ipc-v1_21"),
        FLOW_IPC_V1_22_GENERATED_CHUNK_FIXTURE(
                "--require-flow-ipc-v1_22-generated-chunk-fixture"),
        FLOW_IPC_V1_22("--require-flow-ipc-v1_22"),
        FLOW_IPC_V1_23_PRODUCTION_OWNER_POSITIVE_FIXTURE(
                "--require-flow-ipc-v1_23-production-owner-positive-fixture"),
        FLOW_IPC_V1_23("--require-flow-ipc-v1_23"),
        FLOW_IPC_V1_24_FRONTEND_PIPELINE_CLOSURE(
                "--require-flow-ipc-v1_24-frontend-pipeline-closure"),
        FLOW_IPC_V1_24("--require-flow-ipc-v1_24"),
        FLOW_IPC_V1_25_PACKAGED_FRONTEND_BASELINE(
                "--require-flow-ipc-v1_25-packaged-frontend-baseline"),
        FLOW_IPC_V1_25("--require-flow-ipc-v1_25"),
        FLOW_IPC_V1_26_GENERATED_LAZY_SOURCE_BOUNDARY(
                "--require-flow-ipc-v1_26-generated-lazy-source-boundary"),
        FLOW_IPC_V1_26("--require-flow-ipc-v1_26"),
        FLOW_IPC_V1_27_GENERATED_LAZY_REAL_BUILD_PROBE(
                "--require-flow-ipc-v1_27-generated-lazy-real-build-probe"),
        FLOW_IPC_V1_27("--require-flow-ipc-v1_27"),
        FLOW_IPC_V1_28_GENERATED_LAZY_BUILD_LIFECYCLE(
                "--require-flow-ipc-v1_28-generated-lazy-build-lifecycle"),
        FLOW_IPC_V1_28("--require-flow-ipc-v1_28"),
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
                case FLOW_IPC_V1_17_RESOURCE_SCHEME -> EnumSet.of(FLOW_IPC_V1_17_RESOURCE_SCHEME);
                case FLOW_IPC_V1_17 -> EnumSet.of(FLOW_IPC_V1, FLOW_IPC_V1_1, FLOW_IPC_V1_2,
                        FLOW_IPC_V1_3, FLOW_IPC_V1_4, FLOW_IPC_V1_5, FLOW_IPC_V1_7,
                        FLOW_IPC_V1_8, FLOW_IPC_V1_9_LOADONDEMAND, FLOW_IPC_V1_9,
                        FLOW_IPC_V1_10_LAZY_DEPENDENCY, FLOW_IPC_V1_10_PRODUCTION_RUNTIME,
                        FLOW_IPC_V1_10, FLOW_IPC_V1_11_LOADONDEMAND_CHUNK, FLOW_IPC_V1_11,
                        FLOW_IPC_V1_12_LOADONDEMAND_CHUNK_FILE, FLOW_IPC_V1_12,
                        FLOW_IPC_V1_13_PRODUCTION_OWNER, FLOW_IPC_V1_13,
                        FLOW_IPC_V1_14_PRODUCTION_OWNER_IMPORT, FLOW_IPC_V1_14,
                        FLOW_IPC_V1_15_PRODUCTION_OWNER_DEPENDENCIES, FLOW_IPC_V1_15,
                        FLOW_IPC_V1_16_PRODUCTION_OWNER_ORDERING, FLOW_IPC_V1_16,
                        FLOW_IPC_V1_17_RESOURCE_SCHEME, FLOW_IPC_V1_17);
                case FLOW_IPC_V1_18_PRODUCTION_OWNER_RESOURCE_SCHEME ->
                    EnumSet.of(FLOW_IPC_V1_18_PRODUCTION_OWNER_RESOURCE_SCHEME);
                case FLOW_IPC_V1_18 -> EnumSet.of(FLOW_IPC_V1, FLOW_IPC_V1_1, FLOW_IPC_V1_2,
                        FLOW_IPC_V1_3, FLOW_IPC_V1_4, FLOW_IPC_V1_5, FLOW_IPC_V1_7,
                        FLOW_IPC_V1_8, FLOW_IPC_V1_9_LOADONDEMAND, FLOW_IPC_V1_9,
                        FLOW_IPC_V1_10_LAZY_DEPENDENCY, FLOW_IPC_V1_10_PRODUCTION_RUNTIME,
                        FLOW_IPC_V1_10, FLOW_IPC_V1_11_LOADONDEMAND_CHUNK, FLOW_IPC_V1_11,
                        FLOW_IPC_V1_12_LOADONDEMAND_CHUNK_FILE, FLOW_IPC_V1_12,
                        FLOW_IPC_V1_13_PRODUCTION_OWNER, FLOW_IPC_V1_13,
                        FLOW_IPC_V1_14_PRODUCTION_OWNER_IMPORT, FLOW_IPC_V1_14,
                        FLOW_IPC_V1_15_PRODUCTION_OWNER_DEPENDENCIES, FLOW_IPC_V1_15,
                        FLOW_IPC_V1_16_PRODUCTION_OWNER_ORDERING, FLOW_IPC_V1_16,
                        FLOW_IPC_V1_17_RESOURCE_SCHEME, FLOW_IPC_V1_17,
                        FLOW_IPC_V1_18_PRODUCTION_OWNER_RESOURCE_SCHEME, FLOW_IPC_V1_18);
                case FLOW_IPC_V1_19_PRODUCTION_OWNER_SIDE_EFFECT ->
                    EnumSet.of(FLOW_IPC_V1_19_PRODUCTION_OWNER_SIDE_EFFECT);
                case FLOW_IPC_V1_19 -> EnumSet.of(FLOW_IPC_V1, FLOW_IPC_V1_1, FLOW_IPC_V1_2,
                        FLOW_IPC_V1_3, FLOW_IPC_V1_4, FLOW_IPC_V1_5, FLOW_IPC_V1_7,
                        FLOW_IPC_V1_8, FLOW_IPC_V1_9_LOADONDEMAND, FLOW_IPC_V1_9,
                        FLOW_IPC_V1_10_LAZY_DEPENDENCY, FLOW_IPC_V1_10_PRODUCTION_RUNTIME,
                        FLOW_IPC_V1_10, FLOW_IPC_V1_11_LOADONDEMAND_CHUNK, FLOW_IPC_V1_11,
                        FLOW_IPC_V1_12_LOADONDEMAND_CHUNK_FILE, FLOW_IPC_V1_12,
                        FLOW_IPC_V1_13_PRODUCTION_OWNER, FLOW_IPC_V1_13,
                        FLOW_IPC_V1_14_PRODUCTION_OWNER_IMPORT, FLOW_IPC_V1_14,
                        FLOW_IPC_V1_15_PRODUCTION_OWNER_DEPENDENCIES, FLOW_IPC_V1_15,
                        FLOW_IPC_V1_16_PRODUCTION_OWNER_ORDERING, FLOW_IPC_V1_16,
                        FLOW_IPC_V1_17_RESOURCE_SCHEME, FLOW_IPC_V1_17,
                        FLOW_IPC_V1_18_PRODUCTION_OWNER_RESOURCE_SCHEME, FLOW_IPC_V1_18,
                        FLOW_IPC_V1_19_PRODUCTION_OWNER_SIDE_EFFECT, FLOW_IPC_V1_19);
                case FLOW_IPC_V1_20_PRODUCTION_OWNER_OWNED_RUNTIME ->
                    EnumSet.of(FLOW_IPC_V1_20_PRODUCTION_OWNER_OWNED_RUNTIME);
                case FLOW_IPC_V1_20 -> EnumSet.of(FLOW_IPC_V1, FLOW_IPC_V1_1, FLOW_IPC_V1_2,
                        FLOW_IPC_V1_3, FLOW_IPC_V1_4, FLOW_IPC_V1_5, FLOW_IPC_V1_7,
                        FLOW_IPC_V1_8, FLOW_IPC_V1_9_LOADONDEMAND, FLOW_IPC_V1_9,
                        FLOW_IPC_V1_10_LAZY_DEPENDENCY, FLOW_IPC_V1_10_PRODUCTION_RUNTIME,
                        FLOW_IPC_V1_10, FLOW_IPC_V1_11_LOADONDEMAND_CHUNK, FLOW_IPC_V1_11,
                        FLOW_IPC_V1_12_LOADONDEMAND_CHUNK_FILE, FLOW_IPC_V1_12,
                        FLOW_IPC_V1_13_PRODUCTION_OWNER, FLOW_IPC_V1_13,
                        FLOW_IPC_V1_14_PRODUCTION_OWNER_IMPORT, FLOW_IPC_V1_14,
                        FLOW_IPC_V1_15_PRODUCTION_OWNER_DEPENDENCIES, FLOW_IPC_V1_15,
                        FLOW_IPC_V1_16_PRODUCTION_OWNER_ORDERING, FLOW_IPC_V1_16,
                        FLOW_IPC_V1_17_RESOURCE_SCHEME, FLOW_IPC_V1_17,
                        FLOW_IPC_V1_18_PRODUCTION_OWNER_RESOURCE_SCHEME, FLOW_IPC_V1_18,
                        FLOW_IPC_V1_19_PRODUCTION_OWNER_SIDE_EFFECT, FLOW_IPC_V1_19,
                        FLOW_IPC_V1_20_PRODUCTION_OWNER_OWNED_RUNTIME, FLOW_IPC_V1_20);
                case FLOW_IPC_V1_21_FRONTEND_RESOLVER_CONTRACT ->
                    EnumSet.of(FLOW_IPC_V1_21_FRONTEND_RESOLVER_CONTRACT);
                case FLOW_IPC_V1_21 -> EnumSet.of(FLOW_IPC_V1, FLOW_IPC_V1_1, FLOW_IPC_V1_2,
                        FLOW_IPC_V1_3, FLOW_IPC_V1_4, FLOW_IPC_V1_5, FLOW_IPC_V1_7,
                        FLOW_IPC_V1_8, FLOW_IPC_V1_9_LOADONDEMAND, FLOW_IPC_V1_9,
                        FLOW_IPC_V1_10_LAZY_DEPENDENCY, FLOW_IPC_V1_10_PRODUCTION_RUNTIME,
                        FLOW_IPC_V1_10, FLOW_IPC_V1_11_LOADONDEMAND_CHUNK, FLOW_IPC_V1_11,
                        FLOW_IPC_V1_12_LOADONDEMAND_CHUNK_FILE, FLOW_IPC_V1_12,
                        FLOW_IPC_V1_13_PRODUCTION_OWNER, FLOW_IPC_V1_13,
                        FLOW_IPC_V1_14_PRODUCTION_OWNER_IMPORT, FLOW_IPC_V1_14,
                        FLOW_IPC_V1_15_PRODUCTION_OWNER_DEPENDENCIES, FLOW_IPC_V1_15,
                        FLOW_IPC_V1_16_PRODUCTION_OWNER_ORDERING, FLOW_IPC_V1_16,
                        FLOW_IPC_V1_17_RESOURCE_SCHEME, FLOW_IPC_V1_17,
                        FLOW_IPC_V1_18_PRODUCTION_OWNER_RESOURCE_SCHEME, FLOW_IPC_V1_18,
                        FLOW_IPC_V1_19_PRODUCTION_OWNER_SIDE_EFFECT, FLOW_IPC_V1_19,
                        FLOW_IPC_V1_20_PRODUCTION_OWNER_OWNED_RUNTIME, FLOW_IPC_V1_20,
                        FLOW_IPC_V1_21_FRONTEND_RESOLVER_CONTRACT, FLOW_IPC_V1_21);
                case FLOW_IPC_V1_22_GENERATED_CHUNK_FIXTURE ->
                    EnumSet.of(FLOW_IPC_V1_22_GENERATED_CHUNK_FIXTURE);
                case FLOW_IPC_V1_22 -> EnumSet.of(FLOW_IPC_V1, FLOW_IPC_V1_1, FLOW_IPC_V1_2,
                        FLOW_IPC_V1_3, FLOW_IPC_V1_4, FLOW_IPC_V1_5, FLOW_IPC_V1_7,
                        FLOW_IPC_V1_8, FLOW_IPC_V1_9_LOADONDEMAND, FLOW_IPC_V1_9,
                        FLOW_IPC_V1_10_LAZY_DEPENDENCY, FLOW_IPC_V1_10_PRODUCTION_RUNTIME,
                        FLOW_IPC_V1_10, FLOW_IPC_V1_11_LOADONDEMAND_CHUNK, FLOW_IPC_V1_11,
                        FLOW_IPC_V1_12_LOADONDEMAND_CHUNK_FILE, FLOW_IPC_V1_12,
                        FLOW_IPC_V1_13_PRODUCTION_OWNER, FLOW_IPC_V1_13,
                        FLOW_IPC_V1_14_PRODUCTION_OWNER_IMPORT, FLOW_IPC_V1_14,
                        FLOW_IPC_V1_15_PRODUCTION_OWNER_DEPENDENCIES, FLOW_IPC_V1_15,
                        FLOW_IPC_V1_16_PRODUCTION_OWNER_ORDERING, FLOW_IPC_V1_16,
                        FLOW_IPC_V1_17_RESOURCE_SCHEME, FLOW_IPC_V1_17,
                        FLOW_IPC_V1_18_PRODUCTION_OWNER_RESOURCE_SCHEME, FLOW_IPC_V1_18,
                        FLOW_IPC_V1_19_PRODUCTION_OWNER_SIDE_EFFECT, FLOW_IPC_V1_19,
                        FLOW_IPC_V1_20_PRODUCTION_OWNER_OWNED_RUNTIME, FLOW_IPC_V1_20,
                        FLOW_IPC_V1_21_FRONTEND_RESOLVER_CONTRACT, FLOW_IPC_V1_21,
                        FLOW_IPC_V1_22_GENERATED_CHUNK_FIXTURE, FLOW_IPC_V1_22);
                case FLOW_IPC_V1_23_PRODUCTION_OWNER_POSITIVE_FIXTURE ->
                    EnumSet.of(FLOW_IPC_V1_23_PRODUCTION_OWNER_POSITIVE_FIXTURE);
                case FLOW_IPC_V1_23 -> EnumSet.of(FLOW_IPC_V1, FLOW_IPC_V1_1, FLOW_IPC_V1_2,
                        FLOW_IPC_V1_3, FLOW_IPC_V1_4, FLOW_IPC_V1_5, FLOW_IPC_V1_7,
                        FLOW_IPC_V1_8, FLOW_IPC_V1_9_LOADONDEMAND, FLOW_IPC_V1_9,
                        FLOW_IPC_V1_10_LAZY_DEPENDENCY, FLOW_IPC_V1_10_PRODUCTION_RUNTIME,
                        FLOW_IPC_V1_10, FLOW_IPC_V1_11_LOADONDEMAND_CHUNK, FLOW_IPC_V1_11,
                        FLOW_IPC_V1_12_LOADONDEMAND_CHUNK_FILE, FLOW_IPC_V1_12,
                        FLOW_IPC_V1_13_PRODUCTION_OWNER, FLOW_IPC_V1_13,
                        FLOW_IPC_V1_14_PRODUCTION_OWNER_IMPORT, FLOW_IPC_V1_14,
                        FLOW_IPC_V1_15_PRODUCTION_OWNER_DEPENDENCIES, FLOW_IPC_V1_15,
                        FLOW_IPC_V1_16_PRODUCTION_OWNER_ORDERING, FLOW_IPC_V1_16,
                        FLOW_IPC_V1_17_RESOURCE_SCHEME, FLOW_IPC_V1_17,
                        FLOW_IPC_V1_18_PRODUCTION_OWNER_RESOURCE_SCHEME, FLOW_IPC_V1_18,
                        FLOW_IPC_V1_19_PRODUCTION_OWNER_SIDE_EFFECT, FLOW_IPC_V1_19,
                        FLOW_IPC_V1_20_PRODUCTION_OWNER_OWNED_RUNTIME, FLOW_IPC_V1_20,
                        FLOW_IPC_V1_21_FRONTEND_RESOLVER_CONTRACT, FLOW_IPC_V1_21,
                        FLOW_IPC_V1_22_GENERATED_CHUNK_FIXTURE, FLOW_IPC_V1_22,
                        FLOW_IPC_V1_23_PRODUCTION_OWNER_POSITIVE_FIXTURE, FLOW_IPC_V1_23);
                case FLOW_IPC_V1_24_FRONTEND_PIPELINE_CLOSURE ->
                    EnumSet.of(FLOW_IPC_V1_24_FRONTEND_PIPELINE_CLOSURE);
                case FLOW_IPC_V1_24 -> EnumSet.of(FLOW_IPC_V1, FLOW_IPC_V1_1, FLOW_IPC_V1_2,
                        FLOW_IPC_V1_3, FLOW_IPC_V1_4, FLOW_IPC_V1_5, FLOW_IPC_V1_7,
                        FLOW_IPC_V1_8, FLOW_IPC_V1_9_LOADONDEMAND, FLOW_IPC_V1_9,
                        FLOW_IPC_V1_10_LAZY_DEPENDENCY, FLOW_IPC_V1_10_PRODUCTION_RUNTIME,
                        FLOW_IPC_V1_10, FLOW_IPC_V1_11_LOADONDEMAND_CHUNK, FLOW_IPC_V1_11,
                        FLOW_IPC_V1_12_LOADONDEMAND_CHUNK_FILE, FLOW_IPC_V1_12,
                        FLOW_IPC_V1_13_PRODUCTION_OWNER, FLOW_IPC_V1_13,
                        FLOW_IPC_V1_14_PRODUCTION_OWNER_IMPORT, FLOW_IPC_V1_14,
                        FLOW_IPC_V1_15_PRODUCTION_OWNER_DEPENDENCIES, FLOW_IPC_V1_15,
                        FLOW_IPC_V1_16_PRODUCTION_OWNER_ORDERING, FLOW_IPC_V1_16,
                        FLOW_IPC_V1_17_RESOURCE_SCHEME, FLOW_IPC_V1_17,
                        FLOW_IPC_V1_18_PRODUCTION_OWNER_RESOURCE_SCHEME, FLOW_IPC_V1_18,
                        FLOW_IPC_V1_19_PRODUCTION_OWNER_SIDE_EFFECT, FLOW_IPC_V1_19,
                        FLOW_IPC_V1_20_PRODUCTION_OWNER_OWNED_RUNTIME, FLOW_IPC_V1_20,
                        FLOW_IPC_V1_21_FRONTEND_RESOLVER_CONTRACT, FLOW_IPC_V1_21,
                        FLOW_IPC_V1_22_GENERATED_CHUNK_FIXTURE, FLOW_IPC_V1_22,
                        FLOW_IPC_V1_23_PRODUCTION_OWNER_POSITIVE_FIXTURE, FLOW_IPC_V1_23,
                        FLOW_IPC_V1_24_FRONTEND_PIPELINE_CLOSURE, FLOW_IPC_V1_24);
                case FLOW_IPC_V1_25_PACKAGED_FRONTEND_BASELINE ->
                    EnumSet.of(FLOW_IPC_V1_25_PACKAGED_FRONTEND_BASELINE);
                case FLOW_IPC_V1_25 -> EnumSet.of(FLOW_IPC_V1, FLOW_IPC_V1_1, FLOW_IPC_V1_2,
                        FLOW_IPC_V1_3, FLOW_IPC_V1_4, FLOW_IPC_V1_5, FLOW_IPC_V1_7,
                        FLOW_IPC_V1_8, FLOW_IPC_V1_9_LOADONDEMAND, FLOW_IPC_V1_9,
                        FLOW_IPC_V1_10_LAZY_DEPENDENCY, FLOW_IPC_V1_10_PRODUCTION_RUNTIME,
                        FLOW_IPC_V1_10, FLOW_IPC_V1_11_LOADONDEMAND_CHUNK, FLOW_IPC_V1_11,
                        FLOW_IPC_V1_12_LOADONDEMAND_CHUNK_FILE, FLOW_IPC_V1_12,
                        FLOW_IPC_V1_13_PRODUCTION_OWNER, FLOW_IPC_V1_13,
                        FLOW_IPC_V1_14_PRODUCTION_OWNER_IMPORT, FLOW_IPC_V1_14,
                        FLOW_IPC_V1_15_PRODUCTION_OWNER_DEPENDENCIES, FLOW_IPC_V1_15,
                        FLOW_IPC_V1_16_PRODUCTION_OWNER_ORDERING, FLOW_IPC_V1_16,
                        FLOW_IPC_V1_17_RESOURCE_SCHEME, FLOW_IPC_V1_17,
                        FLOW_IPC_V1_18_PRODUCTION_OWNER_RESOURCE_SCHEME, FLOW_IPC_V1_18,
                        FLOW_IPC_V1_19_PRODUCTION_OWNER_SIDE_EFFECT, FLOW_IPC_V1_19,
                        FLOW_IPC_V1_20_PRODUCTION_OWNER_OWNED_RUNTIME, FLOW_IPC_V1_20,
                        FLOW_IPC_V1_21_FRONTEND_RESOLVER_CONTRACT, FLOW_IPC_V1_21,
                        FLOW_IPC_V1_22_GENERATED_CHUNK_FIXTURE, FLOW_IPC_V1_22,
                        FLOW_IPC_V1_23_PRODUCTION_OWNER_POSITIVE_FIXTURE, FLOW_IPC_V1_23,
                        FLOW_IPC_V1_24_FRONTEND_PIPELINE_CLOSURE, FLOW_IPC_V1_24,
                        FLOW_IPC_V1_25_PACKAGED_FRONTEND_BASELINE, FLOW_IPC_V1_25);
                case FLOW_IPC_V1_26_GENERATED_LAZY_SOURCE_BOUNDARY ->
                    EnumSet.of(FLOW_IPC_V1_26_GENERATED_LAZY_SOURCE_BOUNDARY);
                case FLOW_IPC_V1_26 -> EnumSet.of(FLOW_IPC_V1, FLOW_IPC_V1_1, FLOW_IPC_V1_2,
                        FLOW_IPC_V1_3, FLOW_IPC_V1_4, FLOW_IPC_V1_5, FLOW_IPC_V1_7,
                        FLOW_IPC_V1_8, FLOW_IPC_V1_9_LOADONDEMAND, FLOW_IPC_V1_9,
                        FLOW_IPC_V1_10_LAZY_DEPENDENCY, FLOW_IPC_V1_10_PRODUCTION_RUNTIME,
                        FLOW_IPC_V1_10, FLOW_IPC_V1_11_LOADONDEMAND_CHUNK, FLOW_IPC_V1_11,
                        FLOW_IPC_V1_12_LOADONDEMAND_CHUNK_FILE, FLOW_IPC_V1_12,
                        FLOW_IPC_V1_13_PRODUCTION_OWNER, FLOW_IPC_V1_13,
                        FLOW_IPC_V1_14_PRODUCTION_OWNER_IMPORT, FLOW_IPC_V1_14,
                        FLOW_IPC_V1_15_PRODUCTION_OWNER_DEPENDENCIES, FLOW_IPC_V1_15,
                        FLOW_IPC_V1_16_PRODUCTION_OWNER_ORDERING, FLOW_IPC_V1_16,
                        FLOW_IPC_V1_17_RESOURCE_SCHEME, FLOW_IPC_V1_17,
                        FLOW_IPC_V1_18_PRODUCTION_OWNER_RESOURCE_SCHEME, FLOW_IPC_V1_18,
                        FLOW_IPC_V1_19_PRODUCTION_OWNER_SIDE_EFFECT, FLOW_IPC_V1_19,
                        FLOW_IPC_V1_20_PRODUCTION_OWNER_OWNED_RUNTIME, FLOW_IPC_V1_20,
                        FLOW_IPC_V1_21_FRONTEND_RESOLVER_CONTRACT, FLOW_IPC_V1_21,
                        FLOW_IPC_V1_22_GENERATED_CHUNK_FIXTURE, FLOW_IPC_V1_22,
                        FLOW_IPC_V1_23_PRODUCTION_OWNER_POSITIVE_FIXTURE, FLOW_IPC_V1_23,
                        FLOW_IPC_V1_24_FRONTEND_PIPELINE_CLOSURE, FLOW_IPC_V1_24,
                        FLOW_IPC_V1_25_PACKAGED_FRONTEND_BASELINE, FLOW_IPC_V1_25,
                        FLOW_IPC_V1_26_GENERATED_LAZY_SOURCE_BOUNDARY, FLOW_IPC_V1_26);
                case FLOW_IPC_V1_27_GENERATED_LAZY_REAL_BUILD_PROBE ->
                    EnumSet.of(FLOW_IPC_V1_27_GENERATED_LAZY_REAL_BUILD_PROBE);
                case FLOW_IPC_V1_27 -> EnumSet.of(FLOW_IPC_V1, FLOW_IPC_V1_1, FLOW_IPC_V1_2,
                        FLOW_IPC_V1_3, FLOW_IPC_V1_4, FLOW_IPC_V1_5, FLOW_IPC_V1_7,
                        FLOW_IPC_V1_8, FLOW_IPC_V1_9_LOADONDEMAND, FLOW_IPC_V1_9,
                        FLOW_IPC_V1_10_LAZY_DEPENDENCY, FLOW_IPC_V1_10_PRODUCTION_RUNTIME,
                        FLOW_IPC_V1_10, FLOW_IPC_V1_11_LOADONDEMAND_CHUNK, FLOW_IPC_V1_11,
                        FLOW_IPC_V1_12_LOADONDEMAND_CHUNK_FILE, FLOW_IPC_V1_12,
                        FLOW_IPC_V1_13_PRODUCTION_OWNER, FLOW_IPC_V1_13,
                        FLOW_IPC_V1_14_PRODUCTION_OWNER_IMPORT, FLOW_IPC_V1_14,
                        FLOW_IPC_V1_15_PRODUCTION_OWNER_DEPENDENCIES, FLOW_IPC_V1_15,
                        FLOW_IPC_V1_16_PRODUCTION_OWNER_ORDERING, FLOW_IPC_V1_16,
                        FLOW_IPC_V1_17_RESOURCE_SCHEME, FLOW_IPC_V1_17,
                        FLOW_IPC_V1_18_PRODUCTION_OWNER_RESOURCE_SCHEME, FLOW_IPC_V1_18,
                        FLOW_IPC_V1_19_PRODUCTION_OWNER_SIDE_EFFECT, FLOW_IPC_V1_19,
                        FLOW_IPC_V1_20_PRODUCTION_OWNER_OWNED_RUNTIME, FLOW_IPC_V1_20,
                        FLOW_IPC_V1_21_FRONTEND_RESOLVER_CONTRACT, FLOW_IPC_V1_21,
                        FLOW_IPC_V1_22_GENERATED_CHUNK_FIXTURE, FLOW_IPC_V1_22,
                        FLOW_IPC_V1_23_PRODUCTION_OWNER_POSITIVE_FIXTURE, FLOW_IPC_V1_23,
                        FLOW_IPC_V1_24_FRONTEND_PIPELINE_CLOSURE, FLOW_IPC_V1_24,
                        FLOW_IPC_V1_25_PACKAGED_FRONTEND_BASELINE, FLOW_IPC_V1_25,
                        FLOW_IPC_V1_26_GENERATED_LAZY_SOURCE_BOUNDARY, FLOW_IPC_V1_26,
                        FLOW_IPC_V1_27_GENERATED_LAZY_REAL_BUILD_PROBE, FLOW_IPC_V1_27);
                case FLOW_IPC_V1_28_GENERATED_LAZY_BUILD_LIFECYCLE ->
                    EnumSet.of(FLOW_IPC_V1_28_GENERATED_LAZY_BUILD_LIFECYCLE);
                case FLOW_IPC_V1_28 -> EnumSet.of(FLOW_IPC_V1, FLOW_IPC_V1_1, FLOW_IPC_V1_2,
                        FLOW_IPC_V1_3, FLOW_IPC_V1_4, FLOW_IPC_V1_5, FLOW_IPC_V1_7,
                        FLOW_IPC_V1_8, FLOW_IPC_V1_9_LOADONDEMAND, FLOW_IPC_V1_9,
                        FLOW_IPC_V1_10_LAZY_DEPENDENCY, FLOW_IPC_V1_10_PRODUCTION_RUNTIME,
                        FLOW_IPC_V1_10, FLOW_IPC_V1_11_LOADONDEMAND_CHUNK, FLOW_IPC_V1_11,
                        FLOW_IPC_V1_12_LOADONDEMAND_CHUNK_FILE, FLOW_IPC_V1_12,
                        FLOW_IPC_V1_13_PRODUCTION_OWNER, FLOW_IPC_V1_13,
                        FLOW_IPC_V1_14_PRODUCTION_OWNER_IMPORT, FLOW_IPC_V1_14,
                        FLOW_IPC_V1_15_PRODUCTION_OWNER_DEPENDENCIES, FLOW_IPC_V1_15,
                        FLOW_IPC_V1_16_PRODUCTION_OWNER_ORDERING, FLOW_IPC_V1_16,
                        FLOW_IPC_V1_17_RESOURCE_SCHEME, FLOW_IPC_V1_17,
                        FLOW_IPC_V1_18_PRODUCTION_OWNER_RESOURCE_SCHEME, FLOW_IPC_V1_18,
                        FLOW_IPC_V1_19_PRODUCTION_OWNER_SIDE_EFFECT, FLOW_IPC_V1_19,
                        FLOW_IPC_V1_20_PRODUCTION_OWNER_OWNED_RUNTIME, FLOW_IPC_V1_20,
                        FLOW_IPC_V1_21_FRONTEND_RESOLVER_CONTRACT, FLOW_IPC_V1_21,
                        FLOW_IPC_V1_22_GENERATED_CHUNK_FIXTURE, FLOW_IPC_V1_22,
                        FLOW_IPC_V1_23_PRODUCTION_OWNER_POSITIVE_FIXTURE, FLOW_IPC_V1_23,
                        FLOW_IPC_V1_24_FRONTEND_PIPELINE_CLOSURE, FLOW_IPC_V1_24,
                        FLOW_IPC_V1_25_PACKAGED_FRONTEND_BASELINE, FLOW_IPC_V1_25,
                        FLOW_IPC_V1_26_GENERATED_LAZY_SOURCE_BOUNDARY, FLOW_IPC_V1_26,
                        FLOW_IPC_V1_27_GENERATED_LAZY_REAL_BUILD_PROBE, FLOW_IPC_V1_27,
                        FLOW_IPC_V1_28_GENERATED_LAZY_BUILD_LIFECYCLE, FLOW_IPC_V1_28);
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
                case FLOW_IPC_V1_17_RESOURCE_SCHEME ->
                    summary.hasRequiredFlowIpcV1_17ResourceSchemeTypes();
                case FLOW_IPC_V1_17 -> summary.hasRequiredFlowIpcV1_17Types();
                case FLOW_IPC_V1_18_PRODUCTION_OWNER_RESOURCE_SCHEME ->
                    summary.hasRequiredFlowIpcV1_18ProductionOwnerResourceSchemeTypes();
                case FLOW_IPC_V1_18 -> summary.hasRequiredFlowIpcV1_18Types();
                case FLOW_IPC_V1_19_PRODUCTION_OWNER_SIDE_EFFECT ->
                    summary.hasRequiredFlowIpcV1_19ProductionOwnerSideEffectTypes();
                case FLOW_IPC_V1_19 -> summary.hasRequiredFlowIpcV1_19Types();
                case FLOW_IPC_V1_20_PRODUCTION_OWNER_OWNED_RUNTIME ->
                    summary.hasRequiredFlowIpcV1_20ProductionOwnerOwnedRuntimeTypes();
                case FLOW_IPC_V1_20 -> summary.hasRequiredFlowIpcV1_20Types();
                case FLOW_IPC_V1_21_FRONTEND_RESOLVER_CONTRACT ->
                    summary.hasRequiredFlowIpcV1_21FrontendResolverContractTypes();
                case FLOW_IPC_V1_21 -> summary.hasRequiredFlowIpcV1_21Types();
                case FLOW_IPC_V1_22_GENERATED_CHUNK_FIXTURE ->
                    summary.hasRequiredFlowIpcV1_22GeneratedChunkFixtureTypes();
                case FLOW_IPC_V1_22 -> summary.hasRequiredFlowIpcV1_22Types();
                case FLOW_IPC_V1_23_PRODUCTION_OWNER_POSITIVE_FIXTURE ->
                    summary.hasRequiredFlowIpcV1_23ProductionOwnerPositiveFixtureTypes();
                case FLOW_IPC_V1_23 -> summary.hasRequiredFlowIpcV1_23Types();
                case FLOW_IPC_V1_24_FRONTEND_PIPELINE_CLOSURE ->
                    summary.hasRequiredFlowIpcV1_24FrontendPipelineClosureTypes();
                case FLOW_IPC_V1_24 -> summary.hasRequiredFlowIpcV1_24Types();
                case FLOW_IPC_V1_25_PACKAGED_FRONTEND_BASELINE ->
                    summary.hasRequiredFlowIpcV1_25PackagedFrontendBaselineTypes();
                case FLOW_IPC_V1_25 -> summary.hasRequiredFlowIpcV1_25Types();
                case FLOW_IPC_V1_26_GENERATED_LAZY_SOURCE_BOUNDARY ->
                    summary.hasRequiredFlowIpcV1_26GeneratedLazySourceBoundaryTypes();
                case FLOW_IPC_V1_26 -> summary.hasRequiredFlowIpcV1_26Types();
                case FLOW_IPC_V1_27_GENERATED_LAZY_REAL_BUILD_PROBE ->
                    summary.hasRequiredFlowIpcV1_27GeneratedLazyRealBuildProbeTypes();
                case FLOW_IPC_V1_27 -> summary.hasRequiredFlowIpcV1_27Types();
                case FLOW_IPC_V1_28_GENERATED_LAZY_BUILD_LIFECYCLE ->
                    summary.hasRequiredFlowIpcV1_28GeneratedLazyBuildLifecycleTypes();
                case FLOW_IPC_V1_28 -> summary.hasRequiredFlowIpcV1_28Types();
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
                    && containsStringField(line, "frontendResourceLoaderVersion",
                            "flow-ipc-resource-scheme-v1_17")
                    && containsStringField(line, "frontendResourceLoaderModes",
                            "custom-scheme,file-backed,blob-fallback")
                    && containsStringField(line, "frontendResourceLoaderPreferredMode",
                            "custom-scheme-file-backed")
                    && containsLiteralField(line, "frontendResourceLoaderNoLocalhost", "true")
                    && line.contains("frontendResourceLoaderAcceptableFallbacks")
                    && line.contains("production-owner-blob-fallback")) {
                summary.frontendResourceSchemeV117ManifestObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"componentFrontendResources\"")
                    && containsStringField(line, "productionBundleRuntimeOwnerResourceLoaderVersion",
                            "flow-ipc-production-owner-resource-loader-v1_18")
                    && containsStringField(line, "productionBundleRuntimeOwnerResourceLoaderMode",
                            "custom-scheme-file-backed")
                    && containsStringField(line, "productionBundleRuntimeOwnerResourceScheme",
                            "vatori-resource")
                    && containsStringField(line, "productionBundleRuntimeOwnerResourceGraphMode",
                            "owner-root-and-resolved-dependencies")
                    && containsStringField(line, "productionBundleRuntimeOwnerResourceBasePolicy",
                            "stable-owner-resource-path-base-v1_18")
                    && containsStringField(line, "productionBundleRuntimeOwnerResourceLoaderNamespace",
                            "production-owner")
                    && containsStringField(line, "productionBundleRuntimeOwnerResourceLoaderRootArtifact",
                            "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && containsLiteralField(line, "productionBundleRuntimeOwnerBlobFallbackAllowed", "false")
                    && containsStringField(line, "productionBundleRuntimeOwnerDependencyBlobFallbackPolicy",
                            "explicit-classified-only")) {
                summary.productionRuntimeOwnerResourceSchemeV118ManifestObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"componentFrontendResources\"")
                    && containsStringField(line, "productionBundleRuntimeOwnerSideEffectVersion",
                            "flow-ipc-production-owner-side-effect-v1_19")
                    && containsStringField(line, "productionBundleRuntimeOwnerSideEffectPolicy",
                            "custom-scheme-owner-import-side-effect-contract-v1_19")
                    && containsStringField(line, "productionBundleRuntimeOwnerSideEffectExpectedTags",
                            "vaadin-text-field,vaadin-checkbox,vaadin-select")
                    && containsStringField(line, "productionBundleRuntimeOwnerSideEffectExpectedGlobals",
                            "window.Vaadin,window.Vaadin.Flow,window.Vaadin.Flow.loadOnDemand")
                    && containsLiteralField(line, "productionBundleRuntimeOwnerSideEffectNoBlobFallback", "true")
                    && containsStringField(line, "componentProductionOwnerReuseDecisionVersion",
                            "flow-ipc-component-owner-reuse-decision-v1_19")
                    && containsStringField(line, "componentProductionOwnerReuseDecisionSource",
                            "production-owner-custom-scheme-side-effect-v1_19")) {
                summary.productionRuntimeOwnerSideEffectV119ManifestObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"componentFrontendResources\"")
                    && containsStringField(line, "productionBundleRuntimeOwnerOwnedRuntimeVersion",
                            "flow-ipc-production-owner-owned-runtime-v1_20")
                    && containsStringField(line, "productionBundleRuntimeOwnerOwnedRuntimePolicy",
                            "owner-first-custom-scheme-runtime-reuse-v1_20")
                    && containsStringField(line, "productionBundleRuntimeOwnerOwnedRuntimePhase",
                            "before-component-imports")
                    && containsStringField(line, "productionBundleRuntimeOwnerOwnedRuntimeExpectedTags",
                            "vaadin-text-field,vaadin-checkbox,vaadin-select")
                    && containsLiteralField(line, "productionBundleRuntimeOwnerOwnedRuntimeNoBlobFallback", "true")
                    && containsStringField(line, "componentProductionOwnerReusePolicyVersionV1_20",
                            "flow-ipc-component-owner-reuse-policy-v1_20")
                    && containsStringField(line, "componentProductionOwnerReusePolicyV1_20",
                            "owner-first-then-focused-fallback-v1_20")) {
                summary.productionRuntimeOwnerOwnedRuntimeV120ManifestObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"componentFrontendResources\"")
                    && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureVersion",
                            "flow-ipc-production-owner-positive-runtime-v1_23")
                    && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureSource",
                            "repository-production-owner-positive-fixture-v1_23")
                    && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureRootArtifact",
                            "META-INF/vatori/production-owner/owner-runtime-v1_23.js")
                    && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureDependencyArtifact",
                            "META-INF/vatori/production-owner/owner-dependency-v1_23.js")
                    && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureExpectedTags",
                            "vatori-production-owner-fixture")
                    && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureExpectedGlobals",
                            "window.VaadinDesktopProductionOwnerV123")
                    && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureExecutionPolicy",
                            "owner-first-custom-scheme-positive-fixture-v1_23")
                    && containsLiteralField(line, "productionBundleRuntimeOwnerPositiveFixtureNoBlobFallback",
                            "true")) {
                summary.productionRuntimeOwnerPositiveFixtureV123ManifestObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"componentFrontendResources\"")
                    && containsFrontendPipelineClosureV124Fields(line)) {
                summary.frontendPipelineClosureV124ManifestObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"componentFrontendResources\"")
                    && containsPackagedFrontendBaselineV125Fields(line)) {
                summary.packagedFrontendBaselineV125ManifestObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"componentFrontendResources\"")
                    && containsGeneratedLazySourceBoundaryV126Fields(line)) {
                summary.generatedLazySourceBoundaryV126ManifestObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"componentFrontendResources\"")
                    && containsGeneratedLazyRealBuildProbeV127Fields(line)) {
                summary.generatedLazyRealBuildProbeV127ManifestObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"componentFrontendResources\"")
                    && containsGeneratedLazyBuildLifecycleV128Fields(line)) {
                summary.generatedLazyBuildLifecycleV128ManifestObserved = true;
            }
            if (line.contains("\"type\":\"bootstrapResponse\"")
                    && line.contains("\"componentFrontendResources\"")
                    && containsStringField(line, "frontendResolverContractVersion",
                            "flow-ipc-frontend-resolver-contract-v1_21")
                    && containsStringField(line, "frontendResolverContractScope",
                            "flow-client,component-frontend,theme-static,generated-lazy,flow-lazy-dependency,production-owner,dev-mode")
                    && containsStringField(line, "frontendResolverIdentityPolicy",
                            "stable-source-path-plus-owner-namespace-v1_21")
                    && containsStringField(line, "frontendResolverIntegrityPolicy",
                            "sha256-required-for-bodied-resources-v1_21")
                    && containsStringField(line, "frontendResolverCachePolicy",
                            "window-lifetime-content-addressed-v1_21")
                    && containsStringField(line, "frontendResolverCleanupPolicy",
                            "host-owned-window-lifetime-v1_21")
                    && containsStringField(line, "frontendResolverAllowedSchemes",
                            "blob,vatori-resource")
                    && containsLiteralField(line, "frontendResolverNoLocalhost", "true")
                    && containsStringField(line, "frontendResolverFamiliesCovered",
                            "flow-client-classpath,vaadin-component-classpath-frontend,npm-frontend,theme-static,generated-lazy,flow-lazy-dependency,production-bundle,dev-mode-bundle")
                    && containsStringField(line, "frontendResolverAcceptableGaps",
                            "generated-lazy-chunk-file-not-generated,production-owner-owned-missing-real-artifact,dev-mode-vite-execution-unmapped")) {
                summary.frontendResolverContractV121ManifestObserved = true;
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
                    && (line.contains("\"moduleIdentityStrategy\":\"stable-resource-path-blob-module-identity-v1_7\"")
                            || line.contains("\"moduleIdentityStrategy\":\"owner-attempt-custom-element-class-export-v1_20\"")
                            || line.contains("\"moduleIdentityStrategy\":\"component-graph-existing-custom-element-class-export-v1_7\""))
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
                    && (line.contains("\"purpose\":\"vaadin-web-component-resource-probe\"")
                            || line.contains("\"purpose\":\"production-owner-dependency-v1_15\""))
                    && containsStringField(line, "productionBundleRuntimeOwnerResourceLoaderVersion",
                            "flow-ipc-production-owner-resource-loader-v1_18")
                    && containsStringField(line, "productionBundleRuntimeOwnerResourceLoaderMode",
                            "custom-scheme-file-backed")
                    && containsStringField(line, "productionBundleRuntimeOwnerResourceScheme",
                            "vatori-resource")
                    && containsStringField(line, "productionBundleRuntimeOwnerResourceGraphMode",
                            "owner-root-and-resolved-dependencies")
                    && containsStringField(line, "productionBundleRuntimeOwnerResourceBasePolicy",
                            "stable-owner-resource-path-base-v1_18")
                    && containsLiteralField(line, "productionBundleRuntimeOwnerBlobFallbackAllowed", "false")
                    && containsStringField(line, "productionBundleRuntimeOwnerDependencyBlobFallbackPolicy",
                            "explicit-classified-only")) {
                summary.productionRuntimeOwnerResourceSchemeV118RequestObserved = true;
            }
            if (line.contains("\"type\":\"resourceRequest\"")
                    && (line.contains("\"purpose\":\"vaadin-web-component-resource-probe\"")
                            || line.contains("\"purpose\":\"production-owner-dependency-v1_15\""))
                    && containsStringField(line, "productionBundleRuntimeOwnerSideEffectVersion",
                            "flow-ipc-production-owner-side-effect-v1_19")
                    && containsStringField(line, "productionBundleRuntimeOwnerSideEffectPolicy",
                            "custom-scheme-owner-import-side-effect-contract-v1_19")
                    && containsLiteralField(line, "productionBundleRuntimeOwnerSideEffectNoBlobFallback", "true")
                    && containsStringField(line, "componentProductionOwnerReuseDecisionVersion",
                            "flow-ipc-component-owner-reuse-decision-v1_19")
                    && containsStringField(line, "componentProductionOwnerReuseDecisionSource",
                            "production-owner-custom-scheme-side-effect-v1_19")) {
                summary.productionRuntimeOwnerSideEffectV119RequestObserved = true;
            }
            if (line.contains("\"type\":\"resourceRequest\"")
                    && (line.contains("\"purpose\":\"vaadin-web-component-resource-probe\"")
                            || line.contains("\"purpose\":\"production-owner-dependency-v1_15\""))
                    && containsStringField(line, "productionBundleRuntimeOwnerOwnedRuntimeVersion",
                            "flow-ipc-production-owner-owned-runtime-v1_20")
                    && containsStringField(line, "productionBundleRuntimeOwnerOwnedRuntimePolicy",
                            "owner-first-custom-scheme-runtime-reuse-v1_20")
                    && containsStringField(line, "productionBundleRuntimeOwnerOwnedRuntimePhase",
                            "before-component-imports")
                    && containsLiteralField(line, "productionBundleRuntimeOwnerOwnedRuntimeNoBlobFallback", "true")
                    && containsStringField(line, "componentProductionOwnerReusePolicyVersionV1_20",
                            "flow-ipc-component-owner-reuse-policy-v1_20")
                    && containsStringField(line, "componentProductionOwnerReusePolicyV1_20",
                            "owner-first-then-focused-fallback-v1_20")) {
                summary.productionRuntimeOwnerOwnedRuntimeV120RequestObserved = true;
            }
            if (line.contains("\"type\":\"resourceRequest\"")
                    && containsStringField(line, "frontendResolverContractVersion",
                            "flow-ipc-frontend-resolver-contract-v1_21")
                    && containsStringField(line, "frontendResolverIdentityPolicy",
                            "stable-source-path-plus-owner-namespace-v1_21")
                    && containsStringField(line, "frontendResolverIntegrityPolicy",
                            "sha256-required-for-bodied-resources-v1_21")
                    && containsLiteralField(line, "frontendResolverNoLocalhost", "true")
                    && containsStringField(line, "frontendResolverAllowedSchemes",
                            "blob,vatori-resource")
                    && line.contains("\"expectedSha256\":\"")) {
                summary.frontendResolverContractV121RequestObserved = true;
            }
            if (line.contains("\"type\":\"resourceResponse\"")
                    && containsStringField(line, "frontendResolverContractVersion",
                            "flow-ipc-frontend-resolver-contract-v1_21")
                    && containsStringField(line, "frontendResolverIntegrityPolicy",
                            "sha256-required-for-bodied-resources-v1_21")
                    && containsLiteralField(line, "frontendResolverNoLocalhost", "true")
                    && line.contains("\"sha256\":\"")
                    && line.contains("\"bodyRedacted\":true")) {
                summary.frontendResolverContractV121ResponseObserved = true;
            }
            if (line.contains("\"type\":\"resourceRequest\"")
                    && containsPath(line, "META-INF/vatori/generated/flow/generated-flow-imports-v1_22.js")
                    && containsStringField(line, "generatedLazyChunkFileExecutionVersion",
                            "flow-ipc-loadondemand-chunk-file-execution-v1_22")
                    && containsLiteralField(line, "generatedLazyChunkFileFixture", "true")
                    && containsStringField(line, "generatedLazyChunkFileFixtureRole", "imports")
                    && containsStringField(line, "generatedLazyChunkFileFixtureSource",
                            "repository-vaadin-shaped-generated-flow-imports-v1_22")
                    && containsStringField(line, "generatedLazyChunkFileFixtureChunkArtifact",
                            "META-INF/vatori/generated/flow/chunks/chunk-v1_22.js")
                    && containsStringField(line, "loadOnDemandTableShape", "branch-table-dynamic-import")
                    && containsStringField(line, "chunkFileStatus", "available")) {
                summary.generatedChunkFileFixtureV122RequestObserved = true;
            }
            if (line.contains("\"type\":\"resourceResponse\"")
                    && containsPath(line, "META-INF/vatori/generated/flow/generated-flow-imports-v1_22.js")
                    && containsStringField(line, "generatedLazyChunkFileExecutionVersion",
                            "flow-ipc-loadondemand-chunk-file-execution-v1_22")
                    && containsLiteralField(line, "generatedLazyChunkFileFixture", "true")
                    && containsStringField(line, "loadOnDemandTableShape", "branch-table-dynamic-import")
                    && containsStringField(line, "chunkImportSpecifier", "./chunks/chunk-v1_22.js")
                    && containsStringField(line, "chunkFileStatus", "available")
                    && containsStringField(line, "loadOnDemandChunkFileSideEffectProof",
                            "chunk-file-body-available-for-staged-import")
                    && line.contains("\"sha256\":\"")
                    && line.contains("\"bodyRedacted\":true")) {
                summary.generatedChunkFileFixtureV122ImportsResponseObserved = true;
            }
            if (line.contains("\"type\":\"resourceResponse\"")
                    && containsPath(line, "META-INF/vatori/generated/flow/chunks/chunk-v1_22.js")
                    && containsStringField(line, "generatedLazyChunkFileExecutionVersion",
                            "flow-ipc-loadondemand-chunk-file-execution-v1_22")
                    && containsLiteralField(line, "generatedLazyChunkFileFixture", "true")
                    && containsStringField(line, "generatedLazyChunkFileFixtureRole", "chunk")
                    && containsStringField(line, "chunkFileStatus", "available")
                    && containsStringField(line, "loadOnDemandChunkFileStatus", "available")
                    && containsStringField(line, "loadOnDemandChunkFileSideEffectProof",
                            "chunk-file-body-available-for-staged-import")
                    && line.contains("\"sha256\":\"")
                    && line.contains("\"bodyRedacted\":true")) {
                summary.generatedChunkFileFixtureV122ChunkResponseObserved = true;
            }
            if (line.contains("\"type\":\"resourceRequest\"")
                    && containsPath(line, "META-INF/vatori/production-owner/owner-runtime-v1_23.js")
                    && containsStringField(line, "lookupKind", "Production-owner-positive-fixture-root-v1_23")
                    && containsStringField(line, "resolver", "repository-production-owner-positive-fixture-v1_23")
                    && containsStringField(line, "executionPolicy",
                            "owner-first-custom-scheme-positive-fixture-v1_23")
                    && containsStringField(line, "productionBundleRuntimeOwnerResourceScheme",
                            "vatori-resource")
                    && containsStringField(line, "productionBundleRuntimeOwnerSideEffectExpectedTags",
                            "vatori-production-owner-fixture")
                    && containsStringField(line, "productionBundleRuntimeOwnerOwnedRuntimeExpectedGlobals",
                            "window.VaadinDesktopProductionOwnerV123")
                    && containsLiteralField(line, "productionBundleRuntimeOwnerOwnedRuntimeNoBlobFallback",
                            "true")) {
                summary.productionRuntimeOwnerPositiveFixtureV123RootRequestObserved = true;
            }
            if (line.contains("\"type\":\"resourceRequest\"")
                    && containsPath(line, "META-INF/vatori/production-owner/owner-dependency-v1_23.js")
                    && containsStringField(line, "lookupKind",
                            "Production-owner-positive-fixture-dependency-v1_23")
                    && containsStringField(line, "resolver", "repository-production-owner-positive-fixture-v1_23")
                    && containsStringField(line, "executionPolicy",
                            "owner-positive-fixture-dependency-v1_23")
                    && containsStringField(line, "dependencyOf",
                            "META-INF/vatori/production-owner/owner-runtime-v1_23.js")
                    && containsStringField(line, "productionBundleRuntimeOwnerResourceScheme",
                            "vatori-resource")
                    && containsStringField(line, "productionBundleRuntimeOwnerSideEffectExpectedTags",
                            "vatori-production-owner-fixture")
                    && containsLiteralField(line, "productionBundleRuntimeOwnerOwnedRuntimeNoBlobFallback",
                            "true")) {
                summary.productionRuntimeOwnerPositiveFixtureV123DependencyRequestObserved = true;
            }
            if (line.contains("\"type\":\"resourceResponse\"")
                    && containsPath(line, "META-INF/vatori/production-owner/owner-runtime-v1_23.js")
                    && containsLiteralField(line, "productionBundleRuntimeOwnerPositiveFixture", "true")
                    && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureRole", "root")
                    && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureVersion",
                            "flow-ipc-production-owner-positive-runtime-v1_23")
                    && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureDependencyArtifact",
                            "META-INF/vatori/production-owner/owner-dependency-v1_23.js")
                    && line.contains("\"sha256\":\"")
                    && line.contains("\"bodyRedacted\":true")) {
                summary.productionRuntimeOwnerPositiveFixtureV123RootResponseObserved = true;
            }
            if (line.contains("\"type\":\"resourceResponse\"")
                    && containsPath(line, "META-INF/vatori/production-owner/owner-dependency-v1_23.js")
                    && containsLiteralField(line, "productionBundleRuntimeOwnerPositiveFixture", "true")
                    && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureRole", "dependency")
                    && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureVersion",
                            "flow-ipc-production-owner-positive-runtime-v1_23")
                    && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureDependencyArtifact",
                            "META-INF/vatori/production-owner/owner-dependency-v1_23.js")
                    && line.contains("productionBundleRuntimeOwnerPositiveFixtureDependencySha256")
                    && line.contains("\"sha256\":\"")
                    && line.contains("\"bodyRedacted\":true")) {
                summary.productionRuntimeOwnerPositiveFixtureV123DependencyResponseObserved = true;
            }
            if (line.contains("\"type\":\"resourceRequest\"")
                    && containsFrontendPipelineClosureV124Fields(line)) {
                summary.frontendPipelineClosureV124RequestObserved = true;
            }
            if (line.contains("\"type\":\"resourceResponse\"")
                    && containsFrontendPipelineClosureV124Fields(line)) {
                summary.frontendPipelineClosureV124ResponseObserved = true;
            }
            if (line.contains("\"type\":\"resourceRequest\"")
                    && containsPackagedFrontendBaselineV125Fields(line)) {
                summary.packagedFrontendBaselineV125RequestObserved = true;
            }
            if (line.contains("\"type\":\"resourceResponse\"")
                    && containsPackagedFrontendBaselineV125Fields(line)) {
                summary.packagedFrontendBaselineV125ResponseObserved = true;
            }
            if (line.contains("\"type\":\"resourceRequest\"")
                    && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                    && containsGeneratedLazySourceBoundaryV126Fields(line)) {
                summary.generatedLazySourceBoundaryV126RequestObserved = true;
            }
            if (line.contains("\"type\":\"resourceResponse\"")
                    && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                    && containsGeneratedLazySourceBoundaryV126Fields(line)
                    && line.contains("\"bodyRedacted\":true")) {
                summary.generatedLazySourceBoundaryV126ResponseObserved = true;
            }
            if (line.contains("\"type\":\"resourceRequest\"")
                    && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                    && containsGeneratedLazyRealBuildProbeV127Fields(line)) {
                summary.generatedLazyRealBuildProbeV127RequestObserved = true;
            }
            if (line.contains("\"type\":\"resourceResponse\"")
                    && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                    && containsGeneratedLazyRealBuildProbeV127Fields(line)
                    && line.contains("\"bodyRedacted\":true")) {
                summary.generatedLazyRealBuildProbeV127ResponseObserved = true;
            }
            if (line.contains("\"type\":\"resourceRequest\"")
                    && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                    && containsGeneratedLazyBuildLifecycleV128Fields(line)) {
                summary.generatedLazyBuildLifecycleV128RequestObserved = true;
            }
            if (line.contains("\"type\":\"resourceResponse\"")
                    && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                    && containsGeneratedLazyBuildLifecycleV128Fields(line)
                    && line.contains("\"bodyRedacted\":true")) {
                summary.generatedLazyBuildLifecycleV128ResponseObserved = true;
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
            if (line.contains("\"type\":\"resourceLoad\"")
                    && containsStringField(line, "frontendResourceSchemeVersion",
                            "flow-ipc-resource-scheme-v1_17")
                    && containsStringField(line, "frontendResourceLoaderMode",
                            "custom-scheme-file-backed")
                    && containsStringField(line, "frontendResourceSchemeName", "vatori-resource")
                    && containsLiteralField(line, "frontendResourceSchemeHandlerInstalled", "true")
                    && containsLiteralField(line, "frontendResourceLoaderNoLocalhost", "true")) {
                summary.frontendResourceSchemeV117HostInstallObserved = true;
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && containsStringField(line, "frontendResourceSchemeVersion",
                            "flow-ipc-resource-scheme-v1_17")
                    && containsStringField(line, "frontendResourceLoaderMode",
                            "custom-scheme-file-backed")
                    && containsStringField(line, "frontendResourceSchemeName", "vatori-resource")
                    && containsLiteralField(line, "frontendResourceSchemeHandlerInstalled", "true")
                    && containsLiteralField(line, "frontendResourceLoaderNoLocalhost", "true")
                    && containsLiteralField(line, "frontendResourceFileBacked", "true")
                    && containsStringField(line, "stagingMode", "custom-scheme")
                    && containsStringField(line, "mimeType", "text/javascript; charset=utf-8")
                    && line.contains("filePathRedacted")
                    && line.contains("$REPO/.build/vaadin-desktop-host-swift/flow-ipc-resources")
                    && line.contains("resourceBaseUrl")
                    && line.contains("vatori-resource://")
                    && containsPath(line, "vatori-flow-ipc-lazy-v1_10.js")) {
                summary.frontendResourceSchemeV117StageObserved = true;
                if (line.contains("\"executionStatus\":\"executed\"")) {
                    summary.frontendResourceSchemeV117ExecutedObserved = true;
                }
            }
            if (line.contains("\"type\":\"resourceLoad\"")
                    && containsStringField(line, "frontendResourceSchemeVersion",
                            "flow-ipc-resource-scheme-v1_17")
                    && containsStringField(line, "frontendResourceLoaderMode",
                            "custom-scheme-file-backed")
                    && containsLiteralField(line, "frontendResourceSchemeRequestObserved", "true")
                    && containsLiteralField(line, "frontendResourceLoaderNoLocalhost", "true")
                    && containsPath(line, "vatori-flow-ipc-lazy-v1_10.js")) {
                summary.frontendResourceSchemeV117HandlerRequestObserved = true;
                if (containsLiteralField(line, "frontendResourceSchemeResponseObserved", "true")
                        && containsStringField(line, "executionStatus", "served")) {
                    summary.frontendResourceSchemeV117HandlerResponseObserved = true;
                }
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && containsStringField(line, "frontendResourceSchemeVersion",
                            "flow-ipc-resource-scheme-v1_17")
                    && containsStringField(line, "stagingMode", "blob-fallback")
                    && line.contains("blobFallbackReason")
                    && line.contains("blobFallbackBlockedLayer")
                    && line.contains("blobFallbackNextResolutionStep")) {
                summary.frontendResourceSchemeV117FallbackClassifiedObserved = true;
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && line.contains("\"artifactFamily\":\"production-bundle\"")
                    && containsStringField(line, "productionBundleRuntimeOwnerResourceLoaderVersion",
                            "flow-ipc-production-owner-resource-loader-v1_18")
                    && containsStringField(line, "productionBundleRuntimeOwnerResourceLoaderMode",
                            "custom-scheme-file-backed")
                    && containsStringField(line, "productionBundleRuntimeOwnerResourceScheme",
                            "vatori-resource")
                    && containsStringField(line, "productionBundleRuntimeOwnerResourceGraphMode",
                            "owner-root-and-resolved-dependencies")
                    && containsStringField(line, "productionBundleRuntimeOwnerResourceBasePolicy",
                            "stable-owner-resource-path-base-v1_18")
                    && containsStringField(line, "productionBundleRuntimeOwnerResourceLoaderNamespace",
                            "production-owner")
                    && containsLiteralField(line, "productionBundleRuntimeOwnerBlobFallbackAllowed", "false")
                    && containsStringField(line, "productionBundleRuntimeOwnerDependencyBlobFallbackPolicy",
                            "explicit-classified-only")
                    && containsStringField(line, "frontendResourceLoaderMode",
                            "custom-scheme-file-backed")
                    && containsStringField(line, "stagingMode", "custom-scheme")
                    && containsStringField(line, "productionBundleRuntimeOwnerResourcePathMode",
                            "stable-source-path")
                    && !containsStringField(line, "stagingMode", "blob-fallback")) {
                if (containsStringField(line, "productionBundleRuntimeOwnerResourceLoaderArtifactRole",
                        "root")
                        && containsPath(line,
                                "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                        && (line.contains("\"executionStatus\":\"execution-attempt\"")
                                || line.contains("\"executionStatus\":\"strict-blocker\"")
                                || line.contains("\"executionStatus\":\"executed\""))) {
                    summary.productionRuntimeOwnerResourceSchemeV118RootStageObserved = true;
                    summary.productionRuntimeOwnerResourceSchemeV118ImportObserved = true;
                }
                if (containsStringField(line, "productionBundleRuntimeOwnerResourceLoaderArtifactRole",
                        "dependency")
                        && line.contains("productionBundleRuntimeOwnerDependencyCustomSchemeProbeResult")) {
                    summary.productionRuntimeOwnerResourceSchemeV118DependencyStageObserved = true;
                }
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && line.contains("\"artifactFamily\":\"production-bundle\"")
                    && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && containsStringField(line, "productionBundleRuntimeOwnerSideEffectVersion",
                            "flow-ipc-production-owner-side-effect-v1_19")
                    && containsStringField(line, "productionBundleRuntimeOwnerSideEffectPolicy",
                            "custom-scheme-owner-import-side-effect-contract-v1_19")
                    && containsStringField(line, "productionBundleRuntimeOwnerSideEffectExpectedTags",
                            "vaadin-text-field,vaadin-checkbox,vaadin-select")
                    && line.contains("productionBundleRuntimeOwnerSideEffectObservedTags")
                    && line.contains("productionBundleRuntimeOwnerSideEffectMissingTags")
                    && containsStringField(line, "productionBundleRuntimeOwnerSideEffectExpectedGlobals",
                            "window.Vaadin,window.Vaadin.Flow,window.Vaadin.Flow.loadOnDemand")
                    && containsLiteralField(line, "productionBundleRuntimeOwnerSideEffectNoBlobFallback", "true")
                    && line.contains("productionBundleRuntimeOwnerModuleImported")
                    && line.contains("productionBundleRuntimeOwnerSideEffectSatisfied")
                    && containsProductionOwnerSideEffectV119FinalResult(line,
                            "productionBundleRuntimeOwnerSideEffectFailureLayer")
                    && containsStringField(line, "componentProductionOwnerReuseDecisionVersion",
                            "flow-ipc-component-owner-reuse-decision-v1_19")
                    && containsStringField(line, "componentProductionOwnerReuseDecisionSource",
                            "production-owner-custom-scheme-side-effect-v1_19")
                    && containsComponentOwnerReuseDecisionV119(line,
                            "componentProductionOwnerReuseDecision")
                    && containsLiteralField(line, "productionBundleRuntimeOwnerBlobFallbackUsed", "false")) {
                summary.productionRuntimeOwnerSideEffectV119StageObserved = true;
                summary.productionRuntimeOwnerSideEffectV119ReuseDecisionObserved = true;
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && line.contains("\"artifactFamily\":\"production-bundle\"")
                    && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && containsStringField(line, "productionBundleRuntimeOwnerOwnedRuntimeVersion",
                            "flow-ipc-production-owner-owned-runtime-v1_20")
                    && containsStringField(line, "productionBundleRuntimeOwnerOwnedRuntimePolicy",
                            "owner-first-custom-scheme-runtime-reuse-v1_20")
                    && containsStringField(line, "productionBundleRuntimeOwnerOwnedRuntimePhase",
                            "before-component-imports")
                    && containsStringField(line, "productionBundleRuntimeOwnerOwnedRuntimeActualPhase",
                            "before-component-imports")
                    && containsStringField(line, "productionBundleRuntimeOwnerOwnedRuntimeExpectedTags",
                            "vaadin-text-field,vaadin-checkbox,vaadin-select")
                    && containsLiteralField(line, "productionBundleRuntimeOwnerOwnedRuntimeNoBlobFallback", "true")
                    && containsLiteralField(line, "productionBundleRuntimeOwnerOwnedRuntimeAttempted", "true")
                    && line.contains("productionBundleRuntimeOwnerOwnedRuntimeSatisfied")
                    && containsProductionOwnerOwnedRuntimeV120FinalResult(line,
                            "productionBundleRuntimeOwnerOwnedRuntimeFailureLayer")
                    && containsStringField(line, "componentProductionOwnerReusePolicyVersionV1_20",
                            "flow-ipc-component-owner-reuse-policy-v1_20")
                    && containsStringField(line, "componentProductionOwnerReusePolicyV1_20",
                            "owner-first-then-focused-fallback-v1_20")
                    && containsComponentOwnerReuseDecisionV120(line,
                            "componentProductionOwnerReuseDecision")
                    && containsLiteralField(line, "productionBundleRuntimeOwnerBlobFallbackUsed", "false")) {
                summary.productionRuntimeOwnerOwnedRuntimeV120StageObserved = true;
                summary.productionRuntimeOwnerOwnedRuntimeV120ReuseDecisionObserved = true;
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && (containsStringField(line, "productionBundleRuntimeOwnerOwnedRuntimeFailureLayer",
                            "production-owner-owned-missing-real-artifact")
                            || containsStringField(line, "componentProductionOwnerReuseDecision",
                                    "focused-fallback-after-owner-owned-runtime-attempt"))) {
                summary.productionRuntimeOwnerPositiveFixtureV123RealBlockerObserved = true;
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && containsStringField(line, "frontendResolverContractVersion",
                            "flow-ipc-frontend-resolver-contract-v1_21")
                    && containsStringField(line, "frontendResolverIdentityPolicy",
                            "stable-source-path-plus-owner-namespace-v1_21")
                    && containsStringField(line, "frontendResolverIntegrityPolicy",
                            "sha256-required-for-bodied-resources-v1_21")
                    && containsLiteralField(line, "frontendResolverNoLocalhost", "true")
                    && line.contains("\"sha256\":\"")
                    && line.contains("\"sourcePath\":\"")) {
                summary.frontendResolverContractV121StageObserved = true;
                if (containsStringField(line, "stagedUrlScheme", "blob")
                        || line.contains("\"stagedUrl\":\"blob:")) {
                    summary.frontendResolverContractV121BlobStageObserved = true;
                }
                if (containsStringField(line, "stagedUrlScheme", "vatori-resource")
                        || line.contains("vatori-resource://")) {
                    summary.frontendResolverContractV121CustomSchemeStageObserved = true;
                }
                if (line.contains("\"artifactFamily\":\"generated-lazy\"")
                        || line.contains("\"artifactFamily\":\"production-bundle\"")
                        || line.contains("\"artifactFamily\":\"dev-mode-bundle\"")) {
                    summary.frontendResolverContractV121StrictOrGapStageObserved = true;
                }
            }
            if (line.contains("\"type\":\"resourceLoad\"")
                    && containsStringField(line, "productionBundleRuntimeOwnerResourceLoaderVersion",
                            "flow-ipc-production-owner-resource-loader-v1_18")
                    && containsStringField(line, "productionBundleRuntimeOwnerResourceLoaderMode",
                            "custom-scheme-file-backed")
                    && containsStringField(line, "productionBundleRuntimeOwnerResourceScheme",
                            "vatori-resource")
                    && containsStringField(line, "productionBundleRuntimeOwnerResourcePathMode",
                            "stable-source-path")
                    && containsLiteralField(line, "frontendResourceSchemeRequestObserved", "true")) {
                if (containsStringField(line, "productionBundleRuntimeOwnerResourceLoaderArtifactRole",
                        "root")
                        && containsPath(line,
                                "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")) {
                    summary.productionRuntimeOwnerResourceSchemeV118RootHandlerRequestObserved = true;
                    if (containsLiteralField(line, "frontendResourceSchemeResponseObserved", "true")
                            && containsStringField(line, "executionStatus", "served")) {
                        summary.productionRuntimeOwnerResourceSchemeV118RootHandlerResponseObserved = true;
                    }
                }
                if (containsStringField(line, "productionBundleRuntimeOwnerResourceLoaderArtifactRole",
                        "dependency")) {
                    summary.productionRuntimeOwnerResourceSchemeV118DependencyHandlerRequestObserved = true;
                    if (containsLiteralField(line, "frontendResourceSchemeResponseObserved", "true")
                            && containsStringField(line, "executionStatus", "served")) {
                        summary.productionRuntimeOwnerResourceSchemeV118DependencyHandlerResponseObserved = true;
                    }
                }
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
            if (line.contains("\"type\":\"resourceStage\"")
                    && line.contains("\"artifactFamily\":\"generated-lazy\"")
                    && containsStringField(line, "generatedLazyChunkFileExecutionVersion",
                            "flow-ipc-loadondemand-chunk-file-execution-v1_22")
                    && containsLiteralField(line, "generatedLazyChunkFileFixtureObserved", "true")
                    && containsStringField(line, "generatedLazyChunkFileFixtureSource",
                            "repository-vaadin-shaped-generated-flow-imports-v1_22")
                    && containsPath(line, "META-INF/vatori/generated/flow/generated-flow-imports-v1_22.js")
                    && containsStringField(line, "generatedLazyChunkFileFixtureChunkArtifact",
                            "META-INF/vatori/generated/flow/chunks/chunk-v1_22.js")
                    && containsStringField(line, "generatedLazyChunkFileFixtureChunkId",
                            "vatori-flow-ipc-generated-lazy-chunk-v1_22")
                    && containsStringField(line, "loadOnDemandTableShape", "branch-table-dynamic-import")
                    && containsStringField(line, "chunkFileStatus", "available")
                    && containsLiteralField(line, "generatedLazyChunkFileFixtureStageObserved", "true")
                    && containsLiteralField(line, "generatedLazyChunkFileFixtureExecuted", "true")
                    && containsLiteralField(line, "generatedLazyChunkFileFixtureCallbackResolved", "true")
                    && containsLiteralField(line, "generatedLazyChunkFileFixtureSideEffectSatisfied", "true")
                    && containsStringField(line, "generatedLazyChunkFileFixtureSideEffectProof",
                            "window.VaadinDesktopGeneratedLazyChunkV122.executed")
                    && line.contains("\"executionStatus\":\"executed\"")) {
                summary.generatedChunkFileFixtureV122StageObserved = true;
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && line.contains("\"artifactFamily\":\"production-bundle\"")
                    && containsPath(line, "META-INF/vatori/production-owner/owner-runtime-v1_23.js")
                    && containsLiteralField(line, "productionBundleRuntimeOwnerPositiveFixture", "true")
                    && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureRole", "root")
                    && containsLiteralField(line, "productionBundleRuntimeOwnerPositiveFixtureStageObserved",
                            "true")
                    && containsLiteralField(line, "productionBundleRuntimeOwnerPositiveFixtureImported",
                            "true")
                    && containsLiteralField(line,
                            "productionBundleRuntimeOwnerPositiveFixtureSideEffectSatisfied", "true")
                    && containsLiteralField(line,
                            "productionBundleRuntimeOwnerPositiveFixtureOwnerOwnedRuntimeSatisfied", "true")
                    && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureVersion",
                            "flow-ipc-production-owner-positive-runtime-v1_23")
                    && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureSource",
                            "repository-production-owner-positive-fixture-v1_23")
                    && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureExecutionPolicy",
                            "owner-first-custom-scheme-positive-fixture-v1_23")
                    && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureExpectedTags",
                            "vatori-production-owner-fixture")
                    && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureExpectedGlobals",
                            "window.VaadinDesktopProductionOwnerV123")
                    && containsLiteralField(line, "productionBundleRuntimeOwnerPositiveFixtureNoBlobFallback",
                            "true")
                    && containsStringFieldPrefix(line,
                            "productionBundleRuntimeOwnerPositiveFixtureRootCustomSchemeUrl",
                            "vatori-resource://flow-ipc/production-owner/")
                    && containsStringFieldPrefix(line,
                            "productionBundleRuntimeOwnerPositiveFixtureDependencyCustomSchemeUrl",
                            "vatori-resource://flow-ipc/production-owner/")
                    && containsStringFieldPrefix(line,
                            "productionBundleRuntimeOwnerPositiveFixtureExecutionResult", "executed:")
                    && containsStringField(line,
                            "productionBundleRuntimeOwnerPositiveFixtureComponentReuseDecision",
                            "owner-reused-positive-fixture")
                    && containsStringField(line, "stagedUrlScheme", "vatori-resource")
                    && containsStringField(line, "executionStatus", "executed")) {
                summary.productionRuntimeOwnerPositiveFixtureV123RootStageObserved = true;
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && line.contains("\"artifactFamily\":\"production-bundle\"")
                    && containsPath(line, "META-INF/vatori/production-owner/owner-dependency-v1_23.js")
                    && containsLiteralField(line, "productionBundleRuntimeOwnerPositiveFixture", "true")
                    && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureRole", "dependency")
                    && containsLiteralField(line, "productionBundleRuntimeOwnerPositiveFixtureStageObserved",
                            "true")
                    && containsLiteralField(line, "productionBundleRuntimeOwnerPositiveFixtureImported",
                            "true")
                    && containsLiteralField(line,
                            "productionBundleRuntimeOwnerPositiveFixtureSideEffectSatisfied", "true")
                    && containsLiteralField(line,
                            "productionBundleRuntimeOwnerPositiveFixtureOwnerOwnedRuntimeSatisfied", "true")
                    && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureVersion",
                            "flow-ipc-production-owner-positive-runtime-v1_23")
                    && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureSource",
                            "repository-production-owner-positive-fixture-v1_23")
                    && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureExecutionPolicy",
                            "owner-first-custom-scheme-positive-fixture-v1_23")
                    && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureExpectedTags",
                            "vatori-production-owner-fixture")
                    && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureExpectedGlobals",
                            "window.VaadinDesktopProductionOwnerV123")
                    && containsLiteralField(line, "productionBundleRuntimeOwnerPositiveFixtureNoBlobFallback",
                            "true")
                    && containsStringFieldPrefix(line,
                            "productionBundleRuntimeOwnerPositiveFixtureDependencyCustomSchemeUrl",
                            "vatori-resource://flow-ipc/production-owner/")
                    && containsStringFieldPrefix(line,
                            "productionBundleRuntimeOwnerPositiveFixtureExecutionResult", "executed:")
                    && containsStringField(line,
                            "productionBundleRuntimeOwnerPositiveFixtureComponentReuseDecision",
                            "owner-reused-positive-fixture")
                    && containsStringField(line, "stagedUrlScheme", "vatori-resource")
                    && containsStringField(line, "executionStatus", "executed")) {
                summary.productionRuntimeOwnerPositiveFixtureV123DependencyStageObserved = true;
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && containsFrontendPipelineClosureV124Fields(line)
                    && (line.contains("\"executionStatus\":\"executed\"")
                            || line.contains("\"executionStatus\":\"strict-blocker\""))) {
                summary.frontendPipelineClosureV124StageObserved = true;
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && containsPackagedFrontendBaselineV125Fields(line)
                    && (line.contains("\"executionStatus\":\"executed\"")
                            || line.contains("\"executionStatus\":\"strict-blocker\"")
                            || line.contains("\"executionStatus\":\"execution-attempt\""))) {
                summary.packagedFrontendBaselineV125StageObserved = true;
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && containsPackagedFrontendBaselineV125Fields(line)
                    && containsStringField(line, "stagedUrlScheme", "vatori-resource")
                    && containsStringField(line, "stagingMode", "custom-scheme")) {
                summary.packagedFrontendBaselineV125CustomSchemeStageObserved = true;
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && containsPackagedFrontendBaselineV125Fields(line)
                    && containsStringField(line, "stagedUrlScheme", "blob")
                    && containsStringField(line, "packagedFrontendAllowedFallback",
                            "diagnostic-blob-only")) {
                summary.packagedFrontendBaselineV125DiagnosticBlobFallbackObserved = true;
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                    && containsGeneratedLazySourceBoundaryV126Fields(line)
                    && (line.contains("\"executionStatus\":\"executed\"")
                            || line.contains("\"executionStatus\":\"strict-blocker\"")
                            || line.contains("\"executionStatus\":\"staged\""))) {
                summary.generatedLazySourceBoundaryV126StageObserved = true;
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                    && containsGeneratedLazyRealBuildProbeV127Fields(line)
                    && (line.contains("\"executionStatus\":\"executed\"")
                            || line.contains("\"executionStatus\":\"strict-blocker\"")
                            || line.contains("\"executionStatus\":\"staged\""))) {
                summary.generatedLazyRealBuildProbeV127StageObserved = true;
            }
            if (line.contains("\"type\":\"resourceStage\"")
                    && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                    && containsGeneratedLazyBuildLifecycleV128Fields(line)
                    && (line.contains("\"executionStatus\":\"executed\"")
                            || line.contains("\"executionStatus\":\"strict-blocker\"")
                            || line.contains("\"executionStatus\":\"staged\""))) {
                summary.generatedLazyBuildLifecycleV128StageObserved = true;
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
                    && (containsStringField(line, "frontendArtifactTextFieldModuleIdentityStrategy",
                            "stable-resource-path-blob-module-identity-v1_7")
                            || containsStringField(line, "frontendArtifactTextFieldModuleIdentityStrategy",
                                    "owner-attempt-custom-element-class-export-v1_20")
                            || containsStringField(line, "frontendArtifactTextFieldModuleIdentityStrategy",
                                    "component-graph-existing-custom-element-class-export-v1_7"))
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
                    && containsStringField(line, "frontendResourceSchemeVersion",
                            "flow-ipc-resource-scheme-v1_17")
                    && containsStringField(line, "frontendResourceLoaderMode",
                            "custom-scheme-file-backed")
                    && containsStringField(line, "frontendResourceSchemeName", "vatori-resource")
                    && containsLiteralField(line, "frontendResourceSchemeHandlerInstalled", "true")
                    && containsLiteralField(line, "frontendResourceSchemeRequestObserved", "true")
                    && containsLiteralField(line, "frontendResourceSchemeResponseObserved", "true")
                    && containsLiteralField(line, "frontendResourceLoaderNoLocalhost", "true")
                    && containsLiteralField(line, "frontendResourceFileBacked", "true")
                    && containsLiteralField(line, "frontendResourceLoaderObserved", "true")
                    && containsLiteralField(line, "frontendResourceLoaderExecuted", "true")
                    && containsPath(line, "vatori-flow-ipc-lazy-v1_10.js")
                    && line.contains("frontendResourceFilePathRedacted")
                    && line.contains("$REPO/.build/vaadin-desktop-host-swift/flow-ipc-resources")
                    && line.contains("frontendResourceRelativeImportResolutionMode")
                    && line.contains("scheme-url-native-relative-base-v1_17")
                    && line.contains("custom-scheme-served-by-wkurlschemehandler-no-http-v1_17")) {
                summary.frontendResourceSchemeV117DomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsStringField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerResourceLoaderVersion",
                            "flow-ipc-production-owner-resource-loader-v1_18")
                    && containsStringField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerResourceLoaderMode",
                            "custom-scheme-file-backed")
                    && containsStringField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerResourceScheme",
                            "vatori-resource")
                    && containsStringField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerResourceGraphMode",
                            "owner-root-and-resolved-dependencies")
                    && containsStringField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerResourceBasePolicy",
                            "stable-owner-resource-path-base-v1_18")
                    && containsStringField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerResourceLoaderNamespace",
                            "production-owner")
                    && containsStringField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerResourceLoaderRootArtifact",
                            "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                    && containsStringFieldPrefix(line,
                            "frontendArtifactProductionBundleRuntimeOwnerRootCustomSchemeUrl",
                            "vatori-resource://flow-ipc/production-owner/")
                    && containsPositiveIntegerField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerDependencyCustomSchemeUrlCount")
                    && line.contains("frontendArtifactProductionBundleRuntimeOwnerDependencyCustomSchemePaths")
                    && containsLiteralField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerImportOverCustomScheme", "true")
                    && containsLiteralField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerCustomSchemeRequestObserved", "true")
                    && containsLiteralField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerCustomSchemeResponseObserved", "true")
                    && containsLiteralField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerDependencyCustomSchemeRequestObserved",
                            "true")
                    && containsLiteralField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerDependencyCustomSchemeResponseObserved",
                            "true")
                    && containsLiteralField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerBlobFallbackAllowed", "false")
                    && containsLiteralField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerBlobFallbackUsed", "false")
                    && containsStringField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerDependencyBlobFallbackPolicy",
                            "explicit-classified-only")
                    && containsStringField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerResourcePathMode",
                            "stable-source-path")
                    && (containsProductionOwnerResourceSchemeV118FinalLayer(line,
                            "frontendArtifactProductionBundleRuntimeOwnerFailureLayer")
                            || containsStringField(line,
                                    "frontendArtifactProductionBundleRuntimeOwnerFailureLayer",
                                    "production-owner-component-reuse-contract-missing"))
                    && !containsStringField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerFailureLayer",
                            "production-owner-relative-import-unresolved")
                    && !containsStringField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerFailureLayer",
                            "production-owner-runtime-contract-missing")
                    && !containsStringField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerFailureLayer",
                            "production-owner-missing-real-artifact")) {
                summary.productionRuntimeOwnerResourceSchemeV118DomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsStringField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerSideEffectVersion",
                            "flow-ipc-production-owner-side-effect-v1_19")
                    && containsStringField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerSideEffectPolicy",
                            "custom-scheme-owner-import-side-effect-contract-v1_19")
                    && containsStringField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerSideEffectExpectedTags",
                            "vaadin-text-field,vaadin-checkbox,vaadin-select")
                    && line.contains("frontendArtifactProductionBundleRuntimeOwnerSideEffectObservedTags")
                    && line.contains("frontendArtifactProductionBundleRuntimeOwnerSideEffectMissingTags")
                    && containsStringField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerSideEffectExpectedGlobals",
                            "window.Vaadin,window.Vaadin.Flow,window.Vaadin.Flow.loadOnDemand")
                    && containsLiteralField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerSideEffectNoBlobFallback", "true")
                    && line.contains("frontendArtifactProductionBundleRuntimeOwnerModuleImported")
                    && line.contains("frontendArtifactProductionBundleRuntimeOwnerSideEffectSatisfied")
                    && containsProductionOwnerSideEffectV119FinalResult(line,
                            "frontendArtifactProductionBundleRuntimeOwnerSideEffectFailureLayer")
                    && containsStringField(line, "componentProductionOwnerReuseDecisionVersion",
                            "flow-ipc-component-owner-reuse-decision-v1_19")
                    && containsStringField(line, "componentProductionOwnerReuseDecisionSource",
                            "production-owner-custom-scheme-side-effect-v1_19")
                    && containsComponentOwnerReuseDecisionV119(line,
                            "componentProductionOwnerReuseDecision")
                    && containsLiteralField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerBlobFallbackUsed", "false")
                    && containsLiteralField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerImportOverCustomScheme", "true")) {
                summary.productionRuntimeOwnerSideEffectV119DomObserved = true;
                summary.productionRuntimeOwnerSideEffectV119ReuseDecisionObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsStringField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerOwnedRuntimeVersion",
                            "flow-ipc-production-owner-owned-runtime-v1_20")
                    && containsStringField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerOwnedRuntimePolicy",
                            "owner-first-custom-scheme-runtime-reuse-v1_20")
                    && containsStringField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerOwnedRuntimePhase",
                            "before-component-imports")
                    && containsStringField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerOwnedRuntimeActualPhase",
                            "before-component-imports")
                    && containsStringField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerOwnedRuntimeExpectedTags",
                            "vaadin-text-field,vaadin-checkbox,vaadin-select")
                    && containsLiteralField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerOwnedRuntimeNoBlobFallback", "true")
                    && containsLiteralField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerOwnedRuntimeAttempted", "true")
                    && line.contains("frontendArtifactProductionBundleRuntimeOwnerOwnedRuntimeSatisfied")
                    && containsProductionOwnerOwnedRuntimeV120FinalResult(line,
                            "frontendArtifactProductionBundleRuntimeOwnerOwnedRuntimeFailureLayer")
                    && containsStringField(line, "componentProductionOwnerReusePolicyVersionV1_20",
                            "flow-ipc-component-owner-reuse-policy-v1_20")
                    && containsStringField(line, "componentProductionOwnerReusePolicyV1_20",
                            "owner-first-then-focused-fallback-v1_20")
                    && containsComponentOwnerReuseDecisionV120(line,
                            "componentProductionOwnerReuseDecision")
                    && containsLiteralField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerBlobFallbackUsed", "false")
                    && containsLiteralField(line,
                            "frontendArtifactProductionBundleRuntimeOwnerImportOverCustomScheme", "true")) {
                summary.productionRuntimeOwnerOwnedRuntimeV120DomObserved = true;
                summary.productionRuntimeOwnerOwnedRuntimeV120ReuseDecisionObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "frontendResolverContractObserved", "true")
                    && containsStringField(line, "frontendResolverContractVersion",
                            "flow-ipc-frontend-resolver-contract-v1_21")
                    && containsStringField(line, "frontendResolverIdentityPolicy",
                            "stable-source-path-plus-owner-namespace-v1_21")
                    && containsStringField(line, "frontendResolverIntegrityPolicy",
                            "sha256-required-for-bodied-resources-v1_21")
                    && containsStringField(line, "frontendResolverCachePolicy",
                            "window-lifetime-content-addressed-v1_21")
                    && containsStringField(line, "frontendResolverCleanupPolicy",
                            "host-owned-window-lifetime-v1_21")
                    && containsStringField(line, "frontendResolverAllowedSchemes",
                            "blob,vatori-resource")
                    && containsLiteralField(line, "frontendResolverNoLocalhost", "true")
                    && line.contains("frontendResolverSchemesObserved")
                    && line.contains("blob")
                    && line.contains("vatori-resource")
                    && line.contains("frontendResolverCustomSchemeFamilies")
                    && line.contains("flow-lazy-dependency")
                    && line.contains("production-bundle")
                    && line.contains("frontendResolverBlobFamilies")
                    && line.contains("frontendResolverVerifiedOnlyFamilies")
                    && line.contains("flow-client-classpath")
                    && line.contains("theme-static")
                    && line.contains("frontendResolverStrictBlockerFamilies")
                    && line.contains("generated-lazy")
                    && line.contains("production-bundle")
                    && line.contains("dev-mode-bundle")
                    && containsLiteralField(line, "frontendResolverContractComplete", "true")) {
                summary.frontendResolverContractV121DomObserved = true;
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
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsStringField(line, "generatedLazyChunkFileExecutionVersion",
                            "flow-ipc-loadondemand-chunk-file-execution-v1_22")
                    && containsLiteralField(line, "generatedLazyChunkFileFixtureObserved", "true")
                    && containsStringField(line, "generatedLazyChunkFileFixtureSource",
                            "repository-vaadin-shaped-generated-flow-imports-v1_22")
                    && containsStringField(line, "generatedLazyChunkFileFixtureImportsArtifact",
                            "META-INF/vatori/generated/flow/generated-flow-imports-v1_22.js")
                    && containsStringField(line, "generatedLazyChunkFileFixtureChunkArtifact",
                            "META-INF/vatori/generated/flow/chunks/chunk-v1_22.js")
                    && containsStringField(line, "generatedLazyChunkFileFixtureChunkId",
                            "vatori-flow-ipc-generated-lazy-chunk-v1_22")
                    && containsStringField(line, "generatedLazyChunkFileFixtureTriggerOwner",
                            "repository-fixture-generated-loadondemand-executor")
                    && containsLiteralField(line, "generatedLazyChunkFileFixtureStageObserved", "true")
                    && containsLiteralField(line, "generatedLazyChunkFileFixtureExecuted", "true")
                    && containsLiteralField(line, "generatedLazyChunkFileFixtureCallbackResolved", "true")
                    && containsLiteralField(line, "generatedLazyChunkFileFixtureSideEffectSatisfied", "true")
                    && containsStringFieldPrefix(line, "generatedLazyChunkFileFixtureExecutionResult",
                            "executed:")
                    && containsStringField(line, "generatedLazyChunkFileFixtureSideEffectProof",
                            "window.VaadinDesktopGeneratedLazyChunkV122.executed")
                    && containsStringField(line, "generatedLazyChunkFileFixtureFailureLayer", "")) {
                summary.generatedChunkFileFixtureV122DomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "productionBundleRuntimeOwnerPositiveFixtureObserved",
                            "true")
                    && containsLiteralField(line, "productionBundleRuntimeOwnerPositiveFixtureStageObserved",
                            "true")
                    && containsLiteralField(line, "productionBundleRuntimeOwnerPositiveFixtureImported",
                            "true")
                    && containsLiteralField(line,
                            "productionBundleRuntimeOwnerPositiveFixtureSideEffectSatisfied", "true")
                    && containsLiteralField(line,
                            "productionBundleRuntimeOwnerPositiveFixtureOwnerOwnedRuntimeSatisfied", "true")
                    && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureVersion",
                            "flow-ipc-production-owner-positive-runtime-v1_23")
                    && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureSource",
                            "repository-production-owner-positive-fixture-v1_23")
                    && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureRootArtifact",
                            "META-INF/vatori/production-owner/owner-runtime-v1_23.js")
                    && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureDependencyArtifact",
                            "META-INF/vatori/production-owner/owner-dependency-v1_23.js")
                    && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureExpectedTags",
                            "vatori-production-owner-fixture")
                    && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureExpectedGlobals",
                            "window.VaadinDesktopProductionOwnerV123")
                    && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureExecutionPolicy",
                            "owner-first-custom-scheme-positive-fixture-v1_23")
                    && containsLiteralField(line, "productionBundleRuntimeOwnerPositiveFixtureNoBlobFallback",
                            "true")
                    && containsStringFieldPrefix(line,
                            "productionBundleRuntimeOwnerPositiveFixtureRootCustomSchemeUrl",
                            "vatori-resource://flow-ipc/production-owner/")
                    && containsStringFieldPrefix(line,
                            "productionBundleRuntimeOwnerPositiveFixtureDependencyCustomSchemeUrl",
                            "vatori-resource://flow-ipc/production-owner/")
                    && containsStringFieldPrefix(line,
                            "productionBundleRuntimeOwnerPositiveFixtureExecutionResult", "executed:")
                    && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureSideEffectProof",
                            "customElements:vatori-production-owner-fixture;global:window.VaadinDesktopProductionOwnerV123;dependency=window.VaadinDesktopProductionOwnerDependencyV123.executed")
                    && containsStringField(line,
                            "productionBundleRuntimeOwnerPositiveFixtureComponentReuseDecision",
                            "owner-reused-positive-fixture")
                    && containsStringField(line, "componentProductionOwnerReuseDecision",
                            "owner-reused-positive-fixture")
                    && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureFailureLayer", "")) {
                summary.productionRuntimeOwnerPositiveFixtureV123DomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && (containsStringField(line, "frontendArtifactProductionBundleRuntimeOwnerOwnedRuntimeFailureLayer",
                            "production-owner-owned-missing-real-artifact")
                            || containsStringField(line, "productionBundleRuntimeOwnerOwnedRuntimeFailureLayer",
                                    "production-owner-owned-missing-real-artifact")
                            || containsStringField(line, "componentProductionOwnerReuseDecision",
                                    "focused-fallback-after-owner-owned-runtime-attempt"))) {
                summary.productionRuntimeOwnerPositiveFixtureV123RealBlockerObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "frontendPipelineClosureObserved", "true")
                    && containsFrontendPipelineClosureV124Fields(line)) {
                summary.frontendPipelineClosureV124DomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "packagedFrontendBaselineObserved", "true")
                    && containsPackagedFrontendBaselineV125Fields(line)) {
                summary.packagedFrontendBaselineV125DomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "packagedFrontendCustomSchemeObserved", "true")
                    && line.contains("packagedFrontendHostOwnedStagedPaths")
                    && line.contains("packagedFrontendHostOwnedExecutedPaths")) {
                summary.packagedFrontendBaselineV125CustomSchemeDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "packagedFrontendDiagnosticBlobFallbackObserved", "true")
                    && line.contains("packagedFrontendDiagnosticBlobFallbackPaths")) {
                summary.packagedFrontendBaselineV125DiagnosticBlobFallbackDomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "generatedLazyChunkFileFixtureSideEffectSatisfied", "true")
                    && containsLiteralField(line,
                            "productionBundleRuntimeOwnerPositiveFixtureSideEffectSatisfied", "true")
                    && containsStringField(line, "frontendPipelineClosureSatisfiedProofs",
                            "flow-client-executed,component-frontend-executed,theme-static-verified,flow-lazy-dependency-executed,generated-chunk-fixture-executed,production-owner-fixture-executed")) {
                summary.frontendPipelineClosureV124FixtureProofsObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsStringField(line, "frontendPipelineClosureRemainingBlockers",
                            "real-generated-lazy-chunk-file-not-generated,real-production-owner-owned-runtime-missing-real-artifact,dev-mode-vite-execution-unmapped")
                    && containsStringField(line, "frontendPipelineClosureRealBuildStatus",
                            "product-parity-pending")
                    && (containsStringField(line, "frontendArtifactProductionBundleRuntimeOwnerOwnedRuntimeFailureLayer",
                            "production-owner-owned-missing-real-artifact")
                            || containsStringField(line, "componentProductionOwnerReuseDecision",
                                    "focused-fallback-after-owner-owned-runtime-attempt"))) {
                summary.frontendPipelineClosureV124RealBlockersObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "packagedFrontendRealBlockersObserved", "true")
                    && containsStringField(line, "packagedFrontendRealBuildStatus",
                            "product-parity-pending")
                    && containsStringField(line, "packagedFrontendRemainingBlockers",
                            "real-generated-lazy-chunk-file-not-generated,real-production-owner-owned-runtime-missing-real-artifact,dev-mode-vite-execution-unmapped")) {
                summary.packagedFrontendBaselineV125RealBlockersObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "generatedLazySourceBoundaryObserved", "true")
                    && containsGeneratedLazySourceBoundaryV126Fields(line)) {
                summary.generatedLazySourceBoundaryV126DomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "generatedLazyRealBuildProbeObserved", "true")
                    && containsGeneratedLazyRealBuildProbeV127Fields(line)) {
                summary.generatedLazyRealBuildProbeV127DomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "generatedLazyBuildLifecycleObserved", "true")
                    && containsGeneratedLazyBuildLifecycleV128Fields(line)) {
                summary.generatedLazyBuildLifecycleV128DomObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "generatedLazySourceCurrentBuildNoOpConfirmed", "true")
                    && containsStringField(line, "generatedLazySourceCurrentBuildTableShape",
                            "no-op-promise-resolve-0")
                    && containsStringField(line, "generatedLazySourceCurrentBuildChunkStatus",
                            "not-generated")
                    && containsStringField(line, "generatedLazySourceRemainingBlocker",
                            "real-app-generated-chunk-file-not-produced-by-current-build")
                    && containsStringField(line, "loadOnDemandChunkFileFailureLayer",
                            "chunk-file-not-generated")) {
                summary.generatedLazySourceBoundaryV126CurrentBuildBlockerObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "generatedLazyRealBuildProbeManifestObserved", "true")
                    && containsLiteralField(line, "generatedLazyRealBuildInputRouteObserved", "true")
                    && containsLiteralField(line, "generatedLazyRealBuildInputRouteMatches", "true")
                    && containsLiteralField(line, "generatedLazyRealBuildScannerObserved", "true")
                    && containsLiteralField(line, "generatedLazyRealBuildLazyJavascriptObserved", "true")
                    && containsLiteralField(line, "generatedLazyRealBuildNoOpLoadOnDemand", "true")
                    && containsStringField(line, "generatedLazyRealBuildFailureLayer",
                            "scanner-output-not-connected-to-dev-bundle-generated-imports")
                    && containsStringField(line, "loadOnDemandChunkFileFailureLayer",
                            "chunk-file-not-generated")) {
                summary.generatedLazyRealBuildProbeV127CurrentBuildBlockerObserved = true;
            }
            if (line.contains("\"type\":\"webview.dom\"")
                    && containsLiteralField(line, "generatedLazyBuildLifecycleScannerObserved", "true")
                    && containsLiteralField(line, "generatedLazyBuildLifecycleLazyJavascriptObserved", "true")
                    && containsStringField(line, "generatedLazyBuildLifecycleGeneratedImportsSource",
                            "static-dev-bundle")
                    && containsStringField(line, "generatedLazyBuildLifecycleGeneratedImportsProtocol",
                            "jar")
                    && containsStringField(line, "generatedLazyBuildLifecycleGeneratedImportsTableShape",
                            "no-op-promise-resolve-0")
                    && containsLiteralField(line,
                            "generatedLazyBuildLifecycleGeneratedImportsNoOpLoadOnDemand", "true")
                    && containsLiteralField(line, "generatedLazyBuildLifecycleUpdateImportsObserved", "false")
                    && containsLiteralField(line, "generatedLazyBuildLifecycleWriteTargetObserved", "false")
                    && containsLiteralField(line,
                            "generatedLazyBuildLifecycleRuntimeUsesStaticDevBundle", "true")
                    && containsLiteralField(line, "generatedLazyBuildLifecycleRuntimeProbeMismatch", "true")
                    && containsStringField(line, "generatedLazyBuildLifecycleFailureLayer",
                            "runtime-using-stale-packaged-dev-bundle")
                    && containsStringField(line, "loadOnDemandChunkFileFailureLayer",
                            "chunk-file-not-generated")) {
                summary.generatedLazyBuildLifecycleV128CurrentBuildBlockerObserved = true;
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
                    && containsPath(line, "META-INF/resources/frontend/vaadin-big-decimal-field.js")
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
                    && line.contains("vaadin-big-decimal-field")
                    && line.contains("vaadin-checkbox")
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

    private static boolean containsFrontendPipelineClosureV124Fields(String line) {
        return containsStringField(line, "frontendPipelineClosureVersion",
                "flow-ipc-frontend-pipeline-closure-v1_24")
                && containsStringField(line, "frontendPipelineClosureScope",
                        "flow-client,component-frontend,theme-static,generated-lazy,flow-lazy-dependency,production-owner,dev-mode")
                && containsStringField(line, "frontendPipelineClosureTransport",
                        "no-localhost-ipc-resource-request")
                && containsStringField(line, "frontendPipelineClosureInfrastructureStatus",
                        "closed-for-representative-fixture-paths")
                && containsStringField(line, "frontendPipelineClosureRealBuildStatus",
                        "product-parity-pending")
                && containsStringField(line, "frontendPipelineClosureSatisfiedProofs",
                        "flow-client-executed,component-frontend-executed,theme-static-verified,flow-lazy-dependency-executed,generated-chunk-fixture-executed,production-owner-fixture-executed")
                && containsStringField(line, "frontendPipelineClosureRemainingBlockers",
                        "real-generated-lazy-chunk-file-not-generated,real-production-owner-owned-runtime-missing-real-artifact,dev-mode-vite-execution-unmapped")
                && containsStringField(line, "frontendPipelineClosureDecision",
                        "basic-pipeline-infrastructure-closed-real-build-parity-pending")
                && containsStringField(line, "frontendPipelineClosureNextProductPath",
                        "packaging-time-staged-frontend-or-supported-vaadin-build-spi")
                && containsLiteralField(line, "frontendPipelineClosureNoLocalhost", "true");
    }

    private static boolean containsPackagedFrontendBaselineV125Fields(String line) {
        return containsStringField(line, "packagedFrontendBaselineVersion",
                "flow-ipc-packaged-frontend-baseline-v1_25")
                && containsStringField(line, "packagedFrontendProductPath",
                        "packaging-time-staged-frontend-baseline")
                && containsStringField(line, "packagedFrontendInventoryScope",
                        "flow-client,component-frontend,theme-static,generated-lazy,flow-lazy-dependency,production-owner,dev-mode")
                && containsStringField(line, "packagedFrontendPreferredLoader",
                        "custom-scheme-file-backed")
                && containsStringField(line, "packagedFrontendAllowedFallback",
                        "diagnostic-blob-only")
                && containsStringField(line, "packagedFrontendIntegrity",
                        "sha256-and-byte-count")
                && containsStringField(line, "packagedFrontendCleanupPolicy",
                        "window-lifetime-cache-cleanup")
                && containsStringField(line, "packagedFrontendRealBuildStatus",
                        "product-parity-pending")
                && containsStringField(line, "packagedFrontendRemainingBlockers",
                        "real-generated-lazy-chunk-file-not-generated,real-production-owner-owned-runtime-missing-real-artifact,dev-mode-vite-execution-unmapped");
    }

    private static boolean containsGeneratedLazySourceBoundaryV126Fields(String line) {
        return containsStringField(line, "generatedLazySourceBoundaryVersion",
                "flow-ipc-generated-lazy-source-boundary-v1_26")
                && containsStringField(line, "generatedLazySourceMechanism",
                        "vaadin-AbstractUpdateImports-generateLazyLoadedChunks")
                && line.contains("generatedLazySourceArtifact")
                && containsStringField(line, "generatedLazySourceScanner",
                        "com.vaadin.flow.server.frontend.scanner.FrontendDependencies")
                && containsStringField(line, "generatedLazySourceLazyRouteRule",
                        "ChunkInfo(type=ROUTE,eager=false)")
                && containsStringField(line, "generatedLazySourceChunkTriggerInput",
                        "DependencyTrigger classes plus lazyJavascript/lazyCss")
                && containsStringField(line, "generatedLazySourcePositiveTableShape",
                        "branch-table-dynamic-import")
                && line.contains("pending.push(import('./chunks/chunk-<content-hash>.js'))")
                && containsStringField(line, "generatedLazySourceNoChunkFallback",
                        "Promise.resolve(0)")
                && containsStringField(line, "generatedLazySourceCurrentBuildArtifact",
                        "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                && containsStringField(line, "generatedLazySourceCurrentBuildStatus",
                        "no-real-generated-lazy-chunk-produced")
                && containsStringField(line, "generatedLazySourceCurrentBuildTableShape",
                        "no-op-promise-resolve-0")
                && containsStringField(line, "generatedLazySourceCurrentBuildChunkStatus",
                        "not-generated")
                && containsLiteralField(line, "generatedLazySourceCurrentBuildNoOpConfirmed", "true")
                && containsStringField(line, "generatedLazySourceBuildInputStatus",
                        "current-smoke-dev-bundle-has-no-lazy-javascript-or-css-chunk-input")
                && containsStringField(line, "generatedLazySourceSupportedBuildPath",
                        "run-vaadin-update-imports-with-non-eager-route-dependency-or-supported-lazy-chunk-spi")
                && containsStringField(line, "generatedLazySourceDecision",
                        "build-spi-required-before-real-app-chunk-parity")
                && containsStringField(line, "generatedLazySourceRemainingBlocker",
                        "real-app-generated-chunk-file-not-produced-by-current-build")
                && line.contains("v1.22 fixture")
                && containsLiteralField(line, "generatedLazySourceNoLocalhost", "true");
    }

    private static boolean containsGeneratedLazyRealBuildProbeV127Fields(String line) {
        return containsStringField(line, "generatedLazyRealBuildProbeVersion",
                "flow-ipc-generated-lazy-real-build-probe-v1_27")
                && containsStringField(line, "generatedLazyRealBuildProbeManifest",
                        "META-INF/vatori/generated-lazy-real-build-probe-v1_27.properties")
                && containsLiteralField(line, "generatedLazyRealBuildProbeManifestObserved", "true")
                && containsStringField(line, "generatedLazyRealBuildInputRoute",
                        "flow-ipc-lazy-probe-v1_27")
                && containsStringField(line, "generatedLazyRealBuildInputRouteClass",
                        "dev.vatori.ipcsmoke.FlowIpcGeneratedLazyProbeView")
                && containsStringField(line, "generatedLazyRealBuildInputTrigger",
                        "dev.vatori.ipcsmoke.FlowIpcGeneratedLazyProbeView")
                && containsStringField(line, "generatedLazyRealBuildInputDependency",
                        "context://vatori-flow-ipc-real-build-probe-v1_27.js")
                && containsLiteralField(line, "generatedLazyRealBuildInputEager", "false")
                && containsLiteralField(line, "generatedLazyRealBuildInputRouteObserved", "true")
                && containsLiteralField(line, "generatedLazyRealBuildInputRouteMatches", "true")
                && containsStringField(line, "generatedLazyRealBuildScannerSource",
                        "route-annotation-classpath-probe-v1_27")
                && containsLiteralField(line, "generatedLazyRealBuildScannerObserved", "true")
                && containsLiteralField(line, "generatedLazyRealBuildLazyJavascriptObserved", "true")
                && containsStringField(line, "generatedLazyRealBuildGeneratedImportsArtifact",
                        "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                && containsStringField(line, "generatedLazyRealBuildGeneratedImportsTableShape",
                        "no-op-promise-resolve-0")
                && containsLiteralField(line, "generatedLazyRealBuildNoOpLoadOnDemand", "true")
                && containsStringField(line, "generatedLazyRealBuildChunkStatus", "not-generated")
                && containsStringField(line, "generatedLazyRealBuildFailureLayer",
                        "scanner-output-not-connected-to-dev-bundle-generated-imports")
                && line.contains("non-eager v1.27 route input")
                && containsLiteralField(line, "generatedLazyRealBuildFixtureSubstituted", "false")
                && containsLiteralField(line, "generatedLazyRealBuildNoLocalhost", "true");
    }

    private static boolean containsGeneratedLazyBuildLifecycleV128Fields(String line) {
        return containsStringField(line, "generatedLazyBuildLifecycleVersion",
                "flow-ipc-generated-lazy-build-lifecycle-v1_28")
                && containsStringField(line, "generatedLazyBuildLifecycleInputRoute",
                        "flow-ipc-lazy-probe-v1_27")
                && containsStringField(line, "generatedLazyBuildLifecycleInputRouteClass",
                        "dev.vatori.ipcsmoke.FlowIpcGeneratedLazyProbeView")
                && containsStringField(line, "generatedLazyBuildLifecycleInputDependency",
                        "context://vatori-flow-ipc-real-build-probe-v1_27.js")
                && containsLiteralField(line, "generatedLazyBuildLifecycleInputEager", "false")
                && containsStringField(line, "generatedLazyBuildLifecycleScannerSource",
                        "route-annotation-classpath-probe-plus-vaadin-FrontendDependencies-contract-v1_28")
                && containsStringField(line, "generatedLazyBuildLifecycleScannerStatus", "observed")
                && containsLiteralField(line, "generatedLazyBuildLifecycleScannerObserved", "true")
                && containsLiteralField(line, "generatedLazyBuildLifecycleLazyJavascriptObserved", "true")
                && line.contains("ChunkInfo(type=ROUTE,eager=false")
                && containsStringField(line, "generatedLazyBuildLifecycleBuildTaskSource",
                        "dev-ipc-runtime-classpath-resource-resolver-v1_28")
                && containsLiteralField(line, "generatedLazyBuildLifecycleUpdateImportsObserved", "false")
                && containsStringField(line, "generatedLazyBuildLifecycleGeneratedImportsSource",
                        "static-dev-bundle")
                && line.contains("generatedLazyBuildLifecycleGeneratedImportsRuntimePath")
                && containsStringField(line, "generatedLazyBuildLifecycleGeneratedImportsProtocol", "jar")
                && line.contains("generatedLazyBuildLifecycleGeneratedImportsMtimeMillis")
                && line.contains("generatedLazyBuildLifecycleGeneratedImportsSha256")
                && line.contains("generatedLazyBuildLifecycleGeneratedImportsBytes")
                && containsStringField(line, "generatedLazyBuildLifecycleGeneratedImportsTableShape",
                        "no-op-promise-resolve-0")
                && containsLiteralField(line,
                        "generatedLazyBuildLifecycleGeneratedImportsNoOpLoadOnDemand", "true")
                && line.contains("generatedLazyBuildLifecycleWriteTarget")
                && containsLiteralField(line, "generatedLazyBuildLifecycleWriteTargetObserved", "false")
                && line.contains("generatedLazyBuildLifecycleWriteTargetMtimeMillis")
                && containsLiteralField(line,
                        "generatedLazyBuildLifecycleRuntimeUsesStaticDevBundle", "true")
                && containsLiteralField(line, "generatedLazyBuildLifecycleRuntimeProbeMismatch", "true")
                && containsStringField(line, "generatedLazyBuildLifecycleChunkStatus", "not-generated")
                && containsStringField(line, "generatedLazyBuildLifecycleFailureLayer",
                        "runtime-using-stale-packaged-dev-bundle")
                && line.contains("update-imports")
                && containsLiteralField(line, "generatedLazyBuildLifecycleFixtureSubstituted", "false")
                && containsLiteralField(line, "generatedLazyBuildLifecycleNoLocalhost", "true");
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
                || containsStringField(line, field, "production-owner-global-collision")
                || containsProductionOwnerCustomSchemeFailureLayer(line, field);
    }

    private static boolean containsProductionOwnerDependencyFailureLayer(String line, String field) {
        return containsStringField(line, field, "production-owner-duplicate-custom-element")
                || containsStringField(line, field, "production-owner-missing-real-artifact")
                || containsStringField(line, field, "production-owner-browser-import-failed")
                || containsStringField(line, field, "production-owner-side-effect-unobservable")
                || containsStringField(line, field, "production-owner-global-collision")
                || containsStringField(line, field, "production-owner-component-reuse-contract-missing")
                || containsStringField(line, field, "production-owner-unsupported-vaadin-production-owner-contract")
                || containsProductionOwnerCustomSchemeFailureLayer(line, field);
    }

    private static boolean containsProductionOwnerOrderingFailureLayer(String line, String field) {
        return containsStringField(line, field, "production-owner-duplicate-custom-element")
                || containsStringField(line, field, "production-owner-browser-import-failed")
                || containsStringField(line, field, "production-owner-side-effect-unobservable")
                || containsStringField(line, field, "production-owner-global-collision")
                || containsStringField(line, field, "production-owner-component-reuse-contract-missing")
                || containsStringField(line, field, "production-owner-unsupported-vaadin-production-owner-contract")
                || containsProductionOwnerCustomSchemeFailureLayer(line, field);
    }

    private static boolean containsProductionOwnerCustomSchemeFailureLayer(String line, String field) {
        return containsStringField(line, field, "production-owner-custom-scheme-missing-real-artifact")
                || containsStringField(line, field, "production-owner-custom-scheme-duplicate-custom-element")
                || containsStringField(line, field, "production-owner-custom-scheme-runtime-side-effect-blocked")
                || containsStringField(line, field, "production-owner-custom-scheme-browser-import-failed")
                || containsStringField(line, field, "production-owner-custom-scheme-unsupported-vaadin-contract");
    }

    private static boolean containsProductionOwnerResourceSchemeV118FinalLayer(String line, String field) {
        return containsProductionOwnerCustomSchemeFailureLayer(line, field)
                || containsProductionOwnerOwnedRuntimeFailureLayer(line, field);
    }

    private static boolean containsProductionOwnerSideEffectV119FinalResult(String line, String failureLayerField) {
        return containsLiteralField(line, "productionBundleRuntimeOwnerSideEffectSatisfied", "true")
                || containsLiteralField(line, "frontendArtifactProductionBundleRuntimeOwnerSideEffectSatisfied", "true")
                || containsProductionOwnerCustomSchemeFailureLayer(line, failureLayerField);
    }

    private static boolean containsComponentOwnerReuseDecisionV119(String line, String field) {
        return containsStringField(line, field, "owner-reused")
                || containsStringField(line, field, "owner-reused-positive-fixture")
                || containsStringField(line, field, "focused-fallback-after-owner-side-effect-check")
                || containsStringField(line, field, "focused-fallback-after-owner-owned-runtime-attempt");
    }

    private static boolean containsProductionOwnerOwnedRuntimeV120FinalResult(String line,
            String failureLayerField) {
        return containsLiteralField(line, "productionBundleRuntimeOwnerOwnedRuntimeSatisfied", "true")
                || containsLiteralField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerOwnedRuntimeSatisfied", "true")
                || containsProductionOwnerOwnedRuntimeFailureLayer(line, failureLayerField);
    }

    private static boolean containsProductionOwnerOwnedRuntimeFailureLayer(String line, String field) {
        return containsStringField(line, field, "production-owner-owned-side-effect-unobservable")
                || containsStringField(line, field, "production-owner-owned-duplicate-custom-element")
                || containsStringField(line, field, "production-owner-owned-missing-real-artifact")
                || containsStringField(line, field, "production-owner-owned-browser-import-failed")
                || containsStringField(line, field, "production-owner-owned-unsupported-vaadin-contract");
    }

    private static boolean containsComponentOwnerReuseDecisionV120(String line, String field) {
        return containsStringField(line, field, "owner-reused")
                || containsStringField(line, field, "owner-reused-positive-fixture")
                || containsStringField(line, field, "focused-fallback-after-owner-owned-runtime-attempt");
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
        private boolean frontendResourceSchemeV117ManifestObserved;
        private boolean frontendResourceSchemeV117HostInstallObserved;
        private boolean frontendResourceSchemeV117StageObserved;
        private boolean frontendResourceSchemeV117HandlerRequestObserved;
        private boolean frontendResourceSchemeV117HandlerResponseObserved;
        private boolean frontendResourceSchemeV117ExecutedObserved;
        private boolean frontendResourceSchemeV117DomObserved;
        private boolean frontendResourceSchemeV117FallbackClassifiedObserved;
        private boolean productionRuntimeOwnerResourceSchemeV118ManifestObserved;
        private boolean productionRuntimeOwnerResourceSchemeV118RequestObserved;
        private boolean productionRuntimeOwnerResourceSchemeV118RootStageObserved;
        private boolean productionRuntimeOwnerResourceSchemeV118DependencyStageObserved;
        private boolean productionRuntimeOwnerResourceSchemeV118RootHandlerRequestObserved;
        private boolean productionRuntimeOwnerResourceSchemeV118RootHandlerResponseObserved;
        private boolean productionRuntimeOwnerResourceSchemeV118DependencyHandlerRequestObserved;
        private boolean productionRuntimeOwnerResourceSchemeV118DependencyHandlerResponseObserved;
        private boolean productionRuntimeOwnerResourceSchemeV118ImportObserved;
        private boolean productionRuntimeOwnerResourceSchemeV118DomObserved;
        private boolean productionRuntimeOwnerSideEffectV119ManifestObserved;
        private boolean productionRuntimeOwnerSideEffectV119RequestObserved;
        private boolean productionRuntimeOwnerSideEffectV119StageObserved;
        private boolean productionRuntimeOwnerSideEffectV119DomObserved;
        private boolean productionRuntimeOwnerSideEffectV119ReuseDecisionObserved;
        private boolean productionRuntimeOwnerOwnedRuntimeV120ManifestObserved;
        private boolean productionRuntimeOwnerOwnedRuntimeV120RequestObserved;
        private boolean productionRuntimeOwnerOwnedRuntimeV120StageObserved;
        private boolean productionRuntimeOwnerOwnedRuntimeV120DomObserved;
        private boolean productionRuntimeOwnerOwnedRuntimeV120ReuseDecisionObserved;
        private boolean frontendResolverContractV121ManifestObserved;
        private boolean frontendResolverContractV121RequestObserved;
        private boolean frontendResolverContractV121ResponseObserved;
        private boolean frontendResolverContractV121StageObserved;
        private boolean frontendResolverContractV121BlobStageObserved;
        private boolean frontendResolverContractV121CustomSchemeStageObserved;
        private boolean frontendResolverContractV121StrictOrGapStageObserved;
        private boolean frontendResolverContractV121DomObserved;
        private boolean generatedChunkFileFixtureV122RequestObserved;
        private boolean generatedChunkFileFixtureV122ImportsResponseObserved;
        private boolean generatedChunkFileFixtureV122ChunkResponseObserved;
        private boolean generatedChunkFileFixtureV122StageObserved;
        private boolean generatedChunkFileFixtureV122DomObserved;
        private boolean productionRuntimeOwnerPositiveFixtureV123ManifestObserved;
        private boolean productionRuntimeOwnerPositiveFixtureV123RootRequestObserved;
        private boolean productionRuntimeOwnerPositiveFixtureV123DependencyRequestObserved;
        private boolean productionRuntimeOwnerPositiveFixtureV123RootResponseObserved;
        private boolean productionRuntimeOwnerPositiveFixtureV123DependencyResponseObserved;
        private boolean productionRuntimeOwnerPositiveFixtureV123RootStageObserved;
        private boolean productionRuntimeOwnerPositiveFixtureV123DependencyStageObserved;
        private boolean productionRuntimeOwnerPositiveFixtureV123DomObserved;
        private boolean productionRuntimeOwnerPositiveFixtureV123RealBlockerObserved;
        private boolean frontendPipelineClosureV124ManifestObserved;
        private boolean frontendPipelineClosureV124RequestObserved;
        private boolean frontendPipelineClosureV124ResponseObserved;
        private boolean frontendPipelineClosureV124StageObserved;
        private boolean frontendPipelineClosureV124DomObserved;
        private boolean frontendPipelineClosureV124FixtureProofsObserved;
        private boolean frontendPipelineClosureV124RealBlockersObserved;
        private boolean packagedFrontendBaselineV125ManifestObserved;
        private boolean packagedFrontendBaselineV125RequestObserved;
        private boolean packagedFrontendBaselineV125ResponseObserved;
        private boolean packagedFrontendBaselineV125StageObserved;
        private boolean packagedFrontendBaselineV125CustomSchemeStageObserved;
        private boolean packagedFrontendBaselineV125DiagnosticBlobFallbackObserved;
        private boolean packagedFrontendBaselineV125DomObserved;
        private boolean packagedFrontendBaselineV125CustomSchemeDomObserved;
        private boolean packagedFrontendBaselineV125DiagnosticBlobFallbackDomObserved;
        private boolean packagedFrontendBaselineV125RealBlockersObserved;
        private boolean generatedLazySourceBoundaryV126ManifestObserved;
        private boolean generatedLazySourceBoundaryV126RequestObserved;
        private boolean generatedLazySourceBoundaryV126ResponseObserved;
        private boolean generatedLazySourceBoundaryV126StageObserved;
        private boolean generatedLazySourceBoundaryV126DomObserved;
        private boolean generatedLazySourceBoundaryV126CurrentBuildBlockerObserved;
        private boolean generatedLazyRealBuildProbeV127ManifestObserved;
        private boolean generatedLazyRealBuildProbeV127RequestObserved;
        private boolean generatedLazyRealBuildProbeV127ResponseObserved;
        private boolean generatedLazyRealBuildProbeV127StageObserved;
        private boolean generatedLazyRealBuildProbeV127DomObserved;
        private boolean generatedLazyRealBuildProbeV127CurrentBuildBlockerObserved;
        private boolean generatedLazyBuildLifecycleV128ManifestObserved;
        private boolean generatedLazyBuildLifecycleV128RequestObserved;
        private boolean generatedLazyBuildLifecycleV128ResponseObserved;
        private boolean generatedLazyBuildLifecycleV128StageObserved;
        private boolean generatedLazyBuildLifecycleV128DomObserved;
        private boolean generatedLazyBuildLifecycleV128CurrentBuildBlockerObserved;

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
                    && (productionRuntimeOwnerImportV114StageObserved
                            || productionRuntimeOwnerResourceSchemeV118ImportObserved)
                    && (productionRuntimeOwnerImportV114DomObserved
                            || productionRuntimeOwnerResourceSchemeV118DomObserved);
        }

        public boolean hasRequiredFlowIpcV1_14Types() {
            return hasRequiredFlowIpcV1_13Types()
                    && hasRequiredFlowIpcV1_14ProductionOwnerImportTypes();
        }

        public boolean hasRequiredFlowIpcV1_15ProductionOwnerDependencyTypes() {
            return productionRuntimeOwnerDependenciesV115ManifestObserved
                    && productionRuntimeOwnerDependenciesV115RequestObserved
                    && (productionRuntimeOwnerDependenciesV115StageObserved
                            || productionRuntimeOwnerResourceSchemeV118DependencyStageObserved)
                    && (productionRuntimeOwnerDependenciesV115DomObserved
                            || (productionRuntimeOwnerResourceSchemeV118DependencyHandlerRequestObserved
                                    && productionRuntimeOwnerResourceSchemeV118DependencyHandlerResponseObserved
                                    && productionRuntimeOwnerResourceSchemeV118DomObserved));
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

        public boolean hasRequiredFlowIpcV1_17ResourceSchemeTypes() {
            return frontendResourceSchemeV117ManifestObserved
                    && frontendResourceSchemeV117HostInstallObserved
                    && frontendResourceSchemeV117StageObserved
                    && frontendResourceSchemeV117HandlerRequestObserved
                    && frontendResourceSchemeV117HandlerResponseObserved
                    && frontendResourceSchemeV117ExecutedObserved
                    && frontendResourceSchemeV117DomObserved;
        }

        public boolean hasRequiredFlowIpcV1_17Types() {
            return hasRequiredFlowIpcV1_16Types()
                    && hasRequiredFlowIpcV1_17ResourceSchemeTypes();
        }

        public boolean hasRequiredFlowIpcV1_18ProductionOwnerResourceSchemeTypes() {
            return productionRuntimeOwnerResourceSchemeV118ManifestObserved
                    && productionRuntimeOwnerResourceSchemeV118RequestObserved
                    && productionRuntimeOwnerResourceSchemeV118RootStageObserved
                    && productionRuntimeOwnerResourceSchemeV118DependencyStageObserved
                    && productionRuntimeOwnerResourceSchemeV118RootHandlerRequestObserved
                    && productionRuntimeOwnerResourceSchemeV118RootHandlerResponseObserved
                    && productionRuntimeOwnerResourceSchemeV118DependencyHandlerRequestObserved
                    && productionRuntimeOwnerResourceSchemeV118DependencyHandlerResponseObserved
                    && productionRuntimeOwnerResourceSchemeV118ImportObserved
                    && productionRuntimeOwnerResourceSchemeV118DomObserved;
        }

        public boolean hasRequiredFlowIpcV1_18Types() {
            return hasRequiredFlowIpcV1_17Types()
                    && hasRequiredFlowIpcV1_18ProductionOwnerResourceSchemeTypes();
        }

        public boolean hasRequiredFlowIpcV1_19ProductionOwnerSideEffectTypes() {
            return hasRequiredFlowIpcV1_18ProductionOwnerResourceSchemeTypes()
                    && productionRuntimeOwnerSideEffectV119ManifestObserved
                    && productionRuntimeOwnerSideEffectV119RequestObserved
                    && productionRuntimeOwnerSideEffectV119StageObserved
                    && productionRuntimeOwnerSideEffectV119DomObserved
                    && productionRuntimeOwnerSideEffectV119ReuseDecisionObserved;
        }

        public boolean hasRequiredFlowIpcV1_19Types() {
            return hasRequiredFlowIpcV1_18Types()
                    && hasRequiredFlowIpcV1_19ProductionOwnerSideEffectTypes();
        }

        public boolean hasRequiredFlowIpcV1_20ProductionOwnerOwnedRuntimeTypes() {
            return productionRuntimeOwnerOwnedRuntimeV120ManifestObserved
                    && productionRuntimeOwnerOwnedRuntimeV120RequestObserved
                    && productionRuntimeOwnerOwnedRuntimeV120StageObserved
                    && productionRuntimeOwnerOwnedRuntimeV120DomObserved
                    && productionRuntimeOwnerOwnedRuntimeV120ReuseDecisionObserved;
        }

        public boolean hasRequiredFlowIpcV1_20Types() {
            return hasRequiredFlowIpcV1_19Types()
                    && hasRequiredFlowIpcV1_20ProductionOwnerOwnedRuntimeTypes();
        }

        public boolean hasRequiredFlowIpcV1_21FrontendResolverContractTypes() {
            return frontendResolverContractV121ManifestObserved
                    && frontendResolverContractV121RequestObserved
                    && frontendResolverContractV121ResponseObserved
                    && frontendResolverContractV121StageObserved
                    && frontendResolverContractV121BlobStageObserved
                    && frontendResolverContractV121CustomSchemeStageObserved
                    && frontendResolverContractV121StrictOrGapStageObserved
                    && frontendResolverContractV121DomObserved;
        }

        public boolean hasRequiredFlowIpcV1_21Types() {
            return hasRequiredFlowIpcV1_20Types()
                    && hasRequiredFlowIpcV1_21FrontendResolverContractTypes();
        }

        public boolean hasRequiredFlowIpcV1_22GeneratedChunkFixtureTypes() {
            return hasRequiredFlowIpcV1_12LoadOnDemandChunkFileTypes()
                    && generatedChunkFileFixtureV122RequestObserved
                    && generatedChunkFileFixtureV122ImportsResponseObserved
                    && generatedChunkFileFixtureV122ChunkResponseObserved
                    && generatedChunkFileFixtureV122StageObserved
                    && generatedChunkFileFixtureV122DomObserved;
        }

        public boolean hasRequiredFlowIpcV1_22Types() {
            return hasRequiredFlowIpcV1_21Types()
                    && hasRequiredFlowIpcV1_22GeneratedChunkFixtureTypes();
        }

        public boolean hasRequiredFlowIpcV1_23ProductionOwnerPositiveFixtureTypes() {
            return hasRequiredFlowIpcV1_20ProductionOwnerOwnedRuntimeTypes()
                    && productionRuntimeOwnerPositiveFixtureV123ManifestObserved
                    && productionRuntimeOwnerPositiveFixtureV123RootRequestObserved
                    && productionRuntimeOwnerPositiveFixtureV123DependencyRequestObserved
                    && productionRuntimeOwnerPositiveFixtureV123RootResponseObserved
                    && productionRuntimeOwnerPositiveFixtureV123DependencyResponseObserved
                    && productionRuntimeOwnerPositiveFixtureV123RootStageObserved
                    && productionRuntimeOwnerPositiveFixtureV123DependencyStageObserved
                    && productionRuntimeOwnerPositiveFixtureV123DomObserved
                    && productionRuntimeOwnerPositiveFixtureV123RealBlockerObserved;
        }

        public boolean hasRequiredFlowIpcV1_23Types() {
            return hasRequiredFlowIpcV1_22Types()
                    && hasRequiredFlowIpcV1_23ProductionOwnerPositiveFixtureTypes();
        }

        public boolean hasRequiredFlowIpcV1_24FrontendPipelineClosureTypes() {
            return frontendPipelineClosureV124ManifestObserved
                    && frontendPipelineClosureV124RequestObserved
                    && frontendPipelineClosureV124ResponseObserved
                    && frontendPipelineClosureV124StageObserved
                    && frontendPipelineClosureV124DomObserved
                    && frontendPipelineClosureV124FixtureProofsObserved
                    && frontendPipelineClosureV124RealBlockersObserved;
        }

        public boolean hasRequiredFlowIpcV1_24Types() {
            return hasRequiredFlowIpcV1_23Types()
                    && hasRequiredFlowIpcV1_24FrontendPipelineClosureTypes();
        }

        public boolean hasRequiredFlowIpcV1_25PackagedFrontendBaselineTypes() {
            return packagedFrontendBaselineV125ManifestObserved
                    && packagedFrontendBaselineV125RequestObserved
                    && packagedFrontendBaselineV125ResponseObserved
                    && packagedFrontendBaselineV125StageObserved
                    && packagedFrontendBaselineV125CustomSchemeStageObserved
                    && packagedFrontendBaselineV125DiagnosticBlobFallbackObserved
                    && packagedFrontendBaselineV125DomObserved
                    && packagedFrontendBaselineV125CustomSchemeDomObserved
                    && packagedFrontendBaselineV125DiagnosticBlobFallbackDomObserved
                    && packagedFrontendBaselineV125RealBlockersObserved;
        }

        public boolean hasRequiredFlowIpcV1_25Types() {
            return hasRequiredFlowIpcV1_24Types()
                    && hasRequiredFlowIpcV1_25PackagedFrontendBaselineTypes();
        }

        public boolean hasRequiredFlowIpcV1_26GeneratedLazySourceBoundaryTypes() {
            return generatedLazySourceBoundaryV126ManifestObserved
                    && generatedLazySourceBoundaryV126RequestObserved
                    && generatedLazySourceBoundaryV126ResponseObserved
                    && generatedLazySourceBoundaryV126StageObserved
                    && generatedLazySourceBoundaryV126DomObserved
                    && generatedLazySourceBoundaryV126CurrentBuildBlockerObserved;
        }

        public boolean hasRequiredFlowIpcV1_26Types() {
            return hasRequiredFlowIpcV1_25Types()
                    && hasRequiredFlowIpcV1_26GeneratedLazySourceBoundaryTypes();
        }

        public boolean hasRequiredFlowIpcV1_27GeneratedLazyRealBuildProbeTypes() {
            return generatedLazyRealBuildProbeV127ManifestObserved
                    && generatedLazyRealBuildProbeV127RequestObserved
                    && generatedLazyRealBuildProbeV127ResponseObserved
                    && generatedLazyRealBuildProbeV127StageObserved
                    && generatedLazyRealBuildProbeV127DomObserved
                    && generatedLazyRealBuildProbeV127CurrentBuildBlockerObserved;
        }

        public boolean hasRequiredFlowIpcV1_27Types() {
            return hasRequiredFlowIpcV1_26Types()
                    && hasRequiredFlowIpcV1_27GeneratedLazyRealBuildProbeTypes();
        }

        public boolean hasRequiredFlowIpcV1_28GeneratedLazyBuildLifecycleTypes() {
            return generatedLazyBuildLifecycleV128ManifestObserved
                    && generatedLazyBuildLifecycleV128RequestObserved
                    && generatedLazyBuildLifecycleV128ResponseObserved
                    && generatedLazyBuildLifecycleV128StageObserved
                    && generatedLazyBuildLifecycleV128DomObserved
                    && generatedLazyBuildLifecycleV128CurrentBuildBlockerObserved;
        }

        public boolean hasRequiredFlowIpcV1_28Types() {
            return hasRequiredFlowIpcV1_27Types()
                    && hasRequiredFlowIpcV1_28GeneratedLazyBuildLifecycleTypes();
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
            System.out.println("Flow IPC v1.17 resource scheme manifest observed: "
                    + frontendResourceSchemeV117ManifestObserved);
            System.out.println("Flow IPC v1.17 resource scheme host install observed: "
                    + frontendResourceSchemeV117HostInstallObserved);
            System.out.println("Flow IPC v1.17 resource scheme stage observed: "
                    + frontendResourceSchemeV117StageObserved);
            System.out.println("Flow IPC v1.17 resource scheme handler request/response observed: "
                    + frontendResourceSchemeV117HandlerRequestObserved + "/"
                    + frontendResourceSchemeV117HandlerResponseObserved);
            System.out.println("Flow IPC v1.17 resource scheme executed DOM observed: "
                    + frontendResourceSchemeV117ExecutedObserved + "/"
                    + frontendResourceSchemeV117DomObserved);
            System.out.println("Flow IPC v1.17 resource scheme fallback classification observed: "
                    + frontendResourceSchemeV117FallbackClassifiedObserved);
            System.out.println("Flow IPC v1.18 production owner resource scheme manifest/request observed: "
                    + productionRuntimeOwnerResourceSchemeV118ManifestObserved + "/"
                    + productionRuntimeOwnerResourceSchemeV118RequestObserved);
            System.out.println("Flow IPC v1.18 production owner resource scheme root stage observed: "
                    + productionRuntimeOwnerResourceSchemeV118RootStageObserved);
            System.out.println("Flow IPC v1.18 production owner resource scheme dependency stage observed: "
                    + productionRuntimeOwnerResourceSchemeV118DependencyStageObserved);
            System.out.println("Flow IPC v1.18 production owner resource scheme root handler request/response observed: "
                    + productionRuntimeOwnerResourceSchemeV118RootHandlerRequestObserved + "/"
                    + productionRuntimeOwnerResourceSchemeV118RootHandlerResponseObserved);
            System.out.println("Flow IPC v1.18 production owner resource scheme dependency handler request/response observed: "
                    + productionRuntimeOwnerResourceSchemeV118DependencyHandlerRequestObserved + "/"
                    + productionRuntimeOwnerResourceSchemeV118DependencyHandlerResponseObserved);
            System.out.println("Flow IPC v1.18 production owner resource scheme import/DOM observed: "
                    + productionRuntimeOwnerResourceSchemeV118ImportObserved + "/"
                    + productionRuntimeOwnerResourceSchemeV118DomObserved);
            System.out.println("Flow IPC v1.19 production owner side-effect manifest/request observed: "
                    + productionRuntimeOwnerSideEffectV119ManifestObserved + "/"
                    + productionRuntimeOwnerSideEffectV119RequestObserved);
            System.out.println("Flow IPC v1.19 production owner side-effect stage/DOM observed: "
                    + productionRuntimeOwnerSideEffectV119StageObserved + "/"
                    + productionRuntimeOwnerSideEffectV119DomObserved);
            System.out.println("Flow IPC v1.19 component owner reuse decision observed: "
                    + productionRuntimeOwnerSideEffectV119ReuseDecisionObserved);
            System.out.println("Flow IPC v1.20 production owner-owned runtime manifest/request observed: "
                    + productionRuntimeOwnerOwnedRuntimeV120ManifestObserved + "/"
                    + productionRuntimeOwnerOwnedRuntimeV120RequestObserved);
            System.out.println("Flow IPC v1.20 production owner-owned runtime stage/DOM observed: "
                    + productionRuntimeOwnerOwnedRuntimeV120StageObserved + "/"
                    + productionRuntimeOwnerOwnedRuntimeV120DomObserved);
            System.out.println("Flow IPC v1.20 component owner reuse decision observed: "
                    + productionRuntimeOwnerOwnedRuntimeV120ReuseDecisionObserved);
            System.out.println("Flow IPC v1.21 frontend resolver contract manifest/request/response observed: "
                    + frontendResolverContractV121ManifestObserved + "/"
                    + frontendResolverContractV121RequestObserved + "/"
                    + frontendResolverContractV121ResponseObserved);
            System.out.println("Flow IPC v1.21 frontend resolver contract stage blob/custom/gap observed: "
                    + frontendResolverContractV121BlobStageObserved + "/"
                    + frontendResolverContractV121CustomSchemeStageObserved + "/"
                    + frontendResolverContractV121StrictOrGapStageObserved);
            System.out.println("Flow IPC v1.21 frontend resolver contract DOM observed: "
                    + frontendResolverContractV121DomObserved);
            System.out.println("Flow IPC v1.22 generated chunk fixture request observed: "
                    + generatedChunkFileFixtureV122RequestObserved);
            System.out.println("Flow IPC v1.22 generated chunk fixture imports/chunk response observed: "
                    + generatedChunkFileFixtureV122ImportsResponseObserved + "/"
                    + generatedChunkFileFixtureV122ChunkResponseObserved);
            System.out.println("Flow IPC v1.22 generated chunk fixture stage/DOM observed: "
                    + generatedChunkFileFixtureV122StageObserved + "/"
                    + generatedChunkFileFixtureV122DomObserved);
            System.out.println("Flow IPC v1.23 production owner positive fixture manifest observed: "
                    + productionRuntimeOwnerPositiveFixtureV123ManifestObserved);
            System.out.println("Flow IPC v1.23 production owner positive fixture request root/dependency observed: "
                    + productionRuntimeOwnerPositiveFixtureV123RootRequestObserved + "/"
                    + productionRuntimeOwnerPositiveFixtureV123DependencyRequestObserved);
            System.out.println("Flow IPC v1.23 production owner positive fixture response root/dependency observed: "
                    + productionRuntimeOwnerPositiveFixtureV123RootResponseObserved + "/"
                    + productionRuntimeOwnerPositiveFixtureV123DependencyResponseObserved);
            System.out.println("Flow IPC v1.23 production owner positive fixture stage root/dependency observed: "
                    + productionRuntimeOwnerPositiveFixtureV123RootStageObserved + "/"
                    + productionRuntimeOwnerPositiveFixtureV123DependencyStageObserved);
            System.out.println("Flow IPC v1.23 production owner positive fixture DOM/real blocker observed: "
                    + productionRuntimeOwnerPositiveFixtureV123DomObserved + "/"
                    + productionRuntimeOwnerPositiveFixtureV123RealBlockerObserved);
            System.out.println("Flow IPC v1.24 frontend pipeline closure manifest observed: "
                    + frontendPipelineClosureV124ManifestObserved);
            System.out.println("Flow IPC v1.24 frontend pipeline closure request/response observed: "
                    + frontendPipelineClosureV124RequestObserved + "/"
                    + frontendPipelineClosureV124ResponseObserved);
            System.out.println("Flow IPC v1.24 frontend pipeline closure stage/DOM observed: "
                    + frontendPipelineClosureV124StageObserved + "/"
                    + frontendPipelineClosureV124DomObserved);
            System.out.println("Flow IPC v1.24 frontend pipeline closure fixture proofs/real blockers observed: "
                    + frontendPipelineClosureV124FixtureProofsObserved + "/"
                    + frontendPipelineClosureV124RealBlockersObserved);
            System.out.println("Flow IPC v1.25 packaged frontend baseline manifest observed: "
                    + packagedFrontendBaselineV125ManifestObserved);
            System.out.println("Flow IPC v1.25 packaged frontend baseline request/response observed: "
                    + packagedFrontendBaselineV125RequestObserved + "/"
                    + packagedFrontendBaselineV125ResponseObserved);
            System.out.println("Flow IPC v1.25 packaged frontend baseline stage/custom-scheme/fallback observed: "
                    + packagedFrontendBaselineV125StageObserved + "/"
                    + packagedFrontendBaselineV125CustomSchemeStageObserved + "/"
                    + packagedFrontendBaselineV125DiagnosticBlobFallbackObserved);
            System.out.println("Flow IPC v1.25 packaged frontend baseline DOM/custom-scheme/fallback/real blockers observed: "
                    + packagedFrontendBaselineV125DomObserved + "/"
                    + packagedFrontendBaselineV125CustomSchemeDomObserved + "/"
                    + packagedFrontendBaselineV125DiagnosticBlobFallbackDomObserved + "/"
                    + packagedFrontendBaselineV125RealBlockersObserved);
            System.out.println("Flow IPC v1.26 generated lazy source boundary manifest observed: "
                    + generatedLazySourceBoundaryV126ManifestObserved);
            System.out.println("Flow IPC v1.26 generated lazy source boundary request/response observed: "
                    + generatedLazySourceBoundaryV126RequestObserved + "/"
                    + generatedLazySourceBoundaryV126ResponseObserved);
            System.out.println("Flow IPC v1.26 generated lazy source boundary stage/DOM/current blocker observed: "
                    + generatedLazySourceBoundaryV126StageObserved + "/"
                    + generatedLazySourceBoundaryV126DomObserved + "/"
                    + generatedLazySourceBoundaryV126CurrentBuildBlockerObserved);
            System.out.println("Flow IPC v1.27 generated lazy real build probe manifest observed: "
                    + generatedLazyRealBuildProbeV127ManifestObserved);
            System.out.println("Flow IPC v1.27 generated lazy real build probe request/response observed: "
                    + generatedLazyRealBuildProbeV127RequestObserved + "/"
                    + generatedLazyRealBuildProbeV127ResponseObserved);
            System.out.println("Flow IPC v1.27 generated lazy real build probe stage/DOM/current blocker observed: "
                    + generatedLazyRealBuildProbeV127StageObserved + "/"
                    + generatedLazyRealBuildProbeV127DomObserved + "/"
                    + generatedLazyRealBuildProbeV127CurrentBuildBlockerObserved);
            System.out.println("Flow IPC v1.28 generated lazy build lifecycle manifest observed: "
                    + generatedLazyBuildLifecycleV128ManifestObserved);
            System.out.println("Flow IPC v1.28 generated lazy build lifecycle request/response observed: "
                    + generatedLazyBuildLifecycleV128RequestObserved + "/"
                    + generatedLazyBuildLifecycleV128ResponseObserved);
            System.out.println("Flow IPC v1.28 generated lazy build lifecycle stage/DOM/current blocker observed: "
                    + generatedLazyBuildLifecycleV128StageObserved + "/"
                    + generatedLazyBuildLifecycleV128DomObserved + "/"
                    + generatedLazyBuildLifecycleV128CurrentBuildBlockerObserved);
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
