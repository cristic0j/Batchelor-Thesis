package MoveAdvisor;

/**
 * Driver for MoveAdvisor. Replace the board and activePlayer with your actual game state.
 *
 * Board encoding:
 *   0 = empty, 1 = Player 1's piece, 2 = Player 2's piece
 *   Squares are indexed 0-35, row-major (square 0 = top-left, square 35 = bottom-right).
 *
 * Run:
 *   java -cp bin MoveAdvisor.main
 */
public class main {

    public static void main(String[] args) throws Exception {
        MoveAdvisor advisor = new MoveAdvisor();

        // ── Fixed position ────────────────────────────────────────────────────
        int[] board = {
            0,0,0,0,2,0,
            0,0,1,0,2,0,
            0,0,0,0,1,0,
            0,0,0,0,0,0,
            0,0,0,0,0,0,
            0,0,0,0,0,0,
        };

        printBoard(board);
        advisor.advise(board, 1);

        System.out.println();

        // ── Random position (2 P1 pieces, 2 P2 pieces) ───────────────────────
        int[] random = advisor.randomBoard(2, 2);
        System.out.println("Random board:");
        printBoard(random);
        advisor.advise(random, 1);
    }

    private static void printBoard(int[] board) {
        System.out.println("Board (. = empty, 1 = P1, 2 = P2):");
        for (int i = 0; i < 36; i++) {
            System.out.print(board[i] == 0 ? "." : board[i]);
            System.out.print((i + 1) % 6 == 0 ? "\n" : " ");
        }
        System.out.println();
    }
}
