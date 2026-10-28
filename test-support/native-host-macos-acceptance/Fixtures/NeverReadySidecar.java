import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/** Standalone test-support JVM: deliberately never emits the ready handshake. */
public final class NeverReadySidecar {
    private static final Set<PosixFilePermission> DIRECTORY_MODE = PosixFilePermissions.fromString("rwx------");
    private static final Set<PosixFilePermission> FILE_MODE = PosixFilePermissions.fromString("rw-------");
    private static final Set<String> RECORDS = Set.of("startup.json", "hook-entered.json");
    private static final int MAX_RECORD_BYTES = 256;

    private NeverReadySidecar() {
    }

    public static void main(String[] args) throws IOException {
        String configured = System.getenv("VAADIN_DESKTOP_TEST_READY_TIMEOUT_DIR");
        String mode = System.getenv("VAADIN_DESKTOP_TEST_READY_TIMEOUT_MODE");
        if (configured == null || !("cooperative".equals(mode) || "blocked".equals(mode))) {
            throw new IllegalStateException("Initial-ready timeout fixture configuration is invalid");
        }
        Path directory = Path.of(configured);
        BasicFileAttributes attributes = checkedDirectory(directory);
        try (var entries = Files.newDirectoryStream(directory)) {
            if (entries.iterator().hasNext()) throw invalidState();
        }
        State state = new State(directory, attributes.fileKey(), UUID.randomUUID().toString(),
                ProcessHandle.current().pid(), mode);
        publish(state, "startup.json");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                publish(state, "hook-entered.json");
            } catch (IOException | RuntimeException ignored) {
                // Missing evidence fails acceptance; never disclose state paths
                // or make the deliberately blocked hook become cooperative.
                System.err.println("Initial-ready timeout hook marker failed");
            }
            if ("blocked".equals(state.mode())) awaitForever();
        }, "initial-ready-timeout-shutdown"));
        while (System.in.read() != -1) {
            // Ignore all input, including shutdown. EOF returns main and starts
            // normal JVM shutdown; no System.exit or test keepalive is involved.
        }
    }

    private static void publish(State state, String name) throws IOException {
        BasicFileAttributes directory = checkedDirectory(state.directory());
        if (!state.directoryKey().equals(directory.fileKey())) throw invalidState();
        boolean startup = false;
        try (var entries = Files.newDirectoryStream(state.directory())) {
            for (Path entry : entries) {
                String existing = entry.getFileName().toString();
                if (!RECORDS.contains(existing) || name.equals(existing)) throw invalidState();
                checkRecord(entry, state);
                startup |= "startup.json".equals(existing);
            }
        }
        if (!"startup.json".equals(name) && !startup) throw invalidState();
        Path temporary = state.directory().resolve(".pending-" + UUID.randomUUID());
        Path target = state.directory().resolve(name);
        boolean created = false;
        try {
            try (FileChannel channel = FileChannel.open(temporary,
                    Set.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW, LinkOption.NOFOLLOW_LINKS),
                    PosixFilePermissions.asFileAttribute(FILE_MODE))) {
                created = true;
                ByteBuffer bytes = ByteBuffer.wrap(state.json().getBytes(StandardCharsets.US_ASCII));
                while (bytes.hasRemaining()) channel.write(bytes);
                channel.force(true);
            }
            Files.createLink(target, temporary); // Atomic no-replace publication.
        } finally {
            if (created) Files.deleteIfExists(temporary);
        }
        checkRecord(target, state);
    }

    private static void checkRecord(Path path, State state) throws IOException {
        BasicFileAttributes before = checkedFile(path);
        ByteBuffer bytes = ByteBuffer.allocate(MAX_RECORD_BYTES + 1);
        try (FileChannel channel = FileChannel.open(path, Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS))) {
            while (bytes.hasRemaining() && channel.read(bytes) >= 0) { }
        }
        BasicFileAttributes after = checkedFile(path);
        if (!before.fileKey().equals(after.fileKey()) || before.size() != after.size()
                || bytes.position() != before.size()
                || !state.json().equals(new String(bytes.array(), 0, bytes.position(), StandardCharsets.US_ASCII))) {
            throw invalidState();
        }
    }

    private static BasicFileAttributes checkedDirectory(Path path) throws IOException {
        if (!path.isAbsolute() || !path.equals(path.normalize()) || path.getFileName() == null
                || !path.getFileName().toString().matches("vatori-ready-timeout\\.[A-Za-z0-9]{6}")) throw invalidState();
        BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isDirectory() || attributes.isSymbolicLink() || attributes.fileKey() == null
                || !Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS).equals(DIRECTORY_MODE)
                || !Files.getOwner(path, LinkOption.NOFOLLOW_LINKS).equals(path.getFileSystem()
                        .getUserPrincipalLookupService().lookupPrincipalByName(System.getProperty("user.name")))
                || !path.toRealPath().equals(path)) throw invalidState();
        return attributes;
    }

    private static BasicFileAttributes checkedFile(Path path) throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        Object links = Files.getAttribute(path, "unix:nlink", LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isRegularFile() || attributes.isSymbolicLink() || attributes.fileKey() == null
                || attributes.size() <= 0 || attributes.size() > MAX_RECORD_BYTES
                || !(links instanceof Number count) || count.longValue() != 1
                || !Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS).equals(FILE_MODE)
                || !Files.getOwner(path, LinkOption.NOFOLLOW_LINKS)
                        .equals(Files.getOwner(path.getParent(), LinkOption.NOFOLLOW_LINKS))) throw invalidState();
        return attributes;
    }

    private static void awaitForever() {
        CountDownLatch never = new CountDownLatch(1);
        while (true) {
            try { never.await(); } catch (InterruptedException ignored) { }
        }
    }

    private static IOException invalidState() { return new IOException("Private initial-ready timeout state is invalid"); }

    private record State(Path directory, Object directoryKey, String runId, long processIdentifier, String mode) {
        String json() {
            return "{\"schemaVersion\":1,\"runId\":\"" + runId + "\",\"processIdentifier\":"
                    + processIdentifier + ",\"mode\":\"" + mode + "\"}\n";
        }
    }
}
