package fr.ubordeaux.pdp.model.tools;

import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LoadBoardTest {

  private static final String SAVE_DIR = "Sauvegarde";

  private static class TestableLoadBoard extends LoadBoard {
    @Override
    protected void exitOnLoadError() {
      throw new IllegalStateException("LOAD_ABORTED");
    }
  }

  private Path getSavePath(String fileName) {
    return Path.of(System.getProperty("user.dir"), SAVE_DIR, fileName);
  }

  private void writeSaveFile(String fileName, String content) throws IOException {
    Path dir = Path.of(System.getProperty("user.dir"), SAVE_DIR);
    Files.createDirectories(dir);
    Files.writeString(getSavePath(fileName), content);
  }

  private String buildBoardAscii(int n, Map<String, Character> pieces) {
    StringBuilder sb = new StringBuilder();
    for (int row = n - 1; row >= 0; row--) {
      for (int col = 0; col < n; col++) {
        String sq = "" + (char) ('A' + row) + (col + 1);
        char c = pieces.getOrDefault(sq, '_');
        sb.append(c);
        if (col < n - 1) {
          sb.append(" ");
        }
      }
      sb.append("\n");
    }
    return sb.toString();
  }

  private boolean isPlayableSquare(String square) {
    int row = Character.toUpperCase(square.charAt(0)) - 'A';
    int col = Integer.parseInt(square.substring(1)) - 1;
    return ((row + col) % 2) == 0;
  }

  private void assertBoardEmpty(Board board) {
    int n = board.getSizeBoard();
    for (int row = 0; row < n; row++) {
      for (int col = 0; col < n; col++) {
        String sq = "" + (char) ('A' + row) + (col + 1);
        if (!isPlayableSquare(sq)) {
          continue;
        }
        assertFalse(board.occupied(sq), "Expected empty playable square: " + sq);
      }
    }
  }

  private String initialBoardAscii(int n) {
    return new Board(n).boardString();
  }

  private String baseSettings(int n) {
    return "[settings]\n"
        + "starting-player=white\n"
        + "board-size=" + n + "\n"
        + "time-mode=classic\n"
        + "verbose=false\n"
        + "debug=false\n"
        + "ai-mode=none\n";
  }

  private String fullSave(String settingsBlock, String boardBlock, String historyBlock) {
    return settingsBlock
        + "\n"
        + "[game]\n"
        + boardBlock
        + "\n"
        + "[history]\n"
        + historyBlock;
  }

  @Test
  void testLoadRestoresTurnAndKings() throws IOException {
    int n = 10;

    Map<String, Character> pieces = new HashMap<>();
    pieces.put("A1", 'o');
    pieces.put("B2", 'x');
    pieces.put("C3", 'O');
    pieces.put("D4", 'X');

    String content = "[settings]\n"
        + "starting-player=black\n"
        + "board-size=" + n + "\n"
        + "time-mode=classic\n"
        + "verbose=false\n"
        + "debug=false\n"
        + "ai-mode=none\n"
        + "\n"
        + "[game]\n"
        + buildBoardAscii(n, pieces)
        + "\n"
        + "[history]\n";

    writeSaveFile("load_ok.txt", content);

    LoadBoard loader = new TestableLoadBoard();
    loader.loadGameData("load_ok.txt");

    GameCheckers game = loader.getLoadedGame();
    assertNotNull(game);

    Board board = game.getBoard();

    assertFalse(game.isWhiteTurn());
    assertTrue(board.isWhitePawn("A1"));
    assertTrue(board.isBlackPawn("B2"));
    assertTrue(board.isWhiteChecker("C3"));
    assertTrue(board.isBlackChecker("D4"));
  }

  @Test
  void testLoadRejectsPieceOnNonPlayableSquare() throws IOException {
    int n = 10;

    StringBuilder bad = new StringBuilder();
    for (int row = n - 1; row >= 0; row--) {
      for (int col = 0; col < n; col++) {
        char c = '_';
        if (row == 0 && col == 1) {
          c = 'x';
        }
        bad.append(c);
        if (col < n - 1) {
          bad.append(" ");
        }
      }
      bad.append("\n");
    }

    String content = "[settings]\n"
        + "starting-player=white\n"
        + "board-size=" + n + "\n"
        + "time-mode=classic\n"
        + "verbose=false\n"
        + "debug=false\n"
        + "ai-mode=none\n"
        + "\n"
        + "[game]\n"
        + bad
        + "\n"
        + "[history]\n";

    writeSaveFile("load_bad_square.txt", content);

    LoadBoard loader = new TestableLoadBoard();
    assertThrows(IllegalStateException.class, () -> loader.loadGameData("load_bad_square.txt"));
    assertNull(loader.getLoadedGame());
    assertNull(loader.getLoadedConfiguration());
  }

  @Test
  void testLoadFailsIfBoardSizeInvalid() throws IOException {
    int realSize = 10;

    String content = "[settings]\n"
        + "starting-player=white\n"
        + "board-size=9\n"
        + "time-mode=classic\n"
        + "verbose=false\n"
        + "debug=false\n"
        + "ai-mode=none\n"
        + "\n"
        + "[game]\n"
        + "_ _ _ _ _ _ _ _ _ _\n".repeat(realSize)
        + "\n"
        + "[history]\n";

    writeSaveFile("load_bad_size.txt", content);

    LoadBoard loader = new TestableLoadBoard();
    assertThrows(IllegalStateException.class, () -> loader.loadGameData("load_bad_size.txt"));
    assertNull(loader.getLoadedGame());
    assertNull(loader.getLoadedConfiguration());
  }

  @Test
  void testLoadFailsIfMissingGameSection() throws IOException {
    int n = 10;

    String content = "[settings]\n"
        + "starting-player=white\n"
        + "board-size=" + n + "\n"
        + "time-mode=classic\n"
        + "verbose=false\n"
        + "debug=false\n"
        + "ai-mode=none\n"
        + "\n"
        + "[history]\n";

    writeSaveFile("load_missing_game.txt", content);

    LoadBoard loader = new TestableLoadBoard();
    assertThrows(IllegalStateException.class, () -> loader.loadGameData("load_missing_game.txt"));
    assertNull(loader.getLoadedGame());
    assertNull(loader.getLoadedConfiguration());
  }

  @Test
  void testLoadFailsIfMissingSettingsSection() throws IOException {
    int n = 10;

    String content = "[game]\n"
        + "_ _ _ _ _ _ _ _ _ _\n".repeat(n)
        + "\n"
        + "[history]\n";

    writeSaveFile("load_missing_settings.txt", content);

    LoadBoard loader = new TestableLoadBoard();
    assertThrows(IllegalStateException.class, () -> loader.loadGameData("load_missing_settings.txt"));
    assertNull(loader.getLoadedGame());
    assertNull(loader.getLoadedConfiguration());
  }

  @Test
  void testLoadFailsIfMissingHistorySection() throws IOException {
    int n = 10;

    Map<String, Character> pieces = new HashMap<>();
    pieces.put("A1", 'o');

    String content = "[settings]\n"
        + "starting-player=white\n"
        + "board-size=" + n + "\n"
        + "time-mode=classic\n"
        + "verbose=false\n"
        + "debug=false\n"
        + "ai-mode=none\n"
        + "\n"
        + "[game]\n"
        + buildBoardAscii(n, pieces);

    writeSaveFile("load_missing_history.txt", content);

    LoadBoard loader = new TestableLoadBoard();
    assertThrows(IllegalStateException.class, () -> loader.loadGameData("load_missing_history.txt"));
    assertNull(loader.getLoadedGame());
    assertNull(loader.getLoadedConfiguration());
  }

  @Test
  void testLoadRestoresWhiteTurn() throws IOException {
    int n = 10;

    Map<String, Character> pieces = new HashMap<>();
    pieces.put("A1", 'o');

    String content = "[settings]\n"
        + "starting-player=white\n"
        + "board-size=" + n + "\n"
        + "time-mode=classic\n"
        + "verbose=false\n"
        + "debug=false\n"
        + "ai-mode=none\n"
        + "\n"
        + "[game]\n"
        + buildBoardAscii(n, pieces)
        + "\n"
        + "[history]\n";

    writeSaveFile("load_white_turn.txt", content);

    LoadBoard loader = new TestableLoadBoard();
    loader.loadGameData("load_white_turn.txt");

    GameCheckers game = loader.getLoadedGame();
    assertNotNull(game);
    assertTrue(game.isWhiteTurn());
  }

  @Test
  void testLoadEmptyBoardFileKeepsBoardEmpty() throws IOException {
    int n = 10;

    String content = "[settings]\n"
        + "starting-player=white\n"
        + "board-size=" + n + "\n"
        + "time-mode=classic\n"
        + "verbose=false\n"
        + "debug=false\n"
        + "ai-mode=none\n"
        + "\n"
        + "[game]\n"
        + "_ _ _ _ _ _ _ _ _ _\n".repeat(n)
        + "\n"
        + "[history]\n";

    writeSaveFile("load_empty_board.txt", content);

    LoadBoard loader = new TestableLoadBoard();
    loader.loadGameData("load_empty_board.txt");

    GameCheckers game = loader.getLoadedGame();
    assertNotNull(game);

    Board board = game.getBoard();
    assertBoardEmpty(board);
  }

  @Test
  void testLoadRestoresBlitzVerboseDebugAndWhiteAiConfig() throws IOException {
    int n = 10;

    String settings = "[settings]\n"
        + "starting-player=black\n"
        + "board-size=" + n + "\n"
        + "time-mode=blitz\n"
        + "verbose=true\n"
        + "debug=true\n"
        + "ai-mode=white-minimax\n"
        + "ai-depth=4\n"
        + "ai-time=1500\n";

    String content = fullSave(settings, initialBoardAscii(n), "");

    writeSaveFile("load_white_ai_ok.txt", content);

    LoadBoard loader = new TestableLoadBoard();
    loader.loadGameData("load_white_ai_ok.txt");

    GameCheckers game = loader.getLoadedGame();
    Configuration config = loader.getLoadedConfiguration();

    assertNotNull(game);
    assertNotNull(config);
    assertFalse(game.isWhiteTurn());
    assertTrue(config.isBlitz());
    assertTrue(config.isVerbose());
    assertTrue(config.isDebug());
    assertTrue(config.iswhiteAi());
    assertFalse(config.isblackAi());
    assertEquals("minimax", config.getAiMode());
    assertEquals(4, config.getAiDepth());
    assertEquals(1500L, config.getAiTime());
    assertEquals(n, config.getSize());
  }

  @Test
  void testLoadRestoresBlackAiConfig() throws IOException {
    int n = 10;

    String settings = "[settings]\n"
        + "starting-player=white\n"
        + "board-size=" + n + "\n"
        + "time-mode=classic\n"
        + "verbose=false\n"
        + "debug=false\n"
        + "ai-mode=black-alphabeta\n"
        + "ai-depth=5\n"
        + "ai-time=2000\n";

    String content = fullSave(settings, initialBoardAscii(n), "");

    writeSaveFile("load_black_ai_ok.txt", content);

    LoadBoard loader = new TestableLoadBoard();
    loader.loadGameData("load_black_ai_ok.txt");

    Configuration config = loader.getLoadedConfiguration();
    assertNotNull(config);
    assertFalse(config.iswhiteAi());
    assertTrue(config.isblackAi());
    assertEquals("alphabeta", config.getAiMode());
    assertEquals(5, config.getAiDepth());
    assertEquals(2000L, config.getAiTime());
  }

  @Test
  void testLoadSupportsInlineAndBlockComments() throws IOException {
    int n = 10;

    String content = "[settings] # settings section\n"
        + "starting-player=white # inline comment\n"
        + "board-size=" + n + " {block comment}\n"
        + "time-mode=classic\n"
        + "verbose=false\n"
        + "debug=false\n"
        + "ai-mode=none\n"
        + "\n"
        + "[game]\n"
        + initialBoardAscii(n)
        + "\n"
        + "[history]\n";

    writeSaveFile("load_comments_ok.txt", content);

    LoadBoard loader = new TestableLoadBoard();
    loader.loadGameData("load_comments_ok.txt");

    assertNotNull(loader.getLoadedGame());
    assertNotNull(loader.getLoadedConfiguration());
  }

  @Test
  void testLoadFailsIfStartingPlayerInvalid() throws IOException {
    int n = 10;

    String settings = "[settings]\n"
        + "starting-player=blue\n"
        + "board-size=" + n + "\n"
        + "time-mode=classic\n"
        + "verbose=false\n"
        + "debug=false\n"
        + "ai-mode=none\n";

    String content = fullSave(settings, initialBoardAscii(n), "");

    writeSaveFile("load_bad_starting_player.txt", content);

    LoadBoard loader = new TestableLoadBoard();
    assertThrows(IllegalStateException.class, () -> loader.loadGameData("load_bad_starting_player.txt"));
    assertNull(loader.getLoadedGame());
    assertNull(loader.getLoadedConfiguration());
  }

  @Test
  void testLoadFailsIfTimeModeInvalid() throws IOException {
    int n = 10;

    String settings = "[settings]\n"
        + "starting-player=white\n"
        + "board-size=" + n + "\n"
        + "time-mode=fast\n"
        + "verbose=false\n"
        + "debug=false\n"
        + "ai-mode=none\n";

    String content = fullSave(settings, initialBoardAscii(n), "");

    writeSaveFile("load_bad_time_mode.txt", content);

    LoadBoard loader = new TestableLoadBoard();
    assertThrows(IllegalStateException.class, () -> loader.loadGameData("load_bad_time_mode.txt"));
    assertNull(loader.getLoadedGame());
    assertNull(loader.getLoadedConfiguration());
  }

  @Test
  void testLoadFailsIfDebugValueInvalid() throws IOException {
    int n = 10;

    String settings = "[settings]\n"
        + "starting-player=white\n"
        + "board-size=" + n + "\n"
        + "time-mode=classic\n"
        + "verbose=false\n"
        + "debug=maybe\n"
        + "ai-mode=none\n";

    String content = fullSave(settings, initialBoardAscii(n), "");

    writeSaveFile("load_bad_debug.txt", content);

    LoadBoard loader = new TestableLoadBoard();
    assertThrows(IllegalStateException.class, () -> loader.loadGameData("load_bad_debug.txt"));
    assertNull(loader.getLoadedGame());
    assertNull(loader.getLoadedConfiguration());
  }

  @Test
  void testLoadFailsIfVerboseValueInvalid() throws IOException {
    int n = 10;

    String settings = "[settings]\n"
        + "starting-player=white\n"
        + "board-size=" + n + "\n"
        + "time-mode=classic\n"
        + "verbose=maybe\n"
        + "debug=false\n"
        + "ai-mode=none\n";

    String content = fullSave(settings, initialBoardAscii(n), "");

    writeSaveFile("load_bad_verbose.txt", content);

    LoadBoard loader = new TestableLoadBoard();
    assertThrows(IllegalStateException.class, () -> loader.loadGameData("load_bad_verbose.txt"));
    assertNull(loader.getLoadedGame());
    assertNull(loader.getLoadedConfiguration());
  }

  @Test
  void testLoadFailsIfAiModeInvalid() throws IOException {
    int n = 10;

    String settings = "[settings]\n"
        + "starting-player=white\n"
        + "board-size=" + n + "\n"
        + "time-mode=classic\n"
        + "verbose=false\n"
        + "debug=false\n"
        + "ai-mode=robot-minimax\n";

    String content = fullSave(settings, initialBoardAscii(n), "");

    writeSaveFile("load_bad_ai_mode.txt", content);

    LoadBoard loader = new TestableLoadBoard();
    assertThrows(IllegalStateException.class, () -> loader.loadGameData("load_bad_ai_mode.txt"));
    assertNull(loader.getLoadedGame());
    assertNull(loader.getLoadedConfiguration());
  }

  @Test
  void testLoadFailsIfAiAlgorithmUnknown() throws IOException {
    int n = 10;

    String settings = "[settings]\n"
        + "starting-player=white\n"
        + "board-size=" + n + "\n"
        + "time-mode=classic\n"
        + "verbose=false\n"
        + "debug=false\n"
        + "ai-mode=white-greedy\n"
        + "ai-depth=3\n"
        + "ai-time=1000\n";

    String content = fullSave(settings, initialBoardAscii(n), "");

    writeSaveFile("load_bad_ai_algo.txt", content);

    LoadBoard loader = new TestableLoadBoard();
    assertThrows(IllegalStateException.class, () -> loader.loadGameData("load_bad_ai_algo.txt"));
    assertNull(loader.getLoadedGame());
    assertNull(loader.getLoadedConfiguration());
  }

  @Test
  void testLoadFailsIfActiveAiDepthIsZero() throws IOException {
    int n = 10;

    String settings = "[settings]\n"
        + "starting-player=white\n"
        + "board-size=" + n + "\n"
        + "time-mode=classic\n"
        + "verbose=false\n"
        + "debug=false\n"
        + "ai-mode=white-minimax\n"
        + "ai-depth=0\n"
        + "ai-time=1000\n";

    String content = fullSave(settings, initialBoardAscii(n), "");

    writeSaveFile("load_ai_depth_zero.txt", content);

    LoadBoard loader = new TestableLoadBoard();
    assertThrows(IllegalStateException.class, () -> loader.loadGameData("load_ai_depth_zero.txt"));
    assertNull(loader.getLoadedGame());
    assertNull(loader.getLoadedConfiguration());
  }

  @Test
  void testLoadFailsIfActiveAiTimeIsZero() throws IOException {
    int n = 10;

    String settings = "[settings]\n"
        + "starting-player=white\n"
        + "board-size=" + n + "\n"
        + "time-mode=classic\n"
        + "verbose=false\n"
        + "debug=false\n"
        + "ai-mode=black-mcts\n"
        + "ai-depth=3\n"
        + "ai-time=0\n";

    String content = fullSave(settings, initialBoardAscii(n), "");

    writeSaveFile("load_ai_time_zero.txt", content);

    LoadBoard loader = new TestableLoadBoard();
    assertThrows(IllegalStateException.class, () -> loader.loadGameData("load_ai_time_zero.txt"));
    assertNull(loader.getLoadedGame());
    assertNull(loader.getLoadedConfiguration());
  }

  @Test
  void testLoadFailsIfAiDepthNegative() throws IOException {
    int n = 10;

    String settings = "[settings]\n"
        + "starting-player=white\n"
        + "board-size=" + n + "\n"
        + "time-mode=classic\n"
        + "verbose=false\n"
        + "debug=false\n"
        + "ai-mode=none\n"
        + "ai-depth=-1\n";

    String content = fullSave(settings, initialBoardAscii(n), "");

    writeSaveFile("load_ai_depth_negative.txt", content);

    LoadBoard loader = new TestableLoadBoard();
    assertThrows(IllegalStateException.class, () -> loader.loadGameData("load_ai_depth_negative.txt"));
    assertNull(loader.getLoadedGame());
    assertNull(loader.getLoadedConfiguration());
  }

  @Test
  void testLoadFailsIfAiDepthTooLarge() throws IOException {
    int n = 10;

    String settings = "[settings]\n"
        + "starting-player=white\n"
        + "board-size=" + n + "\n"
        + "time-mode=classic\n"
        + "verbose=false\n"
        + "debug=false\n"
        + "ai-mode=none\n"
        + "ai-depth=15\n";

    String content = fullSave(settings, initialBoardAscii(n), "");

    writeSaveFile("load_ai_depth_too_large.txt", content);

    LoadBoard loader = new TestableLoadBoard();
    assertThrows(IllegalStateException.class, () -> loader.loadGameData("load_ai_depth_too_large.txt"));
    assertNull(loader.getLoadedGame());
    assertNull(loader.getLoadedConfiguration());
  }

  @Test
  void testLoadFailsIfAiTimeNegative() throws IOException {
    int n = 10;

    String settings = "[settings]\n"
        + "starting-player=white\n"
        + "board-size=" + n + "\n"
        + "time-mode=classic\n"
        + "verbose=false\n"
        + "debug=false\n"
        + "ai-mode=none\n"
        + "ai-time=-5\n";

    String content = fullSave(settings, initialBoardAscii(n), "");

    writeSaveFile("load_ai_time_negative.txt", content);

    LoadBoard loader = new TestableLoadBoard();
    assertThrows(IllegalStateException.class, () -> loader.loadGameData("load_ai_time_negative.txt"));
    assertNull(loader.getLoadedGame());
    assertNull(loader.getLoadedConfiguration());
  }

  @Test
  void testLoadFailsIfBoardCharacterInvalid() throws IOException {
    int n = 10;

    StringBuilder bad = new StringBuilder();
    for (int row = n - 1; row >= 0; row--) {
      for (int col = 0; col < n; col++) {
        char c = '_';
        if (row == 0 && col == 0) {
          c = 'z';
        }
        bad.append(c);
        if (col < n - 1) {
          bad.append(" ");
        }
      }
      bad.append("\n");
    }

    String content = fullSave(baseSettings(n), bad.toString(), "");

    writeSaveFile("load_bad_char.txt", content);

    LoadBoard loader = new TestableLoadBoard();
    assertThrows(IllegalStateException.class, () -> loader.loadGameData("load_bad_char.txt"));
    assertNull(loader.getLoadedGame());
    assertNull(loader.getLoadedConfiguration());
  }

  @Test
  void testLoadFailsIfBoardRowHasWrongLength() throws IOException {
    int n = 10;

    StringBuilder board = new StringBuilder();
    for (int row = n - 1; row >= 0; row--) {
      if (row == n - 1) {
        board.append("_ _ _ _ _ _ _ _ _\n");
      } else {
        board.append("_ _ _ _ _ _ _ _ _ _\n");
      }
    }

    String content = fullSave(baseSettings(n), board.toString(), "");

    writeSaveFile("load_bad_row_length.txt", content);

    LoadBoard loader = new TestableLoadBoard();
    assertThrows(IllegalStateException.class, () -> loader.loadGameData("load_bad_row_length.txt"));
    assertNull(loader.getLoadedGame());
    assertNull(loader.getLoadedConfiguration());
  }

  @Test
  void testLoadFailsIfBoardIncomplete() throws IOException {
    int n = 10;

    String board = "_ _ _ _ _ _ _ _ _ _\n".repeat(n - 1);

    String content = fullSave(baseSettings(n), board, "");

    writeSaveFile("load_incomplete_board.txt", content);

    LoadBoard loader = new TestableLoadBoard();
    assertThrows(IllegalStateException.class, () -> loader.loadGameData("load_incomplete_board.txt"));
    assertNull(loader.getLoadedGame());
    assertNull(loader.getLoadedConfiguration());
  }
}