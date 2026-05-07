package EndGameDatabase.logicModules;

import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.dataClasses.IndexingInput;
import java.util.ArrayList;
import java.util.List;

public class Indexing {

    public static IndexingInput createIndexingInput(int[] map, boolean flipThePieces){
        IndexingInput input = new IndexingInput(map.length);

        if (flipThePieces) {
            for (int i = 0; i < map.length; i++) {
                if (map[i] == 2) {
                    input.addPieceToP1List(i);
                }else if(map[i] == 1){
                    input.addPieceToP2List(i);
                }
            }
        }else{
            for (int i = 0; i < map.length; i++) {
                if (map[i] == 1) {
                    input.addPieceToP1List(i);
                }else if(map[i] == 2){
                    input.addPieceToP2List(i);
                }
            }
        }
        return input;
    }

    public static long computeTheNumberOfPossiblePositions(int p1Pieces, int p2Pieces, int boardSize, BinomialTable table){
        return table.binomialTable[boardSize][p1Pieces+p2Pieces]*table.binomialTable[p1Pieces+p2Pieces][p1Pieces];
    }

    public static long indexInputToFilePosition(IndexingInput indexInput, BinomialTable table) {
        List<Integer> p1List = indexInput.getP1Pieces();
        List<Integer> p2List = indexInput.getP2Pieces();
        int p1Count = p1List.size();
        int p2Count = p2List.size();
        int M = p1Count + p2Count;

        // Both lists are already sorted ascending (scan order from createIndexingInput /
        // unrank order from filePositionToIndexingInput), so we merge in O(M) and track
        // p1 relative positions in the same pass — no ArrayList, no sort, no indexOf.
        int[] combined = new int[M];
        int[] relWhite  = new int[p1Count]; // relative positions of p1 pieces in combined[]

        int i1 = 0, i2 = 0, pos = 0, wi = 0;
        while (i1 < p1Count && i2 < p2Count) {
            int v1 = p1List.get(i1);
            int v2 = p2List.get(i2);
            if (v1 < v2) {
                relWhite[wi++] = pos;
                combined[pos++] = v1;
                i1++;
            } else {
                combined[pos++] = v2;
                i2++;
            }
        }
        while (i1 < p1Count) { relWhite[wi++] = pos; combined[pos++] = p1List.get(i1++); }
        while (i2 < p2Count) { combined[pos++] = p2List.get(i2++); }

        // Layer 1: sum C(combined[i], i+1)
        long index = 0;
        for (int i = 0; i < M; i++) {
            index += table.binomialTable[combined[i]][i + 1];
        }

        // Layer 2: relWhite[] is already sorted ascending (pos was monotonically increasing)
        long colorSum = 0;
        for (int i = 0; i < p1Count; i++) {
            colorSum += table.binomialTable[relWhite[i]][i + 1];
        }

        // Final Index calculation
        index += table.binomialTable[36][M] * colorSum;

        return index;
    }

    public static IndexingInput filePositionToIndexingInput(long index, int boardSize, int p1Count, int p2Count, BinomialTable table) {
        int totalPieces = p1Count + p2Count;
        long waysToPlaceM = table.binomialTable[boardSize][totalPieces];

        // 1. Separate the two layers
        long colorSum = index / waysToPlaceM;
        long baseIndex = index % waysToPlaceM;

        // 2. Unrank Layer 1: Find the combined occupied squares
        int[] combinedList = new int[totalPieces];
        long tempBase = baseIndex;
        
        // Work backwards from largest piece to smallest (binary search: O(log boardSize) per piece)
        for (int i = totalPieces - 1; i >= 0; i--) {
            int lo = i, hi = boardSize;
            while (lo < hi) {
                int mid = (lo + hi + 1) >>> 1;
                if (table.binomialTable[mid][i + 1] <= tempBase) lo = mid;
                else hi = mid - 1;
            }
            combinedList[i] = lo;
            tempBase -= table.binomialTable[lo][i + 1];
        }

        // 3. Unrank Layer 2: Find the relative positions of Player 1's pieces
        int[] relativeWhitePositions = new int[p1Count];
        long tempColor = colorSum;

        for (int i = p1Count - 1; i >= 0; i--) {
            int lo = i, hi = totalPieces;
            while (lo < hi) {
                int mid = (lo + hi + 1) >>> 1;
                if (table.binomialTable[mid][i + 1] <= tempColor) lo = mid;
                else hi = mid - 1;
            }
            relativeWhitePositions[i] = lo;
            tempColor -= table.binomialTable[lo][i + 1];
        }

        // 4. Reconstruct the final P1 and P2 piece lists
        List<Integer> p1Pieces = new ArrayList<>();
        List<Integer> p2Pieces = new ArrayList<>();
        
        // Create a boolean array for quick lookup of P1 relative positions
        boolean[] isP1 = new boolean[totalPieces];
        for (int relPos : relativeWhitePositions) {
            isP1[relPos] = true;
        }

        // Map everything back to the actual board squares
        for (int i = 0; i < totalPieces; i++) {
            if (isP1[i]) {
                p1Pieces.add(combinedList[i]);
            } else {
                p2Pieces.add(combinedList[i]);
            }
        }

        IndexingInput result = new IndexingInput(
            p1Pieces,
            p2Pieces,
            boardSize
        );

        return result;
    }

    /**
     * Returns all 8 D4 transforms of a square board:
     * 4 rotations × 2 (with and without horizontal flip).
     *
     * @param board flat n*n board array (row-major).
     * @param n     side length (6 for the standard Surakarta board).
     * @return array of 8 boards:
     *   [0] identity,  [1] rot90,  [2] rot180,  [3] rot270,
     *   [4] flipH,     [5] flipH+rot90, [6] flipH+rot180, [7] flipH+rot270.
     */
    public static int[][] getAllTransforms(int[] board, int n) {
        int size = n * n;
        int[][] result = new int[8][size];

        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                int src = r * n + c;
                int val = board[src];
                result[0][src]                          = val; // identity
                result[1][c * n + (n - 1 - r)]         = val; // rot90
                result[2][(n - 1 - r) * n + (n - 1 - c)] = val; // rot180
                result[3][(n - 1 - c) * n + r]         = val; // rot270
                result[4][r * n + (n - 1 - c)]         = val; // flipH
                result[5][(n - 1 - c) * n + (n - 1 - r)] = val; // flipH+rot90
                result[6][(n - 1 - r) * n + c]         = val; // flipH+rot180
                result[7][c * n + r]                   = val; // flipH+rot270
            }
        }

        return result;
    }

    /**
     * Returns the file-position indexes for all 8 D4 transforms of a board.
     *
     * @param board      flat n*n board array (1=P1, 2=P2, 0=empty).
     * @param n          side length (6 for the standard Surakarta board).
     * @param flipPieces if true, treat 2 as P1 and 1 as P2.
     * @param table      precomputed binomial table.
     * @return array of 8 indexes matching the transform order of getAllTransforms.
     */
    public static long[] getAllTransformIndexes(int[] board, int n, boolean flipPieces, BinomialTable table) {
        int[][] transforms = getAllTransforms(board, n);
        long[] indexes = new long[8];
        for (int t = 0; t < 8; t++) {
            indexes[t] = indexInputToFilePosition(createIndexingInput(transforms[t], flipPieces), table);
        }
        return indexes;
    }

    /**
     * Zero-allocation variant of filePositionToIndexingInput.
     * Writes sorted P1 positions into p1Out[0..p1Count-1] and P2 into p2Out[0..p2Count-1].
     *
     * combinedScratch  — caller-provided scratch, length >= p1Count + p2Count.
     * isP1Scratch      — caller-provided scratch, length >= p1Count + p2Count;
     *                    must be all-false on entry and is restored to all-false on exit.
     */
    public static void filePositionToPrimitiveArrays(
        long index, int boardSize, int p1Count, int p2Count,
        BinomialTable table,
        int[] p1Out, int[] p2Out,
        int[] combinedScratch, boolean[] isP1Scratch
    ) {
        int totalPieces = p1Count + p2Count;
        long waysToPlaceM = table.binomialTable[boardSize][totalPieces];

        long colorSum  = index / waysToPlaceM;
        long baseIndex = index % waysToPlaceM;

        // Unrank layer 1: combined occupied squares (binary search)
        long tempBase = baseIndex;
        for (int i = totalPieces - 1; i >= 0; i--) {
            int lo = i, hi = boardSize;
            while (lo < hi) {
                int mid = (lo + hi + 1) >>> 1;
                if (table.binomialTable[mid][i + 1] <= tempBase) lo = mid;
                else hi = mid - 1;
            }
            combinedScratch[i] = lo;
            tempBase -= table.binomialTable[lo][i + 1];
        }

        // Unrank layer 2: relative positions of P1 pieces (binary search)
        long tempColor = colorSum;
        for (int i = p1Count - 1; i >= 0; i--) {
            int lo = i, hi = totalPieces;
            while (lo < hi) {
                int mid = (lo + hi + 1) >>> 1;
                if (table.binomialTable[mid][i + 1] <= tempColor) lo = mid;
                else hi = mid - 1;
            }
            isP1Scratch[lo] = true;
            tempColor -= table.binomialTable[lo][i + 1];
        }

        // Reconstruct P1/P2 arrays; restore isP1Scratch to all-false in the same pass.
        int w = 0, b = 0;
        for (int i = 0; i < totalPieces; i++) {
            if (isP1Scratch[i]) { p1Out[w++] = combinedScratch[i]; isP1Scratch[i] = false; }
            else                 { p2Out[b++] = combinedScratch[i]; }
        }
    }
}
