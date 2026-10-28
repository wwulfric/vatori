import Foundation

/// Exercises the production callback adapter, not UNUserNotificationCenter or
/// the entire executor. Mutable owner inputs below model a callback arriving
/// after its caller has invalidated/replaced an operation; they do not implement
/// a second copy of the production completion decision.
@main
struct NativeNotificationCompletionSmoke {
    static func main() throws {
        try require(Thread.isMainThread, "smoke must run on the owning main thread")
        try activeSuccessAndError()
        try inactiveAndMismatchedRegistrations()
        try invalidationBeforeLateSuccessAndError()
        try oldAndNewSubmissionsRemainIsolated()
        print("Native notification completion smoke passed (callback adapter only; no OS authorization/removal evidence)")
    }

    private static func activeSuccessAndError() throws {
        for result in ["success", "error"] {
            let recorder = Recorder()
            let callback = completion(
                submitted: "active-identifier", result: result,
                active: { true }, registered: { "active-identifier" },
                recorder: recorder)
            callback()
            try require(recorder.events == ["publish:\(result)"]
                        && recorder.published == [result]
                        && recorder.pending.isEmpty && recorder.delivered.isEmpty,
                        "active \(result) was dropped, rewritten or removed")
        }
    }

    private static func inactiveAndMismatchedRegistrations() throws {
        let cases: [(Bool, String?)] = [
            (false, "submitted"), (false, nil), (false, "replacement"),
            (true, nil), (true, "replacement")
        ]
        for (active, registered) in cases {
            let recorder = Recorder()
            completion(submitted: "submitted", result: "success",
                       active: { active }, registered: { registered },
                       recorder: recorder)()
            try requireDropped(recorder, identifiers: ["submitted"],
                               message: "inactive or mismatched registration")
            try require(!recorder.pending.flatMap { $0 }.contains("replacement")
                        && !recorder.delivered.flatMap { $0 }.contains("replacement"),
                        "stale callback removed a replacement identifier")
        }
    }

    private static func invalidationBeforeLateSuccessAndError() throws {
        for result in ["success", "error"] {
            var operationActive = true
            var registered: String? = "late-identifier"
            let recorder = Recorder()
            let callback = completion(
                submitted: "late-identifier", result: result,
                active: { operationActive }, registered: { registered },
                recorder: recorder)
            // These are caller-state inputs, not a test implementation of
            // DesktopCommandExecutor.invalidateNativeOperations. Even if an
            // earlier removal already ran, a late add callback needs fresh
            // identifier-scoped pending and delivered compensation.
            recorder.removePending(["late-identifier"])
            recorder.removeDelivered(["late-identifier"])
            operationActive = false
            registered = nil
            try require(recorder.events == ["pending:late-identifier", "delivered:late-identifier"],
                        "creating a callback executed it before the controlled completion")
            callback()
            try require(recorder.events == [
                "pending:late-identifier", "delivered:late-identifier",
                "pending:late-identifier", "delivered:late-identifier", "drop"
            ] && recorder.published.isEmpty,
                        "late \(result) did not compensate again before dropping")
            try require(!operationActive && registered == nil,
                        "callback changed invalidated caller state")
        }
    }

    private static func oldAndNewSubmissionsRemainIsolated() throws {
        // Reuse one operation token's registration and retire another, while a
        // third unrelated operation remains active. The adapter must use each
        // callback's captured identifier, never the current registration.
        var activeTokens: Set<Int> = [1, 2, 3]
        var registrations = [1: "old-a", 2: "old-b", 3: "unrelated"]
        let recorder = Recorder()
        let oldA = completion(submitted: "old-a", result: "success",
                              active: { activeTokens.contains(1) },
                              registered: { registrations[1] }, recorder: recorder)
        let oldB = completion(submitted: "old-b", result: "error",
                              active: { activeTokens.contains(2) },
                              registered: { registrations[2] }, recorder: recorder)
        registrations[1] = "new-a"
        activeTokens.remove(2)
        registrations.removeValue(forKey: 2)
        let expectedRegistrations = registrations
        let expectedActiveTokens = activeTokens
        oldB()
        oldA()
        try requireDropped(recorder, identifiers: ["old-b", "old-a"],
                           message: "retired and replaced callback isolation")
        try require(registrations == expectedRegistrations && activeTokens == expectedActiveTokens,
                    "stale callbacks changed current operation registrations")
        let cleanupBefore = recorder.events
        completion(submitted: "new-a", result: "new-success",
                   active: { activeTokens.contains(1) },
                   registered: { registrations[1] }, recorder: recorder)()
        completion(submitted: "unrelated", result: "unrelated-error",
                   active: { activeTokens.contains(3) },
                   registered: { registrations[3] }, recorder: recorder)()
        try require(recorder.events == cleanupBefore + ["publish:new-success", "publish:unrelated-error"]
                    && recorder.published == ["new-success", "unrelated-error"]
                    && recorder.pending == [["old-b"], ["old-a"]]
                    && recorder.delivered == [["old-b"], ["old-a"]],
                    "old callbacks interfered with new or unrelated completions")
    }

    private static func completion(
        submitted: String, result: String,
        active: @escaping () -> Bool, registered: @escaping () -> String?,
        recorder: Recorder
    ) -> () -> Void {
        {
            NativeNotificationCompletion.complete(
                submittedIdentifier: submitted,
                operationActive: active(), registeredIdentifier: registered(),
                removePending: recorder.removePending,
                removeDelivered: recorder.removeDelivered,
                publish: {
                    recorder.events.append("publish:\(result)")
                    recorder.published.append(result)
                },
                drop: { recorder.events.append("drop") })
        }
    }

    private static func requireDropped(_ recorder: Recorder, identifiers: [String],
                                       message: String) throws {
        let expected = identifiers.flatMap { ["pending:\($0)", "delivered:\($0)", "drop"] }
        try require(recorder.events == expected && recorder.published.isEmpty
                    && recorder.pending == identifiers.map { [$0] }
                    && recorder.delivered == identifiers.map { [$0] },
                    "\(message): cleanup was not exact pending -> delivered -> drop")
    }

    private final class Recorder {
        var events: [String] = []
        var published: [String] = []
        var pending: [[String]] = []
        var delivered: [[String]] = []

        func removePending(_ identifiers: [String]) {
            events.append("pending:" + identifiers.joined(separator: ","))
            pending.append(identifiers)
        }

        func removeDelivered(_ identifiers: [String]) {
            events.append("delivered:" + identifiers.joined(separator: ","))
            delivered.append(identifiers)
        }
    }

    private struct Failure: Error, CustomStringConvertible {
        let description: String
    }

    private static func require(_ condition: @autoclosure () -> Bool, _ message: String) throws {
        if !condition() { throw Failure(description: message) }
    }
}
