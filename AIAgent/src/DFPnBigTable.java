// transpositiontable
public final class DFPnBigTable {
    private int countStoredEntries = 0;
  
    private  final DFPnBigEntry entry = new DFPnBigEntry(0, -1, new ProofNode());
 
    private long[]sizeScore;
  
    private long[]moveHashLock;
    
    private int hashKeySize;
  
    private static final int SHIFT_INDEX = DFPnBigEntry.LOCK_SHIFT;
  
    public DFPnBigTable() {}
 
    // creates a transpositiontable
    public DFPnBigTable(int key) {     
        hashKeySize = 1 << key;
    
        moveHashLock = new long[2 * hashKeySize];
    
        sizeScore = new long[2 * hashKeySize];
   
        clear();
    }
   
    // Clear the table, by making a new array
    public void clear() { 
        for (int i = moveHashLock.length; --i >= 0;) {
            moveHashLock[i] = -1;
            sizeScore[i] = 0;
        }
   
        countStoredEntries = 0;
    }
    
    // Gets an element form the transpositiontable
    public DFPnBigEntry retrieve(long HashValue) {
        int key = (int) (HashValue & (hashKeySize-1));
    
        final long lock = HashValue >>> SHIFT_INDEX;
     
        if (moveHashLock[key ] >>> SHIFT_INDEX == lock) {
            entry.setValue(moveHashLock[key], sizeScore[key]);
       
            return entry;      
        }
       
        key += hashKeySize;
        
        if (moveHashLock[key ] >>> SHIFT_INDEX == lock) {
            entry.setValue(moveHashLock[key], sizeScore[key]);
         
            return entry;      
        }
      
        return null;
    }
   
    // Stores  an element
    public void storeTwoBig(long HashValue, long size, ProofNode node) {
        final int first = (int) (HashValue & (hashKeySize-1));
      
        final long lock = HashValue >>> SHIFT_INDEX;
    
        if (moveHashLock[first] >>> SHIFT_INDEX == lock) {   
            moveHashLock[first] = DFPnBigEntry.getMoveHashLock(HashValue,
                    node.proof); 
            sizeScore[first] = DFPnBigEntry.getSizeScore(node.disproof, size);
                 
            return;
        }
 
        final int second = first + hashKeySize;
       
        if (moveHashLock[second ] >>> SHIFT_INDEX == lock) {  
            moveHashLock[second ] = DFPnBigEntry.getMoveHashLock(HashValue,
                    node.proof);
            sizeScore[second ] = DFPnBigEntry.getSizeScore(node.disproof, size);
           
            return;
        }
   
        if ((sizeScore[first] >> DFPnBigEntry.SIZE_SHIFT)
                >= (sizeScore[second] >> DFPnBigEntry.SIZE_SHIFT)) {
            if (moveHashLock[second] == -1) {
            	countStoredEntries++;
            }
       
            sizeScore[second] = sizeScore[first];
        
            moveHashLock[second] = moveHashLock[first];
        }
       
        /* if( Move_HashLock[second] ==-1)
         Counter++;  */
       
     
        moveHashLock[first] = DFPnBigEntry.getMoveHashLock(HashValue,
                node.proof);
        sizeScore[first] = DFPnBigEntry.getSizeScore(node.disproof, size);
      
    }



	public final int getCountStoredEntries() {
		return countStoredEntries;
	}  
     
} 


