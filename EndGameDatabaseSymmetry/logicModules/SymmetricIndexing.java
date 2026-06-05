package EndGameDatabaseSymmetry.logicModules;

import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.dataClasses.IndexingInput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SymmetricIndexing {

    // The 6 canonical squares — one representative per orbit under the dihedral group D4.
    // Orbits: {0,5,35,30}, {1,11,34,24,4,6,31,29}, {2,17,33,18,3,12,32,23},
    //         {7,10,28,25}, {8,16,27,19,9,13,26,22}, {14,15,21,20}.
    private static final int[] CANONICAL_POSITIONS = {0, 1, 2, 7, 8, 14};

    // CANONICAL_SLOT[sq] = slot index (0–5) if sq is canonical, -1 otherwise.
    private static final int[] CANONICAL_SLOT = new int[36];
    static {
        Arrays.fill(CANONICAL_SLOT, -1);
        for (int i = 0; i < CANONICAL_POSITIONS.length; i++) {
            CANONICAL_SLOT[CANONICAL_POSITIONS[i]] = i;
        }
    }

    // All 8 elements of the dihedral group D4 as board permutations.
    // ALL_PERMS[t][oldSquare] = newSquare after transform t.
    // Order: identity, rot90, rot180, rot270, flipH, flipH+rot90, flipH+rot180, flipH+rot270.
    private static final int[][] ALL_PERMS = buildAll8Perms();

    private static int[] buildRot90Perm() {
        int n = 6;
        int[] p = new int[36];
        for (int r = 0; r < n; r++)
            for (int c = 0; c < n; c++)
                p[r * n + c] = c * n + (n - 1 - r);
        return p;
    }

    private static int[] buildFlipHPerm() {
        int n = 6;
        int[] p = new int[36];
        for (int r = 0; r < n; r++)
            for (int c = 0; c < n; c++)
                p[r * n + c] = r * n + (n - 1 - c);
        return p;
    }

    private static int[] composePerm(int[] a, int[] b) {
        int[] result = new int[36];
        for (int i = 0; i < 36; i++) result[i] = b[a[i]];
        return result;
    }

    private static int[] identityPerm() {
        int[] p = new int[36];
        for (int i = 0; i < 36; i++) p[i] = i;
        return p;
    }

    private static int[][] buildAll8Perms() {
        int[] id    = identityPerm();
        int[] r90   = buildRot90Perm();
        int[] r180  = composePerm(r90, r90);
        int[] r270  = composePerm(r180, r90);
        int[] flipH = buildFlipHPerm();
        return new int[][]{
            id, r90, r180, r270, flipH,
            composePerm(flipH, r90),
            composePerm(flipH, r180),
            composePerm(flipH, r270)
        };
    }

    /**
     * Total number of positions in the symmetric index space.
     *
     * Formula: 6 × C(35, p1-1) × C(36-p1, p2)
     *
     *   6           — which of the 6 canonical squares holds the designated "canonical" P1 piece.
     *   C(35, p1-1) — the remaining p1-1 pieces choose any p1-1 of the other 35 squares.
     *   C(36-p1,p2) — P2 pieces on the 36-p1 unoccupied squares.
     */
    public static long computeNumberOfPositions(int p1Count, int p2Count, BinomialTable table) {
        return 6L
            * table.binomialTable[35][p1Count - 1]
            * table.binomialTable[36 - p1Count][p2Count];
    }

    /**
     * Converts an IndexingInput to a symmetric file position.
     *
     * Precondition: exactly one P1 piece is at one of the 6 canonical positions {0,1,2,7,8,14}.
     */
    public static long indexInputToFilePosition(IndexingInput indexInput, BinomialTable table) {
        List<Integer> p1List = indexInput.getP1Pieces();
        List<Integer> p2List = indexInput.getP2Pieces();
        int p1Count = p1List.size();
        int p2Count = p2List.size();

        int firstP1 = -1;
        int canonicalSlot = -1;
        for (int sq : p1List) {
            int slot = CANONICAL_SLOT[sq];
            if (slot != -1) { firstP1 = sq; canonicalSlot = slot; break; }
        }

        long remainingP1Rank = 0;
        int j = 0;
        for (int sq : p1List) {
            if (sq == firstP1) continue;
            int renumbered = sq > firstP1 ? sq - 1 : sq;
            remainingP1Rank += table.binomialTable[renumbered][j + 1];
            j++;
        }

        long p2Rank = 0;
        for (int i = 0; i < p2Count; i++) {
            int sq = p2List.get(i);
            int p1Below = 0;
            for (int p1sq : p1List) if (p1sq < sq) p1Below++;
            int renumbered = sq - p1Below;
            p2Rank += table.binomialTable[renumbered][i + 1];
        }

        long p2BlockSize    = table.binomialTable[36 - p1Count][p2Count];
        long p1RemBlockSize = table.binomialTable[35][p1Count - 1];
        long blockSize      = p1RemBlockSize * p2BlockSize;

        return (long) canonicalSlot * blockSize + remainingP1Rank * p2BlockSize + p2Rank;
    }

    /**
     * Converts a symmetric file position back to an IndexingInput.
     */
    public static IndexingInput filePositionToIndexingInput(long index, int p1Count, int p2Count, BinomialTable table) {
        long p2BlockSize    = table.binomialTable[36 - p1Count][p2Count];
        long p1RemBlockSize = table.binomialTable[35][p1Count - 1];
        long blockSize      = p1RemBlockSize * p2BlockSize;

        int  canonicalSlot   = (int) (index / blockSize);
        long remainder       = index % blockSize;
        long remainingP1Rank = remainder / p2BlockSize;
        long p2Rank          = remainder % p2BlockSize;

        int firstP1 = CANONICAL_POSITIONS[canonicalSlot];

        List<Integer> p1Pieces = new ArrayList<>();
        p1Pieces.add(firstP1);

        long tempRank = remainingP1Rank;
        for (int i = p1Count - 2; i >= 0; i--) {
            int lo = i, hi = 35;
            while (lo < hi) {
                int mid = (lo + hi + 1) >>> 1;
                if (table.binomialTable[mid][i + 1] <= tempRank) lo = mid;
                else hi = mid - 1;
            }
            tempRank -= table.binomialTable[lo][i + 1];
            p1Pieces.add(lo >= firstP1 ? lo + 1 : lo);
        }
        p1Pieces.sort(Integer::compareTo);

        int[] p1Squares = p1Pieces.stream().mapToInt(Integer::intValue).toArray();

        List<Integer> p2Pieces = new ArrayList<>();
        long tempP2 = p2Rank;
        for (int i = p2Count - 1; i >= 0; i--) {
            int lo = i, hi = 36 - p1Count;
            while (lo < hi) {
                int mid = (lo + hi + 1) >>> 1;
                if (table.binomialTable[mid][i + 1] <= tempP2) lo = mid;
                else hi = mid - 1;
            }
            tempP2 -= table.binomialTable[lo][i + 1];

            int actual = lo;
            for (int p1sq : p1Squares) {
                if (p1sq <= actual) actual++;
            }
            p2Pieces.add(actual);
        }
        p2Pieces.sort(Integer::compareTo);

        return new IndexingInput(p1Pieces, p2Pieces, 36);
    }

    /**
     * Finds the canonical symmetric index for a board state.
     * Tries all 8 D4 symmetry transforms and returns the minimum valid index.
     */
    public static long normalizeToCanonicalIndex(
        int[] boardMap, boolean flipPieces, int p1Count, int p2Count, BinomialTable table
    ) {
        int[] p1 = new int[p1Count];
        int[] p2 = new int[p2Count];
        int p1i = 0, p2i = 0;
        for (int sq = 0; sq < boardMap.length; sq++) {
            int piece = boardMap[sq];
            if (!flipPieces) {
                if (piece == 1) p1[p1i++] = sq;
                else if (piece == 2) p2[p2i++] = sq;
            } else {
                if (piece == 2) p1[p1i++] = sq;
                else if (piece == 1) p2[p2i++] = sq;
            }
        }

        long minIndex = Long.MAX_VALUE;
        int[] tp1 = new int[p1Count];
        int[] tp2 = new int[p2Count];

        for (int[] perm : ALL_PERMS) {
            for (int i = 0; i < p1Count; i++) tp1[i] = perm[p1[i]];
            for (int i = 0; i < p2Count; i++) tp2[i] = perm[p2[i]];
            Arrays.sort(tp1);
            Arrays.sort(tp2);

            int canonCount = 0;
            for (int sq : tp1) if (CANONICAL_SLOT[sq] != -1) canonCount++;
            if (canonCount == 0) continue;

            IndexingInput input = new IndexingInput(36);
            for (int sq : tp1) input.addPieceToP1List(sq);
            for (int sq : tp2) input.addPieceToP2List(sq);
            long idx = indexInputToFilePosition(input, table);
            if (idx < minIndex) minIndex = idx;
        }

        return minIndex;
    }
}
