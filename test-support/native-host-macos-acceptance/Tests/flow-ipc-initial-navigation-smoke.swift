import Foundation

@main
struct FlowIpcInitialNavigationSmoke {
    static func main() throws {
        try disabledDoesNotArm()
        try expiresOnceAtDeadline()
        try duplicateOpenDoesNotExtendDeadline()
        try exactFinishCancelsPendingTimeout()
        try alreadyFinishedDoesNotArm()
        try unrelatedFinishDoesNotCancel()
        try invalidTimeoutUsesBoundedDefault()
        print("Flow IPC initial navigation watchdog smoke passed")
    }

    private static func disabledDoesNotArm() throws {
        let objects = Objects()
        var watchdog = FlowIpcInitialNavigationWatchdog()
        try require(!arm(&watchdog, objects.identity(), enabled: false),
                    "autoShutdown=false armed the watchdog")
        try require(watchdog.state == .idle && watchdog.identity == nil,
                    "disabled watchdog captured a live identity")
        try require(!watchdog.expire(now: 1_000), "disabled watchdog expired")
    }

    private static func expiresOnceAtDeadline() throws {
        let objects = Objects()
        let identity = objects.identity()
        var watchdog = FlowIpcInitialNavigationWatchdog()
        try require(arm(&watchdog, identity), "initial navigation did not arm")
        try require(watchdog.identity == identity && watchdog.timeoutSeconds == 5,
                    "initial identity or timeout was not retained")
        try require(!watchdog.expire(now: 104.999), "watchdog expired early")
        try require(watchdog.expire(now: 105), "watchdog missed its deadline")
        try require(watchdog.state == .expired
                        && watchdog.elapsedSeconds(at: 105) == 5,
                    "expiration lost its monotonic timing")
        try require(!watchdog.expire(now: 200), "watchdog expired twice")
        try require(!watchdog.finish(identity: identity),
                    "late finish rewrote a timeout as success")
        try require(!arm(&watchdog, identity, now: 200),
                    "late open rearmed an expired watchdog")
    }

    private static func duplicateOpenDoesNotExtendDeadline() throws {
        let objects = Objects()
        let replacement = Objects()
        var watchdog = FlowIpcInitialNavigationWatchdog()
        try require(arm(&watchdog, objects.identity()), "initial arm failed")
        try require(!arm(&watchdog, objects.identity(), timeout: 500, now: 104),
                    "duplicate open rescheduled the watchdog")
        try require(!arm(&watchdog, replacement.identity(), now: 104),
                    "replacement native context replaced the watched identity")
        try require(watchdog.identity == objects.identity()
                        && watchdog.timeoutSeconds == 5
                        && watchdog.expire(now: 105),
                    "duplicate open extended or replaced the original deadline")
    }

    private static func exactFinishCancelsPendingTimeout() throws {
        let objects = Objects()
        let identity = objects.identity()
        var watchdog = FlowIpcInitialNavigationWatchdog()
        try require(arm(&watchdog, identity), "initial arm failed")
        try require(watchdog.finish(identity: identity), "exact finish was ignored")
        try require(watchdog.state == .finished, "finish did not settle watchdog")
        try require(!watchdog.finish(identity: identity), "duplicate finish settled twice")
        try require(!watchdog.expire(now: 1_000),
                    "already queued timeout fired after exact finish")
        try require(!arm(&watchdog, identity, now: 1_000),
                    "repeated open rearmed a completed initial navigation")
    }

    private static func alreadyFinishedDoesNotArm() throws {
        let objects = Objects()
        var watchdog = FlowIpcInitialNavigationWatchdog()
        // A finish callback before open cannot select an arbitrary identity.
        try require(!watchdog.finish(identity: objects.identity()),
                    "unarmed finish captured an identity")
        try require(!arm(&watchdog, objects.identity(), finished: true),
                    "navigation already finished before open armed a timeout")
        try require(watchdog.state == .finished && !watchdog.expire(now: 1_000),
                    "early navigation completion did not settle the watchdog")
    }

    private static func unrelatedFinishDoesNotCancel() throws {
        let objects = Objects()
        let replacement = Objects()
        let unrelated = [
            objects.identity(windowId: "secondary"),
            objects.identity(sessionId: "other-session"),
            objects.identity(uiId: "other-ui"),
            objects.identity(context: replacement.context),
            objects.identity(window: replacement.window),
            objects.identity(webView: replacement.webView),
            objects.identity(processIdentifier: 42),
            objects.identity(recoveryGeneration: 1)
        ]
        for identity in unrelated {
            var watchdog = FlowIpcInitialNavigationWatchdog()
            try require(arm(&watchdog, objects.identity()), "initial arm failed")
            try require(!watchdog.finish(identity: identity),
                        "unrelated scope, native object or JVM canceled deadline")
            try require(watchdog.state == .waiting && watchdog.expire(now: 105),
                        "unrelated finish disabled the original timeout")
        }
    }

    private static func invalidTimeoutUsesBoundedDefault() throws {
        let objects = Objects()
        for timeout in [Double.nan, .infinity, -.infinity, 0, -1] {
            var watchdog = FlowIpcInitialNavigationWatchdog()
            try require(arm(&watchdog, objects.identity(), timeout: timeout),
                        "invalid timeout prevented bounded arming")
            try require(watchdog.timeoutSeconds == 30
                            && !watchdog.expire(now: 129.999)
                            && watchdog.expire(now: 130),
                        "invalid timeout did not use the finite default")
        }
    }

    private static func arm(
        _ watchdog: inout FlowIpcInitialNavigationWatchdog,
        _ identity: FlowIpcInitialNavigationWatchdog.Identity,
        enabled: Bool = true,
        finished: Bool = false,
        timeout: TimeInterval = 5,
        now: TimeInterval = 100
    ) -> Bool {
        watchdog.arm(
            enabled: enabled,
            identity: identity,
            navigationFinished: finished,
            timeoutSeconds: timeout,
            now: now)
    }

    private final class Objects {
        let context = NSObject()
        let window = NSObject()
        let webView = NSObject()

        func identity(
            windowId: String = "main",
            sessionId: String = "flow-ipc-session",
            uiId: String = "main",
            context: NSObject? = nil,
            window: NSObject? = nil,
            webView: NSObject? = nil,
            processIdentifier: Int32 = 41,
            recoveryGeneration: Int = 0
        ) -> FlowIpcInitialNavigationWatchdog.Identity {
            FlowIpcInitialNavigationWatchdog.Identity(
                windowId: windowId,
                sessionId: sessionId,
                uiId: uiId,
                context: ObjectIdentifier(context ?? self.context),
                window: ObjectIdentifier(window ?? self.window),
                webView: ObjectIdentifier(webView ?? self.webView),
                processIdentifier: processIdentifier,
                recoveryGeneration: recoveryGeneration)
        }
    }

    private static func require(
        _ condition: @autoclosure () -> Bool,
        _ message: String
    ) throws {
        if !condition() { throw SmokeFailure(message: message) }
    }

    private struct SmokeFailure: Error, CustomStringConvertible {
        let message: String
        var description: String { message }
    }
}
