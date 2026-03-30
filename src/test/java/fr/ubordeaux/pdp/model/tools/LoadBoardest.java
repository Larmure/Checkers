package fr.ubordeaux.pdp.model.tools;

import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.model.player.PlayerColor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import fr.ubordeaux.pdp.model.player.ai.Ai;
import fr.ubordeaux.pdp.model.player.ai.Mcts;

public class LoadBoardest {

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

  private GameCheckers newGame(int n) {
    Configuration cfg = new Configuration(
        Utils.DEFAULT_BLITZ,
        Utils.DEFAULT_TIME,
        Utils.DEFAULT_CONTEST,
        n,
        Utils.DEFAULT_VERBOSE,
        Utils.DEFAULT_DEBUG,
        Utils.DEFAULT_WHITE_AI,
        Utils.DEFAULT_BLACK_AI,
        Ai.DEFAULT_MAX_TIME_MS,
        Utils.DEFAULT_AI_MODE,
        Ai.DEFAULT_DEPTH,
        Mcts.DEFAULT_SELECTION_MODE);
    return new GameCheckers(cfg);
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

    String content = "[settings]\n"
        + "starting-player=black\n"
        + "board-size="
        + n
        + "\n"
        + "\n"
        + "[game]\n"
        + buildBoardAscii(n, pieces)
        + "\n"
        + "[history]\n";

    writeSaveFile("load_ok.txt", content);

    GameCheckers game = newGame(n);
    Board board = game.getBoard();

    new LoadBoard(game).loadGameData("load_ok.txt");

    assertFalse(game.isWhiteTurn());

    assertTrue(board.isWhitePawn("A1"));
    assertTrue(board.isBlackPawn("B2"));
    assertTrue(board.isWhiteChecker("C3"));
    assertTrue(board.isBlackChecker("D4"));
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
          c = 'x'; // non playable (A2)
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
        + "board-size="
        + n
        + "\n"
        + "\n"
        + "[game]\n"
        + bad
        + "\n"
        + "[history]\n";

    writeSaveFile("load_bad_square.txt", content);

    GameCheckers game = newGame(n);
    Board board = game.getBoard();

    new LoadBoard(game).loadGameData("load_bad_square.txt");

    assertBoardEmpty(board);
  }

  // --------------------------------------------------------------------
  // ERROR: Board size mismatch (erreur dans settings -> board inchangé)
  // --------------------------------------------------------------------

  @Test
  void testLoadFailsIfBoardSizeMismatch_boardUnchanged() throws IOException {
    int realSize = 10;

    String content = "[settings]\n"
        + "starting-player=white\n"
        + "board-size=12\n"
        + "\n"
        + "[game]\n"
        + "- - - - - - - - - -\n".repeat(realSize)
        + "\n"
        + "[history]\n";

    writeSaveFile("load_bad_size.txt", content);

    GameCheckers game = newGame(realSize);
    Board board = game.getBoard();

    // état initial normal: les deux couleurs ont des pièces
    assertFalse(board.noPiecesLeft(PlayerColor.WHITE));
    assertFalse(board.noPiecesLeft(PlayerColor.BLACK));

    new LoadBoard(game).loadGameData("load_bad_size.txt");

    // inchangé: toujours des pièces des deux côtés
    assertFalse(board.noPiecesLeft(PlayerColor.WHITE));
    assertFalse(board.noPiecesLeft(PlayerColor.BLACK));
  }

  // --------------------------------------------------------------------
  // ERROR: Missing [game] section (strict mode -> inchangé)
  // --------------------------------------------------------------------

  @Test
  void testLoadFailsIfMissingGameSection_boardUnchanged() throws IOException {
    int n = 10;

    String content = "[settings]\n" + "starting-player=white\n" + "board-size=" + n + "\n" + "\n" + "[history]\n";

    writeSaveFile("load_missing_game.txt", content);

    GameCheckers game = newGame(n);
    Board board = game.getBoard();

    assertFalse(board.noPiecesLeft(PlayerColor.WHITE));
    assertFalse(board.noPiecesLeft(PlayerColor.BLACK));

    new LoadBoard(game).loadGameData("load_missing_game.txt");

    assertFalse(board.noPiecesLeft(PlayerColor.WHITE));
    assertFalse(board.noPiecesLeft(PlayerColor.BLACK));
  }

  // --------------------------------------------------------------------
  // ERROR: Missing [settings] section (board reset car entre dans [game])
  // --------------------------------------------------------------------

  @Test
  void testLoadFailsIfMissingSettingsSection_boardReset() throws IOException {
    int n = 10;

    String content = "[game]\n" + "- - - - - - - - - -\n".repeat(n) + "\n" + "[history]\n";

    writeSaveFile("load_missing_settings.txt", content);

    GameCheckers game = newGame(n);
    Board board = game.getBoard();

    new LoadBoard(game).loadGameData("load_missing_settings.txt");

    assertBoardEmpty(board);
  }

  // --------------------------------------------------------------------
  // ERROR: Missing [history] section (board chargé mais erreur finale)
  // --------------------------------------------------------------------

  @Test
  void testLoadFailsIfMissingHistorySection_boardLoaded() throws IOException {
    int n = 10;

    Map<String, Character> pieces = new HashMap<>();
    pieces.put("A1", 'o');

    String content = "[settings]\n"
        + "starting-player=white\n"
        + "board-size="
        + n
        + "\n"
        + "\n"
        + "[game]\n"
        + buildBoardAscii(n, pieces);

    writeSaveFile("load_missing_history.txt", content);

    GameCheckers game = newGame(n);
    Board board = game.getBoard();

    new LoadBoard(game).loadGameData("load_missing_history.txt");

    assertTrue(board.isWhitePawn("A1"));
  }
  @Test
  void testLoadRestoresWhiteTurn() throws IOException {
    int n = 10;

    Map<String, Character> pieces = new HashMap<>();
    pieces.put("A1", 'o');

    String content = "[settings]\n"
        + "starting-player=white\n"
        + "board-size=" + n + "\n"
        + "\n"
        + "[game]\n"
        + buildBoardAscii(n, pieces)
        + "\n"
        + "[history]\n";

    writeSaveFile("load_white_turn.txt", content);

    GameCheckers game = newGame(n);

    new LoadBoard(game).loadGameData("load_white_turn.txt");

    assertTrue(game.isWhiteTurn());
  }

  @Test
  void testLoadMissingHistorySectionWithSeveralPiecesStillLoadsBoard() throws IOException {
    int n = 10;

    Map<String, Character> pieces = new HashMap<>();
    pieces.put("A1", 'o');
    pieces.put("B2", 'x');
    pieces.put("C3", 'O');
    pieces.put("D4", 'X');

    String content = "[settings]\n"
        + "starting-player=white\n"
        + "board-size=" + n + "\n"
        + "\n"
        + "[game]\n"
        + buildBoardAscii(n, pieces);

    writeSaveFile("load_missing_history_many_pieces.txt", content);

    GameCheckers game = newGame(n);
    Board board = game.getBoard();

    new LoadBoard(game).loadGameData("load_missing_history_many_pieces.txt");

    assertTrue(board.isWhitePawn("A1"));
    assertTrue(board.isBlackPawn("B2"));
    assertTrue(board.isWhiteChecker("C3"));
    assertTrue(board.isBlackChecker("D4"));
  }
  @Test
  void testLoadEmptyBoardFileKeepsBoardEmpty() throws IOException {
    int n = 10;

    String content = "[settings]\n"
        + "starting-player=white\n"
        + "board-size=" + n + "\n"
        + "\n"
        + "[game]\n"
        + "- - - - - - - - - -\n".repeat(n)
        + "\n"
        + "[history]\n";

    writeSaveFile("load_empty_board.txt", content);

    GameCheckers game = newGame(n);
    Board board = game.getBoard();

    new LoadBoard(game).loadGameData("load_empty_board.txt");

    assertBoardEmpty(board);
  }


}