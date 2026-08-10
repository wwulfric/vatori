package dev.vatori.desktop.flow;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Configures generic multipart request parsing for the Flow IPC servlet adapter.
 */
public final class DesktopFlowMultipartConfig {
    private static final int DEFAULT_MEMORY_THRESHOLD_BYTES = 64 * 1024;
    private static final long DEFAULT_MAX_PART_COUNT = 10_000;
    private static final int DEFAULT_MAX_PART_HEADER_SIZE_BYTES = 512;
    private static final DesktopFlowMultipartConfig DEFAULTS = builder().build();

    private final Path location;
    private final int memoryThresholdBytes;
    private final long maxFileSizeBytes;
    private final long maxRequestSizeBytes;
    private final long maxPartCount;
    private final int maxPartHeaderSizeBytes;

    private DesktopFlowMultipartConfig(Builder builder) {
        location = builder.location.toAbsolutePath().normalize();
        memoryThresholdBytes = builder.memoryThresholdBytes;
        maxFileSizeBytes = builder.maxFileSizeBytes;
        maxRequestSizeBytes = builder.maxRequestSizeBytes;
        maxPartCount = builder.maxPartCount;
        maxPartHeaderSizeBytes = builder.maxPartHeaderSizeBytes;
    }

    public static DesktopFlowMultipartConfig defaults() {
        return DEFAULTS;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Directory under which request-scoped temporary directories are created. */
    public Path location() {
        return location;
    }

    /** Maximum part size retained in memory before spilling to disk. */
    public int memoryThresholdBytes() {
        return memoryThresholdBytes;
    }

    /** Maximum file-part size, or {@code -1} for no adapter-level limit. */
    public long maxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    /** Maximum complete request size, or {@code -1} for no adapter-level limit. */
    public long maxRequestSizeBytes() {
        return maxRequestSizeBytes;
    }

    /** Maximum number of parts, or {@code -1} for no adapter-level limit. */
    public long maxPartCount() {
        return maxPartCount;
    }

    /** Maximum number of bytes accepted in one part's headers. */
    public int maxPartHeaderSizeBytes() {
        return maxPartHeaderSizeBytes;
    }

    public static final class Builder {
        private Path location = Path.of(Objects.requireNonNullElse(
                System.getProperty("java.io.tmpdir"), "."));
        private int memoryThresholdBytes = DEFAULT_MEMORY_THRESHOLD_BYTES;
        private long maxFileSizeBytes = -1;
        private long maxRequestSizeBytes = -1;
        private long maxPartCount = DEFAULT_MAX_PART_COUNT;
        private int maxPartHeaderSizeBytes = DEFAULT_MAX_PART_HEADER_SIZE_BYTES;

        private Builder() {
        }

        public Builder location(Path location) {
            this.location = Objects.requireNonNull(location, "location");
            return this;
        }

        public Builder memoryThresholdBytes(int memoryThresholdBytes) {
            if (memoryThresholdBytes < 0) {
                throw new IllegalArgumentException(
                        "Multipart memory threshold must not be negative");
            }
            this.memoryThresholdBytes = memoryThresholdBytes;
            return this;
        }

        public Builder maxFileSizeBytes(long maxFileSizeBytes) {
            this.maxFileSizeBytes = limit(maxFileSizeBytes, "file size");
            return this;
        }

        public Builder maxRequestSizeBytes(long maxRequestSizeBytes) {
            this.maxRequestSizeBytes = limit(maxRequestSizeBytes, "request size");
            return this;
        }

        public Builder maxPartCount(long maxPartCount) {
            this.maxPartCount = limit(maxPartCount, "part count");
            return this;
        }

        public Builder maxPartHeaderSizeBytes(int maxPartHeaderSizeBytes) {
            if (maxPartHeaderSizeBytes <= 0) {
                throw new IllegalArgumentException(
                        "Multipart part-header limit must be positive");
            }
            this.maxPartHeaderSizeBytes = maxPartHeaderSizeBytes;
            return this;
        }

        public DesktopFlowMultipartConfig build() {
            return new DesktopFlowMultipartConfig(this);
        }

        private static long limit(long value, String name) {
            if (value < -1) {
                throw new IllegalArgumentException(
                        "Multipart " + name + " must be -1 or non-negative");
            }
            return value;
        }
    }
}
