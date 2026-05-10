package EndGameDatabase.dataClasses;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class AtomicHugeByteArray {

    private static final VarHandle BYTE_HANDLE =
        MethodHandles.arrayElementVarHandle(byte[].class);

    private final byte[][] chunks;        // Array of arrays (chunked)
    private final long totalSize;         // Total number of elements (long)
    private final int chunkSize;          // Elements per chunk (power of 2 recommended)

    public AtomicHugeByteArray(long totalElements) {
        if (totalElements <= 0)
            throw new IllegalArgumentException("Size must be positive");

        this.totalSize = totalElements;

        // 256 MB per chunk (good balance between number of chunks and cache)
        this.chunkSize = 256 * 1024 * 1024;   // 256 million elements per chunk

        int numChunks = (int) ((totalElements + chunkSize - 1) / chunkSize);

        if (numChunks > Integer.MAX_VALUE / 2) {
            throw new OutOfMemoryError("Array too large: too many chunks");
        }

        this.chunks = new byte[numChunks][];

        for (int i = 0; i < numChunks; i++) {
            long remaining = totalElements - (long) i * chunkSize;
            int currentChunkSize = (int) Math.min(chunkSize, remaining);
            chunks[i] = new byte[currentChunkSize];
        }
    }

    private int getChunkIndex(long index) {
        return (int) (index / chunkSize);
    }

    private int getOffsetInChunk(long index) {
        return (int) (index % chunkSize);
    }

    public int get(long index) {
        checkIndex(index);
        int chunkIdx = getChunkIndex(index);
        int offset = getOffsetInChunk(index);
        return (byte) BYTE_HANDLE.get(chunks[chunkIdx], offset) & 0xFF;
    }

    public void set(long index, int value) {
        checkIndex(index);
        if (value < 0 || value > 255)
            throw new IllegalArgumentException("Value must be 0-255");

        int chunkIdx = getChunkIndex(index);
        int offset = getOffsetInChunk(index);
        BYTE_HANDLE.set(chunks[chunkIdx], offset, (byte) value);
    }

    public int getAndSet(long index, int newValue) {
        checkIndex(index);
        if (newValue < 0 || newValue > 255)
            throw new IllegalArgumentException("Value must be 0-255");

        int chunkIdx = getChunkIndex(index);
        int offset = getOffsetInChunk(index);

        byte old = (byte) BYTE_HANDLE.getAndSet(chunks[chunkIdx], offset, (byte) newValue);
        return old & 0xFF;
    }

    public boolean compareAndSet(long index, int expected, int update) {
        checkIndex(index);
        if (expected < 0 || expected > 255 || update < 0 || update > 255)
            throw new IllegalArgumentException();

        int chunkIdx = getChunkIndex(index);
        int offset = getOffsetInChunk(index);

        return BYTE_HANDLE.compareAndSet(chunks[chunkIdx], offset,
                                        (byte) expected, (byte) update);
    }

    private void checkIndex(long index) {
        if (index < 0 || index >= totalSize) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + totalSize);
        }
    }

    public long size() {
        return totalSize;
    }

    // Memory usage in bytes
    public long memoryUsage() {
        return totalSize;   // ≈ 1 byte per element + small overhead
    }
}
