package dev.vatori.ipcframework;

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

/** Test-app-only hook: normal Flow startup, then cooperative or blocked JVM shutdown. */
final class FlowHostShutdownTerminationStartup {
    private static final String DIRECTORY_ENV = "VAADIN_DESKTOP_TEST_HOST_SHUTDOWN_DIR";
    private static final String MODE_ENV = "VAADIN_DESKTOP_TEST_HOST_SHUTDOWN_MODE";
    private static final String STARTUP = "startup.json";
    private static final String RETURNED = "launcher-returned.json";
    private static final String HOOK = "hook-entered.json";
    private static final Set<String> RECORDS = Set.of(STARTUP, RETURNED, HOOK);
    private static final Set<PosixFilePermission> DIRECTORY_MODE =
            PosixFilePermissions.fromString("rwx------");
    private static final Set<PosixFilePermission> FILE_MODE =
            PosixFilePermissions.fromString("rw-------");
    private static final int MAX_RECORD_BYTES = 256;
    private static final Object STATE_LOCK = new Object();
    private static volatile State current;

    private FlowHostShutdownTerminationStartup() { }

    static void initialize() {
        String directory = System.getenv(DIRECTORY_ENV);
        String mode = System.getenv(MODE_ENV);
        if (directory == null && mode == null) return;
        if (directory == null || !("cooperative".equals(mode) || "blocked".equals(mode))
                || System.getenv("VAADIN_DESKTOP_TEST_RECOVERY_TERMINATION_DIR") != null
                || current != null) {
            throw invalidConfiguration();
        }
        try {
            Path path = Path.of(directory);
            BasicFileAttributes attributes = checkedDirectory(path);
            try (var entries = Files.newDirectoryStream(path)) {
                if (entries.iterator().hasNext()) throw invalidState();
            }
            State state = new State(path, attributes.fileKey(), UUID.randomUUID().toString(),
                    ProcessHandle.current().pid(), mode);
            // Atomic no-replace publication also prevents two JVMs claiming
            // the same directory. A previous run is never resumed or replaced.
            publish(state, STARTUP);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    publish(state, HOOK);
                } catch (IOException | RuntimeException ignored) {
                    // Missing evidence fails acceptance; no path or raw state
                    // is logged, and blocked mode must not become cooperative.
                    System.err.println("Host shutdown hook marker failed");
                }
                if ("blocked".equals(state.mode())) awaitForever();
            }, "host-shutdown-termination-hook"));
            current = state;
        } catch (IOException | RuntimeException ignored) {
            throw invalidConfiguration();
        }
    }

    static void launcherReturned() {
        State state = current;
        if (state == null) return;
        try {
            publish(state, RETURNED);
        } catch (IOException | RuntimeException ignored) {
            throw new IllegalStateException("Host shutdown launcher marker failed");
        }
    }

    static boolean enabled() { return current != null; }
    static String runId() { return current == null ? "disabled" : current.runId(); }
    static String mode() { return current == null ? "disabled" : current.mode(); }

    private static void publish(State state, String name) throws IOException {
        synchronized (STATE_LOCK) {
            if (!RECORDS.contains(name)
                    || !state.directoryKey().equals(checkedDirectory(state.directory()).fileKey())) {
                throw invalidState();
            }
            boolean startupPresent = false;
            try (var entries = Files.newDirectoryStream(state.directory())) {
                for (Path entry : entries) {
                    String existing = entry.getFileName().toString();
                    if (!RECORDS.contains(existing) || existing.equals(name)) throw invalidState();
                    checkRecord(entry, state);
                    startupPresent |= STARTUP.equals(existing);
                }
            }
            if (!STARTUP.equals(name) && !startupPresent) throw invalidState();
            Path temporary = state.directory().resolve(".pending-" + UUID.randomUUID());
            Path target = state.directory().resolve(name);
            boolean created = false;
            try {
                try (FileChannel channel = FileChannel.open(temporary,
                        Set.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW,
                                LinkOption.NOFOLLOW_LINKS),
                        PosixFilePermissions.asFileAttribute(FILE_MODE))) {
                    created = true;
                    ByteBuffer bytes = ByteBuffer.wrap(state.json().getBytes(StandardCharsets.US_ASCII));
                    while (bytes.hasRemaining()) channel.write(bytes);
                    channel.force(true);
                }
                // Readers see a complete record or no record, never partial
                // JSON. The brief two-link publication state is not accepted.
                Files.createLink(target, temporary);
            } finally {
                if (created) Files.deleteIfExists(temporary);
            }
            checkRecord(target, state);
        }
    }

    private static void checkRecord(Path path, State state) throws IOException {
        BasicFileAttributes before = checkedFile(path);
        ByteBuffer bytes = ByteBuffer.allocate(MAX_RECORD_BYTES + 1);
        try (FileChannel channel = FileChannel.open(path,
                Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS))) {
            while (bytes.hasRemaining() && channel.read(bytes) >= 0) {
                // Bounded regular records; never read arbitrary state content.
            }
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
                || !path.getFileName().toString().matches("vatori-host-shutdown\\.[A-Za-z0-9]{6}")) {
            throw invalidState();
        }
        BasicFileAttributes attributes = Files.readAttributes(path,
                BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isDirectory() || attributes.isSymbolicLink() || attributes.fileKey() == null
                || !Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS).equals(DIRECTORY_MODE)
                || !Files.getOwner(path, LinkOption.NOFOLLOW_LINKS).equals(path.getFileSystem()
                        .getUserPrincipalLookupService().lookupPrincipalByName(System.getProperty("user.name")))
                || !path.toRealPath().equals(path)) {
            throw invalidState();
        }
        return attributes;
    }

    private static BasicFileAttributes checkedFile(Path path) throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(path,
                BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        Object links = Files.getAttribute(path, "unix:nlink", LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isRegularFile() || attributes.isSymbolicLink() || attributes.fileKey() == null
                || attributes.size() <= 0 || attributes.size() > MAX_RECORD_BYTES
                || !(links instanceof Number count) || count.longValue() != 1
                || !Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS).equals(FILE_MODE)
                || !Files.getOwner(path, LinkOption.NOFOLLOW_LINKS)
                        .equals(Files.getOwner(path.getParent(), LinkOption.NOFOLLOW_LINKS))) {
            throw invalidState();
        }
        return attributes;
    }

    private static void awaitForever() {
        CountDownLatch never = new CountDownLatch(1);
        while (true) {
            try {
                never.await();
            } catch (InterruptedException ignored) {
                // Deliberately non-cooperative hook, not a live-app worker.
            }
        }
    }

    private static IOException invalidState() {
        return new IOException("Private host shutdown state is invalid");
    }

    private static IllegalStateException invalidConfiguration() {
        return new IllegalStateException("Host shutdown startup configuration is invalid");
    }

    private record State(Path directory, Object directoryKey, String runId,
                         long processIdentifier, String mode) {
        String json() {
            return "{\"schemaVersion\":1,\"runId\":\"" + runId + "\",\"processIdentifier\":"
                    + processIdentifier + ",\"mode\":\"" + mode + "\"}\n";
        }
    }
}
