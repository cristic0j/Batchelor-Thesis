import java.util.ArrayList;
import java.util.Arrays;

public class Combinatorics 
{

	
	public static long getIndexMirrorY(byte[][] space, int wS, int bS){
		
		final int sum = wS+bS;
		
		long positionIndex=0;
		long orderIndex=0;
		
		int counter=0;
		int counter2=0;
		
		for (int i= Board.HEIGHT; --i>=0;){
			final byte [] board = space[i];
		
			for (int j=0; j<Board.WIDTH;j++){	
			if (board[j]!=Board.EMPTY) 
			{
				positionIndex+=getIndexedOver(((Board.WIDTH * (Board.HEIGHT-1-i)) + j), counter+1);
					
				if (board[j]==Board.B_PIECE) 
				{
					orderIndex+=getIndexedOver(counter, counter2+1);
					counter2++;
				}
				counter++;
			}
			}
		}

		return positionIndex+(getIndexedOver(Board.SQUARE,sum)*orderIndex);	
	}
	

	public static long getIndexMirrorXY(byte[][] space, int wS, int bS){
		
		final int sum = wS+bS;
		
		long positionIndex=0;
		long orderIndex=0;
		
		int counter=0;
		int counter2=0;
		
		for (int i= Board.HEIGHT; --i>=0;){
			final byte [] board = space[i];
		
			for (int j=Board.WIDTH;--j>=0;){	
			if (board[j]!=Board.EMPTY) 
			{
				positionIndex+=getIndexedOver(((Board.WIDTH * (Board.HEIGHT-1-i)) + Board.WIDTH-1-j), counter+1);
					
				if (board[j]==Board.B_PIECE) 
				{
					orderIndex+=getIndexedOver(counter, counter2+1);
					counter2++;
				}
				counter++;
			}
			}
		}

		return positionIndex+(getIndexedOver(Board.SQUARE,sum)*orderIndex);
		
	}
	
	

	public static long getIndexMirrorX(byte[][] space, int wS, int bS){
		
		final int sum = wS+bS;
		
		long positionIndex=0;
		long orderIndex=0;
		
		int counter=0;
		int counter2=0;
		
		for (int i=0; i<Board.HEIGHT; i++){
			final byte [] board = space[i];
		
			for (int j=Board.WIDTH; --j>=0;){	
			if (board[j]!=Board.EMPTY) 
			{
				positionIndex+=getIndexedOver(((Board.WIDTH * i) + Board.WIDTH-1-j), counter+1);
					
				if (board[j]==Board.B_PIECE) 
				{
					orderIndex+=getIndexedOver(counter, counter2+1);
					counter2++;
				}
				counter++;
			}
			}
		}

		return positionIndex+(getIndexedOver(Board.SQUARE,sum)*orderIndex);
		
	}
	

	public static long getReverseIndex(byte[][] space, int wS, int bS){
		
		final int sum = wS+bS;
				
		long positionIndex=0;
		long orderIndex=0;
		
		int counter=0;
		int counter2=0;
		
		for (int i=0; i<Board.HEIGHT; i++){
			final byte [] board = space[i];
		
			for (int j=0; j<Board.WIDTH; j++){	
			if (board[j]!=Board.EMPTY) 
			{
				positionIndex+=getIndexedOver(((Board.WIDTH * i) + j), counter+1);
						
				if (board[j]==Board.W_PIECE) 
				{
					orderIndex+=getIndexedOver(counter, counter2+1);
					counter2++;
				}
				counter++;
			}
			}
		}
			
		return positionIndex+(getIndexedOver(Board.SQUARE,sum)*orderIndex);
		
	}
	

	public static long getIndex(byte[][] space, int wS, int bS){
		
		final int sum = wS+bS;
				
		long positionIndex=0;
		long orderIndex=0;
		
		int counter=0;
		int counter2=0;
		
		for (int i=0; i<Board.HEIGHT; i++){
			final byte [] board = space[i];
		
			for (int j=0; j<Board.WIDTH; j++){	
			if (board[j]!=Board.EMPTY) 
			{
				positionIndex+=getIndexedOver(((Board.WIDTH * i) + j), counter+1);
						
				if (board[j]==Board.B_PIECE) 
				{
					orderIndex+=getIndexedOver(counter, counter2+1);
					counter2++;
				}
				counter++;
			}
			}
		}
			
		return positionIndex+(getIndexedOver(Board.SQUARE,sum)*orderIndex);
		
	}
		
	public static long getIndex(byte[][] space)
	{
		//Count pieces
	
		int wS=0;
		int bS=0;
		
		for (int i=Board.HEIGHT;--i>=0;){
			
			final byte [] board = space[i];
		
			for (int j=Board.WIDTH;--j>=0;){
			
				final byte pos = board[j];
			
				if (pos==Board.W_PIECE) wS++; 
				else 
				if (pos==Board.B_PIECE) bS++;
			}
		}
		
		return getIndex(space,wS,bS);
	}
	
	public static void setConf(byte[][] board, long index, int wS, int bS){
		
		final int boardSize = Board.SQUARE;
		final int sum = wS+bS;
		
		final long possiblePositions=getIndexedOver(boardSize,sum);
		
		long orderIndex = index/possiblePositions;
		
		/*ArrayList s = symmetryList[wS-1][bS-1];
		//Log.println(s);
		boolean found = false;
		int counter=-1;
	    //Log.println("OrderIndex: "+orderIndex);
		
		
		while (!found)
		{
			counter++;
			//Log.println(wS+" " +bS+" " +counter + " "+s+" "+orderIndex);
			if (((Long)(s.get(counter))).longValue()==orderIndex)
				found=true;
		}
		orderIndex=counter;
		//Log.println("Order Index: "+orderIndex);*/
		
		final long positionIndex = index%possiblePositions;
				
		final byte[] positions = getPositionsfromCombinedIndex(positionIndex, sum, boardSize);
		final byte[] ordering = getPositionsfromCombinedIndex(orderIndex,bS,sum);
		
		
		
		for (int i=bS;--i>=0;)
		{
			board[positions[ordering[i]]/Board.WIDTH][positions[ordering[i]]%Board.WIDTH]=Board.B_PIECE;
		}

		for (int i=sum;--i>=0;)
		{
			int row = positions[i]/Board.WIDTH;
			int col = positions[i]%Board.WIDTH;			
			
			if (board[row][col]!=Board.B_PIECE) board[row][col]=Board.W_PIECE;
		}
			
	}
	
	public static byte[][] getConf(long index, int wS, int bS)
	{	
		final byte[][] board = new byte[Board.HEIGHT][Board.WIDTH];
		setConf(board, index, wS, bS);
		return board;
	
	}
	

	private static byte[] getPositionsfromCombinedIndex(long index,int numberofPieces,int boardSize)
	{
		byte currentGuess = (byte)(boardSize-1);
		final byte [] positions = new byte [numberofPieces];
		for (int i=numberofPieces;i>0;i--)
		{
			while (getIndexedOver(currentGuess,i)>index) currentGuess--;
			positions[i-1]=currentGuess;
			index-=getIndexedOver(currentGuess,i);
			currentGuess--;
		}
		return positions;
	}
	
	public static long over(final int n, final int m) 
	{
        if (n<=0 || n<m || m<0) return 0;
        if (m==0 || m==n) return 1;
        if (m==1 || m==n-1) return n;
        if (n>=binom.size()) bintab(n);
        return readbinom(n,m);
    }

	@SuppressWarnings("rawtypes")
	private static long readbinom(final int n, final int m) 
	{
        if (n>=binom.size()) return 0;
        ArrayList v = (ArrayList)(binom.get(n));
        if (m>=v.size()) return 0;
        return ((Long)(v.get(m))).longValue();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
	private static void bintab(final int n) 
    {
        final Long LongOne = 1L;//new Long(1);
        
        for (int i=binom.size(); i<=n; i++) 
        {
              ArrayList v= new ArrayList();
              v.add(LongOne);
              for (int j=1; j<i; j++)
            	  v.add(Long.valueOf(readbinom(i-1,j-1)+readbinom(i-1,j)));
            v.add(LongOne);
            binom.add(v);
        }
    }
    

    @SuppressWarnings({ "unchecked", "rawtypes" })
	public static void createSymmetryLists(int wP, int bP)
    {
    	int max = wP;
    	if (bP>wP) max=bP;
    	symmetryList = new ArrayList[max][max];
    	
    	for (int i=1;i<max+1;i++)
    	{
    		for (int j=1;j<max+1;j++)
    		{
    			int counter=0;
    			symmetryList[i-1][j-1]=new ArrayList();
    			long maxIndex = over(i+j,j);
    			double mirrorPoint = (i+j-1)/(2.0);
    			//Log.println("Max Index ("+i+"-"+j+"): "+maxIndex);
    			for (int k=0;k<maxIndex;k++)
    			{
    				byte[] ordering = getPositionsfromCombinedIndex(k,j,(byte)(i+j));
    				
    				byte[] mirrorOrdering = new byte[ordering.length];
    				for (int l=0;l<ordering.length;l++)
    				{
    					//Log.print("O: "+ordering[l]);
    					mirrorOrdering[l]=(byte)(ordering[l]+(2*(mirrorPoint-ordering[l])));
    					//Log.print(" M: "+mirrorOrdering[l]+"    ");
    				}
    				//Log.println();
    				Arrays.sort(mirrorOrdering);
    				long mirrorIndex = 0;
    				for (int l=0;l<ordering.length;l++)
    					mirrorIndex+=over(mirrorOrdering[l], l+1);
    	
    				//Log.println("Normal Index: "+k+" MirrorIndex: "+mirrorIndex);
    				if (mirrorIndex>=k) 
					{
						symmetryList[i-1][j-1].add(Long.valueOf(counter));
						counter++;
					}
    				else symmetryList[i-1][j-1].add(Long.valueOf(mirrorIndex));
    	
    			}
    			    		
    		}
    	}
    }

    @SuppressWarnings("rawtypes")
	private static ArrayList binom = new ArrayList();
    private final static long OVER_TABLE[][] = new long[65][24];
    @SuppressWarnings("rawtypes")
	private static ArrayList[][] symmetryList;
   
    
    private final static long getIndexedOver(int n,int m){
    	
    	return OVER_TABLE[n][m];
    }
    
	public static long possiblePositions(int boardSize, int wS, int bS)
	{
				return /*over(boardSize,wS+bS)*maxOrderingValues[wS-1][bS-1];posPos[boardSize][wS+bS][bS];*/over(boardSize,wS+bS)*over(wS+bS,bS);
	}
	
    static{
        	
    	for(int i =OVER_TABLE.length; --i>=0;)
    		for(int j = OVER_TABLE[0].length; --j>=0;)
    			OVER_TABLE[i][j] = over(i,j);
    	   	
    }
}
