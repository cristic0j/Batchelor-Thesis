package EndGameDatabase.logicModules.RetrogradeAnalysis;

import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.dataClasses.IndexingInput;
import EndGameDatabase.dataClasses.Outcome;
import EndGameDatabase.logicModules.Indexing;
import EndGameDatabase.logicModules.MoveListGenerator;
import dataClasses.BoardState;
import dataClasses.Move;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.stream.IntStream;
import logicModules.MoveProcessor;

public class OneVsOneRetrogradeAnalysis {

    // 1. Changed to AtomicIntegerArray
    private AtomicIntegerArray boardResults; 
    private final int p1PiecesNum = 1;
    private final int p2PiecesNum = 1;
    private final int boardSize;
    private BinomialTable table;

    public OneVsOneRetrogradeAnalysis(int boardSize, BinomialTable table){
        this.boardSize = boardSize;
        this.table = table;
        long numberOfResults = Indexing.computeTheNumberOfPossiblePositions(p1PiecesNum, p2PiecesNum, boardSize, table);
        
        // 2. Initialize AtomicIntegerArray
        this.boardResults = new AtomicIntegerArray((int)numberOfResults);
    }

    public void start() {
        System.err.println("[1v1] Starting analysis...");
        initializeBoardStates();
        processUncertainStates();
        System.err.println("[1v1] Analysis complete.");
    }

    private void initializeBoardStates() {
        IntStream.range(0, boardResults.length()).parallel().forEach(i -> {
            int[] map = getMapFromIndex(i);
            if (!MoveListGenerator.getTakeMoveList(map, p1PiecesNum).isEmpty()) {
                setWithSymmetry(i, Outcome.WIN.value);
            } else {
                boardResults.set(i, Outcome.DRAW.value);
            }
        });
    }

    private void processUncertainStates() {
        AtomicBoolean changed = new AtomicBoolean(true);

        while (changed.get()) {
            changed.set(false);

            IntStream.range(0, boardResults.length()).parallel().forEach(i -> {
                if (boardResults.get(i) == Outcome.DRAW.value) {
                    int newResult = evaluateStateOutcome(i);
                    if (newResult != Outcome.DRAW.value) {
                        setWithSymmetry(i, newResult);
                        changed.set(true);
                    }
                }
            });
        }
    }

    private int evaluateStateOutcome(int index) {
        int[] map = getMapFromIndex(index);
        List<Move> simpleMoves = MoveListGenerator.getSimpleMoveList(map, p1PiecesNum);

        boolean foundLossForP2 = false;
        boolean allAreWinsP2 = true;
        boolean hasMoves = !simpleMoves.isEmpty();

        for (Move move : simpleMoves) {
            int nextStateResult = getResultOfMove(move);

            // 5. Lookups are now thread-safe reads
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

    private int[] getMapFromIndex(int i) {
        IndexingInput input = Indexing.filePositionToIndexingInput(i, boardSize, p1PiecesNum, p2PiecesNum, table);
        return input.getMapFromIndexingInput(boardSize);
    }

    private void setWithSymmetry(int index, int value) {
        int[] map = getMapFromIndex(index);
        int n = (int) Math.sqrt(boardSize);
        long[] symIndexes = Indexing.getAllTransformIndexes(map, n, false, table);
        for (long symIdx : symIndexes) {
            boardResults.set((int) symIdx, value);
        }
    }

    // Note: You might want to return AtomicIntegerArray or convert back to int[]
    public AtomicIntegerArray getBoardResults(){
        return boardResults;
    }
}