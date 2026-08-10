import AppKit
import Foundation

extension FlowIpcAcceptanceRuntimeObserver {
    func scheduleFlowInputPopupAcceptance(
        host: WebViewHost,
        context: FlowIpcWindowContext,
        timeoutSeconds: Double
    ) {
        FlowIpcInputPopupAcceptance(
            context: context,
            deadline: Date().addingTimeInterval(timeoutSeconds),
            scope: .full
        ).start()
    }

    func scheduleLocalizedInputPopupAcceptance(
        host: WebViewHost,
        context: FlowIpcWindowContext,
        timeoutSeconds: Double
    ) {
        FlowIpcInputPopupAcceptance(
            context: context,
            deadline: Date().addingTimeInterval(timeoutSeconds),
            scope: .localizedTime
        ).start()
    }
}

private final class FlowIpcInputPopupAcceptance {
    fileprivate enum Scope {
        case full
        case localizedTime
    }

    private enum Phase {
        case ready
        case dedicatedRoute
        case comboOpened
        case comboOptionFocused
        case comboSelected
        case selectOpened
        case selectMoved
        case selectSelected
        case selectEscapeOpened
        case selectEscapeClosed
        case selectPointerOpened
        case selectPointerClosed
        case dateOpened
        case dateFocused
        case dateMoved
        case dateSelected
        case dateEscapeOpened
        case dateEscapeFocused
        case dateEscapeClosed
        case datePointerOpened
        case datePointerFocused
        case datePointerClosed
        case timeOpened
        case timeFirstOptionFocused
        case timeSecondOptionFocused
        case timeSelectedOptionFocused
        case timeMoved
        case timeSelected
    }

    private let context: FlowIpcWindowContext
    private let deadline: Date
    private let scope: Scope
    private var phase = Phase.ready

    fileprivate init(
        context: FlowIpcWindowContext,
        deadline: Date,
        scope: Scope
    ) {
        self.context = context
        self.deadline = deadline
        self.scope = scope
    }

    func start() {
        poll()
    }

    private func poll() {
        inspect { snapshot in
            guard let snapshot else {
                self.retryOrFail(code: "flowInputPopupInspectionFailed")
                return
            }
            guard snapshot.errorFree else {
                self.fail(
                    code: "flowInputPopupBrowserRuntimeError",
                    details: snapshot.tracePayload)
                return
            }

            switch self.phase {
            case .ready:
                guard snapshot.ready else {
                    self.retryOrFail(
                        code: "flowInputPopupReadyTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.perform(
                    Self.navigationScript,
                    next: .dedicatedRoute,
                    failureCode: "flowInputPopupNavigationFailed")
            case .dedicatedRoute:
                guard snapshot.dedicatedRouteReady else {
                    self.retryOrFail(
                        code: "flowInputPopupDedicatedRouteTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                if self.scope == .localizedTime {
                    self.performNativeKey(
                        .arrowDown,
                        Self.focusTimeScript,
                        next: .timeOpened,
                        failureCode: "flowInputPopupTimeOpenFailed")
                } else {
                    self.perform(
                        Self.openComboScript,
                        next: .comboOpened,
                        failureCode: "flowInputPopupComboOpenFailed")
                }
            case .comboOpened:
                guard snapshot.comboOpenedObserved,
                      snapshot.comboA11yObserved,
                      snapshot.comboOwnerFocusObserved else {
                    self.retryOrFail(
                        code: "flowInputPopupComboOpenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.perform(
                    Self.focusComboOptionScript,
                    next: .comboOptionFocused,
                    failureCode: "flowInputPopupComboNavigationFailed")
            case .comboOptionFocused:
                guard snapshot.comboActiveDescendantObserved else {
                    self.retryOrFail(
                        code: "flowInputPopupComboOptionTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.perform(
                    Self.selectComboOptionScript,
                    next: .comboSelected,
                    failureCode: "flowInputPopupComboSelectionFailed")
            case .comboSelected:
                guard snapshot.comboSelectionObserved,
                      snapshot.comboJavaValueObserved,
                      snapshot.comboClosedObserved,
                      snapshot.comboFocusRestoredObserved,
                      snapshot.comboCollapsedA11yObserved else {
                    self.retryOrFail(
                        code: "flowInputPopupComboPatchTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.focusSelectScript,
                    next: .selectOpened,
                    failureCode: "flowInputPopupSelectOpenFailed")
            case .selectOpened:
                guard snapshot.selectOpenedObserved,
                      snapshot.selectA11yObserved,
                      snapshot.selectInitialFocusObserved,
                      snapshot.selectNativeKeyboardObserved else {
                    self.retryOrFail(
                        code: "flowInputPopupSelectOpenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.moveSelectScript,
                    next: .selectMoved,
                    failureCode: "flowInputPopupSelectMoveFailed")
            case .selectMoved:
                guard snapshot.selectKeyboardMoveObserved else {
                    self.retryOrFail(
                        code: "flowInputPopupSelectMoveTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .returnKey,
                    Self.selectSelectScript,
                    next: .selectSelected,
                    failureCode: "flowInputPopupSelectSelectionFailed")
            case .selectSelected:
                guard snapshot.selectSelectionObserved,
                      snapshot.selectJavaValueObserved,
                      snapshot.selectClosedObserved,
                      snapshot.selectFocusRestoredObserved,
                      snapshot.selectCollapsedA11yObserved else {
                    self.retryOrFail(
                        code: "flowInputPopupSelectPatchTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.reopenSelectScript,
                    next: .selectEscapeOpened,
                    failureCode: "flowInputPopupSelectReopenFailed")
            case .selectEscapeOpened:
                guard snapshot.selectEscapeOpenedObserved,
                      snapshot.selectEscapeInitialFocusObserved,
                      snapshot.selectA11yObserved else {
                    self.retryOrFail(
                        code: "flowInputPopupSelectReopenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .escape,
                    Self.closeSelectWithEscapeScript,
                    next: .selectEscapeClosed,
                    failureCode: "flowInputPopupSelectEscapeFailed")
            case .selectEscapeClosed:
                guard snapshot.selectEscapeCloseObserved,
                      snapshot.selectValueRetainedObserved,
                      snapshot.selectFocusRestoredObserved else {
                    self.retryOrFail(
                        code: "flowInputPopupSelectEscapeTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.reopenSelectForPointerScript,
                    next: .selectPointerOpened,
                    failureCode: "flowInputPopupSelectPointerOpenFailed")
            case .selectPointerOpened:
                guard snapshot.selectPointerOpenedObserved,
                      snapshot.selectA11yObserved else {
                    self.retryOrFail(
                        code: "flowInputPopupSelectPointerOpenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativePrimaryClick(
                    Self.prepareSelectOutsidePointerScript,
                    next: .selectPointerClosed,
                    failureCode: "flowInputPopupSelectOutsideClickFailed")
            case .selectPointerClosed:
                guard snapshot.selectPointerCloseObserved,
                      snapshot.selectPointerValueRetainedObserved,
                      snapshot.selectPointerFocusRestoredObserved,
                      snapshot.selectPointerJavaLifecycleObserved,
                      snapshot.selectPointerUnderlyingClickSuppressedObserved,
                      snapshot.nativePointerCurrentObserved else {
                    self.retryOrFail(
                        code: "flowInputPopupSelectOutsideClickTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.perform(
                    Self.openDateScript,
                    next: .dateOpened,
                    failureCode: "flowInputPopupDateOpenFailed")
            case .dateOpened:
                guard snapshot.dateOpenedObserved,
                      snapshot.dateA11yObserved,
                      snapshot.dateJavaLifecycleObserved else {
                    self.retryOrFail(
                        code: "flowInputPopupDateOpenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.focusDateScript,
                    next: .dateFocused,
                    failureCode: "flowInputPopupDateFocusFailed")
            case .dateFocused:
                guard snapshot.dateCalendarFocusObserved else {
                    self.retryOrFail(
                        code: "flowInputPopupDateFocusTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowRight,
                    Self.moveDateScript,
                    next: .dateMoved,
                    failureCode: "flowInputPopupDateMoveFailed")
            case .dateMoved:
                guard snapshot.dateKeyboardMoveObserved else {
                    self.retryOrFail(
                        code: "flowInputPopupDateMoveTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .returnKey,
                    Self.selectDateScript,
                    next: .dateSelected,
                    failureCode: "flowInputPopupDateSelectionFailed")
            case .dateSelected:
                guard snapshot.dateSelectionObserved,
                      snapshot.dateJavaValueObserved,
                      snapshot.dateClosedObserved,
                      snapshot.dateFocusRestoredObserved else {
                    self.retryOrFail(
                        code: "flowInputPopupDatePatchTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.perform(
                    Self.reopenDateScript,
                    next: .dateEscapeOpened,
                    failureCode: "flowInputPopupDateReopenFailed")
            case .dateEscapeOpened:
                guard snapshot.dateEscapeOpenedObserved,
                      snapshot.dateA11yObserved else {
                    self.retryOrFail(
                        code: "flowInputPopupDateReopenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.focusDateScript,
                    next: .dateEscapeFocused,
                    failureCode: "flowInputPopupDateRefocusFailed")
            case .dateEscapeFocused:
                guard snapshot.dateCalendarFocusObserved else {
                    self.retryOrFail(
                        code: "flowInputPopupDateRefocusTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .escape,
                    Self.closeDateWithEscapeScript,
                    next: .dateEscapeClosed,
                    failureCode: "flowInputPopupDateEscapeFailed")
            case .dateEscapeClosed:
                guard snapshot.dateEscapeCloseObserved,
                      snapshot.dateValueRetainedObserved,
                      snapshot.dateJavaLifecycleObserved,
                      snapshot.dateFocusRestoredObserved else {
                    self.retryOrFail(
                        code: "flowInputPopupDateEscapeTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.perform(
                    Self.reopenDateForPointerScript,
                    next: .datePointerOpened,
                    failureCode: "flowInputPopupDatePointerOpenFailed")
            case .datePointerOpened:
                guard snapshot.datePointerOpenedObserved,
                      snapshot.dateA11yObserved else {
                    self.retryOrFail(
                        code: "flowInputPopupDatePointerOpenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.focusDateForPointerScript,
                    next: .datePointerFocused,
                    failureCode: "flowInputPopupDatePointerFocusFailed")
            case .datePointerFocused:
                guard snapshot.datePointerFocusObserved else {
                    self.retryOrFail(
                        code: "flowInputPopupDatePointerFocusTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativePrimaryClick(
                    Self.prepareDateOutsidePointerScript,
                    next: .datePointerClosed,
                    failureCode: "flowInputPopupDateOutsideClickFailed")
            case .datePointerClosed:
                guard snapshot.datePointerCloseObserved,
                      snapshot.datePointerValueRetainedObserved,
                      snapshot.datePointerFocusRestoredObserved,
                      snapshot.datePointerUnderlyingClickSuppressedObserved,
                      snapshot.datePointerJavaLifecycleObserved,
                      snapshot.nativePointerCurrentObserved else {
                    self.retryOrFail(
                        code: "flowInputPopupDateOutsideClickTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.finish(snapshot)
            case .timeOpened:
                guard snapshot.timeOpenedObserved,
                      snapshot.timeA11yObserved,
                      snapshot.timeInitialOwnerFocusObserved,
                      snapshot.timeLocaleFormatObserved,
                      snapshot.timeNativeKeyboardObserved else {
                    self.retryOrFail(
                        code: "flowInputPopupTimeOpenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.moveTimeScript,
                    next: .timeFirstOptionFocused,
                    failureCode: "flowInputPopupTimeMoveFailed")
            case .timeFirstOptionFocused:
                guard snapshot.timeFirstOptionFocusObserved else {
                    self.retryOrFail(
                        code: "flowInputPopupTimeFirstOptionFocusTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.moveTimeScript,
                    next: .timeSecondOptionFocused,
                    failureCode: "flowInputPopupTimeSecondMoveFailed")
            case .timeSecondOptionFocused:
                guard snapshot.timeSecondOptionFocusObserved else {
                    self.retryOrFail(
                        code: "flowInputPopupTimeSecondOptionFocusTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.moveTimeScript,
                    next: .timeSelectedOptionFocused,
                    failureCode: "flowInputPopupTimeSelectedOptionMoveFailed")
            case .timeSelectedOptionFocused:
                guard snapshot.timeSelectedOptionFocusObserved else {
                    self.retryOrFail(
                        code: "flowInputPopupTimeSelectedOptionFocusTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.moveTimeScript,
                    next: .timeMoved,
                    failureCode: "flowInputPopupTimeMoveFailed")
            case .timeMoved:
                guard snapshot.timeKeyboardMoveObserved else {
                    self.retryOrFail(
                        code: "flowInputPopupTimeMoveTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .returnKey,
                    Self.selectTimeScript,
                    next: .timeSelected,
                    failureCode: "flowInputPopupTimeSelectionFailed")
            case .timeSelected:
                guard snapshot.timeSelectionObserved,
                      snapshot.timeJavaValueObserved,
                      snapshot.timeClosedObserved,
                      snapshot.timeFocusRestoredObserved,
                      snapshot.timeLocaleFormatObserved else {
                    self.retryOrFail(
                        code: "flowInputPopupTimePatchTimeout",
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
          const marker = window.VaadinDesktopFlowInputPopup || {};
          window.VaadinDesktopFlowInputPopup = marker;
          marker.nativeKeyboardExpected = '\(key.name)';
          marker.nativeKeyboardCurrentObserved = false;
          const nativeKeyboardStage =
            String(marker.nativeKeyboardStage || '');
          document.addEventListener('keydown', (event) => {
            marker.nativeKeyboardObserved = true;
            marker.nativeKeyboardCurrentObserved = true;
            marker.nativeKeyboardEventCount =
              Number(marker.nativeKeyboardEventCount || 0) + 1;
            marker.nativeKeyboardStages = [
              String(marker.nativeKeyboardStages || ''),
              nativeKeyboardStage
            ].filter(Boolean).join(',');
            if (nativeKeyboardStage.startsWith('select-')) {
              marker.selectNativeKeyboardObserved = true;
            }
            if (nativeKeyboardStage.startsWith('time-')) {
              marker.timeNativeKeyboardObserved = true;
            }
            marker.nativeKeyboardLastKey = String(event.key || '');
            marker.nativeKeyboardLastTarget =
              String(event.target?.id || event.target?.localName || '');
          }, { capture: true, once: true });
          return true;
        })()
        """
        context.webView.evaluateJavaScript(
            instrumentedScript
        ) { result, error in
            guard error == nil, result as? Bool == true else {
                self.retryOrFail(
                    code: failureCode,
                    details: [
                        "message": error?.localizedDescription ?? "",
                        "result": String(describing: result)
                    ])
                return
            }
            self.context.window.makeKeyAndOrderFront(nil)
            NSApplication.shared.activate(ignoringOtherApps: true)
            let previousResponder = self.context.window.firstResponder
            let previousResponderView = previousResponder as? NSView
            let webViewOwnsResponder =
                previousResponderView === self.context.webView
                    || previousResponderView?.isDescendant(
                        of: self.context.webView) == true
            if !webViewOwnsResponder,
               !self.context.window.makeFirstResponder(
                    self.context.webView) {
                self.retryOrFail(code: failureCode, details: [
                    "message": "Unable to route keyboard focus to WKWebView",
                    "key": key.name,
                    "previousResponder":
                        String(describing: previousResponder)
                ])
                return
            }
            let currentResponder = self.context.window.firstResponder
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
            trace("recv", "flowInputPopup.nativeKeyboardDispatch", [
                "inputMode": "appkit-wkwebview-key-event",
                "key": key.name,
                "previousResponder":
                    String(describing: previousResponder),
                "currentResponder":
                    String(describing: currentResponder),
                "windowNumber": self.context.window.windowNumber
            ])
            self.context.webView.keyDown(with: keyDown)
            self.context.webView.keyUp(with: keyUp)
            self.phase = next
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                self.poll()
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
          const marker = window.VaadinDesktopFlowInputPopup || {};
          window.VaadinDesktopFlowInputPopup = marker;
          const expectedTargetId = String(prepared.targetId || '');
          const expectedDocumentBackground =
            prepared.expectedDocumentBackground === true;
          const nativePointerStage =
            String(marker.nativePointerStage || '');
          marker.nativePointerCurrentObserved = false;
          document.addEventListener('click', (event) => {
            const path = typeof event.composedPath === 'function'
              ? event.composedPath()
              : [];
            const expectedTarget = expectedDocumentBackground
              ? event.target === document.documentElement
                || event.target === document.body
              : path.some((element) => element?.id === expectedTargetId);
            if (!expectedTarget || Number(event.button) !== 0) return;
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
            marker.nativePointerLastButton = Number(event.button);
          }, { capture: true, once: true });
          return JSON.stringify({
            centerX: Number(prepared.centerX),
            centerY: Number(prepared.centerY),
            targetMatched: prepared.targetMatched === true,
            hitTarget: String(prepared.hitTarget || '')
          });
        })()
        """
        context.webView.evaluateJavaScript(
            instrumentedScript
        ) { result, error in
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
            guard geometry.targetMatched else {
                self.retryOrFail(code: failureCode, details: [
                    "message": "Outside-click background target mismatch",
                    "webViewX": geometry.centerX,
                    "webViewY": geometry.centerY,
                    "hitTarget": geometry.hitTarget
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
                            "Outside-click target is outside the host window",
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
                trace("recv", "flowInputPopup.nativePointerDispatch", [
                    "inputMode": "appkit-window-pointer-event",
                    "button": "primary",
                    "webViewX": geometry.centerX,
                    "webViewY": geometry.centerY,
                    "windowX": windowPoint.x,
                    "windowY": windowPoint.y,
                    "windowNumber": self.context.window.windowNumber
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
        let localizedTimeOnly = scope == .localizedTime
        let complete = localizedTimeOnly
            ? snapshot.localizedTimeComplete
            : snapshot.complete
        let minimumOpaqueBridgeCount = localizedTimeOnly ? 3 : 6
        var payload = snapshot.tracePayload
        payload.merge([
            "complete": complete,
            "standardBundleMode": snapshot.firstMeaningfulPaint,
            "rootRendered": snapshot.rootRendered,
            "flowClientOwned": snapshot.flowClientCount > 0,
            "opaqueRequestBridgeObserved":
                snapshot.requestCount >= minimumOpaqueBridgeCount,
            "opaqueResponseBridgeObserved":
                snapshot.responseCount >= minimumOpaqueBridgeCount,
            "pushObserved": snapshot.pushMessageCount >= 1,
            "errorFree": snapshot.errorFree,
            "flowInputPopupBrowserProgrammaticInputObserved": true,
            "flowInputPopupLocalizedTimeOnly": localizedTimeOnly
        ]) { _, replacement in replacement }
        trace(
            "recv",
            localizedTimeOnly
                ? "flowInputPopup.localizedTimeAcceptance"
                : "flowInputPopup.frameworkAcceptance",
            payload)
        context.window.close()
    }

    private func fail(code: String, details: [String: Any] = [:]) {
        var payload = details
        payload["code"] = code
        payload["phase"] = String(describing: phase)
        trace("send", "error", payload)
        context.window.close()
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
        let comboOpenedObserved: Bool
        let comboA11yObserved: Bool
        let comboOwnerFocusObserved: Bool
        let comboActiveDescendantObserved: Bool
        let comboSelectionObserved: Bool
        let comboJavaValueObserved: Bool
        let comboClosedObserved: Bool
        let comboFocusRestoredObserved: Bool
        let comboCollapsedA11yObserved: Bool
        let selectOpenedObserved: Bool
        let selectA11yObserved: Bool
        let selectInitialFocusObserved: Bool
        let selectKeyboardMoveObserved: Bool
        let selectSelectionObserved: Bool
        let selectJavaValueObserved: Bool
        let selectClosedObserved: Bool
        let selectFocusRestoredObserved: Bool
        let selectCollapsedA11yObserved: Bool
        let selectEscapeOpenedObserved: Bool
        let selectEscapeInitialFocusObserved: Bool
        let selectEscapeCloseObserved: Bool
        let selectValueRetainedObserved: Bool
        let selectPointerOpenedObserved: Bool
        let selectPointerCloseObserved: Bool
        let selectPointerValueRetainedObserved: Bool
        let selectPointerFocusRestoredObserved: Bool
        let selectPointerJavaLifecycleObserved: Bool
        let selectPointerUnderlyingClickSuppressedObserved: Bool
        let selectNativeKeyboardObserved: Bool
        let selectMenuFocusPreparedByHarness: Bool
        let dateOpenedObserved: Bool
        let dateA11yObserved: Bool
        let dateCalendarFocusObserved: Bool
        let dateKeyboardMoveObserved: Bool
        let dateSelectionObserved: Bool
        let dateJavaValueObserved: Bool
        let dateClosedObserved: Bool
        let dateFocusRestoredObserved: Bool
        let dateEscapeOpenedObserved: Bool
        let dateEscapeCloseObserved: Bool
        let dateValueRetainedObserved: Bool
        let dateJavaLifecycleObserved: Bool
        let datePointerOpenedObserved: Bool
        let datePointerFocusObserved: Bool
        let datePointerCloseObserved: Bool
        let datePointerValueRetainedObserved: Bool
        let datePointerFocusRestoredObserved: Bool
        let datePointerUnderlyingClickSuppressedObserved: Bool
        let datePointerJavaLifecycleObserved: Bool
        let timeOpenedObserved: Bool
        let timeA11yObserved: Bool
        let timeInitialOwnerFocusObserved: Bool
        let timeLocaleFormatObserved: Bool
        let timeFirstOptionFocusObserved: Bool
        let timeSecondOptionFocusObserved: Bool
        let timeSelectedOptionFocusObserved: Bool
        let timeKeyboardMoveObserved: Bool
        let timeSelectionObserved: Bool
        let timeJavaValueObserved: Bool
        let timeClosedObserved: Bool
        let timeFocusRestoredObserved: Bool
        let timeNativeKeyboardObserved: Bool
        let nativeKeyboardObserved: Bool
        let nativeKeyboardCurrentObserved: Bool
        let nativeKeyboardExpected: String
        let nativeKeyboardLastKey: String
        let nativeKeyboardLastTarget: String
        let nativeKeyboardEventCount: Int
        let nativeKeyboardStages: String
        let nativePointerObserved: Bool
        let nativePointerCurrentObserved: Bool
        let nativePointerTrustedObserved: Bool
        let nativePointerEventCount: Int
        let nativePointerTrustedEventCount: Int
        let nativePointerStages: String
        let nativePointerLastTarget: String
        let nativePointerLastButton: Int
        let comboOpenedCurrent: Bool
        let selectOpenedCurrent: Bool
        let dateOpenedCurrent: Bool
        let timeOpenedCurrent: Bool
        let comboInputRole: String
        let comboInputExpanded: String
        let comboInputControls: String
        let comboActiveDescendant: String
        let comboActiveOptionText: String
        let comboValue: String
        let selectButtonRole: String
        let selectButtonHasPopup: String
        let selectButtonExpanded: String
        let selectListBoxRole: String
        let selectFocusedText: String
        let selectSelectedText: String
        let selectValue: String
        let dateInputRole: String
        let dateInputHasPopup: String
        let dateInputExpanded: String
        let dateOverlayContentRole: String
        let dateFocusedValue: String
        let dateValue: String
        let timeInputRole: String
        let timeInputExpanded: String
        let timeInputControls: String
        let timeScrollerRole: String
        let timeFocusedText: String
        let timeSelectedText: String
        let timeValue: String
        let timeInputValue: String
        let comboValueStatus: String
        let selectValueStatus: String
        let selectLifecycleStatus: String
        let dateValueStatus: String
        let dateLifecycleStatus: String
        let timeValueStatus: String
        let outsideActionStatus: String
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
                && comboOpenedObserved
                && comboA11yObserved
                && comboOwnerFocusObserved
                && comboActiveDescendantObserved
                && comboSelectionObserved
                && comboJavaValueObserved
                && comboClosedObserved
                && comboFocusRestoredObserved
                && comboCollapsedA11yObserved
                && selectOpenedObserved
                && selectA11yObserved
                && selectInitialFocusObserved
                && selectKeyboardMoveObserved
                && selectSelectionObserved
                && selectJavaValueObserved
                && selectClosedObserved
                && selectFocusRestoredObserved
                && selectCollapsedA11yObserved
                && selectEscapeOpenedObserved
                && selectEscapeInitialFocusObserved
                && selectEscapeCloseObserved
                && selectValueRetainedObserved
                && selectPointerOpenedObserved
                && selectPointerCloseObserved
                && selectPointerValueRetainedObserved
                && selectPointerFocusRestoredObserved
                && selectPointerJavaLifecycleObserved
                && selectPointerUnderlyingClickSuppressedObserved
                && selectNativeKeyboardObserved
                && dateOpenedObserved
                && dateA11yObserved
                && dateCalendarFocusObserved
                && dateKeyboardMoveObserved
                && dateSelectionObserved
                && dateJavaValueObserved
                && dateClosedObserved
                && dateFocusRestoredObserved
                && dateEscapeOpenedObserved
                && dateEscapeCloseObserved
                && dateValueRetainedObserved
                && dateJavaLifecycleObserved
                && datePointerOpenedObserved
                && datePointerFocusObserved
                && datePointerCloseObserved
                && datePointerValueRetainedObserved
                && datePointerFocusRestoredObserved
                && datePointerUnderlyingClickSuppressedObserved
                && datePointerJavaLifecycleObserved
                && nativeKeyboardObserved
                && nativeKeyboardEventCount >= 12
                && nativePointerObserved
                && nativePointerTrustedObserved
                && nativePointerEventCount >= 2
                && nativePointerTrustedEventCount >= 2
                && requestCount >= 6
                && responseCount >= 6
                && pushMessageCount >= 1
        }

        var localizedTimeComplete: Bool {
            errorFree
                && dedicatedRouteReady
                && timeOpenedObserved
                && timeA11yObserved
                && timeInitialOwnerFocusObserved
                && timeLocaleFormatObserved
                && timeFirstOptionFocusObserved
                && timeSecondOptionFocusObserved
                && timeSelectedOptionFocusObserved
                && timeKeyboardMoveObserved
                && timeSelectionObserved
                && timeJavaValueObserved
                && timeClosedObserved
                && timeFocusRestoredObserved
                && timeNativeKeyboardObserved
                && nativeKeyboardObserved
                && nativeKeyboardEventCount >= 6
                && requestCount >= 3
                && responseCount >= 3
                && pushMessageCount >= 1
        }

        var tracePayload: [String: Any] {
            [
                "flowInputPopupDedicatedRouteObserved": dedicatedRouteReady,
                "flowInputPopupPath": path,
                "flowInputPopupComboOpenedObserved": comboOpenedObserved,
                "flowInputPopupComboA11yObserved": comboA11yObserved,
                "flowInputPopupComboOwnerFocusObserved":
                    comboOwnerFocusObserved,
                "flowInputPopupComboActiveDescendantObserved":
                    comboActiveDescendantObserved,
                "flowInputPopupComboSelectionObserved":
                    comboSelectionObserved,
                "flowInputPopupComboJavaValueObserved":
                    comboJavaValueObserved,
                "flowInputPopupComboClosedObserved": comboClosedObserved,
                "flowInputPopupComboFocusRestoredObserved":
                    comboFocusRestoredObserved,
                "flowInputPopupComboCollapsedA11yObserved":
                    comboCollapsedA11yObserved,
                "flowInputPopupSelectOpenedObserved": selectOpenedObserved,
                "flowInputPopupSelectA11yObserved": selectA11yObserved,
                "flowInputPopupSelectInitialFocusObserved":
                    selectInitialFocusObserved,
                "flowInputPopupSelectKeyboardMoveObserved":
                    selectKeyboardMoveObserved,
                "flowInputPopupSelectSelectionObserved":
                    selectSelectionObserved,
                "flowInputPopupSelectJavaValueObserved":
                    selectJavaValueObserved,
                "flowInputPopupSelectClosedObserved": selectClosedObserved,
                "flowInputPopupSelectFocusRestoredObserved":
                    selectFocusRestoredObserved,
                "flowInputPopupSelectCollapsedA11yObserved":
                    selectCollapsedA11yObserved,
                "flowInputPopupSelectEscapeOpenedObserved":
                    selectEscapeOpenedObserved,
                "flowInputPopupSelectEscapeInitialFocusObserved":
                    selectEscapeInitialFocusObserved,
                "flowInputPopupSelectEscapeCloseObserved":
                    selectEscapeCloseObserved,
                "flowInputPopupSelectValueRetainedObserved":
                    selectValueRetainedObserved,
                "flowInputPopupSelectPointerOpenedObserved":
                    selectPointerOpenedObserved,
                "flowInputPopupSelectPointerCloseObserved":
                    selectPointerCloseObserved,
                "flowInputPopupSelectPointerValueRetainedObserved":
                    selectPointerValueRetainedObserved,
                "flowInputPopupSelectPointerFocusRestoredObserved":
                    selectPointerFocusRestoredObserved,
                "flowInputPopupSelectPointerJavaLifecycleObserved":
                    selectPointerJavaLifecycleObserved,
                "flowInputPopupSelectPointerUnderlyingClickSuppressedObserved":
                    selectPointerUnderlyingClickSuppressedObserved,
                "flowInputPopupSelectNativeKeyboardObserved":
                    selectNativeKeyboardObserved,
                "flowInputPopupSelectMenuFocusPreparedByHarness":
                    selectMenuFocusPreparedByHarness,
                "flowInputPopupDateOpenedObserved": dateOpenedObserved,
                "flowInputPopupDateA11yObserved": dateA11yObserved,
                "flowInputPopupDateCalendarFocusObserved":
                    dateCalendarFocusObserved,
                "flowInputPopupDateKeyboardMoveObserved":
                    dateKeyboardMoveObserved,
                "flowInputPopupDateSelectionObserved": dateSelectionObserved,
                "flowInputPopupDateJavaValueObserved": dateJavaValueObserved,
                "flowInputPopupDateClosedObserved": dateClosedObserved,
                "flowInputPopupDateFocusRestoredObserved":
                    dateFocusRestoredObserved,
                "flowInputPopupDateEscapeOpenedObserved":
                    dateEscapeOpenedObserved,
                "flowInputPopupDateEscapeCloseObserved":
                    dateEscapeCloseObserved,
                "flowInputPopupDateValueRetainedObserved":
                    dateValueRetainedObserved,
                "flowInputPopupDateJavaLifecycleObserved":
                    dateJavaLifecycleObserved,
                "flowInputPopupDatePointerOpenedObserved":
                    datePointerOpenedObserved,
                "flowInputPopupDatePointerFocusObserved":
                    datePointerFocusObserved,
                "flowInputPopupDatePointerCloseObserved":
                    datePointerCloseObserved,
                "flowInputPopupDatePointerValueRetainedObserved":
                    datePointerValueRetainedObserved,
                "flowInputPopupDatePointerFocusRestoredObserved":
                    datePointerFocusRestoredObserved,
                "flowInputPopupDatePointerUnderlyingClickSuppressedObserved":
                    datePointerUnderlyingClickSuppressedObserved,
                "flowInputPopupDatePointerJavaLifecycleObserved":
                    datePointerJavaLifecycleObserved,
                "flowInputPopupTimeOpenedObserved": timeOpenedObserved,
                "flowInputPopupTimeA11yObserved": timeA11yObserved,
                "flowInputPopupTimeInitialOwnerFocusObserved":
                    timeInitialOwnerFocusObserved,
                "flowInputPopupTimeLocaleFormatObserved":
                    timeLocaleFormatObserved,
                "flowInputPopupTimeFirstOptionFocusObserved":
                    timeFirstOptionFocusObserved,
                "flowInputPopupTimeSecondOptionFocusObserved":
                    timeSecondOptionFocusObserved,
                "flowInputPopupTimeSelectedOptionFocusObserved":
                    timeSelectedOptionFocusObserved,
                "flowInputPopupTimeKeyboardMoveObserved":
                    timeKeyboardMoveObserved,
                "flowInputPopupTimeSelectionObserved":
                    timeSelectionObserved,
                "flowInputPopupTimeJavaValueObserved":
                    timeJavaValueObserved,
                "flowInputPopupTimeClosedObserved": timeClosedObserved,
                "flowInputPopupTimeFocusRestoredObserved":
                    timeFocusRestoredObserved,
                "flowInputPopupTimeNativeKeyboardObserved":
                    timeNativeKeyboardObserved,
                "flowInputPopupNativeKeyboardObserved":
                    nativeKeyboardObserved,
                "flowInputPopupNativeKeyboardCurrentObserved":
                    nativeKeyboardCurrentObserved,
                "flowInputPopupNativeKeyboardExpected":
                    nativeKeyboardExpected,
                "flowInputPopupNativeKeyboardLastKey":
                    nativeKeyboardLastKey,
                "flowInputPopupNativeKeyboardLastTarget":
                    nativeKeyboardLastTarget,
                "flowInputPopupNativeKeyboardEventCount":
                    nativeKeyboardEventCount,
                "flowInputPopupNativeKeyboardStages": nativeKeyboardStages,
                "flowInputPopupNativePointerObserved":
                    nativePointerObserved,
                "flowInputPopupNativePointerCurrentObserved":
                    nativePointerCurrentObserved,
                "flowInputPopupNativePointerTrustedObserved":
                    nativePointerTrustedObserved,
                "flowInputPopupNativePointerEventCount":
                    nativePointerEventCount,
                "flowInputPopupNativePointerTrustedEventCount":
                    nativePointerTrustedEventCount,
                "flowInputPopupNativePointerStages": nativePointerStages,
                "flowInputPopupNativePointerLastTarget":
                    nativePointerLastTarget,
                "flowInputPopupNativePointerLastButton":
                    nativePointerLastButton,
                "flowInputPopupComboOpenedCurrent": comboOpenedCurrent,
                "flowInputPopupSelectOpenedCurrent": selectOpenedCurrent,
                "flowInputPopupDateOpenedCurrent": dateOpenedCurrent,
                "flowInputPopupTimeOpenedCurrent": timeOpenedCurrent,
                "flowInputPopupComboInputRole": comboInputRole,
                "flowInputPopupComboInputExpanded": comboInputExpanded,
                "flowInputPopupComboInputControls": comboInputControls,
                "flowInputPopupComboActiveDescendant":
                    comboActiveDescendant,
                "flowInputPopupComboActiveOptionText":
                    comboActiveOptionText,
                "flowInputPopupComboValue": comboValue,
                "flowInputPopupSelectButtonRole": selectButtonRole,
                "flowInputPopupSelectButtonHasPopup": selectButtonHasPopup,
                "flowInputPopupSelectButtonExpanded": selectButtonExpanded,
                "flowInputPopupSelectListBoxRole": selectListBoxRole,
                "flowInputPopupSelectFocusedText": selectFocusedText,
                "flowInputPopupSelectSelectedText": selectSelectedText,
                "flowInputPopupSelectValue": selectValue,
                "flowInputPopupDateInputRole": dateInputRole,
                "flowInputPopupDateInputHasPopup": dateInputHasPopup,
                "flowInputPopupDateInputExpanded": dateInputExpanded,
                "flowInputPopupDateOverlayContentRole":
                    dateOverlayContentRole,
                "flowInputPopupDateFocusedValue": dateFocusedValue,
                "flowInputPopupDateValue": dateValue,
                "flowInputPopupTimeInputRole": timeInputRole,
                "flowInputPopupTimeInputExpanded": timeInputExpanded,
                "flowInputPopupTimeInputControls": timeInputControls,
                "flowInputPopupTimeScrollerRole": timeScrollerRole,
                "flowInputPopupTimeFocusedText": timeFocusedText,
                "flowInputPopupTimeSelectedText": timeSelectedText,
                "flowInputPopupTimeValue": timeValue,
                "flowInputPopupTimeInputValue": timeInputValue,
                "flowInputPopupComboValueStatus": comboValueStatus,
                "flowInputPopupSelectValueStatus": selectValueStatus,
                "flowInputPopupSelectLifecycleStatus": selectLifecycleStatus,
                "flowInputPopupDateValueStatus": dateValueStatus,
                "flowInputPopupDateLifecycleStatus": dateLifecycleStatus,
                "flowInputPopupTimeValueStatus": timeValueStatus,
                "flowInputPopupOutsideActionStatus": outsideActionStatus,
                "flowInputPopupActivePath": activePath,
                "flowInputPopupMarkerError": markerError,
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

    private struct PointerGeometry: Decodable {
        let centerX: Double
        let centerY: Double
        let targetMatched: Bool
        let hitTarget: String
    }

    private enum NativeKey {
        case arrowDown
        case arrowRight
        case returnKey
        case escape

        var name: String {
            switch self {
            case .arrowDown:
                return "ArrowDown"
            case .arrowRight:
                return "ArrowRight"
            case .returnKey:
                return "Enter"
            case .escape:
                return "Escape"
            }
        }

        private var characters: String {
            switch self {
            case .arrowDown:
                return "\u{f701}"
            case .arrowRight:
                return "\u{f703}"
            case .returnKey:
                return "\r"
            case .escape:
                return "\u{1b}"
            }
        }

        private var keyCode: UInt16 {
            switch self {
            case .arrowDown:
                return 125
            case .arrowRight:
                return 124
            case .returnKey:
                return 36
            case .escape:
                return 53
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

    private static let navigationScript = """
    (() => {
      const trigger =
        document.querySelector('#framework-flow-input-popup-open-route');
      if (!trigger || typeof trigger.click !== 'function') return false;
      trigger.click();
      return true;
    })()
    """

    private static let openComboScript = """
    (() => {
      const combo =
        document.querySelector('#framework-flow-input-popup-combo');
      const input = combo?.inputElement
        || combo?.shadowRoot?.querySelector('input');
      if (!combo || !input) return false;
      window.VaadinDesktopFlowInputPopup = {
        comboOpenDispatched: true,
        comboOpenedObserved: false,
        comboA11yObserved: false,
        comboOwnerFocusObserved: false,
        comboNavigationDispatched: false,
        comboActiveDescendantObserved: false,
        comboSelectionDispatched: false,
        comboSelectionObserved: false,
        comboJavaValueObserved: false,
        comboClosedObserved: false,
        comboFocusRestoredObserved: false,
        comboCollapsedA11yObserved: false,
        selectOpenDispatched: false,
        selectOpenedObserved: false,
        selectA11yObserved: false,
        selectInitialFocusObserved: false,
        selectMoveDispatched: false,
        selectKeyboardMoveObserved: false,
        selectSelectionDispatched: false,
        selectSelectionObserved: false,
        selectJavaValueObserved: false,
        selectClosedObserved: false,
        selectFocusRestoredObserved: false,
        selectCollapsedA11yObserved: false,
        selectEscapeOpenDispatched: false,
        selectEscapeOpenedObserved: false,
        selectEscapeInitialFocusObserved: false,
        selectEscapeDispatched: false,
        selectEscapeCloseObserved: false,
        selectValueRetainedObserved: false,
        selectPointerOpenDispatched: false,
        selectPointerOpenedObserved: false,
        selectPointerCloseDispatched: false,
        selectPointerCloseObserved: false,
        selectPointerValueRetainedObserved: false,
        selectPointerFocusRestoredObserved: false,
        selectPointerJavaLifecycleObserved: false,
        selectPointerUnderlyingClickSuppressedObserved: false,
        selectNativeKeyboardObserved: false,
        dateOpenDispatched: false,
        dateOpenedObserved: false,
        dateA11yObserved: false,
        dateCalendarFocusObserved: false,
        dateMoveDispatched: false,
        dateKeyboardMoveObserved: false,
        dateSelectionDispatched: false,
        dateSelectionObserved: false,
        dateJavaValueObserved: false,
        dateClosedObserved: false,
        dateFocusRestoredObserved: false,
        dateEscapeOpenDispatched: false,
        dateEscapeOpenedObserved: false,
        dateEscapeDispatched: false,
        dateEscapeCloseObserved: false,
        dateValueRetainedObserved: false,
        dateJavaLifecycleObserved: false,
        datePointerOpenDispatched: false,
        datePointerOpenedObserved: false,
        datePointerFocusObserved: false,
        datePointerCloseDispatched: false,
        datePointerCloseObserved: false,
        datePointerValueRetainedObserved: false,
        datePointerFocusRestoredObserved: false,
        datePointerUnderlyingClickSuppressedObserved: false,
        datePointerJavaLifecycleObserved: false,
        nativeKeyboardObserved: false,
        nativeKeyboardCurrentObserved: false,
        nativeKeyboardExpected: '',
        nativeKeyboardLastKey: '',
        nativeKeyboardLastTarget: '',
        nativeKeyboardEventCount: 0,
        nativeKeyboardStages: '',
        nativeKeyboardStage: '',
        nativePointerObserved: false,
        nativePointerCurrentObserved: false,
        nativePointerTrustedObserved: false,
        nativePointerEventCount: 0,
        nativePointerTrustedEventCount: 0,
        nativePointerStages: '',
        nativePointerStage: '',
        nativePointerLastTarget: '',
        nativePointerLastButton: -1,
        error: ''
      };
      combo.scrollIntoView({ block: 'center', inline: 'center' });
      input.focus({ preventScroll: true });
      input.dispatchEvent(new KeyboardEvent('keydown', {
        key: 'ArrowDown',
        code: 'ArrowDown',
        keyCode: 40,
        which: 40,
        bubbles: true,
        composed: true,
        cancelable: true
      }));
      return true;
    })()
    """

    private static let focusComboOptionScript = """
    (() => {
      const marker = window.VaadinDesktopFlowInputPopup || {};
      window.VaadinDesktopFlowInputPopup = marker;
      const combo =
        document.querySelector('#framework-flow-input-popup-combo');
      const input = combo?.inputElement
        || combo?.shadowRoot?.querySelector('input');
      if (!combo || combo.opened !== true || !input) return false;
      marker.comboNavigationDispatched = true;
      input.dispatchEvent(new KeyboardEvent('keydown', {
        key: 'ArrowDown',
        code: 'ArrowDown',
        keyCode: 40,
        which: 40,
        bubbles: true,
        composed: true,
        cancelable: true
      }));
      return true;
    })()
    """

    private static let selectComboOptionScript = """
    (() => {
      const marker = window.VaadinDesktopFlowInputPopup || {};
      window.VaadinDesktopFlowInputPopup = marker;
      const combo =
        document.querySelector('#framework-flow-input-popup-combo');
      const input = combo?.inputElement
        || combo?.shadowRoot?.querySelector('input');
      if (!combo || combo.opened !== true || !input) return false;
      marker.comboSelectionDispatched = true;
      input.dispatchEvent(new KeyboardEvent('keydown', {
        key: 'Enter',
        code: 'Enter',
        keyCode: 13,
        which: 13,
        bubbles: true,
        composed: true,
        cancelable: true
      }));
      return true;
    })()
    """

    private static let focusSelectScript = """
    (() => {
      const marker = window.VaadinDesktopFlowInputPopup || {};
      window.VaadinDesktopFlowInputPopup = marker;
      const select =
        document.querySelector('#framework-flow-input-popup-select');
      const button = select?.focusElement
        || select?.shadowRoot?.querySelector('vaadin-select-value-button');
      if (!select || select.opened === true || !button) return false;
      marker.selectOpenDispatched = true;
      marker.nativeKeyboardStage = 'select-open';
      select.scrollIntoView({ block: 'center', inline: 'center' });
      button.focus({ preventScroll: true });
      return true;
    })()
    """

    private static let moveSelectScript = """
    (() => {
      const marker = window.VaadinDesktopFlowInputPopup || {};
      window.VaadinDesktopFlowInputPopup = marker;
      const select =
        document.querySelector('#framework-flow-input-popup-select');
      const listBox = select?.querySelector('vaadin-select-list-box');
      if (!select || select.opened !== true || !listBox) return false;
      const items = Array.from(listBox.items || listBox.children || []);
      if (!items.some((item) => item.focused === true)) {
        listBox.focus({ preventScroll: true });
        marker.selectMenuFocusPreparedByHarness = true;
      }
      marker.selectMoveDispatched = true;
      marker.nativeKeyboardStage = 'select-move';
      return true;
    })()
    """

    private static let selectSelectScript = """
    (() => {
      const marker = window.VaadinDesktopFlowInputPopup || {};
      window.VaadinDesktopFlowInputPopup = marker;
      const select =
        document.querySelector('#framework-flow-input-popup-select');
      if (!select || select.opened !== true) return false;
      marker.selectSelectionDispatched = true;
      marker.nativeKeyboardStage = 'select-select';
      return true;
    })()
    """

    private static let reopenSelectScript = """
    (() => {
      const marker = window.VaadinDesktopFlowInputPopup || {};
      window.VaadinDesktopFlowInputPopup = marker;
      const select =
        document.querySelector('#framework-flow-input-popup-select');
      const button = select?.focusElement
        || select?.shadowRoot?.querySelector('vaadin-select-value-button');
      if (!select || select.opened === true || !button) return false;
      marker.selectEscapeOpenDispatched = true;
      marker.selectA11yObserved = false;
      marker.nativeKeyboardStage = 'select-escape-open';
      button.focus({ preventScroll: true });
      return true;
    })()
    """

    private static let closeSelectWithEscapeScript = """
    (() => {
      const marker = window.VaadinDesktopFlowInputPopup || {};
      window.VaadinDesktopFlowInputPopup = marker;
      const select =
        document.querySelector('#framework-flow-input-popup-select');
      if (!select || select.opened !== true) return false;
      marker.selectEscapeDispatched = true;
      marker.nativeKeyboardStage = 'select-escape-close';
      return true;
    })()
    """

    private static let reopenSelectForPointerScript = """
    (() => {
      const marker = window.VaadinDesktopFlowInputPopup || {};
      window.VaadinDesktopFlowInputPopup = marker;
      const select =
        document.querySelector('#framework-flow-input-popup-select');
      const button = select?.focusElement
        || select?.shadowRoot?.querySelector('vaadin-select-value-button');
      if (!select || select.opened === true || !button) return false;
      marker.selectPointerOpenDispatched = true;
      marker.nativeKeyboardStage = 'select-pointer-open';
      button.focus({ preventScroll: true });
      return true;
    })()
    """

    private static let prepareSelectOutsidePointerScript = """
    (() => {
      const marker = window.VaadinDesktopFlowInputPopup || {};
      window.VaadinDesktopFlowInputPopup = marker;
      const select =
        document.querySelector('#framework-flow-input-popup-select');
      const outside = document.querySelector(
        '#framework-flow-input-popup-outside-target');
      if (!select || select.opened !== true || !outside) return null;
      const anchors = [
        { top: '16px', right: '16px' },
        { top: '16px', left: '16px' },
        { bottom: '16px', right: '16px' },
        { bottom: '16px', left: '16px' }
      ];
      let rect;
      let centerX = 0;
      let centerY = 0;
      let hit = null;
      for (const anchor of anchors) {
        Object.assign(outside.style, {
          position: 'fixed',
          top: '',
          right: '',
          bottom: '',
          left: '',
          ...anchor
        });
        rect = outside.getBoundingClientRect();
        centerX = rect.left + rect.width / 2;
        centerY = rect.top + rect.height / 2;
        hit = document.elementFromPoint(centerX, centerY);
        if (hit === document.documentElement || hit === document.body) break;
      }
      const targetMatched = rect.width > 0 && rect.height > 0
        && centerX >= 0 && centerX <= window.innerWidth
        && centerY >= 0 && centerY <= window.innerHeight
        && (hit === document.documentElement || hit === document.body);
      marker.selectPointerCloseDispatched = true;
      marker.nativePointerStage = 'select-outside-close';
      return {
        centerX,
        centerY,
        targetId: outside.id,
        expectedDocumentBackground: true,
        targetMatched,
        hitTarget: String(hit?.id || hit?.localName || '')
      };
    })()
    """

    private static let openDateScript = """
    (() => {
      const marker = window.VaadinDesktopFlowInputPopup || {};
      window.VaadinDesktopFlowInputPopup = marker;
      const picker =
        document.querySelector('#framework-flow-input-popup-date');
      const input = picker?.inputElement
        || picker?.shadowRoot?.querySelector('input');
      if (!picker || !input) return false;
      marker.dateOpenDispatched = true;
      picker.scrollIntoView({ block: 'center', inline: 'center' });
      input.focus({ preventScroll: true });
      input.dispatchEvent(new KeyboardEvent('keydown', {
        key: 'ArrowDown',
        code: 'ArrowDown',
        keyCode: 40,
        which: 40,
        bubbles: true,
        composed: true,
        cancelable: true
      }));
      return true;
    })()
    """

    private static let moveDateScript = """
    (() => {
      const marker = window.VaadinDesktopFlowInputPopup || {};
      window.VaadinDesktopFlowInputPopup = marker;
      const picker =
        document.querySelector('#framework-flow-input-popup-date');
      if (!picker || picker.opened !== true) return false;
      marker.dateMoveDispatched = true;
      marker.nativeKeyboardStage = 'date-move';
      return true;
    })()
    """

    private static let focusDateScript = """
    (() => {
      const marker = window.VaadinDesktopFlowInputPopup || {};
      window.VaadinDesktopFlowInputPopup = marker;
      const picker =
        document.querySelector('#framework-flow-input-popup-date');
      const input = picker?.inputElement
        || picker?.shadowRoot?.querySelector('input');
      if (!picker || picker.opened !== true || !input) return false;
      marker.nativeKeyboardStage = marker.dateEscapeOpenDispatched === true
        ? 'date-escape-focus'
        : 'date-focus';
      input.focus({ preventScroll: true });
      return true;
    })()
    """

    private static let selectDateScript = """
    (() => {
      const marker = window.VaadinDesktopFlowInputPopup || {};
      window.VaadinDesktopFlowInputPopup = marker;
      const picker =
        document.querySelector('#framework-flow-input-popup-date');
      if (!picker || picker.opened !== true) return false;
      marker.dateSelectionDispatched = true;
      marker.nativeKeyboardStage = 'date-select';
      return true;
    })()
    """

    private static let reopenDateScript = """
    (() => {
      const marker = window.VaadinDesktopFlowInputPopup || {};
      window.VaadinDesktopFlowInputPopup = marker;
      const picker =
        document.querySelector('#framework-flow-input-popup-date');
      const input = picker?.inputElement
        || picker?.shadowRoot?.querySelector('input');
      if (!picker || picker.opened === true || !input) return false;
      marker.dateEscapeOpenDispatched = true;
      marker.dateA11yObserved = false;
      marker.dateCalendarFocusObserved = false;
      input.focus({ preventScroll: true });
      input.dispatchEvent(new KeyboardEvent('keydown', {
        key: 'ArrowDown',
        code: 'ArrowDown',
        keyCode: 40,
        which: 40,
        bubbles: true,
        composed: true,
        cancelable: true
      }));
      return true;
    })()
    """

    private static let closeDateWithEscapeScript = """
    (() => {
      const marker = window.VaadinDesktopFlowInputPopup || {};
      window.VaadinDesktopFlowInputPopup = marker;
      const picker =
        document.querySelector('#framework-flow-input-popup-date');
      if (!picker || picker.opened !== true) return false;
      marker.dateEscapeDispatched = true;
      marker.nativeKeyboardStage = 'date-escape-close';
      return true;
    })()
    """

    private static let reopenDateForPointerScript = """
    (() => {
      const marker = window.VaadinDesktopFlowInputPopup || {};
      window.VaadinDesktopFlowInputPopup = marker;
      const picker =
        document.querySelector('#framework-flow-input-popup-date');
      const input = picker?.inputElement
        || picker?.shadowRoot?.querySelector('input');
      if (!picker || picker.opened === true || !input) return false;
      marker.datePointerOpenDispatched = true;
      input.focus({ preventScroll: true });
      input.dispatchEvent(new KeyboardEvent('keydown', {
        key: 'ArrowDown',
        code: 'ArrowDown',
        keyCode: 40,
        which: 40,
        bubbles: true,
        composed: true,
        cancelable: true
      }));
      return true;
    })()
    """

    private static let focusDateForPointerScript = """
    (() => {
      const marker = window.VaadinDesktopFlowInputPopup || {};
      window.VaadinDesktopFlowInputPopup = marker;
      const picker =
        document.querySelector('#framework-flow-input-popup-date');
      const input = picker?.inputElement
        || picker?.shadowRoot?.querySelector('input');
      if (!picker || picker.opened !== true || !input) return false;
      marker.nativeKeyboardStage = 'date-pointer-focus';
      input.focus({ preventScroll: true });
      return true;
    })()
    """

    private static let prepareDateOutsidePointerScript = """
    (() => {
      const marker = window.VaadinDesktopFlowInputPopup || {};
      window.VaadinDesktopFlowInputPopup = marker;
      const picker =
        document.querySelector('#framework-flow-input-popup-date');
      const outside = document.querySelector(
        '#framework-flow-input-popup-outside-target');
      if (!picker || picker.opened !== true || !outside) return null;
      const anchors = [
        { top: '16px', right: '16px' },
        { top: '16px', left: '16px' },
        { bottom: '16px', right: '16px' },
        { bottom: '16px', left: '16px' }
      ];
      let rect;
      let centerX = 0;
      let centerY = 0;
      let hit = null;
      for (const anchor of anchors) {
        Object.assign(outside.style, {
          position: 'fixed',
          top: '',
          right: '',
          bottom: '',
          left: '',
          ...anchor
        });
        rect = outside.getBoundingClientRect();
        centerX = rect.left + rect.width / 2;
        centerY = rect.top + rect.height / 2;
        hit = document.elementFromPoint(centerX, centerY);
        if (hit === document.documentElement || hit === document.body) break;
      }
      const targetMatched = rect.width > 0 && rect.height > 0
        && centerX >= 0 && centerX <= window.innerWidth
        && centerY >= 0 && centerY <= window.innerHeight
        && (hit === document.documentElement || hit === document.body);
      marker.datePointerCloseDispatched = true;
      marker.nativePointerStage = 'date-outside-close';
      return {
        centerX,
        centerY,
        targetId: outside.id,
        expectedDocumentBackground: true,
        targetMatched,
        hitTarget: String(hit?.id || hit?.localName || '')
      };
    })()
    """

    private static let focusTimeScript = """
    (() => {
      const marker = window.VaadinDesktopFlowInputPopup || {};
      window.VaadinDesktopFlowInputPopup = marker;
      const picker =
        document.querySelector('#framework-flow-input-popup-time');
      const input = picker?.inputElement
        || picker?.shadowRoot?.querySelector('input');
      if (!picker || picker.opened === true || !input) return false;
      marker.timeOpenDispatched = true;
      marker.nativeKeyboardStage = 'time-open';
      picker.scrollIntoView({ block: 'center', inline: 'center' });
      input.focus({ preventScroll: true });
      return true;
    })()
    """

    private static let moveTimeScript = """
    (() => {
      const marker = window.VaadinDesktopFlowInputPopup || {};
      window.VaadinDesktopFlowInputPopup = marker;
      const picker =
        document.querySelector('#framework-flow-input-popup-time');
      const input = picker?.inputElement
        || picker?.shadowRoot?.querySelector('input');
      if (!picker || picker.opened !== true || !input) return false;
      marker.timeMoveDispatched = true;
      marker.timeMoveCount = Number(marker.timeMoveCount || 0) + 1;
      marker.nativeKeyboardStage = 'time-move';
      input.focus({ preventScroll: true });
      return true;
    })()
    """

    private static let selectTimeScript = """
    (() => {
      const marker = window.VaadinDesktopFlowInputPopup || {};
      window.VaadinDesktopFlowInputPopup = marker;
      const picker =
        document.querySelector('#framework-flow-input-popup-time');
      if (!picker || picker.opened !== true) return false;
      marker.timeSelectionDispatched = true;
      marker.nativeKeyboardStage = 'time-select';
      return true;
    })()
    """

    private static let inspectionScript = """
    (() => {
      const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
      const marker = window.VaadinDesktopFlowInputPopup || {};
      window.VaadinDesktopFlowInputPopup = marker;
      const activeDeepElements = () => {
        const elements = [];
        let current = document.activeElement;
        while (current) {
          elements.push(current);
          current = current.shadowRoot?.activeElement || null;
        }
        return elements;
      };
      const findDeep = (predicate) => {
        const visit = (node) => {
          if (!node) return null;
          if (node.nodeType === Node.ELEMENT_NODE && predicate(node)) {
            return node;
          }
          if (node.shadowRoot) {
            const shadowResult = visit(node.shadowRoot);
            if (shadowResult) return shadowResult;
          }
          if (typeof node.assignedNodes === 'function') {
            for (const child of node.assignedNodes({ flatten: true })) {
              const result = visit(child);
              if (result) return result;
            }
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
      const collectDeepText = (rootNode) => {
        const parts = [];
        const visit = (node) => {
          if (!node) return;
          if (node.nodeType === Node.TEXT_NODE) {
            const text = String(node.textContent || '')
              .replace(/\\s+/g, ' ').trim();
            if (text) parts.push(text);
            return;
          }
          if (node.nodeType !== Node.ELEMENT_NODE
              && node.nodeType !== Node.DOCUMENT_FRAGMENT_NODE
              && node.nodeType !== Node.DOCUMENT_NODE) {
            return;
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
      const formatDate = (value) => {
        if (!(value instanceof Date) || Number.isNaN(value.getTime())) {
          return '';
        }
        const year = String(value.getFullYear()).padStart(4, '0');
        const month = String(value.getMonth() + 1).padStart(2, '0');
        const day = String(value.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
      };
      const activePath = activeDeepElements();
      const activeWithin = (rootNode) =>
        !!rootNode && activePath.some((element) =>
          element === rootNode
            || rootNode.contains?.(element)
            || rootNode.shadowRoot?.contains?.(element));
      const outsideTarget = document.querySelector(
        '#framework-flow-input-popup-outside-target');
      const outsideActionStatus = document.querySelector(
        '#framework-flow-input-popup-outside-status')?.textContent || '';

      const combo =
        document.querySelector('#framework-flow-input-popup-combo');
      const comboInput = combo?.inputElement
        || combo?.shadowRoot?.querySelector('input');
      const comboOverlay =
        combo?.shadowRoot?.querySelector('vaadin-combo-box-overlay');
      const comboScroller =
        combo?.querySelector('vaadin-combo-box-scroller');
      const comboActiveDescendant =
        comboInput?.getAttribute?.('aria-activedescendant') || '';
      const comboActiveOption = comboActiveDescendant
        ? findDeep((element) => element.id === comboActiveDescendant)
        : null;
      const comboOpenedCurrent =
        combo?.opened === true
          && (comboOverlay?.opened === true
            || comboOverlay?.hasAttribute?.('opened') === true);
      const comboA11yCurrent =
        comboOpenedCurrent
          && comboInput?.getAttribute?.('role') === 'combobox'
          && comboInput?.getAttribute?.('aria-autocomplete') === 'list'
          && comboInput?.getAttribute?.('aria-expanded') === 'true'
          && !!comboScroller?.id
          && comboInput?.getAttribute?.('aria-controls') === comboScroller.id
          && String(comboInput?.getAttribute?.('aria-label') || '')
            .includes('Flow IPC popup reviewer');
      const comboOwnerFocusCurrent =
        comboOpenedCurrent && activeWithin(comboInput);
      const comboActiveOptionCurrent =
        comboOpenedCurrent
          && !!comboActiveDescendant
          && !!comboActiveOption
          && comboActiveOption.getAttribute?.('role') === 'option'
          && collectDeepText(comboActiveOption).includes('platform');
      const comboValueStatus =
        document.querySelector(
          '#framework-flow-input-popup-combo-value-status')
          ?.textContent || '';
      const comboCollapsedA11yCurrent =
        !comboOpenedCurrent
          && comboInput?.getAttribute?.('aria-expanded') === 'false'
          && !comboInput?.hasAttribute?.('aria-controls');

      marker.comboOpenedObserved =
        marker.comboOpenedObserved === true
          || (marker.comboOpenDispatched === true && comboOpenedCurrent);
      marker.comboA11yObserved =
        marker.comboA11yObserved === true || comboA11yCurrent;
      marker.comboOwnerFocusObserved =
        marker.comboOwnerFocusObserved === true || comboOwnerFocusCurrent;
      marker.comboActiveDescendantObserved =
        marker.comboActiveDescendantObserved === true
          || (marker.comboNavigationDispatched === true
            && comboActiveOptionCurrent);
      marker.comboSelectionObserved =
        marker.comboSelectionObserved === true
          || (marker.comboSelectionDispatched === true
            && comboValueStatus.includes(
              'ComboBox value: platform client=true change=1'));
      marker.comboJavaValueObserved =
        marker.comboJavaValueObserved === true
          || comboValueStatus.includes(
            'ComboBox value: platform client=true change=1');
      marker.comboClosedObserved =
        marker.comboClosedObserved === true
          || (marker.comboSelectionDispatched === true
            && !comboOpenedCurrent);
      marker.comboFocusRestoredObserved =
        marker.comboFocusRestoredObserved === true
          || (marker.comboSelectionDispatched === true
            && !comboOpenedCurrent
            && activeWithin(comboInput));
      marker.comboCollapsedA11yObserved =
        marker.comboCollapsedA11yObserved === true
          || (marker.comboSelectionDispatched === true
            && comboCollapsedA11yCurrent);

      const select =
        document.querySelector('#framework-flow-input-popup-select');
      const selectButton = select?.focusElement
        || select?.shadowRoot?.querySelector('vaadin-select-value-button');
      const selectOverlay =
        select?.shadowRoot?.querySelector('vaadin-select-overlay');
      const selectListBox = findDeep((element) =>
        element.localName === 'vaadin-select-list-box');
      const selectItems = Array.from(
        selectListBox?.items || selectListBox?.children || []);
      const selectFocusedItem = selectItems.find((item) =>
        item.focused === true
          || item.hasAttribute?.('focused')
          || activeWithin(item)) || null;
      const selectSelectedItem = selectItems.find((item) =>
        item.selected === true || item.hasAttribute?.('selected')) || null;
      const selectFocusedText = selectFocusedItem
        ? collectDeepText(selectFocusedItem)
        : '';
      const selectSelectedText = selectSelectedItem
        ? collectDeepText(selectSelectedItem)
        : '';
      const selectValueStatus =
        document.querySelector(
          '#framework-flow-input-popup-select-value-status')
          ?.textContent || '';
      const selectLifecycleStatus =
        document.querySelector(
          '#framework-flow-input-popup-select-lifecycle-status')
          ?.textContent || '';
      const selectOpenedCurrent =
        select?.opened === true
          && (selectOverlay?.opened === true
            || selectOverlay?.hasAttribute?.('opened') === true);
      const selectA11yCurrent =
        selectOpenedCurrent
          && selectButton?.getAttribute?.('role') === 'button'
          && selectButton?.getAttribute?.('aria-haspopup') === 'listbox'
          && selectButton?.getAttribute?.('aria-expanded') === 'true'
          && selectListBox?.getAttribute?.('role') === 'listbox'
          && selectItems.length === 3
          && selectItems.every((item) =>
            item.getAttribute?.('role') === 'option');
      const selectCollapsedA11yCurrent =
        !selectOpenedCurrent
          && selectButton?.getAttribute?.('aria-haspopup') === 'listbox'
          && selectButton?.getAttribute?.('aria-expanded') === 'false';

      marker.selectOpenedObserved =
        marker.selectOpenedObserved === true
          || (marker.selectOpenDispatched === true
            && selectOpenedCurrent);
      marker.selectA11yObserved =
        marker.selectA11yObserved === true || selectA11yCurrent;
      marker.selectInitialFocusObserved =
        marker.selectInitialFocusObserved === true
          || (marker.selectOpenDispatched === true
            && marker.selectMoveDispatched !== true
            && selectOpenedCurrent
            && selectSelectedText.includes('standard')
            && (selectFocusedText.includes('standard')
              || activeWithin(selectButton)));
      marker.selectKeyboardMoveObserved =
        marker.selectKeyboardMoveObserved === true
          || (marker.selectMoveDispatched === true
            && selectOpenedCurrent
            && selectFocusedText.includes('audit')
            && selectSelectedText.includes('standard')
            && selectValueStatus.includes('Select value: standard'));
      marker.selectSelectionObserved =
        marker.selectSelectionObserved === true
          || (marker.selectSelectionDispatched === true
            && !selectOpenedCurrent
            && selectSelectedText.includes('audit')
            && selectValueStatus.includes(
              'Select value: audit client=true change=1'));
      marker.selectJavaValueObserved =
        marker.selectJavaValueObserved === true
          || selectValueStatus.includes(
            'Select value: audit client=true change=1');
      marker.selectClosedObserved =
        marker.selectClosedObserved === true
          || (marker.selectSelectionDispatched === true
            && !selectOpenedCurrent);
      marker.selectFocusRestoredObserved =
        marker.selectFocusRestoredObserved === true
          || ((marker.selectSelectionDispatched === true
              || marker.selectEscapeDispatched === true)
            && !selectOpenedCurrent
            && activeWithin(selectButton));
      marker.selectCollapsedA11yObserved =
        marker.selectCollapsedA11yObserved === true
          || (marker.selectSelectionDispatched === true
            && selectCollapsedA11yCurrent);
      marker.selectEscapeOpenedObserved =
        marker.selectEscapeOpenedObserved === true
          || (marker.selectEscapeOpenDispatched === true
            && selectOpenedCurrent
            && selectSelectedText.includes('audit'));
      marker.selectEscapeInitialFocusObserved =
        marker.selectEscapeInitialFocusObserved === true
          || (marker.selectEscapeOpenDispatched === true
            && selectOpenedCurrent
            && selectFocusedText.includes('audit')
            && selectSelectedText.includes('audit'));
      marker.selectEscapeCloseObserved =
        marker.selectEscapeCloseObserved === true
          || (marker.selectEscapeDispatched === true
            && !selectOpenedCurrent
            && selectCollapsedA11yCurrent);
      marker.selectValueRetainedObserved =
        marker.selectValueRetainedObserved === true
          || (marker.selectEscapeDispatched === true
            && !selectOpenedCurrent
            && selectSelectedText.includes('audit')
            && selectValueStatus.includes(
              'Select value: audit client=true change=1'));
      marker.selectPointerOpenedObserved =
        marker.selectPointerOpenedObserved === true
          || (marker.selectPointerOpenDispatched === true
            && String(marker.nativeKeyboardStages || '')
              .split(',').includes('select-pointer-open')
            && selectOpenedCurrent
            && selectFocusedText.includes('audit')
            && selectSelectedText.includes('audit')
            && selectLifecycleStatus.includes(
              'Select lifecycle: opened client=true change=5'));
      marker.selectPointerCloseObserved =
        marker.selectPointerCloseObserved === true
          || (marker.selectPointerCloseDispatched === true
            && !selectOpenedCurrent
            && selectCollapsedA11yCurrent);
      marker.selectPointerValueRetainedObserved =
        marker.selectPointerValueRetainedObserved === true
          || (marker.selectPointerCloseDispatched === true
            && !selectOpenedCurrent
            && selectSelectedText.includes('audit')
            && selectValueStatus.includes(
              'Select value: audit client=true change=1'));
      marker.selectPointerFocusRestoredObserved =
        marker.selectPointerFocusRestoredObserved === true
          || (marker.selectPointerCloseDispatched === true
            && !selectOpenedCurrent
            && activeWithin(selectButton));
      marker.selectPointerJavaLifecycleObserved =
        marker.selectPointerJavaLifecycleObserved === true
          || selectLifecycleStatus.includes(
            'Select lifecycle: closed client=true change=6');
      marker.selectPointerUnderlyingClickSuppressedObserved =
        marker.selectPointerUnderlyingClickSuppressedObserved === true
          || (marker.selectPointerCloseDispatched === true
            && outsideActionStatus.includes('outside action: pending'));

      const picker =
        document.querySelector('#framework-flow-input-popup-date');
      const dateInput = picker?.inputElement
        || picker?.shadowRoot?.querySelector('input');
      const dateOverlay =
        picker?.shadowRoot?.querySelector('vaadin-date-picker-overlay');
      const dateOverlayContent =
        picker?.querySelector('vaadin-date-picker-overlay-content');
      const dateOpenedCurrent =
        picker?.opened === true
          && (dateOverlay?.opened === true
            || dateOverlay?.hasAttribute?.('opened') === true);
      const dateA11yCurrent =
        dateOpenedCurrent
          && dateInput?.getAttribute?.('role') === 'combobox'
          && dateInput?.getAttribute?.('aria-haspopup') === 'dialog'
          && dateInput?.getAttribute?.('aria-expanded') === 'true'
          && String(dateInput?.getAttribute?.('aria-label') || '')
            .includes('Flow IPC popup due date')
          && dateOverlayContent?.getAttribute?.('role') === 'dialog';
      const dateCalendarFocusCurrent =
        dateOpenedCurrent && activeWithin(dateOverlayContent);
      const dateFocusedValue =
        formatDate(dateOverlayContent?.focusedDate);
      const dateValueStatus =
        document.querySelector(
          '#framework-flow-input-popup-date-value-status')
          ?.textContent || '';
      const dateLifecycleStatus =
        document.querySelector(
          '#framework-flow-input-popup-date-lifecycle-status')
          ?.textContent || '';
      const dateCollapsedA11yCurrent =
        !dateOpenedCurrent
          && dateInput?.getAttribute?.('aria-expanded') === 'false';

      marker.dateOpenedObserved =
        marker.dateOpenedObserved === true
          || (marker.dateOpenDispatched === true && dateOpenedCurrent);
      marker.dateA11yObserved =
        marker.dateA11yObserved === true || dateA11yCurrent;
      marker.dateCalendarFocusObserved =
        marker.dateCalendarFocusObserved === true
          || dateCalendarFocusCurrent;
      marker.dateKeyboardMoveObserved =
        marker.dateKeyboardMoveObserved === true
          || (marker.dateMoveDispatched === true
            && dateFocusedValue === '2026-08-13'
            && dateCalendarFocusCurrent);
      marker.dateSelectionObserved =
        marker.dateSelectionObserved === true
          || (marker.dateSelectionDispatched === true
            && picker?.value === '2026-08-13');
      marker.dateJavaValueObserved =
        marker.dateJavaValueObserved === true
          || dateValueStatus.includes(
            'DatePicker value: 2026-08-13 client=true change=1');
      marker.dateClosedObserved =
        marker.dateClosedObserved === true
          || (marker.dateSelectionDispatched === true
            && !dateOpenedCurrent
            && dateCollapsedA11yCurrent);
      marker.dateFocusRestoredObserved =
        marker.dateFocusRestoredObserved === true
          || ((marker.dateSelectionDispatched === true
              || marker.dateEscapeDispatched === true)
            && !dateOpenedCurrent
            && activeWithin(dateInput));
      marker.dateEscapeOpenedObserved =
        marker.dateEscapeOpenedObserved === true
          || (marker.dateEscapeOpenDispatched === true
            && dateOpenedCurrent
            && picker?.value === '2026-08-13');
      marker.dateEscapeCloseObserved =
        marker.dateEscapeCloseObserved === true
          || (marker.dateEscapeDispatched === true
            && !dateOpenedCurrent
            && dateCollapsedA11yCurrent
            && dateLifecycleStatus.includes(
              'DatePicker lifecycle: closed client=true'));
      marker.dateValueRetainedObserved =
        marker.dateValueRetainedObserved === true
          || (marker.dateEscapeDispatched === true
            && !dateOpenedCurrent
            && picker?.value === '2026-08-13'
            && dateValueStatus.includes('change=1'));
      marker.dateJavaLifecycleObserved =
        marker.dateJavaLifecycleObserved === true
          || dateLifecycleStatus.includes(
            'DatePicker lifecycle: opened client=true');
      marker.datePointerOpenedObserved =
        marker.datePointerOpenedObserved === true
          || (marker.datePointerOpenDispatched === true
            && dateOpenedCurrent
            && picker?.value === '2026-08-13'
            && dateLifecycleStatus.includes(
              'DatePicker lifecycle: opened client=true change=5'));
      marker.datePointerFocusObserved =
        marker.datePointerFocusObserved === true
          || (marker.datePointerOpenDispatched === true
            && String(marker.nativeKeyboardStages || '')
              .split(',').includes('date-pointer-focus')
            && dateCalendarFocusCurrent);
      marker.datePointerCloseObserved =
        marker.datePointerCloseObserved === true
          || (marker.datePointerCloseDispatched === true
            && !dateOpenedCurrent
            && dateCollapsedA11yCurrent);
      marker.datePointerValueRetainedObserved =
        marker.datePointerValueRetainedObserved === true
          || (marker.datePointerCloseDispatched === true
            && !dateOpenedCurrent
            && picker?.value === '2026-08-13'
            && dateValueStatus.includes(
              'DatePicker value: 2026-08-13 client=true change=1'));
      marker.datePointerFocusRestoredObserved =
        marker.datePointerFocusRestoredObserved === true
          || (marker.datePointerCloseDispatched === true
            && !dateOpenedCurrent
            && activeWithin(dateInput));
      marker.datePointerUnderlyingClickSuppressedObserved =
        marker.datePointerUnderlyingClickSuppressedObserved === true
          || (marker.datePointerCloseDispatched === true
            && outsideActionStatus.includes('outside action: pending'));
      marker.datePointerJavaLifecycleObserved =
        marker.datePointerJavaLifecycleObserved === true
          || dateLifecycleStatus.includes(
            'DatePicker lifecycle: closed client=true change=6');

      const timePicker =
        document.querySelector('#framework-flow-input-popup-time');
      const timeInput = timePicker?.inputElement
        || timePicker?.shadowRoot?.querySelector('input');
      const timeOverlay =
        timePicker?.shadowRoot?.querySelector('vaadin-time-picker-overlay');
      const timeScroller = timePicker?._scroller
        || timePicker?.querySelector('vaadin-time-picker-scroller');
      const timeItems = Array.from(timeScroller?.children || [])
        .filter((item) => item.localName === 'vaadin-time-picker-item');
      const timeFocusedData = Array.isArray(timePicker?._dropdownItems)
        ? timePicker._dropdownItems[timePicker._focusedIndex]
        : null;
      const timeFocusedItem = timeItems.find((item) =>
        item.focused === true || item.hasAttribute?.('focused')) || null;
      const timeSelectedItem = timeItems.find((item) =>
        item.selected === true || item.hasAttribute?.('selected')) || null;
      const timeFocusedText = String(timeFocusedData?.label || '')
        || (timeFocusedItem ? collectDeepText(timeFocusedItem) : '');
      const timeSelectedText =
        String(timeScroller?.selectedItem?.label || '')
          || (timeSelectedItem ? collectDeepText(timeSelectedItem) : '');
      const timeValueStatus = document.querySelector(
        '#framework-flow-input-popup-time-value-status')?.textContent || '';
      const timeInputValue = String(timeInput?.value || '');
      const timeOpenedCurrent =
        timePicker?.opened === true
          && (timeOverlay?.opened === true
            || timeOverlay?.hasAttribute?.('opened') === true);
      const timeA11yCurrent =
        timeOpenedCurrent
          && timeInput?.getAttribute?.('role') === 'combobox'
          && timeInput?.getAttribute?.('aria-expanded') === 'true'
          && String(timeInput?.getAttribute?.('aria-label') || '')
            .includes('Flow IPC popup review time')
          && timeScroller?.getAttribute?.('role') === 'listbox'
          && timeItems.length === 5
          && timeItems.every((item) =>
            item.getAttribute?.('role') === 'option');
      const timeCollapsedA11yCurrent =
        !timeOpenedCurrent
          && timeInput?.getAttribute?.('role') === 'combobox'
          && timeInput?.getAttribute?.('aria-expanded') === 'false';
      const timeLocaleFormatCurrent =
        (timePicker?.value === '13:00' && timeInputValue === '1:00 PM')
          || (timePicker?.value === '13:30'
            && timeInputValue === '1:30 PM');

      marker.timeOpenedObserved =
        marker.timeOpenedObserved === true
          || (marker.timeOpenDispatched === true && timeOpenedCurrent);
      marker.timeA11yObserved =
        marker.timeA11yObserved === true || timeA11yCurrent;
      const timeMoveCount = Number(marker.timeMoveCount || 0);
      marker.timeInitialOwnerFocusObserved =
        marker.timeInitialOwnerFocusObserved === true
          || (marker.timeOpenDispatched === true
            && marker.timeMoveDispatched !== true
            && timeOpenedCurrent
            && activeWithin(timeInput)
            && timeFocusedText === ''
            && timeSelectedText.includes('1:00 PM'));
      marker.timeLocaleFormatObserved =
        marker.timeLocaleFormatObserved === true || timeLocaleFormatCurrent;
      marker.timeFirstOptionFocusObserved =
        marker.timeFirstOptionFocusObserved === true
          || (timeMoveCount === 1
            && timeOpenedCurrent
            && timeFocusedText.includes('12:00 PM')
            && timeInputValue === '12:00 PM'
            && timeSelectedText.includes('1:00 PM')
            && timePicker?.value === '13:00');
      marker.timeSecondOptionFocusObserved =
        marker.timeSecondOptionFocusObserved === true
          || (timeMoveCount === 2
            && timeOpenedCurrent
            && timeFocusedText.includes('12:30 PM')
            && timeInputValue === '12:30 PM'
            && timeSelectedText.includes('1:00 PM')
            && timePicker?.value === '13:00');
      marker.timeSelectedOptionFocusObserved =
        marker.timeSelectedOptionFocusObserved === true
          || (timeMoveCount === 3
            && timeOpenedCurrent
            && timeFocusedText.includes('1:00 PM')
            && timeInputValue === '1:00 PM'
            && timeSelectedText.includes('1:00 PM')
            && timePicker?.value === '13:00');
      marker.timeKeyboardMoveObserved =
        marker.timeKeyboardMoveObserved === true
          || (timeMoveCount === 4
            && timeOpenedCurrent
            && timeFocusedText.includes('1:30 PM')
            && timeInputValue === '1:30 PM'
            && timeSelectedText.includes('1:00 PM')
            && timePicker?.value === '13:00');
      marker.timeSelectionObserved =
        marker.timeSelectionObserved === true
          || (marker.timeSelectionDispatched === true
            && timePicker?.value === '13:30'
            && timeInputValue === '1:30 PM');
      marker.timeJavaValueObserved =
        marker.timeJavaValueObserved === true
          || timeValueStatus.includes(
            'TimePicker value: 13:30 client=true change=1');
      marker.timeClosedObserved =
        marker.timeClosedObserved === true
          || (marker.timeSelectionDispatched === true
            && timeCollapsedA11yCurrent);
      marker.timeFocusRestoredObserved =
        marker.timeFocusRestoredObserved === true
          || (marker.timeSelectionDispatched === true
            && timeCollapsedA11yCurrent
            && activeWithin(timeInput));

      const clients = window.Vaadin?.Flow?.clients
        ? Object.keys(window.Vaadin.Flow.clients)
          .filter((name) => name !== 'TypeScript')
        : [];
      return JSON.stringify({
        rootRendered:
          !!document.querySelector('#framework-root')
            || !!document.querySelector(
              '#framework-flow-input-popup-route'),
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
          window.location.pathname.endsWith('/flow-input-popup')
            && !!document.querySelector(
              '#framework-flow-input-popup-route'),
        path: window.location.pathname,
        comboOpenedObserved: marker.comboOpenedObserved === true,
        comboA11yObserved: marker.comboA11yObserved === true,
        comboOwnerFocusObserved:
          marker.comboOwnerFocusObserved === true,
        comboActiveDescendantObserved:
          marker.comboActiveDescendantObserved === true,
        comboSelectionObserved:
          marker.comboSelectionObserved === true,
        comboJavaValueObserved:
          marker.comboJavaValueObserved === true,
        comboClosedObserved: marker.comboClosedObserved === true,
        comboFocusRestoredObserved:
          marker.comboFocusRestoredObserved === true,
        comboCollapsedA11yObserved:
          marker.comboCollapsedA11yObserved === true,
        selectOpenedObserved: marker.selectOpenedObserved === true,
        selectA11yObserved: marker.selectA11yObserved === true,
        selectInitialFocusObserved:
          marker.selectInitialFocusObserved === true,
        selectKeyboardMoveObserved:
          marker.selectKeyboardMoveObserved === true,
        selectSelectionObserved:
          marker.selectSelectionObserved === true,
        selectJavaValueObserved:
          marker.selectJavaValueObserved === true,
        selectClosedObserved: marker.selectClosedObserved === true,
        selectFocusRestoredObserved:
          marker.selectFocusRestoredObserved === true,
        selectCollapsedA11yObserved:
          marker.selectCollapsedA11yObserved === true,
        selectEscapeOpenedObserved:
          marker.selectEscapeOpenedObserved === true,
        selectEscapeInitialFocusObserved:
          marker.selectEscapeInitialFocusObserved === true,
        selectEscapeCloseObserved:
          marker.selectEscapeCloseObserved === true,
        selectValueRetainedObserved:
          marker.selectValueRetainedObserved === true,
        selectPointerOpenedObserved:
          marker.selectPointerOpenedObserved === true,
        selectPointerCloseObserved:
          marker.selectPointerCloseObserved === true,
        selectPointerValueRetainedObserved:
          marker.selectPointerValueRetainedObserved === true,
        selectPointerFocusRestoredObserved:
          marker.selectPointerFocusRestoredObserved === true,
        selectPointerJavaLifecycleObserved:
          marker.selectPointerJavaLifecycleObserved === true,
        selectPointerUnderlyingClickSuppressedObserved:
          marker.selectPointerUnderlyingClickSuppressedObserved === true,
        selectNativeKeyboardObserved:
          marker.selectNativeKeyboardObserved === true,
        selectMenuFocusPreparedByHarness:
          marker.selectMenuFocusPreparedByHarness === true,
        dateOpenedObserved: marker.dateOpenedObserved === true,
        dateA11yObserved: marker.dateA11yObserved === true,
        dateCalendarFocusObserved:
          marker.dateCalendarFocusObserved === true,
        dateKeyboardMoveObserved:
          marker.dateKeyboardMoveObserved === true,
        dateSelectionObserved: marker.dateSelectionObserved === true,
        dateJavaValueObserved: marker.dateJavaValueObserved === true,
        dateClosedObserved: marker.dateClosedObserved === true,
        dateFocusRestoredObserved:
          marker.dateFocusRestoredObserved === true,
        dateEscapeOpenedObserved:
          marker.dateEscapeOpenedObserved === true,
        dateEscapeCloseObserved:
          marker.dateEscapeCloseObserved === true,
        dateValueRetainedObserved:
          marker.dateValueRetainedObserved === true,
        dateJavaLifecycleObserved:
          marker.dateJavaLifecycleObserved === true,
        datePointerOpenedObserved:
          marker.datePointerOpenedObserved === true,
        datePointerFocusObserved:
          marker.datePointerFocusObserved === true,
        datePointerCloseObserved:
          marker.datePointerCloseObserved === true,
        datePointerValueRetainedObserved:
          marker.datePointerValueRetainedObserved === true,
        datePointerFocusRestoredObserved:
          marker.datePointerFocusRestoredObserved === true,
        datePointerUnderlyingClickSuppressedObserved:
          marker.datePointerUnderlyingClickSuppressedObserved === true,
        datePointerJavaLifecycleObserved:
          marker.datePointerJavaLifecycleObserved === true,
        timeOpenedObserved: marker.timeOpenedObserved === true,
        timeA11yObserved: marker.timeA11yObserved === true,
        timeInitialOwnerFocusObserved:
          marker.timeInitialOwnerFocusObserved === true,
        timeLocaleFormatObserved:
          marker.timeLocaleFormatObserved === true,
        timeFirstOptionFocusObserved:
          marker.timeFirstOptionFocusObserved === true,
        timeSecondOptionFocusObserved:
          marker.timeSecondOptionFocusObserved === true,
        timeSelectedOptionFocusObserved:
          marker.timeSelectedOptionFocusObserved === true,
        timeKeyboardMoveObserved:
          marker.timeKeyboardMoveObserved === true,
        timeSelectionObserved: marker.timeSelectionObserved === true,
        timeJavaValueObserved: marker.timeJavaValueObserved === true,
        timeClosedObserved: marker.timeClosedObserved === true,
        timeFocusRestoredObserved:
          marker.timeFocusRestoredObserved === true,
        timeNativeKeyboardObserved:
          marker.timeNativeKeyboardObserved === true,
        nativeKeyboardObserved:
          marker.nativeKeyboardObserved === true,
        nativeKeyboardCurrentObserved:
          marker.nativeKeyboardCurrentObserved === true,
        nativeKeyboardExpected:
          String(marker.nativeKeyboardExpected || ''),
        nativeKeyboardLastKey:
          String(marker.nativeKeyboardLastKey || ''),
        nativeKeyboardLastTarget:
          String(marker.nativeKeyboardLastTarget || ''),
        nativeKeyboardEventCount:
          Number(marker.nativeKeyboardEventCount || 0),
        nativeKeyboardStages:
          String(marker.nativeKeyboardStages || ''),
        nativePointerObserved:
          marker.nativePointerObserved === true,
        nativePointerCurrentObserved:
          marker.nativePointerCurrentObserved === true,
        nativePointerTrustedObserved:
          marker.nativePointerTrustedObserved === true,
        nativePointerEventCount:
          Number(marker.nativePointerEventCount || 0),
        nativePointerTrustedEventCount:
          Number(marker.nativePointerTrustedEventCount || 0),
        nativePointerStages:
          String(marker.nativePointerStages || ''),
        nativePointerLastTarget:
          String(marker.nativePointerLastTarget || ''),
        nativePointerLastButton:
          Number(marker.nativePointerLastButton ?? -1),
        comboOpenedCurrent,
        selectOpenedCurrent,
        dateOpenedCurrent,
        timeOpenedCurrent,
        comboInputRole:
          String(comboInput?.getAttribute?.('role') || ''),
        comboInputExpanded:
          String(comboInput?.getAttribute?.('aria-expanded') || ''),
        comboInputControls:
          String(comboInput?.getAttribute?.('aria-controls') || ''),
        comboActiveDescendant,
        comboActiveOptionText:
          comboActiveOption ? collectDeepText(comboActiveOption) : '',
        comboValue: String(combo?.value || ''),
        selectButtonRole:
          String(selectButton?.getAttribute?.('role') || ''),
        selectButtonHasPopup:
          String(selectButton?.getAttribute?.('aria-haspopup') || ''),
        selectButtonExpanded:
          String(selectButton?.getAttribute?.('aria-expanded') || ''),
        selectListBoxRole:
          String(selectListBox?.getAttribute?.('role') || ''),
        selectFocusedText,
        selectSelectedText,
        selectValue: String(select?.value || ''),
        dateInputRole:
          String(dateInput?.getAttribute?.('role') || ''),
        dateInputHasPopup:
          String(dateInput?.getAttribute?.('aria-haspopup') || ''),
        dateInputExpanded:
          String(dateInput?.getAttribute?.('aria-expanded') || ''),
        dateOverlayContentRole:
          String(dateOverlayContent?.getAttribute?.('role') || ''),
        dateFocusedValue,
        dateValue: String(picker?.value || ''),
        timeInputRole:
          String(timeInput?.getAttribute?.('role') || ''),
        timeInputExpanded:
          String(timeInput?.getAttribute?.('aria-expanded') || ''),
        timeInputControls:
          String(timeInput?.getAttribute?.('aria-controls') || ''),
        timeScrollerRole:
          String(timeScroller?.getAttribute?.('role') || ''),
        timeFocusedText,
        timeSelectedText,
        timeValue: String(timePicker?.value || ''),
        timeInputValue,
        comboValueStatus,
        selectValueStatus,
        selectLifecycleStatus,
        dateValueStatus,
        dateLifecycleStatus,
        timeValueStatus,
        outsideActionStatus,
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
