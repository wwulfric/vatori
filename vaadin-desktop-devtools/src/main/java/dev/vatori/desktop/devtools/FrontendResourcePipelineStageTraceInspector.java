package dev.vatori.desktop.devtools;

final class FrontendResourcePipelineStageTraceInspector {
    private FrontendResourcePipelineStageTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
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
            summary.frontendBuildPipeline.resourceStageObserved = true;
        }
        if (line.contains("\"type\":\"resourceStage\"")
                && line.contains("\"executionStatus\":\"executed\"")) {
            summary.frontendBuildPipeline.resourceStageExecutedObserved = true;
            if (line.contains("\"failureClassification\":\"")
                    && !line.contains("\"failureClassification\":\"\"")) {
                summary.frontendBuildPipeline.resourceStageFailureClassificationObserved = true;
            }
        }
        if (line.contains("\"type\":\"resourceStage\"")
                && line.contains("\"executionStatus\":\"strict-blocker\"")
                && line.contains("\"browserError\"")) {
            summary.frontendBuildPipeline.resourceStageStrictBlockerObserved = true;
            if (line.contains("\"failureClassification\":\"")
                    && !line.contains("\"failureClassification\":\"\"")) {
                summary.frontendBuildPipeline.resourceStageFailureClassificationObserved = true;
            }
        }
        if (line.contains("\"type\":\"resourceStage\"")
                && line.contains("\"lookupKind\":\"TextField-dev-bundle-resource-v1_4\"")
                && line.contains("\"executionStatus\":\"executed\"")) {
            summary.frontendBuildPipeline.textFieldStagedExecutionObserved = true;
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
            summary.frontendBuildPipeline.textFieldV17StageExecutedObserved = true;
        }
        if (line.contains("\"type\":\"resourceStage\"")
                && line.contains("vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                && line.contains("\"moduleIdentityStrategy\":\"stable-resource-path-blob-module-identity-v1_7\"")
                && (line.contains("\"executionStatus\":\"execution-attempt\"")
                        || line.contains("\"executionStatus\":\"staged\"")
                        || line.contains("\"executionStatus\":\"executed\""))) {
            summary.frontendBuildPipeline.textFieldV17ModuleIdentityDependencyObserved = true;
        }
        if (line.contains("\"type\":\"resourceStage\"")
                && line.contains("\"lookupKind\":\"TextField-dev-bundle-resource-v1_4\"")
                && line.contains("\"executionStatus\":\"strict-blocker\"")
                && line.contains("\"browserError\"")
                && line.contains("\"failureClassification\":\"textfield-production-class-execution-blocked\"")) {
            summary.frontendBuildPipeline.textFieldStagedBlockerObserved = true;
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
            summary.frontendBuildPipeline.textFieldV16StageBlockerObserved = true;
        }
        if (line.contains("\"type\":\"resourceStage\"")
                && line.contains("vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                && line.contains("\"executionStatus\":\"strict-blocker\"")
                && line.contains("\"blockerVersion\":\"flow-ipc-textfield-v1_6\"")
                && line.contains("\"failureClassification\":\"")
                && !line.contains("\"failureClassification\":\"\"")
                && line.contains("\"browserError\"")) {
            summary.frontendBuildPipeline.textFieldV16DependencyClassificationObserved = true;
        }
    }
}
