 
public final class Piece implements Cloneable {

    public int row; 
    public int col;

    public int index;

    public int pieceValue;
 
    public Piece(int r, int c, int s) {
        pieceValue = s;
        setValue(r, c);
 
    }
    
    public Piece(Piece p) {
        pieceValue = p.pieceValue;
        row = p.row;
        col = p.col;
        index = p.index;
    }
 
    public final void setValue(int r, int c) {
 
        row = r;
        col = c;
  
        index = (Board.WIDTH * r) + c;
    }
    
    public Object clone(){
        
        try {
  		return super.clone();
  	} catch (CloneNotSupportedException e) {
  		throw new Error("Piece couldn't be cloned");
  	}
      }

	public void setValue(int r, int c, int b) {
		  setValue(r, c);
		  pieceValue = b;		
	}
 
} 

