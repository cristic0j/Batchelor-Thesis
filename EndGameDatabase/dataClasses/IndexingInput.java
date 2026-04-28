package EndGameDatabase.dataClasses;

import java.util.ArrayList;
import java.util.List;

public class IndexingInput {
    private List<Integer> p1Pieces;
    private List<Integer> p2Pieces;
    private int boardSize;

    public IndexingInput(int boardSize){
        this.p1Pieces = new ArrayList<>();
        this.p2Pieces = new ArrayList<>();
        this.boardSize = boardSize;
    }

    public IndexingInput(List<Integer> p1Pieces, List<Integer> p2Pieces, int boardSize){
        this.p1Pieces = p1Pieces;
        this.p2Pieces = p2Pieces;
        this.boardSize = boardSize;
    }

    public void addPieceToP1List(int piece){
        p1Pieces.add(piece);
    }
    public void addPieceToP2List(int piece){
        p2Pieces.add(piece);
    }
    
    public List<Integer> getP1Pieces(){
        return p1Pieces;
    }
    public List<Integer> getP2Pieces(){
        return p2Pieces;
    }
    public int getBoardSize(){
        return boardSize;
    }

    public void displayIndexInputAsAMap(int boardSize){
        for (int i = 0; i < boardSize; i++) {
            if (p1Pieces.contains(i)) {
                System.out.print(1);
            }else if (p2Pieces.contains(i)) {
                System.out.print(2);
            }else{
                System.out.print(0);
            }
            if ((i+1) % 6 == 0) {
                System.err.println("");
            }
        }
    }

    public int[] getMapFromIndexingInput(int boardSize){
        int[] map = new int[boardSize];

        for (int i = 0; i < boardSize; i++) {
            if (p1Pieces.contains(i)) {
                map[i] = 1;
            }else if (p2Pieces.contains(i)) {
                map[i] = 2;
            }else{
                map[i] = 0;
            }
        }
        return map;
    }
}
