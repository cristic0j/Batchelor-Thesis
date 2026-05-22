import java.io.IOException;


public class PNSPlayer extends  AbstractPlayer {     	
  
    private final ProofNode drawValue = new ProofNode();  
  
    public static boolean deleteSecond = true;
  
    protected final Board hulpbord = new Board(); //    new Board(new ProofNode());
  
    protected int memMaxNodes = ProofNode.MAX_NODES;
    
    private int aK = 20;
    private int bK = 5;
       
    public PNSPlayer() {}

    public PNSPlayer(Game eval) { 
   
        gameAI = eval; 
        generator = gameAI.getGenerator();
         
        if (gameAI.isWonTable) {
            winTable = new WinTable(14);
            // ptable = new ProofBigTable(14); 
        }
        
	   double[] WTable = generator.getLOGRPPROB(); 
		
		WeightTable = new int[WTable.length];
		
		for(int i=WTable.length;--i>=0;){
			WTable[i] = Math.exp(WTable[i]);
			//WeightTable[i] = (int)(1000*WTable[i]);
			
			WeightTable[i] = (int)(1000*WTable[i]);	
		}
        
      	initialiseDatabase();
    }
 
    public void setMemMaxNodes(int max) {
        memMaxNodes = max;
    }
    
    public void setKnowledgeParameters(int a, int b) {
       aK = a;
       bK = b;
    }
 
    public Move search(Board b) {
    	
    	Evaluator.parametrise(false);
    	initialiseDatabase();   
    	egdbCounter=0;
        bord = b; 
        final int side = b.sideToMove;
        
        ProofNode root;
   
        ProofNode.nodeCounter = 0;
     
        countNodes = 0;
        duration = 0;      

        root = new ProofNode(b, side);
    
        setDrawValue(root);
     
        // first part algorithm  
        if (gameAI.isWonTable){
            TranspositionTable.setHashValue(b, root.side);
        }
       
        evaluate(b, root);
        // SetProofAndDisproofNumbers(root);
    
        ProofNode currentNode = root;
        
        // Get a snapshot of the current time.
        searchStartTime = System.currentTimeMillis();
   
        long times = 0;

        Log.println("Root Proof: " + root.proof + " Root DisProof: " + root.disproof);
     
        // Have not yet run out of time.
        timeoutFlag = false;

        // Perform iterative deepening
        while (root.proof != 0 && root.disproof != 0
                && ProofNode.nodeCounter <= memMaxNodes && countNodes <= maxNodes
                && (!timeoutFlag)) {
            times++;
        
            // Second Part of the algorithm
            final ProofNode mostProvingNode = selectMostProvingNode(currentNode, root);

            expandNode(mostProvingNode);
            currentNode = updateAncestors(mostProvingNode, root);
        
            if (times % 1000 == 0) {
                Log.println("Tree size in nodes: " + ProofNode.nodeCounter);
            }
        }
        duration = System.currentTimeMillis() - searchStartTime;
        
        Evaluator.parametrise(true);
   
        return getMove(root);
    }
   
    protected final Move getMove(ProofNode root) {
    
        Log.println("Total Time: " + duration);
   
        Log.println("Total Nodes visited: " + countNodes);
        Log.println("Total Nodes: " + ProofNode.nodeCounter);
     
        Log.println("Nps: " + (long) (1000.0 * countNodes / duration));
      
     /*   ProofNode n = root.children;

        if (root.side > 0) { // Max Node
            while (n.proof != root.proof) {
                n = n.sibling;
       
            }
        } else { // Min Node
            while (n.disproof != root.disproof) {
                n = n.sibling;
            }
        }*/
  
        final Move resultmove = makeMove(selectBestNode(root));
  
        if (root.proof == 0 && root.side > 0){ // root.value= Node.TRUE;
            Log.println("Black wins");
            resultmove.score = Board.B_WIN_SCORE;
        } else if (root.disproof == 0 && root.side < 0){ // root.value = Node.FALSE;
            Log.println("White wins");
            resultmove.score = Board.W_WIN_SCORE;
        } else if (root.proof == ProofNode.INFINITY && root.side > 0){ // root.value= Node.TRUE;
            Log.println("First player to lose, White wins");
            resultmove.score = Board.W_WIN_SCORE;
        } else if (root.disproof == ProofNode.INFINITY && root.side < 0){ // root.value = Node.FALSE;
            Log.println("First player to lose, Black wins");
            resultmove.score = Board.B_WIN_SCORE;
        } else       
        { // root.value = Node.UNKNOWN;
            Log.println("I don't know");
              if ((root.proof == 0 && root.side < 0)
                || (root.disproof == 0 && root.side > 0)) {
            resultmove.score = root.side * Board.DRAW_SCORE;
            Log.println("DisProven " + resultmove.score);
        }else
        	resultmove.score=0;
        }
        
        Log.println(resultmove.toString());
        Log.println(
                "Root Proof: " + root.proof + " Root DisProof: " + root.disproof);
    
        return resultmove;
    }
    
    private Move makeMove(ProofNode good) {
    	 
        final Move move_list = generator.generateMoves(bord,bord.sideToMove);
        boolean quit =false;
          
        for(Move m = move_list; m!=null ; m=m.next){
        	m.apply(bord);
        	quit = (good.posB == bord.posB) && (good.posR==bord.posW);
            m.undo(bord);
            if(quit) return m;
        }
            
        return null;
    }
    
    public final ProofNode proofNumberSearch(ProofNode root, long maxnodes) { 
    	
    	return proofNumberSearch(root, maxnodes, false, ProofNode.INFINITY);
    }
    
    public final ProofNode proofNumberSearch(ProofNode root, long maxnodes, boolean isTimes) { 
    	
    	return proofNumberSearch(root, maxnodes, isTimes, ProofNode.INFINITY);
    }
    
    public final ProofNode proofNumberSearch(ProofNode root, long maxnodes,  int threshold) { 
    	
    	return proofNumberSearch(root, maxnodes, false, threshold);
    }           
           
    public final ProofNode proofNumberSearch(ProofNode root, long maxmemnodes, boolean isTimes, int threshold) {    

        /* Board b = new Board(root);         
         if(sbe.Winner_Yes)
         TranspositionTable.setHashValue(b, root.Side);*/
    
        // Evaluate(b,root);
    	if(root.children==null){
	        root.proof = 1;
	        root.disproof = 1;
	     }
        
        ProofNode currentNode = root;
                 
        while (root.proof != 0 && root.disproof != 0
                && ProofNode.nodeCounter <= maxmemnodes && ProofNode.nodeCounter <= memMaxNodes
                && countNodes <= maxNodes && (!timeoutFlag)  ){
                     
            final ProofNode mostProvingNode = selectMostProvingNode(currentNode, root);

            expandNode(mostProvingNode); 
            currentNode = updateAncestors(mostProvingNode, root);              
        }
         
        return root;
    }
    
   
          
    protected final void setProofAndDisproofNumbers(ProofNode node) {
               
        if (node.expanded) { // internal node;
            if (node.side < 0) // Min ProofNodeProofNode
            {
                node.proof = 0;
                node.disproof = ProofNode.INFINITY;
                for (ProofNode n = node.children; n != null; n = n.sibling) {
         
                    if (n.disproof < node.disproof) {
                        node.disproof = n.disproof;
                    }
          
                    if (n.proof + node.proof >= ProofNode.INFINITY-1) {
                        if (node.disproof == 0) {
                            node.proof = n.proof;
                            return;
                        } else {
                            node.proof = ProofNode.INFINITY - 2;
                        }
                    } else {
                        node.proof +=  n.proof;
                    }
          
                }
            } else // Max node
            {
                node.proof = ProofNode.INFINITY;
                node.disproof = 0;
                for (ProofNode n = node.children; n != null; n = n.sibling) { 
                    if (n.proof < node.proof) {
                        node.proof = n.proof;
                    }
           
                    if (n.disproof + node.disproof >= ProofNode.INFINITY-1) { 
                        if (node.proof == 0) {
                            node.disproof = n.disproof;
                            return;
                        } else {
                            node.disproof = ProofNode.INFINITY - 2;
                                         
                        }
             
                    } else {
                        node.disproof +=  n.disproof;
                    }  
         
                }
           
            }
        }
     
    }   
    
   
    
protected void setWeakProofAndDisproofNumbers(ProofNode node) {    	   
        
        if (node.expanded) { // internal node;
        	 int k =0;
        	
        	if (node.side < 0) // Min ProofNodeProofNode
            {
                node.proof = 0;
                node.disproof = ProofNode.INFINITY;
                                
                for (ProofNode n = node.children; n != null; n = n.sibling) {
         
                    if (n.disproof < node.disproof) {
                    	
                        node.disproof = n.disproof;
                        
                        if (node.disproof == 0) {
                            node.proof = n.proof;
                            return;
                         }
                     }
                    
                    if(n.proof !=0){
                    	k++;
                    
                        if (n.proof > node.proof) 
                           node.proof = n.proof;                    
                    }
                }
                     
                if(node.disproof < ProofNode.INFINITY-1){
                   node.proof = node.proof+ k-1;
                   
                   if(node.proof >= ProofNode.INFINITY-1)
                    	node.proof = ProofNode.INFINITY - 2;
                    }
          
                
            } else // Max node
            {
                node.proof = ProofNode.INFINITY;
                node.disproof = 0;
                for (ProofNode n = node.children; n != null; n = n.sibling) { 
                    if (n.proof < node.proof) {
                    	 node.proof = n.proof;
                           if(node.proof == 0) {
                              node.disproof = n.disproof;
                            return;                        
                           }
                    }
                    
                    if(n.disproof !=0){
                    	k++;
                    
                        if (n.disproof > node.disproof) 
                           node.disproof = n.disproof;                    
                    }                 
                }  
                
                if(node.proof < ProofNode.INFINITY-1){
                    node.disproof = node.disproof+ k-1;
                    
                    if(node.disproof >= ProofNode.INFINITY-1)
                     	node.disproof = ProofNode.INFINITY - 2;
                     }                   
            }
        }   
     
    }            
    
         
    protected final ProofNode selectMostProvingNode(ProofNode node, ProofNode root) { 
   
        while (node.expanded) { 
           
          node = selectBestNode(node);  
        }      
        return node;
    }
    
    
    private ProofNode selectBestNode(final ProofNode node){
    	    	
    	  ProofNode n = node.children;
          
          /*if(gameAI.isLMR){
       	   
   
          ProofNode min = n;
       	   
       	  if (node.side > 0) { // Max Node
                   for(n = n.sibling; n!=null;n = n.sibling ) {
                       if(n.proof < min.proof  || (n.proof == min.proof  && n.disproof>min.disproof))
                       	min = n;
                        
                   }
               } else { // Min Node
               	   for(n = n.sibling; n!=null;n = n.sibling ) {
                          if(n.disproof < min.disproof || (n.disproof == min.disproof  && n.proof>min.proof))
                          	min = n;
                      }
               }
       	   return min;
          }else*/{
     
           if (node.side > 0) { // Max Node
               while (/* n.sibling!=null &&*/n.proof != node.proof) {
                   n = n.sibling;

                   /* if(n == null)
                    Log.Log.Log.println("Max Node" + node.proof + " "  + node.disproof);*/}
           } else { // Min Node
               while (/* n.sibling!=null && */n.disproof != node.disproof) {
                   n = n.sibling;
      
                   /* if(n == null)
                    Log.Log.Log.println("Min Node" + node.proof + " "  + node.disproof);*/}
           }
           
           return n;
          }   	
    }
   
    protected final void expandNode(ProofNode node) {        
   
        generateAllChildren(node);      
         node.expanded = true; 
    }
     
    protected final ProofNode updateAncestors(ProofNode node, ProofNode root) {

        
        int oldProof = node.proof;
        int oldDisProof = node.disproof;
     
       	setProofAndDisproofNumbers(node);

        if (node.proof == oldProof && node.disproof == oldDisProof) {
            return  node;
        }
        
        if ((node.proof == 0 || node.disproof == 0) && node.parent != null
                && (deleteSecond || node.firstLevel)) {
            node.deleteSubtree();
         
            if (gameAI.isWonTable) {          
                storeNode(node);
            }
        }
    
       
        while (node != root) {  
            node = node.parent;
                   
            oldProof = node.proof;
            oldDisProof = node.disproof;
       
           	setProofAndDisproofNumbers(node);
       
            if (node.proof == oldProof && node.disproof == oldDisProof) {
                return  node;
            }
        
            if ((node.proof == 0 || node.disproof == 0) && node.parent != null) // && node.first_level)
            {
                node.deleteSubtree();
                if (gameAI.isWonTable) {  
                    storeNode(node);
                }
            }

            /* else
             if(sbe.Winner_Yes)
             ptable.Store(TranspositionTable.setHashValue(node),node);    */   
       
        } 
        return node;
    }
    
    protected final void evaluate(Board b, ProofNode node) {
    	
    	evaluate( b,  node, 0);
    }
  
    private void evaluate(Board b, ProofNode node, double lazyScore) {
        if (gameAI.isWonTable) {
            final int score = winTable.retrieve(b.hashValue);

            if (Math.abs(score) >= Board.WIN_SCORE) {
                if (checkDraw(node)) {
                    return;
                }
              
                if (score > 0) {
                    node.proof = 0;
                    node.disproof = ProofNode.INFINITY;
                } else {
                    node.proof = ProofNode.INFINITY;
                    node.disproof = 0;
                }
              
                return;
            }

        }
      
        countNodes++;
  
        int score =  gameAI.gameTheoreticEvaluation(b, -node.side);
              
        if(gameAI.isEGDB  && score == 0 && node.parent !=null && egdb!=null && gameAI.hasDatabase(b)){   	   
      	            	
         	  try {
         		int info = egdb.getInfo(b);
         		
         	
         		if(info!=0){
         		 egdbCounter++;
      
         		if(info==1)
         			score =node.side*Board.WIN_SCORE;
              	else
         		  if(info==2)
         			score =-node.side*Board.WIN_SCORE;
         		else score = Board.DRAW_SCORE;
             	
         	} 
         	  }catch (IOException e) {
         		e.printStackTrace();
         	}        
        }
        
        if (score == Board.DRAW_SCORE) {
            node.proof = drawValue.proof;
            node.disproof = drawValue.disproof;
            
            return;     
                 
        }    
          
        if (Math.abs(score) == Board.WIN_SCORE) { 
            if (score > 0) {
                node.proof = 0;
                node.disproof = ProofNode.INFINITY;
            } else {
                node.proof = ProofNode.INFINITY;
                node.disproof = 0;
            }
        } else if (checkDraw(node)) {
            return;
        }/* else   if (isKnowledge)// Evaluations as a measure of effort
        {
            if (score > 0) {
                node.proof = 1;
                node.disproof = ((score/100)+1);
            } else {
                node.proof = ((-score/100)+1);
                node.disproof = 1;
            }
        	
        }*/
        else
        if (isMoveEvaluation) {
                     
            final int counter =  gameAI.getEvaluator().getNumberOfMoves(b, node.side);          
            if (counter == 0) {
                /*if (node.side > 0) {
                    node.proof = ProofNode.INFINITY;
                    node.disproof = 0;
                } else {
                    node.proof = 0;
                    node.disproof = ProofNode.INFINITY;
                }*/
            	return;
            }
          
            if (node.side > 0) { // Max node
            	
            	  node.disproof = counter;
            	
                	if (gameAI.isKnowledgePN){
                     //if(gameAI.isFractional)
                    		// lazyScore = gameAI.reward(gameAI.heuristicEvaluation(b, -node.side));	
                    	// lazyScore = gameAI.reward(node.side*selectReplyMove(b,-Board.WIN_SCORE,Board.WIN_SCORE,2));
                		
               		 node.proof += (int) (aK -  aK*lazyScore);
               		 //node.disproof += (int) (20 +  20*lazyScore);
               		 node.disproof += (int) (node.disproof * (bK +  bK*lazyScore));            		 
               	}       
               } else {
               	
               	 node.proof = counter;
               	
               	if (gameAI.isKnowledgePN ){
               		//if(gameAI.isFractional)
               		 //lazyScore = gameAI.reward(gameAI.heuristicEvaluation(b, -node.side));	
               		// lazyScore = gameAI.reward(node.side*selectReplyMove(b,-Board.WIN_SCORE,Board.WIN_SCORE,2));
               		
              		   node.disproof += (int) (aK + aK*lazyScore );
              		// node.proof += (int) (20 -  20*lazyScore);
              		   
              		 node.proof += (int) (node.proof *(bK  - bK*lazyScore));           		   
               	}   
               }
            
        }
        else 	if (gameAI.isKnowledgePN ){
        	
        	lazyScore = gameAI.reward(gameAI.heuristicEvaluation(b, -node.side));         	
        	
       
             		 node.proof += (int) (aK -  aK*lazyScore);
             		 node.disproof += (int) (aK +  aK*lazyScore);           		 
             	    
                    	
        }
    }
    
    private int [] WeightTable;
    private Move[][] moveRepArray = new Move[4][100];
    private int selectReplyMove(Board b, int alpha, int beta, int depth){	
    	
    	/*if(gameAI.epsilonGreedy && randomMCValue() <=0.05)
    	  return selectKnowledgeMemRandomMove( b, list);*/

          
    		final int side = b.sideToMove;
    		int max = side*gameAI.evaluate(b, -side);
    		
    		if(depth == 0 || max<=-Board.WIN_SCORE || max >=Board.WIN_SCORE || max == side*Board.DRAW_SCORE)
    			return max;
    		
  
    		    	
    		Move list = generator.generateMoves(b, side);
    		
    		Move [] mArray = moveRepArray[depth];
    								
    		int counter =0;
    	     for(Move m = list; m!=null; m=m.next){
    			  m.score=WeightTable[m.getMoveType(b)];
    			  mArray[counter++] = m;
    		 }     
    	     
    	 		
    	    final int cutOff = (counter < 5 || depth ==2) ? counter: 5;
    		
    	  	for(int i = 0; i < cutOff; i++){	
    	  		
    	  		int maxValue =  mArray[i].score;
    	  		int maxIndex = i;
    	  		
    	  	 	for(int j = i+1; j< counter; j++){
    	  	 		
    	  	 		if(maxValue< mArray[j].score){
    	  	 			maxValue =  mArray[j].score;
    	  	 			maxIndex = j;
    	  	 		}
    	  	 		
    	  	 	}
    	  		
    	  		Move m = mArray[maxIndex];	
    	  		
    	  		  			
    					m.apply(b);
    					int value =  -selectReplyMove( b,-beta, -alpha,depth-1);
    					m.undo(b);
    					
    					if(value > beta){
    						generator.recycle(list);
    						
    						return value;}			
    				
    					if(value>max){
    						
    						max = value;	
    				}
    					
    					 mArray[maxIndex] =  mArray[i];
    	  	}
    	  	
   
    		generator.recycle(list);
    		return max;			
    	}	
    
     
    private  void generateAllChildren(ProofNode node) {
        final Board b = hulpbord; // new Board(node);
    
        double lazyScore=0;
        if(gameAI.isKnowledgePN && this.isMoveEvaluation){
        	 b.setSpace(node.posB, node.posR);
        	 b.initialise(node.side);
        	 
        	 if(!gameAI.isFractional)
        		 lazyScore = gameAI.reward(gameAI.heuristicEvaluation(b, -node.side)); 
        	 else
        		 lazyScore = node.side*gameAI.reward(node.side*selectReplyMove(b,-Board.WIN_SCORE,Board.WIN_SCORE,2));
        		 
        }
        else  if(gameAI.isKnowledgePN){
        	b.setSpace(node.posB, node.posR);
        	b.initialise(node.side);} 
        else
        b.setBoard(node);
        
        if (gameAI.isWonTable) {
        	TranspositionTable.setHashValue(b, node.side);
     
            final int score = winTable.retrieve(b.hashValue);
      
            if (Math.abs(score) >= Board.WIN_SCORE) {
                ProofNode n = new ProofNode();
              
                node.children = n; 
             
                n.parent = node;
                if (score > 0) {
                    n.proof = 0;
                    n.disproof = ProofNode.INFINITY;
                } else {
                    n.proof = ProofNode.INFINITY;
                    n.disproof = 0;
                }
                return;
            }
     
        }
    
        Move move_list = generator.generateMoves(b, node.side);
        ProofNode n = null;
       
        if (move_list != null) {
            move_list.apply(b);
            n = new ProofNode(b, -node.side);
            node.children = n;
            n.parent = node;
            evaluate(b, n, lazyScore);
            move_list.undo(b);
          //  move_list = move_list.next;
            // search_counter++;
            for (Move m = move_list.next; m != null; m = m.next) {
     
                // Kind of alpha beta cut-off...
                if ((node.side > 0 && n.proof == 0)
                        || (node.side < 0 && n.disproof == 0)) {
                    break;
                }
     
                // search_counter++;
       
                // Apply the move.
                m.apply(b);
                final ProofNode newn = new ProofNode(b, -node.side);

                n.sibling = newn;
                n = n.sibling;
                n.parent = node;
                evaluate(b, newn,lazyScore);
                m.undo(b);
                // undo the move
      
                if ( (timeCheck ) && (countNodes & 1023L) == 0) {
                    long hulptime = System.currentTimeMillis()
                            - searchStartTime;
            
                    if (hulptime > targetTime) {
                        timeoutFlag = true;
                    }
                }  
        
            }
        } else { // pass
            n = new ProofNode(b, -node.side);
            node.children = n;
            n.parent = node;
        }   
 
        generator.recycle(move_list);    
    }
  
    private void storeNode(ProofNode node) {
        // Win2LevelTable tab = (Win2LevelTable) win_table;
        if (node.proof == 0 && node.proof != drawValue.proof) {
            winTable.store(TranspositionTable.getHashValue(node),
                    Board.WIN_SCORE);
        } // ,node.first_level);
        else if (node.disproof == 0 && node.disproof != drawValue.disproof) {
            winTable.store(TranspositionTable.getHashValue(node),
                    -Board.WIN_SCORE);
        }// ,node.first_level); 
    }
    
    private boolean checkDraw(ProofNode node) { 
        ProofNode n = node;
 
        int counter = 0;
 
        while (true) {
            if (n.parent != null) {
                n = n.parent;
       
                if (n.parent != null) {
                    n = n.parent;
                    if (node.posB == n.posB && node.posR == n.posR) {
                        counter++;
           
                        if (counter == 2) {
                            node.proof = drawValue.proof;
                            node.disproof = drawValue.disproof;
                            return true;
                        }
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
   
        }
 
    }
    
    
  
    public void  setDrawValue(ProofNode root) {
        if (root.side > 0) {
            drawValue.proof = ProofNode.INFINITY-1;
            drawValue.disproof = 0;
        } else {
            drawValue.proof = 0;
            drawValue.disproof = ProofNode.INFINITY-1;
        } 

    }
    
    
   /* private static double reward(double x) {
    return  (2 / (1 + Math.exp(-2 * 0.005 * x)) - 1);}
    
    private static final double [] expTable = new double[500];
    
    
    private static double tanh(int x) {
    	if(x >= expTable.length)
    		return 1;
    	
    	if(x<=-expTable.length)
    		return -1;
    
    if(x>=0)
        return expTable[x]; 
    else
    	 return -expTable[-x]; 
    
    }
    
    
    private static final double W = 500;
    private static final double G = 300;
    
    private static double tanh1(int x) {
    	if(x >= W)
    		return 1;
    	
    	if(x<=-W)
    		return -1;
    	
    	
    if(x>=-G && x<=G)
    	return 0;
    
    if(x>=0)
        return x/((W-G)) - W/(W-G); 
    else
    	 return x/((W-G)) +  W/(W-G); 
    
    }
    
    static{
    	
    	for(int i =0; i<expTable.length;i++ )
    		expTable[i] = reward(i);
    }

    public static void main(String args[]) {
    	
    	   System.out.println(tanh1(500));
       	
       	
       	System.out.println(tanh1(100));
       	
    	System.out.println(tanh1(400));
    	
    	
    	System.out.println(tanh1(-400));
    	
    	/*

    	
       System.out.println(tanh(500));
    	
    	
    	System.out.println(tanh(100));
    	
    	System.out.println(reward(0));
    	
    	System.out.println(reward(-1000));
    	
    	System.out.println(reward(10000));*/
    	
    /*	long hulp = System.currentTimeMillis();
    	for(int i=0 ;i <10000000; i++)
    		reward(i);
    	
    	System.out.println( System.currentTimeMillis()-hulp);
    }*/

}
