

import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.dataClasses.IndexingInput;
import EndGameDatabase.logicModules.Indexing;
import EndGameDatabase.logicModules.RandomAccessFile.FileReaderWriter;

import java.io.*;
import java.util.*;

/**
 * Collects random positions from the EndGame Database and validates
 * them against the AI — all in one run.
 *
 * Run from project root:
 *   java -cp Testing/bin:AIAgent/data Tester
 *
 * The program will prompt for p1, p2, and number of samples.
 *
 * Output:
 *   - Live progress printed to console
 *   - Summary stats at the end
 *   - Detailed results saved to Testing/results_<p1>v<p2>.csv
 */
public class Tester {

    static final int    BOARD_SIZE   = 36;
    static final long   SEED         = 42L;
    static final int    AI_THINK_MS  = 5000;
    static final String DB_PATH      = "EndGameDatabase/data/";
    static final String OUT_DIR      = "Testing/";

    static final int DB_LOSS      = 0;
    static final int DB_WIN       = 1;
    static final int DB_UNCERTAIN = 2;

    static final PrintStream REAL_OUT = System.out;
    static final PrintStream NULL_OUT = new PrintStream(new OutputStream() {
        public void write(int b) {}
    });

    public static void main(String[] args) throws Exception {

        // ── Interactive prompt ────────────────────────────────────────
        Scanner input = new Scanner(System.in);

        System.out.print("Player 1 pieces (p1): ");
        int p1 = Integer.parseInt(input.nextLine().trim());

        System.out.print("Player 2 pieces (p2): ");
        int p2 = Integer.parseInt(input.nextLine().trim());

        System.out.print("Number of samples [100]: ");
        String samplesStr = input.nextLine().trim();
        int samples = samplesStr.isEmpty() ? 100 : Integer.parseInt(samplesStr);

        System.out.print("Use tablebase oracle up to how many total pieces? [0 = AI only]: ");
        String tbStr = input.nextLine().trim();
        int maxTbPieces = tbStr.isEmpty() ? 0 : Integer.parseInt(tbStr);

        String dbFile  = DB_PATH + "moves" + p1 + "v" + p2 + ".txt";
        String outFile = OUT_DIR + "results_" + p1 + "v" + p2 + ".csv";

        if (!new File(dbFile).exists()) {
            System.err.println("DB file not found: " + dbFile);
            System.exit(1);
        }

        // ── Init AI silently ──────────────────────────────────────────
        System.setOut(NULL_OUT);
        Board.setSize(6, 6);
        SurakartaGame game = new SurakartaGame(new SurakartaKanazawaEvaluator());
        System.setOut(REAL_OUT);

        // ── Setup DB ──────────────────────────────────────────────────
        BinomialTable binTable = new BinomialTable(BOARD_SIZE);
        long total = Indexing.computeTheNumberOfPossiblePositions(p1, p2, BOARD_SIZE, binTable);
        int n = (int) Math.min(samples, total);

        // ── Register tablebase oracle (if requested) ──────────────────
        EndGameDbOracle oracle = null;
        if (maxTbPieces > 0) {
            oracle = new EndGameDbOracle(binTable, maxTbPieces, DB_PATH);
            AbstractPlayer.customTablebase        = oracle;
            AbstractPlayer.customTablebaseMaxPieces = maxTbPieces;
            REAL_OUT.printf("  → Tablebase oracle active for ≤ %d total pieces%n", maxTbPieces);
        } else {
            AbstractPlayer.customTablebase        = null;
            AbstractPlayer.customTablebaseMaxPieces = 0;
            REAL_OUT.println("  → No tablebase oracle — AI only");
        }

        REAL_OUT.printf("Table: %dv%d  |  total positions: %,d  |  testing: %d%n%n",
                p1, p2, total, n);

        // Pick unique random indexes
        Random rng = new Random(SEED);
        Set<Long> picked = new LinkedHashSet<>();
        while (picked.size() < n) {
            picked.add((long)(rng.nextDouble() * total));
        }

        // ── Run ───────────────────────────────────────────────────────
        // Counters per DB outcome
        int[] dbTotal = new int[3], dbMatch = new int[3];
        int grandTotal = 0, grandMatch = 0;

        // Collect failures for end-of-run display
        List<Failure> failures = new ArrayList<>();

        new File(OUT_DIR).mkdirs();

        try (PrintWriter csv = new PrintWriter(new FileWriter(outFile))) {
            csv.println("index,db_outcome,ai_outcome,match");

            int done = 0;
            for (long idx : picked) {

                // DB
                int dbVal = FileReaderWriter.readValueFromFile(dbFile, idx);
                String dbLabel = label(dbVal);

                // Board
                IndexingInput inp = Indexing.filePositionToIndexingInput(
                        idx, BOARD_SIZE, p1, p2, binTable);
                int[] boardMap = inp.getMapFromIndexingInput(BOARD_SIZE);

                // AI
                String aiLabel = evaluateWithAI(game, boardMap);
                boolean match  = dbLabel.equals(aiLabel);

                // Record
                csv.printf("%d,%s,%s,%b%n", idx, dbLabel, aiLabel, match);

                if (dbVal >= 0 && dbVal <= 2) {
                    dbTotal[dbVal]++;
                    if (match) dbMatch[dbVal]++;
                }
                grandTotal++;
                if (match) grandMatch++;

                if (!match) failures.add(new Failure(idx, dbLabel, aiLabel, boardMap.clone()));

                done++;
                REAL_OUT.printf("\r  [%d/%d]  idx=%-12d  DB=%-10s  AI=%-10s  %s",
                        done, n, idx, dbLabel, aiLabel, match ? "✓" : "✗");
                REAL_OUT.flush();
            }
        }

        // ── Stats ─────────────────────────────────────────────────────
        REAL_OUT.println("\n");
        REAL_OUT.println("══════════════════════════════════════");
        REAL_OUT.printf ("  RESULTS  —  %dv%d  (%d positions)%n", p1, p2, n);
        REAL_OUT.println("══════════════════════════════════════");
        REAL_OUT.printf ("%-12s %6s %6s %8s%n", "DB Outcome", "Total", "Match", "Acc.");
        REAL_OUT.println("──────────────────────────────────────");
        String[] names = {"LOSS", "WIN", "UNCERTAIN"};
        for (int i = 0; i < 3; i++) {
            if (dbTotal[i] == 0) continue;
            REAL_OUT.printf("%-12s %6d %6d %7.1f%%%n",
                    names[i], dbTotal[i], dbMatch[i], 100.0 * dbMatch[i] / dbTotal[i]);
        }
        REAL_OUT.println("──────────────────────────────────────");
        REAL_OUT.printf ("%-12s %6d %6d %7.1f%%%n",
                "OVERALL", grandTotal, grandMatch, 100.0 * grandMatch / grandTotal);
        REAL_OUT.println("══════════════════════════════════════");
        REAL_OUT.println("Saved to: " + outFile);

        // Close any open tablebase file channels
        if (oracle != null) oracle.closeAll();

        // ── Failures ──────────────────────────────────────────────────
        if (!failures.isEmpty()) {
            REAL_OUT.println();
            REAL_OUT.println("══════════════════════════════════════");
            REAL_OUT.printf ("  MISMATCHES  (%d)%n", failures.size());
            REAL_OUT.println("══════════════════════════════════════");
            for (var f : failures) {
                REAL_OUT.printf("  idx=%-12d  DB=%-10s  AI=%-10s%n",
                        f.idx(), f.dbLabel(), f.aiLabel());
                printBoard(f.board());
                REAL_OUT.println();
            }
        }
    }

    // ── Board display ─────────────────────────────────────────────────────────

    /** Prints the 6×6 board. 1=Player1 (●), 2=Player2 (○), 0=empty (·) */
    static void printBoard(int[] board) {
        int cols = 6;
        REAL_OUT.println("    a b c d e f");
        REAL_OUT.println("  ┌─────────────┐");
        for (int row = 0; row < 6; row++) {
            REAL_OUT.printf("%d │", row + 1);
            for (int col = 0; col < cols; col++) {
                int v = board[row * cols + col];
                char ch = v == 1 ? '●' : v == 2 ? '○' : '·';
                REAL_OUT.print(" " + ch);
            }
            REAL_OUT.println(" │");
        }
        REAL_OUT.println("  └─────────────┘");
        REAL_OUT.println("    ● = P1   ○ = P2");
    }

    // ── AI ────────────────────────────────────────────────────────────────────

    static String evaluateWithAI(SurakartaGame game, int[] boardMap) {
        System.setOut(NULL_OUT);
        try {
            Board board = new Board(game.getGenerator(), game);
            for (int row = 0; row < Board.HEIGHT; row++) {
                for (int col = 0; col < Board.WIDTH; col++) {
                    int v = boardMap[row * Board.WIDTH + col];
                    board.space[row][col] = (v == 2) ? Board.W_PIECE : (byte) v;
                }
            }
            board.sideToMove = Board.B_SIDE;
            board.initialise(Board.B_SIDE);

            int winSide = board.checkForWin();
            if (winSide == Board.B_SIDE) return "WIN";
            if (winSide == Board.W_SIDE) return "LOSS";

            NegaMaxPlayer agent = new NegaMaxPlayer(game);
            agent.setFixedTime(AI_THINK_MS);
            Move bestMove = agent.search(board);

            if (bestMove == null) return "LOSS";
            int score = bestMove.score;
            if (score >= Board.WIN_SCORE)  return "WIN";
            if (score <= -Board.WIN_SCORE) return "LOSS";
            return "UNCERTAIN";

        } catch (Exception e) {
            return "ERROR";
        } finally {
            System.setOut(REAL_OUT);
        }
    }

    static class Failure {
        final long idx; final String dbLabel; final String aiLabel; final int[] board;
        Failure(long idx, String dbLabel, String aiLabel, int[] board) {
            this.idx = idx; this.dbLabel = dbLabel; this.aiLabel = aiLabel; this.board = board;
        }
        long idx()      { return idx; }
        String dbLabel(){ return dbLabel; }
        String aiLabel(){ return aiLabel; }
        int[] board()   { return board; }
    }

    static String label(int v) {
        switch (v) {
            case DB_LOSS:      return "LOSS";
            case DB_WIN:       return "WIN";
            case DB_UNCERTAIN: return "UNCERTAIN";
            default:           return "UNKNOWN";
        }
    }

    // ── Tablebase oracle ──────────────────────────────────────────────────────
    /**
     * Oracle implementation that bridges the AIAgent's Board representation
     * with our custom EndGameDatabase files.
     *
     * <p>Convention: the database always stores outcomes from the perspective
     * of "Player 1" (the active player).  When Black (B_SIDE) is to move,
     * Black is treated as P1; when White (W_SIDE) is to move, pieces are
     * flipped so White is treated as P1.</p>
     *
     * <p>Open file channels are cached so each database file is opened only
     * once, avoiding per-node file-open overhead during the search.</p>
     */
    static class EndGameDbOracle implements TablebaseOracle {

        private final BinomialTable binTable;
        private final int           maxPieces;
        private final String        dbPath;

        /** Cached open readers; null entry = file does not exist. */
        private final java.util.HashMap<String, FileReaderWriter.OpenReader> readers
                = new java.util.HashMap<>();
        private final java.util.HashSet<String> missingFiles
                = new java.util.HashSet<>();

        EndGameDbOracle(BinomialTable binTable, int maxPieces, String dbPath) {
            this.binTable  = binTable;
            this.maxPieces = maxPieces;
            this.dbPath    = dbPath;
        }

        @Override
        public int lookup(Board b) {
            int bCount = b.B_Counter;
            int wCount = b.W_Counter;
            int total  = bCount + wCount;
            if (total == 0 || total > maxPieces) return -1;

            // Treat the active player as "P1" in the database convention
            boolean flip    = (b.sideToMove == Board.W_SIDE);
            int     p1Count = flip ? wCount : bCount;
            int     p2Count = flip ? bCount : wCount;

            String key      = p1Count + "v" + p2Count;
            String filePath = dbPath + "moves" + key + ".txt";

            if (missingFiles.contains(key)) return -1;

            FileReaderWriter.OpenReader reader = readers.get(key);
            if (reader == null && !readers.containsKey(key)) {
                // First time we see this key — try to open the file
                if (!new File(filePath).exists()) {
                    missingFiles.add(key);
                    return -1;
                }
                try {
                    long totalPositions = Indexing.computeTheNumberOfPossiblePositions(
                            p1Count, p2Count, BOARD_SIZE, binTable);
                    reader = FileReaderWriter.openReader(filePath, totalPositions);
                    readers.put(key, reader);
                } catch (IOException e) {
                    missingFiles.add(key);
                    return -1;
                }
            }
            if (reader == null) return -1;

            try {
                // Convert AIAgent Board → int[36] (0=empty, 1=P1, 2=P2)
                int[] boardMap = new int[BOARD_SIZE];
                for (int r = 0; r < Board.HEIGHT; r++) {
                    for (int c = 0; c < Board.WIDTH; c++) {
                        byte v = b.space[r][c];
                        if (!flip) {
                            boardMap[r * Board.WIDTH + c] =
                                    (v == Board.B_PIECE) ? 1
                                  : (v == Board.W_PIECE) ? 2 : 0;
                        } else {
                            // White is active player → treat W_PIECE as 1, B_PIECE as 2
                            boardMap[r * Board.WIDTH + c] =
                                    (v == Board.W_PIECE) ? 1
                                  : (v == Board.B_PIECE) ? 2 : 0;
                        }
                    }
                }

                IndexingInput inp = Indexing.createIndexingInput(boardMap, false);
                long          idx = Indexing.indexInputToFilePosition(inp, binTable);
                int raw = reader.get(idx); // 0=LOSS, 1=WIN, 2=UNCERTAIN — from FILE's P1 perspective

                // Convert to GLOBAL Black perspective (required by TablebaseOracle contract).
                // Without flip: P1 = Black → raw value is already from Black's perspective.
                // With    flip: P1 = White → invert WIN/LOSS so 1 always means Black wins.
                if (flip && raw <= 1) return 1 - raw;   // 0↔1 swap; 2 (UNCERTAIN) unchanged
                return raw;

            } catch (Exception e) {
                return -1;
            }
        }

        /** Release all open file channels. Call once after the test run. */
        void closeAll() {
            for (FileReaderWriter.OpenReader r : readers.values()) {
                if (r != null) try { r.close(); } catch (IOException ignored) {}
            }
            readers.clear();
            missingFiles.clear();
        }
    }
}
