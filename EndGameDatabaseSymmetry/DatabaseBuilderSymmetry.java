package EndGameDatabaseSymmetry;

import EndGameDatabase.dataClasses.AtomicHugeByteArray;
import EndGameDatabase.dataClasses.BinomialTable;
import EndGameDatabase.logicModules.RandomAccessFile.FileReaderWriter;
import EndGameDatabaseSymmetry.logicModules.RetrogradeAnalysisExecutor.SymRootRAExecutor;
import EndGameDatabaseSymmetry.logicModules.RetrogradeAnalysisExecutor.SymSingleRAExecutor;
import EndGameDatabaseSymmetry.logicModules.RetrogradeAnalysisExecutor.SymSingleRAParallelExecutor;
import EndGameDatabaseSymmetry.logicModules.SymmetricIndexing;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Builds symmetric endgame database tables on demand.
 *
 * Uses SymmetricIndexing (D4 symmetry reduction) for all position counts and
 * normalisation. Data files are stored under EndGameDatabaseSymmetry/data/.
 *
 * Node types:
 *   ROOT     (1,1)  — SymRootRAExecutor, no dependencies
 *   SINGLE   (n,n)  — SymSingleRAExecutor, depends on moves_{n-1}v{n}
 *   PARALLEL (a,b)  — SymSingleRAParallelExecutor (a<b), builds moves_avb AND moves_bva
 *
 * Usage:
 *   java EndGameDatabaseSymmetry.DatabaseBuilderSymmetry <p1> <p2>
 *   java EndGameDatabaseSymmetry.DatabaseBuilderSymmetry          (interactive)
 */
public class DatabaseBuilderSymmetry {

    private static final int    BOARD_SIZE = 36;
    private static final String DATA_PATH  = "EndGameDatabaseSymmetry/data/";

    private final BinomialTable table;

    public DatabaseBuilderSymmetry() {
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

        Map<String, Integer>      inDegree   = new HashMap<>();
        Map<String, List<String>> dependents = new HashMap<>();
        for (String node : depMap.keySet()) {
            inDegree.put(node, depMap.get(node).size());
            for (String dep : depMap.get(node))
                dependents.computeIfAbsent(dep, k -> new ArrayList<>()).add(node);
        }

        Queue<String> ready = new PriorityQueue<>();
        for (String node : inDegree.keySet())
            if (inDegree.get(node) == 0) ready.add(node);

        List<String> order = new ArrayList<>();
        while (!ready.isEmpty()) {
            String node = ready.poll();
            order.add(node);
            for (String dep : dependents.getOrDefault(node, Collections.emptyList())) {
                inDegree.merge(dep, -1, Integer::sum);
                if (inDegree.get(dep) == 0) ready.add(dep);
            }
        }
        return order;
    }

    // ── File I/O helpers ─────────────────────────────────────────────────────

    private long positionCount(int p1, int p2) {
        return SymmetricIndexing.computeNumberOfPositions(p1, p2, table);
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
        int a = parts[0], b = parts[1];

        if (a == 1 && b == 1) {
            System.err.println("[Building] Sym 1v1 — root analysis");
            SymRootRAExecutor exec = new SymRootRAExecutor(BOARD_SIZE, table);
            exec.start();
            writeFile(1, 1, exec.getBoardResults());

        } else if (a == b) {
            System.err.println("[Building] Sym " + a + "v" + b + " — symmetric single");
            AtomicHugeByteArray prev = readFile(a - 1, b);
            SymSingleRAExecutor exec = new SymSingleRAExecutor(BOARD_SIZE, table, a, b, prev);
            exec.start().join();
            writeFile(a, b, exec.getBranchResults());

        } else {
            System.err.println("[Building] Sym " + a + "v" + b + " + " + b + "v" + a + " — parallel pair");
            AtomicHugeByteArray rightPrev = readFile(b - 1, a);
            AtomicHugeByteArray leftPrev  = (a == 1) ? readFile(1, 1) : readFile(a - 1, b);
            SymSingleRAParallelExecutor exec = new SymSingleRAParallelExecutor(
                BOARD_SIZE, table,
                a, b,
                b, a,
                leftPrev, rightPrev
            );
            exec.start().join();
            writeFile(a, b, exec.getLeftBranchResults());
            writeFile(b, a, exec.getRightBranchResults());
        }

        System.err.println("[Done]     Sym " + key);
    }

    public void build(int p1, int p2) throws IOException {
        if (p1 <= 0 || p2 <= 0) {
            System.err.println("Error: piece counts must be >= 1.");
            return;
        }
        System.err.println("=== Symmetric Database Builder  target: " + p1 + "v" + p2 + " ===");

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
        DatabaseBuilderSymmetry builder = new DatabaseBuilderSymmetry();

        if (args.length >= 2) {
            try {
                builder.build(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
            } catch (NumberFormatException e) {
                System.err.println("Usage: DatabaseBuilderSymmetry <p1> <p2>   (integers >= 1)");
            } catch (IOException e) {
                System.err.println("IO error: " + e.getMessage());
                e.printStackTrace();
            }
            return;
        }

        Scanner scanner = new Scanner(System.in);
        System.out.println("=== Surakarta Symmetric Endgame Database Builder ===");
        System.out.println("Enter target as '<p1> <p2>'  (e.g.  1 1  or  3 3):");
        System.out.print("> ");

        try {
            String line   = scanner.nextLine().trim();
            String[] toks = line.split("\\s+");
            if (toks.length < 2) { System.err.println("Error: expected two integers."); return; }
            builder.build(Integer.parseInt(toks[0]), Integer.parseInt(toks[1]));
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
