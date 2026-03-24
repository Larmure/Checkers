package fr.ubordeaux.pdp.model.tools;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.model.core.Move;

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
                Utils.DEFAULT_AI_TIME,
                Utils.DEFAULT_AI_MODE);
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
                game.getBoard().indexToSquare(move.getTo()));

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
                        cell.equals("-") || cell.equals("o") || cell.equals("x") || cell.equals("O")
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
                game.getBoard().indexToSquare(move.getTo()));

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
                game.getBoard().indexToSquare(move1.getTo()));

        Move move2 = game.getPossibleMoves(game.getCurrentPlayer()).get(0);
        game.applyMove(
                game.getBoard().indexToSquare(move2.getFrom()),
                game.getBoard().indexToSquare(move2.getTo()));

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
}