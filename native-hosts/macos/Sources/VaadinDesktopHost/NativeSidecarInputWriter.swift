import Darwin
import Foundation

/// One generation's byte channel. Admission is bounded and does not mean that
/// the bytes have reached the pipe; only onWritten reports a complete frame.
/// The caller must retire/cancel this owner, never close its handle directly.
final class NativeSidecarInputWriter {
    struct Failure: Error {
        let cause: String
        let signalErrno: Int32?
    }

    private enum Intent {
        case accepting
        case retiring(Data)
        case canceled
    }

    private let queue = DispatchQueue(label: "dev.vatori.sidecar-input-writer", qos: .userInitiated)
    // This lock only protects short in-memory admission and lifecycle changes.
    // No syscall, callback, queue.sync, or wait is performed while holding it.
    private let lock = NSLock()
    private var intent = Intent.accepting
    private var failure: Failure?
    private var closed = false
    private var frames: [Data] = []
    private var retainedBytes = 0
    private var retainedFrames = 0
    private var wakeScheduled = false

    private let handle: FileHandle
    private let maximumPendingBytes: Int
    private let maximumPendingFrames: Int
    private let idleTimeout: TimeInterval
    private let onWritten: (Int) -> Void
    private let onBackpressure: (Int, Int, Int) -> Void
    private let onFailure: (Failure) -> Void

    // Everything below belongs exclusively to queue after initialization.
    private var source: DispatchSourceWrite?
    private var sourceSuspended = true
    private var timer: DispatchSourceTimer?
    private var current: Data?
    private var offset = 0
    private var lastProgress: DispatchTime?
    private var backpressureReported = false
    private var failureReported = false
    private static let drainByteBudget = 256 * 1024
    private static let drainCallBudget = 16

    /// A throwing initializer has installed no live source and has not closed
    /// handle. The caller retains ownership on that path.
    init(
        handle: FileHandle,
        maximumPendingBytes: Int = 16 * 1024 * 1024,
        maximumPendingFrames: Int = 256,
        idleTimeout: TimeInterval = 10,
        onWritten: @escaping (Int) -> Void,
        onBackpressure: @escaping (Int, Int, Int) -> Void,
        onFailure: @escaping (Failure) -> Void
    ) throws {
        precondition(maximumPendingBytes > 0 && maximumPendingFrames > 0)
        precondition(idleTimeout.isFinite && idleTimeout > 0)
        self.handle = handle
        self.maximumPendingBytes = maximumPendingBytes
        self.maximumPendingFrames = maximumPendingFrames
        self.idleTimeout = idleTimeout
        self.onWritten = onWritten
        self.onBackpressure = onBackpressure
        self.onFailure = onFailure
        let descriptor = handle.fileDescriptor
        let flags = Darwin.fcntl(descriptor, F_GETFL)
        guard flags != -1 else {
            throw Failure(cause: "input-flags-unavailable", signalErrno: errno)
        }
        guard Darwin.fcntl(descriptor, F_SETNOSIGPIPE, 1) != -1,
              Darwin.fcntl(descriptor, F_SETFL, flags | O_NONBLOCK) != -1 else {
            throw Failure(cause: "input-nonblocking-unavailable", signalErrno: errno)
        }
        let source = DispatchSource.makeWriteSource(fileDescriptor: descriptor, queue: queue)
        self.source = source
        source.setEventHandler { [weak self] in self?.drain() }
        // A canceled dispatch source can still own its descriptor until this
        // handler. Closing earlier could target a reused descriptor number.
        source.setCancelHandler { try? handle.close() }
        let timer = DispatchSource.makeTimerSource(queue: queue)
        self.timer = timer
        timer.schedule(deadline: .distantFuture)
        timer.setEventHandler { [weak self] in self?.checkDeadline() }
        timer.activate()
    }

    deinit {
        timer?.cancel()
        // Queued work holds self, and an executing event handler holds its weak
        // capture strongly for its call. Thus no owner-queue state can mutate
        // concurrently with this last-reference cleanup.
        if let source {
            source.cancel()
            if sourceSuspended { source.resume() }
        }
    }

    @discardableResult
    func enqueue(_ data: Data) -> Bool {
        lock.lock()
        guard !closed, failure == nil, case .accepting = intent else {
            lock.unlock()
            return false
        }
        let accepted: Bool
        if data.isEmpty {
            failure = Failure(cause: "empty-frame", signalErrno: nil)
            accepted = false
        } else if retainedFrames >= maximumPendingFrames
                    || data.count > maximumPendingBytes - retainedBytes {
            // Once any frame cannot be admitted, continuing this channel would
            // silently omit part of its ordered protocol. Fail the whole owner.
            failure = Failure(cause: "queue-capacity-exceeded", signalErrno: nil)
            accepted = false
        } else {
            frames.append(data)
            retainedBytes += data.count
            retainedFrames += 1
            accepted = true
        }
        lock.unlock()
        requestWake()
        return accepted
    }

    /// Fences admission synchronously. Ordinary queued work is canceled, never
    /// replayed. A partial frame permits EOF only, not an inserted control frame.
    /// The small best-effort shutdown frame intentionally has no onWritten event.
    func retire(shutdownFrame: Data) {
        lock.lock()
        if !closed, case .accepting = intent {
            intent = .retiring(shutdownFrame)
        }
        lock.unlock()
        requestWake()
    }

    func cancel() {
        lock.lock()
        if !closed { intent = .canceled }
        lock.unlock()
        requestWake()
    }

    private func requestWake() {
        lock.lock()
        guard !closed, !wakeScheduled else { lock.unlock(); return }
        wakeScheduled = true
        lock.unlock()
        queue.async {
            self.lock.lock()
            self.wakeScheduled = false
            self.lock.unlock()
            self.drain()
        }
    }

    private func drain() {
        var calls = 0
        var bytes = 0
        while calls < Self.drainCallBudget && bytes < Self.drainByteBudget {
            lock.lock()
            let isClosed = closed
            let requestedFailure = failure
            let requestedIntent = intent
            lock.unlock()
            if isClosed { return }
            if let requestedFailure {
                finish(failure: requestedFailure)
                return
            }
            switch requestedIntent {
            case .canceled:
                finish()
                return
            case .retiring(let shutdownFrame):
                if offset == 0 { writeShutdownBestEffort(shutdownFrame) }
                finish()
                return
            case .accepting:
                break
            }
            if current == nil {
                lock.lock()
                if !frames.isEmpty { current = frames.removeFirst() }
                lock.unlock()
                offset = 0
            }
            guard let current else {
                setMonitoring(false)
                lastProgress = nil
                timer?.schedule(deadline: .distantFuture)
                backpressureReported = false
                return
            }
            if lastProgress == nil { noteProgress() }
            setMonitoring(true)
            let count = min(current.count - offset, Self.drainByteBudget - bytes)
            let written = current.withUnsafeBytes { buffer in
                Darwin.write(handle.fileDescriptor, buffer.baseAddress!.advanced(by: offset), count)
            }
            let errorNumber = written < 0 ? errno : 0
            calls += 1
            if written > 0 {
                offset += written
                bytes += written
                noteProgress()
                if offset == current.count {
                    self.current = nil
                    offset = 0
                    lock.lock()
                    retainedBytes -= current.count
                    retainedFrames -= 1
                    let fullyDrained = retainedFrames == 0
                    lock.unlock()
                    if fullyDrained {
                        backpressureReported = false
                        lastProgress = nil
                        timer?.schedule(deadline: .distantFuture)
                    }
                    onWritten(current.count)
                }
            } else if written < 0 && errorNumber == EINTR {
                continue
            } else if written < 0 && (errorNumber == EAGAIN || errorNumber == EWOULDBLOCK) {
                if !backpressureReported {
                    backpressureReported = true
                    lock.lock()
                    let pendingBytes = retainedBytes
                    let pendingFrames = retainedFrames
                    lock.unlock()
                    onBackpressure(pendingBytes, pendingFrames, offset)
                }
                return
            } else {
                fail(Failure(cause: errorNumber == EPIPE ? "pipe-closed" : "write-failed",
                             signalErrno: written < 0 ? errorNumber : EIO))
                return
            }
        }
        // A permanently writable pipe must not monopolize this serial queue.
        // Coalesced work gives cancellation and timer handlers a scheduling turn.
        requestWake()
    }

    private func setMonitoring(_ enabled: Bool) {
        guard let source else { return }
        if enabled && sourceSuspended {
            sourceSuspended = false
            source.resume()
        } else if !enabled && !sourceSuspended {
            sourceSuspended = true
            source.suspend()
        }
    }

    private func noteProgress() {
        let now = DispatchTime.now()
        lastProgress = now
        timer?.schedule(deadline: now + idleTimeout)
    }

    private func checkDeadline() {
        lock.lock()
        let pending = retainedFrames > 0 && !closed && failure == nil
        let accepting: Bool
        if case .accepting = intent { accepting = true } else { accepting = false }
        lock.unlock()
        guard pending, accepting, let lastProgress else { return }
        let deadline = lastProgress + idleTimeout
        if DispatchTime.now() >= deadline {
            fail(Failure(cause: "write-timeout", signalErrno: nil))
        } else {
            timer?.schedule(deadline: deadline)
        }
    }

    private func fail(_ reason: Failure) {
        lock.lock()
        if failure == nil {
            guard case .accepting = intent else {
                lock.unlock()
                finish()
                return
            }
            failure = reason
        }
        let firstFailure = failure!
        lock.unlock()
        finish(failure: firstFailure)
    }

    private func finish(failure reason: Failure? = nil) {
        lock.lock()
        guard !closed else { lock.unlock(); return }
        closed = true
        let discarded = frames
        frames = []
        retainedBytes = 0
        retainedFrames = 0
        intent = .canceled
        lock.unlock()
        // Release retained buffers outside the admission lock.
        withExtendedLifetime(discarded) { }
        current = nil
        offset = 0
        lastProgress = nil
        timer?.cancel()
        timer = nil
        if let source {
            self.source = nil
            source.cancel()
            if sourceSuspended {
                sourceSuspended = false
                source.resume()
            }
        }
        if let reason, !failureReported {
            failureReported = true
            onFailure(reason)
        }
    }

    private func writeShutdownBestEffort(_ data: Data) {
        guard !data.isEmpty, data.count <= maximumPendingBytes else { return }
        // There is no queue or retry deadline for this optional control frame.
        // Any short write or backpressure falls through to EOF; signal-based
        // retirement is owned independently by JavaSidecar.
        var calls = 0
        repeat {
            let written = data.withUnsafeBytes { buffer in
                Darwin.write(handle.fileDescriptor, buffer.baseAddress!,
                             min(buffer.count, Self.drainByteBudget))
            }
            let errorNumber = written < 0 ? errno : 0
            calls += 1
            if written >= 0 || errorNumber != EINTR { return }
        } while calls < Self.drainCallBudget
    }
}
