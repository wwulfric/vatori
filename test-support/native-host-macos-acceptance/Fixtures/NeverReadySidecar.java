import java.io.IOException;

public final class NeverReadySidecar {
    private NeverReadySidecar() {
    }

    public static void main(String[] args) throws IOException {
        while (System.in.read() != -1) {
            // Stay alive until the host closes the sidecar channel.
        }
    }
}
