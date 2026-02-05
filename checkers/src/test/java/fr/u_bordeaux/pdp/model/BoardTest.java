package fr.u_bordeaux.pdp.model;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class BoardTest {

    @Test
    void testBoardSize8Initialization() {
        Board board = new Board(8);

        // 12 premier bit à 1
        long expectedWhite = (1L << 12) - 1;
        long expectedBlack = expectedWhite << 20;

        assertEquals(expectedWhite, board.getWhitePawns1());
        assertEquals(0L, board.getWhitePawns2());

        assertEquals(expectedBlack, board.getBlackPawns1());
        assertEquals(0L, board.getBlackPawns2());
    }

    @Test
    void testBoardSize10Initialization() {
        Board board = new Board(10);

        long expectedWhite = (1L << 20) - 1;
        long expectedBlack = expectedWhite << 30;

        assertEquals(expectedWhite, board.getWhitePawns1());
        assertEquals(0L, board.getWhitePawns2());

        assertEquals(expectedBlack, board.getBlackPawns1());
        assertEquals(0L, board.getBlackPawns2());
    }

    @Test
    void testBoardSize12Initialization() {
        Board board = new Board(12);

        long expectedWhite = (1L << 30) - 1;
        long expectedBlack1 = expectedWhite << 42;
        long expectedBlack2 = (1L << 8) - 1;

        assertEquals(expectedWhite, board.getWhitePawns1());
        assertEquals(0L, board.getWhitePawns2());

        assertEquals(expectedBlack1, board.getBlackPawns1());
        assertEquals(expectedBlack2, board.getBlackPawns2());
    }

    @Test
    void testInvalidBoardSizeThrowsException() {
        Exception exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Board(7)
        );

        assertTrue(exception.getMessage().contains("Invalid board size"));
    }
}

