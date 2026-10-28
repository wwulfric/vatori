import Foundation

// Acceptance-only state. A navigation callback is not a Flow first-paint proof.
struct FlowIpcInitialNavigationWatchdog {
    struct Identity: Equatable {
        let windowId: String
        let sessionId: String
        let uiId: String
        let context: ObjectIdentifier
        let window: ObjectIdentifier
        let webView: ObjectIdentifier
        let processIdentifier: Int32
        let recoveryGeneration: Int
    }

    enum State: Equatable {
        case idle
        case waiting
        case finished
        case expired
    }

    private(set) var state = State.idle
    private(set) var identity: Identity?
    private(set) var timeoutSeconds: TimeInterval = 0
    private var armedAt: TimeInterval = 0

    mutating func arm(
        enabled: Bool,
        identity: Identity,
        navigationFinished: Bool,
        timeoutSeconds: TimeInterval,
        now: TimeInterval
    ) -> Bool {
        guard enabled, state == .idle else { return false }
        self.identity = identity
        self.timeoutSeconds = timeoutSeconds.isFinite && timeoutSeconds > 0
            ? timeoutSeconds : 30
        armedAt = now
        state = navigationFinished ? .finished : .waiting
        return state == .waiting
    }

    mutating func finish(identity: Identity) -> Bool {
        guard state == .waiting, self.identity == identity else { return false }
        state = .finished
        return true
    }

    mutating func expire(now: TimeInterval) -> Bool {
        guard state == .waiting, elapsedSeconds(at: now) >= timeoutSeconds else {
            return false
        }
        state = .expired
        return true
    }

    func elapsedSeconds(at now: TimeInterval) -> TimeInterval {
        max(0, now - armedAt)
    }
}
