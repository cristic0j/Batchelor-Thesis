package EndGameDatabase;

import EndGameDatabase.inMemory.DatabaseBuilderIM;
import EndGameDatabase.prevOnDisk.DatabaseBuilderPOD;
import EndGameDatabase.diskBacked.DatabaseBuilderDB;
import java.io.IOException;
import java.util.*;

/**
 * Main entry point for the Surakarta endgame database builder.
 *
 * Delegates to one of three implementations:
 *   --im          In-memory mode    (default): previous tables fully loaded into RAM
 *   --pod         Prev-on-disk mode:           previous tables read directly from disk
 *   --db[=N]      Disk-backed mode:            current table processed in chunks of N positions
 *                                              (default N = 2^31)
 *
 * Usage:
 *   java -Xmx4G   -cp bin EndGameDatabase.DatabaseBuilder <p1> <p2> [--im|--pod]
 *   java -Xmx512M -cp bin EndGameDatabase.DatabaseBuilder <p1> <p2> --db
 *   java -Xmx512M -cp bin EndGameDatabase.DatabaseBuilder <p1> <p2> --db=268435456
 *   java -Xmx4G   -cp bin EndGameDatabase.DatabaseBuilder               (interactive)
 */
public class DatabaseBuilder {

    public static void main(String[] args) {
        List<String> argList = new ArrayList<>(Arrays.asList(args));

        boolean prevOnDisk = argList.remove("--pod");
        argList.remove("--im");

        // Check for --db or --db=N
        boolean diskBacked = false;
        long chunkSize = DatabaseBuilderDB.DEFAULT_CHUNK_SIZE;
        for (Iterator<String> it = argList.iterator(); it.hasNext(); ) {
            String a = it.next();
            if (a.equals("--db")) {
                diskBacked = true;
                it.remove();
            } else if (a.startsWith("--db=")) {
                diskBacked = true;
                chunkSize = Long.parseLong(a.substring(5));
                it.remove();
            }
        }

        String[] remaining = argList.toArray(new String[0]);

        if (remaining.length >= 2) {
            if (diskBacked) {
                String[] dbArgs = Arrays.copyOf(remaining, remaining.length + 1);
                dbArgs[remaining.length] = Long.toString(chunkSize);
                DatabaseBuilderDB.main(dbArgs);
            } else if (prevOnDisk) {
                DatabaseBuilderPOD.main(remaining);
            } else {
                DatabaseBuilderIM.main(remaining);
            }
            return;
        }

        // Interactive mode
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== Surakarta Endgame Database Builder ===");
        System.out.println("Mode: [1] in-memory (default)  [2] prev-on-disk  [3] disk-backed");
        System.out.print("> ");

        try {
            String modeLine = scanner.nextLine().trim();
            boolean usePrevOnDisk = modeLine.equals("2") || modeLine.equalsIgnoreCase("pod");
            boolean useDiskBacked = modeLine.equals("3") || modeLine.equalsIgnoreCase("db");

            if (useDiskBacked) {
                System.out.println("Chunk size in positions [default " + DatabaseBuilderDB.DEFAULT_CHUNK_SIZE + "]:");
                System.out.print("> ");
                String chunkLine = scanner.nextLine().trim();
                if (!chunkLine.isEmpty()) chunkSize = Long.parseLong(chunkLine);
            }

            System.out.println("Enter target as '<p1> <p2>'  (e.g.  3 3  or  2 4):");
            System.out.print("> ");
            String targetLine = scanner.nextLine().trim();
            String[] tokens = targetLine.split("\\s+");

            if (tokens.length < 2) {
                System.err.println("Error: expected two integers.");
                return;
            }

            try {
                int p1 = Integer.parseInt(tokens[0]);
                int p2 = Integer.parseInt(tokens[1]);
                long start = System.currentTimeMillis();

                if (useDiskBacked) {
                    new DatabaseBuilderDB(chunkSize).build(p1, p2);
                } else if (usePrevOnDisk) {
                    new DatabaseBuilderPOD().build(p1, p2);
                } else {
                    new DatabaseBuilderIM().build(p1, p2);
                }

                System.err.println("Total time: " + (System.currentTimeMillis() - start) / 1000 + "s");
            } catch (NumberFormatException e) {
                System.err.println("Error: enter integers >= 1.");
            } catch (IOException e) {
                System.err.println("IO error: " + e.getMessage());
                e.printStackTrace();
            }
        } finally {
            scanner.close();
        }
    }
}
