package EndGameDatabase.logicModules;

import dataClasses.BoardState;
import dataClasses.Coordinate;
import dataClasses.Move;
import java.util.ArrayList;
import java.util.List;
import logicModules.MoveValidator;

public class MoveListGenerator {

    public static List<Move> getSimpleMoveList(int[] board, int activePlayer){
        List<Move> moves = new ArrayList<>();

        // One BoardState shared across all moves — the board array is never mutated here
        BoardState boardState = new BoardState(board, 0, 0);

        for (int i = 0; i < board.length; i++) {
            if (board[i] != activePlayer) continue;

            int col = i % 6;
            int row = i / 6;

            // One Coordinate for this piece, shared across its valid moves (read-only after creation)
            Coordinate oldCoord = new Coordinate(i);

            // Check validity inline (bounds via row/col, target must be empty)
            // then allocate a Move only for directions that actually pass.
            if (row > 0             && board[i-6] == 0) moves.add(new Move(boardState, oldCoord, new Coordinate(i-6)));
            if (col < 5             && board[i+1] == 0) moves.add(new Move(boardState, oldCoord, new Coordinate(i+1)));
            if (row < 5             && board[i+6] == 0) moves.add(new Move(boardState, oldCoord, new Coordinate(i+6)));
            if (col > 0             && board[i-1] == 0) moves.add(new Move(boardState, oldCoord, new Coordinate(i-1)));
            if (row > 0 && col < 5  && board[i-5] == 0) moves.add(new Move(boardState, oldCoord, new Coordinate(i-5)));
            if (row < 5 && col < 5  && board[i+7] == 0) moves.add(new Move(boardState, oldCoord, new Coordinate(i+7)));
            if (row > 0 && col > 0  && board[i-7] == 0) moves.add(new Move(boardState, oldCoord, new Coordinate(i-7)));
            if (row < 5 && col > 0  && board[i+5] == 0) moves.add(new Move(boardState, oldCoord, new Coordinate(i+5)));
        }
        return moves;
    }

    public static List<Move> getTakeMoveList(int[] board, int activePlayer) {
        int opponent = (activePlayer == 1) ? 2 : 1;

        // Collect piece positions into int[] — no boxing, no ArrayList overhead
        int[] activePieces   = new int[board.length];
        int[] opponentPieces = new int[board.length];
        int activeCount = 0, opponentCount = 0;

        for (int i = 0; i < board.length; i++) {
            if      (board[i] == activePlayer) activePieces[activeCount++]     = i;
            else if (board[i] == opponent)     opponentPieces[opponentCount++] = i;
        }

        List<Move> moves = new ArrayList<>();

        // One BoardState shared across all valid moves — board is never mutated here
        BoardState boardState = new BoardState(board, 0, 0);

        // Validate using primitives first; allocate Coordinate/Move only on success
        for (int a = 0; a < activeCount; a++) {
            for (int o = 0; o < opponentCount; o++) {
                if (MoveValidator.takeValidator(board, activePieces[a], opponentPieces[o])) {
                    moves.add(new Move(boardState,
                        new Coordinate(activePieces[a]),
                        new Coordinate(opponentPieces[o])));
                }
            }
        }
        return moves;
    }
}
