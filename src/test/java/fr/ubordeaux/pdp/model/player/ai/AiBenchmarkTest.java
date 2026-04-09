package fr.ubordeaux.pdp.model.player.ai;

import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.core.Move;
import fr.ubordeaux.pdp.model.evaluation.MaxEvaluator;
import fr.ubordeaux.pdp.model.player.AiPlayer;
import fr.ubordeaux.pdp.model.player.PlayerColor;
import fr.ubordeaux.pdp.model.tools.ManagerUndoRedo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Manual AI benchmark suite.
 *
 * <p>These tests are long-running and non-deterministic. They only run when
 * {@code -DrunBenchmarks=true} is provided.
 */
@Tag("benchmark")
@EnabledIfSystemProperty(named = "runBenchmarks", matches = "true")
class AiBenchmarkTest {

  private static final int BOARD_SIZE = 8;
  private static final int DEFAULT_MAX_PLIES = 120;
  private static final int DEFAULT_GAMES_PER_DUEL = 12;
  private static final int DEFAULT_AI_TIME_MS = 250;
  private static final int DEFAULT_MINMAX_DEPTH = 3;
  private static final int DEFAULT_MINMAX_DEPTH_A = 3;
  private static final int DEFAULT_MINMAX_DEPTH_B = 4;
  private static final int DEFAULT_ALPHABETA_DEPTH = 4;

  private static final int MAX_PLIES = intProperty("benchmark.maxPlies", DEFAULT_MAX_PLIES);
  private static final int GAMES_PER_DUEL = intProperty("benchmark.games", DEFAULT_GAMES_PER_DUEL);
  private static final long AI_TIME_MS = longProperty("benchmark.aiTimeMs", DEFAULT_AI_TIME_MS);

  private static final int MINMAX_DEPTH = intProperty("benchmark.minmaxDepth",
      DEFAULT_MINMAX_DEPTH);
  private static final int MINMAX_DEPTH_A = intProperty("benchmark.minmaxDepthA",
      DEFAULT_MINMAX_DEPTH_A);
  private static final int MINMAX_DEPTH_B = intProperty("benchmark.minmaxDepthB",
      DEFAULT_MINMAX_DEPTH_B);
  private static final int ALPHABETA_DEPTH = intProperty("benchmark.alphabetaDepth",
      DEFAULT_ALPHABETA_DEPTH);

  private static final int MCTS_DEPTH_IGNORED = 1;

  @Test
  @DisplayName("Benchmark MinMax vs AlphaBeta vs MCTS (W/L/D)")
  void benchmarkAiVsAi() throws IOException {
    List<DuelStats> duels = new ArrayList<>();
    duels.add(runDuel(AlgoProfile.minMax(), AlgoProfile.alphaBeta(), GAMES_PER_DUEL));
    duels.add(runDuel(AlgoProfile.minMax(), AlgoProfile.mctsUct(), GAMES_PER_DUEL));
    duels.add(runDuel(AlgoProfile.alphaBeta(), AlgoProfile.mctsUct(), GAMES_PER_DUEL));

    String report = formatReport("AI vs AI benchmark (MinMax / AlphaBeta / MCTS-UCT)", duels);
    Path output = writeReport("ai-vs-ai", report);

    System.out.println(report);
    System.out.println("Saved benchmark report to: " + output);

    assertEquals(3, duels.size());
    assertTrue(report.contains("MinMax"));
    assertTrue(report.contains("AlphaBeta"));
    assertTrue(report.contains("MCTS-UCT"));
  }

  @Test
  @DisplayName("Benchmark MCTS-UCT vs MCTS-ML")
  void benchmarkMctsUctVsMl() throws IOException {
    Path weightPath = Paths.get(LogisticRegressionTrainer.OUTPUT_FILEPATH);
    assertTrue(Files.exists(weightPath),
        "Missing ML weights file: " + weightPath + ". Train once with -tr before benchmark.");

    Mcts.loadMlWeights(weightPath.toString());

    DuelStats duel = runDuel(AlgoProfile.mctsUct(), AlgoProfile.mctsMl(), GAMES_PER_DUEL);
    String report = formatReport("MCTS selection benchmark (UCT vs ML)", List.of(duel));
    Path output = writeReport("mcts-uct-vs-ml", report);

    System.out.println(report);
    System.out.println("Saved benchmark report to: " + output);

    assertEquals(GAMES_PER_DUEL, duel.totalGames());
    assertTrue(report.contains("MCTS-UCT"));
    assertTrue(report.contains("MCTS-ML"));
  }

  @Test
  @DisplayName("Benchmark MinMax depth A vs depth B")
  void benchmarkMinMaxDepthVsDepth() throws IOException {
    DuelStats duel = runDuel(
        AlgoProfile.minMaxDepth(MINMAX_DEPTH_A),
        AlgoProfile.minMaxDepth(MINMAX_DEPTH_B),
        GAMES_PER_DUEL);

    String title = String.format("MinMax depth benchmark (d=%d vs d=%d)",
        MINMAX_DEPTH_A, MINMAX_DEPTH_B);
    String report = formatReport(title, List.of(duel));
    Path output = writeReport("minmax-depth-vs-depth", report);

    System.out.println(report);
    System.out.println("Saved benchmark report to: " + output);

    assertEquals(GAMES_PER_DUEL, duel.totalGames());
    assertTrue(report.contains("MinMax-d" + MINMAX_DEPTH_A));
    assertTrue(report.contains("MinMax-d" + MINMAX_DEPTH_B));
  }

  @Test
  @DisplayName("Benchmark MinMax vs MCTS-UCT")
  void benchmarkMinMaxVsMctsUct() throws IOException {
    DuelStats duel = runDuel(
        AlgoProfile.minMax(),
        AlgoProfile.mctsUct(),
        GAMES_PER_DUEL);

    String title = String.format("MinMax vs MCTS-UCT benchmark (minmaxDepth=%d)", MINMAX_DEPTH);
    String report = formatReport(title, List.of(duel));
    Path output = writeReport("minmax-vs-mcts-uct", report);

    System.out.println(report);
    System.out.println("Saved benchmark report to: " + output);

    assertEquals(GAMES_PER_DUEL, duel.totalGames());
    assertTrue(report.contains("MinMax"));
    assertTrue(report.contains("MCTS-UCT"));
  }

  @Test
  @DisplayName("Benchmark AlphaBeta vs MCTS-UCT")
  void benchmarkAlphaBetaVsMctsUct() throws IOException {
    DuelStats duel = runDuel(
        AlgoProfile.alphaBeta(),
        AlgoProfile.mctsUct(),
        GAMES_PER_DUEL);

    String title = String.format("AlphaBeta vs MCTS-UCT benchmark (alphabetaDepth=%d)",
        ALPHABETA_DEPTH);
    String report = formatReport(title, List.of(duel));
    Path output = writeReport("alphabeta-vs-mcts-uct", report);

    System.out.println(report);
    System.out.println("Saved benchmark report to: " + output);

    assertEquals(GAMES_PER_DUEL, duel.totalGames());
    assertTrue(report.contains("AlphaBeta"));
    assertTrue(report.contains("MCTS-UCT"));
  }

  @Test
  @DisplayName("Benchmark MinMax vs MCTS-ML")
  void benchmarkMinMaxVsMctsMl() throws IOException {
    Path weightPath = Paths.get(LogisticRegressionTrainer.OUTPUT_FILEPATH);
    assertTrue(Files.exists(weightPath),
        "Missing ML weights file: " + weightPath + ". Train once with -tr before benchmark.");

    Mcts.loadMlWeights(weightPath.toString());

    DuelStats duel = runDuel(
        AlgoProfile.minMax(),
        AlgoProfile.mctsMl(),
        GAMES_PER_DUEL);

    String title = String.format("MinMax vs MCTS-ML benchmark (minmaxDepth=%d)", MINMAX_DEPTH);
    String report = formatReport(title, List.of(duel));
    Path output = writeReport("minmax-vs-mcts-ml", report);

    System.out.println(report);
    System.out.println("Saved benchmark report to: " + output);

    assertEquals(GAMES_PER_DUEL, duel.totalGames());
    assertTrue(report.contains("MinMax"));
    assertTrue(report.contains("MCTS-ML"));
  }

  private DuelStats runDuel(AlgoProfile algoA, AlgoProfile algoB, int games) {
    DuelStats stats = new DuelStats(algoA.name, algoB.name);

    for (int i = 0; i < games; i++) {
      boolean aIsWhite = (i % 2 == 0);
      AiPlayer white = new AiPlayer(
          aIsWhite ? algoA.name + "-W" : algoB.name + "-W",
          aIsWhite ? algoA.createAi() : algoB.createAi(),
          new MaxEvaluator());
      AiPlayer black = new AiPlayer(
          aIsWhite ? algoB.name + "-B" : algoA.name + "-B",
          aIsWhite ? algoB.createAi() : algoA.createAi(),
          new MaxEvaluator());

      long start = System.currentTimeMillis();
      PlayerColor winner = runSingleMatch(white, black);
      long durationMs = System.currentTimeMillis() - start;

      stats.record(winner, aIsWhite, durationMs);
    }

    return stats;
  }

  private PlayerColor runSingleMatch(AiPlayer white, AiPlayer black) {
    Board board = new Board(BOARD_SIZE);
    ManagerUndoRedo undo = new ManagerUndoRedo(board);
    boolean whiteTurn = true;

    for (int plies = 0; plies < MAX_PLIES; plies++) {
      PlayerColor currentColor = whiteTurn ? PlayerColor.WHITE : PlayerColor.BLACK;
      AiPlayer currentPlayer = whiteTurn ? white : black;

      Move move = currentPlayer.getBestMove(undo, board, currentColor);
      if (move == null) {
        return opponent(currentColor);
      }

      board.applyMove(move);
      undo.registerMove(currentColor, move);

      if (board.noPiecesLeft(PlayerColor.WHITE)) {
        return PlayerColor.BLACK;
      }
      if (board.noPiecesLeft(PlayerColor.BLACK)) {
        return PlayerColor.WHITE;
      }

      whiteTurn = !whiteTurn;
    }

    return null;
  }

  private static PlayerColor opponent(PlayerColor color) {
    return color == PlayerColor.WHITE ? PlayerColor.BLACK : PlayerColor.WHITE;
  }

  private String formatReport(String title, List<DuelStats> duels) {
    StringBuilder sb = new StringBuilder();
    sb.append("=== ").append(title).append(" ===\n");
    sb.append("timestamp: ")
        .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
        .append("\n");
    sb.append("games_per_duel: ").append(GAMES_PER_DUEL).append("\n");
    sb.append("max_plies: ").append(MAX_PLIES).append("\n");
    sb.append("ai_time_ms: ").append(AI_TIME_MS).append("\n");
    sb.append("minmax_depth: ").append(MINMAX_DEPTH).append("\n");
    sb.append("minmax_depth_a: ").append(MINMAX_DEPTH_A).append("\n");
    sb.append("minmax_depth_b: ").append(MINMAX_DEPTH_B).append("\n");
    sb.append("alphabeta_depth: ").append(ALPHABETA_DEPTH).append("\n\n");

    for (DuelStats duel : duels) {
      sb.append(duel.formatLine()).append("\n");
    }

    return sb.toString();
  }

  private Path writeReport(String prefix, String report) throws IOException {
    Path outputDir = Paths.get("target", "benchmarks");
    Files.createDirectories(outputDir);

    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    Path outputFile = outputDir.resolve(prefix + "-" + timestamp + ".txt");

    Files.writeString(outputFile, report);
    return outputFile;
  }

  private static final class DuelStats {
    private final String algoA;
    private final String algoB;

    private int winsA;
    private int winsB;
    private int draws;
    private long totalDurationMs;

    DuelStats(String algoA, String algoB) {
      this.algoA = algoA;
      this.algoB = algoB;
    }

    void record(PlayerColor winner, boolean aIsWhite, long durationMs) {
      totalDurationMs += durationMs;
      if (winner == null) {
        draws++;
        return;
      }

      boolean aWon = (winner == PlayerColor.WHITE && aIsWhite)
          || (winner == PlayerColor.BLACK && !aIsWhite);
      if (aWon) {
        winsA++;
      } else {
        winsB++;
      }
    }

    int totalGames() {
      return winsA + winsB + draws;
    }

    String formatLine() {
      int total = totalGames();
      double aRate = total == 0 ? 0.0 : (100.0 * winsA) / total;
      double bRate = total == 0 ? 0.0 : (100.0 * winsB) / total;
      double drawRate = total == 0 ? 0.0 : (100.0 * draws) / total;
      long avgMs = total == 0 ? 0L : totalDurationMs / total;

      return String.format(
          "%s vs %s | games=%d | %s: W=%d (%.1f%%) | %s: W=%d (%.1f%%) | "
              + "draws=%d (%.1f%%) | avg_game_ms=%d",
          algoA, algoB, total, algoA, winsA, aRate, algoB, winsB, bRate, draws, drawRate, avgMs);
    }
  }

  private static final class AlgoProfile {
    private final String name;
    private final AiFactory factory;

    AlgoProfile(String name, AiFactory factory) {
      this.name = name;
      this.factory = factory;
    }

    Ai createAi() {
      return factory.create();
    }

    static AlgoProfile minMax() {
      return new AlgoProfile(
          "MinMax",
          () -> new MinMax(MINMAX_DEPTH, AI_TIME_MS));
    }

    static AlgoProfile minMaxDepth(int depth) {
      return new AlgoProfile(
          "MinMax-d" + depth,
          () -> new MinMax(depth, AI_TIME_MS));
    }

    static AlgoProfile alphaBeta() {
      return new AlgoProfile(
          "AlphaBeta",
          () -> new MinMaxAlphaBeta(ALPHABETA_DEPTH, AI_TIME_MS));
    }

    static AlgoProfile mctsUct() {
      return new AlgoProfile("MCTS-UCT", () -> {
        Mcts mcts = new Mcts(MCTS_DEPTH_IGNORED, AI_TIME_MS, Mcts.DEFAULT_EXPLORATION);
        mcts.setSelectionMode(SelectionMode.UCT);
        return mcts;
      });
    }

    static AlgoProfile mctsMl() {
      return new AlgoProfile("MCTS-ML", () -> {
        Mcts mcts = new Mcts(MCTS_DEPTH_IGNORED, AI_TIME_MS, Mcts.DEFAULT_EXPLORATION);
        mcts.setSelectionMode(SelectionMode.ML);
        return mcts;
      });
    }
  }

  private static int intProperty(String key, int defaultValue) {
    String value = System.getProperty(key);
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    return Integer.parseInt(value);
  }

  private static long longProperty(String key, long defaultValue) {
    String value = System.getProperty(key);
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    return Long.parseLong(value);
  }

  @FunctionalInterface
  private interface AiFactory {
    Ai create();
  }
}
