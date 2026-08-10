struct FlowIpcVerificationSchedule {
    let inspectionDelaysSeconds: [Double]
    let snapshotDelaySeconds: Double
    let autoShutdownTimeoutSeconds: Double

    init(config: HostConfig) {
        inspectionDelaysSeconds = config.flowIpcAutoFrameworkUpdate
            ? [1.5, 3.0, 6.0]
            : [1.0, 2.5]
        snapshotDelaySeconds = config.flowIpcAutoFrameworkUpdate ? 8.0 : 3.0

        if config.flowIpcAutoShutdownTimeoutSeconds > 0 {
            autoShutdownTimeoutSeconds = config.flowIpcAutoShutdownTimeoutSeconds
        } else if config.flowIpcAutoInFlightRequestRecovery
                || config.flowIpcAutoSidecarRestart
                || config.flowIpcAutoSidecarRecoveryExhaustion {
            autoShutdownTimeoutSeconds = 60.0
        } else if config.flowIpcAutoFrameworkUpdate {
            autoShutdownTimeoutSeconds = 45.0
        } else {
            autoShutdownTimeoutSeconds = 30.0
        }
    }
}
