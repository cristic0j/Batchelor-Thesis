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
        
        // Work backwards from largest piece to smallest
        for (int i = totalPieces - 1; i >= 0; i--) {
            int v = i; // Minimum possible square index is i
            // Find the largest 'v' where C(v, i+1) is less than or equal to our remaining index
            while (v + 1 <= boardSize && table.binomialTable[v + 1][i + 1] <= tempBase) {
                v++;
            }
            combinedList[i] = v;
            tempBase -= table.binomialTable[v][i + 1];
        }

        // 3. Unrank Layer 2: Find the relative positions of Player 1's pieces
        int[] relativeWhitePositions = new int[p1Count];
        long tempColor = colorSum;
        
        for (int i = p1Count - 1; i >= 0; i--) {
            int v = i; 
            while (v + 1 <= totalPieces && table.binomialTable[v + 1][i + 1] <= tempColor) {
                v++;
            }
            relativeWhitePositions[i] = v;
            tempColor -= table.binomialTable[v][i + 1];
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

        // Construct and return your object (adjust this line to match your actual constructor)
        IndexingInput result = new IndexingInput(
            p1Pieces,
            p2Pieces,
            boardSize
        );
        
        return result;
    }
}
