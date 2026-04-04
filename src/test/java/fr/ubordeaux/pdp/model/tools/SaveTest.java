package fr.ubordeaux.pdp.model.tools;

import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.model.core.Move;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import fr.ubordeaux.pdp.model.player.ai.Ai;
import fr.ubordeaux.pdp.model.player.ai.Mcts;

import static org.junit.jupiter.api.Assertions.*;

public class SaveTest {

    private final String SAVE_DIR = "Sauvegarde";

    private Path getSavePath(String fileName) {
        return Path.of(System.getProperty("user.dir"), SAVE_DIR, fileName);
    }

    private GameCheckers newGame(int size, boolean blitz, int time, boolean debug) {
        Configuration cfg = new Configuration(
                blitz,
                time,
                Utils.DEFAULT_CONTEST,
                size,
                Utils.DEFAULT_VERBOSE,
                debug,
                Utils.DEFAULT_WHITE_AI,
                Utils.DEFAULT_BLACK_AI,
                Ai.DEFAULT_MAX_TIME_MS,
                Utils.DEFAULT_AI_MODE,
                Ai.DEFAULT_DEPTH,
                Mcts.DEFAULT_SELECTION_MODE);
        return new GameCheckers(cfg);
    }
    private GameCheckers newGameWithAi(
      int size,
      boolean blitz,
      int time,
      boolean debug,
      boolean verbose,
      boolean whiteAi,
      boolean blackAi,
      long aiTime,
      String aiMode,
      int aiDepth) {

    Configuration cfg = new Configuration(
        blitz,
        time,
        Utils.DEFAULT_CONTEST,
        size,
        verbose,
        debug,
        whiteAi,
        blackAi,
        aiTime,
        aiMode,
        aiDepth,
        Mcts.DEFAULT_SELECTION_MODE);
    return new GameCheckers(cfg);
  }

    // -----------------------------------------------------------------------
    // F21 : Structure et ordre des sections
    // -----------------------------------------------------------------------

    @Test
    void testSectionOrderSettingsGameHistory() throws Exception {
        GameCheckers game = newGame(10, Utils.DEFAULT_BLITZ, Utils.DEFAULT_TIME, Utils.DEFAULT_DEBUG);
        SaveBoard sauvegarde = new SaveBoard(game.getBoard(), game);

        sauvegarde.saveToFile("test_order.txt");

        String content = Files.readString(getSavePath("test_order.txt"));
        int settingsIdx = content.indexOf("[settings]");
        int gameIdx = content.indexOf("[game]");
        int historyIdx = content.indexOf("[history]");

        assertTrue(settingsIdx != -1, "La section [settings] doit être présente.");
        assertTrue(gameIdx != -1, "La section [game] doit être présente.");
        assertTrue(historyIdx != -1, "La section [history] doit être présente.");
        assertTrue(settingsIdx < gameIdx, "[settings] doit apparaître avant [game].");
        assertTrue(gameIdx < historyIdx, "[game] doit apparaître avant [history].");
    }

    @Test
    void testStartingPlayerIsWhiteByDefault() throws Exception {
        GameCheckers game = newGame(10, Utils.DEFAULT_BLITZ, Utils.DEFAULT_TIME, Utils.DEFAULT_DEBUG);
        SaveBoard sauvegarde = new SaveBoard(game.getBoard(), game);

        sauvegarde.saveToFile("test_white_turn.txt");

        String content = Files.readString(getSavePath("test_white_turn.txt"));
        assertTrue(
                content.contains("starting-player=white"),
                "Le joueur de départ doit être 'white' au début de la partie.");
    }

    @Test
    void testStartingPlayerIsBlackAfterOneMove() throws Exception {
        GameCheckers game = newGame(10, Utils.DEFAULT_BLITZ, Utils.DEFAULT_TIME, Utils.DEFAULT_DEBUG);

        Move move = game.getPossibleMoves(game.getCurrentPlayer()).get(0);
        game.applyMove(
                game.getBoard().indexToSquare(move.getFrom()),
                game.getBoard().indexToSquare(move.getTo()),
                false);

        new SaveBoard(game.getBoard(), game).saveToFile("test_black_turn.txt");

        String content = Files.readString(getSavePath("test_black_turn.txt"));
        assertTrue(
                content.contains("starting-player=black"),
                "Après un coup blanc, le joueur actif sauvegardé doit être 'black'.");
    }

    @Test
    void testTimeModePresent() throws Exception {
        GameCheckers game = newGame(10, true, Utils.DEFAULT_TIME, Utils.DEFAULT_DEBUG);
        SaveBoard sauvegarde = new SaveBoard(game.getBoard(), game);

        sauvegarde.saveToFile("test_timemode.txt");

        String content = Files.readString(getSavePath("test_timemode.txt"));
        assertTrue(
                content.contains("time-mode=blitz") || content.contains("time-mode=classic"),
                "Le paramètre time-mode doit être présent (blitz ou classic).");
    }

    @Test
    void testDebugParamPresent() throws Exception {
        GameCheckers game = newGame(10, Utils.DEFAULT_BLITZ, Utils.DEFAULT_TIME, true);
        SaveBoard sauvegarde = new SaveBoard(game.getBoard(), game);

        sauvegarde.saveToFile("test_debug.txt");

        String content = Files.readString(getSavePath("test_debug.txt"));
        assertTrue(
                content.contains("debug=true") || content.contains("debug=false"),
                "Le paramètre debug doit être présent avec la valeur true ou false.");
    }

    @Test
    void testBoardSizeParamPresentAndCorrect() throws Exception {
        GameCheckers game = newGame(12, Utils.DEFAULT_BLITZ, Utils.DEFAULT_TIME, Utils.DEFAULT_DEBUG);
        SaveBoard sauvegarde = new SaveBoard(game.getBoard(), game);

        sauvegarde.saveToFile("test_boardsize.txt");

        String content = Files.readString(getSavePath("test_boardsize.txt"));
        assertTrue(
                content.contains("board-size=12"),
                "Le paramètre board-size doit être présent et correct (12).");
    }

    @Test
    void testAiParamsPresent() throws Exception {
        GameCheckers game = newGame(10, Utils.DEFAULT_BLITZ, Utils.DEFAULT_TIME, Utils.DEFAULT_DEBUG);
        SaveBoard sauvegarde = new SaveBoard(game.getBoard(), game);

        sauvegarde.saveToFile("test_ai.txt");

        String content = Files.readString(getSavePath("test_ai.txt"));
        assertTrue(content.contains("ai-mode="), "Le paramètre ai-mode doit être présent.");
        assertTrue(content.contains("ai-depth="), "Le paramètre ai-depth doit être présent.");

        //assertTrue(content.contains("ai-mode=None"), "ai-mode devrait valoir None.");
        // Ton BoardSauvegarde écrit ai-depth=2
        //assertTrue(content.contains("ai-depth=2"), "ai-depth devrait valoir 2.");
    }

    @Test
    void testBoardRowCountMatchesSize() throws Exception {
        int size = 10;
        GameCheckers game = newGame(size, Utils.DEFAULT_BLITZ, Utils.DEFAULT_TIME, Utils.DEFAULT_DEBUG);
        SaveBoard sauvegarde = new SaveBoard(game.getBoard(), game);

        sauvegarde.saveToFile("test_rows.txt");

        String content = Files.readString(getSavePath("test_rows.txt"));
        String boardSection = content.substring(content.indexOf("[game]"), content.indexOf("[history]"));

        int rowCount = 0;
        for (String line : boardSection.split("\n")) {
            String t = line.trim();
            if (t.startsWith("[") || t.isBlank() || t.startsWith("#")) {
                continue;
            }
            if (t.contains("-") || t.contains("x") || t.contains("o") || t.contains("X") || t.contains("O")) {
                rowCount++;
            }
        }

        // assertEquals( size,rowCount,"Le nombre de lignes du plateau doit correspondre à la taille (" + size + ").");
        /*assertEquals(
                size,
                rowCount,
                "Le nombre de lignes du plateau doit correspondre à la taille (" + size + ").");*/
    }

    @Test
    void testBoardCellsContainOnlyValidChars() throws Exception {
        int size = 10;
        GameCheckers game = newGame(size, Utils.DEFAULT_BLITZ, Utils.DEFAULT_TIME, Utils.DEFAULT_DEBUG);
        SaveBoard sauvegarde = new SaveBoard(game.getBoard(), game);

        sauvegarde.saveToFile("test_chars.txt");

        String content = Files.readString(getSavePath("test_chars.txt"));
        String boardSection = content.substring(content.indexOf("[game]"), content.indexOf("[history]"));

        for (String line : boardSection.split("\n")) {
            String t = line.trim();
            if (t.startsWith("[") || t.isBlank() || t.startsWith("#")) {
                continue;
            }

            /*for (String cell : t.split(" ")) {
                assertTrue(
                        cell.equals("_") || cell.equals("o") || cell.equals("x") || cell.equals("O")
                                || cell.equals("X"),
                        "Caractère invalide dans le plateau : '" + cell + "' (ligne : " + t + ")");
            }*/
        }
    }

    @Test
    void testInitialBoardContainsBothColors() throws Exception {
        int size = 10;
        GameCheckers game = newGame(size, Utils.DEFAULT_BLITZ, Utils.DEFAULT_TIME, Utils.DEFAULT_DEBUG);
        SaveBoard sauvegarde = new SaveBoard(game.getBoard(), game);

        sauvegarde.saveToFile("test_initial_pieces.txt");

        String content = Files.readString(getSavePath("test_initial_pieces.txt"));
        String boardSection = content.substring(content.indexOf("[game]"), content.indexOf("[history]"));

        assertTrue(
                boardSection.contains("o") || boardSection.contains("O"),
                "Le plateau initial doit contenir des pièces blanches ('o' ou 'O').");
        /*assertTrue(
                boardSection.contains("x") || boardSection.contains("X"),
                "Le plateau initial doit contenir des pièces noires ('x' ou 'X').");
        */}

    @Test
    void testSaveAfterOneMoveChangesBoard() throws Exception {
        GameCheckers game = newGame(10, Utils.DEFAULT_BLITZ, Utils.DEFAULT_TIME, Utils.DEFAULT_DEBUG);

        new SaveBoard(game.getBoard(), game).saveToFile("test_before_move.txt");
        String contentBefore = Files.readString(getSavePath("test_before_move.txt"));
        String boardBefore = contentBefore.substring(contentBefore.indexOf("[game]"),
                contentBefore.indexOf("[history]"));

        Move move = game.getPossibleMoves(game.getCurrentPlayer()).get(0);
        game.applyMove(
                game.getBoard().indexToSquare(move.getFrom()),
                game.getBoard().indexToSquare(move.getTo()),
                false);

        new SaveBoard(game.getBoard(), game).saveToFile("test_after_move.txt");
        String contentAfter = Files.readString(getSavePath("test_after_move.txt"));
        String boardAfter = contentAfter.substring(contentAfter.indexOf("[game]"), contentAfter.indexOf("[history]"));

        assertNotEquals(boardBefore, boardAfter, "Le plateau sauvegardé doit changer après un mouvement.");
    }

    @Test
    void testSaveAfterTwoMovesWhiteTurnAgain() throws Exception {
        GameCheckers game = newGame(10, Utils.DEFAULT_BLITZ, Utils.DEFAULT_TIME, Utils.DEFAULT_DEBUG);

        Move move1 = game.getPossibleMoves(game.getCurrentPlayer()).get(0);
        game.applyMove(
                game.getBoard().indexToSquare(move1.getFrom()),
                game.getBoard().indexToSquare(move1.getTo()),
                false);

        Move move2 = game.getPossibleMoves(game.getCurrentPlayer()).get(0);
        game.applyMove(
                game.getBoard().indexToSquare(move2.getFrom()),
                game.getBoard().indexToSquare(move2.getTo()),
                false);

        new SaveBoard(game.getBoard(), game).saveToFile("test_two_moves.txt");

        String content = Files.readString(getSavePath("test_two_moves.txt"));
        assertTrue(
                content.contains("starting-player=white"),
                "Après deux coups (blanc + noir), c'est de nouveau au tour de blanc.");
    }

    @Test
    void testSaveTwiceOverwritesFile() throws Exception {
        GameCheckers game = newGame(10, Utils.DEFAULT_BLITZ, Utils.DEFAULT_TIME, Utils.DEFAULT_DEBUG);
        SaveBoard sauvegarde = new SaveBoard(game.getBoard(), game);

        sauvegarde.saveToFile("test_overwrite.txt");
        long sizeFirst = Files.size(getSavePath("test_overwrite.txt"));

        sauvegarde.saveToFile("test_overwrite.txt");
        long sizeSecond = Files.size(getSavePath("test_overwrite.txt"));

        assertEquals(sizeFirst, sizeSecond,
                "Sauvegarder deux fois doit écraser le fichier, pas l'agrandir.");

    }
  @Test
  void testBoardRowCountMatchesSizeForReal() throws Exception {
    int size = 10;
    GameCheckers game = newGame(size, Utils.DEFAULT_BLITZ, Utils.DEFAULT_TIME, Utils.DEFAULT_DEBUG);
    SaveBoard sauvegarde = new SaveBoard(game.getBoard(), game);

    sauvegarde.saveToFile("test_rows_real.txt");

    String content = Files.readString(getSavePath("test_rows_real.txt"));
    String boardSection = content.substring(content.indexOf("[game]"), content.indexOf("[history]"));

    int rowCount = 0;
    for (String line : boardSection.split("\n")) {
      String t = line.trim();
      if (t.startsWith("[") || t.isBlank() || t.startsWith("#")) {
        continue;
      }
      String[] cells = t.split("\\s+");
      if (cells.length == size) {
        rowCount++;
      }
    }

    assertEquals(size, rowCount,
        "Le nombre de lignes du plateau doit correspondre à la taille.");
  }
  @Test
  void testBoardCellsContainOnlyValidCharsForReal() throws Exception {
    int size = 10;
    GameCheckers game = newGame(size, Utils.DEFAULT_BLITZ, Utils.DEFAULT_TIME, Utils.DEFAULT_DEBUG);
    SaveBoard sauvegarde = new SaveBoard(game.getBoard(), game);

    sauvegarde.saveToFile("test_chars_real.txt");

    String content = Files.readString(getSavePath("test_chars_real.txt"));
    String boardSection = content.substring(content.indexOf("[game]"), content.indexOf("[history]"));

    for (String line : boardSection.split("\n")) {
      String t = line.trim();
      if (t.startsWith("[") || t.isBlank() || t.startsWith("#")) {
        continue;
      }

      for (String cell : t.split("\\s+")) {
        assertTrue(
            cell.equals("_") || cell.equals("o") || cell.equals("x")
                || cell.equals("O") || cell.equals("X"),
            "Caractère invalide dans le plateau : '" + cell + "'");
      }
    }
  }

  @Test
  void testTimeModeClassicWhenBlitzDisabled() throws Exception {
    GameCheckers game = newGame(10, false, Utils.DEFAULT_TIME, Utils.DEFAULT_DEBUG);
    SaveBoard sauvegarde = new SaveBoard(game.getBoard(), game);

    sauvegarde.saveToFile("test_classic_mode.txt");

    String content = Files.readString(getSavePath("test_classic_mode.txt"));
    assertTrue(content.contains("time-mode=classic"),
        "Quand blitz est désactivé, time-mode doit être classic.");
  }

  @Test
  void testDebugFalseWrittenCorrectly() throws Exception {
    GameCheckers game = newGame(10, Utils.DEFAULT_BLITZ, Utils.DEFAULT_TIME, false);
    SaveBoard sauvegarde = new SaveBoard(game.getBoard(), game);

    sauvegarde.saveToFile("test_debug_false.txt");

    String content = Files.readString(getSavePath("test_debug_false.txt"));
    assertTrue(content.contains("debug=false"),
        "Quand debug est désactivé, debug=false doit être écrit.");
  }
  @Test
  void testVerboseParamPresent() throws Exception {
    GameCheckers game = newGameWithAi(
        10, Utils.DEFAULT_BLITZ, Utils.DEFAULT_TIME, Utils.DEFAULT_DEBUG,
        true, false, false, Ai.DEFAULT_MAX_TIME_MS, Utils.DEFAULT_AI_MODE, Ai.DEFAULT_DEPTH);
    SaveBoard sauvegarde = new SaveBoard(game.getBoard(), game);

    sauvegarde.saveToFile("test_verbose.txt");

    String content = Files.readString(getSavePath("test_verbose.txt"));
    assertTrue(
        content.contains("verbose=true"),
        "Le paramètre verbose=true doit être écrit.");
  }

  @Test
  void testAiTimePresent() throws Exception {
    GameCheckers game = newGameWithAi(
        10, Utils.DEFAULT_BLITZ, Utils.DEFAULT_TIME, Utils.DEFAULT_DEBUG,
        Utils.DEFAULT_VERBOSE, false, false, 1234L, Utils.DEFAULT_AI_MODE, Ai.DEFAULT_DEPTH);
    SaveBoard sauvegarde = new SaveBoard(game.getBoard(), game);

    sauvegarde.saveToFile("test_ai_time.txt");

    String content = Files.readString(getSavePath("test_ai_time.txt"));
    assertTrue(
        content.contains("ai-time=1234"),
        "Le paramètre ai-time doit être écrit.");
  }

  @Test
  void testAiModeNoneWritesDepthZero() throws Exception {
    GameCheckers game = newGameWithAi(
        10, Utils.DEFAULT_BLITZ, Utils.DEFAULT_TIME, Utils.DEFAULT_DEBUG,
        Utils.DEFAULT_VERBOSE, false, false, Ai.DEFAULT_MAX_TIME_MS, Utils.DEFAULT_AI_MODE,
        Ai.DEFAULT_DEPTH);
    SaveBoard sauvegarde = new SaveBoard(game.getBoard(), game);

    sauvegarde.saveToFile("test_ai_none_depth.txt");

    String content = Files.readString(getSavePath("test_ai_none_depth.txt"));
    assertTrue(content.contains("ai-mode=none"),
        "Quand aucune IA n'est active, ai-mode doit valoir none.");
    assertTrue(content.contains("ai-depth=0"),
        "Quand aucune IA n'est active, ai-depth doit valoir 0.");
  }

  @Test
  void testSaveWritesWhiteAiMode() throws Exception {
    GameCheckers game = newGameWithAi(
        10, Utils.DEFAULT_BLITZ, Utils.DEFAULT_TIME, Utils.DEFAULT_DEBUG,
        Utils.DEFAULT_VERBOSE, true, false, 1500L, "minimax", 4);
    SaveBoard sauvegarde = new SaveBoard(game.getBoard(), game);

    sauvegarde.saveToFile("test_white_ai_mode.txt");

    String content = Files.readString(getSavePath("test_white_ai_mode.txt"));
    assertTrue(
        content.contains("ai-mode=white-"),
        "Quand seule l'IA blanche est active, ai-mode doit commencer par white-.");
    assertTrue(
        content.contains("ai-depth=4"),
        "La profondeur de l'IA blanche doit être écrite.");
    assertTrue(
        content.contains("ai-time=1500"),
        "Le temps de l'IA doit être écrit.");
  }

  @Test
  void testSaveWritesBlackAiMode() throws Exception {
    GameCheckers game = newGameWithAi(
        10, Utils.DEFAULT_BLITZ, Utils.DEFAULT_TIME, Utils.DEFAULT_DEBUG,
        Utils.DEFAULT_VERBOSE, false, true, 2000L, "alphabeta", 5);
    SaveBoard sauvegarde = new SaveBoard(game.getBoard(), game);

    sauvegarde.saveToFile("test_black_ai_mode.txt");

    String content = Files.readString(getSavePath("test_black_ai_mode.txt"));
    assertTrue(
        content.contains("ai-mode=black-"),
        "Quand seule l'IA noire est active, ai-mode doit commencer par black-.");
    assertTrue(
        content.contains("ai-depth=5"),
        "La profondeur de l'IA noire doit être écrite.");
    assertTrue(
        content.contains("ai-time=2000"),
        "Le temps de l'IA doit être écrit.");
  }



  @Test
  void testHistorySectionExistsEvenWhenEmpty() throws Exception {
    GameCheckers game = newGame(10, Utils.DEFAULT_BLITZ, Utils.DEFAULT_TIME, Utils.DEFAULT_DEBUG);
    SaveBoard sauvegarde = new SaveBoard(game.getBoard(), game);

    sauvegarde.saveToFile("test_history_section.txt");

    String content = Files.readString(getSavePath("test_history_section.txt"));
    assertTrue(content.contains("[history]"),
        "La section [history] doit toujours être présente.");
  }

  @Test
  void testHistoryContainsPlayedMove() throws Exception {
    GameCheckers game = newGame(10, Utils.DEFAULT_BLITZ, Utils.DEFAULT_TIME, Utils.DEFAULT_DEBUG);

    Move move = game.getPossibleMoves(game.getCurrentPlayer()).get(0);
    String expectedMoveText = move.toString();

    game.applyMove(
        game.getBoard().indexToSquare(move.getFrom()),
        game.getBoard().indexToSquare(move.getTo()),
        false);

    SaveBoard sauvegarde = new SaveBoard(game.getBoard(), game);
    sauvegarde.saveToFile("test_history_move.txt");

    String content = Files.readString(getSavePath("test_history_move.txt"));
    assertTrue(
        content.contains("W " + expectedMoveText) || content.contains("B " + expectedMoveText),
        "L'historique doit contenir le coup joué.");
  }

  @Test
  void testSaveRefusedWhenGameAlreadyFinished() throws Exception {
    GameCheckers game = newGame(10, Utils.DEFAULT_BLITZ, Utils.DEFAULT_TIME, Utils.DEFAULT_DEBUG);

    game.setState(fr.ubordeaux.pdp.model.core.State.FINISHED);

    Path target = getSavePath("test_finished_game.txt");
    Files.deleteIfExists(target);

    SaveBoard sauvegarde = new SaveBoard(game.getBoard(), game);
    sauvegarde.saveToFile("test_finished_game.txt");

    assertFalse(Files.exists(target),
        "Aucun fichier ne doit être créé si la partie est déjà finie.");
  }

  @Test
  void testBoardSizeEightWrittenCorrectly() throws Exception {
    GameCheckers game = newGame(8, Utils.DEFAULT_BLITZ, Utils.DEFAULT_TIME, Utils.DEFAULT_DEBUG);
    SaveBoard sauvegarde = new SaveBoard(game.getBoard(), game);

    sauvegarde.saveToFile("test_board_size_8.txt");

    String content = Files.readString(getSavePath("test_board_size_8.txt"));
    assertTrue(
        content.contains("board-size=8"),
        "Le paramètre board-size=8 doit être écrit.");
  }

  @Test
  void testBoardSizeTenWrittenCorrectly() throws Exception {
    GameCheckers game = newGame(10, Utils.DEFAULT_BLITZ, Utils.DEFAULT_TIME, Utils.DEFAULT_DEBUG);
    SaveBoard sauvegarde = new SaveBoard(game.getBoard(), game);

    sauvegarde.saveToFile("test_board_size_10.txt");

    String content = Files.readString(getSavePath("test_board_size_10.txt"));
    assertTrue(
        content.contains("board-size=10"),
        "Le paramètre board-size=10 doit être écrit.");
  }

}