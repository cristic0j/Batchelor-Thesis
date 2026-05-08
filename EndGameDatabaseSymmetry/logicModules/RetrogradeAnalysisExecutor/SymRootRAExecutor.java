package EndGameDatabaseSymmetry.logicModules.RetrogradeAnalysisExecutor;

import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabaseSymmetry.logicModules.RetrogradeAnalysis.OneVsOneRetrogradeAnalysisSym;
import EndGameDatabase.dataClasses.AtomicByteArray;

public class SymRootRAExecutor {

    private AtomicByteArray rootRAResults;
    private final int boardSize;
    private final BinomialTable table;
    private final OneVsOneRetrogradeAnalysisSym analysis;

    public SymRootRAExecutor(int boardSize, BinomialTable table) {
        this.boardSize = boardSize;
        this.table     = table;
        this.analysis  = new OneVsOneRetrogradeAnalysisSym(boardSize, table);
    }

    public void start() {
        analysis.start();
        this.rootRAResults = analysis.getBoardResults();
    }

    public AtomicByteArray getBoardResults() {
        return rootRAResults;
    }
}
