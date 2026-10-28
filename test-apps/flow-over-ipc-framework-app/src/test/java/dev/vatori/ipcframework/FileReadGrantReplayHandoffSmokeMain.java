package dev.vatori.ipcframework;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Comparator;
import java.util.List;

/** Standalone JDK-only checks; no app, Vaadin, sidecar or native host is started. */
public final class FileReadGrantReplayHandoffSmokeMain {
    private static final String TOKEN = "00112233-4455-6677-8899-aabbccddeeff";

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("vatori-file-grant-replay-smoke-",
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        try {
            verify(root);
        } catch (IOException | RuntimeException failure) {
            throw new AssertionError("Private file grant handoff smoke failed");
        } finally {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
        System.out.println("PASS: private file grant replay handoff");
    }

    private static void verify(Path root) throws IOException {
        Path file = root.resolve("handoff");
        long producerPid = ProcessHandle.current().pid();
        long consumerPid = producerPid + 1;
        var original = FileReadGrantReplayHandoff.create(file, TOKEN, producerPid);
        check(original.token().equals(TOKEN) && original.producerPid() == producerPid,
                "create readback equality");
        check(Files.getPosixFilePermissions(file, LinkOption.NOFOLLOW_LINKS)
                .equals(PosixFilePermissions.fromString("rw-------")), "owner-only file mode");
        check(original.same(FileReadGrantReplayHandoff.read(file)), "exact record roundtrip");
        check(original.same(FileReadGrantReplayHandoff.readRecovered(file, consumerPid)),
                "different process restores actual token");
        reject(() -> FileReadGrantReplayHandoff.readRecovered(file, producerPid),
                "same producer process is not recovery");
        reject(() -> FileReadGrantReplayHandoff.readRecovered(file, 0), "invalid consumer PID");
        reject(() -> FileReadGrantReplayHandoff.create(file, TOKEN, producerPid),
                "duplicate CREATE_NEW rejected");
        check(original.same(FileReadGrantReplayHandoff.read(file)), "duplicate cannot overwrite record");

        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-r--r--"));
        reject(() -> FileReadGrantReplayHandoff.read(file), "public file permissions rejected");
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
        Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("rwxr-xr-x"));
        reject(() -> FileReadGrantReplayHandoff.read(file), "public parent permissions rejected");
        Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("rwx------"));

        Path link = root.resolve("link");
        Files.createSymbolicLink(link, file);
        reject(() -> FileReadGrantReplayHandoff.read(link), "final symlink rejected");
        reject(() -> FileReadGrantReplayHandoff.create(link, TOKEN, producerPid), "create through symlink rejected");
        Files.delete(link);

        Path actualParent = Files.createDirectory(root.resolve("parent"),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        Path parentLink = root.resolve("parent-link");
        Files.createSymbolicLink(parentLink, actualParent);
        reject(() -> FileReadGrantReplayHandoff.create(parentLink.resolve("handoff"), TOKEN, producerPid),
                "generated parent symlink rejected");
        Files.delete(parentLink);
        Files.delete(actualParent);

        for (String record : List.of("", "v1\nnot-a-uuid\n1\n", "v2\n" + TOKEN + "\n1\n",
                "v1\n" + TOKEN + "\n0\n", "v1\n" + TOKEN + "\n-1\n",
                "v1\n" + TOKEN + "\n01\n", "v1\n" + TOKEN + "\n1",
                "v1\n" + TOKEN + "\n1\nextra\n", "x".repeat(97),
                "v1\n" + TOKEN.toUpperCase() + "\n1\n")) {
            Files.writeString(file, record, StandardCharsets.US_ASCII);
            reject(() -> FileReadGrantReplayHandoff.read(file), "malformed or oversized record rejected");
        }
        Files.delete(file);
        reject(() -> FileReadGrantReplayHandoff.create(file, "bad-token", producerPid),
                "invalid token cannot be persisted");
        reject(() -> FileReadGrantReplayHandoff.create(file, TOKEN, 0), "invalid producer PID");
        check(!Files.exists(file, LinkOption.NOFOLLOW_LINKS), "invalid record never created");
        original = FileReadGrantReplayHandoff.create(file, TOKEN, producerPid);
        check(FileReadGrantReplayHandoff.retained(file, original), "handoff retained before final release");
        check(FileReadGrantReplayHandoff.remove(file, original), "matching handoff removed");
        check(!Files.exists(file, LinkOption.NOFOLLOW_LINKS), "no file after removal");
    }

    private static void reject(CheckedAction action, String label) throws IOException {
        try {
            action.run();
        } catch (IOException expected) {
            return;
        }
        throw new AssertionError(label);
    }

    private static void check(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label);
        }
    }

    @FunctionalInterface
    private interface CheckedAction { void run() throws IOException; }
}
