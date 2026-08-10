package dev.vatori.ipcframework;

import dev.vatori.desktop.flow.DesktopFlowMultipartConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

final class FlowMultipartAcceptanceStorage {
    private static final int THRESHOLD_BYTES = 64 * 1024;
    private static final Path ROOT = Path.of(
            System.getProperty("java.io.tmpdir"),
            "vatori-flow-multipart-acceptance-"
                    + ProcessHandle.current().pid());

    private FlowMultipartAcceptanceStorage() {
    }

    static DesktopFlowMultipartConfig config() {
        try {
            deleteTree(ROOT);
            Files.createDirectories(ROOT);
        } catch (IOException error) {
            throw new IllegalStateException(
                    "Unable to prepare multipart acceptance storage", error);
        }
        return DesktopFlowMultipartConfig.builder()
                .location(ROOT)
                .memoryThresholdBytes(THRESHOLD_BYTES)
                .maxFileSizeBytes(16L * 1024 * 1024)
                .maxRequestSizeBytes(32L * 1024 * 1024)
                .maxPartCount(64)
                .build();
    }

    static Snapshot snapshot() {
        if (!Files.exists(ROOT)) {
            return new Snapshot(0, -1);
        }
        try (var paths = Files.walk(ROOT)) {
            long[] sizes = paths.filter(Files::isRegularFile)
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (IOException error) {
                            throw new StorageInspectionException(error);
                        }
                    })
                    .toArray();
            long largest = -1;
            for (long size : sizes) {
                largest = Math.max(largest, size);
            }
            return new Snapshot(sizes.length, largest);
        } catch (StorageInspectionException error) {
            throw new IllegalStateException(
                    "Unable to inspect multipart acceptance storage",
                    error.getCause());
        } catch (IOException error) {
            throw new IllegalStateException(
                    "Unable to inspect multipart acceptance storage", error);
        }
    }

    static boolean isEmpty() {
        if (!Files.exists(ROOT)) {
            return true;
        }
        try (var paths = Files.list(ROOT)) {
            return paths.findAny().isEmpty();
        } catch (IOException error) {
            throw new IllegalStateException(
                    "Unable to inspect multipart acceptance cleanup", error);
        }
    }

    static RequestDirectoryMonitor monitor() {
        return new RequestDirectoryMonitor();
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    record Snapshot(long fileCount, long largestFileBytes) {
        boolean spilled(long expectedBytes) {
            return fileCount > 0 && largestFileBytes == expectedBytes;
        }
    }

    static final class RequestDirectoryMonitor implements AutoCloseable {
        private static final long POLL_MILLIS = 2;

        private final AtomicBoolean running = new AtomicBoolean(true);
        private final AtomicInteger maximumDirectories = new AtomicInteger();
        private final AtomicInteger maximumFiles = new AtomicInteger();
        private final AtomicReference<String> failure =
                new AtomicReference<>("");
        private final Thread worker;

        private RequestDirectoryMonitor() {
            worker = Thread.ofVirtual()
                    .name("flow-multipart-acceptance-storage-monitor")
                    .start(this::run);
        }

        MonitorSnapshot snapshot() {
            return new MonitorSnapshot(
                    maximumDirectories.get(),
                    maximumFiles.get(),
                    failure.get());
        }

        @Override
        public void close() {
            if (running.compareAndSet(true, false)) {
                worker.interrupt();
            }
        }

        private void run() {
            while (running.get()) {
                try {
                    maximumDirectories.accumulateAndGet(
                            requestDirectoryCount(), Math::max);
                    maximumFiles.accumulateAndGet(
                            regularFileCount(), Math::max);
                    Thread.sleep(POLL_MILLIS);
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (IOException error) {
                    failure.compareAndSet("", error.toString());
                    return;
                }
            }
        }

        private static int requestDirectoryCount() throws IOException {
            if (!Files.exists(ROOT)) {
                return 0;
            }
            try (var paths = Files.list(ROOT)) {
                return Math.toIntExact(paths.filter(Files::isDirectory).count());
            }
        }

        private static int regularFileCount() throws IOException {
            if (!Files.exists(ROOT)) {
                return 0;
            }
            try (var paths = Files.walk(ROOT)) {
                return Math.toIntExact(paths.filter(Files::isRegularFile).count());
            }
        }
    }

    record MonitorSnapshot(
            int maximumDirectories,
            int maximumFiles,
            String failure) {
    }

    private static final class StorageInspectionException
            extends RuntimeException {
        private StorageInspectionException(IOException cause) {
            super(cause);
        }
    }
}
