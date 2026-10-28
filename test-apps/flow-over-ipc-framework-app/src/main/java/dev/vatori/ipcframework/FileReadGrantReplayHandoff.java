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
import java.nio.file.attribute.UserPrincipal;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

/** Private, bounded acceptance handoff, not a product grant persistence API. */
final class FileReadGrantReplayHandoff {
    private static final int MAX_BYTES = 96;
    private static final Set<PosixFilePermission> DIRECTORY_MODE =
            PosixFilePermissions.fromString("rwx------");
    private static final Set<PosixFilePermission> FILE_MODE =
            PosixFilePermissions.fromString("rw-------");

    private FileReadGrantReplayHandoff() { }

    static Path configuredPath() throws IOException {
        String value = System.getenv("VATORI_FILE_READ_GRANT_REPLAY_HANDOFF");
        if (value == null || value.isBlank()) {
            throw invalid();
        }
        Path path = Path.of(value);
        if (!path.isAbsolute()) {
            throw invalid();
        }
        return path.normalize();
    }

    static Snapshot create(Path configured, String token, long producerPid)
            throws IOException {
        Snapshot expected = checkedSnapshot(token, producerPid);
        Path path = checkedPath(configured);
        byte[] bytes = encode(expected);
        try (FileChannel channel = FileChannel.open(path,
                Set.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW,
                        LinkOption.NOFOLLOW_LINKS),
                PosixFilePermissions.asFileAttribute(FILE_MODE))) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
        } finally {
            Arrays.fill(bytes, (byte) 0);
        }
        Snapshot actual = read(path);
        if (!expected.same(actual)) {
            throw invalid();
        }
        return actual;
    }

    static Snapshot readRecovered(Path path, long currentPid) throws IOException {
        Snapshot snapshot = read(path);
        if (currentPid <= 0 || snapshot.producerPid == currentPid) {
            throw invalid();
        }
        return snapshot;
    }

    static Snapshot read(Path configured) throws IOException {
        Path path = checkedPath(configured);
        BasicFileAttributes attributes = Files.readAttributes(path,
                BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isRegularFile() || attributes.isSymbolicLink()
                || attributes.size() <= 0 || attributes.size() > MAX_BYTES
                || !Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS)
                        .equals(FILE_MODE)
                || !Files.getOwner(path, LinkOption.NOFOLLOW_LINKS)
                        .equals(currentOwner(path))) {
            throw invalid();
        }
        ByteBuffer buffer = ByteBuffer.allocate(MAX_BYTES + 1);
        try (FileChannel channel = FileChannel.open(path,
                Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS))) {
            while (buffer.hasRemaining() && channel.read(buffer) >= 0) {
                // The regular file is bounded; never read arbitrary payload size.
            }
        }
        if (buffer.position() > MAX_BYTES) {
            Arrays.fill(buffer.array(), (byte) 0);
            throw invalid();
        }
        byte[] bytes = Arrays.copyOf(buffer.array(), buffer.position());
        Arrays.fill(buffer.array(), (byte) 0);
        try {
            String[] fields = new String(bytes, StandardCharsets.US_ASCII)
                    .split("\n", -1);
            if (fields.length != 4 || !"v1".equals(fields[0])
                    || !fields[3].isEmpty()) {
                throw invalid();
            }
            long pid;
            try {
                pid = Long.parseLong(fields[2]);
            } catch (NumberFormatException ignored) {
                throw invalid();
            }
            Snapshot snapshot = checkedSnapshot(fields[1], pid);
            if (!Long.toString(pid).equals(fields[2])
                    || !Arrays.equals(bytes, encode(snapshot))) {
                throw invalid();
            }
            return snapshot;
        } finally {
            Arrays.fill(bytes, (byte) 0);
        }
    }

    static boolean retained(Path path, Snapshot expected) throws IOException {
        return expected.same(read(path));
    }

    static boolean remove(Path configured, Snapshot expected) throws IOException {
        Path path = checkedPath(configured);
        if (!expected.same(read(path))) {
            throw invalid();
        }
        Files.delete(path);
        return !Files.exists(path, LinkOption.NOFOLLOW_LINKS);
    }

    private static Path checkedPath(Path configured) throws IOException {
        if (!configured.isAbsolute() || configured.getFileName() == null) {
            throw invalid();
        }
        Path normalized = configured.normalize();
        Path parent = normalized.getParent();
        BasicFileAttributes attributes = Files.readAttributes(parent,
                BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isDirectory() || attributes.isSymbolicLink()
                || !Files.getPosixFilePermissions(parent, LinkOption.NOFOLLOW_LINKS)
                        .equals(DIRECTORY_MODE)
                || !Files.getOwner(parent, LinkOption.NOFOLLOW_LINKS)
                        .equals(currentOwner(parent))) {
            throw invalid();
        }
        // Resolve system ancestors such as /var -> /private/var. The generated
        // parent and final handoff itself must not be symbolic links.
        return parent.toRealPath().resolve(normalized.getFileName());
    }

    private static UserPrincipal currentOwner(Path path) throws IOException {
        return path.getFileSystem().getUserPrincipalLookupService()
                .lookupPrincipalByName(System.getProperty("user.name"));
    }

    private static Snapshot checkedSnapshot(String token, long pid) throws IOException {
        try {
            if (token == null || token.length() != 36
                    || !UUID.fromString(token).toString().equals(token) || pid <= 0) {
                throw invalid();
            }
        } catch (IllegalArgumentException ignored) {
            throw invalid();
        }
        return new Snapshot(token, pid);
    }

    private static byte[] encode(Snapshot snapshot) {
        return ("v1\n" + snapshot.token + "\n" + snapshot.producerPid + "\n")
                .getBytes(StandardCharsets.US_ASCII);
    }

    private static IOException invalid() {
        return new IOException("Private file grant handoff is invalid");
    }

    static final class Snapshot {
        private final String token;
        private final long producerPid;

        private Snapshot(String token, long producerPid) {
            this.token = token;
            this.producerPid = producerPid;
        }

        String token() { return token; }
        long producerPid() { return producerPid; }
        boolean same(Snapshot other) {
            return other != null && producerPid == other.producerPid
                    && token.equals(other.token);
        }
    }
}
