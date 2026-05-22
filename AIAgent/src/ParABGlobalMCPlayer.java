
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;



public class ParABGlobalMCPlayer extends AbstractPlayer {
	
	private final PVMCRandProverPlayer [] playerList;
	private final ThreadSearch [] threadList;
	private PVMCRandProverPlayer.MCTable table; 
	
	private int countGames=0;
	
	public void quitSearch() {
		
		super.quitSearch();
		for(int i =0; i < playerList.length; i++)
			playerList[i].quitSearch();
	}
	
	

	public void setForEver(boolean b) {
		
		super.setForEver( b);
		for(int i =0; i < playerList.length; i++)
			playerList[i].setForEver(b);
	}
	
	public ParABGlobalMCPlayer(Game ai){
		
		int proc = ai.Cores;
		gameAI = ai;
		generator = ai.getGenerator();
		//final int shift = (int)(Math.log(proc)/Math.log(2));
		playerList = new PVMCRandProverPlayer[proc];
		threadList = new ThreadSearch[proc];	
		
		table = new PVMCRandProverPlayer.MCTable(gameAI.sizeTT);
				   
	   try {
		
		   Field [] list = ai.getClass().getFields();
			
			for(int i =0; i<proc;i++){
				
					Game g =  ai.getClass().getDeclaredConstructor().newInstance();
					
					g.setEvaluator(ai.getEvaluator().getClass().getDeclaredConstructor().newInstance());
					
					for(int j = list.length; --j>=0;){
					if(list[j].getType().isPrimitive() && !Modifier.isFinal(list[j].getModifiers())){
							
							//Log.println(list[j] + " " + list[j].get(ai));
							list[j].set(g,list[j].get(ai));
							//Log.println(list[j] + " " + list[j].get(g));
							
						}
					}
						
					//g.sizeTT >>=shift;	
					
					//Log.println("Old: " + ai.sizeTT + " " + ai.isRPS);
					//Log.println("New: " + g.sizeTT + " " + g.isRPS);
					
					try {
					playerList[i] = //(ai.isNN)?new AlphaNNMCTSPlayer(g):
						new PVMCRandProverPlayer(g);
					}catch(Exception e) {
						System.out.println("Cannot Load NN Player");
						playerList[i] = new PVMCRandProverPlayer(g);
						e.printStackTrace();
					}
					//playerList[i].setSeed(i + (Game.isRandom?(int)System.currentTimeMillis():0));
					playerList[i].setSeed(i + (Game.isRandom?(int)System.nanoTime()/(i+1):0));
					threadList[i] = new ThreadSearch(playerList[i]);
					
			}
	   
	   }catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException e) {
				e.printStackTrace();}
				
	}
	
	private void printPV(int d){		

		   if(gameAI.checkOnDraw(bord) == Board.REPETITION || bord.numberOfMovesPlayed >= gameAI.getMaxLength()) {
			         return;
			   }	   
					
			Move move_list = generator.generateMoves(bord);
			
			double maxvalue= Integer.MIN_VALUE;
			int games=0;
			
			Move best_move =null;
			
			for(Move m = move_list; m!=null; m=m.next){
				PVMCRandProverPlayer.MCProofEntry entry = table.retrieve(m.hash(bord));
				
				if(entry == null)
					continue;
			       
				double value = (gameAI.iMM)?entry.getIMValue(gameAI.alpha):entry.value;//entry.value;// + 1/Math.sqrt(entry.games);// + entry.mxValue*0.01;
						 
					if(value>maxvalue){
						best_move=m;
						maxvalue = value;
						games = entry.games;
					}
				}
			
			if(d<pv.length)
				pv[d]=best_move;
			
			if(best_move==null)
				return;
			
			Log.print(d + " m = " + best_move+ " n = " + games + "  v = " +  (100*maxvalue)/2 + " ");
		
			best_move.apply(bord);
			printPV(d+1);
			best_move.undo(bord);		
		}

	public Move search(Board b) {
		
		long hulp = System.currentTimeMillis();
		
		if (Board.isTournament)
            setTargetTime(b.getNumberOfMovesPlayed());
		
		Log.print("Target Time: " + (targetTime / 1000f) + "s\r\n");
					
		copy_board = (copy_board==null)?b.duplicate():b.duplicate(copy_board);
		bord = b;
		final int side = b.sideToMove;
		
		gameAI.simplifyBoard(b);
		
				
		countNodes =0;
		countGames =0;
		
		timeoutFlag = false;
				
		//Log.setLOG(true);
		if(gameAI.isRemember){
	    	//long hulp = System.currentTimeMillis();
		/*	PVMCRandProverPlayer.MCTable newTable = new PVMCRandProverPlayer.MCTable(gameAI.sizeTT);
	    	remember(b, table, newTable,3);
	    	table = newTable;
	    	*/
	    	long help = System.currentTimeMillis();
	    	//dfs( bord, table, table.getStamp(), 0, 14 );	    	
 		    int cntNodesKept = table.pack(1);
	    	Log.println( "TT-keep: ms=" + (System.currentTimeMillis()-help) + " cnt=" + cntNodesKept);
	    	
	    	Move list = generator.generateMoves(b);
	    	boolean repetition  = false;
	    	for(Move m = list; m!=null; m=m.next){
	    		m.apply(b);
	    		final int times = gameAI.checkOnDraw(b);
	    	    if(times == Board.REPETITION){
	    		PVMCRandProverPlayer.MCProofEntry entry = table.retrieve(b.hashValue);
	    		if(entry!=null)
	    			entry.value=1;
	    		    repetition  =true; 
	    	    }
	    		
	    		m.undo(b);
	    	}
	    	
	    	if(repetition){
	    		for(Move m = list; m!=null; m=m.next){
	    			PVMCRandProverPlayer.MCProofEntry entry = table.retrieve(m.hash(b));
		    		if(entry!=null)
		    			entry.games = Math.max(1,entry.games/16);
		    	    }
	    	}
	    	//System.out.println(System.currentTimeMillis()-hulp);
	    }
	    else table.clear();
	  
		table.storeBoard(table.storeDefBoard(b.hashValue), 1, 1,side*gameAI.heuristicEvaluation(b, -side),1);//table.storeBoard(table.storeDefBoard(b.hashValue), 1, 1,1);
		//table.storeDefBoard(bord.hashValue);//, 1, 1,1);	
		
		Log.println("Parallel MCTS Player: " + gameAI.Cores + " Cores");
		int counter =0;
		
		do{
					
			/* ThreadGroup tGroup = new ThreadGroup("Parallel Player");
			
		/*for(int i = 0; i<threadList.length; i++){
			Thread t = new Thread(tGroup,threadList[i]);
			t.start();
			t =null;
		}			
			
	   synchronized (tGroup) {
			    while (tGroup.activeCount() > 0) {
				try {
					tGroup.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			    }
		    }*/
	   
	     Thread [] threads = new Thread[threadList.length];
			
		for(int i = 0; i<threadList.length; i++){
				 threads[i] = new Thread(threadList[i]);
				 threads[i].start();
			}		
			
		for (Thread thread : threads) {
			  try {
				thread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}		
	   

	   if(!timeoutFlag && Board.isTournament  && gameAI.uncertainMCTS){
		   if(isStable(b))
			   break;
		   else{
			   counter++;
			   this.setFactorTime(0.5);
			   Log.println("Search is unstable: Additional Search Time: " + (targetTime / 1000f) + "s.");
			   statistic();
		   }
	   }
	   else
		   break;
		}while(counter <2);
	   //Log.setLOG(oldStatus);
			     
	   Move best = null;
	    
	    Move move_list = generator.generateMoves(bord, side);
	    double max = -Board.WIN_SCORE;
	    double maxgames =0;
	
	    for(Move m = move_list; m!=null; m=m.next){
	    	    	
	    	int egames =0;
	    	double evalue =0;
			    	
	    	PVMCRandProverPlayer.MCProofEntry entry = table.retrieve(m.hash(bord));
			
			if(entry==null){
				Log.println("Not explored: " + m);// + " " +  table.retrieveGames(m.hash(b)));
				continue;}
			
			/*if(Math.abs(entry.value)<Board.WIN_SCORE){
			   evalue = (evalue*egames + entry.games*entry.value)/(entry.games);
			   egames = entry.games;
			}
			else{*/
				evalue = entry.value;
				egames = entry.games;
			//}
			
			if(egames != 0){
				
				if(evalue>=Board.WIN_SCORE){
					Log.println("Solved: " + m + " " + evalue/2 + " " +  egames);
					max = evalue;
					best=m;
					best.score = Board.WIN_SCORE;
					//best.score = Board.WIN_SCORE*side;
					continue;
				}	
				
				if(evalue<=-Board.WIN_SCORE){
					if(max<=-Board.WIN_SCORE && egames>maxgames){
					    max = evalue;
				        maxgames = egames;
					    best = m;
					    best.score = -Board.WIN_SCORE;
					    //best.score = -Board.WIN_SCORE*side;
					}
					Log.println(m + " " + evalue/2 + " " +  egames);
					continue;
				}	
				
				m.score =   (int)(evalue*50);	
				double value = (gameAI.iMM)?entry.getIMValue(gameAI.alpha):evalue;// + 1/Math.sqrt(egames);
				Log.println(m + " " + 50*value + " " +  egames);
				if(value>max){
					max = value;
					maxgames = egames;
					best = m;						
				}
			}
	    }
		
	
		statistic();
   
		printPV(0);
		Log.println();
			
	       bord = copy_board.duplicate(bord);
	        
	        if (pv[0] != null && pv[1]!=null) {
	          
	            pv[0].apply(bord);
	    
	            toekomst[0]  = new OpeningBookBigEntry(bord);
	           
	            pv[0].undo(bord);
	            pv[0]=pv[1];
	            pv[1]=null;
	        }
		
	        if (Board.isTournament) 
	        	Log.saveScreen();
	        
	   duration=(System.currentTimeMillis()-hulp);
	   //Log.println("nps: " + countNodes/(duration/1000f));  
	   Log.println("gps: " + countGames/(duration/1000f));
	   Log.println("Time spent in search:" + (duration/1000f) +"s\r\n");     
	     
		
		return best;
	}
	
	protected void setTargetTime(int ply){
		
		
		super.setTargetTime(ply);
		
		setFixedTime(targetTime);		
	}
	
	 public void setFixedTime(long time){
		 
		super.setFixedTime(time);
		 
		for(int i =0; i < playerList.length; i++)
				playerList[i].setFixedTime(time);
	 }
	
	
	private void setFactorTime(double factor){
		
		setFixedTime((long)(targetTime*factor));
		
		//for(int i =0; i < playerList.length; i++)
		//	playerList[i].targetTime = (long) (playerList[i].targetTime*factor);	
	}

	private void statistic(){
		
		for(int i = 0; i < threadList.length; i++){
			countGames+=playerList[i].getCountGames();
			countNodes+=playerList[i].countNodes;			
		}
		
	
	}
	
	private boolean isStable(Board b){
		
		 Move move_list = generator.generateMoves(b, b.sideToMove);
		 double max = -Board.WIN_SCORE;
		 double maxgames =0;
		 Move best_move = null, max_move =null;
		
		 for(Move m = move_list; m!=null; m=m.next){
 	    	
		    	int egames =0;
		    	double evalue =0;
			    	
		    	PVMCRandProverPlayer.MCProofEntry entry = table.retrieve(m.hash(bord));
				
				if(entry==null)				
					continue;
				
				if(Math.abs(entry.value)<Board.WIN_SCORE){
				   evalue = (evalue*egames + entry.games*entry.value)/(entry.games);
				   egames = entry.games;
				}
				else{
					evalue = entry.value;
					egames = entry.games;
				}
				
				if(egames != 0){
							
					if(evalue>max){
						max = evalue;
					    best_move = m;	
					}
					
					if(egames>maxgames){
						maxgames = egames;
					    max_move = m;	
					}
											
				}
		    }
		
		 if(Math.abs(max)<Board.WIN_SCORE)			
		     return (best_move == max_move || max/2>=0.9);
		 
		 return true;
	}
	
	
	/*private void dfs(Board board, PVMCRandProverPlayer.MCTable table, int stamp, int ply, int maxPly)
	{	// Retrieve an entry so that it will be kept by pack().
		 PVMCRandProverPlayer.MCProofEntry entry = table.retrieve(board.hashValue);		  
		if ( entry == null  || ply >= maxPly ) { return; }
		
		//for ( int i=0; i<ply; ++i ) { System.out.print( ' ' ); }
		//System.out.println(  " " + ply + " : " +  entry.hashValue + " " + entry.visited  + " " + stamp );
		
		entry.stamp = stamp;
		
		Move list = generator.generateMoves(board, board.sideToMove);
		for ( Move  m = list; m != null; m=m.next ) {
			m.apply( board );
			dfs( board, table, stamp, ply+1, maxPly );
			m.undo(board);	  
		}
	}
	
	
	  /*private void remember(Board b, PVMCRandProverPlayer.MCTable oldTable, PVMCRandProverPlayer.MCTable newTable, int depth){
		  
		  
		  PVMCRandProverPlayer.MCProofEntry entry = oldTable.retrieve(b.hashValue);
		  
		  if(entry ==null)
			  return;
		  
		  newTable.storeBoard(newTable.storeDefBoard(b.hashValue), entry.value, entry.pbValue, entry.games);
		  
		  if(depth<=0)
			  return;
		  
		  Move list = generator.generateMoves(b, b.sideToMove);
		  
		  for(Move  m = list; m != null; m=m.next){
			  m.apply(b);
			  remember(b,  oldTable,  newTable,  depth -1);
			  m.undo(b);
			  
		  }
	  }*/

	
	private class ThreadSearch implements Runnable{
		
		private final PVMCRandProverPlayer player;
				
		public Move result;
		
		public ThreadSearch(PVMCRandProverPlayer p){
			
			player = p;		
		}
		
	
		public void run() {
					
			result = player.search(bord.duplicate(), table); 
			
			if(result.score*bord.sideToMove >= Board.WIN_SCORE)
				for(int i = playerList.length; --i>=0;){
					if(playerList[i]!=player)
						playerList[i].quitSearch();
				}
		}
	}

}
