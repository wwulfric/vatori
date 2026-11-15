package dev.vatori.desktop.flow.ipc;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.IntConsumer;

final class OpaqueHttpRequestBodyInputStream extends InputStream {
    static final int INITIAL_CREDITS = 4;

    private static final Frame END = new Frame(new byte[0], true, false);
    private static final Frame CANCEL = new Frame(new byte[0], false, true);

    private final BlockingQueue<Frame> frames = new ArrayBlockingQueue<>(INITIAL_CREDITS + 1);
    private final IntConsumer creditSender;
    private final long declaredLength;
    private Frame current;
    private int offset;
    private int expectedSequence;
    private long acceptedBytes;
    private boolean terminalFrameQueued;
    private boolean closed;

    OpaqueHttpRequestBodyInputStream(long declaredLength, IntConsumer creditSender) {
        this.declaredLength = declaredLength;
        this.creditSender = Objects.requireNonNull(creditSender, "creditSender");
    }

    synchronized void accept(int sequence, byte[] bytes) {
        if (terminalFrameQueued || closed) {
            throw new IllegalStateException("Opaque request body is already complete");
        }
        if (sequence != expectedSequence) {
            throw new IllegalArgumentException(
                    "Unexpected opaque request chunk sequence " + sequence
                            + ", expected " + expectedSequence);
        }
        byte[] copy = Objects.requireNonNull(bytes, "bytes").clone();
        if (!frames.offer(new Frame(copy, false, false))) {
            throw new IllegalStateException("Opaque request exceeded its granted chunk credit");
        }
        expectedSequence++;
        acceptedBytes += copy.length;
        if (declaredLength >= 0 && acceptedBytes > declaredLength) {
            throw new IllegalArgumentException("Opaque request body exceeds its declared length");
        }
    }

    synchronized void finish(int chunkCount, long byteCount) {
        if (terminalFrameQueued || closed) {
            return;
        }
        if (chunkCount != expectedSequence || byteCount != acceptedBytes) {
            throw new IllegalArgumentException("Opaque request body completion metadata does not match received chunks");
        }
        if (declaredLength >= 0 && byteCount != declaredLength) {
            throw new IllegalArgumentException("Opaque request body length does not match its declaration");
        }
        terminalFrameQueued = true;
        if (!frames.offer(END)) {
            throw new IllegalStateException("Unable to queue opaque request completion");
        }
    }

    synchronized void cancel() {
        if (closed) {
            return;
        }
        terminalFrameQueued = true;
        frames.clear();
        frames.offer(CANCEL);
    }

    @Override
    public int read() throws IOException {
        byte[] single = new byte[1];
        int read = read(single, 0, 1);
        return read < 0 ? -1 : single[0] & 0xff;
    }

    @Override
    public int read(byte[] bytes, int targetOffset, int length) throws IOException {
        Objects.checkFromIndexSize(targetOffset, length, bytes.length);
        if (length == 0) {
            return 0;
        }
        if (current != null && current.end) {
            return -1;
        }
        while (current == null || offset == current.bytes.length) {
            if (current != null && !current.end && !current.cancel) {
                creditSender.accept(1);
            }
            current = takeFrame();
            offset = 0;
            if (current.end) {
                return -1;
            }
            if (current.cancel) {
                throw new IOException("Opaque request was cancelled");
            }
        }
        int copied = Math.min(length, current.bytes.length - offset);
        System.arraycopy(current.bytes, offset, bytes, targetOffset, copied);
        offset += copied;
        return copied;
    }

    @Override
    public synchronized void close() {
        closed = true;
        frames.clear();
        frames.offer(CANCEL);
    }

    private Frame takeFrame() throws IOException {
        try {
            return frames.take();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while reading opaque request body", error);
        }
    }

    private record Frame(byte[] bytes, boolean end, boolean cancel) {
        private Frame {
            bytes = Arrays.copyOf(bytes, bytes.length);
        }
    }
}
