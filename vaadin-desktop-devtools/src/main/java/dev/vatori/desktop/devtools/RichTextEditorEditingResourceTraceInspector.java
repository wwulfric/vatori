package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.GridResourceTraceFields.*;
import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class RichTextEditorEditingResourceTraceInspector {
    private RichTextEditorEditingResourceTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && containsRichTextEditorEditingMatrixV164ManifestFields(line)) {
            summary.frameworkRichTextEditor.editingMatrixV164ManifestObserved = true;
        }
        if (line.contains("\"type\":\"resourceStage\"")
                && containsCurrentAppProductionMatrixRuntimePath(line)
                && containsRichTextEditorEditingMatrixV164Fields(line)
                && (line.contains("\"executionStatus\":\"executed\"")
                        || line.contains("\"executionStatus\":\"strict-blocker\"")
                        || line.contains("\"executionStatus\":\"staged\""))) {
            summary.frameworkRichTextEditor.editingMatrixV164StageObserved = true;
        }
        if (line.contains("\"type\":\"resourceStage\"")
                && containsCurrentAppProductionMatrixRuntimePath(line)
                && containsRichTextEditorEditingMatrixV164Complete(line)
                && line.contains("\"executionStatus\":\"executed\"")) {
            summary.frameworkRichTextEditor.editingMatrixV164CompleteStageObserved = true;
        }
    }

    private static boolean containsRichTextEditorEditingMatrixV164Fields(String line) {
        return containsStringField(line, "richTextEditorEditingMatrixVersion",
                        "flow-ipc-rich-text-editor-editing-matrix-v1_64")
                && containsStringField(line, "richTextEditorEditingMatrixSource",
                        "rich-text-editor-behavior-matrix-v1_63")
                && containsStringField(line, "richTextEditorEditingMatrixScope",
                        "framework-validation-app-rich-text-editor-format-clipboard-link-list")
                && containsStringField(line, "richTextEditorEditingMatrixAppShape",
                        "framework-validation-app")
                && containsStringField(line, "richTextEditorEditingMatrixTargetTags",
                        "vaadin-rich-text-editor")
                && containsStringField(line, "richTextEditorEditingMatrixJavaClasses",
                        "com.vaadin.flow.component.richtexteditor.RichTextEditor,com.vaadin.flow.component.richtexteditor.RichTextEditorVariant")
                && containsStringField(line, "richTextEditorEditingMatrixComponentIds",
                        "framework-rich-text-editor,framework-rich-text-editor-status,framework-rich-text-editor-behavior-status,framework-rich-text-editor-editing-status")
                && containsStringField(line, "richTextEditorEditingMatrixModuleSpecifiers",
                        "@vaadin/rich-text-editor/src/vaadin-rich-text-editor.js")
                && containsStringField(line, "richTextEditorEditingMatrixExpectedEvents",
                        "vatori-rich-text-editor-format,vatori-rich-text-editor-clipboard,vatori-rich-text-editor-link")
                && containsStringField(line, "richTextEditorEditingMatrixExpectedBehavior",
                        "italic-format,ordered-list,link-format,clear-format,clipboard-copy-paste,extended-selection-range,compact-theme-variant")
                && containsStringFieldPrefix(line, "richTextEditorEditingMatrixSelectedArtifacts",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && containsPositiveIntegerField(line,
                        "richTextEditorEditingMatrixSelectedArtifactCount")
                && containsStringField(line,
                        "richTextEditorEditingMatrixGeneratedImportsOwnerSource",
                        "current-app-generated-imports-select-owner-coverage-v1_41")
                && containsStringField(line,
                        "richTextEditorEditingMatrixGeneratedImportsOwnerArtifact",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && line.contains("richTextEditorEditingMatrixProductionSupportArtifact")
                && line.contains("richTextEditorEditingMatrixProductionSupportSha256")
                && line.contains("richTextEditorEditingMatrixProductionSupportBytes")
                && containsStringFieldPrefix(line,
                        "richTextEditorEditingMatrixProductionEntryArtifact",
                        "current-app-production/VAADIN/build/indexhtml-")
                && containsStringFieldPrefix(line,
                        "richTextEditorEditingMatrixSourceChunkArtifact",
                        "current-app-frontend/generated/flow/chunks/chunk-")
                && containsStringField(line,
                        "richTextEditorEditingMatrixSourceChunkStatus", "available")
                && line.contains("richTextEditorEditingMatrixResolvedSpecifiers")
                && line.contains("richTextEditorEditingMatrixUnresolvedSpecifiers")
                && containsStringField(line,
                        "richTextEditorEditingMatrixAllowedSchemes", "vatori-resource,blob")
                && containsStringField(line,
                        "richTextEditorEditingMatrixIntegrityPolicy",
                        "sha256-and-byte-count-for-selected-artifacts")
                && containsStringField(line,
                        "richTextEditorEditingMatrixMavenGoal", "build-frontend")
                && containsStringField(line,
                        "richTextEditorEditingMatrixBuildStatus", "success")
                && line.contains("richTextEditorEditingMatrixOutputUsability")
                && containsLiteralField(line,
                        "richTextEditorEditingMatrixFrameworkAppRequired", "true")
                && containsLiteralField(line,
                        "richTextEditorEditingMatrixFrameworkAppObserved", "true")
                && containsLiteralField(line,
                        "richTextEditorEditingMatrixCliContractEnabled", "true")
                && containsLiteralField(line,
                        "richTextEditorEditingMatrixManifestReady", "true")
                && containsLiteralField(line,
                        "richTextEditorEditingMatrixRuntimeHandoffRequired", "true")
                && containsLiteralField(line,
                        "richTextEditorEditingMatrixRuntimeHandoffReady", "true")
                && containsLiteralField(line,
                        "richTextEditorEditingMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "richTextEditorEditingMatrixFixtureSubstituted", "false")
                && line.contains("richTextEditorEditingMatrixAcceptableFailures")
                && line.contains("rich-text-editor-format-event-rpc-missing")
                && line.contains("richTextEditorEditingMatrixRemainingBlockers")
                && line.contains("native-clipboard-integration")
                && line.contains("richTextEditorEditingMatrixNextResolutionStep");
    }

    private static boolean containsRichTextEditorEditingMatrixV164ManifestFields(String line) {
        return containsStringField(line, "richTextEditorEditingMatrixVersion",
                        "flow-ipc-rich-text-editor-editing-matrix-v1_64")
                && containsStringField(line, "richTextEditorEditingMatrixTargetTags",
                        "vaadin-rich-text-editor")
                && containsStringField(line, "richTextEditorEditingMatrixJavaClasses",
                        "com.vaadin.flow.component.richtexteditor.RichTextEditor,com.vaadin.flow.component.richtexteditor.RichTextEditorVariant")
                && containsStringField(line, "richTextEditorEditingMatrixComponentIds",
                        "framework-rich-text-editor,framework-rich-text-editor-status,framework-rich-text-editor-behavior-status,framework-rich-text-editor-editing-status")
                && containsLiteralField(line,
                        "richTextEditorEditingMatrixFrameworkAppObserved", "true")
                && containsLiteralField(line,
                        "richTextEditorEditingMatrixManifestReady", "true")
                && containsLiteralField(line,
                        "richTextEditorEditingMatrixRuntimeHandoffReady", "true")
                && containsLiteralField(line,
                        "richTextEditorEditingMatrixNoLocalhost", "true");
    }

    private static boolean containsRichTextEditorEditingMatrixV164Complete(String line) {
        return containsRichTextEditorEditingMatrixV164Fields(line)
                && containsLiteralField(line,
                        "richTextEditorEditingMatrixRuntimeHandoffSatisfied", "true")
                && containsLiteralField(line,
                        "richTextEditorEditingMatrixStaged", "true")
                && containsLiteralField(line,
                        "richTextEditorEditingMatrixCustomSchemeObserved", "true")
                && containsLiteralField(line,
                        "richTextEditorEditingMatrixNoLocalhost", "true");
    }
}
