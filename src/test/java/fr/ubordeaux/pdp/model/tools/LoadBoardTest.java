package fr.ubordeaux.pdp.model.tools;

import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoadBoardTest {

  private static final String SAVE_DIR = "Sauvegarde";

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

    LoadBoard loader = new LoadBoard();
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

    LoadBoard loader = new LoadBoard();
    loader.loadGameData("load_bad_square.txt");

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

    LoadBoard loader = new LoadBoard();
    loader.loadGameData("load_bad_size.txt");

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

    LoadBoard loader = new LoadBoard();
    loader.loadGameData("load_missing_game.txt");

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

    LoadBoard loader = new LoadBoard();
    loader.loadGameData("load_missing_settings.txt");

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

    LoadBoard loader = new LoadBoard();
    loader.loadGameData("load_missing_history.txt");

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

    LoadBoard loader = new LoadBoard();
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

    LoadBoard loader = new LoadBoard();
    loader.loadGameData("load_empty_board.txt");

    GameCheckers game = loader.getLoadedGame();
    assertNotNull(game);

    Board board = game.getBoard();
    assertBoardEmpty(board);
  }
}