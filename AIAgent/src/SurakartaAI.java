/**
 * SurakartaAI
 * ===========
 * One-call API: give it a flat 36-element board map, get back WIN / LOSS / DRAW.
 *
 * Board map convention (matches the user's int[] format):
 *   0  = empty
 *   1  = Player 1 pieces  (always moves first)
 *   2  = Player 2 pieces
 *
 * Example:
 *   int[] boardMap = {
 *       2,0,0,0,0,2,
 *       0,1,0,0,1,0,
 *       0,0,0,0,0,0,
 *       0,0,0,0,0,0,
 *       0,0,0,0,0,0,
 *       2,0,0,0,0,2,
 *   };
 *   SurakartaAI.Result r = SurakartaAI.evaluate(boardMap);
 *   // r.outcome → WIN / LOSS / DRAW / UNKNOWN
 *   // r.bestMove → "b2-c3" style notation
 *   // r.certain  → true if proven, false if heuristic
 *
 * Build & run:
 *   javac -d ../bin *.java
 *   java  -cp bin:data SurakartaAI
 */
public class SurakartaAI {

    // ------------------------------------------------------------------
    // Public result type
    // ------------------------------------------------------------------

    public enum Outcome { WIN, LOSS, DRAW, UNKNOWN }

    public static class Result {
        public final Outcome outcome;
        public final String  bestMove;   // e.g. "b2-c3"
        public final int     score;      // raw alpha-beta score
        public final boolean certain;    // true = mathematically proven

        Result(Outcome outcome, String bestMove, int score, boolean certain) {
            this.outcome  = outcome;
            this.bestMove = bestMove;
            this.score    = score;
            this.certain  = certain;
        }

        @Override
        public String toString() {
            return "Outcome : " + outcome  + (certain ? "  (CERTAIN)" : "  (heuristic)")
                 + "\nBestMove: " + bestMove
                 + "\nScore   : " + score;
        }
    }

    // ------------------------------------------------------------------
    // Singleton internals — built once, reused across calls
    // ------------------------------------------------------------------

    private static final int WIDTH = 6;

    private static final SurakartaGame  GAME;
    private static final NegaMaxPlayer  AGENT;
    private static       EndGameDatabaseFile EGDB;   // null if not built yet

    static {
        Board.setSize(WIDTH, WIDTH);
        GAME  = new SurakartaGame(new SurakartaKanazawaEvaluator());
        AGENT = new NegaMaxPlayer(GAME);
        AGENT.setFixedTime(5000);   // default 5-second budget

        // Try to load a pre-built endgame database
        SurakartaGame g2 = new SurakartaGame();
        g2.isEGDB    = true;
        g2.isMemEGDB = true;
        EGDB = EndGameDatabaseFile.createEndgameDatabase(g2);
    }

    // ------------------------------------------------------------------
    // Main public method
    // ------------------------------------------------------------------

    /**
     * Evaluate a Surakarta position and return the outcome for Player 1
     * (the side that moves first).
     *
     * @param boardMap flat 36-element array, row-major order.
     *                 0 = empty, 1 = Player 1, 2 = Player 2.
     * @return Result with outcome, best move string, score, and certainty flag.
     */
    public static Result evaluate(int[] boardMap) {
        return evaluate(boardMap, 5000);
    }

    /**
     * Same as {@link #evaluate(int[])} but with a custom time budget.
     *
     * @param boardMap     flat 36-element board array (0/1/2)
     * @param timeLimitMs  milliseconds the alpha-beta search may use
     */
    public static Result evaluate(int[] boardMap, long timeLimitMs) {

        if (boardMap.length != WIDTH * WIDTH)
            throw new IllegalArgumentException(
                "boardMap must have exactly " + (WIDTH * WIDTH) + " elements");

        AGENT.setFixedTime(timeLimitMs);

        // 1. Convert int[] → byte[6][6]  (0→EMPTY, 1→B_PIECE, 2→W_PIECE)
        byte[][] space = toByteSpace(boardMap);

        // 2. Build a Board from that space
        Board board = buildBoard(space);

        // 3. Try endgame database first (instant + 100% certain for ≤9 pieces)
        int totalPieces = board.B_Counter + board.W_Counter;
        if (EGDB != null && GAME.hasDatabase(board)) {
            try {
                int info = EGDB.getInfo(board);
                if (info == EndGameDatabaseFile.WIN) {
                    Move m = AGENT.search(board);
                    return new Result(Outcome.WIN,  moveStr(m), m != null ? m.score : 0, true);
                }
                if (info == EndGameDatabaseFile.LOSS) {
                    Move m = AGENT.search(board);
                    return new Result(Outcome.LOSS, moveStr(m), m != null ? m.score : 0, true);
                }
                if (info == EndGameDatabaseFile.DRAW) {
                    Move m = AGENT.search(board);
                    return new Result(Outcome.DRAW, moveStr(m), m != null ? m.score : 0, true);
                }
            } catch (java.io.IOException e) {
                // fall through to alpha-beta
            }
        }

        // 4. Alpha-beta search
        Move best = AGENT.search(board);

        if (best == null)
            return new Result(Outcome.UNKNOWN, "none", 0, false);

        int score = best.score;

        // Score >= WIN_SCORE means the agent found a complete forced sequence
        if (score >= Board.WIN_SCORE)
            return new Result(Outcome.WIN,  moveStr(best), score, true);
        if (score <= -Board.WIN_SCORE)
            return new Result(Outcome.LOSS, moveStr(best), score, true);

        // Heuristic territory
        Outcome likely = score > 0 ? Outcome.WIN
                       : score < 0 ? Outcome.LOSS
                       : Outcome.DRAW;
        return new Result(likely, moveStr(best), score, false);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Convert flat int[36] to byte[6][6] using 0→EMPTY, 1→B_PIECE, 2→W_PIECE */
    private static byte[][] toByteSpace(int[] map) {
        byte[][] space = new byte[WIDTH][WIDTH];
        for (int row = 0; row < WIDTH; row++) {
            for (int col = 0; col < WIDTH; col++) {
                int v = map[row * WIDTH + col];
                space[row][col] = (v == 1) ? Board.B_PIECE
                                : (v == 2) ? Board.W_PIECE
                                : Board.EMPTY;
            }
        }
        return space;
    }

    /**
     * Build a fully-initialised Board from a byte[6][6] space.
     * Player 1 (B_PIECE) moves first.
     */
    private static Board buildBoard(byte[][] space) {
        Board b = new Board(GAME.getGenerator(), GAME);
        b.setSpace(space);                    // copy pieces into space[][]
        b.initialise(Board.B_SIDE);           // build piece lists + bit-lines
        return b;
    }

    /** Null-safe move → human-readable string */
    private static String moveStr(Move m) {
        return m != null ? m.toString() : "none";
    }

    // ------------------------------------------------------------------
    // Optional: build the endgame database (run once, saves to disk)
    // ------------------------------------------------------------------

    /**
     * Build and save the endgame database for all Surakarta positions with
     * ≤ 9 pieces.  This is a one-time offline operation (several minutes).
     * After it runs, every future call to evaluate() with ≤ 9 pieces gets an
     * instant, 100%-certain answer.
     */
    public static void buildEndgameDatabase() {
        Board.setSize(WIDTH, WIDTH);
        SurakartaGame g = new SurakartaGame();
        g.isMemEGDB = false;
        Log.println("Building Surakarta endgame database...");
        long t = System.currentTimeMillis();
        EndGameDatabaseFile.createEndgameDatabase(g);
        Log.println("Done in " + (System.currentTimeMillis() - t) / 1000 + " s");
    }

    // ------------------------------------------------------------------
    // Demo main
    // ------------------------------------------------------------------

    public static void main(String[] args) {

        System.out.println("========================================");
        System.out.println("  Surakarta AI  —  Position Evaluator  ");
        System.out.println("========================================\n");

        // The exact board the user provided
        int[] boardMap = {
            2, 0, 0, 0, 0, 2,
            0, 1, 0, 0, 1, 0,
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
            2, 0, 0, 0, 0, 2,
        };

        printBoard(boardMap);

        System.out.println("Analysing... (up to 5 s)");
        Result r = evaluate(boardMap);

        System.out.println();
        System.out.println(r);
        System.out.println();

        if (!r.certain) {
            int pieces = countPieces(boardMap);
            if (pieces <= 9) {
                System.out.println("Tip: " + pieces + " pieces on board — run buildEndgameDatabase()");
                System.out.println("     once to get a 100%-certain answer for all ≤9-piece positions.");
            } else {
                System.out.println("Tip: increase the time budget for a deeper search,");
                System.out.println("     or wait for the endgame to get a certain result.");
            }
        }
    }

    // ------------------------------------------------------------------
    // Pretty-print helpers
    // ------------------------------------------------------------------

    private static void printBoard(int[] map) {
        System.out.println("Board (1=Player1, 2=Player2, 0=empty):");
        System.out.println("  a b c d e f");
        for (int row = 0; row < 6; row++) {
            System.out.print((row + 1) + " ");
            for (int col = 0; col < 6; col++) {
                int v = map[row * 6 + col];
                System.out.print((v == 1 ? "1" : v == 2 ? "2" : ".") + " ");
            }
            System.out.println();
        }
        System.out.println();
    }

    private static int countPieces(int[] map) {
        int n = 0;
        for (int v : map) if (v != 0) n++;
        return n;
    }
}
