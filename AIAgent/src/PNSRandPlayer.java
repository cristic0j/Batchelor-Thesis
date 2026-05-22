import java.io.IOException;

// import java.io.*;

public class PNSRandPlayer extends  AbstractPlayer {  
    
	
    protected int depth;
 
    // private ProofBigTable ptable;
  
    private final ProofNode drawValue = new ProofNode();  
  
    public static boolean deleteSecond = true;
  
    private final Board hulpbord = new Board(new ProofNode());
  
    protected int memMaxNodes = ProofNode.MAX_NODES;
 
    private boolean isKnowledge = false;
    
    
    PNSRandPlayer() {}

    PNSRandPlayer(Game eval) { 
   
        gameAI = eval; 
        generator = gameAI.getGenerator();
         
        if (gameAI.isWonTable) {
            winTable = new WinTable(14);
            // ptable = new ProofBigTable(14); 
        }
        
      	initialiseDatabase();
    }
 
    public void setMemMaxNodes(int max) {
        memMaxNodes = max;
    }
 
    public Move search(Board b) {
    	
    	initialiseDatabase();
    	
    	egdbCounter=0;
        bord = b; 
        ProofNode root;
   
        ProofNode.nodeCounter = 0;
  
        depth = 0;
    
        countNodes = 0;
        duration = 0;
        
        final int side = b.sideToMove;

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
    
            /* if( times% 100 ==0)
             duration = System.currentTimeMillis() - search_start_time  ;*/
      
        }
        duration = System.currentTimeMillis() - searchStartTime;
   
        return getMove(root);
    }
   
    protected final Move getMove(ProofNode root) {
    
        Log.println("Total Time: " + duration);
   
        Log.println("Total Nodes visited: " + countNodes);
        Log.println("Total Nodes: " + ProofNode.nodeCounter);
     
        Log.println("Nps: " + (long) (1000.0 * countNodes / duration));
      
        ProofNode n = root.children;

        if (root.side > 0) { // Max Node
            while (n.proof != root.proof) {
                n = n.sibling;
       
            }
        } else { // Min Node
            while (n.disproof != root.disproof) {
                n = n.sibling;
            }
        }
  
        final Move resultmove = makeMove(n);
  
        if (root.proof == 0 && root.side > 0) { // root.value= Node.TRUE;
            Log.println("Black wins");
            resultmove.score = Board.B_WIN_SCORE;
        } else if (root.disproof == 0 && root.side < 0) { // root.value = Node.FALSE;
            Log.println("White wins");
            resultmove.score = Board.W_WIN_SCORE;
        } else if (root.proof == ProofNode.INFINITY && root.side > 0) { // root.value= Node.TRUE;
            Log.println("First player to lose, White wins");
            resultmove.score = Board.W_WIN_SCORE;
        } else if (root.disproof == ProofNode.INFINITY && root.side < 0) { // root.value = Node.FALSE;
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

     
    	if(root.children==null){
	        root.proof = 1;
	        root.disproof = 1;
	     }
        
        ProofNode currentNode = root;
        int times =0;
           
    
 
        // Perform iterative deepening
        while (root.proof != 0 && root.disproof != 0
                && ProofNode.nodeCounter <= maxmemnodes && ProofNode.nodeCounter <= memMaxNodes
                && countNodes <= maxNodes && (!timeoutFlag) 
                && !((root.side == ProofNode.AND_NODE && root.proof >threshold) || (root.side == ProofNode.OR_NODE && root.disproof >threshold))&& (!isTimes ||times<10000)) {
           times++;
      
            // Second Part of the algorithm
            final ProofNode mostProvingNode = selectMostProvingNode(currentNode, root);

            expandNode(mostProvingNode); 
            currentNode = updateAncestors(mostProvingNode, root);           
            
        }
   
        return root;
    }
          
    protected void setProofAndDisproofNumbers(ProofNode node) {
   
         
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
         
    protected final ProofNode selectMostProvingNode(ProofNode node, ProofNode root) { 
   
        while (node.expanded) { 

           ProofNode optimal = node.children;
           double max =-1;
      
           
            if (node.side > 0) { // Max Node
            	
            final int minProof = node.proof; 
                for( ProofNode n = node.children; n!=null; n = n.sibling ) {
                   if(n.proof<=minProof+5){
                	   
                	   double valProof = (Game.randomMCValue());
                	   
                	   if(valProof> max){
                		   max= valProof;
                		   optimal = n;
                	   }
                   }  	
                	
                    
                }

            } else { // Max Node
            	
                final int minProof = node.disproof; 
                    for( ProofNode n = node.children; n!=null; n = n.sibling ) {
                       if(n.disproof<=minProof+5){
                    	   
                    	   double valProof = (Game.randomMCValue());
                    	   
                    	   if(valProof> max){
                    		   max= valProof;
                    		   optimal = n;
                    	   }
                       }
                    	
                    }  	
                        
                 }

            node =  optimal;
     
        }
    
        return node;
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

       
        } 
        return node;
    }
  
    protected final void evaluate(Board b, ProofNode node) {
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
  
        int score =  (isKnowledge)?gameAI.evaluate(b, -node.side):gameAI.gameTheoreticEvaluation(b, -node.side);
              
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
        } else   if (isKnowledge)// Evaluations as a measure of effort
        {
            if (score > 0) {
                node.proof = 1;
                node.disproof = ((score/100)+1);
            } else {
                node.proof = ((-score/100)+1);
                node.disproof = 1;
            }
        	
        }
        else
        if (isMoveEvaluation) {
           
            
            final int counter =  gameAI.getEvaluator().getNumberOfMoves(b, node.side);
          
            if (counter == 0) {
              
            	return;
            }
          
            if (node.side > 0) { // Max node
                node.disproof = counter;
            } else {
                node.proof = counter;
            } 
            
        }
           
           
    }
     
    protected final void generateAllChildren(ProofNode node) {
        final Board b = hulpbord; // new Board(node);
    
        if(isKnowledge){
        	 b.setSpace(node.posB, node.posR);
        	 b.initialise(node.side);
        	
        }else
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
            evaluate(b, n);
            move_list.undo(b);
            move_list = move_list.next;
            // search_counter++;
            for (Move m = move_list; m != null; m = m.next) {
     
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
                evaluate(b, newn);
                m.undo(b);
                // undo the move
      
                if (timeCheck && (countNodes & 1023L) == 0) {
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



}
