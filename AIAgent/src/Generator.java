import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;

/**
 Abstract class containing the a move generator for a particular game.
 */
public abstract class Generator {
	
	protected Move usedObjects = null;
	protected long[]markedMoves;
    protected long marker = 0;
    protected int counter;
    
    public final static int FORCED = Integer.MAX_VALUE;
 
    /**
     Generate a list of legal moves from the given board configuration.
     */
    public abstract Move generateMoves(Board b, int side);
    
    public Move generateMoves(Board b){
    	
    	return generateMoves(b, b.sideToMove);
    }
  
    public abstract Move generateQuiescenceMoves(Board b, int side);
    
    public abstract Move generateCaptureMoves(Board b, int side);
    
    public abstract Move generateNoCaptureMoves(Board b, int side);
    
    public final Move generateMove(Move m){
    	
    	return generateMove(m.fromRow,m.fromCol, m.toRow,m.toCol);
    }
    
    protected boolean checkQuiescenceMove(Board b, int side, 
            int row, int col, int to_row, int to_col) {
                            
        return false; }
  
    public final Move generateMove(int f_row, int f_col, int t_row, int t_col) {
        if (usedObjects != null) {
            Move m = usedObjects;

            usedObjects = usedObjects.next;
            m.next = null;
            m.setMove(f_row, f_col, t_row, t_col);
            return m;
        }
   
        return createMove(f_row, f_col, t_row, t_col);
    }
    
    
    public final Move generateMove(int f_row, int f_col, int t_row, int t_col, int index) {
    	    	
        if (usedObjects != null) {
            Move m = usedObjects;

            usedObjects = usedObjects.next;
            m.next = null;
            m.setMove(f_row, f_col, t_row, t_col, index);
            
            return m;
        }
   
        return createMove(f_row, f_col, t_row, t_col);
    }
   
    protected abstract Move createMove(int f_row, int f_col, int t_row, int t_col);

	public final Move generateMove(){
    	
        if (usedObjects != null) {
            Move m = usedObjects;

            usedObjects = usedObjects.next;
            m.next = null;
            return m;
        }
    	
      return createMove();
    }
    
    protected abstract Move createMove();
  
    //check whether a valid move is legal for the particular position
    public boolean validMove(Board b, Move transmove) { 
  	   
        return (generateIncMove(b, b.sideToMove, transmove, null, 0, false) != null);
    } 
    
    // check whether a move is legal for a for the particular position
    public boolean legalMove(Board b, Move checkMove){
    	Move move_list = generateMoves(b);
    	
     for(Move m = move_list; m !=null; m=m.next)
          	if(m.equals(checkMove)) 
                   return true;
    	
    	
    	return false;
    }
  
    public abstract Move generateIncMove(Board b, int side, Move transmove, long[] MMoves, long M, boolean p);
  
    public final Move generateUnMarkedMoves(Board b, int side, long[] MMoves, long M) {
        markedMoves = MMoves;
        marker = M;
   
        final Move move_list = generateMoves(b, side);
   
        markedMoves = null;
 
        return move_list;
    }
  
    public final Move generateUnMarkedCaptureMoves(Board b, int side, long[] MMoves, long M) {
        markedMoves = MMoves;
        marker = M;
   
        final Move move_list = generateQuiescenceMoves(b, side);
   
        markedMoves = null;
 
        return move_list;
    }
  
    public abstract Move movePiece(Move move_list, Board b, int side, int row, int col);
                            
    public final void recycleMove(Move m) {
        m.next = usedObjects; 
        usedObjects = m;
    }
    
    
    public int countRecycledMoves() {
        int count = 0;
 
        for (Move m = usedObjects; m != null; m = m.next) {
            count++;
        }
     
        return count;
    }  
     
    public Move copyMoveList(Move list){
    
	    Move  savedList = list;   
	    list = null;
	    
	    for(Move mo =  savedList; mo.next!=null; mo = mo.next){
			Move hulp =this.generateMove(mo.fromRow, mo.fromCol, mo.toRow, mo.toCol);
			hulp.next = list;
			list = hulp;
		}
	    
	    return list;
    }
            
    protected static void readRPWeights(double [] rp, double [] logrp, String name){
    
        long hulp = System.currentTimeMillis(); 
         
        final int n = rp.length; 
        int counter =0;
        URL url = Generator.class.getResource(name);
        
        if(url==null){
            Log.println("WARNING: " + name + " are not loaded!"); 
            java.util.Arrays.fill(rp, 0.25);
            return;}
              
        try(java.io.InputStream inputStream= url.openStream();
            BufferedReader in = new BufferedReader(new java.io.InputStreamReader (inputStream));){

            for (int i = 0; i < n; i++) {
                String s = in.readLine();
                
                if(s==null)
                 break;

                if (s.equals("NaN")) {
                    rp[i] = 0.25;
                } else {
                    //rp[i] = Double.valueOf(s).floatValue() + 0.000000001;
                    rp[i] = Double.parseDouble(s)+ 0.000000001d;
                    counter++;
                }
                
                logrp[i] = Math.log(rp[i]); 
            }
           
        } catch (IOException e) {
            Log.println("WARNING: " +name + " are not loaded!"); 
            java.util.Arrays.fill(rp, 0.25);}
       finally {
            Log.println(
                    "Time to read weights:" + (System.currentTimeMillis() - hulp));
        }
        
        Log.println("Real Prob:" + counter);	 
    }
    
    public double[] getLOGRPPROB() {
        return new double[1];
    }
    
    public void recycle(Move move_list) {  
  	  
        Move m = move_list, next;   

        while (m != null) {
            next = m.next;
            recycleMove(m);
            m = next;
        }
    }
                            
}
  
