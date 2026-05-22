public class WinTable {

    private long[] table;
    private int hashKeySize;

    public WinTable() {}

    public WinTable(int key) {
        hashKeySize = key;
        table = new long[1 << hashKeySize];
    
        for (int i = table.length; --i>=0;) {
            table[i] = -1;
        }
    }

    public void store(long HashValue, int value) {
        if (value >= Board.WIN_SCORE) {
            value = 1;
        } else {
            value = 0;
        }
     
        int key = (int) (HashValue &(table.length-1));
   
        table[key] = ((HashValue >>> 1) << 1) | value;   
    } 
  
    public int retrieve(long HashValue) {
        int key = (int) (HashValue &(table.length-1));
    
        if ((table[key] >>> 1) == (HashValue >>> 1)) {
            if ((table[key] &1) == 1L) {
                return  Board.WIN_SCORE;
            } else {
                return -Board.WIN_SCORE;
            }
        }
             
        return 0;
    }

    public void emptyPosition(long HashValue) {
        int key = (int) (HashValue &(table.length-1));
   
        table[key] = -1;
    }
 
}
