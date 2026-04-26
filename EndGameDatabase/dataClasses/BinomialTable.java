package EndGameDatabase.dataClasses;

public class BinomialTable {
    public long[][] binomialTable;

    public BinomialTable(int size) {
        binomialTable = new long[size+1][size+1];
        precompute(size);
    }

    private void precompute(int size) {
        // Base case: the top of the triangle
        binomialTable[0][0] = 1;

        for (int n = 1; n <= size; n++) {
            // The first element of every row is always 1 (nC0)
            binomialTable[n][0] = 1;
            
            for (int k = 1; k <= n; k++) {
                // Now n-1 and k-1 are always at least 0
                binomialTable[n][k] = binomialTable[n - 1][k - 1] + binomialTable[n - 1][k];
            }
        }
    }
}
