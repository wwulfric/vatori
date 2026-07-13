package dev.vatori.desktop.devtools;

final class ResourceStageTraceFields {
    private ResourceStageTraceFields() {
    }

    static boolean isResourceStage(String line) {
        return line.contains("\"type\":\"resourceStage\"");
    }

    static boolean hasExecuted(String line) {
        return line.contains("\"executionStatus\":\"executed\"");
    }

    static boolean hasExecutedOrStrictBlocker(String line) {
        return hasExecuted(line)
                || line.contains("\"executionStatus\":\"strict-blocker\"");
    }

    static boolean hasExecutedStrictBlockerOrStaged(String line) {
        return hasExecutedOrStrictBlocker(line)
                || line.contains("\"executionStatus\":\"staged\"");
    }

    static boolean hasExecutedStrictBlockerOrAttempt(String line) {
        return hasExecutedOrStrictBlocker(line)
                || line.contains("\"executionStatus\":\"execution-attempt\"");
    }

    static boolean hasAttemptExecutedOrFailed(String line) {
        return line.contains("\"executionStatus\":\"execution-attempt\"")
                || hasExecuted(line)
                || line.contains("\"executionStatus\":\"execution-failed\"");
    }

    static boolean hasAttemptStrictBlockerOrFailed(String line) {
        return line.contains("\"executionStatus\":\"execution-attempt\"")
                || line.contains("\"executionStatus\":\"strict-blocker\"")
                || line.contains("\"executionStatus\":\"execution-failed\"");
    }

    static boolean hasExecutedStrictBlockerOrFailed(String line) {
        return hasExecutedOrStrictBlocker(line)
                || line.contains("\"executionStatus\":\"execution-failed\"");
    }
}
