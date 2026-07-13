package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class FlowClientResourceGraphTraceInspector {
    private FlowClientResourceGraphTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"flowClientResources\"")
                && line.contains("\"flow-client-classpath\"")
                && containsPath(line, "META-INF/frontend/FlowBootstrap.js")) {
            summary.flowClient.resourceManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"flowClientResources\"")
                && line.contains("\"flow-client-classpath\"")
                && containsPath(line, "META-INF/frontend/FlowClient.js")
                && line.contains("\"ipcAugmented\":true")) {
            summary.flowClient.engineResourceManifestObserved = true;
        }
        if (line.contains("\"type\":\"resourceRequest\"")
                && containsPath(line, "META-INF/frontend/FlowBootstrap.js")
                && line.contains("\"securityKey\"")) {
            summary.flowClient.resourceRequestObserved = true;
        }
        if (line.contains("\"type\":\"resourceRequest\"")
                && containsPath(line, "META-INF/frontend/FlowClient.js")
                && line.contains("\"securityKey\"")) {
            summary.flowClient.engineResourceRequestObserved = true;
        }
        if (line.contains("\"type\":\"resourceRequest\"")
                && line.contains("\"purpose\":\"flow-client-lazy-lookup\"")
                && line.contains("\"lookupKind\":\"Flow.loadOnDemand-minimum-probe\"")
                && containsPath(line, "META-INF/frontend/Flow.js")
                && line.contains("\"securityKey\"")) {
            summary.flowClient.resourceLazyLookupRequestObserved = true;
        }
        if (line.contains("\"type\":\"resourceResponse\"")
                && line.contains("\"flowClientResource\":true")
                && containsPath(line, "META-INF/frontend/FlowBootstrap.js")
                && line.contains("\"bodyRedacted\":true")) {
            summary.flowClient.resourceResponseObserved = true;
        }
        if (line.contains("\"type\":\"resourceResponse\"")
                && line.contains("\"flowClientResource\":true")
                && containsPath(line, "META-INF/frontend/FlowClient.js")
                && line.contains("\"bodyRedacted\":true")) {
            summary.flowClient.engineResourceResponseObserved = true;
        }
        if (line.contains("\"type\":\"resourceResponse\"")
                && line.contains("\"flowClientResource\":true")
                && line.contains("\"requestPurpose\":\"flow-client-lazy-lookup\"")
                && line.contains("\"lookupKind\":\"Flow.loadOnDemand-minimum-probe\"")
                && containsPath(line, "META-INF/frontend/Flow.js")
                && line.contains("\"bodyRedacted\":true")) {
            summary.flowClient.resourceLazyLookupResponseObserved = true;
        }
        inspectDomEvidence(summary, line);
    }

    private static void inspectDomEvidence(TraceSummary summary, String line) {
        if (line.contains("\"type\":\"webview.dom\"")
                && line.contains("flowClientResourceLoaded")
                && line.contains("flowClientResourceVerified")
                && line.contains("flowClientResourceModuleLoaded")
                && line.contains("flowClientBootstrapExported")
                && line.contains("flowClientBootstrapInitialized")
                && line.contains("ROOT-2521314")
                && line.contains("flowClientResourceExecuted")
                && line.contains("META-INF/frontend/FlowBootstrap.js")) {
            summary.flowClient.resourceDomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "flowClientResourceManifestCount", "6")
                && containsLiteralField(line, "flowClientResourceGraphRequestedCount", "6")
                && containsLiteralField(line, "flowClientResourceGraphResponseCount", "6")
                && containsLiteralField(line, "flowClientResourceGraphComplete", "true")
                && containsLiteralField(line, "flowClientResourceGraphVerified", "true")
                && line.contains("META-INF/frontend/FlowBootstrap.js")
                && line.contains("META-INF/frontend/FlowClient.js")
                && line.contains("META-INF/frontend/Flow.js")
                && line.contains("META-INF/frontend/index.js")
                && line.contains("META-INF/resources/VAADIN/static/client/compile.properties")
                && line.contains(".cache.js")) {
            summary.flowClient.resourceGraphDomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "flowClientResourceExecutionClassificationComplete", "true")
                && containsLiteralField(line, "flowClientResourceVerifiedCount", "6")
                && containsLiteralField(line, "flowClientResourceExecutedCount", "2")
                && containsLiteralField(line, "flowClientResourceVerifiedOnlyCount", "4")
                && containsLiteralField(line, "flowClientResourceIntentionallyNotExecutedCount", "4")
                && containsLiteralField(line, "flowClientResourceExecutionFailedCount", "0")
                && line.contains("flowClientResourceExecutedPaths")
                && line.contains("META-INF/frontend/FlowBootstrap.js")
                && line.contains("META-INF/frontend/FlowClient.js")
                && line.contains("flowClientResourceVerifiedOnlyPaths")
                && line.contains("META-INF/frontend/Flow.js")
                && line.contains("META-INF/frontend/index.js")
                && line.contains("META-INF/resources/VAADIN/static/client/compile.properties")
                && line.contains(".cache.js")) {
            summary.flowClient.resourceExecutionClassificationDomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "flowClientResourceLazyLookupRequested", "true")
                && containsLiteralField(line, "flowClientResourceLazyLookupResponse", "true")
                && containsLiteralField(line, "flowClientResourceLazyLookupVerified", "true")
                && containsStringField(line, "flowClientResourceLazyLookupPurpose", "flow-client-lazy-lookup")
                && containsStringField(line, "flowClientResourceLazyLookupKind", "Flow.loadOnDemand-minimum-probe")
                && containsStringField(line, "flowClientResourceLazyLookupPath", "META-INF/frontend/Flow.js")
                && containsStringField(line, "flowClientResourceLazyLookupResult",
                        "verified-only:intentionally-not-executed")
                && line.contains("flowClientResourceLoadOnDemandProbeResult")
                && containsStringField(line, "flowClientResourceBootstrapLazyEntriesResult",
                        "no-bootstrap-LAZY-entry-in-smoke")
                && containsLiteralField(line, "flowClientResourceStaticLookupVerified", "true")
                && containsStringField(line, "flowClientResourceStaticLookupPath",
                        "META-INF/resources/VAADIN/static/client/compile.properties")
                && (containsStringField(line, "flowClientResourceThemeLookupResult",
                        "no-theme-resource-declared-by-smoke-app")
                        || line.contains("\"flowClientResourceThemeLookupResult\":\"verified:")
                        || line.contains("\\\"flowClientResourceThemeLookupResult\\\":\\\"verified:"))) {
            summary.flowClient.resourceLazyLookupDomObserved = true;
        }
    }
}
