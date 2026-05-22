import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;


/**
 A class representing a board for playing games with.
 An instance of Board  remembers the state of a single
 board and supports methods for making
 moves on the board as well as selecting computer moves.
 */
public final class Board {
    // /////////////////////////////////////////////////////////////
    // Constants for board state

    // Size of the board
	public static final int MAX_SIZE = 16;
	
    public static int WIDTH;
    public static int HEIGHT;
    public static int SQUARE;
    public static int REAL_BORDER;
    public static int ORIGIN;       

    // Constants for the 2 different sides
    public static final int W_SIDE = -1;
    public static final int B_SIDE = 1;

    // Constants for the 3 different symbols
    public static final byte EMPTY = 0;
    public static final byte W_PIECE = -1;
    public static final byte B_PIECE = 1;
 
    // /////////////////////////////////////////////////////////////
    // Constants for move generation

    // Time limit for the computer to make a move.
    // give it 5 seconds.
    public static int TIME_LIMIT = 5000;
    public static boolean isTournament =false;
    
    public static long TOURNAMENTTIME = 1800000;
    public static long tournamentTime = TOURNAMENTTIME;
   
    // Winning scores for a move
    public static final int WIN_SCORE = 10000;
    
    //repetition
    public static final int REPETITION = 2;
    public static final int NO_OCCURENCE = 0;
    
    public static final int B_WIN_SCORE = WIN_SCORE;
    public static final int W_WIN_SCORE = -WIN_SCORE;
    
    // Dwaw value/ never zero;
    public static final int DRAW_SCORE = -150;
 
    // Move generator for this game.
    private Generator generator;

    // Static Board Evaluator for this game.
    private Game sbe;
  
    public int numberOfMovesPlayed;
  
    // Two-dimensional array for the contents of the board.
    // Notice that this array is public now.  This will hurt encapsulation
    // and reusability, but it will make the move generation code
    // much easier to write.
    public final byte[][] space;
    public final int[][] indexSpace;
  
    public long hashValue = 0;
    public long blackHashValue = 0;
    public long whiteHashValue = 0;
  
    private AbstractPlayer blackPlayer = null;
    private AbstractPlayer whitePlayer = null;
  
    private Game sbe_B = null, sbe_W = null;
  
    public int B_Counter = 0, W_Counter = 0;
    public int bKCounter = 0, wKCounter = 0;
    public int bPCounter = 0, wPCounter = 0;
  
    public int[]Horizontal_Lines;
    public int[]Vertical_Lines;
    public int[]SWNE_Lines;
    public int[]SENW_Lines;
    
    public int[] Horizontal_Bit_Lines;
    public int[] Vertical_Bit_Lines;
    public int[] SWNE_Bit_Lines;
    public int[] SENW_Bit_Lines;
  
    public int sumOfRowBP = 0;
    public int sumOfRowWP = 0;
    public int sumOfColBP = 0;
    public int sumOfColWP = 0;
    
    public CompressedBoard[]history;
  
    private AbstractPlayer currentPlayer;
  
    public long posB, posW;
  
    public final Piece[] arrayW,
            arrayB;                       
  
    public int sideToMove;    
  
    public Board() {
        this(null, null);
    }
  
    /**
    Constructor used to regenerate board for Proof-Number Search
    */
    public Board(ProofNode node) { 
    	
        this(null, null);
   
        history = new CompressedBoard[2]; 
        history[0] = new CompressedBoard();
        history[1] = new CompressedBoard();
       
        setBoard(node);        
    }
    
    /**
    Make an empty board object.
    */  
    public Board(Game sbeval) {
     this(sbeval.getGenerator(), sbeval); 	
  }
   
    /**
     Make an empty board object. 
     */
    public Board(Generator gen, Game sbeval) {
        // Remember copies of the given generator and Game.
        generator = gen;
        sbe = sbeval;
      
       space = new byte[HEIGHT][WIDTH];
        indexSpace = new int[HEIGHT][WIDTH];
        arrayW = new Piece[Board.SQUARE];
        arrayB = new Piece[Board.SQUARE];
      
       //to remember the history
       history = new CompressedBoard[(sbe==null)?400:sbe.getMaxLength()+1];
       
       for (int i = history.length; --i>=0;) {
                history[i] = new CompressedBoard();
       }
  
        Horizontal_Lines = new int[HEIGHT];
  
        Vertical_Lines = new int[WIDTH];
  
        SWNE_Lines = new int[Board.HEIGHT + Board.WIDTH - 1];
  
        SENW_Lines = new int[SWNE_Lines.length];
   
        Horizontal_Bit_Lines = new int[HEIGHT];
        Vertical_Bit_Lines = new int[WIDTH];
        SWNE_Bit_Lines = new int[SWNE_Lines.length];
        SENW_Bit_Lines = new int[SWNE_Lines.length];
    }
    
    /**
    Constructor used to regenerate board for Proof-Number Search
    */    
    public void setBoard(final ProofNode node) {
        history[0].posW = posW = node.posR;
        history[0].posB = posB = node.posB;
        history[0].conversion = true;
        numberOfMovesPlayed = 0;
        B_Counter = 0;
        W_Counter = 0;
        sideToMove = node.side;
        Piece piece;

        for (int row = 0; row < HEIGHT; row++) {
            for (int col = 0; col < WIDTH; col++) {    
                if (((posW >>> (row * Board.HEIGHT + col)) & 1L) == 1L) {
                     piece = new Piece(row, col, Board.W_PIECE);

                    arrayW[W_Counter] = piece;
                    indexSpace[row][col] = W_Counter;
                    W_Counter++;
                    space[row][col] = Board.W_PIECE;
                } else if (((posB >>> (row * Board.HEIGHT + col)) & 1L) == 1L) {
                     piece = new Piece(row, col, Board.B_PIECE);

                    arrayB[B_Counter] = piece;
                    indexSpace[row][col] = B_Counter;
                    B_Counter++;
                    space[row][col] = Board.B_PIECE;
                } else {
                    space[row][col] = Board.EMPTY;
                }
            } 
        }  
        
        setLines();
    } 

    /**
     Fill in the board with a starting configuration
     */
    public void startingConfig(final Game s_B, final Game  s_R) { 
         sbe.startingConfiguration(this);
        
        setPlayer(B_SIDE, s_B);
        setPlayer(W_SIDE, s_R);  
        numberOfMovesPlayed = 0;
          
        initialise(sideToMove);  
    }

    /**
     Make a copy of this board.
     */
    public Board duplicate() {
  
      return  duplicate(new Board(generator, sbe));
    }
    
    public Board duplicate(Board dup) {     
    
        dup.sideToMove = sideToMove;

        // Fill in all of the spaces of dup to match this
        for (int row = HEIGHT; --row>=0;) {
            //System.arraycopy(space[row], 0, dup.space[row], 0, WIDTH);
            //System.arraycopy(indexSpace[row], 0, dup.indexSpace[row], 0, WIDTH);
        	byte dupspaceR[] = dup.space[row];
        	byte spaceR[] = space[row];
        	int dupIspaceR[] =  dup.indexSpace[row];
        	int IspaceR[] = indexSpace[row];
        	
        	for (int col = WIDTH; --col>=0;) {
        		dupspaceR[col] = spaceR[col];
        		dupIspaceR[col] = IspaceR[col];
        	}
        }

        dup.blackPlayer = blackPlayer; 
        dup.whitePlayer = whitePlayer;
    
        dup.hashValue = hashValue;
        dup.blackHashValue = blackHashValue;
        dup.whiteHashValue = whiteHashValue;

        dup.sbe_W = sbe_W;
        dup.sbe_B = sbe_B;
             
        dup.B_Counter = B_Counter;
        dup.W_Counter = W_Counter;
        dup.bKCounter = bKCounter;
        dup.wKCounter = wKCounter;
        dup.bPCounter = bPCounter;
        dup.wPCounter = wPCounter;
    
        
        //create new objects, cloning is too slow
        for (int i = B_Counter; --i >= 0;) {
            dup.arrayB[i] = new Piece(arrayB[i]);//(Piece) arrayB[i].clone();
        }
     
        for (int i = W_Counter; --i >= 0;) {
            dup.arrayW[i] = new Piece(arrayW[i]);//(Piece) arrayW[i].clone();
        }
    
        for (int i =  Board.WIDTH; --i>=0;) {
            dup.Horizontal_Lines[i] = Horizontal_Lines[i];
            dup.Vertical_Lines[i] = Vertical_Lines[i];
            dup.Horizontal_Bit_Lines[i] = Horizontal_Bit_Lines[i];
            dup.Vertical_Bit_Lines[i] = Vertical_Bit_Lines[i];
        }
           
        for (int i = SWNE_Lines.length; --i >= 0;) {
            dup.SENW_Lines[i] = SENW_Lines[i];
            dup.SWNE_Lines[i] = SWNE_Lines[i];
            dup.SENW_Bit_Lines[i] = SENW_Bit_Lines[i];
            dup.SWNE_Bit_Lines[i] = SWNE_Bit_Lines[i];
        }
       
        dup.numberOfMovesPlayed = numberOfMovesPlayed;
        dup.posB = posB;
        dup.posW = posW;
        dup.sumOfRowBP = sumOfRowBP;
        dup.sumOfRowWP = sumOfRowWP;
        dup.sumOfColBP = sumOfColBP;
        dup.sumOfColWP = sumOfColWP;
   
        for (int i = history.length; --i>=0;) {
            dup.history[i].copy(history[i]);
        }
      
        return dup;
    }
    
    public final void setSpace(byte[][] array){
    	
       for (int row = HEIGHT; --row>=0;) {
            System.arraycopy(array[row], 0, space[row], 0, WIDTH);         
        }
    	
    }
    
    public final void setSpace(long pB, long pW){
    	 for (int row = HEIGHT; --row>=0;) {
             for (int col =  WIDTH; --col>=0;) {    
                 if (((pW >>> (row * Board.HEIGHT + col)) & 1L) == 1L) {
                      space[row][col] = Board.W_PIECE;
                 } else if (((pB >>> (row * Board.HEIGHT + col)) & 1L) == 1L) {
                       space[row][col] = Board.B_PIECE;
                 } else {
                     space[row][col] = Board.EMPTY;
                 }
             } 
         }  
    	
    	
    }
    
    public static void setSize(Game game){
    	
    	setSize(game.getSize(),game.getSize());
    }
    
    public static void setSize(int width, int height){
    	
    	WIDTH =width;
        HEIGHT = height;
        SQUARE = WIDTH*HEIGHT;
        REAL_BORDER = WIDTH - 1;
        ORIGIN = Board.WIDTH + (Board.HEIGHT - 1) - 2;          
    }
  
    public void clear() {
 
        // Clear history
        numberOfMovesPlayed = 0;
  
        // Clear Board
        for (int i = Board.HEIGHT; --i >= 0;) {
            java.util.Arrays.fill(space[i], Board.EMPTY);
        }
    }
    
    public boolean equals(Object b) {
    	
    	if(b instanceof Board)
    		return equals((Board) b);
    	
    	return false;
    	
    }
  
    public boolean equals(Board b) {
        for (int row = 0; row < HEIGHT; row++) {
            for (int col = 0; col < WIDTH; col++) {
                if (this.space[ row ][ col ] != b.space[ row ][ col ]) {
                    return false;
                }
            }
        }
  
        return true; 
    }
    
   public Game getGame(){
   	return sbe;
  }
   

   
   public Game getSBE(int side){
	   	return (side==Board.B_SIDE)?sbe_B:sbe_W;
	  }
   
   public AbstractPlayer getPlayer(int side){
	   	return (side==Board.B_SIDE)?this.blackPlayer:this.whitePlayer;
	  }
  
   public String toString() {
 
        final StringBuffer sBuffer = new StringBuffer(Board.WIDTH*(Board.HEIGHT +2));
      
        for (int row = 0; row < HEIGHT; row++) {
            for (int col = 0; col < WIDTH; col++) {  
                switch(space[row][col]){
                   case Board.EMPTY: sBuffer.append('.'); break;
                   case Board.B_PIECE: sBuffer.append('+'); break;
                   default : sBuffer.append('-'); break;
                }
            }
                
            sBuffer.append("\r\n");
        }
     
        return sBuffer.toString();
    }

 
  
    /**
     Return the list of legal moves from the current configuration.
     */
    public Move legalMoves() { 
        // Just let our move generator do the work.
        return generator.generateMoves(this);
    }

    /**
     See if there is a win.  Return the side (W_SIDE or B_SIDE)
     of the winning player.
     */
    public int checkForWin() {
        final int score = sbe.terminalEvaluation(this, -sideToMove);
     
        if (score == W_WIN_SCORE) {
            return W_SIDE;
        }

        if (score == B_WIN_SCORE) {
            return B_SIDE;
        }
          
        if (score == DRAW_SCORE) {
            return DRAW_SCORE;
        }

        return EMPTY;
    }
    
    public final boolean checkOnDraw(){
    	    	
    	   return sbe.checkOnDraw(this)==REPETITION;
   }	
       
 
    // /////////////////////////////////////////////////////////////////////////
    // Move selection code
    // /////////////////////////////////////////////////////////////////////////

    /**
     Choose a move for the computer and update the board with that move.
     */
    public Move computerMove(int ID) { 

        initialise();
        if (ID == B_SIDE) {
            currentPlayer = blackPlayer;
        } else {
            currentPlayer = whitePlayer;
        }
      
        return  currentPlayer.search(this);     
    }
  
    public void  quitSearch() {
        if (currentPlayer != null) {
            currentPlayer.quitSearch();
        } 	
    }

    public boolean isPondering(Board b) {
        if (currentPlayer != null) {
            return currentPlayer.isPondering(b);
        }
     
        return false;
    }
  
    public void setPlayer(int ID, Game sbeval) {
    	
       	TranspositionTable table = null;
       	
         	 
        if (ID == B_SIDE) {
        	 sbe_B = sbeval;
        	if(blackPlayer instanceof Player)  table =((Player)(blackPlayer)).getTransTable();
            blackPlayer = null;
        } else {
        	sbe_W = sbeval;
        	if(whitePlayer instanceof Player)  table =((Player)(whitePlayer)).getTransTable();
            whitePlayer = null;
        } 
     	
        AbstractPlayer p = null;
    	
        switch (sbeval.playerMode) {
        case AB:
             	
            p = new Player(sbeval,table);
            break;

        case DP:
        
            try {
					p = new DualPlayer(new PNSSquarePlayer(sbeval.getClass().getDeclaredConstructor().newInstance()),new Player(sbeval,table));
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException e1) {
					e1.printStackTrace();
				} 
           
            break;

        case PNS:
            p = new PNSPlayer(sbeval);
            break;

        case PNS2:
            p = new PNSSquarePlayer(sbeval);
            break;

        case DFPN:
            p = new DFPNPlayer(sbeval);
            break;

        case PDS:
            p = new PDSPlayer(sbeval);
            break;   
            
        case COMB:
        	
        	if(sbeval.getGameName().equalsIgnoreCase("Clobber"))
        		//try{
        			
        			p = new CombinatorialPlayer(sbeval,table);
        		
        	//catch(Exception e){
        		// = new CombinatorialPlayer(sbeval,table);}
        	else
        		p = new Player(sbeval,table);
        	
            break;    
                       
        case PAMC:
        	 p = new ParABGlobalMCPlayer(sbeval);
            break;
            
        case PA:
            p = new ParallelPlayer(sbeval,2);
            break;  
            
        case PAPN:
            p = new ParPNSSquarePlayer(sbeval,2);
            break;  
                      
            
        case SHOT:
        	
        
            p = new  ShotPlayer(sbeval);     	
             break;  
               
        case SHOT2:
            p = new  Shot2Player(sbeval);     	
             break;  
        
       case NegaMax:
           p = new NegaMaxPlayer(sbeval);     	
     	
         break;     
            }
   
        if (ID == B_SIDE) {
            blackPlayer = p;
        } else {
            whitePlayer = p;
        } 
    }
  
    public boolean opponentsTime(int ID) { 

        if (ID == B_SIDE) {
            if (blackPlayer != null) {
                return blackPlayer.opponentsTime(this, sideToMove);
            }
        } else {
            if (whitePlayer != null) {
                return whitePlayer.opponentsTime(this, sideToMove);
            }
        }
  
        return false;
    }

    
    public void initialise(){
    	initialise(sideToMove);
    }
    	   
    
    public void initialise(int side) {
 
        initialiseLight(side);
        setLines(); 
    }
    
    //hack should be corrected
    public void initialiseLight(int side){
    sideToMove = side;

    B_Counter = 0;
    W_Counter = 0;
    bKCounter = 0;
    wKCounter = 0;
    bPCounter = 0;
    wPCounter = 0;
    sumOfRowBP = 0;
    sumOfRowWP = 0;
    sumOfColBP = 0;
    sumOfColWP = 0;
                      
      for (int i = 0; i < Board.WIDTH; i++) {
        for (int j = 0; j < Board.HEIGHT; j++) {

            switch (space[i][j]) {
            
            case Board.EMPTY: break;
            
            case Board.B_PIECE:
     		     
            	if(arrayB[B_Counter]==null)
                	arrayB[B_Counter] = new Piece(i, j, Board.B_PIECE);
                else
                	arrayB[B_Counter].setValue(i,j,Board.B_PIECE);

                indexSpace[i][j] = B_Counter;
                B_Counter++;
                bPCounter++;
                sumOfRowBP += i;
                sumOfColBP += j;
                                    
                break;
	                   
             case Board.W_PIECE:
        	   
                if(arrayW[W_Counter]==null)
                	arrayW[W_Counter] = new Piece(i, j, Board.W_PIECE);
                else
                	arrayW[W_Counter].setValue(i,j,Board.W_PIECE);
                
                indexSpace[i][j] = W_Counter;
                W_Counter++;
                wPCounter++;
                sumOfRowWP += i;
                sumOfColWP += j;
        	     
                break;
             default: 
            	 if(space[i][j]>=Board.B_PIECE){
            		 if(arrayB[B_Counter]==null)
                     	arrayB[B_Counter] = new Piece(i, j, space[i][j]);
                     else
                     	arrayB[B_Counter].setValue(i,j,space[i][j]);

                     indexSpace[i][j] = B_Counter;
                     B_Counter++;
                     bKCounter++;
                     sumOfRowBP += i;
                     sumOfColBP += j;
            		 
            	 }
            	 else{
            	     if(arrayW[W_Counter]==null)
                     	arrayW[W_Counter] = new Piece(i, j, space[i][j]);
                     else
                     	arrayW[W_Counter].setValue(i,j,space[i][j]);
                     
                     indexSpace[i][j] = W_Counter;
                     W_Counter++;
                     wKCounter++;
                     sumOfRowWP += i;
                     sumOfColWP += j;
            		 
            		 
            	 }
      		 }
        }
    }
         
   // TranspositionTable.setHashValue(this, side);
  
    blackHashValue = TranspositionTable.getHashSideValue(this, Board.B_SIDE);
    whiteHashValue = TranspositionTable.getHashSideValue(this, Board.W_SIDE);
    hashValue = blackHashValue^whiteHashValue^TranspositionTable.getHashMoveValue(side); 
    
    if (numberOfMovesPlayed == 0) {
        
        history[0].compress(this);
        history[0].conversion = true;

        posB = history[0].posB;
        posW = history[0].posW;
    }
    
   }
   
    private void setLines() {    
    
    	int pieces;
        // Horizontal;
        for (int row = Board.HEIGHT; --row>=0;) {
            pieces = 0;

            Horizontal_Bit_Lines[row] = 0;
        
            for (int col = Board.WIDTH; --col>=0;) {
            	final byte rspace[] = space[ row ];
                if (rspace[ col ] != Board.EMPTY) {
                    pieces++;
          
                    if (rspace[ col ] >= 0) {
                        Horizontal_Bit_Lines[row] ^= (1 << col);
                    } else {
                        Horizontal_Bit_Lines[row] ^= (1 << (col + Board.WIDTH));
                    }
                }
            }
            Horizontal_Lines[row] = pieces;
            // Horizontal_Index_Lines[row] =compressBitmap[Horizontal_Bit_Lines[row]];
     
        // Diagonaal ZO NW  
       
            pieces = 0;

            SENW_Bit_Lines[ row] = 0;
    
            for (int j = 0; row + j < Board.HEIGHT && j < Board.WIDTH; j++) {
                if (space[row + j ][j] != Board.EMPTY) {
                    pieces++;
         
                    if (space[ row + j ][j] >= 0) {
                        SENW_Bit_Lines[row] ^= (1 << j);
                    } else {
                        SENW_Bit_Lines[row] ^= (1 << (j + Board.WIDTH));
                    }
         
                }
            }
            SENW_Lines[ row ] = pieces;
            // SENW_Index_Lines[ row ] = compressBitmap[ SENW_Bit_Lines[ row]];
        } 
        
         
        // Verticaal   
        for (int col =  Board.WIDTH; --col>=0;) {
            pieces = 0; 
           
            Vertical_Bit_Lines[col] = 0;
      
            for (int row =  Board.HEIGHT; --row>=0;) {
                if (space[row][col] != Board.EMPTY) {
                    if (space[ row ][ col ] >= 0) {
                        Vertical_Bit_Lines[col] ^= (1 << row);
                    } else {
                        Vertical_Bit_Lines[col] ^= (1 << (row + Board.WIDTH));
                    }
         
                    pieces++;
                }
            }
            Vertical_Lines[col] = pieces;
            // Vertical_Index_Lines[col] =compressBitmap[Vertical_Bit_Lines[col]];
        
        // ZW NO  
        
            pieces = 0;
            
            SWNE_Bit_Lines[col] = 0;
            
            for (int row = 0; row < Board.HEIGHT && col >= row; row++) {
                if (space[row][ col - row] != Board.EMPTY) {
                    pieces++;
         
                    if (space[row][col - row] >= 0) {
                        SWNE_Bit_Lines[col] ^= 1 << (col - row);
                    } else {
                        SWNE_Bit_Lines[col] ^= (1 << (col - row + Board.WIDTH));
                    }
         
                }
            }
            SWNE_Lines[col] = pieces;
            // SWNE_Index_Lines[col] = compressBitmap[SWNE_Bit_Lines[col]];
        }
    
        for (int row = 1; row < Board.HEIGHT; row++) {
            pieces = 0;

            SWNE_Bit_Lines[REAL_BORDER + row] = 0;
     
            for (int j = 0; row + j < Board.HEIGHT && REAL_BORDER - j >= 0; j++) {
                if (space[row + j][REAL_BORDER - j] != Board.EMPTY) {
                    pieces++;
          
                    if (space[row + j][REAL_BORDER - j] >= 0) {
                        SWNE_Bit_Lines[REAL_BORDER + row] ^= (1
                                << (REAL_BORDER - row - j));
                    } else {
                        SWNE_Bit_Lines[REAL_BORDER + row] ^= (1
                                << ((REAL_BORDER - row - j) + Board.WIDTH));
                    }
         
                }
            }
            SWNE_Lines[REAL_BORDER + row] = pieces;
            // SWNE_Index_Lines[REAL_BORDER + row] = compressBitmap[SWNE_Bit_Lines[REAL_BORDER + row]];
        } 
   
     
  
        for (int col = 1; col < Board.WIDTH; col++) {
            pieces = 0;

            SENW_Bit_Lines[ col + REAL_BORDER] = 0;
    
            for (int j = 0; col + j < Board.WIDTH && j <= Board.HEIGHT; j++) {
                if (space[j][ col + j] != Board.EMPTY) {
                    pieces++;
                    if (space[j][col + j] >= 0) {
                        SENW_Bit_Lines[ col + REAL_BORDER] ^= (1 << j);
                    } else {
                        SENW_Bit_Lines[ col + REAL_BORDER] ^= (1
                                << (j + Board.WIDTH));
                    }
         
                }
            }
            SENW_Lines[col + REAL_BORDER] = pieces;  
            // SENW_Index_Lines[col +REAL_BORDER] = compressBitmap[SENW_Bit_Lines[col +REAL_BORDER]];
        }
         
    }
     
    public final boolean isPrevOccupied(int i, int j, int side) {

        if (numberOfMovesPlayed == 0) {
            return false;
        }

        if (side == Board.B_SIDE) {
            return ((history[numberOfMovesPlayed - 1].posB
                    >>> (i * Board.HEIGHT + j))& 1L) == 1L;
        } 
        
            return ((history[numberOfMovesPlayed - 1].posW
                    >>> (i * Board.HEIGHT + j))& 1L) == 1L;
        
    } 

  
    public final void setOldBoard(int number) {

        numberOfMovesPlayed = number;
        posW = history[numberOfMovesPlayed].posW;    
        posB = history[numberOfMovesPlayed].posB;

        history[numberOfMovesPlayed].decompress(this);

    }  
  
    public final CompressedBoard getBitBoard() {
        return history[numberOfMovesPlayed];
    }
  
    public final boolean isConversion() {
        return history[numberOfMovesPlayed].conversion;
    }
  
    public final int getNumberOfMovesPlayed() {
        return numberOfMovesPlayed;
    }  
    
    public int getWidth(){
     return WIDTH;
    }
    
    public int getHeight(){
   		return HEIGHT; 	
   }
   
  
     
    public void openBoard(String s) throws IOException {
  
        try (BufferedReader in = new BufferedReader(new FileReader(s));){
        	
            for (int row = 0; row < Board.HEIGHT; row++) {
                for (int col = 0; col < Board.WIDTH; col++) {  
                    space[ row ][ col ] = Byte.parseByte(in.readLine());
                }
            }
           
            sideToMove = Integer.parseInt(in.readLine()); 
     
            numberOfMovesPlayed = 0; 
            initialise(sideToMove);
        }
    }

    public void saveBoard(String s) throws IOException {
      
    	try(PrintWriter out = new PrintWriter(new BufferedWriter( new FileWriter(s +  (s.endsWith(".sbf")?"":".sbf"), false),128));){

	        for (int row = 0; row < Board.HEIGHT; row++) {
	            for (int col = 0; col < Board.WIDTH; col++) {
	                out.println(space[ row ][ col ]);
	            }
	        }
           
	        out.println(sideToMove);
	        out.println(toString());
    	}             
    }
       
    public static boolean validBitLine(int line, int length){
    	
    	return (((line & ((1<<length)-1))&(line >>>length)) ==0);
    }
    
	public static byte getPieceFromLineBits(int line, int col, int length){
	    final byte piece = (byte) ((line >>>  col)&1);      
	    
		if(piece==0)
		 return  (byte)-((line >>> (length + col))&1); 
		
		return piece;
	
}
	
	public int hashCode() {
		  return (int) this.hashValue;
		  }

  
    static {
        setSize(8,8);
    }

}

