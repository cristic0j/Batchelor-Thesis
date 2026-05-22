import java.util.ArrayList;
import java.util.Iterator;



public final class CombinatorialPlayer extends Player {
	
	private long subCounter;
	private long subSuccesCounter;
	private long comTTCounter;
	private Combinatorial subGame;
	
	
	private final static int SHIFT = 16;
	private final static long VALUE_MASK = (1<< SHIFT )-1;
	private final static long MASK = ((0xFFFFFFFFFFFFFFFFL >>> SHIFT) << SHIFT);
	private final static int TRANSPOSE_VALUE = TERMINAL_SCORE;
	private final static long HASK_KEY_MASK = (1<< SHIFT)-1;
	private final static int NO_ELEMENT = 11111111;
	private final long[] tTable = new long[1 << SHIFT];
	
	public CombinatorialPlayer() {
		super();
	}

	public CombinatorialPlayer(Game game, TranspositionTable table) {
		super(game,table);
		subGame = game.generateCombinatorial();
	
		
		if(subGame==null){
			game.playerMode = Game.Mode.AB;
		}
		
	}

	public CombinatorialPlayer(Game game) {
		this(game,null);
	}
		
	/*public Move Search(Board b, int side){
		
		Move moveList = generator.generateMoves(b,side);
		
		Move bestMove = null;
		
		ArrayList list = gameAI.getSubGames(b);
		
		if(list.size()>0)
			for(Move m = moveList; m!=null; m=m.next){
			
				m.apply(b);
				int score = combSearch(b,-side);
				m.undo(b);
			
				if(score*side >=  Board.WIN_SCORE){
					bestMove = m;
					bestMove.score = score;
					Log.println("Winning position");
					return m;
			}
				
		}
		
			
			return super.Search(b,side);
	}*/
	
	 public Move search(Board b) {
	
		comTTCounter = subCounter = subSuccesCounter=0;
			 
		Move move =  super.search(b);
		
		Log.print("Com Counter: " + comTTCounter + " Succes ratio: " + (double)subSuccesCounter/subCounter + " " + subCounter + " " + subSuccesCounter+"\r\n");
	
		return move;
	 }
	
	
/*	protected Move selectTransMove(double depth, int side, int beta, int alpha, boolean null_move, int pathlength, int node_type, boolean isExtension){
				
		
		Board conBoard = bord;
		if((depth>5 ||(depth>4 && node_type == PV_NODE)) && bord.W_Counter+bord.B_Counter < Board.SQUARE-32 && gameAI.playerMode == Game.COMB){
		    
			Move best_move = dummyMove;
			
			int value =	Retrieve(bord.hashValue);
		    if( value !=NO_ELEMENT){  		    	
		    	
		    	if(value>=Board.WIN_SCORE)
		    		best_move.score = TERMINAL_SCORE - pathlength;
        		 else 
        			best_move.score = -TERMINAL_SCORE + pathlength;
		    	
		    	comTTCounter++;
		    	return best_move;
		    }
			
			
			subCounter++;
			final int R =2;
			ArrayList list = subGame.getSubGames(bord);
			
        
			if(list.size()>1){
				
				         
				 conBoard =subGame.createBoard(bord.getGame());
				int sum = subGame.filterSimpleGames(list,conBoard); 
				
			
                Iterator it = list.iterator();
                
                while(it.hasNext()){ 
                	
                	Board subBoard = (Board) it.next();
        	
                	int score1 = regularSearch(subBoard, depth-R,  side, beta, alpha, null_move, pathlength, node_type, isExtension);
         
                	if(Math.abs(score1)<Board.WIN_SCORE || timeoutFlag){
                		sum=Combinatorial.UR;
                		 if(timeoutFlag)
                     		break;
                	}
    
                	Move.applyNullMove(subBoard );
                	int score2 = regularSearch(subBoard,depth-R,  -side, alpha, beta, null_move, pathlength, node_type, isExtension); 
                	Move.undoNullMove(subBoard );
         
                	if(Math.abs(score2)<Board.WIN_SCORE || timeoutFlag){
                	  sum=Combinatorial.UR;
                	  if(timeoutFlag)
                		break;
                	  }
         
                  
                	if(score1==score2){
                		if(score1>=Board.B_WIN_SCORE)
                			value = Combinatorial.LEFT;   
                		else if(score1<=Board.W_WIN_SCORE)
                			value = Combinatorial.RIGHT;
                	}
                	else
                		if(side*score1>=Board.WIN_SCORE)
                			value = Combinatorial.FUZZ; 
                		else if(side*score2>=Board.WIN_SCORE){
                			value = Combinatorial.ZERO;
                			continue;}
                	
                	subGame.addSubGame(subBoard,conBoard);
                	
                	if(sum==Combinatorial.UR){
                		continue;
                	}
         
                	if(sum==Combinatorial.LEFTFUZZ)
                	  if(value==Combinatorial.LEFT)
                		 continue;
                	  else
                	  {
                  		  sum=Combinatorial.UR;
                  		 continue;}
                	
                	if(sum==Combinatorial.RIGHTFUZZ)
                  	  if(value==Combinatorial.RIGHT)
                  		 continue;
                  	  else{
                  		  sum=Combinatorial.UR;
                  		  continue;}
                		
                	sum = Combinatorial.OutComeTable[sum][value];       	     
                }
        
                     
            subGame.recycle(list);    
        
            if(sum!=Combinatorial.UR){
            	
            	value =0;
            
		        if(sum==Combinatorial.LEFT)
		        	value = TERMINAL_SCORE - pathlength;
		        else
		         if(sum==Combinatorial.RIGHT)
		        	 value = -TERMINAL_SCORE + pathlength;
		         else
		        	 if(sum==Combinatorial.FUZZ){
		        		 if(side==Board.B_SIDE)
		        			 value = TERMINAL_SCORE - pathlength;
		        		 else 
		        			 value = -TERMINAL_SCORE + pathlength;}
		        	 else
		        		 if(sum==Combinatorial.ZERO){
				            if(side==Board.B_SIDE)
				            	value = -TERMINAL_SCORE + pathlength;
				             else 
				            	 value = TERMINAL_SCORE - pathlength;}
		        		 else
		        			 if(sum==Combinatorial.LEFTFUZZ && side==Board.B_SIDE)
		        				 value = TERMINAL_SCORE - pathlength;
		        			 else
						       if(sum==Combinatorial.RIGHTFUZZ && side==Board.W_SIDE)
						    	   value = -TERMINAL_SCORE + pathlength;
		        
		      if(Math.abs(value)>=Board.WIN_SCORE){
		        	        		        	
		        	subSuccesCounter++;
		        	best_move.score = value;
		        	
		        	Store(bord.hashValue, value);
		        	return best_move;
		        }
				
				}
            conBoard.initialise(bord.sideToMove);
  		  //conBoard=bord;
			}
		}
		
		Board hulpBoard = bord;
        bord = conBoard;
		Move x = super.selectTransMove(depth,  side, beta, alpha, null_move, pathlength, node_type, isExtension);
		bord =hulpBoard;
		return x;
		
	}*/
	 
	
	
	@SuppressWarnings("rawtypes")
	protected Move selectTransMove(double depth, int side, int beta, int alpha, boolean null_move, int pathlength, int node_type, boolean isExtension){
		
		Board conBoard = bord;
		
		if((depth>5 ||(depth>4 && node_type == PV_NODE)) && bord.W_Counter+bord.B_Counter < Board.SQUARE-32){
		    
			Move best_move = dummyMove;
			
			int value =	retrieve(bord.hashValue);
		    if(value !=NO_ELEMENT){  		    	
		    	
		    	if(value>=Board.WIN_SCORE)
		    		best_move.score = TERMINAL_SCORE - pathlength;
        		 else 
        			best_move.score = -TERMINAL_SCORE + pathlength;
		    	
		    	comTTCounter++;
		    	return best_move;
		    }
			
			
			subCounter++;
			final int R =2;
			ArrayList list = subGame.getSubGames(bord);
			
        
			if(list.size()>1){
				
				int star=0, downs =0,ups=0;
				conBoard =  subGame.createBoard(bord.getGame());
								         
				int sum = subGame.filterSimpleGames(list,conBoard); 
				
				if(sum==Combinatorial.UR){
					return super.selectTransMove(depth,  side, beta, alpha, null_move, pathlength, node_type, isExtension);
				}
				
				if(sum==Combinatorial.LEFTFUZZ){
					star=1;
					ups=1;
				}else if(sum==Combinatorial.RIGHTFUZZ){
					star=1;
					downs=1;					
				}
				else if(sum==Combinatorial.FUZZ)
					star=1;
				
                Iterator it = list.iterator();
                
                while(it.hasNext()){ 
                	
                	Board subBoard = (Board) it.next();
        	
                	int score1 = regularSearch(subBoard ,depth-R,  side, beta, alpha, null_move, pathlength, node_type, isExtension);
         
                	if(Math.abs(score1)<Board.WIN_SCORE || timeoutFlag){
                		sum=Combinatorial.UR;
                		subGame.addSubGame(subBoard,conBoard);
                		break;
                	}
    
                	Move.applyNullMove(subBoard );
                	int score2 = regularSearch(subBoard,depth-R,  -side, alpha, beta, null_move, pathlength, node_type, isExtension); 
                	Move.undoNullMove(subBoard );
         
                	if(Math.abs(score2)<Board.WIN_SCORE || timeoutFlag){
                	  sum=Combinatorial.UR;
                	  subGame.addSubGame(subBoard,conBoard);
                		break;}
         
         
                	if(score1==score2)
                		if(score1>=Board.B_WIN_SCORE){
                			value = Combinatorial.LEFT;   
                			ups++;
                			subGame.addSubGame(subBoard,conBoard);
                		}
                		else{
                			value = Combinatorial.RIGHT;
                			downs++;
                			subGame.addSubGame(subBoard,conBoard);
                		}
                	else
                		if(side*score1>=Board.WIN_SCORE){
                			value = Combinatorial.FUZZ; 
                			subGame.addSubGame(subBoard,conBoard);
                		}
                		else{
                			value = Combinatorial.ZERO;
                			continue;}
         
                	if(sum==Combinatorial.LEFTFUZZ)
                	  if(value==Combinatorial.LEFT)
                		 continue;
                	  else
                	  {
                  		  sum=Combinatorial.UR;
                  		  break;}
                	
                	if(sum==Combinatorial.RIGHTFUZZ)
                  	  if(value==Combinatorial.RIGHT)
                  		 continue;
                  	  else{
                  		  sum=Combinatorial.UR;
                  		  break;}
                		
                	sum = Combinatorial.OutComeTable[sum][value];
                	
                	if(sum==Combinatorial.UR){
                		break;
                	}
         
                	     
                }
        
        //Log.println("Outcome:" + sum);
                
        while(it.hasNext())                	
        	 subGame.addSubGame((Board) it.next(),conBoard);
            
        subGame.recycle(list);    
        
            if(sum!=Combinatorial.UR){
            	
            	value =0;
            
		        if(sum==Combinatorial.LEFT)
		        	value = TERMINAL_SCORE - pathlength;
		        else
		         if(sum==Combinatorial.RIGHT)
		        	 value = -TERMINAL_SCORE + pathlength;
		         else
		        	 if(sum==Combinatorial.FUZZ){
		        		 if(side==Board.B_SIDE)
		        			 value = TERMINAL_SCORE - pathlength;
		        		 else 
		        			 value = -TERMINAL_SCORE + pathlength;}
		        	 else
		        		 if(sum==Combinatorial.ZERO){
				            if(side==Board.B_SIDE)
				            	value = -TERMINAL_SCORE + pathlength;
				             else 
				            	 value = TERMINAL_SCORE - pathlength;}
		        		 else
		        			 if(sum==Combinatorial.LEFTFUZZ && (side==Board.B_SIDE||(ups>=2&&star==1)))
		        				 value = TERMINAL_SCORE - pathlength;
		        			 else
						       if(sum==Combinatorial.RIGHTFUZZ && (side==Board.W_SIDE||(downs>=2&&star==1)))
						    	   value = -TERMINAL_SCORE + pathlength;
		        
		      if(Math.abs(value)>=Board.WIN_SCORE){
		        	        		        	
		        	subSuccesCounter++;
		        	best_move.score = value;
		        	
		        	store(bord.hashValue, value);
		        	return best_move;
		        }
				
            }
           conBoard.initialiseLight(bord.sideToMove);
			}
		}
		
	 Board hulpBoard = bord;
	//bord = conBoard;
	Move m = super.selectTransMove(depth,  side, beta, alpha, null_move, pathlength, node_type, isExtension);
	if(hulpBoard!=conBoard)
		subGame.recycle(conBoard);
	
	bord=hulpBoard;
	return m;
	}
	
	private int regularSearch(Board b, double depth, int side, int beta, int alpha, boolean null_move, int pathlength, int node_type, boolean isExtension){
		
		//Seen before
		/*int value  = Retrieve(b.hashValue);
		
		if(value!=NO_ELEMENT)
			return value;*/
		
		//Check whether root is game-over
		int value = gameAI.gameTheoreticEvaluation(b, -side);
           
		  if (value >= Board.WIN_SCORE) {
	            return Board.WIN_SCORE;
		  } 

       if (value <= -Board.WIN_SCORE) {
			 	   	return -Board.WIN_SCORE; 
			 }
		
        
        Board hulpBoard = bord;
        bord = b;
       
       /* if(b.hashValue!=TranspositionTable.getHashValue(b.space, side)){
        	Log.println(b.hashValue + " " + TranspositionTable.getHashValue(b.space, side) + " " + (
        		TranspositionTable.getHashSideValue(b, Board.B_SIDE) ^ TranspositionTable.getHashSideValue(b, Board.W_SIDE) ^TranspositionTable.getHashMoveValue(side)));
           
        	TranspositionTable.setHashValue(b,side);
        	Log.println(""+b.hashValue);
        	Log.println(" " + b.toString());
        	
        	Log.println(b.B_Counter + " " + b.W_Counter );
        	
        	Log.println("Not the same");
        	
        	Log.println(" " + hulpBoard.toString());
        	
        		System.exit(0);
        }*/
        
		value = super.selectTransMove(depth,  side, side * Board.WIN_SCORE, -side *  Board.WIN_SCORE, null_move, pathlength, PV_NODE, isExtension).score; 
		bord= hulpBoard;
		
	    if (value >= Board.WIN_SCORE) {
            return Board.WIN_SCORE;
	    } 

		 if (value <= -Board.WIN_SCORE) {
		 	   	return -Board.WIN_SCORE; 
		 }
		
		/*if(!timeoutFlag)
			Store(b.hashValue,value);*/
		
		return value;
	}
	
	  private void store(long HashValue, int value) {
	  	    tTable[(int) (HashValue & HASK_KEY_MASK)] = (HashValue & MASK)
	  	            | (value + TRANSPOSE_VALUE);
	  	}

	  	private int retrieve(long HashValue) {
	  	    long value = tTable[(int) (HashValue & HASK_KEY_MASK)];
	  	 
	  	    if (value >> SHIFT == HashValue >> SHIFT) {
	  	        return (int) (value & VALUE_MASK) - TRANSPOSE_VALUE;
	  	    }
	  	   
	  	    return NO_ELEMENT; 
	  	}
	  	
	  	

	

}
