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
            int index = 11504881;

            IndexingInput mapAtIndex = Indexing.filePositionToIndexingInput(index, boardSize, 2, 4, table);
            mapAtIndex.displayIndexInputAsAMap(boardSize);

            System.err.println("Value is: "+FileReaderWriter.readValueFromFile("EndGameDatabase/data/moves2v4.txt", index));

            
        } catch (IOException e) {
            System.err.println("Failed to save the database: " + e.getMessage());
            e.printStackTrace();
        }


        int[] boardMap = {                                                                                                                  
            2,0,0,0,0,2, 
            0,1,0,0,1,0,                                                                                                                    
            0,0,0,0,0,0,                                                                                                                    
            0,0,0,0,0,0,                                                                                                                    
            0,0,0,0,0,0,                                                                                                                    
            2,0,0,0,0,2,                                                                         
        };     

        IndexingInput indexingInput = Indexing.createIndexingInput(boardMap, false);
        System.err.println(Indexing.indexInputToFilePosition(indexingInput, table));
    }
    
}