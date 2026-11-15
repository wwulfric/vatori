import Foundation

/// Host-memory records, independent of the requesting JVM's futures. The
/// lifecycle controller must authorize requests and establish actual process
/// outcomes; this store performs neither permission checks nor process actions.
/// Use only on the host's main-thread lifecycle executor. Callers must validate
/// that the supplied owner is still live; an owner UUID is never browser input.
final class RuntimeReplacementRecords {
    enum Outcome: String {
        case replaced
        case rolledBack = "rolled-back"
        case rollbackFailed = "rollback-failed"
        case retirementFailed = "retirement-failed"
        case interrupted
    }

    struct Record {
        let operationId: UUID
        // A native window lifetime identity, not a reusable window label or
        // a Java session/generation that is destroyed during replacement.
        let owner: UUID
        let requestId: String
        let candidateId: String
        let requestingGeneration: Int
        fileprivate(set) var outcome: Outcome?
    }

    enum Admission {
        case accepted(Record)
        case duplicate(Record)
        case rejected(Rejection)
    }

    enum Rejection: String {
        case invalidRequest = "invalid-request"
        case requestConflict = "request-conflict"
        case busy
        case capacity
        case closed
    }

    private struct RequestKey: Hashable {
        let owner: UUID
        let requestId: String
    }

    private let capacity: Int
    private var records: [UUID: Record] = [:]
    private var requests: [RequestKey: UUID] = [:]
    private var latestByOwner: [UUID: UUID] = [:]
    private var activeOperationId: UUID?
    private var detachedOperationId: UUID?
    private var closed = false

    init(capacity: Int = 256) {
        precondition(capacity > 0)
        self.capacity = capacity
    }

    func begin(owner: UUID, requestId: String, candidateId: String,
               requestingGeneration: Int) -> Admission {
        requireMainThread()
        guard !closed else { return .rejected(.closed) }
        guard isValidSidecarIdentifier(requestId),
              isValidSidecarIdentifier(candidateId), requestingGeneration > 0 else {
            return .rejected(.invalidRequest)
        }
        let key = RequestKey(owner: owner, requestId: requestId)
        if let detachedOperationId, records[detachedOperationId]?.owner == owner {
            return .rejected(.closed)
        }
        if let id = requests[key], let record = records[id] {
            guard record.candidateId == candidateId,
                  record.requestingGeneration == requestingGeneration else {
                return .rejected(.requestConflict)
            }
            return .duplicate(record)
        }
        guard activeOperationId == nil else { return .rejected(.busy) }
        // Do not evict and accidentally turn an old request into a new action.
        // Closing its native owner releases records; reusing the window label
        // must use a fresh owner UUID. Capacity exhaustion is side-effect-free.
        guard records.count < capacity else { return .rejected(.capacity) }
        let record = Record(operationId: UUID(), owner: owner,
                            requestId: requestId, candidateId: candidateId,
                            requestingGeneration: requestingGeneration)
        records[record.operationId] = record
        requests[key] = record.operationId
        latestByOwner[owner] = record.operationId
        activeOperationId = record.operationId
        return .accepted(record)
    }

    @discardableResult
    func finish(operationId: UUID, outcome: Outcome) -> Bool {
        requireMainThread()
        guard activeOperationId == operationId,
              var record = records[operationId], record.outcome == nil else {
            return false
        }
        record.outcome = outcome
        if detachedOperationId == operationId {
            records.removeValue(forKey: operationId)
            requests.removeValue(forKey: RequestKey(owner: record.owner, requestId: record.requestId))
            detachedOperationId = nil
        } else {
            records[operationId] = record
        }
        activeOperationId = nil
        return true
    }

    func result(owner: UUID, operationId: UUID) -> Record? {
        requireMainThread()
        guard operationId != detachedOperationId,
              let record = records[operationId], record.owner == owner else { return nil }
        return record
    }

    func latest(owner: UUID) -> Record? {
        requireMainThread()
        return latestByOwner[owner].flatMap { result(owner: owner, operationId: $0) }
    }

    /// Does not release the global active-operation fence. Closing a requester
    /// is not evidence that its child process has stopped. The controller must
    /// still settle the operation before another request can be admitted.
    func forgetOwner(_ owner: UUID) {
        requireMainThread()
        let owned = records.values.filter { $0.owner == owner }
        if let activeOperationId, records[activeOperationId]?.owner == owner {
            detachedOperationId = activeOperationId
        }
        for record in owned where record.operationId != activeOperationId {
            records.removeValue(forKey: record.operationId)
            requests.removeValue(forKey: RequestKey(owner: owner, requestId: record.requestId))
        }
        latestByOwner.removeValue(forKey: owner)
    }

    func shutdown() {
        requireMainThread()
        closed = true
        if let activeOperationId { finish(operationId: activeOperationId, outcome: .interrupted) }
    }

    private func requireMainThread() {
        precondition(Thread.isMainThread, "Runtime replacement records require the host lifecycle executor")
    }
}
