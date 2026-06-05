package Statistics;

import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.logicModules.Indexing;
import EndGameDatabase.logicModules.RandomAccessFile.FileReaderWriter;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class TableSampler {

    private static final int BOARD_SIZE = 36;
    private static final int LARGE_SAMPLE_SIZE = 10_000;
    private static final int SMALL_SAMPLE_SIZE = 1_000;
    private static final long SMALL_TABLE_THRESHOLD = 10_000;

    private static final int LOSS    = 0;
    private static final int WIN     = 1;
    private static final int DRAW    = 2;

    private static final String DATA_PATH = "EndGameDatabase/data/";

    public static void main(String[] args) throws IOException {
        BinomialTable table = new BinomialTable(36);
        Random rng = new Random(42);

        printHeader();

        for (int p1 = 1; p1 <= 6; p1++) {
            for (int p2 = 1; p2 <= 6; p2++) {
                if (p1 + p2 > 10) continue;

                String path = DATA_PATH + "moves" + p1 + "v" + p2 + ".txt";
                if (!new File(path).exists()) continue;

                long totalPositions = Indexing.computeTheNumberOfPossiblePositions(p1, p2, BOARD_SIZE, table);
                int sampleSize = pickSampleSize(totalPositions);

                long[] counts = sampleTable(path, totalPositions, sampleSize, rng);
                long wins   = counts[0];
                long losses = counts[1];
                long draws  = counts[2];

                printRow(p1, p2, totalPositions, sampleSize, wins, losses, draws);
            }
        }

        System.out.println("─".repeat(120));
        System.out.println("Outcomes: WIN=1, LOSS=0, DRAW=2(uncertain)");
        System.out.println("95% CI uses Wilson score interval. Est. counts are rounded to nearest integer.");
    }

    // ── sampling ─────────────────────────────────────────────────────────────

    private static int pickSampleSize(long totalPositions) {
        if (totalPositions < SMALL_TABLE_THRESHOLD) {
            return (int) Math.min(SMALL_SAMPLE_SIZE, totalPositions);
        }
        return (int) Math.min(LARGE_SAMPLE_SIZE, totalPositions);
    }

    private static long[] sampleTable(String path, long totalPositions, int sampleSize, Random rng)
            throws IOException {

        long wins = 0, losses = 0, draws = 0;

        if (totalPositions <= sampleSize) {
            // Table is small enough to scan completely — exact counts, no estimation needed.
            for (long i = 0; i < totalPositions; i++) {
                int val = FileReaderWriter.readValueFromFile(path, i);
                if      (val == WIN)  wins++;
                else if (val == LOSS) losses++;
                else                  draws++;
            }
        } else {
            // Random sampling without replacement via a HashSet of sampled indices.
            Set<Long> sampled = new HashSet<>(sampleSize * 2);
            while (sampled.size() < sampleSize) {
                long idx = (long) (rng.nextDouble() * totalPositions);
                sampled.add(idx);
            }
            for (long idx : sampled) {
                int val = FileReaderWriter.readValueFromFile(path, idx);
                if      (val == WIN)  wins++;
                else if (val == LOSS) losses++;
                else                  draws++;
            }
        }

        return new long[]{wins, losses, draws};
    }

    // ── output ────────────────────────────────────────────────────────────────

    private static void printHeader() {
        System.out.println("╔══════════════════════════════════ Surakarta Endgame Tablebase — Sampling Statistics ══════════════════════════════════╗");
        System.out.printf("║ %-8s │ %14s │ %8s │ %9s │ %9s │ %9s │ %15s │ %15s │ %15s ║%n",
            "Table", "Total Pos", "Samples",
            "Win %", "Loss %", "Draw %",
            "Est. Wins", "Est. Losses", "Est. Draws");
        System.out.println("╠" + "═".repeat(118) + "╣");
    }

    private static void printRow(int p1, int p2, long total, int sampleSize,
                                  long wins, long losses, long draws) {
        boolean exact = (total <= sampleSize);
        String label = p1 + "v" + p2 + (exact ? "*" : " ");

        double winPct  = 100.0 * wins   / sampleSize;
        double lossPct = 100.0 * losses / sampleSize;
        double drawPct = 100.0 * draws  / sampleSize;

        long estWins   = Math.round(winPct  / 100.0 * total);
        long estLosses = Math.round(lossPct / 100.0 * total);
        long estDraws  = Math.round(drawPct / 100.0 * total);

        // 95% Wilson score margin of error for win proportion
        double z = 1.96;
        double n = sampleSize;
        double pW = wins / n;
        double moePct = z * Math.sqrt(pW * (1 - pW) / n) * 100.0;
        String moeStr = exact ? "exact" : String.format("±%.1f%%", moePct);

        System.out.printf("║ %-8s │ %14s │ %8s │ %8.2f%% │ %8.2f%% │ %8.2f%% │ %15s │ %15s │ %15s ║%n",
            label,
            String.format("%,d", total),
            exact ? "all" : String.format("%,d", sampleSize),
            winPct, lossPct, drawPct,
            exact ? String.format("%,d", estWins)   : String.format("%,d (%s)", estWins, moeStr),
            exact ? String.format("%,d", estLosses) : String.format("%,d", estLosses),
            exact ? String.format("%,d", estDraws)  : String.format("%,d", estDraws));
    }
}
