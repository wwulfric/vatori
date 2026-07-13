package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.GridResourceTraceFields.*;
import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class RichTextEditorMatrixTraceInspector {
    private RichTextEditorMatrixTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("dev.vatori.ipcframework.FlowOverIpcFrameworkView")
                && line.contains("framework-rich-text-editor")
                && line.contains("framework-rich-text-editor-status")
                && line.contains("com.vaadin.flow.component.richtexteditor.RichTextEditor")) {
            summary.frameworkRichTextEditor.matrixBootstrapObserved = true;
        }
        if (line.contains("\"type\":\"event\"")
                && line.contains("\"dispatch\":\"flow-client-xhr\"")
                && line.contains("\"componentId\":\"framework-rich-text-editor\"")
                && line.contains("\"event\":\"value-changed\"")
                && line.contains("Flow IPC rich text v1.62")) {
            summary.frameworkRichTextEditor.matrixValueEventRpcObserved = true;
        }
        if (line.contains("\"type\":\"patch\"")
                && line.contains("\"source\":\"UidlRequestHandler\"")
                && line.contains("\"reason\":\"event\"")
                && line.contains("Rich text changed: Flow IPC rich text v1.62")) {
            summary.frameworkRichTextEditor.matrixValuePatchObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && containsRichTextEditorMatrixV162ManifestFields(line)) {
            summary.frameworkRichTextEditor.matrixV162ManifestObserved = true;
        }
        if (line.contains("\"type\":\"resourceStage\"")
                && containsCurrentAppProductionMatrixRuntimePath(line)
                && containsRichTextEditorMatrixV162Fields(line)
                && (line.contains("\"executionStatus\":\"executed\"")
                        || line.contains("\"executionStatus\":\"strict-blocker\"")
                        || line.contains("\"executionStatus\":\"staged\""))) {
            summary.frameworkRichTextEditor.matrixV162StageObserved = true;
        }
        if (line.contains("\"type\":\"resourceStage\"")
                && containsCurrentAppProductionMatrixRuntimePath(line)
                && containsRichTextEditorMatrixV162Complete(line)
                && line.contains("\"executionStatus\":\"executed\"")) {
            summary.frameworkRichTextEditor.matrixV162CompleteStageObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && line.contains("Flow IPC Framework Desk")
                && containsLiteralField(line,
                        "frameworkRichTextEditorMatrixRendered", "true")
                && containsLiteralField(line,
                        "frameworkRichTextEditorMatrixEditorRendered", "true")
                && containsLiteralField(line,
                        "frameworkRichTextEditorMatrixValueEventRpcObserved", "true")
                && containsLiteralField(line,
                        "frameworkRichTextEditorMatrixValueEventObserved", "true")
                && containsLiteralField(line,
                        "frameworkRichTextEditorMatrixComplete", "true")
                && containsStringField(line,
                        "frameworkRichTextEditorMatrixVersion",
                        "flow-ipc-rich-text-editor-matrix-v1_62")
                && line.contains("framework-rich-text-editor")) {
            summary.frameworkRichTextEditor.matrixDomObserved = true;
            summary.frameworkRichTextEditor.matrixV162DomObserved = true;
        }
    }

    private static boolean containsRichTextEditorMatrixV162Fields(String line) {
        return containsStringField(line, "richTextEditorMatrixVersion",
                        "flow-ipc-rich-text-editor-matrix-v1_62")
                && containsStringField(line, "richTextEditorMatrixSource",
                        "lifecycle-focus-behavior-matrix-v1_61")
                && containsStringField(line, "richTextEditorMatrixScope",
                        "framework-validation-app-rich-text-editor")
                && containsStringField(line, "richTextEditorMatrixAppShape",
                        "framework-validation-app")
                && containsStringField(line, "richTextEditorMatrixTargetTags",
                        "vaadin-rich-text-editor")
                && containsStringField(line, "richTextEditorMatrixJavaClasses",
                        "com.vaadin.flow.component.richtexteditor.RichTextEditor")
                && containsStringField(line, "richTextEditorMatrixComponentIds",
                        "framework-rich-text-editor,framework-rich-text-editor-status")
                && containsStringField(line, "richTextEditorMatrixModuleSpecifiers",
                        "@vaadin/rich-text-editor/src/vaadin-rich-text-editor.js")
                && containsStringFieldPrefix(line, "richTextEditorMatrixSelectedArtifacts",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && containsPositiveIntegerField(line,
                        "richTextEditorMatrixSelectedArtifactCount")
                && containsStringField(line,
                        "richTextEditorMatrixGeneratedImportsOwnerSource",
                        "current-app-generated-imports-select-owner-coverage-v1_41")
                && containsStringField(line,
                        "richTextEditorMatrixGeneratedImportsOwnerArtifact",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && line.contains("richTextEditorMatrixProductionSupportArtifact")
                && line.contains("richTextEditorMatrixProductionSupportSha256")
                && line.contains("richTextEditorMatrixProductionSupportBytes")
                && containsStringFieldPrefix(line,
                        "richTextEditorMatrixProductionEntryArtifact",
                        "current-app-production/VAADIN/build/indexhtml-")
                && containsStringFieldPrefix(line,
                        "richTextEditorMatrixSourceChunkArtifact",
                        "current-app-frontend/generated/flow/chunks/chunk-")
                && containsStringField(line,
                        "richTextEditorMatrixSourceChunkStatus", "available")
                && line.contains("richTextEditorMatrixResolvedSpecifiers")
                && line.contains("richTextEditorMatrixUnresolvedSpecifiers")
                && containsStringField(line,
                        "richTextEditorMatrixAllowedSchemes", "vatori-resource,blob")
                && containsStringField(line,
                        "richTextEditorMatrixIntegrityPolicy",
                        "sha256-and-byte-count-for-selected-artifacts")
                && containsStringField(line,
                        "richTextEditorMatrixMavenGoal", "build-frontend")
                && containsStringField(line,
                        "richTextEditorMatrixBuildStatus", "success")
                && line.contains("richTextEditorMatrixOutputUsability")
                && containsLiteralField(line,
                        "richTextEditorMatrixFrameworkAppRequired", "true")
                && containsLiteralField(line,
                        "richTextEditorMatrixFrameworkAppObserved", "true")
                && containsLiteralField(line,
                        "richTextEditorMatrixCliContractEnabled", "true")
                && containsLiteralField(line,
                        "richTextEditorMatrixManifestReady", "true")
                && containsLiteralField(line,
                        "richTextEditorMatrixRuntimeHandoffRequired", "true")
                && containsLiteralField(line,
                        "richTextEditorMatrixRuntimeHandoffReady", "true")
                && containsLiteralField(line,
                        "richTextEditorMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "richTextEditorMatrixFixtureSubstituted", "false")
                && line.contains("richTextEditorMatrixAcceptableFailures")
                && line.contains("rich-text-editor-selection-toolbar-not-covered")
                && line.contains("richTextEditorMatrixRemainingBlockers")
                && line.contains("rich-text-editor-selection-toolbar-parity")
                && line.contains("richTextEditorMatrixNextResolutionStep");
    }

    private static boolean containsRichTextEditorMatrixV162ManifestFields(String line) {
        return containsStringField(line, "richTextEditorMatrixVersion",
                        "flow-ipc-rich-text-editor-matrix-v1_62")
                && containsStringField(line, "richTextEditorMatrixTargetTags",
                        "vaadin-rich-text-editor")
                && containsStringField(line, "richTextEditorMatrixJavaClasses",
                        "com.vaadin.flow.component.richtexteditor.RichTextEditor")
                && containsStringField(line, "richTextEditorMatrixComponentIds",
                        "framework-rich-text-editor,framework-rich-text-editor-status")
                && containsLiteralField(line,
                        "richTextEditorMatrixFrameworkAppObserved", "true")
                && containsLiteralField(line,
                        "richTextEditorMatrixManifestReady", "true")
                && containsLiteralField(line,
                        "richTextEditorMatrixRuntimeHandoffReady", "true")
                && containsLiteralField(line,
                        "richTextEditorMatrixNoLocalhost", "true");
    }

    private static boolean containsRichTextEditorMatrixV162Complete(String line) {
        return containsRichTextEditorMatrixV162Fields(line)
                && containsLiteralField(line,
                        "richTextEditorMatrixRuntimeHandoffSatisfied", "true")
                && containsLiteralField(line,
                        "richTextEditorMatrixStaged", "true")
                && containsLiteralField(line,
                        "richTextEditorMatrixCustomSchemeObserved", "true")
                && containsLiteralField(line,
                        "richTextEditorMatrixNoLocalhost", "true");
    }
}
