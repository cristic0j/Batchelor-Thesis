import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class PVMCRandProverPlayer extends AbstractPlayer {
		
	protected static final int T = 5;//50;
	
	private final static double[] randomTable; 
	private final static double[] logTable;
	private final static int G_MASK = (1<<16)-1;
	protected double [] WTable;
	private int [] WeightTable;
	protected MCTable table; 
	
    private final long[]HistoryR = new long[(1 << Move.BIT_RANGE)];
    private final long[]HistoryB = new long[HistoryR.length]; 
    private final long[]totalB = new long[HistoryR.length];
    private final long[]totalR = new long[HistoryR.length];
	
	private double pvC, rC, rootC, allC, cutC;
	
	private boolean dynamicKnowledge = true;	
	
	//Default Bound. Initialized in method search based on the game.
	protected int bound = 1000;
	private Move rootMoves;
   
    private int gaussianCounter =0;
    
	private int countGames = 0;
	
	private MCResult result = new MCResult();
	private Move[] moveArray = new Move[2*Board.SQUARE];//100];
	
	
	public PVMCRandProverPlayer() {
		super();
	}
	
	public PVMCRandProverPlayer(Game game){
	
		gameAI = game;
		generator = gameAI.getGenerator();
		//table = new MCTable(gameAI.sizeTT);
		WTable = generator.getLOGRPPROB(); 
		
		WeightTable = new int[WTable.length];
		
		for(int i=WTable.length;--i>=0;){
			WTable[i] = Math.exp(WTable[i]);
						
			WeightTable[i] = (int)(1000*WTable[i]);	
		}
		
        initialiseDatabase();
	}
	
	public MCTable getTable(){
		
		return table;
	}
	
	final protected void printPV(Board b, int d){		

	   if(gameAI.checkOnDraw(b) == Board.REPETITION || b.numberOfMovesPlayed >= gameAI.getMaxLength()) {
		         return;
		   }	   
				
		Move move_list = generator.generateMoves(b);
		
		double maxvalue= Integer.MIN_VALUE;
		int games=0;
		
		Move best_move =null;
		
		for(Move m = move_list; m!=null; m=m.next){
			MCProofEntry entry = table.retrieve(m.hash(b));
			
			if(entry == null)
				continue;
		       
			double value = entry.value;// + 1/Math.sqrt(entry.games);// + entry.mxValue*0.01;
					 
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
	
		best_move.apply(b);
		printPV(b,d+1);
		best_move.undo(b);		
	}
	

	
	public void setSeed(int s){
	
	  gaussianCounter = s & G_MASK;
	}
	
	public Move search(Board b, MCTable t) {
		
		table = t;
		
		return search( b);
	}
	
	protected Move search(Board b, Move list) {
		
		rootMoves  = list;
		Move m = search(b); 		
		rootMoves = null;
		return m;
	}
	
	public Move search(Board b) {
		
		//NOTE: There is a bug when trying to simplify a Clobber position if simplifyBoard is
		//set to true
		//Board b2 = gameAI.getNewSimplifiedBoard(b);
		
		long start = System.currentTimeMillis();
		
		final int side = b.sideToMove;

	    Evaluator.parametrise(false);
	
	    Move savedRootMoves = rootMoves;   	   
    
		Game.randomize();		
			
		//teller =0;
		dynamicKnowledge = gameAI.isSolver; 
		
		pvC = gameAI.pvC;
		rC = gameAI.rC;
		allC = gameAI.allC;
		cutC = gameAI.cutC;
		rootC = gameAI.rootC;
		
		if(!gameAI.isRPS)
		for (int i = HistoryR.length; --i >= 0;) 
	            HistoryR[i] = HistoryB[i] = totalR[i] = totalB[i] = 1;
	            
	        
		
		seed = System.currentTimeMillis();	

		bound = (gameAI.isABPlayout)?(int)(0.85*gameAI.boundScore()):gameAI.boundScore();
		
	    initialiseDatabase();
	
		copy_board = (copy_board==null)?b.duplicate():b.duplicate(copy_board);
		bord = b;		
        
					
		countNodes = 0;
		egdbCounter=0;
		timeoutFlag = false;		
		
		//boolean keepTrack = true;
				
		countGames = 0;
		duration  = 0;
		
		// Move the computer is going to make, use a trivially-chosen move
        // as a starting point.		
		Move best =selectDecisiveMove(b,generator.generateMoves(b));
		
		setKillerMoves();
		
		if(best.score >= Board.WIN_SCORE){
			
			best.apply(b);
			table.storeBoard(table.storeDefBoard(b.hashValue), Board.WIN_SCORE, 1, 1, 1);
			best.undo(b);
			Log.print("Mate in one. ");
		    doSomeThing(); 
			return best;
		}
		   
	    
	   rootC = (targetTime<=20000 && !Board.isTournament)? rootC:1.4;
	    			
		//Log.print("Target Time: " + (targetTime / 1000f) + "s\r\n");
		
	    do{
			  if(savedRootMoves!=null){
	            	rootMoves = generator.copyMoveList(savedRootMoves);
	            }	  
		 
		  MCTS(b,0,new AtomicInteger(PV_NODE));
		  countGames+=result.games;
		  
		  if(side*result.score>Board.WIN_SCORE)
			  break;
		  
		  if(side*result.score<-Board.WIN_SCORE)
			  /*if(!neverEnd && !timeoutFlag){
				  Log.print("Position is lost, try to find the best line of defence!\r\n");
				  		  
				  table.clear();
				  dynamicKnowledge = false;
				  MCTS(b,0,new AtomicBoolean(true));
				  keepTrack = false;
				  continue;			  
			  }else */
			  			  {
			  
			     //Log.print("Position is lost!\r\n");
				  break;
			  }
	
			if((getCountGames()&63)==0)
				duration = System.currentTimeMillis()-start;
			
		 }while(getCountGames()<GAMES_LIMIT ||(!this.timeoutFlag && (duration < targetTime || neverEnd) && countNodes <= maxNodes));  
					  
	    doSomeThing();
	    
	    	    
	    double max = Double.NEGATIVE_INFINITY;
		
		  Move list = generator.generateMoves(b, side);
		  
			for(Move m = list; m!=null; m=m.next){
			
				MCProofEntry entry = table.retrieve(m.hash(b));
				
				if(entry != null){
												  				
					m.score = (int)(entry.value*50);	
					
					if(entry.value>=Board.WIN_SCORE){
						//Log.println(m + " " + entry.value/2 + " " +  entry.games +  " " );
						best=m;
						best.score = Board.WIN_SCORE*side;
						max = Board.WIN_SCORE;
					}	
					else{
						double value = entry.value + 1/Math.sqrt(entry.games);// + entry.mxValue*0.01;	
			
						if(value>max){
							max = value;
							best = m;						
						}
					}
				}
			//	else
				//	Log.println("Not found: " + m + " " +  table.retrieveGames(m.hash(b)));
							
			}
		
	
	 //   Log.println(best + " N " +  getCountGames() + " v " +best.score + " n ");
		//Log.println("nps: " + countNodes/(duration/1000f));
	// 	Log.println("gps: " + getCountGames()/(duration/1000f));
		
		//printPV(b,0);
		
       /* b = copy_board.duplicate(b);
        
        if (keepTrack && pv[0] != null && pv[1]!=null) {
          
            pv[0].apply(b);
    
            toekomst[0]  = new OpeningBookBigEntry(b);
           
            pv[0].undo(b);
            pv[0]=pv[1];
            pv[1]=null;
        }
        else{
        	
        	toekomst[0]  = null;
        	toekomst[1]  = null;
        }*/
				
		//Log.println();
		///Log.println("teller: "+teller);
		
		 if(gameAI.isEGDB)
	        	Log.println("EGDB Hits " + egdbCounter);
		//Log.println("");
		Evaluator.parametrise(true);
					
		return best;
	}
		
//MCTS
private void MCTS(Board b, final int height, AtomicInteger nodeType){
					
	//countNodes++;	
		  
	final int side = b.sideToMove;
	
	final int times = gameAI.checkOnDraw(b);
	
    if(times == Board.REPETITION || b.numberOfMovesPlayed >= gameAI.getMaxLength()) {
  	 // result.setValues(1,Board.DRAW_SCORE);
  	  countNodes++;
  	  result.setValues(1,1);
  	 // teller++;
  	
	    return;
	   }
	
	MCProofEntry parent = table.retrieve(b.hashValue);
	
	if(parent!=null && dynamicKnowledge){
		if(parent.value >=Board.WIN_SCORE){
			countNodes++;
			result.setValues(1, (side == Board.B_SIDE) ? (-Board.WIN_SCORE-1): Board.WIN_SCORE+1);
		    return;
		}
		
		if(parent.value <=-Board.WIN_SCORE){
			countNodes++;
			result.setValues(1, (side == Board.B_SIDE) ? Board.WIN_SCORE+1: (-Board.WIN_SCORE-1));
		    return;
		}
		
		if( parent.drawValue == MCProofEntry.DRAW){
			countNodes++;
			 result.setValues(1,Board.DRAW_SCORE);
		    return;
		}
	}
		  
      final int Check_for_Win = (height==0)?gameAI.terminalEvaluation(b, -side) : gameAI.gameTheoreticEvaluation(b, -side);
	  
      if(dynamicKnowledge){
		  if (Check_for_Win >= Board.WIN_SCORE) {
			  countNodes++;
			  result.setValues(1, Check_for_Win+1);
		    	  return;
		  } 

	  if (Check_for_Win <= -Board.WIN_SCORE) {
		  countNodes++;
		  result.setValues(1, Check_for_Win-1);
	    	return;
		
	  }
      }else{    	 
    		if(Check_for_Win >=Board.WIN_SCORE){
    			countNodes++;
    			  result.setValues(1, 2);
				return;
			}
			else	if((Check_for_Win <=-Board.WIN_SCORE)){
				countNodes++;
				  result.setValues(1, 0);
				return;
			}
          }
      
      if(egdb!=null && gameAI.hasDatabase(b) && height>0){   	   
    	  
       	  try {
       		int info = egdb.getInfo(b);
  		  
       		if(info!=0){
       		 egdbCounter++;
       		       	
       		if(info==1){
       			result.setValues(1,side*(Board.WIN_SCORE + ((dynamicKnowledge)?1:0)));
 	  	    	 return;
       		}
       			else
       				if(info==2){
       				  result.setValues(1,-side*(Board.WIN_SCORE+((dynamicKnowledge)?1:0)));
       	  	    	  return;       					
       				}  
       				else {
       						result.setValues(1,1);
       						return;
           				}       		
       		}
       	} catch (IOException e) {
       		e.printStackTrace();
       	}
   }
    	  	  
	  if (Check_for_Win == Board.DRAW_SCORE){            	
		  //result.setValues(1,1);
		  countNodes++;
		  result.setValues(1,Board.DRAW_SCORE);
		  return;
      } 
	  	          
	    Move move_list= (rootMoves!=null && height ==0)?rootMoves:generator.generateMoves(b, side);

	    Move best_move;
	    	
		if(move_list==null){
			
			Move.applyNullMove(b);
			result.score = monteCarloPlayout(b);
			Move.undoNullMove(b);
			
			result.games = 1;
			
			if((result.score >=Board.WIN_SCORE)){
				result.score = 2;
				return;
			}
			else	if((result.score <=-Board.WIN_SCORE)){
				result.score =0;
				return;
			}
			else result.score =1;
						
		}
		else
		{								
			if((height >0) && parent.games<T){
		        best_move = selectRandomMove(b, move_list);
			}
			else{
				best_move = (nodeType.get()==PV_NODE)?selectPVMove(b, move_list, nodeType,height):((gameAI.isST)?selectSTMove(b, move_list):selectPBMove(b, move_list,nodeType));    	  			
			}
			
			
		    best_move.apply(b);
			MCProofEntry entry = table.retrieve(b.hashValue);
									
			if(entry == null){
				entry =table.storeDefBoard(b.hashValue);	
			    result.games = 1;
				double value =0;
				int eval =0;
				
				int backup;
								
				do{        
			     //Playout starts here
				 backup  = monteCarloPlayout(b);
				 
					if( dynamicKnowledge && backup * side==Board.WIN_SCORE+1){
						   if(backup <-Board.WIN_SCORE)
							   result.score  = -Board.WIN_SCORE-1;
								else result.score = Board.WIN_SCORE+1;
						   
						 value=Board.WIN_SCORE;  //teller++;	

						 break;
							}
					else
				if(dynamicKnowledge && backup * side>=-Board.WIN_SCORE-2 && backup * side < -Board.WIN_SCORE){
				   if(backup <-Board.WIN_SCORE)
					   result.score  = -Board.WIN_SCORE-1;
						else result.score = Board.WIN_SCORE+1;
				   
				 value=-Board.WIN_SCORE;  //teller++;	

				 break;
					}
				else{
			
					if(backup==Board.DRAW_SCORE)
						result.score =1;
					else
						if(backup >=0){
						result.score =2;
					}
					else	
						result.score =0;	
						
				eval = (gameAI.iMM)?(gameAI.isMMHybrid?rememberScore : (-side*gameAI.heuristicEvaluation(b,side))):0;
					
				value = (side == Board.B_SIDE)?(result.score)/(result.games):((result.games*2-result.score))/(result.games);
				}
				
				}while(false);
				
							
				best_move.undo(b);
				table.storeBoard(entry, value, gameAI.PBW*WTable[best_move.getMoveType(b)],  eval, result.games);	
				this.updateHH(side, best_move, result.score);
			}
			else{							
				
			   MCTS(b,height+1,nodeType);				

				double average;
				int score=0;				
			
				if(result.score * side>Board.WIN_SCORE){
					  average=Board.WIN_SCORE;
					}
				else	if(result.score * side<-Board.WIN_SCORE){
					average=-Board.WIN_SCORE;					
					}
				else
				 if(result.score == Board.DRAW_SCORE){
						score =1;
						entry.drawValue = MCProofEntry.DRAW;
						average = 1;// (entry.value*entry.games+score)/(entry.games+result.games);
				     } 
				 else{
					 score = (side == Board.B_SIDE)?result.score:(result.games*2-result.score);
					 average = ((entry.value*entry.games+score))/(entry.games+result.games);
				}

				table.storeBoard(entry, average, entry.games+result.games);
				best_move.undo(b);		
				this.updateHH(side, best_move, result.score);
			}	
			
			if(gameAI.iMM){
				double max = Double.NEGATIVE_INFINITY;
				for(Move m = move_list; m !=null; m=m.next){
					MCProofEntry e = table.retrieve(m.hash(b));
				    final double value=(e==null)? parent.eValue:e.mmValue;
					if(value>max)
						max=value;
				}
				 parent.mmValue=2-max;
				}		
					
			if(result.score*side<-Board.WIN_SCORE){				
			   parent.lossCounter++;
				boolean check2ply = (gameAI.isTwoPlyCheck && parent.lossCounter==1);
				for(Move m = move_list; m !=null; m=m.next){
					MCProofEntry e = table.retrieve(m.hash(b));
					if(e==null){
					
						if(check2ply){
							m.apply(b);
							int score= -selectReplyProveMove(b,Board.WIN_SCORE);
							m.undo(b);
							if(score<=-Board.WIN_SCORE)
								continue;							
						}
								
						result.score= (result.score<-Board.WIN_SCORE)?0:2;
						result.games = 1;
						break;}
					
		        	else if( e.value>=0){
		        		
		        			if(e.value>=Board.WIN_SCORE){
								result.score= side*(Board.WIN_SCORE+1);
								result.games = 1;
								break;
		        			}else{
								result.score= (result.score<-Board.WIN_SCORE)?0:2;
								result.games = 1;
								break;
		        				}
		        	}	
				}
			}
			else 
			  if(result.score==Board.DRAW_SCORE){
				
				for(Move m = move_list; m !=null; m=m.next){
					MCProofEntry e = table.retrieve(m.hash(b));
					if(e==null || (e.drawValue != MCProofEntry.DRAW && e.value>=0)){
						result.score= 1;
						result.games = 1;
						parent.drawValue  = MCProofEntry.AT_LEAST_DRAW;
						break;}
				}	
				
			}
				
		}
		
	
		
		generator.recycle(move_list);			
	}
	
	
	
	final protected int monteCarloPlayout(Board b){
				
		final int times = gameAI.checkOnDraw(b);
		
	      if(times == Board.REPETITION)
	    	  return Board.DRAW_SCORE;
						
		return monteCarloPlayout(b,1);// (randomMCValue()))
	}
	
	private int rememberScore;

	
	private int monteCarloPlayout(Board b,int pathlength){
		
		//countNodes++;
		
	      if(b.numberOfMovesPlayed >= gameAI.getMaxLength()-10) {
	    	  
		         return Board.DRAW_SCORE;
		   }	      
	      
		  final int side = b.sideToMove;  
	      final int Check_for_Win =  gameAI.gameTheoreticEvaluation(b, -side);
		                    
		  if (Check_for_Win >= Board.WIN_SCORE) {
			  return Check_for_Win + pathlength;
		  } 

		  if (Check_for_Win <= -Board.WIN_SCORE) {
			  return Check_for_Win- pathlength;
		            }  
		  
		  if (Check_for_Win == Board.DRAW_SCORE) {            	
			  return Check_for_Win;
	      } 
		  
		   if(egdb!=null && gameAI.hasDatabase(b)){   	   
		    	  		       	 
		       	  try {
		       		int info = egdb.getInfo(b);
		  		       		if(info!=0){
		       		 egdbCounter++;
		       		       	
		       		if(info==1){
		       		 return side*(Board.WIN_SCORE);
		       		}
		       			else
		       				if(info==2){
		       					return -side*(Board.WIN_SCORE);
		       					
		       				}  
		       				else {
		           					return	Board.DRAW_SCORE;
		           				}
		       		
		       		}
		       	} catch (IOException e) {
		       		e.printStackTrace();
		       	}
		   }
		   
		   if(gameAI.fastMCImpl()){
				countNodes++;
						return gameAI.monteCarlo(b);
			}
		   
		   if(gameAI.isCutoff && !(gameAI.isABPlayout || gameAI.isGreedyPlayout) && ((pathlength+1)%3==0 || pathlength >=gameAI.playoutLength)){
				
			   final int value = gameAI.heuristicEvaluation(b, -side);
				
				if(value>bound){
					
					return Board.WIN_SCORE + pathlength+100;}
				else if(value<-bound){
					
					return -Board.WIN_SCORE - pathlength-100;}
			}	
		  	          
		    final Move move_list = generator.generateMoves(b, side);
					
			final int backupvalue;
			Move best_move;
			
			if(move_list==null){  
				Move.applyNullMove(b);
				backupvalue = monteCarloPlayout(b,pathlength+1);
				Move.undoNullMove(b);
				
				return backupvalue;
			}
			else
			{	     				
				if(pathlength == 1){
		
			        best_move = selectPlayoutMove(b, move_list, pathlength);//(gameAI.isMMHybrid)?selectPlayoutMove(b, move_list, pathlength):selectCorrective4Move(b, move_list);
			        
			        rememberScore = best_move.score;
				         
			         if(best_move.score >= Board.WIN_SCORE)
			            {
			                generator.recycle(move_list);
			                return side != Board.B_SIDE ? -Board.WIN_SCORE-1-pathlength : Board.WIN_SCORE+1+pathlength;
			            }		         
			        } else
			        {
			        	
			        if(!(gameAI.isABPlayout || gameAI.isGreedyPlayout))	
			        	 best_move = selectRandomMove(b, move_list);
			        else{
			        	best_move = selectPlayoutMove(b, move_list, pathlength);			        
			  
			    		if(best_move.score >= Board.WIN_SCORE)
			            { 
			                generator.recycle(move_list);
			                return side != Board.B_SIDE ? -Board.WIN_SCORE-1-pathlength : Board.WIN_SCORE+1+pathlength;
			            }
			   		  
			    	//Cut-0ff Takes place here!
			  		  if(gameAI.isCutoff){
			  			  
			  			  final int bnd =(pathlength >=gameAI.playoutLength)?0:bound;
														
							if( best_move.score>bnd){	
								generator.recycle(move_list);							
								return side*(Board.WIN_SCORE + pathlength+100);}
							else if( best_move.score<-bnd){
								generator.recycle(move_list);
								return side*(-Board.WIN_SCORE - pathlength-100);}
							
						}			            
			        }
			     }
					
				best_move.apply(b);
				backupvalue = monteCarloPlayout(b,pathlength+1);	
				best_move.undo(b);		
				
				this.updateHH(side, best_move, result.score);
			}
					
			generator.recycle(move_list);
				
			return backupvalue;
		}	

		
	protected final Move selectRandomMove(Board b, Move list){
	
	 //return (gameAI.isRPS)?((gameAI.isFutility && !gameAI.isLMR)?this.selectRPSMove(b, list):(gameAI.isLazy?selectGreedyMove(b,list):this.selectCorrective3Move(b, list))):this.selectRealRandomMove(b, list);
 // return (gameAI.isRPS)?(!(gameAI.isABPlayout || gameAI.isGreedyPlayout)?this.selectRPSMove(b, list):this.selectCorrective3Move(b, list)):selectDecisiveMove(b, list);//this.selectRealRandomMove(b, list);
		return (!(gameAI.isABPlayout || gameAI.isGreedyPlayout)?this.selectRWMove(b, list):this.selectCorrective3Move(b, list));
	}


	private Move selectPlayoutMove(Board b, Move list, int pathlength){
		
	//	 return (gameAI.isRPS)?(gameAI.isABPlayout?selectTwoPlyMove(b,list):selectGreedyMove(b,list)):selectEpsilonGreedyMove(b,list);
		 return (gameAI.isABPlayout?selectTwoPlyMove(b,list):selectGreedyMove(b,list));
}
		
	
   protected Move selectPBMove(Board b, Move list,AtomicInteger nodeType){
		
	   
	    rC=(nodeType.get()==CUT_NODE)?cutC:allC;
		nodeType.set(-nodeType.get());
		
		return selectPBMove(b, list);
	}	
   
   protected final Move selectPBMove(Board b, Move list){
	   
		int nsum=0;
		double  max = Double.NEGATIVE_INFINITY;
		double value;
		Move  best_move = null;	
		
		
		for(Move m = list; m!=null; m=m.next){
			 nsum +=  table.retrieveGames(m.hash(b));
	  }		
		
		final double	CLnNSum = rC*log(nsum);	
		final int side = b.sideToMove;
				
		for(Move m = list; m!=null; m=m.next){
			MCProofEntry entry = table.retrieve(m.hash(b));
			
			//final double amaf = 4*((gameAI.isFutility)?AMAF(m,bord.sideToMove):0);		
		 
			if(entry==null){
				value = 2+gameAI.PBW*((gameAI.isRPS)?(WTable[m.getMoveType(b)]):getRelHH(b.sideToMove,m)); 
		}
			else{				
				value = entry.value;
				
				if(value >= Board.WIN_SCORE)
						return m;					
					
				value = computeValue(entry, CLnNSum, m, side);	 
			}
						
			if(value > max){
					max = value;
					best_move = m;
				}
		}
	   
		 return best_move;
   }
   
   private Move selectSTMove(Board b, Move list){
	   
	   
 		int nsum=0;
 		double  max = Double.NEGATIVE_INFINITY;
 		double value;
 		Move  best_move = null;	 		
 		
 		for(Move m = list; m!=null; m=m.next){
 			MCProofEntry entry = table.retrieve(m.hash(b));
 			if(entry==null)
 				continue;

 			nsum+=entry.games;
 			value = entry.value;
 			if(value > max){
 			max = value;
 			} 			
 		}
 		
 		final double	CLnNSum = (max>=1.7 && nsum>30)?0: rC*log(nsum);	
 		final int side = b.sideToMove;
 		
 		max = Double.NEGATIVE_INFINITY;
 		for(Move m = list; m!=null; m=m.next){
 			MCProofEntry entry = table.retrieve(m.hash(b));
 					
 			//final double amaf = 4*((gameAI.isFutility)?AMAF(m,bord.sideToMove):0);		
 		 
 			if(entry==null){
 				value = 2+gameAI.PBW*((gameAI.isRPS)?(WTable[m.getMoveType(b)]):getRelHH(b.sideToMove,m)); 
 		}
 			else{				
 				value = entry.value;
 				
 				if(value >= Board.WIN_SCORE)
 						return m;					
 					
 				value = computeValue(entry, CLnNSum,m,side);	   
 			}
 						
 			if(value > max){
 					max = value;
 					best_move = m;
 				}
 		}
 		
 		
 	   return best_move;
 	}


protected Move selectPVMove(Board b, Move list, AtomicInteger nodeType, int height){
	
	int nsum=0;
	double  max = Double.NEGATIVE_INFINITY,  pvmax = Double.NEGATIVE_INFINITY;
	double value;
	Move  pv_move=null, best_move = null;			
	
	for(Move m = list; m!=null; m=m.next){
		 nsum +=  table.retrieveGames(m.hash(b));
  }		
	
	final double	CLnNSum = ((height ==0)?rootC:pvC)*log(nsum);	
	final int side = b.sideToMove;
	
	for(Move m = list; m!=null; m=m.next){
		MCProofEntry entry = table.retrieve(m.hash(b));
		
		//final double amaf = 4*((gameAI.isFutility)?AMAF(m,bord.sideToMove):0);		
	 
		if(entry==null){
			value = 2+gameAI.PBW*((gameAI.isRPS)?(WTable[m.getMoveType(b)]):getRelHH(b.sideToMove,m)); 
	}
		else{				
			value = entry.value;
			
			if(value >= Board.WIN_SCORE)
					return m;	
			
			if(value>pvmax){
				pvmax=value;
				pv_move =m;
			}	
				
			value = computeValue(entry, CLnNSum, m, side);	     
		}
					
		if(value > max){
				max = value;
				best_move = m;
			}	
	}
	
	if(pv_move!=best_move)
		nodeType.set(CUT_NODE);
	
   return best_move;
}	


private double computeValue(MCProofEntry entry, double CLnNSum, Move m, int side){
	   
	   double value = entry.value;
	   
	   double L = (1 +(2*entry.games-entry.games*value));
	   
	   if(gameAI.iMM)
			value = entry.getIMValue(gameAI.alpha);
	   
	  final double bias = (gameAI.isRPS)?(entry.pbValue):getRelHH(side,m);
	   
	   if(gameAI.isUCTClassic)
		   return value + Math.sqrt(CLnNSum/entry.games) + bias/(Math.sqrt(L));
	   else 
		   return value+ Math.sqrt(CLnNSum/L) + bias/(Math.sqrt(L))+randomMCValue()*(0.10);   
	
}

	
	private double randomMCValue(){
		
		gaussianCounter = (gaussianCounter +1)& G_MASK ;
		 return randomTable[gaussianCounter];
	}
		
		
	private Move selectRWMove(Board b, Move list){
		
		int U=0;
		for(Move m = list; m!=null; m=m.next){
		  U+=m.score = getMoveWeight(b, m);	  
		}
		
		U = (int)(U*randomMCValue());
		
		for(Move m = list; m!=null; m=m.next){
			U-=m.score;
			if(U<=0)
				return m;
			
		}
		
		return list;
	}
		
/////////This is the  Corrective  Strategy
	private Move selectCorrective3Move(Board b, Move list){
		
		int U=0;
		final int side = b.sideToMove;
		
		int def = side*gameAI.evaluate(b, -side);
		for(Move m = list; m!=null; m=m.next){
			
			m.apply(b);
			int value =side*gameAI.evaluate(b, side);
			
			m.undo(b);
			if(value>=Board.WIN_SCORE){
				m.score = value;
				return m;}
			
						
			if(value<=def){
				U+=m.score=1;
				//teller++;
				
			}
		 else
		  U+=m.score=getMoveWeight(b,m);
		  	
		}
		
		U = (int)(U*randomMCValue());
		
		for(Move m = list; m!=null; m=m.next){
			U-=m.score;
			if(U<=0)
				return m;
			
		}
		
		return list;
	}	
			
/////////This is the  Corrective  Strategy. Difference with the previous one is the nodeCounter
///////// Executed in a leaf node. 
@SuppressWarnings("unused")
private Move selectCorrective4Move(Board b, Move list){
		
		int U=0;
		final int side = b.sideToMove;
		countNodes++;
		
		int def = side*gameAI.evaluate(b, -side);
		for(Move m = list; m!=null; m=m.next){
			
			m.apply(b);
			int value =side*gameAI.evaluate(b, side);
				
			m.undo(b);
			if(value>=Board.WIN_SCORE){
				m.score = value;
				return m;}
			
						
			if(value<=def){
				U+=m.score=1;
								
			}			
			else
		      U+=m.score=getMoveWeight(b,m);
		}
		
		U = (int)(U*randomMCValue());
		
		for(Move m = list; m!=null; m=m.next){
			U-=m.score;
			if(U<=0)
				return m;
			
		}
		
		return list;
	}	


/*private Move selectCorrectiveTwoPLyMove(Board b, Move list){
	
	int U=0;
	final int side = b.sideToMove;
	
	int best_value= -Board.WIN_SCORE;
	countNodes++;	
	
	int def = side*gameAI.evaluate(b, -side);
	for(Move m = list; m!=null; m=m.next){
		
		m.apply(b);
		int value;
		if(best_value <=-Board.WIN_SCORE){
			value =-selectReplyProveMove(b,-best_value);
			if(value>best_value)
				best_value = value;
		}
		else
            value = side*gameAI.evaluate(b, side);
		
		
		m.undo(b);
		if(value>=Board.WIN_SCORE){
			m.score = value;
			return m;}
		
					
		if(value<=def){
			U+=m.score=1;
							
		}			
		else
	  {U+=m.score=WeightTable[m.getMoveType(b)];
	  }	
	}
	
	if(best_value <=-Board.WIN_SCORE){
		list.score=-Board.WIN_SCORE;
		return list;}
	
	U = (int)(U*randomMCValue());
	
	for(Move m = list; m!=null; m=m.next){
		U-=m.score;
		if(U<=0)
			return m;
		
	}
	
	return list;
}*/


private int selectReplyProveMove(Board b, int beta){	
	
	/*if(gameAI.epsilonGreedy && randomMCValue() <=0.05)
	  return selectKnowledgeMemRandomMove( b, list);*/
      
		final int side = b.sideToMove;
		int max = side*gameAI.gameTheoreticEvaluation(b, -side); //side*gameAI.evaluate(b, -side);
		
		if(max<=-Board.WIN_SCORE || max >=Board.WIN_SCORE || max == side*Board.DRAW_SCORE)
			return max;

		max = Integer.MIN_VALUE;
		Marker++;
		final int pathlength=b.numberOfMovesPlayed;
		
	   if(pathlength<KillerMove.length)	
		for(int i=0; i<KillerMove[0].length; i++){	
		 Move kill = generator.generateIncMove(b, side, KillerMove[pathlength][i], null, -1, false);
					 
		 if(kill != null){
			 
			    kill.apply(b);
				int value =  side*gameAI.gameTheoreticEvaluation(b, side);
				kill.undo(b);
				
				if(value >= beta){
					
					 if (pathlength<KillerMove.length && !KillerMove[pathlength][0].equals(kill)) { 
						   KillerMove[pathlength][1].copy(KillerMove[pathlength][0]);
				            KillerMove[pathlength][0].copy(kill);
				        }
					generator.recycle(kill);
					  
					return value;}		
	

				if(value>max){
					
					max = value;	
			}
				 
			   
			    MarkedMoves[kill.index]=Marker;
				generator.recycle(kill);
				  			 
		 }
	}
		Move list = generator.generateUnMarkedMoves(b, side,
                MarkedMoves, Marker);
								
		int counter =0;
	     for(Move m = list; m!=null; m=m.next){
			  m.score=getMoveWeight(b,m);
			  moveRepArray [counter++] = m;
		 }     
	     
	 	  final int cutOff = counter;
		
	  	for(int i = 0; i < cutOff; i++){	
	  		
	  		int maxValue = moveRepArray[i].score;
	  		int maxIndex = i;
	  		
	  	 	for(int j = i+1; j< counter; j++){
	  	 		
	  	 		if(maxValue<moveRepArray[j].score){
	  	 			maxValue = moveRepArray[j].score;
	  	 			maxIndex = j;
	  	 		}
	  	 		
	  	 	}
	  		
	  		Move m = moveRepArray[maxIndex];	
	  			  		  			
					m.apply(b);
					int value =  side*gameAI.gameTheoreticEvaluation(b, side);
					m.undo(b);
					
					if(value >= beta){
						
						  if(pathlength<KillerMove.length) { 
							   KillerMove[pathlength][1].copy(KillerMove[pathlength][0]);
					            KillerMove[pathlength][0].copy(m);
					        }
						  
						  generator.recycle(list);
						return value;}		
		
				
					if(value>max){
						
						max = value;	
				}
					
					moveRepArray[maxIndex] = moveRepArray[i];
	  	}
	  	

		generator.recycle(list);
				
		if(max>-Board.WIN_SCORE && max <Board.WIN_SCORE && max == side*Board.DRAW_SCORE)
			return side*gameAI.heuristicEvaluation(b, -side);
		
		return max;			
	}	

/*private Move selectReplyProveMorePlyMove(Board b, Move list){
	
	int U=0;
	final int side = b.sideToMove;
	
	int alpha= -Board.WIN_SCORE;
	int beta = Board.WIN_SCORE;
	countNodes++;
	
	int def = side*gameAI.evaluate(b, -side);
	
	for(Move m = list; m!=null; m=m.next){
		
		m.apply(b);
		int value =-selectReplyProveMove(b,-beta,-alpha,2,0);
		
		if(value==0)
			value = side*gameAI.heuristicEvaluation(b, side);
		
		m.undo(b);
		
		if(value>=beta){
			m.score = value;
			return m;}
		
		if(value>alpha)
			alpha=value;
		
					
		if(value<=def){
			U+=m.score=1;							
		}			
		else
	  {U+=m.score=WeightTable[m.getMoveType(b)];
	  }	
	}
	
	if(alpha <=-Board.WIN_SCORE){
		list.score=-Board.WIN_SCORE;
		return list;}
	
	U = (int)(U*randomMCValue());
	
	for(Move m = list; m!=null; m=m.next){
		U-=m.score;
		if(U<=0)
			return m;
		
	}
	
	return list;
}*/

/*private int selectReplyProveMove(Board b, int alpha, int beta, int depth, int height){	
      
	final int side = b.sideToMove;
	final int score = side*gameAI.gameTheoreticEvaluation(b, -side);

	if(depth<=0)
		return score;
		
	if(score<=-Board.WIN_SCORE || score >=Board.WIN_SCORE || score == side*Board.DRAW_SCORE)
			return score;
		
		
	int max = Integer.MIN_VALUE;
				
	final long markCount = ++Marker;
	final int pathlength=b.numberOfMovesPlayed;
		
	if(pathlength<KillerMove.length)	
		for(int i=0; i<KillerMove[0].length; i++){	
	   
		 Move kill = generator.generateIncMove(b, side, KillerMove[pathlength][i], MarkedMoves,markCount, false);
					 
		 if(kill != null){
			 
			 kill.apply(b);
			 int value =  -selectReplyProveMove(b, -beta, -alpha, depth-1, height+1);
			 kill.undo(b);
				
			 if(value >= beta){
				if (pathlength<KillerMove.length && !KillerMove[pathlength][0].equals(kill)) { 
						   KillerMove[pathlength][1].copy(KillerMove[pathlength][0]);
				            KillerMove[pathlength][0].copy(kill);
				}
				generator.recycle(kill);
				return value;
			 }		
	
			if(value>max){				
					max = value;
				if(value>alpha)
					alpha =value;	
			}
	         
			   
		MarkedMoves[kill.index]=markCount;
	    generator.recycle(kill);
	  }
	}
		Move list = generator.generateUnMarkedMoves(b, side, MarkedMoves, markCount);
	
		Move [] mArray = moveSaveArray[height];
		
		int counter =0;
	     for(Move m = list; m!=null; m=m.next){
			  m.score=WeightTable[m.getMoveType(b)];
			  mArray [counter++] = m;
		 }     
	     		
	  	for(int i = 0; i < counter; i++){	
	  		
	  		int maxValue = mArray[i].score;
	  		int maxIndex = i;
	  		
	  	 	for(int j = i+1; j< counter; j++){
	  	 		
	  	 		if(maxValue<mArray[j].score){
	  	 			maxValue = mArray[j].score;
	  	 			maxIndex = j;
	  	 		}	  	 		
	  	 	}
	  		
	  		Move m = mArray[maxIndex];	
	  		m.apply(b);
			int value =  -selectReplyProveMove(b, -beta, -alpha, depth-1,height+1);
			m.undo(b);
					
			if(value >= beta){
				generator.recycle(list);
				  if (pathlength<KillerMove.length && !KillerMove[pathlength][0].equals(m)) { 
					   KillerMove[pathlength][1].copy(KillerMove[pathlength][0]);
				        KillerMove[pathlength][0].copy(m);
				  }
			     return value;}		
		
    		if(value>max){
				max = value;	
				if(value>alpha)
					alpha = value;
			}
					
			mArray[maxIndex] = mArray[i];
	  	}
	  	
		generator.recycle(list);
		return max;			
	}
*/

	
/////////This is the Greedy PlayoutStrategy
	private Move selectGreedyMove(Board b, Move list){	
		
		/*if(gameAI.epsilonGreedy && randomMCValue() <=0.05)
		  return selectKnowledgeMemRandomMove( b, list);*/
			
			final int side = b.sideToMove;
			int max = Integer.MIN_VALUE;
			Move best = null;
								
			int counter =0;
		
		     for(Move m = list; m!=null; m=m.next){
				  m.score=getMoveWeight(b,m);
				  moveArray [counter++] = m;
			 }     
				
		    final int cutOff = (counter < 7) ? counter: 7;
			
		  	for(int i = 0; i < cutOff; i++){	
		  		
		  		int maxValue = moveArray[i].score;
		  		int maxIndex = i;
		  		
		  	 	for(int j = i+1; j< counter; j++){
		  	 		
		  	 		if(maxValue<moveArray[j].score){
		  	 			maxValue = moveArray[j].score;
		  	 			maxIndex = j;
		  	 		}
		  	 		
		  	 	}
		  		
		  		Move m = moveArray[maxIndex];	  		
		  			
						m.apply(b);
						int value = m.score = side*gameAI.evaluate(b, side);
						m.undo(b);
						
						if(value > bound){
							
							return m;}		
			
					
						if(!Game.isRandom)
							value+=this.randomMCValue()*64;

						if(value>max){
							best = m;
							max = value;	
					}
						
						moveArray[maxIndex] = moveArray[i];
		  	}
		  	
			for(int i = cutOff; i < counter; i++){
				Move m = moveArray[i];
				m.apply(b);
				final int value =m.score = side*gameAI.gameTheoreticEvaluation(b, side);
					
				m.undo(b);
				if(value>=Board.WIN_SCORE){
					
					return m;}		
			}
				
			return best;			
		}	
	
	
////////This is the Greedy PlayoutStrategy
@SuppressWarnings("unused")
private Move selectEpsilonGreedyMove(Board b, Move list){	
	
	if( randomMCValue() <=0.05)
	  return selectDecisiveMove( b, list);
		
		final int side = b.sideToMove;
		int max = Integer.MIN_VALUE;
		Move best = null;
							
		for(Move m = list; m!=null; m=m.next){	
	  	  	m.apply(b);
			int value = m.score = side*gameAI.evaluate(b, side);
			m.undo(b);
					
			if(value > bound)
				return m;		
	
			if(!Game.isRandom)
				value+=this.randomMCValue()*64;

				if(value>max){
					best = m;
					max = value;	
				}
					
	  	}  	

		return best;			
	}	
	
private Move selectTwoPlyMove(Board b, Move list){	
	
	/*if(gameAI.epsilonGreedy && randomMCValue() <=0.05)
	  return selectKnowledgeMemRandomMove( b, list);*/
		
		final int side = b.sideToMove;
		int max = Integer.MIN_VALUE;
		int alpha = -bound;
		Move best = null;
							
		int counter =0;
	
	     for(Move m = list; m!=null; m=m.next){
			  m.score=getMoveWeight(b,m);
			  moveArray [counter++] = m;
		 }     
			
	   // final int cutOff = (counter < 7) ? counter: 7;
	   final int cutOff = counter;	     
	     
	  	for(int i = 0; i < cutOff; i++){	
	  		
	  		int maxValue = moveArray[i].score;
	  		int maxIndex = i;
	  		
	  	 	for(int j = i+1; j< counter; j++){
	  	 		
	  	 		if(maxValue<moveArray[j].score){
	  	 			maxValue = moveArray[j].score;
	  	 			maxIndex = j;
	  	 		}	  	 		
	  	 	}
	  		
	  		Move m = moveArray[maxIndex];	  		
	  			
					m.apply(b);
					int value;
					if(i>=7){
						value= m.score = side*gameAI.evaluate(b, side);
						if(value>alpha)
							value= m.score = //-selectReplyMove(b,-bound,-alpha,2,0); 
						-selectReplyMove(b,-alpha);
						
					}
					else
			            value= m.score = -selectReplyMove(b,-alpha);
					
					m.undo(b);
					
					if(value > bound){
						
						return m;}		
		
					if(value>max){
						best = m;
						max = value;
						if(value>alpha)
						   alpha = value;
				}
					
					moveArray[maxIndex] = moveArray[i];
	  	}
	  	
		for(int i = cutOff; i < counter; i++){
			Move m = moveArray[i];
			m.apply(b);
			final int value =m.score = side*gameAI.gameTheoreticEvaluation(b, side);
				
			m.undo(b);
			if(value>=Board.WIN_SCORE){
				
				return m;}		
		}
			
		return best;			
}	

private Move[] moveRepArray = new Move[moveArray.length];//[100];
//private Move[][] moveSaveArray = new Move[3][100];
//private Move[][] moveSaveArray = new Move[4][100];
private Move KillerMove[][] = new Move[100][2];
private long Marker;
private final long[] MarkedMoves= new long[(1 << Move.BIT_RANGE)];

protected final void  setKillerMoves(){

for (int i = KillerMove.length; --i >= 0;) {
    KillerMove[i][0] = new Move();
    KillerMove[i][1] = new Move();
	} 
}

private int selectReplyMove(Board b, int beta){	
	
	/*if(gameAI.epsilonGreedy && randomMCValue() <=0.05)
	  return selectKnowledgeMemRandomMove( b, list);*/
      
		final int side = b.sideToMove;
		int max = side*gameAI.evaluate(b, -side);
		
		if(max<=-Board.WIN_SCORE || max >=Board.WIN_SCORE || max == side*Board.DRAW_SCORE)
			return max;
		
		if(!Game.isRandom)
			max+=this.randomMCValue()*64;
		
		//FP, not checked yet
		if(max>beta)
		return beta;
		
		int  teller =0;
		Marker++;
		final int pathlength=b.numberOfMovesPlayed;
		
	   if(pathlength<KillerMove.length)	
		for(int i=0; i<KillerMove[0].length; i++){	
		 Move kill = generator.generateIncMove(b, side, KillerMove[pathlength][i], null, -1, false);
					 
		 if(kill != null){
			    kill.apply(b);
				int value =  side*gameAI.evaluate(b, side);
				kill.undo(b);
				
				if(value > beta){
					
					 if (pathlength<KillerMove.length && !KillerMove[pathlength][0].equals(kill)) { 
						   KillerMove[pathlength][1].copy(KillerMove[pathlength][0]);
				            KillerMove[pathlength][0].copy(kill);
				        }
					generator.recycle(kill);
					  
					return value;}		
	
			
				if(!Game.isRandom)
					value+=this.randomMCValue()*64;

				if(value>max){
					
					max = value;	
			}
				 teller++;
			   
			    MarkedMoves[kill.index]=Marker;
				generator.recycle(kill);
				  			 
		 }
	}
		Move list = generator.generateUnMarkedMoves(b, side,
                MarkedMoves, Marker);
								
		int counter =0;
	     for(Move m = list; m!=null; m=m.next){
			  m.score=getMoveWeight(b,m);
			  moveRepArray [counter++] = m;
		 }     
	     
	 		
	   final int cutOff = (counter < 5-teller) ? counter: 5-teller;
	  //  final int cutOff = counter;
		
	  	for(int i = 0; i < cutOff; i++){	
	  		
	  		int maxValue = moveRepArray[i].score;
	  		int maxIndex = i;
	  		
	  	 	for(int j = i+1; j< counter; j++){
	  	 		
	  	 		if(maxValue<moveRepArray[j].score){
	  	 			maxValue = moveRepArray[j].score;
	  	 			maxIndex = j;
	  	 		}
	  	 		
	  	 	}
	  		
	  		Move m = moveRepArray[maxIndex];	
	  		
	  		  			
					m.apply(b);
					int value =  side*gameAI.evaluate(b, side);
					m.undo(b);
					
					if(value > beta){
						
						  if(pathlength<KillerMove.length) { 
							   KillerMove[pathlength][1].copy(KillerMove[pathlength][0]);
					            KillerMove[pathlength][0].copy(m);
					        }
						  
						  generator.recycle(list);
						return value;}		
		
				
					if(!Game.isRandom)
						value+=this.randomMCValue()*64;

					if(value>max){
						
						max = value;	
				}
					
					moveRepArray[maxIndex] = moveRepArray[i];
	  	}
	  	
	/*	for(int i = cutOff; i < counter; i++){
			Move m = moveRepArray[i];
			m.apply(b);
			final int value =m.score = side*gameAI.gameTheoreticEvaluation(b, side);
				
			m.undo(b);
			if(value>=Board.WIN_SCORE){
				generator.recycle(list);
				return value;}		
		}*/
		generator.recycle(list);
		return max;			
	}	

/*
private int selectReplyMove(Board b, int alpha, int beta, int depth, int height){	

	
	/*if(gameAI.epsilonGreedy && randomMCValue() <=0.05)
	  return selectKnowledgeMemRandomMove( b, list);*/
      
	/*	final int side = b.sideToMove;
		
		
		if(depth<=0){
			 return  side*gameAI.evaluate(b, -side) +(int)((!Game.isRandom)?this.randomMCValue()*64:0);
		}
		
		else{
		  final int score = side*gameAI.gameTheoreticEvaluation(b, -side);
		
		
		if(score<=-Board.WIN_SCORE || score >=Board.WIN_SCORE || score == side*Board.DRAW_SCORE)
			return score;
		}
		int max = Integer.MIN_VALUE;	
		int  teller =0;
		
		
		final long markCount = ++Marker;
		final int pathlength=b.numberOfMovesPlayed;
		
	   if(pathlength<KillerMove.length)	
		for(int i=0; i<KillerMove[0].length; i++){	
		 Move kill = generator.generateIncMove(b, side, KillerMove[pathlength][i], MarkedMoves,markCount, false);
					 
		 if(kill != null){
			 kill.apply(b);
				int value =  -selectReplyMove(b, -beta, -alpha, depth-1, height+1);
				kill.undo(b);
				
				if(value >= beta){
					 if (pathlength<KillerMove.length && !KillerMove[pathlength][0].equals(kill)) { 
						   KillerMove[pathlength][1].copy(KillerMove[pathlength][0]);
				            KillerMove[pathlength][0].copy(kill);
				        }
					generator.recycle(kill);
					  
					return value;}		
	            
				
				if(value>alpha)
					alpha=value;
		

				if(value>max){
					
					max = value;
			}
				 teller++;
			   
			    MarkedMoves[kill.index]=markCount;
				generator.recycle(kill);
				  			 
		 }
	}
		Move list = generator.generateUnMarkedMoves(b, side,
                MarkedMoves, markCount);
	
		Move [] mArray = moveSaveArray[height];
		
		int counter =0;
	     for(Move m = list; m!=null; m=m.next){
			  m.score=WeightTable[m.getMoveType(b)];
			  mArray [counter++] = m;
		 }     
	     		
	  	for(int i = 0; i < counter; i++){	
	  		
	  		int maxValue = mArray[i].score;
	  		int maxIndex = i;
	  		
	  	 	for(int j = i+1; j< counter; j++){
	  	 		
	  	 		if(maxValue<mArray[j].score){
	  	 			maxValue = mArray[j].score;
	  	 			maxIndex = j;
	  	 		}
	  	 		
	  	 	}
	  		
	  		Move m = mArray[maxIndex];	
	  		
	  		  			
					m.apply(b);
					
					int value;
					if(teller>=5){
						value= side*gameAI.evaluate(b, side);
						if(value>alpha  && depth >1)
							value =  -selectReplyMove(b, -beta, -alpha, depth-1,height+1);
						}
						else value =  -selectReplyMove(b, -beta, -alpha, depth-1,height+1);
					m.undo(b);
					
					if(value >= beta){
						generator.recycle(list);
						  if (pathlength<KillerMove.length && !KillerMove[pathlength][0].equals(m)) { 
							   KillerMove[pathlength][1].copy(KillerMove[pathlength][0]);
					            KillerMove[pathlength][0].copy(m);
					        }
						return value;}		
		
				if(value>alpha)
						alpha=value;

					if(value>max){						
						max = value;	
					}
					
					mArray[maxIndex] = mArray[i];
					teller++;
	  	}
	  	

		generator.recycle(list);
		return max;			
	}	*/



protected final Move selectDecisiveMove(Board b, Move list){
	
	double U=0;
	final int side = b.sideToMove;
	for(Move m = list; m!=null; m=m.next){
		countNodes++;	
		m.apply(b);
		int value =side*gameAI.gameTheoreticEvaluation(b, side);
		//MCProofEntry e = table.retrieve(m.hash(b));	
		m.undo(b);
		if(value>=Board.WIN_SCORE){
			m.score = value;
			return m;}
		
		U+=m.score=1;
	}
	
	U *=randomMCValue();
	
	for(Move m = list; m!=null; m=m.next){
		U-=m.score;
		if(U<=0)
			return m;
		
	}
	
	return list;
}
	
@SuppressWarnings("unused")
private Move selectRealRandomMove(final Board b, final Move list){
		
		int U=0;
		
		for(Move m = list; m!=null; m=m.next){
		  U++;
		}
		
		U = (int)(U*randomMCValue());
		
		for(Move m = list; m!=null; m=m.next){
			U--;
			if(U<=0)
				return m;
			
		}
		
		return list;
	}

private void updateHH(final int side, final Move m, int reward){
	
	if(!gameAI.isRPS)
		return;
	
	if(side==Board.B_SIDE){
		this.totalB[m.index]++;
		
		if(reward==Board.DRAW_SCORE)
			reward =1;
		else
			if(reward >=0){
			 reward =2;
		}
		else	
			reward =0;
		
		this.HistoryB[m.index]+=reward;
		
	}else{
		this.totalR[m.index]++;
		
		if(reward==Board.DRAW_SCORE)
			reward =1;
		else
			if(reward <=0){
			 reward =2;
		}
		else	
			reward =0;
		
		this.HistoryR[m.index]+=reward;
	}
}

private int getMoveWeight(Board b, Move m){
	
	return (gameAI.isRPS)?(WeightTable[m.getMoveType(b)]):(int)(1000*getRelHH(b.sideToMove,m)); 
}

private  double getRelHH(int side, Move m){
	
	return (side==Board.B_SIDE)?((double)HistoryB[m.index])/totalB[m.index]:((double)HistoryR[m.index])/totalR[m.index];
}

private static double reward(double x) {
    return  (2 / (1 + Math.exp( -2 * 0.0005* x)) );
}
	
	 private static double log(int N){
		 
		 if(N>=logTable.length)
			 return Math.log(N);
		 else 
			 return logTable[N];		 
	 }
	 private static long seed = 1978L;  
	    private final static long MULTIPLIER = 0x5DEECE66DL;
	    private final static long APPEND = 0xBL;
	    private final static long RMASK = (1L << 48) - 1;
	
	    private static double randomIMCValue() {   
	        
	        long nextseed = (seed * MULTIPLIER + APPEND) & RMASK;

	        seed = nextseed;
	        return (nextseed&Integer.MAX_VALUE)/(double)Integer.MAX_VALUE;
	        } 
	 

	    
	 static{
		 	 

		 logTable = new double[1<<16];
		 for(int i= logTable.length;--i>=0;)
			 logTable[i] = Math.log(i);		 
		 
		 randomTable = new double[G_MASK+1];
	 

	 
	 for(int i=  randomTable.length;--i>=0;)
		 randomTable[i] = randomIMCValue();		
	 
	/* for(int i = 0; i< 10; i++){
		 System.out.println(randomTable[i]);		 
	 }*/
	 
	 }
	 
	  
 
  
 private class MCResult {
	 
	 private int games;
	 private int score;
	 
	 
	 public MCResult() {
		// TODO Auto-generated constructor stub
	}


	public  void setValues(int g, int s){		 	 
		 games = g;
		 score = s;	 
	 }
	 
 }

public int getCountGames() {
	return countGames;
}

public static final class MCProofEntry {
	
	
	public final static byte NO_DRAW =0;
	public final static byte AT_LEAST_DRAW =1;
	public final static byte DRAW =2;
	
	public int games;
	public double value;
    public double pbValue;
	public long hashValue;
	public byte drawValue;
	public double eValue;
	public double mmValue;
	public int stamp;
	public byte lossCounter=0;
	
	public MCProofEntry next = null;
	
	public final boolean hasInfo(){

	 return (games==0);
	}
	
	public double getIMValue(double alpha){
	return (1-alpha)*value+	mmValue*alpha;}
}


public  final static class MCTable {
	
	private MCProofEntry [] array; 
	
	private int stamp;
		
	public MCTable(int key){
		
		 array = new MCProofEntry[1<<key]; 		
	}
	

	
	public void storeBoard(MCProofEntry entry, double value, double pbValue, int evalue, int games){	

		entry.games = games;
		entry.value = value;
		entry.pbValue = pbValue;
		entry.eValue = reward(evalue);
		entry.mmValue = 2-entry.eValue;	
	}
	
	public void storeBoard(MCProofEntry entry, double value, double pbValue,  int games){	

		entry.games = games;
		entry.value = value;
		entry.pbValue = pbValue;
		
	}
	
	
	public void storeBoard(MCProofEntry entry, double value,  int games){
		if(Math.abs(entry.value)<Board.WIN_SCORE){
	   entry.games = games;
	   entry.value = value;	}	  
	}
	
	public MCProofEntry storeDefBoard(long hashValue){
		
		 
		MCProofEntry entry = new MCProofEntry();
	    final int key = (int) (hashValue &(array.length-1));
		entry.games = 1;
    	entry.pbValue = 10;
		entry.hashValue = hashValue;
		entry.next = array[key];
		array[key] = entry;
	    entry.stamp =stamp;
	    
		return entry;
	}
		  
	 public MCProofEntry retrieve(long hashValue) {
		 
	      for(MCProofEntry entry = array[(int) (hashValue &(array.length-1))]; entry!=null; entry = entry.next)	
	        if (entry.hashValue == (hashValue)) {
	        	 entry.stamp =stamp;
	            return entry;
	        }
	             
	        return null;
	    }
	 
	 public int retrieveGames(long hashValue) {
		   for(MCProofEntry entry = array[(int) (hashValue &(array.length-1))]; entry!=null; entry = entry.next)	
		        if (entry.hashValue == (hashValue)) {
		            return entry.games;
		        }
		             
		        return 0;
	    }

	    public void clear() {
	       for(int i = array.length; --i>=0;)
	    	   array[i] = null;
	    	//table = new MCProofEntry[table.length];
	    }
	    
	    public int countNodes(){
	    	
	    	int counter =0;
	    	
	    	for(int i = array.length; --i>=0;)
	    	  for(MCProofEntry entry = array[i]; entry!=null; entry = entry.next)	
	    		  counter++;
	    	
	    	return counter;	    		  
	    }	  
	    
		public int pack( int offset )
		{   // Keep only entries with current stamp (added or visited since last call to pack).
			MCProofEntry entry, first, last;
			int counter = 0;
			for ( int i=0; i<array.length; ++i ) {	    		
				first = last = null;
				entry = array[i];
				while ( entry != null ) {
					if ( entry.stamp >= stamp-offset ) {
						if ( first == null ) {
							first = last = entry;
						}		
						else {
							last.next = entry;
							last = entry;
						}
						counter++;
					}
					entry = entry.next;
				}
				if ( last != null ) {
					last.next = null;
				}	
				array[i] = first;
			}
			stamp++;
			return counter;
		}
		
		public int getStamp() {
			return stamp;
		}
}

}
