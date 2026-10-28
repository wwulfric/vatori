package dev.vatori.ipcframework;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;

/** Acceptance-only startup fixture; never used by a framework launcher. */
final class FlowRecoveryTerminationStartup {
    private static final String ENVIRONMENT =
            "VAADIN_DESKTOP_TEST_RECOVERY_TERMINATION_DIR";
    private static final String LOCK = "state.lock";
    private static final String HOOK = "gen2-hook.json";
    private static final int MAX_RECORD_BYTES = 192;
    private static final Set<PosixFilePermission> DIRECTORY_MODE =
            PosixFilePermissions.fromString("rwx------");
    private static final Set<PosixFilePermission> FILE_MODE =
            PosixFilePermissions.fromString("rw-------");
    private static final Pattern RECORD = Pattern.compile(
            "\\{\"schemaVersion\":1,\"runId\":\"([0-9a-f-]{36})\","
                    + "\"generation\":([123]),\"processIdentifier\":([1-9][0-9]{0,18})\\}\\n");
    private static volatile Snapshot current;

    private FlowRecoveryTerminationStartup() { }

    static void initialize() {
        String configured = System.getenv(ENVIRONMENT);
        if (configured == null) {
            return;
        }
        try {
            Path directory = checkedDirectory(Path.of(configured));
            Snapshot claimed = claim(directory);
            current = claimed;
            if (claimed.generation() == 2) {
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        writeHook(directory, claimed);
                    } catch (IOException | RuntimeException ignored) {
                        // The missing marker makes acceptance fail. Never emit
                        // state paths, and do not turn failure into cooperation.
                        System.err.println("Recovery termination hook marker failed");
                    }
                    awaitForever();
                }, "recovery-termination-blocked-shutdown"));
                // This generation deliberately never calls DesktopLauncher and
                // cannot publish ready. Only the external host can retire it.
                awaitForever();
            }
        } catch (IOException | RuntimeException ignored) {
            throw new IllegalStateException("Recovery termination startup state is invalid");
        }
    }

    static boolean enabled() {
        return current != null;
    }

    static int generation() {
        Snapshot value = current;
        return value == null ? 0 : value.generation();
    }

    private static Snapshot claim(Path directory) throws IOException {
        Path lock = directory.resolve(LOCK);
        try {
            Files.createFile(lock, PosixFilePermissions.asFileAttribute(FILE_MODE));
        } catch (FileAlreadyExistsException ignored) {
            // Existing locks are accepted only after all ownership/type checks.
        }
        checkedFile(lock, true);
        try (FileChannel channel = FileChannel.open(lock,
                Set.of(StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS));
             FileLock held = channel.tryLock()) {
            if (held == null) {
                throw invalid();
            }
            List<Snapshot> records = readState(directory);
            if (records.size() >= 3) {
                throw invalid();
            }
            for (Snapshot previous : records) {
                if (ProcessHandle.of(previous.processIdentifier())
                        .map(ProcessHandle::isAlive).orElse(false)) {
                    // Refuse overlapping starts or a reused PID, rather than
                    // silently advancing another live run's generation.
                    throw invalid();
                }
            }
            int generation = records.size() + 1;
            String runId = records.isEmpty() ? UUID.randomUUID().toString()
                    : records.getFirst().runId();
            Snapshot next = new Snapshot(runId, generation,
                    ProcessHandle.current().pid());
            publish(directory, recordName(generation), next);
            return next;
        }
    }

    private static void writeHook(Path directory, Snapshot expected) throws IOException {
        Path lock = checkedDirectory(directory).resolve(LOCK);
        checkedFile(lock, true);
        try (FileChannel channel = FileChannel.open(lock,
                Set.of(StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS));
             FileLock held = channel.tryLock()) {
            if (held == null) {
                throw invalid();
            }
            List<Snapshot> records = readState(directory);
            if (records.size() != 2 || !expected.equals(records.get(1))) {
                throw invalid();
            }
            publish(directory, HOOK, expected);
        }
    }

    private static List<Snapshot> readState(Path directory) throws IOException {
        checkedDirectory(directory);
        Set<String> names = new HashSet<>();
        Set<String> allowed = Set.of(LOCK, HOOK, recordName(1), recordName(2), recordName(3));
        try (var entries = Files.newDirectoryStream(directory)) {
            for (Path entry : entries) {
                String name = entry.getFileName().toString();
                if (!allowed.contains(name) || !names.add(name)) {
                    throw invalid();
                }
                checkedFile(entry, LOCK.equals(name));
            }
        }
        if (!names.contains(LOCK)) {
            throw invalid();
        }
        List<Snapshot> records = new ArrayList<>();
        Set<Long> pids = new HashSet<>();
        for (int generation = 1; generation <= 3; generation++) {
            if (!names.contains(recordName(generation))) {
                continue;
            }
            Snapshot record = read(directory.resolve(recordName(generation)));
            if (record.generation() != generation || records.size() != generation - 1
                    || !pids.add(record.processIdentifier())
                    || (!records.isEmpty() && !records.getFirst().runId().equals(record.runId()))) {
                throw invalid();
            }
            records.add(record);
        }
        if (names.contains(HOOK)) {
            if (records.size() < 2 || !read(directory.resolve(HOOK)).equals(records.get(1))) {
                throw invalid();
            }
        } else if (records.size() >= 2) {
            // A generation-two process can inspect its own pre-hook state,
            // but generation three must prove the blocked shutdown was entered.
            if (records.get(1).processIdentifier() != ProcessHandle.current().pid()) {
                throw invalid();
            }
        }
        return records;
    }

    private static Snapshot read(Path path) throws IOException {
        BasicFileAttributes before = checkedFile(path, false);
        ByteBuffer bytes = ByteBuffer.allocate(MAX_RECORD_BYTES + 1);
        try (FileChannel channel = FileChannel.open(path,
                Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS))) {
            while (bytes.hasRemaining() && channel.read(bytes) >= 0) {
                // A bounded regular record, never an unbounded state payload.
            }
        }
        BasicFileAttributes after = checkedFile(path, false);
        if (!before.fileKey().equals(after.fileKey()) || bytes.position() != before.size()
                || before.size() != after.size()) {
            throw invalid();
        }
        var match = RECORD.matcher(new String(bytes.array(), 0, bytes.position(),
                StandardCharsets.US_ASCII));
        if (!match.matches()) {
            throw invalid();
        }
        String runId = match.group(1);
        if (!UUID.fromString(runId).toString().equals(runId)) {
            throw invalid();
        }
        return new Snapshot(runId, Integer.parseInt(match.group(2)),
                Long.parseLong(match.group(3)));
    }

    private static void publish(Path directory, String name, Snapshot snapshot) throws IOException {
        checkedDirectory(directory);
        Path temporary = directory.resolve(".pending-" + UUID.randomUUID());
        Path target = directory.resolve(name);
        try {
            try (FileChannel channel = FileChannel.open(temporary,
                    Set.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW,
                            LinkOption.NOFOLLOW_LINKS),
                    PosixFilePermissions.asFileAttribute(FILE_MODE))) {
                ByteBuffer bytes = ByteBuffer.wrap(snapshot.json().getBytes(StandardCharsets.US_ASCII));
                while (bytes.hasRemaining()) {
                    channel.write(bytes);
                }
                channel.force(true);
            }
            // Atomic, no-replace publication: a reader never sees a partially
            // written JSON record, and no existing record can be overwritten.
            Files.createLink(target, temporary);
        } finally {
            Files.deleteIfExists(temporary);
        }
        if (!snapshot.equals(read(target))) {
            throw invalid();
        }
    }

    private static Path checkedDirectory(Path path) throws IOException {
        if (!path.isAbsolute() || !path.equals(path.normalize()) || path.getFileName() == null
                || !path.getFileName().toString().matches("vatori-recovery-termination\\.[A-Za-z0-9]{6}")) {
            throw invalid();
        }
        BasicFileAttributes attributes = Files.readAttributes(path,
                BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isDirectory() || attributes.isSymbolicLink()
                || !Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS).equals(DIRECTORY_MODE)
                || !Files.getOwner(path, LinkOption.NOFOLLOW_LINKS).equals(path.getFileSystem()
                        .getUserPrincipalLookupService().lookupPrincipalByName(System.getProperty("user.name")))
                || !path.toRealPath().equals(path)) {
            throw invalid();
        }
        return path;
    }

    private static BasicFileAttributes checkedFile(Path path, boolean lock) throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(path,
                BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        Object links = Files.getAttribute(path, "unix:nlink", LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isRegularFile() || attributes.isSymbolicLink() || attributes.fileKey() == null
                || attributes.size() > (lock ? 0 : MAX_RECORD_BYTES)
                || (!lock && attributes.size() == 0)
                || !(links instanceof Number count) || count.longValue() != 1
                || !Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS).equals(FILE_MODE)
                || !Files.getOwner(path, LinkOption.NOFOLLOW_LINKS)
                        .equals(Files.getOwner(path.getParent(), LinkOption.NOFOLLOW_LINKS))) {
            throw invalid();
        }
        return attributes;
    }

    private static String recordName(int generation) {
        return "generation-" + generation + ".json";
    }

    private static void awaitForever() {
        CountDownLatch never = new CountDownLatch(1);
        while (true) {
            try {
                never.await();
            } catch (InterruptedException ignored) {
                // Fixture deliberately ignores interruption, including shutdown.
            }
        }
    }

    private static IOException invalid() {
        return new IOException("Private recovery termination state is invalid");
    }

    private record Snapshot(String runId, int generation, long processIdentifier) {
        String json() {
            return "{\"schemaVersion\":1,\"runId\":\"" + runId + "\",\"generation\":" + generation
                    + ",\"processIdentifier\":" + processIdentifier + "}\n";
        }
    }
}
