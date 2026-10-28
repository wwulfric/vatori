package dev.vatori.ipcframework;

/** No host/Flow evidence: exercises the test-app fault trigger in an owned JVM. */
public final class FatalProtocolStartupProbe {
    public static void main(String[] args) throws Exception {
        FlowFatalProtocolStartup.initialize();
        System.out.println("PROBE_READY");
        System.out.flush();
        Thread.sleep(1500);
    }
}
