package EndGameDatabaseRankSelect.dataClasses;

/**
 * Rank-Select Canonical Index for a single Surakarta endgame table.
 *
 * Three coordinated structures:
 *
 *   bitArray   — 1 bit per flat index; set ↔ this index is the D4-canonical representative
 *                of its symmetry orbit (i.e. the minimum flat index among all 8 transforms).
 *
 *   rankIndex  — sampled prefix-sum over the bit array.  Entry b holds the total count of
 *                canonical positions in flat indices [0, b × BLOCK_SIZE).  Stored as int
 *                (32-bit) so tables up to ~2 billion canonical positions are supported.
 *
 *   outcomes   — 2-bit outcomes (WIN=1, LOSS=0, DRAW=2) packed 4-per-byte in the same
 *                MSB-first layout as AtomicPackedBitArray, one entry per canonical position
 *                in ascending flat-index order.
 *
 * Lookup:
 *   1. Compute canonical flat index c = min(getAllTransformIndexes(board))
 *   2. rank(c)  = rankIndex[c / BLOCK_SIZE]
 *              + popcount of bitArray bits in [c / BLOCK_SIZE × BLOCK_SIZE, c)
 *   3. outcome  = outcomes[rank(c) / 4] >> ((3 - rank(c) % 4) * 2) & 0b11
 */
public class RankSelectTable {

    /** Each block spans this many consecutive flat indices. */
    public static final int BLOCK_SIZE = 1 << 16; // 65 536

    private final long   totalFlatPositions;
    private final long   numCanonical;
    private final long[] bitArray;   // 1 bit per flat position, packed in longs
    private final int[]  rankIndex;  // cumulative canonical count at start of each block
    private final byte[] outcomes;   // 2 bits per canonical position

    public RankSelectTable(long totalFlatPositions, long numCanonical,
                           long[] bitArray, int[] rankIndex, byte[] outcomes) {
        this.totalFlatPositions = totalFlatPositions;
        this.numCanonical       = numCanonical;
        this.bitArray           = bitArray;
        this.rankIndex          = rankIndex;
        this.outcomes           = outcomes;
    }

    // ── Core queries ─────────────────────────────────────────────────────────

    /** True iff flatIndex is the canonical representative of its orbit. */
    public boolean isCanonical(long flatIndex) {
        return (bitArray[(int)(flatIndex >>> 6)] & (1L << (int)(flatIndex & 63))) != 0;
    }

    /**
     * Returns the 0-based compact index of flatIndex in the outcomes array.
     * Equal to the number of canonical positions in [0, flatIndex).
     * flatIndex need not be canonical — this gives its "rank" regardless.
     */
    public long rank(long flatIndex) {
        int  blockIdx  = (int)(flatIndex / BLOCK_SIZE);
        long blockStart = (long) blockIdx * BLOCK_SIZE;
        return Integer.toUnsignedLong(rankIndex[blockIdx])
             + countBitsInRange(blockStart, flatIndex);
    }

    /**
     * Returns the 2-bit outcome stored at compact position compactIndex.
     * WIN=1, LOSS=0, DRAW=2.
     */
    public int getOutcome(long compactIndex) {
        int shift = (3 - (int)(compactIndex & 3)) << 1;
        return (outcomes[(int)(compactIndex >>> 2)] >> shift) & 0b11;
    }

    /**
     * Looks up the outcome for a canonical flat index.
     * Caller must ensure isCanonical(flatIndex) == true.
     */
    public int lookup(long flatIndex) {
        return getOutcome(rank(flatIndex));
    }

    // ── Bit-counting helper (package-visible for builder) ────────────────────

    /**
     * Counts the number of set bits in bitArray at positions [from, to).
     */
    long countBitsInRange(long from, long to) {
        if (from >= to) return 0;
        long count   = 0;
        int fromWord = (int)(from >>> 6);
        int toWord   = (int)((to - 1) >>> 6);

        if (fromWord == toWord) {
            int fromBit = (int)(from & 63);
            int toBit   = (int)((to  - 1) & 63);
            long mask = (-1L << fromBit) & (-1L >>> (63 - toBit));
            return Long.bitCount(bitArray[fromWord] & mask);
        }
        // Partial first word
        count += Long.bitCount(bitArray[fromWord] & (-1L << (int)(from & 63)));
        // Full middle words
        for (int w = fromWord + 1; w < toWord; w++) {
            count += Long.bitCount(bitArray[w]);
        }
        // Partial last word
        count += Long.bitCount(bitArray[toWord] & (-1L >>> (63 - (int)((to - 1) & 63))));
        return count;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public long   getTotalFlatPositions() { return totalFlatPositions; }
    public long   getNumCanonical()       { return numCanonical; }
    public long[] getBitArray()           { return bitArray; }
    public int[]  getRankIndex()          { return rankIndex; }
    public byte[] getOutcomes()           { return outcomes; }
}
