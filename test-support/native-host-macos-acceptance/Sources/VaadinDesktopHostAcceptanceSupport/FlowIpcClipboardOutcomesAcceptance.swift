import Cocoa
import Foundation

private var flowIpcClipboardOutcomesAcceptance:
    FlowIpcClipboardOutcomesAcceptance?

func restoreClipboardOutcomesPasteboardOnTermination() {
    flowIpcClipboardOutcomesAcceptance?
        .restorePasteboardOnApplicationTermination()
}

extension WebViewHost {
    func scheduleClipboardOutcomesAcceptanceIfReady() {
        guard config.flowIpcAutoClipboardOutcomes,
              config.transport == .flowIpc else {
            return
        }
        if flowIpcClipboardOutcomesAcceptance == nil {
            let timeout = config.flowIpcAutoShutdownTimeoutSeconds > 0
                ? config.flowIpcAutoShutdownTimeoutSeconds
                : 60
            flowIpcClipboardOutcomesAcceptance =
                FlowIpcClipboardOutcomesAcceptance(
                    host: self,
                    deadline: Date().addingTimeInterval(timeout))
        }
        flowIpcClipboardOutcomesAcceptance?.resume()
    }
}

private final class FlowIpcClipboardOutcomesAcceptance {
    private enum Phase: String {
        case root
        case mainReady
        case noTextInvoked
        case emptyInvoked
        case secondaryOpening
        case deniedInvoked
        case finishDrain

        var needsSecondary: Bool {
            switch self {
            case .secondaryOpening, .deniedInvoked, .finishDrain:
                true
            case .root, .mainReady, .noTextInvoked, .emptyInvoked:
                false
            }
        }
    }

    private static let secondaryWindowId =
        "clipboard-outcomes-secondary"
    private static let initialNoTextStatus =
        "Clipboard no text: pending"
    private static let initialEmptyStatus =
        "Clipboard empty: pending"
    private static let initialDeniedStatus =
        "Clipboard denied: pending"
    private static let initialWindowStatus =
        "Clipboard window: pending"
    private static let openedWindowStatus =
        "Clipboard window: opened=true client=true error=none"
    private static let noTextStatus =
        "Clipboard no text: outcome=NO_TEXT textPresent=false "
        + "length=0 client=true count=1"
    private static let emptyStatus =
        "Clipboard empty: writeOutcome=WRITTEN readOutcome=TEXT "
        + "textPresent=true length=0 client=true count=1"
    private static let deniedStatus =
        "Clipboard denied: writeOutcome=DENIED writeLength=22 "
        + "readOutcome=DENIED "
        + "textPresent=false length=0 client=true count=1"
    private static let deniedWriteProbeLength = 22
    private static let pasteboardRestoreAttemptsPerCall = 2
    private static let maximumPasteboardRestoreAttempts = 6
    private static let markerType = NSPasteboard.PasteboardType(
        "dev.vatori.acceptance.clipboard.no-text")

    private let host: WebViewHost
    private let deadline: Date
    private var phase = Phase.root
    private var pollScheduled = false
    private var terminal = false
    private var originalSnapshot: PasteboardSnapshot?
    private var nativeSnapshotCapturedObserved = false
    private var pasteboardSetupOwned = false
    private var testOwnedChangeCount = 0
    private var pasteboardRestoreAttempts = 0
    private var nativeNoTextSetupObserved = false
    private var nativeNoTextReadNoSideEffectObserved = false
    private var nativeEmptyTextObserved = false
    private var nativeDeniedNoSideEffectObserved = false
    private var nativeSnapshotRestoredObserved = false
    private var nativePasteboardContendedObserved = false
    private var emptyWriteChangeAdopted = false
    private var rootRenderedObserved = false
    private var mainNoTextRequestBaseline = 0
    private var mainNoTextResponseBaseline = 0
    private var mainEmptyRequestBaseline = 0
    private var mainEmptyResponseBaseline = 0
    private var mainPushBaseline = 0
    private var secondaryRequestBaseline = 0
    private var secondaryResponseBaseline = 0
    private var secondaryPushBaseline = 0
    private var secondarySessionId = ""
    private var mainNoTextUidlObserved = false
    private var mainEmptyUidlObserved = false
    private var secondaryDeniedUidlObserved = false
    private var mainPushObserved = false
    private var secondaryPushObserved = false
    private var scopeIsolatedObserved = false
    private var nativePointerStages: [String] = []
    private var nativePointerReceiptStages: [String] = []
    private var nativePointerTrustedStages: [String] = []
    private var browserProgrammaticStages: [String] = []
    private var semanticUidlStages: [String] = []
    private var drainKey = ""
    private var drainBalancedPolls = 0

    init(host: WebViewHost, deadline: Date) {
        self.host = host
        self.deadline = deadline
    }

    func resume() {
        schedulePoll(after: 0.1)
    }

    func restorePasteboardOnApplicationTermination() {
        _ = restorePasteboardIfOwned()
    }

    private func schedulePoll(after delay: Double) {
        guard !terminal, !pollScheduled else { return }
        pollScheduled = true
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) {
            self.pollScheduled = false
            self.poll()
        }
    }

    private func poll() {
        guard !terminal else { return }
        guard let main = host.flowIpcWindowContext("main"),
              main.navigationFinished else {
            retryOrFail(code: "clipboardOutcomesMainWindowTimeout")
            return
        }
        inspect(main) { mainSnapshot in
            guard let mainSnapshot else {
                self.retryOrFail(
                    code: "clipboardOutcomesMainInspectionFailed")
                return
            }
            guard mainSnapshot.errorFree else {
                self.fail(
                    code: "clipboardOutcomesMainRuntimeError",
                    details: mainSnapshot.tracePayload(prefix: "main"))
                return
            }
            guard self.phase.needsSecondary else {
                self.advance(
                    main: main,
                    mainSnapshot: mainSnapshot,
                    secondary: nil,
                    secondarySnapshot: nil)
                return
            }
            guard let secondary = self.host.flowIpcWindowContext(
                Self.secondaryWindowId),
                  secondary.navigationFinished else {
                self.retryOrFail(
                    code: "clipboardOutcomesSecondaryWindowTimeout")
                return
            }
            self.inspect(secondary) { secondarySnapshot in
                guard let secondarySnapshot else {
                    self.retryOrFail(
                        code: "clipboardOutcomesSecondaryInspectionFailed")
                    return
                }
                guard secondarySnapshot.errorFree else {
                    self.fail(
                        code: "clipboardOutcomesSecondaryRuntimeError",
                        details: secondarySnapshot.tracePayload(
                            prefix: "secondary"))
                    return
                }
                self.advance(
                    main: main,
                    mainSnapshot: mainSnapshot,
                    secondary: secondary,
                    secondarySnapshot: secondarySnapshot)
            }
        }
    }

    private func advance(
        main: FlowIpcWindowContext,
        mainSnapshot: Snapshot,
        secondary: FlowIpcWindowContext?,
        secondarySnapshot: Snapshot?
    ) {
        guard maintainPasteboardOwnershipForCurrentPhase() else {
            fail(
                code: "clipboardOutcomesPasteboardContended",
                details: ["ownershipLost": true])
            return
        }
        switch phase {
        case .root:
            guard mainSnapshot.rootRendered,
                  mainSnapshot.firstMeaningfulPaint,
                  mainSnapshot.flowClientCount > 0 else {
                retryOrFail(
                    code: "clipboardOutcomesRootTimeout",
                    details: mainSnapshot.tracePayload(prefix: "main"))
                return
            }
            rootRenderedObserved = true
            guard prepareNoTextPasteboard() else {
                fail(code: "clipboardOutcomesPasteboardSetupFailed")
                return
            }
            dispatchBrowserClick(
                in: main,
                buttonId:
                    "framework-flow-clipboard-outcomes-open-route",
                stage: "route-navigate",
                next: .mainReady,
                failureCode: "clipboardOutcomesNavigationFailed")
        case .mainReady:
            guard mainSnapshot.routeReady,
                  exactMainScope(mainSnapshot),
                  mainSnapshot.noTextStatus == Self.initialNoTextStatus,
                  mainSnapshot.emptyStatus == Self.initialEmptyStatus,
                  mainSnapshot.deniedStatus == Self.initialDeniedStatus,
                  mainSnapshot.windowStatus == Self.initialWindowStatus,
                  nativeNoTextSetupObserved,
                  NSPasteboard.general.changeCount
                    == testOwnedChangeCount,
                  NSPasteboard.general.string(forType: .string) == nil else {
                retryOrFail(
                    code: "clipboardOutcomesMainRouteTimeout",
                    details: mainSnapshot.tracePayload(prefix: "main"))
                return
            }
            mainNoTextRequestBaseline = mainSnapshot.requestCount
            mainNoTextResponseBaseline = mainSnapshot.responseCount
            mainPushBaseline = mainSnapshot.pushMessageCount
            performNativeClick(
                in: main,
                buttonId:
                    "framework-flow-clipboard-outcomes-read-no-text",
                stage: "main-no-text",
                next: .noTextInvoked,
                failureCode: "clipboardOutcomesNoTextInvokeFailed")
        case .noTextInvoked:
            recordPointer(stage: "main-no-text", snapshot: mainSnapshot)
            guard pointerObserved(
                    stage: "main-no-text",
                    snapshot: mainSnapshot),
                  mainSnapshot.noTextStatus == Self.noTextStatus,
                  mainSnapshot.emptyStatus == Self.initialEmptyStatus,
                  mainSnapshot.deniedStatus == Self.initialDeniedStatus,
                  NSPasteboard.general.changeCount
                    == testOwnedChangeCount,
                  NSPasteboard.general.string(forType: .string) == nil else {
                retryOrFail(
                    code: "clipboardOutcomesNoTextResultTimeout",
                    details: mainSnapshot.tracePayload(prefix: "main"))
                return
            }
            mainNoTextUidlObserved =
                mainSnapshot.requestCount > mainNoTextRequestBaseline
                && mainSnapshot.responseCount > mainNoTextResponseBaseline
            guard mainNoTextUidlObserved else {
                retryOrFail(
                    code: "clipboardOutcomesNoTextUidlTimeout",
                    details: mainSnapshot.tracePayload(prefix: "main"))
                return
            }
            nativeNoTextReadNoSideEffectObserved = true
            recordSemanticUidl("main-no-text")
            mainEmptyRequestBaseline = mainSnapshot.requestCount
            mainEmptyResponseBaseline = mainSnapshot.responseCount
            performNativeClick(
                in: main,
                buttonId:
                    "framework-flow-clipboard-outcomes-write-read-empty",
                stage: "main-empty",
                next: .emptyInvoked,
                failureCode: "clipboardOutcomesEmptyInvokeFailed")
        case .emptyInvoked:
            recordPointer(stage: "main-empty", snapshot: mainSnapshot)
            guard pointerObserved(
                    stage: "main-empty",
                    snapshot: mainSnapshot),
                  mainSnapshot.noTextStatus == Self.noTextStatus,
                  mainSnapshot.emptyStatus == Self.emptyStatus,
                  mainSnapshot.deniedStatus == Self.initialDeniedStatus,
                  emptyWriteChangeAdopted,
                  NSPasteboard.general.changeCount
                    == testOwnedChangeCount,
                  NSPasteboard.general.string(forType: .string) == "" else {
                retryOrFail(
                    code: "clipboardOutcomesEmptyResultTimeout",
                    details: mainSnapshot.tracePayload(prefix: "main"))
                return
            }
            mainEmptyUidlObserved =
                mainSnapshot.requestCount > mainEmptyRequestBaseline
                && mainSnapshot.responseCount > mainEmptyResponseBaseline
            guard mainEmptyUidlObserved else {
                retryOrFail(
                    code: "clipboardOutcomesEmptyUidlTimeout",
                    details: mainSnapshot.tracePayload(prefix: "main"))
                return
            }
            nativeEmptyTextObserved = true
            recordSemanticUidl("main-empty")
            dispatchBrowserClick(
                in: main,
                buttonId:
                    "framework-flow-clipboard-outcomes-open-secondary",
                stage: "secondary-open",
                next: .secondaryOpening,
                failureCode: "clipboardOutcomesSecondaryOpenFailed")
        case .secondaryOpening:
            guard let secondary,
                  let secondarySnapshot,
                  mainSnapshot.routeReady,
                  mainSnapshot.noTextStatus == Self.noTextStatus,
                  mainSnapshot.emptyStatus == Self.emptyStatus,
                  mainSnapshot.deniedStatus == Self.initialDeniedStatus,
                  mainSnapshot.windowStatus == Self.openedWindowStatus,
                  secondarySnapshot.routeReady,
                  secondarySnapshot.noTextStatus
                    == Self.initialNoTextStatus,
                  secondarySnapshot.emptyStatus
                    == Self.initialEmptyStatus,
                  secondarySnapshot.deniedStatus
                    == Self.initialDeniedStatus,
                  secondarySnapshot.windowStatus
                    == Self.initialWindowStatus else {
                retryOrFail(
                    code: "clipboardOutcomesSecondaryRouteTimeout",
                    details: mergedPayload(
                        mainSnapshot: mainSnapshot,
                        secondarySnapshot: secondarySnapshot))
                return
            }
            secondarySessionId = secondary.scope.sessionId
            scopeIsolatedObserved = exactMainScope(mainSnapshot)
                && exactSecondaryScope(secondarySnapshot)
                && mainSnapshot.sessionId
                    != secondarySnapshot.sessionId
                && validOpaqueSecondarySession(secondarySessionId)
            guard scopeIsolatedObserved else {
                fail(
                    code: "clipboardOutcomesScopeIsolationFailed",
                    details: secondarySnapshot.tracePayload(
                        prefix: "secondary"))
                return
            }
            secondaryRequestBaseline = secondarySnapshot.requestCount
            secondaryResponseBaseline = secondarySnapshot.responseCount
            secondaryPushBaseline = secondarySnapshot.pushMessageCount
            performNativeClick(
                in: secondary,
                buttonId:
                    "framework-flow-clipboard-outcomes-invoke-denied",
                stage: "secondary-denied",
                next: .deniedInvoked,
                failureCode: "clipboardOutcomesDeniedInvokeFailed")
        case .deniedInvoked:
            guard let secondarySnapshot else {
                retryOrFail(
                    code: "clipboardOutcomesSecondaryInspectionFailed")
                return
            }
            recordPointer(
                stage: "secondary-denied",
                snapshot: secondarySnapshot)
            guard pointerObserved(
                    stage: "secondary-denied",
                    snapshot: secondarySnapshot),
                  mainSnapshot.noTextStatus == Self.noTextStatus,
                  mainSnapshot.emptyStatus == Self.emptyStatus,
                  mainSnapshot.deniedStatus == Self.initialDeniedStatus,
                  mainSnapshot.windowStatus == Self.openedWindowStatus,
                  secondarySnapshot.noTextStatus
                    == Self.initialNoTextStatus,
                  secondarySnapshot.emptyStatus
                    == Self.initialEmptyStatus,
                  secondarySnapshot.deniedStatus == Self.deniedStatus,
                  secondarySnapshot.windowStatus
                    == Self.initialWindowStatus,
                  NSPasteboard.general.changeCount
                    == testOwnedChangeCount,
                  NSPasteboard.general.string(forType: .string) == "" else {
                retryOrFail(
                    code: "clipboardOutcomesDeniedResultTimeout",
                    details: mergedPayload(
                        mainSnapshot: mainSnapshot,
                        secondarySnapshot: secondarySnapshot))
                return
            }
            secondaryDeniedUidlObserved =
                secondarySnapshot.requestCount > secondaryRequestBaseline
                && secondarySnapshot.responseCount
                    > secondaryResponseBaseline
            guard secondaryDeniedUidlObserved else {
                retryOrFail(
                    code: "clipboardOutcomesDeniedUidlTimeout",
                    details: secondarySnapshot.tracePayload(
                        prefix: "secondary"))
                return
            }
            nativeDeniedNoSideEffectObserved = true
            recordSemanticUidl("secondary-denied")
            mainPushObserved =
                mainSnapshot.pushMessageCount > mainPushBaseline
            secondaryPushObserved =
                secondarySnapshot.pushMessageCount > secondaryPushBaseline
            phase = .finishDrain
            schedulePoll(after: 0.5)
        case .finishDrain:
            guard let secondary,
                  let secondarySnapshot,
                  mainSnapshot.noTextStatus == Self.noTextStatus,
                  mainSnapshot.emptyStatus == Self.emptyStatus,
                  mainSnapshot.deniedStatus == Self.initialDeniedStatus,
                  secondarySnapshot.noTextStatus
                    == Self.initialNoTextStatus,
                  secondarySnapshot.emptyStatus
                    == Self.initialEmptyStatus,
                  secondarySnapshot.deniedStatus == Self.deniedStatus,
                  mainSnapshot.pushMessageCount == mainPushBaseline,
                  secondarySnapshot.pushMessageCount
                    == secondaryPushBaseline,
                  NSPasteboard.general.changeCount
                    == testOwnedChangeCount,
                  NSPasteboard.general.string(forType: .string) == "" else {
                fail(
                    code: "clipboardOutcomesResultChangedDuringDrain",
                    details: mergedPayload(
                        mainSnapshot: mainSnapshot,
                        secondarySnapshot: secondarySnapshot))
                return
            }
            let balanced =
                mainSnapshot.requestCount == mainSnapshot.responseCount
                && secondarySnapshot.requestCount
                    == secondarySnapshot.responseCount
            let key = "\(mainSnapshot.requestCount):"
                + "\(mainSnapshot.responseCount):"
                + "\(secondarySnapshot.requestCount):"
                + "\(secondarySnapshot.responseCount)"
            if balanced {
                if drainKey == key {
                    drainBalancedPolls += 1
                } else {
                    drainKey = key
                    drainBalancedPolls = 1
                }
            } else {
                drainKey = ""
                drainBalancedPolls = 0
            }
            guard drainBalancedPolls >= 2 else {
                retryOrFail(
                    code: "clipboardOutcomesDrainTimeout",
                    details: [
                        "mainRequestCount": mainSnapshot.requestCount,
                        "mainResponseCount": mainSnapshot.responseCount,
                        "secondaryRequestCount":
                            secondarySnapshot.requestCount,
                        "secondaryResponseCount":
                            secondarySnapshot.responseCount,
                        "balancedPolls": drainBalancedPolls
                    ],
                    delay: 0.5)
                return
            }
            finish(
                main: main,
                mainSnapshot: mainSnapshot,
                secondary: secondary,
                secondarySnapshot: secondarySnapshot)
        }
    }

    private func prepareNoTextPasteboard() -> Bool {
        guard originalSnapshot == nil else {
            return nativeNoTextSetupObserved
        }
        let pasteboard = NSPasteboard.general
        let capturedChangeCount = pasteboard.changeCount
        guard let snapshot = PasteboardSnapshot.capture(from: pasteboard)
        else {
            return false
        }
        originalSnapshot = snapshot
        nativeSnapshotCapturedObserved = true

        let marker = NSPasteboardItem()
        guard marker.setData(
            Data([0x56, 0x46]),
            forType: Self.markerType) else {
            return false
        }
        guard pasteboard.changeCount == capturedChangeCount else {
            nativePasteboardContendedObserved = true
            return false
        }
        pasteboard.clearContents()
        pasteboardSetupOwned = true
        testOwnedChangeCount = pasteboard.changeCount
        guard pasteboard.writeObjects([marker]) else {
            return false
        }
        testOwnedChangeCount = pasteboard.changeCount
        nativeNoTextSetupObserved =
            pasteboard.string(forType: .string) == nil
            && pasteboard.pasteboardItems?.isEmpty == false
            && pasteboard.pasteboardItems?.allSatisfy { item in
                !item.types.contains(.string)
            } == true
        return nativeNoTextSetupObserved
    }

    private func maintainPasteboardOwnershipForCurrentPhase() -> Bool {
        guard pasteboardSetupOwned else { return true }
        let pasteboard = NSPasteboard.general
        if phase == .emptyInvoked,
           pasteboard.changeCount != testOwnedChangeCount {
            guard !emptyWriteChangeAdopted,
                  pasteboard.changeCount > testOwnedChangeCount,
                  pasteboard.string(forType: .string) == "" else {
                nativePasteboardContendedObserved = true
                return false
            }
            testOwnedChangeCount = pasteboard.changeCount
            emptyWriteChangeAdopted = true
            return true
        }
        guard pasteboard.changeCount == testOwnedChangeCount else {
            nativePasteboardContendedObserved = true
            return false
        }
        return true
    }

    private func restorePasteboardIfOwned() -> Bool {
        guard !nativeSnapshotRestoredObserved else { return true }
        guard pasteboardSetupOwned else {
            return true
        }
        guard !nativePasteboardContendedObserved else { return false }
        let pasteboard = NSPasteboard.general
        guard let originalSnapshot else {
            return false
        }

        var attemptsThisCall = 0
        while attemptsThisCall
                < Self.pasteboardRestoreAttemptsPerCall,
              pasteboardRestoreAttempts
                < Self.maximumPasteboardRestoreAttempts {
            guard pasteboard.changeCount == testOwnedChangeCount else {
                nativePasteboardContendedObserved = true
                return false
            }
            let restoration = originalSnapshot.restore(
                to: pasteboard,
                onlyIfChangeCount: testOwnedChangeCount)
            pasteboardRestoreAttempts += 1
            attemptsThisCall += 1
            if restoration.contended {
                nativePasteboardContendedObserved = true
                return false
            }
            if let ownedChangeCount = restoration.ownedChangeCount {
                testOwnedChangeCount = ownedChangeCount
            }
            if restoration.restored {
                nativeSnapshotRestoredObserved = true
                pasteboardSetupOwned = false
                return true
            }
        }
        return false
    }

    private func dispatchBrowserClick(
        in context: FlowIpcWindowContext,
        buttonId: String,
        stage: String,
        next: Phase,
        failureCode: String
    ) {
        let script = """
        (() => {
          const button = document.querySelector('#\(buttonId)');
          if (!button || typeof button.click !== 'function') return false;
          button.click();
          return true;
        })()
        """
        context.webView.evaluateJavaScript(script) { result, error in
            guard error == nil, result as? Bool == true else {
                self.retryOrFail(
                    code: failureCode,
                    details: [
                        "message": error?.localizedDescription ?? "",
                        "result": String(describing: result),
                        "stage": stage
                    ])
                return
            }
            if !self.browserProgrammaticStages.contains(stage) {
                self.browserProgrammaticStages.append(stage)
            }
            self.phase = next
            self.schedulePoll(after: 0.2)
        }
    }

    private func performNativeClick(
        in context: FlowIpcWindowContext,
        buttonId: String,
        stage: String,
        next: Phase,
        failureCode: String
    ) {
        context.webView.evaluateJavaScript(
            Self.pointerPreparationScript(
                buttonId: buttonId,
                stage: stage)) { result, error in
            guard error == nil,
                  let json = result as? String,
                  let data = json.data(using: .utf8),
                  let geometry = try? JSONDecoder().decode(
                    PointerGeometry.self,
                    from: data),
                  geometry.ready else {
                self.retryOrFail(
                    code: failureCode,
                    details: [
                        "message": error?.localizedDescription ?? "",
                        "result": String(describing: result),
                        "stage": stage
                    ])
                return
            }
            FlowIpcNativeInputActivation.prepare(
                context: context,
                deadline: self.deadline,
                onFailure: { details in
                    self.fail(code: failureCode, details: details)
                }
            ) {
                let point = context.webView.convert(
                    NSPoint(
                        x: geometry.centerX,
                        y: context.webView.isFlipped
                            ? geometry.centerY
                            : context.webView.bounds.height
                                - geometry.centerY),
                    to: nil)
                guard context.window.contentLayoutRect.contains(point) else {
                    self.retryOrFail(
                        code: failureCode,
                        details: [
                            "message": "Pointer target is outside the window",
                            "stage": stage
                        ])
                    return
                }
                let eventNumber = Int(
                    Date().timeIntervalSince1970 * 1000) & 0x7fff
                guard let down = NSEvent.mouseEvent(
                    with: .leftMouseDown,
                    location: point,
                    modifierFlags: [],
                    timestamp: ProcessInfo.processInfo.systemUptime,
                    windowNumber: context.window.windowNumber,
                    context: nil,
                    eventNumber: eventNumber,
                    clickCount: 1,
                    pressure: 1),
                      let up = NSEvent.mouseEvent(
                        with: .leftMouseUp,
                        location: point,
                        modifierFlags: [],
                        timestamp:
                            ProcessInfo.processInfo.systemUptime + 0.05,
                        windowNumber: context.window.windowNumber,
                        context: nil,
                        eventNumber: eventNumber + 1,
                        clickCount: 1,
                        pressure: 0) else {
                    self.fail(code: failureCode)
                    return
                }
                trace("recv", "clipboardOutcomes.nativePointerDispatch", [
                    "inputMode": "appkit-window-pointer-event",
                    "button": "primary",
                    "stage": stage,
                    "webViewX": geometry.centerX,
                    "webViewY": geometry.centerY,
                    "windowX": point.x,
                    "windowY": point.y,
                    "activeApplication": NSApplication.shared.isActive,
                    "keyWindow": context.window.isKeyWindow,
                    "windowId": context.scope.windowId,
                    "sessionId": context.scope.sessionId,
                    "uiId": context.scope.uiId,
                    "recoveryGeneration": context.recoveryGeneration
                ])
                context.window.sendEvent(down)
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
                    context.window.sendEvent(up)
                    if !self.nativePointerStages.contains(stage) {
                        self.nativePointerStages.append(stage)
                    }
                    self.phase = next
                    self.schedulePoll(after: 0.1)
                }
            }
        }
    }

    private func inspect(
        _ context: FlowIpcWindowContext,
        completion: @escaping (Snapshot?) -> Void
    ) {
        context.webView.evaluateJavaScript(Self.inspectionScript) {
            result, error in
            guard error == nil,
                  let json = result as? String,
                  let data = json.data(using: .utf8),
                  let snapshot = try? JSONDecoder().decode(
                    Snapshot.self,
                    from: data) else {
                completion(nil)
                return
            }
            completion(snapshot)
        }
    }

    private func recordPointer(stage: String, snapshot: Snapshot) {
        guard let observation = snapshot.pointerStages[stage] else {
            return
        }
        if observation.receipt,
           !nativePointerReceiptStages.contains(stage) {
            nativePointerReceiptStages.append(stage)
        }
        if observation.trusted,
           !nativePointerTrustedStages.contains(stage) {
            nativePointerTrustedStages.append(stage)
        }
    }

    private func pointerObserved(
        stage: String,
        snapshot: Snapshot
    ) -> Bool {
        snapshot.pointerStages[stage]?.receipt == true
            && snapshot.pointerStages[stage]?.trusted == true
    }

    private func recordSemanticUidl(_ stage: String) {
        if !semanticUidlStages.contains(stage) {
            semanticUidlStages.append(stage)
        }
    }

    private func exactMainScope(_ snapshot: Snapshot) -> Bool {
        snapshot.windowId == "main"
            && snapshot.sessionId == "flow-ipc-session"
            && snapshot.uiId == "main"
    }

    private func exactSecondaryScope(_ snapshot: Snapshot) -> Bool {
        snapshot.windowId == Self.secondaryWindowId
            && !secondarySessionId.isEmpty
            && snapshot.sessionId == secondarySessionId
            && snapshot.uiId == Self.secondaryWindowId
    }

    private func validOpaqueSecondarySession(_ sessionId: String) -> Bool {
        let prefix = "flow-ipc-window:"
        guard sessionId.hasPrefix(prefix) else {
            return false
        }
        return UUID(uuidString: String(sessionId.dropFirst(prefix.count)))
            != nil
    }

    private func retryOrFail(
        code: String,
        details: [String: Any] = [:],
        delay: Double = 0.1
    ) {
        guard Date() < deadline else {
            fail(code: code, details: details)
            return
        }
        schedulePoll(after: delay)
    }

    private func fail(
        code: String,
        details: [String: Any] = [:]
    ) {
        guard !terminal else { return }
        let restored = restorePasteboardIfOwned()
        terminal = true
        var payload = details
        payload["code"] = code
        payload["phase"] = phase.rawValue
        payload["nativeSnapshotCaptured"] =
            nativeSnapshotCapturedObserved
        payload["nativeSnapshotRestored"] = restored
            && nativeSnapshotRestoredObserved
        payload["nativePasteboardContended"] =
            nativePasteboardContendedObserved
        trace("send", "error", payload)
        closeWindows()
    }

    private func finish(
        main: FlowIpcWindowContext,
        mainSnapshot: Snapshot,
        secondary: FlowIpcWindowContext,
        secondarySnapshot: Snapshot
    ) {
        guard restorePasteboardIfOwned() else {
            fail(
                code: nativePasteboardContendedObserved
                    ? "clipboardOutcomesPasteboardContended"
                    : "clipboardOutcomesPasteboardRestoreFailed")
            return
        }
        let expectedPointerStages = [
            "main-no-text",
            "main-empty",
            "secondary-denied"
        ]
        let expectedBrowserStages = [
            "route-navigate",
            "secondary-open"
        ]
        let expectedSemanticStages = [
            "main-no-text",
            "main-empty",
            "secondary-denied"
        ]
        let nativePointerObserved =
            nativePointerStages == expectedPointerStages
            && nativePointerReceiptStages == expectedPointerStages
        let nativePointerTrustedObserved =
            nativePointerTrustedStages == expectedPointerStages
        let browserProgrammaticSetupObserved =
            browserProgrammaticStages == expectedBrowserStages
        let semanticUidlCheckpointsObserved =
            semanticUidlStages == expectedSemanticStages
        let requestCount = mainSnapshot.requestCount
            + secondarySnapshot.requestCount
        let responseCount = mainSnapshot.responseCount
            + secondarySnapshot.responseCount
        let payload: [String: Any] = [
            "complete": true,
            "standardBundleMode":
                mainSnapshot.firstMeaningfulPaint
                && secondarySnapshot.firstMeaningfulPaint,
            "rootRendered": rootRenderedObserved,
            "flowClientOwned": mainSnapshot.flowClientCount > 0
                && secondarySnapshot.flowClientCount > 0,
            "opaqueRequestObserved": mainNoTextUidlObserved
                && mainEmptyUidlObserved
                && secondaryDeniedUidlObserved,
            "opaqueResponseObserved": mainNoTextUidlObserved
                && mainEmptyUidlObserved
                && secondaryDeniedUidlObserved,
            "mainNoTextOutcomeObserved":
                mainSnapshot.noTextStatus == Self.noTextStatus,
            "mainEmptyWriteOutcomeObserved":
                mainSnapshot.emptyStatus == Self.emptyStatus,
            "mainEmptyReadOutcomeObserved":
                mainSnapshot.emptyStatus == Self.emptyStatus,
            "secondaryDeniedWriteOutcomeObserved":
                secondarySnapshot.deniedStatus == Self.deniedStatus,
            "secondaryDeniedReadOutcomeObserved":
                secondarySnapshot.deniedStatus == Self.deniedStatus,
            "deniedWriteProbeLength": Self.deniedWriteProbeLength,
            "nativeSnapshotCapturedObserved":
                nativeSnapshotCapturedObserved,
            "nativeNoTextSetupObserved": nativeNoTextSetupObserved,
            "nativeNoTextReadNoSideEffectObserved":
                nativeNoTextReadNoSideEffectObserved,
            "nativeEmptyTextObserved": nativeEmptyTextObserved,
            "nativeDeniedNoSideEffectObserved":
                nativeDeniedNoSideEffectObserved,
            "nativeOwnershipTrackedObserved": emptyWriteChangeAdopted,
            "nativeSnapshotRestoredObserved":
                nativeSnapshotRestoredObserved,
            "nativePasteboardContendedObserved":
                nativePasteboardContendedObserved,
            "scopeIsolatedObserved": scopeIsolatedObserved,
            "nativePointerObserved": nativePointerObserved,
            "nativePointerTrustedObserved":
                nativePointerTrustedObserved,
            "browserProgrammaticSetupObserved":
                browserProgrammaticSetupObserved,
            "semanticUidlCheckpointsObserved":
                semanticUidlCheckpointsObserved,
            "balancedDrainObserved": drainBalancedPolls >= 2,
            "resultStableAfterDrain":
                mainSnapshot.noTextStatus == Self.noTextStatus
                && mainSnapshot.emptyStatus == Self.emptyStatus
                && secondarySnapshot.deniedStatus == Self.deniedStatus,
            "errorFree": mainSnapshot.errorFree
                && secondarySnapshot.errorFree,
            "pushObserved": mainPushObserved || secondaryPushObserved,
            "mainWindowId": main.scope.windowId,
            "mainSessionId": main.scope.sessionId,
            "mainUiId": main.scope.uiId,
            "secondaryWindowId": secondary.scope.windowId,
            "secondarySessionId": secondary.scope.sessionId,
            "secondaryUiId": secondary.scope.uiId,
            "noTextStatus": mainSnapshot.noTextStatus,
            "emptyStatus": mainSnapshot.emptyStatus,
            "deniedStatus": secondarySnapshot.deniedStatus,
            "mainNoTextRequestBaseline":
                mainNoTextRequestBaseline,
            "mainNoTextResponseBaseline":
                mainNoTextResponseBaseline,
            "mainEmptyRequestBaseline": mainEmptyRequestBaseline,
            "mainEmptyResponseBaseline": mainEmptyResponseBaseline,
            "secondaryRequestBaseline": secondaryRequestBaseline,
            "secondaryResponseBaseline": secondaryResponseBaseline,
            "mainRequestCount": mainSnapshot.requestCount,
            "mainResponseCount": mainSnapshot.responseCount,
            "secondaryRequestCount": secondarySnapshot.requestCount,
            "secondaryResponseCount": secondarySnapshot.responseCount,
            "requestCount": requestCount,
            "responseCount": responseCount,
            "mainPushMessageCount": mainSnapshot.pushMessageCount,
            "secondaryPushMessageCount":
                secondarySnapshot.pushMessageCount,
            "semanticUidlCheckpointCount": semanticUidlStages.count,
            "semanticUidlCheckpointStages":
                semanticUidlStages.joined(separator: ","),
            "nativePointerEventCount": nativePointerStages.count,
            "nativePointerTrustedEventCount":
                nativePointerTrustedStages.count,
            "nativePointerStages":
                nativePointerStages.joined(separator: ","),
            "browserProgrammaticStages":
                browserProgrammaticStages.joined(separator: ",")
        ]
        terminal = true
        trace(
            "recv",
            "clipboardOutcomes.frameworkAcceptance",
            payload)
        secondary.window.close()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            main.window.close()
        }
    }

    private func closeWindows() {
        host.flowIpcWindowContext(Self.secondaryWindowId)?.window.close()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            self.host.flowIpcWindowContext("main")?.window.close()
        }
    }

    private func mergedPayload(
        mainSnapshot: Snapshot,
        secondarySnapshot: Snapshot?
    ) -> [String: Any] {
        var payload = mainSnapshot.tracePayload(prefix: "main")
        if let secondarySnapshot {
            payload.merge(
                secondarySnapshot.tracePayload(prefix: "secondary")) {
                    _, replacement in replacement
                }
        }
        return payload
    }

    private struct PointerGeometry: Decodable {
        let ready: Bool
        let centerX: Double
        let centerY: Double
    }

    private struct PointerObservation: Decodable {
        let receipt: Bool
        let trusted: Bool
    }

    private struct Snapshot: Decodable {
        let windowId: String
        let sessionId: String
        let uiId: String
        let rootRendered: Bool
        let firstMeaningfulPaint: Bool
        let routeReady: Bool
        let noTextStatus: String
        let emptyStatus: String
        let deniedStatus: String
        let windowStatus: String
        let requestCount: Int
        let responseCount: Int
        let pushMessageCount: Int
        let flowClientCount: Int
        let pointerStages: [String: PointerObservation]
        let browserError: String
        let requestError: String
        let responseError: String
        let pushError: String

        var errorFree: Bool {
            browserError.isEmpty && requestError.isEmpty
                && responseError.isEmpty && pushError.isEmpty
        }

        func tracePayload(prefix: String) -> [String: Any] {
            [
                "\(prefix)WindowId": windowId,
                "\(prefix)SessionId": sessionId,
                "\(prefix)UiId": uiId,
                "\(prefix)RootRendered": rootRendered,
                "\(prefix)FirstMeaningfulPaint": firstMeaningfulPaint,
                "\(prefix)RouteReady": routeReady,
                "\(prefix)NoTextStatus": noTextStatus,
                "\(prefix)EmptyStatus": emptyStatus,
                "\(prefix)DeniedStatus": deniedStatus,
                "\(prefix)WindowStatus": windowStatus,
                "\(prefix)RequestCount": requestCount,
                "\(prefix)ResponseCount": responseCount,
                "\(prefix)PushMessageCount": pushMessageCount,
                "\(prefix)FlowClientCount": flowClientCount,
                "\(prefix)PointerStages": pointerStages.keys.sorted()
                    .joined(separator: ","),
                "\(prefix)BrowserError": browserError,
                "\(prefix)RequestError": requestError,
                "\(prefix)ResponseError": responseError,
                "\(prefix)PushError": pushError
            ]
        }
    }

    private struct PasteboardRestoreResult {
        let restored: Bool
        let contended: Bool
        let ownedChangeCount: Int?
    }

    private struct PasteboardSnapshot {
        private struct Representation {
            let type: NSPasteboard.PasteboardType
            let data: Data
        }

        private struct Item {
            let representations: [Representation]
        }

        private let items: [Item]

        static func capture(from pasteboard: NSPasteboard) -> Self? {
            var snapshots: [Item] = []
            for item in pasteboard.pasteboardItems ?? [] {
                var representations: [Representation] = []
                for type in item.types {
                    guard let data = item.data(forType: type) else {
                        return nil
                    }
                    representations.append(Representation(
                        type: type,
                        data: data))
                }
                snapshots.append(Item(
                    representations: representations))
            }
            return PasteboardSnapshot(items: snapshots)
        }

        func restore(
            to pasteboard: NSPasteboard,
            onlyIfChangeCount expectedChangeCount: Int
        ) -> PasteboardRestoreResult {
            var restoredItems: [NSPasteboardItem] = []
            for item in items {
                let restored = NSPasteboardItem()
                for representation in item.representations {
                    guard restored.setData(
                        representation.data,
                        forType: representation.type) else {
                        return PasteboardRestoreResult(
                            restored: false,
                            contended: false,
                            ownedChangeCount: nil)
                    }
                }
                restoredItems.append(restored)
            }
            guard pasteboard.changeCount == expectedChangeCount else {
                return PasteboardRestoreResult(
                    restored: false,
                    contended: true,
                    ownedChangeCount: nil)
            }
            pasteboard.clearContents()
            var ownedChangeCount = pasteboard.changeCount
            if !restoredItems.isEmpty,
               !pasteboard.writeObjects(restoredItems) {
                ownedChangeCount = pasteboard.changeCount
                return PasteboardRestoreResult(
                    restored: false,
                    contended: false,
                    ownedChangeCount: ownedChangeCount)
            }
            ownedChangeCount = pasteboard.changeCount
            return PasteboardRestoreResult(
                restored: matches(pasteboard),
                contended: false,
                ownedChangeCount: ownedChangeCount)
        }

        private func matches(_ pasteboard: NSPasteboard) -> Bool {
            let currentItems = pasteboard.pasteboardItems ?? []
            guard currentItems.count == items.count else {
                return false
            }
            for (current, expected) in zip(currentItems, items) {
                let currentTypes = current.types
                    .map(\.rawValue).sorted()
                let expectedTypes = expected.representations
                    .map { $0.type.rawValue }.sorted()
                guard currentTypes == expectedTypes else {
                    return false
                }
                for representation in expected.representations {
                    guard current.data(forType: representation.type)
                        == representation.data else {
                        return false
                    }
                }
            }
            return true
        }
    }

    private static func pointerPreparationScript(
        buttonId: String,
        stage: String
    ) -> String {
        """
        (() => {
          const button = document.querySelector('#\(buttonId)');
          if (!button) return JSON.stringify({ready:false});
          const rect = button.getBoundingClientRect();
          const centerX = rect.left + rect.width / 2;
          const centerY = rect.top + rect.height / 2;
          const hit = document.elementFromPoint(centerX, centerY);
          if (!(rect.width > 0 && rect.height > 0)
              || !(hit === button || button.contains(hit))) {
            return JSON.stringify({ready:false, centerX, centerY});
          }
          const marker = window.VaadinDesktopClipboardOutcomesPointer
            ||= {stages: {}, pendingHandlers: {}};
          const previous = marker.pendingHandlers['\(stage)'];
          if (typeof previous === 'function') {
            document.removeEventListener('click', previous, true);
          }
          marker.stages['\(stage)'] = {receipt:false, trusted:false};
          const handler = (event) => {
            if (event.button !== 0 || !event.composedPath().includes(button)) {
              return;
            }
            marker.stages['\(stage)'] = {
              receipt:true,
              trusted:event.isTrusted === true
            };
            document.removeEventListener('click', handler, true);
            delete marker.pendingHandlers['\(stage)'];
          };
          marker.pendingHandlers['\(stage)'] = handler;
          document.addEventListener('click', handler, true);
          return JSON.stringify({ready:true, centerX, centerY});
        })()
        """
    }

    private static let inspectionScript = """
    (() => {
      const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
      const context = window.VaadinDesktopHostContext || {};
      const route = document.querySelector(
        '#framework-flow-clipboard-outcomes-route');
      const clients = window.Vaadin?.Flow?.clients
        ? Object.keys(window.Vaadin.Flow.clients)
          .filter((name) => name !== 'TypeScript')
        : [];
      const pointer = window.VaadinDesktopClipboardOutcomesPointer
        || {stages:{}};
      return JSON.stringify({
        windowId: String(context.windowId || ''),
        sessionId: String(context.sessionId || ''),
        uiId: String(context.uiId || ''),
        rootRendered: !!document.querySelector('#framework-root') || !!route,
        firstMeaningfulPaint:
          diagnostics.standardBundleFirstMeaningfulPaintReported === true,
        routeReady:
          window.location.pathname.endsWith('/flow-clipboard-outcomes')
          && !!route
          && !!document.querySelector(
            '#framework-flow-clipboard-outcomes-read-no-text')
          && !!document.querySelector(
            '#framework-flow-clipboard-outcomes-write-read-empty')
          && !!document.querySelector(
            '#framework-flow-clipboard-outcomes-invoke-denied')
          && !!document.querySelector(
            '#framework-flow-clipboard-outcomes-open-secondary'),
        noTextStatus: String(document.querySelector(
          '#framework-flow-clipboard-outcomes-no-text-status')
          ?.textContent || ''),
        emptyStatus: String(document.querySelector(
          '#framework-flow-clipboard-outcomes-empty-status')
          ?.textContent || ''),
        deniedStatus: String(document.querySelector(
          '#framework-flow-clipboard-outcomes-denied-status')
          ?.textContent || ''),
        windowStatus: String(document.querySelector(
          '#framework-flow-clipboard-outcomes-window-status')
          ?.textContent || ''),
        requestCount: Number(
          diagnostics.standardBundleOpaqueRequestCount || 0),
        responseCount: Number(
          diagnostics.standardBundleOpaqueResponseCount || 0),
        pushMessageCount: Number(
          diagnostics.standardBundlePushMessageCount || 0),
        flowClientCount: clients.length,
        pointerStages: pointer.stages || {},
        browserError: String(
          diagnostics.standardBundleBrowserError || ''),
        requestError: String(
          diagnostics.standardBundleOpaqueRequestError || ''),
        responseError: String(
          diagnostics.standardBundleOpaqueResponseError || ''),
        pushError: String(diagnostics.standardBundlePushError || '')
      });
    })()
    """
}
