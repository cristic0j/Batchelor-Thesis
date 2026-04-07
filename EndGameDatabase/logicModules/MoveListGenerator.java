package EndGameDatabase.logicModules;

import dataClasses.BoardState;
import dataClasses.Coordinate;
import dataClasses.Move;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import logicModules.MoveValidator;

public class MoveListGenerator {

    public static List<Move> getSimpleMoveList(int[] board){
        List<Move> moves = new ArrayList<>();
        
        for (int i = 0; i < board.length; i++) {
            if (board[i] == 1) {
                //Common values
                BoardState boardState = new BoardState(board, 0, 0);
                Coordinate oldCoordinate = new Coordinate(i);

                //Create 8 moves
                Move upMove = new Move(boardState, oldCoordinate, new Coordinate(i-6));
                Move rightMove = new Move(boardState, oldCoordinate, new Coordinate(i+1));
                Move downMove = new Move(boardState, oldCoordinate, new Coordinate(i+6));
                Move leftMove = new Move(boardState, oldCoordinate, new Coordinate(i-1));

                Move upRightMove = new Move(boardState, oldCoordinate, new Coordinate(i-5));
                Move downRightMove = new Move(boardState, oldCoordinate, new Coordinate(i+7));
                Move upLeftMove = new Move(boardState, oldCoordinate, new Coordinate(i-7));
                Move downLeftMove = new Move(boardState, oldCoordinate, new Coordinate(i+5));


                Stream.of(upMove, rightMove, downMove, leftMove, upRightMove, downRightMove, upLeftMove, downLeftMove)
                    .filter(MoveValidator::simpleMoveValidator)
                    .forEach(moves::add);
            }
        }
        return moves;
    }

    public static List<Move> getTakeMoveList(int[] board){
        List<Move> moves = new ArrayList<>();
        List<Integer> player1PieceCoordinateList = new ArrayList<>();
        List<Integer> player2PieceCoordinateList = new ArrayList<>();
        
        //Get where player1 and player2 pieces are on the board
        for (int i = 0; i < board.length; i++) {
            if (board[i] == 1) {
                player1PieceCoordinateList.add(i);
            }else if (board[i] == 2) {
                player2PieceCoordinateList.add(i);
            }
        }

        BoardState boardState = new BoardState(board, 0, 0);

        //Iterate throught black pieces
        for (Integer player1PieceCoo : player1PieceCoordinateList) {
            for (Integer player2PieceCoo : player2PieceCoordinateList) {
                int player1Val = player1PieceCoo;
                int player2Val = player2PieceCoo;

                Coordinate oldCoordinate = new Coordinate(player1Val);
                Coordinate newCoordinate = new Coordinate(player2Val);

                Move move = new Move(boardState, oldCoordinate, newCoordinate);

                if (MoveValidator.takeValidator(move)) {
                    // System.err.println("Player 1: "+player1Val+" takes Player 2: "+player2Val);
                    moves.add(move);
                }
            }
        }
        return moves;
    }
}
