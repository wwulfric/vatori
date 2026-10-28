import AppKit
import Foundation

extension FlowIpcAcceptanceRuntimeObserver {
    func scheduleLazyMultiSelectComboBoxAcceptance(
        host: WebViewHost,
        context: FlowIpcWindowContext,
        timeoutSeconds: Double
    ) {
        FlowIpcLazyMultiSelectComboBoxAcceptance(
            context: context,
            deadline: Date().addingTimeInterval(timeoutSeconds)
        ).start()
    }
}

private final class FlowIpcLazyMultiSelectComboBoxAcceptance {
    private enum Phase {
        case ready
        case dedicatedRoute
        case popupOpened
        case farPage
        case zQuery
        case zedFocused
        case selected
        case firstEscape
        case popupClosed
        case repeatZQuery
        case repeatZClosed
        case xQuery
        case finalClosed
        case drain
    }

    private let context: FlowIpcWindowContext
    private let deadline: Date
    private var phase = Phase.ready
    private var popupOpenedObserved = false
    private var farPageObserved = false
    private var zQueryObserved = false
    private var zedFocusedObserved = false
    private var clientSelectionObserved = false
    private var firstEscapeObserved = false
    private var popupClosedObserved = false
    private var repeatZQueryObserved = false
    private var repeatZClosedObserved = false
    private var xQueryObserved = false
    private var finalClosedObserved = false
    private var farPageUidlRoundTripCount = 0
    private var zQueryUidlRoundTripCount = 0
    private var selectionUidlRoundTripCount = 0
    private var repeatZQueryUidlRoundTripCount = 0
    private var xQueryUidlRoundTripCount = 0
    private var repeatZFetchBaseline = -1
    private var requestBaseline = 0
    private var responseBaseline = 0
    private var drainStablePollCount = 0
    private var drainRequestCount: Int?
    private var drainResponseCount: Int?

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
                self.retryOrFail(
                    code: "lazyMultiSelectComboBoxInspectionFailed")
                return
            }
            guard snapshot.errorFree else {
                self.fail(
                    code: "lazyMultiSelectComboBoxBrowserRuntimeError",
                    details: snapshot.tracePayload)
                return
            }

            switch self.phase {
            case .ready:
                guard snapshot.ready else {
                    self.retryOrFail(
                        code: "lazyMultiSelectComboBoxReadyTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.perform(
                    Self.navigationAction,
                    next: .dedicatedRoute,
                    failureCode: "lazyMultiSelectComboBoxNavigationFailed")
            case .dedicatedRoute:
                guard snapshot.dedicatedRouteReady else {
                    self.retryOrFail(
                        code: "lazyMultiSelectComboBoxDedicatedRouteTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativePrimaryClick(
                    Self.prepareOwnerPointerAction,
                    stage: "open-lazy-owner",
                    next: .popupOpened,
                    failureCode: "lazyMultiSelectComboBoxOwnerClickFailed")
            case .popupOpened:
                guard snapshot.popupOpenedCurrent,
                      snapshot.popupA11yCurrent,
                      snapshot.ownerFocusCurrent,
                      snapshot.initialQueryObserved,
                      snapshot.nativePointerEventCount == 1,
                      snapshot.nativePointerTrustedEventCount == 1 else {
                    self.retryOrFail(
                        code: "lazyMultiSelectComboBoxPopupOpenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.popupOpenedObserved = true
                self.captureRoundTripBaseline(snapshot)
                self.perform(
                    Self.scrollFarPageAction,
                    next: .farPage,
                    failureCode: "lazyMultiSelectComboBoxFarPageScrollFailed")
            case .farPage:
                guard snapshot.farPageQueryObserved,
                      snapshot.farPageFreshR060Observed,
                      snapshot.farPageR060AriaSelectedObserved,
                      self.roundTripObserved(snapshot) else {
                    self.retryOrFail(
                        code: "lazyMultiSelectComboBoxFarPageTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                if !self.farPageObserved {
                    self.farPageObserved = true
                    self.farPageUidlRoundTripCount += 1
                }
                self.captureRoundTripBaseline(snapshot)
                self.performNativeKey(
                    .letterZ,
                    Self.typeZAction,
                    expectedInputValue: "z",
                    inputPrefix: "z",
                    next: .zQuery,
                    failureCode: "lazyMultiSelectComboBoxZInputFailed")
            case .zQuery:
                guard snapshot.zKeyDownTrustedObserved,
                      snapshot.zInputTrustedObserved,
                      snapshot.zKeyDownBeforeInputObserved,
                      snapshot.zServerQueryObserved,
                      snapshot.zSingleResultObserved,
                      self.roundTripObserved(snapshot) else {
                    self.retryOrFail(
                        code: "lazyMultiSelectComboBoxZQueryTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                if !self.zQueryObserved {
                    self.zQueryObserved = true
                    self.zQueryUidlRoundTripCount += 1
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.focusZedAction,
                    next: .zedFocused,
                    failureCode: "lazyMultiSelectComboBoxZedFocusFailed")
            case .zedFocused:
                guard snapshot.zedOptionFocusedObserved else {
                    self.retryOrFail(
                        code: "lazyMultiSelectComboBoxZedFocusTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.zedFocusedObserved = true
                self.captureRoundTripBaseline(snapshot)
                self.performNativeKey(
                    .returnKey,
                    Self.selectZedAction,
                    next: .selected,
                    failureCode: "lazyMultiSelectComboBoxZedSelectionFailed")
            case .selected:
                guard snapshot.clientSelectionObserved,
                      snapshot.selectionPreservedObserved,
                      self.roundTripObserved(snapshot) else {
                    self.retryOrFail(
                        code: "lazyMultiSelectComboBoxSelectionTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                if !self.clientSelectionObserved {
                    self.clientSelectionObserved = true
                    self.selectionUidlRoundTripCount += 1
                }
                self.performNativeKey(
                    .escape,
                    Self.clearZedFocusAction,
                    next: .firstEscape,
                    failureCode:
                        "lazyMultiSelectComboBoxFirstEscapeFailed")
            case .firstEscape:
                guard snapshot.firstEscapeClearedOptionObserved,
                      snapshot.popupRetainedAfterFirstEscapeObserved else {
                    self.retryOrFail(
                        code: "lazyMultiSelectComboBoxFirstEscapeTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.firstEscapeObserved = true
                self.performNativeKey(
                    .escape,
                    Self.closeZedPopupAction,
                    next: .popupClosed,
                    failureCode: "lazyMultiSelectComboBoxCloseZedFailed")
            case .popupClosed:
                guard snapshot.popupEscapeClosedObserved,
                      snapshot.popupFocusRestoredObserved,
                      snapshot.zFetchCount > 0 else {
                    self.retryOrFail(
                        code: "lazyMultiSelectComboBoxClosedAfterZedTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.popupClosedObserved = true
                self.repeatZFetchBaseline = snapshot.zFetchCount
                self.captureRoundTripBaseline(snapshot)
                self.performNativeKey(
                    .letterZ,
                    Self.typeRepeatZAction,
                    expectedInputValue: "z",
                    inputPrefix: "repeatZ",
                    next: .repeatZQuery,
                    failureCode:
                        "lazyMultiSelectComboBoxRepeatZInputFailed")
            case .repeatZQuery:
                guard snapshot.repeatZKeyDownTrustedObserved,
                      snapshot.repeatZInputTrustedObserved,
                      snapshot.repeatZKeyDownBeforeInputObserved,
                      snapshot.repeatZServerQueryObserved,
                      snapshot.zFetchCount > self.repeatZFetchBaseline,
                      snapshot.repeatZSingleSelectedResultObserved,
                      snapshot.repeatZFreshR061AriaSelectedObserved,
                      snapshot.repeatZSelectionRetainedObserved,
                      snapshot.javaSelectionChangeCount == 1,
                      self.roundTripObserved(snapshot) else {
                    self.retryOrFail(
                        code: "lazyMultiSelectComboBoxRepeatZQueryTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                if !self.repeatZQueryObserved {
                    self.repeatZQueryObserved = true
                    self.repeatZQueryUidlRoundTripCount += 1
                }
                self.performNativeKey(
                    .escape,
                    Self.closeRepeatZPopupAction,
                    next: .repeatZClosed,
                    failureCode:
                        "lazyMultiSelectComboBoxCloseRepeatZFailed")
            case .repeatZClosed:
                guard snapshot.repeatZPopupClosedObserved,
                      snapshot.repeatZFocusRestoredObserved,
                      snapshot.repeatZFilterClearedObserved,
                      snapshot.javaSelectionChangeCount == 1 else {
                    self.retryOrFail(
                        code: "lazyMultiSelectComboBoxClosedAfterRepeatZTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.repeatZClosedObserved = true
                self.captureRoundTripBaseline(snapshot)
                self.performNativeKey(
                    .letterX,
                    Self.typeXAction,
                    expectedInputValue: "x",
                    inputPrefix: "x",
                    next: .xQuery,
                    failureCode: "lazyMultiSelectComboBoxXInputFailed")
            case .xQuery:
                guard snapshot.xKeyDownTrustedObserved,
                      snapshot.xInputTrustedObserved,
                      snapshot.xKeyDownBeforeInputObserved,
                      snapshot.xServerQueryObserved,
                      snapshot.xSingleResultObserved,
                      snapshot.xFreshR060AriaSelectedObserved,
                      snapshot.xSelectionRetainedObserved,
                      self.roundTripObserved(snapshot) else {
                    self.retryOrFail(
                        code: "lazyMultiSelectComboBoxXQueryTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                if !self.xQueryObserved {
                    self.xQueryObserved = true
                    self.xQueryUidlRoundTripCount += 1
                }
                self.performNativeKey(
                    .escape,
                    Self.closeXPopupAction,
                    next: .finalClosed,
                    failureCode: "lazyMultiSelectComboBoxCloseXFailed")
            case .finalClosed:
                guard snapshot.finalPopupClosedObserved,
                      snapshot.finalFocusRestoredObserved,
                      snapshot.finalFilterClearedObserved else {
                    self.retryOrFail(
                        code: "lazyMultiSelectComboBoxFinalCloseTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.finalClosedObserved = true
                self.phase = .drain
                self.retryOrFail(
                    code: "lazyMultiSelectComboBoxFinishDrainTimeout",
                    details: snapshot.tracePayload)
            case .drain:
                guard snapshot.requestCount == snapshot.responseCount else {
                    self.drainStablePollCount = 0
                    self.drainRequestCount = nil
                    self.drainResponseCount = nil
                    self.retryOrFail(
                        code: "lazyMultiSelectComboBoxFinishDrainTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                if self.drainRequestCount == snapshot.requestCount,
                   self.drainResponseCount == snapshot.responseCount {
                    self.drainStablePollCount += 1
                } else {
                    self.drainRequestCount = snapshot.requestCount
                    self.drainResponseCount = snapshot.responseCount
                    self.drainStablePollCount = 1
                }
                guard self.drainStablePollCount >= 2 else {
                    self.retryOrFail(
                        code: "lazyMultiSelectComboBoxFinishDrainTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.finish(snapshot)
            }
        }
    }

    private func inspect(_ completion: @escaping (Snapshot?) -> Void) {
        context.webView.evaluateJavaScript(Self.inspectionScript) {
            result, error in
            guard error == nil,
                  let json = result as? String,
                  let data = json.data(using: .utf8),
                  let snapshot = try? JSONDecoder().decode(
                    Snapshot.self, from: data) else {
                completion(nil)
                return
            }
            completion(snapshot)
        }
    }

    private func perform(
        _ script: String,
        next: Phase,
        failureCode: String
    ) {
        context.webView.evaluateJavaScript(script) { result, error in
            guard error == nil, result as? Bool == true else {
                self.retryOrFail(code: failureCode, details: [
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

    private func performNativeKey(
        _ key: NativeKey,
        _ preparationScript: String,
        expectedInputValue: String? = nil,
        inputPrefix: String = "",
        next: Phase,
        failureCode: String
    ) {
        let observesInput = expectedInputValue != nil
        let expectedInputValue = expectedInputValue ?? ""
        let instrumentedScript = """
        (() => {
          const prepared = \(preparationScript);
          if (prepared !== true) return false;
          const marker = window.VaadinDesktopFlowLazyMultiSelectComboBox || {};
          window.VaadinDesktopFlowLazyMultiSelectComboBox = marker;
          const combo = document.querySelector(
            '#framework-flow-lazy-multi-select-combo-box-field');
          const input = combo?.inputElement
            || combo?.shadowRoot?.querySelector('input');
          if (!combo || !input) return false;
          if (typeof marker.pendingKeyboardHandler === 'function') {
            document.removeEventListener(
              'keydown', marker.pendingKeyboardHandler, true);
          }
          if (typeof marker.pendingInputHandler === 'function') {
            document.removeEventListener(
              'input', marker.pendingInputHandler, true);
          }
          const stage = String(marker.nativeKeyboardStage || '');
          const observesInput = \(observesInput ? "true" : "false");
          const prefix = '\(inputPrefix)';
          const expectedInputValue = '\(expectedInputValue)';
          if (observesInput) {
            marker[prefix + 'EventOrder'] = [];
            const inputHandler = (event) => {
              const path = event.composedPath?.() || [];
              const value = String(input.value || '');
              if (!path.includes(combo) || !path.includes(input)
                  || value !== expectedInputValue) return;
              document.removeEventListener('input', inputHandler, true);
              marker.pendingInputHandler = null;
              marker[prefix + 'EventOrder'].push('input');
              marker[prefix + 'InputEventCount'] =
                Number(marker[prefix + 'InputEventCount'] || 0) + 1;
              marker[prefix + 'TrustedInputEventCount'] =
                Number(marker[prefix + 'TrustedInputEventCount'] || 0)
                  + (event.isTrusted === true ? 1 : 0);
              marker[prefix + 'InputTrustedObserved'] =
                Number(marker[prefix + 'InputEventCount'] || 0)
                  === Number(marker[prefix + 'TrustedInputEventCount'] || 0);
              marker[prefix + 'KeyDownBeforeInputObserved'] =
                marker[prefix + 'EventOrder'].join(',') === 'keydown,input';
              marker.nativeTextInputObserved = true;
              marker.nativeTextInputEventCount =
                Number(marker.nativeTextInputEventCount || 0) + 1;
              marker.nativeTextInputTrustedEventCount =
                Number(marker.nativeTextInputTrustedEventCount || 0)
                  + (event.isTrusted === true ? 1 : 0);
              marker.nativeTextInputTrustedObserved =
                marker.nativeTextInputEventCount
                  === marker.nativeTextInputTrustedEventCount;
              marker.nativeTextInputStages =
                Array.isArray(marker.nativeTextInputStages)
                  ? marker.nativeTextInputStages : [];
              marker.nativeTextInputStages.push(stage);
              marker.nativeTextInputValues =
                Array.isArray(marker.nativeTextInputValues)
                  ? marker.nativeTextInputValues : [];
              marker.nativeTextInputValues.push(value);
            };
            marker.pendingInputHandler = inputHandler;
            document.addEventListener('input', inputHandler, true);
          }
          const handler = (event) => {
            const path = event.composedPath?.() || [];
            if (String(event.key || '') !== '\(key.name)'
                || !path.includes(combo) || !path.includes(input)) return;
            document.removeEventListener('keydown', handler, true);
            marker.pendingKeyboardHandler = null;
            marker.nativeKeyboardObserved = true;
            marker.nativeKeyboardEventCount =
              Number(marker.nativeKeyboardEventCount || 0) + 1;
            marker.nativeKeyboardTrustedEventCount =
              Number(marker.nativeKeyboardTrustedEventCount || 0)
                + (event.isTrusted === true ? 1 : 0);
            marker.nativeKeyboardTrustedObserved =
              marker.nativeKeyboardEventCount
                === marker.nativeKeyboardTrustedEventCount;
            marker.nativeKeyboardStages =
              Array.isArray(marker.nativeKeyboardStages)
                ? marker.nativeKeyboardStages : [];
            marker.nativeKeyboardStages.push(stage);
            marker.nativeKeyboardKeys =
              Array.isArray(marker.nativeKeyboardKeys)
                ? marker.nativeKeyboardKeys : [];
            marker.nativeKeyboardKeys.push(String(event.key || ''));
            if (observesInput) {
              marker[prefix + 'EventOrder'].push('keydown');
              marker[prefix + 'KeyDownTrustedObserved'] =
                event.isTrusted === true;
            }
          };
          marker.pendingKeyboardHandler = handler;
          document.addEventListener('keydown', handler, true);
          return true;
        })()
        """
        context.webView.evaluateJavaScript(instrumentedScript) {
            result, error in
            guard error == nil, result as? Bool == true else {
                self.retryOrFail(code: failureCode, details: [
                    "message": error?.localizedDescription ?? "",
                    "result": String(describing: result)
                ])
                return
            }
            self.prepareWindowForNativeInput(failureCode: failureCode) {
                let timestamp = ProcessInfo.processInfo.systemUptime
                guard let keyDown = key.event(
                          type: .keyDown,
                          windowNumber: self.context.window.windowNumber,
                          timestamp: timestamp),
                      let keyUp = key.event(
                          type: .keyUp,
                          windowNumber: self.context.window.windowNumber,
                          timestamp: timestamp + 0.01) else {
                    self.retryOrFail(code: failureCode, details: [
                        "message": "Unable to create AppKit keyboard event",
                        "key": key.name
                    ])
                    return
                }
                trace(
                    "recv",
                    "lazyMultiSelectComboBox.nativeKeyboardDispatch",
                    [
                        "inputMode": "appkit-window-key-event",
                        "key": key.name,
                        "activeApplication": NSApplication.shared.isActive,
                        "keyWindow": self.context.window.isKeyWindow,
                        "windowNumber": self.context.window.windowNumber
                    ])
                self.context.window.sendEvent(keyDown)
                self.context.window.sendEvent(keyUp)
                self.phase = next
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.55) {
                    self.poll()
                }
            }
        }
    }

    private func performNativePrimaryClick(
        _ preparationScript: String,
        stage: String,
        next: Phase,
        failureCode: String
    ) {
        context.webView.evaluateJavaScript(preparationScript) {
            result, error in
            guard error == nil,
                  let json = result as? String,
                  let data = json.data(using: .utf8),
                  let geometry = try? JSONDecoder().decode(
                    PointerGeometry.self, from: data),
                  geometry.targetMatched else {
                self.retryOrFail(code: failureCode, details: [
                    "message": error?.localizedDescription ?? "",
                    "result": String(describing: result)
                ])
                return
            }
            self.prepareWindowForNativeInput(failureCode: failureCode) {
                let webViewPoint = NSPoint(
                    x: geometry.centerX,
                    y: self.context.webView.isFlipped
                        ? geometry.centerY
                        : self.context.webView.bounds.height
                            - geometry.centerY)
                let windowPoint = self.context.webView.convert(
                    webViewPoint, to: nil)
                guard self.context.window.contentLayoutRect.contains(
                        windowPoint) else {
                    self.retryOrFail(code: failureCode, details: [
                        "message": "Lazy multi-select target outside window",
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
                          windowNumber: self.context.window.windowNumber,
                          context: nil,
                          eventNumber: 0,
                          clickCount: 1,
                          pressure: 1),
                      let mouseUp = NSEvent.mouseEvent(
                          with: .leftMouseUp,
                          location: windowPoint,
                          modifierFlags: [],
                          timestamp: timestamp + 0.05,
                          windowNumber: self.context.window.windowNumber,
                          context: nil,
                          eventNumber: 1,
                          clickCount: 1,
                          pressure: 0) else {
                    self.retryOrFail(code: failureCode, details: [
                        "message": "Unable to create AppKit pointer event"
                    ])
                    return
                }
                trace(
                    "recv",
                    "lazyMultiSelectComboBox.nativePointerDispatch",
                    [
                        "inputMode": "appkit-window-pointer-event",
                        "stage": stage,
                        "webViewX": geometry.centerX,
                        "webViewY": geometry.centerY,
                        "windowX": windowPoint.x,
                        "windowY": windowPoint.y,
                        "activeApplication": NSApplication.shared.isActive,
                        "keyWindow": self.context.window.isKeyWindow,
                        "windowNumber": self.context.window.windowNumber
                    ])
                self.context.window.sendEvent(mouseDown)
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
                    self.context.window.sendEvent(mouseUp)
                    self.phase = next
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.65) {
                        self.poll()
                    }
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
                "message": "Timed out waiting for native input activation",
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
        let webViewOwnsResponder = responderView === context.webView
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

    private func captureRoundTripBaseline(_ snapshot: Snapshot) {
        requestBaseline = snapshot.requestCount
        responseBaseline = snapshot.responseCount
    }

    private func roundTripObserved(_ snapshot: Snapshot) -> Bool {
        snapshot.requestCount > requestBaseline
            && snapshot.responseCount > responseBaseline
    }

    private func retryOrFail(
        code: String,
        details: [String: Any] = [:]
    ) {
        guard Date() < deadline else {
            fail(code: code, details: details)
            return
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            self.poll()
        }
    }

    private func finish(_ snapshot: Snapshot) {
        let nativePointerContract = snapshot.nativePointerObserved
            && snapshot.nativePointerTrustedObserved
            && snapshot.nativePointerEventCount == 1
            && snapshot.nativePointerTrustedEventCount == 1
            && snapshot.nativePointerStages == "open-lazy-owner"
        let nativeKeyboardContract = snapshot.nativeKeyboardObserved
            && snapshot.nativeKeyboardTrustedObserved
            && snapshot.nativeKeyboardEventCount == 9
            && snapshot.nativeKeyboardTrustedEventCount == 9
            && snapshot.nativeKeyboardStages
                == "type-lazy-z,focus-lazy-zed,select-lazy-zed,"
                    + "escape-clear-zed-focus,escape-close-zed,"
                    + "type-lazy-repeat-z,escape-close-repeat-z,"
                    + "type-lazy-x,escape-close-x"
            && snapshot.nativeKeyboardKeys
                == "z,ArrowDown,Enter,Escape,Escape,z,Escape,x,Escape"
        let nativeTextInputContract = snapshot.nativeTextInputObserved
            && snapshot.nativeTextInputTrustedObserved
            && snapshot.nativeTextInputEventCount == 3
            && snapshot.nativeTextInputTrustedEventCount == 3
            && snapshot.nativeTextInputStages
                == "type-lazy-z,type-lazy-repeat-z,type-lazy-x"
            && snapshot.nativeTextInputValues == "z,z,x"
        let browserSetupContract = snapshot.browserProgrammaticSetupObserved
            && snapshot.browserProgrammaticStages
                == "navigate,position-lazy-owner,scroll-lazy-r060-page"
        let complete = snapshot.errorFree
            && popupOpenedObserved
            && farPageObserved
            && zQueryObserved
            && zedFocusedObserved
            && clientSelectionObserved
            && firstEscapeObserved
            && popupClosedObserved
            && repeatZQueryObserved
            && repeatZClosedObserved
            && xQueryObserved
            && finalClosedObserved
            && farPageUidlRoundTripCount == 1
            && zQueryUidlRoundTripCount == 1
            && selectionUidlRoundTripCount == 1
            && repeatZQueryUidlRoundTripCount == 1
            && xQueryUidlRoundTripCount == 1
            && repeatZFetchBaseline > 0
            && snapshot.zFetchCount > repeatZFetchBaseline
            && snapshot.javaSelectionChangeCount == 1
            && nativePointerContract
            && nativeKeyboardContract
            && nativeTextInputContract
            && browserSetupContract

        var payload = snapshot.tracePayload
        payload.merge([
            "complete": complete,
            "standardBundleMode": snapshot.firstMeaningfulPaint,
            "rootRendered": snapshot.routeRendered,
            "flowClientOwned": snapshot.flowClientCount > 0,
            "opaqueRequestBridgeObserved": snapshot.requestCount >= 2,
            "opaqueResponseBridgeObserved": snapshot.responseCount >= 2,
            "pushObserved": snapshot.pushMessageCount >= 1,
            "errorFree": snapshot.errorFree,
            "lazyMultiSelectComboBoxDedicatedRouteObserved":
                snapshot.dedicatedRouteObserved,
            "lazyMultiSelectComboBoxInitialServerValueObserved":
                snapshot.initialServerValueObserved,
            "lazyMultiSelectComboBoxInitialQueryObserved":
                snapshot.initialQueryObserved,
            "lazyMultiSelectComboBoxFarPageQueryObserved":
                snapshot.farPageQueryObserved,
            "lazyMultiSelectComboBoxFarPageFreshR060Observed":
                snapshot.farPageFreshR060Observed,
            "lazyMultiSelectComboBoxFarPageR060AriaSelectedObserved":
                snapshot.farPageR060AriaSelectedObserved,
            "lazyMultiSelectComboBoxZKeyDownTrustedObserved":
                snapshot.zKeyDownTrustedObserved,
            "lazyMultiSelectComboBoxZInputTrustedObserved":
                snapshot.zInputTrustedObserved,
            "lazyMultiSelectComboBoxZKeyDownBeforeInputObserved":
                snapshot.zKeyDownBeforeInputObserved,
            "lazyMultiSelectComboBoxZServerQueryObserved":
                snapshot.zServerQueryObserved,
            "lazyMultiSelectComboBoxZSingleResultObserved":
                snapshot.zSingleResultObserved,
            "lazyMultiSelectComboBoxZedOptionFocusedObserved":
                snapshot.zedOptionFocusedObserved,
            "lazyMultiSelectComboBoxClientSelectionObserved":
                snapshot.clientSelectionObserved,
            "lazyMultiSelectComboBoxSelectionPreservedObserved":
                snapshot.selectionPreservedObserved,
            "lazyMultiSelectComboBoxFirstEscapeClearedOptionObserved":
                snapshot.firstEscapeClearedOptionObserved,
            "lazyMultiSelectComboBoxPopupRetainedAfterFirstEscapeObserved":
                snapshot.popupRetainedAfterFirstEscapeObserved,
            "lazyMultiSelectComboBoxPopupEscapeClosedObserved":
                snapshot.popupEscapeClosedObserved,
            "lazyMultiSelectComboBoxPopupFocusRestoredObserved":
                snapshot.popupFocusRestoredObserved,
            "lazyMultiSelectComboBoxRepeatZKeyDownTrustedObserved":
                snapshot.repeatZKeyDownTrustedObserved,
            "lazyMultiSelectComboBoxRepeatZInputTrustedObserved":
                snapshot.repeatZInputTrustedObserved,
            "lazyMultiSelectComboBoxRepeatZKeyDownBeforeInputObserved":
                snapshot.repeatZKeyDownBeforeInputObserved,
            "lazyMultiSelectComboBoxRepeatZServerQueryObserved":
                snapshot.repeatZServerQueryObserved,
            "lazyMultiSelectComboBoxRepeatZFetchAdvancedObserved":
                snapshot.zFetchCount > repeatZFetchBaseline,
            "lazyMultiSelectComboBoxRepeatZSingleSelectedResultObserved":
                snapshot.repeatZSingleSelectedResultObserved,
            "lazyMultiSelectComboBoxRepeatZFreshR061AriaSelectedObserved":
                snapshot.repeatZFreshR061AriaSelectedObserved,
            "lazyMultiSelectComboBoxRepeatZSelectionRetainedObserved":
                snapshot.repeatZSelectionRetainedObserved,
            "lazyMultiSelectComboBoxRepeatZPopupClosedObserved":
                snapshot.repeatZPopupClosedObserved,
            "lazyMultiSelectComboBoxRepeatZFocusRestoredObserved":
                snapshot.repeatZFocusRestoredObserved,
            "lazyMultiSelectComboBoxRepeatZFilterClearedObserved":
                snapshot.repeatZFilterClearedObserved,
            "lazyMultiSelectComboBoxRepeatZNoExtraJavaSelectionObserved":
                snapshot.javaSelectionChangeCount == 1,
            "lazyMultiSelectComboBoxXKeyDownTrustedObserved":
                snapshot.xKeyDownTrustedObserved,
            "lazyMultiSelectComboBoxXInputTrustedObserved":
                snapshot.xInputTrustedObserved,
            "lazyMultiSelectComboBoxXKeyDownBeforeInputObserved":
                snapshot.xKeyDownBeforeInputObserved,
            "lazyMultiSelectComboBoxXServerQueryObserved":
                snapshot.xServerQueryObserved,
            "lazyMultiSelectComboBoxXSingleResultObserved":
                snapshot.xSingleResultObserved,
            "lazyMultiSelectComboBoxXFreshR060AriaSelectedObserved":
                snapshot.xFreshR060AriaSelectedObserved,
            "lazyMultiSelectComboBoxXSelectionRetainedObserved":
                snapshot.xSelectionRetainedObserved,
            "lazyMultiSelectComboBoxFinalPopupClosedObserved":
                snapshot.finalPopupClosedObserved,
            "lazyMultiSelectComboBoxFinalFocusRestoredObserved":
                snapshot.finalFocusRestoredObserved,
            "lazyMultiSelectComboBoxFinalFilterClearedObserved":
                snapshot.finalFilterClearedObserved,
            "lazyMultiSelectComboBoxFarPageUidlRoundTripsObserved":
                farPageUidlRoundTripCount == 1,
            "lazyMultiSelectComboBoxFarPageUidlRoundTripCount":
                farPageUidlRoundTripCount,
            "lazyMultiSelectComboBoxZQueryUidlRoundTripsObserved":
                zQueryUidlRoundTripCount == 1,
            "lazyMultiSelectComboBoxZQueryUidlRoundTripCount":
                zQueryUidlRoundTripCount,
            "lazyMultiSelectComboBoxSelectionUidlRoundTripsObserved":
                selectionUidlRoundTripCount == 1,
            "lazyMultiSelectComboBoxSelectionUidlRoundTripCount":
                selectionUidlRoundTripCount,
            "lazyMultiSelectComboBoxRepeatZQueryUidlRoundTripsObserved":
                repeatZQueryUidlRoundTripCount == 1,
            "lazyMultiSelectComboBoxRepeatZQueryUidlRoundTripCount":
                repeatZQueryUidlRoundTripCount,
            "lazyMultiSelectComboBoxRepeatZFetchBaseline":
                repeatZFetchBaseline,
            "lazyMultiSelectComboBoxRepeatZFetchCount":
                snapshot.zFetchCount,
            "lazyMultiSelectComboBoxXQueryUidlRoundTripsObserved":
                xQueryUidlRoundTripCount == 1,
            "lazyMultiSelectComboBoxXQueryUidlRoundTripCount":
                xQueryUidlRoundTripCount,
            "lazyMultiSelectComboBoxJavaSelectionChangeCount":
                snapshot.javaSelectionChangeCount
        ]) { _, replacement in replacement }
        trace("recv", "lazyMultiSelectComboBox.frameworkAcceptance", payload)
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
        let targetMatched: Bool
    }

    private struct Snapshot: Decodable {
        let rootRouteAvailable: Bool
        let routeRendered: Bool
        let firstMeaningfulPaint: Bool
        let requestCount: Int
        let responseCount: Int
        let pushMessageCount: Int
        let flowClientCount: Int
        let dedicatedRouteObserved: Bool
        let initialServerValueObserved: Bool
        let initialQueryObserved: Bool
        let popupOpenedCurrent: Bool
        let popupA11yCurrent: Bool
        let ownerFocusCurrent: Bool
        let farPageQueryObserved: Bool
        let farPageFreshR060Observed: Bool
        let farPageR060AriaSelectedObserved: Bool
        let zKeyDownTrustedObserved: Bool
        let zInputTrustedObserved: Bool
        let zKeyDownBeforeInputObserved: Bool
        let zServerQueryObserved: Bool
        let zSingleResultObserved: Bool
        let zedOptionFocusedObserved: Bool
        let clientSelectionObserved: Bool
        let selectionPreservedObserved: Bool
        let firstEscapeClearedOptionObserved: Bool
        let popupRetainedAfterFirstEscapeObserved: Bool
        let popupEscapeClosedObserved: Bool
        let popupFocusRestoredObserved: Bool
        let repeatZKeyDownTrustedObserved: Bool
        let repeatZInputTrustedObserved: Bool
        let repeatZKeyDownBeforeInputObserved: Bool
        let repeatZServerQueryObserved: Bool
        let repeatZSingleSelectedResultObserved: Bool
        let repeatZFreshR061AriaSelectedObserved: Bool
        let repeatZSelectionRetainedObserved: Bool
        let repeatZPopupClosedObserved: Bool
        let repeatZFocusRestoredObserved: Bool
        let repeatZFilterClearedObserved: Bool
        let zFetchCount: Int
        let xKeyDownTrustedObserved: Bool
        let xInputTrustedObserved: Bool
        let xKeyDownBeforeInputObserved: Bool
        let xServerQueryObserved: Bool
        let xSingleResultObserved: Bool
        let xFreshR060AriaSelectedObserved: Bool
        let xSelectionRetainedObserved: Bool
        let finalPopupClosedObserved: Bool
        let finalFocusRestoredObserved: Bool
        let finalFilterClearedObserved: Bool
        let selectedItems: String
        let visibleChipLabels: String
        let inputValue: String
        let filterValue: String
        let visibleOptionLabels: String
        let initialQueryStatus: String
        let farPageStatus: String
        let zQueryStatus: String
        let repeatZQueryStatus: String
        let xQueryStatus: String
        let selectionStatus: String
        let javaSelectionChangeCount: Int
        let nativePointerObserved: Bool
        let nativePointerTrustedObserved: Bool
        let nativePointerEventCount: Int
        let nativePointerTrustedEventCount: Int
        let nativePointerStages: String
        let nativeKeyboardObserved: Bool
        let nativeKeyboardTrustedObserved: Bool
        let nativeKeyboardEventCount: Int
        let nativeKeyboardTrustedEventCount: Int
        let nativeKeyboardStages: String
        let nativeKeyboardKeys: String
        let nativeTextInputObserved: Bool
        let nativeTextInputTrustedObserved: Bool
        let nativeTextInputEventCount: Int
        let nativeTextInputTrustedEventCount: Int
        let nativeTextInputStages: String
        let nativeTextInputValues: String
        let browserProgrammaticSetupObserved: Bool
        let browserProgrammaticStages: String
        let rawKeyboardEvents: String
        let rawInputEvents: String
        let rawSelectionEvents: String
        let browserError: String
        let requestError: String
        let responseError: String
        let pushError: String

        var errorFree: Bool {
            browserError.isEmpty
                && requestError.isEmpty
                && responseError.isEmpty
                && pushError.isEmpty
        }

        var ready: Bool {
            rootRouteAvailable
                && firstMeaningfulPaint
                && requestCount >= 2
                && responseCount >= 2
                && flowClientCount > 0
                && errorFree
        }

        var dedicatedRouteReady: Bool {
            routeRendered
                && dedicatedRouteObserved
                && initialServerValueObserved
                && errorFree
        }

        var tracePayload: [String: Any] {
            [
                "rootRendered": routeRendered,
                "firstMeaningfulPaint": firstMeaningfulPaint,
                "requestCount": requestCount,
                "responseCount": responseCount,
                "pushMessageCount": pushMessageCount,
                "flowClientCount": flowClientCount,
                "lazyMultiSelectComboBoxSelectedItems": selectedItems,
                "lazyMultiSelectComboBoxVisibleChipLabels":
                    visibleChipLabels,
                "lazyMultiSelectComboBoxInputValue": inputValue,
                "lazyMultiSelectComboBoxFilterValue": filterValue,
                "lazyMultiSelectComboBoxVisibleOptionLabels":
                    visibleOptionLabels,
                "lazyMultiSelectComboBoxInitialQueryStatus":
                    initialQueryStatus,
                "lazyMultiSelectComboBoxFarPageStatus": farPageStatus,
                "lazyMultiSelectComboBoxZQueryStatus": zQueryStatus,
                "lazyMultiSelectComboBoxRepeatZQueryStatus":
                    repeatZQueryStatus,
                "lazyMultiSelectComboBoxRepeatZFetchCount": zFetchCount,
                "lazyMultiSelectComboBoxXQueryStatus": xQueryStatus,
                "lazyMultiSelectComboBoxSelectionStatus": selectionStatus,
                "lazyMultiSelectComboBoxNativePointerObserved":
                    nativePointerObserved,
                "lazyMultiSelectComboBoxNativePointerTrustedObserved":
                    nativePointerTrustedObserved,
                "lazyMultiSelectComboBoxNativePointerEventCount":
                    nativePointerEventCount,
                "lazyMultiSelectComboBoxNativePointerTrustedEventCount":
                    nativePointerTrustedEventCount,
                "lazyMultiSelectComboBoxNativePointerStages":
                    nativePointerStages,
                "lazyMultiSelectComboBoxNativeKeyboardObserved":
                    nativeKeyboardObserved,
                "lazyMultiSelectComboBoxNativeKeyboardTrustedObserved":
                    nativeKeyboardTrustedObserved,
                "lazyMultiSelectComboBoxNativeKeyboardEventCount":
                    nativeKeyboardEventCount,
                "lazyMultiSelectComboBoxNativeKeyboardTrustedEventCount":
                    nativeKeyboardTrustedEventCount,
                "lazyMultiSelectComboBoxNativeKeyboardStages":
                    nativeKeyboardStages,
                "lazyMultiSelectComboBoxNativeKeyboardKeys":
                    nativeKeyboardKeys,
                "lazyMultiSelectComboBoxNativeTextInputObserved":
                    nativeTextInputObserved,
                "lazyMultiSelectComboBoxNativeTextInputTrustedObserved":
                    nativeTextInputTrustedObserved,
                "lazyMultiSelectComboBoxNativeTextInputEventCount":
                    nativeTextInputEventCount,
                "lazyMultiSelectComboBoxNativeTextInputTrustedEventCount":
                    nativeTextInputTrustedEventCount,
                "lazyMultiSelectComboBoxNativeTextInputStages":
                    nativeTextInputStages,
                "lazyMultiSelectComboBoxNativeTextInputValues":
                    nativeTextInputValues,
                "lazyMultiSelectComboBoxBrowserProgrammaticSetupObserved":
                    browserProgrammaticSetupObserved,
                "lazyMultiSelectComboBoxBrowserProgrammaticStages":
                    browserProgrammaticStages,
                "lazyMultiSelectComboBoxRawKeyboardEvents":
                    rawKeyboardEvents,
                "lazyMultiSelectComboBoxRawInputEvents": rawInputEvents,
                "lazyMultiSelectComboBoxRawSelectionEvents":
                    rawSelectionEvents,
                "browserError": browserError,
                "requestError": requestError,
                "responseError": responseError,
                "pushError": pushError
            ]
        }
    }

    private enum NativeKey {
        case arrowDown
        case returnKey
        case escape
        case letterZ
        case letterX

        var name: String {
            switch self {
            case .arrowDown: return "ArrowDown"
            case .returnKey: return "Enter"
            case .escape: return "Escape"
            case .letterZ: return "z"
            case .letterX: return "x"
            }
        }

        private var characters: String {
            switch self {
            case .arrowDown: return "\u{f701}"
            case .returnKey: return "\r"
            case .escape: return "\u{1b}"
            case .letterZ: return "z"
            case .letterX: return "x"
            }
        }

        private var keyCode: UInt16 {
            switch self {
            case .arrowDown: return 125
            case .returnKey: return 36
            case .escape: return 53
            case .letterZ: return 6
            case .letterX: return 7
            }
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
                characters: characters,
                charactersIgnoringModifiers: characters,
                isARepeat: false,
                keyCode: keyCode)
        }
    }

    private static let navigationAction = """
    (() => {
      const button = document.querySelector(
        '#framework-flow-lazy-multi-select-combo-box-open-route');
      if (!button || typeof button.click !== 'function') return false;
      window.VaadinDesktopFlowLazyMultiSelectComboBox = {
        browserProgrammaticSetupObserved: true,
        browserProgrammaticStages: ['navigate'],
        nativePointerObserved: false,
        nativePointerTrustedObserved: false,
        nativePointerEventCount: 0,
        nativePointerTrustedEventCount: 0,
        nativePointerStages: [],
        nativeKeyboardObserved: false,
        nativeKeyboardTrustedObserved: false,
        nativeKeyboardEventCount: 0,
        nativeKeyboardTrustedEventCount: 0,
        nativeKeyboardStages: [],
        nativeKeyboardKeys: [],
        nativeTextInputObserved: false,
        nativeTextInputTrustedObserved: false,
        nativeTextInputEventCount: 0,
        nativeTextInputTrustedEventCount: 0,
        nativeTextInputStages: [],
        nativeTextInputValues: [],
        rawKeyboardEvents: [],
        rawInputEvents: [],
        rawSelectionEvents: []
      };
      button.click();
      return true;
    })()
    """

    private static let prepareOwnerPointerAction = """
    (() => {
      const marker = window.VaadinDesktopFlowLazyMultiSelectComboBox || {};
      window.VaadinDesktopFlowLazyMultiSelectComboBox = marker;
      const combo = document.querySelector(
        '#framework-flow-lazy-multi-select-combo-box-field');
      const input = combo?.inputElement
        || combo?.shadowRoot?.querySelector('input');
      if (!combo || !input || combo.opened === true) return '';
      if (marker.rawDiagnosticsOwner !== combo) {
        marker.rawDiagnosticsOwner = combo;
        marker.rawKeyboardEvents = [];
        marker.rawInputEvents = [];
        marker.rawSelectionEvents = [];
        const timestamp = () => Math.round(performance.now() * 10) / 10;
        const append = (name, entry) => {
          const events = Array.isArray(marker[name]) ? marker[name] : [];
          events.push(entry);
          marker[name] = events.slice(-80);
        };
        const selectedLabels = () => Array.from(combo.selectedItems || [])
          .map((item) => String(
            (typeof combo.itemLabelGenerator === 'function'
              ? combo.itemLabelGenerator(item) : '')
              || item?.label || item?.value || item?.text || ''))
          .map((label) => label.replace(/\\s+/g, ' ').trim())
          .filter(Boolean).sort();
        const belongsToOwner = (event) => {
          const path = event.composedPath?.() || [];
          return path.includes(combo) && path.includes(input);
        };
        const keyboardHandler = (event) => {
          if (!belongsToOwner(event)) return;
          append('rawKeyboardEvents', {
            at: timestamp(),
            type: String(event.type || ''),
            key: String(event.key || ''),
            code: String(event.code || ''),
            trusted: event.isTrusted === true,
            repeat: event.repeat === true,
            input: String(input.value || ''),
            filter: String(combo.filter || ''),
            selected: selectedLabels()
          });
        };
        const inputHandler = (event) => {
          if (!belongsToOwner(event)) return;
          append('rawInputEvents', {
            at: timestamp(),
            type: String(event.type || ''),
            inputType: String(event.inputType || ''),
            data: event.data == null ? null : String(event.data),
            trusted: event.isTrusted === true,
            input: String(input.value || ''),
            filter: String(combo.filter || ''),
            selected: selectedLabels()
          });
        };
        const selectionHandler = (event) => {
          append('rawSelectionEvents', {
            at: timestamp(),
            type: String(event.type || ''),
            trusted: event.isTrusted === true,
            input: String(input.value || ''),
            filter: String(combo.filter || ''),
            selected: selectedLabels()
          });
        };
        document.addEventListener('keydown', keyboardHandler, true);
        document.addEventListener('keyup', keyboardHandler, true);
        document.addEventListener('beforeinput', inputHandler, true);
        document.addEventListener('input', inputHandler, true);
        combo.addEventListener(
          'selected-items-changed', selectionHandler, true);
        combo.addEventListener('change', selectionHandler, true);
      }
      marker.browserProgrammaticSetupObserved = true;
      marker.browserProgrammaticStages =
        Array.isArray(marker.browserProgrammaticStages)
          ? marker.browserProgrammaticStages : [];
      if (!marker.browserProgrammaticStages.includes(
          'position-lazy-owner')) {
        marker.browserProgrammaticStages.push('position-lazy-owner');
      }
      combo.scrollIntoView({ block: 'center', inline: 'center' });
      const rect = input.getBoundingClientRect();
      const centerX = rect.left + rect.width / 2;
      const centerY = rect.top + rect.height / 2;
      const deepHit = (x, y) => {
        let hit = document.elementFromPoint(x, y);
        while (hit?.shadowRoot) {
          const inner = hit.shadowRoot.elementFromPoint(x, y);
          if (!inner || inner === hit) break;
          hit = inner;
        }
        return hit;
      };
      const hit = deepHit(centerX, centerY);
      const targetMatched = rect.width > 0 && rect.height > 0
        && centerX >= 0 && centerX <= window.innerWidth
        && centerY >= 0 && centerY <= window.innerHeight
        && (hit === input || input.contains?.(hit));
      if (typeof marker.pendingPointerHandler === 'function') {
        document.removeEventListener(
          'click', marker.pendingPointerHandler, true);
      }
      const handler = (event) => {
        const path = event.composedPath?.() || [];
        if (Number(event.button) !== 0
            || !path.includes(combo) || !path.includes(input)) return;
        document.removeEventListener('click', handler, true);
        marker.pendingPointerHandler = null;
        marker.nativePointerObserved = true;
        marker.nativePointerEventCount =
          Number(marker.nativePointerEventCount || 0) + 1;
        marker.nativePointerTrustedEventCount =
          Number(marker.nativePointerTrustedEventCount || 0)
            + (event.isTrusted === true ? 1 : 0);
        marker.nativePointerTrustedObserved =
          marker.nativePointerEventCount
            === marker.nativePointerTrustedEventCount;
        marker.nativePointerStages =
          Array.isArray(marker.nativePointerStages)
            ? marker.nativePointerStages : [];
        marker.nativePointerStages.push('open-lazy-owner');
      };
      marker.pendingPointerHandler = handler;
      document.addEventListener('click', handler, true);
      marker.ownerPointerDispatched = true;
      return JSON.stringify({ centerX, centerY, targetMatched });
    })()
    """

    private static let scrollFarPageAction = """
    (() => {
      const marker = window.VaadinDesktopFlowLazyMultiSelectComboBox || {};
      const combo = document.querySelector(
        '#framework-flow-lazy-multi-select-combo-box-field');
      const input = combo?.inputElement
        || combo?.shadowRoot?.querySelector('input');
      const controls = String(
        input?.getAttribute?.('aria-controls') || '');
      const findDeep = (root, predicate, visited = new Set()) => {
        if (!root || visited.has(root)) return null;
        visited.add(root);
        if (root.nodeType === Node.ELEMENT_NODE && predicate(root)) {
          return root;
        }
        if (root.shadowRoot) {
          const found = findDeep(root.shadowRoot, predicate, visited);
          if (found) return found;
        }
        if (typeof root.assignedElements === 'function') {
          for (const child of root.assignedElements({ flatten: true })) {
            const found = findDeep(child, predicate, visited);
            if (found) return found;
          }
        }
        for (const child of root.children || []) {
          const found = findDeep(child, predicate, visited);
          if (found) return found;
        }
        return null;
      };
      const scroller = controls
        ? findDeep(combo, (element) => element.id === controls) : null;
      const option = findDeep(scroller, (element) =>
        element.getAttribute?.('role') === 'option'
          && element.getBoundingClientRect().height > 0);
      if (!combo || !input || !scroller || !option
          || scroller.getAttribute?.('role') !== 'listbox'
          || combo.opened !== true) return false;
      const total = Number(option.getAttribute?.('aria-setsize') || 0);
      const maximum = Math.max(
        0, Number(scroller.scrollHeight || 0)
          - Number(scroller.clientHeight || 0));
      if (!(total > 59) || !(maximum > 0)) return false;
      marker.browserProgrammaticSetupObserved = true;
      marker.browserProgrammaticStages =
        Array.isArray(marker.browserProgrammaticStages)
          ? marker.browserProgrammaticStages : [];
      if (!marker.browserProgrammaticStages.includes(
          'scroll-lazy-r060-page')) {
        marker.browserProgrammaticStages.push('scroll-lazy-r060-page');
      }
      scroller.scrollTop = maximum * 59 / (total - 1);
      scroller.dispatchEvent(new Event('scroll', { bubbles: true }));
      marker.farPageScrollDispatched = true;
      return true;
    })()
    """

    private static let typeZAction = keyPreparation(
        stage: "type-lazy-z",
        flag: "zDispatched",
        condition: "combo.opened === true && input.value === ''"
            + " && String(combo.filter || '') === ''")
    private static let focusZedAction = keyPreparation(
        stage: "focus-lazy-zed",
        flag: "zedFocusDispatched",
        condition: "combo.opened === true && input.value === 'z'"
            + " && String(combo.filter || '') === 'z'"
            + " && marker.zServerQueryObserved === true")
    private static let selectZedAction = keyPreparation(
        stage: "select-lazy-zed",
        flag: "zedSelectionDispatched",
        condition: "combo.opened === true"
            + " && marker.zedOptionFocusedObserved === true")
    private static let clearZedFocusAction = keyPreparation(
        stage: "escape-clear-zed-focus",
        flag: "firstEscapeDispatched",
        condition: "combo.opened === true"
            + " && marker.clientSelectionObserved === true"
            + " && !!input.getAttribute('aria-activedescendant')")
    private static let closeZedPopupAction = keyPreparation(
        stage: "escape-close-zed",
        flag: "zedCloseDispatched",
        condition: "combo.opened === true && input.value === ''"
            + " && String(combo.filter || '') === 'z'"
            + " && !input.getAttribute('aria-activedescendant')"
            + " && marker.firstEscapeClearedOptionObserved === true")
    private static let typeRepeatZAction = keyPreparation(
        stage: "type-lazy-repeat-z",
        flag: "repeatZDispatched",
        condition: "combo.opened !== true && input.value === ''"
            + " && String(combo.filter || '') === ''"
            + " && marker.popupEscapeClosedObserved === true")
    private static let closeRepeatZPopupAction = keyPreparation(
        stage: "escape-close-repeat-z",
        flag: "repeatZCloseDispatched",
        condition: "combo.opened === true && input.value === 'z'"
            + " && String(combo.filter || '') === 'z'"
            + " && !input.getAttribute('aria-activedescendant')"
            + " && marker.repeatZSelectionRetainedObserved === true")
    private static let typeXAction = keyPreparation(
        stage: "type-lazy-x",
        flag: "xDispatched",
        condition: "combo.opened !== true && input.value === ''"
            + " && String(combo.filter || '') === ''"
            + " && marker.repeatZPopupClosedObserved === true")
    private static let closeXPopupAction = keyPreparation(
        stage: "escape-close-x",
        flag: "xCloseDispatched",
        condition: "combo.opened === true && input.value === 'x'"
            + " && String(combo.filter || '') === 'x'"
            + " && !input.getAttribute('aria-activedescendant')"
            + " && marker.xSelectionRetainedObserved === true")

    private static func keyPreparation(
        stage: String,
        flag: String,
        condition: String
    ) -> String {
        """
        (() => {
          const marker = window.VaadinDesktopFlowLazyMultiSelectComboBox || {};
          window.VaadinDesktopFlowLazyMultiSelectComboBox = marker;
          const combo = document.querySelector(
            '#framework-flow-lazy-multi-select-combo-box-field');
          const input = combo?.inputElement
            || combo?.shadowRoot?.querySelector('input');
          if (!combo || !input || !(\(condition))) return false;
          const activePath = [];
          let root = document;
          while (root?.activeElement) {
            const active = root.activeElement;
            activePath.push(active);
            root = active.shadowRoot;
          }
          if (!activePath.includes(input)) return false;
          marker.\(flag) = true;
          marker.nativeKeyboardStage = '\(stage)';
          return true;
        })()
        """
    }

    private static let inspectionScript = """
    (() => {
      const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
      const marker = window.VaadinDesktopFlowLazyMultiSelectComboBox || {};
      window.VaadinDesktopFlowLazyMultiSelectComboBox = marker;
      const route = document.querySelector(
        '#framework-flow-lazy-multi-select-combo-box-route');
      const combo = document.querySelector(
        '#framework-flow-lazy-multi-select-combo-box-field');
      const input = combo?.inputElement
        || combo?.shadowRoot?.querySelector('input');
      const text = (selector) => String(
        document.querySelector(selector)?.textContent || '')
        .replace(/\\s+/g, ' ').trim();
      const initialQueryStatus = text(
        '#framework-flow-lazy-multi-select-combo-box-initial-status');
      const farPageStatus = text(
        '#framework-flow-lazy-multi-select-combo-box-far-status');
      const zQueryStatus = text(
        '#framework-flow-lazy-multi-select-combo-box-z-query-status');
      const repeatZQueryStatus = text(
        '#framework-flow-lazy-multi-select-combo-box-repeat-z-query-status');
      const xQueryStatus = text(
        '#framework-flow-lazy-multi-select-combo-box-x-query-status');
      const selectionStatus = text(
        '#framework-flow-lazy-multi-select-combo-box-selection-status');
      const clients = window.Vaadin && window.Vaadin.Flow
        && window.Vaadin.Flow.clients
          ? Object.keys(window.Vaadin.Flow.clients)
            .filter((name) => name !== 'TypeScript') : [];
      const collectDeepText = (rootNode) => {
        const parts = [];
        const visited = new Set();
        const visit = (node) => {
          if (!node || visited.has(node)) return;
          visited.add(node);
          if (node.nodeType === Node.TEXT_NODE) {
            const value = String(node.textContent || '').trim();
            if (value) parts.push(value);
            return;
          }
          if (node.nodeType !== Node.ELEMENT_NODE
              && node.nodeType !== Node.DOCUMENT_FRAGMENT_NODE
              && node.nodeType !== Node.DOCUMENT_NODE) return;
          if (node.shadowRoot) visit(node.shadowRoot);
          if (typeof node.assignedNodes === 'function') {
            for (const child of node.assignedNodes({ flatten: true })) {
              visit(child);
            }
          }
          for (const child of node.childNodes || []) visit(child);
        };
        visit(rootNode);
        return parts.join(' ').replace(/\\s+/g, ' ').trim();
      };
      const findDeep = (rootNode, predicate, visited = new Set()) => {
        if (!rootNode || visited.has(rootNode)) return null;
        visited.add(rootNode);
        if (rootNode.nodeType === Node.ELEMENT_NODE
            && predicate(rootNode)) return rootNode;
        if (rootNode.shadowRoot) {
          const result = findDeep(rootNode.shadowRoot, predicate, visited);
          if (result) return result;
        }
        if (typeof rootNode.assignedElements === 'function') {
          for (const child of rootNode.assignedElements({ flatten: true })) {
            const result = findDeep(child, predicate, visited);
            if (result) return result;
          }
        }
        for (const child of rootNode.children || []) {
          const result = findDeep(child, predicate, visited);
          if (result) return result;
        }
        return null;
      };
      const findAllDeep = (rootNode, predicate) => {
        const matches = [];
        const visited = new Set();
        const visit = (node) => {
          if (!node || visited.has(node)) return;
          visited.add(node);
          if (node.nodeType === Node.ELEMENT_NODE && predicate(node)) {
            matches.push(node);
          }
          if (node.shadowRoot) visit(node.shadowRoot);
          if (typeof node.assignedElements === 'function') {
            for (const child of node.assignedElements({ flatten: true })) {
              visit(child);
            }
          }
          for (const child of node.children || []) visit(child);
        };
        visit(rootNode);
        return matches;
      };
      const activePath = [];
      let activeRoot = document;
      while (activeRoot?.activeElement) {
        const active = activeRoot.activeElement;
        activePath.push(active);
        activeRoot = active.shadowRoot;
      }
      const ownerFocusCurrent = !!input && activePath.includes(input);
      const controls = String(
        input?.getAttribute?.('aria-controls') || '');
      const scroller = controls
        ? findDeep(combo, (element) => element.id === controls) : null;
      const visibleOptions = findAllDeep(
        scroller,
        (element) => element.getAttribute?.('role') === 'option')
        .filter((element) => {
          const rect = element.getBoundingClientRect();
          return rect.width > 0 && rect.height > 0
            && rect.bottom > 0 && rect.top < window.innerHeight;
        });
      const optionLabel = (element) => String(
        element?.getAttribute?.('aria-label')
          || collectDeepText(element) || '')
        .replace(/\\s+/g, ' ').trim();
      const visibleOptionLabels = visibleOptions
        .map(optionLabel).filter(Boolean).sort().join(',');
      const optionByLabel = (label) => visibleOptions.find(
        (element) => optionLabel(element) === label) || null;
      const activeDescendant = String(
        input?.getAttribute?.('aria-activedescendant') || '');
      const activeOption = activeDescendant
        ? (visibleOptions.find(
          (element) => element.id === activeDescendant)
          || findDeep(scroller,
            (element) => element.id === activeDescendant)) : null;
      const activeOptionLabel = optionLabel(activeOption);
      const activeOptionRole = String(
        activeOption?.getAttribute?.('role') || '');
      const activeOptionAriaSelected = String(
        activeOption?.getAttribute?.('aria-selected') || '');
      const chips = Array.from(combo?.querySelectorAll(
        'vaadin-multi-select-combo-box-chip[slot="chip"]') || []);
      const chipLabels = chips.map((chip) => String(
        chip.getAttribute?.('label') || chip.label
          || chip.getAttribute?.('title') || collectDeepText(chip) || '')
        .replace(/\\s+/g, ' ').trim()).filter(Boolean).sort();
      const visibleChipLabels = chipLabels.join(',');
      const selectedItemLabels = Array.from(combo?.selectedItems || [])
        .map((item) => String(
          (typeof combo?.itemLabelGenerator === 'function'
            ? combo.itemLabelGenerator(item) : '')
            || item?.label || item?.value || item?.text || ''))
        .map((label) => label.replace(/\\s+/g, ' ').trim())
        .filter(Boolean);
      const selectedItems = selectedItemLabels.map((label) => {
        const match = label.match(/^R-\\d{3}/);
        return match ? match[0] : '';
      }).filter(Boolean).sort().join(',');
      const inputValue = String(input?.value || '');
      const filterValue = String(combo?.filter || '');
      const opened = combo?.opened === true;
      const popupA11yCurrent = opened
        && input?.getAttribute?.('role') === 'combobox'
        && input?.getAttribute?.('aria-autocomplete') === 'list'
        && input?.getAttribute?.('aria-expanded') === 'true'
        && !!controls && !!scroller
        && scroller.getAttribute?.('role') === 'listbox'
        && scroller.getAttribute?.('aria-multiselectable') === 'true';
      const closedA11yCurrent = !opened
        && input?.getAttribute?.('aria-expanded') === 'false'
        && !input?.hasAttribute?.('aria-controls');
      const initialStatusExpected =
        'Lazy multi-select initial: filter=all count=80'
          + ' fetch=true offset=0 limit-positive=true'
          + ' fresh=true selected=R-060';
      const farStatusExpected =
        'Lazy multi-select far page: filter=all'
          + ' offset-positive=true limit-positive=true'
          + ' contains=R-060 fresh=true selected=R-060';
      const zStatusExpected =
        'Lazy multi-select z query: filter=z count=1'
          + ' fetch=R-061 offset=0 limit-positive=true'
          + ' fresh=true selected=R-060';
      const xStatusExpected =
        'Lazy multi-select x query: filter=x count=1'
          + ' fetch=R-060 offset=0 limit-positive=true'
          + ' fresh=true selected=R-060,R-061';
      const selectionStatusExpected =
        'Lazy multi-select selection: selected=R-060,R-061 change=1'
          + ' added=R-060,R-061 removed=R-060 client=true';
      const initialSelectionStatusExpected =
        'Lazy multi-select selection: selected=R-060 change=0'
          + ' added=none removed=none client=false';
      const r060Option = optionByLabel('R-060 Xeno');
      const r061Option = optionByLabel('R-061 Zed');
      const repeatZFetchMatch = repeatZQueryStatus.match(
        /(?:^|\\s)fetch-count=(\\d+)(?:\\s|$)/);
      const zFetchCount = repeatZFetchMatch
        ? Number(repeatZFetchMatch[1]) : 0;
      const repeatZStatusObserved =
        /^Lazy multi-select repeated z query: fetch-count=\\d+ fetch=R-061 offset=0 limit-positive=true fresh=true selected=R-060,R-061$/
          .test(repeatZQueryStatus);

      if (route && combo) marker.dedicatedRouteObserved = true;
      if (selectedItems === 'R-060'
          && visibleChipLabels === 'R-060 Xeno'
          && selectionStatus === initialSelectionStatusExpected) {
        marker.initialServerValueObserved = true;
      }
      if (initialQueryStatus === initialStatusExpected) {
        marker.initialQueryObserved = true;
      }
      if (farPageStatus === farStatusExpected) {
        marker.farPageQueryObserved = true;
      }
      if (farPageStatus === farStatusExpected && !!r060Option) {
        marker.farPageFreshR060Observed = true;
      }
      if (farPageStatus === farStatusExpected
          && r060Option?.getAttribute?.('role') === 'option'
          && r060Option?.getAttribute?.('aria-selected') === 'true'
          && selectedItems === 'R-060'
          && visibleChipLabels === 'R-060 Xeno') {
        marker.farPageR060AriaSelectedObserved = true;
      }
      if (zQueryStatus === zStatusExpected) {
        marker.zServerQueryObserved = true;
      }
      if (marker.zServerQueryObserved === true
          && visibleOptionLabels === 'R-061 Zed'
          && inputValue === 'z' && filterValue === 'z'
          && opened && ownerFocusCurrent
          && selectedItems === 'R-060'
          && visibleChipLabels === 'R-060 Xeno') {
        marker.zSingleResultObserved = true;
      }
      if (activeOptionLabel === 'R-061 Zed'
          && activeOptionRole === 'option'
          && activeOptionAriaSelected === 'false'
          && inputValue === 'R-061 Zed' && filterValue === 'z'
          && opened && ownerFocusCurrent) {
        marker.zedOptionFocusedObserved = true;
      }
      if (selectionStatus === selectionStatusExpected
          && selectedItems === 'R-060,R-061'
          && visibleChipLabels === 'R-060 Xeno,R-061 Zed') {
        marker.clientSelectionObserved = true;
      }
      if (marker.clientSelectionObserved === true
          && inputValue === 'z' && filterValue === 'z'
          && opened && ownerFocusCurrent
          && r061Option?.getAttribute?.('aria-selected') === 'true') {
        marker.selectionPreservedObserved = true;
      }
      if (marker.clientSelectionObserved === true
          && inputValue === '' && filterValue === 'z'
          && opened && ownerFocusCurrent
          && activeDescendant === ''
          && visibleOptionLabels === 'R-061 Zed'
          && selectedItems === 'R-060,R-061') {
        marker.firstEscapeClearedOptionObserved = true;
        marker.popupRetainedAfterFirstEscapeObserved = true;
      }
      if (marker.firstEscapeClearedOptionObserved === true
          && closedA11yCurrent && inputValue === '' && filterValue === ''
          && visibleOptionLabels === '' && ownerFocusCurrent
          && selectedItems === 'R-060,R-061'
          && visibleChipLabels === 'R-060 Xeno,R-061 Zed') {
        marker.popupEscapeClosedObserved = true;
        marker.popupFocusRestoredObserved = true;
      }
      if (marker.repeatZDispatched === true
          && repeatZStatusObserved) {
        marker.repeatZServerQueryObserved = true;
      }
      if (marker.repeatZServerQueryObserved === true
          && visibleOptionLabels === 'R-061 Zed'
          && inputValue === 'z' && filterValue === 'z'
          && opened && ownerFocusCurrent
          && activeDescendant === ''
          && selectedItems === 'R-060,R-061'
          && visibleChipLabels === 'R-060 Xeno,R-061 Zed') {
        marker.repeatZSingleSelectedResultObserved = true;
      }
      if (marker.repeatZSingleSelectedResultObserved === true
          && r061Option?.getAttribute?.('role') === 'option'
          && r061Option?.getAttribute?.('aria-selected') === 'true') {
        marker.repeatZFreshR061AriaSelectedObserved = true;
      }
      if (marker.repeatZFreshR061AriaSelectedObserved === true
          && selectedItems === 'R-060,R-061'
          && visibleChipLabels === 'R-060 Xeno,R-061 Zed'
          && selectionStatus === selectionStatusExpected) {
        marker.repeatZSelectionRetainedObserved = true;
      }
      if (marker.repeatZSelectionRetainedObserved === true
          && closedA11yCurrent && inputValue === '' && filterValue === ''
          && visibleOptionLabels === '' && ownerFocusCurrent
          && selectedItems === 'R-060,R-061'
          && visibleChipLabels === 'R-060 Xeno,R-061 Zed'
          && selectionStatus === selectionStatusExpected) {
        marker.repeatZPopupClosedObserved = true;
        marker.repeatZFocusRestoredObserved = true;
        marker.repeatZFilterClearedObserved = true;
      }
      if (xQueryStatus === xStatusExpected) {
        marker.xServerQueryObserved = true;
      }
      if (marker.xServerQueryObserved === true
          && visibleOptionLabels === 'R-060 Xeno'
          && inputValue === 'x' && filterValue === 'x'
          && opened && ownerFocusCurrent
          && activeDescendant === '') {
        marker.xSingleResultObserved = true;
      }
      if (marker.xSingleResultObserved === true
          && r060Option?.getAttribute?.('role') === 'option'
          && r060Option?.getAttribute?.('aria-selected') === 'true') {
        marker.xFreshR060AriaSelectedObserved = true;
      }
      if (marker.xFreshR060AriaSelectedObserved === true
          && selectedItems === 'R-060,R-061'
          && visibleChipLabels === 'R-060 Xeno,R-061 Zed'
          && selectionStatus === selectionStatusExpected) {
        marker.xSelectionRetainedObserved = true;
      }
      if (marker.xSelectionRetainedObserved === true
          && closedA11yCurrent && inputValue === '' && filterValue === ''
          && visibleOptionLabels === '' && ownerFocusCurrent
          && selectedItems === 'R-060,R-061'
          && visibleChipLabels === 'R-060 Xeno,R-061 Zed') {
        marker.finalPopupClosedObserved = true;
        marker.finalFocusRestoredObserved = true;
        marker.finalFilterClearedObserved = true;
      }
      const selectionChangeMatch = selectionStatus.match(
        /(?:^|\\s)change=(\\d+)(?:\\s|$)/);
      const javaSelectionChangeCount = selectionChangeMatch
        ? Number(selectionChangeMatch[1]) : -1;
      return JSON.stringify({
        rootRouteAvailable: !!document.querySelector(
          '#framework-flow-lazy-multi-select-combo-box-open-route'),
        routeRendered: !!route,
        firstMeaningfulPaint:
          diagnostics.standardBundleFirstMeaningfulPaintReported === true,
        requestCount: Number(
          diagnostics.standardBundleOpaqueRequestCount || 0),
        responseCount: Number(
          diagnostics.standardBundleOpaqueResponseCount || 0),
        pushMessageCount: Number(
          diagnostics.standardBundlePushMessageCount || 0),
        flowClientCount: clients.length,
        dedicatedRouteObserved:
          marker.dedicatedRouteObserved === true,
        initialServerValueObserved:
          marker.initialServerValueObserved === true,
        initialQueryObserved: marker.initialQueryObserved === true,
        popupOpenedCurrent: opened,
        popupA11yCurrent,
        ownerFocusCurrent,
        farPageQueryObserved: marker.farPageQueryObserved === true,
        farPageFreshR060Observed:
          marker.farPageFreshR060Observed === true,
        farPageR060AriaSelectedObserved:
          marker.farPageR060AriaSelectedObserved === true,
        zKeyDownTrustedObserved:
          marker.zKeyDownTrustedObserved === true,
        zInputTrustedObserved: marker.zInputTrustedObserved === true,
        zKeyDownBeforeInputObserved:
          marker.zKeyDownBeforeInputObserved === true,
        zServerQueryObserved: marker.zServerQueryObserved === true,
        zSingleResultObserved: marker.zSingleResultObserved === true,
        zedOptionFocusedObserved:
          marker.zedOptionFocusedObserved === true,
        clientSelectionObserved:
          marker.clientSelectionObserved === true,
        selectionPreservedObserved:
          marker.selectionPreservedObserved === true,
        firstEscapeClearedOptionObserved:
          marker.firstEscapeClearedOptionObserved === true,
        popupRetainedAfterFirstEscapeObserved:
          marker.popupRetainedAfterFirstEscapeObserved === true,
        popupEscapeClosedObserved:
          marker.popupEscapeClosedObserved === true,
        popupFocusRestoredObserved:
          marker.popupFocusRestoredObserved === true,
        repeatZKeyDownTrustedObserved:
          marker.repeatZKeyDownTrustedObserved === true,
        repeatZInputTrustedObserved:
          marker.repeatZInputTrustedObserved === true,
        repeatZKeyDownBeforeInputObserved:
          marker.repeatZKeyDownBeforeInputObserved === true,
        repeatZServerQueryObserved:
          marker.repeatZServerQueryObserved === true,
        repeatZSingleSelectedResultObserved:
          marker.repeatZSingleSelectedResultObserved === true,
        repeatZFreshR061AriaSelectedObserved:
          marker.repeatZFreshR061AriaSelectedObserved === true,
        repeatZSelectionRetainedObserved:
          marker.repeatZSelectionRetainedObserved === true,
        repeatZPopupClosedObserved:
          marker.repeatZPopupClosedObserved === true,
        repeatZFocusRestoredObserved:
          marker.repeatZFocusRestoredObserved === true,
        repeatZFilterClearedObserved:
          marker.repeatZFilterClearedObserved === true,
        zFetchCount,
        xKeyDownTrustedObserved:
          marker.xKeyDownTrustedObserved === true,
        xInputTrustedObserved: marker.xInputTrustedObserved === true,
        xKeyDownBeforeInputObserved:
          marker.xKeyDownBeforeInputObserved === true,
        xServerQueryObserved: marker.xServerQueryObserved === true,
        xSingleResultObserved: marker.xSingleResultObserved === true,
        xFreshR060AriaSelectedObserved:
          marker.xFreshR060AriaSelectedObserved === true,
        xSelectionRetainedObserved:
          marker.xSelectionRetainedObserved === true,
        finalPopupClosedObserved:
          marker.finalPopupClosedObserved === true,
        finalFocusRestoredObserved:
          marker.finalFocusRestoredObserved === true,
        finalFilterClearedObserved:
          marker.finalFilterClearedObserved === true,
        selectedItems,
        visibleChipLabels,
        inputValue,
        filterValue,
        visibleOptionLabels,
        initialQueryStatus,
        farPageStatus,
        zQueryStatus,
        repeatZQueryStatus,
        xQueryStatus,
        selectionStatus,
        javaSelectionChangeCount,
        nativePointerObserved: marker.nativePointerObserved === true,
        nativePointerTrustedObserved:
          marker.nativePointerTrustedObserved === true,
        nativePointerEventCount:
          Number(marker.nativePointerEventCount || 0),
        nativePointerTrustedEventCount:
          Number(marker.nativePointerTrustedEventCount || 0),
        nativePointerStages:
          Array.isArray(marker.nativePointerStages)
            ? marker.nativePointerStages.join(',') : '',
        nativeKeyboardObserved: marker.nativeKeyboardObserved === true,
        nativeKeyboardTrustedObserved:
          marker.nativeKeyboardTrustedObserved === true,
        nativeKeyboardEventCount:
          Number(marker.nativeKeyboardEventCount || 0),
        nativeKeyboardTrustedEventCount:
          Number(marker.nativeKeyboardTrustedEventCount || 0),
        nativeKeyboardStages:
          Array.isArray(marker.nativeKeyboardStages)
            ? marker.nativeKeyboardStages.join(',') : '',
        nativeKeyboardKeys:
          Array.isArray(marker.nativeKeyboardKeys)
            ? marker.nativeKeyboardKeys.join(',') : '',
        nativeTextInputObserved:
          marker.nativeTextInputObserved === true,
        nativeTextInputTrustedObserved:
          marker.nativeTextInputTrustedObserved === true,
        nativeTextInputEventCount:
          Number(marker.nativeTextInputEventCount || 0),
        nativeTextInputTrustedEventCount:
          Number(marker.nativeTextInputTrustedEventCount || 0),
        nativeTextInputStages:
          Array.isArray(marker.nativeTextInputStages)
            ? marker.nativeTextInputStages.join(',') : '',
        nativeTextInputValues:
          Array.isArray(marker.nativeTextInputValues)
            ? marker.nativeTextInputValues.join(',') : '',
        browserProgrammaticSetupObserved:
          marker.browserProgrammaticSetupObserved === true,
        browserProgrammaticStages:
          Array.isArray(marker.browserProgrammaticStages)
            ? marker.browserProgrammaticStages.join(',') : '',
        rawKeyboardEvents: JSON.stringify(
          Array.isArray(marker.rawKeyboardEvents)
            ? marker.rawKeyboardEvents : []),
        rawInputEvents: JSON.stringify(
          Array.isArray(marker.rawInputEvents)
            ? marker.rawInputEvents : []),
        rawSelectionEvents: JSON.stringify(
          Array.isArray(marker.rawSelectionEvents)
            ? marker.rawSelectionEvents : []),
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
