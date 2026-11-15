/** Standalone faulty candidate; never included in framework/application artifacts. */
public final class FailBeforeReady {
    public static void main(String[] args) {
        System.out.println("Test candidate exits before ready");
        System.exit(42);
    }
}
