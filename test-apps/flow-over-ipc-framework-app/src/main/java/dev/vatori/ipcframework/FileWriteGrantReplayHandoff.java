package dev.vatori.ipcframework;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

/** Acceptance-only container handoff; reuses the bounded private read codec. */
final class FileWriteGrantReplayHandoff {
    private FileWriteGrantReplayHandoff() { }

    static Path configuredPath() throws IOException {
        String value = System.getenv("VATORI_FILE_WRITE_GRANT_REPLAY_HANDOFF");
        if (value == null || value.isBlank()) {
            throw invalid();
        }
        return checkedPath(Path.of(value));
    }

    static Snapshot create(Path configured, String token, long producerPid) throws IOException {
        Path path = checkedPath(configured);
        // LaunchServices has already initialized the app container. Only this
        // exact random leaf is created, never missing ancestors or an old leaf.
        Files.createDirectory(path.getParent(), PosixFilePermissions.asFileAttribute(
                PosixFilePermissions.fromString("rwx------")));
        var value = FileReadGrantReplayHandoff.create(path, token, producerPid);
        requireSingleLink(path);
        return new Snapshot(value);
    }

    static Snapshot readRecovered(Path configured, long currentPid) throws IOException {
        Path path = checkedPath(configured);
        requireSingleLink(path);
        return new Snapshot(FileReadGrantReplayHandoff.readRecovered(path, currentPid));
    }

    static boolean retained(Path configured, Snapshot expected) throws IOException {
        Path path = checkedPath(configured);
        requireSingleLink(path);
        return expected != null && FileReadGrantReplayHandoff.retained(path, expected.value);
    }

    static boolean remove(Path configured, Snapshot expected) throws IOException {
        Path path = checkedPath(configured);
        requireSingleLink(path);
        return expected != null && FileReadGrantReplayHandoff.remove(path, expected.value);
    }

    private static Path checkedPath(Path path) throws IOException {
        if (!path.isAbsolute() || path.getFileName() == null || path.getParent() == null
                || !path.equals(path.normalize())
                || !"old-grant-token.bin".equals(path.getFileName().toString())
                || path.getParent().getFileName() == null
                || !path.getParent().getFileName().toString()
                        .matches("vatori-file-write-grant-replay\\.[A-Za-z0-9]{6}")) {
            throw invalid();
        }
        return path;
    }

    private static void requireSingleLink(Path path) throws IOException {
        Object links = Files.getAttribute(path, "unix:nlink", LinkOption.NOFOLLOW_LINKS);
        if (!(links instanceof Number count) || count.longValue() != 1) {
            throw invalid();
        }
    }

    private static IOException invalid() {
        return new IOException("Private write grant handoff is invalid");
    }

    static final class Snapshot {
        private final FileReadGrantReplayHandoff.Snapshot value;

        private Snapshot(FileReadGrantReplayHandoff.Snapshot value) { this.value = value; }
        String token() { return value.token(); }
        long producerPid() { return value.producerPid(); }
        boolean same(Snapshot other) { return other != null && value.same(other.value); }
    }
}
