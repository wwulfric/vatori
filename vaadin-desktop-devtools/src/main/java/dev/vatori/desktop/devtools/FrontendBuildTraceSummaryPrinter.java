package dev.vatori.desktop.devtools;

final class FrontendBuildTraceSummaryPrinter {
    private FrontendBuildTraceSummaryPrinter() {
    }

    static void print(TraceSummary summary) {
        FrontendBuildEarlyTraceSummaryPrinter.print(summary);
        FrontendBuildResolverTraceSummaryPrinter.print(summary);
        FrontendBuildCurrentAppBuildOutputTraceSummaryPrinter.print(summary);
        FrontendBuildCurrentAppOwnerTraceSummaryPrinter.print(summary);
        FrontendBuildGeneratedImportsTraceSummaryPrinter.print(summary);
    }
}
