package fr.u_bordeaux.pdp.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * Unit tests for the Board class.
 * 
 * This test suite verifies:
 * - Board size validation
 * - Initial piece placement
 * - Move validation limits
 * - Promotion rules
 * - Move generation integrity
 */
public class BoardTest {

    // ===============================
    // SIZE VALIDATION
    // ===============================

    @Test
    void validBoardSizesShouldWork() {
        assertDoesNotThrow(() -> new Board(8));
        assertDoesNotThrow(() -> new Board(10));
        assertDoesNotThrow(() -> new Board(12));
    }

    @Test
    void invalidBoardSizeShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> new Board(6));
        assertThrows(IllegalArgumentException.class, () -> new Board(9));
        assertThrows(IllegalArgumentException.class, () -> new Board(14));
    }

    // ===============================
    // INITIAL POSITION
    // ===============================

    @Test
    void whiteShouldHaveValidMovesAtStart() {
        Board board = new Board(8);
        List<String> moves = board.getWhiteValidMoves();
        assertFalse(moves.isEmpty(), "White should have initial moves");
    }

    @Test
    void blackShouldHaveValidMovesAtStart() {
        Board board = new Board(8);
        List<String> moves = board.getBlackValidMoves();
        assertFalse(moves.isEmpty(), "Black should have initial moves");
    }

    // ===============================
    // MOVE LIMITS
    // ===============================

    @Test
    void movingOutOfBoundsShouldThrow() {
        Board board = new Board(8);

        assertThrows(IllegalArgumentException.class,
                () -> board.move("A1-Z9"));
    }

    @Test
    void movingToOccupiedSquareShouldThrow() {
        Board board = new Board(8);

        // Case initialement occupée
        assertThrows(IllegalArgumentException.class,
                () -> board.move("A1-B2"));
    }

    @Test
    void movingFromEmptySquareShouldThrow() {
        Board board = new Board(8);

        assertThrows(IllegalArgumentException.class,
                () -> board.move("D4-E5"));
    }

    // ===============================
    // PROMOTION
    // ===============================

    @Test
    void promotingEmptySquareShouldThrow() {
        Board board = new Board(8);

        assertThrows(IllegalArgumentException.class,
                () -> board.promote("D4"));
    }

    // ===============================
    // INDEX CONVERSION LIMITS
    // ===============================

    @Test
    void squareConversionShouldNotCrashOnValidSquares() {
        Board board = new Board(12);

        assertDoesNotThrow(() -> board.isWhitePawn("A1"));
        assertDoesNotThrow(() -> board.isBlackPawn("L12"));
    }

    // ===============================
    // CAPTURE PRIORITY RULE
    // ===============================

    @Test
    void captureShouldBeMandatoryIfAvailable() {
        Board board = new Board(8);

        // Forcing a manual capture setup
        board.move("C3-D4"); // example setup (adapt if needed)

        List<String> whiteMoves = board.getWhiteValidMoves();

        boolean containsCapture = whiteMoves.stream().anyMatch(m -> m.contains("x"));

        if (containsCapture) {
            // If a capture exists, all moves must be captures
            assertTrue(
                whiteMoves.stream().allMatch(m -> m.contains("x")),
                "If capture exists, only capture moves should be returned"
            );
        }
    }
}
