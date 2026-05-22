
public class SurakartaAmsEvaluator extends SurakartaAbstractEvaluator {
	
	private static final int ORIGIN = SurakartaGame.STANDARD_ORIGIN;
	private static final short [][] mobArray;
	private static final short [] hMobArray;
	private static final int BORDER = SurakartaGame.STANDARD_REAL_BORDER-1;
	private static int[] lineCount;
	private final long[] table = new long[1 << SHIFT];
	private static final int COMB_RANGE = (1<<SurakartaGame.STANDARD_WIDTH)-1;

	private static final short [][] line_moves;
	
		
	private final int [] centreValue = 	{3,10,10,10,10,3,
			 10,11,10,10,11,10,
			 10,10,10,10,10,10,
			 10,10,10,10,10,10,
			 10,11,10,10,11,10,
			 3, 10,10,10,10,3,
			};
		
	
	private static final int MOBILITY_VALUE =5;
	
	public SurakartaAmsEvaluator() {
		
	    for (int i = table.length; --i >= 0;) {
	        table[i] = NO_ELEMENT;
	    }
		weight = new double[]{10,5,10,10,10};
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
	    
	    int  lines =0;
		final int [] Horizontal_Bit_Lines = b.Horizontal_Bit_Lines;
		final int [] Vertical_Bit_Lines = b.Vertical_Bit_Lines;
	    int blueB = (Vertical_Bit_Lines[1]&COMB_RANGE) |(Vertical_Bit_Lines[4]&COMB_RANGE) | (Horizontal_Bit_Lines[1]&COMB_RANGE) |(Horizontal_Bit_Lines[4]&COMB_RANGE) ;
	    int blueW = (Vertical_Bit_Lines[1]>>>Board.WIDTH) |(Vertical_Bit_Lines[4]>>>Board.WIDTH) | (Horizontal_Bit_Lines[1]>>>Board.WIDTH) |(Horizontal_Bit_Lines[4]>>>Board.WIDTH);
	
	    
	    int greenB = (Vertical_Bit_Lines[2]&COMB_RANGE) |(Vertical_Bit_Lines[3]&COMB_RANGE) | (Horizontal_Bit_Lines[2]&COMB_RANGE) |(Horizontal_Bit_Lines[3]&COMB_RANGE) ;
	    int greenW = (Vertical_Bit_Lines[2]>>>Board.WIDTH) |(Vertical_Bit_Lines[3]>>>Board.WIDTH) | (Horizontal_Bit_Lines[2]>>>Board.WIDTH) |(Horizontal_Bit_Lines[3]>>>Board.WIDTH);
		
	    final int side = b.sideToMove;
	    if(greenB==0 || greenW==0 || blueB==0 || blueW==0){
	   
	    if(greenB==0 && greenW==0 && blueB==0 && blueW==0)
	    	if(side ==Board.B_SIDE )
	    		lines = 500;
	    	else lines =-500;
	    else if(greenB==0 && greenW==0 && blueB==0 && blueW!=0)
	    	lines =-500;
	    else  if(greenB==0 && greenW==0 && blueB!=0 && blueW==0)
	    	lines =500;
	   /* else  if(greenB==0 && greenW==0 && blueB!=0 && blueW!=0)
	    	lines =0;*/
	    else   if(greenB==0 && greenW!=0 && blueB==0 && blueW==0){
	    	if(side ==Board.W_SIDE )
	    	   lines =-500;
	    	else lines =15;
	    	}
	    else   if(greenB==0 && greenW!=0 && blueB==0 && blueW!=0)
	    	lines =-500;
	  else   if(greenB==0 && greenW!=0 && blueB!=0 && blueW==0)
	    	lines =15;
	  else   if(greenB==0 && greenW!=0 && blueB!=0 && blueW!=0)
	         	lines =-25;
	  else   if(greenB!=0 && greenW==0 && blueB==0 && blueW==0)
		  if(side ==Board.B_SIDE )
	    		lines = 500;
	    	else lines =-15;
	  else   if(greenB!=0 && greenW==0 && blueB==0 && blueW!=0)
		  lines =-15;
	  else   if(greenB!=0 && greenW==0 && blueB!=0 && blueW==0)
		     lines = 500;
	  else   if(greenB!=0 && greenW==0 && blueB!=0 && blueW!=0)
		  lines = 25;
	 // else   if(greenB!=0 && greenW!=0 && blueB==0 && blueW==0)
	  else   if(greenB!=0 && greenW!=0 && blueB==0 && blueW!=0)
		  lines = -40;
	  else   if(greenB!=0 && greenW!=0 && blueB!=0 && blueW==0)
		  lines = 40;
	    }
			 
		boardMobility (b);
	     //(boardMobility(b,Board.B_SIDE)-boardMobility(b,Board.W_SIDE));
		 feature[2] = (side == Board.W_SIDE) ? -15 : 15;
		   	feature[3] = lines;
		 eval =  staticValue_B - staticValue_R  + ((int)( weight[1]* feature[1]+ weight[2]* feature[2]+weight[3]*feature[3])+ (randomIntValue()));
	    
		 if(eval>9000)
			 eval=9000;
		 else if(eval<-9000)
			 eval = -9000;
		 
		 store(b.hashValue, eval);
	    
	    return eval;	
	}
	
	private int computeStaticSideEvaluation(Board b, Piece [] list, int n, int side){
	       
		int value =0;
		
		for(int i = n; --i>=0;){
			Piece p = list[i];
			value += 10*centreValue[p.index];			
		}
		
		int max =2;
		int count;
		
		
		if(side ==Board.B_SIDE)
		for(int i = SurakartaGame.STANDARD_WIDTH;--i>=0;){
			count = lineCount[b.Horizontal_Bit_Lines[i]&COMB_RANGE];
			if(count > max)
				max  =count;
			count = lineCount[b.Vertical_Bit_Lines[i]&COMB_RANGE];
			if(count > max)
				max  =count;
			
		}else
			for(int i = SurakartaGame.STANDARD_WIDTH;--i>=0;){
				count = lineCount[b.Horizontal_Bit_Lines[i]>>SurakartaGame.STANDARD_WIDTH];
				if(count > max)
					max  =count;
				count = lineCount[b.Vertical_Bit_Lines[i]>>SurakartaGame.STANDARD_WIDTH];
				if(count > max)
					max  =count;
				
			}
		
		float distribution = (25f*n)/max;
				
		if(noHash){
		 feature[0] += side*value;		
		 feature[4] += side*distribution;	
		}
		    
		   return (int) ( weight[0]* value + weight[4]*distribution); 
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
	
	  

	  
	  private static int countBits( int x )
	   {
	   // collapsing partial parallel sums method
	   // collapse 32x1 bit counts to 16x2 bit counts, mask 01010101
	   x = (x >>> 1 & 0x55555555) + (x & 0x55555555);
	   // collapse 16x2 bit counts to 8x4 bit counts, mask 00110011
	   x = (x >>> 2 & 0x33333333) + (x & 0x33333333);
	   // collapse 8x4 bit counts to 4x8 bit counts, mask 00001111
	   x = (x >>> 4 & 0x0f0f0f0f) + (x & 0x0f0f0f0f);
	   // collapse 4x8 bit counts to 2x16 bit counts
	   x = (x >>> 8 & 0x00ff00ff) + (x & 0x00ff00ff);
	   // collapse 2x16 bit counts to 1x32 bit count
	   return(x >>> 16) + (x & 0x0000ffff);
	   }

	
	private static void calculateMobility(Move move_list, int j,int side){  
		  int  capteller, b_stones, r_stones;
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
		
		lineCount = new int[1<<SurakartaGame.STANDARD_WIDTH];
		
		for(int i =lineCount.length;--i>=0;)
			lineCount[i] = countBits(i);
	
	
	}

}
