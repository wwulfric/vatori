import AppKit
import Darwin
import Foundation

private var recoveryTerminationAcceptance: FlowIpcRecoveryTerminationAcceptance?

extension FlowIpcAcceptanceRuntimeObserver {
    func scheduleRecoveryTerminationAcceptance(
        host: WebViewHost, context: FlowIpcWindowContext, timeoutSeconds: Double
    ) {
        guard context.scope == .main, context.recoveryGeneration == 0,
              recoveryTerminationAcceptance == nil else { return }
        let driver = FlowIpcRecoveryTerminationAcceptance(
            host: host, context: context, timeoutSeconds: timeoutSeconds)
        recoveryTerminationAcceptance = driver
        driver.start()
    }
}

/// Test-only failure injection and real WKWebView observation. Only the held,
/// ready Gen1 process receives a signal from this driver. Gen2 timeout, TERM,
/// escalation, exit acknowledgement and subsequent restart belong to production.
private final class FlowIpcRecoveryTerminationAcceptance {
    private enum Phase: String { case ready, clicking, result, recovering, drain }
    private let host: WebViewHost
    private let context: FlowIpcWindowContext
    private let windowIdentity: ObjectIdentifier
    private let webViewIdentity: ObjectIdentifier
    private let windowNumber: Int
    private let timeout: Double
    private let deadline: Date
    private let runDirectory: String
    private let initialPID: Int32
    private var currentPID: Int32
    private var blockedPID: Int32 = 0
    private var recoveredPID: Int32 = 0
    private var runId = ""
    private var recoveryGeneration = 0
    private var fixtureGeneration = 1
    private var phase = Phase.ready
    private var terminal = false
    private var pollScheduled = false
    private var injected = false
    private var blockedObserved = false
    private var timeoutWorkItem: DispatchWorkItem?
    private var baseline: Snapshot?
    private var initialSettled: Snapshot?
    private var currentDocumentMarker = ""
    private var currentDocumentOrigin = 0.0
    private var quietSignature = ""
    private var quietPolls = 0
    private var stages: [String] = []
    private var pointerPhase = "not-started"
    private var pointerPhaseStartedAt = 0.0
    private var pointerGeometry: Geometry?

    init(host: WebViewHost, context: FlowIpcWindowContext, timeoutSeconds: Double) {
        self.host = host
        self.context = context
        windowIdentity = ObjectIdentifier(context.window)
        webViewIdentity = ObjectIdentifier(context.webView)
        windowNumber = context.window.windowNumber
        timeout = timeoutSeconds
        deadline = Date().addingTimeInterval(timeoutSeconds)
        runDirectory = host.config.flowIpcRecoveryTerminationDirectory
        initialPID = host.sidecarProcessIdentifier ?? 0
        currentPID = initialPID
    }

    func start() {
        let leaf = URL(fileURLWithPath: runDirectory).lastPathComponent
        guard timeout.isFinite, timeout > 0, timeout <= 600, initialPID > 0,
              runDirectory.hasPrefix("/"),
              leaf.range(of: "^vatori-recovery-termination\\.[A-Za-z0-9]{6}$",
                         options: .regularExpression) != nil else {
            fail("invalid-configuration")
            return
        }
        var directoryStat = stat()
        guard lstat(runDirectory, &directoryStat) == 0,
              directoryStat.st_mode & S_IFMT == S_IFDIR,
              directoryStat.st_mode & 0o777 == 0o700,
              directoryStat.st_uid == geteuid() else {
            fail("invalid-run-directory")
            return
        }
        do {
            guard let marker = try readMarker("generation-1.json", generation: 1),
                  marker.processIdentifier == initialPID else {
                fail("initial-process-marker")
                return
            }
            runId = marker.runId
        } catch {
            fail("initial-process-marker")
            return
        }
        let work = DispatchWorkItem { [weak self] in self?.fail("deadline") }
        timeoutWorkItem = work
        DispatchQueue.main.asyncAfter(deadline: .now() + timeout, execute: work)
        schedule()
    }

    private func retained() -> Bool {
        host.flowIpcWindowContext("main") === context && context.scope == .main
            && ObjectIdentifier(context.window) == windowIdentity
            && ObjectIdentifier(context.webView) == webViewIdentity
            && context.window.windowNumber == windowNumber
    }

    private func live(epoch: Int, process: Int32) -> Bool {
        guard !terminal, phase != .recovering, epoch == recoveryGeneration,
              process == currentPID else { return false }
        guard retained(), context.recoveryGeneration == epoch,
              host.sidecarProcessIdentifier == process else {
            fail("owner-epoch-changed")
            return false
        }
        return true
    }

    private func schedule(_ delay: Double = 0.1) {
        guard !terminal, !pollScheduled else { return }
        pollScheduled = true
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) {
            self.pollScheduled = false
            self.poll()
        }
    }

    private func poll() {
        guard !terminal else { return }
        guard Date() < deadline else { fail("deadline"); return }
        if phase == .recovering { awaitRecovery(); return }
        if phase == .clicking { return }
        let epoch = recoveryGeneration, process = currentPID
        guard live(epoch: epoch, process: process) else { return }
        context.webView.evaluateJavaScript(Self.inspection) { result, error in
            guard self.live(epoch: epoch, process: process) else { return }
            guard error == nil, let json = result as? String,
                  let bytes = json.data(using: .utf8),
                  let state = try? JSONDecoder().decode(Snapshot.self, from: bytes) else {
                self.schedule()
                return
            }
            guard state.errorFree, state.windowId == self.context.scope.windowId,
                  state.sessionId == self.context.scope.sessionId,
                  state.uiId == self.context.scope.uiId,
                  state.recoveryGeneration == epoch else {
                self.fail("browser-scope-or-error")
                return
            }
            guard state.routeReady, state.firstMeaningfulPaint, state.flowClientCount == 1 else {
                self.schedule()
                return
            }
            guard state.fixtureGeneration == self.fixtureGeneration,
                  state.processIdentifier == process,
                  UUID(uuidString: state.documentMarker) != nil,
                  state.documentTimeOriginMillis.isFinite, state.documentTimeOriginMillis > 0,
                  state.identityText == "Recovery termination: generation=\(self.fixtureGeneration) pid=\(process) document=\(state.documentMarker)" else {
                self.fail("document-identity")
                return
            }
            if self.currentDocumentMarker.isEmpty {
                if let old = self.initialSettled,
                   (old.documentMarker == state.documentMarker
                    || old.documentTimeOriginMillis == state.documentTimeOriginMillis) {
                    self.fail("document-not-replaced")
                    return
                }
                self.currentDocumentMarker = state.documentMarker
                self.currentDocumentOrigin = state.documentTimeOriginMillis
            }
            guard self.currentDocumentMarker == state.documentMarker,
                  self.currentDocumentOrigin == state.documentTimeOriginMillis else {
                self.fail("document-changed-within-epoch")
                return
            }
            self.advance(state)
        }
    }

    private func advance(_ state: Snapshot) {
        switch phase {
        case .ready:
            guard state.counterText == "Recovery counter: 0",
                  state.pushText == "Recovery push: pending",
                  state.pointerReceiptCount == 0, state.pointerTrustedCount == 0,
                  quiet(state, required: 6) else { schedule(); return }
            baseline = state
            quietSignature = ""; quietPolls = 0
            dispatchClick(state)
        case .result:
            guard let before = baseline, resultMatches(state),
                  state.requestCount > before.requestCount,
                  state.responseCount > before.responseCount,
                  state.pushMessageCount > before.pushMessageCount,
                  quiet(state, required: 6) else { schedule(); return }
            var payload = frame(state)
            payload.merge([
                "stage": stage, "index": recoveryGeneration,
                "counter": 1, "pushCount": 1, "client": true,
                "backgroundSessionAbsent": true,
                "nativePointerReceiptCount": state.pointerReceiptCount,
                "nativePointerTrustedCount": state.pointerTrustedCount,
                "requestBaseline": before.requestCount,
                "responseBaseline": before.responseCount,
                "pushBaseline": before.pushMessageCount,
                "requestCount": state.requestCount, "responseCount": state.responseCount,
                "pushMessageCount": state.pushMessageCount,
                "balancedDrainObserved": true
            ]) { _, replacement in replacement }
            trace("recv", "flowRecoveryTermination.stageObserved", payload)
            stages.append(stage)
            if recoveryGeneration == 0 { injectInitialFailure(state) }
            else { phase = .drain; quietSignature = ""; quietPolls = 0; schedule() }
        case .drain:
            guard resultMatches(state), quiet(state, required: 6) else { schedule(); return }
            finish(state)
        case .clicking, .recovering:
            break
        }
    }

    private var stage: String { recoveryGeneration == 0 ? "initial" : "recovered" }

    private func resultMatches(_ state: Snapshot) -> Bool {
        state.counterText == "Recovery counter: 1"
            && state.pushText == "Recovery push: generation=\(fixtureGeneration) pid=\(currentPID) document=\(state.documentMarker) count=1 client=true backgroundSessionAbsent=true"
            && state.pointerReceiptCount == 1 && state.pointerTrustedCount == 1
    }

    private func quiet(_ state: Snapshot, required: Int) -> Bool {
        guard state.requestCount == state.responseCount else {
            quietSignature = ""; quietPolls = 0
            return false
        }
        // Six polls leave at least five 100 ms intervals for the official
        // client's asynchronous push callback and a possible transport retry.
        // Optional ACK diagnostics reset this quiet window when they advance;
        // the trace gate, not this delay, proves a consuming acknowledgement.
        let lastAck = state.lastAckServerSyncId.map(String.init) ?? "unavailable"
        let signature = "\(phase):\(state.requestCount):\(state.responseCount):\(state.pushMessageCount):\(lastAck)"
        quietPolls = quietSignature == signature ? quietPolls + 1 : 1
        quietSignature = signature
        return quietPolls >= required
    }

    private func injectInitialFailure(_ state: Snapshot) {
        guard !injected, retained(), context.recoveryGeneration == 0,
              currentPID == initialPID, host.sidecarProcessIdentifier == initialPID,
              initialPID > 0, stages == ["initial"] else {
            fail("failure-injection-owner")
            return
        }
        initialSettled = state
        // Set the fence before any asynchronous callback from process exit.
        phase = .recovering
        guard Darwin.kill(initialPID, SIGKILL) == 0 else {
            fail("failure-injection-signal")
            return
        }
        injected = true
        var payload = frame(state)
        payload.merge([
            "signal": Int(SIGKILL), "targetProcessIdentifier": initialPID,
            "ownedProcessConfirmed": true, "initialInteractionSettled": true
        ]) { _, replacement in replacement }
        trace("send", "flowRecoveryTermination.failureInjected", payload)
        quietSignature = ""; quietPolls = 0
        schedule()
    }

    private func awaitRecovery() {
        guard retained(), context.recoveryGeneration <= 1 else {
            fail("recovery-owner-or-generation")
            return
        }
        do {
            if !blockedObserved {
                guard let process = try readMarker("generation-2.json", generation: 2),
                      let hook = try readMarker("gen2-hook.json", generation: 2) else {
                    schedule()
                    return
                }
                guard process.runId == runId, hook.runId == runId,
                      process.processIdentifier == hook.processIdentifier,
                      process.processIdentifier != initialPID,
                      host.sidecarProcessIdentifier == process.processIdentifier,
                      context.recoveryGeneration == 0 else {
                    fail("blocked-process-marker")
                    return
                }
                blockedPID = process.processIdentifier
                blockedObserved = true
                var payload = context.scope.tracePayload
                payload.merge([
                    "runId": runId, "fixtureGeneration": 2,
                    "processIdentifier": blockedPID, "initialProcessIdentifier": initialPID,
                    "recoveryGeneration": context.recoveryGeneration,
                    "hookMarkerMatched": true, "processMarkerMatched": true,
                    "mainContextRetained": retained(), "mainWindowRetained": retained(),
                    "mainWebViewRetained": retained()
                ]) { _, replacement in replacement }
                trace("recv", "flowRecoveryTermination.blockedShutdownObserved", payload)
            }
            guard let marker = try readMarker("generation-3.json", generation: 3),
                  marker.runId == runId, marker.processIdentifier != initialPID,
                  marker.processIdentifier != blockedPID,
                  host.sidecarProcessIdentifier == marker.processIdentifier,
                  context.recoveryGeneration == 1, context.navigationFinished else {
                schedule()
                return
            }
            // Signal zero is only an existence check, never fault injection.
            errno = 0
            guard Darwin.kill(blockedPID, 0) == -1, errno == ESRCH else {
                fail("blocked-process-still-present")
                return
            }
            recoveredPID = marker.processIdentifier
            currentPID = recoveredPID
            recoveryGeneration = 1
            fixtureGeneration = 3
            currentDocumentMarker = ""; currentDocumentOrigin = 0
            baseline = nil; quietSignature = ""; quietPolls = 0
            phase = .ready
            schedule()
        } catch {
            fail("recovery-process-marker")
        }
    }

    private func dispatchClick(_ state: Snapshot) {
        phase = .clicking
        setPointerPhase("geometry-pending")
        pointerGeometry = nil
        let epoch = recoveryGeneration, process = currentPID
        context.webView.evaluateJavaScript(Self.preparePointer) { result, error in
            guard self.live(epoch: epoch, process: process) else { return }
            guard error == nil, let json = result as? String, let bytes = json.data(using: .utf8),
                  let geometry = try? JSONDecoder().decode(Geometry.self, from: bytes), geometry.ready else {
                self.setPointerPhase("geometry-not-ready")
                self.phase = .ready; self.schedule(); return
            }
            self.pointerGeometry = geometry
            self.setPointerPhase("activation-pending")
            FlowIpcNativeInputActivation.prepare(
                context: self.context, deadline: self.deadline,
                onFailure: { _ in
                    self.setPointerPhase("activation-failed")
                    self.fail("native-activation")
                }
            ) {
                guard self.live(epoch: epoch, process: process) else { return }
                self.setPointerPhase("activation-ready")
                let webView = self.context.webView
                let point = webView.convert(NSPoint(x: geometry.x,
                    y: webView.isFlipped ? geometry.y : webView.bounds.height - geometry.y), to: nil)
                guard self.context.window.contentLayoutRect.contains(point) else {
                    self.fail("pointer-outside-window"); return
                }
                let number = Int(Date().timeIntervalSince1970 * 1000) & 0x7fff
                guard let down = NSEvent.mouseEvent(with: .leftMouseDown, location: point,
                    modifierFlags: [], timestamp: ProcessInfo.processInfo.systemUptime,
                    windowNumber: self.windowNumber, context: nil, eventNumber: number,
                    clickCount: 1, pressure: 1),
                      let up = NSEvent.mouseEvent(with: .leftMouseUp, location: point,
                    modifierFlags: [], timestamp: ProcessInfo.processInfo.systemUptime + 0.05,
                    windowNumber: self.windowNumber, context: nil, eventNumber: number + 1,
                    clickCount: 1, pressure: 0) else {
                    self.fail("pointer-creation"); return
                }
                var payload = self.frame(state)
                payload.merge([
                    "stage": self.stage, "index": epoch,
                    "inputMode": "appkit-window-pointer-event", "button": "primary",
                    "activeApplication": NSApplication.shared.isActive,
                    "keyWindow": self.context.window.isKeyWindow,
                    "webViewX": geometry.x, "webViewY": geometry.y,
                    "windowX": point.x, "windowY": point.y,
                    "dispatchedAt": Date().timeIntervalSince1970 * 1000,
                    "requestBaseline": state.requestCount,
                    "responseBaseline": state.responseCount,
                    "pushBaseline": state.pushMessageCount,
                    "counterBefore": 0, "pushPending": true
                ]) { _, replacement in replacement }
                trace("recv", "flowRecoveryTermination.nativePointerDispatch", payload)
                self.setPointerPhase("mouse-down-dispatched")
                self.context.window.sendEvent(down)
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
                    guard self.live(epoch: epoch, process: process) else { return }
                    self.setPointerPhase("mouse-up-dispatched")
                    self.context.window.sendEvent(up)
                    self.phase = .result
                    self.schedule()
                }
            }
        }
    }

    private func setPointerPhase(_ value: String) {
        pointerPhase = value
        pointerPhaseStartedAt = Date().timeIntervalSince1970 * 1000
    }

    private func frame(_ state: Snapshot) -> [String: Any] {
        var payload = context.scope.tracePayload
        payload.merge([
            "runId": runId, "fixtureGeneration": fixtureGeneration,
            "processIdentifier": currentPID, "recoveryGeneration": recoveryGeneration,
            "documentMarker": state.documentMarker,
            "documentTimeOriginMillis": state.documentTimeOriginMillis,
            "firstMeaningfulPaint": state.firstMeaningfulPaint,
            "flowClientCount": state.flowClientCount,
            "mainContextRetained": retained(), "mainWindowRetained": retained(),
            "mainWebViewRetained": retained(), "mainWindowNumber": windowNumber
        ]) { _, replacement in replacement }
        return payload
    }

    private func finish(_ state: Snapshot) {
        guard let old = initialSettled, stages == ["initial", "recovered"], injected,
              blockedObserved, retained(), recoveryGeneration == 1,
              host.sidecarProcessIdentifier == recoveredPID,
              old.documentMarker != state.documentMarker,
              old.documentTimeOriginMillis != state.documentTimeOriginMillis else {
            fail("final-invariants"); return
        }
        terminal = true
        timeoutWorkItem?.cancel()
        var payload = frame(state)
        payload.merge([
            "complete": true,
            "initialProcessIdentifier": initialPID, "blockedProcessIdentifier": blockedPID,
            "recoveredProcessIdentifier": recoveredPID,
            "initialDocumentMarker": old.documentMarker,
            "initialDocumentTimeOriginMillis": old.documentTimeOriginMillis,
            "initialRecoveryGeneration": 0, "finalRecoveryGeneration": 1,
            "initialRequestCount": old.requestCount, "initialResponseCount": old.responseCount,
            "initialPushMessageCount": old.pushMessageCount,
            "recoveredRequestCount": state.requestCount,
            "recoveredResponseCount": state.responseCount,
            "recoveredPushMessageCount": state.pushMessageCount,
            "nativePointerEventCount": 2, "nativePointerTrustedEventCount": 2,
            "nativePointerStages": stages.joined(separator: ","),
            "initialJavaListenerObserved": true, "recoveredJavaListenerObserved": true,
            "initialUidlObserved": true, "recoveredUidlObserved": true,
            "initialPushObserved": true, "recoveredPushObserved": true,
            "blockedShutdownHookObserved": true, "blockedProcessAbsent": true,
            "newDocumentObserved": true, "balancedDrainObserved": true,
            "resultStableAfterDrain": true, "errorFree": state.errorFree
        ]) { _, replacement in replacement }
        trace("recv", "flowRecoveryTermination.frameworkAcceptance", payload)
        context.window.close()
    }

    private func fail(_ code: String) {
        guard !terminal else { return }
        terminal = true
        timeoutWorkItem?.cancel()
        let responder = context.window.firstResponder
        let responderView = responder as? NSView
        var payload: [String: Any] = [
            "code": "recoveryTermination-" + code, "phase": phase.rawValue,
            "initialProcessIdentifier": initialPID, "blockedProcessIdentifier": blockedPID,
            "recoveredProcessIdentifier": recoveredPID,
            "recoveryGeneration": context.recoveryGeneration,
            "quietPolls": quietPolls, "pollScheduled": pollScheduled,
            "pointerPhase": pointerPhase, "pointerPhaseStartedAt": pointerPhaseStartedAt,
            "pointerGeometryReady": pointerGeometry?.ready == true,
            "activeApplication": NSApp.isActive, "keyWindow": context.window.isKeyWindow,
            "applicationKeyWindowMatches": NSApp.keyWindow === context.window,
            "windowVisible": context.window.isVisible, "windowMiniaturized": context.window.isMiniaturized,
            "firstResponderPresent": responder != nil,
            "firstResponderIsView": responderView != nil,
            "webViewOwnsFirstResponder": responderView === context.webView
                || responderView?.isDescendant(of: context.webView) == true
        ]
        // Class identity is diagnostic only; never stringify a responder object,
        // which can expose arbitrary UI text or an address.
        payload["firstResponderClass"] = responder.map { String(describing: type(of: $0)) } ?? "none"
        if let geometry = pointerGeometry, geometry.x.isFinite, geometry.y.isFinite {
            payload["pointerGeometryX"] = geometry.x
            payload["pointerGeometryY"] = geometry.y
        }
        trace("send", "error", payload)
        context.window.close()
    }

    private struct Marker: Decodable {
        let schemaVersion: Int
        let runId: String
        let generation: Int
        let processIdentifier: Int32
    }
    private enum MarkerError: Error { case invalid }

    private func readMarker(_ name: String, generation: Int) throws -> Marker? {
        let path = URL(fileURLWithPath: runDirectory).appendingPathComponent(name).path
        let descriptor = Darwin.open(path, O_RDONLY | O_NOFOLLOW)
        if descriptor < 0 {
            if errno == ENOENT { return nil }
            throw MarkerError.invalid
        }
        defer { Darwin.close(descriptor) }
        var info = stat()
        guard fstat(descriptor, &info) == 0, info.st_mode & S_IFMT == S_IFREG,
              info.st_uid == geteuid(), info.st_size > 0, info.st_size <= 2048 else {
            throw MarkerError.invalid
        }
        // Java atomically publishes a complete temp file with createLink, then
        // unlinks that temp name. Retry during the short two-link interval;
        // never accept a multiply linked record as final evidence.
        if info.st_nlink == 2 { return nil }
        guard info.st_nlink == 1 else { throw MarkerError.invalid }
        let handle = FileHandle(fileDescriptor: descriptor, closeOnDealloc: false)
        guard let bytes = try handle.read(upToCount: 2049), bytes.count <= 2048,
              let json = try JSONSerialization.jsonObject(with: bytes) as? [String: Any],
              Set(json.keys) == Set(["schemaVersion", "runId", "generation", "processIdentifier"]) else {
            throw MarkerError.invalid
        }
        let marker = try JSONDecoder().decode(Marker.self, from: bytes)
        guard marker.schemaVersion == 1, marker.generation == generation,
              marker.processIdentifier > 0, UUID(uuidString: marker.runId) != nil else {
            throw MarkerError.invalid
        }
        return marker
    }

    private struct Geometry: Decodable { let ready: Bool; let x: Double; let y: Double }
    private struct Snapshot: Decodable {
        let windowId: String; let sessionId: String; let uiId: String
        let recoveryGeneration: Int; let fixtureGeneration: Int; let processIdentifier: Int32
        let documentMarker: String; let documentTimeOriginMillis: Double
        let identityText: String; let routeReady: Bool; let firstMeaningfulPaint: Bool
        let flowClientCount: Int; let counterText: String; let pushText: String
        let pointerReceiptCount: Int; let pointerTrustedCount: Int
        let requestCount: Int; let responseCount: Int; let pushMessageCount: Int
        let lastAckServerSyncId: Int?
        let errorFree: Bool
    }

    private static let preparePointer = """
    (() => {
      const button=document.getElementById('framework-flow-recovery-termination-invoke');
      if (!button) return JSON.stringify({ready:false,x:0,y:0});
      button.scrollIntoView({block:'center'});
      const r=button.getBoundingClientRect(), x=r.left+r.width/2, y=r.top+r.height/2;
      const hit=document.elementFromPoint(x,y);
      if (!(r.width>0&&r.height>0) || !(hit===button||button.contains(hit)))
        return JSON.stringify({ready:false,x,y});
      if (!window.VatoriRecoveryTerminationPointer) {
        const p=window.VatoriRecoveryTerminationPointer={receipt:0,trusted:0};
        document.addEventListener('click',event=>{
          if (event.button!==0 || !event.composedPath().includes(button)) return;
          p.receipt++; if(event.isTrusted) p.trusted++;
        },true);
      }
      return JSON.stringify({ready:true,x,y});
    })()
    """

    private static let inspection = """
    (() => {
      const c=window.VaadinDesktopHostContext||{}, d=window.VaadinDesktopIpcDiagnostics||{};
      const prefix='framework-flow-recovery-termination-';
      const identity=document.getElementById(prefix+'identity');
      const p=window.VatoriRecoveryTerminationPointer||{};
      return JSON.stringify({
        windowId:String(c.windowId||''),sessionId:String(c.sessionId||''),uiId:String(c.uiId||''),
        recoveryGeneration:Number(window.VaadinDesktopHostRecoveryGeneration||0),
        fixtureGeneration:Number(identity?.getAttribute('data-generation')||0),
        processIdentifier:Number(identity?.getAttribute('data-process-id')||0),
        documentMarker:String(identity?.getAttribute('data-document-marker')||''),
        documentTimeOriginMillis:Number(performance.timeOrigin),identityText:String(identity?.textContent||''),
        routeReady:!!document.getElementById(prefix+'route')&&!!document.getElementById(prefix+'invoke')
          &&location.pathname.endsWith('/flow-recovery-termination'),
        firstMeaningfulPaint:d.standardBundleFirstMeaningfulPaintReported===true,
        flowClientCount:Object.keys(window.Vaadin?.Flow?.clients||{}).filter(k=>k!=='TypeScript').length,
        counterText:String(document.getElementById(prefix+'counter')?.textContent||''),
        pushText:String(document.getElementById(prefix+'push-status')?.textContent||''),
        pointerReceiptCount:Number(p.receipt||0),pointerTrustedCount:Number(p.trusted||0),
        requestCount:Number(d.standardBundleOpaqueRequestCount||0),
        responseCount:Number(d.standardBundleOpaqueResponseCount||0),
        pushMessageCount:Number(d.standardBundlePushMessageCount||0),
        lastAckServerSyncId:Number.isSafeInteger(d.standardBundlePushLastAckServerSyncId)
          ?d.standardBundlePushLastAckServerSyncId:null,
        errorFree:!d.standardBundleBrowserError&&!d.standardBundleOpaqueRequestError
          &&!d.standardBundleOpaqueResponseError&&!d.standardBundlePushError
      });
    })()
    """
}
