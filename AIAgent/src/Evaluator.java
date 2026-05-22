public abstract class Evaluator {
  
    /**
	Use the constructor as a chance to initialize constants or
	arrays used in the evaluator.
	*/
	protected static final int SHIFT = 16;
	protected static final long VALUE_MASK = (1<< SHIFT )-1;
	protected static final long MASK = ((0xFFFFFFFFFFFFFFFFL >>> SHIFT) << SHIFT);
	protected static final int TRANSPOSE_VALUE = Board.WIN_SCORE;
	protected static final long HASH_KEY_MASK = (1<< SHIFT)-1;
	protected static final int NO_ELEMENT = 11111111;
	protected double[] weight = new double[1];
    protected double [] feature = new double[1];
 
    protected static boolean noHash = true;

    public static void parametrise(boolean nHash) {
        noHash = nHash;
    }
    
    public abstract int evaluate(Board b, int lastturn);

    public int evaluate(Board b, int pass_thresh, int prune_thresh, int lastturn) {
        return evaluate( b, lastturn);

    }

    public final double[] getWeights(){  	
    	return weight.clone();
      }
      
     public final void setWeights(double[] w){
    	 System.arraycopy(w, 0, weight, 0, weight.length);
      }
      
      public final double[] getFeatures(){
    	  return feature.clone();
      }
    
    public int getNumberOfMoves(Board b, int side){
    	
    	return 1;
    }
    
    public static int  pow(int a, int n) {
        int r = 1;
        int b = a;
        int i = n;

        while (i > 0) {
            if ((i & 1) == 0) {
                b *= b;
                i >>= 1;
            } else {
                r *= b;
                i--; 
            }
        }
        return r;  
    }
    
    private long seed = System.currentTimeMillis();
    
    private static final int RANGE = 7;
    private final static  long MULTIPLIER = 0x5DEECE66DL;
    private final static long APPEND = 0xBL;
    private final static long RMASK = (1L << 48) - 1;
    
    protected final  int randomIntValue() {   
        if (Game.isRandom) {
            final long nextseed = (seed * MULTIPLIER + APPEND) & RMASK;
            seed = nextseed;
            return (int) (nextseed >>> (48 - RANGE)) - (1 << (RANGE - 1));
           
        }
        
        return 0;     
    }  
    
    protected final int trim(int eval){
    	
    	if(eval>9000)
			 return 9000 + (eval / 100);
		 else if(eval<-9000)
			return -9000 + (eval / 100);
    	
    	return eval;
    }
}
