package EndGameDatabase.logicModules.RetrogradeAnalysisExecutor;
import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.logicModules.RetrogradeAnalysis.OneVsOneRetrogradeAnalysis;
import EndGameDatabase.dataClasses.AtomicByteArray;



public class RootRAExecutor {
    // 1. Swapped to AtomicByteArray
    private AtomicByteArray rootRAResults;
    private final int boardSize;
    private final BinomialTable table;
    private OneVsOneRetrogradeAnalysis analysis;

    public RootRAExecutor(int boardSize, BinomialTable table){
        this.boardSize = boardSize;
        this.table = table;
        
        analysis = new OneVsOneRetrogradeAnalysis(boardSize, table);
    }

    public void start(){
        analysis.start();
        this.rootRAResults = analysis.getBoardResults();
    }

    public AtomicByteArray getBoardResults(){
        return rootRAResults;
    }
}
