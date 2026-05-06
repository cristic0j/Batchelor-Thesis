package EndGameDatabase.logicModules;

import EndGameDatabase.dataClasses.BinomialTable;
import java.util.stream.IntStream;

public class BitMapGenerator {

    // 7 non-identity symmetry transforms on the 6x6 board.
    // PERMS[t][oldSquare] = newSquare after transform t.
    // Order: rot90, rot180, rot270, flipH, flipH+rot90, flipH+rot180, flipH+rot270.
    private static final int[][] PERMS = buildAllPerms();

    private static int[] buildRot90Perm() {
        int n = 6;
        int[] perm = new int[36];
        for (int r = 0; r < n; r++)
            for (int c = 0; c < n; c++)
                perm[r * n + c] = c * n + (n - 1 - r);
        return perm;
    }

    private static int[] buildFlipHPerm() {
        int n = 6;
        int[] perm = new int[36];
        for (int r = 0; r < n; r++)
            for (int c = 0; c < n; c++)
                perm[r * n + c] = r * n + (n - 1 - c);
        return perm;
    }

    // result[i] = b[a[i]]  (apply a first, then b)
    private static int[] composePerm(int[] a, int[] b) {
        int[] result = new int[36];
        for (int i = 0; i < 36; i++) result[i] = b[a[i]];
        return result;
    }

    private static int[][] buildAllPerms() {
        int[] rot90  = buildRot90Perm();
        int[] rot180 = composePerm(rot90, rot90);
        int[] rot270 = composePerm(rot180, rot90);
        int[] flipH  = buildFlipHPerm();
        return new int[][]{
            rot90, rot180, rot270, flipH,
            composePerm(flipH, rot90),
            composePerm(flipH, rot180),
            composePerm(flipH, rot270)
        };
    }

    /**
     * Per-thread scratch — allocated once per thread, reused across all iterations.
     * Eliminates all heap allocation inside the hot parallel loop.
     */
    private static final class Scratch {
        final int[] p1Pos, p2Pos;   // current canonical's piece positions
        final int[] tp1,  tp2;      // transformed positions (intermediate)
        final int[] combined;        // merge buffer for ranking
        final int[] relWhite;        // P1 relative positions buffer for ranking
        final int[] combinedUnrank;  // merge buffer for unranking
        final boolean[] isP1;        // P1 flag buffer for unranking (auto-reset after use)

        Scratch(int p1Count, int p2Count) {
            int M = p1Count + p2Count;
            p1Pos          = new int[p1Count];
            p2Pos          = new int[p2Count];
            tp1            = new int[p1Count];
            tp2            = new int[p2Count];
            combined       = new int[M];
            relWhite       = new int[p1Count];
            combinedUnrank = new int[M];
            isP1           = new boolean[M];
        }
    }

    /**
     * Generates a bitmap of canonical positions under the 8-element dihedral symmetry group.
     * bitMap[i] == true iff index i is the lexicographically smallest member of its orbit.
     *
     * Two-phase parallel approach (no shared mutable state in phase 1):
     *   Phase 1 (parallel): canonOf[i] = min index in the orbit of i.
     *   Phase 2 (sequential): bitMap[i] = (canonOf[i] == i).
     *
     * Each thread holds private Scratch arrays so there is zero heap allocation per iteration.
     */
    public static boolean[] generateBitMap(
        BinomialTable table,
        int p1PiecesNum,
        int p2PiecesNum,
        int boardSize
    ) {
        int N = (int) Indexing.computeTheNumberOfPossiblePositions(p1PiecesNum, p2PiecesNum, boardSize, table);
        boolean[] bitMap = new boolean[N];
        int[] canonOf    = new int[N];

        // One Scratch object per worker thread — created lazily on first access.
        ThreadLocal<Scratch> tls = ThreadLocal.withInitial(() -> new Scratch(p1PiecesNum, p2PiecesNum));

        // Phase 1: embarrassingly parallel — each index is independent.
        IntStream.range(0, N).parallel().forEach(i -> {
            Scratch s = tls.get();

            // Zero-allocation unrank: writes directly into s.p1Pos / s.p2Pos.
            Indexing.filePositionToPrimitiveArrays(
                i, boardSize, p1PiecesNum, p2PiecesNum, table,
                s.p1Pos, s.p2Pos, s.combinedUnrank, s.isP1
            );

            // Apply all 7 non-identity transforms; track the minimum orbit index.
            int min = i;
            for (int[] perm : PERMS) {
                int sibling = applyPermAndRank(
                    s.p1Pos, s.p2Pos, s.tp1, s.tp2, perm,
                    s.combined, s.relWhite, table);
                if (sibling < min) min = sibling;
            }
            canonOf[i] = min;
        });

        // Phase 2: canonical iff it is its own orbit minimum.
        for (int i = 0; i < N; i++) {
            if (canonOf[i] == i) bitMap[i] = true;
        }
        return bitMap;
    }

    /**
     * Applies perm to piece positions, sorts in-place, then ranks.
     * All scratch arrays are caller-provided — zero heap allocation.
     */
    private static int applyPermAndRank(
        int[] p1Pos, int[] p2Pos,
        int[] tp1,   int[] tp2,
        int[] perm,
        int[] combined, int[] relWhite,
        BinomialTable table
    ) {
        int p1Count = p1Pos.length, p2Count = p2Pos.length;
        for (int k = 0; k < p1Count; k++) tp1[k] = perm[p1Pos[k]];
        for (int k = 0; k < p2Count; k++) tp2[k] = perm[p2Pos[k]];
        insertionSort(tp1, p1Count);
        insertionSort(tp2, p2Count);
        return rankPrimitive(tp1, p1Count, tp2, p2Count, combined, relWhite, table);
    }

    /**
     * Combinatorial ranking of two sorted position arrays.
     * Mirrors indexInputToFilePosition but takes primitive arrays and caller-provided scratch
     * to avoid all heap allocation.
     */
    private static int rankPrimitive(
        int[] p1, int p1Count, int[] p2, int p2Count,
        int[] combined, int[] relWhite,
        BinomialTable table
    ) {
        int M = p1Count + p2Count;
        int i1 = 0, i2 = 0, pos = 0, wi = 0;
        while (i1 < p1Count && i2 < p2Count) {
            if (p1[i1] < p2[i2]) { relWhite[wi++] = pos; combined[pos++] = p1[i1++]; }
            else                  {                        combined[pos++] = p2[i2++]; }
        }
        while (i1 < p1Count) { relWhite[wi++] = pos; combined[pos++] = p1[i1++]; }
        while (i2 < p2Count) {                        combined[pos++] = p2[i2++]; }

        long index = 0;
        for (int i = 0; i < M; i++) index += table.binomialTable[combined[i]][i + 1];
        long colorSum = 0;
        for (int i = 0; i < p1Count; i++) colorSum += table.binomialTable[relWhite[i]][i + 1];
        return (int)(index + table.binomialTable[36][M] * colorSum);
    }

    // Zero-allocation sort for small fixed-size position arrays.
    private static void insertionSort(int[] a, int len) {
        for (int i = 1; i < len; i++) {
            int key = a[i], j = i - 1;
            while (j >= 0 && a[j] > key) { a[j + 1] = a[j]; j--; }
            a[j + 1] = key;
        }
    }

    // Kept as public utilities for external callers (e.g. TestHashMap).
    public static int[] rotate90Clockwise1D(int[] matrix) {
        int[] result = new int[matrix.length];
        int n = 6;
        for (int r = 0; r < n; r++)
            for (int c = 0; c < n; c++)
                result[c * n + (n - 1 - r)] = matrix[r * n + c];
        return result;
    }

    public static int[] flipHorizontally1D(int[] matrix) {
        int[] result = new int[matrix.length];
        int n = 6;
        for (int r = 0; r < n; r++)
            for (int c = 0; c < n; c++)
                result[r * n + (n - 1 - c)] = matrix[r * n + c];
        return result;
    }
}
