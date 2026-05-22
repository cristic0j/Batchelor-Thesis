public class Symmetry {

    private Symmetry() {}

    public static long mirrorX(long pos) {
   
        long mirror = 0;
   
        for (int row = 0; row < Board.HEIGHT; row++) {
            for (int col = 0; col < Board.WIDTH; col++) {    
                if (((pos >>> (row * Board.HEIGHT + col)) & 1L) == 1L) {
                    mirror |= 1L
                            << (row * Board.HEIGHT + ((Board.WIDTH - 1) - col));
                }
            }
        }
   
        return mirror; 
    }
    
    public static void clear(byte [][] space){
    	
  	  for(int j = Board.HEIGHT; --j>=0;){
		  byte[] hulp = space[j];
		  for(int k = Board.REAL_BORDER; k>=0; k--)
		         hulp[k]=0;
	  }
    	
    	
    }

    public static long mirrorY(long pos) {
   
        long mirror = 0;
      
        for (int row = 0; row < Board.HEIGHT; row++) {
            for (int col = 0; col < Board.WIDTH; col++) {    
                if (((pos >>> (row * Board.HEIGHT + col)) & 1L) == 1L) {
                    mirror |= 1L
                            << (((Board.HEIGHT - 1) - row) * Board.HEIGHT + col);
                }
            }
        }
   
        return mirror; 
    }
    
    public static boolean isMirrorX(byte pos1[][], byte pos2[][]){
    	
       	
   	 for (int row = 0; row < Board.HEIGHT; row++) 
         for (int col = 0; col < Board.WIDTH; col++) {           	 
            	 if(pos1[row][col] != pos2[row][Board.WIDTH-1-col])
            	    return false; 
             }
    	 
    	 return true;
    }
    
   public static boolean isMirrorXY(byte pos1[][], byte pos2[][]){
    	
		 for (int row = 0; row < Board.HEIGHT; row++) 
	            for (int col = 0; col < Board.WIDTH; col++) {           	 
            	 if(pos1[row][col] != pos2[Board.HEIGHT-1-row][Board.WIDTH-1-col])
            	    return false; 
             }
    	 
    	 return true;
    }
    

   public static boolean isMirrorY(byte pos1[][], byte pos2[][]){
   	  	
   	 for (int row = 0; row < Board.HEIGHT; row++) 
            for (int col = 0; col < Board.WIDTH; col++) {           	 
           	 if(pos1[row][col] != pos2[Board.HEIGHT-1-row][col])
           	    return false; 
            }
   	 
   	 return true;
   }
   
   public static boolean isRotate1(byte pos1[][], byte pos2[][]){
  	  	
	   	 for (int row = 0; row < Board.HEIGHT; row++) 
	            for (int col = 0; col < Board.WIDTH; col++) {           	 
	           	 if(pos1[row][col] != pos2[col][row])
	           	    return false; 
	            }
	   	 
	   	 return true;
	   }
   
   public static boolean isRotate2(byte pos1[][], byte pos2[][]){
 	  	
	   	 for (int row = 0; row < Board.HEIGHT; row++) 
	            for (int col = 0; col < Board.WIDTH; col++) {           	 
	           	 if(pos1[row][col] != pos2[Board.WIDTH-1-col][row])
	           	    return false; 
	            }
	   	 
	   	 return true;
	   }
   
   public static boolean isRotate3(byte pos1[][], byte pos2[][]){
 	  	
	   	 for (int row = 0; row < Board.HEIGHT; row++) 
	            for (int col = 0; col < Board.WIDTH; col++) {           	 
	           	 if(pos1[row][col] != pos2[Board.WIDTH-1-col][Board.WIDTH-1-row])
	           	    return false; 
	            }
	   	 
	   	 return true;
	   }
   
   public static boolean isRotate4(byte pos1[][], byte pos2[][]){
	  	
	   	 for (int row = 0; row < Board.HEIGHT; row++) 
	            for (int col = 0; col < Board.WIDTH; col++) {           	 
	           	 if(pos1[row][col] != pos2[col][Board.WIDTH-1-row])
	           	    return false; 
	            }
	   	 
	   	 return true;
	   }
    
    public static void mirrorX(byte pos[][]){
    	
    	final int height = Board.HEIGHT;
    	final int width = Board.WIDTH/2;
    	
    	 for (int row = 0; row < height; row++) 
             for (int col = 0; col < width; col++) {
            	 
            	 final byte old = pos[row][col];
            	 
            	 pos[row][col] = pos[row][Board.WIDTH-1-col];
            	 
            	 pos[row][Board.WIDTH-1-col] =old; 
             }
    }
    

    public static void mirrorY(byte pos[][]){
    	
    	final int height = Board.HEIGHT/2;
    	final int width = Board.WIDTH;
    	
    	 
        for (int col = 0; col < width; col++)
           	 for (int row = 0; row < height; row++){
            	 
            	 byte old = pos[row][col];
            	 
            	 pos[row][col] = pos[Board.HEIGHT-1-row][col];
            	 
            	 pos[Board.HEIGHT-1-row][col] =old;
            	 
             }
    	
    }
    
    
    public static byte[][] getMirrorX(byte[][] space)
	{
		byte[][] mirror = new byte[Board.HEIGHT][Board.WIDTH];
	
		setMirrorX(space, mirror);
		
		return mirror;
	}
    
    public static void setMirrorX(byte[][] space, byte[][] mirror)
	{
    	final int BORDERWIDTH= Board.WIDTH-1;
		
		   for (int row = Board.HEIGHT; --row>=0;){
			   byte[] hulp = space[row];
			   byte[] mirhulp = mirror[row];
			   for (int col = Board.WIDTH; --col>=0;) 
				     mirhulp[BORDERWIDTH-col] = hulp[col];
		   }
	
	}
    
    public static byte[][] getMirrorY(byte[][] space)
	{
		byte[][] mirror = new byte[Board.HEIGHT][Board.WIDTH];
	
		setMirrorY(space, mirror);
		
		return mirror;
	}

    public static void setMirrorY(byte[][] space, byte[][] mirror)
	{
		   
			   for (int row = Board.HEIGHT; --row>=0;){  
				   byte[] hulp = space[row];
			       byte[] mirhulp = mirror[Board.HEIGHT-1-row];
				   for (int col = Board.WIDTH; --col>=0;)
				     mirhulp[col] = hulp[col];
			   }
	
	}
    
    public static byte[][] getMirrorXY(byte[][] space)
	{
		byte[][] mirror = new byte[Board.HEIGHT][Board.WIDTH];
	
		setMirrorXY(space, mirror);
		
		return mirror;
	}

    public static void setMirrorXY(byte[][] space, byte[][] mirror)
	{
    	final int BORDERWIDTH= Board.WIDTH-1;
    	   for (int row = Board.HEIGHT; --row>=0;){
			   byte[] hulp = space[row];
			   byte[] mirhulp = mirror[Board.HEIGHT-1-row];
			   for (int col = Board.WIDTH; --col>=0;) 
				     mirhulp[BORDERWIDTH-col] = hulp[col];
		   }
		
		
	}
    

    public static byte[][] reverse(byte[][] space)
	{
    	byte[][] mirror = new byte[Board.HEIGHT][Board.WIDTH];
    	
		setReverse(space, mirror);
		
		return mirror;
	}
    
    public static void setReverse(byte[][] space, byte mirror[][])
	{
    	  for (int row = Board.HEIGHT; --row>=0;){
			   byte[] hulp = space[row];
			   byte[] mirhulp = mirror[row];
			   for (int col = Board.WIDTH; --col>=0;) 
				     mirhulp[col] = (byte)( -hulp[col]);
    	  }

	}

    public static long mirrorNWSE(long pos) {
   
        long mirror = 0;
   
        for (int row = 0; row < Board.HEIGHT; row++) {
            for (int col = 0; col < Board.WIDTH; col++) {    
                if (((pos >>> (row * Board.HEIGHT + col)) & 1L) == 1L) {
                    mirror |= 1L << (col * Board.HEIGHT + row);
                }
            }
        }
   
        return mirror; 
    }
   
    public static long mirrorNESW(long pos) {
   
        long mirror = 0;
   
        for (int row = 0; row < Board.HEIGHT; row++) {
            for (int col = 0; col < Board.WIDTH; col++) {    
                if (((pos >>> (row * Board.HEIGHT + col)) & 1L) == 1L) {
                    mirror |= 1L
                            << (((Board.HEIGHT - 1) - col) * Board.HEIGHT
                                    + ((Board.WIDTH - 1) - row));
                }
            }
        }
   
        return mirror; 
    } 

}
