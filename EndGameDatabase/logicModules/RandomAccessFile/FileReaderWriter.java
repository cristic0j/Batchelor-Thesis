package EndGameDatabase.logicModules.RandomAccessFile;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class FileReaderWriter {

    /**
     * Function to write an AtomicIntegerArray to a RandomAccessFile.
     * Packs four 2-bit values (0-3) into a single byte.
     */
    public static void writeToFile(String path, AtomicIntegerArray data, int size) throws IOException {

        File file = new File(path);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs(); 
        }
        
        int bytesNeeded = (size + 3) / 4;
        byte[] buffer = new byte[bytesNeeded];

        for (int i = 0; i < size; i++) {
            int val = data.get(i) & 0b11; // Ensure only 2 bits are taken
            int byteIdx = i / 4;
            int shift = (3 - (i % 4)) * 2;
            
            // Logic: OR the shifted value into the specific byte slot
            buffer[byteIdx] |= (byte) (val << shift);
        }

        try (RandomAccessFile raf = new RandomAccessFile(path, "rw")) {
            raf.setLength(0); // Wipe existing file
            raf.write(buffer);
        }
    }

    public static AtomicIntegerArray readFromFile(String path, int totalValues) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            throw new IOException("File not found: " + path);
        }

        int bytesToRead = (totalValues + 3) / 4;
        byte[] buffer = new byte[bytesToRead];

        try (RandomAccessFile raf = new RandomAccessFile(path, "r")) {
            raf.readFully(buffer);
        }

        AtomicIntegerArray result = new AtomicIntegerArray(totalValues);
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