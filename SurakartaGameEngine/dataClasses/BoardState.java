package dataClasses;
//Represents the state of the board at some instance
public class BoardState {
    //A coordonate can be:
        // 0 - empty
        // 1 - player 1's piece
        // 2 - player 2's piece
    private final int[] boardMap;
    private final int player1PiecesNum;
    private final int player2PiecesNum;

    public BoardState(int[] boardMap, int player1PiecesNum, int player2PiecesNum) {
        this.boardMap = boardMap;
        this.player1PiecesNum = player1PiecesNum;
        this.player2PiecesNum = player2PiecesNum;
    }
    //Getters
    public int[] getBoardMap(){
        return this.boardMap;
    }
    public int getPlayer1PiecesNum(){
        return this.player1PiecesNum;
    }
    public int getPlayer2PiecesNum(){
        return this.player2PiecesNum;
    }
}
