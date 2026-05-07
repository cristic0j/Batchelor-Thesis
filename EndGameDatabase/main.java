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
            for (int i = 0; i < 1259; i++) {
                IndexingInput mapAtIndex = Indexing.filePositionToIndexingInput(i, boardSize, 1, 1, table);
                mapAtIndex.displayIndexInputAsAMap(boardSize);

                System.err.println(i+"Value is: "+FileReaderWriter.readValueFromFile("EndGameDatabase/data/moves1v1.txt", i));
            }
            
        } catch (IOException e) {
            System.err.println("Failed to save the database: " + e.getMessage());
            e.printStackTrace();
        }


        int[] boardMap = {                                                                                                                  
            2,0,0,0,0,0, 
            0,0,0,0,0,0,                                                                                                                    
            0,0,0,0,0,0,                                                                                                                    
            0,0,0,0,0,0,                                                                                                                    
            0,0,0,0,0,0,                                                                                                                    
            0,0,0,0,0,1,                                                                         
        };     

        IndexingInput indexingInput = Indexing.createIndexingInput(boardMap, false);
        System.err.println(Indexing.indexInputToFilePosition(indexingInput, table));
    }
    
}