package EndGameDatabase;

import EndGameDatabase.inMemory.DatabaseBuilderIM;
import EndGameDatabase.fileBacked.DatabaseBuilderFB;
import java.io.IOException;
import java.util.*;

/**
 * Main entry point for the Surakarta endgame database builder.
 *
 * Delegates to one of two implementations:
 *   --im   In-memory mode  (default): previous tables fully loaded into RAM
 *   --fb   File-backed mode:          previous tables read directly from disk
 *
 * Usage:
 *   java -Xmx4G -cp bin EndGameDatabase.DatabaseBuilder <p1> <p2> [--im|--fb]
 *   java -Xmx4G -cp bin EndGameDatabase.DatabaseBuilder               (interactive)
 */
public class DatabaseBuilder {

    public static void main(String[] args) {
        List<String> argList = new ArrayList<>(Arrays.asList(args));

        boolean fileBacked = argList.remove("--fb");
        argList.remove("--im"); // accepted but is the default

        String[] remaining = argList.toArray(new String[0]);

        if (remaining.length >= 2) {
            // Non-interactive: pass args straight through to the chosen builder
            if (fileBacked) {
                DatabaseBuilderFB.main(remaining);
            } else {
                DatabaseBuilderIM.main(remaining);
            }
            return;
        }

        // Interactive mode: ask for mode first, then delegate
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== Surakarta Endgame Database Builder ===");
        System.out.println("Mode: [1] in-memory (default)  [2] file-backed");
        System.out.print("> ");

        try {
            String modeLine = scanner.nextLine().trim();
            boolean useFileBacked = modeLine.equals("2") || modeLine.equalsIgnoreCase("fb");

            System.out.println("Enter target as '<p1> <p2>'  (e.g.  3 3  or  2 4):");
            System.out.print("> ");
            String targetLine = scanner.nextLine().trim();
            String[] tokens = targetLine.split("\\s+");

            if (tokens.length < 2) {
                System.err.println("Error: expected two integers.");
                return;
            }

            // Re-use the chosen builder's logic by calling build() directly
            try {
                long start = System.currentTimeMillis();
                if (useFileBacked) {
                    new DatabaseBuilderFB().build(
                        Integer.parseInt(tokens[0]),
                        Integer.parseInt(tokens[1])
                    );
                } else {
                    new DatabaseBuilderIM().build(
                        Integer.parseInt(tokens[0]),
                        Integer.parseInt(tokens[1])
                    );
                }
                long elapsed = System.currentTimeMillis() - start;
                System.err.println("Total time: " + elapsed / 1000 + "s");
            } catch (NumberFormatException e) {
                System.err.println("Error: enter two integers >= 1.");
            } catch (IOException e) {
                System.err.println("IO error: " + e.getMessage());
                e.printStackTrace();
            }
        } finally {
            scanner.close();
        }
    }
}
