
public abstract class AbstractPlayer {

	protected static final int PV_NODE = 0;
	protected static final int ALL_NODE = 1;
	protected static final int CUT_NODE = -1;
	protected static long maxNodes = 5000000000L; // Long.MAX_VALUE;
	public static int GAMES_LIMIT = 100;
	public static int BUDGET = 50000;
	protected long targetTime = Board.TIME_LIMIT;
	
    public boolean checkSymmetry = false;
    public long countNodes;
    
    protected Board bord;
    protected int beginSide;
    // Move generator for this game.
    protected Generator generator;

    // Static Board Evaluator for this game.
    protected Game gameAI;
  
    public WinTable winTable;
    
    protected int nmoves;
    
    protected EndGameDatabaseFile egdb=null;
    
    protected Move[] pv = new  Move[2];
    
    protected OpeningBookBigEntry [] toekomst = new OpeningBookBigEntry[2];
   
    protected volatile Board copy_board;   
   
     // Flag that gets set to true when computer uses up all of its time
    // to make a move.
    protected volatile boolean timeoutFlag;
    
        // Time at which the computer started choosing its move.
    protected long searchStartTime;
    
    public boolean isMoveEvaluation = true;
    protected volatile boolean neverEnd = false;
    protected long  decreaseValue =0;
    protected int egdbCounter =0;

    /**
     * Optional perfect-play endgame oracle (set from outside before calling search).
     * When non-null, every node whose total piece count is ≤ customTablebaseMaxPieces
     * is queried first; if the oracle returns a result the subtree is pruned.
     */
    public static volatile TablebaseOracle customTablebase       = null;
    public static volatile int             customTablebaseMaxPieces = 0;

    public long duration;
    public boolean timeCheck = false;
    
    
	    
	 public abstract Move search(Board b);
	 
	 protected void setTargetTime(int ply) {
		  
	        nmoves = (gameAI.neededMoves) - ply / 2;
	        
	        if (nmoves < gameAI.minNoMoves) {
	            nmoves = gameAI.minNoMoves;
	        }
	        
	        targetTime = ((Board.tournamentTime) / nmoves) -  decreaseValue;
	    }
	 
	 public void setFixedTime(long time){
		 
		 this.targetTime = time ;
	 }
	  
	    protected void doSomeThing() {
	        if (neverEnd) {    
	          
	            Log.print("I am on hold");
	    
	            long hulp = System.currentTimeMillis();
	    
	            while (neverEnd) {  
	              if (System.currentTimeMillis() - hulp >= 5000) {
	                    Log.print(".");
	                    hulp = System.currentTimeMillis();
	                }
	    
	               
	              try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
	            	
	            	
	            	
	            	
	            }
	           Log.println("");
	        }
	    }
	    
	    public final boolean isPondering(Board b) {
	        
	    	 setForEver(false);

	        if (copy_board == null) {
	            Log.println("quit, no board stored");
	        	quitSearch();
	            return false;
	        }
	   
	        if (copy_board.equals(b)) {
	            Log.println("Ponder Move: continue searching");
	            return true;
	        }
	     
	        Log.println("quit");
	        quitSearch();
	        return false;
	    }
	   
	    public final boolean opponentsTime(Board b, int turn) {
	   
	        for (int i = 0; i < toekomst.length; i++) {
	                     
	            if (toekomst[i]!= null && toekomst[i].equals(b)) {
	                Move m = pv[i];
	          
	                if (generator.validMove(b, m)) {
	                    m.apply(b);
	                    Log.println("Thinking move is: " + m);
	                    if (Math.abs(gameAI.terminalEvaluation(b, turn))
	                            >= Board.WIN_SCORE) {
	                        m.undo(b);           
	                        return false;
	                    }
	                    setForEver(true);
	                    return true;
	                }
	            }
	        }
	        return false;
	    }
	    

	   
	    protected final void recycle(Move moveList, Move bestMove) { 
	 
	        Move move = moveList;
	       
	        Move next;   

	        while (move != null) {
	            next = move.next;
	            if (bestMove != move) {
	                generator.recycleMove(move);
	            }
	      
	            move = next;
	        }
	    }
	    
	   
	    public void  quitSearch() {
	        neverEnd = false;
	        timeoutFlag = true;
	    }	

	    public void  setForEver(boolean b) {
	        neverEnd = b;
	    }
	    
	    public final void search(Board b, int side) {
	    	
	    	search(b);
	    }

		
		public static void setMaxNodes(long max) {
			maxNodes = max;
		}
		
	    protected final Move getBestMove(Move list) {

	        if (list.next == null) {
	            return list;
	        }

	        Move max = list;
	        Move previous = list, old = null;

	        for (Move m = list.next; m != null; m = m.next) {
	            if (max.score < m.score) {
	                max = m;
	                old = previous;
	            }
	            previous = m;
	        }
	   
	        if (old != null) { 
	            old.next = max.next;
	            max.next = list;
	        }

	        return max;
	    }

	    protected final Move sort(Move list) {
	        if (list == null) {
	            return null;
	        }

	        list = getBestMove(list);
	        Move old = list;
	  
	        for (Move m = list.next; m != null; m = m.next) {
	            m = getBestMove(m);
	            old.next = m;
	            old = m;
	        }  
	        return list;
	    }
	    
	protected void initialiseDatabase(){
		
		if(gameAI.isEGDB && egdb ==null){
			  egdb =  EndGameDatabaseFile.createEndgameDatabase(gameAI);		
	       }else if(!gameAI.isEGDB)
			   egdb = null;
	}	

}
