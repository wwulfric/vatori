import AppKit

@main
struct NativeResourceCleanupSmoke {
    static func main() {
        do {
            try run()
        } catch {
            fputs("Native resource cleanup smoke failed: \(error)\n", stderr)
            exit(1)
        }
    }

    private static func run() throws {
        // A broken scheduler must not leave the smoke process alive forever.
        let failSafe = DispatchWorkItem {
            fputs("Cleanup smoke exceeded its independent deadline\n", stderr)
            exit(99)
        }
        DispatchQueue.global().asyncAfter(deadline: .now() + 15, execute: failSafe)
        defer { failSafe.cancel() }
        try insideMainDispatchCallback {
            try emptyAndRepeatedDrainInsideModalLoop()
            try pendingCleanupInsideModalLoop()
            try timeoutInsideModalLoop()
        }
        try emptyDrainIsAsynchronous()
        try tracksResourcesAfterOwnerRetirement()
        try completionAndRepeatedDrainAreIdempotent()
        try failuresAreNotReportedAsSuccessfulCleanup()
        try timeoutPreservesPendingWork()
        print("Native resource cleanup tracker smoke passed")
    }

    private static func emptyAndRepeatedDrainInsideModalLoop() throws {
        let tracker = NativeResourceCleanupTracker()
        var results: [NativeResourceCleanupTracker.DrainResult] = []
        tracker.drain(timeout: 0.02) { results.append($0) }
        try require(results.isEmpty, "modal empty drain completed inline")
        try pumpModal { results.count == 1 }
        tracker.drain(timeout: 0) { results.append($0) }
        try require(results.count == 1, "modal repeated drain completed inline")
        try pumpModal { results.count == 2 }
        try require(results.allSatisfy {
            $0.drained && $0.pendingCount == 0 && $0.failureCount == 0
        }, "modal empty/repeated drain changed its result")
        var afterDeadline = false
        NativeMainRunLoop.perform(after: 0.04) { afterDeadline = true }
        try pumpModal { afterDeadline }
        try require(results.count == 2,
                    "modal completed drain delivered again at its deadline")
    }

    private static func pendingCleanupInsideModalLoop() throws {
        let tracker = NativeResourceCleanupTracker()
        let token = tracker.begin()
        var results: [NativeResourceCleanupTracker.DrainResult] = []
        var completionWasOnMainThread = false
        var acceptedCompletion = false
        var rejectedDuplicate = false
        tracker.drain(timeout: 1) { results.append($0) }
        tracker.drain(timeout: 0) { results.append($0) }
        // The worker represents a queued resource close; it uses the same
        // return path as the production file-handle close completion.
        DispatchQueue.global().async {
            NativeMainRunLoop.perform {
                completionWasOnMainThread = Thread.isMainThread
                acceptedCompletion = tracker.complete(token, succeeded: true)
                rejectedDuplicate = !tracker.complete(token, succeeded: false)
            }
        }
        try require(results.isEmpty, "modal pending drain completed inline")
        try pumpModal { results.count == 2 }
        try require(completionWasOnMainThread && acceptedCompletion
                        && rejectedDuplicate && tracker.pendingCount == 0
                        && results.allSatisfy {
                            $0.drained && $0.pendingCount == 0 && $0.failureCount == 0
                        }, "modal cleanup delivery lost main-thread/exactly-once ownership")
    }

    private static func timeoutInsideModalLoop() throws {
        let tracker = NativeResourceCleanupTracker()
        let token = tracker.begin()
        var results: [NativeResourceCleanupTracker.DrainResult] = []
        tracker.drain(timeout: 0.02) { results.append($0) }
        try require(results.isEmpty, "modal timeout completed inline")
        try pumpModal { results.count == 1 }
        try require(!results[0].drained && results[0].pendingCount == 1
                        && tracker.pendingCount == 1,
                    "modal timeout fabricated cleanup")
        try require(tracker.complete(token, succeeded: false),
                    "modal late completion was rejected")
        tracker.drain(timeout: 0) { results.append($0) }
        try require(results.count == 1, "modal late repeated drain ran inline")
        try pumpModal { results.count == 2 }
        try require(results.allSatisfy {
            !$0.drained && $0.pendingCount == 1 && $0.failureCount == 0
        }, "modal late completion rewrote the captured timeout")
    }

    private static func insideMainDispatchCallback(
        _ body: @escaping () throws -> Void
    ) throws {
        var finished = false
        var failure: Error?
        DispatchQueue.main.async {
            dispatchPrecondition(condition: .onQueue(.main))
            do {
                try body()
            } catch {
                failure = error
            }
            finished = true
        }
        try pump { finished }
        if let failure { throw failure }
    }

    private static func pumpModal(until condition: () -> Bool) throws {
        dispatchPrecondition(condition: .onQueue(.main))
        // Keep the modal mode alive without creating NSApplication or windows.
        let keepAlive = Timer(fire: Date.distantFuture, interval: 0,
                              repeats: false) { _ in }
        RunLoop.main.add(keepAlive, forMode: .modalPanel)
        defer { keepAlive.invalidate() }
        let deadline = Date().addingTimeInterval(1.5)
        while !condition(), Date() < deadline {
            RunLoop.main.run(mode: .modalPanel,
                             before: Date().addingTimeInterval(0.005))
        }
        try require(condition(),
                    "cleanup callback starved in a main-dispatch modal run loop")
    }

    private static func emptyDrainIsAsynchronous() throws {
        let tracker = NativeResourceCleanupTracker()
        var result: NativeResourceCleanupTracker.DrainResult?
        tracker.drain(timeout: 1) { result = $0 }
        try require(result == nil, "empty drain completed synchronously")
        try pump { result != nil }
        try require(result?.drained == true
                        && result?.pendingCount == 0
                        && result?.failureCount == 0,
                    "empty drain did not report an empty successful release set")
    }

    private static func tracksResourcesAfterOwnerRetirement() throws {
        // Fake owners test tracker lifetime, not real file handles or executors.
        let tracker = NativeResourceCleanupTracker()
        var retiredOwner: ResourceOwner? = ResourceOwner(tracker: tracker)
        let activeOwner = ResourceOwner(tracker: tracker)
        let retiredToken = retiredOwner!.token
        weak var weakRetiredOwner = retiredOwner
        retiredOwner = nil
        try require(weakRetiredOwner == nil && tracker.pendingCount == 2,
                    "retiring an owner lost its unfinished cleanup")

        var result: NativeResourceCleanupTracker.DrainResult?
        tracker.drain(timeout: 1) { result = $0 }
        try require(tracker.complete(activeOwner.token, succeeded: true),
                    "active owner's completion was rejected")
        try require(result == nil && tracker.pendingCount == 1,
                    "drain ignored retired owner's queued cleanup")
        try require(tracker.complete(retiredToken, succeeded: true),
                    "retired owner's completion was rejected")
        try require(result == nil, "last completion invoked the drain inline")
        try pump { result != nil }
        try require(result?.drained == true && result?.pendingCount == 0,
                    "drain did not wait for both owners")
    }

    private static func completionAndRepeatedDrainAreIdempotent() throws {
        let tracker = NativeResourceCleanupTracker()
        let beforeDrain = tracker.begin()
        try require(tracker.complete(beforeDrain, succeeded: true),
                    "completion before drain was rejected")
        let token = tracker.begin()
        var results: [NativeResourceCleanupTracker.DrainResult] = []
        tracker.drain(timeout: 1) { results.append($0) }
        tracker.drain(timeout: 0) { results.append($0) }
        try require(tracker.complete(token, succeeded: true),
                    "pending cleanup completion was rejected")
        try require(!tracker.complete(token, succeeded: false),
                    "duplicate completion was accepted")
        try require(results.isEmpty, "reentrant drain callbacks ran inline")
        try pump { results.count == 2 }
        tracker.drain(timeout: 0) { results.append($0) }
        try require(results.count == 2, "completed drain callback ran inline")
        try pump { results.count == 3 }
        try require(results.allSatisfy {
            $0.drained && $0.pendingCount == 0 && $0.failureCount == 0
        }, "repeated drain or duplicate completion changed the result")
    }

    private static func failuresAreNotReportedAsSuccessfulCleanup() throws {
        let tracker = NativeResourceCleanupTracker()
        let token = tracker.begin()
        tracker.complete(token, succeeded: false)
        var result: NativeResourceCleanupTracker.DrainResult?
        tracker.drain(timeout: 1) { result = $0 }
        try pump { result != nil }
        try require(result?.drained == true
                        && result?.pendingCount == 0
                        && result?.failureCount == 1,
                    "a failed release was hidden by an empty pending set")
    }

    private static func timeoutPreservesPendingWork() throws {
        let tracker = NativeResourceCleanupTracker()
        let token = tracker.begin()
        var results: [NativeResourceCleanupTracker.DrainResult] = []
        tracker.drain(timeout: 0) { results.append($0) }
        try require(results.isEmpty, "timeout completed synchronously")
        try pump { results.count == 1 }
        try require(!results[0].drained
                        && results[0].pendingCount == 1
                        && results[0].failureCount == 0
                        && tracker.pendingCount == 1,
                    "timeout fabricated successful cleanup")
        try require(tracker.complete(token, succeeded: true),
                    "late cleanup completion was rejected")
        tracker.drain(timeout: 1) { results.append($0) }
        try pump { results.count == 2 }
        try require(tracker.pendingCount == 0
                        && results.count == 2
                        && results.allSatisfy {
                            !$0.drained && $0.pendingCount == 1
                        },
                    "late completion resettled the drain or rewrote its timeout")
    }

    private final class ResourceOwner {
        let token: Int

        init(tracker: NativeResourceCleanupTracker) {
            token = tracker.begin()
        }
    }

    private static func pump(until condition: () -> Bool) throws {
        let deadline = Date().addingTimeInterval(2)
        while !condition(), Date() < deadline {
            RunLoop.main.run(until: Date().addingTimeInterval(0.005))
        }
        try require(condition(), "asynchronous cleanup callback did not arrive")
    }

    private static func require(
        _ condition: @autoclosure () -> Bool,
        _ message: String
    ) throws {
        if !condition() {
            throw SmokeFailure(message: message)
        }
    }

    private struct SmokeFailure: Error, CustomStringConvertible {
        let message: String
        var description: String { message }
    }
}
