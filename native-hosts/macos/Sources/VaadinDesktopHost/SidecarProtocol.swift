import Foundation
import CoreFoundation

let sidecarWirePrefix = "VFIPC "
let sidecarProtocolVersion = 1

func hasValidSidecarVersion(_ object: [String: Any]) -> Bool {
    guard let number = sidecarJSONNumber(object["version"]) else {
        return false
    }
    return number.intValue == sidecarProtocolVersion
        && number.doubleValue == Double(sidecarProtocolVersion)
}

func sidecarJSONNumber(_ value: Any?) -> NSNumber? {
    guard let number = value as? NSNumber,
          CFGetTypeID(number) != CFBooleanGetTypeID() else {
        return nil
    }
    return number
}

func isValidSidecarIdentifier(_ value: String) -> Bool {
    !value.isEmpty
        && value.count <= 128
        && value.range(
            of: "^[A-Za-z0-9._:-]+$",
            options: .regularExpression) != nil
}
