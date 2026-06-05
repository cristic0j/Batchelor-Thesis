package EndGameDatabaseSymmetry.logicModules.RetrogradeAnalysis;

import EndGameDatabase.dataClasses.AtomicHugeByteArray;
import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.dataClasses.IndexingInput;
import EndGameDatabase.dataClasses.Outcome;
import EndGameDatabase.logicModules.MoveListGenerator;
import EndGameDatabaseSymmetry.logicModules.SymmetricIndexing;
import dataClasses.BoardState;
import dataClasses.Move;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import logicModules.MoveProcessor;

public class OneVsOneRetrogradeAnalysisSym {
    private AtomicHugeByteArray boardResults;
    private final int p1PiecesNum = 1;
    private final int p2PiecesNum = 1;
    private final int boardSize;
    private BinomialTable table;

    public OneVsOneRetrogradeAnalysisSym(int boardSize, BinomialTable table) {
        this.boardSize = boardSize;
        this.table = table;
        long numberOfResults = SymmetricIndexing.computeNumberOfPositions(p1PiecesNum, p2PiecesNum, table);
        this.boardResults = new AtomicHugeByteArray(numberOfResults);
    }

    public void start() {
        System.err.println("[Sym 1v1] Starting analysis...");
        initializeBoardStates();
        processUncertainStates();
        System.err.println("[Sym 1v1] Analysis complete.");
    }

    private void initializeBoardStates() {
        IntStream.range(0, (int) boardResults.size()).parallel().forEach(i -> {
            int[] map = getMapFromIndex(i);
            if (!MoveListGenerator.getTakeMoveList(map, p1PiecesNum).isEmpty()) {
                boardResults.set(i, Outcome.WIN.value);
            } else {
                boardResults.set(i, Outcome.DRAW.value);
            }
        });
    }

    private void processUncertainStates() {
        AtomicBoolean changed = new AtomicBoolean(true);
        while (changed.get()) {
            changed.set(false);
            IntStream.range(0, (int) boardResults.size()).parallel().forEach(i -> {
                if (boardResults.get(i) == Outcome.DRAW.value) {
                    int newResult = evaluateStateOutcome(i);
                    if (newResult != Outcome.DRAW.value) {
                        boardResults.set(i, newResult);
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
            if (nextStateResult == Outcome.LOSS.value) {
                foundLossForP2 = true;
                allAreWinsP2 = false;
                break;
            }
            if (nextStateResult != Outcome.WIN.value) {
                allAreWinsP2 = false;
            }
        }

        if (foundLossForP2) return Outcome.WIN.value;
        // No simple moves at all (stalemate) OR every simple move leads to opponent WIN → LOSS
        if (!hasMoves || allAreWinsP2) return Outcome.LOSS.value;
        return Outcome.DRAW.value;
    }

    private int getResultOfMove(Move move) {
        BoardState boardState = MoveProcessor.movePiece(move);
        long canonicalIndex = SymmetricIndexing.normalizeToCanonicalIndex(
            boardState.getBoardMap(), true, p1PiecesNum, p2PiecesNum, table);
        return boardResults.get(canonicalIndex);
    }

    private int[] getMapFromIndex(int i) {
        IndexingInput input = SymmetricIndexing.filePositionToIndexingInput(i, p1PiecesNum, p2PiecesNum, table);
        return input.getMapFromIndexingInput(boardSize);
    }

    public AtomicHugeByteArray getBoardResults() {
        return boardResults;
    }
}
