package fr.ubordeaux.pdp.model;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

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

    private String buildBoardAscii(int n, java.util.Map<String, Character> pieces) {
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row < n; row++) {
            for (int col = 0; col < n; col++) {
                String sq = "" + (char) ('A' + row) + (col + 1);
                char c = pieces.getOrDefault(sq, '-');
                sb.append(c);
                if (col < n - 1) sb.append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private long[] snapshot(Board b) {
        return new long[]{
                b.getWhitePawns1(), b.getWhitePawns2(),
                b.getBlackPawns1(), b.getBlackPawns2(),
                b.getWhiteCheckers1(), b.getWhiteCheckers2(),
                b.getBlackCheckers1(), b.getBlackCheckers2()
        };
    }

    // --------------------------------------------------------------------
    // SUCCESS CASE
    // --------------------------------------------------------------------

    @Test
    void testLoadRestoresTurnAndKings() throws IOException {
        int n = 10;

        var pieces = new java.util.HashMap<String, Character>();
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

        GameCheckers game = new GameCheckers(
                new Configuration(false, Utils.DEFAULT_TIME, false, n, false, false)
        );
        Board board = game.getBoard();

        new BoardChargement(board, game).loadFromFile("load_ok.txt");

        assertFalse(game.isWhiteTurn());

        int a1 = board.squareToIndex("A1");
        int b2 = board.squareToIndex("B2");
        int c3 = board.squareToIndex("C3");
        int d4 = board.squareToIndex("D4");

        assertEquals(1L << a1, board.getWhitePawns1());
        assertEquals(1L << b2, board.getBlackPawns1());
        assertEquals(1L << c3, board.getWhiteCheckers1());
        assertEquals(1L << d4, board.getBlackCheckers1());
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
                if (row == 0 && col == 1) c = 'x'; // non playable
                bad.append(c);
                if (col < n - 1) bad.append(" ");
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

        GameCheckers game = new GameCheckers(
                new Configuration(false, Utils.DEFAULT_TIME, false, n, false, false)
        );
        Board b = game.getBoard();

        new BoardChargement(b, game).loadFromFile("load_bad_square.txt");

        // Ton loader reset dès qu'il entre dans [game]
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
                        "board-size=12\n" +  // mismatch
                        "\n" +
                        "[game]\n" +
                        "- - - - - - - - - -\n".repeat(realSize) +
                        "\n" +
                        "[history]\n";

        writeSaveFile("load_bad_size.txt", content);

        GameCheckers game = new GameCheckers(
                new Configuration(false, Utils.DEFAULT_TIME, false, realSize, false, false)
        );
        Board b = game.getBoard();

        long[] before = snapshot(b);

        new BoardChargement(b, game).loadFromFile("load_bad_size.txt");

        // erreur avant [game] => pas de reset
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

        GameCheckers game = new GameCheckers(
                new Configuration(false, Utils.DEFAULT_TIME, false, n, false, false)
        );
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

        GameCheckers game = new GameCheckers(
                new Configuration(false, Utils.DEFAULT_TIME, false, n, false, false)
        );
        Board b = game.getBoard();

        new BoardChargement(b, game).loadFromFile("load_missing_settings.txt");

        // reset exécuté
        assertEquals(0L, b.getWhitePawns1());
        assertEquals(0L, b.getBlackPawns1());
    }

    // --------------------------------------------------------------------
    // ERROR: Missing [history] section (board chargé mais erreur finale)
    // --------------------------------------------------------------------

    @Test
    void testLoadFailsIfMissingHistorySection_boardLoaded() throws IOException {
        int n = 10;

        var pieces = new java.util.HashMap<String, Character>();
        pieces.put("A1", 'o');

        String content =
                "[settings]\n" +
                        "starting-player=white\n" +
                        "board-size=" + n + "\n" +
                        "\n" +
                        "[game]\n" +
                        buildBoardAscii(n, pieces);

        writeSaveFile("load_missing_history.txt", content);

        GameCheckers game = new GameCheckers(
                new Configuration(false, Utils.DEFAULT_TIME, false, n, false, false)
        );
        Board b = game.getBoard();

        new BoardChargement(b, game).loadFromFile("load_missing_history.txt");

        int a1 = b.squareToIndex("A1");
        assertEquals(1L << a1, b.getWhitePawns1());
    }
}