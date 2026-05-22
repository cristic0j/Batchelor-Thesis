import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;


public class ParPNSSquarePlayer extends PNSSquarePlayer {
	
	private final PNSSquarePlayer [] playerList;
	private final ThreadSearch [] threadList;
	
	public void quitSearch() {
		
		super.quitSearch();
		for(int i =0; i < playerList.length; i++)
			playerList[i].quitSearch();
		
	}

	public ParPNSSquarePlayer(Game ai, int proc){
		super(ai);
		
		gameAI = ai;
		final int shift = (int)(Math.log(proc)/Math.log(2));
		playerList = new PNSSquarePlayer[proc];
		threadList = new ThreadSearch[proc];	
		
		   
	   try {
		   
		   Field [] list = ai.getClass().getFields();
		
			for(int i =0; i<proc;i++){
				
					Game g = ai.getClass().getDeclaredConstructor().newInstance();
					
					g.setEvaluator( ai.getEvaluator().getClass().getDeclaredConstructor().newInstance());
					
					for(int j = list.length; --j>=0;){
					if(list[j].getType().isPrimitive() && !Modifier.isFinal(list[j].getModifiers())){
							
							//Log.println(list[j] + " " + list[j].get(ai));
							list[j].set(g,list[j].get(ai));
							//Log.println(list[j] + " " + list[j].get(g));
							
						}
					}
						
					g.sizeTT >>=shift;	
					
					//Log.println("Old: " + ai.sizeTT + " " + ai.isRPS);
					//Log.println("New: " + g.sizeTT + " " + g.isRPS);
					
					playerList[i] = new PNSSquarePlayer(g);
					threadList[i] = new ThreadSearch(playerList[i]);
					
			}
	   
	   }catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException e) {
				
				e.printStackTrace();
			} 	
	}

	public Move search(Board b) {
	
		ProofNode root = new ProofNode(b);
		for(int i= playerList.length;--i>=0;)
			 playerList[i].setDrawValue(root);
		 return super.search(b);
	}
	
	   protected void delayedExpandNode(ProofNode node) {  

		   int frac =  (int) (ProofNode.firstLevelCount
	                * fraction(ProofNode.firstLevelCount));
                
		   
		   if(frac<1000  || (memMaxNodes-ProofNode.nodeCounter-1000<0)){
			   super.delayedExpandNode(node);
			   
		   }else{
              frac=  (frac + ProofNode.nodeCounter + 1);
			   
			   //super.expandNode(node);
              
              super.proofNumberSearch(node,  ProofNode.nodeCounter + 1000, false);
              
              if(node.children==null)
            	  return;
			  
              if((node.side == ProofNode.OR_NODE &&node.proof>node.disproof) || (node.side == ProofNode.AND_NODE &&node.proof<node.disproof)){
				   ThreadGroup tGroup = new ThreadGroup("Parallell Player");
				   ProofNode [] nodes = new ProofNode[threadList.length];
				   nodes[0]= node;
				   
				   for(int i= nodes.length;--i>=1;){
					   nodes[i] =  (ProofNode)node.clone();
					   nodes[i].children =null;
					  
				   }
				   
				   
				   while (node.proof != 0 && node.disproof != 0
			                && ProofNode.nodeCounter <= frac && ProofNode.nodeCounter <= memMaxNodes
			                && countNodes <= maxNodes   && (!timeoutFlag)) {
					 
					    this.split(nodes);  	
					    
						for(int i = 0; i<threadList.length; i++){
							playerList[i].countNodes = countNodes;
							
							this.setProofAndDisproofNumbers(nodes[i]);
							if(nodes[i].proof==0 || nodes[i].disproof==0)
								continue;
							Thread t = new Thread(tGroup,threadList[i]);
							threadList[i].setRoot(nodes[i],frac);
							t.start();
						}				
						
					   synchronized (tGroup) {
							    while (tGroup.activeCount() > 0) {
								try {
									tGroup.wait();
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							    }
						    }
					   
					 /*  for(int i = 0; i<threadList.length; i++){
							if(nodes[i].children==null){
								System.out.println("How bizar" + nodes[i].proof);
								System.exit(0);}
					   }*/
					   
					   this.merge(nodes);
					   
					   long old = countNodes;
					   for(int i=playerList.length;--i>=0;)
						   countNodes+=playerList[i].countNodes-old;
					   
					   
					   
					   //this.selectMostProvingNode(node, null);
					  
				   }
		        // node=ProofNumberSearch(node,(int) (  ProofNode.Count*Fraction( ProofNode.Count) + ProofNode.Count + 1));
				   //this.selectMostProvingNode(node, null);  
		        for (ProofNode n = node.children; n != null; n = n.sibling) {
		            n.deleteSubtree();
		            n.expanded = false;
		       
		            n.setFirstLevel();
		        }
				   //this.selectMostProvingNode(node, null);
		        
		        //System.out.println("test");
				 
              }				   
		   else
		   {
			   node = proofNumberSearch(node,
		                frac);
		        // node=ProofNumberSearch(node,(int) (  ProofNode.Count*Fraction( ProofNode.Count) + ProofNode.Count + 1));
		           
		        for (ProofNode n = node.children; n != null; n = n.sibling) {
		            n.deleteSubtree();
		            n.expanded = false;
		       
		            n.setFirstLevel();
		      
		        }
			   
		   }
	      }
	    }
	   
	private void merge(ProofNode [] nodes){
		
		ProofNode old=null;// = nodes[nodes.length-1].children;
		for(int i= nodes.length;--i>=1;){
			ProofNode node = nodes[i].children;
			if(old==null)old=nodes[i].children;
			if(nodes[i].children==null)continue;
			while(true){
				node.parent = nodes[0];
				if(node.sibling!=null)
				 node = node.sibling;
				else
					break;
			}
			
			 nodes[i].children =null;			
			node.sibling = nodes[i-1].children;
		}
		
		if(old!=null) nodes[0].children = old;
		
		this.setProofAndDisproofNumbers(nodes[0]);
	}
	
    private ProofNode getBestNode(ProofNode list) {

        if (list.sibling == null) {
            return list;
        }

        ProofNode max = list;
        ProofNode previous = list, old = null;

        for (ProofNode m = list.sibling; m != null; m = m.sibling) {
            if (max.proof < m.proof) {
                max = m;
                old = previous;
            }
            previous = m;
        }
   
        if (old != null) { 
            old.sibling = max.sibling;
            max.sibling = list;
        }

        return max;
    }
	
    private ProofNode sort(ProofNode list) {
        if (list == null) {
            return null;
        }

        list = getBestNode(list);
        ProofNode old = list;
  
        for (ProofNode m = list.sibling; m != null; m = m.sibling) {
            m = getBestNode(m);
            old.sibling = m;
            old = m;
        }  
        return list;
    }
	
	private void split(ProofNode [] nodes){
		
		int i=0;
		nodes[0].children = sort(nodes[0].children);
		ProofNode node = nodes[0].children;
		nodes[0].children=null;
		
		while(node!=null){
			ProofNode next = node.sibling;
			i=i%this.playerList.length;
			node.parent = nodes[i];
		    node.sibling = nodes[i].children;
			
			nodes[i].children = node;
			i++;			
			node = next;			
		}	
	}
	   
	  
	
	private class ThreadSearch implements Runnable{
		
		private final PNSSquarePlayer player;
		private int maxnodes;
		
		private ProofNode root;
		
		
		public ThreadSearch(PNSSquarePlayer p){
			
			player = p;			
		}
		
		public void setRoot(ProofNode node, int max){
			
			root = node;
			maxnodes = max;
			player.timeoutFlag =false;
		}

		public void run() {
							
			player.proofNumberSearch(root, maxnodes,true);	
			
			if((root.side == Board.B_SIDE && root.proof==0) ||(root.side == Board.W_SIDE && root.disproof==0))
				for(int i = playerList.length; --i>=0;){
					if(playerList[i]!=player)
						playerList[i].quitSearch();
				}
		}
		
	}

}
