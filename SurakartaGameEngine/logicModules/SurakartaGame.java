package logicModules;
import dataClasses.BoardState;
import dataClasses.Coordinate;
import dataClasses.Move;
import java.util.Scanner;
import java.util.Stack;
public class SurakartaGame{
    public static SurakartaGame game;

    private Stack<BoardState> gameStack;
    private BoardState currentState;

    public SurakartaGame(BoardState startState){
        //Initialize the Stack
        this.gameStack = new Stack<>();
        this.gameStack.add(startState);

        //Initialize the currentState
        this.currentState = startState;
    }

    public static void startGame(){
        BoardState startState = SurakartaGame.startState();
        game = new SurakartaGame(startState);
        game.printCurrentBoardStateMatrix();
        Scanner scanner = new Scanner(System.in);
        while (game.currentState.getPlayer1PiecesNum() > 0 && game.currentState.getPlayer2PiecesNum() > 0) {

            System.err.println("From coordinate: ");
            int oldCoordinate = scanner.nextInt();
            
            System.err.println("To coordinate: ");
            int newCoordinate = scanner.nextInt();

            if (oldCoordinate < 0 || newCoordinate < 0 || oldCoordinate > 35 || newCoordinate > 35) {
                System.out.println("Bad input");
                continue;
            }

            Move move = new Move(game.getCurrentState(), new Coordinate(oldCoordinate), new Coordinate(newCoordinate));

            if (MoveValidator.validateMove(move, game.getPlayerTurn())) {
                BoardState newState = MoveProcessor.movePiece(move);
                game.updateStateDataStructures(newState);
                game.printCurrentBoardStateMatrix();
            }else{
                System.err.println("Invalid moove or coordinate, try again!");
            }   
        }
        if (game.currentState.getPlayer1PiecesNum() > 0) {
            System.err.println("Player1 Won!");
        }else if(game.currentState.getPlayer2PiecesNum() > 0){
            System.err.println("Player2 Won!");
        }
    }


    public static BoardState startState(){
        int player1PiecesNum = 1;
        int player2PiecesNum = 1;
        int[] boardMap = {
                        0,0,0,0,0,0,
                        1,0,0,0,0,0,
                        0,0,0,0,0,0,
                        0,0,0,0,0,2,
                        0,0,0,0,0,0,
                        0,0,0,0,0,0,
                    };
        //Create starting state
        BoardState startState = new BoardState(boardMap, player1PiecesNum, player2PiecesNum);
        return startState;
    }

    //Setters
    public void changeCurrentState(BoardState newState){
        this.currentState = newState;
    }

    public void addStateToStack(BoardState newState){
        this.gameStack.add(newState);
    }

    public void updateStateDataStructures(BoardState newState){
        changeCurrentState(newState);
        addStateToStack(newState);
    }

    //Getters

    public BoardState getCurrentState(){
        return this.currentState;
    }

    public int getStackLength(){
        int stackLength = this.gameStack.size();
        System.err.println("Stack length: "+ stackLength);
        return stackLength;
    }

    public int getPlayerTurn(){
        if (getStackLength() % 2 == 0) {
            System.err.println("Player1's turn");
            return 2;
        }
        
        System.err.println("Player2's turn");
        return 1;
    }
    
    public void printCurrentBoardStateMatrix(){
        int[] boardMatrix = this.currentState.getBoardMap();

        for (int i = 0; i < boardMatrix.length; i++) {
            if (i % 6 == 0) {
                System.err.println();
            }
            System.err.print(boardMatrix[i]);
        }
        System.err.println();
        System.err.println();
    }
}