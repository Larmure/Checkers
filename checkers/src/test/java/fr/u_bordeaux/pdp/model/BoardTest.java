package fr.u_bordeaux.pdp.model;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class BoardTest {

    @Test
    void testBoardSize8Initialization() {
        Board board = new Board(8);
        board.printBoard();
        board.move("A1-D1");
        board.printBoard();
        board.promote("H2");
        board.printBoard();
    }

    @Test
    void testBoardSize10Initialization() {
        Board board = new Board(10);
    }

    @Test
    void testBoardSize12Initialization() {
        Board board = new Board(12);
    }

    @Test
    void testInvalidBoardSizeThrowsException() {
    }
}

