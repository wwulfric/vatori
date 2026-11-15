import java.util.concurrent.CountDownLatch;

/** Faulty test candidate: no ready envelope and no graceful TERM completion. */
public final class HangBeforeReady {
    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Test candidate shutdown hook blocked");
            System.out.flush();
            waitForever();
        }, "acceptance-candidate-shutdown"));
        System.out.println("Test candidate deliberately never ready");
        waitForever();
    }

    private static void waitForever() {
        var never = new CountDownLatch(1);
        while (true) {
            try { never.await(); } catch (InterruptedException ignored) { }
        }
    }
}
