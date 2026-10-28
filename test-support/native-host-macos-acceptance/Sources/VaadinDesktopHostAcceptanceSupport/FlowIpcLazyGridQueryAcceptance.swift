import AppKit
import Foundation

extension FlowIpcAcceptanceRuntimeObserver {
    func scheduleLazyGridQueryAcceptance(
        host: WebViewHost,
        context: FlowIpcWindowContext,
        timeoutSeconds: Double
    ) {
        FlowIpcLazyGridQueryAcceptance(
            context: context,
            deadline: Date().addingTimeInterval(timeoutSeconds)
        ).start()
    }
}

private final class FlowIpcLazyGridQueryAcceptance {
    private enum Phase {
        case ready
        case dedicatedRoute
        case farViewport
        case firstSelection
        case secondSelection
        case refreshItem
        case returnViewport
        case sort
        case filter
        case refreshAllOffViewport
        case refreshAll
        case refreshAllRehydration
        case filterReset
        case deselection
        case finalViewport
        case unknownInitialEstimate
        case unknownEstimateGrowth
        case unknownExactEnd
        case unknownTailSelection
        case unknownMutationResetTop
        case unknownMutationRefreshAll
        case unknownMutationRegrowth
        case unknownMutationSelectionRehydration
        case unknownMutationSecondGrowth
        case unknownMutationExactEnd
        case unknownShrinkResetTop
        case unknownShrinkRefreshAll
        case unknownShrinkRegrowth
        case unknownShrinkSelectionRehydration
        case unknownShrinkExactEnd
    }

    private let context: FlowIpcWindowContext
    private let deadline: Date
    private var phase = Phase.ready
    private var farViewportObserved = false
    private var firstSelectionObserved = false
    private var secondSelectionObserved = false
    private var refreshItemObserved = false
    private var refreshUidlRoundTripObserved = false
    private var refreshWithoutSelectionEventObserved = false
    private var returnViewportObserved = false
    private var sortObserved = false
    private var filterObserved = false
    private var refreshAllOffViewportObserved = false
    private var refreshAllObserved = false
    private var refreshAllRemovedRowObserved = false
    private var refreshAllUidlRoundTripObserved = false
    private var refreshAllWithoutSelectionEventObserved = false
    private var refreshAllFetchAndCountAdvancedObserved = false
    private var refreshAllRehydrationObserved = false
    private var filterResetObserved = false
    private var deselectionObserved = false
    private var finalViewportObserved = false
    private var unknownFetchOnlyObserved = false
    private var unknownInitialEstimatedObserved = false
    private var unknownEstimateGrowthObserved = false
    private var unknownFetchCrossedInitialEstimateObserved = false
    private var unknownTerminalShortFetchObserved = false
    private var unknownExactEndObserved = false
    private var unknownTailRenderedObserved = false
    private var unknownTailSelectionObserved = false
    private var unknownSelectionUidlRoundTripObserved = false
    private var unknownInitialCyclePreservedObserved = false
    private var unknownMutationResetTopObserved = false
    private var unknownMutationRefreshAllObserved = false
    private var unknownMutationResetEstimateObserved = false
    private var unknownMutationRegrowthObserved = false
    private var unknownMutationCrossedPriorExactObserved = false
    private var unknownMutationSelectionRehydratedObserved = false
    private var unknownMutationSecondGrowthObserved = false
    private var unknownMutationTerminalShortFetchObserved = false
    private var unknownMutationExactEndObserved = false
    private var unknownMutationTailRenderedObserved = false
    private var unknownMutationSelectionRetainedObserved = false
    private var unknownMutationWithoutSelectionEventObserved = false
    private var unknownMutationUidlRoundTripObserved = false
    private var unknownShrinkResetTopObserved = false
    private var unknownShrinkRefreshAllObserved = false
    private var unknownShrinkResetEstimateObserved = false
    private var unknownShrinkRegrowthObserved = false
    private var unknownShrinkSelectionRehydratedObserved = false
    private var unknownShrinkUidlRoundTripObserved = false
    private var unknownInitialEstimate = 0
    private var unknownEstimateIncrease = 0
    private var unknownMaxEstimatedCount = 0
    private var unknownExactCount = 0
    private var unknownClientSize = 0
    private var unknownTerminalReturned = 0
    private var unknownTerminalLimit = 0
    private var unknownMutationResetEstimate = 0
    private var unknownMutationMaxEstimatedCount = 0
    private var unknownMutationExactCount = 0
    private var unknownMutationClientSize = 0
    private var unknownMutationTerminalReturned = 0
    private var unknownMutationTerminalLimit = 0
    private var unknownInitialConfigStatus = ""
    private var unknownInitialFetchStatus = ""
    private var unknownInitialCountStatus = ""
    private var unknownInitialSelectionStatus = ""
    private var unknownMutationStatus = ""
    private var unknownMutationFetchStatus = ""
    private var unknownMutationCountStatus = ""
    private var unknownMutationSelectionStatus = ""
    private var selectionUidlRoundTripCount = 0
    private var pointerRequestBaseline = 0
    private var pointerResponseBaseline = 0
    private var filteredQueryStatus = ""
    private var filteredCountStatus = ""
    private var filteredSelectionStatus = ""
    private var refreshStatus = ""
    private var refreshAllStatus = ""
    private var refreshAllQueryStatus = ""
    private var refreshAllCountStatus = ""
    private var refreshAllFetchBaseline = 0
    private var refreshAllCountBaseline = 0
    private var refreshAllFetchCount = 0
    private var refreshAllCountCount = 0
    private var resetQueryStatus = ""
    private var resetCountStatus = ""
    private var resetSelectionStatus = ""
    private var knownFinalSnapshot: Snapshot?

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
                self.retryOrFail(code: "lazyGridQueryInspectionFailed")
                return
            }
            guard snapshot.errorFree else {
                self.fail(
                    code: "lazyGridQueryBrowserRuntimeError",
                    details: snapshot.tracePayload)
                return
            }

            switch self.phase {
            case .ready:
                guard snapshot.ready else {
                    self.retryOrFail(
                        code: "lazyGridQueryReadyTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.perform(
                    Self.navigationAction,
                    next: .dedicatedRoute,
                    failureCode: "lazyGridQueryNavigationFailed")
            case .dedicatedRoute:
                guard snapshot.dedicatedRouteReady else {
                    self.retryOrFail(
                        code: "lazyGridQueryDedicatedRouteTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.perform(
                    Self.farViewportAction,
                    next: .farViewport,
                    failureCode: "lazyGridQueryFarViewportDispatchFailed")
            case .farViewport:
                guard snapshot.farViewportObserved else {
                    self.retryOrFail(
                        code: "lazyGridQueryFarViewportTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.farViewportObserved = true
                self.performNativePrimaryClick(
                    label: "L-061",
                    stage: "select-L-061",
                    next: .firstSelection,
                    failureCode: "lazyGridQueryFirstSelectionDispatchFailed")
            case .firstSelection:
                guard snapshot.firstClientSelectionObserved,
                      snapshot.nativePointerEventCount == 1,
                      snapshot.nativePointerTrustedEventCount == 1,
                      self.pointerRoundTripObserved(in: snapshot) else {
                    self.retryOrFail(
                        code: "lazyGridQueryFirstSelectionTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.firstSelectionObserved = true
                self.selectionUidlRoundTripCount += 1
                self.performNativePrimaryClick(
                    label: "L-060",
                    stage: "select-L-060",
                    next: .secondSelection,
                    failureCode: "lazyGridQuerySecondSelectionDispatchFailed")
            case .secondSelection:
                guard snapshot.secondClientSelectionObserved,
                      snapshot.multiSelectionDomObserved,
                      snapshot.nativePointerEventCount == 2,
                      snapshot.nativePointerTrustedEventCount == 2,
                      self.pointerRoundTripObserved(in: snapshot) else {
                    self.retryOrFail(
                        code: "lazyGridQuerySecondSelectionTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.secondSelectionObserved = true
                self.selectionUidlRoundTripCount += 1
                self.performNativePrimaryClick(
                    label: "refresh-L-060",
                    stage: "refresh-L-060",
                    preparationScript: Self.prepareRefreshPointerScript,
                    next: .refreshItem,
                    failureCode: "lazyGridQueryRefreshDispatchFailed")
            case .refreshItem:
                guard snapshot.refreshItemObserved,
                      snapshot.nativePointerEventCount == 3,
                      snapshot.nativePointerTrustedEventCount == 3,
                      self.pointerRoundTripObserved(in: snapshot) else {
                    self.retryOrFail(
                        code: "lazyGridQueryRefreshTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.refreshItemObserved = true
                self.refreshUidlRoundTripObserved = true
                self.refreshWithoutSelectionEventObserved = true
                self.refreshStatus = snapshot.refreshStatus
                self.perform(
                    Self.returnViewportAction,
                    next: .returnViewport,
                    failureCode: "lazyGridQueryReturnViewportDispatchFailed")
            case .returnViewport:
                guard snapshot.returnViewportObserved else {
                    self.retryOrFail(
                        code: "lazyGridQueryReturnViewportTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.returnViewportObserved = true
                self.perform(
                    Self.sortAction,
                    next: .sort,
                    failureCode: "lazyGridQuerySortDispatchFailed")
            case .sort:
                guard snapshot.sortObserved else {
                    self.retryOrFail(
                        code: "lazyGridQuerySortTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.sortObserved = true
                self.perform(
                    Self.filterAction,
                    next: .filter,
                    failureCode: "lazyGridQueryFilterDispatchFailed")
            case .filter:
                guard snapshot.filterObserved else {
                    self.retryOrFail(
                        code: "lazyGridQueryFilterTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.filterObserved = true
                self.filteredQueryStatus = snapshot.queryStatus
                self.filteredCountStatus = snapshot.countStatus
                self.filteredSelectionStatus = snapshot.selectionStatus
                self.perform(
                    Self.refreshAllOffViewportAction,
                    next: .refreshAllOffViewport,
                    failureCode: "lazyGridQueryRefreshAllOffViewportDispatchFailed")
            case .refreshAllOffViewport:
                guard snapshot.refreshAllOffViewportObserved else {
                    self.retryOrFail(
                        code: "lazyGridQueryRefreshAllOffViewportTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.refreshAllOffViewportObserved = true
                self.refreshAllFetchBaseline = snapshot.queryCallCount
                self.refreshAllCountBaseline = snapshot.countCallCount
                self.performNativePrimaryClick(
                    label: "refresh-all-L-003",
                    stage: "refresh-all-remove-L-003",
                    preparationScript: Self.prepareRefreshAllPointerScript,
                    next: .refreshAll,
                    failureCode: "lazyGridQueryRefreshAllDispatchFailed")
            case .refreshAll:
                let fetchAndCountAdvanced =
                    snapshot.queryCallCount > self.refreshAllFetchBaseline
                    && snapshot.countCallCount > self.refreshAllCountBaseline
                guard snapshot.refreshAllObserved,
                      snapshot.refreshAllRemovedRowObserved,
                      fetchAndCountAdvanced,
                      snapshot.nativePointerEventCount == 4,
                      snapshot.nativePointerTrustedEventCount == 4,
                      self.pointerRoundTripObserved(in: snapshot) else {
                    self.retryOrFail(
                        code: "lazyGridQueryRefreshAllTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.refreshAllObserved = true
                self.refreshAllRemovedRowObserved = true
                self.refreshAllUidlRoundTripObserved = true
                self.refreshAllWithoutSelectionEventObserved = true
                self.refreshAllFetchAndCountAdvancedObserved = true
                self.refreshAllStatus = snapshot.refreshAllStatus
                self.refreshAllQueryStatus = snapshot.queryStatus
                self.refreshAllCountStatus = snapshot.countStatus
                self.refreshAllFetchCount = snapshot.queryCallCount
                self.refreshAllCountCount = snapshot.countCallCount
                self.perform(
                    Self.refreshAllRehydrationAction,
                    next: .refreshAllRehydration,
                    failureCode: "lazyGridQueryRefreshAllRehydrationDispatchFailed")
            case .refreshAllRehydration:
                guard snapshot.refreshAllRehydrationObserved else {
                    self.retryOrFail(
                        code: "lazyGridQueryRefreshAllRehydrationTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.refreshAllRehydrationObserved = true
                self.perform(
                    Self.filterResetAction,
                    next: .filterReset,
                    failureCode: "lazyGridQueryFilterResetDispatchFailed")
            case .filterReset:
                guard snapshot.filterResetObserved else {
                    self.retryOrFail(
                        code: "lazyGridQueryFilterResetTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.filterResetObserved = true
                self.resetQueryStatus = snapshot.queryStatus
                self.resetCountStatus = snapshot.countStatus
                self.resetSelectionStatus = snapshot.selectionStatus
                self.performNativePrimaryClick(
                    label: "L-061",
                    stage: "deselect-L-061",
                    next: .deselection,
                    failureCode: "lazyGridQueryDeselectionDispatchFailed")
            case .deselection:
                guard snapshot.clientDeselectionObserved,
                      snapshot.nativePointerEventCount == 5,
                      snapshot.nativePointerTrustedEventCount == 5,
                      self.pointerRoundTripObserved(in: snapshot) else {
                    self.retryOrFail(
                        code: "lazyGridQueryDeselectionTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.deselectionObserved = true
                self.selectionUidlRoundTripCount += 1
                self.perform(
                    Self.finalViewportAction,
                    next: .finalViewport,
                    failureCode: "lazyGridQueryFinalViewportDispatchFailed")
            case .finalViewport:
                guard snapshot.finalViewportObserved else {
                    self.retryOrFail(
                        code: "lazyGridQueryFinalViewportTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.finalViewportObserved = true
                self.knownFinalSnapshot = snapshot
                self.perform(
                    Self.positionUnknownGridAction,
                    next: .unknownInitialEstimate,
                    failureCode: "lazyGridUnknownSizePositionDispatchFailed")
            case .unknownInitialEstimate:
                guard snapshot.unknownFetchOnlyObserved,
                      snapshot.unknownInitialEstimatedObserved,
                      snapshot.unknownClientSize == 40 else {
                    self.retryOrFail(
                        code: "lazyGridUnknownSizeInitialEstimateTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.unknownFetchOnlyObserved = true
                self.unknownInitialEstimatedObserved = true
                self.unknownInitialEstimate = snapshot.unknownInitialEstimate
                self.unknownEstimateIncrease = snapshot.unknownEstimateIncrease
                self.perform(
                    Self.unknownEstimateGrowthAction,
                    next: .unknownEstimateGrowth,
                    failureCode: "lazyGridUnknownSizeGrowthDispatchFailed")
            case .unknownEstimateGrowth:
                guard snapshot.unknownEstimateGrowthObserved,
                      snapshot.unknownFetchCrossedInitialEstimateObserved,
                      snapshot.unknownClientSize >= 80 else {
                    self.retryOrFail(
                        code: "lazyGridUnknownSizeGrowthTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.unknownEstimateGrowthObserved = true
                self.unknownFetchCrossedInitialEstimateObserved = true
                self.unknownMaxEstimatedCount = snapshot.unknownMaxEstimatedCount
                self.perform(
                    Self.unknownExactEndAction,
                    next: .unknownExactEnd,
                    failureCode: "lazyGridUnknownSizeEndDispatchFailed")
            case .unknownExactEnd:
                guard snapshot.unknownTerminalShortFetchObserved,
                      snapshot.unknownExactEndObserved,
                      snapshot.unknownTailRenderedObserved,
                      snapshot.unknownClientSize == 67 else {
                    self.retryOrFail(
                        code: "lazyGridUnknownSizeEndTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.unknownTerminalShortFetchObserved = true
                self.unknownExactEndObserved = true
                self.unknownTailRenderedObserved = true
                self.unknownMaxEstimatedCount = max(
                    self.unknownMaxEstimatedCount,
                    snapshot.unknownMaxEstimatedCount)
                self.unknownExactCount = snapshot.unknownExactCount
                self.unknownClientSize = snapshot.unknownClientSize
                self.unknownTerminalReturned = snapshot.unknownTerminalReturned
                self.unknownTerminalLimit = snapshot.unknownTerminalLimit
                self.performNativePrimaryClick(
                    label: "U-067",
                    stage: "select-U-067",
                    preparationScript: Self.prepareUnknownTailPointerScript,
                    next: .unknownTailSelection,
                    failureCode: "lazyGridUnknownSizeSelectionDispatchFailed")
            case .unknownTailSelection:
                guard snapshot.unknownTailSelectionObserved,
                      snapshot.nativePointerEventCount == 6,
                      snapshot.nativePointerTrustedEventCount == 6,
                      self.pointerRoundTripObserved(in: snapshot) else {
                    self.retryOrFail(
                        code: "lazyGridUnknownSizeSelectionTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.unknownTailSelectionObserved = true
                self.unknownSelectionUidlRoundTripObserved = true
                self.unknownInitialCyclePreservedObserved =
                    snapshot.unknownInitialCycleObserved
                self.unknownInitialConfigStatus = snapshot.unknownConfigStatus
                self.unknownInitialFetchStatus = snapshot.unknownFetchStatus
                self.unknownInitialCountStatus = snapshot.unknownCountStatus
                self.unknownInitialSelectionStatus =
                    snapshot.unknownSelectionStatus
                self.perform(
                    Self.unknownMutationResetTopAction,
                    next: .unknownMutationResetTop,
                    failureCode:
                        "lazyGridUnknownSizeMutationResetTopDispatchFailed")
            case .unknownMutationResetTop:
                guard snapshot.unknownMutationResetTopObserved else {
                    self.retryOrFail(
                        code: "lazyGridUnknownSizeMutationResetTopTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.unknownMutationResetTopObserved = true
                self.performNativePrimaryClick(
                    label: "refresh-all-unknown-size",
                    stage: "refresh-all-unknown-append",
                    preparationScript:
                        Self.prepareUnknownMutationPointerScript,
                    next: .unknownMutationRefreshAll,
                    failureCode:
                        "lazyGridUnknownSizeMutationRefreshAllDispatchFailed")
            case .unknownMutationRefreshAll:
                guard snapshot.unknownMutationRefreshAllObserved,
                      snapshot.unknownMutationResetEstimateObserved,
                      snapshot.nativePointerEventCount == 7,
                      snapshot.nativePointerTrustedEventCount == 7,
                      self.pointerRoundTripObserved(in: snapshot) else {
                    self.retryOrFail(
                        code: "lazyGridUnknownSizeMutationResetTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.unknownMutationRefreshAllObserved = true
                self.unknownMutationResetEstimateObserved = true
                self.unknownMutationSelectionRetainedObserved = true
                self.unknownMutationWithoutSelectionEventObserved = true
                self.unknownMutationUidlRoundTripObserved = true
                self.unknownMutationResetEstimate =
                    snapshot.unknownMutationResetEstimate
                self.unknownMutationStatus = snapshot.unknownMutationStatus
                self.perform(
                    Self.unknownMutationRegrowthAction,
                    next: .unknownMutationRegrowth,
                    failureCode:
                        "lazyGridUnknownSizeMutationRegrowthDispatchFailed")
            case .unknownMutationRegrowth:
                guard snapshot.unknownMutationRegrowthObserved else {
                    self.retryOrFail(
                        code: "lazyGridUnknownSizeMutationRegrowthTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.unknownMutationRegrowthObserved = true
                self.unknownMutationMaxEstimatedCount = max(
                    self.unknownMutationMaxEstimatedCount,
                    snapshot.unknownMutationMaxEstimatedCount)
                self.perform(
                    Self.unknownMutationSelectionRehydrationAction,
                    next: .unknownMutationSelectionRehydration,
                    failureCode:
                        "lazyGridUnknownSizeMutationSelectionDispatchFailed")
            case .unknownMutationSelectionRehydration:
                guard snapshot.unknownMutationCrossedPriorExactObserved,
                      snapshot.unknownMutationSelectionRehydratedObserved else {
                    self.retryOrFail(
                        code:
                            "lazyGridUnknownSizeMutationSelectionRehydrationTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.unknownMutationCrossedPriorExactObserved = true
                self.unknownMutationSelectionRehydratedObserved = true
                self.unknownMutationSelectionRetainedObserved = true
                self.unknownMutationMaxEstimatedCount = max(
                    self.unknownMutationMaxEstimatedCount,
                    snapshot.unknownMutationMaxEstimatedCount)
                self.perform(
                    Self.unknownMutationSecondGrowthAction,
                    next: .unknownMutationSecondGrowth,
                    failureCode:
                        "lazyGridUnknownSizeMutationSecondGrowthDispatchFailed")
            case .unknownMutationSecondGrowth:
                guard snapshot.unknownMutationSecondGrowthObserved else {
                    self.retryOrFail(
                        code: "lazyGridUnknownSizeMutationSecondGrowthTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.unknownMutationSecondGrowthObserved = true
                self.unknownMutationMaxEstimatedCount = max(
                    self.unknownMutationMaxEstimatedCount,
                    snapshot.unknownMutationMaxEstimatedCount)
                self.perform(
                    Self.unknownMutationExactEndAction,
                    next: .unknownMutationExactEnd,
                    failureCode:
                        "lazyGridUnknownSizeMutationEndDispatchFailed")
            case .unknownMutationExactEnd:
                guard snapshot.unknownInitialCyclePreservedObserved,
                      snapshot.unknownMutationTerminalShortFetchObserved,
                      snapshot.unknownMutationExactEndObserved,
                      snapshot.unknownMutationTailRenderedObserved,
                      snapshot.unknownMutationSelectionRetainedObserved else {
                    self.retryOrFail(
                        code: "lazyGridUnknownSizeMutationEndTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.unknownInitialCyclePreservedObserved = true
                self.unknownMutationTerminalShortFetchObserved = true
                self.unknownMutationExactEndObserved = true
                self.unknownMutationTailRenderedObserved = true
                self.unknownMutationSelectionRetainedObserved = true
                self.unknownMutationMaxEstimatedCount = max(
                    self.unknownMutationMaxEstimatedCount,
                    snapshot.unknownMutationMaxEstimatedCount)
                self.unknownMutationExactCount = snapshot.unknownExactCount
                self.unknownMutationClientSize = snapshot.unknownClientSize
                self.unknownMutationTerminalReturned =
                    snapshot.unknownMutationTerminalReturned
                self.unknownMutationTerminalLimit =
                    snapshot.unknownMutationTerminalLimit
                self.unknownMutationStatus = snapshot.unknownMutationStatus
                self.unknownMutationFetchStatus = snapshot.unknownFetchStatus
                self.unknownMutationCountStatus = snapshot.unknownCountStatus
                self.unknownMutationSelectionStatus =
                    snapshot.unknownSelectionStatus
                self.perform(
                    Self.unknownShrinkResetTopAction,
                    next: .unknownShrinkResetTop,
                    failureCode: "lazyGridUnknownSizeShrinkResetTopDispatchFailed")
            case .unknownShrinkResetTop:
                guard snapshot.unknownShrinkResetTopObserved else {
                    self.retryOrFail(
                        code: "lazyGridUnknownSizeShrinkResetTopTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.unknownShrinkResetTopObserved = true
                self.performNativePrimaryClick(
                    label: "shrink-unknown-size",
                    stage: "refresh-all-unknown-shrink",
                    preparationScript: Self.prepareUnknownShrinkPointerScript,
                    next: .unknownShrinkRefreshAll,
                    failureCode: "lazyGridUnknownSizeShrinkRefreshAllDispatchFailed")
            case .unknownShrinkRefreshAll:
                guard snapshot.unknownShrinkRefreshAllObserved,
                      snapshot.unknownShrinkResetEstimateObserved,
                      snapshot.unknownShrinkFetchAdvancedObserved,
                      snapshot.unknownShrinkCountEventAdvancedObserved,
                      snapshot.nativePointerEventCount == 8,
                      snapshot.nativePointerTrustedEventCount == 8,
                      self.pointerRoundTripObserved(in: snapshot) else {
                    self.retryOrFail(
                        code: "lazyGridUnknownSizeShrinkResetTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.unknownShrinkRefreshAllObserved = true
                self.unknownShrinkResetEstimateObserved = true
                self.unknownShrinkUidlRoundTripObserved = true
                self.perform(
                    Self.unknownShrinkRegrowthAction,
                    next: .unknownShrinkRegrowth,
                    failureCode: "lazyGridUnknownSizeShrinkRegrowthDispatchFailed")
            case .unknownShrinkRegrowth:
                guard snapshot.unknownShrinkRegrowthObserved else {
                    self.retryOrFail(
                        code: "lazyGridUnknownSizeShrinkRegrowthTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.unknownShrinkRegrowthObserved = true
                self.perform(
                    Self.unknownShrinkSelectionRehydrationAction,
                    next: .unknownShrinkSelectionRehydration,
                    failureCode: "lazyGridUnknownSizeShrinkSelectionDispatchFailed")
            case .unknownShrinkSelectionRehydration:
                guard snapshot.unknownShrinkSelectionRehydratedObserved,
                      snapshot.unknownShrinkRemovedTailAbsentObserved,
                      snapshot.unknownShrinkFetchAdvancedObserved else {
                    self.retryOrFail(
                        code: "lazyGridUnknownSizeShrinkSelectionRehydrationTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.unknownShrinkSelectionRehydratedObserved = true
                self.perform(
                    Self.unknownShrinkExactEndAction,
                    next: .unknownShrinkExactEnd,
                    failureCode: "lazyGridUnknownSizeShrinkEndDispatchFailed")
            case .unknownShrinkExactEnd:
                guard snapshot.unknownShrinkTerminalShortFetchObserved,
                      snapshot.unknownShrinkExactEndObserved,
                      snapshot.unknownShrinkTailRenderedObserved,
                      snapshot.unknownShrinkRemovedTailAbsentObserved,
                      snapshot.unknownShrinkSelectionRetainedObserved,
                      snapshot.unknownShrinkFetchAdvancedObserved,
                      snapshot.unknownShrinkCountEventAdvancedObserved,
                      snapshot.unknownShrinkCountEventEpochCount >= 3 else {
                    self.retryOrFail(
                        code: "lazyGridUnknownSizeShrinkEndTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.finish(snapshot)
            }
        }
    }

    private func inspect(_ completion: @escaping (Snapshot?) -> Void) {
        context.webView.evaluateJavaScript(Self.inspectionScript) { result, error in
            guard error == nil,
                  let json = result as? String,
                  let data = json.data(using: .utf8),
                  let snapshot = try? JSONDecoder().decode(Snapshot.self, from: data) else {
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

    private func performNativePrimaryClick(
        label: String,
        stage: String,
        preparationScript: String? = nil,
        next: Phase,
        failureCode: String
    ) {
        context.webView.evaluateJavaScript(
            preparationScript
                ?? Self.prepareSelectionPointerScript(
                    label: label, stage: stage)
        ) { result, error in
            guard error == nil,
                  let json = result as? String,
                  let data = json.data(using: .utf8),
                  let geometry = try? JSONDecoder().decode(
                    PointerGeometry.self, from: data) else {
                self.retryOrFail(
                    code: failureCode,
                    details: [
                        "message": error?.localizedDescription ?? "",
                        "result": String(describing: result)
                    ])
                return
            }
            self.pointerRequestBaseline = geometry.requestCount
            self.pointerResponseBaseline = geometry.responseCount
            self.prepareWindowForNativeInput(failureCode: failureCode) {
                let webViewPoint = NSPoint(
                    x: geometry.centerX,
                    y: self.context.webView.isFlipped
                        ? geometry.centerY
                        : self.context.webView.bounds.height - geometry.centerY)
                let windowPoint = self.context.webView.convert(webViewPoint, to: nil)
                guard self.context.window.contentLayoutRect.contains(windowPoint) else {
                    self.retryOrFail(code: failureCode, details: [
                        "message": "Lazy Grid selection target is outside the host window",
                        "windowX": windowPoint.x,
                        "windowY": windowPoint.y,
                        "label": label,
                        "stage": stage
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
                        timestamp: timestamp + 0.02,
                        windowNumber: self.context.window.windowNumber,
                        context: nil,
                        eventNumber: 1,
                        clickCount: 1,
                        pressure: 0) else {
                    self.retryOrFail(code: failureCode, details: [
                        "message": "Unable to create AppKit primary pointer event",
                        "label": label,
                        "stage": stage
                    ])
                    return
                }
                trace("recv", "lazyGridQuery.nativePointerDispatch", [
                    "inputMode": "appkit-window-pointer-event",
                    "button": "primary",
                    "label": label,
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
                self.context.window.sendEvent(mouseUp)
                self.phase = next
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.65) {
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

    private func pointerRoundTripObserved(in snapshot: Snapshot) -> Bool {
        snapshot.requestCount > pointerRequestBaseline
            && snapshot.responseCount > pointerResponseBaseline
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
        let knownSnapshot = knownFinalSnapshot ?? snapshot
        let expectedNativeStages =
            "select-L-061,select-L-060,refresh-L-060,"
            + "refresh-all-remove-L-003,deselect-L-061,select-U-067,"
            + "refresh-all-unknown-append,refresh-all-unknown-shrink"
        let expectedNativeLabels =
            "L-061,L-060,refresh-L-060,refresh-all-L-003,L-061,U-067,"
            + "refresh-all-unknown-size,shrink-unknown-size"
        let expectedBrowserStages =
            "navigate,scroll-far,position-refresh-item,scroll-return,"
            + "sort-owner,filter-platform,scroll-refresh-all-offscreen,"
            + "position-refresh-all,scroll-refresh-all-selected,"
            + "filter-reset,scroll-final,position-unknown-grid,"
            + "scroll-unknown-growth,scroll-unknown-end,"
            + "scroll-unknown-reset-top,position-unknown-mutation,"
            + "scroll-unknown-regrowth,"
            + "scroll-unknown-selection-rehydrate,"
            + "scroll-unknown-second-growth,scroll-unknown-mutated-end,"
            + "scroll-unknown-shrink-reset-top,position-unknown-shrink,"
            + "scroll-unknown-shrink-regrowth,"
            + "scroll-unknown-shrink-selection-rehydrate,scroll-unknown-shrink-end"
        let nativePointerContract = snapshot.nativePointerObserved
            && snapshot.nativePointerTrustedObserved
            && snapshot.nativePointerEventCount == 8
            && snapshot.nativePointerTrustedEventCount == 8
            && snapshot.nativePointerStages == expectedNativeStages
            && snapshot.nativePointerLabels == expectedNativeLabels
        let browserSetupContract = snapshot.browserProgrammaticActionsObserved
            && snapshot.browserProgrammaticStages == expectedBrowserStages
        let complete = snapshot.errorFree
            && farViewportObserved
            && firstSelectionObserved
            && secondSelectionObserved
            && refreshItemObserved
            && refreshUidlRoundTripObserved
            && refreshWithoutSelectionEventObserved
            && returnViewportObserved
            && sortObserved
            && filterObserved
            && refreshAllOffViewportObserved
            && refreshAllObserved
            && refreshAllRemovedRowObserved
            && refreshAllUidlRoundTripObserved
            && refreshAllWithoutSelectionEventObserved
            && refreshAllFetchAndCountAdvancedObserved
            && refreshAllRehydrationObserved
            && filterResetObserved
            && deselectionObserved
            && finalViewportObserved
            && selectionUidlRoundTripCount == 3
            && unknownFetchOnlyObserved
            && unknownInitialEstimatedObserved
            && unknownEstimateGrowthObserved
            && unknownFetchCrossedInitialEstimateObserved
            && unknownTerminalShortFetchObserved
            && unknownExactEndObserved
            && unknownTailRenderedObserved
            && unknownTailSelectionObserved
            && unknownSelectionUidlRoundTripObserved
            && unknownInitialCyclePreservedObserved
            && unknownMutationResetTopObserved
            && unknownMutationRefreshAllObserved
            && unknownMutationResetEstimateObserved
            && unknownMutationRegrowthObserved
            && unknownMutationCrossedPriorExactObserved
            && unknownMutationSelectionRehydratedObserved
            && unknownMutationSecondGrowthObserved
            && unknownMutationTerminalShortFetchObserved
            && unknownMutationExactEndObserved
            && unknownMutationTailRenderedObserved
            && unknownMutationSelectionRetainedObserved
            && unknownMutationWithoutSelectionEventObserved
            && unknownMutationUidlRoundTripObserved
            && unknownInitialEstimate == 40
            && unknownEstimateIncrease == 40
            && unknownMaxEstimatedCount >= 80
            && unknownExactCount == 67
            && unknownClientSize == 67
            && unknownTerminalReturned > 0
            && unknownTerminalReturned < unknownTerminalLimit
            && unknownMutationResetEstimate == 40
            && unknownMutationMaxEstimatedCount >= 120
            && unknownMutationExactCount == 93
            && unknownMutationClientSize == 93
            && unknownMutationTerminalReturned > 0
            && unknownMutationTerminalReturned < unknownMutationTerminalLimit
            && unknownShrinkResetTopObserved
            && unknownShrinkRefreshAllObserved
            && unknownShrinkResetEstimateObserved
            && unknownShrinkRegrowthObserved
            && unknownShrinkSelectionRehydratedObserved
            && unknownShrinkUidlRoundTripObserved
            && snapshot.unknownShrinkTerminalShortFetchObserved
            && snapshot.unknownShrinkExactEndObserved
            && snapshot.unknownShrinkTailRenderedObserved
            && snapshot.unknownShrinkRemovedTailAbsentObserved
            && snapshot.unknownShrinkSelectionRetainedObserved
            && snapshot.unknownShrinkFetchAdvancedObserved
            && snapshot.unknownShrinkCountEventAdvancedObserved
            && snapshot.unknownShrinkResetEstimate == 40
            && snapshot.unknownShrinkMaxEstimatedCount >= 80
            && snapshot.unknownShrinkExactCount == 72
            && snapshot.unknownClientSize == 72
            && snapshot.unknownShrinkCountEventEpochCount >= 3
            && nativePointerContract
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
            "lazyGridFarViewportObserved": farViewportObserved,
            "lazyGridSelectionObserved": secondSelectionObserved,
            "lazyGridReturnViewportObserved": returnViewportObserved,
            "lazyGridSortObserved": sortObserved,
            "lazyGridFilterObserved": filterObserved,
            "lazyGridFilterResetObserved": filterResetObserved,
            "lazyGridSelectionColumnObserved":
                knownSnapshot.selectionColumnObserved,
            "lazyGridAriaMultiSelectableObserved":
                knownSnapshot.ariaMultiSelectableObserved,
            "lazyGridReferenceCellObserved": knownSnapshot.referenceCellObserved,
            "lazyGridReferenceRowObserved": knownSnapshot.referenceRowObserved,
            "lazyGridReferenceCheckboxObserved":
                knownSnapshot.referenceCheckboxObserved,
            "lazyGridRenderedRowCount": knownSnapshot.renderedRowCount,
            "lazyGridMultiSelectionModeObserved":
                knownSnapshot.multiSelectionModeObserved,
            "lazyGridFirstClientSelectionObserved": firstSelectionObserved,
            "lazyGridSecondClientSelectionObserved": secondSelectionObserved,
            "lazyGridMultiSelectionDomObserved": secondSelectionObserved,
            "lazyGridRefreshItemObserved": refreshItemObserved,
            "lazyGridFreshUnequalReplacementObserved": refreshItemObserved,
            "lazyGridRefreshSelectionRetainedObserved": refreshItemObserved,
            "lazyGridRefreshWithoutSelectionEventObserved":
                refreshWithoutSelectionEventObserved,
            "lazyGridRefreshUidlRoundTripObserved":
                refreshUidlRoundTripObserved,
            "lazyGridRefreshedStateRehydratedObserved":
                knownSnapshot.finalViewportObserved,
            "lazyGridRecycledRowsObserved": returnViewportObserved,
            "lazyGridSortSelectionRetainedObserved": sortObserved,
            "lazyGridFilteredSelectionRetainedObserved": filterObserved,
            "lazyGridFilteredVisibleSelectionRehydratedObserved": filterObserved,
            "lazyGridRefreshAllOffViewportObserved":
                refreshAllOffViewportObserved,
            "lazyGridRefreshAllObserved": refreshAllObserved,
            "lazyGridRefreshAllCardinalityObserved": refreshAllObserved,
            "lazyGridRefreshAllRemovedRowObserved":
                refreshAllRemovedRowObserved,
            "lazyGridRefreshAllDataChangeEventObserved": refreshAllObserved,
            "lazyGridRefreshAllSelectionRetainedObserved": refreshAllObserved,
            "lazyGridRefreshAllWithoutSelectionEventObserved":
                refreshAllWithoutSelectionEventObserved,
            "lazyGridRefreshAllUidlRoundTripObserved":
                refreshAllUidlRoundTripObserved,
            "lazyGridRefreshAllFetchAndCountAdvancedObserved":
                refreshAllFetchAndCountAdvancedObserved,
            "lazyGridRefreshAllSelectedRowRehydratedObserved":
                refreshAllRehydrationObserved,
            "lazyGridFilterResetSelectionRetainedObserved": filterResetObserved,
            "lazyGridResetVisibleSelectionRehydratedObserved": filterResetObserved,
            "lazyGridClientDeselectionObserved": deselectionObserved,
            "lazyGridFinalSelectionObserved": finalViewportObserved,
            "lazyGridSelectionUidlRoundTripsObserved":
                selectionUidlRoundTripCount == 3,
            "lazyGridSelectionUidlRoundTripCount": selectionUidlRoundTripCount,
            "lazyGridUnknownSizeFetchOnlyObserved": unknownFetchOnlyObserved,
            "lazyGridUnknownSizeInitialEstimatedObserved":
                unknownInitialEstimatedObserved,
            "lazyGridUnknownSizeEstimateGrowthObserved":
                unknownEstimateGrowthObserved,
            "lazyGridUnknownSizeFetchCrossedInitialEstimateObserved":
                unknownFetchCrossedInitialEstimateObserved,
            "lazyGridUnknownSizeTerminalShortFetchObserved":
                unknownTerminalShortFetchObserved,
            "lazyGridUnknownSizeExactEndObserved": unknownExactEndObserved,
            "lazyGridUnknownSizeTailRenderedObserved":
                unknownTailRenderedObserved,
            "lazyGridUnknownSizeTailSelectionObserved":
                unknownTailSelectionObserved,
            "lazyGridUnknownSizeSelectionUidlRoundTripObserved":
                unknownSelectionUidlRoundTripObserved,
            "lazyGridUnknownSizeInitialCyclePreservedObserved":
                unknownInitialCyclePreservedObserved,
            "lazyGridUnknownSizeInitialEstimate": unknownInitialEstimate,
            "lazyGridUnknownSizeEstimateIncrease": unknownEstimateIncrease,
            "lazyGridUnknownSizeMaxEstimatedCount": unknownMaxEstimatedCount,
            "lazyGridUnknownSizeExactCount": unknownExactCount,
            "lazyGridUnknownSizeClientSize": unknownClientSize,
            "lazyGridUnknownSizeTerminalReturned": unknownTerminalReturned,
            "lazyGridUnknownSizeTerminalLimit": unknownTerminalLimit,
            "lazyGridUnknownSizeJavaSelectionChangeCount": 1,
            "lazyGridUnknownSizeConfigStatus": unknownInitialConfigStatus,
            "lazyGridUnknownSizeFetchStatus": unknownInitialFetchStatus,
            "lazyGridUnknownSizeCountStatus": unknownInitialCountStatus,
            "lazyGridUnknownSizeSelectionStatus":
                unknownInitialSelectionStatus,
            "lazyGridUnknownSizeTailId": "U-067",
            "lazyGridUnknownSizeMutationResetTopObserved":
                unknownMutationResetTopObserved,
            "lazyGridUnknownSizeMutationRefreshAllObserved":
                unknownMutationRefreshAllObserved,
            "lazyGridUnknownSizeMutationDataChangeEventObserved":
                unknownMutationRefreshAllObserved,
            "lazyGridUnknownSizeMutationNoDataRefreshEventObserved":
                unknownMutationRefreshAllObserved,
            "lazyGridUnknownSizeMutationResetEstimateObserved":
                unknownMutationResetEstimateObserved,
            "lazyGridUnknownSizeMutationRegrowthObserved":
                unknownMutationRegrowthObserved,
            "lazyGridUnknownSizeMutationCrossedPriorExactObserved":
                unknownMutationCrossedPriorExactObserved,
            "lazyGridUnknownSizeMutationSelectionRehydratedObserved":
                unknownMutationSelectionRehydratedObserved,
            "lazyGridUnknownSizeMutationSecondGrowthObserved":
                unknownMutationSecondGrowthObserved,
            "lazyGridUnknownSizeMutationTerminalShortFetchObserved":
                unknownMutationTerminalShortFetchObserved,
            "lazyGridUnknownSizeMutationExactEndObserved":
                unknownMutationExactEndObserved,
            "lazyGridUnknownSizeMutationTailRenderedObserved":
                unknownMutationTailRenderedObserved,
            "lazyGridUnknownSizeMutationSelectionRetainedObserved":
                unknownMutationSelectionRetainedObserved,
            "lazyGridUnknownSizeMutationWithoutSelectionEventObserved":
                unknownMutationWithoutSelectionEventObserved,
            "lazyGridUnknownSizeMutationUidlRoundTripObserved":
                unknownMutationUidlRoundTripObserved,
            "lazyGridUnknownSizeMutationResetEstimate":
                unknownMutationResetEstimate,
            "lazyGridUnknownSizeMutationMaxEstimatedCount":
                unknownMutationMaxEstimatedCount,
            "lazyGridUnknownSizeMutationExactCount":
                unknownMutationExactCount,
            "lazyGridUnknownSizeMutationClientSize":
                unknownMutationClientSize,
            "lazyGridUnknownSizeMutationTerminalReturned":
                unknownMutationTerminalReturned,
            "lazyGridUnknownSizeMutationTerminalLimit":
                unknownMutationTerminalLimit,
            "lazyGridUnknownSizeMutationJavaSelectionChangeCount": 1,
            "lazyGridUnknownSizeMutationChangeCount": 1,
            "lazyGridUnknownSizeMutationStatus": unknownMutationStatus,
            "lazyGridUnknownSizeMutationFetchStatus":
                unknownMutationFetchStatus,
            "lazyGridUnknownSizeMutationCountStatus":
                unknownMutationCountStatus,
            "lazyGridUnknownSizeMutationSelectionStatus":
                unknownMutationSelectionStatus,
            "lazyGridUnknownSizeMutationSelectedId": "U-067",
            "lazyGridUnknownSizeMutationTailId": "U-093",
            "lazyGridUnknownSizeShrinkResetTopObserved": unknownShrinkResetTopObserved,
            "lazyGridUnknownSizeShrinkRefreshAllObserved": unknownShrinkRefreshAllObserved,
            "lazyGridUnknownSizeShrinkDataChangeEventObserved": unknownShrinkRefreshAllObserved,
            "lazyGridUnknownSizeShrinkNoDataRefreshEventObserved": unknownShrinkRefreshAllObserved,
            "lazyGridUnknownSizeShrinkResetEstimateObserved": unknownShrinkResetEstimateObserved,
            "lazyGridUnknownSizeShrinkRegrowthObserved": unknownShrinkRegrowthObserved,
            "lazyGridUnknownSizeShrinkSelectionRehydratedObserved": unknownShrinkSelectionRehydratedObserved,
            "lazyGridUnknownSizeShrinkUidlRoundTripObserved": unknownShrinkUidlRoundTripObserved,
            "lazyGridUnknownSizeShrinkTerminalShortFetchObserved": snapshot.unknownShrinkTerminalShortFetchObserved,
            "lazyGridUnknownSizeShrinkExactEndObserved": snapshot.unknownShrinkExactEndObserved,
            "lazyGridUnknownSizeShrinkTailRenderedObserved": snapshot.unknownShrinkTailRenderedObserved,
            "lazyGridUnknownSizeShrinkRemovedTailAbsentObserved": snapshot.unknownShrinkRemovedTailAbsentObserved,
            "lazyGridUnknownSizeShrinkSelectionRetainedObserved": snapshot.unknownShrinkSelectionRetainedObserved,
            "lazyGridUnknownSizeShrinkWithoutSelectionEventObserved": snapshot.unknownShrinkSelectionRetainedObserved,
            "lazyGridUnknownSizeShrinkFetchAdvancedObserved": snapshot.unknownShrinkFetchAdvancedObserved,
            "lazyGridUnknownSizeShrinkCountEventAdvancedObserved": snapshot.unknownShrinkCountEventAdvancedObserved,
            "lazyGridUnknownSizeShrinkResetEstimate": snapshot.unknownShrinkResetEstimate,
            "lazyGridUnknownSizeShrinkMaxEstimatedCount": snapshot.unknownShrinkMaxEstimatedCount,
            "lazyGridUnknownSizeShrinkExactCount": snapshot.unknownShrinkExactCount,
            "lazyGridUnknownSizeShrinkTerminalReturned": snapshot.unknownShrinkTerminalReturned,
            "lazyGridUnknownSizeShrinkTerminalLimit": snapshot.unknownShrinkTerminalLimit,
            "lazyGridUnknownSizeShrinkFetchBaseline": snapshot.unknownShrinkFetchBaseline,
            "lazyGridUnknownSizeShrinkFetchCount": snapshot.unknownShrinkFetchCount,
            "lazyGridUnknownSizeShrinkFetchEpochCount": snapshot.unknownShrinkFetchEpochCount,
            "lazyGridUnknownSizeShrinkCountEventBaseline": snapshot.unknownShrinkCountEventBaseline,
            "lazyGridUnknownSizeShrinkCountEventCount": snapshot.unknownShrinkCountEventCount,
            "lazyGridUnknownSizeShrinkCountEventEpochCount": snapshot.unknownShrinkCountEventEpochCount,
            "lazyGridUnknownSizeShrinkClientSize": snapshot.unknownClientSize,
            "lazyGridUnknownSizeShrinkJavaSelectionChangeCount": 1,
            "lazyGridUnknownSizeShrinkChangeCount": 1,
            "lazyGridUnknownSizeShrinkStatus": snapshot.unknownShrinkStatus,
            "lazyGridUnknownSizeShrinkFetchStatus": snapshot.unknownFetchStatus,
            "lazyGridUnknownSizeShrinkCountStatus": snapshot.unknownCountStatus,
            "lazyGridUnknownSizeShrinkSelectionStatus": snapshot.unknownSelectionStatus,
            "lazyGridUnknownSizeShrinkSelectedId": "U-067",
            "lazyGridUnknownSizeShrinkTailId": "U-072",
            "lazyGridNativePointerObserved": snapshot.nativePointerObserved,
            "lazyGridNativePointerTrustedObserved":
                snapshot.nativePointerTrustedObserved,
            "lazyGridNativePointerEventCount": snapshot.nativePointerEventCount,
            "lazyGridNativePointerTrustedEventCount":
                snapshot.nativePointerTrustedEventCount,
            "lazyGridNativePointerStages": snapshot.nativePointerStages,
            "lazyGridNativePointerLabels": snapshot.nativePointerLabels,
            "lazyGridBrowserProgrammaticActionsObserved":
                snapshot.browserProgrammaticActionsObserved,
            "lazyGridBrowserProgrammaticStages":
                snapshot.browserProgrammaticStages,
            "lazyGridJavaSelectionChangeCount": 3,
            "lazyGridSelectedIds": "L-060",
            "lazyGridFilteredQueryStatus": filteredQueryStatus,
            "lazyGridFilteredCountStatus": filteredCountStatus,
            "lazyGridFilteredSelectionStatus": filteredSelectionStatus,
            "lazyGridRefreshStatus": refreshStatus,
            "lazyGridRefreshAllStatus": refreshAllStatus,
            "lazyGridRefreshAllQueryStatus": refreshAllQueryStatus,
            "lazyGridRefreshAllCountStatus": refreshAllCountStatus,
            "lazyGridRefreshAllFetchBaseline": refreshAllFetchBaseline,
            "lazyGridRefreshAllFetchCount": refreshAllFetchCount,
            "lazyGridRefreshAllCountBaseline": refreshAllCountBaseline,
            "lazyGridRefreshAllCountCount": refreshAllCountCount,
            "lazyGridResetQueryStatus": resetQueryStatus,
            "lazyGridResetCountStatus": resetCountStatus,
            "lazyGridResetSelectionStatus": resetSelectionStatus
        ]) { _, replacement in replacement }
        trace("recv", "lazyGridQuery.frameworkAcceptance", payload)
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
        let requestCount: Int
        let responseCount: Int
    }

    private struct Snapshot: Decodable {
        let rootRouteAvailable: Bool
        let routeRendered: Bool
        let firstMeaningfulPaint: Bool
        let requestCount: Int
        let responseCount: Int
        let pushMessageCount: Int
        let flowClientCount: Int
        let queryStatus: String
        let countStatus: String
        let queryCallCount: Int
        let countCallCount: Int
        let selectionStatus: String
        let refreshStatus: String
        let refreshAllStatus: String
        let unknownConfigStatus: String
        let unknownFetchStatus: String
        let unknownCountStatus: String
        let unknownSelectionStatus: String
        let unknownMutationStatus: String
        let unknownShrinkStatus: String
        let unknownShrinkResetEstimate: Int
        let unknownShrinkMaxEstimatedCount: Int
        let unknownShrinkExactCount: Int
        let unknownShrinkTerminalReturned: Int
        let unknownShrinkTerminalLimit: Int
        let unknownShrinkFetchBaseline: Int
        let unknownShrinkFetchCount: Int
        let unknownShrinkFetchEpochCount: Int
        let unknownShrinkCountEventBaseline: Int
        let unknownShrinkCountEventCount: Int
        let unknownShrinkCountEventEpochCount: Int
        let unknownShrinkResetTopObserved: Bool
        let unknownShrinkRefreshAllObserved: Bool
        let unknownShrinkResetEstimateObserved: Bool
        let unknownShrinkRegrowthObserved: Bool
        let unknownShrinkSelectionRehydratedObserved: Bool
        let unknownShrinkTerminalShortFetchObserved: Bool
        let unknownShrinkExactEndObserved: Bool
        let unknownShrinkTailRenderedObserved: Bool
        let unknownShrinkRemovedTailAbsentObserved: Bool
        let unknownShrinkSelectionRetainedObserved: Bool
        let unknownShrinkFetchAdvancedObserved: Bool
        let unknownShrinkCountEventAdvancedObserved: Bool
        let unknownInitialEstimate: Int
        let unknownEstimateIncrease: Int
        let unknownMaxEstimatedCount: Int
        let unknownExactCount: Int
        let unknownClientSize: Int
        let unknownTerminalReturned: Int
        let unknownTerminalLimit: Int
        let unknownMutationResetEstimate: Int
        let unknownMutationMaxEstimatedCount: Int
        let unknownMutationTerminalReturned: Int
        let unknownMutationTerminalLimit: Int
        let filterValue: String
        let selectionColumnObserved: Bool
        let ariaMultiSelectableObserved: Bool
        let referenceCellObserved: Bool
        let referenceRowObserved: Bool
        let referenceCheckboxObserved: Bool
        let multiSelectionModeObserved: Bool
        let renderedRowCount: Int
        let farViewportObserved: Bool
        let firstClientSelectionObserved: Bool
        let secondClientSelectionObserved: Bool
        let multiSelectionDomObserved: Bool
        let refreshItemObserved: Bool
        let returnViewportObserved: Bool
        let sortObserved: Bool
        let filterObserved: Bool
        let refreshAllOffViewportObserved: Bool
        let refreshAllObserved: Bool
        let refreshAllRemovedRowObserved: Bool
        let refreshAllRehydrationObserved: Bool
        let filterResetObserved: Bool
        let clientDeselectionObserved: Bool
        let finalViewportObserved: Bool
        let unknownFetchOnlyObserved: Bool
        let unknownInitialEstimatedObserved: Bool
        let unknownEstimateGrowthObserved: Bool
        let unknownFetchCrossedInitialEstimateObserved: Bool
        let unknownTerminalShortFetchObserved: Bool
        let unknownExactEndObserved: Bool
        let unknownTailRenderedObserved: Bool
        let unknownTailSelectionObserved: Bool
        let unknownInitialCycleObserved: Bool
        let unknownInitialCyclePreservedObserved: Bool
        let unknownMutationResetTopObserved: Bool
        let unknownMutationRefreshAllObserved: Bool
        let unknownMutationResetEstimateObserved: Bool
        let unknownMutationRegrowthObserved: Bool
        let unknownMutationCrossedPriorExactObserved: Bool
        let unknownMutationSelectionRehydratedObserved: Bool
        let unknownMutationSecondGrowthObserved: Bool
        let unknownMutationTerminalShortFetchObserved: Bool
        let unknownMutationExactEndObserved: Bool
        let unknownMutationTailRenderedObserved: Bool
        let unknownMutationSelectionRetainedObserved: Bool
        let nativePointerObserved: Bool
        let nativePointerTrustedObserved: Bool
        let nativePointerEventCount: Int
        let nativePointerTrustedEventCount: Int
        let nativePointerStages: String
        let nativePointerLabels: String
        let browserProgrammaticActionsObserved: Bool
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
                && multiSelectionModeObserved
                && queryStatus.contains("sort=none")
                && queryStatus.contains("filter=all")
                && queryStatus.contains("matches=80")
                && queryStatus.contains("selected=none")
                && countStatus.contains("filter=all")
                && countStatus.contains("matches=80")
                && countStatus.contains("selected=none")
                && renderedRowCount > 0
                && renderedRowCount < 80
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
                "lazyGridQueryStatus": queryStatus,
                "lazyGridCountStatus": countStatus,
                "lazyGridQueryCallCount": queryCallCount,
                "lazyGridCountCallCount": countCallCount,
                "lazyGridSelectionStatus": selectionStatus,
                "lazyGridRefreshStatus": refreshStatus,
                "lazyGridRefreshAllStatus": refreshAllStatus,
                "lazyGridUnknownSizeConfigStatus": unknownConfigStatus,
                "lazyGridUnknownSizeFetchStatus": unknownFetchStatus,
                "lazyGridUnknownSizeCountStatus": unknownCountStatus,
                "lazyGridUnknownSizeSelectionStatus": unknownSelectionStatus,
                "lazyGridUnknownSizeMutationStatus": unknownMutationStatus,
                "lazyGridUnknownSizeShrinkStatus": unknownShrinkStatus,
                "lazyGridUnknownSizeShrinkResetEstimate": unknownShrinkResetEstimate,
                "lazyGridUnknownSizeShrinkMaxEstimatedCount": unknownShrinkMaxEstimatedCount,
                "lazyGridUnknownSizeShrinkExactCount": unknownShrinkExactCount,
                "lazyGridUnknownSizeShrinkTerminalReturned": unknownShrinkTerminalReturned,
                "lazyGridUnknownSizeShrinkTerminalLimit": unknownShrinkTerminalLimit,
                "lazyGridUnknownSizeShrinkFetchBaseline": unknownShrinkFetchBaseline,
                "lazyGridUnknownSizeShrinkFetchCount": unknownShrinkFetchCount,
                "lazyGridUnknownSizeShrinkFetchEpochCount": unknownShrinkFetchEpochCount,
                "lazyGridUnknownSizeShrinkCountEventBaseline": unknownShrinkCountEventBaseline,
                "lazyGridUnknownSizeShrinkCountEventCount": unknownShrinkCountEventCount,
                "lazyGridUnknownSizeShrinkCountEventEpochCount": unknownShrinkCountEventEpochCount,
                "lazyGridUnknownSizeShrinkResetTopObserved": unknownShrinkResetTopObserved,
                "lazyGridUnknownSizeShrinkRefreshAllObserved": unknownShrinkRefreshAllObserved,
                "lazyGridUnknownSizeShrinkResetEstimateObserved": unknownShrinkResetEstimateObserved,
                "lazyGridUnknownSizeShrinkRegrowthObserved": unknownShrinkRegrowthObserved,
                "lazyGridUnknownSizeShrinkSelectionRehydratedObserved": unknownShrinkSelectionRehydratedObserved,
                "lazyGridUnknownSizeShrinkTerminalShortFetchObserved": unknownShrinkTerminalShortFetchObserved,
                "lazyGridUnknownSizeShrinkExactEndObserved": unknownShrinkExactEndObserved,
                "lazyGridUnknownSizeShrinkTailRenderedObserved": unknownShrinkTailRenderedObserved,
                "lazyGridUnknownSizeShrinkRemovedTailAbsentObserved": unknownShrinkRemovedTailAbsentObserved,
                "lazyGridUnknownSizeShrinkSelectionRetainedObserved": unknownShrinkSelectionRetainedObserved,
                "lazyGridUnknownSizeShrinkFetchAdvancedObserved": unknownShrinkFetchAdvancedObserved,
                "lazyGridUnknownSizeShrinkCountEventAdvancedObserved": unknownShrinkCountEventAdvancedObserved,
                "lazyGridUnknownSizeInitialEstimate": unknownInitialEstimate,
                "lazyGridUnknownSizeEstimateIncrease": unknownEstimateIncrease,
                "lazyGridUnknownSizeMaxEstimatedCount": unknownMaxEstimatedCount,
                "lazyGridUnknownSizeExactCount": unknownExactCount,
                "lazyGridUnknownSizeClientSize": unknownClientSize,
                "lazyGridUnknownSizeTerminalReturned": unknownTerminalReturned,
                "lazyGridUnknownSizeTerminalLimit": unknownTerminalLimit,
                "lazyGridUnknownSizeMutationResetEstimate":
                    unknownMutationResetEstimate,
                "lazyGridUnknownSizeMutationMaxEstimatedCount":
                    unknownMutationMaxEstimatedCount,
                "lazyGridUnknownSizeMutationTerminalReturned":
                    unknownMutationTerminalReturned,
                "lazyGridUnknownSizeMutationTerminalLimit":
                    unknownMutationTerminalLimit,
                "lazyGridFilterValue": filterValue,
                "lazyGridSelectionColumnObserved": selectionColumnObserved,
                "lazyGridAriaMultiSelectableObserved": ariaMultiSelectableObserved,
                "lazyGridReferenceCellObserved": referenceCellObserved,
                "lazyGridReferenceRowObserved": referenceRowObserved,
                "lazyGridReferenceCheckboxObserved": referenceCheckboxObserved,
                "lazyGridRenderedRowCount": renderedRowCount,
                "lazyGridNativePointerEventCount": nativePointerEventCount,
                "lazyGridNativePointerTrustedEventCount":
                    nativePointerTrustedEventCount,
                "lazyGridNativePointerStages": nativePointerStages,
                "lazyGridNativePointerLabels": nativePointerLabels,
                "lazyGridBrowserProgrammaticStages": browserProgrammaticStages,
                "browserError": browserError,
                "requestError": requestError,
                "responseError": responseError,
                "pushError": pushError
            ]
        }
    }

    private static let inspectionScript = """
    (() => {
      const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
      const marker = window.VaadinDesktopFlowLazyGridSelection || {};
      window.VaadinDesktopFlowLazyGridSelection = marker;
      const grid = document.querySelector('#framework-flow-lazy-grid-selection-grid');
      const unknownGrid = document.querySelector('#framework-flow-unknown-size-grid');
      const route = document.querySelector('#framework-flow-lazy-grid-selection-route');
      const queryStatus = document.querySelector(
        '#framework-flow-lazy-grid-selection-query-status')?.textContent || '';
      const countStatus = document.querySelector(
        '#framework-flow-lazy-grid-selection-count-status')?.textContent || '';
      const selectionStatus = document.querySelector(
        '#framework-flow-lazy-grid-selection-status')?.textContent || '';
      const refreshStatus = document.querySelector(
        '#framework-flow-lazy-grid-selection-refresh-status')?.textContent || '';
      const refreshAllStatus = document.querySelector(
        '#framework-flow-lazy-grid-selection-refresh-all-status')?.textContent || '';
      const unknownConfigStatus = document.querySelector(
        '#framework-flow-unknown-size-grid-config-status')?.textContent || '';
      const unknownFetchStatus = document.querySelector(
        '#framework-flow-unknown-size-grid-fetch-status')?.textContent || '';
      const unknownCountStatus = document.querySelector(
        '#framework-flow-unknown-size-grid-count-status')?.textContent || '';
      const unknownSelectionStatus = document.querySelector(
        '#framework-flow-unknown-size-grid-selection-status')?.textContent || '';
      const unknownMutationStatus = document.querySelector(
        '#framework-flow-unknown-size-grid-mutation-status')?.textContent || '';
      const unknownShrinkStatus = document.querySelector(
        '#framework-flow-unknown-size-grid-shrink-status')?.textContent || '';
      const queryCallCount = Number(
        queryStatus.match(/^Lazy grid query (\\d+):/)?.[1] || 0);
      const countCallCount = Number(
        countStatus.match(/^Lazy Grid selection count (\\d+):/)?.[1] || 0);
      const filterValue = String(document.querySelector(
        '#framework-flow-lazy-grid-selection-filter')?.value || '');
      const clients = window.Vaadin && window.Vaadin.Flow && window.Vaadin.Flow.clients
        ? Object.keys(window.Vaadin.Flow.clients).filter((name) => name !== 'TypeScript')
        : [];
      const visit = (node, predicate) => {
        if (!node) return null;
        if (node.nodeType === Node.ELEMENT_NODE && predicate(node)) return node;
        if (node.shadowRoot) {
          const shadowResult = visit(node.shadowRoot, predicate);
          if (shadowResult) return shadowResult;
        }
        if (typeof node.assignedElements === 'function') {
          for (const child of node.assignedElements({ flatten: true })) {
            const result = visit(child, predicate);
            if (result) return result;
          }
        }
        const children = node.children || node.childNodes || [];
        for (const child of children) {
          const result = visit(child, predicate);
          if (result) return result;
        }
        return null;
      };
      const cellForGridLabel = (targetGrid, label) => targetGrid
        ? visit(targetGrid,
          (element) => element.localName === 'vaadin-grid-cell-content'
          && String(element.textContent || '').trim() === label)
        : null;
      const cellForLabel = (label) => cellForGridLabel(grid, label);
      const rowForGridLabel = (targetGrid, label) => {
        const cell = cellForGridLabel(targetGrid, label);
        const slot = cell?.assignedSlot;
        if (!slot) return null;
        return slot.closest?.('tr')
          || Array.from(targetGrid?.shadowRoot?.querySelectorAll('tr') || [])
            .find((row) => row.contains(slot))
          || null;
      };
      const rowForLabel = (label) => {
        return rowForGridLabel(grid, label);
      };
      const checkboxForGridLabel = (targetGrid, label) => {
        const row = rowForGridLabel(targetGrid, label);
        return row
          ? visit(row, (element) => element.localName === 'vaadin-checkbox')
          : null;
      };
      const checkboxForLabel = (label) => {
        return checkboxForGridLabel(grid, label);
      };
      const hasRenderedGridItem = (targetGrid, label) =>
        !!cellForGridLabel(targetGrid, label);
      const hasRenderedItem = (label) => hasRenderedGridItem(grid, label);
      const hasVisibleGridItem = (targetGrid, label) => {
        const cell = cellForGridLabel(targetGrid, label);
        if (!cell || !targetGrid) return false;
        const rect = cell.getBoundingClientRect();
        const gridRect = targetGrid.getBoundingClientRect();
        return rect.width > 0
          && rect.height > 0
          && rect.bottom > gridRect.top
          && rect.top < gridRect.bottom
          && rect.bottom > 0
          && rect.top < window.innerHeight;
      };
      const hasVisibleItem = (label) => {
        return hasVisibleGridItem(grid, label);
      };
      const gridRowSelected = (targetGrid, label) => {
        const row = rowForGridLabel(targetGrid, label);
        const checkbox = checkboxForGridLabel(targetGrid, label);
        return !!row
          && checkbox?.checked === true
          && (row.hasAttribute?.('selected')
            || row.getAttribute?.('aria-selected') === 'true'
            || row.selected === true);
      };
      const gridRowAriaSelected = (targetGrid, label) =>
        rowForGridLabel(targetGrid, label)?.getAttribute?.('aria-selected')
          === 'true';
      const rowSelected = (label) => {
        return gridRowSelected(grid, label);
      };
      const rowNotSelected = (label) => {
        const row = rowForLabel(label);
        const checkbox = checkboxForLabel(label);
        return !!row
          && checkbox?.checked === false
          && !row.hasAttribute?.('selected')
          && row.getAttribute?.('aria-selected') !== 'true'
          && row.selected !== true;
      };
      const hasStatusTokens = (status, ...expectedTokens) => {
        const tokens = new Set(String(status || '').trim().split(/\\s+/));
        return expectedTokens.every((token) => tokens.has(token));
      };
      const refreshedL060Observed = !!rowForLabel('L-060')
        && rowForLabel('L-060') === rowForLabel('refreshed');
      const renderedRows = grid?._getRenderedRows?.()
        || Array.from(grid?.shadowRoot?.querySelectorAll('tr[part~="body-row"]') || []);
      const renderedRowCount = Number(renderedRows?.length || 0);
      const ariaGrid = grid
        ? visit(grid, (element) => element.getAttribute?.('role') === 'grid'
          && element.getAttribute?.('aria-multiselectable') === 'true')
        : null;
      const selectionColumnObserved =
        !!grid?.querySelector('vaadin-grid-flow-selection-column');
      const ariaMultiSelectableObserved = !!grid
        && (grid.getAttribute('aria-multiselectable') === 'true' || !!ariaGrid);
      const referenceLabel = hasVisibleItem('L-060') ? 'L-060' : 'L-001';
      const referenceCellObserved = !!cellForLabel(referenceLabel);
      const referenceRowObserved = !!rowForLabel(referenceLabel);
      const referenceCheckboxObserved = !!checkboxForLabel(referenceLabel);
      const multiSelectionModeObserved = selectionColumnObserved
        && referenceCheckboxObserved;
      const eventTwoStatus = selectionStatus.includes(
        'Lazy grid selection: L-060,L-061; change=2, added=L-060, removed=none, client=true');
      const eventThreeStatus = selectionStatus.includes(
        'Lazy grid selection: L-060; change=3, added=none, removed=L-061, client=true');
      const statusNumber = (status, field) => Number(
        String(status || '').match(
          new RegExp('(?:^|[,;]\\\\s*)' + field + '=(\\\\d+)'))?.[1] || 0);
      const statusValue = (status, field) => String(
        String(status || '').match(
          new RegExp('(?:^|[,;]\\\\s*)' + field + '=([^,;\\\\s]+)'))?.[1]
            || '');
      const unknownInitialEstimate = statusNumber(
        unknownConfigStatus, 'estimate');
      const unknownEstimateIncrease = statusNumber(
        unknownConfigStatus, 'increase');
      const unknownMaxEstimatedCount = statusNumber(
        unknownCountStatus, 'maxEstimated');
      const unknownExactCount = statusNumber(unknownCountStatus, 'current');
      const unknownClientSize = Number(unknownGrid?.size || 0);
      const unknownTerminalReturned = statusNumber(
        unknownFetchStatus, 'terminalReturned');
      const unknownTerminalLimit = statusNumber(
        unknownFetchStatus, 'terminalLimit');
      const unknownInitialMaxEstimated = statusNumber(
        unknownCountStatus, 'initialMaxEstimated');
      const unknownInitialExact = statusNumber(
        unknownCountStatus, 'initialExact');
      const unknownInitialHistory = statusValue(
        unknownCountStatus, 'initialHistory');
      const unknownMutationResetEstimate = statusNumber(
        unknownCountStatus, 'mutationResetEstimate');
      const unknownMutationMaxEstimatedCount = statusNumber(
        unknownCountStatus, 'mutationMaxEstimated');
      const unknownMutationExact = statusNumber(
        unknownCountStatus, 'mutationExact');
      const unknownMutationHistory = statusValue(
        unknownCountStatus, 'mutationHistory');
      const unknownMutationTerminalReturned = statusNumber(
        unknownFetchStatus, 'mutationTerminalReturned');
      const unknownMutationTerminalLimit = statusNumber(
        unknownFetchStatus, 'mutationTerminalLimit');
      const unknownSelectionU067Status = unknownSelectionStatus.includes(
        'Unknown-size Grid selection: U-067; change=1, added=U-067, '
          + 'removed=none, client=true');
      const unknownMutationExactStatus = unknownMutationStatus.includes(
        'Unknown-size Grid mutation: operation=refreshAll; '
          + 'added=U-068..U-093, before=67, after=93, selected=U-067, '
          + 'selectionChanges=1, mutationChanges=1, dataChangeEvents=1, '
          + 'dataRefreshEvents=0, client=true');
      const unknownFetchOnlyObserved = unknownConfigStatus.includes(
        'Unknown-size Grid: fetchOnly=true')
        && unknownInitialEstimate === 40
        && unknownEstimateIncrease === 40
        && unknownConfigStatus.includes('backendItems=67');
      const unknownInitialEstimatedObserved = unknownClientSize === 40
        && unknownCountStatus.includes('current=40')
        && unknownCountStatus.includes('estimated=true')
        && /history=.*\\b40E\\b/.test(unknownCountStatus);
      const unknownEstimateGrowthObserved = unknownClientSize >= 80
        && unknownMaxEstimatedCount >= 80
        && unknownCountStatus.includes('estimated=true');
      const unknownFetchCrossedInitialEstimateObserved =
        unknownFetchStatus.includes('crossedInitial=true');
      const unknownTerminalShortFetchObserved =
        unknownFetchStatus.includes('terminalShort=true')
        && unknownTerminalReturned > 0
        && unknownTerminalReturned < unknownTerminalLimit;
      const unknownExactEndObserved = unknownClientSize === 67
        && unknownExactCount === 67
        && unknownCountStatus.includes('estimated=false')
        && /history=.*\\b67X\\b/.test(unknownCountStatus)
        && unknownFetchStatus.includes('terminalEnd=67')
        && unknownFetchStatus.includes('terminalLast=U-067')
        && unknownFetchStatus.includes('maxEnd=67');
      const unknownTailRenderedObserved =
        hasVisibleGridItem(unknownGrid, 'U-067');
      const unknownTailSelectionObserved =
        unknownSelectionU067Status
        && unknownTailRenderedObserved
        && gridRowSelected(unknownGrid, 'U-067');
      const unknownInitialCycleObserved = unknownFetchOnlyObserved
        && unknownInitialEstimatedObserved
        && unknownEstimateGrowthObserved
        && unknownTerminalShortFetchObserved
        && unknownExactEndObserved
        && unknownTailSelectionObserved;
      const unknownInitialCyclePreservedObserved =
        unknownInitialMaxEstimated >= 80
        && unknownInitialExact === 67
        && unknownInitialHistory === '40E>80E>67X'
        && unknownFetchStatus.includes('terminalEnd=67')
        && unknownFetchStatus.includes('terminalLast=U-067');
      const unknownMutationResetTopObserved = unknownClientSize === 67
        && unknownExactCount === 67
        && unknownCountStatus.includes('estimated=false')
        && hasVisibleGridItem(unknownGrid, 'U-001')
        && !hasRenderedGridItem(unknownGrid, 'U-067')
        && unknownSelectionU067Status;
      const unknownMutationResetEstimateObserved =
        unknownMutationExactStatus
        && unknownFetchStatus.includes('mutationActive=true')
        && unknownClientSize === 40
        && unknownExactCount === 40
        && unknownCountStatus.includes('estimated=true')
        && unknownMutationResetEstimate === 40
        && unknownMutationHistory.startsWith('40E')
        && unknownSelectionU067Status;
      const unknownMutationRegrowthObserved =
        unknownMutationResetEstimate === 40
        && unknownClientSize >= 80
        && unknownMutationMaxEstimatedCount >= 80
        && unknownCountStatus.includes('estimated=true')
        && /(?:^|>)80E(?:>|$)/.test(unknownMutationHistory);
      const unknownMutationCrossedPriorExactObserved =
        unknownFetchStatus.includes('mutationCrossedPriorExact=true')
        && statusNumber(unknownFetchStatus, 'mutationMaxEnd') > 67;
      const unknownMutationSelectionRehydratedObserved =
        unknownSelectionU067Status
        && hasVisibleGridItem(unknownGrid, 'U-067')
        && gridRowSelected(unknownGrid, 'U-067')
        && gridRowAriaSelected(unknownGrid, 'U-067')
        && hasVisibleGridItem(unknownGrid, 'U-068');
      const unknownMutationSecondGrowthObserved =
        unknownMutationMaxEstimatedCount >= 120
        && /(?:^|>)120E(?:>|$)/.test(unknownMutationHistory);
      const unknownMutationTerminalShortFetchObserved =
        unknownFetchStatus.includes('mutationTerminalShort=true')
        && unknownMutationTerminalReturned > 0
        && unknownMutationTerminalReturned < unknownMutationTerminalLimit;
      const unknownMutationExactEndObserved = unknownClientSize === 93
        && unknownExactCount === 93
        && unknownMutationExact === 93
        && unknownCountStatus.includes('estimated=false')
        && /(?:^|>)93X$/.test(unknownMutationHistory)
        && unknownFetchStatus.includes('mutationTerminalEnd=93')
        && unknownFetchStatus.includes('mutationTerminalLast=U-093')
        && statusNumber(unknownFetchStatus, 'mutationMaxEnd') === 93;
      const unknownMutationTailRenderedObserved =
        hasVisibleGridItem(unknownGrid, 'U-093');
      const unknownMutationSelectionRetainedObserved =
        unknownMutationExactStatus && unknownSelectionU067Status;

      const unknownShrinkResetEstimate = statusNumber(unknownCountStatus, 'shrinkResetEstimate');
      const unknownShrinkMaxEstimatedCount = statusNumber(unknownCountStatus, 'shrinkMaxEstimated');
      const unknownShrinkExactCount = statusNumber(unknownCountStatus, 'shrinkExact');
      const unknownShrinkHistory = statusValue(unknownCountStatus, 'shrinkHistory');
      const unknownShrinkTerminalReturned = statusNumber(unknownFetchStatus, 'shrinkTerminalReturned');
      const unknownShrinkTerminalLimit = statusNumber(unknownFetchStatus, 'shrinkTerminalLimit');
      const unknownShrinkFetchBaseline = statusNumber(unknownShrinkStatus, 'fetchBaseline');
      const unknownShrinkFetchCount = Number(unknownFetchStatus.match(/\\bcall=(\\d+)/)?.[1] || 0);
      const unknownShrinkFetchEpochCount = statusNumber(unknownFetchStatus, 'shrinkFetchCount');
      const unknownShrinkCountEventBaseline = statusNumber(unknownShrinkStatus, 'countBaseline');
      const unknownShrinkCountEventCount = Number(unknownCountStatus.match(/\\bevents=(\\d+)/)?.[1] || 0);
      const unknownShrinkCountEventEpochCount = statusNumber(unknownCountStatus, 'shrinkCountEvents');
      const unknownShrinkExactStatus = unknownShrinkStatus.includes(
        'Unknown-size Grid shrink: operation=refreshAll; '
          + 'removed=U-073..U-093, before=93, after=72, removedCount=21, '
          + 'removedSelected=false, retainedSameInstance=true, selected=U-067, '
          + 'selectionChanges=1, shrinkChanges=1, dataChangeEvents=1, '
          + 'dataRefreshEvents=0, fetchBaseline=')
        && unknownShrinkStatus.endsWith(', client=true');
      const unknownShrinkFetchAdvancedObserved = unknownShrinkExactStatus
        && unknownShrinkFetchBaseline > 0 && unknownShrinkFetchEpochCount > 0
        && unknownShrinkFetchCount - unknownShrinkFetchBaseline === unknownShrinkFetchEpochCount;
      const unknownShrinkCountEventAdvancedObserved = unknownShrinkExactStatus
        && unknownShrinkCountEventBaseline > 0 && unknownShrinkCountEventEpochCount > 0
        && unknownShrinkCountEventCount - unknownShrinkCountEventBaseline === unknownShrinkCountEventEpochCount
        && unknownShrinkHistory.split('>').length === unknownShrinkCountEventEpochCount;
      const unknownShrinkResetTopObserved = unknownClientSize === 93
        && unknownExactCount === 93 && unknownCountStatus.includes('estimated=false')
        && hasVisibleGridItem(unknownGrid, 'U-001')
        && !hasRenderedGridItem(unknownGrid, 'U-067')
        && !hasRenderedGridItem(unknownGrid, 'U-093')
        && unknownShrinkStatus === 'Unknown-size Grid shrink: pending'
        && unknownSelectionU067Status;
      const unknownShrinkResetEstimateObserved = unknownShrinkExactStatus
        && unknownFetchStatus.includes('shrinkActive=true')
        && unknownClientSize === 40 && unknownExactCount === 40
        && unknownCountStatus.includes('estimated=true')
        && unknownShrinkResetEstimate === 40 && unknownShrinkHistory.startsWith('40E')
        && unknownSelectionU067Status;
      const unknownShrinkRegrowthObserved = unknownShrinkResetEstimate === 40
        && unknownClientSize >= 80 && unknownShrinkMaxEstimatedCount >= 80
        && unknownCountStatus.includes('estimated=true')
        && /(?:^|>)80E(?:>|$)/.test(unknownShrinkHistory)
        && unknownFetchStatus.includes('shrinkCrossedInitial=true')
        && statusNumber(unknownFetchStatus, 'shrinkMaxEnd') > 40;
      const unknownShrinkSelectionRehydratedObserved = unknownShrinkExactStatus
        && unknownSelectionU067Status && hasVisibleGridItem(unknownGrid, 'U-067')
        && gridRowSelected(unknownGrid, 'U-067')
        && gridRowAriaSelected(unknownGrid, 'U-067');
      const unknownShrinkTerminalShortFetchObserved = unknownFetchStatus.includes('shrinkTerminalShort=true')
        && unknownShrinkTerminalReturned > 0 && unknownShrinkTerminalReturned < unknownShrinkTerminalLimit;
      const unknownShrinkExactEndObserved = unknownClientSize === 72
        && unknownExactCount === 72 && unknownShrinkExactCount === 72
        && unknownCountStatus.includes('estimated=false')
        && /(?:^|>)72X$/.test(unknownShrinkHistory)
        && unknownFetchStatus.includes('shrinkTerminalEnd=72')
        && unknownFetchStatus.includes('shrinkTerminalLast=U-072')
        && statusNumber(unknownFetchStatus, 'shrinkMaxEnd') === 72;
      const unknownShrinkTailRenderedObserved = hasVisibleGridItem(unknownGrid, 'U-072');
      // Check exact cell labels throughout rendered light/shadow/slot content,
      // not a visible-row text prefix or a viewport-only absence.
      const unknownShrinkRemovedTailAbsentObserved = Array.from(
        {length: 21}, (_, i) => 'U-' + String(i + 73).padStart(3, '0'))
          .every(label => !hasRenderedGridItem(unknownGrid, label));
      const unknownShrinkSelectionRetainedObserved = unknownShrinkExactStatus && unknownSelectionU067Status;
      return JSON.stringify({
        rootRouteAvailable: !!document.querySelector(
          '#framework-flow-lazy-grid-selection-open-route'),
        routeRendered: !!route,
        firstMeaningfulPaint:
          diagnostics.standardBundleFirstMeaningfulPaintReported === true,
        requestCount: Number(diagnostics.standardBundleOpaqueRequestCount || 0),
        responseCount: Number(diagnostics.standardBundleOpaqueResponseCount || 0),
        pushMessageCount: Number(diagnostics.standardBundlePushMessageCount || 0),
        flowClientCount: clients.length,
        queryStatus,
        countStatus,
        queryCallCount,
        countCallCount,
        selectionStatus,
        refreshStatus,
        refreshAllStatus,
        unknownConfigStatus,
        unknownFetchStatus,
        unknownCountStatus,
        unknownSelectionStatus,
        unknownMutationStatus,
        unknownShrinkStatus,
        unknownShrinkResetEstimate,
        unknownShrinkMaxEstimatedCount,
        unknownShrinkExactCount,
        unknownShrinkTerminalReturned,
        unknownShrinkTerminalLimit,
        unknownShrinkFetchBaseline,
        unknownShrinkFetchCount,
        unknownShrinkFetchEpochCount,
        unknownShrinkCountEventBaseline,
        unknownShrinkCountEventCount,
        unknownShrinkCountEventEpochCount,
        unknownShrinkResetTopObserved,
        unknownShrinkRefreshAllObserved: unknownShrinkExactStatus,
        unknownShrinkResetEstimateObserved,
        unknownShrinkRegrowthObserved,
        unknownShrinkSelectionRehydratedObserved,
        unknownShrinkTerminalShortFetchObserved,
        unknownShrinkExactEndObserved,
        unknownShrinkTailRenderedObserved,
        unknownShrinkRemovedTailAbsentObserved,
        unknownShrinkSelectionRetainedObserved,
        unknownShrinkFetchAdvancedObserved,
        unknownShrinkCountEventAdvancedObserved,
        unknownInitialEstimate,
        unknownEstimateIncrease,
        unknownMaxEstimatedCount,
        unknownExactCount,
        unknownClientSize,
        unknownTerminalReturned,
        unknownTerminalLimit,
        unknownMutationResetEstimate,
        unknownMutationMaxEstimatedCount,
        unknownMutationTerminalReturned,
        unknownMutationTerminalLimit,
        filterValue,
        selectionColumnObserved,
        ariaMultiSelectableObserved,
        referenceCellObserved,
        referenceRowObserved,
        referenceCheckboxObserved,
        multiSelectionModeObserved,
        renderedRowCount,
        farViewportObserved:
          hasVisibleItem('L-060')
            && hasVisibleItem('L-061')
            && rowNotSelected('L-060')
            && rowNotSelected('L-061')
            && renderedRowCount > 0
            && renderedRowCount < 80,
        firstClientSelectionObserved:
          selectionStatus.includes(
            'Lazy grid selection: L-061; change=1, added=L-061, removed=none, client=true')
            && rowSelected('L-061')
            && rowNotSelected('L-060'),
        secondClientSelectionObserved:
          eventTwoStatus && rowSelected('L-060') && rowSelected('L-061'),
        multiSelectionDomObserved:
          eventTwoStatus && rowSelected('L-060') && rowSelected('L-061'),
        refreshItemObserved:
          refreshStatus.includes(
            'Lazy grid refresh: id=L-060; previous=ready, current=refreshed, '
              + 'sameInstance=false, equals=false, stableId=true, selected=true, '
              + 'selectionChanges=2, refreshChanges=1, client=true, '
              + 'operation=refreshItem')
            && refreshedL060Observed
            && rowSelected('L-060')
            && eventTwoStatus,
        returnViewportObserved:
          hasVisibleItem('L-001')
            && !hasVisibleItem('L-060')
            && !hasVisibleItem('L-061')
            && eventTwoStatus,
        sortObserved:
          hasStatusTokens(queryStatus, 'sort=owner:ascending', 'filter=all',
            'matches=80', 'selected=L-060,L-061')
            && hasVisibleItem('L-002')
            && eventTwoStatus,
        filterObserved:
          filterValue === 'platform'
            && hasStatusTokens(queryStatus, 'sort=owner:ascending',
              'filter=platform', 'matches=26', 'selected=L-060,L-061')
            && hasStatusTokens(countStatus, 'filter=platform', 'matches=26',
              'selected=L-060,L-061')
            && hasVisibleItem('L-060')
            && refreshedL060Observed
            && rowSelected('L-060')
            && !hasVisibleItem('L-061')
            && eventTwoStatus,
        refreshAllOffViewportObserved:
          filterValue === 'platform'
            && hasStatusTokens(queryStatus, 'sort=owner:ascending',
              'filter=platform', 'matches=26', 'selected=L-060,L-061')
            && hasStatusTokens(countStatus, 'filter=platform', 'matches=26',
              'selected=L-060,L-061')
            && hasVisibleItem('L-003')
            && !hasRenderedItem('L-060')
            && !hasRenderedItem('L-061')
            && eventTwoStatus,
        refreshAllObserved:
          refreshAllStatus.includes(
            'Lazy grid refresh all: operation=refreshAll; removed=L-003, '
              + 'beforeTotal=80, afterTotal=79, beforeFiltered=26, '
              + 'afterFiltered=25, selected=L-060,L-061, '
              + 'selectionChanges=2, refreshAllChanges=1, '
              + 'dataChangeEvents=1, dataRefreshEvents=0, client=true')
            && filterValue === 'platform'
            && hasStatusTokens(queryStatus, 'sort=owner:ascending',
              'filter=platform', 'matches=25', 'selected=L-060,L-061')
            && hasStatusTokens(countStatus, 'filter=platform', 'matches=25',
              'selected=L-060,L-061')
            && hasVisibleItem('L-006')
            && !hasRenderedItem('L-060')
            && !hasRenderedItem('L-061')
            && eventTwoStatus,
        refreshAllRemovedRowObserved:
          refreshAllStatus.includes('operation=refreshAll')
            && !hasRenderedItem('L-003'),
        refreshAllRehydrationObserved:
          refreshAllStatus.includes('operation=refreshAll')
            && hasStatusTokens(queryStatus, 'sort=owner:ascending',
              'filter=platform', 'matches=25', 'selected=L-060,L-061')
            && hasStatusTokens(countStatus, 'filter=platform', 'matches=25',
              'selected=L-060,L-061')
            && hasVisibleItem('L-060')
            && refreshedL060Observed
            && rowSelected('L-060')
            && !hasVisibleItem('L-061')
            && eventTwoStatus,
        filterResetObserved:
          filterValue === ''
            && hasStatusTokens(queryStatus, 'sort=owner:ascending', 'filter=all',
              'matches=79', 'selected=L-060,L-061')
            && hasStatusTokens(countStatus, 'filter=all', 'matches=79',
              'selected=L-060,L-061')
            && hasVisibleItem('L-061')
            && rowSelected('L-061')
            && eventTwoStatus,
        clientDeselectionObserved:
          eventThreeStatus && hasVisibleItem('L-061') && rowNotSelected('L-061'),
        finalViewportObserved:
          eventThreeStatus
            && hasVisibleItem('L-060')
            && refreshedL060Observed
            && rowSelected('L-060'),
        unknownFetchOnlyObserved,
        unknownInitialEstimatedObserved,
        unknownEstimateGrowthObserved,
        unknownFetchCrossedInitialEstimateObserved,
        unknownTerminalShortFetchObserved,
        unknownExactEndObserved,
        unknownTailRenderedObserved,
        unknownTailSelectionObserved,
        unknownInitialCycleObserved,
        unknownInitialCyclePreservedObserved,
        unknownMutationResetTopObserved,
        unknownMutationRefreshAllObserved: unknownMutationExactStatus,
        unknownMutationResetEstimateObserved,
        unknownMutationRegrowthObserved,
        unknownMutationCrossedPriorExactObserved,
        unknownMutationSelectionRehydratedObserved,
        unknownMutationSecondGrowthObserved,
        unknownMutationTerminalShortFetchObserved,
        unknownMutationExactEndObserved,
        unknownMutationTailRenderedObserved,
        unknownMutationSelectionRetainedObserved,
        nativePointerObserved: marker.nativePointerObserved === true,
        nativePointerTrustedObserved:
          marker.nativePointerTrustedObserved === true,
        nativePointerEventCount: Number(marker.nativePointerEventCount || 0),
        nativePointerTrustedEventCount:
          Number(marker.nativePointerTrustedEventCount || 0),
        nativePointerStages:
          Array.isArray(marker.nativePointerStages)
            ? marker.nativePointerStages.join(',')
            : '',
        nativePointerLabels:
          Array.isArray(marker.nativePointerLabels)
            ? marker.nativePointerLabels.join(',')
            : '',
        browserProgrammaticActionsObserved:
          marker.browserProgrammaticActionsObserved === true,
        browserProgrammaticStages:
          Array.isArray(marker.browserProgrammaticStages)
            ? marker.browserProgrammaticStages.join(',')
            : '',
        browserError: diagnostics.standardBundleBrowserError || '',
        requestError: diagnostics.standardBundleOpaqueRequestError || '',
        responseError: diagnostics.standardBundleOpaqueResponseError || '',
        pushError: diagnostics.standardBundlePushError || ''
      });
    })()
    """

    private static let navigationAction = """
    (() => {
      const button = document.querySelector(
        '#framework-flow-lazy-grid-selection-open-route');
      if (!button || typeof button.click !== 'function') return false;
      const marker = window.VaadinDesktopFlowLazyGridSelection || {};
      window.VaadinDesktopFlowLazyGridSelection = marker;
      marker.browserProgrammaticActionsObserved = true;
      marker.browserProgrammaticStages = ['navigate'];
      button.click();
      return true;
    })()
    """

    private static let farViewportAction = """
    (() => {
      const grid = document.querySelector('#framework-flow-lazy-grid-selection-grid');
      if (!grid || typeof grid.scrollToIndex !== 'function') return false;
      const marker = window.VaadinDesktopFlowLazyGridSelection || {};
      marker.browserProgrammaticActionsObserved = true;
      marker.browserProgrammaticStages =
        Array.isArray(marker.browserProgrammaticStages)
          ? marker.browserProgrammaticStages
          : [];
      marker.browserProgrammaticStages.push('scroll-far');
      grid.scrollIntoView({ block: 'center', inline: 'center' });
      grid.scrollToIndex(59);
      return true;
    })()
    """

    private static let returnViewportAction = """
    (() => {
      const grid = document.querySelector('#framework-flow-lazy-grid-selection-grid');
      if (!grid || typeof grid.scrollToIndex !== 'function') return false;
      const marker = window.VaadinDesktopFlowLazyGridSelection || {};
      marker.browserProgrammaticStages.push('scroll-return');
      grid.scrollToIndex(0);
      return true;
    })()
    """

    private static let sortAction = """
    (() => {
      const grid = document.querySelector('#framework-flow-lazy-grid-selection-grid');
      if (!grid) return false;
      const visit = (node) => {
        if (!node) return null;
        if (node.nodeType === Node.ELEMENT_NODE
            && node.localName === 'vaadin-grid-sorter'
            && String(node.textContent || '').includes('Lazy owner')) {
          return node;
        }
        if (node.shadowRoot) {
          const result = visit(node.shadowRoot);
          if (result) return result;
        }
        const children = node.children || node.childNodes || [];
        for (const child of children) {
          const result = visit(child);
          if (result) return result;
        }
        return null;
      };
      const sorter = visit(grid);
      if (!sorter || typeof sorter.click !== 'function') return false;
      const marker = window.VaadinDesktopFlowLazyGridSelection || {};
      marker.browserProgrammaticStages.push('sort-owner');
      sorter.click();
      setTimeout(() => grid.scrollToIndex?.(0), 350);
      return true;
    })()
    """

    private static let filterAction = setFilterAction(
        value: "platform",
        stage: "filter-platform",
        scrollIndex: 19)
    private static let refreshAllOffViewportAction = """
    (() => {
      const grid = document.querySelector('#framework-flow-lazy-grid-selection-grid');
      if (!grid || typeof grid.scrollToIndex !== 'function') return false;
      const marker = window.VaadinDesktopFlowLazyGridSelection || {};
      marker.browserProgrammaticStages.push('scroll-refresh-all-offscreen');
      grid.scrollToIndex(0);
      return true;
    })()
    """
    private static let refreshAllRehydrationAction = """
    (() => {
      const grid = document.querySelector('#framework-flow-lazy-grid-selection-grid');
      if (!grid || typeof grid.scrollToIndex !== 'function') return false;
      const marker = window.VaadinDesktopFlowLazyGridSelection || {};
      marker.browserProgrammaticStages.push('scroll-refresh-all-selected');
      grid.scrollToIndex(18);
      return true;
    })()
    """
    private static let filterResetAction = setFilterAction(
        value: "",
        stage: "filter-reset",
        scrollIndex: 47)

    private static func setFilterAction(
        value: String,
        stage: String,
        scrollIndex: Int
    ) -> String {
        """
        (() => {
          const field = document.querySelector(
            '#framework-flow-lazy-grid-selection-filter');
          const grid = document.querySelector(
            '#framework-flow-lazy-grid-selection-grid');
          if (!field || !grid) return false;
          const marker = window.VaadinDesktopFlowLazyGridSelection || {};
          marker.browserProgrammaticStages.push('\(stage)');
          const value = '\(value)';
          const nativeInput = field.inputElement;
          field.scrollIntoView({ block: 'center', inline: 'center' });
          field.value = value;
          if (nativeInput) {
            const descriptor = Object.getOwnPropertyDescriptor(
              HTMLInputElement.prototype, 'value');
            if (descriptor?.set) {
              descriptor.set.call(nativeInput, value);
            } else {
              nativeInput.value = value;
            }
            nativeInput.dispatchEvent(
              new Event('input', { bubbles: true, composed: true }));
            nativeInput.dispatchEvent(
              new Event('change', { bubbles: true, composed: true }));
          }
          field.dispatchEvent(
            new Event('input', { bubbles: true, composed: true }));
          field.dispatchEvent(new CustomEvent('value-changed', {
            bubbles: true,
            composed: true,
            detail: { value }
          }));
          field.dispatchEvent(
            new Event('change', { bubbles: true, composed: true }));
          setTimeout(() => grid.scrollToIndex?.(\(scrollIndex)), 450);
          return true;
        })()
        """
    }

    private static let finalViewportAction = """
    (() => {
      const grid = document.querySelector('#framework-flow-lazy-grid-selection-grid');
      if (!grid || typeof grid.scrollToIndex !== 'function') return false;
      const marker = window.VaadinDesktopFlowLazyGridSelection || {};
      marker.browserProgrammaticStages.push('scroll-final');
      grid.scrollToIndex(72);
      return true;
    })()
    """

    private static let positionUnknownGridAction = """
    (() => {
      const grid = document.querySelector('#framework-flow-unknown-size-grid');
      if (!grid || typeof grid.scrollToIndex !== 'function') return false;
      const marker = window.VaadinDesktopFlowLazyGridSelection || {};
      marker.browserProgrammaticActionsObserved = true;
      marker.browserProgrammaticStages =
        Array.isArray(marker.browserProgrammaticStages)
          ? marker.browserProgrammaticStages
          : [];
      marker.browserProgrammaticStages.push('position-unknown-grid');
      grid.scrollIntoView({ block: 'center', inline: 'center' });
      grid.scrollToIndex(0);
      return true;
    })()
    """

    private static let unknownEstimateGrowthAction = """
    (() => {
      const grid = document.querySelector('#framework-flow-unknown-size-grid');
      if (!grid || typeof grid.scrollToIndex !== 'function') return false;
      const marker = window.VaadinDesktopFlowLazyGridSelection || {};
      marker.browserProgrammaticStages.push('scroll-unknown-growth');
      grid.scrollToIndex(38);
      return true;
    })()
    """

    private static let unknownExactEndAction = """
    (() => {
      const grid = document.querySelector('#framework-flow-unknown-size-grid');
      if (!grid || typeof grid.scrollToIndex !== 'function') return false;
      const marker = window.VaadinDesktopFlowLazyGridSelection || {};
      marker.browserProgrammaticStages.push('scroll-unknown-end');
      grid.scrollToIndex(66);
      return true;
    })()
    """

    private static let unknownMutationResetTopAction = """
    (() => {
      const grid = document.querySelector('#framework-flow-unknown-size-grid');
      if (!grid || typeof grid.scrollToIndex !== 'function') return false;
      const marker = window.VaadinDesktopFlowLazyGridSelection || {};
      marker.browserProgrammaticStages.push('scroll-unknown-reset-top');
      grid.scrollIntoView({ block: 'center', inline: 'center' });
      grid.scrollToIndex(0);
      return true;
    })()
    """

    private static let unknownMutationRegrowthAction = """
    (() => {
      const grid = document.querySelector('#framework-flow-unknown-size-grid');
      if (!grid || typeof grid.scrollToIndex !== 'function') return false;
      const marker = window.VaadinDesktopFlowLazyGridSelection || {};
      marker.browserProgrammaticStages.push('scroll-unknown-regrowth');
      grid.scrollIntoView({ block: 'center', inline: 'center' });
      grid.scrollToIndex(38);
      return true;
    })()
    """

    private static let unknownMutationSelectionRehydrationAction = """
    (() => {
      const grid = document.querySelector('#framework-flow-unknown-size-grid');
      if (!grid || typeof grid.scrollToIndex !== 'function') return false;
      const marker = window.VaadinDesktopFlowLazyGridSelection || {};
      marker.browserProgrammaticStages.push(
        'scroll-unknown-selection-rehydrate');
      grid.scrollIntoView({ block: 'center', inline: 'center' });
      grid.scrollToIndex(66);
      return true;
    })()
    """

    private static let unknownMutationSecondGrowthAction = """
    (() => {
      const grid = document.querySelector('#framework-flow-unknown-size-grid');
      if (!grid || typeof grid.scrollToIndex !== 'function') return false;
      const marker = window.VaadinDesktopFlowLazyGridSelection || {};
      marker.browserProgrammaticStages.push('scroll-unknown-second-growth');
      grid.scrollIntoView({ block: 'center', inline: 'center' });
      grid.scrollToIndex(78);
      return true;
    })()
    """

    private static let unknownMutationExactEndAction = """
    (() => {
      const grid = document.querySelector('#framework-flow-unknown-size-grid');
      if (!grid || typeof grid.scrollToIndex !== 'function') return false;
      const marker = window.VaadinDesktopFlowLazyGridSelection || {};
      marker.browserProgrammaticStages.push('scroll-unknown-mutated-end');
      grid.scrollIntoView({ block: 'center', inline: 'center' });
      grid.scrollToIndex(92);
      return true;
    })()
    """

    private static let unknownShrinkResetTopAction = """
    (() => {
      const grid = document.querySelector('#framework-flow-unknown-size-grid');
      if (!grid || typeof grid.scrollToIndex !== 'function') return false;
      const marker = window.VaadinDesktopFlowLazyGridSelection || {};
      marker.browserProgrammaticStages.push('scroll-unknown-shrink-reset-top');
      grid.scrollIntoView({ block: 'center', inline: 'center' });
      grid.scrollToIndex(0);
      return true;
    })()
    """

    private static let unknownShrinkRegrowthAction = """
    (() => {
      const grid = document.querySelector('#framework-flow-unknown-size-grid');
      if (!grid || typeof grid.scrollToIndex !== 'function') return false;
      const marker = window.VaadinDesktopFlowLazyGridSelection || {};
      marker.browserProgrammaticStages.push('scroll-unknown-shrink-regrowth');
      grid.scrollIntoView({ block: 'center', inline: 'center' });
      grid.scrollToIndex(38);
      return true;
    })()
    """

    private static let unknownShrinkSelectionRehydrationAction = """
    (() => {
      const grid = document.querySelector('#framework-flow-unknown-size-grid');
      if (!grid || typeof grid.scrollToIndex !== 'function') return false;
      const marker = window.VaadinDesktopFlowLazyGridSelection || {};
      marker.browserProgrammaticStages.push('scroll-unknown-shrink-selection-rehydrate');
      grid.scrollIntoView({ block: 'center', inline: 'center' });
      grid.scrollToIndex(66);
      return true;
    })()
    """

    private static let unknownShrinkExactEndAction = """
    (() => {
      const grid = document.querySelector('#framework-flow-unknown-size-grid');
      if (!grid || typeof grid.scrollToIndex !== 'function') return false;
      const marker = window.VaadinDesktopFlowLazyGridSelection || {};
      marker.browserProgrammaticStages.push('scroll-unknown-shrink-end');
      grid.scrollIntoView({ block: 'center', inline: 'center' });
      grid.scrollToIndex(71);
      return true;
    })()
    """

    private static let prepareRefreshPointerScript = prepareButtonPointerScript(
        buttonId: "framework-flow-lazy-grid-selection-refresh-item",
        positionStage: "position-refresh-item",
        stage: "refresh-L-060",
        label: "refresh-L-060")

    private static let prepareRefreshAllPointerScript = prepareButtonPointerScript(
        buttonId: "framework-flow-lazy-grid-selection-refresh-all",
        positionStage: "position-refresh-all",
        stage: "refresh-all-remove-L-003",
        label: "refresh-all-L-003")

    private static let prepareUnknownTailPointerScript =
        prepareSelectionPointerScript(
            label: "U-067",
            stage: "select-U-067",
            gridId: "framework-flow-unknown-size-grid")

    private static let prepareUnknownMutationPointerScript =
        prepareButtonPointerScript(
            buttonId:
                "framework-flow-unknown-size-grid-mutation-refresh-all",
            positionStage: "position-unknown-mutation",
            stage: "refresh-all-unknown-append",
            label: "refresh-all-unknown-size")

    private static let prepareUnknownShrinkPointerScript =
        prepareButtonPointerScript(
            buttonId: "framework-flow-unknown-size-grid-shrink-refresh-all",
            positionStage: "position-unknown-shrink",
            stage: "refresh-all-unknown-shrink",
            label: "shrink-unknown-size")

    private static func prepareButtonPointerScript(
        buttonId: String,
        positionStage: String,
        stage: String,
        label: String
    ) -> String {
        """
        (() => {
          const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
          const marker = window.VaadinDesktopFlowLazyGridSelection || {};
          window.VaadinDesktopFlowLazyGridSelection = marker;
          const button = document.querySelector('#\(buttonId)');
          if (!button || button.disabled === true) {
            return 'lazy-grid-button-target-unavailable';
          }
          marker.browserProgrammaticActionsObserved = true;
          marker.browserProgrammaticStages =
            Array.isArray(marker.browserProgrammaticStages)
              ? marker.browserProgrammaticStages
              : [];
          if (!marker.browserProgrammaticStages.includes('\(positionStage)')) {
            marker.browserProgrammaticStages.push('\(positionStage)');
          }
          button.scrollIntoView({ block: 'nearest', inline: 'nearest' });
          const rect = button.getBoundingClientRect();
          if (rect.width <= 0 || rect.height <= 0
              || rect.bottom <= 0 || rect.top >= window.innerHeight) {
            return 'lazy-grid-button-target-not-visible';
          }
          if (typeof marker.pendingPointerHandler === 'function') {
            document.removeEventListener(
              'click', marker.pendingPointerHandler, true);
          }
          const handler = (event) => {
            const path = event.composedPath?.() || [];
            if (event.button !== 0 || !path.includes(button)) return;
            document.removeEventListener('click', handler, true);
            marker.pendingPointerHandler = null;
            marker.nativePointerObserved = true;
            marker.nativePointerTrustedObserved =
              marker.nativePointerTrustedObserved === true
                || event.isTrusted === true;
            marker.nativePointerEventCount =
              Number(marker.nativePointerEventCount || 0) + 1;
            marker.nativePointerTrustedEventCount =
              Number(marker.nativePointerTrustedEventCount || 0)
                + (event.isTrusted === true ? 1 : 0);
            marker.nativePointerStages =
              Array.isArray(marker.nativePointerStages)
                ? marker.nativePointerStages
                : [];
            marker.nativePointerLabels =
              Array.isArray(marker.nativePointerLabels)
                ? marker.nativePointerLabels
                : [];
            marker.nativePointerStages.push('\(stage)');
            marker.nativePointerLabels.push('\(label)');
          };
          marker.pendingPointerHandler = handler;
          document.addEventListener('click', handler, true);
          return JSON.stringify({
            centerX: rect.left + rect.width / 2,
            centerY: rect.top + rect.height / 2,
            requestCount: Number(
              diagnostics.standardBundleOpaqueRequestCount || 0),
            responseCount: Number(
              diagnostics.standardBundleOpaqueResponseCount || 0)
          });
        })()
        """
    }

    private static func prepareSelectionPointerScript(
        label: String,
        stage: String,
        gridId: String = "framework-flow-lazy-grid-selection-grid"
    ) -> String {
        """
        (() => {
          const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
          const marker = window.VaadinDesktopFlowLazyGridSelection || {};
          window.VaadinDesktopFlowLazyGridSelection = marker;
          const grid = document.querySelector(
            '#\(gridId)');
          if (!grid) return 'lazy-grid-unavailable';
          const visit = (node, predicate) => {
            if (!node) return null;
            if (node.nodeType === Node.ELEMENT_NODE && predicate(node)) return node;
            if (node.shadowRoot) {
              const shadowResult = visit(node.shadowRoot, predicate);
              if (shadowResult) return shadowResult;
            }
            if (typeof node.assignedElements === 'function') {
              for (const child of node.assignedElements({ flatten: true })) {
                const result = visit(child, predicate);
                if (result) return result;
              }
            }
            const children = node.children || node.childNodes || [];
            for (const child of children) {
              const result = visit(child, predicate);
              if (result) return result;
            }
            return null;
          };
          const cell = visit(grid, (element) =>
            element.localName === 'vaadin-grid-cell-content'
              && String(element.textContent || '').trim() === '\(label)');
          const slot = cell?.assignedSlot;
          const row = slot?.closest?.('tr')
            || Array.from(grid.shadowRoot?.querySelectorAll('tr') || [])
              .find((candidate) => slot && candidate.contains(slot));
          const checkbox = row
            ? visit(row, (element) => element.localName === 'vaadin-checkbox')
            : null;
          if (!cell || !row || !checkbox) {
            return 'lazy-grid-selection-target-unavailable:'
              + 'cell=' + !!cell + ',row=' + !!row + ',checkbox=' + !!checkbox;
          }
          const rect = checkbox.getBoundingClientRect();
          if (rect.width <= 0 || rect.height <= 0
              || rect.bottom <= 0 || rect.top >= window.innerHeight) {
            return 'lazy-grid-selection-target-not-visible';
          }
          if (typeof marker.pendingPointerHandler === 'function') {
            document.removeEventListener(
              'click', marker.pendingPointerHandler, true);
          }
          const handler = (event) => {
            const path = event.composedPath?.() || [];
            if (event.button !== 0 || !path.includes(checkbox)) return;
            document.removeEventListener('click', handler, true);
            marker.pendingPointerHandler = null;
            marker.nativePointerObserved = true;
            marker.nativePointerTrustedObserved =
              marker.nativePointerTrustedObserved === true
                || event.isTrusted === true;
            marker.nativePointerEventCount =
              Number(marker.nativePointerEventCount || 0) + 1;
            marker.nativePointerTrustedEventCount =
              Number(marker.nativePointerTrustedEventCount || 0)
                + (event.isTrusted === true ? 1 : 0);
            marker.nativePointerStages =
              Array.isArray(marker.nativePointerStages)
                ? marker.nativePointerStages
                : [];
            marker.nativePointerLabels =
              Array.isArray(marker.nativePointerLabels)
                ? marker.nativePointerLabels
                : [];
            marker.nativePointerStages.push('\(stage)');
            marker.nativePointerLabels.push('\(label)');
          };
          marker.pendingPointerHandler = handler;
          document.addEventListener('click', handler, true);
          return JSON.stringify({
            centerX: rect.left + rect.width / 2,
            centerY: rect.top + rect.height / 2,
            requestCount: Number(
              diagnostics.standardBundleOpaqueRequestCount || 0),
            responseCount: Number(
              diagnostics.standardBundleOpaqueResponseCount || 0)
          });
        })()
        """
    }
}
