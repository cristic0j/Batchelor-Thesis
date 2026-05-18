# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Surakarta game engine + endgame tablebase generator for a Bachelor's Thesis. Surakarta is a 2-player board game on a 6x6 grid (36 squares) where pieces can capture via circular rails. The project has three modules:

- **SurakartaGameEngine** — Interactive 2-player game
- **EndGameDatabase** — Retrograde analysis to build optimal-play endgame databases
- **MoveAdvisor** — Given a board state, queries the database and recommends the best move

## Build & Run

No build system (no Maven/Gradle). Manual `javac` compilation required. No external dependencies beyond the Java standard library.

**Compile and run the game:**
```bash
javac -d bin SurakartaGameEngine/dataClasses/*.java SurakartaGameEngine/logicModules/*.java SurakartaGameEngine/main.java
java -cp bin main
```

**Compile the database module (required for both entry points below):**
```bash
javac -d bin SurakartaGameEngine/dataClasses/*.java SurakartaGameEngine/logicModules/*.java EndGameDatabase/dataClasses/*.java EndGameDatabase/logicModules/Indexing.java EndGameDatabase/logicModules/MoveListGenerator.java EndGameDatabase/logicModules/RetrogradeAnalysis/*.java EndGameDatabase/logicModules/RetrogradeAnalysisExecutor/*.java EndGameDatabase/logicModules/RandomAccesFile/*.java EndGameDatabase/DatabaseBuilder.java EndGameDatabase/main.java
```

**Run the database builder (choose any target; dependencies built automatically):**
```bash
# Command-line args
java -Xmx4G -cp bin EndGameDatabase.DatabaseBuilder 3 3

# Interactive mode
java -Xmx4G -cp bin EndGameDatabase.DatabaseBuilder
```

The builder prints a dependency plan, skips any tables whose files already exist in `EndGameDatabase/data/`, then builds remaining tables in topological order.

**Compile and run the move advisor:**
```bash
javac -d bin SurakartaGameEngine/dataClasses/*.java SurakartaGameEngine/logicModules/*.java EndGameDatabase/dataClasses/*.java EndGameDatabase/logicModules/Indexing.java EndGameDatabase/logicModules/MoveListGenerator.java EndGameDatabase/logicModules/RandomAccessFile/FileReaderWriter.java MoveAdvisor/MoveAdvisor.java MoveAdvisor/main.java
java -cp bin MoveAdvisor.main
```

Edit `MoveAdvisor/main.java` to set the board and active player for your position.

**Run the symmetry test:**
```bash
javac -d bin EndGameDatabase/dataClasses/*.java EndGameDatabase/logicModules/Indexing.java SurakartaGameEngine/dataClasses/*.java Testing/TestHashMap.java Testing/main.java
java -cp bin main
```

## Architecture

### Board Representation

The 6x6 board is a flat `int[36]` array. Squares are indexed 0–35 (row-major). Piece values: `0` = empty, `1` = Player 1, `2` = Player 2. `BoardState` holds the array plus piece counts.

### Move Types

- **Simple move:** Slide to an adjacent empty square (non-capturing).
- **Capture move:** Follow circular rail tracks until landing on an opponent piece. The four circular loops pass through the inner ring of each edge. Validated recursively by `MoveValidator`, which reuses `CircleMoveMap` (a static HashMap of 16 border-corner transitions) and `BoardBorder` (enum for edge detection).

### Game Engine (`SurakartaGameEngine/`)

`SurakartaGame` drives the game loop: read stdin → validate with `MoveValidator` → execute with `MoveProcessor` → check win. Full move history is kept as a `Stack<BoardState>`.

### Endgame Database (`EndGameDatabase/`)

**Indexing:** Positions are mapped to a linear index using combinatorial (ranking) indexing via `BinomialTable` (precomputed Pascal's triangle). The index is two-layered:
1. Which M squares among 36 are occupied → `C(36, M)` possibilities.
2. Among those M squares, which belong to Player 1 → `C(M, p1)` possibilities.

`Indexing.java` converts between board state and integer index in both directions.

**Retrograde analysis:** Starts from terminal positions (captures possible = WIN) and propagates backwards:
- `RootRetrogradeAnalysis` handles 1v1.
- `SingleRetrogradeAnalysis` handles NvM, reading the already-computed (N−1)vM and Nv(M−1) databases as subgame oracles.
- Outcomes: `LOSS=0`, `WIN=1`, `UNCERTAIN=2`.
- Executor classes (`RootRAExecutor`, `SingleRAExecutor`, `SingleRAParalelExecutor`) wrap analysis in `CompletableFuture` for async/parallel execution. The parallel variant splits the index space into left/right halves using `AtomicByteArray` for thread safety.

**Storage:** `FileReaderWriter` packs 4 two-bit outcomes per byte into a `RandomAccessFile`. Database files are `EndGameDatabase/data/moves<p1>v<p2>.txt`.

**`AtomicByteArray`** (`EndGameDatabase/dataClasses/AtomicByteArray.java`): Custom thread-safe byte array using `VarHandle`. Replaces `AtomicIntegerArray` to reduce memory usage (1 byte per element vs 4). API mirrors `AtomicIntegerArray`: `get`, `set`, `getAndSet`, `compareAndSet`, `getAndAdd`, `getAndIncrement`, `length`. Values are unsigned 0–255.

**Current state (`EndGameDatabase/main.java`):** Reads `moves1v2.txt` (21,420 positions), computes the 2v2 analysis, writes `moves2v2.txt`. Commented-out blocks show the intended sequence: 1v1 → 1v2 → 2v1 → 1v3 → 3v1 → 2v2 → ...

### Move Advisor (`MoveAdvisor/`)

`MoveAdvisor.advise(int[] board, int activePlayer)` queries the pre-built database to suggest the best move for the active player:

1. Counts pieces, looks up the current position in `EndGameDatabase/data/moves<my>v<opp>.txt`.
2. Reports the current outcome (WIN / DRAW / LOSS) for the active player.
3. For **WIN**: finds and prints a move that puts the opponent in a LOSS position.
4. For **DRAW**: finds and prints a move that keeps the opponent in a DRAW (or LOSS) position.
5. For **LOSS**: reports that no winning or drawing move exists.

The indexing perspective flips when looking up the opponent's resulting position (`flipThePieces = (opponent == 2)`), so the file and index always match the active-player-as-P1 convention used during database construction.

### Key dependency between modules

`EndGameDatabase` and `MoveAdvisor` both import from `SurakartaGameEngine` — specifically `BoardState`, `Move`, and `MoveValidator` — so the game engine classes must always be on the classpath when compiling those modules.
