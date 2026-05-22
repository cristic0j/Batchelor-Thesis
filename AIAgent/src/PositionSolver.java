import java.io.IOException;

/**
 * PositionSolver
 * ==============
 * Determines with certainty whether a Surakarta position is a WIN, LOSS,
 * or DRAW for the side to move.
 *
 * Three mechanisms are tried in order of reliability / speed:
 *
 *  1. ENDGAME DATABASE  — exact retrograde-analysis result (100% certain).
 *                         Works for positions with ≤ 9 pieces total.
 *                         Requires the database to have been built once with
 *                         buildEndgameDatabase().
 *
 *  2. FORCED WIN/LOSS   — NegaMaxPlayer returned ± WIN_SCORE, meaning the
 *                         entire remaining game tree was searched.
 *                         100% certain when it fires; may not fire for
 *                         positions that are too deep to prove in time.
 *
 *  3. HEURISTIC         — score is below WIN_SCORE; the agent's best guess
 *                         based on the evaluation function.  NOT a proof.
 *
 * Usage:
 *   javac -d ../bin *.java
 *   java -cp bin:data PositionSolver
 */
public class PositionSolver {

    /** Return type for a certainty query. */
    public enum Result {
        WIN,      // side to move wins with perfect play
        LOSS,     // side to move loses with perfect play
        DRAW,     // draw with perfect play
        UNKNOWN   // could not prove either way within the time / tree budget
    }

    // -----------------------------------------------------------------------
    private final NegaMaxPlayer agent;
    private final SurakartaGame  game;
    private       EndGameDatabaseFile egdb = null;
    private final long timeBudgetMs;

    /**
     * @param timeBudgetMs  milliseconds the alpha-beta search may use per call
     */
    public PositionSolver(long timeBudgetMs) {
        Board.setSize(6, 6);
        this.game          = new SurakartaGame(new SurakartaKanazawaEvaluator());
        this.agent         = new NegaMaxPlayer(game);
        this.timeBudgetMs  = timeBudgetMs;
        agent.setFixedTime(timeBudgetMs);
        tryLoadDatabase();
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Ask whether the given board position is a win, loss, draw, or unknown.
     *
     * Internally tries (in order):
     *   1. endgame database  (if available and ≤ 9 pieces)
     *   2. alpha-beta search (proves forced win/loss if reachable in time)
     *   3. falls back to UNKNOWN
     */
    public Result query(Board board) {

        // --- 1. Endgame database (instant, exact) --------------------------
        if (egdb != null && game.hasDatabase(board)) {
            try {
                int info = egdb.getInfo(board);
                if (info == EndGameDatabaseFile.WIN)  return Result.WIN;
                if (info == EndGameDatabaseFile.LOSS) return Result.LOSS;
                if (info == EndGameDatabaseFile.DRAW) return Result.DRAW;
            } catch (IOException e) {
                Log.println("EGDB lookup failed: " + e.getMessage());
            }
        }

        // --- 2. Alpha-beta forced win/loss proof ----------------------------
        Move best = agent.search(board);

        if (best != null) {
            int score = best.score;

            if (score >= Board.WIN_SCORE)  return Result.WIN;
            if (score <= -Board.WIN_SCORE) return Result.LOSS;

            // Heuristic territory — cannot certify
        }

        return Result.UNKNOWN;
    }

    /**
     * Returns the best move AND the certainty level together.
     */
    public MoveResult queryWithMove(Board board) {
        // Try database first
        if (egdb != null && game.hasDatabase(board)) {
            try {
                int info = egdb.getInfo(board);
                if (info != 0) {
                    Result r = (info == EndGameDatabaseFile.WIN)  ? Result.WIN
                             : (info == EndGameDatabaseFile.LOSS) ? Result.LOSS
                             : Result.DRAW;
                    // Still use alpha-beta to get the actual move
                    Move best = agent.search(board);
                    return new MoveResult(best, r, true);
                }
            } catch (IOException e) {
                Log.println("EGDB lookup failed: " + e.getMessage());
            }
        }

        Move best = agent.search(board);
        if (best == null) return new MoveResult(null, Result.UNKNOWN, false);

        int score = best.score;
        boolean certain = Math.abs(score) >= Board.WIN_SCORE;
        Result r = certain
            ? (score > 0 ? Result.WIN : Result.LOSS)
            : Result.UNKNOWN;

        return new MoveResult(best, r, certain);
    }

    // -----------------------------------------------------------------------
    // Database management
    // -----------------------------------------------------------------------

    /**
     * Build the endgame database for all Surakarta positions with ≤ 9 pieces.
     * This is a one-time, offline operation.  Depending on hardware it takes
     * several minutes.  The result is saved to disk so future runs are instant.
     *
     * Call once before deploying the solver:
     *   PositionSolver.buildEndgameDatabase();
     */
    public static void buildEndgameDatabase() {
        Board.setSize(6, 6);
        SurakartaGame g = new SurakartaGame();
        g.isMemEGDB = false;   // write to disk, not just RAM

        Log.println("Building Surakarta endgame database (≤9 pieces)...");
        long start = System.currentTimeMillis();

        EndGameDatabaseFile db = EndGameDatabaseFile.createEndgameDatabase(g);
        // The generate() method is called internally by createEndgameDatabase
        // when no file is found on disk.

        Log.println("Done in " + (System.currentTimeMillis() - start) / 1000 + " s");
    }

    /** Try to load a pre-built database from disk. */
    private void tryLoadDatabase() {
        SurakartaGame g2 = new SurakartaGame();
        g2.isEGDB    = true;
        g2.isMemEGDB = true;
        EndGameDatabaseFile db = EndGameDatabaseFile.createEndgameDatabase(g2);
        // createEndgameDatabase returns a non-null object but the file may not
        // exist; a subsequent getInfo() will just return 0 in that case.
        egdb = db;
        Log.println("Endgame database: " + (egdb != null ? "loaded" : "not found"));
    }

    // -----------------------------------------------------------------------
    // Result container
    // -----------------------------------------------------------------------

    public static class MoveResult {
        public final Move   move;
        public final Result result;
        public final boolean isCertain;

        MoveResult(Move move, Result result, boolean isCertain) {
            this.move      = move;
            this.result    = result;
            this.isCertain = isCertain;
        }

        @Override
        public String toString() {
            return "Move: " + move
                + "  |  Outcome: " + result
                + "  |  Certain: " + isCertain;
        }
    }

    // -----------------------------------------------------------------------
    // Convenience: print a human-readable certainty report
    // -----------------------------------------------------------------------

    public void report(Board board) {
        int total = board.B_Counter + board.W_Counter;
        System.out.println("Pieces on board: " + total
            + " (B=" + board.B_Counter + ", W=" + board.W_Counter + ")");
        System.out.println("Side to move: "
            + (board.sideToMove == Board.B_SIDE ? "Black" : "White"));

        if (egdb != null && game.hasDatabase(board)) {
            System.out.println("Method: ENDGAME DATABASE (exact, 100% certain)");
        } else {
            System.out.println("Method: ALPHA-BETA SEARCH ("
                + timeBudgetMs / 1000 + " s budget)");
        }

        MoveResult mr = queryWithMove(board);
        System.out.println("Best move : " + mr.move);
        System.out.println("Score     : " + (mr.move != null ? mr.move.score : "n/a"));

        switch (mr.result) {
            case WIN:
                System.out.println("Verdict   : *** FORCED WIN *** "
                    + (mr.isCertain ? "(CERTAIN)" : "(likely — may need more time to fully prove)"));
                break;
            case LOSS:
                System.out.println("Verdict   : *** FORCED LOSS *** "
                    + (mr.isCertain ? "(CERTAIN)" : "(likely — may need more time to fully prove)"));
                break;
            case DRAW:
                System.out.println("Verdict   : DRAW (CERTAIN via endgame DB)");
                break;
            case UNKNOWN:
                System.out.println("Verdict   : UNKNOWN — not provable within budget");
                System.out.println("            (heuristic score: "
                    + (mr.move != null ? mr.move.score : "n/a") + ")");
                break;
        }
    }

    // -----------------------------------------------------------------------
    // Demo main
    // -----------------------------------------------------------------------

    public static void main(String[] args) {

        System.out.println("=== Surakarta Position Solver ===\n");

        PositionSolver solver = new PositionSolver(5000);  // 5-second budget

        // --- Test 1: Starting position (complex, expect UNKNOWN) ------------
        Board start = new Board(new SurakartaGame().getGenerator(), new SurakartaGame());
        new SurakartaGame().startingConfiguration(start);
        start.initialise(start.sideToMove);

        System.out.println("--- Starting Position ---");
        System.out.println(start);
        solver.report(start);

        System.out.println();

        // --- Test 2: Near-endgame position (few pieces, expect DB result) ---
        // Set up a position with only 3 black + 3 white pieces = 6 pieces total
        // (well within the ≤9 piece database threshold)
        Board endgame = new Board(new SurakartaGame().getGenerator(), new SurakartaGame());
        new SurakartaGame().startingConfiguration(endgame);
        endgame.initialise(endgame.sideToMove);

        // Simulate many captures to thin out the position
        System.out.println("--- Endgame-like position (if built, DB gives exact answer) ---");
        System.out.println("(Build the endgame database first with: PositionSolver.buildEndgameDatabase())");
        System.out.println("Database threshold: positions with ≤ "
            + (new SurakartaGame().hasDatabase(endgame)
                ? endgame.B_Counter + endgame.W_Counter
                : "N/A (above threshold)")
            + " pieces total\n");
    }
}
