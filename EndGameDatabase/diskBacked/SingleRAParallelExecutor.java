package EndGameDatabase.diskBacked;

import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.dataClasses.TableReader;
import EndGameDatabase.logicModules.RandomAccessFile.FileReaderWriter;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.CompletableFuture;

public class SingleRAParallelExecutor {

    private final int boardSize;
    private final BinomialTable table;
    private final long chunkSize;

    private final int leftP1Num;
    private final int leftP2Num;
    private final int rightP1Num;
    private final int rightP2Num;

    private final TableReader leftPreviousResults;
    private final TableReader rightPreviousResults;

    private final String leftWorkingFilePath;
    private final String rightWorkingFilePath;

    private final SingleRetrogradeAnalysis leftBranch;
    private final SingleRetrogradeAnalysis rightBranch;

    public SingleRAParallelExecutor(
        int boardSize,
        BinomialTable table,
        int leftP1Num,
        int leftP2Num,
        int rightP1Num,
        int rightP2Num,
        TableReader leftPreviousResults,
        TableReader rightPreviousResults,
        String leftWorkingFilePath,
        String rightWorkingFilePath,
        long chunkSize
    ) {
        this.boardSize = boardSize;
        this.table = table;
        this.chunkSize = chunkSize;
        this.leftP1Num = leftP1Num;
        this.leftP2Num = leftP2Num;
        this.rightP1Num = rightP1Num;
        this.rightP2Num = rightP2Num;
        this.leftPreviousResults = leftPreviousResults;
        this.rightPreviousResults = rightPreviousResults;
        this.leftWorkingFilePath = leftWorkingFilePath;
        this.rightWorkingFilePath = rightWorkingFilePath;

        leftBranch = new SingleRetrogradeAnalysis(boardSize, leftP1Num, leftP2Num, table, rightPreviousResults, leftWorkingFilePath, chunkSize);
        rightBranch = new SingleRetrogradeAnalysis(boardSize, rightP1Num, rightP2Num, table, leftPreviousResults, rightWorkingFilePath, chunkSize);
    }

    public CompletableFuture<Void> start() {
        String label = "[" + leftP1Num + "v" + leftP2Num + " | " + rightP1Num + "v" + rightP2Num + "]";
        System.err.println(label + " Starting parallel disk-backed analysis...");

        return CompletableFuture.runAsync(() -> {
            try {
                boolean leftExists  = new File(leftWorkingFilePath).exists();
                boolean rightExists = new File(rightWorkingFilePath).exists();

                if (leftExists && rightExists) {
                    System.err.println(label + " Resuming both branches from existing .tmp files.");
                } else {
                    // Initialize whichever branches are missing
                    CompletableFuture<Void> initLeft  = leftExists  ? CompletableFuture.completedFuture(null)
                        : CompletableFuture.runAsync(() -> { try { leftBranch.initialize();  } catch (IOException e) { throw new UncheckedIOException(e); } });
                    CompletableFuture<Void> initRight = rightExists ? CompletableFuture.completedFuture(null)
                        : CompletableFuture.runAsync(() -> { try { rightBranch.initialize(); } catch (IOException e) { throw new UncheckedIOException(e); } });

                    long initStart = System.currentTimeMillis();
                    CompletableFuture.allOf(initLeft, initRight).join();
                    System.err.println(label + " Init: " + (System.currentTimeMillis() - initStart) + "ms");
                }

                // Wire up cross-file readers for oppositeBoard lookups
                // Opened fresh each iteration so they see the latest file state
                long procStart = System.currentTimeMillis();
                boolean keepGoing = true;
                int iteration = 1;

                while (keepGoing) {
                    System.err.println(label + " Iteration " + iteration);

                    try (FileReaderWriter.OpenReader leftReader  = FileReaderWriter.openReader(leftWorkingFilePath,  leftBranch.getTotalPositions());
                         FileReaderWriter.OpenReader rightReader = FileReaderWriter.openReader(rightWorkingFilePath, rightBranch.getTotalPositions())) {

                        leftBranch.setOppositeBoard(rightReader);
                        rightBranch.setOppositeBoard(leftReader);

                        CompletableFuture<Boolean> passLeft  = CompletableFuture.supplyAsync(() -> {
                            try { return leftBranch.processUncertainStatesPass(); }
                            catch (IOException e) { throw new UncheckedIOException(e); }
                        });
                        CompletableFuture<Boolean> passRight = CompletableFuture.supplyAsync(() -> {
                            try { return rightBranch.processUncertainStatesPass(); }
                            catch (IOException e) { throw new UncheckedIOException(e); }
                        });

                        boolean leftChanged  = passLeft.join();
                        boolean rightChanged = passRight.join();
                        keepGoing = leftChanged || rightChanged;
                    }

                    iteration++;
                }

                System.err.println(label + " Processing: " + (System.currentTimeMillis() - procStart) + "ms");
                System.err.println(label + " Analysis complete. Converged in " + (iteration - 1) + " iterations.");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public String getLeftWorkingFilePath()  { return leftWorkingFilePath;  }
    public String getRightWorkingFilePath() { return rightWorkingFilePath; }
    public long getLeftTotalPositions()     { return leftBranch.getTotalPositions();  }
    public long getRightTotalPositions()    { return rightBranch.getTotalPositions(); }
}
