import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.dataClasses.IndexingInput;
import EndGameDatabase.logicModules.Indexing;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Scanner;

public class FindOutcome {

    static final int BOARD_SIZE  = 36;
    static final int SIDE        = 6;
    static final int BATCH_BYTES = 1 << 22; // 4 MB ≈ 16 M positions per batch

    static final int LOSS      = 0;
    static final int WIN       = 1;
    static final int UNCERTAIN = 2;

    public static void main(String[] args) throws IOException {
        Scanner sc = new Scanner(System.in);

        System.out.print("P1 pieces: ");
        int p1 = Integer.parseInt(sc.nextLine().trim());

        System.out.print("P2 pieces: ");
        int p2 = Integer.parseInt(sc.nextLine().trim());

        int target = -1;
        while (target < 0) {
            System.out.print("Looking for (LOSS / WIN / DRAW): ");
            target = parseOutcome(sc.nextLine().trim());
            if (target < 0) System.out.println("  Please enter LOSS, WIN, or DRAW.");
        }

        String path = "EndGameDatabase/data/moves" + p1 + "v" + p2 + ".txt";

        File f = new File(path);
        if (!f.exists()) {
            System.err.println("File not found: " + path);
            return;
        }

        BinomialTable table          = new BinomialTable(BOARD_SIZE);
        long          totalPositions = Indexing.computeTheNumberOfPossiblePositions(p1, p2, BOARD_SIZE, table);
        long          totalBytes     = (totalPositions + 3) / 4;

        System.out.printf("%nScanning %s  (%,d positions)...%n", path, totalPositions);

        byte[] buffer  = new byte[(int) Math.min(BATCH_BYTES, totalBytes)];
        boolean found  = false;

        try (RandomAccessFile raf = new RandomAccessFile(path, "r")) {
            long bytePos = 0;
            outer:
            while (bytePos < totalBytes) {
                int toRead = (int) Math.min(buffer.length, totalBytes - bytePos);
                raf.readFully(buffer, 0, toRead);

                for (int b = 0; b < toRead; b++) {
                    long baseIdx = (bytePos + b) * 4;
                    for (int bit = 0; bit < 4; bit++) {
                        long idx = baseIdx + bit;
                        if (idx >= totalPositions) break;

                        int shift = (3 - bit) * 2;
                        int val   = (buffer[b] >> shift) & 0b11;

                        if (val == target) {
                            System.out.printf("%nFound %s at index %,d%n", outcomeName(target), idx);
                            printBoard(idx, p1, p2, table);
                            found = true;
                            break outer;
                        }
                    }
                }
                bytePos += toRead;
            }
        }

        if (!found) {
            System.out.printf("%nNo %s position found in this table.%n", outcomeName(target));
        }
    }

    static int parseOutcome(String s) {
        switch (s.toUpperCase()) {
            case "LOSS": return LOSS;
            case "WIN":  return WIN;
            case "DRAW":
            case "UNCERTAIN": return UNCERTAIN;
            default: return -1;
        }
    }

    static String outcomeName(int v) {
        switch (v) {
            case LOSS:      return "LOSS";
            case WIN:       return "WIN";
            case UNCERTAIN: return "DRAW/UNCERTAIN";
            default:        return "UNKNOWN(" + v + ")";
        }
    }

    static void printBoard(long index, int p1, int p2, BinomialTable table) {
        IndexingInput inp   = Indexing.filePositionToIndexingInput(index, BOARD_SIZE, p1, p2, table);
        int[]         board = inp.getMapFromIndexingInput(BOARD_SIZE);

        System.out.printf("  P1(1): %s%n", inp.getP1Pieces());
        System.out.printf("  P2(2): %s%n", inp.getP2Pieces());
        System.out.println("  Board:");
        for (int r = 0; r < SIDE; r++) {
            System.out.print("    ");
            for (int c = 0; c < SIDE; c++) {
                int v = board[r * SIDE + c];
                System.out.print(v == 0 ? ". " : v + " ");
            }
            System.out.println();
        }
    }
}
