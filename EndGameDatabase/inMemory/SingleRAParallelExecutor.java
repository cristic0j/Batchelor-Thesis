package EndGameDatabase.inMemory;

import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.dataClasses.AtomicHugeByteArray;
import java.io.File;
import java.util.concurrent.CompletableFuture;

public class SingleRAParallelExecutor {

    private int boardSize;
    private BinomialTable table;

    private AtomicHugeByteArray leftPreviousResults;   // in-RAM: loaded from completed table
    private AtomicHugeByteArray rightPreviousResults;  // in-RAM: loaded from completed table

    private int leftP1Num;
    private int leftP2Num;

    private int rightP1Num;
    private int rightP2Num;

    private String leftCheckpointPath;
    private String rightCheckpointPath;

    private AtomicHugeByteArray leftSingleRAResults;   // in-RAM: being built
    private AtomicHugeByteArray rightSingleRAResults;  // in-RAM: being built

    private SingleRetrogradeAnalysis leftBranch;
    private SingleRetrogradeAnalysis rightBranch;

    public SingleRAParallelExecutor(
        int boardSize,
        BinomialTable table,
        int leftP1Num,
        int leftP2Num,
        int rightP1Num,
        int rightP2Num,
        AtomicHugeByteArray leftPreviousResults,
        AtomicHugeByteArray rightPreviousResults,
        String leftCheckpointPath,
        String rightCheckpointPath
    ){
        this.boardSize = boardSize;
        this.table = table;
        this.leftP1Num = leftP1Num;
        this.leftP2Num = leftP2Num;
        this.rightP1Num = rightP1Num;
        this.rightP2Num = rightP2Num;
        this.leftPreviousResults = leftPreviousResults;
        this.rightPreviousResults = rightPreviousResults;
        this.leftCheckpointPath = leftCheckpointPath;
        this.rightCheckpointPath = rightCheckpointPath;

        leftBranch = new SingleRetrogradeAnalysis(
            boardSize,
            leftP1Num,
            leftP2Num,
            table,
            rightPreviousResults
        );

        rightBranch = new SingleRetrogradeAnalysis(
            boardSize,
            rightP1Num,
            rightP2Num,
            table,
            leftPreviousResults
        );

        leftSingleRAResults = leftBranch.getBoardResults();
        rightSingleRAResults = rightBranch.getBoardResults();

        leftBranch.setOppositeBoard(rightSingleRAResults);
        rightBranch.setOppositeBoard(leftSingleRAResults);
    }

    public CompletableFuture<Void> start() {
        String label = "[" + leftP1Num + "v" + leftP2Num + " | " + rightP1Num + "v" + rightP2Num + "]";
        System.err.println(label + " Starting parallel analysis...");

        return CompletableFuture.runAsync(() -> {
            boolean bothCheckpointsExist = leftCheckpointPath != null
                && new File(leftCheckpointPath).exists()
                && new File(rightCheckpointPath).exists();

            if (bothCheckpointsExist) {
                try {
                    CompletableFuture<Void> loadLeft  = CompletableFuture.runAsync(() -> {
                        try { leftBranch.loadFromCheckpoint(leftCheckpointPath); }
                        catch (Exception e) { throw new RuntimeException(e); }
                    });
                    CompletableFuture<Void> loadRight = CompletableFuture.runAsync(() -> {
                        try { rightBranch.loadFromCheckpoint(rightCheckpointPath); }
                        catch (Exception e) { throw new RuntimeException(e); }
                    });
                    CompletableFuture.allOf(loadLeft, loadRight).join();
                    System.err.println(label + " Resumed from checkpoint.");
                } catch (Exception e) {
                    System.err.println(label + " Checkpoint load failed, starting fresh: " + e.getMessage());
                    bothCheckpointsExist = false;
                }
            }

            if (!bothCheckpointsExist) {
                // Phase 1: Initialize both branches in parallel
                long initStart = System.currentTimeMillis();
                CompletableFuture<Void> initLeft  = CompletableFuture.runAsync(() -> leftBranch.initialize());
                CompletableFuture<Void> initRight = CompletableFuture.runAsync(() -> rightBranch.initialize());
                CompletableFuture.allOf(initLeft, initRight).join();
                System.err.println(label + " Init: " + (System.currentTimeMillis() - initStart) + "ms");
                if (leftCheckpointPath != null) {
                    leftBranch.saveCheckpoint(leftCheckpointPath);
                    rightBranch.saveCheckpoint(rightCheckpointPath);
                }
            }

            // Phase 2: Synchronized solving loop
            long procStart = System.currentTimeMillis();
            boolean keepGoing = true;
            int iteration = 1;

            while (keepGoing) {
                System.err.println(label + " Iteration " + iteration);
                CompletableFuture<Boolean> passLeft  = CompletableFuture.supplyAsync(() -> leftBranch.processUncertainStatesPass());
                CompletableFuture<Boolean> passRight = CompletableFuture.supplyAsync(() -> rightBranch.processUncertainStatesPass());

                boolean leftChanged  = passLeft.join();
                boolean rightChanged = passRight.join();

                if (leftCheckpointPath != null) {
                    leftBranch.saveCheckpoint(leftCheckpointPath);
                    rightBranch.saveCheckpoint(rightCheckpointPath);
                }

                keepGoing = leftChanged || rightChanged;
                iteration++;
            }

            System.err.println(label + " Processing: " + (System.currentTimeMillis() - procStart) + "ms");
            System.err.println(label + " Analysis complete. Converged in " + (iteration - 1) + " iterations.");
        });
    }

    public AtomicHugeByteArray getLeftBranchResults() {
        return leftSingleRAResults;
    }

    public AtomicHugeByteArray getRightBranchResults() {
        return rightSingleRAResults;
    }
}
