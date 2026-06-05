package EndGameDatabaseRankSelect;

import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.logicModules.Indexing;
import EndGameDatabaseRankSelect.dataClasses.RankSelectTable;
import EndGameDatabaseRankSelect.logicModules.RankSelectBuilder;
import EndGameDatabaseRankSelect.logicModules.RankSelectFileReaderWriter;

import java.io.IOException;

/**
 * Example: look up a board position using the Rank-Select index.
 *
 * Demonstrates the full lookup pipeline:
 *   board → 8 D4 transform indices → canonical (min) index
 *   → rank(canonical) → compact outcomes[rank] → WIN / LOSS / DRAW
 */
public class main {

    public static void main(String[] args) throws IOException {

        BinomialTable table = new BinomialTable(36);

        // Example board: 3v3 position
        int[] board = {
            1, 0, 0, 0, 0, 0,
            0, 2, 0, 0, 0, 0,
            0, 0, 1, 0, 0, 0,
            0, 0, 0, 1, 0, 0,
            0, 0, 0, 0, 2, 0,
            2, 0, 0, 0, 0, 0,
        };
        int p1 = 3, p2 = 3;

        // ── Step 1: Compute canonical flat index ──────────────────────────────
        long[] allIdxs = Indexing.getAllTransformIndexes(board, 6, false, table);
        long canonicalIdx = allIdxs[0];
        for (long idx : allIdxs) if (idx < canonicalIdx) canonicalIdx = idx;

        System.out.println("Flat index (no symmetry): "
            + Indexing.indexInputToFilePosition(Indexing.createIndexingInput(board, false), table));
        System.out.println("Canonical flat index:      " + canonicalIdx);

        // ── Step 2: Load (or verify) the rank-select table ────────────────────
        String rskPath = "EndGameDatabaseRankSelect/data/moves" + p1 + "v" + p2 + ".rsk";
        RankSelectTable rskTable;
        try {
            rskTable = RankSelectFileReaderWriter.read(rskPath);
            System.out.println("Loaded from: " + rskPath);
        } catch (IOException e) {
            System.err.println("RSK file not found: " + rskPath);
            System.err.println("Run DatabaseBuilderRankSelect " + p1 + " " + p2 + " first.");
            return;
        }

        // ── Step 3: Rank-select lookup ────────────────────────────────────────
        System.out.println("Is canonical: " + rskTable.isCanonical(canonicalIdx));
        long rank = rskTable.rank(canonicalIdx);
        System.out.println("Compact index (rank): " + rank);

        int outcome = rskTable.lookup(canonicalIdx);
        String label = switch (outcome) {
            case 0  -> "LOSS";
            case 1  -> "WIN";
            case 2  -> "DRAW";
            default -> "UNKNOWN";
        };
        System.out.println("Outcome for P1: " + label + "  (raw=" + outcome + ")");

        // ── Step 4: Sanity — verify all 8 transforms give the same outcome ────
        System.out.println("\nVerifying all 8 D4 transforms give identical outcome:");
        boolean allSame = true;
        for (int t = 0; t < allIdxs.length; t++) {
            long idx = allIdxs[t];
            long canon = rskTable.isCanonical(idx) ? idx : findCanonical(allIdxs);
            int o = rskTable.lookup(canon);
            String lab = switch (o) { case 0 -> "LOSS"; case 1 -> "WIN"; case 2 -> "DRAW"; default -> "?"; };
            System.out.printf("  transform[%d]  flat=%-12d  canonical=%-12d  outcome=%s%n", t, idx, canon, lab);
            if (o != outcome) allSame = false;
        }
        System.out.println(allSame ? "✓  All transforms agree." : "✗  Mismatch detected!");

        // ── Step 5: Index statistics ──────────────────────────────────────────
        System.out.println();
        long total      = rskTable.getTotalFlatPositions();
        long canonical  = rskTable.getNumCanonical();
        System.out.printf("Table statistics:%n");
        System.out.printf("  Total flat positions : %,d%n", total);
        System.out.printf("  Canonical positions  : %,d  (%.2f%%)%n",
            canonical, 100.0 * canonical / total);
        System.out.printf("  Compression ratio    : %.2fx  (flat: %,d bytes vs compact: %,d bytes)%n",
            (double) total / canonical,
            (total + 3) / 4,
            RankSelectFileReaderWriter.expectedFileSize(total, canonical));
    }

    private static long findCanonical(long[] idxs) {
        long min = idxs[0];
        for (long i : idxs) if (i < min) min = i;
        return min;
    }
}
