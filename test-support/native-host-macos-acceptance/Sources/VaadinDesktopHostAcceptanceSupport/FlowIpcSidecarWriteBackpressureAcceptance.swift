import AppKit
import Darwin
import Foundation

private var sidecarWriteBackpressureAcceptance: FlowIpcSidecarWriteBackpressureAcceptance?

extension FlowIpcAcceptanceRuntimeObserver {
    func scheduleSidecarWriteBackpressureAcceptance(
        host: WebViewHost, context: FlowIpcWindowContext, timeoutSeconds: Double
    ) {
        guard context.scope == .main, sidecarWriteBackpressureAcceptance == nil else { return }
        let driver = FlowIpcSidecarWriteBackpressureAcceptance(
            host: host, context: context, timeoutSeconds: timeoutSeconds)
        sidecarWriteBackpressureAcceptance = driver
        driver.start()
    }

    func observeSidecarWriteBackpressure() {
        sidecarWriteBackpressureAcceptance?.applicationWillTerminate()
    }
}

/// Test-only exact-owned SIGSTOP injection after a real Flow preparation round trip.
/// No shutdown hook is expected to run: normal AppKit Quit must retire the stopped JVM.
private final class FlowIpcSidecarWriteBackpressureAcceptance {
    private enum Phase: String { case ready, preparing, clicking, suspending, flooding, closing, failed }
    private let host: WebViewHost
    private let context: FlowIpcWindowContext
    private let windowIdentity: ObjectIdentifier
    private let webViewIdentity: ObjectIdentifier
    private let windowNumber: Int
    private let processIdentifier: Int32
    private let timeout: Double
    private let deadline: Date
    private let directory: String
    private let mode: String
    private var runId = ""
    private var phase = Phase.ready
    private var terminal = false
    private var scheduled = false
    private var prepared = false
    private var sidecarSuspended = false
    private var burstDispatched = false
    private var heartbeatObserved = false
    private var quitDispatched = false
    private var baseline: Snapshot?
    private var documentOrigin = 0.0
    private var quietSignature = ""
    private var quietPolls = 0
    private var pollCount = 0
    private var inspectionStartedCount = 0
    private var inspectionCompletedCount = 0
    private var inspectionDecodedCount = 0
    private var lastInspectionStartedAt = 0.0
    private var lastInspectionCompletedAt = 0.0
    private var lastInspectionOutcome = "not-started"
    private var lastSnapshot: Snapshot?
    private var pointerPhase = "not-started"
    private var pointerPhaseStartedAt = 0.0
    private var pointerGeometry: Geometry?

    init(host: WebViewHost, context: FlowIpcWindowContext, timeoutSeconds: Double) {
        self.host = host
        self.context = context
        windowIdentity = ObjectIdentifier(context.window)
        webViewIdentity = ObjectIdentifier(context.webView)
        windowNumber = context.window.windowNumber
        processIdentifier = host.sidecarProcessIdentifier ?? 0
        timeout = timeoutSeconds
        deadline = Date().addingTimeInterval(timeoutSeconds)
        directory = host.config.flowIpcHostShutdownTerminationDirectory
        mode = host.config.flowIpcHostShutdownTerminationMode
    }

    func start() {
        guard timeout.isFinite, timeout > 0, timeout <= 600, processIdentifier > 0,
              mode == "cooperative", validDirectory() else {
            fail("configuration"); return
        }
        do {
            let marker = try readMarker("startup.json")
            guard marker.processIdentifier == processIdentifier, marker.mode == mode else {
                fail("startup-marker"); return
            }
            runId = marker.runId
        } catch { fail("startup-marker"); return }
        // Remains live in AppKit's terminate-later nested loop. The independent
        // runner deadline is the final safeguard; this driver never kills a JVM.
        NativeMainRunLoop.perform(after: timeout) { [weak self] in
            guard let self, !self.terminal else { return }
            self.fail("deadline")
        }
        schedule()
    }

    private var identity: [String: Any] {
        var value = context.scope.tracePayload
        value.merge([
            "runId": runId, "processIdentifier": processIdentifier,
            "hostProcessIdentifier": ProcessInfo.processInfo.processIdentifier,
            "recoveryGeneration": 0
        ]) { _, new in new }
        return value
    }

    private func retained() -> Bool {
        host.flowIpcWindowContext("main") === context && context.scope == .main
            && ObjectIdentifier(context.window) == windowIdentity
            && ObjectIdentifier(context.webView) == webViewIdentity
            && context.window.windowNumber == windowNumber && context.recoveryGeneration == 0
    }

    private func live() -> Bool {
        guard !terminal, phase != .closing, phase != .suspending, phase != .flooding else { return false }
        guard retained(), host.sidecarProcessIdentifier == processIdentifier else {
            fail("owner-changed"); return false
        }
        return true
    }

    private func schedule() {
        guard !terminal, !scheduled, phase != .closing else { return }
        scheduled = true
        NativeMainRunLoop.perform(after: 0.1) {
            self.scheduled = false
            self.poll()
        }
    }

    private func poll() {
        guard live(), phase != .clicking else { return }
        guard Date() < deadline else { fail("deadline"); return }
        pollCount += 1
        inspectionStartedCount += 1
        lastInspectionStartedAt = Date().timeIntervalSince1970 * 1000
        lastInspectionOutcome = "pending"
        context.webView.evaluateJavaScript(Self.inspection) { value, error in
            guard self.live() else { return }
            self.inspectionCompletedCount += 1
            self.lastInspectionCompletedAt = Date().timeIntervalSince1970 * 1000
            guard error == nil, let json = value as? String, let bytes = json.data(using: .utf8),
                  let state = try? JSONDecoder().decode(Snapshot.self, from: bytes) else {
                self.lastInspectionOutcome = error == nil ? "snapshot-decode-failed" : "javascript-error"
                self.schedule(); return
            }
            self.inspectionDecodedCount += 1
            self.lastInspectionOutcome = "decoded"
            self.lastSnapshot = state
            guard state.errorFree, state.windowId == "main", state.sessionId == "flow-ipc-session",
                  state.uiId == "main", state.recoveryGeneration == 0 else {
                self.fail("browser-scope-or-error"); return
            }
            guard state.routeReady, state.firstMeaningfulPaint, state.flowClientCount == 1 else {
                self.schedule(); return
            }
            guard state.runId == self.runId, state.processIdentifier == self.processIdentifier,
                  state.mode == self.mode, state.documentTimeOriginMillis.isFinite,
                  state.documentTimeOriginMillis > 0 else {
                self.fail("document-identity"); return
            }
            if self.documentOrigin == 0 { self.documentOrigin = state.documentTimeOriginMillis }
            guard self.documentOrigin == state.documentTimeOriginMillis else {
                self.fail("document-changed"); return
            }
            switch self.phase {
            case .ready:
                guard state.counter == "Host shutdown counter: 0", state.push == "Host shutdown push: pending",
                      state.finalDisabled, state.pointerReceiptCount == 0, state.pointerTrustedCount == 0,
                      self.quiet(state) else { self.schedule(); return }
                self.baseline = state
                self.quietSignature = ""; self.quietPolls = 0
                self.click(stage: "prepare", suffix: "prepare", index: 0)
            case .preparing:
                guard let before = self.baseline,
                      state.counter == "Host shutdown counter: 1",
                      state.push == "Host shutdown push: runId=\(self.runId) pid=\(self.processIdentifier) mode=\(self.mode) count=1 client=true backgroundSessionAbsent=true",
                      !state.finalDisabled, state.pointerReceiptCount == 1, state.pointerTrustedCount == 1,
                      state.requestCount > before.requestCount, state.responseCount > before.responseCount,
                      state.pushMessageCount > before.pushMessageCount,
                      self.quiet(state) else { self.schedule(); return }
                var payload = self.identity
                payload.merge([
                    "documentTimeOriginMillis": state.documentTimeOriginMillis,
                    "mainWindowNumber": self.windowNumber, "routeReady": true,
                    "firstMeaningfulPaint": true, "flowClientCount": 1,
                    "mainContextRetained": self.retained(), "mainWindowRetained": self.retained(),
                    "mainWebViewRetained": self.retained(), "counter": 1, "pushCount": 1,
                    "client": true, "trusted": true, "backgroundSessionAbsent": true,
                    "requestBaseline": before.requestCount, "responseBaseline": before.responseCount,
                    "pushBaseline": before.pushMessageCount, "requestCount": state.requestCount,
                    "responseCount": state.responseCount, "pushMessageCount": state.pushMessageCount,
                    "nativePointerReceiptCount": state.pointerReceiptCount,
                    "nativePointerTrustedCount": state.pointerTrustedCount, "balancedDrainObserved": true
                ]) { _, new in new }
                self.prepared = true
                trace("recv", "flowSidecarWriteBackpressure.prepared", payload)
                self.suspendSidecar()
            case .clicking, .suspending, .flooding, .closing, .failed: break
            }
        }
    }

    private func quiet(_ state: Snapshot) -> Bool {
        guard state.requestCount == state.responseCount else {
            quietSignature = ""; quietPolls = 0; return false
        }
        let ack = state.lastAckServerSyncId.map(String.init) ?? "unavailable"
        let signature = "\(phase):\(state.requestCount):\(state.responseCount):\(state.pushMessageCount):\(ack)"
        quietPolls = signature == quietSignature ? quietPolls + 1 : 1
        quietSignature = signature
        // Keep two full seconds quiet: the transport retries pending pushes on
        // a one-second timer, and its first ACK can precede Flow consumption.
        // Retries/ACK advances reset the interval. The independent gate still
        // requires a consuming ACK; this wait alone is never success evidence.
        return quietPolls >= 21
    }

    private func click(stage: String, suffix: String, index: Int) {
        guard index == 0, stage == "prepare", suffix == "prepare" else { fail("unexpected-pointer"); return }
        phase = .clicking
        setPointerPhase("geometry-pending")
        pointerGeometry = nil
        let script = """
        (() => {
          const b=document.getElementById('framework-flow-host-shutdown-termination-\(suffix)');
          if(!b||b.disabled) return JSON.stringify({ready:false,x:0,y:0});
          b.scrollIntoView({block:'center'});
          const r=b.getBoundingClientRect(),x=r.left+r.width/2,y=r.top+r.height/2;
          const hit=document.elementFromPoint(x,y);
          if(!(r.width>0&&r.height>0&&(hit===b||b.contains(hit)))) return JSON.stringify({ready:false,x,y});
          if('\(stage)'==='prepare'&&!window.VatoriHostShutdownPointer){
            const p=window.VatoriHostShutdownPointer={receipt:0,trusted:0};
            document.addEventListener('click',e=>{
              if(e.button!==0||!e.composedPath().includes(b)) return;
              p.receipt++;if(e.isTrusted)p.trusted++;
            },true);
          }
          return JSON.stringify({ready:true,x,y});
        })()
        """
        context.webView.evaluateJavaScript(script) { value, error in
            guard self.live() else { return }
            guard error == nil, let json = value as? String, let bytes = json.data(using: .utf8),
                  let geometry = try? JSONDecoder().decode(Geometry.self, from: bytes), geometry.ready,
                  geometry.x.isFinite, geometry.y.isFinite, geometry.x > 0, geometry.y > 0 else {
                self.setPointerPhase("geometry-not-ready")
                self.fail("pointer-geometry"); return
            }
            self.pointerGeometry = geometry
            self.setPointerPhase("activation-pending")
            FlowIpcNativeInputActivation.prepare(context: self.context, deadline: self.deadline,
                onFailure: { _ in
                    self.setPointerPhase("activation-failed")
                    self.fail("activation")
                }) {
                guard self.live() else { return }
                self.setPointerPhase("activation-ready")
                let webView = self.context.webView
                let point = webView.convert(NSPoint(x: geometry.x,
                    y: webView.isFlipped ? geometry.y : webView.bounds.height - geometry.y), to: nil)
                let eventNumber = Int(Date().timeIntervalSince1970 * 1000) & 0x7fff
                guard self.context.window.contentLayoutRect.contains(point),
                      let down = NSEvent.mouseEvent(with: .leftMouseDown, location: point,
                        modifierFlags: [], timestamp: ProcessInfo.processInfo.systemUptime,
                        windowNumber: self.windowNumber, context: nil, eventNumber: eventNumber,
                        clickCount: 1, pressure: 1),
                      let up = NSEvent.mouseEvent(with: .leftMouseUp, location: point,
                        modifierFlags: [], timestamp: ProcessInfo.processInfo.systemUptime + 0.05,
                        windowNumber: self.windowNumber, context: nil, eventNumber: eventNumber + 1,
                        clickCount: 1, pressure: 0) else { self.fail("pointer-creation"); return }
                var payload = self.identity
                payload.merge([
                    "stage": stage, "index": index, "inputMode": "appkit-window-pointer-event",
                    "button": "primary", "activeApplication": NSApp.isActive,
                    "keyWindow": self.context.window.isKeyWindow,
                    "webViewX": geometry.x, "webViewY": geometry.y,
                    "windowX": point.x, "windowY": point.y,
                    "dispatchedAt": Date().timeIntervalSince1970 * 1000
                ]) { _, new in new }
                trace("recv", "flowSidecarWriteBackpressure.nativePointerDispatch", payload)
                self.setPointerPhase("mouse-down-dispatched")
                self.context.window.sendEvent(down)
                NativeMainRunLoop.perform(after: 0.05) {
                    guard self.live() else { return }
                    self.setPointerPhase("mouse-up-dispatched")
                    self.context.window.sendEvent(up)
                    if index == 0 { self.phase = .preparing; self.schedule() }
                }
            }
        }
    }

    private func setPointerPhase(_ value: String) {
        pointerPhase = value
        pointerPhaseStartedAt = Date().timeIntervalSince1970 * 1000
    }

    private func stoppedProcessState() -> Bool {
        var process = kinfo_proc()
        var bytes = MemoryLayout<kinfo_proc>.stride
        var query: [Int32] = [CTL_KERN, KERN_PROC, KERN_PROC_PID, processIdentifier]
        return sysctl(&query, u_int(query.count), &process, &bytes, nil, 0) == 0
            && bytes == MemoryLayout<kinfo_proc>.stride
            && process.kp_proc.p_pid == processIdentifier && Int32(process.kp_proc.p_stat) == SSTOP
    }

    private func suspendSidecar() {
        guard prepared, retained(), host.sidecarProcessIdentifier == processIdentifier,
              context.recoveryGeneration == 0, Darwin.kill(processIdentifier, SIGSTOP) == 0 else {
            fail("suspend"); return
        }
        phase = .suspending
        waitForStopped(until: Date().addingTimeInterval(2))
    }

    private func waitForStopped(until limit: Date) {
        guard !terminal, phase == .suspending else { return }
        guard retained(), host.sidecarProcessIdentifier == processIdentifier else { fail("suspend-owner"); return }
        if !stoppedProcessState() {
            guard Date() < limit else { fail("suspend-not-observed"); return }
            NativeMainRunLoop.perform(after: 0.02) { self.waitForStopped(until: limit) }
            return
        }
        sidecarSuspended = true
        trace("recv", "flowSidecarWriteBackpressure.sidecarSuspended",
            identity.merging(["signal": "SIGSTOP", "processState": 4, "stopped": true]) { _, new in new })
        phase = .flooding
        trace("recv", "flowSidecarWriteBackpressure.burstRequested",
            identity.merging(["requestCount": 16, "headerBytes": 16_384, "browserProgrammatic": true]) { _, new in new })
        // Ordinary fetch traverses the production opaque bridge. Headers are
        // deliberately finite; body bytes would require credits from the stopped JVM.
        context.webView.evaluateJavaScript("""
        (() => {
          const padding='w'.repeat(16384);
          for(let i=0;i<16;i++) {
            fetch('/__vatori_acceptance/write-backpressure?probe='+i,
              {method:'GET',headers:{'X-Vatori-Backpressure':padding},cache:'no-store'})
              .catch(()=>{});
          }
          return 16;
        })()
        """) { value, error in
            guard !self.terminal, self.phase == .flooding else { return }
            guard error == nil, let count = value as? NSNumber, count.intValue == 16,
                  self.retained(), self.host.sidecarProcessIdentifier == self.processIdentifier else {
                self.fail("browser-dispatch"); return
            }
            self.burstDispatched = true
            trace("recv", "flowSidecarWriteBackpressure.browserDispatchCompleted",
                self.identity.merging(["requestCount": 16]) { _, new in new })
            NativeMainRunLoop.perform(after: 1) { self.heartbeatAndQuit() }
        }
    }

    private func heartbeatAndQuit() {
        guard !terminal, phase == .flooding, burstDispatched, Thread.isMainThread,
              retained(), host.sidecarProcessIdentifier == processIdentifier,
              stoppedProcessState() else { fail("heartbeat-owner"); return }
        heartbeatObserved = true
        trace("recv", "flowSidecarWriteBackpressure.mainLoopHeartbeat",
            identity.merging(["mainThread": true, "sidecarStopped": true,
                "windowRetained": true, "webViewRetained": true,
                "browserDispatchCompleted": true]) { _, new in new })
        guard let menu = NSApp.mainMenu, let applicationMenu = menu.items.first?.submenu,
              applicationMenu.items.contains(where: {
                $0.action == #selector(NSApplication.terminate(_:)) && $0.target == nil
                    && $0.keyEquivalent == "q" && $0.keyEquivalentModifierMask == .command
              }), NSApp.isActive, context.window.isKeyWindow,
              let event = NSEvent.keyEvent(with: .keyDown, location: .zero, modifierFlags: .command,
                timestamp: ProcessInfo.processInfo.systemUptime, windowNumber: windowNumber,
                context: nil, characters: "q", charactersIgnoringModifiers: "q", isARepeat: false, keyCode: 12) else {
            fail("product-quit-menu"); return
        }
        // Record intent before dispatch: AppKit may not return from its nested
        // terminate-later loop. Only applicationWillTerminate proves completion.
        phase = .closing
        quitDispatched = true
        trace("recv", "flowSidecarWriteBackpressure.quitDispatched",
            identity.merging(["source": "product-application-menu", "action": "terminate:",
                "keyEquivalent": "q", "commandModifier": true, "menuMatched": true,
                "activeApplication": true, "keyWindow": true]) { _, new in new })
        if !menu.performKeyEquivalent(with: event) { fail("quit-not-handled") }
    }

    func applicationWillTerminate() {
        guard !terminal else { return }
        let expected = prepared && sidecarSuspended && burstDispatched && heartbeatObserved
            && quitDispatched && phase == .closing
        let marker = try? readMarker("startup.json")
        let matched = marker == Marker(schemaVersion: 1, runId: runId,
            processIdentifier: processIdentifier, mode: "cooperative")
        errno = 0
        let absent = host.sidecarProcessIdentifier == nil
            && Darwin.kill(processIdentifier, 0) == -1 && errno == ESRCH
        let ownerRetained = retained()
        terminal = true
        trace("recv", "flowSidecarWriteBackpressure.applicationWillTerminate",
            identity.merging(["complete": expected && absent && ownerRetained && matched,
                "preparedObserved": prepared, "sidecarSuspendedObserved": sidecarSuspended,
                "burstDispatched": burstDispatched, "heartbeatObserved": heartbeatObserved,
                "quitDispatched": quitDispatched, "terminationSource": "applicationWillTerminate",
                "currentProcessIdentifier": host.sidecarProcessIdentifier ?? 0,
                "originalSidecarAbsent": absent, "originalWindowRetained": ownerRetained,
                "originalWebViewRetained": ownerRetained, "startupMarkerMatched": matched]) { _, new in new })
    }

    private func fail(_ code: String) {
        guard !terminal else { return }
        var payload = identity
        payload["code"] = "sidecarWriteBackpressure-" + code
        let responder = context.window.firstResponder
        let responderView = responder as? NSView
        payload.merge([
            "phase": phase.rawValue, "quietPolls": quietPolls,
            "pollCount": pollCount, "inspectionStartedCount": inspectionStartedCount,
            "inspectionCompletedCount": inspectionCompletedCount, "inspectionDecodedCount": inspectionDecodedCount,
            "lastInspectionStartedAt": lastInspectionStartedAt,
            "lastInspectionCompletedAt": lastInspectionCompletedAt,
            "lastInspectionOutcome": lastInspectionOutcome, "scheduled": scheduled,
            "pointerPhase": pointerPhase, "pointerPhaseStartedAt": pointerPhaseStartedAt,
            "pointerGeometryReady": pointerGeometry?.ready == true,
            "activeApplication": NSApp.isActive, "keyWindow": context.window.isKeyWindow,
            "applicationKeyWindowMatches": NSApp.keyWindow === context.window,
            "windowVisible": context.window.isVisible, "windowMiniaturized": context.window.isMiniaturized,
            "firstResponderPresent": responder != nil,
            "firstResponderIsView": responderView != nil,
            "webViewOwnsFirstResponder": responderView === context.webView
                || responderView?.isDescendant(of: context.webView) == true
        ]) { _, new in new }
        payload["firstResponderClass"] = responder.map { String(describing: type(of: $0)) } ?? "none"
        if let geometry = pointerGeometry, geometry.x.isFinite, geometry.y.isFinite {
            payload["pointerGeometryX"] = geometry.x
            payload["pointerGeometryY"] = geometry.y
        }
        if let state = lastSnapshot {
            // Report predicates and counters only, never arbitrary DOM text.
            payload.merge([
                "snapshotRouteReady": state.routeReady,
                "snapshotFirstMeaningfulPaint": state.firstMeaningfulPaint,
                "snapshotFlowClientCount": state.flowClientCount,
                "snapshotCounterInitial": state.counter == "Host shutdown counter: 0",
                "snapshotCounterPrepared": state.counter == "Host shutdown counter: 1",
                "snapshotPushPending": state.push == "Host shutdown push: pending",
                "snapshotPushPrepared": state.push == "Host shutdown push: runId=\(runId) pid=\(processIdentifier) mode=\(mode) count=1 client=true backgroundSessionAbsent=true",
                "snapshotFinalDisabled": state.finalDisabled,
                "snapshotPointerReceiptCount": state.pointerReceiptCount,
                "snapshotPointerTrustedCount": state.pointerTrustedCount,
                "snapshotRequestCount": state.requestCount, "snapshotResponseCount": state.responseCount,
                "snapshotPushMessageCount": state.pushMessageCount,
                "snapshotLastAckAvailable": state.lastAckServerSyncId != nil,
                "snapshotLastAckServerSyncId": state.lastAckServerSyncId ?? -1,
                "snapshotErrorFree": state.errorFree
            ]) { _, new in new }
        }
        terminal = true; phase = .failed
        trace("send", "error", payload)
        NSApp.perform(#selector(NSApplication.terminate(_:)), with: nil, afterDelay: 0)
    }

    private struct Marker: Decodable, Equatable {
        let schemaVersion: Int; let runId: String; let processIdentifier: Int32; let mode: String
    }
    private enum MarkerError: Error { case invalid }

    private func validDirectory() -> Bool {
        let leaf = URL(fileURLWithPath: directory).lastPathComponent
        var info = stat()
        return directory.hasPrefix("/")
            && leaf.range(of: "^vatori-host-shutdown\\.[A-Za-z0-9]{6}$", options: .regularExpression) != nil
            && lstat(directory, &info) == 0 && info.st_mode & S_IFMT == S_IFDIR
            && info.st_mode & 0o777 == 0o700 && info.st_uid == geteuid()
    }

    private func readMarker(_ name: String) throws -> Marker {
        guard validDirectory() else { throw MarkerError.invalid }
        let path = URL(fileURLWithPath: directory).appendingPathComponent(name).path
        let descriptor = Darwin.open(path, O_RDONLY | O_NOFOLLOW)
        guard descriptor >= 0 else { throw MarkerError.invalid }
        defer { Darwin.close(descriptor) }
        var info = stat()
        guard fstat(descriptor, &info) == 0, info.st_mode & S_IFMT == S_IFREG,
              info.st_uid == geteuid(), info.st_nlink == 1, info.st_mode & 0o777 == 0o600,
              info.st_size > 0, info.st_size <= 2048 else { throw MarkerError.invalid }
        let handle = FileHandle(fileDescriptor: descriptor, closeOnDealloc: false)
        guard let bytes = try handle.read(upToCount: 2049), bytes.count <= 2048,
              let json = try JSONSerialization.jsonObject(with: bytes) as? [String: Any],
              Set(json.keys) == Set(["schemaVersion", "runId", "processIdentifier", "mode"]) else {
            throw MarkerError.invalid
        }
        let value = try JSONDecoder().decode(Marker.self, from: bytes)
        guard value.schemaVersion == 1, value.processIdentifier > 0,
              UUID(uuidString: value.runId)?.uuidString.lowercased() == value.runId,
              value.mode == "cooperative" else { throw MarkerError.invalid }
        return value
    }

    private struct Geometry: Decodable { let ready: Bool; let x: Double; let y: Double }
    private struct Snapshot: Decodable {
        let windowId: String; let sessionId: String; let uiId: String; let recoveryGeneration: Int
        let runId: String; let processIdentifier: Int32; let mode: String
        let documentTimeOriginMillis: Double; let routeReady: Bool; let firstMeaningfulPaint: Bool
        let flowClientCount: Int; let counter: String; let push: String; let finalDisabled: Bool
        let pointerReceiptCount: Int; let pointerTrustedCount: Int
        let requestCount: Int; let responseCount: Int; let pushMessageCount: Int; let lastAckServerSyncId: Int?
        let errorFree: Bool
    }
    private static let inspection = """
    (()=>{
      const c=window.VaadinDesktopHostContext||{},d=window.VaadinDesktopIpcDiagnostics||{};
      const prefix='framework-flow-host-shutdown-termination-',id=document.getElementById(prefix+'identity');
      const p=window.VatoriHostShutdownPointer||{},final=document.getElementById(prefix+'final');
      return JSON.stringify({
        windowId:String(c.windowId||''),sessionId:String(c.sessionId||''),uiId:String(c.uiId||''),
        recoveryGeneration:Number(window.VaadinDesktopHostRecoveryGeneration||0),
        runId:String(id?.getAttribute('data-run-id')||''),processIdentifier:Number(id?.getAttribute('data-process-id')||0),
        mode:String(id?.getAttribute('data-mode')||''),documentTimeOriginMillis:Number(performance.timeOrigin),
        routeReady:!!document.getElementById(prefix+'route')&&!!document.getElementById(prefix+'prepare')&&!!final
          &&location.pathname.endsWith('/flow-host-shutdown-termination'),
        firstMeaningfulPaint:d.standardBundleFirstMeaningfulPaintReported===true,
        flowClientCount:Object.keys(window.Vaadin?.Flow?.clients||{}).filter(k=>k!=='TypeScript').length,
        counter:String(document.getElementById(prefix+'counter')?.textContent||''),
        push:String(document.getElementById(prefix+'push-status')?.textContent||''),finalDisabled:final?.disabled===true,
        pointerReceiptCount:Number(p.receipt||0),pointerTrustedCount:Number(p.trusted||0),
        requestCount:Number(d.standardBundleOpaqueRequestCount||0),responseCount:Number(d.standardBundleOpaqueResponseCount||0),
        pushMessageCount:Number(d.standardBundlePushMessageCount||0),
        lastAckServerSyncId:Number.isSafeInteger(d.standardBundlePushLastAckServerSyncId)?d.standardBundlePushLastAckServerSyncId:null,
        errorFree:!d.standardBundleBrowserError&&!d.standardBundleOpaqueRequestError
          &&!d.standardBundleOpaqueResponseError&&!d.standardBundlePushError
      });
    })()
    """
}
