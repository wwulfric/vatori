package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class FrameworkLazyGridPatchTraceInspector {
    private FrameworkLazyGridPatchTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (!line.contains("\"source\":\"UidlRequestHandler\"")
                || !line.contains("\"reason\":\"event\"")) {
            return;
        }
        String fetchRange = lazyGridFetchRange(line);
        if (!fetchRange.isEmpty()) {
            if (summary.gridResources.lazyFetchPatchRange.isEmpty()) {
                summary.gridResources.lazyFetchPatchRange = fetchRange;
                summary.gridResources.lazyFetchPatchObserved = true;
            } else if (!fetchRange.equals(summary.gridResources.lazyFetchPatchRange)) {
                summary.gridResources.scrollFetchPatchObserved = true;
            }
        }
        String reportRange = lazyGridReportRange(line);
        if (!reportRange.isEmpty()) {
            if (summary.gridResources.lazyFetchPatchRange.isEmpty()
                    || reportRange.equals(summary.gridResources.lazyFetchPatchRange)) {
                summary.gridResources.lazyReportPatchObserved = true;
            } else {
                summary.gridResources.scrollReportPatchObserved = true;
            }
        }
    }
}
