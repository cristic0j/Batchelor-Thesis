package EndGameDatabase.logicModules.RetrogradeAnalysisExecutor;

import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.logicModules.RetrogradeAnalysis.SingleRetrogradeAnalysis;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class SingleRAParallelExecutor {

    private int boardSize;
    private BinomialTable table;

    private AtomicIntegerArray leftPreviousResults;
    private AtomicIntegerArray rightPreviousResults;  

    private int leftP1Num;
    private int leftP2Num;

    private int rightP1Num;
    private int rightP2Num;

    private AtomicIntegerArray leftSingleRAResults;
    private AtomicIntegerArray rightSingleRAResults;

    private SingleRetrogradeAnalysis leftBranch;
    private SingleRetrogradeAnalysis rightBranch;

    public SingleRAParallelExecutor(
        int boardSize, 
        BinomialTable table, 
        int leftP1Num, 
        int leftP2Num, 
        int rightP1Num, 
        int rightP2Num, 
        AtomicIntegerArray leftPreviousResults, 
        AtomicIntegerArray rightPreviousResults
    ){
        this.boardSize = boardSize;
        this.table = table;
        this.leftP1Num = leftP1Num;
        this.leftP2Num = leftP2Num;
        this.rightP1Num = rightP1Num;
        this.rightP2Num = rightP2Num;
        this.leftPreviousResults = leftPreviousResults;
        this.rightPreviousResults = rightPreviousResults;

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

            // Phase 1: Initialize both branches in parallel
            CompletableFuture<Void> initLeft = CompletableFuture.runAsync(() -> leftBranch.initialize());
            CompletableFuture<Void> initRight = CompletableFuture.runAsync(() -> rightBranch.initialize());

            // Wait for both to finish initializing before moving on
            CompletableFuture.allOf(initLeft, initRight).join();

            // Phase 2: Synchronized Solving Loop
            boolean keepGoing = true;
            int iteration = 1;

            while (keepGoing) {
                CompletableFuture<Boolean> passLeft = CompletableFuture.supplyAsync(() -> leftBranch.processUncertainStatesPass());
                CompletableFuture<Boolean> passRight = CompletableFuture.supplyAsync(() -> rightBranch.processUncertainStatesPass());

                boolean leftChanged = passLeft.join();
                boolean rightChanged = passRight.join();

                keepGoing = leftChanged || rightChanged;
                iteration++;
            }

            System.err.println(label + " Analysis complete. Converged in " + (iteration - 1) + " iterations.");
        });
    }

    public AtomicIntegerArray getLeftBranchResults(){
        return leftSingleRAResults;
    }

    public AtomicIntegerArray getRightBranchResults(){
        return rightSingleRAResults;
    }
}
