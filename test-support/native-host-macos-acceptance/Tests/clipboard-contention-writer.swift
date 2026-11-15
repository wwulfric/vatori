import AppKit
import Foundation

// A separate test process, never part of the host product. No private content
// is read or printed. The parent retains the original snapshot only in memory.
guard ProcessInfo.processInfo.environment["VATORI_TEST_CLIPBOARD_AUTHORIZED"] == "1",
      CommandLine.arguments.count == 2,
      let expected = Int(CommandLine.arguments[1]) else { exit(2) }
DispatchQueue.global().asyncAfter(deadline: .now() + 5) { exit(124) }
let pasteboard = NSPasteboard.general
guard pasteboard.changeCount == expected else { exit(3) }
let cleared = pasteboard.clearContents()
guard cleared > expected, pasteboard.changeCount == cleared else { exit(4) }
guard pasteboard.setString("vatori-n08-contender", forType: .string) else { exit(5) }
let written = pasteboard.changeCount
guard written >= cleared,
      pasteboard.string(forType: .string) == "vatori-n08-contender",
      pasteboard.changeCount == written else { exit(6) }
let receipt: [String: Any] = [
    "processIdentifier": ProcessInfo.processInfo.processIdentifier,
    "beforeChangeCount": expected, "clearedChangeCount": cleared,
    "writtenChangeCount": written, "fixtureMatches": true
]
let data = try JSONSerialization.data(withJSONObject: receipt, options: [.sortedKeys])
FileHandle.standardOutput.write(data)
