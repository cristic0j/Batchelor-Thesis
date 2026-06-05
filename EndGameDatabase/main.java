package EndGameDatabase;

import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.dataClasses.IndexingInput;
import EndGameDatabase.logicModules.Indexing;
import java.util.Arrays;

/**
 * Demonstrates the combinatorial indexing system with a round-trip verification:
 * encode a sample board to a file-position index, decode it back, and confirm
 * the recovered board matches the original.
 */
public class main {
    public static void main(String[] args) {
        final int boardSize = 36;
        BinomialTable table = new BinomialTable(boardSize);

        int[] board = {
            1, 0, 0, 0, 0, 0,
            0, 2, 0, 0, 0, 0,
            0, 0, 1, 0, 0, 0,
            0, 0, 0, 1, 0, 0,
            0, 0, 0, 0, 2, 0,
            2, 0, 0, 0, 0, 0,
        };
        int p1 = 3, p2 = 3;

        IndexingInput input = Indexing.createIndexingInput(board, false);
        long index = Indexing.indexInputToFilePosition(input, table);
        System.out.println("Encoded index: " + index);

        IndexingInput decoded = Indexing.filePositionToIndexingInput(index, boardSize, p1, p2, table);
        int[] recoveredBoard = decoded.getMapFromIndexingInput(boardSize);

        System.out.println("Round-trip match: " + Arrays.equals(board, recoveredBoard));
        decoded.displayIndexInputAsAMap(boardSize);
    }
}
