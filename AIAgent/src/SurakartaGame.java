
public class SurakartaGame extends Game {
	
	public static final int STANDARD_WIDTH = 6;
	public static final int STANDARD_REAL_BORDER = STANDARD_WIDTH - 1;
	public static final int STANDARD_ORIGIN = STANDARD_WIDTH + (STANDARD_WIDTH - 1) - 2;

	public SurakartaGame() {
		this(new SurakartaKanazawaEvaluator());	 
	}
	
	public SurakartaGame(Evaluator e){
		generator = new SurakartaGenerator();
		bookName = "SurakartaOpeningBook";
		evaluator = e;
		isRPS = true;
		incMoveGeneration =true;
		this.isNullmove = false;
		neededMoves = 25;
		contemptFactor =-25;
		this.isEGDB  = false; 
		isMemEGDB  = true;
		bound = 2000;
		isKnowledgePN  =true;
        isLazy = false;
		//this.donThink=true;
		
	}
	
    public void startingConfiguration(Board b){
    	
    	super.startingConfiguration(b);
    	       	
       	byte [][]space = {{Board.W_PIECE,Board.W_PIECE,Board.W_PIECE,Board.W_PIECE,Board.W_PIECE,Board.W_PIECE},
		  {Board.W_PIECE,Board.W_PIECE,Board.W_PIECE,Board.W_PIECE,Board.W_PIECE,Board.W_PIECE},
		  {0,0,0,0,0,0},
		  {0,0,0,0,0,0},
		  {Board.B_PIECE,Board.B_PIECE,Board.B_PIECE,Board.B_PIECE,Board.B_PIECE,Board.B_PIECE},
		  {Board.B_PIECE,Board.B_PIECE,Board.B_PIECE,Board.B_PIECE,Board.B_PIECE,Board.B_PIECE}};
           
       	   b.setSpace(space);
       	   b.sideToMove = Board.B_SIDE;
    }



	public int gameTheoreticEvaluation(Board b, int lastturn) {
		
		if(b.B_Counter==0)
			return Board.W_WIN_SCORE;
		
		if(b.W_Counter==0)
			return Board.B_WIN_SCORE;
		
		
		return 0;
	}
	
	public final int checkOnDraw(Board b){
		
    	CompressedBoard[] history = b.history;
    	int counter = b.getNumberOfMovesPlayed();
  
        if (history[counter].conversion) {
            return 0;
        }
      
       int teller=0;
       int numOfOccurence = 0;
       --counter;
       long posB = b.posB;
       long posR = b.posW;
   
        while (counter > 0) {
            if (history[counter].conversion) {
                return numOfOccurence;
            }
            
            teller++;
            
            if(teller==50)
            	return Board.REPETITION;
       
             //next one
            if (history[--counter].posB == posB && history[counter].posW == posR) {
                if (numOfOccurence >0) {
                    return Board.REPETITION;
                } else {
                    numOfOccurence++;
                }
            } 
  
            if (history[counter].conversion) {
                return numOfOccurence;
            }  
       
            counter--;   
        }
 
        return numOfOccurence;
	}

	public int terminalEvaluation(Board b, int lastturn) {
		return gameTheoreticEvaluation(b, lastturn);
	}

	public String[][] getEvalNames() {
		
		String [][] names = {{"SIA 0.1", SurakartaEvaluator.class.getName()},
		{"SIA 0.2", SurakartaMobEvaluator.class.getName()},
		{"SIA 0.3", SurakartaAmsEvaluator.class.getName()},
		{"SIA 0.4", SurakartaUltimateEvaluator.class.getName()},
		{"SIA 1.1", SurakartaKanazawaEvaluator.class.getName()},
		{"SIA 1.2", Surakarta2016Evaluator.class.getName()},
		};
				
		return names;
	}

	public int getSize() {
		
		return STANDARD_WIDTH;
	}

	public void setSize(int size) {

	}
	
	 public double reward(int x) {
	     	if(x >= 2000)
	     		return 1;
	     	
	     	if(x<=-2000)
	     		return -1;
	     
	     return 0;
	 }
	 
	  /* private static double reward(double x) {
	    return  (2 / (1 + Math.exp(-2 * 0.005 * x)) - 1);}
	    
	    private static final double [] expTable = new double[500];
	    */
	    
	    public  double reward1(int x) {
	    	if(x >= 2000)
	    		return 1;
	    	
	    	if(x<=-2000)
	    		return -1;
	    
	  return  (2 / (1 + Math.exp(-0.5 * 0.005 * x)) - 1);
	    
	    }
	
    public final void assignValues(Move list, Board bord, long[] history, long[] total, double depth) {

          
            byte[][]space = bord.space;       
            
            if(total==null || !isHistory){
            	 for (Move m = list; m != null; m = m.next) {
                     if (space[m.toRow][m.toCol] != Board.EMPTY) {
                      
                     	m.score =  (int)   history[m.index]>>2;
                                        
                     } else {  
                         m.score =(int)( -Integer.MAX_VALUE / 2
                                 + history[m.index]>>2);
                                                
                     } 
                 }
            }
            else            	
        
                for (Move m = list; m != null; m = m.next) {
                    if (space[m.toRow][m.toCol] != Board.EMPTY) {
                     
                    	m.score =    (int) (10000 * ((0.5 * history[m.index]) / total[m.index]));
                            

          
                    } else {  
                        m.score = -Integer.MAX_VALUE / 2
                                + (int) (10000
                                        * ((0.5 * history[m.index])
                                                / total[m.index]));
                    } 
                }
            
   
    }
	


	public boolean hasDatabase(Board b) {

		return (b.B_Counter+b.W_Counter<=9);
	}




}
