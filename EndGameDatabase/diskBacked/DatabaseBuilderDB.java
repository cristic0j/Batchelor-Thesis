package EndGameDatabase.diskBacked;

import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.logicModules.Indexing;
import EndGameDatabase.logicModules.RandomAccessFile.FileReaderWriter;
import EndGameDatabase.logicModules.RetrogradeAnalysisExecutor.RootRAExecutor;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Disk-backed variant of the database builder.
 *
 * The table being computed is processed in chunks of `chunkSize` positions at a time.
 * Only the current chunk lives in RAM — previous tables are read from disk via OpenReader.
 * Suitable for very large tables that do not fit in RAM.
 *
 * The .tmp file is both the working state and the checkpoint: if interrupted, just re-run
 * and the existing .tmp file will be resumed automatically.
 *
 * Usage:
 *   java -Xmx512M -cp bin EndGameDatabase.diskBacked.DatabaseBuilderDB <p1> <p2> [chunkSize] [symBufCapacity]
 *   java -Xmx512M -cp bin EndGameDatabase.diskBacked.DatabaseBuilderDB   (interactive)
 *
 * Default chunkSize = 2^31. Default symBufCapacity = 4,000,000 entries (~36 MB).
 */
public class DatabaseBuilderDB {

    private static final int BOARD_SIZE = 36;
    private static final String DATA_PATH = "EndGameDatabase/data/";
    public static final long DEFAULT_CHUNK_SIZE = 1L << 31;
    public static final int  DEFAULT_SYM_BUF_CAPACITY = 4_000_000;

    private final BinomialTable table;
    private final long chunkSize;
    private final int symBufCapacity;

    public DatabaseBuilderDB(long chunkSize, int symBufCapacity) {
        this.table = new BinomialTable(BOARD_SIZE);
        this.chunkSize = chunkSize;
        this.symBufCapacity = symBufCapacity;
    }

    // ── Key helpers ──────────────────────────────────────────────────────────

    private static String nodeKey(int p1, int p2) {
        return Math.min(p1, p2) + "v" + Math.max(p1, p2);
    }

    private static int[] parseKey(String key) {
        String[] parts = key.split("v");
        return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
    }

    private static String filePath(int p1, int p2) {
        return DATA_PATH + "moves" + p1 + "v" + p2 + ".txt";
    }

    private static String tempPath(int p1, int p2) {
        return DATA_PATH + "moves" + p1 + "v" + p2 + ".tmp";
    }

    private static boolean fileExists(int p1, int p2) {
        return new File(filePath(p1, p2)).exists();
    }

    private static boolean nodeComplete(int a, int b) {
        if (a == b) return fileExists(a, b);
        return fileExists(a, b) && fileExists(b, a);
    }

    // ── Dependency graph ─────────────────────────────────────────────────────

    private List<String> getDependencies(int a, int b) {
        List<String> deps = new ArrayList<>();
        if (a == 1 && b == 1) {
        } else if (a == b) {
            deps.add(nodeKey(a - 1, b));
        } else {
            deps.add(nodeKey(b - 1, a));
            deps.add(a == 1 ? nodeKey(1, 1) : nodeKey(a - 1, b));
            deps = new ArrayList<>(new LinkedHashSet<>(deps));
        }
        return deps;
    }

    private List<String> topologicalOrder(int p1, int p2) {
        String target = nodeKey(p1, p2);

        Map<String, List<String>> depMap = new LinkedHashMap<>();
        Queue<String> bfsQueue = new LinkedList<>();
        bfsQueue.add(target);
        while (!bfsQueue.isEmpty()) {
            String node = bfsQueue.poll();
            if (depMap.containsKey(node)) continue;
            int[] parts = parseKey(node);
            List<String> deps = getDependencies(parts[0], parts[1]);
            depMap.put(node, deps);
            bfsQueue.addAll(deps);
        }

        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> dependents = new HashMap<>();
        for (String node : depMap.keySet()) {
            inDegree.put(node, depMap.get(node).size());
            for (String dep : depMap.get(node)) {
                dependents.computeIfAbsent(dep, k -> new ArrayList<>()).add(node);
            }
        }

        Queue<String> ready = new PriorityQueue<>();
        for (String node : inDegree.keySet()) {
            if (inDegree.get(node) == 0) ready.add(node);
        }

        List<String> order = new ArrayList<>();
        while (!ready.isEmpty()) {
            String node = ready.poll();
            order.add(node);
            for (String dependent : dependents.getOrDefault(node, Collections.emptyList())) {
                inDegree.merge(dependent, -1, Integer::sum);
                if (inDegree.get(dependent) == 0) ready.add(dependent);
            }
        }
        return order;
    }

    // ── File I/O helpers ─────────────────────────────────────────────────────

    private long positionCount(int p1, int p2) {
        return Indexing.computeTheNumberOfPossiblePositions(p1, p2, BOARD_SIZE, table);
    }

    private FileReaderWriter.OpenReader openReader(int p1, int p2) throws IOException {
        return FileReaderWriter.openReader(filePath(p1, p2), positionCount(p1, p2));
    }

    private void writeFile(int p1, int p2, String tmpPath) {
        new File(tmpPath).renameTo(new File(filePath(p1, p2)));
    }

    // ── Build logic ──────────────────────────────────────────────────────────

    private void buildNode(String key) throws IOException {
        int[] parts = parseKey(key);
        int a = parts[0], b = parts[1];

        if (a == 1 && b == 1) {
            System.err.println("[Building] 1v1 — root analysis");
            RootRAExecutor exec = new RootRAExecutor(BOARD_SIZE, table);
            exec.start();
            FileReaderWriter.writeToFile(filePath(1, 1), exec.getBoardResults(), positionCount(1, 1));

        } else if (a == b) {
            System.err.println("[Building] " + a + "v" + b + " — symmetric single (disk-backed, chunkSize=" + chunkSize + ", symBuf=" + symBufCapacity + ")");
            try (FileReaderWriter.OpenReader prev = openReader(a - 1, b)) {
                SingleRAExecutor exec = new SingleRAExecutor(
                    BOARD_SIZE, table, a, b, prev, tempPath(a, b), chunkSize, symBufCapacity, true
                );
                exec.start().join();
                writeFile(a, b, tempPath(a, b));
            }

        } else {
            System.err.println("[Building] " + a + "v" + b + " + " + b + "v" + a + " — parallel pair (disk-backed, chunkSize=" + chunkSize + ", symBuf=" + symBufCapacity + ")");
            try (FileReaderWriter.OpenReader rightPrev = openReader(b - 1, a);
                 FileReaderWriter.OpenReader leftPrev  = (a == 1) ? openReader(1, 1) : openReader(a - 1, b)) {

                SingleRAParallelExecutor exec = new SingleRAParallelExecutor(
                    BOARD_SIZE, table,
                    a, b,
                    b, a,
                    leftPrev, rightPrev,
                    tempPath(a, b),
                    tempPath(b, a),
                    chunkSize, symBufCapacity, true
                );
                exec.start().join();
                writeFile(a, b, tempPath(a, b));
                writeFile(b, a, tempPath(b, a));
            }
        }

        System.err.println("[Done]     " + key);
    }

    public void build(int p1, int p2) throws IOException {
        if (p1 <= 0 || p2 <= 0) {
            System.err.println("Error: piece counts must be >= 1.");
            return;
        }

        System.err.println("=== Database Builder (disk-backed)  target: " + p1 + "v" + p2 + "  chunkSize: " + chunkSize + "  symBuf: " + symBufCapacity + " ===");

        List<String> order = topologicalOrder(p1, p2);

        System.err.println("Build plan:");
        int toBuild = 0;
        for (String node : order) {
            int[] ps = parseKey(node);
            boolean done = nodeComplete(ps[0], ps[1]);
            System.err.println("  " + node + (done ? "  [exists — skip]" : "  [will build]"));
            if (!done) toBuild++;
        }
        if (toBuild == 0) {
            System.err.println("Nothing to build — all files already exist.");
            return;
        }
        System.err.println();

        int built = 0, skipped = 0;
        for (String node : order) {
            int[] ps = parseKey(node);
            if (nodeComplete(ps[0], ps[1])) {
                System.err.println("[Skipping] " + node);
                skipped++;
            } else {
                buildNode(node);
                built++;
            }
        }

        System.err.println("\n=== Finished: " + built + " built, " + skipped + " skipped ===");
    }

    // ── Entry point ──────────────────────────────────────────────────────────

    public static void main(String[] args) {
        long chunkSize = DEFAULT_CHUNK_SIZE;
        int symBufCapacity = DEFAULT_SYM_BUF_CAPACITY;
        int p1 = -1, p2 = -1;

        if (args.length >= 2) {
            try {
                p1 = Integer.parseInt(args[0]);
                p2 = Integer.parseInt(args[1]);
                if (args.length >= 3) chunkSize      = Long.parseLong(args[2]);
                if (args.length >= 4) symBufCapacity = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                System.err.println("Usage: DatabaseBuilderDB <p1> <p2> [chunkSize] [symBufCapacity]");
                return;
            }
            try {
                new DatabaseBuilderDB(chunkSize, symBufCapacity).build(p1, p2);
            } catch (IOException e) {
                System.err.println("IO error: " + e.getMessage());
                e.printStackTrace();
            }
            return;
        }

        Scanner scanner = new Scanner(System.in);
        System.out.println("=== Surakarta Endgame Database Builder (disk-backed) ===");
        System.out.println("Only the current chunk lives in RAM — suitable for large tables.");
        System.out.println("Chunk size in positions [default " + DEFAULT_CHUNK_SIZE + "]:");
        System.out.print("> ");

        try {
            String chunkLine = scanner.nextLine().trim();
            if (!chunkLine.isEmpty()) chunkSize = Long.parseLong(chunkLine);

            System.out.println("Symmetry buffer capacity in entries [default " + DEFAULT_SYM_BUF_CAPACITY + "]:");
            System.out.print("> ");
            String symLine = scanner.nextLine().trim();
            if (!symLine.isEmpty()) symBufCapacity = Integer.parseInt(symLine);

            System.out.println("Enter target as '<p1> <p2>'  (e.g.  3 3  or  2 4):");
            System.out.print("> ");
            String targetLine = scanner.nextLine().trim();
            String[] tokens = targetLine.split("\\s+");
            if (tokens.length < 2) { System.err.println("Error: expected two integers."); return; }
            p1 = Integer.parseInt(tokens[0]);
            p2 = Integer.parseInt(tokens[1]);

            long start = System.currentTimeMillis();
            new DatabaseBuilderDB(chunkSize, symBufCapacity).build(p1, p2);
            System.err.println("Total time: " + (System.currentTimeMillis() - start) / 1000 + "s");
        } catch (NumberFormatException e) {
            System.err.println("Error: enter integers >= 1.");
        } catch (IOException e) {
            System.err.println("IO error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
}
