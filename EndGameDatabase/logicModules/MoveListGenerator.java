package EndGameDatabase.logicModules;

import dataClasses.BoardState;
import dataClasses.Coordinate;
import dataClasses.Move;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import logicModules.MoveValidator;

public class MoveListGenerator {

    public static List<Move> getSimpleMoveList(int[] board, int activePlayer){
        List<Move> moves = new ArrayList<>();
        
        for (int i = 0; i < board.length; i++) {
            if (board[i] == activePlayer) {
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

    public static List<Move> getTakeMoveList(int[] board, int activePlayer) {
        List<Move> moves = new ArrayList<>();
        List<Integer> activePlayerPieces = new ArrayList<>();
        List<Integer> opponentPieces = new ArrayList<>();
        
        // Determine the opponent based on the activePlayer
        int opponent = (activePlayer == 1) ? 2 : 1;
        
        // 1. Identify where the active player's pieces and the opponent's pieces are
        for (int i = 0; i < board.length; i++) {
            if (board[i] == activePlayer) {
                activePlayerPieces.add(i);
            } else if (board[i] == opponent) {
                opponentPieces.add(i);
            }
        }

        BoardState boardState = new BoardState(board, 0, 0);

        // 2. Iterate through active player pieces and check for valid takes against opponent pieces
        for (Integer activeCoo : activePlayerPieces) {
            for (Integer opponentCoo : opponentPieces) {
                
                Coordinate oldCoordinate = new Coordinate(activeCoo);
                Coordinate newCoordinate = new Coordinate(opponentCoo);

                Move move = new Move(boardState, oldCoordinate, newCoordinate);

                // The validator should now check if the move from active -> opponent is valid
                if (MoveValidator.takeValidator(move)) {
                    moves.add(move);
                }
            }
        }
        return moves;
    }
}
