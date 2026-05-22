
public class SurakartaMobEvaluator extends SurakartaAbstractEvaluator {
	
	private static final int ORIGIN = SurakartaGame.STANDARD_ORIGIN;
	private static final short [][] mobArray;
	private static final short [] hMobArray;
	private static final int BORDER = SurakartaGame.STANDARD_REAL_BORDER-1;
	private final long[] table = new long[1 << SHIFT];

	private static final short [][] line_moves;
	
		
	private final int [] centreValue = 	{3,9,9,9,9,3,
			 10,11,10,10,11,10,
			 10,10,10,10,10,10,
			 10,10,10,10,10,10,
			 10,11,10,10,11,10,
			 3, 9,9,9,9,3,
			};
		
	
	private static final int MOBILITY_VALUE =5;
	
	public SurakartaMobEvaluator() {
		
	    for (int i = table.length; --i >= 0;) {
	        table[i] = NO_ELEMENT;
	    }
		weight = new double[]{1,1,1};
		feature = new double[weight.length];
	}
	
	private void store(long HashValue, int value) {
	    table[(int) (HashValue & HASH_KEY_MASK)] = (HashValue & MASK)
	            | (value + TRANSPOSE_VALUE);
	}

	private int retrieve(long HashValue) {
	    long value = table[(int) (HashValue & HASH_KEY_MASK)];
	 
	    if (value >> SHIFT == HashValue >> SHIFT) {
	        return (int) (value & VALUE_MASK) - TRANSPOSE_VALUE;
	    }
	   
	    return NO_ELEMENT; 
	}


	public int evaluate(Board b, int lastturn) {
		
		if(noHash){
			
			for(int i = feature.length;--i>=0;)
				feature[i]=0;
			
		}
		
	    int eval = retrieve(b.hashValue);

	    if (eval != NO_ELEMENT && (!noHash)) {
	        return eval;
	    }

	    // phase = (b.B_Counter+b.R_Counter) / MAX_PIECES;
	    // find patterns in TT, otherwise calculate them

	    int staticValue_B = retrieve(b.blackHashValue);

	    if (staticValue_B == NO_ELEMENT || noHash) {
	        staticValue_B = computeStaticSideEvaluation(b, b.arrayB, b.B_Counter,
	                Board.B_SIDE);
	        store(b.blackHashValue, staticValue_B);
	    }

	    /* else
	     TTHitsB++;*/
	 
	    int staticValue_R = retrieve(b.whiteHashValue); 

	    if (staticValue_R == NO_ELEMENT || noHash) {
	        staticValue_R = computeStaticSideEvaluation(b, b.arrayW, b.W_Counter,
	                Board.W_SIDE);
	        store(b.whiteHashValue, staticValue_R);
	    }

	    /* else
	     TTHitsR++;*/
			
		boardMobility (b);
	     //(boardMobility(b,Board.B_SIDE)-boardMobility(b,Board.W_SIDE));
		 feature[2] = (lastturn == Board.B_SIDE) ? -15 : 15;		
		 eval =  staticValue_B - staticValue_R  + ((int)( weight[1]* feature[1]+ weight[2]* feature[2])+ (randomIntValue()/4));
	    store(b.hashValue, eval);
	    
	    return eval;
	
	}
	
	private int computeStaticSideEvaluation(Board b, Piece [] list, int n, int side){
	       
	int value =0;
		
		for(int i = n; --i>=0;){
			Piece p = list[i];
			value += 10*centreValue[p.index];			
		}
				
		if(noHash){
		 feature[0] += side*value;		
		}
		    
		   return (int) ( weight[0]* value); 
	 }
	

	

	  private  void boardMobility (Board b){
		   
			 int i;
			 final int [] Horizontal_Bit_Lines = b.Horizontal_Bit_Lines;
			 final int [] Vertical_Bit_Lines = b.Vertical_Bit_Lines;
			 final int [] SWNE_Bit_Lines = b.SWNE_Bit_Lines;
			 final int [] SENW_Bit_Lines = b.SENW_Bit_Lines;   
		     
			 int mobility = hMobArray[SENW_Bit_Lines[0]] + hMobArray[SWNE_Bit_Lines[Board.REAL_BORDER]];
			
			 for(i =  Board.REAL_BORDER; --i>=1;)
			   {mobility += hMobArray[Horizontal_Bit_Lines[i]] + hMobArray[Vertical_Bit_Lines[i]]
				+  mobArray[i-1][SWNE_Bit_Lines[i]] + mobArray[BORDER-i][SENW_Bit_Lines[i]];
			   } 

		    int horigin =0;
			 for(i = ORIGIN; i >=Board.WIDTH; i--, horigin++){
			 	mobility += mobArray[horigin][SWNE_Bit_Lines[i]] + mobArray[horigin][SENW_Bit_Lines[i]];
			  }
		     
			mobility += (  ((hMobArray[Horizontal_Bit_Lines[Board.REAL_BORDER]] + hMobArray[Vertical_Bit_Lines[Board.REAL_BORDER]]+
			+ hMobArray[Horizontal_Bit_Lines[0]] + hMobArray[Vertical_Bit_Lines[0]])>>1));
			
			if(mobility>50)
				mobility=50;
			else
				if(mobility<-50)
					mobility=-50;
			
			feature[1]=mobility;
		   }
	

	
	private static void calculateMobility(Move move_list, int j,int side){  
		  int capteller, b_stones, r_stones;
		  Move m;
		  
			for(int i =6-1; --i>=0;)
			 {
			  capteller =0;      
			  b_stones = j & ((1<<6)-1);
			  r_stones = j>>6;
			 
			  if(b_stones <(1<<(i+2)) && r_stones <(1<<(i+2))){
			   for(m = move_list; m !=null; m = m.next)
				   if(m.fromRow == 0 && m.toRow == 0 && m.toCol <= i+1){
				   
					capteller++; 
					
					 if(m.toCol > 0 && m.toCol < i+1){
					  				
					   capteller++;
					 }	   
					 
				   }
			       line_moves[i][j] += MOBILITY_VALUE*side * capteller;// ((i==0)?(side * (teller + capteller)): (side * 2* (teller + capteller))); //(byte)((line_moves[0][i][j] &7) - (line_moves[0][i][j]>>3));  
			  
			  }
			 }
	}
	
	
	static{
	 final int	n=6;
		 
		 line_moves = new short[n-1][];  
		 long hulp  = System.currentTimeMillis();

		Board.setSize(n, n);

		final int nBits = 1<<(2*n);

		SurakartaGenerator gen =new SurakartaGenerator();


		for(int i = 1 ; i < n; i++){
			line_moves[i-1] = new short[1<< (n+1+i)];
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

		 Log.println("Surakarta Eval Time: " + (System.currentTimeMillis()-hulp));
		 Log.println(""+n);

		mobArray = line_moves;
		hMobArray = line_moves[BORDER];

		hulp = System.currentTimeMillis();

		Log.println("PatternDatabase Time: " + (System.currentTimeMillis()-hulp));
		}

}
