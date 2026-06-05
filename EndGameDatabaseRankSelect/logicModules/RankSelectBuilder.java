package EndGameDatabaseRankSelect.logicModules;

import EndGameDatabase.dataClasses.AtomicPackedBitArray;
import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.logicModules.Indexing;
import EndGameDatabaseRankSelect.dataClasses.RankSelectTable;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.stream.LongStream;

/**
 * Converts a flat AtomicPackedBitArray (the output of retrograde analysis) into
 * a RankSelectTable containing the three compact structures described in the thesis:
 *   - bit array  (canonical flags)
 *   - rank index (sampled prefix-sum)
 *   - outcomes   (2-bit compact array, canonical positions only)
 */
public class RankSelectBuilder {

    public static RankSelectTable build(
        AtomicPackedBitArray flatResults,
        long totalFlatPositions,
        int p1Count, int p2Count,
        BinomialTable table
    ) {
        System.err.println("[RankSelect] Building " + p1Count + "v" + p2Count
            + "  (" + String.format("%,d", totalFlatPositions) + " flat positions)...");

        // ── Step 1: Build bit array (parallel) ───────────────────────────────
        int numWords = (int)((totalFlatPositions + 63) >>> 6);
        AtomicLongArray atomicBits     = new AtomicLongArray(numWords);
        AtomicLong      numCanonicalAL = new AtomicLong(0);

        LongStream.range(0, totalFlatPositions).parallel().forEach(i -> {
            int[] board  = decodeBoard(i, p1Count, p2Count, table);
            long  minIdx = canonicalFlatIndex(board, table);
            if (minIdx == i) {
                int  wordIdx = (int)(i >>> 6);
                long bit     = 1L << (int)(i & 63);
                // Atomic OR via CAS spin
                long prev, next;
                do {
                    prev = atomicBits.get(wordIdx);
                    next = prev | bit;
                } while (!atomicBits.compareAndSet(wordIdx, prev, next));
                numCanonicalAL.incrementAndGet();
            }
        });

        long   numCanonical = numCanonicalAL.get();
        long[] bitArray     = new long[numWords];
        for (int w = 0; w < numWords; w++) bitArray[w] = atomicBits.get(w);

        System.err.println("[RankSelect] Canonical positions: " + String.format("%,d", numCanonical)
            + "  (" + String.format("%.2f", 100.0 * numCanonical / totalFlatPositions) + "%)");

        // ── Step 2: Build rank index ──────────────────────────────────────────
        int numBlocks = (int)((totalFlatPositions + RankSelectTable.BLOCK_SIZE - 1) / RankSelectTable.BLOCK_SIZE);
        int[] rankIndex  = new int[numBlocks];
        long  cumulative = 0;
        for (int b = 0; b < numBlocks; b++) {
            rankIndex[b] = (int) cumulative;
            long blockStart = (long) b * RankSelectTable.BLOCK_SIZE;
            long blockEnd   = Math.min(blockStart + RankSelectTable.BLOCK_SIZE, totalFlatPositions);
            cumulative += countBitsInRange(bitArray, blockStart, blockEnd);
        }

        // ── Step 3: Build compact outcomes (sequential, preserves flat-index order) ──
        byte[] outcomes = new byte[(int)((numCanonical + 3) >>> 2)];
        long   compactIdx = 0;
        for (long i = 0; i < totalFlatPositions; i++) {
            if ((bitArray[(int)(i >>> 6)] & (1L << (int)(i & 63))) != 0) {
                int outcome = flatResults.get(i) & 0b11;
                int shift   = (3 - (int)(compactIdx & 3)) << 1;
                outcomes[(int)(compactIdx >>> 2)] |= (byte)(outcome << shift);
                compactIdx++;
            }
        }

        System.err.println("[RankSelect] Build complete.");
        return new RankSelectTable(totalFlatPositions, numCanonical, bitArray, rankIndex, outcomes);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns the canonical flat index for a board: the minimum flat index among
     * all 8 D4 symmetry transforms (geometric only — player assignments are preserved).
     */
    static long canonicalFlatIndex(int[] board, BinomialTable table) {
        long[] idxs = Indexing.getAllTransformIndexes(board, 6, false, table);
        long min = idxs[0];
        for (int t = 1; t < idxs.length; t++) if (idxs[t] < min) min = idxs[t];
        return min;
    }

    private static int[] decodeBoard(long flatIdx, int p1Count, int p2Count, BinomialTable table) {
        return Indexing.filePositionToIndexingInput(flatIdx, 36, p1Count, p2Count, table)
                       .getMapFromIndexingInput(36);
    }

    /** Counts set bits in bitArray for flat positions [from, to). */
    static long countBitsInRange(long[] bitArray, long from, long to) {
        if (from >= to) return 0;
        long count   = 0;
        int fromWord = (int)(from >>> 6);
        int toWord   = (int)((to - 1) >>> 6);

        if (fromWord == toWord) {
            int  fromBit = (int)(from & 63);
            int  toBit   = (int)((to - 1) & 63);
            long mask    = (-1L << fromBit) & (-1L >>> (63 - toBit));
            return Long.bitCount(bitArray[fromWord] & mask);
        }
        count += Long.bitCount(bitArray[fromWord] & (-1L << (int)(from & 63)));
        for (int w = fromWord + 1; w < toWord; w++) count += Long.bitCount(bitArray[w]);
        count += Long.bitCount(bitArray[toWord] & (-1L >>> (63 - (int)((to - 1) & 63))));
        return count;
    }
}
