package EndGameDatabaseRankSelect;

import EndGameDatabase.dataClasses.AtomicPackedBitArray;
import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.logicModules.Indexing;
import EndGameDatabase.logicModules.RandomAccessFile.FileReaderWriter;
import EndGameDatabaseRankSelect.dataClasses.RankSelectTable;
import EndGameDatabaseRankSelect.logicModules.RankSelectBuilder;
import EndGameDatabaseRankSelect.logicModules.RankSelectFileReaderWriter;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

/**
 * Converts existing EndGameDatabase flat tables (EndGameDatabase/data/movesNvM.txt)
 * into compact Rank-Select tables (EndGameDatabaseRankSelect/data/movesNvM.rsk).
 *
 * This module does NOT re-run retrograde analysis — it post-processes the already-built
 * flat outcomes from EndGameDatabase.  Run EndGameDatabase.DatabaseBuilder first.
 *
 * Usage:
 *   java -Xmx4G -cp bin EndGameDatabaseRankSelect.DatabaseBuilderRankSelect <p1> <p2>
 *   java -Xmx4G -cp bin EndGameDatabaseRankSelect.DatabaseBuilderRankSelect all
 *   java -Xmx4G -cp bin EndGameDatabaseRankSelect.DatabaseBuilderRankSelect   (interactive)
 */
public class DatabaseBuilderRankSelect {

    private static final int    BOARD_SIZE  = 36;
    private static final String FLAT_DIR    = "EndGameDatabase/data/";
    private static final String OUTPUT_DIR  = "EndGameDatabaseRankSelect/data/";

    private final BinomialTable table;

    public DatabaseBuilderRankSelect() {
        this.table = new BinomialTable(BOARD_SIZE);
    }

    // ── Path helpers ─────────────────────────────────────────────────────────

    private static String flatPath(int p1, int p2) {
        return FLAT_DIR + "moves" + p1 + "v" + p2 + ".txt";
    }

    private static String rskPath(int p1, int p2) {
        return OUTPUT_DIR + "moves" + p1 + "v" + p2 + ".rsk";
    }

    // ── Single-table build ───────────────────────────────────────────────────

    public void build(int p1, int p2) throws IOException {
        if (p1 <= 0 || p2 <= 0) {
            System.err.println("Error: piece counts must be >= 1.");
            return;
        }

        String rsk  = rskPath(p1, p2);
        String flat = flatPath(p1, p2);

        if (new File(rsk).exists()) {
            System.err.println("[Skip]  " + p1 + "v" + p2 + " — .rsk already exists.");
            return;
        }
        if (!new File(flat).exists()) {
            System.err.println("[Error] " + p1 + "v" + p2 + " — flat source missing: " + flat);
            System.err.println("        Build EndGameDatabase first.");
            return;
        }

        long size = Indexing.computeTheNumberOfPossiblePositions(p1, p2, BOARD_SIZE, table);
        System.err.println("[Load]  " + p1 + "v" + p2 + "  (" + String.format("%,d", size) + " positions)");

        AtomicPackedBitArray flatData = FileReaderWriter.readFromFilePacked(flat, size);
        RankSelectTable rskTable = RankSelectBuilder.build(flatData, size, p1, p2, table);
        RankSelectFileReaderWriter.write(rsk, rskTable);

        long flatBytes = new File(flat).length();
        long rskBytes  = new File(rsk).length();
        System.err.printf("[Done]  %dv%d  flat: %,d bytes → rsk: %,d bytes  (%.1f%% of flat)%n",
            p1, p2, flatBytes, rskBytes, 100.0 * rskBytes / flatBytes);
    }

    /** Converts every flat table that exists in FLAT_DIR. */
    public void buildAll() throws IOException {
        System.err.println("=== Rank-Select Builder — converting all EndGameDatabase tables ===");
        File dir = new File(FLAT_DIR);
        File[] files = dir.listFiles((d, name) -> name.matches("moves\\d+v\\d+\\.txt"));
        if (files == null || files.length == 0) {
            System.err.println("No flat tables found in " + FLAT_DIR);
            return;
        }
        // Sort by total pieces (small first)
        java.util.Arrays.sort(files, (a, b) -> {
            int[] pa = parseFilename(a.getName()), pb = parseFilename(b.getName());
            return Integer.compare(pa[0] + pa[1], pb[0] + pb[1]);
        });
        for (File f : files) {
            int[] ps = parseFilename(f.getName());
            build(ps[0], ps[1]);
        }
        System.err.println("=== All done ===");
    }

    private static int[] parseFilename(String name) {
        // "movesNvM.txt" → {N, M}
        String[] parts = name.replace("moves", "").replace(".txt", "").split("v");
        return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
    }

    // ── Entry point ──────────────────────────────────────────────────────────

    public static void main(String[] args) {
        DatabaseBuilderRankSelect builder = new DatabaseBuilderRankSelect();

        if (args.length >= 2) {
            try {
                long start = System.currentTimeMillis();
                builder.build(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
                System.err.println("Time: " + (System.currentTimeMillis() - start) / 1000 + "s");
            } catch (NumberFormatException e) {
                System.err.println("Usage: DatabaseBuilderRankSelect <p1> <p2>  |  all");
            } catch (IOException e) {
                System.err.println("IO error: " + e.getMessage()); e.printStackTrace();
            }
            return;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("all")) {
            try {
                long start = System.currentTimeMillis();
                builder.buildAll();
                System.err.println("Total time: " + (System.currentTimeMillis() - start) / 1000 + "s");
            } catch (IOException e) {
                System.err.println("IO error: " + e.getMessage()); e.printStackTrace();
            }
            return;
        }

        // Interactive
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== Surakarta Rank-Select Database Builder ===");
        System.out.println("Reads from EndGameDatabase/data/  →  writes to EndGameDatabaseRankSelect/data/");
        System.out.println("Enter '<p1> <p2>' to build one table, or 'all' to convert every flat table:");
        System.out.print("> ");
        try {
            String line = scanner.nextLine().trim();
            if (line.equalsIgnoreCase("all")) {
                long start = System.currentTimeMillis();
                builder.buildAll();
                System.err.println("Total time: " + (System.currentTimeMillis() - start) / 1000 + "s");
            } else {
                String[] toks = line.split("\\s+");
                if (toks.length < 2) { System.err.println("Error: expected two integers."); return; }
                long start = System.currentTimeMillis();
                builder.build(Integer.parseInt(toks[0]), Integer.parseInt(toks[1]));
                System.err.println("Time: " + (System.currentTimeMillis() - start) / 1000 + "s");
            }
        } catch (NumberFormatException e) {
            System.err.println("Error: enter two integers >= 1.");
        } catch (IOException e) {
            System.err.println("IO error: " + e.getMessage()); e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
}
