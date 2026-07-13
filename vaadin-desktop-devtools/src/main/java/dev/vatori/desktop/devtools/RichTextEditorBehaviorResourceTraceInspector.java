package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.GridResourceTraceFields.*;
import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class RichTextEditorBehaviorResourceTraceInspector {
    private RichTextEditorBehaviorResourceTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && containsRichTextEditorBehaviorMatrixV163ManifestFields(line)) {
            summary.frameworkRichTextEditor.behaviorMatrixV163ManifestObserved = true;
        }
        if (line.contains("\"type\":\"resourceStage\"")
                && containsCurrentAppProductionMatrixRuntimePath(line)
                && containsRichTextEditorBehaviorMatrixV163Fields(line)
                && (line.contains("\"executionStatus\":\"executed\"")
                        || line.contains("\"executionStatus\":\"strict-blocker\"")
                        || line.contains("\"executionStatus\":\"staged\""))) {
            summary.frameworkRichTextEditor.behaviorMatrixV163StageObserved = true;
        }
        if (line.contains("\"type\":\"resourceStage\"")
                && containsCurrentAppProductionMatrixRuntimePath(line)
                && containsRichTextEditorBehaviorMatrixV163Complete(line)
                && line.contains("\"executionStatus\":\"executed\"")) {
            summary.frameworkRichTextEditor.behaviorMatrixV163CompleteStageObserved = true;
        }
    }

    private static boolean containsRichTextEditorBehaviorMatrixV163Fields(String line) {
        return containsStringField(line, "richTextEditorBehaviorMatrixVersion",
                        "flow-ipc-rich-text-editor-behavior-matrix-v1_63")
                && containsStringField(line, "richTextEditorBehaviorMatrixSource",
                        "rich-text-editor-matrix-v1_62")
                && containsStringField(line, "richTextEditorBehaviorMatrixScope",
                        "framework-validation-app-rich-text-editor-toolbar-selection-keyboard")
                && containsStringField(line, "richTextEditorBehaviorMatrixAppShape",
                        "framework-validation-app")
                && containsStringField(line, "richTextEditorBehaviorMatrixTargetTags",
                        "vaadin-rich-text-editor")
                && containsStringField(line, "richTextEditorBehaviorMatrixJavaClasses",
                        "com.vaadin.flow.component.richtexteditor.RichTextEditor,com.vaadin.flow.component.richtexteditor.RichTextEditorVariant")
                && containsStringField(line, "richTextEditorBehaviorMatrixComponentIds",
                        "framework-rich-text-editor,framework-rich-text-editor-status,framework-rich-text-editor-behavior-status")
                && containsStringField(line, "richTextEditorBehaviorMatrixModuleSpecifiers",
                        "@vaadin/rich-text-editor/src/vaadin-rich-text-editor.js")
                && containsStringField(line, "richTextEditorBehaviorMatrixExpectedEvents",
                        "vatori-rich-text-editor-selection,vatori-rich-text-editor-toolbar,keydown,value-changed")
                && containsStringField(line, "richTextEditorBehaviorMatrixExpectedBehavior",
                        "selection-range,toolbar-bold,keyboard-bold,delta-html-value,compact-theme-variant")
                && containsStringFieldPrefix(line, "richTextEditorBehaviorMatrixSelectedArtifacts",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && containsPositiveIntegerField(line,
                        "richTextEditorBehaviorMatrixSelectedArtifactCount")
                && containsStringField(line,
                        "richTextEditorBehaviorMatrixGeneratedImportsOwnerSource",
                        "current-app-generated-imports-select-owner-coverage-v1_41")
                && containsStringField(line,
                        "richTextEditorBehaviorMatrixGeneratedImportsOwnerArtifact",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && line.contains("richTextEditorBehaviorMatrixProductionSupportArtifact")
                && line.contains("richTextEditorBehaviorMatrixProductionSupportSha256")
                && line.contains("richTextEditorBehaviorMatrixProductionSupportBytes")
                && containsStringFieldPrefix(line,
                        "richTextEditorBehaviorMatrixProductionEntryArtifact",
                        "current-app-production/VAADIN/build/indexhtml-")
                && containsStringFieldPrefix(line,
                        "richTextEditorBehaviorMatrixSourceChunkArtifact",
                        "current-app-frontend/generated/flow/chunks/chunk-")
                && containsStringField(line,
                        "richTextEditorBehaviorMatrixSourceChunkStatus", "available")
                && line.contains("richTextEditorBehaviorMatrixResolvedSpecifiers")
                && line.contains("richTextEditorBehaviorMatrixUnresolvedSpecifiers")
                && containsStringField(line,
                        "richTextEditorBehaviorMatrixAllowedSchemes", "vatori-resource,blob")
                && containsStringField(line,
                        "richTextEditorBehaviorMatrixIntegrityPolicy",
                        "sha256-and-byte-count-for-selected-artifacts")
                && containsStringField(line,
                        "richTextEditorBehaviorMatrixMavenGoal", "build-frontend")
                && containsStringField(line,
                        "richTextEditorBehaviorMatrixBuildStatus", "success")
                && line.contains("richTextEditorBehaviorMatrixOutputUsability")
                && containsLiteralField(line,
                        "richTextEditorBehaviorMatrixFrameworkAppRequired", "true")
                && containsLiteralField(line,
                        "richTextEditorBehaviorMatrixFrameworkAppObserved", "true")
                && containsLiteralField(line,
                        "richTextEditorBehaviorMatrixCliContractEnabled", "true")
                && containsLiteralField(line,
                        "richTextEditorBehaviorMatrixManifestReady", "true")
                && containsLiteralField(line,
                        "richTextEditorBehaviorMatrixRuntimeHandoffRequired", "true")
                && containsLiteralField(line,
                        "richTextEditorBehaviorMatrixRuntimeHandoffReady", "true")
                && containsLiteralField(line,
                        "richTextEditorBehaviorMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "richTextEditorBehaviorMatrixFixtureSubstituted", "false")
                && line.contains("richTextEditorBehaviorMatrixAcceptableFailures")
                && line.contains("rich-text-editor-toolbar-not-rendered")
                && line.contains("richTextEditorBehaviorMatrixRemainingBlockers")
                && line.contains("full-rich-text-editor-toolbar-parity")
                && line.contains("richTextEditorBehaviorMatrixNextResolutionStep");
    }

    private static boolean containsRichTextEditorBehaviorMatrixV163ManifestFields(String line) {
        return containsStringField(line, "richTextEditorBehaviorMatrixVersion",
                        "flow-ipc-rich-text-editor-behavior-matrix-v1_63")
                && containsStringField(line, "richTextEditorBehaviorMatrixTargetTags",
                        "vaadin-rich-text-editor")
                && containsStringField(line, "richTextEditorBehaviorMatrixJavaClasses",
                        "com.vaadin.flow.component.richtexteditor.RichTextEditor,com.vaadin.flow.component.richtexteditor.RichTextEditorVariant")
                && containsStringField(line, "richTextEditorBehaviorMatrixComponentIds",
                        "framework-rich-text-editor,framework-rich-text-editor-status,framework-rich-text-editor-behavior-status")
                && containsLiteralField(line,
                        "richTextEditorBehaviorMatrixFrameworkAppObserved", "true")
                && containsLiteralField(line,
                        "richTextEditorBehaviorMatrixManifestReady", "true")
                && containsLiteralField(line,
                        "richTextEditorBehaviorMatrixRuntimeHandoffReady", "true")
                && containsLiteralField(line,
                        "richTextEditorBehaviorMatrixNoLocalhost", "true");
    }

    private static boolean containsRichTextEditorBehaviorMatrixV163Complete(String line) {
        return containsRichTextEditorBehaviorMatrixV163Fields(line)
                && containsLiteralField(line,
                        "richTextEditorBehaviorMatrixRuntimeHandoffSatisfied", "true")
                && containsLiteralField(line,
                        "richTextEditorBehaviorMatrixStaged", "true")
                && containsLiteralField(line,
                        "richTextEditorBehaviorMatrixCustomSchemeObserved", "true")
                && containsLiteralField(line,
                        "richTextEditorBehaviorMatrixNoLocalhost", "true");
    }
}
