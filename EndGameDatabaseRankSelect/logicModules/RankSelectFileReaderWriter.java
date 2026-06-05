package EndGameDatabaseRankSelect.logicModules;

import EndGameDatabaseRankSelect.dataClasses.RankSelectTable;

import java.io.*;

/**
 * Reads and writes RankSelectTable objects to disk.
 *
 * File layout (big-endian binary):
 *   8  bytes  — totalFlatPositions  (long)
 *   8  bytes  — numCanonical        (long)
 *   8 × numWords bytes  — bit array (one long per word)
 *   4 × numBlocks bytes — rank index (one int per block)
 *   numOutcomeBytes     — compact outcomes (2 bits per canonical position)
 *
 * numWords  = ceil(totalFlatPositions / 64)
 * numBlocks = ceil(totalFlatPositions / 65536)
 * numOutcomeBytes = ceil(numCanonical / 4)
 */
public class RankSelectFileReaderWriter {

    public static void write(String path, RankSelectTable t) throws IOException {
        File f = new File(path);
        if (f.getParentFile() != null) f.getParentFile().mkdirs();

        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(path)))) {
            out.writeLong(t.getTotalFlatPositions());
            out.writeLong(t.getNumCanonical());

            for (long w : t.getBitArray())   out.writeLong(w);
            for (int  r : t.getRankIndex())  out.writeInt(r);
            for (byte b : t.getOutcomes())   out.write(b & 0xFF);
        }
    }

    public static RankSelectTable read(String path) throws IOException {
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(path)))) {

            long totalFlatPositions = in.readLong();
            long numCanonical       = in.readLong();

            int numWords        = (int)((totalFlatPositions + 63) >>> 6);
            int numBlocks       = (int)((totalFlatPositions + RankSelectTable.BLOCK_SIZE - 1)
                                        / RankSelectTable.BLOCK_SIZE);
            int numOutcomeBytes = (int)((numCanonical + 3) >>> 2);

            long[] bitArray  = new long[numWords];
            int[]  rankIndex = new int[numBlocks];
            byte[] outcomes  = new byte[numOutcomeBytes];

            for (int w = 0; w < numWords;        w++) bitArray[w]  = in.readLong();
            for (int b = 0; b < numBlocks;        b++) rankIndex[b] = in.readInt();
            for (int o = 0; o < numOutcomeBytes;  o++) outcomes[o]  = (byte) in.read();

            return new RankSelectTable(totalFlatPositions, numCanonical, bitArray, rankIndex, outcomes);
        }
    }

    /** Returns the expected file size in bytes for the given parameters. */
    public static long expectedFileSize(long totalFlatPositions, long numCanonical) {
        int numWords  = (int)((totalFlatPositions + 63) >>> 6);
        int numBlocks = (int)((totalFlatPositions + RankSelectTable.BLOCK_SIZE - 1) / RankSelectTable.BLOCK_SIZE);
        int numOutcomeBytes = (int)((numCanonical + 3) >>> 2);
        return 8L + 8L + (long) numWords * 8 + (long) numBlocks * 4 + numOutcomeBytes;
    }
}
