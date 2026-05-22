

/**
 *A simple move that just moves a piece from a source location
 *to a destination location, possibly with a capture 
 */

public final class SurakartaMove extends Move {

   
	public final static int[][]MOVE_CATEGORY_TABLE = new int[SurakartaGame.STANDARD_WIDTH][SurakartaGame.STANDARD_WIDTH];
	
    private long savedValue;
    private long hashValueB, hashValueW;
     
    // Save the contents of the destination
    // positions.
    private Piece changePiece;

    /**
     Construct a move that moves the piece from f_row, f_col to
     t_row, t_col
     */  
    public SurakartaMove() {}
   
    public SurakartaMove(int f_row, int f_col, int t_row, int t_col) {
        setMove(f_row, f_col, t_row, t_col);
    }
  
    public SurakartaMove(String s) {
       super(s);
    }
     
    private void  updateBitmap(Board b, int row, int col, int party) {
        if (party < 0) {
            b.Horizontal_Bit_Lines[row] ^= (1 << (col + Board.WIDTH));    
            b.Vertical_Bit_Lines[col] ^= (1 << (row + Board.WIDTH));
            final int hulp = row + col;
    
            if (hulp <= Board.REAL_BORDER) {
                b.SWNE_Bit_Lines[ hulp] ^= (1 << (col + Board.WIDTH));
            } else {
                b.SWNE_Bit_Lines[hulp] ^= (1
                        << (col + (2 * Board.WIDTH - 1) - hulp));
            }
          
            if (row >= col) {
                b.SENW_Bit_Lines[row - col] ^= (1 << (col + Board.WIDTH));
            } else {
                b.SENW_Bit_Lines[col - row + Board.REAL_BORDER] ^= (1
                        << (row + Board.WIDTH));
            }
        } else {
            b.Horizontal_Bit_Lines[row] ^= (1 << col);    
            b.Vertical_Bit_Lines[col] ^= (1 << row);
            final int hulp = row + col;
     
            if (hulp <= Board.REAL_BORDER) {
                b.SWNE_Bit_Lines[ hulp] ^= (1 << col);
            } else {
                b.SWNE_Bit_Lines[hulp] ^= (1
                        << (col - (hulp - (Board.REAL_BORDER))));
            }
         
            if (row >= col) {
                b.SENW_Bit_Lines[row - col] ^= (1 << col);
            } else {
                b.SENW_Bit_Lines[col - row + Board.REAL_BORDER] ^= (1 << row);
            }
        }  
    }  
   
    /**
     Move a piece from the from position to the to position
     */
    public final void apply(Board b) {
        // Make a copy of the target piece.
        byte space[][] = b.space;
        int index_space[][] = b.indexSpace;

        savedPiece = space[ toRow ][ toCol ];
        savedValue = b.hashValue;
        hashValueB = b.blackHashValue;
        hashValueW = b.whiteHashValue;
    
        int side = b.sideToMove;
        b.sideToMove = -side;
       
        if (side == Board.B_SIDE) {
            changePiece = b.arrayB[index_space[ fromRow ][ fromCol ]];
         
            b.blackHashValue ^= (TranspositionTable.getHashValue(fromRow,
                    fromCol, Board.B_SIDE) 
                    ^ TranspositionTable.getHashValue(toRow, toCol,
                    Board.B_SIDE));
        
            if (savedPiece != Board.EMPTY) { 
                b.whiteHashValue ^= TranspositionTable.getHashValue(toRow,
                        toCol, Board.W_SIDE);
            }
        } else {
            changePiece = b.arrayW[index_space[ fromRow ][ fromCol ]];
 
            b.whiteHashValue ^= (TranspositionTable.getHashValue(fromRow,
                    fromCol, Board.W_SIDE) 
                    ^ TranspositionTable.getHashValue(toRow, toCol,
                    Board.W_SIDE));  
            if (savedPiece != Board.EMPTY) {    
                b.blackHashValue ^= TranspositionTable.getHashValue(toRow,
                        toCol, Board.B_SIDE);
            }
        }
      
        changePiece.setValue(toRow, toCol);
        b.hashValue = b.blackHashValue ^ b.whiteHashValue
                ^ TranspositionTable.getHashMoveValue(-side);
    
        // Make a copy of the piece in the to position
        space[ toRow ][ toCol ] = (byte)side;

        // Empty the from position
        space[ fromRow ][ fromCol ] = Board.EMPTY;    
        updateBitmap(b, fromRow, fromCol, side);
        updateBitmap(b, toRow, toCol, side);
   
        
        if (savedPiece != Board.EMPTY) {
            updateBitmap(b, toRow, toCol, savedPiece);
    
            if (savedPiece == Board.B_SIDE) {
                b.B_Counter--;
          
                int old = index_space[toRow][toCol];
          
                Piece p = b.arrayB[old];
                Piece q = b.arrayB[b.B_Counter];
          
                b.arrayB[old] = q;
                index_space[q.row][q.col] = old;
                b.arrayB[b.B_Counter] = p;
         
            
            } else {
                b.W_Counter--;
         
                int old = index_space[toRow][toCol];
           
                Piece p = b.arrayW[old];
                Piece q = b.arrayW[b.W_Counter];
          
                b.arrayW[old] = q;
                index_space[q.row][q.col] = old;
                b.arrayW[b.W_Counter] = p;
          
               
            }
          
            addToHistory(b, side, true);
    
        } else {
          
            addToHistory(b, side, false);
    
        }
   
        index_space[toRow][toCol] = index_space[fromRow][fromCol];
    }

    /**
     Erase the mark from the space at row/col.
     */
    public final void undo(final Board b) {
        // Restore the source and destination move fl
        byte space[][] = b.space;
        int index_space[][] = b.indexSpace;
        int side = -b.sideToMove;

        b.sideToMove = side;

        removeFromHistory(b);
  
        changePiece.setValue(fromRow, fromCol);
  
        // Put a copy of the piece in the back in the from position
        space[ fromRow ][ fromCol ] = (byte)side;

        // Empty the to position
        space[ toRow ][ toCol ] = savedPiece;
        b.hashValue = savedValue;
        b.blackHashValue = hashValueB;
        b.whiteHashValue = hashValueW;        
        
    
        index_space[fromRow][fromCol] = index_space[toRow][toCol];
    
        // int side = b.space[from_row][from_col];

        updateBitmap(b, fromRow, fromCol, side);
        updateBitmap(b, toRow, toCol, side);
  
        if (savedPiece != Board.EMPTY) {  
            updateBitmap(b, toRow, toCol, savedPiece);
      
            if (savedPiece == Board.B_SIDE) {
                index_space[toRow][toCol] = b.B_Counter;
                b.B_Counter++;
              
            } else {
                index_space[toRow][toCol] = b.W_Counter;
                b.W_Counter++; 
              
            }
        }      
    }
    
  
 
   
    public final int getMoveType(Board b) {
   	 
         int cat = ( b.space[toRow][toCol] != Board.EMPTY) ? 16 : 0;   

        switch (MOVE_CATEGORY_TABLE[fromRow][fromCol]) {
        case 0:
            cat += MOVE_CATEGORY_TABLE[toRow][toCol];
            break;

        case 1:
            cat += 4 + MOVE_CATEGORY_TABLE[toRow][toCol];
            break;

        case 2:
            cat += 8 + MOVE_CATEGORY_TABLE[toRow][toCol];
            break;

        default:
            cat += 12 + MOVE_CATEGORY_TABLE[toRow][toCol];
            break;

        }	 	 
        return   cat;
    }
    
    static{
    	
    	 for (int i = 0; i < SurakartaGame.STANDARD_WIDTH; i++) {
             for (int j = 0; j < SurakartaGame.STANDARD_WIDTH; j++) {
                 if ((i == 0 || i == SurakartaGame.STANDARD_WIDTH - 1)
                         && (j == 0 || j == SurakartaGame.STANDARD_WIDTH - 1)) {
                     MOVE_CATEGORY_TABLE[i][j] = 0;
                 } else if (i == 0 || i == SurakartaGame.STANDARD_WIDTH - 1 || j == 0
                         || j == SurakartaGame.STANDARD_WIDTH - 1) {
                     MOVE_CATEGORY_TABLE[i][j] = 1;
                 } else if (i == 1 || i == SurakartaGame.STANDARD_WIDTH - 2 || j == 1
                         || j == SurakartaGame.STANDARD_WIDTH - 2) {
                     MOVE_CATEGORY_TABLE[i][j] = 2;
                 } else {
                     MOVE_CATEGORY_TABLE[i][j] = 3;
                 }
             }
         }
    	 
    	/* for (int i = 0; i < SurakartaGame.STANDARD_WIDTH; i++) {
             for (int j = 0; j < SurakartaGame.STANDARD_WIDTH; j++){
            	 System.out.print( MOVE_CATEGORY_TABLE[i][j] + " ");
            	 
             }
             System.out.println();
    	 }*/
    }
}

