
// Entry in the table
public final class DFPnBigEntry {

    public static final int LOCK_SHIFT = ProofNode.BIT_RANGE;
  
    public static final int SIZE_SHIFT = LOCK_SHIFT;

    private final static int PROOF_RANGE = (1 << SIZE_SHIFT) - 1;

    private long moveHashLock; 
  
    private long sizeScore;
  
    public DFPnBigEntry() {}
  
    public DFPnBigEntry(long HashValue, long s, ProofNode n) {
        setElement(HashValue, s, n);
    }
     
    public void setElement(long HashValue, long s, ProofNode n) {
        moveHashLock = getMoveHashLock(HashValue, n.proof);
      
        sizeScore = getSizeScore(n.disproof, s);
    }
     
    public static long getMoveHashLock(long HashValue, int proofnumber) {
        return (((HashValue >>> LOCK_SHIFT) << LOCK_SHIFT) | proofnumber);
    }
     
    public static long getSizeScore(int disproofnumber, long size) {
        return  ((size << SIZE_SHIFT) | disproofnumber);
    }
    
    public int getDisProofNumber() {
        return (int) (sizeScore & PROOF_RANGE);
    }

    public int getProofNumber() {
        return (int) (moveHashLock & PROOF_RANGE);
    }
   
    public long getHashLock() {  
        return (moveHashLock >>> LOCK_SHIFT);
    }
      
    public long getSize() {
        return (sizeScore >>> SIZE_SHIFT);
   
    }    
  
    public void copy(DFPnBigEntry entry) {
        this.moveHashLock = entry.moveHashLock;
        this.sizeScore = entry.sizeScore;
    }
     
    public void setValue(long MFH, long H_S) {
        moveHashLock = MFH;
        sizeScore = H_S;
    }    
     
}
