package EndGameDatabase.logicModules.RetrogradeAnalysisExecutor;

import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.logicModules.RetrogradeAnalysis.SingleRetrogradeAnalysis;
import java.util.concurrent.CompletableFuture;
import EndGameDatabase.dataClasses.AtomicByteArray;

public class SingleRAExecutor {

    private int boardSize;
    private BinomialTable table;

    private int p1Num;
    private int p2Num;
    
    private AtomicByteArray previousBoardResults;

    private AtomicByteArray singleRAResults;

    private SingleRetrogradeAnalysis singleBranch;

    public SingleRAExecutor(
        int boardSize, 
        BinomialTable table, 
        int p1Num,
        int p2Num,
        AtomicByteArray previousBoardResults
    ){
        this.boardSize = boardSize;
        this.table = table;
        this.p1Num = p1Num;
        this.p2Num = p2Num;
        this.previousBoardResults = previousBoardResults;
        singleBranch = new SingleRetrogradeAnalysis(boardSize, p1Num, p2Num, table, previousBoardResults);

        singleRAResults = singleBranch.getBoardResults();
    }

    public CompletableFuture<Void> start() {
        return CompletableFuture.runAsync(() -> {
            singleBranch.initialize();
            boolean keepGoing = true;
            int iteration = 0;
            while (keepGoing) {
                iteration++;
                System.err.println("[" + p1Num + "v" + p2Num + "] Iteration " + iteration);
                keepGoing = singleBranch.processUncertainStatesPass();
            }
            System.err.println("[" + p1Num + "v" + p2Num + "] Converged in " + iteration + " iterations.");
            singleBranch.postProcessing();
        });
    }

    public AtomicByteArray getBranchResults(){
        return singleRAResults;
    }
}
