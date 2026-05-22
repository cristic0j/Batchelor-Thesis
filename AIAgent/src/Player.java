import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;


public class Player extends AbstractPlayer { 

	private static final int ALLFUTMOVES = 5;
    private static final int CUTFUTMOVES = 15;
	
    private static int kBestMode = 0;
  
    protected static final int TERMINAL_SCORE = Board.WIN_SCORE + 100; 
    private static final double OVERHEAD_FACTOR = 1.5;
    private static final double PANIC_FACTOR = 2.5;
  
   
    private static final int MAX_LENGTH = 40;
     
    private static boolean isSearchExperiments = false;
    private static String logname;
    private static int atLeastDepth = 2; 
    //private static final long[][] NullMoveStatistic = new long[13][2];
    
    private static final int QUIESENCE_DEPTH = 10;
   
    // Counter used to check some things infrequently during search (elapsed
    // time for example).
    private long lazyCounter;

    // Search node counter to measure pruning performance.
    protected long searchCounter;
        
    private long searchCounterFQ, counterTTHITS, realCounterTTHITS;
  
    private final Move[][]KillerMove = new Move[MAX_LENGTH][2];
   
    private Move currentBestMove = new Move();
  
    private final Move[]pvMoves = new Move[KillerMove.length + 1];
    private final Move[][]KillerMove_QS = new Move[KillerMove.length + QUIESENCE_DEPTH][2];
     
    private final long[]HistoryR = new long[(1 << Move.BIT_RANGE)];
    private final long[]HistoryB = new long[HistoryR.length];
    
    private final long[]totalB = new long[HistoryR.length];
    private final long[]totalR = new long[HistoryR.length];
    
    private final long[]RootBigTable = new long[HistoryR.length];
      
    private final long[]MarkedMoves = new long[HistoryR.length];
   
    private TranspositionTable TransTable; 
  
    private int maxDepth;
      
    private Move choice;
  
    private long panicTime;    
     
    protected final Move dummyMove = new Move();
    
    private int momentum = 16;
   
    private long[] effective = new long[2];  

    public boolean advTimeCtrl = true;
    public boolean adaptive = true;
   
    private boolean  isNullMove = false;
    public boolean bookGeneration = false;
    
    private boolean isMultiCut;
    private boolean isALLCUT;
    private boolean isTrans;
    private boolean isQuiescence;
    private final boolean isPVS = true;
    private boolean panicNotice = false;    
    private boolean isLMR = false;
    private boolean isRPS;
   // private boolean LogHistory = false;

    private boolean isFutility  = false;

    
    private int drawValue = 0;
  
    //private long falseCutCounter, multiCutCounter; 
  
    private int allCutC = 2, allCutM = 10, allCutDelta = 525;
    private final static int ALL_R = 2;
    //private static final int ASPIRATION_WIDTH = 100;
    private final static int LMR_R =2; 

    private double[] RP_Table;
    private static final double minFP = 0.5;
    private double rpWeight = 0.25;
    
    private boolean isEGDB = false;
  
  
	private Move rootMoves;

	protected long totalNodes;
	private boolean isERPSONE;
	private boolean isERPSTWO;
	
	private int databasePly;
	
	private boolean isLeafThreat = true;
	private int threatCounter = 0;
    
    public Player(Game eval, TranspositionTable table) {
                
        gameAI = eval;
        generator = eval.getGenerator();
     
        if (gameAI.isTT) {
        	if(table != null && table.getKey() == gameAI.sizeTT){
        		
        	 TransTable = table;
        	 table.clear();
        	 
           }
        	else
            TransTable = new TranspositionTable(gameAI.sizeTT);
        } else {
            TransTable = new TranspositionTable(0);
        }
            
        
        for (int i = KillerMove.length; --i >= 0;) {
            KillerMove[i][0] = new Move();
            KillerMove[i][1] = new Move();
        }
    
        for (int i = KillerMove_QS.length; --i >= 0;) {
            KillerMove_QS[i][0] = new Move();  
            KillerMove_QS[i][1] = new Move(); 
        }  
       
        neverEnd = false;  
     
        Game.randomize();
   
        double [] hulp= generator.getLOGRPPROB();
                
        if (hulp != null)
            setRPWeights(hulp);
        
        
 	  initialiseDatabase();

    }
   
    public Player(Game eval) { 
    
    this(eval, null);
    }      
    
    protected Player() {
		
	}

	public TranspositionTable getTransTable(){
    	
    return 	TransTable;
     }
 
    public static void setSearchExperiments(boolean set, String fileName) { 
        isSearchExperiments = set;
        logname = fileName;
    }
     
    public static void setMinimalDepth(int depth) {
        atLeastDepth = depth;
    }
   
    public void setANC(int c, int m, int delta) {  
        allCutC = c; 
        allCutM = m;
        allCutDelta = delta;       
    }
 
    private void setRPWeights(double[] table) {
    	RP_Table = new double[table.length];
    	
         for (int i = RP_Table.length; --i >= 0;) {
            RP_Table[i] = conToFP(table[i]);
        }  
        
      // System.out.println( java.util.Arrays.toString(RP_Table));
      } 
 
    private double conToFP(double rp) {
    	
        final double hulp = ((int) (100 * rp / Math.log(rpWeight))) / 100.0;

        if (hulp > 4) {
            return 4;
        } else if (hulp < minFP) {
            return minFP;
        }
 	
        return hulp;   
    }

    
    public void setMomentum(int mom) {
        momentum = mom;
    }
   
    //Not beneficial for small number of moves
  /*  private final Move quickSort(final Move lijst) {

        if (lijst == null || lijst.next == null) {
            return lijst;
        }

        // quicksort
        Move smaller = null;
        Move temp;
        final Move p = lijst;
 
        Move list = lijst.next;
        p.next = null;
        Move bigger = p;

        while (list != null) {
            if (p.score < list.score) {
                temp = list;
                list = list.next;
                temp.next = bigger;
                bigger = temp;
            } else {
                temp = list;
                list = list.next;
                temp.next = smaller;
                smaller = temp;
            }
        }

        smaller = quickSort(smaller);
        bigger = quickSort(bigger);
        p.next = smaller;
     
        return bigger;
    }*/
 
    
 
    private void setTransKill(Move list, Move TransMove, int pathlength) {
    	
    	final Move Killer1 = KillerMove[pathlength][0];    	
    	final Move Killer2 = KillerMove[pathlength][1];
    	

        for (Move m = list; m != null; m = m.next) {
            if (m.equals(TransMove)) {
                m.score = Integer.MAX_VALUE;
                continue;
            }
         
            if (m.equals(Killer1)) {
                m.score = Integer.MAX_VALUE - 1;
                continue;
            }
         
            if (m.equals(Killer2)) {
                m.score = Integer.MAX_VALUE - 2;
                continue;
            }    
        }    
    }
 
    private Move moveOrdening(Move list, long[] history, long[] total, double depth) {  
        gameAI.assignValues(list, bord, history,total, depth); 
        return sort(list);  
    }

    private void saveResults(long[][] result, int depth) {
    	
    	int[] aantal = new int[MAX_LENGTH];
	 
        try(DataInputStream in = new DataInputStream(new FileInputStream(logname));){        
            for (int i = 0; i < MAX_LENGTH; i++) {
                aantal[i] = 0; 
              
                  if (i < depth
                            && !(result[i][0] == 0 && result[i][1] == 0
                            && result[i][2] == 0)) {
                        aantal[i] = 1;
                        aantal[i] = 1 + in.readInt();
                    } else {   
                        aantal[i] = in.readInt();
                    } 
               
                    result[i][0] += in.readLong();
                    result[i][1] += in.readLong();
                    result[i][2] += in.readLong();
              
            }
           } catch (IOException e) {
        	           e.printStackTrace(); Log.println("Cannot Log: " + e.getClass().getName()); System.exit(0);
             } 
            
        try(
            PrintWriter netout = new PrintWriter(new BufferedWriter(new FileWriter(logname + ".txt")));
            DataOutputStream out = new DataOutputStream(new FileOutputStream(logname));){
        
             netout.println("n Time Nodes TTHits");
      
            for (int i = 0; i < MAX_LENGTH; i++) {  
                out.writeInt(aantal[i]); 
                netout.print(aantal[i]); 
         
                out.writeLong(result[i][0]); 
                netout.print(" " + result[i][0]);
           
                out.writeLong(result[i][1]); 
                netout.print(" " + result[i][1]);
           
                out.writeLong(result[i][2]); 
                netout.println(" " + result[i][2] + "\r");
            }
        } catch (IOException e) {
           e.printStackTrace(); Log.println("Cannot Log: " + e.getClass().getName()); System.exit(0);
        }

    }
 
    // /////////////////////////////////////////////////////////////////////////
    // Move selection code
    // /////////////////////////////////////////////////////////////////////////

    /**
     Choose a move for the computer and update the board with that move. Iterative deepening alpha-beta search.
     Minimax values are always from the perspective of the Black Player (Max).
     */        
    	public Move search(Board b) {
    	
    	 return search(b,null);
    	}
    	
    	public Move search(Board b, Move list) {
    		    	   	
        rootMoves = list;
    	// Get a snapshot of the current time.
        searchStartTime = System.currentTimeMillis();
    	
    	copy_board = (copy_board==null)?b.duplicate():b.duplicate(copy_board);
        bord = b;
        final int side = b.sideToMove;
        gameAI.simplifyBoard(bord);
        beginSide = side;

        int ply = bord.getNumberOfMovesPlayed();
        
        databasePly =2;

        drawValue = side * gameAI.contemptFactor;
    
        duration = 0;
        long hulptime = 0;
        long oudhulptime = 1;
        long last_period = 0;
        totalNodes = 0;
	
        lazyCounter = 0;
        egdbCounter =0;
   
        Evaluator.parametrise(false);
        
        isFutility = gameAI.isFutility;
        
        isLMR = gameAI.isLMR;                    
        
        isRPS = (this.RP_Table == null) ? false  : gameAI.isRPS;
        isMultiCut = gameAI.isMultiCut;
        isALLCUT = gameAI.isAllCut;
        isTrans = gameAI.isTT;
        isQuiescence = gameAI.isQuiescence;
        isNullMove = gameAI.isNullmove;
        isEGDB = gameAI.isEGDB;        
        isERPSONE =  isRPS;//isLMR && isRPS;
        isERPSTWO =  isRPS; // isLMR && isRPS;
        isLeafThreat = gameAI.isLeafThreat;
    
        // safety
        dummyMove.score = 0;       
        
        initialiseDatabase();
      // Move the computer is going to make, In earlier implementations a trivially-chosen move was determined as a starting point.	
        choice = null; // selectMove( 1, side , side * WIN_SCORE, -side * WIN_SCORE);
        Move m, new_move;
        long[][] results = new long[MAX_LENGTH][3];
    
        effective = new long[2];
    
        for (int i = KillerMove.length; --i >= 0;) {
            KillerMove[i][0].setMove(0, 0, 0, 0);
            KillerMove[i][1].setMove(0, 0, 0, 0);
        }
     
        for (int i = pvMoves.length; --i >= 0;) {
            pvMoves[i] = null;
        } 
  
        for (int i = KillerMove_QS.length; --i >= 0;) {
            KillerMove_QS[i][0].setMove(0, 0, 0, 0);  
            KillerMove_QS[i][1].setMove(0, 0, 0, 0);
        } 
       
        // Have not yet run out of time.
        timeoutFlag = false;
        maxDepth = 0;
        counterTTHITS = 0;
        realCounterTTHITS = 0;
    
        for (int i = MarkedMoves.length; --i >= 0;) {
            MarkedMoves[i] = -1;
        }
   
        if (Board.isTournament) {
            setTargetTime(ply);
        }
   
        countNodes = 0;
        panicTime = (long) (PANIC_FACTOR * targetTime);
        Move storedMove = null;
     
        for (int i = 0; i < toekomst.length; i++) {         
            if (toekomst[i]!=null && toekomst[i].equals(bord) && generator.validMove(bord, pv[i])) {
                storedMove = pv[i];
            }        
        }
      
        TransTable.mark(); 
              
     
        TableEntry element;
        // update history
        for (int i = HistoryR.length; --i >= 0;) {
            HistoryR[i] /= momentum;
            HistoryB[i] /= momentum;
            totalB[i] = Math.max(totalB[i] / momentum, 1);
            totalR[i] = Math.max(totalR[i] / momentum, 1);
            RootBigTable[i] /= momentum;
        }
  
        Move  savedRootMoves = rootMoves;     
        
        if(gameAI.donThink) this.targetTime=0;
         
        do {
            maxDepth++;
            panicNotice = true;
      
            // Reset the search counter.
            searchCounter = 0;
            searchCounterFQ = 0; 
            
            // update history
            if (storedMove != null && storedMove.score == maxDepth) { 
                m = storedMove;
                for (int i = 0; i < pvMoves.length && m != null; i++, m = m.next) {
                    pvMoves[i] = m;
                }
            }
         
            currentBestMove = new Move();
            if (choice != null) {
                currentBestMove.score = choice.score;
            }    
            
            if(savedRootMoves!=null){
            	rootMoves = null;
            	for(Move mo =  savedRootMoves; mo.next!=null; mo = mo.next){
            		Move zetje =generator.generateMove(mo.fromRow, mo.fromCol, mo.toRow, mo.toCol);
            		zetje.next = rootMoves;
            		rootMoves = zetje;
            	}
            }
      
           // aspiration windows, not used
           /* if (false && choice != null) {
                int prune_thresh = choice.score + side * ASPIRATION_WIDTH;
                int pass_thresh = choice.score - side * ASPIRATION_WIDTH;

                Log.println(
                        "(alpha,beta)= " + pass_thresh + "," + prune_thresh
                       );
         
                new_move = selectRootMove(maxDepth, side, prune_thresh,
                        pass_thresh,  0, PV_NODE, false);
         
                if (new_move.score * side <= side * pass_thresh
                        || new_move.score * side >= prune_thresh * side) {
                    Log.println("research: " + new_move.score);
                    currentBestMove = new Move();
                    currentBestMove.score = new_move.score;
                    new_move = selectRootMove(maxDepth, side,
                            side * (TERMINAL_SCORE - maxDepth),
                            -side * (TERMINAL_SCORE - maxDepth + 1), 0,
                            PV_NODE, false);  
                }  
            } else {*/
                int prune_thresh = side * (TERMINAL_SCORE - maxDepth);
                int pass_thresh = -side * (TERMINAL_SCORE - maxDepth + 1); 

                new_move = selectRootMove(maxDepth, side, prune_thresh,
                        pass_thresh,  0, PV_NODE, false);
            //}
       
            if (timeoutFlag && new_move.defined() && !new_move.equals(choice)) {
                Log.println(
                        "Better move with partial analysis: " + new_move + " "
                        + new_move.score);
            }
                     
            hulptime = System.currentTimeMillis() - searchStartTime;
            totalNodes += searchCounter + searchCounterFQ;
      
            // Report the chosen move as the computer generates it.
            if (new_move.defined() && (!timeoutFlag || (!new_move.equals(choice)))) {
        
                if (isSearchExperiments) {
                    results[maxDepth - 1][0] = hulptime;
                    results[maxDepth - 1][1] = totalNodes;
                    results[maxDepth - 1][2] = /* multiCutCounter;*/counterTTHITS;
                }
	
                Log.print(
                        "Nodes: " + searchCounter + " NodesFQ: "
                        + searchCounterFQ + " Iteration Nodes: "
                        + (searchCounter + searchCounterFQ) + " Total nodes: "
                        + totalNodes + "\r\n" + "Time "
                        + new DecimalFormat("#.##").format(hulptime / 1000f) + " nps: "
                        + (int) (1000.0 * totalNodes / hulptime) + " TTH "
                        + counterTTHITS + "  MDPs: " + (realCounterTTHITS)  + "\r\n\r\n");  
		  		  
                choice = new_move;
            }
          
            // Only take the move if we did not run out of time while selecting it.
            // Otherwise, we don't really know how good it is.
            if (!timeoutFlag) {
                choice = new_move;
            }        
               
            // forced win         
            if (choice != null && (side * choice.score >= Board.WIN_SCORE)
                    && !isSearchExperiments) {
            	if(egdb !=null  && maxDepth <20){
            		databasePly++;
            		TransTable.mark(); 
            	}
            	else
                timeoutFlag = true;
            }
          
            // /loss   
            if (side * choice.score <= -Board.WIN_SCORE) {
                if (!isRPS && !isEGDB) {
                    timeoutFlag = true;
                } else {
                    TransTable.mark();     
                    maxDepth = Math.max(maxDepth - 1, 1);
                    isRPS = false;
                    isEGDB = false;
                    //egdb =null;
                    Log.println("Find the best line of defence!");
                    if (Board.isTournament) {
                        targetTime = panicTime;
                    }
                }
            }
               
            if (!neverEnd && !timeoutFlag && Board.isTournament
                    && side * choice.score > -Board.WIN_SCORE && maxDepth>=atLeastDepth) {
                Move move_list = generator.generateMoves(bord, side);
                int defence_counter = 0;
         
                for (m = move_list; m != null && defence_counter < 2; m = m.next) {
                    element = TransTable.retrieve(m.hash(bord));
                    if (element.getHeight() >= 0 && !element.isMarked()
                            && element.getFlag() != TranspositionTable.UBOUND) {
                        if (side * element.getScore() <= -Board.WIN_SCORE) {
                            continue;
                        }
                    }  
         		  
                    defence_counter++;            
                }     
     
                // play the last line of defence. Only used in tournament mode.  
                if (defence_counter == 1) {
                    Log.println("We are playing the last line of defence!");
                    timeoutFlag = true;
                }else   if (move_list.next == null ) {
                    Log.println("Only move!");
                    timeoutFlag = true;
                } 
                	
            }else if(maxDepth>=atLeastDepth || bookGeneration){
                Move move_list = generator.generateMoves(bord, side);
              
                if (move_list.next == null) {
                    Log.println("Only move!");
                    timeoutFlag = true;
                } 
            }
            	
            m = new_move;
       
            for (int i = 0; i < maxDepth && m != null; i++, m = m.next) {
                pvMoves[i] = m;
            }
       
            for (int i = KillerMove_QS.length; --i >= 0;) {
                KillerMove_QS[i][0].setMove(0, 0, 0, 0);
                KillerMove_QS[i][1].setMove(0, 0, 0, 0);
            }

            long current_period = hulptime - oudhulptime;
        
            if (!neverEnd && maxDepth >= atLeastDepth) {
                if (hulptime >= targetTime) {
                    Log.println("quit overtime");
                    timeoutFlag = true;
                } else if (last_period > 100) {
                    double rate = ((double) current_period - last_period)
                            / last_period;
                    double estimate_time = hulptime
                            + (1 + rate) * current_period;

                    Log.println(
                            "Target Time:" + (targetTime / 1000)
                            + " Estimated time " + (int) (estimate_time / 1000)
                            + "s");
                    if ((estimate_time >= 1.3 * targetTime)
                            || (Board.isTournament
                                    && (Board.tournamentTime - estimate_time)
                                            / (nmoves - 1)
                                                    < 10000)) {
                        if (hulptime > (targetTime / 3)) {
                            Log.println("quit");
                            timeoutFlag = true;
                        }
                    }
                }    
            }
   
            last_period = current_period;
            oudhulptime = hulptime;
        }while (!timeoutFlag && maxDepth < MAX_LENGTH - 1);
   
        doSomeThing();  
               
        if (choice == null) {
            Log.println("Choice is null!");
        }
               
        bord = copy_board.duplicate(bord);
    
        if (choice != null && choice.next != null && generator.validMove(bord, choice)) {
            pv[0] = choice.next;
            pv[0].score = maxDepth - 1;
 
            choice.apply(bord);
    
            toekomst[0]  = new OpeningBookBigEntry(bord);
    
            if (choice.next != null && pv[0].next != null) {
                pv[1] = pv[0].next;
                pv[1].score = maxDepth - 2;
                choice.next.apply(bord);
    
                toekomst[1] = new OpeningBookBigEntry(bord);
    
                choice.next.undo(bord);
            }
    
            choice.undo(bord);
        }
     
        if (isSearchExperiments) { 
            saveResults(results, maxDepth);
        }
       
        duration = System.currentTimeMillis() - searchStartTime;
      
        Log.println("\r\n" + "Time:" + duration);
        
        if(isEGDB)
        	Log.println("EGDB Hits " + egdbCounter);
        
        if(isLeafThreat)
        	Log.println("Leaf Hits " +this.threatCounter);
        
        if (effective[0] + effective[1] != 0) {
            Log.println(
                    "Percentage="
                            + (100 * effective[0]
                            / (effective[0] + effective[1]))
                            );
        }
        
        if (Board.isTournament) 
        	Log.saveScreen();
				   	 
        Evaluator.parametrise(true); 
        
	  if(egdb !=null && !gameAI.isMemEGDB){    	
		       Log.println("Close database");
				egdb.closeDatabase();
				egdb = null;
    	}
            
       return choice; 
    }  

    private void printPV(Move move_list) {
                
        int counter = bord.getNumberOfMovesPlayed() +1;
        
        StringBuilder hulp = ((counter & 1) ==1)?new StringBuilder(" " + (counter+1)/2 + " " +move_list)
        					:new StringBuilder(" " + (counter/2) + "... " +move_list);
        counter++;      
        for (Move m = move_list.next; m != null && m.defined(); m = m.next, counter++) {                       
            if((counter&1)==1)
               	hulp.append(" " + (counter+1)/2 + " " + m);	
            else
            	hulp.append(" " + m);
        }
   
        Log.print("Iteration: " + maxDepth + " " +  move_list.score + hulp + "\r\n");
    }

    private int reduceSearch(double depth, int side) {
  
        if (side == Board.B_SIDE) {
            if (depth > 8 || (depth > 6 && bord.B_Counter >= 5)) {
                return 3;
            }
        } else {
            if (depth > 8 || (depth > 6 && bord.W_Counter >= 5)) {
                return 3;
            }
        }
        return 2;
    }
 
    protected final boolean timeToQuit() {

        if (((searchCounter & 4095L) == 0L) && !neverEnd
                && (maxDepth > atLeastDepth)) {
            long time = System.currentTimeMillis() - searchStartTime;
   
            if(time < targetTime)
              return false;
            //normal
            if (!Board.isTournament){         
         		timeoutFlag = true;
                return true;
            }
           else
             if(time >= OVERHEAD_FACTOR * targetTime){
             	
               if(advTimeCtrl){
                if (currentBestMove.score * beginSide <= -Board.WIN_SCORE){
                    if (panicNotice) {
                        Log.print("Losing score: " + currentBestMove.score + "\r\n");
                        panicNotice = false;
                    }
                    return false;
                }
              
                if (time <= 2 * panicTime
                        && currentBestMove.score * beginSide < 1500
                        && currentBestMove.score * beginSide
                                < choice.score * beginSide - 400) {
                    if (panicNotice) {
                        Log.println(
                                "Severe Panic: " + currentBestMove.score + " "
                                + choice.score);
                        // Log.Log.print("Sever Panic: " + currentBestMove[0].score + " " +  choice.score + " " + "\r\n");
                        panicNotice = false;
                    }
            
                    return false;
                }
            
                if (time <= panicTime
                        && currentBestMove.score * beginSide < 1000
                        && currentBestMove.score * beginSide
                                < choice.score * beginSide - 200) {
                    if (panicNotice) {
                        Log.print(
                                "Panic: " + currentBestMove.score + " "
                                + choice.score + " " + "\r\n");
                        // Log.Log.print("Panic: " + currentBestMove[0].score + " " +  choice.score + " " + "\r\n");
                        panicNotice = false;
                    }
                    return false;
                }
              }
              
                timeoutFlag = true;
                return true;
            }
        }
        return false;   
    } 
  
    /*private boolean mateChecker(int side) {

        Move move_list = generator.generateMoves(bord, side);
 
        for (Move  m = move_list; m != null; m = m.next) {
            search_counter++;
            m.apply(bord);
            int value = sbe.terminalEvaluation(bord, -side) * side;

            m.undo(bord);
   
            if (value >= Board.WIN_SCORE) {
                return true;
            }
        }
         
        return false;
    }  */

    private Move selectRootMove(double depth, int side, int beta, int alpha, int pathlength, int node_type, boolean pbCut) {
    
        lazyCounter++;
        long oldCounter;
        Move bestMoveArray[] = new Move[kBestMode + 1];
        
        bestMoveArray[0] = dummyMove;              
      
        if (countNodes >= maxNodes && !neverEnd) { 
            timeoutFlag = true;
            return  bestMoveArray[0];
        }
           
        // Seek score and best move for the current position
        Move transMove = null;
      
        final int oldalpha = alpha;   
        TableEntry element = TransTable.retrieve(bord.hashValue);  
        double  height = element.getHeight();
        boolean TTHit = false;
      
        if (isTrans) {
            if (checkSymmetry && depth > 6 && height < 0) {
                element = getSymmetry(bord);
                height = element.getHeight();
            }
        
            transMove = pvMoves[pathlength];
            pvMoves[pathlength] = null;
        
            if (height >= 0) {       
                transMove = element.getMove();
                Move nextTrans = pvMoves[pathlength + 1] = transMove.next;
         
                transMove = generator.generateIncMove(bord, side, transMove,
                        null, -1, false); 
                   
                if (transMove == null) {
                    Log.print("wrong \r\n");   
                    Log.print(bord.toString() + "\r\n");        
                    Log.print(element.getMove() + " " + side + " " + depth + "\r\n");
                } else {
                    TTHit = true;
                    
                    
                }
                   
                counterTTHITS++;
             
                if (!(/* node_type == PV_NODE &&*/element.isMarked())
                        && transMove != null) {
                    transMove.score = element.getScore(); 
      
                    if (/* !isRealizationProbability && */(height >= depth
                            || Math.abs(transMove.score) >= Board.WIN_SCORE)) {
                        int flag = element.getFlag();
        
                        pvMoves[pathlength + 1] = null;    
            
                        // Correct depth when game-over position 
                        if (Math.abs(transMove.score) >= Board.WIN_SCORE) {  
                            if (transMove.score > 0) {
                                transMove.score -= pathlength;
                            } else {
                                transMove.score += pathlength;
                            }
                        }    
         
                        if (flag == TranspositionTable.VALID) {
                            updateWFreq((side == Board.B_SIDE) ? totalB : totalR,
                                    transMove,  depth);
                            return  done(depth, 1000, side, transMove,
                                    oldalpha, beta, pathlength);
                            // /forward prune fully seen before
                        }
          
                        if (flag == TranspositionTable.LBOUND
                                && transMove.score * side > alpha * side) {
                            alpha = transMove.score;
                        } // Narrow the window
                        if (flag == TranspositionTable.UBOUND
                                && beta * side > transMove.score * side) {
                            beta = transMove.score;
                        }  // Narrow the window
                        if (alpha * side >= side * beta) // Forward prune, no futher interest
                        {
                            updateWFreq((side == Board.B_SIDE) ? totalB : totalR,
                                    transMove, depth); 
                            return  done(depth, 1000, side, transMove,
                                    oldalpha, beta, pathlength);       
                        }
                        pvMoves[pathlength + 1] = nextTrans;
                    }
                } else { // override position when same depth
                    if (depth == height) {
                        height--;
                    } 
                }  
            } else {
                transMove = generator.generateIncMove(bord, side, transMove/* pvMoves[pathlength]*/,
                        null, -1, false);
            } 
        }
    
        // Count bord as one move node expansion.
        searchCounter++;    
        
        if (!TTHit) {
            int Check_for_Win = gameAI.terminalEvaluation(bord, -side);
           
            if (Check_for_Win != 0) {
                if (Check_for_Win == Board.DRAW_SCORE) {
                    bestMoveArray[0].score = drawValue;
                } else if (Check_for_Win > 0) {
                    bestMoveArray[0].score = TERMINAL_SCORE - pathlength;
                } else {
                    bestMoveArray[0].score = -TERMINAL_SCORE + pathlength;
                }
                 
                return  bestMoveArray[0];
            }     
        }   
      
        Move move_list = (rootMoves==null)?generator.generateMoves(bord, side):rootMoves;
        Move m, next, opponent_move = null;
        
        if (move_list != null) {
            // we have generated the moves, order them   
            gameAI.assignValues(move_list, bord, RootBigTable, null, depth);
           
         
            setTransKill(move_list, transMove, pathlength);       
            move_list = sort(move_list);
            
            double moveCounter = 0;
            final long[] wfreq = (side == Board.B_SIDE) ? totalB : totalR;
            double  dec_depth;
            int k = 0;  
            dec_depth = this.getDepth(move_list);
            oldCounter = lazyCounter;
        
            move_list.apply(bord);
            updateWFreq(wfreq, move_list, depth);
         
            if (isRPS) {
            	if(isERPSONE && dec_depth>gameAI.thresholdRPS)
            		dec_depth =gameAI.thresholdRPS;
            	
                opponent_move = selectTransMove(depth - dec_depth, -side,
                        //alpha, alpha + side, true, pathlength + 1,
                        //(node_type == CUT_NODE) ? ALL_NODE : CUT_NODE, pbCut);
                		alpha, beta, true, pathlength + 1,
                        -node_type, pbCut); 
                if (side * opponent_move.score > side * alpha
                        && (node_type == PV_NODE || depth - minFP > 0)) {
                
                    recycle(opponent_move, dummyMove);
                    opponent_move = selectTransMove(depth - minFP, -side,
                            alpha, beta, true, pathlength + 1,
                            -node_type, true);
                }
            } else {      
                opponent_move = selectTransMove(depth - 1, -side, alpha,
                        beta, true, pathlength + 1, -node_type, pbCut);
            }
     
            move_list.undo(bord);
            RootBigTable[move_list.index] = lazyCounter - oldCounter;
            m = move_list.next;    
            bestMoveArray[0] = currentBestMove = move_list; 
            bestMoveArray[0].score = opponent_move.score;
            bestMoveArray[0].next = opponent_move;
            printPV(bestMoveArray[0]);
         
            if (kBestMode == 0
                    && side * bestMoveArray[0].score >= side * beta) { 	 
                // recycle(move_list, best_move);
                effective[0]++;
                return done(depth, height, side, bestMoveArray[0],
                        oldalpha, beta, pathlength);
            }
                   	      
            moveCounter++;   
         
            // Look at every possible move.
            while (m != null) { 
                next = m.next;
                moveCounter++;
                oldCounter = lazyCounter;
                dec_depth = getDepth(m);
                // Apply the move.
                m.apply(bord);
                updateWFreq(wfreq, m,  depth);
             
                // fail soft condition
                if (bestMoveArray[kBestMode] != null
                        && alpha * side
                                < side * bestMoveArray[kBestMode].score) {
                    alpha = bestMoveArray[kBestMode].score;
                } 
            
                // See how our opponent can respond.
                // Zero-width minimal window search
                if (isPVS) {
                    // full window, when we are in a pv node and have a fail-low
                    if (node_type == PV_NODE
                            && bestMoveArray[0].score * side
                                    < alpha * side
                                    && !isRPS) { 
                        opponent_move = selectTransMove(depth - 1, -side,
                                alpha, beta, true, pathlength + 1,
                                PV_NODE, pbCut);
                    } else {
                    	 
                         	if(isERPSONE && dec_depth>gameAI.thresholdRPS && moveCounter <=gameAI.moveRPS)
                         		dec_depth =gameAI.thresholdRPS;
                        
                         	opponent_move = selectTransMove(depth - dec_depth, -side,
                                alpha, alpha + side, true,
                                pathlength + 1,
                                (node_type == CUT_NODE) ? ALL_NODE : CUT_NODE,
                                pbCut);  
         	    
                        // If bord gives us a better outcome, take bord move.
                        // if(side * opponent_move.score > side * best_move.score ) 
                        // re-search, if fail high
                        if (((side * alpha < side * opponent_move.score)
                                && (side * opponent_move.score
                                        < side * beta)
                                        && !(depth <= 2 && !isQuiescence))
                                                || (isALLCUT
                                                        && node_type == PV_NODE
                                                        && beta
                                                                == alpha
                                                                        + side
                                                                        && opponent_move.score
                                                                                == beta)
                                                                                || (isRPS
                                                                                        && side
                                                                                                * opponent_move.score
                                                                                                        > side
                                                                                                                * alpha
                                                                                                                && (node_type
                                                                                                                        == PV_NODE
                                                                                                                                || depth
                                                                                                                                      - minFP
                                                                                                                                                > 0))) {            
                            
                      
                        	
                        	Move oldopponent_move;
              
                            if ((isALLCUT
                                    && opponent_move.score == alpha + side)
                                            || isRPS) {
                                opponent_move.score = alpha;
                            }
                   
                            dec_depth = (isRPS) ? minFP : 1;
                 
                            oldopponent_move = opponent_move;
                            opponent_move = selectTransMove(depth - dec_depth,
                                    -side, opponent_move.score, beta,
                                    true, pathlength + 1, -node_type/* PV_NODE*/,
                                    true);
                            recycle(oldopponent_move, dummyMove);
                           
                        }
                    }
                } else {
                    opponent_move = selectTransMove(depth - 1, -side,
                            alpha, beta, true, pathlength + 1,
                            PV_NODE, pbCut);
                }
         
                // Undo the move (so that we can apply another one)  
                m.undo(bord);   
                RootBigTable[m.index] = lazyCounter - oldCounter;
         
                // If bord gives us a better outcome, take bord move.
                if ((bestMoveArray[kBestMode] == null
                        || side * opponent_move.score
                                > side * bestMoveArray[kBestMode].score)
                                        && !timeoutFlag) {

                              
                    if (k < kBestMode) {
                        k++;
                    }
                    bestMoveArray[k] = m;
                    bestMoveArray[k].score = opponent_move.score;
                    bestMoveArray[k].next = opponent_move;
                    printPV(bestMoveArray[k]);
                    Move.mergeSort(bestMoveArray, k + 1, side);       
                    currentBestMove = bestMoveArray[0];
      
                    // cut off?  
                    if (side * bestMoveArray[k].score >= side * beta) {
                        // bestMTable[mcat]++;
                        effective[1]++;
                        generator.recycle(next); // , best_move); 
                        // best_move.next = best_opponent_move;
                        return done(depth, height, side, bestMoveArray[0],
                                oldalpha, beta, pathlength);
                    }
                } else {
                    recycle(opponent_move, dummyMove); 
                    generator.recycleMove(m);
                }
          
                m = next;     
            }     
        }            
        // If there are no moves, just pass
        if (move_list == null) {           
            Move.applyNullMove(bord);
            opponent_move = selectTransMove(depth - 1, -side, alpha,
                    beta, true, pathlength + 1, -node_type, pbCut);
            Move.undoNullMove(bord);
            return opponent_move;
        }
                  
                   
        return done(depth, height, side, bestMoveArray[0], oldalpha,
                beta, pathlength);
    }

    // Alpha beta with the transposition table enhancement
    protected Move selectTransMove(double depth, int side, int beta, int alpha, boolean null_move, int pathlength, int node_type, boolean isExtension) {
        lazyCounter++;
           
        Move best_move = dummyMove;
   
        if (timeoutFlag || (timeToQuit())) {
            return best_move;
        }
        
        final int numOfOccurence  = gameAI.checkOnDraw(bord);
      
        if (numOfOccurence == Board.REPETITION ||(isRPS && numOfOccurence != 0 && pathlength > 3)) {
            best_move.score = drawValue;
            return best_move;
        }
           
        
        if(isLeafThreat && depth <1 && gameAI.extendSearch(bord) && side * gameAI.evaluate(bord, -side)>side *alpha){
        	  	depth++;
        	  	threatCounter++;
        }        	
        
        if (depth <= 0 || pathlength >= MAX_LENGTH - 1) 
                depth = 0;    
     
        if (gameAI.isWonTable && depth < maxDepth) {
            best_move.score = winTable.retrieve(bord.hashValue);
               
            if (best_move.score / beginSide > 0) {
                best_move.score += beginSide * pathlength;
      
                return  best_move;
                // forward prune fully seen before
            }
        }
     
        if (countNodes >= maxNodes && !neverEnd) { 
            timeoutFlag = true;
            return best_move;
        }
        
         
        // Seek score and best move for the current position
        Move transMove = null;
   
        final int oldalpha = alpha;   
        TableEntry element = TransTable.retrieve(bord.hashValue);  
        double  height = element.getHeight();
        boolean checkTerminal = false;
        boolean failednullMove = false;
     
        if (isTrans) {
            if (checkSymmetry && depth > 6 && height < 0) {
                element = getSymmetry(bord);
                height = element.getHeight();
            }
     
            transMove = pvMoves[pathlength];
            pvMoves[pathlength] = null;
     
            if (height >= 0) {       
                transMove = element.getMove();
                Move nextTrans = pvMoves[pathlength + 1] = transMove.next;
      
                transMove = generator.generateIncMove(bord, side, transMove,
                        null, -1, false); 
                
                if (transMove == null) {
                    Log.println(
                            "Collision\n\r" + bord + "\n\r" + element.getMove()
                            + " " + side + " " + depth);
                    height=-1;//signal nothing found, can be stored in done
                   
                } else {
                    checkTerminal = true;
                }
                
                counterTTHITS++;
          
                if (!element.isMarked() && transMove != null) {
                    transMove.score = element.getScore(); 
     
                    if (depth > 2 && depth - 2 <= height
                            && transMove.score * side < side * beta
                            && element.getFlag() == TranspositionTable.UBOUND) {
                        failednullMove = true;     	
                    }
   
                    if ((height >= depth
                            || Math.abs(transMove.score) >= Board.WIN_SCORE)) {
                        int flag = element.getFlag();
     
                        pvMoves[pathlength + 1] = null;    
         
                        // Correct depth when game-over position 
                        if (Math.abs(transMove.score) >= Board.WIN_SCORE) { 
                        	
                            if (transMove.score > 0) {
                                transMove.score -= pathlength;
                                
                                if(transMove.score < TERMINAL_SCORE-MAX_LENGTH)
                                	transMove.score = TERMINAL_SCORE-MAX_LENGTH;
                                
                                
                            } else {
                                transMove.score += pathlength;
                                
                                if(transMove.score > -TERMINAL_SCORE+MAX_LENGTH)
                                	transMove.score = -TERMINAL_SCORE+MAX_LENGTH;
                                
                            }
                                                   
                        }                            
                        
                        if (flag == TranspositionTable.LBOUND
                                && transMove.score * side > alpha * side) {
                            alpha = transMove.score;
                        } else if (flag == TranspositionTable.UBOUND
                                && beta * side > transMove.score * side) {
                            beta = transMove.score;
                        } else if (flag == TranspositionTable.VALID) {
                            updateWFreq((side == Board.B_SIDE) ? totalB : totalR,
                                    transMove, depth);
                         
                            return  done(depth, 1000, side, transMove,
                                    oldalpha, beta, pathlength);
                            // /TransMove; ///forward prune fully seen before
                        }
        
                        // Narrow the window
                        if (alpha * side >= side * beta) // Forward prune, no futher interest
                        {                                          	
                            updateWFreq((side == Board.B_SIDE) ? totalB : totalR,
                                    transMove, depth); 
                            return  done(depth,1000/*height +1*/, side, transMove,
                                    oldalpha, beta, pathlength);       
                            // return TransMove;
                        }
                        pvMoves[pathlength + 1] = nextTrans;
                    }
                } else { // override position when same depth
                    if (depth == height) {
                        height--;
                    } 
                }  
            } else {
                transMove = generator.generateIncMove(bord, side, transMove/* pvMoves[pathlength]*/,
                        null, -1, false);
            } 
        }
 
        // Count bord as one move node expansion.
        searchCounter++;    
   
        if (depth <= 0) {
        	countNodes++;    
        	
            if (isQuiescence && gameAI.incMoveGeneration){
            	best_move.score = quiescSearch(QUIESENCE_DEPTH, side, beta,
                        alpha, pathlength);
            	
            	   return best_move;
            }else  if (isQuiescence){
            	best_move.score = quiescForcedSearch(QUIESENCE_DEPTH, side, beta,
                        alpha, pathlength);
            	
            	   return best_move;
            }else
                best_move.score = gameAI.evaluate(bord, -side); 
        
                if (best_move.score == Board.DRAW_SCORE) {
                    best_move.score = drawValue;
                    return best_move;
                } 
                else                
               	  if (Math.abs(best_move.score) >= Board.WIN_SCORE) { 
                    
               		 if (best_move.score > 0) {
                        best_move.score = TERMINAL_SCORE - pathlength;
                    } else {
                        best_move.score = -TERMINAL_SCORE + pathlength;
                    }
                    
                    return best_move;
            }
                return best_move;
        }    
           
        if (!checkTerminal) {
            int Check_for_Win = gameAI.gameTheoreticEvaluation(bord, -side);
                    
            if (Check_for_Win >= Board.WIN_SCORE) {
                    best_move.score = TERMINAL_SCORE - pathlength;
                    return best_move;
            } 

            if (Check_for_Win <= -Board.WIN_SCORE) { 
                    best_move.score = -TERMINAL_SCORE + pathlength;
                    return best_move;
            }  
            
            if (Check_for_Win == Board.DRAW_SCORE) {            	
                best_move.score = drawValue;
                return best_move;
            }
        }           
        
//      /Endgame DataBase
       if(isEGDB  &&egdb!=null && gameAI.hasDatabase(bord) && pathlength > databasePly){   	   
    	  
       	  try {
       		int info = egdb.getInfo(bord);
       		//System.out.println(info);
       		if(info!=0){
       		 egdbCounter++;
       		        		       	
       		if(info==1){
       		 best_move.score =side*(TERMINAL_SCORE-pathlength -gameAI.heuristicDTC(bord, side));//(TERMINAL_SCORE-pathlength -);
       			return	best_move;}
       			else
       				if(info==2){
       					best_move.score =-side*(TERMINAL_SCORE-pathlength -gameAI.heuristicDTC(bord, -side));//*(TERMINAL_SCORE-pathlength);
       					return	best_move;
       				}  
       				else {
           					best_move.score = this.drawValue;
           					return	best_move;
           				}
       		
       		}
       	} catch (IOException e) {
       		e.printStackTrace();
       	}
	  
    }
        
  //  Mate-Distance Pruning a la Fruit
  //         
 //Adjust LowerBound     
  int matescore = -side*(TERMINAL_SCORE-(pathlength+1));
    
       if(alpha*side<matescore*side){
    	          
        if(matescore*side>=beta*side){
        	
        	 realCounterTTHITS++;
        	best_move.score = matescore;
            return best_move;
        	
        }
        
        alpha=matescore;
       }
 
// Adjust UpperBound      
        matescore = side*(TERMINAL_SCORE-(pathlength+1));
       
       if(side*matescore<side*beta){
       
         if(side*matescore<=alpha*side){
        	 
        	realCounterTTHITS++;
          	best_move.score = matescore;
            return best_move;
          }
         
         beta=matescore;
       }

///////////////////////////////////////////////////////////////////////////////
       
        Move move_list = null, m;   
 
        boolean listGenerated = false;     
 
        if (depth > 2 ||  !gameAI.incMoveGeneration) {
            move_list = generator.generateMoves(bord, side);
            listGenerated = true;
            
            if (isTrans) {
            	
                int score, flag;
                double h;
                double newdepth = (isRPS)
                        ? depth - minFP
                        : depth - 1;
            
                for (m = move_list; m != null; m = m.next) { 
                    element = TransTable.retrieve(m.hash(bord));
                    h = element.getHeight();  
                    if (h >= 0 && !element.isMarked()) {
                        flag = element.getFlag();
                        if (flag != TranspositionTable.LBOUND) {
                            counterTTHITS++;
                            score = element.getScore();
                            if ((h >= newdepth
                                    || Math.abs(score) >= Board.WIN_SCORE)) {
                                // Correct depth when game-over position 
                                if (score >= Board.WIN_SCORE) {
                                    score -= pathlength + 1;
                                } else if (score <= -Board.WIN_SCORE) {
                                    score += pathlength + 1;
                                }        
           
                                if (side * score >= side * beta) {
                                    recycle(move_list, m);
                                    updateWFreq(
                                            (side == Board.B_SIDE)
                                                    ? totalB
                                                    : totalR,
                                                    m,
                                                    depth);
                                    m.score = score;
                                    m.next = null;  
                                    return done(depth, height, side, m,
                                            oldalpha, beta,
                                            pathlength);
                                }              
                            }
                        }  
                    }                                         
                  
                }
            }
        }
   
        Move opponent_move = null;
        final long Marker = lazyCounter;
      
        boolean tryMultiCut = true;
        
        // This is the begin of the null move part
        boolean doNullMove = (/* node_type == CUT_NODE &&*/node_type != PV_NODE && beta*side<Board.WIN_SCORE 
                && isNullMove && null_move
                && ((node_type == ALL_NODE && depth > 1) || depth > 2) 
                && !failednullMove 
                && ((side == Board.B_SIDE && bord.B_Counter > 3)
                        || (side == Board.W_SIDE && bord.W_Counter > 3)));
  
           
        if (doNullMove) {                    
            // adaptive null move pruning
            int R = (node_type == ALL_NODE/* || !pbCut*/)
                ? R = 3
                : ((adaptive && (((int) (depth) &1) == 1))
                        ? reduceSearch(depth, side)
                        : 2);
         
            int  null_pass_thresh = alpha;
            int  null_prune_thresh = beta;
           
            if ((/*(isRPS && depth <=2) ||*/depth == 2) && node_type == ALL_NODE) {
                null_prune_thresh = beta - /* ALLCUT_DELTA*/200 * side;
                null_pass_thresh = null_prune_thresh - side;
            }
         
            Move.applyNullMove(bord);            
            opponent_move = selectTransMove(Math.max(depth - 1 - R, 0), -side,
                    null_pass_thresh, null_prune_thresh, false, pathlength + 1,
                    -node_type, isExtension);
            Move.undoNullMove(bord);
     
            // cut off
            if (doNullMove  && side * opponent_move.score >= side * null_prune_thresh) {
                            	         	
                best_move.score = beta;
                generator.recycle(move_list);              	
                
                return best_move;
            }
            else
            // No cutting stuff this is an endgame position  
            if (opponent_move.score * side <= -Board.WIN_SCORE) {
                    tryMultiCut = false;
                }
        }     
 
        // /boolean multiCutPruning = false;  
        int phase = 0;
        final long[] wfreq = (side == Board.B_SIDE) ? totalB : totalR;
        double dec_depth;
     	
        // we have generated the moves, order them
        if (listGenerated) {
               
            if (side == Board.B_SIDE) {
                gameAI.assignValues(move_list, bord, HistoryB,wfreq, depth);
            } else {
                gameAI.assignValues(move_list, bord, HistoryR,wfreq, depth);
            }  
            
                 
            setTransKill(move_list, transMove, pathlength);       
            move_list = sort(move_list);
   
            int mc_R = 2;
 
            // Multi-Cut pruning
            if (((tryMultiCut && depth > mc_R
                    && ((node_type == CUT_NODE && isMultiCut)
                    || (isALLCUT && node_type == ALL_NODE))))
                    && move_list != null) {
 
                int mc_C = (node_type == CUT_NODE) ? 3 : allCutC;
                // if(pbCut && node_type == ALL_NODE){pbCut= false; mc_C=--;}
                int mc_M = (node_type == CUT_NODE) ? 10/* +1-mc_C*/ : allCutM;
                
                      
                double new_depth = Math.max(
                        depth - ((node_type == ALL_NODE)
                                        ? ALL_R : mc_R), 1);
                int beta_cuts = 0;
                int moveCounter = 0;

                best_move = move_list;
                int best_value = -side * TERMINAL_SCORE;

                m = move_list;  
                int value;     
                
               // Move beta_move =null;
      
                // Look at every possible move.
                for (; m != null && moveCounter < mc_M; m = m.next, moveCounter++) { 
                    // Apply the move.
                	dec_depth = getDepth(m);      
                	
                 	/*if(isERPSONE && dec_depth>gameAI.thresholdRPS && moveCounter <gameAI.moveRPS)
                 		dec_depth =gameAI.thresholdRPS;*/
      
                    m.apply(bord); 
                    updateWFreq(wfreq, m, new_depth);
                    // See how our opponent can respond.     
                    opponent_move = selectTransMove(new_depth - dec_depth, -side,
                            alpha, beta, true, pathlength + 1,
                            -node_type,/* (node_type == ALL_NODE)?false:*/ isExtension);
                    // Undo the move (so that we can apply another one) 
                    if (isRPS &&  side * opponent_move.score > side * alpha
                            && new_depth - minFP >= 0) {
                        recycle(opponent_move, dummyMove);
                                               
                        
                       opponent_move = selectTransMove(new_depth - minFP,
                                -side, alpha, beta, true,
                                pathlength + 1, -node_type, true);
                    }      
                        
                    m.undo(bord); 
                    value = opponent_move.score;
                    m.score = -moveCounter;
                    recycle(opponent_move, dummyMove); 
      
                    // If bord gives us a better outcome, take bord move.
                    if (side * value > side * best_value) {
                        best_move = m;
                        best_value = value;
                    }
    
                    if (side * value >= side * beta) {
                    	
                        beta_cuts++;	
                        m.score = 500000 - moveCounter; // + side * opponent_move.score;
                        if (side == Board.B_SIDE) {
                            HistoryB[m.index] += incrHis(new_depth);
                        } else {
                            HistoryR[m.index] += incrHis(new_depth);
                        }
      
                        // if(node_type == CUT_NODE )mc_M++;
           
                        // quit when three beta cuts. Forced win also exists at deeper searches, so quit.
                        if (beta_cuts >= mc_C || side * value >= Board.WIN_SCORE
                                || (node_type == ALL_NODE
                                && (side * value
                                        >= side * beta + allCutDelta)) /*|| (beta_cuts == 2 && mc_C==3 && !m.isSimilar(beta_move))*/) {
                            // multiCutPruning = true;
                            if (side == Board.B_SIDE) {
                                HistoryB[best_move.index] -= incrHis(new_depth);
                            } else {
                                HistoryR[best_move.index] -= incrHis(new_depth);
                            }

                            /* if(node_type==CUT_NODE && !pbCut && pathlength <=1)
                             multiCutCounter++;*/
                            // break;
                            recycle(move_list, best_move);
                            best_move.next = null;
     	 
                            if (node_type == CUT_NODE
                                    || side * value >= Board.WIN_SCORE) {
                                best_move.score = best_value;
                                return done(new_depth, height, side, best_move,
                                        oldalpha, beta, pathlength);
                            } else {
                                best_move.score = beta;
                                return done(new_depth, height, side, best_move,
                                        oldalpha, beta, pathlength); // best_move;
                            }
                        }
                        
                    }
      
                }
    
               //keep default sorting
                int i = -moveCounter;

                for (; m != null; m = m.next, i--) {
                    m.score = i;
                }	     
  
                // keep TT move in front
                if (transMove != null && height >= new_depth) {
                    move_list.score = Integer.MAX_VALUE;
                }
     
            }

          if(move_list!=null)
            phase = 3;
          else
        	phase = 1000;
        }
 
        Move next, best_opponent_move = null;
        boolean firstMoveDone = false; 
               
        boolean futilPrune = false;
           
        int indexTrans=0, indexKiller1=0, indexKiller2=0;
        int moveCounter = 0;
        boolean doNormalSearch;
     
        // incremental generation 
        for (; phase < 4; phase++) {
            if (!listGenerated) {          
                switch (phase) {
                case 0: 
                    move_list = transMove;   
                                        
                    if (move_list != null) {
                        indexTrans = move_list.index;
                        break;
                    } else {
                        continue;
                    }

                case 1: 
                	MarkedMoves[indexTrans] = Marker;
                    move_list = generator.generateIncMove(bord, side,
                            KillerMove[pathlength][0], MarkedMoves, Marker,
                            false);
                      
                    if (move_list != null) {
                        indexKiller1 = move_list.index;
                        break;
                    } else {
                        continue;
                    } 

                case 2:
    
                	MarkedMoves[indexTrans] = Marker;
                    MarkedMoves[indexKiller1] = Marker;  
                    move_list = generator.generateIncMove(bord, side,
                            KillerMove[pathlength][1], MarkedMoves, Marker,
                            false);
                              
                    if (move_list != null) {
                        indexKiller2 = move_list.index;
                        break;
                    } else {
                        continue;
                    }

                case 3:
                    MarkedMoves[indexTrans] = Marker;
                    MarkedMoves[indexKiller1] = Marker;
                    MarkedMoves[indexKiller2] = Marker;
           
                    // Get the list of possible moves from the move generator. 
                    move_list = generator.generateUnMarkedMoves(bord, side,
                            MarkedMoves, Marker);
                
                    // sort the list with all the moves
                    if (side == Board.B_SIDE) {
                        gameAI.assignValues(move_list, bord, HistoryB,wfreq, depth);
                    } else {
                        gameAI.assignValues(move_list, bord, HistoryR,wfreq, depth);
                    }
                    
                    if (move_list == null) 
                    	continue;
                    else break; 
        
                // default: break;
                }
            }             
                
            // Principal variation?    
            if (!firstMoveDone) { 
                move_list = getBestMove(move_list);
                dec_depth = getDepth(move_list);
                                
                move_list.apply(bord);
                updateWFreq(wfreq, move_list, depth);
      
                if (isRPS) {
                  	if(isERPSONE && dec_depth>gameAI.thresholdRPS)
                 		dec_depth =gameAI.thresholdRPS;
                  	
                    best_opponent_move = opponent_move = selectTransMove(
                            depth - dec_depth, -side, alpha,
                           beta,  //alpha + side,                             
                            true, pathlength + 1,
                           -node_type,// (node_type == CUT_NODE) ? ALL_NODE : CUT_NODE,
                            		isExtension);  
                    
                    if (side * opponent_move.score > side * alpha && (node_type == PV_NODE || depth - minFP > 0)) {
                                          	
                    	doNormalSearch =true;
                        if(isERPSTWO){
                        	 dec_depth =  (minFP +dec_depth)/2;
                        	 
                        	 if(dec_depth>gameAI.deltaRPS && depth-dec_depth >0.1){                     	 
                        		 
                        		 recycle(opponent_move, dummyMove);
                                                                   
                        		 best_opponent_move=opponent_move = selectTransMove(depth - dec_depth,
                                          -side, alpha, beta, true, pathlength + 1, -node_type/* PV_NODE*/, true);  
                                
                                 doNormalSearch = (side*opponent_move.score > side*alpha);
                            
                        	 }
                        }
                        if(doNormalSearch ){                    	
                    	
                        recycle(opponent_move, dummyMove);
                        best_opponent_move = opponent_move = selectTransMove(
                                depth - minFP, -side, alpha,
                                beta, true, pathlength + 1, -node_type,
                                true);}
                    }
                } else {      
                    best_opponent_move = opponent_move = selectTransMove(
                            depth - dec_depth, -side, alpha, beta, true,
                            pathlength + 1, -node_type, isExtension);
                }
      
                best_move = move_list;   
                best_move.score = opponent_move.score;
                move_list.undo(bord);
      
                if (side * best_move.score >= side * beta) { 	 
                    recycle(move_list, best_move);
                    best_move.next = best_opponent_move;
                    effective[0]++;
                    // bestMTable[mcat]++;
                    return done(depth, height, side, best_move, oldalpha,
                            beta, pathlength);
                }
                 
                m = move_list.next;

                firstMoveDone = true;
                moveCounter++;
            } else {
                m = move_list;
            }           
            
            final int futMoves = (node_type == CUT_NODE)?CUTFUTMOVES: ALLFUTMOVES;
            // Look at every possible move.
            while (m != null) { 
                m = getBestMove(m);
                next = m.next;
                moveCounter++;
  
                dec_depth = getDepth(m);
                
                // fail soft condition
                if (alpha * side < side * best_move.score) {
                    alpha = best_move.score;
                } 
                                
                if(isFutility && moveCounter == futMoves &&  (depth<=1 || (isRPS && depth <=4)) && node_type!= PV_NODE){
                
                	final int margin = (int)( gameAI.futMargin *(1+depth*0.1));//1000+(int)(depth*100);
                	
                	if(side*gameAI.evaluate(bord, -side)+ margin <= side*alpha)
                		futilPrune = true;
                }
                
                if(futilPrune  && moveCounter >= futMoves &&  dec_depth >=1 && depth -  dec_depth <=0){
                	 generator.recycleMove(m);
                     m = next;
                    
                	 continue;
                }
                
                // Apply the move.
                m.apply(bord);
                updateWFreq(wfreq, m, depth);
                   
                // See how our opponent can respond.
                // Zero-width minimal window search
                if (isPVS) {
                    // full window, when we are in a pv node and have a fail-low
                    if (node_type == PV_NODE && best_move.score * side < alpha * side && !isRPS) { 
                        opponent_move = selectTransMove(depth - 1, -side,
                                alpha, beta, true, pathlength + 1,
                                PV_NODE, isExtension);
                    } else {
                    
                    	
                    if(isLMR && node_type!=PV_NODE && ((!isRPS && moveCounter> 4) || (isRPS && node_type==CUT_NODE &&  moveCounter> 9))  && depth >= (LMR_R+1) )// || (isRPS && moveCounter> 6 && depth >= (LMR_R+dec_depth) && gameAI.evaluate(bord,side)*side<=alpha*side)))
                    	{
                    	opponent_move = selectTransMove(depth - dec_depth-LMR_R, -side,
                                alpha, alpha + side, true,
                                pathlength + 1,
                                (node_type == CUT_NODE) ? ALL_NODE : CUT_NODE,
                                isExtension);
                    	
                    	if(side*opponent_move.score > side*alpha)
                    		doNormalSearch = true;
                    	else
                    		doNormalSearch = false;
                    	}
                    else doNormalSearch = true;
                    	
                    if(doNormalSearch){
                    	
                    	if(isERPSONE && dec_depth>gameAI.thresholdRPS && moveCounter <=gameAI.moveRPS)
                     		dec_depth =gameAI.thresholdRPS;
                    	
                        opponent_move = selectTransMove(depth - dec_depth, -side,
                                alpha, alpha + side, true,
                                pathlength + 1,
                                (node_type == CUT_NODE) ? ALL_NODE : CUT_NODE,
                                isExtension);  
      	    
                        // If bord gives us a better outcome, take bord move.
                        if (((side * alpha < side * opponent_move.score)&& (side * opponent_move.score  < side * beta) && !(depth <= 2 && !isQuiescence)) 
                        		|| (isALLCUT && node_type == PV_NODE  && beta == alpha + side && opponent_move.score == beta)
                        		|| (isRPS && side * opponent_move.score  > side * alpha &&  (node_type == PV_NODE || (depth - minFP > 0 )))) {     
                        	
                        	doNormalSearch =true;
                            if(isERPSTWO){
                            	 dec_depth =  (minFP +dec_depth)/2;
                            	 
                            	 if(dec_depth>gameAI.deltaRPS && depth-dec_depth >0.1){                     	 
                            		 
                            		 recycle(opponent_move, dummyMove);
                                                                       
                            		  opponent_move = selectTransMove(depth - dec_depth,
                                              -side, alpha, beta,
                                              true, pathlength + 1, -node_type/* PV_NODE*/,
                                              true);  
                                    
                                     doNormalSearch = (side*opponent_move.score > side*alpha);                                
                            	 }
                            }
                            if(doNormalSearch ){
                                 if ((isALLCUT && opponent_move.score == alpha + side)|| isRPS) {
                                     opponent_move.score = alpha;
                                 }
                      
                            dec_depth = (isRPS) ? minFP : 1;
                            Move oldopponent_move = opponent_move;

                            opponent_move = selectTransMove(depth - dec_depth,
                                    -side, opponent_move.score, beta,
                                    true, pathlength + 1, -node_type/* PV_NODE*/,
                                    true);
                            recycle(oldopponent_move, dummyMove);}
             
                            /* if(opponent_move.score * side <  side* fscore && fscore != pass_thresh + side ){
                             Log.Log.println("Lower than first, is not possible " + MaxDepth);
                             Move zmove = null; zmove.toString();
                             }*/
                        }
                    }
                  }
                } else {
                    opponent_move = selectTransMove(depth - 1, -side,
                            alpha, beta, true, pathlength + 1,
                            PV_NODE, isExtension);
                }
      
                // Undo the move (so that we can apply another one)  
                m.undo(bord);   
      
                // If bord gives us a better outcome, take bord move.
                if (side * opponent_move.score > side * best_move.score
                        && !timeoutFlag) {
                    generator.recycleMove(best_move);
                    recycle(best_opponent_move, dummyMove);
                    best_move = m;
                    best_move.score = opponent_move.score;
                    best_opponent_move = opponent_move;
   
                    // cut off?  
                    if (side * best_move.score >= side * beta) {
                        // bestMTable[mcat]++;
                        effective[1]++;
                        generator.recycle(next); // , best_move); 
                        best_move.next = best_opponent_move;
                        return done(depth, height, side, best_move,
                                oldalpha, beta, pathlength);
                    }
                } else {
                	recycle(opponent_move, dummyMove); 
                    generator.recycleMove(m);
                }
       
                m = next;
            }            
        }
  
        // If there are no moves, just pass
        if (!firstMoveDone) {           
            Move.applyNullMove(bord);
            opponent_move = selectTransMove(depth - 1, -side, alpha,
                    beta, true, pathlength + 1, -node_type, isExtension);
            Move.undoNullMove(bord);
            best_move.score = opponent_move.score;
            return best_move;
        }
               
        /* if(multiCutPruning)
         falseCutCounter++;  */
	    
        /* if(nullMoveCut)
         NullMoveStatistic[(side==Board.B_SIDE)?bord.B_Counter:bord.R_Counter][1]++; */  
	      
        best_move.next = best_opponent_move;
    
        if (isALLCUT && node_type == CUT_NODE
                && best_move.score == oldalpha && depth > 2) {
            height = 1000;
        }       
        
        if(futilPrune && best_move.score*side<side*beta){
        	best_move.score = alpha;
        }
          
        return  done(depth, height, side, best_move, oldalpha,
                beta, pathlength);
    }
  
  
    // bord is the goto done in the pseudo algorithm
    private Move done(double depth, double height, int side, Move  best_move, int  alpha, int beta, int pathlength) { 
        // never store incomplete information!
        if (timeoutFlag) {
            return best_move;
        }

        // update history heuristic
        if (side == Board.B_SIDE) {
            HistoryB[best_move.index] += incrHis(depth);
        } else {
            HistoryR[best_move.index] += incrHis(depth);
        }
   
        // two level killermove scheme
        if (!KillerMove[pathlength][0].equals(best_move)) { 
            KillerMove[pathlength][1].copy(KillerMove[pathlength][0]);
            KillerMove[pathlength][0].copy(best_move);
        }
          
        // store infomation in the TT. 
        if (height <= depth  && best_move != dummyMove && isTrans) {
    
            // Exact Value?, UpperBound or Lower Bound
            int flag;
    
            if (best_move.score * side <= alpha * side) {
                flag = TranspositionTable.UBOUND;
            }
            else if (best_move.score * side >= beta * side) {
                flag = TranspositionTable.LBOUND;
            }
            else flag = TranspositionTable.VALID;
    
            if (best_move.next == dummyMove) {
                best_move.next = null;
            }
     
            // When terminal position,  store the length to mate 
            // from the current internal node (and not from the root)
            if (Math.abs(best_move.score) >= Board.WIN_SCORE) {
            	           	
                dummyMove.copy(best_move);
     
                if (dummyMove.score > 0) {
                    dummyMove.score += pathlength;
                } else {
                    dummyMove.score -= pathlength;
                } 
                       
                TransTable.storeTwoDeep(bord.hashValue, depth, flag, dummyMove);
            } else { 
                TransTable.storeTwoDeep(bord.hashValue, depth, flag, best_move);
            }
        }            
        return best_move;
    }
     
    private void updateWFreq(long[] total, Move m, double depth) {
        total[m.index] += 1; // (1<<depth);
    }
   
    private final int incrHis(double depth) {
        return 1 << (int) (depth);
    }   

    private int quiescSearch(int depth, int side, int beta, int alpha, int pathlength) {
   
        // Terminal position?
        //int best_val = gameAI.evaluate(bord, -side); 
    	
       //To handle lazy evaluation, separate game-theoretic part and heuristic part
       int best_val = gameAI.gameTheoreticEvaluation(bord, -side);
       
       if (best_val == Board.DRAW_SCORE) {
               return drawValue;
       }
      
      if (best_val >= Board.WIN_SCORE) { 
               return TERMINAL_SCORE - pathlength;
           }
     
      if (best_val <= -Board.WIN_SCORE) {
           return  -TERMINAL_SCORE + pathlength; 
       }
   
      
       best_val = gameAI.isLazy ?gameAI.heuristicEvaluation(bord,alpha,beta, -side):gameAI.heuristicEvaluation(bord, -side);
    
          
        if (depth <= 0) {
            return best_val;
        }  
       
        // FP1
        if (best_val * side >= beta * side) {
            return best_val;
        } 

        // FP2
        if (best_val * side > alpha * side) {
            alpha = best_val;
        }
       
        final long Marker = lazyCounter;       
          
        int indexKiller1 = 0, indexKiller2=0;
   
        Move move_list = null;
       // Move complete_list = null, lastMove = null;
        Move best_move = null;
   
        // incremental generation 
        for (int i = 1; i < 4; i++) {
                      
            switch (i) {
    
            case 1: 
                
                move_list = generator.generateIncMove(bord, side,
                        KillerMove_QS[pathlength][0], MarkedMoves, Marker, true);
                
                if(move_list!=null)
            		indexKiller1 = move_list.index;      
                
                
                break;  

            case 2:
               
                MarkedMoves[indexKiller1] = Marker;
           
                move_list = generator.generateIncMove(bord, side,
                        KillerMove_QS[pathlength][1], MarkedMoves, Marker, true);
                
                if(move_list!=null)
            		indexKiller2 = move_list.index;
                
                break;    

            case 3:
                
                MarkedMoves[indexKiller1] = Marker;
                MarkedMoves[indexKiller2] = Marker;
           
                // Get the list of possible moves from the move generator.
                move_list = generator.generateUnMarkedCaptureMoves(bord, side,
                        MarkedMoves, Marker);
	       
                if (move_list != null && move_list.next != null) {
                    if (side == Board.B_SIDE) {
                        move_list = moveOrdening(move_list, HistoryB,totalB,0);
                    } else {
                        move_list = moveOrdening(move_list, HistoryR,totalR,0);
                    }
                }
         
                break; 
            } 
            
            if (move_list == null)
            	continue;
         
           /* if (move_list != null) {
            	
                // danger assume lists of one move
               if (lastMove == null) {
                    complete_list = move_list;
                    lastMove = move_list;
                } else {
                    lastMove.next = move_list;
                    lastMove = move_list;
                }
            } else {
                continue;
            }*/

            Move m = move_list;
            // Look at every possible move.
            //for (Move m = move_list; m != null; m = m.next) {
            while(m!=null){
                searchCounterFQ++;
                lazyCounter++;
                countNodes++;
  
                // Apply the move.
                m.apply(bord);
                // See how our opponent can respond.     
                int act_val = quiescSearch(depth - 1, -side, alpha,
                        beta, pathlength + 1); 

                // Undo the move (so that we can apply another one)  
                m.undo(bord); 
      
                // If bord gives us a better outcome, take bord move.
                if (side * act_val > side * best_val) {
                    best_val = act_val;
                    best_move = m;
   
                    if (side * best_val >= side * beta) { /* if(side==1)
                         HistoryB_QS[best_move.index]+= (1<<depth);
                         else
                         HistoryR_QS[best_move.index]+= (1<<depth); */
      	    
                        // KillerMove_QS[pathlength].copy(best_move);
      	  
                        if (!KillerMove_QS[pathlength][0].equals(best_move)) {
                            KillerMove_QS[pathlength][1].copy(
                                    KillerMove_QS[pathlength][0]);
                            KillerMove_QS[pathlength][0].copy(best_move);
                        }
                       // generator.recycle(complete_list);
                        generator.recycle(m);
      	     
                        return best_val;
                    }
           		 
                    if (best_val * side > alpha * side) {
                        alpha = best_val;
                    }
                }
                
                Move next = m.next;            
                generator.recycleMove(m);
                m = next;                   
            }
        }    
      
        /* if(best_move != null)
         { if(side==1)
         HistoryB_QS[best_move.index]+= (1<<depth);
         else
         HistoryR_QS[best_move.index]+= (1<<depth);        
         }*/	      
       
          
      //generator.recycle(complete_list);
         
        return best_val;
    }
    
    private int quiescForcedSearch(int depth, int side, int beta, int alpha, int pathlength) {
    	   
        // Terminal position?
        int best_val = gameAI.gameTheoreticEvaluation(bord, -side);
        
        if (best_val == Board.DRAW_SCORE) {
                return drawValue;
        }
       
       if (best_val >= Board.WIN_SCORE) { 
                return TERMINAL_SCORE - pathlength;
            }
      
       if (best_val <= -Board.WIN_SCORE) {
            return  -TERMINAL_SCORE + pathlength; 
        }
    
       
        if (depth <= 0) 
            return gameAI.isLazy ?gameAI.heuristicEvaluation(bord,alpha,beta, -side):gameAI.heuristicEvaluation(bord, -side);
            
                     
        Move move_list = null;
        Move complete_list = null;
        Move best_move = null;
       
        // Get the list of possible moves from the move generator.
        move_list = generator.generateQuiescenceMoves(bord, side);
    	    
        if (move_list != null){
			  if(move_list.score!=Generator.FORCED){
					  best_val = gameAI.isLazy ?gameAI.heuristicEvaluation(bord,alpha,beta, -side):gameAI.heuristicEvaluation(bord, -side);
			     // FP1
			    if (best_val * side >= beta * side) {
			            return best_val;
			    } 
			
			    // FP2
			    if (best_val * side > alpha * side) {
			            alpha = best_val;
			        }
			  }
			     else {best_val =  -side * TERMINAL_SCORE;
			     
			     //System.out.println( (Integer.MIN_VALUE *side));
			  }
        }else
        return gameAI.isLazy ?gameAI.heuristicEvaluation(bord,alpha,beta, -side):gameAI.heuristicEvaluation(bord, -side);
        
       if (move_list.next != null) {
           if (side == Board.B_SIDE) {
              move_list = moveOrdening(move_list, HistoryB,totalB,0);
           } else {
               move_list = moveOrdening(move_list, HistoryR,totalR,0);
                  }
      }      
               
        
        complete_list = move_list;
      //  lastMove = move_list;

            // Look at every possible move.
            for (Move m = move_list; m != null; m = m.next) {
                searchCounterFQ++;
                lazyCounter++;
                countNodes++;
  
                // Apply the move.
                m.apply(bord);
                // See how our opponent can respond.     
                int act_val = quiescForcedSearch(depth - 1, -side, alpha,
                        beta, pathlength + 1); 

                // Undo the move (so that we can apply another one)  
                m.undo(bord); 
      
                // If bord gives us a better outcome, take bord move.
      
                if (side * act_val > side * best_val) {
                    best_val = act_val;
                    best_move = m;
   
                    if (side * best_val >= side * beta) {
      	  
                        if (!KillerMove_QS[pathlength][0].equals(best_move)) {
                            KillerMove_QS[pathlength][1].copy(
                                    KillerMove_QS[pathlength][0]);
                            KillerMove_QS[pathlength][0].copy(best_move);
                        }
                        generator.recycle(complete_list);
      	     
                        return best_val;
                    }
           		 
                    if (best_val * side > alpha * side) {
                        alpha = best_val;
                    }
                }
            }
                      
        generator.recycle(complete_list);
        
        return best_val;
        }
    
   
    private TableEntry getSymmetry(Board b) {

        long hash_value = TranspositionTable.getMirrorXHashValue(b);
   
        TableEntry entry = TransTable.retrieve(hash_value);
        double h = entry.getHeight();
 
        if (h >= 0) {
            Move m = entry.getMove();
            
            m.mirrorX(); 
   
            m.score = entry.getScore();
   
            if (m.next != null) {  
                m.next.mirrorX();
            }
  
            entry.setElement(hash_value, h, entry.getFlag(), m);
           
            return entry;
        } 
   
        hash_value =  TranspositionTable.getMirrorYHashValue(b);
   
        entry = TransTable.retrieve(hash_value);
        h = entry.getHeight();
 
        if (h >= 0) {
            Move m = entry.getMove();
            m.mirrorY(); 
            m.score = entry.getScore();
   
            if (m.next != null) {  
                m.next.mirrorY();
            }
  
            entry.setElement(hash_value, h, entry.getFlag(), m);
               
            return entry;
        }    

        hash_value =  TranspositionTable.getMirrorXYHashValue(b);
   
        entry = TransTable.retrieve(hash_value);
        h = entry.getHeight();
 
        if (h >= 0) {
            Move m = entry.getMove();

            m.mirrorXY(); 
        
            m.score = entry.getScore();
   
            if (m.next != null) {  
                m.next.mirrorXY(); 
            }
  
            entry.setElement(hash_value, h, entry.getFlag(), m);
              
            return entry;
        } 

        return entry;
    }
       
    private double getDepth(Move m){
    	
         return  (isRPS)?RP_Table[m.getMoveType(bord)]:(gameAI.isFractional)?(m.getFrac(bord)):1; 
    }
    	    
    /*private boolean linRegression(int [] values,int pathlength,  int alpha, int beta, int side, final int parameter){
    	
    	if(pathlength<2)
    		return false;
    	
    	final int l =(Math.min(pathlength,6))/2;
    	final int n = l+1;
     	
    	final int sumX = (n*(n+1))>>1;
    	int sumXX=0;
    	int sumXY=0;
    	int sumY=0;
    	
    	int counter =1;
    
    	
    	for(int i = pathlength - 2*l; i <= pathlength; i+=2){
    		sumXX+=counter*counter;
    		sumXY+=counter*values[i];
    		sumY+= values[i];
    		//Log.Log.print(values[i] + " ");
    		counter++;
    	}
    	
    	sumXY*=side;
    	sumY*=side;
    	alpha*= side;
    	beta*= side;
    	
    	final double a = (n*sumXY - sumX*sumY)/(double)(n*sumXX- sumX*sumX);
    	final double b = (sumY-a*sumX)/n;
    	
    	double alphaPoint = (alpha-b)/a;
    	
    	if(alphaPoint > n && alphaPoint <= n+parameter)
    	 return true;
    	
    	double betaPoint = (beta-b)/a;
    	
    	if(betaPoint > n && betaPoint <= n+parameter)
       	 return true;
    	
    	
    	return false;
    	
    }*/
    
    public static void setKBestMode(int kBest) {
		kBestMode = kBest;
	}

	public static int getKBestMode() {
		return kBestMode;
	}

    
    
    /*static{
    	
    	int [] values = {0,98,79,8,9,9,12,2,17};
    	
    	linRegression(values,8,1);
    	
    }
    */

  
} 
