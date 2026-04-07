package logicModules;
import dataClasses.BoardBorder;
import dataClasses.BoardState;
import dataClasses.Coordinate;
import dataClasses.Move;
import dataClasses.RecursionInput;

public class MoveValidator {
    // 0 - simple move
    // 1 - take
    public static int moveTypeIdentifier(Move move){

        int[] boardMatrix = move.getBoardState().getBoardMap();
        
        int oldCoordonateToX = move.getOldCoordinate().value % 6;
        int oldCoordonateToY = move.getOldCoordinate().value / 6;

        int newCoordonateToX = move.getNewCoordinate().value % 6;
        int newCoordonateToY = move.getNewCoordinate().value / 6;

        int valueAtNewCoordinates = boardMatrix[move.getNewCoordinate().value];
        int distanceX = Math.abs(oldCoordonateToX - newCoordonateToX);
        int distanceY = Math.abs(oldCoordonateToY - newCoordonateToY);
        
        //Check if the move is one sqare radius
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

        int oldCoordonateToX = move.getOldCoordinate().value % 6;
        int oldCoordonateToY = move.getOldCoordinate().value / 6;

        int newCoordonateToX = move.getNewCoordinate().value % 6;
        int newCoordonateToY = move.getNewCoordinate().value / 6;

        int distanceX = Math.abs(oldCoordonateToX - newCoordonateToX);
        int distanceY = Math.abs(oldCoordonateToY - newCoordonateToY);

        if (move.getOldCoordinate().value < 0 || move.getNewCoordinate().value < 0) {
            return false;
        }

        int valueAtOldCoordinates = boardMatrix[move.getOldCoordinate().value];
        int valueAtNewCoordinates = boardMatrix[move.getNewCoordinate().value];

        //Check if the move is one sqare radius
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

    public static boolean takeValidator(Move move){
        int[] boardMatrix = move.getBoardState().getBoardMap();
        int valueAtOldCoordinates = boardMatrix[move.getOldCoordinate().value];
        int valueAtNewCoordinates = boardMatrix[move.getNewCoordinate().value];

        if (valueAtNewCoordinates == 0 || valueAtNewCoordinates == valueAtOldCoordinates || valueAtOldCoordinates == 0) {
            return false;
        }

        // recursive validation ...
        // Set the starting position to 0 so we dont bump into it while checking
        int[] recursiveCheckMatrixCopy = boardMatrix.clone();
        recursiveCheckMatrixCopy[move.getOldCoordinate().value] = 0;
        

        Move recursiveUpMove = new Move(new BoardState(recursiveCheckMatrixCopy,0,0), 
                            new Coordinate(move.getOldCoordinate().value), new Coordinate(move.getNewCoordinate().value));

        Move recursiveRightMove = new Move(new BoardState(recursiveCheckMatrixCopy,0,0), 
                            new Coordinate(move.getOldCoordinate().value), new Coordinate(move.getNewCoordinate().value));

        Move recursiveDownMove = new Move(new BoardState(recursiveCheckMatrixCopy,0,0), 
                            new Coordinate(move.getOldCoordinate().value), new Coordinate(move.getNewCoordinate().value));

        Move recursiveLeftMove = new Move(new BoardState(recursiveCheckMatrixCopy,0,0), 
                            new Coordinate(move.getOldCoordinate().value), new Coordinate(move.getNewCoordinate().value));

        // Y == 0
        //Up lastIterationWasALoop = false
        //Down lastIterationWasALoop = true
        //Left/Right always false
        if (BoardBorder.isTop(move.getOldCoordinate().value)) {
            return (
                recursiveValidation(new RecursionInput(0, 1, false, false, recursiveUpMove))||
                recursiveValidation(new RecursionInput(2, 1, false, true, recursiveDownMove))
            );
        }
        // X == 5
        //Right lastIterationWasALoop = false
        //Left lastIterationWasALoop = true
        //Up/Down always false
        else if(BoardBorder.isRight(move.getOldCoordinate().value)){
            return (
                recursiveValidation(new RecursionInput(1, 1, false, false, recursiveRightMove))||
                recursiveValidation(new RecursionInput(3, 1, false, true, recursiveLeftMove))
            );
        }
        // Y == 5
        //Up lastIterationWasALoop = true
        //Down lastIterationWasALoop = false
        //Left/Right always false
        else if(BoardBorder.isBottom(move.getOldCoordinate().value)){
            return (
                recursiveValidation(new RecursionInput(0, 1, false, true, recursiveUpMove))||
                recursiveValidation(new RecursionInput(2, 1, false, false, recursiveDownMove))
            );
        }
        // X == 0
        //Right lastIterationWasALoop = true
        //Left lastIterationWasALoop = false
        //Up/Down always false
        else if(BoardBorder.isLeft(move.getOldCoordinate().value)){
            return (
                recursiveValidation(new RecursionInput(1, 1, false, true, recursiveRightMove))||
                recursiveValidation(new RecursionInput(3, 1, false, false, recursiveLeftMove))
            );
        }
        else{
            return (
                recursiveValidation(new RecursionInput(0, 1, false, false, recursiveUpMove))||
                recursiveValidation(new RecursionInput(1, 1, false, false, recursiveRightMove))||
                recursiveValidation(new RecursionInput(2, 1, false, false, recursiveDownMove))||
                recursiveValidation(new RecursionInput(3, 1, false, false, recursiveLeftMove))
            );
        }
    }

    public static boolean recursiveValidation(RecursionInput input){
        //Base case(If coordinates match)
        if ((input.getOldCoordinate().value == input.getNewCoordinate().value) && input.didPerformALoop()) {
            return true;
        }

        //Return False if iteration is higher than 24 or oldCoordinate value is different than 0 
        if (input.getIteration() > 24 || input.getBoardMap()[input.getOldCoordinate().value] != 0 ) {
            return false;
        }

        //If top left corner is reached
        if ((input.getOldCoordinate().value == 0) && (input.getDirection() == 0 || input.getDirection() == 3)) {
            return false;
        }
        //If top right corner is reached
        if ((input.getOldCoordinate().value == 5) && (input.getDirection() == 0 || input.getDirection() == 1)) {
            return false;
        }
        //If bottom left corner is reached
        if ((input.getOldCoordinate().value == 30) && (input.getDirection() == 2 || input.getDirection() == 3)) {
            return false;
        }
        //If bottom right corner is reached
        if ((input.getOldCoordinate().value == 35) && (input.getDirection() == 2 || input.getDirection() == 1)) {
            return false;
        }


        //Increase iteration
        input.increaseIteration();

        //Is not on margin or just came out of a loop
        if (!input.isOldCoordinateOnMargin() || input.lastIterationWasALoop()) {
            //Set last iteration was a loop to false
            input.setFalseLastIterationWasALoop();
            switch (input.getDirection()) {
                case 0 -> input.moveOldCoordinateUp();
                case 1 -> input.moveOldCoordinateRight();
                case 2 -> input.moveOldCoordinateDown();
                case 3 -> input.moveOldCoordinateLeft();
            }
            return recursiveValidation(input);
        }else{
            //Set last iteration was a loop to true
            input.setTrueLastIterationWasALoop();
            //Set true did perform at least a loop  
            input.setTrueDidPerformALoop();
            //Change the direction
            input.changeDirectionOrientationAfterLoop();
            //Set the new coordinate
            input.setOldCoordinateWithNewFromCircleMoveMap();
            
            return recursiveValidation(input);
        }
    } 
}
