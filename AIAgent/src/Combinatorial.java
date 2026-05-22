import java.util.ArrayList;

public abstract class Combinatorial {
	
	public static final int ZERO = 0;
	public static final int LEFT = 1;
	public static final int RIGHT = 2;
	public static final int FUZZ = 3;
	public static final int UR= 4;
	public static final int LEFTFUZZ= 5;
	public static final int RIGHTFUZZ= 6;
	
	public static final int [][] OutComeTable = {
		{ZERO,LEFT,RIGHT,FUZZ},
		{LEFT,LEFT,UR,LEFTFUZZ},
		{RIGHT,UR,RIGHT,RIGHTFUZZ},
		{FUZZ,LEFTFUZZ,RIGHTFUZZ,UR}
	};
	
   @SuppressWarnings("rawtypes")
protected final ArrayList boardPool = new ArrayList();
	
   @SuppressWarnings("rawtypes")
public abstract ArrayList getSubGames(Board b);
    
   @SuppressWarnings("rawtypes")
public abstract int filterSimpleGames(ArrayList list, Board conBoard);
   
   @SuppressWarnings({ "unchecked", "rawtypes" })
  public void recycle (ArrayList list){
	   
	   boardPool.addAll(list);
	   
   }
   
   @SuppressWarnings("unchecked")
  public void recycle (Object o){
	   
	   boardPool.add(o);
	   
   }
   
	public int getMaxMovPieces(Board b){
		return 0;
	}   

	public void addSubGame(Board from, Board to){

	        byte[][] tospace = to.space;
	        byte[][] fromspace = from.space;
		  	
			Piece[] p = from.arrayB;
				
			for(int i = from.B_Counter;--i>=0;)
			 tospace[p[i].row][p[i].col] = fromspace[p[i].row][p[i].col];
			
			p = from.arrayW;
			
			for(int i = from.W_Counter;--i>=0;)
				tospace[p[i].row][p[i].col] = fromspace[p[i].row][p[i].col];
	}
   
	public final Board createBoard(final Game game){
		
		final int poolSize = boardPool.size();
		  
		  if(poolSize>0){
			  
			  Board newBoard = (Board) boardPool.get(poolSize-1);
			  boardPool.remove(poolSize-1);
			  return newBoard;
		  }
		  else
		  return new Board(game);
	}
	

}
