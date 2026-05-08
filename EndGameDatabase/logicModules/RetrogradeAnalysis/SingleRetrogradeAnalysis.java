package EndGameDatabase.logicModules.RetrogradeAnalysis;

import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.dataClasses.IndexingInput;
import EndGameDatabase.dataClasses.Outcome;
import EndGameDatabase.logicModules.Indexing;
import EndGameDatabase.logicModules.MoveListGenerator;
import dataClasses.BoardState;
import dataClasses.Move;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import EndGameDatabase.dataClasses.AtomicByteArray;
import java.util.stream.IntStream;
import logicModules.MoveProcessor;

public class SingleRetrogradeAnalysis {
    private AtomicByteArray boardResults;
    private AtomicByteArray previousBoardResults;
    private AtomicByteArray oppositeBoard;
    
    private final int p1PiecesNum;
    private final int p2PiecesNum;
    private final int boardSize;
    private BinomialTable table;

    public SingleRetrogradeAnalysis(
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
        long numberOfResults = Indexing.computeTheNumberOfPossiblePositions(p1PiecesNum, p2PiecesNum, boardSize, table);
        this.boardResults = new AtomicByteArray((int)numberOfResults);
    }

    public void preInitialization() {
        System.err.println("[" + p1PiecesNum + "v" + p2PiecesNum + "] Starting pre initialization...");
        IntStream.range(0, boardResults.length()).parallel().forEach(i -> {
            if (boardResults.get(i) != Outcome.NON_CANONICAL_INDEX.value) {
                int[] map = getMapFromIndex(i);
                long[] transformIndexes = Indexing.getAllTransformIndexes(map, 6, false, table);
                Arrays.sort(transformIndexes);
                boardResults.set((int)transformIndexes[0], 2);
                for (int j = 1; j < transformIndexes.length; j++) {
                    if (transformIndexes[j] != transformIndexes[0]) {
                        boardResults.set((int)transformIndexes[j], Outcome.NON_CANONICAL_INDEX.value);
                    }
                }
            }
        });
        System.err.println("[" + p1PiecesNum + "v" + p2PiecesNum + "] Pre initialization complete.");
    }

    public void postProcessing() {
        System.err.println("[" + p1PiecesNum + "v" + p2PiecesNum + "] Starting post processing...");
        IntStream.range(0, boardResults.length()).parallel().forEach(i -> {
            if (boardResults.get(i) == Outcome.NON_CANONICAL_INDEX.value) {
                int[] map = getMapFromIndex(i);
                long[] transformIndexes = Indexing.getAllTransformIndexes(map, 6, false, table);
                Arrays.sort(transformIndexes);
                boardResults.set(i, boardResults.get((int)transformIndexes[0]));
            }
        });
        System.err.println("[" + p1PiecesNum + "v" + p2PiecesNum + "] Post processing complete.");
    }

    public void initialize() {
        preInitialization();
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
        IntStream.range(0, boardResults.length()).parallel().forEach(i -> {
            if (boardResults.get(i) == Outcome.NON_CANONICAL_INDEX.value) return;
            int[] map = getMapFromIndex(i);
            List<Move> takeMoves = MoveListGenerator.getTakeMoveList(map, 1);
            boardResults.set(i, !takeMoves.isEmpty() ? Outcome.WIN.value : Outcome.DRAW.value);
        });
    }

    private void initializeForSingleP1Piece() {
        IntStream.range(0, boardResults.length()).parallel().forEach(i -> {
            if (boardResults.get(i) == Outcome.NON_CANONICAL_INDEX.value) return;
            int[] map = getMapFromIndex(i);
            List<Move> takeMoves = MoveListGenerator.getTakeMoveList(map, 1);
            boardResults.set(i, evaluateP1SinglePieceState(i, takeMoves));
        });
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
        int[] nextMap = boardState.getBoardMap();
        long filePosition = Indexing.indexInputToFilePosition(Indexing.createIndexingInput(nextMap, true), table);
        int result = boardResults.get((int) filePosition);
        if (result == Outcome.NON_CANONICAL_INDEX.value) {
            long[] transforms = Indexing.getAllTransformIndexes(nextMap, 6, true, table);
            Arrays.sort(transforms);
            result = boardResults.get((int) transforms[0]);
        }
        return result;
    }

    private int getResultOfMoveFromPrevious(Move move) {
        BoardState boardState = MoveProcessor.movePiece(move);
        IndexingInput p2Input = Indexing.createIndexingInput(boardState.getBoardMap(), true);
        long filePosition = Indexing.indexInputToFilePosition(p2Input, table);
        return previousBoardResults.get((int) filePosition);
    }

    private int getResultOfMoveFromOpposite(Move move) {
        BoardState boardState = MoveProcessor.movePiece(move);
        int[] nextMap = boardState.getBoardMap();
        long filePosition = Indexing.indexInputToFilePosition(Indexing.createIndexingInput(nextMap, true), table);
        int result = oppositeBoard.get((int) filePosition);
        if (result == Outcome.NON_CANONICAL_INDEX.value) {
            long[] transforms = Indexing.getAllTransformIndexes(nextMap, 6, true, table);
            Arrays.sort(transforms);
            result = oppositeBoard.get((int) transforms[0]);
        }
        return result;
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

    public AtomicByteArray getBoardResults() {
        return this.boardResults;
    }

    public void setOppositeBoard(AtomicByteArray oppositeBoard){
        this.oppositeBoard = oppositeBoard;
    }
}
