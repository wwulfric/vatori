import Foundation

/// Routes a notification add callback on the owning main thread. A stale
/// submission is removed again because the OS may have accepted it after the
/// owner's earlier invalidation/removal. Removal is best-effort, not proof that
/// a notification was never presented or that its sound was never played.
enum NativeNotificationCompletion {
    static func complete(
        submittedIdentifier: String,
        operationActive: Bool,
        registeredIdentifier: String?,
        removePending: ([String]) -> Void,
        removeDelivered: ([String]) -> Void,
        publish: () -> Void,
        drop: () -> Void
    ) {
        precondition(Thread.isMainThread)
        guard operationActive, registeredIdentifier == submittedIdentifier else {
            // Use only the captured submission identity, never the identifier
            // currently registered to a reused operation token.
            removePending([submittedIdentifier])
            removeDelivered([submittedIdentifier])
            drop()
            return
        }
        publish()
    }
}
