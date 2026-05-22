

public class OpeningBookBigEntry extends OpeningBookEntry {


	private final byte[][] position = new byte[Board.HEIGHT][Board.WIDTH];
	/**
	 * 
	 */
	private static final long serialVersionUID = -2500414072087475859L;

	public OpeningBookBigEntry(Board b) {
		super(b);
		
		for(int i = Board.WIDTH; --i>=0;)
			for(int j = Board.HEIGHT; --j>=0;)
				position[i][j] =  b.space[i][j];
	}

	public OpeningBookBigEntry(OpeningBookBigEntry entry) {
		super(entry);
		
		for(int i = Board.WIDTH; --i>=0;)
			for(int j = Board.HEIGHT; --j>=0;)
				position[i][j] =  entry.position[i][j];
	}
	
	
	   public void setPosition(Board b){
		   hash = b.hashValue;
	    	
		   for(int i = Board.WIDTH; --i>=0;)
				for(int j = Board.HEIGHT; --j>=0;)
					position[i][j] =  b.space[i][j];

	    }
	   
	   private  void setPosition(OpeningBookBigEntry entry){
		   hash = entry.hash;
	    	
		   for(int i = Board.WIDTH; --i>=0;)
				for(int j = Board.HEIGHT; --j>=0;)
					position[i][j] =  entry.position[i][j];

	    }
	   
	   public byte[][] getPosition(){
		  return  position.clone();

	    }
	   
	   public void computeHash(){
	    	hash= TranspositionTable.getHashValue(position,
	               Side);
	    }
	
    
    public void setBestMove(Move m) {
       best_Move = (short)m.index;
   }
   
    

   public Move getBestMove(Game sbe) {
       Move m =  sbe.getGenerator().generateMove();
       m.setMove((char)best_Move);
       return m;
   }
   
  
   public boolean equals(OpeningBookEntry entry){
	   
	   if(entry instanceof OpeningBookBigEntry){
		   
		   OpeningBookBigEntry en = (OpeningBookBigEntry) entry;
		   
			if(hash!= en.hash || this.Side != en.Side)
				return false;
			
			for(int i = Board.WIDTH; --i>=0;)
				for(int j = Board.HEIGHT; --j>=0;)
					if(position[i][j] !=  en.position[i][j])
						return false;
	         }
	   
	   return true;
   }
   
   
	
	public boolean equals(Board b){
		
		if(hash!= b.hashValue  || this.Side != b.sideToMove)
			return false;
		
		for(int i = Board.WIDTH; --i>=0;)
			for(int j = Board.HEIGHT; --j>=0;)
				if(b.space[i][j]!= position[i][j])
					return false;
		
		
		return true;
	}
	
    public void setMirrorXEntry(OpeningBookEntry entry, Game sbe){
   	 
    	OpeningBookBigEntry en = (OpeningBookBigEntry) entry;
    	setPosition(en);
    	Symmetry.mirrorX(position); 
        setMirrorX(entry,sbe);
    	
    } 

    public void setMirrorYEntry(OpeningBookEntry entry, Game sbe){   

    	OpeningBookBigEntry en = (OpeningBookBigEntry) entry;
    	setPosition(en);
    	Symmetry.mirrorY(position); 
        setMirrorY(entry,sbe);
    } 

    public void setMirrorXYEntry(OpeningBookEntry entry, Game sbe){  

    	OpeningBookBigEntry en = (OpeningBookBigEntry) entry;
    	setPosition(en);
    	Symmetry.mirrorX(position); 
    	Symmetry.mirrorY(position); 
        setMirrorXY(entry,sbe);
    }
}
