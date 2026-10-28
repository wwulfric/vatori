package dev.vatori.ipcframework;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Comparator;
import java.util.List;

/** JDK-only private-record checks. Does not launch a host or prove Sandbox policy. */
public final class FileWriteGrantReplayHandoffSmokeMain {
    private static final String TOKEN = "00112233-4455-6677-8899-aabbccddeeff";
    private static final String OTHER = "10112233-4455-6677-8899-aabbccddeeff";

    public static void main(String[] args) throws Exception {
        Path base = Files.createTempDirectory("vatori-write-replay-handoff-smoke-",
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        try {
            verify(base);
        } catch (IOException | RuntimeException ignored) {
            throw new AssertionError("Private write grant handoff smoke failed");
        } finally {
            try (var paths = Files.walk(base)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) { Files.delete(path); }
            }
        }
        System.out.println("PASS: private write grant replay handoff");
    }

    private static void verify(Path base) throws IOException {
        Path root = base.resolve("vatori-file-write-grant-replay.A1b2C3");
        Path file = root.resolve("old-grant-token.bin");
        long producer = ProcessHandle.current().pid();
        check(!Files.exists(root, LinkOption.NOFOLLOW_LINKS), "leaf begins absent");
        var old = FileWriteGrantReplayHandoff.create(file, TOKEN, producer);
        check(Files.getPosixFilePermissions(root).equals(PosixFilePermissions.fromString("rwx------")), "private leaf mode");
        check(Files.getPosixFilePermissions(file).equals(PosixFilePermissions.fromString("rw-------")), "private record mode");
        check(old.token().equals(TOKEN) && old.producerPid() == producer, "actual token and producer readback");
        check(old.same(FileWriteGrantReplayHandoff.readRecovered(file, producer + 1)), "new PID restores exact old token");
        check(FileWriteGrantReplayHandoff.retained(file, old), "private original retained");
        reject(() -> FileWriteGrantReplayHandoff.readRecovered(file, producer), "same PID is not recovery");
        reject(() -> FileWriteGrantReplayHandoff.readRecovered(file, 0), "invalid consumer PID");
        reject(() -> FileWriteGrantReplayHandoff.create(file, OTHER, producer), "cannot reuse existing leaf");
        check(FileWriteGrantReplayHandoff.retained(file, old), "duplicate create cannot overwrite original");

        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-r--r--"));
        reject(() -> FileWriteGrantReplayHandoff.readRecovered(file, producer + 1), "broad file mode rejected");
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
        Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("rwxr-xr-x"));
        reject(() -> FileWriteGrantReplayHandoff.readRecovered(file, producer + 1), "broad root mode rejected");
        Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("rwx------"));

        Path hardLink = root.resolve("other-link");
        Files.createLink(hardLink, file);
        reject(() -> FileWriteGrantReplayHandoff.readRecovered(file, producer + 1), "hard-linked record rejected");
        reject(() -> FileWriteGrantReplayHandoff.remove(file, old), "hard-linked removal rejected");
        Files.delete(hardLink);
        check(FileWriteGrantReplayHandoff.retained(file, old), "single link restored");

        Path saved = root.resolve("saved-record");
        Files.move(file, saved);
        Files.createSymbolicLink(file, saved);
        reject(() -> FileWriteGrantReplayHandoff.readRecovered(file, producer + 1), "final symlink rejected");
        Files.delete(file);
        Files.move(saved, file);
        Path parentLink = base.resolve("vatori-file-write-grant-replay.D4e5F6");
        Files.createSymbolicLink(parentLink, root);
        reject(() -> FileWriteGrantReplayHandoff.readRecovered(parentLink.resolve("old-grant-token.bin"), producer + 1),
                "generated parent symlink rejected");
        reject(() -> FileWriteGrantReplayHandoff.create(parentLink.resolve("old-grant-token.bin"), TOKEN, producer),
                "cannot create through existing parent symlink");
        Files.delete(parentLink);

        String original = "v1\n" + TOKEN + "\n" + producer + "\n";
        for (String value : List.of("", "x".repeat(97), "v1\nnot-a-token\n1\n", "v2\n" + TOKEN + "\n1\n",
                "v1\n" + TOKEN + "\n0\n", "v1\n" + TOKEN + "\n01\n", "v1\n" + TOKEN + "\n1",
                "v1\n" + TOKEN.toUpperCase() + "\n1\n", "v1\n" + TOKEN + "\n1\nextra\n")) {
            Files.writeString(file, value, StandardCharsets.US_ASCII);
            reject(() -> FileWriteGrantReplayHandoff.readRecovered(file, producer + 1), "malformed bounded record rejected");
        }
        Files.writeString(file, "v1\n" + OTHER + "\n" + producer + "\n", StandardCharsets.US_ASCII);
        check(!FileWriteGrantReplayHandoff.retained(file, old), "changed actual token detected");
        reject(() -> FileWriteGrantReplayHandoff.remove(file, old), "mismatching private record removal rejected");
        check(Files.exists(file, LinkOption.NOFOLLOW_LINKS)
                && FileWriteGrantReplayHandoff.readRecovered(file, producer + 1).token().equals(OTHER),
                "changed record remains present and unchanged");
        Files.writeString(file, original, StandardCharsets.US_ASCII);
        check(FileWriteGrantReplayHandoff.remove(file, old), "matching private record removed");
        check(!Files.exists(file, LinkOption.NOFOLLOW_LINKS) && Files.isDirectory(root), "record unlinked but owned root retained");
        reject(() -> FileWriteGrantReplayHandoff.create(file, TOKEN, producer), "empty old leaf cannot be reused");

        Path missing = base.resolve("missing-parent").resolve("vatori-file-write-grant-replay.G7h8I9").resolve("old-grant-token.bin");
        reject(() -> FileWriteGrantReplayHandoff.create(missing, TOKEN, producer), "missing ancestors are not recursively created");
        check(!Files.exists(base.resolve("missing-parent")), "no ancestor created");
        reject(() -> FileWriteGrantReplayHandoff.create(Path.of("relative"), TOKEN, producer), "relative path rejected");
        reject(() -> FileWriteGrantReplayHandoff.create(root.resolve("..").resolve(root.getFileName()).resolve("old-grant-token.bin"),
                TOKEN, producer), "unnormalized parent traversal rejected");
        reject(() -> FileWriteGrantReplayHandoff.create(base.resolve("wrong-leaf").resolve("old-grant-token.bin"), TOKEN, producer), "wrong leaf rejected");
        reject(() -> FileWriteGrantReplayHandoff.create(base.resolve("vatori-file-write-grant-replay.J1k2L3").resolve("wrong-name"), TOKEN, producer), "wrong filename rejected");
        Path bad = base.resolve("vatori-file-write-grant-replay.M4n5O6").resolve("old-grant-token.bin");
        reject(() -> FileWriteGrantReplayHandoff.create(bad, "bad-token", producer), "invalid token cannot be recorded");
        check(!Files.exists(bad, LinkOption.NOFOLLOW_LINKS), "invalid token creates no record");
        Path badPid = base.resolve("vatori-file-write-grant-replay.P7q8R9").resolve("old-grant-token.bin");
        reject(() -> FileWriteGrantReplayHandoff.create(badPid, TOKEN, 0), "invalid producer PID cannot be recorded");
        check(!Files.exists(badPid, LinkOption.NOFOLLOW_LINKS), "invalid producer creates no record");
    }

    private static void reject(CheckedAction action, String label) throws IOException {
        try { action.run(); } catch (IOException expected) { return; }
        throw new AssertionError(label);
    }

    private static void check(boolean condition, String label) { if (!condition) { throw new AssertionError(label); } }
    @FunctionalInterface
    private interface CheckedAction { void run() throws IOException; }
}
