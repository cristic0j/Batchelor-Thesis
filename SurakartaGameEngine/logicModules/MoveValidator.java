package logicModules;
import dataClasses.BoardBorder;
import dataClasses.Move;

public class MoveValidator {

    // Circular rail jump table: CIRCLE_JUMPS[pos] = destination after a loop transition.
    // -1 means the square is not a rail junction. Mirrors CircleMoveMap exactly.
    private static final int[] CIRCLE_JUMPS = new int[36];
    static {
        java.util.Arrays.fill(CIRCLE_JUMPS, -1);
        CIRCLE_JUMPS[6]  = 1;   CIRCLE_JUMPS[1]  = 6;
        CIRCLE_JUMPS[12] = 2;   CIRCLE_JUMPS[2]  = 12;
        CIRCLE_JUMPS[4]  = 11;  CIRCLE_JUMPS[11] = 4;
        CIRCLE_JUMPS[3]  = 17;  CIRCLE_JUMPS[17] = 3;
        CIRCLE_JUMPS[24] = 31;  CIRCLE_JUMPS[31] = 24;
        CIRCLE_JUMPS[18] = 32;  CIRCLE_JUMPS[32] = 18;
        CIRCLE_JUMPS[23] = 33;  CIRCLE_JUMPS[33] = 23;
        CIRCLE_JUMPS[34] = 29;  CIRCLE_JUMPS[29] = 34;
    }

    // 0 - simple move
    // 1 - take
    public static int moveTypeIdentifier(Move move){

        int[] boardMatrix = move.getBoardState().getBoardMap();

        int oldCoordinateToX = move.getOldCoordinate().value % 6;
        int oldCoordinateToY = move.getOldCoordinate().value / 6;

        int newCoordinateToX = move.getNewCoordinate().value % 6;
        int newCoordinateToY = move.getNewCoordinate().value / 6;

        int valueAtNewCoordinates = boardMatrix[move.getNewCoordinate().value];
        int distanceX = Math.abs(oldCoordinateToX - newCoordinateToX);
        int distanceY = Math.abs(oldCoordinateToY - newCoordinateToY);

        //Check if the move is one square radius
        if (distanceX <= 1 && distanceY <= 1) {
            if (valueAtNewCoordinates == 0) {
                return 0;
            }
        }

        return 1;
    }

    public static boolean validateMove(Move move, int playerTurn){
        if (!turnValidator(move, playerTurn)) {
            return false;
        }

        if (moveTypeIdentifier(move) == 0) {
            return simpleMoveValidator(move);
        }

        return takeValidator(move);

    }

    public static boolean turnValidator(Move move, int playerTurn){
        int[] boardMatrix = move.getBoardState().getBoardMap();
        int valueAtOldCoordinates = boardMatrix[move.getOldCoordinate().value];

        return valueAtOldCoordinates == playerTurn;
    }

    public static boolean simpleMoveValidator(Move move){
        int[] boardMatrix = move.getBoardState().getBoardMap();

        int oldCoordinateToX = move.getOldCoordinate().value % 6;
        int oldCoordinateToY = move.getOldCoordinate().value / 6;

        int newCoordinateToX = move.getNewCoordinate().value % 6;
        int newCoordinateToY = move.getNewCoordinate().value / 6;

        int distanceX = Math.abs(oldCoordinateToX - newCoordinateToX);
        int distanceY = Math.abs(oldCoordinateToY - newCoordinateToY);

        if (move.getOldCoordinate().value < 0 ||
            move.getNewCoordinate().value < 0 ||
            move.getOldCoordinate().value > move.getBoardState().getBoardMap().length-1 ||
            move.getNewCoordinate().value > move.getBoardState().getBoardMap().length-1) {
            return false;
        }

        int valueAtOldCoordinates = boardMatrix[move.getOldCoordinate().value];
        int valueAtNewCoordinates = boardMatrix[move.getNewCoordinate().value];

        //Check if the move is one square radius
        if (distanceX > 1 || distanceY > 1) {
            return false;
        }

        //Check if starting move is not 0
        if (valueAtOldCoordinates == 0){
            return false;
        }

        //Check if new coordinate is 0
        if (valueAtNewCoordinates != 0) {
            return false;
        }

        return true;
    }

    // Game-engine entry point: unwraps the Move and delegates to the primitive overload.
    public static boolean takeValidator(Move move){
        return takeValidator(
            move.getBoardState().getBoardMap(),
            move.getOldCoordinate().value,
            move.getNewCoordinate().value
        );
    }

    // Primitive overload — no object allocation. Called directly by MoveListGenerator
    // to avoid creating Coordinate/Move/BoardState before knowing the move is valid.
    public static boolean takeValidator(int[] boardMatrix, int oldPos, int newPos){
        int oldVal = boardMatrix[oldPos];
        int newVal = boardMatrix[newPos];

        if (newVal == 0 || newVal == oldVal || oldVal == 0) {
            return false;
        }

        if (oldPos == 0 || oldPos == 5 || oldPos == 30 || oldPos == 35) {
            return false;
        }

        if (newPos == 0 || newPos == 5 || newPos == 30 || newPos == 35) {
            return false;
        }

        // Clone once and zero the starting square so the rail trace doesn't collide with it
        int[] board = boardMatrix.clone();
        board[oldPos] = 0;

        // Y == 0
        // Up  lastWasLoop=false  (piece about to enter the rail going up)
        // Down lastWasLoop=true  (piece is already facing away from the rail)
        if (BoardBorder.isTop(oldPos)) {
            return traceRail(board, oldPos, newPos, 0, false, false) ||
                   traceRail(board, oldPos, newPos, 2, false, true);
        }
        // X == 5
        else if (BoardBorder.isRight(oldPos)) {
            return traceRail(board, oldPos, newPos, 1, false, false) ||
                   traceRail(board, oldPos, newPos, 3, false, true);
        }
        // Y == 5
        else if (BoardBorder.isBottom(oldPos)) {
            return traceRail(board, oldPos, newPos, 0, false, true) ||
                   traceRail(board, oldPos, newPos, 2, false, false);
        }
        // X == 0
        else if (BoardBorder.isLeft(oldPos)) {
            return traceRail(board, oldPos, newPos, 1, false, true) ||
                   traceRail(board, oldPos, newPos, 3, false, false);
        }
        else {
            return traceRail(board, oldPos, newPos, 0, false, false) ||
                   traceRail(board, oldPos, newPos, 1, false, false) ||
                   traceRail(board, oldPos, newPos, 2, false, false) ||
                   traceRail(board, oldPos, newPos, 3, false, false);
        }
    }

    // Iterative rail tracer. Replaces the old recursiveValidation(RecursionInput).
    // All state is passed as primitives — zero heap allocation per step.
    //
    // board       : board with the moving piece already zeroed at oldPos
    // pos         : starting square (== oldPos, already 0 in board)
    // target      : destination square the attacker wants to reach
    // direction   : 0=up, 1=right, 2=down, 3=left
    // didLoop     : true once the path has completed at least one circular loop
    // lastWasLoop : true if the previous step was a rail jump (suppresses the next jump check)
    private static boolean traceRail(int[] board, int pos, int target,
                                     int direction, boolean didLoop, boolean lastWasLoop) {
        for (int step = 0; step <= 24; step++) {
            // Base case: arrived at target after completing at least one loop
            if (pos == target && didLoop) return true;

            // Mirrors the original "iteration > 24" guard — only base-case can win on step 24
            if (step == 24) break;

            // Current square must be passable (start was zeroed before call)
            if (board[pos] != 0) return false;

            // Dead-end corners: the path cannot continue in this direction
            if (pos == 0  && (direction == 0 || direction == 3)) return false;
            if (pos == 5  && (direction == 0 || direction == 1)) return false;
            if (pos == 30 && (direction == 2 || direction == 3)) return false;
            if (pos == 35 && (direction == 2 || direction == 1)) return false;

            boolean onBorder = (pos < 6) || (pos >= 30) || (pos % 6 == 0) || (pos % 6 == 5);

            if (!onBorder || lastWasLoop) {
                // Normal step: move one square in the current direction
                lastWasLoop = false;
                switch (direction) {
                    case 0: pos -= 6; break;
                    case 1: pos += 1; break;
                    case 2: pos += 6; break;
                    case 3: pos -= 1; break;
                }
            } else {
                // Rail junction: change direction (using current pos), then jump
                lastWasLoop = true;
                didLoop = true;
                switch (direction) {
                    case 0: direction = (pos <= 2)  ? 1 : 3; break;
                    case 1: direction = (pos <= 17) ? 2 : 0; break;
                    case 2: direction = (pos <= 32) ? 1 : 3; break;
                    case 3: direction = (pos <= 12) ? 2 : 0; break;
                }
                pos = CIRCLE_JUMPS[pos];
            }
        }
        return false;
    }
}
