package EndGameDatabaseSymmetry.logicModules.RetrogradeAnalysis;

import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.dataClasses.IndexingInput;
import EndGameDatabase.dataClasses.Outcome;
import EndGameDatabase.logicModules.MoveListGenerator;
import EndGameDatabaseSymmetry.logicModules.SymmetricIndexing;
import dataClasses.BoardState;
import dataClasses.Move;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import EndGameDatabase.dataClasses.AtomicByteArray;
import java.util.stream.IntStream;
import logicModules.MoveProcessor;

public class SingleRetrogradeAnalysisSym {
    private AtomicByteArray boardResults;
    private AtomicByteArray previousBoardResults;
    private AtomicByteArray oppositeBoard;
    
    private final int p1PiecesNum;
    private final int p2PiecesNum;
    private final int boardSize;
    private BinomialTable table;

    public SingleRetrogradeAnalysisSym(
        int boardSize, 
        int p1PiecesNum, 
        int p2PiecesNum, 
        BinomialTable table, 
        AtomicByteArray previousBoardResults 
    ){
        this.boardSize = boardSize;
        this.p1PiecesNum = p1PiecesNum;
        this.p2PiecesNum = p2PiecesNum;
        this.table = table;
        this.previousBoardResults = previousBoardResults;
        long numberOfResults = SymmetricIndexing.computeNumberOfPositions(p1PiecesNum, p2PiecesNum, table);
        this.boardResults = new AtomicByteArray((int)numberOfResults);
    }

    public void initialize() {
        System.err.println("[" + p1PiecesNum + "v" + p2PiecesNum + "] Starting analysis...");
        initializeBoardStates();
        System.err.println("[" + p1PiecesNum + "v" + p2PiecesNum + "] Initialized.");
    }

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

    public boolean processUncertainStatesPass() {
        AtomicBoolean changed = new AtomicBoolean(false);

        IntStream.range(0, boardResults.length()).parallel().forEach(i -> {
            if (boardResults.get(i) == Outcome.DRAW.value) {
                int newResult = evaluateStateOutcome(i);
                if (newResult != Outcome.DRAW.value) {
                    boardResults.set(i, newResult);
                    changed.set(true);
                }
            }
        });

        return changed.get();
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
        long canonicalIndex = SymmetricIndexing.normalizeToCanonicalIndex(boardState.getBoardMap(), true, p1PiecesNum, p2PiecesNum, table);      
        return boardResults.get((int) canonicalIndex);
    }

    private int getResultOfMoveFromOpposite(Move move) {
        BoardState boardState = MoveProcessor.movePiece(move);
        // After a simple move + flip (2→P1, 1→P2): "P1" count = p2PiecesNum, "P2" count = p1PiecesNum.
        long canonicalIndex = SymmetricIndexing.normalizeToCanonicalIndex(boardState.getBoardMap(), true, p2PiecesNum, p1PiecesNum, table);
        return oppositeBoard.get((int) canonicalIndex);
    }

    private int getResultOfMoveFromPrevious(Move move) {
        BoardState boardState = MoveProcessor.movePiece(move);
        // After P1 captures a P2 piece + flip (2→P1, 1→P2):
        //   "P1" count = p2PiecesNum - 1,  "P2" count = p1PiecesNum.
        long canonicalIndex = SymmetricIndexing.normalizeToCanonicalIndex(boardState.getBoardMap(), true, p2PiecesNum - 1, p1PiecesNum, table);
        return previousBoardResults.get((int) canonicalIndex);
    }

    private int[] getMapFromIndex(int i) {
        IndexingInput input = SymmetricIndexing.filePositionToIndexingInput(i, p1PiecesNum, p2PiecesNum, table);
        return input.getMapFromIndexingInput(boardSize);
    }
    
    public int getP1PiecesNum(){
        return p1PiecesNum;
    }

    public int getP2PiecesNum(){
        return p2PiecesNum;
    }

    public AtomicByteArray getBoardResults() {
        return this.boardResults;
    }

    public void setOppositeBoard(AtomicByteArray oppositeBoard){
        this.oppositeBoard = oppositeBoard;
    }
}
