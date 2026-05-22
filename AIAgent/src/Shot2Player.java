
import java.util.concurrent.atomic.AtomicInteger;





public class Shot2Player extends PVMCRandProverPlayer {
	
	 private static final double[] logCeilTable;
		
	private boolean dynamicKnowledge = true;	
 
	private int countGames = 0;
	
		
	public Shot2Player() {
		super();
		table = new MCTable(gameAI.sizeTT);
	}
	
	public Shot2Player(Game game){
	
		super(game);
		table = new MCTable(gameAI.sizeTT);
	}
	
	
	public Move search(Board b) {
		
		//NOTE: There is a bug when trying to simplify a Clobber position if simplifyBoard is
		//set to true
		//Board b2 = gameAI.getNewSimplifiedBoard(b);
		

	    Evaluator.parametrise(false);
	
	      
	    long start = System.currentTimeMillis();
		Game.randomize();	
		final int side = b.sideToMove;
		
		copy_board = (copy_board==null)?b.duplicate():b.duplicate(copy_board);
		bord = b;		
		//not optimal will lose history
        gameAI.simplifyBoard(b);
					
		
		if(gameAI.isRemember){
	    
	    	long help = System.currentTimeMillis();
	    	    	
 		    int cntNodesKept = table.pack(1);
	    	Log.println( "TT-keep: ms=" + (System.currentTimeMillis()-help) + " cnt=" + cntNodesKept);
	    	
	    	Move list = generator.generateMoves(b);
	    	boolean repetition  = false;
	    	for(Move m = list; m!=null; m=m.next){
	    		m.apply(b);
	    		final int times = gameAI.checkOnDraw(b);
	    	    if(times == Board.REPETITION){
	    	   MCProofEntry entry = table.retrieve(b.hashValue);
	    		if(entry!=null)
	    			entry.value=1;
	    		    repetition  =true; 
	    	    }
	    		
	    		m.undo(b);
	    	}
	    	
	    	if(repetition){
	    		for(Move m = list; m!=null; m=m.next){
		    		MCProofEntry entry = table.retrieve(m.hash(b));
		    		if(entry!=null)
		    			entry.games = Math.max(1,entry.games/16);
		    	    }
	    	}
	    	//System.out.println(System.currentTimeMillis()-hulp);
	    }
	    else table.clear();
		
		table.storeBoard(table.storeDefBoard(b.hashValue), 1, 1,1);
		//teller =0;
		dynamicKnowledge = gameAI.isSolver;      
		
		if(Game.isRandom)
			this.setSeed((int)System.currentTimeMillis());	

		bound = (gameAI.isABPlayout)?(int)(0.85*gameAI.boundScore()):gameAI.boundScore();
		
	    initialiseDatabase();
	
	    countNodes = 0;
		egdbCounter=0;
		timeoutFlag = false;		
		
		boolean keepTrack = true;
				
		countGames = 0;
		duration  = 0;
		
		Move best =selectDecisiveMove(b,generator.generateMoves(b,b.sideToMove));
		
	   setKillerMoves();
		
		if(best.score >= Board.WIN_SCORE){
			
			best.apply(b);
			table.storeBoard(table.storeDefBoard(b.hashValue), Board.WIN_SCORE, 1, 1);
			best.undo(b);
			Log.print("Mate in one");
		    doSomeThing(); 
			return best;
		}
		    
	   Log.print("Target Time: " + (targetTime / 1000f) + "s\r\n");
	   AtomicInteger playouts = new AtomicInteger(0);
	   AtomicInteger budgetUsed = new AtomicInteger(0);
	   Shot(b,0,BUDGET,budgetUsed,playouts,new AtomicInteger(0));
	   countGames=playouts.intValue();
		  		    
	    doSomeThing();   
	    
	    Move list = generator.generateMoves(b, side);
	    double max = -Board.WIN_SCORE;
	    double max2 = -Board.WIN_SCORE;
	    int maxgames =0;
	    int maxgames2 =0;
	    Move best2 =null;
		  
	 	for(Move m = list; m!=null; m=m.next){
  	    	
		    	int egames =0;
		    	double evalue =0;
				    	
		    	MCProofEntry entry = table.retrieve(m.hash(bord));
				
				if(entry==null){
					Log.println("null");
					continue;}
				
				if(Math.abs(entry.value)<Board.WIN_SCORE){
				   evalue = (evalue*egames + entry.games*entry.value)/(entry.games);
				   egames = entry.games;
				}
				else{
					evalue = entry.value;
					egames = entry.games;
				}
				
				if(egames != 0){
					
					if(evalue>=Board.WIN_SCORE){
						Log.println("Solved: " + m + " " + evalue/2 + " " +  egames);
						max = evalue;
						best=m;
						best.score = Board.WIN_SCORE*side;
						continue;
					}	
					
					if(evalue<=-Board.WIN_SCORE){
						if(max<=-Board.WIN_SCORE && egames>maxgames){
						    max = evalue;
					        maxgames = egames;
						    best = m;
						    best.score = -Board.WIN_SCORE*side;
						}
						Log.println(m + " " + evalue/2 + " " +  egames);
						continue;
					}	
					
					m.score =   (int)(evalue*50);	
					double value = evalue;// + 1/Math.sqrt(egames);
					
					Log.println(m + " " + 50*evalue + " " +  egames);
					if(evalue > -Board.WIN_SCORE && max < Board.WIN_SCORE)
						if(egames>maxgames ){//value>max){
							max2=max;
							maxgames2=maxgames;
							best2=best;
							
							max = value;
							maxgames = egames;
							best = m;						
						}
						else if(egames>maxgames2)
						{
							max2 = value;
							maxgames2 = egames;
							best2 = m;						
						}
				
				}
		    }
	 	
	 	if(max > -Board.WIN_SCORE && max < Board.WIN_SCORE && best2!=null)
	 		if(max2>max){
	 			max=max2;
	 			best = best2;
	 			maxgames = maxgames2;
	 		}
			
	    duration=(System.currentTimeMillis()-start);
		      
		Log.println("gps: " + countGames/(duration/1000f));
		Log.println("time: " + duration);
		Log.println("Budget Used: " + budgetUsed + " N: " + countGames);
		Log.println("Move Selected: " + best + " Visits: " +  maxgames);
		printPV(b,0);
		
        b = copy_board.duplicate(b);
        
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
        }
				
		Log.println();
			
		 if(gameAI.isEGDB)
	        	Log.println("EGDB Hits " + egdbCounter);
		
		Evaluator.parametrise(true);
					
		return best;
	}


//SHOT
private int Shot(Board b, int pathlength,int budget, AtomicInteger budgetUsed, AtomicInteger playouts, AtomicInteger wins){
					
	//countNodes++;	
	final int side = b.sideToMove;		    
	
	final int times = gameAI.checkOnDraw(b);
	
    if(times == Board.REPETITION || b.numberOfMovesPlayed >= gameAI.getMaxLength()){
    	budgetUsed.addAndGet(1);
		playouts.addAndGet(1);
	    return 0;
	   }
	
	MCProofEntry parent = table.retrieve(b.hashValue);
	
	if(parent!=null){
				
		if(parent.value >=Board.WIN_SCORE){
			//budgetUsed.addAndGet(budget);
			playouts.addAndGet(1);
			wins.addAndGet(2);
		    return Board.WIN_SCORE;
		}
		
		if( parent.drawValue <=-Board.WIN_SCORE){
			//budgetUsed.addAndGet(budget);
			playouts.addAndGet(1);
			
		    return -Board.WIN_SCORE;
		}
		
		if( parent.drawValue == MCProofEntry.DRAW){
			//budgetUsed.addAndGet(budget);
			playouts.addAndGet(budget);
			wins.addAndGet(budget);
		    return 0;
		}
	}     
	  	          
	    Move move_list= generator.generateMoves(b, side);
	    	
		if(move_list==null){	
			Move.applyNullMove(b);
			int backup = monteCarloPlayout(b);
			Move.undoNullMove(b);	
			
			int score=0;
			
			if(backup==Board.DRAW_SCORE)
				score =1;
			else
				if(backup >=0)
				 score =2;
					 	
		    score = (side == Board.B_SIDE)?(score):((2-score));
			
			playouts.incrementAndGet();
			budgetUsed.incrementAndGet();
			wins.addAndGet(2-score);		
			return 0;
		}
		
		AtomicInteger count = new AtomicInteger(0);
		move_list = filterCountMoves(b,move_list,count);
		int S = count.intValue(); 
					 
		if(S==0)
			 return  Board.WIN_SCORE;
		
	    if(S ==1){ 
			move_list.apply(b);
			
			MCProofEntry entry = table.retrieve(b.hashValue);	
			if(entry==null)
				entry =table.storeDefBoard(b.hashValue);
			
			int proofValue = Shot(b,pathlength+1, budget, budgetUsed,  playouts, wins);
				
			move_list.undo(b);	
			
			if(dynamicKnowledge && (proofValue>=Board.WIN_SCORE || proofValue<=-Board.WIN_SCORE)){
				entry.value = proofValue;
				proofValue = -proofValue;
			}else
				update(entry,wins,playouts);
			
			wins.set(2*playouts.intValue()-wins.intValue());
			generator.recycle(move_list);	
			
			
			return proofValue;
		}
	    
			  
		   if((int)((parent.games+budget)/(S*logCEIL(S)))<=30 && pathlength >0 && gameAI.isHybrid){
		  	   
			   for(int i=0; i<budget;i++){
				  final int proofValue = UCT(b,pathlength, budgetUsed, playouts,  wins);
				  if(proofValue!=0)
					  return proofValue;
			   }
			   			   
			      return 0;
		   }
		
		   
		   
		 if(gameAI.PBW>0){
			 assignValues(b,move_list);
			 move_list = sort(move_list);}
		 
		 boolean childProven = false;
		 
		//if(parent.games<=S)
			for(Move m=move_list;m!=null; m=m.next)
		      if(table.retrieveGames(m.hash(b))==0){
							   
				int value;
				int backup;
							        
			     //Playout starts here
				m.apply(b);
				MCProofEntry entry =table.storeDefBoard(b.hashValue);
				backup  = monteCarloPlayout(b);
				m.undo(b);
				
				if(backup * side==Board.WIN_SCORE+1){
					  value=Board.WIN_SCORE;
					  table.storeBoard(entry, value, 1,  1);	
					  playouts.incrementAndGet();
					  budgetUsed.incrementAndGet();
					  return -Board.WIN_SCORE;					  
				}
				else if(backup * side>=-Board.WIN_SCORE-2 && backup * side < -Board.WIN_SCORE){
				      childProven  =true;
					  value=-Board.WIN_SCORE;
					  wins.addAndGet(2);
				}
				else{
					int score=0;
			
					if(backup==Board.DRAW_SCORE)
						score =1;
					else
						if(backup >=0){
						 score =2;
					}
							 	
				    value = (side == Board.B_SIDE)?(score):((2-score));
				    wins.addAndGet(2-value);
				}									
			
				table.storeBoard(entry, value, gameAI.PBW*WTable[m.getMoveType(b)],  1);	
				
				playouts.incrementAndGet();				
				
				if(budgetUsed.incrementAndGet()>=budget){
					generator.recycle(move_list);	
					return 0;}
			}
			
		if(childProven && dynamicKnowledge){
			move_list = filterCountMoves(b,move_list,count);
			S = count.intValue(); 
			
			if(S==0)
			  return  Board.WIN_SCORE;
		}
		
		 int bud=0;
		 int possibleMoves=S;
		 
		 if(gameAI.PBW==0){
			 assignValues(b,move_list);
			 move_list = sort(move_list);}
		 
		 final int tBudgetNode = parent.games;
		 
		 Move discardedList = null;
		 
		  while (true){
			 bud = bud +Math.max(1,(int)((tBudgetNode+budget)/(S*logCEIL(possibleMoves))));
			 Move m = move_list;
			 childProven = false;
			 
			 for(int i=0;i<S;i++){
				 int playoutM=table.retrieveGames(m.hash(b));
				
				 if(playoutM<bud){
					int bud1 = bud-playoutM;
					
					if(pathlength==0 && S==2 && i == 0){
						int newBud1 = budget-budgetUsed.intValue()-(bud-table.retrieveGames(m.next.hash(b)));
						
						if(newBud1>bud1)
							bud1 = newBud1;
					}
				 					
					bud1=Math.min(bud1, budget-budgetUsed.intValue());
					AtomicInteger u = new AtomicInteger(0);
					AtomicInteger p = new AtomicInteger(0);
					AtomicInteger w = new AtomicInteger(0);
					m.apply(b);
					int proofValue= Shot(b,pathlength+1, bud1, u,  p, w);
					MCProofEntry entry = table.retrieve(b.hashValue);
					m.undo(b);
					
					playouts.addAndGet(p.intValue());
					budgetUsed.addAndGet(u.intValue());
					wins.addAndGet((2*p.intValue())-w.intValue());
				    
					if(proofValue>=Board.WIN_SCORE){
				    	entry.value=Board.WIN_SCORE;
				    	return -proofValue;
				    }
					else
					if(proofValue<=-Board.WIN_SCORE){
						entry.value=proofValue;
						childProven  =true;
					}
				    else
				    	update(entry,w,p);
							
				    if(budgetUsed.intValue()>=budget)
				    	break;
				 }
				   
				    m=m.next;
			 	}	
			 
			    if(childProven && dynamicKnowledge){
			       	move_list = filterCountMoves(b,move_list,count);
					int noProven;
					
					if(gameAI.isHalving){
						 noProven      = 	S-count.intValue();			
						possibleMoves -= noProven;}
					else{
						noProven = possibleMoves - count.intValue();
						possibleMoves = count.intValue();
					}				
								
					if(possibleMoves==0)
					  return Board.WIN_SCORE;
										
					if(gameAI.isHalving && gameAI.isSolver){
					 assignValues(b,discardedList);
					 discardedList = sort(discardedList);
					 				 
					 if(move_list!=null)
					    discardedList = reduceList(move_list, discardedList, noProven , count);
					 else{
						 move_list = discardedList;
						 discardedList = reduceList(discardedList, noProven , count);
					 }
					 
					 noProven = noProven-count.intValue();	
					}
					/*else
					 S-= noProven;*/
					
					if(budgetUsed.intValue()>=budget){
					  generator.recycle(move_list);	
				      return 0;}
					
					int newS = (int)Math.ceil(S/2.0);
					S-=noProven;
					
					if(newS<S)
						S =newS;
					
			     }
			    else S= (int)Math.ceil(S/2.0);
					 
				 if(S<=1)
					 break;
				 
				 assignValues(b,move_list);
				 move_list = sort(move_list);
				 
				 if(gameAI.isHalving){
					 Move remList = splitMoves(move_list,S);
					 if(gameAI.isSolver)
						 discardedList=joinMoves(discardedList,remList);}
		  }		 	 
			
		generator.recycle(move_list);	
		return 0;
	}	

private int UCT(Board b, int pathlength, AtomicInteger budgetUsed, AtomicInteger playouts, AtomicInteger wins){
	
	final int side = b.sideToMove;		  
   
	
	final int times = gameAI.checkOnDraw(b);
	
    if(times == Board.REPETITION || b.numberOfMovesPlayed >= gameAI.getMaxLength()) {
    	budgetUsed.addAndGet(1);
		playouts.addAndGet(1);
	    return 0;
	   }
	
	MCProofEntry parent = table.retrieve(b.hashValue);
	
	if(parent!=null){
				
		if(parent.value >=Board.WIN_SCORE){
			//budgetUsed.addAndGet(budget);
			playouts.addAndGet(1);
			wins.addAndGet(2);
		    return Board.WIN_SCORE;
		}
		
		if( parent.drawValue <=-Board.WIN_SCORE){
			//budgetUsed.addAndGet(budget);
			playouts.addAndGet(1);
			
		    return -Board.WIN_SCORE;
		}
		
		if( parent.drawValue == MCProofEntry.DRAW){
			//budgetUsed.addAndGet(budget);
			playouts.addAndGet(1);
			wins.addAndGet(1);
		    return 0;
		}
	}     
	  	          
	    Move move_list= generator.generateMoves(b, side);
	    	
		if(move_list==null){	
			Move.applyNullMove(b);
			int backup = monteCarloPlayout(b);
			Move.undoNullMove(b);	
			
			int score=0;
			
			if(backup==Board.DRAW_SCORE)
				score =1;
			else
				if(backup >=0)
				 score =2;
					 	
		    score = (side == Board.B_SIDE)?(score):((2-score));
			
			playouts.incrementAndGet();
			budgetUsed.incrementAndGet();
			wins.addAndGet(2-score);		
			return 0;
		}
		
		Move bestMove = (pathlength >0 && parent.games<T)?selectRandomMove(b, move_list):selectPBMove(b, move_list);    	  			
		
	     if(table.retrieveGames(bestMove.hash(b))==0){
							   
				int value;
				int backup;
							        
			     //Playout starts here
				bestMove.apply(b);
				MCProofEntry entry =table.storeDefBoard(b.hashValue);
				backup  = monteCarloPlayout(b);
				bestMove.undo(b);
				
				if(backup * side==Board.WIN_SCORE+1){
					  value=Board.WIN_SCORE;
					  table.storeBoard(entry, value, 1,  1);	
					  playouts.incrementAndGet();
					  budgetUsed.incrementAndGet();
					  return -Board.WIN_SCORE;
					  
				}
				else if(backup * side>=-Board.WIN_SCORE-2 && backup * side < -Board.WIN_SCORE){
					  value=-Board.WIN_SCORE;
					  wins.addAndGet(2);
				}
				else{
					int score=0;
			
					if(backup==Board.DRAW_SCORE)
						score =1;
					else
						if(backup >=0){
						 score =2;
					}
							 	
				    value = (side == Board.B_SIDE)?(score):((2-score));
				    wins.addAndGet(2-value);
				}									
			
				table.storeBoard(entry, value, gameAI.PBW*WTable[bestMove.getMoveType(b)],  1);	
				
				playouts.incrementAndGet();		
				budgetUsed.incrementAndGet();
				
	     }
	     else{
					AtomicInteger u = new AtomicInteger(0);
					AtomicInteger p = new AtomicInteger(0);
					AtomicInteger w = new AtomicInteger(0);
					bestMove.apply(b);
					int proofValue= UCT(b,pathlength+1,  u,  p, w);
					MCProofEntry entry = table.retrieve(b.hashValue);
					bestMove.undo(b);
					
					playouts.addAndGet(p.intValue());
					budgetUsed.addAndGet(u.intValue());
					wins.addAndGet((2*p.intValue())-w.intValue());
				    
					if(proofValue>=Board.WIN_SCORE){
				    	entry.value=Board.WIN_SCORE;
				    	return -proofValue;
				    }
					else
					if(proofValue<=-Board.WIN_SCORE){
						entry.value=proofValue;
						if(dynamicKnowledge){
							proofValue=this.lossCheck(b, move_list);
							if(proofValue!=0){
					    	return proofValue;}
							}
				    }
				    else
				    	update(entry,w,p);
							
		 }			 	 
			
		generator.recycle(move_list);	
		return 0;
	}	
		
	private Move filterCountMoves(Board b, Move list, AtomicInteger S){
		
	        Move newList = list;
	        Move previous =  null;
	        int counter = 0;
	        Move m = list;
	        
	        while( m != null) {
	        	
	        	MCProofEntry entry = (dynamicKnowledge)?table.retrieve(m.hash(b)):null;
				if( entry == null || entry.value>-Board.WIN_SCORE){
				  counter++;
				  previous = m;
				  m = m.next;
				 }
				else{
					
					if(previous==null)
						newList = m.next;
					else
					    previous.next=m.next;
					
					Move old = m;
					m = m.next;
					old.next=null;
				}
			}
	        
	        if(previous == null)
	        	newList =null;
	        	        
	        S.set(counter);
	        return newList;
	}
	
	private Move splitMoves(Move list, int S){
		
        Move previous = null;
        Move m = list;
        
        for (int i =0; i < S; i++) {        	
        	previous = m;
        	m=m.next;	
        }
        	
        previous.next = null;
        return m;
}
	
    private  Move joinMoves(Move oldList, Move newList){
		
    	if(oldList == null)
    		return newList;
    	
        Move previous = null;   
        for (Move m = newList; m!=null; m = m.next ){       	
        	previous = m;
        }
        	
        previous.next= oldList;
        
        return newList;
     }
    
    private Move reduceList(Move currentList, Move disList, int N, AtomicInteger count){
		   	
    	if(disList==null)
    		return disList;    	
    	
    	Move previous = null;
    	
        for(Move m = currentList; m!=null; m = m.next){       	
        	previous = m;
        }
        	
        previous.next= disList;
                
       return reduceList(disList, N, count);
     }
    
    private Move reduceList(Move disList, int N, AtomicInteger count){
    	
    	count.set(0); 
        Move m = disList;
        Move previous =null;
        
        for(int i = 0; i<N && m!=null; i++){
        	count.incrementAndGet();
        	previous=m;
        	m = m.next;
        }
        
        disList= m;
        previous.next = null;
        return disList;    	
    	
    }
		
	
   private void assignValues(Board b, Move list){
	
	   for(Move m = list; m!=null; m=m.next){
			MCProofEntry entry = table.retrieve(m.hash(b));
			if(entry!=null){
				double L = (1 +(2*entry.games-entry.games*entry.value));
				m.score= (int)(1000*(entry.value + gameAI.PBW*entry.pbValue/(50*(L+1))));	
			}
			else m.score= (1000*(2 + gameAI.PBW*m.getMoveType(b)/50));
    }
  }	
   
   private void update(MCProofEntry entry, AtomicInteger w, AtomicInteger  p){
	
	   
		if(!(entry.value>=Board.WIN_SCORE || entry.value<=-Board.WIN_SCORE || entry.value==Board.DRAW_SCORE))
		       entry.value=((entry.value*entry.games)+w.intValue())/(entry.games+p.intValue());		
		
		entry.games+=p.intValue();
   } 
   
   private int lossCheck(Board b, Move move_list){
	  	   
		for(Move m = move_list; m !=null; m=m.next){
			MCProofEntry e = table.retrieve(m.hash(b));
			if(e==null)
				return 0;			
        	else if( e.value>=0){
        		
        			if(e.value>=Board.WIN_SCORE)
						return -Board.WIN_SCORE;
        			  else
						return 0;  				
        	}	
		}
		
		return Board.WIN_SCORE;
	   
   }
    
	
	 private static double logCEIL(int N){
		 
		 if(N>=logCeilTable.length)
			 return  Math.ceil(Math.log(N)/Math.log(2));
		 else 
			 return logCeilTable[N];		 
	 }

	 
static{
	logCeilTable = new double[1<<16];
	 for(int i= logCeilTable.length;--i>=0;)
		 logCeilTable[i] = Math.ceil(Math.log(i)/Math.log(2));
	
}
	    


}
