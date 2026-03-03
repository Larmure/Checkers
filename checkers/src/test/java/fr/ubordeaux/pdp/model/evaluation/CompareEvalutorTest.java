package fr.ubordeaux.pdp.model.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import fr.ubordeaux.pdp.model.core.Board;

public class CompareEvalutorTest {
     // ===========================================================================
  //  Comparaison entre les 3 fonctions
  // ===========================================================================

  @Test
  void comparison_simpleIgnoresPosition() {
    Board board = new Board(12);
    board.clearBoard();

    board.addWhite("A1");
    int s1 = new SimpleEvaluator().evaluate(board);
    board.clearBoard();
    board.addWhite("L12");
    int s2 = new SimpleEvaluator().evaluate(board);

    assertEquals(s1, s2,
        "evaluateSimple must return the same score regardless of position");
  }

  @Test
  void comparison_advancedDiffersFromSimpleWhenPawnAdvanced() {
    Board board = new Board(12);
    board.clearBoard();
    board.addWhite("K11");

    assertNotEquals(new SimpleEvaluator().evaluate(board), new AdvancedEvaluator().evaluate(board),
        "evaluateAdvanced must differ from evaluateSimple for an advanced pawn");
  }

  @Test
  void comparison_maxDiffersFromAdvancedForCentralPiece() {
    Board board = new Board(12);
    board.clearBoard();
    board.addWhite("F6");

    assertNotEquals(new AdvancedEvaluator().evaluate(board), new MaxEvaluator().evaluate(board),
        "evaluateMax must differ from evaluateAdvanced (center/mobility bonus)");
  }

  @Test
  void comparison_allThreeEqualOnEmptyBoard() {
    Board board = new Board(12);
    board.clearBoard();

    assertEquals(0, new SimpleEvaluator().evaluate(board));
    assertEquals(0, new AdvancedEvaluator().evaluate(board));
    assertEquals(0, new MaxEvaluator().evaluate(board));
  }

  @Test
  void comparison_rankingSimpleLessAdvancedLessMax_forGoodPosition() {
    // Un pion blanc en case centrale avancée doit donner :
    // simple < advanced < max (les bonus s'accumulent)
    Board board = new Board(12);
    board.clearBoard();
    board.addWhite("F6"); // rangée 5 (avancée) + case centrale

    int s = new SimpleEvaluator().evaluate(board);
    int a = new AdvancedEvaluator().evaluate(board);
    int m = new MaxEvaluator().evaluate(board);

    assertTrue(s <= a, "evaluateAdvanced must be >= evaluateSimple for an advanced pawn");
    assertTrue(a <= m, "evaluateMax must be >= evaluateAdvanced for a central pawn");
    assertTrue(s < m,  "evaluateMax must strictly exceed evaluateSimple");
  }
}
