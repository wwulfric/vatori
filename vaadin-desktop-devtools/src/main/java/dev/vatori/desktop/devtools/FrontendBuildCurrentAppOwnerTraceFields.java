package dev.vatori.desktop.devtools;

final class FrontendBuildCurrentAppOwnerTraceFields {
    private FrontendBuildCurrentAppOwnerTraceFields() {
    }

    static boolean containsCurrentAppProductionRuntimeOwnershipV135Fields(String line) {
        return FrontendBuildCurrentAppProductionOwnerTraceFields
                .containsCurrentAppProductionRuntimeOwnershipV135Fields(line);
    }

    static boolean containsCurrentAppProductionOwnerImportV136Fields(String line) {
        return FrontendBuildCurrentAppProductionOwnerTraceFields
                .containsCurrentAppProductionOwnerImportV136Fields(line);
    }

    static boolean containsCurrentAppProductionOwnerImportV136FailureOrSuccess(String line) {
        return FrontendBuildCurrentAppProductionOwnerTraceFields
                .containsCurrentAppProductionOwnerImportV136FailureOrSuccess(line);
    }

    static boolean containsCurrentAppProductionOwnerComponentReuseV137Fields(String line) {
        return FrontendBuildCurrentAppOwnerComponentReuseTraceFields
                .containsCurrentAppProductionOwnerComponentReuseV137Fields(line);
    }

    static boolean containsCurrentAppOwnerComponentReuseMatrixV138Fields(String line) {
        return FrontendBuildCurrentAppOwnerComponentReuseTraceFields
                .containsCurrentAppOwnerComponentReuseMatrixV138Fields(line);
    }

    static boolean containsCurrentAppOwnerComponentReuseMatrixCoveredComponentPath(String line) {
        return FrontendBuildCurrentAppOwnerComponentReuseTraceFields
                .containsCurrentAppOwnerComponentReuseMatrixCoveredComponentPath(line);
    }

    static boolean containsCurrentAppOwnerComponentReuseMatrixV138Complete(String line) {
        return FrontendBuildCurrentAppOwnerComponentReuseTraceFields
                .containsCurrentAppOwnerComponentReuseMatrixV138Complete(line);
    }

    static boolean containsCurrentAppOwnerEarlyImportV139Fields(String line) {
        return FrontendBuildCurrentAppOwnerEarlyImportTraceFields
                .containsCurrentAppOwnerEarlyImportV139Fields(line);
    }

    static boolean containsCurrentAppOwnerEarlyImportV139Complete(String line) {
        return FrontendBuildCurrentAppOwnerEarlyImportTraceFields
                .containsCurrentAppOwnerEarlyImportV139Complete(line);
    }
}
