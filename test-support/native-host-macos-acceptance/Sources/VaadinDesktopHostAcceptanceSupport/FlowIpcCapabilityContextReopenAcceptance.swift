import Cocoa
import Foundation
import WebKit

private var flowIpcCapabilityContextReopenAcceptance:
    FlowIpcCapabilityContextReopenAcceptance?

extension WebViewHost {
    func scheduleCapabilityContextReopenAcceptanceIfReady() {
        guard config.flowIpcAutoCapabilityContextReopen,
              config.transport == .flowIpc else {
            return
        }
        if flowIpcCapabilityContextReopenAcceptance == nil {
            let timeout = config.flowIpcAutoShutdownTimeoutSeconds > 0
                ? config.flowIpcAutoShutdownTimeoutSeconds
                : 60
            flowIpcCapabilityContextReopenAcceptance =
                FlowIpcCapabilityContextReopenAcceptance(
                    host: self,
                    deadline: Date().addingTimeInterval(timeout))
        }
        flowIpcCapabilityContextReopenAcceptance?.resume()
    }
}

private final class FlowIpcCapabilityContextReopenAcceptance {
    private enum Phase: String {
        case root
        case mainRoute
        case firstIncarnationReady
        case firstCaptureInvoked
        case firstPushDrain
        case firstCloseInvoked
        case reopenedIncarnationReady
        case reopenedProbeInvoked
        case reopenedPushDrain
        case reopenedCloseInvoked
        case thirdIncarnationReady
        case thirdProbeInvoked
        case finishDrain
    }

    private static let secondaryWindowId = "capability-reopen-secondary"
    private static let secondaryUiId = "capability-reopen-secondary"
    private static let firstTitle = "Flow capability context incarnation A"
    private static let reopenedTitle = "Flow capability context incarnation B"
    private static let thirdTitle = "Flow capability context incarnation C"
    private static let initialWindowStatus =
        "Capability context reopen window: pending"
    private static let initialFirstStatus =
        "Capability context reopen first: pending"
    private static let initialProbeStatus =
        "Capability context reopen probe: pending"
    private static let initialThirdStatus =
        "Capability context reopen third: pending"
    private static let expectedFirstStatus =
        "Capability context reopen first: firstSuccess=true "
        + "capabilityContextCaptured=true backgroundSessionAbsent=true "
        + "client=true change=1"
    private static let expectedProbeStatus =
        "Capability context reopen probe: "
        + "retiredBeforeInvalidated=true "
        + "retiredBeforeReason=owner-window-closed freshSuccess=true "
        + "retiredAfterInvalidated=true "
        + "retiredAfterReason=owner-window-closed "
        + "capabilityContextCaptured=true backgroundSessionAbsent=true "
        + "client=true change=1"
    private static let expectedThirdStatus =
        "Capability context reopen third: "
        + "firstBeforeInvalidated=true "
        + "firstBeforeReason=owner-window-closed "
        + "reopenedBeforeInvalidated=true "
        + "reopenedBeforeReason=owner-window-closed freshSuccess=true "
        + "firstAfterInvalidated=true "
        + "firstAfterReason=owner-window-closed "
        + "reopenedAfterInvalidated=true "
        + "reopenedAfterReason=owner-window-closed "
        + "capabilityContextCaptured=true backgroundSessionAbsent=true "
        + "client=true change=1"

    private let host: WebViewHost
    private let deadline: Date
    private var phase = Phase.root
    private var pollScheduled = false
    private var terminal = false
    private var initialMainWindowIdentifier: ObjectIdentifier?
    private var initialMainWebViewIdentifier: ObjectIdentifier?
    private var initialProcessIdentifier: Int32 = 0
    private var firstWindowIdentifier: ObjectIdentifier?
    private var firstWebViewIdentifier: ObjectIdentifier?
    private var firstWindow: NSWindow?
    private var firstWebView: WKWebView?
    private var reopenedWindowIdentifier: ObjectIdentifier?
    private var reopenedWebViewIdentifier: ObjectIdentifier?
    private var reopenedWindow: NSWindow?
    private var reopenedWebView: WKWebView?
    private var firstSessionId = ""
    private var reopenedSessionId = ""
    private var thirdSessionId = ""
    private var firstPushBaseline = 0
    private var firstPushMessageCount = 0
    private var reopenedPushBaseline = 0
    private var reopenedPushMessageCount = 0
    private var thirdPushBaseline = 0
    private var thirdPushMessageCount = 0
    private var firstRequestCount = 0
    private var firstResponseCount = 0
    private var reopenedRequestCount = 0
    private var reopenedResponseCount = 0
    private var firstContextReleasedObserved = false
    private var firstWindowHiddenObserved = false
    private var reopenedContextReleasedObserved = false
    private var reopenedWindowHiddenObserved = false
    private var samePublicIdObserved = false
    private var differentSessionObserved = false
    private var windowIdentityRecreatedObserved = false
    private var webViewIdentityRecreatedObserved = false
    private var threeDistinctSessionsObserved = false
    private var threeDistinctWindowIdentitiesObserved = false
    private var threeDistinctWebViewIdentitiesObserved = false
    private var firstCaptureSuccessObserved = false
    private var retiredBeforeInvalidatedObserved = false
    private var retiredAfterInvalidatedObserved = false
    private var freshSuccessObserved = false
    private var firstBeforeInvalidatedObserved = false
    private var reopenedBeforeInvalidatedObserved = false
    private var firstAfterInvalidatedObserved = false
    private var reopenedAfterInvalidatedObserved = false
    private var thirdFreshSuccessObserved = false
    private var firstBackgroundSessionAbsentObserved = false
    private var reopenedBackgroundSessionAbsentObserved = false
    private var thirdBackgroundSessionAbsentObserved = false
    private var mainWindowRetainedObserved = false
    private var mainWebViewRetainedObserved = false
    private var mainProcessRetainedObserved = false
    private var nativePointerStages: [String] = []
    private var nativePointerReceiptStages: [String] = []
    private var nativePointerTrustedStages: [String] = []
    private var browserProgrammaticStages: [String] = []
    private var firstDrainKey = ""
    private var firstDrainBalancedPolls = 0
    private var reopenedDrainKey = ""
    private var reopenedDrainBalancedPolls = 0
    private var drainKey = ""
    private var drainBalancedPolls = 0

    init(host: WebViewHost, deadline: Date) {
        self.host = host
        self.deadline = deadline
    }

    func resume() {
        schedulePoll(after: 0.1)
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
            retryOrFail(code: "capabilityContextReopenMainWindowTimeout")
            return
        }
        inspect(main) { mainSnapshot in
            guard let mainSnapshot else {
                self.retryOrFail(
                    code: "capabilityContextReopenMainInspectionFailed")
                return
            }
            guard mainSnapshot.errorFree else {
                self.fail(
                    code: "capabilityContextReopenMainRuntimeError",
                    details: mainSnapshot.tracePayload())
                return
            }
            guard let secondary = self.host.flowIpcWindowContext(
                Self.secondaryWindowId) else {
                self.advance(
                    main: main,
                    mainSnapshot: mainSnapshot,
                    secondary: nil,
                    secondarySnapshot: nil)
                return
            }
            guard secondary.navigationFinished else {
                self.retryOrFail(
                    code: "capabilityContextReopenSecondaryNavigationTimeout")
                return
            }
            self.inspect(secondary) { secondarySnapshot in
                guard let secondarySnapshot else {
                    self.retryOrFail(
                        code: "capabilityContextReopenSecondaryInspectionFailed")
                    return
                }
                guard secondarySnapshot.errorFree else {
                    self.fail(
                        code: "capabilityContextReopenSecondaryRuntimeError",
                        details: secondarySnapshot.tracePayload())
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
        switch phase {
        case .root:
            guard mainSnapshot.rootRendered,
                  mainSnapshot.firstMeaningfulPaint,
                  mainSnapshot.flowClientCount > 0,
                  main.recoveryGeneration == 0 else {
                retryOrFail(
                    code: "capabilityContextReopenInitialRootTimeout",
                    details: mainSnapshot.tracePayload())
                return
            }
            initialMainWindowIdentifier = ObjectIdentifier(main.window)
            initialMainWebViewIdentifier = ObjectIdentifier(main.webView)
            initialProcessIdentifier = host.sidecarProcessIdentifier ?? 0
            guard initialProcessIdentifier > 0 else {
                retryOrFail(
                    code: "capabilityContextReopenInitialProcessTimeout")
                return
            }
            dispatchBrowserClick(
                in: main,
                buttonId: "framework-flow-capability-context-reopen-open-route",
                stage: "route-navigate",
                next: .mainRoute,
                failureCode: "capabilityContextReopenNavigationFailed")
        case .mainRoute:
            guard mainSnapshot.routeReady,
                  mainSnapshot.windowStatus == Self.initialWindowStatus,
                  mainSnapshot.firstStatus == Self.initialFirstStatus,
                  mainSnapshot.probeStatus == Self.initialProbeStatus,
                  mainSnapshot.thirdStatus == Self.initialThirdStatus else {
                retryOrFail(
                    code: "capabilityContextReopenMainRouteTimeout",
                    details: mainSnapshot.tracePayload())
                return
            }
            dispatchBrowserClick(
                in: main,
                buttonId: "framework-flow-capability-context-reopen-open-window",
                stage: "open-a",
                next: .firstIncarnationReady,
                failureCode: "capabilityContextReopenFirstOpenFailed")
        case .firstIncarnationReady:
            guard mainSnapshot.routeReady,
                  mainSnapshot.windowStatus.contains("opened=true"),
                  let secondary,
                  let secondarySnapshot,
                  secondarySnapshot.routeReady,
                  secondarySnapshot.firstStatus == Self.initialFirstStatus,
                  secondarySnapshot.probeStatus == Self.initialProbeStatus,
                  secondarySnapshot.thirdStatus == Self.initialThirdStatus,
                  exactSecondaryScope(secondarySnapshot),
                  opaqueSecondarySession(secondarySnapshot.sessionId) else {
                retryOrFail(
                    code: "capabilityContextReopenFirstReadyTimeout",
                    details: mainSnapshot.tracePayload())
                return
            }
            firstSessionId = secondary.scope.sessionId
            guard secondarySnapshot.sessionId == firstSessionId else {
                fail(
                    code: "capabilityContextReopenFirstScopeMismatch",
                    details: secondarySnapshot.tracePayload())
                return
            }
            firstWindowIdentifier = ObjectIdentifier(secondary.window)
            firstWebViewIdentifier = ObjectIdentifier(secondary.webView)
            firstWindow = secondary.window
            firstWebView = secondary.webView
            firstPushBaseline = secondarySnapshot.pushMessageCount
            performNativeClick(
                in: secondary,
                buttonId: "framework-flow-capability-context-reopen-capture-first",
                stage: "first-capture",
                next: .firstCaptureInvoked,
                failureCode: "capabilityContextReopenFirstCaptureFailed")
        case .firstCaptureInvoked:
            guard let secondary,
                  let secondarySnapshot,
                  secondary.scope.sessionId == firstSessionId else {
                retryOrFail(
                    code: "capabilityContextReopenFirstLostBeforeClose")
                return
            }
            recordPointer(stage: "first-capture", snapshot: secondarySnapshot)
            guard pointerObserved(
                    stage: "first-capture",
                    snapshot: secondarySnapshot),
                  secondarySnapshot.firstStatus == Self.expectedFirstStatus,
                  secondary.window.title == Self.firstTitle,
                  secondarySnapshot.pushMessageCount > firstPushBaseline else {
                retryOrFail(
                    code: "capabilityContextReopenFirstResultTimeout",
                    details: secondarySnapshot.tracePayload())
                return
            }
            firstCaptureSuccessObserved = true
            firstBackgroundSessionAbsentObserved = true
            firstPushMessageCount = secondarySnapshot.pushMessageCount
            firstRequestCount = secondarySnapshot.requestCount
            firstResponseCount = secondarySnapshot.responseCount
            phase = .firstPushDrain
            schedulePoll(after: 0.3)
        case .firstPushDrain:
            guard let secondary,
                  let secondarySnapshot,
                  secondary.scope.sessionId == firstSessionId,
                  secondarySnapshot.firstStatus == Self.expectedFirstStatus,
                  secondary.window.title == Self.firstTitle,
                  secondarySnapshot.pushMessageCount > firstPushBaseline else {
                retryOrFail(
                    code: "capabilityContextReopenFirstPushDrainTimeout",
                    details: secondarySnapshot?.tracePayload()
                        ?? mainSnapshot.tracePayload(),
                    delay: 0.3)
                return
            }
            let firstBalanced = secondarySnapshot.requestCount
                == secondarySnapshot.responseCount
            let firstKey = "\(secondarySnapshot.requestCount):"
                + "\(secondarySnapshot.responseCount):"
                + "\(secondarySnapshot.pushMessageCount)"
            if firstBalanced {
                if firstDrainKey == firstKey {
                    firstDrainBalancedPolls += 1
                } else {
                    firstDrainKey = firstKey
                    firstDrainBalancedPolls = 1
                }
            } else {
                firstDrainKey = ""
                firstDrainBalancedPolls = 0
            }
            guard firstDrainBalancedPolls >= 2 else {
                retryOrFail(
                    code: "capabilityContextReopenFirstPushDrainTimeout",
                    details: secondarySnapshot.tracePayload(),
                    delay: 0.3)
                return
            }
            firstPushMessageCount = secondarySnapshot.pushMessageCount
            firstRequestCount = secondarySnapshot.requestCount
            firstResponseCount = secondarySnapshot.responseCount
            performNativeClick(
                in: main,
                buttonId: "framework-flow-capability-context-reopen-close-window",
                stage: "close-a",
                next: .firstCloseInvoked,
                failureCode: "capabilityContextReopenFirstCloseFailed")
        case .firstCloseInvoked:
            recordPointer(stage: "close-a", snapshot: mainSnapshot)
            guard pointerObserved(stage: "close-a", snapshot: mainSnapshot),
                  mainSnapshot.windowStatus.contains("closing=true") else {
                retryOrFail(
                    code: "capabilityContextReopenFirstCloseResultTimeout",
                    details: mainSnapshot.tracePayload())
                return
            }
            guard secondary == nil else {
                retryOrFail(
                    code: "capabilityContextReopenFirstReleaseTimeout")
                return
            }
            firstContextReleasedObserved = true
            firstWindowHiddenObserved = firstWindow != nil
                && firstWindow?.isVisible == false
            mainWindowRetainedObserved = initialMainWindowIdentifier
                == ObjectIdentifier(main.window)
            mainWebViewRetainedObserved = initialMainWebViewIdentifier
                == ObjectIdentifier(main.webView)
            mainProcessRetainedObserved = initialProcessIdentifier > 0
                && host.sidecarProcessIdentifier == initialProcessIdentifier
                && main.recoveryGeneration == 0
            guard firstWindowHiddenObserved,
                  mainWindowRetainedObserved,
                  mainWebViewRetainedObserved,
                  mainProcessRetainedObserved else {
                fail(
                    code: "capabilityContextReopenFirstReleaseContinuityFailed",
                    details: [
                        "firstWindowHidden": firstWindowHiddenObserved,
                        "mainWindowRetained": mainWindowRetainedObserved,
                        "mainWebViewRetained": mainWebViewRetainedObserved,
                        "mainProcessRetained": mainProcessRetainedObserved
                    ])
                return
            }
            dispatchBrowserClick(
                in: main,
                buttonId: "framework-flow-capability-context-reopen-open-window",
                stage: "reopen-b",
                next: .reopenedIncarnationReady,
                failureCode: "capabilityContextReopenSecondOpenFailed")
        case .reopenedIncarnationReady:
            guard let secondary,
                  let secondarySnapshot,
                  secondarySnapshot.routeReady,
                  secondarySnapshot.firstStatus == Self.initialFirstStatus,
                  secondarySnapshot.probeStatus == Self.initialProbeStatus,
                  secondarySnapshot.thirdStatus == Self.initialThirdStatus,
                  exactSecondaryScope(secondarySnapshot),
                  opaqueSecondarySession(secondarySnapshot.sessionId) else {
                retryOrFail(
                    code: "capabilityContextReopenSecondReadyTimeout",
                    details: mainSnapshot.tracePayload())
                return
            }
            reopenedSessionId = secondary.scope.sessionId
            samePublicIdObserved = secondary.scope.windowId
                == Self.secondaryWindowId
                && secondary.scope.uiId == Self.secondaryUiId
                && secondarySnapshot.windowId == Self.secondaryWindowId
            differentSessionObserved = !firstSessionId.isEmpty
                && reopenedSessionId != firstSessionId
            windowIdentityRecreatedObserved = firstWindowIdentifier
                != ObjectIdentifier(secondary.window)
            webViewIdentityRecreatedObserved = firstWebViewIdentifier
                != ObjectIdentifier(secondary.webView)
            guard secondarySnapshot.sessionId == reopenedSessionId,
                  samePublicIdObserved,
                  differentSessionObserved,
                  windowIdentityRecreatedObserved,
                  webViewIdentityRecreatedObserved else {
                fail(
                    code: "capabilityContextReopenIncarnationIdentityFailed",
                    details: [
                        "firstSessionId": firstSessionId,
                        "reopenedSessionId": reopenedSessionId,
                        "samePublicId": samePublicIdObserved,
                        "differentSession": differentSessionObserved,
                        "windowIdentityRecreated":
                            windowIdentityRecreatedObserved,
                        "webViewIdentityRecreated":
                            webViewIdentityRecreatedObserved
                    ])
                return
            }
            reopenedWindowIdentifier = ObjectIdentifier(secondary.window)
            reopenedWebViewIdentifier = ObjectIdentifier(secondary.webView)
            reopenedWindow = secondary.window
            reopenedWebView = secondary.webView
            reopenedPushBaseline = secondarySnapshot.pushMessageCount
            performNativeClick(
                in: secondary,
                buttonId: "framework-flow-capability-context-reopen-probe-reopened",
                stage: "reopened-probe",
                next: .reopenedProbeInvoked,
                failureCode: "capabilityContextReopenSecondProbeFailed")
        case .reopenedProbeInvoked:
            guard let secondary,
                  let secondarySnapshot,
                  secondary.scope.sessionId == reopenedSessionId else {
                retryOrFail(
                    code: "capabilityContextReopenSecondLostBeforeResult")
                return
            }
            recordPointer(
                stage: "reopened-probe",
                snapshot: secondarySnapshot)
            guard pointerObserved(
                    stage: "reopened-probe",
                    snapshot: secondarySnapshot),
                  secondarySnapshot.probeStatus == Self.expectedProbeStatus,
                  secondary.window.title == Self.reopenedTitle,
                  secondarySnapshot.pushMessageCount
                    > reopenedPushBaseline else {
                retryOrFail(
                    code: "capabilityContextReopenSecondResultTimeout",
                    details: secondarySnapshot.tracePayload())
                return
            }
            retiredBeforeInvalidatedObserved = true
            freshSuccessObserved = true
            retiredAfterInvalidatedObserved = true
            reopenedBackgroundSessionAbsentObserved = true
            reopenedPushMessageCount = secondarySnapshot.pushMessageCount
            phase = .reopenedPushDrain
            schedulePoll(after: 0.3)
        case .reopenedPushDrain:
            guard let secondary,
                  let secondarySnapshot,
                  secondary.scope.sessionId == reopenedSessionId,
                  secondarySnapshot.probeStatus == Self.expectedProbeStatus,
                  secondary.window.title == Self.reopenedTitle,
                  secondarySnapshot.pushMessageCount
                    > reopenedPushBaseline else {
                retryOrFail(
                    code: "capabilityContextReopenSecondPushDrainTimeout",
                    details: secondarySnapshot?.tracePayload()
                        ?? mainSnapshot.tracePayload(),
                    delay: 0.3)
                return
            }
            let reopenedBalanced = secondarySnapshot.requestCount
                == secondarySnapshot.responseCount
            let reopenedKey = "\(secondarySnapshot.requestCount):"
                + "\(secondarySnapshot.responseCount):"
                + "\(secondarySnapshot.pushMessageCount)"
            if reopenedBalanced {
                if reopenedDrainKey == reopenedKey {
                    reopenedDrainBalancedPolls += 1
                } else {
                    reopenedDrainKey = reopenedKey
                    reopenedDrainBalancedPolls = 1
                }
            } else {
                reopenedDrainKey = ""
                reopenedDrainBalancedPolls = 0
            }
            guard reopenedDrainBalancedPolls >= 2 else {
                retryOrFail(
                    code: "capabilityContextReopenSecondPushDrainTimeout",
                    details: secondarySnapshot.tracePayload(),
                    delay: 0.3)
                return
            }
            reopenedPushMessageCount = secondarySnapshot.pushMessageCount
            reopenedRequestCount = secondarySnapshot.requestCount
            reopenedResponseCount = secondarySnapshot.responseCount
            performNativeClick(
                in: main,
                buttonId: "framework-flow-capability-context-reopen-close-window",
                stage: "close-b",
                next: .reopenedCloseInvoked,
                failureCode: "capabilityContextReopenSecondCloseFailed")
        case .reopenedCloseInvoked:
            recordPointer(stage: "close-b", snapshot: mainSnapshot)
            guard pointerObserved(stage: "close-b", snapshot: mainSnapshot),
                  mainSnapshot.windowStatus.contains("closing=true") else {
                retryOrFail(
                    code: "capabilityContextReopenSecondCloseResultTimeout",
                    details: mainSnapshot.tracePayload())
                return
            }
            guard secondary == nil else {
                retryOrFail(
                    code: "capabilityContextReopenSecondReleaseTimeout")
                return
            }
            reopenedContextReleasedObserved = true
            reopenedWindowHiddenObserved = reopenedWindow != nil
                && reopenedWindow?.isVisible == false
            mainWindowRetainedObserved = initialMainWindowIdentifier
                == ObjectIdentifier(main.window)
            mainWebViewRetainedObserved = initialMainWebViewIdentifier
                == ObjectIdentifier(main.webView)
            mainProcessRetainedObserved = initialProcessIdentifier > 0
                && host.sidecarProcessIdentifier == initialProcessIdentifier
                && main.recoveryGeneration == 0
            guard reopenedWindowHiddenObserved,
                  firstWindow?.isVisible == false,
                  firstWindow?.title == Self.firstTitle,
                  reopenedWindow?.title == Self.reopenedTitle,
                  mainWindowRetainedObserved,
                  mainWebViewRetainedObserved,
                  mainProcessRetainedObserved else {
                fail(
                    code: "capabilityContextReopenSecondReleaseContinuityFailed",
                    details: [
                        "firstWindowHidden": firstWindow?.isVisible == false,
                        "reopenedWindowHidden": reopenedWindowHiddenObserved,
                        "mainWindowRetained": mainWindowRetainedObserved,
                        "mainWebViewRetained": mainWebViewRetainedObserved,
                        "mainProcessRetained": mainProcessRetainedObserved
                    ])
                return
            }
            dispatchBrowserClick(
                in: main,
                buttonId: "framework-flow-capability-context-reopen-open-window",
                stage: "reopen-c",
                next: .thirdIncarnationReady,
                failureCode: "capabilityContextReopenThirdOpenFailed")
        case .thirdIncarnationReady:
            guard let secondary,
                  let secondarySnapshot,
                  secondarySnapshot.routeReady,
                  secondarySnapshot.firstStatus == Self.initialFirstStatus,
                  secondarySnapshot.probeStatus == Self.initialProbeStatus,
                  secondarySnapshot.thirdStatus == Self.initialThirdStatus,
                  exactSecondaryScope(secondarySnapshot),
                  opaqueSecondarySession(secondarySnapshot.sessionId) else {
                retryOrFail(
                    code: "capabilityContextReopenThirdReadyTimeout",
                    details: mainSnapshot.tracePayload())
                return
            }
            thirdSessionId = secondary.scope.sessionId
            let thirdWindowIdentifier = ObjectIdentifier(secondary.window)
            let thirdWebViewIdentifier = ObjectIdentifier(secondary.webView)
            threeDistinctSessionsObserved = !firstSessionId.isEmpty
                && !reopenedSessionId.isEmpty
                && thirdSessionId != firstSessionId
                && thirdSessionId != reopenedSessionId
                && firstSessionId != reopenedSessionId
            threeDistinctWindowIdentitiesObserved = firstWindow != nil
                && reopenedWindow != nil
                && firstWindowIdentifier != reopenedWindowIdentifier
                && firstWindowIdentifier != thirdWindowIdentifier
                && reopenedWindowIdentifier != thirdWindowIdentifier
            threeDistinctWebViewIdentitiesObserved = firstWebView != nil
                && reopenedWebView != nil
                && firstWebViewIdentifier != reopenedWebViewIdentifier
                && firstWebViewIdentifier != thirdWebViewIdentifier
                && reopenedWebViewIdentifier != thirdWebViewIdentifier
            samePublicIdObserved = samePublicIdObserved
                && secondary.scope.windowId == Self.secondaryWindowId
                && secondary.scope.uiId == Self.secondaryUiId
                && secondarySnapshot.windowId == Self.secondaryWindowId
            mainWindowRetainedObserved = initialMainWindowIdentifier
                == ObjectIdentifier(main.window)
            mainWebViewRetainedObserved = initialMainWebViewIdentifier
                == ObjectIdentifier(main.webView)
            mainProcessRetainedObserved = initialProcessIdentifier > 0
                && host.sidecarProcessIdentifier == initialProcessIdentifier
                && main.recoveryGeneration == 0
            guard secondarySnapshot.sessionId == thirdSessionId,
                  samePublicIdObserved,
                  threeDistinctSessionsObserved,
                  threeDistinctWindowIdentitiesObserved,
                  threeDistinctWebViewIdentitiesObserved,
                  firstWindow?.isVisible == false,
                  reopenedWindow?.isVisible == false,
                  mainWindowRetainedObserved,
                  mainWebViewRetainedObserved,
                  mainProcessRetainedObserved else {
                fail(
                    code: "capabilityContextReopenThirdIdentityFailed",
                    details: [
                        "firstSessionId": firstSessionId,
                        "reopenedSessionId": reopenedSessionId,
                        "thirdSessionId": thirdSessionId,
                        "samePublicId": samePublicIdObserved,
                        "threeDistinctSessions":
                            threeDistinctSessionsObserved,
                        "threeDistinctWindowIdentities":
                            threeDistinctWindowIdentitiesObserved,
                        "threeDistinctWebViewIdentities":
                            threeDistinctWebViewIdentitiesObserved
                    ])
                return
            }
            thirdPushBaseline = secondarySnapshot.pushMessageCount
            performNativeClick(
                in: secondary,
                buttonId: "framework-flow-capability-context-reopen-probe-third",
                stage: "third-probe",
                next: .thirdProbeInvoked,
                failureCode: "capabilityContextReopenThirdProbeFailed")
        case .thirdProbeInvoked:
            guard let secondary,
                  let secondarySnapshot,
                  secondary.scope.sessionId == thirdSessionId else {
                retryOrFail(
                    code: "capabilityContextReopenThirdLostBeforeResult")
                return
            }
            recordPointer(stage: "third-probe", snapshot: secondarySnapshot)
            guard pointerObserved(
                    stage: "third-probe",
                    snapshot: secondarySnapshot),
                  secondarySnapshot.thirdStatus == Self.expectedThirdStatus,
                  secondary.window.title == Self.thirdTitle,
                  secondarySnapshot.pushMessageCount > thirdPushBaseline else {
                retryOrFail(
                    code: "capabilityContextReopenThirdResultTimeout",
                    details: secondarySnapshot.tracePayload())
                return
            }
            firstBeforeInvalidatedObserved = true
            reopenedBeforeInvalidatedObserved = true
            thirdFreshSuccessObserved = true
            firstAfterInvalidatedObserved = true
            reopenedAfterInvalidatedObserved = true
            thirdBackgroundSessionAbsentObserved = true
            thirdPushMessageCount = secondarySnapshot.pushMessageCount
            phase = .finishDrain
            schedulePoll(after: 0.5)
        case .finishDrain:
            guard let secondary,
                  let secondarySnapshot,
                  secondary.scope.sessionId == thirdSessionId,
                  secondarySnapshot.thirdStatus == Self.expectedThirdStatus,
                  secondary.window.title == Self.thirdTitle,
                  firstWindow?.title == Self.firstTitle,
                  firstWindow?.isVisible == false,
                  reopenedWindow?.title == Self.reopenedTitle,
                  reopenedWindow?.isVisible == false,
                  secondarySnapshot.pushMessageCount > thirdPushBaseline,
                  initialMainWindowIdentifier == ObjectIdentifier(main.window),
                  initialMainWebViewIdentifier == ObjectIdentifier(main.webView),
                  initialProcessIdentifier > 0,
                  host.sidecarProcessIdentifier == initialProcessIdentifier,
                  main.recoveryGeneration == 0 else {
                fail(
                    code: "capabilityContextReopenResultChangedDuringDrain",
                    details: secondarySnapshot?.tracePayload()
                        ?? mainSnapshot.tracePayload())
                return
            }
            let balanced = mainSnapshot.requestCount
                == mainSnapshot.responseCount
                && secondarySnapshot.requestCount
                == secondarySnapshot.responseCount
                && firstRequestCount == firstResponseCount
                && reopenedRequestCount == reopenedResponseCount
            let key = "\(mainSnapshot.requestCount):"
                + "\(mainSnapshot.responseCount):"
                + "\(secondarySnapshot.requestCount):"
                + "\(secondarySnapshot.responseCount):"
                + "\(secondarySnapshot.pushMessageCount)"
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
                    code: "capabilityContextReopenDrainTimeout",
                    details: [
                        "mainRequestCount": mainSnapshot.requestCount,
                        "mainResponseCount": mainSnapshot.responseCount,
                        "reopenedRequestCount": reopenedRequestCount,
                        "reopenedResponseCount": reopenedResponseCount,
                        "thirdRequestCount": secondarySnapshot.requestCount,
                        "thirdResponseCount": secondarySnapshot.responseCount,
                        "firstRequestCount": firstRequestCount,
                        "firstResponseCount": firstResponseCount,
                        "balancedPolls": drainBalancedPolls
                    ],
                    delay: 0.5)
                return
            }
            thirdPushMessageCount = secondarySnapshot.pushMessageCount
            finish(
                main: main,
                mainSnapshot: mainSnapshot,
                third: secondary,
                thirdSnapshot: secondarySnapshot)
        }
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
                            "windowX": point.x,
                            "windowY": point.y,
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
                trace(
                    "recv",
                    "flowCapabilityContextReopen.nativePointerDispatch",
                    [
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
                        "uiId": context.scope.uiId
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

    private func exactSecondaryScope(_ snapshot: Snapshot) -> Bool {
        snapshot.windowId == Self.secondaryWindowId
            && snapshot.uiId == Self.secondaryUiId
    }

    private func opaqueSecondarySession(_ sessionId: String) -> Bool {
        let prefix = "flow-ipc-window:"
        guard sessionId.hasPrefix(prefix) else { return false }
        return UUID(uuidString: String(sessionId.dropFirst(prefix.count))) != nil
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

    private func fail(code: String, details: [String: Any] = [:]) {
        guard !terminal else { return }
        terminal = true
        var payload = details
        payload["code"] = code
        payload["phase"] = phase.rawValue
        trace("send", "error", payload)
        host.flowIpcWindowContext(Self.secondaryWindowId)?.window.close()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            self.host.flowIpcWindowContext("main")?.window.close()
        }
    }

    private func finish(
        main: FlowIpcWindowContext,
        mainSnapshot: Snapshot,
        third: FlowIpcWindowContext,
        thirdSnapshot: Snapshot
    ) {
        let expectedPointerStages = [
            "first-capture",
            "close-a",
            "reopened-probe",
            "close-b",
            "third-probe"
        ]
        let expectedBrowserStages = [
            "route-navigate",
            "open-a",
            "reopen-b",
            "reopen-c"
        ]
        let nativePointerObserved = nativePointerStages
            == expectedPointerStages
            && nativePointerReceiptStages == expectedPointerStages
        let nativePointerTrustedObserved = nativePointerTrustedStages
            == expectedPointerStages
        let browserProgrammaticSetupObserved = browserProgrammaticStages
            == expectedBrowserStages
        let payload: [String: Any] = [
            "complete": true,
            "standardBundleMode": thirdSnapshot.firstMeaningfulPaint,
            "rootRendered": thirdSnapshot.rootRendered,
            "flowClientOwned": thirdSnapshot.flowClientCount > 0,
            "samePublicId": samePublicIdObserved,
            "differentSession": differentSessionObserved,
            "threeDistinctSessionsObserved":
                threeDistinctSessionsObserved,
            "threeDistinctWindowIdentitiesObserved":
                threeDistinctWindowIdentitiesObserved,
            "threeDistinctWebViewIdentitiesObserved":
                threeDistinctWebViewIdentitiesObserved,
            "firstWindowId": Self.secondaryWindowId,
            "firstSessionId": firstSessionId,
            "firstUiId": Self.secondaryUiId,
            "reopenedWindowId": Self.secondaryWindowId,
            "reopenedSessionId": reopenedSessionId,
            "reopenedUiId": Self.secondaryUiId,
            "thirdWindowId": Self.secondaryWindowId,
            "thirdSessionId": thirdSessionId,
            "thirdUiId": Self.secondaryUiId,
            "firstContextReleasedObserved": firstContextReleasedObserved,
            "firstWindowHiddenObserved": firstWindowHiddenObserved,
            "reopenedContextReleasedObserved":
                reopenedContextReleasedObserved,
            "reopenedWindowHiddenObserved": reopenedWindowHiddenObserved,
            "windowIdentityRecreatedObserved":
                windowIdentityRecreatedObserved,
            "webViewIdentityRecreatedObserved":
                webViewIdentityRecreatedObserved,
            "firstCaptureSuccess": firstCaptureSuccessObserved,
            "retiredBeforeInvalidated":
                retiredBeforeInvalidatedObserved,
            "retiredBeforeReason": "owner-window-closed",
            "freshSuccess": freshSuccessObserved,
            "retiredAfterInvalidated": retiredAfterInvalidatedObserved,
            "retiredAfterReason": "owner-window-closed",
            "firstBeforeInvalidated": firstBeforeInvalidatedObserved,
            "firstBeforeReason": "owner-window-closed",
            "reopenedBeforeInvalidated":
                reopenedBeforeInvalidatedObserved,
            "reopenedBeforeReason": "owner-window-closed",
            "thirdFreshSuccess": thirdFreshSuccessObserved,
            "firstAfterInvalidated": firstAfterInvalidatedObserved,
            "firstAfterReason": "owner-window-closed",
            "reopenedAfterInvalidated":
                reopenedAfterInvalidatedObserved,
            "reopenedAfterReason": "owner-window-closed",
            "backgroundSessionAbsent":
                firstBackgroundSessionAbsentObserved
                && reopenedBackgroundSessionAbsentObserved
                && thirdBackgroundSessionAbsentObserved,
            "firstBackgroundSessionAbsent":
                firstBackgroundSessionAbsentObserved,
            "reopenedBackgroundSessionAbsent":
                reopenedBackgroundSessionAbsentObserved,
            "thirdBackgroundSessionAbsent":
                thirdBackgroundSessionAbsentObserved,
            "firstPushBaseline": firstPushBaseline,
            "firstPushMessageCount": firstPushMessageCount,
            "reopenedPushBaseline": reopenedPushBaseline,
            "reopenedPushMessageCount": reopenedPushMessageCount,
            "thirdPushBaseline": thirdPushBaseline,
            "thirdPushMessageCount": thirdPushMessageCount,
            "firstRequestCount": firstRequestCount,
            "firstResponseCount": firstResponseCount,
            "mainRequestCount": mainSnapshot.requestCount,
            "mainResponseCount": mainSnapshot.responseCount,
            "reopenedRequestCount": reopenedRequestCount,
            "reopenedResponseCount": reopenedResponseCount,
            "thirdRequestCount": thirdSnapshot.requestCount,
            "thirdResponseCount": thirdSnapshot.responseCount,
            "firstStatus": Self.expectedFirstStatus,
            "reopenedStatus": Self.expectedProbeStatus,
            "thirdStatus": Self.expectedThirdStatus,
            "firstTitle": Self.firstTitle,
            "reopenedTitle": Self.reopenedTitle,
            "thirdTitle": Self.thirdTitle,
            "mainWindowRetainedObserved": mainWindowRetainedObserved,
            "mainWebViewRetainedObserved": mainWebViewRetainedObserved,
            "mainProcessRetainedObserved": mainProcessRetainedObserved,
            "nativePointerObserved": nativePointerObserved,
            "nativePointerTrustedObserved": nativePointerTrustedObserved,
            "nativePointerEventCount": nativePointerStages.count,
            "nativePointerTrustedEventCount":
                nativePointerTrustedStages.count,
            "nativePointerStages": nativePointerStages.joined(separator: ","),
            "browserProgrammaticSetupObserved":
                browserProgrammaticSetupObserved,
            "browserProgrammaticStages":
                browserProgrammaticStages.joined(separator: ","),
            "balancedDrainObserved": drainBalancedPolls >= 2,
            "resultStableAfterDrain":
                thirdSnapshot.thirdStatus == Self.expectedThirdStatus,
            "errorFree": mainSnapshot.errorFree
                && thirdSnapshot.errorFree,
            "initialProcessIdentifier": initialProcessIdentifier,
            "currentProcessIdentifier":
                host.sidecarProcessIdentifier ?? 0,
            "recoveryGeneration": third.recoveryGeneration
        ]
        terminal = true
        trace(
            "recv",
            "flowCapabilityContextReopen.frameworkAcceptance",
            payload)
        third.window.close()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            main.window.close()
        }
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
        let windowStatus: String
        let firstStatus: String
        let probeStatus: String
        let thirdStatus: String
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

        func tracePayload() -> [String: Any] {
            [
                "windowId": windowId,
                "sessionId": sessionId,
                "uiId": uiId,
                "rootRendered": rootRendered,
                "firstMeaningfulPaint": firstMeaningfulPaint,
                "routeReady": routeReady,
                "windowStatus": windowStatus,
                "firstStatus": firstStatus,
                "probeStatus": probeStatus,
                "thirdStatus": thirdStatus,
                "requestCount": requestCount,
                "responseCount": responseCount,
                "pushMessageCount": pushMessageCount,
                "flowClientCount": flowClientCount,
                "pointerStages": pointerStages.keys.sorted()
                    .joined(separator: ","),
                "browserError": browserError,
                "requestError": requestError,
                "responseError": responseError,
                "pushError": pushError
            ]
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
          const marker = window.VaadinDesktopCapabilityContextReopenPointer ||=
            {stages:{}, pendingHandlers:{}};
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
        '#framework-flow-capability-context-reopen-route');
      const clients = window.Vaadin?.Flow?.clients
        ? Object.keys(window.Vaadin.Flow.clients)
          .filter((name) => name !== 'TypeScript')
        : [];
      const pointer =
        window.VaadinDesktopCapabilityContextReopenPointer || {stages:{}};
      return JSON.stringify({
        windowId: String(context.windowId || ''),
        sessionId: String(context.sessionId || ''),
        uiId: String(context.uiId || ''),
        rootRendered: !!document.querySelector('#framework-root') || !!route,
        firstMeaningfulPaint:
          diagnostics.standardBundleFirstMeaningfulPaintReported === true,
        routeReady:
          window.location.pathname.endsWith(
            '/flow-capability-context-reopen')
          && !!route
          && !!document.querySelector(
            '#framework-flow-capability-context-reopen-open-window')
          && !!document.querySelector(
            '#framework-flow-capability-context-reopen-close-window')
          && !!document.querySelector(
            '#framework-flow-capability-context-reopen-capture-first')
          && !!document.querySelector(
            '#framework-flow-capability-context-reopen-probe-reopened')
          && !!document.querySelector(
            '#framework-flow-capability-context-reopen-probe-third'),
        windowStatus: String(document.querySelector(
          '#framework-flow-capability-context-reopen-window-status')
          ?.textContent || ''),
        firstStatus: String(document.querySelector(
          '#framework-flow-capability-context-reopen-first-status')
          ?.textContent || ''),
        probeStatus: String(document.querySelector(
          '#framework-flow-capability-context-reopen-reopen-status')
          ?.textContent || ''),
        thirdStatus: String(document.querySelector(
          '#framework-flow-capability-context-reopen-third-status')
          ?.textContent || ''),
        requestCount: Number(
          diagnostics.standardBundleOpaqueRequestCount || 0),
        responseCount: Number(
          diagnostics.standardBundleOpaqueResponseCount || 0),
        pushMessageCount: Number(
          diagnostics.standardBundlePushMessageCount || 0),
        flowClientCount: clients.length,
        pointerStages: pointer.stages || {},
        browserError: String(diagnostics.standardBundleBrowserError || ''),
        requestError: String(
          diagnostics.standardBundleOpaqueRequestError || ''),
        responseError: String(
          diagnostics.standardBundleOpaqueResponseError || ''),
        pushError: String(diagnostics.standardBundlePushError || '')
      });
    })()
    """
}
