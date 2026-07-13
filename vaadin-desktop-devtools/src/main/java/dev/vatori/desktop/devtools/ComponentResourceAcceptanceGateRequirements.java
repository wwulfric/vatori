package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.AcceptanceGate.*;
import static dev.vatori.desktop.devtools.AcceptanceGateExpansion.*;

import java.util.EnumSet;

final class ComponentResourceAcceptanceGateRequirements {
    private ComponentResourceAcceptanceGateRequirements() {
    }

    static EnumSet<AcceptanceGate> expand(AcceptanceGate gate) {
        return switch (gate) {
            case FLOW_IPC_V1_46_BROADER_COMPONENT_RESOURCE_MATRIX ->
                single(FLOW_IPC_V1_46_BROADER_COMPONENT_RESOURCE_MATRIX);
            case FLOW_IPC_V1_46 -> pair(
                    FLOW_IPC_V1_46_BROADER_COMPONENT_RESOURCE_MATRIX, FLOW_IPC_V1_46);
            case FLOW_IPC_V1_47_DATA_COMPONENT_RESOURCE_MATRIX ->
                single(FLOW_IPC_V1_47_DATA_COMPONENT_RESOURCE_MATRIX);
            case FLOW_IPC_V1_47 -> pair(
                    FLOW_IPC_V1_47_DATA_COMPONENT_RESOURCE_MATRIX, FLOW_IPC_V1_47);
            case FLOW_IPC_V1_48_GRID_LAZY_DATA_PROVIDER ->
                single(FLOW_IPC_V1_48_GRID_LAZY_DATA_PROVIDER);
            case FLOW_IPC_V1_48 -> pair(
                    FLOW_IPC_V1_48_GRID_LAZY_DATA_PROVIDER, FLOW_IPC_V1_48);
            case FLOW_IPC_V1_49_GRID_SCROLL_VIRTUALIZATION ->
                single(FLOW_IPC_V1_49_GRID_SCROLL_VIRTUALIZATION);
            case FLOW_IPC_V1_49 -> pair(
                    FLOW_IPC_V1_49_GRID_SCROLL_VIRTUALIZATION, FLOW_IPC_V1_49);
            case FLOW_IPC_V1_50_STREAM_RESOURCE ->
                single(FLOW_IPC_V1_50_STREAM_RESOURCE);
            case FLOW_IPC_V1_50 -> pair(FLOW_IPC_V1_50_STREAM_RESOURCE, FLOW_IPC_V1_50);
            case FLOW_IPC_V1_51_STREAM_RESOURCE_RANGE ->
                single(FLOW_IPC_V1_51_STREAM_RESOURCE_RANGE);
            case FLOW_IPC_V1_51 -> pair(
                    FLOW_IPC_V1_51_STREAM_RESOURCE_RANGE, FLOW_IPC_V1_51);
            case FLOW_IPC_V1_52_STREAM_RESOURCE_CHUNKS ->
                single(FLOW_IPC_V1_52_STREAM_RESOURCE_CHUNKS);
            case FLOW_IPC_V1_52 -> pair(
                    FLOW_IPC_V1_52_STREAM_RESOURCE_CHUNKS, FLOW_IPC_V1_52);
            case FLOW_IPC_V1_53_UPLOAD_RECEIVER ->
                single(FLOW_IPC_V1_53_UPLOAD_RECEIVER);
            case FLOW_IPC_V1_53 -> pair(FLOW_IPC_V1_53_UPLOAD_RECEIVER, FLOW_IPC_V1_53);
            case FLOW_IPC_V1_54_UPLOAD_MULTIPART ->
                single(FLOW_IPC_V1_54_UPLOAD_MULTIPART);
            case FLOW_IPC_V1_54 -> pair(FLOW_IPC_V1_54_UPLOAD_MULTIPART, FLOW_IPC_V1_54);
            case FLOW_IPC_V1_55_DOWNLOAD_SAVE_OPEN ->
                single(FLOW_IPC_V1_55_DOWNLOAD_SAVE_OPEN);
            case FLOW_IPC_V1_55 -> pair(FLOW_IPC_V1_55_DOWNLOAD_SAVE_OPEN, FLOW_IPC_V1_55);
            case FLOW_IPC_V1_56_FRONTEND_RESOURCE_PIPELINE_BASIC_CLOSURE ->
                single(FLOW_IPC_V1_56_FRONTEND_RESOURCE_PIPELINE_BASIC_CLOSURE);
            case FLOW_IPC_V1_56 -> pair(
                    FLOW_IPC_V1_56_FRONTEND_RESOURCE_PIPELINE_BASIC_CLOSURE,
                    FLOW_IPC_V1_56);
            case FLOW_IPC_V1_57_INPUT_COMPONENT_PARITY_MATRIX ->
                single(FLOW_IPC_V1_57_INPUT_COMPONENT_PARITY_MATRIX);
            case FLOW_IPC_V1_57 -> pair(
                    FLOW_IPC_V1_57_INPUT_COMPONENT_PARITY_MATRIX, FLOW_IPC_V1_57);
            case FLOW_IPC_V1_58_MULTI_VALUE_NAVIGATION_DISCLOSURE_MATRIX ->
                single(FLOW_IPC_V1_58_MULTI_VALUE_NAVIGATION_DISCLOSURE_MATRIX);
            case FLOW_IPC_V1_58 -> pair(
                    FLOW_IPC_V1_58_MULTI_VALUE_NAVIGATION_DISCLOSURE_MATRIX,
                    FLOW_IPC_V1_58);
            case FLOW_IPC_V1_59_OVERLAY_CONTEXT_MENU_MATRIX ->
                single(FLOW_IPC_V1_59_OVERLAY_CONTEXT_MENU_MATRIX);
            case FLOW_IPC_V1_59 -> pair(
                    FLOW_IPC_V1_59_OVERLAY_CONTEXT_MENU_MATRIX, FLOW_IPC_V1_59);
            case FLOW_IPC_V1_60_CONTENT_APP_SHELL_LOGIN_MATRIX ->
                single(FLOW_IPC_V1_60_CONTENT_APP_SHELL_LOGIN_MATRIX);
            case FLOW_IPC_V1_60 -> pair(
                    FLOW_IPC_V1_60_CONTENT_APP_SHELL_LOGIN_MATRIX, FLOW_IPC_V1_60);
            case FLOW_IPC_V1_61_LIFECYCLE_FOCUS_BEHAVIOR_MATRIX ->
                single(FLOW_IPC_V1_61_LIFECYCLE_FOCUS_BEHAVIOR_MATRIX);
            case FLOW_IPC_V1_61 -> pair(
                    FLOW_IPC_V1_61_LIFECYCLE_FOCUS_BEHAVIOR_MATRIX, FLOW_IPC_V1_61);
            case FLOW_IPC_V1_62_RICH_TEXT_EDITOR_MATRIX ->
                single(FLOW_IPC_V1_62_RICH_TEXT_EDITOR_MATRIX);
            case FLOW_IPC_V1_62 -> pair(
                    FLOW_IPC_V1_62_RICH_TEXT_EDITOR_MATRIX, FLOW_IPC_V1_62);
            case FLOW_IPC_V1_63_RICH_TEXT_EDITOR_BEHAVIOR_MATRIX ->
                single(FLOW_IPC_V1_63_RICH_TEXT_EDITOR_BEHAVIOR_MATRIX);
            case FLOW_IPC_V1_63 -> pair(
                    FLOW_IPC_V1_63_RICH_TEXT_EDITOR_BEHAVIOR_MATRIX, FLOW_IPC_V1_63);
            case FLOW_IPC_V1_64_RICH_TEXT_EDITOR_EDITING_MATRIX ->
                single(FLOW_IPC_V1_64_RICH_TEXT_EDITOR_EDITING_MATRIX);
            case FLOW_IPC_V1_64 -> pair(
                    FLOW_IPC_V1_64_RICH_TEXT_EDITOR_EDITING_MATRIX, FLOW_IPC_V1_64);
            case FLOW_IPC_V1_65_NATIVE_CLIPBOARD_READ_MATRIX ->
                single(FLOW_IPC_V1_65_NATIVE_CLIPBOARD_READ_MATRIX);
            case FLOW_IPC_V1_65 -> pair(
                    FLOW_IPC_V1_65_NATIVE_CLIPBOARD_READ_MATRIX, FLOW_IPC_V1_65);
            default -> throw new IllegalArgumentException("Unsupported component/resource gate: " + gate);
        };
    }
}
