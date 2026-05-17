package EndGameDatabase.inMemory;

import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.dataClasses.AtomicHugeByteArray;
import EndGameDatabase.dataClasses.AtomicPackedBitArray;
import EndGameDatabase.logicModules.Indexing;
import EndGameDatabase.logicModules.RandomAccessFile.FileReaderWriter;
import EndGameDatabase.logicModules.RetrogradeAnalysisExecutor.RootRAExecutor;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * In-memory variant of the database builder.
 *
 * Previous (dependency) tables are fully loaded into RAM as AtomicHugeByteArray
 * before analysis begins. Faster per-lookup than file-backed, but uses more peak RAM.
 *
 * Output is written to EndGameDatabase/data/.
 *
 * Usage:
 *   java -Xmx4G -cp bin EndGameDatabase.inMemory.DatabaseBuilderIM <p1> <p2>
 *   java -Xmx4G -cp bin EndGameDatabase.inMemory.DatabaseBuilderIM   (interactive)
 */
public class DatabaseBuilderIM {

    private static final int BOARD_SIZE = 36;
    private static final String DATA_PATH = "EndGameDatabase/data/";

    private final BinomialTable table;

    public DatabaseBuilderIM() {
        this.table = new BinomialTable(BOARD_SIZE);
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
            // ROOT — no dependencies
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

    private AtomicPackedBitArray readFile(int p1, int p2) throws IOException {
        return FileReaderWriter.readFromFilePacked(filePath(p1, p2), positionCount(p1, p2));
    }

    private void writeFile(int p1, int p2, AtomicPackedBitArray data) throws IOException {
        FileReaderWriter.writeToFile(filePath(p1, p2), data, positionCount(p1, p2));
    }

    // ── Build logic ──────────────────────────────────────────────────────────

    private void buildNode(String key) throws IOException {
        int[] parts = parseKey(key);
        int a = parts[0], b = parts[1]; // a <= b

        if (a == 1 && b == 1) {
            System.err.println("[Building] 1v1 — root analysis");
            RootRAExecutor exec = new RootRAExecutor(BOARD_SIZE, table);
            exec.start();
            FileReaderWriter.writeToFile(filePath(1, 1), exec.getBoardResults(), positionCount(1, 1));

        } else if (a == b) {
            System.err.println("[Building] " + a + "v" + b + " — symmetric single (in-memory prev)");
            AtomicPackedBitArray prev = readFile(a - 1, b);
            SingleRAExecutor exec = new SingleRAExecutor(BOARD_SIZE, table, a, b, prev, tempPath(a, b));
            exec.start().join();
            new File(tempPath(a, b)).renameTo(new File(filePath(a, b)));

        } else {
            // a < b: parallel pair produces both avb and bva
            System.err.println("[Building] " + a + "v" + b + " + " + b + "v" + a + " — parallel pair (in-memory prev)");
            AtomicPackedBitArray rightPrev = readFile(b - 1, a);
            AtomicPackedBitArray leftPrev  = (a == 1) ? readFile(1, 1) : readFile(a - 1, b);
            SingleRAParallelExecutor exec = new SingleRAParallelExecutor(
                BOARD_SIZE, table,
                a, b,
                b, a,
                leftPrev, rightPrev,
                tempPath(a, b),
                tempPath(b, a)
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

        System.err.println("=== Database Builder (in-memory)  target: " + p1 + "v" + p2 + " ===");

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
        DatabaseBuilderIM builder = new DatabaseBuilderIM();

        if (args.length >= 2) {
            try {
                builder.build(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
            } catch (NumberFormatException e) {
                System.err.println("Usage: DatabaseBuilderIM <p1> <p2>   (integers >= 1)");
            } catch (IOException e) {
                System.err.println("IO error: " + e.getMessage());
                e.printStackTrace();
            }
            return;
        }

        // Interactive mode
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== Surakarta Endgame Database Builder (in-memory) ===");
        System.out.println("Previous tables are fully loaded into RAM — faster lookups.");
        System.out.println("Enter target as '<p1> <p2>'  (e.g.  3 3  or  2 4):");
        System.out.print("> ");

        try {
            String line = scanner.nextLine().trim();
            String[] tokens = line.split("\\s+");
            if (tokens.length < 2) {
                System.err.println("Error: expected two integers.");
                return;
            }
            long start = System.currentTimeMillis();
            builder.build(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]));
            long elapsed = System.currentTimeMillis() - start;
            System.err.println("Total time: " + elapsed / 1000 + "s");
        } catch (NumberFormatException e) {
            System.err.println("Error: enter two integers >= 1.");
        } catch (IOException e) {
            System.err.println("IO error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
}
