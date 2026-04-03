package dataClasses;
public class RecursionInput {
    //The direcrtion where the piece moves
    //      0 - up
    //      1 - right
    //      2 - down
    //      3 - left
    private int direction;
    //Number uf iterations
    private int iteration;
    //Checks if a piece has looped at least once
    private boolean didPerformALoop;
    //Tells if a piece just executed a loop
    private boolean lastIterationWasALoop;
    //Current move with the boardState and the oldCoordinates - (what we are checking now), newCoordinates - (where we want to arrive)
    private Move move;
    

    public RecursionInput(int direction, int iteration, boolean didPerformALoop, boolean lastIterationWasALoop, Move move) {
        this.direction = direction;
        this.iteration = iteration;
        this.didPerformALoop = didPerformALoop;
        this.lastIterationWasALoop = lastIterationWasALoop;
        this.move = move;
    }
    
    //Getters
    public int getDirection(){
        return direction;
    }
    public int getIteration(){
        return iteration;
    }
    public boolean didPerformALoop(){
        return didPerformALoop;
    }
    public boolean lastIterationWasALoop(){
        return lastIterationWasALoop;
    }
    public Coordinate getOldCoordinate(){
        return move.getOldCoordinate();
    }
    public Coordinate getNewCoordinate(){
        return move.getNewCoordinate();
    }
    public int[] getBoardMap(){
        return move.getBoardState().getBoardMap();
    }

    //Settters
    public void increaseIteration(){
        this.iteration += 1;
    }
    public void setTrueDidPerformALoop(){
        this.didPerformALoop = true;
    }
    public void setTrueLastIterationWasALoop(){
        this.lastIterationWasALoop = true;
    }
    public void setFalseLastIterationWasALoop(){
        this.lastIterationWasALoop = false;
    }
    public void changeDirectionOrientationAfterLoop(){
        switch (this.direction) {
            case 0:
                if (getOldCoordinate().value <= 2) {
                    this.direction = 1;
                }else{
                    this.direction = 3;
                }
                break;
            case 1:
                if (getOldCoordinate().value <= 17) {
                    this.direction = 2;
                }else{
                    this.direction = 0;
                }
                break;
            case 2:
                if (getOldCoordinate().value <= 32) {
                    this.direction = 1;
                }else{
                    this.direction = 3;
                }
                break;
            case 3:
                if (getOldCoordinate().value <= 12) {
                    this.direction = 2;
                }else{
                    this.direction = 0;
                }
                break;
        }
    }
    public void setOldCoordinateWithNewFromCircleMoveMap(){
        this.move.setOldCoordinate(getCoordinateFromCircleMoveMap());
    }

    //Others
    public void moveOldCoordinateUp(){
        getOldCoordinate().value -= 6;
    }
    public void moveOldCoordinateRight(){
        getOldCoordinate().value += 1;
    }
    public void moveOldCoordinateDown(){
        getOldCoordinate().value += 6;
    }
    public void moveOldCoordinateLeft(){
        getOldCoordinate().value -= 1;
    }
    public boolean isOldCoordinateOnMargin(){
        return BoardBorder.isOnAnyBorder(getOldCoordinate().value);
    }
    public int getKeyForCircleMoveMap(){
        return getOldCoordinate().value;
    }
    public Coordinate getCoordinateFromCircleMoveMap(){
        return CircleMoveMap.moves.get(getKeyForCircleMoveMap());
    }

}
