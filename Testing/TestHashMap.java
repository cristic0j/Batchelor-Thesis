package Testing;

import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.dataClasses.IndexingInput;
import EndGameDatabase.logicModules.Indexing;
import java.util.HashMap;

public class TestHashMap {
    private BinomialTable table;
    
    public TestHashMap() {
        table = new BinomialTable(36);
        HashMap<Integer, Integer> hashMap = new HashMap<>();
        for (int i = 0; i < 353430; i++) {
            // Original mapping
            int[] map = getMapFromIndex(i);
            int mapIndex = getIndexFromMap(map);

            // Standard Rotations
            int[] map90 = rotate90Clockwise1D(map);
            int mapIndex90 = getIndexFromMap(map90);

            int[] map180 = rotate90Clockwise1D(map90);
            int mapIndex180 = getIndexFromMap(map180);

            int[] map270 = rotate90Clockwise1D(map180);
            int mapIndex270 = getIndexFromMap(map270);

            // Mirrored Mappings
            int[] mirrorMap = flipHorizontally1D(map);
            int mirrorMapIndex = getIndexFromMap(mirrorMap);

            int[] mirrorMap90 = rotate90Clockwise1D(mirrorMap);
            int mirrorMapIndex90 = getIndexFromMap(mirrorMap90);

            int[] mirrorMap180 = rotate90Clockwise1D(mirrorMap90);
            int mirrorMapIndex180 = getIndexFromMap(mirrorMap180);

            int[] mirrorMap270 = rotate90Clockwise1D(mirrorMap180);
            int mirrorMapIndex270 = getIndexFromMap(mirrorMap270);

            int minIndex = Math.min(
                Math.min(Math.min(mapIndex, mapIndex90), Math.min(mapIndex180, mapIndex270)),
                Math.min(Math.min(mirrorMapIndex, mirrorMapIndex90), Math.min(mirrorMapIndex180, mirrorMapIndex270))
            );

            hashMap.put(minIndex, 0);
        }
        hashMap.keySet().stream()
            .sorted() // This sorts the keys on the fly
            .forEach(key -> System.out.println("Canonical Index: " + key));
        System.err.println(hashMap.keySet().size());
    }

    private int[] getMapFromIndex(int i) {
        IndexingInput input = Indexing.filePositionToIndexingInput(i, 36, 2, 2, table);
        return input.getMapFromIndexingInput(36);
    }

    private int getIndexFromMap(int[] map) {
        IndexingInput input = Indexing.createIndexingInput(map, false);
        long index = Indexing.indexInputToFilePosition(input, table);
        return (int)index;
    }

    public static int[] rotate90Clockwise1D(int[] matrix) {
        int[] result = new int[matrix.length];
        //Stands for 6 from 6X6
        int n = 6;

        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                int oldIndex = r * n + c;
                // Mathematical mapping for 90-degree clockwise rotation
                int newIndex = c * n + (n - 1 - r);
                
                result[newIndex] = matrix[oldIndex];
            }
        }
        return result;
    }

    public static int[] flipHorizontally1D(int[] matrix) {
        int[] result = new int[matrix.length];
        //Stands for 6 from 6X6
        int n = 6;
        
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                int oldIndex = r * n + c;
                // Mathematical mapping for horizontal flip
                int newIndex = r * n + (n - 1 - c);
                
                result[newIndex] = matrix[oldIndex];
            }
        }
        return result;
    }
}
