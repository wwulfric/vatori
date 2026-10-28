import AppKit
import Foundation

extension FlowIpcAcceptanceRuntimeObserver {
    func scheduleFlowOverlayStackAcceptance(
        host: WebViewHost,
        context: FlowIpcWindowContext,
        timeoutSeconds: Double
    ) {
        FlowIpcOverlayStackAcceptance(
            context: context,
            deadline: Date().addingTimeInterval(timeoutSeconds)
        ).start()
    }
}

private final class FlowIpcOverlayStackAcceptance {
    private enum Phase {
        case ready
        case dedicatedRoute
        case dialog
        case popover
        case confirm
        case confirmClosed
        case popoverClosed
        case dialogClosed
    }

    private let context: FlowIpcWindowContext
    private let deadline: Date
    private var phase = Phase.ready

    init(context: FlowIpcWindowContext, deadline: Date) {
        self.context = context
        self.deadline = deadline
    }

    func start() {
        poll()
    }

    private func poll() {
        inspect { snapshot in
            guard let snapshot else {
                self.retryOrFail(code: "flowOverlayStackInspectionFailed")
                return
            }
            guard snapshot.errorFree else {
                self.fail(
                    code: "flowOverlayStackBrowserRuntimeError",
                    details: snapshot.tracePayload)
                return
            }

            switch self.phase {
            case .ready:
                guard snapshot.ready else {
                    self.retryOrFail(
                        code: "flowOverlayStackReadyTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.perform(
                    Self.navigationScript,
                    next: .dedicatedRoute,
                    failureCode: "flowOverlayStackNavigationFailed")
            case .dedicatedRoute:
                guard snapshot.dedicatedRouteReady else {
                    self.retryOrFail(
                        code: "flowOverlayStackDedicatedRouteTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performWithNativeWindowActive(
                    Self.openDialogScript,
                    next: .dialog,
                    failureCode: "flowOverlayStackDialogOpenFailed")
            case .dialog:
                guard snapshot.dialogOpenedObserved else {
                    self.retryOrFail(
                        code: "flowOverlayStackDialogTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.perform(
                    Self.openPopoverScript,
                    next: .popover,
                    failureCode: "flowOverlayStackPopoverOpenFailed")
            case .popover:
                guard snapshot.popoverOpenedObserved else {
                    self.retryOrFail(
                        code: "flowOverlayStackPopoverTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.perform(
                    Self.openConfirmScript,
                    next: .confirm,
                    failureCode: "flowOverlayStackConfirmOpenFailed")
            case .confirm:
                guard snapshot.threeLevelStackOpenedObserved,
                      snapshot.a11yObserved,
                      snapshot.confirmInitialFocusObserved else {
                    self.retryOrFail(
                        code: "flowOverlayStackThreeLevelTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .escape,
                    Self.prepareConfirmEscapeScript,
                    next: .confirmClosed,
                    failureCode: "flowOverlayStackConfirmEscapeFailed")
            case .confirmClosed:
                guard snapshot.topEscapeObserved,
                      snapshot.parentLayersRemainObserved,
                      snapshot.confirmFocusRestoredObserved else {
                    self.retryOrFail(
                        code: "flowOverlayStackConfirmCloseTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativePrimaryClick(
                    Self.preparePopoverOutsideScript,
                    next: .popoverClosed,
                    failureCode: "flowOverlayStackPopoverOutsideCloseFailed")
            case .popoverClosed:
                guard snapshot.popoverOutsideCloseObserved,
                      snapshot.dialogRemainsObserved,
                      snapshot.outsideFocusPreservedObserved else {
                    self.retryOrFail(
                        code: "flowOverlayStackPopoverCloseTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .escape,
                    Self.prepareDialogEscapeScript,
                    next: .dialogClosed,
                    failureCode: "flowOverlayStackDialogEscapeFailed")
            case .dialogClosed:
                guard snapshot.dialogEscapeObserved,
                      snapshot.layeredFocusRestoredObserved else {
                    self.retryOrFail(
                        code: "flowOverlayStackDialogCloseTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.finish(snapshot)
            }
        }
    }

    private func perform(
        _ script: String,
        next: Phase,
        failureCode: String
    ) {
        context.webView.evaluateJavaScript(script) { result, error in
            guard error == nil, result as? Bool == true else {
                self.retryOrFail(
                    code: failureCode,
                    details: [
                        "message": error?.localizedDescription ?? "",
                        "result": String(describing: result)
                    ])
                return
            }
            self.phase = next
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                self.poll()
            }
        }
    }

    private func performWithNativeWindowActive(
        _ script: String,
        next: Phase,
        failureCode: String
    ) {
        prepareWindowForNativeInput(failureCode: failureCode) {
            self.perform(
                script,
                next: next,
                failureCode: failureCode)
        }
    }

    private func performNativeKey(
        _ key: NativeKey,
        _ preparationScript: String,
        next: Phase,
        failureCode: String
    ) {
        let instrumentedScript = """
        (() => {
          const prepared = \(preparationScript);
          if (prepared !== true) return false;
          const marker = window.VaadinDesktopFlowOverlayStack || {};
          window.VaadinDesktopFlowOverlayStack = marker;
          const nativeKeyboardStage =
            String(marker.nativeKeyboardStage || '');
          marker.nativeKeyboardCurrentObserved = false;
          const nativeKeyboardHandler = (event) => {
            if (String(event.key || '') !== '\(key.name)') return;
            document.removeEventListener(
              'keydown', nativeKeyboardHandler, true);
            marker.nativeKeyboardObserved = true;
            marker.nativeKeyboardCurrentObserved = true;
            marker.nativeKeyboardEventCount =
              Number(marker.nativeKeyboardEventCount || 0) + 1;
            if (event.isTrusted === true) {
              marker.nativeKeyboardTrustedEventCount =
                Number(marker.nativeKeyboardTrustedEventCount || 0) + 1;
            }
            marker.nativeKeyboardTrustedObserved =
              Number(marker.nativeKeyboardTrustedEventCount || 0)
                === Number(marker.nativeKeyboardEventCount || 0);
            marker.nativeKeyboardStages = [
              String(marker.nativeKeyboardStages || ''),
              nativeKeyboardStage
            ].filter(Boolean).join(',');
            marker.nativeKeyboardLastKey = String(event.key || '');
            marker.nativeKeyboardLastTarget =
              String(event.target?.id || event.target?.localName || '');
          };
          document.addEventListener(
            'keydown', nativeKeyboardHandler, true);
          return true;
        })()
        """
        context.webView.evaluateJavaScript(instrumentedScript) {
            result, error in
            guard error == nil, result as? Bool == true else {
                self.retryOrFail(
                    code: failureCode,
                    details: [
                        "message": error?.localizedDescription ?? "",
                        "result": String(describing: result)
                    ])
                return
            }
            self.prepareWindowForNativeInput(
                failureCode: failureCode
            ) {
                let timestamp = ProcessInfo.processInfo.systemUptime
                guard let keyDown = key.event(
                          type: .keyDown,
                          windowNumber:
                              self.context.window.windowNumber,
                          timestamp: timestamp),
                      let keyUp = key.event(
                          type: .keyUp,
                          windowNumber:
                              self.context.window.windowNumber,
                          timestamp: timestamp + 0.01) else {
                    self.retryOrFail(code: failureCode, details: [
                        "message":
                            "Unable to create AppKit keyboard event",
                        "key": key.name
                    ])
                    return
                }
                trace("recv", "flowOverlayStack.nativeKeyboardDispatch", [
                    "inputMode": "appkit-window-key-event",
                    "key": key.name,
                    "activeApplication":
                        NSApplication.shared.isActive,
                    "keyWindow": self.context.window.isKeyWindow,
                    "firstResponder":
                        String(describing:
                            self.context.window.firstResponder),
                    "windowNumber":
                        self.context.window.windowNumber
                ])
                self.context.window.sendEvent(keyDown)
                self.context.window.sendEvent(keyUp)
                self.phase = next
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                    self.poll()
                }
            }
        }
    }

    private func performNativePrimaryClick(
        _ preparationScript: String,
        next: Phase,
        failureCode: String
    ) {
        let instrumentedScript = """
        (() => {
          const prepared = \(preparationScript);
          if (!prepared || typeof prepared !== 'object') return '';
          const marker = window.VaadinDesktopFlowOverlayStack || {};
          window.VaadinDesktopFlowOverlayStack = marker;
          const expectedTargetId = String(prepared.targetId || '');
          const nativePointerStage =
            String(marker.nativePointerStage || '');
          marker.nativePointerCurrentObserved = false;
          const nativePointerHandler = (event) => {
            const path = typeof event.composedPath === 'function'
              ? event.composedPath()
              : [];
            if (Number(event.button) !== 0
                || !path.some((element) =>
                  element?.id === expectedTargetId)) return;
            document.removeEventListener(
              'click', nativePointerHandler, true);
            marker.nativePointerObserved = true;
            marker.nativePointerCurrentObserved = true;
            marker.nativePointerEventCount =
              Number(marker.nativePointerEventCount || 0) + 1;
            if (event.isTrusted === true) {
              marker.nativePointerTrustedEventCount =
                Number(marker.nativePointerTrustedEventCount || 0) + 1;
            }
            marker.nativePointerTrustedObserved =
              Number(marker.nativePointerTrustedEventCount || 0)
                === Number(marker.nativePointerEventCount || 0);
            marker.nativePointerStages = [
              String(marker.nativePointerStages || ''),
              nativePointerStage
            ].filter(Boolean).join(',');
            marker.nativePointerLastTarget =
              String(event.target?.id || event.target?.localName || '');
          };
          document.addEventListener(
            'click', nativePointerHandler, true);
          return JSON.stringify({
            centerX: Number(prepared.centerX),
            centerY: Number(prepared.centerY),
            targetId: expectedTargetId,
            targetVisible: prepared.targetVisible === true
          });
        })()
        """
        context.webView.evaluateJavaScript(instrumentedScript) {
            result, error in
            guard error == nil,
                  let json = result as? String,
                  let data = json.data(using: .utf8),
                  let geometry = try? JSONDecoder().decode(
                    PointerGeometry.self,
                    from: data) else {
                self.retryOrFail(
                    code: failureCode,
                    details: [
                        "message": error?.localizedDescription ?? "",
                        "result": String(describing: result)
                    ])
                return
            }
            guard geometry.targetVisible else {
                self.retryOrFail(code: failureCode, details: [
                    "message": "Popover outside target is not visible",
                    "targetId": geometry.targetId,
                    "webViewX": geometry.centerX,
                    "webViewY": geometry.centerY
                ])
                return
            }
            self.prepareWindowForNativeInput(
                failureCode: failureCode
            ) {
                let webViewPoint = NSPoint(
                    x: geometry.centerX,
                    y: self.context.webView.isFlipped
                        ? geometry.centerY
                        : self.context.webView.bounds.height
                            - geometry.centerY)
                let windowPoint = self.context.webView.convert(
                    webViewPoint,
                    to: nil)
                guard self.context.window.contentLayoutRect.contains(
                        windowPoint) else {
                    self.retryOrFail(code: failureCode, details: [
                        "message":
                            "Popover outside target is outside the host window",
                        "windowX": windowPoint.x,
                        "windowY": windowPoint.y
                    ])
                    return
                }
                let timestamp = ProcessInfo.processInfo.systemUptime
                guard let mouseDown = NSEvent.mouseEvent(
                          with: .leftMouseDown,
                          location: windowPoint,
                          modifierFlags: [],
                          timestamp: timestamp,
                          windowNumber:
                              self.context.window.windowNumber,
                          context: nil,
                          eventNumber: 0,
                          clickCount: 1,
                          pressure: 1),
                      let mouseUp = NSEvent.mouseEvent(
                          with: .leftMouseUp,
                          location: windowPoint,
                          modifierFlags: [],
                          timestamp: timestamp + 0.02,
                          windowNumber:
                              self.context.window.windowNumber,
                          context: nil,
                          eventNumber: 1,
                          clickCount: 1,
                          pressure: 0) else {
                    self.retryOrFail(code: failureCode, details: [
                        "message": "Unable to create AppKit pointer event"
                    ])
                    return
                }
                trace("recv", "flowOverlayStack.nativePointerDispatch", [
                    "inputMode": "appkit-window-pointer-event",
                    "button": "primary",
                    "targetId": geometry.targetId,
                    "webViewX": geometry.centerX,
                    "webViewY": geometry.centerY,
                    "windowX": windowPoint.x,
                    "windowY": windowPoint.y,
                    "windowNumber":
                        self.context.window.windowNumber
                ])
                self.context.window.sendEvent(mouseDown)
                self.context.window.sendEvent(mouseUp)
                self.phase = next
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.75) {
                    self.poll()
                }
            }
        }
    }

    private func prepareWindowForNativeInput(
        failureCode: String,
        completion: @escaping () -> Void
    ) {
        requestWindowActivation()
        waitForWindowActivation(
            failureCode: failureCode,
            completion: completion)
    }

    private func requestWindowActivation() {
        NSApplication.shared.unhide(nil)
        context.window.makeKeyAndOrderFront(nil)
        NSRunningApplication.current.activate(options: [.activateAllWindows])
        NSApplication.shared.activate(ignoringOtherApps: true)
    }

    private func waitForWindowActivation(
        failureCode: String,
        completion: @escaping () -> Void
    ) {
        guard Date() < deadline else {
            fail(code: failureCode, details: [
                "message":
                    "Timed out waiting for native input activation",
                "activeApplication": NSApplication.shared.isActive,
                "keyWindow": context.window.isKeyWindow
            ])
            return
        }
        guard NSApplication.shared.isActive,
              context.window.isKeyWindow else {
            requestWindowActivation()
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                self.waitForWindowActivation(
                    failureCode: failureCode,
                    completion: completion)
            }
            return
        }
        let responder = context.window.firstResponder
        let responderView = responder as? NSView
        let webViewOwnsResponder =
            responderView === context.webView
                || responderView?.isDescendant(of: context.webView) == true
        if !webViewOwnsResponder,
           !context.window.makeFirstResponder(context.webView) {
            fail(code: failureCode, details: [
                "message": "Unable to route native input to WKWebView",
                "previousResponder": String(describing: responder)
            ])
            return
        }
        completion()
    }

    private func inspect(_ completion: @escaping (Snapshot?) -> Void) {
        context.webView.evaluateJavaScript(Self.inspectionScript) { result, error in
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

    private func retryOrFail(
        code: String,
        details: [String: Any] = [:]
    ) {
        if Date() >= deadline {
            fail(code: code, details: details)
            return
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            self.poll()
        }
    }

    private func finish(_ snapshot: Snapshot) {
        var payload = snapshot.tracePayload
        payload.merge([
            "complete": snapshot.complete,
            "standardBundleMode": snapshot.firstMeaningfulPaint,
            "rootRendered": snapshot.rootRendered,
            "flowClientOwned": snapshot.flowClientCount > 0,
            "opaqueRequestBridgeObserved": snapshot.requestCount >= 5,
            "opaqueResponseBridgeObserved": snapshot.responseCount >= 5,
            "pushObserved": snapshot.pushMessageCount >= 1,
            "errorFree": snapshot.errorFree,
            "flowOverlayStackBrowserProgrammaticSetupObserved": true
        ]) { _, replacement in replacement }
        trace("recv", "flowOverlayStack.frameworkAcceptance", payload)
        context.window.close()
    }

    private func fail(code: String, details: [String: Any] = [:]) {
        var payload = details
        payload["code"] = code
        payload["phase"] = String(describing: phase)
        trace("send", "error", payload)
        context.window.close()
    }

    private struct PointerGeometry: Decodable {
        let centerX: Double
        let centerY: Double
        let targetId: String
        let targetVisible: Bool
    }

    private enum NativeKey {
        case escape

        var name: String {
            "Escape"
        }

        func event(
            type: NSEvent.EventType,
            windowNumber: Int,
            timestamp: TimeInterval
        ) -> NSEvent? {
            NSEvent.keyEvent(
                with: type,
                location: .zero,
                modifierFlags: [],
                timestamp: timestamp,
                windowNumber: windowNumber,
                context: nil,
                characters: "\u{1b}",
                charactersIgnoringModifiers: "\u{1b}",
                isARepeat: false,
                keyCode: 53)
        }
    }

    private struct Snapshot: Decodable {
        let rootRendered: Bool
        let firstMeaningfulPaint: Bool
        let requestCount: Int
        let responseCount: Int
        let pushMessageCount: Int
        let flowClientCount: Int
        let dedicatedRouteReady: Bool
        let path: String
        let dialogOpenedObserved: Bool
        let popoverOpenedObserved: Bool
        let confirmOpenedObserved: Bool
        let threeLevelStackOpenedObserved: Bool
        let a11yObserved: Bool
        let confirmInitialFocusObserved: Bool
        let nativeKeyboardObserved: Bool
        let nativeKeyboardTrustedObserved: Bool
        let nativeKeyboardEventCount: Int
        let nativeKeyboardTrustedEventCount: Int
        let nativeKeyboardStages: String
        let nativePointerObserved: Bool
        let nativePointerTrustedObserved: Bool
        let nativePointerEventCount: Int
        let nativePointerTrustedEventCount: Int
        let nativePointerStages: String
        let topEscapeObserved: Bool
        let parentLayersRemainObserved: Bool
        let confirmFocusRestoredObserved: Bool
        let popoverOutsideCloseObserved: Bool
        let dialogRemainsObserved: Bool
        let outsideFocusPreservedObserved: Bool
        let dialogEscapeObserved: Bool
        let layeredFocusRestoredObserved: Bool
        let dialogOpenedCurrent: Bool
        let dialogInitialFocusCurrent: Bool
        let dialogA11yCurrent: Bool
        let confirmA11yCurrent: Bool
        let confirmInitialFocusCurrent: Bool
        let dialogHostRole: String
        let dialogOverlayRole: String
        let dialogRoleAriaModal: String
        let dialogOverlayAriaModal: String
        let dialogOverlayText: String
        let confirmHostRole: String
        let confirmRoleAriaModal: String
        let confirmAccessibleText: String
        let confirmActionText: String
        let dialogLifecycleStatus: String
        let popoverLifecycleStatus: String
        let confirmLifecycleStatus: String
        let confirmClosedStatus: String
        let dialogRequestStatus: String
        let confirmRequestStatus: String
        let activePath: String
        let markerError: String
        let browserError: String
        let requestError: String
        let responseError: String
        let pushError: String

        var errorFree: Bool {
            markerError.isEmpty
                && browserError.isEmpty
                && requestError.isEmpty
                && responseError.isEmpty
                && pushError.isEmpty
        }

        var ready: Bool {
            rootRendered
                && firstMeaningfulPaint
                && requestCount >= 2
                && responseCount >= 2
                && pushMessageCount >= 1
                && flowClientCount > 0
                && errorFree
        }

        var complete: Bool {
            errorFree
                && dedicatedRouteReady
                && dialogOpenedObserved
                && popoverOpenedObserved
                && confirmOpenedObserved
                && threeLevelStackOpenedObserved
                && a11yObserved
                && confirmInitialFocusObserved
                && nativeKeyboardObserved
                && nativeKeyboardTrustedObserved
                && nativeKeyboardEventCount == 2
                && nativeKeyboardTrustedEventCount == 2
                && nativeKeyboardStages == "confirm-close,dialog-close"
                && nativePointerObserved
                && nativePointerTrustedObserved
                && nativePointerEventCount == 1
                && nativePointerTrustedEventCount == 1
                && nativePointerStages == "popover-outside"
                && topEscapeObserved
                && parentLayersRemainObserved
                && confirmFocusRestoredObserved
                && popoverOutsideCloseObserved
                && dialogRemainsObserved
                && outsideFocusPreservedObserved
                && dialogEscapeObserved
                && layeredFocusRestoredObserved
                && requestCount >= 5
                && responseCount >= 5
                && pushMessageCount >= 1
        }

        var tracePayload: [String: Any] {
            [
                "flowOverlayStackDedicatedRouteObserved": dedicatedRouteReady,
                "flowOverlayStackPath": path,
                "flowOverlayStackDialogOpenedObserved": dialogOpenedObserved,
                "flowOverlayStackPopoverOpenedObserved": popoverOpenedObserved,
                "flowOverlayStackConfirmOpenedObserved": confirmOpenedObserved,
                "flowOverlayStackThreeLevelOpenedObserved":
                    threeLevelStackOpenedObserved,
                "flowOverlayStackA11yObserved": a11yObserved,
                "flowOverlayStackConfirmInitialFocusObserved":
                    confirmInitialFocusObserved,
                "flowOverlayStackNativeKeyboardObserved":
                    nativeKeyboardObserved,
                "flowOverlayStackNativeKeyboardTrustedObserved":
                    nativeKeyboardTrustedObserved,
                "flowOverlayStackNativeKeyboardEventCount":
                    nativeKeyboardEventCount,
                "flowOverlayStackNativeKeyboardTrustedEventCount":
                    nativeKeyboardTrustedEventCount,
                "flowOverlayStackNativeKeyboardStages":
                    nativeKeyboardStages,
                "flowOverlayStackNativePointerObserved":
                    nativePointerObserved,
                "flowOverlayStackNativePointerTrustedObserved":
                    nativePointerTrustedObserved,
                "flowOverlayStackNativePointerEventCount":
                    nativePointerEventCount,
                "flowOverlayStackNativePointerTrustedEventCount":
                    nativePointerTrustedEventCount,
                "flowOverlayStackNativePointerStages":
                    nativePointerStages,
                "flowOverlayStackTopEscapeObserved": topEscapeObserved,
                "flowOverlayStackParentLayersRemainObserved":
                    parentLayersRemainObserved,
                "flowOverlayStackConfirmFocusRestoredObserved":
                    confirmFocusRestoredObserved,
                "flowOverlayStackPopoverOutsideCloseObserved":
                    popoverOutsideCloseObserved,
                "flowOverlayStackDialogRemainsObserved":
                    dialogRemainsObserved,
                "flowOverlayStackOutsideFocusPreservedObserved":
                    outsideFocusPreservedObserved,
                "flowOverlayStackDialogEscapeObserved": dialogEscapeObserved,
                "flowOverlayStackLayeredFocusRestoredObserved":
                    layeredFocusRestoredObserved,
                "flowOverlayStackDialogOpenedCurrent": dialogOpenedCurrent,
                "flowOverlayStackDialogInitialFocusCurrent":
                    dialogInitialFocusCurrent,
                "flowOverlayStackDialogA11yCurrent": dialogA11yCurrent,
                "flowOverlayStackConfirmA11yCurrent": confirmA11yCurrent,
                "flowOverlayStackConfirmInitialFocusCurrent":
                    confirmInitialFocusCurrent,
                "flowOverlayStackDialogHostRole": dialogHostRole,
                "flowOverlayStackDialogOverlayRole": dialogOverlayRole,
                "flowOverlayStackDialogRoleAriaModal": dialogRoleAriaModal,
                "flowOverlayStackDialogOverlayAriaModal":
                    dialogOverlayAriaModal,
                "flowOverlayStackDialogOverlayText": dialogOverlayText,
                "flowOverlayStackConfirmHostRole": confirmHostRole,
                "flowOverlayStackConfirmRoleAriaModal":
                    confirmRoleAriaModal,
                "flowOverlayStackConfirmAccessibleText":
                    confirmAccessibleText,
                "flowOverlayStackConfirmActionText": confirmActionText,
                "flowOverlayStackDialogLifecycleStatus":
                    dialogLifecycleStatus,
                "flowOverlayStackPopoverLifecycleStatus":
                    popoverLifecycleStatus,
                "flowOverlayStackConfirmLifecycleStatus":
                    confirmLifecycleStatus,
                "flowOverlayStackConfirmClosedStatus": confirmClosedStatus,
                "flowOverlayStackDialogRequestStatus": dialogRequestStatus,
                "flowOverlayStackConfirmRequestStatus": confirmRequestStatus,
                "flowOverlayStackActivePath": activePath,
                "flowOverlayStackMarkerError": markerError,
                "requestCount": requestCount,
                "responseCount": responseCount,
                "pushMessageCount": pushMessageCount,
                "flowClientCount": flowClientCount,
                "browserError": browserError,
                "requestError": requestError,
                "responseError": responseError,
                "pushError": pushError
            ]
        }
    }

    private static let navigationScript = """
    (() => {
      const element =
        document.querySelector('#framework-flow-overlay-stack-open-route');
      if (!element || typeof element.click !== 'function') return false;
      element.click();
      return true;
    })()
    """

    private static let openDialogScript = """
    (() => {
      const trigger =
        document.querySelector('#framework-flow-overlay-stack-open');
      if (!trigger || typeof trigger.click !== 'function') return false;
      window.VaadinDesktopFlowOverlayStack = {
        dialogOpenedObserved: false,
        popoverOpenedObserved: false,
        confirmOpenedObserved: false,
        threeLevelStackOpenedObserved: false,
        a11yObserved: false,
        confirmInitialFocusObserved: false,
        nativeKeyboardObserved: false,
        nativeKeyboardTrustedObserved: false,
        nativeKeyboardEventCount: 0,
        nativeKeyboardTrustedEventCount: 0,
        nativeKeyboardStages: '',
        nativeKeyboardStage: '',
        nativePointerObserved: false,
        nativePointerTrustedObserved: false,
        nativePointerEventCount: 0,
        nativePointerTrustedEventCount: 0,
        nativePointerStages: '',
        nativePointerStage: '',
        topEscapeDispatched: false,
        parentLayersRemainObserved: false,
        confirmFocusRestoredObserved: false,
        popoverOutsideDispatched: false,
        dialogRemainsObserved: false,
        outsideFocusPreservedObserved: false,
        dialogEscapeDispatched: false,
        layeredFocusRestoredObserved: false,
        error: ''
      };
      trigger.scrollIntoView({ block: 'center', inline: 'center' });
      trigger.focus({ preventScroll: true });
      trigger.click();
      return true;
    })()
    """

    private static let openPopoverScript = """
    (() => {
      const dialog =
        document.querySelector('#framework-flow-overlay-stack-dialog');
      const target =
        document.querySelector('#framework-flow-overlay-stack-popover-target');
      if (!dialog || dialog.opened !== true
          || !target || typeof target.click !== 'function') return false;
      target.focus({ preventScroll: true });
      target.click();
      return true;
    })()
    """

    private static let openConfirmScript = """
    (() => {
      const popover =
        document.querySelector('#framework-flow-overlay-stack-popover');
      const trigger =
        document.querySelector('#framework-flow-overlay-stack-confirm-open');
      if (!popover || popover.opened !== true
          || !trigger || typeof trigger.click !== 'function') return false;
      trigger.focus({ preventScroll: true });
      trigger.click();
      return true;
    })()
    """

    private static let prepareConfirmEscapeScript = """
    (() => {
      const marker = window.VaadinDesktopFlowOverlayStack || {};
      window.VaadinDesktopFlowOverlayStack = marker;
      try {
        const dialog =
          document.querySelector('#framework-flow-overlay-stack-dialog');
        const popover =
          document.querySelector('#framework-flow-overlay-stack-popover');
        const confirm =
          document.querySelector('#framework-flow-overlay-stack-confirm');
        if (!dialog || dialog.opened !== true
            || !popover || popover.opened !== true
            || !confirm || confirm.opened !== true) {
          marker.error = 'three-level-overlay-stack-not-open';
          return true;
        }
        marker.nativeKeyboardStage = 'confirm-close';
        marker.topEscapeDispatched = true;
        return true;
      } catch (error) {
        marker.error =
          String(error?.message || error || 'confirm-escape-error');
        return true;
      }
    })()
    """

    private static let preparePopoverOutsideScript = """
    (() => {
      const marker = window.VaadinDesktopFlowOverlayStack || {};
      window.VaadinDesktopFlowOverlayStack = marker;
      const activeDeepElements = () => {
        const elements = [];
        let current = document.activeElement;
        while (current) {
          elements.push(current);
          current = current.shadowRoot?.activeElement || null;
        }
        return elements;
      };
      try {
        const dialog =
          document.querySelector('#framework-flow-overlay-stack-dialog');
        const popover =
          document.querySelector('#framework-flow-overlay-stack-popover');
        const confirm =
          document.querySelector('#framework-flow-overlay-stack-confirm');
        const confirmTrigger =
          document.querySelector('#framework-flow-overlay-stack-confirm-open');
        const outside =
          document.querySelector(
            '#framework-flow-overlay-stack-dialog-outside-target');
        if (!dialog || dialog.opened !== true
            || !popover || popover.opened !== true
            || (confirm && confirm.opened === true)
          || !confirmTrigger || !outside) {
          marker.error = 'popover-outside-close-precondition-failed';
          return {
            centerX: 0,
            centerY: 0,
            targetId: '',
            targetVisible: false
          };
        }
        marker.confirmFocusRestoredObserved =
          activeDeepElements().includes(confirmTrigger);
        outside.scrollIntoView({ block: 'center', inline: 'center' });
        const rect = outside.getBoundingClientRect();
        const centerX = rect.left + rect.width / 2;
        const centerY = rect.top + rect.height / 2;
        const targetVisible = rect.width > 0 && rect.height > 0
          && centerX >= 0 && centerX <= window.innerWidth
          && centerY >= 0 && centerY <= window.innerHeight;
        marker.nativePointerStage = 'popover-outside';
        marker.popoverOutsideDispatched = true;
        return {
          centerX,
          centerY,
          targetId: outside.id,
          targetVisible
        };
      } catch (error) {
        marker.error =
          String(error?.message || error || 'popover-outside-close-error');
        return {
          centerX: 0,
          centerY: 0,
          targetId: '',
          targetVisible: false
        };
      }
    })()
    """

    private static let prepareDialogEscapeScript = """
    (() => {
      const marker = window.VaadinDesktopFlowOverlayStack || {};
      window.VaadinDesktopFlowOverlayStack = marker;
      try {
        const dialog =
          document.querySelector('#framework-flow-overlay-stack-dialog');
        const popover =
          document.querySelector('#framework-flow-overlay-stack-popover');
        const outside =
          document.querySelector(
            '#framework-flow-overlay-stack-dialog-outside-target');
        if (!dialog || dialog.opened !== true
            || !popover || popover.opened === true || !outside) {
          marker.error = 'dialog-escape-precondition-failed';
          return true;
        }
        marker.nativeKeyboardStage = 'dialog-close';
        marker.dialogEscapeDispatched = true;
        return true;
      } catch (error) {
        marker.error =
          String(error?.message || error || 'dialog-escape-error');
        return true;
      }
    })()
    """

    private static let inspectionScript = """
    (() => {
      const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
      const marker = window.VaadinDesktopFlowOverlayStack || {};
      window.VaadinDesktopFlowOverlayStack = marker;
      const findDeep = (predicate) => {
        const visit = (node) => {
          if (!node) return null;
          if (node.nodeType === 1 && predicate(node)) return node;
          if (node.shadowRoot) {
            const shadowResult = visit(node.shadowRoot);
            if (shadowResult) return shadowResult;
          }
          const children = node.children || node.childNodes || [];
          for (const child of children) {
            const result = visit(child);
            if (result) return result;
          }
          return null;
        };
        return visit(document);
      };
      const findDeepFrom = (rootNode, predicate) => {
        const visit = (node) => {
          if (!node) return null;
          if (node.nodeType === 1 && predicate(node)) return node;
          if (node.shadowRoot) {
            const shadowResult = visit(node.shadowRoot);
            if (shadowResult) return shadowResult;
          }
          const children = node.children || node.childNodes || [];
          for (const child of children) {
            const result = visit(child);
            if (result) return result;
          }
          return null;
        };
        return visit(rootNode);
      };
      const collectDeepText = (rootNode) => {
        const parts = [];
        const append = (value) => {
          const text = String(value || '').replace(/\\s+/g, ' ').trim();
          if (text) parts.push(text);
        };
        const visit = (node) => {
          if (!node) return;
          if (node.nodeType === Node.TEXT_NODE) {
            append(node.textContent);
            return;
          }
          if (node.nodeType !== Node.ELEMENT_NODE
              && node.nodeType !== Node.DOCUMENT_FRAGMENT_NODE
              && node.nodeType !== Node.DOCUMENT_NODE) {
            return;
          }
          if (node.nodeType === Node.ELEMENT_NODE) {
            for (const name of [
              'aria-label', 'aria-description', 'title', 'value'
            ]) {
              if (node.hasAttribute?.(name)) append(node.getAttribute(name));
            }
          }
          if (node.shadowRoot) visit(node.shadowRoot);
          if (typeof node.assignedNodes === 'function') {
            for (const child of node.assignedNodes({ flatten: true })) {
              visit(child);
            }
          }
          const children = node.childNodes || [];
          for (const child of children) visit(child);
        };
        visit(rootNode);
        return parts.join(' ').replace(/\\s+/g, ' ').trim();
      };
      const activeDeepElements = () => {
        const elements = [];
        let current = document.activeElement;
        while (current) {
          elements.push(current);
          current = current.shadowRoot?.activeElement || null;
        }
        return elements;
      };
      const activePath = activeDeepElements();
      const activeWithin = (rootNode) =>
        !!rootNode && activePath.some((element) =>
          element === rootNode
            || rootNode.contains?.(element)
            || rootNode.shadowRoot?.contains?.(element));
      const dialog =
        document.querySelector('#framework-flow-overlay-stack-dialog');
      const dialogTrigger =
        document.querySelector('#framework-flow-overlay-stack-open');
      const popover =
        document.querySelector('#framework-flow-overlay-stack-popover');
      const popoverTarget =
        document.querySelector('#framework-flow-overlay-stack-popover-target');
      const confirm =
        document.querySelector('#framework-flow-overlay-stack-confirm');
      const confirmTrigger =
        document.querySelector('#framework-flow-overlay-stack-confirm-open');
      const outside =
        document.querySelector(
          '#framework-flow-overlay-stack-dialog-outside-target');
      const dialogLifecycleStatus =
        document.querySelector(
          '#framework-flow-overlay-stack-dialog-lifecycle-status')
          ?.textContent || '';
      const popoverLifecycleStatus =
        document.querySelector(
          '#framework-flow-overlay-stack-popover-lifecycle-status')
          ?.textContent || '';
      const confirmLifecycleStatus =
        document.querySelector(
          '#framework-flow-overlay-stack-confirm-lifecycle-status')
          ?.textContent || '';
      const confirmClosedStatus =
        document.querySelector(
          '#framework-flow-overlay-stack-confirm-closed-status')
          ?.textContent || '';
      const dialogRequestStatus =
        document.querySelector(
          '#framework-flow-overlay-stack-dialog-request-status')
          ?.textContent || '';
      const confirmRequestStatus =
        document.querySelector(
          '#framework-flow-overlay-stack-confirm-request-status')
          ?.textContent || '';
      const dialogOverlay = findDeep((element) =>
        element.localName === 'vaadin-dialog-overlay'
          && collectDeepText(element).includes(
            'Flow IPC three-level overlay stack'));
      const popoverOverlay =
        popover?.shadowRoot?.querySelector('vaadin-popover-overlay');
      const confirmOverlay = findDeep((element) =>
        element.localName === 'vaadin-confirm-dialog-overlay'
          || (element.localName === 'vaadin-dialog-overlay'
            && collectDeepText(element).includes(
              'Flow IPC top confirmation')));
      const dialogOpened =
        dialog?.opened === true
          || dialog?.hasAttribute?.('opened') === true
          || dialogOverlay?.opened === true
          || dialogOverlay?.hasAttribute?.('opened') === true;
      const popoverOpened =
        popover?.opened === true
          || popover?.hasAttribute?.('opened') === true;
      const confirmOpened =
        confirm?.opened === true
          || confirm?.hasAttribute?.('opened') === true
          || confirmOverlay?.opened === true
          || confirmOverlay?.hasAttribute?.('opened') === true;
      const dialogRoleElement =
        (['dialog', 'alertdialog'].includes(
          String(dialog?.getAttribute?.('role') || '').toLowerCase())
          ? dialog : null)
        || (dialogOverlay && (['dialog', 'alertdialog'].includes(
          String(dialogOverlay.getAttribute?.('role') || '').toLowerCase())
          ? dialogOverlay : null))
        || (dialogOverlay
          ? findDeepFrom(dialogOverlay, (element) =>
              ['dialog', 'alertdialog'].includes(
                String(element.getAttribute?.('role') || '').toLowerCase()))
          : null);
      const confirmRoleElement =
        (String(confirm?.getAttribute?.('role') || '').toLowerCase()
          === 'alertdialog' ? confirm : null)
        || (confirmOverlay && (
          String(confirmOverlay.getAttribute?.('role') || '').toLowerCase()
            === 'alertdialog' ? confirmOverlay : null))
        || (confirmOverlay
          ? findDeepFrom(confirmOverlay, (element) =>
              String(element.getAttribute?.('role') || '').toLowerCase()
                === 'alertdialog')
          : null);
      const confirmActionButton =
        confirm?.querySelector?.('[slot="confirm-button"]')
          || (confirm
            ? findDeepFrom(confirm, (element) =>
                element.localName === 'vaadin-button'
                  && collectDeepText(element).includes('Confirm'))
            : null);
      const confirmAccessibleText = [
        confirm?.getAttribute?.('aria-label') || '',
        confirm?.getAttribute?.('aria-description') || '',
        confirmRoleElement?.getAttribute?.('aria-label') || '',
        confirmRoleElement?.getAttribute?.('aria-description') || '',
        confirm ? collectDeepText(confirm) : '',
        confirmOverlay ? collectDeepText(confirmOverlay) : ''
      ].join(' ').replace(/\\s+/g, ' ').trim();
      const popoverAriaTarget = popoverTarget?.ariaTarget || popoverTarget;
      const dialogA11yCurrent =
        dialogOpened
          && !!dialogRoleElement
          && String(
            dialogRoleElement.getAttribute?.('aria-modal')
              || dialogOverlay?.getAttribute?.('aria-modal') || '')
            .toLowerCase() === 'true'
          && [
            dialog?.getAttribute?.('aria-label') || '',
            dialogRoleElement.getAttribute?.('aria-label') || '',
            dialogOverlay ? collectDeepText(dialogOverlay) : ''
          ].join(' ').includes('Flow IPC three-level overlay stack');
      const popoverA11yCurrent =
        popoverOpened
          && !!popover
          && !!popoverOverlay
          && popover.target === popoverTarget
          && popoverOverlay.positionTarget === popoverTarget
          && popoverOverlay.modeless === true
          && popoverOverlay.focusTrap !== true
          && String(popover.getAttribute?.('role') || '').toLowerCase()
            === 'dialog'
          && String(popover.getAttribute?.('aria-label') || '')
            .includes('Flow IPC middle modeless popover')
          && popoverAriaTarget?.getAttribute?.('aria-expanded') === 'true'
          && popoverAriaTarget?.getAttribute?.('aria-controls') === popover.id
          && popoverAriaTarget?.getAttribute?.('aria-haspopup') === 'dialog';
      const confirmA11yCurrent =
        confirmOpened
          && !!confirmRoleElement
          && String(
            confirmRoleElement.getAttribute?.('aria-modal')
              || confirmOverlay?.getAttribute?.('aria-modal') || '')
            .toLowerCase() === 'true'
          && [
            'Flow IPC top confirmation',
            'Verify the top modal layer without closing its parents',
            'Confirm',
            'Cancel'
          ].every((text) => confirmAccessibleText.includes(text));
      const dialogInitialFocusCurrent =
        dialogOpened
          && !!popoverTarget
          && activePath.includes(popoverTarget);
      const confirmInitialFocusCurrent =
        confirmOpened
          && !!confirmActionButton
          && activeWithin(confirmActionButton);

      marker.dialogOpenedObserved =
        marker.dialogOpenedObserved === true
          || (dialogOpened
            && dialogInitialFocusCurrent
            && dialogA11yCurrent
            && dialogRequestStatus.includes('dialog request: 1')
            && dialogLifecycleStatus.includes(
              'Dialog lifecycle: opened client=false'));
      marker.popoverOpenedObserved =
        marker.popoverOpenedObserved === true
          || (dialogOpened
            && popoverOpened
            && popoverA11yCurrent
            && popoverLifecycleStatus.includes(
              'Popover lifecycle: opened client=true'));
      marker.confirmOpenedObserved =
        marker.confirmOpenedObserved === true
          || (dialogOpened
            && popoverOpened
            && confirmOpened
            && confirmRequestStatus.includes('confirm request: 1')
            && confirmLifecycleStatus.includes(
              'ConfirmDialog lifecycle: opened client=false'));
      marker.threeLevelStackOpenedObserved =
        marker.threeLevelStackOpenedObserved === true
          || (marker.dialogOpenedObserved === true
            && marker.popoverOpenedObserved === true
            && marker.confirmOpenedObserved === true);
      marker.a11yObserved =
        marker.a11yObserved === true
          || (dialogA11yCurrent
            && popoverA11yCurrent
            && confirmA11yCurrent);
      marker.confirmInitialFocusObserved =
        marker.confirmInitialFocusObserved === true
          || confirmInitialFocusCurrent;
      marker.parentLayersRemainObserved =
        marker.parentLayersRemainObserved === true
          || (marker.topEscapeDispatched === true
            && !confirmOpened
            && popoverOpened
            && dialogOpened);
      marker.confirmFocusRestoredObserved =
        marker.confirmFocusRestoredObserved === true
          || (marker.topEscapeDispatched === true
            && !confirmOpened
            && !!confirmTrigger
            && activePath.includes(confirmTrigger));
      const topEscapeObserved =
        marker.topEscapeDispatched === true
          && !confirmOpened
          && confirmLifecycleStatus.includes(
            'ConfirmDialog lifecycle: closed client=true')
          && confirmClosedStatus.includes(
            'ConfirmDialog closed event: client=true');
      marker.dialogRemainsObserved =
        marker.dialogRemainsObserved === true
          || (marker.popoverOutsideDispatched === true
            && !popoverOpened
            && dialogOpened);
      marker.outsideFocusPreservedObserved =
        marker.outsideFocusPreservedObserved === true
          || (marker.popoverOutsideDispatched === true
            && !popoverOpened
            && !!outside
            && activePath.includes(outside));
      const popoverOutsideCloseObserved =
        marker.popoverOutsideDispatched === true
          && !popoverOpened
          && popoverLifecycleStatus.includes(
            'Popover lifecycle: closed client=true');
      marker.layeredFocusRestoredObserved =
        marker.layeredFocusRestoredObserved === true
          || (marker.dialogEscapeDispatched === true
            && !dialogOpened
            && !!dialogTrigger
            && activePath.includes(dialogTrigger));
      const dialogEscapeObserved =
        marker.dialogEscapeDispatched === true
          && !dialogOpened
          && dialogLifecycleStatus.includes(
            'Dialog lifecycle: closed client=true');
      const clients = window.Vaadin?.Flow?.clients
        ? Object.keys(window.Vaadin.Flow.clients)
          .filter((name) => name !== 'TypeScript')
        : [];
      return JSON.stringify({
        rootRendered:
          !!document.querySelector('#framework-root')
            || !!document.querySelector(
              '#framework-flow-overlay-stack-route'),
        firstMeaningfulPaint:
          diagnostics.standardBundleFirstMeaningfulPaintReported === true,
        requestCount:
          Number(diagnostics.standardBundleOpaqueRequestCount || 0),
        responseCount:
          Number(diagnostics.standardBundleOpaqueResponseCount || 0),
        pushMessageCount:
          Number(diagnostics.standardBundlePushMessageCount || 0),
        flowClientCount: clients.length,
        dedicatedRouteReady:
          window.location.pathname.endsWith('/flow-overlay-stack')
            && !!document.querySelector(
              '#framework-flow-overlay-stack-route'),
        path: window.location.pathname,
        dialogOpenedObserved: marker.dialogOpenedObserved === true,
        popoverOpenedObserved: marker.popoverOpenedObserved === true,
        confirmOpenedObserved: marker.confirmOpenedObserved === true,
        threeLevelStackOpenedObserved:
          marker.threeLevelStackOpenedObserved === true,
        a11yObserved: marker.a11yObserved === true,
        confirmInitialFocusObserved:
          marker.confirmInitialFocusObserved === true,
        nativeKeyboardObserved:
          marker.nativeKeyboardObserved === true,
        nativeKeyboardTrustedObserved:
          marker.nativeKeyboardTrustedObserved === true,
        nativeKeyboardEventCount:
          Number(marker.nativeKeyboardEventCount || 0),
        nativeKeyboardTrustedEventCount:
          Number(marker.nativeKeyboardTrustedEventCount || 0),
        nativeKeyboardStages:
          String(marker.nativeKeyboardStages || ''),
        nativePointerObserved:
          marker.nativePointerObserved === true,
        nativePointerTrustedObserved:
          marker.nativePointerTrustedObserved === true,
        nativePointerEventCount:
          Number(marker.nativePointerEventCount || 0),
        nativePointerTrustedEventCount:
          Number(marker.nativePointerTrustedEventCount || 0),
        nativePointerStages:
          String(marker.nativePointerStages || ''),
        topEscapeObserved,
        parentLayersRemainObserved:
          marker.parentLayersRemainObserved === true,
        confirmFocusRestoredObserved:
          marker.confirmFocusRestoredObserved === true,
        popoverOutsideCloseObserved,
        dialogRemainsObserved: marker.dialogRemainsObserved === true,
        outsideFocusPreservedObserved:
          marker.outsideFocusPreservedObserved === true,
        dialogEscapeObserved,
        layeredFocusRestoredObserved:
          marker.layeredFocusRestoredObserved === true,
        dialogOpenedCurrent: dialogOpened,
        dialogInitialFocusCurrent,
        dialogA11yCurrent,
        confirmA11yCurrent,
        confirmInitialFocusCurrent,
        dialogHostRole:
          String(dialog?.getAttribute?.('role') || ''),
        dialogOverlayRole:
          String(dialogOverlay?.getAttribute?.('role') || ''),
        dialogRoleAriaModal:
          String(dialogRoleElement?.getAttribute?.('aria-modal') || ''),
        dialogOverlayAriaModal:
          String(dialogOverlay?.getAttribute?.('aria-modal') || ''),
        dialogOverlayText:
          dialogOverlay ? collectDeepText(dialogOverlay) : '',
        confirmHostRole:
          String(confirm?.getAttribute?.('role') || ''),
        confirmRoleAriaModal:
          String(confirmRoleElement?.getAttribute?.('aria-modal') || ''),
        confirmAccessibleText,
        confirmActionText:
          confirmActionButton ? collectDeepText(confirmActionButton) : '',
        dialogLifecycleStatus,
        popoverLifecycleStatus,
        confirmLifecycleStatus,
        confirmClosedStatus,
        dialogRequestStatus,
        confirmRequestStatus,
        activePath: activePath.map((element) =>
          element.id || element.localName || element.tagName || '').join('>'),
        markerError: String(marker.error || ''),
        browserError: diagnostics.standardBundleBrowserError || '',
        requestError: diagnostics.standardBundleOpaqueRequestError || '',
        responseError: diagnostics.standardBundleOpaqueResponseError || '',
        pushError: diagnostics.standardBundlePushError || ''
      });
    })()
    """
}
