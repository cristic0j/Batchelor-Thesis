package EndGameDatabase.ramPrevDiskCurrent;

import EndGameDatabase.dataClasses.AtomicHugeByteArray;
import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.diskBacked.SingleRAExecutor;
import EndGameDatabase.diskBacked.SingleRAParallelExecutor;
import EndGameDatabase.logicModules.Indexing;
import EndGameDatabase.logicModules.RandomAccessFile.FileReaderWriter;
import EndGameDatabase.logicModules.RetrogradeAnalysisExecutor.RootRAExecutor;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Hybrid variant: previous (dependency) tables fully loaded into RAM as AtomicHugeByteArray,
 * current table processed in disk-backed chunks via the diskBacked executors.
 *
 * Best of both worlds: fast RAM lookups for previous tables, low peak RAM for the current table.
 *
 * Usage:
 *   java -Xmx4G   -cp bin EndGameDatabase.ramPrevDiskCurrent.DatabaseBuilderRPDC <p1> <p2>
 *   java -Xmx4G   -cp bin EndGameDatabase.ramPrevDiskCurrent.DatabaseBuilderRPDC <p1> <p2> [chunkSize]
 *   java -Xmx4G   -cp bin EndGameDatabase.ramPrevDiskCurrent.DatabaseBuilderRPDC   (interactive)
 */
public class DatabaseBuilderRPDC {

    private static final int BOARD_SIZE = 36;
    private static final String DATA_PATH = "EndGameDatabase/data/";
    public static final long DEFAULT_CHUNK_SIZE = 1L << 31;

    private final BinomialTable table;
    private final long chunkSize;

    public DatabaseBuilderRPDC(long chunkSize) {
        this.table = new BinomialTable(BOARD_SIZE);
        this.chunkSize = chunkSize;
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

    private AtomicHugeByteArray readFileIntoRam(int p1, int p2) throws IOException {
        return FileReaderWriter.readFromFile(filePath(p1, p2), positionCount(p1, p2));
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
            System.err.println("[Building] " + a + "v" + b + " — symmetric single (RAM prev, disk current, chunkSize=" + chunkSize + ")");
            AtomicHugeByteArray prev = readFileIntoRam(a - 1, b);
            SingleRAExecutor exec = new SingleRAExecutor(BOARD_SIZE, table, a, b, prev, tempPath(a, b), chunkSize);
            exec.start().join();
            new File(tempPath(a, b)).renameTo(new File(filePath(a, b)));

        } else {
            System.err.println("[Building] " + a + "v" + b + " + " + b + "v" + a + " — parallel pair (RAM prev, disk current, chunkSize=" + chunkSize + ")");
            AtomicHugeByteArray rightPrev = readFileIntoRam(b - 1, a);
            AtomicHugeByteArray leftPrev  = (a == 1) ? readFileIntoRam(1, 1) : readFileIntoRam(a - 1, b);
            SingleRAParallelExecutor exec = new SingleRAParallelExecutor(
                BOARD_SIZE, table,
                a, b,
                b, a,
                leftPrev, rightPrev,
                tempPath(a, b),
                tempPath(b, a),
                chunkSize
            );
            exec.start().join();
            new File(tempPath(a, b)).renameTo(new File(filePath(a, b)));
            new File(tempPath(b, a)).renameTo(new File(filePath(b, a)));
        }

        System.err.println("[Done]     " + key);
    }

    public void build(int p1, int p2) throws IOException {
        if (p1 <= 0 || p2 <= 0) {
            System.err.println("Error: piece counts must be >= 1.");
            return;
        }

        System.err.println("=== Database Builder (RAM prev + disk current)  target: " + p1 + "v" + p2 + "  chunkSize: " + chunkSize + " ===");

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
        int p1 = -1, p2 = -1;

        if (args.length >= 2) {
            try {
                p1 = Integer.parseInt(args[0]);
                p2 = Integer.parseInt(args[1]);
                if (args.length >= 3) chunkSize = Long.parseLong(args[2]);
            } catch (NumberFormatException e) {
                System.err.println("Usage: DatabaseBuilderRPDC <p1> <p2> [chunkSize]");
                return;
            }
            try {
                new DatabaseBuilderRPDC(chunkSize).build(p1, p2);
            } catch (IOException e) {
                System.err.println("IO error: " + e.getMessage());
                e.printStackTrace();
            }
            return;
        }

        Scanner scanner = new Scanner(System.in);
        System.out.println("=== Surakarta Endgame Database Builder (RAM prev + disk current) ===");
        System.out.println("Previous tables loaded into RAM, current table processed in disk chunks.");
        System.out.println("Enter: '<p1> <p2> [chunkSize]'  (e.g.  3 3  or  4 4 1073741824):");
        System.out.print("> ");

        try {
            String line = scanner.nextLine().trim();
            String[] tokens = line.split("\\s+");
            if (tokens.length < 2) { System.err.println("Error: expected at least two integers."); return; }
            p1 = Integer.parseInt(tokens[0]);
            p2 = Integer.parseInt(tokens[1]);
            if (tokens.length >= 3) chunkSize = Long.parseLong(tokens[2]);

            long start = System.currentTimeMillis();
            new DatabaseBuilderRPDC(chunkSize).build(p1, p2);
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
