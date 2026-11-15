package dev.vatori.ipcframework;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.Set;

/** Test-app-only, one-shot protocol fault after an external preparation check. */
final class FlowFatalProtocolStartup {
    static final String DIRECTORY_ENV = "VAADIN_DESKTOP_TEST_FATAL_PROTOCOL_DIR";
    private static final String STARTUP = "startup.pid";
    private static final String TRIGGER = "trigger";
    private static boolean initialized;

    private FlowFatalProtocolStartup() { }

    static synchronized void initialize() {
        String value = System.getenv(DIRECTORY_ENV);
        if (value == null) return;
        if (initialized
                || System.getenv("VAADIN_DESKTOP_TEST_HOST_SHUTDOWN_DIR") != null
                || System.getenv("VAADIN_DESKTOP_TEST_RECOVERY_TERMINATION_DIR") != null)
            throw invalidConfiguration();
        try {
            Path directory = Path.of(value);
            Object key = checkDirectory(directory);
            try (var entries = Files.newDirectoryStream(directory)) {
                if (entries.iterator().hasNext()) throw new IOException();
            }
            String pid = Long.toString(ProcessHandle.current().pid()) + "\n";
            Path startup = directory.resolve(STARTUP);
            // CREATE_NEW fails for any existing entry, including a symlink.
            try (var channel = Files.newByteChannel(startup,
                    Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE,
                            LinkOption.NOFOLLOW_LINKS),
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")))) {
                var bytes = java.nio.ByteBuffer.wrap(pid.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
                while (bytes.hasRemaining()) channel.write(bytes);
            }
            Object startupKey = checkFile(startup, pid.length());
            initialized = true;
            Thread.ofPlatform().daemon().name("test-fatal-protocol-trigger").start(() -> {
                long deadline = System.nanoTime() + Duration.ofMinutes(10).toNanos();
                try {
                    while (System.nanoTime() < deadline) {
                        checkPreparation(directory, key, startup, startupKey, pid);
                        Path trigger = directory.resolve(TRIGGER);
                        if (Files.exists(trigger, LinkOption.NOFOLLOW_LINKS)) {
                            checkFile(trigger, 0);
                            // The producer writes preparation state before publishing
                            // the trigger. Validate again after observing publication:
                            // an earlier poll may have read the old valid startup file.
                            checkPreparation(directory, key, startup, startupKey, pid);
                            // No application data or path enters the wire. This tests
                            // the real version rejection, not a direct host callback.
                            System.out.println("VFIPC {\"version\":2,\"type\":\"error\",\"payload\":{},\"error\":null}");
                            System.out.flush();
                            return;
                        }
                        Thread.sleep(25);
                    }
                    System.err.println("Fatal protocol test trigger expired");
                } catch (IOException | InterruptedException | RuntimeException failure) {
                    // Invalid or changed state must never trigger a protocol fault.
                    System.err.println("Fatal protocol test trigger rejected");
                }
            });
        } catch (IOException | RuntimeException failure) {
            throw invalidConfiguration();
        }
    }

    private static void checkPreparation(Path directory, Object key, Path startup,
            Object startupKey, String pid) throws IOException {
        if (!key.equals(checkDirectory(directory))) throw new IOException();
        if (!startupKey.equals(checkFile(startup, pid.length()))) throw new IOException();
        var bytes = java.nio.ByteBuffer.allocate(32);
        try (var channel = Files.newByteChannel(startup,
                Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS))) {
            while (bytes.hasRemaining() && channel.read(bytes) >= 0) { }
        }
        if (!startupKey.equals(checkFile(startup, pid.length()))
                || bytes.position() != pid.length()
                || !pid.equals(new String(bytes.array(), 0, bytes.position(),
                        java.nio.charset.StandardCharsets.US_ASCII))) throw new IOException();
        try (var entries = Files.newDirectoryStream(directory)) {
            for (Path entry : entries) {
                if (!Set.of(STARTUP, TRIGGER).contains(entry.getFileName().toString()))
                    throw new IOException();
            }
        }
    }

    private static Object checkDirectory(Path path) throws IOException {
        if (!path.isAbsolute() || !path.equals(path.normalize()) || path.getFileName() == null
                || !path.getFileName().toString().matches("vatori-fatal-protocol\\.[A-Za-z0-9]{6}")
                || !path.toRealPath().equals(path)) throw new IOException();
        var attributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isDirectory() || attributes.isSymbolicLink() || attributes.fileKey() == null
                || !Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS)
                        .equals(PosixFilePermissions.fromString("rwx------"))
                || !Files.getOwner(path, LinkOption.NOFOLLOW_LINKS).equals(path.getFileSystem()
                        .getUserPrincipalLookupService().lookupPrincipalByName(System.getProperty("user.name"))))
            throw new IOException();
        return attributes.fileKey();
    }

    private static Object checkFile(Path path, long size) throws IOException {
        var attributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        Object links = Files.getAttribute(path, "unix:nlink", LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isRegularFile() || attributes.isSymbolicLink() || attributes.fileKey() == null
                || attributes.size() != size
                || !(links instanceof Number count) || count.longValue() != 1
                || !Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS)
                        .equals(PosixFilePermissions.fromString("rw-------"))
                || !Files.getOwner(path, LinkOption.NOFOLLOW_LINKS)
                        .equals(Files.getOwner(path.getParent(), LinkOption.NOFOLLOW_LINKS))) throw new IOException();
        return attributes.fileKey();
    }

    private static IllegalStateException invalidConfiguration() {
        return new IllegalStateException("Fatal protocol test startup configuration is invalid");
    }
}
