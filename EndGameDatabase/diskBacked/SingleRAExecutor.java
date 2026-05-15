package EndGameDatabase.diskBacked;

import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.dataClasses.TableReader;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.CompletableFuture;

public class SingleRAExecutor {

    private final int boardSize;
    private final BinomialTable table;
    private final int p1Num;
    private final int p2Num;
    private final String workingFilePath;
    private final long chunkSize;

    private final SingleRetrogradeAnalysis branch;

    public SingleRAExecutor(
        int boardSize,
        BinomialTable table,
        int p1Num,
        int p2Num,
        TableReader previousBoardResults,
        String workingFilePath,
        long chunkSize
    ) {
        this.boardSize = boardSize;
        this.table = table;
        this.p1Num = p1Num;
        this.p2Num = p2Num;
        this.workingFilePath = workingFilePath;
        this.chunkSize = chunkSize;
        this.branch = new SingleRetrogradeAnalysis(boardSize, p1Num, p2Num, table, previousBoardResults, workingFilePath, chunkSize);
    }

    public CompletableFuture<Void> start() {
        String label = "[" + p1Num + "v" + p2Num + "]";
        return CompletableFuture.runAsync(() -> {
            try {
                if (new File(workingFilePath).exists()) {
                    System.err.println(label + " Resuming from existing .tmp file.");
                } else {
                    long initStart = System.currentTimeMillis();
                    branch.initialize();
                    System.err.println(label + " Init: " + (System.currentTimeMillis() - initStart) + "ms");
                }

                long procStart = System.currentTimeMillis();
                boolean keepGoing = true;
                int iteration = 1;
                while (keepGoing) {
                    System.err.println(label + " Iteration " + iteration);
                    keepGoing = branch.processUncertainStatesPass();
                    iteration++;
                }
                System.err.println(label + " Processing: " + (System.currentTimeMillis() - procStart) + "ms");
                System.err.println(label + " Analysis complete. Converged in " + (iteration - 1) + " iterations.");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public String getWorkingFilePath() { return workingFilePath; }
    public long getTotalPositions()    { return branch.getTotalPositions(); }
}
