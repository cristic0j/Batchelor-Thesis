package EndGameDatabase.inMemory;

import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.dataClasses.IndexingInput;
import EndGameDatabase.dataClasses.Outcome;
import EndGameDatabase.logicModules.Indexing;
import EndGameDatabase.logicModules.MoveListGenerator;
import EndGameDatabase.logicModules.RandomAccessFile.FileReaderWriter;
import dataClasses.BoardState;
import dataClasses.Move;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import EndGameDatabase.dataClasses.AtomicPackedBitArray;
import java.util.stream.LongStream;
import logicModules.MoveProcessor;

public class SingleRetrogradeAnalysis {
    private AtomicPackedBitArray boardResults;
    private AtomicPackedBitArray previousBoardResults;  // in-RAM: loaded from completed table
    private AtomicPackedBitArray oppositeBoard;          // in-RAM: built live by parallel partner

    private final int p1PiecesNum;
    private final int p2PiecesNum;
    private final int boardSize;
    private BinomialTable table;

    public SingleRetrogradeAnalysis(
        int boardSize,
        int p1PiecesNum,
        int p2PiecesNum,
        BinomialTable table,
        AtomicPackedBitArray previousBoardResults
    ){
        this.boardSize = boardSize;
        this.p1PiecesNum = p1PiecesNum;
        this.p2PiecesNum = p2PiecesNum;
        this.table = table;
        this.previousBoardResults = previousBoardResults;
        long numberOfResults = Indexing.computeTheNumberOfPossiblePositions(p1PiecesNum, p2PiecesNum, boardSize, table);
        this.boardResults = new AtomicPackedBitArray(numberOfResults);
    }

    public void initialize() {
        System.err.println("[" + p1PiecesNum + "v" + p2PiecesNum + "] Starting analysis...");
        initializeBoardStates();
        System.err.println("[" + p1PiecesNum + "v" + p2PiecesNum + "] Initialized.");
    }

    private void initializeBoardStates() {
        boardResults.fill(Outcome.DRAW.value);
        if (p2PiecesNum == 1) {
            initializeForSingleP2Piece();
        } else {
            initializeForSingleP1Piece();
        }
    }

    private void initializeForSingleP2Piece() {
        LongStream.range(0, boardResults.size()).parallel().forEach(i -> {
            if (boardResults.get(i) != Outcome.DRAW.value) return;
            int[] map = getMapFromIndex(i);
            List<Move> takeMoves = MoveListGenerator.getTakeMoveList(map, 1);
            if (!takeMoves.isEmpty()) {
                setWithSymmetry(i, Outcome.WIN.value);
            }
        });
    }

    private void initializeForSingleP1Piece() {
        LongStream.range(0, boardResults.size()).parallel().forEach(i -> {
            if (boardResults.get(i) != Outcome.DRAW.value) return;
            int[] map = getMapFromIndex(i);
            List<Move> takeMoves = MoveListGenerator.getTakeMoveList(map, 1);
            int result = evaluateP1SinglePieceState(i, takeMoves);
            if (result == Outcome.WIN.value) {
                setWithSymmetry(i, result);
            }
        });
    }

    private int evaluateP1SinglePieceState(long stateIndex, List<Move> moves) {
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

        LongStream.range(0, boardResults.size()).parallel().forEach(i -> {
            if (boardResults.get(i) == Outcome.DRAW.value) {
                int newResult = evaluateStateOutcome(i);
                if (newResult != Outcome.DRAW.value) {
                    setWithSymmetry(i, newResult);
                    changed.set(true);
                }
            }
        });

        return changed.get();
    }

    private int evaluateStateOutcome(long index) {
        int[] map = getMapFromIndex(index);
        List<Move> simpleMoves = MoveListGenerator.getSimpleMoveList(map, 1);

        boolean foundLossForP2 = false;
        boolean allAreWinsP2 = true;
        boolean hasMoves = !simpleMoves.isEmpty();

        for (Move move : simpleMoves) {
            int nextStateResult;

            if (p1PiecesNum == p2PiecesNum) {
                nextStateResult = getResultOfMove(move);
            } else {
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
        }

        // P1 is LOSS if ALL moves (simple + capture) lead to P2 WIN.
        // Also covers the edge case where hasMoves=false (P1 has no simple moves at all —
        // allAreWinsP2 stays vacuously true, so we fall through to the capture check).
        if (hasMoves ? allAreWinsP2 : true) {
            List<Move> takeMoves = MoveListGenerator.getTakeMoveList(map, 1);
            boolean hasAnyMove = hasMoves || !takeMoves.isEmpty();
            if (!hasAnyMove) {
                return Outcome.LOSS.value; // no moves at all — stalemate is a loss
            }
            for (Move move : takeMoves) {
                int nextStateResult = getResultOfMoveFromPrevious(move);
                if (nextStateResult != Outcome.WIN.value) {
                    return Outcome.DRAW.value; // capture escapes to draw (or better)
                }
            }
            return Outcome.LOSS.value; // every move leads to P2 WIN
        }
        return Outcome.DRAW.value;
    }

    private int getResultOfMove(Move move) {
        BoardState boardState = MoveProcessor.movePiece(move);
        IndexingInput p2Input = Indexing.createIndexingInput(boardState.getBoardMap(), true);
        long filePosition = Indexing.indexInputToFilePosition(p2Input, table);
        return boardResults.get(filePosition);
    }

    private int getResultOfMoveFromPrevious(Move move) {
        BoardState boardState = MoveProcessor.movePiece(move);
        IndexingInput p2Input = Indexing.createIndexingInput(boardState.getBoardMap(), true);
        long filePosition = Indexing.indexInputToFilePosition(p2Input, table);
        return previousBoardResults.get(filePosition);
    }

    private int getResultOfMoveFromOpposite(Move move) {
        BoardState boardState = MoveProcessor.movePiece(move);
        IndexingInput p2Input = Indexing.createIndexingInput(boardState.getBoardMap(), true);
        long filePosition = Indexing.indexInputToFilePosition(p2Input, table);
        return oppositeBoard.get(filePosition);
    }

    private int[] getMapFromIndex(long i) {
        IndexingInput input = Indexing.filePositionToIndexingInput(i, boardSize, p1PiecesNum, p2PiecesNum, table);
        return input.getMapFromIndexingInput(boardSize);
    }

    private void setWithSymmetry(long index, int value) {
        int[] map = getMapFromIndex(index);
        int n = (int) Math.sqrt(boardSize);
        long[] symIndexes = Indexing.getAllTransformIndexes(map, n, false, table);
        for (long symIdx : symIndexes) {
            boardResults.set(symIdx, value);
        }
    }

    public int getP1PiecesNum() {
        return p1PiecesNum;
    }

    public int getP2PiecesNum() {
        return p2PiecesNum;
    }

    public AtomicPackedBitArray getBoardResults() {
        return this.boardResults;
    }

    public void setOppositeBoard(AtomicPackedBitArray oppositeBoard) {
        this.oppositeBoard = oppositeBoard;
    }

    public void loadFromCheckpoint(String path) throws IOException {
        FileReaderWriter.readIntoArray(path, boardResults);
    }

    public void saveCheckpoint(String path) {
        try {
            String tmp = path + ".tmp";
            FileReaderWriter.writeToFile(tmp, boardResults, boardResults.size());
            Files.move(Paths.get(tmp), Paths.get(path), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("[" + p1PiecesNum + "v" + p2PiecesNum + "] Checkpoint save failed: " + e.getMessage());
        }
    }
}
