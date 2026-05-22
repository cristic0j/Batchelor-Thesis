import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Main entry point for the Surakarta AI Agent.
 *
 * ── HOW TO USE ────────────────────────────────────────────────────────────────
 *
 *  1. Set USE_CUSTOM_BOARD = true  to analyse your own position.
 *     Set USE_CUSTOM_BOARD = false to use the standard starting configuration.
 *
 *  2. Fill in CUSTOM_BOARD (flat int[36], row-major, top-left = index 0):
 *       0  = empty
 *       1  = Player 1 / Black  (+)
 *       2  = Player 2 / White  (-)
 *
 *     Visual layout (index per cell):
 *       [ 0][ 1][ 2][ 3][ 4][ 5]   ← row 0 (top)
 *       [ 6][ 7][ 8][ 9][10][11]   ← row 1
 *       [12][13][14][15][16][17]   ← row 2
 *       [18][19][20][21][22][23]   ← row 3
 *       [24][25][26][27][28][29]   ← row 4
 *       [30][31][32][33][34][35]   ← row 5 (bottom)
 *
 *  3. Set SIDE_TO_MOVE:
 *       Board.B_SIDE ( 1) = Player 1 / Black to move
 *       Board.W_SIDE (-1) = Player 2 / White to move
 *
 *  4. Optionally adjust SEARCH_TIME_MS (milliseconds the AI gets to think).
 *
 * Build:  bash build.sh            (from the AIAgent/ directory)
 * Run:    java -cp bin:data Main
 *
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class Main {

    // =========================================================================
    //  ★ EDIT THESE TO CONFIGURE YOUR RUN
    // =========================================================================

    /** true = use CUSTOM_BOARD below; false = use standard starting position */
    static final boolean USE_CUSTOM_BOARD = true;

    /**
     * Your board position — flat int[36], row-major.
     *   0 = empty,  1 = Player 1 / Black (+),  2 = Player 2 / White (-)
     */
    static final int[] CUSTOM_BOARD = {
        0, 1, 0, 0, 1, 0,
        0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0,
        0, 0, 2, 2, 0, 0,
        0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0,
    };

    /** Who is to move: Board.B_SIDE (1) = Player 1, Board.W_SIDE (-1) = Player 2 */
    static final int SIDE_TO_MOVE = Board.B_SIDE;

    /** Time budget for the AI search in milliseconds */
    static final int SEARCH_TIME_MS = 3000;

    // =========================================================================
    //  MAIN
    // =========================================================================

    static final PrintStream REAL_OUT = System.out;
    static final PrintStream NULL_OUT = new PrintStream(new OutputStream() {
        public void write(int b) {}
    });

    public static void main(String[] args) {

        // Silence noisy engine init output
        System.setOut(NULL_OUT);

        Board.setSize(6, 6);
        SurakartaGame game = new SurakartaGame(new SurakartaKanazawaEvaluator());
        Board board = new Board(game.getGenerator(), game);

        if (USE_CUSTOM_BOARD) {
            loadCustomBoard(board, CUSTOM_BOARD, SIDE_TO_MOVE);
        } else {
            game.startingConfiguration(board);
            board.initialise(board.sideToMove);
        }

        System.setOut(REAL_OUT);

        // Print the board
        REAL_OUT.println(board);

        String mover = (board.sideToMove == Board.B_SIDE) ? "Player 1 (+)" : "Player 2 (-)";
        REAL_OUT.println("Player 1 (+) pieces : " + board.B_Counter);
        REAL_OUT.println("Player 2 (-) pieces : " + board.W_Counter);
        REAL_OUT.println("Side to move        : " + mover);
        REAL_OUT.println();

        // Check if already over (one side has no pieces)
        int winSide = board.checkForWin();
        if (winSide == Board.B_SIDE) {
            REAL_OUT.println("Result: PLAYER 1 WINS");
            return;
        }
        if (winSide == Board.W_SIDE) {
            REAL_OUT.println("Result: PLAYER 2 WINS");
            return;
        }

        // Run the search silently
        System.setOut(NULL_OUT);
        NegaMaxPlayer agent = new NegaMaxPlayer(game);
        agent.setFixedTime(SEARCH_TIME_MS);
        Move bestMove = agent.search(board);
        System.setOut(REAL_OUT);

        if (bestMove == null) {
            REAL_OUT.println("Result: No legal moves — game over.");
            return;
        }

        verdict(bestMove.score, board.sideToMove);
    }

    // =========================================================================
    //  HELPERS
    // =========================================================================

    /**
     * Translate the engine score into a plain-English verdict and print it.
     *
     * Definitive:  score >= WIN_SCORE        → WIN  for the side to move
     *              score <= -WIN_SCORE        → WIN  for the opponent
     *              |score| <= DRAW_THRESHOLD  → DRAW (equal or drawn by repetition contempt)
     * Uncertain:   everything else            → percentage estimate
     *
     * Board.DRAW_SCORE = -150 is the engine's draw-contempt value.
     * Scores within ±DRAW_THRESHOLD of that are treated as drawn.
     */
    static final int DRAW_THRESHOLD = 50;   // scores within DRAW_SCORE ± this → DRAW

    static void verdict(int score, int sideToMove) {

        String p1 = "Player 1 (+)";
        String p2 = "Player 2 (-)";
        String mover   = (sideToMove == Board.B_SIDE) ? p1 : p2;
        String opponent = (sideToMove == Board.B_SIDE) ? p2 : p1;

        // Forced win/loss
        if (score >= Board.WIN_SCORE) {
            REAL_OUT.println("Result: " + mover + " WINS");
            return;
        }
        if (score <= -Board.WIN_SCORE) {
            REAL_OUT.println("Result: " + opponent + " WINS");
            return;
        }

        // Draw — engine returns DRAW_SCORE (-150) as its draw-contempt value,
        // so treat anything within ±DRAW_THRESHOLD of it as a draw.
        if (Math.abs(score - Board.DRAW_SCORE) <= DRAW_THRESHOLD) {
            REAL_OUT.println("Result: DRAW");
            return;
        }

        // Non-decisive — map score to a winning-chance percentage for Player 1.
        // Engine scores range roughly -2000 (dominant for P2) to +2000 (dominant for P1).
        // We map that range linearly onto 5%–95%.
        int scoreFromP1 = (sideToMove == Board.B_SIDE) ? score : -score;
        double pct = 50.0 + (scoreFromP1 / 2000.0) * 45.0;
        pct = Math.max(5, Math.min(95, pct));
        int rounded = (int) Math.round(pct);

        if (rounded >= 50) {
            REAL_OUT.println("Result: Unclear — Player 1 winning chance ~" + rounded + "%");
        } else {
            REAL_OUT.println("Result: Unclear — Player 2 winning chance ~" + (100 - rounded) + "%");
        }
    }

    /**
     * Load a custom position into {@code board}.
     *
     * @param flatBoard  flat int[36] — 0 empty, 1 Player1/Black, 2 Player2/White (row-major)
     * @param sideToMove Board.B_SIDE or Board.W_SIDE
     */
    static void loadCustomBoard(Board board, int[] flatBoard, int sideToMove) {

        if (flatBoard.length != Board.WIDTH * Board.HEIGHT) {
            throw new IllegalArgumentException(
                "flatBoard must have " + (Board.WIDTH * Board.HEIGHT)
                + " elements, got " + flatBoard.length);
        }

        for (int i = 0; i < flatBoard.length; i++) {
            if (flatBoard[i] != 0 && flatBoard[i] != 1 && flatBoard[i] != 2) {
                throw new IllegalArgumentException(
                    "flatBoard[" + i + "] = " + flatBoard[i]
                    + " — only 0 (empty), 1 (Player 1), or 2 (Player 2) are allowed.");
            }
        }

        for (int row = 0; row < Board.HEIGHT; row++) {
            for (int col = 0; col < Board.WIDTH; col++) {
                int val = flatBoard[row * Board.WIDTH + col];
                board.space[row][col] = (val == 2) ? Board.W_PIECE : (byte) val;
            }
        }

        board.sideToMove = sideToMove;
        board.initialise(sideToMove);
    }
}
