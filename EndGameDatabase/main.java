package EndGameDatabase;

import EndGameDatabase.logicModules.MoveListGenerator;

public class main {
    public static void main(String[] args) {
        // PiecePlacementGenerator mGenerator = new PiecePlacementGenerator(2, 2, 6);
        // mGenerator.start();

        int[] boardMap = {
            0,1,1,1,0,0,
            0,0,2,0,0,0,
            0,0,0,0,1,0,
            0,2,0,0,0,0,
            0,0,0,0,0,0,
            2,0,2,0,2,0,
        };

        System.err.println("Amount of simple moves: "+MoveListGenerator.getSimpleMoveList(boardMap).size());
        System.err.println("Amount of take moves: "+MoveListGenerator.getTakeMoveList(boardMap).size());
    }
    
}