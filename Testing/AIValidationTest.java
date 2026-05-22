// No package — default package so it can directly use AIAgent classes
// while also importing EndGameDatabase named-package classes.

import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.dataClasses.IndexingInput;
import EndGameDatabase.logicModules.Indexing;
import EndGameDatabase.logicModules.RandomAccessFile.FileReaderWriter;

import java.io.*;
import java.util.*;

/**
 * Validation test: compares EndGameDatabase retrograde outcomes
 * against the AIAgent alpha-beta search on 100 random positions per table.
 *
 * Output: Testing/results.csv
 *   columns: p1, p2, index, db_outcome, ai_outcome, match
 *
 * Build & run (from project root):
 *   bash Testing/build_and_run_test.sh
 */
public class AIValidationTest {

    // ── Config ────────────────────────────────────────────────────────────────
    static final int  BOARD_SIZE        = 36;
    static final int  SAMPLES_PER_TABLE = 100;
    static final int  AI_THINK_MS       = 1000;   // ms per position
    static final long RANDOM_SEED       = 42L;
    static final String DB_PATH         = "EndGameDatabase/data/";
    static final String OUT_FILE        = "Testing/results.csv";

    // DB outcome encoding
    static final int DB_LOSS      = 0;
    static final int DB_WIN       = 1;
    static final int DB_UNCERTAIN = 2;

    // Silence engine noise
    static final PrintStream REAL_OUT = System.out;
    static final PrintStream NULL_OUT = new PrintStream(new OutputStream() {
        public void write(int b) {}
    });

    // ── Main ──────────────────────────────────────────────────────────────────
    public static void main(String[] args) throws Exception {

        // Init AI engine once (silently) — reused across all positions
        System.setOut(NULL_OUT);
        Board.setSize(6, 6);
        SurakartaGame game = new SurakartaGame(new SurakartaKanazawaEvaluator());
        System.setOut(REAL_OUT);

        BinomialTable binTable = new BinomialTable(BOARD_SIZE);
        Random rng = new Random(RANDOM_SEED);

        new File(OUT_FILE).getParentFile().mkdirs();

        int totalTests = 0, totalMatches = 0;

        try (PrintWriter csv = new PrintWriter(new FileWriter(OUT_FILE))) {

            csv.println("p1,p2,index,db_outcome,ai_outcome,match");

            // Iterate over every table that exists on disk
            for (int p1 = 1; p1 <= 6; p1++) {
                for (int p2 = 1; p2 <= 6; p2++) {

                    String path = DB_PATH + "moves" + p1 + "v" + p2 + ".txt";
                    if (!new File(path).exists()) continue;

                    long total = Indexing.computeTheNumberOfPossiblePositions(
                            p1, p2, BOARD_SIZE, binTable);

                    int n = (int) Math.min(SAMPLES_PER_TABLE, total);
                    REAL_OUT.printf("%-12s  %,12d positions  → sampling %d%n",
                            p1 + "v" + p2, total, n);

                    // Pick n unique random indexes
                    Set<Long> picked = new LinkedHashSet<>();
                    while (picked.size() < n) {
                        picked.add((long)(rng.nextDouble() * total));
                    }

                    int tableMatches = 0;

                    for (long idx : picked) {

                        // ── DB side ───────────────────────────────────────
                        int dbVal = FileReaderWriter.readValueFromFile(path, idx);
                        String dbOutcome = label(dbVal);

                        // ── Convert index → board ─────────────────────────
                        IndexingInput inp = Indexing.filePositionToIndexingInput(
                                idx, BOARD_SIZE, p1, p2, binTable);
                        int[] boardMap = inp.getMapFromIndexingInput(BOARD_SIZE);

                        // ── AI side ───────────────────────────────────────
                        String aiOutcome = evaluateWithAI(game, boardMap);

                        boolean match = dbOutcome.equals(aiOutcome);
                        if (match) { tableMatches++; totalMatches++; }
                        totalTests++;

                        csv.printf("%d,%d,%d,%s,%s,%b%n",
                                p1, p2, idx, dbOutcome, aiOutcome, match);
                    }

                    csv.flush();
                    REAL_OUT.printf("  matches: %d / %d%n", tableMatches, n);
                }
            }
        }

        REAL_OUT.println();
        REAL_OUT.printf("=== TOTAL: %d / %d matches  (%.1f%%)%n",
                totalMatches, totalTests,
                totalTests == 0 ? 0.0 : 100.0 * totalMatches / totalTests);
        REAL_OUT.println("Results saved to: " + OUT_FILE);
    }

    // ── AI evaluation ─────────────────────────────────────────────────────────

    /**
     * Evaluate boardMap (int[36], 0/1/2) from Player 1's perspective.
     * DB convention: Player 1 is always the active (to-move) player.
     * Returns "WIN", "LOSS", or "UNCERTAIN".
     */
    static String evaluateWithAI(SurakartaGame game, int[] boardMap) {
        System.setOut(NULL_OUT);
        try {
            Board board = new Board(game.getGenerator(), game);

            // Load board: 0→EMPTY, 1→B_PIECE, 2→W_PIECE(-1)
            for (int row = 0; row < Board.HEIGHT; row++) {
                for (int col = 0; col < Board.WIDTH; col++) {
                    int v = boardMap[row * Board.WIDTH + col];
                    board.space[row][col] = (v == 2) ? Board.W_PIECE : (byte) v;
                }
            }
            board.sideToMove = Board.B_SIDE;   // P1 to move
            board.initialise(Board.B_SIDE);

            // Immediate terminal check (one side has no pieces)
            int winSide = board.checkForWin();
            if (winSide == Board.B_SIDE) return "WIN";
            if (winSide == Board.W_SIDE) return "LOSS";

            // Alpha-beta search
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    static String label(int v) {
        switch (v) {
            case DB_LOSS:      return "LOSS";
            case DB_WIN:       return "WIN";
            case DB_UNCERTAIN: return "UNCERTAIN";
            default:           return "UNKNOWN";
        }
    }
}
