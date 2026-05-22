
public class SurakartaGenerator extends Generator {
	
	private static double[] RPPROB ;
    private static double[] LOGPROB;
    
	private boolean prune;
	
	private int markDoubleMove =0;
	private int[] markedTable = new int[1<<Move.BIT_RANGE];
	
	
	private final static byte [][] entryLinkTable = 
	   {{0,0,1,2,3,0},
		{4,0,0,0,0,5},
		{6,0,0,0,0,7},
		{8,0,0,0,0,9},
		{10,0,0,0,0,11},
		{0,12,13,14,15,0}};
	
	private final static byte [][] linkTable =
	{{1,0,0,1},
	 {2,0,0,1},
	 {2,5,0,-1},
	 {1,5,0,-1},
	 {0,1,1,0},
	 {0,4,1,0},
	 {0,2,1,0},
	 {0,3,1,0},
	 {5,2,-1,0},
	 {5,3,-1,0},
	 {5,1,-1,0},
	 {5,4,-1,0},
	 {4,0,0,1},
	 {3,0,0,1},
	 {3,5,0,-1},
	 {4,5,0,-1}};
	 

	public SurakartaGenerator() {
		 super();
	}
	
    private Piece sukararta(Board b, int opponent, 
            int row, int col, int x, int y, int length, int loop) {                                 
             
        int teller = 0; 
        
        int newrow; 
        int newcol;
        
        do {
        	newrow = row + teller * x;
        	newcol = col + teller * y;
        	
        	byte value = b.space[ newrow][ newcol ];
            if (value  == opponent) {
            	if(loop==0)
            		return null;
                if(opponent==Board.B_SIDE)
                	return b.arrayB[b.indexSpace[ newrow  ][ newcol]];
                else
                	return b.arrayW[b.indexSpace[ newrow ][ newcol ]];
            }
            else if(value==-opponent){
            	return null;
            	
            }
            else {
            	teller++;
            	
            }
        } while (teller <= length); 
       
        if(loop==4)
          return null; 
        else{
        	byte [] values = linkTable[entryLinkTable[newrow][newcol]];
           return sukararta( b, opponent, 
        		   values[0], values[1],  values[2], values[3], 5, loop+1);
        }
    }   
    
   	
	 private Move moveCapturePiece(Move move_list, Board b, int side, 
	            int row, int col
	            ) { 
		 
		 Piece p;
		 b.space[row][col]=0;
		 
		 if(row >=1 && row <= 4 ){
			 p=  sukararta( b, -side, row, col,  0, 1, 5-col, 0);
			 boolean next = true;
			 if(p!=null){
				 move_list =moveLine(move_list, b, side, row, col, p.row, p.col);
               
                 next = p.row != row;
			 }
				 
			
			if(next){
			 p =sukararta( b, -side, row, col,  0, -1, col, 0);
			 
			 if(p!=null){
				 move_list =moveLine(move_list, b, side, row, col, p.row, p.col);
				 }
			}  		
			 
		 }
		 
		 if(col >=1 && col <= 4){
			 boolean next = true;
			 p =sukararta( b, -side, row, col,  1, 0, 5-row, 0);
			 
			 if(p!=null){
				    move_list =moveLine(move_list, b, side, row, col, p.row, p.col);
	                 next = p.col != col;
			 }
				 
			
			if(next){
			 p =  sukararta( b, -side, row, col,  -1, 0, row, 0);
			 
			 if(p!=null){
				 move_list =moveLine(move_list, b, side, row, col, p.row, p.col);
				 }
			}
			 
		 }
		 b.space[row][col]=(byte)side;
    
		 
		 return move_list;
	 }
		  
	
	 private Move moveLine(Move move_list, Board b, int side, 
	            int row, int col, int to_row, int to_col
	            ) { 
		 
		 int index = Move.computeIndex(row, col, to_row, to_col);
		 
	        if ( (markedTable[index]
		                        == markDoubleMove)||(markedMoves != null
	                && markedMoves[ index]
	                        == marker)/*                  
	                                || (prune
	                                        && !checkQuiescenceMove(b, side, row,
	                                        col, to_row, to_col))*/) {
	            return move_list;
	        }                    
	        
	        markedTable[index]= markDoubleMove;
	        Move m = generateMove(row, col, to_row, to_col);

	        m.next = move_list;
	  
	        return m;
	    }  
	  
	    // Generate all possible moves for the checker at position
	    // row, col.  Add these moves to the front of move_list and return
	    // the result.
	    public final Move movePiece(Move move_list, Board b, int side, 
	            int row, int col) {  
	    	
	    	markDoubleMove++;
            return move(move_list, b, side, row, col);
	     
	    }
	    
	    public final Move move(Move move_list, Board b, int side, 
	            int row, int col) {  
	    	
	    	
	    	move_list = moveNoCapturePiece(move_list, b, side, row, col);
            return moveCapturePiece(move_list, b, side, row, col);
	     
	    }
	   
	    public final Move  moveNoCapturePiece(Move move_list, Board b, int side, 
	            final int row, final int col) {  
	    	
	    	byte [][] space = b.space;
	        // Links
	        if (col >= 1 && space[ row   ][ col - 1 ] ==0) {
	            move_list = moveLine(move_list, b, side, row, col, row, col-1);
	        }
	        
	        // Rechts
	        if (col < SurakartaGame.STANDARD_REAL_BORDER
	                && space[ row   ][ col + 1 ] ==0) {
	            move_list = moveLine(move_list, b, side, row, col, row, col+1);
	        }	    
	 
	        // boven
	        if (row  >= 1 && space[ row - 1 ][ col ] ==0) {
	            move_list = moveLine(move_list, b, side, row, col, row-1, col);
	        }
	        
	        // onder
	        if (row  < SurakartaGame.STANDARD_REAL_BORDER
	                && space[ row + 1 ][ col  ] ==0) {
	            move_list = moveLine(move_list, b, side, row, col, row+1, col);
	        }
	   
	        if (row  < SurakartaGame.STANDARD_REAL_BORDER && col  < SurakartaGame.STANDARD_REAL_BORDER
	                && space[ row + 1  ][ col + 1  ] ==0) {
	            move_list = moveLine(move_list, b, side, row, col, row+1, col+1);
	        }
	        
	        // NW
	        if (row  >= 1 && col  >= 1
	                && space[ row - 1 ][ col - 1  ] ==0) {
	            move_list = moveLine(move_list, b, side, row, col, row-1, col-1);
	        }
	       
	 
	        // NO
	        if (row  < SurakartaGame.STANDARD_REAL_BORDER && col >= 1
	                && space[ row + 1  ][ col - 1  ] ==0) {
	            move_list = moveLine(move_list, b, side, row, col, row+1, col-1);
	        } 
	        
	        // ZW
	        if (row  >= 1 && col  < SurakartaGame.STANDARD_REAL_BORDER
	                && space[ row - 1 ][ col + 1  ] ==0) {
	        
	            move_list = moveLine(move_list, b, side, row, col, row-1, col+1);
	        }

	        return move_list;
	    }
	    
	    private Move generateCaptures(Board b, int side, boolean pr) {
	    	
	     Move move_list = null;
	     markDoubleMove++;
		    
	        prune = pr;

	        if (side == Board.B_SIDE) {
	            Piece[] p = b.arrayB;
	     
	            for (int i = b.B_Counter; --i >= 0;) {
	                move_list = moveCapturePiece(move_list, b, side, p[i].row, p[i].col);
	            }
	        } else {
	            Piece[] p = b.arrayW;  
	     
	            for (int i = b.W_Counter; --i >= 0;) {
	                move_list = moveCapturePiece(move_list, b, side, p[i].row, p[i].col);
	            }
	        }   
	          
	        return move_list;
	    	
	    }
	    
		public Move generateMoves(Board b, int side) {
	        Move move_list = null;
	        
	        prune = false;
	        markDoubleMove++;

	        if (side == Board.B_SIDE) {
	            Piece[] p = b.arrayB;
	     
	            for (int i = b.B_Counter; --i >= 0;) {
	            	move_list = move(move_list, b, side, p[i].row, p[i].col);
	            }
	        } else {
	            Piece[] p = b.arrayW;  
	     
	            for (int i = b.W_Counter; --i >= 0;) {
	            	move_list = move(move_list, b, side, p[i].row, p[i].col);
	            }
	        }   
	          
	        return move_list;}


		public Move generateQuiescenceMoves(Board b, int side) {
			return generateCaptures(b,side,true);
		
		}

	public Move generateCaptureMoves(Board b, int side) {
		return generateCaptures(b,side,false);
		
	}

	public Move generateNoCaptureMoves(Board b, int side) {
	
	        // Start out with an empty move list.
	        Move move_list = null;
	        markDoubleMove++;
	    
	        prune = false;

	        if (side == Board.B_SIDE) {
	            Piece[] p = b.arrayB;
	     
	            for (int i = b.B_Counter; --i >= 0;) {
	                move_list = moveNoCapturePiece(move_list, b, side, p[i].row, p[i].col);
	            }
	        } else {
	            Piece[] p = b.arrayW;  
	     
	            for (int i = b.W_Counter; --i >= 0;) {
	                move_list = moveNoCapturePiece(move_list, b, side, p[i].row, p[i].col);
	            }
	        }   
	          
	        return move_list;
	    
	}

	protected Move createMove(int f_row, int f_col, int t_row, int t_col) {
		
		return new SurakartaMove(f_row,f_col,t_row,t_col);
	}

	protected Move createMove() {
		
		return new SurakartaMove();
	}

	public Move generateIncMove(Board b, int side, Move transmove, long[] MMoves, long M, boolean pr) {
		 
		if (transmove == null || b.space[transmove.fromRow][transmove.fromCol] !=side ) {
	            return null;
	        }
	  
	        prune = pr;  
	        markDoubleMove++;
	    
	        if (b.space[transmove.toRow][transmove.toCol] == Board.EMPTY  && Math.abs(transmove.toRow-transmove.fromRow)<=1 && Math.abs(transmove.toCol-transmove.fromCol)<=1){
	        	
	        		if(prune) return null;	                
	            
	                markedMoves = MMoves;
	                marker = M;
	                transmove = moveLine(null, b, side, transmove.fromRow,
	                        transmove.fromCol, transmove.toRow, transmove.toCol);
	                markedMoves = null;
	                return transmove;	          
	        }   
	        
	        
	        if (b.space[transmove.toRow][transmove.toCol] ==-side ){
	       
	        	markedMoves = MMoves;
	            marker = M;	
	            Move list = moveCapturePiece(null,b,side,transmove.fromRow,
                   transmove.fromCol);
	             markedMoves = null;
		   		Move next,m=list;	
	             while (m != null) {
	                 next = m.next;
	                 if(m.equals(transmove)){
	                	 m.next =null;
	                	 this.recycle(next);
			   	          return m;	
	                 }
			   			else
			   				this.recycleMove(m);
	                 m = next;
	             }
	        }
	   		 
	       
	   	
	        
	        return null;
	    }   
	
	  public double[] getLOGRPPROB() {
	        return LOGPROB.clone();
	    }
	  
	  static{
		  
		    final int n = 32; 

	        RPPROB = new double[n]; 
	        LOGPROB = new double[n];
	        
	        Generator.readRPWeights(RPPROB,LOGPROB,"surakartarps.txt");
	        
	     //   System.out.println( java.util.Arrays.toString(RPPROB));
	     //   System.out.println( java.util.Arrays.toString(LOGPROB));
		  
	  }



}
