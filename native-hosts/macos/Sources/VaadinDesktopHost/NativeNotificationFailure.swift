import Foundation
import UserNotifications

/// An independent settings read, never NSError domain/code/message, determines
/// whether authorization is denied. Other API errors retain structured platform
/// diagnostics: arbitrary domains and userInfo stay out of diagnostic fields,
/// while the original error message remains private to the application result.
enum NativeNotificationFailure {
    enum Stage {
        case authorization, scheduling

        var errorCode: String {
            switch self {
            case .authorization: return "notification-authorization-failed"
            case .scheduling: return "notification-scheduling-failed"
            }
        }
    }

    static func authorizationFailure(
        command: String,
        error: Error,
        observedStatus: UNAuthorizationStatus
    ) -> [String: Any] {
        // The caller must independently read current notification settings after
        // the authorization callback fails. Even UNErrorDomain/code 1 alone does
        // not distinguish an OS denial from an authorization API failure.
        if observedStatus == .denied {
            return [
                "success": false,
                "command": command,
                "mode": "native-user-notifications",
                "scheduled": false,
                "errorCode": "notification-authorization-denied",
                "error": "Notification permission was denied"
            ]
        }
        return payload(command: command, stage: .authorization, error: error)
    }

    static func payload(command: String, stage: Stage, error: Error) -> [String: Any] {
        let native = error as NSError
        return [
            "success": false,
            "command": command,
            "mode": "native-user-notifications",
            "scheduled": false,
            "errorCode": stage.errorCode,
            "nativeErrorDomain": native.domain == UNErrorDomain ? "UNErrorDomain" : "other",
            "nativeErrorCode": native.code,
            "error": native.localizedDescription
        ]
    }
}
