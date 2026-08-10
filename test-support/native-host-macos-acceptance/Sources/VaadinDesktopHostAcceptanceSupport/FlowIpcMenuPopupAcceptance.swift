import AppKit
import Foundation

extension FlowIpcAcceptanceRuntimeObserver {
    func scheduleFlowMenuPopupAcceptance(
        host: WebViewHost,
        context: FlowIpcWindowContext,
        timeoutSeconds: Double
    ) {
        FlowIpcMenuPopupAcceptance(
            context: context,
            deadline: Date().addingTimeInterval(timeoutSeconds)
        ).start()
    }
}

private final class FlowIpcMenuPopupAcceptance {
    private enum Phase {
        case ready
        case dedicatedRoute
        case menuOpened
        case menuMoved
        case menuSelected
        case menuEscapeOpened
        case menuEscapeClosed
        case nestedRootOpened
        case nestedFirstMoved
        case nestedParentFocused
        case nestedChildOpened
        case nestedChildMoved
        case nestedReturned
        case nestedReopened
        case nestedReselected
        case nestedSelected
        case nestedEscapeRootOpened
        case nestedEscapeFirstMoved
        case nestedEscapeParentFocused
        case nestedEscapeChildOpened
        case nestedEscapeChildClosed
        case nestedEscapeRootClosed
        case overflowOpened
        case overflowSelected
        case overflowEscapeOpened
        case overflowEscapeClosed
        case overflowNestedRootOpened
        case overflowNestedParentFocused
        case overflowNestedChildOpened
        case overflowNestedChildMoved
        case overflowNestedReturned
        case overflowNestedReopened
        case overflowNestedReselected
        case overflowNestedSelected
        case overflowNestedEscapeRootOpened
        case overflowNestedEscapeParentFocused
        case overflowNestedEscapeChildOpened
        case overflowNestedEscapeChildClosed
        case overflowNestedEscapeRootClosed
        case overflowExpanded
        case contextOpened
        case contextMoved
        case contextSelected
        case contextEscapeOpened
        case contextEscapeClosed
        case contextNestedRootOpened
        case contextNestedFirstMoved
        case contextNestedParentFocused
        case contextNestedChildOpened
        case contextNestedChildMoved
        case contextNestedReturned
        case contextNestedReopened
        case contextNestedReselected
        case contextNestedSelected
        case contextNestedEscapeRootOpened
        case contextNestedEscapeFirstMoved
        case contextNestedEscapeParentFocused
        case contextNestedEscapeChildOpened
        case contextNestedEscapeChildClosed
        case contextNestedEscapeRootClosed
        case gridViewport
        case gridOpened
        case gridMoved
        case gridSelected
        case gridNestedRootOpened
        case gridNestedFirstMoved
        case gridNestedParentFocused
        case gridNestedChildOpened
        case gridNestedChildMoved
        case gridNestedReturned
        case gridNestedReopened
        case gridNestedReselected
        case gridNestedSelected
        case gridNestedEscapeRootOpened
        case gridNestedEscapeFirstMoved
        case gridNestedEscapeParentFocused
        case gridNestedEscapeChildOpened
        case gridNestedEscapeChildClosed
        case gridNestedEscapeRootClosed
        case gridPolicyHeaderRejected
        case gridPolicyEmptyRejected
        case gridPolicyRowOpened
        case gridPolicyRowSelected
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
                self.retryOrFail(code: "flowMenuPopupInspectionFailed")
                return
            }
            guard snapshot.errorFree else {
                self.fail(
                    code: "flowMenuPopupBrowserRuntimeError",
                    details: snapshot.tracePayload)
                return
            }

            switch self.phase {
            case .ready:
                guard snapshot.ready else {
                    self.retryOrFail(
                        code: "flowMenuPopupReadyTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.perform(
                    Self.navigationScript,
                    next: .dedicatedRoute,
                    failureCode: "flowMenuPopupNavigationFailed")
            case .dedicatedRoute:
                guard snapshot.dedicatedRouteReady else {
                    self.retryOrFail(
                        code: "flowMenuPopupDedicatedRouteTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.openMenuScript,
                    next: .menuOpened,
                    failureCode: "flowMenuPopupMenuOpenFailed")
            case .menuOpened:
                guard snapshot.menuOpenedObserved,
                      snapshot.menuA11yObserved,
                      snapshot.menuOwnerFocusWhileOpenObserved,
                      snapshot.menuExpandedButtonMatchesOwner,
                      snapshot.menuInitialFocusObserved else {
                    self.retryOrFail(
                        code: "flowMenuPopupMenuOpenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.moveMenuScript,
                    next: .menuMoved,
                    failureCode: "flowMenuPopupMenuMoveFailed")
            case .menuMoved:
                guard snapshot.menuKeyboardMoveObserved else {
                    self.retryOrFail(
                        code: "flowMenuPopupMenuMoveTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .returnKey,
                    Self.selectMenuScript,
                    next: .menuSelected,
                    failureCode: "flowMenuPopupMenuSelectionFailed")
            case .menuSelected:
                guard snapshot.menuSelectionObserved,
                      snapshot.menuJavaListenerObserved,
                      snapshot.menuClosedObserved,
                      snapshot.menuFocusRestoredObserved else {
                    self.retryOrFail(
                        code: "flowMenuPopupMenuPatchTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.reopenMenuScript,
                    next: .menuEscapeOpened,
                    failureCode: "flowMenuPopupMenuReopenFailed")
            case .menuEscapeOpened:
                guard snapshot.menuEscapeOpenedObserved,
                      snapshot.menuA11yObserved else {
                    self.retryOrFail(
                        code: "flowMenuPopupMenuReopenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .escape,
                    Self.closeMenuScript,
                    next: .menuEscapeClosed,
                    failureCode: "flowMenuPopupMenuEscapeFailed")
            case .menuEscapeClosed:
                guard snapshot.menuEscapeCloseObserved,
                      snapshot.menuFocusRestoredObserved else {
                    self.retryOrFail(
                        code: "flowMenuPopupMenuEscapeTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.prepareNestedMenuKey(
                        stage: "open-root",
                        rootOpened: false,
                        focusOwner: true),
                    next: .nestedRootOpened,
                    failureCode: "flowMenuPopupNestedRootOpenFailed")
            case .nestedRootOpened:
                guard snapshot.nestedRootOpenedObserved,
                      snapshot.nestedRootInitialFocusObserved else {
                    self.retryOrFail(
                        code: "flowMenuPopupNestedRootOpenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.prepareNestedMenuKey(
                        stage: "move-first",
                        rootOpened: true),
                    next: .nestedFirstMoved,
                    failureCode: "flowMenuPopupNestedFirstMoveFailed")
            case .nestedFirstMoved:
                guard snapshot.nestedFirstMoveObserved else {
                    self.retryOrFail(
                        code: "flowMenuPopupNestedFirstMoveTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.prepareNestedMenuKey(
                        stage: "focus-parent",
                        rootOpened: true),
                    next: .nestedParentFocused,
                    failureCode: "flowMenuPopupNestedParentFocusFailed")
            case .nestedParentFocused:
                guard snapshot.nestedParentFocusObserved else {
                    self.retryOrFail(
                        code: "flowMenuPopupNestedParentFocusTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowRight,
                    Self.prepareNestedMenuKey(
                        stage: "open-child",
                        rootOpened: true,
                        nestedOpened: false),
                    next: .nestedChildOpened,
                    failureCode: "flowMenuPopupNestedChildOpenFailed")
            case .nestedChildOpened:
                guard snapshot.nestedChildOpenedObserved,
                      snapshot.nestedA11yObserved,
                      snapshot.nestedInitialFocusObserved else {
                    self.retryOrFail(
                        code: "flowMenuPopupNestedChildOpenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.prepareNestedMenuKey(
                        stage: "move-child",
                        rootOpened: true,
                        nestedOpened: true),
                    next: .nestedChildMoved,
                    failureCode: "flowMenuPopupNestedChildMoveFailed")
            case .nestedChildMoved:
                guard snapshot.nestedKeyboardMoveObserved else {
                    self.retryOrFail(
                        code: "flowMenuPopupNestedChildMoveTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowLeft,
                    Self.prepareNestedMenuKey(
                        stage: "return-parent",
                        rootOpened: true,
                        nestedOpened: true),
                    next: .nestedReturned,
                    failureCode: "flowMenuPopupNestedLeftReturnFailed")
            case .nestedReturned:
                guard snapshot.nestedLeftReturnObserved,
                      snapshot.nestedParentRemainsOpenObserved,
                      snapshot.nestedParentFocusRestoredObserved else {
                    self.retryOrFail(
                        code: "flowMenuPopupNestedLeftReturnTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowRight,
                    Self.prepareNestedMenuKey(
                        stage: "reopen-child",
                        rootOpened: true,
                        nestedOpened: false),
                    next: .nestedReopened,
                    failureCode: "flowMenuPopupNestedChildReopenFailed")
            case .nestedReopened:
                guard snapshot.nestedReopenedObserved,
                      snapshot.nestedReopenedInitialFocusObserved else {
                    self.retryOrFail(
                        code: "flowMenuPopupNestedChildReopenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.prepareNestedMenuKey(
                        stage: "move-child-for-selection",
                        rootOpened: true,
                        nestedOpened: true),
                    next: .nestedReselected,
                    failureCode:
                        "flowMenuPopupNestedSelectionMoveFailed")
            case .nestedReselected:
                guard snapshot.nestedSelectionMoveObserved else {
                    self.retryOrFail(
                        code: "flowMenuPopupNestedSelectionMoveTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .returnKey,
                    Self.prepareNestedMenuKey(
                        stage: "select-leaf",
                        rootOpened: true,
                        nestedOpened: true),
                    next: .nestedSelected,
                    failureCode: "flowMenuPopupNestedSelectionFailed")
            case .nestedSelected:
                guard snapshot.nestedSelectionObserved,
                      snapshot.nestedJavaListenerObserved,
                      snapshot.nestedClosedObserved,
                      snapshot.nestedFocusRestoredObserved else {
                    self.retryOrFail(
                        code: "flowMenuPopupNestedSelectionPatchTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.prepareNestedMenuKey(
                        stage: "escape-open-root",
                        rootOpened: false,
                        focusOwner: true),
                    next: .nestedEscapeRootOpened,
                    failureCode:
                        "flowMenuPopupNestedEscapeRootOpenFailed")
            case .nestedEscapeRootOpened:
                guard snapshot.nestedEscapeRootOpenedObserved else {
                    self.retryOrFail(
                        code: "flowMenuPopupNestedEscapeRootOpenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.prepareNestedMenuKey(
                        stage: "escape-move-first",
                        rootOpened: true),
                    next: .nestedEscapeFirstMoved,
                    failureCode:
                        "flowMenuPopupNestedEscapeFirstMoveFailed")
            case .nestedEscapeFirstMoved:
                guard snapshot.nestedEscapeFirstMoveObserved else {
                    self.retryOrFail(
                        code: "flowMenuPopupNestedEscapeFirstMoveTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.prepareNestedMenuKey(
                        stage: "escape-focus-parent",
                        rootOpened: true),
                    next: .nestedEscapeParentFocused,
                    failureCode:
                        "flowMenuPopupNestedEscapeParentFocusFailed")
            case .nestedEscapeParentFocused:
                guard snapshot.nestedEscapeParentFocusObserved else {
                    self.retryOrFail(
                        code: "flowMenuPopupNestedEscapeParentFocusTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowRight,
                    Self.prepareNestedMenuKey(
                        stage: "escape-open-child",
                        rootOpened: true,
                        nestedOpened: false),
                    next: .nestedEscapeChildOpened,
                    failureCode:
                        "flowMenuPopupNestedEscapeChildOpenFailed")
            case .nestedEscapeChildOpened:
                guard snapshot.nestedEscapeChildOpenedObserved else {
                    self.retryOrFail(
                        code: "flowMenuPopupNestedEscapeChildOpenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .escape,
                    Self.prepareNestedMenuKey(
                        stage: "escape-close-child",
                        rootOpened: true,
                        nestedOpened: true),
                    next: .nestedEscapeChildClosed,
                    failureCode:
                        "flowMenuPopupNestedEscapeChildCloseFailed")
            case .nestedEscapeChildClosed:
                guard snapshot.nestedEscapeChildCloseObserved,
                      snapshot.nestedEscapeParentRemainsOpenObserved,
                      snapshot.nestedEscapeParentFocusRestoredObserved else {
                    self.retryOrFail(
                        code: "flowMenuPopupNestedEscapeChildCloseTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .escape,
                    Self.prepareNestedMenuKey(
                        stage: "escape-close-root",
                        rootOpened: true,
                        nestedOpened: false),
                    next: .nestedEscapeRootClosed,
                    failureCode:
                        "flowMenuPopupNestedEscapeRootCloseFailed")
            case .nestedEscapeRootClosed:
                guard snapshot.nestedEscapeRootCloseObserved,
                      snapshot.nestedEscapeOwnerFocusRestoredObserved else {
                    self.retryOrFail(
                        code: "flowMenuPopupNestedEscapeRootCloseTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.prepareOverflowMenuKey(
                        stage: "open",
                        opened: false,
                        focusOwner: true),
                    next: .overflowOpened,
                    failureCode: "flowMenuPopupOverflowOpenFailed")
            case .overflowOpened:
                guard snapshot.overflowNarrowObserved,
                      snapshot.overflowOpenedObserved,
                      snapshot.overflowA11yObserved,
                      snapshot.overflowInitialFocusObserved else {
                    self.retryOrFail(
                        code: "flowMenuPopupOverflowOpenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .returnKey,
                    Self.prepareOverflowMenuKey(
                        stage: "select",
                        opened: true),
                    next: .overflowSelected,
                    failureCode: "flowMenuPopupOverflowSelectionFailed")
            case .overflowSelected:
                guard snapshot.overflowSelectionObserved,
                      snapshot.overflowJavaListenerObserved,
                      snapshot.overflowClosedObserved,
                      snapshot.overflowFocusRestoredObserved else {
                    self.retryOrFail(
                        code: "flowMenuPopupOverflowSelectionPatchTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.prepareOverflowMenuKey(
                        stage: "escape-open",
                        opened: false,
                        focusOwner: true),
                    next: .overflowEscapeOpened,
                    failureCode: "flowMenuPopupOverflowEscapeOpenFailed")
            case .overflowEscapeOpened:
                guard snapshot.overflowEscapeOpenedObserved,
                      snapshot.overflowEscapeInitialFocusObserved else {
                    self.retryOrFail(
                        code: "flowMenuPopupOverflowEscapeOpenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .escape,
                    Self.prepareOverflowMenuKey(
                        stage: "escape-close",
                        opened: true),
                    next: .overflowEscapeClosed,
                    failureCode: "flowMenuPopupOverflowEscapeCloseFailed")
            case .overflowEscapeClosed:
                guard snapshot.overflowEscapeCloseObserved,
                      snapshot.overflowEscapeFocusRestoredObserved else {
                    self.retryOrFail(
                        code: "flowMenuPopupOverflowEscapeCloseTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.prepareOverflowNestedMenuKey(
                        stage: "open-root",
                        rootOpened: false,
                        childOpened: false,
                        focusOwner: true),
                    next: .overflowNestedRootOpened,
                    failureCode:
                        "flowMenuPopupOverflowNestedRootOpenFailed")
            case .overflowNestedRootOpened:
                guard snapshot.overflowNestedCollapsedParentObserved,
                      snapshot.overflowNestedRootOpenedObserved,
                      snapshot.overflowNestedRootInitialFocusObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupOverflowNestedRootOpenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.prepareOverflowNestedMenuKey(
                        stage: "focus-parent",
                        rootOpened: true,
                        childOpened: false),
                    next: .overflowNestedParentFocused,
                    failureCode:
                        "flowMenuPopupOverflowNestedParentFocusFailed")
            case .overflowNestedParentFocused:
                guard snapshot.overflowNestedParentFocusObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupOverflowNestedParentFocusTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowRight,
                    Self.prepareOverflowNestedMenuKey(
                        stage: "open-child",
                        rootOpened: true,
                        childOpened: false),
                    next: .overflowNestedChildOpened,
                    failureCode:
                        "flowMenuPopupOverflowNestedChildOpenFailed")
            case .overflowNestedChildOpened:
                guard snapshot.overflowNestedChildOpenedObserved,
                      snapshot.overflowNestedA11yObserved,
                      snapshot.overflowNestedInitialFocusObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupOverflowNestedChildOpenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.prepareOverflowNestedMenuKey(
                        stage: "move-child",
                        rootOpened: true,
                        childOpened: true),
                    next: .overflowNestedChildMoved,
                    failureCode:
                        "flowMenuPopupOverflowNestedChildMoveFailed")
            case .overflowNestedChildMoved:
                guard snapshot.overflowNestedKeyboardMoveObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupOverflowNestedChildMoveTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowLeft,
                    Self.prepareOverflowNestedMenuKey(
                        stage: "return-parent",
                        rootOpened: true,
                        childOpened: true),
                    next: .overflowNestedReturned,
                    failureCode:
                        "flowMenuPopupOverflowNestedLeftReturnFailed")
            case .overflowNestedReturned:
                guard snapshot.overflowNestedLeftReturnObserved,
                      snapshot.overflowNestedParentRemainsOpenObserved,
                      snapshot
                        .overflowNestedParentFocusRestoredObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupOverflowNestedLeftReturnTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowRight,
                    Self.prepareOverflowNestedMenuKey(
                        stage: "reopen-child",
                        rootOpened: true,
                        childOpened: false),
                    next: .overflowNestedReopened,
                    failureCode:
                        "flowMenuPopupOverflowNestedChildReopenFailed")
            case .overflowNestedReopened:
                guard snapshot.overflowNestedReopenedObserved,
                      snapshot
                        .overflowNestedReopenedInitialFocusObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupOverflowNestedChildReopenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.prepareOverflowNestedMenuKey(
                        stage: "move-child-for-selection",
                        rootOpened: true,
                        childOpened: true),
                    next: .overflowNestedReselected,
                    failureCode:
                        "flowMenuPopupOverflowNestedSelectionMoveFailed")
            case .overflowNestedReselected:
                guard snapshot.overflowNestedSelectionMoveObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupOverflowNestedSelectionMoveTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .returnKey,
                    Self.prepareOverflowNestedMenuKey(
                        stage: "select-leaf",
                        rootOpened: true,
                        childOpened: true),
                    next: .overflowNestedSelected,
                    failureCode:
                        "flowMenuPopupOverflowNestedSelectionFailed")
            case .overflowNestedSelected:
                guard snapshot.overflowNestedSelectionObserved,
                      snapshot.overflowNestedJavaListenerObserved,
                      snapshot.overflowNestedClosedObserved,
                      snapshot.overflowNestedFocusRestoredObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupOverflowNestedSelectionPatchTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.prepareOverflowNestedMenuKey(
                        stage: "escape-open-root",
                        rootOpened: false,
                        childOpened: false,
                        focusOwner: true),
                    next: .overflowNestedEscapeRootOpened,
                    failureCode:
                        "flowMenuPopupOverflowNestedEscapeRootOpenFailed")
            case .overflowNestedEscapeRootOpened:
                guard snapshot.overflowNestedEscapeRootOpenedObserved,
                      snapshot
                        .overflowNestedEscapeRootInitialFocusObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupOverflowNestedEscapeRootOpenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.prepareOverflowNestedMenuKey(
                        stage: "escape-focus-parent",
                        rootOpened: true,
                        childOpened: false),
                    next: .overflowNestedEscapeParentFocused,
                    failureCode:
                        "flowMenuPopupOverflowNestedEscapeParentFocusFailed")
            case .overflowNestedEscapeParentFocused:
                guard snapshot
                    .overflowNestedEscapeParentFocusObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupOverflowNestedEscapeParentFocusTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowRight,
                    Self.prepareOverflowNestedMenuKey(
                        stage: "escape-open-child",
                        rootOpened: true,
                        childOpened: false),
                    next: .overflowNestedEscapeChildOpened,
                    failureCode:
                        "flowMenuPopupOverflowNestedEscapeChildOpenFailed")
            case .overflowNestedEscapeChildOpened:
                guard snapshot
                    .overflowNestedEscapeChildOpenedObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupOverflowNestedEscapeChildOpenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .escape,
                    Self.prepareOverflowNestedMenuKey(
                        stage: "escape-close-child",
                        rootOpened: true,
                        childOpened: true),
                    next: .overflowNestedEscapeChildClosed,
                    failureCode:
                        "flowMenuPopupOverflowNestedEscapeChildCloseFailed")
            case .overflowNestedEscapeChildClosed:
                guard snapshot
                        .overflowNestedEscapeChildCloseObserved,
                      snapshot
                        .overflowNestedEscapeParentRemainsOpenObserved,
                      snapshot
                        .overflowNestedEscapeParentFocusRestoredObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupOverflowNestedEscapeChildCloseTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .escape,
                    Self.prepareOverflowNestedMenuKey(
                        stage: "escape-close-root",
                        rootOpened: true,
                        childOpened: false),
                    next: .overflowNestedEscapeRootClosed,
                    failureCode:
                        "flowMenuPopupOverflowNestedEscapeRootCloseFailed")
            case .overflowNestedEscapeRootClosed:
                guard snapshot
                        .overflowNestedEscapeRootCloseObserved,
                      snapshot
                        .overflowNestedEscapeOwnerFocusRestoredObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupOverflowNestedEscapeRootCloseTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.perform(
                    Self.expandOverflowMenuScript,
                    next: .overflowExpanded,
                    failureCode: "flowMenuPopupOverflowExpandFailed")
            case .overflowExpanded:
                guard snapshot.overflowExpandedObserved,
                      snapshot.overflowItemsRestoredObserved else {
                    self.retryOrFail(
                        code: "flowMenuPopupOverflowExpandTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeContextMenu(
                    Self.prepareContextPointerScript(),
                    next: .contextOpened,
                    failureCode: "flowMenuPopupContextOpenFailed")
            case .contextOpened:
                guard snapshot.contextOpenedObserved,
                      snapshot.contextA11yObserved,
                      snapshot.contextInitialFocusObserved,
                      snapshot.contextJavaOpenObserved,
                      snapshot.nativePointerObserved,
                      snapshot.nativePointerTrustedObserved else {
                    self.retryOrFail(
                        code: "flowMenuPopupContextOpenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.moveContextScript,
                    next: .contextMoved,
                    failureCode: "flowMenuPopupContextMoveFailed")
            case .contextMoved:
                guard snapshot.contextKeyboardMoveObserved else {
                    self.retryOrFail(
                        code: "flowMenuPopupContextMoveTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .returnKey,
                    Self.selectContextScript,
                    next: .contextSelected,
                    failureCode: "flowMenuPopupContextSelectionFailed")
            case .contextSelected:
                guard snapshot.contextSelectionObserved,
                      snapshot.contextJavaListenerObserved,
                      snapshot.contextJavaCloseObserved,
                      snapshot.contextClosedObserved,
                      snapshot.contextFocusRestoredObserved else {
                    self.retryOrFail(
                        code: "flowMenuPopupContextPatchTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeContextMenu(
                    Self.prepareContextPointerScript(),
                    next: .contextEscapeOpened,
                    failureCode: "flowMenuPopupContextReopenFailed")
            case .contextEscapeOpened:
                guard snapshot.contextEscapeOpenedObserved,
                      snapshot.contextA11yObserved,
                      snapshot.nativePointerEventCount >= 2 else {
                    self.retryOrFail(
                        code: "flowMenuPopupContextReopenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .escape,
                    Self.closeContextScript,
                    next: .contextEscapeClosed,
                    failureCode: "flowMenuPopupContextEscapeFailed")
            case .contextEscapeClosed:
                guard snapshot.contextEscapeCloseObserved,
                      snapshot.contextJavaCloseObserved,
                      snapshot.contextFocusRestoredObserved else {
                    self.retryOrFail(
                        code: "flowMenuPopupContextEscapeTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeContextMenu(
                    Self.prepareContextPointerScript(
                        nestedStage: "open-root"),
                    next: .contextNestedRootOpened,
                    failureCode:
                        "flowMenuPopupContextNestedRootOpenFailed")
            case .contextNestedRootOpened:
                guard snapshot.contextNestedRootOpenedObserved,
                      snapshot.contextNestedRootInitialFocusObserved,
                      snapshot.contextNestedJavaRootOpenObserved,
                      snapshot.nativePointerEventCount >= 3 else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupContextNestedRootOpenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.prepareNestedContextMenuKey(
                        stage: "move-first",
                        rootOpened: true,
                        childOpened: false),
                    next: .contextNestedFirstMoved,
                    failureCode:
                        "flowMenuPopupContextNestedFirstMoveFailed")
            case .contextNestedFirstMoved:
                guard snapshot.contextNestedFirstMoveObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupContextNestedFirstMoveTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.prepareNestedContextMenuKey(
                        stage: "focus-parent",
                        rootOpened: true,
                        childOpened: false),
                    next: .contextNestedParentFocused,
                    failureCode:
                        "flowMenuPopupContextNestedParentFocusFailed")
            case .contextNestedParentFocused:
                guard snapshot.contextNestedParentFocusObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupContextNestedParentFocusTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowRight,
                    Self.prepareNestedContextMenuKey(
                        stage: "open-child",
                        rootOpened: true,
                        childOpened: false),
                    next: .contextNestedChildOpened,
                    failureCode:
                        "flowMenuPopupContextNestedChildOpenFailed")
            case .contextNestedChildOpened:
                guard snapshot.contextNestedChildOpenedObserved,
                      snapshot.contextNestedA11yObserved,
                      snapshot.contextNestedInitialFocusObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupContextNestedChildOpenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.prepareNestedContextMenuKey(
                        stage: "move-child",
                        rootOpened: true,
                        childOpened: true),
                    next: .contextNestedChildMoved,
                    failureCode:
                        "flowMenuPopupContextNestedChildMoveFailed")
            case .contextNestedChildMoved:
                guard snapshot.contextNestedKeyboardMoveObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupContextNestedChildMoveTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowLeft,
                    Self.prepareNestedContextMenuKey(
                        stage: "return-parent",
                        rootOpened: true,
                        childOpened: true),
                    next: .contextNestedReturned,
                    failureCode:
                        "flowMenuPopupContextNestedLeftReturnFailed")
            case .contextNestedReturned:
                guard snapshot.contextNestedLeftReturnObserved,
                      snapshot.contextNestedParentRemainsOpenObserved,
                      snapshot.contextNestedParentFocusRestoredObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupContextNestedLeftReturnTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowRight,
                    Self.prepareNestedContextMenuKey(
                        stage: "reopen-child",
                        rootOpened: true,
                        childOpened: false),
                    next: .contextNestedReopened,
                    failureCode:
                        "flowMenuPopupContextNestedChildReopenFailed")
            case .contextNestedReopened:
                guard snapshot.contextNestedReopenedObserved,
                      snapshot
                        .contextNestedReopenedInitialFocusObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupContextNestedChildReopenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.prepareNestedContextMenuKey(
                        stage: "move-child-for-selection",
                        rootOpened: true,
                        childOpened: true),
                    next: .contextNestedReselected,
                    failureCode:
                        "flowMenuPopupContextNestedSelectionMoveFailed")
            case .contextNestedReselected:
                guard snapshot.contextNestedSelectionMoveObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupContextNestedSelectionMoveTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .returnKey,
                    Self.prepareNestedContextMenuKey(
                        stage: "select-leaf",
                        rootOpened: true,
                        childOpened: true),
                    next: .contextNestedSelected,
                    failureCode:
                        "flowMenuPopupContextNestedSelectionFailed")
            case .contextNestedSelected:
                guard snapshot.contextNestedSelectionObserved,
                      snapshot.contextNestedJavaListenerObserved,
                      snapshot.contextNestedJavaRootCloseObserved,
                      snapshot.contextNestedClosedObserved,
                      snapshot.contextNestedFocusRestoredObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupContextNestedPatchTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeContextMenu(
                    Self.prepareContextPointerScript(
                        nestedStage: "escape-open-root"),
                    next: .contextNestedEscapeRootOpened,
                    failureCode:
                        "flowMenuPopupContextNestedEscapeRootOpenFailed")
            case .contextNestedEscapeRootOpened:
                guard snapshot.contextNestedEscapeRootOpenedObserved,
                      snapshot
                        .contextNestedEscapeRootInitialFocusObserved,
                      snapshot.contextNestedEscapeJavaRootOpenObserved,
                      snapshot.nativePointerEventCount >= 4 else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupContextNestedEscapeRootOpenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.prepareNestedContextMenuKey(
                        stage: "escape-move-first",
                        rootOpened: true,
                        childOpened: false),
                    next: .contextNestedEscapeFirstMoved,
                    failureCode:
                        "flowMenuPopupContextNestedEscapeFirstMoveFailed")
            case .contextNestedEscapeFirstMoved:
                guard snapshot.contextNestedEscapeFirstMoveObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupContextNestedEscapeFirstMoveTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.prepareNestedContextMenuKey(
                        stage: "escape-focus-parent",
                        rootOpened: true,
                        childOpened: false),
                    next: .contextNestedEscapeParentFocused,
                    failureCode:
                        "flowMenuPopupContextNestedEscapeParentFocusFailed")
            case .contextNestedEscapeParentFocused:
                guard snapshot.contextNestedEscapeParentFocusObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupContextNestedEscapeParentFocusTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowRight,
                    Self.prepareNestedContextMenuKey(
                        stage: "escape-open-child",
                        rootOpened: true,
                        childOpened: false),
                    next: .contextNestedEscapeChildOpened,
                    failureCode:
                        "flowMenuPopupContextNestedEscapeChildOpenFailed")
            case .contextNestedEscapeChildOpened:
                guard snapshot.contextNestedEscapeChildOpenedObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupContextNestedEscapeChildOpenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .escape,
                    Self.prepareNestedContextMenuKey(
                        stage: "escape-close-child",
                        rootOpened: true,
                        childOpened: true),
                    next: .contextNestedEscapeChildClosed,
                    failureCode:
                        "flowMenuPopupContextNestedEscapeChildCloseFailed")
            case .contextNestedEscapeChildClosed:
                guard snapshot.contextNestedEscapeChildCloseObserved,
                      snapshot
                        .contextNestedEscapeParentRemainsOpenObserved,
                      snapshot
                        .contextNestedEscapeParentFocusRestoredObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupContextNestedEscapeChildCloseTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .escape,
                    Self.prepareNestedContextMenuKey(
                        stage: "escape-close-root",
                        rootOpened: true,
                        childOpened: false),
                    next: .contextNestedEscapeRootClosed,
                    failureCode:
                        "flowMenuPopupContextNestedEscapeRootCloseFailed")
            case .contextNestedEscapeRootClosed:
                guard snapshot.contextNestedEscapeRootCloseObserved,
                      snapshot.contextNestedEscapeJavaRootCloseObserved,
                      snapshot
                        .contextNestedEscapeOwnerFocusRestoredObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupContextNestedEscapeRootCloseTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.perform(
                    Self.prepareGridViewportScript,
                    next: .gridViewport,
                    failureCode:
                        "flowMenuPopupGridViewportPreparationFailed")
            case .gridViewport:
                guard snapshot.gridVirtualizedTargetReady else {
                    self.retryOrFail(
                        code: "flowMenuPopupGridViewportTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeContextMenu(
                    Self.prepareGridPointerScript(),
                    next: .gridOpened,
                    failureCode: "flowMenuPopupGridOpenFailed")
            case .gridOpened:
                guard snapshot.gridOpenedObserved,
                      snapshot.gridVirtualizedTargetObserved,
                      snapshot.gridRowContextObserved,
                      snapshot.gridColumnContextObserved,
                      snapshot.gridInitialFocusObserved,
                      snapshot.gridJavaOpenObserved,
                      snapshot.gridPointerTrustedObserved else {
                    self.retryOrFail(
                        code: "flowMenuPopupGridOpenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.moveGridContextScript,
                    next: .gridMoved,
                    failureCode: "flowMenuPopupGridMoveFailed")
            case .gridMoved:
                guard snapshot.gridKeyboardMoveObserved else {
                    self.retryOrFail(
                        code: "flowMenuPopupGridMoveTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .returnKey,
                    Self.selectGridContextScript,
                    next: .gridSelected,
                    failureCode: "flowMenuPopupGridSelectionFailed")
            case .gridSelected:
                guard snapshot.gridSelectionObserved,
                      snapshot.gridJavaListenerObserved,
                      snapshot.gridJavaCloseObserved,
                      snapshot.gridClosedObserved,
                      snapshot.gridFocusRestoredObserved else {
                    self.retryOrFail(
                        code: "flowMenuPopupGridPatchTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeContextMenu(
                    Self.prepareGridPointerScript(
                        nestedStage: "open-root"),
                    next: .gridNestedRootOpened,
                    failureCode:
                        "flowMenuPopupGridNestedRootOpenFailed")
            case .gridNestedRootOpened:
                guard snapshot.gridNestedRootOpenedObserved,
                      snapshot.gridNestedRootInitialFocusObserved,
                      snapshot.gridNestedJavaRootOpenObserved,
                      snapshot.gridNestedContextObserved,
                      snapshot.nativePointerEventCount >= 6 else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupGridNestedRootOpenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.prepareNestedGridContextMenuKey(
                        stage: "move-first",
                        rootOpened: true,
                        childOpened: false),
                    next: .gridNestedFirstMoved,
                    failureCode:
                        "flowMenuPopupGridNestedFirstMoveFailed")
            case .gridNestedFirstMoved:
                guard snapshot.gridNestedFirstMoveObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupGridNestedFirstMoveTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.prepareNestedGridContextMenuKey(
                        stage: "focus-parent",
                        rootOpened: true,
                        childOpened: false),
                    next: .gridNestedParentFocused,
                    failureCode:
                        "flowMenuPopupGridNestedParentFocusFailed")
            case .gridNestedParentFocused:
                guard snapshot.gridNestedParentFocusObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupGridNestedParentFocusTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowRight,
                    Self.prepareNestedGridContextMenuKey(
                        stage: "open-child",
                        rootOpened: true,
                        childOpened: false),
                    next: .gridNestedChildOpened,
                    failureCode:
                        "flowMenuPopupGridNestedChildOpenFailed")
            case .gridNestedChildOpened:
                guard snapshot.gridNestedChildOpenedObserved,
                      snapshot.gridNestedA11yObserved,
                      snapshot.gridNestedInitialFocusObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupGridNestedChildOpenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.prepareNestedGridContextMenuKey(
                        stage: "move-child",
                        rootOpened: true,
                        childOpened: true),
                    next: .gridNestedChildMoved,
                    failureCode:
                        "flowMenuPopupGridNestedChildMoveFailed")
            case .gridNestedChildMoved:
                guard snapshot.gridNestedKeyboardMoveObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupGridNestedChildMoveTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowLeft,
                    Self.prepareNestedGridContextMenuKey(
                        stage: "return-parent",
                        rootOpened: true,
                        childOpened: true),
                    next: .gridNestedReturned,
                    failureCode:
                        "flowMenuPopupGridNestedLeftReturnFailed")
            case .gridNestedReturned:
                guard snapshot.gridNestedLeftReturnObserved,
                      snapshot.gridNestedParentRemainsOpenObserved,
                      snapshot.gridNestedParentFocusRestoredObserved,
                      snapshot.gridNestedContextRetainedObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupGridNestedLeftReturnTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowRight,
                    Self.prepareNestedGridContextMenuKey(
                        stage: "reopen-child",
                        rootOpened: true,
                        childOpened: false),
                    next: .gridNestedReopened,
                    failureCode:
                        "flowMenuPopupGridNestedChildReopenFailed")
            case .gridNestedReopened:
                guard snapshot.gridNestedReopenedObserved,
                      snapshot.gridNestedReopenedInitialFocusObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupGridNestedChildReopenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.prepareNestedGridContextMenuKey(
                        stage: "move-child-for-selection",
                        rootOpened: true,
                        childOpened: true),
                    next: .gridNestedReselected,
                    failureCode:
                        "flowMenuPopupGridNestedSelectionMoveFailed")
            case .gridNestedReselected:
                guard snapshot.gridNestedSelectionMoveObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupGridNestedSelectionMoveTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .returnKey,
                    Self.prepareNestedGridContextMenuKey(
                        stage: "select-leaf",
                        rootOpened: true,
                        childOpened: true),
                    next: .gridNestedSelected,
                    failureCode:
                        "flowMenuPopupGridNestedSelectionFailed")
            case .gridNestedSelected:
                guard snapshot.gridNestedSelectionObserved,
                      snapshot.gridNestedJavaListenerObserved,
                      snapshot.gridNestedJavaRootCloseObserved,
                      snapshot.gridNestedClosedObserved,
                      snapshot.gridNestedFocusRestoredObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupGridNestedPatchTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeContextMenu(
                    Self.prepareGridPointerScript(
                        nestedStage: "escape-open-root"),
                    next: .gridNestedEscapeRootOpened,
                    failureCode:
                        "flowMenuPopupGridNestedEscapeRootOpenFailed")
            case .gridNestedEscapeRootOpened:
                guard snapshot.gridNestedEscapeRootOpenedObserved,
                      snapshot
                        .gridNestedEscapeRootInitialFocusObserved,
                      snapshot.gridNestedEscapeJavaRootOpenObserved,
                      snapshot.nativePointerEventCount >= 7 else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupGridNestedEscapeRootOpenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.prepareNestedGridContextMenuKey(
                        stage: "escape-move-first",
                        rootOpened: true,
                        childOpened: false),
                    next: .gridNestedEscapeFirstMoved,
                    failureCode:
                        "flowMenuPopupGridNestedEscapeFirstMoveFailed")
            case .gridNestedEscapeFirstMoved:
                guard snapshot.gridNestedEscapeFirstMoveObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupGridNestedEscapeFirstMoveTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowDown,
                    Self.prepareNestedGridContextMenuKey(
                        stage: "escape-focus-parent",
                        rootOpened: true,
                        childOpened: false),
                    next: .gridNestedEscapeParentFocused,
                    failureCode:
                        "flowMenuPopupGridNestedEscapeParentFocusFailed")
            case .gridNestedEscapeParentFocused:
                guard snapshot.gridNestedEscapeParentFocusObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupGridNestedEscapeParentFocusTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .arrowRight,
                    Self.prepareNestedGridContextMenuKey(
                        stage: "escape-open-child",
                        rootOpened: true,
                        childOpened: false),
                    next: .gridNestedEscapeChildOpened,
                    failureCode:
                        "flowMenuPopupGridNestedEscapeChildOpenFailed")
            case .gridNestedEscapeChildOpened:
                guard snapshot.gridNestedEscapeChildOpenedObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupGridNestedEscapeChildOpenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .escape,
                    Self.prepareNestedGridContextMenuKey(
                        stage: "escape-close-child",
                        rootOpened: true,
                        childOpened: true),
                    next: .gridNestedEscapeChildClosed,
                    failureCode:
                        "flowMenuPopupGridNestedEscapeChildCloseFailed")
            case .gridNestedEscapeChildClosed:
                guard snapshot.gridNestedEscapeChildCloseObserved,
                      snapshot
                        .gridNestedEscapeParentRemainsOpenObserved,
                      snapshot
                        .gridNestedEscapeParentFocusRestoredObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupGridNestedEscapeChildCloseTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .escape,
                    Self.prepareNestedGridContextMenuKey(
                        stage: "escape-close-root",
                        rootOpened: true,
                        childOpened: false),
                    next: .gridNestedEscapeRootClosed,
                    failureCode:
                        "flowMenuPopupGridNestedEscapeRootCloseFailed")
            case .gridNestedEscapeRootClosed:
                guard snapshot.gridNestedEscapeRootCloseObserved,
                      snapshot.gridNestedEscapeJavaRootCloseObserved,
                      snapshot
                        .gridNestedEscapeOwnerFocusRestoredObserved else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupGridNestedEscapeRootCloseTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeContextMenu(
                    Self.prepareGridPolicyPointerScript(stage: "header"),
                    next: .gridPolicyHeaderRejected,
                    failureCode:
                        "flowMenuPopupGridPolicyHeaderDispatchFailed")
            case .gridPolicyHeaderRejected:
                guard snapshot.gridPolicyHeaderPointerTrustedObserved,
                      snapshot.gridPolicyHeaderContextObserved,
                      snapshot.gridPolicyHeaderJavaRejectObserved,
                      snapshot.gridPolicyHeaderClosedObserved,
                      snapshot.nativePointerEventCount >= 8 else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupGridPolicyHeaderRejectTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeContextMenu(
                    Self.prepareGridPolicyPointerScript(stage: "empty"),
                    next: .gridPolicyEmptyRejected,
                    failureCode:
                        "flowMenuPopupGridPolicyEmptyDispatchFailed")
            case .gridPolicyEmptyRejected:
                guard snapshot.gridPolicyEmptyPointerTrustedObserved,
                      snapshot.gridPolicyEmptyGeometryObserved,
                      snapshot.gridPolicyEmptyContextObserved,
                      snapshot.gridPolicyEmptyJavaRejectObserved,
                      snapshot.gridPolicyEmptyClosedObserved,
                      snapshot.nativePointerEventCount >= 9 else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupGridPolicyEmptyRejectTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeContextMenu(
                    Self.prepareGridPolicyPointerScript(stage: "row"),
                    next: .gridPolicyRowOpened,
                    failureCode:
                        "flowMenuPopupGridPolicyRowDispatchFailed")
            case .gridPolicyRowOpened:
                guard snapshot.gridPolicyRowPointerTrustedObserved,
                      snapshot.gridPolicyRowContextObserved,
                      snapshot.gridPolicyRowOpenedObserved,
                      snapshot.gridPolicyRowInitialFocusObserved,
                      snapshot.gridPolicyDynamicContentObserved,
                      snapshot.gridPolicyJavaOpenObserved,
                      snapshot.nativePointerEventCount >= 10 else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupGridPolicyRowOpenTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.performNativeKey(
                    .returnKey,
                    Self.selectGridPolicyContextScript,
                    next: .gridPolicyRowSelected,
                    failureCode:
                        "flowMenuPopupGridPolicySelectionFailed")
            case .gridPolicyRowSelected:
                guard snapshot.gridPolicyJavaListenerObserved,
                      snapshot.gridPolicyJavaCloseObserved,
                      snapshot.gridPolicyClosedObserved,
                      snapshot.gridPolicyFocusRestoredObserved,
                      snapshot.nativeKeyboardEventCount >= 69 else {
                    self.retryOrFail(
                        code:
                            "flowMenuPopupGridPolicySelectionTimeout",
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
          const marker = window.VaadinDesktopFlowMenuPopup || {};
          window.VaadinDesktopFlowMenuPopup = marker;
          marker.nativeKeyboardExpected = '\(key.name)';
          marker.nativeKeyboardCurrentObserved = false;
          document.addEventListener('keydown', (event) => {
            marker.nativeKeyboardObserved = true;
            marker.nativeKeyboardCurrentObserved = true;
            marker.nativeKeyboardTrustedObserved =
              marker.nativeKeyboardTrustedObserved === true
                || event.isTrusted === true;
            marker.nativeKeyboardLastKey = String(event.key || '');
            marker.nativeKeyboardLastTarget =
              String(event.target?.id || event.target?.localName || '');
            marker.nativeKeyboardSequence =
              Array.isArray(marker.nativeKeyboardSequence)
                ? marker.nativeKeyboardSequence
                : [];
            marker.nativeKeyboardSequence.push(String(event.key || ''));
          }, { capture: true, once: true });
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
                trace("recv", "flowMenuPopup.nativeKeyboardDispatch", [
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

    private func performNativeContextMenu(
        _ preparationScript: String,
        next: Phase,
        failureCode: String
    ) {
        context.webView.evaluateJavaScript(
            preparationScript
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
                            "Context-menu target is outside the host window",
                        "windowX": windowPoint.x,
                        "windowY": windowPoint.y
                    ])
                    return
                }
                let timestamp = ProcessInfo.processInfo.systemUptime
                guard let mouseDown = NSEvent.mouseEvent(
                          with: .rightMouseDown,
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
                          with: .rightMouseUp,
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
                        "message":
                            "Unable to create AppKit pointer event"
                    ])
                    return
                }
                trace("recv", "flowMenuPopup.nativePointerDispatch", [
                    "inputMode": "appkit-window-pointer-event",
                    "button": "secondary",
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
            "opaqueRequestBridgeObserved": snapshot.requestCount >= 6,
            "opaqueResponseBridgeObserved": snapshot.responseCount >= 6,
            "pushObserved": snapshot.pushMessageCount >= 1,
            "errorFree": snapshot.errorFree,
            "flowMenuPopupBrowserProgrammaticNavigationObserved": true
        ]) { _, replacement in replacement }
        trace("recv", "flowMenuPopup.frameworkAcceptance", payload)
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
        let menuOpenedObserved: Bool
        let menuA11yObserved: Bool
        let menuOwnerFocusWhileOpenObserved: Bool
        let menuExpandedButtonMatchesOwner: Bool
        let menuKeyEventCount: Int
        let menuKeyDiagnostics: String
        let menuOwnedOverlayDiagnostics: String
        let menuInitialFocusObserved: Bool
        let menuKeyboardMoveObserved: Bool
        let menuSelectionObserved: Bool
        let menuJavaListenerObserved: Bool
        let menuClosedObserved: Bool
        let menuFocusRestoredObserved: Bool
        let menuEscapeOpenedObserved: Bool
        let menuEscapeCloseObserved: Bool
        let nestedRootOpenedObserved: Bool
        let nestedRootInitialFocusObserved: Bool
        let nestedFirstMoveObserved: Bool
        let nestedParentFocusObserved: Bool
        let nestedChildOpenedObserved: Bool
        let nestedA11yObserved: Bool
        let nestedInitialFocusObserved: Bool
        let nestedKeyboardMoveObserved: Bool
        let nestedLeftReturnObserved: Bool
        let nestedParentRemainsOpenObserved: Bool
        let nestedParentFocusRestoredObserved: Bool
        let nestedReopenedObserved: Bool
        let nestedReopenedInitialFocusObserved: Bool
        let nestedSelectionMoveObserved: Bool
        let nestedSelectionObserved: Bool
        let nestedJavaListenerObserved: Bool
        let nestedClosedObserved: Bool
        let nestedFocusRestoredObserved: Bool
        let nestedEscapeRootOpenedObserved: Bool
        let nestedEscapeFirstMoveObserved: Bool
        let nestedEscapeParentFocusObserved: Bool
        let nestedEscapeChildOpenedObserved: Bool
        let nestedEscapeChildCloseObserved: Bool
        let nestedEscapeParentRemainsOpenObserved: Bool
        let nestedEscapeParentFocusRestoredObserved: Bool
        let nestedEscapeRootCloseObserved: Bool
        let nestedEscapeOwnerFocusRestoredObserved: Bool
        let overflowNarrowObserved: Bool
        let overflowOpenedObserved: Bool
        let overflowA11yObserved: Bool
        let overflowInitialFocusObserved: Bool
        let overflowSelectionObserved: Bool
        let overflowJavaListenerObserved: Bool
        let overflowClosedObserved: Bool
        let overflowFocusRestoredObserved: Bool
        let overflowEscapeOpenedObserved: Bool
        let overflowEscapeInitialFocusObserved: Bool
        let overflowEscapeCloseObserved: Bool
        let overflowEscapeFocusRestoredObserved: Bool
        let overflowNestedCollapsedParentObserved: Bool
        let overflowNestedRootOpenedObserved: Bool
        let overflowNestedRootInitialFocusObserved: Bool
        let overflowNestedParentFocusObserved: Bool
        let overflowNestedChildOpenedObserved: Bool
        let overflowNestedA11yObserved: Bool
        let overflowNestedInitialFocusObserved: Bool
        let overflowNestedKeyboardMoveObserved: Bool
        let overflowNestedLeftReturnObserved: Bool
        let overflowNestedParentRemainsOpenObserved: Bool
        let overflowNestedParentFocusRestoredObserved: Bool
        let overflowNestedReopenedObserved: Bool
        let overflowNestedReopenedInitialFocusObserved: Bool
        let overflowNestedSelectionMoveObserved: Bool
        let overflowNestedSelectionObserved: Bool
        let overflowNestedJavaListenerObserved: Bool
        let overflowNestedClosedObserved: Bool
        let overflowNestedFocusRestoredObserved: Bool
        let overflowNestedEscapeRootOpenedObserved: Bool
        let overflowNestedEscapeRootInitialFocusObserved: Bool
        let overflowNestedEscapeParentFocusObserved: Bool
        let overflowNestedEscapeChildOpenedObserved: Bool
        let overflowNestedEscapeChildCloseObserved: Bool
        let overflowNestedEscapeParentRemainsOpenObserved: Bool
        let overflowNestedEscapeParentFocusRestoredObserved: Bool
        let overflowNestedEscapeRootCloseObserved: Bool
        let overflowNestedEscapeOwnerFocusRestoredObserved: Bool
        let overflowExpandedObserved: Bool
        let overflowItemsRestoredObserved: Bool
        let contextOpenedObserved: Bool
        let contextA11yObserved: Bool
        let contextInitialFocusObserved: Bool
        let contextKeyboardMoveObserved: Bool
        let contextSelectionObserved: Bool
        let contextJavaListenerObserved: Bool
        let contextClosedObserved: Bool
        let contextFocusRestoredObserved: Bool
        let contextEscapeOpenedObserved: Bool
        let contextEscapeCloseObserved: Bool
        let contextJavaOpenObserved: Bool
        let contextJavaCloseObserved: Bool
        let contextNestedRootOpenedObserved: Bool
        let contextNestedRootInitialFocusObserved: Bool
        let contextNestedJavaRootOpenObserved: Bool
        let contextNestedFirstMoveObserved: Bool
        let contextNestedParentFocusObserved: Bool
        let contextNestedChildOpenedObserved: Bool
        let contextNestedA11yObserved: Bool
        let contextNestedInitialFocusObserved: Bool
        let contextNestedKeyboardMoveObserved: Bool
        let contextNestedLeftReturnObserved: Bool
        let contextNestedParentRemainsOpenObserved: Bool
        let contextNestedParentFocusRestoredObserved: Bool
        let contextNestedReopenedObserved: Bool
        let contextNestedReopenedInitialFocusObserved: Bool
        let contextNestedSelectionMoveObserved: Bool
        let contextNestedSelectionObserved: Bool
        let contextNestedJavaListenerObserved: Bool
        let contextNestedJavaRootCloseObserved: Bool
        let contextNestedClosedObserved: Bool
        let contextNestedFocusRestoredObserved: Bool
        let contextNestedEscapeRootOpenedObserved: Bool
        let contextNestedEscapeRootInitialFocusObserved: Bool
        let contextNestedEscapeJavaRootOpenObserved: Bool
        let contextNestedEscapeFirstMoveObserved: Bool
        let contextNestedEscapeParentFocusObserved: Bool
        let contextNestedEscapeChildOpenedObserved: Bool
        let contextNestedEscapeChildCloseObserved: Bool
        let contextNestedEscapeParentRemainsOpenObserved: Bool
        let contextNestedEscapeParentFocusRestoredObserved: Bool
        let contextNestedEscapeRootCloseObserved: Bool
        let contextNestedEscapeJavaRootCloseObserved: Bool
        let contextNestedEscapeOwnerFocusRestoredObserved: Bool
        let gridVirtualizedTargetReady: Bool
        let gridOpenedObserved: Bool
        let gridVirtualizedTargetObserved: Bool
        let gridRowContextObserved: Bool
        let gridColumnContextObserved: Bool
        let gridInitialFocusObserved: Bool
        let gridKeyboardMoveObserved: Bool
        let gridSelectionObserved: Bool
        let gridJavaOpenObserved: Bool
        let gridJavaListenerObserved: Bool
        let gridJavaCloseObserved: Bool
        let gridClosedObserved: Bool
        let gridFocusRestoredObserved: Bool
        let gridPointerTrustedObserved: Bool
        let gridNestedRootOpenedObserved: Bool
        let gridNestedRootInitialFocusObserved: Bool
        let gridNestedJavaRootOpenObserved: Bool
        let gridNestedContextObserved: Bool
        let gridNestedFirstMoveObserved: Bool
        let gridNestedParentFocusObserved: Bool
        let gridNestedChildOpenedObserved: Bool
        let gridNestedA11yObserved: Bool
        let gridNestedInitialFocusObserved: Bool
        let gridNestedKeyboardMoveObserved: Bool
        let gridNestedLeftReturnObserved: Bool
        let gridNestedParentRemainsOpenObserved: Bool
        let gridNestedParentFocusRestoredObserved: Bool
        let gridNestedContextRetainedObserved: Bool
        let gridNestedReopenedObserved: Bool
        let gridNestedReopenedInitialFocusObserved: Bool
        let gridNestedSelectionMoveObserved: Bool
        let gridNestedSelectionObserved: Bool
        let gridNestedJavaListenerObserved: Bool
        let gridNestedJavaRootCloseObserved: Bool
        let gridNestedClosedObserved: Bool
        let gridNestedFocusRestoredObserved: Bool
        let gridNestedEscapeRootOpenedObserved: Bool
        let gridNestedEscapeRootInitialFocusObserved: Bool
        let gridNestedEscapeJavaRootOpenObserved: Bool
        let gridNestedEscapeFirstMoveObserved: Bool
        let gridNestedEscapeParentFocusObserved: Bool
        let gridNestedEscapeChildOpenedObserved: Bool
        let gridNestedEscapeChildCloseObserved: Bool
        let gridNestedEscapeParentRemainsOpenObserved: Bool
        let gridNestedEscapeParentFocusRestoredObserved: Bool
        let gridNestedEscapeRootCloseObserved: Bool
        let gridNestedEscapeJavaRootCloseObserved: Bool
        let gridNestedEscapeOwnerFocusRestoredObserved: Bool
        let gridPolicyHeaderPointerTrustedObserved: Bool
        let gridPolicyHeaderContextObserved: Bool
        let gridPolicyHeaderJavaRejectObserved: Bool
        let gridPolicyHeaderClosedObserved: Bool
        let gridPolicyEmptyPointerTrustedObserved: Bool
        let gridPolicyEmptyGeometryObserved: Bool
        let gridPolicyEmptyContextObserved: Bool
        let gridPolicyEmptyJavaRejectObserved: Bool
        let gridPolicyEmptyClosedObserved: Bool
        let gridPolicyRowPointerTrustedObserved: Bool
        let gridPolicyRowContextObserved: Bool
        let gridPolicyRowOpenedObserved: Bool
        let gridPolicyRowInitialFocusObserved: Bool
        let gridPolicyDynamicContentObserved: Bool
        let gridPolicyJavaOpenObserved: Bool
        let gridPolicyJavaListenerObserved: Bool
        let gridPolicyJavaCloseObserved: Bool
        let gridPolicyClosedObserved: Bool
        let gridPolicyFocusRestoredObserved: Bool
        let nativeKeyboardObserved: Bool
        let nativeKeyboardTrustedObserved: Bool
        let nativeKeyboardCurrentObserved: Bool
        let nativeKeyboardEventCount: Int
        let nativeKeyboardExpected: String
        let nativeKeyboardLastKey: String
        let nativeKeyboardLastTarget: String
        let nativeKeyboardSequence: String
        let nativePointerObserved: Bool
        let nativePointerTrustedObserved: Bool
        let nativePointerEventCount: Int
        let nativePointerLastTarget: String
        let nativePointerLastButton: Int
        let menuOpenedCurrent: Bool
        let menuOwnerExpanded: String
        let menuFocusedText: String
        let nestedMenuStage: String
        let nestedMenuStages: String
        let nestedOpenedCurrent: Bool
        let nestedParentExpanded: String
        let nestedRootFocusedText: String
        let nestedFocusedText: String
        let nestedStatus: String
        let nestedDiagnostics: String
        let overflowStage: String
        let overflowStages: String
        let overflowOpenedCurrent: Bool
        let overflowCollapsedItemCount: Int
        let overflowHiddenButtonCount: Int
        let overflowExpectedItemId: String
        let overflowFocusedText: String
        let overflowStatus: String
        let overflowDiagnostics: String
        let overflowNestedStage: String
        let overflowNestedStages: String
        let overflowNestedOpenedCurrent: Bool
        let overflowNestedRootFocusedText: String
        let overflowNestedFocusedText: String
        let overflowNestedStatus: String
        let overflowNestedDiagnostics: String
        let contextOpenedCurrent: Bool
        let contextFocusedText: String
        let contextNestedStage: String
        let contextNestedStages: String
        let contextNestedOpenedCurrent: Bool
        let contextNestedRootFocusedText: String
        let contextNestedFocusedText: String
        let contextNestedStatus: String
        let contextNestedDiagnostics: String
        let gridOpenedCurrent: Bool
        let gridFocusedText: String
        let gridNestedStage: String
        let gridNestedStages: String
        let gridNestedOpenedCurrent: Bool
        let gridNestedRootFocusedText: String
        let gridNestedFocusedText: String
        let gridNestedStatus: String
        let gridNestedDiagnostics: String
        let gridPolicyStage: String
        let gridPolicyStages: String
        let gridPolicyOpenedCurrent: Bool
        let gridPolicyFocusedText: String
        let gridPolicyStatus: String
        let gridPolicyLifecycleStatus: String
        let gridPolicyActionStatus: String
        let gridPolicyDiagnostics: String
        let menuStatus: String
        let contextStatus: String
        let contextLifecycleStatus: String
        let gridOpenStatus: String
        let gridActionStatus: String
        let gridLifecycleStatus: String
        let gridPointerTargetText: String
        let gridRenderedItemCount: Int
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
                && menuOpenedObserved
                && menuA11yObserved
                && menuOwnerFocusWhileOpenObserved
                && menuExpandedButtonMatchesOwner
                && menuInitialFocusObserved
                && menuKeyboardMoveObserved
                && menuSelectionObserved
                && menuJavaListenerObserved
                && menuClosedObserved
                && menuFocusRestoredObserved
                && menuEscapeOpenedObserved
                && menuEscapeCloseObserved
                && nestedRootOpenedObserved
                && nestedRootInitialFocusObserved
                && nestedFirstMoveObserved
                && nestedParentFocusObserved
                && nestedChildOpenedObserved
                && nestedA11yObserved
                && nestedInitialFocusObserved
                && nestedKeyboardMoveObserved
                && nestedLeftReturnObserved
                && nestedParentRemainsOpenObserved
                && nestedParentFocusRestoredObserved
                && nestedReopenedObserved
                && nestedReopenedInitialFocusObserved
                && nestedSelectionMoveObserved
                && nestedSelectionObserved
                && nestedJavaListenerObserved
                && nestedClosedObserved
                && nestedFocusRestoredObserved
                && nestedEscapeRootOpenedObserved
                && nestedEscapeFirstMoveObserved
                && nestedEscapeParentFocusObserved
                && nestedEscapeChildOpenedObserved
                && nestedEscapeChildCloseObserved
                && nestedEscapeParentRemainsOpenObserved
                && nestedEscapeParentFocusRestoredObserved
                && nestedEscapeRootCloseObserved
                && nestedEscapeOwnerFocusRestoredObserved
                && overflowNarrowObserved
                && overflowOpenedObserved
                && overflowA11yObserved
                && overflowInitialFocusObserved
                && overflowSelectionObserved
                && overflowJavaListenerObserved
                && overflowClosedObserved
                && overflowFocusRestoredObserved
                && overflowEscapeOpenedObserved
                && overflowEscapeInitialFocusObserved
                && overflowEscapeCloseObserved
                && overflowEscapeFocusRestoredObserved
                && overflowNestedCollapsedParentObserved
                && overflowNestedRootOpenedObserved
                && overflowNestedRootInitialFocusObserved
                && overflowNestedParentFocusObserved
                && overflowNestedChildOpenedObserved
                && overflowNestedA11yObserved
                && overflowNestedInitialFocusObserved
                && overflowNestedKeyboardMoveObserved
                && overflowNestedLeftReturnObserved
                && overflowNestedParentRemainsOpenObserved
                && overflowNestedParentFocusRestoredObserved
                && overflowNestedReopenedObserved
                && overflowNestedReopenedInitialFocusObserved
                && overflowNestedSelectionMoveObserved
                && overflowNestedSelectionObserved
                && overflowNestedJavaListenerObserved
                && overflowNestedClosedObserved
                && overflowNestedFocusRestoredObserved
                && overflowNestedEscapeRootOpenedObserved
                && overflowNestedEscapeRootInitialFocusObserved
                && overflowNestedEscapeParentFocusObserved
                && overflowNestedEscapeChildOpenedObserved
                && overflowNestedEscapeChildCloseObserved
                && overflowNestedEscapeParentRemainsOpenObserved
                && overflowNestedEscapeParentFocusRestoredObserved
                && overflowNestedEscapeRootCloseObserved
                && overflowNestedEscapeOwnerFocusRestoredObserved
                && overflowExpandedObserved
                && overflowItemsRestoredObserved
                && contextOpenedObserved
                && contextA11yObserved
                && contextInitialFocusObserved
                && contextKeyboardMoveObserved
                && contextSelectionObserved
                && contextJavaListenerObserved
                && contextClosedObserved
                && contextFocusRestoredObserved
                && contextEscapeOpenedObserved
                && contextEscapeCloseObserved
                && contextJavaOpenObserved
                && contextJavaCloseObserved
                && contextNestedRootOpenedObserved
                && contextNestedRootInitialFocusObserved
                && contextNestedJavaRootOpenObserved
                && contextNestedFirstMoveObserved
                && contextNestedParentFocusObserved
                && contextNestedChildOpenedObserved
                && contextNestedA11yObserved
                && contextNestedInitialFocusObserved
                && contextNestedKeyboardMoveObserved
                && contextNestedLeftReturnObserved
                && contextNestedParentRemainsOpenObserved
                && contextNestedParentFocusRestoredObserved
                && contextNestedReopenedObserved
                && contextNestedReopenedInitialFocusObserved
                && contextNestedSelectionMoveObserved
                && contextNestedSelectionObserved
                && contextNestedJavaListenerObserved
                && contextNestedJavaRootCloseObserved
                && contextNestedClosedObserved
                && contextNestedFocusRestoredObserved
                && contextNestedEscapeRootOpenedObserved
                && contextNestedEscapeRootInitialFocusObserved
                && contextNestedEscapeJavaRootOpenObserved
                && contextNestedEscapeFirstMoveObserved
                && contextNestedEscapeParentFocusObserved
                && contextNestedEscapeChildOpenedObserved
                && contextNestedEscapeChildCloseObserved
                && contextNestedEscapeParentRemainsOpenObserved
                && contextNestedEscapeParentFocusRestoredObserved
                && contextNestedEscapeRootCloseObserved
                && contextNestedEscapeJavaRootCloseObserved
                && contextNestedEscapeOwnerFocusRestoredObserved
                && gridOpenedObserved
                && gridVirtualizedTargetObserved
                && gridRowContextObserved
                && gridColumnContextObserved
                && gridInitialFocusObserved
                && gridKeyboardMoveObserved
                && gridSelectionObserved
                && gridJavaOpenObserved
                && gridJavaListenerObserved
                && gridJavaCloseObserved
                && gridClosedObserved
                && gridFocusRestoredObserved
                && gridPointerTrustedObserved
                && gridNestedRootOpenedObserved
                && gridNestedRootInitialFocusObserved
                && gridNestedJavaRootOpenObserved
                && gridNestedContextObserved
                && gridNestedFirstMoveObserved
                && gridNestedParentFocusObserved
                && gridNestedChildOpenedObserved
                && gridNestedA11yObserved
                && gridNestedInitialFocusObserved
                && gridNestedKeyboardMoveObserved
                && gridNestedLeftReturnObserved
                && gridNestedParentRemainsOpenObserved
                && gridNestedParentFocusRestoredObserved
                && gridNestedContextRetainedObserved
                && gridNestedReopenedObserved
                && gridNestedReopenedInitialFocusObserved
                && gridNestedSelectionMoveObserved
                && gridNestedSelectionObserved
                && gridNestedJavaListenerObserved
                && gridNestedJavaRootCloseObserved
                && gridNestedClosedObserved
                && gridNestedFocusRestoredObserved
                && gridNestedEscapeRootOpenedObserved
                && gridNestedEscapeRootInitialFocusObserved
                && gridNestedEscapeJavaRootOpenObserved
                && gridNestedEscapeFirstMoveObserved
                && gridNestedEscapeParentFocusObserved
                && gridNestedEscapeChildOpenedObserved
                && gridNestedEscapeChildCloseObserved
                && gridNestedEscapeParentRemainsOpenObserved
                && gridNestedEscapeParentFocusRestoredObserved
                && gridNestedEscapeRootCloseObserved
                && gridNestedEscapeJavaRootCloseObserved
                && gridNestedEscapeOwnerFocusRestoredObserved
                && gridPolicyHeaderPointerTrustedObserved
                && gridPolicyHeaderContextObserved
                && gridPolicyHeaderJavaRejectObserved
                && gridPolicyHeaderClosedObserved
                && gridPolicyEmptyPointerTrustedObserved
                && gridPolicyEmptyGeometryObserved
                && gridPolicyEmptyContextObserved
                && gridPolicyEmptyJavaRejectObserved
                && gridPolicyEmptyClosedObserved
                && gridPolicyRowPointerTrustedObserved
                && gridPolicyRowContextObserved
                && gridPolicyRowOpenedObserved
                && gridPolicyRowInitialFocusObserved
                && gridPolicyDynamicContentObserved
                && gridPolicyJavaOpenObserved
                && gridPolicyJavaListenerObserved
                && gridPolicyJavaCloseObserved
                && gridPolicyClosedObserved
                && gridPolicyFocusRestoredObserved
                && nativeKeyboardObserved
                && nativeKeyboardTrustedObserved
                && nativeKeyboardEventCount >= 69
                && nativePointerObserved
                && nativePointerTrustedObserved
                && nativePointerEventCount >= 10
                && requestCount >= 6
                && responseCount >= 6
                && pushMessageCount >= 1
        }

        var tracePayload: [String: Any] {
            [
                "flowMenuPopupDedicatedRouteObserved": dedicatedRouteReady,
                "flowMenuPopupPath": path,
                "flowMenuPopupMenuOpenedObserved": menuOpenedObserved,
                "flowMenuPopupMenuA11yObserved": menuA11yObserved,
                "flowMenuPopupMenuOwnerFocusWhileOpenObserved":
                    menuOwnerFocusWhileOpenObserved,
                "flowMenuPopupMenuExpandedButtonMatchesOwner":
                    menuExpandedButtonMatchesOwner,
                "flowMenuPopupMenuKeyEventCount": menuKeyEventCount,
                "flowMenuPopupMenuKeyDiagnostics": menuKeyDiagnostics,
                "flowMenuPopupMenuOwnedOverlayDiagnostics":
                    menuOwnedOverlayDiagnostics,
                "flowMenuPopupMenuInitialFocusObserved":
                    menuInitialFocusObserved,
                "flowMenuPopupMenuKeyboardMoveObserved":
                    menuKeyboardMoveObserved,
                "flowMenuPopupMenuSelectionObserved":
                    menuSelectionObserved,
                "flowMenuPopupMenuJavaListenerObserved":
                    menuJavaListenerObserved,
                "flowMenuPopupMenuClosedObserved": menuClosedObserved,
                "flowMenuPopupMenuFocusRestoredObserved":
                    menuFocusRestoredObserved,
                "flowMenuPopupMenuEscapeOpenedObserved":
                    menuEscapeOpenedObserved,
                "flowMenuPopupMenuEscapeCloseObserved":
                    menuEscapeCloseObserved,
                "flowMenuPopupNestedRootOpenedObserved":
                    nestedRootOpenedObserved,
                "flowMenuPopupNestedRootInitialFocusObserved":
                    nestedRootInitialFocusObserved,
                "flowMenuPopupNestedFirstMoveObserved":
                    nestedFirstMoveObserved,
                "flowMenuPopupNestedParentFocusObserved":
                    nestedParentFocusObserved,
                "flowMenuPopupNestedChildOpenedObserved":
                    nestedChildOpenedObserved,
                "flowMenuPopupNestedA11yObserved":
                    nestedA11yObserved,
                "flowMenuPopupNestedInitialFocusObserved":
                    nestedInitialFocusObserved,
                "flowMenuPopupNestedKeyboardMoveObserved":
                    nestedKeyboardMoveObserved,
                "flowMenuPopupNestedLeftReturnObserved":
                    nestedLeftReturnObserved,
                "flowMenuPopupNestedParentRemainsOpenObserved":
                    nestedParentRemainsOpenObserved,
                "flowMenuPopupNestedParentFocusRestoredObserved":
                    nestedParentFocusRestoredObserved,
                "flowMenuPopupNestedReopenedObserved":
                    nestedReopenedObserved,
                "flowMenuPopupNestedReopenedInitialFocusObserved":
                    nestedReopenedInitialFocusObserved,
                "flowMenuPopupNestedSelectionMoveObserved":
                    nestedSelectionMoveObserved,
                "flowMenuPopupNestedSelectionObserved":
                    nestedSelectionObserved,
                "flowMenuPopupNestedJavaListenerObserved":
                    nestedJavaListenerObserved,
                "flowMenuPopupNestedClosedObserved":
                    nestedClosedObserved,
                "flowMenuPopupNestedFocusRestoredObserved":
                    nestedFocusRestoredObserved,
                "flowMenuPopupNestedEscapeRootOpenedObserved":
                    nestedEscapeRootOpenedObserved,
                "flowMenuPopupNestedEscapeFirstMoveObserved":
                    nestedEscapeFirstMoveObserved,
                "flowMenuPopupNestedEscapeParentFocusObserved":
                    nestedEscapeParentFocusObserved,
                "flowMenuPopupNestedEscapeChildOpenedObserved":
                    nestedEscapeChildOpenedObserved,
                "flowMenuPopupNestedEscapeChildCloseObserved":
                    nestedEscapeChildCloseObserved,
                "flowMenuPopupNestedEscapeParentRemainsOpenObserved":
                    nestedEscapeParentRemainsOpenObserved,
                "flowMenuPopupNestedEscapeParentFocusRestoredObserved":
                    nestedEscapeParentFocusRestoredObserved,
                "flowMenuPopupNestedEscapeRootCloseObserved":
                    nestedEscapeRootCloseObserved,
                "flowMenuPopupNestedEscapeOwnerFocusRestoredObserved":
                    nestedEscapeOwnerFocusRestoredObserved,
                "flowMenuPopupOverflowNarrowObserved":
                    overflowNarrowObserved,
                "flowMenuPopupOverflowOpenedObserved":
                    overflowOpenedObserved,
                "flowMenuPopupOverflowA11yObserved":
                    overflowA11yObserved,
                "flowMenuPopupOverflowInitialFocusObserved":
                    overflowInitialFocusObserved,
                "flowMenuPopupOverflowSelectionObserved":
                    overflowSelectionObserved,
                "flowMenuPopupOverflowJavaListenerObserved":
                    overflowJavaListenerObserved,
                "flowMenuPopupOverflowClosedObserved":
                    overflowClosedObserved,
                "flowMenuPopupOverflowFocusRestoredObserved":
                    overflowFocusRestoredObserved,
                "flowMenuPopupOverflowEscapeOpenedObserved":
                    overflowEscapeOpenedObserved,
                "flowMenuPopupOverflowEscapeInitialFocusObserved":
                    overflowEscapeInitialFocusObserved,
                "flowMenuPopupOverflowEscapeCloseObserved":
                    overflowEscapeCloseObserved,
                "flowMenuPopupOverflowEscapeFocusRestoredObserved":
                    overflowEscapeFocusRestoredObserved,
                "flowMenuPopupOverflowNestedCollapsedParentObserved":
                    overflowNestedCollapsedParentObserved,
                "flowMenuPopupOverflowNestedRootOpenedObserved":
                    overflowNestedRootOpenedObserved,
                "flowMenuPopupOverflowNestedRootInitialFocusObserved":
                    overflowNestedRootInitialFocusObserved,
                "flowMenuPopupOverflowNestedParentFocusObserved":
                    overflowNestedParentFocusObserved,
                "flowMenuPopupOverflowNestedChildOpenedObserved":
                    overflowNestedChildOpenedObserved,
                "flowMenuPopupOverflowNestedA11yObserved":
                    overflowNestedA11yObserved,
                "flowMenuPopupOverflowNestedInitialFocusObserved":
                    overflowNestedInitialFocusObserved,
                "flowMenuPopupOverflowNestedKeyboardMoveObserved":
                    overflowNestedKeyboardMoveObserved,
                "flowMenuPopupOverflowNestedLeftReturnObserved":
                    overflowNestedLeftReturnObserved,
                "flowMenuPopupOverflowNestedParentRemainsOpenObserved":
                    overflowNestedParentRemainsOpenObserved,
                "flowMenuPopupOverflowNestedParentFocusRestoredObserved":
                    overflowNestedParentFocusRestoredObserved,
                "flowMenuPopupOverflowNestedReopenedObserved":
                    overflowNestedReopenedObserved,
                "flowMenuPopupOverflowNestedReopenedInitialFocusObserved":
                    overflowNestedReopenedInitialFocusObserved,
                "flowMenuPopupOverflowNestedSelectionMoveObserved":
                    overflowNestedSelectionMoveObserved,
                "flowMenuPopupOverflowNestedSelectionObserved":
                    overflowNestedSelectionObserved,
                "flowMenuPopupOverflowNestedJavaListenerObserved":
                    overflowNestedJavaListenerObserved,
                "flowMenuPopupOverflowNestedClosedObserved":
                    overflowNestedClosedObserved,
                "flowMenuPopupOverflowNestedFocusRestoredObserved":
                    overflowNestedFocusRestoredObserved,
                "flowMenuPopupOverflowNestedEscapeRootOpenedObserved":
                    overflowNestedEscapeRootOpenedObserved,
                "flowMenuPopupOverflowNestedEscapeRootInitialFocusObserved":
                    overflowNestedEscapeRootInitialFocusObserved,
                "flowMenuPopupOverflowNestedEscapeParentFocusObserved":
                    overflowNestedEscapeParentFocusObserved,
                "flowMenuPopupOverflowNestedEscapeChildOpenedObserved":
                    overflowNestedEscapeChildOpenedObserved,
                "flowMenuPopupOverflowNestedEscapeChildCloseObserved":
                    overflowNestedEscapeChildCloseObserved,
                "flowMenuPopupOverflowNestedEscapeParentRemainsOpenObserved":
                    overflowNestedEscapeParentRemainsOpenObserved,
                "flowMenuPopupOverflowNestedEscapeParentFocusRestoredObserved":
                    overflowNestedEscapeParentFocusRestoredObserved,
                "flowMenuPopupOverflowNestedEscapeRootCloseObserved":
                    overflowNestedEscapeRootCloseObserved,
                "flowMenuPopupOverflowNestedEscapeOwnerFocusRestoredObserved":
                    overflowNestedEscapeOwnerFocusRestoredObserved,
                "flowMenuPopupOverflowExpandedObserved":
                    overflowExpandedObserved,
                "flowMenuPopupOverflowItemsRestoredObserved":
                    overflowItemsRestoredObserved,
                "flowMenuPopupContextOpenedObserved":
                    contextOpenedObserved,
                "flowMenuPopupContextA11yObserved": contextA11yObserved,
                "flowMenuPopupContextInitialFocusObserved":
                    contextInitialFocusObserved,
                "flowMenuPopupContextKeyboardMoveObserved":
                    contextKeyboardMoveObserved,
                "flowMenuPopupContextSelectionObserved":
                    contextSelectionObserved,
                "flowMenuPopupContextJavaListenerObserved":
                    contextJavaListenerObserved,
                "flowMenuPopupContextClosedObserved":
                    contextClosedObserved,
                "flowMenuPopupContextFocusRestoredObserved":
                    contextFocusRestoredObserved,
                "flowMenuPopupContextEscapeOpenedObserved":
                    contextEscapeOpenedObserved,
                "flowMenuPopupContextEscapeCloseObserved":
                    contextEscapeCloseObserved,
                "flowMenuPopupContextJavaOpenObserved":
                    contextJavaOpenObserved,
                "flowMenuPopupContextJavaCloseObserved":
                    contextJavaCloseObserved,
                "flowMenuPopupContextNestedRootOpenedObserved":
                    contextNestedRootOpenedObserved,
                "flowMenuPopupContextNestedRootInitialFocusObserved":
                    contextNestedRootInitialFocusObserved,
                "flowMenuPopupContextNestedJavaRootOpenObserved":
                    contextNestedJavaRootOpenObserved,
                "flowMenuPopupContextNestedFirstMoveObserved":
                    contextNestedFirstMoveObserved,
                "flowMenuPopupContextNestedParentFocusObserved":
                    contextNestedParentFocusObserved,
                "flowMenuPopupContextNestedChildOpenedObserved":
                    contextNestedChildOpenedObserved,
                "flowMenuPopupContextNestedA11yObserved":
                    contextNestedA11yObserved,
                "flowMenuPopupContextNestedInitialFocusObserved":
                    contextNestedInitialFocusObserved,
                "flowMenuPopupContextNestedKeyboardMoveObserved":
                    contextNestedKeyboardMoveObserved,
                "flowMenuPopupContextNestedLeftReturnObserved":
                    contextNestedLeftReturnObserved,
                "flowMenuPopupContextNestedParentRemainsOpenObserved":
                    contextNestedParentRemainsOpenObserved,
                "flowMenuPopupContextNestedParentFocusRestoredObserved":
                    contextNestedParentFocusRestoredObserved,
                "flowMenuPopupContextNestedReopenedObserved":
                    contextNestedReopenedObserved,
                "flowMenuPopupContextNestedReopenedInitialFocusObserved":
                    contextNestedReopenedInitialFocusObserved,
                "flowMenuPopupContextNestedSelectionMoveObserved":
                    contextNestedSelectionMoveObserved,
                "flowMenuPopupContextNestedSelectionObserved":
                    contextNestedSelectionObserved,
                "flowMenuPopupContextNestedJavaListenerObserved":
                    contextNestedJavaListenerObserved,
                "flowMenuPopupContextNestedJavaRootCloseObserved":
                    contextNestedJavaRootCloseObserved,
                "flowMenuPopupContextNestedClosedObserved":
                    contextNestedClosedObserved,
                "flowMenuPopupContextNestedFocusRestoredObserved":
                    contextNestedFocusRestoredObserved,
                "flowMenuPopupContextNestedEscapeRootOpenedObserved":
                    contextNestedEscapeRootOpenedObserved,
                "flowMenuPopupContextNestedEscapeRootInitialFocusObserved":
                    contextNestedEscapeRootInitialFocusObserved,
                "flowMenuPopupContextNestedEscapeJavaRootOpenObserved":
                    contextNestedEscapeJavaRootOpenObserved,
                "flowMenuPopupContextNestedEscapeFirstMoveObserved":
                    contextNestedEscapeFirstMoveObserved,
                "flowMenuPopupContextNestedEscapeParentFocusObserved":
                    contextNestedEscapeParentFocusObserved,
                "flowMenuPopupContextNestedEscapeChildOpenedObserved":
                    contextNestedEscapeChildOpenedObserved,
                "flowMenuPopupContextNestedEscapeChildCloseObserved":
                    contextNestedEscapeChildCloseObserved,
                "flowMenuPopupContextNestedEscapeParentRemainsOpenObserved":
                    contextNestedEscapeParentRemainsOpenObserved,
                "flowMenuPopupContextNestedEscapeParentFocusRestoredObserved":
                    contextNestedEscapeParentFocusRestoredObserved,
                "flowMenuPopupContextNestedEscapeRootCloseObserved":
                    contextNestedEscapeRootCloseObserved,
                "flowMenuPopupContextNestedEscapeJavaRootCloseObserved":
                    contextNestedEscapeJavaRootCloseObserved,
                "flowMenuPopupContextNestedEscapeOwnerFocusRestoredObserved":
                    contextNestedEscapeOwnerFocusRestoredObserved,
                "flowMenuPopupGridOpenedObserved": gridOpenedObserved,
                "flowMenuPopupGridVirtualizedTargetObserved":
                    gridVirtualizedTargetObserved,
                "flowMenuPopupGridRowContextObserved":
                    gridRowContextObserved,
                "flowMenuPopupGridColumnContextObserved":
                    gridColumnContextObserved,
                "flowMenuPopupGridInitialFocusObserved":
                    gridInitialFocusObserved,
                "flowMenuPopupGridKeyboardMoveObserved":
                    gridKeyboardMoveObserved,
                "flowMenuPopupGridSelectionObserved":
                    gridSelectionObserved,
                "flowMenuPopupGridJavaOpenObserved":
                    gridJavaOpenObserved,
                "flowMenuPopupGridJavaListenerObserved":
                    gridJavaListenerObserved,
                "flowMenuPopupGridJavaCloseObserved":
                    gridJavaCloseObserved,
                "flowMenuPopupGridClosedObserved": gridClosedObserved,
                "flowMenuPopupGridFocusRestoredObserved":
                    gridFocusRestoredObserved,
                "flowMenuPopupGridPointerTrustedObserved":
                    gridPointerTrustedObserved,
                "flowMenuPopupGridNestedRootOpenedObserved":
                    gridNestedRootOpenedObserved,
                "flowMenuPopupGridNestedRootInitialFocusObserved":
                    gridNestedRootInitialFocusObserved,
                "flowMenuPopupGridNestedJavaRootOpenObserved":
                    gridNestedJavaRootOpenObserved,
                "flowMenuPopupGridNestedContextObserved":
                    gridNestedContextObserved,
                "flowMenuPopupGridNestedFirstMoveObserved":
                    gridNestedFirstMoveObserved,
                "flowMenuPopupGridNestedParentFocusObserved":
                    gridNestedParentFocusObserved,
                "flowMenuPopupGridNestedChildOpenedObserved":
                    gridNestedChildOpenedObserved,
                "flowMenuPopupGridNestedA11yObserved":
                    gridNestedA11yObserved,
                "flowMenuPopupGridNestedInitialFocusObserved":
                    gridNestedInitialFocusObserved,
                "flowMenuPopupGridNestedKeyboardMoveObserved":
                    gridNestedKeyboardMoveObserved,
                "flowMenuPopupGridNestedLeftReturnObserved":
                    gridNestedLeftReturnObserved,
                "flowMenuPopupGridNestedParentRemainsOpenObserved":
                    gridNestedParentRemainsOpenObserved,
                "flowMenuPopupGridNestedParentFocusRestoredObserved":
                    gridNestedParentFocusRestoredObserved,
                "flowMenuPopupGridNestedContextRetainedObserved":
                    gridNestedContextRetainedObserved,
                "flowMenuPopupGridNestedReopenedObserved":
                    gridNestedReopenedObserved,
                "flowMenuPopupGridNestedReopenedInitialFocusObserved":
                    gridNestedReopenedInitialFocusObserved,
                "flowMenuPopupGridNestedSelectionMoveObserved":
                    gridNestedSelectionMoveObserved,
                "flowMenuPopupGridNestedSelectionObserved":
                    gridNestedSelectionObserved,
                "flowMenuPopupGridNestedJavaListenerObserved":
                    gridNestedJavaListenerObserved,
                "flowMenuPopupGridNestedJavaRootCloseObserved":
                    gridNestedJavaRootCloseObserved,
                "flowMenuPopupGridNestedClosedObserved":
                    gridNestedClosedObserved,
                "flowMenuPopupGridNestedFocusRestoredObserved":
                    gridNestedFocusRestoredObserved,
                "flowMenuPopupGridNestedEscapeRootOpenedObserved":
                    gridNestedEscapeRootOpenedObserved,
                "flowMenuPopupGridNestedEscapeRootInitialFocusObserved":
                    gridNestedEscapeRootInitialFocusObserved,
                "flowMenuPopupGridNestedEscapeJavaRootOpenObserved":
                    gridNestedEscapeJavaRootOpenObserved,
                "flowMenuPopupGridNestedEscapeFirstMoveObserved":
                    gridNestedEscapeFirstMoveObserved,
                "flowMenuPopupGridNestedEscapeParentFocusObserved":
                    gridNestedEscapeParentFocusObserved,
                "flowMenuPopupGridNestedEscapeChildOpenedObserved":
                    gridNestedEscapeChildOpenedObserved,
                "flowMenuPopupGridNestedEscapeChildCloseObserved":
                    gridNestedEscapeChildCloseObserved,
                "flowMenuPopupGridNestedEscapeParentRemainsOpenObserved":
                    gridNestedEscapeParentRemainsOpenObserved,
                "flowMenuPopupGridNestedEscapeParentFocusRestoredObserved":
                    gridNestedEscapeParentFocusRestoredObserved,
                "flowMenuPopupGridNestedEscapeRootCloseObserved":
                    gridNestedEscapeRootCloseObserved,
                "flowMenuPopupGridNestedEscapeJavaRootCloseObserved":
                    gridNestedEscapeJavaRootCloseObserved,
                "flowMenuPopupGridNestedEscapeOwnerFocusRestoredObserved":
                    gridNestedEscapeOwnerFocusRestoredObserved,
                "flowMenuPopupGridPolicyHeaderPointerTrustedObserved":
                    gridPolicyHeaderPointerTrustedObserved,
                "flowMenuPopupGridPolicyHeaderContextObserved":
                    gridPolicyHeaderContextObserved,
                "flowMenuPopupGridPolicyHeaderJavaRejectObserved":
                    gridPolicyHeaderJavaRejectObserved,
                "flowMenuPopupGridPolicyHeaderClosedObserved":
                    gridPolicyHeaderClosedObserved,
                "flowMenuPopupGridPolicyEmptyPointerTrustedObserved":
                    gridPolicyEmptyPointerTrustedObserved,
                "flowMenuPopupGridPolicyEmptyGeometryObserved":
                    gridPolicyEmptyGeometryObserved,
                "flowMenuPopupGridPolicyEmptyContextObserved":
                    gridPolicyEmptyContextObserved,
                "flowMenuPopupGridPolicyEmptyJavaRejectObserved":
                    gridPolicyEmptyJavaRejectObserved,
                "flowMenuPopupGridPolicyEmptyClosedObserved":
                    gridPolicyEmptyClosedObserved,
                "flowMenuPopupGridPolicyRowPointerTrustedObserved":
                    gridPolicyRowPointerTrustedObserved,
                "flowMenuPopupGridPolicyRowContextObserved":
                    gridPolicyRowContextObserved,
                "flowMenuPopupGridPolicyRowOpenedObserved":
                    gridPolicyRowOpenedObserved,
                "flowMenuPopupGridPolicyRowInitialFocusObserved":
                    gridPolicyRowInitialFocusObserved,
                "flowMenuPopupGridPolicyDynamicContentObserved":
                    gridPolicyDynamicContentObserved,
                "flowMenuPopupGridPolicyJavaOpenObserved":
                    gridPolicyJavaOpenObserved,
                "flowMenuPopupGridPolicyJavaListenerObserved":
                    gridPolicyJavaListenerObserved,
                "flowMenuPopupGridPolicyJavaCloseObserved":
                    gridPolicyJavaCloseObserved,
                "flowMenuPopupGridPolicyClosedObserved":
                    gridPolicyClosedObserved,
                "flowMenuPopupGridPolicyFocusRestoredObserved":
                    gridPolicyFocusRestoredObserved,
                "flowMenuPopupNativeKeyboardObserved":
                    nativeKeyboardObserved,
                "flowMenuPopupNativeKeyboardTrustedObserved":
                    nativeKeyboardTrustedObserved,
                "flowMenuPopupNativeKeyboardCurrentObserved":
                    nativeKeyboardCurrentObserved,
                "flowMenuPopupNativeKeyboardEventCount":
                    nativeKeyboardEventCount,
                "flowMenuPopupNativeKeyboardExpected":
                    nativeKeyboardExpected,
                "flowMenuPopupNativeKeyboardLastKey":
                    nativeKeyboardLastKey,
                "flowMenuPopupNativeKeyboardLastTarget":
                    nativeKeyboardLastTarget,
                "flowMenuPopupNativeKeyboardSequence":
                    nativeKeyboardSequence,
                "flowMenuPopupNativePointerObserved":
                    nativePointerObserved,
                "flowMenuPopupNativePointerTrustedObserved":
                    nativePointerTrustedObserved,
                "flowMenuPopupNativePointerEventCount":
                    nativePointerEventCount,
                "flowMenuPopupNativePointerLastTarget":
                    nativePointerLastTarget,
                "flowMenuPopupNativePointerLastButton":
                    nativePointerLastButton,
                "flowMenuPopupMenuOpenedCurrent": menuOpenedCurrent,
                "flowMenuPopupMenuOwnerExpanded": menuOwnerExpanded,
                "flowMenuPopupMenuFocusedText": menuFocusedText,
                "flowMenuPopupNestedStage": nestedMenuStage,
                "flowMenuPopupNestedStages": nestedMenuStages,
                "flowMenuPopupNestedOpenedCurrent": nestedOpenedCurrent,
                "flowMenuPopupNestedParentExpanded":
                    nestedParentExpanded,
                "flowMenuPopupNestedRootFocusedText":
                    nestedRootFocusedText,
                "flowMenuPopupNestedFocusedText": nestedFocusedText,
                "flowMenuPopupNestedStatus": nestedStatus,
                "flowMenuPopupNestedDiagnostics": nestedDiagnostics,
                "flowMenuPopupOverflowStage": overflowStage,
                "flowMenuPopupOverflowStages": overflowStages,
                "flowMenuPopupOverflowOpenedCurrent":
                    overflowOpenedCurrent,
                "flowMenuPopupOverflowCollapsedItemCount":
                    overflowCollapsedItemCount,
                "flowMenuPopupOverflowHiddenButtonCount":
                    overflowHiddenButtonCount,
                "flowMenuPopupOverflowExpectedItemId":
                    overflowExpectedItemId,
                "flowMenuPopupOverflowFocusedText":
                    overflowFocusedText,
                "flowMenuPopupOverflowStatus": overflowStatus,
                "flowMenuPopupOverflowDiagnostics":
                    overflowDiagnostics,
                "flowMenuPopupOverflowNestedStage":
                    overflowNestedStage,
                "flowMenuPopupOverflowNestedStages":
                    overflowNestedStages,
                "flowMenuPopupOverflowNestedOpenedCurrent":
                    overflowNestedOpenedCurrent,
                "flowMenuPopupOverflowNestedRootFocusedText":
                    overflowNestedRootFocusedText,
                "flowMenuPopupOverflowNestedFocusedText":
                    overflowNestedFocusedText,
                "flowMenuPopupOverflowNestedStatus":
                    overflowNestedStatus,
                "flowMenuPopupOverflowNestedDiagnostics":
                    overflowNestedDiagnostics,
                "flowMenuPopupContextOpenedCurrent": contextOpenedCurrent,
                "flowMenuPopupContextFocusedText": contextFocusedText,
                "flowMenuPopupContextNestedStage": contextNestedStage,
                "flowMenuPopupContextNestedStages": contextNestedStages,
                "flowMenuPopupContextNestedOpenedCurrent":
                    contextNestedOpenedCurrent,
                "flowMenuPopupContextNestedRootFocusedText":
                    contextNestedRootFocusedText,
                "flowMenuPopupContextNestedFocusedText":
                    contextNestedFocusedText,
                "flowMenuPopupContextNestedStatus":
                    contextNestedStatus,
                "flowMenuPopupContextNestedDiagnostics":
                    contextNestedDiagnostics,
                "flowMenuPopupGridOpenedCurrent": gridOpenedCurrent,
                "flowMenuPopupGridFocusedText": gridFocusedText,
                "flowMenuPopupGridNestedStage": gridNestedStage,
                "flowMenuPopupGridNestedStages": gridNestedStages,
                "flowMenuPopupGridNestedOpenedCurrent":
                    gridNestedOpenedCurrent,
                "flowMenuPopupGridNestedRootFocusedText":
                    gridNestedRootFocusedText,
                "flowMenuPopupGridNestedFocusedText":
                    gridNestedFocusedText,
                "flowMenuPopupGridNestedStatus": gridNestedStatus,
                "flowMenuPopupGridNestedDiagnostics":
                    gridNestedDiagnostics,
                "flowMenuPopupGridPolicyStage": gridPolicyStage,
                "flowMenuPopupGridPolicyStages": gridPolicyStages,
                "flowMenuPopupGridPolicyOpenedCurrent":
                    gridPolicyOpenedCurrent,
                "flowMenuPopupGridPolicyFocusedText":
                    gridPolicyFocusedText,
                "flowMenuPopupGridPolicyStatus": gridPolicyStatus,
                "flowMenuPopupGridPolicyLifecycleStatus":
                    gridPolicyLifecycleStatus,
                "flowMenuPopupGridPolicyActionStatus":
                    gridPolicyActionStatus,
                "flowMenuPopupGridPolicyDiagnostics":
                    gridPolicyDiagnostics,
                "flowMenuPopupMenuStatus": menuStatus,
                "flowMenuPopupContextStatus": contextStatus,
                "flowMenuPopupContextLifecycleStatus":
                    contextLifecycleStatus,
                "flowMenuPopupGridOpenStatus": gridOpenStatus,
                "flowMenuPopupGridActionStatus": gridActionStatus,
                "flowMenuPopupGridLifecycleStatus":
                    gridLifecycleStatus,
                "flowMenuPopupGridPointerTargetText":
                    gridPointerTargetText,
                "flowMenuPopupGridRenderedItemCount":
                    gridRenderedItemCount,
                "flowMenuPopupActivePath": activePath,
                "flowMenuPopupMarkerError": markerError,
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

    private enum NativeKey {
        case arrowDown
        case arrowLeft
        case arrowRight
        case returnKey
        case escape

        var name: String {
            switch self {
            case .arrowDown:
                return "ArrowDown"
            case .arrowLeft:
                return "ArrowLeft"
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
            case .arrowLeft:
                return "\u{f702}"
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
            case .arrowLeft:
                return 123
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

    private static func prepareNestedMenuKey(
        stage: String,
        rootOpened: Bool? = nil,
        nestedOpened: Bool? = nil,
        focusOwner: Bool = false
    ) -> String {
        let rootCondition = rootOpened.map {
            "menu?._subMenu?.opened === \($0 ? "true" : "false")"
        } ?? "true"
        let nestedCondition = nestedOpened.map {
            "menu?._subMenu?._subMenu?.opened === "
                + ($0 ? "true" : "false")
        } ?? "true"
        let focusOwnerStatement = focusOwner
            ? "owner.focus({ preventScroll: true });"
            : ""
        return """
        (() => {
          const marker = window.VaadinDesktopFlowMenuPopup || {};
          window.VaadinDesktopFlowMenuPopup = marker;
          const menu =
            document.querySelector('#framework-flow-menu-popup-menubar');
          const owner = Array.from(
            menu?.querySelectorAll('vaadin-menu-bar-button') || [])
            .find((element) =>
              String(element.textContent || '').includes('Operations'));
          if (!menu || !owner
              || !(\(rootCondition))
              || !(\(nestedCondition))) {
            return false;
          }
          marker.nestedMenuStage = '\(stage)';
          marker.nestedMenuStages =
            Array.isArray(marker.nestedMenuStages)
              ? marker.nestedMenuStages
              : [];
          marker.nestedMenuStages.push('\(stage)');
          \(focusOwnerStatement)
          return true;
        })()
        """
    }

    private static func prepareOverflowMenuKey(
        stage: String,
        opened: Bool,
        focusOwner: Bool = false
    ) -> String {
        let focusOwnerStatement = focusOwner
            ? "overflow.focus({ preventScroll: true });"
            : ""
        return """
        (() => {
          const marker = window.VaadinDesktopFlowMenuPopup || {};
          window.VaadinDesktopFlowMenuPopup = marker;
          const menu = document.querySelector(
            '#framework-flow-menu-popup-overflow-menubar');
          const overflow = menu?._overflow
            || menu?.querySelector(
              'vaadin-menu-bar-button[slot="overflow"]');
          const children = Array.from(
            overflow?.item?.children || []);
          const firstComponent = children[0]?.component;
          const expectedItemId = String(firstComponent?.id || '');
          if (!menu || !overflow
              || overflow.hasAttribute('hidden')
              || children.length === 0
              || menu?._subMenu?.opened !== \(opened ? "true" : "false")
              || !expectedItemId) {
            return false;
          }
          marker.overflowStage = '\(stage)';
          marker.overflowStages =
            Array.isArray(marker.overflowStages)
              ? marker.overflowStages
              : [];
          marker.overflowStages.push('\(stage)');
          marker.overflowExpectedItemId = expectedItemId;
          marker.overflowExpectedAction = expectedItemId.replace(
            'framework-flow-menu-popup-overflow-', '');
          \(focusOwnerStatement)
          return true;
        })()
        """
    }

    private static func prepareOverflowNestedMenuKey(
        stage: String,
        rootOpened: Bool,
        childOpened: Bool,
        focusOwner: Bool = false
    ) -> String {
        let focusOwnerStatement = focusOwner
            ? "overflow.focus({ preventScroll: true });"
            : ""
        return """
        (() => {
          const marker = window.VaadinDesktopFlowMenuPopup || {};
          window.VaadinDesktopFlowMenuPopup = marker;
          const menu = document.querySelector(
            '#framework-flow-menu-popup-overflow-menubar');
          const overflow = menu?._overflow
            || menu?.querySelector(
              'vaadin-menu-bar-button[slot="overflow"]');
          const children = Array.from(
            overflow?.item?.children || []);
          const parentId =
            'framework-flow-menu-popup-overflow-observability';
          const collapsedParent = children.find((item) =>
            String(item?.component?.id || '') === parentId);
          const childMenu = menu?._subMenu?._subMenu;
          const childOpenedCurrent = childMenu?.opened === true;
          if (!menu || !overflow
              || overflow.hasAttribute('hidden')
              || !collapsedParent
              || menu?._subMenu?.opened
                !== \(rootOpened ? "true" : "false")
              || childOpenedCurrent
                !== \(childOpened ? "true" : "false")) {
            return false;
          }
          marker.overflowNestedStage = '\(stage)';
          marker.overflowNestedStages =
            Array.isArray(marker.overflowNestedStages)
              ? marker.overflowNestedStages
              : [];
          marker.overflowNestedStages.push('\(stage)');
          \(focusOwnerStatement)
          return true;
        })()
        """
    }

    private static let expandOverflowMenuScript = """
    (() => {
      const marker = window.VaadinDesktopFlowMenuPopup || {};
      window.VaadinDesktopFlowMenuPopup = marker;
      const menu = document.querySelector(
        '#framework-flow-menu-popup-overflow-menubar');
      if (!menu || menu?._subMenu?.opened === true
          || !marker.overflowExpectedItemId) {
        return false;
      }
      marker.overflowStage = 'expand';
      marker.overflowStages =
        Array.isArray(marker.overflowStages)
          ? marker.overflowStages
          : [];
      marker.overflowStages.push('expand');
      marker.overflowExpandDispatched = true;
      menu.style.width = '900px';
      return true;
    })()
    """

    private static let navigationScript = """
    (() => {
      const trigger =
        document.querySelector('#framework-flow-menu-popup-open-route');
      if (!trigger || typeof trigger.click !== 'function') return false;
      trigger.click();
      return true;
    })()
    """

    private static let openMenuScript = """
    (() => {
      const menu =
        document.querySelector('#framework-flow-menu-popup-menubar');
      const owner = Array.from(
        menu?.querySelectorAll('vaadin-menu-bar-button') || [])
        .find((element) =>
          String(element.textContent || '').includes('Operations'));
      if (!menu || !owner) return false;
      window.VaadinDesktopFlowMenuPopup = {
        menuOpenDispatched: true,
        menuOpenedObserved: false,
        menuA11yObserved: false,
        menuOwnerFocusWhileOpenObserved: false,
        menuExpandedButtonMatchesOwner: false,
        menuKeyDiagnostics: [],
        menuInitialFocusDispatched: true,
        menuInitialFocusObserved: false,
        menuMoveDispatched: false,
        menuKeyboardMoveObserved: false,
        menuSelectionDispatched: false,
        menuSelectionObserved: false,
        menuJavaListenerObserved: false,
        menuClosedObserved: false,
        menuFocusRestoredObserved: false,
        menuEscapeOpenDispatched: false,
        menuEscapeOpenedObserved: false,
        menuEscapeDispatched: false,
        menuEscapeCloseObserved: false,
        nestedMenuStage: '',
        nestedMenuStages: [],
        nestedRootOpenedObserved: false,
        nestedRootInitialFocusObserved: false,
        nestedFirstMoveObserved: false,
        nestedParentFocusObserved: false,
        nestedChildOpenedObserved: false,
        nestedA11yObserved: false,
        nestedInitialFocusObserved: false,
        nestedKeyboardMoveObserved: false,
        nestedLeftReturnObserved: false,
        nestedParentRemainsOpenObserved: false,
        nestedParentFocusRestoredObserved: false,
        nestedReopenedObserved: false,
        nestedReopenedInitialFocusObserved: false,
        nestedSelectionMoveObserved: false,
        nestedSelectionObserved: false,
        nestedJavaListenerObserved: false,
        nestedClosedObserved: false,
        nestedFocusRestoredObserved: false,
        nestedEscapeRootOpenedObserved: false,
        nestedEscapeFirstMoveObserved: false,
        nestedEscapeParentFocusObserved: false,
        nestedEscapeChildOpenedObserved: false,
        nestedEscapeChildCloseObserved: false,
        nestedEscapeParentRemainsOpenObserved: false,
        nestedEscapeParentFocusRestoredObserved: false,
        nestedEscapeRootCloseObserved: false,
        nestedEscapeOwnerFocusRestoredObserved: false,
        overflowStage: '',
        overflowStages: [],
        overflowExpectedItemId: '',
        overflowExpectedAction: '',
        overflowExpandDispatched: false,
        overflowNarrowObserved: false,
        overflowOpenedObserved: false,
        overflowA11yObserved: false,
        overflowInitialFocusObserved: false,
        overflowSelectionObserved: false,
        overflowJavaListenerObserved: false,
        overflowClosedObserved: false,
        overflowFocusRestoredObserved: false,
        overflowEscapeOpenedObserved: false,
        overflowEscapeInitialFocusObserved: false,
        overflowEscapeCloseObserved: false,
        overflowEscapeFocusRestoredObserved: false,
        overflowNestedStage: '',
        overflowNestedStages: [],
        overflowNestedCollapsedParentObserved: false,
        overflowNestedRootOpenedObserved: false,
        overflowNestedRootInitialFocusObserved: false,
        overflowNestedParentFocusObserved: false,
        overflowNestedChildOpenedObserved: false,
        overflowNestedA11yObserved: false,
        overflowNestedInitialFocusObserved: false,
        overflowNestedKeyboardMoveObserved: false,
        overflowNestedLeftReturnObserved: false,
        overflowNestedParentRemainsOpenObserved: false,
        overflowNestedParentFocusRestoredObserved: false,
        overflowNestedReopenedObserved: false,
        overflowNestedReopenedInitialFocusObserved: false,
        overflowNestedSelectionMoveObserved: false,
        overflowNestedSelectionObserved: false,
        overflowNestedJavaListenerObserved: false,
        overflowNestedClosedObserved: false,
        overflowNestedFocusRestoredObserved: false,
        overflowNestedEscapeRootOpenedObserved: false,
        overflowNestedEscapeRootInitialFocusObserved: false,
        overflowNestedEscapeParentFocusObserved: false,
        overflowNestedEscapeChildOpenedObserved: false,
        overflowNestedEscapeChildCloseObserved: false,
        overflowNestedEscapeParentRemainsOpenObserved: false,
        overflowNestedEscapeParentFocusRestoredObserved: false,
        overflowNestedEscapeRootCloseObserved: false,
        overflowNestedEscapeOwnerFocusRestoredObserved: false,
        overflowExpandedObserved: false,
        overflowItemsRestoredObserved: false,
        contextOpenDispatchCount: 0,
        contextOpenedObserved: false,
        contextA11yObserved: false,
        contextInitialFocusObserved: false,
        contextMoveDispatched: false,
        contextKeyboardMoveObserved: false,
        contextSelectionDispatched: false,
        contextSelectionObserved: false,
        contextJavaListenerObserved: false,
        contextClosedObserved: false,
        contextFocusRestoredObserved: false,
        contextEscapeOpenedObserved: false,
        contextEscapeDispatched: false,
        contextEscapeCloseObserved: false,
        contextJavaOpenObserved: false,
        contextJavaCloseObserved: false,
        contextNestedStage: '',
        contextNestedStages: [],
        contextNestedRootOpenedObserved: false,
        contextNestedRootInitialFocusObserved: false,
        contextNestedJavaRootOpenObserved: false,
        contextNestedFirstMoveObserved: false,
        contextNestedParentFocusObserved: false,
        contextNestedChildOpenedObserved: false,
        contextNestedA11yObserved: false,
        contextNestedInitialFocusObserved: false,
        contextNestedKeyboardMoveObserved: false,
        contextNestedLeftReturnObserved: false,
        contextNestedParentRemainsOpenObserved: false,
        contextNestedParentFocusRestoredObserved: false,
        contextNestedReopenedObserved: false,
        contextNestedReopenedInitialFocusObserved: false,
        contextNestedSelectionMoveObserved: false,
        contextNestedSelectionObserved: false,
        contextNestedJavaListenerObserved: false,
        contextNestedJavaRootCloseObserved: false,
        contextNestedClosedObserved: false,
        contextNestedFocusRestoredObserved: false,
        contextNestedEscapeRootOpenedObserved: false,
        contextNestedEscapeRootInitialFocusObserved: false,
        contextNestedEscapeJavaRootOpenObserved: false,
        contextNestedEscapeFirstMoveObserved: false,
        contextNestedEscapeParentFocusObserved: false,
        contextNestedEscapeChildOpenedObserved: false,
        contextNestedEscapeChildCloseObserved: false,
        contextNestedEscapeParentRemainsOpenObserved: false,
        contextNestedEscapeParentFocusRestoredObserved: false,
        contextNestedEscapeRootCloseObserved: false,
        contextNestedEscapeJavaRootCloseObserved: false,
        contextNestedEscapeOwnerFocusRestoredObserved: false,
        gridViewportDispatched: false,
        gridPointerDispatched: false,
        gridPointerTrustedObserved: false,
        gridPointerTargetText: '',
        gridOpenedObserved: false,
        gridVirtualizedTargetObserved: false,
        gridRowContextObserved: false,
        gridColumnContextObserved: false,
        gridInitialFocusObserved: false,
        gridMoveDispatched: false,
        gridKeyboardMoveObserved: false,
        gridSelectionDispatched: false,
        gridSelectionObserved: false,
        gridJavaOpenObserved: false,
        gridJavaListenerObserved: false,
        gridJavaCloseObserved: false,
        gridClosedObserved: false,
        gridFocusRestoredObserved: false,
        gridNestedStage: '',
        gridNestedStages: [],
        gridNestedRootOpenedObserved: false,
        gridNestedRootInitialFocusObserved: false,
        gridNestedJavaRootOpenObserved: false,
        gridNestedContextObserved: false,
        gridNestedFirstMoveObserved: false,
        gridNestedParentFocusObserved: false,
        gridNestedChildOpenedObserved: false,
        gridNestedA11yObserved: false,
        gridNestedInitialFocusObserved: false,
        gridNestedKeyboardMoveObserved: false,
        gridNestedLeftReturnObserved: false,
        gridNestedParentRemainsOpenObserved: false,
        gridNestedParentFocusRestoredObserved: false,
        gridNestedContextRetainedObserved: false,
        gridNestedReopenedObserved: false,
        gridNestedReopenedInitialFocusObserved: false,
        gridNestedSelectionMoveObserved: false,
        gridNestedSelectionObserved: false,
        gridNestedJavaListenerObserved: false,
        gridNestedJavaRootCloseObserved: false,
        gridNestedClosedObserved: false,
        gridNestedFocusRestoredObserved: false,
        gridNestedEscapeRootOpenedObserved: false,
        gridNestedEscapeRootInitialFocusObserved: false,
        gridNestedEscapeJavaRootOpenObserved: false,
        gridNestedEscapeFirstMoveObserved: false,
        gridNestedEscapeParentFocusObserved: false,
        gridNestedEscapeChildOpenedObserved: false,
        gridNestedEscapeChildCloseObserved: false,
        gridNestedEscapeParentRemainsOpenObserved: false,
        gridNestedEscapeParentFocusRestoredObserved: false,
        gridNestedEscapeRootCloseObserved: false,
        gridNestedEscapeJavaRootCloseObserved: false,
        gridNestedEscapeOwnerFocusRestoredObserved: false,
        gridPolicyStage: '',
        gridPolicyStages: [],
        gridPolicyHeaderPointerTrustedObserved: false,
        gridPolicyHeaderContextObserved: false,
        gridPolicyHeaderJavaRejectObserved: false,
        gridPolicyHeaderClosedObserved: false,
        gridPolicyEmptyPointerTrustedObserved: false,
        gridPolicyEmptyGeometryObserved: false,
        gridPolicyEmptyContextObserved: false,
        gridPolicyEmptyJavaRejectObserved: false,
        gridPolicyEmptyClosedObserved: false,
        gridPolicyRowPointerTrustedObserved: false,
        gridPolicyRowContextObserved: false,
        gridPolicyRowOpenedObserved: false,
        gridPolicyRowInitialFocusObserved: false,
        gridPolicyDynamicContentObserved: false,
        gridPolicyJavaOpenObserved: false,
        gridPolicySelectionDispatched: false,
        gridPolicyJavaListenerObserved: false,
        gridPolicyJavaCloseObserved: false,
        gridPolicyClosedObserved: false,
        gridPolicyFocusRestoredObserved: false,
        gridPolicyEventSection: '',
        gridPolicyEventItemKey: '',
        gridPolicyEventColumnId: '',
        gridPolicyEventTargetText: '',
        nativeKeyboardObserved: false,
        nativeKeyboardTrustedObserved: false,
        nativeKeyboardCurrentObserved: false,
        nativeKeyboardExpected: '',
        nativeKeyboardLastKey: '',
        nativeKeyboardLastTarget: '',
        nativeKeyboardSequence: [],
        nativePointerObserved: false,
        nativePointerTrustedObserved: false,
        nativePointerEventCount: 0,
        nativePointerLastTarget: '',
        nativePointerLastButton: -1,
        error: ''
      };
      const marker = window.VaadinDesktopFlowMenuPopup;
      const activeDeepElements = () => {
        const elements = [];
        let current = document.activeElement;
        while (current) {
          elements.push(current);
          current = current.shadowRoot?.activeElement || null;
        }
        return elements;
      };
      const ownerHasDeepFocus = () =>
        activeDeepElements().some((element) =>
          element === owner
            || owner.contains?.(element)
            || owner.shadowRoot?.contains?.(element));
      const recordMenuKeyState = (stage, event) => {
        const list = menu._subMenu?._listBox;
        const focused = list?.focused;
        marker.menuOwnerFocusWhileOpenObserved =
          marker.menuOwnerFocusWhileOpenObserved === true
            || (menu._subMenu?.opened === true
              && ownerHasDeepFocus());
        marker.menuExpandedButtonMatchesOwner =
          marker.menuExpandedButtonMatchesOwner === true
            || menu._expandedButton === owner;
        marker.menuKeyDiagnostics.push([
          stage,
          String(event.key || ''),
          `expanded=${menu._expandedButton === owner}`,
          `opened=${menu._subMenu?.opened === true}`,
          `ownerActive=${ownerHasDeepFocus()}`,
          `focused=${String(focused?.id || focused?.localName || '')}`,
          `defaultPrevented=${event.defaultPrevented === true}`
        ].join(':'));
      };
      menu.addEventListener('keydown', (event) => {
        recordMenuKeyState('after-handler', event);
        requestAnimationFrame(() =>
          recordMenuKeyState('animation-frame', event));
      });
      menu.scrollIntoView({ block: 'center', inline: 'center' });
      owner.focus({ preventScroll: true });
      return true;
    })()
    """

    private static let moveMenuScript = """
    (() => {
      const marker = window.VaadinDesktopFlowMenuPopup || {};
      const menu =
        document.querySelector('#framework-flow-menu-popup-menubar');
      if (!menu || menu._subMenu?.opened !== true) return false;
      marker.menuMoveDispatched = true;
      return true;
    })()
    """

    private static let selectMenuScript = """
    (() => {
      const marker = window.VaadinDesktopFlowMenuPopup || {};
      const menu =
        document.querySelector('#framework-flow-menu-popup-menubar');
      if (!menu || menu._subMenu?.opened !== true) return false;
      marker.menuSelectionDispatched = true;
      return true;
    })()
    """

    private static let reopenMenuScript = """
    (() => {
      const marker = window.VaadinDesktopFlowMenuPopup || {};
      const menu =
        document.querySelector('#framework-flow-menu-popup-menubar');
      const owner = Array.from(
        menu?.querySelectorAll('vaadin-menu-bar-button') || [])
        .find((element) =>
          String(element.textContent || '').includes('Operations'));
      if (!menu || !owner || menu._subMenu?.opened === true) return false;
      marker.menuEscapeOpenDispatched = true;
      owner.focus({ preventScroll: true });
      return true;
    })()
    """

    private static let closeMenuScript = """
    (() => {
      const marker = window.VaadinDesktopFlowMenuPopup || {};
      const menu =
        document.querySelector('#framework-flow-menu-popup-menubar');
      if (!menu || menu._subMenu?.opened !== true) return false;
      marker.menuEscapeDispatched = true;
      return true;
    })()
    """

    private static func prepareContextPointerScript(
        nestedStage: String? = nil
    ) -> String {
        let nestedStageStatements: String
        if let nestedStage {
            nestedStageStatements = """
            marker.contextNestedStage = '\(nestedStage)';
            marker.contextNestedStages =
              Array.isArray(marker.contextNestedStages)
                ? marker.contextNestedStages
                : [];
            marker.contextNestedStages.push('\(nestedStage)');
            """
        } else {
            nestedStageStatements = ""
        }
        return """
        (() => {
          const marker = window.VaadinDesktopFlowMenuPopup || {};
          window.VaadinDesktopFlowMenuPopup = marker;
          const target =
            document.querySelector(
              '#framework-flow-menu-popup-context-target');
          if (!target) return '';
          target.scrollIntoView({ block: 'center', inline: 'center' });
          target.focus({ preventScroll: true });
          marker.contextOpenDispatchCount =
            Number(marker.contextOpenDispatchCount || 0) + 1;
          \(nestedStageStatements)
          target.addEventListener('contextmenu', (event) => {
            marker.nativePointerObserved = true;
            marker.nativePointerTrustedObserved =
              marker.nativePointerTrustedObserved === true
                || event.isTrusted === true;
            marker.nativePointerEventCount =
              Number(marker.nativePointerEventCount || 0) + 1;
            marker.nativePointerLastTarget =
              String(event.target?.id || event.target?.localName || '');
            marker.nativePointerLastButton = Number(event.button);
            marker.nativePointerLastX = Number(event.clientX);
            marker.nativePointerLastY = Number(event.clientY);
          }, { capture: true, once: true });
          const rect = target.getBoundingClientRect();
          return JSON.stringify({
            centerX: rect.left + rect.width / 2,
            centerY: rect.top + rect.height / 2
          });
        })()
        """
    }

    private static func prepareNestedContextMenuKey(
        stage: String,
        rootOpened: Bool,
        childOpened: Bool
    ) -> String {
        """
        (() => {
          const marker = window.VaadinDesktopFlowMenuPopup || {};
          window.VaadinDesktopFlowMenuPopup = marker;
          const menu = document.querySelector(
            '#framework-flow-menu-popup-context-menu');
          const child = menu?._subMenu;
          if (!menu
              || menu.opened !== \(rootOpened ? "true" : "false")
              || (child?.opened === true)
                !== \(childOpened ? "true" : "false")) {
            return false;
          }
          marker.contextNestedStage = '\(stage)';
          marker.contextNestedStages =
            Array.isArray(marker.contextNestedStages)
              ? marker.contextNestedStages
              : [];
          marker.contextNestedStages.push('\(stage)');
          return true;
        })()
        """
    }

    private static let moveContextScript = """
    (() => {
      const marker = window.VaadinDesktopFlowMenuPopup || {};
      const findDeep = (node) => {
        if (!node) return null;
        if (node.nodeType === Node.ELEMENT_NODE
            && (node.id === 'framework-flow-menu-popup-context-menu'
              || node.localName === 'vaadin-context-menu')) {
          return node;
        }
        if (node.shadowRoot) {
          const shadowResult = findDeep(node.shadowRoot);
          if (shadowResult) return shadowResult;
        }
        if (typeof node.assignedNodes === 'function') {
          for (const child of node.assignedNodes({ flatten: true })) {
            const result = findDeep(child);
            if (result) return result;
          }
        }
        const children = node.children || node.childNodes || [];
        for (const child of children) {
          const result = findDeep(child);
          if (result) return result;
        }
        return null;
      };
      const menu =
        document.querySelector('#framework-flow-menu-popup-context-menu')
          || findDeep(document);
      if (!menu || menu.opened !== true) return false;
      marker.contextMoveDispatched = true;
      return true;
    })()
    """

    private static let selectContextScript = """
    (() => {
      const marker = window.VaadinDesktopFlowMenuPopup || {};
      const findDeep = (node) => {
        if (!node) return null;
        if (node.nodeType === Node.ELEMENT_NODE
            && (node.id === 'framework-flow-menu-popup-context-menu'
              || node.localName === 'vaadin-context-menu')) {
          return node;
        }
        if (node.shadowRoot) {
          const shadowResult = findDeep(node.shadowRoot);
          if (shadowResult) return shadowResult;
        }
        if (typeof node.assignedNodes === 'function') {
          for (const child of node.assignedNodes({ flatten: true })) {
            const result = findDeep(child);
            if (result) return result;
          }
        }
        const children = node.children || node.childNodes || [];
        for (const child of children) {
          const result = findDeep(child);
          if (result) return result;
        }
        return null;
      };
      const menu =
        document.querySelector('#framework-flow-menu-popup-context-menu')
          || findDeep(document);
      if (!menu || menu.opened !== true) return false;
      marker.contextSelectionDispatched = true;
      return true;
    })()
    """

    private static let closeContextScript = """
    (() => {
      const marker = window.VaadinDesktopFlowMenuPopup || {};
      const findDeep = (node) => {
        if (!node) return null;
        if (node.nodeType === Node.ELEMENT_NODE
            && (node.id === 'framework-flow-menu-popup-context-menu'
              || node.localName === 'vaadin-context-menu')) {
          return node;
        }
        if (node.shadowRoot) {
          const shadowResult = findDeep(node.shadowRoot);
          if (shadowResult) return shadowResult;
        }
        if (typeof node.assignedNodes === 'function') {
          for (const child of node.assignedNodes({ flatten: true })) {
            const result = findDeep(child);
            if (result) return result;
          }
        }
        const children = node.children || node.childNodes || [];
        for (const child of children) {
          const result = findDeep(child);
          if (result) return result;
        }
        return null;
      };
      const menu =
        document.querySelector('#framework-flow-menu-popup-context-menu')
          || findDeep(document);
      if (!menu || menu.opened !== true) return false;
      marker.contextEscapeDispatched = true;
      return true;
    })()
    """

    private static let prepareGridViewportScript = """
    (() => {
      const marker = window.VaadinDesktopFlowMenuPopup || {};
      window.VaadinDesktopFlowMenuPopup = marker;
      const grid =
        document.querySelector('#framework-flow-menu-popup-grid');
      if (!grid || typeof grid.scrollToIndex !== 'function') return false;
      grid.scrollIntoView({ block: 'center', inline: 'center' });
      grid.focus({ preventScroll: true });
      grid.scrollToIndex(120);
      marker.gridViewportDispatched = true;
      return true;
    })()
    """

    private static func prepareGridPointerScript(
        nestedStage: String? = nil
    ) -> String {
        let nestedStageStatements: String
        if let nestedStage {
            nestedStageStatements = """
            marker.gridNestedStage = '\(nestedStage)';
            marker.gridNestedStages =
              Array.isArray(marker.gridNestedStages)
                ? marker.gridNestedStages
                : [];
            marker.gridNestedStages.push('\(nestedStage)');
            """
        } else {
            nestedStageStatements = ""
        }
        return """
        (() => {
          const marker = window.VaadinDesktopFlowMenuPopup || {};
          window.VaadinDesktopFlowMenuPopup = marker;
          \(nestedStageStatements)
      const grid =
        document.querySelector('#framework-flow-menu-popup-grid');
      if (!grid) return '';
      const visit = (node) => {
        if (!node) return null;
        if (node.nodeType === Node.ELEMENT_NODE
            && node.localName === 'vaadin-grid-cell-content'
            && String(node.textContent || '').trim() === 'M-121') {
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
      const target = visit(grid);
      if (!target) {
        grid.scrollToIndex?.(120);
        return '';
      }
      const rect = target.getBoundingClientRect();
      const gridRect = grid.getBoundingClientRect();
      const visible =
        rect.width > 0
          && rect.height > 0
          && rect.bottom > gridRect.top
          && rect.top < gridRect.bottom
          && rect.right > gridRect.left
          && rect.left < gridRect.right;
      if (!visible) return '';
      grid.focus({ preventScroll: true });
      marker.gridPointerDispatched = true;
      grid.addEventListener('contextmenu', (event) => {
        const path = event.composedPath?.() || [];
        const cell = path.find((element) =>
          element?.localName === 'vaadin-grid-cell-content'
            && String(element.textContent || '').trim() === 'M-121');
        marker.nativePointerObserved = true;
        marker.nativePointerTrustedObserved =
          marker.nativePointerTrustedObserved === true
            || event.isTrusted === true;
        marker.nativePointerEventCount =
          Number(marker.nativePointerEventCount || 0) + 1;
        marker.nativePointerLastTarget =
          String(event.target?.id || event.target?.localName || '');
        marker.nativePointerLastButton = Number(event.button);
        marker.nativePointerLastX = Number(event.clientX);
        marker.nativePointerLastY = Number(event.clientY);
        marker.gridPointerTrustedObserved =
          marker.gridPointerTrustedObserved === true
            || event.isTrusted === true;
        marker.gridPointerTargetText =
          String(cell?.textContent || event.target?.textContent || '')
            .replace(/\\s+/g, ' ').trim();
        marker.gridVirtualizedTargetObserved =
          marker.gridVirtualizedTargetObserved === true
            || !!cell;
      }, { capture: true, once: true });
      return JSON.stringify({
        centerX: rect.left + rect.width / 2,
        centerY: rect.top + rect.height / 2
      });
        })()
    """
    }

    private static func prepareNestedGridContextMenuKey(
        stage: String,
        rootOpened: Bool,
        childOpened: Bool
    ) -> String {
        """
        (() => {
          const marker = window.VaadinDesktopFlowMenuPopup || {};
          window.VaadinDesktopFlowMenuPopup = marker;
          const menu = document.querySelector(
            '#framework-flow-menu-popup-grid-context-menu');
          const child = menu?._subMenu;
          if (!menu
              || menu.opened !== \(rootOpened ? "true" : "false")
              || (child?.opened === true)
                !== \(childOpened ? "true" : "false")) {
            return false;
          }
          marker.gridNestedStage = '\(stage)';
          marker.gridNestedStages =
            Array.isArray(marker.gridNestedStages)
              ? marker.gridNestedStages
              : [];
          marker.gridNestedStages.push('\(stage)');
          return true;
        })()
        """
    }

    private static let moveGridContextScript = """
    (() => {
      const marker = window.VaadinDesktopFlowMenuPopup || {};
      const menu = document.querySelector(
        '#framework-flow-menu-popup-grid-context-menu');
      if (!menu || menu.opened !== true) return false;
      marker.gridMoveDispatched = true;
      return true;
    })()
    """

    private static let selectGridContextScript = """
    (() => {
      const marker = window.VaadinDesktopFlowMenuPopup || {};
      const menu = document.querySelector(
        '#framework-flow-menu-popup-grid-context-menu');
      if (!menu || menu.opened !== true) return false;
      marker.gridSelectionDispatched = true;
      return true;
    })()
    """

    private static func prepareGridPolicyPointerScript(
        stage: String
    ) -> String {
        """
        (() => {
          const marker = window.VaadinDesktopFlowMenuPopup || {};
          window.VaadinDesktopFlowMenuPopup = marker;
          const grid = document.querySelector(
            '#framework-flow-menu-popup-grid-policy');
          const menu = document.querySelector(
            '#framework-flow-menu-popup-grid-policy-context-menu');
          if (!grid || menu?.opened === true) {
            return `gridPolicyHostUnavailable:grid=${!!grid},`
              + `opened=${menu?.opened === true}`;
          }
          const visit = (node, expectedText) => {
            if (!node) return null;
            if (node.nodeType === Node.ELEMENT_NODE
                && node.localName === 'vaadin-grid-cell-content'
                && String(node.textContent || '').trim()
                  === expectedText) {
              return node;
            }
            if (node.shadowRoot) {
              const shadowResult = visit(node.shadowRoot, expectedText);
              if (shadowResult) return shadowResult;
            }
            if (typeof node.assignedNodes === 'function') {
              for (const child of node.assignedNodes({ flatten: true })) {
                const result = visit(child, expectedText);
                if (result) return result;
              }
            }
            const children = node.children || node.childNodes || [];
            for (const child of children) {
              const result = visit(child, expectedText);
              if (result) return result;
            }
            return null;
          };
          grid.scrollIntoView({ block: 'center', inline: 'center' });
          grid.focus({ preventScroll: true });
          const headerTarget = visit(grid, 'Policy ticket');
          const rowTarget = visit(grid, 'P-001');
          if (!headerTarget || !rowTarget) {
            const cellTexts = [];
            const collectCells = (node) => {
              if (!node) return;
              if (node.nodeType === Node.ELEMENT_NODE
                  && node.localName === 'vaadin-grid-cell-content') {
                cellTexts.push(String(node.textContent || '')
                  .replace(/\\s+/g, ' ').trim());
              }
              if (node.shadowRoot) collectCells(node.shadowRoot);
              if (typeof node.assignedNodes === 'function') {
                for (const child of node.assignedNodes({ flatten: true })) {
                  collectCells(child);
                }
              }
              const children = node.children || node.childNodes || [];
              for (const child of children) collectCells(child);
            };
            collectCells(grid);
            return `gridPolicyTargetsMissing:header=${!!headerTarget},`
              + `row=${!!rowTarget},cells=${cellTexts.join('|')}`;
          }
          const gridRect = grid.getBoundingClientRect();
          const headerRect = headerTarget.getBoundingClientRect();
          const rowRect = rowTarget.getBoundingClientRect();
          let centerX = 0;
          let centerY = 0;
          if ('\(stage)' === 'header') {
            centerX = headerRect.left + headerRect.width / 2;
            centerY = headerRect.top + headerRect.height / 2;
          } else if ('\(stage)' === 'row') {
            centerX = rowRect.left + rowRect.width / 2;
            centerY = rowRect.top + rowRect.height / 2;
          } else if ('\(stage)' === 'empty') {
            centerX = rowRect.left + rowRect.width / 2;
            centerY = Math.min(
              gridRect.bottom - 24,
              rowRect.bottom + Math.max(
                24,
                (gridRect.bottom - rowRect.bottom) / 2));
            marker.gridPolicyEmptyGeometryObserved =
              centerY > rowRect.bottom + 8
                && centerY < gridRect.bottom - 8;
            marker.gridPolicyEmptyRowBottom = Number(rowRect.bottom);
            marker.gridPolicyEmptyGridBottom = Number(gridRect.bottom);
          } else {
            return '';
          }
          if (centerX <= gridRect.left || centerX >= gridRect.right
              || centerY <= gridRect.top || centerY >= gridRect.bottom) {
            return '';
          }
          marker.gridPolicyStage = '\(stage)';
          marker.gridPolicyStages =
            Array.isArray(marker.gridPolicyStages)
              ? marker.gridPolicyStages
              : [];
          marker.gridPolicyStages.push('\(stage)');
          grid.addEventListener('contextmenu', (event) => {
            const context = typeof grid.getEventContext === 'function'
              ? grid.getEventContext(event)
              : {};
            const section = String(context?.section || '');
            const itemKey = String(context?.item?.key || '');
            const columnId = String(context?.column?.id || '');
            const path = event.composedPath?.() || [];
            const pathCell = path.find((element) =>
              element?.localName === 'vaadin-grid-cell-content');
            const targetText = String(
              pathCell?.textContent || event.target?.textContent || '')
              .replace(/\\s+/g, ' ').trim();
            marker.nativePointerObserved = true;
            marker.nativePointerTrustedObserved =
              marker.nativePointerTrustedObserved === true
                || event.isTrusted === true;
            marker.nativePointerEventCount =
              Number(marker.nativePointerEventCount || 0) + 1;
            marker.nativePointerLastTarget = String(
              event.target?.id || event.target?.localName || '');
            marker.nativePointerLastButton = Number(event.button);
            marker.nativePointerLastX = Number(event.clientX);
            marker.nativePointerLastY = Number(event.clientY);
            marker.gridPolicyEventSection = section;
            marker.gridPolicyEventItemKey = itemKey;
            marker.gridPolicyEventColumnId = columnId;
            marker.gridPolicyEventTargetText = targetText;
            if ('\(stage)' === 'header') {
              marker.gridPolicyHeaderPointerTrustedObserved =
                event.isTrusted === true;
              marker.gridPolicyHeaderContextObserved =
                section === 'header'
                  && !itemKey
                  && columnId === 'policy-ticket'
                  && targetText.includes('Policy ticket');
            } else if ('\(stage)' === 'empty') {
              marker.gridPolicyEmptyPointerTrustedObserved =
                event.isTrusted === true;
              marker.gridPolicyEmptyContextObserved =
                !itemKey
                  && section !== 'header'
                  && event.clientY
                    > Number(marker.gridPolicyEmptyRowBottom || 0) + 8
                  && event.clientY
                    < Number(marker.gridPolicyEmptyGridBottom || 0) - 8;
            } else {
              marker.gridPolicyRowPointerTrustedObserved =
                event.isTrusted === true;
              marker.gridPolicyRowContextObserved =
                section === 'body'
                  && !!itemKey
                  && columnId === 'policy-ticket'
                  && targetText === 'P-001';
            }
          }, { capture: true, once: true });
          return JSON.stringify({ centerX, centerY });
        })()
        """
    }

    private static let selectGridPolicyContextScript = """
    (() => {
      const marker = window.VaadinDesktopFlowMenuPopup || {};
      const menu = document.querySelector(
        '#framework-flow-menu-popup-grid-policy-context-menu');
      if (!menu || menu.opened !== true) return false;
      marker.gridPolicySelectionDispatched = true;
      return true;
    })()
    """

    private static let inspectionScript = """
    (() => {
      const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
      const marker = window.VaadinDesktopFlowMenuPopup || {};
      window.VaadinDesktopFlowMenuPopup = marker;
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
      const activePath = activeDeepElements();
      const activeWithin = (rootNode) =>
        !!rootNode && activePath.some((element) =>
          element === rootNode
            || rootNode.contains?.(element)
            || rootNode.shadowRoot?.contains?.(element));

      const menu =
        document.querySelector('#framework-flow-menu-popup-menubar');
      const menuOwner = Array.from(
        menu?.querySelectorAll('vaadin-menu-bar-button') || [])
        .find((element) =>
          String(element.textContent || '').includes('Operations'));
      const menuOverlay = menu?._subMenu?._overlayElement
        || findDeep((element) =>
          element.localName === 'vaadin-menu-bar-overlay'
            && collectDeepText(element).includes('Approve')
            && collectDeepText(element).includes('Escalate'));
      const menuList = menu?._subMenu?._listBox
        || findDeep((element) =>
          element.localName === 'vaadin-menu-bar-list-box'
            && collectDeepText(element).includes('Approve')
            && collectDeepText(element).includes('Escalate'));
      const menuItems = Array.from(
        menuList?.items || menuList?.children || []);
      const menuApprove = menuItems.find((element) =>
        element.id === 'framework-flow-menu-popup-menubar-approve'
          || collectDeepText(element).includes('Approve'))
        || findDeep((element) =>
          element.id === 'framework-flow-menu-popup-menubar-approve');
      const menuEscalate = menuItems.find((element) =>
        element.id === 'framework-flow-menu-popup-menubar-escalate'
          || collectDeepText(element).includes('Escalate'))
        || findDeep((element) =>
          element.id === 'framework-flow-menu-popup-menubar-escalate');
      const menuFocused = menuList?.focused
        || [menuApprove, menuEscalate].find((element) =>
          element?.hasAttribute?.('focused'));
      const menuOpenedCurrent =
        menu?._subMenu?.opened === true
          && menuOwner?.getAttribute?.('aria-expanded') === 'true'
          && (menuOverlay?.opened === true
            || menuOverlay?.hasAttribute?.('opened') === true);
      const menuA11yCurrent =
        menuOpenedCurrent
          && menu?.getAttribute?.('role') === 'menubar'
          && menuOwner?.getAttribute?.('role') === 'menuitem'
          && menuOwner?.getAttribute?.('aria-haspopup') === 'true'
          && menuList?.getAttribute?.('role') === 'menu'
          && menuApprove?.getAttribute?.('role') === 'menuitem'
          && menuEscalate?.getAttribute?.('role') === 'menuitem';
      const menuFocusedText = collectDeepText(menuFocused);
      const menuOverlayRect = menuOverlay?.getBoundingClientRect?.();
      const menuOverlayStyle = menuOverlay
        ? getComputedStyle(menuOverlay)
        : null;
      const menuFirstItem = menuItems[0];
      const menuOwnedOverlayDiagnostics = [
        `documentFocus=${document.hasFocus()}`,
        `overlayConnected=${menuOverlay?.isConnected === true}`,
        `overlayOpening=${menuOverlay?.hasAttribute?.('opening') === true}`,
        `overlayHidden=${menuOverlay?.hasAttribute?.('hidden') === true}`,
        `overlayInert=${menuOverlay?.inert === true}`,
        `overlayDisplay=${String(menuOverlayStyle?.display || '')}`,
        `overlayVisibility=${String(menuOverlayStyle?.visibility || '')}`,
        `overlayWidth=${Number(menuOverlayRect?.width || 0)}`,
        `overlayHeight=${Number(menuOverlayRect?.height || 0)}`,
        `contentFirst=${String(
          menu?._subMenu?._overlayElement?._contentRoot
            ?.firstElementChild?.localName || '')}`,
        `listItems=${menuItems.length}`,
        `listChildren=${Number(menuList?.children?.length || 0)}`,
        `first=${String(menuFirstItem?.id
          || menuFirstItem?.localName || '')}`,
        `firstDisabled=${menuFirstItem?.hasAttribute?.('disabled') === true}`,
        `firstHidden=${menuFirstItem?.hasAttribute?.('hidden') === true}`,
        `firstInert=${menuFirstItem?.inert === true}`,
        `firstTabIndex=${Number(menuFirstItem?.tabIndex ?? -99)}`
      ].join(',');
      const menuStatus =
        document.querySelector('#framework-flow-menu-popup-menubar-status')
          ?.textContent || '';

      marker.menuOpenedObserved =
        marker.menuOpenedObserved === true
          || (marker.menuOpenDispatched === true && menuOpenedCurrent);
      marker.menuA11yObserved =
        marker.menuA11yObserved === true || menuA11yCurrent;
      marker.menuOwnerFocusWhileOpenObserved =
        marker.menuOwnerFocusWhileOpenObserved === true
          || (menuOpenedCurrent && activeWithin(menuOwner));
      marker.menuInitialFocusObserved =
        marker.menuInitialFocusObserved === true
          || (marker.menuInitialFocusDispatched === true
            && menuOpenedCurrent
            && (menuFocusedText.includes('Approve')
              || activeWithin(menuList)));
      marker.menuKeyboardMoveObserved =
        marker.menuKeyboardMoveObserved === true
          || (marker.menuMoveDispatched === true
            && menuOpenedCurrent
            && menuFocusedText.includes('Escalate'));
      marker.menuSelectionObserved =
        marker.menuSelectionObserved === true
          || (marker.menuSelectionDispatched === true
            && menuStatus.includes(
              'MenuBar action: escalate client=true change=1'));
      marker.menuJavaListenerObserved =
        marker.menuJavaListenerObserved === true
          || menuStatus.includes(
            'MenuBar action: escalate client=true change=1');
      marker.menuClosedObserved =
        marker.menuClosedObserved === true
          || (marker.menuSelectionDispatched === true
            && !menuOpenedCurrent
            && menuOwner?.getAttribute?.('aria-expanded') === 'false');
      marker.menuFocusRestoredObserved =
        marker.menuFocusRestoredObserved === true
          || ((marker.menuSelectionDispatched === true
              || marker.menuEscapeDispatched === true)
            && !menuOpenedCurrent
            && activeWithin(menuOwner));
      marker.menuEscapeOpenedObserved =
        marker.menuEscapeOpenedObserved === true
          || (marker.menuEscapeOpenDispatched === true
            && menuOpenedCurrent);
      marker.menuEscapeCloseObserved =
        marker.menuEscapeCloseObserved === true
          || (marker.menuEscapeDispatched === true
            && !menuOpenedCurrent
            && menuOwner?.getAttribute?.('aria-expanded') === 'false');

      const nestedParent = menuItems.find((element) =>
        element.id === 'framework-flow-menu-popup-menubar-delivery')
        || findDeep((element) =>
          element.id === 'framework-flow-menu-popup-menubar-delivery');
      const nestedMenu = menu?._subMenu?._subMenu;
      const nestedOverlay = nestedMenu?._overlayElement;
      const nestedList = nestedMenu?._listBox
        || findDeep((element) =>
          element.localName === 'vaadin-menu-bar-list-box'
            && collectDeepText(element).includes('Canary')
            && collectDeepText(element).includes('Production'));
      const nestedItems = Array.from(
        nestedList?.items || nestedList?.children || []);
      const nestedCanary = nestedItems.find((element) =>
        element.id === 'framework-flow-menu-popup-menubar-canary')
        || findDeep((element) =>
          element.id === 'framework-flow-menu-popup-menubar-canary');
      const nestedProduction = nestedItems.find((element) =>
        element.id === 'framework-flow-menu-popup-menubar-production')
        || findDeep((element) =>
          element.id === 'framework-flow-menu-popup-menubar-production');
      const nestedFocused = nestedList?.focused
        || [nestedCanary, nestedProduction].find((element) =>
          element?.hasAttribute?.('focused'));
      const nestedRootFocused = menuList?.focused
        || [menuApprove, menuEscalate, nestedParent].find((element) =>
          element?.hasAttribute?.('focused'));
      const nestedRootFocusedText = collectDeepText(nestedRootFocused);
      const nestedFocusedText = collectDeepText(nestedFocused);
      const nestedOpenedCurrent =
        menuOpenedCurrent
          && nestedMenu?.opened === true
          && nestedMenu?.listenOn === nestedParent
          && (nestedOverlay?.opened === true
            || nestedOverlay?.hasAttribute?.('opened') === true);
      const nestedA11yCurrent =
        nestedOpenedCurrent
          && nestedParent?.getAttribute?.('role') === 'menuitem'
          && nestedParent?.getAttribute?.('aria-haspopup') === 'true'
          && nestedParent?.getAttribute?.('aria-expanded') === 'true'
          && nestedList?.getAttribute?.('role') === 'menu'
          && nestedCanary?.getAttribute?.('role') === 'menuitem'
          && nestedProduction?.getAttribute?.('role') === 'menuitem';
      const nestedParentFocusedCurrent =
        nestedRootFocused === nestedParent
          || nestedRootFocused?.id
            === 'framework-flow-menu-popup-menubar-delivery';
      const nestedCanaryFocusedCurrent =
        nestedFocused === nestedCanary
          || nestedFocused?.id
            === 'framework-flow-menu-popup-menubar-canary';
      const nestedProductionFocusedCurrent =
        nestedFocused === nestedProduction
          || nestedFocused?.id
            === 'framework-flow-menu-popup-menubar-production';
      const nestedStatus =
        document.querySelector(
          '#framework-flow-menu-popup-nested-menubar-status')
          ?.textContent || '';
      const nestedMenuStage = String(marker.nestedMenuStage || '');
      const nestedOverlayRect = nestedOverlay?.getBoundingClientRect?.();
      const nestedDiagnostics = [
        `stage=${nestedMenuStage}`,
        `rootOpened=${menuOpenedCurrent}`,
        `nestedOpened=${nestedOpenedCurrent}`,
        `nestedConnected=${nestedMenu?.isConnected === true}`,
        `nestedOwnsParent=${nestedMenu?.listenOn === nestedParent}`,
        `parentExpanded=${String(
          nestedParent?.getAttribute?.('aria-expanded') || '')}`,
        `rootFocused=${String(
          nestedRootFocused?.id || nestedRootFocused?.localName || '')}`,
        `nestedFocused=${String(
          nestedFocused?.id || nestedFocused?.localName || '')}`,
        `nestedItems=${nestedItems.length}`,
        `nestedWidth=${Number(nestedOverlayRect?.width || 0)}`,
        `nestedHeight=${Number(nestedOverlayRect?.height || 0)}`
      ].join(',');

      marker.nestedRootOpenedObserved =
        marker.nestedRootOpenedObserved === true
          || (nestedMenuStage === 'open-root' && menuOpenedCurrent);
      marker.nestedRootInitialFocusObserved =
        marker.nestedRootInitialFocusObserved === true
          || (nestedMenuStage === 'open-root'
            && menuOpenedCurrent
            && (nestedRootFocused === menuApprove
              || nestedRootFocusedText.includes('Approve')));
      marker.nestedFirstMoveObserved =
        marker.nestedFirstMoveObserved === true
          || (nestedMenuStage === 'move-first'
            && menuOpenedCurrent
            && (nestedRootFocused === menuEscalate
              || nestedRootFocusedText.includes('Escalate')));
      marker.nestedParentFocusObserved =
        marker.nestedParentFocusObserved === true
          || (nestedMenuStage === 'focus-parent'
            && menuOpenedCurrent
            && nestedParentFocusedCurrent
            && nestedParent?.getAttribute?.('aria-haspopup') === 'true');
      marker.nestedChildOpenedObserved =
        marker.nestedChildOpenedObserved === true
          || (nestedMenuStage === 'open-child'
            && nestedOpenedCurrent);
      marker.nestedA11yObserved =
        marker.nestedA11yObserved === true || nestedA11yCurrent;
      marker.nestedInitialFocusObserved =
        marker.nestedInitialFocusObserved === true
          || (nestedMenuStage === 'open-child'
            && nestedOpenedCurrent
            && nestedCanaryFocusedCurrent);
      marker.nestedKeyboardMoveObserved =
        marker.nestedKeyboardMoveObserved === true
          || (nestedMenuStage === 'move-child'
            && nestedOpenedCurrent
            && nestedProductionFocusedCurrent);
      marker.nestedLeftReturnObserved =
        marker.nestedLeftReturnObserved === true
          || (nestedMenuStage === 'return-parent'
            && !nestedOpenedCurrent
            && nestedParent?.getAttribute?.('aria-expanded') === 'false');
      marker.nestedParentRemainsOpenObserved =
        marker.nestedParentRemainsOpenObserved === true
          || (nestedMenuStage === 'return-parent'
            && menuOpenedCurrent
            && !nestedOpenedCurrent);
      marker.nestedParentFocusRestoredObserved =
        marker.nestedParentFocusRestoredObserved === true
          || (nestedMenuStage === 'return-parent'
            && menuOpenedCurrent
            && !nestedOpenedCurrent
            && nestedParentFocusedCurrent);
      marker.nestedReopenedObserved =
        marker.nestedReopenedObserved === true
          || (nestedMenuStage === 'reopen-child'
            && nestedOpenedCurrent);
      marker.nestedReopenedInitialFocusObserved =
        marker.nestedReopenedInitialFocusObserved === true
          || (nestedMenuStage === 'reopen-child'
            && nestedOpenedCurrent
            && nestedCanaryFocusedCurrent);
      marker.nestedSelectionMoveObserved =
        marker.nestedSelectionMoveObserved === true
          || (nestedMenuStage === 'move-child-for-selection'
            && nestedOpenedCurrent
            && nestedProductionFocusedCurrent);
      marker.nestedSelectionObserved =
        marker.nestedSelectionObserved === true
          || (nestedMenuStage === 'select-leaf'
            && nestedStatus.includes(
              'nested MenuBar action: production client=true change=1'));
      marker.nestedJavaListenerObserved =
        marker.nestedJavaListenerObserved === true
          || nestedStatus.includes(
            'nested MenuBar action: production client=true change=1');
      marker.nestedClosedObserved =
        marker.nestedClosedObserved === true
          || (nestedMenuStage === 'select-leaf'
            && !menuOpenedCurrent
            && !nestedOpenedCurrent);
      marker.nestedFocusRestoredObserved =
        marker.nestedFocusRestoredObserved === true
          || (nestedMenuStage === 'select-leaf'
            && !menuOpenedCurrent
            && !nestedOpenedCurrent
            && activeWithin(menuOwner));
      marker.nestedEscapeRootOpenedObserved =
        marker.nestedEscapeRootOpenedObserved === true
          || (nestedMenuStage === 'escape-open-root'
            && menuOpenedCurrent);
      marker.nestedEscapeFirstMoveObserved =
        marker.nestedEscapeFirstMoveObserved === true
          || (nestedMenuStage === 'escape-move-first'
            && menuOpenedCurrent
            && (nestedRootFocused === menuEscalate
              || nestedRootFocusedText.includes('Escalate')));
      marker.nestedEscapeParentFocusObserved =
        marker.nestedEscapeParentFocusObserved === true
          || (nestedMenuStage === 'escape-focus-parent'
            && menuOpenedCurrent
            && nestedParentFocusedCurrent);
      marker.nestedEscapeChildOpenedObserved =
        marker.nestedEscapeChildOpenedObserved === true
          || (nestedMenuStage === 'escape-open-child'
            && nestedOpenedCurrent
            && nestedCanaryFocusedCurrent);
      marker.nestedEscapeChildCloseObserved =
        marker.nestedEscapeChildCloseObserved === true
          || (nestedMenuStage === 'escape-close-child'
            && !nestedOpenedCurrent
            && nestedParent?.getAttribute?.('aria-expanded') === 'false');
      marker.nestedEscapeParentRemainsOpenObserved =
        marker.nestedEscapeParentRemainsOpenObserved === true
          || (nestedMenuStage === 'escape-close-child'
            && menuOpenedCurrent
            && !nestedOpenedCurrent);
      marker.nestedEscapeParentFocusRestoredObserved =
        marker.nestedEscapeParentFocusRestoredObserved === true
          || (nestedMenuStage === 'escape-close-child'
            && menuOpenedCurrent
            && !nestedOpenedCurrent
            && nestedParentFocusedCurrent);
      marker.nestedEscapeRootCloseObserved =
        marker.nestedEscapeRootCloseObserved === true
          || (nestedMenuStage === 'escape-close-root'
            && !menuOpenedCurrent
            && !nestedOpenedCurrent);
      marker.nestedEscapeOwnerFocusRestoredObserved =
        marker.nestedEscapeOwnerFocusRestoredObserved === true
          || (nestedMenuStage === 'escape-close-root'
            && !menuOpenedCurrent
            && !nestedOpenedCurrent
            && activeWithin(menuOwner));

      const overflowMenu = document.querySelector(
        '#framework-flow-menu-popup-overflow-menubar');
      const overflowButton = overflowMenu?._overflow
        || overflowMenu?.querySelector(
          'vaadin-menu-bar-button[slot="overflow"]');
      const overflowRootButtons = Array.from(
        overflowMenu?.querySelectorAll('vaadin-menu-bar-button') || [])
        .filter((button) => button !== overflowButton);
      const overflowHiddenButtons = overflowRootButtons.filter((button) =>
        button.style.visibility === 'hidden');
      const overflowChildren = Array.from(
        overflowButton?.item?.children || []);
      const overflowExpectedItemId = String(
        marker.overflowExpectedItemId
          || overflowChildren[0]?.component?.id
          || '');
      const overflowExpectedAction = String(
        marker.overflowExpectedAction
          || overflowExpectedItemId.replace(
            'framework-flow-menu-popup-overflow-', ''));
      const overflowExpectedComponent = overflowExpectedItemId
        ? document.getElementById(overflowExpectedItemId)
          || findDeep((element) =>
            element.id === overflowExpectedItemId)
        : null;
      const overflowOverlay = overflowMenu?._subMenu?._overlayElement;
      const overflowList = overflowMenu?._subMenu?._listBox;
      const overflowItems = Array.from(
        overflowList?.items || overflowList?.children || []);
      const overflowExpectedItem = overflowItems.find((element) =>
        element.id === overflowExpectedItemId)
        || overflowExpectedComponent;
      const overflowFocused = overflowList?.focused
        || overflowItems.find((element) =>
          element?.hasAttribute?.('focused'));
      const overflowOpenedCurrent =
        overflowMenu?._subMenu?.opened === true
          && overflowMenu?._subMenu?.listenOn === overflowButton
          && overflowButton?.getAttribute?.('aria-expanded') === 'true'
          && (overflowOverlay?.opened === true
            || overflowOverlay?.hasAttribute?.('opened') === true);
      const overflowStage = String(marker.overflowStage || '');
      const overflowStatus =
        document.querySelector(
          '#framework-flow-menu-popup-overflow-menubar-status')
          ?.textContent || '';
      const overflowMenuRect = overflowMenu?.getBoundingClientRect?.();
      const overflowExpectedRootButton =
        overflowExpectedComponent?.closest?.('vaadin-menu-bar-button');
      const overflowFocusedText = collectDeepText(overflowFocused);
      const overflowDiagnostics = [
        `stage=${overflowStage}`,
        `menuWidth=${Number(overflowMenuRect?.width || 0)}`,
        `opened=${overflowOpenedCurrent}`,
        `overflowHidden=${overflowButton?.hasAttribute?.('hidden') === true}`,
        `collapsedItems=${overflowChildren.length}`,
        `hiddenButtons=${overflowHiddenButtons.length}`,
        `expectedItem=${overflowExpectedItemId}`,
        `focused=${String(
          overflowFocused?.id || overflowFocused?.localName || '')}`,
        `restoredParent=${String(
          overflowExpectedRootButton?.localName || '')}`
      ].join(',');

      marker.overflowNarrowObserved =
        marker.overflowNarrowObserved === true
          || (Number(overflowMenuRect?.width || 0) > 0
            && Number(overflowMenuRect?.width || 0) <= 300
            && overflowMenu?.getAttribute?.('role') === 'menubar'
            && overflowButton?.hasAttribute?.('hidden') === false
            && overflowChildren.length > 0
            && overflowHiddenButtons.length === overflowChildren.length);
      marker.overflowOpenedObserved =
        marker.overflowOpenedObserved === true
          || (overflowStage === 'open'
            && overflowOpenedCurrent);
      marker.overflowA11yObserved =
        marker.overflowA11yObserved === true
          || (overflowOpenedCurrent
            && overflowButton?.getAttribute?.('role') === 'menuitem'
            && overflowButton?.getAttribute?.('aria-haspopup') === 'true'
            && overflowButton?.getAttribute?.('aria-label')
              === 'More options'
            && overflowList?.getAttribute?.('role') === 'menu'
            && overflowExpectedItem?.getAttribute?.('role') === 'menuitem');
      marker.overflowInitialFocusObserved =
        marker.overflowInitialFocusObserved === true
          || (overflowStage === 'open'
            && overflowOpenedCurrent
            && overflowFocused?.id === overflowExpectedItemId);
      marker.overflowSelectionObserved =
        marker.overflowSelectionObserved === true
          || (overflowStage === 'select'
            && !!overflowExpectedAction
            && overflowStatus.includes(
              `overflow MenuBar action: ${overflowExpectedAction}`
                + ' client=true change=1'));
      marker.overflowJavaListenerObserved =
        marker.overflowJavaListenerObserved === true
          || (!!overflowExpectedAction
            && overflowStatus.includes(
              `overflow MenuBar action: ${overflowExpectedAction}`
                + ' client=true change=1'));
      marker.overflowClosedObserved =
        marker.overflowClosedObserved === true
          || (overflowStage === 'select'
            && !overflowOpenedCurrent
            && overflowButton?.getAttribute?.('aria-expanded') === 'false');
      marker.overflowFocusRestoredObserved =
        marker.overflowFocusRestoredObserved === true
          || (overflowStage === 'select'
            && !overflowOpenedCurrent
            && activeWithin(overflowButton));
      marker.overflowEscapeOpenedObserved =
        marker.overflowEscapeOpenedObserved === true
          || (overflowStage === 'escape-open'
            && overflowOpenedCurrent);
      marker.overflowEscapeInitialFocusObserved =
        marker.overflowEscapeInitialFocusObserved === true
          || (overflowStage === 'escape-open'
            && overflowOpenedCurrent
            && overflowFocused?.id === overflowExpectedItemId);
      marker.overflowEscapeCloseObserved =
        marker.overflowEscapeCloseObserved === true
          || (overflowStage === 'escape-close'
            && !overflowOpenedCurrent
            && overflowButton?.getAttribute?.('aria-expanded') === 'false');
      marker.overflowEscapeFocusRestoredObserved =
        marker.overflowEscapeFocusRestoredObserved === true
          || (overflowStage === 'escape-close'
            && !overflowOpenedCurrent
            && activeWithin(overflowButton));

      const overflowNestedParentId =
        'framework-flow-menu-popup-overflow-observability';
      const overflowNestedParentComponent =
        document.getElementById(overflowNestedParentId)
          || findDeep((element) =>
            element.id === overflowNestedParentId);
      const overflowNestedParent = overflowItems.find((element) =>
        element.id === overflowNestedParentId)
          || overflowNestedParentComponent;
      const overflowNestedParentRootButton =
        overflowRootButtons.find((button) =>
          button.item?.component?.id === overflowNestedParentId);
      const overflowNestedCollapsedParentCurrent =
        overflowChildren.some((item) =>
          item?.component?.id === overflowNestedParentId)
          && overflowNestedParentRootButton?.style.visibility === 'hidden';
      const overflowNestedMenu = overflowMenu?._subMenu?._subMenu;
      const overflowNestedOverlay =
        overflowNestedMenu?._overlayElement;
      const overflowNestedList = overflowNestedMenu?._listBox
        || findDeep((element) =>
          element.localName === 'vaadin-menu-bar-list-box'
            && collectDeepText(element).includes('Metrics')
            && collectDeepText(element).includes('Traces'));
      const overflowNestedItems = Array.from(
        overflowNestedList?.items
          || overflowNestedList?.children
          || []);
      const overflowNestedMetrics = overflowNestedItems.find((element) =>
        element.id
          === 'framework-flow-menu-popup-overflow-observability-metrics')
        || findDeep((element) =>
          element.id
            === 'framework-flow-menu-popup-overflow-observability-metrics');
      const overflowNestedTraces = overflowNestedItems.find((element) =>
        element.id
          === 'framework-flow-menu-popup-overflow-observability-traces')
        || findDeep((element) =>
          element.id
            === 'framework-flow-menu-popup-overflow-observability-traces');
      const overflowNestedRootFocused = overflowList?.focused
        || overflowItems.find((element) =>
          element?.hasAttribute?.('focused'));
      const overflowNestedFocused = overflowNestedList?.focused
        || overflowNestedItems.find((element) =>
          element?.hasAttribute?.('focused'));
      const overflowNestedOpenedCurrent =
        overflowOpenedCurrent
          && overflowNestedMenu?.opened === true
          && overflowNestedMenu?.listenOn === overflowNestedParent
          && (overflowNestedOverlay?.opened === true
            || overflowNestedOverlay?.hasAttribute?.('opened') === true);
      const overflowNestedParentFocusedCurrent =
        overflowNestedRootFocused === overflowNestedParent
          || overflowNestedRootFocused?.id === overflowNestedParentId;
      const overflowNestedMetricsFocusedCurrent =
        overflowNestedFocused === overflowNestedMetrics
          || overflowNestedFocused?.id
            === 'framework-flow-menu-popup-overflow-observability-metrics';
      const overflowNestedTracesFocusedCurrent =
        overflowNestedFocused === overflowNestedTraces
          || overflowNestedFocused?.id
            === 'framework-flow-menu-popup-overflow-observability-traces';
      const overflowNestedA11yCurrent =
        overflowNestedOpenedCurrent
          && overflowNestedCollapsedParentCurrent
          && overflowNestedParent?.getAttribute?.('role') === 'menuitem'
          && overflowNestedParent?.getAttribute?.('aria-haspopup') === 'true'
          && overflowNestedParent?.getAttribute?.('aria-expanded') === 'true'
          && overflowNestedList?.getAttribute?.('role') === 'menu'
          && overflowNestedMetrics?.getAttribute?.('role') === 'menuitem'
          && overflowNestedTraces?.getAttribute?.('role') === 'menuitem';
      const overflowNestedStage =
        String(marker.overflowNestedStage || '');
      const overflowNestedStatus =
        document.querySelector(
          '#framework-flow-menu-popup-overflow-nested-status')
          ?.textContent || '';
      const overflowNestedRootFocusedText =
        collectDeepText(overflowNestedRootFocused);
      const overflowNestedFocusedText =
        collectDeepText(overflowNestedFocused);
      const overflowNestedOverlayRect =
        overflowNestedOverlay?.getBoundingClientRect?.();
      const overflowNestedDiagnostics = [
        `stage=${overflowNestedStage}`,
        `rootOpened=${overflowOpenedCurrent}`,
        `childOpened=${overflowNestedOpenedCurrent}`,
        `collapsedParent=${overflowNestedCollapsedParentCurrent}`,
        `childOwnsParent=${
          overflowNestedMenu?.listenOn === overflowNestedParent}`,
        `parentExpanded=${String(
          overflowNestedParent?.getAttribute?.('aria-expanded') || '')}`,
        `rootFocused=${String(
          overflowNestedRootFocused?.id
            || overflowNestedRootFocused?.localName
            || '')}`,
        `childFocused=${String(
          overflowNestedFocused?.id
            || overflowNestedFocused?.localName
            || '')}`,
        `childItems=${overflowNestedItems.length}`,
        `childWidth=${Number(overflowNestedOverlayRect?.width || 0)}`,
        `childHeight=${Number(overflowNestedOverlayRect?.height || 0)}`
      ].join(',');

      marker.overflowNestedCollapsedParentObserved =
        marker.overflowNestedCollapsedParentObserved === true
          || (overflowNestedStage === 'open-root'
            && overflowNestedCollapsedParentCurrent
            && overflowNestedParent?.getAttribute?.('role') === 'menuitem');
      marker.overflowNestedRootOpenedObserved =
        marker.overflowNestedRootOpenedObserved === true
          || (overflowNestedStage === 'open-root'
            && overflowOpenedCurrent
            && overflowMenu?._subMenu?.listenOn === overflowButton);
      marker.overflowNestedRootInitialFocusObserved =
        marker.overflowNestedRootInitialFocusObserved === true
          || (overflowNestedStage === 'open-root'
            && overflowOpenedCurrent
            && overflowNestedRootFocused?.id === overflowExpectedItemId);
      marker.overflowNestedParentFocusObserved =
        marker.overflowNestedParentFocusObserved === true
          || (overflowNestedStage === 'focus-parent'
            && overflowOpenedCurrent
            && overflowNestedParentFocusedCurrent
            && overflowNestedParent?.getAttribute?.('aria-haspopup')
              === 'true');
      marker.overflowNestedChildOpenedObserved =
        marker.overflowNestedChildOpenedObserved === true
          || (overflowNestedStage === 'open-child'
            && overflowNestedOpenedCurrent);
      marker.overflowNestedA11yObserved =
        marker.overflowNestedA11yObserved === true
          || overflowNestedA11yCurrent;
      marker.overflowNestedInitialFocusObserved =
        marker.overflowNestedInitialFocusObserved === true
          || (overflowNestedStage === 'open-child'
            && overflowNestedOpenedCurrent
            && overflowNestedMetricsFocusedCurrent);
      marker.overflowNestedKeyboardMoveObserved =
        marker.overflowNestedKeyboardMoveObserved === true
          || (overflowNestedStage === 'move-child'
            && overflowNestedOpenedCurrent
            && overflowNestedTracesFocusedCurrent);
      marker.overflowNestedLeftReturnObserved =
        marker.overflowNestedLeftReturnObserved === true
          || (overflowNestedStage === 'return-parent'
            && !overflowNestedOpenedCurrent
            && overflowNestedParent?.getAttribute?.('aria-expanded')
              === 'false');
      marker.overflowNestedParentRemainsOpenObserved =
        marker.overflowNestedParentRemainsOpenObserved === true
          || (overflowNestedStage === 'return-parent'
            && overflowOpenedCurrent
            && !overflowNestedOpenedCurrent);
      marker.overflowNestedParentFocusRestoredObserved =
        marker.overflowNestedParentFocusRestoredObserved === true
          || (overflowNestedStage === 'return-parent'
            && overflowOpenedCurrent
            && !overflowNestedOpenedCurrent
            && overflowNestedParentFocusedCurrent);
      marker.overflowNestedReopenedObserved =
        marker.overflowNestedReopenedObserved === true
          || (overflowNestedStage === 'reopen-child'
            && overflowNestedOpenedCurrent);
      marker.overflowNestedReopenedInitialFocusObserved =
        marker.overflowNestedReopenedInitialFocusObserved === true
          || (overflowNestedStage === 'reopen-child'
            && overflowNestedOpenedCurrent
            && overflowNestedMetricsFocusedCurrent);
      marker.overflowNestedSelectionMoveObserved =
        marker.overflowNestedSelectionMoveObserved === true
          || (overflowNestedStage === 'move-child-for-selection'
            && overflowNestedOpenedCurrent
            && overflowNestedTracesFocusedCurrent);
      marker.overflowNestedSelectionObserved =
        marker.overflowNestedSelectionObserved === true
          || (overflowNestedStage === 'select-leaf'
            && overflowNestedStatus.includes(
              'overflow nested MenuBar action: traces'
                + ' client=true change=1'));
      marker.overflowNestedJavaListenerObserved =
        marker.overflowNestedJavaListenerObserved === true
          || overflowNestedStatus.includes(
            'overflow nested MenuBar action: traces'
              + ' client=true change=1');
      marker.overflowNestedClosedObserved =
        marker.overflowNestedClosedObserved === true
          || (overflowNestedStage === 'select-leaf'
            && !overflowOpenedCurrent
            && !overflowNestedOpenedCurrent
            && overflowButton?.getAttribute?.('aria-expanded') === 'false');
      marker.overflowNestedFocusRestoredObserved =
        marker.overflowNestedFocusRestoredObserved === true
          || (overflowNestedStage === 'select-leaf'
            && !overflowOpenedCurrent
            && !overflowNestedOpenedCurrent
            && activeWithin(overflowButton));
      marker.overflowNestedEscapeRootOpenedObserved =
        marker.overflowNestedEscapeRootOpenedObserved === true
          || (overflowNestedStage === 'escape-open-root'
            && overflowOpenedCurrent);
      marker.overflowNestedEscapeRootInitialFocusObserved =
        marker.overflowNestedEscapeRootInitialFocusObserved === true
          || (overflowNestedStage === 'escape-open-root'
            && overflowOpenedCurrent
            && overflowNestedRootFocused?.id === overflowExpectedItemId);
      marker.overflowNestedEscapeParentFocusObserved =
        marker.overflowNestedEscapeParentFocusObserved === true
          || (overflowNestedStage === 'escape-focus-parent'
            && overflowOpenedCurrent
            && overflowNestedParentFocusedCurrent);
      marker.overflowNestedEscapeChildOpenedObserved =
        marker.overflowNestedEscapeChildOpenedObserved === true
          || (overflowNestedStage === 'escape-open-child'
            && overflowNestedOpenedCurrent
            && overflowNestedMetricsFocusedCurrent);
      marker.overflowNestedEscapeChildCloseObserved =
        marker.overflowNestedEscapeChildCloseObserved === true
          || (overflowNestedStage === 'escape-close-child'
            && !overflowNestedOpenedCurrent
            && overflowNestedParent?.getAttribute?.('aria-expanded')
              === 'false');
      marker.overflowNestedEscapeParentRemainsOpenObserved =
        marker.overflowNestedEscapeParentRemainsOpenObserved === true
          || (overflowNestedStage === 'escape-close-child'
            && overflowOpenedCurrent
            && !overflowNestedOpenedCurrent);
      marker.overflowNestedEscapeParentFocusRestoredObserved =
        marker.overflowNestedEscapeParentFocusRestoredObserved === true
          || (overflowNestedStage === 'escape-close-child'
            && overflowOpenedCurrent
            && !overflowNestedOpenedCurrent
            && overflowNestedParentFocusedCurrent);
      marker.overflowNestedEscapeRootCloseObserved =
        marker.overflowNestedEscapeRootCloseObserved === true
          || (overflowNestedStage === 'escape-close-root'
            && !overflowOpenedCurrent
            && !overflowNestedOpenedCurrent);
      marker.overflowNestedEscapeOwnerFocusRestoredObserved =
        marker.overflowNestedEscapeOwnerFocusRestoredObserved === true
          || (overflowNestedStage === 'escape-close-root'
            && !overflowOpenedCurrent
            && !overflowNestedOpenedCurrent
            && activeWithin(overflowButton));
      marker.overflowExpandedObserved =
        marker.overflowExpandedObserved === true
          || (overflowStage === 'expand'
            && marker.overflowExpandDispatched === true
            && Number(overflowMenuRect?.width || 0) >= 800
            && overflowButton?.hasAttribute?.('hidden') === true
            && overflowChildren.length === 0
            && overflowHiddenButtons.length === 0);
      marker.overflowItemsRestoredObserved =
        marker.overflowItemsRestoredObserved === true
          || (overflowStage === 'expand'
            && overflowExpectedRootButton?.isConnected === true
            && overflowExpectedRootButton.style.visibility !== 'hidden'
            && overflowExpectedComponent?.getAttribute?.('role') !== 'menuitem'
            && overflowRootButtons.every((button) =>
              button.style.visibility !== 'hidden'));

      const contextMenu =
        document.querySelector('#framework-flow-menu-popup-context-menu')
          || findDeep((element) =>
            element.id === 'framework-flow-menu-popup-context-menu'
              || element.localName === 'vaadin-context-menu');
      const contextTarget =
        document.querySelector('#framework-flow-menu-popup-context-target');
      const contextOverlay = findDeep((element) =>
        element.localName === 'vaadin-context-menu-overlay'
          && collectDeepText(element).includes('Inspect')
          && collectDeepText(element).includes('Archive'));
      const contextList = findDeep((element) =>
        element.localName === 'vaadin-context-menu-list-box'
          && collectDeepText(element).includes('Inspect')
          && collectDeepText(element).includes('Archive'));
      const contextItems = Array.from(
        contextList?.items || contextList?.children || []);
      const contextInspect = contextItems.find((element) =>
        element.id === 'framework-flow-menu-popup-context-inspect')
        || findDeep((element) =>
          element.id === 'framework-flow-menu-popup-context-inspect');
      const contextArchive = contextItems.find((element) =>
        element.id === 'framework-flow-menu-popup-context-archive')
        || findDeep((element) =>
          element.id === 'framework-flow-menu-popup-context-archive');
      const contextShare = contextItems.find((element) =>
        element.id === 'framework-flow-menu-popup-context-share')
        || findDeep((element) =>
          element.id === 'framework-flow-menu-popup-context-share');
      const contextFocused = contextList?.focused
        || [contextInspect, contextArchive, contextShare].find((element) =>
          element?.hasAttribute?.('focused'));
      const contextOpenedCurrent =
        contextMenu?.opened === true
          && (contextOverlay?.opened === true
            || contextOverlay?.hasAttribute?.('opened') === true);
      const contextA11yCurrent =
        contextOpenedCurrent
          && contextList?.getAttribute?.('role') === 'menu'
          && contextInspect?.getAttribute?.('role') === 'menuitem'
          && contextArchive?.getAttribute?.('role') === 'menuitem';
      const contextFocusedText = collectDeepText(contextFocused);
      const contextStatus =
        document.querySelector('#framework-flow-menu-popup-context-status')
          ?.textContent || '';
      const contextLifecycleStatus =
        document.querySelector(
          '#framework-flow-menu-popup-context-lifecycle')
          ?.textContent || '';
      const contextNestedMenu = contextMenu?._subMenu;
      const contextNestedOverlay = contextNestedMenu?._overlayElement;
      const contextNestedList = contextNestedMenu?._listBox
        || findDeep((element) =>
          element.localName === 'vaadin-context-menu-list-box'
            && collectDeepText(element).includes('Copy link')
            && collectDeepText(element).includes('Export report'));
      const contextNestedItems = Array.from(
        contextNestedList?.items
          || contextNestedList?.children
          || []);
      const contextNestedCopyLink = contextNestedItems.find((element) =>
        element.id
          === 'framework-flow-menu-popup-context-share-copy-link')
        || findDeep((element) =>
          element.id
            === 'framework-flow-menu-popup-context-share-copy-link');
      const contextNestedExportReport =
        contextNestedItems.find((element) =>
          element.id
            === 'framework-flow-menu-popup-context-share-export-report')
        || findDeep((element) =>
          element.id
            === 'framework-flow-menu-popup-context-share-export-report');
      const contextNestedRootFocused = contextList?.focused
        || contextItems.find((element) =>
          element?.hasAttribute?.('focused'));
      const contextNestedFocused = contextNestedList?.focused
        || contextNestedItems.find((element) =>
          element?.hasAttribute?.('focused'));
      const contextNestedOpenedCurrent =
        contextOpenedCurrent
          && contextNestedMenu?.opened === true
          && contextNestedMenu?.listenOn === contextShare
          && (contextNestedOverlay?.opened === true
            || contextNestedOverlay?.hasAttribute?.('opened') === true);
      const contextNestedParentFocusedCurrent =
        contextNestedRootFocused === contextShare
          || contextNestedRootFocused?.id
            === 'framework-flow-menu-popup-context-share';
      const contextNestedCopyLinkFocusedCurrent =
        contextNestedFocused === contextNestedCopyLink
          || contextNestedFocused?.id
            === 'framework-flow-menu-popup-context-share-copy-link';
      const contextNestedExportReportFocusedCurrent =
        contextNestedFocused === contextNestedExportReport
          || contextNestedFocused?.id
            === 'framework-flow-menu-popup-context-share-export-report';
      const contextNestedA11yCurrent =
        contextNestedOpenedCurrent
          && contextShare?.getAttribute?.('role') === 'menuitem'
          && contextShare?.getAttribute?.('aria-haspopup') === 'true'
          && contextShare?.getAttribute?.('aria-expanded') === 'true'
          && contextNestedList?.getAttribute?.('role') === 'menu'
          && contextNestedCopyLink?.getAttribute?.('role') === 'menuitem'
          && contextNestedExportReport?.getAttribute?.('role')
            === 'menuitem'
          && contextNestedOverlay?.parentOverlay === contextOverlay;
      const contextNestedStage =
        String(marker.contextNestedStage || '');
      const contextNestedStatus =
        document.querySelector(
          '#framework-flow-menu-popup-context-nested-status')
          ?.textContent || '';
      const contextNestedRootFocusedText =
        collectDeepText(contextNestedRootFocused);
      const contextNestedFocusedText =
        collectDeepText(contextNestedFocused);
      const contextNestedOverlayRect =
        contextNestedOverlay?.getBoundingClientRect?.();
      const contextNestedDiagnostics = [
        `stage=${contextNestedStage}`,
        `rootOpened=${contextOpenedCurrent}`,
        `childOpened=${contextNestedOpenedCurrent}`,
        `childOwnsParent=${
          contextNestedMenu?.listenOn === contextShare}`,
        `childParentOverlay=${
          contextNestedOverlay?.parentOverlay === contextOverlay}`,
        `parentExpanded=${String(
          contextShare?.getAttribute?.('aria-expanded') || '')}`,
        `rootFocused=${String(
          contextNestedRootFocused?.id
            || contextNestedRootFocused?.localName
            || '')}`,
        `childFocused=${String(
          contextNestedFocused?.id
            || contextNestedFocused?.localName
            || '')}`,
        `childItems=${contextNestedItems.length}`,
        `childWidth=${Number(contextNestedOverlayRect?.width || 0)}`,
        `childHeight=${Number(contextNestedOverlayRect?.height || 0)}`,
        `lifecycle=${contextLifecycleStatus.replace(/\\s+/g, ' ').trim()}`
      ].join(',');

      marker.contextOpenedObserved =
        marker.contextOpenedObserved === true
          || (Number(marker.contextOpenDispatchCount || 0) >= 1
            && contextOpenedCurrent);
      marker.contextA11yObserved =
        marker.contextA11yObserved === true || contextA11yCurrent;
      marker.contextInitialFocusObserved =
        marker.contextInitialFocusObserved === true
          || (contextOpenedCurrent
            && (contextFocusedText.includes('Inspect')
              || activeWithin(contextList)));
      marker.contextKeyboardMoveObserved =
        marker.contextKeyboardMoveObserved === true
          || (marker.contextMoveDispatched === true
            && contextOpenedCurrent
            && contextFocusedText.includes('Archive'));
      marker.contextSelectionObserved =
        marker.contextSelectionObserved === true
          || (marker.contextSelectionDispatched === true
            && contextStatus.includes(
              'ContextMenu action: archive client=true change=1'));
      marker.contextJavaListenerObserved =
        marker.contextJavaListenerObserved === true
          || contextStatus.includes(
            'ContextMenu action: archive client=true change=1');
      marker.contextClosedObserved =
        marker.contextClosedObserved === true
          || (marker.contextSelectionDispatched === true
            && !contextOpenedCurrent);
      marker.contextFocusRestoredObserved =
        marker.contextFocusRestoredObserved === true
          || ((marker.contextSelectionDispatched === true
              || marker.contextEscapeDispatched === true)
            && !contextOpenedCurrent
            && activeWithin(contextTarget));
      marker.contextEscapeOpenedObserved =
        marker.contextEscapeOpenedObserved === true
          || (Number(marker.contextOpenDispatchCount || 0) >= 2
            && contextOpenedCurrent);
      marker.contextEscapeCloseObserved =
        marker.contextEscapeCloseObserved === true
          || (marker.contextEscapeDispatched === true
            && !contextOpenedCurrent);
      marker.contextJavaOpenObserved =
        marker.contextJavaOpenObserved === true
          || contextLifecycleStatus.includes(
            'ContextMenu lifecycle: opened client=true');
      marker.contextJavaCloseObserved =
        marker.contextJavaCloseObserved === true
          || contextLifecycleStatus.includes(
            'ContextMenu lifecycle: closed client=true');
      marker.contextNestedRootOpenedObserved =
        marker.contextNestedRootOpenedObserved === true
          || (contextNestedStage === 'open-root'
            && contextOpenedCurrent);
      marker.contextNestedRootInitialFocusObserved =
        marker.contextNestedRootInitialFocusObserved === true
          || (contextNestedStage === 'open-root'
            && contextOpenedCurrent
            && (contextNestedRootFocused === contextInspect
              || contextNestedRootFocusedText.includes('Inspect')));
      marker.contextNestedJavaRootOpenObserved =
        marker.contextNestedJavaRootOpenObserved === true
          || (contextNestedStage === 'open-root'
            && contextLifecycleStatus.includes(
              'ContextMenu lifecycle: opened client=true'));
      marker.contextNestedFirstMoveObserved =
        marker.contextNestedFirstMoveObserved === true
          || (contextNestedStage === 'move-first'
            && contextOpenedCurrent
            && (contextNestedRootFocused === contextArchive
              || contextNestedRootFocusedText.includes('Archive')));
      marker.contextNestedParentFocusObserved =
        marker.contextNestedParentFocusObserved === true
          || (contextNestedStage === 'focus-parent'
            && contextOpenedCurrent
            && contextNestedParentFocusedCurrent
            && contextShare?.getAttribute?.('aria-haspopup') === 'true');
      marker.contextNestedChildOpenedObserved =
        marker.contextNestedChildOpenedObserved === true
          || (contextNestedStage === 'open-child'
            && contextNestedOpenedCurrent);
      marker.contextNestedA11yObserved =
        marker.contextNestedA11yObserved === true
          || contextNestedA11yCurrent;
      marker.contextNestedInitialFocusObserved =
        marker.contextNestedInitialFocusObserved === true
          || (contextNestedStage === 'open-child'
            && contextNestedOpenedCurrent
            && contextNestedCopyLinkFocusedCurrent);
      marker.contextNestedKeyboardMoveObserved =
        marker.contextNestedKeyboardMoveObserved === true
          || (contextNestedStage === 'move-child'
            && contextNestedOpenedCurrent
            && contextNestedExportReportFocusedCurrent);
      marker.contextNestedLeftReturnObserved =
        marker.contextNestedLeftReturnObserved === true
          || (contextNestedStage === 'return-parent'
            && !contextNestedOpenedCurrent
            && contextShare?.getAttribute?.('aria-expanded') === 'false');
      marker.contextNestedParentRemainsOpenObserved =
        marker.contextNestedParentRemainsOpenObserved === true
          || (contextNestedStage === 'return-parent'
            && contextOpenedCurrent
            && !contextNestedOpenedCurrent);
      marker.contextNestedParentFocusRestoredObserved =
        marker.contextNestedParentFocusRestoredObserved === true
          || (contextNestedStage === 'return-parent'
            && contextOpenedCurrent
            && !contextNestedOpenedCurrent
            && contextNestedParentFocusedCurrent);
      marker.contextNestedReopenedObserved =
        marker.contextNestedReopenedObserved === true
          || (contextNestedStage === 'reopen-child'
            && contextNestedOpenedCurrent);
      marker.contextNestedReopenedInitialFocusObserved =
        marker.contextNestedReopenedInitialFocusObserved === true
          || (contextNestedStage === 'reopen-child'
            && contextNestedOpenedCurrent
            && contextNestedCopyLinkFocusedCurrent);
      marker.contextNestedSelectionMoveObserved =
        marker.contextNestedSelectionMoveObserved === true
          || (contextNestedStage === 'move-child-for-selection'
            && contextNestedOpenedCurrent
            && contextNestedExportReportFocusedCurrent);
      marker.contextNestedSelectionObserved =
        marker.contextNestedSelectionObserved === true
          || (contextNestedStage === 'select-leaf'
            && contextNestedStatus.includes(
              'nested ContextMenu action: export-report'
                + ' client=true change=1'));
      marker.contextNestedJavaListenerObserved =
        marker.contextNestedJavaListenerObserved === true
          || contextNestedStatus.includes(
            'nested ContextMenu action: export-report'
              + ' client=true change=1');
      marker.contextNestedJavaRootCloseObserved =
        marker.contextNestedJavaRootCloseObserved === true
          || (contextNestedStage === 'select-leaf'
            && contextLifecycleStatus.includes(
              'ContextMenu lifecycle: closed client=true'));
      marker.contextNestedClosedObserved =
        marker.contextNestedClosedObserved === true
          || (contextNestedStage === 'select-leaf'
            && !contextOpenedCurrent
            && !contextNestedOpenedCurrent);
      marker.contextNestedFocusRestoredObserved =
        marker.contextNestedFocusRestoredObserved === true
          || (contextNestedStage === 'select-leaf'
            && !contextOpenedCurrent
            && !contextNestedOpenedCurrent
            && activeWithin(contextTarget));
      marker.contextNestedEscapeRootOpenedObserved =
        marker.contextNestedEscapeRootOpenedObserved === true
          || (contextNestedStage === 'escape-open-root'
            && contextOpenedCurrent);
      marker.contextNestedEscapeRootInitialFocusObserved =
        marker.contextNestedEscapeRootInitialFocusObserved === true
          || (contextNestedStage === 'escape-open-root'
            && contextOpenedCurrent
            && (contextNestedRootFocused === contextInspect
              || contextNestedRootFocusedText.includes('Inspect')));
      marker.contextNestedEscapeJavaRootOpenObserved =
        marker.contextNestedEscapeJavaRootOpenObserved === true
          || (contextNestedStage === 'escape-open-root'
            && contextLifecycleStatus.includes(
              'ContextMenu lifecycle: opened client=true'));
      marker.contextNestedEscapeFirstMoveObserved =
        marker.contextNestedEscapeFirstMoveObserved === true
          || (contextNestedStage === 'escape-move-first'
            && contextOpenedCurrent
            && (contextNestedRootFocused === contextArchive
              || contextNestedRootFocusedText.includes('Archive')));
      marker.contextNestedEscapeParentFocusObserved =
        marker.contextNestedEscapeParentFocusObserved === true
          || (contextNestedStage === 'escape-focus-parent'
            && contextOpenedCurrent
            && contextNestedParentFocusedCurrent);
      marker.contextNestedEscapeChildOpenedObserved =
        marker.contextNestedEscapeChildOpenedObserved === true
          || (contextNestedStage === 'escape-open-child'
            && contextNestedOpenedCurrent
            && contextNestedCopyLinkFocusedCurrent);
      marker.contextNestedEscapeChildCloseObserved =
        marker.contextNestedEscapeChildCloseObserved === true
          || (contextNestedStage === 'escape-close-child'
            && !contextNestedOpenedCurrent
            && contextShare?.getAttribute?.('aria-expanded') === 'false');
      marker.contextNestedEscapeParentRemainsOpenObserved =
        marker.contextNestedEscapeParentRemainsOpenObserved === true
          || (contextNestedStage === 'escape-close-child'
            && contextOpenedCurrent
            && !contextNestedOpenedCurrent
            && contextLifecycleStatus.includes(
              'ContextMenu lifecycle: opened client=true'));
      marker.contextNestedEscapeParentFocusRestoredObserved =
        marker.contextNestedEscapeParentFocusRestoredObserved === true
          || (contextNestedStage === 'escape-close-child'
            && contextOpenedCurrent
            && !contextNestedOpenedCurrent
            && contextNestedParentFocusedCurrent);
      marker.contextNestedEscapeRootCloseObserved =
        marker.contextNestedEscapeRootCloseObserved === true
          || (contextNestedStage === 'escape-close-root'
            && !contextOpenedCurrent
            && !contextNestedOpenedCurrent);
      marker.contextNestedEscapeJavaRootCloseObserved =
        marker.contextNestedEscapeJavaRootCloseObserved === true
          || (contextNestedStage === 'escape-close-root'
            && contextLifecycleStatus.includes(
              'ContextMenu lifecycle: closed client=true'));
      marker.contextNestedEscapeOwnerFocusRestoredObserved =
        marker.contextNestedEscapeOwnerFocusRestoredObserved === true
          || (contextNestedStage === 'escape-close-root'
            && !contextOpenedCurrent
            && !contextNestedOpenedCurrent
            && activeWithin(contextTarget));

      const grid =
        document.querySelector('#framework-flow-menu-popup-grid');
      const gridTarget = findDeep((element) =>
        element.localName === 'vaadin-grid-cell-content'
          && String(element.textContent || '').trim() === 'M-121');
      const gridItemTexts = new Set();
      const collectGridItems = (node) => {
        if (!node) return;
        if (node.nodeType === Node.ELEMENT_NODE
            && node.localName === 'vaadin-grid-cell-content') {
          const text = String(node.textContent || '').trim();
          if (/^M-\\d{3}$/.test(text)) gridItemTexts.add(text);
        }
        if (node.shadowRoot) collectGridItems(node.shadowRoot);
        if (typeof node.assignedNodes === 'function') {
          for (const child of node.assignedNodes({ flatten: true })) {
            collectGridItems(child);
          }
        }
        const children = node.children || node.childNodes || [];
        for (const child of children) collectGridItems(child);
      };
      collectGridItems(grid);
      const gridRenderedItemCount = gridItemTexts.size;
      const gridTargetRect = gridTarget?.getBoundingClientRect?.();
      const gridRect = grid?.getBoundingClientRect?.();
      const gridVirtualizedTargetReady =
        marker.gridViewportDispatched === true
          && !!gridTarget
          && gridRenderedItemCount > 0
          && gridRenderedItemCount < 180
          && Number(gridTargetRect?.width || 0) > 0
          && Number(gridTargetRect?.height || 0) > 0
          && Number(gridTargetRect?.bottom || 0)
            > Number(gridRect?.top || 0)
          && Number(gridTargetRect?.top || 0)
            < Number(gridRect?.bottom || 0);
      const gridContextMenu =
        document.querySelector(
          '#framework-flow-menu-popup-grid-context-menu');
      const gridContextOverlay = findDeep((element) =>
        element.localName === 'vaadin-context-menu-overlay'
          && collectDeepText(element).includes('Inspect row')
          && collectDeepText(element).includes('Archive row'));
      const gridContextList = findDeep((element) =>
        element.localName === 'vaadin-context-menu-list-box'
          && collectDeepText(element).includes('Inspect row')
          && collectDeepText(element).includes('Archive row'));
      const gridInspect = findDeep((element) =>
        element.id === 'framework-flow-menu-popup-grid-inspect'
          || (element.getAttribute?.('role') === 'menuitem'
            && collectDeepText(element).includes('Inspect row')));
      const gridArchive = findDeep((element) =>
        element.id === 'framework-flow-menu-popup-grid-archive'
          || (element.getAttribute?.('role') === 'menuitem'
            && collectDeepText(element).includes('Archive row')));
      const gridManage = findDeep((element) =>
        element.id === 'framework-flow-menu-popup-grid-manage'
          || (element.getAttribute?.('role') === 'menuitem'
            && collectDeepText(element).includes('Manage row')));
      const gridFocused = gridContextList?.focused
        || [gridInspect, gridArchive, gridManage].find((element) =>
          element?.hasAttribute?.('focused'));
      const gridFocusedText = collectDeepText(gridFocused);
      const gridOpenedCurrent =
        gridContextMenu?.opened === true
          && (gridContextOverlay?.opened === true
            || gridContextOverlay?.hasAttribute?.('opened') === true);
      const gridOpenStatus =
        document.querySelector(
          '#framework-flow-menu-popup-grid-open-status')
          ?.textContent || '';
      const gridActionStatus =
        document.querySelector(
          '#framework-flow-menu-popup-grid-action-status')
          ?.textContent || '';
      const gridLifecycleStatus =
        document.querySelector(
          '#framework-flow-menu-popup-grid-lifecycle-status')
          ?.textContent || '';
      const gridNestedStatus =
        document.querySelector(
          '#framework-flow-menu-popup-grid-nested-action-status')
          ?.textContent || '';
      const gridRowContextCurrent =
        gridOpenStatus.includes('item=M-121')
          && gridOpenStatus.includes('client=true');
      const gridColumnContextCurrent =
        gridOpenStatus.includes('column=ticket');
      const gridNestedMenu = gridContextMenu?._subMenu;
      const gridNestedOverlay = gridNestedMenu?._overlayElement;
      const gridNestedList = gridNestedMenu?._listBox
        || findDeep((element) =>
          element.localName === 'vaadin-context-menu-list-box'
            && collectDeepText(element).includes('Assign desktop')
            && collectDeepText(element).includes('Escalate audit'));
      const gridNestedItems = Array.from(
        gridNestedList?.items || gridNestedList?.children || []);
      const gridNestedAssign = gridNestedItems.find((element) =>
        element.id === 'framework-flow-menu-popup-grid-manage-assign')
        || findDeep((element) =>
          element.id === 'framework-flow-menu-popup-grid-manage-assign');
      const gridNestedEscalate = gridNestedItems.find((element) =>
        element.id === 'framework-flow-menu-popup-grid-manage-escalate')
        || findDeep((element) =>
          element.id === 'framework-flow-menu-popup-grid-manage-escalate');
      const gridNestedRootItems = Array.from(
        gridContextList?.items || gridContextList?.children || []);
      const gridNestedRootFocused = gridContextList?.focused
        || gridNestedRootItems.find((element) =>
          element?.hasAttribute?.('focused'));
      const gridNestedFocused = gridNestedList?.focused
        || gridNestedItems.find((element) =>
          element?.hasAttribute?.('focused'));
      const gridNestedOpenedCurrent =
        gridOpenedCurrent
          && gridNestedMenu?.opened === true
          && gridNestedMenu?.listenOn === gridManage
          && (gridNestedOverlay?.opened === true
            || gridNestedOverlay?.hasAttribute?.('opened') === true);
      const gridNestedParentFocusedCurrent =
        gridNestedRootFocused === gridManage
          || gridNestedRootFocused?.id
            === 'framework-flow-menu-popup-grid-manage';
      const gridNestedAssignFocusedCurrent =
        gridNestedFocused === gridNestedAssign
          || gridNestedFocused?.id
            === 'framework-flow-menu-popup-grid-manage-assign';
      const gridNestedEscalateFocusedCurrent =
        gridNestedFocused === gridNestedEscalate
          || gridNestedFocused?.id
            === 'framework-flow-menu-popup-grid-manage-escalate';
      const gridNestedA11yCurrent =
        gridNestedOpenedCurrent
          && gridManage?.getAttribute?.('role') === 'menuitem'
          && gridManage?.getAttribute?.('aria-haspopup') === 'true'
          && gridManage?.getAttribute?.('aria-expanded') === 'true'
          && gridNestedList?.getAttribute?.('role') === 'menu'
          && gridNestedAssign?.getAttribute?.('role') === 'menuitem'
          && gridNestedEscalate?.getAttribute?.('role') === 'menuitem'
          && gridNestedOverlay?.parentOverlay === gridContextOverlay;
      const gridNestedStage = String(marker.gridNestedStage || '');
      const gridNestedRootFocusedText =
        collectDeepText(gridNestedRootFocused);
      const gridNestedFocusedText = collectDeepText(gridNestedFocused);
      const gridNestedOverlayRect =
        gridNestedOverlay?.getBoundingClientRect?.();
      const gridNestedDiagnostics = [
        `stage=${gridNestedStage}`,
        `rootOpened=${gridOpenedCurrent}`,
        `childOpened=${gridNestedOpenedCurrent}`,
        `childOwnsParent=${gridNestedMenu?.listenOn === gridManage}`,
        `childParentOverlay=${
          gridNestedOverlay?.parentOverlay === gridContextOverlay}`,
        `parentExpanded=${String(
          gridManage?.getAttribute?.('aria-expanded') || '')}`,
        `rootFocused=${String(
          gridNestedRootFocused?.id
            || gridNestedRootFocused?.localName
            || '')}`,
        `childFocused=${String(
          gridNestedFocused?.id || gridNestedFocused?.localName || '')}`,
        `childItems=${gridNestedItems.length}`,
        `childWidth=${Number(gridNestedOverlayRect?.width || 0)}`,
        `childHeight=${Number(gridNestedOverlayRect?.height || 0)}`,
        `open=${gridOpenStatus.replace(/\\s+/g, ' ').trim()}`,
        `lifecycle=${gridLifecycleStatus.replace(/\\s+/g, ' ').trim()}`
      ].join(',');

      marker.gridOpenedObserved =
        marker.gridOpenedObserved === true
          || (marker.gridPointerDispatched === true
            && gridOpenedCurrent);
      marker.gridVirtualizedTargetObserved =
        marker.gridVirtualizedTargetObserved === true
          || (marker.gridPointerDispatched === true
            && String(marker.gridPointerTargetText || '') === 'M-121'
            && gridRenderedItemCount > 0
            && gridRenderedItemCount < 180);
      marker.gridRowContextObserved =
        marker.gridRowContextObserved === true
          || gridRowContextCurrent;
      marker.gridColumnContextObserved =
        marker.gridColumnContextObserved === true
          || gridColumnContextCurrent;
      marker.gridInitialFocusObserved =
        marker.gridInitialFocusObserved === true
          || (gridOpenedCurrent
            && (gridFocusedText.includes('Inspect row')
              || activeWithin(gridContextList)));
      marker.gridKeyboardMoveObserved =
        marker.gridKeyboardMoveObserved === true
          || (marker.gridMoveDispatched === true
            && gridOpenedCurrent
            && gridFocusedText.includes('Archive row'));
      marker.gridSelectionObserved =
        marker.gridSelectionObserved === true
          || (marker.gridSelectionDispatched === true
            && gridActionStatus.includes(
              'GridContextMenu action: archive item=M-121 client=true change=1'));
      marker.gridJavaOpenObserved =
        marker.gridJavaOpenObserved === true
          || (gridRowContextCurrent && gridColumnContextCurrent);
      marker.gridJavaListenerObserved =
        marker.gridJavaListenerObserved === true
          || gridActionStatus.includes(
            'GridContextMenu action: archive item=M-121 client=true change=1');
      marker.gridJavaCloseObserved =
        marker.gridJavaCloseObserved === true
          || (gridLifecycleStatus.includes(
              'GridContextMenu lifecycle: closed')
            && gridLifecycleStatus.includes('item=M-121')
            && gridLifecycleStatus.includes('column=ticket')
            && gridLifecycleStatus.includes('client=true'));
      marker.gridClosedObserved =
        marker.gridClosedObserved === true
          || (marker.gridSelectionDispatched === true
            && !gridOpenedCurrent);
      marker.gridFocusRestoredObserved =
        marker.gridFocusRestoredObserved === true
          || (marker.gridSelectionDispatched === true
            && !gridOpenedCurrent
            && activeWithin(grid));
      marker.gridNestedRootOpenedObserved =
        marker.gridNestedRootOpenedObserved === true
          || (gridNestedStage === 'open-root'
            && gridOpenedCurrent);
      marker.gridNestedRootInitialFocusObserved =
        marker.gridNestedRootInitialFocusObserved === true
          || (gridNestedStage === 'open-root'
            && gridOpenedCurrent
            && (gridNestedRootFocused === gridInspect
              || gridNestedRootFocusedText.includes('Inspect row')));
      marker.gridNestedJavaRootOpenObserved =
        marker.gridNestedJavaRootOpenObserved === true
          || (gridNestedStage === 'open-root'
            && gridLifecycleStatus.includes(
              'GridContextMenu lifecycle: opened')
            && gridLifecycleStatus.includes('item=M-121')
            && gridLifecycleStatus.includes('column=ticket')
            && gridLifecycleStatus.includes('client=true'));
      marker.gridNestedContextObserved =
        marker.gridNestedContextObserved === true
          || (gridNestedStage === 'open-root'
            && gridRowContextCurrent
            && gridColumnContextCurrent);
      marker.gridNestedFirstMoveObserved =
        marker.gridNestedFirstMoveObserved === true
          || (gridNestedStage === 'move-first'
            && gridOpenedCurrent
            && (gridNestedRootFocused === gridArchive
              || gridNestedRootFocusedText.includes('Archive row')));
      marker.gridNestedParentFocusObserved =
        marker.gridNestedParentFocusObserved === true
          || (gridNestedStage === 'focus-parent'
            && gridOpenedCurrent
            && gridNestedParentFocusedCurrent
            && gridManage?.getAttribute?.('aria-haspopup') === 'true');
      marker.gridNestedChildOpenedObserved =
        marker.gridNestedChildOpenedObserved === true
          || (gridNestedStage === 'open-child'
            && gridNestedOpenedCurrent);
      marker.gridNestedA11yObserved =
        marker.gridNestedA11yObserved === true
          || gridNestedA11yCurrent;
      marker.gridNestedInitialFocusObserved =
        marker.gridNestedInitialFocusObserved === true
          || (gridNestedStage === 'open-child'
            && gridNestedOpenedCurrent
            && gridNestedAssignFocusedCurrent);
      marker.gridNestedKeyboardMoveObserved =
        marker.gridNestedKeyboardMoveObserved === true
          || (gridNestedStage === 'move-child'
            && gridNestedOpenedCurrent
            && gridNestedEscalateFocusedCurrent);
      marker.gridNestedLeftReturnObserved =
        marker.gridNestedLeftReturnObserved === true
          || (gridNestedStage === 'return-parent'
            && !gridNestedOpenedCurrent
            && gridManage?.getAttribute?.('aria-expanded') === 'false');
      marker.gridNestedParentRemainsOpenObserved =
        marker.gridNestedParentRemainsOpenObserved === true
          || (gridNestedStage === 'return-parent'
            && gridOpenedCurrent
            && !gridNestedOpenedCurrent);
      marker.gridNestedParentFocusRestoredObserved =
        marker.gridNestedParentFocusRestoredObserved === true
          || (gridNestedStage === 'return-parent'
            && gridOpenedCurrent
            && !gridNestedOpenedCurrent
            && gridNestedParentFocusedCurrent);
      marker.gridNestedContextRetainedObserved =
        marker.gridNestedContextRetainedObserved === true
          || (gridNestedStage === 'return-parent'
            && gridRowContextCurrent
            && gridColumnContextCurrent
            && gridLifecycleStatus.includes(
              'GridContextMenu lifecycle: opened')
            && gridLifecycleStatus.includes('item=M-121')
            && gridLifecycleStatus.includes('column=ticket')
            && gridLifecycleStatus.includes('client=true'));
      marker.gridNestedReopenedObserved =
        marker.gridNestedReopenedObserved === true
          || (gridNestedStage === 'reopen-child'
            && gridNestedOpenedCurrent);
      marker.gridNestedReopenedInitialFocusObserved =
        marker.gridNestedReopenedInitialFocusObserved === true
          || (gridNestedStage === 'reopen-child'
            && gridNestedOpenedCurrent
            && gridNestedAssignFocusedCurrent);
      marker.gridNestedSelectionMoveObserved =
        marker.gridNestedSelectionMoveObserved === true
          || (gridNestedStage === 'move-child-for-selection'
            && gridNestedOpenedCurrent
            && gridNestedEscalateFocusedCurrent);
      marker.gridNestedSelectionObserved =
        marker.gridNestedSelectionObserved === true
          || (gridNestedStage === 'select-leaf'
            && gridNestedStatus.includes(
              'nested GridContextMenu action: escalate'
                + ' item=M-121 client=true change=1'));
      marker.gridNestedJavaListenerObserved =
        marker.gridNestedJavaListenerObserved === true
          || gridNestedStatus.includes(
            'nested GridContextMenu action: escalate'
              + ' item=M-121 client=true change=1');
      marker.gridNestedJavaRootCloseObserved =
        marker.gridNestedJavaRootCloseObserved === true
          || (gridNestedStage === 'select-leaf'
            && gridLifecycleStatus.includes(
              'GridContextMenu lifecycle: closed')
            && gridLifecycleStatus.includes('item=M-121')
            && gridLifecycleStatus.includes('column=ticket')
            && gridLifecycleStatus.includes('client=true'));
      marker.gridNestedClosedObserved =
        marker.gridNestedClosedObserved === true
          || (gridNestedStage === 'select-leaf'
            && !gridOpenedCurrent
            && !gridNestedOpenedCurrent);
      marker.gridNestedFocusRestoredObserved =
        marker.gridNestedFocusRestoredObserved === true
          || (gridNestedStage === 'select-leaf'
            && !gridOpenedCurrent
            && !gridNestedOpenedCurrent
            && activeWithin(grid));
      marker.gridNestedEscapeRootOpenedObserved =
        marker.gridNestedEscapeRootOpenedObserved === true
          || (gridNestedStage === 'escape-open-root'
            && gridOpenedCurrent);
      marker.gridNestedEscapeRootInitialFocusObserved =
        marker.gridNestedEscapeRootInitialFocusObserved === true
          || (gridNestedStage === 'escape-open-root'
            && gridOpenedCurrent
            && (gridNestedRootFocused === gridInspect
              || gridNestedRootFocusedText.includes('Inspect row')));
      marker.gridNestedEscapeJavaRootOpenObserved =
        marker.gridNestedEscapeJavaRootOpenObserved === true
          || (gridNestedStage === 'escape-open-root'
            && gridLifecycleStatus.includes(
              'GridContextMenu lifecycle: opened')
            && gridLifecycleStatus.includes('item=M-121')
            && gridLifecycleStatus.includes('column=ticket')
            && gridLifecycleStatus.includes('client=true'));
      marker.gridNestedEscapeFirstMoveObserved =
        marker.gridNestedEscapeFirstMoveObserved === true
          || (gridNestedStage === 'escape-move-first'
            && gridOpenedCurrent
            && (gridNestedRootFocused === gridArchive
              || gridNestedRootFocusedText.includes('Archive row')));
      marker.gridNestedEscapeParentFocusObserved =
        marker.gridNestedEscapeParentFocusObserved === true
          || (gridNestedStage === 'escape-focus-parent'
            && gridOpenedCurrent
            && gridNestedParentFocusedCurrent);
      marker.gridNestedEscapeChildOpenedObserved =
        marker.gridNestedEscapeChildOpenedObserved === true
          || (gridNestedStage === 'escape-open-child'
            && gridNestedOpenedCurrent
            && gridNestedAssignFocusedCurrent);
      marker.gridNestedEscapeChildCloseObserved =
        marker.gridNestedEscapeChildCloseObserved === true
          || (gridNestedStage === 'escape-close-child'
            && !gridNestedOpenedCurrent
            && gridManage?.getAttribute?.('aria-expanded') === 'false');
      marker.gridNestedEscapeParentRemainsOpenObserved =
        marker.gridNestedEscapeParentRemainsOpenObserved === true
          || (gridNestedStage === 'escape-close-child'
            && gridOpenedCurrent
            && !gridNestedOpenedCurrent
            && gridLifecycleStatus.includes(
              'GridContextMenu lifecycle: opened'));
      marker.gridNestedEscapeParentFocusRestoredObserved =
        marker.gridNestedEscapeParentFocusRestoredObserved === true
          || (gridNestedStage === 'escape-close-child'
            && gridOpenedCurrent
            && !gridNestedOpenedCurrent
            && gridNestedParentFocusedCurrent);
      marker.gridNestedEscapeRootCloseObserved =
        marker.gridNestedEscapeRootCloseObserved === true
          || (gridNestedStage === 'escape-close-root'
            && !gridOpenedCurrent
            && !gridNestedOpenedCurrent);
      marker.gridNestedEscapeJavaRootCloseObserved =
        marker.gridNestedEscapeJavaRootCloseObserved === true
          || (gridNestedStage === 'escape-close-root'
            && gridLifecycleStatus.includes(
              'GridContextMenu lifecycle: closed')
            && gridLifecycleStatus.includes('item=M-121')
            && gridLifecycleStatus.includes('column=ticket')
            && gridLifecycleStatus.includes('client=true'));
      marker.gridNestedEscapeOwnerFocusRestoredObserved =
        marker.gridNestedEscapeOwnerFocusRestoredObserved === true
          || (gridNestedStage === 'escape-close-root'
            && !gridOpenedCurrent
            && !gridNestedOpenedCurrent
            && activeWithin(grid));

      const gridPolicy = document.querySelector(
        '#framework-flow-menu-popup-grid-policy');
      const gridPolicyContextMenu = document.querySelector(
        '#framework-flow-menu-popup-grid-policy-context-menu');
      const gridPolicyOverlay =
        gridPolicyContextMenu?._overlayElement
        || findDeep((element) =>
          element.localName === 'vaadin-context-menu-overlay'
            && collectDeepText(element).includes('Inspect P-001'));
      const gridPolicyList =
        gridPolicyContextMenu?._listBox
        || findDeep((element) =>
          element.localName === 'vaadin-context-menu-list-box'
            && (collectDeepText(element).includes('Inspect P-001')
              || collectDeepText(element)
                .includes('Inspect policy row')));
      const gridPolicyInspect = findDeep((element) =>
        element.id === 'framework-flow-menu-popup-grid-policy-inspect');
      const gridPolicyFocused = gridPolicyList?.focused
        || [gridPolicyInspect].find((element) =>
          element?.hasAttribute?.('focused'));
      const gridPolicyFocusedText = collectDeepText(gridPolicyFocused);
      const gridPolicyInspectText = collectDeepText(gridPolicyInspect);
      const gridPolicyOpenedCurrent =
        gridPolicyContextMenu?.opened === true
          && (gridPolicyOverlay?.opened === true
            || gridPolicyOverlay?.hasAttribute?.('opened') === true);
      const gridPolicyStatus = document.querySelector(
        '#framework-flow-menu-popup-grid-policy-status')
        ?.textContent || '';
      const gridPolicyLifecycleStatus = document.querySelector(
        '#framework-flow-menu-popup-grid-policy-lifecycle-status')
        ?.textContent || '';
      const gridPolicyActionStatus = document.querySelector(
        '#framework-flow-menu-popup-grid-policy-action-status')
        ?.textContent || '';
      const gridPolicyStage = String(marker.gridPolicyStage || '');
      const gridPolicyDiagnostics = [
        `stage=${gridPolicyStage}`,
        `opened=${gridPolicyOpenedCurrent}`,
        `section=${String(marker.gridPolicyEventSection || '')}`,
        `itemKey=${String(marker.gridPolicyEventItemKey || '')}`,
        `column=${String(marker.gridPolicyEventColumnId || '')}`,
        `target=${String(marker.gridPolicyEventTargetText || '')}`,
        `emptyGeometry=${
          marker.gridPolicyEmptyGeometryObserved === true}`,
        `focused=${String(
          gridPolicyFocused?.id
            || gridPolicyFocused?.localName
            || '')}`,
        `itemText=${gridPolicyInspectText}`,
        `policy=${gridPolicyStatus.replace(/\\s+/g, ' ').trim()}`,
        `lifecycle=${
          gridPolicyLifecycleStatus.replace(/\\s+/g, ' ').trim()}`,
        `action=${
          gridPolicyActionStatus.replace(/\\s+/g, ' ').trim()}`
      ].join(',');

      marker.gridPolicyHeaderJavaRejectObserved =
        marker.gridPolicyHeaderJavaRejectObserved === true
          || (gridPolicyStage === 'header'
            && gridPolicyStatus.includes(
              'GridContextMenu policy: item=none allowed=false change=1'));
      marker.gridPolicyHeaderClosedObserved =
        marker.gridPolicyHeaderClosedObserved === true
          || (gridPolicyStage === 'header'
            && marker.gridPolicyHeaderJavaRejectObserved === true
            && !gridPolicyOpenedCurrent);
      marker.gridPolicyEmptyJavaRejectObserved =
        marker.gridPolicyEmptyJavaRejectObserved === true
          || (gridPolicyStage === 'empty'
            && gridPolicyStatus.includes(
              'GridContextMenu policy: item=none allowed=false change=2'));
      marker.gridPolicyEmptyClosedObserved =
        marker.gridPolicyEmptyClosedObserved === true
          || (gridPolicyStage === 'empty'
            && marker.gridPolicyEmptyJavaRejectObserved === true
            && !gridPolicyOpenedCurrent);
      marker.gridPolicyRowOpenedObserved =
        marker.gridPolicyRowOpenedObserved === true
          || (gridPolicyStage === 'row'
            && gridPolicyOpenedCurrent
            && gridPolicyList?.getAttribute?.('role') === 'menu'
            && gridPolicyInspect?.getAttribute?.('role') === 'menuitem');
      marker.gridPolicyRowInitialFocusObserved =
        marker.gridPolicyRowInitialFocusObserved === true
          || (gridPolicyStage === 'row'
            && gridPolicyOpenedCurrent
            && (gridPolicyFocused === gridPolicyInspect
              || gridPolicyFocusedText.includes('Inspect P-001')));
      marker.gridPolicyDynamicContentObserved =
        marker.gridPolicyDynamicContentObserved === true
          || (gridPolicyStage === 'row'
            && gridPolicyStatus.includes(
              'GridContextMenu policy: item=P-001 allowed=true change=3')
            && gridPolicyInspectText.includes('Inspect P-001'));
      marker.gridPolicyJavaOpenObserved =
        marker.gridPolicyJavaOpenObserved === true
          || (gridPolicyStage === 'row'
            && gridPolicyLifecycleStatus.includes(
              'GridContextMenu policy lifecycle: opened')
            && gridPolicyLifecycleStatus.includes('item=P-001')
            && gridPolicyLifecycleStatus.includes(
              'column=policy-ticket')
            && gridPolicyLifecycleStatus.includes('client=true'));
      marker.gridPolicyJavaListenerObserved =
        marker.gridPolicyJavaListenerObserved === true
          || gridPolicyActionStatus.includes(
            'GridContextMenu policy action: inspect item=P-001'
              + ' client=true change=1');
      marker.gridPolicyJavaCloseObserved =
        marker.gridPolicyJavaCloseObserved === true
          || (marker.gridPolicySelectionDispatched === true
            && gridPolicyLifecycleStatus.includes(
              'GridContextMenu policy lifecycle: closed')
            && gridPolicyLifecycleStatus.includes('item=P-001')
            && gridPolicyLifecycleStatus.includes(
              'column=policy-ticket')
            && gridPolicyLifecycleStatus.includes('client=true'));
      marker.gridPolicyClosedObserved =
        marker.gridPolicyClosedObserved === true
          || (marker.gridPolicySelectionDispatched === true
            && !gridPolicyOpenedCurrent);
      marker.gridPolicyFocusRestoredObserved =
        marker.gridPolicyFocusRestoredObserved === true
          || (marker.gridPolicySelectionDispatched === true
            && !gridPolicyOpenedCurrent
            && activeWithin(gridPolicy));

      const clients = window.Vaadin?.Flow?.clients
        ? Object.keys(window.Vaadin.Flow.clients)
          .filter((name) => name !== 'TypeScript')
        : [];
      return JSON.stringify({
        rootRendered:
          !!document.querySelector('#framework-root')
            || !!document.querySelector(
              '#framework-flow-menu-popup-route'),
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
          window.location.pathname.endsWith('/flow-menu-popup')
            && !!document.querySelector(
              '#framework-flow-menu-popup-route'),
        path: window.location.pathname,
        menuOpenedObserved: marker.menuOpenedObserved === true,
        menuA11yObserved: marker.menuA11yObserved === true,
        menuOwnerFocusWhileOpenObserved:
          marker.menuOwnerFocusWhileOpenObserved === true,
        menuExpandedButtonMatchesOwner:
          marker.menuExpandedButtonMatchesOwner === true,
        menuKeyEventCount:
          Array.isArray(marker.menuKeyDiagnostics)
            ? marker.menuKeyDiagnostics.length
            : 0,
        menuKeyDiagnostics:
          Array.isArray(marker.menuKeyDiagnostics)
            ? marker.menuKeyDiagnostics.join('|')
            : '',
        menuOwnedOverlayDiagnostics,
        menuInitialFocusObserved:
          marker.menuInitialFocusObserved === true,
        menuKeyboardMoveObserved:
          marker.menuKeyboardMoveObserved === true,
        menuSelectionObserved: marker.menuSelectionObserved === true,
        menuJavaListenerObserved:
          marker.menuJavaListenerObserved === true,
        menuClosedObserved: marker.menuClosedObserved === true,
        menuFocusRestoredObserved:
          marker.menuFocusRestoredObserved === true,
        menuEscapeOpenedObserved:
          marker.menuEscapeOpenedObserved === true,
        menuEscapeCloseObserved:
          marker.menuEscapeCloseObserved === true,
        nestedRootOpenedObserved:
          marker.nestedRootOpenedObserved === true,
        nestedRootInitialFocusObserved:
          marker.nestedRootInitialFocusObserved === true,
        nestedFirstMoveObserved:
          marker.nestedFirstMoveObserved === true,
        nestedParentFocusObserved:
          marker.nestedParentFocusObserved === true,
        nestedChildOpenedObserved:
          marker.nestedChildOpenedObserved === true,
        nestedA11yObserved:
          marker.nestedA11yObserved === true,
        nestedInitialFocusObserved:
          marker.nestedInitialFocusObserved === true,
        nestedKeyboardMoveObserved:
          marker.nestedKeyboardMoveObserved === true,
        nestedLeftReturnObserved:
          marker.nestedLeftReturnObserved === true,
        nestedParentRemainsOpenObserved:
          marker.nestedParentRemainsOpenObserved === true,
        nestedParentFocusRestoredObserved:
          marker.nestedParentFocusRestoredObserved === true,
        nestedReopenedObserved:
          marker.nestedReopenedObserved === true,
        nestedReopenedInitialFocusObserved:
          marker.nestedReopenedInitialFocusObserved === true,
        nestedSelectionMoveObserved:
          marker.nestedSelectionMoveObserved === true,
        nestedSelectionObserved:
          marker.nestedSelectionObserved === true,
        nestedJavaListenerObserved:
          marker.nestedJavaListenerObserved === true,
        nestedClosedObserved:
          marker.nestedClosedObserved === true,
        nestedFocusRestoredObserved:
          marker.nestedFocusRestoredObserved === true,
        nestedEscapeRootOpenedObserved:
          marker.nestedEscapeRootOpenedObserved === true,
        nestedEscapeFirstMoveObserved:
          marker.nestedEscapeFirstMoveObserved === true,
        nestedEscapeParentFocusObserved:
          marker.nestedEscapeParentFocusObserved === true,
        nestedEscapeChildOpenedObserved:
          marker.nestedEscapeChildOpenedObserved === true,
        nestedEscapeChildCloseObserved:
          marker.nestedEscapeChildCloseObserved === true,
        nestedEscapeParentRemainsOpenObserved:
          marker.nestedEscapeParentRemainsOpenObserved === true,
        nestedEscapeParentFocusRestoredObserved:
          marker.nestedEscapeParentFocusRestoredObserved === true,
        nestedEscapeRootCloseObserved:
          marker.nestedEscapeRootCloseObserved === true,
        nestedEscapeOwnerFocusRestoredObserved:
          marker.nestedEscapeOwnerFocusRestoredObserved === true,
        overflowNarrowObserved:
          marker.overflowNarrowObserved === true,
        overflowOpenedObserved:
          marker.overflowOpenedObserved === true,
        overflowA11yObserved:
          marker.overflowA11yObserved === true,
        overflowInitialFocusObserved:
          marker.overflowInitialFocusObserved === true,
        overflowSelectionObserved:
          marker.overflowSelectionObserved === true,
        overflowJavaListenerObserved:
          marker.overflowJavaListenerObserved === true,
        overflowClosedObserved:
          marker.overflowClosedObserved === true,
        overflowFocusRestoredObserved:
          marker.overflowFocusRestoredObserved === true,
        overflowEscapeOpenedObserved:
          marker.overflowEscapeOpenedObserved === true,
        overflowEscapeInitialFocusObserved:
          marker.overflowEscapeInitialFocusObserved === true,
        overflowEscapeCloseObserved:
          marker.overflowEscapeCloseObserved === true,
        overflowEscapeFocusRestoredObserved:
          marker.overflowEscapeFocusRestoredObserved === true,
        overflowNestedCollapsedParentObserved:
          marker.overflowNestedCollapsedParentObserved === true,
        overflowNestedRootOpenedObserved:
          marker.overflowNestedRootOpenedObserved === true,
        overflowNestedRootInitialFocusObserved:
          marker.overflowNestedRootInitialFocusObserved === true,
        overflowNestedParentFocusObserved:
          marker.overflowNestedParentFocusObserved === true,
        overflowNestedChildOpenedObserved:
          marker.overflowNestedChildOpenedObserved === true,
        overflowNestedA11yObserved:
          marker.overflowNestedA11yObserved === true,
        overflowNestedInitialFocusObserved:
          marker.overflowNestedInitialFocusObserved === true,
        overflowNestedKeyboardMoveObserved:
          marker.overflowNestedKeyboardMoveObserved === true,
        overflowNestedLeftReturnObserved:
          marker.overflowNestedLeftReturnObserved === true,
        overflowNestedParentRemainsOpenObserved:
          marker.overflowNestedParentRemainsOpenObserved === true,
        overflowNestedParentFocusRestoredObserved:
          marker.overflowNestedParentFocusRestoredObserved === true,
        overflowNestedReopenedObserved:
          marker.overflowNestedReopenedObserved === true,
        overflowNestedReopenedInitialFocusObserved:
          marker.overflowNestedReopenedInitialFocusObserved === true,
        overflowNestedSelectionMoveObserved:
          marker.overflowNestedSelectionMoveObserved === true,
        overflowNestedSelectionObserved:
          marker.overflowNestedSelectionObserved === true,
        overflowNestedJavaListenerObserved:
          marker.overflowNestedJavaListenerObserved === true,
        overflowNestedClosedObserved:
          marker.overflowNestedClosedObserved === true,
        overflowNestedFocusRestoredObserved:
          marker.overflowNestedFocusRestoredObserved === true,
        overflowNestedEscapeRootOpenedObserved:
          marker.overflowNestedEscapeRootOpenedObserved === true,
        overflowNestedEscapeRootInitialFocusObserved:
          marker.overflowNestedEscapeRootInitialFocusObserved === true,
        overflowNestedEscapeParentFocusObserved:
          marker.overflowNestedEscapeParentFocusObserved === true,
        overflowNestedEscapeChildOpenedObserved:
          marker.overflowNestedEscapeChildOpenedObserved === true,
        overflowNestedEscapeChildCloseObserved:
          marker.overflowNestedEscapeChildCloseObserved === true,
        overflowNestedEscapeParentRemainsOpenObserved:
          marker.overflowNestedEscapeParentRemainsOpenObserved === true,
        overflowNestedEscapeParentFocusRestoredObserved:
          marker.overflowNestedEscapeParentFocusRestoredObserved === true,
        overflowNestedEscapeRootCloseObserved:
          marker.overflowNestedEscapeRootCloseObserved === true,
        overflowNestedEscapeOwnerFocusRestoredObserved:
          marker.overflowNestedEscapeOwnerFocusRestoredObserved === true,
        overflowExpandedObserved:
          marker.overflowExpandedObserved === true,
        overflowItemsRestoredObserved:
          marker.overflowItemsRestoredObserved === true,
        overflowNestedStage,
        overflowNestedStages:
          Array.isArray(marker.overflowNestedStages)
            ? marker.overflowNestedStages.join(',')
            : '',
        overflowNestedOpenedCurrent,
        overflowNestedRootFocusedText,
        overflowNestedFocusedText,
        overflowNestedStatus,
        overflowNestedDiagnostics,
        contextOpenedObserved: marker.contextOpenedObserved === true,
        contextA11yObserved: marker.contextA11yObserved === true,
        contextInitialFocusObserved:
          marker.contextInitialFocusObserved === true,
        contextKeyboardMoveObserved:
          marker.contextKeyboardMoveObserved === true,
        contextSelectionObserved:
          marker.contextSelectionObserved === true,
        contextJavaListenerObserved:
          marker.contextJavaListenerObserved === true,
        contextClosedObserved: marker.contextClosedObserved === true,
        contextFocusRestoredObserved:
          marker.contextFocusRestoredObserved === true,
        contextEscapeOpenedObserved:
          marker.contextEscapeOpenedObserved === true,
        contextEscapeCloseObserved:
          marker.contextEscapeCloseObserved === true,
        contextJavaOpenObserved:
          marker.contextJavaOpenObserved === true,
        contextJavaCloseObserved:
          marker.contextJavaCloseObserved === true,
        contextNestedRootOpenedObserved:
          marker.contextNestedRootOpenedObserved === true,
        contextNestedRootInitialFocusObserved:
          marker.contextNestedRootInitialFocusObserved === true,
        contextNestedJavaRootOpenObserved:
          marker.contextNestedJavaRootOpenObserved === true,
        contextNestedFirstMoveObserved:
          marker.contextNestedFirstMoveObserved === true,
        contextNestedParentFocusObserved:
          marker.contextNestedParentFocusObserved === true,
        contextNestedChildOpenedObserved:
          marker.contextNestedChildOpenedObserved === true,
        contextNestedA11yObserved:
          marker.contextNestedA11yObserved === true,
        contextNestedInitialFocusObserved:
          marker.contextNestedInitialFocusObserved === true,
        contextNestedKeyboardMoveObserved:
          marker.contextNestedKeyboardMoveObserved === true,
        contextNestedLeftReturnObserved:
          marker.contextNestedLeftReturnObserved === true,
        contextNestedParentRemainsOpenObserved:
          marker.contextNestedParentRemainsOpenObserved === true,
        contextNestedParentFocusRestoredObserved:
          marker.contextNestedParentFocusRestoredObserved === true,
        contextNestedReopenedObserved:
          marker.contextNestedReopenedObserved === true,
        contextNestedReopenedInitialFocusObserved:
          marker.contextNestedReopenedInitialFocusObserved === true,
        contextNestedSelectionMoveObserved:
          marker.contextNestedSelectionMoveObserved === true,
        contextNestedSelectionObserved:
          marker.contextNestedSelectionObserved === true,
        contextNestedJavaListenerObserved:
          marker.contextNestedJavaListenerObserved === true,
        contextNestedJavaRootCloseObserved:
          marker.contextNestedJavaRootCloseObserved === true,
        contextNestedClosedObserved:
          marker.contextNestedClosedObserved === true,
        contextNestedFocusRestoredObserved:
          marker.contextNestedFocusRestoredObserved === true,
        contextNestedEscapeRootOpenedObserved:
          marker.contextNestedEscapeRootOpenedObserved === true,
        contextNestedEscapeRootInitialFocusObserved:
          marker.contextNestedEscapeRootInitialFocusObserved === true,
        contextNestedEscapeJavaRootOpenObserved:
          marker.contextNestedEscapeJavaRootOpenObserved === true,
        contextNestedEscapeFirstMoveObserved:
          marker.contextNestedEscapeFirstMoveObserved === true,
        contextNestedEscapeParentFocusObserved:
          marker.contextNestedEscapeParentFocusObserved === true,
        contextNestedEscapeChildOpenedObserved:
          marker.contextNestedEscapeChildOpenedObserved === true,
        contextNestedEscapeChildCloseObserved:
          marker.contextNestedEscapeChildCloseObserved === true,
        contextNestedEscapeParentRemainsOpenObserved:
          marker.contextNestedEscapeParentRemainsOpenObserved === true,
        contextNestedEscapeParentFocusRestoredObserved:
          marker.contextNestedEscapeParentFocusRestoredObserved === true,
        contextNestedEscapeRootCloseObserved:
          marker.contextNestedEscapeRootCloseObserved === true,
        contextNestedEscapeJavaRootCloseObserved:
          marker.contextNestedEscapeJavaRootCloseObserved === true,
        contextNestedEscapeOwnerFocusRestoredObserved:
          marker.contextNestedEscapeOwnerFocusRestoredObserved === true,
        gridVirtualizedTargetReady,
        gridOpenedObserved: marker.gridOpenedObserved === true,
        gridVirtualizedTargetObserved:
          marker.gridVirtualizedTargetObserved === true
            && gridRenderedItemCount > 0
            && gridRenderedItemCount < 180,
        gridRowContextObserved:
          marker.gridRowContextObserved === true,
        gridColumnContextObserved:
          marker.gridColumnContextObserved === true,
        gridInitialFocusObserved:
          marker.gridInitialFocusObserved === true,
        gridKeyboardMoveObserved:
          marker.gridKeyboardMoveObserved === true,
        gridSelectionObserved:
          marker.gridSelectionObserved === true,
        gridJavaOpenObserved:
          marker.gridJavaOpenObserved === true,
        gridJavaListenerObserved:
          marker.gridJavaListenerObserved === true,
        gridJavaCloseObserved:
          marker.gridJavaCloseObserved === true,
        gridClosedObserved: marker.gridClosedObserved === true,
        gridFocusRestoredObserved:
          marker.gridFocusRestoredObserved === true,
        gridPointerTrustedObserved:
          marker.gridPointerTrustedObserved === true,
        gridNestedRootOpenedObserved:
          marker.gridNestedRootOpenedObserved === true,
        gridNestedRootInitialFocusObserved:
          marker.gridNestedRootInitialFocusObserved === true,
        gridNestedJavaRootOpenObserved:
          marker.gridNestedJavaRootOpenObserved === true,
        gridNestedContextObserved:
          marker.gridNestedContextObserved === true,
        gridNestedFirstMoveObserved:
          marker.gridNestedFirstMoveObserved === true,
        gridNestedParentFocusObserved:
          marker.gridNestedParentFocusObserved === true,
        gridNestedChildOpenedObserved:
          marker.gridNestedChildOpenedObserved === true,
        gridNestedA11yObserved:
          marker.gridNestedA11yObserved === true,
        gridNestedInitialFocusObserved:
          marker.gridNestedInitialFocusObserved === true,
        gridNestedKeyboardMoveObserved:
          marker.gridNestedKeyboardMoveObserved === true,
        gridNestedLeftReturnObserved:
          marker.gridNestedLeftReturnObserved === true,
        gridNestedParentRemainsOpenObserved:
          marker.gridNestedParentRemainsOpenObserved === true,
        gridNestedParentFocusRestoredObserved:
          marker.gridNestedParentFocusRestoredObserved === true,
        gridNestedContextRetainedObserved:
          marker.gridNestedContextRetainedObserved === true,
        gridNestedReopenedObserved:
          marker.gridNestedReopenedObserved === true,
        gridNestedReopenedInitialFocusObserved:
          marker.gridNestedReopenedInitialFocusObserved === true,
        gridNestedSelectionMoveObserved:
          marker.gridNestedSelectionMoveObserved === true,
        gridNestedSelectionObserved:
          marker.gridNestedSelectionObserved === true,
        gridNestedJavaListenerObserved:
          marker.gridNestedJavaListenerObserved === true,
        gridNestedJavaRootCloseObserved:
          marker.gridNestedJavaRootCloseObserved === true,
        gridNestedClosedObserved:
          marker.gridNestedClosedObserved === true,
        gridNestedFocusRestoredObserved:
          marker.gridNestedFocusRestoredObserved === true,
        gridNestedEscapeRootOpenedObserved:
          marker.gridNestedEscapeRootOpenedObserved === true,
        gridNestedEscapeRootInitialFocusObserved:
          marker.gridNestedEscapeRootInitialFocusObserved === true,
        gridNestedEscapeJavaRootOpenObserved:
          marker.gridNestedEscapeJavaRootOpenObserved === true,
        gridNestedEscapeFirstMoveObserved:
          marker.gridNestedEscapeFirstMoveObserved === true,
        gridNestedEscapeParentFocusObserved:
          marker.gridNestedEscapeParentFocusObserved === true,
        gridNestedEscapeChildOpenedObserved:
          marker.gridNestedEscapeChildOpenedObserved === true,
        gridNestedEscapeChildCloseObserved:
          marker.gridNestedEscapeChildCloseObserved === true,
        gridNestedEscapeParentRemainsOpenObserved:
          marker.gridNestedEscapeParentRemainsOpenObserved === true,
        gridNestedEscapeParentFocusRestoredObserved:
          marker.gridNestedEscapeParentFocusRestoredObserved === true,
        gridNestedEscapeRootCloseObserved:
          marker.gridNestedEscapeRootCloseObserved === true,
        gridNestedEscapeJavaRootCloseObserved:
          marker.gridNestedEscapeJavaRootCloseObserved === true,
        gridNestedEscapeOwnerFocusRestoredObserved:
          marker.gridNestedEscapeOwnerFocusRestoredObserved === true,
        gridPolicyHeaderPointerTrustedObserved:
          marker.gridPolicyHeaderPointerTrustedObserved === true,
        gridPolicyHeaderContextObserved:
          marker.gridPolicyHeaderContextObserved === true,
        gridPolicyHeaderJavaRejectObserved:
          marker.gridPolicyHeaderJavaRejectObserved === true,
        gridPolicyHeaderClosedObserved:
          marker.gridPolicyHeaderClosedObserved === true,
        gridPolicyEmptyPointerTrustedObserved:
          marker.gridPolicyEmptyPointerTrustedObserved === true,
        gridPolicyEmptyGeometryObserved:
          marker.gridPolicyEmptyGeometryObserved === true,
        gridPolicyEmptyContextObserved:
          marker.gridPolicyEmptyContextObserved === true,
        gridPolicyEmptyJavaRejectObserved:
          marker.gridPolicyEmptyJavaRejectObserved === true,
        gridPolicyEmptyClosedObserved:
          marker.gridPolicyEmptyClosedObserved === true,
        gridPolicyRowPointerTrustedObserved:
          marker.gridPolicyRowPointerTrustedObserved === true,
        gridPolicyRowContextObserved:
          marker.gridPolicyRowContextObserved === true,
        gridPolicyRowOpenedObserved:
          marker.gridPolicyRowOpenedObserved === true,
        gridPolicyRowInitialFocusObserved:
          marker.gridPolicyRowInitialFocusObserved === true,
        gridPolicyDynamicContentObserved:
          marker.gridPolicyDynamicContentObserved === true,
        gridPolicyJavaOpenObserved:
          marker.gridPolicyJavaOpenObserved === true,
        gridPolicyJavaListenerObserved:
          marker.gridPolicyJavaListenerObserved === true,
        gridPolicyJavaCloseObserved:
          marker.gridPolicyJavaCloseObserved === true,
        gridPolicyClosedObserved:
          marker.gridPolicyClosedObserved === true,
        gridPolicyFocusRestoredObserved:
          marker.gridPolicyFocusRestoredObserved === true,
        nativeKeyboardObserved:
          marker.nativeKeyboardObserved === true,
        nativeKeyboardTrustedObserved:
          marker.nativeKeyboardTrustedObserved === true,
        nativeKeyboardCurrentObserved:
          marker.nativeKeyboardCurrentObserved === true,
        nativeKeyboardEventCount:
          Array.isArray(marker.nativeKeyboardSequence)
            ? marker.nativeKeyboardSequence.length
            : 0,
        nativeKeyboardExpected:
          String(marker.nativeKeyboardExpected || ''),
        nativeKeyboardLastKey:
          String(marker.nativeKeyboardLastKey || ''),
        nativeKeyboardLastTarget:
          String(marker.nativeKeyboardLastTarget || ''),
        nativeKeyboardSequence:
          Array.isArray(marker.nativeKeyboardSequence)
            ? marker.nativeKeyboardSequence.join(',')
            : '',
        nativePointerObserved:
          marker.nativePointerObserved === true,
        nativePointerTrustedObserved:
          marker.nativePointerTrustedObserved === true,
        nativePointerEventCount:
          Number(marker.nativePointerEventCount || 0),
        nativePointerLastTarget:
          String(marker.nativePointerLastTarget || ''),
        nativePointerLastButton:
          Number(marker.nativePointerLastButton ?? -1),
        menuOpenedCurrent,
        menuOwnerExpanded:
          String(menuOwner?.getAttribute?.('aria-expanded') || ''),
        menuFocusedText,
        nestedMenuStage,
        nestedMenuStages:
          Array.isArray(marker.nestedMenuStages)
            ? marker.nestedMenuStages.join(',')
            : '',
        nestedOpenedCurrent,
        nestedParentExpanded:
          String(nestedParent?.getAttribute?.('aria-expanded') || ''),
        nestedRootFocusedText,
        nestedFocusedText,
        nestedStatus,
        nestedDiagnostics,
        overflowStage,
        overflowStages:
          Array.isArray(marker.overflowStages)
            ? marker.overflowStages.join(',')
            : '',
        overflowOpenedCurrent,
        overflowCollapsedItemCount: overflowChildren.length,
        overflowHiddenButtonCount: overflowHiddenButtons.length,
        overflowExpectedItemId,
        overflowFocusedText,
        overflowStatus,
        overflowDiagnostics,
        contextOpenedCurrent,
        contextFocusedText,
        contextNestedStage,
        contextNestedStages:
          Array.isArray(marker.contextNestedStages)
            ? marker.contextNestedStages.join(',')
            : '',
        contextNestedOpenedCurrent,
        contextNestedRootFocusedText,
        contextNestedFocusedText,
        contextNestedStatus,
        contextNestedDiagnostics,
        gridOpenedCurrent,
        gridFocusedText,
        gridNestedStage,
        gridNestedStages:
          Array.isArray(marker.gridNestedStages)
            ? marker.gridNestedStages.join(',')
            : '',
        gridNestedOpenedCurrent,
        gridNestedRootFocusedText,
        gridNestedFocusedText,
        gridNestedStatus,
        gridNestedDiagnostics,
        gridPolicyStage,
        gridPolicyStages:
          Array.isArray(marker.gridPolicyStages)
            ? marker.gridPolicyStages.join(',')
            : '',
        gridPolicyOpenedCurrent,
        gridPolicyFocusedText,
        gridPolicyStatus,
        gridPolicyLifecycleStatus,
        gridPolicyActionStatus,
        gridPolicyDiagnostics,
        menuStatus,
        contextStatus,
        contextLifecycleStatus,
        gridOpenStatus,
        gridActionStatus,
        gridLifecycleStatus,
        gridPointerTargetText:
          String(marker.gridPointerTargetText || ''),
        gridRenderedItemCount,
        activePath:
          activePath.map((element) =>
            element.id || element.localName || '').join('>'),
        markerError: String(marker.error || ''),
        browserError:
          String(diagnostics.standardBundleBrowserError || ''),
        requestError:
          String(diagnostics.standardBundleOpaqueRequestError || ''),
        responseError:
          String(diagnostics.standardBundleOpaqueResponseError || ''),
        pushError:
          String(diagnostics.standardBundlePushError || '')
      });
    })()
    """
}
