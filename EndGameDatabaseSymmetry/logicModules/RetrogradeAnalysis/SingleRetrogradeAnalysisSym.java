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

public class SingleRetrogradeAnalysisSym {
    private AtomicHugeByteArray boardResults;
    private AtomicHugeByteArray previousBoardResults;
    private AtomicHugeByteArray oppositeBoard;

    private final int p1PiecesNum;
    private final int p2PiecesNum;
    private final int boardSize;
    private BinomialTable table;

    public SingleRetrogradeAnalysisSym(
        int boardSize,
        int p1PiecesNum,
        int p2PiecesNum,
        BinomialTable table,
        AtomicHugeByteArray previousBoardResults
    ) {
        this.boardSize = boardSize;
        this.p1PiecesNum = p1PiecesNum;
        this.p2PiecesNum = p2PiecesNum;
        this.table = table;
        this.previousBoardResults = previousBoardResults;
        long numberOfResults = SymmetricIndexing.computeNumberOfPositions(p1PiecesNum, p2PiecesNum, table);
        this.boardResults = new AtomicHugeByteArray(numberOfResults);
    }

    public void initialize() {
        System.err.println("[Sym " + p1PiecesNum + "v" + p2PiecesNum + "] Starting analysis...");
        initializeBoardStates();
        System.err.println("[Sym " + p1PiecesNum + "v" + p2PiecesNum + "] Initialized.");
    }

    private void initializeBoardStates() {
        if (p2PiecesNum == 1) {
            initializeForSingleP2Piece();
        } else {
            initializeForSingleP1Piece();
        }
    }

    private void initializeForSingleP2Piece() {
        IntStream.range(0, (int) boardResults.size()).parallel().forEach(i -> {
            int[] map = getMapFromIndex(i);
            List<Move> takeMoves = MoveListGenerator.getTakeMoveList(map, 1);
            boardResults.set(i, takeMoves.isEmpty() ? Outcome.DRAW.value : Outcome.WIN.value);
        });
    }

    private void initializeForSingleP1Piece() {
        IntStream.range(0, (int) boardResults.size()).parallel().forEach(i -> {
            int[] map = getMapFromIndex(i);
            List<Move> takeMoves = MoveListGenerator.getTakeMoveList(map, 1);
            boardResults.set(i, evaluateP1SinglePieceState(i, takeMoves));
        });
    }

    public boolean processUncertainStatesPass() {
        AtomicBoolean changed = new AtomicBoolean(false);
        IntStream.range(0, (int) boardResults.size()).parallel().forEach(i -> {
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
        if (moves.isEmpty()) return Outcome.DRAW.value;
        for (Move move : moves) {
            if (getResultOfMoveFromPrevious(move) == Outcome.LOSS.value) {
                return Outcome.WIN.value;
            }
        }
        return Outcome.DRAW.value;
    }

    private int evaluateStateOutcome(int index) {
        int[] map = getMapFromIndex(index);
        List<Move> simpleMoves = MoveListGenerator.getSimpleMoveList(map, 1);

        boolean foundLossForP2 = false;
        boolean allAreWinsP2 = true;
        boolean hasMoves = !simpleMoves.isEmpty();

        for (Move move : simpleMoves) {
            int nextStateResult = (p1PiecesNum == p2PiecesNum)
                ? getResultOfMove(move)
                : getResultOfMoveFromOpposite(move);

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

        // P1 is LOSS if all simple moves lead to opponent WIN, or if there are no simple moves.
        // Either way we must also check capture moves — a capture might escape to a draw.
        if (hasMoves ? allAreWinsP2 : true) {
            List<Move> takeMoves = MoveListGenerator.getTakeMoveList(map, 1);
            boolean hasAnyMove = hasMoves || !takeMoves.isEmpty();
            if (!hasAnyMove) {
                return Outcome.LOSS.value; // stalemate — no moves at all
            }
            for (Move move : takeMoves) {
                if (getResultOfMoveFromPrevious(move) != Outcome.WIN.value) {
                    return Outcome.DRAW.value; // capture escapes to draw or better
                }
            }
            return Outcome.LOSS.value; // every move leads to opponent WIN
        }

        return Outcome.DRAW.value;
    }

    private int getResultOfMove(Move move) {
        BoardState boardState = MoveProcessor.movePiece(move);
        long canonicalIndex = SymmetricIndexing.normalizeToCanonicalIndex(
            boardState.getBoardMap(), true, p1PiecesNum, p2PiecesNum, table);
        return boardResults.get(canonicalIndex);
    }

    private int getResultOfMoveFromOpposite(Move move) {
        BoardState boardState = MoveProcessor.movePiece(move);
        long canonicalIndex = SymmetricIndexing.normalizeToCanonicalIndex(
            boardState.getBoardMap(), true, p2PiecesNum, p1PiecesNum, table);
        return oppositeBoard.get(canonicalIndex);
    }

    private int getResultOfMoveFromPrevious(Move move) {
        BoardState boardState = MoveProcessor.movePiece(move);
        long canonicalIndex = SymmetricIndexing.normalizeToCanonicalIndex(
            boardState.getBoardMap(), true, p2PiecesNum - 1, p1PiecesNum, table);
        return previousBoardResults.get(canonicalIndex);
    }

    private int[] getMapFromIndex(int i) {
        IndexingInput input = SymmetricIndexing.filePositionToIndexingInput(i, p1PiecesNum, p2PiecesNum, table);
        return input.getMapFromIndexingInput(boardSize);
    }

    public int getP1PiecesNum() { return p1PiecesNum; }
    public int getP2PiecesNum() { return p2PiecesNum; }
    public AtomicHugeByteArray getBoardResults() { return boardResults; }

    public void setOppositeBoard(AtomicHugeByteArray oppositeBoard) {
        this.oppositeBoard = oppositeBoard;
    }
}
