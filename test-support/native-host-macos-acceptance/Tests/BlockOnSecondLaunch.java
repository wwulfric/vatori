import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

/** Private stable-JAR fixture: run real Flow once, hang before rollback ready. */
public final class BlockOnSecondLaunch {
    public static void main(String[] args) throws Exception {
        String marker = System.getenv("VATORI_ACCEPTANCE_ROLLBACK_MARKER");
        if (marker == null || !Path.of(marker).isAbsolute()) {
            throw new IllegalStateException("An explicit private acceptance marker is required");
        }
        try {
            Files.createFile(Path.of(marker));
        } catch (FileAlreadyExistsException secondLaunch) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Test rollback shutdown hook blocked");
                System.out.flush();
                waitForever();
            }, "acceptance-rollback-shutdown"));
            System.out.println("Test rollback deliberately never ready");
            waitForever();
            return;
        }
        Class.forName("dev.vatori.ipcframework.FlowOverIpcFrameworkApplication")
                .getMethod("main", String[].class).invoke(null, (Object) args);
    }

    private static void waitForever() {
        var never = new CountDownLatch(1);
        while (true) {
            try { never.await(); } catch (InterruptedException ignored) { }
        }
    }
}
