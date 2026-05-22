import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Random;
import java.util.StringTokenizer;


/**
 *Class containing Static Board Evaluator for a particular game.
 */
public abstract class Game  {
	
	public enum Mode{
		
		   AB("\u03b1\u03b2 Search"), NegaMax("NegaMax Search"), DP("PN-\u03b1\u03b2 Player"), PNS("PN Search"), 
		   PNS2("PN\u00B2 Search"), DFPN("df-pn Search"), PDS, COMB("Comb-\u03b1\u03b2"), 
		    PAMC("Parallel MCTS"), PA("Parallel \u03b1\u03b2"), PAPN("Par PN\u00B2 Search"),
		     SHOT("MC-SHOT"), SHOT2("MC-SHOT2");
		
		  private String alias;
		
          
		  private Mode(String s){
			  alias =s;			  
		  }
		  		  
		  private Mode(){
			  alias =name();  
		  }
		  
		  public String toString(){
			  return alias;
		  }		
	} 
    
    private static int initialTTSize = 16;
    public static boolean useBook = false;
    private static int NO_CORES; 

    public boolean isQuiescence  = true;
    public boolean isNullmove = true;
    public boolean isTT = true;
    public boolean isWonTable = false;
    public boolean isMultiCut = true;
    public boolean isRPS = false;
    public boolean isHistory = true;
    public boolean isAllCut = true;
    public boolean isFutility = false;
    public boolean isLMR  = false;
    public boolean isLeafThreat = false;
    public boolean isEGDB = false;
    public boolean isMemEGDB = false;
    public boolean donThink = false;
    public boolean isUCT = true;
    public boolean isFractional = false;
    public boolean incMoveGeneration =false;
    public boolean isLazy =false;
    public boolean isFastMC = false;
    public boolean isPB = false;
 
    public boolean isPUCT = false;
    public boolean isNN = false;
   
 
    public boolean isRemember = true;
    public boolean randomError = false;
    public boolean isABPlayout = false;
    public boolean isGreedyPlayout = false;
    public boolean isCutoff  = true;
    public boolean isKnowledgePN = false;
    public boolean isCombinatorialKnowledge = false; //Use combinatorial knowledge
    public boolean simplifyBoard = false; //Simplify the board using combinatorial knowledge
    public boolean isTwoPlyCheck = false;
    public boolean isST = false;
    public boolean iMM = false;
    public boolean isHybrid = false;
    public boolean isMMHybrid = false;
    public boolean isSolver =true;
    public int playoutLength = Integer.MAX_VALUE;
    
    public boolean isUCTClassic =false;
    public boolean isHalving =false;
    
    public boolean uncertainMCTS = true;
    
    public double[] nSimulations = new double[0];
    
    public int PBW = 10;
    public double pvC =0.6;
	public double rC = 0.4;
	public double cutC = rC;
	public double allC = rC;
	public double rootC = 0.8;
	public int budget = 20;
    
    public int Cores =  NO_CORES;
    
    public Mode playerMode = Mode.AB;
    public int neededMoves =20;
    public int minNoMoves =10;
    
	public  double deltaRPS = 1000;
	public  int moveRPS = -1;
	public  double thresholdRPS = 1000;
    public double alpha = 0.2;
    public int futMargin =1000;
    
    public String bookName;
   
    public int sizeTT = getInitialTTSize();
      
    public static boolean isRandom = false;
   
    protected Evaluator evaluator;
    private Evaluator evaluatorOP;
   
    private static Random rand = new Random();
   
    protected Generator generator;
	public int contemptFactor =-150;
   
    private static long seed;
    protected int bound =1000;
      
    private final static long MULTIPLIER = 0x5DEECE66DL;
    private final static long APPEND = 0xBL;
    private final static long MASK = (1L << 48) - 1;
   
    /**
     Return an integer score indicating which side is winning in the board,
     b.
     */   
    

    
       
      public int evaluate(Board b, int lastturn){
      	
          return eval(b,evaluator,lastturn);
      }
      
      public int evaluateOM(Board b, int lastturn){
      	
          return eval(b,evaluatorOP,lastturn);
      }
      
      
      

      
      private int eval(Board b, Evaluator eval, int lastturn){
      	
          int winner = gameTheoreticEvaluation(b, lastturn);
      
          if (winner != 0) {
              return winner;
          }
           
          winner = eval.evaluate(b, lastturn);
          
          return (winner==Board.DRAW_SCORE)?winner+lastturn:winner; 
      }
    

    public final int heuristicEvaluation(Board b, int lastturn) {
        return  evaluator.evaluate(b, lastturn);
    }  
    
    public final int heuristicEvaluation(Board b, int pass_thresh, int prune_thresh, int lastturn) {
        return    evaluator.evaluate(b, pass_thresh, prune_thresh,  lastturn);
    }  
        
    public abstract int gameTheoreticEvaluation(Board b, int lastturn); 
      
    public abstract int terminalEvaluation(Board b, int lastturn);
    
    public abstract String[][] getEvalNames();
    
    public abstract int getSize();
    
    public abstract void setSize(int size);
    
    public void startingConfiguration(Board b){
    	
    	int row, col;
    	
    	byte [][]space = b.space;
    
        // Set the board to empty.
        for (row = space.length; --row>=0;) {
            for (col = space[0].length;--col>=0;) {
                space[row][col] = Board.EMPTY;
            }
        }
        
       b.sideToMove = Board.B_SIDE;
    }
    
    public int getMaxLength(){
    	return 400;
    }
    
    public int checkOnDraw(Board b) {
    	
    	return 0;	
    }
  
    public Generator getGenerator() {
        return generator;
    }
   
    public Evaluator getEvaluator() {
        return evaluator;
    }
    
 
    
    public boolean hasDatabase(Board b){
    	
    	return false;
    }
    
	public int heuristicDTC(Board b, int side){
		
		if(side==Board.B_SIDE)			
			return  9*b.W_Counter;
		
		return  9*b.B_Counter;
	}
   
    public void setEvaluator(Evaluator e) {
        evaluator = e;
    }
    

    
    public void simplifyBoard(Board b){
    	
    }
    
    public Board getNewSimplifiedBoard(Board b)
    {    	
    	return b;
    }
    
   public void assignValues(Move list, Board bord, long[] history, long[] total, double depth) {
   	
   	if(isHistory && total!=null)
         for (Move m = list; m != null; m = m.next)
                m.score =  (int) (10000 * (((double) history[m.index])
                                                    / total[m.index]));
    else
      for (Move m = list; m != null; m = m.next)
                m.score = -Integer.MAX_VALUE  +  (int) history[m.index];
                                           
    }
   
    public static void randomize() {
  
        rand = new Random();
        seed = System.currentTimeMillis();
    }
  
    public final static int randomValue() {
        if (isRandom) {
            return (int) (200 * rand.nextDouble() - 100);
        }
      
        return 0;
    }
    
    public int checkOnRepetition(Board b){
    	
    	final CompressedBoard[] history = b.history;
    	int counter = b.getNumberOfMovesPlayed();
  
        if (history[counter].conversion) {
            return 0;
        }
      
       int numOfOccurence = 0;
      // --counter;
      // final long posB = b.posB;
      // final long posR = b.posW;
       final long hashValue = b.hashValue;
   
        while (--counter > 0) {
        	
            if (history[counter].conversion) {
                return numOfOccurence;
            }
       
             //next one
            if (history[--counter].hashValue == hashValue && history[counter].posB == b.posB && history[counter].posW == b.posW) {
                if (numOfOccurence > 0) {
                    return Board.REPETITION;
                } else {
                    numOfOccurence++;
                }
            } 
  
            if (history[counter].conversion) {
                return numOfOccurence;
            }  
       
            //counter--;   
        }
 
        return numOfOccurence;    	
    }
    
    public boolean extendSearch(Board bord){
    	
      return false;
    	
    }
    
    
    public Move[] getMoveArray(String filesave) throws IOException {
    	
         final ArrayList<Move> list = new ArrayList<>();        

        try (
            BufferedReader in = new BufferedReader(new FileReader(filesave));){
            in.readLine();
            String hulp = in.readLine();
                  
            while (hulp != null) {
            	//counter++;           	
            
                StringTokenizer st = new StringTokenizer(hulp);
                
                if(st.countTokens()<=1)
                	break;
                
                st.nextToken();
                Move m = generator.generateMove();
                
                String s = st.nextToken();
                m.setMove(s);
                list.add(m);
                
                if(st.hasMoreTokens()){
                
                	m = generator.generateMove();
                    m.setMove(st.nextToken());
                    list.add(m);
                	
                    hulp= in.readLine();
                }
                else break;
                
            }
        }
        
        return list.toArray(new Move[list.size()]);
    } 
    
    public Combinatorial generateCombinatorial(){
    	
    	return null;
    }
     
    
 
    public final static int randomMCIntValue() {   
        
        final long nextseed = (seed * MULTIPLIER + APPEND) & MASK;

        seed = nextseed;
        return (int) (nextseed&Integer.MAX_VALUE);
        }  
    
    public final static double randomMCValue() {   
        
       final long nextseed = (seed * MULTIPLIER + APPEND) & MASK;

        seed = nextseed;
        return (nextseed&Integer.MAX_VALUE)/(double)Integer.MAX_VALUE;
        }     
    
   
    public final String getGameName(){
    	    	
    	return this.getClass().getName().replaceAll("Game","");
    }
    
    public static void setInitialTTSize(int initTTSize) {
		initialTTSize = initTTSize;
	}
    
    public boolean fastMCImpl(){
    	
    	return isFastMC;
    }
    
    public static void setCores(int cores) {
		Game.NO_CORES = cores;
	}
    
    public static int getCores(){
    	
    	return NO_CORES;
    }

    public int monteCarlo(byte [][] space , byte turn){return 0;}
    
    public int monteCarloPlus(Board b){return monteCarlo(b);}
    
    public int monteCarlo(Board b){return 0;}
    
    public Move combinatorialMove(Board b){return null;}//Added by Jeroen
    
	public static int getInitialTTSize() {
		return initialTTSize;
	}
	
	public final int boundScore(){
		
		return bound;
	}
	
	public final void setBound(int b){
		
		bound = b;
	}
	
	

	
	 public double reward(int x) {
     	return x;
     
     }
	
	
	public Evaluator getOPEvaluator() {
		return evaluatorOP;
	}

	public void setOPEvaluator(Evaluator evalOP) {
		try {
			evaluatorOP = evalOP.getClass().getDeclaredConstructor().newInstance();
		} catch (IllegalAccessException  | InstantiationException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException e) {
			e.printStackTrace();
		}
	}


	static {
        randomize();
        NO_CORES= Math.max(1, Runtime.getRuntime().availableProcessors());
    }
	   
}
