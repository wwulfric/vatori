import Darwin
import Foundation

private struct SmokeFailure: Error, CustomStringConvertible {
    let description: String
}

private final class Recorder {
    struct Pressure {
        let bytes: Int
        let frames: Int
        let partial: Int
    }
    private let lock = NSLock()
    private var writtenValues: [Int] = []
    private var pressureValues: [Pressure] = []
    private var failureValues: [NativeSidecarInputWriter.Failure] = []
    func wrote(_ value: Int) { lock.lock(); writtenValues.append(value); lock.unlock() }
    func pressured(_ bytes: Int, _ frames: Int, _ partial: Int) {
        lock.lock(); pressureValues.append(Pressure(bytes: bytes, frames: frames, partial: partial)); lock.unlock()
    }
    func failed(_ value: NativeSidecarInputWriter.Failure) { lock.lock(); failureValues.append(value); lock.unlock() }
    var written: [Int] { lock.lock(); defer { lock.unlock() }; return writtenValues }
    var pressures: [Pressure] { lock.lock(); defer { lock.unlock() }; return pressureValues }
    var failures: [NativeSidecarInputWriter.Failure] { lock.lock(); defer { lock.unlock() }; return failureValues }
}

private final class Harness {
    let pipe: Pipe
    let recorder = Recorder()
    let writer: NativeSidecarInputWriter
    let descriptor: Int32
    let prefilled: Data
    var received = Data()
    var eof = false
    private var readClosed = false

    init(bytes: Int = 4 * 1024 * 1024, frames: Int = 256,
         idle: TimeInterval = 2, fill: Bool = false) throws {
        pipe = Pipe()
        descriptor = pipe.fileHandleForWriting.fileDescriptor
        let readDescriptor = pipe.fileHandleForReading.fileDescriptor
        let readFlags = Darwin.fcntl(readDescriptor, F_GETFL)
        try require(readFlags != -1 && Darwin.fcntl(readDescriptor, F_SETFL, readFlags | O_NONBLOCK) == 0,
                    "read end could not become nonblocking")
        prefilled = fill ? try Self.fillPipe(descriptor) : Data()
        let recorder = self.recorder
        writer = try NativeSidecarInputWriter(
            handle: pipe.fileHandleForWriting, maximumPendingBytes: bytes,
            maximumPendingFrames: frames, idleTimeout: idle,
            onWritten: { recorder.wrote($0) },
            onBackpressure: { recorder.pressured($0, $1, $2) },
            onFailure: { recorder.failed($0) })
    }

    deinit { writer.cancel(); try? pipe.fileHandleForReading.close() }

    func closeReader() throws {
        try pipe.fileHandleForReading.close()
        readClosed = true
    }

    func readAvailable() throws {
        guard !eof && !readClosed else { return }
        var buffer = [UInt8](repeating: 0, count: 64 * 1024)
        for _ in 0..<256 {
            let count = Darwin.read(pipe.fileHandleForReading.fileDescriptor, &buffer, buffer.count)
            if count > 0 { received.append(contentsOf: buffer[0..<count]); continue }
            if count == 0 { eof = true; return }
            if errno == EINTR { continue }
            if errno == EAGAIN || errno == EWOULDBLOCK { return }
            throw SmokeFailure(description: "read failed: \(errno)")
        }
    }

    func readUntil(seconds: TimeInterval = 2, _ condition: () -> Bool) throws {
        let deadline = ProcessInfo.processInfo.systemUptime + seconds
        repeat {
            try readAvailable()
            if condition() { return }
            Thread.sleep(forTimeInterval: 0.002)
        } while ProcessInfo.processInfo.systemUptime < deadline
        throw SmokeFailure(description: "timed out while reading owned pipe")
    }

    func readEOF() throws { try readUntil { eof } }

    private static func fillPipe(_ descriptor: Int32) throws -> Data {
        let flags = Darwin.fcntl(descriptor, F_GETFL)
        try require(flags != -1 && Darwin.fcntl(descriptor, F_SETFL, flags | O_NONBLOCK) == 0
                    && Darwin.fcntl(descriptor, F_SETNOSIGPIPE, 1) == 0, "prefill flags failed")
        let bytes = [UInt8](repeating: 0x5A, count: 4096)
        var filled = Data()
        for _ in 0..<4096 {
            let count = Darwin.write(descriptor, bytes, bytes.count)
            if count > 0 { filled.append(contentsOf: bytes[0..<count]); continue }
            if count < 0 && errno == EINTR { continue }
            if count < 0 && (errno == EAGAIN || errno == EWOULDBLOCK) { return filled }
            throw SmokeFailure(description: "owned pipe prefill failed: \(errno)")
        }
        throw SmokeFailure(description: "owned pipe did not reach backpressure")
    }
}

private func require(_ value: @autoclosure () -> Bool, _ message: String) throws {
    if !value() { throw SmokeFailure(description: message) }
}

private func until(seconds: TimeInterval = 2, _ condition: () -> Bool) -> Bool {
    let deadline = ProcessInfo.processInfo.systemUptime + seconds
    repeat {
        if condition() { return true }
        Thread.sleep(forTimeInterval: 0.002)
    } while ProcessInfo.processInfo.systemUptime < deadline
    return condition()
}

@main
struct NativeSidecarInputWriterSmoke {
    static func main() {
        do {
            try orderedPartialWritesRecover()
            try backpressureBurstsResetOnlyAfterDrain()
            try overflowFailsClosed(frameLimit: false)
            try overflowFailsClosed(frameLimit: true)
            try closedPipeDoesNotRaiseSIGPIPE()
            try stalledDeadlineDoesNotExtendOnAdmission()
            try idleCancellationClosesSuspendedSource()
            try retireWritesControlOnlyBetweenFrames()
            try retireDropsPartialRemainder()
            try cancelDropsPartialRemainder()
            try canceledWriterCannotTouchReusedDescriptor()
            print("Native sidecar input writer smoke passed: 11 real-pipe cases (no JVM, GUI or component semantics)")
        } catch {
            FileHandle.standardError.write(Data("FAIL: \(error)\n".utf8))
            exit(1)
        }
    }

    private static func orderedPartialWritesRecover() throws {
        let h = try Harness()
        let a = Data("first complete frame\n".utf8)
        let b = Data(repeating: 0x42, count: 1024 * 1024) + Data("\n".utf8)
        let c = Data("third complete frame\n".utf8)
        try require(h.writer.enqueue(a) && h.writer.enqueue(b) && h.writer.enqueue(c), "normal admission rejected")
        try require(until { !h.recorder.pressures.isEmpty }, "real pipe never reached EAGAIN")
        try require(h.recorder.written == [a.count], "partial frame was reported as fully written")
        let pressure = h.recorder.pressures[0]
        try require(pressure.partial > 0 && pressure.partial < b.count
                    && pressure.frames == 2 && pressure.bytes == b.count + c.count,
                    "partial backpressure lost retained whole-frame accounting")
        try h.readUntil { h.recorder.written.count == 3 && h.received.count == a.count + b.count + c.count }
        h.writer.cancel(); try h.readEOF()
        try require(h.received == a + b + c && h.recorder.written == [a.count, b.count, c.count],
                    "FIFO writes lost, duplicated or interleaved bytes")
        try require(h.recorder.pressures.count == 1 && h.recorder.failures.isEmpty, "one blocked burst emitted duplicate diagnostics")
    }

    private static func backpressureBurstsResetOnlyAfterDrain() throws {
        let h = try Harness()
        let frame = Data(repeating: 0x43, count: 512 * 1024)
        for burst in 1...2 {
            try require(h.writer.enqueue(frame), "new burst admission rejected")
            try require(until { h.recorder.pressures.count == burst }, "new fully-drained burst was not observed")
            try h.readUntil { h.recorder.written.count == burst && h.received.count == frame.count * burst }
            // The complete callback precedes the queue's next empty observation.
            Thread.sleep(forTimeInterval: 0.03)
        }
        h.writer.cancel(); try h.readEOF()
        try require(h.recorder.failures.isEmpty && h.recorder.pressures.count == 2, "burst accounting did not reset after empty drain")
    }

    private static func overflowFailsClosed(frameLimit: Bool) throws {
        let h = try Harness(bytes: frameLimit ? 4096 : 512, frames: frameLimit ? 1 : 10, fill: true)
        try require(h.writer.enqueue(Data(repeating: 0x41, count: 400)), "first bounded frame rejected")
        try require(until { h.recorder.pressures.count == 1 }, "prefilled pipe was not blocked")
        let start = ProcessInfo.processInfo.systemUptime
        try require(!h.writer.enqueue(Data(repeating: 0x42, count: frameLimit ? 1 : 200)), "overflow was accepted")
        try require(ProcessInfo.processInfo.systemUptime - start < 0.1, "admission blocked behind pipe I/O")
        try require(until { h.recorder.failures.count == 1 }, "overflow failure did not settle")
        try require(!h.writer.enqueue(Data([0x43])), "failed channel continued accepting frames")
        h.writer.cancel(); h.writer.retire(shutdownFrame: Data("shutdown\n".utf8))
        try h.readEOF()
        try require(h.received == h.prefilled && h.recorder.written.isEmpty
                    && h.recorder.failures.count == 1 && h.recorder.failures[0].cause == "queue-capacity-exceeded",
                    "overflow wrote discarded data, repeated failure or hid the capacity cause")
    }

    private static func closedPipeDoesNotRaiseSIGPIPE() throws {
        let h = try Harness()
        try h.closeReader()
        try require(h.writer.enqueue(Data("ordinary frame\n".utf8)), "closed pipe was not first admitted")
        try require(until { h.recorder.failures.count == 1 }, "closed pipe did not fail")
        try require(h.recorder.failures[0].cause == "pipe-closed"
                    && h.recorder.failures[0].signalErrno == EPIPE && h.recorder.written.isEmpty,
                    "ordinary EPIPE was not an observable failure")
        try require(!h.writer.enqueue(Data([1])), "EPIPE left channel usable")
    }

    private static func stalledDeadlineDoesNotExtendOnAdmission() throws {
        let h = try Harness(idle: 0.15, fill: true)
        let start = ProcessInfo.processInfo.systemUptime
        try require(h.writer.enqueue(Data(repeating: 0x44, count: 400)), "deadline setup failed")
        try require(until { !h.recorder.pressures.isEmpty }, "deadline setup was not blocked")
        while h.recorder.failures.isEmpty && ProcessInfo.processInfo.systemUptime - start < 0.8 {
            _ = h.writer.enqueue(Data([0x45]))
            Thread.sleep(forTimeInterval: 0.01)
        }
        try require(h.recorder.failures.count == 1 && h.recorder.failures[0].cause == "write-timeout"
                    && ProcessInfo.processInfo.systemUptime - start < 0.6, "new admission extended the no-progress deadline")
        try h.readEOF()
        try require(h.recorder.pressures.count == 1 && h.recorder.written.isEmpty && h.received == h.prefilled,
                    "stalled channel silently completed or repeated backpressure")
    }

    private static func idleCancellationClosesSuspendedSource() throws {
        let h = try Harness()
        h.writer.cancel(); h.writer.cancel()
        try h.readEOF()
        try require(!h.writer.enqueue(Data([1])) && h.recorder.failures.isEmpty && h.recorder.written.isEmpty,
                    "idle cancel did not close/fence the suspended source cleanly")
    }

    private static func retireWritesControlOnlyBetweenFrames() throws {
        let h = try Harness()
        let shutdown = Data("VFIPC shutdown\n".utf8)
        h.writer.retire(shutdownFrame: shutdown)
        try require(!h.writer.enqueue(Data([1])), "retirement did not fence admission immediately")
        try h.readEOF()
        try require(h.received == shutdown && h.recorder.written.isEmpty && h.recorder.failures.isEmpty,
                    "between-frame retirement did not send only its best-effort control frame")

        let full = try Harness(fill: true)
        try require(full.writer.enqueue(Data(repeating: 0x41, count: 400)), "full-pipe retirement setup failed")
        try require(until { !full.recorder.pressures.isEmpty }, "full pipe never blocked")
        full.writer.retire(shutdownFrame: shutdown)
        try full.readEOF()
        try require(full.received == full.prefilled && full.recorder.failures.isEmpty && full.recorder.written.isEmpty,
                    "retirement waited for space, sent ordinary queued data or failed on best-effort backpressure")
    }

    private static func retireDropsPartialRemainder() throws {
        let h = try Harness()
        let frame = Data(repeating: 0x46, count: 1024 * 1024)
        try require(h.writer.enqueue(frame) && h.writer.enqueue(Data("must-not-follow\n".utf8)), "partial setup failed")
        try require(until { !h.recorder.pressures.isEmpty }, "partial setup never blocked")
        let prefixBytes = h.recorder.pressures[0].partial
        try require(prefixBytes > 0, "retirement test did not write a real partial frame")
        h.writer.retire(shutdownFrame: Data("must-not-insert-shutdown\n".utf8))
        try h.readEOF()
        try require(h.received == frame.prefix(prefixBytes) && h.recorder.written.isEmpty && h.recorder.failures.isEmpty,
                    "retirement inserted shutdown into an incomplete frame or leaked its remainder")
    }

    private static func cancelDropsPartialRemainder() throws {
        let h = try Harness()
        let frame = Data(repeating: 0x47, count: 1024 * 1024)
        try require(h.writer.enqueue(frame), "cancel setup failed")
        try require(until { !h.recorder.pressures.isEmpty }, "cancel setup never blocked")
        let prefixBytes = h.recorder.pressures[0].partial
        let start = ProcessInfo.processInfo.systemUptime
        h.writer.cancel()
        try require(ProcessInfo.processInfo.systemUptime - start < 0.1, "cancel blocked behind pipe I/O")
        try h.readEOF()
        try require(prefixBytes > 0 && h.received == frame.prefix(prefixBytes)
                    && h.recorder.written.isEmpty && h.recorder.failures.isEmpty,
                    "cancel did not settle an incomplete frame without false written success")
    }

    private static func canceledWriterCannotTouchReusedDescriptor() throws {
        let old = try Harness()
        let reused = old.descriptor
        old.writer.cancel(); try old.readEOF()
        try require(Darwin.fcntl(reused, F_GETFD) == -1 && errno == EBADF, "old cancel handler has not closed its descriptor")
        var descriptors: [Int32] = [-1, -1]
        try require(Darwin.pipe(&descriptors) == 0, "neighbor pipe setup failed")
        // Relocate the test-owned read end if the allocator reused the old slot
        // for it, then make that old number the neighbor's writable end.
        if descriptors[0] == reused {
            let replacement = Darwin.fcntl(descriptors[0], F_DUPFD_CLOEXEC, reused + 100)
            try require(replacement >= 0, "neighbor read relocation failed")
            Darwin.close(descriptors[0]); descriptors[0] = replacement
        }
        if descriptors[1] != reused {
            try require(Darwin.fcntl(reused, F_GETFD) == -1 && errno == EBADF, "old slot unexpectedly belongs to another handle")
            try require(Darwin.dup2(descriptors[1], reused) == reused, "neighbor write relocation failed")
            Darwin.close(descriptors[1]); descriptors[1] = reused
        }
        defer { Darwin.close(descriptors[0]); Darwin.close(descriptors[1]) }
        let readFlags = Darwin.fcntl(descriptors[0], F_GETFL)
        try require(Darwin.fcntl(descriptors[0], F_SETFL, readFlags | O_NONBLOCK) == 0, "neighbor read flags failed")
        old.writer.cancel(); old.writer.retire(shutdownFrame: Data("stale-shutdown\n".utf8))
        try require(!old.writer.enqueue(Data("stale-work\n".utf8)), "old writer accepted work after descriptor reuse")
        Thread.sleep(forTimeInterval: 0.03)
        let sentinel = Data("neighbor-is-still-owned\n".utf8)
        let written = sentinel.withUnsafeBytes { Darwin.write(descriptors[1], $0.baseAddress!, $0.count) }
        try require(written == sentinel.count, "stale cancellation closed the neighbor descriptor")
        var bytes = [UInt8](repeating: 0, count: 256)
        let count = Darwin.read(descriptors[0], &bytes, bytes.count)
        try require(count == sentinel.count && Data(bytes.prefix(max(0, count))) == sentinel,
                    "stale writer touched the neighbor's byte stream")
    }
}
