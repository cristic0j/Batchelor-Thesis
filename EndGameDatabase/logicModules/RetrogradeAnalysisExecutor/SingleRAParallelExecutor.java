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
        System.err.println("Starting Synchronized Parallel Retrograde Analysis...");

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
                // System.err.println("Starting iteration " + iteration); // Optional: helpful for debugging

                // Launch one pass for both branches
                CompletableFuture<Boolean> passLeft = CompletableFuture.supplyAsync(() -> leftBranch.processUncertainStatesPass());
                CompletableFuture<Boolean> passRight = CompletableFuture.supplyAsync(() -> rightBranch.processUncertainStatesPass());

                // .join() acts as a barrier. The loop will not continue until BOTH are done.
                boolean leftChanged = passLeft.join();
                boolean rightChanged = passRight.join();

                // If EITHER branch found a new result, we must run another full pass for both.
                keepGoing = leftChanged || rightChanged;
                iteration++;
            }

            System.err.println("Both branches have converged and finished in " + (iteration - 1) + " iterations!");
        });
    }

    public AtomicIntegerArray getLeftBranchResults(){
        return leftSingleRAResults;
    }

    public AtomicIntegerArray getRightBranchResults(){
        return rightSingleRAResults;
    }
}
