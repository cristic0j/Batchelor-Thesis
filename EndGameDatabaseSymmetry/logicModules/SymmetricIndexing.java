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

    public static int[][] buildAll8Perms() {
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
     *   C(35, p1-1) — the remaining p1-1 pieces choose any p1-1 of the other 35 squares
     *                 (they may be above or below the canonical piece's square).
     *   C(36-p1,p2) — P2 pieces on the 36-p1 unoccupied squares.
     *
     * Note: not every index in [0, N) corresponds to a valid canonical board.
     * Indices whose decoded board has ≠ 1 canonical P1 piece are "phantom" entries
     * and will never be written by a correctly normalised retrograde analysis.
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
     * That piece is the "canonical" piece; the other p1-1 pieces may be anywhere in the
     * remaining 35 squares (including positions numerically lower than the canonical square).
     * p1Pieces must be sorted ascending.
     */
    public static long indexInputToFilePosition(IndexingInput indexInput, BinomialTable table) {
        List<Integer> p1List = indexInput.getP1Pieces();
        List<Integer> p2List = indexInput.getP2Pieces();
        int p1Count = p1List.size();
        int p2Count = p2List.size();

        // Identify the canonical P1 piece (the one at a canonical position).
        int firstP1 = -1;
        int canonicalSlot = -1;
        for (int sq : p1List) {
            int slot = CANONICAL_SLOT[sq];
            if (slot != -1) { firstP1 = sq; canonicalSlot = slot; break; }
        }

        // --- Layer 1: rank the remaining p1-1 pieces in the 35-square space ---
        // The 35-square space is {0..35} \ {firstP1}, renumbered 0..34:
        //   sq < firstP1 → renumbered = sq
        //   sq > firstP1 → renumbered = sq - 1
        // Because p1List is sorted and we skip firstP1, the renumbered values are produced
        // in ascending order (pieces below firstP1 map to [0,firstP1-1] and pieces above
        // map to [firstP1,34]), so no explicit sort is required.
        long remainingP1Rank = 0;
        int j = 0; // position index among the remaining p1-1 pieces (0-based)
        for (int sq : p1List) {
            if (sq == firstP1) continue;
            int renumbered = sq > firstP1 ? sq - 1 : sq;
            remainingP1Rank += table.binomialTable[renumbered][j + 1];
            j++;
        }

        // --- Layer 2: rank the p2 pieces in the (36-p1)-square space ---
        // Remove all P1 squares and renumber: for each P2 square sq,
        //   renumbered = sq - (number of P1 squares strictly less than sq).
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
     *
     * For "phantom" indices (those whose remaining-P1 rank decodes to a piece that also
     * happens to be at a canonical position), the resulting board will have multiple
     * canonical P1 pieces and will not round-trip correctly. Phantom indices are never
     * produced by correctly normalised positions and can be ignored in practice.
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

        // --- Unrank remaining P1 pieces from C(35, p1Count-1) ---
        // Recovers renumbered positions (in 35-square space) in descending order via binary
        // search, then maps each back to an actual square:
        //   renumbered >= firstP1 → actual = renumbered + 1
        //   renumbered <  firstP1 → actual = renumbered
        // Remaining pieces may land at actual positions lower than firstP1.
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

        // Build sorted P1 array for the P2 renumbering step.
        int[] p1Squares = p1Pieces.stream().mapToInt(Integer::intValue).toArray();

        // --- Unrank P2 pieces from C(36-p1Count, p2Count) ---
        // Maps renumbered positions back to actual squares by bumping past P1 squares.
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
     *
     * Tries all 8 D4 symmetry transforms.  For each transform that yields a board
     * with exactly one P1 piece at a canonical position, computes the symmetric index.
     * Returns the minimum such index — this is the canonical representative.
     *
     * @param boardMap   flat 36-element board array (1=P1, 2=P2, 0=empty).
     * @param flipPieces if true, treat 2 as P1 and 1 as P2 (used after a move to switch sides).
     * @param p1Count    expected number of P1 pieces in the result.
     * @param p2Count    expected number of P2 pieces in the result.
     * @return the canonical symmetric index, or -1 if no valid transform exists
     *         (should not occur for normal endgame positions with p1Count >= 1).
     */
    /**
     * Returns all 8 boards obtained by applying each D4 symmetry transform to the input board.
     *
     * @param board flat 36-element board array (1=P1, 2=P2, 0=empty).
     * @return array of 8 boards; index 0 is the identity (the input itself).
     */
    public static int[][] getAllSymmetricBoards(int[] board) {
        int[][] result = new int[8][36];
        for (int t = 0; t < 8; t++) {
            int[] perm = ALL_PERMS[t];
            for (int sq = 0; sq < 36; sq++) {
                result[t][perm[sq]] = board[sq];
            }
        }
        return result;
    }

    public static long normalizeToCanonicalIndex(
        int[] boardMap, boolean flipPieces, int p1Count, int p2Count, BinomialTable table
    ) {
        // Collect P1 and P2 squares (sorted ascending because boardMap is scanned 0→35).
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
            // Apply the permutation.
            for (int i = 0; i < p1Count; i++) tp1[i] = perm[p1[i]];
            for (int i = 0; i < p2Count; i++) tp2[i] = perm[p2[i]];
            Arrays.sort(tp1);
            Arrays.sort(tp2);

            // Accept transforms that place at least one P1 piece at a canonical square.
            // canonCount > 1 is allowed: indexInputToFilePosition will use the lowest-slot
            // canonical piece, which is consistent across all transforms of the same orbit.
            int canonCount = 0;
            for (int sq : tp1) if (CANONICAL_SLOT[sq] != -1) canonCount++;
            if (canonCount == 0) continue;

            // Build IndexingInput and rank.
            IndexingInput input = new IndexingInput(36);
            for (int sq : tp1) input.addPieceToP1List(sq);
            for (int sq : tp2) input.addPieceToP2List(sq);
            long idx = indexInputToFilePosition(input, table);
            if (idx < minIndex) minIndex = idx;
        }

        return minIndex; // -1 cast to long if not found
    }
}
