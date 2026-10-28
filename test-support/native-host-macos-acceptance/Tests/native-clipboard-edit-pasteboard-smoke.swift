import Foundation

private typealias Item = NativeClipboardEditPasteboardItem
private typealias Representation = NativeClipboardEditPasteboardRepresentation

private final class FakePasteboard: NativeClipboardEditPasteboardProvider {
    var changeCount = 10
    var items: [Item]
    var fixtureText = false
    var fixtureLength = 1
    var captureFails = false
    var prepareFailures = 0
    var partialWrites = 0
    var foreignWrite = false
    var reorderedTypes = false
    var clearFails = false
    var clearCalls = 0
    var writeCalls = 0
    var prepareCalls = 0
    var snapshotCalls = 0
    var textCalls = 0
    var onSnapshot: ((FakePasteboard) -> Void)?
    var onText: ((FakePasteboard) -> Void)?
    var onPrepare: ((FakePasteboard) -> Void)?
    var onClear: ((FakePasteboard) -> Void)?
    private var prepared: [Item] = []

    init(_ items: [Item] = originalItems()) { self.items = items }
    func snapshot() -> [Item]? {
        snapshotCalls += 1; onSnapshot?(self)
        return captureFails ? nil : items
    }
    func fixtureTextObservation() -> NativeClipboardEditPasteboardTextObservation {
        textCalls += 1; onText?(self)
        return .init(matches: fixtureText, length: fixtureText ? fixtureLength : 0)
    }
    func prepare(_ items: [Item]) -> Bool {
        prepareCalls += 1; onPrepare?(self)
        if prepareFailures > 0 { prepareFailures -= 1; return false }
        prepared = items; return true
    }
    func clearContents() -> Int {
        clearCalls += 1
        if clearFails { return changeCount }
        changeCount += 1; items = []; fixtureText = false
        let owned = changeCount; onClear?(self)
        return owned
    }
    func writePrepared() -> Bool {
        writeCalls += 1; changeCount += 1
        if foreignWrite { items = [item("private.foreign", [9])]; return false }
        if partialWrites > 0 {
            partialWrites -= 1
            items = prepared.first.map { [Item(representations: Array($0.representations.prefix(1)))] } ?? []
            return false
        }
        items = reorderedTypes ? prepared.map { Item(representations: Array($0.representations.reversed())) } : prepared
        return true
    }
    func nativeCut() { changeCount += 3; fixtureText = true; items = [item("public.utf8-plain-text", [120])] }
    func foreignChange() { changeCount += 1; fixtureText = false; items = [item("private.foreign", [9])] }
}

private func item(_ type: String, _ bytes: [UInt8]) -> Item {
    .init(representations: [.init(type: type, data: Data(bytes))])
}
private func originalItems() -> [Item] {
    [.init(representations: [
        .init(type: "private.test.text", data: Data([0, 255, 10, 42])),
        .init(type: "private.test.alternate", data: Data([19, 18, 17]))]),
     item("private.test.second", [6, 5, 4, 3])]
}
private func check(_ condition: @autoclosure () -> Bool, _ message: String) {
    if !condition() {
        FileHandle.standardError.write(Data(("FAIL: " + message + "\n").utf8))
        exit(1)
    }
}
private func boolean(_ payload: [String: Any], _ key: String) -> Bool { payload[key] as? Bool == true }
private func number(_ payload: [String: Any], _ key: String) -> Int { payload[key] as? Int ?? -999 }
private func adopted(_ provider: FakePasteboard) -> NativeClipboardEditPasteboardGuard {
    let guarder = NativeClipboardEditPasteboardGuard(provider: provider)
    check(guarder.capture(), "capture")
    check(guarder.armCut(), "arm")
    provider.nativeCut()
    check(guarder.adoptCut(causalEvidence: true), "causal adoption")
    check(guarder.checkOwned(), "owned after adoption")
    return guarder
}

@main
private struct NativeClipboardEditPasteboardSmoke {
    static func main() {
        // The fake provider is the only provider used here. No application,
        // private system pasteboard or general pasteboard is accessed.
        do {
            let p = FakePasteboard(); let g = NativeClipboardEditPasteboardGuard(provider: p)
            check(g.capture() && g.capture(), "capture is idempotent")
            check(!g.adoptCut(causalEvidence: true), "cannot adopt before arm")
            check(p.clearCalls == 0 && p.writeCalls == 0, "capture never writes")
            let r = g.restore(reason: "failure").payload
            check(boolean(r, "restoreSkipped") && !boolean(r, "ownershipUncertain"), "unarmed cleanup needs no mutation")
            check(p.clearCalls == 0 && p.writeCalls == 0, "unarmed cleanup never writes")
        }
        do {
            let p = FakePasteboard(); let g = NativeClipboardEditPasteboardGuard(provider: p)
            check(g.capture() && g.armCut(), "capture and arm")
            check(!g.adoptCut(causalEvidence: true) && !g.contended, "unchanged count is retryable pending cut")
            p.nativeCut()
            check(!g.adoptCut(causalEvidence: false), "matching cut text without causal proof is insufficient")
            let r = g.restore(reason: "failure").payload
            check(boolean(r, "ownershipUncertain") && boolean(r, "restoreSkipped"), "pending cut cleanup remains uncertain")
            check(p.clearCalls == 0 && p.writeCalls == 0, "pending cut never overwrites clipboard")
            check(!g.adoptCut(causalEvidence: true), "late evidence cannot revive uncertain cleanup")
        }
        do {
            let p = FakePasteboard(); let g = adopted(p); let cut = p.changeCount
            check(boolean(g.payload, "cutTextMatches") && number(g.payload, "cutTextLength") == 1, "actual cut comparison recorded")
            let r = g.restore(reason: "success").payload
            check(boolean(r, "snapshotRestored") && boolean(r, "snapshotVerified"), "full multi-item restoration")
            check(number(r, "ownedChangeCount") == cut && number(r, "restoreExpectedChangeCount") == cut
                && number(r, "restoreBeforeChangeCount") == cut, "historical cut epoch retained")
            check(number(r, "restoredChangeCount") == p.changeCount && p.changeCount > cut, "restoration advances epoch")
            check(number(r, "restoreAttempts") == 1 && p.clearCalls == 1 && p.writeCalls == 1, "one successful restore")
            _ = g.restore(reason: "termination")
            check(p.clearCalls == 1 && p.writeCalls == 1, "termination after success is idempotent")
            check(r.values.allSatisfy { $0 is Bool || $0 is Int }, "metadata contains only booleans and counts")
        }
        do {
            let p = FakePasteboard([]); let g = adopted(p)
            check(g.restore(reason: "success").verified && p.items.isEmpty, "empty original clipboard restored")
        }
        do {
            let p = FakePasteboard(); p.reorderedTypes = true; let g = adopted(p)
            check(g.restore(reason: "success").verified, "type ordering does not change representation identity")
        }
        do {
            let p = FakePasteboard(); p.captureFails = true
            let g = NativeClipboardEditPasteboardGuard(provider: p)
            check(!g.capture() && !g.armCut(), "unmaterializable snapshot fails before arm")
            _ = g.restore(reason: "failure")
            check(p.clearCalls == 0 && p.writeCalls == 0, "materialization failure cannot mutate")
        }
        do {
            let p = FakePasteboard(); p.onSnapshot = { $0.foreignChange() }
            let g = NativeClipboardEditPasteboardGuard(provider: p)
            check(!g.capture() && g.contended, "count change during snapshot rejected")
            check(p.clearCalls == 0, "unstable capture has no clear")
        }
        do {
            let p = FakePasteboard(); let g = NativeClipboardEditPasteboardGuard(provider: p)
            check(g.capture(), "capture before contention")
            p.foreignChange()
            check(!g.armCut() && g.contended, "contention before arming")
            _ = g.restore(reason: "failure")
            check(p.clearCalls == 0, "pre-arm contention not overwritten")
        }
        do {
            let p = FakePasteboard(); let g = NativeClipboardEditPasteboardGuard(provider: p)
            check(g.capture() && g.armCut(), "arm before foreign write")
            p.foreignChange()
            check(!g.adoptCut(causalEvidence: true) && g.contended, "foreign content cannot be adopted")
            _ = g.restore(reason: "failure")
            check(p.clearCalls == 0, "foreign adoption failure never clears")
        }
        do {
            let p = FakePasteboard(); let g = NativeClipboardEditPasteboardGuard(provider: p)
            check(g.capture() && g.armCut(), "arm before read race"); p.nativeCut()
            p.onText = { $0.changeCount += 1 }
            check(!g.adoptCut(causalEvidence: true) && g.contended, "change during native text comparison")
            check(p.clearCalls == 0, "raced comparison no writes")
        }
        do {
            let p = FakePasteboard(); let g = adopted(p); p.foreignChange()
            check(!g.checkOwned() && g.contended, "pre-paste foreign epoch detected")
            let r = g.restore(reason: "failure").payload
            check(boolean(r, "restoreSkipped") && p.clearCalls == 0, "post-adopt contention skipped")
        }
        do {
            let p = FakePasteboard(); let g = adopted(p); p.fixtureText = false
            check(!g.checkOwned() && g.contended, "same-count text mismatch fails ownership check")
            _ = g.restore(reason: "failure"); check(p.clearCalls == 0, "text mismatch never restored over")
        }
        do {
            let p = FakePasteboard(); let g = adopted(p); p.onPrepare = { $0.foreignChange() }
            let r = g.restore(reason: "success").payload
            check(boolean(r, "pasteboardContended") && p.clearCalls == 0, "external change before clear")
        }
        do {
            let p = FakePasteboard(); let g = adopted(p); p.onClear = { $0.foreignChange() }
            let r = g.restore(reason: "success").payload
            check(boolean(r, "pasteboardContended") && p.writeCalls == 0, "change after clear prevents write")
        }
        do {
            let p = FakePasteboard(); let g = adopted(p); p.prepareFailures = 9
            let r = g.restore(reason: "success").payload
            check(number(r, "restoreAttempts") == 6 && !boolean(r, "snapshotRestored"), "six preparation attempts bounded")
            check(p.clearCalls == 0 && p.writeCalls == 0, "prepare failures before clear")
            _ = g.restore(reason: "termination")
            check(p.prepareCalls == 6, "six attempts are lifetime not per call")
        }
        do {
            let p = FakePasteboard(); let g = adopted(p); p.prepareFailures = 1; p.partialWrites = 1
            let r = g.restore(reason: "success").payload
            check(boolean(r, "snapshotVerified") && number(r, "restoreAttempts") == 3, "prepare and recognizable partial write retry")
            check(p.clearCalls == 2 && p.writeCalls == 2, "only prepared attempts mutate")
        }
        do {
            let p = FakePasteboard(); let g = adopted(p); p.partialWrites = 20
            let r = g.restore(reason: "success").payload
            check(!boolean(r, "snapshotRestored") && number(r, "restoreAttempts") == 6 && p.writeCalls == 6, "partial writes bounded at six")
        }
        do {
            let p = FakePasteboard(); let g = adopted(p); p.foreignWrite = true
            let r = g.restore(reason: "failure").payload
            check(boolean(r, "ownershipUncertain") && boolean(r, "restoreSkipped") && p.writeCalls == 1, "unknown partial content is not retried")
            _ = g.restore(reason: "termination"); check(p.writeCalls == 1, "unknown content never overwritten by cleanup retry")
        }
        do {
            let p = FakePasteboard(); let g = adopted(p); p.onSnapshot = { $0.changeCount += 1 }
            let r = g.restore(reason: "success").payload
            check(boolean(r, "pasteboardContended") && !boolean(r, "snapshotVerified"), "verification count race rejected")
        }
        do {
            let p = FakePasteboard(); let g = adopted(p); p.captureFails = true
            let r = g.restore(reason: "success").payload
            check(boolean(r, "ownershipUncertain") && boolean(r, "restoreSkipped")
                && !boolean(r, "snapshotRestored") && !boolean(r, "snapshotVerified"), "verification retrieval failure is not an empty snapshot")
            check(p.clearCalls == 1 && p.writeCalls == 1, "unverifiable restoration is not retried")
            _ = g.restore(reason: "termination")
            check(p.clearCalls == 1 && p.writeCalls == 1, "termination cannot overwrite after unverifiable restoration")
        }
        do {
            let p = FakePasteboard(); let g = adopted(p); p.clearFails = true
            let r = g.restore(reason: "success").payload
            check(boolean(r, "ownershipUncertain") && p.writeCalls == 0, "failed clear does not authorize write")
        }
        do {
            let duplicate = Representation(type: "private.duplicate", data: Data([1]))
            let p = FakePasteboard([Item(representations: [duplicate, duplicate])])
            let g = NativeClipboardEditPasteboardGuard(provider: p)
            check(!g.capture() && p.clearCalls == 0, "ambiguous duplicate representations rejected")
            let many = FakePasteboard(Array(repeating: item("private.item", []), count: 65))
            check(!NativeClipboardEditPasteboardGuard(provider: many).capture(), "item count bounded")
            let large = FakePasteboard([Item(representations: [.init(type: "private.large", data: Data(count: 16 * 1024 * 1024 + 1))])])
            check(!NativeClipboardEditPasteboardGuard(provider: large).capture(), "materialized byte retention bounded")
        }
        check(!String(describing: originalItems()).contains("private.test"), "description hides representation names")
        check(!String(reflecting: originalItems()).contains("private.test"), "debug output hides representation names")
        // SIGKILL does not execute cleanup. There is intentionally no test or
        // claim that private process-memory snapshots survive a killed process.
        print("Native clipboard edit pasteboard fake-provider smoke PASS")
    }
}
