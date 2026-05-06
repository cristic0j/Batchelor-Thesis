package EndGameDatabaseSymmetry.logicModules.RetrogradeAnalysisExecutor;

import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabaseSymmetry.logicModules.RetrogradeAnalysis.SingleRetrogradeAnalysisSym;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class SymSingleRAExecutor {

    private final int boardSize;
    private final BinomialTable table;
    private final int p1Num;
    private final int p2Num;

    private final SingleRetrogradeAnalysisSym singleBranch;
    private final AtomicIntegerArray singleRAResults;

    public SymSingleRAExecutor(
        int boardSize,
        BinomialTable table,
        int p1Num,
        int p2Num,
        AtomicIntegerArray previousBoardResults
    ) {
        this.boardSize  = boardSize;
        this.table      = table;
        this.p1Num      = p1Num;
        this.p2Num      = p2Num;
        this.singleBranch   = new SingleRetrogradeAnalysisSym(boardSize, p1Num, p2Num, table, previousBoardResults);
        this.singleRAResults = singleBranch.getBoardResults();
    }

    public CompletableFuture<Void> start() {
        return CompletableFuture.runAsync(() -> {
            singleBranch.initialize();
            while (singleBranch.processUncertainStatesPass()) { /* iterate to convergence */ }
        });
    }

    public AtomicIntegerArray getBranchResults() {
        return singleRAResults;
    }
}
