import Foundation

@main
struct RuntimeReplacementRecordsSmoke {
    static func main() {
        typealias Store = RuntimeReplacementRecords
        let owner = UUID(), sibling = UUID(), reopened = UUID()
        let store = Store(capacity: 2)
        let requestCommand = RuntimeReplacementRequestCommand.name
        var admissions = 0
        let requestArgs: [String: Any] = ["requestId": "request", "candidateId": "next"]
        func admit(_ args: [String: Any], mode: String = "manifest-enforced",
                   owner: Bool = true, allowed: Bool = true) -> [String: Any] {
            let policy = CapabilityManifest(path: "test", loaded: true, permissionMode: mode,
                commandsByWindow: ["main": allowed ? [requestCommand] : []], parseError: "")
            return RuntimeReplacementRequestCommand.execute(args: args, hasOwner: owner,
                windowId: "main", manifest: policy) { request, candidate in
                    precondition(request == "request" && candidate == "next")
                    admissions += 1
                    return ["success": true, "status": "pending"]
                }
        }
        for mode in ["allow-all-dev", "trace-only-dry-run", "invalid"] {
            precondition(admit(requestArgs, mode: mode)["permissionDecision"] as? String == "denied")
        }
        precondition(admit(requestArgs, allowed: false)["permissionDecision"] as? String == "denied")
        precondition(admit(requestArgs, owner: false)["operationInvalidated"] as? Bool == true)
        for malformed: [String: Any] in [[:], ["requestId": "request"],
                ["requestId": "request", "candidateId": "../next"],
                ["requestId": "request", "candidateId": "next", "owner": "other"]] {
            precondition(admit(malformed)["success"] as? Bool == false)
        }
        precondition(admissions == 0)
        precondition(admit(requestArgs)["status"] as? String == "pending")
        precondition(admissions == 1)
        let queryStore = Store()
        let command = RuntimeReplacementStatusCommand.name
        let manifest = CapabilityManifest(path: "test", loaded: true, permissionMode: "manifest-enforced",
            commandsByWindow: ["main": [command]], parseError: "")
        func query(_ owner: UUID?, window: String = "main", args: [String: Any] = [:]) -> [String: Any] {
            RuntimeReplacementStatusCommand.execute(args: args, owner: owner, windowId: window,
                manifest: manifest, records: queryStore)
        }
        precondition(query(owner)["status"] as? String == "none")
        precondition(query(owner, window: "sibling")["permissionDecision"] as? String == "denied")
        precondition(query(nil)["operationInvalidated"] as? Bool == true)
        precondition(query(owner, args: ["owner": sibling.uuidString])["success"] as? Bool == false)
        guard case .accepted(let queryRecord) = queryStore.begin(owner: owner, requestId: "query-test",
            candidateId: "next", requestingGeneration: 1) else { fatalError("query admission") }
        precondition(query(owner)["status"] as? String == "pending")
        precondition(query(sibling)["status"] as? String == "none")
        precondition(query(owner)["operationId"] as? String == queryRecord.operationId.uuidString)
        queryStore.finish(operationId: queryRecord.operationId, outcome: .replaced)
        precondition(query(owner)["status"] as? String == "replaced")
        queryStore.forgetOwner(owner)
        precondition(query(reopened)["status"] as? String == "none")
        func begin(_ owner: UUID, _ request: String, _ candidate: String = "next",
                   generation: Int = 1) -> Store.Admission {
            store.begin(owner: owner, requestId: request, candidateId: candidate,
                        requestingGeneration: generation)
        }
        func rejected(_ admission: Store.Admission, _ expected: Store.Rejection) {
            guard case .rejected(let reason) = admission, reason == expected else {
                fatalError("expected rejection \(expected)")
            }
        }
        func accepted(_ admission: Store.Admission) -> Store.Record {
            guard case .accepted(let record) = admission else { fatalError("expected acceptance") }
            return record
        }
        for invalid in ["", "../next", String(repeating: "x", count: 129)] {
            rejected(begin(owner, invalid), .invalidRequest)
            rejected(begin(owner, "valid", invalid), .invalidRequest)
        }
        rejected(begin(owner, "valid", generation: 0), .invalidRequest)
        precondition(store.latest(owner: owner) == nil)
        let first = accepted(begin(owner, "request-1"))
        precondition(first.outcome == nil)
        precondition(store.result(owner: sibling, operationId: first.operationId) == nil)
        precondition(store.latest(owner: sibling) == nil)
        guard case .duplicate(let duplicate) = begin(owner, "request-1") else { fatalError("duplicate admitted") }
        precondition(duplicate.operationId == first.operationId)
        rejected(begin(owner, "request-1", "different"), .requestConflict)
        rejected(begin(owner, "request-1", generation: 2), .requestConflict)
        rejected(begin(sibling, "request-1"), .busy)
        rejected(begin(owner, "request-2"), .busy)
        precondition(!store.finish(operationId: UUID(), outcome: .replaced))
        precondition(store.finish(operationId: first.operationId, outcome: .rolledBack))
        precondition(!store.finish(operationId: first.operationId, outcome: .replaced))
        precondition(store.result(owner: owner, operationId: first.operationId)?.outcome == .rolledBack)
        guard case .duplicate(let terminal) = begin(owner, "request-1") else { fatalError("terminal replay admitted") }
        precondition(terminal.outcome == .rolledBack)
        let second = accepted(begin(sibling, "request-1", generation: 2))
        precondition(second.operationId != first.operationId)
        precondition(store.finish(operationId: second.operationId, outcome: .replaced))
        rejected(begin(owner, "request-3"), .capacity)
        // Capacity refusal does not evict a terminal result or its replay guard.
        precondition(store.latest(owner: owner)?.operationId == first.operationId)
        store.forgetOwner(owner)
        precondition(store.latest(owner: owner) == nil)
        precondition(store.result(owner: owner, operationId: first.operationId) == nil)
        precondition(store.result(owner: reopened, operationId: first.operationId) == nil)
        let third = accepted(begin(reopened, "request-1", generation: 3))
        store.forgetOwner(reopened)
        precondition(store.latest(owner: reopened) == nil)
        precondition(store.result(owner: reopened, operationId: third.operationId) == nil)
        rejected(begin(reopened, "request-1", generation: 3), .closed)
        rejected(begin(sibling, "request-2", generation: 3), .busy)
        precondition(!store.finish(operationId: first.operationId, outcome: .replaced))
        precondition(store.finish(operationId: third.operationId, outcome: .retirementFailed))
        // Detached completion clears the fence but cannot republish a dead owner.
        precondition(store.result(owner: reopened, operationId: third.operationId) == nil)
        let fourth = accepted(begin(sibling, "request-2", generation: 4))
        store.shutdown()
        store.shutdown()
        precondition(store.latest(owner: sibling)?.outcome == .interrupted)
        precondition(!store.finish(operationId: fourth.operationId, outcome: .replaced))
        rejected(begin(UUID(), "request-1"), .closed)

        // Every terminal outcome is immutable; a late callback cannot complete
        // the following operation even when request labels match across owners.
        for outcome: Store.Outcome in [.replaced, .rolledBack, .rollbackFailed,
                                        .retirementFailed, .interrupted] {
            let records = Store(capacity: 2)
            let operation = accepted(records.begin(owner: owner, requestId: "request",
                candidateId: "next", requestingGeneration: 1))
            precondition(records.finish(operationId: operation.operationId, outcome: outcome))
            let next = accepted(records.begin(owner: sibling, requestId: "request",
                candidateId: "next", requestingGeneration: 2))
            precondition(!records.finish(operationId: operation.operationId, outcome: .interrupted))
            precondition(records.latest(owner: sibling)?.operationId == next.operationId)
            precondition(records.latest(owner: sibling)?.outcome == nil)
            precondition(records.latest(owner: owner)?.outcome == outcome)
        }
        // An owner can disappear during any lifecycle phase. Detachment must
        // hide its record without freeing capacity/admission until that exact
        // operation settles. Exercise each terminal outcome, including failure.
        for outcome: Store.Outcome in [.replaced, .rolledBack, .rollbackFailed,
                                        .retirementFailed, .interrupted] {
            let records = Store(capacity: 1)
            let active = accepted(records.begin(owner: owner, requestId: "same-request",
                candidateId: "next", requestingGeneration: 1))
            records.forgetOwner(owner)
            records.forgetOwner(owner)
            precondition(records.latest(owner: owner) == nil)
            for observer in [owner, sibling, reopened] {
                precondition(records.result(owner: observer, operationId: active.operationId) == nil)
            }
            rejected(records.begin(owner: owner, requestId: "another-request",
                candidateId: "next", requestingGeneration: 1), .closed)
            rejected(records.begin(owner: reopened, requestId: "same-request",
                candidateId: "next", requestingGeneration: 2), .busy)
            precondition(!records.finish(operationId: UUID(), outcome: outcome))
            rejected(records.begin(owner: sibling, requestId: "another-request",
                candidateId: "next", requestingGeneration: 2), .busy)
            precondition(records.finish(operationId: active.operationId, outcome: outcome))
            precondition(records.latest(owner: owner) == nil)
            precondition(records.result(owner: owner, operationId: active.operationId) == nil)
            // Capacity one proves detached completion actually releases storage.
            let next = accepted(records.begin(owner: reopened, requestId: "same-request",
                candidateId: "next", requestingGeneration: 2))
            precondition(next.operationId != active.operationId)
            records.forgetOwner(owner) // Late duplicate close cannot detach new owner.
            precondition(!records.finish(operationId: active.operationId, outcome: .replaced))
            precondition(records.latest(owner: reopened)?.operationId == next.operationId)
            precondition(records.latest(owner: reopened)?.outcome == nil)
            rejected(records.begin(owner: sibling, requestId: "third-request",
                candidateId: "next", requestingGeneration: 2), .busy)
            precondition(records.finish(operationId: next.operationId, outcome: .replaced))
        }
        let detachedShutdown = Store(capacity: 1)
        let shutdownOperation = accepted(detachedShutdown.begin(owner: owner, requestId: "shutdown",
            candidateId: "next", requestingGeneration: 1))
        detachedShutdown.forgetOwner(owner)
        detachedShutdown.shutdown()
        detachedShutdown.shutdown()
        precondition(detachedShutdown.latest(owner: owner) == nil)
        precondition(detachedShutdown.result(owner: owner, operationId: shutdownOperation.operationId) == nil)
        precondition(!detachedShutdown.finish(operationId: shutdownOperation.operationId, outcome: .replaced))
        rejected(detachedShutdown.begin(owner: reopened, requestId: "shutdown",
            candidateId: "next", requestingGeneration: 2), .closed)
        print("Runtime replacement records smoke passed (admission, replay, ownership, capacity, terminal fencing and shutdown; no process or public API evidence)")
    }
}
