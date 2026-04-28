package EndGameDatabase.logicModules.RetrogradeAnalysis;

import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.dataClasses.IndexingInput;
import EndGameDatabase.dataClasses.Outcome;
import EndGameDatabase.logicModules.Indexing;
import EndGameDatabase.logicModules.MoveListGenerator;
import dataClasses.BoardState;
import dataClasses.Move;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerArray;
import logicModules.MoveProcessor;

public class SingleRetrogradeAnalysis {
    private AtomicIntegerArray boardResults;
    private AtomicIntegerArray previousBoardResults;
    private AtomicIntegerArray oppositeBoard;
    
    private final int p1PiecesNum;
    private final int p2PiecesNum;
    private final int boardSize;
    private BinomialTable table;

    public SingleRetrogradeAnalysis(
        int boardSize, 
        int p1PiecesNum, 
        int p2PiecesNum, 
        BinomialTable table, 
        AtomicIntegerArray previousBoardResults 
    ){
        this.boardSize = boardSize;
        this.p1PiecesNum = p1PiecesNum;
        this.p2PiecesNum = p2PiecesNum;
        this.table = table;
        this.previousBoardResults = previousBoardResults;
        long numberOfResults = Indexing.computeTheNumberOfPossiblePositions(p1PiecesNum, p2PiecesNum, boardSize, table);
        this.boardResults = new AtomicIntegerArray((int)numberOfResults);
    }

    public void initialize() {
        System.err.println("[" + p1PiecesNum + "v" + p2PiecesNum + "] Starting analysis...");
        initializeBoardStates();
        System.err.println("[" + p1PiecesNum + "v" + p2PiecesNum + "] Initialized.");
    }

    //Initialize
    private void initializeBoardStates() {
        if (p2PiecesNum == 1) {
            initializeForSingleP2Piece();
        }else{
            initializeForSingleP1Piece();
        }
    }

    private void initializeForSingleP2Piece() {
        for (int i = 0; i < boardResults.length(); i++) {
            int[] map = getMapFromIndex(i);
            List<Move> takeMoves = MoveListGenerator.getTakeMoveList(map, 1);
            boolean canTake = !takeMoves.isEmpty();

            boardResults.set(i, canTake ? Outcome.WIN.value : Outcome.DRAW.value);
        }
    }

    private void initializeForSingleP1Piece() {
        for (int i = 0; i < boardResults.length(); i++) {
            int[] map = getMapFromIndex(i);
            List<Move> takeMoves = MoveListGenerator.getTakeMoveList(map, 1);

            boardResults.set(i, evaluateP1SinglePieceState(i, takeMoves));
        }
    }

    private int evaluateP1SinglePieceState(int stateIndex, List<Move> moves) {
        if (moves.isEmpty()) {
            return Outcome.DRAW.value;
        }

        boolean foundLossForP2 = false;

        for (Move move : moves) {
            int nextStateResult = getResultOfMoveFromPrevious(move);

            if (nextStateResult == Outcome.LOSS.value) {
                foundLossForP2 = true;
                break; 
            }
        }

        if (foundLossForP2) return Outcome.WIN.value;
        
        return Outcome.DRAW.value;
    }

    public boolean processUncertainStatesPass() {
        boolean changed = false;
        
        for (int i = 0; i < boardResults.length(); i++) {
            if (boardResults.get(i) == Outcome.DRAW.value) {
                int newResult = evaluateStateOutcome(i);

                if (newResult != Outcome.DRAW.value) {
                    boardResults.set(i, newResult);
                    changed = true; // Track that we made a change during this pass
                }
            }
        }
        
        // Return true if we changed anything, false if we are stuck/done
        return changed;
    }


    private int evaluateStateOutcome(int index) {
        int[] map = getMapFromIndex(index);
        List<Move> simpleMoves = MoveListGenerator.getSimpleMoveList(map, 1);

        boolean foundLossForP2 = false;
        boolean allAreWinsP2 = true;
        boolean hasMoves = !simpleMoves.isEmpty();
        
        for (Move move : simpleMoves) {
            int nextStateResult;

            if (p1PiecesNum == p2PiecesNum) {
                nextStateResult = getResultOfMove(move);
            }else{
                nextStateResult = getResultOfMoveFromOpposite(move);
            }

            if (nextStateResult == Outcome.LOSS.value) {
                foundLossForP2 = true;
                allAreWinsP2 = false;
                break; 
            } 

            if (nextStateResult != Outcome.WIN.value) {
                allAreWinsP2 = false;
            }
        }

        if (foundLossForP2) {
            return Outcome.WIN.value;
        } else if (hasMoves && allAreWinsP2) {
            if (p1PiecesNum != p2PiecesNum) {
                List<Move> takeMoves = MoveListGenerator.getTakeMoveList(map, 1);
                for (Move move : takeMoves) {
                    int nextStateResult = getResultOfMoveFromPrevious(move);
                    if (nextStateResult != Outcome.WIN.value) {
                        return Outcome.DRAW.value;
                    }
                }
            }
            return Outcome.LOSS.value;
        }
        return Outcome.DRAW.value;
    }

    private int getResultOfMove(Move move) {
        BoardState boardState = MoveProcessor.movePiece(move);
        IndexingInput p2Input = Indexing.createIndexingInput(boardState.getBoardMap(), true);
        long filePosition = Indexing.indexInputToFilePosition(p2Input, table);        
        return boardResults.get((int) filePosition);
    }

    private int getResultOfMoveFromPrevious(Move move) {
        BoardState boardState = MoveProcessor.movePiece(move);
        IndexingInput p2Input = Indexing.createIndexingInput(boardState.getBoardMap(), true);
        long filePosition = Indexing.indexInputToFilePosition(p2Input, table);
        return previousBoardResults.get((int) filePosition);
    }

    private int getResultOfMoveFromOpposite(Move move) {
        BoardState boardState = MoveProcessor.movePiece(move);
        IndexingInput p2Input = Indexing.createIndexingInput(boardState.getBoardMap(), true);
        long filePosition = Indexing.indexInputToFilePosition(p2Input, table);
        return oppositeBoard.get((int) filePosition);
    }
    
    private int[] getMapFromIndex(int i) {
        IndexingInput input = Indexing.filePositionToIndexingInput(i, boardSize, p1PiecesNum, p2PiecesNum, table);
        return input.getMapFromIndexingInput(boardSize);
    }

    public int getP1PiecesNum(){
        return p1PiecesNum;
    }

    public int getP2PiecesNum(){
        return p2PiecesNum;
    }

    public AtomicIntegerArray getBoardResults() {
        return this.boardResults;
    }

    public void setOppositeBoard(AtomicIntegerArray oppositeBoard){
        this.oppositeBoard = oppositeBoard;
    }
}
