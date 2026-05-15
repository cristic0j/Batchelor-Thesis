package EndGameDatabase;

import EndGameDatabase.inMemory.DatabaseBuilderIM;
import EndGameDatabase.diskBacked.DatabaseBuilderDB;
import EndGameDatabase.ramPrevDiskCurrent.DatabaseBuilderRPDC;
import java.io.IOException;
import java.util.*;

/**
 * Main entry point for the Surakarta endgame database builder.
 *
 * Delegates to one of three implementations:
 *   --im           In-memory mode       (default): all tables fully loaded into RAM
 *   --rpdc[=N]     RAM-prev disk-current mode:    previous in RAM, current in disk chunks of N
 *   --db[=N]       Disk-backed mode:              both previous on disk, current in disk chunks of N
 *                                                 (default N = 2^31 for --rpdc and --db)
 *
 * Usage:
 *   java -Xmx4G   -cp bin EndGameDatabase.DatabaseBuilder <p1> <p2> [--im]
 *   java -Xmx4G   -cp bin EndGameDatabase.DatabaseBuilder <p1> <p2> --rpdc
 *   java -Xmx512M -cp bin EndGameDatabase.DatabaseBuilder <p1> <p2> --db
 *   java -Xmx512M -cp bin EndGameDatabase.DatabaseBuilder <p1> <p2> --db=268435456
 *   java -Xmx4G   -cp bin EndGameDatabase.DatabaseBuilder               (interactive)
 */
public class DatabaseBuilder {

    public static void main(String[] args) {
        List<String> argList = new ArrayList<>(Arrays.asList(args));

        argList.remove("--im");

        // Check for --db[=N], --rpdc[=N]
        boolean diskBacked = false;
        boolean ramPrevDiskCurrent = false;
        long chunkSize = DatabaseBuilderDB.DEFAULT_CHUNK_SIZE;
        for (Iterator<String> it = argList.iterator(); it.hasNext(); ) {
            String a = it.next();
            if (a.equals("--db")) {
                diskBacked = true; it.remove();
            } else if (a.startsWith("--db=")) {
                diskBacked = true; chunkSize = Long.parseLong(a.substring(5)); it.remove();
            } else if (a.equals("--rpdc")) {
                ramPrevDiskCurrent = true; it.remove();
            } else if (a.startsWith("--rpdc=")) {
                ramPrevDiskCurrent = true; chunkSize = Long.parseLong(a.substring(7)); it.remove();
            }
        }

        String[] remaining = argList.toArray(new String[0]);

        if (remaining.length >= 2) {
            String[] withChunk = Arrays.copyOf(remaining, remaining.length + 1);
            withChunk[remaining.length] = Long.toString(chunkSize);
            if (diskBacked) {
                DatabaseBuilderDB.main(withChunk);
            } else if (ramPrevDiskCurrent) {
                DatabaseBuilderRPDC.main(withChunk);
            } else {
                DatabaseBuilderIM.main(remaining);
            }
            return;
        }

        // Interactive mode
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== Surakarta Endgame Database Builder ===");
        System.out.println("Mode: [1] in-memory (default)  [2] RAM-prev disk-current  [3] disk-backed");
        System.out.print("> ");

        try {
            String modeLine = scanner.nextLine().trim();
            boolean useRamPrevDiskCurrent = modeLine.equals("2") || modeLine.equalsIgnoreCase("rpdc");
            boolean useDiskBacked       = modeLine.equals("3") || modeLine.equalsIgnoreCase("db");

            if (useRamPrevDiskCurrent || useDiskBacked) {
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
                } else if (useRamPrevDiskCurrent) {
                    new DatabaseBuilderRPDC(chunkSize).build(p1, p2);
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
