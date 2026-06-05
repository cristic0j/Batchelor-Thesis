package EndGameDatabase;

import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.dataClasses.IndexingInput;
import EndGameDatabase.logicModules.Indexing;
import EndGameDatabase.logicModules.RandomAccessFile.FileReaderWriter;
import java.io.IOException;

public class main {
    public static void main(String[] args) {
        
        final int boardSize = 36;
        BinomialTable table = new BinomialTable(boardSize);


        try {
            int index = 4594009;

            IndexingInput mapAtIndex = Indexing.filePositionToIndexingInput(index, boardSize, 3, 3, table);
            mapAtIndex.displayIndexInputAsAMap(boardSize);

            System.err.println("Value is: "+FileReaderWriter.readValueFromFile("EndGameDatabase/data/moves3v3.txt", index));

            
        } catch (IOException e) {
            System.err.println("Failed to save the database: " + e.getMessage());
            e.printStackTrace();
        }


        int[] boardMap = {
            1, 0, 0, 0, 0, 0,
            0, 2, 0, 0, 0, 0,
            0, 0, 1, 0, 0, 0,
            0, 0, 0, 1, 0, 0,
            0, 0, 0, 0, 2, 0,
            2, 0, 0, 0, 0, 0,
        };    

        IndexingInput indexingInput = Indexing.createIndexingInput(boardMap, false);
        System.err.println(Indexing.indexInputToFilePosition(indexingInput, table));
    }
    
}