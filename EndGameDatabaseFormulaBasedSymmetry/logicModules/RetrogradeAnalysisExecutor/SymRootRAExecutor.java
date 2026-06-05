package EndGameDatabaseFormulaBasedSymmetry.logicModules.RetrogradeAnalysisExecutor;

import EndGameDatabase.dataClasses.AtomicHugeByteArray;
import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabaseFormulaBasedSymmetry.logicModules.RetrogradeAnalysis.OneVsOneRetrogradeAnalysisSym;

public class SymRootRAExecutor {

    private AtomicHugeByteArray rootRAResults;
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

    public AtomicHugeByteArray getBoardResults() {
        return rootRAResults;
    }
}
