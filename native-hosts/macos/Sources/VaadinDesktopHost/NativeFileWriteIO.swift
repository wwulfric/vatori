import Darwin
import Foundation

/// Byte-oriented primitives for host-owned, create-new-only file grants.
/// Neither a successful write nor close promises fsync or atomic publication.
enum NativeFileWriteIO {
    static let maxWriteBytes = 64 * 1024
    static let maxEncodedWriteBytes = ((maxWriteBytes + 2) / 3) * 4

    /// Only this signal crosses the main-thread/worker boundary. Do not hold
    /// its lock across write(2): cancellation cannot interrupt a blocking call.
    final class CancellationSignal: @unchecked Sendable {
        private let lock = NSLock()
        private var requested = false

        func requestCancel() {
            lock.lock()
            requested = true
            lock.unlock()
        }

        var isRequested: Bool {
            lock.lock()
            defer { lock.unlock() }
            return requested
        }
    }

    /// Main-thread-only close intent. Cleanup is registered when cancel is
    /// admitted, but the descriptor must not close until its final prefix has
    /// been published on the main thread (not merely returned by the worker).
    struct CancellationCloseBarrier {
        struct Pending {
            let operationToken: Int
            let cleanupToken: Int
        }
        private var pending: Pending?

        mutating func request(operationToken: Int, cleanupToken: Int) {
            precondition(pending == nil)
            pending = Pending(operationToken: operationToken, cleanupToken: cleanupToken)
        }

        mutating func takeIfWriteSettled(writeInFlight: Bool) -> Pending? {
            guard !writeInFlight else { return nil }
            defer { pending = nil }
            return pending
        }
    }

    enum CreateResult {
        case opened(Int32)
        case failed(Int32)
    }

    struct WriteResult {
        let bytesWritten: Int
        let errorNumber: Int32?
        let canceled: Bool

        init(bytesWritten: Int, errorNumber: Int32?, canceled: Bool = false) {
            self.bytesWritten = bytesWritten
            self.errorNumber = errorNumber
            self.canceled = canceled
        }
    }

    static func createNew(at url: URL) -> CreateResult {
        url.withUnsafeFileSystemRepresentation { path -> CreateResult in
            guard let path else { return .failed(EINVAL) }
            let descriptor = Darwin.open(
                path, O_CREAT | O_EXCL | O_WRONLY | O_CLOEXEC | O_NOFOLLOW,
                mode_t(S_IRUSR | S_IWUSR))
            return descriptor >= 0 ? .opened(descriptor) : .failed(errno)
        }
    }

    static func boundedBody(_ value: Any?) -> Data? {
        guard let encoded = value as? String,
              !encoded.isEmpty,
              encoded.utf8.count <= maxEncodedWriteBytes,
              let data = Data(base64Encoded: encoded),
              !data.isEmpty, data.count <= maxWriteBytes,
              data.base64EncodedString() == encoded else {
            return nil
        }
        return data
    }

    static func validRange(offset: Int64, nextOffset: Int64, byteCount: Int) -> Bool {
        offset >= 0 && offset == nextOffset
            && byteCount > 0 && byteCount <= maxWriteBytes
            && offset <= Int64.max - Int64(byteCount)
    }

    static func writeAll(
        descriptor: Int32, data: Data, cancellation: CancellationSignal? = nil
    ) -> WriteResult {
        writeAll(data: data, shouldCancel: { cancellation?.isRequested == true }) { pointer, count in
            let written = Darwin.write(descriptor, pointer, count)
            return (written, written < 0 ? errno : 0)
        }
    }

    // Keeping the syscall behind a closure permits deterministic short-write
    // and interruption checks without placing fault injection in the runtime.
    static func writeAll(
        data: Data,
        shouldCancel: () -> Bool = { false },
        operation: (UnsafeRawPointer, Int) -> (count: Int, error: Int32)
    ) -> WriteResult {
        guard !data.isEmpty, data.count <= maxWriteBytes else {
            return WriteResult(bytesWritten: 0, errorNumber: EINVAL)
        }
        return data.withUnsafeBytes { buffer in
            guard let base = buffer.baseAddress else {
                return WriteResult(bytesWritten: 0, errorNumber: EINVAL)
            }
            var written = 0
            while written < buffer.count {
                // A request can race after this checkpoint. That attempt may
                // finish, including the complete block; never rewrite a full
                // successful result as cancellation after the fact.
                if shouldCancel() {
                    return WriteResult(bytesWritten: written, errorNumber: nil, canceled: true)
                }
                let remaining = buffer.count - written
                let attempt = operation(base.advanced(by: written), remaining)
                if attempt.count < 0, attempt.error == EINTR { continue }
                guard attempt.count > 0, attempt.count <= remaining else {
                    return WriteResult(
                        bytesWritten: written,
                        errorNumber: attempt.count < 0 ? attempt.error : EIO)
                }
                written += attempt.count
            }
            return WriteResult(bytesWritten: written, errorNumber: nil)
        }
    }

    static func close(descriptor: Int32) -> Bool {
        // Do not retry close after EINTR: the descriptor may already have been
        // closed and reused by another operation in this process.
        Darwin.close(descriptor) == 0
    }
}
