import AppKit

@main
struct NativeApplicationMenuSmoke {
    static func main() {
        do {
            try run()
            print("Native application menu smoke passed")
        } catch {
            fputs("Native application menu smoke failed: \(error)\n", stderr)
            exit(1)
        }
    }

    private static func run() throws {
        // Menu structure only: no window, activation, editing action, clipboard
        // access or termination is needed. Real WKWebView input is a separate gate.
        let application = NSApplication.shared
        application.setActivationPolicy(.prohibited)
        let previousMenu = application.mainMenu
        defer { application.mainMenu = previousMenu }

        let menu = NativeApplicationMenu.makeMenu(applicationName: "Example App")
        try require(menu.items.count == 2 && menu.autoenablesItems,
                    "main menu must contain the application and Edit menus")
        guard let appMenu = menu.items[0].submenu,
              let editMenu = menu.items[1].submenu else {
            throw Failure(message: "missing native submenus")
        }
        try require(menu.items[0].title == "Example App"
                        && appMenu.title == "Example App"
                        && appMenu.items.count == 1 && appMenu.autoenablesItems,
                    "application menu identity or automatic validation changed")
        try command(appMenu.items[0], title: "Quit Example App",
                    action: "terminate:", key: "q")
        try require(menu.items[1].title == "Edit" && editMenu.title == "Edit"
                        && editMenu.items.count == 8 && editMenu.autoenablesItems,
                    "Edit menu structure or automatic validation changed")
        try command(editMenu.items[0], title: "Undo", action: "undo:", key: "z")
        try command(editMenu.items[1], title: "Redo", action: "redo:", key: "z",
                    modifiers: [.command, .shift])
        try require(editMenu.items[2].isSeparatorItem
                        && editMenu.items[6].isSeparatorItem,
                    "editing groups must remain separated")
        try command(editMenu.items[3], title: "Cut", action: "cut:", key: "x")
        try command(editMenu.items[4], title: "Copy", action: "copy:", key: "c")
        try command(editMenu.items[5], title: "Paste", action: "paste:", key: "v")
        try command(editMenu.items[7], title: "Select All", action: "selectAll:", key: "a")

        let another = NativeApplicationMenu.makeMenu(applicationName: "Another App")
        try require(another !== menu && another.items[1].submenu !== editMenu
                        && another.items[0].submenu?.items[0].title == "Quit Another App"
                        && appMenu.items[0].title == "Quit Example App",
                    "menu creation shared mutable state or cached the app identity")
        NativeApplicationMenu.install(on: application)
        try require(application.mainMenu?.items.count == 2
                        && application.mainMenu?.items[1].submenu?.items[7].action
                            == NSSelectorFromString("selectAll:")
                        && application.mainMenu?.items[0].submenu?.items[0].action
                            == #selector(NSApplication.terminate(_:)),
                    "installation omitted standard editing or normal termination")
    }

    private static func command(
        _ item: NSMenuItem, title: String, action: String, key: String,
        modifiers: NSEvent.ModifierFlags = .command
    ) throws {
        try require(item.title == title && item.action == NSSelectorFromString(action)
                        && item.keyEquivalent == key
                        && item.keyEquivalentModifierMask == modifiers
                        && item.target == nil && item.submenu == nil,
                    "\(title) lost its standard nil-target responder-chain command")
    }

    private static func require(_ condition: Bool, _ message: String) throws {
        if !condition { throw Failure(message: message) }
    }

    private struct Failure: Error, CustomStringConvertible {
        let message: String
        var description: String { message }
    }
}
