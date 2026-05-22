
// Entry in the table
public final class TableEntry {

    private static final int STAMP_SHIFT = Move.BIT_RANGE+2;
    private static final int STAMP_MARK = 1 << STAMP_SHIFT;
  
    public final static int SHIFT_LOCK = STAMP_SHIFT+1;
    private final static long MASK_LOCK = ((0xFFFFFFFFFFFFFFFFL >>> SHIFT_LOCK)
            << SHIFT_LOCK);
                        
    private final static int MAX_SCORE = 1 << 14;
  
    private final static int SCORE_RANGE = (1 << 15) - 1;
  
    public final static int SHIFT_DEPTH = 13;
  
    private final static int SECOND_MOVE_SHIFT = SHIFT_DEPTH + 15;        
  
    public static final int DEPTH_RANGE = (1 << SHIFT_DEPTH) - 1;
  
    public static final int MOVE_RANGE = (1 << Move.BIT_RANGE) - 1;

    private long moveFlagHashLock; 
  
    private long heightScore;
  
    private Move dummyMove, dummy2Move;
  
    TableEntry() {
        dummyMove = new Move();
        dummy2Move = new Move();
    }
  
    TableEntry(long HashValue, double h, int f, Move m) { 
        this();
        setElement(HashValue, h, f, m);
    }
     
    public void setElement(long HashValue, double h, int f, Move m) { 
        moveFlagHashLock = getMoveFlagHashLock(HashValue, f, m);
        heightScore = getHeightScore(m.next, m.score, h);
    }
    
    public static long getMoveFlagHashLock(long HashValue, int f, Move m) {
        // return ((((HashValue >> SHIFT_LOCK) << SHIFT_LOCK) |    (m.index << 2)  | f ));
        return ((HashValue & MASK_LOCK)
                | (m.index
                        << 2) | f);
    }
     
    public static long getHeightScore(Move m, int score, double h) {
       final long index = (m == null) ? 0 : m.index;
    
        return  (index << SECOND_MOVE_SHIFT) | 
                 ((score + MAX_SCORE) << SHIFT_DEPTH) | ((int) (100 * (h + 1)));
    }
    
    public boolean isMarked() {
        return isMarked(moveFlagHashLock);
    }  
    
    public static boolean isMarked(long mfh) {
        return (mfh & STAMP_MARK) == STAMP_MARK;
    }     
     
    public static long mark(long moveFlagHashLock) {
        return (moveFlagHashLock | STAMP_MARK);
    }
      
    public Move getMove() {
   
      
        dummyMove.setMove((int) ((moveFlagHashLock >> 2) & MOVE_RANGE));
        // /dummyMove.setMove( (int)((Move_Flag_HashLock >>2)&Move_Range));
       final int index = (int) ((heightScore >>> SECOND_MOVE_SHIFT));
 
        if (index > 0) {
            
            dummy2Move.setMove(index);
            dummyMove.next = dummy2Move;
        } else {
            dummyMove.next = null;
        }
        return dummyMove;
    }
   
    public int getScore() {
        return (int) (((heightScore >> SHIFT_DEPTH) & SCORE_RANGE) - MAX_SCORE);
    }

    public int getFlag() {
        return (int) (moveFlagHashLock & 3);
    }
      
    public long getHashLock() {  
        return (moveFlagHashLock >>> SHIFT_LOCK);
    }
            
    public double getHeight() {
        return ((heightScore & DEPTH_RANGE) / 100d - 1);
    }    
        
    public void copy(TableEntry entry) {
        this.moveFlagHashLock = entry.moveFlagHashLock;
        this.heightScore = entry.heightScore;
    }
     
    public void setValue(long MFH, long H_S) {
        moveFlagHashLock = MFH;
        heightScore = H_S; 
    }    
     
}
