import java.util.concurrent.CountDownLatch;

/** Test-only launcher installed in the private initial JAR, never the candidate. */
public final class BlockOnShutdown {
    public static void main(String[] args) throws Exception {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Test initial JVM shutdown hook blocked");
            System.out.flush();
            var never = new CountDownLatch(1);
            while (true) {
                try { never.await(); } catch (InterruptedException ignored) { }
            }
        }, "acceptance-blocked-shutdown"));
        Class.forName("dev.vatori.ipcframework.FlowOverIpcFrameworkApplication")
                .getMethod("main", String[].class).invoke(null, (Object) args);
    }
}
