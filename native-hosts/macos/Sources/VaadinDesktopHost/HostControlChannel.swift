import Foundation

private let hostControlWirePrefix = "VATORI_HOST_CONTROL "
private let hostControlResultWirePrefix = "VATORI_HOST_CONTROL_RESULT "
private let hostControlProtocolVersion = 1

enum HostControlCommand {
    case replaceSidecar(requestId: String, reason: String)
    case rollbackSidecar(requestId: String, reason: String)
}

enum HostControlReplacementStatus: String {
    case ready
    case failed
    case rolledBack
    case rollbackFailed
}

final class HostControlChannel {
    private let input: FileHandle
    private let output: FileHandle
    private let onCommand: (HostControlCommand) -> Void
    private var lineReader: LineReader?

    init(
        input: FileHandle = .standardInput,
        output: FileHandle = .standardOutput,
        onCommand: @escaping (HostControlCommand) -> Void
    ) {
        self.input = input
        self.output = output
        self.onCommand = onCommand
    }

    func start() {
        guard lineReader == nil else {
            return
        }
        let reader = LineReader { [weak self] line in
            self?.handle(line)
        }
        lineReader = reader
        input.readabilityHandler = { [weak self] handle in
            let data = handle.availableData
            if data.isEmpty {
                self?.stop()
                return
            }
            reader.append(data)
        }
    }

    func stop() {
        input.readabilityHandler = nil
        lineReader = nil
    }

    private func handle(_ line: String) {
        guard line.hasPrefix(hostControlWirePrefix) else {
            return
        }
        let raw = String(line.dropFirst(hostControlWirePrefix.count))
        guard let data = raw.data(using: .utf8),
              let object = try? JSONSerialization.jsonObject(with: data)
                as? [String: Any],
              validVersion(object["version"]),
              let type = object["type"] as? String,
              let requestId = validRequestId(object["requestId"]),
              let reason = validReason(object["reason"]) else {
            trace("recv", "hostControl.rejected", [
                "reason": "invalid-command"
            ])
            return
        }
        let command: HostControlCommand
        switch type {
        case "replaceSidecar":
            command = .replaceSidecar(
                requestId: requestId,
                reason: reason)
        case "rollbackSidecar":
            command = .rollbackSidecar(
                requestId: requestId,
                reason: reason)
        default:
            trace("recv", "hostControl.rejected", [
                "reason": "unsupported-command",
                "type": type
            ])
            return
        }
        DispatchQueue.main.async { [weak self] in
            self?.onCommand(command)
        }
    }

    func sendReplacementResult(
        requestId: String,
        status: HostControlReplacementStatus,
        cause: String
    ) {
        guard let requestId = validRequestId(requestId),
              let cause = validToken(cause, maximumLength: 128) else {
            trace("send", "hostControl.resultRejected", [
                "reason": "invalid-result"
            ])
            return
        }
        let line = "\(hostControlResultWirePrefix)"
            + "\(hostControlProtocolVersion) \(requestId) "
            + "\(status.rawValue) \(cause)\n"
        guard let data = line.data(using: .utf8) else {
            return
        }
        do {
            try output.write(contentsOf: data)
        } catch {
            trace("send", "hostControl.resultFailed", [
                "requestId": requestId,
                "status": status.rawValue,
                "cause": "output-write-failed"
            ])
        }
    }

    private func validVersion(_ value: Any?) -> Bool {
        guard let number = sidecarJSONNumber(value) else {
            return false
        }
        return number.intValue == hostControlProtocolVersion
            && number.doubleValue == Double(hostControlProtocolVersion)
    }

    private func validReason(_ value: Any?) -> String? {
        guard let value = value as? String,
              !value.isEmpty,
              value.count <= 128,
              value.unicodeScalars.allSatisfy({
                  $0.value >= 0x20 && $0.value != 0x7F
              }) else {
            return nil
        }
        return value
    }

    private func validRequestId(_ value: Any?) -> String? {
        guard let value = value as? String else {
            return nil
        }
        return validToken(value, maximumLength: 64)
    }

    private func validToken(
        _ value: String,
        maximumLength: Int
    ) -> String? {
        guard !value.isEmpty,
              value.count <= maximumLength,
              value.unicodeScalars.allSatisfy({
                  ($0.value >= 0x30 && $0.value <= 0x39)
                      || ($0.value >= 0x41 && $0.value <= 0x5A)
                      || ($0.value >= 0x61 && $0.value <= 0x7A)
                      || $0.value == 0x2D
                      || $0.value == 0x5F
              }) else {
            return nil
        }
        return value
    }
}
