import Foundation
import UserNotifications

@main
struct NativeNotificationFailureSmoke {
    static func main() throws {
        for stage in [NativeNotificationFailure.Stage.authorization, .scheduling] {
            for domain in [UNErrorDomain, "private-domain:/user/document", "[redacted]"] {
                for code in [0, 1, -1, Int.min, Int.max] {
                    let payload = NativeNotificationFailure.payload(
                        command: "notification.show", stage: stage,
                        error: NSError(domain: domain, code: code, userInfo: [
                            NSLocalizedDescriptionKey: "private-error-message",
                            "private-key": "private-value",
                            NSUnderlyingErrorKey: NSError(domain: "private-underlying", code: 2)
                        ]))
                    try require(Set(payload.keys) == [
                        "success", "command", "mode", "scheduled", "errorCode",
                        "nativeErrorDomain", "nativeErrorCode", "error"
                    ], "unexpected native failure payload fields")
                    try require(payload["success"] as? Bool == false
                                && payload["scheduled"] as? Bool == false
                                && payload["command"] as? String == "notification.show"
                                && payload["mode"] as? String == "native-user-notifications"
                                && payload["errorCode"] as? String == stage.errorCode,
                                "native error was reclassified or lost its operation")
                    try require(payload["nativeErrorDomain"] as? String
                                    == (domain == UNErrorDomain ? "UNErrorDomain" : "other")
                                && payload["nativeErrorCode"] as? Int == code,
                                "unsafe domain or altered numeric code")
                    try require(payload["error"] as? String == "private-error-message",
                                "original application-facing error message changed")
                    let encoded = try JSONSerialization.data(withJSONObject: payload)
                    let decoded = try JSONSerialization.jsonObject(with: encoded) as? [String: Any]
                    try require(decoded?["nativeErrorCode"] as? Int == code,
                                "diagnostic code did not round-trip as an integer")
                }
            }
        }
        try authorizationFailureRequiresIndependentDeniedStatus()
        print("Native notification failure smoke passed (observed-state classification and structured diagnostics only; no OS permission evidence)")
    }

    private static func authorizationFailureRequiresIndependentDeniedStatus() throws {
        let states: [UNAuthorizationStatus] = [
            .notDetermined, .denied, .authorized, .provisional,
            // The SDK reserves raw value 4 for App Clip ephemeral authorization,
            // but marks the named case unavailable on macOS. Treat it as a
            // non-denied foreign/future status without relying on that symbol.
            UNAuthorizationStatus(rawValue: 4)!,
            UNAuthorizationStatus(rawValue: -1)!,
            UNAuthorizationStatus(rawValue: Int.max)!
        ]
        for state in states {
            for domain in [UNErrorDomain, "private-domain:/user/document", "[redacted]"] {
                for code in [0, 1, -1, Int.min, Int.max] {
                    for message in [
                        "private-error-message",
                        "Notifications are not allowed for this application",
                        "Notification permission was denied",
                        "notification-authorization-denied"
                    ] {
                        let native = NSError(domain: domain, code: code, userInfo: [
                            NSLocalizedDescriptionKey: message,
                            "private-key": "private-value",
                            NSUnderlyingErrorKey: NSError(domain: "private-underlying", code: 2)
                        ])
                        let result = NativeNotificationFailure.authorizationFailure(
                            command: "notification.show", error: native,
                            observedStatus: state)
                        try require(result["success"] as? Bool == false
                                    && result["scheduled"] as? Bool == false
                                    && result["command"] as? String == "notification.show"
                                    && result["mode"] as? String == "native-user-notifications",
                                    "authorization failure scheduled or lost its operation")
                        if state == .denied {
                            try require(Set(result.keys) == [
                                "success", "command", "mode", "scheduled", "errorCode", "error"
                            ], "observed denial leaked diagnostic fields or an identifier")
                            try require(result["errorCode"] as? String
                                            == "notification-authorization-denied"
                                        && result["error"] as? String
                                            == "Notification permission was denied",
                                        "independently observed denial was not normalized")
                        } else {
                            let expected = NativeNotificationFailure.payload(
                                command: "notification.show", stage: .authorization,
                                error: native)
                            try require(NSDictionary(dictionary: result).isEqual(to: expected),
                                        "non-denied or unknown OS state inferred denial from NSError")
                            try require(result["nativeErrorDomain"] as? String
                                            == (domain == UNErrorDomain ? "UNErrorDomain" : "other")
                                        && result["nativeErrorCode"] as? Int == code
                                        && result["error"] as? String == message,
                                        "non-denied authorization diagnostics changed")
                        }
                    }
                }
            }
        }
    }

    private struct Failure: Error { let message: String }
    private static func require(_ condition: @autoclosure () -> Bool, _ message: String) throws {
        if !condition() { throw Failure(message: message) }
    }
}
