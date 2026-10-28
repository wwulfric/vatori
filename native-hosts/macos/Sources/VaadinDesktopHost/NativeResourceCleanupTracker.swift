import Foundation

/// Tracks asynchronous releases independently of the lifetime of their owner.
/// All registration, completion and drain operations belong to the main thread.
final class NativeResourceCleanupTracker {
    struct DrainResult {
        let drained: Bool
        let pendingCount: Int
        let failureCount: Int
    }

    private var nextToken = 0
    private var pendingTokens: Set<Int> = []
    private var failureCount = 0
    private var drainStarted = false
    private var drainResult: DrainResult?
    private var drainCompletions: [(DrainResult) -> Void] = []

    var pendingCount: Int { pendingTokens.count }

    func begin() -> Int {
        precondition(Thread.isMainThread)
        precondition(drainResult == nil,
                     "Native resources cannot start closing after shutdown drained")
        nextToken += 1
        pendingTokens.insert(nextToken)
        return nextToken
    }

    @discardableResult
    func complete(_ token: Int, succeeded: Bool) -> Bool {
        precondition(Thread.isMainThread)
        guard pendingTokens.remove(token) != nil else {
            return false
        }
        if !succeeded {
            failureCount += 1
        }
        finishDrainIfReady()
        return true
    }

    /// Completion is always deferred, including an empty or repeated drain.
    /// A timeout reports outstanding work; it never fabricates its completion.
    func drain(
        timeout: TimeInterval,
        completion: @escaping (DrainResult) -> Void
    ) {
        precondition(Thread.isMainThread)
        precondition(timeout.isFinite && timeout >= 0)
        if let drainResult {
            NativeMainRunLoop.perform { completion(drainResult) }
            return
        }
        drainCompletions.append(completion)
        guard !drainStarted else {
            return
        }
        drainStarted = true
        NativeMainRunLoop.perform { [weak self] in
            self?.finishDrainIfReady()
        }
        NativeMainRunLoop.perform(after: timeout) { [weak self] in
            self?.finishDrain()
        }
    }

    private func finishDrainIfReady() {
        precondition(Thread.isMainThread)
        guard drainStarted, pendingTokens.isEmpty else {
            return
        }
        finishDrain()
    }

    private func finishDrain() {
        precondition(Thread.isMainThread)
        guard drainResult == nil else {
            return
        }
        let result = DrainResult(
            drained: pendingTokens.isEmpty,
            pendingCount: pendingTokens.count,
            failureCount: failureCount)
        drainResult = result
        let completions = drainCompletions
        drainCompletions.removeAll()
        NativeMainRunLoop.perform {
            for completion in completions {
                completion(result)
            }
        }
    }
}
