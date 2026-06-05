package EndGameDatabaseFormulaBasedSymmetry.logicModules.RetrogradeAnalysisExecutor;

import EndGameDatabase.dataClasses.AtomicHugeByteArray;
import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabaseFormulaBasedSymmetry.logicModules.RetrogradeAnalysis.SingleRetrogradeAnalysisSym;
import java.util.concurrent.CompletableFuture;

public class SymSingleRAExecutor {

    private final int boardSize;
    private final BinomialTable table;
    private final int p1Num;
    private final int p2Num;

    private final SingleRetrogradeAnalysisSym singleBranch;
    private final AtomicHugeByteArray singleRAResults;

    public SymSingleRAExecutor(
        int boardSize,
        BinomialTable table,
        int p1Num,
        int p2Num,
        AtomicHugeByteArray previousBoardResults
    ) {
        this.boardSize = boardSize;
        this.table     = table;
        this.p1Num     = p1Num;
        this.p2Num     = p2Num;
        this.singleBranch    = new SingleRetrogradeAnalysisSym(boardSize, p1Num, p2Num, table, previousBoardResults);
        this.singleRAResults = singleBranch.getBoardResults();
    }

    public CompletableFuture<Void> start() {
        String label = "[Sym " + p1Num + "v" + p2Num + "]";
        return CompletableFuture.runAsync(() -> {
            singleBranch.initialize();
            boolean keepGoing = true;
            int iteration = 1;
            while (keepGoing) {
                System.err.println(label + " Iteration " + iteration);
                keepGoing = singleBranch.processUncertainStatesPass();
                iteration++;
            }
            System.err.println(label + " Analysis complete. Converged in " + (iteration - 1) + " iterations.");
        });
    }

    public AtomicHugeByteArray getBranchResults() {
        return singleRAResults;
    }
}
