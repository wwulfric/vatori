import AppKit
import Foundation

extension FlowIpcAcceptanceRuntimeObserver {
    func scheduleMultiSelectComboBoxAcceptance(
        host: WebViewHost,
        context: FlowIpcWindowContext,
        timeoutSeconds: Double
    ) {
        FlowIpcMultiSelectComboBoxAcceptance(
            context: context,
            deadline: Date().addingTimeInterval(timeoutSeconds)
        ).start()
    }
}

private final class FlowIpcMultiSelectComboBoxAcceptance {
    private enum Phase {
        case ready
        case dedicatedRoute
        case popupOpened
        case avaFocused
        case solFocused
        case selected
        case firstEscape
        case popupClosed
        case chipFocused
        case removed
        case removeButtonRemoved
        case clearButtonRemoved
        case filteringOwnerOpened
        case filteringQueryObserved
        case filteringKaiFocused
        case filteringSelected
        case filteringFirstEscape
        case filteringPopupClosed
        case defaultClearOwnerOpened
        case defaultClearQueryObserved
        case defaultClearKaiFocused
        case defaultClearSelected
    }

    private let context: FlowIpcWindowContext
    private let deadline: Date
    private var phase = Phase.ready
    private var popupOpenedObserved = false
    private var avaFocusedObserved = false
    private var solFocusedObserved = false
    private var firstSelectionObserved = false
    private var firstEscapeObserved = false
    private var popupClosedObserved = false
    private var chipFocusedObserved = false
    private var removalObserved = false
    private var removeButtonRemovalObserved = false
    private var clearButtonRemovalObserved = false
    private var filteringOwnerOpenedObserved = false
    private var filteringServerQueryObserved = false
    private var filteringKaiFocusedObserved = false
    private var filteringSelectionObserved = false
    private var filteringFirstEscapeObserved = false
    private var filteringPopupClosedObserved = false
    private var defaultClearOwnerOpenedObserved = false
    private var defaultClearServerQueryObserved = false
    private var defaultClearKaiFocusedObserved = false
    private var defaultClearSelectionObserved = false
    private var defaultClearDrainStablePollCount = 0
    private var defaultClearDrainRequestCount: Int?
    private var defaultClearDrainResponseCount: Int?
    private var selectionUidlRoundTripCount = 0
    private var clearSelectionUidlRoundTripCount = 0
    private var filteringQueryUidlRoundTripCount = 0
    private var filteringSelectionUidlRoundTripCount = 0
    private var defaultClearQueryUidlRoundTripCount = 0
    private var defaultClearSelectionUidlRoundTripCount = 0
    private var selectionRequestBaseline = 0
    private var selectionResponseBaseline = 0
    private var additionStatus = ""
    private var removalStatus = ""
    private var removeButtonRemovalStatus = ""
    private var clearButtonRemovalStatus = ""
    private var filteringServerQueryStatus = ""
    private var filteringSelectionStatus = ""
    private var defaultClearServerQueryStatus = ""
    private var defaultClearSelectionStatus = ""

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
                    code: "multiSelectComboBoxInspectionFailed")
                return
            }
            guard snapshot.errorFree else {
                self.fail(
                    code: "multiSelectComboBoxBrowserRuntimeError",
                    details: snapshot.tracePayload)
                return
            }

            switch self.phase {
            case .ready:
                guard snapshot.ready else {
                    self.retryOrFail(
                        code: "multiSelectComboBoxReadyTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.perform(
                    Self.navigationAction,
                    next: .dedicatedRoute,
                    failureCode: "multiSelectComboBoxNavigationFailed")
            case .dedicatedRoute:
                guard snapshot.dedicatedRouteReady else {
                    self.retryOrFail(
                        code: "multiSelectComboBoxDedicatedRouteTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativePrimaryClick(
                    Self.prepareOwnerPointerAction,
                    stage: "open-owner",
                    next: .popupOpened,
                    failureCode: "multiSelectComboBoxOwnerClickFailed")
            case .popupOpened:
                guard snapshot.popupOpenedObserved,
                      snapshot.popupA11yObserved,
                      snapshot.popupOwnerFocusObserved,
                      snapshot.nativePointerEventCount == 1,
                      snapshot.nativePointerTrustedEventCount == 1 else {
                    self.retryOrFail(
                        code: "multiSelectComboBoxPopupOpenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.popupOpenedObserved = true
                self.performNativeKey(
                    .arrowDown,
                    Self.focusAvaAction,
                    next: .avaFocused,
                    failureCode: "multiSelectComboBoxAvaFocusFailed")
            case .avaFocused:
                guard snapshot.avaOptionFocusedObserved else {
                    self.retryOrFail(
                        code: "multiSelectComboBoxAvaFocusTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.avaFocusedObserved = true
                self.performNativeKey(
                    .arrowDown,
                    Self.focusSolAction,
                    next: .solFocused,
                    failureCode: "multiSelectComboBoxSolFocusFailed")
            case .solFocused:
                guard snapshot.solOptionFocusedObserved else {
                    self.retryOrFail(
                        code: "multiSelectComboBoxSolFocusTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.solFocusedObserved = true
                self.selectionRequestBaseline = snapshot.requestCount
                self.selectionResponseBaseline = snapshot.responseCount
                self.performNativeKey(
                    .returnKey,
                    Self.selectSolAction,
                    next: .selected,
                    failureCode: "multiSelectComboBoxSolSelectionFailed")
            case .selected:
                if !self.firstSelectionObserved {
                    guard snapshot.firstClientSelectionObserved,
                          snapshot.twoTokensObserved,
                          snapshot.popupRetainedAfterSelectionObserved,
                          self.selectionRoundTripObserved(in: snapshot) else {
                        self.retryOrFail(
                            code: "multiSelectComboBoxSelectionTimeout",
                            details: snapshot.tracePayload)
                        return
                    }
                    self.firstSelectionObserved = true
                    self.selectionUidlRoundTripCount += 1
                    self.additionStatus = snapshot.selectionStatus
                }
                self.performNativeKey(
                    .escape,
                    Self.clearOptionFocusAction,
                    next: .firstEscape,
                    failureCode:
                        "multiSelectComboBoxFirstEscapeFailed")
            case .firstEscape:
                guard snapshot.firstEscapeClearedOptionObserved,
                      snapshot.popupRetainedAfterFirstEscapeObserved else {
                    self.retryOrFail(
                        code: "multiSelectComboBoxFirstEscapeTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.firstEscapeObserved = true
                self.performNativeKey(
                    .escape,
                    Self.closePopupAction,
                    next: .popupClosed,
                    failureCode: "multiSelectComboBoxCloseFailed")
            case .popupClosed:
                guard snapshot.popupEscapeClosedObserved,
                      snapshot.popupFocusRestoredObserved,
                      snapshot.twoTokensObserved else {
                    self.retryOrFail(
                        code: "multiSelectComboBoxCloseTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.popupClosedObserved = true
                self.performNativeKey(
                    .backspace,
                    Self.focusLastChipAction,
                    next: .chipFocused,
                    failureCode: "multiSelectComboBoxChipFocusFailed")
            case .chipFocused:
                guard snapshot.solChipFocusedObserved,
                      snapshot.firstBackspacePreservedSelectionObserved,
                      snapshot.popupFocusRestoredObserved else {
                    self.retryOrFail(
                        code: "multiSelectComboBoxChipFocusTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.chipFocusedObserved = true
                self.selectionRequestBaseline = snapshot.requestCount
                self.selectionResponseBaseline = snapshot.responseCount
                self.performNativeKey(
                    .backspace,
                    Self.removeLastChipAction,
                    next: .removed,
                    failureCode: "multiSelectComboBoxChipRemovalFailed")
            case .removed:
                if !self.removalObserved {
                    guard snapshot.secondClientRemovalObserved,
                          snapshot.tokenRemovedObserved,
                          snapshot.finalFocusObserved,
                          self.selectionRoundTripObserved(in: snapshot) else {
                        self.retryOrFail(
                            code: "multiSelectComboBoxRemovalTimeout",
                            details: snapshot.tracePayload)
                        return
                    }
                    self.removalObserved = true
                    self.selectionUidlRoundTripCount += 1
                    self.removalStatus = snapshot.selectionStatus
                    self.selectionRequestBaseline = snapshot.requestCount
                    self.selectionResponseBaseline = snapshot.responseCount
                }
                self.performNativePrimaryClick(
                    Self.prepareRemoveButtonPointerAction,
                    stage: "remove-ava-chip",
                    next: .removeButtonRemoved,
                    failureCode:
                        "multiSelectComboBoxRemoveButtonClickFailed")
            case .removeButtonRemoved:
                if !self.removeButtonRemovalObserved {
                    guard snapshot.removeButtonDeepHitObserved,
                          snapshot.removeButtonPointerReceiptObserved,
                          snapshot.removeButtonPointerTrustedObserved,
                          snapshot.removeButtonFocusAtReceiptObserved,
                          snapshot.removeButtonClientRemovalObserved,
                          snapshot.removeButtonLastTokenRemovedObserved,
                          snapshot.removeButtonFinalFocusClosedObserved,
                          self.selectionRoundTripObserved(in: snapshot) else {
                        self.retryOrFail(
                            code: "multiSelectComboBoxRemoveButtonTimeout",
                            details: snapshot.tracePayload)
                        return
                    }
                    self.removeButtonRemovalObserved = true
                    self.selectionUidlRoundTripCount += 1
                    self.removeButtonRemovalStatus = snapshot.selectionStatus
                    self.selectionRequestBaseline = snapshot.requestCount
                    self.selectionResponseBaseline = snapshot.responseCount
                }
                self.performNativePrimaryClick(
                    Self.prepareClearButtonPointerAction,
                    stage: "clear-selection",
                    next: .clearButtonRemoved,
                    failureCode:
                        "multiSelectComboBoxClearButtonClickFailed")
            case .clearButtonRemoved:
                if !self.clearButtonRemovalObserved {
                    guard snapshot.clearButtonDeepHitObserved,
                          snapshot.clearButtonPointerReceiptObserved,
                          snapshot.clearButtonPointerTrustedObserved,
                          snapshot.clearButtonFocusAtReceiptObserved,
                          snapshot.clearButtonClientRemovalObserved,
                          snapshot.clearButtonAllTokensRemovedObserved,
                          snapshot.clearButtonFinalFocusClosedObserved,
                          self.selectionRoundTripObserved(in: snapshot) else {
                        self.retryOrFail(
                            code: "multiSelectComboBoxClearButtonTimeout",
                            details: snapshot.tracePayload)
                        return
                    }
                    self.clearButtonRemovalObserved = true
                    self.clearSelectionUidlRoundTripCount += 1
                    self.clearButtonRemovalStatus =
                        snapshot.clearSelectionStatus
                    self.selectionRequestBaseline = snapshot.requestCount
                    self.selectionResponseBaseline = snapshot.responseCount
                }
                self.performNativePrimaryClick(
                    Self.prepareFilteringOwnerPointerAction,
                    stage: "open-filter-owner",
                    next: .filteringOwnerOpened,
                    failureCode:
                        "multiSelectComboBoxFilteringOwnerClickFailed")
            case .filteringOwnerOpened:
                guard snapshot.filteringPopupOpenedObserved,
                      snapshot.filteringInitialServerValueObserved,
                      snapshot.nativePointerEventCount == 4,
                      snapshot.nativePointerTrustedEventCount == 4 else {
                    self.retryOrFail(
                        code: "multiSelectComboBoxFilteringOpenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.filteringOwnerOpenedObserved = true
                self.selectionRequestBaseline = snapshot.requestCount
                self.selectionResponseBaseline = snapshot.responseCount
                self.performNativeKey(
                    .letterK,
                    Self.typeFilterKAction,
                    comboSelector:
                        "#framework-flow-multi-select-combo-box-filter-field",
                    expectedInputValue: "k",
                    next: .filteringQueryObserved,
                    failureCode:
                        "multiSelectComboBoxFilteringTextInputFailed")
            case .filteringQueryObserved:
                if !self.filteringServerQueryObserved {
                    guard snapshot.filteringKeyDownTrustedObserved,
                          snapshot.filteringInputTrustedObserved,
                          snapshot.filteringKeyDownBeforeInputObserved,
                          snapshot.filteringServerQueryObserved,
                          snapshot.filteringSingleResultObserved,
                          self.selectionRoundTripObserved(in: snapshot) else {
                        self.retryOrFail(
                            code: "multiSelectComboBoxFilteringQueryTimeout",
                            details: snapshot.tracePayload)
                        return
                    }
                    self.filteringServerQueryObserved = true
                    self.filteringQueryUidlRoundTripCount += 1
                    self.filteringServerQueryStatus =
                        snapshot.filteringServerQueryStatus
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.focusFilteredKaiAction,
                    comboSelector:
                        "#framework-flow-multi-select-combo-box-filter-field",
                    next: .filteringKaiFocused,
                    failureCode:
                        "multiSelectComboBoxFilteringKaiFocusFailed")
            case .filteringKaiFocused:
                guard snapshot.filteringKaiOptionFocusedObserved else {
                    self.retryOrFail(
                        code: "multiSelectComboBoxFilteringKaiFocusTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.filteringKaiFocusedObserved = true
                self.selectionRequestBaseline = snapshot.requestCount
                self.selectionResponseBaseline = snapshot.responseCount
                self.performNativeKey(
                    .returnKey,
                    Self.selectFilteredKaiAction,
                    comboSelector:
                        "#framework-flow-multi-select-combo-box-filter-field",
                    next: .filteringSelected,
                    failureCode:
                        "multiSelectComboBoxFilteringKaiSelectionFailed")
            case .filteringSelected:
                if !self.filteringSelectionObserved {
                    guard snapshot.filteringClientSelectionObserved,
                          snapshot.filteringSelectionPreservedObserved,
                          snapshot.filteringFinalFocusOpenObserved,
                          self.selectionRoundTripObserved(in: snapshot) else {
                        self.retryOrFail(
                            code:
                                "multiSelectComboBoxFilteringSelectionTimeout",
                            details: snapshot.tracePayload)
                        return
                    }
                    self.filteringSelectionObserved = true
                    self.filteringSelectionUidlRoundTripCount += 1
                    self.filteringSelectionStatus =
                        snapshot.filteringSelectionStatus
                    self.selectionRequestBaseline = snapshot.requestCount
                    self.selectionResponseBaseline = snapshot.responseCount
                }
                self.performNativeKey(
                    .escape,
                    Self.clearFilteringOptionFocusAction,
                    comboSelector:
                        "#framework-flow-multi-select-combo-box-filter-field",
                    next: .filteringFirstEscape,
                    failureCode:
                        "multiSelectComboBoxFilteringFirstEscapeFailed")
            case .filteringFirstEscape:
                guard snapshot.filteringFirstEscapeClearedOptionObserved,
                      snapshot
                        .filteringPopupRetainedAfterFirstEscapeObserved else {
                    self.retryOrFail(
                        code:
                            "multiSelectComboBoxFilteringFirstEscapeTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.filteringFirstEscapeObserved = true
                self.performNativeKey(
                    .escape,
                    Self.closeFilteringPopupAction,
                    comboSelector:
                        "#framework-flow-multi-select-combo-box-filter-field",
                    next: .filteringPopupClosed,
                    failureCode:
                        "multiSelectComboBoxFilteringCloseFailed")
            case .filteringPopupClosed:
                guard snapshot.filteringPopupEscapeClosedObserved,
                      snapshot.filteringPopupFocusRestoredObserved,
                      snapshot.filteringSelectionPreservedObserved else {
                    self.retryOrFail(
                        code:
                            "multiSelectComboBoxFilteringCloseTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.filteringPopupClosedObserved = true
                self.performNativePrimaryClick(
                    Self.prepareDefaultClearOwnerPointerAction,
                    stage: "open-default-clear-owner",
                    next: .defaultClearOwnerOpened,
                    failureCode:
                        "multiSelectComboBoxDefaultClearOwnerClickFailed")
            case .defaultClearOwnerOpened:
                guard snapshot.defaultClearPopupOpenedObserved,
                      snapshot.defaultClearInitialServerValueObserved,
                      snapshot.nativePointerEventCount == 5,
                      snapshot.nativePointerTrustedEventCount == 5 else {
                    self.retryOrFail(
                        code: "multiSelectComboBoxDefaultClearOpenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.defaultClearOwnerOpenedObserved = true
                self.selectionRequestBaseline = snapshot.requestCount
                self.selectionResponseBaseline = snapshot.responseCount
                self.performNativeKey(
                    .letterK,
                    Self.typeDefaultClearKAction,
                    comboSelector:
                        "#framework-flow-multi-select-combo-box-default-clear-field",
                    expectedInputValue: "k",
                    textInputPrefix: "defaultClear",
                    next: .defaultClearQueryObserved,
                    failureCode:
                        "multiSelectComboBoxDefaultClearTextInputFailed")
            case .defaultClearQueryObserved:
                if !self.defaultClearServerQueryObserved {
                    guard snapshot.defaultClearKeyDownTrustedObserved,
                          snapshot.defaultClearInputTrustedObserved,
                          snapshot.defaultClearKeyDownBeforeInputObserved,
                          snapshot.defaultClearServerQueryObserved,
                          snapshot.defaultClearSingleResultObserved,
                          self.selectionRoundTripObserved(in: snapshot) else {
                        self.retryOrFail(
                            code:
                                "multiSelectComboBoxDefaultClearQueryTimeout",
                            details: snapshot.tracePayload)
                        return
                    }
                    self.defaultClearServerQueryObserved = true
                    self.defaultClearQueryUidlRoundTripCount += 1
                    self.defaultClearServerQueryStatus =
                        snapshot.defaultClearServerQueryStatus
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.focusDefaultClearKaiAction,
                    comboSelector:
                        "#framework-flow-multi-select-combo-box-default-clear-field",
                    next: .defaultClearKaiFocused,
                    failureCode:
                        "multiSelectComboBoxDefaultClearKaiFocusFailed")
            case .defaultClearKaiFocused:
                guard snapshot.defaultClearKaiOptionFocusedObserved else {
                    self.retryOrFail(
                        code:
                            "multiSelectComboBoxDefaultClearKaiFocusTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.defaultClearKaiFocusedObserved = true
                self.selectionRequestBaseline = snapshot.requestCount
                self.selectionResponseBaseline = snapshot.responseCount
                self.performNativeKey(
                    .returnKey,
                    Self.selectDefaultClearKaiAction,
                    comboSelector:
                        "#framework-flow-multi-select-combo-box-default-clear-field",
                    next: .defaultClearSelected,
                    failureCode:
                        "multiSelectComboBoxDefaultClearKaiSelectionFailed")
            case .defaultClearSelected:
                if !self.defaultClearSelectionObserved {
                    guard snapshot.defaultClearClientSelectionObserved,
                          snapshot.defaultClearFilterResetObserved,
                          snapshot.defaultClearAllResultsRestoredObserved,
                          snapshot
                            .defaultClearRestoredOptionSelectionObserved,
                          snapshot.defaultClearSelectionPreservedObserved,
                          snapshot.defaultClearFinalFocusOpenObserved,
                          self.selectionRoundTripObserved(in: snapshot) else {
                        self.retryOrFail(
                            code:
                                "multiSelectComboBoxDefaultClearSelectionTimeout",
                            details: snapshot.tracePayload)
                        return
                    }
                    self.defaultClearSelectionObserved = true
                    self.defaultClearSelectionUidlRoundTripCount += 1
                    self.defaultClearSelectionStatus =
                        snapshot.defaultClearSelectionStatus
                }
                let balanced = snapshot.requestCount
                    == snapshot.responseCount
                let unchanged = self.defaultClearDrainRequestCount
                    == snapshot.requestCount
                    && self.defaultClearDrainResponseCount
                        == snapshot.responseCount
                self.defaultClearDrainRequestCount = snapshot.requestCount
                self.defaultClearDrainResponseCount = snapshot.responseCount
                if balanced {
                    self.defaultClearDrainStablePollCount = unchanged
                        ? self.defaultClearDrainStablePollCount + 1
                        : 1
                } else {
                    self.defaultClearDrainStablePollCount = 0
                }
                guard self.defaultClearDrainStablePollCount >= 2 else {
                    var details = snapshot.tracePayload
                    details["drainRequestCount"] = snapshot.requestCount
                    details["drainResponseCount"] = snapshot.responseCount
                    details["drainBalanced"] = balanced
                    details["drainStablePollCount"] =
                        self.defaultClearDrainStablePollCount
                    self.retryOrFail(
                        code:
                            "multiSelectComboBoxDefaultClearFinishDrainTimeout",
                        details: details)
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
        comboSelector: String =
            "#framework-flow-multi-select-combo-box-field",
        expectedInputValue: String? = nil,
        textInputPrefix: String = "filtering",
        next: Phase,
        failureCode: String
    ) {
        let observesTextInput = expectedInputValue != nil
        let expectedInputValue = expectedInputValue ?? ""
        let instrumentedScript = """
        (() => {
          const prepared = \(preparationScript);
          if (prepared !== true) return false;
          const marker = window.VaadinDesktopFlowMultiSelectComboBox || {};
          window.VaadinDesktopFlowMultiSelectComboBox = marker;
          const combo = document.querySelector(
            '\(comboSelector)');
          const input = combo?.inputElement
            || combo?.shadowRoot?.querySelector('input');
          if (!combo || !input) return false;
          if (typeof marker.pendingKeyboardHandler === 'function') {
            document.removeEventListener(
              'keydown', marker.pendingKeyboardHandler, true);
          }
          const stage = String(marker.nativeKeyboardStage || '');
          const observesTextInput =
            \(observesTextInput ? "true" : "false");
          const expectedInputValue = '\(expectedInputValue)';
          const textInputPrefix = '\(textInputPrefix)';
          const eventOrderKey = textInputPrefix + 'EventOrder';
          const inputEventCountKey = textInputPrefix + 'InputEventCount';
          const trustedInputEventCountKey =
            textInputPrefix + 'TrustedInputEventCount';
          const inputTrustedObservedKey =
            textInputPrefix + 'InputTrustedObserved';
          const keyDownBeforeInputObservedKey =
            textInputPrefix + 'KeyDownBeforeInputObserved';
          const keyDownTrustedObservedKey =
            textInputPrefix + 'KeyDownTrustedObserved';
          if (typeof marker.pendingTextInputHandler === 'function') {
            document.removeEventListener(
              'input', marker.pendingTextInputHandler, true);
          }
          if (observesTextInput) {
            marker[eventOrderKey] = [];
            const inputHandler = (event) => {
              const path = event.composedPath?.() || [];
              const value = String(input.value || '');
              if (!path.includes(combo) || !path.includes(input)
                  || value !== expectedInputValue) return;
              document.removeEventListener('input', inputHandler, true);
              marker.pendingTextInputHandler = null;
              marker[eventOrderKey].push('input');
              marker[inputEventCountKey] =
                Number(marker[inputEventCountKey] || 0) + 1;
              marker[trustedInputEventCountKey] =
                Number(marker[trustedInputEventCountKey] || 0)
                  + (event.isTrusted === true ? 1 : 0);
              marker[inputTrustedObservedKey] =
                Number(marker[inputEventCountKey] || 0)
                  === Number(marker[trustedInputEventCountKey] || 0);
              marker[keyDownBeforeInputObservedKey] =
                marker[eventOrderKey].join(',') === 'keydown,input';
              marker.nativeTextInputObserved = true;
              marker.nativeTextInputEventCount =
                Number(marker.nativeTextInputEventCount || 0) + 1;
              marker.nativeTextInputTrustedEventCount =
                Number(marker.nativeTextInputTrustedEventCount || 0)
                  + (event.isTrusted === true ? 1 : 0);
              marker.nativeTextInputTrustedObserved =
                Number(marker.nativeTextInputEventCount || 0)
                  === Number(
                    marker.nativeTextInputTrustedEventCount || 0);
              marker.nativeTextInputStages =
                Array.isArray(marker.nativeTextInputStages)
                  ? marker.nativeTextInputStages
                  : [];
              marker.nativeTextInputStages.push(stage);
              marker.nativeTextInputValues =
                Array.isArray(marker.nativeTextInputValues)
                  ? marker.nativeTextInputValues
                  : [];
              marker.nativeTextInputValues.push(value);
            };
            marker.pendingTextInputHandler = inputHandler;
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
              Number(marker.nativeKeyboardTrustedEventCount || 0)
                === Number(marker.nativeKeyboardEventCount || 0);
            marker.nativeKeyboardStages =
              Array.isArray(marker.nativeKeyboardStages)
                ? marker.nativeKeyboardStages
                : [];
            marker.nativeKeyboardStages.push(stage);
            marker.nativeKeyboardKeys =
              Array.isArray(marker.nativeKeyboardKeys)
                ? marker.nativeKeyboardKeys
                : [];
            marker.nativeKeyboardKeys.push(String(event.key || ''));
            if (observesTextInput) {
              marker[eventOrderKey].push('keydown');
              marker[keyDownTrustedObservedKey] =
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
                trace("recv", "multiSelectComboBox.nativeKeyboardDispatch", [
                    "inputMode": "appkit-window-key-event",
                    "key": key.name,
                    "activeApplication": NSApplication.shared.isActive,
                    "keyWindow": self.context.window.isKeyWindow,
                    "firstResponder": String(describing:
                        self.context.window.firstResponder),
                    "windowNumber": self.context.window.windowNumber
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
                        "message":
                            "Multi-select pointer target is outside the window",
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
                trace("recv", "multiSelectComboBox.nativePointerDispatch", [
                    "inputMode": "appkit-window-pointer-event",
                    "button": "primary",
                    "stage": stage,
                    "webViewX": geometry.centerX,
                    "webViewY": geometry.centerY,
                    "hitTarget": geometry.hitTarget ?? "unavailable",
                    "pointStrategy":
                        geometry.pointStrategy ?? "center",
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

    private func selectionRoundTripObserved(in snapshot: Snapshot) -> Bool {
        snapshot.requestCount > selectionRequestBaseline
            && snapshot.responseCount > selectionResponseBaseline
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
        let expectedKeyboardStages =
            "focus-ava,focus-sol,select-sol,escape-clear-option-focus,"
                + "escape-close,focus-sol-chip,remove-sol-chip,"
                + "type-filter-k,focus-filtered-kai,select-filtered-kai,"
                + "escape-clear-filtering-option-focus,"
                + "escape-close-filtering,"
                + "type-default-clear-k,focus-default-clear-kai,"
                + "select-default-clear-kai"
        let expectedKeyboardKeys =
            "ArrowDown,ArrowDown,Enter,Escape,Escape,Backspace,Backspace,"
                + "k,ArrowDown,Enter,Escape,Escape,k,ArrowDown,Enter"
        let nativePointerContract = snapshot.nativePointerObserved
            && snapshot.nativePointerTrustedObserved
            && snapshot.nativePointerEventCount == 5
            && snapshot.nativePointerTrustedEventCount == 5
            && snapshot.nativePointerStages
                == "open-owner,remove-ava-chip,clear-selection,"
                    + "open-filter-owner,open-default-clear-owner"
        let nativeKeyboardContract = snapshot.nativeKeyboardObserved
            && snapshot.nativeKeyboardTrustedObserved
            && snapshot.nativeKeyboardEventCount == 15
            && snapshot.nativeKeyboardTrustedEventCount == 15
            && snapshot.nativeKeyboardStages == expectedKeyboardStages
            && snapshot.nativeKeyboardKeys == expectedKeyboardKeys
        let nativeTextInputContract = snapshot.nativeTextInputObserved
            && snapshot.nativeTextInputTrustedObserved
            && snapshot.nativeTextInputEventCount == 2
            && snapshot.nativeTextInputTrustedEventCount == 2
            && snapshot.nativeTextInputStages
                == "type-filter-k,type-default-clear-k"
            && snapshot.nativeTextInputValues == "k,k"
        let browserSetupContract =
            snapshot.browserProgrammaticSetupObserved
                && snapshot.browserProgrammaticStages
                    == "navigate,position-owner,focus-owner,"
                        + "empty-input-caret-start,"
                        + "position-ava-remove-button,"
                        + "position-clear-button,position-filter-owner,"
                        + "position-default-clear-owner"
        let complete = snapshot.errorFree
            && popupOpenedObserved
            && avaFocusedObserved
            && solFocusedObserved
            && firstSelectionObserved
            && firstEscapeObserved
            && popupClosedObserved
            && chipFocusedObserved
            && removalObserved
            && removeButtonRemovalObserved
            && clearButtonRemovalObserved
            && filteringOwnerOpenedObserved
            && filteringServerQueryObserved
            && filteringKaiFocusedObserved
            && filteringSelectionObserved
            && filteringFirstEscapeObserved
            && filteringPopupClosedObserved
            && defaultClearOwnerOpenedObserved
            && defaultClearServerQueryObserved
            && defaultClearKaiFocusedObserved
            && defaultClearSelectionObserved
            && selectionUidlRoundTripCount == 3
            && clearSelectionUidlRoundTripCount == 1
            && filteringQueryUidlRoundTripCount == 1
            && filteringSelectionUidlRoundTripCount == 1
            && defaultClearQueryUidlRoundTripCount == 1
            && defaultClearSelectionUidlRoundTripCount == 1
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
            "multiSelectComboBoxDedicatedRouteObserved":
                snapshot.routeRendered,
            "multiSelectComboBoxInitialServerValueObserved":
                snapshot.initialServerValueObserved,
            "multiSelectComboBoxClearInitialServerValueObserved":
                snapshot.clearInitialServerValueObserved,
            "multiSelectComboBoxClearButtonVisibleObserved":
                snapshot.clearButtonVisibleObserved,
            "multiSelectComboBoxFilteringInitialServerValueObserved":
                snapshot.filteringInitialServerValueObserved,
            "multiSelectComboBoxDefaultClearInitialServerValueObserved":
                snapshot.defaultClearInitialServerValueObserved,
            "multiSelectComboBoxPopupOpenedObserved": popupOpenedObserved,
            "multiSelectComboBoxPopupA11yObserved":
                snapshot.popupA11yObserved,
            "multiSelectComboBoxPopupOwnerFocusObserved":
                snapshot.popupOwnerFocusObserved,
            "multiSelectComboBoxAvaOptionFocusedObserved":
                avaFocusedObserved,
            "multiSelectComboBoxSolOptionFocusedObserved":
                solFocusedObserved,
            "multiSelectComboBoxFirstClientSelectionObserved":
                firstSelectionObserved,
            "multiSelectComboBoxTwoTokensObserved":
                snapshot.twoTokensObserved,
            "multiSelectComboBoxPopupRetainedAfterSelectionObserved":
                snapshot.popupRetainedAfterSelectionObserved,
            "multiSelectComboBoxFirstEscapeClearedOptionObserved":
                firstEscapeObserved,
            "multiSelectComboBoxPopupRetainedAfterFirstEscapeObserved":
                snapshot.popupRetainedAfterFirstEscapeObserved,
            "multiSelectComboBoxPopupEscapeClosedObserved":
                popupClosedObserved,
            "multiSelectComboBoxPopupFocusRestoredObserved":
                snapshot.popupFocusRestoredObserved,
            "multiSelectComboBoxSolChipFocusedObserved":
                chipFocusedObserved,
            "multiSelectComboBoxFirstBackspacePreservedSelectionObserved":
                snapshot.firstBackspacePreservedSelectionObserved,
            "multiSelectComboBoxSecondClientRemovalObserved":
                removalObserved,
            "multiSelectComboBoxTokenRemovedObserved":
                snapshot.tokenRemovedObserved,
            "multiSelectComboBoxFinalFocusObserved":
                snapshot.finalFocusObserved,
            "multiSelectComboBoxRemoveButtonDeepHitObserved":
                snapshot.removeButtonDeepHitObserved,
            "multiSelectComboBoxRemoveButtonPointerReceiptObserved":
                snapshot.removeButtonPointerReceiptObserved,
            "multiSelectComboBoxRemoveButtonPointerTrustedObserved":
                snapshot.removeButtonPointerTrustedObserved,
            "multiSelectComboBoxRemoveButtonFocusAtReceiptObserved":
                snapshot.removeButtonFocusAtReceiptObserved,
            "multiSelectComboBoxRemoveButtonClientRemovalObserved":
                removeButtonRemovalObserved,
            "multiSelectComboBoxRemoveButtonLastTokenRemovedObserved":
                snapshot.removeButtonLastTokenRemovedObserved,
            "multiSelectComboBoxRemoveButtonFinalFocusClosedObserved":
                snapshot.removeButtonFinalFocusClosedObserved,
            "multiSelectComboBoxClearButtonDeepHitObserved":
                snapshot.clearButtonDeepHitObserved,
            "multiSelectComboBoxClearButtonPointerReceiptObserved":
                snapshot.clearButtonPointerReceiptObserved,
            "multiSelectComboBoxClearButtonPointerTrustedObserved":
                snapshot.clearButtonPointerTrustedObserved,
            "multiSelectComboBoxClearButtonFocusAtReceiptObserved":
                snapshot.clearButtonFocusAtReceiptObserved,
            "multiSelectComboBoxClearButtonClientRemovalObserved":
                clearButtonRemovalObserved,
            "multiSelectComboBoxClearButtonAllTokensRemovedObserved":
                snapshot.clearButtonAllTokensRemovedObserved,
            "multiSelectComboBoxClearButtonFinalFocusClosedObserved":
                snapshot.clearButtonFinalFocusClosedObserved,
            "multiSelectComboBoxFilteringPopupOpenedObserved":
                filteringOwnerOpenedObserved,
            "multiSelectComboBoxFilteringKeyDownTrustedObserved":
                snapshot.filteringKeyDownTrustedObserved,
            "multiSelectComboBoxFilteringInputTrustedObserved":
                snapshot.filteringInputTrustedObserved,
            "multiSelectComboBoxFilteringKeyDownBeforeInputObserved":
                snapshot.filteringKeyDownBeforeInputObserved,
            "multiSelectComboBoxFilteringServerQueryObserved":
                filteringServerQueryObserved,
            "multiSelectComboBoxFilteringSingleResultObserved":
                snapshot.filteringSingleResultObserved,
            "multiSelectComboBoxFilteringKaiOptionFocusedObserved":
                filteringKaiFocusedObserved,
            "multiSelectComboBoxFilteringClientSelectionObserved":
                filteringSelectionObserved,
            "multiSelectComboBoxFilteringSelectionPreservedObserved":
                snapshot.filteringSelectionPreservedObserved,
            "multiSelectComboBoxFilteringFinalFocusOpenObserved":
                snapshot.filteringFinalFocusOpenObserved,
            "multiSelectComboBoxFilteringFirstEscapeClearedOptionObserved":
                filteringFirstEscapeObserved,
            "multiSelectComboBoxFilteringPopupRetainedAfterFirstEscapeObserved":
                snapshot.filteringPopupRetainedAfterFirstEscapeObserved,
            "multiSelectComboBoxFilteringPopupEscapeClosedObserved":
                filteringPopupClosedObserved,
            "multiSelectComboBoxFilteringPopupFocusRestoredObserved":
                snapshot.filteringPopupFocusRestoredObserved,
            "multiSelectComboBoxDefaultClearPopupOpenedObserved":
                defaultClearOwnerOpenedObserved,
            "multiSelectComboBoxDefaultClearKeyDownTrustedObserved":
                snapshot.defaultClearKeyDownTrustedObserved,
            "multiSelectComboBoxDefaultClearInputTrustedObserved":
                snapshot.defaultClearInputTrustedObserved,
            "multiSelectComboBoxDefaultClearKeyDownBeforeInputObserved":
                snapshot.defaultClearKeyDownBeforeInputObserved,
            "multiSelectComboBoxDefaultClearServerQueryObserved":
                defaultClearServerQueryObserved,
            "multiSelectComboBoxDefaultClearSingleResultObserved":
                snapshot.defaultClearSingleResultObserved,
            "multiSelectComboBoxDefaultClearKaiOptionFocusedObserved":
                defaultClearKaiFocusedObserved,
            "multiSelectComboBoxDefaultClearClientSelectionObserved":
                defaultClearSelectionObserved,
            "multiSelectComboBoxDefaultClearFilterResetObserved":
                snapshot.defaultClearFilterResetObserved,
            "multiSelectComboBoxDefaultClearAllResultsRestoredObserved":
                snapshot.defaultClearAllResultsRestoredObserved,
            "multiSelectComboBoxDefaultClearRestoredOptionSelectionObserved":
                snapshot.defaultClearRestoredOptionSelectionObserved,
            "multiSelectComboBoxDefaultClearOptionSelectionMap":
                snapshot.defaultClearOptionSelectionMap,
            "multiSelectComboBoxDefaultClearSelectionPreservedObserved":
                snapshot.defaultClearSelectionPreservedObserved,
            "multiSelectComboBoxDefaultClearFinalFocusOpenObserved":
                snapshot.defaultClearFinalFocusOpenObserved,
            "multiSelectComboBoxSelectionUidlRoundTripsObserved":
                selectionUidlRoundTripCount == 3,
            "multiSelectComboBoxSelectionUidlRoundTripCount":
                selectionUidlRoundTripCount,
            "multiSelectComboBoxClearSelectionUidlRoundTripsObserved":
                clearSelectionUidlRoundTripCount == 1,
            "multiSelectComboBoxClearSelectionUidlRoundTripCount":
                clearSelectionUidlRoundTripCount,
            "multiSelectComboBoxFilteringQueryUidlRoundTripsObserved":
                filteringQueryUidlRoundTripCount == 1,
            "multiSelectComboBoxFilteringQueryUidlRoundTripCount":
                filteringQueryUidlRoundTripCount,
            "multiSelectComboBoxFilteringSelectionUidlRoundTripsObserved":
                filteringSelectionUidlRoundTripCount == 1,
            "multiSelectComboBoxFilteringSelectionUidlRoundTripCount":
                filteringSelectionUidlRoundTripCount,
            "multiSelectComboBoxDefaultClearQueryUidlRoundTripsObserved":
                defaultClearQueryUidlRoundTripCount == 1,
            "multiSelectComboBoxDefaultClearQueryUidlRoundTripCount":
                defaultClearQueryUidlRoundTripCount,
            "multiSelectComboBoxDefaultClearSelectionUidlRoundTripsObserved":
                defaultClearSelectionUidlRoundTripCount == 1,
            "multiSelectComboBoxDefaultClearSelectionUidlRoundTripCount":
                defaultClearSelectionUidlRoundTripCount,
            "multiSelectComboBoxNativePointerObserved":
                snapshot.nativePointerObserved,
            "multiSelectComboBoxNativePointerTrustedObserved":
                snapshot.nativePointerTrustedObserved,
            "multiSelectComboBoxNativePointerEventCount":
                snapshot.nativePointerEventCount,
            "multiSelectComboBoxNativePointerTrustedEventCount":
                snapshot.nativePointerTrustedEventCount,
            "multiSelectComboBoxNativePointerStages":
                snapshot.nativePointerStages,
            "multiSelectComboBoxNativeKeyboardObserved":
                snapshot.nativeKeyboardObserved,
            "multiSelectComboBoxNativeKeyboardTrustedObserved":
                snapshot.nativeKeyboardTrustedObserved,
            "multiSelectComboBoxNativeKeyboardEventCount":
                snapshot.nativeKeyboardEventCount,
            "multiSelectComboBoxNativeKeyboardTrustedEventCount":
                snapshot.nativeKeyboardTrustedEventCount,
            "multiSelectComboBoxNativeKeyboardStages":
                snapshot.nativeKeyboardStages,
            "multiSelectComboBoxNativeKeyboardKeys":
                snapshot.nativeKeyboardKeys,
            "multiSelectComboBoxNativeTextInputObserved":
                snapshot.nativeTextInputObserved,
            "multiSelectComboBoxNativeTextInputTrustedObserved":
                snapshot.nativeTextInputTrustedObserved,
            "multiSelectComboBoxNativeTextInputEventCount":
                snapshot.nativeTextInputEventCount,
            "multiSelectComboBoxNativeTextInputTrustedEventCount":
                snapshot.nativeTextInputTrustedEventCount,
            "multiSelectComboBoxNativeTextInputStages":
                snapshot.nativeTextInputStages,
            "multiSelectComboBoxNativeTextInputValues":
                snapshot.nativeTextInputValues,
            "multiSelectComboBoxBrowserProgrammaticSetupObserved":
                snapshot.browserProgrammaticSetupObserved,
            "multiSelectComboBoxBrowserProgrammaticStages":
                snapshot.browserProgrammaticStages,
            "multiSelectComboBoxJavaSelectionChangeCount":
                snapshot.selectionChangeCount,
            "multiSelectComboBoxSelectedItems": snapshot.selectedItems,
            "multiSelectComboBoxVisibleChipLabels":
                snapshot.visibleChipLabels,
            "multiSelectComboBoxClearJavaSelectionChangeCount":
                snapshot.clearSelectionChangeCount,
            "multiSelectComboBoxClearSelectedItems":
                snapshot.clearSelectedItems,
            "multiSelectComboBoxClearVisibleChipLabels":
                snapshot.clearVisibleChipLabels,
            "multiSelectComboBoxFilteringJavaSelectionChangeCount":
                snapshot.filteringSelectionChangeCount,
            "multiSelectComboBoxFilteringServerQueryCount":
                snapshot.filteringServerQueryCount,
            "multiSelectComboBoxFilteringInputEventCount":
                snapshot.filteringInputEventCount,
            "multiSelectComboBoxFilteringTrustedInputEventCount":
                snapshot.filteringTrustedInputEventCount,
            "multiSelectComboBoxFilteringSelectedItems":
                snapshot.filteringSelectedItems,
            "multiSelectComboBoxFilteringVisibleChipLabels":
                snapshot.filteringVisibleChipLabels,
            "multiSelectComboBoxFilteringInputValue":
                snapshot.filteringInputValue,
            "multiSelectComboBoxFilteringFilterValue":
                snapshot.filteringFilterValue,
            "multiSelectComboBoxFilteringVisibleOptionLabels":
                snapshot.filteringVisibleOptionLabels,
            "multiSelectComboBoxDefaultClearJavaSelectionChangeCount":
                snapshot.defaultClearSelectionChangeCount,
            "multiSelectComboBoxDefaultClearServerQueryCount":
                snapshot.defaultClearServerQueryCount,
            "multiSelectComboBoxDefaultClearInputEventCount":
                snapshot.defaultClearInputEventCount,
            "multiSelectComboBoxDefaultClearTrustedInputEventCount":
                snapshot.defaultClearTrustedInputEventCount,
            "multiSelectComboBoxDefaultClearSelectedItems":
                snapshot.defaultClearSelectedItems,
            "multiSelectComboBoxDefaultClearVisibleChipLabels":
                snapshot.defaultClearVisibleChipLabels,
            "multiSelectComboBoxDefaultClearInputValue":
                snapshot.defaultClearInputValue,
            "multiSelectComboBoxDefaultClearFilterValue":
                snapshot.defaultClearFilterValue,
            "multiSelectComboBoxDefaultClearVisibleOptionLabels":
                snapshot.defaultClearVisibleOptionLabels,
            "multiSelectComboBoxAdditionStatus": additionStatus,
            "multiSelectComboBoxRemovalStatus": removalStatus,
            "multiSelectComboBoxRemoveButtonRemovalStatus":
                removeButtonRemovalStatus,
            "multiSelectComboBoxClearButtonRemovalStatus":
                clearButtonRemovalStatus,
            "multiSelectComboBoxFilteringServerQueryStatus":
                filteringServerQueryStatus,
            "multiSelectComboBoxFilteringSelectionStatus":
                filteringSelectionStatus,
            "multiSelectComboBoxDefaultClearServerQueryStatus":
                defaultClearServerQueryStatus,
            "multiSelectComboBoxDefaultClearSelectionStatus":
                defaultClearSelectionStatus
        ]) { _, replacement in replacement }
        trace("recv", "multiSelectComboBox.frameworkAcceptance", payload)
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
        let hitTarget: String?
        let pointStrategy: String?
    }

    private struct Snapshot: Decodable {
        let rootRouteAvailable: Bool
        let routeRendered: Bool
        let firstMeaningfulPaint: Bool
        let requestCount: Int
        let responseCount: Int
        let pushMessageCount: Int
        let flowClientCount: Int
        let selectionStatus: String
        let selectionChangeCount: Int
        let selectedItems: String
        let visibleChipLabels: String
        let clearSelectionStatus: String
        let clearSelectionChangeCount: Int
        let clearSelectedItems: String
        let clearVisibleChipLabels: String
        let filteringServerQueryStatus: String
        let filteringSelectionStatus: String
        let filteringSelectionChangeCount: Int
        let filteringServerQueryCount: Int
        let filteringSelectedItems: String
        let filteringVisibleChipLabels: String
        let filteringInputValue: String
        let filteringFilterValue: String
        let filteringVisibleOptionLabels: String
        let filteringActiveDescendant: String
        let filteringActiveOptionLabel: String
        let filteringActiveOptionRole: String
        let filteringActiveOptionAriaSelected: String
        let filteringOwnerFocusCurrent: Bool
        let filteringOpenedCurrent: Bool
        let defaultClearServerQueryStatus: String
        let defaultClearSelectionStatus: String
        let defaultClearSelectionChangeCount: Int
        let defaultClearServerQueryCount: Int
        let defaultClearSelectedItems: String
        let defaultClearVisibleChipLabels: String
        let defaultClearInputValue: String
        let defaultClearFilterValue: String
        let defaultClearVisibleOptionLabels: String
        let initialServerValueObserved: Bool
        let clearInitialServerValueObserved: Bool
        let clearButtonVisibleObserved: Bool
        let filteringInitialServerValueObserved: Bool
        let defaultClearInitialServerValueObserved: Bool
        let popupOpenedObserved: Bool
        let popupA11yObserved: Bool
        let popupOwnerFocusObserved: Bool
        let avaOptionFocusedObserved: Bool
        let solOptionFocusedObserved: Bool
        let firstClientSelectionObserved: Bool
        let twoTokensObserved: Bool
        let popupRetainedAfterSelectionObserved: Bool
        let firstEscapeClearedOptionObserved: Bool
        let popupRetainedAfterFirstEscapeObserved: Bool
        let popupEscapeClosedObserved: Bool
        let popupFocusRestoredObserved: Bool
        let solChipFocusedObserved: Bool
        let firstBackspacePreservedSelectionObserved: Bool
        let secondClientRemovalObserved: Bool
        let tokenRemovedObserved: Bool
        let finalFocusObserved: Bool
        let removeButtonDeepHitObserved: Bool
        let removeButtonPointerReceiptObserved: Bool
        let removeButtonPointerTrustedObserved: Bool
        let removeButtonFocusAtReceiptObserved: Bool
        let removeButtonClientRemovalObserved: Bool
        let removeButtonLastTokenRemovedObserved: Bool
        let removeButtonFinalFocusClosedObserved: Bool
        let clearButtonDeepHitObserved: Bool
        let clearButtonPointerReceiptObserved: Bool
        let clearButtonPointerTrustedObserved: Bool
        let clearButtonFocusAtReceiptObserved: Bool
        let clearButtonClientRemovalObserved: Bool
        let clearButtonAllTokensRemovedObserved: Bool
        let clearButtonFinalFocusClosedObserved: Bool
        let filteringPopupOpenedObserved: Bool
        let filteringKeyDownTrustedObserved: Bool
        let filteringInputTrustedObserved: Bool
        let filteringKeyDownBeforeInputObserved: Bool
        let filteringServerQueryObserved: Bool
        let filteringSingleResultObserved: Bool
        let filteringKaiOptionFocusedObserved: Bool
        let filteringClientSelectionObserved: Bool
        let filteringSelectionPreservedObserved: Bool
        let filteringFinalFocusOpenObserved: Bool
        let filteringFirstEscapeClearedOptionObserved: Bool
        let filteringPopupRetainedAfterFirstEscapeObserved: Bool
        let filteringPopupEscapeClosedObserved: Bool
        let filteringPopupFocusRestoredObserved: Bool
        let filteringOwnerDeepHitObserved: Bool
        let filteringOwnerPointerReceiptObserved: Bool
        let filteringOwnerPointerTrustedObserved: Bool
        let filteringOwnerFocusAtReceiptObserved: Bool
        let defaultClearPopupOpenedObserved: Bool
        let defaultClearKeyDownTrustedObserved: Bool
        let defaultClearInputTrustedObserved: Bool
        let defaultClearKeyDownBeforeInputObserved: Bool
        let defaultClearServerQueryObserved: Bool
        let defaultClearSingleResultObserved: Bool
        let defaultClearKaiOptionFocusedObserved: Bool
        let defaultClearClientSelectionObserved: Bool
        let defaultClearFilterResetObserved: Bool
        let defaultClearAllResultsRestoredObserved: Bool
        let defaultClearRestoredOptionSelectionObserved: Bool
        let defaultClearOptionSelectionMap: String
        let defaultClearSelectionPreservedObserved: Bool
        let defaultClearFinalFocusOpenObserved: Bool
        let removeButtonRawEvents: String
        let removeButtonRawPath: String
        let removeButtonRawClientX: Double
        let removeButtonRawClientY: Double
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
        let filteringInputEventCount: Int
        let filteringTrustedInputEventCount: Int
        let defaultClearInputEventCount: Int
        let defaultClearTrustedInputEventCount: Int
        let browserProgrammaticSetupObserved: Bool
        let browserProgrammaticStages: String
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
                && initialServerValueObserved
                && clearInitialServerValueObserved
                && clearButtonVisibleObserved
                && filteringInitialServerValueObserved
                && defaultClearInitialServerValueObserved
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
                "multiSelectComboBoxSelectionStatus": selectionStatus,
                "multiSelectComboBoxSelectedItems": selectedItems,
                "multiSelectComboBoxVisibleChipLabels": visibleChipLabels,
                "multiSelectComboBoxClearSelectionStatus":
                    clearSelectionStatus,
                "multiSelectComboBoxClearSelectedItems": clearSelectedItems,
                "multiSelectComboBoxClearVisibleChipLabels":
                    clearVisibleChipLabels,
                "multiSelectComboBoxFilteringServerQueryStatus":
                    filteringServerQueryStatus,
                "multiSelectComboBoxFilteringSelectionStatus":
                    filteringSelectionStatus,
                "multiSelectComboBoxFilteringServerQueryCount":
                    filteringServerQueryCount,
                "multiSelectComboBoxFilteringSelectedItems":
                    filteringSelectedItems,
                "multiSelectComboBoxFilteringVisibleChipLabels":
                    filteringVisibleChipLabels,
                "multiSelectComboBoxFilteringInputValue":
                    filteringInputValue,
                "multiSelectComboBoxFilteringFilterValue":
                    filteringFilterValue,
                "multiSelectComboBoxFilteringVisibleOptionLabels":
                    filteringVisibleOptionLabels,
                "multiSelectComboBoxFilteringActiveDescendant":
                    filteringActiveDescendant,
                "multiSelectComboBoxFilteringActiveOptionLabel":
                    filteringActiveOptionLabel,
                "multiSelectComboBoxFilteringActiveOptionRole":
                    filteringActiveOptionRole,
                "multiSelectComboBoxFilteringActiveOptionAriaSelected":
                    filteringActiveOptionAriaSelected,
                "multiSelectComboBoxFilteringOwnerFocusCurrent":
                    filteringOwnerFocusCurrent,
                "multiSelectComboBoxFilteringOpenedCurrent":
                    filteringOpenedCurrent,
                "multiSelectComboBoxDefaultClearServerQueryStatus":
                    defaultClearServerQueryStatus,
                "multiSelectComboBoxDefaultClearSelectionStatus":
                    defaultClearSelectionStatus,
                "multiSelectComboBoxDefaultClearJavaSelectionChangeCount":
                    defaultClearSelectionChangeCount,
                "multiSelectComboBoxDefaultClearServerQueryCount":
                    defaultClearServerQueryCount,
                "multiSelectComboBoxDefaultClearSelectedItems":
                    defaultClearSelectedItems,
                "multiSelectComboBoxDefaultClearVisibleChipLabels":
                    defaultClearVisibleChipLabels,
                "multiSelectComboBoxDefaultClearInputValue":
                    defaultClearInputValue,
                "multiSelectComboBoxDefaultClearFilterValue":
                    defaultClearFilterValue,
                "multiSelectComboBoxDefaultClearVisibleOptionLabels":
                    defaultClearVisibleOptionLabels,
                "multiSelectComboBoxDefaultClearOptionSelectionMap":
                    defaultClearOptionSelectionMap,
                "multiSelectComboBoxDefaultClearRestoredOptionSelectionObserved":
                    defaultClearRestoredOptionSelectionObserved,
                "multiSelectComboBoxNativePointerEventCount":
                    nativePointerEventCount,
                "multiSelectComboBoxNativePointerTrustedEventCount":
                    nativePointerTrustedEventCount,
                "multiSelectComboBoxNativePointerStages":
                    nativePointerStages,
                "multiSelectComboBoxNativeKeyboardEventCount":
                    nativeKeyboardEventCount,
                "multiSelectComboBoxNativeKeyboardTrustedEventCount":
                    nativeKeyboardTrustedEventCount,
                "multiSelectComboBoxNativeKeyboardStages":
                    nativeKeyboardStages,
                "multiSelectComboBoxNativeKeyboardKeys":
                    nativeKeyboardKeys,
                "multiSelectComboBoxBrowserProgrammaticStages":
                    browserProgrammaticStages,
                "multiSelectComboBoxRemoveButtonDeepHitObserved":
                    removeButtonDeepHitObserved,
                "multiSelectComboBoxRemoveButtonPointerReceiptObserved":
                    removeButtonPointerReceiptObserved,
                "multiSelectComboBoxRemoveButtonPointerTrustedObserved":
                    removeButtonPointerTrustedObserved,
                "multiSelectComboBoxRemoveButtonFocusAtReceiptObserved":
                    removeButtonFocusAtReceiptObserved,
                "multiSelectComboBoxRemoveButtonClientRemovalObserved":
                    removeButtonClientRemovalObserved,
                "multiSelectComboBoxRemoveButtonLastTokenRemovedObserved":
                    removeButtonLastTokenRemovedObserved,
                "multiSelectComboBoxRemoveButtonFinalFocusClosedObserved":
                    removeButtonFinalFocusClosedObserved,
                "multiSelectComboBoxClearButtonDeepHitObserved":
                    clearButtonDeepHitObserved,
                "multiSelectComboBoxClearButtonPointerReceiptObserved":
                    clearButtonPointerReceiptObserved,
                "multiSelectComboBoxClearButtonPointerTrustedObserved":
                    clearButtonPointerTrustedObserved,
                "multiSelectComboBoxClearButtonFocusAtReceiptObserved":
                    clearButtonFocusAtReceiptObserved,
                "multiSelectComboBoxClearButtonClientRemovalObserved":
                    clearButtonClientRemovalObserved,
                "multiSelectComboBoxClearButtonAllTokensRemovedObserved":
                    clearButtonAllTokensRemovedObserved,
                "multiSelectComboBoxClearButtonFinalFocusClosedObserved":
                    clearButtonFinalFocusClosedObserved,
                "multiSelectComboBoxFilteringPopupOpenedObserved":
                    filteringPopupOpenedObserved,
                "multiSelectComboBoxFilteringKeyDownTrustedObserved":
                    filteringKeyDownTrustedObserved,
                "multiSelectComboBoxFilteringInputTrustedObserved":
                    filteringInputTrustedObserved,
                "multiSelectComboBoxFilteringKeyDownBeforeInputObserved":
                    filteringKeyDownBeforeInputObserved,
                "multiSelectComboBoxFilteringServerQueryObserved":
                    filteringServerQueryObserved,
                "multiSelectComboBoxFilteringSingleResultObserved":
                    filteringSingleResultObserved,
                "multiSelectComboBoxFilteringKaiOptionFocusedObserved":
                    filteringKaiOptionFocusedObserved,
                "multiSelectComboBoxFilteringClientSelectionObserved":
                    filteringClientSelectionObserved,
                "multiSelectComboBoxFilteringSelectionPreservedObserved":
                    filteringSelectionPreservedObserved,
                "multiSelectComboBoxFilteringFinalFocusOpenObserved":
                    filteringFinalFocusOpenObserved,
                "multiSelectComboBoxFilteringFirstEscapeClearedOptionObserved":
                    filteringFirstEscapeClearedOptionObserved,
                "multiSelectComboBoxFilteringPopupRetainedAfterFirstEscapeObserved":
                    filteringPopupRetainedAfterFirstEscapeObserved,
                "multiSelectComboBoxFilteringPopupEscapeClosedObserved":
                    filteringPopupEscapeClosedObserved,
                "multiSelectComboBoxFilteringPopupFocusRestoredObserved":
                    filteringPopupFocusRestoredObserved,
                "multiSelectComboBoxDefaultClearInitialServerValueObserved":
                    defaultClearInitialServerValueObserved,
                "multiSelectComboBoxDefaultClearPopupOpenedObserved":
                    defaultClearPopupOpenedObserved,
                "multiSelectComboBoxDefaultClearKeyDownTrustedObserved":
                    defaultClearKeyDownTrustedObserved,
                "multiSelectComboBoxDefaultClearInputTrustedObserved":
                    defaultClearInputTrustedObserved,
                "multiSelectComboBoxDefaultClearKeyDownBeforeInputObserved":
                    defaultClearKeyDownBeforeInputObserved,
                "multiSelectComboBoxDefaultClearServerQueryObserved":
                    defaultClearServerQueryObserved,
                "multiSelectComboBoxDefaultClearSingleResultObserved":
                    defaultClearSingleResultObserved,
                "multiSelectComboBoxDefaultClearKaiOptionFocusedObserved":
                    defaultClearKaiOptionFocusedObserved,
                "multiSelectComboBoxDefaultClearClientSelectionObserved":
                    defaultClearClientSelectionObserved,
                "multiSelectComboBoxDefaultClearFilterResetObserved":
                    defaultClearFilterResetObserved,
                "multiSelectComboBoxDefaultClearAllResultsRestoredObserved":
                    defaultClearAllResultsRestoredObserved,
                "multiSelectComboBoxDefaultClearSelectionPreservedObserved":
                    defaultClearSelectionPreservedObserved,
                "multiSelectComboBoxDefaultClearFinalFocusOpenObserved":
                    defaultClearFinalFocusOpenObserved,
                "multiSelectComboBoxNativeTextInputEventCount":
                    nativeTextInputEventCount,
                "multiSelectComboBoxNativeTextInputTrustedEventCount":
                    nativeTextInputTrustedEventCount,
                "multiSelectComboBoxNativeTextInputStages":
                    nativeTextInputStages,
                "multiSelectComboBoxNativeTextInputValues":
                    nativeTextInputValues,
                "multiSelectComboBoxDefaultClearInputEventCount":
                    defaultClearInputEventCount,
                "multiSelectComboBoxDefaultClearTrustedInputEventCount":
                    defaultClearTrustedInputEventCount,
                "multiSelectComboBoxRemoveButtonRawEvents":
                    removeButtonRawEvents,
                "multiSelectComboBoxRemoveButtonRawPath":
                    removeButtonRawPath,
                "multiSelectComboBoxRemoveButtonRawClientX":
                    removeButtonRawClientX,
                "multiSelectComboBoxRemoveButtonRawClientY":
                    removeButtonRawClientY,
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
        case backspace
        case letterK

        var name: String {
            switch self {
            case .arrowDown:
                return "ArrowDown"
            case .returnKey:
                return "Enter"
            case .escape:
                return "Escape"
            case .backspace:
                return "Backspace"
            case .letterK:
                return "k"
            }
        }

        private var characters: String {
            switch self {
            case .arrowDown:
                return "\u{f701}"
            case .returnKey:
                return "\r"
            case .escape:
                return "\u{1b}"
            case .backspace:
                return "\u{7f}"
            case .letterK:
                return "k"
            }
        }

        private var keyCode: UInt16 {
            switch self {
            case .arrowDown:
                return 125
            case .returnKey:
                return 36
            case .escape:
                return 53
            case .backspace:
                return 51
            case .letterK:
                return 40
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
        '#framework-flow-multi-select-combo-box-open-route');
      if (!button || typeof button.click !== 'function') return false;
      window.VaadinDesktopFlowMultiSelectComboBox = {
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
        error: ''
      };
      button.click();
      return true;
    })()
    """

    private static let prepareOwnerPointerAction = """
    (() => {
      const marker = window.VaadinDesktopFlowMultiSelectComboBox || {};
      window.VaadinDesktopFlowMultiSelectComboBox = marker;
      const combo = document.querySelector(
        '#framework-flow-multi-select-combo-box-field');
      const input = combo?.inputElement
        || combo?.shadowRoot?.querySelector('input');
      if (!combo || !input || combo.opened === true) return '';
      marker.browserProgrammaticSetupObserved = true;
      marker.browserProgrammaticStages =
        Array.isArray(marker.browserProgrammaticStages)
          ? marker.browserProgrammaticStages
          : [];
      if (!marker.browserProgrammaticStages.includes('position-owner')) {
        marker.browserProgrammaticStages.push('position-owner');
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
          Number(marker.nativePointerTrustedEventCount || 0)
            === Number(marker.nativePointerEventCount || 0);
        marker.nativePointerStages =
          Array.isArray(marker.nativePointerStages)
            ? marker.nativePointerStages
            : [];
        marker.nativePointerStages.push('open-owner');
      };
      marker.pendingPointerHandler = handler;
      document.addEventListener('click', handler, true);
      marker.openDispatched = true;
      return JSON.stringify({ centerX, centerY, targetMatched });
    })()
    """

    private static let prepareRemoveButtonPointerAction = """
    (() => {
      const marker = window.VaadinDesktopFlowMultiSelectComboBox || {};
      window.VaadinDesktopFlowMultiSelectComboBox = marker;
      const combo = document.querySelector(
        '#framework-flow-multi-select-combo-box-field');
      const input = combo?.inputElement
        || combo?.shadowRoot?.querySelector('input');
      const chips = Array.from(combo?.querySelectorAll(
        'vaadin-multi-select-combo-box-chip[slot="chip"]') || []);
      const chip = chips.find((candidate) =>
        String(candidate?.label || candidate?.getAttribute?.('title') || '')
          .replace(/\\s+/g, ' ').trim() === 'ava');
      const removeButton = chip?.shadowRoot?.querySelector(
        '[part~="remove-button"]');
      if (!combo || !input || !chip || !removeButton
          || combo.opened === true) return '';
      const activePath = [];
      let activeRoot = document;
      while (activeRoot?.activeElement) {
        const active = activeRoot.activeElement;
        activePath.push(active);
        activeRoot = active.shadowRoot;
      }
      if (!activePath.includes(input)) return '';
      marker.browserProgrammaticSetupObserved = true;
      marker.browserProgrammaticStages =
        Array.isArray(marker.browserProgrammaticStages)
          ? marker.browserProgrammaticStages
          : [];
      if (!marker.browserProgrammaticStages.includes(
          'position-ava-remove-button')) {
        marker.browserProgrammaticStages.push(
          'position-ava-remove-button');
      }
      chip.scrollIntoView({ block: 'center', inline: 'nearest' });
      const rect = removeButton.getBoundingClientRect();
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
        && (hit === removeButton || removeButton.contains?.(hit));
      marker.removeButtonDeepHitObserved = targetMatched;
      if (typeof marker.pendingRemoveButtonDiagnosticHandler
          === 'function') {
        for (const type of ['mousedown', 'mouseup', 'click']) {
          document.removeEventListener(
            type, marker.pendingRemoveButtonDiagnosticHandler, true);
        }
      }
      marker.removeButtonRawEvents = [];
      marker.removeButtonRawPath = '';
      const diagnosticHandler = (event) => {
        if (Number(event.button) !== 0) return;
        const path = event.composedPath?.() || [];
        marker.removeButtonRawEvents.push(String(event.type || ''));
        marker.removeButtonRawPath = path.map((element) => {
          if (!element) return '';
          const localName = String(element.localName || '');
          const part = String(element.getAttribute?.('part') || '');
          return part ? `${localName}[part=${part}]` : localName;
        }).filter(Boolean).join('>');
        marker.removeButtonRawClientX = Number(event.clientX || 0);
        marker.removeButtonRawClientY = Number(event.clientY || 0);
        if (event.type === 'click') {
          for (const type of ['mousedown', 'mouseup', 'click']) {
            document.removeEventListener(type, diagnosticHandler, true);
          }
          marker.pendingRemoveButtonDiagnosticHandler = null;
        }
      };
      marker.pendingRemoveButtonDiagnosticHandler = diagnosticHandler;
      for (const type of ['mousedown', 'mouseup', 'click']) {
        document.addEventListener(type, diagnosticHandler, true);
      }
      if (typeof marker.pendingPointerHandler === 'function') {
        document.removeEventListener(
          'click', marker.pendingPointerHandler, true);
      }
      const handler = (event) => {
        const path = event.composedPath?.() || [];
        const removeButtonIndex = path.indexOf(removeButton);
        const chipIndex = path.indexOf(chip);
        const comboIndex = path.indexOf(combo);
        if (Number(event.button) !== 0
            || removeButtonIndex < 0
            || chipIndex <= removeButtonIndex
            || comboIndex <= chipIndex) return;
        document.removeEventListener('click', handler, true);
        marker.pendingPointerHandler = null;
        marker.nativePointerObserved = true;
        marker.nativePointerEventCount =
          Number(marker.nativePointerEventCount || 0) + 1;
        marker.nativePointerTrustedEventCount =
          Number(marker.nativePointerTrustedEventCount || 0)
            + (event.isTrusted === true ? 1 : 0);
        marker.nativePointerTrustedObserved =
          Number(marker.nativePointerTrustedEventCount || 0)
            === Number(marker.nativePointerEventCount || 0);
        marker.nativePointerStages =
          Array.isArray(marker.nativePointerStages)
            ? marker.nativePointerStages
            : [];
        marker.nativePointerStages.push('remove-ava-chip');
        const receiptActivePath = [];
        let receiptRoot = document;
        while (receiptRoot?.activeElement) {
          const active = receiptRoot.activeElement;
          receiptActivePath.push(active);
          receiptRoot = active.shadowRoot;
        }
        marker.removeButtonPointerReceiptObserved = true;
        marker.removeButtonPointerTrustedObserved =
          event.isTrusted === true;
        marker.removeButtonFocusAtReceiptObserved =
          receiptActivePath.includes(input);
      };
      marker.pendingPointerHandler = handler;
      document.addEventListener('click', handler, true);
      marker.removeButtonDispatched = targetMatched;
      return JSON.stringify({ centerX, centerY, targetMatched });
    })()
    """

    private static let prepareClearButtonPointerAction = """
    (() => {
      const marker = window.VaadinDesktopFlowMultiSelectComboBox || {};
      window.VaadinDesktopFlowMultiSelectComboBox = marker;
      const combo = document.querySelector(
        '#framework-flow-multi-select-combo-box-clear-field');
      const input = combo?.inputElement
        || combo?.shadowRoot?.querySelector('input');
      const clearButton = combo?.shadowRoot?.querySelector(
        '[part~="clear-button"]');
      if (!combo || !input || !clearButton
          || combo.clearButtonVisible !== true
          || combo.opened === true
          || !Array.isArray(combo.selectedItems)
          || combo.selectedItems.length !== 2) return '';
      marker.browserProgrammaticSetupObserved = true;
      marker.browserProgrammaticStages =
        Array.isArray(marker.browserProgrammaticStages)
          ? marker.browserProgrammaticStages
          : [];
      if (!marker.browserProgrammaticStages.includes(
          'position-clear-button')) {
        marker.browserProgrammaticStages.push('position-clear-button');
      }
      combo.scrollIntoView({ block: 'center', inline: 'center' });
      const rect = clearButton.getBoundingClientRect();
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
        && (hit === clearButton || clearButton.contains?.(hit));
      marker.clearButtonDeepHitObserved = targetMatched;
      if (typeof marker.pendingPointerHandler === 'function') {
        document.removeEventListener(
          'click', marker.pendingPointerHandler, true);
      }
      const handler = (event) => {
        const path = event.composedPath?.() || [];
        const clearButtonIndex = path.indexOf(clearButton);
        const comboIndex = path.indexOf(combo);
        if (Number(event.button) !== 0
            || clearButtonIndex < 0
            || comboIndex <= clearButtonIndex) return;
        document.removeEventListener('click', handler, true);
        marker.pendingPointerHandler = null;
        marker.nativePointerObserved = true;
        marker.nativePointerEventCount =
          Number(marker.nativePointerEventCount || 0) + 1;
        marker.nativePointerTrustedEventCount =
          Number(marker.nativePointerTrustedEventCount || 0)
            + (event.isTrusted === true ? 1 : 0);
        marker.nativePointerTrustedObserved =
          Number(marker.nativePointerTrustedEventCount || 0)
            === Number(marker.nativePointerEventCount || 0);
        marker.nativePointerStages =
          Array.isArray(marker.nativePointerStages)
            ? marker.nativePointerStages
            : [];
        marker.nativePointerStages.push('clear-selection');
        const receiptActivePath = [];
        let receiptRoot = document;
        while (receiptRoot?.activeElement) {
          const active = receiptRoot.activeElement;
          receiptActivePath.push(active);
          receiptRoot = active.shadowRoot;
        }
        marker.clearButtonPointerReceiptObserved = true;
        marker.clearButtonPointerTrustedObserved =
          event.isTrusted === true;
        marker.clearButtonFocusAtReceiptObserved =
          receiptActivePath.includes(input);
      };
      marker.pendingPointerHandler = handler;
      document.addEventListener('click', handler, true);
      marker.clearButtonDispatched = targetMatched;
      return JSON.stringify({ centerX, centerY, targetMatched });
    })()
    """

    private static let prepareFilteringOwnerPointerAction = """
    (() => {
      const marker = window.VaadinDesktopFlowMultiSelectComboBox || {};
      window.VaadinDesktopFlowMultiSelectComboBox = marker;
      const combo = document.querySelector(
        '#framework-flow-multi-select-combo-box-filter-field');
      const input = combo?.inputElement
        || combo?.shadowRoot?.querySelector('input');
      const labels = Array.from(combo?.selectedItems || [])
        .map((item) => String(
          item?.label ?? item?.value ?? item?.text ?? item ?? ''))
        .sort().join(',');
      if (!combo || !input || combo.opened === true
          || combo.keepFilter !== true || labels !== 'ava'
          || String(input.value || '') !== '') return '';
      marker.browserProgrammaticSetupObserved = true;
      marker.browserProgrammaticStages =
        Array.isArray(marker.browserProgrammaticStages)
          ? marker.browserProgrammaticStages
          : [];
      if (!marker.browserProgrammaticStages.includes(
          'position-filter-owner')) {
        marker.browserProgrammaticStages.push('position-filter-owner');
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
      marker.filteringOwnerDeepHitObserved = targetMatched;
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
          Number(marker.nativePointerTrustedEventCount || 0)
            === Number(marker.nativePointerEventCount || 0);
        marker.nativePointerStages =
          Array.isArray(marker.nativePointerStages)
            ? marker.nativePointerStages
            : [];
        marker.nativePointerStages.push('open-filter-owner');
        const activePath = [];
        let activeRoot = document;
        while (activeRoot?.activeElement) {
          const active = activeRoot.activeElement;
          activePath.push(active);
          activeRoot = active.shadowRoot;
        }
        marker.filteringOwnerPointerReceiptObserved = true;
        marker.filteringOwnerPointerTrustedObserved =
          event.isTrusted === true;
        marker.filteringOwnerFocusAtReceiptObserved =
          activePath.includes(input);
      };
      marker.pendingPointerHandler = handler;
      document.addEventListener('click', handler, true);
      marker.filteringOwnerDispatched = targetMatched;
      return JSON.stringify({ centerX, centerY, targetMatched });
    })()
    """

    private static let prepareDefaultClearOwnerPointerAction = """
    (() => {
      const marker = window.VaadinDesktopFlowMultiSelectComboBox || {};
      window.VaadinDesktopFlowMultiSelectComboBox = marker;
      const combo = document.querySelector(
        '#framework-flow-multi-select-combo-box-default-clear-field');
      const input = combo?.inputElement
        || combo?.shadowRoot?.querySelector('input');
      const labels = Array.from(combo?.selectedItems || [])
        .map((item) => String(
          item?.label ?? item?.value ?? item?.text ?? item ?? ''))
        .sort().join(',');
      if (!combo || !input || combo.opened === true
          || combo.keepFilter !== false || labels !== 'ava'
          || String(input.value || '') !== ''
          || String(combo.filter || '') !== '') return '';
      marker.browserProgrammaticSetupObserved = true;
      marker.browserProgrammaticStages =
        Array.isArray(marker.browserProgrammaticStages)
          ? marker.browserProgrammaticStages
          : [];
      if (!marker.browserProgrammaticStages.includes(
          'position-default-clear-owner')) {
        marker.browserProgrammaticStages.push(
          'position-default-clear-owner');
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
      const describeHit = (hit) => {
        if (!hit) return 'none';
        const root = hit.getRootNode?.();
        const owner = root?.host;
        return [
          String(hit.localName || hit.nodeName || 'unknown'),
          hit.id ? `#${hit.id}` : '',
          hit.getAttribute?.('part')
            ? `[part=${hit.getAttribute('part')}]` : '',
          hit.getAttribute?.('role')
            ? `[role=${hit.getAttribute('role')}]` : '',
          owner?.localName ? `@${owner.localName}` : ''
        ].join('');
      };
      const hit = deepHit(centerX, centerY);
      const targetMatched = rect.width > 0 && rect.height > 0
        && centerX >= 0 && centerX <= window.innerWidth
        && centerY >= 0 && centerY <= window.innerHeight
        && (hit === input || input.contains?.(hit));
      marker.defaultClearOwnerDeepHitObserved = targetMatched;
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
          Number(marker.nativePointerTrustedEventCount || 0)
            === Number(marker.nativePointerEventCount || 0);
        marker.nativePointerStages =
          Array.isArray(marker.nativePointerStages)
            ? marker.nativePointerStages
            : [];
        marker.nativePointerStages.push('open-default-clear-owner');
        const activePath = [];
        let activeRoot = document;
        while (activeRoot?.activeElement) {
          const active = activeRoot.activeElement;
          activePath.push(active);
          activeRoot = active.shadowRoot;
        }
        marker.defaultClearOwnerPointerReceiptObserved = true;
        marker.defaultClearOwnerPointerTrustedObserved =
          event.isTrusted === true;
        marker.defaultClearOwnerFocusAtReceiptObserved =
          activePath.includes(input);
      };
      marker.pendingPointerHandler = handler;
      document.addEventListener('click', handler, true);
      marker.defaultClearOwnerDispatched = targetMatched;
      return JSON.stringify({
        centerX,
        centerY,
        targetMatched,
        hitTarget: describeHit(hit),
        pointStrategy: 'center',
        rect: {
          left: rect.left,
          top: rect.top,
          right: rect.right,
          bottom: rect.bottom,
          width: rect.width,
          height: rect.height
        },
        viewport: { width: window.innerWidth, height: window.innerHeight }
      });
    })()
    """

    private static let focusAvaAction = keyboardPreparation(
        stage: "focus-ava",
        flag: "avaFocusDispatched",
        condition: "combo.opened === true")
    private static let focusSolAction = keyboardPreparation(
        stage: "focus-sol",
        flag: "solFocusDispatched",
        condition: "combo.opened === true")
    private static let selectSolAction = keyboardPreparation(
        stage: "select-sol",
        flag: "selectionDispatched",
        condition: "combo.opened === true")
    private static let clearOptionFocusAction = keyboardPreparation(
        stage: "escape-clear-option-focus",
        flag: "firstEscapeDispatched",
        condition: "combo.opened === true")
    private static let closePopupAction = keyboardPreparation(
        stage: "escape-close",
        flag: "closeDispatched",
        condition: "combo.opened === true")
    private static let focusLastChipAction = keyboardPreparation(
        stage: "focus-sol-chip",
        flag: "chipFocusDispatched",
        condition: "combo.opened !== true")
    private static let removeLastChipAction = keyboardPreparation(
        stage: "remove-sol-chip",
        flag: "removalDispatched",
        condition: "combo.opened !== true")
    private static let typeFilterKAction = filteringKeyboardPreparation(
        stage: "type-filter-k",
        flag: "filteringTextDispatched",
        condition: "combo.opened === true && input.value === ''")
    private static let focusFilteredKaiAction =
        filteringKeyboardPreparation(
            stage: "focus-filtered-kai",
            flag: "filteringKaiFocusDispatched",
            condition: "combo.opened === true && input.value === 'k'"
                + " && combo.filter === 'k'"
                + " && marker.filteringServerQueryObserved === true")
    private static let selectFilteredKaiAction =
        filteringKeyboardPreparation(
            stage: "select-filtered-kai",
            flag: "filteringSelectionDispatched",
            condition: "combo.opened === true && input.value === 'kai'"
                + " && combo.filter === 'k'"
                + " && marker.filteringKaiOptionFocusedObserved === true")
    private static let clearFilteringOptionFocusAction =
        filteringKeyboardPreparation(
            stage: "escape-clear-filtering-option-focus",
            flag: "filteringFirstEscapeDispatched",
            condition: "combo.opened === true && input.value === 'k'"
                + " && combo.filter === 'k'"
                + " && marker.filteringFinalFocusOpenObserved === true")
    private static let closeFilteringPopupAction =
        filteringKeyboardPreparation(
            stage: "escape-close-filtering",
            flag: "filteringCloseDispatched",
            condition: "combo.opened === true && input.value === ''"
                + " && combo.filter === 'k'"
                + " && marker."
                + "filteringFirstEscapeClearedOptionObserved === true"
                + " && marker."
                + "filteringPopupRetainedAfterFirstEscapeObserved === true")
    private static let typeDefaultClearKAction =
        filteringKeyboardPreparation(
            stage: "type-default-clear-k",
            flag: "defaultClearTextDispatched",
            condition: "combo.opened === true && input.value === ''"
                + " && combo.filter === '' && combo.keepFilter === false",
            comboSelector:
                "#framework-flow-multi-select-combo-box-default-clear-field")
    private static let focusDefaultClearKaiAction =
        filteringKeyboardPreparation(
            stage: "focus-default-clear-kai",
            flag: "defaultClearKaiFocusDispatched",
            condition: "combo.opened === true && input.value === 'k'"
                + " && combo.filter === 'k'"
                + " && marker.defaultClearServerQueryObserved === true",
            comboSelector:
                "#framework-flow-multi-select-combo-box-default-clear-field")
    private static let selectDefaultClearKaiAction =
        filteringKeyboardPreparation(
            stage: "select-default-clear-kai",
            flag: "defaultClearSelectionDispatched",
            condition: "combo.opened === true && input.value === 'kai'"
                + " && combo.filter === 'k'"
                + " && marker.defaultClearKaiOptionFocusedObserved === true",
            comboSelector:
                "#framework-flow-multi-select-combo-box-default-clear-field")

    private static func keyboardPreparation(
        stage: String,
        flag: String,
        condition: String
    ) -> String {
        """
        (() => {
          const marker = window.VaadinDesktopFlowMultiSelectComboBox || {};
          window.VaadinDesktopFlowMultiSelectComboBox = marker;
          const combo = document.querySelector(
            '#framework-flow-multi-select-combo-box-field');
          const input = combo?.inputElement
            || combo?.shadowRoot?.querySelector('input');
          if (!combo || !input || !(\(condition))) return false;
          marker.browserProgrammaticSetupObserved = true;
          marker.browserProgrammaticStages =
            Array.isArray(marker.browserProgrammaticStages)
              ? marker.browserProgrammaticStages
              : [];
          if (!marker.browserProgrammaticStages.includes('focus-owner')) {
            marker.browserProgrammaticStages.push('focus-owner');
          }
          input.focus({ preventScroll: true });
          const activePath = [];
          let activeRoot = document;
          while (activeRoot?.activeElement) {
            const active = activeRoot.activeElement;
            activePath.push(active);
            activeRoot = active.shadowRoot;
          }
          if (!activePath.includes(input)) {
            return false;
          }
          if ('\(stage)'.startsWith('focus-sol-chip')
              || '\(stage)'.startsWith('remove-sol-chip')) {
            if (!marker.browserProgrammaticStages.includes(
                'empty-input-caret-start')) {
              marker.browserProgrammaticStages.push(
                'empty-input-caret-start');
            }
            input.value = '';
            input.setSelectionRange?.(0, 0);
          }
          marker.\(flag) = true;
          marker.nativeKeyboardStage = '\(stage)';
          return true;
        })()
        """
    }

    private static func filteringKeyboardPreparation(
        stage: String,
        flag: String,
        condition: String,
        comboSelector: String =
            "#framework-flow-multi-select-combo-box-filter-field"
    ) -> String {
        """
        (() => {
          const marker = window.VaadinDesktopFlowMultiSelectComboBox || {};
          window.VaadinDesktopFlowMultiSelectComboBox = marker;
          const combo = document.querySelector(
            '\(comboSelector)');
          const input = combo?.inputElement
            || combo?.shadowRoot?.querySelector('input');
          if (!combo || !input || !(\(condition))) return false;
          const activePath = [];
          let activeRoot = document;
          while (activeRoot?.activeElement) {
            const active = activeRoot.activeElement;
            activePath.push(active);
            activeRoot = active.shadowRoot;
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
      const marker = window.VaadinDesktopFlowMultiSelectComboBox || {};
      window.VaadinDesktopFlowMultiSelectComboBox = marker;
      const combo = document.querySelector(
        '#framework-flow-multi-select-combo-box-field');
      const clearCombo = document.querySelector(
        '#framework-flow-multi-select-combo-box-clear-field');
      const filteringCombo = document.querySelector(
        '#framework-flow-multi-select-combo-box-filter-field');
      const defaultClearCombo = document.querySelector(
        '#framework-flow-multi-select-combo-box-default-clear-field');
      const route = document.querySelector(
        '#framework-flow-multi-select-combo-box-route');
      const input = combo?.inputElement
        || combo?.shadowRoot?.querySelector('input');
      const clearInput = clearCombo?.inputElement
        || clearCombo?.shadowRoot?.querySelector('input');
      const filteringInput = filteringCombo?.inputElement
        || filteringCombo?.shadowRoot?.querySelector('input');
      const defaultClearInput = defaultClearCombo?.inputElement
        || defaultClearCombo?.shadowRoot?.querySelector('input');
      const clearButton = clearCombo?.shadowRoot?.querySelector(
        '[part~="clear-button"]');
      const scroller = combo?.querySelector(
        'vaadin-multi-select-combo-box-scroller');
      const filteringScroller = filteringCombo?.querySelector(
        'vaadin-multi-select-combo-box-scroller');
      const defaultClearScroller = defaultClearCombo?.querySelector(
        'vaadin-multi-select-combo-box-scroller');
      const selectionStatus = document.querySelector(
        '#framework-flow-multi-select-combo-box-selection-status')
        ?.textContent || '';
      const clearSelectionStatus = document.querySelector(
        '#framework-flow-multi-select-combo-box-clear-status')
        ?.textContent || '';
      const filteringServerQueryStatus = document.querySelector(
        '#framework-flow-multi-select-combo-box-filter-query-status')
        ?.textContent || '';
      const filteringSelectionStatus = document.querySelector(
        '#framework-flow-multi-select-combo-box-filter-status')
        ?.textContent || '';
      const defaultClearServerQueryStatus = document.querySelector(
        '#framework-flow-multi-select-combo-box-default-clear-query-status')
        ?.textContent || '';
      const defaultClearSelectionStatus = document.querySelector(
        '#framework-flow-multi-select-combo-box-default-clear-status')
        ?.textContent || '';
      const clients = window.Vaadin && window.Vaadin.Flow
        && window.Vaadin.Flow.clients
          ? Object.keys(window.Vaadin.Flow.clients)
            .filter((name) => name !== 'TypeScript')
          : [];
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
          const children = node.childNodes || [];
          for (const child of children) visit(child);
        };
        visit(rootNode);
        return parts.join(' ').replace(/\\s+/g, ' ').trim();
      };
      const findDeep = (rootNode, predicate) => {
        if (!rootNode) return null;
        if (rootNode.nodeType === Node.ELEMENT_NODE
            && predicate(rootNode)) return rootNode;
        if (rootNode.shadowRoot) {
          const result = findDeep(rootNode.shadowRoot, predicate);
          if (result) return result;
        }
        if (typeof rootNode.assignedElements === 'function') {
          for (const child of rootNode.assignedElements({ flatten: true })) {
            const result = findDeep(child, predicate);
            if (result) return result;
          }
        }
        const children = rootNode.children || rootNode.childNodes || [];
        for (const child of children) {
          const result = findDeep(child, predicate);
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
          const children = node.children || node.childNodes || [];
          for (const child of children) visit(child);
        };
        visit(rootNode);
        return matches;
      };
      const activeDeepElements = () => {
        const elements = [];
        let rootNode = document;
        while (rootNode?.activeElement) {
          const active = rootNode.activeElement;
          elements.push(active);
          rootNode = active.shadowRoot;
        }
        return elements;
      };
      const activePath = activeDeepElements();
      const ownerFocusCurrent = !!input && activePath.includes(input);
      const clearOwnerFocusCurrent = !!clearInput
        && activePath.includes(clearInput);
      const filteringOwnerFocusCurrent = !!filteringInput
        && activePath.includes(filteringInput);
      const defaultClearOwnerFocusCurrent = !!defaultClearInput
        && activePath.includes(defaultClearInput);
      const itemLabel = (item) => String(
        combo?._getItemLabel?.(item)
          ?? item?.label ?? item?.value ?? item?.text ?? item ?? '');
      const clearItemLabel = (item) => String(
        clearCombo?._getItemLabel?.(item)
          ?? item?.label ?? item?.value ?? item?.text ?? item ?? '');
      const filteringItemLabel = (item) => String(
        item?.label ?? item?.value ?? item?.text ?? item ?? '');
      const defaultClearItemLabel = (item) => String(
        item?.label ?? item?.value ?? item?.text ?? item ?? '');
      const selectedItems = Array.from(combo?.selectedItems || [])
        .map(itemLabel).sort().join(',');
      const clearSelectedItems = Array.from(
        clearCombo?.selectedItems || [])
        .map(clearItemLabel).sort().join(',');
      const filteringSelectedItems = Array.from(
        filteringCombo?.selectedItems || [])
        .map(filteringItemLabel).sort().join(',');
      const defaultClearSelectedItems = Array.from(
        defaultClearCombo?.selectedItems || [])
        .map(defaultClearItemLabel).sort().join(',');
      const chips = Array.from(combo?.querySelectorAll(
        'vaadin-multi-select-combo-box-chip[slot="chip"]') || []);
      const clearChips = Array.from(clearCombo?.querySelectorAll(
        'vaadin-multi-select-combo-box-chip[slot="chip"]') || []);
      const filteringChips = Array.from(
        filteringCombo?.querySelectorAll(
          'vaadin-multi-select-combo-box-chip[slot="chip"]') || []);
      const defaultClearChips = Array.from(
        defaultClearCombo?.querySelectorAll(
          'vaadin-multi-select-combo-box-chip[slot="chip"]') || []);
      const visibleChips = chips.filter((chip) => {
        const rect = chip.getBoundingClientRect();
        return rect.width > 0 && rect.height > 0
          && rect.bottom > 0 && rect.top < window.innerHeight;
      });
      const clearVisibleChips = clearChips.filter((chip) => {
        const rect = chip.getBoundingClientRect();
        return rect.width > 0 && rect.height > 0
          && rect.bottom > 0 && rect.top < window.innerHeight;
      });
      const filteringVisibleChips = filteringChips.filter((chip) => {
        const rect = chip.getBoundingClientRect();
        return rect.width > 0 && rect.height > 0
          && rect.bottom > 0 && rect.top < window.innerHeight;
      });
      const defaultClearVisibleChips = defaultClearChips.filter((chip) => {
        const rect = chip.getBoundingClientRect();
        return rect.width > 0 && rect.height > 0
          && rect.bottom > 0 && rect.top < window.innerHeight;
      });
      const chipLabel = (chip) => String(
        chip?.label || chip?.getAttribute?.('title')
          || collectDeepText(chip) || '');
      const visibleChipLabels = visibleChips
        .map(chipLabel).sort().join(',');
      const clearVisibleChipLabels = clearVisibleChips
        .map(chipLabel).sort().join(',');
      const filteringVisibleChipLabels = filteringVisibleChips
        .map(chipLabel).sort().join(',');
      const defaultClearVisibleChipLabels = defaultClearVisibleChips
        .map(chipLabel).sort().join(',');
      const filteringVisibleOptions = findAllDeep(
        filteringScroller,
        (element) => element.getAttribute?.('role') === 'option')
        .filter((element) => {
          const rect = element.getBoundingClientRect();
          return rect.width > 0 && rect.height > 0
            && rect.bottom > 0 && rect.top < window.innerHeight;
        });
      const filteringVisibleOptionLabels = filteringVisibleOptions
        .map((element) => String(
          element.getAttribute?.('aria-label')
            || collectDeepText(element) || ''))
        .map((label) => label.replace(/\\s+/g, ' ').trim())
        .filter(Boolean).sort().join(',');
      const defaultClearVisibleOptions = findAllDeep(
        defaultClearScroller,
        (element) => element.getAttribute?.('role') === 'option')
        .filter((element) => {
          const rect = element.getBoundingClientRect();
          return rect.width > 0 && rect.height > 0
            && rect.bottom > 0 && rect.top < window.innerHeight;
        });
      const defaultClearVisibleOptionLabels = defaultClearVisibleOptions
        .map((element) => String(
          element.getAttribute?.('aria-label')
            || collectDeepText(element) || ''))
        .map((label) => label.replace(/\\s+/g, ' ').trim())
        .filter(Boolean).sort().join(',');
      const defaultClearOptionSelectionMap = defaultClearVisibleOptions
        .map((element) => {
          const label = String(element.getAttribute?.('aria-label')
            || collectDeepText(element) || '')
            .replace(/\\s+/g, ' ').trim();
          return label
            ? `${label}:${String(
              element.getAttribute?.('aria-selected') || '')}`
            : '';
        })
        .filter(Boolean).sort().join(',');
      const focusedChipLabel = chipLabel(
        visibleChips.find((chip) => chip.hasAttribute('focused')));
      const openedCurrent = combo?.opened === true;
      const clearOpenedCurrent = clearCombo?.opened === true;
      const filteringOpenedCurrent = filteringCombo?.opened === true;
      const filteringInputValue = String(filteringInput?.value || '');
      const filteringFilterValue = String(
        filteringCombo?.filter || '');
      const defaultClearOpenedCurrent = defaultClearCombo?.opened === true;
      const defaultClearInputValue = String(
        defaultClearInput?.value || '');
      const defaultClearFilterValue = String(
        defaultClearCombo?.filter || '');
      const clearButtonConfiguredCurrent =
        clearCombo?.clearButtonVisible === true;
      const clearButtonRect = clearButton?.getBoundingClientRect();
      const clearButtonVisibleCurrent = !!clearButtonRect
        && clearButtonRect.width > 0 && clearButtonRect.height > 0
        && getComputedStyle(clearButton).display !== 'none';
      const activeDescendant =
        String(input?.getAttribute?.('aria-activedescendant') || '');
      const activeOption = activeDescendant
        ? findDeep(document, (element) => element.id === activeDescendant)
        : null;
      const activeOptionLabel = activeOption
        ? itemLabel(activeOption.item).replace(/\\s+/g, ' ').trim()
        : '';
      const filteringActiveDescendant = String(
        filteringInput?.getAttribute?.('aria-activedescendant') || '');
      const filteringActiveOption = filteringActiveDescendant
        ? (filteringVisibleOptions.find(
          (element) => element.id === filteringActiveDescendant)
          || findDeep(
            filteringScroller,
            (element) => element.id === filteringActiveDescendant))
        : null;
      const filteringActiveOptionLabel = filteringActiveOption
        ? String(filteringActiveOption.getAttribute?.('aria-label')
          || collectDeepText(filteringActiveOption) || '')
          .replace(/\\s+/g, ' ').trim()
        : '';
      const filteringActiveOptionRole = String(
        filteringActiveOption?.getAttribute?.('role') || '');
      const filteringActiveOptionAriaSelected = String(
        filteringActiveOption?.getAttribute?.('aria-selected') || '');
      const defaultClearActiveDescendant = String(
        defaultClearInput?.getAttribute?.('aria-activedescendant') || '');
      const defaultClearActiveOption = defaultClearActiveDescendant
        ? (defaultClearVisibleOptions.find(
          (element) => element.id === defaultClearActiveDescendant)
          || findDeep(
            defaultClearScroller,
            (element) => element.id === defaultClearActiveDescendant))
        : null;
      const defaultClearActiveOptionLabel = defaultClearActiveOption
        ? String(defaultClearActiveOption.getAttribute?.('aria-label')
          || collectDeepText(defaultClearActiveOption) || '')
          .replace(/\\s+/g, ' ').trim()
        : '';
      const popupA11yCurrent = openedCurrent
        && input?.getAttribute?.('role') === 'combobox'
        && input?.getAttribute?.('aria-autocomplete') === 'list'
        && input?.getAttribute?.('aria-expanded') === 'true'
        && !!scroller?.id
        && input?.getAttribute?.('aria-controls') === scroller.id
        && scroller?.getAttribute?.('role') === 'listbox'
        && scroller?.getAttribute?.('aria-multiselectable') === 'true';
      const collapsedA11yCurrent = !openedCurrent
        && input?.getAttribute?.('aria-expanded') === 'false';
      const clearCollapsedA11yCurrent = !clearOpenedCurrent
        && clearInput?.getAttribute?.('aria-expanded') === 'false';
      const filteringCollapsedA11yCurrent = !filteringOpenedCurrent
        && filteringInput?.getAttribute?.('aria-expanded') === 'false'
        && !filteringInput?.hasAttribute?.('aria-controls');
      const eventOneStatus = selectionStatus ===
        'Multi-select selection: selected=ava,sol change=1'
          + ' added=sol removed=none client=true';
      const eventTwoStatus = selectionStatus ===
        'Multi-select selection: selected=ava change=2'
          + ' added=none removed=sol client=true';
      const eventThreeStatus = selectionStatus ===
        'Multi-select selection: selected=none change=3'
          + ' added=none removed=ava client=true';
      const selectionChangeMatch = selectionStatus.match(
        /(?:^|\\s)change=(\\d+)(?:\\s|$)/);
      const selectionChangeCount = selectionChangeMatch
        ? Number(selectionChangeMatch[1]) : -1;
      const clearEventStatus = clearSelectionStatus ===
        'Multi-select clear: selected=none change=1'
          + ' added=none removed=kai,sol client=true';
      const clearSelectionChangeMatch = clearSelectionStatus.match(
        /(?:^|\\s)change=(\\d+)(?:\\s|$)/);
      const clearSelectionChangeCount = clearSelectionChangeMatch
        ? Number(clearSelectionChangeMatch[1]) : -1;
      const filteringServerQueryStatusCurrent =
        filteringServerQueryStatus ===
          'Multi-select filtering query: filter=k server=true'
            + ' matches=kai selected=ava';
      const filteringSelectionEventStatus =
        filteringSelectionStatus ===
          'Multi-select filtering selection: selected=ava,kai change=1'
            + ' added=kai removed=none client=true';
      const filteringSelectionChangeMatch =
        filteringSelectionStatus.match(
          /(?:^|\\s)change=(\\d+)(?:\\s|$)/);
      const filteringSelectionChangeCount = filteringSelectionChangeMatch
        ? Number(filteringSelectionChangeMatch[1]) : -1;
      const defaultClearServerQueryStatusCurrent =
        defaultClearServerQueryStatus ===
          'Multi-select default-clear query: filter=k server=true'
            + ' matches=kai selected=ava';
      const defaultClearSelectionEventStatus =
        defaultClearSelectionStatus ===
          'Multi-select default-clear selection: selected=ava,kai change=1'
            + ' added=kai removed=none client=true';
      const defaultClearSelectionChangeMatch =
        defaultClearSelectionStatus.match(
          /(?:^|\\s)change=(\\d+)(?:\\s|$)/);
      const defaultClearSelectionChangeCount =
        defaultClearSelectionChangeMatch
          ? Number(defaultClearSelectionChangeMatch[1]) : -1;
      const initialServerValueCurrent = selectedItems === 'ava'
        && visibleChipLabels === 'ava'
        && selectionStatus ===
          'Multi-select selection: selected=ava change=0'
            + ' added=none removed=none client=false';
      const clearInitialServerValueCurrent =
        clearSelectedItems === 'kai,sol'
          && clearVisibleChipLabels === 'kai,sol'
          && clearSelectionStatus ===
            'Multi-select clear: selected=kai,sol change=0'
              + ' added=none removed=none client=false'
          && !clearOpenedCurrent;
      const filteringInitialServerValueCurrent =
        filteringSelectedItems === 'ava'
          && filteringVisibleChipLabels === 'ava'
          && filteringCombo?.keepFilter === true
          && filteringServerQueryStatus ===
            'Multi-select filtering query: filter=none server=false'
              + ' matches=ava,kai,sol selected=ava'
          && filteringSelectionStatus ===
            'Multi-select filtering selection: selected=ava change=0'
              + ' added=none removed=none client=false'
          && !filteringOpenedCurrent;
      const defaultClearInitialServerValueCurrent =
        defaultClearSelectedItems === 'ava'
          && defaultClearVisibleChipLabels === 'ava'
          && defaultClearCombo?.keepFilter === false
          && defaultClearInputValue === ''
          && defaultClearFilterValue === ''
          && defaultClearServerQueryStatus ===
            'Multi-select default-clear query: filter=none server=false'
              + ' matches=ava,kai,sol selected=ava'
          && defaultClearSelectionStatus ===
            'Multi-select default-clear selection: selected=ava change=0'
              + ' added=none removed=none client=false'
          && !defaultClearOpenedCurrent;

      marker.initialServerValueObserved =
        marker.initialServerValueObserved === true
          || initialServerValueCurrent;
      marker.clearInitialServerValueObserved =
        marker.clearInitialServerValueObserved === true
          || clearInitialServerValueCurrent;
      marker.filteringInitialServerValueObserved =
        marker.filteringInitialServerValueObserved === true
          || filteringInitialServerValueCurrent;
      marker.defaultClearInitialServerValueObserved =
        marker.defaultClearInitialServerValueObserved === true
          || defaultClearInitialServerValueCurrent;
      marker.clearButtonVisibleObserved =
        marker.clearButtonVisibleObserved === true
          || (clearInitialServerValueCurrent
            && clearButtonConfiguredCurrent
            && clearButtonVisibleCurrent);
      marker.popupOpenedObserved = marker.popupOpenedObserved === true
        || (marker.openDispatched === true && openedCurrent);
      marker.popupA11yObserved = marker.popupA11yObserved === true
        || popupA11yCurrent;
      marker.popupOwnerFocusObserved =
        marker.popupOwnerFocusObserved === true
          || (marker.openDispatched === true
            && openedCurrent && ownerFocusCurrent);
      marker.avaOptionFocusedObserved =
        marker.avaOptionFocusedObserved === true
          || (marker.avaFocusDispatched === true
            && openedCurrent
            && activeOption?.getAttribute?.('role') === 'option'
            && activeOptionLabel === 'ava'
            && activeOption?.getAttribute?.('aria-selected') === 'true');
      marker.solOptionFocusedObserved =
        marker.solOptionFocusedObserved === true
          || (marker.solFocusDispatched === true
            && openedCurrent
            && activeOption?.getAttribute?.('role') === 'option'
            && activeOptionLabel === 'sol'
            && activeOption?.getAttribute?.('aria-selected') === 'false');
      marker.firstClientSelectionObserved =
        marker.firstClientSelectionObserved === true
          || (marker.selectionDispatched === true
            && eventOneStatus
            && selectedItems === 'ava,sol');
      marker.twoTokensObserved = marker.twoTokensObserved === true
        || (eventOneStatus && visibleChipLabels === 'ava,sol');
      marker.popupRetainedAfterSelectionObserved =
        marker.popupRetainedAfterSelectionObserved === true
          || (eventOneStatus && openedCurrent && ownerFocusCurrent
            && activeOptionLabel === 'sol');
      marker.firstEscapeClearedOptionObserved =
        marker.firstEscapeClearedOptionObserved === true
          || (marker.firstEscapeDispatched === true
            && eventOneStatus && openedCurrent
            && activeDescendant === '');
      marker.popupRetainedAfterFirstEscapeObserved =
        marker.popupRetainedAfterFirstEscapeObserved === true
          || (marker.firstEscapeDispatched === true
            && eventOneStatus && openedCurrent && ownerFocusCurrent);
      marker.popupEscapeClosedObserved =
        marker.popupEscapeClosedObserved === true
          || (marker.closeDispatched === true
            && eventOneStatus && collapsedA11yCurrent);
      marker.popupFocusRestoredObserved =
        marker.popupFocusRestoredObserved === true
          || (marker.closeDispatched === true
            && !openedCurrent && ownerFocusCurrent);
      marker.solChipFocusedObserved =
        marker.solChipFocusedObserved === true
          || (marker.chipFocusDispatched === true
            && eventOneStatus && focusedChipLabel === 'sol'
            && ownerFocusCurrent);
      marker.firstBackspacePreservedSelectionObserved =
        marker.firstBackspacePreservedSelectionObserved === true
          || (marker.chipFocusDispatched === true
            && eventOneStatus
            && selectedItems === 'ava,sol'
            && visibleChipLabels === 'ava,sol');
      marker.secondClientRemovalObserved =
        marker.secondClientRemovalObserved === true
          || (marker.removalDispatched === true
            && eventTwoStatus && selectedItems === 'ava');
      marker.tokenRemovedObserved = marker.tokenRemovedObserved === true
        || (eventTwoStatus && visibleChipLabels === 'ava'
          && !chips.some((chip) => chipLabel(chip) === 'sol'));
      marker.finalFocusObserved = marker.finalFocusObserved === true
        || (eventTwoStatus && !openedCurrent && ownerFocusCurrent);
      marker.removeButtonClientRemovalObserved =
        marker.removeButtonClientRemovalObserved === true
          || (marker.removeButtonDispatched === true
            && eventThreeStatus && selectedItems === '');
      marker.removeButtonLastTokenRemovedObserved =
        marker.removeButtonLastTokenRemovedObserved === true
          || (eventThreeStatus && chips.length === 0
            && visibleChipLabels === '');
      marker.removeButtonFinalFocusClosedObserved =
        marker.removeButtonFinalFocusClosedObserved === true
          || (eventThreeStatus && collapsedA11yCurrent
            && ownerFocusCurrent);
      marker.clearButtonClientRemovalObserved =
        marker.clearButtonClientRemovalObserved === true
          || (marker.clearButtonDispatched === true
            && clearEventStatus && clearSelectedItems === '');
      marker.clearButtonAllTokensRemovedObserved =
        marker.clearButtonAllTokensRemovedObserved === true
          || (clearEventStatus && clearChips.length === 0
            && clearVisibleChipLabels === '');
      marker.clearButtonFinalFocusClosedObserved =
        marker.clearButtonFinalFocusClosedObserved === true
          || (clearEventStatus && clearCollapsedA11yCurrent
            && clearOwnerFocusCurrent && clearButtonConfiguredCurrent
            && !clearButtonVisibleCurrent);
      marker.filteringPopupOpenedObserved =
        marker.filteringPopupOpenedObserved === true
          || (marker.filteringOwnerDispatched === true
            && filteringOpenedCurrent && filteringOwnerFocusCurrent
            && filteringSelectedItems === 'ava'
            && filteringVisibleChipLabels === 'ava'
            && filteringInput?.getAttribute?.('role') === 'combobox'
            && filteringInput?.getAttribute?.('aria-expanded') === 'true'
            && !!filteringScroller?.id
            && filteringInput?.getAttribute?.('aria-controls')
              === filteringScroller.id
            && filteringScroller?.getAttribute?.('role') === 'listbox'
            && filteringScroller?.getAttribute?.('aria-multiselectable')
              === 'true');
      const filteringServerQueryCurrent =
        marker.filteringTextDispatched === true
          && filteringServerQueryStatusCurrent
          && filteringCombo?.filter === 'k'
          && filteringInputValue === 'k'
          && filteringCombo?.loading !== true
          && filteringOpenedCurrent && filteringOwnerFocusCurrent
          && filteringSelectedItems === 'ava'
          && filteringVisibleChipLabels === 'ava'
          && filteringVisibleOptionLabels === 'kai'
          && filteringSelectionChangeCount === 0;
      if (marker.filteringServerQueryObserved !== true
          && filteringServerQueryCurrent) {
        marker.filteringServerQueryObserved = true;
        marker.filteringServerQueryCount =
          Number(marker.filteringServerQueryCount || 0) + 1;
      }
      marker.filteringSingleResultObserved =
        marker.filteringSingleResultObserved === true
          || filteringServerQueryCurrent;
      marker.filteringKaiOptionFocusedObserved =
        marker.filteringKaiOptionFocusedObserved === true
          || (marker.filteringKaiFocusDispatched === true
            && filteringServerQueryStatusCurrent
            && filteringCombo?.filter === 'k'
            && filteringInputValue === 'kai'
            && filteringVisibleOptionLabels === 'kai'
            && filteringSelectedItems === 'ava'
            && filteringVisibleChipLabels === 'ava'
            && filteringOpenedCurrent && filteringOwnerFocusCurrent
            && filteringActiveOptionLabel === 'kai'
            && filteringActiveOption?.getAttribute?.('role') === 'option'
            && filteringActiveOption?.getAttribute?.('aria-selected')
              === 'false');
      marker.filteringClientSelectionObserved =
        marker.filteringClientSelectionObserved === true
          || (marker.filteringSelectionDispatched === true
            && filteringSelectionEventStatus
            && filteringSelectedItems === 'ava,kai');
      marker.filteringSelectionPreservedObserved =
        marker.filteringSelectionPreservedObserved === true
          || (filteringSelectionEventStatus
            && filteringSelectedItems === 'ava,kai'
            && filteringVisibleChipLabels === 'ava,kai');
      marker.filteringFinalFocusOpenObserved =
        marker.filteringFinalFocusOpenObserved === true
          || (filteringSelectionEventStatus
            && filteringServerQueryStatusCurrent
            && filteringCombo?.filter === 'k'
            && filteringInputValue === 'k'
            && filteringVisibleOptionLabels === 'kai'
            && filteringOpenedCurrent && filteringOwnerFocusCurrent
            && filteringActiveOptionLabel === 'kai'
            && filteringActiveOption?.getAttribute?.('role') === 'option'
            && filteringActiveOption?.getAttribute?.('aria-selected')
              === 'true');
      const filteringFirstEscapeStable =
        marker.filteringFirstEscapeDispatched === true
          && filteringSelectionEventStatus
          && filteringFilterValue === 'k'
          && filteringInputValue === ''
          && filteringSelectedItems === 'ava,kai'
          && filteringVisibleChipLabels === 'ava,kai'
          && filteringVisibleOptionLabels === 'kai'
          && filteringOpenedCurrent && filteringOwnerFocusCurrent
          && filteringInput?.getAttribute?.('aria-expanded') === 'true'
          && filteringActiveDescendant === '';
      marker.filteringFirstEscapeClearedOptionObserved =
        marker.filteringFirstEscapeClearedOptionObserved === true
          || filteringFirstEscapeStable;
      marker.filteringPopupRetainedAfterFirstEscapeObserved =
        marker.filteringPopupRetainedAfterFirstEscapeObserved === true
          || filteringFirstEscapeStable;
      const filteringClosedStable =
        marker.filteringCloseDispatched === true
          && filteringSelectionEventStatus
          && filteringFilterValue === ''
          && filteringInputValue === ''
          && filteringSelectedItems === 'ava,kai'
          && filteringVisibleChipLabels === 'ava,kai'
          && filteringVisibleOptionLabels === ''
          && filteringCollapsedA11yCurrent
          && filteringOwnerFocusCurrent
          && filteringActiveDescendant === '';
      marker.filteringPopupEscapeClosedObserved =
        marker.filteringPopupEscapeClosedObserved === true
          || filteringClosedStable;
      marker.filteringPopupFocusRestoredObserved =
        marker.filteringPopupFocusRestoredObserved === true
          || filteringClosedStable;
      marker.defaultClearPopupOpenedObserved =
        marker.defaultClearPopupOpenedObserved === true
          || (marker.defaultClearOwnerDispatched === true
            && defaultClearOpenedCurrent && defaultClearOwnerFocusCurrent
            && defaultClearSelectedItems === 'ava'
            && defaultClearVisibleChipLabels === 'ava'
            && defaultClearInput?.getAttribute?.('role') === 'combobox'
            && defaultClearInput?.getAttribute?.('aria-expanded') === 'true'
            && !!defaultClearScroller?.id
            && defaultClearInput?.getAttribute?.('aria-controls')
              === defaultClearScroller.id
            && defaultClearScroller?.getAttribute?.('role') === 'listbox'
            && defaultClearScroller?.getAttribute?.('aria-multiselectable')
              === 'true');
      const defaultClearServerQueryCurrent =
        marker.defaultClearTextDispatched === true
          && defaultClearServerQueryStatusCurrent
          && defaultClearFilterValue === 'k'
          && defaultClearInputValue === 'k'
          && defaultClearCombo?.loading !== true
          && defaultClearOpenedCurrent && defaultClearOwnerFocusCurrent
          && defaultClearSelectedItems === 'ava'
          && defaultClearVisibleChipLabels === 'ava'
          && defaultClearVisibleOptionLabels === 'kai'
          && defaultClearSelectionChangeCount === 0;
      if (marker.defaultClearServerQueryObserved !== true
          && defaultClearServerQueryCurrent) {
        marker.defaultClearServerQueryObserved = true;
        marker.defaultClearServerQueryCount =
          Number(marker.defaultClearServerQueryCount || 0) + 1;
      }
      marker.defaultClearSingleResultObserved =
        marker.defaultClearSingleResultObserved === true
          || defaultClearServerQueryCurrent;
      marker.defaultClearKaiOptionFocusedObserved =
        marker.defaultClearKaiOptionFocusedObserved === true
          || (marker.defaultClearKaiFocusDispatched === true
            && defaultClearServerQueryStatusCurrent
            && defaultClearFilterValue === 'k'
            && defaultClearInputValue === 'kai'
            && defaultClearVisibleOptionLabels === 'kai'
            && defaultClearSelectedItems === 'ava'
            && defaultClearVisibleChipLabels === 'ava'
            && defaultClearOpenedCurrent && defaultClearOwnerFocusCurrent
            && defaultClearActiveOptionLabel === 'kai'
            && defaultClearActiveOption?.getAttribute?.('role') === 'option'
            && defaultClearActiveOption?.getAttribute?.('aria-selected')
              === 'false');
      marker.defaultClearClientSelectionObserved =
        marker.defaultClearClientSelectionObserved === true
          || (marker.defaultClearSelectionDispatched === true
            && defaultClearSelectionEventStatus
            && defaultClearSelectedItems === 'ava,kai');
      marker.defaultClearFilterResetObserved =
        marker.defaultClearFilterResetObserved === true
          || (defaultClearSelectionEventStatus
            && defaultClearInputValue === ''
            && defaultClearFilterValue === '');
      marker.defaultClearAllResultsRestoredObserved =
        marker.defaultClearAllResultsRestoredObserved === true
          || (defaultClearSelectionEventStatus
            && defaultClearInputValue === ''
            && defaultClearFilterValue === ''
            && defaultClearVisibleOptionLabels === 'ava,kai,sol');
      marker.defaultClearRestoredOptionSelectionObserved =
        marker.defaultClearRestoredOptionSelectionObserved === true
          || (defaultClearSelectionEventStatus
            && defaultClearOptionSelectionMap
              === 'ava:true,kai:true,sol:false');
      marker.defaultClearSelectionPreservedObserved =
        marker.defaultClearSelectionPreservedObserved === true
          || (defaultClearSelectionEventStatus
            && defaultClearSelectedItems === 'ava,kai'
            && defaultClearVisibleChipLabels === 'ava,kai');
      marker.defaultClearFinalFocusOpenObserved =
        marker.defaultClearFinalFocusOpenObserved === true
          || (defaultClearSelectionEventStatus
            && defaultClearInputValue === ''
            && defaultClearFilterValue === ''
            && defaultClearOpenedCurrent && defaultClearOwnerFocusCurrent
            && defaultClearInput?.getAttribute?.('aria-expanded') === 'true'
            && defaultClearInput?.getAttribute?.('aria-controls')
              === defaultClearScroller?.id);

      return JSON.stringify({
        rootRouteAvailable: !!document.querySelector(
          '#framework-flow-multi-select-combo-box-open-route'),
        routeRendered: !!route,
        firstMeaningfulPaint:
          diagnostics.standardBundleFirstMeaningfulPaintReported === true,
        requestCount:
          Number(diagnostics.standardBundleOpaqueRequestCount || 0),
        responseCount:
          Number(diagnostics.standardBundleOpaqueResponseCount || 0),
        pushMessageCount:
          Number(diagnostics.standardBundlePushMessageCount || 0),
        flowClientCount: clients.length,
        selectionStatus,
        selectionChangeCount,
        selectedItems,
        visibleChipLabels,
        clearSelectionStatus,
        clearSelectionChangeCount,
        clearSelectedItems,
        clearVisibleChipLabels,
        filteringServerQueryStatus,
        filteringSelectionStatus,
        filteringSelectionChangeCount,
        filteringServerQueryCount:
          Number(marker.filteringServerQueryCount || 0),
        filteringSelectedItems,
        filteringVisibleChipLabels,
        filteringInputValue,
        filteringFilterValue,
        filteringVisibleOptionLabels,
        filteringActiveDescendant,
        filteringActiveOptionLabel,
        filteringActiveOptionRole,
        filteringActiveOptionAriaSelected,
        filteringOwnerFocusCurrent,
        filteringOpenedCurrent,
        defaultClearServerQueryStatus,
        defaultClearSelectionStatus,
        defaultClearSelectionChangeCount,
        defaultClearServerQueryCount:
          Number(marker.defaultClearServerQueryCount || 0),
        defaultClearSelectedItems,
        defaultClearVisibleChipLabels,
        defaultClearInputValue,
        defaultClearFilterValue,
        defaultClearVisibleOptionLabels,
        defaultClearOptionSelectionMap,
        initialServerValueObserved:
          marker.initialServerValueObserved === true,
        clearInitialServerValueObserved:
          marker.clearInitialServerValueObserved === true,
        clearButtonVisibleObserved:
          marker.clearButtonVisibleObserved === true,
        filteringInitialServerValueObserved:
          marker.filteringInitialServerValueObserved === true,
        defaultClearInitialServerValueObserved:
          marker.defaultClearInitialServerValueObserved === true,
        popupOpenedObserved: marker.popupOpenedObserved === true,
        popupA11yObserved: marker.popupA11yObserved === true,
        popupOwnerFocusObserved:
          marker.popupOwnerFocusObserved === true,
        avaOptionFocusedObserved:
          marker.avaOptionFocusedObserved === true,
        solOptionFocusedObserved:
          marker.solOptionFocusedObserved === true,
        firstClientSelectionObserved:
          marker.firstClientSelectionObserved === true,
        twoTokensObserved: marker.twoTokensObserved === true,
        popupRetainedAfterSelectionObserved:
          marker.popupRetainedAfterSelectionObserved === true,
        firstEscapeClearedOptionObserved:
          marker.firstEscapeClearedOptionObserved === true,
        popupRetainedAfterFirstEscapeObserved:
          marker.popupRetainedAfterFirstEscapeObserved === true,
        popupEscapeClosedObserved:
          marker.popupEscapeClosedObserved === true,
        popupFocusRestoredObserved:
          marker.popupFocusRestoredObserved === true,
        solChipFocusedObserved:
          marker.solChipFocusedObserved === true,
        firstBackspacePreservedSelectionObserved:
          marker.firstBackspacePreservedSelectionObserved === true,
        secondClientRemovalObserved:
          marker.secondClientRemovalObserved === true,
        tokenRemovedObserved: marker.tokenRemovedObserved === true,
        finalFocusObserved: marker.finalFocusObserved === true,
        removeButtonDeepHitObserved:
          marker.removeButtonDeepHitObserved === true,
        removeButtonPointerReceiptObserved:
          marker.removeButtonPointerReceiptObserved === true,
        removeButtonPointerTrustedObserved:
          marker.removeButtonPointerTrustedObserved === true,
        removeButtonFocusAtReceiptObserved:
          marker.removeButtonFocusAtReceiptObserved === true,
        removeButtonClientRemovalObserved:
          marker.removeButtonClientRemovalObserved === true,
        removeButtonLastTokenRemovedObserved:
          marker.removeButtonLastTokenRemovedObserved === true,
        removeButtonFinalFocusClosedObserved:
          marker.removeButtonFinalFocusClosedObserved === true,
        clearButtonDeepHitObserved:
          marker.clearButtonDeepHitObserved === true,
        clearButtonPointerReceiptObserved:
          marker.clearButtonPointerReceiptObserved === true,
        clearButtonPointerTrustedObserved:
          marker.clearButtonPointerTrustedObserved === true,
        clearButtonFocusAtReceiptObserved:
          marker.clearButtonFocusAtReceiptObserved === true,
        clearButtonClientRemovalObserved:
          marker.clearButtonClientRemovalObserved === true,
        clearButtonAllTokensRemovedObserved:
          marker.clearButtonAllTokensRemovedObserved === true,
        clearButtonFinalFocusClosedObserved:
          marker.clearButtonFinalFocusClosedObserved === true,
        filteringPopupOpenedObserved:
          marker.filteringPopupOpenedObserved === true,
        filteringKeyDownTrustedObserved:
          marker.filteringKeyDownTrustedObserved === true,
        filteringInputTrustedObserved:
          marker.filteringInputTrustedObserved === true,
        filteringKeyDownBeforeInputObserved:
          marker.filteringKeyDownBeforeInputObserved === true,
        filteringServerQueryObserved:
          marker.filteringServerQueryObserved === true,
        filteringSingleResultObserved:
          marker.filteringSingleResultObserved === true,
        filteringKaiOptionFocusedObserved:
          marker.filteringKaiOptionFocusedObserved === true,
        filteringClientSelectionObserved:
          marker.filteringClientSelectionObserved === true,
        filteringSelectionPreservedObserved:
          marker.filteringSelectionPreservedObserved === true,
        filteringFinalFocusOpenObserved:
          marker.filteringFinalFocusOpenObserved === true,
        filteringFirstEscapeClearedOptionObserved:
          marker.filteringFirstEscapeClearedOptionObserved === true,
        filteringPopupRetainedAfterFirstEscapeObserved:
          marker.filteringPopupRetainedAfterFirstEscapeObserved === true,
        filteringPopupEscapeClosedObserved:
          marker.filteringPopupEscapeClosedObserved === true,
        filteringPopupFocusRestoredObserved:
          marker.filteringPopupFocusRestoredObserved === true,
        filteringOwnerDeepHitObserved:
          marker.filteringOwnerDeepHitObserved === true,
        filteringOwnerPointerReceiptObserved:
          marker.filteringOwnerPointerReceiptObserved === true,
        filteringOwnerPointerTrustedObserved:
          marker.filteringOwnerPointerTrustedObserved === true,
        filteringOwnerFocusAtReceiptObserved:
          marker.filteringOwnerFocusAtReceiptObserved === true,
        defaultClearPopupOpenedObserved:
          marker.defaultClearPopupOpenedObserved === true,
        defaultClearKeyDownTrustedObserved:
          marker.defaultClearKeyDownTrustedObserved === true,
        defaultClearInputTrustedObserved:
          marker.defaultClearInputTrustedObserved === true,
        defaultClearKeyDownBeforeInputObserved:
          marker.defaultClearKeyDownBeforeInputObserved === true,
        defaultClearServerQueryObserved:
          marker.defaultClearServerQueryObserved === true,
        defaultClearSingleResultObserved:
          marker.defaultClearSingleResultObserved === true,
        defaultClearKaiOptionFocusedObserved:
          marker.defaultClearKaiOptionFocusedObserved === true,
        defaultClearClientSelectionObserved:
          marker.defaultClearClientSelectionObserved === true,
        defaultClearFilterResetObserved:
          marker.defaultClearFilterResetObserved === true,
        defaultClearAllResultsRestoredObserved:
          marker.defaultClearAllResultsRestoredObserved === true,
        defaultClearRestoredOptionSelectionObserved:
          marker.defaultClearRestoredOptionSelectionObserved === true,
        defaultClearSelectionPreservedObserved:
          marker.defaultClearSelectionPreservedObserved === true,
        defaultClearFinalFocusOpenObserved:
          marker.defaultClearFinalFocusOpenObserved === true,
        removeButtonRawEvents:
          Array.isArray(marker.removeButtonRawEvents)
            ? marker.removeButtonRawEvents.join(',') : '',
        removeButtonRawPath:
          String(marker.removeButtonRawPath || ''),
        removeButtonRawClientX:
          Number(marker.removeButtonRawClientX || 0),
        removeButtonRawClientY:
          Number(marker.removeButtonRawClientY || 0),
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
        filteringInputEventCount:
          Number(marker.filteringInputEventCount || 0),
        filteringTrustedInputEventCount:
          Number(marker.filteringTrustedInputEventCount || 0),
        defaultClearInputEventCount:
          Number(marker.defaultClearInputEventCount || 0),
        defaultClearTrustedInputEventCount:
          Number(marker.defaultClearTrustedInputEventCount || 0),
        browserProgrammaticSetupObserved:
          marker.browserProgrammaticSetupObserved === true,
        browserProgrammaticStages:
          Array.isArray(marker.browserProgrammaticStages)
            ? marker.browserProgrammaticStages.join(',') : '',
        browserError: diagnostics.standardBundleBrowserError || '',
        requestError: diagnostics.standardBundleOpaqueRequestError || '',
        responseError: diagnostics.standardBundleOpaqueResponseError || '',
        pushError: diagnostics.standardBundlePushError || ''
      });
    })()
    """
}
