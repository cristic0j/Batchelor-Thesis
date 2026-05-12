package EndGameDatabase.inMemory;

import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.dataClasses.AtomicHugeByteArray;
import java.util.concurrent.CompletableFuture;

public class SingleRAExecutor {

    private int boardSize;
    private BinomialTable table;

    private int p1Num;
    private int p2Num;

    private AtomicHugeByteArray previousBoardResults;

    private AtomicHugeByteArray singleRAResults;

    private SingleRetrogradeAnalysis singleBranch;

    public SingleRAExecutor(
        int boardSize,
        BinomialTable table,
        int p1Num,
        int p2Num,
        AtomicHugeByteArray previousBoardResults
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
        String label = "[" + p1Num + "v" + p2Num + "]";
        return CompletableFuture.runAsync(() -> {
            long initStart = System.currentTimeMillis();
            singleBranch.initialize();
            System.err.println(label + " Init: " + (System.currentTimeMillis() - initStart) + "ms");

            long procStart = System.currentTimeMillis();
            boolean keepGoing = true;
            int iteration = 1;
            while (keepGoing) {
                System.err.println(label + " Iteration " + iteration);
                keepGoing = singleBranch.processUncertainStatesPass();
                iteration++;
            }
            System.err.println(label + " Processing: " + (System.currentTimeMillis() - procStart) + "ms");
            System.err.println(label + " Analysis complete. Converged in " + (iteration - 1) + " iterations.");
        });
    }

    public AtomicHugeByteArray getBranchResults() {
        return singleRAResults;
    }
}
