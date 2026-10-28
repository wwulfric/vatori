import Cocoa
import Darwin
import Foundation

private var flowIpcFileReadGrantAcceptance:
    FlowIpcFileReadGrantAcceptance?

extension WebViewHost {
    func scheduleFileReadGrantAcceptanceIfReady() {
        guard config.flowIpcAutoFileReadGrant
                || config.flowIpcAutoFileReadGrantSidecarExit
                || config.flowIpcAutoFileReadGrantHostShutdown,
              config.transport == .flowIpc else {
            return
        }
        if flowIpcFileReadGrantAcceptance == nil {
            let timeout = config.flowIpcAutoShutdownTimeoutSeconds > 0
                ? config.flowIpcAutoShutdownTimeoutSeconds
                : 60
            flowIpcFileReadGrantAcceptance = FlowIpcFileReadGrantAcceptance(
                host: self,
                deadline: Date().addingTimeInterval(timeout))
        }
        flowIpcFileReadGrantAcceptance?.resume()
    }

    func observeFileReadGrantHostShutdown() {
        flowIpcFileReadGrantAcceptance?.applicationWillTerminate()
    }
}

private final class FlowIpcFileReadGrantAcceptance {
    private enum Phase: String {
        case root
        case mainRoute
        case secondaryOpening
        case secondaryReady
        case firstSelectInvoked
        case firstSelectionSubmitted
        case hostShutdownWaiting
        case recovering
        case recoveredMainRoute
        case recoveredSelectInvoked
        case recoveredSelectionSubmitted
        case readInvoked
        case releaseInvoked
        case postReleaseReadInvoked
        case secondSelectInvoked
        case secondSelectionSubmitted
        case closeInvoked
        case finishDrain
    }

    private static let secondaryWindowId = "file-read-grant-secondary"
    private static let fixtureFilename =
        "vatori-file-read-grant-fixture.txt"
    private static let fixtureContents =
        "vatori-file-read-grant-fixture::0123456789::tail"
    private static let fixtureByteCount = 48
    private static let readByteCount = 19
    private static let maxReadBytes = 65_536
    private static let firstPanelTitle =
        "Flow file read grant explicit release"
    private static let secondPanelTitle =
        "Flow file read grant automatic revoke"

    private static let expectedPointerStages = [
        "main-open-secondary",
        "secondary-select-first",
        "secondary-read-first",
        "secondary-release-first",
        "secondary-read-after-release",
        "secondary-select-second",
        "main-close-secondary"
    ]
    private static let expectedPanelStages = ["first", "second"]

    private var sidecarExitMode: Bool {
        host.config.flowIpcAutoFileReadGrantSidecarExit
    }

    private var hostShutdownMode: Bool {
        host.config.flowIpcAutoFileReadGrantHostShutdown
    }

    private var expectedPointerStages: [String] {
        if hostShutdownMode {
            return ["main-open-secondary", "secondary-select-first"]
        }
        return sidecarExitMode ? [
            "main-open-secondary", "secondary-select-first",
            "recovered-select-first", "secondary-read-first",
            "secondary-release-first", "secondary-read-after-release",
            "main-close-secondary"
        ] : Self.expectedPointerStages
    }

    private var expectedPanelStages: [String] {
        if hostShutdownMode { return ["first"] }
        return sidecarExitMode ? ["first", "recovered"] : Self.expectedPanelStages
    }

    private var expectedBrowserStages: [String] {
        sidecarExitMode ? ["route-navigate", "recovered-route-navigate"]
            : ["route-navigate"]
    }

    private let host: WebViewHost
    private let deadline: Date
    private var phase = Phase.root
    private var pollScheduled = false
    private var terminal = false
    private var hostShutdownPrepared = false
    private var acceptanceQuitMenuRoot: NSMenuItem?
    private var acceptanceQuitMenuItem: NSMenuItem?

    private var fixtureDirectory: URL?
    private var fixtureURL: URL?
    private var fixtureCreated = false
    private var fixtureRemoved = false

    private var mainRequestBaseline = 0
    private var mainResponseBaseline = 0
    private var secondaryRequestBaseline = 0
    private var secondaryResponseBaseline = 0
    private var secondaryPushBaseline = 0
    private var secondaryFinalRequestCount = 0
    private var secondaryFinalResponseCount = 0
    private var secondaryFinalPushCount = 0
    private var secondarySessionId = ""
    private weak var secondaryWindow: NSWindow?
    private var initialWindowIdentifier: ObjectIdentifier?
    private var initialWebViewIdentifier: ObjectIdentifier?
    private var initialSecondaryWindowIdentifier: ObjectIdentifier?
    private var initialSecondaryWebViewIdentifier: ObjectIdentifier?
    private var initialProcessIdentifier: Int32 = 0
    private var failedProcessIdentifier: Int32 = 0
    private var recoveredProcessIdentifier: Int32 = 0
    private var windowRetained = false
    private var webViewRetained = false
    private var secondaryWindowRetained = false
    private var secondaryWebViewRetained = false
    private var processReplaced = false
    private var recoveredStateResetObserved = false
    private var oldSecondaryRequestBaseline = 0
    private var oldSecondaryResponseBaseline = 0
    private var oldSecondaryRequestCount = 0
    private var oldSecondaryResponseCount = 0

    private var scopeIsolatedObserved = false
    private var mainUidlRoundTripObserved = false
    private var secondaryUidlRoundTripObserved = false
    private var secondaryContextRemoved = false
    private var secondaryWindowClosedObserved = false

    private var firstSelectionStatusObserved = false
    private var readStatusObserved = false
    private var releaseStatusObserved = false
    private var postReleaseReadStatusObserved = false
    private var secondSelectionStatusObserved = false
    private var contentMatchesObserved = false
    private var firstSecurityScoped: Bool?
    private var secondSecurityScoped: Bool?

    private var nativePointerStages: [String] = []
    private var nativePointerReceiptStages: [String] = []
    private var nativePointerTrustedStages: [String] = []
    private var browserProgrammaticStages: [String] = []
    private var externalPanelSelectionStages: [String] = []
    private var observedPanels: [NSOpenPanel] = []
    private var panelAttachedStages: [String] = []
    private var panelVisibleStages: [String] = []
    private var panelURLVerifiedStages: [String] = []

    private var drainKey = ""
    private var drainBalancedPolls = 0

    init(host: WebViewHost, deadline: Date) {
        self.host = host
        self.deadline = deadline
        createFixture()
    }

    func resume() {
        schedulePoll(after: 0.1)
    }

    private func createFixture() {
        let environment = ProcessInfo.processInfo.environment
        let root = environment[
            "VAADIN_DESKTOP_FLOW_IPC_FILE_READ_GRANT_FIXTURE_ROOT"
        ].map { URL(fileURLWithPath: $0, isDirectory: true) }
            ?? FileManager.default.temporaryDirectory
        let directory = root
            .appendingPathComponent(
                "vatori-file-read-grant-\(UUID().uuidString.lowercased())",
                isDirectory: true)
        let file = directory.appendingPathComponent(
            Self.fixtureFilename,
            isDirectory: false)
        do {
            let data = Data(Self.fixtureContents.utf8)
            guard data.count == Self.fixtureByteCount else { return }
            fixtureDirectory = directory
            try FileManager.default.createDirectory(
                at: directory,
                withIntermediateDirectories: false)
            try data.write(to: file, options: .atomic)
            fixtureURL = file
            fixtureCreated = true
        } catch {
            try? FileManager.default.removeItem(at: directory)
            fixtureDirectory = nil
            fixtureURL = nil
        }
    }

    private func removeFixture() {
        guard !fixtureRemoved, let fixtureDirectory else { return }
        do {
            try FileManager.default.removeItem(at: fixtureDirectory)
            fixtureRemoved = true
            self.fixtureDirectory = nil
            fixtureURL = nil
        } catch {
            fixtureRemoved = false
        }
    }

    private var awaitingRequestedHostTermination: Bool {
        // Keep auditing while waiting for external Quit, but do not race the
        // host's intentional asynchronous teardown after that Quit is accepted.
        hostShutdownPrepared && phase == .hostShutdownWaiting && host.isTerminationRequested
    }

    private func schedulePoll(after delay: Double) {
        guard !terminal, !awaitingRequestedHostTermination, !pollScheduled else { return }
        pollScheduled = true
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) {
            self.pollScheduled = false
            self.poll()
        }
    }

    private func poll() {
        guard !terminal, !awaitingRequestedHostTermination else { return }
        if phase == .recovering {
            awaitRecovery()
            return
        }
        guard let main = host.flowIpcWindowContext("main"),
              main.navigationFinished else {
            retryOrFail(code: "fileReadGrantMainWindowTimeout")
            return
        }
        inspect(main) { snapshot in
            guard let snapshot else {
                self.retryOrFail(code: "fileReadGrantMainInspectionFailed")
                return
            }
            guard snapshot.errorFree else {
                self.fail(
                    code: "fileReadGrantMainRuntimeError",
                    details: snapshot.tracePayload(prefix: "main"))
                return
            }
            self.advance(main: main, mainSnapshot: snapshot)
        }
    }

    private func advance(
        main: FlowIpcWindowContext,
        mainSnapshot: Snapshot
    ) {
        switch phase {
        case .root:
            guard fixtureCreated,
                  fixtureDirectory != nil,
                  fixtureURL != nil,
                  mainSnapshot.initialRootReady,
                  mainSnapshot.firstMeaningfulPaint,
                  mainSnapshot.flowClientCount > 0,
                  mainSnapshot.requestCount == mainSnapshot.responseCount,
                  exactMainScope(mainSnapshot),
                  main.recoveryGeneration == 0 else {
                retryOrFail(
                    code: "fileReadGrantInitialRootTimeout",
                    details: mainSnapshot.tracePayload(prefix: "main"))
                return
            }
            mainRequestBaseline = mainSnapshot.requestCount
            mainResponseBaseline = mainSnapshot.responseCount
            initialWindowIdentifier = ObjectIdentifier(main.window)
            initialWebViewIdentifier = ObjectIdentifier(main.webView)
            initialProcessIdentifier = host.sidecarProcessIdentifier ?? 0
            dispatchNavigation(in: main)

        case .mainRoute:
            guard mainSnapshot.routeReady,
                  mainSnapshot.openSecondaryStatus
                    == "File grant secondary open: pending",
                  mainSnapshot.closeSecondaryStatus
                    == "File grant secondary close: pending",
                  mainSnapshot.operationsPending else {
                retryOrFail(
                    code: "fileReadGrantMainRouteTimeout",
                    details: mainSnapshot.tracePayload(prefix: "main"))
                return
            }
            performNativeClick(
                in: main,
                buttonId:
                    "framework-flow-file-read-grant-open-secondary",
                stage: "main-open-secondary",
                next: .secondaryOpening,
                failureCode: "fileReadGrantOpenSecondaryFailed")

        case .secondaryOpening:
            recordPointer(
                stage: "main-open-secondary",
                snapshot: mainSnapshot)
            guard pointerObserved(
                    stage: "main-open-secondary",
                    snapshot: mainSnapshot),
                  mainSnapshot.openSecondaryStatus
                    == "File grant secondary open: opened=true client=true change=1" else {
                retryOrFail(
                    code: "fileReadGrantSecondaryOpenResultTimeout",
                    details: mainSnapshot.tracePayload(prefix: "main"))
                return
            }
            withSecondary(mainSnapshot: mainSnapshot) {
                secondary, secondarySnapshot in
                guard secondarySnapshot.routeReady,
                      secondarySnapshot.operationsPending,
                      secondarySnapshot.requestCount
                        == secondarySnapshot.responseCount,
                      secondary.window.attachedSheet == nil,
                      Self.visibleOpenPanels().isEmpty,
                      self.initialSecondaryScopeIsolated(
                        mainSnapshot,
                        secondarySnapshot) else {
                    self.retryOrFail(
                        code: "fileReadGrantSecondaryRouteTimeout",
                        details: secondarySnapshot.tracePayload(
                            prefix: "secondary"))
                    return
                }
                self.secondarySessionId = secondarySnapshot.sessionId
                self.secondaryWindow = secondary.window
                self.initialSecondaryWindowIdentifier =
                    ObjectIdentifier(secondary.window)
                self.initialSecondaryWebViewIdentifier =
                    ObjectIdentifier(secondary.webView)
                self.secondaryRequestBaseline =
                    secondarySnapshot.requestCount
                self.secondaryResponseBaseline =
                    secondarySnapshot.responseCount
                self.secondaryPushBaseline =
                    secondarySnapshot.pushMessageCount
                self.scopeIsolatedObserved = true
                self.phase = .secondaryReady
                self.schedulePoll(after: 0.1)
            }

        case .secondaryReady:
            withSecondary(mainSnapshot: mainSnapshot) {
                secondary, secondarySnapshot in
                guard self.exactSecondaryScope(secondarySnapshot),
                      secondarySnapshot.operationsPending,
                      secondary.window.attachedSheet == nil,
                      Self.visibleOpenPanels().isEmpty else {
                    self.retryOrFail(
                        code: "fileReadGrantSecondaryScopeTimeout",
                        details: secondarySnapshot.tracePayload(
                            prefix: "secondary"))
                    return
                }
                self.performNativeClick(
                    in: secondary,
                    buttonId:
                        "framework-flow-file-read-grant-select-first",
                    stage: "secondary-select-first",
                    next: .firstSelectInvoked,
                    failureCode: "fileReadGrantFirstSelectInvokeFailed")
            }

        case .firstSelectInvoked:
            withSecondary(mainSnapshot: mainSnapshot) {
                secondary, secondarySnapshot in
                self.selectFixture(
                    in: secondary,
                    snapshot: secondarySnapshot,
                    pointerStage: "secondary-select-first",
                    panelStage: "first",
                    expectedTitle: Self.firstPanelTitle,
                    next: .firstSelectionSubmitted)
            }

        case .firstSelectionSubmitted:
            withSecondary(mainSnapshot: mainSnapshot) {
                secondary, secondarySnapshot in
                guard self.selectionStatusMatches(
                        secondarySnapshot.firstSelectionStatus,
                        label: "First file grant selection"),
                      secondary.window.attachedSheet == nil,
                      Self.visibleOpenPanels().isEmpty,
                      let securityScoped = Self.boolField(
                        "securityScoped",
                        in: secondarySnapshot.firstSelectionStatus) else {
                    self.retryOrFail(
                        code: "fileReadGrantFirstSelectionResultTimeout",
                        details: secondarySnapshot.tracePayload(
                            prefix: "secondary"))
                    return
                }
                self.firstSelectionStatusObserved = true
                self.firstSecurityScoped = securityScoped
                if self.hostShutdownMode {
                    self.awaitHostShutdown(
                        main: main,
                        mainSnapshot: mainSnapshot,
                        secondary: secondary,
                        secondarySnapshot: secondarySnapshot)
                    return
                }
                if self.sidecarExitMode {
                    guard secondarySnapshot.requestCount
                            > self.secondaryRequestBaseline,
                          secondarySnapshot.responseCount
                            > self.secondaryResponseBaseline,
                          secondarySnapshot.requestCount
                            == secondarySnapshot.responseCount,
                          mainSnapshot.requestCount
                            == mainSnapshot.responseCount,
                          secondarySnapshot.readStatus
                            == "First file grant read: pending",
                          secondarySnapshot.releaseStatus
                            == "First file grant release: pending",
                          secondarySnapshot.postReleaseReadStatus
                            == "First file grant read after release: pending",
                          secondarySnapshot.secondSelectionStatus
                            == "Second file grant selection: pending",
                          self.nativePointerStages == [
                            "main-open-secondary", "secondary-select-first"
                          ],
                          self.nativePointerReceiptStages
                            == self.nativePointerStages,
                          self.nativePointerTrustedStages
                            == self.nativePointerStages,
                          self.exactSecondaryScope(secondarySnapshot),
                          main.recoveryGeneration == 0,
                          secondary.recoveryGeneration == 0 else {
                        self.retryOrFail(
                            code: "fileReadGrantBeforeExitDrainTimeout",
                            details: secondarySnapshot.tracePayload(
                                prefix: "secondary"))
                        return
                    }
                    self.oldSecondaryRequestBaseline =
                        self.secondaryRequestBaseline
                    self.oldSecondaryResponseBaseline =
                        self.secondaryResponseBaseline
                    self.oldSecondaryRequestCount =
                        secondarySnapshot.requestCount
                    self.oldSecondaryResponseCount =
                        secondarySnapshot.responseCount
                    guard self.failSidecar() else {
                        self.fail(code: "fileReadGrantSidecarFailureInjectionFailed")
                        return
                    }
                    self.phase = .recovering
                    self.schedulePoll(after: 0.1)
                    return
                }
                self.performNativeClick(
                    in: secondary,
                    buttonId:
                        "framework-flow-file-read-grant-read-first",
                    stage: "secondary-read-first",
                    next: .readInvoked,
                    failureCode: "fileReadGrantReadInvokeFailed")
            }

        case .recovering:
            awaitRecovery()

        case .hostShutdownWaiting:
            withSecondary(mainSnapshot: mainSnapshot) {
                secondary, secondarySnapshot in
                self.awaitHostShutdown(
                    main: main,
                    mainSnapshot: mainSnapshot,
                    secondary: secondary,
                    secondarySnapshot: secondarySnapshot)
            }

        case .recoveredMainRoute:
            guard mainSnapshot.routeReady,
                  mainSnapshot.openSecondaryStatus
                    == "File grant secondary open: pending",
                  mainSnapshot.closeSecondaryStatus
                    == "File grant secondary close: pending",
                  mainSnapshot.operationsPending,
                  mainSnapshot.requestCount == mainSnapshot.responseCount,
                  exactMainScope(mainSnapshot),
                  retainedRecoveredContexts() else {
                retryOrFail(code: "fileReadGrantRecoveredMainRouteTimeout")
                return
            }
            withSecondary(mainSnapshot: mainSnapshot) {
                secondary, secondarySnapshot in
                guard secondarySnapshot.routeReady,
                      secondarySnapshot.operationsPending,
                      secondarySnapshot.openSecondaryStatus
                        == "File grant secondary open: pending",
                      secondarySnapshot.closeSecondaryStatus
                        == "File grant secondary close: pending",
                      secondarySnapshot.firstMeaningfulPaint,
                      secondarySnapshot.flowClientCount > 0,
                      secondarySnapshot.requestCount
                        == secondarySnapshot.responseCount,
                      secondarySnapshot.pointerStages.isEmpty,
                      self.exactSecondaryScope(secondarySnapshot),
                      secondary.window.attachedSheet == nil,
                      Self.visibleOpenPanels().isEmpty else {
                    self.retryOrFail(
                        code: "fileReadGrantRecoveredStateResetTimeout",
                        details: secondarySnapshot.tracePayload(
                            prefix: "secondary"))
                    return
                }
                self.recoveredStateResetObserved = true
                self.mainRequestBaseline = mainSnapshot.requestCount
                self.mainResponseBaseline = mainSnapshot.responseCount
                self.secondaryRequestBaseline = secondarySnapshot.requestCount
                self.secondaryResponseBaseline = secondarySnapshot.responseCount
                self.secondaryPushBaseline = secondarySnapshot.pushMessageCount
                self.performNativeClick(
                    in: secondary,
                    buttonId: "framework-flow-file-read-grant-select-first",
                    stage: "recovered-select-first",
                    next: .recoveredSelectInvoked,
                    failureCode: "fileReadGrantRecoveredSelectInvokeFailed")
            }

        case .recoveredSelectInvoked:
            withSecondary(mainSnapshot: mainSnapshot) {
                secondary, secondarySnapshot in
                self.selectFixture(
                    in: secondary,
                    snapshot: secondarySnapshot,
                    pointerStage: "recovered-select-first",
                    panelStage: "recovered",
                    expectedTitle: Self.firstPanelTitle,
                    next: .recoveredSelectionSubmitted)
            }

        case .recoveredSelectionSubmitted:
            withSecondary(mainSnapshot: mainSnapshot) {
                secondary, secondarySnapshot in
                guard self.selectionStatusMatches(
                        secondarySnapshot.firstSelectionStatus,
                        label: "First file grant selection"),
                      secondary.window.attachedSheet == nil,
                      Self.visibleOpenPanels().isEmpty,
                      let securityScoped = Self.boolField(
                        "securityScoped",
                        in: secondarySnapshot.firstSelectionStatus) else {
                    self.retryOrFail(
                        code: "fileReadGrantRecoveredSelectionResultTimeout",
                        details: secondarySnapshot.tracePayload(
                            prefix: "secondary"))
                    return
                }
                self.secondSelectionStatusObserved = true
                self.secondSecurityScoped = securityScoped
                self.performNativeClick(
                    in: secondary,
                    buttonId: "framework-flow-file-read-grant-read-first",
                    stage: "secondary-read-first",
                    next: .readInvoked,
                    failureCode: "fileReadGrantReadInvokeFailed")
            }

        case .readInvoked:
            withSecondary(mainSnapshot: mainSnapshot) {
                secondary, secondarySnapshot in
                self.recordPointer(
                    stage: "secondary-read-first",
                    snapshot: secondarySnapshot)
                guard self.pointerObserved(
                        stage: "secondary-read-first",
                        snapshot: secondarySnapshot),
                      self.readStatusMatches(
                        secondarySnapshot.readStatus) else {
                    self.retryOrFail(
                        code: "fileReadGrantReadResultTimeout",
                        details: secondarySnapshot.tracePayload(
                            prefix: "secondary"))
                    return
                }
                self.readStatusObserved = true
                self.contentMatchesObserved = true
                self.performNativeClick(
                    in: secondary,
                    buttonId:
                        "framework-flow-file-read-grant-release-first",
                    stage: "secondary-release-first",
                    next: .releaseInvoked,
                    failureCode: "fileReadGrantReleaseInvokeFailed")
            }

        case .releaseInvoked:
            withSecondary(mainSnapshot: mainSnapshot) {
                secondary, secondarySnapshot in
                self.recordPointer(
                    stage: "secondary-release-first",
                    snapshot: secondarySnapshot)
                guard self.pointerObserved(
                        stage: "secondary-release-first",
                        snapshot: secondarySnapshot),
                      self.releaseStatusMatches(
                        secondarySnapshot.releaseStatus) else {
                    self.retryOrFail(
                        code: "fileReadGrantReleaseResultTimeout",
                        details: secondarySnapshot.tracePayload(
                            prefix: "secondary"))
                    return
                }
                self.releaseStatusObserved = true
                self.performNativeClick(
                    in: secondary,
                    buttonId:
                        "framework-flow-file-read-grant-read-after-release",
                    stage: "secondary-read-after-release",
                    next: .postReleaseReadInvoked,
                    failureCode: "fileReadGrantPostReleaseInvokeFailed")
            }

        case .postReleaseReadInvoked:
            withSecondary(mainSnapshot: mainSnapshot) {
                secondary, secondarySnapshot in
                self.recordPointer(
                    stage: "secondary-read-after-release",
                    snapshot: secondarySnapshot)
                guard self.pointerObserved(
                        stage: "secondary-read-after-release",
                        snapshot: secondarySnapshot),
                      self.postReleaseStatusMatches(
                        secondarySnapshot.postReleaseReadStatus) else {
                    self.retryOrFail(
                        code: "fileReadGrantPostReleaseResultTimeout",
                        details: secondarySnapshot.tracePayload(
                            prefix: "secondary"))
                    return
                }
                self.postReleaseReadStatusObserved = true
                if self.sidecarExitMode {
                    self.closeSecondaryAfterOperations(
                        main: main,
                        secondarySnapshot: secondarySnapshot)
                    return
                }
                self.performNativeClick(
                    in: secondary,
                    buttonId:
                        "framework-flow-file-read-grant-select-second",
                    stage: "secondary-select-second",
                    next: .secondSelectInvoked,
                    failureCode: "fileReadGrantSecondSelectInvokeFailed")
            }

        case .secondSelectInvoked:
            withSecondary(mainSnapshot: mainSnapshot) {
                secondary, secondarySnapshot in
                self.selectFixture(
                    in: secondary,
                    snapshot: secondarySnapshot,
                    pointerStage: "secondary-select-second",
                    panelStage: "second",
                    expectedTitle: Self.secondPanelTitle,
                    next: .secondSelectionSubmitted)
            }

        case .secondSelectionSubmitted:
            withSecondary(mainSnapshot: mainSnapshot) {
                secondary, secondarySnapshot in
                guard self.selectionStatusMatches(
                        secondarySnapshot.secondSelectionStatus,
                        label: "Second file grant selection"),
                      secondary.window.attachedSheet == nil,
                      Self.visibleOpenPanels().isEmpty,
                      let securityScoped = Self.boolField(
                        "securityScoped",
                        in: secondarySnapshot.secondSelectionStatus),
                      securityScoped == self.firstSecurityScoped,
                      secondarySnapshot.requestCount
                        > self.secondaryRequestBaseline,
                      secondarySnapshot.responseCount
                        > self.secondaryResponseBaseline,
                      secondarySnapshot.requestCount
                        == secondarySnapshot.responseCount else {
                    self.retryOrFail(
                        code: "fileReadGrantSecondSelectionResultTimeout",
                        details: secondarySnapshot.tracePayload(
                            prefix: "secondary"))
                    return
                }
                self.secondSelectionStatusObserved = true
                self.secondSecurityScoped = securityScoped
                self.closeSecondaryAfterOperations(
                    main: main,
                    secondarySnapshot: secondarySnapshot)
            }

        case .closeInvoked:
            recordPointer(
                stage: "main-close-secondary",
                snapshot: mainSnapshot)
            guard pointerObserved(
                    stage: "main-close-secondary",
                    snapshot: mainSnapshot),
                  mainSnapshot.closeSecondaryStatus
                    == "File grant secondary close: closed=true client=true change=1",
                  host.flowIpcWindowContext(Self.secondaryWindowId) == nil,
                  secondaryWindow?.isVisible != true,
                  Self.visibleOpenPanels().isEmpty else {
                retryOrFail(
                    code: "fileReadGrantSecondaryCloseTimeout",
                    details: mainSnapshot.tracePayload(prefix: "main"))
                return
            }
            secondaryContextRemoved = true
            secondaryWindowClosedObserved = true
            mainUidlRoundTripObserved =
                mainSnapshot.requestCount > mainRequestBaseline
                && mainSnapshot.responseCount > mainResponseBaseline
            guard mainUidlRoundTripObserved else {
                retryOrFail(code: "fileReadGrantMainUidlTimeout")
                return
            }
            phase = .finishDrain
            schedulePoll(after: 0.5)

        case .finishDrain:
            let balanced = mainSnapshot.requestCount
                == mainSnapshot.responseCount
            let key = "\(mainSnapshot.requestCount):\(mainSnapshot.responseCount)"
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
                    code: "fileReadGrantDrainTimeout",
                    details: [
                        "mainRequestCount": mainSnapshot.requestCount,
                        "mainResponseCount": mainSnapshot.responseCount,
                        "balancedPolls": drainBalancedPolls
                    ],
                    delay: 0.5)
                return
            }
            removeFixture()
            guard fixtureRemoved else {
                fail(code: "fileReadGrantFixtureCleanupFailed")
                return
            }
            finish(main: main, snapshot: mainSnapshot)
        }
    }

    private func awaitHostShutdown(
        main: FlowIpcWindowContext,
        mainSnapshot: Snapshot,
        secondary: FlowIpcWindowContext,
        secondarySnapshot: Snapshot
    ) {
        let pointersObserved = nativePointerStages == expectedPointerStages
            && nativePointerReceiptStages == expectedPointerStages
            && nativePointerTrustedStages == expectedPointerStages
        let panelsObserved = observedPanels.count == 1
            && panelAttachedStages == expectedPanelStages
            && panelVisibleStages == expectedPanelStages
            && panelURLVerifiedStages == expectedPanelStages
            && externalPanelSelectionStages == expectedPanelStages
        let operationsPending = mainSnapshot.operationsPending
            && mainSnapshot.closeSecondaryStatus
                == "File grant secondary close: pending"
            && secondarySnapshot.unusedGrantOperationsPending
            && secondarySnapshot.openSecondaryStatus
                == "File grant secondary open: pending"
            && secondarySnapshot.closeSecondaryStatus
                == "File grant secondary close: pending"
        let ownerWindowsOpen = ownerWindowOpen(main.window)
            && ownerWindowOpen(secondary.window)
        let contextsRetained = retainedHostShutdownContexts(
            main: main, secondary: secondary)
        mainUidlRoundTripObserved = mainSnapshot.requestCount > mainRequestBaseline
            && mainSnapshot.responseCount > mainResponseBaseline
        secondaryUidlRoundTripObserved =
            secondarySnapshot.requestCount > secondaryRequestBaseline
            && secondarySnapshot.responseCount > secondaryResponseBaseline
        let stable = hostShutdownMode && !sidecarExitMode
            && !host.config.flowIpcAutoFileReadGrant
            && contextsRetained && ownerWindowsOpen
            && mainSnapshot.rootRendered && secondarySnapshot.rootRendered
            && mainSnapshot.routeReady && secondarySnapshot.routeReady
            && mainSnapshot.firstMeaningfulPaint
            && secondarySnapshot.firstMeaningfulPaint
            && mainSnapshot.flowClientCount > 0
            && secondarySnapshot.flowClientCount > 0
            && mainSnapshot.openSecondaryStatus
                == "File grant secondary open: opened=true client=true change=1"
            && exactMainScope(mainSnapshot)
            && exactSecondaryScope(secondarySnapshot)
            && selectionStatusMatches(
                secondarySnapshot.firstSelectionStatus,
                label: "First file grant selection")
            && Self.boolField("securityScoped", in: secondarySnapshot.firstSelectionStatus)
                == firstSecurityScoped
            && firstSecurityScoped != nil && firstSelectionStatusObserved
            && operationsPending && pointersObserved && panelsObserved
            && browserProgrammaticStages == expectedBrowserStages
            && mainUidlRoundTripObserved && secondaryUidlRoundTripObserved
            && mainSnapshot.requestCount == mainSnapshot.responseCount
            && secondarySnapshot.requestCount == secondarySnapshot.responseCount
            && mainSnapshot.errorFree && secondarySnapshot.errorFree
            && main.window.attachedSheet == nil
            && secondary.window.attachedSheet == nil
            && Self.visibleOpenPanels().isEmpty
        guard stable else {
            drainKey = ""
            drainBalancedPolls = 0
            if hostShutdownPrepared {
                fail(code: "fileReadGrantHostShutdownPreparedStateChanged")
            } else {
                retryOrFail(
                    code: "fileReadGrantHostShutdownPreparationTimeout",
                    details: secondarySnapshot.tracePayload(prefix: "secondary"),
                    delay: 0.5)
            }
            return
        }
        let key = "\(mainSnapshot.requestCount):\(mainSnapshot.responseCount)"
            + ":\(secondarySnapshot.requestCount):\(secondarySnapshot.responseCount)"
        if drainKey == key {
            drainBalancedPolls += 1
        } else {
            drainKey = key
            drainBalancedPolls = 1
        }
        guard drainBalancedPolls >= 2 else {
            retryOrFail(code: "fileReadGrantHostShutdownDrainTimeout", delay: 0.5)
            return
        }
        if !hostShutdownPrepared {
            guard installAcceptanceQuitMenu() else {
                fail(code: "fileReadGrantAcceptanceQuitMenuInstallationFailed")
                return
            }
            hostShutdownPrepared = true
            phase = .hostShutdownWaiting
            trace("recv", "flowFileReadGrant.hostShutdownPrepared", [
                "complete": true,
                "standardBundleMode": mainSnapshot.firstMeaningfulPaint
                    && secondarySnapshot.firstMeaningfulPaint,
                "rootRendered": mainSnapshot.rootRendered && secondarySnapshot.rootRendered,
                "flowClientOwned": mainSnapshot.flowClientCount > 0
                    && secondarySnapshot.flowClientCount > 0,
                "fixtureCreated": fixtureCreated,
                "acceptanceQuitMenuInstalled": acceptanceQuitMenuInstalled,
                "fixtureByteCount": Self.fixtureByteCount,
                "firstSelectionStatusObserved": firstSelectionStatusObserved,
                "firstSelectionOutcome": "SELECTED",
                "firstSecurityScoped": firstSecurityScoped ?? false,
                "securityScopedValueObserved": firstSecurityScoped != nil,
                "mainWindowId": mainSnapshot.windowId,
                "mainSessionId": mainSnapshot.sessionId,
                "mainUiId": mainSnapshot.uiId,
                "secondaryWindowId": secondarySnapshot.windowId,
                "secondarySessionId": secondarySnapshot.sessionId,
                "secondaryUiId": secondarySnapshot.uiId,
                "mainRequestBaseline": mainRequestBaseline,
                "mainResponseBaseline": mainResponseBaseline,
                "mainRequestCount": mainSnapshot.requestCount,
                "mainResponseCount": mainSnapshot.responseCount,
                "secondaryRequestBaseline": secondaryRequestBaseline,
                "secondaryResponseBaseline": secondaryResponseBaseline,
                "secondaryRequestCount": secondarySnapshot.requestCount,
                "secondaryResponseCount": secondarySnapshot.responseCount,
                "initialProcessIdentifier": initialProcessIdentifier,
                "windowRetained": windowRetained,
                "webViewRetained": webViewRetained,
                "secondaryWindowRetained": secondaryWindowRetained,
                "secondaryWebViewRetained": secondaryWebViewRetained,
                "scopeIsolatedObserved": scopeIsolatedObserved,
                "mainUidlRoundTripObserved": mainUidlRoundTripObserved,
                "secondaryUidlRoundTripObserved": secondaryUidlRoundTripObserved,
                "opaqueRequestObserved": mainUidlRoundTripObserved
                    && secondaryUidlRoundTripObserved,
                "opaqueResponseObserved": mainUidlRoundTripObserved
                    && secondaryUidlRoundTripObserved,
                "panelCount": observedPanels.count,
                "realOpenPanelObserved": panelsObserved,
                "panelsAttachedObserved": panelAttachedStages == expectedPanelStages,
                "panelsVisibleObserved": panelVisibleStages == expectedPanelStages,
                "panelURLsVerified": panelURLVerifiedStages == expectedPanelStages,
                "externalPanelSelection": externalPanelSelectionStages == expectedPanelStages,
                "externalPanelSelectionStages": externalPanelSelectionStages.joined(separator: ","),
                "nativePointerObserved": pointersObserved,
                "nativePointerTrustedObserved": nativePointerTrustedStages == expectedPointerStages,
                "nativePointerEventCount": nativePointerStages.count,
                "nativePointerTrustedEventCount": nativePointerTrustedStages.count,
                "nativePointerStages": nativePointerStages.joined(separator: ","),
                "browserProgrammaticSetupObserved": browserProgrammaticStages == expectedBrowserStages,
                "browserProgrammaticStages": browserProgrammaticStages.joined(separator: ","),
                "operationsPending": operationsPending,
                "ownerWindowsOpen": ownerWindowsOpen,
                "balancedDrainObserved": drainBalancedPolls >= 2,
                "resultStableAfterDrain": stable,
                "errorFree": mainSnapshot.errorFree && secondarySnapshot.errorFree,
                "recoveryGeneration": main.recoveryGeneration,
                "physicalInput": false,
                "sandboxEnforced": false
            ])
        }
        // Selection and Quit are external native UI actions. Never close an
        // owner or terminate the app while awaiting the host-shutdown trigger.
        retryOrFail(code: "fileReadGrantExternalHostShutdownTimeout", delay: 0.5)
    }

    private var acceptanceQuitMenuInstalled: Bool {
        guard let root = acceptanceQuitMenuRoot,
              let item = acceptanceQuitMenuItem,
              let mainMenu = NSApplication.shared.mainMenu,
              let submenu = root.submenu else {
            return false
        }
        return mainMenu.items.contains { $0 === root }
            && submenu.items.contains { $0 === item }
            && item.target === NSApplication.shared
            && item.action == #selector(NSApplication.terminate(_:))
            && item.keyEquivalent == "q"
            && item.keyEquivalentModifierMask == .command
    }

    private func installAcceptanceQuitMenu() -> Bool {
        guard hostShutdownMode, !sidecarExitMode,
              !host.config.flowIpcAutoFileReadGrant else {
            return false
        }
        if acceptanceQuitMenuInstalled { return true }
        // This menu is an acceptance-only external input surface, not a
        // production application-menu implementation. The driver only installs
        // the standard AppKit action; an operator/native UI driver invokes it.
        let mainMenu = NSApplication.shared.mainMenu ?? NSMenu(title: "")
        let root = NSMenuItem(title: "Acceptance", action: nil, keyEquivalent: "")
        let submenu = NSMenu(title: "Acceptance")
        let quit = NSMenuItem(
            title: "Quit Vaadin Desktop Host",
            action: #selector(NSApplication.terminate(_:)),
            keyEquivalent: "q")
        quit.target = NSApplication.shared
        quit.keyEquivalentModifierMask = .command
        submenu.addItem(quit)
        root.submenu = submenu
        mainMenu.addItem(root)
        NSApplication.shared.mainMenu = mainMenu
        acceptanceQuitMenuRoot = root
        acceptanceQuitMenuItem = quit
        return acceptanceQuitMenuInstalled
    }

    private func ownerWindowOpen(_ window: NSWindow) -> Bool {
        NSApplication.shared.windows.contains { $0 === window }
            && (window.isVisible || window.isMiniaturized)
    }

    private func retainedHostShutdownContexts(
        main: FlowIpcWindowContext,
        secondary: FlowIpcWindowContext,
        requireRunningSidecar: Bool = true
    ) -> Bool {
        windowRetained = initialWindowIdentifier == ObjectIdentifier(main.window)
        webViewRetained = initialWebViewIdentifier == ObjectIdentifier(main.webView)
        secondaryWindowRetained = initialSecondaryWindowIdentifier
            == ObjectIdentifier(secondary.window)
        secondaryWebViewRetained = initialSecondaryWebViewIdentifier
            == ObjectIdentifier(secondary.webView)
        return windowRetained && webViewRetained && secondaryWindowRetained
            && secondaryWebViewRetained && initialProcessIdentifier > 0
            && (!requireRunningSidecar || host.sidecarProcessIdentifier == initialProcessIdentifier)
            && main.recoveryGeneration == 0 && secondary.recoveryGeneration == 0
            && main.scope.windowId == "main"
            && main.scope.sessionId == "flow-ipc-session"
            && main.scope.uiId == "main"
            && secondary.scope.windowId == Self.secondaryWindowId
            && secondary.scope.sessionId == secondarySessionId
            && secondary.scope.uiId == Self.secondaryWindowId
            && opaqueSession(secondarySessionId)
    }

    func applicationWillTerminate() {
        guard hostShutdownMode else { return }
        let main = host.flowIpcWindowContext("main")
        let secondary = host.flowIpcWindowContext(Self.secondaryWindowId)
        let contextsRetained: Bool
        let ownerWindowsOpen: Bool
        if let main, let secondary {
            contextsRetained = retainedHostShutdownContexts(
                main: main, secondary: secondary, requireRunningSidecar: false)
            ownerWindowsOpen = ownerWindowOpen(main.window)
                && ownerWindowOpen(secondary.window)
        } else {
            contextsRetained = false
            ownerWindowsOpen = false
        }
        let externalTerminationExpected = hostShutdownPrepared
            && phase == .hostShutdownWaiting && !terminal
            && acceptanceQuitMenuInstalled
        errno = 0
        let sidecarAbsent = initialProcessIdentifier > 0 && host.sidecarProcessIdentifier == nil
            && Darwin.kill(initialProcessIdentifier, 0) == -1 && errno == ESRCH
        terminal = true
        trace("recv", "flowFileReadGrant.hostShutdownObserved", [
            "complete": externalTerminationExpected && contextsRetained
                && ownerWindowsOpen && !sidecarExitMode
                && !host.config.flowIpcAutoFileReadGrant && sidecarAbsent,
            "preparedObserved": hostShutdownPrepared,
            "terminationSource": "applicationWillTerminate",
            "externalTerminationExpected": externalTerminationExpected,
            "initialProcessIdentifier": initialProcessIdentifier,
            "currentProcessIdentifier": host.sidecarProcessIdentifier ?? 0,
            "originalSidecarAbsent": sidecarAbsent,
            "ownerWindowsOpen": ownerWindowsOpen,
            "mainWindowId": main?.scope.windowId ?? "",
            "mainSessionId": main?.scope.sessionId ?? "",
            "mainUiId": main?.scope.uiId ?? "",
            "secondaryWindowId": secondary?.scope.windowId ?? "",
            "secondarySessionId": secondary?.scope.sessionId ?? "",
            "secondaryUiId": secondary?.scope.uiId ?? "",
            "windowRetained": windowRetained,
            "webViewRetained": webViewRetained,
            "secondaryWindowRetained": secondaryWindowRetained,
            "secondaryWebViewRetained": secondaryWebViewRetained,
            "recoveryGeneration": main?.recoveryGeneration ?? -1,
            "physicalInput": false
        ])
        // The runner removes its fixture root only after the host has exited.
        // This observer must neither remove the live file nor claim cleanup.
    }

    private func closeSecondaryAfterOperations(
        main: FlowIpcWindowContext,
        secondarySnapshot: Snapshot
    ) {
        guard exactSecondaryScope(secondarySnapshot),
              secondarySnapshot.requestCount > secondaryRequestBaseline,
              secondarySnapshot.responseCount > secondaryResponseBaseline,
              secondarySnapshot.requestCount == secondarySnapshot.responseCount else {
            retryOrFail(
                code: "fileReadGrantSecondaryOperationDrainTimeout",
                details: secondarySnapshot.tracePayload(prefix: "secondary"))
            return
        }
        secondaryFinalRequestCount = secondarySnapshot.requestCount
        secondaryFinalResponseCount = secondarySnapshot.responseCount
        secondaryFinalPushCount = secondarySnapshot.pushMessageCount
        secondaryUidlRoundTripObserved = true
        performNativeClick(
            in: main,
            buttonId: "framework-flow-file-read-grant-close-secondary",
            stage: "main-close-secondary",
            next: .closeInvoked,
            failureCode: "fileReadGrantCloseSecondaryFailed")
    }

    private func failSidecar() -> Bool {
        guard let processIdentifier = host.sidecarProcessIdentifier,
              processIdentifier > 0,
              processIdentifier == initialProcessIdentifier,
              Darwin.kill(processIdentifier, SIGKILL) == 0 else {
            return false
        }
        failedProcessIdentifier = processIdentifier
        trace("send", "acceptance.sidecarFailureInjected", [
            "reason": "active-file-read-grant",
            "command": "fileDialog.openReadGrant",
            "processIdentifier": processIdentifier
        ])
        return true
    }

    private func retainedRecoveredContexts() -> Bool {
        guard let main = host.flowIpcWindowContext("main"),
              let secondary = host.flowIpcWindowContext(Self.secondaryWindowId),
              main.recoveryGeneration == 1,
              secondary.recoveryGeneration == 1,
              main.navigationFinished,
              secondary.navigationFinished,
              host.sidecarProcessIdentifier == recoveredProcessIdentifier,
              recoveredProcessIdentifier > 0,
              recoveredProcessIdentifier != initialProcessIdentifier,
              main.scope.windowId == "main",
              main.scope.sessionId == "flow-ipc-session",
              main.scope.uiId == "main",
              secondary.scope.windowId == Self.secondaryWindowId,
              secondary.scope.sessionId == secondarySessionId,
              secondary.scope.uiId == Self.secondaryWindowId else {
            return false
        }
        windowRetained = initialWindowIdentifier == ObjectIdentifier(main.window)
        webViewRetained = initialWebViewIdentifier == ObjectIdentifier(main.webView)
        secondaryWindowRetained = initialSecondaryWindowIdentifier
            == ObjectIdentifier(secondary.window)
        secondaryWebViewRetained = initialSecondaryWebViewIdentifier
            == ObjectIdentifier(secondary.webView)
        processReplaced = initialProcessIdentifier == failedProcessIdentifier
            && initialProcessIdentifier > 0
            && recoveredProcessIdentifier != failedProcessIdentifier
        return windowRetained && webViewRetained && secondaryWindowRetained
            && secondaryWebViewRetained && processReplaced
    }

    private func awaitRecovery() {
        guard let main = host.flowIpcWindowContext("main"),
              let secondary = host.flowIpcWindowContext(Self.secondaryWindowId) else {
            retryOrFail(code: "fileReadGrantRecoveryWindowsLost")
            return
        }
        if main.recoveryGeneration > 1 || secondary.recoveryGeneration > 1 {
            fail(code: "fileReadGrantUnexpectedExtraRecovery")
            return
        }
        recoveredProcessIdentifier = host.sidecarProcessIdentifier ?? 0
        guard retainedRecoveredContexts() else {
            retryOrFail(code: "fileReadGrantRecoveryContinuityTimeout")
            return
        }
        inspect(main) { snapshot in
            guard let snapshot,
                  snapshot.errorFree,
                  snapshot.initialRootReady,
                  snapshot.firstMeaningfulPaint,
                  snapshot.flowClientCount > 0,
                  snapshot.requestCount == snapshot.responseCount,
                  snapshot.pointerStages.isEmpty,
                  self.exactMainScope(snapshot) else {
                self.retryOrFail(
                    code: "fileReadGrantRecoveredRootTimeout",
                    details: snapshot?.tracePayload(prefix: "main") ?? [:])
                return
            }
            self.dispatchNavigation(in: main, recovered: true)
        }
    }

    private func withSecondary(
        mainSnapshot: Snapshot,
        completion: @escaping (FlowIpcWindowContext, Snapshot) -> Void
    ) {
        if sidecarExitMode && failedProcessIdentifier > 0,
           !retainedRecoveredContexts() {
            fail(code: "fileReadGrantRecoveredIdentityChanged")
            return
        }
        guard let secondary = host.flowIpcWindowContext(
                Self.secondaryWindowId),
              secondary.navigationFinished else {
            retryOrFail(code: "fileReadGrantSecondaryWindowLost")
            return
        }
        inspect(secondary) { snapshot in
            guard let snapshot, snapshot.errorFree else {
                self.retryOrFail(
                    code: "fileReadGrantSecondaryInspectionFailed",
                    details: snapshot?.tracePayload(prefix: "secondary") ?? [:])
                return
            }
            completion(secondary, snapshot)
        }
    }

    private func inspect(
        _ context: FlowIpcWindowContext,
        completion: @escaping (Snapshot?) -> Void
    ) {
        guard !terminal, !awaitingRequestedHostTermination else { return }
        context.webView.evaluateJavaScript(Self.inspectionScript) {
            result, error in
            guard !self.terminal, !self.awaitingRequestedHostTermination else { return }
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

    private func dispatchNavigation(
        in context: FlowIpcWindowContext,
        recovered: Bool = false
    ) {
        context.webView.evaluateJavaScript(Self.navigationScript) {
            result, error in
            guard error == nil, result as? Bool == true else {
                self.retryOrFail(
                    code: "fileReadGrantNavigationFailed",
                    details: [
                        "dispatchError": error != nil,
                        "dispatchAccepted": result as? Bool == true
                    ])
                return
            }
            self.browserProgrammaticStages.append(
                recovered ? "recovered-route-navigate" : "route-navigate")
            self.phase = recovered ? .recoveredMainRoute : .mainRoute
            self.schedulePoll(after: 0.3)
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
                        "pointerPrepared": false,
                        "stage": stage
                    ])
                return
            }
            let dispatchPointer = {
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
                            "pointerInsideWindow": false,
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
                trace("recv", "flowFileReadGrant.nativePointerDispatch", [
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
            FlowIpcNativeInputActivation.prepare(
                context: context,
                deadline: self.deadline,
                onFailure: { details in
                    self.fail(code: failureCode, details: details)
                },
                completion: dispatchPointer)
        }
    }

    private func selectFixture(
        in context: FlowIpcWindowContext,
        snapshot: Snapshot,
        pointerStage: String,
        panelStage: String,
        expectedTitle: String,
        next: Phase
    ) {
        recordPointer(stage: pointerStage, snapshot: snapshot)
        let selectionStatus = panelStage != "second"
            ? snapshot.firstSelectionStatus
            : snapshot.secondSelectionStatus
        if let outcome = Self.field("outcome", in: selectionStatus) {
            fail(
                code: "fileReadGrantPanelCompletedBeforeSelection",
                details: ["stage": panelStage, "outcome": outcome])
            return
        }
        guard pointerObserved(stage: pointerStage, snapshot: snapshot),
              let fixture = fixtureURL,
              let panel = context.window.attachedSheet as? NSOpenPanel,
              panel.title == expectedTitle,
              panel.sheetParent === context.window,
              panel.isVisible,
              Self.visibleOpenPanels().contains(where: { $0 === panel }) else {
            retryOrFail(
                code: "fileReadGrantPanelTimeout",
                details: [
                    "stage": panelStage,
                    "pointerObserved": pointerObserved(
                        stage: pointerStage,
                        snapshot: snapshot),
                    "attachedOpenPanel":
                        context.window.attachedSheet is NSOpenPanel,
                    "visibleOpenPanelCount": Self.visibleOpenPanels().count
                ],
                delay: 0.05)
            return
        }

        let firstObservation = !observedPanels.contains(where: { $0 === panel })
        if firstObservation {
            observedPanels.append(panel)
            trace("recv", "flowFileReadGrant.panelReady", [
                "stage": panelStage,
                "sheetAttached": true,
                "panelVisible": true,
                "inputMode": "external-native-panel-selection"
            ])
        }
        if !panelAttachedStages.contains(panelStage) {
            panelAttachedStages.append(panelStage)
        }
        if !panelVisibleStages.contains(panelStage) {
            panelVisibleStages.append(panelStage)
        }

        // Selection is performed through the system UI by an external native UI
        // driver or operator. Modern open panels may host their content remotely;
        // neither an internal view hierarchy nor locally synthesized key events
        // are a reliable contract for selecting a file in that process.
        let selectedURL = panel.url?
            .resolvingSymlinksInPath()
            .standardizedFileURL
        let expectedURL = fixture
            .resolvingSymlinksInPath()
            .standardizedFileURL
        guard selectedURL == expectedURL else {
            retryOrFail(
                code: "fileReadGrantPanelSelectionTimeout",
                details: [
                    "stage": panelStage,
                    "panelURLMatchesFixture": false
                ],
                delay: 0.05)
            return
        }

        panelURLVerifiedStages.append(panelStage)
        externalPanelSelectionStages.append(panelStage)
        trace("recv", "flowFileReadGrant.panelObserved", [
            "complete": true,
            "stage": panelStage,
            "title": expectedTitle,
            "sheetAttached": true,
            "panelVisible": true,
            "panelURLVerified": true,
            "externalPanelSelection": true,
            "inputMode": "external-native-panel-selection",
            "physicalInput": false,
            "windowId": context.scope.windowId,
            "sessionId": context.scope.sessionId,
            "uiId": context.scope.uiId
        ])
        phase = next
        // The external UI driver must confirm with the real Open control after
        // this observation. NSSavePanel.ok(_:) is not implemented by modern
        // remote panels; do not synthesize or bypass their completion path.
        schedulePoll(after: 0.1)
    }

    private func recordPointer(stage: String, snapshot: Snapshot) {
        guard let observation = snapshot.pointerStages[stage] else { return }
        if observation.receipt,
           !nativePointerReceiptStages.contains(stage) {
            nativePointerReceiptStages.append(stage)
        }
        if observation.trusted,
           !nativePointerTrustedStages.contains(stage) {
            nativePointerTrustedStages.append(stage)
        }
    }

    private func pointerObserved(stage: String, snapshot: Snapshot) -> Bool {
        snapshot.pointerStages[stage]?.receipt == true
            && snapshot.pointerStages[stage]?.trusted == true
    }

    private func exactMainScope(_ snapshot: Snapshot) -> Bool {
        snapshot.windowId == "main"
            && snapshot.sessionId == "flow-ipc-session"
            && snapshot.uiId == "main"
    }

    private func initialSecondaryScopeIsolated(
        _ main: Snapshot,
        _ secondary: Snapshot
    ) -> Bool {
        exactMainScope(main)
            && secondary.windowId == Self.secondaryWindowId
            && secondary.uiId == Self.secondaryWindowId
            && opaqueSession(secondary.sessionId)
            && main.sessionId != secondary.sessionId
    }

    private func exactSecondaryScope(_ snapshot: Snapshot) -> Bool {
        snapshot.windowId == Self.secondaryWindowId
            && snapshot.uiId == Self.secondaryWindowId
            && snapshot.sessionId == secondarySessionId
            && opaqueSession(snapshot.sessionId)
    }

    private func opaqueSession(_ sessionId: String) -> Bool {
        let prefix = "flow-ipc-window:"
        guard sessionId.hasPrefix(prefix) else { return false }
        return UUID(uuidString: String(sessionId.dropFirst(prefix.count))) != nil
    }

    private func selectionStatusMatches(
        _ status: String,
        label: String
    ) -> Bool {
        status.hasPrefix("\(label): ")
            && Self.field("outcome", in: status) == "SELECTED"
            && Self.field("grantPresent", in: status) == "true"
            && Self.field("filename", in: status) == Self.fixtureFilename
            && Self.field("byteCount", in: status)
                == String(Self.fixtureByteCount)
            && Self.field("maxReadBytes", in: status)
                == String(Self.maxReadBytes)
            && Self.boolField("securityScoped", in: status) != nil
            && Self.field("capabilityContextCaptured", in: status) == "true"
            && Self.field("backgroundSessionAbsent", in: status) == "true"
            && Self.field("client", in: status) == "true"
            && Self.field("change", in: status) == "1"
    }

    private func readStatusMatches(_ status: String) -> Bool {
        status.hasPrefix("First file grant read: ")
            && Self.field("outcome", in: status) == "DATA"
            && Self.field("requestedOffset", in: status) == "0"
            && Self.field("requestedBytes", in: status)
                == String(Self.readByteCount)
            && Self.field("bytesRead", in: status)
                == String(Self.readByteCount)
            && Self.field("offset", in: status) == "0"
            && Self.field("nextOffset", in: status)
                == String(Self.readByteCount)
            && Self.field("eof", in: status) == "false"
            && Self.field("grantPresent", in: status) == "true"
            && Self.field("filename", in: status) == Self.fixtureFilename
            && Self.field("byteCount", in: status)
                == String(Self.fixtureByteCount)
            && Self.field("maxReadBytes", in: status)
                == String(Self.maxReadBytes)
            && Self.boolField("securityScoped", in: status)
                == (sidecarExitMode ? secondSecurityScoped : firstSecurityScoped)
            && Self.field("client", in: status) == "true"
            && Self.field("change", in: status) == "1"
            && Self.field("contentMatches", in: status) == "true"
    }

    private func releaseStatusMatches(_ status: String) -> Bool {
        status.hasPrefix("First file grant release: ")
            && Self.field("outcome", in: status) == "RELEASED"
            && Self.field("grantPresent", in: status) == "true"
            && Self.field("filename", in: status) == Self.fixtureFilename
            && Self.field("byteCount", in: status)
                == String(Self.fixtureByteCount)
            && Self.field("maxReadBytes", in: status)
                == String(Self.maxReadBytes)
            && Self.boolField("securityScoped", in: status)
                == (sidecarExitMode ? secondSecurityScoped : firstSecurityScoped)
            && Self.field("client", in: status) == "true"
            && Self.field("change", in: status) == "1"
    }

    private func postReleaseStatusMatches(_ status: String) -> Bool {
        status.hasPrefix("First file grant read after release: ")
            && Self.field("outcome", in: status) == "UNAVAILABLE"
            && Self.field("requestedOffset", in: status) == "0"
            && Self.field("requestedBytes", in: status)
                == String(Self.readByteCount)
            && Self.field("bytesRead", in: status) == "0"
            && Self.field("offset", in: status) == "-1"
            && Self.field("nextOffset", in: status) == "-1"
            && Self.field("eof", in: status) == "false"
            && Self.field("grantPresent", in: status) == "true"
            && Self.field("filename", in: status) == Self.fixtureFilename
            && Self.field("byteCount", in: status)
                == String(Self.fixtureByteCount)
            && Self.field("maxReadBytes", in: status)
                == String(Self.maxReadBytes)
            && Self.boolField("securityScoped", in: status)
                == (sidecarExitMode ? secondSecurityScoped : firstSecurityScoped)
            && Self.field("client", in: status) == "true"
            && Self.field("change", in: status) == "1"
    }

    private static func field(_ name: String, in status: String) -> String? {
        let prefix = "\(name)="
        guard let component = status.split(separator: " ").first(where: {
            $0.hasPrefix(prefix)
        }) else {
            return nil
        }
        return String(component.dropFirst(prefix.count))
    }

    private static func boolField(
        _ name: String,
        in status: String
    ) -> Bool? {
        switch field(name, in: status) {
        case "true": return true
        case "false": return false
        default: return nil
        }
    }

    private static func visibleOpenPanels() -> [NSOpenPanel] {
        NSApplication.shared.windows.compactMap { $0 as? NSOpenPanel }
            .filter(\.isVisible)
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
        removeFixture()
        host.flowIpcWindowContext(Self.secondaryWindowId)?.window.close()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            self.host.flowIpcWindowContext("main")?.window.close()
        }
    }

    private func finish(
        main: FlowIpcWindowContext,
        snapshot: Snapshot
    ) {
        let pointerObserved =
            nativePointerStages == expectedPointerStages
            && nativePointerReceiptStages == expectedPointerStages
        let trustedPointerObserved =
            nativePointerTrustedStages == expectedPointerStages
        let panelsObserved = observedPanels.count == 2
            && panelAttachedStages == expectedPanelStages
            && panelVisibleStages == expectedPanelStages
            && panelURLVerifiedStages == expectedPanelStages
            && externalPanelSelectionStages == expectedPanelStages
        let statusesObserved = firstSelectionStatusObserved
            && readStatusObserved
            && releaseStatusObserved
            && postReleaseReadStatusObserved
            && secondSelectionStatusObserved
        let securityScopedValueObserved = firstSecurityScoped != nil
            && secondSecurityScoped != nil
            && (sidecarExitMode || firstSecurityScoped == secondSecurityScoped)
        let recoveryObserved = !sidecarExitMode || (
            windowRetained && webViewRetained
                && secondaryWindowRetained && secondaryWebViewRetained
                && processReplaced && recoveredStateResetObserved
                && main.recoveryGeneration == 1
                && initialWindowIdentifier == ObjectIdentifier(main.window)
                && initialWebViewIdentifier == ObjectIdentifier(main.webView)
                && host.sidecarProcessIdentifier == recoveredProcessIdentifier)
        let stable = statusesObserved
            && secondaryContextRemoved
            && secondaryWindowClosedObserved
            && panelsObserved
            && pointerObserved
            && trustedPointerObserved
            && securityScopedValueObserved
            && recoveryObserved

        var payload: [String: Any] = [
            "complete": stable,
            "standardBundleMode": snapshot.firstMeaningfulPaint,
            "rootRendered": snapshot.rootRendered,
            "flowClientOwned": snapshot.flowClientCount > 0,
            "fixtureCreated": fixtureCreated,
            "fixtureRemoved": fixtureRemoved,
            "fixtureByteCount": Self.fixtureByteCount,
            "realOpenPanelObserved": panelsObserved,
            "panelsAttachedObserved":
                panelAttachedStages == expectedPanelStages,
            "panelsVisibleObserved":
                panelVisibleStages == expectedPanelStages,
            "panelURLsVerified":
                panelURLVerifiedStages == expectedPanelStages,
            "panelCount": observedPanels.count,
            "externalPanelSelection":
                externalPanelSelectionStages
                    == expectedPanelStages,
            "externalPanelSelectionStages":
                externalPanelSelectionStages.joined(separator: ","),
            "physicalInput": false,
            "sandboxEnforced": false,
            "securityScopedValueObserved": securityScopedValueObserved,
            "securityScopedObserved": firstSecurityScoped ?? false,
            "firstSecurityScoped": firstSecurityScoped ?? false,
            "secondSecurityScoped": secondSecurityScoped ?? false,
            "scopeIsolatedObserved": scopeIsolatedObserved,
            "firstSelectionStatusObserved": firstSelectionStatusObserved,
            "readStatusObserved": readStatusObserved,
            "releaseStatusObserved": releaseStatusObserved,
            "postReleaseReadStatusObserved":
                postReleaseReadStatusObserved,
            "secondSelectionStatusObserved": secondSelectionStatusObserved,
            "firstSelectionOutcome": "SELECTED",
            "readOutcome": "DATA",
            "releaseOutcome": "RELEASED",
            "postReleaseReadOutcome": "UNAVAILABLE",
            "secondSelectionOutcome": "SELECTED",
            "readBytes": Self.readByteCount,
            "contentMatches": contentMatchesObserved,
            "mainUidlRoundTripObserved": mainUidlRoundTripObserved,
            "secondaryUidlRoundTripObserved":
                secondaryUidlRoundTripObserved,
            "opaqueRequestObserved": mainUidlRoundTripObserved
                && secondaryUidlRoundTripObserved,
            "opaqueResponseObserved": mainUidlRoundTripObserved
                && secondaryUidlRoundTripObserved,
            "secondaryContextRemoved": secondaryContextRemoved,
            "secondaryWindowClosedObserved":
                secondaryWindowClosedObserved,
            "nativePointerObserved": pointerObserved,
            "nativePointerTrustedObserved": trustedPointerObserved,
            "nativePointerEventCount": nativePointerStages.count,
            "nativePointerTrustedEventCount":
                nativePointerTrustedStages.count,
            "secondaryNativePointerEventCount":
                nativePointerStages.filter {
                    $0.hasPrefix("secondary-")
                        || $0 == "recovered-select-first"
                }.count,
            "openCloseNativePointerEventCount":
                nativePointerStages.filter {
                    $0 == "main-open-secondary"
                        || $0 == "main-close-secondary"
                }.count,
            "nativePointerStages":
                nativePointerStages.joined(separator: ","),
            "browserProgrammaticSetupObserved":
                browserProgrammaticStages == expectedBrowserStages,
            "browserProgrammaticStages":
                browserProgrammaticStages.joined(separator: ","),
            "mainWindowId": snapshot.windowId,
            "mainSessionId": snapshot.sessionId,
            "mainUiId": snapshot.uiId,
            "secondaryWindowId": Self.secondaryWindowId,
            "secondarySessionId": secondarySessionId,
            "secondaryUiId": Self.secondaryWindowId,
            "mainRequestBaseline": mainRequestBaseline,
            "mainResponseBaseline": mainResponseBaseline,
            "mainRequestCount": snapshot.requestCount,
            "mainResponseCount": snapshot.responseCount,
            "secondaryRequestBaseline": secondaryRequestBaseline,
            "secondaryResponseBaseline": secondaryResponseBaseline,
            "secondaryPushBaseline": secondaryPushBaseline,
            "secondaryRequestCount": secondaryFinalRequestCount,
            "secondaryResponseCount": secondaryFinalResponseCount,
            "secondaryPushCount": secondaryFinalPushCount,
            "balancedDrainObserved": drainBalancedPolls >= 2,
            "resultStableAfterDrain": stable,
            "errorFree": snapshot.errorFree,
            "gracefulCloseRequested": true,
            "recoveryGeneration": main.recoveryGeneration
        ]
        if sidecarExitMode {
            payload.merge([
                "initialProcessIdentifier": initialProcessIdentifier,
                "failedProcessIdentifier": failedProcessIdentifier,
                "recoveredProcessIdentifier": recoveredProcessIdentifier,
                "windowRetained": windowRetained,
                "webViewRetained": webViewRetained,
                "secondaryWindowRetained": secondaryWindowRetained,
                "secondaryWebViewRetained": secondaryWebViewRetained,
                "processReplaced": processReplaced,
                "recoveredStateResetObserved": recoveredStateResetObserved,
                "oldSecondaryRequestBaseline": oldSecondaryRequestBaseline,
                "oldSecondaryResponseBaseline": oldSecondaryResponseBaseline,
                "oldSecondaryRequestCount": oldSecondaryRequestCount,
                "oldSecondaryResponseCount": oldSecondaryResponseCount
            ]) { _, value in value }
        }
        terminal = true
        trace("recv", sidecarExitMode
            ? "flowFileReadGrant.sidecarExitAcceptance"
            : "flowFileReadGrant.frameworkAcceptance", payload)
        main.window.close()
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
        let initialRootReady: Bool
        let firstMeaningfulPaint: Bool
        let routeReady: Bool
        let openSecondaryStatus: String
        let closeSecondaryStatus: String
        let firstSelectionStatus: String
        let readStatus: String
        let releaseStatus: String
        let postReleaseReadStatus: String
        let secondSelectionStatus: String
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

        var operationsPending: Bool {
            firstSelectionStatus
                == "First file grant selection: pending"
                && unusedGrantOperationsPending
        }

        var unusedGrantOperationsPending: Bool {
            readStatus == "First file grant read: pending"
                && releaseStatus == "First file grant release: pending"
                && postReleaseReadStatus
                    == "First file grant read after release: pending"
                && secondSelectionStatus
                    == "Second file grant selection: pending"
        }

        func tracePayload(prefix: String) -> [String: Any] {
            [
                "\(prefix)WindowId": windowId,
                "\(prefix)SessionId": sessionId,
                "\(prefix)UiId": uiId,
                "\(prefix)RootRendered": rootRendered,
                "\(prefix)InitialRootReady": initialRootReady,
                "\(prefix)FirstMeaningfulPaint": firstMeaningfulPaint,
                "\(prefix)RouteReady": routeReady,
                "\(prefix)OpenSecondaryCompleted":
                    openSecondaryStatus.contains("opened=true"),
                "\(prefix)CloseSecondaryCompleted":
                    closeSecondaryStatus.contains("closed=true"),
                "\(prefix)FirstSelectionOutcome":
                    FlowIpcFileReadGrantAcceptance.field(
                        "outcome",
                        in: firstSelectionStatus) ?? "missing",
                "\(prefix)ReadOutcome":
                    FlowIpcFileReadGrantAcceptance.field(
                        "outcome",
                        in: readStatus) ?? "missing",
                "\(prefix)ReleaseOutcome":
                    FlowIpcFileReadGrantAcceptance.field(
                        "outcome",
                        in: releaseStatus) ?? "missing",
                "\(prefix)PostReleaseReadOutcome":
                    FlowIpcFileReadGrantAcceptance.field(
                        "outcome",
                        in: postReleaseReadStatus) ?? "missing",
                "\(prefix)SecondSelectionOutcome":
                    FlowIpcFileReadGrantAcceptance.field(
                        "outcome",
                        in: secondSelectionStatus) ?? "missing",
                "\(prefix)RequestCount": requestCount,
                "\(prefix)ResponseCount": responseCount,
                "\(prefix)PushMessageCount": pushMessageCount,
                "\(prefix)FlowClientCount": flowClientCount,
                "\(prefix)PointerStages": pointerStages.keys.sorted()
                    .joined(separator: ","),
                "\(prefix)BrowserErrorPresent": !browserError.isEmpty,
                "\(prefix)RequestErrorPresent": !requestError.isEmpty,
                "\(prefix)ResponseErrorPresent": !responseError.isEmpty,
                "\(prefix)PushErrorPresent": !pushError.isEmpty
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
          button.scrollIntoView({block:'center', inline:'center'});
          const rect = button.getBoundingClientRect();
          const centerX = rect.left + rect.width / 2;
          const centerY = rect.top + rect.height / 2;
          const hit = document.elementFromPoint(centerX, centerY);
          if (!(rect.width > 0 && rect.height > 0)
              || !(hit === button || button.contains(hit))) {
            return JSON.stringify({ready:false, centerX, centerY});
          }
          const marker = window.VaadinDesktopFileReadGrantPointer ||= {
            stages: {}, pendingHandlers: {}
          };
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

    private static let navigationScript = """
    (() => {
      const route = document.querySelector(
        '#framework-flow-file-read-grant-route');
      if (route
          && window.location.pathname.endsWith('/flow-file-read-grant')) {
        return true;
      }
      const button = document.querySelector(
        '#framework-flow-file-read-grant-open-route');
      if (!button) return false;
      button.click();
      return true;
    })()
    """

    private static let inspectionScript = """
    (() => {
      const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
      const context = window.VaadinDesktopHostContext || {};
      const route = document.querySelector(
        '#framework-flow-file-read-grant-route');
      const clients = window.Vaadin?.Flow?.clients
        ? Object.keys(window.Vaadin.Flow.clients)
          .filter((name) => name !== 'TypeScript')
        : [];
      const pointer =
        window.VaadinDesktopFileReadGrantPointer || {stages:{}};
      const text = (id) => String(
        document.querySelector(`#${id}`)?.textContent || '');
      return JSON.stringify({
        windowId: String(context.windowId || ''),
        sessionId: String(context.sessionId || ''),
        uiId: String(context.uiId || ''),
        rootRendered: !!document.querySelector('#framework-root') || !!route,
        initialRootReady: !!document.querySelector('#framework-root') && !route,
        firstMeaningfulPaint:
          diagnostics.standardBundleFirstMeaningfulPaintReported === true,
        routeReady:
          window.location.pathname.endsWith('/flow-file-read-grant')
          && !!route
          && !!document.querySelector(
            '#framework-flow-file-read-grant-open-secondary')
          && !!document.querySelector(
            '#framework-flow-file-read-grant-close-secondary')
          && !!document.querySelector(
            '#framework-flow-file-read-grant-select-first')
          && !!document.querySelector(
            '#framework-flow-file-read-grant-read-first')
          && !!document.querySelector(
            '#framework-flow-file-read-grant-release-first')
          && !!document.querySelector(
            '#framework-flow-file-read-grant-read-after-release')
          && !!document.querySelector(
            '#framework-flow-file-read-grant-select-second'),
        openSecondaryStatus: text(
          'framework-flow-file-read-grant-open-secondary-status'),
        closeSecondaryStatus: text(
          'framework-flow-file-read-grant-close-secondary-status'),
        firstSelectionStatus: text(
          'framework-flow-file-read-grant-select-first-status'),
        readStatus: text(
          'framework-flow-file-read-grant-read-first-status'),
        releaseStatus: text(
          'framework-flow-file-read-grant-release-first-status'),
        postReleaseReadStatus: text(
          'framework-flow-file-read-grant-read-after-release-status'),
        secondSelectionStatus: text(
          'framework-flow-file-read-grant-select-second-status'),
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
