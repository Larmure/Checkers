package fr.ubordeaux.pdp.model.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;

import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.core.GameCheckers;

public class BoardChargementTest {

  private final String SAVE_DIR = "Sauvegarde";

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
    for (int row = 0; row < n; row++) {
      for (int col = 0; col < n; col++) {
        String sq = "" + (char) ('A' + row) + (col + 1);
        char c = pieces.getOrDefault(sq, '-');
        sb.append(c);
        if (col < n - 1) {
          sb.append(" ");
        }
      }
      sb.append("\n");
    }
    return sb.toString();
  }

  private long[] snapshot(Board b) {
    return new long[] {
        b.getWhitePawns1(), b.getWhitePawns2(),
        b.getBlackPawns1(), b.getBlackPawns2(),
        b.getWhiteCheckers1(), b.getWhiteCheckers2(),
        b.getBlackCheckers1(), b.getBlackCheckers2()
    };
  }

  private GameCheckers newGame(int n) {
    Configuration cfg = new Configuration(
        Utils.DEFAULT_BLITZ,
        Utils.DEFAULT_TIME,
        Utils.DEFAULT_CONTEST,
        n,
        Utils.DEFAULT_VERBOSE,
        Utils.DEFAULT_DEBUG,
        Utils.DEFAULT_WHITE_AI,
        Utils.DEFAULT_BLACK_AI
    );
    return new GameCheckers(cfg);
  }

  /**
   * Calcule l'index "playable" utilisé par BoardChargement :
   * index = (row * n + col) / 2, uniquement si case jouable (row+col)%2==0
   */
  private int playableIndex(int n, String square) {
    int row = Character.toUpperCase(square.charAt(0)) - 'A';
    int col = Integer.parseInt(square.substring(1)) - 1;

    if (((row + col) % 2) != 0) {
      throw new IllegalArgumentException("Non-playable square: " + square);
    }
    return (row * n + col) / 2;
  }

  private long maskForPlayableIndex(int playableIndex) {
    return 1L << (playableIndex % 64);
  }

  // --------------------------------------------------------------------
  // SUCCESS CASE
  // --------------------------------------------------------------------

  @Test
  void testLoadRestoresTurnAndKings() throws IOException {
    int n = 10;

    Map<String, Character> pieces = new HashMap<>();
    pieces.put("A1", 'o');
    pieces.put("B2", 'x');
    pieces.put("C3", 'O');
    pieces.put("D4", 'X');

    String content =
        "[settings]\n" +
        "starting-player=black\n" +
        "board-size=" + n + "\n" +
        "\n" +
        "[game]\n" +
        buildBoardAscii(n, pieces) +
        "\n" +
        "[history]\n";

    writeSaveFile("load_ok.txt", content);

    GameCheckers game = newGame(n);
    Board board = game.getBoard();

    new BoardChargement(board, game).loadFromFile("load_ok.txt");

    assertFalse(game.isWhiteTurn());

    int a1 = playableIndex(n, "A1");
    int b2 = playableIndex(n, "B2");
    int c3 = playableIndex(n, "C3");
    int d4 = playableIndex(n, "D4");

    assertEquals(maskForPlayableIndex(a1), board.getWhitePawns1());
    assertEquals(maskForPlayableIndex(b2), board.getBlackPawns1());
    assertEquals(maskForPlayableIndex(c3), board.getWhiteCheckers1());
    assertEquals(maskForPlayableIndex(d4), board.getBlackCheckers1());
  }

  // --------------------------------------------------------------------
  // ERROR: Non playable square
  // --------------------------------------------------------------------

  @Test
  void testLoadRejectsPieceOnNonPlayableSquare_boardReset() throws IOException {
    int n = 10;

    StringBuilder bad = new StringBuilder();
    for (int row = 0; row < n; row++) {
      for (int col = 0; col < n; col++) {
        char c = '-';
        if (row == 0 && col == 1) {
          c = 'x'; // non playable (car A2 => (0+1)%2==1)
        }
        bad.append(c);
        if (col < n - 1) {
          bad.append(" ");
        }
      }
      bad.append("\n");
    }

    String content =
        "[settings]\n" +
        "starting-player=white\n" +
        "board-size=" + n + "\n" +
        "\n" +
        "[game]\n" +
        bad +
        "\n" +
        "[history]\n";

    writeSaveFile("load_bad_square.txt", content);

    GameCheckers game = newGame(n);
    Board b = game.getBoard();

    new BoardChargement(b, game).loadFromFile("load_bad_square.txt");

    assertEquals(0L, b.getWhitePawns1());
    assertEquals(0L, b.getBlackPawns1());
    assertEquals(0L, b.getWhiteCheckers1());
    assertEquals(0L, b.getBlackCheckers1());
  }

  // --------------------------------------------------------------------
  // ERROR: Board size mismatch (erreur dans settings -> board inchangé)
  // --------------------------------------------------------------------

  @Test
  void testLoadFailsIfBoardSizeMismatch_boardUnchanged() throws IOException {
    int realSize = 10;

    String content =
        "[settings]\n" +
        "starting-player=white\n" +
        "board-size=12\n" +
        "\n" +
        "[game]\n" +
        "- - - - - - - - - -\n".repeat(realSize) +
        "\n" +
        "[history]\n";

    writeSaveFile("load_bad_size.txt", content);

    GameCheckers game = newGame(realSize);
    Board b = game.getBoard();

    long[] before = snapshot(b);

    new BoardChargement(b, game).loadFromFile("load_bad_size.txt");

    assertArrayEquals(before, snapshot(b));
  }

  // --------------------------------------------------------------------
  // ERROR: Missing [game] section (strict mode -> inchangé)
  // --------------------------------------------------------------------

  @Test
  void testLoadFailsIfMissingGameSection_boardUnchanged() throws IOException {
    int n = 10;

    String content =
        "[settings]\n" +
        "starting-player=white\n" +
        "board-size=" + n + "\n" +
        "\n" +
        "[history]\n";

    writeSaveFile("load_missing_game.txt", content);

    GameCheckers game = newGame(n);
    Board b = game.getBoard();

    long[] before = snapshot(b);

    new BoardChargement(b, game).loadFromFile("load_missing_game.txt");

    assertArrayEquals(before, snapshot(b));
  }

  // --------------------------------------------------------------------
  // ERROR: Missing [settings] section (board reset car entre dans [game])
  // --------------------------------------------------------------------

  @Test
  void testLoadFailsIfMissingSettingsSection_boardReset() throws IOException {
    int n = 10;

    String content =
        "[game]\n" +
        "- - - - - - - - - -\n".repeat(n) +
        "\n" +
        "[history]\n";

    writeSaveFile("load_missing_settings.txt", content);

    GameCheckers game = newGame(n);
    Board b = game.getBoard();

    new BoardChargement(b, game).loadFromFile("load_missing_settings.txt");

    assertEquals(0L, b.getWhitePawns1());
    assertEquals(0L, b.getBlackPawns1());
  }

  // --------------------------------------------------------------------
  // ERROR: Missing [history] section (board chargé mais erreur finale)
  // --------------------------------------------------------------------

  @Test
  void testLoadFailsIfMissingHistorySection_boardLoaded() throws IOException {
    int n = 10;

    Map<String, Character> pieces = new HashMap<>();
    pieces.put("A1", 'o');

    String content =
        "[settings]\n" +
        "starting-player=white\n" +
        "board-size=" + n + "\n" +
        "\n" +
        "[game]\n" +
        buildBoardAscii(n, pieces);

    writeSaveFile("load_missing_history.txt", content);

    GameCheckers game = newGame(n);
    Board b = game.getBoard();

    new BoardChargement(b, game).loadFromFile("load_missing_history.txt");

    int a1 = playableIndex(n, "A1");
    assertEquals(maskForPlayableIndex(a1), b.getWhitePawns1());
  }
}