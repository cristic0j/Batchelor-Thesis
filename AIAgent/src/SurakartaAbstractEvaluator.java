
public abstract class SurakartaAbstractEvaluator extends Evaluator {

	private static byte [][] partArray;
	
	public final int getNumberOfMoves(Board b, final int side){
	     
		  int i, mobility=0;
		  final int [] Horizontal_Bit_Lines = b.Horizontal_Bit_Lines;
		  final int [] Vertical_Bit_Lines = b.Vertical_Bit_Lines;
		  final int [] SWNE_Bit_Lines = b.SWNE_Bit_Lines;
		  final int [] SENW_Bit_Lines = b.SENW_Bit_Lines; 
		     
		  for(i =  SurakartaGame.STANDARD_WIDTH; --i>=0;)
			   {mobility += getIncNumberOfMoves( SurakartaGame.STANDARD_REAL_BORDER - 1, Horizontal_Bit_Lines[i],side )
				         + getIncNumberOfMoves( SurakartaGame.STANDARD_REAL_BORDER - 1, Vertical_Bit_Lines[i],side)       
				         +  getIncNumberOfMoves(i-1, SWNE_Bit_Lines[i],side)
				         +  getIncNumberOfMoves( SurakartaGame.STANDARD_REAL_BORDER - 1-i, SENW_Bit_Lines[i],side );
			   } 
		      
			 for(i = b.SWNE_Bit_Lines.length -1; i >= SurakartaGame.STANDARD_WIDTH; i--)
			   {
				mobility += getIncNumberOfMoves( SurakartaGame.STANDARD_ORIGIN-i, SWNE_Bit_Lines[i],side )+
				getIncNumberOfMoves( SurakartaGame.STANDARD_ORIGIN-i, SENW_Bit_Lines[i],side );
			   }
		         
		   return mobility;
		}
	
	private static int getIncNumberOfMoves(int i, int j, int side){

		  if(i<0)
		   return 0;

		 if(side > 0)
		   return partArray[i][j]&(7);

		 return partArray[i][j]>>3; 
		}
	
	private static void calculateMobility(Move move_list, int j,int side){  
		  int  teller, b_stones, r_stones;
		  Move m;
		  
			for(int i =6-1; --i>=0;)
			 {
			  teller =0;      
			  b_stones = j & ((1<<6)-1);
			  r_stones = j>>6;
			 
		    
			  if(b_stones <(1<<(i+2)) && r_stones <(1<<(i+2))){
			   for(m = move_list; m !=null; m = m.next)
				   if(m.fromRow == 0 && m.toRow == 0 && m.toCol <= i+1){
				   
					teller++; 			
					   
					 
				   }
				 if(side > 0)     
					 partArray[i][j] ^= (byte) teller;
				  else
					  partArray [i][j] ^= (byte) (teller <<3);
			
				  }// ((i==0)?(side * (teller + capteller)): (side * 2* (teller + capteller))); //(byte)((line_moves[0][i][j] &7) - (line_moves[0][i][j]>>3));  
			  
			  }
			 }
	
static{
	 final int	n=6;
		 
	 partArray = new byte[n-1][];  
		 long hulp  = System.currentTimeMillis();

		Board.setSize(n, n);

		final int nBits = 1<<(2*n);

		SurakartaGenerator gen =new SurakartaGenerator();

		for(int i = 1 ; i < n; i++){
			partArray[i-1] = new byte[1<< (n+1+i)];
		}

		Board b = new Board(gen, new SurakartaGame());
								  
		int col;						  
		for(int i =nBits; --i>=0;){ 
			
		if(!Board.validBitLine(i,6))	
			continue;

		 b.clear();
		 byte[] row = b.space[0];
		 
		 for( col = n; --col>=0;)
			row[col] = Board.getPieceFromLineBits(i,col,n); 

		     
		 b.initialise(Board.B_SIDE);
		      
		 calculateMobility(gen.generateNoCaptureMoves(b,Board.B_SIDE), i, Board.B_SIDE);
		 calculateMobility(gen.generateNoCaptureMoves(b,Board.W_SIDE), i, Board.W_SIDE); 
		} 

		 Log.println("Surakarta Move Patterns: " + (System.currentTimeMillis()-hulp));	
	}
	   
}
