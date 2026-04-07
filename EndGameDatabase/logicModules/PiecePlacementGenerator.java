package EndGameDatabase.logicModules;
import EndGameDatabase.dataClasses.RecursivePiecePlacementInput;

public class PiecePlacementGenerator {
    private final int numberOfBlackPieces;
    private final int numberOfWhitePieces;
    private final int numberOfBlackAndWhitePieces;

    private int numberOfPossibleMoves;
    private int[] board;

    public PiecePlacementGenerator(int numberOfBlackPieces, int numberOfWhitePieces, int boardSize){
        this.numberOfBlackPieces = numberOfBlackPieces;
        this.numberOfWhitePieces = numberOfWhitePieces;
        this.numberOfBlackAndWhitePieces = numberOfBlackPieces+numberOfWhitePieces;
        this.board = new int[boardSize];
        for (int i = 0; i < board.length; i++) {
            board[i] = 0;
        }
        this.numberOfPossibleMoves = 0;
    }

    public void start(){
         RecursivePiecePlacementInput nextPieceInput = 
            new RecursivePiecePlacementInput(1, 1, 0);  
        recursiveLooping(nextPieceInput);

        System.err.println("Number of possible moves: " + numberOfPossibleMoves);
    }

    private void increaseNumberOfPossibleMoves(){
        numberOfPossibleMoves += 1;
    }

    public void addPieceOnBoardAtPosition(int position, int pieceType){
        board[position] = pieceType;
    }
    public void removePieceOnBoardAtPosition(int position){
        board[position] = 0;
    }

    public void recursiveLooping(RecursivePiecePlacementInput input){
        //If on the last recursion(print or do something)
        if (input.getIterationNumber() == numberOfBlackAndWhitePieces) {
            lastRecursionIteration(input);
        }else{

            for (int i = input.getStaringPositionNumber(); i < board.length; i++) {
                if (board[i] == 0){
                    //Place the piece on the board
                    addPieceOnBoardAtPosition(i,input.getPieceType());
                    //If this piece is the last black piece
                    if (input.getIterationNumber() == numberOfBlackPieces) {
                        RecursivePiecePlacementInput nextPieceInput = 
                            new RecursivePiecePlacementInput(input.getIterationNumber()+1, 2, 0);

                        recursiveLooping(nextPieceInput);
                    }else{
                        RecursivePiecePlacementInput nextPieceInput = 
                            new RecursivePiecePlacementInput(input.getIterationNumber()+1, input.getPieceType(), i+1);
                        recursiveLooping(nextPieceInput);
                    }
                    //Remove the piece from the board
                    removePieceOnBoardAtPosition(i);
                }
            }
        }
    }

    public void lastRecursionIteration(RecursivePiecePlacementInput input){
        for (int i = input.getStaringPositionNumber(); i < board.length; i++) {
            if (board[i] == 0){
                addPieceOnBoardAtPosition(i,input.getPieceType());

                // Do something ...

                removePieceOnBoardAtPosition(i);

                increaseNumberOfPossibleMoves();
            }
        }
    }    
}