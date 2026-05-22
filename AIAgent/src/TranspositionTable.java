/*import java.io.BufferedReader;
import java.io.FileReader;*/


// transpositiontable
public final class TranspositionTable {
	 
    public static final int VALID = 0;
    public static final int UBOUND = 2;
    public static final int LBOUND = 1;
  
    private static long[] RandomTable; 
    private final TableEntry dummyEntry = new TableEntry(0, -1, -8, new Move());

    private  final TableEntry Entry = new TableEntry(0, -1, -8, new Move());
    // private TableEntry[][] Table ;
 
    private long[] heightScore;
  
    private long[] moveFlagHashLock;
    
    private final int hashKeySize;
    private final int hashKeyMask;
  
    private static final long HASHSIDE = 4248521026125069682L;
  
    // creates a transpositiontable
    TranspositionTable(int key) {     
        hashKeySize = 1 << key;
        hashKeyMask = hashKeySize - 1;      
        moveFlagHashLock = new long[2 * hashKeySize];
        heightScore = new long[2 * hashKeySize];
 
    }
    
    public int getKey(){
    	
    return (int)(Math.log(hashKeySize)/Math.log(2)); 	
   }
   
    // Clear the table, by making a new array
    public void clear() {   
        for (int i = moveFlagHashLock.length; --i >= 0;) {
            moveFlagHashLock[i] = -1;
        }
    }
    
    // Clear the table, by making a new array
    public void mark() { 
        for (int i = moveFlagHashLock.length; --i >= 0;) {
            moveFlagHashLock[i] = TableEntry.mark(moveFlagHashLock[i]);
        }
    }  
     
    // Gets an element form the transpositiontable
    public final TableEntry retrieve(long HashValue) {
        int key = (int) (HashValue & hashKeyMask);
    
        final long lock = HashValue >>> TableEntry.SHIFT_LOCK;
     
        if (moveFlagHashLock[key] >>> TableEntry.SHIFT_LOCK == lock) {
            Entry.setValue(moveFlagHashLock[key], heightScore[key]);
                 
            return Entry;      
        }
       
        key += hashKeySize;
        
        if (moveFlagHashLock[key ] >>> TableEntry.SHIFT_LOCK == lock) {
            Entry.setValue(moveFlagHashLock[key], heightScore[key]);
               
            return Entry;      
        }
      
        return dummyEntry;
    }
   
    // Stores  an element
    public final void storeTwoDeep(long HashValue, double Height, int flag, Move move) {
        final int first = (int) (HashValue & hashKeyMask);
        
        long lock = HashValue >>> TableEntry.SHIFT_LOCK;
    
        if (moveFlagHashLock[first] >>> TableEntry.SHIFT_LOCK == lock
                || TableEntry.isMarked(moveFlagHashLock[first])) {   
            moveFlagHashLock[first] = TableEntry.getMoveFlagHashLock(
                    HashValue, flag, move); 
            heightScore[first] = TableEntry.getHeightScore(move.next,
                    move.score, Height);
                 
            return;
        }
 
        final int second = first + hashKeySize;
       
        if (moveFlagHashLock[second] >>> TableEntry.SHIFT_LOCK == lock
                || TableEntry.isMarked(moveFlagHashLock[second])) {  
            moveFlagHashLock[second ] = TableEntry.getMoveFlagHashLock(
                    HashValue, flag, move);
            heightScore[second ] = TableEntry.getHeightScore(move.next,
                    move.score, Height);
           
            return;
        }
   
        if ((heightScore[first] & TableEntry.DEPTH_RANGE)
                >= (heightScore[second] & TableEntry.DEPTH_RANGE)) {
            heightScore[second] = heightScore[first];
        
            moveFlagHashLock[second] = moveFlagHashLock[first];
        }
       
        heightScore[first] = TableEntry.getHeightScore(move.next, move.score,
                Height);
        moveFlagHashLock[first] = TableEntry.getMoveFlagHashLock(HashValue,
                flag, move); 
          
        /* int min = ((height_score[first]& TableEntry.Depth_Range) >= (height_score[second]& TableEntry.Depth_Range))
         ? second : first;
         
         if((height_score[min]& TableEntry.Depth_Range) <= Height){
         height_score[min]=TableEntry.getHeight_Score(move.next, move.score, Height);
         Move_Flag_HashLock[min] =  TableEntry.getMove_Flag_HashLock(HashValue,  flag, move);}*/    
    }  
 
    private static void generateHashValues() { 
  
        RandomTable = new long[4 *Board.MAX_SIZE*Board.MAX_SIZE + 1];//new long[2 * Board.HEIGHT * Board.WIDTH + 1];
  
  
	  java.util.Random rand = new java.util.Random(19780405);
	 
  
        for (int i = 0; i < RandomTable.length; i++) {
        	
        	long hulp =0;
             for (int j = 64; --j>=0;) {
              if(rand.nextBoolean())
               hulp += (1L<<j);
            }
            
            RandomTable[i] =hulp;
        }
         

    }
    
    
    public static final long getHashValue(final int row, final int col, final int piece) { 
        switch(piece){
        case 1: return RandomTable[Board.HEIGHT * row + col];
        case -1: return RandomTable[Board.HEIGHT * row + col + Board.SQUARE];
        case 0: return 0L;  
        case 2: return RandomTable[Board.HEIGHT * row + col +2*Board.SQUARE];
        case -2: return RandomTable[Board.HEIGHT * row + col + 3*Board.SQUARE];
        default: return 0L;  
        }
        
        
    }
  

     
  /*  public static final long getHashValue(final int row, final int col, final int piece) { 
        if (piece > 0) {
            return RandomTable[Board.HEIGHT * row + col];
        }
            
        if (piece < 0) {
            return RandomTable[Board.HEIGHT * row + col + Board.SQUARE];
        }
             
        return 0L;      
    }
  
    public static final long getPieceHashValue(int row, int col, int piece) { 
        if (piece > 0) {
            return RandomTable[Board.HEIGHT * row + col];
        }
            
        return RandomTable[Board.HEIGHT * row + col + Board.SQUARE];   
    }*/

    public static final long getHashMoveValue(int side) {
        if (side > 0) {
            return HASHSIDE;
        }
  
        return 0L;
        // return (side>0)?HASHSIDE:0L;
    }
    
    public static final long getHashMoveValue() {
          return HASHSIDE;
     
        // return (side>0)?HASHSIDE:0L;
    }
    
    public static final void setHashValue(Board b){
    	
    	setHashValue(b, b.sideToMove);
    }
     
    public static final void setHashValue(Board b, int side) {
    	b.hashValue =getHashMoveValue(side);
  
        /* for(int i = 0; i< Board.HEIGHT; i++)
         for(int j = 0; j < Board.WIDTH; j++)
         b.HashValue ^= getHashValue(i,j,b.space[i][j]) ;*/
    
        Piece[]p = b.arrayB;
    
        for (int i = b.B_Counter; --i >= 0;) {
        	b.hashValue ^= getHashValue(p[i].row, p[i].col, p[i].pieceValue);
        }
 
        p = b.arrayW; 
 
        for (int i = b.W_Counter; --i >= 0;) {
        	b.hashValue ^= getHashValue(p[i].row, p[i].col, p[i].pieceValue);
        }
    }
   
    public static final long getHashValue(byte[][] space, int side) 
    {
        long hashValue = getHashMoveValue(side);
  
        for (int row = Board.HEIGHT; --row>=0;) {
            for (int col =  Board.WIDTH; --col>=0;) {
                if (space[row][col] == Board.B_PIECE) {
                    hashValue ^= getHashValue(row, col, space[row][col]);
                } else if (space[row][col] == Board.W_PIECE) {		 
                    hashValue ^= getHashValue(row, col, space[row][col]);
                }
            }
        }
   
        return hashValue;        
    }

    public static final long getHashValue(ProofNode node) {
        return getHashValue(node.posB, node.posR, node.side);
    }
    
    public static final long getHashValue(long posB, long posR, int side) {
        long HashValue = getHashMoveValue(side);
         
        for (int row = 0; row < Board.HEIGHT; row++) {
            for (int col = 0; col < Board.WIDTH; col++) {
                if (((posR >>> (row * Board.HEIGHT + col)) & 1L) == 1L) {
                    HashValue ^= getHashValue(row, col, Board.W_PIECE);
                } else if (((posB >>> (row * Board.HEIGHT + col)) & 1L) == 1L) {		 
                    HashValue ^= getHashValue(row, col, Board.B_PIECE);
                }
            }
        }
   
        return HashValue; 
    }   
   
    public static final long getHashSideValue(Board b, int side) {
        long hash = 0L;
    
        if (side == Board.B_SIDE) {
            final Piece[] p = b.arrayB;
  
            for (int i = b.B_Counter; --i >= 0;) {
                hash ^= getHashValue(p[i].row, p[i].col, p[i].pieceValue);
            }
        } else {
            final Piece[] p = b.arrayW; 
  
            for (int i = b.W_Counter; --i >= 0;) {
                hash ^= getHashValue(p[i].row, p[i].col, p[i].pieceValue);
            }
        }

        return hash;
    }   
    
    
    public static final long getMirrorXHashValue(Board b) {
        
    	long hashValue = getHashMoveValue(b.sideToMove);
      
        Piece[]p = b.arrayB;
    
        for (int i = b.B_Counter; --i >= 0;) {
            hashValue ^= getHashValue(p[i].row, Board.REAL_BORDER-p[i].col, p[i].pieceValue);
        }
 
        p = b.arrayW; 
 
        for (int i = b.W_Counter; --i >= 0;) {
            hashValue ^= getHashValue(p[i].row, Board.REAL_BORDER-p[i].col, p[i].pieceValue);
        }
  
        return hashValue;
    }
    
    public static final long getMirrorXYHashValue(Board b) {
        
    	long hashValue = getHashMoveValue(b.sideToMove);
      
        Piece[]p = b.arrayB;
    
        for (int i = b.B_Counter; --i >= 0;) {
            hashValue ^= getHashValue(Board.REAL_BORDER-p[i].row, Board.REAL_BORDER-p[i].col, p[i].pieceValue);
        }
 
        p = b.arrayW; 
 
        for (int i = b.W_Counter; --i >= 0;) {
            hashValue ^= getHashValue(Board.REAL_BORDER-p[i].row, Board.REAL_BORDER-p[i].col, p[i].pieceValue);
        }
  
        return hashValue;
    }
    
    public static final long getMirrorYHashValue(Board b) {
        long hashValue = getHashMoveValue(b.sideToMove);
      
        Piece[]p = b.arrayB;
    
        for (int i = b.B_Counter; --i >= 0;) {
            hashValue ^= getHashValue(Board.REAL_BORDER-p[i].row, p[i].col, p[i].pieceValue);
        }
 
        p = b.arrayW; 
 
        for (int i = b.W_Counter; --i >= 0;) {
            hashValue ^= getHashValue(Board.REAL_BORDER-p[i].row, p[i].col, p[i].pieceValue);
        }
  
        return hashValue;
    }
    

 

 
    static {
        generateHashValues(); 
        
    }  
    
} 


