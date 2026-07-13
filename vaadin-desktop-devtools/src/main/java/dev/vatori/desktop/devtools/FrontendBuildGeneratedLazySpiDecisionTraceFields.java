package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class FrontendBuildGeneratedLazySpiDecisionTraceFields {
    boolean decisionV143ManifestObserved;
    boolean decisionV143RequestObserved;
    boolean decisionV143ResponseObserved;
    boolean decisionV143StageObserved;
    boolean decisionV143ChunkExecutedObserved;
    boolean decisionV143CompleteStageObserved;
    boolean decisionV143DomObserved;

    boolean hasDecisionV1_43Signals() {
        return decisionV143ManifestObserved
                && decisionV143RequestObserved
                && decisionV143ResponseObserved
                && decisionV143StageObserved
                && decisionV143ChunkExecutedObserved
                && decisionV143CompleteStageObserved
                && decisionV143DomObserved;
    }

    static boolean containsGeneratedLazySpiDecisionV143Fields(String line) {
        return containsStringField(line, "generatedLazySpiDecisionVersion",
                        "flow-ipc-generated-lazy-spi-decision-v1_43")
                && containsStringField(line, "generatedLazySpiDecisionSource",
                        "current-app-generated-imports-owner-runtime-contract-v1_42")
                && containsStringField(line, "generatedLazySpiAuditedSourceSurfaces",
                        "AbstractUpdateImports,FrontendDependencies,ChunkInfo,build-frontend-generated-output,production-stats-json,prod-bundle")
                && containsStringField(line, "generatedLazySpiChunkInfoApiStatus",
                        "internal-may-be-renamed-or-removed")
                && containsStringField(line, "generatedLazySpiRegistrySource",
                        "current-app-build-frontend-output-v1_32")
                && containsStringField(line, "generatedLazySpiGeneratedImportsArtifact",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && line.contains("generatedLazySpiGeneratedImportsSha256")
                && line.contains("generatedLazySpiGeneratedImportsBytes")
                && containsStringField(line, "generatedLazySpiGeneratedImportsTableShape",
                        "branch-table-dynamic-import")
                && line.contains("generatedLazySpiChunkId")
                && containsStringFieldPrefix(line, "generatedLazySpiChunkImportSpecifier",
                        "./chunks/chunk-")
                && containsStringFieldPrefix(line, "generatedLazySpiChunkArtifact",
                        "current-app-frontend/generated/flow/chunks/chunk-")
                && containsStringField(line, "generatedLazySpiChunkStatus", "available")
                && line.contains("generatedLazySpiChunkSha256")
                && line.contains("generatedLazySpiChunkBytes")
                && containsStringField(line, "generatedLazySpiBuildOutputSource", "build-frontend")
                && containsStringFieldPrefix(line, "generatedLazySpiProductionSupportArtifact",
                        "current-app-production/VAADIN/build/generated-flow-imports-")
                && line.contains("generatedLazySpiStatsPath")
                && line.contains("generatedLazySpiProdBundlePath")
                && containsStringField(line, "generatedLazySpiDecision",
                        "packaging-time-build-frontend-staging")
                && containsStringField(line, "generatedLazySpiUnavailableReason",
                        "vaadin-chunkinfo-internal-no-supported-runtime-spi")
                && containsStringField(line, "generatedLazySpiFailureLayer",
                        "supported-vaadin-lazy-chunk-spi-unavailable")
                && containsStringField(line, "generatedLazySpiProductPath",
                        "packaging-time-build-frontend-output-staging")
                && containsStringField(line, "generatedLazySpiExecutionEvidence",
                        "current-app-generated-chunk-loadondemand-executed-v1_33")
                && containsStringField(line, "generatedLazySpiExecutionTrigger",
                        "host-decision-audit-probe-not-flow-client-spi")
                && line.contains("generatedLazySpiNextResolutionStep")
                && containsLiteralField(line, "generatedLazySpiAvailable", "false")
                && containsLiteralField(line, "generatedLazySpiRuntimePathSelected", "false")
                && containsLiteralField(line, "generatedLazySpiPackagingTimeStagingSelected", "true")
                && containsLiteralField(line,
                        "generatedLazySpiCurrentAppGeneratedImportsObserved", "true")
                && containsLiteralField(line, "generatedLazySpiCurrentAppChunkAvailable", "true")
                && containsLiteralField(line, "generatedLazySpiProductionSupportObserved", "true")
                && containsLiteralField(line, "generatedLazySpiStatsObserved", "true")
                && containsLiteralField(line, "generatedLazySpiProdBundleObserved", "true")
                && containsLiteralField(line, "generatedLazySpiNoLocalhost", "true")
                && containsLiteralField(line, "generatedLazySpiFixtureSubstituted", "false");
    }

    static boolean containsGeneratedLazySpiDecisionV143ChunkExecuted(String line) {
        return containsGeneratedLazySpiDecisionV143Fields(line)
                && containsLiteralField(line, "generatedLazySpiCurrentAppChunkExecuted", "true");
    }
}
