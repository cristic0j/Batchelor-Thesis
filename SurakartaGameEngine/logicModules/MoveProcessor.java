package logicModules;
import dataClasses.BoardState;
import dataClasses.Move;

public class MoveProcessor {
    
    public static BoardState movePiece(Move move){
        int[] previousBoardStateMatrix = move.getBoardState().getBoardMap();

        int previousBoardStatePlayer1PiecesNum = move.getBoardState().getPlayer1PiecesNum();
        int previousBoardStatePlayer2PiecesNum = move.getBoardState().getPlayer2PiecesNum();

        int valueAtOld = previousBoardStateMatrix[move.getOldCoordinate().value];
        int valueAtNew = previousBoardStateMatrix[move.getNewCoordinate().value];
        
        //Clone initial array and data
        int[] newBoardStateMatrix = previousBoardStateMatrix.clone();

        int newBoardStatePlayer1PiecesNum = previousBoardStatePlayer1PiecesNum;
        int newBoardStatePlayer2PiecesNum = previousBoardStatePlayer2PiecesNum;
        
        //Set old and new coordinate to their values
        //Decrease the pieces number
        switch (valueAtNew) {
            case 0:
                break;  
            case 1:
                newBoardStatePlayer1PiecesNum = newBoardStatePlayer1PiecesNum-1;
                break;
            case 2:
                newBoardStatePlayer2PiecesNum = newBoardStatePlayer2PiecesNum-1;
                break;
        }

        newBoardStateMatrix[move.getNewCoordinate().value] = valueAtOld;
        newBoardStateMatrix[move.getOldCoordinate().value] = 0;

        return new BoardState(newBoardStateMatrix, newBoardStatePlayer1PiecesNum, newBoardStatePlayer2PiecesNum);
    }   

}
