package EndGameDatabaseSymmetry.logicModules.RetrogradeAnalysisExecutor;

import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabaseSymmetry.logicModules.RetrogradeAnalysis.SingleRetrogradeAnalysisSym;
import java.util.concurrent.CompletableFuture;
import EndGameDatabase.dataClasses.AtomicByteArray;

/**
 * Builds both AvB and BvA (A < B) in parallel, with each branch reading from
 * the other as its "opposite board" during the iterative phase.
 */
public class SymSingleRAParallelExecutor {

    private final int boardSize;
    private final BinomialTable table;

    private final int leftP1Num,  leftP2Num;
    private final int rightP1Num, rightP2Num;

    private final SingleRetrogradeAnalysisSym leftBranch;
    private final SingleRetrogradeAnalysisSym rightBranch;

    private final AtomicByteArray leftSingleRAResults;
    private final AtomicByteArray rightSingleRAResults;

    public SymSingleRAParallelExecutor(
        int boardSize,
        BinomialTable table,
        int leftP1Num,  int leftP2Num,
        int rightP1Num, int rightP2Num,
        AtomicByteArray leftPreviousResults,
        AtomicByteArray rightPreviousResults
    ) {
        this.boardSize  = boardSize;
        this.table      = table;
        this.leftP1Num  = leftP1Num;
        this.leftP2Num  = leftP2Num;
        this.rightP1Num = rightP1Num;
        this.rightP2Num = rightP2Num;

        // leftBranch(a,b) uses rightPrev = moves_{b-1}va
        leftBranch  = new SingleRetrogradeAnalysisSym(boardSize, leftP1Num,  leftP2Num,  table, rightPreviousResults);
        // rightBranch(b,a) uses leftPrev = moves_{a-1}vb (or 1v1 when a==1)
        rightBranch = new SingleRetrogradeAnalysisSym(boardSize, rightP1Num, rightP2Num, table, leftPreviousResults);

        leftSingleRAResults  = leftBranch.getBoardResults();
        rightSingleRAResults = rightBranch.getBoardResults();

        leftBranch.setOppositeBoard(rightSingleRAResults);
        rightBranch.setOppositeBoard(leftSingleRAResults);
    }

    public CompletableFuture<Void> start() {
        String label = "[Sym " + leftP1Num + "v" + leftP2Num
                     + " | " + rightP1Num + "v" + rightP2Num + "]";
        System.err.println(label + " Starting parallel analysis...");

        return CompletableFuture.runAsync(() -> {
            // Phase 1: initialize both branches in parallel.
            CompletableFuture.allOf(
                CompletableFuture.runAsync(() -> leftBranch.initialize()),
                CompletableFuture.runAsync(() -> rightBranch.initialize())
            ).join();

            // Phase 2: synchronized solving — both branches must finish each pass before
            // the next one starts so that each sees the other's latest results.
            boolean keepGoing = true;
            int iteration = 1;
            while (keepGoing) {
                boolean leftChanged  = CompletableFuture.supplyAsync(() -> leftBranch.processUncertainStatesPass()).join();
                boolean rightChanged = CompletableFuture.supplyAsync(() -> rightBranch.processUncertainStatesPass()).join();
                keepGoing = leftChanged || rightChanged;
                iteration++;
            }
            System.err.println(label + " Analysis complete. Converged in " + (iteration - 1) + " iterations.");
        });
    }

    public AtomicByteArray getLeftBranchResults()  { return leftSingleRAResults; }
    public AtomicByteArray getRightBranchResults() { return rightSingleRAResults; }
}
