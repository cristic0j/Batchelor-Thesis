public class CompressedBoard {

    public long posB;
    public long posW; 
   
    public long hashValue;
 
    public boolean conversion = false;
  
    public CompressedBoard() {}
   
    public void compress(Board b) {
        posB = 0;
        posW = 0;
    
        Piece[]p = b.arrayB;
    
        for (int i = b.B_Counter; --i >= 0;) {
            posB |= 1L << (Board.HEIGHT * p[i].row + p[i].col);
        }
         
        p = b.arrayW;     
             
        for (int i = b.W_Counter; --i >= 0;) {
            posW |= 1L << (Board.HEIGHT * p[i].row + p[i].col);
        }  
   
        hashValue = b.hashValue;
    } 
    
    public void decompress(Board b) {
        for (int row = 0; row < Board.HEIGHT; row++) {
            for (int col = 0; col < Board.WIDTH; col++) {    
                if (((posW >>> (row * Board.HEIGHT + col)) & 1L) == 1L) {
                    b.space[row][col] = Board.W_PIECE;
                } else if (((posB >>> (row * Board.HEIGHT + col)) & 1L) == 1L) {		 
                    b.space[row][col] = Board.B_PIECE;
                } else {
                    b.space[row][col] = Board.EMPTY;
                }
            }
        }
        
        b.hashValue = hashValue;
    }  
   
    public void copy(CompressedBoard copyBoard) {
  
        posB = copyBoard.posB;
        posW = copyBoard.posW;
        hashValue = copyBoard.hashValue;
        conversion = copyBoard.conversion;
  
    }
 
}
