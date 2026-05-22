import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.dataClasses.IndexingInput;
import EndGameDatabase.logicModules.Indexing;
import EndGameDatabase.logicModules.RandomAccessFile.FileReaderWriter;

import java.io.*;
import java.util.*;

/**
 * Step 1 — Collect positions from the EndGame Database.
 *
 * Usage (from project root):
 *   java -cp Testing/collector_bin Collector <p1> <p2> [samples]
 *
 *   p1      — number of Player 1 pieces
 *   p2      — number of Player 2 pieces
 *   samples — how many random positions to collect (default 100)
 *
 * Output: Testing/collected_<p1>v<p2>.csv
 *   columns: index, db_outcome, b0, b1, ..., b35
 *             (b0-b35 = flat board, 0=empty 1=P1 2=P2)
 *
 * Build:  bash Testing/build_collector.sh
 * Run:    java -cp Testing/collector_bin Collector 2 3 100
 */
public class Collector {

    static final int  BOARD_SIZE = 36;
    static final int  SIDE       = 6;
    static final long SEED       = 42L;
    static final String DB_PATH  = "EndGameDatabase/data/";
    static final String OUT_DIR  = "Testing/";

    static final int LOSS      = 0;
    static final int WIN       = 1;
    static final int UNCERTAIN = 2;

    public static void main(String[] args) throws Exception {

        // ── Parse args ────────────────────────────────────────────────
        if (args.length < 2) {
            System.err.println("Usage: Collector <p1> <p2> [samples]");
            System.err.println("  e.g. Collector 2 3 100");
            System.exit(1);
        }
        int p1      = Integer.parseInt(args[0]);
        int p2      = Integer.parseInt(args[1]);
        int samples = args.length >= 3 ? Integer.parseInt(args[2]) : 100;

        String dbFile  = DB_PATH + "moves" + p1 + "v" + p2 + ".txt";
        String outFile = OUT_DIR + "collected_" + p1 + "v" + p2 + ".csv";

        if (!new File(dbFile).exists()) {
            System.err.println("DB file not found: " + dbFile);
            System.exit(1);
        }

        // ── Setup ─────────────────────────────────────────────────────
        BinomialTable binTable = new BinomialTable(BOARD_SIZE);
        long total = Indexing.computeTheNumberOfPossiblePositions(p1, p2, BOARD_SIZE, binTable);
        int n = (int) Math.min(samples, total);

        System.out.printf("Table: %dv%d  |  total positions: %,d  |  sampling: %d%n",
                p1, p2, total, n);

        // ── Pick unique random indexes ─────────────────────────────────
        Random rng = new Random(SEED);
        Set<Long> picked = new LinkedHashSet<>();
        while (picked.size() < n) {
            picked.add((long)(rng.nextDouble() * total));
        }

        // ── Write CSV ─────────────────────────────────────────────────
        new File(OUT_DIR).mkdirs();
        int written = 0;

        try (PrintWriter csv = new PrintWriter(new FileWriter(outFile))) {

            // Header: index, db_outcome, b0..b35
            StringBuilder header = new StringBuilder("index,db_outcome");
            for (int i = 0; i < BOARD_SIZE; i++) header.append(",b").append(i);
            csv.println(header);

            for (long idx : picked) {
                int dbVal = FileReaderWriter.readValueFromFile(dbFile, idx);

                IndexingInput inp = Indexing.filePositionToIndexingInput(
                        idx, BOARD_SIZE, p1, p2, binTable);
                int[] board = inp.getMapFromIndexingInput(BOARD_SIZE);

                StringBuilder row = new StringBuilder();
                row.append(idx).append(",").append(label(dbVal));
                for (int v : board) row.append(",").append(v);
                csv.println(row);

                written++;
                if (written % 10 == 0 || written == n) {
                    System.out.printf("\r  collected %d / %d", written, n);
                    System.out.flush();
                }
            }
        }

        System.out.println();
        System.out.println("Saved to: " + outFile);
    }

    static String label(int v) {
        switch (v) {
            case LOSS:      return "LOSS";
            case WIN:       return "WIN";
            case UNCERTAIN: return "UNCERTAIN";
            default:        return "UNKNOWN";
        }
    }
}
