package EndGameDatabase.logicModules.RandomAccessFile;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import EndGameDatabase.dataClasses.AtomicHugeByteArray;

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
