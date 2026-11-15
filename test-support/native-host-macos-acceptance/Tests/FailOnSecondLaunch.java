import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Private acceptance-JAR launcher; never packaged into framework artifacts. */
public final class FailOnSecondLaunch {
    public static void main(String[] args) throws Exception {
        String marker = System.getenv("VATORI_ACCEPTANCE_ROLLBACK_MARKER");
        if (marker == null || !Path.of(marker).isAbsolute()) {
            throw new IllegalStateException("An explicit private acceptance marker is required");
        }
        try {
            Files.createFile(Path.of(marker));
        } catch (FileAlreadyExistsException secondLaunch) {
            System.out.println("Test stable runtime exits before rollback ready");
            System.exit(43);
            return;
        }
        Class.forName("dev.vatori.ipcframework.FlowOverIpcFrameworkApplication")
                .getMethod("main", String[].class).invoke(null, (Object) args);
    }
}
