public class PNSSquarePlayer extends PNSPlayer {  
	
	public boolean isBreuker = false;
	
	public boolean isTheshold = false;
	public boolean isLeastDeleted = false;
 
    public long savedNodes = 0;
   
    private double parA = 2 * ProofNode.MAX_NODES, parB = 8
            * ProofNode.MAX_NODES / 30;
    
    private ProofNode focusNode = null;

    protected  PNSSquarePlayer() {   
    }
    
    public PNSSquarePlayer(Game eval) {
        super(eval);
       // Log.println(parA + " " + parB);
    }
    
    public void  setMemMaxNodes(int max) {
   
        super.setMemMaxNodes(max);
   
        parA = 2 * memMaxNodes;
        parB = 8 * memMaxNodes / 30;
    }  
    
    public Move searchMove(Board b, Move m){
    	
        Log.println("Focus Move: " + m);
    	m.apply(b);
    	focusNode = new ProofNode(b, b.sideToMove);
        evaluate(b, focusNode);
    	m.undo(b);
    	Move back = search(b);
    	focusNode = null;
    	return back;
    }
     
    public Move  search(Board b) {
       
       Evaluator.parametrise(false);
    	bord = b; 
    	final int side = b.sideToMove;
        ProofNode.nodeCounter = 0;
        ProofNode root; 
        
    	initialiseDatabase();
    	egdbCounter = 0;
    
        countNodes = 0;
        duration = 0;
    
        timeoutFlag = false;
    
        // Get a snapshot of the current time.
        searchStartTime = System.currentTimeMillis();
    
        root = new ProofNode(b, side);
        setDrawValue(root);
    
        ProofNode.firstLevelCount = 1;
        ProofNode.secondLevelCount = 0;
        root.firstLevel = true;
    
        evaluate(b, root);
        delayedExpandNode(root);
    
        pn2search(root);  
        duration = System.currentTimeMillis() - searchStartTime;
        
      /*  for(long[] e : stat)
        	System.out.print(" " + (1.0*e[1])/e[0]);
        
        System.out.println();*/
        
        if(egdb != null)
        	Log.println("EGDB Counter: " + egdbCounter);
        
        
        Evaluator.parametrise(true);
     
        return getMove(root);  
    }
  
    protected final void pn2search(ProofNode root) {
   
        ProofNode currentNode = root;
   
        long times = 0;
       
        if(focusNode!=null){
        	for(ProofNode n = root.children; n!=null; n=n.sibling)  
        		if(!focusNode.equals(n)){
        		        	
        			      if (n.side == ProofNode.OR_NODE) {
        			    	      			    	 
        	                    n.proof = 0;
        	                    n.disproof = ProofNode.INFINITY;
        	                } else {
        	                    n.proof = ProofNode.INFINITY;
        	                    n.disproof = 0;
        	                }
        		
        }
        	updateAncestors(root, root);
        }
        else if(root.children!=null && this.checkSymmetry)
        	for(ProofNode n = root.children; n!=null; n=n.sibling) {  
        		if(n.proof !=0 && n.disproof!=0)
        		for(ProofNode k = n.sibling; k!=null; k=k.sibling){
        			if( ((n.posB == Symmetry.mirrorX(k.posB) && n.posR == Symmetry.mirrorX(k.posR)) || 
        					(n.posB == Symmetry.mirrorY(k.posB) && n.posR == Symmetry.mirrorY(k.posR)) || (n.posB == Symmetry.mirrorY(Symmetry.mirrorX(k.posB)) && n.posR == Symmetry.mirrorY(Symmetry.mirrorX(k.posR)))) )
        			      if (n.side == ProofNode.OR_NODE) {
        			    	//  System.out.println(++counter);
        			    	 
        	                    k.proof = 0;
        	                    k.disproof = ProofNode.INFINITY;
        	                } else {
        	                //	System.out.println(++counter);
        	                //	 System.out.println(k.posB + " " + k.posR);
        	                    k.proof = ProofNode.INFINITY;
        	                    k.disproof = 0;
        	                }
        		}
        	}
        
 //System.out.println(counter +" "+ root.proof);
      
        while (root.proof != 0 && root.disproof != 0
                && ProofNode.nodeCounter <= memMaxNodes && countNodes <= maxNodes
                && (!timeoutFlag)) // && times < 1)
        {
            times++;
      
            final ProofNode mostProvingNode = selectMostProvingNode(currentNode, root);

            if(isLeastDeleted && times>=100 && ProofNode.nodeCounter < memMaxNodes  
            	&& (times%2)==0){
            		
            		final ProofNode leastProvingNode = selectLeastProvingNode(root);
            		if(leastProvingNode.parent!=null && leastProvingNode.parent!=root && leastProvingNode.parent!=mostProvingNode.parent )
            		{
            			ProofNode gone = leastProvingNode.parent;
            			gone.deleteSubtree();
            			gone.proof = ProofNode.INFINITY -2;
            			gone.disproof = ProofNode.INFINITY -2;
            		    gone.expanded = false;
            			         			
            		}
            		 		
            	}
            
            delayedExpandNode(mostProvingNode);
            currentNode = updateAncestors(mostProvingNode.parent, root);
     
            if (times % 1000 == 0) {
                Log.println(
                        "Tree Size in Nodes: " + ProofNode.nodeCounter + " Pn: "
                        + root.proof + " Dpn:  "
                        + root.disproof + " " + ((egdb!=null)?"EGDB Counter: " + egdbCounter : ""));
            }
        }
    }
   
    protected final double fraction(int x) {
        return (isBreuker)?((1.0 / (1 + Math.pow(Math.E, (parA - x) / parB)))):1;
    }
   
    protected void delayedExpandNode(ProofNode node) {  
    	
    if(isTheshold &&  node.parent!=null){
    	int max =0;
        if (node.side ==ProofNode.AND_NODE) { // Max Node       	
        	
           for(ProofNode n = node.parent.children; n!=null; n=n.sibling)
               if(max<n.proof)
            	   max = n.proof;
        } else { // Min Node
            for(ProofNode n = node.parent.children; n!=null; n=n.sibling)
                if(max<n.disproof)
             	   max = n.disproof;
        }
        
        node = proofNumberSearch(node,
                (int) (ProofNode.firstLevelCount
                * fraction(ProofNode.firstLevelCount)
                        + ProofNode.nodeCounter + 1), 2*max);
    }
    else   node = proofNumberSearch(node,
                (int) (ProofNode.firstLevelCount
                * fraction(ProofNode.firstLevelCount)
                        + ProofNode.nodeCounter + 1));
        // node=ProofNumberSearch(node,(int) (  ProofNode.Count*Fraction( ProofNode.Count) + ProofNode.Count + 1));
           
        for (ProofNode n = node.children; n != null; n = n.sibling) {
            n.deleteSubtree();
            n.expanded = false;
       
            n.setFirstLevel();      
        }
    }
    
    protected final ProofNode selectLeastProvingNode(ProofNode node) { 
    	   
        while (node.children !=null) { 
   
            int max =0;
            ProofNode best =null;
           
            if (node.side > 0) { // Max Node
            	for(ProofNode n = node.children; n!=null; n=n.sibling) {
            		if(max<n.proof&& n.disproof>0){
            			max = n.proof;
            			best=n;
            		}
            	}
            } else { // Min Node
            	for(ProofNode n = node.children; n!=null; n=n.sibling) {
            		if(max<n.disproof && n.proof>0)
            		{
            			max = n.disproof;
            			best=n;
            		}            	
            	}
            }

            node = best;     
        }
    
        return node;
    }
   
   
}
