package EndGameDatabase.diskBacked;

import EndGameDatabase.dataClasses.AtomicPackedBitArray;
import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.dataClasses.IndexingInput;
import EndGameDatabase.dataClasses.Outcome;
import EndGameDatabase.dataClasses.TableReader;
import EndGameDatabase.logicModules.Indexing;
import EndGameDatabase.logicModules.MoveListGenerator;
import EndGameDatabase.logicModules.RandomAccessFile.FileReaderWriter;
import EndGameDatabase.logicModules.SymmetryBuffer;
import dataClasses.BoardState;
import dataClasses.Move;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.LongStream;
import logicModules.MoveProcessor;

public class SingleRetrogradeAnalysis {

    private final int p1PiecesNum;
    private final int p2PiecesNum;
    private final int boardSize;
    private final BinomialTable table;
    private final long chunkSize;
    private final long totalPositions;
    private final String workingFilePath;
    private final TableReader previousBoardResults;
    private TableReader oppositeBoard;

    private final AtomicPackedBitArray windowBuffer;
    private final SymmetryBuffer symmetryBuffer;
    private final boolean initSymmetry;

    private volatile long windowStart = -1;
    private volatile long currentWindowSize = 0;
    private volatile FileReaderWriter.OpenReader selfReader = null;

    public SingleRetrogradeAnalysis(
        int boardSize,
        int p1PiecesNum,
        int p2PiecesNum,
        BinomialTable table,
        TableReader previousBoardResults,
        String workingFilePath,
        long chunkSize,
        int symBufCapacity,
        boolean initSymmetry
    ) {
        this.boardSize = boardSize;
        this.p1PiecesNum = p1PiecesNum;
        this.p2PiecesNum = p2PiecesNum;
        this.table = table;
        this.previousBoardResults = previousBoardResults;
        this.workingFilePath = workingFilePath;
        this.chunkSize = chunkSize;
        this.initSymmetry = initSymmetry;
        this.totalPositions = Indexing.computeTheNumberOfPossiblePositions(p1PiecesNum, p2PiecesNum, boardSize, table);
        this.windowBuffer = new AtomicPackedBitArray(Math.min(chunkSize, totalPositions));
        this.symmetryBuffer = new SymmetryBuffer(symBufCapacity);
    }

    public void initialize() throws IOException {
        System.err.println("[" + p1PiecesNum + "v" + p2PiecesNum + "] Initializing (disk-backed)...");
        FileReaderWriter.createPrefilled(workingFilePath, totalPositions, Outcome.DRAW.value);

        long pos = 0;
        while (pos < totalPositions) {
            long wSize = Math.min(chunkSize, totalPositions - pos);
            windowStart = pos;
            currentWindowSize = wSize;

            windowBuffer.fill(Outcome.DRAW.value);

            if (p2PiecesNum == 1) {
                initSingleP2Window(pos, wSize);
            } else {
                initSingleP1Window(pos, wSize);
            }

            FileReaderWriter.writeWindow(workingFilePath, pos, wSize, windowBuffer);
            if (initSymmetry) symmetryBuffer.flush(workingFilePath);
            pos += chunkSize;
        }
        System.err.println("[" + p1PiecesNum + "v" + p2PiecesNum + "] Initialized.");
    }

    private void initSingleP2Window(long wStart, long wSize) {
        LongStream.range(0, wSize).parallel().forEach(i -> {
            if (windowBuffer.get(i) != Outcome.DRAW.value) return;
            int[] map = getMapFromIndex(wStart + i);
            List<Move> takeMoves = MoveListGenerator.getTakeMoveList(map, 1);
            if (!takeMoves.isEmpty()) {
                if (initSymmetry) {
                    setWithSymmetry(wStart + i, Outcome.WIN.value);
                } else {
                    windowBuffer.set(i, Outcome.WIN.value);
                }
            }
        });
    }

    private void initSingleP1Window(long wStart, long wSize) {
        LongStream.range(0, wSize).parallel().forEach(i -> {
            if (windowBuffer.get(i) != Outcome.DRAW.value) return;
            int[] map = getMapFromIndex(wStart + i);
            List<Move> takeMoves = MoveListGenerator.getTakeMoveList(map, 1);
            int result = evaluateP1SinglePieceState(takeMoves);
            if (result == Outcome.WIN.value) {
                if (initSymmetry) {
                    setWithSymmetry(wStart + i, result);
                } else {
                    windowBuffer.set(i, result);
                }
            }
        });
    }

    private int evaluateP1SinglePieceState(List<Move> moves) {
        if (moves.isEmpty()) return Outcome.DRAW.value;
        for (Move move : moves) {
            if (getResultOfMoveFromPrevious(move) == Outcome.LOSS.value) return Outcome.WIN.value;
        }
        return Outcome.DRAW.value;
    }

    public boolean processUncertainStatesPass() throws IOException {
        AtomicBoolean changed = new AtomicBoolean(false);

        try (FileReaderWriter.OpenReader reader = FileReaderWriter.openReader(workingFilePath, totalPositions)) {
            selfReader = reader;

            long pos = 0;
            while (pos < totalPositions) {
                long wSize = Math.min(chunkSize, totalPositions - pos);
                FileReaderWriter.readWindow(workingFilePath, pos, wSize, windowBuffer);
                windowStart = pos;
                currentWindowSize = wSize;

                final long wStart = pos;
                final long ws = wSize;

                LongStream.range(0, ws).parallel().forEach(i -> {
                    if (windowBuffer.get(i) == Outcome.DRAW.value) {
                        int newResult = evaluateStateOutcome(wStart + i);
                        if (newResult != Outcome.DRAW.value) {
                            setWithSymmetry(wStart + i, newResult);
                            changed.set(true);
                        }
                    }
                });

                FileReaderWriter.writeWindow(workingFilePath, pos, wSize, windowBuffer);
                symmetryBuffer.flush(workingFilePath);
                pos += chunkSize;
            }
        } finally {
            selfReader = null;
        }

        return changed.get();
    }

    private void setWithSymmetry(long globalIndex, int value) {
        int[] map = getMapFromIndex(globalIndex);
        int n = (int) Math.sqrt(boardSize);
        long[] symIndexes = Indexing.getAllTransformIndexes(map, n, false, table);
        for (long symIdx : symIndexes) {
            if (symIdx >= windowStart && symIdx < windowStart + currentWindowSize) {
                windowBuffer.set(symIdx - windowStart, value);
            } else {
                symmetryBuffer.add(symIdx, (byte) value, workingFilePath);
            }
        }
    }

    private int evaluateStateOutcome(long index) {
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
            if (nextStateResult != Outcome.WIN.value) allAreWinsP2 = false;
        }

        if (foundLossForP2) return Outcome.WIN.value;

        if (hasMoves && allAreWinsP2) {
            // Check capture moves regardless of symmetric/asymmetric — in the NvN case,
            // previousBoardResults holds the (N-1)vN table which is the correct sub-table
            // for post-capture positions.  Skipping this for p1==p2 caused positions with
            // a drawing capture to be incorrectly classified as LOSS.
            List<Move> takeMoves = MoveListGenerator.getTakeMoveList(map, 1);
            for (Move move : takeMoves) {
                if (getResultOfMoveFromPrevious(move) != Outcome.WIN.value) return Outcome.DRAW.value;
            }
            return Outcome.LOSS.value;
        }

        return Outcome.DRAW.value;
    }

    private int getResultAt(long position) {
        if (position >= windowStart && position < windowStart + currentWindowSize) {
            return windowBuffer.get(position - windowStart);
        }
        return selfReader.get(position);
    }

    private int getResultOfMove(Move move) {
        BoardState bs = MoveProcessor.movePiece(move);
        IndexingInput input = Indexing.createIndexingInput(bs.getBoardMap(), true);
        return getResultAt(Indexing.indexInputToFilePosition(input, table));
    }

    private int getResultOfMoveFromPrevious(Move move) {
        BoardState bs = MoveProcessor.movePiece(move);
        IndexingInput input = Indexing.createIndexingInput(bs.getBoardMap(), true);
        return previousBoardResults.get(Indexing.indexInputToFilePosition(input, table));
    }

    private int getResultOfMoveFromOpposite(Move move) {
        BoardState bs = MoveProcessor.movePiece(move);
        IndexingInput input = Indexing.createIndexingInput(bs.getBoardMap(), true);
        return oppositeBoard.get(Indexing.indexInputToFilePosition(input, table));
    }

    private int[] getMapFromIndex(long i) {
        return Indexing.filePositionToIndexingInput(i, boardSize, p1PiecesNum, p2PiecesNum, table)
                       .getMapFromIndexingInput(boardSize);
    }

    public void setOppositeBoard(TableReader oppositeBoard) {
        this.oppositeBoard = oppositeBoard;
    }

    public String getWorkingFilePath() { return workingFilePath; }
    public long getTotalPositions()    { return totalPositions; }
    public int getP1PiecesNum()        { return p1PiecesNum; }
    public int getP2PiecesNum()        { return p2PiecesNum; }
}
