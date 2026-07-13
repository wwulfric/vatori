package dev.vatori.desktop.devtools;

final class FrontendBuildGeneratedImportsOwnerTraceFields {
    boolean sideEffectV140ManifestObserved;
    boolean sideEffectV140RequestObserved;
    boolean sideEffectV140ResponseObserved;
    boolean sideEffectV140StageObserved;
    boolean sideEffectV140CompleteStageObserved;
    boolean sideEffectV140DomObserved;
    boolean selectOwnerCoverageV141ManifestObserved;
    boolean selectOwnerCoverageV141RequestObserved;
    boolean selectOwnerCoverageV141ResponseObserved;
    boolean selectOwnerCoverageV141StageObserved;
    boolean selectOwnerCoverageV141CompleteStageObserved;
    boolean selectOwnerCoverageV141DomObserved;
    boolean runtimeContractV142ManifestObserved;
    boolean runtimeContractV142RequestObserved;
    boolean runtimeContractV142ResponseObserved;
    boolean runtimeContractV142StageObserved;
    boolean runtimeContractV142CompleteStageObserved;
    boolean runtimeContractV142DomObserved;

    boolean hasSideEffectV1_40Signals() {
        return sideEffectV140ManifestObserved
                && sideEffectV140RequestObserved
                && sideEffectV140ResponseObserved
                && sideEffectV140StageObserved
                && sideEffectV140CompleteStageObserved
                && sideEffectV140DomObserved;
    }

    boolean hasSelectOwnerCoverageV1_41Signals() {
        return selectOwnerCoverageV141ManifestObserved
                && selectOwnerCoverageV141RequestObserved
                && selectOwnerCoverageV141ResponseObserved
                && selectOwnerCoverageV141StageObserved
                && selectOwnerCoverageV141CompleteStageObserved
                && selectOwnerCoverageV141DomObserved;
    }

    boolean hasRuntimeContractV1_42Signals() {
        return runtimeContractV142ManifestObserved
                && runtimeContractV142RequestObserved
                && runtimeContractV142ResponseObserved
                && runtimeContractV142StageObserved
                && runtimeContractV142CompleteStageObserved
                && runtimeContractV142DomObserved;
    }

    static boolean containsCurrentAppGeneratedImportsOwnerSideEffectV140Fields(String line) {
        return FrontendBuildGeneratedImportsOwnerSideEffectTraceFields
                .containsCurrentAppGeneratedImportsOwnerSideEffectV140Fields(line);
    }

    static boolean containsCurrentAppGeneratedImportsOwnerSideEffectV140Complete(String line) {
        return FrontendBuildGeneratedImportsOwnerSideEffectTraceFields
                .containsCurrentAppGeneratedImportsOwnerSideEffectV140Complete(line);
    }

    static boolean containsCurrentAppGeneratedImportsSelectOwnerCoverageV141Fields(String line) {
        return FrontendBuildGeneratedImportsOwnerSideEffectTraceFields
                .containsCurrentAppGeneratedImportsSelectOwnerCoverageV141Fields(line);
    }

    static boolean containsCurrentAppGeneratedImportsSelectOwnerCoverageV141Complete(String line) {
        return FrontendBuildGeneratedImportsOwnerSideEffectTraceFields
                .containsCurrentAppGeneratedImportsSelectOwnerCoverageV141Complete(line);
    }

    static boolean containsCurrentAppGeneratedImportsOwnerRuntimeContractV142Fields(String line) {
        return FrontendBuildGeneratedImportsOwnerRuntimeContractTraceFields
                .containsCurrentAppGeneratedImportsOwnerRuntimeContractV142Fields(line);
    }

    static boolean containsCurrentAppGeneratedImportsOwnerRuntimeContractV142ImportComplete(String line) {
        return FrontendBuildGeneratedImportsOwnerRuntimeContractTraceFields
                .containsCurrentAppGeneratedImportsOwnerRuntimeContractV142ImportComplete(line);
    }

    static boolean containsCurrentAppGeneratedImportsOwnerRuntimeContractV142DomComplete(String line) {
        return FrontendBuildGeneratedImportsOwnerRuntimeContractTraceFields
                .containsCurrentAppGeneratedImportsOwnerRuntimeContractV142DomComplete(line);
    }

    static boolean containsGeneratedLazySpiDecisionV143Fields(String line) {
        return FrontendBuildGeneratedLazySpiDecisionTraceFields.containsGeneratedLazySpiDecisionV143Fields(line);
    }

    static boolean containsGeneratedLazySpiDecisionV143ChunkExecuted(String line) {
        return FrontendBuildGeneratedLazySpiDecisionTraceFields
                .containsGeneratedLazySpiDecisionV143ChunkExecuted(line);
    }
}
