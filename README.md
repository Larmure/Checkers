# Checkers Java

Java checkers project featuring:
- interactive terminal mode
- JavaFX graphical interface
- configurable AI engines (minimax, alphabeta, iterative deepening, MCTS)
- network mode (host, join, list servers)

## Prerequisites

- Java 17
- Maven 3.8+
- Linux

Check versions:

```bash
java -version
mvn -version
```

## Run the Project

The `checkers` script starts the application through Maven:

```bash
./checkers
```

Show CLI help:

```bash
./checkers -h
```

Start in GUI mode:

```bash
./checkers -g
```

## Command-Line Options

Main options:

- `-h`, `--help`: show help
- `-V`, `--version`: show version
- `-v`, `--verbose`: enable verbose logs
- `-d`, `--debug`: enable debug logs
- `-g`, `--gui`: launch graphical interface
- `-b`, `--blitz`: enable blitz mode
- `-t`, `--time <minutes>`: time per player in blitz mode
- `-s`, `--size <8|10|12>`: board size
- `-c`, `--contest <arg>`: enable contest mode

AI options:

- `-a`, `--ai [W|B|A]`: enable AI for White, Black, or both
- `-at`, `--ai-time <seconds>`: AI time budget
- `-am`, `--ai-mode <minimax|alphabeta|iterative|mcts>`: global AI algorithm
- `-wam`, `--white-ai-mode <...>`: White-side AI algorithm
- `-bam`, `--black-ai-mode <...>`: Black-side AI algorithm
- `-ad`, `--ai-minimax-depth <n>`: minimax/alphabeta depth
- `-ams`, `--ai-minimax-scoring <simple|advanced|max>`: minimax scoring function
- `-as`, `--ai-mcts-selection <uct|ml>`: MCTS selection mode
- `-tr`, `--train <n>`: run ML training for `n` games

Examples:

```bash
# Local 10x10 game with AI vs human
./checkers -s 10 -a B -am alphabeta -at 3

# AI vs AI in 5-minute blitz mode
./checkers -a A -b -t 5

# GUI with White AI using MCTS
./checkers -g -a W -wam mcts -as uct
```

## In-Game Commands (Terminal Mode)

Moves:

- `A3 B4` (square-to-square notation)
- `12-16` (Manoury notation)

Local commands:

- `new [ARGS]`
- `help [CMD]`
- `show board|history|time|configuration`
- `set PARAM=VALUE`
- `save FILE`
- `load FILE`
- `undo [N]`
- `redo [N]`
- `hint`
- `pause`
- `continue`
- `quit`

Network commands:

- `server list`
- `server start [PORT]`
- `server stop`
- `server status`
- `join [IP[:PORT]]`
- `ping`
- `players`
- `score`

## Build, Tests, Quality

Compile:

```bash
mvn clean compile
```

Tests (JUnit + headless TestFX):

```bash
mvn test
```

AI benchmarks (manual, long-running):

```bash
mvn -DrunBenchmarks=true -Dtest=AiBenchmarkTest test
```

Optional runtime tuning:

```bash
mvn -DrunBenchmarks=true -Dtest=AiBenchmarkTest test \
	-Dbenchmark.games=20 -Dbenchmark.aiTimeMs=200 \
	-Dbenchmark.maxPlies=120 -Dbenchmark.minmaxDepth=3 -Dbenchmark.alphabetaDepth=4
```

Generated benchmark reports are saved to:

- `target/benchmarks/`
- one file per run (timestamped)

This produces:

- MinMax vs AlphaBeta vs MCTS-UCT (wins/losses/draws)
- MCTS-UCT vs MCTS-ML (wins/losses/draws)

Note: the MCTS-ML benchmark requires `ml_weights.txt` to be present.

Full verification (tests + PMD + CPD):

```bash
mvn verify
```

Run application without the wrapper script:

```bash
mvn exec:java -Dexec.mainClass=fr.ubordeaux.pdp.App
```

## User Configuration

At startup, the application loads (or creates) this file:

- `~/.checkersrc`

It contains, among other things:

- default options (`verbose`, `debug`, `blitz`, `timeout`, `contest`, `size`)
- keyboard shortcuts for the GUI

## Useful Project Structure

- `src/main/java`: main source code
- `src/test/java`: tests
- `src/main/resources`: resources (i18n, style)
- `checkers`: launch script
- `ml_weights.txt`: weights used by ML components
- `pdp/`: project report and deliverables

## Notes

- In CONNECTED mode, moves/commands are forwarded to the remote server.
- In SERVER mode, local game commands are blocked.
- The project uses JavaFX, Apache Commons CLI, JLine, and TensorFlow Java.
