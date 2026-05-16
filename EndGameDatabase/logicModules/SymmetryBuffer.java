package EndGameDatabase.logicModules;

import EndGameDatabase.logicModules.RandomAccessFile.FileReaderWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe fixed-capacity buffer for out-of-window symmetry writes.
 *
 * Each entry packs a long index (bits 63..2) and a 2-bit outcome value (bits 1..0)
 * into a single long, so Arrays.sort naturally orders by index for writeScattered.
 *
 * Flushes to disk when the buffer reaches capacity (inside add()) or when flush()
 * is called explicitly by the main thread after each window.
 */
public class SymmetryBuffer {

    private final long[] packed;
    private int count;
    private final int capacity;
    private final ReentrantLock lock = new ReentrantLock();

    public SymmetryBuffer(int capacity) {
        this.capacity = capacity;
        this.packed = new long[capacity];
        this.count = 0;
    }

    /**
     * Add an out-of-window symmetry entry. Safe to call concurrently from parallel streams.
     * If the buffer reaches capacity, flushes immediately to disk.
     */
    public void add(long index, byte value, String filePath) {
        lock.lock();
        try {
            packed[count++] = (index << 2) | (value & 0b11L);
            if (count == capacity) {
                flushLocked(filePath);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Flush all remaining buffered entries to disk.
     * Called by the main thread after each window write — workers are idle at this point.
     */
    public void flush(String filePath) throws IOException {
        lock.lock();
        try {
            flushLocked(filePath);
        } finally {
            lock.unlock();
        }
    }

    private void flushLocked(String filePath) throws IOException {
        if (count == 0) return;
        Arrays.sort(packed, 0, count);
        List<Map.Entry<Long, Integer>> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            long idx = packed[i] >>> 2;
            int  val = (int)(packed[i] & 0b11L);
            entries.add(Map.entry(idx, val));
        }
        FileReaderWriter.writeScattered(filePath, entries);
        count = 0;
    }

    public int size() {
        lock.lock();
        try { return count; } finally { lock.unlock(); }
    }
}
