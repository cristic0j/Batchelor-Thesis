/**
 Utility class for keeping up with a move and it's score.

 This class is not encapsulated.
 */
public class Move implements Cloneable{

    /**
     Dummy instance of Move used for type checking in the defined method.
     */  
   
    public static  final int BIT_RANGE = 16; 
   
    private final static Move dummyInst = new Move();
    
    private static final int BIT_DIMENSION = 4;
    
    private static final int SHIFT_FCOL = BIT_DIMENSION;
  
    private static final int SHIFT_TROW = 2 * BIT_DIMENSION;
  
    private static final int SHIFT_TCOL = 3 * BIT_DIMENSION;
  
    private static final int MOVE_RANGE = (1 << BIT_DIMENSION) - 1;
    
    private static final int OLDBIT_DIMENSION = 3;
    
    private static final int OLDSHIFT_FCOL = OLDBIT_DIMENSION;
  
    private static final int OLDSHIFT_TROW = 2 * OLDBIT_DIMENSION;
  
    private static final int OLDSHIFT_TCOL = 3 * OLDBIT_DIMENSION;
  
    private static final int OLDMOVE_RANGE = (1 << OLDBIT_DIMENSION) - 1;
    
    public static final Move PASS_MOVE = new Move();
  

    /**
     Score for this move.
     */
    public int score;
  
    public byte savedPiece;

    // Next move in the linked list of moves.
    public Move next;

    // Source location for this move.
    public int fromRow, fromCol;

    // Destination location for this move.
    public int toRow, toCol;
  
    public int index;

    public Move() {}

    public Move(int f_row, int f_col, int t_row, int t_col) {
        setMove(f_row, f_col, t_row, t_col);
    }
    
    public Move(String s){
      setMove(s);	
    }
    
 
    public Object clone(){
     
      try {
		return super.clone();
	} catch (CloneNotSupportedException e) {
		throw new Error("Move couldn't be cloned");
	}
    }

    /**
     Return true if this move is defined (i.e. if it is a class
     derived from Move rather than an instance of Move itself.
     */
    public final static void applyNullMove(Board b) {
        b.hashValue ^= TranspositionTable.getHashMoveValue();
        addToHistory(b);
        b.sideToMove = -b.sideToMove;
    }
      
    public final static void undoNullMove(Board b) {
       // b.hashValue ^= TranspositionTable.getHashMoveValue();
        removeFromHistory(b);
        b.sideToMove = -b.sideToMove;
    }
    
    protected void computeBitBoard(Board b, int side, boolean capture){    
    	
        if (side == Board.B_SIDE) {
                     
            if (capture) {
                final long to = 1L << (toRow * Board.HEIGHT + toCol);
            	b.posB ^= (1L << (fromRow * Board.HEIGHT + fromCol))^ to;
                b.posW ^= to;
            } 
            else 
            	 b.posB ^= (1L << (fromRow * Board.HEIGHT + fromCol))^(1L << (toRow * Board.HEIGHT + toCol));
        
        } else {       
                  
            if (capture) {
            	final long to = 1L << (toRow * Board.HEIGHT + toCol);
                b.posW ^= (1L << (fromRow * Board.HEIGHT + fromCol))^to;
                b.posB ^= to;
            }
            else   
                b.posW ^= (1L << (fromRow * Board.HEIGHT + fromCol))^(1L << (toRow * Board.HEIGHT + toCol));
        }
    }
    
    protected final void addToHistory(Board b, int side, boolean capture) {    
    	computeBitBoard(b, side, capture);
     
        CompressedBoard cb = b.history[++b.numberOfMovesPlayed];
        
        cb.conversion = capture;
        cb.posW = b.posW;    
        cb.posB = b.posB; 
        cb.hashValue = b.hashValue;
    }
  
    private static final void addToHistory(Board b) { 
       
        CompressedBoard cb = b.history[++b.numberOfMovesPlayed];
        
        cb.conversion = false;
        cb.posW = b.posW;    
        cb.posB = b.posB;
        cb.hashValue = b.hashValue;
    } 
  
    protected static final void removeFromHistory(Board b) {
       // b.numberOfMovesPlayed--;
   
        CompressedBoard cb = b.history[--b.numberOfMovesPlayed];
        b.posW = cb.posW;    
        b.posB = cb.posB;
        b.hashValue = cb.hashValue;
    }
  

  
    public boolean defined() {
        return getClass() != dummyInst.getClass();
    }

    /**
     * 
     Modify the given board to reflect making this move.

     Classes derived from Move can override this function
     to make more interesting moves.
     
     */
    public void apply(Board b) {Move.applyNullMove(b);}

    /**
     Modify the given board to return it to the state before
     this move was made.

     Classes derived from Move can override this function
     to make more interesting moves.
     */
    public void undo(Board b) {Move.undoNullMove(b);}
  
    public final void setMove(int f_row, int f_col, int t_row, int t_col) {
        fromRow = f_row;
        fromCol = f_col;
        toRow = t_row;
        toCol = t_col;
    
        index = computeIndex(f_row, f_col, t_row, t_col);
    }
  
    public final void setMove(int f_row, int f_col, int t_row, int t_col, int in) {
        fromRow = f_row;
        fromCol = f_col;
        toRow = t_row;
        toCol = t_col;
    
        index = in;
    }
    
    public void setMove(String s){
    
    int length = s.length();

       if (length == 4) {
                setMove(decodeChar(s.charAt(1)),
                        decodeChar(s.charAt(0)),
                        decodeChar(s.charAt(3)),
                        decodeChar(s.charAt(2)));
            } else if (length >= 4){
            	
            
            	  int fcol = Move.decodeChar(s.charAt(0));
            	  
            	  int teller;
            	  int frow;
            	  
            	 
        
            	  if(Character.isDigit(s.charAt(2))){
            		  //Log.println(-'0');
            		  frow = Board.HEIGHT- Integer.parseInt(s.substring(1,3));
            		  teller =4;
            		   
            	  }else{
            		  frow = Board.HEIGHT- Integer.parseInt(s.substring(1,2));
            		  teller =3;
            	  }
				
				 int tcol = Move.decodeChar(s.charAt(teller));
				 int trow = Board.HEIGHT-Integer.parseInt(s.substring(teller+1,length));
            	
				 setMove(frow,fcol,trow,tcol);
                /*setMove(decodeChar(s.charAt(1)),
                        decodeChar(s.charAt(0)),
                        decodeChar(s.charAt(4)),
                        decodeChar(s.charAt(3)));*/
            }   	
    }
      
    public final void setMove(int i) {
        fromRow = i & MOVE_RANGE;
        fromCol = (i >>> SHIFT_FCOL) & MOVE_RANGE;
        toRow = (i >>> SHIFT_TROW) & MOVE_RANGE;
        toCol = i >>> SHIFT_TCOL;
    
        index = i;
    }
    
    public final void setOldIndexToMove(int i) {
        fromRow = i & OLDMOVE_RANGE;
        fromCol = (i >>> OLDSHIFT_FCOL) & OLDMOVE_RANGE;
        toRow = (i >>> OLDSHIFT_TROW) & OLDMOVE_RANGE;
        toCol = i >>> OLDSHIFT_TCOL;
    
        index = computeIndex(fromRow, fromCol, toRow, toCol);
    }
  
  
    public final void copy(Move m) {
        fromRow = m.fromRow;
        fromCol = m.fromCol;
        toRow = m.toRow;
        toCol = m.toCol;
        score = m.score;

        index = m.index;
    }
    
    public boolean equals(Object m) {
    	
    	if(m instanceof Move)
    		return equals((Move) m);
    	
    	return false;    	
    }
    
    
    public final boolean equals(Move m) {
    
        return (m != null && m.index == this.index);
    }
    
    public int hashCode() {
   	 
  	  return index; 
  	  }
       
    public String toString() {
    	  
        if (this == PASS_MOVE) {
            return "Pass";
        }
    
        String s = (savedPiece != 0) ? "x" : "-";
   
        return (char) (fromCol + 'a') + "" + (Board.HEIGHT - fromRow) + s
                + (char) (toCol + 'a') + "" + (Board.WIDTH - toRow);
    }  
 
    
    public String toServerString() {
    	 
    
        return  ("" + (char) ((fromCol + 'a')) + " " + (Board.HEIGHT - fromRow)
                + " " + (char) ((toCol + 'a')) + " " + (Board.WIDTH - toRow) + "\n");
    }
 
    public String toShortString() {
    	
    	  if (this == PASS_MOVE) {
              return "Pass";
          }

        return  ("" + (char) ((fromCol + 'a')) + "" + (Board.HEIGHT - fromRow)
                + (char) ((toCol + 'a')) + "" + (Board.WIDTH - toRow) + "");
    }     
  
    public final boolean isCapture(Board b) {
 
          return (b.space[toRow][toCol] != Board.EMPTY); 
    } 
    
    public final boolean isSimilar(Move m) {
  
      	return (m.fromRow == fromRow && m.fromCol == fromCol )||(m.toRow == toRow && m.toCol == toCol);    	
    }
 
    public int isTreath(Board b, Game sbe) {

        int side = b.sideToMove;

        this.apply(b);
        int win = sbe.terminalEvaluation(b, side);
     
        if (side * win == Board.WIN_SCORE) {
            this.undo(b);
            return 100;
        }
  
        int counter = 0;
        Generator gen = sbe.generator;
        applyNullMove(b);       
        
        Move  move_list = gen.generateMoves(b, side);
  
        Move m = move_list, volgende;

        while (m != null) {
            volgende = m.next;
            m.apply(b);
            win = sbe.terminalEvaluation(b, side);
  
            if (side * win == Board.WIN_SCORE) {
                counter++;
            }
            m.undo(b);
            gen.recycleMove(m);
            m = volgende;
        }
        undoNullMove(b);
        this.undo(b);
        return counter;  
    }
    
    public long hash(Board b) { 
 	   
 	   final byte p = b.space[fromRow][fromCol];
    
         return (b.hashValue
                 ^ TranspositionTable.getHashValue(fromRow, fromCol,
                 p) 
                 ^ TranspositionTable.getHashValue(toRow, toCol,
                 p)
                 ^ TranspositionTable.getHashValue(toRow, toCol,
                 b.space[toRow][toCol]) 
                 ^ TranspositionTable.getHashMoveValue());}
  
    public static void mergeSort(Move[] move_array, int nbrOfMoves, int side) {
     
        int i, j, mn, max;
        int n = nbrOfMoves - 1;
        Move hulp;
   
        for (i = 0; i < n; i++) {
            max = side * move_array[i].score;
            mn = i;
            for (j = i + 1; j < nbrOfMoves; j++) {
                if (max < side * move_array[j].score) {
                    max = move_array[j].score * side;
                    mn = j;
                }
            }
      
            hulp = move_array[mn];
            move_array[mn] = move_array[i];
            move_array[i] = hulp;  
        }  
    }       
   
    public int getMoveType(Board b) {
  
        return 0; 
    }
    
    public int getEvalMoveType(Board b) {
    	  
       int  old  = b.getSBE(b.sideToMove).getEvaluator().evaluate(b, -b.sideToMove);
        
       apply(b);
       int newE = b.getSBE(b.sideToMove).getEvaluator().evaluate(b, -b.sideToMove);
       undo(b);
       
       int val = b.sideToMove * (newE - old);
       
       //System.out.println("old: " + old +  " new: " + newE + " total:" + val);
       
       if(val > 500)
    	 return  41;
       
       if(val < -500)
    	   return 0;
       
       if(val>0)
    	   return 21 + (val/25);
       
      
    	   return 20 + (val/25);           
    }
 
    public int getMoveNewType(Board b) {
  
        return getMoveType(b);
    }
  
    public final static int computeIndex(int f_row, int f_col, int t_row, int t_col) {
        return (f_row | (f_col << SHIFT_FCOL) | (t_row << SHIFT_TROW)
                | (t_col << SHIFT_TCOL)); 
    }
    
    public final static int computeOldIndex(Move m) {
      return computeOldIndex(m.fromRow,m.fromCol,m.toRow,m.toCol);
   }
    
    public final static int computeOldIndex(int f_row, int f_col, int t_row, int t_col) {
        return (f_row | (f_col << OLDSHIFT_FCOL) | (t_row << OLDSHIFT_TROW)
                | (t_col << OLDSHIFT_TCOL)); 
    }
 
    public void mirrorX() {
   
        fromCol = (Board.WIDTH - 1) - fromCol;
        toCol = (Board.WIDTH - 1) - toCol;
   
        index = computeIndex(fromRow, fromCol, toRow, toCol);
    }
 
    public void mirrorY() {
   
        fromRow = (Board.WIDTH - 1) - fromRow;
        toRow = (Board.WIDTH - 1) - toRow;
   
        index = computeIndex(fromRow, fromCol, toRow, toCol);
    }
    
    public void mirrorXY() {
    	   
       /* fromCol = (Board.WIDTH - 1) - fromCol;
        toCol = (Board.WIDTH - 1) - toCol;
        fromRow = (Board.WIDTH - 1) - fromRow;
        toRow = (Board.WIDTH - 1) - toRow;*/
        
        fromCol = (Board.WIDTH - 1) - fromCol;
        toCol = (Board.WIDTH - 1) - toCol;
    
        fromRow = (Board.HEIGHT - 1) - fromRow;
        toRow = (Board.HEIGHT - 1) - toRow;
   
        index = computeIndex(fromRow, fromCol, toRow, toCol);
    }
 
   
    public void mirrorNWSE() {
   
        int old_from_col = fromCol;
        int old_to_col = toCol;
   
        fromCol = fromRow;
        toCol = toRow;
    
        fromRow = old_from_col;
        toRow = old_to_col;
   
        index = computeIndex(fromRow, fromCol, toRow, toCol);
    }
   
    public void mirrorNESW() {
   
        int old_from_col = fromCol;
        int old_to_col = toCol;
   
        fromCol = (Board.WIDTH - 1) - fromRow;
        toCol = (Board.WIDTH - 1) - toRow;
    
        fromRow = (Board.HEIGHT - 1) - old_from_col;
        toRow = (Board.HEIGHT - 1) - old_to_col;
   
        index = computeIndex(fromRow, fromCol, toRow, toCol);
    } 
    
   /* public void rotate() {
    	   
        fromCol = (Board.WIDTH - 1) - fromCol;
        toCol = (Board.WIDTH - 1) - toCol;
    
        fromRow = (Board.HEIGHT - 1) - fromRow;
        toRow = (Board.HEIGHT - 1) - toRow;
   
        index = computeIndex(fromRow, fromCol, toRow, toCol);
    }*/ 
   
    public static int  decodeChar(char c) {
    	
        if (c >= 'a' && c <= 'z') {
            return c - 'a';
        } else {
            return Board.HEIGHT - c + '0';
        }
        
    }
    
  
    public static Move quickSort(Move list, int side) {

        if (list == null || list.next == null) {
            return list;
        }

        // quicksort
        Move smaller = null;
        Move temp;
        Move p = list;
 
        list = list.next;
        p.next = null;
        Move bigger = p;

        while (list != null) {
            if (p.score * side < list.score * side) {
                temp = list;
                list = list.next;
                temp.next = bigger;
                bigger = temp;
            } else {
                temp = list;
                list = list.next;
                temp.next = smaller;
                smaller = temp;
            }
        }

        smaller = quickSort(smaller, side);
        bigger = quickSort(bigger, side);
        p.next = smaller;
     
        return bigger;
    }  
  
    static {

        /*int teller = 0;

        for (int i = 0; i < Board.WIDTH; i++) {
            for (int j = 0; j < Board.WIDTH; j++) {
                for (int k = 0; k < Board.WIDTH; k++) {
                    for (int l = 0; l < Board.WIDTH; l++) { 
   
                        if (i == k || j == l
                                || Math.abs(i - k) == Math.abs(j - l)) {
                            if (!(i == k && j == l)) {
                                teller++;
                            }
                        }
      
                    }
                }
            }
        }
   
        LEGAL_MOVES = teller++;*/
   
/*        MoveToIndex = new short[computeIndex(Board.HEIGHT,Board.WIDTH,Board.HEIGHT,Board.WIDTH)];
        IndexToMove = new  byte[LEGAL_MOVES][4];
   
        teller = 0; 
   
        for (int i = 0; i < Board.WIDTH; i++) {
            for (int j = 0; j < Board.WIDTH; j++) {
                for (int k = 0; k < Board.WIDTH; k++) {
                    for (int l = 0; l < Board.WIDTH; l++) { 

                        if (i == k || j == l
                                || Math.abs(i - k) == Math.abs(j - l)) {
                            if (!(i == k && j == l)) {
                                MoveToIndex[computeIndex(i,j,k,l)] = (short) teller;
                                IndexToMove[teller][0] = (byte) i;
                                IndexToMove[teller][1] = (byte) j;
                                IndexToMove[teller][2] = (byte) k;
                                IndexToMove[teller][3] = (byte) l;
     
                                teller++;
                            }
                        } 
                    }
                }
            }
        } 
   
        Log.println("legal moves: " + teller);*/
   
        //BIT_RANGE = (int) Math.ceil(Math.log(teller) / Math.log(2));
   

    }

	public double getFrac(Board b) {
		// TODO Auto-generated method stub
		return 1;
	}
}

