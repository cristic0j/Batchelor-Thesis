package EndGameDatabaseFormulaBasedSymmetry;

import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.logicModules.RandomAccessFile.FileReaderWriter;
import EndGameDatabaseFormulaBasedSymmetry.logicModules.SymmetricIndexing;
import java.io.IOException;

/**
 * Demonstrates the formula-based symmetry builder: computes the canonical
 * symmetric index for a sample board and looks up its outcome in the database.
 */
public class main {
    public static void main(String[] args) {

        BinomialTable table = new BinomialTable(36);

        int[] board = {
            1, 0, 0, 0, 0, 0,
            0, 2, 0, 0, 0, 0,
            0, 0, 1, 0, 0, 0,
            0, 0, 0, 1, 0, 0,
            0, 0, 0, 0, 2, 0,
            2, 0, 0, 0, 0, 0,
        };

        int p1Pieces = 3;
        int p2Pieces = 3;

        long canonicalIndex = SymmetricIndexing.normalizeToCanonicalIndex(
            board, false, p1Pieces, p2Pieces, table
        );

        System.out.println("Canonical symmetric index: " + canonicalIndex);

        try {
            int outcome = FileReaderWriter.readValueFromFile(
                "EndGameDatabaseFormulaBasedSymmetry/data/moves3v3.txt",
                canonicalIndex
            );

            String outcomeLabel = switch (outcome) {
                case 0  -> "LOSS";
                case 1  -> "WIN";
                case 2  -> "DRAW";
                default -> "UNKNOWN";
            };

            System.out.println("Outcome for P1: " + outcomeLabel + " (raw value: " + outcome + ")");

        } catch (IOException e) {
            System.err.println("Could not read database file: " + e.getMessage());
            System.err.println("Make sure to run DatabaseBuilderSymmetry first to generate the file.");
        }
    }
}
