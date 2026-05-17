package EndGameDatabase.dataClasses;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;

/**
 * Thread-safe 2-bit-per-entry array backed by a chunked byte[].
 *
 * get()  — volatile byte read + bit extract
 * set()  — CAS loop on the containing byte (4 positions share one byte)
 * fill() — bulk Arrays.fill on every chunk; use for bulk initialization
 *
 * Bit layout matches the disk format in FileReaderWriter:
 *   position i → byte[i/4], shift = (3 - i%4) * 2
 */
public class AtomicPackedBitArray implements TableReader {

    private static final VarHandle BYTE_HANDLE =
        MethodHandles.arrayElementVarHandle(byte[].class);

    // 64 MB per chunk → 256 M positions per chunk
    static final int CHUNK_SIZE_BYTES = 64 * 1024 * 1024;

    final byte[][] chunks;
    private final long totalPositions;

    public AtomicPackedBitArray(long totalPositions) {
        if (totalPositions <= 0)
            throw new IllegalArgumentException("Size must be positive");

        this.totalPositions = totalPositions;
        long totalBytes = (totalPositions + 3) >>> 2;
        int numChunks = (int) ((totalBytes + CHUNK_SIZE_BYTES - 1) / CHUNK_SIZE_BYTES);
        this.chunks = new byte[numChunks][];
        for (int i = 0; i < numChunks; i++) {
            long remaining = totalBytes - (long) i * CHUNK_SIZE_BYTES;
            chunks[i] = new byte[(int) Math.min(CHUNK_SIZE_BYTES, remaining)];
        }
    }

    @Override
    public int get(long index) {
        long byteIdx = index >>> 2;
        int shift    = (3 - (int) (index & 3)) * 2;
        int chunkIdx = (int) (byteIdx / CHUNK_SIZE_BYTES);
        int offset   = (int) (byteIdx % CHUNK_SIZE_BYTES);
        byte b = (byte) BYTE_HANDLE.getVolatile(chunks[chunkIdx], offset);
        return (b >> shift) & 0b11;
    }

    public void set(long index, int value) {
        long byteIdx = index >>> 2;
        int shift    = (3 - (int) (index & 3)) * 2;
        int mask     = ~(0b11 << shift);
        int shifted  = (value & 0b11) << shift;
        int chunkIdx = (int) (byteIdx / CHUNK_SIZE_BYTES);
        int offset   = (int) (byteIdx % CHUNK_SIZE_BYTES);
        byte[] chunk = chunks[chunkIdx];
        byte cur, upd;
        do {
            cur = (byte) BYTE_HANDLE.getVolatile(chunk, offset);
            upd = (byte) ((cur & mask) | shifted);
        } while (!BYTE_HANDLE.compareAndSet(chunk, offset, cur, upd));
    }

    /** Sets every entry to value. Much faster than calling set() in a loop. */
    public void fill(int value) {
        int v = value & 0b11;
        byte fillByte = (byte) ((v << 6) | (v << 4) | (v << 2) | v);
        for (byte[] chunk : chunks) {
            Arrays.fill(chunk, fillByte);
        }
    }

    @Override
    public long size() {
        return totalPositions;
    }

    public long sizeInBytes() {
        return (totalPositions + 3) >>> 2;
    }

    public int numChunks() {
        return chunks.length;
    }

    public byte[] getChunk(int i) {
        return chunks[i];
    }
}
