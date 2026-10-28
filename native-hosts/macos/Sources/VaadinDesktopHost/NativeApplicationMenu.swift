import AppKit

/// Standard application commands resolve against AppKit's current responder
/// chain, including whichever native view or WebView owns editing focus.
enum NativeApplicationMenu {
    static func install(on application: NSApplication) {
        let applicationName = (Bundle.main.object(
            forInfoDictionaryKey: "CFBundleDisplayName") as? String)
            ?? (Bundle.main.object(forInfoDictionaryKey: "CFBundleName") as? String)
            ?? ProcessInfo.processInfo.processName
        application.mainMenu = makeMenu(applicationName: applicationName)
    }

    static func makeMenu(applicationName: String) -> NSMenu {
        precondition(Thread.isMainThread)
        let menu = NSMenu(title: "")
        let applicationMenu = NSMenu(title: applicationName)
        applicationMenu.addItem(command(
            "Quit \(applicationName)",
            action: #selector(NSApplication.terminate(_:)), key: "q"))
        addSubmenu(applicationMenu, to: menu)

        let editMenu = NSMenu(title: "Edit")
        editMenu.addItem(command(
            "Undo", action: NSSelectorFromString("undo:"), key: "z"))
        editMenu.addItem(command(
            "Redo", action: NSSelectorFromString("redo:"), key: "z",
            modifiers: [.command, .shift]))
        editMenu.addItem(.separator())
        editMenu.addItem(command(
            "Cut", action: #selector(NSText.cut(_:)), key: "x"))
        editMenu.addItem(command(
            "Copy", action: #selector(NSText.copy(_:)), key: "c"))
        editMenu.addItem(command(
            "Paste", action: #selector(NSText.paste(_:)), key: "v"))
        editMenu.addItem(.separator())
        editMenu.addItem(command(
            "Select All", action: #selector(NSText.selectAll(_:)), key: "a"))
        addSubmenu(editMenu, to: menu)
        return menu
    }

    private static func addSubmenu(_ submenu: NSMenu, to menu: NSMenu) {
        let item = NSMenuItem(title: submenu.title, action: nil, keyEquivalent: "")
        item.submenu = submenu
        menu.addItem(item)
    }

    private static func command(
        _ title: String, action: Selector, key: String,
        modifiers: NSEvent.ModifierFlags = .command
    ) -> NSMenuItem {
        let item = NSMenuItem(title: title, action: action, keyEquivalent: key)
        // A fixed target would steal commands from another window or from an
        // AppKit text field in a sheet. AppKit also validates nil-target actions.
        item.target = nil
        item.keyEquivalentModifierMask = modifiers
        return item
    }
}
