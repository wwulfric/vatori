package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceSummaryLinePrinter.printLine;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;

final class TraceSummaryPrinter {
    private TraceSummaryPrinter() {
    }

    static void print(TraceSummary summary, Path path, boolean printBaseMissingTypes) {
        print(summary, path, printBaseMissingTypes, System.out);
    }

    static void print(
            TraceSummary summary,
            Path path,
            boolean printBaseMissingTypes,
            PrintStream output) {
        TraceSummaryLinePrinter.withOutput(
                output,
                () -> printWithOutput(summary, path, printBaseMissingTypes, output));
    }

    private static void printWithOutput(
            TraceSummary summary,
            Path path,
            boolean printBaseMissingTypes,
            PrintStream output) {
        CoreTraceFields core = summary.core;
        output.println("Trace: " + path);
        output.println("Messages: " + summary.total);
        output.println("Directions: " + summary.directions);
        output.println("Types: " + summary.types);
        printLine("transport=flow-ipc", core.flowIpcTransport);
        printLine("compatibilityMode=false", core.compatibilityFalse);
        CoreCapabilityTraceSummaryPrinter.print(summary.coreCapabilities);
        printLine("server sequence observed", core.serverSequenceObserved);
        printLine("Flow IPC server sequence DOM observed", core.flowIpcServerSequenceDomObserved);
        printLine("click patch observed", core.clickPatchObserved);
        printLine("native clipboard observed", summary.coreCapabilities.nativeClipboardObserved);
        printLine("capability permission observed", summary.coreCapabilities.capabilityPermissionObserved);
        printLine("WebView error observed", core.webViewErrorObserved);
        printLine("UIDL writer observed", core.uidlWriterObserved);
        printLine("Flow security key observed", core.securityKeyObserved);
        printLine("resync security key observed", core.resyncSecurityKeyObserved);
        printLine("bootstrap scope observed", core.bootstrapScopeObserved);
        printLine("event scope observed", core.eventScopeObserved);
        printLine("resync scope observed", core.resyncScopeObserved);
        FlowClientTraceSummaryPrinter.print(summary.flowClient);
        VaadinComponentTraceSummaryPrinter.print(summary.vaadinComponents);
        printLine("UIDL request handler patch observed", core.uidlPatchObserved);
        printLine("server push UIDL observed", core.pushUidlObserved);
        printLine("multi-push UIDL observed", core.multiPushUidlObserved);
        printLine("duplicate/late push probe observed", core.duplicateLatePushProbeObserved);
        printLine("recovery resync request observed", core.recoveryResyncRequestObserved);
        printLine("resync UIDL observed", core.resyncUidlObserved);
        FrameworkAppTraceSummaryPrinter.printInteractionSignals(summary.frameworkApp);
        FrameworkAppTraceSummaryPrinter.printNativeClipboardReadMatrix(summary.frameworkApp);
        ThemeMatrixTraceSummaryPrinter.print(summary);
        AuthenticationTraceSummaryPrinter.print(summary);
        FrameworkAppTraceSummaryPrinter.printNotificationSignals(summary.frameworkApp);
        printLine("first meaningful paint observed", core.firstMeaningfulPaintObserved);
        FrameworkAppTraceSummaryPrinter.printDomSignals(summary.frameworkApp);
        FrameworkComponentMatrixTraceSummaryPrinter.print(summary);
        FrontendBuildTraceSummaryPrinter.print(summary);
        ComponentParityMatrixTraceSummaryPrinter.print(summary);
        ResourcePipelineTraceSummaryPrinter.print(summary);
        List<String> missing = TraceRequiredTypes.missingBaseTypes(summary);
        if (printBaseMissingTypes && !missing.isEmpty()) {
            output.println("Missing required types: " + missing);
        }
    }
}
