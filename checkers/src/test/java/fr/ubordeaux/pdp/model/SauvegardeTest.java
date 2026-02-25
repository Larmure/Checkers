package fr.ubordeaux.pdp.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
public class SauvegardeTest {
    @Test
    void testSaveBoardSize8() throws IOException {
        Board board = new Board(8);
        BoardSauvegarde sauvegarde = new BoardSauvegarde(board);

        String fileName = "test_board_8.txt";
        sauvegarde.saveToFile(fileName);

        Path path = Path.of("Sauvegarde", fileName);
        assertTrue(Files.exists(path));

        String content = Files.readString(path);

        String expected =
                         "- o - o - o - o\n" +
                        "o - o - o - o -\n" +
                        "- o - o - o - o\n" +
                        "- - - - - - - -\n" +
                        "- - - - - - - -\n" +
                        "x - x - x - x -\n" +
                        "- x - x - x - x\n" +
                        "x - x - x - x -\n";


        assertEquals(expected, content);
    }

    @Test
    void testSaveBoardSize10() throws IOException {
        Board board = new Board(10);
        BoardSauvegarde sauvegarde = new BoardSauvegarde(board);

        String fileName = "test_board_10.txt";
        sauvegarde.saveToFile(fileName);

        Path path = Path.of("Sauvegarde", fileName);
        assertTrue(Files.exists(path));

        String content = Files.readString(path);

        // On vérifie juste des propriétés clés (pas tout le plateau)
        assertTrue(content.contains("x"));
        assertTrue(content.contains("o"));
        assertEquals(10, content.split("\n").length);
    }

    @Test
    void testSaveBoardSize12() throws IOException {
        Board board = new Board(12);
        BoardSauvegarde sauvegarde = new BoardSauvegarde(board);

        String fileName = "test_board_12.txt";
        sauvegarde.saveToFile(fileName);

        Path path = Path.of("Sauvegarde", fileName);
        assertTrue(Files.exists(path));

        String content = Files.readString(path);

        assertEquals(12, content.split("\n").length);
    }
}
