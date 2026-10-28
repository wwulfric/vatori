import AppKit

/// Defers lifecycle work on the main thread without depending on main-queue
/// reentrancy. AppKit's terminate-later loop uses the modal-panel mode and can
/// be entered while a main dispatch callback is still on the stack.
enum NativeMainRunLoop {
    static func perform(_ work: @escaping () -> Void) {
        let runLoop = CFRunLoopGetMain()
        let modes = [RunLoop.Mode.default.rawValue,
                     RunLoop.Mode.modalPanel.rawValue] as CFArray
        CFRunLoopPerformBlock(runLoop, modes, work)
        CFRunLoopWakeUp(runLoop)
    }

    static func perform(
        after delay: TimeInterval,
        _ work: @escaping () -> Void
    ) {
        precondition(delay.isFinite && delay >= 0)
        // The deadline must remain live even when AppKit is waiting inside a
        // main dispatch callback. Mutable lifecycle state is touched only by
        // the subsequently scheduled main-run-loop block.
        DispatchQueue.global(qos: .utility).asyncAfter(deadline: .now() + delay) {
            perform(work)
        }
    }
}
