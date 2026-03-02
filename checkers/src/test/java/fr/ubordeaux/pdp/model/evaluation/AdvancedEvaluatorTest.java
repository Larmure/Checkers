package fr.ubordeaux.pdp.model.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import fr.ubordeaux.pdp.model.Board;

public class AdvancedEvaluatorTest {
    // ===========================================================================
  //  evaluateAdvanced() — matériel + avancement
  // ===========================================================================

  @Test
  void advanced_emptyBoardIsZero() {
    Board board = new Board(12);
    board.clearBoard();
    assertEquals(0, new AdvancedEvaluator().evaluate(board), "Empty board must be 0");
  }

  @Test
  void advanced_initialPositionIsSymmetric() {
    assertEquals(0, new AdvancedEvaluator().evaluate(new Board(8)),  "8x8 initial must be 0");
    assertEquals(0, new AdvancedEvaluator().evaluate(new Board(10)), "10x10 initial must be 0");
    assertEquals(0, new AdvancedEvaluator().evaluate(new Board(12)), "12x12 initial must be 0");
  }

  @Test
  void advanced_whitePawnAdvancementIncreasesScore() {
    Board board = new Board(12);
    board.clearBoard();

    board.addWhite("A1"); // row 0, bonus = 0
    int scoreBack = new AdvancedEvaluator().evaluate(board);

    board.clearBoard();
    board.addWhite("K11"); // row 10, bonus = 10*8 = 80
    int scoreAdvanced = new AdvancedEvaluator().evaluate(board);

    assertTrue(scoreAdvanced > scoreBack,
        "White pawn further north must score higher");
    assertEquals(80, scoreAdvanced - scoreBack,
        "Advancement bonus difference must be 80 (10 rows * 8)");
  }

  @Test
  void advanced_blackPawnAdvancementDecreasesScore() {
    Board board = new Board(12);
    board.clearBoard();

    board.addBlack("L12"); // row 11, bonus = (11-11)*8 = 0 — position de départ
    int scoreBack = new AdvancedEvaluator().evaluate(board);

    board.clearBoard();
    board.addBlack("B2"); // row 1, bonus = (11-1)*8 = 80 — proche du but
    int scoreAdvanced = new AdvancedEvaluator().evaluate(board);

    assertTrue(scoreAdvanced < scoreBack,
        "Black pawn nearer its goal (B2) must score more negatively than at start (L12)");
    assertEquals(-80, scoreAdvanced - scoreBack,
        "Black advancement bonus difference must be -80");
  }

  @Test
  void advanced_strictlyMoreThanSimple_forAdvancedWhitePawn() {
    Board board = new Board(12);
    board.clearBoard();
    board.addWhite("K11"); // row 10 → bonus avancement non nul

    assertTrue(new AdvancedEvaluator().evaluate(board) > new SimpleEvaluator().evaluate(board),
        "evaluateAdvanced must exceed evaluateSimple for an advanced white pawn");
  }

  @Test
  void advanced_checkerNoAdvancementBonus() {
    // Les checkers n'ont pas de bonus d'avancement dans evaluateAdvanced
    Board board = new Board(12);
    board.clearBoard();

    board.addWhite("A1");
    board.promote("A1");
    int scoreBack = new AdvancedEvaluator().evaluate(board);

    board.clearBoard();
    board.addWhite("K11");
    board.promote("K11");
    int scoreAdvanced = new AdvancedEvaluator().evaluate(board);

    assertEquals(scoreBack, scoreAdvanced,
        "Checkers must not receive advancement bonus in evaluateAdvanced");
  }
}
