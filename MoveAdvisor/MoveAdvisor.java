package MoveAdvisor;

import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.dataClasses.Outcome;
import EndGameDatabase.logicModules.Indexing;
import EndGameDatabase.logicModules.MoveListGenerator;
import EndGameDatabase.logicModules.RandomAccessFile.FileReaderWriter;
import dataClasses.BoardState;
import dataClasses.Move;
import logicModules.MoveProcessor;

import EndGameDatabase.dataClasses.IndexingInput;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MoveAdvisor {

    private static final int BOARD_SIZE = 36;
    private static final String DATA_PATH = "EndGameDatabase/data/";

    private final BinomialTable table;

    public MoveAdvisor() {
        this.table = new BinomialTable(BOARD_SIZE);
    }

    /**
     * Given a board state and the active player (1 or 2), looks up the current
     * position in the endgame database and suggests the best available move.
     *
     * Outcome for active player:
     *   WIN  — prints a winning move (one that puts opponent in LOSS)
     *   DRAW — prints the best draw-preserving move (opponent stays in DRAW or LOSS)
     *   LOSS — reports that no winning or drawing move exists
     */
    public void advise(int[] board, int activePlayer) throws IOException {
        int p1Count = countPieces(board, 1);
        int p2Count = countPieces(board, 2);

        int myCount  = (activePlayer == 1) ? p1Count : p2Count;
        int oppCount = (activePlayer == 1) ? p2Count : p1Count;
        // When player 2 is active, their pieces (value=2) are the "P1" side of the index
        boolean flip = (activePlayer == 2);

        String currentFile = DATA_PATH + "moves" + myCount + "v" + oppCount + ".txt";
        if (!new File(currentFile).exists()) {
            System.out.println("Database file not found: " + currentFile);
            System.out.println("This position is not covered by the available database.");
            return;
        }

        long idx      = Indexing.indexInputToFilePosition(Indexing.createIndexingInput(board, flip), table);
        int  rawVal   = FileReaderWriter.readValueFromFile(currentFile, idx);
        if (rawVal < 0 || rawVal > 2) {
            System.out.println("Invalid database entry at index " + idx);
            return;
        }

        Outcome current = Outcome.values()[rawVal];
        System.out.println("Current position outcome for player " + activePlayer + ": " + current);

        if (current == Outcome.LOSS) {
            System.out.println("No winning or drawing move exists — all moves lead to a loss.");
            return;
        }

        // Try take moves first (higher value), then simple moves
        List<Move> takeMoves   = MoveListGenerator.getTakeMoveList(board, activePlayer);
        List<Move> simpleMoves = MoveListGenerator.getSimpleMoveList(board, activePlayer);

        Move bestWin  = null;
        Move bestDraw = null;

        for (Move move : takeMoves) {
            Outcome opp = opponentOutcomeAfterMove(move, activePlayer);
            if (opp == Outcome.LOSS && bestWin  == null) bestWin  = move;
            if (opp == Outcome.DRAW && bestDraw == null) bestDraw = move;
            if (bestWin != null && bestDraw != null) break;
        }

        for (Move move : simpleMoves) {
            if (bestWin != null && bestDraw != null) break;
            Outcome opp = opponentOutcomeAfterMove(move, activePlayer);
            if (opp == Outcome.LOSS && bestWin  == null) bestWin  = move;
            if (opp == Outcome.DRAW && bestDraw == null) bestDraw = move;
        }

        if (current == Outcome.WIN) {
            if (bestWin != null) {
                System.out.println("Winning move: " + formatMove(bestWin));
            } else {
                // WIN but no single-ply winning move found — multi-step win, show draw path
                System.out.println("Position is WIN (multi-step). No immediate knockout found.");
                if (bestDraw != null)
                    System.out.println("Best available move (draw-preserving): " + formatMove(bestDraw));
                else
                    System.out.println("No safe move found in the database for this position.");
            }
        } else { // DRAW
            Move best = bestDrawMove(bestWin, bestDraw);
            if (best != null) {
                System.out.println("Best draw move: " + formatMove(best));
            } else {
                System.out.println("No draw-preserving move found in the database for this position.");
            }
        }
    }

    // For a DRAW position, prefer a LOSS-for-opponent move if it exists (unexpected but handle it),
    // otherwise prefer DRAW-for-opponent.
    private static Move bestDrawMove(Move win, Move draw) {
        if (win  != null) return win;
        if (draw != null) return draw;
        return null;
    }

    /**
     * Applies the move, then looks up the resulting position from the opponent's perspective.
     * Returns null if the resulting table is not in the database.
     */
    private Outcome opponentOutcomeAfterMove(Move move, int activePlayer) throws IOException {
        BoardState result    = MoveProcessor.movePiece(move);
        int[]      newBoard  = result.getBoardMap();

        // Recount from board — MoveListGenerator creates BoardState(board, 0, 0) so piece
        // counts in result are unreliable; counting directly is always correct.
        int newP1 = countPieces(newBoard, 1);
        int newP2 = countPieces(newBoard, 2);

        int opponent    = (activePlayer == 1) ? 2 : 1;
        int oppMyCount  = (opponent == 1) ? newP1 : newP2;
        int oppOppCount = (opponent == 1) ? newP2 : newP1;
        boolean oppFlip = (opponent == 2);

        String oppFile = DATA_PATH + "moves" + oppMyCount + "v" + oppOppCount + ".txt";
        if (!new File(oppFile).exists()) return null;

        long idx = Indexing.indexInputToFilePosition(
            Indexing.createIndexingInput(newBoard, oppFlip), table);
        int val = FileReaderWriter.readValueFromFile(oppFile, idx);
        if (val < 0 || val > 2) return null;

        return Outcome.values()[val];
    }

    private static String formatMove(Move move) {
        int from = move.getOldCoordinate().value;
        int to   = move.getNewCoordinate().value;
        return String.format("%d -> %d  (r%d,c%d -> r%d,c%d)",
            from, to,
            from / 6, from % 6,
            to   / 6, to   % 6);
    }

    /**
     * Returns a uniformly random board with exactly p1Count P1 pieces and p2Count P2 pieces.
     * Picks a random index in [0, C(36, p1+p2) * C(p1+p2, p1)) and decodes it.
     */
    public int[] randomBoard(int p1Count, int p2Count) {
        long total = Indexing.computeTheNumberOfPossiblePositions(p1Count, p2Count, BOARD_SIZE, table);
        long idx   = ThreadLocalRandom.current().nextLong(total);
        IndexingInput input = Indexing.filePositionToIndexingInput(idx, BOARD_SIZE, p1Count, p2Count, table);
        return input.getMapFromIndexingInput(BOARD_SIZE);
    }

    private static int countPieces(int[] board, int player) {
        int count = 0;
        for (int v : board) if (v == player) count++;
        return count;
    }
}
