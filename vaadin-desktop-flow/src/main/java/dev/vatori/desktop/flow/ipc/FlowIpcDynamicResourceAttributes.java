package dev.vatori.desktop.flow.ipc;

import java.util.LinkedHashMap;
import java.util.Map;

final class FlowIpcDynamicResourceAttributes {
    private static final FlowIpcComponentMatrixAttributes.Spec GRID_LAZY_DATA_PROVIDER =
            new FlowIpcComponentMatrixAttributes.Spec(
                    "gridLazyDataProvider",
                    "flow-ipc-grid-lazy-data-provider-v1_48",
                    "data-component-resource-matrix-v1_47",
                    "framework-validation-app-grid-callback-data-provider-viewport-rpc",
                    "vaadin-grid,vaadin-grid-column",
                    "com.vaadin.flow.component.grid.Grid,com.vaadin.flow.data.provider.CallbackDataProvider,com.vaadin.flow.data.provider.DataCommunicator",
                    "framework-lazy-grid,framework-lazy-grid-status,framework-lazy-grid-report",
                    "@vaadin/grid/src/vaadin-grid.js",
                    "VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_V1_48",
                    "published-server-method-rpc-missing,data-provider-fetch-not-observed,grid-row-virtualization-dom-not-materialized,browser-import-error",
                    "framework-grid-lazy-data-provider-runtime-validation-pending",
                    "full-grid-scroll-virtualization,upload-download,theme-variants,dev-mode-vite-parity,installer-packaging,durable-reconnect",
                    "turn the callback-backed Grid viewport RPC evidence into full scroll virtualization or move to Upload/Download resource parity");
    private static final FlowIpcComponentMatrixAttributes.Spec GRID_SCROLL_VIRTUALIZATION =
            new FlowIpcComponentMatrixAttributes.Spec(
                    "gridScrollVirtualization",
                    "flow-ipc-grid-scroll-virtualization-v1_49",
                    "grid-lazy-data-provider-v1_48",
                    "framework-validation-app-grid-client-scroll-api-probe",
                    "vaadin-grid,vaadin-grid-column",
                    "com.vaadin.flow.component.grid.Grid,com.vaadin.flow.data.provider.CallbackDataProvider,com.vaadin.flow.data.provider.DataCommunicator",
                    "framework-lazy-grid,framework-lazy-grid-status,framework-lazy-grid-report",
                    "@vaadin/grid/src/vaadin-grid.js",
                    "VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_V1_49",
                    "client-scroll-api-no-viewport-rpc,grid-client-api-missing,data-provider-fetch-not-observed,browser-import-error",
                    "framework-grid-scroll-virtualization-runtime-validation-pending",
                    "full-grid-scroll-virtualization,upload-download,theme-variants,dev-mode-vite-parity,installer-packaging,durable-reconnect",
                    "turn the bounded scrollToIndex probe into full Grid scroll container virtualization or move to Upload/Download resource parity");
    private static final Spec STREAM_RESOURCE = new Spec(
            "streamResource",
            "flow-ipc-stream-resource-v1_50",
            "grid-scroll-virtualization-v1_49",
            "framework-validation-app-dynamic-stream-resource-download",
            "com.vaadin.flow.component.html.Anchor,com.vaadin.flow.server.StreamResource,com.vaadin.flow.server.StreamResourceRegistry",
            "framework-stream-resource-download,framework-stream-resource-status",
            "VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_V1_50",
            "stream-resource-registry-unavailable,dynamic-resource-href-missing,resource-response-missing,body-hash-mismatch",
            "framework-stream-resource-runtime-validation-pending",
            "full-upload-download-streaming,range-requests,large-file-backpressure,theme-variants,dev-mode-vite-parity,installer-packaging,durable-reconnect",
            "turn this bounded StreamResource download into Upload/Download streaming and broader dynamic resource parity");
    private static final Spec STREAM_RESOURCE_RANGE = new Spec(
            "streamResourceRange",
            "flow-ipc-stream-resource-range-v1_51",
            "stream-resource-v1_50",
            "framework-validation-app-binary-stream-resource-range-download",
            "com.vaadin.flow.component.html.Anchor,com.vaadin.flow.server.StreamResource,com.vaadin.flow.server.StreamResourceRegistry",
            "framework-stream-resource-range-download,framework-stream-resource-range-status",
            "VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_V1_51",
            "stream-resource-registry-unavailable,dynamic-resource-href-missing,range-response-missing,range-body-marker-missing,range-slice-metadata-mismatch",
            "framework-stream-resource-range-runtime-validation-pending",
            "full-upload-download-streaming,large-file-backpressure,progress-cancellation,downloadhandler-parity,upload-receiver-parity,theme-variants,dev-mode-vite-parity,installer-packaging,durable-reconnect",
            "turn binary range-slice download evidence into DownloadHandler/Upload streaming with progress, cancellation and OS file grants");
    private static final Spec STREAM_RESOURCE_CHUNKS = new Spec(
            "streamResourceChunks",
            "flow-ipc-stream-resource-chunks-v1_52",
            "stream-resource-range-v1_51",
            "framework-validation-app-binary-stream-resource-sequential-chunk-download",
            "com.vaadin.flow.component.html.Anchor,com.vaadin.flow.server.StreamResource,com.vaadin.flow.server.StreamResourceRegistry",
            "framework-stream-resource-range-download,framework-stream-resource-range-status,framework-stream-resource-chunks-status",
            "VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_V1_52",
            "stream-resource-registry-unavailable,dynamic-resource-href-missing,chunk-response-missing,chunk-order-mismatch,chunk-body-marker-missing,chunk-metadata-mismatch",
            "framework-stream-resource-chunks-runtime-validation-pending",
            "full-upload-download-streaming,parallel-chunk-backpressure,retry-resume,progress-cancellation,downloadhandler-parity,upload-receiver-parity,theme-variants,dev-mode-vite-parity,installer-packaging,durable-reconnect",
            "turn bounded sequential chunk evidence into generalized streaming with retry/resume, progress, cancellation and OS file grants");
    private static final Spec UPLOAD_RECEIVER = new Spec(
            "uploadReceiver",
            "flow-ipc-upload-receiver-v1_53",
            "stream-resource-chunks-v1_52",
            "framework-validation-app-upload-element-request-handler-bounded-raw-body",
            "com.vaadin.flow.component.upload.Upload,com.vaadin.flow.server.streams.UploadHandler,com.vaadin.flow.server.StreamResourceRegistry.ElementStreamResource",
            "framework-upload,framework-upload-status",
            "VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_V1_53",
            "upload-target-missing,upload-resource-registry-unavailable,upload-resource-not-element-request-handler,upload-handler-error,uidl-patch-missing",
            "framework-upload-receiver-runtime-validation-pending",
            "multipart-upload-parity,multi-file-upload,large-file-backpressure,progress-cancellation,retry-resume,downloadhandler-parity,theme-variants,dev-mode-vite-parity,installer-packaging,durable-reconnect",
            "turn bounded raw UploadHandler evidence into multipart/progress/cancel Upload parity and generalized streaming");
    private static final Spec UPLOAD_MULTIPART = new Spec(
            "uploadMultipart",
            "flow-ipc-upload-multipart-v1_54",
            "flow-ipc-upload-receiver-v1_53",
            "framework-validation-app-upload-element-request-handler-bounded-multipart-progress-cancel",
            "com.vaadin.flow.component.upload.Upload,com.vaadin.flow.server.streams.UploadHandler,com.vaadin.flow.server.StreamResourceRegistry.ElementStreamResource,jakarta.servlet.http.Part",
            "framework-upload,framework-upload-status",
            "VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_V1_54",
            "upload-multipart-target-missing,upload-multipart-servlet-request-adapter-missing,upload-multipart-parser-error,upload-multipart-part-missing,upload-multipart-handler-error,upload-multipart-progress-unconsumed,upload-multipart-cancel-classified",
            "framework-upload-multipart-runtime-validation-pending",
            "multi-file-upload,large-file-streaming,parallel-upload,retry-resume,real-backpressure,upload-client-progress-ui-consumption,cancel-abort-wiring,downloadhandler-parity,os-file-grants,dev-mode-vite-parity,installer-packaging,durable-reconnect",
            "turn bounded single-file multipart evidence into streaming upload with real progress/cancel/backpressure");
    private static final Spec DOWNLOAD_HANDLER_SAVE_OPEN = new Spec(
            "downloadHandlerSaveOpen",
            "flow-ipc-download-handler-save-open-v1_55",
            "flow-ipc-upload-multipart-v1_54",
            "framework-validation-app-downloadhandler-native-save-open-grant",
            "com.vaadin.flow.component.html.Anchor,com.vaadin.flow.server.streams.DownloadHandler,com.vaadin.flow.server.streams.DownloadResponse,com.vaadin.flow.server.StreamResourceRegistry.ElementStreamResource",
            "framework-download-handler-save-open,framework-download-handler-save-open-status",
            "VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_V1_55",
            "download-handler-target-missing,download-handler-registry-unavailable,download-handler-not-element-download-handler,download-handler-response-missing,download-handler-body-unverified,download-save-capability-denied,download-save-write-failed,download-open-after-save-classified",
            "framework-download-handler-save-open-runtime-validation-pending",
            "large-file-streaming,parallel-chunk,retry-resume,real-backpressure,native-save-panel-user-flow,security-scoped-production-grants,open-after-save-user-flow,dev-mode-vite-parity,installer-packaging,durable-reconnect",
            "turn bounded DownloadHandler save/open evidence into large-file streaming and production file grants");
    private static final String GRID_LAZY_DATA_PROVIDER_PUBLISHED_METHOD =
            "setViewportRange";
    private static final String GRID_LAZY_DATA_PROVIDER_VIEWPORT_RANGE = "20:8";
    private static final String GRID_LAZY_DATA_PROVIDER_EXPECTED_RESULT =
            "callback-backed-grid-fetch-normalized-range-15-20-after-viewport-20-8";
    private static final String GRID_SCROLL_VIRTUALIZATION_CLIENT_API = "scrollToIndex";
    private static final int GRID_SCROLL_VIRTUALIZATION_REQUESTED_INDEX = 45;
    private static final String GRID_SCROLL_VIRTUALIZATION_EXPECTED_RESULT =
            "client-scroll-api-triggers-viewport-rpc-and-lazy-fetch-near-index-45";
    private static final String STREAM_RESOURCE_DYNAMIC_PATH_PREFIX =
            "VAADIN/dynamic/resource/";
    private static final String STREAM_RESOURCE_EXPECTED_FILE =
            "flow-ipc-framework-v1_50.txt";
    private static final String STREAM_RESOURCE_EXPECTED_CONTENT_TYPE =
            "text/plain; charset=UTF-8";
    private static final String STREAM_RESOURCE_EXPECTED_BODY_MARKER =
            "Flow IPC StreamResource v1.50";
    private static final String STREAM_RESOURCE_RANGE_EXPECTED_FILE =
            "flow-ipc-framework-v1_51.bin";
    private static final String STREAM_RESOURCE_RANGE_EXPECTED_CONTENT_TYPE =
            "application/octet-stream";
    private static final String STREAM_RESOURCE_RANGE_EXPECTED_BODY_MARKER =
            "Flow IPC Binary StreamResource v1.51";
    private static final String STREAM_RESOURCE_RANGE_HEADER = "bytes=0-255";
    private static final String STREAM_RESOURCE_CHUNKS_RANGE_PLAN =
            "bytes=0-255,bytes=256-511,bytes=512-767";
    private static final String UPLOAD_RECEIVER_DYNAMIC_PATH_PREFIX =
            "VAADIN/dynamic/resource/";
    private static final String UPLOAD_RECEIVER_EXPECTED_FILE =
            "flow-ipc-upload-v1_53.txt";
    private static final String UPLOAD_RECEIVER_EXPECTED_CONTENT_TYPE =
            "text/plain; charset=UTF-8";
    private static final String UPLOAD_RECEIVER_EXPECTED_BODY_MARKER =
            "Flow IPC Upload v1.53";
    private static final String UPLOAD_RECEIVER_TRANSPORT =
            "ipc-upload-request-raw-xhr-compatible";
    private static final String UPLOAD_MULTIPART_EXPECTED_FILE =
            "flow-ipc-upload-multipart-v1_54.txt";
    private static final String UPLOAD_MULTIPART_EXPECTED_CONTENT_TYPE =
            "text/plain; charset=UTF-8";
    private static final String UPLOAD_MULTIPART_EXPECTED_BODY_MARKER =
            "Flow IPC Upload Multipart v1.54";
    private static final String UPLOAD_MULTIPART_TRANSPORT =
            "ipc-upload-request-multipart-form-data";
    private static final String DOWNLOAD_HANDLER_SAVE_OPEN_EXPECTED_FILE =
            "flow-ipc-download-handler-v1_55.txt";
    private static final String DOWNLOAD_HANDLER_SAVE_OPEN_EXPECTED_CONTENT_TYPE =
            "text/plain; charset=UTF-8";
    private static final String DOWNLOAD_HANDLER_SAVE_OPEN_EXPECTED_BODY_MARKER =
            "Flow IPC DownloadHandler v1.55";
    private static final String DOWNLOAD_HANDLER_SAVE_OPEN_TRANSPORT =
            "ipc-resource-request-downloadhandler-to-fileTransfer.download";

    private FlowIpcDynamicResourceAttributes() {
    }

    static Map<String, Object> matrixAttributes(
            FlowIpcComponentMatrixAttributes.Context componentContext,
            Context dynamicContext) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        putGridLazyDataProviderAttributes(attributes, componentContext);
        putGridScrollVirtualizationAttributes(attributes, componentContext);
        putStreamResourceAttributes(attributes, dynamicContext);
        putStreamResourceRangeAttributes(attributes, dynamicContext);
        putStreamResourceChunksAttributes(attributes, dynamicContext);
        putUploadReceiverAttributes(attributes, dynamicContext);
        putUploadMultipartAttributes(attributes, dynamicContext);
        putDownloadHandlerSaveOpenAttributes(attributes, dynamicContext);
        return attributes;
    }

    private static void putGridLazyDataProviderAttributes(
            Map<String, Object> attributes,
            FlowIpcComponentMatrixAttributes.Context componentContext) {
        FlowIpcComponentMatrixAttributes.put(attributes, GRID_LAZY_DATA_PROVIDER, componentContext);
        attributes.put("gridLazyDataProviderPublishedMethod",
                GRID_LAZY_DATA_PROVIDER_PUBLISHED_METHOD);
        attributes.put("gridLazyDataProviderViewportRange",
                GRID_LAZY_DATA_PROVIDER_VIEWPORT_RANGE);
        attributes.put("gridLazyDataProviderExpectedResult",
                GRID_LAZY_DATA_PROVIDER_EXPECTED_RESULT);
        attributes.put("gridLazyDataProviderPublishedRpcObserved", false);
        attributes.put("gridLazyDataProviderCallbackFetchObserved", false);
    }

    private static void putGridScrollVirtualizationAttributes(
            Map<String, Object> attributes,
            FlowIpcComponentMatrixAttributes.Context componentContext) {
        FlowIpcComponentMatrixAttributes.put(attributes, GRID_SCROLL_VIRTUALIZATION,
                componentContext);
        attributes.put("gridScrollVirtualizationClientApi", GRID_SCROLL_VIRTUALIZATION_CLIENT_API);
        attributes.put("gridScrollVirtualizationRequestedIndex",
                GRID_SCROLL_VIRTUALIZATION_REQUESTED_INDEX);
        attributes.put("gridScrollVirtualizationExpectedResult",
                GRID_SCROLL_VIRTUALIZATION_EXPECTED_RESULT);
        attributes.put("gridScrollVirtualizationClientApiAvailable", false);
        attributes.put("gridScrollVirtualizationClientApiInvoked", false);
        attributes.put("gridScrollVirtualizationPublishedRpcObserved", false);
        attributes.put("gridScrollVirtualizationCallbackFetchObserved", false);
        attributes.put("gridScrollVirtualizationFetchRange", "");
        attributes.put("gridScrollVirtualizationReportEventObserved", false);
        attributes.put("gridScrollVirtualizationAcceptedBlockerObserved", false);
    }

    private static void putStreamResourceAttributes(Map<String, Object> attributes,
            Context context) {
        put(attributes, STREAM_RESOURCE, context);
        attributes.put("streamResourceDynamicPathPrefix", STREAM_RESOURCE_DYNAMIC_PATH_PREFIX);
        attributes.put("streamResourceExpectedFile", STREAM_RESOURCE_EXPECTED_FILE);
        attributes.put("streamResourceExpectedContentType", STREAM_RESOURCE_EXPECTED_CONTENT_TYPE);
        attributes.put("streamResourceExpectedBodyMarker", STREAM_RESOURCE_EXPECTED_BODY_MARKER);
        attributes.put("streamResourceAnchorHrefObserved", false);
        attributes.put("streamResourceRequestObserved", false);
        attributes.put("streamResourceResponseObserved", false);
        attributes.put("streamResourceBodyVerified", false);
    }

    private static void putStreamResourceRangeAttributes(Map<String, Object> attributes,
            Context context) {
        put(attributes, STREAM_RESOURCE_RANGE, context);
        attributes.put("streamResourceRangeDynamicPathPrefix", STREAM_RESOURCE_DYNAMIC_PATH_PREFIX);
        attributes.put("streamResourceRangeExpectedFile", STREAM_RESOURCE_RANGE_EXPECTED_FILE);
        attributes.put("streamResourceRangeExpectedContentType",
                STREAM_RESOURCE_RANGE_EXPECTED_CONTENT_TYPE);
        attributes.put("streamResourceRangeExpectedBodyMarker",
                STREAM_RESOURCE_RANGE_EXPECTED_BODY_MARKER);
        attributes.put("streamResourceRangeHeader", STREAM_RESOURCE_RANGE_HEADER);
        attributes.put("streamResourceRangeAnchorHrefObserved", false);
        attributes.put("streamResourceRangeRequestObserved", false);
        attributes.put("streamResourceRangeResponseObserved", false);
        attributes.put("streamResourceRangeBodyVerified", false);
        attributes.put("streamResourceRangeFullBodyOmitted", true);
        attributes.put("streamResourceRangeResponseMode", "range-slice");
    }

    private static void putStreamResourceChunksAttributes(Map<String, Object> attributes,
            Context context) {
        put(attributes, STREAM_RESOURCE_CHUNKS, context);
        attributes.put("streamResourceChunksDynamicPathPrefix", STREAM_RESOURCE_DYNAMIC_PATH_PREFIX);
        attributes.put("streamResourceChunksExpectedFile", STREAM_RESOURCE_RANGE_EXPECTED_FILE);
        attributes.put("streamResourceChunksExpectedContentType",
                STREAM_RESOURCE_RANGE_EXPECTED_CONTENT_TYPE);
        attributes.put("streamResourceChunksExpectedBodyMarker",
                STREAM_RESOURCE_RANGE_EXPECTED_BODY_MARKER);
        attributes.put("streamResourceChunksRangePlan", STREAM_RESOURCE_CHUNKS_RANGE_PLAN);
        attributes.put("streamResourceChunksChunkCount", 3);
        attributes.put("streamResourceChunksChunkSize", 256);
        attributes.put("streamResourceChunksExpectedAggregateBytes", 768);
        attributes.put("streamResourceChunksBackpressureWindow", 1);
        attributes.put("streamResourceChunksAnchorHrefObserved", false);
        attributes.put("streamResourceChunksRequestObserved", false);
        attributes.put("streamResourceChunksResponseObserved", false);
        attributes.put("streamResourceChunksBodyVerified", false);
        attributes.put("streamResourceChunksFullBodyOmitted", true);
        attributes.put("streamResourceChunksResponseMode", "range-chunk-sequential");
    }

    private static void putUploadReceiverAttributes(Map<String, Object> attributes,
            Context context) {
        put(attributes, UPLOAD_RECEIVER, context);
        attributes.put("uploadReceiverDynamicPathPrefix", UPLOAD_RECEIVER_DYNAMIC_PATH_PREFIX);
        attributes.put("uploadReceiverExpectedFile", UPLOAD_RECEIVER_EXPECTED_FILE);
        attributes.put("uploadReceiverExpectedContentType", UPLOAD_RECEIVER_EXPECTED_CONTENT_TYPE);
        attributes.put("uploadReceiverExpectedBodyMarker", UPLOAD_RECEIVER_EXPECTED_BODY_MARKER);
        attributes.put("uploadReceiverTransport", UPLOAD_RECEIVER_TRANSPORT);
        attributes.put("uploadReceiverTargetHrefObserved", false);
        attributes.put("uploadReceiverRequestObserved", false);
        attributes.put("uploadReceiverResponseObserved", false);
        attributes.put("uploadReceiverPatchObserved", false);
        attributes.put("uploadReceiverBodyVerified", false);
        attributes.put("uploadReceiverMultipartParity", false);
    }

    private static void putUploadMultipartAttributes(Map<String, Object> attributes,
            Context context) {
        put(attributes, UPLOAD_MULTIPART, context);
        attributes.put("uploadMultipartDynamicPathPrefix", UPLOAD_RECEIVER_DYNAMIC_PATH_PREFIX);
        attributes.put("uploadMultipartExpectedFile", UPLOAD_MULTIPART_EXPECTED_FILE);
        attributes.put("uploadMultipartExpectedContentType", UPLOAD_MULTIPART_EXPECTED_CONTENT_TYPE);
        attributes.put("uploadMultipartExpectedBodyMarker", UPLOAD_MULTIPART_EXPECTED_BODY_MARKER);
        attributes.put("uploadMultipartTransport", UPLOAD_MULTIPART_TRANSPORT);
        attributes.put("uploadMultipartTargetHrefObserved", false);
        attributes.put("uploadMultipartRequestObserved", false);
        attributes.put("uploadMultipartResponseObserved", false);
        attributes.put("uploadMultipartPatchObserved", false);
        attributes.put("uploadMultipartBodyVerified", false);
        attributes.put("uploadMultipartProgressObserved", false);
        attributes.put("uploadMultipartCancelProbeObserved", false);
        attributes.put("uploadMultipartParity", true);
    }

    private static void putDownloadHandlerSaveOpenAttributes(Map<String, Object> attributes,
            Context context) {
        put(attributes, DOWNLOAD_HANDLER_SAVE_OPEN, context);
        attributes.put("downloadHandlerSaveOpenDynamicPathPrefix", STREAM_RESOURCE_DYNAMIC_PATH_PREFIX);
        attributes.put("downloadHandlerSaveOpenExpectedFile", DOWNLOAD_HANDLER_SAVE_OPEN_EXPECTED_FILE);
        attributes.put("downloadHandlerSaveOpenExpectedContentType",
                DOWNLOAD_HANDLER_SAVE_OPEN_EXPECTED_CONTENT_TYPE);
        attributes.put("downloadHandlerSaveOpenExpectedBodyMarker",
                DOWNLOAD_HANDLER_SAVE_OPEN_EXPECTED_BODY_MARKER);
        attributes.put("downloadHandlerSaveOpenTransport", DOWNLOAD_HANDLER_SAVE_OPEN_TRANSPORT);
        attributes.put("downloadHandlerSaveOpenAnchorHrefObserved", false);
        attributes.put("downloadHandlerSaveOpenRequestObserved", false);
        attributes.put("downloadHandlerSaveOpenResponseObserved", false);
        attributes.put("downloadHandlerSaveOpenCapabilityResultObserved", false);
        attributes.put("downloadHandlerSaveOpenBodyVerified", false);
        attributes.put("downloadHandlerSaveOpenFileWritten", false);
        attributes.put("downloadHandlerSaveOpenPathRedacted", true);
        attributes.put("downloadHandlerSaveOpenSecurityScoped", false);
    }

    static void put(Map<String, Object> attributes, Spec spec, Context context) {
        String prefix = spec.prefix();
        attributes.put(prefix + "Version", spec.version());
        attributes.put(prefix + "Source", spec.source());
        attributes.put(prefix + "Scope", spec.scope());
        attributes.put(prefix + "AppShape", context.appShape());
        attributes.put(prefix + "JavaClasses", spec.javaClasses());
        attributes.put(prefix + "ComponentIds", spec.componentIds());
        attributes.put(prefix + "SelectedArtifacts", context.selectedArtifacts());
        attributes.put(prefix + "SelectedArtifactCount",
                FlowIpcJavaScriptModuleGraph.csvValues(context.selectedArtifacts()).size());
        attributes.put(prefix + "GeneratedImportsOwnerSource", context.generatedImportsOwnerSource());
        attributes.put(prefix + "GeneratedImportsOwnerArtifact", context.generatedImportsOwnerArtifact());
        attributes.put(prefix + "ProductionSupportArtifact", context.productionSupportArtifact());
        attributes.put(prefix + "ProductionSupportSha256", context.productionSupportSha256());
        attributes.put(prefix + "ProductionSupportBytes", context.productionSupportBytes());
        attributes.put(prefix + "ProductionEntryArtifact", context.productionEntryArtifact());
        attributes.put(prefix + "MavenGoal", context.mavenGoal());
        attributes.put(prefix + "BuildStatus", context.buildStatus());
        attributes.put(prefix + "OutputUsability", context.outputUsability());
        attributes.put(prefix + "FrameworkAppRequired", true);
        attributes.put(prefix + "FrameworkAppObserved", context.frameworkApp());
        attributes.put(prefix + "CliContractEnabled", "1".equals(System.getenv(spec.cliFlag())));
        attributes.put(prefix + "ManifestReady", context.runtimeHandoffReady() && context.frameworkApp());
        attributes.put(prefix + "RuntimeHandoffRequired", true);
        attributes.put(prefix + "RuntimeHandoffReady", context.runtimeHandoffReady());
        attributes.put(prefix + "NoLocalhost", true);
        attributes.put(prefix + "FixtureSubstituted", false);
        attributes.put(prefix + "AcceptableFailures", spec.acceptableFailures());
        attributes.put(prefix + "FailureLayer", context.frameworkApp()
                ? spec.frameworkPendingFailureLayer()
                : "framework-validation-app-not-selected");
        attributes.put(prefix + "RemainingBlockers", spec.remainingBlockers());
        attributes.put(prefix + "NextResolutionStep", spec.nextResolutionStep());
    }

    record Spec(
            String prefix,
            String version,
            String source,
            String scope,
            String javaClasses,
            String componentIds,
            String cliFlag,
            String acceptableFailures,
            String frameworkPendingFailureLayer,
            String remainingBlockers,
            String nextResolutionStep) {
    }

    record Context(
            String appShape,
            boolean frameworkApp,
            String selectedArtifacts,
            String generatedImportsOwnerSource,
            String generatedImportsOwnerArtifact,
            String productionSupportArtifact,
            String productionSupportSha256,
            int productionSupportBytes,
            String productionEntryArtifact,
            String mavenGoal,
            String buildStatus,
            String outputUsability,
            boolean runtimeHandoffReady) {
    }
}
