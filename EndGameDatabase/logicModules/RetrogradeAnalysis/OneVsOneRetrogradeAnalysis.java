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
        System.err.println("[1v1] Starting pre initialization");
        preInitialization();
        System.err.println("[1v1] Pre initialization complete");

        System.err.println("[1v1] Starting initialization");
        initializeBoardStates();
        System.err.println("[1v1] Initialization complete");

        System.err.println("[1v1] Starting processing");
        processUncertainStates();
        System.err.println("[1v1] Processing complete");

        System.err.println("[1v1] Starting post processing");
        postProcessing();
        System.err.println("[1v1] Post processing complete");
    }

    public void preInitialization(){
        for (int i = 0; i < boardResults.length(); i++) {
            if (boardResults.get(i) != Outcome.NON_CANONICAL_INDEX.value) {
                int[] map = getMapFromIndex(i);
                //get all the transform indexes (Symmetries)
                long[] transfomIndexes = Indexing.getAllTransformIndexes(map, 6, false, table);
                //Sort 
                Arrays.sort(transfomIndexes);

                //Lable non canonical indexes
                boardResults.set((int)transfomIndexes[0], 2);
                for (int j = 1; j < transfomIndexes.length; j++) {
                    if (transfomIndexes[j] != transfomIndexes[0]) {
                        boardResults.set((int)transfomIndexes[j], 3);
                    }
                }
            }
        }
    }

    public void postProcessing(){
        for (int i = 0; i < boardResults.length(); i++) {
            if (boardResults.get(i) == Outcome.NON_CANONICAL_INDEX.value) {
                int[] map = getMapFromIndex(i);
                long[] transformIndexes = Indexing.getAllTransformIndexes(map, 6, false, table);
                Arrays.sort(transformIndexes);
                boardResults.set(i, boardResults.get((int)transformIndexes[0]));
            }
        }
    }

    private void initializeBoardStates() {
        for (int i = 0; i < boardResults.length(); i++) {

            if (boardResults.get(i) == Outcome.NON_CANONICAL_INDEX.value) {
                continue;
            }

            int[] map = getMapFromIndex(i);
            
            if (!MoveListGenerator.getTakeMoveList(map, p1PiecesNum).isEmpty()) {
                // 3. Use .set(index, value)
                boardResults.set(i, Outcome.WIN.value);
            } else {
                boardResults.set(i, Outcome.DRAW.value);
            }
        }
    }

    private void processUncertainStates() {
        AtomicBoolean changed = new AtomicBoolean(true);
        int iteration = 0;

        while (changed.get()) {
            changed.set(false);
            iteration++;
            System.err.println("[1v1] Processing iteration " + iteration);

            IntStream.range(0, boardResults.length()).parallel().forEach(i -> {
                if (boardResults.get(i) == Outcome.DRAW.value) {
                    int newResult = evaluateStateOutcome(i);
                    if (newResult != Outcome.DRAW.value) {
                        boardResults.set(i, newResult);
                        changed.set(true);
                    }
                }
            });
        }

        System.err.println("[1v1] Converged in " + iteration + " iterations.");
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

    private int[] getMapFromIndex(int i) {
        IndexingInput input = Indexing.filePositionToIndexingInput(i, boardSize, p1PiecesNum, p2PiecesNum, table);
        return input.getMapFromIndexingInput(boardSize);
    }

    // Note: You might want to return AtomicIntegerArray or convert back to int[]
    public AtomicIntegerArray getBoardResults(){
        return boardResults;
    }
}