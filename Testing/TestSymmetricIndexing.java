package Testing;

import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.dataClasses.IndexingInput;
import EndGameDatabaseSymmetry.logicModules.SymmetricIndexing;
import java.util.Arrays;
import java.util.List;

/**
 * Tests for SymmetricIndexing.
 *
 * Key concepts tested:
 *   1. computeNumberOfPositions — spot-check known values.
 *   2. Round-trip decode→encode  — for "valid" indices (those that decode to a board with
 *      exactly one canonical P1 piece), re-encoding must return the same index.
 *   3. Bijection encode→decode   — every board with exactly one canonical P1 piece maps
 *      to a unique index in [0, N). No collisions, no out-of-range results.
 *   4. Manual spot-checks        — hand-verified encode / decode pairs.
 *
 * Background: the index space has size 6×C(35,p1-1)×C(36-p1,p2). Not every index in
 * this range corresponds to a valid canonical board: indices whose decoded remaining-P1
 * piece also lands on a canonical square produce boards with two canonical pieces and
 * are "phantom" entries (they will never be written by a correct retrograde analysis).
 */
public class TestSymmetricIndexing {

    private static final BinomialTable TABLE = new BinomialTable(36);

    // The 6 canonical positions {0,1,2,7,8,14} — must match SymmetricIndexing's constant.
    private static final int[] CANONICAL_POSITIONS = {0, 1, 2, 7, 8, 14};
    private static final boolean[] IS_CANONICAL = new boolean[36];
    static {
        for (int c : CANONICAL_POSITIONS) IS_CANONICAL[c] = true;
    }

    private static int passed = 0;
    private static int failed = 0;

    // -------------------------------------------------------------------------
    // Entry point
    // -------------------------------------------------------------------------

    public static void run() {
        System.out.println("=== TestSymmetricIndexing ===");

        testComputeNumberOfPositions();

        // Round-trip: decode every index; if the board has exactly one canonical P1 piece
        // it is a "valid" index and must re-encode to itself.
        testRoundTripDecodeEncode(1, 1);
        testRoundTripDecodeEncode(1, 2);
        testRoundTripDecodeEncode(2, 1);
        testRoundTripDecodeEncode(2, 2);
        testRoundTripDecodeEncode(3, 1);
        testRoundTripDecodeEncode(1, 3);

        // Bijection: enumerate every board with exactly one canonical P1 piece;
        // no two boards should share an index, and all indices must be in [0, N).
        testBijectionEncodeAll(1, 1);
        testBijectionEncodeAll(1, 2);
        testBijectionEncodeAll(2, 1);
        testBijectionEncodeAll(2, 2);

        testManualSpotChecks();

        System.out.println("\nResults: " + passed + " passed, " + failed + " failed.");
    }

    // -------------------------------------------------------------------------
    // Test 1 — computeNumberOfPositions
    // -------------------------------------------------------------------------

    private static void testComputeNumberOfPositions() {
        // 1v1:  6 × C(35,0) × C(35,1) = 6 × 1 × 35 = 210
        assertEq("computeNumberOfPositions(1,1)", 210L,
            SymmetricIndexing.computeNumberOfPositions(1, 1, TABLE));

        // 1v2:  6 × C(35,0) × C(35,2) = 6 × 595 = 3570
        assertEq("computeNumberOfPositions(1,2)", 3570L,
            SymmetricIndexing.computeNumberOfPositions(1, 2, TABLE));

        // 2v1:  6 × C(35,1) × C(34,1) = 6 × 35 × 34 = 7140
        assertEq("computeNumberOfPositions(2,1)", 7140L,
            SymmetricIndexing.computeNumberOfPositions(2, 1, TABLE));

        // 2v2:  6 × C(35,1) × C(34,2) = 6 × 35 × 561 = 117810
        assertEq("computeNumberOfPositions(2,2)", 117810L,
            SymmetricIndexing.computeNumberOfPositions(2, 2, TABLE));

        // 3v3:  6 × C(35,2) × C(33,3) = 6 × 595 × 5456 = 19 477 920
        assertEq("computeNumberOfPositions(3,3)", 19_477_920L,
            SymmetricIndexing.computeNumberOfPositions(3, 3, TABLE));
    }

    // -------------------------------------------------------------------------
    // Test 2 — round-trip decode→encode (valid indices only)
    // -------------------------------------------------------------------------

    /**
     * For every index in [0, N):
     *   - Decode to a board.
     *   - If the board has exactly one canonical P1 piece, it is "valid": re-encode and
     *     assert the result equals the original index.
     *   - If the board has 0 or ≥2 canonical P1 pieces it is a "phantom" index: skip.
     */
    private static void testRoundTripDecodeEncode(int p1Count, int p2Count) {
        long N = SymmetricIndexing.computeNumberOfPositions(p1Count, p2Count, TABLE);
        int errors = 0;
        int skipped = 0;
        for (long i = 0; i < N; i++) {
            IndexingInput decoded = SymmetricIndexing.filePositionToIndexingInput(i, p1Count, p2Count, TABLE);
            if (!hasExactlyOneCanonicalP1(decoded)) { skipped++; continue; }
            long reEncoded = SymmetricIndexing.indexInputToFilePosition(decoded, TABLE);
            if (reEncoded != i) {
                if (errors < 5) {
                    System.err.println("  FAIL round-trip " + p1Count + "v" + p2Count
                        + ": index=" + i + " re-encoded=" + reEncoded
                        + " p1=" + decoded.getP1Pieces() + " p2=" + decoded.getP2Pieces());
                }
                errors++;
            }
        }
        assertEq("round-trip decode→encode " + p1Count + "v" + p2Count + " errors (skipped " + skipped + " phantom)", 0, errors);
    }

    /** Returns true iff exactly one P1 piece is at a canonical position. */
    private static boolean hasExactlyOneCanonicalP1(IndexingInput input) {
        int count = 0;
        for (int sq : input.getP1Pieces()) if (IS_CANONICAL[sq]) count++;
        return count == 1;
    }

    // -------------------------------------------------------------------------
    // Test 3 — bijection: boards with exactly one canonical P1 piece → [0, N)
    // -------------------------------------------------------------------------

    /**
     * Enumerates all boards where exactly one P1 piece is at a canonical position.
     * Checks:
     *   a) every computed index is in [0, N)
     *   b) no two boards share an index (no collisions)
     */
    private static void testBijectionEncodeAll(int p1Count, int p2Count) {
        long N = SymmetricIndexing.computeNumberOfPositions(p1Count, p2Count, TABLE);
        boolean[] seen = new boolean[(int) N];
        int[] collisions  = {0};
        int[] outOfRange  = {0};

        for (int c : CANONICAL_POSITIONS) {
            // Enumerate p1Count-1 more P1 pieces from {0..35}\{c}\{all canonical positions}.
            // "Exactly one canonical" means remaining pieces must NOT be at any canonical square.
            int[] remaining = new int[p1Count - 1];
            enumerateRemainingP1(
                c, remaining, 0,
                new int[p2Count], p2Count,
                seen, collisions, outOfRange, (int) N, p1Count
            );
        }

        assertEq("bijection encode " + p1Count + "v" + p2Count + " out-of-range",  0, outOfRange[0]);
        assertEq("bijection encode " + p1Count + "v" + p2Count + " collisions",    0, collisions[0]);
    }

    /**
     * Recursively fills remaining[pos..p1Count-2] with non-canonical squares
     * not equal to 'canonical', then enumerates all P2 placements.
     */
    private static void enumerateRemainingP1(
        int canonical, int[] remaining, int pos,
        int[] p2Squares, int p2Total,
        boolean[] seen, int[] collisions, int[] outOfRange, int N, int p1Count
    ) {
        if (pos == p1Count - 1) {
            // All P1 placed — build full P1 array and enumerate P2.
            int[] allP1 = buildP1Array(canonical, remaining, p1Count);
            enumerateP2(allP1, p2Squares, 0, p2Total,
                seen, collisions, outOfRange, N);
            return;
        }
        int startFrom = (pos == 0) ? 0 : remaining[pos - 1] + 1;
        for (int sq = startFrom; sq < 36; sq++) {
            if (sq == canonical) continue;       // skip the canonical square itself
            if (IS_CANONICAL[sq]) continue;      // skip other canonical squares (would give 2 canonical pieces)
            remaining[pos] = sq;
            enumerateRemainingP1(canonical, remaining, pos + 1,
                p2Squares, p2Total, seen, collisions, outOfRange, N, p1Count);
        }
    }

    private static int[] buildP1Array(int canonical, int[] remaining, int p1Count) {
        int[] all = new int[p1Count];
        all[0] = canonical;
        System.arraycopy(remaining, 0, all, 1, p1Count - 1);
        Arrays.sort(all);
        return all;
    }

    /** Recursively fills p2Squares[pos..p2Total-1] with squares not occupied by P1. */
    private static void enumerateP2(
        int[] p1Squares, int[] p2Squares, int pos, int p2Total,
        boolean[] seen, int[] collisions, int[] outOfRange, int N
    ) {
        if (pos == p2Total) {
            IndexingInput input = buildInput(p1Squares, p2Squares, p2Total);
            long idx = SymmetricIndexing.indexInputToFilePosition(input, TABLE);
            int i = (int) idx;
            if (i < 0 || i >= N) { outOfRange[0]++; return; }
            if (seen[i])          { collisions[0]++; return; }
            seen[i] = true;
            return;
        }
        int startFrom = (pos == 0) ? 0 : p2Squares[pos - 1] + 1;
        outer:
        for (int sq = startFrom; sq < 36; sq++) {
            for (int p1sq : p1Squares) if (p1sq == sq) continue outer;
            p2Squares[pos] = sq;
            enumerateP2(p1Squares, p2Squares, pos + 1, p2Total,
                seen, collisions, outOfRange, N);
        }
    }

    private static IndexingInput buildInput(int[] p1Sq, int[] p2Sq, int p2Total) {
        IndexingInput input = new IndexingInput(36);
        for (int sq : p1Sq) input.addPieceToP1List(sq);
        for (int i = 0; i < p2Total; i++) input.addPieceToP2List(p2Sq[i]);
        return input;
    }

    // -------------------------------------------------------------------------
    // Test 4 — manual spot checks (hand-verified)
    // -------------------------------------------------------------------------

    private static void testManualSpotChecks() {
        // ---- 1v1 ----
        // canonicalSlot=0 (firstP1=0), p2Rank=0
        //   p2Rank: sq=1, p1Below=1(0<1), renumbered=0, C(0,1)=0 → rank=0
        //   index = 0*35 + 0 = 0
        checkEncode("1v1 p1=[0] p2=[1]",   new int[]{0}, new int[]{1},   0L);
        checkDecode("1v1 index=0",         0L, 1, 1, new int[]{0}, new int[]{1});

        // canonicalSlot=0, p2Rank=1
        //   sq=2, p1Below=1, renumbered=1, C(1,1)=1 → rank=1
        //   index = 1
        checkEncode("1v1 p1=[0] p2=[2]",   new int[]{0}, new int[]{2},   1L);
        checkDecode("1v1 index=1",         1L, 1, 1, new int[]{0}, new int[]{2});

        // canonicalSlot=0, p2Rank=34
        //   sq=35, p1Below=1(0<35), renumbered=34, C(34,1)=34 → rank=34
        //   index = 34
        checkEncode("1v1 p1=[0] p2=[35]",  new int[]{0}, new int[]{35},  34L);

        // canonicalSlot=1 (firstP1=1), p2Rank=0
        //   sq=0, p1Below=0, renumbered=0, C(0,1)=0 → rank=0
        //   index = 1*35 + 0 = 35
        checkEncode("1v1 p1=[1] p2=[0]",   new int[]{1}, new int[]{0},   35L);
        checkDecode("1v1 index=35",        35L, 1, 1, new int[]{1}, new int[]{0});

        // canonicalSlot=5 (firstP1=14), p2Rank=34
        //   sq=35, p1Below=1(14<35), renumbered=34, C(34,1)=34 → rank=34
        //   index = 5*35 + 34 = 209  (last 1v1 index)
        checkEncode("1v1 p1=[14] p2=[35]", new int[]{14}, new int[]{35}, 209L);
        checkDecode("1v1 index=209",      209L, 1, 1, new int[]{14}, new int[]{35});

        // ---- 2v1 ----
        // p1=[0,3], p2=[5]   canonical=0 (slot 0), remaining=3 (non-canonical)
        //   remainingP1Rank: sq=3, renumbered=3>0→2, rank=C(2,1)=2
        //   p2Rank: sq=5, p1Below=2 (0<5,3<5), renumbered=3, C(3,1)=3
        //   p2BlockSize=C(34,1)=34, p1RemBlockSize=C(35,1)=35, blockSize=1190
        //   index = 0*1190 + 2*34 + 3 = 71
        checkEncode("2v1 p1=[0,3] p2=[5]", new int[]{0, 3}, new int[]{5}, 71L);
        checkDecode("2v1 index=71",        71L, 2, 1, new int[]{0, 3}, new int[]{5});

        // p1=[3,7], p2=[5]   canonical=7 (slot 3), remaining=3 (non-canonical)
        //   remaining: sq=3 (< canonical=7), renumbered=3, rank=C(3,1)=3
        //   p2Rank: sq=5, p1Below=1 (3<5, 7>5), renumbered=4, C(4,1)=4
        //   index = 3*1190 + 3*34 + 4 = 3570+102+4 = 3676
        checkEncode("2v1 p1=[3,7] p2=[5]", new int[]{3, 7}, new int[]{5}, 3676L);
        checkDecode("2v1 index=3676",      3676L, 2, 1, new int[]{3, 7}, new int[]{5});

        // p1=[0,3], p2=[9]   canonical=0 (slot 0), remaining=3
        //   remainingP1Rank=2 (same as above)
        //   p2Rank: sq=9, p1Below=2 (0<9,3<9), renumbered=7, C(7,1)=7
        //   index = 0 + 2*34 + 7 = 75
        checkEncode("2v1 p1=[0,3] p2=[9]", new int[]{0, 3}, new int[]{9}, 75L);
    }

    private static void checkEncode(String label, int[] p1, int[] p2, long expectedIndex) {
        IndexingInput input = new IndexingInput(36);
        for (int sq : p1) input.addPieceToP1List(sq);
        for (int sq : p2) input.addPieceToP2List(sq);
        long actual = SymmetricIndexing.indexInputToFilePosition(input, TABLE);
        if (actual == expectedIndex) {
            pass(label);
        } else {
            fail(label + ": expected " + expectedIndex + " got " + actual
                + "  p1=" + Arrays.toString(p1) + " p2=" + Arrays.toString(p2));
        }
    }

    private static void checkDecode(String label, long index, int p1Count, int p2Count,
                                    int[] expectedP1, int[] expectedP2) {
        IndexingInput result = SymmetricIndexing.filePositionToIndexingInput(index, p1Count, p2Count, TABLE);
        List<Integer> gotP1 = result.getP1Pieces();
        List<Integer> gotP2 = result.getP2Pieces();
        boolean ok = listMatchesArray(gotP1, expectedP1) && listMatchesArray(gotP2, expectedP2);
        if (ok) {
            pass(label);
        } else {
            fail(label + ": expected p1=" + Arrays.toString(expectedP1)
                + " p2=" + Arrays.toString(expectedP2)
                + "  got p1=" + gotP1 + " p2=" + gotP2);
        }
    }

    private static boolean listMatchesArray(List<Integer> list, int[] arr) {
        if (list.size() != arr.length) return false;
        for (int i = 0; i < arr.length; i++) {
            if (list.get(i) != arr[i]) return false;
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static void assertEq(String label, long expected, long actual) {
        if (expected == actual) {
            pass(label);
        } else {
            fail(label + ": expected " + expected + " got " + actual);
        }
    }

    private static void assertEq(String label, int expected, int actual) {
        assertEq(label, (long) expected, (long) actual);
    }

    private static void pass(String label) {
        System.out.println("  PASS  " + label);
        passed++;
    }

    private static void fail(String label) {
        System.out.println("  FAIL  " + label);
        failed++;
    }
}
