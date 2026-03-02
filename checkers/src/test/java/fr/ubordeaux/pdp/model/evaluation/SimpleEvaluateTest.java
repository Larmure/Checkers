package fr.ubordeaux.pdp.model.evaluation;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import fr.ubordeaux.pdp.model.Board;

/**
 * Tests for the three evaluation functions:
 *   - evaluateSimple()   : material only (pawns + checkers)
 *   - evaluateAdvanced() : material + advancement bonus
 *   - evaluateMax()      : material + advancement + center + mobility
 *
 * Convention: positive score = white advantage, negative = black advantage.
 */
class SimpleEvaluateTest {

  // ===========================================================================
  //  evaluateSimple() — matériel uniquement
  // ===========================================================================

  @Test
  void simple_emptyBoardIsZero() {
    Board board = new Board(12);
    Evaluator e = new SimpleEvaluator();
    board.clearBoard();
    assertEquals(0, e.evaluate(board), "Empty board must be 0");
  }

  @Test
  void simple_initialPositionIsSymmetric() {
    assertEquals(0, new SimpleEvaluator().evaluate(new Board(8)),  "8x8 initial must be 0");
    assertEquals(0, new SimpleEvaluator().evaluate(new Board(10)), "10x10 initial must be 0");
    assertEquals(0, new SimpleEvaluator().evaluate(new Board(12)), "12x12 initial must be 0");
  }

  @Test
  void simple_onePawnWhite_positive() {
    Board board = new Board(12);
    board.clearBoard();
    board.addWhite("F6");
    assertTrue(new SimpleEvaluator().evaluate(board) > 0, "One white pawn must be positive");
  }

  @Test
  void simple_onePawnBlack_negative() {
    Board board = new Board(12);
    board.clearBoard();
    board.addBlack("G7");
    assertTrue(new SimpleEvaluator().evaluate(board) < 0, "One black pawn must be negative");
  }

  @Test
  void simple_checkerWorthMoreThanPawn() {
    Board board = new Board(12);
    board.clearBoard();

    board.addWhite("F6");
    int pawnScore = new SimpleEvaluator().evaluate(board);

    board.clearBoard();
    board.addWhite("F6");
    board.promote("F6");
    int checkerScore = new SimpleEvaluator().evaluate(board);

    assertTrue(checkerScore > pawnScore, "Checker (350) must score more than pawn (100)");
  }

  @Test
  void simple_materialDifference_twoVsOne() {
    Board board = new Board(12);
    board.clearBoard();
    board.addWhite("C3");
    board.addWhite("E5");
    board.addBlack("H8");

    assertEquals(100, new SimpleEvaluator().evaluate(board), "2 white pawns vs 1 black pawn = +100");
  }

  @Test
  void simple_locationDoesNotMatter() {
    Board board = new Board(12);
    board.clearBoard();

    board.addWhite("A1");
    int scoreCorner = new SimpleEvaluator().evaluate(board);

    board.clearBoard();
    board.addWhite("F6");
    int scoreCenter = new SimpleEvaluator().evaluate(board);

    assertEquals(scoreCorner, scoreCenter,
        "evaluateSimple must not vary with position (no positional bonus)");
  }

  @Test
  void simple_signFlipWhenColorsReversed() {
    Board board = new Board(12);
    board.clearBoard();
    board.addWhite("C3");
    board.addWhite("E5");
    board.addBlack("H8");
    int white = new SimpleEvaluator().evaluate(board);

    board.clearBoard();
    board.addBlack("C3");
    board.addBlack("E5");
    board.addWhite("H8");
    int black = new SimpleEvaluator().evaluate(board);

    assertEquals(-white, black, "Reversing colors must negate the score exactly");
  }

}