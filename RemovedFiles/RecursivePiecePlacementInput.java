package RemovedFiles;

public class RecursivePiecePlacementInput {
    private final int iterationNumber;
    private final int pieceType;
    private final int staringPositionNumber;

    public RecursivePiecePlacementInput(int iterationNumber, int pieceType, int staringPositionNumber){
        this.iterationNumber = iterationNumber;
        this.pieceType = pieceType;
        this.staringPositionNumber = staringPositionNumber;
    }

    public int getIterationNumber(){
        return iterationNumber;
    }
    public int getPieceType(){
        return pieceType;
    }
    public int getStaringPositionNumber(){
        return staringPositionNumber;
    }
}