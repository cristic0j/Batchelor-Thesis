package bitflip;

import EndGameDatabase.dataClasses.AtomicPackedBitArray;
import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.logicModules.Indexing;
import EndGameDatabase.logicModules.RandomAccessFile.FileReaderWriter;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;

/**
 * Loads an endgame database into an AtomicPackedBitArray (2 bits/position)
 * and checks every index against its horizontal-flip and vertical-flip
 * symmetric equivalents in parallel.
 *
 * All three variants of the same board must carry the same outcome.
 * Any disagreement is printed immediately as a bit-flip candidate.
 */
public class BitFlipChecker {

    static final int BOARD_SIZE = 36;
    static final int SIDE       = 6;

    public static void main(String[] args) throws IOException {
        Scanner sc = new Scanner(System.in);

        System.out.println("=== Bit-Flip Checker ===");
        System.out.print("P1 pieces: ");
        System.out.flush();
        int p1 = Integer.parseInt(sc.nextLine().trim());

        System.out.print("P2 pieces: ");
        System.out.flush();
        int p2 = Integer.parseInt(sc.nextLine().trim());

        String path = "EndGameDatabase/data/moves" + p1 + "v" + p2 + ".txt";
        if (!new File(path).exists()) {
            System.err.println("File not found: " + path);
            System.exit(1);
        }

        BinomialTable table          = new BinomialTable(BOARD_SIZE);
        long          totalPositions = Indexing.computeTheNumberOfPossiblePositions(p1, p2, BOARD_SIZE, table);

        System.out.println("Loading " + path + " into RAM  (" + totalPositions + " positions, " + ((totalPositions + 3) / 4) + " bytes packed)...");
        System.out.flush();

        AtomicPackedBitArray data = FileReaderWriter.readFromFilePacked(path, totalPositions);

        System.out.println("Loaded. Running parallel symmetry check...");
        System.out.flush();

        final int fp1 = p1, fp2 = p2;

        ThreadLocal<int[]>     tlP1     = ThreadLocal.withInitial(() -> new int[Math.max(fp1, 1)]);
        ThreadLocal<int[]>     tlP2     = ThreadLocal.withInitial(() -> new int[Math.max(fp2, 1)]);
        ThreadLocal<int[]>     tlComb   = ThreadLocal.withInitial(() -> new int[fp1 + fp2]);
        ThreadLocal<boolean[]> tlIsP1   = ThreadLocal.withInitial(() -> new boolean[fp1 + fp2]);
        ThreadLocal<int[]>     tlBoard  = ThreadLocal.withInitial(() -> new int[BOARD_SIZE]);
        ThreadLocal<int[]>     tlBoardH = ThreadLocal.withInitial(() -> new int[BOARD_SIZE]);
        ThreadLocal<int[]>     tlBoardV = ThreadLocal.withInitial(() -> new int[BOARD_SIZE]);

        ConcurrentLinkedQueue<long[]> mismatches = new ConcurrentLinkedQueue<>();
        AtomicLong counter = new AtomicLong(0);
        long reportEvery = Math.max(1_000_000L, totalPositions / 20);

        LongStream.range(0, totalPositions).parallel().forEach(idx -> {
            int val = data.get(idx);

            int[]     p1Out  = tlP1.get();
            int[]     p2Out  = tlP2.get();
            int[]     comb   = tlComb.get();
            boolean[] isP1   = tlIsP1.get();
            int[]     board  = tlBoard.get();
            int[]     boardH = tlBoardH.get();
            int[]     boardV = tlBoardV.get();

            Indexing.filePositionToPrimitiveArrays(
                idx, BOARD_SIZE, fp1, fp2, table, p1Out, p2Out, comb, isP1
            );

            Arrays.fill(board,  0);
            Arrays.fill(boardH, 0);
            Arrays.fill(boardV, 0);
            for (int i = 0; i < fp1; i++) board[p1Out[i]] = 1;
            for (int i = 0; i < fp2; i++) board[p2Out[i]] = 2;

            // flipH: (r, c) -> (r, 5-c)
            // flipV: (r, c) -> (5-r, c)
            for (int sq = 0; sq < BOARD_SIZE; sq++) {
                int v = board[sq];
                if (v == 0) continue;
                int r = sq / SIDE, c = sq % SIDE;
                boardH[r * SIDE + (SIDE - 1 - c)] = v;
                boardV[(SIDE - 1 - r) * SIDE + c] = v;
            }

            long flipHIdx = Indexing.indexInputToFilePosition(
                Indexing.createIndexingInput(boardH, false), table);
            long flipVIdx = Indexing.indexInputToFilePosition(
                Indexing.createIndexingInput(boardV, false), table);

            int flipHVal = data.get(flipHIdx);
            int flipVVal = data.get(flipVIdx);

            if (val != flipHVal || val != flipVVal) {
                long[] entry = new long[]{ idx, flipHIdx, flipVIdx, val, flipHVal, flipVVal };
                mismatches.add(entry);
                synchronized (BitFlipChecker.class) {
                    System.out.println("[MISMATCH] idx=" + idx
                        + "  flipH=" + flipHIdx + "  flipV=" + flipVIdx
                        + "  val=" + outcomeName((int)entry[3])
                        + "  flipHVal=" + outcomeName((int)entry[4])
                        + "  flipVVal=" + outcomeName((int)entry[5]));
                    System.out.flush();
                }
            }

            long done = counter.incrementAndGet();
            if (done % reportEvery == 0) {
                synchronized (BitFlipChecker.class) {
                    System.out.println("Progress: " + done + " / " + totalPositions
                        + "  (" + mismatches.size() + " mismatch(es) so far)");
                    System.out.flush();
                }
            }
        });

        System.out.println();
        System.out.println("=== Done ===");
        System.out.println("Total mismatches: " + mismatches.size());
        System.out.flush();
    }

    static String outcomeName(int v) {
        switch (v) {
            case 0:  return "LOSS";
            case 1:  return "WIN";
            case 2:  return "DRAW";
            default: return "?(" + v + ")";
        }
    }
}
