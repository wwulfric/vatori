import Foundation

struct SidecarWindowDescriptor {
    let id: String
    let title: String
    let width: Double
    let height: Double
    let route: String

    init(id: String, title: String, width: Double, height: Double, route: String = "") {
        self.id = id
        self.title = title
        self.width = width
        self.height = height
        self.route = route
    }

    static func parse(_ object: [String: Any]) throws -> SidecarWindowDescriptor {
        guard let id = object["id"] as? String,
              isValidSidecarIdentifier(id),
              let title = nonBlank(object["title"]),
              let width = positiveNumber(object["width"]),
              let height = positiveNumber(object["height"]),
              let route = relativeRoute(object["route"]) else {
            throw parseError("Sidecar window descriptor is invalid")
        }
        return SidecarWindowDescriptor(
            id: id, title: title, width: width, height: height, route: route)
    }

    private static func nonBlank(_ value: Any?) -> String? {
        guard let text = value as? String,
              !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            return nil
        }
        return text
    }

    private static func positiveNumber(_ value: Any?) -> Double? {
        guard let number = sidecarJSONNumber(value),
              number.doubleValue > 0 else {
            return nil
        }
        return number.doubleValue
    }

    private static func relativeRoute(_ value: Any?) -> String? {
        guard let route = value as? String else {
            return nil
        }
        if route.isEmpty {
            return route
        }
        guard !route.hasPrefix("/"), !route.hasPrefix("~"),
              !route.contains("\\"), !route.contains("://") else {
            return nil
        }
        let segments = route.split(separator: "/", omittingEmptySubsequences: false)
        guard segments.allSatisfy({ !$0.isEmpty && $0 != "." && $0 != ".." }) else {
            return nil
        }
        return route
    }

    private static func parseError(_ message: String) -> NSError {
        NSError(
            domain: "dev.vatori.desktop.sidecar-window",
            code: 1,
            userInfo: [NSLocalizedDescriptionKey: message])
    }
}

struct SidecarApplicationDescriptor {
    let name: String
    let metadata: [String: String]
    let mainWindow: SidecarWindowDescriptor
}

struct SidecarReady {
    let transport: HostTransport
    let application: SidecarApplicationDescriptor
    let applicationURL: URL?

    static func parse(_ envelope: [String: Any], expectedTransport: HostTransport) throws -> SidecarReady {
        guard envelope["type"] as? String == "ready",
              let payload = envelope["payload"] as? [String: Any] else {
            throw parseError("Sidecar ready payload is missing")
        }
        guard let transportValue = payload["transport"] as? String,
              let transport = HostTransport(rawValue: transportValue),
              transport == expectedTransport else {
            throw parseError("Sidecar ready transport does not match the native host")
        }
        guard let application = payload["application"] as? [String: Any],
              let name = nonBlank(application["name"]),
              let metadata = application["metadata"] as? [String: String],
              let mainWindow = application["mainWindow"] as? [String: Any] else {
            throw parseError("Sidecar ready application descriptor is invalid")
        }
        let mainWindowDescriptor: SidecarWindowDescriptor
        do {
            mainWindowDescriptor = try SidecarWindowDescriptor.parse(mainWindow)
        } catch {
            throw parseError("Sidecar ready application descriptor is invalid")
        }
        let applicationURL: URL?
        if transport == .compatibilityHttp {
            guard let urlText = nonBlank(payload["applicationUrl"]),
                  let url = URL(string: urlText),
                  url.scheme == "http",
                  url.host == "127.0.0.1" else {
                throw parseError("Compatibility sidecar ready requires a loopback applicationUrl")
            }
            applicationURL = url
        } else {
            applicationURL = nil
        }
        return SidecarReady(
            transport: transport,
            application: SidecarApplicationDescriptor(
                name: name,
                metadata: metadata,
                mainWindow: mainWindowDescriptor),
            applicationURL: applicationURL)
    }

    private static func nonBlank(_ value: Any?) -> String? {
        guard let text = value as? String,
              !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            return nil
        }
        return text
    }

    private static func parseError(_ message: String) -> NSError {
        NSError(
            domain: "dev.vatori.desktop.sidecar-ready",
            code: 1,
            userInfo: [NSLocalizedDescriptionKey: message])
    }
}
