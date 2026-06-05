# Surakarta Endgame Tablebase — Bachelor's Thesis

This project builds perfect-play endgame databases for **Surakarta**, a 2-player capture game played on a 6×6 board. Given any endgame position (typically ≤ 6 total pieces), the database tells you whether the position is a **WIN**, **LOSS**, or **DRAW** under optimal play, and which move achieves that outcome.

Three different indexing strategies are implemented and compared:

| Module | Strategy | Size reduction |
|--------|----------|---------------|
| `EndGameDatabase` | Flat combinatorial index | baseline |
| `EndGameDatabaseFormulaBasedSymmetry` | D4 symmetry reduction | up to ~8× fewer positions |
| `EndGameDatabaseRankSelect` | Rank-Select canonical indexing | same positions as flat, but compact storage |

---

## The Game

Surakarta is played on a 6×6 grid. Each player starts with pieces on their half of the board and alternates turns. Two move types exist:

- **Simple move** — slide to any adjacent empty square (including diagonals).
- **Capture move** — travel along the circular rail tracks embedded at the board edges. A piece must traverse at least one full loop before it can land on (and capture) an opponent's piece. Pieces on the four corner squares cannot enter the rails.

A player wins by capturing all of the opponent's pieces.

---

## Repository Structure

```
.
├── SurakartaGameEngine/          # Interactive 2-player game (stdin/stdout)
│   ├── dataClasses/              # BoardState, Move, Coordinate, BoardBorder
│   └── logicModules/             # SurakartaGame, MoveValidator, MoveProcessor
│
├── EndGameDatabase/              # Retrograde analysis — flat combinatorial index
│   ├── dataClasses/              # BinomialTable, IndexingInput, Outcome, AtomicHugeByteArray, …
│   ├── logicModules/             # Indexing, MoveListGenerator, FileReaderWriter, …
│   ├── inMemory/                 # All tables loaded into RAM (fastest, most memory)
│   ├── diskBacked/               # Both tables on disk (slowest, least memory)
│   ├── ramPrevDiskCurrent/       # Previous table in RAM, current on disk (balanced)
│   ├── DatabaseBuilder.java      # Unified entry point — routes to the three backends
│   ├── main.java                 # Indexing round-trip demonstration
│   └── data/                     # Output: moves{p1}v{p2}.txt  (2 bits/position)
│
├── EndGameDatabaseFormulaBasedSymmetry/   # Retrograde analysis — D4 symmetry reduction
│   ├── logicModules/
│   │   ├── SymmetricIndexing.java          # D4 canonical indexing (8 transforms)
│   │   ├── RetrogradeAnalysis/             # OneVsOne and Single analysis (symmetric)
│   │   └── RetrogradeAnalysisExecutor/     # Async executors for symmetric analysis
│   ├── DatabaseBuilderSymmetry.java        # Entry point
│   ├── main.java                           # Symmetry lookup demonstration
│   └── data/                               # Output: moves{p1}v{p2}.txt
│
├── EndGameDatabaseRankSelect/    # Rank-Select post-processing of flat tables
│   ├── dataClasses/RankSelectTable.java    # Three-structure compact index
│   ├── logicModules/
│   │   ├── RankSelectBuilder.java          # Builds the rank-select structure
│   │   └── RankSelectFileReaderWriter.java # Serialises/deserialises .rsk files
│   ├── DatabaseBuilderRankSelect.java      # Entry point
│   ├── main.java                           # Rank-select lookup demonstration
│   └── data/                               # Output: moves{p1}v{p2}.rsk
│
├── MoveAdvisor/                  # Queries a built database and recommends moves
│   ├── MoveAdvisor.java
│   └── main.java
│
├── Statistics/                   # Samples database tables for WIN/LOSS/DRAW ratios
│   └── TableSampler.java
│
├── Testing/                      # Validation: compares DB outcomes against AIAgent search
│   ├── Collector.java            # Step 1 — extract random positions to CSV
│   ├── Validator.java            # Step 2 — run AI on collected positions
│   ├── Tester.java               # All-in-one: collect + validate in one run
│   ├── FindOutcome.java          # Interactive: find first LOSS/WIN/DRAW in a table
│   └── AIValidationTest.java     # Batch comparison: DB vs AI alpha-beta
│
├── bitflip/                      # Integrity checker — detects bit-flip errors in .txt files
│   └── BitFlipChecker.java
│
└── AIAgent/                      # External Surakarta AI engine (reference implementation)
```

---

## Prerequisites

- **Java 17+** (uses `var`, `switch` expressions, `VarHandle`)
- No build tool required — all compilation is manual `javac`
- No external dependencies beyond the Java standard library

---

## Building

All modules share the `SurakartaGameEngine` source files, so they must be compiled together. The `-d bin` flag places all `.class` files under `bin/`.

### Game engine only

```bash
javac -d bin \
  SurakartaGameEngine/dataClasses/*.java \
  SurakartaGameEngine/logicModules/*.java \
  SurakartaGameEngine/main.java
```

### EndGameDatabase (all three storage backends)

```bash
javac -d bin \
  SurakartaGameEngine/dataClasses/*.java \
  SurakartaGameEngine/logicModules/*.java \
  EndGameDatabase/dataClasses/*.java \
  EndGameDatabase/logicModules/Indexing.java \
  EndGameDatabase/logicModules/MoveListGenerator.java \
  EndGameDatabase/logicModules/BitMapGenerator.java \
  EndGameDatabase/logicModules/SymmetryBuffer.java \
  EndGameDatabase/logicModules/RetrogradeAnalysis/*.java \
  EndGameDatabase/logicModules/RetrogradeAnalysisExecutor/*.java \
  EndGameDatabase/logicModules/RandomAccessFile/*.java \
  EndGameDatabase/inMemory/*.java \
  EndGameDatabase/diskBacked/*.java \
  EndGameDatabase/ramPrevDiskCurrent/*.java \
  EndGameDatabase/DatabaseBuilder.java \
  EndGameDatabase/main.java
```

### Formula-Based Symmetry module (add after the above)

```bash
javac -d bin \
  EndGameDatabaseFormulaBasedSymmetry/logicModules/SymmetricIndexing.java \
  EndGameDatabaseFormulaBasedSymmetry/logicModules/RetrogradeAnalysis/*.java \
  EndGameDatabaseFormulaBasedSymmetry/logicModules/RetrogradeAnalysisExecutor/*.java \
  EndGameDatabaseFormulaBasedSymmetry/DatabaseBuilderSymmetry.java \
  EndGameDatabaseFormulaBasedSymmetry/main.java
```

### Rank-Select module (add after EndGameDatabase)

```bash
javac -d bin \
  EndGameDatabaseRankSelect/dataClasses/RankSelectTable.java \
  EndGameDatabaseRankSelect/logicModules/*.java \
  EndGameDatabaseRankSelect/DatabaseBuilderRankSelect.java \
  EndGameDatabaseRankSelect/main.java
```

### MoveAdvisor (add after EndGameDatabase)

```bash
javac -d bin \
  MoveAdvisor/MoveAdvisor.java \
  MoveAdvisor/main.java
```

---

## Running

### Play the game

```bash
java -cp bin main
```

Prompts you for coordinates on each turn. Enter the square index (0–35, row-major) for the source and destination of your move.

---

### Build a flat endgame table

```bash
# Command-line (builds 3v3 and all its dependencies automatically)
java -Xmx4G -cp bin EndGameDatabase.DatabaseBuilder 3 3

# Interactive mode
java -Xmx4G -cp bin EndGameDatabase.DatabaseBuilder
```

Three storage backends are available. Choose based on available RAM:

| Flag | Backend | RAM needed | Speed |
|------|---------|-----------|-------|
| *(default)* / `--im` | In-memory | ~4 GB for 3v3 | Fastest |
| `--rpdc` | RAM-prev / disk-current | ~1–2 GB | Medium |
| `--db` | Fully disk-backed | ~512 MB | Slowest |

```bash
# Low-memory example
java -Xmx512M -cp bin EndGameDatabase.DatabaseBuilder 3 3 --db
```

Output files are written to `EndGameDatabase/data/moves{p1}v{p2}.txt`. Each file stores 2 bits per position (LOSS=0, WIN=1, DRAW=2) packed 4 outcomes per byte.

The builder performs topological ordering automatically — if you request `3v3`, it will first build `1v1`, then `1v2` + `2v1`, then `2v2`, and finally `3v3`, skipping any that already exist on disk.

---

### Build a symmetry-reduced table

```bash
java -Xmx2G -cp bin EndGameDatabaseFormulaBasedSymmetry.DatabaseBuilderSymmetry 3 3

# Interactive
java -Xmx2G -cp bin EndGameDatabaseFormulaBasedSymmetry.DatabaseBuilderSymmetry
```

Output goes to `EndGameDatabaseFormulaBasedSymmetry/data/`. The D4 symmetry group (4 rotations × 2 reflections) reduces the number of stored positions by up to 8×. Positions with fewer symmetries (e.g. pieces on a symmetry axis) gain less compression.

---

### Build a rank-select compressed table

The Rank-Select module does **not** re-run retrograde analysis. It post-processes flat tables already built by `EndGameDatabase.DatabaseBuilder`.

```bash
# Convert one table
java -Xmx4G -cp bin EndGameDatabaseRankSelect.DatabaseBuilderRankSelect 3 3

# Convert all flat tables at once
java -Xmx4G -cp bin EndGameDatabaseRankSelect.DatabaseBuilderRankSelect all

# Interactive
java -Xmx4G -cp bin EndGameDatabaseRankSelect.DatabaseBuilderRankSelect
```

Output goes to `EndGameDatabaseRankSelect/data/moves{p1}v{p2}.rsk`. The `.rsk` format stores:

1. **Bit array** — one bit per flat index, set if this index is the D4-canonical representative.
2. **Rank index** — sampled prefix-sum over the bit array for O(1) rank queries.
3. **Outcomes array** — 2 bits per canonical position, in ascending flat-index order.

This achieves similar space savings to the symmetry module, but without changing the retrograde analysis itself.

---

### Query a position with the Move Advisor

Edit `MoveAdvisor/main.java` to set your board and active player, then:

```bash
java -cp bin MoveAdvisor.main
```

The advisor prints the current outcome (WIN/LOSS/DRAW) for the active player, then recommends the best available move — a move that puts the opponent into a LOSS position if one exists, or a DRAW-preserving move otherwise.

---

### Sample database statistics

Prints WIN/LOSS/DRAW percentages for every table found in `EndGameDatabase/data/`:

```bash
javac -d bin Statistics/TableSampler.java   # (if not already compiled)
java -cp bin Statistics.TableSampler
```

Tables with fewer than 10,000 positions are sampled with 1,000 draws; larger tables with 10,000.

---

### Validate database against AI search

The `Testing/` utilities compare database outcomes against an alpha-beta AI to verify correctness.

```bash
# Step 1 — collect random positions from the DB into a CSV
java -cp Testing/collector_bin Collector <p1> <p2> [samples]

# Step 2 — run AI on each position and compare
java -cp Testing/validator_bin:AIAgent/data Validator Testing/collected_<p1>v<p2>.csv

# Or run both steps in one go
java -cp Testing/bin:AIAgent/data Tester
```

---

## Architecture

### Indexing

Positions are mapped to integers via **combinatorial (ranking) indexing** in two layers:

1. **Which M squares are occupied** — rank among all `C(36, M)` subsets.
2. **Which of those M squares belong to Player 1** — rank among all `C(M, p1)` subsets.

`BinomialTable` pre-computes Pascal's triangle up to size 36. `Indexing.java` converts between a board array and a file-position integer in both directions (`indexInputToFilePosition` / `filePositionToIndexingInput`).

### Retrograde Analysis

Retrograde analysis works backwards from terminal positions:

- **Terminal (capture possible) = WIN** — if the active player can immediately capture, the position is a win.
- Propagation: a position is a **LOSS** if every move leads to a WIN for the opponent; it is a **WIN** if at least one move leads to a LOSS for the opponent. Positions that never resolve become **DRAW**.

Two analysis classes handle different table shapes:

- `OneVsOneRetrogradeAnalysis` — handles the 1v1 base case.
- `SingleRetrogradeAnalysis` — handles NvM by consulting the already-built (N-1)vM and Nv(M-1) tables as sub-game oracles.

Parallel variants (`SingleRAParallelExecutor`) split the index space in half and process both halves concurrently using `AtomicHugeByteArray` for thread-safe byte-level updates.

### Storage Format

Each `.txt` file packs **4 outcomes per byte** (2 bits each, MSB-first):

```
byte = [outcome3 | outcome2 | outcome1 | outcome0]
         bits 7-6   bits 5-4   bits 3-2   bits 1-0
```

`FileReaderWriter` provides random-access reads and writes to these files via `java.io.RandomAccessFile`.

### D4 Symmetry (Formula-Based)

The Surakarta board has 8-fold dihedral symmetry (D4): 4 rotations and 4 reflections. `SymmetricIndexing` applies all 8 transforms to a position, picks the lexicographically smallest index as the canonical representative, and stores only that one. The 36 board squares collapse into 6 symmetry orbits; the canonical index is computed directly from the orbit structure without enumeration.

### Rank-Select Indexing

`RankSelectTable` combines three coordinated arrays:

- **`bitArray`** — 1 bit per flat index, marking canonical positions.
- **`rankIndex`** — sampled prefix sums (one entry per 65,536 flat indices) for fast rank computation.
- **`outcomes`** — packed 2-bit outcomes, one per canonical position.

A lookup takes the canonical flat index, computes its rank using `popcount` over the bit array, and indexes into `outcomes`. All three operations are O(1).

---

## Data Files

| Pattern | Location | Format | Size (3v3) |
|---------|----------|--------|-----------|
| `moves{p1}v{p2}.txt` | `EndGameDatabase/data/` | 2 bits/position, packed | ~760 KB |
| `moves{p1}v{p2}.txt` | `EndGameDatabaseFormulaBasedSymmetry/data/` | same format, ~8× fewer positions | ~100 KB |
| `moves{p1}v{p2}.rsk` | `EndGameDatabaseRankSelect/data/` | rank-select binary | varies |

Data files are not included in the repository. Build them by running the respective `DatabaseBuilder` class.
