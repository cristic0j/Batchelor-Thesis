package EndGameDatabase.logicModules.RandomAccessFile;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import EndGameDatabase.dataClasses.AtomicByteArray;

public class FileReaderWriter {

    /**
     * Function to write an AtomicByteArray to a RandomAccessFile.
     * Packs four 2-bit values (0-3) into a single byte.
     */
    private static final int WRITE_CHUNK_BYTES = 1 << 22; // 4 MB chunks

    public static void writeToFile(String path, AtomicByteArray data, int size) throws IOException {

        File file = new File(path);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (RandomAccessFile raf = new RandomAccessFile(path, "rw")) {
            raf.setLength(0);

            int totalBytes = (size + 3) / 4;
            byte[] buffer = new byte[Math.min(WRITE_CHUNK_BYTES, totalBytes)];
            int bufIdx = 0;

            for (int i = 0; i < size; i++) {
                int val = data.get(i) & 0b11;
                int shift = (3 - (i % 4)) * 2;
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

    public static AtomicByteArray readFromFile(String path, int totalValues) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            throw new IOException("File not found: " + path);
        }

        int bytesToRead = (totalValues + 3) / 4;
        byte[] buffer = new byte[bytesToRead];

        try (RandomAccessFile raf = new RandomAccessFile(path, "r")) {
            raf.readFully(buffer);
        }

        AtomicByteArray result = new AtomicByteArray(totalValues);
        for (int i = 0; i < totalValues; i++) {
            int byteIdx = i / 4;
            int shift = (3 - (i % 4)) * 2;
            
            // Extract the 2 bits and store in the atomic array
            int val = (buffer[byteIdx] >> shift) & 0b11;
            result.set(i, val);
        }
        
        return result;
    }

    /**
     * Function to read a specific 2-bit value from a file.
     */
    public static int readValueFromFile(String path, int index) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(path, "r")) {
            long bytePos = index / 4;
            if (bytePos >= raf.length()) return -1; // Out of bounds

            raf.seek(bytePos);
            int b = raf.read();
            int shift = (3 - (index % 4)) * 2;
            
            return (b >> shift) & 0b11;
        }
    }
}