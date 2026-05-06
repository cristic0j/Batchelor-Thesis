package Testing;

import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.dataClasses.IndexingInput;
import EndGameDatabaseSymmetry.logicModules.SymmetricIndexing;

public class main {
    public static void main(String[] args) {
        int p1Count = 3;
        int p2Count = 3;
        int tableSize = 36;
        BinomialTable table = new BinomialTable(tableSize);

        //
        // System.err.println(SymmetricIndexing.computeNumberOfPositions(p1Count, p2Count, table));
        IndexingInput indexingInput = SymmetricIndexing.filePositionToIndexingInput(5257919, p1Count, p2Count, table);
        indexingInput.displayIndexInputAsAMap(tableSize);

        
    }
}
