public class ProofNode implements Cloneable {

    public static final byte FALSE = -1;
    public static final byte UNKNOWN = 0;
    public static final byte TRUE = 1;
    
    public static final int BIT_RANGE = 21;
    protected static final int INFINITY = (1<< BIT_RANGE)-1;
    
    public static final int OR_NODE = 1;
    public static final int AND_NODE = -1;
  
    public static int nodeCounter = 0;
    public static int firstLevelCount = 0;
    public static int secondLevelCount = 0;
  
    public byte side;
    public boolean expanded = false;
    public boolean firstLevel = false;
    public int proof = 1;
    public int disproof = 1;
  
    public ProofNode sibling = null;
    public ProofNode children = null;
    public ProofNode parent = null;
    
 
    public long posB = 0;
    public long posR = 0;
 
    public static final int MAX_NODES = 10_000_000;
  
    public  ProofNode() {
        nodeCounter++;
        secondLevelCount++;
    }
  
    public ProofNode(int s) {
        side = (byte) s;

        nodeCounter++;
        secondLevelCount++;
    }
    
    public ProofNode(Board b, int s) {
        side = (byte) s;
        posB = b.posB;
        posR = b.posW;
  
        nodeCounter++;
        secondLevelCount++;
    }
    
    public ProofNode(Board b) {
      this(b,b.sideToMove);
    }
   
    public void setPosition(long pB, long pR) {
        posB = pB;
        posR = pR;   
    }
    
    public Object clone(){
        
        try {
  		return super.clone();
  	} catch (CloneNotSupportedException e) {
  		throw new Error("ProofNode couldn't be cloned");
  	}
      }
  
   
    public final void setFirstLevel() {
        firstLevel = true;
        secondLevelCount--;
        firstLevelCount++;
    }
        
    public final void deleteSubtree() { 
 
        for (ProofNode n = children; n != null; n = n.sibling) {
            ProofNode.nodeCounter--;
            if (n.firstLevel) {
                firstLevelCount--;
            } else {
                secondLevelCount--;
            }
        
            n.deleteSubtree();
       
            n.parent = null;
        }
      
        children = null;
 
    }
  
    public final long countTree() {
        long count = 1;

        for (ProofNode n = children; n != null; n = n.sibling) {
            count += n.countTree();
        }
  
        return count;
    }

    public void compress(Board b) {
    
        Piece[]p = b.arrayB;
    
        for (int i = b.B_Counter; --i >= 0;) {
            posB |= 1L << (Board.HEIGHT * p[i].row + p[i].col);
        }
         
        p = b.arrayW;     
             
        for (int i = b.W_Counter; --i >= 0;){
            posR |= 1L << (Board.HEIGHT * p[i].row + p[i].col);
        }  
 
    }
    
    public boolean equals(Object n) {
    	
    	if(n instanceof ProofNode)
    		return equals((ProofNode) n);
    	
    	return false;
    	
    }
    
    public final int hashCode() {
		  return (int) (posB & posR & side) ;
		  }
   
    public final boolean equals(ProofNode n) {
        
        return (posB == n.posB && posR == n.posR && side == n.side);
   
    }
   
}
