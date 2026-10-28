import Darwin
import Foundation

@main
struct NativeFileWriteIOSmoke {
    static func main() throws {
        try exclusiveCreateAndWrite()
        try boundedCanonicalBody()
        try sequentialRange()
        try interruptedAndPartialWrites()
        try cooperativeCancellation()
        try crossThreadCancellation()
        try cancellationCloseBarrier()
        print("Native create-new file write I/O smoke passed")
    }

    private static func exclusiveCreateAndWrite() throws {
        var template = Array("/private/tmp/vatori-file-write-io.XXXXXX".utf8CString)
        guard let path = mkdtemp(&template) else { throw Failure("mkdtemp failed") }
        let directory = URL(fileURLWithPath: String(cString: path), isDirectory: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let target = directory.appendingPathComponent("new-file.bin")
        guard case .opened(let descriptor) = NativeFileWriteIO.createNew(at: target) else {
            throw Failure("new destination was not created")
        }
        var open = true
        defer { if open { _ = NativeFileWriteIO.close(descriptor: descriptor) } }
        var attributes = stat()
        try require(fstat(descriptor, &attributes) == 0
                    && (attributes.st_mode & mode_t(S_IFMT)) == mode_t(S_IFREG)
                    && (attributes.st_mode & 0o077) == 0,
                    "exclusive create did not produce a private regular file")
        try require((fcntl(descriptor, F_GETFD) & FD_CLOEXEC) == FD_CLOEXEC,
                    "created descriptor can leak into a child process")
        let bytes = Data((0..<NativeFileWriteIO.maxWriteBytes).map { UInt8($0 % 251) })
        let first = NativeFileWriteIO.writeAll(descriptor: descriptor, data: bytes)
        let tail = Data([0, 255, 1, 254, 2, 253])
        let second = NativeFileWriteIO.writeAll(descriptor: descriptor, data: tail)
        let cancellation = NativeFileWriteIO.CancellationSignal()
        cancellation.requestCancel()
        let canceled = NativeFileWriteIO.writeAll(
            descriptor: descriptor, data: Data([17]), cancellation: cancellation)
        try require(first.bytesWritten == bytes.count && first.errorNumber == nil
                    && !first.canceled && second.bytesWritten == tail.count
                    && second.errorNumber == nil && !second.canceled,
                    "descriptor did not accept exact bounded chunks")
        try require(canceled.canceled && canceled.bytesWritten == 0 && canceled.errorNumber == nil,
                    "canceled descriptor write changed the settled prefix")
        try require(NativeFileWriteIO.close(descriptor: descriptor), "descriptor close failed")
        open = false
        let retained = try Data(contentsOf: target)
        try require(retained == bytes + tail, "close lost or deleted written bytes")
        try require(existsFailure(at: target), "existing file was not rejected with EEXIST")
        let unchanged = try Data(contentsOf: target)
        try require(unchanged == retained, "existing file was truncated or replaced")

        let link = directory.appendingPathComponent("existing-link")
        try FileManager.default.createSymbolicLink(at: link, withDestinationURL: target)
        try require(existsFailure(at: link), "existing symlink was followed")
        let missing = directory.appendingPathComponent("missing-target")
        let dangling = directory.appendingPathComponent("dangling-link")
        try FileManager.default.createSymbolicLink(at: dangling, withDestinationURL: missing)
        try require(existsFailure(at: dangling)
                    && !FileManager.default.fileExists(atPath: missing.path),
                    "dangling symlink created its target")
        try require(existsFailure(at: directory), "existing directory was accepted")
    }

    private static func existsFailure(at url: URL) -> Bool {
        switch NativeFileWriteIO.createNew(at: url) {
        case .failed(let error): return error == EEXIST
        case .opened(let descriptor):
            _ = NativeFileWriteIO.close(descriptor: descriptor)
            return false
        }
    }

    private static func boundedCanonicalBody() throws {
        let maximum = Data(repeating: 0xa5, count: NativeFileWriteIO.maxWriteBytes)
        try require(NativeFileWriteIO.boundedBody(maximum.base64EncodedString()) == maximum,
                    "maximum bounded body was rejected")
        try require(NativeFileWriteIO.boundedBody("Zg==") == Data([0x66]),
                    "canonical one-byte body was rejected")
        let invalid: [Any] = [
            "", "Zg=", "Zh==", "Zg==\n", " Zg==", "_w==", "%%%%", NSNull(),
            ["bodyBase64": "Zg=="], ["Zg=="], 17, true,
            Data(repeating: 0, count: NativeFileWriteIO.maxWriteBytes + 1).base64EncodedString(),
            String(repeating: "A", count: NativeFileWriteIO.maxEncodedWriteBytes + 4)
        ]
        for value in invalid {
            try require(NativeFileWriteIO.boundedBody(value) == nil,
                        "malformed, empty, noncanonical or oversized body was accepted")
        }
        try require(NativeFileWriteIO.boundedBody(nil) == nil, "missing body was accepted")
    }

    private static func sequentialRange() throws {
        try require(NativeFileWriteIO.validRange(offset: 0, nextOffset: 0, byteCount: 65_536)
                    && NativeFileWriteIO.validRange(offset: 65_536, nextOffset: 65_536, byteCount: 6)
                    && NativeFileWriteIO.validRange(offset: Int64.max - 1,
                                                   nextOffset: Int64.max - 1, byteCount: 1),
                    "valid sequential range was rejected")
        let invalidRanges: [(Int64, Int64, Int)] = [
            (-1, 0, 1), (0, 1, 1), (2, 1, 1), (0, 0, 0), (0, 0, -1),
            (0, 0, 65_537), (Int64.max, Int64.max, 1),
            (Int64.max - 1, Int64.max - 1, 2)
        ]
        for range in invalidRanges {
            try require(!NativeFileWriteIO.validRange(
                offset: range.0, nextOffset: range.1, byteCount: range.2),
                "invalid, replayed, skipped or overflowing range was accepted")
        }
    }

    private static func interruptedAndPartialWrites() throws {
        let body = Data([10, 20, 30, 40, 50])
        var attempts: [(Int, Int32)] = [(-1, EINTR), (2, 0), (-1, EINTR), (3, 0)]
        var accepted = Data()
        let complete = NativeFileWriteIO.writeAll(data: body) { pointer, remaining in
            let attempt = attempts.removeFirst()
            if attempt.0 > 0 {
                precondition(attempt.0 <= remaining)
                accepted.append(pointer.assumingMemoryBound(to: UInt8.self), count: attempt.0)
            }
            return attempt
        }
        try require(complete.bytesWritten == body.count && complete.errorNumber == nil
                    && attempts.isEmpty && accepted == body,
                    "EINTR/partial retry changed byte order, lost data, or repeated bytes")
        var calls = 0
        let partial = NativeFileWriteIO.writeAll(data: body) { _, remaining in
            calls += 1
            return calls == 1 ? (2, 0) : (-1, ENOSPC)
        }
        try require(partial.bytesWritten == 2 && partial.errorNumber == ENOSPC && calls == 2,
                    "partial failure did not preserve actual bytes written")
        let stopped = NativeFileWriteIO.writeAll(data: body) { _, _ in (0, 0) }
        try require(stopped.bytesWritten == 0 && stopped.errorNumber == EIO,
                    "zero-byte write was treated as success or retried forever")
        let impossible = NativeFileWriteIO.writeAll(data: body) { _, count in (count + 1, 0) }
        try require(impossible.bytesWritten == 0 && impossible.errorNumber == EIO,
                    "invalid syscall count was accepted")
        for data in [Data(), Data(repeating: 0, count: NativeFileWriteIO.maxWriteBytes + 1)] {
            let invalid = NativeFileWriteIO.writeAll(data: data) { _, _ in
                preconditionFailure("invalid write reached syscall")
            }
            try require(invalid.bytesWritten == 0 && invalid.errorNumber == EINVAL,
                        "empty or oversized write bypassed primitive bound")
        }
    }

    private static func cooperativeCancellation() throws {
        let body = Data([10, 20, 30, 40, 50])
        let beforeFirst = NativeFileWriteIO.CancellationSignal()
        beforeFirst.requestCancel()
        let preCanceled = NativeFileWriteIO.writeAll(
            data: body, shouldCancel: { beforeFirst.isRequested }) { _, _ in
                preconditionFailure("pre-canceled write reached syscall")
            }
        try require(preCanceled.canceled && preCanceled.bytesWritten == 0
                    && preCanceled.errorNumber == nil,
                    "pre-first cancellation fabricated progress or an I/O error")

        let partialSignal = NativeFileWriteIO.CancellationSignal()
        var accepted = Data()
        var partialCalls = 0
        let partial = NativeFileWriteIO.writeAll(
            data: body, shouldCancel: { partialSignal.isRequested }) { pointer, _ in
                partialCalls += 1
                accepted.append(pointer.assumingMemoryBound(to: UInt8.self), count: 2)
                partialSignal.requestCancel()
                return (2, 0)
            }
        try require(partial.canceled && partial.bytesWritten == 2
                    && partial.errorNumber == nil && partialCalls == 1
                    && accepted == body.prefix(2),
                    "cancellation after a partial syscall lost the exact prefix or issued another syscall")

        let interruptSignal = NativeFileWriteIO.CancellationSignal()
        var interruptCalls = 0
        let interrupted = NativeFileWriteIO.writeAll(
            data: body, shouldCancel: { interruptSignal.isRequested }) { _, _ in
                interruptCalls += 1
                interruptSignal.requestCancel()
                return (-1, EINTR)
            }
        try require(interrupted.canceled && interrupted.bytesWritten == 0
                    && interrupted.errorNumber == nil && interruptCalls == 1,
                    "EINTR retry bypassed its cancellation checkpoint")

        let completedSignal = NativeFileWriteIO.CancellationSignal()
        let completed = NativeFileWriteIO.writeAll(
            data: body, shouldCancel: { completedSignal.isRequested }) { _, remaining in
                completedSignal.requestCancel()
                return (remaining, 0)
            }
        try require(!completed.canceled && completed.bytesWritten == body.count
                    && completed.errorNumber == nil,
                    "cancellation rewrote a fully completed syscall as canceled")

        let errorSignal = NativeFileWriteIO.CancellationSignal()
        var errorCalls = 0
        let failed = NativeFileWriteIO.writeAll(
            data: body, shouldCancel: { errorSignal.isRequested }) { _, _ in
                errorCalls += 1
                if errorCalls == 1 { return (2, 0) }
                errorSignal.requestCancel()
                return (-1, ENOSPC)
            }
        try require(!failed.canceled && failed.bytesWritten == 2
                    && failed.errorNumber == ENOSPC && errorCalls == 2,
                    "a simultaneous cancellation concealed a real I/O failure")
    }

    private static func crossThreadCancellation() throws {
        let signal = NativeFileWriteIO.CancellationSignal()
        let syscallEntered = DispatchSemaphore(value: 0)
        let syscallMayReturn = DispatchSemaphore(value: 0)
        let completed = DispatchSemaphore(value: 0)
        let outcome = WriteOutcome()
        DispatchQueue(label: "native-write-cancel-smoke").async {
            var calls = 0
            let result = NativeFileWriteIO.writeAll(
                data: Data([1, 2, 3]), shouldCancel: { signal.isRequested }) { _, _ in
                    calls += 1
                    syscallEntered.signal()
                    guard syscallMayReturn.wait(timeout: .now() + 5) == .success else {
                        return (-1, ETIMEDOUT)
                    }
                    return (1, 0)
                }
            outcome.store(result, calls: calls)
            completed.signal()
        }
        try require(syscallEntered.wait(timeout: .now() + 5) == .success,
                    "worker did not enter the deterministic syscall seam")
        signal.requestCancel()
        signal.requestCancel() // Requests are idempotent and never clear the signal.
        try require(completed.wait(timeout: .now()) == .timedOut,
                    "cancellation incorrectly interrupted the already-entered syscall")
        syscallMayReturn.signal()
        try require(completed.wait(timeout: .now() + 5) == .success,
                    "worker did not observe the cross-thread cancellation")
        let value = outcome.load()
        try require(value.result?.canceled == true && value.result?.bytesWritten == 1
                    && value.result?.errorNumber == nil && value.calls == 1,
                    "cross-thread cancellation did not preserve its admitted syscall prefix")
    }

    private static func cancellationCloseBarrier() throws {
        var barrier = NativeFileWriteIO.CancellationCloseBarrier()
        try require(barrier.takeIfWriteSettled(writeInFlight: false) == nil,
                    "unrequested cancellation scheduled a close")
        barrier.request(operationToken: 23, cleanupToken: 41)
        // Even after the worker has returned, this remains true until its
        // AppKit callback publishes the prefix. A cancel/invalidation drain
        // keeps the original cleanup token pending across that boundary.
        try require(barrier.takeIfWriteSettled(writeInFlight: true) == nil
                    && barrier.takeIfWriteSettled(writeInFlight: true) == nil,
                    "cancel close ran before the write's main-thread settlement")
        let publishedPrefix: Int64 = 65_538
        var closeObservedPrefix: Int64?
        if let pending = barrier.takeIfWriteSettled(writeInFlight: false) {
            try require(pending.operationToken == 23 && pending.cleanupToken == 41,
                        "waiting cancellation lost its operation or cleanup identity")
            closeObservedPrefix = publishedPrefix
        }
        try require(closeObservedPrefix == 65_538
                    && barrier.takeIfWriteSettled(writeInFlight: false) == nil,
                    "cancel close did not consume exactly one settled prefix")
        var settled = NativeFileWriteIO.CancellationCloseBarrier()
        settled.request(operationToken: 24, cleanupToken: 42)
        try require(settled.takeIfWriteSettled(writeInFlight: false)?.cleanupToken == 42,
                    "settled cancellation unnecessarily waited for another write callback")
    }

    private final class WriteOutcome: @unchecked Sendable {
        private let lock = NSLock()
        private var result: NativeFileWriteIO.WriteResult?
        private var calls = 0
        func store(_ result: NativeFileWriteIO.WriteResult, calls: Int) {
            lock.lock()
            self.result = result
            self.calls = calls
            lock.unlock()
        }
        func load() -> (result: NativeFileWriteIO.WriteResult?, calls: Int) {
            lock.lock()
            defer { lock.unlock() }
            return (result, calls)
        }
    }

    private static func require(_ condition: @autoclosure () -> Bool, _ message: String) throws {
        if !condition() { throw Failure(message) }
    }

    private struct Failure: Error, CustomStringConvertible {
        let description: String
        init(_ description: String) { self.description = description }
    }
}
