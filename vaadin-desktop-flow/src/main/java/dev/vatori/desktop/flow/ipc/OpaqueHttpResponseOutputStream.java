package dev.vatori.desktop.flow.ipc;

import dev.vatori.desktop.flow.transport.DesktopFlowHttpResponseHead;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Semaphore;

final class OpaqueHttpResponseOutputStream extends OutputStream {
    static final int CHUNK_SIZE = 32 * 1024;
    static final int MAX_CREDIT_GRANT = 16;

    private final ResponseSender sender;
    private final Semaphore credits = new Semaphore(0);
    private final byte[] buffer = new byte[CHUNK_SIZE];
    private int buffered;
    private int sequence;
    private long totalBytes;
    private boolean started;
    private boolean finished;
    private volatile boolean cancelled;

    OpaqueHttpResponseOutputStream(ResponseSender sender) {
        this.sender = Objects.requireNonNull(sender, "sender");
    }

    synchronized void start(DesktopFlowHttpResponseHead head) {
        if (started) {
            throw new IllegalStateException("Opaque response metadata was already sent");
        }
        started = true;
        sender.start(head);
    }

    void grantCredits(int credit) {
        if (credit <= 0 || credit > MAX_CREDIT_GRANT) {
            throw new IllegalArgumentException("Opaque response credit must be between 1 and "
                    + MAX_CREDIT_GRANT);
        }
        credits.release(credit);
    }

    void cancel() {
        cancelled = true;
        credits.release(MAX_CREDIT_GRANT);
    }

    synchronized boolean hasStarted() {
        return started;
    }

    boolean isCancelled() {
        return cancelled;
    }

    @Override
    public synchronized void write(int value) throws IOException {
        ensureWritable();
        buffer[buffered++] = (byte) value;
        if (buffered == buffer.length) {
            sendBufferedChunk();
        }
    }

    @Override
    public synchronized void write(byte[] bytes, int offset, int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, bytes.length);
        ensureWritable();
        int sourceOffset = offset;
        int remaining = length;
        while (remaining > 0) {
            int copied = Math.min(remaining, buffer.length - buffered);
            System.arraycopy(bytes, sourceOffset, buffer, buffered, copied);
            buffered += copied;
            sourceOffset += copied;
            remaining -= copied;
            if (buffered == buffer.length) {
                sendBufferedChunk();
            }
        }
    }

    @Override
    public synchronized void flush() throws IOException {
        ensureWritable();
        sendBufferedChunk();
    }

    synchronized void finish() throws IOException {
        if (finished) {
            return;
        }
        ensureWritable();
        if (!started) {
            throw new IOException("Opaque response completed before sending metadata");
        }
        sendBufferedChunk();
        finished = true;
        sender.end(sequence, totalBytes);
    }

    private void sendBufferedChunk() throws IOException {
        if (buffered == 0) {
            return;
        }
        awaitCredit();
        byte[] bytes = Arrays.copyOf(buffer, buffered);
        sender.chunk(sequence++, bytes);
        totalBytes += bytes.length;
        buffered = 0;
    }

    private void awaitCredit() throws IOException {
        try {
            credits.acquire();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for opaque response credit", error);
        }
        if (cancelled) {
            throw new IOException("Opaque response was cancelled");
        }
    }

    private void ensureWritable() throws IOException {
        if (finished) {
            throw new IOException("Opaque response is already complete");
        }
        if (cancelled) {
            throw new IOException("Opaque response was cancelled");
        }
    }

    interface ResponseSender {
        void start(DesktopFlowHttpResponseHead head);

        void chunk(int sequence, byte[] bytes);

        void end(int chunkCount, long byteCount);
    }
}
