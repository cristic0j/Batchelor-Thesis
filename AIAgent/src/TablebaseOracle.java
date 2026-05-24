/**
 * TablebaseOracle
 * ===============
 * Callback interface that lets external code plug a perfect-play endgame
 * database into the NegaMax search engine.
 *
 * The oracle is queried for every internal node whose total piece count is
 * within the registered threshold.  If the position is found in the database
 * the search tree is pruned at that node; otherwise the normal alpha-beta
 * evaluation continues.
 *
 * *** IMPORTANT — return value convention ***
 *
 * Values must always be expressed from the GLOBAL first-player perspective
 * (Black / B_SIDE), NOT from the perspective of whoever is currently to move.
 * This matches the NegaMax score formula used in AbstractPlayer / NegaMaxPlayer,
 * which uses  score = side × WIN_SCORE  for a Black win  and
 *             score = −side × WIN_SCORE  for a White win.
 *
 *   1   → WIN  for Black (B_SIDE) — regardless of who is to move
 *   0   → WIN  for White (W_SIDE) / LOSS for Black
 *   2   → UNCERTAIN / DRAW
 *  -1   → position not found in this database (fall through to normal search)
 */
public interface TablebaseOracle {

    /**
     * Look up the current board position.
     *
     * <p>The return value must follow the <em>global</em> Black-perspective
     * convention described in the class javadoc — never flip it based on
     * {@code b.sideToMove}.</p>
     *
     * @param b  the current board state (read-only; do NOT modify it)
     * @return   1 = Black wins, 0 = White wins, 2 = UNCERTAIN, -1 = not found
     */
    int lookup(Board b);
}
