package fr.ubordeaux.pdp.model.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import fr.ubordeaux.pdp.model.core.Board;

public class MaxEvaluatorTest {
    // ===========================================================================
  //  evaluateMax() — matériel + avancement + centre + mobilité
  // ===========================================================================

  @Test
  void max_emptyBoardIsZero() {
    Board board = new Board(12);
    board.clearBoard();
    assertEquals(0, new MaxEvaluator().evaluate(board), "Empty board must be 0");
  }

  @Test
  void max_initialPositionIsSymmetric() {
    assertEquals(0, new MaxEvaluator().evaluate(new Board(8)), "8x8 initial must be 0");
    assertEquals(0, new MaxEvaluator().evaluate(new Board(10)), "10x10 initial must be 0");
    assertEquals(0, new MaxEvaluator().evaluate(new Board(12)),
        "12x12 initial must be 0 after center fix (sizeBoard/2 ± 2)");
  }

  @Test
  void max_whitePawnInCenterScoresMoreThanEdge() {
    Board board = new Board(12);
    board.clearBoard();

    board.addWhite("A1");
    int scoreEdge = new MaxEvaluator().evaluate(board);

    board.clearBoard();
    board.addWhite("F6");
    int scoreCenter = new MaxEvaluator().evaluate(board);

    assertTrue(scoreCenter > scoreEdge,
        "White pawn in center must score higher than on edge");
  }

  @Test
  void max_blackPawnInCenterScoresMoreNegativeThanEdge() {
    Board board = new Board(12);
    board.clearBoard();

    board.addBlack("L12");
    int scoreEdge = new MaxEvaluator().evaluate(board);

    board.clearBoard();
    board.addBlack("G7");
    int scoreCenter = new MaxEvaluator().evaluate(board);

    assertTrue(scoreCenter < scoreEdge,
        "Black pawn in center must score more negatively than on edge");
  }

  @Test
  void max_mobilityAddsScore_moreMovesIsBetter() {
    Board board = new Board(12);
    board.clearBoard();

    board.addWhite("A1"); // coin, 1 seul mouvement possible
    int scoreLow = new MaxEvaluator().evaluate(board);

    board.clearBoard();
    board.addWhite("E5");
    board.addWhite("G5");
    board.addWhite("C5"); // 3 pions au centre = plus de mobilité
    int scoreHigh = new MaxEvaluator().evaluate(board);

    assertTrue(scoreHigh > scoreLow,
        "More white pieces with more mobility must score higher");
  }

  @Test
  void max_strictlyMoreThanAdvanced_forCentralPawn() {
    Board board = new Board(12);
    board.clearBoard();
    board.addWhite("F6"); // case centrale

    assertTrue(new MaxEvaluator().evaluate(board) > new AdvancedEvaluator().evaluate(board),
        "evaluateMax must exceed evaluateAdvanced for a central pawn (center + mobility bonus)");
  }

  @Test
  void max_signFlipWhenColorsReversed() {
    Board board = new Board(12);
    board.clearBoard();
    board.addWhite("C3");
    board.addWhite("E5");
    board.addBlack("H8");
    int white = new MaxEvaluator().evaluate(board);

    board.clearBoard();
    board.addBlack("C3");
    board.addBlack("E5");
    board.addWhite("H8");
    int black = new MaxEvaluator().evaluate(board);

    assertTrue(white > 0 && black < 0,
        "Reversing colors must flip the sign of evaluateMax");
  }

  @Test
  void max_checkerVsPawnAdvantageIsSignificant() {
    Board board = new Board(12);
    board.clearBoard();
    board.addWhite("F6");
    board.promote("F6");
    board.addBlack("G7");

    assertTrue(new MaxEvaluator().evaluate(board) > 100,
        "White checker vs black pawn must yield a score > 100");
  }
}
