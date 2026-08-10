import Foundation

extension FlowIpcAcceptanceRuntimeObserver {
    func schedulePickerValidationAcceptance(
        host: WebViewHost,
        context: FlowIpcWindowContext,
        timeoutSeconds: Double
    ) {
        FlowIpcPickerValidationAcceptance(
            context: context,
            deadline: Date().addingTimeInterval(timeoutSeconds)
        ).start()
    }
}

private final class FlowIpcPickerValidationAcceptance {
    private enum Phase {
        case ready
        case dedicatedRoute
        case minimumDispatched
        case minimumRecoveryDispatched
        case maximumDispatched
        case maximumRecoveryDispatched
        case incompleteDispatched
        case incompleteRecoveryDispatched
        case requiredDispatched
        case malformedDispatched
        case finalRecoveryDispatched
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
                self.retryOrFail(code: "pickerValidationInspectionFailed")
                return
            }
            guard snapshot.errorFree else {
                self.fail(
                    code: "pickerValidationBrowserRuntimeError",
                    details: snapshot.tracePayload)
                return
            }

            switch self.phase {
            case .ready:
                guard snapshot.ready else {
                    self.retryOrFail(
                        code: "pickerValidationReadyTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.perform(
                    Self.navigationScript,
                    next: .dedicatedRoute,
                    failureCode: "pickerValidationNavigationFailed")
            case .dedicatedRoute:
                guard snapshot.dedicatedRouteReady,
                      snapshot.initialStateObserved,
                      snapshot.constraintPropertiesObserved else {
                    self.retryOrFail(
                        code: "pickerValidationDedicatedRouteTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.perform(
                    Self.minimumValueScript,
                    next: .minimumDispatched,
                    failureCode: "pickerValidationMinimumDispatchFailed")
            case .minimumDispatched:
                guard snapshot.invalidActionDispatched,
                      snapshot.minimumBrowserChangeObserved,
                      snapshot.javaInvalidObserved,
                      snapshot.invalidPatchObserved,
                      snapshot.errorMessagePatchObserved,
                      snapshot.childInvalidPatchObserved else {
                    self.retryOrFail(
                        code: "pickerValidationMinimumPatchTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.perform(
                    Self.minimumRecoveryValueScript,
                    next: .minimumRecoveryDispatched,
                    failureCode: "pickerValidationMinimumRecoveryDispatchFailed")
            case .minimumRecoveryDispatched:
                guard snapshot.minimumRecoveryComplete else {
                    self.retryOrFail(
                        code: "pickerValidationMinimumRecoveryPatchTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.perform(
                    Self.maximumValueScript,
                    next: .maximumDispatched,
                    failureCode: "pickerValidationMaximumDispatchFailed")
            case .maximumDispatched:
                guard snapshot.maximumComplete else {
                    self.retryOrFail(
                        code: "pickerValidationMaximumPatchTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.perform(
                    Self.maximumRecoveryValueScript,
                    next: .maximumRecoveryDispatched,
                    failureCode: "pickerValidationMaximumRecoveryDispatchFailed")
            case .maximumRecoveryDispatched:
                guard snapshot.maximumRecoveryObserved else {
                    self.retryOrFail(
                        code: "pickerValidationMaximumRecoveryPatchTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.perform(
                    Self.incompleteValueScript,
                    next: .incompleteDispatched,
                    failureCode: "pickerValidationIncompleteDispatchFailed")
            case .incompleteDispatched:
                guard snapshot.incompleteComplete else {
                    self.retryOrFail(
                        code: "pickerValidationIncompletePatchTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.perform(
                    Self.incompleteRecoveryValueScript,
                    next: .incompleteRecoveryDispatched,
                    failureCode: "pickerValidationIncompleteRecoveryDispatchFailed")
            case .incompleteRecoveryDispatched:
                guard snapshot.incompleteRecoveryObserved else {
                    self.retryOrFail(
                        code: "pickerValidationIncompleteRecoveryPatchTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.perform(
                    Self.requiredValueScript,
                    next: .requiredDispatched,
                    failureCode: "pickerValidationRequiredDispatchFailed")
            case .requiredDispatched:
                guard snapshot.requiredComplete else {
                    self.retryOrFail(
                        code: "pickerValidationRequiredPatchTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.perform(
                    Self.malformedValueScript,
                    next: .malformedDispatched,
                    failureCode: "pickerValidationMalformedDispatchFailed")
            case .malformedDispatched:
                guard snapshot.malformedComplete else {
                    self.retryOrFail(
                        code: "pickerValidationMalformedPatchTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.perform(
                    Self.finalRecoveryValueScript,
                    next: .finalRecoveryDispatched,
                    failureCode: "pickerValidationFinalRecoveryDispatchFailed")
            case .finalRecoveryDispatched:
                guard snapshot.complete else {
                    self.retryOrFail(
                        code: "pickerValidationFinalRecoveryPatchTimeout",
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
        guard Date() < deadline else {
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
            "opaqueRequestBridgeObserved": snapshot.requestCount >= 4,
            "opaqueResponseBridgeObserved": snapshot.responseCount >= 4,
            "pushObserved": snapshot.pushMessageCount >= 1,
            "errorFree": snapshot.errorFree,
            "pickerValidationBrowserProgrammaticInputObserved": true
        ]) { _, replacement in replacement }
        trace("recv", "pickerValidation.frameworkAcceptance", payload)
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
        let initialStateObserved: Bool
        let constraintPropertiesObserved: Bool
        let invalidActionDispatched: Bool
        let minimumBrowserChangeObserved: Bool
        let javaInvalidObserved: Bool
        let invalidPatchObserved: Bool
        let errorMessagePatchObserved: Bool
        let childInvalidPatchObserved: Bool
        let recoveryActionDispatched: Bool
        let minimumRecoveryBrowserChangeObserved: Bool
        let javaRecoveryObserved: Bool
        let recoveredValuePatchObserved: Bool
        let validPatchObserved: Bool
        let errorClearedObserved: Bool
        let childValidPatchObserved: Bool
        let maximumActionDispatched: Bool
        let maximumBrowserChangeObserved: Bool
        let javaMaximumObserved: Bool
        let maximumInvalidPatchObserved: Bool
        let maximumErrorMessagePatchObserved: Bool
        let maximumChildInvalidPatchObserved: Bool
        let maximumRecoveryActionDispatched: Bool
        let maximumRecoveryBrowserChangeObserved: Bool
        let maximumRecoveryObserved: Bool
        let incompleteActionDispatched: Bool
        let incompleteBrowserChangeObserved: Bool
        let javaIncompleteObserved: Bool
        let incompleteInvalidPatchObserved: Bool
        let incompleteErrorMessagePatchObserved: Bool
        let incompleteChildInvalidPatchObserved: Bool
        let incompleteRecoveryActionDispatched: Bool
        let incompleteRecoveryBrowserChangeObserved: Bool
        let incompleteRecoveryObserved: Bool
        let requiredActionDispatched: Bool
        let requiredBrowserChangeObserved: Bool
        let javaRequiredObserved: Bool
        let requiredInvalidPatchObserved: Bool
        let requiredErrorMessagePatchObserved: Bool
        let requiredChildInvalidPatchObserved: Bool
        let malformedActionDispatched: Bool
        let malformedInputEventObserved: Bool
        let malformedChildUnparsableEventObserved: Bool
        let malformedParentUnparsableEventObserved: Bool
        let javaMalformedValidationObserved: Bool
        let malformedInvalidPatchObserved: Bool
        let malformedErrorMessagePatchObserved: Bool
        let malformedChildInvalidPatchObserved: Bool
        let finalRecoveryActionDispatched: Bool
        let finalRecoveryBrowserChangeObserved: Bool
        let finalJavaRecoveryObserved: Bool
        let finalValuePatchObserved: Bool
        let finalValidPatchObserved: Bool
        let finalErrorClearedObserved: Bool
        let finalChildValidPatchObserved: Bool
        let overlaysClosedObserved: Bool
        let pickerValue: String
        let pickerMin: String
        let pickerMax: String
        let pickerInvalid: Bool
        let pickerErrorMessage: String
        let datePickerInvalid: Bool
        let timePickerInvalid: Bool
        let validationStatus: String
        let validationEventStatus: String
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

        var minimumRecoveryComplete: Bool {
            recoveryActionDispatched
                && minimumRecoveryBrowserChangeObserved
                && javaRecoveryObserved
                && recoveredValuePatchObserved
                && validPatchObserved
                && errorClearedObserved
                && childValidPatchObserved
        }

        var maximumComplete: Bool {
            maximumActionDispatched
                && maximumBrowserChangeObserved
                && javaMaximumObserved
                && maximumInvalidPatchObserved
                && maximumErrorMessagePatchObserved
                && maximumChildInvalidPatchObserved
        }

        var incompleteComplete: Bool {
            incompleteActionDispatched
                && incompleteBrowserChangeObserved
                && javaIncompleteObserved
                && incompleteInvalidPatchObserved
                && incompleteErrorMessagePatchObserved
                && incompleteChildInvalidPatchObserved
        }

        var requiredComplete: Bool {
            requiredActionDispatched
                && requiredBrowserChangeObserved
                && javaRequiredObserved
                && requiredInvalidPatchObserved
                && requiredErrorMessagePatchObserved
                && requiredChildInvalidPatchObserved
        }

        var malformedComplete: Bool {
            malformedActionDispatched
                && malformedInputEventObserved
                && malformedChildUnparsableEventObserved
                && malformedParentUnparsableEventObserved
                && javaMalformedValidationObserved
                && malformedInvalidPatchObserved
                && malformedErrorMessagePatchObserved
                && malformedChildInvalidPatchObserved
        }

        var complete: Bool {
            errorFree
                && dedicatedRouteReady
                && initialStateObserved
                && constraintPropertiesObserved
                && invalidActionDispatched
                && minimumBrowserChangeObserved
                && javaInvalidObserved
                && invalidPatchObserved
                && errorMessagePatchObserved
                && childInvalidPatchObserved
                && minimumRecoveryComplete
                && maximumComplete
                && maximumRecoveryActionDispatched
                && maximumRecoveryBrowserChangeObserved
                && maximumRecoveryObserved
                && incompleteComplete
                && incompleteRecoveryActionDispatched
                && incompleteRecoveryBrowserChangeObserved
                && incompleteRecoveryObserved
                && requiredComplete
                && malformedComplete
                && finalRecoveryActionDispatched
                && finalRecoveryBrowserChangeObserved
                && finalJavaRecoveryObserved
                && finalValuePatchObserved
                && finalValidPatchObserved
                && finalErrorClearedObserved
                && finalChildValidPatchObserved
                && overlaysClosedObserved
                && requestCount >= 4
                && responseCount >= 4
                && pushMessageCount >= 1
        }

        var tracePayload: [String: Any] {
            [
                "pickerValidationDedicatedRouteObserved": dedicatedRouteReady,
                "pickerValidationPath": path,
                "pickerValidationInitialStateObserved": initialStateObserved,
                "pickerValidationConstraintPropertiesObserved":
                    constraintPropertiesObserved,
                "pickerValidationInvalidActionDispatched":
                    invalidActionDispatched,
                "pickerValidationMinimumBrowserChangeObserved":
                    minimumBrowserChangeObserved,
                "pickerValidationJavaInvalidObserved": javaInvalidObserved,
                "pickerValidationInvalidPatchObserved": invalidPatchObserved,
                "pickerValidationErrorMessagePatchObserved":
                    errorMessagePatchObserved,
                "pickerValidationChildInvalidPatchObserved":
                    childInvalidPatchObserved,
                "pickerValidationRecoveryActionDispatched":
                    recoveryActionDispatched,
                "pickerValidationMinimumRecoveryBrowserChangeObserved":
                    minimumRecoveryBrowserChangeObserved,
                "pickerValidationJavaRecoveryObserved": javaRecoveryObserved,
                "pickerValidationRecoveredValuePatchObserved":
                    recoveredValuePatchObserved,
                "pickerValidationValidPatchObserved": validPatchObserved,
                "pickerValidationErrorClearedObserved": errorClearedObserved,
                "pickerValidationChildValidPatchObserved":
                    childValidPatchObserved,
                "pickerValidationMaximumActionDispatched":
                    maximumActionDispatched,
                "pickerValidationMaximumBrowserChangeObserved":
                    maximumBrowserChangeObserved,
                "pickerValidationJavaMaximumObserved": javaMaximumObserved,
                "pickerValidationMaximumInvalidPatchObserved":
                    maximumInvalidPatchObserved,
                "pickerValidationMaximumErrorMessagePatchObserved":
                    maximumErrorMessagePatchObserved,
                "pickerValidationMaximumChildInvalidPatchObserved":
                    maximumChildInvalidPatchObserved,
                "pickerValidationMaximumRecoveryActionDispatched":
                    maximumRecoveryActionDispatched,
                "pickerValidationMaximumRecoveryBrowserChangeObserved":
                    maximumRecoveryBrowserChangeObserved,
                "pickerValidationMaximumRecoveryObserved":
                    maximumRecoveryObserved,
                "pickerValidationIncompleteActionDispatched":
                    incompleteActionDispatched,
                "pickerValidationIncompleteBrowserChangeObserved":
                    incompleteBrowserChangeObserved,
                "pickerValidationJavaIncompleteObserved":
                    javaIncompleteObserved,
                "pickerValidationIncompleteInvalidPatchObserved":
                    incompleteInvalidPatchObserved,
                "pickerValidationIncompleteErrorMessagePatchObserved":
                    incompleteErrorMessagePatchObserved,
                "pickerValidationIncompleteChildInvalidPatchObserved":
                    incompleteChildInvalidPatchObserved,
                "pickerValidationIncompleteRecoveryActionDispatched":
                    incompleteRecoveryActionDispatched,
                "pickerValidationIncompleteRecoveryBrowserChangeObserved":
                    incompleteRecoveryBrowserChangeObserved,
                "pickerValidationIncompleteRecoveryObserved":
                    incompleteRecoveryObserved,
                "pickerValidationRequiredActionDispatched":
                    requiredActionDispatched,
                "pickerValidationRequiredBrowserChangeObserved":
                    requiredBrowserChangeObserved,
                "pickerValidationJavaRequiredObserved": javaRequiredObserved,
                "pickerValidationRequiredInvalidPatchObserved":
                    requiredInvalidPatchObserved,
                "pickerValidationRequiredErrorMessagePatchObserved":
                    requiredErrorMessagePatchObserved,
                "pickerValidationRequiredChildInvalidPatchObserved":
                    requiredChildInvalidPatchObserved,
                "pickerValidationMalformedActionDispatched":
                    malformedActionDispatched,
                "pickerValidationMalformedInputEventObserved":
                    malformedInputEventObserved,
                "pickerValidationMalformedChildUnparsableEventObserved":
                    malformedChildUnparsableEventObserved,
                "pickerValidationMalformedParentUnparsableEventObserved":
                    malformedParentUnparsableEventObserved,
                "pickerValidationJavaMalformedValidationObserved":
                    javaMalformedValidationObserved,
                "pickerValidationMalformedInvalidPatchObserved":
                    malformedInvalidPatchObserved,
                "pickerValidationMalformedErrorMessagePatchObserved":
                    malformedErrorMessagePatchObserved,
                "pickerValidationMalformedChildInvalidPatchObserved":
                    malformedChildInvalidPatchObserved,
                "pickerValidationFinalRecoveryActionDispatched":
                    finalRecoveryActionDispatched,
                "pickerValidationFinalRecoveryBrowserChangeObserved":
                    finalRecoveryBrowserChangeObserved,
                "pickerValidationFinalJavaRecoveryObserved":
                    finalJavaRecoveryObserved,
                "pickerValidationFinalValuePatchObserved":
                    finalValuePatchObserved,
                "pickerValidationFinalValidPatchObserved":
                    finalValidPatchObserved,
                "pickerValidationFinalErrorClearedObserved":
                    finalErrorClearedObserved,
                "pickerValidationFinalChildValidPatchObserved":
                    finalChildValidPatchObserved,
                "pickerValidationOverlaysClosedObserved":
                    overlaysClosedObserved,
                "pickerValidationValue": pickerValue,
                "pickerValidationMin": pickerMin,
                "pickerValidationMax": pickerMax,
                "pickerValidationInvalid": pickerInvalid,
                "pickerValidationErrorMessage": pickerErrorMessage,
                "pickerValidationDatePickerInvalid": datePickerInvalid,
                "pickerValidationTimePickerInvalid": timePickerInvalid,
                "pickerValidationJavaStatus": validationStatus,
                "pickerValidationJavaValidationStatus":
                    validationEventStatus,
                "pickerValidationMarkerError": markerError,
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
      const trigger =
        document.querySelector('#framework-flow-input-popup-open-route');
      if (!trigger || typeof trigger.click !== 'function') return false;
      trigger.click();
      return true;
    })()
    """

    private static let minimumValueScript = valueScript(
        dateValue: "2026-08-12",
        timeValue: "11:30",
        dateInputValue: "8/12/2026",
        timeInputValue: "11:30 AM",
        value: "2026-08-12T11:30",
        markerProperty: "invalidActionDispatched",
        browserMarkerProperty: "minimumBrowserChangeObserved")

    private static let minimumRecoveryValueScript = valueScript(
        dateValue: "2026-08-12",
        timeValue: "13:30",
        dateInputValue: "8/12/2026",
        timeInputValue: "1:30 PM",
        value: "2026-08-12T13:30",
        markerProperty: "recoveryActionDispatched",
        browserMarkerProperty: "minimumRecoveryBrowserChangeObserved")

    private static let maximumValueScript = valueScript(
        dateValue: "2026-08-12",
        timeValue: "15:30",
        dateInputValue: "8/12/2026",
        timeInputValue: "3:30 PM",
        value: "2026-08-12T15:30",
        markerProperty: "maximumActionDispatched",
        browserMarkerProperty: "maximumBrowserChangeObserved")

    private static let maximumRecoveryValueScript = valueScript(
        dateValue: "2026-08-12",
        timeValue: "13:30",
        dateInputValue: "8/12/2026",
        timeInputValue: "1:30 PM",
        value: "2026-08-12T13:30",
        markerProperty: "maximumRecoveryActionDispatched",
        browserMarkerProperty: "maximumRecoveryBrowserChangeObserved")

    private static let incompleteValueScript = valueScript(
        dateValue: "2026-08-12",
        timeValue: "",
        dateInputValue: "8/12/2026",
        timeInputValue: "",
        value: "",
        markerProperty: "incompleteActionDispatched",
        browserMarkerProperty: "incompleteBrowserChangeObserved")

    private static let incompleteRecoveryValueScript = valueScript(
        dateValue: "2026-08-12",
        timeValue: "13:30",
        dateInputValue: "8/12/2026",
        timeInputValue: "1:30 PM",
        value: "2026-08-12T13:30",
        markerProperty: "incompleteRecoveryActionDispatched",
        browserMarkerProperty: "incompleteRecoveryBrowserChangeObserved")

    private static let requiredValueScript = valueScript(
        dateValue: "",
        timeValue: "",
        dateInputValue: "",
        timeInputValue: "",
        value: "",
        markerProperty: "requiredActionDispatched",
        browserMarkerProperty: "requiredBrowserChangeObserved")

    private static let finalRecoveryValueScript = valueScript(
        dateValue: "2026-08-12",
        timeValue: "13:30",
        dateInputValue: "8/12/2026",
        timeInputValue: "1:30 PM",
        value: "2026-08-12T13:30",
        markerProperty: "finalRecoveryActionDispatched",
        browserMarkerProperty: "finalRecoveryBrowserChangeObserved")

    private static func valueScript(
        dateValue: String,
        timeValue: String,
        dateInputValue: String,
        timeInputValue: String,
        value: String,
        markerProperty: String,
        browserMarkerProperty: String
    ) -> String {
        """
        (() => {
          const picker =
            document.querySelector('#framework-flow-picker-validation');
          if (!picker) return false;
          const datePicker = picker.datePicker
            || picker.__datePicker
            || picker.querySelector('vaadin-date-picker');
          const timePicker = picker.timePicker
            || picker.__timePicker
            || picker.querySelector('vaadin-time-picker');
          if (!datePicker || !timePicker) return false;
          const marker = window.VaadinDesktopPickerValidation || {};
          window.VaadinDesktopPickerValidation = marker;
          marker.\(markerProperty) = true;
          picker.addEventListener('change', () => {
            marker.\(browserMarkerProperty) = true;
          }, { once: true });
          const setNativeInputValue = (target, nextValue) => {
            if (!target) return;
            const descriptor = Object.getOwnPropertyDescriptor(
              HTMLInputElement.prototype, 'value');
            if (descriptor?.set) {
              descriptor.set.call(target, nextValue);
            } else {
              target.value = nextValue;
            }
            target.dispatchEvent(new Event('input', {
              bubbles: true,
              composed: true
            }));
            target.dispatchEvent(new Event('change', {
              bubbles: true,
              composed: true
            }));
          };
          picker.scrollIntoView({ block: 'center', inline: 'center' });
          datePicker.value = '\(dateValue)';
          setNativeInputValue(
            datePicker.inputElement
              || datePicker.shadowRoot?.querySelector('input'),
            '\(dateInputValue)');
          datePicker.dispatchEvent(new CustomEvent('value-changed', {
            bubbles: true,
            composed: true,
            detail: { value: '\(dateValue)' }
          }));
          timePicker.value = '\(timeValue)';
          setNativeInputValue(
            timePicker.inputElement
              || timePicker.shadowRoot?.querySelector('input'),
            '\(timeInputValue)');
          timePicker.dispatchEvent(new CustomEvent('value-changed', {
            bubbles: true,
            composed: true,
            detail: { value: '\(timeValue)' }
          }));
          if (picker.value !== '\(value)'
              || typeof picker.__commitPendingValueChange !== 'function'
              || picker.__hasPendingValueChange !== true) {
            return false;
          }
          picker.__commitPendingValueChange();
          if (typeof datePicker.close === 'function') datePicker.close();
          if (typeof timePicker.close === 'function') timePicker.close();
          datePicker.opened = false;
          timePicker.opened = false;
          return true;
        })()
        """
    }

    private static let malformedValueScript = """
    (() => {
      const picker =
        document.querySelector('#framework-flow-picker-validation');
      if (!picker) return false;
      const datePicker = picker.datePicker
        || picker.__datePicker
        || picker.querySelector('vaadin-date-picker');
      const timePicker = picker.timePicker
        || picker.__timePicker
        || picker.querySelector('vaadin-time-picker');
      const dateInput = datePicker?.inputElement
        || datePicker?.shadowRoot?.querySelector('input');
      if (!datePicker || !timePicker || !dateInput) return false;
      const marker = window.VaadinDesktopPickerValidation || {};
      window.VaadinDesktopPickerValidation = marker;
      marker.malformedActionDispatched = true;
      dateInput.addEventListener('input', () => {
        marker.malformedInputEventObserved = true;
      }, { once: true });
      datePicker.addEventListener('unparsable-change', () => {
        marker.malformedChildUnparsableEventObserved = true;
      }, { once: true });
      picker.addEventListener('unparsable-change', () => {
        marker.malformedParentUnparsableEventObserved = true;
      }, { once: true });
      datePicker.value = '';
      const descriptor = Object.getOwnPropertyDescriptor(
        HTMLInputElement.prototype, 'value');
      if (descriptor?.set) {
        descriptor.set.call(dateInput, 'not-a-date');
      } else {
        dateInput.value = 'not-a-date';
      }
      dateInput.dispatchEvent(new Event('input', {
        bubbles: true,
        composed: true
      }));
      datePicker._inputElementValue = 'not-a-date';
      if (typeof datePicker.__commitValueChange === 'function') {
        datePicker.__commitValueChange();
      } else {
        return false;
      }
      if (typeof datePicker.close === 'function') datePicker.close();
      if (typeof timePicker.close === 'function') timePicker.close();
      datePicker.opened = false;
      timePicker.opened = false;
      return true;
    })()
    """

    private static let inspectionScript = """
    (() => {
      const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
      const marker = window.VaadinDesktopPickerValidation || {};
      window.VaadinDesktopPickerValidation = marker;
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
          for (const child of node.childNodes || []) visit(child);
        };
        visit(rootNode);
        return parts.join(' ').replace(/\\s+/g, ' ').trim();
      };
      const picker =
        document.querySelector('#framework-flow-picker-validation');
      const datePicker = picker?.datePicker
        || picker?.__datePicker
        || picker?.querySelector('vaadin-date-picker');
      const timePicker = picker?.timePicker
        || picker?.__timePicker
        || picker?.querySelector('vaadin-time-picker');
      const validationStatus = document.querySelector(
        '#framework-flow-picker-validation-status')?.textContent || '';
      const validationEventStatus = document.querySelector(
        '#framework-flow-picker-validation-event-status')?.textContent || '';
      const pickerValue = String(picker?.value || '');
      const pickerMin = String(picker?.min || '');
      const pickerMax = String(picker?.max || '');
      const pickerInvalid = picker?.invalid === true;
      const pickerErrorMessage = String(picker?.errorMessage || '');
      const datePickerInvalid = datePicker?.invalid === true;
      const timePickerInvalid = timePicker?.invalid === true;
      const deepText = picker ? collectDeepText(picker) : '';
      const initialStateCurrent = !!picker
        && pickerValue.startsWith('2026-08-12T13:00')
        && pickerInvalid === false
        && validationStatus.includes(
          'value=2026-08-12T13:00 invalid=false error=none');
      const constraintPropertiesCurrent = !!picker
        && picker.required === true
        && pickerMin.startsWith('2026-08-12T12:00')
        && pickerMax.startsWith('2026-08-12T14:00');
      const javaInvalidCurrent = validationStatus.includes(
          'value=2026-08-12T11:30')
        && validationStatus.includes('invalid=true')
        && validationStatus.includes(
          'error=Picker validation value is too early')
        && validationStatus.includes('client=true');
      const invalidPatchCurrent = pickerInvalid
        && pickerValue.startsWith('2026-08-12T11:30');
      const errorMessagePatchCurrent =
        pickerErrorMessage === 'Picker validation value is too early'
          && deepText.includes('Picker validation value is too early');
      const childInvalidPatchCurrent =
        datePickerInvalid && timePickerInvalid;
      const validJavaCurrent = validationStatus.includes(
          'value=2026-08-12T13:30')
        && validationStatus.includes('invalid=false')
        && validationStatus.includes('error=none')
        && validationStatus.includes('client=true');
      const validValuePatchCurrent =
        pickerValue.startsWith('2026-08-12T13:30');
      const validPatchCurrent = pickerInvalid === false;
      const errorClearedCurrent = pickerErrorMessage === ''
        && !deepText.includes('Picker validation value is too early')
        && !deepText.includes('Picker validation value is too late')
        && !deepText.includes('Picker validation value is incomplete')
        && !deepText.includes('Picker validation value is required')
        && !deepText.includes('Picker validation value is malformed');
      const childValidPatchCurrent = !!datePicker && !!timePicker
        && datePickerInvalid === false
        && timePickerInvalid === false;
      const validStateCurrent = validJavaCurrent
        && validValuePatchCurrent
        && validPatchCurrent
        && errorClearedCurrent
        && childValidPatchCurrent;
      const javaMaximumCurrent = validationStatus.includes(
          'value=2026-08-12T15:30')
        && validationStatus.includes('invalid=true')
        && validationStatus.includes(
          'error=Picker validation value is too late')
        && validationStatus.includes('client=true');
      const maximumInvalidPatchCurrent = pickerInvalid
        && pickerValue.startsWith('2026-08-12T15:30');
      const maximumErrorMessagePatchCurrent =
        pickerErrorMessage === 'Picker validation value is too late'
          && deepText.includes('Picker validation value is too late');
      const javaIncompleteCurrent = validationStatus.includes('value=null')
        && validationStatus.includes('invalid=true')
        && validationStatus.includes(
          'error=Picker validation value is incomplete')
        && validationStatus.includes('client=true');
      const incompleteInvalidPatchCurrent = pickerInvalid
        && pickerValue === '';
      const incompleteErrorMessagePatchCurrent =
        pickerErrorMessage === 'Picker validation value is incomplete'
          && deepText.includes('Picker validation value is incomplete');
      const javaRequiredCurrent = validationStatus.includes('value=null')
        && validationStatus.includes('invalid=true')
        && validationStatus.includes(
          'error=Picker validation value is required')
        && validationStatus.includes('client=true');
      const requiredInvalidPatchCurrent = pickerInvalid
        && pickerValue === '';
      const requiredErrorMessagePatchCurrent =
        pickerErrorMessage === 'Picker validation value is required'
          && deepText.includes('Picker validation value is required');
      const javaMalformedValidationCurrent = validationEventStatus.includes(
          'valid=false')
        && validationEventStatus.includes('invalid=true')
        && validationEventStatus.includes(
          'error=Picker validation value is malformed');
      const malformedInvalidPatchCurrent = pickerInvalid
        && pickerValue === '';
      const malformedErrorMessagePatchCurrent =
        pickerErrorMessage === 'Picker validation value is malformed'
          && deepText.includes('Picker validation value is malformed');
      const overlaysClosedCurrent = !!datePicker && !!timePicker
        && datePicker.opened !== true
        && timePicker.opened !== true;

      marker.initialStateObserved = marker.initialStateObserved === true
        || initialStateCurrent;
      marker.constraintPropertiesObserved =
        marker.constraintPropertiesObserved === true
          || constraintPropertiesCurrent;
      marker.javaInvalidObserved = marker.javaInvalidObserved === true
        || (marker.invalidActionDispatched === true && javaInvalidCurrent);
      marker.invalidPatchObserved = marker.invalidPatchObserved === true
        || (marker.invalidActionDispatched === true && invalidPatchCurrent);
      marker.errorMessagePatchObserved =
        marker.errorMessagePatchObserved === true
          || (marker.invalidActionDispatched === true
            && errorMessagePatchCurrent);
      marker.childInvalidPatchObserved =
        marker.childInvalidPatchObserved === true
          || (marker.invalidActionDispatched === true
            && childInvalidPatchCurrent);
      marker.javaRecoveryObserved = marker.javaRecoveryObserved === true
        || (marker.recoveryActionDispatched === true
          && validJavaCurrent);
      marker.recoveredValuePatchObserved =
        marker.recoveredValuePatchObserved === true
          || (marker.recoveryActionDispatched === true
            && validValuePatchCurrent);
      marker.validPatchObserved = marker.validPatchObserved === true
        || (marker.recoveryActionDispatched === true && validPatchCurrent);
      marker.errorClearedObserved = marker.errorClearedObserved === true
        || (marker.recoveryActionDispatched === true
          && errorClearedCurrent);
      marker.childValidPatchObserved =
        marker.childValidPatchObserved === true
          || (marker.recoveryActionDispatched === true
            && childValidPatchCurrent);
      marker.javaMaximumObserved = marker.javaMaximumObserved === true
        || (marker.maximumActionDispatched === true
          && javaMaximumCurrent);
      marker.maximumInvalidPatchObserved =
        marker.maximumInvalidPatchObserved === true
          || (marker.maximumActionDispatched === true
            && maximumInvalidPatchCurrent);
      marker.maximumErrorMessagePatchObserved =
        marker.maximumErrorMessagePatchObserved === true
          || (marker.maximumActionDispatched === true
            && maximumErrorMessagePatchCurrent);
      marker.maximumChildInvalidPatchObserved =
        marker.maximumChildInvalidPatchObserved === true
          || (marker.maximumActionDispatched === true
            && childInvalidPatchCurrent);
      marker.maximumRecoveryObserved =
        marker.maximumRecoveryObserved === true
          || (marker.maximumRecoveryActionDispatched === true
            && marker.maximumRecoveryBrowserChangeObserved === true
            && validStateCurrent);
      marker.javaIncompleteObserved =
        marker.javaIncompleteObserved === true
          || (marker.incompleteActionDispatched === true
            && javaIncompleteCurrent);
      marker.incompleteInvalidPatchObserved =
        marker.incompleteInvalidPatchObserved === true
          || (marker.incompleteActionDispatched === true
            && incompleteInvalidPatchCurrent);
      marker.incompleteErrorMessagePatchObserved =
        marker.incompleteErrorMessagePatchObserved === true
          || (marker.incompleteActionDispatched === true
            && incompleteErrorMessagePatchCurrent);
      marker.incompleteChildInvalidPatchObserved =
        marker.incompleteChildInvalidPatchObserved === true
          || (marker.incompleteActionDispatched === true
            && childInvalidPatchCurrent);
      marker.incompleteRecoveryObserved =
        marker.incompleteRecoveryObserved === true
          || (marker.incompleteRecoveryActionDispatched === true
            && marker.incompleteRecoveryBrowserChangeObserved === true
            && validStateCurrent);
      marker.javaRequiredObserved = marker.javaRequiredObserved === true
        || (marker.requiredActionDispatched === true
          && javaRequiredCurrent);
      marker.requiredInvalidPatchObserved =
        marker.requiredInvalidPatchObserved === true
          || (marker.requiredActionDispatched === true
            && requiredInvalidPatchCurrent);
      marker.requiredErrorMessagePatchObserved =
        marker.requiredErrorMessagePatchObserved === true
          || (marker.requiredActionDispatched === true
            && requiredErrorMessagePatchCurrent);
      marker.requiredChildInvalidPatchObserved =
        marker.requiredChildInvalidPatchObserved === true
          || (marker.requiredActionDispatched === true
            && childInvalidPatchCurrent);
      marker.javaMalformedValidationObserved =
        marker.javaMalformedValidationObserved === true
          || (marker.malformedActionDispatched === true
            && javaMalformedValidationCurrent);
      marker.malformedInvalidPatchObserved =
        marker.malformedInvalidPatchObserved === true
          || (marker.malformedActionDispatched === true
            && malformedInvalidPatchCurrent);
      marker.malformedErrorMessagePatchObserved =
        marker.malformedErrorMessagePatchObserved === true
          || (marker.malformedActionDispatched === true
            && malformedErrorMessagePatchCurrent);
      marker.malformedChildInvalidPatchObserved =
        marker.malformedChildInvalidPatchObserved === true
          || (marker.malformedActionDispatched === true
            && childInvalidPatchCurrent);
      marker.finalJavaRecoveryObserved =
        marker.finalJavaRecoveryObserved === true
          || (marker.finalRecoveryActionDispatched === true
            && validJavaCurrent);
      marker.finalValuePatchObserved =
        marker.finalValuePatchObserved === true
          || (marker.finalRecoveryActionDispatched === true
            && validValuePatchCurrent);
      marker.finalValidPatchObserved =
        marker.finalValidPatchObserved === true
          || (marker.finalRecoveryActionDispatched === true
            && validPatchCurrent);
      marker.finalErrorClearedObserved =
        marker.finalErrorClearedObserved === true
          || (marker.finalRecoveryActionDispatched === true
            && errorClearedCurrent);
      marker.finalChildValidPatchObserved =
        marker.finalChildValidPatchObserved === true
          || (marker.finalRecoveryActionDispatched === true
            && childValidPatchCurrent);
      marker.overlaysClosedObserved =
        marker.overlaysClosedObserved === true
          || (marker.finalRecoveryActionDispatched === true
            && overlaysClosedCurrent);

      const clients = window.Vaadin?.Flow?.clients
        ? Object.keys(window.Vaadin.Flow.clients)
          .filter((name) => name !== 'TypeScript')
        : [];
      return JSON.stringify({
        rootRendered:
          !!document.querySelector('#framework-root')
            || !!document.querySelector('#framework-flow-input-popup-route'),
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
            && !!document.querySelector('#framework-flow-input-popup-route')
            && !!picker,
        path: window.location.pathname,
        initialStateObserved: marker.initialStateObserved === true,
        constraintPropertiesObserved:
          marker.constraintPropertiesObserved === true,
        invalidActionDispatched:
          marker.invalidActionDispatched === true,
        minimumBrowserChangeObserved:
          marker.minimumBrowserChangeObserved === true,
        javaInvalidObserved: marker.javaInvalidObserved === true,
        invalidPatchObserved: marker.invalidPatchObserved === true,
        errorMessagePatchObserved:
          marker.errorMessagePatchObserved === true,
        childInvalidPatchObserved:
          marker.childInvalidPatchObserved === true,
        recoveryActionDispatched:
          marker.recoveryActionDispatched === true,
        minimumRecoveryBrowserChangeObserved:
          marker.minimumRecoveryBrowserChangeObserved === true,
        javaRecoveryObserved: marker.javaRecoveryObserved === true,
        recoveredValuePatchObserved:
          marker.recoveredValuePatchObserved === true,
        validPatchObserved: marker.validPatchObserved === true,
        errorClearedObserved: marker.errorClearedObserved === true,
        childValidPatchObserved:
          marker.childValidPatchObserved === true,
        maximumActionDispatched:
          marker.maximumActionDispatched === true,
        maximumBrowserChangeObserved:
          marker.maximumBrowserChangeObserved === true,
        javaMaximumObserved: marker.javaMaximumObserved === true,
        maximumInvalidPatchObserved:
          marker.maximumInvalidPatchObserved === true,
        maximumErrorMessagePatchObserved:
          marker.maximumErrorMessagePatchObserved === true,
        maximumChildInvalidPatchObserved:
          marker.maximumChildInvalidPatchObserved === true,
        maximumRecoveryActionDispatched:
          marker.maximumRecoveryActionDispatched === true,
        maximumRecoveryBrowserChangeObserved:
          marker.maximumRecoveryBrowserChangeObserved === true,
        maximumRecoveryObserved:
          marker.maximumRecoveryObserved === true,
        incompleteActionDispatched:
          marker.incompleteActionDispatched === true,
        incompleteBrowserChangeObserved:
          marker.incompleteBrowserChangeObserved === true,
        javaIncompleteObserved:
          marker.javaIncompleteObserved === true,
        incompleteInvalidPatchObserved:
          marker.incompleteInvalidPatchObserved === true,
        incompleteErrorMessagePatchObserved:
          marker.incompleteErrorMessagePatchObserved === true,
        incompleteChildInvalidPatchObserved:
          marker.incompleteChildInvalidPatchObserved === true,
        incompleteRecoveryActionDispatched:
          marker.incompleteRecoveryActionDispatched === true,
        incompleteRecoveryBrowserChangeObserved:
          marker.incompleteRecoveryBrowserChangeObserved === true,
        incompleteRecoveryObserved:
          marker.incompleteRecoveryObserved === true,
        requiredActionDispatched:
          marker.requiredActionDispatched === true,
        requiredBrowserChangeObserved:
          marker.requiredBrowserChangeObserved === true,
        javaRequiredObserved:
          marker.javaRequiredObserved === true,
        requiredInvalidPatchObserved:
          marker.requiredInvalidPatchObserved === true,
        requiredErrorMessagePatchObserved:
          marker.requiredErrorMessagePatchObserved === true,
        requiredChildInvalidPatchObserved:
          marker.requiredChildInvalidPatchObserved === true,
        malformedActionDispatched:
          marker.malformedActionDispatched === true,
        malformedInputEventObserved:
          marker.malformedInputEventObserved === true,
        malformedChildUnparsableEventObserved:
          marker.malformedChildUnparsableEventObserved === true,
        malformedParentUnparsableEventObserved:
          marker.malformedParentUnparsableEventObserved === true,
        javaMalformedValidationObserved:
          marker.javaMalformedValidationObserved === true,
        malformedInvalidPatchObserved:
          marker.malformedInvalidPatchObserved === true,
        malformedErrorMessagePatchObserved:
          marker.malformedErrorMessagePatchObserved === true,
        malformedChildInvalidPatchObserved:
          marker.malformedChildInvalidPatchObserved === true,
        finalRecoveryActionDispatched:
          marker.finalRecoveryActionDispatched === true,
        finalRecoveryBrowserChangeObserved:
          marker.finalRecoveryBrowserChangeObserved === true,
        finalJavaRecoveryObserved:
          marker.finalJavaRecoveryObserved === true,
        finalValuePatchObserved:
          marker.finalValuePatchObserved === true,
        finalValidPatchObserved:
          marker.finalValidPatchObserved === true,
        finalErrorClearedObserved:
          marker.finalErrorClearedObserved === true,
        finalChildValidPatchObserved:
          marker.finalChildValidPatchObserved === true,
        overlaysClosedObserved:
          marker.overlaysClosedObserved === true,
        pickerValue,
        pickerMin,
        pickerMax,
        pickerInvalid,
        pickerErrorMessage,
        datePickerInvalid,
        timePickerInvalid,
        validationStatus,
        validationEventStatus,
        markerError: String(marker.error || ''),
        browserError: diagnostics.standardBundleBrowserError || '',
        requestError:
          diagnostics.standardBundleOpaqueRequestError || '',
        responseError:
          diagnostics.standardBundleOpaqueResponseError || '',
        pushError: diagnostics.standardBundlePushError || ''
      });
    })()
    """
}
