import java.io.Serializable;


public  class OpeningBookEntry  implements Serializable {
	
	///protected final static short NO_MOVE =-1;
		
	public long hash;

    public final byte Side;

    public byte depth;

    public short Value;

    protected short best_Move;

    public OpeningBookEntry next;
    private static final long serialVersionUID = -137771967928656700L;
	
	public long posB, posR;
   
    
    public static OpeningBookEntry createInstance(Board b){

    	// Use BigEntry for large boards (e.g. Draughts 10x10); standard entry for Surakarta 6x6
    	if(Board.WIDTH > 8 || Board.HEIGHT > 8)
    		return new OpeningBookBigEntry(b);

    	return new OpeningBookEntry(b);
    }
    
    public static OpeningBookEntry createInstance(OpeningBookEntry entry){
    	
    	if(entry instanceof OpeningBookBigEntry)
    		return new OpeningBookBigEntry((OpeningBookBigEntry) entry);
    	
    	return new OpeningBookEntry(entry);
    }
    
    public OpeningBookEntry(Board b) {
    	
        Side = (byte) (b.sideToMove);
        hash = b.hashValue;
        posB = 	b.posB;
    	posR =	b.posW;
    	//best_Move = NO_MOVE;
    }
    
    public OpeningBookEntry(OpeningBookEntry entry) {
      
        Side = entry.Side;
        depth = entry.depth;
        Value = entry.Value;
        best_Move = entry.best_Move;
		hash = entry.hash;
		posB = 	entry.posB;
    	posR =	entry.posR;
    }
    
    public void setBestMove(Move m) {
    	
        best_Move = (short)Move.computeOldIndex(m.fromRow,m.fromCol,m.toRow,m.toCol);
    }

   public Move getBestMove(Game sbe) {
	 
       Move m =  sbe.getGenerator().generateMove();
       m.setOldIndexToMove(best_Move);
       return m;
   }
    
    public void setPosition(Board b){
    	
    	hash = b.hashValue;
    	posB = 	b.posB;
    	posR =	b.posW;	
			
    }
    
    public void computeHash(){
    	hash= TranspositionTable.getHashValue(posB, posR,Side);
    }


    
    public boolean equals(Board b){
    	
    	return posB == b.posB && posR == b.posW && Side == b.sideToMove;
    }
    
    public boolean equals(OpeningBookEntry entry){
 
    	
    	return posB == entry.posB && posR == entry.posR && Side == entry.Side;
    }
    public boolean equals(Object e) {
    	
    	if(e instanceof OpeningBookEntry)
    		return equals((OpeningBookEntry) e);
    	
    	return false;
    	
    }
    
    public final int hashCode() {
		  return (int) (this.hash) ;
		  }
    
    

    public void setMirrorXEntry(OpeningBookEntry entry, Game sbe){
 
    	
    	posB = Symmetry.mirrorX(entry.posB);   
        posR = Symmetry.mirrorX(entry.posR); 
        setMirrorX(entry,sbe);
    	
    } 

    public void setMirrorYEntry(OpeningBookEntry entry, Game sbe){   

    	
    	posB = Symmetry.mirrorY(entry.posB);   
        posR = Symmetry.mirrorY(entry.posR); 
        setMirrorY(entry,sbe);
    } 

    public void setMirrorXYEntry(OpeningBookEntry entry, Game sbe){  

    	
    	posB = Symmetry.mirrorX(Symmetry.mirrorY(entry.posB));   
        posR = Symmetry.mirrorX(Symmetry.mirrorY(entry.posR)); 
        setMirrorXY(entry,sbe);
    }


  /*  public void setBestMove(Move m) {
        best_Move = (short) ((m.from_row << 9) + (m.from_col << 6)
                + (m.to_row << 3) + m.to_col);
    }
    

    public Move getBestMove(Game sbe) {
        return  sbe.getGenerator().generateMove(((best_Move >> 9) & 7),
                ((best_Move >> 6) & 7), ((best_Move >> 3) & 7),
                ((best_Move) & 7));
    }*/
    

   


    public void copyRest(OpeningBookEntry entry) {

        depth = entry.depth;
        Value = entry.Value;
        best_Move = entry.best_Move;	
    }
    

    
protected final void setMirrorX(OpeningBookEntry entry, Game sbe){
 
    	Move m = entry.getBestMove(sbe);
    	m.mirrorX();   
    	setBestMove(m);
    	computeHash();
    } 

protected final void setMirrorY(OpeningBookEntry entry, Game sbe){   

    	Move m = entry.getBestMove(sbe);
    	m.mirrorY();   
    	setBestMove(m);
    	computeHash();
    } 

protected final void setMirrorXY(OpeningBookEntry entry, Game sbe){  

    	Move m = entry.getBestMove(sbe);
    	m.mirrorXY(); 
    	setBestMove(m);
    	computeHash();
    }
}

