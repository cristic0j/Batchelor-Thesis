package EndGameDatabase.logicModules.RetrogradeAnalysisExecutor;
import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.logicModules.RetrogradeAnalysis.OneVsOneAnalysis;
import java.util.concurrent.atomic.AtomicIntegerArray;



public class RootRAExecutor {
    // 1. Swapped to AtomicIntegerArray
    private AtomicIntegerArray rootRAResults;
    private final int boardSize;
    private final BinomialTable table;
    private OneVsOneAnalysis analysis;

    public RootRAExecutor(int boardSize, BinomialTable table){
        this.boardSize = boardSize;
        this.table = table;
        
        analysis = new OneVsOneAnalysis(boardSize, table);
    }

    public void start(){
        analysis.start();
        this.rootRAResults = analysis.getBoardResults();
    }

    public AtomicIntegerArray getBoardResults(){
        return rootRAResults;
    }
}
