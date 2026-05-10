package EndGameDatabase;

import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.logicModules.Indexing;
import EndGameDatabase.logicModules.RandomAccessFile.FileReaderWriter;
import EndGameDatabase.logicModules.RetrogradeAnalysisExecutor.RootRAExecutor;
import EndGameDatabase.logicModules.RetrogradeAnalysisExecutor.SingleRAExecutor;
import EndGameDatabase.logicModules.RetrogradeAnalysisExecutor.SingleRAParallelExecutor;
import java.io.File;
import java.io.IOException;
import java.util.*;
import EndGameDatabase.dataClasses.AtomicHugeByteArray;

/**
 * Builds endgame database tables on demand.
 *
 * Node types:
 *   ROOT     (1,1)  — RootRAExecutor, no dependencies
 *   SINGLE   (n,n)  — SingleRAExecutor, depends on moves_{n-1}v{n}
 *   PARALLEL (a,b)  — SingleRAParallelExecutor (a<b), builds moves_avb AND moves_bva
 *                     rightPrev = moves_{b-1}v{a}
 *                     leftPrev  = moves_{a-1}v{b}  (or moves_1v1 when a==1)
 *
 * Usage:
 *   java EndGameDatabase.DatabaseBuilder <p1> <p2>
 *   java EndGameDatabase.DatabaseBuilder          (interactive)
 */
public class DatabaseBuilder {

    private static final int BOARD_SIZE = 36;
    private static final String DATA_PATH = "EndGameDatabase/data/";

    private final BinomialTable table;

    public DatabaseBuilder() {
        this.table = new BinomialTable(BOARD_SIZE);
    }

    // ── Key helpers ──────────────────────────────────────────────────────────

    /** Canonical node key: smaller piece count first. */
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

    private static boolean fileExists(int p1, int p2) {
        return new File(filePath(p1, p2)).exists();
    }

    /**
     * A node (a,b) with a<=b is complete when:
     *   a==b  → moves_avb.txt exists
     *   a<b   → both moves_avb.txt AND moves_bva.txt exist
     *           (parallel executor always produces both)
     */
    private static boolean nodeComplete(int a, int b) {
        if (a == b) return fileExists(a, b);
        return fileExists(a, b) && fileExists(b, a);
    }

    // ── Dependency graph ─────────────────────────────────────────────────────

    /**
     * Returns the canonical node keys that must be built before this node.
     * a<=b is assumed (caller should always pass a canonical key).
     */
    private List<String> getDependencies(int a, int b) {
        List<String> deps = new ArrayList<>();
        if (a == 1 && b == 1) {
            // ROOT — no dependencies
        } else if (a == b) {
            // SINGLE(n): captures go from (n,n) → (n,n-1), flipped = (n-1,n)
            deps.add(nodeKey(a - 1, b));
        } else {
            // PARALLEL(a,b):
            //   leftBranch(a,b)  uses rightPrev = moves_{b-1}v{a}
            //   rightBranch(b,a) uses leftPrev  = moves_{a-1}v{b}  (or 1v1 when a==1)
            deps.add(nodeKey(b - 1, a));
            deps.add(a == 1 ? nodeKey(1, 1) : nodeKey(a - 1, b));
            // deduplicate (e.g. pair(1,2) both point to (1,1))
            deps = new ArrayList<>(new LinkedHashSet<>(deps));
        }
        return deps;
    }

    /** Collect all transitive dependencies and return them in build order. */
    private List<String> topologicalOrder(int p1, int p2) {
        String target = nodeKey(p1, p2);

        // BFS to collect all nodes
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

        // Kahn's algorithm
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> dependents = new HashMap<>();
        for (String node : depMap.keySet()) {
            inDegree.put(node, depMap.get(node).size());
            for (String dep : depMap.get(node)) {
                dependents.computeIfAbsent(dep, k -> new ArrayList<>()).add(node);
            }
        }

        // PriorityQueue for deterministic ordering within the same level
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

    private AtomicHugeByteArray readFile(int p1, int p2) throws IOException {
        return FileReaderWriter.readFromFile(filePath(p1, p2), positionCount(p1, p2));
    }

    private void writeFile(int p1, int p2, AtomicHugeByteArray data) throws IOException {
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
            writeFile(1, 1, exec.getBoardResults());

        } else if (a == b) {
            System.err.println("[Building] " + a + "v" + b + " — symmetric single");
            AtomicHugeByteArray prev = readFile(a - 1, b);
            SingleRAExecutor exec = new SingleRAExecutor(BOARD_SIZE, table, a, b, prev);
            exec.start().join();
            writeFile(a, b, exec.getBranchResults());

        } else {
            // a < b: parallel pair produces both avb and bva
            System.err.println("[Building] " + a + "v" + b + " + " + b + "v" + a + " — parallel pair");
            AtomicHugeByteArray rightPrev = readFile(b - 1, a);
            AtomicHugeByteArray leftPrev  = (a == 1) ? readFile(1, 1) : readFile(a - 1, b);
            SingleRAParallelExecutor exec = new SingleRAParallelExecutor(
                BOARD_SIZE, table,
                a, b,
                b, a,
                leftPrev, rightPrev
            );
            exec.start().join();
            writeFile(a, b, exec.getLeftBranchResults());
            writeFile(b, a, exec.getRightBranchResults());
        }

        System.err.println("[Done]     " + key);
    }

    public void build(int p1, int p2) throws IOException {
        if (p1 <= 0 || p2 <= 0) {
            System.err.println("Error: piece counts must be >= 1.");
            return;
        }

        System.err.println("=== Database Builder  target: " + p1 + "v" + p2 + " ===");

        List<String> order = topologicalOrder(p1, p2);

        // Print plan
        System.err.println("Build plan:");
        int toBuild = 0;
        for (String node : order) {
            int[] parts = parseKey(node);
            boolean done = nodeComplete(parts[0], parts[1]);
            System.err.println("  " + node + (done ? "  [exists — skip]" : "  [will build]"));
            if (!done) toBuild++;
        }
        if (toBuild == 0) {
            System.err.println("Nothing to build — all files already exist.");
            return;
        }
        System.err.println();

        // Execute
        int built = 0, skipped = 0;
        for (String node : order) {
            int[] parts = parseKey(node);
            if (nodeComplete(parts[0], parts[1])) {
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
        DatabaseBuilder builder = new DatabaseBuilder();

        if (args.length >= 2) {
            try {
                builder.build(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
            } catch (NumberFormatException e) {
                System.err.println("Usage: DatabaseBuilder <p1> <p2>   (integers >= 1)");
            } catch (IOException e) {
                System.err.println("IO error: " + e.getMessage());
                e.printStackTrace();
            }
            return;
        }

        // Interactive mode
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== Surakarta Endgame Database Builder ===");
        System.out.println("Builds all required dependency tables automatically.");
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
