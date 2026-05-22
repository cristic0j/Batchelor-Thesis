# Surakarta AI Agent

A self-contained AI agent that computes optimal moves for the game of **Surakarta** (6×6).

---

## Agent: NegaMaxPlayer + SurakartaKanazawaEvaluator

This is the most efficient agent from the GameMaster framework:

| Component | Class | Role |
|-----------|-------|------|
| Search | `NegaMaxPlayer` | Iterative-deepening alpha-beta with PVS, TT, null-move, LMR, quiescence, RPS |
| Evaluator | `SurakartaKanazawaEvaluator` | Default & strongest Surakarta evaluator |
| Game | `SurakartaGame` | Rules, move ordering, draw detection |
| Move gen | `SurakartaGenerator` | Generates captures + non-captures; loads `surakartarps.txt` |

All other Surakarta evaluators (`SurakartaUltimateEvaluator`, `Surakarta2016Evaluator`, …) are included and can be swapped into `SurakartaGame`'s constructor.

---

## Directory Layout

```
AIAgent/
├── build.sh              # compile + run instructions
├── README.md             # this file
├── src/                  # 52 Java source files
│   ├── Main.java         # standalone entry point  ← start here
│   │
│   ├── — Surakarta game ——————————————————————
│   ├── SurakartaGame.java
│   ├── SurakartaGenerator.java
│   ├── SurakartaMove.java
│   ├── SurakartaAbstractEvaluator.java
│   ├── SurakartaKanazawaEvaluator.java   ← most efficient (default)
│   ├── SurakartaUltimateEvaluator.java
│   ├── Surakarta2016Evaluator.java
│   ├── SurakartaAmsEvaluator.java
│   ├── SurakartaEvaluator.java
│   ├── SurakartaMobEvaluator.java
│   ├── SurakartaPerfectEvaluator.java
│   │
│   ├── — Core framework ——————————————————————
│   ├── Board.java
│   ├── Game.java
│   ├── Generator.java
│   ├── Move.java / MoveStep.java / Piece.java
│   ├── Evaluator.java
│   ├── TranspositionTable.java / TableEntry.java
│   ├── CompressedBoard.java
│   ├── Log.java / Symmetry.java / Combinatorial.java / Combinatorics.java
│   │
│   └── — Player & utilities ——————————————————
│       ├── NegaMaxPlayer.java  (the main agent)
│       ├── Player.java / AbstractPlayer.java
│       ├── OpeningBookEntry.java (modified – no DraughtsGame dep)
│       ├── OpeningBookBigEntry.java / OpeningBookTable.java
│       ├── EndGameDatabaseFile.java / WinTable.java
│       └── ... (PNSPlayer, DFPNPlayer, Shot2Player, etc. – pulled in by Board)
│
└── data/
    ├── SurakartaOpeningBook6x6   # pre-computed opening book
    └── surakartarps.txt          # RPS move-type probability weights
```

---

## Build & Run

```bash
# Requires Java 11+
./build.sh
java -cp bin:data Main
```

Expected output (first call):
```
Surakarta AI Agent ready  (evaluator: SurakartaKanazawaEvaluator)
...
Best move : b2-c3
Score     : 150
```

---

## Using the Agent in Your Own Code

```java
// 1. Set board size
Board.setSize(6, 6);

// 2. Create game + agent
SurakartaGame game  = new SurakartaGame(new SurakartaKanazawaEvaluator());
NegaMaxPlayer agent = new NegaMaxPlayer(game);
agent.setFixedTime(3000);      // 3-second budget per move

// 3. Build starting position
Board board = new Board(game.getGenerator(), game);
game.startingConfiguration(board);
board.initialise(board.sideToMove);  // ← required: computes bit-lines

// 4. Search
Move best = agent.search(board);
best.apply(board);
```

### Swapping evaluators

```java
new SurakartaGame(new SurakartaUltimateEvaluator())   // more aggressive
new SurakartaGame(new SurakartaEvaluator())            // lightweight
```

---

## Data Files

| File | Required | Notes |
|------|----------|-------|
| `SurakartaOpeningBook6x6` | Recommended | Pre-computed opening book; must be on classpath |
| `surakartarps.txt` | Optional | RPS probability weights; defaults to 0.25 if missing |

Both must be on the JVM classpath at runtime (`-cp bin:data`).
