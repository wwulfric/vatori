import AppKit
import Foundation

// Test-support data only. Never serialize these values into a trace or error.
struct NativeClipboardEditPasteboardRepresentation: CustomStringConvertible, CustomDebugStringConvertible {
    let type: String
    let data: Data
    var description: String { "Private pasteboard representation" }
    var debugDescription: String { description }
}

struct NativeClipboardEditPasteboardItem: CustomStringConvertible, CustomDebugStringConvertible {
    let representations: [NativeClipboardEditPasteboardRepresentation]
    var description: String { "Private pasteboard item" }
    var debugDescription: String { description }
}

// Methods that inspect or prepare must not mutate the pasteboard. Only clear
// and writePrepared may do so; the guard calls them after causal adoption.
protocol NativeClipboardEditPasteboardProvider: AnyObject {
    var changeCount: Int { get }
    func snapshot() -> [NativeClipboardEditPasteboardItem]?
    func fixtureTextObservation() -> NativeClipboardEditPasteboardTextObservation
    func prepare(_ items: [NativeClipboardEditPasteboardItem]) -> Bool
    func clearContents() -> Int
    func writePrepared() -> Bool
}

struct NativeClipboardEditPasteboardTextObservation {
    let matches: Bool
    let length: Int
}

private enum NativeClipboardEditPasteboardLimits {
    static let items = 64
    static let representationsPerItem = 64
    static let bytes = 16 * 1024 * 1024

    static func accepts(_ items: [NativeClipboardEditPasteboardItem]) -> Bool {
        guard items.count <= self.items else { return false }
        var total = 0
        for item in items {
            guard item.representations.count <= representationsPerItem else { return false }
            var types = Set<String>()
            for representation in item.representations {
                guard !representation.type.isEmpty, types.insert(representation.type).inserted,
                      representation.data.count <= bytes - total else { return false }
                total += representation.data.count
            }
        }
        return true
    }
}

private final class NativeClipboardEditSystemPasteboard: NativeClipboardEditPasteboardProvider {
    private let pasteboard: NSPasteboard
    private var prepared: [NSPasteboardItem]?

    init(_ pasteboard: NSPasteboard) { self.pasteboard = pasteboard }
    var changeCount: Int { pasteboard.changeCount }

    func snapshot() -> [NativeClipboardEditPasteboardItem]? {
        guard let source = pasteboard.pasteboardItems else { return nil }
        guard source.count <= NativeClipboardEditPasteboardLimits.items else { return nil }
        var result: [NativeClipboardEditPasteboardItem] = []
        var total = 0
        for item in source {
            let types = item.types
            guard types.count <= NativeClipboardEditPasteboardLimits.representationsPerItem else { return nil }
            var representations: [NativeClipboardEditPasteboardRepresentation] = []
            for type in types {
                guard let data = item.data(forType: type),
                      data.count <= NativeClipboardEditPasteboardLimits.bytes - total else { return nil }
                total += data.count
                representations.append(.init(type: type.rawValue, data: data))
            }
            result.append(.init(representations: representations))
        }
        return result
    }

    func fixtureTextObservation() -> NativeClipboardEditPasteboardTextObservation {
        let text = pasteboard.string(forType: .string)
        return .init(matches: text == "x", length: text?.utf16.count ?? 0)
    }

    func prepare(_ items: [NativeClipboardEditPasteboardItem]) -> Bool {
        prepared = nil
        var result: [NSPasteboardItem] = []
        for item in items {
            let target = NSPasteboardItem()
            for representation in item.representations {
                guard target.setData(representation.data, forType: .init(representation.type)) else { return false }
            }
            result.append(target)
        }
        prepared = result
        return true
    }

    func clearContents() -> Int { pasteboard.clearContents() }
    func writePrepared() -> Bool {
        guard let prepared else { return false }
        return prepared.isEmpty || pasteboard.writeObjects(prepared)
    }
}

struct NativeClipboardEditPasteboardReport {
    let payload: [String: Any]
    var restored: Bool { payload["snapshotRestored"] as? Bool == true }
    var verified: Bool { payload["snapshotVerified"] as? Bool == true }
}

// A bounded, best-effort preservation guard, not an atomic pasteboard lock.
// changeCount checks cannot eliminate a writer racing between an AppKit call
// and its next observation. Materialized bytes/types are preserved, not the
// original owner, lazy providers, file promises or process-crash recovery.
final class NativeClipboardEditPasteboardGuard {
    private let provider: any NativeClipboardEditPasteboardProvider
    private var original: [NativeClipboardEditPasteboardItem]?
    private var workingRestoreCount = -1
    private var verified = false
    private var cutTextMatches = false
    private var cutTextLength = 0
    private var skipped = false
    private var restoreAttempts = 0
    private var capturedCount = -1
    private var cutCount = -1
    private var restoredCount = -1
    private var restoreExpectedCount = -1
    private var restoreBeforeCount = -1
    private var restoreAfterCount = -1
    private(set) var captured = false
    private(set) var armed = false
    private(set) var adopted = false
    private(set) var restored = false
    private(set) var contended = false
    private(set) var ownershipUncertain = false

    convenience init(pasteboard: NSPasteboard = .general) {
        self.init(provider: NativeClipboardEditSystemPasteboard(pasteboard))
    }

    init(provider: any NativeClipboardEditPasteboardProvider) { self.provider = provider }

    var payload: [String: Any] {
        ["snapshotCaptured": captured, "cutArmed": armed, "cutAdopted": adopted,
         "snapshotRestored": restored, "snapshotVerified": verified,
         "cutTextMatches": cutTextMatches, "cutTextLength": cutTextLength,
         "pasteboardContended": contended, "ownershipUncertain": ownershipUncertain,
         "restoreSkipped": skipped, "restoreAttempts": restoreAttempts,
         "capturedChangeCount": capturedCount, "ownedChangeCount": cutCount,
         "currentChangeCount": provider.changeCount, "restoredChangeCount": restoredCount,
         "restoreExpectedChangeCount": restoreExpectedCount,
         "restoreBeforeChangeCount": restoreBeforeCount, "restoreAfterChangeCount": restoreAfterCount]
    }

    func capture() -> Bool {
        guard !captured else { return !contended && !ownershipUncertain }
        guard !contended && !ownershipUncertain else { return false }
        let before = provider.changeCount
        guard let items = provider.snapshot(), NativeClipboardEditPasteboardLimits.accepts(items) else { return false }
        guard provider.changeCount == before else { contended = true; return false }
        original = items; capturedCount = before; captured = true
        return true
    }

    func armCut() -> Bool {
        guard captured, !adopted, !restored, !contended, !ownershipUncertain else { return false }
        guard provider.changeCount == capturedCount else { contended = true; return false }
        armed = true
        return true
    }

    // The caller supplies evidence of the actual owned trusted cut AND
    // deleteByCut input. Matching clipboard text alone never authorizes adoption.
    func adoptCut(causalEvidence: Bool) -> Bool {
        guard causalEvidence, captured, armed, !restored, !contended, !ownershipUncertain else { return false }
        if adopted { return checkOwned() }
        let before = provider.changeCount
        if before == capturedCount { return false } // Native cut may still be pending.
        let text = provider.fixtureTextObservation()
        guard before > capturedCount, text.matches, text.length == 1, provider.changeCount == before else {
            contended = true; return false
        }
        cutTextMatches = text.matches; cutTextLength = text.length
        cutCount = before; workingRestoreCount = before; adopted = true
        return true
    }

    func checkOwned() -> Bool {
        guard adopted, !restored, !contended, !ownershipUncertain else { return false }
        guard provider.changeCount == cutCount else { contended = true; return false }
        let text = provider.fixtureTextObservation()
        guard text.matches, text.length == 1, provider.changeCount == cutCount else {
            contended = true; return false
        }
        return true
    }

    // reason is accepted for the driver's call-site readability, but deliberately
    // excluded from metadata so arbitrary diagnostic strings cannot expose data.
    func restore(reason: String) -> NativeClipboardEditPasteboardReport {
        _ = reason
        guard !restored else { return report() }
        guard captured, armed else { skipped = true; return report() }
        guard adopted else { ownershipUncertain = true; skipped = true; return report() }
        guard !contended, !ownershipUncertain, let original else { skipped = true; return report() }
        if restoreExpectedCount < 0 {
            restoreExpectedCount = cutCount; restoreBeforeCount = provider.changeCount
            guard checkOwned() else { skipped = true; return report() }
        }
        while restoreAttempts < 6 {
            guard provider.changeCount == workingRestoreCount else { contended = true; skipped = true; return report() }
            restoreAttempts += 1
            // Construct all native items before clearing any original data.
            let prepared = provider.prepare(original)
            guard provider.changeCount == workingRestoreCount else { contended = true; skipped = true; return report() }
            guard prepared else { continue }
            let clearCount = provider.clearContents()
            guard clearCount > workingRestoreCount else { ownershipUncertain = true; skipped = true; return report() }
            workingRestoreCount = clearCount; restoreAfterCount = clearCount
            guard provider.changeCount == clearCount else { contended = true; skipped = true; return report() }
            let written = provider.writePrepared()
            let afterWrite = provider.changeCount
            workingRestoreCount = afterWrite; restoreAfterCount = afterWrite
            guard let actual = provider.snapshot(), NativeClipboardEditPasteboardLimits.accepts(actual) else {
                ownershipUncertain = true; skipped = true; return report()
            }
            guard provider.changeCount == afterWrite else { contended = true; skipped = true; return report() }
            if written && Self.matches(actual, original) {
                restored = true; verified = true; restoredCount = afterWrite
                return report()
            }
            // Retry only a recognizable partial result of our own write. An
            // unrelated representation is not ours to overwrite, even if the
            // external writer raced inside the synchronous write call.
            guard Self.isPartial(actual, of: original) else {
                ownershipUncertain = true; skipped = true; return report()
            }
        }
        return report()
    }

    private func report() -> NativeClipboardEditPasteboardReport { .init(payload: payload) }

    private static func matches(_ actual: [NativeClipboardEditPasteboardItem], _ expected: [NativeClipboardEditPasteboardItem]) -> Bool {
        actual.count == expected.count && isPartial(actual, of: expected)
            && zip(actual, expected).allSatisfy { pair in pair.0.representations.count == pair.1.representations.count }
    }

    private static func isPartial(_ actual: [NativeClipboardEditPasteboardItem], of expected: [NativeClipboardEditPasteboardItem]) -> Bool {
        guard actual.count <= expected.count else { return false }
        for (actualItem, expectedItem) in zip(actual, expected) {
            for representation in actualItem.representations {
                guard let original = expectedItem.representations.first(where: { $0.type == representation.type }),
                      representation.data == original.data else { return false }
            }
        }
        return true
    }
}
