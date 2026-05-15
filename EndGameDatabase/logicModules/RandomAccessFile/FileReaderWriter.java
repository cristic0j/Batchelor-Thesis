package EndGameDatabase.logicModules.RandomAccessFile;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import EndGameDatabase.dataClasses.AtomicHugeByteArray;
import EndGameDatabase.dataClasses.TableReader;

public class FileReaderWriter {

    /**
     * Function to write an AtomicHugeByteArray to a RandomAccessFile.
     * Packs four 2-bit values (0-3) into a single byte.
     */
    private static final int WRITE_CHUNK_BYTES = 1 << 22; // 4 MB chunks

    public static void writeToFile(String path, AtomicHugeByteArray data, long size) throws IOException {

        File file = new File(path);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (RandomAccessFile raf = new RandomAccessFile(path, "rw")) {
            raf.setLength(0);

            long totalBytes = (size + 3) / 4;
            byte[] buffer = new byte[(int) Math.min(WRITE_CHUNK_BYTES, totalBytes)];
            int bufIdx = 0;

            for (long i = 0; i < size; i++) {
                int val = data.get(i) & 0b11;
                int shift = (3 - (int)(i % 4)) * 2;
                buffer[bufIdx] |= (byte) (val << shift);

                if (i % 4 == 3) { // finished packing this byte
                    bufIdx++;
                    if (bufIdx == buffer.length) {
                        raf.write(buffer, 0, bufIdx);
                        bufIdx = 0;
                        java.util.Arrays.fill(buffer, (byte) 0);
                    }
                }
            }
            // flush remaining partial byte (if size % 4 != 0)
            if (size % 4 != 0) bufIdx++;
            if (bufIdx > 0) raf.write(buffer, 0, bufIdx);
        }
    }

    public static AtomicHugeByteArray readFromFile(String path, long totalValues) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            throw new IOException("File not found: " + path);
        }

        long bytesToRead = (totalValues + 3) / 4;
        AtomicHugeByteArray result = new AtomicHugeByteArray(totalValues);
        byte[] buffer = new byte[(int) Math.min(WRITE_CHUNK_BYTES, bytesToRead)];

        try (RandomAccessFile raf = new RandomAccessFile(path, "r")) {
            long bytePos = 0;
            while (bytePos < bytesToRead) {
                int toRead = (int) Math.min(buffer.length, bytesToRead - bytePos);
                raf.readFully(buffer, 0, toRead);
                for (int b = 0; b < toRead; b++) {
                    long baseIdx = (bytePos + b) * 4;
                    for (int bit = 0; bit < 4; bit++) {
                        long idx = baseIdx + bit;
                        if (idx >= totalValues) break;
                        int shift = (3 - bit) * 2;
                        int val = (buffer[b] >> shift) & 0b11;
                        result.set(idx, val);
                    }
                }
                bytePos += toRead;
            }
        }

        return result;
    }

    /**
     * Opens a file for direct random-access reads without loading it into RAM.
     * The returned OpenReader keeps a FileChannel open — call close() when done.
     */
    public static OpenReader openReader(String path, long totalValues) throws IOException {
        return new OpenReader(path, totalValues);
    }

    /**
     * A TableReader backed by an open FileChannel.
     * FileChannel.read(ByteBuffer, long) is thread-safe for positional reads,
     * so this can be shared across a parallel LongStream without synchronization.
     * Each thread gets its own ByteBuffer via ThreadLocal to avoid buffer sharing.
     */
    public static class OpenReader implements TableReader, AutoCloseable {

        private final FileChannel channel;
        private final long totalValues;
        private final ThreadLocal<ByteBuffer> tlBuffer =
            ThreadLocal.withInitial(() -> ByteBuffer.allocate(1));

        private OpenReader(String path, long totalValues) throws IOException {
            this.channel = FileChannel.open(Paths.get(path), StandardOpenOption.READ);
            this.totalValues = totalValues;
        }

        @Override
        public int get(long index) {
            if (index < 0 || index >= totalValues)
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + totalValues);

            long bytePos = index / 4;
            int shift = (3 - (int)(index % 4)) * 2;

            ByteBuffer buf = tlBuffer.get();
            buf.clear();
            try {
                channel.read(buf, bytePos);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return (buf.get(0) >> shift) & 0b11;
        }

        @Override
        public long size() {
            return totalValues;
        }

        @Override
        public void close() throws IOException {
            channel.close();
        }
    }

    /**
     * Reads a packed file into an existing AtomicHugeByteArray without allocating a new one.
     * Used for checkpoint resume.
     */
    public static void readIntoArray(String path, AtomicHugeByteArray dest) throws IOException {
        long totalValues = dest.size();
        long bytesToRead = (totalValues + 3) / 4;
        byte[] buffer = new byte[(int) Math.min(WRITE_CHUNK_BYTES, bytesToRead)];

        try (RandomAccessFile raf = new RandomAccessFile(path, "r")) {
            long bytePos = 0;
            while (bytePos < bytesToRead) {
                int toRead = (int) Math.min(buffer.length, bytesToRead - bytePos);
                raf.readFully(buffer, 0, toRead);
                for (int b = 0; b < toRead; b++) {
                    long baseIdx = (bytePos + b) * 4;
                    for (int bit = 0; bit < 4; bit++) {
                        long idx = baseIdx + bit;
                        if (idx >= totalValues) break;
                        int shift = (3 - bit) * 2;
                        int val = (buffer[b] >> shift) & 0b11;
                        dest.set(idx, val);
                    }
                }
                bytePos += toRead;
            }
        }
    }

    /**
     * Creates a file of the correct packed size, all values set to fillValue.
     * chunkSize must be a multiple of 4 so that startPos is always byte-aligned.
     */
    public static void createPrefilled(String path, long totalValues, int fillValue) throws IOException {
        File file = new File(path);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) parentDir.mkdirs();

        long totalBytes = (totalValues + 3) / 4;
        int v = fillValue & 0b11;
        byte fillByte = (byte) ((v << 6) | (v << 4) | (v << 2) | v);
        byte[] buffer = new byte[(int) Math.min(WRITE_CHUNK_BYTES, totalBytes)];
        java.util.Arrays.fill(buffer, fillByte);

        try (RandomAccessFile raf = new RandomAccessFile(path, "rw")) {
            raf.setLength(0);
            long written = 0;
            while (written < totalBytes) {
                int toWrite = (int) Math.min(buffer.length, totalBytes - written);
                raf.write(buffer, 0, toWrite);
                written += toWrite;
            }
        }
    }

    /**
     * Reads a window of `count` positions starting at `startPos` from a packed file into dest.
     * startPos must be a multiple of 4 for byte alignment.
     */
    public static void readWindow(String path, long startPos, long count, AtomicHugeByteArray dest) throws IOException {
        long startByte = startPos / 4;
        long byteCount = (count + 3) / 4;
        byte[] buffer = new byte[(int) Math.min(WRITE_CHUNK_BYTES, byteCount)];

        try (RandomAccessFile raf = new RandomAccessFile(path, "r")) {
            raf.seek(startByte);
            long bytePos = 0;
            while (bytePos < byteCount) {
                int toRead = (int) Math.min(buffer.length, byteCount - bytePos);
                raf.readFully(buffer, 0, toRead);
                for (int b = 0; b < toRead; b++) {
                    long baseIdx = (bytePos + b) * 4;
                    for (int bit = 0; bit < 4; bit++) {
                        long idx = baseIdx + bit;
                        if (idx >= count) break;
                        int shift = (3 - bit) * 2;
                        int val = (buffer[b] >> shift) & 0b11;
                        dest.set(idx, val);
                    }
                }
                bytePos += toRead;
            }
        }
    }

    /**
     * Writes a window of `count` positions from src back into the packed file at startPos.
     * startPos must be a multiple of 4 for byte alignment.
     */
    public static void writeWindow(String path, long startPos, long count, AtomicHugeByteArray src) throws IOException {
        long startByte = startPos / 4;
        long totalBytes = (count + 3) / 4;
        byte[] buffer = new byte[(int) Math.min(WRITE_CHUNK_BYTES, totalBytes)];
        int bufIdx = 0;

        try (RandomAccessFile raf = new RandomAccessFile(path, "rw")) {
            raf.seek(startByte);
            for (long i = 0; i < count; i++) {
                int val = src.get(i) & 0b11;
                int shift = (3 - (int)(i % 4)) * 2;
                buffer[bufIdx] |= (byte)(val << shift);

                if (i % 4 == 3) {
                    bufIdx++;
                    if (bufIdx == buffer.length) {
                        raf.write(buffer, 0, bufIdx);
                        bufIdx = 0;
                        java.util.Arrays.fill(buffer, (byte) 0);
                    }
                }
            }
            if (count % 4 != 0) bufIdx++;
            if (bufIdx > 0) raf.write(buffer, 0, bufIdx);
        }
    }

    /**
     * Function to read a specific 2-bit value from a file.
     */
    public static int readValueFromFile(String path, long index) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(path, "r")) {
            long bytePos = index / 4;
            if (bytePos >= raf.length()) return -1; // Out of bounds

            raf.seek(bytePos);
            int b = raf.read();
            int shift = (3 - (int)(index % 4)) * 2;

            return (b >> shift) & 0b11;
        }
    }
}
