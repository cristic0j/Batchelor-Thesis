package EndGameDatabase.diskBacked;

import EndGameDatabase.dataClasses.AtomicHugeByteArray;
import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.dataClasses.IndexingInput;
import EndGameDatabase.dataClasses.Outcome;
import EndGameDatabase.dataClasses.TableReader;
import EndGameDatabase.logicModules.Indexing;
import EndGameDatabase.logicModules.MoveListGenerator;
import EndGameDatabase.logicModules.RandomAccessFile.FileReaderWriter;
import dataClasses.BoardState;
import dataClasses.Move;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.LongStream;
import logicModules.MoveProcessor;

public class SingleRetrogradeAnalysis {

    private final int p1PiecesNum;
    private final int p2PiecesNum;
    private final int boardSize;
    private final BinomialTable table;
    private final long chunkSize;
    private final long flushThreshold;
    private final long totalPositions;
    private final String workingFilePath;
    private final TableReader previousBoardResults;
    private TableReader oppositeBoard;

    private final AtomicHugeByteArray windowBuffer;

    private volatile long windowStart = -1;
    private volatile long currentWindowSize = 0;
    private volatile FileReaderWriter.OpenReader selfReader = null;

    // Deferred symmetric writes for positions outside the current window
    private final ConcurrentHashMap<Long, Integer> deferredWrites = new ConcurrentHashMap<>();

    public SingleRetrogradeAnalysis(
        int boardSize,
        int p1PiecesNum,
        int p2PiecesNum,
        BinomialTable table,
        TableReader previousBoardResults,
        String workingFilePath,
        long chunkSize,
        long flushThreshold
    ) {
        this.boardSize = boardSize;
        this.p1PiecesNum = p1PiecesNum;
        this.p2PiecesNum = p2PiecesNum;
        this.table = table;
        this.previousBoardResults = previousBoardResults;
        this.workingFilePath = workingFilePath;
        this.chunkSize = chunkSize;
        this.flushThreshold = flushThreshold;
        this.totalPositions = Indexing.computeTheNumberOfPossiblePositions(p1PiecesNum, p2PiecesNum, boardSize, table);
        this.windowBuffer = new AtomicHugeByteArray(Math.min(chunkSize, totalPositions));
    }

    public void initialize() throws IOException {
        System.err.println("[" + p1PiecesNum + "v" + p2PiecesNum + "] Initializing (disk-backed)...");
        FileReaderWriter.createPrefilled(workingFilePath, totalPositions, Outcome.DRAW.value);

        long pos = 0;
        while (pos < totalPositions) {
            flushDeferredWrites();
            long wSize = Math.min(chunkSize, totalPositions - pos);
            windowStart = pos;
            currentWindowSize = wSize;

            final long ws = wSize;
            LongStream.range(0, ws).parallel().forEach(i -> windowBuffer.set(i, Outcome.DRAW.value));

            if (p2PiecesNum == 1) {
                initSingleP2Window(pos, wSize);
            } else {
                initSingleP1Window(pos, wSize);
            }

            FileReaderWriter.writeWindow(workingFilePath, pos, wSize, windowBuffer);
            pos += chunkSize;
        }
        flushDeferredWrites();
        System.err.println("[" + p1PiecesNum + "v" + p2PiecesNum + "] Initialized.");
    }

    private void initSingleP2Window(long wStart, long wSize) {
        LongStream.range(0, wSize).parallel().forEach(i -> {
            long absIdx = wStart + i;
            int[] map = getMapFromIndex(absIdx);
            List<Move> takeMoves = MoveListGenerator.getTakeMoveList(map, 1);
            if (!takeMoves.isEmpty()) {
                setWithSymmetryInBuffer(absIdx, Outcome.WIN.value);
            }
        });
    }

    private void initSingleP1Window(long wStart, long wSize) {
        LongStream.range(0, wSize).parallel().forEach(i -> {
            long absIdx = wStart + i;
            int[] map = getMapFromIndex(absIdx);
            List<Move> takeMoves = MoveListGenerator.getTakeMoveList(map, 1);
            int result = evaluateP1SinglePieceState(takeMoves);
            if (result == Outcome.WIN.value) {
                setWithSymmetryInBuffer(absIdx, result);
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
                flushDeferredWrites();
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
                            setWithSymmetryInBuffer(wStart + i, newResult);
                            changed.set(true);
                        }
                    }
                });

                FileReaderWriter.writeWindow(workingFilePath, pos, wSize, windowBuffer);
                pos += chunkSize;
            }
        } finally {
            selfReader = null;
            flushDeferredWrites();
        }

        return changed.get();
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
            if (p1PiecesNum != p2PiecesNum) {
                List<Move> takeMoves = MoveListGenerator.getTakeMoveList(map, 1);
                for (Move move : takeMoves) {
                    if (getResultOfMoveFromPrevious(move) != Outcome.WIN.value) return Outcome.DRAW.value;
                }
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

    private void setInBuffer(long absPosition, int value) {
        if (absPosition >= windowStart && absPosition < windowStart + currentWindowSize) {
            windowBuffer.set(absPosition - windowStart, value);
        } else {
            deferredWrites.put(absPosition, value);
            if (deferredWrites.size() >= flushThreshold) {
                flushDeferredWrites();
            }
        }
    }

    private void setWithSymmetryInBuffer(long index, int value) {
        int[] map = getMapFromIndex(index);
        int n = (int) Math.sqrt(boardSize);
        long[] symIndexes = Indexing.getAllTransformIndexes(map, n, false, table);
        for (long symIdx : symIndexes) {
            setInBuffer(symIdx, value);
        }
    }

    private synchronized void flushDeferredWrites() {
        if (deferredWrites.isEmpty()) return;

        // Drain entries atomically per key
        List<Map.Entry<Long, Integer>> toFlush = new ArrayList<>(deferredWrites.size());
        deferredWrites.forEach((k, v) -> {
            if (deferredWrites.remove(k, v)) toFlush.add(Map.entry(k, v));
        });

        if (toFlush.isEmpty()) return;

        toFlush.sort(Map.Entry.comparingByKey());

        try {
            FileReaderWriter.writeScattered(workingFilePath, toFlush);
        } catch (IOException e) {
            System.err.println("[" + p1PiecesNum + "v" + p2PiecesNum + "] Deferred flush failed: " + e.getMessage());
        }
        System.err.println("[" + p1PiecesNum + "v" + p2PiecesNum + "] Flushed " + toFlush.size() + " deferred symmetric writes.");
    }

    public void setOppositeBoard(TableReader oppositeBoard) {
        this.oppositeBoard = oppositeBoard;
    }

    public String getWorkingFilePath() {
        return workingFilePath;
    }

    public long getTotalPositions() {
        return totalPositions;
    }

    public int getP1PiecesNum() {
        return p1PiecesNum;
    }

    public int getP2PiecesNum() {
        return p2PiecesNum;
    }
}
