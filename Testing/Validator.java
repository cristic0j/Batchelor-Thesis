import java.io.*;
import java.util.*;

/**
 * Step 2 — Validate collected positions using the AI agent.
 *
 * Reads a CSV produced by Collector, runs the AI on each board,
 * compares with the DB outcome, and prints summary statistics.
 *
 * Usage (from project root):
 *   java -cp Testing/validator_bin:AIAgent/data Validator <collected_file>
 *
 *   e.g.  java -cp Testing/validator_bin:AIAgent/data Validator Testing/collected_2v3.csv
 *
 * Output:
 *   - Prints stats to stdout
 *   - Saves detailed results to Testing/validated_<p1>v<p2>.csv
 *     columns: index, db_outcome, ai_outcome, match
 *
 * Build:  bash Testing/build_validator.sh
 */
public class Validator {

    static final int AI_THINK_MS = 1000;

    static final PrintStream REAL_OUT = System.out;
    static final PrintStream NULL_OUT = new PrintStream(new OutputStream() {
        public void write(int b) {}
    });

    public static void main(String[] args) throws Exception {

        // ── Parse args ────────────────────────────────────────────────
        if (args.length < 1) {
            System.err.println("Usage: Validator <collected_file.csv>");
            System.err.println("  e.g. Validator Testing/collected_2v3.csv");
            System.exit(1);
        }
        String inFile = args[0];
        if (!new File(inFile).exists()) {
            System.err.println("File not found: " + inFile);
            System.exit(1);
        }

        // Derive output file name: collected_2v3.csv → validated_2v3.csv
        String outFile = inFile
                .replace("collected_", "validated_")
                .replace("Testing/", "Testing/");

        // ── Init AI (silently) ────────────────────────────────────────
        System.setOut(NULL_OUT);
        Board.setSize(6, 6);
        SurakartaGame game = new SurakartaGame(new SurakartaKanazawaEvaluator());
        System.setOut(REAL_OUT);

        // ── Read input CSV ────────────────────────────────────────────
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(inFile))) {
            String header = br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) rows.add(line.split(","));
            }
        }

        int total = rows.size();
        REAL_OUT.printf("Validating %d positions from %s ...%n%n", total, inFile);

        // Counters: per db_outcome category
        Map<String, int[]> stats = new LinkedHashMap<>();
        for (String k : new String[]{"WIN","LOSS","UNCERTAIN"}) {
            stats.put(k, new int[]{0, 0}); // [total, matched]
        }
        int grandTotal = 0, grandMatch = 0;

        // ── Write output CSV ──────────────────────────────────────────
        try (PrintWriter csv = new PrintWriter(new FileWriter(outFile))) {
            csv.println("index,db_outcome,ai_outcome,match");

            for (int i = 0; i < rows.size(); i++) {
                String[] row = rows.get(i);
                long   idx       = Long.parseLong(row[0].trim());
                String dbOutcome = row[1].trim();

                // Read board (columns 2..37)
                int[] board = new int[36];
                for (int c = 0; c < 36; c++) {
                    board[c] = Integer.parseInt(row[2 + c].trim());
                }

                String aiOutcome = evaluateWithAI(game, board);
                boolean match    = dbOutcome.equals(aiOutcome);

                csv.printf("%d,%s,%s,%b%n", idx, dbOutcome, aiOutcome, match);

                // Accumulate stats
                int[] s = stats.get(dbOutcome);
                if (s != null) {
                    s[0]++;
                    if (match) s[1]++;
                }
                grandTotal++;
                if (match) grandMatch++;

                // Progress
                if ((i + 1) % 10 == 0 || (i + 1) == total) {
                    REAL_OUT.printf("\r  %d / %d", i + 1, total);
                    REAL_OUT.flush();
                }
            }
        }

        // ── Print stats ───────────────────────────────────────────────
        REAL_OUT.println("\n");
        REAL_OUT.println("══════════════════════════════════════");
        REAL_OUT.println("  RESULTS");
        REAL_OUT.println("══════════════════════════════════════");
        REAL_OUT.printf("%-12s %6s %6s %7s%n", "DB Outcome", "Total", "Match", "Acc.");
        REAL_OUT.println("──────────────────────────────────────");
        for (Map.Entry<String, int[]> e : stats.entrySet()) {
            int t = e.getValue()[0], m = e.getValue()[1];
            if (t == 0) continue;
            REAL_OUT.printf("%-12s %6d %6d %6.1f%%%n",
                    e.getKey(), t, m, 100.0 * m / t);
        }
        REAL_OUT.println("──────────────────────────────────────");
        REAL_OUT.printf("%-12s %6d %6d %6.1f%%%n",
                "OVERALL", grandTotal, grandMatch, 100.0 * grandMatch / grandTotal);
        REAL_OUT.println("══════════════════════════════════════");
        REAL_OUT.println("Details saved to: " + outFile);
    }

    // ── AI evaluation ─────────────────────────────────────────────────────────

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
}
