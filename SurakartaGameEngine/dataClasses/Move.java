package dataClasses;
public class Move {

    private final BoardState boardState;
    
    private Coordinate oldCoordinate;
    private Coordinate newCoordinate;

    public Move(BoardState boardState, Coordinate oldCoordinate, Coordinate newCoordinate){
        this.boardState = boardState;
        this.oldCoordinate = oldCoordinate;
        this.newCoordinate = newCoordinate;
    }
    //Getters
    public BoardState getBoardState(){
        return boardState;
    }
    public Coordinate getOldCoordinate(){
        return oldCoordinate;
    }
    public Coordinate getNewCoordinate(){
        return newCoordinate;
    }
    //Setters
    public void setOldCoordinate(Coordinate coordinate){
        this.oldCoordinate = coordinate;
    }
}
