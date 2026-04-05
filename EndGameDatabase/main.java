package EndGameDatabase;

import EndGameDatabase.moveGenerator.MoveGenerator;

public class main {
    public static void main(String[] args) {
        MoveGenerator mGenerator = new MoveGenerator(2, 2, 6);
        mGenerator.start();
    }
    
}