import AppKit
import Foundation

extension FlowIpcAcceptanceRuntimeObserver {
    func scheduleExternalFileDropAcceptance(
        host: WebViewHost,
        context: FlowIpcWindowContext,
        timeoutSeconds: Double
    ) {
        FlowIpcExternalFileDropAcceptance(
            host: host, context: context,
            externalWaitSeconds: timeoutSeconds).start()
    }
}

// Observes an externally initiated file drop. It neither reads the source file
// nor creates drag sessions, File/FileList/DataTransfer objects, or upload input.
// A trusted DOM drop cannot identify Finder; provenance is a separate receipt.
private final class FlowIpcExternalFileDropAcceptance {
    private enum Phase { case root, route, prepared, waiting, drain }

    private let host: WebViewHost
    private let context: FlowIpcWindowContext
    private let metadata: Metadata?
    private let externalWaitSeconds: Double
    private var deadline = Date().addingTimeInterval(30)
    private var deadlineWorkItem: DispatchWorkItem?
    private var phase = Phase.root
    private var finished = false
    private var processIdentifier: Int32 = 0
    private var readyRequestBaseline = 0
    private var readyResponseBaseline = 0
    private var dropRecorded = false
    private var drainCounts: [Int] = []
    private var drainQuietPollCount = 0
    private var lastDiagnostic = ""
    private var lastSnapshot: Snapshot?

    private static let prefix = "framework-flow-external-file-drop-"
    private static let configStatus = "External file drop config: format=MULTIPART, autoUpload=true, dropAllowed=true, maxFiles=1, maxFileSize=1048576"
    private static let pendingUpload = "External file drop upload: pending"
    private static let pendingResponse = "External file drop response: pending"
    private static let pendingListeners = "External file drop listeners: allFinished=0, allFinishedClient=false, rejected=0, removed=0"
    private static let completedListeners = "External file drop listeners: allFinished=1, allFinishedClient=false, rejected=0, removed=0"

    init(host: WebViewHost, context: FlowIpcWindowContext, externalWaitSeconds: Double) {
        self.host = host
        self.context = context
        self.externalWaitSeconds = externalWaitSeconds
        self.metadata = Metadata(environment: ProcessInfo.processInfo.environment)
    }

    func start() {
        guard metadata != nil, externalWaitSeconds.isFinite,
              externalWaitSeconds > 0, externalWaitSeconds <= 600 else {
            fail("invalid-run-metadata-or-timeout"); return
        }
        armDeadline(after: 30)
        poll()
    }

    private func poll() {
        guard !finished else { return }
        guard Date() < deadline else { fail("deadline", details: lastSnapshot?.payload ?? [:]); return }
        context.webView.evaluateJavaScript(Self.inspectionScript) { result, error in
            guard !self.finished else { return }
            guard error == nil, let json = result as? String,
                  let data = json.data(using: .utf8),
                  let state = try? JSONDecoder().decode(Snapshot.self, from: data) else {
                self.diagnostic("snapshot-unavailable"); self.schedule(); return
            }
            self.lastSnapshot = state
            guard state.errorFree else {
                self.fail("browser-or-observer-error", details: state.payload); return
            }
            guard state.browserWindowId == self.context.scope.windowId,
                  state.browserSessionId == self.context.scope.sessionId,
                  state.browserUiId == self.context.scope.uiId,
                  self.context.recoveryGeneration == 0,
                  self.processIdentifier == 0 || self.host.sidecarProcessIdentifier == self.processIdentifier else {
                self.fail("runtime-or-scope-changed", details: state.payload); return
            }
            self.advance(state)
        }
    }

    private func advance(_ state: Snapshot) {
        switch phase {
        case .root:
            guard context.navigationFinished, state.firstMeaningfulPaint,
                  state.rootRouteAvailable, state.flowClientCount > 0,
                  let pid = host.sidecarProcessIdentifier, pid > 0 else {
                diagnostic("root-not-ready"); schedule(); return
            }
            processIdentifier = pid
            guard positionWindow() else { fail("window-positioning-unavailable"); return }
            evaluate("""
            (() => {
              const trigger = document.getElementById('\(Self.prefix)open-route');
              if (!trigger) return false;
              window.VaadinDesktopExternalFileDrop = { browserProgrammaticStages: ['navigate'] };
              trigger.click();
              return true;
            })()
            """) {
                self.phase = .route
                self.schedule()
            }
        case .route:
            guard state.routeReady, state.uploadStatus == Self.pendingUpload,
                  state.responseStatus == Self.pendingResponse,
                  state.listenerStatus == Self.pendingListeners else {
                diagnostic("dedicated-route-not-ready"); schedule(); return
            }
            evaluate(Self.installObserverScript) {
                self.phase = .prepared
                self.schedule()
            }
        case .prepared:
            guard state.routeReady, state.observerInstalled, state.dropTargetVisible,
                  state.balanced, state.dropEventCount == 0,
                  state.uploadRequestEventCount == 0, state.uploadSuccessEventCount == 0,
                  state.remainingFileCount == 0, state.eventOrder.isEmpty,
                  state.uploadStatus == Self.pendingUpload,
                  state.responseStatus == Self.pendingResponse,
                  state.listenerStatus == Self.pendingListeners else {
                diagnostic("external-drop-target-not-ready"); schedule(); return
            }
            readyRequestBaseline = state.requestCount
            readyResponseBaseline = state.responseCount
            armDeadline(after: externalWaitSeconds)
            phase = .waiting
            record("readyForExternalDrop", state: state)
            schedule()
        case .waiting:
            guard state.routeReady, state.observerInstalled else {
                fail("external-drop-route-lost", details: state.payload); return
            }
            if state.dropEventCount > 0 && !dropRecorded {
                guard dropMatches(state) else {
                    fail("external-drop-metadata", details: state.payload); return
                }
                dropRecorded = true
                record("dropObserved", state: state)
            }
            guard dropRecorded, completed(state), state.balanced else {
                diagnostic(dropRecorded ? "await-upload-and-java" : "await-external-file-drop")
                schedule(); return
            }
            record("uploadObserved", state: state)
            phase = .drain
            schedule()
        case .drain:
            guard completed(state), state.balanced else {
                drainCounts = []; drainQuietPollCount = 0
                diagnostic("upload-drain-not-ready"); schedule(); return
            }
            let counts = [state.requestCount, state.responseCount, state.pushMessageCount]
            if counts == drainCounts { drainQuietPollCount += 1 }
            else { drainCounts = counts; drainQuietPollCount = 1 }
            guard drainQuietPollCount >= 3 else { schedule(after: 0.3); return }
            finish(state)
        }
    }

    private func dropMatches(_ state: Snapshot) -> Bool {
        guard let metadata else { return false }
        return state.dropEventCount == 1 && state.dropTrustedEventCount == 1
            && state.dropFileCount == 1 && state.dropTargetOwned
            && state.dropTypes.contains("Files")
            && state.dropFileName == metadata.fileName && state.dropFileBytes == metadata.fileBytes
            && state.dropRequestBaseline >= readyRequestBaseline
            && state.dropResponseBaseline >= readyResponseBaseline
    }

    private func completed(_ state: Snapshot) -> Bool {
        guard let metadata else { return false }
        // File.type may be empty for a native .bin. Multipart then uses the
        // standard octet-stream fallback; this is not an expected-value fill.
        let partContentType = state.dropFileType.isEmpty ? "application/octet-stream" : state.dropFileType
        let upload = "External file drop upload: handlerCalls=1, handlerFailures=0, fileName=\(metadata.fileName), declaredBytes=\(metadata.fileBytes), bytesRead=\(metadata.fileBytes), sha256=\(metadata.fileSha256), contentType=\(partContentType), requestContentType=multipart/form-data, requestMethod=POST, multipart=true"
        let response = "External file drop response: responseCalls=1, successfulResponses=1, failedResponses=0, success=true, acceptedFiles=1, acceptedFile=\(metadata.fileName), rejectedFiles=0, exception=false"
        return state.routeReady && state.observerInstalled && state.errorFree && dropMatches(state)
            && state.uploadRequestEventCount == 1 && state.multipartRequestBody
            && state.formDataEntryCount == 1 && !state.formDataFieldName.isEmpty
            && state.formDataFileName == metadata.fileName && state.formDataFileBytes == metadata.fileBytes
            && state.formDataFileType == state.dropFileType
            && state.requestFileName == metadata.fileName && state.requestFileBytes == metadata.fileBytes
            && state.requestFileType == state.dropFileType && state.requestFileSameAsDrop
            && state.uploadSuccessEventCount == 1 && state.successFileSameAsDrop
            && state.eventOrder == ["drop", "upload-request", "upload-success"]
            && state.uploadStatus == upload && state.responseStatus == response
            && state.listenerStatus == Self.completedListeners
            && state.remainingFileCount == 1 && state.remainingFileName == metadata.fileName
            && state.remainingFileBytes == metadata.fileBytes && state.remainingFileComplete
            && state.requestCount > state.dropRequestBaseline && state.responseCount > state.dropResponseBaseline
            && state.browserProgrammaticStages == ["navigate", "position-drop-target"]
    }

    private func frame(_ state: Snapshot) -> [String: Any] {
        var payload = state.payload
        payload.merge(context.scope.tracePayload) { _, replacement in replacement }
        payload["hostProcessIdentifier"] = getpid()
        payload["processIdentifier"] = processIdentifier
        payload["recoveryGeneration"] = context.recoveryGeneration
        payload["runId"] = metadata?.runId ?? ""
        payload["expectedFileName"] = metadata?.fileName ?? ""
        payload["expectedFileBytes"] = metadata?.fileBytes ?? -1
        payload["expectedFileSha256"] = metadata?.fileSha256 ?? ""
        payload["sourceApplicationVerified"] = false
        payload["externalProvenanceRequired"] = true
        payload["generatedInput"] = false
        payload["readyRequestBaseline"] = readyRequestBaseline
        payload["readyResponseBaseline"] = readyResponseBaseline
        payload["drainQuietPollCount"] = drainQuietPollCount
        payload["externalWaitTimeoutSeconds"] = externalWaitSeconds
        payload["nativeSetupStages"] = ["position-window-right"]
        payload["nativeCoordinateSpace"] = "appkit-screen-points-bottom-left"
        payload["windowFrame"] = Self.rectPayload(context.window.frame)
        if let screen = context.window.screen ?? NSScreen.main {
            payload["screenFrame"] = Self.rectPayload(screen.frame)
            payload["screenVisibleFrame"] = Self.rectPayload(screen.visibleFrame)
            payload["screenBackingScaleFactor"] = screen.backingScaleFactor
        }
        let target = state.dropTargetViewRect
        let webViewRect = NSRect(x: target.x,
            y: context.webView.isFlipped ? target.y
                : context.webView.bounds.height - target.y - target.height,
            width: target.width, height: target.height)
        let windowRect = context.webView.convert(webViewRect, to: nil)
        payload["dropTargetScreenRect"] = Self.rectPayload(context.window.convertToScreen(windowRect))
        return payload
    }

    private static func rectPayload(_ rect: NSRect) -> [String: Double] {
        ["x": Double(rect.origin.x), "y": Double(rect.origin.y),
         "width": Double(rect.width), "height": Double(rect.height)]
    }

    private func record(_ type: String, state: Snapshot) {
        trace("recv", "externalFileDrop." + type, frame(state))
    }

    private func finish(_ state: Snapshot) {
        var payload = frame(state)
        payload.merge([
            "complete": true, "errorFree": true,
            "standardBundleMode": state.firstMeaningfulPaint,
            "rootRendered": state.routeReady, "flowClientOwned": state.flowClientCount > 0,
            "opaqueRequestBridgeObserved": state.requestCount > readyRequestBaseline,
            "opaqueResponseBridgeObserved": state.responseCount > readyResponseBaseline,
            "pushObserved": false, "trustedFileDropObserved": true,
            "standardMultipartObserved": true, "javaExactBytesAndHashObserved": true,
            "javaResponseObserved": true, "javaListenersObserved": true
        ]) { _, replacement in replacement }
        finished = true
        cancelDeadline()
        trace("recv", "externalFileDrop.frameworkAcceptance", payload)
        context.window.close()
    }

    private func positionWindow() -> Bool {
        guard let screen = context.window.screen ?? NSScreen.main else { return false }
        let visible = screen.visibleFrame
        let width = min(visible.width - 16, max(640, visible.width / 2 - 12))
        let height = min(context.window.frame.height, visible.height - 16)
        let frame = NSRect(x: visible.maxX - width - 8, y: visible.maxY - height - 8,
            width: width, height: height)
        context.window.setFrame(frame, display: true)
        return true
    }

    private func evaluate(_ script: String, completion: @escaping () -> Void) {
        context.webView.evaluateJavaScript(script) { result, error in
            guard !self.finished else { return }
            guard error == nil, result as? Bool == true else {
                self.fail("observer-or-navigation-setup"); return
            }
            completion()
        }
    }

    private func schedule(after delay: Double = 0.25) {
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) { self.poll() }
    }

    private func armDeadline(after seconds: Double) {
        cancelDeadline()
        deadline = Date().addingTimeInterval(seconds)
        // Independent of WebKit's completion callback, while the AppKit main
        // queue is responsive. This is not a watchdog for a frozen host process.
        let workItem = DispatchWorkItem { [self] in
            guard !finished else { return }
            fail("deadline", details: lastSnapshot?.payload ?? [:])
        }
        deadlineWorkItem = workItem
        DispatchQueue.main.asyncAfter(deadline: .now() + seconds, execute: workItem)
    }

    private func cancelDeadline() {
        deadlineWorkItem?.cancel()
        deadlineWorkItem = nil
    }

    private func diagnostic(_ reason: String) {
        let signature = "\(phase):\(reason):\(NSApplication.shared.isActive):\(context.window.isKeyWindow)"
        guard signature != lastDiagnostic else { return }
        lastDiagnostic = signature
        var payload = context.scope.tracePayload
        payload["phase"] = String(describing: phase)
        payload["reason"] = reason
        payload["runId"] = metadata?.runId ?? ""
        payload["activeApplication"] = NSApplication.shared.isActive
        payload["keyWindow"] = context.window.isKeyWindow
        if let state = lastSnapshot {
            payload["routeReady"] = state.routeReady
            payload["dropTargetVisible"] = state.dropTargetVisible
            payload["dropEventCount"] = state.dropEventCount
            payload["uploadRequestEventCount"] = state.uploadRequestEventCount
            payload["uploadSuccessEventCount"] = state.uploadSuccessEventCount
            payload["requestCount"] = state.requestCount
            payload["responseCount"] = state.responseCount
        }
        trace("recv", "externalFileDrop.driverDiagnostic", payload)
    }

    private func fail(_ code: String, details: [String: Any] = [:]) {
        guard !finished else { return }
        finished = true
        cancelDeadline()
        var payload = details
        payload["code"] = "externalFileDrop-" + code
        payload["phase"] = String(describing: phase)
        payload["runId"] = metadata?.runId ?? ""
        trace("send", "error", payload)
        context.window.close()
    }

    private struct Metadata {
        let runId: String
        let fileName: String
        let fileBytes: Int
        let fileSha256: String

        init?(environment: [String: String]) {
            let prefix = "VAADIN_DESKTOP_FLOW_IPC_EXTERNAL_DROP_"
            guard let runId = environment[prefix + "RUN_ID"],
                  runId.range(of: "^[A-Za-z0-9][A-Za-z0-9._-]{5,95}$", options: .regularExpression) != nil,
                  let fileName = environment[prefix + "FILE_NAME"],
                  fileName.range(of: "^[A-Za-z0-9][A-Za-z0-9._-]{0,119}$", options: .regularExpression) != nil,
                  let bytes = environment[prefix + "FILE_BYTES"], let fileBytes = Int(bytes),
                  fileBytes > 0, fileBytes <= 1048576,
                  let fileSha256 = environment[prefix + "FILE_SHA256"],
                  fileSha256.range(of: "^[0-9a-f]{64}$", options: .regularExpression) != nil else { return nil }
            self.runId = runId
            self.fileName = fileName
            self.fileBytes = fileBytes
            self.fileSha256 = fileSha256
        }
    }

    private struct Rect: Codable { let x: Double; let y: Double; let width: Double; let height: Double }
    private struct Snapshot: Codable {
        let rootRouteAvailable: Bool
        let routeReady: Bool
        let firstMeaningfulPaint: Bool
        let requestCount: Int
        let responseCount: Int
        let pushMessageCount: Int
        let flowClientCount: Int
        let browserWindowId: String
        let browserSessionId: String
        let browserUiId: String
        let configStatus: String
        let uploadStatus: String
        let responseStatus: String
        let listenerStatus: String
        let uploadFormat: String
        let autoUpload: Bool
        let dropAllowed: Bool
        let maxFiles: Int
        let maxFileSize: Int
        let observerInstalled: Bool
        let dropTargetVisible: Bool
        let dropTargetViewRect: Rect
        let dropEventCount: Int
        let dropTrustedEventCount: Int
        let dropFileCount: Int
        let dropTargetOwned: Bool
        let dropTypes: [String]
        let dropFileName: String
        let dropFileBytes: Int
        let dropFileType: String
        let dropRequestBaseline: Int
        let dropResponseBaseline: Int
        let uploadRequestEventCount: Int
        let multipartRequestBody: Bool
        let formDataEntryCount: Int
        let formDataFieldName: String
        let formDataFileName: String
        let formDataFileBytes: Int
        let formDataFileType: String
        let requestFileName: String
        let requestFileBytes: Int
        let requestFileType: String
        let requestFileSameAsDrop: Bool
        let uploadSuccessEventCount: Int
        let successFileSameAsDrop: Bool
        let uploadErrorEventCount: Int
        let rejectEventCount: Int
        let abortEventCount: Int
        let removeEventCount: Int
        let eventOrder: [String]
        let remainingFileCount: Int
        let remainingFileName: String
        let remainingFileBytes: Int
        let remainingFileComplete: Bool
        let browserProgrammaticStages: [String]
        let browserError: String
        let requestError: String
        let responseError: String
        let pushError: String
        let markerError: String

        var balanced: Bool { requestCount > 0 && requestCount == responseCount }
        var errorFree: Bool {
            browserError.isEmpty && requestError.isEmpty && responseError.isEmpty
                && pushError.isEmpty && markerError.isEmpty && uploadErrorEventCount == 0
                && rejectEventCount == 0 && abortEventCount == 0 && removeEventCount == 0
        }
        var payload: [String: Any] {
            guard let data = try? JSONEncoder().encode(self),
                  let value = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return [:] }
            return value
        }
    }

    private static let installObserverScript = """
    (() => {
      const marker = window.VaadinDesktopExternalFileDrop;
      const upload = document.getElementById('framework-flow-external-file-drop-upload');
      if (!marker || !upload || marker.observerInstalled) return false;
      marker.observerInstalled = true;
      marker.eventOrder = [];
      marker.dropEventCount = 0;
      marker.dropTrustedEventCount = 0;
      marker.uploadRequestEventCount = 0;
      marker.uploadSuccessEventCount = 0;
      marker.uploadErrorEventCount = 0;
      marker.rejectEventCount = 0;
      marker.abortEventCount = 0;
      marker.removeEventCount = 0;
      document.addEventListener('drop', event => {
        const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
        marker.dropEventCount++;
        marker.dropTrustedEventCount += event.isTrusted === true ? 1 : 0;
        marker.eventOrder.push('drop');
        marker.dropTargetOwned = event.composedPath().includes(upload);
        const files = Array.from(event.dataTransfer?.files || []);
        marker.dropFileCount = files.length;
        marker.dropTypes = Array.from(event.dataTransfer?.types || []).map(String);
        marker.droppedFile = files[0] || null;
        marker.dropFileName = String(files[0]?.name || '');
        marker.dropFileBytes = Number(files[0]?.size ?? -1);
        marker.dropFileType = String(files[0]?.type || '');
        marker.dropRequestBaseline = Number(diagnostics.standardBundleOpaqueRequestCount || 0);
        marker.dropResponseBaseline = Number(diagnostics.standardBundleOpaqueResponseCount || 0);
        if (!marker.dropTargetOwned || files.length !== 1 || !(files[0] instanceof File)) {
          marker.markerError = 'drop-target-or-file-list';
        }
      }, true);
      upload.addEventListener('upload-request', event => {
        marker.uploadRequestEventCount++;
        marker.eventOrder.push('upload-request');
        const file = event.detail?.file;
        const body = event.detail?.requestBody;
        marker.requestFileName = String(file?.name || '');
        marker.requestFileBytes = Number(file?.size ?? -1);
        marker.requestFileType = String(file?.type || '');
        marker.requestFileSameAsDrop = !!file && file === marker.droppedFile;
        marker.multipartRequestBody = event.detail?.uploadFormat === 'multipart' && body instanceof FormData;
        const entries = body instanceof FormData ? Array.from(body.entries()) : [];
        marker.formDataEntryCount = entries.length;
        const entry = entries[0];
        marker.formDataFieldName = String(entry?.[0] || '');
        marker.formDataFileName = String(entry?.[1]?.name || '');
        marker.formDataFileBytes = Number(entry?.[1]?.size ?? -1);
        marker.formDataFileType = String(entry?.[1]?.type || '');
        if (entries.length !== 1 || !(entry?.[1] instanceof File)) marker.markerError = 'multipart-file-entry';
      });
      upload.addEventListener('upload-success', event => {
        marker.uploadSuccessEventCount++;
        marker.eventOrder.push('upload-success');
        marker.successFileSameAsDrop = !!event.detail?.file && event.detail.file === marker.droppedFile;
      });
      upload.addEventListener('upload-error', () => { marker.uploadErrorEventCount++; });
      upload.addEventListener('file-reject', () => { marker.rejectEventCount++; });
      upload.addEventListener('upload-abort', () => { marker.abortEventCount++; });
      upload.addEventListener('file-remove', () => { marker.removeEventCount++; });
      upload.scrollIntoView({block:'center',inline:'nearest'});
      marker.browserProgrammaticStages.push('position-drop-target');
      return true;
    })()
    """

    private static let inspectionScript = """
    (() => {
      const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
      const marker = window.VaadinDesktopExternalFileDrop || {};
      const scope = window.VaadinDesktopHostContext || {};
      const upload = document.getElementById('framework-flow-external-file-drop-upload');
      const text = suffix => String(document.getElementById('framework-flow-external-file-drop-' + suffix)?.textContent || '').replace(/\\s+/g,' ').trim();
      const clients = Object.keys(window.Vaadin?.Flow?.clients || {}).filter(name => name !== 'TypeScript');
      const files = Array.isArray(upload?.files) ? upload.files : [];
      const rect = upload?.getBoundingClientRect();
      const visible = !!rect && rect.width > 0 && rect.height > 0
        && rect.left >= 0 && rect.top >= 0 && rect.right <= innerWidth && rect.bottom <= innerHeight;
      const array = key => Array.isArray(marker[key]) ? marker[key] : [];
      const number = (key, fallback = 0) => Number(marker[key] ?? fallback);
      const string = key => String(marker[key] || '');
      return JSON.stringify({
        rootRouteAvailable: !!document.getElementById('framework-flow-external-file-drop-open-route'),
        routeReady: !!document.getElementById('framework-flow-external-file-drop-route') && !!upload
          && text('config-status') === '\(configStatus)' && upload.uploadFormat === 'multipart'
          && upload.noAuto !== true && upload.nodrop !== true && upload.maxFiles === 1 && upload.maxFileSize === 1048576,
        firstMeaningfulPaint: diagnostics.standardBundleFirstMeaningfulPaintReported === true,
        requestCount: Number(diagnostics.standardBundleOpaqueRequestCount || 0),
        responseCount: Number(diagnostics.standardBundleOpaqueResponseCount || 0),
        pushMessageCount: Number(diagnostics.standardBundlePushMessageCount || 0), flowClientCount: clients.length,
        browserWindowId: String(scope.windowId || ''), browserSessionId: String(scope.sessionId || ''), browserUiId: String(scope.uiId || ''),
        configStatus: text('config-status'), uploadStatus: text('upload-status'), responseStatus: text('response-status'), listenerStatus: text('listener-status'),
        uploadFormat: String(upload?.uploadFormat || ''), autoUpload: !!upload && upload.noAuto !== true,
        dropAllowed: !!upload && upload.nodrop !== true, maxFiles: Number(upload?.maxFiles ?? -1), maxFileSize: Number(upload?.maxFileSize ?? -1),
        observerInstalled: marker.observerInstalled === true, dropTargetVisible: visible,
        dropTargetViewRect: {x:rect?.x || 0,y:rect?.y || 0,width:rect?.width || 0,height:rect?.height || 0},
        dropEventCount: number('dropEventCount'), dropTrustedEventCount: number('dropTrustedEventCount'),
        dropFileCount: number('dropFileCount'), dropTargetOwned: marker.dropTargetOwned === true,
        dropTypes: array('dropTypes'), dropFileName: string('dropFileName'), dropFileBytes: number('dropFileBytes',-1), dropFileType: string('dropFileType'),
        dropRequestBaseline: number('dropRequestBaseline'), dropResponseBaseline: number('dropResponseBaseline'),
        uploadRequestEventCount: number('uploadRequestEventCount'), multipartRequestBody: marker.multipartRequestBody === true,
        formDataEntryCount: number('formDataEntryCount'), formDataFieldName: string('formDataFieldName'),
        formDataFileName: string('formDataFileName'), formDataFileBytes: number('formDataFileBytes',-1), formDataFileType: string('formDataFileType'),
        requestFileName: string('requestFileName'), requestFileBytes: number('requestFileBytes',-1), requestFileType: string('requestFileType'),
        requestFileSameAsDrop: marker.requestFileSameAsDrop === true,
        uploadSuccessEventCount: number('uploadSuccessEventCount'), successFileSameAsDrop: marker.successFileSameAsDrop === true,
        uploadErrorEventCount: number('uploadErrorEventCount'), rejectEventCount: number('rejectEventCount'), abortEventCount: number('abortEventCount'), removeEventCount: number('removeEventCount'),
        eventOrder: array('eventOrder'), remainingFileCount: files.length, remainingFileName: String(files[0]?.name || ''),
        remainingFileBytes: Number(files[0]?.size ?? -1), remainingFileComplete: files[0]?.complete === true,
        browserProgrammaticStages: array('browserProgrammaticStages'),
        browserError: String(diagnostics.standardBundleBrowserError || ''),
        requestError: String(diagnostics.standardBundleOpaqueRequestError || ''),
        responseError: String(diagnostics.standardBundleOpaqueResponseError || ''),
        pushError: String(diagnostics.standardBundlePushError || ''), markerError: string('markerError')
      });
    })()
    """
}
